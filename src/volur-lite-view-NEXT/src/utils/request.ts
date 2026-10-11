import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/common/api'
import { buildSessionExpireHref } from '@/utils/flow'

declare module 'axios' {
  export interface AxiosRequestConfig {
    skipToast?: boolean
  }
}

function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null
  const match = document.cookie.match(new RegExp('(?:^|;\\s*)' + name + '=([^;]*)'))
  return match ? decodeURIComponent(match[1]) : null
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

service.interceptors.request.use(
  (config) => {
    const method = (config.method || 'get').toUpperCase()
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
      const xsrfToken = getCookie('XSRF-TOKEN')
      if (xsrfToken && config.headers) {
        config.headers['X-XSRF-TOKEN'] = xsrfToken
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<any>
    if (res && typeof res.success === 'boolean' && !res.success) {
      const msg = res.message || '请求处理失败'
      const skipToast = Boolean(response.config?.skipToast)
      if (!skipToast && typeof window !== 'undefined' && window.$message) {
        window.$message.error(msg)
      }
      const err = new Error(msg)
      ;(err as any).response = response
      const lower = msg.toLowerCase()
      if (
        (response.status === 401 && (lower.includes('dsh') || lower.includes('会话') || lower.includes('失效'))) ||
        (response.status === 403 && (lower.includes('tl') || lower.includes('流转票据') || lower.includes('已被核销') || lower.includes('重放')))
      ) {
        if (typeof window !== 'undefined' && window.location.pathname !== '/v1/info/sessionexpire') {
          window.location.href = buildSessionExpireHref()
        }
      }
      return Promise.reject(err)
    }
    return response.data
  },
  (error) => {
    const errorMsg =
      error.response?.data?.message ||
      error.message ||
      '网络请求异常，请检查网络连接'
    const skipToast = Boolean(error.config?.skipToast)
    if (!skipToast && typeof window !== 'undefined' && window.$message) {
      window.$message.error(errorMsg)
    }
    const status = error.response?.status
    const lower = String(errorMsg).toLowerCase()
    if (
      (status === 401 && (lower.includes('dsh') || lower.includes('会话') || lower.includes('失效'))) ||
      (status === 403 && (lower.includes('tl') || lower.includes('流转票据') || lower.includes('已被核销') || lower.includes('重放')))
    ) {
      if (typeof window !== 'undefined' && window.location.pathname !== '/v1/info/sessionexpire') {
        window.location.href = buildSessionExpireHref()
      }
    }
    const normalizedError = new Error(errorMsg)
    ;(normalizedError as any).response = error.response
    ;(normalizedError as any).status = error.response?.status
    return Promise.reject(normalizedError)
  }
)

export default service

export function get<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.get(url, config)
}

export function post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.post(url, data, config)
}

export function put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.put(url, data, config)
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.delete(url, config)
}

export function patch<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.patch(url, data, config)
}
