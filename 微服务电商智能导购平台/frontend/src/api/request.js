import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器 — 注入 JWT Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('eshop_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器 — 统一解包 + 错误处理
request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body.code === 200) return body.data
    // Token 过期 → 清除登录态
    if (body.code === 401) {
      localStorage.removeItem('eshop_token')
      localStorage.removeItem('eshop_user')
      window.location.reload()
    }
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('eshop_token')
      localStorage.removeItem('eshop_user')
    }
    return Promise.reject(error)
  },
)

export default request
