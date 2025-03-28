package org.davidmoten.Experiment.PerformanceEval;

import org.davidmoten.Experiment.Comparison.FixRangeCompareToConstructionOne;
import org.davidmoten.Scheme.Construction.ConstructionOne;
import org.davidmoten.Scheme.RSKQ.RSKQ_Biginteger;
import org.davidmoten.Scheme.SKQ.SKQ_Biginteger;

import java.lang.reflect.Array;
import java.util.*;

import static org.davidmoten.Experiment.Comparison.FixRangeCompareToConstructionOne.loadDataFromFile;
import static org.davidmoten.Experiment.Comparison.UpdateComparison.countLines;
import static org.davidmoten.Experiment.Comparison.UpdateComparison.removeKMaxAndMin;

public class Storage_Test {


    public static void main(String[] args) throws Exception {
        // 数据文件路径
        String filepath = "src/dataset/spatial_data_set_2W.csv";

        // 实验参数配置
        int rangePredicate = 1 << 30; // 范围谓词值，用于控制搜索范围
        int objectnums = 1000000; // 数据集大小（对象数量）
        int[] maxfilesArray = {1 << 20}; // 不同实验场景的最大文件数配置
        int[] hilbertOrders = {10}; // Hilbert 曲线的阶数配置

        int lambda = 128;
        System.out.printf("加载数据文件：%s\n", filepath);
        // 加载数据到内存
        List<FixRangeCompareToConstructionOne.DataRow> dataRows = loadDataFromFile(filepath);

        // 遍历 maxFiles 和 hilbertOrder 的组合
        for (int maxFiles : maxfilesArray) {
            for (int hilbertOrder : hilbertOrders) {
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
                SKQ_Biginteger tdsc2023_biginteger = new SKQ_Biginteger(lambda, rangePredicate, maxFiles, hilbertOrder, 2);
                // 使用 Set 来去重
                Set<String> uniqueP = new HashSet<>();
                Set<String> uniqueW = new HashSet<>();

                // 遍历 dataRows，处理每个数据行
                for (FixRangeCompareToConstructionOne.DataRow dataRow : dataRows) {
                    // 获取 preCode 的返回值并添加到 uniqueP 集合
                    List<String> P = spqs.preCode(new long[]{dataRow.pointX, dataRow.pointY});
                    uniqueP.addAll(P);

                    // 将 dataRow.keywords 转换为 List 后添加到 uniqueW 集合
                    List<String> W = Arrays.asList(dataRow.keywords);
                    uniqueW.addAll(W);
                }

                // 输出去重后的集合
                System.out.println("Unique P: " + uniqueP.size());
                System.out.println("Unique W: " + uniqueW.size());
                // 计算 (uniqueP.size() + uniqueW.size()) * (3*128 + 3*(1<<20))bit，并转换为GB
                long totalSizeBits = (uniqueP.size() + uniqueW.size()) * (3 * 128 + 3 * (1 << 20));
                double totalSizeGB = totalSizeBits / (8.0 * 1024 * 1024 * 1024); // 转换为GB
                System.out.println("Total size (GB): " + totalSizeGB);

                // 循环遍历数据行
                for (int i = 0; i < maxCoordinate; i++) {
                    FixRangeCompareToConstructionOne.DataRow dataRow = dataRows.get(i - dataRows.size() < 0 ? i : i - dataRows.size());

                    // 计算Pi和Pi_prime
                    int[] Pi = new int[]{(int) dataRow.pointX % maxCoordinate, (int) dataRow.pointY % maxCoordinate};
                    int[] Pi_prime = new int[]{(int) (dataRow.pointX * dataRow.pointX) % maxCoordinate,
                            (int) (dataRow.pointY * dataRow.pointY) % maxCoordinate};

                    // 调用客户端更新和服务器更新
                    List<List<String>> updates = con1.clientUpdate(Pi, Pi_prime);
                    con1.serverUpdate(updates);
                }

                // 输出去重后的集合
                System.out.println("Unique Ux: " + con1.Ux.size());
                System.out.println("Unique Uy: " + con1.Uy.size());
                // 计算 (con1.Ux.size() + con1.Uy.size()) * (128 + 128)bit，并转换为GB
                long totalSizeBitsUxUy = (con1.Ux.size() + con1.Uy.size()) * (128 + 128);
                double totalSizeGBUxUy = totalSizeBitsUxUy / (8.0 * 1024 * 1024 * 1024); // 转换为GB
                System.out.println("Total size (GB) for Ux and Uy: " + totalSizeGBUxUy);
            }
        }
    }
}
