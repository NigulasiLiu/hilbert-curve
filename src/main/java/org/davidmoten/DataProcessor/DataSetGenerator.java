package org.davidmoten.DataProcessor;

import java.util.*;

public class DataSetGenerator {

    public static int[] generateFiles(int n, int l) {
        // 检查输入合法性
        if (n <= 0 || l <= 1) {
            throw new IllegalArgumentException("n 必须大于0，l 必须大于1");
        }

        int[] result = new int[n];
        Random random = new Random();

        for (int i = 0; i < n; i++) {
            // 生成范围在 1 到 l-1 之间的随机数
            result[i] = random.nextInt(l - 1) + 1;
        }

        return result;
    }

    public static String[] generateKeywords(int W, int k) {
        // 检查输入合法性
        if (W <= 0 || k <= 0 || k > W) {
            throw new IllegalArgumentException("W 和 k 必须大于0，且 k 必须小于等于 W");
        }

        // 创建 Zipf 分布的概率表
        double[] probabilities = new double[W];
        double sum = 0.0;
        double s = 1.0; // 参数 s 决定 Zipf 分布的形状

        for (int i = 1; i <= W; i++) {
            probabilities[i - 1] = 1.0 / Math.pow(i, s);
            sum += probabilities[i - 1];
        }

        // 归一化，使得概率和为 1
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

        return keywordsSet.toArray(new String[0]);
    }

    public static String[][] generateKwItems(int W, int k, int n) {
        // 检查输入合法性
        if (W <= 0 || k <= 0 || k > W || n <= 0) {
            throw new IllegalArgumentException("W 和 k 必须大于0，且 k 必须小于等于 W, n 必须大于0");
        }

        // 创建 Zipf 分布的概率表
        double[] probabilities = new double[W];
        double sum = 0.0;
        double s = 1.0; // 参数 s 决定 Zipf 分布的形状

        for (int i = 1; i <= W; i++) {
            probabilities[i - 1] = 1.0 / Math.pow(i, s);
            sum += probabilities[i - 1];
        }

        // 归一化，使得概率和为 1
        for (int i = 0; i < W; i++) {
            probabilities[i] /= sum;
        }

        Random random = new Random();
        String[][] result = new String[n][k];

        // 生成 n 次随机抽取，每次抽取 k 个关键字
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

        return result;
    }
    public static long[] generatePoints(int xrange, String distributionType) {
        // 检查输入合法性
        if (xrange <= 0) {
            throw new IllegalArgumentException("xrange 必须大于0");
        }

        Random random = new Random();
        long x, y;

        switch (distributionType.toLowerCase()) {
            case "gaussian":
                // 使用高斯分布生成点
                double mean = xrange / 2.0;
                double stdDev = xrange / 6.0; // 标准差控制范围
                x = (long) Math.max(0, Math.min(xrange - 1, random.nextGaussian() * stdDev + mean));
                y = (long) Math.max(0, Math.min(xrange - 1, random.nextGaussian() * stdDev + mean));
                break;

            case "uniform":
                // 使用均匀分布生成点
                x = random.nextInt(xrange);
                y = random.nextInt(xrange);
                break;

            case "multi-gaussian":
                // 使用多高斯分布生成点，模拟多个聚集区域
                double[][] centers = {
                        {xrange * 0.25, xrange * 0.25},
                        {xrange * 0.75, xrange * 0.75},
                        {xrange * 0.5, xrange * 0.5}
                };
                double[] weights = {0.4, 0.3, 0.3}; // 权重用于选择不同的中心
                int centerIndex = random.nextDouble() < weights[0] ? 0 : (random.nextDouble() < weights[0] + weights[1] ? 1 : 2);
                mean = centers[centerIndex][0];
                stdDev = xrange / 12.0; // 分布标准差稍小
                x = (long) Math.max(0, Math.min(xrange - 1, random.nextGaussian() * stdDev + mean));
                mean = centers[centerIndex][1];
                y = (long) Math.max(0, Math.min(xrange - 1, random.nextGaussian() * stdDev + mean));
                break;

            default:
                throw new IllegalArgumentException("未知的分布类型: " + distributionType);
        }

        return new long[]{x, y};
    }
    public static int generateRandomIntWithProbability(double alpha, int l) {
        if (alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha 必须在 0 和 1 之间");
        }

        Random random = new Random();
        // 以 alpha 概率生成 0，否则生成 1
        return random.nextDouble() < alpha ? 0 : 1;
    }
    public static void main(String[] args) {
        // 示例使用
        int n = 10; // 文件个数
        int l = 1 << 20; // 文件最大数量

        int[] dataSet = generateFiles(n, l);

        // 打印生成的数组
        for (int num : dataSet) {
            System.out.print(num + " ");
        }
        System.out.println();

        // 生成关键字数组示例
        int W = 8000; // 关键字总数
        int k = 12; // 所需关键字数量

        String[] keywords = generateKeywords(W, k);

        // 打印生成的关键字数组
        for (String keyword : keywords) {
            System.out.print(keyword + " ");
        }
        int items = 4; // 抽取次数

        String[][] keywordSets = generateKwItems(W, k, items);

        // 打印每次抽取的关键字集
        for (String[] kws : keywordSets) {
            for (String keyword : kws) {
                System.out.print(keyword + " ");
            }
            System.out.println();
        }

        int xrange = 1 << 17; // 用于 Hilbert 曲线范围的2次幂
        String distributionType = "multi-gaussian"; // 可以设置为 "gaussian", "uniform", 或 "multi-gaussian"

        // 测试生成的 pSet
        for (int i = 0; i < 10; i++) {
            long[] point = generatePoints(xrange, distributionType);
            System.out.println("pSet: (" + point[0] + ", " + point[1] + ")");
        }
    }
}
