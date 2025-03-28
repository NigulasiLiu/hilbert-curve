package org.davidmoten.Scheme.RSKQ;


import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.davidmoten.BPC.BPCGenerator;
import org.davidmoten.SpatialDataProcessor.StaticData.DataSetAccess;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.davidmoten.Experiment.TestByUserInput.BRQComparisonInput.generateHilbertMatrix;


public class RSKQ_Biginteger {
    public ConcurrentHashMap<String, CipherTextBytes> PDB; // 服务器存储的密文数据库
    public ConcurrentHashMap<String, CipherTextBytes> KDB; // 服务器存储的密文数据库
    // 列表用于存储 update 和 search 的时间
    public List<Double> totalUpdateTimes = new ArrayList<>();    // 存储 update 操作的总耗时
    public List<Double> clientSearchTimes = new ArrayList<>();   // 存储客户端 search 操作的时间
    public List<Double> serverSearchTimes = new ArrayList<>();   // 存储服务器 search 操作的时间
    private static final int HASH_OUTPUT_LENGTH = 16; // 128 位（16 字节）
    public static final int LAMBDA = 128;  // 安全参数 λ    // 缓存的MessageDigest实例
    private final SecureRandom secureRandom; // 用于生成随机数
    private final MessageDigest messageDigest;
    private final byte[] intBuffer = new byte[4]; // 用于缓存int转byte的缓冲区
    private HashMap<String, ClientStateBytes> SC;  // 客户端状态
    private HashMap<String, BigInteger> SS;  // 服务器状态
//    private ConcurrentHashMap<String, Object[]> PDB; // 服务器存储的密文数据库
//    private ConcurrentHashMap<String, Object[]> KDB; // 服务器存储的密文数据库

    // 伪随机函数的哈希算法
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String HMAC_ALGORITHM = "HmacSHA256";


    //    private int maxnums_w; // 关键字最大数量
    //    private String filePath; // 数据集路径
    private int maxFiles; // 最大文件数
    private int dimension; // 2维数据
    private int order; // Hilbert curve 阶数
    public HilbertCurve hilbertCurve;

