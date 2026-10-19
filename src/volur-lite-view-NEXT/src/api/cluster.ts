import { request } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type {
  AdminClusterSettingsVO,
  AdminClusterSettingsDTO,
  AdminClusterNodeVO,
  AdminClusterNodeCreateDTO,
  AdminClusterNodeUpdateDTO,
  AdminClusterNodeRoleUpdateDTO,
  AdminContainerTestConnectionDTO,
  AdminContainerTestConnectionVO
} from '@/types/cluster'

export function getAdminClusterSettingsApi(): Promise<ApiResponse<AdminClusterSettingsVO>> {
  return request.get<AdminClusterSettingsVO>('/admin/clusters/settings')
}

export function updateAdminClusterSettingsApi(data: AdminClusterSettingsDTO): Promise<ApiResponse<void>> {
  return request.put<void>('/admin/clusters/settings', data)
}

export function testAdminClusterConnectionApi(
  data?: AdminContainerTestConnectionDTO
): Promise<ApiResponse<AdminContainerTestConnectionVO>> {
  return request.post<AdminContainerTestConnectionVO>('/admin/clusters/test-connection', data, { timeout: 30000 })
}

export function getAdminClusterNodesApi(): Promise<ApiResponse<AdminClusterNodeVO[]>> {
  return request.get<AdminClusterNodeVO[]>('/admin/clusters/nodes')
}

export function addAdminClusterNodeApi(
  data: AdminClusterNodeCreateDTO
): Promise<ApiResponse<AdminClusterNodeVO>> {
  return request.post<AdminClusterNodeVO>('/admin/clusters/nodes', data, { timeout: 60000 })
}

export function updateAdminClusterNodeApi(
  id: string | number,
  data: AdminClusterNodeUpdateDTO
): Promise<ApiResponse<AdminClusterNodeVO>> {
  return request.put<AdminClusterNodeVO>(`/admin/clusters/nodes/${id}`, data, { timeout: 60000 })
}

export function testAdminClusterNodeConnectionApi(
  id: string | number,
  data?: AdminClusterNodeUpdateDTO
): Promise<ApiResponse<AdminContainerTestConnectionVO>> {
  return request.post<AdminContainerTestConnectionVO>(`/admin/clusters/nodes/${id}/test-connection`, data, { timeout: 30000 })
}

export function updateAdminClusterNodeRoleApi(
  id: string | number,
  data: AdminClusterNodeRoleUpdateDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/clusters/nodes/${id}/role`, data)
}

export function removeAdminClusterNodeApi(id: string | number): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/clusters/nodes/${id}`)
}

export function syncImagesToClusterNodeApi(id: string | number): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/clusters/nodes/${id}/sync-images`, undefined, { timeout: 120000 })
}

export function checkAdminClusterNodeHealthApi(
  id: string | number
): Promise<ApiResponse<AdminClusterNodeVO>> {
  return request.post<AdminClusterNodeVO>(`/admin/clusters/nodes/${id}/health-check`)
}

export function checkAllAdminClusterNodesHealthApi(): Promise<ApiResponse<AdminClusterNodeVO[]>> {
  return request.post<AdminClusterNodeVO[]>('/admin/clusters/nodes/health-check-all')
}

export function joinAdminClusterNodeApi(id: string | number): Promise<ApiResponse<AdminClusterNodeVO>> {
  return request.post<AdminClusterNodeVO>(`/admin/clusters/nodes/${id}/join-cluster`, undefined, { timeout: 45000 })
}

export function syncK3sClusterNodesApi(): Promise<ApiResponse<AdminClusterNodeVO[]>> {
  return request.post<AdminClusterNodeVO[]>('/admin/clusters/nodes/sync-k3s', undefined, { timeout: 45000 })
}
