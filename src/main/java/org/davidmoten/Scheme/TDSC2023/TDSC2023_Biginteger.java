package org.davidmoten.Scheme.TDSC2023;

import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.davidmoten.BPC.BPCGenerator;
import org.davidmoten.DataProcessor.DataSetAccess;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;
import org.davidmoten.Scheme.TDSC2023.DPRF.BRC_DPRF;
import org.davidmoten.Scheme.TDSC2023.DPRF.DPRF_Simplify;

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

import static org.davidmoten.Experiment.UseDataAccessClass.BRQComparisonInput.generateHilbertMatrix;


public class TDSC2023_Biginteger {
    // 列表用于存储 update 和 search 的时间
    public List<Double> totalUpdateTimes = new ArrayList<>();    // 存储 update 操作的总耗时
    public List<Double> clientSearchTimes = new ArrayList<>();         // 存储客户端 search 操作的时间
    public List<Double> serverSearchTimes = new ArrayList<>();         // 存储服务器 search 操作的时间
    public static final int LAMBDA = 128;  // 安全参数 λ    // 缓存的MessageDigest实例
    private final byte[] intBuffer = new byte[4]; // 用于缓存int转byte的缓冲区

    private static final int HASH_OUTPUT_LENGTH = 16; // 128 位（16 字节）
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HASH_ALGORITHM = "SHA-256";
    private String KS; // 主密钥
    private int lambda; // 公共参数
    public Map<String, Integer> T; // 计数器表
    public ConcurrentHashMap<String, BigInteger> PDB;
    public ConcurrentHashMap<String, BigInteger> KDB;
    private BRC_DPRF dprf;
    public final Mac hmac;

    private int dimension; // 2维数据
    private int order; // Hilbert curve 阶数
    public HilbertCurve hilbertCurve;

    //    private int rangeLimit; // 关键字最大数量
//    private String filePath; // 数据集路径
    private int maxFiles; // 最大文件数
    private BigInteger n; // 最大文件数

    // 修改后的构造函数
    public TDSC2023_Biginteger(int securityParameter, int rangeLimit, int maxFiles, int order, int dimension) throws Exception {
//        this.rangeLimit = rangeLimit;
//        this.filePath = filePath;
        this.hmac = Mac.getInstance(HMAC_ALGORITHM);
        this.maxFiles = maxFiles;
        this.n = BigInteger.valueOf(2).pow(maxFiles);

        this.KS = generateMasterKey(securityParameter);
//        this.homomorphicEncryption = new HomomorphicEncryption(maxFiles); // 初始化同态加密实例
//        this.dprfSimplify = new DPRF_Simplify(rangeLimit);
        this.dprf = new BRC_DPRF(lambda, (int) Math.log(maxFiles));

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
        Map<Integer, List<BigInteger>> resultMap = BPCGenerator.GetBPCValueMap(R, this.order * 2);
//        System.out.println("BPC:" + BPCGenerator.convertMapToPrefixString(resultMap,this.order*2));
        return BPCGenerator.convertMapToPrefixString(resultMap, this.order * 2);
    }

    public List<String> preCover(BigInteger[][] Matrix) {
        //生成min到max的所有Bigint
        BigInteger[] R = new BigInteger[Matrix.length * Matrix[0].length];
        for (int i = 0; i < Matrix.length; i++) {
            System.arraycopy(Matrix[i], 0, R, i * Matrix[0].length, Matrix[0].length);
        }

        // 获取BPC结果（包括分组）
        Map<Integer, List<BigInteger>> resultMap = BPCGenerator.GetBPCValueMap(R, this.order * 2);
//        System.out.println("BPC:" + BPCGenerator.convertMapToPrefixString(resultMap,this.order*2));
        return BPCGenerator.convertMapToPrefixString(resultMap, this.order * 2);
    }

