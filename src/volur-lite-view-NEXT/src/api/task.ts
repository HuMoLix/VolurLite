import { get, post, del } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  AdminAsyncTaskVO,
  AdminAsyncTaskStatisticsVO,
  AdminAsyncTaskPageRequestDTO
} from '@/types/task'

export function getAdminTaskStatisticsApi(): Promise<ApiResponse<AdminAsyncTaskStatisticsVO>> {
  return get<AdminAsyncTaskStatisticsVO>('/admin/tasks/stats')
}

export function getAdminTaskListApi(
  params?: AdminAsyncTaskPageRequestDTO
): Promise<ApiResponse<PageResponse<AdminAsyncTaskVO>>> {
  return get<PageResponse<AdminAsyncTaskVO>>('/admin/tasks', { params })
}

export function getAdminTaskDetailApi(
  id: string | number
): Promise<ApiResponse<AdminAsyncTaskVO>> {
  return get<AdminAsyncTaskVO>(`/admin/tasks/${id}`)
}

export function cancelAdminTaskApi(
  id: string | number
): Promise<ApiResponse<void>> {
  return post<void>(`/admin/tasks/${id}/cancel`)
}

export function deleteAdminTaskApi(
  id: string | number
): Promise<ApiResponse<void>> {
  return del<void>(`/admin/tasks/${id}`)
}

export function clearAdminTaskHistoryApi(
  status?: string
): Promise<ApiResponse<void>> {
  return del<void>('/admin/tasks/history', {
    params: status ? { status } : undefined
  })
}
