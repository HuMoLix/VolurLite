import type {
  CaptchaTicketVO,
  CaptchaVerifyRequestDTO,
  CaptchaResponseVO,
  CaptchaVerifyDTO
} from '@/types/verification'

export type CaptchaDisplayType = 'NONE' | 'IMAGE' | 'SLIDER' | 'EMAIL' | 'SMS'

export interface VolurCaptchaProps {
  scene?: string
  sceneId?: string
  account?: string
  type?: CaptchaDisplayType | 'ADAPTIVE'
  autoLoad?: boolean
  headless?: boolean
}

export interface VolurCaptchaExpose {
  getPayload: () => Promise<CaptchaVerifyRequestDTO>
  verifyBehavior: (customScene?: string) => Promise<CaptchaTicketVO>
  handleFailure: (err: any) => void
  reset: () => void
  refresh: () => Promise<void>
  captchaType: CaptchaDisplayType
  mode: CaptchaDisplayType
}

export type { CaptchaTicketVO, CaptchaVerifyRequestDTO, CaptchaResponseVO, CaptchaVerifyDTO }
