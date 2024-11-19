package org.davidmoten.Scheme.SPQS;

import java.util.Arrays;

public class ClientState {
    private int c0;       // 起始计数器
    private int c;        // 当前计数器
    private byte[] Rc;    // 随机数 Rc+1

    // 构造函数
    public ClientState(int c0, int c, byte[] Rc) {
        this.c0 = c0;
        this.c = c;
        this.Rc = Rc;
    }

    // Getter 和 Setter
    public int getC0() {
        return c0;
    }

    public void setC0(int c0) {
        this.c0 = c0;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    public byte[] getRc() {
        return Rc;
    }

    public void setRc(byte[] Rc) {
        this.Rc = Rc;
    }

    @Override
    public String toString() {
        return "ClientState{" +
                "c0=" + c0 +
                ", c=" + c +
                ", Rc=" + Arrays.toString(Rc) +
                '}';
    }
}
