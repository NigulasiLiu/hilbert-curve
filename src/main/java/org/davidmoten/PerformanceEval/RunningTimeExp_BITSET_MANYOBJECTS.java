package org.davidmoten.PerformanceEval;

import org.davidmoten.Scheme.SPQS.SPQS_BITSET;
import org.davidmoten.Scheme.TDSC2023.TDSC2023_BITSET;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.*;

public class RunningTimeExp_BITSET_MANYOBJECTS {
    public static void main(String[] args) throws Exception {
        // 定义实验参数
        String filepath = "src/dataset/spatialdata.csv";
        List<Integer> kList = Arrays.asList(4); // 随机选取的关键字数量
        List<Integer> lList = Arrays.asList(419430); // R_min 和 R_max 之间的差距
        List<Integer> expTimesList = Arrays.asList(1); // 实验次数
        List<Integer> insertions = Arrays.asList(1,40000,60000,80000,100000); // 实验次数
        List<Integer> maxfilesList = Arrays.asList(1 << 20); // 文件数量
        List<Integer> orderList = Arrays.asList(16); // Hilbert curve 级别
        List<String> schemes = Arrays.asList("SPQS_BITSET");  // 待测试方案列表

        SPQS_BITSET spqs = new SPQS_BITSET(maxfilesList.get(0),orderList.get(0), 2);
        TDSC2023_BITSET tdsc2023_BITSET = new TDSC2023_BITSET(128, 10000, maxfilesList.get(0), orderList.get(0), 2);
        // 遍历所有的 k 和 maxfiles 组合
        for (int k : kList) {
            for (int maxfiles : maxfilesList) {
                // 创建输出目录
                String dirName = "src/results/UpdateRunTime_BITSET_MANYOBJECTS/k" + k + "_maxfiles_" + maxfiles;
                Path dirPath = Paths.get(dirName);
                if (!Files.exists(dirPath)) {
                    Files.createDirectories(dirPath);
                }

                // 遍历剩下的参数 l、expTimes 和 order 进行实验
                for (int l : lList) {
                    for (int expTimes : expTimesList) {
                        for (int order : orderList) {
                            // 文件名前缀使用类名，结果保存在新创建的目录中
                            for (String scheme : schemes) {
                                String fileName = dirName + "/" + scheme + "_order_" + order + "_maxfiles_" + maxfiles
                                        + "_keywordsnum_" + k + "_rangelen_" + (2 * l) + "_exptimes_" + expTimes + ".txt";
                                try (PrintStream out = new PrintStream(Files.newOutputStream(Paths.get(fileName)))) {
                                    // 创建不同的方案对象并运行实验
                                    if (scheme.equals("SPQS_BITSET")) {
                                        spqs.setup(SPQS_BITSET.LAMBDA, maxfiles);
                                        runExperiments(spqs, k, l, expTimes, insertions.get(0), filepath, maxfiles, out,dirName);
                                    } else if (scheme.equals("TDSC2023_BITSET")) {
                                        runExperiments(tdsc2023_BITSET, k, l, expTimes, insertions.get(0),filepath, maxfiles, out, dirName);
                                    }

                                    System.out.println("实验结果保存至: " + fileName);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
        // 打印时间列表中的所有值
        tdsc2023_BITSET.printTimes();
        tdsc2023_BITSET.removeExtremesUpdateTime();
        tdsc2023_BITSET.removeExtremesSearchTime();
        // 输出平均值
        System.out.println("tdsc2023_BITSET平均更新耗时: " + tdsc2023_BITSET.getAverageUpdateTime() + " ms");
        System.out.println("tdsc2023_BITSET平均查询客户端耗时: " + tdsc2023_BITSET.getAverageClientTime() + " ms");
        System.out.println("tdsc2023_BITSET平均查询服务器耗时: " + tdsc2023_BITSET.getAverageServerTime() + " ms");
        // 打印时间列表中的所有值
        spqs.printTimes();
        spqs.removeExtremesUpdateTime();
        spqs.removeExtremesSearchTime();
        // 输出平均值
        System.out.println("spqs平均更新耗时: " + spqs.getAverageUpdateTime() + " ms");
        System.out.println("spqs平均查询客户端耗时: " + spqs.getAverageClientTime() + " ms");
        System.out.println("spqs平均查询服务器耗时: " + spqs.getAverageServerTime() + " ms");
    }

    // 运行实验，适用于不同的方案对象
    private static void runExperiments(Object scheme, int k, int l, int expTimes,int insertions, String filepath, int maxfiles, PrintStream out, String dirName) throws Exception {
        // 模拟从数据集中获取一项数据
        Object[] test = SPQS_BITSET.GetRandomItem(12, filepath);
        if (test == null) {
//            System.out.println("获取数据失败。");
            return;
        }
        int[] files = new int[]{maxfiles-1};
        long[] pSet = (long[]) test[1];
        String[] W = (String[]) test[2];

        String[] WQ = getRandomKeywords(W, k);
        System.out.println("自动选取的关键字: ");
        for (String keyword : WQ) {
            System.out.print(keyword + " ");
        }
        System.out.println();

        // 进行一次update和search,不计算本次运行时间
        if (scheme instanceof SPQS_BITSET) {
            SPQS_BITSET spqs = (SPQS_BITSET) scheme;
            spqs.ObjectUpdate(pSet, W, "add", files, 500);
            BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSet);
            BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
            BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
            // Search
            BigInteger BR = spqs.ObjectSearch(R_min, R_max, WQ);
        } else if (scheme instanceof TDSC2023_BITSET) {
            TDSC2023_BITSET tdsc2023 = (TDSC2023_BITSET) scheme;
            tdsc2023.update(pSet, W, "add", files, 500);
            BigInteger pointHilbertIndex = tdsc2023.hilbertCurve.index(pSet);
            BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
            BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
            // Search
            BigInteger BR = tdsc2023.Search(R_min, R_max, WQ);
        }

//        System.setOut(out);  // 重定向输出
        System.out.println("正式实验: ");
        // 初始化一个存储实验结果的二维数组
        Object[][] results = new Object[insertions][3];
        Object[] result;

        // 获取 insertions 次的随机数据，并将其存储到 results 中
        for (int i = 0; i < insertions; i++) {
            result = SPQS_BITSET.GetRandomItem(12, filepath);
            if (result == null) {
                System.out.println("获取数据失败。");
                return;
            }
            results[i] = result;
            files = new int[]{((BigInteger) results[i][0]).intValue()}; // 从 results 中获取文件
            pSet = (long[]) results[i][1]; // 从 results 中获取 pSet
            W = (String[]) results[i][2]; // 从 results 中获取 W
            double progress = (double) ((i + 1) / (double) insertions * 100);
//            System.err.println(" 实验进度: " + progress + "%");
            System.out.println("实验次数: " + (i + 1));

            // 执行 Update
            if (scheme instanceof SPQS_BITSET) {
                SPQS_BITSET spqs = (SPQS_BITSET) scheme;
                spqs.ObjectUpdate(pSet, W, "add", files, 100000);
            } else if (scheme instanceof TDSC2023_BITSET) {
                TDSC2023_BITSET tdsc2023 = (TDSC2023_BITSET) scheme;
                tdsc2023.update(pSet, W, "add", files, 100000);
            }
//            System.out.println("第" + (i + 1) + "次insert完成。\n");
        }

        // 重定向输出到新的文件 fileName + "search"
        String searchFileName = dirName + "/" +  scheme + "_keywordsnum_" + k + "_rangelen_" + (2 * l) + "_exptimes_" + expTimes + "_search.txt";
        try (PrintStream searchOut = new PrintStream(Files.newOutputStream(Paths.get(searchFileName)))) {
//            System.setOut(searchOut);
            if (scheme instanceof SPQS_BITSET) {
                SPQS_BITSET spqs = (SPQS_BITSET) scheme;
                BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSet);
                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
                BigInteger BR = spqs.ObjectSearch(R_min, R_max, WQ);
            } else if (scheme instanceof TDSC2023_BITSET) {
                TDSC2023_BITSET tdsc2023 = (TDSC2023_BITSET) scheme;
                BigInteger pointHilbertIndex = tdsc2023.hilbertCurve.index(pSet);
                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
                BigInteger BR = tdsc2023.Search(R_min, R_max, WQ);
            }
        }
    }


    // 随机选取W中的k个关键字
    private static String[] getRandomKeywords(String[] W, int k) {
        Random random = new Random();
        String[] WQ = new String[k];
        Set<Integer> selectedIndexes = new HashSet<>();

        while (selectedIndexes.size() < k) {
            int index = random.nextInt(W.length);
            if (!selectedIndexes.contains(index)) {
                WQ[selectedIndexes.size()] = W[index];
                selectedIndexes.add(index);
            }
        }

        return WQ;
    }
}