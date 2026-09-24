import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  CompetitionSummaryVO,
  CompetitionDetailVO,
  CompetitionAboutVO,
  TeamSignupRequestDTO,
  TeamUpdateRequestDTO,
  TeamInviteAcceptDTO,
  TeamInvitationVO,
  MyTeamInfoVO,
  AdminCompetitionListVO,
  AdminCompetitionDetailVO,
  CompetitionCreateRequestDTO,
  CompetitionUpdateRequestDTO,
  AdminTeamListVO,
  AdminTeamStatsVO,
  AdminTeamDetailVO,
  TeamAuditRequestDTO,
  AdminTeamUpdateDTO,
  BatchTeamAuditDTO,
  BatchTeamDeleteDTO,
  CtfChallengeMountRequestDTO,
  CtfChallengeBatchMountRequestDTO,
  CtfCompetitionChallengeVO,
  CtfCompetitionChallengeDetailVO,
  CtfFlagSubmitRequestDTO,
  CompositeScoreboardVO,
  CtfScoreboardVO,
  CompetitionAnnouncementDTO,
  CompetitionAnnouncementVO,
  CompetitionWriteupVO,
  CompetitionWriteupAuditDTO,
  AdminCompetitionLogVO,
  AdminCompetitionLogStatisticsVO,
  AdminCompetitionLogQueryDTO
} from '@/types/competition'

// ==========================================
// 1. 公开端竞赛查询 API (CompetitionController)
// ==========================================

/**
 * 分页获取公开竞赛列表
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
export function getCompetitionAboutApi(id: number | string): Promise<ApiResponse<CompetitionAboutVO>> {
  return request.get<CompetitionAboutVO>(`/competitions/${id}/about`)
}

// ==========================================
// 2. 竞赛计分板 API (CompetitionScoreboardController)
// ==========================================

/**
 * 获取竞赛天梯总排行榜
 * 对应后端: GET /api/v1/competitions/{competitionId}/scoreboard
 */
export function getCompetitionScoreboardApi(
  competitionId: number | string
): Promise<ApiResponse<CompositeScoreboardVO>> {
  return request.get<CompositeScoreboardVO>(`/competitions/${competitionId}/scoreboard`)
}

/**
 * 获取 CTF 赛制专属天梯榜单
 * 对应后端: GET /api/v1/competitions/{competitionId}/scoreboard/ctf
 */
export function getCtfScoreboardApi(
  competitionId: number | string
): Promise<ApiResponse<CtfScoreboardVO>> {
  return request.get<CtfScoreboardVO>(`/competitions/${competitionId}/scoreboard/ctf`)
}
export const getCtfStageScoreboardApi = getCtfScoreboardApi

// ==========================================
// 3. 竞赛战队与报名 API (CompetitionTeamController)
// ==========================================

/**
 * 创建队伍并完成报名
 * 对应后端: POST /api/v1/competitions/{competitionId}/teams/signup
 */
export function signupTeamApi(
  competitionId: number | string,
  data: TeamSignupRequestDTO
): Promise<ApiResponse<number | string>> {
  return request.post<number | string>(`/competitions/${competitionId}/teams/signup`, data)
}

/**
 * 获取当前用户在该比赛中的队伍详情
 * 对应后端: GET /api/v1/competitions/{competitionId}/teams/me
 */
export function getMyTeamApi(
  competitionId: number | string
): Promise<ApiResponse<MyTeamInfoVO>> {
  return request.get<MyTeamInfoVO>(`/competitions/${competitionId}/teams/me`)
}

/**
 * 更新我的战队信息 (队长权限)
 * 对应后端: PUT /api/v1/competitions/{competitionId}/teams/me
 */
