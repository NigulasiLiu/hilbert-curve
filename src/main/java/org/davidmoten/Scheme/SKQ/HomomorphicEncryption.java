package org.davidmoten.Scheme.SKQ;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class HomomorphicEncryption {

    private BigInteger n;

    public BigInteger getN() {
        return n;
    }

    public HomomorphicEncryption(int max_files) {
        // 初始化公共参数n
        this.n = BigInteger.valueOf(2).pow(max_files);
    }
    // Enc方法
    public BigInteger enc(BigInteger sk, BigInteger m) {
        return sk.add(m).mod(n);
    }
    // Dec方法
    public BigInteger dec(BigInteger sk, BigInteger e) {
        return e.subtract(sk).add(n).mod(n);
    }

    // Add方法
    public BigInteger add(BigInteger e1, BigInteger e2) {
        return e1.add(e2).mod(n);
    }

    // 将 BitSet 转换为字节数组
    private byte[] BitSetToByteArray(BitSet bitSet) {
        byte[] bytes = new byte[(bitSet.length() + 7) / 8];
        for (int i = 0; i < bitSet.length(); i++) {
            if (bitSet.get(i)) {
                bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
            }
        }
        return bytes;
    }

    // 生成随机一次性密钥
    public BigInteger generateSecretKey() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(n.bitLength(), random).mod(n);
    }

    // 将BitMap_Existence指定位置设置为1
    public void setBit(BitSet bitMap, int id) {
        bitMap.set(id);
    }

    // 处理 BitMap_Existence
    public BigInteger processBitMapExistence(BitSet bitMap) {
        return new BigInteger(1, BitSetToByteArray(bitMap));//所有可能的位图数量为2^20+2^19+...=2^21-1 << 2^128，所以不会出现取模后冲突的情况
    }

    // 将 BigInteger 转换为 BitSet
    public BitSet BigIntegerToBitSet(BigInteger bigInt) {
        byte[] byteArray = bigInt.toByteArray();
        BitSet bitSet = BitSet.valueOf(byteArray);
        return bitSet;
    }

    public static void findIndexesOfOne(BigInteger number) {
        // 收集所有位索引
        List<Integer> indexes = new ArrayList<>();
        int index = 0;

        // 遍历所有可能的位
        while (number.signum() != 0) {
            // 检查当前最低位是否为1
            if (number.and(BigInteger.ONE).equals(BigInteger.ONE)) {
                // 收集当前位的索引
                indexes.add(index);
            }

            // 右移一位处理下一位
            number = number.shiftRight(1);
            index++;
        }

        // 打印结果
        if (indexes.isEmpty()) {
            System.out.println("没有找到1位。");
        } else {
            System.out.println("位图中1的位置索引为：");
            for (Integer idx : indexes) {
                System.out.println("索引: " + idx);
            }
        }
    }

//    public static void main(String[] args) throws Exception {
//        int maxfiles = 1 << 20;
//        HomomorphicEncryption he = new HomomorphicEncryption(maxfiles);
//
//        BigInteger n = he.n;
//
//        // 创建一个长度为2^20的 BitSet
//        BitSet bitMap = new BitSet(maxfiles);
//
//        int id1 = 8;
//        int id2 = 1025;
//        he.setBit(bitMap, id1);
//        BigInteger processedBitMap1 = he.processBitMapExistence(bitMap);
//        BigInteger sk1 = he.generateSecretKey();
////        if(sk1.compareTo(n)>0){
////            System.out.println("sk1 > n");
////        }
////        else{
////            System.out.println("sk1 < n");
////        }
////        if(processedBitMap1.compareTo(n)>0){
////            System.out.println("processedBitMap1 > n");
////        }
////        else{
////            System.out.println("processedBitMap1 < n");
////        }
//
//        BigInteger encryptedBitMap1 = he.enc(sk1, processedBitMap1);
////        if(encryptedBitMap1.compareTo(sk1)>0){
////            System.out.println("encryptedBitMap1 > sk1");
////        }
////        else{
////            System.out.println("encryptedBitMap1 < sk1");
////        }
//        // 清空 bitMap 以设置 id2
//        bitMap.clear();
//        System.out.println("BitMap Clear");
//        he.setBit(bitMap, id2);
//        BigInteger processedBitMap2 = he.processBitMapExistence(bitMap);
//        BigInteger sk2 = he.generateSecretKey();
//        BigInteger encryptedBitMap2 = he.enc(sk2, processedBitMap2);
//
//        // 使用同态加法将两个密文相加
//        BigInteger encryptedSum = he.add(encryptedBitMap1, encryptedBitMap2);
//        BigInteger skSum = sk1.add(sk2).mod(n);
//        BigInteger decryptedSum = he.dec(skSum, encryptedSum);
//        System.out.println("Decrypted Sum: " + decryptedSum.toString(2));
//        // 打印所有设置为1的位的索引
//        System.out.println("当前存在文件: ");
//        findIndexesOfOne(decryptedSum);
//        // 如果1.成功, 现在以某种密文计算方式将 id1=123456 对应的比特位置置为0
//
//        // 删除文档id1,设计算法思路：
//        // 1. 对于 id1 的位置，生成一个仅包含此位的 BitSet。
//        BitSet id1BitSet = new BitSet(maxfiles);
//        System.out.println("删除文件: "+id1);
//        id1BitSet.set(id1);
//
//        // 2. 对此位置执行同态加法以产生反向操作（将1变为0）。
//        BigInteger processedBitMapToZero = he.processBitMapExistence(id1BitSet);
//        BigInteger skZero = he.generateSecretKey();
//        BigInteger encryptedBitMapToZero = he.enc(skZero, n.subtract(processedBitMapToZero));
//
//        // 3. 将此密文加到现有的加密总和中，从而将该位清零。
//        BigInteger encryptedSumWithZero = he.add(encryptedSum, encryptedBitMapToZero);
//        BigInteger skSumWithZero = skSum.add(skZero).mod(n);
//        BigInteger decryptedSumWithZero = he.dec(skSumWithZero, encryptedSumWithZero);
//        System.out.println("当前存在文件: ");
//        findIndexesOfOne(decryptedSumWithZero);
//
//    }
}
