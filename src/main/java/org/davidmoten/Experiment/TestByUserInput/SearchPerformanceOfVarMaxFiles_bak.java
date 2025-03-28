package org.davidmoten.Experiment.TestByUserInput;

import org.davidmoten.Scheme.SKQ.SKQ_Biginteger;
import org.davidmoten.SpatialDataProcessor.StaticData.DataSetAccess;
import org.davidmoten.Scheme.Construction.ConstructionOne;
import org.davidmoten.Scheme.Construction.ConstructionTwo;
import org.davidmoten.Scheme.RSKQ.RSKQ_Biginteger;

import java.math.BigInteger;
import java.util.*;

import static org.davidmoten.Experiment.TestByUserInput.BRQComparisonInput.generateHilbertMatrix;

public class SearchPerformanceOfVarMaxFiles_bak {

    public static void main(String[] args) throws Exception {
        //清除"最大耗时-最小耗时"对数,便于计算合理的平均值
        int delupdatetimes = 0;
        //需要在内存中存储，所以需要插入updatetime个Object
        int updatetimes = 400;
        int batchSize = 400; // 每次处理x个更新
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
        List<Double> consOneSearchTimes = new ArrayList<>();
        List<Double> consTwoSearchTimes = new ArrayList<>();

        // 初始化 RSKQ_Biginteger 和 SKQ_Biginteger 实例
        RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxfilesArray[0], hilbertOrders[0], 2);
        SKQ_Biginteger tdsc2023 = new SKQ_Biginteger(128, rangePredicate, maxfilesArray[0], hilbertOrders[0], 2);
        // 初始化 ConstructionOne 实例
        int lambda = 128;
        // 确定合理的 t 值，根据最大边界来设置
        int maxCoordinate = 1 << hilbertOrders[0]; // 假设 0 <= x, y <= (1 << 17)
        int t = (int) (Math.log(maxCoordinate) / Math.log(2));

        // 分离 x 和 y 坐标
        int[] xCoordinates = new int[updatetimes];
        int[] yCoordinates = new int[updatetimes];

        // 执行更新操作
//        int[] updateItems = new int[updatetimes];
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
            tdsc2023.update(pSet, W, "add", files, rangePredicate);
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
        ConstructionTwo con2 = new ConstructionTwo(lambda, t, updatetimes, xCoordinates, yCoordinates);

        // 构建con1,con2二叉树和倒排索引
        con1.BTx = con1.buildBinaryTree(t);
        con1.BTy = con1.buildBinaryTree(t);
        Map<Integer, String> Sx = con1.buildxNodeInvertedIndex(con1.buildInvertedIndex(t, updatetimes, xCoordinates), t);
        Map<Integer, String> Sy = con1.buildyNodeInvertedIndex(con1.buildInvertedIndex(t, updatetimes, yCoordinates), t);
        con1.setupEDS(Sx, Sy);
        con2.BTx = con2.buildBinaryTree(t);
        con2.BTy = con2.buildBinaryTree(t);
        Map<Integer, String> Sx_2 = con2.buildxNodeInvertedIndex(con2.buildInvertedIndex(t, updatetimes, xCoordinates), t);
        Map<Integer, String> Sy_2 = con2.buildyNodeInvertedIndex(con2.buildInvertedIndex(t, updatetimes, yCoordinates), t);
        con2.setupEDS(Sx_2, Sy_2);
        // 移除初始的异常值
        for (int i = 0; i < delupdatetimes; i++) {
            spqs.removeExtremesUpdateTime(); // 移除SPQS的异常更新时间
            tdsc2023.removeExtremesUpdateTime(); // 移除TDSC2023的异常更新时间
//            spqs.removeExtremesSearchTime();
//            tdsc2023.removeExtremesSearchTime();
        }

        boolean continueSearch = true;
        int div = 100;
        int searchtimes = 1;
        int searchEdgeLengthPer = 5;

        Scanner scanner = new Scanner(System.in);
        // 询问用户输入 totalSearchTimes
        System.out.print("请输入总循环次数 totalSearchTimes: ");
        int totalSearchTimes = scanner.nextInt();

        if (totalSearchTimes <= 0) {
            System.out.println("totalSearchTimes 必须是正整数。程序退出。");
            return;
        }

