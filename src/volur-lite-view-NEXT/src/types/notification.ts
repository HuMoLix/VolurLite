export type NotificationType = 'SYSTEM' | 'COMPETITION' | 'TEAM' | 'AWARD' | string
export type NotificationStatus = 'UNREAD' | 'READ' | string
export type NotificationActionStatus = 'NONE' | 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED' | string

export interface NotificationVO {
  id: number | string
  senderId?: number | string
  senderUsername?: string
  receiverId?: number | string
  title: string
  content: string
  type: NotificationType
  status: NotificationStatus
  actionStatus?: NotificationActionStatus
  actionData?: string
  expireTime?: string
  readTime?: string
  createdAt: string
}

export interface NotificationQueryDTO {
  page?: number
  size?: number
  keyword?: string
  type?: NotificationType
  status?: NotificationStatus
  actionStatus?: NotificationActionStatus
  unreadOnly?: boolean
}

export type BroadcastDisplayType = 'MODAL' | 'BANNER' | 'TOAST' | 'DRAWER'

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
  category?: string
  publishTime?: string
  createdAt?: string
  expireTime?: string
}

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

export interface AdminBroadcastPageRequestDTO {
  page?: number
  size?: number
  keyword?: string
  displayType?: BroadcastDisplayType | string
  category?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export interface AdminBroadcastStatisticsVO {
  totalAnnouncements: number
  totalPromotions: number
  urgentModals: number
  sseOnlineStatus?: boolean
}

