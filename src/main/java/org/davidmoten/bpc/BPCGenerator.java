package org.davidmoten.BPC;

import java.math.BigInteger;
import java.util.*;

import static org.davidmoten.Scheme.SRDSSE.SRDSSE.random;

public class BPCGenerator {

    private int order;
    public Map<BigInteger, Integer> shiftCounts = new HashMap<>();

    public BPCGenerator(int order) {
        this.order = order;
    }

    public List<BigInteger> GetBPCValueList(BigInteger[] R) {
        List<BigInteger> results = new ArrayList<>();
        Set<BigInteger> currentSet = new HashSet<>();

//        for (BigInteger value = R[0]; value.compareTo(R[1]) <= 0; value = value.add(BigInteger.ONE)) {
//            currentSet.add(value);
//            shiftCounts.put(value, 0); // 初始的右移次数为0
//        }
        for (BigInteger value:R) {
            currentSet.add(value);
            shiftCounts.put(value, 0); // 初始的右移次数为0
        }
        int iteration = 0;
        while (currentSet.size() > 1 && iteration < order) {
            Map<BigInteger, List<BigInteger>> map = new HashMap<>();
            for (BigInteger value : currentSet) {
                BigInteger parentValue = value.shiftRight(1);
                map.computeIfAbsent(parentValue, k -> new ArrayList<>()).add(value);
            }

            currentSet.clear();
            for (Map.Entry<BigInteger, List<BigInteger>> entry : map.entrySet()) {
                if (entry.getValue().size() > 1) {
                    currentSet.add(entry.getKey());
                    // 更新右移次数
                    shiftCounts.put(entry.getKey(), shiftCounts.get(entry.getValue().get(0)) + 1);
                } else {
                    results.addAll(entry.getValue());
                }
            }

            iteration++;
        }
        // 添加剩余的元素到结果中
        results.addAll(currentSet);
        return results;
    }

    // 将BigInteger值转换为Order位的二进制字符串，不足部分用*补齐
    public String toBinaryStringWithStars(BigInteger value, int order, int shiftCount) {
        String binaryString = value.toString(2); // 转换为二进制字符串
        int actualLength = order - shiftCount; // 二进制前缀的实际长度
        StringBuilder sb = new StringBuilder();

        // 补齐前导零
        while (binaryString.length() < actualLength) {
            binaryString = "0" + binaryString;
        }

        sb.append(binaryString);

        // 用*补齐剩余位数
        while (sb.length() < order) {
            sb.append('*');
        }

        return sb.toString();
    }
    // 新方法：根据k返回一个BigInteger数组R，使得results.size()==k
    public BigInteger[] generateRForSize(int k) {
        BigInteger[] R;
        List<BigInteger> results;

        // 尝试生成不同的R，直到results.size() == k
        do {
            // 随机生成两个不同的 BigInteger 作为 R 的范围
            BigInteger start = new BigInteger(order, random);  // 生成order位的随机数
            BigInteger end = start.add(BigInteger.valueOf(random.nextInt(10) + 1));  // 保证end和start不同

            R = new BigInteger[] {start, end};
            results = GetBPCValueList(R);

        } while (results.size() != k); // 直到满足results.size() == k

        return R;
    }
    public static void main(String[] args) {
        int order = 17*2;
        BPCGenerator bpc = new BPCGenerator(order);
        BigInteger[] R1 = {new BigInteger("7045786223"), new BigInteger("7045786225")};
        List<BigInteger> results1 = bpc.GetBPCValueList(R1);

        System.out.println("BPCValue Results: " + results1);

        for (BigInteger result : results1) {
            // 使用右移次数计算并输出二进制字符串
            System.out.println("BPCValue Result (with stars): " + bpc.toBinaryStringWithStars(result, order, bpc.shiftCounts.get(result)));
        }

        // 调用新方法生成R，确保results.size() == k
        int k = 3;  // 期望结果数量为k
        BigInteger[] R = bpc.generateRForSize(k);

        // 输出生成的R数组
        System.out.println("Generated R: " + Arrays.toString(R));

        // 验证results的大小
        List<BigInteger> results = bpc.GetBPCValueList(R);
        System.out.println("Results size: " + results.size());
    }
}
