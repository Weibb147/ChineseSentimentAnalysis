// src/api/feedback.js
import request from '@/utils/request.js'

/**
 * 提交反馈
 * @param {Object} data - 反馈数据
 * @param {string} data.category - 反馈分类
 * @param {string} data.content - 反馈内容
 * @returns {Promise}
 */
export const submitFeedback = (data) => {
    return request.post('/feedback/submit', data)
}

/**
 * 查询我的反馈列表
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页大小
 * @returns {Promise} 反馈分页列表
 */
export const getMyFeedbacks = (params) => {
    return request.get('/feedback/my', { params })
}

/**
 * 查询所有反馈（管理员）
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页大小
 * @param {string} params.status - 反馈状态（可选）
 * @returns {Promise} 反馈分页列表
 */
export const getAllFeedbacks = (params) => {
    return request.get('/feedback/all', { params })
}

/**
 * 回复反馈（管理员）
 * @param {number} id - 反馈ID
 * @param {string} reply - 回复内容
 * @returns {Promise}
 */
export const replyFeedback = (id, reply) => {
    return request.put(`/feedback/${id}/reply`, reply, {
        headers: {
            'Content-Type': 'text/plain'
        }
    })
}

/**
 * 更新反馈（用户）
 * @param {number} id - 反馈ID
 * @param {Object} data - 反馈数据
 * @param {string} data.category - 反馈分类
 * @param {string} data.content - 反馈内容
 * @returns {Promise}
 */
export const updateFeedback = (id, data) => {
    return request.put(`/feedback/${id}`, data)
}

/**
 * 删除反馈（用户/管理员）
 * @param {number} id - 反馈ID
 * @returns {Promise}
 */
export const deleteFeedback = (id) => {
    return request.delete(`/feedback/${id}`)
}