    public double update(long[] pSet, String[] W, String op, int[] files, int CounterLimits) throws Exception {
        byte[] combinedKey;
        byte[] Kp = new byte[LAMBDA / 8];
        byte[] Kp_prime = new byte[LAMBDA / 8];
        // 记录开始时间
        long startTime = System.nanoTime();
        List<String> P = preCode(pSet);
        for (String p : P) {
            long startTime1 = System.nanoTime();            // 记录单次循环的开始时间
            long loopStartTime = System.nanoTime();
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            //Kp = Arrays.copyOfRange(combinedKey, 0, LAMBDA / 8);
            //Kp_prime = Arrays.copyOfRange(combinedKey, LAMBDA / 8, LAMBDA / 4); // 假设 LAMBDA / 4 是所需的长度
            System.arraycopy(combinedKey, 0, Kp, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kp_prime, 0, LAMBDA / 8);

            int c = T.getOrDefault(p, -1);

            // 使用 DPRF_Simplify.Derive获取Tp_c_plus_1
//            byte[] Tp_c_plus_1 = dprfSimplify.Derive(new SecretKeySpec(Kp, HMAC_ALGORITHM), c + 1);
            byte[] Tp_c_plus_1 = dprf.newDerivedKey(Kp, c + 1);
//            System.out.println("Tp_c_plus_1:"+ Arrays.toString(Tp_c_plus_1));
            T.put(p, c + 1);
            byte[] UTp_c_plus_1 = hashFunction1(Kp_prime, Tp_c_plus_1);
//            System.out.println("UTp_c_plus_1:"+ Arrays.toString(UTp_c_plus_1));
            BigInteger skp_c1 = hashFunction2(Kp_prime, c + 1);

            long startTime2 = System.nanoTime();            // 设置位图
//            BigInteger B = BigInteger.ZERO;  // 使用 BigInteger 作为位图
//            for (int fileIndex : files) {
//                if ("add".equals(op)) {
//                    B = B.setBit(fileIndex);  // 添加操作，设置bsa中相应位为1
//                } else if ("del".equals(op)) {
//                    B = B.setBit(fileIndex).not();  // 删除操作，设置bsa中相应位为1,然后取反
//                }
//            }
            BitSet bitSet = new BitSet();
            for (int fileIndex : files) {
                bitSet.set(fileIndex);
            }

//            BigInteger ep_c1 = skp_c1.add(B).mod(n);
            long startTime4 = System.nanoTime();
            PDB.put(new String(UTp_c_plus_1, StandardCharsets.UTF_8),
                    skp_c1.add(new BigInteger(1, bitSet.toByteArray())).mod(n));
        }
        long pTime = System.nanoTime();
        for (String w : W) {
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            //Kp = Arrays.copyOfRange(combinedKey, 0, LAMBDA / 8);
            //Kp_prime = Arrays.copyOfRange(combinedKey, LAMBDA / 8, LAMBDA / 4); // 假设 LAMBDA / 4 是所需的长度
            System.arraycopy(combinedKey, 0, Kp, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kp_prime, 0, LAMBDA / 8);
            int c = T.getOrDefault(w, -1);

            // 使用 DPRF_Simplify 来生成 delKey 和 Derive
//            byte[] Tw_c1 = dprfSimplify.Derive(new SecretKeySpec(Kp, HMAC_ALGORITHM), c + 1);
            byte[] Tw_c1 = dprf.newDerivedKey(Kp, c + 1);
            T.put(w, c + 1);
            byte[] UTw_c1 = hashFunction1(Kp_prime, Tw_c1);
            BigInteger skw_c1 = hashFunction2(Kp_prime, c + 1);
            // 设置位图
//            BigInteger B = BigInteger.ZERO;  // 使用 BigInteger 作为位图
//            for (int fileIndex : files) {
//                if ("add".equals(op)) {
//                    B = B.setBit(fileIndex);  // 添加操作，设置bsa中相应位为1
//                } else if ("del".equals(op)) {
//                    B = B.setBit(fileIndex).not();  // 删除操作，设置bsa中相应位为1,然后取反
//                }
//            }
            // 使用 BitSet 构造掩码
            BitSet bitSet = new BitSet();
            for (int fileIndex : files) {
                bitSet.set(fileIndex);
            }
//            BigInteger ew_c1 = skw_c1.add(B).mod(n);
//            KDB.put(new String(UTw_c1, StandardCharsets.UTF_8), ew_c1);
            KDB.put(new String(UTw_c1, StandardCharsets.UTF_8),
                    skw_c1.add(new BigInteger(1, bitSet.toByteArray())).mod(n));
        }
        long wTime = System.nanoTime();
        // 输出总耗时
        double totalLoopTimeMs = (System.nanoTime() - startTime) / 1e6;
//        System.out.println("TDSC2023_BITSET Total update time: " + totalLoopTimeMs + " ms).");
//        System.out.println("TDSC2023_BITSET ptime: " + (pTime-startTime) / 1e6 + " ms.");
//        System.out.println("TDSC2023_BITSET wtime: " + (wTime-pTime) / 1e6 + " ms.");
        // 存储到列表中
        totalUpdateTimes.add(totalLoopTimeMs);
//        System.out.println("Update operation completed.");
        return totalLoopTimeMs;
    }

