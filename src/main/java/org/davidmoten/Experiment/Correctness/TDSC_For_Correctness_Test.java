package org.davidmoten.Experiment.Correctness;

import org.davidmoten.BPC.BPCGenerator;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;
import org.davidmoten.Scheme.TDSC2023.DPRF.BRC_DPRF;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TDSC_For_Correctness_Test {
    // 列表用于存储 update 和 search 的时间
    public List<Double> totalUpdateTimes = new ArrayList<>();    // 存储 update 操作的总耗时
    public List<Double> clientSearchTimes = new ArrayList<>();         // 存储客户端 search 操作的时间
    public List<Double> serverSearchTimes = new ArrayList<>();         // 存储服务器 search 操作的时间
    public static final int LAMBDA = 128;  // 安全参数 λ    // 缓存的MessageDigest实例
    private final MessageDigest messageDigest;
    private final byte[] intBuffer = new byte[4]; // 用于缓存int转byte的缓冲区

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HASH_ALGORITHM = "SHA-256";
    private String KS; // 主密钥
    private int lambda; // 公共参数
    public Map<String, Integer> T; // 计数器表
    private ConcurrentHashMap<String, BigInteger> PDB;
    private ConcurrentHashMap<String, BigInteger> KDB;
    private BRC_DPRF dprf; // 使用 DPRF 进行密钥派生
    public final Mac hmac;

    private int dimension; // 2维数据
    private int order; // Hilbert curve 阶数
    public HilbertCurve hilbertCurve;

    //    private int maxnums_w; // 关键字最大数量
//    private String filePath; // 数据集路径
    private int maxFiles; // 最大文件数
    private BigInteger n; // 最大文件数

    // 修改后的构造函数
    public TDSC_For_Correctness_Test(int securityParameter, int maxnums_w, int maxFiles, int order, int dimension) throws Exception {
//        this.maxnums_w = maxnums_w;
//        this.filePath = filePath;
        this.hmac = Mac.getInstance(HMAC_ALGORITHM);
        this.messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
        this.maxFiles = maxFiles;
        this.n = BigInteger.valueOf(2).pow(maxFiles);

        this.KS = generateMasterKey(securityParameter);
//        this.homomorphicEncryption = new HomomorphicEncryption(maxFiles); // 初始化同态加密实例
        this.dprf = new BRC_DPRF(lambda,(int)Math.log(maxFiles));

        this.T = new HashMap<>();
        this.PDB = new ConcurrentHashMap<>();
        this.KDB = new ConcurrentHashMap<>();
//        this.hashFunctions = new HashFunctions();

        this.order = order;
        this.dimension = dimension;
        this.hilbertCurve = HilbertCurve.bits(order).dimensions(dimension);
    }

    // 生成主密钥
    private String generateMasterKey(int securityParameter) throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        byte[] keyBytes = new byte[securityParameter / 8];
        secureRandom.nextBytes(keyBytes);
        return bytesToHex(keyBytes);
    }

    // 将字节数组转换为十六进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }


    private List<String> preCode(long[] pSet) {
        // 计算点的 Hilbert 索引
        BigInteger pointHilbertIndex = this.hilbertCurve.index(pSet);

        // 必要的长度为 2 * order 位
        int requiredLength = 2 * order;

        // 获取 Hilbert 索引的二进制字符串，并补充前导零
        String binaryString = String.format("%" + requiredLength + "s", pointHilbertIndex.toString(2)).replace(' ', '0');

        // 初始化结果列表
        List<String> prefixList = new ArrayList<>(requiredLength + 1);

        // 添加完整的二进制字符串
        prefixList.add(binaryString);

        // 从最后一个字符开始替换为 '*'，逐步生成前缀
        StringBuilder builder = new StringBuilder(binaryString);
        for (int i = binaryString.length() - 1; i >= 0; i--) {
            builder.setCharAt(i, '*');
            prefixList.add(builder.toString());
        }

        return prefixList;
    }

    public List<String> preCover(BigInteger min, BigInteger max) {
        //生成min到max的所有Bigint
        BigInteger[] R = Stream.iterate(min, n -> n.add(BigInteger.ONE))
                .limit(max.subtract(min).add(BigInteger.ONE).intValueExact())
                .toArray(BigInteger[]::new);

        // 获取BPC结果（包括分组）
        Map<Integer, List<BigInteger>> resultMap = BPCGenerator.GetBPCValueMap(R, this.order*2);
//        System.out.println("BPC:" + BPCGenerator.convertMapToPrefixString(resultMap,this.order*2));
        return BPCGenerator.convertMapToPrefixString(resultMap,this.order*2);
    }
    private int getCounter(String input) {
        return T.getOrDefault(input, -1);
    }

    // 更新操作
    public void update(long[] pSet, String[] W, String op, int[] files, int CounterLimits) throws Exception {
        byte[] combinedKey;
        byte[] Kp;
        byte[] Kp_prime;
        // 记录开始时间
        long startTime = System.nanoTime();
        List<String> P = preCode(pSet);
        for (String p : P) {
            long startTime1 = System.nanoTime();            // 记录单次循环的开始时间
            long loopStartTime = System.nanoTime();
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            Kp = Arrays.copyOfRange(combinedKey, 0, LAMBDA / 8);
            Kp_prime = Arrays.copyOfRange(combinedKey, LAMBDA / 8, LAMBDA / 4); // 假设 LAMBDA / 4 是所需的长度

            int c = T.getOrDefault(p, -1);

            // 使用 DPRF.Derive获取Tp_c_plus_1
            byte[] Tp_c_plus_1 = dprf.newDerivedKey(Kp, c + 1);
//            System.out.println("Tp_c_plus_1:"+ Arrays.toString(Tp_c_plus_1));
            T.put(p, c + 1);
            byte[] UTp_c_plus_1 = hashFunction1(Kp_prime, Tp_c_plus_1);
//            System.out.println("UTp_c_plus_1:"+ Arrays.toString(UTp_c_plus_1));
            BigInteger skp_c1 = hashFunction2(Kp_prime, c + 1);

            long startTime2 = System.nanoTime();            // 设置位图
            BigInteger B = BigInteger.ZERO;  // 使用 BigInteger 作为位图
            for (int fileIndex : files) {
                if ("add".equals(op)) {
                    B = B.setBit(fileIndex);  // 添加操作，设置bsa中相应位为1
                } else if ("del".equals(op)) {
                    B = B.setBit(fileIndex).not();  // 删除操作，设置bsa中相应位为1,然后取反
                }
            }

            BigInteger ep_c1 = skp_c1.add(B).mod(n);
            long startTime4 = System.nanoTime();
            PDB.put(new String(UTp_c_plus_1, StandardCharsets.UTF_8), ep_c1);
//            System.out.println("TDSC_BITSET per ptime1: " + (startTime2-startTime1) / 1_000_000.0 + " ms.");
//            System.out.println("TDSC_BITSET per ptime2: " + (startTime3-startTime2) / 1_000_000.0 + " ms.");
//            System.out.println("TDSC_BITSET per ptime3: " + (startTime4-startTime3) / 1_000_000.0 + " ms.");
        }
        long pTime = System.nanoTime();

        for (String w : W) {
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            Kp = Arrays.copyOfRange(combinedKey, 0, LAMBDA / 8);
            Kp_prime = Arrays.copyOfRange(combinedKey, LAMBDA / 8, LAMBDA / 4); // 假设 LAMBDA / 4 是所需的长度

            int c = T.getOrDefault(w, -1);

            // 使用 DPRF 来生成 DelKey 和 Derive
            byte[] Tw_c1 = dprf.newDerivedKey(Kp, c + 1);
            T.put(w, c + 1);
            byte[] UTw_c1 = hashFunction1(Kp_prime, Tw_c1);
            BigInteger skw_c1 = hashFunction2(Kp_prime, c + 1);
            // 设置位图
            BigInteger B = BigInteger.ZERO;  // 使用 BigInteger 作为位图
            for (int fileIndex : files) {
                if ("add".equals(op)) {
                    B = B.setBit(fileIndex);  // 添加操作，设置bsa中相应位为1
                } else if ("del".equals(op)) {
                    B = B.setBit(fileIndex).not();  // 删除操作，设置bsa中相应位为1,然后取反
                }
            }
            BigInteger ew_c1 = skw_c1.add(B).mod(n);
            KDB.put(new String(UTw_c1, StandardCharsets.UTF_8), ew_c1);
        }
        long wTime = System.nanoTime();
        // 输出总耗时
        double totalLoopTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
//        System.out.println("TDSC2023_BITSET Total update time: " + totalLoopTimeMs + " ms).");
//        System.out.println("TDSC2023_BITSET ptime: " + (pTime-startTime) / 1_000_000.0 + " ms.");
//        System.out.println("TDSC2023_BITSET wtime: " + (wTime-pTime) / 1_000_000.0 + " ms.");

        // 存储到列表中
        totalUpdateTimes.add(totalLoopTimeMs);
//        System.out.println("Update operation completed.");
    }

    // 整合客户端和服务器的搜索操作
    public BigInteger Search(BigInteger R_min, BigInteger R_max, String[] WQ) throws Exception {
        byte[] combinedKey;
        byte[] Kp;
        byte[] Kp_prime;
        // 客户端：生成搜索请求
        long startTime = System.nanoTime();
        List<String> BPC = preCover(R_min, R_max);
        long client_time2 = System.nanoTime();
        BigInteger SumP = BigInteger.ZERO;
        boolean exist = true;
        for (String p : BPC) {
            // 客户端处理
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            Kp = Arrays.copyOfRange(combinedKey, 0, LAMBDA / 8);
            Kp_prime = Arrays.copyOfRange(combinedKey, LAMBDA / 8, LAMBDA / 4);
            int c = getCounter(p);
            if (c == -1) {
//                System.out.println("没有匹配的结果");
                continue;
            }
//            Key STp = dprf.DelKey(Kp_prime, c);
            List<BRC_DPRF.Trapdoor> STp = dprf.delKey(Kp, c);
//            System.out.println("STp:"+ STp);
            // 服务器处理
            BigInteger SumPe = BigInteger.ZERO;
            // 从 c 开始迭代
            for (int i = c; i >= 0; i--) {
                byte[] Ti = dprf.deriveByIndex(i, STp);
//                System.out.println("Ti:"+ Arrays.toString(Ti));
                byte[] UTi = hashFunction1(Kp_prime, Ti);
//                System.out.println("UTi:"+ Arrays.toString(UTi));

                BigInteger e_p_i = PDB.get(new String(UTi, StandardCharsets.UTF_8));
//                System.out.println("c:"+c);
//                System.out.println(new String(UTi, StandardCharsets.UTF_8) + ": ");
//                for (Map.Entry<String, BigInteger> entry : PDB.entrySet()) {
//                    System.out.println("c:"+c);
//                    System.out.println(entry.getKey() + ": " + entry.getValue());
//                }
                if (e_p_i == null) {
//                    System.out.println("e_p_i = null");
                    break;
                } else {
                    SumPe = SumPe.add(e_p_i).mod(n);
                    PDB.remove(new String(UTi, StandardCharsets.UTF_8)); // 将密文标记为已删除
                }
            }
            byte[] Tc = dprf.deriveByIndex(c, STp);
            byte[] UTc = hashFunction1(Kp_prime, Tc);
            PDB.put(new String(UTc, StandardCharsets.UTF_8), SumPe); // 将最新的索引更新至UTc
            SumP = SumP.add(SumPe).mod(n);
        }

        List<BigInteger> SumWList = new ArrayList<>();
        for (String w : WQ) {
            // 客户端处理
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            byte[] Kw = new byte[LAMBDA / 8];
            byte[] Kw_prime = new byte[LAMBDA / 8];
            Kw = Arrays.copyOfRange(combinedKey, 0, LAMBDA / 8);
            Kw_prime = Arrays.copyOfRange(combinedKey, LAMBDA / 8, LAMBDA / 4);
            int c = getCounter(w);
            if (c == -1) {
                exist = false;
//                System.out.println("没有匹配"+w+"的结果");
                break;
            }
            List<BRC_DPRF.Trapdoor> STw = dprf.delKey(Kw, c);
//            clientRequest_w.add(new Object[]{Kw_prime, STw, c});
            BigInteger SumWe = BigInteger.ZERO;

            // 服务器处理
            // 从 c 开始迭代
            for (int i = c; i >= 0; i--) {
                byte[] Ti = dprf.deriveByIndex(i, STw);
                byte[] UTi = hashFunction1(Kw_prime, Ti);

                BigInteger e_p_i = KDB.get(new String(UTi, StandardCharsets.UTF_8));
                if (e_p_i == null) {
                    break;
                } else {
                    SumWe = SumWe.add(e_p_i).mod(n);
                    KDB.remove(new String(UTi, StandardCharsets.UTF_8)); // 将密文标记为已删除
                }
            }
            byte[] Tc = dprf.deriveByIndex(c, STw);
            byte[] UTc = hashFunction1(Kw_prime, Tc);
            KDB.put(new String(UTc, StandardCharsets.UTF_8), SumWe); // 将最新的索引更新至UTc
            SumWList.add(SumWe);
        }
        if(!exist){
            long client_time_notexist = System.nanoTime();
            // 存储到列表中
            double msclient_time = (client_time_notexist - startTime) / 1_000_000.0;
            double msserver_time = 0 / 1_000_000.0;
            clientSearchTimes.add(msclient_time);
            serverSearchTimes.add(msserver_time);
            return BigInteger.ZERO;
        }
        //客户端解密阶段
        long client_time3 = System.nanoTime();
        BigInteger SumP_sk = BigInteger.ZERO;
        for (String p : BPC) {
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            Kp_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, LAMBDA / 8, Kp_prime, 0, LAMBDA / 8);
            int c = getCounter(p);

            for (int i = c; i >= 0; i--) {
                BigInteger skp_i = hashFunction2(Kp_prime, i);
                SumP_sk = SumP_sk.add(skp_i).mod(n);
            }
        }
        // 解密前缀部分
        BigInteger BR = SumP.subtract(SumP_sk).add(n).mod(n);
