package org.davidmoten.DataProcessor;

import java.util.*;

public class DataSetAccess {

    // 实例变量，供不同方法和类访问
    public int[] fileDataSet;
    private String[] keywordDataSet;
    public String[][] keywordItemSets;
    public long[][] pointDataSet;

    public int[] generateFiles(int n, int l) {
        if (n <= 0 || l <= 1) {
            throw new IllegalArgumentException("n 必须大于0，l 必须大于1");
        }

        int[] result = new int[n];
        Random random = new Random();

        for (int i = 0; i < n; i++) {
            result[i] = random.nextInt(l - 1) + 1;
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
                        String keyword = "keyword" + (j + 1);
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

    public int generateRandomIntWithProbability(double alpha, int l) {
        if (alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha 必须在 0 和 1 之间");
        }

        Random random = new Random();
        return random.nextDouble() < alpha ? 0 : 1;
    }

    public static void main(String[] args) {
        DataSetAccess dataSetAccess = new DataSetAccess();
        int maxFiles = 1 << 20;
        int filesNeed = 10;
        int edgeLength = 1 << 17;
        int Wnum = 8000;
        int attachedKeywords = 12;
        int items = 20000;
        String distributionType = "multi-gaussian";

        // 调用方法并存储数据
        dataSetAccess.generateFiles(filesNeed, maxFiles);
        dataSetAccess.generateKwItems(Wnum, attachedKeywords, items);
        dataSetAccess.generatePoints(edgeLength, distributionType, items);

        // 验证数据已存储
        System.out.println("文件数据集大小: " + dataSetAccess.fileDataSet.length);
        System.out.println("关键字项集数量: " + dataSetAccess.keywordItemSets.length);
        System.out.println("点数据集大小: " + dataSetAccess.pointDataSet.length);
    }
}
