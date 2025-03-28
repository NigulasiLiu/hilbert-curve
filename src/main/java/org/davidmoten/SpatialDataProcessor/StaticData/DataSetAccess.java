package org.davidmoten.SpatialDataProcessor.StaticData;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataSetAccess {

    // 实例变量，供不同方法和类访问
    public int[] fileDataSet;
    public String[][] keywordItemSets;
    public long[][] pointDataSet;
    private String[] keywordDataSet;
    public HilbertCurve hilbertCurve;
    public int order;
    public DataSetAccess(int order){
        this.order = order;
        this.hilbertCurve = HilbertCurve.bits(order).dimensions(2);
    }
    //共生成fileIDSetNum个fileID，每个fileID范围小于maxfiles
    public int[] generateFiles(int fileIDSetNum, int maxfiles) {
        if (fileIDSetNum <= 0 || maxfiles <= 1) {
            throw new IllegalArgumentException("n 必须大于0，l 必须大于1");
        }

        int[] result = new int[fileIDSetNum];
        Random random = new Random();

        for (int i = 0; i < fileIDSetNum; i++) {
            result[i] = random.nextInt(maxfiles - 1) + 1;
        }

        // 存储到实例变量中
        this.fileDataSet = result;
        return result;
    }

    public String[] generateKeywords(int W, int k) {
        if (W <= 0 || k <= 0 || k > W) {
            throw new IllegalArgumentException("W 和 k 必须大于0，且 k 必须小于等于 W");
        }

        double[] probabilities = new double[W];
        double sum = 0.0;
        double s = 1.0;

        for (int i = 1; i <= W; i++) {
            probabilities[i - 1] = 1.0 / Math.pow(i, s);
            sum += probabilities[i - 1];
        }

        for (int i = 0; i < W; i++) {
            probabilities[i] /= sum;
        }

        Random random = new Random();
        Set<String> keywordsSet = new HashSet<>();

        while (keywordsSet.size() < k) {
            double rand = random.nextDouble();
            double cumulativeProbability = 0.0;

            for (int i = 0; i < W; i++) {
                cumulativeProbability += probabilities[i];
                if (rand <= cumulativeProbability) {
                    keywordsSet.add("keyword" + (i + 1));
                    break;
                }
            }
        }

        String[] result = keywordsSet.toArray(new String[0]);
        // 存储到实例变量中
        this.keywordDataSet = result;
        return result;
    }

    public String[][] generateKwItems(int W, int k, int n) {
        if (W <= 0 || k <= 0 || k > W || n <= 0) {
            throw new IllegalArgumentException("W 和 k 必须大于0，且 k 必须小于等于 W, n 必须大于0");
        }

        double[] probabilities = new double[W];
        double sum = 0.0;
        double s = 1.0;

        for (int i = 1; i <= W; i++) {
            probabilities[i - 1] = 1.0 / Math.pow(i, s);
            sum += probabilities[i - 1];
        }

        for (int i = 0; i < W; i++) {
            probabilities[i] /= sum;
        }

        Random random = new Random();
        String[][] result = new String[n][k];

        for (int i = 0; i < n; i++) {
            List<String> selectedKeywords = new ArrayList<>();
            while (selectedKeywords.size() < k) {
                double rand = random.nextDouble();
                double cumulativeProbability = 0.0;

                for (int j = 0; j < W; j++) {
                    cumulativeProbability += probabilities[j];
                    if (rand <= cumulativeProbability) {
                        String keyword = "k" + (j + 1);
                        if (!selectedKeywords.contains(keyword)) {
                            selectedKeywords.add(keyword);
                        }
                        break;
                    }
                }
            }
            result[i] = selectedKeywords.toArray(new String[0]);
        }

        // 存储到实例变量中
        this.keywordItemSets = result;
        return result;
    }

    public List<long[]> generatePoints(int edgeLength, String distributionType, int count) {
        if (edgeLength <= 0) {
            throw new IllegalArgumentException("edgeLength 必须大于0");
        }

        Random random = new Random();
        List<long[]> points = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            long x, y;
            switch (distributionType.toLowerCase()) {
                case "gaussian":
                    double mean = edgeLength / 2.0;
                    double stdDev = edgeLength / 6.0;
                    x = (long) Math.max(0, Math.min(edgeLength - 1, random.nextGaussian() * stdDev + mean));
                    y = (long) Math.max(0, Math.min(edgeLength - 1, random.nextGaussian() * stdDev + mean));
                    break;

                case "uniform":
                    x = random.nextInt(edgeLength);
                    y = random.nextInt(edgeLength);
                    break;

                case "multi-gaussian":
                    double[][] centers = {
                            {edgeLength * 0.25, edgeLength * 0.25},
                            {edgeLength * 0.75, edgeLength * 0.75},
                            {edgeLength * 0.5, edgeLength * 0.5}
                    };
                    double[] weights = {0.4, 0.3, 0.3};
                    int centerIndex = random.nextDouble() < weights[0] ? 0 : (random.nextDouble() < weights[0] + weights[1] ? 1 : 2);
                    mean = centers[centerIndex][0];
                    stdDev = edgeLength / 12.0;
                    x = (long) Math.max(0, Math.min(edgeLength - 1, random.nextGaussian() * stdDev + mean));
                    mean = centers[centerIndex][1];
                    y = (long) Math.max(0, Math.min(edgeLength - 1, random.nextGaussian() * stdDev + mean));
                    break;

                default:
                    throw new IllegalArgumentException("未知的分布类型: " + distributionType);
            }
            points.add(new long[]{x, y});
        }

        // 转换 List 为数组并存储到实例变量中
        this.pointDataSet = points.toArray(new long[points.size()][]);
        return points;
    }

    public void saveToCSV(String fileName, HilbertCurve hilbertCurve) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            // 写入表头
            writer.write("FileID,PointX,PointY,HilbertIndex,Keyword1,Keyword2,Keyword3,Keyword4,Keyword5,Keyword6,Keyword7,Keyword8,Keyword9,Keyword10,Keyword11,Keyword12\n");

            // 遍历每个数据项
            for (int i = 0; i < fileDataSet.length; i++) {
                int fileData = fileDataSet[i];
                String[] keywordItems = keywordItemSets[i];
                long[] pointData = pointDataSet[i];

                // 计算 Hilbert 索引
                BigInteger hIndex = hilbertCurve.index(pointData[0], pointData[1]);

                // 补充不足的关键词
                StringBuilder keywords = new StringBuilder();
                for (int j = 0; j < 12; j++) {
                    if (j < keywordItems.length) {
                        keywords.append(keywordItems[j]);
                    }
                    if (j != 11) {
                        keywords.append(",");
                    }
                }

                // 写入 CSV 文件
                writer.write(fileData + "," +
                        pointData[0] + "," +
                        pointData[1] + "," +
                        hIndex + "," +
                        keywords + "\n");
            }
        }
        System.out.println("数据已保存到文件: " + fileName);
    }

    private List<String> preCode(BigInteger pointHilbertIndex) {

        // 将 Hilbert 索引转换为二进制字符串，并确保其长度为 2 * order 位
        String hilbertBinary = pointHilbertIndex.toString(2);
        int requiredLength = 2 * order;

        // 如果二进制字符串长度不足，前面补0
        hilbertBinary = String.format("%" + requiredLength + "s", hilbertBinary).replace(' ', '0');

        List<String> prefixList = new ArrayList<>();

        // 从完整的前缀开始，逐步减少长度
        for (int i = 0; i <= requiredLength; i++) {
            String prefix = hilbertBinary.substring(0, requiredLength - i);
            StringBuilder paddedPrefix = new StringBuilder(prefix);

            // 使用循环来替代 .repeat() 功能
            for (int j = 0; j < requiredLength - prefix.length(); j++) {
                paddedPrefix.append('*');
            }
            prefixList.add(paddedPrefix.toString());
        }

        // 确保返回的 prefixList 包含 2 * order 个串
        if (prefixList.size() < requiredLength + 1) {
            // 添加足够数量的前缀串，直到数量达到 2 * order
            for (int i = prefixList.size(); i <= requiredLength; i++) {
                StringBuilder prefix = new StringBuilder();

                // 构建前缀
                for (int j = 0; j < i; j++) {
                    prefix.append("");
                }
                // 构建后缀
                for (int j = 0; j < requiredLength - i; j++) {
                    prefix.append('*');
                }
                prefixList.add(prefix.toString());
            }
        }

        return prefixList;
    }
    public void preProcessIndex(String inputFileName, String kdbFileName, String pdbFileName) {
        // 使用高效的 ConcurrentHashMap 存储前缀和关键字的映射
        Map<String, List<String>> prefixMap = new ConcurrentHashMap<>();
        Map<String, List<String>> keywordMap = new ConcurrentHashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName))) {
            String line;
            boolean isFirstLine = true; // 标志是否是第一行

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // 跳过第一行
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 16) {
                    System.out.println("数据行格式错误: " + line);
                    continue;
                }

                // 获取 fileData 和 hIndex
                String fileData = parts[0];
                BigInteger hIndex = new BigInteger(parts[3]);

                // 获取前缀编码
                List<String> prefixes = preCode(hIndex);

                // 更新 prefixMap
                for (String prefix : prefixes) {
                    prefixMap.computeIfAbsent(prefix, k -> Collections.synchronizedList(new ArrayList<>())).add(fileData);
                }

                // 获取关键字（从第5列到第16列，共12个关键字）
                for (int i = 4; i < 16; i++) {
                    String keyword = parts[i];
                    keywordMap.computeIfAbsent(keyword, k -> Collections.synchronizedList(new ArrayList<>())).add(fileData);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 将 prefixMap 写入 PDB_pre.csv
        try (BufferedWriter pdbWriter = new BufferedWriter(new FileWriter(pdbFileName))) {
            pdbWriter.write("Key,Values\n");
            for (Map.Entry<String, List<String>> entry : prefixMap.entrySet()) {
                String key = entry.getKey();
                String values = String.join(";", entry.getValue());
                pdbWriter.write(key + ",\"" + values + "\"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 将 keywordMap 写入 KDB_pre.csv
        try (BufferedWriter kdbWriter = new BufferedWriter(new FileWriter(kdbFileName))) {
            kdbWriter.write("Key,Values\n");
            for (Map.Entry<String, List<String>> entry : keywordMap.entrySet()) {
                String key = entry.getKey();
                String values = String.join(";", entry.getValue());
                kdbWriter.write(key + ",\"" + values + "\"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Preprocessing complete. Data saved to:");
        System.out.println("PDB: " + pdbFileName);
        System.out.println("KDB: " + kdbFileName);
    }

    public void convertToBitMap(String inputFileName, String outputFileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFileName));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {

            // 写入CSV文件的标题行
            writer.write("Key,Bitmap\n");

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // 跳过第一行标题
                    continue;
                }

                String[] parts = line.split(",", 2); // 只分割成两部分
                if (parts.length < 2) {
                    System.out.println("数据行格式错误: " + line);
                    continue;
                }

                String key = parts[0];
                String values = parts[1].replace("\"", ""); // 去除可能的引号
                String[] valueArray = values.split(";"); // 按分号分隔

                // 初始化一个BigInteger表示位图
                BigInteger bitmap = BigInteger.ZERO;

                for (String value : valueArray) {
                    try {
                        int index = Integer.parseInt(value.trim());
                        bitmap = bitmap.setBit(index); // 将对应位设置为1
                    } catch (NumberFormatException e) {
                        System.out.println("值解析错误: " + value);
                    }
                }

                // 写入输出文件：Key, Bitmap
                writer.write(key + "," + bitmap + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Conversion complete. Data saved to " + outputFileName);
    }
    public static void main(String[] args) throws IOException {
        int hilbertOrder = 12;
        DataSetAccess dataSetAccess = new DataSetAccess(hilbertOrder);
        int maxFiles = 1 << 20;
        int filesNeed = 1;
        int edgeLength = 1 << hilbertOrder;
        int Wnum = 8000;
        int attachedKeywords = 12;
        int items = 10*10000;
        String distributionType = "multi-gaussian";
        // 创建 Hilbert Curve 实例
        HilbertCurve hilbertCurve = HilbertCurve.bits(hilbertOrder).dimensions(2);

//        // 生成数据
//        dataSetAccess.generateFiles(items, maxFiles);
//        dataSetAccess.generateKwItems(Wnum, attachedKeywords, items);
//        dataSetAccess.generatePoints(edgeLength, distributionType, items);
//        // 保存数据到 CSV 文件
//        dataSetAccess.saveToCSV("src/dataset/spatial_data_for_update.csv", hilbertCurve);
//
//        // 验证数据已存储
//        System.out.println("文件数据集大小: " + dataSetAccess.fileDataSet.length);
//        System.out.println("关键字项集数量: " + dataSetAccess.keywordItemSets.length);
//        System.out.println("点数据集大小: " + dataSetAccess.pointDataSet.length);

//        String inputFileName = "src/dataset/spatial_data_for_update.csv";
//        String outputFileName = "src/dataset/preprocessed_data.csv";
        String kdbFileName = "src/dataset/KDB_pre.csv";
        String pdbFileName = "src/dataset/PDB_pre.csv";
//
//        dataSetAccess.preProcessIndex(inputFileName, kdbFileName,pdbFileName);
        // 转换 KDB_pre.csv 和 PDB_pre.csv
        dataSetAccess.convertToBitMap(kdbFileName, "src/dataset/KDB.csv");
        dataSetAccess.convertToBitMap(pdbFileName, "src/dataset/PDB.csv");
    }
}