    public BigInteger Search(BigInteger[][] Matrix, String[] WQ) throws Exception {
        byte[] combinedKey;
        byte[] Kp = new byte[LAMBDA / 8];
        byte[] Kp_prime = new byte[LAMBDA / 8];
        // 客户端：生成搜索请求
        long startTime = System.nanoTime();
        List<String> BPC = preCover(Matrix);
        long client_time2 = System.nanoTime();
        BigInteger SumP = BigInteger.ZERO;
        boolean exist = true;
        long client_time_for_plus = 0;
        long server_time_for_plus = 0;
        List<Integer> pCounterList = new ArrayList<>();
        List<Integer> wCounterList = new ArrayList<>();
        for (String p : BPC) {
            long client_loop_start = System.nanoTime();
            // 客户端处理
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            System.arraycopy(combinedKey, 0, Kp, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kp_prime, 0, LAMBDA / 8);
            int c = T.getOrDefault(p, -1);
            if (c == -1) {
//                System.out.println("没有匹配的结果");
                continue;
            }
//            Key STp = dprfSimplify.delKey(Kp, c);
            List<BRC_DPRF.Trapdoor> STp = dprf.delKey(Kp, c);
            long client_loop_end = System.nanoTime();
            client_time_for_plus += client_loop_end - client_loop_start;
            // 服务器处理
            BigInteger SumPe = BigInteger.ZERO;
            // 从 c 开始迭代
            for (int i = c; i >= 0; i--) {
//                byte[] Ti = dprfSimplify.Derive(STp, i);
                byte[] Ti = dprf.deriveByIndex(i, STp);
                byte[] UTi = hashFunction1(Kp_prime, Ti);

                BigInteger e_p_i = PDB.get(new String(UTi, StandardCharsets.UTF_8));
                if (e_p_i == null) {
//                    System.out.println("e_p_i = null");
                    break;
                } else {
                    pCounterList.add(i);
                    SumPe = SumPe.add(e_p_i).mod(n);
                    PDB.remove(new String(UTi, StandardCharsets.UTF_8)); // 将密文标记为已删除
                }
            }
//            byte[] Tc = dprfSimplify.Derive(STp, c);
            byte[] Tc = dprf.deriveByIndex(c, STp);
            byte[] UTc = hashFunction1(Kp_prime, Tc);
            PDB.put(new String(UTc, StandardCharsets.UTF_8), SumPe); // 将最新的索引更新至UTc
            SumP = SumP.add(SumPe).mod(n);
            long server_loop_end = System.nanoTime();
            server_time_for_plus += (server_loop_end - client_loop_end);
//            System.out.print(p + ":" + ((server_loop_end - client_loop_end) / 1e6) + "ms");
        }
        List<BigInteger> SumWList = new ArrayList<>();
        for (String w : WQ) {
            long client_loop_start = System.nanoTime();
            // 客户端处理
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            byte[] Kw = new byte[LAMBDA / 8];
            byte[] Kw_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);
            int c = T.getOrDefault(w, -1);
            if (c == -1) {
                exist = false;
//                System.out.println("没有匹配"+w+"的结果");
                break;
            }
            List<BRC_DPRF.Trapdoor> STw = dprf.delKey(Kw, c);
//            clientRequest_w.add(new Object[]{Kw_prime, STw, c});
            BigInteger SumWe = BigInteger.ZERO;
            long client_loop_end = System.nanoTime();
            client_time_for_plus += client_loop_end - client_loop_start;
            // 服务器处理
            // 从 c 开始迭代
            for (int i = c; i >= 0; i--) {
//                byte[] Ti = dprfSimplify.Derive(STw, i);
                byte[] Ti = dprf.deriveByIndex(i, STw);
                byte[] UTi = hashFunction1(Kw_prime, Ti);

                BigInteger e_p_i = KDB.get(new String(UTi, StandardCharsets.UTF_8));
                if (e_p_i == null) {
                    break;
                } else {
                    wCounterList.add(i);
                    SumWe = SumWe.add(e_p_i).mod(n);
                    KDB.remove(new String(UTi, StandardCharsets.UTF_8)); // 将密文标记为已删除
                }
            }
            byte[] Tc = dprf.deriveByIndex(c, STw);
            byte[] UTc = hashFunction1(Kw_prime, Tc);
            KDB.put(new String(UTc, StandardCharsets.UTF_8), SumWe); // 将最新的索引更新至UTc
            SumWList.add(SumWe);
            long server_loop_end = System.nanoTime();
            server_time_for_plus += (server_loop_end - client_loop_end);
//            System.out.print(w + ":" + ((server_loop_end - client_loop_end) / 1e6) + "ms");
        }
        if (!exist) {
//            long client_time_notexist = System.nanoTime();
            // 存储到列表中
            double msclient_time = (client_time2 - startTime + client_time_for_plus) / 1e6;
            double msserver_time = server_time_for_plus / 1e6;
            clientSearchTimes.add(msclient_time);
            serverSearchTimes.add(msserver_time);
            return BigInteger.ZERO;
        }
        //客户端解密阶段
        long client_time_dec = System.nanoTime();
        BigInteger SumP_sk = BigInteger.ZERO;
        for (String p : BPC) {
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            Kp_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, LAMBDA / 8, Kp_prime, 0, LAMBDA / 8);
            int c = T.getOrDefault(p, -1);
            for (int i : pCounterList) {
                BigInteger skp_i = hashFunction2(Kp_prime, i);
                SumP_sk = SumP_sk.add(skp_i).mod(n);
            }
//            for (int i = c; i >= 0; i--) {
//                BigInteger skp_i = hashFunction2(Kp_prime, i);
//                SumP_sk = SumP_sk.add(skp_i).mod(n);
//            }
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
            int c = T.getOrDefault(w, -1);
            BigInteger SumW_sk = BigInteger.ZERO;
            for (int i : wCounterList) {
                BigInteger skw_i = hashFunction2(Kw_prime, i);
                SumW_sk = SumW_sk.add(skw_i).mod(n);
            }
            // 解密并与前缀部分进行与操作
            BR = BR.and(SumWList.get(j).subtract(SumW_sk).add(n).mod(n));
        }
