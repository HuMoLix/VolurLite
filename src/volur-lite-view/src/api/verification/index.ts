import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  CaptchaResponseVO,
  CaptchaTicketVO,
  CaptchaVerifyDTO,
  EmailCaptchaRequestDTO,
  EmailLogVO,
  AdminEmailStatisticsVO,
  AdminEmailQueryDTO,
  AdminSendEmailDTO
} from '@/types/verification'

/**
 * 获取场景化人机验证挑战 (滑块/旋转等)
 * 对应后端: GET /api/v1/captcha/challenge
 */
export function getCaptchaChallengeApi(params?: {
  scene?: string
  type?: string
  oldCaptchaId?: string
}): Promise<ApiResponse<CaptchaResponseVO>> {
  return request.get<CaptchaResponseVO>('/captcha/challenge', { params })
}

/**
 * 获取通用图形验证码
 * 对应后端: GET /api/v1/captcha
 */
export function getCaptchaApi(oldCaptchaId?: string): Promise<ApiResponse<CaptchaResponseVO>> {
  return request.get<CaptchaResponseVO>('/captcha', { params: { oldCaptchaId } })
}

/**
 * 核验验证码并颁发通行凭证 (Ticket)
 * 对应后端: POST /api/v1/captcha/verify-ticket
 */
export function verifyCaptchaTicketApi(data: CaptchaVerifyDTO): Promise<ApiResponse<CaptchaTicketVO>> {
  return request.post<CaptchaTicketVO>('/captcha/verify-ticket', data)
}

/**
 * 发送注册邮箱验证码
 * 对应后端: POST /api/v1/email/captcha/register
 */
export function sendRegisterEmailCodeApi(data: EmailCaptchaRequestDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/email/captcha/register', data)
}

/**
 * 管理端：获取邮件发信统计指标
 * 对应后端: GET /api/v1/admin/emails/statistics
 */
export function getAdminEmailStatisticsApi(): Promise<ApiResponse<AdminEmailStatisticsVO>> {
  return request.get<AdminEmailStatisticsVO>('/admin/emails/statistics')
}

/**
 * 管理端：分页查询邮件发送记录列表
 * 对应后端: GET /api/v1/admin/emails
 */
export function getAdminEmailListApi(
  params?: AdminEmailQueryDTO
): Promise<ApiResponse<PageResponse<EmailLogVO>>> {
  return request.get<PageResponse<EmailLogVO>>('/admin/emails', { params })
}

/**
 * 管理端：获取指定邮件详细内容
 * 对应后端: GET /api/v1/admin/emails/{id}
 */
export function getAdminEmailDetailApi(id: number | string): Promise<ApiResponse<EmailLogVO>> {
  return request.get<EmailLogVO>(`/admin/emails/${id}`)
}

/**
 * 管理端：管理员手动发送邮件
 * 对应后端: POST /api/v1/admin/emails/send
 */
export function sendAdminManualEmailApi(data: AdminSendEmailDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/emails/send', data)
}

/**
 * 管理端：重发指定邮件
 * 对应后端: POST /api/v1/admin/emails/{id}/resend
 */
export function resendAdminEmailApi(id: number | string): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/emails/${id}/resend`)
}

