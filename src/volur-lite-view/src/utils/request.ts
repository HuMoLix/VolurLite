import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types/common/api'
import type { AuthenticateResponseVO } from '@/types/auth'
import { getAccessToken, getRefreshToken, setAccessToken, clearAuthStorage } from './auth'

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

let lastNetworkErrorTime = 0
const NETWORK_ERROR_THROTTLE = 2000

function showThrottledNetworkError(msg: string) {
  const now = Date.now()
  const currentPath = window.location.pathname
  if (currentPath === '/maintenance') {
    return
  }
  if (now - lastNetworkErrorTime > NETWORK_ERROR_THROTTLE) {
    lastNetworkErrorTime = now
    if (window.$message) {
      window.$message.error(msg)
    }
  }
}

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<any>>) => {
    const res = response.data
    if (res && typeof res.success === 'boolean') {
      if (res.success) {
        return response
      }
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

    const navigateToMaintenance = () => {
      const currentPath = window.location.pathname
      if (currentPath !== '/maintenance') {
        window.location.href = '/maintenance'
      }
    }

    if (error.response) {
      const { status, data } = error.response
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
        showThrottledNetworkError(message)
        navigateToMaintenance()
      } else if (status === 502 || status === 504) {
        const message = data?.message || '后端服务连接超时或网关异常，请稍后重试'
        showThrottledNetworkError(message)
      } else {
        let message = data?.message
        if (data instanceof Blob && (data.type?.includes('application/json') || data.type?.includes('text/plain'))) {
          try {
            const text = await data.text()
            const json = JSON.parse(text)
            message = json.message || json.msg
          } catch {
            // ignore
          }
        }
        message = message || error.message || '网络请求错误'
        if (window.$message) {
          window.$message.error(message)
        }
      }
    } else {
      const isTimeout = error.code === 'ECONNABORTED' || error.message?.includes('timeout')
      const message = isTimeout ? '网络请求超时，请检查服务与网络连接' : (error.message || '网络请求错误，请检查网络连接')
      showThrottledNetworkError(message)
    }
    return Promise.reject(error)
  }
)

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
  },
  async download(url: string, config?: AxiosRequestConfig): Promise<Blob> {
    const res = await service.get(url, {
      responseType: 'blob',
      timeout: 0,
      ...config
    })
    return res.data as Blob
  }
}

export default service
