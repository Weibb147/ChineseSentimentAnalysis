// request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { isTokenExpired } from '@/utils/jwtUtils'

const request = axios.create({
  baseURL: '/api', // 确保 baseURL 是 /api，这样会通过代理访问后端
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const authStore = useAuthStore()
    // 检查是否有token，并正确设置Authorization头
    if (authStore.token) {
      // 检查token是否过期
      if (isTokenExpired(authStore.token)) {
        authStore.removeAuth()
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(new Error('Token expired'))
      }
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    // 兼容 code=0 和 code=200 的成功状态
    if (res.code === 0 || res.code === 200) {
      return res
    }
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // 未登录或token过期
          const authStore = useAuthStore()
          authStore.removeAuth()
          if (router.currentRoute.value.path !== '/login') {
            router.push('/login')
            ElMessage.error('请重新登录')
          }
          break
        case 400:
          ElMessage.error(error.response.data.message || '请求参数错误')
          break
        case 403:
          ElMessage.error('没有操作权限')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.response.data.message || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('网络错误,请检查网络连接')
    } else {
      ElMessage.error('请求配置错误')
    }
    return Promise.reject(error)
  }
)

export default request