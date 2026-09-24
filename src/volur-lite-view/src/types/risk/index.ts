/**
 * 风控与 IP 治理模块类型定义 (对应 com.humolix.volur.verification)
 */

export type IpRuleType = 'BLACKLIST' | 'WHITELIST'
export type IpRuleSource = 'MANUAL' | 'AUTO_TRIGGER'
export type IpRuleStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED'

/** 风控全局基础设置 VO */
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

/** 风控全局基础设置更新 DTO */
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

/** 风控策略与防御阈值 VO */
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

/** 风控策略与防御阈值更新 DTO */
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

/** IP 治理规则实体 VO */
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

/** 手动添加 IP 规则 DTO */
export interface AdminIpRuleCreateDTO {
  ip: string
  ruleType: IpRuleType
  reason?: string
  durationMinutes?: number | null
}

/** IP 规则分页查询参数 DTO */
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

/** IP 实时风险画像与探针诊断 VO */
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

/** 风控与 IP 治理统计指标 VO */
export interface AdminRiskStatisticsVO {
  totalBannedCount: number
  activeBannedCount: number
  totalWhitelistedCount: number
  todayAutoBannedCount: number
  todayManualBannedCount: number
}
