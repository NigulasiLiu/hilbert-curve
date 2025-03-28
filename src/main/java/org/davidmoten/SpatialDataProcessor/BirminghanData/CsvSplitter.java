package org.davidmoten.SpatialDataProcessor.BirminghanData;

import java.io.*;

public class CsvSplitter {

    private static final String INPUT_CSV_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large_final.csv";
    private static final String OUTPUT_CSV_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large_final_1.csv";

    public static void main(String[] args) {

        int k1 = 0;
        int k2 = 20;

        // 计算2^k1和2^k2的值
        long startLine = (long) Math.pow(2, k1);
        long endLine = (long) Math.pow(2, k2);

        try {
            long totalLines = countLines(INPUT_CSV_PATH);

            if (totalLines <= startLine) {
                System.out.println("文件行数小于2^k1，跳过处理。");
                return;
            }

            // 如果文件行数小于2^k2，则将endLine设为最后一行
            if (totalLines < endLine) {
                endLine = totalLines;
            }

            splitCsvFile(startLine, endLine);
            System.out.println("CSV文件已拆分并保存到 " + OUTPUT_CSV_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void splitCsvFile(long startLine, long endLine) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(INPUT_CSV_PATH));
             BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_CSV_PATH))) {

            String line = reader.readLine();  // 读取并写入表头
            writer.write(line);
            writer.newLine();

            long currentLine = 1;
            while ((line = reader.readLine()) != null) {
                if (currentLine >= startLine && currentLine <= endLine) {
                    writer.write(line);
                    writer.newLine();
                }
                currentLine++;

                // 如果当前行已经超过endLine，停止读取文件
                if (currentLine > endLine) {
                    break;
                }
            }
        }
    }

    private static long countLines(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            long lines = 0;
            while (reader.readLine() != null) {
                lines++;
            }
            return lines - 1; // 减去表头
        }
    }
}
