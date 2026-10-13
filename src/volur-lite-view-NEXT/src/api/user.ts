import { get, post, put, del, patch } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  UserSummaryVO,
  UserProfileResponseVO,
  UpdateUserProfileRequestDTO,
  UpdateAvatarRequestDTO,
  AdminUserDetailsVO,
  AdminUserStatisticsVO,
  AdminUserPageRequestDTO,
  AdminUserCreateDTO,
  AdminUserUpdateDTO,
  RoleVO
} from '@/types/user'

export function getCurrentUserApi(): Promise<ApiResponse<UserSummaryVO>> {
  return get<UserSummaryVO>('/users/me', { skipToast: true })
}

export function getCurrentUserProfileApi(): Promise<ApiResponse<UserProfileResponseVO>> {
  return get<UserProfileResponseVO>('/users/profile', { skipToast: true })
}

export function getUserProfileByIdApi(userId: number | string): Promise<ApiResponse<UserProfileResponseVO>> {
  return get<UserProfileResponseVO>(`/users/${userId}/profile`, { skipToast: true })
}

export function updateUserProfileApi(
  data: UpdateUserProfileRequestDTO
): Promise<ApiResponse<UserProfileResponseVO>> {
  return put<UserProfileResponseVO>('/users/profile', data)
}

export function updateAvatarApi(
  data: UpdateAvatarRequestDTO
): Promise<ApiResponse<string>> {
  return put<string>('/users/avatar', data)
}

export function getAdminUserStatisticsApi(): Promise<ApiResponse<AdminUserStatisticsVO>> {
  return get<AdminUserStatisticsVO>('/admin/users/stats')
}

export function getAdminUserListApi(
  params?: AdminUserPageRequestDTO
): Promise<ApiResponse<PageResponse<AdminUserDetailsVO>>> {
  return get<PageResponse<AdminUserDetailsVO>>('/admin/users', { params })
}

export function getAdminUserDetailsApi(id: number | string): Promise<ApiResponse<AdminUserDetailsVO>> {
  return get<AdminUserDetailsVO>(`/admin/users/${id}`)
}

export function createAdminUserApi(data: AdminUserCreateDTO): Promise<ApiResponse<number | string>> {
  return post<number | string>('/admin/users', data)
}

export function updateAdminUserApi(
  id: number | string,
  data: AdminUserUpdateDTO
): Promise<ApiResponse<void>> {
  return put<void>(`/admin/users/${id}`, data)
}

export function deleteAdminUserApi(id: number | string): Promise<ApiResponse<void>> {
  return del<void>(`/admin/users/${id}`)
}

export function batchDeleteAdminUsersApi(ids: (number | string)[]): Promise<ApiResponse<void>> {
  return post<void>('/admin/users/batch/delete', { ids })
}

export function toggleAdminUserStatusApi(id: number | string, status: boolean): Promise<ApiResponse<void>> {
  return patch<void>(`/admin/users/${id}/status/active`, null, { params: { status } })
}

export function listRolesApi(): Promise<ApiResponse<RoleVO[]>> {
  return get<RoleVO[]>('/admin/users/roles')
}
