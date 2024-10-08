package org.davidmoten.Scheme.TDSC2023;

import org.davidmoten.B.B;
import org.davidmoten.BPC.BPCGenerator;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;

import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Stream;

public class TDSC2023 {
    // 列表用于存储 update 和 search 的时间
    public List<Double> totalUpdateTimes = new ArrayList<>();    // 存储 update 操作的总耗时
    public List<Double> clientSearchTimes = new ArrayList<>();         // 存储客户端 search 操作的时间
    public List<Double> serverSearchTimes = new ArrayList<>();         // 存储服务器 search 操作的时间


    private static final String HMAC_SHA256 = "HmacSHA256";
    private String KS; // 主密钥
    private int lambda; // 公共参数
    private Map<String, Integer> T; // 计数器表
    private Map<String, BigInteger> SDB; // 存储空间前缀的加密数据库
    private Map<String, BigInteger> KDB; // 存储关键字的加密数据库
    private DPRF dprf; // 使用 DPRF 进行密钥派生
    private HashFunctions hashFunctions; // 哈希函数类实例
    public HomomorphicEncryption homomorphicEncryption; // 同态加密类实例

    private int dimension; // 2维数据
    private int order; // Hilbert curve 阶数
    public HilbertCurve hilbertCurve;
    private BPCGenerator bpcGenerator;

    //    private int maxnums_w; // 关键字最大数量
//    private String filePath; // 数据集路径
    private int maxFiles; // 最大文件数

