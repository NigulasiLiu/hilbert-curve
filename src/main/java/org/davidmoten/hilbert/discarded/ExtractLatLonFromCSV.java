package org.davidmoten.hilbert.discarded;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ExtractLatLonFromCSV {

    public static void main(String[] args) {
        String inputFilePath = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\satlite\\kivaData_augmented\\kivaData_augmented.csv";
        String outputFilePath = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\satlite\\kivaData_spatial.csv";

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            String headerLine = reader.readLine(); // 读取并跳过表头
            String[] headers = headerLine.split("\\t|,"); // 处理制表符或逗号分隔符

            // 找到lat和lon所在的列索引
            int latIndex = -1;
            int lonIndex = -1;
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase("latitude")) {
                    latIndex = i;
                }
                if (headers[i].trim().equalsIgnoreCase("longitude")) {
                    lonIndex = i;
                }
            }

            if (latIndex == -1 || lonIndex == -1) {
                System.out.println("无法找到latitude或longitude列。");
                return;
            }

            // 写入CSV头
            writer.write("latitude,longitude");
            writer.newLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split("\\t|,"); // 处理制表符或逗号分隔符

                // 检查是否存在lat和lon数据
                if (columns.length > latIndex && columns.length > lonIndex) {
                    String latitude = columns[latIndex].trim();
                    String longitude = columns[lonIndex].trim();

                    // 写入CSV文件
                    writer.write(latitude + "," + longitude);
                    writer.newLine();
                }
            }

            System.out.println("数据提取完成，已保存到：" + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
