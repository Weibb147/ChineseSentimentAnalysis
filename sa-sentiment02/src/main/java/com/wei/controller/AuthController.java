// src/main/java/com/wei/controller/AuthController.java
package com.wei.controller;

import com.wei.common.utils.RedisKeyPrefixes;
import com.wei.common.utils.ResultUtils;
import com.wei.pojo.dto.UserDTO;
import com.wei.service.EmailService;
import com.wei.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 用户登录
     * @param userDTO 用户登录信息
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResultUtils<Map<String, Object>> login(@RequestBody UserDTO userDTO) {
        try {
            Map<String, Object> result = userService.login(userDTO);
            // 登录token存储在Redis中，键名为TOKEN:{token}
            System.out.println("登录token: " + result.get("token"));
            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("登录失败：", e);
            return ResultUtils.error(e.getMessage());
        }
    }

    /**
     * 发送邮箱验证码
     * @param email 邮箱地址
     * @return 发送结果
     */
    @PostMapping("/send-code")
    public ResultUtils<Void> sendVerificationCode(@RequestParam String email) {
        try {
            if (email == null || email.isEmpty()) {
                return ResultUtils.error("邮箱地址不能为空");
            }

            // 生成6位验证码
            String code = String.format("%06d", (int) (Math.random() * 1000000));

            // 发送邮件
            emailService.sendVerificationCode(email, code);

            // 将验证码存储到Redis，设置5分钟过期时间
            // 使用与UserService中验证时相同的键名
            stringRedisTemplate.opsForValue().set(RedisKeyPrefixes.PREFIX_EMAIL_CODE + email, code, 5, TimeUnit.MINUTES);

            log.info("验证码已发送至邮箱: {}, 验证码: {}", email, code);
            return ResultUtils.success();
        } catch (Exception e) {
            log.error("发送验证码失败：", e);
            return ResultUtils.error("验证码发送失败");
        }
    }

    /**
     * 用户注册（带验证码）
     * @param userDTO 用户注册信息（包含邮箱和验证码）
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResultUtils<Void> register(@RequestBody UserDTO userDTO) {
        try {
            userService.register(userDTO);
            return ResultUtils.success();
        } catch (Exception e) {
            log.error("注册失败：", e);
            return ResultUtils.error(e.getMessage());
        }
    }
}