export function updateMyTeamApi(
  competitionId: number | string,
  data: TeamUpdateRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/competitions/${competitionId}/teams/me`, data)
}

/**
 * 解散我的战队 (队长权限)
 * 对应后端: DELETE /api/v1/competitions/{competitionId}/teams/me
 */
export function deleteMyTeamApi(
  competitionId: number | string
): Promise<ApiResponse<void>> {
  return request.delete<void>(`/competitions/${competitionId}/teams/me`)
}

/**
 * 获取当前用户在指定竞赛中的待处理组队邀请
 * 对应后端: GET /api/v1/competitions/{competitionId}/teams/invitations/me
 */
export function getMyInvitationsApi(
  competitionId: number | string
): Promise<ApiResponse<TeamInvitationVO[]>> {
  return request.get<TeamInvitationVO[]>(`/competitions/${competitionId}/teams/invitations/me`)
}

/**
 * 接受组队邀请
 * 对应后端: POST /api/v1/competitions/{competitionId}/teams/{teamId}/invite/accept
 */
export function acceptInvitationApi(
  competitionId: number | string,
  teamId: number | string,
  data?: TeamInviteAcceptDTO
): Promise<ApiResponse<void>> {
  return request.post<void>(`/competitions/${competitionId}/teams/${teamId}/invite/accept`, data)
}

/**
 * 拒绝组队邀请
 * 对应后端: POST /api/v1/competitions/{competitionId}/teams/{teamId}/invite/reject
 */
export function rejectInvitationApi(
  competitionId: number | string,
  teamId: number | string
): Promise<ApiResponse<void>> {
  return request.post<void>(`/competitions/${competitionId}/teams/${teamId}/invite/reject`)
}

/**
 * 取消队员组队邀请 (队长权限)
 * 对应后端: DELETE /api/v1/competitions/{competitionId}/teams/{teamId}/invite/cancel
 */
export function cancelInvitationApi(
  competitionId: number | string,
  teamId: number | string,
  userId: number | string
): Promise<ApiResponse<void>> {
  return request.delete<void>(`/competitions/${competitionId}/teams/${teamId}/invite/cancel`, {
    params: { userId }
  })
}

/**
 * 队长重新邀请队员
 * 对应后端: POST /api/v1/competitions/{competitionId}/teams/{teamId}/invite/reinvite
 */
export function reinviteMemberApi(
  competitionId: number | string,
  teamId: number | string,
  userId: number | string
): Promise<ApiResponse<void>> {
  return request.post<void>(`/competitions/${competitionId}/teams/${teamId}/invite/reinvite`, null, {
    params: { userId }
  })
}

// ==========================================
// 4. CTF 赛制答题与环境 API (CtfCompetitionController)
// ==========================================

/**
 * 获取竞赛 CTF 题目列表
 * 对应后端: GET /api/v1/competitions/{competitionId}/ctf/challenges
 */
export function listCtfChallengesApi(
  competitionId: number | string
): Promise<ApiResponse<CtfCompetitionChallengeVO[]>> {
  return request.get<CtfCompetitionChallengeVO[]>(`/competitions/${competitionId}/ctf/challenges`)
}

/**
 * 获取竞赛 CTF 题目详情
 * 对应后端: GET /api/v1/competitions/{competitionId}/ctf/challenges/{challengeId}
 */
export function getCtfChallengeDetailApi(
  competitionId: number | string,
  challengeId: number | string
): Promise<ApiResponse<CtfCompetitionChallengeDetailVO>> {
  return request.get<CtfCompetitionChallengeDetailVO>(
    `/competitions/${competitionId}/ctf/challenges/${challengeId}`
  )
}

/**
 * 提交 CTF 赛题 Flag
 * 对应后端: POST /api/v1/competitions/{competitionId}/ctf/challenges/{challengeId}/submit
 */
export function submitCtfFlagApi(
  competitionId: number | string,
  challengeId: number | string,
  data: CtfFlagSubmitRequestDTO
): Promise<ApiResponse<boolean>> {
  return request.post<boolean>(
    `/competitions/${competitionId}/ctf/challenges/${challengeId}/submit`,
    data
  )
}

/**
 * 启动 CTF 赛题独立靶机容器
 * 对应后端: POST /api/v1/competitions/{competitionId}/ctf/challenges/{challengeId}/instance/start
 */
export function startCtfInstanceApi(
  competitionId: number | string,
  challengeId: number | string
): Promise<ApiResponse<any>> {
  return request.post<any>(
    `/competitions/${competitionId}/ctf/challenges/${challengeId}/instance/start`
  )
}

/**
 * 销毁 CTF 赛题独立靶机容器
 * 对应后端: POST /api/v1/competitions/{competitionId}/ctf/challenges/{challengeId}/instance/stop
 */
export function stopCtfInstanceApi(
  competitionId: number | string,
  challengeId: number | string
): Promise<ApiResponse<void>> {
  return request.post<void>(
    `/competitions/${competitionId}/ctf/challenges/${challengeId}/instance/stop`
  )
}

/**
 * 延长 CTF 靶机容器租期
 * 对应后端: POST /api/v1/competitions/{competitionId}/ctf/challenges/{challengeId}/instance/extend
 */
export function extendCtfInstanceApi(
  competitionId: number | string,
  challengeId: number | string
): Promise<ApiResponse<any>> {
  return request.post<any>(
    `/competitions/${competitionId}/ctf/challenges/${challengeId}/instance/extend`
  )
}

// ==========================================
// 5. 管理端竞赛管理 API (AdminCompetitionController)
// ==========================================

/**
 * 分页获取管理端竞赛列表
 * 对应后端: GET /api/v1/admin/competitions
 */
export function getAdminCompetitionListApi(params?: {
  page?: number
  size?: number
  keyword?: string
}): Promise<ApiResponse<PageResponse<AdminCompetitionListVO>>> {
  return request.get<PageResponse<AdminCompetitionListVO>>('/admin/competitions', { params })
}

/**
 * 获取指定竞赛管理详情
 * 对应后端: GET /api/v1/admin/competitions/{id}
 */
export function getAdminCompetitionByIdApi(
  id: number | string
): Promise<ApiResponse<AdminCompetitionDetailVO>> {
  return request.get<AdminCompetitionDetailVO>(`/admin/competitions/${id}`)
}

/**
 * 创建新竞赛 (一赛一制)
 * 对应后端: POST /api/v1/admin/competitions
 */
export function createCompetitionApi(
  data: CompetitionCreateRequestDTO
): Promise<ApiResponse<number | string>> {
  return request.post<number | string>('/admin/competitions', data)
}

/**
 * 更新竞赛基本信息与规则
 * 对应后端: PUT /api/v1/admin/competitions/{id}
 */
export function updateCompetitionApi(
  id: number | string,
  data: CompetitionUpdateRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/competitions/${id}`, data)
}

