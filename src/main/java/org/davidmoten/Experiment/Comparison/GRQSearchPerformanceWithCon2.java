package org.davidmoten.Experiment.Comparison;

import org.davidmoten.Scheme.Construction.ConstructionTwo;
import org.davidmoten.Scheme.RSKQ.RSKQ_Biginteger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.davidmoten.Experiment.UseDataAccessClass.BRQComparisonInput.generateHilbertMatrix;

public class GRQSearchPerformanceWithCon2 {

    public static void main(String[] args) throws Exception {
        String filepath = "src/dataset/spatial_data_for_update.csv";

        // 参数配置
        int delupdatetimes = 0;
        int batchSize = 500; // 每次处理x个更新
        int objectnums = 100000; // 数据集大小
        int rangePredicate = 100000;
        int[] maxfilesArray = {1 << 20};
        int[] hilbertOrders = {8, 10, 12};
        int edgeLength = 1 << hilbertOrders[0];
        int div = 100;
        int[] searchtimes = {9000, 1000};
        int[] rangeList = {3, 6, 9};
        Random random = new Random();
        // 加载数据到内存
        List<DataRow> dataRows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                // 跳过表头
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // 按行解析数据
                String[] fields = line.split(",");
                if (fields.length < 15) {
                    System.err.println("Invalid row format: " + line);
                    continue;
                }

                // 提取数据并存储到列表中
                int fileID = Integer.parseInt(fields[0]);
                long pointX = Long.parseLong(fields[1]);
                long pointY = Long.parseLong(fields[2]);
//                String[] keywords = Arrays.copyOfRange(fields, 4, 4 + searchKeywords);

                dataRows.add(new DataRow(fileID, pointX, pointY, new String[]{}));
            }
        }

        // 开始性能评估
        System.out.printf("实例 maxFiles: %d\n", maxfilesArray[0]);
        for (int range : rangeList) {
            System.out.printf("GRQ Range %d%%\n", range);
            for (int hilbertOrder : hilbertOrders) {
                System.out.printf("正在初始化实例 | hilbertOrder: %d\n", hilbertOrder);
                RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxfilesArray[0], hilbertOrder, 2);
                //初始化实例ConsOne
                List<Double> con2SearchTimes = new ArrayList<>();
                int lambda = 128;
                // 确定合理的 t 值，根据最大边界来设置
                int maxCoordinate = 1 << hilbertOrders[0]; // 假设 0 <= x, y <= (1 << 17)
                int t = (int) (Math.log(maxCoordinate) / Math.log(2));
                // 分离 x 和 y 坐标
                int[] xCoordinates = new int[dataRows.size()];
                int[] yCoordinates = new int[dataRows.size()];
                // 执行更新操作
                for (int i = 0; i < dataRows.size(); i++) {
                    DataRow row = dataRows.get(i);

                    long[] pSet = new long[]{row.pointX, row.pointY};
                    int[] files = new int[]{row.fileID};

                    spqs.ObjectUpdate(pSet, row.keywords, new String[]{"add"}, files);
                    xCoordinates[i] = Math.toIntExact(row.pointX);
                    yCoordinates[i] = Math.toIntExact(row.pointY);

//                    // 更新进度条
//                    printProgressBar(i, dataRows.size());
                }
                //初始化实例ConsOne
                ConstructionTwo con2 = new ConstructionTwo(lambda, t, dataRows.size(), xCoordinates, yCoordinates);
                // 构建con1,con2二叉树和倒排索引
                con2.BTx = con2.buildBinaryTree(t);
                con2.BTy = con2.buildBinaryTree(t);
                Map<Integer, String> Sx = con2.buildxNodeInvertedIndex(con2.buildInvertedIndex(t, dataRows.size(), xCoordinates), t);
                Map<Integer, String> Sy = con2.buildyNodeInvertedIndex(con2.buildInvertedIndex(t, dataRows.size(), yCoordinates), t);
                con2.setupEDS(Sx, Sy);

                // 预先搜索
                int xstart = random.nextInt(edgeLength * (div - 1) / div);
                int ystart = random.nextInt(edgeLength * (div - 1) / div);
                int searchRange = edgeLength / div;

                BigInteger[][] matrixToSearch = generateHilbertMatrix(
                        spqs.hilbertCurve, xstart, ystart, searchRange, searchRange);

                for (int i = 0; i < searchtimes[0]; i++) {
                    spqs.GRQSearch(matrixToSearch);

                    int[] rangex = con2.rangeConvert(t, new int[]{xstart, xstart + searchRange});
                    int[] rangey = con2.rangeConvert(t, new int[]{ystart, ystart + searchRange});
                    long startTime = System.nanoTime();
                    con2.clientSearch(rangex, rangey, t);
                    long endTime = System.nanoTime();
                    con2SearchTimes.add((endTime - startTime) / 1e6);
                    if (i > 0 && i % 1000 == 0) {
                        System.out.printf("\n %d次预先搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|| Construction2: |%-10.6f|ms|\n",
                                i, spqs.getAverageSearchTime(),
                                con2SearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                        spqs.clearSearchTime();
                        con2SearchTimes.clear();
                    }
                }

                // 打印预先搜索时间
                System.out.printf("\n预先搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|| Construction2: |%-10.6f|ms|\n",
                        spqs.getAverageSearchTime(),
                        con2SearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                spqs.clearSearchTime();
                con2SearchTimes.clear();

                // 正式搜索
                xstart = random.nextInt(edgeLength * (div - range) / div);
                ystart = random.nextInt(edgeLength * (div - range) / div);
                searchRange = edgeLength * range / div;

                matrixToSearch = generateHilbertMatrix(
                        spqs.hilbertCurve, xstart, ystart, searchRange, searchRange);

                for (int i = 0; i < searchtimes[1]; i++) {
                    spqs.GRQSearch(matrixToSearch);

                    int[] rangex = con2.rangeConvert(t, new int[]{xstart, xstart + searchRange});
                    int[] rangey = con2.rangeConvert(t, new int[]{ystart, ystart + searchRange});
                    long startTime = System.nanoTime();
                    con2.clientSearch(rangex, rangey, t);
                    long endTime = System.nanoTime();
                    con2SearchTimes.add((endTime - startTime) / 1e6);
                }

                // 打印正式搜索时间
                System.out.printf("\n正式搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|| Construction2: |%-10.6f|ms|\n",
                        spqs.getAverageSearchTime(),
                        con2SearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
            }
        }
    }

    static class DataRow {
        int fileID;
        long pointX;
        long pointY;
        String[] keywords;

        DataRow(int fileID, long pointX, long pointY, String[] keywords) {
            this.fileID = fileID;
            this.pointX = pointX;
            this.pointY = pointY;
            this.keywords = keywords;
        }
    }
}
