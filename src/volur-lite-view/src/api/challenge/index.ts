import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  CtfChallengeListVO,
  CtfChallengeDetailVO,
  CtfCategoryVO
} from '@/types/challenge'

/**
 * 管理端：分页获取 CTF 题目列表
 * 对应后端: GET /api/v1/admin/challenges/ctf
 */
export function getAdminCtfChallengeListApi(params?: {
  page?: number
  size?: number
  categoryId?: number | string
  keyword?: string
}): Promise<ApiResponse<PageResponse<CtfChallengeListVO>>> {
  return request.get<PageResponse<CtfChallengeListVO>>('/admin/challenges/ctf', { params })
}

/**
 * 管理端：获取指定 CTF 题目详情
 * 对应后端: GET /api/v1/admin/challenges/ctf/{id}
 */
export function getAdminCtfChallengeDetailApi(id: number | string): Promise<ApiResponse<CtfChallengeDetailVO>> {
  return request.get<CtfChallengeDetailVO>(`/admin/challenges/ctf/${id}`)
}

/**
 * 管理端：获取 CTF 题目分类列表
 * 对应后端: GET /api/v1/admin/challenges/ctf/categories
 */
export function getAdminCtfCategoriesApi(): Promise<ApiResponse<CtfCategoryVO[]>> {
  return request.get<CtfCategoryVO[]>('/admin/challenges/ctf/categories')
}
