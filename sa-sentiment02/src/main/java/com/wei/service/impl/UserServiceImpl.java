// src/main/java/com/wei/service/impl/UserServiceImpl.java
package com.wei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wei.common.utils.RedisKeyPrefixes;
import com.wei.common.utils.ResultCode;
import com.wei.common.exception.BusinessException;
import com.wei.common.utils.JwtUtil;
import com.wei.pojo.User;
import com.wei.pojo.dto.UserDTO;
import com.wei.pojo.enums.UserRole;
import com.wei.pojo.enums.UserStatus;
import com.wei.pojo.vo.UserVO;
import com.wei.mapper.UserMapper;
import com.wei.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Map<String, Object> login(UserDTO userDTO) {
        // 参数校验
        if (userDTO == null) {
            throw new BusinessException(ResultCode.FAIL, "登录信息不能为空");
        }

        if (!StringUtils.hasText(userDTO.getUsername())) {
            throw new BusinessException(ResultCode.FAIL, "用户名不能为空");
        }

        if (!StringUtils.hasText(userDTO.getPassword())) {
            throw new BusinessException(ResultCode.FAIL, "密码不能为空");
        }

        if (!StringUtils.hasText(userDTO.getUuid())) {
            throw new BusinessException(ResultCode.FAIL, "验证码UUID不能为空");
        }

        if (!StringUtils.hasText(userDTO.getCode())) {
            throw new BusinessException(ResultCode.FAIL, "验证码不能为空");
        }


        // 验证验证码
        if (!validateCaptcha(userDTO.getUuid(), userDTO.getCode())) {
            throw new BusinessException(ResultCode.VERIFICATION_CODE_ERROR, "验证码错误或已过期");
        }

        // 验证用户名和密码
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, userDTO.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST, "用户不存在");
        }

        if (!passwordEncoder.matches(userDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "密码错误");
        }
        //查看 用户状态，如果是禁用状态，抛出异常
        if (!user.isEnabled()) {
            throw new BusinessException(ResultCode.USER_DISABLED, "用户已被禁用");
        }

        // 生成 JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().getCode());
        String token = JwtUtil.genToken(claims);

        // 将 token 存入 Redis，设置24小时过期
        stringRedisTemplate.opsForValue().set(RedisKeyPrefixes.PREFIX_TOKEN + token,
                String.valueOf(user.getId()), 24, TimeUnit.HOURS);

        // 记录登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole().getCode());

        return result;
    }

    /**
     * 验证码校验方法
     * @param uuid 验证码唯一标识
     * @param code 用户输入的验证码
     * @return 是否验证通过
     */
    private boolean validateCaptcha(String uuid, String code) {
        String key = RedisKeyPrefixes.PREFIX_CAPTCHA + uuid;
        String realCode = stringRedisTemplate.opsForValue().get(key);

        if (realCode == null) {
            return false;
        }

        boolean isValid = code.equalsIgnoreCase(realCode);
        
        // 验证码使用后从Redis中删除
        if (isValid) {
            stringRedisTemplate.delete(key);
        }
        
        return isValid;
    }

    // 在 UserServiceImpl.java 中的 register 方法应该类似这样：
    @Override
    public void register(UserDTO userDTO) {
        // 参数校验
        if (userDTO == null) {
            throw new BusinessException(ResultCode.FAIL, "注册信息不能为空");
        }

        if (!StringUtils.hasText(userDTO.getUsername())) {
            throw new BusinessException(ResultCode.FAIL, "用户名不能为空");
        }

        if (!StringUtils.hasText(userDTO.getPassword())) {
            throw new BusinessException(ResultCode.FAIL, "密码不能为空");
        }

        if (!StringUtils.hasText(userDTO.getEmail())) {
            throw new BusinessException(ResultCode.FAIL, "邮箱不能为空");
        }

        if (!StringUtils.hasText(userDTO.getCode())) {
            throw new BusinessException(ResultCode.FAIL, "验证码不能为空");
        }

        try {
            // 验证验证码
            String redisKey = RedisKeyPrefixes.PREFIX_EMAIL_CODE + userDTO.getEmail();
            String storedCode = stringRedisTemplate.opsForValue().get(redisKey);
            if (storedCode == null || !storedCode.equals(userDTO.getCode())) {
                throw new BusinessException(ResultCode.VERIFICATION_CODE_ERROR, "验证码错误或已过期");
            }

            // 验证成功后移除验证码
            stringRedisTemplate.delete(redisKey);

            // 检查用户名是否已存在
            User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, userDTO.getUsername()));
            if (existingUser != null) {
                throw new BusinessException(ResultCode.USER_ALREADY_EXIST, "用户名已存在");
            }

            // 检查邮箱是否已存在
            existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, userDTO.getEmail()));
            if (existingUser != null) {
                throw new BusinessException(ResultCode.EMAIL_ALREADY_REGISTERED, "邮箱已被注册");
            }

            // 创建新用户
            User user = new User();
            user.setUsername(userDTO.getUsername());
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            user.setEmail(userDTO.getEmail());
            user.setRole(UserRole.USER);
            user.setStatus(UserStatus.ENABLED);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            userMapper.insert(user);
        } catch (BusinessException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("注册失败", e);
            throw new BusinessException(ResultCode.FAIL, "注册失败，请稍后重试");
        }
    }


    @Override
    public void updateProfile(User partialUser) {
        if (partialUser.getId() == null) {
            throw new BusinessException(ResultCode.FAIL, "用户ID不能为空");
        }

        // 确保用户只能更新自己的信息
        User currentUser = userMapper.selectById(partialUser.getId());
        if (currentUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST, "用户不存在");
        }

        // 防止更新敏感字段
        partialUser.setUsername(null); // 不允许通过此接口修改用户名
        partialUser.setRole(null);     // 不允许通过此接口修改角色
        partialUser.setStatus(null);   // 不允许通过此接口修改状态

        partialUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(partialUser);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) {
            throw new BusinessException(ResultCode.FAIL, "用户ID不能为空");
        }

        if (!StringUtils.hasText(oldPassword)) {
            throw new BusinessException(ResultCode.FAIL, "原密码不能为空");
        }

        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException(ResultCode.FAIL, "新密码不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST, "用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR, "原密码错误");
        }

        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(ResultCode.FAIL, "新密码不能与原密码相同");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public UserVO getProfile(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.FAIL, "用户ID不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST, "用户不存在");
        }

        // 转换成 VO
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setImageUrl(user.getImageUrl());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole() != null ? user.getRole().getCode() : null);
        vo.setSex(user.getSex() != null ? user.getSex().getCode() : null);
        vo.setStatus(user.getStatus() != null ? user.getStatus().getCode() : null);
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());

        return vo;
    }
}