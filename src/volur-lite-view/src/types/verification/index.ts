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
