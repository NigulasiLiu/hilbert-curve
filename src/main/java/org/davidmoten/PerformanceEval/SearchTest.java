package org.davidmoten.PerformanceEval;

import org.davidmoten.Scheme.SPQS.SPQS_BITSET;
import org.davidmoten.Scheme.TDSC2023.TDSC2023_BITSET;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SearchTest {

    public static void main(String[] args) throws Exception {
        int delupdatetimes = 1;
        int updatetimes = 600;
        int searchtimes = 100;
        int objectnums = 2000;
        int rangePredicate = 20000;
        int Wnums = 6;
        int[] maxfilesArray = {1 << 20};
        int[] hilbertOrders = {17};

        // 初始化200个Object集合，每个包含pSet, W, files
        Object[] predefinedObjects = new Object[objectnums];
        for (int i = 0; i < objectnums; i++) {
            long[] pSet = {i, i + 1}; // pSet
            String[] W = new String[Wnums]; // 创建W数组
            for (int j = 0; j < Wnums; j++) {
                W[j] = "keyword" + (i + j + 1); // 动态生成关键词
            }
            int[] files = {i}; // files
            predefinedObjects[i] = new Object[]{pSet, W, files};
        }

        Random random = new Random(); // 用于随机选择对象

        // 初始化标题行
//        System.out.println("SPQS and TDSC2023 Update Times:");

        List<Double> spqsSearchTimes = new ArrayList<>();
        List<Double> tdscSearchTimes = new ArrayList<>();
        // 外层循环：遍历 maxfiles 值
        for (int maxfiles : maxfilesArray) {
            // 内层循环：遍历 hilbert orders 值
            for (int hilbertOrder : hilbertOrders) {
                // 初始化 SPQS_BITSET 和 TDSC2023_BITSET 实例
                SPQS_BITSET spqs = new SPQS_BITSET(maxfiles, hilbertOrder, 2);
                TDSC2023_BITSET tdsc2023 = new TDSC2023_BITSET(128, rangePredicate, maxfiles, hilbertOrder, 2);
                int batchSize = 10; // 每次处理50个更新
                // 执行 update 操作 (模拟多次操作获取平均时间)
                for (int i = 0; i < updatetimes; i++) {
                    // 从预定义的集合中随机取出一个对象
                    Object[] selectedObject = (Object[]) predefinedObjects[random.nextInt(objectnums)];
                    long[] pSet = (long[]) selectedObject[0];
                    String[] W = (String[]) selectedObject[1];
                    int[] files = (int[]) selectedObject[2];

                    // 进行更新操作
                    spqs.ObjectUpdate(pSet, W, "add", files, rangePredicate);
                    tdsc2023.update(pSet, W, "add", files, rangePredicate);
                    if ((i + 1) % batchSize == 0) {
                        System.gc(); // 执行垃圾回收
                        System.out.println("Completed batch " + (i + 1) / batchSize + " of updates.");
                    }
                }
                for (int i = 0; i < delupdatetimes; i++) {
                    spqs.removeExtremesUpdateTime(); // 移除第一次耗时，去掉初始的异常值
                    tdsc2023.removeExtremesUpdateTime(); // 同样移除
                }
                for (int i = 0;i < searchtimes; i++){
                    int randomValue = random.nextInt(objectnums*4);
                    long[] pSetQ = {randomValue, randomValue + 1}; // pSet

                    String[] WQ = new String[Wnums]; // 创建W数组
                    for (int j = 0; j < Wnums; j++) {
                        WQ[j] = "keyword" + (randomValue + j + 1); // 动态生成关键词
                    }
                    BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSetQ);
                    BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(100));
                    BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(100));
                    spqs.ObjectSearch(R_min,R_max,WQ);
                    tdsc2023.Search(R_min,R_max,WQ);
                    // 记录每次循环的平均更新耗时
                    double spqsAvgClientSearchTime = spqs.getAverageClientTime();
                    double spqsAvgServerSearchTime = spqs.getAverageServerTime();
                    double spqsAvgSearchTime = spqs.getAverageSearchTime();

                    double tdscAvgClientSearchTime = tdsc2023.getAverageClientTime();
                    double tdscAvgServerSearchTime = tdsc2023.getAverageServerTime();
                    double tdscAvgSearchTime = tdsc2023.getAverageSearchTime();
                    tdsc2023.printTimes();
                    spqsSearchTimes.add(spqsAvgSearchTime);
                    tdscSearchTimes.add(tdscAvgSearchTime);
                    // 初始化标题行
                    System.out.println("SPQS and TDSC2023 Search Times:");
                    // 打印每次组合的结果
                    System.out.printf("maxfiles: %-10d hilbertOrder: %-2d | SPQS: %-10.6fms Client: %-10.6fms SPQS Server: %-10.6fms | TDSC2023: %-10.6fms Client: %-10.6fms TDSC2023 Server: %-10.6fms\n", maxfiles,
                            hilbertOrder, spqsAvgSearchTime,spqsAvgClientSearchTime, spqsAvgServerSearchTime, tdscAvgSearchTime,tdscAvgClientSearchTime, tdscAvgServerSearchTime);
                }

            }
        }
        System.out.printf("avg:| SPQS: %-10.6fms | TDSC2023: %-10.6fms\n",
                spqsSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                tdscSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        spqsSearchTimes.clear();
        tdscSearchTimes.clear();
    }
}

