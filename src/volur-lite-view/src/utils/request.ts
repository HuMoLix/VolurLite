import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types/common/api'
import type { AuthenticateResponseVO } from '@/types/auth'
import { getAccessToken, getRefreshToken, setAccessToken, clearAuthStorage } from './auth'

/**
 * 安全 JSON 反序列化：自动将超出 JS 安全整数范围（16 位及以上）的 Long / Snowflake ID 转换为字符串，彻底杜绝精度截断
 */
export function safeJsonParse(data: any): any {
  if (typeof data !== 'string') {
    return data
  }
  try {
    const safeData = data.replace(/([:,\[]\s*)(-?\d{16,})(?=[,\s\]}])/g, '$1"$2"')
    return JSON.parse(safeData)
  } catch {
    return JSON.parse(data)
  }
}

// Create Axios instance
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  },
  transformResponse: [
    (data) => safeJsonParse(data)
  ]
})

let isRefreshing = false
let refreshSubscribers: ((token: string) => void)[] = []

function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

function onRefreshed(token: string) {
  refreshSubscribers.forEach((cb) => cb(token))
  refreshSubscribers = []
}

// Request interceptor
service.interceptors.request.use(
  (config) => {
    const token = getAccessToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<any>>) => {
    const res = response.data
    // If backend returns standard ApiResponse
    if (res && typeof res.success === 'boolean') {
      if (res.success) {
        return response
      }
      // Business Error
      const errorMsg = res.message || '系统业务处理异常'
      if (window.$message) {
        window.$message.error(errorMsg)
      }
      return Promise.reject(new Error(errorMsg))
    }
    return response
  },
  async (error) => {
    const originalRequest = error.config

    if (error.response) {
      const { status, data } = error.response

      // Handle 401 Unauthorized with Refresh Token
      if (status === 401 && !originalRequest._retry) {
        const refreshToken = getRefreshToken()
        if (refreshToken) {
          if (!isRefreshing) {
            isRefreshing = true
            originalRequest._retry = true

            try {
              const refreshResponse = await axios.post<ApiResponse<AuthenticateResponseVO>>(
                `${import.meta.env.VITE_API_BASE_URL || '/api/v1'}/auth/refresh`,
                { refreshToken }
              )

              if (refreshResponse.data.success && refreshResponse.data.data?.accessToken) {
                const newToken = refreshResponse.data.data.accessToken
                setAccessToken(newToken)
                isRefreshing = false
                onRefreshed(newToken)
                originalRequest.headers.Authorization = `Bearer ${newToken}`
                return service(originalRequest)
              }
            } catch (refreshErr) {
              isRefreshing = false
              clearAuthStorage()
              if (window.$message) {
                window.$message.warning('登录状态已过期，请重新登录')
              }
              window.location.href = '/login'
              return Promise.reject(refreshErr)
            }
          } else {
            // Queue requests while refreshing
            return new Promise((resolve) => {
              subscribeTokenRefresh((newToken: string) => {
                originalRequest.headers.Authorization = `Bearer ${newToken}`
                resolve(service(originalRequest))
              })
            })
          }
        } else {
          clearAuthStorage()
          if (window.$message) {
            window.$message.warning('请先登录账号')
          }
        }
      } else if (status === 503) {
        const message = data?.message || '系统正在全站维护升级中，暂不可用'
        if (window.$message) {
          window.$message.warning(message)
        }
        const currentPath = window.location.pathname
        if (currentPath !== '/maintenance' && !currentPath.startsWith('/auth') && !currentPath.startsWith('/admin')) {
          window.location.href = '/maintenance'
        }
      } else {
        const message = data?.message || error.message || '网络请求错误'
        if (window.$message) {
          window.$message.error(message)
        }
      }
    } else {
      if (window.$message) {
        window.$message.error('网络连接不可用，请检查网络设置')
      }
    }
    return Promise.reject(error)
  }
)

/**
 * Standard typed request helper functions
 */
export const request = {
  async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    const res = await service.get<ApiResponse<T>>(url, config)
    return res.data
  },
  async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    const res = await service.post<ApiResponse<T>>(url, data, config)
    return res.data
  },
  async put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    const res = await service.put<ApiResponse<T>>(url, data, config)
    return res.data
  },
  async patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    const res = await service.patch<ApiResponse<T>>(url, data, config)
    return res.data
  },
  async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    const res = await service.delete<ApiResponse<T>>(url, config)
    return res.data
  }
}

export default service
