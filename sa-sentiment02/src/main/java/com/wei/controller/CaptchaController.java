// src/main/java/com/wei/controller/CaptchaController.java
package com.wei.controller;

import com.wei.common.utils.RedisKeyPrefixes;
import com.wei.common.yanzhengma.CaptchaImageProperties;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 验证码控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private final StringRedisTemplate redisTemplate;
    private final CaptchaImageProperties captchaImageProperties;

    @Autowired
    public CaptchaController(StringRedisTemplate redisTemplate, CaptchaImageProperties captchaImageProperties) {
        this.redisTemplate = redisTemplate;
        this.captchaImageProperties = captchaImageProperties;
    }

    /**
     * 生成验证码
     * @param uuid 前端生成的uuid
     * @param response HttpServletResponse对象
     */
    @GetMapping("/generate")
    public void generate(@RequestParam String uuid, HttpServletResponse response) throws IOException {
        // 验证uuid格式
        if (!StringUtils.hasText(uuid)) {
            log.warn("验证码生成失败：UUID不能为空");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "UUID不能为空");
            return;
        }

        try {
            Integer width = captchaImageProperties.getWidth() != null ? captchaImageProperties.getWidth() : 120;
            Integer height = captchaImageProperties.getHeight() != null ? captchaImageProperties.getHeight() : 40;
            Integer length = captchaImageProperties.getLength() != null ? captchaImageProperties.getLength() : 4;

            SpecCaptcha captcha = new SpecCaptcha(width, height);
            captcha.setLen(length);
            captcha.setCharType(Captcha.TYPE_DEFAULT);

            String code = captcha.text();
            log.debug("生成验证码：{}，UUID：{}", code, uuid);

            System.out.println("生成验证码：" + code);
            // 根据uuid拼接前缀得到验证码的key
            String key = RedisKeyPrefixes.PREFIX_CAPTCHA + uuid;

            // 缓存验证码
            redisTemplate.opsForValue().set(key, code);
            
            // 设置验证码5分钟后过期
            redisTemplate.expire(key, 5, TimeUnit.MINUTES);

            // 设置响应头
            response.setContentType("image/png");
            response.setDateHeader("Expires", 0);
            response.setHeader("Pragma", "No-cache");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Access-Control-Allow-Origin", "*");

            // 输出图片流
            captcha.out(response.getOutputStream());

            log.info("验证码生成成功，UUID：{}", uuid);
        } catch (Exception e) {
            log.error("验证码生成异常：", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "验证码生成失败");
        }
    }
}