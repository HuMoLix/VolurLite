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

/**
 * 获取集群配置与节点概况
 * 对应后端: GET /api/v1/admin/clusters/settings
 */
export function getAdminClusterSettingsApi(): Promise<ApiResponse<AdminClusterSettingsVO>> {
  return request.get<AdminClusterSettingsVO>('/admin/clusters/settings')
}

/**
 * 更新集群基础参数与调度配置
 * 对应后端: PUT /api/v1/admin/clusters/settings
 */
export function updateAdminClusterSettingsApi(data: AdminClusterSettingsDTO): Promise<ApiResponse<void>> {
  return request.put<void>('/admin/clusters/settings', data)
}

/**
 * 测试容器集群连通性
 * 对应后端: POST /api/v1/admin/clusters/test-connection
 */
export function testAdminClusterConnectionApi(
  data?: AdminContainerTestConnectionDTO
): Promise<ApiResponse<AdminContainerTestConnectionVO>> {
  return request.post<AdminContainerTestConnectionVO>('/admin/clusters/test-connection', data, { timeout: 30000 })
}

/**
 * 获取集群所有节点列表
 * 对应后端: GET /api/v1/admin/clusters/nodes
 */
export function getAdminClusterNodesApi(): Promise<ApiResponse<AdminClusterNodeVO[]>> {
  return request.get<AdminClusterNodeVO[]>('/admin/clusters/nodes')
}

/**
 * 添加节点扩展集群
 * 对应后端: POST /api/v1/admin/clusters/nodes
 */
export function addAdminClusterNodeApi(
  data: AdminClusterNodeCreateDTO
): Promise<ApiResponse<AdminClusterNodeVO>> {
  return request.post<AdminClusterNodeVO>('/admin/clusters/nodes', data, { timeout: 60000 })
}

/**
 * 修改集群节点信息与认证配置
 * 对应后端: PUT /api/v1/admin/clusters/nodes/{id}
 */
export function updateAdminClusterNodeApi(
  id: string | number,
  data: AdminClusterNodeUpdateDTO
): Promise<ApiResponse<AdminClusterNodeVO>> {
  return request.put<AdminClusterNodeVO>(`/admin/clusters/nodes/${id}`, data, { timeout: 60000 })
}

/**
 * 单节点存活与连通性测试
 * 对应后端: POST /api/v1/admin/clusters/nodes/{id}/test-connection
 */
export function testAdminClusterNodeConnectionApi(
  id: string | number,
  data?: AdminClusterNodeUpdateDTO
): Promise<ApiResponse<AdminContainerTestConnectionVO>> {
  return request.post<AdminContainerTestConnectionVO>(`/admin/clusters/nodes/${id}/test-connection`, data, { timeout: 30000 })
}

/**
 * 修改节点角色与资源调度分配
 * 对应后端: PUT /api/v1/admin/clusters/nodes/{id}/role
 */
export function updateAdminClusterNodeRoleApi(
  id: string | number,
  data: AdminClusterNodeRoleUpdateDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/clusters/nodes/${id}/role`, data)
}

/**
 * 移除集群节点
 * 对应后端: DELETE /api/v1/admin/clusters/nodes/{id}
 */
export function removeAdminClusterNodeApi(id: string | number): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/clusters/nodes/${id}`)
}

/**
 * 向指定节点同步题目镜像
 * 对应后端: POST /api/v1/admin/clusters/nodes/{id}/sync-images
 */
export function syncImagesToClusterNodeApi(id: string | number): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/clusters/nodes/${id}/sync-images`, undefined, { timeout: 120000 })
}

/**
 * 节点存活健康检测与实时资源对账
 * 对应后端: POST /api/v1/admin/clusters/nodes/{id}/health-check
 */
export function checkAdminClusterNodeHealthApi(
  id: string | number
): Promise<ApiResponse<AdminClusterNodeVO>> {
  return request.post<AdminClusterNodeVO>(`/admin/clusters/nodes/${id}/health-check`)
}

/**
 * 一键全量检测所有集群节点存活状态与资源对账
 * 对应后端: POST /api/v1/admin/clusters/nodes/health-check-all
 */
export function checkAllAdminClusterNodesHealthApi(): Promise<ApiResponse<AdminClusterNodeVO[]>> {
  return request.post<AdminClusterNodeVO[]>('/admin/clusters/nodes/health-check-all')
}

/**
 * 触发工作节点重新加入编排集群
 * 对应后端: POST /api/v1/admin/clusters/nodes/{id}/join-cluster
 */
export function joinAdminClusterNodeApi(id: string | number): Promise<ApiResponse<AdminClusterNodeVO>> {
  return request.post<AdminClusterNodeVO>(`/admin/clusters/nodes/${id}/join-cluster`, undefined, { timeout: 45000 })
}
