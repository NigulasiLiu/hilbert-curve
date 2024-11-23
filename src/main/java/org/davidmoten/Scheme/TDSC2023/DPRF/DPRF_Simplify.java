package org.davidmoten.Scheme.TDSC2023.DPRF;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

public class DPRF_Simplify {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final int c_max;  // 定义最大范围 c_max

    // 构造函数，接受最大范围 c_max
    public DPRF_Simplify(int c_max) {
        this.c_max = c_max;
    }

    // 生成委托密钥 ST
    public Key DelKey(byte[] K, int c_from_counter) throws Exception {
        // 检查 c_from_counter 是否在允许的范围内
        if (!isInRange(c_from_counter)) {
            throw new IllegalArgumentException("Counter out of range: " + c_from_counter + " not in [0, " + c_max + "]");
        }

        // 在DelKey中实现GGM PRF
        // 调用GGM PRF来生成基于谓词（counter）的委托密钥
//        return new SecretKeySpec(GGM_PRF(K, c_from_counter).getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        return new SecretKeySpec(K, HMAC_SHA256);
    }
    public byte[] Derive(Key key, int counter) throws Exception {
        if (!isInRange(counter)) {
            throw new IllegalArgumentException("Counter out of range: " + counter + " not in [0, " + c_max + "]");
        }

        String input = Integer.toString(counter);
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(key);
        return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
    }

    // 检查 counter 是否在范围 [0, c_max] 内
    private boolean isInRange(int counter) {
        return counter >= 0 && counter <= c_max;
    }
//    // 使用 HMAC 生成 GGM PRF 的下一步伪随机值
//    private String generateNextKey(String currentKey, int bit) throws Exception {
//        String input = currentKey + bit;  // 将当前密钥和路径位结合
//        Mac mac = Mac.getInstance(HMAC_SHA256);
//        mac.init(new SecretKeySpec(currentKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
//        byte[] derivedBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
//        return Base64.getEncoder().encodeToString(derivedBytes);
//    }
    //    // GGM PRF 的 Derive 实现
//    public String Derive(Key key, int counter) throws Exception {
//        if (!isInRange(counter)) {
//            throw new IllegalArgumentException("Counter out of range: " + counter + " not in [0, " + c_max + "]");
//        }
//
//        String input = Integer.toString(counter);
//        Mac mac = Mac.getInstance(HMAC_SHA256);
//        mac.init(key);
//        byte[] derivedBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
//        return Base64.getEncoder().encodeToString(derivedBytes);
//    }
    // GGM PRF 的 Derive 实现
    // GGM PRF 实现：GGM PRF 是通过递归的方式生成 PRF 值
//    private String GGM_PRF(String seed, int index) throws Exception {
//        int depth = (int) Math.ceil(Math.log(c_max) / Math.log(2));  // 二叉树的深度取决于 c_max
//        String currentKey = seed;  // 初始种子（密钥）
//
//        // 遍历二叉树路径
//        for (int i = 0; i < depth; i++) {
//            // 生成二进制路径，左(0)或右(1)
//            int bit = (index >> (depth - 1 - i)) & 1;
//
//            // 使用HMAC来生成左右分支的伪随机值
//            currentKey = generateNextKey(currentKey, bit);
//        }
//
//        return currentKey;
//    }

//    public static void main(String[] args) throws Exception {
//        int c_max = 200;  // 自定义最大范围 c_max 为 200
//
//        // 创建 DPRF_Simplify 实例，设置最大范围 c_max
//        DPRF_Simplify dprf = new DPRF_Simplify(c_max);
//
//        // 测试过程：Derive -> DelKey -> Derive
//        String K = "SomeKey";
//        int c = 150;
//
//        // 第一次 Derive
//        String derivedValue1 = dprf.Derive(new SecretKeySpec(K.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), c);
//        System.out.println("Derived Value 1 (from K): " + derivedValue1);
//
//        // 生成 ST 并 Derive
//        Key stKey = dprf.DelKey(K, c);
//        String derivedValue2 = dprf.Derive(stKey, c);
//        System.out.println("Derived Value 2 (from ST): " + derivedValue2);
//
//        // 验证两个 Derived Values 是否匹配
//        if (derivedValue1.equals(derivedValue2)) {
//            System.out.println("Derived values match.");
//        } else {
//            System.out.println("Derived values do not match.");
//        }
//    }
}
