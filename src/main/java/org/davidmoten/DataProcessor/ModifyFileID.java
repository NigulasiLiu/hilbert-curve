package org.davidmoten.DataProcessor;
import java.io.*;
import java.nio.file.*;
public class ModifyFileID {
    public static void main(String[] args) {
        String inputFilePath = "src/dataset/spatial_data_set.csv";
        String outputFilePath = "src/dataset/spatial_data_set_100W.csv";
        String output10WFilePath = "src/dataset/spatial_data_set_10W.csv";
//        processCSV(inputFilePath, outputFilePath);
        extractTopRows(outputFilePath, output10WFilePath, 100000);
//        int rowCount = 0;
//        try (
//                BufferedReader reader = Files.newBufferedReader(Paths.get(inputFilePath));
//                BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilePath))
//        ) {
//            String line;
//            boolean isFirstLine = true;
//
//            while ((line = reader.readLine()) != null) {
//                if (isFirstLine) {
//                    // Write header directly
//                    writer.write(line);
//                    writer.newLine();
//                    isFirstLine = false;
//                } else {
//                    // Split and modify the first column
//                    String[] fields = line.split(",");
//                    fields[0] = String.valueOf(++rowCount); // Replace the first column with rowCount
//                    writer.write(String.join(",", fields));
//                    writer.newLine();
//                }
//            }
//
//            System.out.println("File processed successfully!");
//            System.out.println("Total rows: " + rowCount);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
    /**
     * Process the CSV file to modify the first column and count total rows.
     */
    private static void processCSV(String inputFilePath, String outputFilePath) {
        int rowCount = 0;

        try (
                BufferedReader reader = Files.newBufferedReader(Paths.get(inputFilePath));
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilePath))
        ) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    // Write header directly
                    writer.write(line);
                    writer.newLine();
                    isFirstLine = false;
                } else {
                    // Split and modify the first column
                    String[] fields = line.split(",");
                    fields[0] = String.valueOf(++rowCount); // Replace the first column with rowCount
                    writer.write(String.join(",", fields));
                    writer.newLine();
                }
            }

            System.out.println("File processed successfully!");
            System.out.println("Total rows: " + rowCount);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Extract the first N rows (including the header) and write to a new file.
     */
    private static void extractTopRows(String inputFilePath, String outputFilePath, int rowLimit) {
        int rowCount = 0;

        try (
                BufferedReader reader = Files.newBufferedReader(Paths.get(inputFilePath));
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilePath))
        ) {
            String line;

            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
                rowCount++;

                // Stop after writing the specified number of rows
                if (rowCount >= rowLimit + 1) { // +1 to include the header
                    break;
                }
            }

            System.out.println("Extracted first " + rowLimit + " rows to " + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