//        System.out.println("BR1:");
//        findIndexesOfOne(BR);
        for (int j = 0; j < WQ.length; j++) {
            String w = WQ[j];
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            byte[] Kw_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);
            int c = getCounter(w);
            BigInteger SumW_sk = BigInteger.ZERO;
            for (int i = c; i >= 0; i--) {
                BigInteger skw_i = hashFunction2(Kw_prime, i);
                SumW_sk = SumW_sk.add(skw_i).mod(n);
            }

            // 解密并与前缀部分进行与操作
            BR = BR.and(SumWList.get(j).subtract(SumW_sk).add(n).mod(n));
        }
//        System.out.println("BR2:");
//        findIndexesOfOne(BR);
        long client_time4 = System.nanoTime();
        // 输出总耗时
//        double totalLoopTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;
//        System.out.println("TDSC_For_Correctness_Test Total search time: " + totalLoopTimeMs + " ms).");
        // 客户端部分结束计时
//        long server_time2 = System.nanoTime();
        // 输出客户端和服务器端的时间消耗
        double msclient_time = ((client_time2 - startTime) + (client_time4 - client_time3)) / 1_000_000.0;
        double msserver_time = (client_time3 - client_time2) / 1_000_000.0;
//        double total_time = msclient_time + msserver_time;
//        System.out.println("TDSC: Client time part 1: " + msclient_time1 + " ms, Server time: " + msserver_time + " ms, Total time: " + total_time + " ms");

        // 存储到列表中
        clientSearchTimes.add(msclient_time);
        serverSearchTimes.add(msserver_time);
        return BR;
    }
    /**
     * 伪随机函数 P'
     *
     * @param key     安全密钥
     * @param keyword 关键词
     * @return 伪随机生成的密钥
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    private byte[] pseudoRandomFunction(byte[] key, String keyword) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec keySpec = new SecretKeySpec(key, HMAC_ALGORITHM);
        hmac.init(keySpec);
        return hmac.doFinal(keyword.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 安全哈希函数 H1, H2
     *
     * @param input1 输入值1
     * @param input2 输入值2
     * @return 哈希后的值
     * @throws NoSuchAlgorithmException
     */
    // 重载方法，接受byte[]类型的input2
    private byte[] hashFunction1(byte[] input1, byte[] input2) {
        messageDigest.reset(); // 重置MessageDigest实例
        messageDigest.update(input1);
        // 直接更新byte[]到MessageDigest
        messageDigest.update(input2);
        return messageDigest.digest();
    }

    private BigInteger hashFunction2(byte[] input1, int input2) {
        messageDigest.reset(); // 重置MessageDigest实例
        messageDigest.update(input1);
        // 将int转为byte并更新到MessageDigest
        intBuffer[0] = (byte) (input2 >> 24);
        intBuffer[1] = (byte) (input2 >> 16);
        intBuffer[2] = (byte) (input2 >> 8);
        intBuffer[3] = (byte) input2;
        messageDigest.update(intBuffer);
        return new BigInteger(1, messageDigest.digest());
    }

    public static Object[] GetRandomItem(int W_num, String FILE_PATH) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH));
        String selectedRow = null;
        String line;
        Random random = new Random();

        // 跳过第一行（标题行）
        String header = reader.readLine();

        int currentLine = 0;
        while ((line = reader.readLine()) != null) {
            currentLine++;
            // 以1/N的概率选择当前行，确保每一行被选中的概率相同
            if (random.nextInt(currentLine) == 0) {
                selectedRow = line;
            }
        }

        reader.close();

        if (selectedRow == null) {
            System.out.println("CSV文件是空的或只有标题行。");
            return null;
        }

        // 分割该行，提取数据
        String[] columns = selectedRow.split(",");

        // id 转换为位图B
        BigInteger id = new BigInteger(columns[0]);
        // x 和 y 转换为 long[] pSet
        long x = Long.parseLong(columns[1]);
        long y = Long.parseLong(columns[2]);
        long[] pSet = new long[]{x, y};

        // key1 到 key12 转换为 String[] W
        String[] W = new String[W_num];
        for (int i = 0; i < W_num; i++) {
            // 如果列存在，则取值，否则设为null
            if (columns.length > (i + 3)) {
                W[i] = columns[i + 3];
            } else {
                W[i] = null;
            }
        }

        // 输出结果


        // 返回pSet和W
        return new Object[]{id, pSet, W};
    }

    public double getAverageSearchTime() {
        if (clientSearchTimes.size() != serverSearchTimes.size() || clientSearchTimes.isEmpty()) {
            System.out.println("列表大小不一致或者为空，无法计算平均搜索时间。");
            return 0.0;
        }

        // 使用stream高阶函数计算两个列表对应位置的元素之和的平均值
        return IntStream.range(0, clientSearchTimes.size())
                .mapToDouble(i -> clientSearchTimes.get(i) + serverSearchTimes.get(i))
                .average()
                .orElse(0.0);
    }

    public void removeExtremesUpdateTime() {
        if (totalUpdateTimes.size() > 2) { // 确保列表中至少有3个元素
            // 找到最大值和最小值的索引
            int maxIndex = totalUpdateTimes.indexOf(Collections.max(totalUpdateTimes));
            int minIndex = totalUpdateTimes.indexOf(Collections.min(totalUpdateTimes));

            // 先移除较大的索引，避免影响较小索引
            if (maxIndex > minIndex) {
                totalUpdateTimes.remove(maxIndex);
                totalUpdateTimes.remove(minIndex);
            } else {
                totalUpdateTimes.remove(minIndex);
                totalUpdateTimes.remove(maxIndex);
            }

//            System.out.println("已移除 totalUpdateTimes 列表中的最大值和最小值。");
        } else {
            System.out.println("totalUpdateTimes 列表元素不足，无法移除最大值和最小值。");
        }
    }

    public void removeExtremesSearchTime() {
        if (clientSearchTimes.size() > 2 && serverSearchTimes.size() > 2) { // 确保两个列表中至少有3个元素
            // 找到 clientSearchTimes 和 serverSearchTimes 中的最大值和最小值的索引
            int maxClientIndex = clientSearchTimes.indexOf(Collections.max(clientSearchTimes));
            int minClientIndex = clientSearchTimes.indexOf(Collections.min(clientSearchTimes));
            int maxServerIndex = serverSearchTimes.indexOf(Collections.max(serverSearchTimes));
            int minServerIndex = serverSearchTimes.indexOf(Collections.min(serverSearchTimes));

            // 先移除较大的索引，避免影响较小索引
            if (maxClientIndex > minClientIndex) {
                clientSearchTimes.remove(maxClientIndex);
                clientSearchTimes.remove(minClientIndex);
            } else {
                clientSearchTimes.remove(minClientIndex);
                clientSearchTimes.remove(maxClientIndex);
            }

            if (maxServerIndex > minServerIndex) {
                serverSearchTimes.remove(maxServerIndex);
                serverSearchTimes.remove(minServerIndex);
            } else {
                serverSearchTimes.remove(minServerIndex);
                serverSearchTimes.remove(maxServerIndex);
            }

            System.out.println("已移除 clientSearchTimes 和 serverSearchTimes 列表中的最大值和最小值。");
        } else {
            System.out.println("clientSearchTimes 或 serverSearchTimes 列表元素不足，无法移除最大值和最小值。");
        }
    }

    // 获取更新操作的平均时间
    public double getAverageUpdateTime() {
        return totalUpdateTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    // 获取客户端查询操作的平均时间
    public double getAverageClientTime() {
        return clientSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    // 获取服务器查询操作的平均时间
    public double getAverageServerTime() {
        return serverSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    // 打印 update 和 search 的时间列表
    public void printTimes() {
        System.out.println("Update Times:");
        for (Double time : totalUpdateTimes) {
            System.out.print(time + " ms ");
        }
        System.out.println();
        System.out.println("Client Search Times:");
        for (Double time : clientSearchTimes) {
            System.out.print(time + " ms ");
        }
        System.out.println();
        System.out.println("Server Search Times:");
        for (Double time : serverSearchTimes) {
            System.out.print(time + " ms ");
        }
        System.out.println();
    }


    public static void main(String[] args) throws Exception {
        // 设置一些参数
        int securityParameter = 128; // 安全参数 λ
        int maxnums_w = 6; // 关键字数量
        int maxFiles = 1 << 20; // 最大文件数
        int order = 12; // Hilbert curve 阶数
        int dimension = 2; // 维度

        // 初始化 TDSC_For_Correctness_Test 实例
        TDSC_For_Correctness_Test tdsc2023 = new TDSC_For_Correctness_Test(securityParameter, maxnums_w, maxFiles, order, dimension);

        // 创建随机生成器和一些测试数据
        Random random = new Random();
        int numObjects = 1; // 插入5个对象进行测试
        int rangePredicate = 10000;

        // 初始化测试对象的数据
        long[][] pSets = new long[numObjects][2];
        String[][] WSets = new String[numObjects][maxnums_w];
        int[][] fileSets = new int[numObjects][1]; // 每个对象关联一个文件

        // 填充对象数据
        for (int i = 0; i < numObjects; i++) {
            // 创建pSet (二维数据)
            pSets[i][0] = random.nextInt(10000);
            pSets[i][1] = pSets[i][0] + 1;

            // 创建关键词W
            for (int j = 0; j < maxnums_w; j++) {
                WSets[i][j] = "keyword" + (random.nextInt(100) + 1);
            }

            // 关联一个文件
            fileSets[i][0] = random.nextInt(maxFiles);
        }

        // 打印插入的数据
        System.out.println("即将插入的数据:");
        for (int i = 0; i < numObjects; i++) {
            System.out.println("Object " + (i + 1) + ":");
            System.out.println("  pSet: " + Arrays.toString(pSets[i]));
            System.out.println("  W: " + Arrays.toString(WSets[i]));
            System.out.println("  File ID: " + Arrays.toString(fileSets[i]));
        }
        // 执行update操作（插入数据）
        System.out.println("插入操作开始...");
//        printMap(tdsc2023.T);
//        System.out.println("PDB:");
//        printMap(tdsc2023.PDB);
//        System.out.println("KDB:");
//        printMap(tdsc2023.KDB);
        for (int i = 0; i < numObjects; i++) {
            tdsc2023.update(pSets[i], WSets[i], "add", fileSets[i], rangePredicate);
        }
        System.out.println("插入操作完成。");
//        printMap(tdsc2023.T);
//        System.out.println("PDB:");
//        printMap(tdsc2023.PDB);
//        System.out.println("KDB:");
//        printMap(tdsc2023.KDB);
        // 测试搜索操作
        System.out.println("开始搜索...");
        for (int i = 0; i < numObjects; i++) {
            // 通过 Hilbert 曲线计算范围
            BigInteger pointHilbertIndex = tdsc2023.hilbertCurve.index(pSets[i]);
            BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(100));
            BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(100));

            // 执行搜索操作
            BigInteger result = tdsc2023.Search(R_min, R_max, WSets[i]);

            // 打印搜索结果
            System.out.println("\n搜索结果 (pSet " + Arrays.toString(pSets[i]) + "): ");
            findIndexesOfOne(result); // 打印出结果中的位图索引
        }
        System.out.println("搜索操作完成。");
    }
    public static void findIndexesOfOne(BigInteger number) {
        // 收集所有位索引
        List<Integer> indexes = new ArrayList<>();
        int index = 0;

        // 遍历所有可能的位
        while (number.signum() != 0) {
            // 检查当前最低位是否为1
            if (number.and(BigInteger.ONE).equals(BigInteger.ONE)) {
                // 收集当前位的索引
                indexes.add(index);
            }

            // 右移一位处理下一位
            number = number.shiftRight(1);
            index++;
        }

        // 打印结果
        if (indexes.isEmpty()) {
            System.out.println("没有找到1位。");
        } else {
            System.out.println("位图中1的位置索引为：");
            for (Integer idx : indexes) {
                System.out.println("索引: " + idx);
            }
        }
    }
}