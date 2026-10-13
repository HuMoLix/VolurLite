export type IpRuleType = 'BLACKLIST' | 'WHITELIST'
export type IpRuleSource = 'MANUAL' | 'AUTO_TRIGGER'
export type IpRuleStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED'

export interface AdminRiskSettingsVO {
  enabled: boolean
  ipFilterEnabled: boolean
  defaultBanDurationMinutes: number
  banNoticeMessage: string
  bypassLoopback: boolean
  captchaEnabled: boolean
  captchaTolerance: number
  captchaTicketTtl: number
  captchaMaxAttempts: number
  forceCaptchaOnReset: boolean
}

export interface AdminRiskSettingsDTO {
  enabled: boolean
  ipFilterEnabled: boolean
  defaultBanDurationMinutes: number
  banNoticeMessage: string
  bypassLoopback: boolean
  captchaEnabled: boolean
  captchaTolerance: number
  captchaTicketTtl: number
  captchaMaxAttempts: number
  forceCaptchaOnReset: boolean
}

export interface AdminRiskPolicyVO {
  emailWhitelistSuffixes: string[]
  rateLimitWindow: number
  rateLimitMaxRequests: number
  velocityMediumThreshold: number
  accountFailuresMediumThreshold: number
  accountFailuresHighThreshold: number
  failuresMediumThreshold: number
  failuresHighThreshold: number
  failuresBanThreshold: number
  recoveryStepCount: number
}

export interface AdminRiskPolicyDTO {
  emailWhitelistSuffixes: string[]
  rateLimitWindow: number
  rateLimitMaxRequests: number
  velocityMediumThreshold: number
  accountFailuresMediumThreshold: number
  accountFailuresHighThreshold: number
  failuresMediumThreshold: number
  failuresHighThreshold: number
  failuresBanThreshold: number
  recoveryStepCount: number
}

export interface AdminIpRuleVO {
  id: string | number
  ip: string
  ruleType: IpRuleType
  sourceType: IpRuleSource
  reason?: string
  status: IpRuleStatus
  expiresAt?: string
  operatorId?: string | number
  operatorName?: string
  operatorIp?: string
  createdAt: string
  updatedAt: string
  isEffective: boolean
}

export interface AdminIpRuleCreateDTO {
  ip: string
  ruleType: IpRuleType
  reason?: string
  durationMinutes?: number | null
}

export interface AdminIpRuleQueryDTO {
  page?: number
  size?: number
  ip?: string
  ruleType?: IpRuleType
  sourceType?: IpRuleSource
  status?: IpRuleStatus
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export interface AdminIpDiagnosisVO {
  ip: string
  isBanned: boolean
  isWhitelisted: boolean
  isBypassed: boolean
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'BANNED' | string
  recentFailures: number
  recentVelocity: number
  banReason?: string
  banExpiresAt?: string
  banSource?: string
  deviceFingerprint?: string
  browserFamily?: string
  tlsProtocol?: string
  tlsCipher?: string
  tlsAlpn?: string
}

export interface AdminRiskStatisticsVO {
  totalBannedCount: number
  activeBannedCount: number
  totalWhitelistedCount: number
  todayAutoBannedCount: number
  todayManualBannedCount: number
}
