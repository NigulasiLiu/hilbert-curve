package org.davidmoten.Experiment.Comparison;

import org.davidmoten.Experiment.Correctness.RSKQ_Biginteger2;
import org.davidmoten.Experiment.Correctness.TDSC2023_Biginteger2;
import org.davidmoten.Scheme.RSKQ.RSKQ_Biginteger;
import org.davidmoten.Scheme.TDSC2023.TDSC2023_Biginteger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UpdateComparison {
    // 统计 CSV 文件的行数
    public static int countLines(String filePath) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            int lines = 0;
            while (br.readLine() != null) {
                lines++;
            }
            return lines;
        }
    }

    public static void main(String[] args) throws Exception {
        String filepath = "src/dataset/spatial_data_set_10W.csv";
        String filepath1 = "src/dataset/spatial_data_set_100W.csv";
        int batchSize = 500; // 每次处理 x 个更新
        int objectnums = 1000000; // 数据集大小
        int searchKeywords = 6; // 关键字数量
        int[] maxfilesArray = {1 << 16, 1 << 18, 1 << 20, 1 << 22}; // maxFiles 参数
        int[] hilbertOrders = {8, 10, 12}; // Hilbert 阶数

        //设置“修改”类型的更新占比
        // 例如，设置 α 为 0.7
        double[] alphas = new double[]{0.2, 0.4, 0.6, 0.8, 1.0}; // 不同的 α 值

        for (double alpha : alphas) {
            System.out.printf("Testing with α = %.2f%n", alpha);

            // 计算总行数，减去表头
            int totalLines = countLines(filepath) - 1;

            // 第一种和第二种情况的次数
            int firstCaseCount = (int) (alpha * totalLines);
            int secondCaseCount = totalLines - firstCaseCount;

            // 填充第一种情况和第二种情况的标记
            ArrayList<Boolean> updateTypes = new ArrayList<>();
            for (int i = 0; i < firstCaseCount; i++) updateTypes.add(true);
            for (int i = 0; i < secondCaseCount; i++) updateTypes.add(false);

            // 打乱标记顺序
            Collections.shuffle(updateTypes);

            // 遍历 maxFilesArray 和 hilbertOrders 的组合
            for (int maxFiles : maxfilesArray) {
                for (int hilbertOrder : hilbertOrders) {
                    System.out.printf("正在初始化 RSKQ_Biginteger 实例 | maxFiles: %d, hilbertOrder: %d\n", maxFiles, hilbertOrder);

                    List<Double> averageRSKQUpdateTimes = new ArrayList<>();
                    List<Double> averageTDSCUpdateTimes = new ArrayList<>();

                    // 初始化 RSKQ_Biginteger 和 TDSC2023_Biginteger 实例
                    RSKQ_Biginteger2 spqs = new RSKQ_Biginteger2(maxFiles, hilbertOrder, 2);
                    TDSC2023_Biginteger2 tdsc2023_biginteger = new TDSC2023_Biginteger2(128, 2147483640, maxFiles, hilbertOrder, 2);

                    String[][] keywordItemSets = new String[objectnums][searchKeywords];
                    int rowCount = 0; // 记录处理的行数

                    // 开始处理 CSV 文件
                    try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
                        String line;
                        boolean isFirstLine = true;

                        while ((line = br.readLine()) != null) {
                            // 跳过表头
                            if (isFirstLine) {
                                isFirstLine = false;
                                continue;
                            }

                            // 按行解析数据
                            String[] fields = line.split(",");
                            if (fields.length < 15) {
                                System.err.println("Invalid row format: " + line);
                                continue;
                            }

                            // 提取数据
                            int fileID = Integer.parseInt(fields[0]) % maxFiles;
                            long pointX = Long.parseLong(fields[1]);
                            long pointY = Long.parseLong(fields[2]);
                            System.arraycopy(fields, 4, keywordItemSets[rowCount], 0, searchKeywords);

                            int[] files;
                            String[] ops;

                            // 构造更新参数
                            long[] pSet = new long[]{pointX, pointY};

                            if (updateTypes.get(rowCount)) {
                                // 第一种情况：修改
                                ops = new String[]{"add","del"};
                                long start = System.nanoTime();
                                spqs.ObjectUpdate(pSet, keywordItemSets[rowCount], ops, new int[]{fileID, (fileID + 1) % maxFiles},100000);
                                double rskq_time = (System.nanoTime() - start)/1e6;
                                long tdsc_time1 = System.nanoTime();
                                tdsc2023_biginteger.update(pSet, keywordItemSets[rowCount], ops[0], new int[]{fileID}, objectnums);
                                tdsc2023_biginteger.update(pSet, keywordItemSets[rowCount], ops[1], new int[]{(fileID + 1) % maxFiles}, objectnums);
                                double tdsc_time2 = (System.nanoTime() - tdsc_time1)/1e6;
                                averageTDSCUpdateTimes.add(tdsc_time2);
                                averageRSKQUpdateTimes.add(rskq_time);
                            } else {
                                // 第二种情况：添加
                                files = new int[]{fileID};
                                ops = new String[]{"add"};
                                long start = System.nanoTime();
                                spqs.ObjectUpdate(pSet, keywordItemSets[rowCount], ops, new int[]{fileID},100000);
                                double rskq_time = (System.nanoTime() - start)/1e6;
                                long tdsc_time1 = System.nanoTime();
                                tdsc2023_biginteger.update(pSet, keywordItemSets[rowCount], ops[0], new int[]{fileID}, objectnums);
                                double tdsc_time2 = (System.nanoTime() - tdsc_time1)/1e6;
                                averageTDSCUpdateTimes.add(tdsc_time2);
                                averageRSKQUpdateTimes.add(rskq_time);
                            }

                            rowCount++;

                            // 更新进度条
//                            printProgressBar(rowCount, totalLines);
                        }
                    }

                    // 计算平均更新时间
                    double avgRskqTime = averageRSKQUpdateTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double avgTdscTime = averageTDSCUpdateTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                    // 打印结果
                    System.out.printf("实例完成α percent Modify Update | maxFiles: %d, hilbertOrder: %d | RSKQ 平均更新时间: %-10.6f ms| TDSC 平均更新时间: %-10.6f ms\n",
                            maxFiles, hilbertOrder, avgRskqTime, avgTdscTime);

                    // 清除实例数据
//                    spqs.clearUpdateTime();
//                    tdsc2023_biginteger.clearUpdateTime();
                }
            }
        }
    }
    // 打印进度条
    public static void printProgressBar(int current, int total) {
        int progressWidth = 50; // 进度条宽度
        double progress = (double) current / total;
        int completed = (int) (progress * progressWidth);

        StringBuilder progressBar = new StringBuilder("[");
        for (int i = 0; i < completed; i++) {
            progressBar.append("#");
        }
        for (int i = completed; i < progressWidth; i++) {
            progressBar.append(" ");
        }
        progressBar.append("] ");
        progressBar.append(String.format("%.2f", progress * 100)).append("%");

        // 打印到控制台
        System.out.print("\r" + progressBar.toString());
    }
