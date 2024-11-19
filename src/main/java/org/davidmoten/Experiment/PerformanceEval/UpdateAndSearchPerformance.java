package org.davidmoten.Experiment.PerformanceEval;

import org.davidmoten.Scheme.SPQS.RSKQ_Biginteger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.*;

import static org.davidmoten.Experiment.Comparison.BRQComparison.generateHilbertMatrix;

public class UpdateAndSearchPerformance {

    public static void main(String[] args) throws Exception {
        String csvFilePath = "src/dataset/spatial_data_set.csv";
        String csvFilePath1 = "src/dataset/spatial_data_for_update.csv";
        //清除"最大耗时-最小耗时"对数,便于计算合理的平均值
        int delupdatetimes = 0;
        //需要在内存中存储，所以需要插入updatetime个Object
        int batchSize = 500; // 每次处理x个更新
        //数据集大小为 1 Million 个条目
        int objectnums = 1000000;
        //相同元素(关键字或者位置point)的最大数量为10W
        int rangePredicate = 100000;
        int[] maxfilesArray = {1 << 16};//20,1 << 18,1 << 16,1 << 14,1 << 12
        int[] hilbertOrders = {8};

        int edgeLength = 1 << hilbertOrders[0];
        int Wnum = 8000;
        int attachedKeywords = 12;
        int searchKeywords = 6;
        String distributionType = "multi-gaussian";

        // 初始化 RSKQ_Biginteger
        RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxfilesArray[0], hilbertOrders[0], 2);

        // 读取 CSV 文件并直接处理
        int rowCount = 0; // 记录处理的行数
        String[][] keywordItemSets = new String[objectnums][searchKeywords];
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath1))) {
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
//                String[] keywords = new String[searchKeywords];
                System.arraycopy(fields, 4, keywordItemSets[rowCount], 0, searchKeywords);

                // 构造更新参数
                long[] pSet = new long[]{pointX, pointY};
                int[] files = new int[]{fileID};

                // 更新操作
                spqs.ObjectUpdate(pSet, keywordItemSets[rowCount], new String[]{"add"}, files);
//                tdsc2023.update(pSet, keywords, "add", files, rangePredicate);

                rowCount++;

                // 每处理 batchSize 条记录打印日志
                if (rowCount % batchSize == 0) {
                    System.out.println("Completed batch " + (rowCount / batchSize) + " of updates.");
                    System.out.printf("Update完成，平均更新时间: |RSKQ_Biginteger: |%-10.6f|ms\n",
                            spqs.getAverageUpdateTime());
                }
            }
        }
        int div = 100;
        Random random = new Random();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n请选择操作 (1: 搜索, 2: 打印键数量, -1: 退出): ");
            int choice = scanner.nextInt();

            if (choice == -1) {
                System.out.println("程序已退出。");
                break;
            }

            switch (choice) {
                case 1: // 搜索操作
                    System.out.print("请输入搜索次数 (正整数): ");
                    int searchtimes = scanner.nextInt();

                    if (searchtimes <= 0) {
                        System.out.println("搜索次数必须是正整数。请重新选择操作。");
                        continue;
                    }

                    System.out.print("请输入搜索范围 (0-100%): ");
                    int searchEdgeLengthPer = scanner.nextInt();

                    if (searchEdgeLengthPer <= 0 || searchEdgeLengthPer > 100) {
                        System.out.println("搜索范围必须在 0 到 100 之间。请重新选择操作。");
                        continue;
                    }

                    int xstart = random.nextInt(edgeLength * (div - searchEdgeLengthPer) / div);
                    int ystart = random.nextInt(edgeLength * (div - searchEdgeLengthPer) / div);
                    int searchRange = edgeLength * searchEdgeLengthPer / div;

                    BigInteger[][] matrixToSearch = generateHilbertMatrix(
                            spqs.hilbertCurve, xstart, ystart, searchRange, searchRange);

                    for (int i = 0; i < searchtimes; i++) {
                        int indexToSearch = random.nextInt(objectnums);
                        String[] WQ = keywordItemSets[indexToSearch];
                        spqs.ObjectSearch(matrixToSearch, WQ);
                    }

                    // 移除异常值
                    for (int i = 0; i < delupdatetimes; i++) {
                        spqs.removeExtremesSearchTime();
                    }

                    // 打印平均搜索时间
                    System.out.printf("搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|\n",
                            spqs.getAverageSearchTime());
                    break;

                case 2: // 打印 PDB 和 KDB 键的数量
                    int pdbKeyCount = spqs.PDB.size();
                    int kdbKeyCount = spqs.KDB.size();
                    System.out.printf("RSKQ PDB 键的数量: %d, KDB 键的数量: %d\n",
                            pdbKeyCount, kdbKeyCount);
                    break;

                default:
                    System.out.println("无效选项，请重新选择。");
            }
        }
        scanner.close();
    }
}

