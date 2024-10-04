package org.davidmoten.BPC;

import java.math.BigInteger;
import java.util.*;

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

    public static void main(String[] args) {
        int order = 17*2;
        BPCGenerator bpc = new BPCGenerator(order);
        BigInteger[] R = {new BigInteger("7045786223"), new BigInteger("7045786225")};
        List<BigInteger> results = bpc.GetBPCValueList(R);

        System.out.println("BPCValue Results: " + results);

        for (BigInteger result : results) {
            // 使用右移次数计算并输出二进制字符串
            System.out.println("BPCValue Result (with stars): " + bpc.toBinaryStringWithStars(result, order, bpc.shiftCounts.get(result)));
        }
    }
}
