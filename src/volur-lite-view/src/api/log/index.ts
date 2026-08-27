import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type { AdminLogVO, AdminLogStatisticsVO, AdminLogQueryDTO } from '@/types/log'

/**
 * 管理端：分页获取系统日志列表
 * 对应后端: GET /api/v1/admin/logs
 */
export function getAdminLogListApi(
  params?: AdminLogQueryDTO
): Promise<ApiResponse<PageResponse<AdminLogVO>>> {
  return request.get<PageResponse<AdminLogVO>>('/admin/logs', { params })
}

/**
 * 管理端：获取系统日志统计数据
 * 对应后端: GET /api/v1/admin/logs/statistics
 */
export function getAdminLogStatisticsApi(): Promise<ApiResponse<AdminLogStatisticsVO>> {
  return request.get<AdminLogStatisticsVO>('/admin/logs/statistics')
}

/**
 * 管理端：获取指定日志详情
 * 对应后端: GET /api/v1/admin/logs/{id}
 */
export function getAdminLogDetailApi(id: number | string): Promise<ApiResponse<AdminLogVO>> {
  return request.get<AdminLogVO>(`/admin/logs/${id}`)
}
