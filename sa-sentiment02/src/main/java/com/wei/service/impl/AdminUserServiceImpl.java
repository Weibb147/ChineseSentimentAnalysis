package com.wei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wei.common.utils.ResultCode;
import com.wei.common.exception.BusinessException;
import com.wei.pojo.User;
import com.wei.pojo.enums.UserRole;
import com.wei.pojo.enums.UserStatus;
import com.wei.mapper.UserMapper;
import com.wei.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 分页查询用户列表
    @Override
    public IPage<User> listUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(User::getUsername, keyword)
                    .or().like(User::getEmail, keyword);
        }
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    // 根据ID查询用户
    @Override
    public User getUserById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.FAIL, "用户ID不能为空");
        }
        return userMapper.selectById(id);
    }

    // 创建用户
    @Override
    public void createUser(User user) {
        // 参数校验
        validateUser(user);

        // 检查用户名是否重复
        long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXIST, "用户名已存在");
        }

        // 检查邮箱是否重复
        count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, user.getEmail()));
        if (count > 0) {
            throw new BusinessException(ResultCode.EMAIL_ALREADY_REGISTERED, "邮箱已被注册");
        }

        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(user.getRole() != null ? user.getRole() : UserRole.USER); // 默认角色为USER
        user.setStatus(user.getStatus() != null ? user.getStatus() : UserStatus.ENABLED); // 默认状态为启用
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
    }

    // 更新用户
    @Override
    public void updateUser(User partialUser) {
        if (partialUser.getId() == null) {
            throw new BusinessException(ResultCode.FAIL, "用户ID不能为空");
        }
        // 如果更新了密码，则加密
        if (partialUser.getPassword() != null && !partialUser.getPassword().isEmpty()) {
            partialUser.setPassword(passwordEncoder.encode(partialUser.getPassword()));
        }
        User user = userMapper.selectById(partialUser.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST, "用户不存在");
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(partialUser);
    }

    // 删除用户
    @Override
    public void deleteUser(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.FAIL, "用户ID不能为空");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST, "用户不存在");
        }
        userMapper.deleteById(id);
    }

    /**
     * 验证用户参数
     * @param user 用户对象
     */
    private void validateUser(User user) {
        if (user == null) {
            throw new BusinessException(ResultCode.FAIL, "用户信息不能为空");
        }

        if (!StringUtils.hasText(user.getUsername())) {
            throw new BusinessException(ResultCode.FAIL, "用户名不能为空");
        }

        if (!StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(ResultCode.FAIL, "密码不能为空");
        }

        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException(ResultCode.FAIL, "邮箱不能为空");
        }
    }
}
