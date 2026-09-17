import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  CtfChallengeListVO,
  CtfChallengeDetailVO,
  CtfChallengeCreateRequestDTO,
  CtfChallengeUpdateRequestDTO,
  CtfCategoryVO,
  CtfCategoryCreateRequestDTO,
  CtfCategoryUpdateRequestDTO,
  TheoryChallengeListVO,
  TheoryChallengeDetailVO,
  TheoryChallengeCreateRequestDTO,
  TheoryChallengeUpdateRequestDTO,
  TheoryType,
  ChallengeImportResultVO
} from '@/types/challenge'

/* =========================================================================
 * 1. 管理端 CTF 题目管理 (Admin CTF Challenges)
 * ========================================================================= */

/**
 * 分页获取 CTF 题目列表
 * 对应后端: GET /api/v1/admin/challenges/ctf
 */
export function getAdminCtfChallengeListApi(params?: {
  page?: number
  size?: number
  categoryId?: number | string
  name?: string
  keyword?: string
}): Promise<ApiResponse<PageResponse<CtfChallengeListVO>>> {
  return request.get<PageResponse<CtfChallengeListVO>>('/admin/challenges/ctf', { params })
}

/**
 * 获取指定 CTF 题目详情
 * 对应后端: GET /api/v1/admin/challenges/ctf/{id}
 */
export function getAdminCtfChallengeDetailApi(id: number | string): Promise<ApiResponse<CtfChallengeDetailVO>> {
  return request.get<CtfChallengeDetailVO>(`/admin/challenges/ctf/${id}`)
}

/**
 * 创建 CTF 题目
 * 对应后端: POST /api/v1/admin/challenges/ctf
 */
export function createAdminCtfChallengeApi(data: CtfChallengeCreateRequestDTO): Promise<ApiResponse<number | string>> {
  return request.post<number | string>('/admin/challenges/ctf', data)
}

/**
 * 更新 CTF 题目
 * 对应后端: PUT /api/v1/admin/challenges/ctf/{id}
 */
export function updateAdminCtfChallengeApi(
  id: number | string,
  data: CtfChallengeUpdateRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/challenges/ctf/${id}`, data)
}

/**
 * 删除 CTF 题目
 * 对应后端: DELETE /api/v1/admin/challenges/ctf/{id}
 */
export function deleteAdminCtfChallengeApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/challenges/ctf/${id}`)
}

/**
 * 导入 CTF 题目 (支持单个题目 ZIP 压缩包，包含 challenge.json / build.json 及附件)
 * 对应后端: POST /api/v1/admin/challenges/ctf/import
 */
