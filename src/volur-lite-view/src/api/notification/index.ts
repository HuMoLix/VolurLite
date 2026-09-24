import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  NotificationVO,
  BroadcastMessageVO,
  BroadcastMessageDTO,
  AdminBroadcastPageRequestDTO,
  AdminBroadcastStatisticsVO
} from '@/types/notification'

/**
 * 分页查询当前用户通知列表
 * 对应后端: GET /api/v1/notifications
 */
export function getNotificationListApi(params?: {
  page?: number
  size?: number
  keyword?: string
  status?: string
  unreadOnly?: boolean
  type?: string
}): Promise<ApiResponse<PageResponse<NotificationVO>>> {
  return request.get<PageResponse<NotificationVO>>('/notifications', { params })
}

/**
 * 获取当前用户未读通知数
 * 对应后端: GET /api/v1/notifications/unread-count
 */
export function getUnreadNotificationCountApi(): Promise<ApiResponse<number>> {
  return request.get<number>('/notifications/unread-count')
}

/**
 * 获取指定通知详情并自动标为已读
 * 对应后端: GET /api/v1/notifications/{id}
 */
export function getNotificationDetailApi(id: number | string): Promise<ApiResponse<NotificationVO>> {
  return request.get<NotificationVO>(`/notifications/${id}`)
}

/**
 * 一键全部标记为已读
 * 对应后端: PATCH /api/v1/notifications/read-all
 */
export function markAllNotificationsReadApi(): Promise<ApiResponse<void>> {
  return request.patch<void>('/notifications/read-all')
}

/**
 * 删除指定通知
 * 对应后端: DELETE /api/v1/notifications/{id}
 */
export function deleteNotificationApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/notifications/${id}`)
}

/**
 * 前台用户：分页获取全站公告列表
 * 对应后端: GET /api/v1/broadcasts
 */
export function getBroadcastListApi(
  params?: AdminBroadcastPageRequestDTO
): Promise<ApiResponse<PageResponse<BroadcastMessageVO>>> {
  return request.get<PageResponse<BroadcastMessageVO>>('/broadcasts', { params })
}

/**
 * 前台用户：获取指定公告详情
 * 对应后端: GET /api/v1/broadcasts/{id}
 */
export function getBroadcastDetailApi(id: number | string): Promise<ApiResponse<BroadcastMessageVO>> {
  return request.get<BroadcastMessageVO>(`/broadcasts/${id}`)
}

/**
 * 管理端：获取全站广播统计指标
 * 对应后端: GET /api/v1/admin/broadcasts/stats
 */
export function getAdminBroadcastStatisticsApi(): Promise<ApiResponse<AdminBroadcastStatisticsVO>> {
  return request.get<AdminBroadcastStatisticsVO>('/admin/broadcasts/stats')
}

/**
 * 管理端：分页获取广播消息列表
 * 对应后端: GET /api/v1/admin/broadcasts
 */
export function getAdminBroadcastListApi(
  params?: AdminBroadcastPageRequestDTO
): Promise<ApiResponse<PageResponse<BroadcastMessageVO>>> {
  return request.get<PageResponse<BroadcastMessageVO>>('/admin/broadcasts', { params })
}

/**
 * 管理端：发布全站系统公告
 * 对应后端: POST /api/v1/admin/broadcasts/announcement
 */
export function publishAnnouncementApi(data: BroadcastMessageDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/broadcasts/announcement', data)
}

/**
 * 管理端：发布运营活动与广告弹窗
 * 对应后端: POST /api/v1/admin/broadcasts/promotion
 */
export function publishPromotionApi(data: BroadcastMessageDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/broadcasts/promotion', data)
}

/**
 * 管理端：删除/撤回指定广播通知
 * 对应后端: DELETE /api/v1/admin/broadcasts/{id}
 */
export function deleteAdminBroadcastApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/broadcasts/${id}`)
}
