import request from '@/utils/request.js'
import axios from 'axios'

export const getAllResults = (params) => {
  return request.get('/admin/analysis/results', { params })
}

export const getAdminOverview = (data) => {
  return request.post('/admin/analysis/overview', data)
}

export const getSentimentStats = (params) => {
  return axios.get('/fastapi/api/visualization/stats/sentiment', { params })
}

export const getTrendStats = (params) => {
  return axios.get('/fastapi/api/visualization/stats/trend', { params })
}

export const getAllStats = (params) => {
  // keywords top for admin
  return axios.get('/fastapi/api/visualization/keywords/top', { params })
}

export const getVisualizationData = (params) => {
  return axios.get('/fastapi/api/visualization/data', { params })
    .then(res => res.data)
}

// Normalized helpers for visualization
export const getAdminVizData = (params) => {
  return request.get('/admin/analysis/visualization-data', { params })
    .then(res => res.data || [])
}

export const getAdminKeywordsTop = (params) => {
  return axios.get('/fastapi/api/visualization/keywords/top', { params })
    .then(res => Array.isArray(res?.data) ? res.data : [])
}
