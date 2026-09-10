/**
 * 验证码与凭证模块类型定义 (对应 com.humolix.volur.verification)
 */

/** 验证码获取响应 VO (对应 com.humolix.volur.verification.domain.vo.CaptchaResponseVO) */
export interface CaptchaResponseVO {
  captchaId: string
  captchaType: string
  imageBase64: string
  sliderBase64?: string
  yPosition?: number
  backgroundImage?: string
  sliderImage?: string
  yCoordinate?: number
}

/** 验证码核销凭证 VO (对应 com.humolix.volur.verification.domain.vo.CaptchaTicketVO) */
export interface CaptchaTicketVO {
  ticket: string
  scene?: string
  expireSeconds?: number
}

/** 人机验证核验请求 DTO (对应 com.humolix.volur.verification.domain.dto.CaptchaVerifyDTO) */
export interface CaptchaVerifyDTO {
  captchaId: string
  userInput: string
  scene?: string
  costTime?: number
}

/** 发送邮箱验证码请求 DTO (对应 com.humolix.volur.verification.domain.dto.EmailCaptchaRequestDTO) */
export interface EmailCaptchaRequestDTO {
  email: string
  username?: string
  captchaId?: string
  captchaCode?: string
  captchaTicket?: string
}

/** 邮件日志视图 VO (对应 com.humolix.volur.verification.domain.vo.EmailLogVO) */
export interface EmailLogVO {
  id: number | string
  recipient: string
  subject: string
  content?: string
  type: string
  status: 'SUCCESS' | 'FAILED' | string
  errorMessage?: string
  sender?: string
  operatorId?: number | string
  operatorIp?: string
  createdAt: string
}

/** 管理端邮件发信统计指标 VO (对应 com.humolix.volur.verification.domain.vo.AdminEmailStatisticsVO) */
export interface AdminEmailStatisticsVO {
  totalEmails: number
  successEmails: number
  failedEmails: number
  todayEmails: number
}

/** 管理端邮件分页检索入参 DTO (对应 com.humolix.volur.verification.domain.dto.AdminEmailQueryDTO) */
export interface AdminEmailQueryDTO {
  page?: number
  size?: number
  sortBy?: string
  sortOrder?: string
  keyword?: string
  type?: string
  status?: string
  startTime?: string
  endTime?: string
}

/** 管理端手动发送邮件请求 DTO (对应 com.humolix.volur.verification.domain.dto.AdminSendEmailDTO) */
export interface AdminSendEmailDTO {
  recipient: string
  subject: string
  content: string
  html?: boolean
}