    // 修改后的构造函数
    public TDSC2023(int securityParameter, int maxnums_w, int maxFiles, int order, int dimension) throws Exception {
//        this.maxnums_w = maxnums_w;
//        this.filePath = filePath;
        this.maxFiles = maxFiles;

        this.KS = generateMasterKey(securityParameter);
        this.homomorphicEncryption = new HomomorphicEncryption(maxFiles); // 初始化同态加密实例
        this.dprf = new DPRF(maxnums_w);

        this.T = new HashMap<>();
        this.SDB = new HashMap<>();
        this.KDB = new HashMap<>();
        this.hashFunctions = new HashFunctions();

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
        System.out.println("Hilbert Index (BigInteger): " + pointHilbertIndex);

        // 将 Hilbert 索引转换为二进制字符串，并确保其长度为 2 * order 位
        String hilbertBinary = pointHilbertIndex.toString(2);
        int requiredLength = 2 * order;

        // 如果二进制字符串长度不足，前面补0
        hilbertBinary = String.format("%" + requiredLength + "s", hilbertBinary).replace(' ', '0');

        // 打印二进制表示及其长度
//        System.out.println("Hilbert Index (Binary): " + hilbertBinary);
        System.out.println("Length of Hilbert Binary: " + hilbertBinary.length());

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

    private String[] F_K_sigma(String KS, String input) throws Exception {
        hashFunctions.hmacSHA256.init(new SecretKeySpec(KS.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        byte[] result = hashFunctions.hmacSHA256.doFinal(input.getBytes(StandardCharsets.UTF_8));
        String K = Base64.getEncoder().encodeToString(Arrays.copyOfRange(result, 0, result.length / 2));
        String KPrime = Base64.getEncoder().encodeToString(Arrays.copyOfRange(result, result.length / 2, result.length));
        return new String[]{K, KPrime};
    }

    private int getCounter(String input) {
        return T.getOrDefault(input, -1);
    }

    // 更新操作
    public void update(long[] pSet, String[] W, String op, int[] files, int CounterLimits) throws Exception {
        System.out.println("Starting update operation...");
        System.out.println("Input pSet: " + Arrays.toString(pSet));
        System.out.println("Input W: " + Arrays.toString(W));
//        System.out.println("BitMap: " + B);
//        System.out.println("BitMap_Op: " + BitMap_Op);
        System.out.println("CounterLimits: " + CounterLimits);

        // 记录开始时间
        long startTime = System.nanoTime();
        List<String> P = preCode(pSet);
        for (String p : P) {
            // 记录单次循环的开始时间
            long loopStartTime = System.nanoTime();
            String[] keys = F_K_sigma(KS, p);
            String Kp = keys[0];
            String KpPrime = keys[1];
            int c = T.getOrDefault(p, -1);

            // 使用 DPRF.Derive获取Tp_c_plus_1
            String Tp_c_plus_1 = dprf.Derive(new SecretKeySpec(Kp.getBytes(StandardCharsets.UTF_8), HMAC_SHA256), c + 1);
            T.put(p, c + 1);
            String UTp_c_plus_1 = hashFunctions.H1(KpPrime, Tp_c_plus_1);
            BigInteger skp_c1 = hashFunctions.H2(KpPrime, c + 1);

            // 设置位图
            B b = new B(maxFiles);
            for (int fileIndex : files) {
                if ("add".equals(op)) {
                    b.setBit(fileIndex);  // 添加操作，设置bsa中相应位为1
                } else if ("del".equals(op)) {
                    b.setBit(fileIndex);  // 删除操作，设置bsa中相应位为1,然后取反
                    b.not();
                }
            }

            BigInteger ep_c1 = homomorphicEncryption.enc(skp_c1, b.toBigInteger());
            SDB.put(UTp_c_plus_1, ep_c1);
        }

        for (String w : W) {
            String[] keys = F_K_sigma(KS, w);
            String Kw = keys[0];
            String KwPrime = keys[1];
            int c = T.getOrDefault(w, -1);

            // 使用 DPRF 来生成 DelKey 和 Derive
            String Tw_c1 = dprf.Derive(new SecretKeySpec(Kw.getBytes(StandardCharsets.UTF_8), HMAC_SHA256), c + 1);
            T.put(w, c + 1);
            String UTw_c1 = hashFunctions.H1(KwPrime, Tw_c1);
            BigInteger skw_c1 = hashFunctions.H2(KwPrime, c + 1);
            // 设置位图
            B b = new B(maxFiles);
            for (int fileIndex : files) {
                if ("add".equals(op)) {
                    b.setBit(fileIndex);  // 添加操作，设置bsa中相应位为1
                } else if ("del".equals(op)) {
                    b.setBit(fileIndex);  // 删除操作，设置bsa中相应位为1,然后取反
                    b.not();
                }
            }
            BigInteger ew_c1 = homomorphicEncryption.enc(skw_c1, b.toBigInteger());
            KDB.put(UTw_c1, ew_c1);
        }
        // 输出总耗时
        double totalLoopTimeMs = (System.nanoTime()-startTime) / 1_000_000.0;
        System.out.println("TDSC2023 Total update time: " + totalLoopTimeMs + " ms).");

        // 存储到列表中
        totalUpdateTimes.add(totalLoopTimeMs);
        System.out.println("Update operation completed.");
    }
    // 整合客户端和服务器的搜索操作
    public BigInteger Search(BigInteger R_min, BigInteger R_max, String[] WQ) throws Exception {
        // 客户端：生成搜索请求
        long startTime = System.nanoTime();
        List<String> BPC = preCover(R_min, R_max);
        // 客户端：生成搜索请求
        List<Object[]> clientRequest_p = new ArrayList<>();
        List<Object[]> clientRequest_w = new ArrayList<>();
        // 客户端处理前缀集合
        for (String p : BPC) {
            String[] keys = F_K_sigma(KS, p);
            String Kp = keys[0];
            String KpPrime = keys[1];
            int c = getCounter(p);
            if (c == -1) {
                continue; // 如果计数器无效，跳过这个前缀
            }
            Key STp = dprf.DelKey(Kp, c);
            clientRequest_p.add(new Object[]{KpPrime, STp, c});
        }
        // 客户端处理关键字集合
        for (String w : WQ) {
            String[] keys = F_K_sigma(KS, w);
            String Kw = keys[0];
            String KwPrime = keys[1];
            int c = getCounter(w);
            if (c == -1) {
                continue; // 如果计数器无效，跳过这个关键字
            }
            Key STw = dprf.DelKey(Kw, c);
            clientRequest_w.add(new Object[]{KwPrime, STw, c});
        }
        // 客户端部分结束计时
        long client_time2 = System.nanoTime();
        // 模拟服务器端处理
        BigInteger SumP = BigInteger.ZERO;
        List<BigInteger> SumWList = new ArrayList<>();
        // 服务器处理收到的前缀集合和关键字集合
        for (Object[] request : clientRequest_p) {
            String KPrime = (String) request[0];
            Key ST = (Key) request[1];
            int c = (int) request[2];

            BigInteger SumE = BigInteger.ZERO;

            // 从 c 开始迭代
            for (int i = c; i >= 0; i--) {
                String Ti = dprf.Derive(ST, i);
//                String Ti = HMAC_SHA256;
                String UTi = hashFunctions.H1(KPrime, Ti);

                BigInteger e_p_i = KDB.get(UTi);
                if (e_p_i == null) {
                    break;
                } else {
                    SumE = SumE.add(e_p_i).mod(homomorphicEncryption.getN());
                    KDB.put(UTi, null); // 将密文标记为已删除
                }
            }
            String Tc = dprf.Derive(ST, c);
            String UTc = hashFunctions.H1(KPrime, Tc);
            KDB.put(UTc, SumE); // 将最新的索引更新至UTc
        }
        for (Object[] request : clientRequest_w) {
            BigInteger SumWe = BigInteger.ZERO;
            String KwPrime = (String) request[0];
            Key ST = (Key) request[1];
            int c = (int) request[2];

            // 从 c 开始迭代
            for (int i = c; i >= 0; i--) {
                String Ti = dprf.Derive(ST, i);
//                String Ti = HMAC_SHA256;
                String UTi = hashFunctions.H1(KwPrime, Ti);

                BigInteger e_p_i = KDB.get(UTi);
                if (e_p_i == null) {
                    break;
                } else {
                    SumWe = SumWe.add(e_p_i).mod(homomorphicEncryption.getN());
                    KDB.put(UTi, null); // 将密文标记为已删除
                }
            }
            String Tc = dprf.Derive(ST, c);
            String UTc = hashFunctions.H1(KwPrime, Tc);
            KDB.put(UTc, SumWe); // 将最新的索引更新至UTc
            SumWList.add(SumWe);  // 假设只有一个关键字
        }
        // 客户端接收服务器响应并处理
        BigInteger SumP_sk = BigInteger.ZERO;
        // 处理前缀集合
        for (String p : BPC) {
            String[] keys = F_K_sigma(KS, p);
            String KpPrime = keys[1];
            int c = getCounter(p);

            for (int i = c; i >= 0; i--) {
                BigInteger skp_i = hashFunctions.H2(KpPrime, i);
                SumP_sk = SumP_sk.add(skp_i).mod(homomorphicEncryption.getN());
            }
        }
        // 解密前缀部分
        BigInteger BR = homomorphicEncryption.dec(SumP_sk, SumP);
        // 处理关键字集合
        for (int j = 0; j < WQ.length; j++) {
            String w = WQ[j];
            String[] keys = F_K_sigma(KS, w);
            String KwPrime = keys[1];
            int c = getCounter(w);

            BigInteger SumW_sk = BigInteger.ZERO;

            for (int i = c; i >= 0; i--) {
                BigInteger skw_i = hashFunctions.H2(KwPrime, i);
                SumW_sk = SumW_sk.add(skw_i).mod(homomorphicEncryption.getN());
            }

            // 解密并与前缀部分进行与操作
            BR = BR.and(homomorphicEncryption.dec(SumW_sk, SumWList.get(j)));
        }
        // 输出总耗时
        double totalLoopTimeMs = (System.nanoTime()-startTime) / 1_000_000.0;
        System.out.println("TDSC2023 Total search time: " + totalLoopTimeMs + " ms).");
        // 客户端部分结束计时
        long server_time2 = System.nanoTime();
        // 输出客户端和服务器端的时间消耗
        double msclient_time1 = (client_time2 - startTime) / 1_000_000.0;
        double msserver_time = (server_time2 - client_time2) / 1_000_000.0;
        double total_time = msclient_time1 + msserver_time;
        System.out.println("TDSC: Client time part 1: " + msclient_time1 + " ms, Server time: " + msserver_time + " ms, Total time: " + total_time + " ms");

        // 存储到列表中
        clientSearchTimes.add(msclient_time1);
        serverSearchTimes.add(msserver_time);
        return BR;
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
        System.out.println("文件id: " + id);
        System.out.println("pSet: [x=" + pSet[0] + ", y=" + pSet[1] + "]");
        System.out.print("W: [");
        for (String w : W) {
            System.out.print((w != null ? w : "null") + " ");
        }
        System.out.println("]");

        // 返回pSet和W
        return new Object[]{id, pSet, W};
    }

    // 获取三个列表中所有值的平均值
    public double getAverageUpdateTime() {
        return calculateAverage(totalUpdateTimes);
    }

    public double getAverageClientTime() {
        return calculateAverage(clientSearchTimes);
    }

    public double getAverageServerTime() {
        return calculateAverage(serverSearchTimes);
    }

    // 计算平均值的方法
    private double calculateAverage(List<Double> times) {
        if (times.isEmpty()) {
            return 0.0;  // 避免除以 0
        }
        double sum = 0.0;
        for (double time : times) {
            sum += time;
        }
        return sum / times.size();
    }
    public static void main(String[] args) throws Exception {
        TDSC2023 tdsc2023 = new TDSC2023(128, 200, 500, 16, 2);

        // 测试 update 操作
        for (int i = 0; i < 10; i++) {
            long[] pSet = {i, i + 1};
            String[] W = {"keyword1", "keyword2"};
            int[] files = {i};
            tdsc2023.update(pSet, W, "add", files, 1000);
        }

        // 测试 search 操作
        BigInteger R_min = BigInteger.valueOf(1);
        BigInteger R_max = BigInteger.valueOf(100);
        String[] WQ = {"keyword1", "keyword2"};
        for (int i = 0; i < 10; i++) {
            tdsc2023.Search(R_min, R_max, WQ);
        }

        // 输出平均值
        System.out.println("平均更新耗时: " + tdsc2023.getAverageUpdateTime() + " ms");
        System.out.println("平均查询客户端耗时: " + tdsc2023.getAverageClientTime() + " ms");
        System.out.println("平均查询服务器耗时: " + tdsc2023.getAverageServerTime() + " ms");
        System.out.println("程序结束。");
    }


}