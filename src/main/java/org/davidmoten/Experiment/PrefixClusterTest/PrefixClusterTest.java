package org.davidmoten.Experiment.PrefixClusterTest;

import org.davidmoten.BPC.BPCGenerator;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.davidmoten.Experiment.TestByUserInput.BRQComparisonInput.generateHilbertMatrix;

public class PrefixClusterTest {

    // 希尔伯特曲线阶数
    private static final int[] hilbertOrders = {8, 10, 12, 14, 16};
    private static final int[] range = {5, 10, 15, 20, 25};

    // 实验配置
    private static final int div = 100; // 分割数，根据需要调整
    public int expTimes = 1000; // 修改为400次实验

    public static void main(String[] args) {

        HilbertCurve hilbertCurve = HilbertCurve.bits(3).dimensions(2);
        // 获取每个点的索引值，并打印对应的6位二进制字符串
        System.out.println("Index for (5, 6): " + hilbertCurve.index(5, 6) + " => " + String.format("%6s", hilbertCurve.index(5, 6).toString(2)).replace(' ', '0'));
        System.out.println("Index for (2, 4): " + hilbertCurve.index(2, 4) + " => " + String.format("%6s", hilbertCurve.index(2, 4)).replace(' ', '0'));
        System.out.println("Index for (1, 3): " + hilbertCurve.index(1, 3) + " => " + String.format("%6s", hilbertCurve.index(1, 3)).replace(' ', '0'));
        System.out.println("Index for (4, 0): " + hilbertCurve.index(4, 0) + " => " + String.format("%6s", hilbertCurve.index(4, 0)).replace(' ', '0'));
    }

    public void runExperiment() {
        // 遍历每个 range
        for (int r : range) {
            System.out.println("Testing with range: " + r);
            // 为每个希尔伯特曲线阶数执行实验
            for (int order : hilbertOrders) {

                // 初始化 Hilbert 曲线
                HilbertCurve hilbertCurve = HilbertCurve.bits(order).dimensions(2);
                int edgeLength = 1 << order; // 计算边长，2^hilbertOrder
                Random random = new Random();

                // 测试 expTimes 次
                long totalMapTime = 0;
                long totalListTime = 0;

                for (int i = 0; i < expTimes; i++) {
                    // 随机生成搜索矩阵的起始点和范围
                    int xstart = random.nextInt(edgeLength * (div - r) / div);
                    int ystart = random.nextInt(edgeLength * (div - r) / div);
                    int searchRange = edgeLength / div;

                    // 生成希尔伯特矩阵
                    BigInteger[][] matrixToSearch = generateHilbertMatrix(hilbertCurve, xstart, ystart, searchRange, searchRange);

                    // 将矩阵转换为 R 数组
                    BigInteger[] R = new BigInteger[matrixToSearch.length * matrixToSearch[0].length];
                    for (int j = 0; j < matrixToSearch.length; j++) {
                        System.arraycopy(matrixToSearch[j], 0, R, j * matrixToSearch[0].length, matrixToSearch[0].length);
                    }

                    // 计时获取 BPC 结果 (resultMap)
                    long startTime = System.nanoTime();
                    Map<Integer, List<BigInteger>> resultMap = BPCGenerator.GetBPCValueMap(R, order * 2);
                    long endTime = System.nanoTime();
                    totalMapTime += (endTime - startTime);

                    // 计时获取 resultList
                    startTime = System.nanoTime();
                    List<String> resultList = BPCGenerator.convertMapToPrefixString(resultMap, order * 2);
                    endTime = System.nanoTime();
                    totalListTime += (endTime - startTime);
                }

                // 计算每个希尔伯特阶数的平均总耗时
                long averageMapTime = totalMapTime / expTimes;
                long averageListTime = totalListTime / expTimes;
                long totalAverageTime = (totalMapTime + totalListTime) / expTimes;

                // 输出时间转换为毫秒(ms)和纳秒(ns)，并输出
                System.out.printf("Order: %d | Average time for resultMap: %.3f ms (%.0f ns) | Average time for resultList: %.3f ms (%.0f ns) | Average total time: %.3f ms (%.0f ns)%n",
                        order,
                        averageMapTime / 1_000_000.0, (double) averageMapTime,
                        averageListTime / 1_000_000.0, (double) averageListTime,
                        totalAverageTime / 1_000_000.0, (double) totalAverageTime);
            }
        }
    }

    // 输出矩阵的简单方法
    private void printMatrix(BigInteger[][] matrix) {
        for (BigInteger[] row : matrix) {
            for (BigInteger value : row) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }
}
