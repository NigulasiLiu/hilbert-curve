package org.davidmoten.hilbert.discarded;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class EnhancedBoundingBoxCalculator {

    // 数据集路径
    private static final String FILE_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\Combined\\2^n_DataSet.csv";
    private static final String OUTPUT_FILE_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\Combined\\normalization_2^n_DataSet.csv";

    public static void main(String[] args) {
        try {
            double minLongitude = Double.POSITIVE_INFINITY;
            double maxLongitude = Double.NEGATIVE_INFINITY;
            double minLatitude = Double.POSITIVE_INFINITY;
            double maxLatitude = Double.NEGATIVE_INFINITY;

            List<String[]> coordinatesWithAttributes = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH));
            String line;

            // 跳过标题行
            String header = reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");

                if (columns.length >= 3) {
                    try {
                        double longitude = Double.parseDouble(columns[1].trim());
                        double latitude = Double.parseDouble(columns[2].trim());

                        // 更新最小/最大经纬度
                        if (longitude < minLongitude) {
                            minLongitude = longitude;
                        }
                        if (longitude > maxLongitude) {
                            maxLongitude = longitude;
                        }
                        if (latitude < minLatitude) {
                            minLatitude = latitude;
                        }
                        if (latitude > maxLatitude) {
                            maxLatitude = latitude;
                        }

                        // 添加到坐标和属性列表中
                        coordinatesWithAttributes.add(columns);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing longitude/latitude: " + e.getMessage());
                    }
                }
            }

            reader.close();

            // 输出最小长方形区域的四个顶点
            System.out.println("Bounding Box:");
            System.out.println("Top-left (minLongitude, maxLatitude): (" + minLongitude + ", " + maxLatitude + ")");
            System.out.println("Top-right (maxLongitude, maxLatitude): (" + maxLongitude + ", " + maxLatitude + ")");
            System.out.println("Bottom-left (minLongitude, minLatitude): (" + minLongitude + ", " + minLatitude + ")");
            System.out.println("Bottom-right (maxLongitude, minLatitude): (" + maxLongitude + ", " + minLatitude + ")");

            double width = maxLongitude - minLongitude;
            double height = maxLatitude - minLatitude;

            double sideLength = Math.max(width, height);
            System.out.println("\nSquare Bounding Box:");
            System.out.println("Top-left: (0, 0)");
            System.out.println("Top-right: (" + sideLength + ", 0)");
            System.out.println("Bottom-left: (0, " + sideLength + ")");
            System.out.println("Bottom-right: (" + sideLength + ", " + sideLength + ")");

            // 选择处理方式
            Scanner scanner = new Scanner(System.in);
            System.out.println("请选择Longitude和Latitude的处理方式:");
            System.out.println("1. 同时乘以10^k");
            System.out.println("2. 去除小数点");

            int choice = scanner.nextInt();
            long multiplier = 1;

            if (choice == 1) {
                System.out.println("请输入k值:");
                int k = scanner.nextInt();
                multiplier = (long) Math.pow(10, k);
            }

            List<String[]> finalRegionCoordinates = new ArrayList<>();
            calculateAndNormalizeRegion(coordinatesWithAttributes, minLongitude, maxLatitude, sideLength, 90, finalRegionCoordinates, multiplier, choice == 2);

            // 导出新的数据文件
            exportToCSV(finalRegionCoordinates, header);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void calculateAndNormalizeRegion(List<String[]> coordinatesWithAttributes, double minLongitude, double maxLatitude, double sideLength, int threshold, List<String[]> finalRegionCoordinates, long multiplier, boolean removeDecimalPoint) {
        int[] counts = new int[4];
        double midX = minLongitude + sideLength / 2;
        double midY = maxLatitude - sideLength / 2;

        List<String[]> topLeft = new ArrayList<>();
        List<String[]> topRight = new ArrayList<>();
        List<String[]> bottomLeft = new ArrayList<>();
        List<String[]> bottomRight = new ArrayList<>();

        for (String[] columns : coordinatesWithAttributes) {
            double x = Double.parseDouble(columns[1]);
            double y = Double.parseDouble(columns[2]);

            if (x < midX && y < midY) {
                topLeft.add(columns);
                counts[0]++;  // Top-left
            } else if (x >= midX && y < midY) {
                topRight.add(columns);
                counts[1]++;  // Top-right
            } else if (x < midX && y >= midY) {
                bottomLeft.add(columns);
                counts[2]++;  // Bottom-left
            } else if (x >= midX && y >= midY) {
                bottomRight.add(columns);
                counts[3]++;  // Bottom-right
            }
        }

        int totalPoints = coordinatesWithAttributes.size();
        System.out.printf("Top-left: %.2f%%\n", 100.0 * counts[0] / totalPoints);
        System.out.printf("Top-right: %.2f%%\n", 100.0 * counts[1] / totalPoints);
        System.out.printf("Bottom-left: %.2f%%\n", 100.0 * counts[2] / totalPoints);
        System.out.printf("Bottom-right: %.2f%%\n", 100.0 * counts[3] / totalPoints);

        if (100.0 * counts[0] / totalPoints > threshold) {
            calculateAndNormalizeRegion(topLeft, minLongitude, maxLatitude, sideLength / 2, threshold, finalRegionCoordinates, multiplier, removeDecimalPoint);
        } else if (100.0 * counts[1] / totalPoints > threshold) {
            calculateAndNormalizeRegion(topRight, midX, maxLatitude, sideLength / 2, threshold, finalRegionCoordinates, multiplier, removeDecimalPoint);
        } else if (100.0 * counts[2] / totalPoints > threshold) {
            calculateAndNormalizeRegion(bottomLeft, minLongitude, midY, sideLength / 2, threshold, finalRegionCoordinates, multiplier, removeDecimalPoint);
        } else if (100.0 * counts[3] / totalPoints > threshold) {
            calculateAndNormalizeRegion(bottomRight, midX, midY, sideLength / 2, threshold, finalRegionCoordinates, multiplier, removeDecimalPoint);
        } else {
            normalizeCoordinates(finalRegionCoordinates, coordinatesWithAttributes, minLongitude, maxLatitude, multiplier, removeDecimalPoint);
        }
    }

    private static void normalizeCoordinates(List<String[]> finalRegionCoordinates, List<String[]> coordinates, double originLongitude, double originLatitude, long multiplier, boolean removeDecimalPoint) {
        int id = 0;  // 新的自增ID

        for (String[] columns : coordinates) {
            double longitude = Double.parseDouble(columns[1]);
            double latitude = Double.parseDouble(columns[2]);

            double normalizedLongitude = longitude - originLongitude;
            double normalizedLatitude = originLatitude - latitude;

            if (removeDecimalPoint) {
                normalizedLongitude = Double.parseDouble(Long.toString((long) normalizedLongitude));
                normalizedLatitude = Double.parseDouble(Long.toString((long) normalizedLatitude));
            } else {
                normalizedLongitude *= multiplier;
                normalizedLatitude *= multiplier;
            }

            String[] newColumns = new String[columns.length];
            newColumns[0] = String.valueOf(id++);  // 新ID
            newColumns[1] = String.valueOf((long) normalizedLongitude);
            newColumns[2] = String.valueOf((long) normalizedLatitude);
            System.arraycopy(columns, 3, newColumns, 3, columns.length - 3);

            finalRegionCoordinates.add(newColumns);
        }
    }

    private static void exportToCSV(List<String[]> finalRegionCoordinates, String header) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH))) {
            writer.write(header);
            writer.newLine();

            for (String[] columns : finalRegionCoordinates) {
                writer.write(String.join(",", columns));
                writer.newLine();
            }

            System.out.println("Normalization complete. Data exported to: " + OUTPUT_FILE_PATH);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
