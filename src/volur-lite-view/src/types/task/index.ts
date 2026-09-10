/**
 * 异步任务中心类型定义 (对应后端 com.humolix.volur.system)
 */

export type AsyncTaskType =
  | 'IMAGE_TRANSFER'
  | 'IMAGE_PULL'
  | 'IMAGE_BACKUP'
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

/** 异步任务详情视图对象 */
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

/** 异步任务提交凭据视图对象 */
export interface AdminAsyncTaskSubmitVO {
  taskId: string | number
  taskName: string
  taskType: AsyncTaskType
  status: AsyncTaskStatus
}

/** 异步任务分页列表查询请求参数 */
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

/** 异步任务全局统计指标视图对象 */
export interface AdminAsyncTaskStatisticsVO {
  totalCount: number
  runningCount: number
  pendingCount: number
  successCount: number
  failedCount: number
}
