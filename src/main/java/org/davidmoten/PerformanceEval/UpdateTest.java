package org.davidmoten.PerformanceEval;

import org.davidmoten.Scheme.SPQS.SPQS_BITSET;
import org.davidmoten.Scheme.SPQS.SPQS_Biginteger;
//import org.davidmoten.Scheme.TDSC2023.TDSC2023_BITSET;
import org.davidmoten.Scheme.TDSC2023.TDSC2023_Biginteger;

import java.util.Random;

public class UpdateTest {
    public static void main(String[] args) throws Exception {
        int delExtremValueTimes = 5;
//        int updatetimes = 30;
        int batchupdatetimes = 50;
        int objectnums = 10000;
        int rangePredicate = 20000;
        int Wnums = 12;
        int[] maxfilesArray = {1 << 12};//20,1 << 18,1 << 16,1 << 14,1 << 12
        int[] hilbertOrders = {12};

        Random random = new Random(); // 用于随机选择对象
        // 初始化200个Object集合，每个包含pSet, W, files
        Object[] predefinedObjects = new Object[objectnums];
        for (int i = 0; i < objectnums; i++) {
            long[] pSet = {random.nextInt(objectnums), random.nextInt(objectnums)}; // pSet
            String[] W = new String[Wnums]; // 创建W数组
            for (int j = 0; j < Wnums; j++) {
                W[j] = "keyword" + (i + j + 1); // 动态生成关键词
            }
            int[] files = {random.nextInt(maxfilesArray[0])}; // files
            predefinedObjects[i] = new Object[]{pSet, W, files};
        }
        System.out.println("SPQS and TDSC2023 Cross Update Times:");
        // 外层循环：遍历 maxfiles 值
        for (int maxfiles : maxfilesArray) {
            // 内层循环：遍历 hilbert orders 值
            for (int hilbertOrder : hilbertOrders) {
                // 初始化 SPQS_BITSET 和 TDSC2023_BITSET 实例
                SPQS_Biginteger spqs = new SPQS_Biginteger(maxfiles, hilbertOrder, 2);
                TDSC2023_Biginteger tdsc2023 = new TDSC2023_Biginteger(128, rangePredicate, maxfiles, hilbertOrder, 2);

                // spqs执行 batch update 操作
                for (int i = 0; i < batchupdatetimes; i++) {
                    // 从预定义的集合中随机取出一个对象
                    Object[] selectedObject = (Object[]) predefinedObjects[random.nextInt(objectnums)];
                    long[] pSet = (long[]) selectedObject[0];
                    String[] W = (String[]) selectedObject[1];
                    int[] files = (int[]) selectedObject[2];

                    // 进行更新操作
                    spqs.ObjectUpdate(pSet, W, "add", files, rangePredicate);
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
        }
    }
}
