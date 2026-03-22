// src/api/analysis.js
import request from '@/utils/request.js'
import axios from 'axios'

/**
 * 单条文本情感分析
 * @param {Object} data - 分析请求数据
 * @param {string} data.content - 要分析的文本内容
 * @param {number} data.modelId - 模型ID（可选）
 * @param {string} data.taskName - 任务名称（可选）
 * @returns {Promise} 分析任务结果
 */
export const analyzeSingleText = (data) => {
    // Call FastAPI directly via proxy
    return axios.post('/fastapi/api/analysis/single', data).then(res => res.data)
}

/**
 * 批量文本情感分析
 * @param {Object} data - 批量分析请求数据
 * @param {number} data.fileId - 文件ID
 * @param {number} data.modelId - 模型ID（可选）
 * @param {string} data.taskName - 任务名称（可选）
 * @returns {Promise} 分析任务结果
 */
export const analyzeBatchText = (data) => {
    // Call FastAPI directly via proxy
    return axios.post('/fastapi/api/analysis/batch', data).then(res => res.data)
}

/**
 * 查询用户任务列表
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页大小
 * @param {string} params.status - 任务状态（可选）
 * @param {number} params.userId - 用户ID（可选，仅管理员可用）
 * @returns {Promise} 任务分页列表
 */
export const getTaskList = (params) => {
    return request.get('/analysis/tasks', { params })
}

/**
 * 查询任务结果列表
 * @param {number} taskId - 任务ID
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页大小
 * @returns {Promise} 结果分页列表
 */
export const getTaskResults = (taskId, params) => {
    return request.get(`/analysis/results/${taskId}`, { params })
}

/**
 * 删除任务
 * @param {number} taskId - 任务ID
 * @returns {Promise}
 */
export const deleteTask = (taskId) => {
    return request.delete(`/analysis/tasks/${taskId}`)
}

/**
 * 删除单条分析结果
 * @param {number} resultId - 结果ID
 * @returns {Promise}
 */
export const deleteResult = (resultId) => {
    return request.delete(`/analysis/results/${resultId}`)
}

/**
 * 获取可视化数据
 * @param {Object} params - 查询参数
 * @param {string} params.startDate - 开始日期
 * @param {string} params.endDate - 结束日期
 * @returns {Promise} 可视化数据列表
 */
export const getVisualizationData = (params) => {
    return request.get('/analysis/visualization-data', { params })
}

export const extractKeywords = (data) => {
    // Call FastAPI directly via proxy
    return axios.post('/fastapi/api/keywords/extract', data)
}

export const wordCloud = (params) => {
    return axios.get('/fastapi/api/visualization/wordcloud', { params })
}

// Normalized helpers for visualization
export const getUserVizData = (params) => {
    return request.get('/analysis/visualization-data', { params })
        .then(res => res.data || [])
}

export const getWordCloudData = (params) => {
    return axios.get('/fastapi/api/visualization/wordcloud', { params })
        .then(res => Array.isArray(res?.data?.data) ? res.data.data : (Array.isArray(res?.data) ? res.data : []))
}

export const sentimentPie = (data) => {
    return request.post('/analysis/visualization/sentiment_pie', data)
}

export const lineTrend = (data) => {
    return request.post('/analysis/visualization/line_trend', data)
}

export const overview = (data) => {
    return request.post('/analysis/visualization/overview', data)
}

export const uploadAndPredict = (formData) => {
    return axios.post('/fastapi/api/analysis/upload_predict', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}
