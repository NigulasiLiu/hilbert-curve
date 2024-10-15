package org.davidmoten.Scheme.TDSC2023;

import org.davidmoten.BPC.BPCGenerator;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;

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

public class TDSC2023_Biginteger {
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
    private Map<String, Integer> T; // 计数器表
    private ConcurrentHashMap<String, BigInteger> PDB;
    private ConcurrentHashMap<String, BigInteger> KDB;
    private DPRF dprf; // 使用 DPRF 进行密钥派生
    public final Mac hmac;

    private int dimension; // 2维数据
    private int order; // Hilbert curve 阶数
    public HilbertCurve hilbertCurve;
    private BPCGenerator bpcGenerator;

    //    private int maxnums_w; // 关键字最大数量
//    private String filePath; // 数据集路径
    private int maxFiles; // 最大文件数
    private BigInteger n; // 最大文件数

    // 修改后的构造函数
    public TDSC2023_Biginteger(int securityParameter, int maxnums_w, int maxFiles, int order, int dimension) throws Exception {
//        this.maxnums_w = maxnums_w;
//        this.filePath = filePath;
        this.hmac = Mac.getInstance(HMAC_ALGORITHM);
        this.messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
        this.maxFiles = maxFiles;
        this.n = BigInteger.valueOf(2).pow(maxFiles);

        this.KS = generateMasterKey(securityParameter);
//        this.homomorphicEncryption = new HomomorphicEncryption(maxFiles); // 初始化同态加密实例
        this.dprf = new DPRF(maxnums_w);

        this.T = new HashMap<>();
        this.PDB = new ConcurrentHashMap<>();
        this.KDB = new ConcurrentHashMap<>();
//        this.hashFunctions = new HashFunctions();

        this.order = order;
        this.dimension = dimension;
        this.hilbertCurve = HilbertCurve.bits(order).dimensions(dimension);
        this.bpcGenerator = new BPCGenerator(order * 2); // Hilbert曲线编码最大值为2^(2*order)
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

        // 打印 Hilbert 索引的值
        //System.out.println("Hilbert Index (BigInteger): " + pointHilbertIndex);

        // 将 Hilbert 索引转换为二进制字符串，并确保其长度为 2 * order 位
        String hilbertBinary = pointHilbertIndex.toString(2);
        int requiredLength = 2 * order;

        // 如果二进制字符串长度不足，前面补0
        hilbertBinary = String.format("%" + requiredLength + "s", hilbertBinary).replace(' ', '0');

        // 打印二进制表示及其长度
//        System.out.println("Hilbert Index (Binary): " + hilbertBinary);
        //System.out.println("Length of Hilbert Binary: " + hilbertBinary.length());

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

    public List<String> preCover(BigInteger min, BigInteger max) {
        //生成min到max的所有Bigint
        BigInteger[] R = Stream.iterate(min, n -> n.add(BigInteger.ONE))
                .limit(max.subtract(min).add(BigInteger.ONE).intValueExact())
                .toArray(BigInteger[]::new);


        List<BigInteger> results = this.bpcGenerator.GetBPCValueList(R);
        List<String> BinaryResults = new ArrayList<>();
//        System.out.println("BPC1: " + results);
        for (BigInteger result : results) {
            String bpc_string = this.bpcGenerator.toBinaryStringWithStars(result, order * 2, this.bpcGenerator.shiftCounts.get(result));
            BinaryResults.add(bpc_string);
        }
//        System.out.println("BPC2:" + BinaryResults);
        return BinaryResults;
    }

//    private String[] F_K_sigma(String KS, String input) throws Exception {
//        hashFunctions.hmacSHA256.init(new SecretKeySpec(KS.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
//        byte[] result = hashFunctions.hmacSHA256.doFinal(input.getBytes(StandardCharsets.UTF_8));
//        String K = Base64.getEncoder().encodeToString(Arrays.copyOfRange(result, 0, result.length / 2));
//        String KPrime = Base64.getEncoder().encodeToString(Arrays.copyOfRange(result, result.length / 2, result.length));
//        return new String[]{K, KPrime};
//    }

    private int getCounter(String input) {
        return T.getOrDefault(input, 0);
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

//            String[] keys = F_K_sigma(KS, p);
//            String Kp = keys[0];
//            String KpPrime = keys[1];
            int c = T.getOrDefault(p, 0);

            // 使用 DPRF.Derive获取Tp_c_plus_1
            byte[] Tp_c_plus_1 = dprf.Derive(new SecretKeySpec(Kp, HMAC_ALGORITHM), c + 1);
            T.put(p, c + 1);
            byte[] UTp_c_plus_1 = hashFunction1(Kp, Tp_c_plus_1);
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
            PDB.put(new String(UTp_c_plus_1), ep_c1);
//            System.out.println("TDSC_BITSET per ptime1: " + (startTime2-startTime1) / 1_000_000.0 + " ms.");
//            System.out.println("TDSC_BITSET per ptime2: " + (startTime3-startTime2) / 1_000_000.0 + " ms.");
//            System.out.println("TDSC_BITSET per ptime3: " + (startTime4-startTime3) / 1_000_000.0 + " ms.");
        }
        long pTime = System.nanoTime();

        for (String w : W) {
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            Kp = Arrays.copyOfRange(combinedKey, 0, LAMBDA / 8);
            Kp_prime = Arrays.copyOfRange(combinedKey, LAMBDA / 8, LAMBDA / 4); // 假设 LAMBDA / 4 是所需的长度

            int c = T.getOrDefault(w, 0);

            // 使用 DPRF 来生成 DelKey 和 Derive
            byte[] Tw_c1 = dprf.Derive(new SecretKeySpec(Kp, HMAC_ALGORITHM), c + 1);
            T.put(w, c + 1);
            byte[] UTw_c1 = hashFunction1(Kp, Tw_c1);
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
            KDB.put(new String(UTw_c1), ew_c1);
        }
        long wTime = System.nanoTime();
        // 输出总耗时
        double totalLoopTimeMs = (System.nanoTime()-startTime) / 1_000_000.0;
//        System.out.println("TDSC2023_BITSET Total update time: " + totalLoopTimeMs + " ms).");
//        System.out.println("TDSC2023_BITSET ptime: " + (pTime-startTime) / 1_000_000.0 + " ms.");
//        System.out.println("TDSC2023_BITSET wtime: " + (wTime-pTime) / 1_000_000.0 + " ms.");

        // 存储到列表中
        totalUpdateTimes.add(totalLoopTimeMs);
//        System.out.println("Update operation completed.");
    }
    // 整合客户端和服务器的搜索操作
    public BigInteger Search(BigInteger R_min, BigInteger R_max, String[] WQ) throws Exception {
        // 客户端：生成搜索请求
        long startTime = System.nanoTime();
        List<String> BPC = preCover(R_min, R_max);
        BigInteger SumP = BigInteger.ZERO;
        long client_time2 = System.nanoTime();
        for (String p : BPC) {
            // 客户端处理
            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            byte[] Kp = new byte[LAMBDA / 8];
            byte[] Kp_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, 0, Kp, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kp_prime, 0, LAMBDA / 8);
            int c = getCounter(p);
            Key STp = dprf.DelKey(new String(Kp_prime), c);
//            clientRequest_p.add(new Object[]{Kp_prime, STp, c});

            // 服务器处理
            BigInteger SumPe = BigInteger.ZERO;
            // 从 c 开始迭代
            for (int i = c; i >= 0; i--) {
                byte[] Ti = dprf.Derive(STp, i);
//                String Ti = HMAC_SHA256;
                byte[] UTi = hashFunction1(Kp_prime, Ti);

                BigInteger e_p_i = PDB.get(new String(UTi));
                if (e_p_i == null) {
                    break;
                } else {
                    SumPe = SumPe.add(e_p_i).mod(n);
                    PDB.remove(new String(UTi)); // 将密文标记为已删除
                }
            }
            byte[] Tc = dprf.Derive(STp, c);
            byte[] UTc = hashFunction1(Kp_prime, Tc);
            PDB.put(new String(UTc), SumPe); // 将最新的索引更新至UTc
            SumP = SumP.add(SumPe).mod(n);
        }
        List<BigInteger> SumWList = new ArrayList<>();
        for (String w : WQ) {
            // 客户端处理
            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            byte[] Kw = new byte[LAMBDA / 8];
            byte[] Kw_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);
            int c = getCounter(w);
            Key STw = dprf.DelKey(new String(Kw), c);
//            clientRequest_w.add(new Object[]{Kw_prime, STw, c});
            BigInteger SumWe = BigInteger.ZERO;

