package org.davidmoten.PerformanceEval;

import org.davidmoten.DataProcessor.DataSetAccess;
import org.davidmoten.Scheme.SPQS.SPQS_Biginteger;
//import org.davidmoten.Scheme.TDSC2023.TDSC2023_BITSET;
import org.davidmoten.Scheme.TDSC2023.TDSC2023_Biginteger;

import java.util.Random;

public class UpdateTest {
    public static void main(String[] args) throws Exception {
        DataSetAccess dataSetAccess = new DataSetAccess();
        //清除"最大耗时-最小耗时"对数,便于计算合理的平均值
        int delExtremValueTimes = 3;
//        int updatetimes = 30;
        int updateTimes = 300;
        int objectnums = 10000;
        int rangePredicate = 20000;
        int[] maxfilesArray = {1 << 20};//20,1 << 18,1 << 16,1 << 14,1 << 12
        int[] hilbertOrders = {17};

        int edgeLength = 1 << hilbertOrders[0];
        int Wnum = 8000;
        int attachedKeywords = 12;
        String distributionType = "multi-gaussian";


        dataSetAccess.generateFiles(objectnums,maxfilesArray[0]);
        dataSetAccess.generateKwItems(Wnum,attachedKeywords,objectnums);
        dataSetAccess.generatePoints(edgeLength,distributionType,objectnums);

        System.out.println("SPQS and TDSC2023 Update Times:");
        // 外层循环：遍历 maxfiles 值
        for (int maxfiles : maxfilesArray) {
            //内层循环：遍历 hilbert orders 值
            for (int hilbertOrder : hilbertOrders) {
                SPQS_Biginteger spqs = new SPQS_Biginteger(maxfiles, hilbertOrder, 2);
                TDSC2023_Biginteger tdsc2023 = new TDSC2023_Biginteger(128, rangePredicate, maxfiles, hilbertOrder, 2);

                // spqs执行 update 操作
                for (int i = 0; i < updateTimes; i++) {
                    long[] pSet = dataSetAccess.pointDataSet[i];
                    String[] W = dataSetAccess.keywordItemSets[i];
                    int[] files = new int[]{dataSetAccess.fileDataSet[i]};
                    // 进行更新操作
                    spqs.ObjectUpdate(pSet, W, new String[]{"add"}, files, rangePredicate);
                    tdsc2023.update(pSet, W, "add", files, rangePredicate);
                }

                for (int i = 0; i < delExtremValueTimes; i++) {
                    spqs.removeExtremesUpdateTime(); // 移除最值
                    tdsc2023.removeExtremesUpdateTime(); // 同样移除
                }

                // 记录每次循环的平均更新耗时
                double spqsAvgUpdateTime = spqs.getAverageUpdateTime();
                double tdscAvgUpdateTime = tdsc2023.getAverageUpdateTime();

                // 打印每次组合的结果
                System.out.printf("maxfiles: %-10d hilbertOrder: %-2d | SPQS: |%-10.6f| ms | TDSC2023: |%-10.6f| ms\n",
                        maxfiles, hilbertOrder, spqsAvgUpdateTime, tdscAvgUpdateTime);
            }
            for (int hilbertOrder : hilbertOrders) {
                // 初始化 SPQS_BITSET 和 TDSC2023_BITSET 实例
                SPQS_Biginteger spqs = new SPQS_Biginteger(maxfiles, hilbertOrder, 2);
                TDSC2023_Biginteger tdsc2023 = new TDSC2023_Biginteger(128, rangePredicate, maxfiles, hilbertOrder, 2);

                // spqs执行 batch update 操作
                for (int i = 0; i < updateTimes; i++) {
                    long[] pSet = dataSetAccess.pointDataSet[i];
                    String[] W = dataSetAccess.keywordItemSets[i];
                    int[] files = new int[2];
                    files[0] =dataSetAccess.fileDataSet[i];//added file
                    files[1] =(files[0])%maxfilesArray[0];//del file
                    // 进行更新操作
                    spqs.ObjectUpdate(pSet, W, new String[]{"add","del"}, files, rangePredicate);
                    tdsc2023.update(pSet, W, "add", new int[]{files[0]}, rangePredicate);
                    tdsc2023.update(pSet, W, "del", new int[]{files[1]}, rangePredicate);
                }

                for (int i = 0; i < delExtremValueTimes; i++) {
                    spqs.removeExtremesUpdateTime(); // 移除最值
                    tdsc2023.removeExtremesUpdateTime(); // 同样移除
                }

                // 记录每次循环的平均更新耗时
                double spqsAvgUpdateTime = spqs.getAverageUpdateTime();
                double tdscAvgUpdateTime = tdsc2023.getAverageUpdateTime();

                // 打印每次组合的结果
                System.out.printf("maxfiles: %-10d hilbertOrder: %-2d | SPQS: |%-10.6f| ms | TDSC2023: |%-10.6f| ms\n",
                        maxfiles, hilbertOrder, spqsAvgUpdateTime, tdscAvgUpdateTime);
            }
        }
    }
}
