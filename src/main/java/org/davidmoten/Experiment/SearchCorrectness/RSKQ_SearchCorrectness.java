package org.davidmoten.Experiment.SearchCorrectness;


import org.davidmoten.BPC.BPCGenerator;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class RSKQ_SearchCorrectness {
    // 列表用于存储 update 和 search 的时间
    public List<Double> totalUpdateTimes = new ArrayList<>();    // 存储 update 操作的总耗时
    public List<Double> clientSearchTimes = new ArrayList<>();         // 存储客户端 search 操作的时间
    public List<Double> serverSearchTimes = new ArrayList<>();         // 存储服务器 search 操作的时间

    public static final int LAMBDA = 128;  // 安全参数 λ    // 缓存的MessageDigest实例
    private final MessageDigest messageDigest;
    private final byte[] intBuffer = new byte[4]; // 用于缓存int转byte的缓冲区
    private HashMap<String, int[]> SC;  // 客户端状态
    private HashMap<String, BigInteger> SS;  // 服务器状态
    private ConcurrentHashMap<String, Object[]> PDB; // 服务器存储的密文数据库
    private ConcurrentHashMap<String, Object[]> KDB; // 服务器存储的密文数据库

    // 伪随机函数的哈希算法
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HASH_ALGORITHM = "SHA-256";

    // 预生成的随机数池
    private final int[] randomPool;
    private int poolIndex;

    //    private int maxnums_w; // 关键字最大数量
//    private String filePath; // 数据集路径
    private int maxFiles; // 最大文件数
    private int dimension; // 2维数据
    private int order; // Hilbert curve 阶数
    public HilbertCurve hilbertCurve;

    // 修改后的构造函数
    public RSKQ_SearchCorrectness(int maxFiles, int order, int dimension) throws NoSuchAlgorithmException {
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
        this.randomPool = new int[10000]; // 设置随机数池大小为 10000
        this.poolIndex = 0;
        // 预生成随机数池
        fillRandomPool();
    }

    // 填充随机数池
    private void fillRandomPool() {
        for (int i = 0; i < randomPool.length; i++) {
            randomPool[i] = ThreadLocalRandom.current().nextInt();
        }
    }

    // 从随机数池中获取随机数
    private int getRandomFromPool() {
        if (poolIndex >= randomPool.length) {
            poolIndex = 0;
            fillRandomPool(); // 如果用完了，重新填充随机数池
        }
        return randomPool[poolIndex++];
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
//        System.out.println("PreCover time: " + precoverTime + " ns (" + (precoverTime/1_000_000.0) + " ms).");
        // 累积的客户端和服务器时间
        double totalClientTime = 0.0;
        double totalServerTime = 0.0;
        //存储p位图结果
        BigInteger Sump = BigInteger.ZERO;
        // 客户端处理前缀集合
        for (String p : BPC) {
            //Sump = Sump.or(search(p));
            // 记录单次循环的开始时间
            long loopStartTime = System.nanoTime();
            // 客户端部分计时
            long client_time1 = System.nanoTime();

            // Step 1: 生成Kw和Kw_prime (客户端)
            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            byte[] Kw = new byte[LAMBDA / 8];
            byte[] Kw_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);

            // Step 2: 获取客户端的当前关键词状态
            int[] state = SC.getOrDefault(p, new int[]{0, -1, getRandomFromPool()});

            // Step 3: 检查状态是否为null

            // 记录 Rc, c0, c (客户端)
            int Rc = state[2];
            int c0 = state[0];
            int c = state[1];

            // 客户端部分结束计时
            long client_time2 = System.nanoTime();

            // 开始服务器部分计时
            long server_time1 = System.nanoTime();

            // Step 1: 检查 SS[Kw] 的状态 (服务器)
            // 如果不存在，则初始化为全0的BigInteger
            BigInteger ep = SS.getOrDefault(new String(Kw, StandardCharsets.UTF_8), BigInteger.ZERO); // 从SS中读取
            // Step 2: 初始化一个空的map来存储结果E (服务器)
            Map<Integer, BigInteger[]> E = new HashMap<>();

            // Step 3: 从c到c0进行循环 (服务器)
            int Ri = Rc;
            for (int i = c; i >= c0; i--) {
                // Step 4: 计算I
                byte[] I = hashFunction(Kw, Ri);

                // Step 5: 从PDB中检索密文
                Object[] ciphertext = PDB.get(new String(I, StandardCharsets.UTF_8));

                // Step 6: 将检索到的密文存储在E中 (只存储ea和eb)
                BigInteger ea = (BigInteger) ciphertext[1];
                BigInteger eb = (BigInteger) ciphertext[2];
                E.put(i - c0, new BigInteger[]{ea, eb});

                // Step 7: 从PDB中移除该密文
                PDB.remove(new String(I, StandardCharsets.UTF_8));

                // Step 8: 更新Ri-1 = C ⊕ H2(Kw, Ri)
                byte[] C = (byte[]) ciphertext[0];
                Ri = new BigInteger(C).xor(new BigInteger(hashFunction(Kw, Ri))).intValue();
            }

            // 服务器部分结束计时
            long server_time2 = System.nanoTime();

            // 开始客户端接收并处理部分计时
            long client_time3 = System.nanoTime();

            // Step 1: 初始化匹配文件的位图 (客户端)
            BigInteger bsp = BigInteger.ZERO;

            // Step 2-3: 解密ep (客户端)
            if (!ep.equals(BigInteger.ZERO)) {
                byte[] H5 = hashFunction(Kw_prime, c0);
                BigInteger H5BigInt = new BigInteger(1, H5);
                bsp = ep.xor(H5BigInt);  // 执行异或操作
            }

            // Step 5: 循环解密每个密文并更新bsw (客户端)
            for (int i = c0; i <= c; i++) {
                BigInteger[] encryptPDBiBitmap = E.get(i - c0);
                BigInteger ea = encryptPDBiBitmap[0];
                BigInteger eb = encryptPDBiBitmap[1];

                BigInteger hashKw_prime_H3 = new BigInteger(1, hashFunction(Kw_prime, i));
//                BigInteger hashKw_prime_H4 = new BigInteger(1, hashFunction(Kw_prime, i));
                BigInteger bsa = ea.xor(hashKw_prime_H3);
                BigInteger bsb = eb.xor(hashKw_prime_H3);

                // 更新bsw,not按位取反，negate取负数
                bsp = bsp.and(bsa.not()).xor(bsa.and(bsb));
            }

            // Step 10: 更新客户端状态
            int Rc_plus_1 = getRandomFromPool();
            SC.put(p, new int[]{c + 1, c, Rc_plus_1});
            // Step 11: 重新加密 bsw
            byte[] H5 = hashFunction(Kw_prime, c + 1);
            ep = bsp.xor(new BigInteger(1, H5));

            // 客户端接收部分结束计时
            long client_time4 = System.nanoTime();
            // 记录结束时间并计算本次迭代的耗时
            long loopEndTime = System.nanoTime();
            long loopDurationNs = loopEndTime - loopStartTime;
            totalLoopTime += loopDurationNs; // 累加每次迭代的时间

//            double loopDurationMs = loopDurationNs / 1_000_000.0;
            //System.out.println(" search took " + loopDurationNs + " ns (" + loopDurationMs + " ms).");

            // Step 13: 将新的加密状态 ep 发送到服务器
            PDB.put(new String(Kw, StandardCharsets.UTF_8), new Object[]{ep});

            // 服务器更新 SS
            SS.put(new String(Kw, StandardCharsets.UTF_8), ep);
            // 输出客户端和服务器端的时间消耗
            double msclient_time1 = (client_time2 - client_time1) / 1_000_000.0;
            double msclient_time2 = (client_time4 - client_time3) / 1_000_000.0;
            double msserver_time = (server_time2 - server_time1) / 1_000_000.0;
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
            int[] state = SC.getOrDefault(w, new int[]{0, -1, getRandomFromPool()});

            // Step 3: 检查状态是否为null

            // 记录 Rc, c0, c (客户端)
            int Rc = state[2];
            int c0 = state[0];
            int c = state[1];

            // 客户端部分结束计时
            long client_time2 = System.nanoTime();

            // 开始服务器部分计时
            long server_time1 = System.nanoTime();

            // Step 1: 检查 SS[Kw] 的状态 (服务器)
            // 如果不存在，则初始化为全0的BigInteger
            BigInteger ew = SS.getOrDefault(new String(Kw, StandardCharsets.UTF_8), BigInteger.ZERO); // 从SS中读取
            // Step 2: 初始化一个空的map来存储结果E (服务器)
            Map<Integer, BigInteger[]> E = new HashMap<>();

            // Step 3: 从c到c0进行循环 (服务器)
            int Ri = Rc;
            for (int i = c; i >= c0; i--) {
                // Step 4: 计算I
                byte[] I = hashFunction(Kw, Ri);

                // Step 5: 从PDB中检索密文
                Object[] ciphertext = KDB.get(new String(I, StandardCharsets.UTF_8));

                // Step 6: 将检索到的密文存储在E中 (只存储ea和eb)
                BigInteger ea = (BigInteger) ciphertext[1];
                BigInteger eb = (BigInteger) ciphertext[2];
                E.put(i - c0, new BigInteger[]{ea, eb});

                // Step 7: 从PDB中移除该密文
                KDB.remove(new String(I, StandardCharsets.UTF_8));

                // Step 8: 更新Ri-1 = C ⊕ H2(Kw, Ri)
                byte[] C = (byte[]) ciphertext[0];
                Ri = new BigInteger(C).xor(new BigInteger(hashFunction(Kw, Ri))).intValue();
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
            int Rc_plus_1 = getRandomFromPool();
            SC.put(w, new int[]{c + 1, c, Rc_plus_1});
            // Step 11: 重新加密 bsw
            byte[] H5 = hashFunction(Kw_prime, c + 1);
            ew = bsw.xor(new BigInteger(1, H5));

            // 客户端接收部分结束计时
            long client_time4 = System.nanoTime();
            // 记录结束时间并计算本次迭代的耗时
            long loopEndTime = System.nanoTime();
            long loopDurationNs = loopEndTime - loopStartTime;
            totalLoopTime += loopDurationNs; // 累加每次迭代的时间

//            double loopDurationMs = loopDurationNs / 1_000_000.0;
            //System.out.println(" search took " + loopDurationNs + " ns (" + loopDurationMs + " ms).");

            // Step 13: 将新的加密状态 ew 发送到服务器
            KDB.put(new String(Kw, StandardCharsets.UTF_8), new Object[]{ew});

            // 服务器更新 SS
            SS.put(new String(Kw, StandardCharsets.UTF_8), ew);
            // 输出客户端和服务器端的时间消耗
            double msclient_time1 = (client_time2 - client_time1) / 1_000_000.0;
            double msclient_time2 = (client_time4 - client_time3) / 1_000_000.0;
            double msserver_time = (server_time2 - server_time1) / 1_000_000.0;
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
        // 输出总耗时
        double totalLoopTimeMs = totalLoopTime / 1_000_000.0;
//        System.out.println("Total loop time: " + totalLoopTime + " ns (" + totalLoopTimeMs + " ms).");
//        System.out.println("SPQS_BITSET Total search time: " + (totalLoopTime+precoverTime) + " ns (" + (totalLoopTimeMs+(precoverTime/1_000_000.0)) + " ms).");
        return Sump.and(Sumw);
    }
    public void ObjectUpdate(long[] pSet, String[] W, String op, int[] files, int CounterLimits) throws Exception {
        byte[] combinedKey;
        byte[] Kw;
        byte[] Kw_prime;
        long startTime = System.nanoTime();
        List<String> P = preCode(pSet);
        for (String p : P) {
            //Client
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            Kw = Arrays.copyOfRange(combinedKey, 0, LAMBDA / 8);
            Kw_prime = Arrays.copyOfRange(combinedKey, LAMBDA / 8, LAMBDA / 4); // 假设 LAMBDA / 4 是所需的长度

            // Step 2: 获取客户端的当前关键词状态
            int[] state = SC.getOrDefault(p, new int[]{0, -1, getRandomFromPool()});
            // Step 3: 随机生成 Rc+1
            int Rc_plus_1 = getRandomFromPool();

            // Step 4: 计算 I,C = hashFunction(Kw, Rc_plus_1) ⊕ state[2]
            byte[] I = hashFunction(Kw, Rc_plus_1); // 根据Kw和Rc+1计算索引I
            byte[] C = xorBytes(hashFunction(Kw, Rc_plus_1), intToBytes(state[2]));

            // Step 5: 根据操作选择 bi-bitmap (bsa, bsb)
            BigInteger bsa = BigInteger.ZERO;  // 使用 BigInteger 作为位图
            BigInteger bsb = BigInteger.ZERO;
            // 根据操作设置 bsa 和 bsb
            for (int fileIndex : files) {
                bsa = bsa.setBit(fileIndex);  // 添加操作，设置bsa中相应位为1
                if ("add".equals(op)) {
                    bsb = bsb.setBit(fileIndex);  // 添加操作，设置bsb中相应位为1
                }
            }

//            // 根据操作设置 bsa 和 bsb
//            for (int i = 0; i < files.length; i++) {
//                bsa = bsa.setBit(files[i]);  // 添加操作，设置bsa中相应位为1
//                if ("add".equals(op[i])) {
//                    bsb = bsb.setBit(files[i]);  // 添加操作，设置bsb中相应位为1
//                }  //bsb = bsb.clearBit(fileIndex);  // 删除操作，清除bsb中相应位（设置为0）
//
//            }
            // Step 6: 加密 bsa 和 bsb，不使用加密函数，而是和 hashFunction(Kw_prime, state[1] + 1) 异或
            BigInteger hashKw_prime = new BigInteger(1, hashFunction(Kw_prime, state[1] + 1));
//            BigInteger ea = bsa.xor(hashKw_prime);
//            BigInteger eb = bsb.xor(hashKw_prime);
            // Step 7: 更新客户端状态
            SC.put(p, new int[]{state[0], state[1] + 1, Rc_plus_1});
            //Server
            // Step 8: 将 (I, C, (ea, eb)) 发送到服务器（存入PDB）
            PDB.put(new String(I, StandardCharsets.UTF_8), new Object[]{C, bsa.xor(hashKw_prime), bsb.xor(hashKw_prime)});


        }
        long pTime = System.nanoTime();
        for (String w : W) {
            //Client
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            Kw = Arrays.copyOfRange(combinedKey, 0, LAMBDA / 8);
            Kw_prime = Arrays.copyOfRange(combinedKey, LAMBDA / 8, LAMBDA / 4); // 假设 LAMBDA / 4 是所需的长度

            // Step 2: 获取客户端的当前关键词状态
            int[] state = SC.getOrDefault(w, new int[]{0, -1, getRandomFromPool()});
            // Step 3: 随机生成 Rc+1
            int Rc_plus_1 = getRandomFromPool();

            // Step 4: 计算 I,C = hashFunction(Kw, Rc_plus_1) ⊕ state[2]
            byte[] I = hashFunction(Kw, Rc_plus_1); // 根据Kw和Rc+1计算索引I
            byte[] C = xorBytes(hashFunction(Kw, Rc_plus_1), intToBytes(state[2]));
            // Step 5: 根据操作选择 bi-bitmap (bsa, bsb)
            BigInteger bsa = BigInteger.ZERO;  // 使用 BigInteger 作为位图
            BigInteger bsb = BigInteger.ZERO;

            // 根据操作设置 bsa 和 bsb
            for (int fileIndex : files) {
                bsa = bsa.setBit(fileIndex);  // 添加操作，设置bsa中相应位为1
                if ("add".equals(op)) {
                    bsb = bsb.setBit(fileIndex);  // 添加操作，设置bsb中相应位为1
                }
            }
            for (int fileIndex : files) {
                if ("add".equals(op)) {
                    bsa = bsa.setBit(fileIndex);  // 添加操作，设置bsa中相应位为1
                    bsb = bsb.setBit(fileIndex);  // 添加操作，设置bsb中相应位为1
                } else if ("del".equals(op)) {
                    bsa = bsa.setBit(fileIndex);  // 删除操作，设置bsa中相应位为1
                    //bsb = bsb.clearBit(fileIndex);  // 删除操作，清除bsb中相应位（设置为0）
                }
            }
            // Step 6: 加密 bsa 和 bsb，不使用加密函数，而是和 hashFunction(Kw_prime, state[1] + 1) 异或
            BigInteger hashKw_prime = new BigInteger(1, hashFunction(Kw_prime, state[1] + 1));
            BigInteger ea = bsa.xor(hashKw_prime);
            BigInteger eb = bsb.xor(hashKw_prime);
            // Step 7: 更新客户端状态
            SC.put(w, new int[]{state[0], state[1] + 1, Rc_plus_1});
            //Server
            // Step 8: 将 (I, C, (ea, eb)) 发送到服务器（存入PDB）
            KDB.put(new String(I, StandardCharsets.UTF_8), new Object[]{C, ea, eb});
        }
        long wTime = System.nanoTime();
        // 输出总耗时
        double totalLoopTimeMs = (System.nanoTime()-startTime) / 1_000_000.0;
//        System.out.println("SPQS_BITSET ptime: " + (pTime-startTime) / 1_000_000.0 + " ms.");
//        System.out.println("SPQS_BITSET wtime: " + (wTime-pTime) / 1_000_000.0 + " ms.");
//        System.out.println("SPQS_BITSET Total update time: " + totalLoopTimeMs + " ms.");
        // 存储到列表中
        totalUpdateTimes.add(totalLoopTimeMs);
//        System.out.println("Update operation completed.");
    }
    public void ObjectUpdate(long[] pSet, String[] W, String[] op, int[] files, int CounterLimits) throws Exception {
        byte[] combinedKey;
        byte[] Kw;
        byte[] Kw_prime;
        long startTime = System.nanoTime();
        List<String> P = preCode(pSet);
        for (String p : P) {
            //Client
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            Kw = Arrays.copyOfRange(combinedKey, 0, LAMBDA / 8);
            Kw_prime = Arrays.copyOfRange(combinedKey, LAMBDA / 8, LAMBDA / 4); // 假设 LAMBDA / 4 是所需的长度

            // Step 2: 获取客户端的当前关键词状态
            int[] state = SC.getOrDefault(p, new int[]{0, -1, getRandomFromPool()});
            // Step 3: 随机生成 Rc+1
            int Rc_plus_1 = getRandomFromPool();

            // Step 4: 计算 I,C = hashFunction(Kw, Rc_plus_1) ⊕ state[2]
            byte[] I = hashFunction(Kw, Rc_plus_1); // 根据Kw和Rc+1计算索引I
            byte[] C = xorBytes(hashFunction(Kw, Rc_plus_1), intToBytes(state[2]));

            // Step 5: 根据操作选择 bi-bitmap (bsa, bsb)
            BigInteger bsa = BigInteger.ZERO;  // 使用 BigInteger 作为位图
            BigInteger bsb = BigInteger.ZERO;
            // 根据操作设置 bsa 和 bsb
            for (int i=0;i<files.length;i++) {
                if ("add".equals(op[i])) {
                    bsa = bsa.setBit(files[i]);  // 添加操作，设置bsa中相应位为1
                    bsb = bsb.setBit(files[i]);  // 添加操作，设置bsb中相应位为1
                } else if ("del".equals(op[i])) {
                    bsa = bsa.setBit(files[i]);  // 删除操作，设置bsa中相应位为1
                    //bsb = bsb.clearBit(fileIndex);  // 删除操作，清除bsb中相应位（设置为0）
                }
            }
            // Step 6: 加密 bsa 和 bsb，不使用加密函数，而是和 hashFunction(Kw_prime, state[1] + 1) 异或
            BigInteger hashKw_prime = new BigInteger(1, hashFunction(Kw_prime, state[1] + 1));
//            BigInteger ea = bsa.xor(hashKw_prime);
//            BigInteger eb = bsb.xor(hashKw_prime);
            // Step 7: 更新客户端状态
            SC.put(p, new int[]{state[0], state[1] + 1, Rc_plus_1});
            //Server
            // Step 8: 将 (I, C, (ea, eb)) 发送到服务器（存入PDB）
            PDB.put(new String(I, StandardCharsets.UTF_8), new Object[]{C, bsa.xor(hashKw_prime), bsb.xor(hashKw_prime)});


        }
        long pTime = System.nanoTime();
        for (String w : W) {
            //Client
            combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            Kw = Arrays.copyOfRange(combinedKey, 0, LAMBDA / 8);
            Kw_prime = Arrays.copyOfRange(combinedKey, LAMBDA / 8, LAMBDA / 4); // 假设 LAMBDA / 4 是所需的长度

            // Step 2: 获取客户端的当前关键词状态
            int[] state = SC.getOrDefault(w, new int[]{0, -1, getRandomFromPool()});
            // Step 3: 随机生成 Rc+1
            int Rc_plus_1 = getRandomFromPool();

            // Step 4: 计算 I,C = hashFunction(Kw, Rc_plus_1) ⊕ state[2]
            byte[] I = hashFunction(Kw, Rc_plus_1); // 根据Kw和Rc+1计算索引I
            byte[] C = xorBytes(hashFunction(Kw, Rc_plus_1), intToBytes(state[2]));
            // Step 5: 根据操作选择 bi-bitmap (bsa, bsb)
            BigInteger bsa = BigInteger.ZERO;  // 使用 BigInteger 作为位图
            BigInteger bsb = BigInteger.ZERO;

            // 根据操作设置 bsa 和 bsb
            for (int i=0;i<files.length;i++) {
                if ("add".equals(op[i])) {
                    bsa = bsa.setBit(files[i]);  // 添加操作，设置bsa中相应位为1
                    bsb = bsb.setBit(files[i]);  // 添加操作，设置bsb中相应位为1
                } else if ("del".equals(op[i])) {
                    bsa = bsa.setBit(files[i]);  // 删除操作，设置bsa中相应位为1
                    //bsb = bsb.clearBit(fileIndex);  // 删除操作，清除bsb中相应位（设置为0）
                }
            }
            // Step 6: 加密 bsa 和 bsb，不使用加密函数，而是和 hashFunction(Kw_prime, state[1] + 1) 异或
            BigInteger hashKw_prime = new BigInteger(1, hashFunction(Kw_prime, state[1] + 1));
//            BigInteger ea = bsa.xor(hashKw_prime);
//            BigInteger eb = bsb.xor(hashKw_prime);
            // Step 7: 更新客户端状态
            SC.put(w, new int[]{state[0], state[1] + 1, Rc_plus_1});
            //Server
            // Step 8: 将 (I, C, (ea, eb)) 发送到服务器（存入PDB）
            KDB.put(new String(I, StandardCharsets.UTF_8), new Object[]{C, bsa.xor(hashKw_prime), bsb.xor(hashKw_prime)});
        }
        long wTime = System.nanoTime();
        // 输出总耗时
        double totalLoopTimeMs = (System.nanoTime()-startTime) / 1_000_000.0;
//        System.out.println("SPQS_BITSET ptime: " + (pTime-startTime) / 1_000_000.0 + " ms.");
//        System.out.println("SPQS_BITSET wtime: " + (wTime-pTime) / 1_000_000.0 + " ms.");
//        System.out.println("SPQS_BITSET Total update time: " + totalLoopTimeMs + " ms.");
        // 存储到列表中
        totalUpdateTimes.add(totalLoopTimeMs);
//        System.out.println("Update operation completed.");
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
     * 安全哈希函数 H1, H2, H3, H4, H5
     *
     * @param input1 输入值1
     * @param input2 输入值2
     * @return 哈希后的值
     * @throws NoSuchAlgorithmException
     */
    private byte[] hashFunction(byte[] input1, int input2) {
        messageDigest.reset(); // 重置MessageDigest实例
        messageDigest.update(input1);
        // 将int转为byte并更新到MessageDigest
        intBuffer[0] = (byte) (input2 >> 24);
        intBuffer[1] = (byte) (input2 >> 16);
        intBuffer[2] = (byte) (input2 >> 8);
        intBuffer[3] = (byte) input2;
        messageDigest.update(intBuffer);
        return messageDigest.digest();
    }

    /**
     * 异或操作，将两个字节数组进行逐位异或
     *
     * @param a 字节数组a
     * @param b 字节数组b
     * @return 异或后的结果
     */
    private byte[] xorBytes(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i % b.length]);
        }
        return result;
    }

    /**
     * 将整数转换为字节数组
     *
     * @param value 整数
     * @return 字节数组
     */
    private byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
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
        // 定义参数
        int maxFiles = 1 << 20; // 最大文件数，2^20
        int order = 17; // Hilbert curve 阶数
        int dimension = 2; // 维度

        // 初始化 RSKQ_SearchCorrectness 实例
        RSKQ_SearchCorrectness spqs = new RSKQ_SearchCorrectness(maxFiles, order, dimension);

        // 模拟一些数据
        Random random = new Random();
        int numObjects = 1; // 插入5个对象进行测试
        int rangePredicate = 1000;

        // 初始化测试对象的数据
        long[][] pSets = new long[numObjects][2];
        String[][] WSets = new String[numObjects][6]; // 假设每个对象有6个关键词
        int[][] fileSets = new int[numObjects][1]; // 每个对象关联一个文件

        // 填充对象数据
        for (int i = 0; i < numObjects; i++) {
            // 创建pSet (二维数据)
            pSets[i][0] = random.nextInt(1<<(order-1));
            pSets[i][1] = pSets[i][0] + 1;

            // 创建关键词W
            for (int j = 0; j < 6; j++) {
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

        if (spqs.SC != null) {
            for (Map.Entry<String, int[]> entry : spqs.SC.entrySet()) {
                System.out.println(entry.getKey() + ": " + Arrays.toString(entry.getValue()));
            }
        } else {
            System.out.println("Map is empty or not initialized.");
        }
        // 执行 update 操作（插入数据）
        System.out.println("插入操作开始...");
        for (int i = 0; i < numObjects; i++) {
//            spqs.ObjectUpdate(pSets[i], WSets[i], "add", fileSets[i], rangePredicate);
            spqs.ObjectUpdate(pSets[i], WSets[i], new String[]{"add"}, fileSets[i], rangePredicate);
        }
        System.out.println("插入操作完成。");

        // 测试 search 操作
        System.out.println("开始搜索...");
        for (int i = 0; i < numObjects; i++) {
            // 通过 Hilbert 曲线计算范围
            BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSets[i]);
            System.out.println("Hilbert Index:"+pointHilbertIndex);
            BigInteger R_min = pointHilbertIndex.subtract(BigInteger.valueOf(100));
            BigInteger R_max = pointHilbertIndex.add(BigInteger.valueOf(100));

            // 执行搜索操作
            BigInteger result = spqs.ObjectSearch(R_min, R_max, WSets[i]);

            // 打印搜索结果
            System.out.println("\n搜索结果 (pSet " + Arrays.toString(pSets[i]) + "): ");
            findIndexesOfOne(result); // 打印出结果中的位图索引
        }
        System.out.println("搜索操作完成。");
    }
}