            // 服务器处理
            // 从 c 开始迭代
            for (int i = c; i >= 0; i--) {
                byte[] Ti = dprf.Derive(STw, i);
                byte[] UTi = hashFunction1(Kw_prime, Ti);

                BigInteger e_p_i = KDB.get(new String(UTi));
                if (e_p_i == null) {
                    break;
                } else {
                    SumWe = SumWe.add(e_p_i).mod(n);
                    KDB.remove(new String(UTi)); // 将密文标记为已删除
                }
            }
            byte[] Tc = dprf.Derive(STw, c);
            byte[] UTc = hashFunction1(Kw_prime, Tc);
            KDB.put(new String(UTc), SumWe); // 将最新的索引更新至UTc
            SumWList.add(SumWe);
        }
        //客户端解密阶段
        long client_time3 = System.nanoTime();
        BigInteger SumP_sk = BigInteger.ZERO;
        for (String p : BPC) {
            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            byte[] Kp_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, LAMBDA / 8, Kp_prime, 0, LAMBDA / 8);
            int c = getCounter(p);

            for (int i = c; i >= 0; i--) {
                BigInteger skp_i = hashFunction2(Kp_prime, i);
                SumP_sk = SumP_sk.add(skp_i).mod(n);
            }
        }
        // 解密前缀部分
        BigInteger BR = SumP.subtract(SumP_sk).add(n).mod(n);
        for (int j = 0; j < WQ.length; j++) {
            String w = WQ[j];
            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
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
        long client_time4 = System.nanoTime();
        // 输出总耗时
        double totalLoopTimeMs = (System.nanoTime()-startTime) / 1_000_000.0;
//        System.out.println("TDSC2023_Biginteger Total search time: " + totalLoopTimeMs + " ms).");
        // 客户端部分结束计时
        long server_time2 = System.nanoTime();
        // 输出客户端和服务器端的时间消耗
        double msclient_time =((client_time2 - startTime)+(client_time4 - client_time3))/ 1_000_000.0;
        double msserver_time = (client_time3 - client_time2) / 1_000_000.0;
        double total_time = msclient_time + msserver_time;
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
        return new BigInteger(1,messageDigest.digest());
    }
    public static Object[] GetRandomItem(int W_num,String FILE_PATH) throws IOException {
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
        System.out.println("程序结束。");
    }


}