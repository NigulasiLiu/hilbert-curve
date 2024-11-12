package org.davidmoten.Scheme.Construction;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.*;
import java.util.stream.Collectors;

// ConstructionOne类，将所有之前的方法合并到此类中
public class ConstructionOne {

    private Map<String, String> Ux, Uy;
    private Random prf; // 伪随机函数 (PRF)
    private int lambda; // 安全参数 λ
    private int t;
    // 初始化密钥
    private int Ks;
    private int Kx;
    private int Ky;
    private final BigInteger n = new BigInteger("100000");
    private int N;  // 私有变量N
    private int[] xCoordinates;  // x坐标数组
    private int[] yCoordinates;  // y坐标数组
    private int C, C_prime;

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

    public Node[] BTx;
    public Node[] BTy;

    // 构造函数
    public ConstructionOne(int lambda, int t, int N, int[] xCoordinates, int[] yCoordinates) {
        Ux = new HashMap<>();
        Uy = new HashMap<>();
        prf = new Random();  // 模拟PRF伪随机函数
        this.lambda = lambda;
        this.t = t;
        this.N = N;  // 初始化N
        this.xCoordinates = Arrays.copyOf(xCoordinates, N);  // 初始化x坐标数组
        this.yCoordinates = Arrays.copyOf(yCoordinates, N);  // 初始化y坐标数组
        // 初始化密钥
        Ks = prf.nextInt();  // 主密钥 Ks
        Kx = prf.nextInt();  // x 轴的密钥
        Ky = prf.nextInt();  // y 轴的密钥
        this.C = 1;
        this.C_prime = 1;
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
    }    // AES加密
    // 数字偏移加密方法，返回只包含数字的加密字符串
    private String aesEncrypt(String key, String data) throws Exception {
        // 将密钥转换为整数，用于生成偏移量
        int shift = computeNumericShift(key);

        StringBuilder encryptedData = new StringBuilder();
        for (char ch : data.toCharArray()) {
            // 偏移字符，使其仅包含数字字符（0-9）
            int encryptedChar = (ch + shift) % 10;  // 确保数字字符在0-9范围内循环
            encryptedData.append(encryptedChar);
        }
        return encryptedData.toString();
    }

    // 数字偏移解密方法
    private String aesDecrypt(String key, String encryptedData) throws Exception {
        // 计算与加密相同的偏移量
        int shift = computeNumericShift(key);

        StringBuilder decryptedData = new StringBuilder();
        for (char ch : encryptedData.toCharArray()) {
            // 反向偏移字符，使其恢复原字符
            int decryptedChar = (ch - '0' - shift + 10) % 10 + '0';
            decryptedData.append((char) decryptedChar);
        }
        return decryptedData.toString();
    }

    // 计算数字偏移量（根据密钥生成一个整数偏移量）
    private int computeNumericShift(String key) {
        // 计算 key 的哈希值，然后对其取模，生成一个 0 到 9 之间的偏移量
        int shift = 0;
        for (char ch : key.toCharArray()) {
            shift += ch;  // 累加字符的 ASCII 值
        }
        return shift % 10;  // 将偏移量限制在 0-9 范围内
    }

    // 修改后的encrypt方法，使用BigInteger类型处理加密
    public String encrypt(String key, BigInteger index, int C, String type) throws Exception {
        BigInteger bigKey = new BigInteger(key);  // 将key转换为BigInteger

        if ("homomorphic".equalsIgnoreCase(type)) {
            // 使用同态加密
            return homomorphicEncrypt(bigKey, index);
        } else if ("aes".equalsIgnoreCase(type)) {
            // 使用AES加密
            return aesEncrypt(key, index.toString());
        } else {
            throw new IllegalArgumentException("未知加密类型: " + type);
        }
    }

