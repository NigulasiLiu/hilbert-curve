//package org.davidmoten.Experiment.PerformanceEval;
//
//import org.davidmoten.Scheme.RSKQ.RSKQ_Biginteger;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.util.ArrayList;
//import java.util.List;
//
//public class UpdatePerformance {
//    // 统计 CSV 文件的行数
//    public static int countLines(String filePath) throws Exception {
//        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
//            int lines = 0;
//            while (br.readLine() != null) {
//                lines++;
//            }
//            return lines;
//        }
//    }
//
//    public static void main(String[] args) throws Exception {
//        String csvFilePath = "src/dataset/spatial_data_set_7K.csv";
//        int batchSize = 500; // 每次处理 x 个更新
//        int objectnums = 100000; // 数据集大小
//        int searchKeywords = 6; // 关键字数量
//        int[] maxfilesArray = {1 << 16, 1 << 18, 1 << 20, 1 << 22}; // maxFiles 参数
//        int[] hilbertOrders = {8, 10, 12}; // Hilbert 阶数
//
//        int totalLines = countLines(csvFilePath) - 1; // 计算总行数，减去表头行
//        // 遍历 maxfilesArray 和 hilbertOrders 的组合
//        for (int maxFiles : maxfilesArray) {
//            for (int hilbertOrder : hilbertOrders) {
//                System.out.printf("正在初始化 RSKQ_Biginteger 实例 | maxFiles: %d, hilbertOrder: %d\n", maxFiles, hilbertOrder);
//
//                String[][] keywordItemSets = new String[objectnums][searchKeywords];
//                // 初始化 RSKQ_Biginteger 实例
//                RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxFiles, hilbertOrder, 2);
////                // 开始处理 CSV 文件
////                int rowCount = 0; // 记录处理的行数
////                try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
////                    String line;
////                    boolean isFirstLine = true;
////
////                    while ((line = br.readLine()) != null) {
////                        // 跳过表头
////                        if (isFirstLine) {
////                            isFirstLine = false;
////                            continue;
////                        }
////
////                        // 按行解析数据
////                        String[] fields = line.split(",");
////                        if (fields.length < 15) {
////                            System.err.println("Invalid row format: " + line);
////                            continue;
////                        }
////
////                        // 提取数据
////                        int fileID = Integer.parseInt(fields[0]) % maxFiles;
////                        long pointX = Long.parseLong(fields[1]);
////                        long pointY = Long.parseLong(fields[2]);
////                        System.arraycopy(fields, 4, keywordItemSets[rowCount], 0, searchKeywords);
////
////                        // 构造更新参数
////                        long[] pSet = new long[]{pointX, pointY};
////                        int[] files = new int[]{fileID,(fileID+1)%maxFiles};
////
////                        // 更新操作
////                        spqs.ObjectUpdate(pSet, keywordItemSets[rowCount], new String[]{"del","add"}, files);
////
////                        rowCount++;
////
////                        // 更新进度条
////                        printProgressBar(rowCount, totalLines);
//////                        // 每处理 batchSize 条记录打印日志
//////                        if (rowCount % batchSize == 0) {
//////                            System.out.println("Completed batch " + (rowCount / batchSize) + " of updates.");
//////                            System.out.printf("Update完成，平均更新时间: |RSKQ_Biginteger: |%-10.6f|ms\n",
//////                                    spqs.getAverageUpdateTime());
//////                        }
////                    }
////                }
////                // 记录当前实例的平均更新时间
////                double averageTime = spqs.getAverageUpdateTime();
////                averageOnlyUpdateTimes.add(averageTime);
////                System.out.printf("实例完成Only Update | maxFiles: %d, hilbertOrder: %d | 平均更新时间: %-10.6f ms\n", maxFiles, hilbertOrder, averageTime);
////                spqs.clearUpdateTime();
//                int rowCount = 0; // 第二次进行Del and Add
//                try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
//                    String line;
//                    boolean isFirstLine = true;
//
//                    while ((line = br.readLine()) != null) {
//                        // 跳过表头
//                        if (isFirstLine) {
//                            isFirstLine = false;
//                            continue;
//                        }
//
//                        // 按行解析数据
//                        String[] fields = line.split(",");
//                        if (fields.length < 15) {
//                            System.err.println("Invalid row format: " + line);
//                            continue;
//                        }
//
//                        // 提取数据
//                        int fileID = Integer.parseInt(fields[0]) % maxFiles;
//                        long pointX = Long.parseLong(fields[1]);
//                        long pointY = Long.parseLong(fields[2]);
//                        System.arraycopy(fields, 4, keywordItemSets[rowCount], 0, searchKeywords);
//
//                        // 构造更新参数
//                        long[] pSet = new long[]{pointX, pointY};
//                        int[] files = new int[]{fileID,(fileID+1)%maxFiles};
//
//                        // 更新操作
//                        spqs.ObjectUpdate(pSet, keywordItemSets[rowCount], new String[]{"del","add"}, files);
//
//                        rowCount++;
//
//                        // 更新进度条
////                        printProgressBar(rowCount, totalLines);
//                    }
//                }
//                System.out.printf("实例完成Modify Update | maxFiles: %d, hilbertOrder: %d | 平均更新时间: %-10.6f ms\n",
//                        maxFiles, hilbertOrder, spqs.getAverageUpdateTime());
//            }
//        }
//    }
//
//
//}
//
