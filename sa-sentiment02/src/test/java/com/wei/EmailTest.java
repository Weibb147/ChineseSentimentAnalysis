package com.wei;

import com.wei.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 邮件服务测试类
 * 测试邮件发送和验证码功能
 */
@SpringBootTest
@ActiveProfiles("test")
public class EmailTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 测试邮件验证码发送功能
     */
    @Test
    public void testSendVerificationCode() {
        // 准备测试数据
        String testEmail = "2806667556@qq.com";
        String code = "123456";

        try {
            // 执行发送验证码操作
            emailService.sendVerificationCode(testEmail, code);

            // 验证邮件发送成功（此处仅能验证方法执行无异常）
            assertTrue(true, "邮件发送方法执行成功");
        } catch (Exception e) {
            fail("邮件发送失败: " + e.getMessage());
        }
    }

    /**
     * 测试验证码存储到Redis功能
     */
    @Test
    public void testStoreVerificationCodeInRedis() {
        // 准备测试数据
        String testEmail = "test@example.com";
        String code = "654321";
        String redisKey = "VERIFICATION_CODE:" + testEmail;

        try {
            // 存储验证码到Redis
            stringRedisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);

            // 从Redis中获取验证码
            String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

            // 验证验证码存储正确
            assertNotNull(storedCode, "验证码应存储在Redis中");
            assertEquals(code, storedCode, "存储的验证码应与原始验证码一致");

            // 清理测试数据
            stringRedisTemplate.delete(redisKey);
        } catch (Exception e) {
            fail("Redis操作失败: " + e.getMessage());
        }
    }

    /**
     * 测试验证码生成和格式
     */
    @Test
    public void testVerificationCodeGeneration() {
        // 生成多个验证码进行测试
        for (int i = 0; i < 10; i++) {
            String code = String.format("%06d", (int) (Math.random() * 1000000));

            // 验证验证码长度
            assertEquals(6, code.length(), "验证码应为6位数字");

            // 验证验证码为数字
            assertTrue(code.matches("\\d{6}"), "验证码应只包含数字");
        }
    }

    /**
     * 测试验证码过期时间
     */
    @Test
    public void testVerificationCodeExpiration() throws InterruptedException {
        // 准备测试数据
        String testEmail = "test@example.com";
        String code = "111111";
        String redisKey = "VERIFICATION_CODE:" + testEmail;

        try {
            // 存储验证码到Redis，设置1秒过期时间
            stringRedisTemplate.opsForValue().set(redisKey, code, 1, TimeUnit.SECONDS);

            // 验证验证码存在
            String storedCode = stringRedisTemplate.opsForValue().get(redisKey);
            assertNotNull(storedCode, "验证码应存在");

            // 等待2秒
            Thread.sleep(2000);

            // 验证验证码已过期
            String expiredCode = stringRedisTemplate.opsForValue().get(redisKey);
            assertNull(expiredCode, "验证码应已过期并被删除");
        } catch (Exception e) {
            fail("验证码过期测试失败: " + e.getMessage());
        }
    }
}
