package org.davidmoten.Experiment.PerformanceEval;

import org.davidmoten.Scheme.RSKQ.RSKQ_Biginteger;
import org.davidmoten.Scheme.TDSC2023.TDSC2023_Biginteger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.*;

import static org.davidmoten.Experiment.UseDataAccessClass.BRQComparisonInput.generateHilbertMatrix;
import static org.davidmoten.Experiment.PerformanceEval.UpdatePerformance.printProgressBar;

public class SearchPerformance {

    public static void main(String[] args) throws Exception {
        String filepath = "src/dataset/spatial_data_for_update.csv";

        // 参数配置
        int delupdatetimes = 0;
        int batchSize = 500; // 每次处理x个更新
        int objectnums = 100000; // 数据集大小
        int rangePredicate = 1<<20;
        int[] maxfilesArray = {1 << 20};
        int[] hilbertOrders = {8, 10, 12};
        int edgeLength = 1 << hilbertOrders[0];
        int searchKeywords = 6;
        int div = 100;
        int[] searchtimes = {5000, 1000};
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
                String[] keywords = Arrays.copyOfRange(fields, 4, 4 + searchKeywords);

                dataRows.add(new DataRow(fileID, pointX, pointY, keywords));
            }
        }

        // 开始性能评估
        System.out.printf("实例 maxFiles: %d\n", maxfilesArray[0]);
        for (int range : rangeList) {
            System.out.printf("GRQ Range %d%%\n", range);
            for (int hilbertOrder : hilbertOrders) {
                System.out.printf("正在初始化实例 | hilbertOrder: %d\n", hilbertOrder);

                RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxfilesArray[0], hilbertOrder, 2);
                TDSC2023_Biginteger tdsc2023 = new TDSC2023_Biginteger(128, 1<<30, maxfilesArray[0], hilbertOrder, 2);

                // 执行更新操作
                for (int i = 0; i < dataRows.size(); i++) {
                    DataRow row = dataRows.get(i);

                    long[] pSet = new long[]{row.pointX, row.pointY};
                    int[] files = new int[]{row.fileID};

                    spqs.ObjectUpdate(pSet, row.keywords, new String[]{"add"}, files);
                    tdsc2023.update(pSet, row.keywords, "add", files, rangePredicate);

//                    // 更新进度条
//                    printProgressBar(i, dataRows.size());
//                    if ((i + 1) % batchSize == 0) {
//                        System.out.printf("Completed batch %d of updates.\n", (i + 1) / batchSize);
//                    }
                }

                // 预先搜索
                int xstart = random.nextInt(edgeLength * (div - 1) / div);
                int ystart = random.nextInt(edgeLength * (div - 1) / div);
                int searchRange = edgeLength / div;

                BigInteger[][] matrixToSearch = generateHilbertMatrix(
                        spqs.hilbertCurve, xstart, ystart, searchRange, searchRange);

                for (int i = 0; i < searchtimes[0]; i++) {
                    DataRow row = dataRows.get(random.nextInt(dataRows.size()));

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
                    DataRow row = dataRows.get(random.nextInt(dataRows.size()));

                    spqs.ObjectSearch(matrixToSearch, row.keywords);
                    tdsc2023.Search(matrixToSearch, row.keywords);
                }

                // 打印正式搜索时间
                System.out.printf("\n正式搜索完成，平均搜索时间: |RSKQ_Biginteger: |%-10.6f|ms|| TDSC_Biginteger: |%-10.6f|ms|\n",
                        spqs.getAverageSearchTime(), tdsc2023.getAverageSearchTime());
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
