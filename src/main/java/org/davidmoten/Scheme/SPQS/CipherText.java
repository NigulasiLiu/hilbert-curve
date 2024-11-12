package org.davidmoten.Scheme.SPQS;

import java.math.BigInteger;

// 创建 CipherText 类来替代 Object[]
public class CipherText {
    private final byte[] C;
    private final BigInteger ea;
    private final BigInteger eb;

    public CipherText(byte[] C, BigInteger ea, BigInteger eb) {
        this.C = C;
        this.ea = ea;
        this.eb = eb;
    }

    public byte[] getC() {
        return C;
    }

    public BigInteger getEa() {
        return ea;
    }

    public BigInteger getEb() {
        return eb;
    }
}
