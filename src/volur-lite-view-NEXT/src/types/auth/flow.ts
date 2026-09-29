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
  accessToken?: string
  refreshToken?: string
  tokenType?: string
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
  verificationCode?: string
  rememberMe?: boolean
}
