package org.davidmoten.hilbert.app;

import java.security.SecureRandom;

public class HomomorphicEncryption {

    private int n;

    public HomomorphicEncryption(int securityParameter) {
        // 初始化公共参数n
        this.n = (int) Math.pow(2, securityParameter);
    }

    // Init方法
    public int init() {
        return n;
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

    public static void main(String[] args) {
        int securityParameter = 10; // 假设安全参数为10位
        HomomorphicEncryption he = new HomomorphicEncryption(securityParameter);

        int n = he.init();
        int m1 = 5;
        int m2 = 7;

        int sk1 = he.generateSecretKey();
        int sk2 = he.generateSecretKey();

        int e1 = he.enc(sk1, m1);
        int e2 = he.enc(sk2, m2);

        int eSum = he.add(e1, e2);
        int skSum = (sk1 + sk2) % n;
        int mSum = he.dec(skSum, eSum);

        System.out.println("Original messages: m1 = " + m1 + ", m2 = " + m2);
        System.out.println("Encrypted messages: e1 = " + e1 + ", e2 = " + e2);
        System.out.println("Decrypted sum: m1 + m2 = " + mSum);
    }
}
