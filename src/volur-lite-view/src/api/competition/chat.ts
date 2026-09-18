import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  ChatMessageVO,
  ChatMessageDTO,
  ChatMessagePageQueryDTO,
  RefereeTeamSummaryVO,
  ChatReadDTO,
  ChatUnreadCountVO
} from '@/types/competition/chat'

/**
 * 获取当前竞赛各频道未读消息统计（用户刷新页面轻量拉取，避免无谓查库）
 * 对应后端: GET /api/v1/competitions/{competitionId}/chat/unread
 */
export function getChatUnreadCountApi(
  competitionId: number | string
): Promise<ApiResponse<ChatUnreadCountVO>> {
  return request.get<ChatUnreadCountVO>(`/competitions/${competitionId}/chat/unread`)
}

/**
 * 标记指定频道消息为已读
 * 对应后端: POST /api/v1/competitions/{competitionId}/chat/read
 */
export function markChatReadApi(
  competitionId: number | string,
  data: ChatReadDTO
): Promise<ApiResponse<void>> {
  return request.post<void>(`/competitions/${competitionId}/chat/read`, data)
}

/**
 * 分页获取历史聊天记录
 * 对应后端: GET /api/v1/competitions/{competitionId}/chat/history
 */
export function getChatHistoryApi(
  competitionId: number | string,
  params?: ChatMessagePageQueryDTO
): Promise<ApiResponse<PageResponse<ChatMessageVO>>> {
  return request.get<PageResponse<ChatMessageVO>>(`/competitions/${competitionId}/chat/history`, { params })
}

/**
 * 裁判获取所有参赛队伍工单概览列表
 * 对应后端: GET /api/v1/competitions/{competitionId}/chat/referee/teams
 */
export function getRefereeTeamsApi(
  competitionId: number | string
): Promise<ApiResponse<RefereeTeamSummaryVO[]>> {
  return request.get<RefereeTeamSummaryVO[]>(`/competitions/${competitionId}/chat/referee/teams`)
}

/**
 * 发送聊天消息 (HTTP 降级通道)
 * 对应后端: POST /api/v1/competitions/{competitionId}/chat/send
 */
export function sendChatMessageApi(
  competitionId: number | string,
  data: ChatMessageDTO
): Promise<ApiResponse<ChatMessageVO>> {
  return request.post<ChatMessageVO>(`/competitions/${competitionId}/chat/send`, data)
}
