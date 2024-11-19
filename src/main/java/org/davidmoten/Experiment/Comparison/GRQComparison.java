package org.davidmoten.Experiment.Comparison;

import org.davidmoten.DataProcessor.DataSetAccess;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;
import org.davidmoten.Scheme.Construction.ConstructionOne;
import org.davidmoten.Scheme.SPQS.RSKQ_Biginteger;

import java.math.BigInteger;
import java.util.*;


public class GRQComparison {
    public static BigInteger[][] generateHilbertMatrix(HilbertCurve hilbertCurve, int startX, int startY, int width, int height) {
        BigInteger[][] matrix = new BigInteger[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
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
        List<Double> consOneSearchTimes = new ArrayList<>();

        // 初始化 RSKQ_Biginteger 和 TDSC2023_Biginteger 实例
        RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxfilesArray[0], hilbertOrders[0], 2);

        int lambda = 128;
        // 确定合理的 t 值，根据最大边界来设置
        int maxCoordinate = 1 << hilbertOrders[0]; // 假设 0 <= x, y <= (1 << 17)
        int t = (int) (Math.log(maxCoordinate) / Math.log(2));

        // 分离 x 和 y 坐标
        int[] xCoordinates = new int[updatetimes];
        int[] yCoordinates = new int[updatetimes];

        Random random = new Random();
        for (int i = 0; i < updatetimes; i++) {
            int randomIndex = random.nextInt(objectnums);
//            updateItems[i] = randomIndex;
            long[] pSet = dataSetAccess.pointDataSet[randomIndex];
            String[] W = dataSetAccess.keywordItemSets[randomIndex];
            int[] files = new int[]{dataSetAccess.fileDataSet[randomIndex]};
            xCoordinates[i] = Math.toIntExact(dataSetAccess.pointDataSet[randomIndex][0]);
            yCoordinates[i] = Math.toIntExact(dataSetAccess.pointDataSet[randomIndex][1]);
            // 进行更新操作
            spqs.ObjectUpdate(pSet, W, new String[]{"add"}, files);
            if ((i + 1) % batchSize == 0) {
                //System.gc(); // 执行垃圾回收
                System.out.println("Completed batch " + (i + 1) / batchSize + " of updates.");
            }
        }

        // 清空 dataSetAccess 引用以释放内存
        dataSetAccess = null;
        System.gc(); // 提示 JVM 垃圾回收

        //初始化ConstructionOne/Two
        ConstructionOne con1 = new ConstructionOne(lambda, t, updatetimes, xCoordinates, yCoordinates);
//        ConstructionTwo con2 = new ConstructionTwo(lambda, t, updatetimes, xCoordinates, yCoordinates);

        // 构建con1,con2二叉树和倒排索引
        con1.BTx = con1.buildBinaryTree(t);
        con1.BTy = con1.buildBinaryTree(t);
        Map<Integer, String> Sx = con1.buildxNodeInvertedIndex(con1.buildInvertedIndex(t, updatetimes, xCoordinates), t);
        Map<Integer, String> Sy = con1.buildyNodeInvertedIndex(con1.buildInvertedIndex(t, updatetimes, yCoordinates), t);
        con1.setupEDS(Sx, Sy);
//        con2.BTx = con2.buildBinaryTree(t);
//        con2.BTy = con2.buildBinaryTree(t);
//        Map<Integer, String> Sx_2 = con2.buildxNodeInvertedIndex(con2.buildInvertedIndex(t, updatetimes, xCoordinates), t);
//        Map<Integer, String> Sy_2 = con2.buildyNodeInvertedIndex(con2.buildInvertedIndex(t, updatetimes, yCoordinates), t);
//        con2.setupEDS(Sx_2, Sy_2);
        // 移除初始的异常值
        for (int i = 0; i < delupdatetimes; i++) {
            spqs.removeExtremesUpdateTime(); // 移除SPQS的异常更新时间
        }

        int div = 100;

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
                        spqs.GRQSearch(matrixToSearch);
                        int[] rangex = con1.rangeConvert(t, new int[]{xstart, xstart + searchRange});
                        int[] rangey = con1.rangeConvert(t, new int[]{ystart, ystart + searchRange});

                        long startTime = System.nanoTime();
                        con1.clientSearch(rangex, rangey, t);
                        long endTime = System.nanoTime();
                        consOneSearchTimes.add((endTime - startTime) / 1e6);
                    }

                    // 移除异常值
                    for (int i = 0; i < delupdatetimes; i++) {
                        spqs.removeExtremesSearchTime();
                    }

                    // 打印平均搜索时间
                    System.out.printf("搜索完成，平均搜索时间:| RSKQ_Biginteger: |%-10.6f|ms | Cons-1: |%-10.6f|ms\n",
                            spqs.getAverageSearchTime(),
                            consOneSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
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

