package org.davidmoten.hilbert.app;

import org.davidmoten.hilbert.HilbertComponent.HilbertCurve;

import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class DSSE {
    // 数据集路径
    private static final String FILE_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large_final_1.csv";
    private String KS; // 主密钥
    private int n; // 公共参数
    private Map<String, Integer> T; // 计数器表
    private Map<String, String> SDB; // 存储空间前缀的加密数据库
    private Map<String, String> KDB; // 存储关键字的加密数据库
    private DPRF dprf; // 使用 DPRF 进行密钥派生
    private HashFunctions hashFunctions; // 哈希函数类实例
    private HomomorphicEncryption homomorphicEncryption; // 同态加密类实例

    private int order; // Hilbert curve 阶数
    private int dimension; // 2维数据
    private HilbertCurve hilbertCurve;
    private BPCGenerator bpcGenerator;

    // 构造函数
    public DSSE(int securityParameter) throws Exception {
        this.KS = generateMasterKey(securityParameter);
        this.n = initializePublicParameter(securityParameter);
        this.T = new HashMap<>();
        this.SDB = new HashMap<>();
        this.KDB = new HashMap<>();
        this.dprf = new DPRF(new SecretKeySpec(KS.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), 128, 200); // 使用 DPRF 替代 KeyDerivationFunction
        this.hashFunctions = new HashFunctions();
        this.homomorphicEncryption = new HomomorphicEncryption(securityParameter); // 初始化同态加密实例
        this.order = 17; // |P| = 17*2
        this.dimension = 2;
        this.hilbertCurve = HilbertCurve.bits(order).dimensions(dimension);
        this.bpcGenerator = new BPCGenerator(this.order);
    }

    // 生成主密钥
    private String generateMasterKey(int securityParameter) throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        byte[] keyBytes = new byte[securityParameter / 8];
        secureRandom.nextBytes(keyBytes);
        return bytesToHex(keyBytes);
    }

    // 初始化公共参数（此处假设为常量）
    private int initializePublicParameter(int securityParameter) {
        return securityParameter;
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

    // 搜索操作
    public void search(long R_min, long R_max, String[] WQ) throws Exception {
        List<String> BPC = preCover(R_min, R_max);

        for (String w : WQ) {
            String[] keys = F_K_sigma(KS, w);
            String Kw = keys[0];
            String KwPrime = keys[1];
            int c = getCounter(w);
            if (c == -1) {
                return;
            }

            // 使用 DPRF 来生成 DelKey 和 Derive
            Key delegatedKey = dprf.DelKey(new SecretKeySpec(Kw.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), String.valueOf(c));
            String STw = dprf.Derive(delegatedKey, c);
            sendToServer(KwPrime, STw, c);
        }

        for (String p : BPC) {
            String[] keys = F_K_sigma(KS, p);
            String Kp = keys[0];
            String KpPrime = keys[1];
            int c = getCounter(p);
            if (c == -1) {
                return;
            }

            // 使用 DPRF 来生成 DelKey 和 Derive
            Key delegatedKey = dprf.DelKey(new SecretKeySpec(Kp.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), String.valueOf(c));
            String STp = dprf.Derive(delegatedKey, c);
            sendToServer(KpPrime, STp, c);
        }

        processServerResponse();
    }

    // 更新操作
    public void update(long[] pSet, String[] W, String BitMap_Existence, String BitMap_Op, int Cc) throws Exception {
        System.out.println("Starting update operation...");
        System.out.println("Input pSet: " + Arrays.toString(pSet));
        System.out.println("Input W: " + Arrays.toString(W));
        System.out.println("BitMap_Existence: " + BitMap_Existence);
        System.out.println("BitMap_Op: " + BitMap_Op);
        System.out.println("Cc: " + Cc);

        List<String> P = preCode(pSet);
        System.out.println("PreCode P: " + P);

        for (String p : P) {
            String[] keys = F_K_sigma(KS, p);
            String Kp = keys[0];
            String KpPrime = keys[1];
            int c = T.getOrDefault(p, -1);
            System.out.println("Processing prefix: " + p);
            System.out.println("Kp: " + Kp);
            System.out.println("KpPrime: " + KpPrime);
            System.out.println("Counter c: " + c);
//
//            // 使用 DPRF 来生成 DelKey 和 Derive
//            Key delegatedKey = dprf.DelKey(new SecretKeySpec(Kp.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), String.valueOf(c + 1));
//            String Tp_c_plus_1 = dprf.Derive(delegatedKey, Cc + 1);
//            System.out.println("Delegated Key: " + Base64.getEncoder().encodeToString(delegatedKey.getEncoded()));
//            System.out.println("Tp_c_plus_1: " + Tp_c_plus_1);
//
//            T.put(p, c + 1);
//            System.out.println("Updated Counter for prefix " + p + ": " + T.get(p));
//
//            String UTp_c_plus_1 = hashFunctions.H1(Kp, Tp_c_plus_1);
//            String skp_c1 = hashFunctions.H2(Kp, c + 1);
//            int ep_c1 = homomorphicEncryption.enc(homomorphicEncryption.generateSecretKey(), Integer.parseInt(BitMap_Existence));
//            System.out.println("UTp_c_plus_1: " + UTp_c_plus_1);
//            System.out.println("skp_c1: " + skp_c1);
//            System.out.println("Encrypted value ep_c1: " + ep_c1);
//
//            SDB.put(UTp_c_plus_1, String.valueOf(ep_c1));
        }

        for (String w : W) {
            String[] keys = F_K_sigma(KS, w);
            String Kw = keys[0];
            String KwPrime = keys[1];
            int c = T.getOrDefault(w, -1);
            System.out.println("Processing keyword: " + w);
            System.out.println("Kw: " + Kw);
            System.out.println("KwPrime: " + KwPrime);
            System.out.println("Counter c: " + c);

            // 使用 DPRF 来生成 DelKey 和 Derive
            Key delegatedKey = dprf.DelKey(new SecretKeySpec(Kw.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), String.valueOf(c + 1));
            String Tw_c1 = dprf.Derive(delegatedKey, Cc + 1);
            System.out.println("Delegated Key: " + Base64.getEncoder().encodeToString(delegatedKey.getEncoded()));
            System.out.println("Tw_c1: " + Tw_c1);

            T.put(w, c + 1);
            System.out.println("Updated Counter for keyword " + w + ": " + T.get(w));

            String UTw_c1 = hashFunctions.H1(Kw, Tw_c1);
            String skw_c1 = hashFunctions.H2(Kw, c + 1);
            int ew_c1 = homomorphicEncryption.enc(homomorphicEncryption.generateSecretKey(), Integer.parseInt(BitMap_Existence));
            System.out.println("UTw_c1: " + UTw_c1);
            System.out.println("skw_c1: " + skw_c1);
            System.out.println("Encrypted value ew_c1: " + ew_c1);

            KDB.put(UTw_c1, String.valueOf(ew_c1));
        }

        System.out.println("Update operation completed.");
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
        System.out.println("Hilbert Index (Binary): " + hilbertBinary);
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





    // 用于处理调用 findPrefixes 后的结果
    public List<String> preCover(long min, long max) {
        long[] R = {min, max};
        List<Long> results = this.bpcGenerator.GetBPCValueList(R);
        List<String> BinaryResults = new ArrayList<>();
//        System.out.println("BPCValue for Results: " + results);
        for (Long result : results) {
            String bpc_string = this.bpcGenerator.toBinaryStringWithStars(result, order, this.bpcGenerator.shiftCounts.get(result));
            BinaryResults.add(bpc_string);
        }
        return BinaryResults;
    }

    private String[] F_K_sigma(String KS, String input) throws Exception {
        hashFunctions.hmacSHA256.init(new SecretKeySpec(KS.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] result = hashFunctions.hmacSHA256.doFinal(input.getBytes(StandardCharsets.UTF_8));
        String K = Base64.getEncoder().encodeToString(Arrays.copyOfRange(result, 0, result.length / 2));
        String KPrime = Base64.getEncoder().encodeToString(Arrays.copyOfRange(result, result.length / 2, result.length));
        return new String[]{K, KPrime};
    }

    private int getCounter(String input) {
        return T.getOrDefault(input, -1);
    }

    private void sendToServer(String key, String token, int counter) {
        System.out.println("Sending to server: Key = " + key + ", Token = " + token + ", Counter = " + counter);
    }

    private void processServerResponse() {
        System.out.println("Processing server response...");
    }

    public static Object[] GetRandomItem() throws IOException {
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
        String B = columns[0];
        // x 和 y 转换为 long[] pSet
        long x = Long.parseLong(columns[1]);
        long y = Long.parseLong(columns[2]);
        long[] pSet = new long[]{x, y};

        // key1 到 key12 转换为 String[] W
        String[] W = new String[12];
        for (int i = 0; i < 12; i++) {
            // 如果列存在，则取值，否则设为null
            if (columns.length > (i + 3)) {
                W[i] = columns[i + 3];
            } else {
                W[i] = null;
            }
        }

        // 输出结果
        System.out.println("pSet: [x=" + pSet[0] + ", y=" + pSet[1] + "]");
        System.out.print("W: [");
        for (String w : W) {
            System.out.print((w != null ? w : "null") + " ");
        }
        System.out.println("]");

        // 返回pSet和W
        return new Object[]{B,pSet, W};
    }

    public static void main(String[] args) throws Exception {
        // 初始化DSSE系统
        int securityParameter = 128;
        DSSE dsse = new DSSE(securityParameter);

        // 模拟从数据集中获取一项数据
        Object[] result = DSSE.GetRandomItem();
        if (result == null) {
            System.out.println("获取数据失败。");
            return;
        }
        long[] pSet = (long[]) result[1];
        String[] W = (String[]) result[2];
        //update
        dsse.update(pSet,W,"1","1",1);





        // 给定Hilbert Index Range
        long R1 = 38; // 示例的最小值
        long R2 = 47; // 示例的最大值

        // 生成覆盖范围的前缀
        List<String> preCoverResult = dsse.preCover(R1, R2);
        System.out.println("PreCover Result: " + preCoverResult);
    }

}