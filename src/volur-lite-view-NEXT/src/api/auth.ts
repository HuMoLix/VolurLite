import { get, post } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type {
  FlowInitVO,
  FlowInitParams,
  SignInFlowResponseVO,
  SignInIdentifierRequestDTO,
  SignInChallengeRequestDTO,
  ChallengeOptionVO,
  SignUpFlowResponseVO,
  SignUpNameRequestDTO,
  SignUpEmailRequestDTO,
  SignUpEmailResendRequestDTO,
  SignUpEtotpChallengeRequestDTO,
  SignUpPasswordRequestDTO,
  SignUpOptionVO,
  SignUpCompleteRequestDTO,
  PasskeyCreationOptionsVO,
  PasskeyRegisterRequestDTO,
  PasskeyRegisterResponseVO
} from '@/types/auth/flow'

export function initFlowSessApi(params: FlowInitParams): Promise<ApiResponse<FlowInitVO>> {
  return get<FlowInitVO>('/flow/initSess', { params, skipToast: true })
}

export function submitSignInIdentifierApi(
  data: SignInIdentifierRequestDTO
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/auth/signin/identifier', data, { skipToast: true })
}

export function submitSignInChallengeApi(
  data: SignInChallengeRequestDTO
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/auth/signin/challenge', data, { skipToast: true })
}

export function fetchChallengeSelectionApi(params?: {
  dsh?: string
  currentType?: string
}): Promise<ApiResponse<ChallengeOptionVO[]>> {
  return get<ChallengeOptionVO[]>('/auth/signin/challenge/selection', { params, skipToast: true })
}

export function submitSignUpNameApi(
  data: SignUpNameRequestDTO
): Promise<ApiResponse<SignUpFlowResponseVO>> {
  return post<SignUpFlowResponseVO>('/auth/signup/name', data, { skipToast: true })
}

export function submitSignUpEmailApi(
  data: SignUpEmailRequestDTO
): Promise<ApiResponse<SignUpFlowResponseVO>> {
  return post<SignUpFlowResponseVO>('/auth/signup/email', data, { skipToast: true })
}

export function resendSignUpEmailApi(
  data: SignUpEmailResendRequestDTO
): Promise<ApiResponse<void>> {
  return post<void>('/auth/signup/email/resend', data, { skipToast: true })
}

export function submitSignUpEtotpApi(
  data: SignUpEtotpChallengeRequestDTO
): Promise<ApiResponse<SignUpFlowResponseVO>> {
  return post<SignUpFlowResponseVO>('/auth/signup/challenge/etotp', data, { skipToast: true })
}

export function submitSignUpPasswordApi(
  data: SignUpPasswordRequestDTO
): Promise<ApiResponse<SignUpFlowResponseVO>> {
  return post<SignUpFlowResponseVO>('/auth/signup/password', data, { skipToast: true })
}

export function fetchSignUpOptionsApi(params: {
  dsh: string
  tl: string
}): Promise<ApiResponse<SignUpOptionVO[]>> {
  return get<SignUpOptionVO[]>('/auth/signup/protect/options', { params, skipToast: true })
}

export function completeSignUpApi(
  data: SignUpCompleteRequestDTO
): Promise<ApiResponse<SignUpFlowResponseVO>> {
  return post<SignUpFlowResponseVO>('/auth/signup/complete', data, { skipToast: true })
}

export function fetchPasskeyCreationOptionsApi(params: {
  dsh: string
  tl: string
  authenticatorType?: string
}): Promise<ApiResponse<PasskeyCreationOptionsVO>> {
  return get<PasskeyCreationOptionsVO>('/captcha/challenge/passkey', { params, skipToast: true })
}

export function fetchSupportedPasskeyAuthenticatorsApi(): Promise<
  ApiResponse<
    Array<{
      key: string
      name: string
      attachment: string
      description: string
      icon: string
      available: boolean
    }>
  >
> {
  return get('/captcha/challenge/passkey/authenticators', { skipToast: true })
}

export function submitPasskeyRegisterApi(
  params: { dsh: string; tl: string },
  data: PasskeyRegisterRequestDTO
): Promise<ApiResponse<PasskeyRegisterResponseVO>> {
  return post<PasskeyRegisterResponseVO>('/captcha/challenge/passkey', data, { params, skipToast: true })
}

export function refreshTokenApi(): Promise<ApiResponse<void>> {
  return post<void>('/auth/refresh', undefined, { skipToast: true })
}

export function logoutApi(): Promise<ApiResponse<void>> {
  return post<void>('/auth/logout', undefined, { skipToast: true })
}

