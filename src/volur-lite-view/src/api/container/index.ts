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

/**
 * 启动靶机容器 (选手端)
 * 对应后端: POST /api/v1/challenges/instances/{challengeId}/start
 */
export function startInstanceApi(challengeId: number | string): Promise<ApiResponse<ChallengeInstanceResponseVO>> {
  return request.post<ChallengeInstanceResponseVO>(`/challenges/instances/${challengeId}/start`)
}

/**
 * 销毁/停止靶机容器 (选手端)
 * 对应后端: POST /api/v1/challenges/instances/{challengeId}/stop
 */
export function stopInstanceApi(challengeId: number | string): Promise<ApiResponse<void>> {
  return request.post<void>(`/challenges/instances/${challengeId}/stop`)
}

/**
 * 延长靶机容器租期 (选手端)
 * 对应后端: POST /api/v1/challenges/instances/{challengeId}/extend
 */
export function extendInstanceApi(challengeId: number | string): Promise<ApiResponse<ChallengeInstanceResponseVO>> {
  return request.post<ChallengeInstanceResponseVO>(`/challenges/instances/${challengeId}/extend`)
}

/* =================================================================================
 * 管理端容器与靶机生命周期管理接口 (Admin Container APIs)
 * 严格 1:1 对齐 com.humolix.volur.container.controller.v1.admin.AdminContainerController
 * ================================================================================= */

/**
 * 管理端：获取容器集群与运行统计数据
 * 对应后端: GET /api/v1/admin/containers/statistics
 */
export function getAdminContainerStatisticsApi(): Promise<ApiResponse<AdminContainerStatisticsVO>> {
  return request.get<AdminContainerStatisticsVO>('/admin/containers/statistics')
}

/**
 * 管理端：获取支持部署的候选资源节点列表
 * 对应后端: GET /api/v1/admin/containers/candidate-nodes
 */
export function getAdminContainerCandidateNodesApi(): Promise<ApiResponse<AdminClusterNodeVO[]>> {
  return request.get<AdminClusterNodeVO[]>('/admin/containers/candidate-nodes')
}

/**
 * 管理端：分页获取容器实例列表
 * 对应后端: GET /api/v1/admin/containers
 */
export function getAdminContainerListApi(
  params?: AdminContainerPageRequestDTO
): Promise<ApiResponse<PageResponse<AdminContainerInstanceVO>>> {
  return request.get<PageResponse<AdminContainerInstanceVO>>('/admin/containers', { params })
}

/**
 * 管理端：手动启动指定题目的动态靶机容器
 * 对应后端: POST /api/v1/admin/containers/start
 */
export function manualStartAdminContainerApi(
  data: AdminContainerManualStartDTO
): Promise<ApiResponse<AdminContainerInstanceVO>> {
  return request.post<AdminContainerInstanceVO>('/admin/containers/start', data)
}

/**
 * 管理端：在容器内执行调试命令 (交互式 Web Shell)
 * 对应后端: POST /api/v1/admin/containers/{id}/exec
 */
export function execAdminContainerApi(
  id: number | string,
  data: AdminContainerExecDTO
): Promise<ApiResponse<AdminContainerExecResultVO>> {
  return request.post<AdminContainerExecResultVO>(`/admin/containers/${id}/exec`, data)
}

/**
 * 管理端：获取容器交互终端会话信息
 * 对应后端: GET /api/v1/admin/containers/{id}/terminal-info
 */
export function getAdminContainerTerminalInfoApi(
  id: number | string
): Promise<ApiResponse<AdminContainerTerminalInfoVO>> {
  return request.get<AdminContainerTerminalInfoVO>(`/admin/containers/${id}/terminal-info`)
}

/**
 * 管理端：强制销毁并释放单个容器实例
 * 对应后端: DELETE /api/v1/admin/containers/{id}
 */
export function stopAdminContainerApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/containers/${id}`)
}

/**
 * 管理端：批量销毁容器实例
 * 对应后端: POST /api/v1/admin/containers/batch-stop
 */
export function batchStopAdminContainersApi(
  data: AdminContainerBatchStopRequestDTO
): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/containers/batch-stop', data)
}

/**
 * 管理端：延长容器实例运行时间
 * 对应后端: PUT /api/v1/admin/containers/{id}/renew
 */
export function renewAdminContainerApi(
  id: number | string,
  data: AdminContainerRenewRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/containers/${id}/renew`, data)
}
