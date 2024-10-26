package org.davidmoten.Scheme.Construction;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.util.Base64;
import java.util.*;

// ConstructionOne类，将所有之前的方法合并到此类中
public class ConstructionOne {

    private Map<String, String> Ux, Uy;
    private Random prf; // 伪随机函数 (PRF)
    private int lambda; // 安全参数 λ
    private final BigInteger n = new BigInteger("100000");

    // Node类作为二叉树节点
    class Node {
        int index;
        String label;
        Node left, right;

        Node(int index) {
            this.index = index;
            this.label = ""; // 初始化为空字符串
            this.left = this.right = null;
        }

        @Override
        public String toString() {
            return "Node [index=" + index + ", label=" + label + "]";
        }
    }

    // 构造函数
    public ConstructionOne(int lambda) {
        Ux = new HashMap<>();
        Uy = new HashMap<>();
        prf = new Random();  // 模拟PRF伪随机函数
        this.lambda = lambda;
    }
    // 生成伪随机函数 (PRF) 输出
    private String generatePRF(int key, int nodeIndex) {
        prf.setSeed(key + nodeIndex);  // 使用 key 和节点索引作为种子
        return String.valueOf(prf.nextInt(100000));  // 模拟PRF输出
    }
    // 使用BigInteger的同态加密
    private String homomorphicEncrypt(BigInteger key, BigInteger value) {
        // 示例逻辑: (key + value) % n
        BigInteger encryptedValue = key.add(value).mod(n);
        return encryptedValue.toString();
    }

    // AES加密
    private String aesEncrypt(String key, String data) throws Exception {
        // 使用AES算法加密
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());

        // 将加密后的字节转换为Base64字符串
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // 修改后的encrypt方法，增加type参数，选择加密方式
    public String encrypt(String key, int C, String type) throws Exception {
        BigInteger bigKey = new BigInteger(key);
        BigInteger bigC = BigInteger.valueOf(C);

        if ("homomorphic".equalsIgnoreCase(type)) {
            // 使用同态加密
            return homomorphicEncrypt(bigKey, bigC);
        } else if ("aes".equalsIgnoreCase(type)) {
            // 使用AES加密
            return aesEncrypt(key, String.valueOf(C));
        } else {
            throw new IllegalArgumentException("未知加密类型: " + type);
        }
    }

    // 修改后的buildBinaryTree方法，返回Node[]数组
    public Node[] buildBinaryTree(int t) {
        int numNodes = (int) Math.pow(2, t + 1) - 1;  // 总节点数 = 2^(t+1) - 1
        Node[] nodes = new Node[numNodes];  // 初始化节点数组

        // 生成叶子节点，从 (2^t - 1) 到 (2^(t+1) - 2)
        for (int i = (int) Math.pow(2, t) - 1; i < Math.pow(2, t + 1) - 1; i++) {
            Node node = new Node(i);
            nodes[i] = node;   // 将叶子节点放入数组的正确位置
        }

        // 生成内部节点，从 (2^t - 2) 到 0
        for (int i = (int) Math.pow(2, t) - 2; i >= 0; i--) {
            Node node = new Node(i);
            node.left = nodes[2 * i + 1];   // 获取左子节点
            node.right = nodes[2 * i + 2];  // 获取右子节点
            nodes[i] = node;                // 将内部节点放入数组的正确位置
        }

        // 返回构建的二叉树节点数组
        return nodes;
    }

    // 修改后的buildInvertedIndex方法，返回List<String>
    public List<String> buildInvertedIndex(int t, int N, int[] coordinates) {
        List<String> invertedIndex = new ArrayList<>();

        for (int i = 0; i < Math.pow(2, t); i++) {
            StringBuilder bitString = new StringBuilder();
            for (int j = 0; j < N; j++) {
                if (coordinates[j] == i) {
                    bitString.append("1");
                } else {
                    bitString.append("0");
                }
            }
            invertedIndex.add(bitString.toString());
        }

        return invertedIndex;
    }

