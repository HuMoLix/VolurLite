import { get, post, put, del, download } from '@/utils/request'
import type { ApiResponse, PageResponse, SliceResponse } from '@/types/common/api'
import type {
  CompetitionSummaryVO,
  CompetitionDetailVO,
  CompetitionAboutVO,
  CompetitionAnnouncementVO,
  CompetitionAnnouncementDTO,
  CompetitionQueryDTO,
  TheoryPaperExamVO,
  TheoryPaperSubmitRequestDTO,
  TheoryPaperResultVO,
  TheoryCheatReportDTO,
  TheoryCheatReportVO,
  TheoryProctorHeartbeatDTO,
  TheoryProctorHeartbeatVO,
  TheoryProctorSnapshotDTO,
  TheoryProctorRtcSignalDTO,
  AdminTheoryCandidateMonitorVO,
  TheoryCompetitionConfigVO,
  TheoryCompetitionConfigDTO,
  TheoryCompetitionPaperGenerateRequestDTO,
  TheoryPaperQuestionAdminVO,
  TeamSignupRequestDTO,
  MyTeamInfoVO,
  TeamUpdateRequestDTO,
  TeamInvitationVO,
  TeamInviteAcceptDTO,
  CompositeScoreboardVO,
  CtfScoreboardVO,
  CtfCompetitionChallengeVO,
  CtfCompetitionChallengeDetailVO,
  CtfFlagSubmitRequestDTO,
  CtfTeamSolveRecordVO,
  CompetitionWriteupVO,
  CompetitionWriteupSubmitDTO,
  CompetitionWriteupTemplateVO,
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
  CompetitionWriteupAuditDTO,
  AdminCompetitionLogVO,
  AdminCompetitionLogStatisticsVO,
  AdminCompetitionLogQueryDTO,
  CtfCompetitionSubmissionVO,
  CtfChallengeInstanceVO
} from '@/types/competition'

export function getCompetitionListApi(
  params?: CompetitionQueryDTO
): Promise<ApiResponse<PageResponse<CompetitionSummaryVO>>> {
  return get<PageResponse<CompetitionSummaryVO>>('/competitions', { params })
}

export function getCompetitionSliceApi(
  params?: CompetitionQueryDTO
): Promise<ApiResponse<SliceResponse<CompetitionSummaryVO>>> {
  return get<SliceResponse<CompetitionSummaryVO>>('/competitions/slice', { params })
}

export function getCompetitionBySlugApi(slug: string): Promise<ApiResponse<CompetitionDetailVO>> {
  return get<CompetitionDetailVO>(`/competitions/by-slug/${slug}`)
}

export function getCompetitionByIdApi(id: number | string): Promise<ApiResponse<CompetitionDetailVO>> {
  return get<CompetitionDetailVO>(`/competitions/${id}`)
}

export function getCompetitionAboutApi(id: number | string): Promise<ApiResponse<CompetitionAboutVO>> {
  return get<CompetitionAboutVO>(`/competitions/${id}/about`)
}

export function listAnnouncementsApi(
  id: number | string
): Promise<ApiResponse<CompetitionAnnouncementVO[]>> {
  return get<CompetitionAnnouncementVO[]>(`/competitions/${id}/announcements`)
}

export function listCompetitionAnnouncementsApi(
  id: number | string
): Promise<ApiResponse<CompetitionAnnouncementVO[]>> {
  return get<CompetitionAnnouncementVO[]>(`/competitions/${id}/announcements`)
}

export function getCompetitionScoreboardApi(
  competitionId: number | string
): Promise<ApiResponse<CompositeScoreboardVO>> {
  return get<CompositeScoreboardVO>(`/competitions/${competitionId}/scoreboard`)
}

export function getCtfScoreboardApi(
  competitionId: number | string
): Promise<ApiResponse<CtfScoreboardVO>> {
  return get<CtfScoreboardVO>(`/competitions/${competitionId}/scoreboard/ctf`)
}

export interface ExportFieldOptionVO {
  key: string
  label: string
  group: string
  defaultSelected: boolean
}

