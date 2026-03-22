// src/main/java/com/wei/service/CaptchaService.java
package com.wei.service;

import com.wei.common.utils.RedisKeyPrefixes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {
    
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 验证图片验证码
     * @param uuid 验证码UUID
     * @param code 用户输入的验证码
     * @return 是否验证通过
     */
    public boolean validateImageCaptcha(String uuid, String code) {
        if (uuid == null || code == null) {
            return false;
        }
        
        String key = RedisKeyPrefixes.PREFIX_CAPTCHA + uuid;
        String realCode = redisTemplate.opsForValue().get(key);
        
        if (realCode == null) {
            log.debug("验证码已失效，UUID：{}", uuid);
            return false;
        }
        
        boolean isValid = code.equalsIgnoreCase(realCode);
        
        // 验证码使用后从Redis中删除
        if (isValid) {
            redisTemplate.delete(key);
            log.debug("验证码验证成功，已删除验证码，UUID：{}", uuid);
        } else {
            log.debug("验证码验证失败，正确验证码：{}，输入验证码：{}，UUID：{}", realCode, code, uuid);
        }
        
        return isValid;
    }
    
    /**
     * 验证邮箱验证码
     * @param email 用户邮箱
     * @param code 用户输入的验证码
     * @return 是否验证通过
     */
    public boolean validateEmailCaptcha(String email, String code) {
        if (email == null || code == null) {
            return false;
        }
        
        String key = "VERIFICATION_CODE:" + email;
        String realCode = redisTemplate.opsForValue().get(key);
        
        if (realCode == null) {
            log.debug("邮箱验证码已失效，邮箱：{}", email);
            return false;
        }
        
        boolean isValid = code.equals(realCode);
        
        // 验证码使用后从Redis中删除
        if (isValid) {
            redisTemplate.delete(key);
            log.debug("邮箱验证码验证成功，已删除验证码，邮箱：{}", email);
        } else {
            log.debug("邮箱验证码验证失败，正确验证码：{}，输入验证码：{}，邮箱：{}", realCode, code, email);
        }
        
        return isValid;
    }
}