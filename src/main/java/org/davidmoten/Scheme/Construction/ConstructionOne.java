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

        // 将解密后的字符串填充到2^t的长度
        int requiredLength = (int) Math.pow(2, t);
        return String.format("%0" + requiredLength + "d", new BigInteger(decrypted));
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
    public Map<Integer, String> buildNodeInvertedIndex(Node[] BT, List<String> invertedIndex, int t) {

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

    private String xorStrings(String s1, String s2) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s1.length(); i++) {
            // 按位异或操作，将两个位串的对应位进行异或运算
            result.append((s1.charAt(i) == s2.charAt(i)) ? '0' : '1');
        }
        return result.toString();
    }

    // 修改后的查找最小覆盖节点的方法
    private List<Node> findMinimumCover(int[] range, Node[] BT) {
        List<Node> nodesInRange = new ArrayList<>();

        // 初次调用递归函数
        findMinimumCoverRecursive(range[0], range[1], BT, nodesInRange);

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

    private int[] rangeConvert(int t, int[] R) {
        return new int[]{(int) Math.pow(2, t) - 1 + R[0], (int) Math.pow(2, t) - 1 + R[1]};
    }


    // setupEDS 方法实现
    public void setupEDS(Map<Integer, String> Sx, Map<Integer, String> Sy) throws Exception {
        // 遍历二叉树的每个节点
        // 处理 X 轴 (BTx)
        for (int i = 0; i < BTx.length; i++) {
            // 从二叉树中检索节点
            Node ni = BTx[i];

            // 生成 PRF 密钥 Ki
            String Ki = generatePRF(Ks, ni.index);

            // 生成 X 轴的标签 TAGXi
            String TAGXi = generatePRF(Kx, ni.index);  // X 轴的标签
            System.out.print("K" + i + ":" + Ki + ":");

            // 对 X 轴的节点进行加密处理并存储到 Ux
            if (Sx.containsKey(ni.index)) {
                // 打印 BTx[ni.index].label 和 ni.index
                System.out.println("X-Axis Node Index: " + ni.index + ", Label: " + BTx[ni.index].label);

                String ei = encrypt(Ki, Integer.parseInt(BTx[ni.index].label), C, "homomorphic");  // 加密
                Ux.put(TAGXi, ei);  // 存储加密后的数据到 Ux
            }
        }

        // 处理 Y 轴 (BTy)
        for (int i = 0; i < BTy.length; i++) {
            // 从二叉树中检索节点
            Node ni = BTy[i];

            // 生成 PRF 密钥 Ki
            String Ki = generatePRF(Ks, ni.index);

            // 生成 Y 轴的标签 TAGYi
            String TAGYi = generatePRF(Ky, ni.index);  // Y 轴的标签
            System.out.print("K" + i + ":" + Ki + ":");

            // 对 Y 轴的节点进行加密处理并存储到 Uy
            if (Sy.containsKey(ni.index)) {
                // 打印 BTy[ni.index].label 和 ni.index
                System.out.println("Y-Axis Node Index: " + ni.index + ", Label: " + BTy[ni.index].label);

                String ei = encrypt(Ki, Integer.parseInt(BTy[ni.index].label), C_prime, "homomorphic");  // 加密
                Uy.put(TAGYi, ei);  // 存储加密后的数据到 Uy
            }
        }

        System.out.println();
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
        List<Node> xNodes = findMinimumCover(xRange, BTx);  // 找到最小覆盖节点

        // 构建 X 轴的搜索令牌
        List<String> TAGX = new ArrayList<>();
        for (Node ni : xNodes) {
            String TAGXi = generatePRF(Kx, ni.index);  // 使用 X 轴的密钥
            TAGX.add(TAGXi);
        }

        // 构建 Y 轴的二叉树并找到覆盖查询范围的最小节点
//        Node[] BTy = buildBinaryTree(t);
        List<Node> yNodes = findMinimumCover(yRange, BTy);  // 找到最小覆盖节点

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
            String decryptedX = decrypt(ER.get(0).get(i), C, Kx_dec.get(0), "homomorphic");

            // 打印 xNodes.get(i) 和 decryptedX
            System.out.println("xNode[" + i + "]: " + xNodes.get(i));
            System.out.println("Decrypted X: " + decryptedX);

            // 使用 orStrings 累积解密后的结果
            Sx_result = orStrings(Sx_result, decryptedX);
        }

        // 对 Y 轴的每个节点进行解密，并使用 orStrings 累积结果
        for (int i = 0; i < yNodes.size(); i++) {
            List<String> Ky_dec = buildKey(yNodes.get(i), Ks);
            String decryptedY = decrypt(ER.get(1).get(i), C_prime, Ky_dec.get(0), "homomorphic");

            // 打印 yNodes.get(i) 和 decryptedY
            System.out.println("yNode[" + i + "]: " + yNodes.get(i));
            System.out.println("Decrypted Y: " + decryptedY);

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
                System.out.println("TAGX (X-axis search token)不存在:");
            }
        }

        // 搜索 Y 轴的加密数据 (ei_prime)
        for (String tagY : TAGY) {
            String ei_prime = Uy.get(tagY);
            if (ei_prime != null) {
                ER_Y.add(ei_prime);
            } else {
                System.out.println("TAGY (Y-axis search token)不存在:");
            }
        }

        List<List<String>> ER = new ArrayList<>();
        ER.add(ER_X);  // X 轴结果
        ER.add(ER_Y);  // Y 轴结果

        return ER;  // 返回加密结果集，分别为 ei 和 ei_prime
    }

    // Update-x 方法实现
    public List<String> updateX(int Ks, int Kx, int t, int[] Pi, int[] Pi_prime, boolean isX) throws Exception {
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
            System.out.println("Point Pi not found in xCoordinates or yCoordinates. No update necessary.");
            return null;
        }
        // 输出新的 xCoordinates 和 yCoordinates
