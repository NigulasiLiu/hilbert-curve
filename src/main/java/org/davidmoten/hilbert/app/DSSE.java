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
import java.text.DecimalFormat;
import java.util.*;

public class DSSE {
    // 数据集路径
    private static final String FILE_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large_final_1.csv";
    private static final int MAX_FILES = 1 << 20; // 2^20
    private String KS; // 主密钥
    private int lambda; // 公共参数
    private Map<String, Integer> T; // 计数器表
    private Map<String, BigInteger> SDB; // 存储空间前缀的加密数据库
    private Map<String, BigInteger> KDB; // 存储关键字的加密数据库
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
        this.homomorphicEncryption = new HomomorphicEncryption(MAX_FILES); // 初始化同态加密实例
        this.dprf = new DPRF(new SecretKeySpec(KS.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), securityParameter, 200); // 使用 DPRF 替代 KeyDerivationFunction

        this.T = new HashMap<>();
        this.SDB = new HashMap<>();
        this.KDB = new HashMap<>();
        this.hashFunctions = new HashFunctions();

        this.order = 17; // |P| = 17*2
        this.dimension = 2;
        this.hilbertCurve = HilbertCurve.bits(order).dimensions(dimension);
        this.bpcGenerator = new BPCGenerator(order * 2);//因Hilbert曲线编码最大值为2^(2*order)
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

