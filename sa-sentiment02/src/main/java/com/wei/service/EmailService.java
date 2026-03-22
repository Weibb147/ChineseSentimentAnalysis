// src/main/java/com/wei/service/EmailService.java
package com.wei.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * 发送验证码
     * @param to 收件人邮箱
     * @param code 验证码
     */
    public void sendVerificationCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("2932126249@qq.com"); // 设置发件人地址
            message.setTo(to);
            message.setSubject("您的验证码");
            message.setText("您的验证码是: " + code + "，5分钟内有效。");
            mailSender.send(message);
            log.info("验证码发送成功，邮箱：{}，验证码：{}", to, code);
        } catch (Exception e) {
            log.error("验证码发送失败，邮箱：{}，错误：", to, e);
            throw new RuntimeException("验证码发送失败", e);
        }
    }
}