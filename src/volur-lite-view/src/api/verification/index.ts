import { request } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type {
  CaptchaResponseVO,
  CaptchaTicketVO,
  CaptchaVerifyDTO,
  EmailCaptchaRequestDTO
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
