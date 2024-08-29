package org.davidmoten.hilbert.DataProcessor;

import java.io.*;

public class MercatorProjection {

    //    2. 调整坐标使其适合希尔伯特曲线编码
//为了使希尔伯特曲线编码的区域尽可能紧凑，我们需要将所有坐标缩放到一个合适的整数范围内，并确保它们都在一个正方形的二维空间内（因为希尔伯特曲线是定义在正方形区域上的）。
//
//a. 计算区域的宽度和高度
//计算平移后的坐标中，x 和 y 的最大值，这将决定区域的宽度和高度。
//
//width = max(x')
//height = max(y')
//b. 确保区域是正方形
//如果宽度和高度不相等，则需要将它们调整为相同的大小。通常，选择较大的那个作为正方形的边长。
//
//side_length = max(width, height)
//c. 缩放坐标到整数范围
//为了适应希尔伯特曲线编码，可以将坐标缩放到一个范围内，例如 [0, 2^n - 1] 的整数值范围内。这里 n 是希尔伯特曲线的阶数。
//
//x'' = (x' / side_length) * (2^n - 1)
//y'' = (y' / side_length) * (2^n - 1)
//其中，2^n 的值应足够大，以确保所有坐标点在缩放后仍然保持分辨率。
// 墨卡托投影常数
    private static final double R_MAJOR = 6378137.0;

    public static void main(String[] args) {
        String inputFilePath = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large.csv";
        String outputFilePath = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large_projection.csv";

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            // 读取并跳过表头
            String headerLine = reader.readLine();
            writer.write("id,x,y");
            writer.newLine();

            String line;
            int newId = 0; // 新的ID从0开始
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                long osmId = Long.parseLong(columns[0]);
                double latitude = Double.parseDouble(columns[1]);
                double longitude = Double.parseDouble(columns[2]);

                // 1. 使用墨卡托投影处理经纬度
                double x = mercatorX(longitude);
                double y = mercatorY(latitude);

                // 2. 写入新的ID和转换后的坐标
                writer.write(newId + "," + x + "," + y);
                writer.newLine();

                newId++; // 递增新的ID
            }

            System.out.println("数据处理完成，已保存到：" + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 墨卡托投影X坐标转换
    private static double mercatorX(double lon) {
        return R_MAJOR * Math.toRadians(lon);
    }

    // 墨卡托投影Y坐标转换
    private static double mercatorY(double lat) {
        if (lat > 89.5) lat = 89.5;
        if (lat < -89.5) lat = -89.5;
        double temp = Math.toRadians(lat);
        return R_MAJOR * Math.log(Math.tan(Math.PI / 4.0 + temp / 2.0));
    }
}
