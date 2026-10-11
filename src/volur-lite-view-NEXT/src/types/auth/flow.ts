export interface SessionVO {
  dsh: string
  tl?: string
  flowName: string
  flowEntry: string
  cid: number
  fp?: string
  serverTime?: number
}

export interface SessionInitParams {
  flowName: string
  flowEntry: string
  fp: string
  t: number
  data: string
}

export interface FlowResponseVO {
  dsh?: string
  tl?: string
  cid?: number
  nextStep?: string
  status: 'CHALLENGE' | 'SUCCESS' | string
  identifier?: string
  errorMessage?: string
}

export interface SignInFlowResponseVO extends FlowResponseVO {
  captchaType?: string
  username?: string
  email?: string
  displayName?: string
  avatarUrl?: string
}

export interface RecoveryFlowResponseVO extends FlowResponseVO {}

export interface SignInIdentifierRequestDTO {
  identifier: string
  dsh: string
  cid?: number
}

export interface SignInChallengeRequestDTO {
  dsh: string
  tl: string
  cid: number
  challengeType: string
  password?: string
  captchaCode?: string
  captchaTicket?: string
  verificationCode?: string
  rememberMe?: boolean
}

export interface SignInChallengeSelectRequestDTO {
  dsh: string
  tl: string
  cid: number
  targetChallengeType: string
}

export interface SignInAuthenticateRequestDTO {
  dsh: string
  tl: string
  cid: number
  rememberMe?: boolean
}

export interface ChallengeOptionVO {
  type: string
  title: string
  subtitle?: string
  icon?: string
  path?: string
}

export interface SignUpFlowResponseVO extends FlowResponseVO {
  username?: string
  email?: string
}

export interface SignUpNameRequestDTO {
  dsh: string
  tl?: string
  cid?: number
  username: string
}

export interface SignUpEmailRequestDTO {
  dsh: string
  tl: string
  cid: number
  email: string
}

export interface SignUpEmailResendRequestDTO {
  dsh: string
  tl: string
  cid: number
}

export interface SignUpEtotpChallengeRequestDTO {
  dsh: string
  tl: string
  cid: number
  code: string
}

export interface SignUpPasswordRequestDTO {
  dsh: string
  tl: string
  cid: number
  password: string
  confirmPassword: string
}

export interface SignUpOptionVO {
  type: string
  title: string
  subtitle?: string
  icon?: string
  tag?: string
  available?: boolean
}

export interface SignUpCompleteRequestDTO {
  dsh: string
  tl: string
  cid: number
  skipOptions?: boolean
  selectedOptions?: string[]
}

export interface TotpSetupVO {
  secret: string
  otpAuthUrl: string
  account: string
  issuer: string
}

export interface TotpEnableRequestDTO {
  dsh: string
  tl: string
  cid: number
  code: string
}

export interface TotpEnableResponseVO {
  success: boolean
  message?: string
}

export interface TOTPChallengeRequestDTO {
  dsh: string
  tl: string
  cid: number
  code: string
  rememberMe?: boolean
}

export interface PasskeyCreationOptionsVO {
  rp: {
    name: string
    id?: string
  }
  user: {
    id: string
    name: string
    displayName: string
  }
  challenge: string
  pubKeyCredParams: Array<{
    type: string
    alg: number
  }>
  timeout?: number
  excludeCredentials?: Array<{
    type: string
    id: string
    transports?: string[]
  }>
  authenticatorSelection?: {
    authenticatorAttachment?: string
    requireResidentKey?: boolean
    residentKey?: string
    userVerification?: string
  }
  attestation?: string
  extensions?: Record<string, any>
}

export interface PasskeyRegisterRequestDTO {
  label?: string
  credential: Record<string, any>
}

export interface PasskeyRegisterResponseVO {
  success: boolean
  credentialId?: string
}

export interface AccountChooserItemVO {
  identifier: string
  username?: string
  email?: string
  displayName?: string
  avatarUrl?: string
  sessionStatus: 'ACTIVE' | 'SIGNED_OUT' | 'DISABLED' | 'EXPIRED'
}

export interface AccountChooserProbeResponseVO {
  accounts: AccountChooserItemVO[]
}

export interface AccountChooserProbeRequestDTO {
  identifiers: string[]
}

export interface AccountChooserSelectRequestDTO {
  dsh: string
  identifier: string
}

export interface AccountChooserRemoveRequestDTO {
  identifier: string
}

export interface KnownAccount {
  identifier: string
  username: string
  email?: string
  displayName?: string
  avatarUrl?: string
  lastLoginTime?: number
}

export interface PasskeyCredentialVO {
  credentialId: string
  label?: string
  created?: string
  lastUsed?: string
  signatureCount?: number
  authenticatorTransports?: string
}

export interface TotpStatusVO {
  enabled: boolean
  updatedAt?: string
}