    // 修改后的buildNodeInvertedIndex方法，返回Map<Integer, String>
    public Map<Integer, String> buildNodeInvertedIndex(int t, int N, int[] coordinates) {
        // 1. 调用buildBinaryTree生成二叉树节点数组
        Node[] nodes = buildBinaryTree(t);

        // 2. 调用buildInvertedIndex生成倒排索引
        List<String> invertedIndex = buildInvertedIndex(t, N, coordinates);

        // 3. 初始化结果Map，键为节点index，值为节点label
        Map<Integer, String> nodeLabelMap = new HashMap<>();

        // 计算叶子节点的起始索引
        int leafStartIndex = (int) Math.pow(2, t) - 1;  // 叶子节点的起始索引
        int numLeaves = nodes.length - leafStartIndex;  // 叶子节点的数量

        // 4. 给叶子节点分配倒排索引字符串
        for (int i = 0; i < numLeaves; i++) {
            Node node = nodes[leafStartIndex + i];  // 获取叶子节点
            node.label = invertedIndex.get(i);  // 将倒排索引字符串分配给节点的label
            nodeLabelMap.put(node.index, node.label);  // 将节点index和label存入map
        }

        // 5. 递归地为内部节点计算位串，按位或操作合并左右子节点的label
        for (int i = leafStartIndex - 1; i >= 0; i--) {
            Node node = nodes[i];  // 获取内部节点
            Node leftChild = nodes[2 * i + 1];  // 左子节点
            Node rightChild = nodes[2 * i + 2];  // 右子节点

            // 将左右子节点的位串按位或后，赋值给父节点的label
            node.label = orStrings(leftChild.label, rightChild.label);
            nodeLabelMap.put(node.index, node.label);  // 将内部节点index和label存入map
        }

        // 返回节点的索引和标签映射
        return nodeLabelMap;
    }


    // setupEDS 方法实现
    public void setupEDS(int t, Map<Integer, String> Sx, Map<Integer, String> Sy, Node[] BT) throws Exception {
        // 初始化密钥
        int Ks = prf.nextInt();  // 主密钥 Ks
        int Kx = prf.nextInt();  // x 轴的密钥
        int Ky = prf.nextInt();  // y 轴的密钥
        int C = 1;
        // 遍历二叉树的每个节点
        for (int i = 0; i < BT.length; i++) {
            Node ni = BT[i];  // 从二叉树中检索节点

            // 生成 PRF 密钥 Ki
            String Ki = generatePRF(Ks, ni.index);

            // 生成 X 轴和 Y 轴的标签 TAGXi 和 TAGYi
            String TAGXi = generatePRF(Kx, ni.index);  // X 轴的标签
            String TAGYi = generatePRF(Ky, ni.index);  // Y 轴的标签

            // 对 X 轴的节点进行加密处理并存储到 Ux
            if (Sx.containsKey(ni.index)) {
                String ei = encrypt(Ki, C, "aes");  // 加密
                Ux.put(TAGXi, ei);  // 存储加密后的数据到 Ux
            }

            // 对 Y 轴的节点进行加密处理并存储到 Uy
            if (Sy.containsKey(ni.index)) {
                String ei = encrypt(Ki, C, "aes");  // 加密
                Uy.put(TAGYi, ei);  // 存储加密后的数据到 Uy
            }
        }
    }


