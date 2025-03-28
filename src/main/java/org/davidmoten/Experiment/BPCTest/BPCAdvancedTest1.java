package org.davidmoten.Experiment.BPCTest;

import org.davidmoten.BPC.BPCGenerator;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import static org.davidmoten.Experiment.TestByUserInput.BRQComparisonInput.generateHilbertMatrix;
public class BPCAdvancedTest1 {

    private HilbertCurve hilbertCurve;
    private int order;

    // 构造函数初始化HilbertCurve实例
    public BPCAdvancedTest1(int order) {
        this.order = order;
        this.hilbertCurve = HilbertCurve.bits(order).dimensions(2); // 创建HilbertCurve实例
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        Random random = new Random();

        // 定义hilbertOrders和sizeRatios
        int[] hilbertOrders = {8, 9,10,11, 12};
        double[] sizeRatios = {0.02, 0.04, 0.06, 0.08, 0.10, 0.2, 0.4, 0.6};

        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(30);  // 使用更多的线程
        List<Future<Void>> futures = new ArrayList<>();

        // 遍历 hilbertOrders 和 sizeRatios，提交每个任务到线程池
        for (int hilbertOrder : hilbertOrders) {
            BPCAdvancedTest1 bpctest = new BPCAdvancedTest1(hilbertOrder);
            int edgeLength = 1 << hilbertOrder; // 2的hilbertOrder次方

            // 提交任务到线程池
            for (int i = 0; i < sizeRatios.length - 1; i++) {
                for (int j = i + 1; j < sizeRatios.length; j++) {
                    final double ratio1 = sizeRatios[i];
                    final double ratio2 = sizeRatios[j];

                    // 创建线程任务
                    Callable<Void> task = () -> {
                        String threadName = Thread.currentThread().getName();
                        System.out.println("线程 " + threadName + " 开始处理 hilbertOrder = " + hilbertOrder + ", ratio1 = " + ratio1 + ", ratio2 = " + ratio2);

                        // 进行5次实验
                        for (int rectNum = 0; rectNum < 5; rectNum++) {
                            // 每次实验时，清零统计量，确保每个实验独立
                            int experimentResultSetSizeNonZero = 0;
                            int experimentResultSetSizeZero = 0;

                            // 计算查询区域的宽度和高度
                            int xsearchRange = (int) (edgeLength * ratio1);
                            int ysearchRange = (int) (edgeLength * ratio2);

                            // 查询区域的中心点遵循二维高斯分布
                            double xmean = edgeLength / 2.0;
                            double ymean = edgeLength / 2.0;
                            double xstddev = edgeLength / 6.0;
                            double ystddev = edgeLength / 6.0;

                            int xcenter = (int) Math.max(0, Math.min(xmean + random.nextGaussian() * xstddev, edgeLength - 1));
                            int ycenter = (int) Math.max(0, Math.min(ymean + random.nextGaussian() * ystddev, edgeLength - 1));

                            // 计算查询区域的起始坐标
                            int xstart = xcenter - xsearchRange / 2;
                            int ystart = ycenter - ysearchRange / 2;

                            // 确保查询区域不超出边界，调整 xstart 和 ystart
                            int maxCoordinate = edgeLength - 1; // 最大坐标值是 edgeLength - 1
                            if (xstart < 0) {
                                xstart = 0; // xstart 不能小于 0
                            } else if (xstart + xsearchRange > maxCoordinate) {
                                xstart = maxCoordinate - xsearchRange; // xstart 不能超出边界
                            }

                            if (ystart < 0) {
                                ystart = 0; // ystart 不能小于 0
                            } else if (ystart + ysearchRange > maxCoordinate) {
                                ystart = maxCoordinate - ysearchRange; // ystart 不能超出边界
                            }
                            int testCount = xsearchRange*ysearchRange;
                            // 生成Hilbert矩阵并计算结果
                            BigInteger[][] matrixToSearch = generateHilbertMatrix(bpctest.hilbertCurve, xstart, ystart, xsearchRange, ysearchRange);
                            List<String> result = bpctest.preCover(matrixToSearch);

                            // 进行100次搜索
                            for (int n = 0; n < testCount; n++) {
                                // 从查询区域内随机选取point1和point2
                                long[] point1 = new long[]{xstart + random.nextInt(xsearchRange), ystart + random.nextInt(ysearchRange)};
                                long[] point2 = new long[]{xstart + random.nextInt(xsearchRange), ystart + random.nextInt(ysearchRange)};

                                List<String> prefixList1 = bpctest.preCode(point1);
                                List<String> prefixList2 = bpctest.preCode(point2);

                                // 使用Set存储prefixList1和prefixList2的交集
                                Set<String> commonPrefix = new HashSet<>(prefixList1);
                                commonPrefix.retainAll(prefixList2);

                                // 获取commonPrefix和result的交集
                                Set<String> resultSet = new HashSet<>(result);
                                resultSet.retainAll(commonPrefix);

                                int resultSetSize = resultSet.size();

                                // 更新resultSet的计数
                                if (resultSetSize != 0) {
                                    experimentResultSetSizeNonZero++;
                                } else {
                                    experimentResultSetSizeZero++;
                                }
                            }
                            double alpha = (double)experimentResultSetSizeNonZero/(experimentResultSetSizeZero+experimentResultSetSizeNonZero);
                            // 获取每个线程的文件名
                            String fileName = String.format("src/dataset/BPCTestResult/%d_%.2f_%.2f_result.csv", hilbertOrder, ratio1, ratio2);
                            // 写入到该文件
                            try (FileWriter csvWriter = new FileWriter(fileName, true)) {
                                csvWriter.append(hilbertOrder + ", " + ratio1 + ", " + ratio2 + ", " + experimentResultSetSizeNonZero + ", " + experimentResultSetSizeZero + ", "+alpha+"\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        // 打印线程结束信息
                        System.out.println("线程 " + threadName + " 完成处理 hilbertOrder = " + hilbertOrder + ", ratio1 = " + ratio1 + ", ratio2 = " + ratio2);
                        return null; // 任务完成
                    };

                    // 提交任务到线程池
                    futures.add(executorService.submit(task));
                }
            }
        }

        // 等待所有任务完成
        for (Future<Void> future : futures) {
            future.get();
        }

        // 关闭线程池
        executorService.shutdown();
    }

    // 计算点的 Hilbert 索引并生成前缀字符串
    public List<String> preCode(long[] pSet) {
        // 计算点的 Hilbert 索引
        BigInteger pointHilbertIndex = this.hilbertCurve.index(pSet);

        // 必要的长度为 2 * order 位
        int requiredLength = 2 * order;

        // 获取 Hilbert 索引的二进制字符串，并补充前导零
        String binaryString = String.format("%" + requiredLength + "s", pointHilbertIndex.toString(2)).replace(' ', '0');

        // 初始化结果列表
        List<String> prefixList = new ArrayList<>(requiredLength + 1);

        // 添加完整的二进制字符串
        prefixList.add(binaryString);

        // 从最后一个字符开始替换为 '*'，逐步生成前缀
        StringBuilder builder = new StringBuilder(binaryString);
        for (int i = binaryString.length() - 1; i >= 0; i--) {
            builder.setCharAt(i, '*');
            prefixList.add(builder.toString());
        }

        return prefixList;
    }

    // 处理Hilbert矩阵并获取BPC结果
    public List<String> preCover(BigInteger[][] Matrix) {
        //生成min到max的所有Bigint
        BigInteger[] R = new BigInteger[Matrix.length * Matrix[0].length];
        for (int i = 0; i < Matrix.length; i++) {
            System.arraycopy(Matrix[i], 0, R, i * Matrix[0].length, Matrix[0].length);
        }

        // 获取BPC结果（包括分组）
        Map<Integer, List<BigInteger>> resultMap = BPCGenerator.GetBPCValueMap(R, this.order * 2);
        return BPCGenerator.convertMapToPrefixString(resultMap, this.order * 2);
    }
}
