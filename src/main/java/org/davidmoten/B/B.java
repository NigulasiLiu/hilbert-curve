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

    // 或操作：this | other
    public void or(B other) {
        if (this.size != other.size) throw new IllegalArgumentException("Sizes must match");
        for (int i = 0; i < bits.length; i++) {
            this.bits[i] |= other.bits[i];
        }
    }

    // 异或操作：this ^ other
    public void xor(B other) {
        if (this.size != other.size) throw new IllegalArgumentException("Sizes must match");
        for (int i = 0; i < bits.length; i++) {
            this.bits[i] ^= other.bits[i];
        }
    }

    // 取反操作：~this
    public void not() {
        for (int i = 0; i < bits.length; i++) {
            bits[i] = (byte) ~bits[i];
        }
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
//        bitset.printBits();

        // 查找所有1的下标
        List<Integer> indices = bitset.findIndexofOne();
        System.out.println("1所在的位置: " + indices);

        // 将 bitset 转换为 BigInteger
        BigInteger bigIntValue = bitset.toBigInteger();
//        System.out.println("BigInteger 值: " + bigIntValue);

        // 创建一个新的 B 实例并从 BigInteger 恢复
        B newBitset = new B(1 << 20);
        newBitset.fromBigInteger(bigIntValue);

        // 打印从 BigInteger 恢复的 bit 串
//        newBitset.printBits();

        // 再次查找所有1的下标
        indices = newBitset.findIndexofOne();
        System.out.println("恢复后1所在的位置: " + indices);
    }

}
