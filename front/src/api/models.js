// src/api/models.js
import request from '@/utils/request.js'

/**
 * 获取可用模型列表
 * @returns {Promise} 模型列表
 */
export const getModelList = () => {
    return request.get('/models/list')
}

/**
 * 获取模型详细信息
 * @param {string} modelName - 模型名称
 * @returns {Promise} 模型详细信息
 */
export const getModelInfo = (modelName) => {
    return request.get(`/models/info/${modelName}`)
}

/**
 * 创建模型（管理员）
 * @param {Object} data - 模型数据
 * @param {string} data.modelName - 模型名称
 * @param {string} data.modelType - 模型类型
 * @param {string} data.version - 版本号
 * @param {string} data.description - 描述
 * @param {string} data.modelFilePath - 模型文件路径
 * @returns {Promise}
 */
export const createModel = (data) => {
    return request.post('/models', data)
}

/**
 * 激活模型（管理员）
 * @param {number} id - 模型ID
 * @returns {Promise}
 */
export const activateModel = (id) => {
    return request.put(`/models/${id}/activate`)
}

/**
 * 停用模型（管理员）
 * @param {number} id - 模型ID
 * @returns {Promise}
 */
export const deactivateModel = (id) => {
    return request.put(`/models/${id}/deactivate`)
}

/**
 * 删除模型（管理员）
 * @param {number} id - 模型ID
 * @returns {Promise}
 */
export const deleteModel = (id) => {
    return request.delete(`/models/${id}`)
}
