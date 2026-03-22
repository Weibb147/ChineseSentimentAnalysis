package com.wei.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wei.common.utils.ResultUtils;
import com.wei.pojo.User;
import com.wei.pojo.dto.UserDTO;
import com.wei.pojo.enums.Gender;
import com.wei.pojo.enums.UserRole;
import com.wei.pojo.enums.UserStatus;
import com.wei.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员用户管理接口
 * 提供对所有用户的增删改查功能
 * @author admin
 */
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 分页查询用户列表
     * @param page 页码（默认1）
     * @param size 每页大小（默认10）
     * @param keyword 搜索关键词（可选）
     * @return 用户分页列表
     */
    @GetMapping("/list")
    public ResultUtils<IPage<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        IPage<User> pager = adminUserService.listUsers(page, size, keyword);
        return ResultUtils.success(pager);
    }

    /**
     * 获取用户详情
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public ResultUtils<User> getUser(@PathVariable Long id) {
        User user = adminUserService.getUserById(id);
        return ResultUtils.success(user);
    }

    /**
     * 创建用户
     * @param userDTO 用户信息DTO
     * @return 创建结果
     */
    @PostMapping
    public ResultUtils<Void> createUser(@RequestBody UserDTO userDTO) {
        // 将 UserDTO 转换为 User 实体
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        
        // 转换枚举类型
        if (userDTO.getSex() != null && !userDTO.getSex().isEmpty()) {
            user.setSex(Gender.fromCode(userDTO.getSex()));
        }
        if (userDTO.getRole() != null && !userDTO.getRole().isEmpty()) {
            user.setRole(UserRole.fromCode(userDTO.getRole()));
        }
        if (userDTO.getStatus() != null) {
            user.setStatus(UserStatus.fromCode(userDTO.getStatus()));
        }
        
        adminUserService.createUser(user);
        return ResultUtils.success();
    }

    /**
     * 更新用户信息
     * @param id 用户ID
     * @param userDTO 部分用户信息DTO
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResultUtils<Void> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        // 将 UserDTO 转换为 User 实体
        User partialUser = new User();
        BeanUtils.copyProperties(userDTO, partialUser);
        partialUser.setId(id);
        
        // 转换枚举类型
        if (userDTO.getSex() != null && !userDTO.getSex().isEmpty()) {
            partialUser.setSex(Gender.fromCode(userDTO.getSex()));
        }
        if (userDTO.getRole() != null && !userDTO.getRole().isEmpty()) {
            partialUser.setRole(UserRole.fromCode(userDTO.getRole()));
        }
        if (userDTO.getStatus() != null) {
            partialUser.setStatus(UserStatus.fromCode(userDTO.getStatus()));
        }
        
        adminUserService.updateUser(partialUser);
        return ResultUtils.success();
    }

    /**
     * 删除用户
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResultUtils<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResultUtils.success();
    }
}