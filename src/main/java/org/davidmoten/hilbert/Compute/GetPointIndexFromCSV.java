package org.davidmoten.hilbert.Compute;

import org.davidmoten.hilbert.HilbertComponent.HilbertCurve;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;

public class GetPointIndexFromCSV {

    public static void main(String[] args) {
        String inputFilePath = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\spillt_csv_10k\\spatial_2_part_1.csv";
        String outputFilePath = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\spillt_csv_10k\\spatial_2_part_1_results.txt";
        int order = 15; // 希尔伯特曲线的阶数
        int dimension = 2; // 2D 空间

        // 创建Hilbert曲线对象
        HilbertCurve c = HilbertCurve.bits(order).dimensions(dimension);

        // 读取CSV文件并处理
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
             FileWriter writer = new FileWriter(outputFilePath)) {

            String line;
            // 跳过第一行标题
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");

                // 获取longitude和latitude并转换为long值
                BigInteger longitude = new BigInteger(values[1].split("\\.")[0]);
                BigInteger latitude = new BigInteger(values[2].split("\\.")[0]);

                // 将点坐标转换为希尔伯特值
                long[] point = {longitude.longValue(), latitude.longValue()};
                BigInteger pointHilbertIndex = c.index(point);

                // 输出到控制台
                System.out.println("点: [" + longitude + "," + latitude + "] -> 希尔伯特值: " + pointHilbertIndex);

                // 保存到文件
                writer.write("点: [" + longitude + "," + latitude + "] -> 希尔伯特值: " + pointHilbertIndex + "\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

