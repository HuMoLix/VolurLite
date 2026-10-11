import { get, post, put, del } from '@/utils/request'
import type { ApiResponse, PageResponse, SliceResponse } from '@/types/common/api'
import type {
  CompetitionSummaryVO,
  CompetitionDetailVO,
  CompetitionAboutVO,
  CompetitionAnnouncementVO,
  CompetitionQueryDTO,
  TheoryPaperExamVO,
  TheoryPaperSubmitRequestDTO,
  TheoryPaperResultVO,
  TeamSignupRequestDTO,
  MyTeamInfoVO,
  TeamUpdateRequestDTO,
  TeamInvitationVO,
  TeamInviteAcceptDTO
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


