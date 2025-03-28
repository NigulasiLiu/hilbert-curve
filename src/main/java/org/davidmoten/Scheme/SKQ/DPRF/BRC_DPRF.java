package org.davidmoten.Scheme.SKQ.DPRF;

import org.davidmoten.BPC.BPCGenerator;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class BRC_DPRF {
    private int depth;  // 树的深度
    private int maxRange;
    private int lambda; // 安全参数 λ
    private static final MessageDigest sha256; // 全局共享的 MessageDigest 实例

    static {
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    // 构造函数，允许动态设置 lambda 和 depth
    public BRC_DPRF(int lambda, int depth) {
        this.lambda = lambda;
        this.depth = depth;
        this.maxRange = (1 << depth) - 1;
    }

    // PRG 伪随机生成器，基于全局共享的 SHA-256，输出 256 位
    public static byte[] pseudorandomGenerator(byte[] seed) {
        synchronized (sha256) { // 确保线程安全
            sha256.reset();
            return sha256.digest(seed);
        }
    }

    // G 的左半部分输出（128 位）
    public static byte[] G0(byte[] seed) {
        byte[] result = pseudorandomGenerator(seed);
        byte[] leftHalf = new byte[16]; // 前 128 位
        System.arraycopy(result, 0, leftHalf, 0, 16);
        return leftHalf;
    }

    // G 的右半部分输出（128 位）
    public static byte[] G1(byte[] seed) {
        byte[] result = pseudorandomGenerator(seed);
        byte[] rightHalf = new byte[16]; // 后 128 位
        System.arraycopy(result, 16, rightHalf, 0, 16);
        return rightHalf;
    }

    // 根据 GGM 树的机制计算 PRF 值 fk(x)
    public static byte[] computePRF(byte[] key, String binaryInput) {
        byte[] current = key;
        for (char bit : binaryInput.toCharArray()) {
            current = (bit == '0') ? G0(current) : G1(current);
        }
        return current;
    }

    public byte[] deriveByIndex(int index, List<Trapdoor> delkeys) {
        for (Trapdoor delkey : delkeys) {
            //找到当前输入的index对应哪个BRC节点，path
            if (delkey.getPosition().contains(index)) {
                String indexBinaryString = String.format("%" + depth + "s", Integer.toBinaryString(index)).replace(' ', '0');
                int computeLen = indexBinaryString.length() - delkey.getPath().length();
                if (computeLen == 0) {
                    return delkey.getPartialPRF();
                } else {
                    String t = indexBinaryString.substring(depth - computeLen);
                    return computePRF(delkey.getPartialPRF(), t);
                }
            }
        }
        //未授权的index，返回一个相同长度的SecureRandom
        byte[] randomBytes = new byte[this.lambda / 8]; // λ bits = λ / 8 bytes
        SecureRandom prgtemp = new SecureRandom();
        prgtemp.nextBytes(randomBytes);
        return randomBytes;
    }

    public byte[] newDerivedKey(byte[] Kp, int newIndex) {
        String t = String.format("%" + depth + "s", Integer.toBinaryString(newIndex)).replace(' ', '0');
        return computePRF(Kp, t);
    }

    // 陷门生成算法 T：计算覆盖范围 [a, b] 的最小子树集合
    public List<Trapdoor> delKey(byte[] Kp, int currentRange) {
        List<Trapdoor> delKeys = new ArrayList<>();
        BigInteger[] RangeSide = new BigInteger[]{BigInteger.ZERO, new BigInteger(String.valueOf(currentRange))};
        //生成min到max的所有Bigint
        BigInteger[] Range = Stream.iterate(RangeSide[0], n -> n.add(BigInteger.ONE))
                .limit(RangeSide[1].subtract(RangeSide[0]).add(BigInteger.ONE).intValueExact())
                .toArray(BigInteger[]::new);
        List<String> BRC = BPCGenerator.convertToOnlyPrefix(BPCGenerator.GetBPCValueMap(Range, depth), depth);
        for (String bpc : BRC) {
            int shiftBits = depth - bpc.length();
            //bpc长度等于depth时，说明是一个单独的叶子节点，否则存储多个index
            if (shiftBits != 0) {
//            int[] containedPos = new int[1<<(depth-bpc.length())];
                List<Integer> containsPos = new ArrayList<>();
                int range = 1 << shiftBits; //前缀bpc下包含了多range个子前缀编码
                int base = 0;
                if (!(shiftBits == depth && bpc.isEmpty())) {
                    base = Integer.valueOf(bpc, 2) << shiftBits;  //bpc是一个二进制序列，将其左移(depth - bpc.length())位
                }
                for (int i = 0; i < range; i++) {
                    containsPos.add(base + i);
                }
                delKeys.add(new Trapdoor(computePRF(Kp, bpc), containsPos, bpc));
            } else {
                List<Integer> containsPos = new ArrayList<>();
                containsPos.add(Integer.valueOf(bpc, 2));
                delKeys.add(new Trapdoor(computePRF(Kp, bpc), containsPos, bpc));
            }
        }
        return delKeys;
    }

    public static void main(String[] args) {
        // 初始化 DPRF，设置 lambda 和树深度
        int lambda = 128;  // 安全参数
        int depth = 12; // 树的深度
        BRC_DPRF dprf = new BRC_DPRF(lambda, depth);
        // 测试范围：[2, 7]
        int r1 = 8;
        int r2 = 13;
        BigInteger[] RangeSide = new BigInteger[]{new BigInteger(String.valueOf(r1)), new BigInteger(String.valueOf(r2))};
        //生成min到max的所有Bigint
        BigInteger[] Range = Stream.iterate(RangeSide[0], n -> n.add(BigInteger.ONE))
                .limit(RangeSide[1].subtract(RangeSide[0]).add(BigInteger.ONE).intValueExact())
                .toArray(BigInteger[]::new);
        // 获取叶子节点路径
        List<String> leafPaths = dprf.getLeafPaths(0, r2);
        List<String> BRC = BPCGenerator.convertToOnlyPrefix(BPCGenerator.GetBPCValueMap(Range, depth), depth);
        // 测试 DelKey 方法
        byte[] Kp = "master-key".getBytes(); // 假设的主密钥
//        System.out.println("叶子节点的二进制路径：");
//        for (int i = 0; i < r2; i++) {
//            System.out.printf("叶子节点索引: %d, 二进制路径: %s ", i, leafPaths.get(i));
//            System.out.printf("PRF值: %s\n", Arrays.toString(computePRF(Kp, leafPaths.get(i))));
//        }
        List<Trapdoor> trapdoors = dprf.delKey(Kp, 0);
        // 输出 Trapdoor
        System.out.println("\n生成的 Trapdoor：");
        for (Trapdoor t : trapdoors) {
            System.out.printf("Trapdoor: partialPRF=%s, positions=%s, path=%s\n",
                    Arrays.toString(t.getPartialPRF()),
                    t.getPosition(),
                    t.getPath());
        }

        System.out.printf("叶子节点索引: %d, 二进制路径: %s ", 0, leafPaths.get(0));
        System.out.printf("PRF值: %s\n", Arrays.toString(dprf.deriveByIndex(0, trapdoors)));

        System.out.printf("叶子节点索引: %d, 二进制路径: %s ", 8, leafPaths.get(8));
//        System.out.printf("缓存PRF值: %s\n", Arrays.toString(trapdoors.get(1).getPartialPRF()));
//        System.out.printf("手动PRF值: %s\n", Arrays.toString(computePRF(trapdoors.get(1).getPartialPRF(), "00")));
        System.out.printf("newDerived PRF值: %s\n", Arrays.toString(dprf.newDerivedKey(Kp, 0)));
//        System.out.printf("PRF值: %s\n", Arrays.toString(dprf.deriveByIndex(8, trapdoors)));

        System.out.println("叶子节点的BRC：");
        for (String brc : BRC) {
            System.out.printf(brc + " ");
        }

    }


    // 表示单个陷门条目：部分 PRF 和深度信息
    public static class Trapdoor {
        private final byte[] partialPRF;
        private List<Integer> position;//代表该节点所在层数，从下往上计算
        private final String path;

        public Trapdoor(byte[] partialPRF, List<Integer> position, String path) {
            this.partialPRF = partialPRF;
            this.position = position;
            this.path = path;
        }

        public byte[] getPartialPRF() {
            return partialPRF;
        }

        public List<Integer> getPosition() {
            return position;
        }

        public String getPath() {
            return path;
        }
    }


    /**
     * 获取叶子节点的二进制路径
     *
     * @param r1 起始叶子节点索引
     * @param r2 结束叶子节点索引
     * @return 范围内所有叶子节点的二进制路径
     */
    public List<String> getLeafPaths(int r1, int r2) {
        List<String> paths = new ArrayList<>();
        for (int i = r1; i <= r2; i++) {
            // 将索引转换为二进制字符串，并补齐至 depth 长度
            String binaryPath = String.format("%" + depth + "s", Integer.toBinaryString(i)).replace(' ', '0');
            paths.add(binaryPath);
        }
        return paths;
    }

}
