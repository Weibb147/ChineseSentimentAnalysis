// src/api/file.js
import request from '@/utils/request.js'

/**
 * 上传文件
 * @param {FormData} formData - 包含文件的表单数据
 * @returns {Promise} 上传结果
 */
export const uploadFile = (formData) => {
    return request.post('/file/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

/**
 * 上传用户头像（同时更新用户头像URL）
 * @param {FormData} formData - 包含 file 字段的表单数据
 * @returns {Promise<{data: {url: string}}>} 包含头像URL
 */
export const uploadAvatar = (formData) => {
    return request.post('/file/upload/avatar', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

/**
 * 获取文件列表
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页大小
 * @returns {Promise} 文件分页列表
 */
export const getFileList = (params) => {
    return request.get('/file/list', { params })
}

/**
 * 删除文件
 * @param {number} id - 文件ID
 * @returns {Promise}
 */
export const deleteFile = (id) => {
    return request.delete(`/file/${id}`)
}
