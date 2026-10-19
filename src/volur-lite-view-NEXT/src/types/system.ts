export interface StorageResponseVO {
  fileName: string
  filePath: string
  fileSize: number
  sha256?: string
}

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

export interface SystemSettingVO {
  group?: string
  key: string
  value: string
  valueType?: string
  isPublic?: boolean
  description?: string
}

export interface SystemSettingUpdateDTO {
  key: string
  value: string
}
