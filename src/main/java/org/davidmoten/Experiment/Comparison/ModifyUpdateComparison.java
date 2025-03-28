package org.davidmoten.Experiment.Comparison;

import org.davidmoten.Scheme.RSKQ.RSKQ_Biginteger;
import org.davidmoten.Scheme.SKQ.SKQ_Biginteger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UpdateComparison 实验代码
 * <p>
 * 功能目的：
 * 1. 评估两种空间查询方案（RSKQ 和 TDSC2023）在不同更新类型（添加和修改）下的性能表现。
 * 2. 测试不同 Hilbert 曲线阶数和文件大小（maxFiles）的组合对更新性能的影响。
 * 3. 通过调整 α 参数控制更新类型的分布，测试其对更新性能的影响。
 * <p>
 * 主要实验内容：
 * - 通过读取 CSV 文件加载空间数据。
 * - 根据 α 值动态生成更新类型的分布：
 * - α 表示“修改”类型更新的占比。
 * - 对比两种方案的平均更新时间。
 * - 打印各实验配置的性能结果。
 * <p>
 * 核心实现：
 * 1. 读取数据文件，生成更新操作所需的 `pSet` 和 `keywordItemSets`。
 * 2. 根据 α 计算每种更新类型的数量，随机生成更新类型分布。
 * 3. 在每种实验配置下，对比 RSKQ 和 TDSC2023 的更新性能。
 */
public class ModifyUpdateComparison {

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
        int[] hilbertOrders = {8,10,12}; // Hilbert 曲线的阶数配置

        // α 参数，控制“修改”类型更新的占比
        double[] alphas = new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};

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

                    // 初始化 RSKQ 和 TDSC2023 实例
                    RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxFiles, hilbertOrder, 2);
                    SKQ_Biginteger tdsc2023_biginteger = new SKQ_Biginteger(128, rangePredicate, maxFiles, hilbertOrder, 2);

                    String[][] keywordItemSets = new String[objectnums][attachedKeywords];
                    int rowCount = 0; // 当前处理的行数

                    // 读取 CSV 文件并执行更新操作
                    try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
                        String line;
                        boolean isFirstLine = true;

                        while ((line = br.readLine()) != null) {
                            // 跳过表头
                            if (isFirstLine) {
                                isFirstLine = false;
                                continue;
                            }

                            // 解析 CSV 行数据
                            String[] fields = line.split(",");
                            if (fields.length < 15) {
                                System.err.println("Invalid row format: " + line);
                                continue;
                            }

                            // 提取字段
                            int fileID = Integer.parseInt(fields[0]) % maxFiles;
                            long pointX = Long.parseLong(fields[1]);
                            long pointY = Long.parseLong(fields[2]);
                            System.arraycopy(fields, 4, keywordItemSets[rowCount], 0, attachedKeywords);

                            long[] pSet = new long[]{pointX, pointY};
                            String[] ops;
                            int[] files;

                            // 根据更新类型执行不同操作
                            if (updateTypes.get(rowCount)) {
                                // 第一种情况：修改更新
                                ops = new String[]{"add", "del"};
                                long start = System.nanoTime();
                                spqs.ObjectUpdate(pSet, keywordItemSets[rowCount], ops, new int[]{fileID, (fileID + 1) % maxFiles});
                                double rskqTime = (System.nanoTime() - start) / 1e6;

                                long tdscStart = System.nanoTime();
                                tdsc2023_biginteger.update(pSet, keywordItemSets[rowCount], ops[0], new int[]{fileID}, objectnums);
                                tdsc2023_biginteger.update(pSet, keywordItemSets[rowCount], ops[1], new int[]{(fileID + 1) % maxFiles}, objectnums);
                                double tdscTime = (System.nanoTime() - tdscStart) / 1e6;

                                averageRSKQUpdateTimes.add(rskqTime);
                                averageTDSCUpdateTimes.add(tdscTime);
                            } else {
                                // 第二种情况：添加更新
                                ops = new String[]{"add"};
                                files = new int[]{fileID};
                                long start = System.nanoTime();
                                spqs.ObjectUpdate(pSet, keywordItemSets[rowCount], ops, files);
                                double rskqTime = (System.nanoTime() - start) / 1e6;

                                long tdscStart = System.nanoTime();
                                tdsc2023_biginteger.update(pSet, keywordItemSets[rowCount], ops[0], files, objectnums);
                                double tdscTime = (System.nanoTime() - tdscStart) / 1e6;

                                averageRSKQUpdateTimes.add(rskqTime);
                                averageTDSCUpdateTimes.add(tdscTime);
                            }

                            rowCount++;
                        }
                    }
                    removeKMaxAndMin(averageRSKQUpdateTimes,5);
                    removeKMaxAndMin(averageTDSCUpdateTimes,5);
                    // 计算平均更新时间
                    double avgRskqTime = averageRSKQUpdateTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double avgTdscTime = averageTDSCUpdateTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                    // 打印实验结果
                    System.out.printf("实验完成 α = %.2f | maxFiles: %d, hilbertOrder: %d | RSKQ 平均更新时间: %-10.6f ms | TDSC 平均更新时间: %-10.6f ms\n",
                            alpha, maxFiles, hilbertOrder, avgRskqTime, avgTdscTime);
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
