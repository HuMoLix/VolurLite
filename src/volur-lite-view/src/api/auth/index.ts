import { request } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type {
  AuthenticateRequestDTO,
  AuthenticateResponseVO,
  RegisterRequestDTO,
  RefreshTokenRequestDTO,
  ForgotPasswordRequestDTO,
  ForgotPasswordResetRequestDTO,
  ResetPasswordRequestDTO,
  VerifyChangePasswordCodeRequestDTO,
  ChangePasswordRequestDTO,
  DeleteAccountRequestDTO
} from '@/types/auth'
import type { EmailCaptchaRequestDTO } from '@/types/verification'

/**
 * 用户注册
 * 对应后端: POST /api/v1/auth/register
 */
export function registerApi(data: RegisterRequestDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/auth/register', data)
}

/**
 * 用户登录
 * 对应后端: POST /api/v1/auth/login
 */
export function loginApi(data: AuthenticateRequestDTO): Promise<ApiResponse<AuthenticateResponseVO>> {
  return request.post<AuthenticateResponseVO>('/auth/login', data)
}

/**
 * 刷新 JWT 访问令牌
 * 对应后端: POST /api/v1/auth/refresh
 */
export function refreshTokenApi(data: RefreshTokenRequestDTO): Promise<ApiResponse<AuthenticateResponseVO>> {
  return request.post<AuthenticateResponseVO>('/auth/refresh', data)
}

/**
 * 用户登出
 * 对应后端: POST /api/v1/auth/logout
 */
export function logoutApi(): Promise<ApiResponse<void>> {
  return request.post<void>('/auth/logout')
}

/**
 * 发起找回密码申请
 * 对应后端: POST /api/v1/auth/forgot-password
 */
export function forgotPasswordApi(data: ForgotPasswordRequestDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/auth/forgot-password', data)
}

/**
 * 根据重置令牌重置密码
 * 对应后端: POST /api/v1/auth/password-resets
 */
export function resetPasswordByTokenApi(data: ForgotPasswordResetRequestDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/auth/password-resets', data)
}

/**
 * 根据邮箱验证码确认重置密码
 * 对应后端: POST /api/v1/auth/reset-password-confirm
 */
export function resetPasswordConfirmApi(data: ResetPasswordRequestDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/auth/reset-password-confirm', data)
}

/**
 * 发送修改密码邮箱验证码
 * 对应后端: POST /api/v1/auth/password/send-code
 */
export function sendChangePasswordEmailCodeApi(data: EmailCaptchaRequestDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/auth/password/send-code', data)
}

/**
 * 核验修改密码邮箱验证码并获取令牌
 * 对应后端: POST /api/v1/auth/password/verify-code
 */
export function verifyChangePasswordCodeApi(data: VerifyChangePasswordCodeRequestDTO): Promise<ApiResponse<string>> {
  return request.post<string>('/auth/password/verify-code', data)
}

/**
 * 修改当前用户密码
 * 对应后端: POST /api/v1/auth/password/change
 */
export function changePasswordApi(data: ChangePasswordRequestDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/auth/password/change', data)
}

/**
 * 发送注销账号邮箱验证码 (2FA)
 * 对应后端: POST /api/v1/auth/account/send-code
 */
export function sendDeleteAccountEmailCodeApi(data?: EmailCaptchaRequestDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/auth/account/send-code', data || {})
}

/**
 * 双重验证安全注销当前用户账号 (2FA)
 * 对应后端: POST /api/v1/auth/account/delete
 */
export function deleteAccountWith2FaApi(data: DeleteAccountRequestDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/auth/account/delete', data)
}

/**
 * 注销当前用户账号 (DELETE兼容)
 * 对应后端: DELETE /api/v1/auth/account
 */
export function deleteAccountApi(data?: DeleteAccountRequestDTO): Promise<ApiResponse<void>> {
  return request.delete<void>('/auth/account', { data })
}
