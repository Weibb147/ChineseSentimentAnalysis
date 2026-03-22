package com.wei.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wei.pojo.User;
import com.wei.pojo.dto.UserDTO;

public interface AdminUserService {
    /**
     * 分页查询用户列表
     * @param page 页码
     * @param size 每页大小
     * @param keyword 搜索关键词(用户名或邮箱)
     * @return 用户分页数据
     */
    IPage<User> listUsers(int page, int size, String keyword);

    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户信息
     */
    User getUserById(Long id);

    /**
     * 创建用户
     * @param user 用户信息
     */
    void createUser(User user);

    /**
     * 更新用户
     * @param user 用户信息
     */
    void updateUser(User user);

    /**
     * 删除用户
     * @param id 用户ID
     */
    void deleteUser(Long id);
}
