/**
 * 靶机与容器模块类型定义 (对应 com.humolix.volur.container)
 */

/** 容器启动状态枚举 */
export type InstanceStartupStatus =
  | 'QUEUED'
  | 'DOWNLOADING_IMAGE'
  | 'STARTING'
  | 'RUNNING'
  | 'FAILED'
  | 'STOPPED'

/** 靶机实例详情 VO (选手侧) */
export interface ChallengeInstanceResponseVO {
  challengeId: number | string
  accessUrl: string
  status: InstanceStartupStatus
  createdAt: string
  expiresAt: string
  remainingTimeSeconds: number
}

/** 实例启动/销毁请求 DTO */
export interface ContainerInstanceDTO {
  challengeId: number | string
  competitionId?: number | string
}

/** 管理端容器实例分页请求 DTO */
export interface AdminContainerPageRequestDTO {
  page?: number
  size?: number
  sortField?: string
  sortOrder?: string
  keyword?: string
  status?: InstanceStartupStatus
  challengeId?: number | string
  userId?: number | string
}

/** 管理端延长容器时间 DTO */
export interface AdminContainerRenewRequestDTO {
  durationMinutes: number
}

/** 管理端批量停止容器 DTO */
export interface AdminContainerBatchStopRequestDTO {
  instanceIds: (number | string)[]
}

/** 管理端容器端口映射 DTO */
export interface ContainerPortMappingDTO {
  hostPort?: number | null
  containerPort?: number | null
  protocol?: 'TCP' | 'UDP' | string
}

/** 管理端容器数据卷映射 DTO */
export interface ContainerVolumeMappingDTO {
  hostPath?: string
  containerPath?: string
  mode?: 'rw' | 'ro' | string
}

/** 管理端容器环境变量 DTO */
export interface ContainerEnvVarDTO {
  name: string
  value: string
}

/** 管理端手动启动容器 DTO */
export interface AdminContainerManualStartDTO {
  challengeId?: number | string | null
  imageName?: string
  containerName?: string
  nodeIp?: string
  ports?: ContainerPortMappingDTO[]
  volumes?: ContainerVolumeMappingDTO[]
  environments?: ContainerEnvVarDTO[]
  cpuLimit?: number | null
  memoryLimit?: number | null
  userId?: number | string
  durationMinutes?: number
  customFlag?: string
}

/** 管理端容器内执行命令 DTO */
export interface AdminContainerExecDTO {
  command: string
  workingDir?: string
  shell?: string
}

/** 管理端容器内执行命令结果 VO */
export interface AdminContainerExecResultVO {
  output?: string
  errorOutput?: string
  exitCode?: number
  workingDir?: string
  executionTimeMs?: number
  containerShortId?: string
  currentUser?: string
  prompt?: string
}

/** 管理端容器终端交互会话信息 VO */
export interface AdminContainerTerminalInfoVO {
  ready: boolean
  containerShortId?: string
  currentUser?: string
  workingDir?: string
  prompt?: string
  taskState?: string
  message?: string
}

/** 管理端容器实例视图展示 VO */
export interface AdminContainerInstanceVO {
  id: string
  challengeId?: string | null
  challengeTitle?: string
  challengeCategory?: string
  userId?: string
  username?: string
  containerId: string
  accessUrl: string
  flag: string
  status: InstanceStartupStatus
  createdAt: string
  expiresAt: string
  remainingSeconds: number
  containerType?: 'DOCKER_SWARM' | 'K3S' | string
  imageName?: string
  containerName?: string
  ports?: string
  volumes?: string
  envVars?: string
  nodeIp?: string
  nodeName?: string
  nodeRole?: string
  isNodeOffline?: boolean
}

/** 管理端容器集群统计 VO */
export interface AdminContainerStatisticsVO {
  totalInstances: number
  runningCount: number
  startingCount: number
  failedCount: number
  todayCreatedCount: number
  activeContainerType: string
}
