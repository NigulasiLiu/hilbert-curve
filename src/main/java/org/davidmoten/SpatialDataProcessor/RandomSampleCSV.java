package org.davidmoten.SpatialDataProcessor;
import java.io.*;
import java.util.*;

public class RandomSampleCSV {

    public static void main(String[] args) throws IOException {
        // 原始文件路径
        String inputFilePath = "src/dataset/spatial_data_set_10W.csv";

        // 输出文件路径
        String outputDirectory = "src/dataset/";
        String[] outputFileNames = {
                "spatial_data_set_2W.csv",
                "spatial_data_set_4W.csv",
                "spatial_data_set_6W.csv",
                "spatial_data_set_8W.csv"
        };

        // 每个文件需要的条目数
        int[] sampleSizes = {20000, 40000, 60000, 80000};

        // 调用方法处理文件
        generateSampleFiles(inputFilePath, outputDirectory, outputFileNames, sampleSizes);
    }

    private static void generateSampleFiles(String inputFilePath, String outputDirectory,
                                            String[] outputFileNames, int[] sampleSizes) throws IOException {
        // 读取源文件到内存
        List<String> allLines = new ArrayList<>();
        String header = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    header = line; // 保存表头
                    isFirstLine = false;
                } else {
                    allLines.add(line);
                }
            }
        }

        // 检查样本数量是否超过文件实际条目数
        if (allLines.isEmpty()) {
            throw new IllegalStateException("源文件没有有效数据！");
        }
        for (int size : sampleSizes) {
            if (size > allLines.size()) {
                throw new IllegalArgumentException("样本数量超过源文件数据量！");
            }
        }

        // 随机生成样本并保存到文件
        Random random = new Random();
        for (int i = 0; i < sampleSizes.length; i++) {
            int sampleSize = sampleSizes[i];
            String outputFilePath = outputDirectory + outputFileNames[i];

            // 随机抽取样本
            List<String> sample = getRandomSample(allLines, sampleSize, random);

            // 写入样本到新文件
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                writer.write(header); // 写入表头
                writer.newLine();
                for (String line : sample) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            System.out.printf("文件 %s 已生成，包含 %d 条数据。\n", outputFilePath, sampleSize);
        }
    }

    /**
     * 从数据列表中随机选取指定数量的条目
     */
    private static List<String> getRandomSample(List<String> data, int sampleSize, Random random) {
        // 使用 Collections.shuffle 方法随机打乱
        List<String> shuffledData = new ArrayList<>(data);
        Collections.shuffle(shuffledData, random);

        // 截取前 sampleSize 条
        return shuffledData.subList(0, sampleSize);
    }
}
