package org.davidmoten.Experiment.BPCTest;

import org.davidmoten.BPC.BPCGenerator;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static org.davidmoten.Experiment.TestByUserInput.BRQComparisonInput.generateHilbertMatrix;

public class BPCTest {

    private HilbertCurve hilbertCurve;
    private int order;

    // 构造函数初始化HilbertCurve实例
    public BPCTest(int order) {
        this.order = order;
        this.hilbertCurve = HilbertCurve.bits(order).dimensions(2); // 创建HilbertCurve实例
    }

    public static void main(String[] args) throws IOException {
        Random random = new Random();

        // 定义hilbertOrders和sizeRatios
        int[] hilbertOrders = {8, 10, 12};
        double[] sizeRatios = {0.02, 0.04, 0.06, 0.08, 0.10, 0.2, 0.4, 0.6};
        int[] Klist = new int[8]; // Klist {1, 2, 4, 8, ..., 2^8}
        for (int i = 0; i < 8; i++) {
            Klist[i] = (int) Math.pow(2, i);
        }

        // 创建CSV文件
        FileWriter csvWriter = new FileWriter("src/dataset/BPCTestResult.csv");
        // 写入CSV表头
        csvWriter.append("k, hilbertOrder, ratio1, ratio2, commonPrefixSize, resultSetSize, resultSetSizeNonZero, resultSetSizeZero\n");

        // 遍历Klist和hilbertOrders
        for (int k : Klist) {
            for (int hilbertOrder : hilbertOrders) {
                // 初始化BPCTest实例
                BPCTest bpctest = new BPCTest(hilbertOrder);
                int edgeLength = 1 << hilbertOrder; // 2的hilbertOrder次方
                long[] point1 = generateGaussianPoint(edgeLength, random);

                // 根据k生成第二个点
                long[] point2 = generateSecondPoint(point1, k, edgeLength, random);

                List<String> prefixList1 = bpctest.preCode(point1);
                List<String> prefixList2 = bpctest.preCode(point2);

                // 使用Set存储prefixList1和prefixList2的交集
                Set<String> commonPrefix = new HashSet<>(prefixList1);
                commonPrefix.retainAll(prefixList2);

                int commonPrefixSize = commonPrefix.size();

                // 统计变量
                int resultSetSizeNonZero = 0;
                int resultSetSizeZero = 0;

                // 累计结果值
                double avgCommonPrefixSize = 0;
                double avgResultSetSize = 0;

                // 开始搜索...
                // 进行100次搜索
                for (int i = 0; i < sizeRatios.length - 1; i++) {
                    for (int j = i + 1; j < sizeRatios.length; j++) {
                        // 使用 sizeRatios 中的值来定义查询区域的长宽
                        double ratio1 = sizeRatios[i];
                        double ratio2 = sizeRatios[j];

                        // 计算查询区域的宽度和高度
                        int xsearchRange = (int) (edgeLength * ratio1);
                        int ysearchRange = (int) (edgeLength * ratio2);

                        // 进行100次搜索，每次对xstart, ystart进行轻微扰动
                        int totalResultSetSize = 0;
                        int totalCommonPrefixSize = 0;
                        for (int n = 0; n < 100; n++) {
                            // 将 point1 视作区域内的点，计算查询区域的起始坐标
                            int xstart = (int) (point1[0] - xsearchRange / 2) + random.nextInt(5) - 2; // 扰动范围在[-2, 2]之间
                            int ystart = (int) (point1[1] - ysearchRange / 2) + random.nextInt(5) - 2; // 扰动范围在[-2, 2]之间

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

                            // 生成Hilbert矩阵并计算结果
                            BigInteger[][] matrixToSearch = generateHilbertMatrix(bpctest.hilbertCurve, xstart, ystart, xsearchRange, ysearchRange);
                            List<String> result = bpctest.preCover(matrixToSearch);

                            // 获取commonPrefix和result的交集
                            Set<String> resultSet = new HashSet<>(result);
                            resultSet.retainAll(commonPrefix);

                            int resultSetSize = resultSet.size();

                            // 更新统计值
                            totalCommonPrefixSize += commonPrefixSize;
                            totalResultSetSize += resultSetSize;

                            // 更新resultSet的计数
                            if (resultSetSize != 0) {
                                resultSetSizeNonZero++;
                            } else {
                                resultSetSizeZero++;
                            }
                        }

                        // 计算平均值
                        avgCommonPrefixSize = totalCommonPrefixSize / 100.0;
                        avgResultSetSize = totalResultSetSize / 100.0;

                        // 将每次搜索的平均值和统计信息写入到CSV文件
                        csvWriter.append(k + ", " + hilbertOrder + ", " + ratio1 + ", " + ratio2 + ", " + avgCommonPrefixSize + ", " + avgResultSetSize + ", " + resultSetSizeNonZero + ", " + resultSetSizeZero + "\n");
                    }
                }
            }
        }

        // 关闭文件写入器
        csvWriter.flush();
        csvWriter.close();
    }

    // 生成符合高斯分布的点，返回一个长度为2的数组[x, y]
    public static long[] generateGaussianPoint(int edgeLength, Random random) {
        // 使用高斯分布生成点
        double x = random.nextGaussian() * edgeLength / 3 + edgeLength / 2;
        double y = random.nextGaussian() * edgeLength / 3 + edgeLength / 2;

        // 限制点的范围在[0, edgeLength]内
        x = Math.max(0, Math.min(x, edgeLength - 1));
        y = Math.max(0, Math.min(y, edgeLength - 1));

        return new long[]{(long) x, (long) y};
    }

    // 根据k值生成第二个点，确保k不会超出边界
    public static long[] generateSecondPoint(long[] point1, int k, int edgeLength, Random random) {
        // 第二个点应该与第一个点相距k个格子
        // 通过随机选择方向来确定第二个点的偏移量
        int dx = random.nextBoolean() ? k : -k; // x轴方向偏移
        int dy = random.nextBoolean() ? k : -k; // y轴方向偏移

        // 计算第二个点的坐标，确保它在边界范围内
        long x2 = point1[0] + dx;
        long y2 = point1[1] + dy;

        // 如果偏移超出边界，强制将点放置在[0, 2^hilbertOrder)的区域内
        int maxCoordinate = edgeLength; // 2^hilbertOrder
        if (x2 < 0) x2 = 0;
        if (y2 < 0) y2 = 0;
        if (x2 >= maxCoordinate) x2 = maxCoordinate - 1;
        if (y2 >= maxCoordinate) y2 = maxCoordinate - 1;

        return new long[]{x2, y2};
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

    // 获取Set的前N个元素
    public static Set<String> getFirstNElements(Set<String> set, int n) {
        Set<String> result = new LinkedHashSet<>();
        int count = 0;
        for (String s : set) {
            if (count >= n) break;
            result.add(s);
            count++;
        }
        return result;
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