export function importAdminCtfChallengeApi(formData: FormData): Promise<ApiResponse<ChallengeImportResultVO>> {
  return request.post<ChallengeImportResultVO>('/admin/challenges/ctf/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 兼容历史导入函数别名
 */
export const importAdminCtfChallengesApi = importAdminCtfChallengeApi

/**
 * 下载标准 CTF 题目包模版 (ZIP 压缩包直接流式下载)
 * 对应后端: GET /api/v1/admin/challenges/ctf/template
 */
export function downloadAdminCtfTemplateApi(): Promise<Blob> {
  return request.download('/admin/challenges/ctf/template')
}

/**
 * 批量导出 CTF 题目 (ZIP 压缩包直接流式下载)
 * 对应后端: GET /api/v1/admin/challenges/ctf/export
 */
export function exportAdminCtfChallengesApi(ids?: (number | string)[]): Promise<Blob> {
  const params = ids && ids.length > 0 ? { ids: ids.join(',') } : {}
  return request.download('/admin/challenges/ctf/export', { params })
}

/* =========================================================================
 * 2. 管理端 CTF 赛题分类管理 (Admin CTF Categories)
 * ========================================================================= */

/**
 * 获取所有 CTF 题目分类列表
 * 对应后端: GET /api/v1/admin/challenges/ctf/categories
 */
export function getAdminCtfCategoriesApi(): Promise<ApiResponse<CtfCategoryVO[]>> {
  return request.get<CtfCategoryVO[]>('/admin/challenges/ctf/categories')
}

/**
 * 获取指定 CTF 赛题分类详情
 * 对应后端: GET /api/v1/admin/challenges/ctf/categories/{id}
 */
export function getAdminCtfCategoryDetailApi(id: number | string): Promise<ApiResponse<CtfCategoryVO>> {
  return request.get<CtfCategoryVO>(`/admin/challenges/ctf/categories/${id}`)
}

/**
 * 创建 CTF 赛题分类
 * 对应后端: POST /api/v1/admin/challenges/ctf/categories
 */
export function createAdminCtfCategoryApi(data: CtfCategoryCreateRequestDTO): Promise<ApiResponse<number | string>> {
  return request.post<number | string>('/admin/challenges/ctf/categories', data)
}

/**
 * 更新 CTF 赛题分类
 * 对应后端: PUT /api/v1/admin/challenges/ctf/categories/{id}
 */
export function updateAdminCtfCategoryApi(
  id: number | string,
  data: CtfCategoryUpdateRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/challenges/ctf/categories/${id}`, data)
}

/**
 * 删除 CTF 赛题分类
 * 对应后端: DELETE /api/v1/admin/challenges/ctf/categories/{id}
 */
export function deleteAdminCtfCategoryApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/challenges/ctf/categories/${id}`)
}

/* =========================================================================
 * 3. 管理端理论题库管理 (Admin Theory Challenges)
 * ========================================================================= */

/**
 * 分页获取理论题目列表
 * 对应后端: GET /api/v1/admin/challenges/theory
 */
export function getAdminTheoryChallengeListApi(params?: {
  page?: number
  size?: number
  type?: TheoryType
  category?: string
  keyword?: string
}): Promise<ApiResponse<PageResponse<TheoryChallengeListVO>>> {
  return request.get<PageResponse<TheoryChallengeListVO>>('/admin/challenges/theory', { params })
}

/**
 * 获取指定理论题目详情
 * 对应后端: GET /api/v1/admin/challenges/theory/{id}
 */
export function getAdminTheoryChallengeDetailApi(id: number | string): Promise<ApiResponse<TheoryChallengeDetailVO>> {
  return request.get<TheoryChallengeDetailVO>(`/admin/challenges/theory/${id}`)
}

/**
 * 创建理论题目
 * 对应后端: POST /api/v1/admin/challenges/theory
 */
export function createAdminTheoryChallengeApi(data: TheoryChallengeCreateRequestDTO): Promise<ApiResponse<number | string>> {
  return request.post<number | string>('/admin/challenges/theory', data)
}

/**
 * 更新理论题目
 * 对应后端: PUT /api/v1/admin/challenges/theory/{id}
 */
export function updateAdminTheoryChallengeApi(
  id: number | string,
  data: TheoryChallengeUpdateRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/challenges/theory/${id}`, data)
}

/**
 * 删除理论题目
 * 对应后端: DELETE /api/v1/admin/challenges/theory/{id}
 */
export function deleteAdminTheoryChallengeApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/challenges/theory/${id}`)
}

/**
 * 批量导入理论题目 (JSON 文件或包含 JSON 的 ZIP)
 * 对应后端: POST /api/v1/admin/challenges/theory/import
 */
export function importAdminTheoryChallengesApi(formData: FormData): Promise<ApiResponse<ChallengeImportResultVO>> {
  return request.post<ChallengeImportResultVO>('/admin/challenges/theory/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 批量导出理论题目 (JSON 流式下载)
 * 对应后端: GET /api/v1/admin/challenges/theory/export
 */
export function exportAdminTheoryChallengesApi(ids?: (number | string)[]): Promise<Blob> {
  const params = ids && ids.length > 0 ? { ids: ids.join(',') } : {}
  return request.download('/admin/challenges/theory/export', { params })
}
