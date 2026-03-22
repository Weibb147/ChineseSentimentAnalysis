// src/api/admin.js
import request from '@/utils/request.js'

// 管理员用户管理接口

/**
 * 获取用户列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页大小
 * @param {string} params.keyword - 搜索关键词
 * @returns {Promise} 用户分页列表
 */
export const getUserListAPI = (params) => {
    return request.get('admin/user/list', { params })
}

/**
 * 获取用户详情
 * @param {number} id - 用户ID
 * @returns {Promise} 用户信息
 */
export const getUserAPI = (id) => {
    return request.get(`/admin/user/${id}`)
}

/**
 * 创建用户
 * @param {Object} userData - 用户信息
 * @returns {Promise}
 */
export const createUserAPI = (userData) => {
    return request.post('/admin/user', userData)
}

/**
 * 更新用户信息
 * @param {number} id - 用户ID
 * @param {Object} userData - 用户信息
 * @returns {Promise}
 */
export const updateUserAPI = (id, userData) => {
    return request.put(`/admin/user/${id}`, userData)
}

/**
 * 删除用户
 * @param {number} id - 用户ID
 * @returns {Promise}
 */
export const deleteUserAPI = (id) => {
    return request.delete(`/admin/user/${id}`)
}

/**
 * 切换用户状态
 * @param {number} id - 用户ID
 * @param {Object} statusData - 状态数据
 * @returns {Promise}
 */
export const toggleUserStatusAPI = (id, statusData) => {
    return request.put(`/admin/user/${id}`, statusData)
}