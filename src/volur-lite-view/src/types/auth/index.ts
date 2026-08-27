/**
 * 认证模块类型定义 (对应 com.humolix.volur.auth)
 */

/** 用户登录请求 DTO */
export interface AuthenticateRequestDTO {
  username: string
  password: string
  captchaId?: string
  captchaCode?: string
  captchaTicket?: string
  rememberMe?: boolean
}

/** 用户登录成功响应 VO */
export interface AuthenticateResponseVO {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userId: number | string
  username: string
  nickname: string
  role: string
  avatar?: string
}

/** 用户注册请求 DTO (对应 com.humolix.volur.auth.domain.dto.RegisterRequestDTO) */
export interface RegisterRequestDTO {
  username: string
  password: string
  email: string
  emailCaptcha?: string
  captchaId?: string
  captchaCode?: string
  captchaTicket?: string
}

/** 刷新 Token 请求 DTO */
export interface RefreshTokenRequestDTO {
  refreshToken: string
}

/** 找回密码申请请求 DTO (对应 com.humolix.volur.auth.domain.dto.ForgotPasswordRequestDTO) */
export interface ForgotPasswordRequestDTO {
  email: string
  username: string
  captchaId?: string
  captchaCode?: string
  captchaTicket?: string
}

/** 凭借 Token 重置密码请求 DTO (对应 com.humolix.volur.auth.domain.dto.ForgotPasswordResetRequestDTO) */
export interface ForgotPasswordResetRequestDTO {
  token: string
  newPassword: string
}

/** 根据邮箱验证码确认重置密码 DTO (对应 com.humolix.volur.auth.domain.dto.ResetPasswordRequestDTO) */
export interface ResetPasswordRequestDTO {
  email: string
  code: string
  password: string
}

/** 校验修改密码邮箱验证码请求 DTO (对应 com.humolix.volur.auth.domain.dto.VerifyChangePasswordCodeRequestDTO) */
export interface VerifyChangePasswordCodeRequestDTO {
  email: string
  username?: string
  code: string
}

/** 修改密码请求 DTO (对应 com.humolix.volur.auth.domain.dto.ChangePasswordRequestDTO) */
export interface ChangePasswordRequestDTO {
  token: string
  newPassword: string
}
