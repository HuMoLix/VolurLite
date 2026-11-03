export type { RecoveryFlowResponseVO } from './flow'

export interface RecoveryMethodVO {
  method: 'EMAIL' | 'PHONE'
  name: string
  description: string
  icon?: string
}

export interface UsernameRecoveryOptionsVO {
  methods: RecoveryMethodVO[]
}

export interface PasswordRecoveryOptionsVO {
  methods: RecoveryMethodVO[]
  maskedEmail?: string
  maskedPhone?: string
}

export interface PasswordRecoverySelectRequestDTO {
  dsh: string
  tl?: string
  cid?: number
  method: string
}

export interface PasswordRecoveryEmailOtpRequestDTO {
  dsh: string
  tl: string
  cid?: number
  code: string
}

export interface PasswordRecoveryResetRequestDTO {
  dsh: string
  tl: string
  cid?: number
  password: string
  confirmPassword: string
}
