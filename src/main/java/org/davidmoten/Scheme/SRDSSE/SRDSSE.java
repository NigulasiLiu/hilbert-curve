package org.davidmoten.Scheme.SRDSSE;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SRDSSE {

    private static final int LAMBDA = 128;  // 安全参数 λ
    private static final int MAX_FILES = 1 << 20; // 最大文件数目 2^20
    private static final Random random = new Random();

    private HashMap<String, int[]> SC;  // 客户端状态
    private HashMap<String, BigInteger> SS;  // 服务器状态
    private ConcurrentHashMap<String, Object[]> EDB; // 服务器存储的密文数据库

    // 伪随机函数的哈希算法
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HASH_ALGORITHM = "SHA-256";

    // 预生成的随机数池
    private final int[] randomPool;
    private int poolIndex;

    public SRDSSE() {
        this.SC = new HashMap<>();
        this.SS = new HashMap<>();
        this.EDB = new ConcurrentHashMap<>();
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

    /**
     * Setup 初始化操作
     *
     * @param lambda 安全参数
     * @param n      最大支持的文件数量
     */
    public void setup(int lambda, int n) {
        System.out.println("Setup complete with λ = " + lambda + ", n = " + n);
        // 初始化空的客户端状态SC和服务器存储的密文数据库EDB
        SC = new HashMap<>();
        EDB = new ConcurrentHashMap<>();
    }

    /**
     * Update 协议，用于添加/删除文件
     *
     * @param keyword   需要更新的关键词
     * @param operation 操作（add / delete）
     * @param file      输入的文件数组（表示多个文件的索引）
     * @throws Exception
     */
    public void update(String keyword, String operation, int[] file) throws Exception {
        // 记录开始时间
        long startTime = System.nanoTime();
        //Client
        byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], keyword);
        byte[] Kw = new byte[LAMBDA / 8];
        byte[] Kw_prime = new byte[LAMBDA / 8];
        System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
        System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);

        // Step 2: 获取客户端的当前关键词状态
        int[] state = SC.getOrDefault(keyword, new int[]{0, -1, getRandomFromPool()});

        // Step 3: 如果当前关键词状态不存在，初始化状态
        if (state[1] == -1) {
            state[0] = 0;  // 初始化c0
            state[1] = -1; // 初始化c
            state[2] = getRandomFromPool(); // 初始化随机数 Rc
        }

        // Step 4: 随机生成 Rc+1
        int Rc_plus_1 = getRandomFromPool();

        // Step 5: 计算 I,C = hashFunction(Kw, Rc_plus_1) ⊕ state[2]
        byte[] I = hashFunction(Kw, Rc_plus_1); // 根据Kw和Rc+1计算索引I
        byte[] C = xorBytes(hashFunction(Kw, Rc_plus_1), intToBytes(state[2]));

        // Step 6: 根据操作选择 bi-bitmap (bsa, bsb)
        BigInteger bsa = BigInteger.ZERO;  // 使用 BigInteger 作为位图
        BigInteger bsb = BigInteger.ZERO;

        // 根据操作设置 bsa 和 bsb
        for (int fileIndex : file) {
            if ("add".equals(operation)) {
                bsa = bsa.setBit(fileIndex);  // 添加操作，设置bsa中相应位为1
                bsb = bsb.setBit(fileIndex);  // 添加操作，设置bsb中相应位为1
            } else if ("del".equals(operation)) {
                bsa = bsa.setBit(fileIndex);  // 删除操作，设置bsa中相应位为1
                //bsb = bsb.clearBit(fileIndex);  // 删除操作，清除bsb中相应位（设置为0）
            }
        }

        // Step 7: 加密 bsa 和 bsb，不使用加密函数，而是和 hashFunction(Kw_prime, state[1] + 1) 异或
        BigInteger hashKw_prime = new BigInteger(1, hashFunction(Kw_prime, state[1] + 1));
        BigInteger ea = bsa.xor(hashKw_prime);
        BigInteger eb = bsb.xor(hashKw_prime);
        // 记录结束时间并计算耗时
        long endTime = System.nanoTime();
        long durationNs = endTime - startTime;
        double durationMs = durationNs / 1_000_000.0;
        System.out.println(operation + " operation took " + durationNs + " ns (" + durationMs + " ms).");

        //Server
        // Step 8: 将 (I, C, (ea, eb)) 发送到服务器（存入EDB）
        EDB.put(new String(I, StandardCharsets.UTF_8), new Object[]{C, ea, eb});

        // Step 9: 更新客户端状态
        SC.put(keyword, new int[]{state[0], state[1] + 1, Rc_plus_1});
    }

    /**
     * Search 协议，客户端生成搜索陷阱门，服务器检索并返回文件集合
     *
     * @param keyword 搜索关键词
     */
    public void search(String keyword) throws Exception {
        // 客户端部分计时
        long client_time1 = System.nanoTime();

        // Step 1: 生成Kw和Kw_prime (客户端)
        byte[] combinedKey = pseudoRandomFunction(new byte[LAMBDA], keyword);
        byte[] Kw = new byte[LAMBDA / 8];
        byte[] Kw_prime = new byte[LAMBDA / 8];
        System.arraycopy(combinedKey, 0, Kw, 0, LAMBDA / 8);
        System.arraycopy(combinedKey, LAMBDA / 8, Kw_prime, 0, LAMBDA / 8);

        // Step 2: 获取客户端的当前关键词状态
        int[] state = SC.getOrDefault(keyword, new int[]{0, -1, getRandomFromPool()});

        // Step 3: 检查状态是否为null
        if (state[1] == -1) {
            System.out.println("Keyword not found!");
            return;
        }

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

            // Step 5: 从EDB中检索密文
            Object[] ciphertext = EDB.get(new String(I, StandardCharsets.UTF_8));

            // Step 6: 将检索到的密文存储在E中 (只存储ea和eb)
            BigInteger ea = (BigInteger) ciphertext[1];
            BigInteger eb = (BigInteger) ciphertext[2];
            E.put(i - c0, new BigInteger[]{ea, eb});

            // Step 7: 从EDB中移除该密文
            EDB.remove(new String(I, StandardCharsets.UTF_8));

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
            BigInteger[] encryptedBiBitmap = E.get(i - c0);
            BigInteger ea = encryptedBiBitmap[0];
            BigInteger eb = encryptedBiBitmap[1];

            // 解密 bi-bitmap-index
            byte[] H3 = hashFunction(Kw_prime, i);
            byte[] H4 = hashFunction(Kw_prime, i);
            BigInteger bsa = new BigInteger(1, xorBytes(ea.toByteArray(), H3));
            BigInteger bsb = new BigInteger(1, xorBytes(eb.toByteArray(), H4));

            // 更新bsw
            bsw = bsw.and(bsa).xor(bsa.and(bsb));
        }

        // Step 10: 更新客户端状态
        int Rc_plus_1 = getRandomFromPool();
        SC.put("keyword", new int[]{c + 1, c, Rc_plus_1});
        // Step 11: 重新加密 bsw
        byte[] H5 = hashFunction(Kw_prime, c + 1);
        ew = bsw.xor(new BigInteger(1, H5));

        // 客户端接收部分结束计时
        long client_time4 = System.nanoTime();
        // Step 12: 解析 bsw 并打印出哪些位为 1