    private String orStrings(String s1, String s2) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s1.length(); i++) {
            // 按位或操作，将两个位串的对应位进行或运算
            result.append((s1.charAt(i) == '1' || s2.charAt(i) == '1') ? '1' : '0');
        }
        return result.toString();
    }
    private String andStrings(String s1, String s2) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s1.length(); i++) {
            result.append((s1.charAt(i) == '1' && s2.charAt(i) == '1') ? "1" : "0");
        }
        return result.toString();
    }

    // Client Search：由数据所有者执行，生成搜索令牌并解密结果
    public String clientSearch(int[] xRange, int[] yRange, int t, int C, int C_prime) {
        // 构建 X 轴的二叉树并找到覆盖查询范围的最小节点
        Node[] BTx = buildBinaryTree(t);
        List<Node> xNodes = findMinimumNodes(xRange, BTx);  // 找到最小覆盖节点

        // 构建 X 轴的搜索令牌
        List<String> TAGX = new ArrayList<>();
        for (Node ni : xNodes) {
            String TAGXi = generatePRF(Kx, ni.index);
            TAGX.add(TAGXi);
        }

        // 构建 Y 轴的二叉树并找到覆盖查询范围的最小节点
        Node[] BTy = buildBinaryTree(t);
        List<Node> yNodes = findMinimumNodes(yRange, BTy);  // 找到最小覆盖节点

        // 构建 Y 轴的搜索令牌
        List<String> TAGY = new ArrayList<>();
        for (Node ni : yNodes) {
            String TAGYi = generatePRF(Ky, ni.index);
            TAGY.add(TAGYi);
        }

        // 发送令牌并获取服务器返回的加密结果 ER
        List<String> ER = serverSearch(TAGX, TAGY);

        // 客户端解密结果
        String Sx = decryptResults(ER, C, true);   // 解密 X 轴的结果
        String Sy = decryptResults(ER, C_prime, false);  // 解密 Y 轴的结果

        // 取 X 轴和 Y 轴结果的交集
        String result = andStrings(Sx, Sy);

        return result;  // 返回最终搜索结果
    }

    // 查找最小覆盖节点的方法
    private List<Node> findMinimumNodes(int[] range, Node[] BT) {
        List<Node> nodesInRange = new ArrayList<>();
        for (Node node : BT) {
            if (node.index >= range[0] && node.index <= range[1]) {
                nodesInRange.add(node);  // 找到范围内的最小节点
            }
        }
        return nodesInRange;
    }

    // Server Search：服务器根据搜索令牌 (TAGX, TAGY) 返回加密结果
    private List<String> serverSearch(List<String> TAGX, List<String> TAGY) {
        List<String> ER = new ArrayList<>();

        // 搜索 X 轴的加密数据
        for (String tagX : TAGX) {
            String ei = Ux.get(tagX);
            if (ei != null) {
                ER.add(ei);
            }
        }

        // 搜索 Y 轴的加密数据
        for (String tagY : TAGY) {
            String ei = Uy.get(tagY);
            if (ei != null) {
                ER.add(ei);
            }
        }

        return ER;  // 返回加密结果集
    }

    // 解密服务器返回的结果
    private String decryptResults(List<String> ER, int C, boolean isX) {
        StringBuilder decryptedResult = new StringBuilder();

        for (String ei : ER) {
            String decrypted = decrypt(ei);  // 使用解密算法解密
            decryptedResult.append(decrypted);
        }

        return decryptedResult.toString();  // 返回解密后的结果
    }



    public static void main(String[] args) {
        // 实例化ConstructionOne类
        ConstructionOne construction = new ConstructionOne(128);
        int t = 3;  // 树的高度 - 例如有8个叶子节点（2^3 = 8）

        // 构建二叉树
        construction.buildBinaryTree(t);

        // 假设我们有8个数据点的x和y坐标，生成倒排索引
        int[] xCoordinates = {1, 1, 6, 4, 7, 4, 2, 5};  // x轴的坐标
        int[] yCoordinates = {1, 3, 2, 4, 4, 5, 7, 7};  // y轴的坐标
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
        construction.clientSearch(xRange, yRange, t);  // 搜索并打印结果

//        // 测试更新功能：将第3个数据点从(3,2)更新到(6,5)
//        int oldX = 3, oldY = 2;
//        int newX = 6, newY = 5;
//        int pointIndex = 2;  // 第3个点的索引为2（从0开始计数）
//        System.out.println("更新数据点 (3,2) 到 (6,5)");
//        construction.updatePoint(oldX, oldY, newX, newY, pointIndex);
//
//        // 再次测试搜索，查看更新后的效果
//        System.out.println("更新后的搜索结果:");
//        construction.searchRange(xRange, yRange);  // 再次搜索并打印结果
    }
}
