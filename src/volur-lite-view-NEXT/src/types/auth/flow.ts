export interface FlowInitVO {
  dsh: string
  flowName: string
  flowEntry: string
  cid: number
  fp?: string
  serverTime?: number
}

export interface FlowInitParams {
  flowName?: string
  flowEntry?: string
  fp?: string
  t?: number
  dsh?: string
}

export interface SignInFlowResponseVO {
  dsh?: string
  tl?: string
  cid?: number
  nextStep?: string
  status: 'CHALLENGE' | 'SUCCESS'
  identifier?: string
  errorMessage?: string
}

export interface SignInIdentifierRequestDTO {
  identifier: string
  dsh: string
  cid?: number
  deviceFingerprint?: string
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

export interface ChallengeOptionVO {
  type: string
  title: string
  subtitle?: string
  icon?: string
  path?: string
}

export interface SignUpFlowResponseVO {
  dsh?: string
  tl?: string
  cid?: number
  nextStep?: string
  status: 'CHALLENGE' | 'SUCCESS'
  username?: string
  email?: string
  errorMessage?: string
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