//        System.out.println("Matching files found at indices:");
//        for (int bitIndex = 0; bitIndex < MAX_FILES; bitIndex++) {
//            if (bsw.testBit(bitIndex)) {
//                System.out.println("File index: " + bitIndex);
//            }
//        }


        // Step 13: 将新的加密状态 ew 发送到服务器
        EDB.put(new String(Kw, StandardCharsets.UTF_8), new Object[]{ew});

        // 服务器更新 SS
        SS.put(new String(Kw, StandardCharsets.UTF_8), ew);
        // 输出客户端和服务器端的时间消耗
        double msclient_time1 = (client_time2 - client_time1) / 1_000_000.0;
        double msclient_time2 = (client_time4 - client_time3) / 1_000_000.0;
        double msserver_time = (server_time2 - server_time1) / 1_000_000.0;
        double total_time = msclient_time1 + msclient_time2 + msserver_time;
        System.out.println("Client time part 1: " + msclient_time1 + " ms, Client time part 2: " + msclient_time2 + " ms, Server time: " + msserver_time + " ms, Total time: " + total_time + " ms");

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
        SRDSSE srDsse = new SRDSSE();
        srDsse.setup(LAMBDA, MAX_FILES);

        // 示例文件的索引
        int[] fileIndexes = {0, 2, 1 << 19 + 1};  // 假设文件有索引0, 2, 1<<19 + 1

        // 执行添加操作
        srDsse.update("w1", "add", fileIndexes);
        srDsse.update("w2", "add", fileIndexes);
        srDsse.update("w3", "add", fileIndexes);
        srDsse.update("w1", "add", fileIndexes);
        // 执行删除操作
        srDsse.update("w1", "del", fileIndexes);


        // 执行搜索操作
        srDsse.search("w1");
    }
}
