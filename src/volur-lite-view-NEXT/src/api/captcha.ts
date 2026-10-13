import { get, post } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type { SignInFlowResponseVO } from '@/types/auth/flow'

export interface RecaptchaChallengeVO {
  challengeType: 'CLICK' | 'PUZZLE' | 'SLIDER' | 'IMAGE' | 'SELECT_IMAGE'
  recaptchaToken: string
  publicKey: string
  backgroundImage?: string
  puzzleImage?: string
  puzzleY?: number
  captchaImage?: string
  promptText?: string
  extraData?: Record<string, unknown>
}

export interface RecaptchaVerifyVO {
  passed: boolean
  recaptchaTicket: string
}

export function getRecaptchaChallengeApi(dsh: string, tl: string): Promise<ApiResponse<RecaptchaChallengeVO>> {
  return get<RecaptchaChallengeVO>('/captcha/challenge/recaptcha', {
    params: { dsh, tl },
    skipToast: true
  })
}

export function verifyRecaptchaChallengeApi(
  dsh: string,
  tl: string,
  data: { recaptchaToken: string; payload: string }
): Promise<ApiResponse<RecaptchaVerifyVO>> {
  return post<RecaptchaVerifyVO>('/captcha/challenge/recaptcha/verify', data, {
    params: { dsh, tl },
    skipToast: true
  })
}

export function submitRecaptchaChallengeApi(data: {
  dsh: string
  tl: string
  cid?: number
  recaptchaTicket: string
  recoveryType?: string
}): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/captcha/challenge/recaptcha', data, { skipToast: true })
}

export interface CaptchaImageChallengeVO {
  captchaId: string
  captchaType?: string
  imageBase64: string
}

export function getImageCaptchaApi(params?: {
  scene?: string
  type?: string
  oldCaptchaId?: string
}): Promise<ApiResponse<CaptchaImageChallengeVO>> {
  return get<CaptchaImageChallengeVO>('/captcha/challenge', { params, skipToast: true })
}

