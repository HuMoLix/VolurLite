import { get, post, del } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type {
  SessionVO,
  SessionInitParams,
  RecoveryFlowResponseVO,
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
  TotpSetupVO,
  TotpEnableRequestDTO,
  TotpEnableResponseVO,
  TOTPChallengeRequestDTO,
  PasskeyCreationOptionsVO,
  PasskeyRegisterRequestDTO,
  PasskeyRegisterResponseVO,
  PasskeyCredentialVO,
  TotpStatusVO,
  AccountChooserProbeRequestDTO,
  AccountChooserProbeResponseVO,
  AccountChooserSelectRequestDTO,
  AccountChooserRemoveRequestDTO
} from '@/types/auth/flow'
import type {
  UsernameRecoveryOptionsVO,
  PasswordRecoveryOptionsVO,
  PasswordRecoverySelectRequestDTO,
  PasswordRecoveryEmailOtpRequestDTO,
  PasswordRecoveryResetRequestDTO
} from '@/types/auth/recovery'

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
  return post<SignInFlowResponseVO>('/captcha/challenge/emailotp', data, { skipToast: true })
}

export function resendEmailOTPChallengeApi(params: {
  dsh: string
  tl: string
}): Promise<ApiResponse<void>> {
  return post<void>('/captcha/challenge/emailotp/send', undefined, { params, skipToast: true })
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

export function fetchTotpSetupApi(params: {
  dsh: string
  tl: string
}): Promise<ApiResponse<TotpSetupVO>> {
  return get<TotpSetupVO>('/captcha/challenge/totp/setup', { params, skipToast: true })
}

export function submitTotpEnableApi(
  data: TotpEnableRequestDTO
): Promise<ApiResponse<TotpEnableResponseVO>> {
  return post<TotpEnableResponseVO>('/captcha/challenge/totp/enable', data, { skipToast: true })
}

export function submitTotpChallengeApi(
  data: TOTPChallengeRequestDTO
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/captcha/challenge/totp/verify', data, { skipToast: true })
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

export function getUsernameRecoveryOptionsApi(): Promise<ApiResponse<UsernameRecoveryOptionsVO>> {
  return get<UsernameRecoveryOptionsVO>('/auth/recovery/username/options', { skipToast: true })
}

export function submitRecoveryEmailApi(data: {
  dsh: string
  email: string
  tl?: string
  cid?: number
}): Promise<ApiResponse<RecoveryFlowResponseVO>> {
  return post<RecoveryFlowResponseVO>('/auth/recovery/username/email', data, { skipToast: true })
}

export function selectRecoveryMethodApi(data: {
  dsh: string
  method: string
  tl?: string
  cid?: number
}): Promise<ApiResponse<RecoveryFlowResponseVO>> {
  return post<RecoveryFlowResponseVO>('/auth/recovery/username/select', data, { skipToast: true })
}

export function getPasswordRecoveryOptionsApi(dsh: string): Promise<ApiResponse<PasswordRecoveryOptionsVO>> {
  return get<PasswordRecoveryOptionsVO>('/auth/recovery/password/options', { params: { dsh }, skipToast: true })
}

export function selectPasswordRecoveryMethodApi(
  data: PasswordRecoverySelectRequestDTO
): Promise<ApiResponse<RecoveryFlowResponseVO>> {
  return post<RecoveryFlowResponseVO>('/auth/recovery/password/select', data, { skipToast: true })
}

export function sendPasswordRecoveryEmailOtpApi(dsh: string, tl: string): Promise<ApiResponse<void>> {
  return post<void>('/auth/recovery/password/emailotp/send', undefined, { params: { dsh, tl }, skipToast: true })
}

export function verifyPasswordRecoveryEmailOtpApi(
  data: PasswordRecoveryEmailOtpRequestDTO
): Promise<ApiResponse<RecoveryFlowResponseVO>> {
  return post<RecoveryFlowResponseVO>('/auth/recovery/password/emailotp/verify', data, { skipToast: true })
}

export function submitPasswordResetApi(
  data: PasswordRecoveryResetRequestDTO
): Promise<ApiResponse<RecoveryFlowResponseVO>> {
  return post<RecoveryFlowResponseVO>('/auth/recovery/password/reset', data, { skipToast: true })
}

export function fetchPasskeyCredentialsApi(): Promise<ApiResponse<PasskeyCredentialVO[]>> {
  return get<PasskeyCredentialVO[]>('/captcha/challenge/pk/credentials', { skipToast: true })
}

export function deletePasskeyCredentialApi(credentialId: string): Promise<ApiResponse<void>> {
  return del<void>(`/captcha/challenge/pk/credentials/${credentialId}`, { skipToast: true })
}

export function fetchTotpStatusApi(): Promise<ApiResponse<TotpStatusVO>> {
  return get<TotpStatusVO>('/captcha/challenge/totp/status', { skipToast: true })
}

export function disableTotpApi(): Promise<ApiResponse<void>> {
  return post<void>('/captcha/challenge/totp/disable', undefined, { skipToast: true })
}


