export interface AdminLogVO {
  id: string | number
  level: 'INFO' | 'WARN' | 'ERROR'
  module: string
  action: string
  operator: string
  operatorIp?: string
  requestUri?: string
  requestMethod?: string
  status: boolean
  durationMs?: number
  params?: string
  result?: string
  errorMessage?: string
  userAgent?: string
  createdAt: string
}

export interface AdminLogStatisticsVO {
  totalLogs: number
  authLogs: number
  securityLogs: number
  errorLogs: number
}

export interface AdminLogQueryDTO {
  page?: number
  size?: number
  keyword?: string
  level?: string
  module?: string
  status?: boolean
  startTime?: string
  endTime?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}
