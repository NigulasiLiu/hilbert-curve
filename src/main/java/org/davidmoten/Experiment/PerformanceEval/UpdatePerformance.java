package org.davidmoten.Experiment.PerformanceEval;

import org.davidmoten.Scheme.SPQS.RSKQ_Biginteger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class UpdatePerformance {
    // 统计 CSV 文件的行数
    private static int countLines(String filePath) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            int lines = 0;
            while (br.readLine() != null) {
                lines++;
            }
            return lines;
        }
    }
    public static void main(String[] args) throws Exception {
        String csvFilePath = "src/dataset/spatial_data_for_update.csv";
        String csvFilePath1 = "src/dataset/spatial_data_for_update.csv";
        int batchSize = 500; // 每次处理 x 个更新
        int objectnums = 1000000; // 数据集大小
        int searchKeywords = 6; // 关键字数量
        int[] maxfilesArray = {1 << 16, 1 << 20, 1 << 24}; // maxFiles 参数
        int[] hilbertOrders = {8, 10, 12}; // Hilbert 阶数

        // 记录每种实例的平均更新时间
        List<Double> averageUpdateTimes = new ArrayList<>();

        // 遍历 maxfilesArray 和 hilbertOrders 的组合
        for (int maxFiles : maxfilesArray) {
            for (int hilbertOrder : hilbertOrders) {
                System.out.printf("正在初始化 RSKQ_Biginteger 实例 | maxFiles: %d, hilbertOrder: %d\n", maxFiles, hilbertOrder);

                // 初始化 RSKQ_Biginteger 实例
                RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxFiles, hilbertOrder, 2);

                // 开始处理 CSV 文件
                int rowCount = 0; // 记录处理的行数
                String[][] keywordItemSets = new String[objectnums][searchKeywords];
                int totalLines = countLines(csvFilePath) - 1; // 计算总行数，减去表头行
                try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
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
                        int fileID = Integer.parseInt(fields[0]);
                        long pointX = Long.parseLong(fields[1]);
                        long pointY = Long.parseLong(fields[2]);
                        System.arraycopy(fields, 4, keywordItemSets[rowCount], 0, searchKeywords);

                        // 构造更新参数
                        long[] pSet = new long[]{pointX, pointY};
                        int[] files = new int[]{fileID};

                        // 更新操作
                        spqs.ObjectUpdate(pSet, keywordItemSets[rowCount], new String[]{"add"}, files);

                        rowCount++;

                        // 更新进度条
                        printProgressBar(rowCount, totalLines);
//                        // 每处理 batchSize 条记录打印日志
//                        if (rowCount % batchSize == 0) {
//                            System.out.println("Completed batch " + (rowCount / batchSize) + " of updates.");
//                            System.out.printf("Update完成，平均更新时间: |RSKQ_Biginteger: |%-10.6f|ms\n",
//                                    spqs.getAverageUpdateTime());
//                        }
                    }
                }

                // 记录当前实例的平均更新时间
                double averageTime = spqs.getAverageUpdateTime();
                averageUpdateTimes.add(averageTime);
                System.out.printf("实例完成 | maxFiles: %d, hilbertOrder: %d | 平均更新时间: %-10.6f ms\n", maxFiles, hilbertOrder, averageTime);
            }
        }
    }
    // 打印进度条
    private static void printProgressBar(int current, int total) {
        int progressWidth = 50; // 进度条宽度
        double progress = (double) current / total;
        int completed = (int) (progress * progressWidth);

        String progressBar = "[" + "#".repeat(Math.max(0, completed)) +
                " ".repeat(Math.max(0, progressWidth - completed)) +
                "] " +
                String.format("%.2f", progress * 100) + "%";

        // 打印到控制台
        System.out.print("\r" + progressBar);
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

