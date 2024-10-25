package org.davidmoten.Scheme.Construction;

import java.util.*;

// ConstructionOne类，将所有之前的方法合并到此类中
public class ConstructionOne {

    private Map<String, String> Ux, Uy;
    private List<Node> nodes;
    private List<String> invertedIndexX, invertedIndexY;
    private List<String> nodeInvertedIndexX, nodeInvertedIndexY;

    // Node类作为二叉树节点
    class Node {
        int label;
        int index;
        Node left, right;

        Node(int label) {
            this.label = label;
            this.left = this.right = null;
        }
    }

    // 构造函数
    public ConstructionOne() {
        Ux = new HashMap<>();
        Uy = new HashMap<>();
        nodes = new ArrayList<>();
        invertedIndexX = new ArrayList<>();
        invertedIndexY = new ArrayList<>();
        nodeInvertedIndexX = new ArrayList<>();
        nodeInvertedIndexY = new ArrayList<>();
    }

    // 1. BuildBinaryTree(t)
    public void buildBinaryTree(int t) {
        // Generate leaf nodes
        int j = 0;
        for (int i = (int) Math.pow(2, t) - 1; i < Math.pow(2, t + 1) - 1; i++) {
            Node node = new Node(i);
            node.index = j++; // Assign index to leaf nodes
            nodes.add(node);
        }

        // Generate internal nodes
        for (int i = (int) Math.pow(2, t) - 2; i >= 0; i--) {
            Node node = new Node(i);
            node.left = nodes.get(2 * i + 1);
            node.right = nodes.get(2 * i + 2);
            nodes.add(0, node); // Add internal nodes at the beginning
        }
    }

    // 2. BuildInvertedIndex(t, N, coordinates)
    public void buildInvertedIndex(int t, int N, int[] coordinates, boolean isX) {
        for (int i = 0; i < Math.pow(2, t); i++) {
            StringBuilder bitString = new StringBuilder();
            for (int j = 0; j < N; j++) {
                if (coordinates[j] == i) {
                    bitString.append("1");
                } else {
                    bitString.append("0");
                }
            }
            if (isX) {
                invertedIndexX.add(bitString.toString());
            } else {
                invertedIndexY.add(bitString.toString());
            }
        }
    }

    // 3. BuildNodeInvertedIndex(ST, BT)
    public void buildNodeInvertedIndex(boolean isX) {
        int t = nodes.size() / 2;

        // Assign bit strings to leaf nodes
        for (int i = t - 1; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (isX) {
                nodeInvertedIndexX.add(invertedIndexX.get(node.index));
            } else {
                nodeInvertedIndexY.add(invertedIndexY.get(node.index));
            }
        }

        // Generate bit strings for parent nodes
        for (int i = t - 2; i >= 0; i--) {
            Node node = nodes.get(i);
            if (isX) {
                String bitString = orStrings(nodeInvertedIndexX.get(2 * i + 1), nodeInvertedIndexX.get(2 * i + 2));
                nodeInvertedIndexX.add(0, bitString);
            } else {
                String bitString = orStrings(nodeInvertedIndexY.get(2 * i + 1), nodeInvertedIndexY.get(2 * i + 2));
                nodeInvertedIndexY.add(0, bitString);
            }
        }
    }

