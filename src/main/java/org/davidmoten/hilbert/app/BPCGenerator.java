//package org.davidmoten.hilbert.Compute;
//
//import java.util.*;
//import java.util.stream.Collectors;
//import java.util.stream.LongStream;
//
//public class BPCGenerator {
//
//    private int order;
//
//    public BPCGenerator(int order) {
//        this.order = order;
//    }
//
//    public List<Long> BPCValue(long[] R) {
//        List<Long> results = new ArrayList<>();
//        Set<Long> currentSet = new HashSet<>();
//
////        for (long value : R) {
////            currentSet.add(value);
////        }
//        for(long value=R[0];value<=R[1];value++){
//            currentSet.add(value);
//        }
//        int iteration = 0;
//        while (currentSet.size() > 1 && iteration < order) {
//            Map<Long, List<Long>> map = new HashMap<>();
//            for (long value : currentSet) {
//                long parentValue = value >> 1;
//                map.computeIfAbsent(parentValue, k -> new ArrayList<>()).add(value);
//            }
//
//            currentSet.clear();
//            for (Map.Entry<Long, List<Long>> entry : map.entrySet()) {
//                if (entry.getValue().size() > 1) {
//                    currentSet.add(entry.getKey());
//                } else {
//                    results.addAll(entry.getValue());
//                }
//            }
//
//            iteration++;
//        }
//
//        results.addAll(currentSet);
//        return results;
//    }
//
//    // 将long值转换为Order位的二进制字符串，不足部分用*补齐
//    public String toBinaryStringWithStars(long value, int depth) {
//        String binaryString = Long.toBinaryString(value);
//        int actualLength = order - depth; // 二进制前缀的实际长度
//        StringBuilder sb = new StringBuilder();
//
//        // 补齐前导零
//        while (binaryString.length() < actualLength) {
//            binaryString = "0" + binaryString;
//        }
//
//        sb.append(binaryString);
//
//        // 用*补齐剩余位数
//        while (sb.length() < order) {
//            sb.append('*');
//        }
//
//        return sb.toString();
//    }
//
//
//    public static void main(String[] args) {
//        int order = 6;
//        BPCGenerator bpc = new BPCGenerator(order); // 假设 Order 为 6
//        long[] R = {36, 47};
//        List<Long> results = bpc.BPCValue(R);
//        System.out.println("BPCValue Results: " + results);
//        for (Long result : results) {
//            System.out.println("BPCValue Results: " + bpc.toBinaryStringWithStars(result, order));
//        }
//    }
//}
package org.davidmoten.hilbert.app;

import java.util.*;

public class BPCGenerator {

    private int order;
    public Map<Long, Integer> shiftCounts = new HashMap<>();
    public BPCGenerator(int order) {
        this.order = order;
    }

    public List<Long> GetBPCValueList(long[] R) {
        List<Long> results = new ArrayList<>();
        Set<Long> currentSet = new HashSet<>();

        for (long value = R[0]; value <= R[1]; value++) {
            currentSet.add(value);
            shiftCounts.put(value, 0); // 初始的右移次数为0
        }

        int iteration = 0;
        while (currentSet.size() > 1 && iteration < order) {
            Map<Long, List<Long>> map = new HashMap<>();
            for (long value : currentSet) {
                long parentValue = value >> 1;
                map.computeIfAbsent(parentValue, k -> new ArrayList<>()).add(value);
            }

            currentSet.clear();
            for (Map.Entry<Long, List<Long>> entry : map.entrySet()) {
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

    // 将long值转换为Order位的二进制字符串，不足部分用*补齐
    public String toBinaryStringWithStars(long value, int order, int shiftCount) {
        String binaryString = Long.toBinaryString(value);
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
        int order = 6;
        BPCGenerator bpc = new BPCGenerator(order); // 假设 Order 为 6
        long[] R = {38, 47};
        List<Long> results = bpc.GetBPCValueList(R);

        System.out.println("BPCValue Results: " + results);

        for (Long result : results) {
            // 使用右移次数计算并输出二进制字符串
            System.out.println("BPCValue Result (with stars): " + bpc.toBinaryStringWithStars(result, order, bpc.shiftCounts.get(result)));
        }
    }
}
