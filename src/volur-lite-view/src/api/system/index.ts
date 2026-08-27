import { request } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type { SystemSettingVO, SystemSettingUpdateDTO, AdminSystemOverviewVO } from '@/types/system'

/**
 * 获取公开系统设置
 * 对应后端: GET /api/v1/system/settings/public
 */
export function getPublicSystemSettingsApi(): Promise<ApiResponse<Record<string, string>>> {
  return request.get<Record<string, string>>('/system/settings/public')
}

/**
 * 管理端：获取系统运行健康与底层环境概览
 * 对应后端: GET /api/v1/admin/system/settings/overview
 */
export function getAdminSystemOverviewApi(): Promise<ApiResponse<AdminSystemOverviewVO>> {
  return request.get<AdminSystemOverviewVO>('/admin/system/settings/overview')
}

/**
 * 管理端：获取所有系统设置列表
 * 对应后端: GET /api/v1/admin/system/settings
 */
export function getAdminSystemSettingsApi(): Promise<ApiResponse<SystemSettingVO[]>> {
  return request.get<SystemSettingVO[]>('/admin/system/settings')
}

/**
 * 管理端：单个更新系统设置
 * 对应后端: POST /api/v1/admin/system/settings/update
 */
export function updateAdminSystemSettingApi(data: SystemSettingUpdateDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/system/settings/update', data)
}

/**
 * 管理端：批量更新系统设置
 * 对应后端: POST /api/v1/admin/system/settings/batch-update
 */
export function batchUpdateAdminSystemSettingsApi(data: SystemSettingUpdateDTO[]): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/system/settings/batch-update', data)
}

/**
 * 管理端：测试 SMTP 邮件服务器连通性
 * 对应后端: POST /api/v1/email/test
 */
export function testSmtpConnectionApi(): Promise<ApiResponse<void>> {
  return request.post<void>('/email/test')
}
