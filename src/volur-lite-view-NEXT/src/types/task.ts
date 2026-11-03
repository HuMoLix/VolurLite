export type AsyncTaskType =
  | 'IMAGE_TRANSFER'
  | 'IMAGE_PULL'
  | 'IMAGE_BACKUP'
  | 'IMAGE_IMPORT'
  | 'IMAGE_SYNC'
  | 'CONTAINER_BATCH_STOP'
  | 'EMAIL_BATCH_SEND'
  | 'SYSTEM_CLEANUP'
  | 'OTHER'

export type AsyncTaskStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCESS'
  | 'FAILED'
  | 'CANCELLED'

export interface AdminAsyncTaskVO {
  id: string | number
  taskName: string
  taskType: AsyncTaskType
  taskTypeDesc?: string
  status: AsyncTaskStatus
  statusDesc?: string
  progress: number
  statusMessage?: string
  errorMessage?: string
  parameters?: string
  resultData?: string
  operatorId?: string | number
  operatorName?: string
  createdAt: string
  startedAt?: string
  finishedAt?: string
  executionDurationMs?: number
}

export interface AdminAsyncTaskSubmitVO {
  taskId: string | number
  taskName: string
  taskType: AsyncTaskType
  status: AsyncTaskStatus
}

export interface AdminAsyncTaskPageRequestDTO {
  page?: number
  size?: number
  keyword?: string
  taskType?: AsyncTaskType
  status?: AsyncTaskStatus
  startTime?: string
  endTime?: string
  sortField?: string
  sortOrder?: 'ASC' | 'DESC'
}

export interface AdminAsyncTaskStatisticsVO {
  totalCount: number
  runningCount: number
  pendingCount: number
  successCount: number
  failedCount: number
}
