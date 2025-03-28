package org.davidmoten.Experiment.Comparison;

import org.davidmoten.Scheme.Construction.ConstructionOne;
import org.davidmoten.Scheme.RSKQ.RSKQ_Biginteger;
import org.davidmoten.Scheme.SKQ.SKQ_Biginteger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.davidmoten.Experiment.Comparison.FixRangeCompareToConstructionOne.loadDataFromFile;
import static org.davidmoten.Scheme.Construction.ConstructionOne.generateRandomCoordinatePair;

public class UpdateComparison {

    // 统计 CSV 文件的行数
    public static int countLines(String filePath) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            int lines = 0;
            while (br.readLine() != null) {
                lines++;
            }
            return lines;
        }
    }

    public static void main(String[] args) throws Exception {
        // 数据文件路径
        String filepath = "src/dataset/spatial_data_set_2W.csv";

        // 实验参数配置
        int rangePredicate = 1 << 30; // 范围谓词值，用于控制搜索范围
        int objectnums = 1000000; // 数据集大小（对象数量）
        int attachedKeywords = 12; // 每个对象的关键词数量
//        int[] maxfilesArray = {1 << 18, 1 << 19,1 << 20, 1 << 21,1 << 22, 1 << 23,1 << 24}; // 不同实验场景的最大文件数配置
//        int[] maxfilesArray = {1 << 19, 1 << 21, 1 << 23}; // 不同实验场景的最大文件数配置
        int[] maxfilesArray = {1 << 20}; // 不同实验场景的最大文件数配置
        int[] hilbertOrders = {8, 9, 10, 11, 12, 13, 14}; // Hilbert 曲线的阶数配置

        // α 参数，控制“修改”类型更新的占比
        double[] alphas = new double[]{1.0};
        System.out.printf("加载数据文件：%s\n", filepath);
        // 加载数据到内存
        List<FixRangeCompareToConstructionOne.DataRow> dataRows = loadDataFromFile(filepath);
        // 遍历每个 α 值
        for (double alpha : alphas) {
            System.out.printf("Testing with α = %.2f%n", alpha);

            // 计算 CSV 文件的总行数（减去表头）
            int totalLines = countLines(filepath) - 1;

            // 根据 α 值计算两种更新类型的数量
            int firstCaseCount = (int) (alpha * totalLines); // 修改类型更新的数量
            int secondCaseCount = totalLines - firstCaseCount; // 添加类型更新的数量

            // 创建更新类型的标记列表
            ArrayList<Boolean> updateTypes = new ArrayList<>();
            for (int i = 0; i < firstCaseCount; i++) updateTypes.add(true); // 修改类型标记
            for (int i = 0; i < secondCaseCount; i++) updateTypes.add(false); // 添加类型标记

            // 打乱标记顺序，确保更新类型的随机性
            Collections.shuffle(updateTypes);

            // 遍历 maxFiles 和 hilbertOrder 的组合
            for (int maxFiles : maxfilesArray) {
                for (int hilbertOrder : hilbertOrders) {
                    System.out.printf("正在初始化 RSKQ_Biginteger 实例 | maxFiles: %d, hilbertOrder: %d\n", maxFiles, hilbertOrder);

                    // 用于存储更新时间的列表
                    List<Double> averageRSKQUpdateTimes = new ArrayList<>();
                    List<Double> averageTDSCUpdateTimes = new ArrayList<>();
                    List<Double> averageCon1UpdateTimes = new ArrayList<>();
                    int lambda = 128;
                    int maxCoordinate = 1 << hilbertOrder;
                    int t = (int) (Math.log(maxCoordinate) / Math.log(2));
                    int[] xCoordinates = new int[maxCoordinate];
                    int[] yCoordinates = new int[maxCoordinate];
                    for (int i = 0; i < maxCoordinate; i++) {
                        FixRangeCompareToConstructionOne.DataRow row = dataRows.get(i - dataRows.size() < 0 ? i : i - dataRows.size());
                        xCoordinates[i] = (int) (row.pointX % maxCoordinate);
                        yCoordinates[i] = (int) (row.pointY % maxCoordinate);
                    }
                    ConstructionOne con1 = new ConstructionOne(lambda, t, maxCoordinate, xCoordinates, yCoordinates);
                    con1.BTx = con1.buildBinaryTree(t);
                    con1.BTy = con1.buildBinaryTree(t);
                    Map<Integer, String> Sx = con1.buildxNodeInvertedIndex(con1.buildInvertedIndex(t, maxCoordinate, xCoordinates), t);
                    Map<Integer, String> Sy = con1.buildyNodeInvertedIndex(con1.buildInvertedIndex(t, maxCoordinate, yCoordinates), t);
                    con1.setupEDS(Sx, Sy);

                    // 初始化 RSKQ 和 TDSC2023 实例
                    RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxFiles, hilbertOrder, 2);
                    SKQ_Biginteger tdsc2023_biginteger = new SKQ_Biginteger(128, rangePredicate, maxFiles, hilbertOrder, 2);

                    int rowCount = 0; // 当前处理的行数
                    for (FixRangeCompareToConstructionOne.DataRow dataRow : dataRows) {
                        // 根据更新类型执行不同操作
                        if (updateTypes.get(rowCount)) {
                            // 第一种情况：修改更新
                            String[] ops = new String[]{"add", "del"};
                            long[] pSet = new long[]{dataRow.pointX, dataRow.pointY};
                            int fileID = dataRow.fileID;
                            long start = System.nanoTime();
                            spqs.ObjectUpdate(pSet, dataRow.keywords, ops, new int[]{fileID, (fileID + 1) % maxFiles});
                            double rskqTime = (System.nanoTime() - start) / 1e6;

                            long tdscStart = System.nanoTime();
                            tdsc2023_biginteger.update(pSet, dataRow.keywords, ops[0], new int[]{fileID}, objectnums);
                            tdsc2023_biginteger.update(pSet, dataRow.keywords, ops[1], new int[]{(fileID + 1) % maxFiles}, objectnums);
                            double tdscTime = (System.nanoTime() - tdscStart) / 1e6;
//                            System.out.printf("Update time for %d updates: %.5f ms%n", totalLines, elapsedTime);
                            averageRSKQUpdateTimes.add(rskqTime);
                            averageTDSCUpdateTimes.add(tdscTime);
                        }
                    }
                    for (int i = 0; i < maxCoordinate; i++) {
                        FixRangeCompareToConstructionOne.DataRow dataRow = dataRows.get(i - dataRows.size() < 0 ? i : i - dataRows.size());

                        int[] Pi = new int[]{(int) dataRow.pointX % maxCoordinate, (int) dataRow.pointY % maxCoordinate};
                        int[] Pi_prime = new int[]{(int) (dataRow.pointX * dataRow.pointX) % maxCoordinate,
                                (int) (dataRow.pointY * dataRow.pointY) % maxCoordinate};

                        long startTime = System.nanoTime();
                        List<List<String>> updates = con1.clientUpdate(Pi, Pi_prime);
                        con1.serverUpdate(updates);
                        long endTime = System.nanoTime();

                        double elapsedTime = (endTime - startTime) / 1e6; // 转换为毫秒
                        averageCon1UpdateTimes.add(elapsedTime);
                    }
                    removeKMaxAndMin(averageRSKQUpdateTimes, 3);
                    removeKMaxAndMin(averageTDSCUpdateTimes, 3);
                    removeKMaxAndMin(averageCon1UpdateTimes, 3);
                    // 计算平均更新时间
                    double avgRskqTime = averageRSKQUpdateTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double avgTdscTime = averageTDSCUpdateTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double avgCon1Time = averageCon1UpdateTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);


                    // 打印实验结果
                    System.out.printf("实验完成 α = %.2f | maxFiles: %d, hilbertOrder: %d | RSKQ 平均更新时间: %-10.6f ms | TDSC 平均更新时间: %-10.6f ms | Con1 平均更新时间: %-10.6f ms\n",
                            alpha, maxFiles, hilbertOrder, avgRskqTime, avgTdscTime, avgCon1Time);
                }
            }
        }
    }

    // 移除list中k个最大值和k个最小值
    public static void removeKMaxAndMin(List<Double> list, int k) {
        if (list == null || list.size() <= 2 * k) {
            System.out.println("List is too small to remove k max and min values.");
            return;
        }

        // 对列表进行排序
        Collections.sort(list);

        // 移除k个最小值
        for (int i = 0; i < k; i++) {
            list.remove(0);  // 移除第一个元素（最小值）
        }

        // 移除k个最大值
        for (int i = 0; i < k; i++) {
            list.remove(list.size() - 1);  // 移除最后一个元素（最大值）
        }
    }
}
