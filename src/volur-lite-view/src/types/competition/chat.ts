export type ChatChannelType = 'TEAM' | 'REFEREE' | 'PRIVATE'
export type ChatMessageType = 'TEXT' | 'CODE' | 'SYSTEM'

export interface ChatMessageVO {
  id: string | number
  competitionId: string | number
  channelType: ChatChannelType
  teamId: string | number
  teamName: string
  senderId: string | number
  senderName: string
  senderRole: string
  senderAvatar: string | null
  receiverId?: string | number | null
  receiverName?: string | null
  mentionUserIds?: string | null
  content: string
  msgType: ChatMessageType
  createdAt: string
}

export interface ChatMessageDTO {
  channelType: ChatChannelType
  teamId?: string | number
  receiverId?: string | number
  mentionUserIds?: (string | number)[]
  content: string
  msgType?: ChatMessageType
}

export interface ChatMessagePageQueryDTO {
  channelType?: ChatChannelType
  teamId?: string | number
  targetUserId?: string | number
  beforeId?: string | number
  page?: number
  size?: number
}

export interface RefereeTeamSummaryVO {
  teamId: string | number
  teamName: string
  teamAvatar: string | null
  lastSenderName: string
  lastSenderRole: string
  lastContent: string
  lastCreatedAt: string
  totalMessages: number
}

export interface ChatReadDTO {
  target: string
  lastMessageId?: string | number
}

export interface ChatUnreadCountVO {
  teamUnreadCount: number
  refereeUnreadCount: number
  privateTotalUnreadCount: number
  totalUnreadCount: number
  privateDetails: Record<string, number>
}

export interface WebSocketReadyData {
  userId: string | number
  username: string
  competitionId: string | number
  teamId: string | number
  teamName: string
  isReferee: boolean
  role: string
  unread?: ChatUnreadCountVO
}
