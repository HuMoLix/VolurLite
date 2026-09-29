import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/common/api'

declare module 'axios' {
  export interface AxiosRequestConfig {
    skipToast?: boolean
  }
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

service.interceptors.request.use(
  (config) => {
    const token =
      typeof localStorage !== 'undefined'
        ? localStorage.getItem('access_token') || sessionStorage.getItem('access_token')
        : null
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
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
