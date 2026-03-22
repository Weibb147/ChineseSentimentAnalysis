// src/main/java/com/wei/common/CaptchaImageProperties.java
package com.wei.common.yanzhengma;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 验证码图片配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "captcha.image")
public class CaptchaImageProperties {

    /**
     * 验证码宽度
     */
    private Integer width = 120;

    /**
     * 验证码高度
     */
    private Integer height = 40;

    /**
     * 验证码的字符长度
     */
    private Integer length = 4;
}