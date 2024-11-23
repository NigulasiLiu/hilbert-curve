package org.davidmoten.Scheme.TDSC2023.DPRF;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class DPRF_bak {
    private final int lambdaSec; // 安全参数
    private final byte[] masterKey; // 主密钥

    /**
     * 构造函数，初始化安全参数和主密钥。
     *
     * @param lambdaSec 安全参数（密钥长度，单位：字节）
     */
    public DPRF_bak(int lambdaSec) {
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

        /**
         * 递归处理当前子树的范围。
         *
         * @param nodePath 当前节点路径
         * @param range    当前子树覆盖的范围
         */
        class DelegateRange {
            void process(String nodePath, int[] range) {
                int nodeStart = range[0];
                int nodeEnd = range[1];

                if (start <= nodeStart && nodeEnd <= end) {
                    // 当前范围完全在目标范围内
                    delegatedKeys.add(new NodeKey(nodePath, prf(nodePath)));
                } else if (nodeEnd < start || nodeStart > end) {
                    // 当前范围完全在目标范围之外，跳过处理
                    return;
                } else {
                    // 分割当前范围并递归处理左右子树
                    int mid = (nodeStart + nodeEnd) / 2;
                    process(nodePath + "0", new int[]{nodeStart, mid});
                    process(nodePath + "1", new int[]{mid + 1, nodeEnd});
                }
            }
        }

        // 从根节点开始覆盖整个输入域
        new DelegateRange().process("", new int[]{0, (int) Math.pow(2, Integer.toBinaryString(end).length()) - 1});
        return delegatedKeys;
    }

    /**
     * 从委托密钥派生输入值的伪随机值。
     *
     * @param delegatedKeys 委托密钥列表
     * @param v             输入值
     * @return 对应的伪随机值
     */
    public byte[] derive(List<NodeKey> delegatedKeys, int v) {
        String binaryV = Integer.toBinaryString(v); // 将输入值转换为二进制路径
        for (NodeKey nodeKey : delegatedKeys) {
            if (binaryV.startsWith(nodeKey.nodePath)) {
                // 使用委托的子树根密钥派生伪随机值
                return prf(binaryV.substring(nodeKey.nodePath.length()));
            }
        }
        throw new IllegalArgumentException("输入值不在委托范围内");
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

    public static void main(String[] args) {
        int lambdaSec = 16; // 安全参数（字节）
        DPRF_bak dprfBak = new DPRF_bak(lambdaSec);

        // 定义范围谓词 [start, end]
        int start = 2;
        int end = 7;

        // 生成委托密钥
        List<NodeKey> delegatedKeys = dprfBak.delKey(start, end);
        System.out.println("委托密钥生成成功，内容如下:");
        for (NodeKey nodeKey : delegatedKeys) {
            System.out.println("路径: " + nodeKey.nodePath + "，部分伪随机值: " + bytesToHex(nodeKey.key));
        }
        System.out.println();

        // 测试输入在范围内的情况
        System.out.println("==== 测试输入在范围内的情况 ====");
        for (int v = start; v <= end; v++) {
            byte[] prfValue = dprfBak.derive(delegatedKeys, v);
            System.out.println("输入值 " + v + " 的伪随机值: " + bytesToHex(prfValue));
        }
        System.out.println();

        // 测试输入不在范围内的情况
        System.out.println("==== 测试输入不在范围内的情况 ====");
        int[] testInputs = {0, 1, 8, 10}; // 超出范围的测试值
        for (int v : testInputs) {
            try {
                byte[] prfValue = dprfBak.derive(delegatedKeys, v);
                System.out.println("输入值 " + v + " 的伪随机值: " + bytesToHex(prfValue));
            } catch (IllegalArgumentException e) {
                System.out.println("输入值 " + v + " 不在委托范围内，错误信息: " + e.getMessage());
            }
        }
        System.out.println();

        // 测试边界条件
        System.out.println("==== 测试边界条件 ====");
        int lowerBoundary = start - 1;
        int upperBoundary = end + 1;
        try {
            byte[] lowerValue = dprfBak.derive(delegatedKeys, lowerBoundary);
            System.out.println("下界输入值 " + lowerBoundary + " 的伪随机值: " + bytesToHex(lowerValue));
        } catch (IllegalArgumentException e) {
            System.out.println("下界输入值 " + lowerBoundary + " 不在范围内，错误信息: " + e.getMessage());
        }

        try {
            byte[] upperValue = dprfBak.derive(delegatedKeys, upperBoundary);
            System.out.println("上界输入值 " + upperBoundary + " 的伪随机值: " + bytesToHex(upperValue));
        } catch (IllegalArgumentException e) {
            System.out.println("上界输入值 " + upperBoundary + " 不在范围内，错误信息: " + e.getMessage());
        }
        System.out.println();

        // 打印委托密钥范围
        System.out.println("==== 委托密钥覆盖范围 ====");
        System.out.println("此委托密钥覆盖范围: [" + start + ", " + end + "]");
    }

    /**
     * 将字节数组转换为十六进制字符串。
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
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
}