/**
 * 删除竞赛
 * 对应后端: DELETE /api/v1/admin/competitions/{id}
 */
export function deleteCompetitionApi(
  id: number | string
): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/competitions/${id}`)
}

/**
 * 设置竞赛天梯榜封榜/解封状态
 * 对应后端: POST /api/v1/admin/competitions/{id}/freeze
 */
export function setFreezeScoreboardApi(
  id: number | string,
  frozen: boolean
): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/competitions/${id}/freeze`, null, {
    params: { frozen }
  })
}

/**
 * 归档竞赛
 * 对应后端: POST /api/v1/admin/competitions/{id}/archive
 */
export function archiveCompetitionApi(id: number | string): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/competitions/${id}/archive`)
}

/**
 * 取消归档竞赛
 * 对应后端: POST /api/v1/admin/competitions/{id}/unarchive
 */
export function unarchiveCompetitionApi(id: number | string): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/competitions/${id}/unarchive`)
}

// ==========================================
// 6. 管理端战队审核 API (AdminCompetitionTeamController)
// ==========================================

/**
 * 分页查询参赛队伍列表
 * 对应后端: GET /api/v1/admin/competitions/{competitionId}/teams
 */
export function listAdminTeamsApi(
  competitionId: number | string,
  params?: {
    page?: number
    size?: number
    status?: string
    keyword?: string
  }
): Promise<ApiResponse<PageResponse<AdminTeamListVO>>> {
  return request.get<PageResponse<AdminTeamListVO>>(
    `/admin/competitions/${competitionId}/teams`,
    { params }
  )
}