    // 修改后的构造函数
    public RSKQ_Biginteger(int maxFiles, int order, int dimension) throws NoSuchAlgorithmException {
        this.messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
//        this.filePath = filePath;
//        this.maxFiles = maxFiles;

        this.SC = new HashMap<>();
        this.SS = new HashMap<>();
        this.PDB = new ConcurrentHashMap<>();
        this.KDB = new ConcurrentHashMap<>();

        this.order = order;
        this.dimension = dimension;
        this.hilbertCurve = HilbertCurve.bits(order).dimensions(dimension);
        this.secureRandom = new SecureRandom(); // 初始化 SecureRandom 实例
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
        Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
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
    private byte[] hashFunction(byte[] input1, byte[] input2) {
        Blake2bDigest digest = new Blake2bDigest(HASH_OUTPUT_LENGTH * 8); // 输出位数为 128 位

        // 更新哈希数据
        digest.update(input1, 0, input1.length);
        digest.update(input2, 0, input2.length);

        // 输出哈希值
        byte[] result = new byte[HASH_OUTPUT_LENGTH];
        digest.doFinal(result, 0);
        return result;
    }
    /**
     * 安全哈希函数 H3, H4, H5
     *
     * @param input1 输入值1
     * @param input2 输入值2
     * @return 哈希后的值
     * @throws NoSuchAlgorithmException
     */
    private byte[] hashFunction(byte[] input1, int input2) {
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
        return result;
    }
    // 生成长度为 λ 的随机数 Rc+1
    private byte[] generateRandomRc() {
        byte[] randomBytes = new byte[LAMBDA / 8]; // λ bits = λ / 8 bytes
        secureRandom.nextBytes(randomBytes);
        return randomBytes;
    }

    public List<String> preCode(long[] pSet) {
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

    /**
     * Setup 初始化操作
     *
     * @param lambda 安全参数
     * @param n      最大支持的文件数量
     */
    public void setup(int lambda, int n) {
        System.out.println("Setup complete with λ = " + lambda + ", n = " + n);
    }

    public BigInteger ObjectSearch(BigInteger R_min, BigInteger R_max, String[] WQ) throws Exception {
        long totalLoopTime = 0; // 初始化总时间变量
        // 客户端：生成搜索请求
        long startTime = System.nanoTime();
        List<String> BPC = preCover(R_min, R_max);
        long precoverTime = System.nanoTime() - startTime;
//        System.out.println("PreCover time: " + precoverTime + " ns (" + (precoverTime/1e6) + " ms).");
        // 累积的客户端和服务器时间
        double totalClientTime = 0.0;
        double totalServerTime = 0.0;
        //存储p位图结果
        BigInteger Sump = BigInteger.ZERO;
        boolean exist = true;
        // 客户端处理前缀集合
        for (String p : BPC) {
            //Sump = Sump.or(search(p));
            // 记录单次循环的开始时间
            long loopStartTime = System.nanoTime();
            // 客户端部分计时
            long client_time1 = System.nanoTime();

            // Step 1: 生成Kw和Kw_prime (客户端)
            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            byte[] Kp = new byte[LAMBDA / 8];
            byte[] Kp_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, 0, Kp, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kp_prime, 0, LAMBDA / 8);

            // Step 2: 获取客户端的当前关键词状态
            ClientStateBytes state = SC.get(p);
            // 客户端部分结束计时
            long client_time2 = System.nanoTime();
            // 若state为null，则跳出循环
            if (state == null) {
//                exist = false;
//                System.out.println("没有匹配:"+p+"的结果");
                double msclient_time1 = (client_time2 - client_time1) / 1e6;
                totalClientTime += msclient_time1;
                totalServerTime += 0; // 没有进行服务器操作，因此设为0
                continue;
            }
            // Step 3: 检查状态是否为null

            // 记录 Rc, c0, c (客户端)
            byte[] Ri = state.getRc();
            int c0 = state.getC0();
            int c = state.getC();


            // 开始服务器部分计时
            long server_time1 = System.nanoTime();

            // Step 1: 检查 SS[Kw] 的状态 (服务器)
            // 如果不存在，则初始化为全0的BigInteger
            BigInteger ep = SS.getOrDefault(new String(Kp, StandardCharsets.UTF_8), BigInteger.ZERO); // 从SS中读取
            // Step 2: 初始化一个空的map来存储结果E (服务器)
            Map<Integer, BigInteger[]> E = new HashMap<>();

            // Step 3: 从c到c0进行循环 (服务器)
//            int Ri = Rc;
            for (int i = c; i >= c0; i--) {
                // Step 4: 计算I
                byte[] I = hashFunction(Kp, Ri);

                // Step 5: 从PDB中检索密文
//                Object[] ciphertext = PDB.get(new String(I, StandardCharsets.UTF_8));
                CipherTextBytes ciphertext = PDB.get(new String(I, StandardCharsets.UTF_8));

                // Step 6: 将检索到的密文存储在E中 (只存储ea和eb)
                BigInteger ea = ciphertext.getEa();
                BigInteger eb = ciphertext.getEb();
                E.put(i - c0, new BigInteger[]{ea, eb});

                // Step 7: 从PDB中移除该密文
                PDB.remove(new String(I, StandardCharsets.UTF_8));

                // Step 8: 更新Ri-1 = C ⊕ H2(Kw, Ri)
                byte[] C = ciphertext.getC();
//                Ri = new BigInteger(C).xor(new BigInteger(hashFunction(Kp, Ri)));
                Ri = xorBytes(C, hashFunction(Kp, Ri));
            }

            // 服务器部分结束计时
            long server_time2 = System.nanoTime();

            // 开始客户端接收并处理部分计时
            long client_time3 = System.nanoTime();

            // Step 1: 初始化匹配文件的位图 (客户端)
            BigInteger bsp = BigInteger.ZERO;

            // Step 2-3: 解密ep (客户端)
            if (!ep.equals(BigInteger.ZERO)) {
                byte[] H5 = hashFunction(Kp_prime, c0);
                BigInteger H5BigIntHash = new BigInteger(1, H5);
                bsp = ep.xor(H5BigIntHash);  // 执行异或操作
            }

            // Step 5: 循环解密每个密文并更新bsw (客户端)
            for (int i = c0; i <= c; i++) {
                BigInteger[] encryptPDBiBitmap = E.get(i - c0);
                BigInteger ea = encryptPDBiBitmap[0];
                BigInteger eb = encryptPDBiBitmap[1];

                BigInteger hashKw_prime_H3 = new BigInteger(1, hashFunction(Kp_prime, i));
//                BigInteger hashKw_prime_H4 = new BigInteger(1, hashFunction(Kw_prime, i));
                BigInteger bsa = ea.xor(hashKw_prime_H3);
                BigInteger bsb = eb.xor(hashKw_prime_H3);

                // 更新bsw,not按位取反，negate取负数
                bsp = bsp.and(bsa.not()).xor(bsa.and(bsb));
            }

            // Step 10: 更新客户端状态
            byte[] Rc_plus_1 = generateRandomRc();
            SC.put(p, new ClientStateBytes(c + 1, c, Rc_plus_1));
            // Step 11: 重新加密 bsw
            byte[] H5 = hashFunction(Kp_prime, c + 1);
            ep = bsp.xor(new BigInteger(1, H5));

            // 客户端接收部分结束计时
            long client_time4 = System.nanoTime();
            // 记录结束时间并计算本次迭代的耗时
            long loopEndTime = System.nanoTime();
            long loopDurationNs = loopEndTime - loopStartTime;
            totalLoopTime += loopDurationNs; // 累加每次迭代的时间

//            double loopDurationMs = loopDurationNs / 1e6;
            //System.out.println(" search took " + loopDurationNs + " ns (" + loopDurationMs + " ms).");


            // 服务器更新 SS
            SS.put(new String(Kp, StandardCharsets.UTF_8), ep);
            // 输出客户端和服务器端的时间消耗
            double msclient_time1 = (client_time2 - client_time1) / 1e6;
            double msclient_time2 = (client_time4 - client_time3) / 1e6;
            double msserver_time = (server_time2 - server_time1) / 1e6;
            // 累加客户端和服务器的时间
            totalClientTime += msclient_time1 + msclient_time2;
            totalServerTime += msserver_time;
            double total_time = msclient_time1 + msclient_time2 + msserver_time;
//            System.out.println("prefix encode: Client time part 1: " + msclient_time1 + " ms, Client time part 2: " + msclient_time2 + " ms, Server time: " + msserver_time + " ms, Total time: " + total_time + " ms");

//            System.out.println(" search took " + loopDurationNs + " ns (" + loopDurationMs + " ms).");
            Sump = Sump.or(bsp);
        }

        //存储w位图结果
        BigInteger Sumw = BigInteger.ZERO;
        // 客户端处理关键字集合
        for (String w : WQ) {
            // 记录单次循环的开始时间
            long loopStartTime = System.nanoTime();
            // 客户端部分计时
            long client_time1 = System.nanoTime();

            // Step 1: 生成Kw和Kw_prime (客户端)
            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            byte[] Kw = new byte[LAMBDA / 8];
            byte[] Kw_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);

            // Step 2: 获取客户端的当前关键词状态
            ClientStateBytes state = SC.get(w);
            // 客户端部分结束计时
            long client_time2 = System.nanoTime();
            if (state == null) {
                exist = false;
//                System.out.println("没有匹配:"+w+"的结果");
                double msclient_time1 = (client_time2 - client_time1) / 1e6;
                totalClientTime += msclient_time1;
                totalServerTime += 0; // 没有服务器操作
                break;
            }
            // Step 3: 检查状态是否为null

            // 记录 Rc, c0, c (客户端)
            byte[] Ri = state.getRc();
            int c0 = state.getC0();
            int c = state.getC();


            // 开始服务器部分计时
            long server_time1 = System.nanoTime();

            // Step 1: 检查 SS[Kw] 的状态 (服务器)
            // 如果不存在，则初始化为全0的BigInteger
            BigInteger ew = SS.getOrDefault(new String(Kw, StandardCharsets.UTF_8), BigInteger.ZERO); // 从SS中读取
            // Step 2: 初始化一个空的map来存储结果E (服务器)
            Map<Integer, BigInteger[]> E = new HashMap<>();

            // Step 3: 从c到c0进行循环 (服务器)
            for (int i = c; i >= c0; i--) {
                // Step 4: 计算I
                byte[] I = hashFunction(Kw, Ri);

                // Step 5: 从PDB中检索密文
                CipherTextBytes ciphertext = KDB.get(new String(I, StandardCharsets.UTF_8));

                // Step 6: 将检索到的密文存储在E中 (只存储ea和eb)
                BigInteger ea = ciphertext.getEa();
                BigInteger eb = ciphertext.getEb();
                E.put(i - c0, new BigInteger[]{ea, eb});

                // Step 7: 从PDB中移除该密文
                KDB.remove(new String(I, StandardCharsets.UTF_8));

                // Step 8: 更新Ri-1 = C ⊕ H2(Kw, Ri)
                byte[] C = ciphertext.getC();
                Ri = xorBytes(C, hashFunction(Kw, Ri));
            }

            // 服务器部分结束计时
            long server_time2 = System.nanoTime();

            // 开始客户端接收并处理部分计时
            long client_time3 = System.nanoTime();

            // Step 1: 初始化匹配文件的位图 (客户端)
            BigInteger bsw = BigInteger.ZERO;

            // Step 2-3: 解密ew (客户端)
            if (!ew.equals(BigInteger.ZERO)) {
                bsw = ew;
                byte[] H5 = hashFunction(Kw_prime, c0);
                BigInteger H5BigInt = new BigInteger(1, H5);
                bsw = bsw.xor(H5BigInt);  // 执行异或操作
            }

            // Step 5: 循环解密每个密文并更新bsw (客户端)
            for (int i = c0; i <= c; i++) {
                BigInteger[] encryptPDBiBitmap = E.get(i - c0);
                BigInteger ea = encryptPDBiBitmap[0];
                BigInteger eb = encryptPDBiBitmap[1];

                BigInteger hashKw_prime_H3 = new BigInteger(1, hashFunction(Kw_prime, i));
                BigInteger hashKw_prime_H4 = new BigInteger(1, hashFunction(Kw_prime, i));
                BigInteger bsa = ea.xor(hashKw_prime_H3);
                BigInteger bsb = eb.xor(hashKw_prime_H4);

                // 更新bsw,not按位取反，negate取负数
                bsw = bsw.and(bsa.not()).xor(bsa.and(bsb));
            }

            // Step 10: 更新客户端状态
            byte[] Rc_plus_1 = generateRandomRc();
            SC.put(w, new ClientStateBytes(c + 1, c, Rc_plus_1));
            // Step 11: 重新加密 bsw
            byte[] H5 = hashFunction(Kw_prime, c + 1);
            ew = bsw.xor(new BigInteger(1, H5));

            // 客户端接收部分结束计时
            long client_time4 = System.nanoTime();
            // 记录结束时间并计算本次迭代的耗时
            long loopEndTime = System.nanoTime();
            long loopDurationNs = loopEndTime - loopStartTime;
            totalLoopTime += loopDurationNs; // 累加每次迭代的时间

//            double loopDurationMs = loopDurationNs / 1e6;
            //System.out.println(" search took " + loopDurationNs + " ns (" + loopDurationMs + " ms).");

            // 服务器更新 SS
            SS.put(new String(Kw, StandardCharsets.UTF_8), ew);
            // 输出客户端和服务器端的时间消耗
            double msclient_time1 = (client_time2 - client_time1) / 1e6;
            double msclient_time2 = (client_time4 - client_time3) / 1e6;
            double msserver_time = (server_time2 - server_time1) / 1e6;
            // 累加客户端和服务器的时间
            totalClientTime += msclient_time1 + msclient_time2;
            totalServerTime += msserver_time;
            double total_time = msclient_time1 + msclient_time2 + msserver_time;
//            System.out.println("keyword: Client time part 1: " + msclient_time1 + " ms, Client time part 2: " + msclient_time2 + " ms, Server time: " + msserver_time + " ms, Total time: " + total_time + " ms");

//            System.out.println(" search took " + loopDurationNs + " ns (" + loopDurationMs + " ms).");
            Sumw = Sumw.or(bsw);
        }
        // 将累计的客户端和服务器时间分别存储到列表中
        clientSearchTimes.add(totalClientTime);
        serverSearchTimes.add(totalServerTime);
        if (!exist) return BigInteger.ZERO;
        // 输出总耗时
//        double totalLoopTimeMs = totalLoopTime / 1e6;
//        findIndexesOfOne(Sump);
//        findIndexesOfOne(Sumw);
//        System.out.println("Total loop time: " + totalLoopTime + " ns (" + totalLoopTimeMs + " ms).");
//        System.out.println("RSKQ_Biginteger Total search time: " + (totalLoopTime+precoverTime) + " ns (" + (totalLoopTimeMs+(precoverTime/1e6)) + " ms).");
        return Sump.and(Sumw);
    }

