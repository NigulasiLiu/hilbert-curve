package org.davidmoten.hilbert.app;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class DSSE {

    private String KS; // 主密钥
    private int n; // 公共参数
    private Map<String, Integer> T; // 计数器表
    private Map<String, String> SDB; // 存储空间前缀的加密数据库
    private Map<String, String> KDB; // 存储关键字的加密数据库
    private KeyDerivationFunction kdf; // 用于派生密钥
    private HashFunctions hashFunctions; // 哈希函数类实例
    private HomomorphicEncryption homomorphicEncryption; // 同态加密类实例

    // 构造函数
    public DSSE(int securityParameter) throws Exception {
        this.KS = generateMasterKey(securityParameter);
        this.n = initializePublicParameter(securityParameter);
        this.T = new HashMap<>();
        this.SDB = new HashMap<>();
        this.KDB = new HashMap<>();
        this.kdf = new KeyDerivationFunction(new SecretKeySpec(KS.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        this.hashFunctions = new HashFunctions();
        this.homomorphicEncryption = new HomomorphicEncryption(securityParameter); // 初始化同态加密实例
    }

    // 生成主密钥
    private String generateMasterKey(int securityParameter) throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        byte[] keyBytes = new byte[securityParameter / 8];
        secureRandom.nextBytes(keyBytes);
        return bytesToHex(keyBytes);
    }

    // 初始化公共参数（此处假设为常量）
    private int initializePublicParameter(int securityParameter) {
        return securityParameter;
    }

    // 将字节数组转换为十六进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // 更新操作
    public void update(String[] pSet, String[] W, String B, int Cc, String up) throws Exception {
        List<String> P = preCode(pSet);

        for (String p : P) {
            String[] keys = F_K_sigma(KS, p);
            String Kp = keys[0];
            String KpPrime = keys[1];
            int c = T.getOrDefault(p, -1);

            String Tp_c_plus_1 = kdf.Derive(Kp, Cc + 1);
            T.put(p, c + 1);

            String UTp_c_plus_1 = hashFunctions.H1(Kp, Tp_c_plus_1);
            String skp_c1 = hashFunctions.H2(Kp, c + 1);
            int ep_c1 = homomorphicEncryption.enc(homomorphicEncryption.generateSecretKey(), Integer.parseInt(B));

            SDB.put(UTp_c_plus_1, String.valueOf(ep_c1));
        }

        for (String w : W) {
            String[] keys = F_K_sigma(KS, w);
            String Kw = keys[0];
            String KwPrime = keys[1];
            int c = T.getOrDefault(w, -1);

            String Tw_c1 = kdf.Derive(Kw, Cc + 1);
            T.put(w, c + 1);

            String UTw_c1 = hashFunctions.H1(Kw, Tw_c1);
            String skw_c1 = hashFunctions.H2(Kw, c + 1);
            int ew_c1 = homomorphicEncryption.enc(homomorphicEncryption.generateSecretKey(), Integer.parseInt(B));

            KDB.put(UTw_c1, String.valueOf(ew_c1));
        }
    }

    // 搜索操作
    public void search(String R, String[] WQ) throws Exception {
        List<String> BPC = preCover(R);

        for (String w : WQ) {
            String[] keys = F_K_sigma(KS, w);
            String Kw = keys[0];
            String KwPrime = keys[1];
            int c = getCounter(w);
            if (c == -1) {
                return;
            }

            String STw = String.valueOf(kdf.DelKey(Kw, String.valueOf(c)));
            sendToServer(KwPrime, STw, c);
        }

        for (String p : BPC) {
            String[] keys = F_K_sigma(KS, p);
            String Kp = keys[0];
            String KpPrime = keys[1];
            int c = getCounter(p);
            if (c == -1) {
                return;
            }

            String STp = String.valueOf(kdf.DelKey(Kp, String.valueOf(c)));
            sendToServer(KpPrime, STp, c);
        }

        processServerResponse();
    }

    private List<String> preCode(String[] pSet) {
        return Arrays.asList(pSet);
    }

    private List<String> preCover(String R) {
        return Arrays.asList(R);
    }

    private String[] F_K_sigma(String KS, String input) throws Exception {
        hashFunctions.hmacSHA256.init(new SecretKeySpec(KS.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] result = hashFunctions.hmacSHA256.doFinal(input.getBytes(StandardCharsets.UTF_8));
        String K = Base64.getEncoder().encodeToString(Arrays.copyOfRange(result, 0, result.length / 2));
        String KPrime = Base64.getEncoder().encodeToString(Arrays.copyOfRange(result, result.length / 2, result.length));
        return new String[]{K, KPrime};
    }

    private int getCounter(String input) {
        return T.getOrDefault(input, -1);
    }

    private void sendToServer(String key, String token, int counter) {
        System.out.println("Sending to server: Key = " + key + ", Token = " + token + ", Counter = " + counter);
    }

    private void processServerResponse() {
        System.out.println("Processing server response...");
    }

    public static void main(String[] args) throws Exception {
        // 初始化DSSE系统
        int securityParameter = 128;
        DSSE dsse = new DSSE(securityParameter);

        // 执行一次更新操作
        String[] pSet = {"p1", "p2"};
        String[] W = {"w1", "w2", "w3"};
        String B = "block_data";
        int Cc = 100;
        dsse.update(pSet, W, B, Cc, "ins");

        // 执行一次搜索操作
        String R = "range1";
        String[] WQ = {"w1", "w2", "w3"};
        dsse.search(R, WQ);
    }
}