export function getAdminCompetitionExportFieldsApi(
  competitionId: number | string,
  scope: string = 'summary'
): Promise<ApiResponse<ExportFieldOptionVO[]>> {
  return get<ExportFieldOptionVO[]>(`/admin/competitions/${competitionId}/scores/export/fields`, {
    params: { scope }
  })
}

export function exportAdminCompetitionScoresApi(
  competitionId: number | string,
  format: 'xlsx' | 'csv' | 'json' = 'xlsx',
  scope: string = 'summary',
  fields?: string[]
): Promise<Blob> {
  const params: Record<string, string | undefined> = { format, scope }
  if (fields && fields.length > 0) {
    params.fields = fields.join(',')
  }
  return download(`/admin/competitions/${competitionId}/scores/export`, {
    params
  })
}

export function listCtfChallengesApi(
  competitionId: number | string
): Promise<ApiResponse<CtfCompetitionChallengeVO[]>> {
  return get<CtfCompetitionChallengeVO[]>(`/competitions/${competitionId}/ctf/challenges`)
}

export function getCtfChallengeDetailApi(
  competitionId: number | string,
  challengeId: number | string
): Promise<ApiResponse<CtfCompetitionChallengeDetailVO>> {
  return get<CtfCompetitionChallengeDetailVO>(
    `/competitions/${competitionId}/ctf/challenges/${challengeId}`
  )
}

export function submitCtfFlagApi(
  competitionId: number | string,
  challengeId: number | string,
  data: CtfFlagSubmitRequestDTO
): Promise<ApiResponse<boolean>> {
  return post<boolean>(
    `/competitions/${competitionId}/ctf/challenges/${challengeId}/submit`,
    data
  )
}

export function getCtfInstanceApi(
  competitionId: number | string,
  challengeId: number | string
): Promise<ApiResponse<CtfChallengeInstanceVO>> {
  return get<CtfChallengeInstanceVO>(
    `/competitions/${competitionId}/ctf/challenges/${challengeId}/instance`
  )
}

export function startCtfInstanceApi(
  competitionId: number | string,
  challengeId: number | string
): Promise<ApiResponse<CtfChallengeInstanceVO>> {
  return post<CtfChallengeInstanceVO>(
    `/competitions/${competitionId}/ctf/challenges/${challengeId}/instance/start`
  )
}

export function stopCtfInstanceApi(
  competitionId: number | string,
  challengeId: number | string
): Promise<ApiResponse<void>> {
  return post<void>(
    `/competitions/${competitionId}/ctf/challenges/${challengeId}/instance/stop`
  )
}

export function extendCtfInstanceApi(
  competitionId: number | string,
  challengeId: number | string
): Promise<ApiResponse<CtfChallengeInstanceVO>> {
  return post<CtfChallengeInstanceVO>(
    `/competitions/${competitionId}/ctf/challenges/${challengeId}/instance/extend`
  )
}

export function getTheoryPaperApi(
  competitionId: number | string
): Promise<ApiResponse<TheoryPaperExamVO>> {
  return get<TheoryPaperExamVO>(`/competitions/${competitionId}/theory/paper`)
}

export function submitTheoryPaperApi(
  competitionId: number | string,
  data: TheoryPaperSubmitRequestDTO
): Promise<ApiResponse<TheoryPaperResultVO>> {
  return post<TheoryPaperResultVO>(`/competitions/${competitionId}/theory/submit`, data)
}

export function reportTheoryCheatEventApi(
  competitionId: number | string,
  data: TheoryCheatReportDTO
): Promise<ApiResponse<TheoryCheatReportVO>> {
  return post<TheoryCheatReportVO>(`/competitions/${competitionId}/theory/cheat-event`, data)
}

export function registerProctorReadyApi(
  competitionId: number | string
): Promise<ApiResponse<void>> {
  return post<void>(`/competitions/${competitionId}/theory/proctor/ready`)
}

