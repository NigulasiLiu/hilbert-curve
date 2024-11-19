//package org.davidmoten.PerformanceEval;
//
//import org.davidmoten.Scheme.SPQS.SPQS_BITSET;
//import org.davidmoten.Scheme.TDSC2023.TDSC2023_BITSET;
//
//import java.math.BigInteger;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ForkJoinPool;
//import java.util.concurrent.RecursiveAction;
//import java.util.concurrent.ThreadLocalRandom;
//
//public class SearchTestMultiThread {
//
//    public static void main(String[] args) throws Exception {
//        int delupdatetimes = 1;
//        int updatetimes = 600; // 可以增大到更多次
//        int searchtimes = 100;
//        int objectnums = 2000;
//        int rangePredicate = 20000;
//        int Wnums = 6;
//        int[] maxfilesArray = {1 << 20};
//        int[] hilbertOrders = {17};
//
//        // 初始化2000个对象
//        Object[] predefinedObjects = new Object[objectnums];
//        for (int i = 0; i < objectnums; i++) {
//            long[] pSet = {i, i + 1};
//            String[] W = new String[Wnums];
//            for (int j = 0; j < Wnums; j++) {
//                W[j] = "keyword" + (i + j + 1);
//            }
//            int[] files = {i};
//            predefinedObjects[i] = new Object[]{pSet, W, files};
//        }
//
//        // 优化为 ForkJoinPool，动态分配任务
//        ForkJoinPool pool = new ForkJoinPool(); // 可以根据需要自动调整线程数量
//
//        List<Double> spqsSearchTimes = new ArrayList<>();
//        List<Double> tdscSearchTimes = new ArrayList<>();
//
//        for (int maxfiles : maxfilesArray) {
//            for (int hilbertOrder : hilbertOrders) {
//                SPQS_BITSET spqs = new SPQS_BITSET(maxfiles, hilbertOrder, 2);
//                TDSC2023_BITSET tdsc2023 = new TDSC2023_BITSET(128, rangePredicate, maxfiles, hilbertOrder, 2);
//
//                // 使用 ForkJoin 进行并行更新操作
//                pool.invoke(new UpdateTask(predefinedObjects, spqs, tdsc2023, rangePredicate, objectnums, updatetimes));
//
//                // 移除极端耗时数据
//                for (int i = 0; i < delupdatetimes; i++) {
//                    spqs.removeExtremesUpdateTime();
//                    tdsc2023.removeExtremesUpdateTime();
//                }
//
//                // 搜索任务保持不变
//                for (int i = 0; i < searchtimes; i++) {
//                    int randomValue = ThreadLocalRandom.current().nextInt(objectnums * 4);
//                    long[] pSetQ = {randomValue, randomValue + 1};
//
//                    String[] WQ = new String[Wnums];
//                    for (int j = 0; j < Wnums; j++) {
//                        WQ[j] = "keyword" + (randomValue + j + 1);
//                    }
//
//                    BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSetQ);
//                    BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(100));
//                    BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(100));
//
//                    spqs.ObjectSearch(R_min, R_max, WQ);
//                    tdsc2023.Search(R_min, R_max, WQ);
//
//                    double spqsAvgSearchTime = spqs.getAverageSearchTime();
//                    double tdscAvgSearchTime = tdsc2023.getAverageSearchTime();
//
//                    spqsSearchTimes.add(spqsAvgSearchTime);
//                    tdscSearchTimes.add(tdscAvgSearchTime);
//
//                    System.out.printf("maxfiles: %-10d hilbertOrder: %-2d | SPQS: %-10.6fms | TDSC2023: %-10.6fms\n",
//                            maxfiles, hilbertOrder, spqsAvgSearchTime, tdscAvgSearchTime);
//                }
//            }
//        }
//
//        // 输出平均搜索时间
//        System.out.printf("avg:| SPQS: %-10.6fms | TDSC2023: %-10.6fms\n",
//                spqsSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
//                tdscSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
//    }
//
//    // 内部类：处理更新操作的并行任务
//    static class UpdateTask extends RecursiveAction {
//        private final Object[] predefinedObjects;
//        private final SPQS_BITSET spqs;
//        private final TDSC2023_BITSET tdsc2023;
//        private final int rangePredicate;
//        private final int objectnums;
//        private final int updatetimes;
//        private final int batchSize = 10;
//
//        public UpdateTask(Object[] predefinedObjects, SPQS_BITSET spqs, TDSC2023_BITSET tdsc2023,
//                          int rangePredicate, int objectnums, int updatetimes) {
//            this.predefinedObjects = predefinedObjects;
//            this.spqs = spqs;
//            this.tdsc2023 = tdsc2023;
//            this.rangePredicate = rangePredicate;
//            this.objectnums = objectnums;
//            this.updatetimes = updatetimes;
//        }
//
//        @Override
//        protected void compute() {
//            if (updatetimes <= batchSize) {
//                for (int i = 0; i < updatetimes; i++) {
//                    Object[] selectedObject = (Object[]) predefinedObjects[ThreadLocalRandom.current().nextInt(objectnums)];
//                    long[] pSet = (long[]) selectedObject[0];
//                    String[] W = (String[]) selectedObject[1];
//                    int[] files = (int[]) selectedObject[2];
//
//                    try {
//                        spqs.ObjectUpdate(pSet, W, "add", files, rangePredicate);
//                        tdsc2023.update(pSet, W, "add", files, rangePredicate);
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//
//                    if ((i + 1) % batchSize == 0) {
//                        System.gc(); // 可选的 GC 调用（仅用于非常大的批次时）
//                        System.out.println("Completed batch " + (i + 1) / batchSize + " of updates.");
//                    }
//                }
//            } else {
//                int mid = updatetimes / 2;
//                UpdateTask task1 = new UpdateTask(predefinedObjects, spqs, tdsc2023, rangePredicate, objectnums, mid);
//                UpdateTask task2 = new UpdateTask(predefinedObjects, spqs, tdsc2023, rangePredicate, objectnums, updatetimes - mid);
//                invokeAll(task1, task2);
//            }
//        }
//    }
//}
