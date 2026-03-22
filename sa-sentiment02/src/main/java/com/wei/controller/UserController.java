package com.wei.controller;

import com.wei.common.exception.BusinessException;
import com.wei.common.utils.ResultCode;
import com.wei.common.utils.ResultUtils;
import com.wei.common.utils.AuthContext;
import com.wei.pojo.User;
import com.wei.pojo.dto.UserDTO;
import com.wei.pojo.enums.Gender;
import com.wei.pojo.vo.UserVO;
import com.wei.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户信息管理接口
 * 提供用户个人信息管理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 更新个人信息（用户自己操作）
     * @param userDTO 部分用户信息DTO
     * @return 更新结果
     */
    @PutMapping("/profile")
    public ResultUtils<Void> updateProfile(@RequestBody UserDTO userDTO) {
        // 从认证上下文中获取当前用户ID
        Long currentUserId = AuthContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResultUtils.error("用户未登录");
        }

        try {
            // 将DTO转换为Entity
            User partialUser = new User();
            partialUser.setId(currentUserId);
            // 只复制允许用户修改的字段
            if (StringUtils.hasText(userDTO.getNickname())) {
                partialUser.setNickname(userDTO.getNickname());
            }
            if (StringUtils.hasText(userDTO.getEmail())) {
                partialUser.setEmail(userDTO.getEmail());
            }
            if (StringUtils.hasText(userDTO.getPhone())) {
                partialUser.setPhone(userDTO.getPhone());
            }
            if (StringUtils.hasText(userDTO.getSex())) {
                partialUser.setSex(Gender.fromCode(userDTO.getSex()));
            }
            if (StringUtils.hasText(userDTO.getImageUrl())) {
                partialUser.setImageUrl(userDTO.getImageUrl());
            }
            
            userService.updateProfile(partialUser);
            return ResultUtils.success();
        } catch (BusinessException e) {
            log.error("更新个人信息失败: {}", e.getMessage());
            return ResultUtils.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新个人信息失败", e);
            return ResultUtils.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码
     * @param body 密码信息 {"oldPassword": "...", "newPassword": "..."}
     * @return 修改结果
     */
    @PutMapping("/password")
    public ResultUtils<Void> updatePassword(@RequestBody Map<String, String> body) {
        Long currentUserId = AuthContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResultUtils.error("用户未登录");
        }

        try {
            String oldPwd = body.get("oldPassword");
            String newPwd = body.get("newPassword");

            if (!StringUtils.hasText(oldPwd)) {
                return ResultUtils.error("原密码不能为空");
            }

            if (!StringUtils.hasText(newPwd)) {
                return ResultUtils.error("新密码不能为空");
            }

            if (newPwd.length() < 6 || newPwd.length() > 20) {
                return ResultUtils.error("密码长度应在6-20个字符之间");
            }

            userService.updatePassword(currentUserId, oldPwd, newPwd);
            return ResultUtils.success();
        } catch (BusinessException e) {
            log.error("修改密码失败: {}", e.getMessage());
            return ResultUtils.error(e.getMessage());
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return ResultUtils.error("修改密码失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     * @return 用户信息VO
     */
    @GetMapping("/profile")
    public ResultUtils<UserVO> getProfile() {
        Long currentUserId = AuthContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResultUtils.error("用户未登录");
        }

        try {
            UserVO userVO = userService.getProfile(currentUserId);
            return ResultUtils.success(userVO);
        } catch (BusinessException e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return ResultUtils.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ResultUtils.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户头像
     * @param body 头像URL {"imageUrl": "..."}
     * @return 更新结果
     */
    @PutMapping("/avatar")
    public ResultUtils<UserVO> updateAvatar(@RequestBody Map<String, String> body) {
        Long currentUserId = AuthContext.getCurrentUserId();
        if (currentUserId == null) {
            return ResultUtils.error("用户未登录");
        }

        try {
            String imageUrl = body.get("imageUrl");
            if (!StringUtils.hasText(imageUrl)) {
                return ResultUtils.error("头像URL不能为空");
            }

            // 验证URL格式
            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                return ResultUtils.error("无效的头像URL");
            }

            User partialUser = new User();
            partialUser.setId(currentUserId);
            partialUser.setImageUrl(imageUrl);
            userService.updateProfile(partialUser);
            
            // 返回更新后的用户信息
            UserVO userVO = userService.getProfile(currentUserId);
            return ResultUtils.success(userVO);
        } catch (BusinessException e) {
            log.error("更新头像失败: {}", e.getMessage());
            return ResultUtils.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新头像失败", e);
            return ResultUtils.error("更新头像失败: " + e.getMessage());
        }
    }
}
