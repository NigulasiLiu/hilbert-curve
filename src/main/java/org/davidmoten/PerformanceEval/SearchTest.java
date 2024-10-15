package org.davidmoten.PerformanceEval;

import org.davidmoten.Scheme.SPQS.SPQS_Biginteger;
import org.davidmoten.Scheme.TDSC2023.TDSC2023_Biginteger;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class SearchTest {

    public static void main(String[] args) throws Exception {
        int delupdatetimes = 3;
        int searchtimes;
        int updatetime = 800;
        int objectnum = 1000000;
        int rangePredicate = 100000;
        int Wnums = 6;
        int maxfiles = 1 << 20;
        int hilbertOrder = 17;
        int l; // 将 l 的初始定义移到 while 循环内

        Random random = new Random(); // 用于随机选择对象
        // 初始化200个Object集合，每个包含pSet, W, files
        Object[] predefinedObjects = new Object[objectnum];
        for (int i = 0; i < objectnum; i++) {
            long[] pSet = {i, i + 1}; // pSet
            String[] W = new String[Wnums]; // 创建W数组
            for (int j = 0; j < Wnums; j++) {
                W[j] = "keyword" + (i + j + 1); // 动态生成关键词
            }
            int[] files = {random.nextInt(maxfiles)}; // files
            predefinedObjects[i] = new Object[]{pSet, W, files};
        }

        List<Double> spqsSearchTimes = new ArrayList<>();
        List<Double> tdscSearchTimes = new ArrayList<>();

        // 初始化 SPQS_Biginteger 和 TDSC2023_Biginteger 实例
        SPQS_Biginteger spqs = new SPQS_Biginteger(maxfiles, hilbertOrder, 2);
        TDSC2023_Biginteger tdsc2023 = new TDSC2023_Biginteger(128, rangePredicate, maxfiles, hilbertOrder, 2);
        int batchSize = 100; // 每次处理100个更新

        // 执行更新操作
        for (int i = 0; i < updatetime; i++) {
            Object[] selectedObject = (Object[]) predefinedObjects[random.nextInt(objectnum)];
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

        // 移除初始的异常值
        for (int i = 0; i < delupdatetimes; i++) {
            spqs.removeExtremesUpdateTime(); // 移除SPQS的异常更新时间
            tdsc2023.removeExtremesUpdateTime(); // 移除TDSC2023的异常更新时间
        }

        Scanner scanner = new Scanner(System.in);
        boolean continueSearch = true;

        while (continueSearch) {
            // 获取用户输入的 searchtimes 和 l
            System.out.print("请输入 searchtimes: ");
            searchtimes = scanner.nextInt();
            System.out.print("请输入 l 的值 (控制 Hilbert 范围): ");
            l = scanner.nextInt();

            // 执行搜索操作
            for (int i = 0; i < searchtimes; i++) {
                int randomValue = random.nextInt(objectnum);
                long[] pSetQ = {randomValue, randomValue + 1}; // pSet

                String[] WQ = new String[Wnums]; // 创建W数组
                for (int j = 0; j < Wnums; j++) {
                    WQ[j] = "keyword" + (randomValue + j + 1); // 动态生成关键词
                }
                BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSetQ);
                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
                spqs.ObjectSearch(R_min, R_max, WQ);
                tdsc2023.Search(R_min, R_max, WQ);

                // 记录每次搜索的平均耗时
                double spqsAvgClientSearchTime = spqs.getAverageClientTime();
                double spqsAvgServerSearchTime = spqs.getAverageServerTime();
                double spqsAvgSearchTime = spqs.getAverageSearchTime();

                double tdscAvgClientSearchTime = tdsc2023.getAverageClientTime();
                double tdscAvgServerSearchTime = tdsc2023.getAverageServerTime();
                double tdscAvgSearchTime = tdsc2023.getAverageSearchTime();

                spqsSearchTimes.add(spqsAvgSearchTime);
                tdscSearchTimes.add(tdscAvgSearchTime);

                // 打印每次搜索的结果
                System.out.printf("SPQS: |%-10.6f|ms Client: |%-10.6f|ms SPQS Server: |%-10.6f|ms | TDSC2023: |%-10.6f|ms Client: |%-10.6f|ms TDSC2023 Server: |%-10.6f|ms\n",
                        spqsAvgSearchTime, spqsAvgClientSearchTime, spqsAvgServerSearchTime,
                        tdscAvgSearchTime, tdscAvgClientSearchTime, tdscAvgServerSearchTime);
            }

            // 打印平均搜索时间
            System.out.printf("avg:| SPQS: %-10.6fms | TDSC2023: %-10.6fms\n",
                    spqsSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    tdscSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

            spqsSearchTimes.clear();
            tdscSearchTimes.clear();

            // 询问用户是否继续
            System.out.print("是否继续输入新的 searchtimes 和 l 进行搜索? (y/n): ");
            String input = scanner.next();
            if (!input.equalsIgnoreCase("y")) {
                continueSearch = false;
            }
        }

        scanner.close();
    }
}
