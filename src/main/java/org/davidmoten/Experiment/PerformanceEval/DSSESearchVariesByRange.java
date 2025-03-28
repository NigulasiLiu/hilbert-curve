package org.davidmoten.Experiment.PerformanceEval;

import org.davidmoten.Experiment.Comparison.FixRangeCompareToConstructionOne;
import org.davidmoten.Scheme.RSKQ.RSKQ_Biginteger;
import org.davidmoten.Scheme.SKQ.SKQ_Biginteger;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

import static org.davidmoten.Experiment.TestByUserInput.BRQComparisonInput.generateHilbertMatrix;

public class DSSESearchVariesByRange {

    public static void main(String[] args) throws Exception {
        // 输入文件列表
        String[] filepaths = {
//                "src/dataset/spatial_data_set_2W.csv",
//                "src/dataset/spatial_data_set_4W.csv",
//                "src/dataset/spatial_data_set_6W.csv",
//                "src/dataset/spatial_data_set_8W.csv",
                "src/dataset/spatial_data_set_10W.csv"
        };

        // 参数配置
        int rangePredicate = 1 << 30; // 范围谓词
        int[] maxfilesArray = {1 << 20}; // 最大文件数
//        int[] hilbertOrders = {8, 10, 12}; // Hilbert 曲线阶数
        int[] hilbertOrders = {10}; // Hilbert 曲线阶数
        int div = 100; // 划分搜索区域比例
        int[] searchtimes = {9000, 1000}; // 预先搜索和正式搜索次数
//        int[] rangeList = {3, 6, 9}; // 搜索范围（边长百分比）
        Random random = new Random();
        int[] keyNums = {4,8,12}; // Hilbert 曲线阶数
        int[] rangeList = {8,10,12}; // 搜索范围（边长百分比）
//        int[] rangeList = {2, 4, 6, 8, 10, 12}; // 搜索范围（边长百分比）

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
