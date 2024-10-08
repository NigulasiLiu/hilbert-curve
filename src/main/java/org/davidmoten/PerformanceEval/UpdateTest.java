package org.davidmoten.PerformanceEval;

import org.davidmoten.Scheme.SPQS.SPQS_BITSET;
import org.davidmoten.Scheme.TDSC2023.TDSC2023_BITSET;

import java.util.Random;

public class UpdateTest {

    public static void main(String[] args) throws Exception {
        int deltimes = 1;
        int updatetimes = 23;
        int objectnums = 201;
        int rangePredicate = 20000;
        int[] maxfilesArray = {1 << 14};
        int[] hilbertOrders = {17};

        // 初始化200个Object集合，每个包含pSet, W, files
        Object[] predefinedObjects = new Object[objectnums];
        for (int i = 0; i < objectnums; i++) {
            long[] pSet = {i, i + 1}; // pSet
            String[] W = {"keyword" + (i + 1), "keyword" + (i + 2), "keyword" + (i + 3), "keyword" + (i + 4),
                    "keyword" + (i + 5), "keyword" + (i + 6), "keyword" + (i + 7), "keyword" + (i + 8),
                    "keyword" + (i + 9), "keyword" + (i + 10), "keyword" + (i + 11), "keyword" + (i + 12)}; // W
            int[] files = {i}; // files
            predefinedObjects[i] = new Object[]{pSet, W, files};
        }

        Random random = new Random(); // 用于随机选择对象

        // 初始化标题行
        System.out.println("SPQS and TDSC2023 Update Times:");

        // 外层循环：遍历 maxfiles 值
        for (int maxfiles : maxfilesArray) {
            // 内层循环：遍历 hilbert orders 值
            for (int hilbertOrder : hilbertOrders) {
                // 初始化 SPQS_BITSET 和 TDSC2023_BITSET 实例
                SPQS_BITSET spqs = new SPQS_BITSET(maxfiles, hilbertOrder, 2);
                TDSC2023_BITSET tdsc2023 = new TDSC2023_BITSET(128, rangePredicate, maxfiles, hilbertOrder, 2);

                // 执行 update 操作 (模拟多次操作获取平均时间)
                for (int i = 0; i < updatetimes; i++) {
                    // 从预定义的集合中随机取出一个对象
                    Object[] selectedObject = (Object[]) predefinedObjects[random.nextInt(objectnums)];
                    long[] pSet = (long[]) selectedObject[0];
                    String[] W = (String[]) selectedObject[1];
                    int[] files = (int[]) selectedObject[2];

                    // 进行更新操作
                    spqs.ObjectUpdate(pSet, W, "add", files, rangePredicate);
                    tdsc2023.update(pSet, W, "add", files, rangePredicate);
                }

                for (int i = 0; i < deltimes; i++) {
                    spqs.removeFirstUpdateTime(); // 移除第一次耗时，去掉初始的异常值
                    tdsc2023.removeFirstUpdateTime(); // 同样移除
                }

                // 记录每次循环的平均更新耗时
                double spqsAvgUpdateTime = spqs.getAverageUpdateTime();
                double tdscAvgUpdateTime = tdsc2023.getAverageUpdateTime();

                // 打印每次组合的结果
                System.out.printf("maxfiles: %-10d hilbertOrder: %-2d | SPQS: %-10.6f ms | TDSC2023: %-10.6f ms\n",
                        maxfiles, hilbertOrder, spqsAvgUpdateTime, tdscAvgUpdateTime);
            }
        }
    }
}
