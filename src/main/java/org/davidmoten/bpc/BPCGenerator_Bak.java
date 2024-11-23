package org.davidmoten.BPC;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

public class BPCGenerator_Bak {

    private int bits;
    public Map<BigInteger, Integer> shiftCounts = new HashMap<>();
    public List<Map<BigInteger, Integer>> shiftCountsHistory = new ArrayList<>(); // 每轮迭代的shiftCounts

    public BPCGenerator_Bak(int bits) {
        this.bits = bits;
    }

    public List<BigInteger> GetBPCValueList(BigInteger[] R) {
        List<BigInteger> results = new ArrayList<>();
        Set<BigInteger> currentSet = new HashSet<>();//存储所有Biginteger,并且去重
        for (BigInteger value : R) {
            currentSet.add(value);
            shiftCounts.put(value, 0); // 每个Bigint的初始的右移次数为0
        }
        int iteration = 0;
        //终止条件是：1.只剩下一个前缀，无法继续合并；2.已经迭代了bits次,这是字符串的最大长度了
        while (currentSet.size() > 1 && iteration < bits) {
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

    public Map<Integer, List<BigInteger>> GetBPCValueMap(BigInteger[] R) {
        Map<Integer, List<BigInteger>> currentShiftCounts = new HashMap<>(); // 当前轮的右移次数
        // 当前轮的BigInteger集合
        Set<BigInteger> currentSet = new HashSet<>(Arrays.asList(R));
        int iteration = 0;
        // 终止条件：currentSet大小为1或达到bits次迭代
        while (currentSet.size() > 1 && iteration < bits) {
            Map<BigInteger, List<BigInteger>> map = new HashMap<>();
            // 构建父子节点映射关系
            for (BigInteger value : currentSet) {
                BigInteger parentValue = value.shiftRight(1); // 模拟父节点计算
                map.computeIfAbsent(parentValue, k -> new ArrayList<>()).add(value);
            }
            currentSet.clear(); // 清空currentSet，为下一轮迭代准备
            for (Map.Entry<BigInteger, List<BigInteger>> entry : map.entrySet()) {
                if (entry.getValue().size() > 1) {
                    // 如果当前父节点有多个子节点，则保留父节点
                    currentSet.add(entry.getKey());
                } else {
                    // 在iteration+1轮的迭代中，假如entry.getValue().size()=1，那么该元素是要被提取出来的
                    currentShiftCounts.computeIfAbsent(iteration, k -> new ArrayList<>(entry.getValue()));
                }
            }
            iteration++; // 增加迭代次数
        }
        // set中的最后一个元素
        currentShiftCounts.computeIfAbsent(iteration, k -> new ArrayList<>()).addAll(currentSet);
        return currentShiftCounts;
    }

    // 将BigInteger值转换为Order位的二进制字符串，不足部分用*补齐
    public String toBinaryStringWithStars(BigInteger value, int bits, int shiftCount) {
        String binaryString = value.toString(2); // 转换为二进制字符串
        int actualLength = bits - shiftCount; // 二进制前缀的实际长度
        StringBuilder sb = new StringBuilder();

        // 补齐前导零
        while (binaryString.length() < actualLength) {
            binaryString = "0" + binaryString;
        }

        sb.append(binaryString);
        // 用*补齐剩余位数
        while (sb.length() < bits) {
            sb.append('*');
        }
        return sb.toString();
    }

    // 将BigInteger值转换为Order位的二进制字符串，会出现长短不一的最佳前缀
    /**
     * 将BigInteger值转换为Order位的二进制字符串，并生成具有最佳前缀的字符串列表。
     *
     * @param resultMap 包含每次迭代结果的Map<Integer, List<BigInteger>>
     * @return 最佳前缀的二进制字符串列表
     */
    public List<String> convertMapToPrefixString(Map<Integer, List<BigInteger>> resultMap) {
        List<String> binaryStrings = new ArrayList<>();

        // 遍历每次迭代的结果
        for (Map.Entry<Integer, List<BigInteger>> entry : resultMap.entrySet()) {
            int iteration = entry.getKey();
            List<BigInteger> values = entry.getValue();

            for (BigInteger value : values) {
                // 将 BigInteger 转换为二进制字符串
                String binaryString;
                if(!value.equals(BigInteger.ZERO)) {
                    binaryString = value.toString(2);
                }
                else{
                    binaryString = "";
                }

                // 计算有效长度和前缀位数
                int effectiveLength = bits - iteration;
                int leadingZeros = Math.max(effectiveLength - binaryString.length(), 0);

                // 构建完整的二进制字符串（前导0 + 二进制值 + '*')
                StringBuilder sb = new StringBuilder(bits);
                while (leadingZeros-- > 0) {
                    sb.append('0');
                }
                sb.append(binaryString);
                for (int i = 0; i < iteration; i++) {
                    sb.append('*');
                }

                // 添加结果到列表
                binaryStrings.add(sb.toString());
            }
        }

        return binaryStrings;
    }


    public static void main(String[] args) {
        int bits = 6;
        BPCGenerator_Bak bpc = new BPCGenerator_Bak(bits);
        BigInteger[] R2 = {new BigInteger("1"), new BigInteger("3")};
        BigInteger[] R1 = {new BigInteger("33"), new BigInteger("63")};

        //生成min到max的所有Bigint
        BigInteger[] R = Stream.iterate(R1[0], n -> n.add(BigInteger.ONE))
                .limit(R1[1].subtract(R1[0]).add(BigInteger.ONE).intValueExact())
                .toArray(BigInteger[]::new);

        System.out.println("R:");
        for (BigInteger r : R) {
            System.out.print(r + " ");
        }
//        List<BigInteger> results = bpc.GetBPCValueList(R);
//        List<String> BinaryResults = new ArrayList<>();
////        System.out.println("BPC1: " + results);
//        for (BigInteger result : results) {
//            String bpc_string = bpc.toBinaryStringWithStars(result, bits, bpc.shiftCounts.get(result));
//            BinaryResults.add(bpc_string);
//        }
//        System.out.println("\nBPC1:" + BinaryResults);
        // 获取BPC结果（包括分组）
        Map<Integer, List<BigInteger>> results1 = bpc.GetBPCValueMap(R);
//        System.out.println("\nbpc_string:");
//        int groupIndex = 1; // 分组计数器

//        for (Map.Entry<Integer, List<BigInteger>> entry : results1.entrySet()) {
//            System.out.println("Group " + groupIndex + " (Iteration " + entry.getKey() + "):");
//            System.out.println(entry.getValue());
//            groupIndex++;
//        }
        System.out.println("BPC2:" + bpc.convertMapToPrefixString(results1));

    }
}