/**
 * 获取指定竞赛报名队伍统计概览
 * 对应后端: GET /api/v1/admin/competitions/{competitionId}/teams/stats
 */
export function getAdminTeamStatsApi(
  competitionId: number | string
): Promise<ApiResponse<AdminTeamStatsVO>> {
  return request.get<AdminTeamStatsVO>(`/admin/competitions/${competitionId}/teams/stats`)
}

/**
 * 获取指定参赛队伍管理详情
 * 对应后端: GET /api/v1/admin/competitions/{competitionId}/teams/{teamId}
 */
export function getAdminTeamDetailApi(
  competitionId: number | string,
  teamId: number | string
): Promise<ApiResponse<AdminTeamDetailVO>> {
  return request.get<AdminTeamDetailVO>(`/admin/competitions/${competitionId}/teams/${teamId}`)
}

/**
 * 审核参赛队伍资质 (通过/驳回)
 * 对应后端: POST /api/v1/admin/competitions/{competitionId}/teams/{teamId}/audit
 */
export function auditAdminTeamApi(
  competitionId: number | string,
  teamId: number | string,
  data: TeamAuditRequestDTO
): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/competitions/${competitionId}/teams/${teamId}/audit`, data)
}

/**
 * 管理员修改队伍基本信息（如修改队伍名称）
 * 对应后端: PUT /api/v1/admin/competitions/{competitionId}/teams/{teamId}
 */
export function updateAdminTeamApi(
  competitionId: number | string,
  teamId: number | string,
  data: AdminTeamUpdateDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/competitions/${competitionId}/teams/${teamId}`, data)
}

/**
 * 批量审核参赛队伍资质
 * 对应后端: POST /api/v1/admin/competitions/{competitionId}/teams/batch-audit
 */
export function batchAuditAdminTeamsApi(
  competitionId: number | string,
  data: BatchTeamAuditDTO
): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/competitions/${competitionId}/teams/batch-audit`, data)
}

/**
 * 批量删除参赛队伍
 * 对应后端: POST /api/v1/admin/competitions/{competitionId}/teams/batch-delete
 */
export function batchDeleteAdminTeamsApi(
  competitionId: number | string,
  data: BatchTeamDeleteDTO
): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/competitions/${competitionId}/teams/batch-delete`, data)
}

/**
 * 删除参赛队伍
 * 对应后端: DELETE /api/v1/admin/competitions/{competitionId}/teams/{teamId}
 */
export function deleteAdminTeamApi(
  competitionId: number | string,
  teamId: number | string
): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/competitions/${competitionId}/teams/${teamId}`)
}

// ==========================================
// 7. 管理端 CTF 赛题挂载 API (AdminCtfMountController)
// ==========================================

/**
 * 获取比赛已挂载的 CTF 赛题列表
 * 对应后端: GET /api/v1/admin/competitions/{competitionId}/ctf/mount
 */
export function listMountedCtfChallengesApi(
  competitionId: number | string
): Promise<ApiResponse<CtfCompetitionChallengeVO[]>> {
  return request.get<CtfCompetitionChallengeVO[]>(
    `/admin/competitions/${competitionId}/ctf/mount`
  )
}

/**
 * 单个挂载题库题目到竞赛
 * 对应后端: POST /api/v1/admin/competitions/{competitionId}/ctf/mount
 */
export function mountCtfChallengeApi(
  competitionId: number | string,
  data: CtfChallengeMountRequestDTO
): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/competitions/${competitionId}/ctf/mount`, data)
}

