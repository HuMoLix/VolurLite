package com.humolix.volur.verification.crypto;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Volur 平台魔改 RC4 静态算法工具类 (Rc4Util)
 * <p>
 * 参考 {@link com.humolix.volur.security.crypto.Sm3Util} 工具类规范设计，
 * 提供纯函数式静态加密、解密及流转换工具方法，底层委托至 {@link Rc4CaptchaEncoder}。
 */
@Component
public class Rc4Util {

    /**
     * 对称流数据处理
     *
     * @param data 输入数据字节数组
     * @param key  会话密钥字节数组
     * @return 转换后的字节数组
     */
    public static byte[] process(byte[] data, byte[] key) {
        return Rc4CaptchaEncoder.processBytes(data, key);
    }

    /**
     * 加密数据字节流（对称加密）
     *
     * @param data 原始明文字节数组
     * @param key  会话密钥字节数组
     * @return 加密后的密文字节数组
     */
    public static byte[] encrypt(byte[] data, byte[] key) {
        return Rc4CaptchaEncoder.processBytes(data, key);
    }

    /**
     * 解密数据字节流（对称解密）
     *
     * @param data 密文字节数组
     * @param key  会话密钥字节数组
     * @return 解密还原的明文字节数组
     */
    public static byte[] decrypt(byte[] data, byte[] key) {
        return Rc4CaptchaEncoder.processBytes(data, key);
    }

    /**
     * 加密字符串为字节数组
     *
     * @param text 原始明文字符串
     * @param key  会话密钥字节数组
     * @return 加密后的密文字节数组
     */
    public static byte[] encrypt(String text, byte[] key) {
        if (text == null) {
            return new byte[0];
        }
        return encrypt(text.getBytes(StandardCharsets.UTF_8), key);
    }

    /**
     * 解密字节流为 UTF-8 字符串
     *
     * @param data 密文字节数组
     * @param key  会话密钥字节数组
     * @return 解密还原的明文字符串
     */
    public static String decryptToString(byte[] data, byte[] key) {
        byte[] plainBytes = decrypt(data, key);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

}
