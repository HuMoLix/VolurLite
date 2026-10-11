import { get, patch, del } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type { NotificationVO, NotificationQueryDTO } from '@/types/notification'

export function getNotificationListApi(
  params?: NotificationQueryDTO
): Promise<ApiResponse<PageResponse<NotificationVO>>> {
  return get<PageResponse<NotificationVO>>('/notifications', { params })
}

export function getUnreadNotificationCountApi(): Promise<ApiResponse<number>> {
  return get<number>('/notifications/unread-count')
}

export function getNotificationDetailApi(id: number | string): Promise<ApiResponse<NotificationVO>> {
  return get<NotificationVO>(`/notifications/${id}`)
}

export function markAllNotificationsReadApi(): Promise<ApiResponse<void>> {
  return patch<void>('/notifications/read-all')
}

export function deleteNotificationApi(id: number | string): Promise<ApiResponse<void>> {
  return del<void>(`/notifications/${id}`)
}