        // 开始 for 循环，总计执行 totalSearchTimes 次
        for (int currentRound = 1; currentRound <= totalSearchTimes; currentRound++) {
            System.out.printf("第 %d/%d 次循环\n", currentRound, totalSearchTimes);
            int xstart = random.nextInt(edgeLength * (div - searchEdgeLengthPer) / div);
            int ystart = random.nextInt(edgeLength * (div - searchEdgeLengthPer) / div);
            int searchRange = edgeLength * searchEdgeLengthPer / div;
            BigInteger[][] matrixToSearch = generateHilbertMatrix(spqs.hilbertCurve, xstart, ystart, searchRange, searchRange);

            // 执行 searchtimes 次循环
            for (int i = 0; i < searchtimes; i++) {
                int indexToSearch = random.nextInt(objectnums);
//                long[] pSetQ = dataSetAccess.pointDataSet[indexToSearch];
//                String[] WQ = dataSetAccess.keywordItemSets[indexToSearch];
                String[] WQ = {};

                // 执行搜索操作并记录时间
                spqs.ObjectSearch(matrixToSearch, WQ);
                tdsc2023.Search(matrixToSearch, WQ);
                spqsSearchTimes.add(spqs.getAverageSearchTime());
                tdscSearchTimes.add(tdsc2023.getAverageSearchTime());

                int[] rangex = con1.rangeConvert(t, new int[]{xstart, xstart + searchRange});
                int[] rangey = con1.rangeConvert(t, new int[]{ystart, ystart + searchRange});

                long startTime = System.nanoTime();
                con1.clientSearch(rangex, rangey, t);
                long endTime = System.nanoTime();
                consOneSearchTimes.add((endTime - startTime) / 1e6);

                long startTime2 = System.nanoTime();
                con2.clientSearch(rangex, rangey, t);
                long endTime2 = System.nanoTime();
                consTwoSearchTimes.add((endTime2 - startTime2) / 1e6);
            }

            // 移除异常值
            for (int i = 0; i < delupdatetimes; i++) {
                spqs.removeExtremesSearchTime();
                tdsc2023.removeExtremesSearchTime();
            }

            // 打印平均搜索时间
            System.out.printf("avg:| SPQS: |%-10.6f|ms | TDSC2023: |%-10.6f|ms | Cons-1: |%-10.6f|ms | Cons-2: |%-10.6f|ms\n",
                    spqsSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    tdscSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    consOneSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                    consTwoSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        }

//        while (continueSearch) {
//            // 获取用户输入的 searchtimes 和 l
//            System.out.print("请输入 searchtimes: ");
//            searchtimes = scanner.nextInt();
//            // 检查输入是否为 -1，退出程序
//            if (searchtimes == -1) {
//                System.out.println("程序已退出。");
//                break;
//            }
//            // 检查输入是否小于 100
//            if (searchtimes >= 100 || searchtimes <= delupdatetimes) {
//                System.out.println("输入的 searchtimes 必须大于 delupdatetimes 且小于 100，请重新输入。");
//                continue;
//            }
//
//            // 获取用户输入的 searchEdgeLengthPer
//            System.out.print("请输入 searchEdgeLengthPer 的值 (控制 Hilbert 范围): ");
//            searchEdgeLengthPer = scanner.nextInt();
//
//            // 检查输入是否为 -1，退出程序
//            if (searchEdgeLengthPer == -1) {
//                System.out.println("程序已退出。");
//                break;
//            }
//
//            // 检查输入是否小于 100
//            if (searchEdgeLengthPer >= 100) {
//                System.out.println("输入的 searchEdgeLengthPer 必须小于 100，请重新输入。");
//                continue;
//            }
//
////            System.out.print("请输入 div: ");
//            div = 100;
//            int xstart =random.nextInt(edgeLength*(div-searchEdgeLengthPer)/div);
//            int ystart =random.nextInt(edgeLength*(div-searchEdgeLengthPer)/div);
//            int i1 = edgeLength * (searchEdgeLengthPer) / div;
//            int xlen = i1;
//            int ylen = i1;
//            BigInteger[][] matrixToSearch = generateHilbertMatrix(spqs.hilbertCurve,
//                    xstart,ystart,xlen,ylen);
//            // Construction-One/Two搜索耗时列表
//            List<Double> consOneSearchTimes = new ArrayList<>();
//            List<Double> consTwoSearchTimes = new ArrayList<>();
//            for (int i = 0; i < searchtimes; i++) {
//                int indexToSearch = random.nextInt(objectnums);
//                long[] pSetQ = dataSetAccess.pointDataSet[indexToSearch];
//                String[] WQ = dataSetAccess.keywordItemSets[indexToSearch];
//
////                // 获取该对象的 Hilbert 索引,进行条形搜索
////                BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSetQ);
////                // 设置条形搜索范围 R_min 和 R_max
////                BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(searchEdgeLengthPer));
////                BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(searchEdgeLengthPer));
////                // 执行搜索操作
////                spqs.ObjectSearch(R_min, R_max, WQ);
////                tdsc2023.Search(R_min, R_max, WQ);
//                // 执行搜索操作
//                spqs.ObjectSearch(matrixToSearch, WQ);
//
//                // 测量搜索时间
////                long startTimeTDSC = System.nanoTime();
//                tdsc2023.Search(matrixToSearch, WQ);
////                tdscSearchTimes.add((System.nanoTime() - startTimeTDSC) / 1e6);
//                // 记录每次搜索的平均耗时
////                double spqsAvgClientSearchTime = spqs.getAverageClientTime();
////                double spqsAvgServerSearchTime = spqs.getAverageServerTime();
//                double spqsAvgSearchTime = spqs.getAverageSearchTime();
//
////                double tdscAvgClientSearchTime = tdsc2023.getAverageClientTime();
////                double tdscAvgServerSearchTime = tdsc2023.getAverageServerTime();
//                double tdscAvgSearchTime = tdsc2023.getAverageSearchTime();
//
//                spqsSearchTimes.add(spqsAvgSearchTime);
//                tdscSearchTimes.add(tdscAvgSearchTime);
//
//                // 定义搜索范围，模拟查询
//                int[] rangex = con1.rangeConvert(t, new int[]{xstart, xstart + xlen});
//                int[] rangey = con1.rangeConvert(t, new int[]{ystart, ystart + ylen});
//
//                // 测量搜索时间
//                long startTime = System.nanoTime();
//                con1.clientSearch(rangex, rangey, t);
//                long endTime = System.nanoTime();
//                consOneSearchTimes.add((endTime - startTime) / 1e6);
//                long startTime2 = System.nanoTime();
//                con2.clientSearch(rangex, rangey, t);
//                long endTime2 = System.nanoTime();
//                consOneSearchTimes.add((endTime2 - startTime2) / 1e6);
//                consTwoSearchTimes.add((endTime2 - startTime2) / 1e6);
////                System.out.printf("Search time for N=%d with t=%d: %.5f ms%n", updatetimes, t, elapsedTime);
//
//                // 打印每次搜索的结果
////                System.out.printf("SPQS: |%-10.6f|ms Client: |%-10.6f|ms SPQS Server: |%-10.6f|ms | TDSC2023: |%-10.6f|ms Client: |%-10.6f|ms TDSC2023 Server: |%-10.6f|ms\n",
////                        spqsAvgSearchTime, spqsAvgClientSearchTime, spqsAvgServerSearchTime,
////                        tdscAvgSearchTime, tdscAvgClientSearchTime, tdscAvgServerSearchTime);
//            }
//
//            // 移除初始的异常值
//            for (int i = 0; i < delupdatetimes; i++) {
//                spqs.removeExtremesSearchTime();
//                tdsc2023.removeExtremesSearchTime();
//            }
//            // 打印平均搜索时间
//            System.out.printf("avg:| SPQS: |%-10.6f|ms | TDSC2023: |%-10.6f|ms | Cons-1: |%-10.6f|ms | Cons-2: |%-10.6f|ms\n",
//                    spqsSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
//                    tdscSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
//                    consOneSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
//                    consTwoSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
//
//            spqsSearchTimes.clear();
//            tdscSearchTimes.clear();
//            consOneSearchTimes.clear();
//            consTwoSearchTimes.clear();
//
//            // 询问用户是否继续
//            System.out.print("是否继续输入新的 searchtimes 和 l 进行搜索? (y/n): ");
//            String input = scanner.next();
//            if (!input.equalsIgnoreCase("y")) {
//                continueSearch = false;
//            }
//        }
        scanner.close();
    }
}