    public double ObjectUpdate(long[] pSet, String[] W, String[] op, int[] files) throws Exception {
        byte[] combinedKey;
        byte[] Kw = new byte[LAMBDA / 8];
        byte[] Kw_prime = new byte[LAMBDA / 8];
        long startTime = System.nanoTime();
        List<String> P = preCode(pSet);
        for (String p : P) {
            //Client
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);
            // Step 2: 获取客户端的当前关键词状态
            ClientStateBytes state = SC.getOrDefault(p, new ClientStateBytes(0, -1, generateRandomRc()));
            // Step 3: 随机生成 Rc+1
            byte[] Rc_plus_1 = generateRandomRc();
            BitSet bitmap_a = new BitSet();
            BitSet bitmap_b = new BitSet();
            // 根据操作设置 bsa 和 bsb
            for (int i = 0; i < files.length; i++) {
                bitmap_a.set(files[i]);
                if ("add".equals(op[i])) {
                    bitmap_b.set(files[i]);
                }  //bsb = bsb.clearBit(fileIndex);  // 删除操作，清除bsb中相应位（设置为0）
            }
            byte[] hashKw_prime = hashFunction(Kw_prime, state.getC() + 1);
            // Step 7: 更新客户端状态
            SC.put(p, new ClientStateBytes(state.getC0(), state.getC() + 1, Rc_plus_1));
            //Server
            // Step 8: 将 (I, C, (ea, eb)) 发送到服务器（存入PDB）
            String key = new String(hashFunction(Kw, Rc_plus_1), StandardCharsets.UTF_8);

            PDB.put(key,
                    new CipherTextBytes(
                            xorBytes(hashFunction(Kw, Rc_plus_1), state.getRc()),
                            new BigInteger(1, hashKw_prime).xor(new BigInteger(1, bitmap_a.toByteArray())),
                            new BigInteger(1, hashKw_prime).xor(bitmap_b.isEmpty() ? BigInteger.ZERO : new BigInteger(1, bitmap_b.toByteArray()))
                    )
            );
//            PDB.put(new String(hashFunction(Kw, Rc_plus_1), StandardCharsets.UTF_8),
//                    new CipherTextBytes(xorBytes(hashFunction(Kw, Rc_plus_1), intToBytes(state[2])),
//                    new BigInteger(1, xorBytes(hashKw_prime, bitmap_a.toByteArray())),
//                    new BigInteger(1, xorBytes(hashKw_prime, bitmap_b.toByteArray()))));
        }
        long pTime = System.nanoTime();
        for (String w : W) {
            //Client
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);
            // Step 2: 获取客户端的当前关键词状态
            ClientStateBytes state = SC.getOrDefault(w, new ClientStateBytes(0, -1, generateRandomRc()));
            // Step 3: 随机生成 Rc+1
            byte[] Rc_plus_1 = generateRandomRc();

