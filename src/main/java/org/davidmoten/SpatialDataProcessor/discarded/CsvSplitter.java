package org.davidmoten.SpatialDataProcessor.discarded;

import java.io.*;

public class CsvSplitter {

    public static void main(String[] args) {
        // 固定输入文件地址
        String inputFile = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large_final.csv";
        String outputFile = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large_final_1.csv";  // 可以根据需要修改输出文件的路径
        int n = 20;  // 这里固定n为3，你可以根据需要调整

        int limit = (int) Math.pow(2, n);  // 计算2^n

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            int count = 0;
            // 跳过标题行
            br.readLine();
            bw.write("id,latitude,longitude\n");  // 写入CSV文件头

            while ((line = br.readLine()) != null && count < limit) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    long id = count;  // 使用递增的ID
                    double latitude = Double.parseDouble(parts[1]) * 1000000;
                    double longitude = Double.parseDouble(parts[2]) * 1000000;

                    bw.write(id + "," + String.format("%.7f", latitude) + "," + String.format("%.7f", longitude) + "\n");
                    count++;
                }
            }

            System.out.println("Generated " + count + " entries in " + outputFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
