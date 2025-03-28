package org.davidmoten.SpatialDataProcessor.BirminghanData;

import java.io.*;
import java.util.*;

public class GetKeywordUniverseSize {
    // 数据集路径
    private static final String FILE_PATH = "src/dataset/birminghan_large_final_1.csv";
    private static final String OUTPUT_FILE_PATH = "src/dataset/spatialdata.csv";

    // 去重后的关键字数量
    public static final int KEYWORDS_UNIVERSE;

    // 比KEYWORDS_UNIVERSE稍大的2的幂次值
    public static final int MAX_KEYWORDS;

    // 关键字到序号的映射表
    private static final Map<String, Integer> keywordToIndexMap = new HashMap<>();

    static {
        Set<String> uniqueKeys = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line = reader.readLine(); // 读取标题行
            if (line == null) {
                System.err.println("CSV文件为空或仅包含标题行。");
            } else {
                while ((line = reader.readLine()) != null) {
                    String[] columns = line.split(",");
                    // 假设key1到key12位于columns[3]到columns[14]
                    for (int i = 3; i < columns.length && i < 15; i++) {
                        String key = columns[i].trim();
                        if (!key.isEmpty()) {
                            uniqueKeys.add(key);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 生成关键字到序号的映射
        int index = 1;
        for (String key : uniqueKeys) {
            keywordToIndexMap.put(key, index++);
        }

        KEYWORDS_UNIVERSE = uniqueKeys.size();
        MAX_KEYWORDS = nextPowerOfTwo(KEYWORDS_UNIVERSE + 1); // 设置为稍大的2的幂次
    }

    // 辅助方法：计算下一个大于等于n的2的幂次值
    private static int nextPowerOfTwo(int n) {
        if (n <= 0) return 1;
        int power = 1;
        while (power < n) {
            power <<= 1;
        }
        return power;
    }

    public static void generateSpatialDataCSV() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH));
             BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH))) {

            String line = reader.readLine(); // 读取标题行
            if (line == null) {
                System.err.println("CSV文件为空或仅包含标题行。");
            } else {
                writer.write(line); // 写入标题行到新文件
                writer.newLine();

                while ((line = reader.readLine()) != null) {
                    String[] columns = line.split(",");
                    // 替换key1到key12（假设是columns[3]到columns[14]）
                    for (int i = 3; i < columns.length && i < 15; i++) {
                        String key = columns[i].trim();
                        if (!key.isEmpty() && keywordToIndexMap.containsKey(key)) {
                            columns[i] = String.valueOf(keywordToIndexMap.get(key)); // 将关键字替换为序号
                        }
                    }

                    // 将修改后的行写入新的CSV文件
                    writer.write(String.join(",", columns));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("去重后的关键字数量 (KEYWORDS_UNIVERSE): " + KEYWORDS_UNIVERSE);
        System.out.println("稍大的2的幂次值 (MAX_KEYWORDS): " + MAX_KEYWORDS);

        // 生成新的文件 spatialdata.csv
//        generateSpatialDataCSV();
//        System.out.println("文件生成完毕：spatialdata.csv");
    }
}
