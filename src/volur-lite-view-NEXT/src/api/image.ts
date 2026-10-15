import { request } from '@/utils/request'
import type { AxiosRequestConfig } from 'axios'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  AdminClusterImageVO,
  AdminClusterImageDetailVO,
  AdminClusterImageStatisticsVO,
  AdminImagePageRequestDTO,
  AdminImagePullRequestDTO,
  AdminImageTransferDTO,
  AdminImageBackupDTO
} from '@/types/image'
import type { AdminAsyncTaskSubmitVO } from '@/types/task'

export function getAdminImageStatisticsApi(): Promise<ApiResponse<AdminClusterImageStatisticsVO>> {
  return request.get<AdminClusterImageStatisticsVO>('/admin/images/stats')
}

export function getAdminImageListApi(
  params?: AdminImagePageRequestDTO
): Promise<ApiResponse<PageResponse<AdminClusterImageVO>>> {
  return request.get<PageResponse<AdminClusterImageVO>>('/admin/images', { params })
}

export function getAdminImageDetailApi(
  id: string | number
): Promise<ApiResponse<AdminClusterImageDetailVO>> {
  return request.get<AdminClusterImageDetailVO>(`/admin/images/${id}`)
}

export function scanAndSyncAdminImagesApi(): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/images/scan')
}

export function pullAdminImageApi(data: AdminImagePullRequestDTO): Promise<ApiResponse<AdminAsyncTaskSubmitVO>> {
  return request.post<AdminAsyncTaskSubmitVO>('/admin/images/pull', data)
}

export function transferAdminImageApi(
  data: AdminImageTransferDTO
): Promise<ApiResponse<AdminAsyncTaskSubmitVO>> {
  return request.post<AdminAsyncTaskSubmitVO>('/admin/images/transfer', data)
}

export function backupAdminImageApi(
  data: AdminImageBackupDTO
): Promise<ApiResponse<AdminAsyncTaskSubmitVO>> {
  return request.post<AdminAsyncTaskSubmitVO>('/admin/images/backup', data)
}

export function exportAdminImageApi(id: string | number): Promise<Blob> {
  return request.download(`/admin/images/${id}/export`)
}

export function importAdminImageApi(
  formData: FormData,
  config?: AxiosRequestConfig
): Promise<ApiResponse<AdminAsyncTaskSubmitVO>> {
  return request.post<AdminAsyncTaskSubmitVO>('/admin/images/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 0,
    ...config
  })
}

export function deleteAdminImageApi(
  id: string | number,
  targetNodeIp?: string | string[]
): Promise<ApiResponse<void>> {
  const ipParam = Array.isArray(targetNodeIp) ? targetNodeIp.join(',') : targetNodeIp
  return request.delete<void>(`/admin/images/${id}`, {
    params: ipParam ? { targetNodeIp: ipParam } : undefined
  })
}
