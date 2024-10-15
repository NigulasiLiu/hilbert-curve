package org.davidmoten.Scheme.TDSC2023;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HashFunctions {

    Mac hmacSHA256;

    public HashFunctions() throws Exception {
        this.hmacSHA256 = Mac.getInstance("HmacSHA256");
    }

    /**
     * H1函数，用于生成哈希值
     *
     * @param key  HMAC的密钥
     * @param data 输入数据
     * @return 返回生成的哈希值
     * @throws Exception 抛出异常
     */
    public String H1(String key, String data) throws Exception {
        hmacSHA256.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] result = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(result);
    }

    /**
     * H2函数，用于生成哈希值
     *
     * @param key     HMAC的密钥
     * @param counter 计数器
     * @return 返回生成的哈希值
     * @throws Exception 抛出异常
     */
//    public String H2(String key, int counter) throws Exception {
//        hmacSHA256.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
//        byte[] result = hmacSHA256.doFinal(Integer.toString(counter).getBytes(StandardCharsets.UTF_8));
//        return Base64.getEncoder().encodeToString(result);
//    }
    public BigInteger H2(String key, int counter) throws Exception {
        hmacSHA256.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] result = hmacSHA256.doFinal(Integer.toString(counter).getBytes(StandardCharsets.UTF_8));
        // 将 HMAC-SHA256 结果转换为 BigInteger
        return new BigInteger(1, result);
    }

}
