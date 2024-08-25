package org.davidmoten.hilbert.app;

import org.davidmoten.hilbert.HilbertComponent.HilbertCurve;

import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class DSSE {

    // 数据集路径
    private static final String FILE_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\Combined\\2^n_DataSet.csv";
    private String KS; // 主密钥
    private int n; // 公共参数
    private Map<String, Integer> T; // 计数器表
    private Map<String, String> SDB; // 存储空间前缀的加密数据库
    private Map<String, String> KDB; // 存储关键字的加密数据库
    private KeyDerivationFunction kdf; // 用于派生密钥
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
        this.kdf = new KeyDerivationFunction(new SecretKeySpec(KS.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        this.hashFunctions = new HashFunctions();
        this.homomorphicEncryption = new HomomorphicEncryption(securityParameter); // 初始化同态加密实例

        this.order = 20; // |P| = 20
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

    //    // 搜索操作
//    public void search(String R, String[] WQ) throws Exception {
//        List<String> BPC = preCover(R);
//
//        for (String w : WQ) {
//            String[] keys = F_K_sigma(KS, w);
//            String Kw = keys[0];
//            String KwPrime = keys[1];
//            int c = getCounter(w);
//            if (c == -1) {
//                return;
//            }
//
//            String STw = String.valueOf(kdf.DelKey(Kw, String.valueOf(c)));
//            sendToServer(KwPrime, STw, c);
//        }
//
//        for (String p : BPC) {
//            String[] keys = F_K_sigma(KS, p);
//            String Kp = keys[0];
//            String KpPrime = keys[1];
//            int c = getCounter(p);
//            if (c == -1) {
//                return;
//            }
//
//            String STp = String.valueOf(kdf.DelKey(Kp, String.valueOf(c)));
//            sendToServer(KpPrime, STp, c);
//        }
//
//        processServerResponse();
//    }
    // 更新操作
    public void update(long[] pSet, String[] W, String BitMap_Existence, String BitMap_Op, int Cc) throws Exception {
        List<String> P = preCode(pSet);

        for (String p : P) {
            String[] keys = F_K_sigma(KS, p);
            String Kp = keys[0];
            String KpPrime = keys[1];
            int c = T.getOrDefault(p, -1);

            String Tp_c_plus_1 = kdf.Derive(Kp, Cc + 1);
            T.put(p, c + 1);

            String UTp_c_plus_1 = hashFunctions.H1(Kp, Tp_c_plus_1);
            String skp_c1 = hashFunctions.H2(Kp, c + 1);
            int ep_c1 = homomorphicEncryption.enc(homomorphicEncryption.generateSecretKey(), Integer.parseInt(BitMap_Existence));

            SDB.put(UTp_c_plus_1, String.valueOf(ep_c1));
        }

        for (String w : W) {
            String[] keys = F_K_sigma(KS, w);
            String Kw = keys[0];
            String KwPrime = keys[1];
            int c = T.getOrDefault(w, -1);

            String Tw_c1 = kdf.Derive(Kw, Cc + 1);
            T.put(w, c + 1);

            String UTw_c1 = hashFunctions.H1(Kw, Tw_c1);
            String skw_c1 = hashFunctions.H2(Kw, c + 1);
            int ew_c1 = homomorphicEncryption.enc(homomorphicEncryption.generateSecretKey(), Integer.parseInt(BitMap_Existence));

            KDB.put(UTw_c1, String.valueOf(ew_c1));
        }
    }

    private List<String> preCode(long[] pSet) {
        // 计算点的 Hilbert 索引
        BigInteger pointHilbertIndex = this.hilbertCurve.index(pSet);
        // 将 Hilbert 索引转换为长度为20的二进制串
        String hilbertBinary = String.format("%20s", pointHilbertIndex.toString(2)).replace(' ', '0');

        // 将二进制串拆分成单个字符并存储到列表中
        List<String> binaryList = new ArrayList<>(Arrays.asList(hilbertBinary.split("")));

        return binaryList;
    }


//    private long[] preCode(long[] pSet) {
//        return pSet;
//    }
    // 用于处理调用findPrefixes后的结果
    public List<String> preCover(long min, long max) {
        long[] R = {min, max};
        List<Long> results = this.bpcGenerator.GetBPCValueList(R);
        List<String> BinaryResults = new ArrayList<>();
        System.out.println("BPCValue for Results: " + results);
        for (Long result : results) {
            String bpc_string = this.bpcGenerator.toBinaryStringWithStars(result, order,this.bpcGenerator.shiftCounts.get(result));
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


    public static void GetRandomItem() throws IOException {
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
            return;
        }

        // 分割该行，提取数据
        String[] columns = selectedRow.split(",");

        // id 转换为20位的二进制串
        long Id = Long.parseLong(columns[0]);
        String IdBinary = String.format("%20s", Long.toBinaryString(Id)).replace(' ', '0');

        // latitude 和 longitude 转换为 long[] pSet
        long latitude = Math.round(Double.parseDouble(columns[2]));
        long longitude = Math.round(Double.parseDouble(columns[1]));
        long[] pSet = new long[]{latitude, longitude};

        // key1 到 key12 转换为 String[] W
        String[] W = new String[12];
        System.arraycopy(columns, 3, W, 0, 12);

        // 输出结果
        System.out.println("id (Binary 20-bit): " + IdBinary);
        System.out.println("pSet: [latitude=" + pSet[0] + ", longitude=" + pSet[1] + "]");
        System.out.print("W: [");
        for (String w : W) {
            System.out.print(w + " ");
        }
        System.out.println("]");
    }


    public static void main(String[] args) throws Exception {
        // 初始化DSSE系统
        int securityParameter = 128;
        DSSE dsse = new DSSE(securityParameter);
        // 给定Hilbert Index Range
        long R1 = 8; // 示例的最小值
        long R2 = 15; // 示例的最大值
        // 生成覆盖范围的前缀
        List<String> preCoverResult = dsse.preCover(R1, R2);
        System.out.println("PreCover Result: " + preCoverResult);


//        // 执行一次更新操作
//        String[] pSet = {"p1", "p2"};
//        String[] W = {"w1", "w2", "w3"};
//        String BitMap_Existence = "block_data";
//        int Cc = 100;
//        dsse.update(pSet, W, BitMap_Existence, Cc, "ins");
//        // 执行一次搜索操作
//        String R = "range1";
//        String[] WQ = {"w1", "w2", "w3"};
//        dsse.search(R, WQ);
    }

}

