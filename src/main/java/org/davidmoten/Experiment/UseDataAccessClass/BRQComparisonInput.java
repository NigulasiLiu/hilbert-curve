package org.davidmoten.Experiment.UseDataAccessClass;

import org.davidmoten.DataProcessor.DataSetAccess;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;
import org.davidmoten.Scheme.RSKQ.RSKQ_Biginteger;
import org.davidmoten.Scheme.TDSC2023.TDSC2023_Biginteger;

import java.math.BigInteger;
import java.util.*;


public class BRQComparisonInput {
    public static BigInteger[][] generateHilbertMatrix(HilbertCurve hilbertCurve, int startX, int startY, int width, int height) {
        BigInteger[][] matrix = new BigInteger[width + 1][height + 1];

        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= height; y++) {
                matrix[x][y] = hilbertCurve.index(startX + x, startY + y);
            }
        }

        return matrix;
    }

    public static void main(String[] args) throws Exception {
        //清除"最大耗时-最小耗时"对数,便于计算合理的平均值
        int delupdatetimes = 1;
        //需要在内存中存储，所以需要插入updatetime个Object
        int updatetimes = 100000;
        int batchSize = 500; // 每次处理x个更新
        //数据集大小为 1 Million 个条目
        int objectnums = 1000000;
        //相同元素(关键字或者位置point)的最大数量为10W
        int rangePredicate = 100000;
        int[] maxfilesArray = {1 << 20};//20,1 << 18,1 << 16,1 << 14,1 << 12
        int[] hilbertOrders = {12};

        int edgeLength = 1 << hilbertOrders[0];
        int Wnum = 8000;
        int attachedKeywords = 12;
        String distributionType = "multi-gaussian";

        DataSetAccess dataSetAccess = new DataSetAccess(hilbertOrders[0]);
        dataSetAccess.generateFiles(objectnums, maxfilesArray[0]);
        dataSetAccess.generateKwItems(Wnum, attachedKeywords, objectnums);
        dataSetAccess.generatePoints(edgeLength, distributionType, objectnums);

        List<Double> spqsSearchTimes = new ArrayList<>();
        List<Double> tdscSearchTimes = new ArrayList<>();

        // 初始化 RSKQ_Biginteger 和 TDSC2023_Biginteger 实例
        RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxfilesArray[0], hilbertOrders[0], 2);
        TDSC2023_Biginteger tdsc2023 = new TDSC2023_Biginteger(128, rangePredicate, maxfilesArray[0], hilbertOrders[0], 2);

        Random random = new Random();
        for (int i = 0; i < updatetimes; i++) {
            int randomIndex = random.nextInt(objectnums);
//            updateItems[i] = randomIndex;
            long[] pSet = dataSetAccess.pointDataSet[randomIndex];
            String[] W = dataSetAccess.keywordItemSets[randomIndex];
            int[] files = new int[]{dataSetAccess.fileDataSet[randomIndex]};
            // 进行更新操作
            spqs.ObjectUpdate(pSet, W, new String[]{"add"}, files);
            tdsc2023.update(pSet, W, "add", files, rangePredicate);
            if ((i + 1) % batchSize == 0) {
                //System.gc(); // 执行垃圾回收
                System.out.println("Completed batch " + (i + 1) / batchSize + " of updates.");
                // 打印平均搜索时间
                System.out.printf("Update完成，平均更新时间: | RSKQ_Biginteger: |%-10.6f|ms| TDSC2023_Biginteger: |%-10.6f|ms\n",
                        spqs.getAverageUpdateTime(),tdsc2023.getAverageUpdateTime());
            }
        }

        // 移除初始的异常值
        for (int i = 0; i < delupdatetimes; i++) {
//            spqs.removeExtremesUpdateTime(); // 移除SPQS的异常更新时间
//            tdsc2023.removeExtremesUpdateTime(); // 移除TDSC2023的异常更新时间
            spqs.totalUpdateTimes.clear();
            tdsc2023.totalUpdateTimes.clear();
        }
        int div = 100;
//        int searchtimes = 5;
//        int searchEdgeLengthPer = 5;

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
                        String[] WQ = dataSetAccess.keywordItemSets[indexToSearch];

                        spqs.ObjectSearch(matrixToSearch, WQ);
                    }

                    // 移除异常值
                    for (int i = 0; i < delupdatetimes; i++) {
                        spqs.removeExtremesSearchTime();
                    }

                    // 打印平均搜索时间
                    System.out.printf("搜索完成，平均搜索时间: | RSKQ_Biginteger: |%-10.6f|ms| TDSC2023_Biginteger: |%-10.6f|ms\n",
                            spqs.getAverageSearchTime(),tdsc2023.getAverageSearchTime());
                    break;

                case 2: // 打印 PDB 和 KDB 键的数量
                    int pdbKeyCount = spqs.PDB.size();
                    int kdbKeyCount = spqs.KDB.size();
                    System.out.printf("RSKQ PDB 键的数量: %d, KDB 键的数量: %d|TDSC PDB 键的数量: %d, KDB 键的数量: %d\n",
                            pdbKeyCount, kdbKeyCount,
                            tdsc2023.PDB.size(),tdsc2023.KDB.size());
                    break;

                default:
                    System.out.println("无效选项，请重新选择。");
            }
        }
        scanner.close();
    }
}

