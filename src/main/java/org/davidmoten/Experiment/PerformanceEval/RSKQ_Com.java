package org.davidmoten.Experiment.PerformanceEval;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class RSKQ_Com {
    public static void main(String[] args) {
        // maxfiles 数组
        int[] maxFiles = {29, 33, 37};

        // 更新阶段位长数组
        long[] bitLengthsUpdate = {
                256 + (2 * (1 << 14)), // 256 + 2*(1<<14)
                256 + (2 * (1 << 15)), // 256 + 2*(1<<15)
                256 + (2 * (1 << 16)), // 256 + 2*(1<<16)
                256 + (2 * (1 << 17)), // 256 + 2*(1<<17)
                256 + (2 * (1 << 18)), // 256 + 2*(1<<18)
                256 + (2 * (1 << 19)), // 256 + 2*(1<<19)
                256 + (2 * (1 << 20))  // 256 + 2*(1<<20)
        };

        // 搜索阶段第一阶段位长数组
        long[] bitLengthsSearchPhase1 = {
                256 + (3 * (1 << 14)), // 256 + 3*(1<<14)
                256 + (3 * (1 << 15)), // 256 + 3*(1<<15)
                256 + (3 * (1 << 16)), // 256 + 3*(1<<16)
                256 + (3 * (1 << 17)), // 256 + 3*(1<<17)
                256 + (3 * (1 << 18)), // 256 + 3*(1<<18)
                256 + (3 * (1 << 19)), // 256 + 3*(1<<19)
                256 + (3 * (1 << 20))  // 256 + 3*(1<<20)
        };

        // 搜索阶段第二阶段位长数组
        long[] bitLengthsSearchPhase2 = {
                1 << 14, // 1<<14
                1 << 15, // 1<<15
                1 << 16, // 1<<16
                1 << 17, // 1<<17
                1 << 18, // 1<<18
                1 << 19, // 1<<19
                1 << 20  // 1<<20
        };

        // CSV 文件路径
        String filePath = "src/dataset/rskq_communication_overhead1.csv";

        // 写入文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // 写入简化后的表头
            writer.write("MaxFiles,Up_256+2*(1<<14),Up_256+2*(1<<15),Up_256+2*(1<<16),Up_256+2*(1<<17),Up_256+2*(1<<18),Up_256+2*(1<<19),Up_256+2*(1<<20),Sp1_256+3*(1<<14),Sp1_256+3*(1<<15),Sp1_256+3*(1<<16),Sp1_256+3*(1<<17),Sp1_256+3*(1<<18),Sp1_256+3*(1<<19),Sp1_256+3*(1<<20),Sp2_1<<14,Sp2_1<<15,Sp2_1<<16,Sp2_1<<17,Sp2_1<<18,Sp2_1<<19,Sp2_1<<20\n");

            // 计算并写入数据
            for (int maxFile : maxFiles) {
                writer.write(maxFile + ",");
                for (long bitLength : bitLengthsUpdate) {
                    double communicationOverhead = calculateCommunicationOverhead(maxFile, bitLength);
                    writer.write(String.format("%.5f", communicationOverhead) + ",");
                }
                for (long bitLength : bitLengthsSearchPhase1) {
                    double communicationOverhead = calculateCommunicationOverhead(maxFile, bitLength);
                    writer.write(String.format("%.5f", communicationOverhead) + ",");
                }
                for (long bitLength : bitLengthsSearchPhase2) {
                    double communicationOverhead = calculateCommunicationOverhead(maxFile, bitLength);
                    writer.write(String.format("%.5f", communicationOverhead) + ",");
                }
                writer.newLine(); // 换行
            }
            System.out.println("CSV 文件已生成并保存到 " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 计算通信开销的方法
    private static double calculateCommunicationOverhead(int maxFile, long bitLength) {
        // 将位长从bit转换为字节并转换为KB
        return (maxFile * bitLength) / (8.0 * 1024.0);
    }
}
