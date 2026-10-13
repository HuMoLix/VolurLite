import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type { AdminClusterNodeVO } from '@/types/cluster'
import type {
  ChallengeInstanceResponseVO,
  AdminContainerPageRequestDTO,
  AdminContainerRenewRequestDTO,
  AdminContainerBatchStopRequestDTO,
  AdminContainerManualStartDTO,
  AdminContainerExecDTO,
  AdminContainerExecResultVO,
  AdminContainerInstanceVO,
  AdminContainerStatisticsVO,
  AdminContainerTerminalInfoVO
} from '@/types/container'

export function startInstanceApi(challengeId: number | string): Promise<ApiResponse<ChallengeInstanceResponseVO>> {
  return request.post<ChallengeInstanceResponseVO>(`/challenges/instances/${challengeId}/start`)
}

export function stopInstanceApi(challengeId: number | string): Promise<ApiResponse<void>> {
  return request.post<void>(`/challenges/instances/${challengeId}/stop`)
}

export function extendInstanceApi(challengeId: number | string): Promise<ApiResponse<ChallengeInstanceResponseVO>> {
  return request.post<ChallengeInstanceResponseVO>(`/challenges/instances/${challengeId}/extend`)
}

export function getAdminContainerStatisticsApi(): Promise<ApiResponse<AdminContainerStatisticsVO>> {
  return request.get<AdminContainerStatisticsVO>('/admin/containers/stats')
}

export function getAdminContainerCandidateNodesApi(): Promise<ApiResponse<AdminClusterNodeVO[]>> {
  return request.get<AdminClusterNodeVO[]>('/admin/containers/candidate-nodes')
}

export function getAdminContainerListApi(
  params?: AdminContainerPageRequestDTO
): Promise<ApiResponse<PageResponse<AdminContainerInstanceVO>>> {
  return request.get<PageResponse<AdminContainerInstanceVO>>('/admin/containers', { params })
}

export function manualStartAdminContainerApi(
  data: AdminContainerManualStartDTO
): Promise<ApiResponse<AdminContainerInstanceVO>> {
  return request.post<AdminContainerInstanceVO>('/admin/containers/start', data)
}

export function execAdminContainerApi(
  id: number | string,
  data: AdminContainerExecDTO
): Promise<ApiResponse<AdminContainerExecResultVO>> {
  return request.post<AdminContainerExecResultVO>(`/admin/containers/${id}/exec`, data)
}

export function getAdminContainerTerminalInfoApi(
  id: number | string
): Promise<ApiResponse<AdminContainerTerminalInfoVO>> {
  return request.get<AdminContainerTerminalInfoVO>(`/admin/containers/${id}/terminal-info`)
}

export function stopAdminContainerApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/containers/${id}`)
}

export function batchStopAdminContainersApi(
  data: AdminContainerBatchStopRequestDTO
): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/containers/batch-stop', data)
}

export function renewAdminContainerApi(
  id: number | string,
  data: AdminContainerRenewRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/containers/${id}/renew`, data)
}
