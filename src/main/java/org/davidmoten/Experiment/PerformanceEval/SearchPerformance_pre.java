//package org.davidmoten.Experiment.PerformanceEval;
//
//import org.davidmoten.Scheme.RSKQ.RSKQ_Biginteger;
//import org.davidmoten.Scheme.TDSC2023.SKQ_Biginteger;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.math.BigInteger;
//import java.util.*;
//
//import static org.davidmoten.Experiment.TestByUserInput.BRQComparisonInput.generateHilbertMatrix;
//import static org.davidmoten.Experiment.PerformanceEval.UpdatePerformance.countLines;
//
//public class SearchPerformance_pre {
//
//    public static void main(String[] args) throws Exception {
//        String filepath = "src/dataset/spatial_data_for_update.csv";
//        //清除"最大耗时-最小耗时"对数,便于计算合理的平均值
//        int delupdatetimes = 0;
//        //需要在内存中存储，所以需要插入updatetime个Object
//        int batchSize = 500; // 每次处理x个更新
//        //数据集大小为 10W 个条目
//        int[] objectnums = new int[]{20000,40000,60000,80000,100000};
//        //相同元素(关键字或者位置point)的最大数量为10W
//        int rangePredicate = 100000;
//        int[] maxfilesArray = {1 << 20};//20,1 << 18,1 << 16,1 << 14,1 << 12
//        int[] hilbertOrders = {12};
//
//        int edgeLength = 1 << hilbertOrders[0];
//        int Wnum = 8000;
//        int attachedKeywords = 12;
//        int searchKeywords = 6;
//        String distributionType = "multi-gaussian";
//        int div = 100;
//        int[] searchtimes = {9000,1000};
//        int[] rangeList = {3,6,9};
////        List<Double> rskqtimes = new ArrayList<>();
////        List<Double> tdsctimes = new ArrayList<>();
//        Random random = new Random();
//        System.out.printf("实例 maxFiles: %d\n", maxfilesArray[0]);
//        for(int objectnum:objectnums){
//            for (int range : rangeList) {
//                System.out.printf("GRQ Range %d%%\n", range);
//                for (int hilbertOrder : hilbertOrders) {
//                    System.out.printf("正在初始化实例 | hilbertOrder: %d\n", hilbertOrder);
//                    RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxfilesArray[0], hilbertOrder, 2);
//                    SKQ_Biginteger tdsc2023 = new SKQ_Biginteger(128, rangePredicate, maxfilesArray[0], hilbertOrder, 2);
//
//                    int totalLines = countLines(filepath) - 1; // 计算总行数，减去表头行
//                    // 读取 CSV 文件并直接处理
//                    int rowCount = 0; // 记录处理的行数
//                    String[][] keywordItemSets = new String[objectnum][searchKeywords];
//                    try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
//                        String line;
//                        boolean isFirstLine = true;
//
//                        while ((line = br.readLine()) != null) {
//                            // 跳过表头
//                            if (isFirstLine) {
//                                isFirstLine = false;
//                                continue;
//                            }
//                            // 按行解析数据
//                            String[] fields = line.split(",");
//                            if (fields.length < 15) {
//                                System.err.println("Invalid row format: " + line);
//                                continue;
//                            }
//                            // 提取数据
//                            int fileID = Integer.parseInt(fields[0]);
//                            long pointX = Long.parseLong(fields[1]);
//                            long pointY = Long.parseLong(fields[2]);
////                String[] keywords = new String[searchKeywords];
//                            System.arraycopy(fields, 4, keywordItemSets[rowCount], 0, searchKeywords);
//                            // 构造更新参数
//                            long[] pSet = new long[]{pointX, pointY};
//                            int[] files = new int[]{fileID};
//                            // 更新操作
//                            spqs.ObjectUpdate(pSet, keywordItemSets[rowCount], new String[]{"add"}, files);
//                            tdsc2023.update(pSet, keywordItemSets[rowCount], "add", files, rangePredicate);
//
//                            rowCount++;
//                            // 更新进度条
////                            printProgressBar(rowCount, totalLines);
//                        }
//                    }
//                    //预先搜索，每个区域大小为[1% * 1%]尝试聚合
//                    int xstart = random.nextInt(edgeLength * (div - 1) / div);
//                    int ystart = random.nextInt(edgeLength * (div - 1) / div);
//                    int searchRange = edgeLength / div;
//                    BigInteger[][] matrixToSearch = generateHilbertMatrix(
//                            spqs.hilbertCurve, xstart, ystart, searchRange, searchRange);
//                    for (int i = 0; i < searchtimes[0]; i++) {
//                        int indexToSearch = random.nextInt(objectnum);
//                        String[] WQ = keywordItemSets[indexToSearch];
////                    long startTime = System.nanoTime();
//                        spqs.ObjectSearch(matrixToSearch, WQ);
////                    long rskqTime = System.nanoTime()-startTime;
////                    rskqtimes.add(rskqTime/1e6);
//                        tdsc2023.Search(matrixToSearch, WQ);
////                    tdsctimes.add((System.nanoTime()-rskqTime)/1e6);
//                        if(i>0&&i%1000==0){
//                            // 打印平均搜索时间
//                            System.out.printf("\n %d次预先搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|| TDSC_Biginteger: |%-10.6f|ms|\n",
//                                    i,
//                                    spqs.getAverageSearchTime(),
//                                    tdsc2023.getAverageSearchTime());
//                            spqs.clearSearchTime();
//                            tdsc2023.clearSearchTime();
////                        System.out.printf("\n 外部计时: |RSKQ_Biginteger: |%-10.6f|ms|| TDSC_Biginteger: |%-10.6f|ms|\n",
////                                rskqtimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
////                                tdsctimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
////                        rskqtimes.clear();
////                        tdsctimes.clear();
//                        }
//                    }
//                    // 打印平均搜索时间
//                    System.out.printf("\n预先搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|| TDSC_Biginteger: |%-10.6f|ms|\n",
//                            spqs.getAverageSearchTime(),
//                            tdsc2023.getAverageSearchTime());
//                    spqs.clearSearchTime();
//                    tdsc2023.clearSearchTime();
////                System.out.printf("\n 外部计时: |RSKQ_Biginteger: |%-10.6f|ms|| TDSC_Biginteger: |%-10.6f|ms|\n",
////                        rskqtimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
////                        tdsctimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
////                rskqtimes.clear();
////                tdsctimes.clear();
//
//                    //正式搜索,每个区域[rang% * range%]
//                    xstart = random.nextInt(edgeLength * (div - range) / div);
//                    ystart = random.nextInt(edgeLength * (div - range) / div);
//                    searchRange = edgeLength * range / div;
//                    matrixToSearch = generateHilbertMatrix(
//                            spqs.hilbertCurve, xstart, ystart, searchRange, searchRange);
//                    for (int i = 0; i < searchtimes[1]; i++) {
//                        int indexToSearch = random.nextInt(objectnum);
//                        String[] WQ = keywordItemSets[indexToSearch];
////                    long startTime = System.nanoTime();
//                        spqs.ObjectSearch(matrixToSearch, WQ);
////                    long rskqTime = System.nanoTime()-startTime;
////                    rskqtimes.add(rskqTime/1e6);
//                        tdsc2023.Search(matrixToSearch, WQ);
////                    tdsctimes.add((System.nanoTime()-rskqTime)/1e6);
//                    }
//                    // 打印平均搜索时间
//                    System.out.printf("\n正式搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|| TDSC_Biginteger: |%-10.6f|ms|\n",
//                            spqs.getAverageSearchTime(),
//                            tdsc2023.getAverageSearchTime());
////                System.out.printf("\n 外部计时: |RSKQ_Biginteger: |%-10.6f|ms|| TDSC_Biginteger: |%-10.6f|ms|\n",
////                        rskqtimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
////                        tdsctimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
//                }
//            }
//        }
////        Scanner scanner = new Scanner(System.in);
////        while (true) {
////            System.out.print("\n请选择操作 (1: 搜索, 2: 打印键数量, -1: 退出): ");
////            int choice = scanner.nextInt();
////
////            if (choice == -1) {
////                System.out.println("程序已退出。");
////                break;
////            }
////
////            switch (choice) {
////                case 1: // 搜索操作
////                    System.out.print("请输入搜索次数 (正整数): ");
////                    int searchtimes = scanner.nextInt();
////
////                    if (searchtimes <= 0) {
////                        System.out.println("搜索次数必须是正整数。请重新选择操作。");
////                        continue;
////                    }
////
////                    System.out.print("请输入搜索范围 (0-100%): ");
////                    int searchEdgeLengthPer = scanner.nextInt();
////
////                    if (searchEdgeLengthPer <= 0 || searchEdgeLengthPer > 100) {
////                        System.out.println("搜索范围必须在 0 到 100 之间。请重新选择操作。");
////                        continue;
////                    }
////
////                    int xstart = random.nextInt(edgeLength * (div - searchEdgeLengthPer) / div);
////                    int ystart = random.nextInt(edgeLength * (div - searchEdgeLengthPer) / div);
////                    int searchRange = edgeLength * searchEdgeLengthPer / div;
////
////                    BigInteger[][] matrixToSearch = generateHilbertMatrix(
////                            spqs.hilbertCurve, xstart, ystart, searchRange, searchRange);
////
////                    for (int i = 0; i < searchtimes; i++) {
////                        int indexToSearch = random.nextInt(objectnums);
////                        String[] WQ = keywordItemSets[indexToSearch];
////                        spqs.ObjectSearch(matrixToSearch, WQ);
////                    }
////
////                    // 打印平均搜索时间
////                    System.out.printf("搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|\n",
////                            spqs.getAverageSearchTime());
////                    break;
////
////                case 2: // 打印 PDB 和 KDB 键的数量
////                    int pdbKeyCount = spqs.PDB.size();
////                    int kdbKeyCount = spqs.KDB.size();
////                    System.out.printf("RSKQ PDB 键的数量: %d, KDB 键的数量: %d\n",
////                            pdbKeyCount, kdbKeyCount);
////                    break;
////
////                default:
////                    System.out.println("无效选项，请重新选择。");
////            }
////        }
////        scanner.close();
//    }
//}
//