//        System.out.println("BR2:");
//        findIndexesOfOne(BR);
        long client_time_dec_end = System.nanoTime();
        // 输出总耗时
//        double totalLoopTimeMs = (System.nanoTime() - startTime) / 1e6;
//        System.out.println("TDSC2023_Biginteger Total search time: " + totalLoopTimeMs + " ms).");
        // 客户端部分结束计时
//        long server_time2 = System.nanoTime();
        // 输出客户端和服务器端的时间消耗
        double msclient_time = ((client_time2 - startTime + client_time_for_plus) + (client_time_dec_end - client_time_dec)) / 1e6;
        double msserver_time = server_time_for_plus / 1e6;
//        double total_time = msclient_time + msserver_time;
//        System.out.println("TDSC: Client time part 1: " + msclient_time1 + " ms, Server time: " + msserver_time + " ms, Total time: " + total_time + " ms");

        // 存储到列表中
        clientSearchTimes.add(msclient_time);
        serverSearchTimes.add(msserver_time);
        return BR;
    }

    public static void main(String[] args) throws Exception {
        // 清除"最大耗时-最小耗时"对数,便于计算合理的平均值
        int delupdatetimes = 1;
        // 需要在内存中存储，所以需要插入 updatetime 个 Object
        int updatetimes = 100000;
        int batchSize = 500; // 每次处理 x 个更新
        // 数据集大小为 1 Million 个条目
        int objectnums = 1000000;
        // 相同元素(关键字或者位置 point)的最大数量为 10W
        int rangePredicate = 1000000;
        int[] maxfilesArray = {1 << 20}; // 20, 1 << 18, 1 << 16, 1 << 14, 1 << 12
        int[] hilbertOrders = {12};

        int edgeLength = 1 << hilbertOrders[0];
        int div = 100;

        DataSetAccess dataSetAccess = new DataSetAccess(hilbertOrders[0]);
        dataSetAccess.generateFiles(objectnums, maxfilesArray[0]);
        dataSetAccess.generateKwItems(8000, 12, objectnums);
        dataSetAccess.generatePoints(edgeLength, "multi-gaussian", objectnums);

        TDSC2023_Biginteger tdsc2023 = new TDSC2023_Biginteger(128, rangePredicate, maxfilesArray[0], hilbertOrders[0], 2);

        // 初始化搜索耗时统计
        List<Double> tdscSearchTimes = new ArrayList<>();
        // 随机数据生成器
        Random random = new Random();
        // 执行更新操作
        for (int i = 0; i < updatetimes; i++) {
            int randomIndex = random.nextInt(objectnums);
            long[] pSet = dataSetAccess.pointDataSet[randomIndex];
            String[] W = dataSetAccess.keywordItemSets[randomIndex];
            int[] files = new int[]{dataSetAccess.fileDataSet[randomIndex]};
            tdsc2023.update(pSet, W, "add", files, rangePredicate);

            if ((i + 1) % batchSize == 0) {
                System.out.println("Completed batch " + (i + 1) / batchSize + " of updates.");
                // 打印平均搜索时间
                System.out.printf("Update完成，平均更新时间: | RSKQ_Biginteger: |%-10.6f|ms\n",
                        tdsc2023.getAverageUpdateTime());
                System.out.printf("更新数量: | PDB: |%d| KDB: |%d|\n",
                        tdsc2023.getPDBSize(), tdsc2023.getKDBSize());
            }
        }
        // 移除初始的异常值
        for (int i = 0; i < delupdatetimes; i++) {
            tdsc2023.removeExtremesUpdateTime();
        }
        // 打印平均搜索时间
        System.out.printf("Update完成，平均更新时间: | RSKQ_Biginteger: |%-10.6f|ms\n",
                tdsc2023.getAverageUpdateTime());
        tdsc2023.clearUpdateTime();
        // 用户交互部分
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\n请选择操作 (1: 搜索, 2: 打印键数量, -1: 退出): ");
            int choice = scanner.nextInt();

            if (choice == -1) {
                System.out.println("程序已退出。");
                break;
            }

            switch (choice) {
                case 1: // 搜索操作
                    System.out.print("请输入搜索次数 (正整数): ");
                    int searchtimes = scanner.nextInt();

                    if (searchtimes <= 0) {
                        System.out.println("搜索次数必须是正整数。请重新选择操作。");
                        continue;
                    }

                    System.out.print("请输入搜索范围 (0-100%): ");
                    int searchEdgeLengthPer = scanner.nextInt();

                    if (searchEdgeLengthPer <= 0 || searchEdgeLengthPer > 100) {
                        System.out.println("搜索范围必须在 0 到 100 之间。请重新选择操作。");
                        continue;
                    }

                    int xstart = random.nextInt(edgeLength * (div - searchEdgeLengthPer) / div);
                    int ystart = random.nextInt(edgeLength * (div - searchEdgeLengthPer) / div);
                    int searchRange = edgeLength * searchEdgeLengthPer / div;

                    BigInteger[][] matrixToSearch = generateHilbertMatrix(
                            tdsc2023.hilbertCurve, xstart, ystart, searchRange, searchRange);

                    // 执行搜索
                    for (int i = 0; i < searchtimes; i++) {
                        int indexToSearch = random.nextInt(objectnums);
                        String[] WQ = dataSetAccess.keywordItemSets[indexToSearch];

                        tdsc2023.Search(matrixToSearch, WQ);
                    }

                    // 移除异常值
                    for (int i = 0; i < delupdatetimes; i++) {
                        tdsc2023.removeExtremesSearchTime();
                    }

                    // 打印平均搜索时间
                    System.out.printf("搜索完成，平均搜索时间: | TDSC2023: |%-10.6f|ms\n",
                            tdsc2023.clientSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                    break;

                case 2: // 打印 PDB 和 KDB 键的数量
                    int pdbKeyCount = tdsc2023.PDB.size();
                    int kdbKeyCount = tdsc2023.KDB.size();
                    System.out.printf("PDB 键的数量: %d, KDB 键的数量: %d\n", pdbKeyCount, kdbKeyCount);
                    break;

                default:
                    System.out.println("无效选项，请重新选择。");
            }
        }

        scanner.close();
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
        Blake2bDigest digest = new Blake2bDigest(HASH_OUTPUT_LENGTH * 8); // 输出位数为 128 位

        // 更新哈希数据
        digest.update(input1, 0, input1.length);
        digest.update(input2, 0, input2.length);

        // 输出哈希值
        byte[] result = new byte[HASH_OUTPUT_LENGTH];
        digest.doFinal(result, 0);
        return result;
    }

    private BigInteger hashFunction2(byte[] input1, int input2) {
        Blake2bDigest digest = new Blake2bDigest(HASH_OUTPUT_LENGTH * 8); // 设置输出位数为 128 位

        // 更新哈希数据
        digest.update(input1, 0, input1.length);

        // 将 int 转为 byte[] 并添加到哈希
        intBuffer[0] = (byte) (input2 >> 24);
        intBuffer[1] = (byte) (input2 >> 16);
        intBuffer[2] = (byte) (input2 >> 8);
        intBuffer[3] = (byte) input2;
        digest.update(intBuffer, 0, intBuffer.length);

        // 输出哈希值
        byte[] result = new byte[HASH_OUTPUT_LENGTH];
        digest.doFinal(result, 0);
        return new BigInteger(1, result);
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

    public void clearUpdateTime() {
        totalUpdateTimes.clear();
    }

    public void clearSearchTime() {
        serverSearchTimes.clear();
        clientSearchTimes.clear();
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

    public int getPDBSize() {
        return this.PDB.size();
    }

    public int getKDBSize() {
        return this.KDB.size();
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

}