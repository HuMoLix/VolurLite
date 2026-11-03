import { get, post, patch, del } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  NotificationVO,
  NotificationQueryDTO,
  BroadcastMessageVO,
  BroadcastMessageDTO,
  AdminBroadcastPageRequestDTO,
  AdminBroadcastStatisticsVO
} from '@/types/notification'

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

export function getBroadcastListApi(
  params?: AdminBroadcastPageRequestDTO
): Promise<ApiResponse<PageResponse<BroadcastMessageVO>>> {
  return get<PageResponse<BroadcastMessageVO>>('/broadcasts', { params })
}

export function getBroadcastDetailApi(id: number | string): Promise<ApiResponse<BroadcastMessageVO>> {
  return get<BroadcastMessageVO>(`/broadcasts/${id}`)
}

export function getAdminBroadcastStatisticsApi(): Promise<ApiResponse<AdminBroadcastStatisticsVO>> {
  return get<AdminBroadcastStatisticsVO>('/admin/broadcasts/stats')
}

export function getAdminBroadcastListApi(
  params?: AdminBroadcastPageRequestDTO
): Promise<ApiResponse<PageResponse<BroadcastMessageVO>>> {
  return get<PageResponse<BroadcastMessageVO>>('/admin/broadcasts', { params })
}

export function publishAnnouncementApi(data: BroadcastMessageDTO): Promise<ApiResponse<void>> {
  return post<void>('/admin/broadcasts/announcement', data)
}

export function publishPromotionApi(data: BroadcastMessageDTO): Promise<ApiResponse<void>> {
  return post<void>('/admin/broadcasts/promotion', data)
}

export function deleteAdminBroadcastApi(id: number | string): Promise<ApiResponse<void>> {
  return del<void>(`/admin/broadcasts/${id}`)
}