export function sendProctorHeartbeatApi(
  competitionId: number | string,
  data: TheoryProctorHeartbeatDTO
): Promise<ApiResponse<TheoryProctorHeartbeatVO>> {
  return post<TheoryProctorHeartbeatVO>(`/competitions/${competitionId}/theory/proctor/heartbeat`, data)
}


export function signupTeamApi(
  competitionId: number | string,
  data: TeamSignupRequestDTO
): Promise<ApiResponse<number | string>> {
  return post<number | string>(`/competitions/${competitionId}/teams/signup`, data)
}

export function getMyTeamApi(
  competitionId: number | string
): Promise<ApiResponse<MyTeamInfoVO>> {
  return get<MyTeamInfoVO>(`/competitions/${competitionId}/teams/me`)
}

export function getTeamByIdApi(
  competitionId: number | string,
  teamId: number | string
): Promise<ApiResponse<MyTeamInfoVO>> {
  return get<MyTeamInfoVO>(`/competitions/${competitionId}/teams/${teamId}`)
}

export function getTeamSolvesApi(
  competitionId: number | string,
  teamId: number | string
): Promise<ApiResponse<CtfTeamSolveRecordVO[]>> {
  return get<CtfTeamSolveRecordVO[]>(`/competitions/${competitionId}/ctf/teams/${teamId}/solves`)
}

export function updateMyTeamApi(
  competitionId: number | string,
  data: TeamUpdateRequestDTO
): Promise<ApiResponse<void>> {
  return put<void>(`/competitions/${competitionId}/teams/me`, data)
}

export function deleteMyTeamApi(
  competitionId: number | string
): Promise<ApiResponse<void>> {
  return del<void>(`/competitions/${competitionId}/teams/me`)
}

export function getMyInvitationsApi(
  competitionId: number | string
): Promise<ApiResponse<TeamInvitationVO[]>> {
  return get<TeamInvitationVO[]>(`/competitions/${competitionId}/teams/invitations/me`)
}

export function acceptInvitationApi(
  competitionId: number | string,
  teamId: number | string,
  data?: TeamInviteAcceptDTO
): Promise<ApiResponse<void>> {
  return post<void>(`/competitions/${competitionId}/teams/${teamId}/invite/accept`, data)
}

export function rejectInvitationApi(
  competitionId: number | string,
  teamId: number | string
): Promise<ApiResponse<void>> {
  return post<void>(`/competitions/${competitionId}/teams/${teamId}/invite/reject`)
}

export function cancelInvitationApi(
  competitionId: number | string,
  teamId: number | string,
  userId: number | string
): Promise<ApiResponse<void>> {
  return del<void>(`/competitions/${competitionId}/teams/${teamId}/invite/cancel?userId=${userId}`)
}

export function reinviteMemberApi(
  competitionId: number | string,
  teamId: number | string,
  userId: number | string
): Promise<ApiResponse<void>> {
  return post<void>(`/competitions/${competitionId}/teams/${teamId}/invite/reinvite?userId=${userId}`)
}

export function getMyWriteupsApi(
  competitionId: number | string
): Promise<ApiResponse<CompetitionWriteupVO[]>> {
  return get<CompetitionWriteupVO[]>(`/competitions/${competitionId}/writeups/my`)
}

export function submitWriteupApi(
  competitionId: number | string,
  data: CompetitionWriteupSubmitDTO
): Promise<ApiResponse<CompetitionWriteupVO>> {
  return post<CompetitionWriteupVO>(`/competitions/${competitionId}/writeups`, data)
}

export function getWriteupTemplateApi(
  competitionId: number | string
): Promise<ApiResponse<CompetitionWriteupTemplateVO>> {
  return get<CompetitionWriteupTemplateVO>(`/competitions/${competitionId}/writeups/template`)
}

export function getAdminCompetitionListApi(params?: {
  page?: number
  size?: number
  keyword?: string
  type?: string
}): Promise<ApiResponse<PageResponse<AdminCompetitionListVO>>> {
  return get<PageResponse<AdminCompetitionListVO>>('/admin/competitions', { params })
}