//        int div = 100;
//        Random random = new Random();
//        Scanner scanner = new Scanner(System.in);
//        while (true) {
//            System.out.print("\n请选择操作 (1: 搜索, 2: 打印键数量, -1: 退出): ");
//            int choice = scanner.nextInt();
//
//            if (choice == -1) {
//                System.out.println("程序已退出。");
//                break;
//            }
//
//            switch (choice) {
//                case 1: // 搜索操作
//                    System.out.print("请输入搜索次数 (正整数): ");
//                    int searchtimes = scanner.nextInt();
//
//                    if (searchtimes <= 0) {
//                        System.out.println("搜索次数必须是正整数。请重新选择操作。");
//                        continue;
//                    }
//
//                    System.out.print("请输入搜索范围 (0-100%): ");
//                    int searchEdgeLengthPer = scanner.nextInt();
//
//                    if (searchEdgeLengthPer <= 0 || searchEdgeLengthPer > 100) {
//                        System.out.println("搜索范围必须在 0 到 100 之间。请重新选择操作。");
//                        continue;
//                    }
//
//                    int xstart = random.nextInt(edgeLength * (div - searchEdgeLengthPer) / div);
//                    int ystart = random.nextInt(edgeLength * (div - searchEdgeLengthPer) / div);
//                    int searchRange = edgeLength * searchEdgeLengthPer / div;
//
//                    BigInteger[][] matrixToSearch = generateHilbertMatrix(
//                            spqs.hilbertCurve, xstart, ystart, searchRange, searchRange);
//
//                    for (int i = 0; i < searchtimes; i++) {
//                        int indexToSearch = random.nextInt(objectnums);
//                        String[] WQ = keywordItemSets[indexToSearch];
//                        spqs.ObjectSearch(matrixToSearch, WQ);
//                    }
//
//                    // 移除异常值
//                    for (int i = 0; i < delupdatetimes; i++) {
//                        spqs.removeExtremesSearchTime();
//                    }
//
//                    // 打印平均搜索时间
//                    System.out.printf("搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|\n",
//                            spqs.getAverageSearchTime());
//                    break;
//
//                case 2: // 打印 PDB 和 KDB 键的数量
//                    int pdbKeyCount = spqs.PDB.size();
//                    int kdbKeyCount = spqs.KDB.size();
//                    System.out.printf("RSKQ PDB 键的数量: %d, KDB 键的数量: %d\n",
//                            pdbKeyCount, kdbKeyCount);
//                    break;
//
//                default:
//                    System.out.println("无效选项，请重新选择。");
//            }
//        }
//        scanner.close();

}

