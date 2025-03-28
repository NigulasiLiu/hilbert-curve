package org.davidmoten.SpatialDataProcessor.BirminghanData;

import java.io.*;

public class HilbertCoordinateProcessor {

    public static void main(String[] args) {
        String inputFilePath = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large_projection.csv";
        String outputFilePath = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large_hilbert.csv";

        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            // 初始化最大和最小值
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double maxY = Double.MIN_VALUE;

            // 第一次读取文件，找到最小和最大的x,y值
            reader = new BufferedReader(new FileReader(inputFilePath));
            String line;
            reader.readLine(); // 跳过表头
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                double x = Double.parseDouble(columns[1]);
                double y = Double.parseDouble(columns[2]);

                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }

            double sideLength = Math.max(maxX - minX, maxY - minY);

            // 计算区域最左上角的原点
            double originX = minX;
            double originY = maxY; // 左上角的y

            // 关闭reader并重新打开以从头读取文件
            reader.close();
            reader = new BufferedReader(new FileReader(inputFilePath));

            // 打开writer进行输出
            writer = new BufferedWriter(new FileWriter(outputFilePath));
            writer.write("id,x,y");
            writer.newLine();

            int n = 19; // 希尔伯特曲线阶数
            int maxCoord = (int) Math.pow(2, n) - 1;

            reader.readLine(); // 跳过表头
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                int id = Integer.parseInt(columns[0]);
                double x = Double.parseDouble(columns[1]);
                double y = Double.parseDouble(columns[2]);

                // 平移并缩放坐标
                int scaledX = (int) (((x - originX) / sideLength) * maxCoord);
                int scaledY = (int) (((originY - y) / sideLength) * maxCoord);

                writer.write(id + "," + scaledX + "," + scaledY);
                writer.newLine();
            }

            System.out.println("数据处理完成，已保存到：" + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 确保资源在程序结束时被关闭
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
