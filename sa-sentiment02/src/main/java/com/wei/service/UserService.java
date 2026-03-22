// src/main/java/com/wei/service/UserService.java
package com.wei.service;

import com.wei.pojo.User;
import com.wei.pojo.dto.UserDTO;
import com.wei.pojo.vo.UserVO;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {
    /**
     * 用户登录
     * @param userDTO 用户登录信息
     * @return 登录结果，包含token等信息
     */
    Map<String, Object> login(UserDTO userDTO);

    /**
     * 用户注册（带验证码）
     * @param userDTO 用户注册信息(     * @param email 用户邮箱
     *      * @param code 验证码)
     */
    void register(UserDTO userDTO);

    /**
     * 更新个人信息（用户自己操作）
     * @param partialUser 部分用户信息
     */
    void updateProfile(User partialUser);

    /**
     * 更新密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息VO
     */
    UserVO getProfile(Long userId);
}