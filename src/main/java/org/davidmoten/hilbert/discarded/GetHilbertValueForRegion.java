package org.davidmoten.hilbert.discarded;

import org.davidmoten.hilbert.HilbertComponent.HilbertCurve;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class GetHilbertValueForRegion {

    public static void main(String[] args) {
        int order = 20; // 希尔伯特曲线的阶数,本实验选择19或者20都可以
        int dimension = 2; // 2D 空间

        // 坐标系转换的范围
        long minLongitude = -74150000;
        long maxLongitude = -73750000;
        long minLatitude = 40680000;
        long maxLatitude = 40950000;

        // 创建Hilbert曲线对象
        HilbertCurve c = HilbertCurve.bits(order).dimensions(dimension);

        // 处理16个文件
        for (int regionNumber = 0; regionNumber < 16; regionNumber++) {
            String inputFilePath = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\16Region\\Multiply_1000000\\region_" + regionNumber + ".csv";
            String outputFilePath = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\16Region\\Multiply_1000000\\region_" + regionNumber + "_Hilbert_Value.csv";

            processFile(inputFilePath, outputFilePath, c, minLongitude, minLatitude);
        }
    }

    private static void processFile(String inputFilePath, String outputFilePath, HilbertCurve c, long minLongitude, long minLatitude) {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
             FileWriter writer = new FileWriter(outputFilePath)) {

            String line;
            // 跳过第一行标题
            br.readLine();

            // 写入输出文件的标题行
            writer.write("osm_id,longitude,latitude,HilbertValue\n");

            while ((line = br.readLine()) != null) {
                // 分割行内容
                String[] values = line.split(",");

                // 检查是否有足够的列数
                if (values.length < 3) {
                    System.err.println("Invalid line: " + line);
                    continue;
                }

                try {
                    // 获取osm_id、longitude和latitude并转换为long值
                    String osm_id = values[0];
                    long longitude = new BigDecimal(values[1]).multiply(new BigDecimal("100000")).longValue();
                    long latitude = new BigDecimal(values[2]).multiply(new BigDecimal("100000")).longValue();

                    // 将longitude和latitude转换为相对坐标
                    long x = longitude - minLongitude;
                    long y = latitude - minLatitude;

                    // 将点坐标转换为希尔伯特值
                    long[] point = {x, y};
                    BigInteger pointHilbertIndex = c.index(point);

                    // 输出到控制台
                    System.out.println("osm_id: " + osm_id + ", 点: [" + longitude + "," + latitude + "] -> 希尔伯特值: " + pointHilbertIndex);

                    // 保存到文件
                    writer.write(osm_id + "," + longitude + "," + latitude + "," + pointHilbertIndex + "\n");
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing numbers in line: " + line);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