export function getAdminCompetitionByIdApi(
  id: number | string
): Promise<ApiResponse<AdminCompetitionDetailVO>> {
  return get<AdminCompetitionDetailVO>(`/admin/competitions/${id}`)
}

export function createCompetitionApi(
  data: CompetitionCreateRequestDTO
): Promise<ApiResponse<number | string>> {
  return post<number | string>('/admin/competitions', data)
}

export function updateCompetitionApi(
  id: number | string,
  data: CompetitionUpdateRequestDTO
): Promise<ApiResponse<void>> {
  return put<void>(`/admin/competitions/${id}`, data)
}

export function deleteCompetitionApi(
  id: number | string
): Promise<ApiResponse<void>> {
  return del<void>(`/admin/competitions/${id}`)
}

export function setFreezeScoreboardApi(
  id: number | string,
  frozen: boolean
): Promise<ApiResponse<void>> {
  return post<void>(`/admin/competitions/${id}/freeze`, null, {
    params: { frozen }
  })
}

export function archiveCompetitionApi(id: number | string): Promise<ApiResponse<void>> {
  return post<void>(`/admin/competitions/${id}/archive`)
}

export function unarchiveCompetitionApi(id: number | string): Promise<ApiResponse<void>> {
  return post<void>(`/admin/competitions/${id}/unarchive`)
}

export function listAdminTeamsApi(
  competitionId: number | string,
  params?: {
    page?: number
    size?: number
    status?: string
    keyword?: string
  }
): Promise<ApiResponse<PageResponse<AdminTeamListVO>>> {
  return get<PageResponse<AdminTeamListVO>>(
    `/admin/competitions/${competitionId}/teams`,
    { params }
  )
}

export function getAdminTeamStatsApi(
  competitionId: number | string
): Promise<ApiResponse<AdminTeamStatsVO>> {
  return get<AdminTeamStatsVO>(`/admin/competitions/${competitionId}/teams/stats`)
}

export function getAdminTeamDetailApi(
  competitionId: number | string,
  teamId: number | string
): Promise<ApiResponse<AdminTeamDetailVO>> {
  return get<AdminTeamDetailVO>(`/admin/competitions/${competitionId}/teams/${teamId}`)
}

export function auditAdminTeamApi(
  competitionId: number | string,
  teamId: number | string,
  data: TeamAuditRequestDTO
): Promise<ApiResponse<void>> {
  return post<void>(`/admin/competitions/${competitionId}/teams/${teamId}/audit`, data)
}

export function updateAdminTeamApi(
  competitionId: number | string,
  teamId: number | string,
  data: AdminTeamUpdateDTO
): Promise<ApiResponse<void>> {
  return put<void>(`/admin/competitions/${competitionId}/teams/${teamId}`, data)
}

export function batchAuditAdminTeamsApi(
  competitionId: number | string,
  data: BatchTeamAuditDTO
): Promise<ApiResponse<void>> {
  return post<void>(`/admin/competitions/${competitionId}/teams/batch-audit`, data)
}

export function batchDeleteAdminTeamsApi(
  competitionId: number | string,
  data: BatchTeamDeleteDTO
): Promise<ApiResponse<void>> {
  return post<void>(`/admin/competitions/${competitionId}/teams/batch-delete`, data)
}

export function deleteAdminTeamApi(
  competitionId: number | string,
  teamId: number | string
): Promise<ApiResponse<void>> {
  return del<void>(`/admin/competitions/${competitionId}/teams/${teamId}`)
}

export function listMountedCtfChallengesApi(
  competitionId: number | string
): Promise<ApiResponse<CtfCompetitionChallengeVO[]>> {
  return get<CtfCompetitionChallengeVO[]>(
    `/admin/competitions/${competitionId}/ctf/mount`
  )
}

export function mountCtfChallengeApi(
  competitionId: number | string,
  data: CtfChallengeMountRequestDTO
): Promise<ApiResponse<void>> {
  return post<void>(`/admin/competitions/${competitionId}/ctf/mount`, data)
}

