import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  EmailLogVO,
  AdminEmailStatisticsVO,
  AdminEmailQueryDTO,
  AdminSendEmailDTO
} from '@/types/email'

export function getAdminEmailStatisticsApi(): Promise<ApiResponse<AdminEmailStatisticsVO>> {
  return request.get<AdminEmailStatisticsVO>('/admin/emails/stats')
}

export function getAdminEmailListApi(
  params?: AdminEmailQueryDTO
): Promise<ApiResponse<PageResponse<EmailLogVO>>> {
  return request.get<PageResponse<EmailLogVO>>('/admin/emails', { params })
}

export function getAdminEmailDetailApi(id: number | string): Promise<ApiResponse<EmailLogVO>> {
  return request.get<EmailLogVO>(`/admin/emails/${id}`)
}

export function sendAdminManualEmailApi(data: AdminSendEmailDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/emails/send', data)
}

export function resendAdminEmailApi(id: number | string): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/emails/${id}/resend`)
}
