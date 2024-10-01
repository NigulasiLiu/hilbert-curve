package org.davidmoten.quadtree;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

public class DPRF {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final int c_max;  // 定义最大范围 c_max

    // 构造函数，接受最大范围 c_max
    public DPRF(int c_max) {
        this.c_max = c_max;
    }

    // 生成委托密钥 ST
    public Key DelKey(String K, int c_from_counter) throws Exception {
        // 检查 c_from_counter 是否在允许的范围内
        if (!isInRange(c_from_counter)) {
            throw new IllegalArgumentException("Counter out of range: " + c_from_counter + " not in [0, " + c_max + "]");
        }
        // 直接返回Kw或者Kp作为STw/STp
        return new SecretKeySpec(K.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
    }

    // GGM PRF 的 Derive 实现
    public String Derive(Key key, int counter) throws Exception {
        if (!isInRange(counter)) {
            throw new IllegalArgumentException("Counter out of range: " + counter + " not in [0, " + c_max + "]");
        }

        String input = Integer.toString(counter);
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(key);
        byte[] derivedBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(derivedBytes);
    }


    // 检查 counter 是否在范围 [0, c_max] 内
    private boolean isInRange(int counter) {
        return counter >= 0 && counter <= c_max;
    }

    public static void main(String[] args) throws Exception {
        int c_max = 200;  // 自定义最大范围 c_max 为 200

        // 创建 DPRF 实例，设置最大范围 c_max
        DPRF dprf = new DPRF(c_max);

        // 测试过程：Derive -> DelKey -> Derive
        String K = "SomeKey";
        int c = 150;

        // 第一次 Derive
        String derivedValue1 = dprf.Derive(new SecretKeySpec(K.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), c);
        System.out.println("Derived Value 1 (from K): " + derivedValue1);

        // 生成 ST 并 Derive
        Key stKey = dprf.DelKey(K, c);
        String derivedValue2 = dprf.Derive(stKey, c);
        System.out.println("Derived Value 2 (from ST): " + derivedValue2);

        // 验证两个 Derived Values 是否匹配
        if (derivedValue1.equals(derivedValue2)) {
            System.out.println("Derived values match.");
        } else {
            System.out.println("Derived values do not match.");
        }
    }
}
