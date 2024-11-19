package org.davidmoten.Scheme.SPQS;

public class BitMapAsInteger {
    private long value;  // 使用 long 存储位图值

    public BitMapAsInteger() {
        this.value = 0L;
    }

    // 设置某个位为1
    public void setBit(int index) {
        value |= (1L << index);
    }

    // 清除某个位（设置为0）
    public void clearBit(int index) {
        value &= ~(1L << index);
    }

    // 获取某个位的值
    public boolean getBit(int index) {
        return (value & (1L << index)) != 0;
    }

    // 逻辑与操作
    public void and(long other) {
        value &= other;
    }

    // 逻辑或操作
    public void or(long other) {
        value |= other;
    }

    // 逻辑异或操作
    public void xor(long other) {
        value ^= other;
    }

    // 取反操作
    public void not() {
        value = ~value & ((1L << 64) - 1);  // 确保只取低 64 位
    }

    // 设置某一位为1，然后取反
    public void setBitAndInvert(int index) {
        setBit(index);  // 设置某位为1
        not();          // 取反整个值
    }

    // 取模操作
    public long mod(long n) {
        return value % n;
    }

    @Override
    public String toString() {
        return Long.toBinaryString(value);
    }
}
