package org.davidmoten.PerformanceEval;

import org.davidmoten.DataProcessor.DataSetAccess;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;
import org.davidmoten.Scheme.SPQS.SPQS_Biginteger;
import org.davidmoten.Scheme.TDSC2023.TDSC2023_Biginteger;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;


public class SearchTest {
    public static BigInteger[][] generateHilbertMatrix(HilbertCurve hilbertCurve,int startX, int startY, int width, int height) {
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
        int delupdatetimes = 3;
        //搜索次数,用于计算合理的平均值
        int searchtimes;
        //需要在内存中存储，所以需要插入updatetime个Object
        int updatetimes = 20000;
        int batchSize = 400; // 每次处理300个更新
        //数据集大小为 1 Million 个条目
        int objectnums = 1000000;
        //相同元素(关键字或者位置point)的最大数量为10W
        int rangePredicate = 100000;
        //每个Obejct附着6个随机关键字
        int[] maxfilesArray = {1 << 20};//20,1 << 18,1 << 16,1 << 14,1 << 12
        int[] hilbertOrders = {17};

        int edgeLength = 1 << hilbertOrders[0];
        int Wnum = 8000;
        int attachedKeywords = 12;
        String distributionType = "multi-gaussian";

        DataSetAccess dataSetAccess = new DataSetAccess();
        dataSetAccess.generateFiles(objectnums,maxfilesArray[0]);
        dataSetAccess.generateKwItems(Wnum,attachedKeywords,objectnums);
        dataSetAccess.generatePoints(edgeLength,distributionType,objectnums);

        List<Double> spqsSearchTimes = new ArrayList<>();
        List<Double> tdscSearchTimes = new ArrayList<>();

        // 初始化 SPQS_Biginteger 和 TDSC2023_Biginteger 实例
        SPQS_Biginteger spqs = new SPQS_Biginteger(maxfilesArray[0], hilbertOrders[0], 2);
        TDSC2023_Biginteger tdsc2023 = new TDSC2023_Biginteger(128, rangePredicate, maxfilesArray[0], hilbertOrders[0], 2);


        // 执行更新操作
        int[] updateItems = new int[updatetimes];
        Random random = new Random();
        for (int i = 0; i < updatetimes; i++) {
            int randomIndex = random.nextInt(objectnums);
            updateItems[i] = randomIndex;
            long[] pSet = dataSetAccess.pointDataSet[randomIndex];
            String[] W = dataSetAccess.keywordItemSets[randomIndex];
            int[] files = new int[]{dataSetAccess.fileDataSet[randomIndex]};
            // 进行更新操作
            spqs.ObjectUpdate(pSet, W, new String[]{"add"}, files, rangePredicate);
            tdsc2023.update(pSet, W, "add", files, rangePredicate);
            if ((i + 1) % batchSize == 0) {
                System.gc(); // 执行垃圾回收
                System.out.println("Completed batch " + (i + 1) / batchSize + " of updates.");
            }
        }

        // 移除初始的异常值
        for (int i = 0; i < delupdatetimes; i++) {
            spqs.removeExtremesUpdateTime(); // 移除SPQS的异常更新时间
            tdsc2023.removeExtremesUpdateTime(); // 移除TDSC2023的异常更新时间
        }

        Scanner scanner = new Scanner(System.in);
        boolean continueSearch = true;
        int searchEdgeLengthPer;

        while (continueSearch) {
            // 获取用户输入的 searchtimes 和 l
            System.out.print("请输入 searchtimes: ");
            searchtimes = scanner.nextInt();
            System.out.print("请输入 searchEdgeLengthPer 的值 (控制 Hilbert 范围): ");
            searchEdgeLengthPer = scanner.nextInt();
            BigInteger[][] matrixToSearch = generateHilbertMatrix(spqs.hilbertCurve,
                    random.nextInt(edgeLength*(100-searchEdgeLengthPer)/100),
                    edgeLength*(100-searchEdgeLengthPer)/100,
            edgeLength*(searchEdgeLengthPer)/100,edgeLength*(searchEdgeLengthPer)/100);
            for (int i = 0; i < searchtimes; i++) {
                int indexToSearch = random.nextInt(objectnums);
                long[] pSetQ = dataSetAccess.pointDataSet[indexToSearch];
                String[] WQ = dataSetAccess.keywordItemSets[indexToSearch];

//                // 获取该对象的 Hilbert 索引,进行条形搜索
//                BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSetQ);
//                // 设置条形搜索范围 R_min 和 R_max
//                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(searchEdgeLengthPer));
//                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(searchEdgeLengthPer));
//                // 执行搜索操作
//                spqs.ObjectSearch(R_min, R_max, WQ);
//                tdsc2023.Search(R_min, R_max, WQ);
                // 执行搜索操作
                spqs.ObjectSearch(matrixToSearch, WQ);
                tdsc2023.Search(matrixToSearch, WQ);

                // 记录每次搜索的平均耗时
                double spqsAvgClientSearchTime = spqs.getAverageClientTime();
                double spqsAvgServerSearchTime = spqs.getAverageServerTime();
                double spqsAvgSearchTime = spqs.getAverageSearchTime();

                double tdscAvgClientSearchTime = tdsc2023.getAverageClientTime();
                double tdscAvgServerSearchTime = tdsc2023.getAverageServerTime();
                double tdscAvgSearchTime = tdsc2023.getAverageSearchTime();

                spqsSearchTimes.add(spqsAvgSearchTime);
                tdscSearchTimes.add(tdscAvgSearchTime);

                // 打印每次搜索的结果
//                System.out.printf("SPQS: |%-10.6f|ms Client: |%-10.6f|ms SPQS Server: |%-10.6f|ms | TDSC2023: |%-10.6f|ms Client: |%-10.6f|ms TDSC2023 Server: |%-10.6f|ms\n",
//                        spqsAvgSearchTime, spqsAvgClientSearchTime, spqsAvgServerSearchTime,
//                        tdscAvgSearchTime, tdscAvgClientSearchTime, tdscAvgServerSearchTime);
            }

            // 打印平均搜索时间
            System.out.printf("avg:| SPQS: |%-10.6f|ms | TDSC2023: |%-10.6f|ms\n",
                    spqsSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    tdscSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

            spqsSearchTimes.clear();
            tdscSearchTimes.clear();

            // 询问用户是否继续
            System.out.print("是否继续输入新的 searchtimes 和 l 进行搜索? (y/n): ");
            String input = scanner.next();
            if (!input.equalsIgnoreCase("y")) {
                continueSearch = false;
            }
        }

        scanner.close();
    }
}
