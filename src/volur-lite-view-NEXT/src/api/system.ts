import { get, post } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type {
  StorageResponseVO,
  AdminSystemOverviewVO,
  SystemSettingVO,
  SystemSettingUpdateDTO
} from '@/types/system'

export function getPublicSystemSettingsApi(): Promise<ApiResponse<Record<string, string>>> {
  return get<Record<string, string>>('/system/settings/public')
}

export function getAdminSystemOverviewApi(): Promise<ApiResponse<AdminSystemOverviewVO>> {
  return get<AdminSystemOverviewVO>('/admin/system/settings/overview')
}

export function getAdminSystemSettingsApi(): Promise<ApiResponse<SystemSettingVO[]>> {
  return get<SystemSettingVO[]>('/admin/system/settings')
}

export function updateAdminSystemSettingApi(data: SystemSettingUpdateDTO): Promise<ApiResponse<void>> {
  return post<void>('/admin/system/settings/update', data)
}

export function batchUpdateAdminSystemSettingsApi(data: SystemSettingUpdateDTO[]): Promise<ApiResponse<void>> {
  return post<void>('/admin/system/settings/batch-update', data)
}

export function testSmtpConnectionApi(): Promise<ApiResponse<void>> {
  return post<void>('/email/test')
}

export function uploadImageApi(
  file: File | FormData,
  folder: string = 'images'
): Promise<ApiResponse<StorageResponseVO>> {
  const data =
    file instanceof FormData
      ? file
      : (() => {
          const fd = new FormData()
          fd.append('file', file)
          return fd
        })()
  return post<StorageResponseVO>('/system/upload/image', data, {
    params: { folder },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadResourceApi(
  file: File | FormData,
  folder: string = 'resources'
): Promise<ApiResponse<StorageResponseVO>> {
  const data =
    file instanceof FormData
      ? file
      : (() => {
          const fd = new FormData()
          fd.append('file', file)
          return fd
        })()
  return post<StorageResponseVO>('/system/upload/resource', data, {
    params: { folder },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
