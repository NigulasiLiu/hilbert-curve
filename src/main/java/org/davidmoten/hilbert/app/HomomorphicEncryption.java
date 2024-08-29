package org.davidmoten.hilbert.app;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.BitSet;

public class HomomorphicEncryption {

    private int n;

    public HomomorphicEncryption(int securityParameter) {
        // 初始化公共参数n
        this.n = (int) Math.pow(2, securityParameter);
    }

    // Enc方法
    public int enc(int sk, int m) {
        return (sk + m) % n;
    }

    // Dec方法
    public int dec(int sk, int e) {
        return (e - sk + n) % n;
    }

    // Add方法
    public int add(int e1, int e2) {
        return (e1 + e2) % n;
    }

    // 生成随机一次性密钥
    public int generateSecretKey() {
        SecureRandom random = new SecureRandom();
        return random.nextInt(n);
    }

    // 将BitMap_Existence指定位置设置为1
    public void setBit(BitSet bitMap, int id) {
        bitMap.set(id);
    }

    // 处理 BitMap_Existence
    public int processBitMapExistence(BitSet bitMap) {
        // 将BitSet转换为一个整数
        long[] longArray = bitMap.toLongArray();
        BigInteger m = new BigInteger(1, BitSetToByteArray(bitMap));
        return m.mod(BigInteger.valueOf(n)).intValue();
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

    public static void main(String[] args) throws Exception {
        int securityParameter = 20; // 假设安全参数为20位
        HomomorphicEncryption he = new HomomorphicEncryption(securityParameter);

        int n = he.n;

        // 创建一个长度为2^20的 BitSet
        BitSet bitMap = new BitSet((int) Math.pow(2, 20));

        // 设置id1=123456并加密
        int id1 = 123456;
        he.setBit(bitMap, id1);
        int processedBitMap1 = he.processBitMapExistence(bitMap);
        int sk1 = he.generateSecretKey();
        int encryptedBitMap1 = he.enc(sk1, processedBitMap1);
        System.out.println("Encrypted BitMap_Existence for id1=123456: " + encryptedBitMap1);

        // 清空 bitMap 以设置 id2
        bitMap.clear();

        // 设置id2=123457并加密
        int id2 = 123457;
        he.setBit(bitMap, id2);
        int processedBitMap2 = he.processBitMapExistence(bitMap);
        int sk2 = he.generateSecretKey();
        int encryptedBitMap2 = he.enc(sk2, processedBitMap2);
        System.out.println("Encrypted BitMap_Existence for id2=123457: " + encryptedBitMap2);

        // 使用同态加法将两个密文相加
        int encryptedSum = he.add(encryptedBitMap1, encryptedBitMap2);
        int skSum = (sk1 + sk2) % n;
        int decryptedSum = he.dec(skSum, encryptedSum);
        System.out.println("Decrypted Sum: " + decryptedSum);

        // 检查 id1 和 id2 对应的位是否为1
        BitSet decryptedBitSet = BitSet.valueOf(BigInteger.valueOf(decryptedSum).toByteArray());
        System.out.println("Bit at id1=123456: " + decryptedBitSet.get(id1));
        System.out.println("Bit at id2=123457: " + decryptedBitSet.get(id2));

        // 如果1.成功, 现在以某种密文计算方式将 id1=123456 对应的比特位置置为0

        // 设计算法思路：
        // 1. 对于 id1 的位置，生成一个仅包含此位的 BitSet。
        BitSet id1BitSet = new BitSet((int) Math.pow(2, 20));
        id1BitSet.set(id1);

        // 2. 对此位置执行同态加法以产生反向操作（将1变为0）。
        int processedBitMapToZero = he.processBitMapExistence(id1BitSet);
        int skZero = he.generateSecretKey();
        int encryptedBitMapToZero = he.enc(skZero, n - processedBitMapToZero);

        // 3. 将此密文加到现有的加密总和中，从而将该位清零。
        int encryptedSumWithZero = he.add(encryptedSum, encryptedBitMapToZero);
        int skSumWithZero = (skSum + skZero) % n;
        int decryptedSumWithZero = he.dec(skSumWithZero, encryptedSumWithZero);

        // 检查 id1 是否被成功置为0
        BitSet finalDecryptedBitSet = BitSet.valueOf(BigInteger.valueOf(decryptedSumWithZero).toByteArray());
        System.out.println("Final Bit at id1=123456: " + finalDecryptedBitSet.get(id1));
        System.out.println("Final Bit at id2=123457: " + finalDecryptedBitSet.get(id2));
    }

}
