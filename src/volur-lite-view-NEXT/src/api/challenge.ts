import { request } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  CtfChallengeListVO,
  CtfChallengeDetailVO,
  CtfChallengeCreateRequestDTO,
  CtfChallengeUpdateRequestDTO,
  CtfAttachmentVO,
  CtfCategoryVO,
  CtfCategoryCreateRequestDTO,
  CtfCategoryUpdateRequestDTO,
  TheoryBankListVO,
  TheoryBankDetailVO,
  TheoryBankCreateRequestDTO,
  TheoryBankUpdateRequestDTO,
  TheoryChallengeListVO,
  TheoryChallengeDetailVO,
  TheoryChallengeCreateRequestDTO,
  TheoryChallengeUpdateRequestDTO,
  TheoryType,
  ChallengeImportResultVO
} from '@/types/challenge'

export function getAdminCtfChallengeListApi(params?: {
  page?: number
  size?: number
  categoryId?: number | string
  name?: string
  keyword?: string
}): Promise<ApiResponse<PageResponse<CtfChallengeListVO>>> {
  return request.get<PageResponse<CtfChallengeListVO>>('/admin/challenges/ctf', { params })
}

export function getAdminCtfChallengeDetailApi(id: number | string): Promise<ApiResponse<CtfChallengeDetailVO>> {
  return request.get<CtfChallengeDetailVO>(`/admin/challenges/ctf/${id}`)
}

export function createAdminCtfChallengeApi(data: CtfChallengeCreateRequestDTO): Promise<ApiResponse<number | string>> {
  return request.post<number | string>('/admin/challenges/ctf', data)
}

export function updateAdminCtfChallengeApi(
  id: number | string,
  data: CtfChallengeUpdateRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/challenges/ctf/${id}`, data)
}

export function deleteAdminCtfChallengeApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/challenges/ctf/${id}`)
}

export function importAdminCtfChallengeApi(formData: FormData): Promise<ApiResponse<ChallengeImportResultVO>> {
  return request.post<ChallengeImportResultVO>('/admin/challenges/ctf/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadAdminCtfAttachmentApi(formData: FormData): Promise<ApiResponse<CtfAttachmentVO>> {
  return request.post<CtfAttachmentVO>('/admin/challenges/ctf/attachments/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function downloadAdminCtfTemplateApi(): Promise<Blob> {
  return request.download('/admin/challenges/ctf/template')
}

export function exportAdminCtfChallengesApi(ids?: (number | string)[]): Promise<Blob> {
  const params = ids && ids.length > 0 ? { ids: ids.join(',') } : {}
  return request.download('/admin/challenges/ctf/export', { params })
}

export function getAdminCtfCategoriesApi(): Promise<ApiResponse<CtfCategoryVO[]>> {
  return request.get<CtfCategoryVO[]>('/admin/challenges/ctf/categories')
}

export function getAdminCtfCategoryDetailApi(id: number | string): Promise<ApiResponse<CtfCategoryVO>> {
  return request.get<CtfCategoryVO>(`/admin/challenges/ctf/categories/${id}`)
}

export function createAdminCtfCategoryApi(data: CtfCategoryCreateRequestDTO): Promise<ApiResponse<number | string>> {
  return request.post<number | string>('/admin/challenges/ctf/categories', data)
}

export function updateAdminCtfCategoryApi(
  id: number | string,
  data: CtfCategoryUpdateRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/challenges/ctf/categories/${id}`, data)
}

export function deleteAdminCtfCategoryApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/challenges/ctf/categories/${id}`)
}

export function getAdminTheoryChallengeListApi(params?: {
  page?: number
  size?: number
  type?: TheoryType
  category?: string
  keyword?: string
}): Promise<ApiResponse<PageResponse<TheoryChallengeListVO>>> {
  return request.get<PageResponse<TheoryChallengeListVO>>('/admin/challenges/theory', { params })
}

export function getAdminTheoryChallengeDetailApi(id: number | string): Promise<ApiResponse<TheoryChallengeDetailVO>> {
  return request.get<TheoryChallengeDetailVO>(`/admin/challenges/theory/${id}`)
}

export function createAdminTheoryChallengeApi(data: TheoryChallengeCreateRequestDTO): Promise<ApiResponse<number | string>> {
  return request.post<number | string>('/admin/challenges/theory', data)
}

export function updateAdminTheoryChallengeApi(
  id: number | string,
  data: TheoryChallengeUpdateRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/challenges/theory/${id}`, data)
}

export function deleteAdminTheoryChallengeApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/challenges/theory/${id}`)
}