            // Step 4: 计算 I,C = hashFunction(Kw, Rc_plus_1) ⊕ state[2]
//            byte[] I = hashFunction(Kw, Rc_plus_1); // 根据Kw和Rc+1计算索引I
//            byte[] C = xorBytes(hashFunction(Kw, Rc_plus_1), intToBytes(state[2]));
//            // Step 5: 根据操作选择 bi-bitmap (bsa, bsb)
//            BigInteger bsa = BigInteger.ZERO;  // 使用 BigInteger 作为位图
//            BigInteger bsb = BigInteger.ZERO;
//
//            // 根据操作设置 bsa 和 bsb
//            for (int fileIndex : files) {
//                if ("add".equals(op)) {
//                    bsa = bsa.setBit(fileIndex);  // 添加操作，设置bsa中相应位为1
//                    bsb = bsb.setBit(fileIndex);  // 添加操作，设置bsb中相应位为1
//                } else if ("del".equals(op)) {
//                    bsa = bsa.setBit(fileIndex);  // 删除操作，设置bsa中相应位为1
//                    //bsb = bsb.clearBit(fileIndex);  // 删除操作，清除bsb中相应位（设置为0）
//                }
//            }
//            // Step 6: 加密 bsa 和 bsb，不使用加密函数，而是和 hashFunction(Kw_prime, state[1] + 1) 异或
//            BigInteger hashKw_prime = new BigInteger(1, hashFunction(Kw_prime, state[1] + 1));
//            BigInteger ea = bsa.xor(hashKw_prime);
//            BigInteger eb = bsb.xor(hashKw_prime);
            BitSet bitmap_a = new BitSet();
            BitSet bitmap_b = new BitSet();
            // 根据操作设置 bsa 和 bsb
            for (int i = 0; i < files.length; i++) {
                bitmap_a.set(files[i]);
                if ("add".equals(op[i])) {
                    bitmap_b.set(files[i]);
                }  //bsb = bsb.clearBit(fileIndex);  // 删除操作，清除bsb中相应位（设置为0）

            }
            byte[] hashKw_prime = hashFunction(Kw_prime, state.getC() + 1);
            // Step 7: 更新客户端状态
            SC.put(w, new ClientStateBytes(state.getC0(), state.getC() + 1, Rc_plus_1));
            //Server
            // Step 8: 将 (I, C, (ea, eb)) 发送到服务器（存入PDB）
            // 计算键值
            String key = new String(hashFunction(Kw, Rc_plus_1), StandardCharsets.UTF_8);

            KDB.put(key,
                    new CipherTextBytes(
                            xorBytes(hashFunction(Kw, Rc_plus_1), state.getRc()),
                            new BigInteger(1, hashKw_prime).xor(new BigInteger(1, bitmap_a.toByteArray())),
                            new BigInteger(1, hashKw_prime).xor(bitmap_b.isEmpty() ? BigInteger.ZERO : new BigInteger(1, bitmap_b.toByteArray()))
                    )
            );
            // 检查键是否已存在
//            if (KDB.containsKey(key)) {
//                // 键已存在，打印日志说明
////                System.out.println("Key already exists in KDB: " + key);
////                System.out.print(findRIndexes(Rc_plus_1));
//            } else {
//                // 键不存在，执行 put 操作
//                KDB.put(key,
//                        new CipherTextBytes(
//                                xorBytes(hashFunction(Kw, Rc_plus_1), state.getRc()),
//                                new BigInteger(1, hashKw_prime).xor(new BigInteger(1, bitmap_a.toByteArray())),
//                                new BigInteger(1, hashKw_prime).xor(bitmap_b.isEmpty()? BigInteger.ZERO : new BigInteger(1,bitmap_b.toByteArray()))
//                        )
//                );
//            }
        }
        long wTime = System.nanoTime();
        // 输出总耗时
        double totalLoopTimeMs = (System.nanoTime() - startTime) / 1e6;
//        System.out.println("RSKQ_Biginteger ptime: " + (pTime-startTime) / 1e6 + " ms.");
//        System.out.println("RSKQ_Biginteger wtime: " + (wTime-pTime) / 1e6 + " ms.");
//        System.out.println("RSKQ_Biginteger Total update time: " + totalLoopTimeMs + " ms.");
        // 存储到列表中
        totalUpdateTimes.add(totalLoopTimeMs);
