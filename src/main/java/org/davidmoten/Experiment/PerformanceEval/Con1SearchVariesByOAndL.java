package org.davidmoten.Experiment.PerformanceEval;

import org.davidmoten.Experiment.Comparison.FixRangeCompareToConstructionOne;
import org.davidmoten.Scheme.Construction.ConstructionOne;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Con1SearchVariesByOAndL {

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
//        int[] maxfilesArray = {1 << 18, 1 << 19,1 << 20, 1 << 21,1 << 22, 1 << 23,1 << 24}; // 不同实验场景的最大文件数配置
        int[] maxfilesArray = {1 << 20}; // 不同实验场景的最大文件数配置
        int[] hilbertOrders = {8,10,12,14,16}; // Hilbert 曲线阶数
        int div = 100; // 划分搜索区域比例
        int[] searchtimes = {900, 100}; // 预先搜索和正式搜索次数
//        int[] rangeList = {3, 6, 9}; // 搜索范围（边长百分比）
        Random random = new Random();
        int[] keyNums = {4,8,12}; // Hilbert 曲线阶数
//        int[] rangeList = {8,10,12}; // 搜索范围（边长百分比）
        int[] rangeList = {3}; // 搜索范围（边长百分比）

        // 遍历文件列表，逐一进行实验
        for (String filepath : filepaths) {
            System.out.printf("加载数据文件：%s\n", filepath);

            // 加载数据到内存（使用 FixRangeCompareToConstructionOne 的静态方法）
            List<FixRangeCompareToConstructionOne.DataRow> dataRows = FixRangeCompareToConstructionOne.loadDataFromFile(filepath);

            // 开始性能评估
            for(int maxfilearray : maxfilesArray) {
                System.out.printf("实例 maxFiles: %d\n", maxfilearray);
                for (int range : rangeList) {
                    System.out.printf("GRQ Range %d%%\n", range);
                    for (int hilbertOrder : hilbertOrders) {
                        System.out.printf("正在初始化实例 | hilbertOrder: %d\n", hilbertOrder);
                        List<Double> consOneSearchTimes = new ArrayList<>();
                        int lambda = 128;
                        int maxCoordinate = 1 << hilbertOrder;
                        int t = (int) (Math.log(maxCoordinate) / Math.log(2));
                        int[] xCoordinates = new int[dataRows.size()];
                        int[] yCoordinates = new int[dataRows.size()];

                        for (int i = 0; i < dataRows.size(); i++) {
                            FixRangeCompareToConstructionOne.DataRow row = dataRows.get(i);
                            xCoordinates[i] = Math.toIntExact(row.pointX);
                            yCoordinates[i] = Math.toIntExact(row.pointY);
                        }
                        ConstructionOne con1 = new ConstructionOne(lambda, t, dataRows.size(), xCoordinates, yCoordinates);
                        con1.BTx = con1.buildBinaryTree(t);
                        con1.BTy = con1.buildBinaryTree(t);
                        Map<Integer, String> Sx = con1.buildxNodeInvertedIndex(con1.buildInvertedIndex(t, dataRows.size(), xCoordinates), t);
                        Map<Integer, String> Sy = con1.buildyNodeInvertedIndex(con1.buildInvertedIndex(t, dataRows.size(), yCoordinates), t);
                        con1.setupEDS(Sx, Sy);

                        int edgeLength = 1 << hilbertOrder;

                        // 正式搜索
                        for (int i = 0; i < searchtimes[1]; i++) {
                            int xstart = random.nextInt(edgeLength * (div - range) / div);
                            int ystart = random.nextInt(edgeLength * (div - range) / div);
                            int searchRange = edgeLength * range / div;
                            if (i % 10 == 0) {
                                int[] rangex = con1.rangeConvert(t, new int[]{xstart, xstart + searchRange});
                                int[] rangey = con1.rangeConvert(t, new int[]{ystart, ystart + searchRange});
                                long startTime = System.nanoTime();
                                con1.clientSearch(rangex, rangey, t);
                                long endTime = System.nanoTime();
                                consOneSearchTimes.add((endTime - startTime) / 1e6);
                            }
                        }
                        // 打印正式搜索时间
                        System.out.printf("\n正式搜索完成，平均搜索时间: |Construction1(avg of 100 times): |%-10.6f|ms|\n",
                                consOneSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                    }
                }
            }
        }
    }
}
