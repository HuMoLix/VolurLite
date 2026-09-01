/**
 * 通知与实时流模块类型定义 (对应 com.humolix.volur.notification)
 */

/** 站内信通知 VO (对应 com.humolix.volur.notification.domain.vo.NotificationVO) */
export interface NotificationVO {
  id: number | string
  senderId?: number | string
  senderUsername?: string
  receiverId?: number | string
  title: string
  content: string
  type: 'SYSTEM' | 'COMPETITION' | 'TEAM' | 'AWARD'
  status: 'UNREAD' | 'READ'
  actionStatus?: 'NONE' | 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED'
  actionData?: string
  readTime?: string
  expireTime?: string
  createdAt: string
}

export type BroadcastDisplayType = 'MODAL' | 'BANNER' | 'TOAST' | 'DRAWER'

/** 广播发送请求 DTO (对应 com.humolix.volur.notification.domain.dto.BroadcastMessageDTO) */
export interface BroadcastMessageDTO {
  title: string
  content: string
  displayType: BroadcastDisplayType
  coverImageUrl?: string
  linkUrl?: string
  forcePopup?: boolean
  priority?: number
  expireTime?: string
}

/** 全局广播消息 VO (对应 com.humolix.volur.notification.domain.vo.BroadcastMessageVO) */
export interface BroadcastMessageVO {
  id: number | string
  title: string
  content: string
  displayType: BroadcastDisplayType
  coverImageUrl?: string
  linkUrl?: string
  forcePopup?: boolean
  priority?: number
  publisher?: string
  category?: 'ANNOUNCEMENT' | 'PROMOTION' | string
  publishTime?: string
  createdAt?: string
  expireTime?: string
}

/** 管理端广播分页查询请求 DTO (对应 com.humolix.volur.notification.domain.dto.AdminBroadcastPageRequestDTO) */
export interface AdminBroadcastPageRequestDTO {
  page?: number
  size?: number
  keyword?: string
  displayType?: BroadcastDisplayType | string
  category?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

/** 管理端广播统计指标 VO (对应 com.humolix.volur.notification.domain.vo.AdminBroadcastStatisticsVO) */
export interface AdminBroadcastStatisticsVO {
  totalAnnouncements: number
  totalPromotions: number
  urgentModals: number
  sseOnlineStatus?: boolean
}

/** SSE 实时事件包装模型 */
export interface SseEventMessage<T = any> {
  event: string
  data: T
  id?: string
  retry?: number
}