//        System.out.println("Update operation completed.");
        return totalLoopTimeMs;
    }

    public BigInteger ObjectSearch(BigInteger[][] Matrix, String[] WQ) throws Exception {
        long totalLoopTime = 0; // 初始化总时间变量
        // 客户端：生成搜索请求
        long startTime = System.nanoTime();
        List<String> BPC = preCover(Matrix);
//        System.out.println("PreCover time: " + precoverTime + " ns (" + (precoverTime/1e6) + " ms).");
        // 累积的客户端和服务器时间
        long totalClientTime = System.nanoTime() - startTime;
        long totalServerTime = 0;
        //存储p位图结果
        BigInteger Sump = BigInteger.ZERO;
        boolean exist = true;
        // 客户端处理前缀集合
        for (String p : BPC) {
            // 客户端部分计时
            long client_time_loop_start = System.nanoTime();

            // Step 1: 生成Kw和Kw_prime (客户端)
            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            byte[] Kp = new byte[LAMBDA / 8];
            byte[] Kp_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, 0, Kp, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kp_prime, 0, LAMBDA / 8);

            // Step 2: 获取客户端的当前关键词状态
            ClientStateBytes state = SC.get(p);
            // 客户端部分结束计时
            long client_time_loop_end = System.nanoTime();
            // 若state为null，则跳出循环
            if (state == null) {
//                exist = false;
//                System.out.println("没有匹配:"+p+"的结果");
                totalClientTime += (client_time_loop_end - client_time_loop_start);
                // 没有进行服务器操作，因此设为0
                continue;
            }
            // Step 3: 检查状态是否为null

            // 记录 Rc, c0, c (客户端)
            byte[] Ri = state.getRc();
            int c0 = state.getC0();
            int c = state.getC();

            totalClientTime += (client_time_loop_end - client_time_loop_start);

            // 开始服务器部分计时
            long server_time_loop_start = System.nanoTime();

            // Step 1: 检查 SS[Kw] 的状态 (服务器)
            // 如果不存在，则初始化为全0的BigInteger
            BigInteger ep = SS.getOrDefault(new String(Kp, StandardCharsets.UTF_8), BigInteger.ZERO); // 从SS中读取
            // Step 2: 初始化一个空的map来存储结果E (服务器)
            Map<Integer, BigInteger[]> E = new HashMap<>();

            // Step 3: 从c到c0进行循环 (服务器)
//            int Ri = Rc;
            for (int i = c; i >= c0; i--) {
                // Step 4: 计算I
//                byte[] I = hashFunction(Kp, Ri);
                String keyI = new String(hashFunction(Kp, Ri), StandardCharsets.UTF_8);
                if (!PDB.containsKey(keyI)) {
//                    System.out.println("Key does not exist in PDB: " + keyI);
//                    System.out.printf("p: %s |", p);
                    continue;
//                System.out.print(findRIndexes(Rc_plus_1));
                }
                // Step 5: 从PDB中检索密文
                CipherTextBytes ciphertext = PDB.get(keyI);

                // Step 6: 将检索到的密文存储在E中 (只存储ea和eb)
                E.put(i - c0, new BigInteger[]{ciphertext.getEa(), ciphertext.getEb()});

                // Step 7: 从PDB中移除该密文
                PDB.remove(keyI);

                // Step 8: 更新Ri-1 = C ⊕ H2(Kw, Ri)
                byte[] C = ciphertext.getC();
//                Ri = new BigInteger(C).xor(new BigInteger(hashFunction(Kp, Ri))).intValue();
                Ri = xorBytes(C, hashFunction(Kp, Ri));

            }

            // 服务器部分结束计时
            long server_time_loop_end = System.nanoTime();
            totalServerTime += (server_time_loop_end - server_time_loop_start);
            // 开始客户端接收并处理部分计时
//            long client_time_dec_start = System.nanoTime();

            // Step 1: 初始化匹配文件的位图 (客户端)
            BigInteger bsp = BigInteger.ZERO;
            // Step 2-3: 解密ep (客户端)
            if (!ep.equals(BigInteger.ZERO)) {
                byte[] H5 = hashFunction(Kp_prime, c0);
                bsp = ep.xor(new BigInteger(1, H5));  // 执行异或操作
            }

            // Step 5: 循环解密每个密文并更新bsw (客户端)
            for (int i = c0; i <= c; i++) {
                BigInteger hashKw_prime_H3 = new BigInteger(1, hashFunction(Kp_prime, i));
//                BigInteger hashKw_prime_H4 = new BigInteger(1, hashFunction(Kw_prime, i));

                if (E.containsKey(i - c0)) {
                    BigInteger bsa = E.get(i - c0)[0].xor(hashKw_prime_H3);
                    BigInteger bsb = E.get(i - c0)[1].xor(hashKw_prime_H3);

                    // 更新bsw,not按位取反，negate取负数
                    bsp = bsp.and(bsa.not()).xor(bsa.and(bsb));
                }
            }

            // Step 10: 更新客户端状态
            byte[] Rc_plus_1 = generateRandomRc();
            SC.put(p, new ClientStateBytes(c + 1, c, Rc_plus_1));
            // Step 11: 重新加密 bsw
            byte[] H5 = hashFunction(Kp_prime, c + 1);
            ep = bsp.xor(new BigInteger(1, H5));

            // 客户端接收部分结束计时
            long client_time_dec_end = System.nanoTime();
            // 服务器更新 SS
            SS.put(new String(Kp, StandardCharsets.UTF_8), ep);
            // 累加客户端和服务器的时间
            totalClientTime += (client_time_dec_end - server_time_loop_end);
            Sump = Sump.or(bsp);
        }
        //存储w位图结果
        BigInteger Sumw = BigInteger.ZERO;
        // 客户端处理关键字集合
        for (String w : WQ) {
            // 客户端部分计时
            long client_time1 = System.nanoTime();

            // Step 1: 生成Kw和Kw_prime (客户端)
            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            byte[] Kw = new byte[LAMBDA / 8];
            byte[] Kw_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);

            // Step 2: 获取客户端的当前关键词状态
            ClientStateBytes state = SC.get(w);
            // 客户端部分结束计时
            long client_time2 = System.nanoTime();
            if (state == null) {
                exist = false;
//                System.out.println("没有匹配:"+w+"的结果");
                totalClientTime += (client_time2 - client_time1);
                // 服务器耗时不增
                break;
            }
            // Step 3: 检查状态是否为null

            // 记录 Rc, c0, c (客户端)
            byte[] Ri = state.getRc();
            int c0 = state.getC0();
            int c = state.getC();
            totalClientTime += (client_time2 - client_time1);

            // 开始服务器部分计时
            long server_time_loop_start = System.nanoTime();

            // Step 1: 检查 SS[Kw] 的状态 (服务器)
            // 如果不存在，则初始化为全0的BigInteger
            BigInteger ew = SS.getOrDefault(new String(Kw, StandardCharsets.UTF_8), BigInteger.ZERO); // 从SS中读取
            // Step 2: 初始化一个空的map来存储结果E (服务器)
            Map<Integer, BigInteger[]> E = new HashMap<>();

            // Step 3: 从c到c0进行循环 (服务器)
            for (int i = c; i >= c0; i--) {
                // Step 4: 计算I
                byte[] I = hashFunction(Kw, Ri);

                // Step 5: 从PDB中检索密文
                CipherTextBytes ciphertext = KDB.get(new String(I, StandardCharsets.UTF_8));

                // Step 6: 将检索到的密文存储在E中 (只存储ea和eb)
                BigInteger ea = ciphertext.getEa();
                BigInteger eb = ciphertext.getEb();
                E.put(i - c0, new BigInteger[]{ea, eb});

                // Step 7: 从PDB中移除该密文
                KDB.remove(new String(I, StandardCharsets.UTF_8));

                // Step 8: 更新Ri-1 = C ⊕ H2(Kw, Ri)
                byte[] C = ciphertext.getC();
//                Ri = new BigInteger(C).xor(new BigInteger(hashFunction(Kw, Ri))).intValue();
                Ri = xorBytes(C, hashFunction(Kw, Ri));
            }

            // 服务器部分结束计时
            long server_time_loop_end = System.nanoTime();
            totalServerTime += (server_time_loop_end - server_time_loop_start);
            // 开始客户端接收并处理部分计时
