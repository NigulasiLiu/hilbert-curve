package org.davidmoten.Scheme.Construction;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.util.Base64;
import java.util.*;
import java.util.stream.Collectors;

// ConstructionOne类，将所有之前的方法合并到此类中
public class ConstructionOne {

    private Map<String, String> Ux, Uy;
    private Random prf; // 伪随机函数 (PRF)
    private int lambda; // 安全参数 λ
    // 初始化密钥
    private int Ks;
    private int Kx;
    private int Ky;
    private final BigInteger n = new BigInteger("100000");

    //
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
        // 初始化密钥
        Ks = prf.nextInt();  // 主密钥 Ks
        Kx = prf.nextInt();  // x 轴的密钥
        Ky = prf.nextInt();  // y 轴的密钥
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
    public String encrypt(String key, int index, int C, String type) throws Exception {
        BigInteger bigKey = new BigInteger(key);
        BigInteger bigC = BigInteger.valueOf(index);

        if ("homomorphic".equalsIgnoreCase(type)) {
            // 使用同态加密
            return homomorphicEncrypt(bigKey, bigC);
        } else if ("aes".equalsIgnoreCase(type)) {
            // 使用AES加密
            return aesEncrypt(key, String.valueOf(bigC));
        } else {
            throw new IllegalArgumentException("未知加密类型: " + type);
        }
    }
    private String decrypt(String ei, int C, String key, String type) throws Exception {
        StringBuilder decryptedResult = new StringBuilder();

        String decrypted;
        if ("homomorphic".equalsIgnoreCase(type)) {
            // 使用同态解密，示例逻辑: (加密值 - key) % n
            BigInteger encryptedValue = new BigInteger(ei);
            BigInteger bigKey = new BigInteger(key);
            BigInteger decryptedValue = encryptedValue.subtract(bigKey).mod(n);  // 解密
            decrypted = decryptedValue.toString();
        } else if ("aes".equalsIgnoreCase(type)) {
            // 使用AES解密
            decrypted = aesDecrypt(key, ei);
        } else {
            throw new IllegalArgumentException("未知解密类型: " + type);
        }
        decryptedResult.append(decrypted);

        return decryptedResult.toString();  // 返回解密后的结果
    }

    // AES解密
    private String aesDecrypt(String key, String encryptedData) throws Exception {
        // 将Base64编码的加密数据转换为字节数组
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);

        // 生成AES密钥
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();

