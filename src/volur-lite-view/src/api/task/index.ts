import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  AdminAsyncTaskVO,
  AdminAsyncTaskStatisticsVO,
  AdminAsyncTaskPageRequestDTO
} from '@/types/task'

/**
 * 获取异步任务全局统计指标看板数据
 * 对应后端: GET /api/v1/admin/tasks/stats
 */
export function getAdminTaskStatisticsApi(): Promise<ApiResponse<AdminAsyncTaskStatisticsVO>> {
  return request.get<AdminAsyncTaskStatisticsVO>('/admin/tasks/stats')
}

/**
 * 分页获取异步任务列表
 * 对应后端: GET /api/v1/admin/tasks
 */
export function getAdminTaskListApi(
  params?: AdminAsyncTaskPageRequestDTO
): Promise<ApiResponse<PageResponse<AdminAsyncTaskVO>>> {
  return request.get<PageResponse<AdminAsyncTaskVO>>('/admin/tasks', { params })
}

/**
 * 获取指定异步任务详情
 * 对应后端: GET /api/v1/admin/tasks/{id}
 */
export function getAdminTaskDetailApi(
  id: string | number
): Promise<ApiResponse<AdminAsyncTaskVO>> {
  return request.get<AdminAsyncTaskVO>(`/admin/tasks/${id}`)
}

/**
 * 取消正在排队或执行中的异步任务
 * 对应后端: POST /api/v1/admin/tasks/{id}/cancel
 */
export function cancelAdminTaskApi(
  id: string | number
): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/tasks/${id}/cancel`)
}

/**
 * 删除指定任务记录
 * 对应后端: DELETE /api/v1/admin/tasks/{id}
 */
export function deleteAdminTaskApi(
  id: string | number
): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/tasks/${id}`)
}

/**
 * 清理已结束的历史任务记录
 * 对应后端: DELETE /api/v1/admin/tasks/history
 */
export function clearAdminTaskHistoryApi(
  status?: string
): Promise<ApiResponse<void>> {
  return request.delete<void>('/admin/tasks/history', {
    params: status ? { status } : undefined
  })
}
