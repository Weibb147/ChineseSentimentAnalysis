// src/main/java/com/wei/common/utils/RedisKeyPrefixes.java
package com.wei.common.utils;

/**
 * Redis键前缀常量
 */
public class RedisKeyPrefixes {
    /**
     * 验证码前缀
     */
    public static final String PREFIX_CAPTCHA = "CAPTCHA:";

    /**
     * Token前缀
     */
    public static final String PREFIX_TOKEN = "TOKEN:";

    /**
     * 邮箱验证码前缀
     */
    public static final String PREFIX_EMAIL_CODE = "VERIFICATION_CODE:";
}