    // 整合客户端和服务器的搜索操作
    public BigInteger searchProtocol(BigInteger R_min, BigInteger R_max, String[] WQ) throws Exception {
        // 客户端：生成搜索请求
        List<String> BPC = preCover(R_min, R_max);
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
            Key STp = dprf.DelKey(new SecretKeySpec(Kp.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), c);
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
            Key STw = dprf.DelKey(new SecretKeySpec(Kw.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), c);
            clientRequest_w.add(new Object[]{KwPrime, STw, c});
        }

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
//                String Ti = dprf.Derive(ST, i);
                String Ti = "HmacSHA256";
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
//                String Ti = dprf.Derive(ST, i);
                String Ti = "HmacSHA256";
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
        return BR;
    }

    // 更新操作
    public void update(long[] pSet, String[] W, BigInteger B, int Cc) throws Exception {
        System.out.println("Starting update operation...");
        System.out.println("Input pSet: " + Arrays.toString(pSet));
        System.out.println("Input W: " + Arrays.toString(W));
//        System.out.println("BitMap: " + B);
//        System.out.println("BitMap_Op: " + BitMap_Op);
        System.out.println("Cc: " + Cc);

        List<String> P = preCode(pSet);
        System.out.println("PreCode P: " + P);

        for (String p : P) {
            String[] keys = F_K_sigma(KS, p);
            String Kp = keys[0];
            String KpPrime = keys[1];
            int c = T.getOrDefault(p, -1);
//            System.out.println("Processing prefix: " + p);
//            System.out.println("Kp: " + Kp);
//            System.out.println("KpPrime: " + KpPrime);
//            System.out.println("Counter c: " + c);

            // 使用 DPRF.Derive获取Tp_c_plus_1
//            String Tp_c_plus_1 = dprf.Derive(new SecretKeySpec(Kp.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), c + 1);
            String Tp_c_plus_1 = "HmacSHA256";
//            System.out.println("Delegated Key: " + Base64.getEncoder().encodeToString(delegatedKey.getEncoded()));
//            System.out.println("Tp_c_plus_1: " + Tp_c_plus_1);

            T.put(p, c + 1);
//            System.out.println("Updated Counter for prefix " + p + ": " + T.get(p));

            String UTp_c_plus_1 = hashFunctions.H1(KpPrime, Tp_c_plus_1);
            BigInteger skp_c1 = hashFunctions.H2(KpPrime, c + 1);
            BigInteger ep_c1 = homomorphicEncryption.enc(skp_c1, B);
//            System.out.println("UTp_c_plus_1: " + UTp_c_plus_1);
//            System.out.println("skp_c1: " + skp_c1);
//            System.out.println("Encrypted value ep_c1: " + ep_c1);

            SDB.put(UTp_c_plus_1, ep_c1);
        }

        for (String w : W) {
            String[] keys = F_K_sigma(KS, w);
            String Kw = keys[0];
            String KwPrime = keys[1];
            int c = T.getOrDefault(w, -1);
//            System.out.println("Processing keyword: " + w);
//            System.out.println("Kw: " + Kw);
//            System.out.println("KwPrime: " + KwPrime);
//            System.out.println("Counter c: " + c);

            // 使用 DPRF 来生成 DelKey 和 Derive
//            String Tw_c1 = dprf.Derive(new SecretKeySpec(Kw.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), c + 1);
            String Tw_c1 = "HmacSHA256";
//            System.out.println("Delegated Key: " + Base64.getEncoder().encodeToString(delegatedKey.getEncoded()));
//            System.out.println("Tw_c1: " + Tw_c1);

            T.put(w, c + 1);
//            System.out.println("Updated Counter for keyword " + w + ": " + T.get(w));

            String UTw_c1 = hashFunctions.H1(KwPrime, Tw_c1);
            BigInteger skw_c1 = hashFunctions.H2(KwPrime, c + 1);
            BigInteger ew_c1 = homomorphicEncryption.enc(skw_c1, B);
//            System.out.println("UTw_c1: " + UTw_c1);
//            System.out.println("skw_c1: " + skw_c1);
//            System.out.println("Encrypted value ew_c1: " + ew_c1);

            KDB.put(UTw_c1, ew_c1);
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
    public List<String> preCover(BigInteger min, BigInteger max) {
        BigInteger[] R = {min, max};
        List<BigInteger> results = this.bpcGenerator.GetBPCValueList(R);
        List<String> BinaryResults = new ArrayList<>();
        System.out.println("BPC1: " + results);
        for (BigInteger result : results) {
            String bpc_string = this.bpcGenerator.toBinaryStringWithStars(result, order * 2, this.bpcGenerator.shiftCounts.get(result));
            BinaryResults.add(bpc_string);
        }
        System.out.println("BPC2:" + BinaryResults);
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
        BigInteger id = new BigInteger(columns[0]);
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
        BigInteger id = (BigInteger) result[0];
        BitSet bitMap = new BitSet(MAX_FILES); // 创建一个长度为2^20的 BitSet, 支持最多2^20个文件
        dsse.homomorphicEncryption.setBit(bitMap, id.intValue()); // 将id转换为长度为2^20，第id位为1的bitmap
        BigInteger B = dsse.homomorphicEncryption.processBitMapExistence(bitMap); // 将位图序列转换为BigInteger值
        long[] pSet = (long[]) result[1];
        String[] W = (String[]) result[2];

        // 创建 DecimalFormat 实例以格式化毫秒输出
        DecimalFormat df = new DecimalFormat("#.00");

        // 统计 update 方法的运行时间
        long startTimeUpdate = System.nanoTime();
        dsse.update(pSet, W, B, 200);
        long endTimeUpdate = System.nanoTime();
        double durationUpdateMs = (endTimeUpdate - startTimeUpdate) / 1_000_000.0;
        System.out.println("Update方法运行时间: " + df.format(durationUpdateMs) + " 毫秒");

        // 获取用户输入的搜索参数
        Scanner scanner = new Scanner(System.in);

        System.out.print("请输入 R_min: ");
        BigInteger R_min = scanner.nextBigInteger();

        System.out.print("请输入 R_max: ");
        BigInteger R_max = scanner.nextBigInteger();

        System.out.print("请输入关键字数量: ");
        int WQSize = scanner.nextInt();
        scanner.nextLine(); // 清除换行符

        String[] WQ = new String[WQSize];
        for (int i = 0; i < WQSize; i++) {
            System.out.print("请输入关键字 WQ[" + i + "]: ");
            WQ[i] = scanner.nextLine();
        }

        // 统计 searchProtocol 方法的运行时间
        long startTimeSearch = System.nanoTime();
        BigInteger BR = dsse.searchProtocol(R_min, R_max, WQ);
        long endTimeSearch = System.nanoTime();
        double durationSearchMs = (endTimeSearch - startTimeSearch) / 1_000_000.0;
        System.out.println("SearchProtocol方法运行时间: " + df.format(durationSearchMs) + " 毫秒");
        // 返回最终解密后的位图信息 BR
        HomomorphicEncryption.findIndexesOfOne(BR);
    }


}