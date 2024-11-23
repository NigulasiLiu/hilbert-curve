package org.davidmoten.Scheme.RSKQ;

import java.math.BigInteger;

// 创建 CipherTextBytes 类来替代 Object[]
public class CipherText {
    private final BigInteger C;
    private final BigInteger ea;
    private final BigInteger eb;

    public CipherText(BigInteger C, BigInteger ea, BigInteger eb) {
        this.C = C;
        this.ea = ea;
        this.eb = eb;
    }

    public BigInteger getC() {
        return C;
    }

    public BigInteger getEa() {
        return ea;
    }

    public BigInteger getEb() {
        return eb;
    }
}
