package org.davidmoten.quadtree;

import org.davidmoten.bpc.BPCGenerator;
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
import java.util.stream.Stream;

public class DSSE {
    // 数据集路径
    private static final String FILE_PATH = "C:\\Users\\Admin\\Desktop\\SpatialDataSet\\osmfiles\\birminghan_large_final_1.csv";
    private static final int MAX_FILES = 1 << 20; // 2^20

    private static BigInteger minX = BigInteger.ZERO;
    private static BigInteger minY = BigInteger.ZERO;
    private static BigInteger maxX = BigInteger.valueOf(1L << 17); // 2^17
    private static BigInteger maxY = BigInteger.valueOf(1L << 17); // 2^17

    //DSSE参数
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int maxnums_w = 200;
    private String KS; // 主密钥
    private int lambda; // 公共参数
    private Map<String, Integer> T; // 计数器表
    private Map<String, BigInteger> SDB; // 存储空间前缀的加密数据库
    private Map<String, BigInteger> KDB; // 存储关键字的加密数据库
    private DPRF dprf; // 使用 DPRF 进行密钥派生
    private HashFunctions hashFunctions; // 哈希函数类实例
    private HomomorphicEncryption homomorphicEncryption; // 同态加密类实例

    private int order; // 四叉树深度
    private Quadtree quadtree;
    private BPCGenerator bpcGenerator;

    // 构造函数
    public DSSE(int securityParameter) throws Exception {
        this.KS = generateMasterKey(securityParameter);
        this.homomorphicEncryption = new HomomorphicEncryption(MAX_FILES); // 初始化同态加密实例
        this.dprf = new DPRF(maxnums_w);

        this.T = new HashMap<>();
        this.SDB = new HashMap<>();
        this.KDB = new HashMap<>();
        this.hashFunctions = new HashFunctions();

        this.order = 17; // |P| = 17*2
        this.quadtree = new Quadtree(minX, minY, maxX, maxY, this.order);
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


    private List<String> preCode(BigInteger[] pSet) {
        System.out.println("points: [" + pSet[0]+","+pSet[1]+"]");
        // 计算点的 四叉树 编码值，然后将编码值转换为Biginteger
        String quadtreeEncoding = this.quadtree.getEncodeString(pSet[0], pSet[1]);
        System.out.println("quadtreeEncoding: " + quadtreeEncoding);
        BigInteger quadtreeIndex = new BigInteger(quadtreeEncoding, 2); // Base 2 (binary) to BigInteger
        // 打印 Hilbert 索引的值
        System.out.println("quadtree Index (BigInteger): " + quadtreeIndex);

        int requiredLength = 2 * order;

        List<String> prefixList = new ArrayList<>();

        // 从完整的前缀开始，逐步减少长度
        for (int i = 0; i <= requiredLength; i++) {
            String prefix = quadtreeEncoding.substring(0, requiredLength - i);
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
        //生成min到max的所有Bigint
        BigInteger[] R = Stream.iterate(min, n -> n.add(BigInteger.ONE))
                .limit(max.subtract(min).add(BigInteger.ONE).intValueExact())
                .toArray(BigInteger[]::new);


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
    public void update(BigInteger[] pSet, String[] W, BigInteger B, int Cc) throws Exception {
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

            // 使用 DPRF.Derive获取Tp_c_plus_1
            String Tp_c_plus_1 = dprf.Derive(new SecretKeySpec(Kp.getBytes(StandardCharsets.UTF_8), HMAC_SHA256), c + 1);


            T.put(p, c + 1);

            String UTp_c_plus_1 = hashFunctions.H1(KpPrime, Tp_c_plus_1);
            BigInteger skp_c1 = hashFunctions.H2(KpPrime, c + 1);
            BigInteger ep_c1 = homomorphicEncryption.enc(skp_c1, B);

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
//            System.out.println("Updated Counter for keyword " + w + ": " + T.get(w));

            String UTw_c1 = hashFunctions.H1(KwPrime, Tw_c1);
            BigInteger skw_c1 = hashFunctions.H2(KwPrime, c + 1);
            BigInteger ew_c1 = homomorphicEncryption.enc(skw_c1, B);

            KDB.put(UTw_c1, ew_c1);
        }

        System.out.println("Update operation completed.");
    }
    // 整合客户端和服务器的搜索操作
    public BigInteger Search(BigInteger R_min, BigInteger R_max, String[] WQ) throws Exception {
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
        return BR;
    }

    public static Object[] GetRandomItem(int W_num) throws IOException {
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
        BigInteger x = new BigInteger(columns[1]);
        BigInteger y = new BigInteger(columns[2]);
        BigInteger[] pSet = new BigInteger[]{x, y};

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


    public static void main(String[] args) throws Exception {
        // 初始化DSSE系统
        int securityParameter = 128;
        DSSE dsse = new DSSE(securityParameter);

        // 进行主流程测试
        while (true) {
            // 模拟从数据集中获取一项数据
            Object[] result = DSSE.GetRandomItem(12);
            if (result == null) {
                System.out.println("获取数据失败。");
                continue;
            }

            BigInteger id = (BigInteger) result[0];
            BitSet bitMap = new BitSet(MAX_FILES); // 创建一个长度为2^20的 BitSet, 支持最多2^20个文件
            dsse.homomorphicEncryption.setBit(bitMap, id.intValue()); // 将id转换为长度为2^20，第id位为1的bitmap
            BigInteger B = dsse.homomorphicEncryption.processBitMapExistence(bitMap); // 将位图序列转换为BigInteger值

            // 对此位置执行同态加法以产生反向操作（将1变为0）,update时，将该值视为B
            BigInteger B_deleteId = dsse.homomorphicEncryption.getN().subtract(B);

            BigInteger[] pSet = (BigInteger[]) result[1];
            String[] W = (String[]) result[2];

            // 创建 DecimalFormat 实例以格式化毫秒输出
            DecimalFormat df = new DecimalFormat("#.00");

            // 统计 update 方法的运行时间
            long startTimeUpdate = System.nanoTime();
            dsse.update(pSet, W, B, maxnums_w);
            long endTimeUpdate = System.nanoTime();
            double durationUpdateMs = (endTimeUpdate - startTimeUpdate) / 1_000_000.0;
            System.out.println("Add Update方法运行时间: " + df.format(durationUpdateMs) + " 毫秒");

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

            // 统计 Search 方法的运行时间
            long startTimeSearch = System.nanoTime();
            BigInteger BR = dsse.Search(R_min, R_max, WQ);
            long endTimeSearch = System.nanoTime();
            double durationSearchMs = (endTimeSearch - startTimeSearch) / 1_000_000.0;
            System.out.println("Search方法运行时间: " + df.format(durationSearchMs) + " 毫秒");
            // 返回最终解密后的位图信息 BR
            HomomorphicEncryption.findIndexesOfOne(BR);

            // 统计 del update 方法的运行时间
            long startTimeUpdate_delete = System.nanoTime();
            dsse.update(pSet, W, B_deleteId, maxnums_w);
            long endTimeUpdate_delete = System.nanoTime();
            double durationUpdateMs_delete = (endTimeUpdate_delete - startTimeUpdate_delete) / 1_000_000.0;
            System.out.println("Del Update方法运行时间: " + df.format(durationUpdateMs_delete) + " 毫秒");

            // 继续进行下一个循环
            System.out.print("是否继续？(yes/no): ");
            String response = scanner.nextLine();
            if (!response.equalsIgnoreCase("yes")) {
                break;
            }
        }

        System.out.println("程序结束。");
    }


}