        // 使用AES解密
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        // 将解密后的字节转换为字符串
        return new String(decryptedBytes);
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
    public Map<Integer, String> buildNodeInvertedIndex(Node[] BT, List<String> invertedIndex, int t, int N, int[] coordinates) {

        // 3. 初始化结果Map，键为节点index，值为节点label
        Map<Integer, String> nodeLabelMap = new HashMap<>();

        // 计算叶子节点的起始索引
        int leafStartIndex = (int) Math.pow(2, t) - 1;  // 叶子节点的起始索引
        int numLeaves = BT.length - leafStartIndex;  // 叶子节点的数量

        // 4. 给叶子节点分配倒排索引字符串
        for (int i = 0; i < numLeaves; i++) {
            Node node = BT[leafStartIndex + i];  // 获取叶子节点
            node.label = invertedIndex.get(i);  // 将倒排索引字符串分配给节点的label
            nodeLabelMap.put(node.index, node.label);  // 将节点index和label存入map
        }

        // 5. 递归地为内部节点计算位串，按位或操作合并左右子节点的label
        for (int i = leafStartIndex - 1; i >= 0; i--) {
            Node node = BT[i];  // 获取内部节点
            Node leftChild = BT[2 * i + 1];  // 左子节点
            Node rightChild = BT[2 * i + 2];  // 右子节点

            // 将左右子节点的位串按位或后，赋值给父节点的label
            node.label = orStrings(leftChild.label, rightChild.label);
            nodeLabelMap.put(node.index, node.label);  // 将内部节点index和label存入map
        }

        // 返回节点的索引和标签映射
        return nodeLabelMap;
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

    // 修改后的查找最小覆盖节点的方法
    private List<Node> findMinimumCover(int[] range, Node[] BT) {
        List<Node> nodesInRange = new ArrayList<>();

        // 初次调用递归函数
        findMinimumCoverRecursive(range[0], range[1], BT, nodesInRange);

//        return nodesInRange.stream().sorted(Comparator.comparing(node -> node.index)).collect(Collectors.toList());
        return nodesInRange;

    }

    // 递归辅助函数，查找最小的覆盖节点
    private void findMinimumCoverRecursive(int start, int end, Node[] BT, List<Node> nodesInRange) {
        // 如果范围内的节点数少于 2，直接添加到结果并返回
        if (start == end) {
            nodesInRange.add(BT[start]);
            return;
        }

        // 创建一个集合，用于保存右移后的结果
        Map<Integer, List<Integer>> shiftedResults = new HashMap<>();
        List<Integer> remainingNodes = new ArrayList<>();

        // 1. 将 range[start] 到 range[end] 的每个节点右移一位
        for (int i = start; i <= end; i++) {
            int shiftedValue = (i-1) >> 1;  // 相当于除以2
            shiftedResults.computeIfAbsent(shiftedValue, k -> new ArrayList<>()).add(i);
        }
        // 2. 处理右移后的结果，合并相同值的节点，保留无法合并的节点
        for (Map.Entry<Integer, List<Integer>> entry : shiftedResults.entrySet()) {
            List<Integer> indices = entry.getValue();

            if (indices.size() > 1) {
                // 如果有两个数右移后的结果相同，替换成新的值
                remainingNodes.add(entry.getKey());  // 将右移后的结果作为新的节点
            } else {
                // 如果右移后的结果只有一个来源，保留原来的数
                nodesInRange.add(BT[indices.get(0)]);
            }
        }

        // 3. 如果剩下的节点数量和原来的数量相同，说明无法再进行合并，终止递归
        if (remainingNodes.isEmpty()) {
            return;
        }

        // 4. 递归调用，将新的序列作为 range 继续递归处理
        int[] newRange = {remainingNodes.get(0), remainingNodes.get(remainingNodes.size() - 1)};
        findMinimumCoverRecursive(newRange[0], newRange[1], BT, nodesInRange);
    }

    private int[] rangeConvert(int t, int[] R){
        return new int[]{(int) Math.pow(2, t) - 1 + R[0], (int) Math.pow(2, t) - 1 + R[1]};
    }


    // setupEDS 方法实现
    public void setupEDS(int t, Map<Integer, String> Sx, Map<Integer, String> Sy, Node[] BTx, Node[] BTy) throws Exception {
        int C = 1;
        int C_prime = 1;
        // 遍历二叉树的每个节点
        for (int i = 0; i < BTx.length; i++) {
            // 从二叉树中检索节点
            Node ni = BTx[i];

            // 生成 PRF 密钥 Ki
            String Ki = generatePRF(Ks, ni.index);

            // 生成 X 轴和 Y 轴的标签 TAGXi 和 TAGYi
            String TAGXi = generatePRF(Kx, ni.index);  // X 轴的标签
            String TAGYi = generatePRF(Ky, ni.index);  // Y 轴的标签

            // 对 X 轴的节点进行加密处理并存储到 Ux
            if (Sx.containsKey(ni.index)) {
                String ei = encrypt(Ki, ni.index, C,"aes");  // 加密
                Ux.put(TAGXi, ei);  // 存储加密后的数据到 Ux
            }

            // 对 Y 轴的节点进行加密处理并存储到 Uy
            if (Sy.containsKey(ni.index)) {
                String ei = encrypt(Ki, ni.index, C_prime, "aes");  // 加密
                Uy.put(TAGYi, ei);  // 存储加密后的数据到 Uy
            }
        }
    }
    // BuildKey: 生成以 ni 为根的子树中所有节点的密钥
    public List<String> buildKey(Node ni, int Ks) {
        List<String> keys = new ArrayList<>();

        // 递归生成子树中每个节点的密钥
        generateKeysForSubtree(ni, Ks, keys);

        return keys;  // 返回密钥集合
    }

    // 递归函数：遍历以 ni 为根的子树，并生成每个节点的密钥
    private void generateKeysForSubtree(Node node, int Ks, List<String> keys) {
        if (node == null) {
            return;
        }

        // 生成当前节点的密钥 Kj = F(Ks, node.index)
        String Kj = generatePRF(Ks, node.index);
        keys.add(Kj);

        // 递归遍历左右子树
        generateKeysForSubtree(node.left, Ks, keys);
        generateKeysForSubtree(node.right, Ks, keys);
    }
    // Client Search：由数据所有者执行，生成搜索令牌并解密结果
    public List<String> clientSearch(int[] xRange, int[] yRange, int t, int C, int C_prime) throws Exception {
        // 构建 X 轴的二叉树并找到覆盖查询范围的最小节点
        Node[] BTx = buildBinaryTree(t);
        List<Node> xNodes = findMinimumCover(xRange, BTx);  // 找到最小覆盖节点

        // 构建 X 轴的搜索令牌
        List<String> TAGX = new ArrayList<>();
        for (Node ni : xNodes) {
            String TAGXi = generatePRF(Kx, ni.index);  // 使用 X 轴的密钥
            TAGX.add(TAGXi);
        }

        // 构建 Y 轴的二叉树并找到覆盖查询范围的最小节点
        Node[] BTy = buildBinaryTree(t);
        List<Node> yNodes = findMinimumCover(yRange, BTy);  // 找到最小覆盖节点

        // 构建 Y 轴的搜索令牌
        List<String> TAGY = new ArrayList<>();
        for (Node ni : yNodes) {
            String TAGYi = generatePRF(Ky, ni.index);  // 使用 Y 轴的密钥
            TAGY.add(TAGYi);
        }

        // 发送令牌并获取服务器返回的加密结果 ER
        List<List<String>> ER = serverSearch(TAGX, TAGY);  // ER[0] 是 ei, ER[1] 是 ei_prime
        List<String> Sx = new ArrayList<>();
        List<String> Sy = new ArrayList<>();
        for (int i =0;i<xNodes.size();i++) {
            List<String> Kx_dec = buildKey(xNodes.get(i), Ks);
            Sx.add(decrypt(ER.get(0).get(i), C,Kx_dec.get(0),"ase"));
        }
        for (int i =0;i<yNodes.size();i++) {
            List<String> Ky_dec = buildKey(yNodes.get(i), Ks);
            Sx.add(decrypt(ER.get(0).get(i), C,Ky_dec.get(0),"ase"));
        }
        // 取 X 轴和 Y 轴结果的交集
        List<String> result = intersectStrings(Sx, Sy);

        return result;  // 返回最终搜索结果
    }

    // Server Search：服务器根据搜索令牌 (TAGX, TAGY) 返回加密结果
    private List<List<String>> serverSearch(List<String> TAGX, List<String> TAGY) {
        List<String> ER_X = new ArrayList<>();
        List<String> ER_Y = new ArrayList<>();

        // 搜索 X 轴的加密数据 (ei)
        for (String tagX : TAGX) {
            String ei = Ux.get(tagX);
            if (ei != null) {
                ER_X.add(ei);
            }
        }

        // 搜索 Y 轴的加密数据 (ei_prime)
        for (String tagY : TAGY) {
            String ei_prime = Uy.get(tagY);
            if (ei_prime != null) {
                ER_Y.add(ei_prime);
            }
        }

        List<List<String>> ER = new ArrayList<>();
        ER.add(ER_X);  // X 轴结果
        ER.add(ER_Y);  // Y 轴结果

        return ER;  // 返回加密结果集，分别为 ei 和 ei_prime
    }
    // intersectStrings: 使用 HashSet 高效计算 Sx 和 Sy 的交集
    private List<String> intersectStrings(List<String> Sx, List<String> Sy) {
        // 如果 Sx 比 Sy 小，使用 Sx 初始化 HashSet，否则使用 Sy
        Set<String> set = (Sx.size() < Sy.size()) ? new HashSet<>(Sx) : new HashSet<>(Sy);
        List<String> smallerList = (Sx.size() < Sy.size()) ? Sy : Sx;

        // 创建交集结果列表
        List<String> result = new ArrayList<>();

        // 遍历较小的列表，并检查 HashSet 中是否包含该元素
        for (String s : smallerList) {
            if (set.contains(s)) {
                result.add(s);
            }
        }

        return result;  // 返回交集
    }


    public static void main(String[] args) throws Exception {
        // 实例化ConstructionOne类
        ConstructionOne construction = new ConstructionOne(128);
        int t = 3;  // 树的高度 - 例如有8个叶子节点（2^3 = 8）
        // 假设我们有8个数据点的x和y坐标，生成倒排索引
        int[] xCoordinates = {1, 1, 6, 4, 7, 4, 2, 5};  // x轴的坐标
        int[] yCoordinates = {1, 3, 2, 4, 4, 5, 7, 7};  // y轴的坐标
        int N = 8;  // 数据点数量


        // 1. 调用buildBinaryTree生成二叉树节点数组
        Node[] BTx =  construction.buildBinaryTree(t);
        Node[] BTy =  construction.buildBinaryTree(t);

        // 2. 调用buildInvertedIndex生成倒排索引
        List<String> invertedIndex = construction.buildInvertedIndex(t, N, xCoordinates);
        List<String> invertedIndey = construction.buildInvertedIndex(t, N, yCoordinates);

        // 构建节点倒排索引
        Map<Integer, String> Sx = construction.buildNodeInvertedIndex(BTx, invertedIndex, t, N, xCoordinates);  // X轴节点倒排索引
        Map<Integer, String> Sy = construction.buildNodeInvertedIndex(BTy, invertedIndey, t, N, yCoordinates); // Y轴节点倒排索引

        // 设置加密数据集 (EDS)
        int lambda = 128;  // 安全参数
        construction.setupEDS(lambda, Sx, Sy, BTx, BTy);

        int[] rangex = construction.rangeConvert(3, new int[]{2, 6});;  // 查询范围
        int[] rangey = construction.rangeConvert(3, new int[]{0, 3});;  // 查询范围
        List<Node> xnodesCovered = construction.findMinimumCover(rangex, BTx);
        List<Node> ynodesCovered = construction.findMinimumCover(rangey, BTy);

        // 打印结果
        for (Node node : xnodesCovered) {
            System.out.println("Index: " + node.index + ", xLabel: " + node.label);
        }
        for (Node node : ynodesCovered) {
            System.out.println("Index: " + node.index + ", yLabel: " + node.label);
        }
    }
}