export function batchMountCtfChallengesApi(
  competitionId: number | string,
  data: CtfChallengeBatchMountRequestDTO
): Promise<ApiResponse<void>> {
  return post<void>(`/admin/competitions/${competitionId}/ctf/mount/batch`, data)
}

export function updateMountedCtfChallengeApi(
  competitionId: number | string,
  competitionChallengeId: number | string,
  data: CtfChallengeMountRequestDTO
): Promise<ApiResponse<void>> {
  return put<void>(
    `/admin/competitions/${competitionId}/ctf/mount/${competitionChallengeId}`,
    data
  )
}

export function unmountCtfChallengeApi(
  competitionId: number | string,
  competitionChallengeId: number | string
): Promise<ApiResponse<void>> {
  return del<void>(
    `/admin/competitions/${competitionId}/ctf/mount/${competitionChallengeId}`
  )
}

export function listAdminCompetitionAnnouncementsApi(
  competitionId: number | string
): Promise<ApiResponse<CompetitionAnnouncementVO[]>> {
  return get<CompetitionAnnouncementVO[]>(
    `/admin/competitions/${competitionId}/announcements`
  )
}

export function createAdminCompetitionAnnouncementApi(
  competitionId: number | string,
  data: CompetitionAnnouncementDTO
): Promise<ApiResponse<CompetitionAnnouncementVO>> {
  return post<CompetitionAnnouncementVO>(
    `/admin/competitions/${competitionId}/announcements`,
    data
  )
}

export function updateAdminCompetitionAnnouncementApi(
  competitionId: number | string,
  announcementId: number | string,
  data: CompetitionAnnouncementDTO
): Promise<ApiResponse<CompetitionAnnouncementVO>> {
  return put<CompetitionAnnouncementVO>(
    `/admin/competitions/${competitionId}/announcements/${announcementId}`,
    data
  )
}

export function togglePinAdminCompetitionAnnouncementApi(
  competitionId: number | string,
  announcementId: number | string,
  pinned: boolean
): Promise<ApiResponse<void>> {
  return post<void>(
    `/admin/competitions/${competitionId}/announcements/${announcementId}/pin`,
    null,
    { params: { pinned } }
  )
}

export function deleteAdminCompetitionAnnouncementApi(
  competitionId: number | string,
  announcementId: number | string
): Promise<ApiResponse<void>> {
  return del<void>(
    `/admin/competitions/${competitionId}/announcements/${announcementId}`
  )
}

export function listAdminCompetitionWriteupsApi(
  competitionId: number | string,
  params?: { page?: number; size?: number; keyword?: string }
): Promise<ApiResponse<PageResponse<CompetitionWriteupVO>>> {
  return get<PageResponse<CompetitionWriteupVO>>(
    `/admin/competitions/${competitionId}/writeups`,
    { params }
  )
}

export function listAdminCtfSubmissionsApi(
  competitionId: number | string,
  params?: { page?: number; size?: number; challengeId?: number | string; keyword?: string }
): Promise<ApiResponse<PageResponse<CtfCompetitionSubmissionVO>>> {
  return get<PageResponse<CtfCompetitionSubmissionVO>>(
    `/admin/competitions/${competitionId}/ctf/submissions`,
    { params }
  )
}

export function getAdminCompetitionWriteupDetailApi(
  competitionId: number | string,
  writeupId: number | string
): Promise<ApiResponse<CompetitionWriteupVO>> {
  return get<CompetitionWriteupVO>(
    `/admin/competitions/${competitionId}/writeups/${writeupId}`
  )
}

export function auditAdminCompetitionWriteupApi(
  competitionId: number | string,
  writeupId: number | string,
  data: CompetitionWriteupAuditDTO
): Promise<ApiResponse<CompetitionWriteupVO>> {
  return put<CompetitionWriteupVO>(
    `/admin/competitions/${competitionId}/writeups/${writeupId}/audit`,
    data
  )
}

export function deleteAdminCompetitionWriteupApi(
  competitionId: number | string,
  writeupId: number | string
): Promise<ApiResponse<void>> {
  return del<void>(
    `/admin/competitions/${competitionId}/writeups/${writeupId}`
  )
}

