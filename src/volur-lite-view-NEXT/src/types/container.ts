export type InstanceStartupStatus =
  | 'QUEUED'
  | 'DOWNLOADING_IMAGE'
  | 'STARTING'
  | 'RUNNING'
  | 'FAILED'
  | 'STOPPED'

export interface ChallengeInstanceResponseVO {
  challengeId: number | string
  accessUrl: string
  status: InstanceStartupStatus
  createdAt: string
  expiresAt: string
  remainingTimeSeconds: number
}

export interface ContainerInstanceDTO {
  challengeId: number | string
  competitionId?: number | string
}

export interface AdminContainerPageRequestDTO {
  page?: number
  size?: number
  sortField?: string
  sortOrder?: string
  keyword?: string
  status?: InstanceStartupStatus
  challengeId?: number | string
  stageId?: number | string
  userId?: number | string
}

export interface AdminContainerRenewRequestDTO {
  durationMinutes: number
}

export interface AdminContainerBatchStopRequestDTO {
  instanceIds: (number | string)[]
}

export interface ContainerPortMappingDTO {
  hostPort?: number | null
  containerPort?: number | null
  protocol?: 'TCP' | 'UDP' | string
}

export interface ContainerVolumeMappingDTO {
  hostPath?: string
  containerPath?: string
  mode?: 'rw' | 'ro' | string
}

export interface ContainerEnvVarDTO {
  name: string
  value: string
}

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
  outboundEnabled?: boolean
}

export interface AdminContainerExecDTO {
  command: string
  workingDir?: string
  shell?: string
}

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

export interface AdminContainerTerminalInfoVO {
  ready: boolean
  containerShortId?: string
  currentUser?: string
  workingDir?: string
  prompt?: string
  taskState?: string
  message?: string
}

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

export interface AdminContainerStatisticsVO {
  totalInstances: number
  runningCount: number
  startingCount: number
  failedCount: number
  todayCreatedCount: number
  activeContainerType: string
}