//            long client_time3 = System.nanoTime();

            // Step 1: 初始化匹配文件的位图 (客户端)
            BigInteger bsw = BigInteger.ZERO;

            // Step 2-3: 解密ew (客户端)
            if (!ew.equals(BigInteger.ZERO)) {
                bsw = ew;
                byte[] H5 = hashFunction(Kw_prime, c0);
                BigInteger H5BigInt = new BigInteger(1, H5);
                bsw = bsw.xor(H5BigInt);  // 执行异或操作
            }

            // Step 5: 循环解密每个密文并更新bsw (客户端)
            for (int i = c0; i <= c; i++) {
                BigInteger[] encryptPDBiBitmap = E.get(i - c0);
                BigInteger ea = encryptPDBiBitmap[0];
                BigInteger eb = encryptPDBiBitmap[1];

                BigInteger hashKw_prime_H3 = new BigInteger(1, hashFunction(Kw_prime, i));
                BigInteger hashKw_prime_H4 = new BigInteger(1, hashFunction(Kw_prime, i));
                BigInteger bsa = ea.xor(hashKw_prime_H3);
                BigInteger bsb = eb.xor(hashKw_prime_H4);

                // 更新bsw,not按位取反，negate取负数
                bsw = bsw.and(bsa.not()).xor(bsa.and(bsb));
            }

            // Step 10: 更新客户端状态
            byte[] Rc_plus_1 = generateRandomRc();
            SC.put(w, new ClientStateBytes(c + 1, c, Rc_plus_1));
            // Step 11: 重新加密 bsw
            byte[] H5 = hashFunction(Kw_prime, c + 1);
            ew = bsw.xor(new BigInteger(1, H5));

            // 客户端接收部分结束计时
            long client_time_dec_end = System.nanoTime();
            // 记录结束时间并计算本次迭代的耗时
//            long loopEndTime = System.nanoTime();
//            long loopDurationNs = loopEndTime - loopStartTime;
//            totalLoopTime += loopDurationNs; // 累加每次迭代的时间

//            double loopDurationMs = loopDurationNs / 1e6;
            //System.out.println(" search took " + loopDurationNs + " ns (" + loopDurationMs + " ms).");

            // 服务器更新 SS
            SS.put(new String(Kw, StandardCharsets.UTF_8), ew);
            // 累加客户端和服务器的时间
            totalClientTime += (client_time_dec_end - server_time_loop_end);
            Sumw = Sumw.or(bsw);
        }
        // 将累计的客户端和服务器时间分别存储到列表中
        clientSearchTimes.add(totalClientTime / 1e6);
        serverSearchTimes.add(totalServerTime / 1e6);
        if (!exist) return BigInteger.ZERO;
        // 输出总耗时
