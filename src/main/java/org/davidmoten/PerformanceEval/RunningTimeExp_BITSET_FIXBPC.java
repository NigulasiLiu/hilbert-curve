//package org.davidmoten.PerformanceEval;
//
//import org.davidmoten.Scheme.SPQS.SPQS_BITSET;
//import org.davidmoten.Scheme.TDSC2023.TDSC2023_BITSET;
//
//import java.io.*;
//import java.math.BigInteger;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.nio.file.Path;
//import java.util.*;
//
//public class RunningTimeExp_BITSET_FIXBPC {
//    public static void main(String[] args) throws Exception {
//        // 定义实验参数
//        String filepath = "src/dataset/spatialdata.csv";
//        List<Integer> kList = Arrays.asList(2, 4, 6, 8); // 随机选取的关键字数量
//        List<Integer> lList = Arrays.asList(100, 150, 200, 250); // R_min 和 R_max 之间的差距
//        List<Integer> expTimesList = Arrays.asList(20, 40, 60, 80); // 实验次数
//        List<Integer> maxfilesList = Arrays.asList(1 << 12, 1 << 14, 1 << 16, 1 << 18, 1 << 20); // 文件数量
//        List<Integer> orderList = Arrays.asList(15, 16, 17); // Hilbert curve 级别
//        List<String> schemes = Arrays.asList("SPQS_BITSET", "TDSC2023_BITSET");  // 待测试方案列表
//
//        // 遍历所有的 k 和 maxfiles 组合
//        for (int k : kList) {
//            for (int maxfiles : maxfilesList) {
//                // 创建输出目录
//                String dirName = "src/results/UpdateRunTime_BITSET_FIXBPC/k" + k + "_maxfiles_" + maxfiles;
//                Path dirPath = Paths.get(dirName);
//                if (!Files.exists(dirPath)) {
//                    Files.createDirectories(dirPath);
//                }
//
//                // 遍历剩下的参数 l、expTimes 和 order 进行实验
//                for (int l : lList) {
//                    for (int expTimes : expTimesList) {
//                        for (int order : orderList) {
//                            // 文件名前缀使用类名，结果保存在新创建的目录中
//                            for (String scheme : schemes) {
//                                String fileName = dirName + "/" + scheme + "_order_" + order + "_maxfiles_" + maxfiles
//                                        + "_keywordsnum_" + k + "_rangelen_" + (2 * l) + "_exptimes_" + expTimes + ".txt";
//                                try (PrintStream out = new PrintStream(Files.newOutputStream(Paths.get(fileName)))) {
//                                    // 创建不同的方案对象并运行实验
//                                    if (scheme.equals("SPQS_BITSET")) {
//                                        SPQS_BITSET spqs = new SPQS_BITSET(maxfiles,order, 2);
//                                        spqs.setup(SPQS_BITSET.LAMBDA, maxfiles);
//                                        runExperiments(spqs, k, l, expTimes, filepath, maxfiles, out,dirName);
//                                    } else if (scheme.equals("TDSC2023_BITSET")) {
//                                        TDSC2023_BITSET tdsc2023_BITSET = new TDSC2023_BITSET(128, 500, maxfiles, order, 2);
//                                        runExperiments(tdsc2023_BITSET, k, l, expTimes, filepath, maxfiles, out, dirName);
//                                    }
//
//                                    System.out.println("实验结果保存至: " + fileName);
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    // 运行实验，适用于不同的方案对象
//    private static void runExperiments(Object scheme, int k, int l, int expTimes, String filepath, int maxfiles, PrintStream out, String dirName) throws Exception {
//         // 模拟从数据集中获取一项数据
//        Object[] test = SPQS_BITSET.GetRandomItem(12, filepath);
//        if (test == null) {
////            System.out.println("获取数据失败。");
//            return;
//        }
//        int[] files = new int[]{((BigInteger) test[0]).intValue()};
//        long[] pSet = (long[]) test[1];
//        String[] W = (String[]) test[2];
//
//        String[] WQ = getRandomKeywords(W, k);
//        System.out.println("自动选取的关键字: ");
//        for (String keyword : WQ) {
//            System.out.print(keyword + " ");
//        }
//        System.out.println();
//
//        // 进行一次update和search,不计算本次运行时间
//        if (scheme instanceof SPQS_BITSET) {
//            SPQS_BITSET spqs = (SPQS_BITSET) scheme;
//            spqs.ObjectUpdate(pSet, W, "add", new int[]{1}, 500);
//            BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSet);
//            BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
//            BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
//            // Search
//            BigInteger BR = spqs.ObjectSearch(R_min, R_max, WQ);
//        } else if (scheme instanceof TDSC2023_BITSET) {
//            TDSC2023_BITSET tdsc2023 = (TDSC2023_BITSET) scheme;
//            tdsc2023.update(pSet, W, "add", new int[]{1}, 500);
//            BigInteger pointHilbertIndex = tdsc2023.hilbertCurve.index(pSet);
//            BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
//            BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
//            // Search
//            BigInteger BR = tdsc2023.Search(R_min, R_max, WQ);
//        }
//
//        System.setOut(out);  // 重定向输出
//        System.out.println("正式实验: ");
//        // 初始化一个存储实验结果的二维数组
//        Object[][] results = new Object[expTimes][3];
//
//        // 获取 expTimes 次的随机数据，并将其存储到 results 中
//        for (int i = 0; i < expTimes; i++) {
//            Object[] result = SPQS_BITSET.GetRandomItem(12, filepath);
//            if (result == null) {
//                System.out.println("获取数据失败。");
//                return;
//            }
//            results[i] = result;
//        }
//        for (int i = 0; i < expTimes; i++) {
//            files = new int[]{((BigInteger) results[i][0]).intValue()}; // 从 results 中获取文件
//            pSet = (long[]) results[i][1]; // 从 results 中获取 pSet
//            W = (String[]) results[i][2]; // 从 results 中获取 W
//            int progress = (int) ((i + 1) / (double) expTimes * 100);
//            System.err.println(" 实验进度: " + progress + "%");
//            System.out.println("实验次数: " + (i + 1));
//
//            // 执行 Update
//            if (scheme instanceof SPQS_BITSET) {
//                SPQS_BITSET spqs = (SPQS_BITSET) scheme;
//                BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSet);
//                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
//                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
//                System.out.println("自动生成的区间: R_min = " + R_min + ", R_max = " + R_max);
//                runSPQSExperiment(spqs, files, pSet, W, R_min, R_max, WQ);
//            } else if (scheme instanceof TDSC2023_BITSET) {
//                TDSC2023_BITSET tdsc2023 = (TDSC2023_BITSET) scheme;
//                BigInteger pointHilbertIndex = tdsc2023.hilbertCurve.index(pSet);
//                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
//                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
//                System.out.println("自动生成的区间: R_min = " + R_min + ", R_max = " + R_max);
//                runTDSC2023Experiment(tdsc2023, files, pSet, W, R_min, R_max, WQ);
//            }
//            System.out.println("第" + (i + 1) + "次实验完成。\n");
//        }
//
//        // 重定向输出到新的文件 fileName + "search"
//        String searchFileName = dirName + "/" +  scheme + "_keywordsnum_" + k + "_rangelen_" + (2 * l) + "_exptimes_" + expTimes + "_search.txt";
//        try (PrintStream searchOut = new PrintStream(Files.newOutputStream(Paths.get(searchFileName)))) {
//            System.setOut(searchOut);
//            if (scheme instanceof SPQS_BITSET) {
//                SPQS_BITSET spqs = (SPQS_BITSET) scheme;
//                BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSet);
//                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
//                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
//                BigInteger BR = spqs.ObjectSearch(R_min, R_max, WQ);
//            } else if (scheme instanceof TDSC2023_BITSET) {
//                TDSC2023_BITSET tdsc2023 = (TDSC2023_BITSET) scheme;
//                BigInteger pointHilbertIndex = tdsc2023.hilbertCurve.index(pSet);
//                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
//                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
//                BigInteger BR = tdsc2023.Search(R_min, R_max, WQ);
//            }
//        }
//    }
//    // 执行 SPQS_BITSET 方案的实验
//    private static void runSPQSExperiment(SPQS_BITSET spqs, int[] files, long[] pSet, String[] W, BigInteger R_min, BigInteger R_max, String[] WQ) throws Exception {
//        // Add Update
//        spqs.ObjectUpdate(pSet, W, "add", files, 500);
//        // Search
////        BigInteger BR = spqs.ObjectSearch(R_min, R_max, WQ);
//        // 返回最终解密后的位图信息
//        // spqs.findIndexesOfOne(BR);
//
//        // Del Update
////        spqs.ObjectUpdate(pSet, W, "del", files, 500);
//    }
//
//    // 执行 TDSC2023_BITSET 方案的实验
//    private static void runTDSC2023Experiment(TDSC2023_BITSET tdsc2023, int[] files, long[] pSet, String[] W, BigInteger R_min, BigInteger R_max, String[] WQ) throws Exception {
//        // Add Update
//        tdsc2023.update(pSet, W, "add", files, 500);
//
//        // Search
////        BigInteger BR = tdsc2023.Search(R_min, R_max, WQ);
//        // 返回最终解密后的位图信息
//        // tdsc2023.homomorphicEncryption.findIndexesOfOne(BR);
//
//        // Del Update
////        tdsc2023.update(pSet, W, "del", files, 500);
//    }
//
//
//    // 随机选取W中的k个关键字
//    private static String[] getRandomKeywords(String[] W, int k) {
//        Random random = new Random();
//        String[] WQ = new String[k];
//        Set<Integer> selectedIndexes = new HashSet<>();
//
//        while (selectedIndexes.size() < k) {
//            int index = random.nextInt(W.length);
//            if (!selectedIndexes.contains(index)) {
//                WQ[selectedIndexes.size()] = W[index];
//                selectedIndexes.add(index);
//            }
//        }
//
//        return WQ;
//    }
//}