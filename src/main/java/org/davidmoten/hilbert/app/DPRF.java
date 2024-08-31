package org.davidmoten.hilbert.app;//package org.davidmoten.hilbert.app;
//
//import javax.crypto.Mac;
//import javax.crypto.spec.SecretKeySpec;
//import java.nio.charset.StandardCharsets;
//import java.security.Key;
//import java.util.Base64;
//
//public class DPRF {
//
//    private static final String HMAC_SHA256 = "HmacSHA256";
//    private final Key masterKey;
//    private final int order;
//    private final int c_max;  // 定义最大范围 c_max
//
//    // 构造函数，接受主密钥、树的深度（order）和最大范围 c_max
//    public DPRF(Key masterKey, int order, int c_max) {
//        this.masterKey = masterKey;
//        this.order = order;
//        this.c_max = c_max;
//    }
//
//    // GGM PRF 的 DelKey 实现
//    public Key DelKey(Key masterKey, String rangePredicate) throws Exception {
//        // 从根节点开始生成密钥
//        Key currentKey = masterKey;
//        for (char bit : rangePredicate.toCharArray()) {
//            currentKey = deriveChildKey(currentKey, bit);
//        }
//        return currentKey;
//    }
//
//    // GGM PRF 的 Derive 实现
//    public String Derive(Key delegatedKey, int counter) throws Exception {
//        // 检查 counter 是否在委托密钥允许的范围内
//        if (!isInRange(counter)) {
//            throw new IllegalArgumentException("Counter out of range: " + counter + " not in [0, " + c_max + "]");
//        }
//
//        String input = Integer.toString(counter);
//        Mac mac = Mac.getInstance(HMAC_SHA256);
//        mac.init(delegatedKey);
//        byte[] derivedBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
//        return Base64.getEncoder().encodeToString(derivedBytes);
//    }
//
//    // 检查 counter 是否在范围 [0, c_max] 内
//    private boolean isInRange(int counter) {
//        return counter >= 0 && counter <= c_max;
//    }
//
//    // 根据父密钥和当前位派生子密钥
//    private Key deriveChildKey(Key parentKey, char bit) throws Exception {
//        Mac mac = Mac.getInstance(HMAC_SHA256);
//        mac.init(parentKey);
//        byte[] childKeyBytes = mac.doFinal(new byte[]{(byte) bit});
//        return new SecretKeySpec(childKeyBytes, HMAC_SHA256);
//    }
//
//    public static void main(String[] args) throws Exception {
//        int order = 6;  // 假设 order 为 6
//        int c_max = 200;  // 自定义最大范围 c_max 为 200
//
//        // 示例主密钥
//        byte[] keyBytes = "SuperSecretKeyForDPRF".getBytes(StandardCharsets.UTF_8);
//        Key masterKey = new SecretKeySpec(keyBytes, HMAC_SHA256);
//
//        // 创建 DPRF 实例，设置最大范围 c_max
//        DPRF dprf = new DPRF(masterKey, order, c_max);
//
//        // 生成委托密钥
//        int c =150;
//        String rangePredicate = Integer.toBinaryString(c);  // 计数器值的二进制表示
//        Key delegatedKey = dprf.DelKey(masterKey, rangePredicate);
//        System.out.println("Delegated Key: " + Base64.getEncoder().encodeToString(delegatedKey.getEncoded()));
//
//        // 使用 Derive 方法生成伪随机值
//        try {
//            // 示例输入 counter 在范围内
//            String derivedValue = dprf.Derive(delegatedKey, 150);
//            System.out.println("Derived Value (150): " + derivedValue);
//
//            // 示例输入 counter 超出范围
//            derivedValue = dprf.Derive(delegatedKey, 250);
//            System.out.println("Derived Value (250): " + derivedValue);
//        } catch (IllegalArgumentException e) {
//            System.out.println("Error: " + e.getMessage());
//        }
//    }
//}
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DPRF {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final Key masterKey;
    private final int order;
    private final int c_max;  // 定义最大范围 c_max

    // 构造函数，接受主密钥、树的深度（order）和最大范围 c_max
    public DPRF(Key masterKey, int order, int c_max) {
        this.masterKey = masterKey;
        this.order = order;
        this.c_max = c_max;
    }

    public Key DelKey(Key masterKey, int Cc) throws Exception {
        // 将计数器值 Cc 转换为二进制表示
        String rangePredicate = Integer.toBinaryString(Cc);

        // 生成委托密钥 STw
        Key currentKey = masterKey;
        for (char bit : rangePredicate.toCharArray()) {
            currentKey = deriveChildKey(currentKey, bit);
        }
        return currentKey;
    }

    // GGM PRF 的 Derive 实现
    public String Derive(Key delegatedKey, int counter) throws Exception {
        if (!isInRange(counter)) {
            throw new IllegalArgumentException("Counter out of range: " + counter + " not in [0, " + c_max + "]");
        }

        String input = Integer.toString(counter);
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(delegatedKey);
        byte[] derivedBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(derivedBytes);
    }

    // 检查 counter 是否在范围 [0, c_max] 内
    private boolean isInRange(int counter) {
        return counter >= 0 && counter <= c_max;
    }

    // 根据父密钥和当前位派生子密钥
    private Key deriveChildKey(Key parentKey, char bit) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(parentKey);
        byte[] childKeyBytes = mac.doFinal(new byte[]{(byte) bit});
        return new SecretKeySpec(childKeyBytes, HMAC_SHA256);
    }

    public static void main(String[] args) throws Exception {
        int order = 6;  // 假设 order 为 6
        int c_max = 200;  // 自定义最大范围 c_max 为 200

        // 示例主密钥
        byte[] keyBytes = "SuperSecretKeyForDPRF".getBytes(StandardCharsets.UTF_8);
        Key masterKey = new SecretKeySpec(keyBytes, HMAC_SHA256);

        // 创建 DPRF 实例，设置最大范围 c_max
        DPRF dprf = new DPRF(masterKey, order, c_max);

        // 假设 Cc 为 150
        int Cc = 150;
        Key delegatedKey = dprf.DelKey(masterKey, Cc);
        System.out.println("Delegated Key: " + Base64.getEncoder().encodeToString(delegatedKey.getEncoded()));

        // 使用 Derive 方法生成伪随机值
        try {
            // 示例输入 counter 在范围内
            String derivedValue = dprf.Derive(delegatedKey, 150);
            System.out.println("Derived Value (150): " + derivedValue);

            // 示例输入 counter 超出范围
            derivedValue = dprf.Derive(delegatedKey, 250);
            System.out.println("Derived Value (250): " + derivedValue);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

}
