/**
 * 系统审计与运行日志类型定义
 */

/** 系统日志条目 VO */
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

/** 系统日志统计 VO */
export interface AdminLogStatisticsVO {
  totalLogs: number
  authLogs: number
  securityLogs: number
  errorLogs: number
}

/** 系统日志查询 DTO */
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
