package org.davidmoten.PerformanceEval;

import org.davidmoten.Scheme.SPQS.SPQS_Biginteger;
import org.davidmoten.Scheme.TDSC2023.TDSC2023_Biginteger;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.*;

public class RunningTimeExp {
    public static void main(String[] args) throws Exception {
        // 定义实验参数
        String filepath = "src/dataset/spatialdata.csv";
        List<Integer> kList = Arrays.asList(2, 4, 6, 8); // 随机选取的关键字数量
        List<Integer> lList = Arrays.asList(10, 15, 20, 25); // R_min 和 R_max 之间的差距
        List<Integer> expTimesList = Arrays.asList(20, 40, 60, 80); // 实验次数
        List<Integer> maxfilesList = Arrays.asList(1 << 12, 1 << 14, 1 << 16, 1 << 18, 1 << 20); // 文件数量
        List<Integer> orderList = Arrays.asList(15, 16, 17); // Hilbert curve 级别
        List<String> schemes = Arrays.asList("SPQS_Biginteger", "TDSC2023_Biginteger");  // 待测试方案列表

        // 遍历所有的 k 和 maxfiles 组合
        for (int k : kList) {
            for (int maxfiles : maxfilesList) {
                // 创建输出目录
                String dirName = "src/results/UpdateRunTime/k" + k + "_maxfiles_" + maxfiles;
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
                                    if (scheme.equals("SPQS_Biginteger")) {
                                        SPQS_Biginteger spqsBiginteger = new SPQS_Biginteger(maxfiles,order, 2);
                                        spqsBiginteger.setup(SPQS_Biginteger.LAMBDA, maxfiles);
                                        runExperiments(spqsBiginteger, k, l, expTimes, filepath, maxfiles, out,dirName);
                                    } else if (scheme.equals("TDSC2023_Biginteger")) {
                                        TDSC2023_Biginteger tdsc2023Biginteger = new TDSC2023_Biginteger(128, 200, maxfiles, order, 2);
                                        runExperiments(tdsc2023Biginteger, k, l, expTimes, filepath, maxfiles, out, dirName);
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
    }

    // 运行实验，适用于不同的方案对象
    private static void runExperiments(Object scheme, int k, int l, int expTimes, String filepath, int maxfiles, PrintStream out, String dirName) throws Exception {
        // 模拟从数据集中获取一项数据
        Object[] result = SPQS_Biginteger.GetRandomItem(12, filepath);
        if (result == null) {
            System.out.println("获取数据失败。");
            return;
        }
        int[] files = new int[]{maxfiles};
        long[] pSet = (long[]) result[1];
        String[] W = (String[]) result[2];

        String[] WQ = getRandomKeywords(W, k);
        System.out.println("自动选取的关键字: ");
        for (String keyword : WQ) {
            System.out.print(keyword + " ");
        }
        System.out.println();

        // 进行一次update和search,不计算本次运行时间
        if (scheme instanceof SPQS_Biginteger) {
            SPQS_Biginteger spqsBiginteger = (SPQS_Biginteger) scheme;
            spqsBiginteger.ObjectUpdate(pSet, W, "add", new int[]{1}, 200);
            BigInteger pointHilbertIndex = spqsBiginteger.hilbertCurve.index(pSet);
            BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
            BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
            // Search
            BigInteger BR = spqsBiginteger.ObjectSearch(R_min, R_max, WQ);
        } else if (scheme instanceof TDSC2023_Biginteger) {
            TDSC2023_Biginteger tdsc2023Biginteger = (TDSC2023_Biginteger) scheme;
            tdsc2023Biginteger.update(pSet, W, "add", new int[]{1}, 200);
            BigInteger pointHilbertIndex = tdsc2023Biginteger.hilbertCurve.index(pSet);
            BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
            BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
            // Search
            BigInteger BR = tdsc2023Biginteger.Search(R_min, R_max, WQ);
        }

        System.setOut(out);  // 重定向输出
        System.out.println("正式实验: ");
        for (int i = 0; i < expTimes; i++) {
            files[0]--;
            int progress = (int) ((i + 1) / (double) expTimes * 100);
            System.err.println(" 实验进度: " + progress + "%");
            System.out.println("实验次数: " + (i + 1));

            // 执行 Update
            if (scheme instanceof SPQS_Biginteger) {
                SPQS_Biginteger spqsBiginteger = (SPQS_Biginteger) scheme;
                BigInteger pointHilbertIndex = spqsBiginteger.hilbertCurve.index(pSet);
                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
                System.out.println("自动生成的区间: R_min = " + R_min + ", R_max = " + R_max);
                runSPQSExperiment(spqsBiginteger, files, pSet, W, R_min, R_max, WQ);
            } else if (scheme instanceof TDSC2023_Biginteger) {
                TDSC2023_Biginteger tdsc2023Biginteger = (TDSC2023_Biginteger) scheme;
                BigInteger pointHilbertIndex = tdsc2023Biginteger.hilbertCurve.index(pSet);
                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
                System.out.println("自动生成的区间: R_min = " + R_min + ", R_max = " + R_max);
                runTDSC2023Experiment(tdsc2023Biginteger, files, pSet, W, R_min, R_max, WQ);
            }
            System.out.println("第" + (i + 1) + "次实验完成。\n");
        }

        // 重定向输出到新的文件 fileName + "search"
        String searchFileName = dirName + "/" +  scheme + "_keywordsnum_" + k + "_rangelen_" + (2 * l) + "_exptimes_" + expTimes + "_search.txt";
        try (PrintStream searchOut = new PrintStream(Files.newOutputStream(Paths.get(searchFileName)))) {
            System.setOut(searchOut);
            if (scheme instanceof SPQS_Biginteger) {
                SPQS_Biginteger spqsBiginteger = (SPQS_Biginteger) scheme;
                BigInteger pointHilbertIndex = spqsBiginteger.hilbertCurve.index(pSet);
                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
                BigInteger BR = spqsBiginteger.ObjectSearch(R_min, R_max, WQ);
            } else if (scheme instanceof TDSC2023_Biginteger) {
                TDSC2023_Biginteger tdsc2023Biginteger = (TDSC2023_Biginteger) scheme;
                BigInteger pointHilbertIndex = tdsc2023Biginteger.hilbertCurve.index(pSet);
                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));
                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));
                BigInteger BR = tdsc2023Biginteger.Search(R_min, R_max, WQ);
            }
        }
    }
    // 执行 SPQS_Biginteger 方案的实验
    private static void runSPQSExperiment(SPQS_Biginteger spqsBiginteger, int[] files, long[] pSet, String[] W, BigInteger R_min, BigInteger R_max, String[] WQ) throws Exception {
        // Add Update
        spqsBiginteger.ObjectUpdate(pSet, W, "add", files, 200);
        // Search
//        BigInteger BR = spqsBiginteger.ObjectSearch(R_min, R_max, WQ);
        // 返回最终解密后的位图信息
        // spqsBiginteger.findIndexesOfOne(BR);

        // Del Update
//        spqsBiginteger.ObjectUpdate(pSet, W, "del", files, 200);
    }

    // 执行 TDSC2023_Biginteger 方案的实验
    private static void runTDSC2023Experiment(TDSC2023_Biginteger tdsc2023Biginteger, int[] files, long[] pSet, String[] W, BigInteger R_min, BigInteger R_max, String[] WQ) throws Exception {
        // Add Update
        tdsc2023Biginteger.update(pSet, W, "add", files, 200);

        // Search
//        BigInteger BR = tdsc2023Biginteger.Search(R_min, R_max, WQ);
        // 返回最终解密后的位图信息
        // tdsc2023Biginteger.homomorphicEncryption.findIndexesOfOne(BR);

        // Del Update
//        tdsc2023Biginteger.update(pSet, W, "del", files, 200);
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

//    public static void main(String[] args) throws Exception {
//        SPQS_Biginteger spqs = new SPQS_Biginteger();
//        spqs.setup(LAMBDA, maxfiles);
//
//        // 初始化DSSE系统
//        int securityParameter = 128;
//        TDSC2023_Biginteger TDSC2023_Biginteger = new TDSC2023_Biginteger(securityParameter);
//        // 定义k值，表示从W中随机选取的关键字数量
//        int k = 5;
//        int l = 10;
//        // 定义实验次数
//        int expTimes = 11;
//
//        // 文件名: RunningTimeEval_keywordsnum_k_rangelen_2l_exptimes_expTimes.txt
//        String fileName = "src/results/RunningTimeEval_keywordsnum_" + k + "_rangelen_" + (2 * l) + "_exptimes_" + expTimes + ".txt";
//
//        // 创建文件写入流，并重定向System.out
//        try (PrintStream out = new PrintStream(new FileOutputStream(fileName))) {
//            // 将所有标准输出重定向到文件
//            System.setOut(out);
//
//            // 主流程测试，循环expTimes次
//            for (int i = 0; i < expTimes; i++) {
//                System.out.println("实验次数: " + (i + 1));
//
//                // 模拟从数据集中获取一项数据
//                Object[] result = SPQS_Biginteger.GetRandomItem(12);
//                if (result == null) {
//                    System.out.println("获取数据失败。");
//                    continue;
//                }
//
//                int[] files = new int[]{((BigInteger) result[0]).intValue()};
//                long[] pSet = (long[]) result[1];
//                System.out.println("pSet:" + Arrays.toString(pSet));
//                String[] W = (String[]) result[2];
//                BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSet);
//
//                // 自动生成一个包含pointHilbertIndex的连续区间
//                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(l));  // 生成较小的区间
//                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(l));       // 生成较大的区间
//
//                System.out.println("自动生成的区间: R_min = " + R_min + ", R_max = " + R_max);
//
//                // 随机选取W中的k个关键字
//                String[] WQ = getRandomKeywords(W, k);
//                System.out.println("自动选取的关键字: ");
//                for (String keyword : WQ) {
//                    System.out.println(keyword);
//                }
//
//                // 统计 update 方法的运行时间
//                spqs.ObjectUpdate(pSet, W, "add", files, maxnums_w);
//
//                // 执行搜索操作
//                BigInteger BR = spqs.ObjectSearch(R_min, R_max, WQ);
//                // 返回最终解密后的位图信息 BR
//                // SPQS_Biginteger.findIndexesOfOne(BR); // 替换为实际处理逻辑
//
//                spqs.ObjectUpdate(pSet, W, "del", files, maxnums_w);
//
//                System.out.println("第" + (i + 1) + "次实验完成。\n");
//            }
//
//            System.out.println("所有实验结束。");
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("实验结果保存至: " + fileName);
//    }