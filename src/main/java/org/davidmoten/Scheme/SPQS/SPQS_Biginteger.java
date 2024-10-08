package org.davidmoten.Scheme.SPQS;


import org.davidmoten.BPC.BPCGenerator;
import org.davidmoten.Hilbert.HilbertComponent.HilbertCurve;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SPQS_Biginteger {

    public static final int LAMBDA = 128;  // 安全参数 λ

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
//    private int maxFiles; // 最大文件数
    private int dimension; // 2维数据
    private int order; // Hilbert curve 阶数
    public HilbertCurve hilbertCurve;
    private BPCGenerator bpcGenerator;

    // 修改后的构造函数
    public SPQS_Biginteger(int order, int dimension) {
//        this.maxnums_w = maxnums_w;
//        this.filePath = filePath;
//        this.maxFiles = maxFiles;

        this.SC = new HashMap<>();
        this.SS = new HashMap<>();
        this.PDB = new ConcurrentHashMap<>();
        this.KDB = new ConcurrentHashMap<>();

        this.order = order;
        this.dimension = dimension;
        this.hilbertCurve = HilbertCurve.bits(order).dimensions(dimension);
        this.bpcGenerator = new BPCGenerator(order * 2); // Hilbert曲线编码最大值为2^(2*order)
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

        // 打印 Hilbert 索引的值
        System.out.println("Hilbert Index (BigInteger): " + pointHilbertIndex);

        // 将 Hilbert 索引转换为二进制字符串，并确保其长度为 2 * order 位
        String hilbertBinary = pointHilbertIndex.toString(2);
        int requiredLength = 2 * order;

        // 如果二进制字符串长度不足，前面补0
        hilbertBinary = String.format("%" + requiredLength + "s", hilbertBinary).replace(' ', '0');

        // 打印二进制表示及其长度
//        System.out.println("Hilbert Index (Binary): " + hilbertBinary);
//        System.out.println("Length of Hilbert Binary: " + hilbertBinary.length());

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
        System.out.println("BPC2:" + BinaryResults);
        return BinaryResults;
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

    // 更新操作
//    public void ObjectUpdate(long[] pSet, String[] W, String op, int[] files, int CounterLimits) throws Exception {
//        System.out.println("Starting update operation...");
////        System.out.println("Input pSet: " + Arrays.toString(pSet));
////        System.out.println("Input W: " + Arrays.toString(W));
//        long totalLoopTime = 0; // 初始化总时间变量
//        // 记录开始时间
//        long startTime = System.nanoTime();
//        List<String> P = preCode(pSet);
////        System.out.println("PreCode P: " + P);
//        long precodeTime = System.nanoTime() - startTime;
//        System.out.println("PreCode time: " + precodeTime + " ns (" + (precodeTime/1_000_000.0) + " ms).");
//        for (String p : P) {
//            //update(p, op, files);
//            // 记录单次循环的开始时间
//            long loopStartTime = System.nanoTime();
//            //Client
//            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
//            byte[] Kw = new byte[LAMBDA / 8];
//            byte[] Kw_prime = new byte[LAMBDA / 8];
//            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
//            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);
//
//            // Step 2: 获取客户端的当前关键词状态
//            int[] state = SC.getOrDefault(p, new int[]{0, -1, getRandomFromPool()});
//            // Step 3: 随机生成 Rc+1
//            int Rc_plus_1 = getRandomFromPool();
//
//            // Step 4: 计算 I,C = hashFunction(Kw, Rc_plus_1) ⊕ state[2]
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
//            // 记录结束时间并计算本次迭代的耗时
//            long loopEndTime = System.nanoTime();
//            long loopDurationNs = loopEndTime - loopStartTime;
//            totalLoopTime += loopDurationNs; // 累加每次迭代的时间
//
//            double loopDurationMs = loopDurationNs / 1_000_000.0;
////            System.out.println(op + " operation took " + loopDurationNs + " ns (" + loopDurationMs + " ms).");
//
//            // Step 7: 更新客户端状态
//            SC.put(p, new int[]{state[0], state[1] + 1, Rc_plus_1});
//            //Server
//            // Step 8: 将 (I, C, (ea, eb)) 发送到服务器（存入PDB）
//            PDB.put(new String(I, StandardCharsets.UTF_8), new Object[]{C, ea, eb});
//        }
//        for (String w : W) {
//            //update(p, op, files);
//            // 记录单次循环的开始时间
//            long loopStartTime = System.nanoTime();
//            //Client
//            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
//            byte[] Kw = new byte[LAMBDA / 8];
//            byte[] Kw_prime = new byte[LAMBDA / 8];
//            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
//            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);
//
//            // Step 2: 获取客户端的当前关键词状态
//            int[] state = SC.getOrDefault(w, new int[]{0, -1, getRandomFromPool()});
//            // Step 3: 随机生成 Rc+1
//            int Rc_plus_1 = getRandomFromPool();
//
//            // Step 4: 计算 I,C = hashFunction(Kw, Rc_plus_1) ⊕ state[2]
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
//            // 记录结束时间并计算本次迭代的耗时
//            long loopEndTime = System.nanoTime();
//            long loopDurationNs = loopEndTime - loopStartTime;
//            totalLoopTime += loopDurationNs; // 累加每次迭代的时间
//
//            double loopDurationMs = loopDurationNs / 1_000_000.0;
////            System.out.println(op + " operation took " + loopDurationNs + " ns (" + loopDurationMs + " ms).");
//
//            // Step 7: 更新客户端状态
//            SC.put(w, new int[]{state[0], state[1] + 1, Rc_plus_1});
//            //Server
//            // Step 8: 将 (I, C, (ea, eb)) 发送到服务器（存入PDB）
//            KDB.put(new String(I, StandardCharsets.UTF_8), new Object[]{C, ea, eb});
//        }
//
//        // 输出总耗时
//        double totalLoopTimeMs = totalLoopTime / 1_000_000.0;
//        System.out.println("Total loop time: " + totalLoopTime + " ns (" + totalLoopTimeMs + " ms).");
//        System.out.println("Total update time: " + (totalLoopTime+precodeTime) + " ns (" + (totalLoopTimeMs+(precodeTime/1_000_000.0)) + " ms).");
//
//        System.out.println("Update operation completed.");
//    }

    public BigInteger ObjectSearch(BigInteger R_min, BigInteger R_max, String[] WQ) throws Exception {
        long totalLoopTime = 0; // 初始化总时间变量
        // 客户端：生成搜索请求
        long startTime = System.nanoTime();
        List<String> BPC = preCover(R_min, R_max);
        long precoverTime = System.nanoTime() - startTime;
//        System.out.println("PreCover time: " + precoverTime + " ns (" + (precoverTime/1_000_000.0) + " ms).");
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
            BigInteger ep;
            // 如果不存在，则初始化为全0的BigInteger
            ep = SS.getOrDefault(new String(Kw, StandardCharsets.UTF_8), BigInteger.ZERO); // 从SS中读取
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

            // Step 2-3: 解密ew (客户端)
            if (!ep.equals(BigInteger.ZERO)) {
                bsp = ep;
                byte[] H5 = hashFunction(Kw_prime, c0);
                BigInteger H5BigInt = new BigInteger(1, H5);
                bsp = bsp.xor(H5BigInt);  // 执行异或操作
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
            double total_time = msclient_time1 + msclient_time2 + msserver_time;
            System.out.println("prefix encode: Client time part 1: " + msclient_time1 + " ms, Client time part 2: " + msclient_time2 + " ms, Server time: " + msserver_time + " ms, Total time: " + total_time + " ms");

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
            BigInteger ew;
            // 如果不存在，则初始化为全0的BigInteger
            ew = SS.getOrDefault(new String(Kw, StandardCharsets.UTF_8), BigInteger.ZERO); // 从SS中读取
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
            double total_time = msclient_time1 + msclient_time2 + msserver_time;
            System.out.println("keyword: Client time part 1: " + msclient_time1 + " ms, Client time part 2: " + msclient_time2 + " ms, Server time: " + msserver_time + " ms, Total time: " + total_time + " ms");

//            System.out.println(" search took " + loopDurationNs + " ns (" + loopDurationMs + " ms).");
            Sumw = Sumw.or(bsw);
        }
        // 输出总耗时
        double totalLoopTimeMs = totalLoopTime / 1_000_000.0;
        System.out.println("Total loop time: " + totalLoopTime + " ns (" + totalLoopTimeMs + " ms).");
        System.out.println("SPQS_Biginteger Total search time: " + (totalLoopTime+precoverTime) + " ns (" + (totalLoopTimeMs+(precoverTime/1_000_000.0)) + " ms).");
        return Sump.and(Sumw);
    }
    public void ObjectUpdate(long[] pSet, String[] W, String op, int[] files, int CounterLimits) throws Exception {
        long startTime = System.nanoTime();
        List<String> P = preCode(pSet);
        for (String p : P) {
            //Client
            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
            byte[] Kw = new byte[LAMBDA / 8];
            byte[] Kw_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);

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
            SC.put(p, new int[]{state[0], state[1] + 1, Rc_plus_1});
            //Server
            // Step 8: 将 (I, C, (ea, eb)) 发送到服务器（存入PDB）
            PDB.put(new String(I, StandardCharsets.UTF_8), new Object[]{C, ea, eb});
        }
        for (String w : W) {
            //Client
            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
            byte[] Kw = new byte[LAMBDA / 8];
            byte[] Kw_prime = new byte[LAMBDA / 8];
            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);

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

        // 输出总耗时
        double totalLoopTimeMs = (System.nanoTime()-startTime) / 1_000_000.0;
        System.out.println("SPQS_Biginteger Total update time: " + totalLoopTimeMs + " ms.");
        System.out.println("Update operation completed.");
    }

//    public BigInteger ObjectSearch(BigInteger R_min, BigInteger R_max, String[] WQ) throws Exception {
//        // 客户端：生成搜索请求
//        long startTime = System.nanoTime();
//        List<String> BPC = preCover(R_min, R_max);
//        //存储p位图结果
//        BigInteger Sump = BigInteger.ZERO;
//        // 客户端处理前缀集合
//        for (String p : BPC) {
//            // Step 1: 生成Kw和Kw_prime (客户端)
//            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], p);
//            byte[] Kw = new byte[LAMBDA / 8];
//            byte[] Kw_prime = new byte[LAMBDA / 8];
//            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
//            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);
//
//            // Step 2: 获取客户端的当前关键词状态
//            int[] state = SC.getOrDefault(p, new int[]{0, -1, getRandomFromPool()});
//
//            // Step 3: 检查状态是否为null
//
//            // 记录 Rc, c0, c (客户端)
//            int Rc = state[2];
//            int c0 = state[0];
//            int c = state[1];
//
//            // Step 1: 检查 SS[Kw] 的状态 (服务器)
//            BigInteger ew;
//            // 如果不存在，则初始化为全0的BigInteger
//            ew = SS.getOrDefault(new String(Kw, StandardCharsets.UTF_8), BigInteger.ZERO); // 从SS中读取
//            // Step 2: 初始化一个空的map来存储结果E (服务器)
//            Map<Integer, BigInteger[]> E = new HashMap<>();
//
//            // Step 3: 从c到c0进行循环 (服务器)
//            int Ri = Rc;
//            for (int i = c; i >= c0; i--) {
//                // Step 4: 计算I
//                byte[] I = hashFunction(Kw, Ri);
//
//                // Step 5: 从PDB中检索密文
//                Object[] ciphertext = PDB.get(new String(I, StandardCharsets.UTF_8));
//
//                // Step 6: 将检索到的密文存储在E中 (只存储ea和eb)
//                BigInteger ea = (BigInteger) ciphertext[1];
//                BigInteger eb = (BigInteger) ciphertext[2];
//                E.put(i - c0, new BigInteger[]{ea, eb});
//
//                // Step 7: 从PDB中移除该密文
//                PDB.remove(new String(I, StandardCharsets.UTF_8));
//
//                // Step 8: 更新Ri-1 = C ⊕ H2(Kw, Ri)
//                byte[] C = (byte[]) ciphertext[0];
//                Ri = new BigInteger(C).xor(new BigInteger(hashFunction(Kw, Ri))).intValue();
//            }
//            // Step 1: 初始化匹配文件的位图 (客户端)
//            BigInteger bsw = BigInteger.ZERO;
//            // Step 2-3: 解密ew (客户端)
//            if (!ew.equals(BigInteger.ZERO)) {
//                bsw = ew;
//                byte[] H5 = hashFunction(Kw_prime, c0);
//                BigInteger H5BigInt = new BigInteger(1, H5);
//                bsw = bsw.xor(H5BigInt);  // 执行异或操作
//            }
//
//            // Step 5: 循环解密每个密文并更新bsw (客户端)
//            for (int i = c0; i <= c; i++) {
//                BigInteger[] encryptPDBiBitmap = E.get(i - c0);
//                BigInteger ea = encryptPDBiBitmap[0];
//                BigInteger eb = encryptPDBiBitmap[1];
//
//                BigInteger hashKw_prime_H3 = new BigInteger(1, hashFunction(Kw_prime, i));
//                BigInteger hashKw_prime_H4 = new BigInteger(1, hashFunction(Kw_prime, i));
//                BigInteger bsa = ea.xor(hashKw_prime_H3);
//                BigInteger bsb = eb.xor(hashKw_prime_H4);
//
//                // 更新bsw,not按位取反，negate取负数
//                bsw = bsw.and(bsa.not()).xor(bsa.and(bsb));
//            }
//
//            // Step 10: 更新客户端状态
//            int Rc_plus_1 = getRandomFromPool();
//            SC.put(p, new int[]{c + 1, c, Rc_plus_1});
//            // Step 11: 重新加密 bsw
//            byte[] H5 = hashFunction(Kw_prime, c + 1);
//            ew = bsw.xor(new BigInteger(1, H5));
//
//            // Step 13: 将新的加密状态 ew 发送到服务器
//            PDB.put(new String(Kw, StandardCharsets.UTF_8), new Object[]{ew});
//
//            // 服务器更新 SS
//            SS.put(new String(Kw, StandardCharsets.UTF_8), ew);
//            Sump = Sump.or(bsw);
//        }
//        //存储w位图结果
//        BigInteger Sumw = BigInteger.ZERO;
//        // 客户端处理关键字集合
//        for (String w : WQ) {
//            // Step 1: 生成Kw和Kw_prime (客户端)
//            byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], w);
//            byte[] Kw = new byte[LAMBDA / 8];
//            byte[] Kw_prime = new byte[LAMBDA / 8];
//            System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
//            System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);
//
//            // Step 2: 获取客户端的当前关键词状态
//            int[] state = SC.getOrDefault(w, new int[]{0, -1, getRandomFromPool()});
//            // Step 3: 检查状态是否为null
//            // 记录 Rc, c0, c (客户端)
//            int Rc = state[2];
//            int c0 = state[0];
//            int c = state[1];
//            // Step 1: 检查 SS[Kw] 的状态 (服务器)
//            BigInteger ew;
//            // 如果不存在，则初始化为全0的BigInteger
//            ew = SS.getOrDefault(new String(Kw, StandardCharsets.UTF_8), BigInteger.ZERO); // 从SS中读取
//            // Step 2: 初始化一个空的map来存储结果E (服务器)
//            Map<Integer, BigInteger[]> E = new HashMap<>();
//
//            // Step 3: 从c到c0进行循环 (服务器)
//            int Ri = Rc;
//            for (int i = c; i >= c0; i--) {
//                // Step 4: 计算I
//                byte[] I = hashFunction(Kw, Ri);
//
//                // Step 5: 从PDB中检索密文
//                Object[] ciphertext = KDB.get(new String(I, StandardCharsets.UTF_8));
//
//                // Step 6: 将检索到的密文存储在E中 (只存储ea和eb)
//                BigInteger ea = (BigInteger) ciphertext[1];
//                BigInteger eb = (BigInteger) ciphertext[2];
//                E.put(i - c0, new BigInteger[]{ea, eb});
//
//                // Step 7: 从PDB中移除该密文
//                KDB.remove(new String(I, StandardCharsets.UTF_8));
//
//                // Step 8: 更新Ri-1 = C ⊕ H2(Kw, Ri)
//                byte[] C = (byte[]) ciphertext[0];
//                Ri = new BigInteger(C).xor(new BigInteger(hashFunction(Kw, Ri))).intValue();
//            }
//            // Step 1: 初始化匹配文件的位图 (客户端)
//            BigInteger bsw = BigInteger.ZERO;
//
//            // Step 2-3: 解密ew (客户端)
//            if (!ew.equals(BigInteger.ZERO)) {
//                bsw = ew;
//                byte[] H5 = hashFunction(Kw_prime, c0);
//                BigInteger H5BigInt = new BigInteger(1, H5);
//                bsw = bsw.xor(H5BigInt);  // 执行异或操作
//            }
//
//            // Step 5: 循环解密每个密文并更新bsw (客户端)
//            for (int i = c0; i <= c; i++) {
//                BigInteger[] encryptPDBiBitmap = E.get(i - c0);
//                BigInteger ea = encryptPDBiBitmap[0];
//                BigInteger eb = encryptPDBiBitmap[1];
//
//                BigInteger hashKw_prime_H3 = new BigInteger(1, hashFunction(Kw_prime, i));
//                BigInteger hashKw_prime_H4 = new BigInteger(1, hashFunction(Kw_prime, i));
//                BigInteger bsa = ea.xor(hashKw_prime_H3);
//                BigInteger bsb = eb.xor(hashKw_prime_H4);
//
//                // 更新bsw,not按位取反，negate取负数
//                bsw = bsw.and(bsa.not()).xor(bsa.and(bsb));
//            }
//
//            // Step 10: 更新客户端状态
//            int Rc_plus_1 = getRandomFromPool();
//            SC.put(w, new int[]{c + 1, c, Rc_plus_1});
//            // Step 11: 重新加密 bsw
//            byte[] H5 = hashFunction(Kw_prime, c + 1);
//            ew = bsw.xor(new BigInteger(1, H5));
//            // Step 13: 将新的加密状态 ew 发送到服务器
//            KDB.put(new String(Kw, StandardCharsets.UTF_8), new Object[]{ew});
//
//            // 服务器更新 SS
//            SS.put(new String(Kw, StandardCharsets.UTF_8), ew);
//                        Sumw = Sumw.or(bsw);
//        }
//        // 输出总耗时
//        double totalLoopTimeMs = (System.nanoTime()-startTime) / 1_000_000.0;
//        System.out.println("SPQS_Biginteger Total search time: " + totalLoopTimeMs + " ms).");
//        return Sump.and(Sumw);
//    }


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
    private byte[] hashFunction(byte[] input1, int input2) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        md.update(input1);
        md.update(intToBytes(input2));
        return md.digest();
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

    // 测试SR-DSSE算法的运行
    public static void main(String[] args) throws Exception {
//        SPQS_Biginteger spqs = new SPQS_Biginteger();
//        spqs.setup(LAMBDA, MAX_FILES);

        // 示例文件的索引
//        int[] fileIndexes = {0, 2, 1 << 19};  // 假设文件有索引0, 2, 1<<19 + 1
//        int[] fileIndexes1 = {2, 3};  // 假设文件有索引0, 2, 1<<19 + 1

//        spqs.update("w1", "add", fileIndexes);
//        spqs.update("w1", "add", fileIndexes1);
//        spqs.update("w3", "del", fileIndexes);
//        spqs.update("w1", "del", fileIndexes);
//        spqs.search("w1");
//        spqs.search("w3");

        // 进行主流程测试
//        while (true) {
//            // 模拟从数据集中获取一项数据
//            Object[] result = GetRandomItem(12);
//            if (result == null) {
//                System.out.println("获取数据失败。");
//                continue;
//            }
//
//            int[] files = new int[]{((BigInteger) result[0]).intValue()};
//            long[] pSet = (long[]) result[1];
//            String[] W = (String[]) result[2];
//            BigInteger pointHilbertIndex = spqs.hilbertCurve.index(pSet);
//            // 统计 update 方法的运行时间
//            spqs.ObjectUpdate(pSet, W, "add", files, maxnums_w);
//
//            // 获取用户输入的搜索参数
//            Scanner scanner = new Scanner(System.in);
//
//            System.out.print("请输入 R_min: ");
//            BigInteger R_min = scanner.nextBigInteger();
//
//            System.out.print("请输入 R_max: ");
//            BigInteger R_max = scanner.nextBigInteger();
//
//            System.out.print("请输入关键字数量: ");
//            int WQSize = scanner.nextInt();
//            scanner.nextLine(); // 清除换行符
//
//            String[] WQ = new String[WQSize];
//            for (int i = 0; i < WQSize; i++) {
//                System.out.print("请输入关键字 WQ[" + i + "]: ");
//                WQ[i] = scanner.nextLine();
//            }
//
//            BigInteger BR = spqs.ObjectSearch(R_min, R_max, WQ);
//            // 返回最终解密后的位图信息 BR
//            findIndexesOfOne(BR);
//
//            spqs.ObjectUpdate(pSet, W, "del", files, maxnums_w);
//
//            // 继续进行下一个循环
//            System.out.print("是否继续？(yes/no): ");
//            String response = scanner.nextLine();
//            if (!response.equalsIgnoreCase("yes")) {
//                break;
//            }
//        }

        System.out.println("程序结束。");
    }
}

