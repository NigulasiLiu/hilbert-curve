package org.davidmoten.quadtree;



import org.davidmoten.bpc.BPCGenerator;
import org.davidmoten.hilbert.HilbertComponent.HilbertCurve;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;

public class Quadtree {
    private Node root;
    private BigInteger minX, minY, maxX, maxY;
    private int maxDepth;
    private Map<BigInteger, String> encodingMap = new HashMap<>(); // 存储坐标和四叉树编码的映射

    static class Node {
        BigInteger x, y;
        Node[] children = new Node[4];
        boolean isLeaf;

        public Node(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
            this.isLeaf = true;
        }
    }

    public Quadtree(BigInteger minX, BigInteger minY, BigInteger maxX, BigInteger maxY, int maxDepth) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxDepth = maxDepth;
        this.root = new Node(BigInteger.ZERO, BigInteger.ZERO); // Initialize root
    }

    public String getEncodeString(BigInteger pointX,BigInteger pointY) {
        int depth = this.maxDepth;
        StringBuilder K = new StringBuilder();
        BigInteger I = pointX;
        BigInteger J = pointY;

        // 从最高位开始，逐层编码
        for (int level = depth - 1; level >= 0; level--) {
            // 提取 I 和 J 的相应位
            BigInteger I_bit = I.shiftRight(level).and(BigInteger.ONE);
            BigInteger J_bit = J.shiftRight(level).and(BigInteger.ONE);

            // 根据 I_bit 和 J_bit 判断象限
            if (I_bit.equals(BigInteger.ZERO) && J_bit.equals(BigInteger.ZERO)) {
                K.append("00");  // NW象限
            } else if (I_bit.equals(BigInteger.ZERO) && J_bit.equals(BigInteger.ONE)) {
                K.append("01");  // NE象限
            } else if (I_bit.equals(BigInteger.ONE) && J_bit.equals(BigInteger.ZERO)) {
                K.append("10");  // SW象限
            } else {
                K.append("11");  // SE象限
            }
        }

        return K.toString();  // 返回生成的编码
    }
    public BigInteger getEncodeBigInteger(BigInteger pointX, BigInteger pointY) {
        int depth = this.maxDepth;
        BigInteger result = BigInteger.ZERO; // 用于存储最终的编码结果

        BigInteger I = pointX;
        BigInteger J = pointY;

        // 从最高位开始，逐层编码
        for (int level = depth - 1; level >= 0; level--) {
            // 提取 I 和 J 的相应位
            BigInteger I_bit = I.shiftRight(level).and(BigInteger.ONE);
            BigInteger J_bit = J.shiftRight(level).and(BigInteger.ONE);

            // 计算当前象限对应的编码值，并将其加到 result 中
            if (I_bit.equals(BigInteger.ZERO) && J_bit.equals(BigInteger.ZERO)) {
                // NW象限: 对应 "00"，此时编码为 0，不变
                result = result.shiftLeft(2); // 左移两位，相当于加上 "00"
            } else if (I_bit.equals(BigInteger.ZERO) && J_bit.equals(BigInteger.ONE)) {
                // NE象限: 对应 "01"，编码为 1
                result = result.shiftLeft(2).add(BigInteger.ONE); // 左移两位，加 1
            } else if (I_bit.equals(BigInteger.ONE) && J_bit.equals(BigInteger.ZERO)) {
                // SW象限: 对应 "10"，编码为 2
                result = result.shiftLeft(2).add(BigInteger.valueOf(2)); // 左移两位，加 2
            } else {
                // SE象限: 对应 "11"，编码为 3
                result = result.shiftLeft(2).add(BigInteger.valueOf(3)); // 左移两位，加 3
            }
        }

        return result;  // 返回计算得到的BigInteger编码结果
    }

    private void encode(Node node, BigInteger x, BigInteger y, BigInteger minX, BigInteger minY, BigInteger maxX, BigInteger maxY, int depth, StringBuilder code) {
        if (depth >= maxDepth) return;

        BigInteger midX = (minX.add(maxX)).shiftRight(1);
        BigInteger midY = (minY.add(maxY)).shiftRight(1);

        if (x.compareTo(midX) <= 0 && y.compareTo(midY) <= 0) {
            code.append("00");
            if (node.children[0] == null) node.children[0] = new Node(x, y);
            encode(node.children[0], x, y, minX, minY, midX, midY, depth + 1, code);
        } else if (x.compareTo(midX) <= 0 && y.compareTo(midY) > 0) {
            code.append("01");
            if (node.children[1] == null) node.children[1] = new Node(x, y);
            encode(node.children[1], x, y, minX, midY, midX, maxY, depth + 1, code);
        } else if (x.compareTo(midX) > 0 && y.compareTo(midY) <= 0) {
            code.append("10");
            if (node.children[2] == null) node.children[2] = new Node(x, y);
            encode(node.children[2], x, y, midX, minY, maxX, midY, depth + 1, code);
        } else {
            code.append("11");
            if (node.children[3] == null) node.children[3] = new Node(x, y);
            encode(node.children[3], x, y, midX, midY, maxX, maxY, depth + 1, code);
        }
    }


    public List<BigInteger> getBigIntegerEncodings() {
        List<BigInteger> bigIntegerEncodings = new ArrayList<>();
        Random random = new Random();
        BigInteger xStart = generateRandomCoordinate(minX, maxX, random);
        BigInteger yStart = generateRandomCoordinate(minY, maxY, random);
        BigInteger xEnd = xStart.add(BigInteger.TWO);
        BigInteger yEnd = yStart.add(BigInteger.TWO);

        List<BigInteger[]> points = randomPointsInBounds(xStart, yStart, xEnd, yEnd);

        for (BigInteger[] point : points) {
            String encoding = getEncodeString(point[1], point[2]);
            System.out.println("points: [" + point[1]+","+point[2]+"]");
            System.out.println("encoding: " + encoding);
            BigInteger encodingBigInt = new BigInteger(encoding, 2); // Base 2 (binary) to BigInteger
            bigIntegerEncodings.add(encodingBigInt);
        }
        System.out.println("bigIntegerEncodings: " + bigIntegerEncodings);
        return bigIntegerEncodings;
    }

    private BigInteger generateRandomCoordinate(BigInteger min, BigInteger max, Random random) {
        BigInteger range = max.subtract(min).add(BigInteger.ONE);
        return min.add(new BigInteger(range.bitLength(), random).mod(range));
    }

    private List<BigInteger[]> randomPointsInBounds(BigInteger xStart, BigInteger yStart, BigInteger xEnd, BigInteger yEnd) {
        List<BigInteger[]> points = new ArrayList<>();
        BigInteger x = xStart;
        while (x.compareTo(xEnd) <= 0) {
            BigInteger y = yStart;
            while (y.compareTo(yEnd) <= 0) {
                points.add(new BigInteger[]{BigInteger.valueOf(points.size()), x, y});
                y = y.add(BigInteger.ONE);
            }
            x = x.add(BigInteger.ONE);
        }
        return points;
    }

    public static String hilbertToQuadtreeDirect(int order, int h) {
        StringBuilder quadtreeBinaryStr = new StringBuilder();  // 初始化四叉树编码的二进制串

        // Hilbert曲线和四叉树的象限必须逐层处理
        for (int i = order - 1; i >= 0; i--) {
            int s = 2 * i;  // 计算当前层的位移量（2位表示一个象限）
            int hilbertQuadrant = (h >> s) & 3;  // 从希尔伯特编码中提取两位象限位

            // 使用与getEncodeString相同的象限映射规则
            String quadtreeQuadrantBinary;
            switch (hilbertQuadrant) {
                case 0: quadtreeQuadrantBinary = "00"; break;  // NW象限 -> 四叉树左下象限（00）
                case 1: quadtreeQuadrantBinary = "01"; break;  // NE象限 -> 四叉树右下象限（01）
                case 2: quadtreeQuadrantBinary = "10"; break;  // SW象限 -> 四叉树左上象限（10）
                case 3: quadtreeQuadrantBinary = "11"; break;  // SE象限 -> 四叉树右上象限（11）
                default: throw new IllegalArgumentException("Invalid Hilbert quadrant");
            }

            quadtreeBinaryStr.append(quadtreeQuadrantBinary);  // 将象限二进制字符串拼接到结果中
        }

        return quadtreeBinaryStr.toString();  // 返回最终的二进制串
    }


    public static void main(String[] args) {
        int order = 2;
        BigInteger x = new BigInteger("3");
        BigInteger y = new BigInteger("3");
        System.out.println("目标点: " + "("+x+","+y+")");
        // 定义minX, minY, maxX, maxY用于构建Quadtree
        BigInteger minX = BigInteger.ZERO;
        BigInteger minY = BigInteger.ZERO;
        BigInteger maxX = BigInteger.valueOf(1L << order);  // 2^order
        BigInteger maxY = BigInteger.valueOf(1L << order);  // 2^order

        Quadtree quadtree = new Quadtree(minX, minY, maxX, maxY, order);
        HilbertCurve hilbertCurve;
        hilbertCurve = HilbertCurve.bits(order).dimensions(2);
        DecimalFormat df = new DecimalFormat("#.00");

        // 计算 getEncodeString 方法的运行时间（纳秒）
        long startTimeUpdate = System.nanoTime();
        String encode = quadtree.getEncodeString(x, y);
        long endTimeUpdate = System.nanoTime();
        long durationUpdateNs = endTimeUpdate - startTimeUpdate;
        System.out.println("getEncodeString 方法运行时间: " + df.format(durationUpdateNs) + " 纳秒");
        System.out.println("encode: " + encode);

        // 计算 getEncodeBigInteger 方法的运行时间
        long startTimeUpdate_2 = System.nanoTime();
        BigInteger encodevalue = quadtree.getEncodeBigInteger(x, y);
        long endTimeUpdate_2 = System.nanoTime();
        long durationUpdateNs_2 = endTimeUpdate_2 - startTimeUpdate_2;
        System.out.println("encodevalue 方法运行时间: " + df.format(durationUpdateNs_2) + " 纳秒");
        System.out.println("encodevalue: " + encodevalue);

        // 计算 HilbertCurve.index 方法的运行时间（纳秒）
        long startTimeUpdate1 = System.nanoTime();
        BigInteger num = hilbertCurve.index(x.longValue(), y.longValue());
        long endTimeUpdate1 = System.nanoTime();
        long durationUpdateNs1 = endTimeUpdate1 - startTimeUpdate1;
        System.out.println("hilbertCurve.index 方法运行时间: " + df.format(durationUpdateNs1) + " 纳秒");

        System.out.println("Hilbert编码值 (num): " + num);

        // 调用 hilbertToQuadtreeDirect
        String quadtreeBinary = Quadtree.hilbertToQuadtreeDirect(order, num.intValue());
        System.out.println("四叉树编码 (num Quadtree): " + quadtreeBinary);

        // 比较结果是否一致
        if (encode.equals(quadtreeBinary)) {
            System.out.println("两者一致！");
        } else {
            System.out.println("两者不一致，需进一步检查。");
        }
    }
}
