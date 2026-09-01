/**
 * 系统模块类型定义 (对应 com.humolix.volur.system)
 */

/** 系统运行健康与底层环境概览 VO */
export interface AdminSystemOverviewVO {
  kernelVersion: string
  springBootVersion: string
  javaVersion: string
  runtimeEnv: string
  osInfo: string
  redisStatus: 'CONNECTED' | 'DISCONNECTED' | string
  redisLatencyMs: number
  sseStatus: 'ONLINE' | 'OFFLINE' | string
  uptimeSeconds: number
  heapMemoryUsedMb: number
  heapMemoryMaxMb: number
}

/** 系统配置项 VO */
export interface SystemSettingVO {
  group?: string
  key: string
  value: string
  valueType?: string
  isPublic?: boolean
  description?: string
}

/** 系统配置更新请求 DTO */
export interface SystemSettingUpdateDTO {
  key: string
  value: string
}

/** 文件存储上传响应 VO (对应 com.humolix.volur.system.domain.vo.StorageResponseVO) */
export interface StorageResponseVO {
  fileName: string
  filePath: string
  fileSize: number
  sha256?: string
}


