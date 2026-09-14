import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  AdminClusterImageVO,
  AdminClusterImageDetailVO,
  AdminClusterImageStatisticsVO,
  AdminImagePageRequestDTO,
  AdminImagePullRequestDTO,
  AdminImageTransferDTO,
  AdminImageBackupDTO
} from '@/types/image'
import type { AdminAsyncTaskSubmitVO } from '@/types/task'

/**
 * 获取 Docker 镜像概览统计数据
 * 对应后端: GET /api/v1/admin/images/statistics
 */
export function getAdminImageStatisticsApi(): Promise<ApiResponse<AdminClusterImageStatisticsVO>> {
  return request.get<AdminClusterImageStatisticsVO>('/admin/images/statistics')
}

/**
 * 分页获取 Docker 镜像列表
 * 对应后端: GET /api/v1/admin/images
 */
export function getAdminImageListApi(
  params?: AdminImagePageRequestDTO
): Promise<ApiResponse<PageResponse<AdminClusterImageVO>>> {
  return request.get<PageResponse<AdminClusterImageVO>>('/admin/images', { params })
}

/**
 * 获取 Docker 镜像深度分层详情 (包含 Layers 与 Inspect 信息)
 * 对应后端: GET /api/v1/admin/images/{id}
 */
export function getAdminImageDetailApi(
  id: string | number
): Promise<ApiResponse<AdminClusterImageDetailVO>> {
  return request.get<AdminClusterImageDetailVO>(`/admin/images/${id}`)
}

/**
 * 重新扫描加载本地及集群子节点中的 Docker 镜像
 * 对应后端: POST /api/v1/admin/images/scan
 */
export function scanAndSyncAdminImagesApi(): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/images/scan')
}

/**
 * 拉取/下载 Docker 镜像至指定节点
 * 对应后端: POST /api/v1/admin/images/pull
 */
export function pullAdminImageApi(data: AdminImagePullRequestDTO): Promise<ApiResponse<AdminAsyncTaskSubmitVO>> {
  return request.post<AdminAsyncTaskSubmitVO>('/admin/images/pull', data)
}

/**
 * 镜像节点转移
 * 对应后端: POST /api/v1/admin/images/transfer
 */
export function transferAdminImageApi(
  data: AdminImageTransferDTO
): Promise<ApiResponse<AdminAsyncTaskSubmitVO>> {
  return request.post<AdminAsyncTaskSubmitVO>('/admin/images/transfer', data)
}

/**
 * 镜像备份 (节点容灾副本)
 * 对应后端: POST /api/v1/admin/images/backup
 */
export function backupAdminImageApi(
  data: AdminImageBackupDTO
): Promise<ApiResponse<AdminAsyncTaskSubmitVO>> {
  return request.post<AdminAsyncTaskSubmitVO>('/admin/images/backup', data)
}

/**
 * 导出 Docker 镜像归档文件 (.tar) 并提供浏览器直接流式下载
 * 对应后端: GET /api/v1/admin/images/{id}/export
 */
export function exportAdminImageApi(id: string | number): Promise<Blob> {
  return request.download(`/admin/images/${id}/export`)
}

/**
 * 从节点删除镜像或全量删除
 * 对应后端: DELETE /api/v1/admin/images/{id}
 */
export function deleteAdminImageApi(
  id: string | number,
  targetNodeIp?: string | string[]
): Promise<ApiResponse<void>> {
  const ipParam = Array.isArray(targetNodeIp) ? targetNodeIp.join(',') : targetNodeIp
  return request.delete<void>(`/admin/images/${id}`, {
    params: ipParam ? { targetNodeIp: ipParam } : undefined
  })
}

/**
 * 一键将主节点所有题目镜像同步至所有资源节点
 * 对应后端: POST /api/v1/admin/images/sync-all
 */
export function syncAllImagesToResourceNodesApi(): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/images/sync-all')
}

/**
 * 将镜像推送至中心私有镜像仓库 (Registry)
 * 对应后端: POST /api/v1/admin/images/{id}/push-registry
 */
export function pushAdminImageToRegistryApi(id: string | number): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/images/${id}/push-registry`)
}