export function importAdminTheoryChallengesApi(formData: FormData): Promise<ApiResponse<ChallengeImportResultVO>> {
  return request.post<ChallengeImportResultVO>('/admin/challenges/theory/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function exportAdminTheoryChallengesApi(ids?: (number | string)[]): Promise<Blob> {
  const params = ids && ids.length > 0 ? { ids: ids.join(',') } : {}
  return request.download('/admin/challenges/theory/export', { params })
}

export function getAdminTheoryBankListApi(params?: {
  page?: number
  size?: number
  keyword?: string
}): Promise<ApiResponse<PageResponse<TheoryBankListVO>>> {
  return request.get<PageResponse<TheoryBankListVO>>('/admin/challenges/theory/banks', { params })
}

export function getAdminTheoryBankDetailApi(id: number | string): Promise<ApiResponse<TheoryBankDetailVO>> {
  return request.get<TheoryBankDetailVO>(`/admin/challenges/theory/banks/${id}`)
}

export function createAdminTheoryBankApi(data: TheoryBankCreateRequestDTO): Promise<ApiResponse<number | string>> {
  return request.post<number | string>('/admin/challenges/theory/banks', data)
}

export function updateAdminTheoryBankApi(
  id: number | string,
  data: TheoryBankUpdateRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/challenges/theory/banks/${id}`, data)
}

export function deleteAdminTheoryBankApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/challenges/theory/banks/${id}`)
}

export function importAdminTheoryBanksApi(formData: FormData): Promise<ApiResponse<ChallengeImportResultVO>> {
  return request.post<ChallengeImportResultVO>('/admin/challenges/theory/banks/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function exportAdminTheoryBanksApi(ids?: (number | string)[]): Promise<Blob> {
  const params = ids && ids.length > 0 ? { ids: ids.join(',') } : {}
  return request.download('/admin/challenges/theory/banks/export', { params })
}

export function getAdminTheoryBankQuestionsApi(
  bankId: number | string,
  params?: {
    page?: number
    size?: number
    type?: TheoryType
    category?: string
    keyword?: string
  }
): Promise<ApiResponse<PageResponse<TheoryChallengeListVO>>> {
  return request.get<PageResponse<TheoryChallengeListVO>>(`/admin/challenges/theory/banks/${bankId}/questions`, { params })
}

export function createAdminTheoryBankQuestionApi(
  bankId: number | string,
  data: TheoryChallengeCreateRequestDTO
): Promise<ApiResponse<number | string>> {
  return request.post<number | string>(`/admin/challenges/theory/banks/${bankId}/questions`, data)
}

export function updateAdminTheoryBankQuestionApi(
  bankId: number | string,
  questionId: number | string,
  data: TheoryChallengeUpdateRequestDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/challenges/theory/banks/${bankId}/questions/${questionId}`, data)
}

export function deleteAdminTheoryBankQuestionApi(
  bankId: number | string,
  questionId: number | string
): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/challenges/theory/banks/${bankId}/questions/${questionId}`)
}

export function importAdminTheoryBankQuestionsApi(
  bankId: number | string,
  formData: FormData
): Promise<ApiResponse<ChallengeImportResultVO>> {
  return request.post<ChallengeImportResultVO>(`/admin/challenges/theory/banks/${bankId}/questions/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function exportAdminTheoryBankQuestionsApi(
  bankId: number | string,
  ids?: (number | string)[]
): Promise<Blob> {
  const params = ids && ids.length > 0 ? { ids: ids.join(',') } : {}
  return request.download(`/admin/challenges/theory/banks/${bankId}/questions/export`, { params })
}
