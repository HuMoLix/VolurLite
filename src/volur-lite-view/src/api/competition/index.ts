import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  CompetitionSummaryVO,
  CompetitionDetailVO,
  CompetitionTeamVO,
  TeamCreateDTO,
  TeamInviteRequestDTO,
  CompositeScoreboardVO
} from '@/types/competition'

/**
 * 分页获取竞赛列表
 * 对应后端: GET /api/v1/competitions
 */
export function getCompetitionListApi(params?: {
  page?: number
  size?: number
  keyword?: string
}): Promise<ApiResponse<PageResponse<CompetitionSummaryVO>>> {
  return request.get<PageResponse<CompetitionSummaryVO>>('/competitions', { params })
}

/**
 * 根据 ID 获取竞赛详情
 * 对应后端: GET /api/v1/competitions/{id}
 */
export function getCompetitionByIdApi(id: number | string): Promise<ApiResponse<CompetitionDetailVO>> {
  return request.get<CompetitionDetailVO>(`/competitions/${id}`)
}

/**
 * 根据 Slug 获取竞赛详情
 * 对应后端: GET /api/v1/competitions/by-slug/{slug}
 */
export function getCompetitionBySlugApi(slug: string): Promise<ApiResponse<CompetitionDetailVO>> {
  return request.get<CompetitionDetailVO>(`/competitions/by-slug/${slug}`)
}

/**
 * 获取竞赛关于与规则介绍
 * 对应后端: GET /api/v1/competitions/{id}/about
 */
export function getCompetitionAboutApi(id: number | string): Promise<ApiResponse<any>> {
  return request.get<any>(`/competitions/${id}/about`)
}

/**
 * 获取竞赛综合排行榜
 * 对应后端: GET /api/v1/competitions/{competitionId}/scoreboard
 */
export function getCompetitionScoreboardApi(competitionId: number | string): Promise<ApiResponse<CompositeScoreboardVO>> {
  return request.get<CompositeScoreboardVO>(`/competitions/${competitionId}/scoreboard`)
}

/**
 * 创建竞赛战队
 * 对应后端: POST /api/v1/competitions/{competitionId}/teams
 */
export function createTeamApi(
  competitionId: number | string,
  data: TeamCreateDTO
): Promise<ApiResponse<CompetitionTeamVO>> {
  return request.post<CompetitionTeamVO>(`/competitions/${competitionId}/teams`, data)
}

/**
 * 邀请队员加入战队
 * 对应后端: POST /api/v1/competitions/{competitionId}/teams/invite
 */
export function inviteTeamMemberApi(
  competitionId: number | string,
  data: TeamInviteRequestDTO
): Promise<ApiResponse<void>> {
  return request.post<void>(`/competitions/${competitionId}/teams/invite`, data)
}

/**
 * 获取当前用户在竞赛中的战队信息
 * 对应后端: GET /api/v1/competitions/{competitionId}/teams/my
 */
export function getMyTeamInfoApi(competitionId: number | string): Promise<ApiResponse<any>> {
  return request.get<any>(`/competitions/${competitionId}/teams/my`)
}

/**
 * 提交 CTF 赛题 Flag
 * 对应后端: POST /api/v1/competitions/{competitionId}/stage/ctf/challenges/{challengeId}/submit
 */
export function submitCtfFlagApi(
  competitionId: number | string,
  challengeId: number | string,
  data: { flag: string }
): Promise<ApiResponse<any>> {
  return request.post<any>(`/competitions/${competitionId}/stage/ctf/challenges/${challengeId}/submit`, data)
}
