import { get, post } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type {
  SessionVO,
  SessionInitParams,
  SignInFlowResponseVO,
  SignInIdentifierRequestDTO,
  SignInChallengeSelectRequestDTO,
  SignInAuthenticateRequestDTO,
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
  PasskeyRegisterResponseVO,
  AccountChooserProbeRequestDTO,
  AccountChooserProbeResponseVO,
  AccountChooserSelectRequestDTO,
  AccountChooserRemoveRequestDTO
} from '@/types/auth/flow'

export function initSessionApi(params: SessionInitParams): Promise<ApiResponse<SessionVO>> {
  return get<SessionVO>('/session/initSess', { params, skipToast: true })
}

export function submitSignInIdentifierApi(
  data: SignInIdentifierRequestDTO
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/auth/signin/identifier', data, { skipToast: true })
}

export function probeAccountChooserApi(
  data: AccountChooserProbeRequestDTO
): Promise<ApiResponse<AccountChooserProbeResponseVO>> {
  return post<AccountChooserProbeResponseVO>('/auth/signin/accountchooser/probe', data, { skipToast: true })
}

export function selectAccountChooserApi(
  data: AccountChooserSelectRequestDTO
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/auth/signin/accountchooser/select', data, { skipToast: true })
}

export function removeAccountChooserApi(
  data: AccountChooserRemoveRequestDTO
): Promise<ApiResponse<void>> {
  return post<void>('/auth/signin/accountchooser/remove', data, { skipToast: true })
}

export function submitDynamicChallengeApi(
  step: string,
  data: Record<string, any>
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>(`/captcha/challenge/${step}`, data, { skipToast: true })
}

export function submitPasswordChallengeApi(data: {
  dsh: string
  tl: string
  cid?: number
  password: string
  rememberMe?: boolean
}): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/captcha/challenge/pwd', data, { skipToast: true })
}

export function submitEmailOTPChallengeApi(data: {
  dsh: string
  tl: string
  cid?: number
  code: string
  rememberMe?: boolean
}): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/captcha/challenge/etotp', data, { skipToast: true })
}

export function resendEmailOTPChallengeApi(params: {
  dsh: string
  tl: string
}): Promise<ApiResponse<void>> {
  return post<void>('/captcha/challenge/etotp/send', undefined, { params, skipToast: true })
}

export function submitSMSOTPChallengeApi(data: {
  dsh: string
  tl: string
  cid?: number
  code: string
  rememberMe?: boolean
}): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/captcha/challenge/smsotp', data, { skipToast: true })
}

export function selectSignInChallengeApi(
  data: SignInChallengeSelectRequestDTO
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/captcha/challenge/select', data, { skipToast: true })
}

export function submitSignInAuthenticateApi(
  data: SignInAuthenticateRequestDTO
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/auth/signin/authenticate', data, { skipToast: true })
}

export function fetchChallengeSelectionApi(params?: {
  dsh?: string
  currentType?: string
}): Promise<ApiResponse<ChallengeOptionVO[]>> {
  return get<ChallengeOptionVO[]>('/captcha/challenge/selection', { params, skipToast: true })
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
  return get<PasskeyCreationOptionsVO>('/captcha/challenge/pk', { params, skipToast: true })
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
  return get('/captcha/challenge/pk/authenticators', { skipToast: true })
}

export function submitPasskeyRegisterApi(
  params: { dsh: string; tl: string },
  data: PasskeyRegisterRequestDTO
): Promise<ApiResponse<PasskeyRegisterResponseVO>> {
  return post<PasskeyRegisterResponseVO>('/captcha/challenge/pk', data, { params, skipToast: true })
}

export function fetchPasskeyAssertionOptionsApi(params: {
  dsh: string
  tl: string
}): Promise<ApiResponse<any>> {
  return get<any>('/captcha/challenge/pk/assertion-options', { params, skipToast: true })
}

export function submitPasskeyChallengeApi(data: {
  dsh: string
  tl: string
  cid?: number
  credential: any
  rememberMe?: boolean
}): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/captcha/challenge/pk', data, { skipToast: true })
}

export function refreshTokenApi(): Promise<ApiResponse<void>> {
  return post<void>('/auth/refresh', undefined, { skipToast: true })
}

export function logoutApi(): Promise<ApiResponse<void>> {
  return post<void>('/auth/logout', undefined, { skipToast: true })
}