//        double totalLoopTimeMs = totalLoopTime / 1e6;
//        findIndexesOfOne(Sump);
//        findIndexesOfOne(Sumw);
//        System.out.println("Total loop time: " + totalLoopTime + " ns (" + totalLoopTimeMs + " ms).");
//        System.out.println("RSKQ_Biginteger Total search time: " + (totalLoopTime+precoverTime) + " ns (" + (totalLoopTimeMs+(precoverTime/1e6)) + " ms).");
        return Sump.and(Sumw);
    }

    public BigInteger GRQSearch(BigInteger[][] Matrix) throws Exception {
        long totalLoopTime = 0; // 初始化总时间变量
        // 客户端：生成搜索请求
        long startTime = System.nanoTime();
        List<String> BPC = preCover(Matrix);
//        System.out.println("PreCover time: " + precoverTime + " ns (" + (precoverTime/1e6) + " ms).");
        // 累积的客户端和服务器时间
        long totalClientTime = System.nanoTime() - startTime;
        long totalServerTime = 0;
        //存储p位图结果
        BigInteger Sump = BigInteger.ZERO;
        boolean exist = true;
        // 客户端处理前缀集合
        for (String p : BPC) {
            // 客户端部分计时
            long client_time_loop_start = System.nanoTime();

            // Step 1: 生成Kw和Kw_prime (客户端)
            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            byte[] Kp = new byte[LAMBDA / 8];
            byte[] Kp_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, 0, Kp, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kp_prime, 0, LAMBDA / 8);

            // Step 2: 获取客户端的当前关键词状态
            ClientStateBytes state = SC.get(p);
            // 客户端部分结束计时
            long client_time_loop_end = System.nanoTime();
            // 若state为null，则跳出循环
            if (state == null) {
//                exist = false;
//                System.out.println("没有匹配:"+p+"的结果");
                totalClientTime += (client_time_loop_end - client_time_loop_start);
                // 没有进行服务器操作，因此设为0
                continue;
            }
            // Step 3: 检查状态是否为null

            // 记录 Rc, c0, c (客户端)
            byte[] Ri = state.getRc();
            int c0 = state.getC0();
            int c = state.getC();

            totalClientTime += (client_time_loop_end - client_time_loop_start);

            // 开始服务器部分计时
            long server_time_loop_start = System.nanoTime();

            // Step 1: 检查 SS[Kw] 的状态 (服务器)
            // 如果不存在，则初始化为全0的BigInteger
            BigInteger ep = SS.getOrDefault(new String(Kp, StandardCharsets.UTF_8), BigInteger.ZERO); // 从SS中读取
            // Step 2: 初始化一个空的map来存储结果E (服务器)
            Map<Integer, BigInteger[]> E = new HashMap<>();

            // Step 3: 从c到c0进行循环 (服务器)
            for (int i = c; i >= c0; i--) {
                // Step 4: 计算I
                String keyI = new String(hashFunction(Kp, Ri), StandardCharsets.UTF_8);
                if (!PDB.containsKey(keyI)) {
//                    System.out.println("Key does not exist in PDB: " + keyI);
//                    System.out.printf("p: %s |", p);
                    continue;
//                System.out.print(findRIndexes(Rc_plus_1));
                }
                // Step 5: 从PDB中检索密文
                CipherTextBytes ciphertext = PDB.get(keyI);

                // Step 6: 将检索到的密文存储在E中 (只存储ea和eb)
                E.put(i - c0, new BigInteger[]{ciphertext.getEa(), ciphertext.getEb()});

                // Step 7: 从PDB中移除该密文
                PDB.remove(keyI);

                // Step 8: 更新Ri-1 = C ⊕ H2(Kw, Ri)
                byte[] C = ciphertext.getC();
                Ri = xorBytes(C, hashFunction(Kp, Ri));
            }

            // 服务器部分结束计时
            long server_time_loop_end = System.nanoTime();
            totalServerTime += (server_time_loop_end - server_time_loop_start);
            // 开始客户端接收并处理部分计时
//            long client_time_dec_start = System.nanoTime();

            // Step 1: 初始化匹配文件的位图 (客户端)
            BigInteger bsp = BigInteger.ZERO;

            // Step 2-3: 解密ep (客户端)
            if (!ep.equals(BigInteger.ZERO)) {
                bsp = ep.xor(new BigInteger(1, hashFunction(Kp_prime, c0)));  // 执行异或操作
            }

            // Step 5: 循环解密每个密文并更新bsw (客户端)
            for (int i = c0; i <= c; i++) {
                BigInteger hashKw_prime_H3 = new BigInteger(1, hashFunction(Kp_prime, i));
//                BigInteger hashKw_prime_H4 = new BigInteger(1, hashFunction(Kw_prime, i));
                if (E.containsKey(i - c0)) {
                    BigInteger bsa = E.get(i - c0)[0].xor(hashKw_prime_H3);
                    BigInteger bsb = E.get(i - c0)[1].xor(hashKw_prime_H3);
                    // 更新bsw,not按位取反，negate取负数
                    bsp = bsp.and(bsa.not()).xor(bsa.and(bsb));
                }
            }
            // Step 10: 更新客户端状态
            SC.put(p, new ClientStateBytes(c + 1, c, generateRandomRc()));
            // Step 11: 重新加密 bsw
            ep = bsp.xor(new BigInteger(1, hashFunction(Kp_prime, c + 1)));
            // 客户端接收部分结束计时
            long client_time_dec_end = System.nanoTime();
            // 服务器更新 SS
            SS.put(new String(Kp, StandardCharsets.UTF_8), ep);
            // 累加客户端和服务器的时间
            totalClientTime += (client_time_dec_end - server_time_loop_end);
            Sump = Sump.or(bsp);
        }
        // 将累计的客户端和服务器时间分别存储到列表中
        clientSearchTimes.add(totalClientTime / 1e6);
        serverSearchTimes.add(totalServerTime / 1e6);
        if (!exist) return BigInteger.ZERO;
        return Sump;
    }

    public static void main(String[] args) throws Exception {
        // 清除"最大耗时-最小耗时"对数,便于计算合理的平均值
        int delupdatetimes = 1;
        // 需要在内存中存储，所以需要插入 updatetime 个 Object
        int updatetimes = 1000;
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

        RSKQ_Biginteger spqsBitset = new RSKQ_Biginteger(maxfilesArray[0], hilbertOrders[0], 2);

        // 随机数据生成器
        Random random = new Random();

        // 执行更新操作
        for (int i = 0; i < updatetimes; i++) {
            int randomIndex = random.nextInt(objectnums);
            long[] pSet = dataSetAccess.pointDataSet[randomIndex];
            String[] W = dataSetAccess.keywordItemSets[randomIndex];
            int[] files = new int[]{dataSetAccess.fileDataSet[randomIndex]};
            spqsBitset.ObjectUpdate(pSet, W, new String[]{"add"}, files);

            if ((i + 1) % batchSize == 0) {
                System.out.println("Completed batch " + (i + 1) / batchSize + " of updates.");
                // 打印平均搜索时间
                System.out.printf("Update完成，平均更新时间: | RSKQ_Biginteger: |%-10.6f|ms\n",
                        spqsBitset.getAverageUpdateTime());
                System.out.printf("更新数量: | PDB: |%d| KDB: |%d|\n",
                        spqsBitset.getPDBSize(), spqsBitset.getKDBSize());
            }
        }

        // 移除初始的异常值
        for (int i = 0; i < delupdatetimes; i++) {
            spqsBitset.removeExtremesUpdateTime();
        }
        // 打印平均搜索时间
        System.out.printf("Update完成，平均更新时间: | RSKQ_Biginteger: |%-10.6f|ms\n",
                spqsBitset.getAverageUpdateTime());
        spqsBitset.clearUpdateTime();
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
                            spqsBitset.hilbertCurve, xstart, ystart, searchRange, searchRange);

                    for (int i = 0; i < searchtimes; i++) {
                        int indexToSearch = random.nextInt(objectnums);
                        String[] WQ = dataSetAccess.keywordItemSets[indexToSearch];

                        spqsBitset.ObjectSearch(matrixToSearch, WQ);
                    }

                    // 移除异常值
                    for (int i = 0; i < delupdatetimes; i++) {
                        spqsBitset.removeExtremesSearchTime();
                    }

                    // 打印平均搜索时间
                    System.out.printf("搜索完成，平均搜索时间: | RSKQ_Biginteger: |%-10.6f|ms\n",
                            spqsBitset.getAverageSearchTime());
                    break;

                case 2: // 打印 PDB 和 KDB 键的数量
                    int pdbKeyCount = spqsBitset.PDB.size();
                    int kdbKeyCount = spqsBitset.KDB.size();
                    System.out.printf("PDB 键的数量: %d, KDB 键的数量: %d\n", pdbKeyCount, kdbKeyCount);
                    break;

                default:
                    System.out.println("无效选项，请重新选择。");
            }
        }
        scanner.close();