//        System.out.println("New xCoordinates: " + Arrays.toString(newXCoordinates));
//        System.out.println("New yCoordinates: " + Arrays.toString(newYCoordinates));
        // 找到 Pi[0] 和 P_prime[0] 对应的叶子节点
        int leafStartIndex = (int) Math.pow(2, t) - 1;
        int leafNodeIndex1 = leafStartIndex + Pi[0];
        nodesToUpdate.add(leafNodeIndex1);
        int leafNodeIndex2 = leafStartIndex + Pi_prime[0];
        nodesToUpdate.add(leafNodeIndex2);
        // 初始化 Sux 为全零串
        StringBuilder Sux_builder = new StringBuilder("0".repeat((int) Math.pow(2, t)));
        Sux_builder.setCharAt(indexToReplace, '1');
        String Sux = Sux_builder.toString();

        if (isX) {
            for (int i : nodesToUpdate) {
                LNx.add(BTx[i]);
            }
            for (Node nAlpha : LNx) {
                // 使用 orStrings 更新位串
                nAlpha.label = xorStrings(Sux, nAlpha.label);
                // 生成标签
                String TAGXAlpha = generatePRF(Kx, nAlpha.index);
                // 生成加密密钥并加密
                String KAlpha = generatePRF(Ks, nAlpha.index);
                String eU = encrypt(KAlpha, Integer.parseInt(Sux), C, "homomorphic");
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
                    String eUBeta = encrypt(KBeta, Integer.parseInt(BTx[beta].label), C, "homomorphic");
                    // 添加到 LUx
                    LUx.add(TAGXBeta + "," + eUBeta);
                }
            }
        } else {
            for (int i : nodesToUpdate) {
                LNx.add(BTy[i]);
            }
            for (Node nAlpha : LNx) {
                // 使用 orStrings 更新位串
                nAlpha.label = xorStrings(Sux, nAlpha.label);
                // 生成标签
                String TAGXAlpha = generatePRF(Kx, nAlpha.index);
                // 生成加密密钥并加密
                String KAlpha = generatePRF(Ks, nAlpha.index);
                String eU = encrypt(KAlpha, Integer.parseInt(Sux), C_prime, "homomorphic");
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
                    String eUBeta = encrypt(KBeta, Integer.parseInt(BTy[beta].label), C_prime, "homomorphic");
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
    public Map<String, List<String>> clientUpdate(int[] Pi, int[] Pi_prime) throws Exception {
        Map<String, List<String>> updates = new HashMap<>();

        // 更新 X 维度
        if (Pi[0] != Pi_prime[0]) {
            C += 1;
            List<String> LUx = updateX(Ks, Kx, t, Pi, Pi_prime, true);
            updates.put("LUx", LUx);
        }

        // 更新 Y 维度
        if (Pi[1] != Pi_prime[1]) {
            C_prime += 1;
            List<String> LUy = updateX(Ks, Ky, t, Pi, Pi_prime, false);  // Y 更新与 X 类似，使用 Ky 密钥
            updates.put("LUy", LUy);
        }

        return updates;  // 返回 LUx 和 LUy
    }

    // Server Update 方法
    public void serverUpdate(Map<String, List<String>> updates) {
        List<String> LUx = updates.get("LUx");
        List<String> LUy = updates.get("LUy");

        // 更新 Ux
        for (String update : LUx) {
            String[] parts = update.split(",");
            String tag = parts[0];  // TAGX
            String eU = parts[1];  // 加密后的新值

            if (Ux.containsKey(tag)) {
                System.out.println("替换 Ux 中的键: " + tag);
            } else {
                System.out.println("添加到 Ux: " + tag);
            }
            Ux.put(tag, eU);  // 更新或添加到 Ux
        }

        // 更新 Uy
        for (String update : LUy) {
            String[] parts = update.split(",");
            String tag = parts[0];  // TAGY
            String eU = parts[1];  // 加密后的新值

            if (Uy.containsKey(tag)) {
                System.out.println("替换 Uy 中的键: " + tag);
            } else {
                System.out.println("添加到 Uy: " + tag);
            }
            Uy.put(tag, eU);  // 更新或添加到 Uy
        }

    }

    public static void main(String[] args) throws Exception {
        // 实例化ConstructionOne类
        // 设置加密数据集 (EDS)
        int lambda = 128;  // 安全参数
        int t = 3;  // 树的高度 - 例如有8个叶子节点（2^3 = 8）
        // 假设我们有8个数据点的x和y坐标，生成倒排索引
        int[] xCoordinates = {1, 1, 6, 4, 7, 4, 2, 5};  // x轴的坐标
        int[] yCoordinates = {1, 3, 2, 4, 4, 5, 7, 7};  // y轴的坐标
        int N = 8;  // 数据点数量
        ConstructionOne construction = new ConstructionOne(128, t, N, xCoordinates, yCoordinates);


        // 1. 调用buildBinaryTree生成二叉树节点数组
        construction.BTx = construction.buildBinaryTree(t);
        construction.BTy = construction.buildBinaryTree(t);
        // 2. 调用buildInvertedIndex生成倒排索引
        List<String> invertedIndex = construction.buildInvertedIndex(t, N, xCoordinates);
        List<String> invertedIndey = construction.buildInvertedIndex(t, N, yCoordinates);
        // 构建节点倒排索引
        Map<Integer, String> Sx = construction.buildNodeInvertedIndex(construction.BTx, invertedIndex, t);  // X轴节点倒排索引
        Map<Integer, String> Sy = construction.buildNodeInvertedIndex(construction.BTy, invertedIndey, t); // Y轴节点倒排索引

//        for (Node node : BTx) {
//            System.out.println("Index: " + node.index + ", xLabel: " + node.label);
//        }
//        for (Node node : BTy) {
//            System.out.println("Index: " + node.index + ", yLabel: " + node.label);
//        }
        construction.setupEDS(Sx, Sy);

        int[] rangex = construction.rangeConvert(3, new int[]{2, 6});
        ;  // 查询范围
        int[] rangey = construction.rangeConvert(3, new int[]{3, 6});
        ;  // 查询范围
        List<Node> xnodesCovered = construction.findMinimumCover(rangex, construction.BTx);
        List<Node> ynodesCovered = construction.findMinimumCover(rangey, construction.BTy);

        // 打印结果
        for (Node node : xnodesCovered) {
            System.out.println("Index: " + node.index + ", xLabel: " + node.label);
        }
        for (Node node : ynodesCovered) {
            System.out.println("Index: " + node.index + ", yLabel: " + node.label);
        }
        // 调用clientSearch进行搜索
        String searchResult = construction.clientSearch(rangex, rangey, t);
        // 打印clientSearch返回的结果
        construction.printBinaryWithIndexes(searchResult);
        int[] Pi = new int[]{1, 3};
        int[] Pi_prime = new int[]{2, 6};
        Map<String, List<String>> updates = construction.clientUpdate(Pi, Pi_prime);
        construction.serverUpdate(updates);
        // 调用clientSearch进行搜索
        searchResult = construction.clientSearch(rangex, rangey, t);
        construction.printBinaryWithIndexes(searchResult);
    }

    // 打印解密结果并找到每个为1的位的下标
    public void printBinaryWithIndexes(String binaryResult) {
        System.out.println("Client Search Results: " + binaryResult);
        System.out.print("Indexes of points: ");

        // 遍历二进制字符串，找到每个为1的位的下标
        for (int i = 0; i < binaryResult.length(); i++) {
            if (binaryResult.charAt(i) == '1') {
                // 打印1所在的位置（下标从1开始）
                System.out.print((i + 1) + " ");
            }
        }
        System.out.println();  // 换行
    }

//    private List<Integer> findLeafNodesTobeUpdated(int[] Pi, int[] P_prime, int[] xCoordinates, int[] yCoordinates) {
//        List<Integer> nodesToUpdate = new ArrayList<>();
//
//        // 判断 Pi 是否存在于 xCoordinates 和 yCoordinates 中
//        boolean isInX = false;
//        boolean isInY = false;
//
//        // 创建新的坐标数组
//        int[] newXCoordinates = Arrays.copyOf(xCoordinates, xCoordinates.length);
//        int[] newYCoordinates = Arrays.copyOf(yCoordinates, yCoordinates.length);
//
//        // 遍历查找 Pi 在 xCoordinates 和 yCoordinates 中的位置
//        int indexToReplace = -1;
//        for (int i = 0; i < xCoordinates.length; i++) {
//            if (xCoordinates[i] == Pi[0] && yCoordinates[i] == Pi[1]) {
//                // 找到 Pi 在 xCoordinates 和 yCoordinates 中的位置
//                isInX = true;
//                isInY = true;
//                indexToReplace = i;
//
//                // 移除 Pi 并添加 P_prime 到新的坐标数组
//                newXCoordinates[i] = P_prime[0];
//                newYCoordinates[i] = P_prime[1];
//                break;  // 找到后立即跳出循环
//            }
//        }
//
//        // 如果 Pi 不存在于 xCoordinates 或 yCoordinates 中，直接返回空列表
//        if (!isInX || !isInY || indexToReplace == -1) {
//            System.out.println("Point Pi not found in xCoordinates or yCoordinates. No update necessary.");
//            return nodesToUpdate;
//        }
//
//        // 输出新的 xCoordinates 和 yCoordinates
//        System.out.println("New xCoordinates: " + Arrays.toString(newXCoordinates));
//        System.out.println("New yCoordinates: " + Arrays.toString(newYCoordinates));
//
//        // 找到 Pi[0] 和 P_prime[0] 对应的叶子节点
//        int leafStartIndex = (int) Math.pow(2, t) - 1;
//        int leafNodeIndex1 = leafStartIndex + Pi[0];
//        nodesToUpdate.add(leafNodeIndex1);
//        int leafNodeIndex2 = leafStartIndex + P_prime[0];
//        nodesToUpdate.add(leafNodeIndex2);
//
//        // 添加这两个叶子节点到更新列表中
////        addNodeAndParents(nodesToUpdate, leafNodeIndex1);
////        addNodeAndParents(nodesToUpdate, leafNodeIndex2);
//
//        return nodesToUpdate;
//    }
//
//    private void addNodeAndParents(List<Integer> nodesToUpdate, int nodeIndex) {
//        // 如果节点已经存在于更新列表中，则不需要重复添加
//        if (!nodesToUpdate.contains(nodeIndex)) {
//            nodesToUpdate.add(nodeIndex);
//        }
//        // 递归查找父节点，并添加到更新列表中
//        int currentIndex = nodeIndex;
//        while (currentIndex > 0) {
//            // 计算父节点索引
//            int parentIndex = (currentIndex - 1) / 2;
//            // 如果父节点尚未记录，则添加
//            if (!nodesToUpdate.contains(parentIndex)) {
//                nodesToUpdate.add(parentIndex);
//            }
//            // 更新当前节点为父节点
//            currentIndex = parentIndex;
//        }
//    }
}



