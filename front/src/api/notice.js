// src/api/notice.js
import request from '@/utils/request.js'

/**
 * 获取公告列表
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页大小
 * @returns {Promise} 公告分页列表
 */
export const getNoticeList = (params) => {
    return request.get('/notice/list', { params })
}

/**
 * 创建公告（管理员）
 * @param {Object} data - 公告数据
 * @param {string} data.title - 公告标题
 * @param {string} data.content - 公告内容
 * @param {string} data.type - 公告类型
 * @returns {Promise}
 */
export const createNotice = (data) => {
    return request.post('/notice/create', data)
}

/**
 * 更新公告（管理员）
 * @param {number} id - 公告ID
 * @param {Object} data - 公告数据
 * @param {string} data.title - 公告标题
 * @param {string} data.content - 公告内容
 * @param {string} data.type - 公告类型
 * @returns {Promise}
 */
export const updateNotice = (id, data) => {
    return request.put(`/notice/${id}`, data)
}

/**
 * 删除公告（管理员）
 * @param {number} id - 公告ID
 * @returns {Promise}
 */
export const deleteNotice = (id) => {
    return request.delete(`/notice/${id}`)
}