    private String orStrings(String s1, String s2) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s1.length(); i++) {
            result.append((s1.charAt(i) == '1' || s2.charAt(i) == '1') ? "1" : "0");
        }
        return result.toString();
    }

    // 4. EDS Setup(λ, t, Sx, Sy, BT)
    public void setupEDS(int lambda) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            String key = generateKey(lambda, node.label);
            String encryptedX = encrypt(key, nodeInvertedIndexX.get(i));
            String encryptedY = encrypt(key, nodeInvertedIndexY.get(i));
            Ux.put("TAGX" + node.label, encryptedX);
            Uy.put("TAGY" + node.label, encryptedY);
        }
    }

    private String generateKey(int lambda, int label) {
        // Simulating key generation using a hash function
        return "KEY_" + lambda + "_" + label;
    }

    private String encrypt(String key, String data) {
        // Placeholder encryption function
        return key + "_" + data;
    }

    public String getEncryptedX(String tag) {
        return Ux.get(tag);
    }

    public String getEncryptedY(String tag) {
        return Uy.get(tag);
    }

    // 5. Search
    public void searchRange(int[] xRange, int[] yRange) {
        // Find the minimum nodes covering the range
        List<Node> xNodes = findNodes(xRange);
        List<Node> yNodes = findNodes(yRange);

        // Retrieve and decrypt data
        String SxResult = retrieveAndDecrypt(xNodes, true);
        String SyResult = retrieveAndDecrypt(yNodes, false);

        // Perform intersection
        String result = andStrings(SxResult, SyResult);
        System.out.println("Query Result: " + result);
    }

    private List<Node> findNodes(int[] range) {
        List<Node> nodesInRange = new ArrayList<>();
        for (Node node : nodes) {
            if (node.index >= range[0] && node.index <= range[1]) {
                nodesInRange.add(node);
            }
        }
        return nodesInRange;
    }

    private String retrieveAndDecrypt(List<Node> nodes, boolean isX) {
        StringBuilder result = new StringBuilder();
        for (Node node : nodes) {
            String tag = (isX ? "TAGX" : "TAGY") + node.label;
            String encryptedData = isX ? getEncryptedX(tag) : getEncryptedY(tag);
            result.append(decrypt(encryptedData));
        }
        return result.toString();
    }

    private String decrypt(String encryptedData) {
        // Placeholder decrypt function
        return encryptedData.split("_")[2];
    }

    private String andStrings(String s1, String s2) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s1.length(); i++) {
            result.append((s1.charAt(i) == '1' && s2.charAt(i) == '1') ? "1" : "0");
        }
        return result.toString();
    }

    // 6. Update
    public void updatePoint(int oldX, int oldY, int newX, int newY, int pointIndex) {
        // Update X dimension
        if (oldX != newX) {
            updateDimension(oldX, newX, pointIndex, true);
        }
        // Update Y dimension
        if (oldY != newY) {
            updateDimension(oldY, newY, pointIndex, false);
        }
    }

    private void updateDimension(int oldVal, int newVal, int pointIndex, boolean isX) {
        List<Node> oldNodes = findNodes(new int[]{oldVal, oldVal});
        List<Node> newNodes = findNodes(new int[]{newVal, newVal});

        // Simulate update with encryption and replace the bit strings
        for (Node oldNode : oldNodes) {
            String tag = (isX ? "TAGX" : "TAGY") + oldNode.label;
            String newBitString = generateUpdateBitString(pointIndex, false);
            Ux.put(tag, newBitString);  // Update for X dimension, similarly for Y
        }

        for (Node newNode : newNodes) {
            String tag = (isX ? "TAGX" : "TAGY") + newNode.label;
            String newBitString = generateUpdateBitString(pointIndex, true);
            Ux.put(tag, newBitString);
        }
    }

    private String generateUpdateBitString(int pointIndex, boolean isAdd) {
        StringBuilder bitString = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == pointIndex) {
                bitString.append(isAdd ? "1" : "0");
            } else {
                bitString.append("0");
            }
        }
        return bitString.toString();
    }
    public static void main(String[] args) {
        // 实例化ConstructionOne类
        ConstructionOne construction = new ConstructionOne();
        int t = 3;  // 树的高度 - 例如有8个叶子节点（2^3 = 8）

        // 构建二叉树
        construction.buildBinaryTree(t);

        // 假设我们有8个数据点的x和y坐标，生成倒排索引
        int[] xCoordinates = {0, 1, 2, 3, 4, 5, 6, 7};  // x轴的坐标
        int[] yCoordinates = {0, 1, 2, 3, 4, 5, 6, 7};  // y轴的坐标
        int N = 8;  // 数据点数量

        // 构建倒排索引 (分别针对X和Y轴)
        construction.buildInvertedIndex(t, N, xCoordinates, true);  // X轴倒排索引
        construction.buildInvertedIndex(t, N, yCoordinates, false); // Y轴倒排索引

        // 构建节点倒排索引
        construction.buildNodeInvertedIndex(true);  // X轴节点倒排索引
        construction.buildNodeInvertedIndex(false); // Y轴节点倒排索引

        // 设置加密数据集 (EDS)
        int lambda = 128;  // 安全参数
        construction.setupEDS(lambda);

        // 测试搜索功能：假设我们想查询x坐标范围[2, 5] 和 y坐标范围[3, 6] 的数据点
        int[] xRange = {2, 5};  // 查询x轴范围
        int[] yRange = {3, 6};  // 查询y轴范围
        System.out.println("搜索结果:");
        construction.searchRange(xRange, yRange);  // 搜索并打印结果

        // 测试更新功能：将第3个数据点从(3,2)更新到(6,5)
        int oldX = 3, oldY = 2;
        int newX = 6, newY = 5;
        int pointIndex = 2;  // 第3个点的索引为2（从0开始计数）
        System.out.println("更新数据点 (3,2) 到 (6,5)");
        construction.updatePoint(oldX, oldY, newX, newY, pointIndex);

        // 再次测试搜索，查看更新后的效果
        System.out.println("更新后的搜索结果:");
        construction.searchRange(xRange, yRange);  // 再次搜索并打印结果
    }
}