export function getAdminCompetitionLogListApi(
  competitionId: number | string,
  params?: AdminCompetitionLogQueryDTO
): Promise<ApiResponse<PageResponse<AdminCompetitionLogVO>>> {
  return get<PageResponse<AdminCompetitionLogVO>>(
    `/admin/competitions/${competitionId}/logs`,
    { params }
  )
}

export function getAdminCompetitionLogStatisticsApi(
  competitionId: number | string
): Promise<ApiResponse<AdminCompetitionLogStatisticsVO>> {
  return get<AdminCompetitionLogStatisticsVO>(
    `/admin/competitions/${competitionId}/logs/stats`
  )
}

export function getAdminCompetitionLogDetailApi(
  competitionId: number | string,
  id: number | string
): Promise<ApiResponse<AdminCompetitionLogVO>> {
  return get<AdminCompetitionLogVO>(
    `/admin/competitions/${competitionId}/logs/${id}`
  )
}

export function getAdminTheoryCompetitionConfigApi(
  competitionId: number | string
): Promise<ApiResponse<TheoryCompetitionConfigVO>> {
  return get<TheoryCompetitionConfigVO>(
    `/admin/competitions/${competitionId}/theory/config`
  )
}

export function saveAdminTheoryCompetitionConfigApi(
  competitionId: number | string,
  data: TheoryCompetitionConfigDTO
): Promise<ApiResponse<TheoryCompetitionConfigVO>> {
  return put<TheoryCompetitionConfigVO>(
    `/admin/competitions/${competitionId}/theory/config`,
    data
  )
}

export function generateAdminTheoryCompetitionPaperApi(
  competitionId: number | string,
  data: TheoryCompetitionPaperGenerateRequestDTO
): Promise<ApiResponse<TheoryPaperExamVO>> {
  return post<TheoryPaperExamVO>(
    `/admin/competitions/${competitionId}/theory/paper/generate`,
    data
  )
}

export function getAdminTheoryCompetitionPaperApi(
  competitionId: number | string
): Promise<ApiResponse<TheoryPaperQuestionAdminVO[]>> {
  return get<TheoryPaperQuestionAdminVO[]>(
    `/admin/competitions/${competitionId}/theory/paper`
  )
}

export function removeAdminTheoryCompetitionPaperQuestionApi(
  competitionId: number | string,
  paperId: number | string
): Promise<ApiResponse<void>> {
  return del<void>(
    `/admin/competitions/${competitionId}/theory/paper/${paperId}`
  )
}

export function getAdminTheoryCandidateMonitorsApi(
  competitionId: number | string
): Promise<ApiResponse<AdminTheoryCandidateMonitorVO[]>> {
  return get<AdminTheoryCandidateMonitorVO[]>(
    `/admin/competitions/${competitionId}/theory/monitor/candidates`
  )
}

export function uploadTheoryProctorSnapshotApi(
  competitionId: number | string,
  data: TheoryProctorSnapshotDTO
): Promise<ApiResponse<void>> {
  return post<void>(`/competitions/${competitionId}/theory/proctor/snapshot`, data)
}

export function sendTheoryProctorRtcSignalApi(
  competitionId: number | string,
  data: TheoryProctorRtcSignalDTO
): Promise<ApiResponse<void>> {
  return post<void>(`/competitions/${competitionId}/theory/proctor/rtc/signal`, data)
}

export function requestCandidateHighResSnapshotApi(
  competitionId: number | string,
  candidateUserId: number | string
): Promise<ApiResponse<void>> {
  return post<void>(
    `/admin/competitions/${competitionId}/theory/monitor/candidates/${candidateUserId}/snapshot-request`
  )
}

export function sendAdminTheoryProctorRtcSignalApi(
  competitionId: number | string,
  data: TheoryProctorRtcSignalDTO
): Promise<ApiResponse<void>> {
  return post<void>(
    `/admin/competitions/${competitionId}/theory/monitor/rtc/signal`,
    data
  )
}

