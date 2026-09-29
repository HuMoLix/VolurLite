package com.humolix.volur.verification.crypto;

import com.humolix.volur.framework.common.thread.VolurThreadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Volur 平台验证码专用魔改 RC4 编码器 (Rc4CaptchaEncoder)
 * <p>
 * 参考 {@link com.humolix.volur.security.crypto.Sm3PasswordEncoder} 编码器规范设计，
 * 封装 Volur 平台针对人机验证码场景深度定制的魔改 RC4 流对称加密算法：
 * 1. S-Box 异或掩码混淆初始化：S[k] = (k ^ 0x5A) & 0xFF；
 * 2. KSA 增加额外置换轮次（2 轮）及附加常数偏移（0x3C）；
 * 3. PRGA 丢弃前 256 字节弱输出流以消除初始密钥偏差，并在输出流生成时加入次级位移与动态掩码；
 * 4. 加密与解密具有对称性（encode 与 decode 均通过流密码执行 process 逆向还原）；
 * 5. 支持接入 {@link VolurThreadManager} 实现高并发异步计算调度，与无容器环境本地同步降级。
 */
@Component
@RequiredArgsConstructor
public class Rc4CaptchaEncoder {

    private final VolurThreadManager threadManager;

    /**
     * 无参构造函数：支持轻量离线回退及单元测试脱离容器环境直接运行
     */
    public Rc4CaptchaEncoder() {
        this.threadManager = null;
    }

    /**
     * 编码（加密）数据字节数组
     *
     * @param data 原始明文字节数组
     * @param key  会话对称密钥字节数组
     * @return 加密后的密文字节数组
     */
    public byte[] encode(byte[] data, byte[] key) {
        return process(data, key);
    }

    /**
     * 解码（解密）数据字节数组
     *
     * @param data 密文字节数组
     * @param key  会话对称密钥字节数组
     * @return 解密后的明文字节数组
     */
    public byte[] decode(byte[] data, byte[] key) {
        return process(data, key);
    }

    /**
     * 编码（加密）字符串为密文字节数组
     *
     * @param text 原始明文字符串
     * @param key  会话对称密钥字节数组
     * @return 加密后的密文字节数组
     */
    public byte[] encode(String text, byte[] key) {
        if (text == null) {
            return new byte[0];
        }
        return encode(text.getBytes(StandardCharsets.UTF_8), key);
    }

    /**
     * 解码（解密）密文字节数组为 UTF-8 明文字符串
     *
     * @param data 密文字节数组
     * @param key  会话对称密钥字节数组
     * @return 解密还原的 UTF-8 明文字符串
     */
    public String decodeToString(byte[] data, byte[] key) {
        byte[] plainBytes = decode(data, key);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    /**
     * 对称流数据处理（支持线程池卸载调度）
     *
     * @param data 输入数据字节数组
     * @param key  会话对称密钥
     * @return 转换后的字节数组
     */
    public byte[] process(byte[] data, byte[] key) {
        if (data == null) {
            return new byte[0];
        }
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("RC4 key cannot be empty");
        }
        if (threadManager != null) {
            return threadManager.supplyCompute(() -> processInternal(data, key)).join();
        }
        return processInternal(data, key);
    }

    /**
     * 异步编码（加密）计算管道
     *
     * @param data 原始明文字节数组
     * @param key  会话对称密钥
     * @return 异步执行结果 CompletableFuture
     */
    public CompletableFuture<byte[]> encodeAsync(byte[] data, byte[] key) {
        if (data == null) {
            return CompletableFuture.completedFuture(new byte[0]);
        }
        if (key == null || key.length == 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("RC4 key cannot be empty"));
        }
        if (threadManager != null) {
            return threadManager.supplyCompute(() -> processInternal(data, key));
        }
        return CompletableFuture.supplyAsync(() -> processInternal(data, key));
    }

    /**
     * 异步解码（解密）计算管道
     *
     * @param data 密文字节数组
     * @param key  会话对称密钥
     * @return 异步执行结果 CompletableFuture
     */
    public CompletableFuture<byte[]> decodeAsync(byte[] data, byte[] key) {
        return encodeAsync(data, key);
    }

    /**
     * 异步解码（解密）为明文字符串管道
     *
     * @param data 密文字节数组
     * @param key  会话对称密钥
     * @return 异步执行结果 CompletableFuture
     */
    public CompletableFuture<String> decodeToStringAsync(byte[] data, byte[] key) {
        return decodeAsync(data, key)
                .thenApply(plainBytes -> new String(plainBytes, StandardCharsets.UTF_8));
    }

    /**
     * 静态直接处理方法，便于轻量或无依赖上下文直接调用
     *
     * @param data 数据
     * @param key  密钥
     * @return 处理结果
     */
    public static byte[] processBytes(byte[] data, byte[] key) {
        return processInternal(data, key);
    }

    /**
     * 创建底层的魔改 RC4 流密码状态机实例
     *
     * @param key 密钥字节数组
     * @return 状态机实例
     */
    public static Rc4CipherState createCipher(byte[] key) {
        return new Rc4CipherState(key);
    }

    private static byte[] processInternal(byte[] data, byte[] key) {
        if (data == null) {
            return new byte[0];
        }
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("RC4 key cannot be empty");
        }
        Rc4CipherState cipher = new Rc4CipherState(key);
        return cipher.process(data);
    }

    /**
     * 魔改 RC4 流加密内部状态机
     */
    public static class Rc4CipherState {
        private final int[] s = new int[256];
        private int i = 0;
        private int j = 0;

        public Rc4CipherState(byte[] key) {
            if (key == null || key.length == 0) {
                throw new IllegalArgumentException("RC4 key cannot be empty");
            }

            // 1. 异或掩码混淆初始化
            for (int k = 0; k < 256; k++) {
                s[k] = (k ^ 0x5A) & 0xFF;
            }

            // 2. KSA 增加到 2 轮变换并引入 0x3C 常数
            int curJ = 0;
            for (int round = 0; round < 2; round++) {
                for (int k = 0; k < 256; k++) {
                    int keyByte = key[k % key.length] & 0xFF;
                    curJ = (curJ + s[k] + keyByte + 0x3C) & 0xFF;
                    int tmp = s[k];
                    s[k] = s[curJ];
                    s[curJ] = tmp;
                }
            }

            // 3. 丢弃前 256 字节伪随机输出流以消除初始弱密钥偏差
            for (int k = 0; k < 256; k++) {
                nextByte();
            }
        }

        private int nextByte() {
            i = (i + 1) & 0xFF;
            j = (j + s[i] + 0x07) & 0xFF;
            int tmp = s[i];
            s[i] = s[j];
            s[j] = tmp;
            return s[(s[i] + s[j] + 0xA5) & 0xFF];
        }

        public byte[] process(byte[] data) {
            if (data == null) {
                return new byte[0];
            }
            byte[] out = new byte[data.length];
            for (int k = 0; k < data.length; k++) {
                out[k] = (byte) ((data[k] & 0xFF) ^ nextByte());
            }
            return out;
        }
    }

}