    // 修改后的decrypt方法，使用BigInteger类型处理解密
    private String decrypt(String ei, int C, String key, String type) throws Exception {
        String decrypted;
        if ("homomorphic".equalsIgnoreCase(type)) {
            // 使用同态解密，示例逻辑: (加密值 - key) % n
            BigInteger encryptedValue = new BigInteger(ei);  // 加密值转换为BigInteger
            BigInteger bigKey = new BigInteger(key);  // 密钥转换为BigInteger
            BigInteger decryptedValue = encryptedValue.subtract(bigKey).mod(n);  // 解密

            // 如果结果是负数，进行调整
            if (decryptedValue.signum() < 0) {
                decryptedValue = decryptedValue.add(n);
            }
            decrypted = decryptedValue.toString();
        } else if ("aes".equalsIgnoreCase(type)) {
            // 使用AES解密
            decrypted = aesDecrypt(key, ei);
        } else {
            throw new IllegalArgumentException("未知解密类型: " + type);
        }

        // 将解密后的字符串填充到2^t的长度
        int requiredLength = (int) Math.pow(2, t);
        return String.format("%0" + requiredLength + "d", new BigInteger(decrypted));
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
    public Map<Integer, String> buildxNodeInvertedIndex(List<String> invertedIndex, int t) {

        // 3. 初始化结果Map，键为节点index，值为节点label
        Map<Integer, String> nodeLabelMap = new HashMap<>();

        // 计算叶子节点的起始索引
        int leafStartIndex = (int) Math.pow(2, t) - 1;  // 叶子节点的起始索引
        int numLeaves = BTx.length - leafStartIndex;  // 叶子节点的数量

        // 4. 给叶子节点分配倒排索引字符串
        for (int i = 0; i < numLeaves; i++) {
            Node node = BTx[leafStartIndex + i];  // 获取叶子节点
            node.label = invertedIndex.get(i);  // 将倒排索引字符串分配给节点的label
            nodeLabelMap.put(node.index, node.label);  // 将节点index和label存入map
        }

        // 5. 递归地为内部节点计算位串，按位或操作合并左右子节点的label
        for (int i = leafStartIndex - 1; i >= 0; i--) {
            Node node = BTx[i];  // 获取内部节点
            Node leftChild = BTx[2 * i + 1];  // 左子节点
            Node rightChild = BTx[2 * i + 2];  // 右子节点

            // 将左右子节点的位串按位或后，赋值给父节点的label
            node.label = orStrings(leftChild.label, rightChild.label);
            nodeLabelMap.put(node.index, node.label);  // 将内部节点index和label存入map
        }

        // 返回节点的索引和标签映射
        return nodeLabelMap;
    }
    public Map<Integer, String> buildyNodeInvertedIndex(List<String> invertedIndex, int t) {

        // 3. 初始化结果Map，键为节点index，值为节点label
        Map<Integer, String> nodeLabelMap = new HashMap<>();

        // 计算叶子节点的起始索引
        int leafStartIndex = (int) Math.pow(2, t) - 1;  // 叶子节点的起始索引
        int numLeaves = BTy.length - leafStartIndex;  // 叶子节点的数量

        // 4. 给叶子节点分配倒排索引字符串
        for (int i = 0; i < numLeaves; i++) {
            Node node = BTy[leafStartIndex + i];  // 获取叶子节点
            node.label = invertedIndex.get(i);  // 将倒排索引字符串分配给节点的label
            nodeLabelMap.put(node.index, node.label);  // 将节点index和label存入map
        }

        // 5. 递归地为内部节点计算位串，按位或操作合并左右子节点的label
        for (int i = leafStartIndex - 1; i >= 0; i--) {
            Node node = BTy[i];  // 获取内部节点
            Node leftChild = BTy[2 * i + 1];  // 左子节点
            Node rightChild = BTy[2 * i + 2];  // 右子节点

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

    private String xorStrings(String s1, String s2) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s1.length(); i++) {
            // 按位异或操作，将两个位串的对应位进行异或运算
            result.append((s1.charAt(i) == s2.charAt(i)) ? '0' : '1');
        }
        return result.toString();
    }

    // 修改后的查找最小覆盖节点的方法
    private List<Node> findMinimumCover(int[] range, boolean isX) {
        List<Node> nodesInRange = new ArrayList<>();

        // 初次调用递归函数
        if(isX){
            findMinimumCoverRecursive(range[0], range[1], BTx, nodesInRange);
        }
        else{
            findMinimumCoverRecursive(range[0], range[1], BTy, nodesInRange);
        }

        //return nodesInRange.stream().sorted(Comparator.comparing(node -> node.index)).collect(Collectors.toList());
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
            int shiftedValue = (i - 1) >> 1;  // 相当于除以2
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

    public int[] rangeConvert(int t, int[] R) {
        return new int[]{(int) Math.pow(2, t) - 1 + R[0], (int) Math.pow(2, t) - 1 + R[1]};
    }


    // setupEDS 方法实现
    public void setupEDS(Map<Integer, String> Sx, Map<Integer, String> Sy) throws Exception {
        int totalSteps = BTx.length + BTy.length;
        int completedSteps = 0;

        // 处理 X 轴 (BTx)
        for (int i = 0; i < BTx.length; i++) {
            Node ni = BTx[i];

            // 生成 PRF 密钥 Ki
            String Ki = generatePRF(Ks, ni.index);

            // 生成 X 轴的标签 TAGXi
            String TAGXi = generatePRF(Kx, ni.index);

            // 对 X 轴的节点进行加密处理并存储到 Ux
            if (Sx.containsKey(ni.index)) {
                // 使用 BigInteger 处理label
                String ei = encrypt(Ki, new BigInteger(BTx[ni.index].label), C, "aes");
                Ux.put(TAGXi, ei);
            }

            // 更新进度条
            completedSteps++;
            printProgress(completedSteps, totalSteps);
        }

        // 处理 Y 轴 (BTy)
        for (int i = 0; i < BTy.length; i++) {
            Node ni = BTy[i];

            // 生成 PRF 密钥 Ki
            String Ki = generatePRF(Ks, ni.index);

            // 生成 Y 轴的标签 TAGYi
            String TAGYi = generatePRF(Ky, ni.index);

            // 对 Y 轴的节点进行加密处理并存储到 Uy
            if (Sy.containsKey(ni.index)) {
                // 使用 BigInteger 处理label
                String ei = encrypt(Ki, new BigInteger(BTy[ni.index].label), C_prime, "aes");
                Uy.put(TAGYi, ei);
            }

            // 更新进度条
            completedSteps++;
            printProgress(completedSteps, totalSteps);
        }
    }

    // 进度条打印函数
    private void printProgress(int completed, int total) {
        int progressWidth = 50; // 进度条的宽度
        int progress = (int) ((completed * 1.0 / total) * progressWidth);
        StringBuilder progressBar = new StringBuilder();

        progressBar.append("\r[");
        for (int i = 0; i < progressWidth; i++) {
            if (i < progress) {
                progressBar.append("#");
            } else {
                progressBar.append(" ");
            }
        }
        progressBar.append("] ");
        progressBar.append(String.format("%d%%", (completed * 100) / total));

        System.out.print(progressBar);

        // 打印完成换行
        if (completed == total) {
            System.out.println();
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

    public String clientSearch(int[] xRange, int[] yRange, int t) throws Exception {
        // 构建 X 轴的二叉树并找到覆盖查询范围的最小节点
//        Node[] BTx = buildBinaryTree(t);
        List<Node> xNodes = findMinimumCover(xRange, true);  // 找到最小覆盖节点

        // 构建 X 轴的搜索令牌
        List<String> TAGX = new ArrayList<>();
        for (Node ni : xNodes) {
            String TAGXi = generatePRF(Kx, ni.index);  // 使用 X 轴的密钥
            TAGX.add(TAGXi);
        }

        // 构建 Y 轴的二叉树并找到覆盖查询范围的最小节点
//        Node[] BTy = buildBinaryTree(t);
        List<Node> yNodes = findMinimumCover(yRange, false);  // 找到最小覆盖节点

        // 构建 Y 轴的搜索令牌
        List<String> TAGY = new ArrayList<>();
        for (Node ni : yNodes) {
            String TAGYi = generatePRF(Ky, ni.index);  // 使用 Y 轴的密钥
            TAGY.add(TAGYi);
        }

        List<List<String>> ER = serverSearch(TAGX, TAGY);  // ER[0] 是 ei, ER[1] 是 ei_prime

        // 初始化 Sx 和 Sy 为长度为 2^t 的全 0 字符串
        String Sx_result = "0".repeat((int) Math.pow(2, t));
        String Sy_result = "0".repeat((int) Math.pow(2, t));

        // 对 X 轴的每个节点进行解密，并使用 orStrings 累积结果
        for (int i = 0; i < xNodes.size(); i++) {
            List<String> Kx_dec = buildKey(xNodes.get(i), Ks);
            String decryptedX = decrypt(ER.get(0).get(i), C, Kx_dec.get(0), "aes");

            // 打印 ER.get(0).get(i)
//            System.out.print("Kx: " + Kx_dec.get(0));
//            System.out.print(" TAGX[" + i + "]: " + TAGX.get(i));
            //System.out.println(" Encrypted ER_X[" + i + "]: " + ER.get(0).get(i));
            // 打印 xNodes.get(i) 和 decryptedX
            //System.out.println("xNode[" + i + "]: " + xNodes.get(i));
            //System.out.println("Decrypted X: " + decryptedX);

            // 使用 orStrings 累积解密后的结果
            Sx_result = orStrings(Sx_result, decryptedX);
        }

        // 对 Y 轴的每个节点进行解密，并使用 orStrings 累积结果
        for (int i = 0; i < yNodes.size(); i++) {
            List<String> Ky_dec = buildKey(yNodes.get(i), Ks);
            String decryptedY = decrypt(ER.get(1).get(i), C_prime, Ky_dec.get(0), "aes");

            // 打印 yNodes.get(i) 和 decryptedY
//            System.out.print("Ky: " + Ky_dec.get(0));
//            System.out.print(" TAGY[" + i + "]: " + TAGY.get(i));
            //System.out.println(" Encrypted ER_Y[" + i + "]: " + ER.get(1).get(i));
            //System.out.println("yNode[" + i + "]: " + yNodes.get(i));
            //System.out.println("Decrypted Y: " + decryptedY);

            // 使用 orStrings 累积解密后的结果
            Sy_result = orStrings(Sy_result, decryptedY);
        }


        // 最后使用 andStrings 获取 X 轴和 Y 轴的交集
        return andStrings(Sx_result, Sy_result);  // 返回最终搜索结果
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
            } else {
                //System.out.println("TAGX (X-axis search token)不存在:");
            }
        }

        // 搜索 Y 轴的加密数据 (ei_prime)
        for (String tagY : TAGY) {
            String ei_prime = Uy.get(tagY);
            if (ei_prime != null) {
                ER_Y.add(ei_prime);
            } else {
                //System.out.println("TAGY (Y-axis search token)不存在:");
            }
        }

        List<List<String>> ER = new ArrayList<>();
        ER.add(ER_X);  // X 轴结果
        ER.add(ER_Y);  // Y 轴结果

        return ER;  // 返回加密结果集，分别为 ei 和 ei_prime
    }

    // Update-x 方法实现
    public List<String> updateX(int[] Pi, int[] Pi_prime, boolean isX) throws Exception {
        int[] xCoordinates = this.xCoordinates;
        int[] yCoordinates = this.yCoordinates;
        // 输出 LUx
        List<String> LUx = new ArrayList<>();
        // 构建树
//        Node[] BTx_temp = isX?BTx:BTy;
        // 查找需要更新的叶子节点
        List<Node> LNx = new ArrayList<>(); // Find leaf nodes

        //find nodes to be updated
        List<Integer> nodesToUpdate = new ArrayList<>();
        // 判断 Pi 是否存在于 xCoordinates 和 yCoordinates 中
        boolean isInX = false;
        boolean isInY = false;
        // 创建新的坐标数组
        int[] newXCoordinates = Arrays.copyOf(xCoordinates, xCoordinates.length);
        int[] newYCoordinates = Arrays.copyOf(yCoordinates, yCoordinates.length);
        // 遍历查找 Pi 在 xCoordinates 和 yCoordinates 中的位置
        int indexToReplace = -1;
        for (int i = 0; i < xCoordinates.length; i++) {
            if (xCoordinates[i] == Pi[0] && yCoordinates[i] == Pi[1]) {
                // 找到 Pi 在 xCoordinates 和 yCoordinates 中的位置
                isInX = true;
                isInY = true;
                indexToReplace = i;
                // 移除 Pi 并添加 P_prime 到新的坐标数组
                newXCoordinates[i] = Pi_prime[0];
                newYCoordinates[i] = Pi_prime[1];
                break;  // 找到后立即跳出循环
            }
        }
        // 如果 Pi 不存在于 xCoordinates 或 yCoordinates 中，直接返回空列表
        if (!isInX || !isInY || indexToReplace == -1) {
            //System.out.println("Point Pi not found in xCoordinates or yCoordinates. No update necessary.");
            return null;
        }
        // 输出新的 xCoordinates 和 yCoordinates
//        //System.out.println("New xCoordinates: " + Arrays.toString(newXCoordinates));
//        //System.out.println("New yCoordinates: " + Arrays.toString(newYCoordinates));

        // 初始化 Sux 为全零串
        StringBuilder Sux_builder = new StringBuilder("0".repeat(N));
        if (isX) {
            // 找到 Pi[0] 和 P_prime[0] 对应的叶子节点
            int leafStartIndex = (int) Math.pow(2, t) - 1;
            int leafNodeIndex1 = leafStartIndex + Pi[0];//注意这是x坐标
            int leafNodeIndex2 = leafStartIndex + Pi_prime[0];//注意这是x坐标
            LNx.add(BTx[leafNodeIndex1]);
            LNx.add(BTx[leafNodeIndex2]);
            Sux_builder.setCharAt(indexToReplace, '1');
            String Sux = Sux_builder.toString();
            for (Node nAlpha : LNx) {
                // 使用 orStrings 更新位串
                //System.out.println("Before XOR - nAlpha.label: " + nAlpha.label + ", Sux: " + Sux);
                nAlpha.label = xorStrings(Sux, nAlpha.label);
                //System.out.println("After XOR - nAlpha.label: " + nAlpha.label);

                // 生成标签
                String TAGXAlpha = generatePRF(Kx, nAlpha.index);
                //System.out.println("Generated TAGXAlpha: " + TAGXAlpha + ", for nAlpha.index: " + nAlpha.index);

                // 生成加密密钥并加密
                String KAlpha = generatePRF(Ks, nAlpha.index);
                //System.out.println("Generated KAlpha: " + KAlpha + ", for nAlpha.index: " + nAlpha.index);

                // 进行加密
                String eU = encrypt(KAlpha, new BigInteger(BTx[nAlpha.index].label), C, "aes");
                //System.out.println("BTx[" + nAlpha.index + "].label: " + BTx[nAlpha.index].label + ", eUx: " + eU + ", C: " + C);

                // 将更新的标签和密文存入 LUx
                LUx.add(TAGXAlpha + "," + eU);
                // 更新父节点
                int beta = nAlpha.index;
                while (beta != 0) {
                    // 判断节点是左子还是右子
                    if (beta % 2 == 0) {
                        beta = beta / 2 - 1;  // 左子节点
                        BTx[beta].label = orStrings(BTx[nAlpha.index].label, BTx[nAlpha.index - 1].label);
                    } else {
                        beta = (beta - 1) / 2;  // 右子节点
                        BTx[beta].label = orStrings(BTx[nAlpha.index].label, BTx[nAlpha.index + 1].label);
                    }
                    // 更新父节点
                    String TAGXBeta = generatePRF(Kx, beta);
                    String KBeta = generatePRF(Ks, beta);
                    String eUBeta = encrypt(KBeta, new BigInteger(BTx[beta].label), C, "aes");
                    // 添加到 LUx
                    LUx.add(TAGXBeta + "," + eUBeta);
                }
            }
        } else {
            // 找到 Pi[1] 和 P_prime[1] 对应的叶子节点
            int leafStartIndex = (int) Math.pow(2, t) - 1;
            int leafNodeIndex1 = leafStartIndex + Pi[1];//注意这是y坐标
            int leafNodeIndex2 = leafStartIndex + Pi_prime[1];//注意这是y坐标
            LNx.add(BTy[leafNodeIndex1]);
            LNx.add(BTy[leafNodeIndex2]);
            Sux_builder.setCharAt(indexToReplace, '1');
            String Sux = Sux_builder.toString();
            for (Node nAlpha : LNx) {
                // 使用 orStrings 更新位串
                //System.out.println("Before XOR - nAlpha.label: " + nAlpha.label + ", Sux: " + Sux);
                nAlpha.label = xorStrings(Sux, nAlpha.label);
                //System.out.println("After XOR - nAlpha.label: " + nAlpha.label);

                // 生成标签
                String TAGXAlpha = generatePRF(Ky, nAlpha.index);
                //System.out.println("Generated TAGXAlpha: " + TAGXAlpha + ", for nAlpha.index: " + nAlpha.index);

                // 生成加密密钥并加密
                String KAlpha = generatePRF(Ks, nAlpha.index);
                //System.out.println("Generated KAlpha: " + KAlpha + ", for nAlpha.index: " + nAlpha.index);

                // 进行加密
                String eU = encrypt(KAlpha, new BigInteger(BTy[nAlpha.index].label), C_prime, "aes");
                //System.out.println("BTy[" + nAlpha.index + "].label: " + BTy[nAlpha.index].label + ", eUy: " + eU + ", C_prime: " + C_prime);
                // 将更新的标签和密文存入 LUx
                LUx.add(TAGXAlpha + "," + eU);
                // 更新父节点
                int beta = nAlpha.index;
                while (beta != 0) {
                    // 判断节点是左子还是右子
                    if (beta % 2 == 0) {
                        beta = (beta - 1) / 2;  // 左子节点
                        BTy[beta].label = orStrings(BTy[nAlpha.index].label, BTy[nAlpha.index - 1].label);
                    } else {
                        beta = (beta - 1) / 2;  // 右子节点
                        BTy[beta].label = orStrings(BTy[nAlpha.index].label, BTy[nAlpha.index + 1].label);
                    }
                    // 更新父节点
                    String TAGXBeta = generatePRF(Kx, beta);
                    String KBeta = generatePRF(Ks, beta);
                    String eUBeta = encrypt(KBeta, new BigInteger(BTy[beta].label), C_prime, "aes");
                    // 添加到 LUx
                    LUx.add(TAGXBeta + "," + eUBeta);
                }
            }
            this.xCoordinates = newXCoordinates;
            this.yCoordinates = newYCoordinates;
        }
        return LUx;  // 返回 LUx
    }

    // Client Update 方法
    public List<List<String>> clientUpdate(int[] Pi, int[] Pi_prime) throws Exception {
        List<List<String>> updates = new ArrayList<>();

        // 更新X维度
        if (Pi[0] != Pi_prime[0]) {
            C += 1;
            List<String> LUx = updateX(Pi, Pi_prime, true);
            updates.add(LUx);  // 将LUx添加到更新列表
        } else {
            updates.add(new ArrayList<>());  // 空列表作为占位符
        }

        // 更新Y维度
        if (Pi[1] != Pi_prime[1]) {
            C_prime += 1;
            List<String> LUy = updateX(Pi, Pi_prime, false);  // Y 更新与 X 类似，使用 Ky 密钥
            updates.add(LUy);  // 将LUy添加到更新列表
        } else {
            updates.add(new ArrayList<>());  // 空列表作为占位符
        }

        return updates;  // 返回 LUx 和 LUy
    }

    // Server Update 方法
    public void serverUpdate(List<List<String>> updates) {
        // 从列表中获取LUx和LUy
        List<String> LUx = updates.get(0);  // 第一个列表是LUx
        List<String> LUy = updates.get(1);  // 第二个列表是LUy

        // 更新 Ux
        for (String update : LUx) {
            String[] parts = update.split(",");
            String tag = parts[0];  // TAGX
            String eU = parts[1];  // 加密后的新值

            if (Ux.containsKey(tag)) {
                //System.out.println("替换 Ux 中的键: " + tag + " 值:" + eU);
            } else {
                //System.out.println("添加到 Ux: " + tag);
            }
            Ux.put(tag, eU);  // 更新或添加到 Ux
        }

        // 更新 Uy
        for (String update : LUy) {
            String[] parts = update.split(",");
            String tag = parts[0];  // TAGY
            String eU = parts[1];  // 加密后的新值

            if (Uy.containsKey(tag)) {
                //System.out.println("替换 Uy 中的键: " + tag + " 值:" + eU);
            } else {
                //System.out.println("添加到 Uy: " + tag);
            }
            Uy.put(tag, eU);  // 更新或添加到 Uy
        }
    }


    public static void main(String[] args) throws Exception {
//        // 参数设置
//        int lambda = 128;  // 安全参数
//        int t = 3;  // 树的高度 - 例如有8个叶子节点（2^3 = 8）
//        int[] xCoordinates = {1, 1, 6, 4, 7, 4, 2, 5};  // x轴的坐标
//        int[] yCoordinates = {1, 3, 2, 4, 4, 5, 7, 7};  // y轴的坐标
//        int N = 8;  // 数据点数量
//
//        // 1. 实例化ConstructionOne类
//        double startTime = System.nanoTime();
//        ConstructionOne construction = new ConstructionOne(lambda, t, N, xCoordinates, yCoordinates);
//        double endTime = System.nanoTime();
//        System.out.printf("实例化 ConstructionOne 类时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        // 2. 生成二叉树节点数组
//        startTime = System.nanoTime();
//        construction.BTx = construction.buildBinaryTree(t);
//        construction.BTy = construction.buildBinaryTree(t);
//        endTime = System.nanoTime();
//        System.out.printf("生成二叉树节点数组时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        // 3. 生成倒排索引
//        startTime = System.nanoTime();
//        List<String> invertedIndex = construction.buildInvertedIndex(t, N, xCoordinates);
//        List<String> invertedIndey = construction.buildInvertedIndex(t, N, yCoordinates);
//        endTime = System.nanoTime();
//        System.out.printf("生成倒排索引时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        // 4. 构建节点倒排索引
//        startTime = System.nanoTime();
//        Map<Integer, String> Sx = construction.buildxNodeInvertedIndex(invertedIndex, t);  // X轴节点倒排索引
//        Map<Integer, String> Sy = construction.buildyNodeInvertedIndex(invertedIndey, t); // Y轴节点倒排索引
//        endTime = System.nanoTime();
//        System.out.printf("构建节点倒排索引时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        // 5. 设置加密数据集 (EDS)
//        startTime = System.nanoTime();
//        construction.setupEDS(Sx, Sy);
//        endTime = System.nanoTime();
//        System.out.printf("设置加密数据集时间 (setupEDS): %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        // 6. 转换查询范围
//        startTime = System.nanoTime();
//        int[] rangex = construction.rangeConvert(t, new int[]{2, 6});
//        int[] rangey = construction.rangeConvert(t, new int[]{3, 6});
//        endTime = System.nanoTime();
//        System.out.printf("转换查询范围时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        // 7. 查找最小覆盖节点
//        startTime = System.nanoTime();
//        List<Node> xnodesCovered = construction.findMinimumCover(rangex, true);
//        List<Node> ynodesCovered = construction.findMinimumCover(rangey, false);
//        endTime = System.nanoTime();
//        System.out.printf("查找最小覆盖节点时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        // 打印二叉树
//        startTime = System.nanoTime();
//        for (Node node : construction.BTx) {
//            //System.out.println("Index: " + node.index + ", xLabel: " + node.label);
//        }
//        for (Node node : construction.BTy) {
//            //System.out.println("Index: " + node.index + ", yLabel: " + node.label);
//        }
//        endTime = System.nanoTime();
//        System.out.printf("打印二叉树时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        // 8. 调用clientSearch进行搜索
//        startTime = System.nanoTime();
//        String searchResult = construction.clientSearch(rangex, rangey, t);
//        endTime = System.nanoTime();
//        System.out.printf("ClientSearch 时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        // 9. 打印搜索结果
//        startTime = System.nanoTime();
//        construction.printBinaryWithIndexes(searchResult);
//        construction.printU();
//        endTime = System.nanoTime();
//        System.out.printf("打印搜索结果时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        // 10. 更新数据并再次进行搜索
//        int[] Pi = new int[]{1, 3};
//        int[] Pi_prime = new int[]{2, 6};
//
//        startTime = System.nanoTime();
//        Map<String, List<String>> updates = construction.clientUpdate(Pi, Pi_prime);
//        endTime = System.nanoTime();
//        System.out.printf("ClientUpdate 时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        startTime = System.nanoTime();
//        construction.serverUpdate(updates);
//        endTime = System.nanoTime();
//        System.out.printf("ServerUpdate 时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        // 11. 再次进行搜索
//        startTime = System.nanoTime();
//        String searchResult1 = construction.clientSearch(rangex, rangey, t);
//        endTime = System.nanoTime();
//        System.out.printf("第二次 ClientSearch 时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
//
//        startTime = System.nanoTime();
//        construction.printBinaryWithIndexes(searchResult1);
//        endTime = System.nanoTime();
//        System.out.printf("第二次打印搜索结果时间: %.5f ms%n", (endTime - startTime) / 1_000_000.0);
        experiment_setup_cost();
        experiment_search_complexity_dimension();
        experiment_search_complexity_data_points();
        experiment_update_complexity_dimension();
//        experiment_update_complexity_update_points();
    }

    public static void experiment_setup_cost() throws Exception {
        int lambda = 128;
        int t = 15;  // 设置最大维度大小
        int N = 2000;  // 设置数据点数量

        // 随机生成大规模的x和y坐标
        int[] xCoordinates = generateRandomCoordinates(N, t);
        int[] yCoordinates = generateRandomCoordinates(N, t);

        long startTime = System.nanoTime();

        // 初始化构建
        ConstructionOne construction = new ConstructionOne(lambda, t, N, xCoordinates, yCoordinates);

        // 生成二叉树节点数组
        construction.BTx = construction.buildBinaryTree(t);
        construction.BTy = construction.buildBinaryTree(t);

        // 生成倒排索引
        List<String> invertedIndexX = construction.buildInvertedIndex(t, N, xCoordinates);
        List<String> invertedIndexY = construction.buildInvertedIndex(t, N, yCoordinates);

        // 生成节点倒排索引
        Map<Integer, String> Sx = construction.buildxNodeInvertedIndex(invertedIndexX, t);
        Map<Integer, String> Sy = construction.buildyNodeInvertedIndex(invertedIndexY, t);

        // 设置加密数据集
        construction.setupEDS(Sx, Sy);

        long endTime = System.nanoTime();
        double elapsedTime = (endTime - startTime) / 1e6; // 转换为毫秒
        System.out.printf("Setup cost: %.5f ms%n", elapsedTime);
    }
    public static void experiment_search_complexity_dimension() throws Exception {
        int lambda = 128;
        int N = 20000;

        for (int t = 7; t <= 7; t++) {
            int[] xCoordinates = generateRandomCoordinates(N, t);
            int[] yCoordinates = generateRandomCoordinates(N, t);

            ConstructionOne construction = new ConstructionOne(lambda, t, N, xCoordinates, yCoordinates);

            // 初始化二叉树和倒排索引
            construction.BTx = construction.buildBinaryTree(t);
            construction.BTy = construction.buildBinaryTree(t);

            List<String> invertedIndexX = construction.buildInvertedIndex(t, N, xCoordinates);
            List<String> invertedIndexY = construction.buildInvertedIndex(t, N, yCoordinates);
            Map<Integer, String> Sx = construction.buildxNodeInvertedIndex(invertedIndexX, t);
            Map<Integer, String> Sy = construction.buildyNodeInvertedIndex(invertedIndexY, t);
            construction.setupEDS(Sx, Sy);

            // 搜索范围
            int[] rangex = construction.rangeConvert(t, new int[]{2, 6});
            int[] rangey = construction.rangeConvert(t, new int[]{3, 6});

            // 测量搜索时间
            long startTime = System.nanoTime();
            String result = construction.clientSearch(rangex, rangey, t);
            long endTime = System.nanoTime();

            double elapsedTime = (endTime - startTime) / 1e6; // 转换为毫秒
            System.out.printf("Search time for dimension D=2^%d: %.5f ms%n", t, elapsedTime);
        }
    }
    public static void experiment_search_complexity_data_points() throws Exception {
        int lambda = 128;
        int t = 15;  // 维度大小

        for (int N = 400; N <= 400; N += 2000) {
            int[] xCoordinates = generateRandomCoordinates(N, t);
            int[] yCoordinates = generateRandomCoordinates(N, t);

            ConstructionOne construction = new ConstructionOne(lambda, t, N, xCoordinates, yCoordinates);

            // 初始化二叉树和倒排索引
            construction.BTx = construction.buildBinaryTree(t);
            construction.BTy = construction.buildBinaryTree(t);

            List<String> invertedIndexX = construction.buildInvertedIndex(t, N, xCoordinates);
            List<String> invertedIndexY = construction.buildInvertedIndex(t, N, yCoordinates);
            Map<Integer, String> Sx = construction.buildxNodeInvertedIndex(invertedIndexX, t);
            Map<Integer, String> Sy = construction.buildyNodeInvertedIndex(invertedIndexY, t);
            construction.setupEDS(Sx, Sy);

            // 搜索范围
            int[] rangex = construction.rangeConvert(t, new int[]{2, 6});
            int[] rangey = construction.rangeConvert(t, new int[]{3, 6});

            // 测量搜索时间
            long startTime = System.nanoTime();
            String result = construction.clientSearch(rangex, rangey, t);
            long endTime = System.nanoTime();

            double elapsedTime = (endTime - startTime) / 1e6; // 转换为毫秒
            System.out.printf("Search time for N=%d: %.5f ms%n", N, elapsedTime);
        }
    }
    public static void experiment_update_complexity_dimension() throws Exception {
        int lambda = 128;
        int N = 20000;  // 数据点数量
        int[] Pi = {1, 3};
        int[] Pi_prime = {2, 6};

        for (int t = 7; t <= 7; t++) {
            int[] xCoordinates = generateRandomCoordinates(N, t);
            int[] yCoordinates = generateRandomCoordinates(N, t);

            ConstructionOne construction = new ConstructionOne(lambda, t, N, xCoordinates, yCoordinates);

            // 初始化二叉树和倒排索引
            construction.BTx = construction.buildBinaryTree(t);
            construction.BTy = construction.buildBinaryTree(t);

            List<String> invertedIndexX = construction.buildInvertedIndex(t, N, xCoordinates);
            List<String> invertedIndexY = construction.buildInvertedIndex(t, N, yCoordinates);
            Map<Integer, String> Sx = construction.buildxNodeInvertedIndex(invertedIndexX, t);
            Map<Integer, String> Sy = construction.buildyNodeInvertedIndex(invertedIndexY, t);
            construction.setupEDS(Sx, Sy);

            // 测量更新时间
            long startTime = System.nanoTime();
            List<List<String>> updates = construction.clientUpdate(Pi, Pi_prime);
            construction.serverUpdate(updates);
            long endTime = System.nanoTime();

            double elapsedTime = (endTime - startTime) / 1e6; // 转换为毫秒
            System.out.printf("Update time for dimension D=2^%d: %.5f ms%n", t, elapsedTime);
        }
    }
    public static void experiment_update_complexity_update_points() throws Exception {
        int lambda = 128;
        int t = 15;  // 维度大小
        int N = 2000;  // 数据点数量

        for (int numUpdates = 10; numUpdates <= 100; numUpdates += 10) {
            int[] xCoordinates = generateRandomCoordinates(N, t);
            int[] yCoordinates = generateRandomCoordinates(N, t);

            ConstructionOne construction = new ConstructionOne(lambda, t, N, xCoordinates, yCoordinates);

            // 初始化二叉树和倒排索引
            construction.BTx = construction.buildBinaryTree(t);
            construction.BTy = construction.buildBinaryTree(t);

            List<String> invertedIndexX = construction.buildInvertedIndex(t, N, xCoordinates);
            List<String> invertedIndexY = construction.buildInvertedIndex(t, N, yCoordinates);
            Map<Integer, String> Sx = construction.buildxNodeInvertedIndex(invertedIndexX, t);
            Map<Integer, String> Sy = construction.buildyNodeInvertedIndex(invertedIndexY, t);
            construction.setupEDS(Sx, Sy);

            // 随机生成Pi和Pi_prime的多个更新点
            for (int i = 0; i < numUpdates; i++) {
                int[] Pi = generateRandomCoordinatePair(t);
                int[] Pi_prime = generateRandomCoordinatePair(t);

                long startTime = System.nanoTime();
                List<List<String>> updates = construction.clientUpdate(Pi, Pi_prime);
                construction.serverUpdate(updates);
                long endTime = System.nanoTime();

                double elapsedTime = (endTime - startTime) / 1e6; // 转换为毫秒
                System.out.printf("Update time for %d updates: %.5f ms%n", numUpdates, elapsedTime);
            }
        }
    }

    // 生成指定数量的随机坐标，坐标值的范围为 [0, 2^t - 1]
    public static int[] generateRandomCoordinates(int N, int t) {
        Random random = new Random();
        int maxValue = (int) Math.pow(2, t);  // 最大值为 2^t
        int[] coordinates = new int[N];

        // 为每个坐标生成随机值
        for (int i = 0; i < N; i++) {
            coordinates[i] = random.nextInt(maxValue);  // 在 [0, maxValue) 之间生成随机值
        }
        return coordinates;
    }
    // 生成一个随机的坐标对，x 和 y 坐标的范围为 [0, 2^t - 1]
    public static int[] generateRandomCoordinatePair(int t) {
        Random random = new Random();
        int maxValue = (int) Math.pow(2, t);  // 最大值为 2^t
        int[] coordinatePair = new int[2];

        // 生成随机的 x 坐标
        coordinatePair[0] = random.nextInt(maxValue);  // x 在 [0, maxValue) 之间生成随机值

        // 生成随机的 y 坐标
        coordinatePair[1] = random.nextInt(maxValue);  // y 在 [0, maxValue) 之间生成随机值

        return coordinatePair;
    }


    // 打印解密结果并找到每个为1的位的下标
    public void printBinaryWithIndexes(String binaryResult) {
        //System.out.println("Client Search Results: " + binaryResult);
        System.out.print("Indexes of points: ");

        // 遍历二进制字符串，找到每个为1的位的下标
        for (int i = 0; i < binaryResult.length(); i++) {
            if (binaryResult.charAt(i) == '1') {
                // 打印1所在的位置（下标从1开始）
                System.out.print((i + 1) + " ");
            }
        }
        //System.out.println();  // 换行
    }
    public void printU() {
        //System.out.println("Ux contains the following key-value pairs:");
        for (Map.Entry<String, String> entry : Ux.entrySet()) {
            //System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
        //System.out.println("Uy contains the following key-value pairs:");
        for (Map.Entry<String, String> entry : Uy.entrySet()) {
            //System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
    }
}



