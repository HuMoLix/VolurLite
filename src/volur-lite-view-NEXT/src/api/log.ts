import { get } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type { AdminLogVO, AdminLogStatisticsVO, AdminLogQueryDTO } from '@/types/log'

export function getAdminLogListApi(
  params?: AdminLogQueryDTO
): Promise<ApiResponse<PageResponse<AdminLogVO>>> {
  return get<PageResponse<AdminLogVO>>('/admin/logs', { params })
}

export function getAdminLogStatisticsApi(): Promise<ApiResponse<AdminLogStatisticsVO>> {
  return get<AdminLogStatisticsVO>('/admin/logs/stats')
}

export function getAdminLogDetailApi(id: number | string): Promise<ApiResponse<AdminLogVO>> {
  return get<AdminLogVO>(`/admin/logs/${id}`)
}
