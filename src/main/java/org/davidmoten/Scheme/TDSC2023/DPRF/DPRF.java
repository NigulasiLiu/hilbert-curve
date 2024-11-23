package org.davidmoten.Scheme.TDSC2023.DPRF;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DPRF {
    private final int lambdaSec; // 安全参数
    private final byte[] masterKey; // 主密钥

    /**
     * 构造函数，初始化安全参数和主密钥。
     *
     * @param lambdaSec 安全参数（密钥长度，单位：字节）
     */
    public DPRF(int lambdaSec) {
        this.lambdaSec = lambdaSec;
        this.masterKey = generateRandomKey(lambdaSec);
    }

    /**
     * 生成随机主密钥。
     *
     * @param length 密钥长度
     * @return 随机生成的字节数组
     */
    private byte[] generateRandomKey(int length) {
        byte[] key = new byte[length];
        for (int i = 0; i < length; i++) {
            key[i] = (byte) (Math.random() * 256); // 随机生成字节值
        }
        return key;
    }

    /**
     * 伪随机生成器（PRG），将输入种子扩展为两部分。
     *
     * @param seed 输入种子
     * @return 一个长度为2的数组，分别表示左子树和右子树的伪随机值
     */
    private byte[][] prg(byte[] seed) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] left = sha256.digest(seed);
            byte[] reversedSeed = new byte[seed.length];
            for (int i = 0; i < seed.length; i++) {
                reversedSeed[i] = seed[seed.length - 1 - i];
            }
            byte[] right = sha256.digest(reversedSeed);
            return new byte[][]{left, right};
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found");
        }
    }

    /**
     * GGM伪随机函数（PRF），通过输入路径递归计算伪随机值。
     *
     * @param x 输入路径（二进制字符串）
     * @return 对应的伪随机值
     */
    private byte[] prf(String x) {
        byte[] k = masterKey;
        for (char bit : x.toCharArray()) {
            byte[][] prgOutput = prg(k);
            k = (bit == '0') ? prgOutput[0] : prgOutput[1]; // 根据路径选择左或右子树
        }
        return k;
    }

    /**
     * 委托密钥生成函数，根据范围谓词生成子树根密钥。
     *
     * @param start 范围起始值
     * @param end   范围结束值
     * @return 委托密钥列表，每个元素包含子树根路径和部分伪随机值
     */
    public List<NodeKey> delKey(int start, int end) {
        List<NodeKey> delegatedKeys = new ArrayList<>();

        class DelegateRange {
            void process(String nodePath, int[] range) {
                int nodeStart = range[0];
                int nodeEnd = range[1];

                if (start <= nodeStart && nodeEnd <= end) {
                    delegatedKeys.add(new NodeKey(nodePath, prf(nodePath)));
                } else if (nodeEnd < start || nodeStart > end) {
                    return;
                } else {
                    int mid = (nodeStart + nodeEnd) / 2;
                    process(nodePath + "0", new int[]{nodeStart, mid});
                    process(nodePath + "1", new int[]{mid + 1, nodeEnd});
                }
            }
        }

        new DelegateRange().process("", new int[]{0, (int) Math.pow(2, Integer.toBinaryString(end).length()) - 1});
        return delegatedKeys;
    }

    /**
     * 根据范围批量派生伪随机值。
     *
     * @param delegatedKeys 委托密钥列表
     * @param start         范围起始值
     * @param end           范围结束值
     * @return 范围内每个值对应的伪随机值
     */
    public Map<Integer, byte[]> deriveRange(List<NodeKey> delegatedKeys, int start, int end) {
        Map<Integer, byte[]> resultMap = new HashMap<>();
        for (int i = start; i <= end; i++) {
            String binaryI = Integer.toBinaryString(i);
            for (NodeKey nodeKey : delegatedKeys) {
                if (binaryI.startsWith(nodeKey.nodePath)) {
                    resultMap.put(i, prf(binaryI.substring(nodeKey.nodePath.length())));
                    break;
                }
            }
        }
        return resultMap;
    }

    /**
     * 生成隐私保护的STp。
     *
     * @param c            上传次数
     * @param elementHash  元素p的哈希值
     * @return 隐私保护的STp
     */
    public byte[] generateSTp(int c, byte[] elementHash) {
        return prf(Integer.toBinaryString(c) + bytesToHex(elementHash));
    }

    /**
     * 重建Tp历史记录。
     *
     * @param STp 隐私保护的STp
     * @param c   上传次数
     * @return Tp历史记录
     */
    public Map<Integer, byte[]> reconstructHistory(byte[] STp, int c) {
        Map<Integer, byte[]> history = new HashMap<>();
        for (int i = 0; i <= c; i++) {
            history.put(i, prf(Integer.toBinaryString(i) + bytesToHex(STp)));
        }
        return history;
    }

    /**
     * 节点密钥类，表示子树根路径及其对应的部分伪随机值。
     */
    static class NodeKey {
        String nodePath; // 子树根路径
        byte[] key; // 部分伪随机值

        public NodeKey(String nodePath, byte[] key) {
            this.nodePath = nodePath;
            this.key = key;
        }
    }

    /**
     * 将字节数组转换为十六进制字符串。
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static void main(String[] args) {
        int lambdaSec = 128;
        DPRF dprf = new DPRF(lambdaSec);

        int start = 0, end = 7;
        List<NodeKey> keys = dprf.delKey(start, end);

        // 生成范围派生
        Map<Integer, byte[]> derivedRange = dprf.deriveRange(keys, 0, 7);
        System.out.println("派生范围结果:");
        derivedRange.forEach((k, v) -> System.out.println(k + ": " + bytesToHex(v)));

        // 生成隐私保护的STp
        byte[] STp = dprf.generateSTp(7, "p".getBytes());
        System.out.println("\n生成的STp: " + bytesToHex(STp));

        // 重建历史记录
        Map<Integer, byte[]> history = dprf.reconstructHistory(STp, 7);
        System.out.println("\n历史记录重建:");
        history.forEach((k, v) -> System.out.println(k + ": " + bytesToHex(v)));
    }
}