/**
 * 批量挂载题库题目到竞赛
 * 对应后端: POST /api/v1/admin/competitions/{competitionId}/ctf/mount/batch
 */
export function batchMountCtfChallengesApi(
  competitionId: number | string,
  data: CtfChallengeBatchMountRequestDTO
): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/competitions/${competitionId}/ctf/mount/batch`, data)
}

/**
 * 更新已挂载赛题配置
 * 对应后端: PUT /api/v1/admin/competitions/{competitionId}/ctf/mount/{competitionChallengeId}
 */
export function updateMountedCtfChallengeApi(
  competitionId: number | string,
  competitionChallengeId: number | string,
  data: CtfChallengeMountRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(
    `/admin/competitions/${competitionId}/ctf/mount/${competitionChallengeId}`,
    data
  )
}

/**
 * 从竞赛中卸载赛题
 * 对应后端: DELETE /api/v1/admin/competitions/{competitionId}/ctf/mount/{competitionChallengeId}
 */
export function unmountCtfChallengeApi(
  competitionId: number | string,
  competitionChallengeId: number | string
): Promise<ApiResponse<void>> {
  return request.delete<void>(
    `/admin/competitions/${competitionId}/ctf/mount/${competitionChallengeId}`
  )
}

/**
 * 获取比赛专属公告列表（前台/竞技场使用）
 * 对应后端: GET /api/v1/competitions/{competitionId}/announcements
 */
export function listCompetitionAnnouncementsApi(
  competitionId: number | string
): Promise<ApiResponse<CompetitionAnnouncementVO[]>> {
  return request.get<CompetitionAnnouncementVO[]>(
    `/competitions/${competitionId}/announcements`
  )
}

// ==========================================
// 8. 管理端比赛专属公告 API (AdminCompetitionAnnouncementController)
// ==========================================

/**
 * 获取比赛所有专属公告列表
 * 对应后端: GET /api/v1/admin/competitions/{competitionId}/announcements
 */
export function listAdminCompetitionAnnouncementsApi(
  competitionId: number | string
): Promise<ApiResponse<CompetitionAnnouncementVO[]>> {
  return request.get<CompetitionAnnouncementVO[]>(
    `/admin/competitions/${competitionId}/announcements`
  )
}

/**
 * 发布比赛专属公告
 * 对应后端: POST /api/v1/admin/competitions/{competitionId}/announcements
 */
export function createAdminCompetitionAnnouncementApi(
  competitionId: number | string,
  data: CompetitionAnnouncementDTO
): Promise<ApiResponse<CompetitionAnnouncementVO>> {
  return request.post<CompetitionAnnouncementVO>(
    `/admin/competitions/${competitionId}/announcements`,
    data
  )
}

/**
 * 更新比赛专属公告
 * 对应后端: PUT /api/v1/admin/competitions/{competitionId}/announcements/{announcementId}
 */
export function updateAdminCompetitionAnnouncementApi(
  competitionId: number | string,
  announcementId: number | string,
  data: CompetitionAnnouncementDTO
): Promise<ApiResponse<CompetitionAnnouncementVO>> {
  return request.put<CompetitionAnnouncementVO>(
    `/admin/competitions/${competitionId}/announcements/${announcementId}`,
    data
  )
}

/**
 * 置顶或取消置顶比赛公告
 * 对应后端: POST /api/v1/admin/competitions/{competitionId}/announcements/{announcementId}/pin
 */
export function togglePinAdminCompetitionAnnouncementApi(
  competitionId: number | string,
  announcementId: number | string,
  pinned: boolean
): Promise<ApiResponse<void>> {
  return request.post<void>(
    `/admin/competitions/${competitionId}/announcements/${announcementId}/pin`,
    null,
    { params: { pinned } }
  )
}

/**
 * 删除比赛公告
 * 对应后端: DELETE /api/v1/admin/competitions/{competitionId}/announcements/{announcementId}
 */
export function deleteAdminCompetitionAnnouncementApi(
  competitionId: number | string,
  announcementId: number | string
): Promise<ApiResponse<void>> {
  return request.delete<void>(
    `/admin/competitions/${competitionId}/announcements/${announcementId}`
  )
}

// ==========================================
// 9. 管理端比赛题解 WP API (AdminCompetitionWriteupController)
// ==========================================

/**
 * 获取比赛已提交题解 WP 列表
 * 对应后端: GET /api/v1/admin/competitions/{competitionId}/writeups
 */
export function listAdminCompetitionWriteupsApi(
  competitionId: number | string,
  params?: { keyword?: string }
): Promise<ApiResponse<CompetitionWriteupVO[]>> {
  return request.get<CompetitionWriteupVO[]>(
    `/admin/competitions/${competitionId}/writeups`,
    { params }
  )
}

/**
 * 获取单个题解 WP 详情
 * 对应后端: GET /api/v1/admin/competitions/{competitionId}/writeups/{writeupId}
 */
export function getAdminCompetitionWriteupDetailApi(
  competitionId: number | string,
  writeupId: number | string
): Promise<ApiResponse<CompetitionWriteupVO>> {
  return request.get<CompetitionWriteupVO>(
    `/admin/competitions/${competitionId}/writeups/${writeupId}`
  )
}

/**
 * 审核或评分队伍题解 WP
 * 对应后端: PUT /api/v1/admin/competitions/{competitionId}/writeups/{writeupId}/audit
 */
export function auditAdminCompetitionWriteupApi(
  competitionId: number | string,
  writeupId: number | string,
  data: CompetitionWriteupAuditDTO
): Promise<ApiResponse<CompetitionWriteupVO>> {
  return request.put<CompetitionWriteupVO>(
    `/admin/competitions/${competitionId}/writeups/${writeupId}/audit`,
    data
  )
}

/**
 * 删除队伍题解 WP
 * 对应后端: DELETE /api/v1/admin/competitions/{competitionId}/writeups/{writeupId}
 */
export function deleteAdminCompetitionWriteupApi(
  competitionId: number | string,
  writeupId: number | string
): Promise<ApiResponse<void>> {
  return request.delete<void>(
    `/admin/competitions/${competitionId}/writeups/${writeupId}`
  )
}

// ==========================================
// 10. 管理端比赛运行操作日志 API (AdminCompetitionLogController)
// ==========================================

/**
 * 分页获取单场比赛运行操作日志列表
 * 对应后端: GET /api/v1/admin/competitions/{competitionId}/logs
 */
export function getAdminCompetitionLogListApi(
  competitionId: number | string,
  params?: AdminCompetitionLogQueryDTO
): Promise<ApiResponse<PageResponse<AdminCompetitionLogVO>>> {
  return request.get<PageResponse<AdminCompetitionLogVO>>(
    `/admin/competitions/${competitionId}/logs`,
    { params }
  )
}

/**
 * 获取单场比赛操作日志统计数据
 * 对应后端: GET /api/v1/admin/competitions/{competitionId}/logs/stats
 */
export function getAdminCompetitionLogStatisticsApi(
  competitionId: number | string
): Promise<ApiResponse<AdminCompetitionLogStatisticsVO>> {
  return request.get<AdminCompetitionLogStatisticsVO>(
    `/admin/competitions/${competitionId}/logs/stats`
  )
}

/**
 * 获取指定比赛日志详细记录
 * 对应后端: GET /api/v1/admin/competitions/{competitionId}/logs/{id}
 */
export function getAdminCompetitionLogDetailApi(
  competitionId: number | string,
  id: number | string
): Promise<ApiResponse<AdminCompetitionLogVO>> {
  return request.get<AdminCompetitionLogVO>(
    `/admin/competitions/${competitionId}/logs/${id}`
  )
}


