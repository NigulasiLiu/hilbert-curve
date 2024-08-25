package org.davidmoten.hilbert.app;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;

public class KeyDerivationFunction {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final Key masterKey;

    // 构造函数，接受主密钥
    public KeyDerivationFunction(Key masterKey) {
        this.masterKey = masterKey;
    }

    // DelKey 方法：生成委托密钥 KC
    public Key DelKey(Key masterKey, String rangePredicate) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(masterKey);
        byte[] delegatedKeyBytes = mac.doFinal(rangePredicate.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(delegatedKeyBytes, HMAC_SHA256);
    }


    // DelKey 方法的重载：生成委托密钥 KC（String, String）
    public Key DelKey(String masterKeyStr, String rangePredicate) throws Exception {
        // 将字符串转换为密钥对象
        Key masterKey = new SecretKeySpec(masterKeyStr.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        // 调用原始 DelKey 方法
        return DelKey(masterKey, rangePredicate);
    }


    // Derive 方法：根据委托密钥 KC 和输入 v 进行衍生计算
    public String Derive(Key delegatedKey, String input) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(delegatedKey);
        byte[] derivedBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(derivedBytes);
    }

    // 重载 Derive 方法：接受 String 和 int 类型输入
    public String Derive(String key, int counter) throws Exception {
        Key delegatedKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        return Derive(delegatedKey, Integer.toString(counter));
    }

    // 检查输入 v 是否属于范围 C 的辅助方法
    public boolean isInRange(String input, String rangePredicate) throws Exception {
        Key delegatedKey = DelKey(masterKey, rangePredicate);
        String derivedValue = Derive(delegatedKey, input);
        return Arrays.equals(derivedValue.getBytes(StandardCharsets.UTF_8), someExpectedValue(rangePredicate)); // 需要定义 someExpectedValue 方法
    }

    // 示例方法，用于根据 rangePredicate 返回期望的 derivedValue
    private byte[] someExpectedValue(String rangePredicate) {
        return rangePredicate.getBytes(StandardCharsets.UTF_8); // 示例实现，实际应用中应根据需求修改
    }

    public static void main(String[] args) throws Exception {
        // 示例主密钥
        byte[] keyBytes = "SuperSecretKeyForDPRF".getBytes(StandardCharsets.UTF_8);
        Key masterKey = new SecretKeySpec(keyBytes, HMAC_SHA256);

        // 创建 KeyDerivationFunction 实例
        KeyDerivationFunction kdf = new KeyDerivationFunction(masterKey);

        // 委托密钥的生成
        String rangePredicate = "range123";
        Key delegatedKey = kdf.DelKey(masterKey, rangePredicate);
        System.out.println("Delegated Key: " + Arrays.toString(delegatedKey.getEncoded()));

        // 基于委托密钥的衍生
        String input = "inputValue";
        String derivedValue = kdf.Derive(delegatedKey, input);
        System.out.println("Derived Value: " + derivedValue);

        // 使用String和int类型的派生
        String result = kdf.Derive("test_key", 123);
        System.out.println("Derived Value from String and int: " + result);

        // 检查输入 v 是否在范围 C 内
        boolean inRange = kdf.isInRange(input, rangePredicate);
        System.out.println("Is in range: " + inRange);
    }
}
