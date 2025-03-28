package org.davidmoten.Experiment.Comparison;

import org.davidmoten.Scheme.RSKQ.RSKQ_Biginteger;
import org.davidmoten.Scheme.SKQ.SKQ_Biginteger;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

import static org.davidmoten.Experiment.TestByUserInput.BRQComparisonInput.generateHilbertMatrix;

/**
 * 这段代码的主要目的：
 * 1. 比较两种方案的搜索性能：
 *    - RSKQ_Biginteger
 *    - SKQ_Biginteger
 *
 * 主要实验内容：
 * 1. 依次加载 2W、4W、6W、8W 和 10W 数据集，进行初始化。
 * 2. 预先搜索（预热），测试性能和结果稳定性。
 * 3. 正式搜索，记录两种方案的平均搜索时间。
 * 4. 打印每种方案的性能结果，展示搜索效率对比。
 *
 * 实现过程：
 * - 数据加载：调用 `FixRangeCompareToConstructionOne` 类中的静态方法加载不同文件的数据。
 * - 文件中的数据为空间分布数据，基于 Hilbert 曲线进行搜索。
 * - 预先搜索：
 *   - 随机生成搜索范围，对两种方案进行多次预热查询。
 *   - 记录和输出预热阶段的平均查询时间。
 * - 正式搜索：
 *   - 在更复杂的查询范围内，比较两种方案的查询效率。
 *   - 输出正式搜索阶段的平均查询时间。
 */
public class FixRangeCompareToSKQ {

    public static void main(String[] args) throws Exception {
        // 输入文件列表
        String[] filepaths = {
                "src/dataset/spatial_data_set_2W.csv",
                "src/dataset/spatial_data_set_4W.csv",
                "src/dataset/spatial_data_set_6W.csv",
                "src/dataset/spatial_data_set_8W.csv",
                "src/dataset/spatial_data_set_10W.csv"
        };

        // 参数配置
        int rangePredicate = 1 << 30; // 范围谓词
        int[] maxfilesArray = {1 << 20}; // 最大文件数
        int[] hilbertOrders = {8, 10, 12}; // Hilbert 曲线阶数
        int div = 100; // 划分搜索区域比例
        int[] searchtimes = {9000, 1000}; // 预先搜索和正式搜索次数
        int[] rangeList = {3, 6, 9}; // 搜索范围（边长百分比）
        Random random = new Random();

        // 遍历文件列表，逐一进行实验
        for (String filepath : filepaths) {
            System.out.printf("加载数据文件：%s\n", filepath);

            // 加载数据到内存（使用 FixRangeCompareToConstructionOne 的静态方法）
            List<FixRangeCompareToConstructionOne.DataRow> dataRows = FixRangeCompareToConstructionOne.loadDataFromFile(filepath);

            // 开始性能评估
            System.out.printf("实例 maxFiles: %d\n", maxfilesArray[0]);
            for (int range : rangeList) {
                System.out.printf("GRQ Range %d%%\n", range);
                for (int hilbertOrder : hilbertOrders) {
                    System.out.printf("正在初始化实例 | hilbertOrder: %d\n", hilbertOrder);

                    RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxfilesArray[0], hilbertOrder, 2);
                    SKQ_Biginteger tdsc2023 = new SKQ_Biginteger(128, rangePredicate, maxfilesArray[0], hilbertOrder, 2);

                    // 执行更新操作
                    for (FixRangeCompareToConstructionOne.DataRow row : dataRows) {
                        long[] pSet = new long[]{row.pointX, row.pointY};
                        int[] files = new int[]{row.fileID};

                        spqs.ObjectUpdate(pSet, row.keywords, new String[]{"add"}, files);
                        tdsc2023.update(pSet, row.keywords, "add", files, rangePredicate);
                    }

                    int edgeLength = 1 << hilbertOrder; // Hilbert 边长
                    // 预先搜索
                    int xstart = random.nextInt(edgeLength * (div - 1) / div);
                    int ystart = random.nextInt(edgeLength * (div - 1) / div);
                    int searchRange = edgeLength / div;

                    BigInteger[][] matrixToSearch = generateHilbertMatrix(
                            spqs.hilbertCurve, xstart, ystart, searchRange, searchRange);

                    for (int i = 0; i < searchtimes[0]; i++) {
                        FixRangeCompareToConstructionOne.DataRow row = dataRows.get(random.nextInt(dataRows.size()));

                        spqs.ObjectSearch(matrixToSearch, row.keywords);
                        tdsc2023.Search(matrixToSearch, row.keywords);

                        if (i > 0 && i % 1000 == 0) {
                            System.out.printf("\n %d次预先搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|| TDSC_Biginteger: |%-10.6f|ms|\n",
                                    i, spqs.getAverageSearchTime(), tdsc2023.getAverageSearchTime());
                            spqs.clearSearchTime();
                            tdsc2023.clearSearchTime();
                        }
                    }

                    // 打印预先搜索时间
                    System.out.printf("\n预先搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|| TDSC_Biginteger: |%-10.6f|ms|\n",
                            spqs.getAverageSearchTime(), tdsc2023.getAverageSearchTime());

                    spqs.clearSearchTime();
                    tdsc2023.clearSearchTime();

                    // 正式搜索
                    xstart = random.nextInt(edgeLength * (div - range) / div);
                    ystart = random.nextInt(edgeLength * (div - range) / div);
                    searchRange = edgeLength * range / div;

                    matrixToSearch = generateHilbertMatrix(
                            spqs.hilbertCurve, xstart, ystart, searchRange, searchRange);

                    for (int i = 0; i < searchtimes[1]; i++) {
                        FixRangeCompareToConstructionOne.DataRow row = dataRows.get(random.nextInt(dataRows.size()));

                        spqs.ObjectSearch(matrixToSearch, row.keywords);
                        tdsc2023.Search(matrixToSearch, row.keywords);
                    }

                    // 打印正式搜索时间
                    System.out.printf("\n正式搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|| TDSC_Biginteger: |%-10.6f|ms|\n",
                            spqs.getAverageSearchTime(), tdsc2023.getAverageSearchTime());
                }
            }
        }
    }
}
