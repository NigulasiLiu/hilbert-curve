package org.davidmoten.B;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class B {
    private byte[] bits;
    private final int size;

    // 构造函数，初始化长度为size的bit数组
    public B(int size) {
        this.size = size;
        this.bits = new byte[(size + 7) / 8]; // 每8位存储在一个byte中
    }

    // 设置第i位为1
    public void setBit(int i) {
        if (i >= size || i < 0) throw new IndexOutOfBoundsException();
        bits[i / 8] |= (1 << (i % 8));
    }

    // 设置第i位为0
    public void clearBit(int i) {
        if (i >= size || i < 0) throw new IndexOutOfBoundsException();
        bits[i / 8] &= ~(1 << (i % 8));
    }

    // 获取第i位的值（1或0）
    public int getBit(int i) {
        if (i >= size || i < 0) throw new IndexOutOfBoundsException();
        return (bits[i / 8] >> (i % 8)) & 1;
    }

    // 与操作：this & other
    public void and(B other) {
        if (this.size != other.size) throw new IllegalArgumentException("Sizes must match");
        for (int i = 0; i < bits.length; i++) {
            this.bits[i] &= other.bits[i];
        }
    }

    // 接受 byte[] 参数的与操作，并返回结果的 BigInteger
//    public BigInteger and(byte[] other) {
//        if (other.length != bits.length) throw new IllegalArgumentException("Sizes must match");
//        byte[] result = new byte[bits.length];
//        for (int i = 0; i < bits.length; i++) {
//            result[i] = (byte) (this.bits[i] & other[i]);
//        }
//        return new BigInteger(1, result);
//    }

    // 或操作：this | other
    public void or(B other) {
        if (this.size != other.size) throw new IllegalArgumentException("Sizes must match");
        for (int i = 0; i < bits.length; i++) {
            this.bits[i] |= other.bits[i];
        }
    }

    // 接受 byte[] 参数的或操作，并返回结果的 BigInteger
//    public BigInteger or(byte[] other) {
//        if (other.length != bits.length) throw new IllegalArgumentException("Sizes must match");
//        byte[] result = new byte[bits.length];
//        for (int i = 0; i < bits.length; i++) {
//            result[i] = (byte) (this.bits[i] | other[i]);
//        }
//        return new BigInteger(1, result);
//    }

    // 异或操作：this ^ other
    public void xor(B other) {
        if (this.size != other.size) throw new IllegalArgumentException("Sizes must match");
        for (int i = 0; i < bits.length; i++) {
            this.bits[i] ^= other.bits[i];
        }
    }

    // 接受 byte[] 参数的异或操作，并返回结果的 BigInteger
//    public BigInteger xor(byte[] other) {
//        if (other.length != bits.length) throw new IllegalArgumentException("Sizes must match");
//        byte[] result = new byte[bits.length];
//        for (int i = 0; i < bits.length; i++) {
//            result[i] = (byte) (this.bits[i] ^ other[i]);
//        }
//        return new BigInteger(1, result);
//    }
    // 异或操作：this ^ other
    public BigInteger xor(byte[] other) {
        int maxLength = Math.max(this.bits.length, other.length);

        // 扩展本地 bits 和 other 到相同的长度
        byte[] extendedBits = extendByteArray(this.bits, maxLength);
        byte[] extendedOther = extendByteArray(other, maxLength);

        byte[] result = new byte[maxLength];
        for (int i = 0; i < maxLength; i++) {
            result[i] = (byte) (extendedBits[i] ^ extendedOther[i]);
        }
        return new BigInteger(1, result); // 返回正数的 BigInteger
    }
    // 与操作：this & other
    public BigInteger and(byte[] other) {
        int maxLength = Math.max(this.bits.length, other.length);

        // 扩展本地 bits 和 other 到相同的长度
        byte[] extendedBits = extendByteArray(this.bits, maxLength);
        byte[] extendedOther = extendByteArray(other, maxLength);

        byte[] result = new byte[maxLength];
        for (int i = 0; i < maxLength; i++) {
            result[i] = (byte) (extendedBits[i] & extendedOther[i]);
        }
        return new BigInteger(1, result); // 返回正数的 BigInteger
    }

    // 或操作：this | other
    public BigInteger or(byte[] other) {
        int maxLength = Math.max(this.bits.length, other.length);

        // 扩展本地 bits 和 other 到相同的长度
        byte[] extendedBits = extendByteArray(this.bits, maxLength);
        byte[] extendedOther = extendByteArray(other, maxLength);

        byte[] result = new byte[maxLength];
        for (int i = 0; i < maxLength; i++) {
            result[i] = (byte) (extendedBits[i] | extendedOther[i]);
        }
        return new BigInteger(1, result); // 返回正数的 BigInteger
    }
    // 辅助方法：扩展 byte[] 数组到指定长度，前面补 0
    private byte[] extendByteArray(byte[] original, int newLength) {
        byte[] extended = new byte[newLength];
        System.arraycopy(original, 0, extended, newLength - original.length, original.length);
        return extended;
    }

    // 取反操作：~this
    public void not() {
        for (int i = 0; i < bits.length; i++) {
            bits[i] = (byte) ~bits[i];
        }
    }

    // 返回取反后的 BigInteger 结果
    public BigInteger notToBigInteger() {
        byte[] result = new byte[bits.length];
        for (int i = 0; i < bits.length; i++) {
            result[i] = (byte) ~bits[i];
        }
        return new BigInteger(1, result);
    }

    // 查找所有1所在的位置的下标
    public List<Integer> findIndexofOne() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (getBit(i) == 1) {
                indices.add(i);
            }
        }
        return indices;
    }

    // 将 byte[] 转换为 BigInteger
    public BigInteger toBigInteger() {
        return new BigInteger(1, bits); // 参数1表示非负数
    }

    // 将 BigInteger 转换为 byte[] 并存储在当前实例中
    public void fromBigInteger(BigInteger value) {
        byte[] byteArray = value.toByteArray();
        // 确保 byte[] 长度与 bits 匹配
        if (byteArray.length > bits.length) {
            throw new IllegalArgumentException("BigInteger value too large to fit in BitSet");
        }
        // 从右对齐拷贝值
        System.arraycopy(byteArray, 0, bits, bits.length - byteArray.length, byteArray.length);
    }

    // 打印出bit的内容，便于查看
    public void printBits() {
        for (int i = 0; i < size; i++) {
            System.out.print(getBit(i));
            if (i % 8 == 7) System.out.print(" "); // 每8位分隔
        }
        System.out.println();
    }

    // 返回 bit 数组
    public byte[] getBytes() {
        return bits;
    }

    public static void main(String[] args) {
        // 创建一个长度为 1<<20 的 B 实例
        B bitset = new B(1 << 20);

        // 设置第 0 位和第 10 位为 1
        bitset.setBit(0);
        bitset.setBit(10);

        // 打印当前 bit 串
        bitset.printBits();

        // 进行与操作
        byte[] otherBits = new byte[(1 << 20) / 8];
        otherBits[0] = (byte) 0xFF; // 设置部分字节为 1
        BigInteger andResult = bitset.and(otherBits);
        System.out.println("And Result: " + andResult);

        // 进行或操作
        BigInteger orResult = bitset.or(otherBits);
        System.out.println("Or Result: " + orResult);

        // 进行异或操作
        BigInteger xorResult = bitset.xor(otherBits);
        System.out.println("Xor Result: " + xorResult);

        // 进行取反操作
        BigInteger notResult = bitset.notToBigInteger();
        System.out.println("Not Result: " + notResult);
    }
}
