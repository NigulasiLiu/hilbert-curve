package org.davidmoten.DataProcessor;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SpatialKeywordCombiner {

    private static final String SPATIAL_CSV_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large_hilbert.csv";
    private static final String KEYWORDS_FILE_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\20news-bydate\\vocabulary.txt";
    private static final String OUTPUT_CSV_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large_final.csv";

    public static void main(String[] args) {
        try {
            List<String> keywords = loadKeywords(KEYWORDS_FILE_PATH);
            combineSpatialAndKeywords(SPATIAL_CSV_PATH, keywords, OUTPUT_CSV_PATH);
            System.out.println("组合完成，数据已保存到 " + OUTPUT_CSV_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 加载关键词列表
    private static List<String> loadKeywords(String filePath) throws IOException {
        List<String> keywords = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                keywords.add(line.trim());
            }
        }
        System.out.println("加载了 " + keywords.size() + " 个关键词。");
        return keywords;
    }

    // 将空间数据和关键词组合
    private static void combineSpatialAndKeywords(String spatialCsvPath, List<String> keywords, String outputCsvPath) throws IOException {
        Random random = new Random();

        int lineCount = countLines(spatialCsvPath); // 统计文件行数，用于计算进度
        int processedCount = 0; // 已处理行数计数器
        int progressInterval = 1000; // 每处理1000行打印一次进度

        try (BufferedReader reader = new BufferedReader(new FileReader(spatialCsvPath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputCsvPath))) {

            // 写入CSV文件头
            writer.write("id,x,y,key1,key2,key3,key4,key5,key6,key7,key8,key9,key10,key11,key12");
            writer.newLine();

            String line = reader.readLine();  // 读取并跳过表头
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                String osmId = parts[0];
                String x = parts[1];
                String y = parts[2];

                // 随机选择12个关键词
                List<String> selectedKeywords = getRandomKeywords(keywords, 12, random);

                // 写入输出文件
                writer.write(osmId + "," + x + "," + y + "," + String.join(",", selectedKeywords));
                writer.newLine();

                processedCount++;
                if (processedCount % progressInterval == 0 || processedCount == lineCount) {
                    double progress = (processedCount / (double) lineCount) * 100;
                    System.out.printf("已处理 %d / %d 行 (%.2f%% 完成)%n", processedCount, lineCount, progress);
                }
            }
        }
    }

    // 获取随机关键词
    private static List<String> getRandomKeywords(List<String> keywords, int numKeywords, Random random) {
        List<String> selectedKeywords = new ArrayList<>(numKeywords);
        Collections.shuffle(keywords, random);
        selectedKeywords.addAll(keywords.subList(0, numKeywords));
        return selectedKeywords;
    }

    // 统计文件行数
    private static int countLines(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            int lines = 0;
            while (reader.readLine() != null) {
                lines++;
            }
            return lines - 1; // 减去表头
        }
    }
}