//        // 用户交互部分
//        Scanner scanner = new Scanner(System.in);
//        while (true) {
//            // 询问搜索次数
//            System.out.print("请输入搜索次数 (输入 -1 退出): ");
//            int searchtimes = scanner.nextInt();
//
//            if (searchtimes == -1) {
//                System.out.println("程序已退出。");
//                break;
//            }
//
//            if (searchtimes <= 0) {
//                System.out.println("搜索次数必须是正整数。请重新输入。");
//                continue;
//            }
//
//            // 询问搜索范围
//            System.out.print("请输入搜索范围 (0-100%): ");
//            int searchEdgeLengthPer = scanner.nextInt();
//
//            if (searchEdgeLengthPer == -1) {
//                System.out.println("程序已退出。");
//                break;
//            }
//
//            if (searchEdgeLengthPer <= 0 || searchEdgeLengthPer > 100) {
//                System.out.println("搜索范围必须在 0 到 100 之间。请重新输入。");
//                continue;
//            }
//
//            // 计算搜索矩阵范围
//            int xstart = random.nextInt(edgeLength * (div - searchEdgeLengthPer) / div);
//            int ystart = random.nextInt(edgeLength * (div - searchEdgeLengthPer) / div);
//            int searchRange = edgeLength * searchEdgeLengthPer / div;
//
//            BigInteger[][] matrixToSearch = generateHilbertMatrix(spqsBitset.hilbertCurve, xstart, ystart, searchRange, searchRange);
//
//            // 执行搜索
//            for (int i = 0; i < searchtimes; i++) {
//                int indexToSearch = random.nextInt(objectnums);
//                String[] WQ = dataSetAccess.keywordItemSets[indexToSearch];
//
//                spqsBitset.ObjectSearch(matrixToSearch, WQ);
//            }
//
//            // 移除异常值
//            for (int i = 0; i < delupdatetimes; i++) {
//                spqsBitset.removeExtremesSearchTime();
//            }
//
//            // 记录搜索时间
//            rskqSearchTimes.add(spqsBitset.getAverageSearchTime());
//
//            // 打印平均搜索时间
//            System.out.printf("搜索完成，平均搜索时间: | RSKQ: |%-10.6f|ms\n",
//                    rskqSearchTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
//        }
//        scanner.close();
    }

    /**
     * 异或操作，将两个字节数组进行逐位异或
     *
     * @param a 字节数组a
     * @param b 字节数组b
     * @return 异或后的结果
     */
    private byte[] xorBytes(byte[] a, byte[] b) {
        if (b.length < a.length) {
            throw new IllegalArgumentException("Input array 'b' must be at least as long as 'a'");
        }

        // 遍历数组并计算异或，直接修改 b
        for (int i = 0; i < a.length; i++) {
            b[i] = (byte) (a[i] ^ b[i]);
        }

        // 保留 b 的多余部分，a 的内容被完全异或到 b 上
        return b;
    }

    // 获取更新操作的平均时间
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

    public void clearUpdateTime() {
        totalUpdateTimes.clear();
    }

    public void clearSearchTime() {
        serverSearchTimes.clear();
        clientSearchTimes.clear();
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

    //    public static void main(String[] args) throws Exception {
//        // 定义参数
//        int maxFiles = 1 << 20; // 最大文件数，2^20
//        int order = 17; // Hilbert curve 阶数
//        int dimension = 2; // 维度
//
//        // 初始化 RSKQ_Biginteger 实例
//        RSKQ_Biginteger spqs = new RSKQ_Biginteger(maxFiles, order, dimension);
//
//        // 模拟一些数据
//        Random random = new Random();
//        int numObjects = 1; // 插入5个对象进行测试
//        int rangePredicate = 1000;
//
//        // 初始化测试对象的数据
//        long[][] pSets = new long[numObjects][2];
//        String[][] WSets = new String[numObjects][6]; // 假设每个对象有6个关键词
//        int[][] fileSets = new int[numObjects][1]; // 每个对象关联一个文件
//
//        // 填充对象数据
//        for (int i = 0; i < numObjects; i++) {
//            // 创建pSet (二维数据)
//            pSets[i][0] = random.nextInt(1 << (order - 1));
//            pSets[i][1] = pSets[i][0] + 1;
//
//            // 创建关键词W
//            for (int j = 0; j < 6; j++) {
//                WSets[i][j] = "keyword" + (random.nextInt(100) + 1);
//            }
//
//            // 关联一个文件
//            fileSets[i][0] = random.nextInt(maxFiles);
//        }
//
//        // 打印插入的数据
//        System.out.println("即将插入的数据:");
//        for (int i = 0; i < numObjects; i++) {
//            System.out.println("Object " + (i + 1) + ":");
//            System.out.println("  pSet: " + Arrays.toString(pSets[i]));
//            System.out.println("  W: " + Arrays.toString(WSets[i]));
//            System.out.println("  File ID: " + Arrays.toString(fileSets[i]));
//        }
//
//        if (spqs.SC != null) {
//            for (Map.Entry<String, int[]> entry : spqs.SC.entrySet()) {
//                System.out.println(entry.getKey() + ": " + Arrays.toString(entry.getValue()));
//            }
//        } else {
//            System.out.println("Map is empty or not initialized.");
//        }
//        // 执行 update 操作（插入数据）
//        System.out.println("插入操作开始...");
//        for (int i = 0; i < numObjects; i++) {
//            spqs.ObjectUpdate(pSets[i], WSets[i], new String[]{"add"}, fileSets[i]);
//        }
//        System.out.println("插入操作完成。");
//
//        // 测试 search 操作
//        System.out.println("开始搜索...");
//        for (int i = 0; i < numObjects; i++) {
//            // 通过 Hilbert 曲线计算范围
//            BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSets[i]);
//            System.out.println("Hilbert Index:" + pointHilbertIndex);
//            BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(100));
//            BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(100));
//
//            // 执行搜索操作
//            BigInteger result = spqs.ObjectSearch(R_min, R_max, WSets[i]);
//
//            // 打印搜索结果
//            System.out.println("\n搜索结果 (pSet " + Arrays.toString(pSets[i]) + "): ");
//            findIndexesOfOne(result); // 打印出结果中的位图索引
//        }
//        System.out.println("搜索操作完成。");
//
//        // 打印时间统计
//        System.out.println("平均更新时间: " + spqs.getAverageUpdateTime() + " ms");
//        System.out.println("平均客户端搜索时间: " + spqs.getAverageClientTime() + " ms");
//        System.out.println("平均服务器搜索时间: " + spqs.getAverageServerTime() + " ms");
//
//        spqs.printTimes();
//    }

}

