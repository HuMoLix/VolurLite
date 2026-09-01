import { request } from '@/utils/request'
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

/**
 * 获取当前登录用户概要基础信息 (顶部栏/全局鉴权使用)
 * 对应后端: GET /api/v1/users/me
 */
export function getCurrentUserApi(): Promise<ApiResponse<UserSummaryVO>> {
  return request.get<UserSummaryVO>('/users/me')
}

/**
 * 获取当前登录用户详细个人资料 (个人中心编辑使用)
 * 对应后端: GET /api/v1/users/profile
 */
export function getCurrentUserProfileApi(): Promise<ApiResponse<UserProfileResponseVO>> {
  return request.get<UserProfileResponseVO>('/users/profile')
}

/**
 * 根据用户 ID 获取指定用户资料
 * 对应后端: GET /api/v1/users/{userId}/profile
 */
export function getUserProfileByIdApi(userId: number | string): Promise<ApiResponse<UserProfileResponseVO>> {
  return request.get<UserProfileResponseVO>(`/users/${userId}/profile`)
}

/**
 * 更新当前登录用户资料与隐私设置
 * 对应后端: PUT /api/v1/users/profile
 */
export function updateUserProfileApi(data: UpdateUserProfileRequestDTO): Promise<ApiResponse<UserProfileResponseVO>> {
  return request.put<UserProfileResponseVO>('/users/profile', data)
}

/**
 * 上传头像文件并更新当前用户头像
 * 对应后端: POST /api/v1/users/avatar
 */
export function uploadUserAvatarApi(formData: FormData): Promise<ApiResponse<string>> {
  return request.post<string>('/users/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 直接更新当前用户头像 URL
 * 对应后端: PUT /api/v1/users/avatar
 */
export function updateUserAvatarApi(data: UpdateAvatarRequestDTO): Promise<ApiResponse<string>> {
  return request.put<string>('/users/avatar', data)
}

/**
 * 管理端：获取系统用户统计数据
 * 对应后端: GET /api/v1/admin/users/statistics
 */
export function getAdminUserStatisticsApi(): Promise<ApiResponse<AdminUserStatisticsVO>> {
  return request.get<AdminUserStatisticsVO>('/admin/users/statistics')
}

/**
 * 管理端：分页获取用户列表（支持多维度检索与排序）
 * 对应后端: GET /api/v1/admin/users
 */
export function getAdminUserListApi(
  params?: AdminUserPageRequestDTO
): Promise<ApiResponse<PageResponse<AdminUserDetailsVO>>> {
  return request.get<PageResponse<AdminUserDetailsVO>>('/admin/users', { params })
}

/**
 * 管理端：获取指定用户详情
 * 对应后端: GET /api/v1/admin/users/{id}
 */
export function getAdminUserDetailsApi(id: number | string): Promise<ApiResponse<AdminUserDetailsVO>> {
  return request.get<AdminUserDetailsVO>(`/admin/users/${id}`)
}

/**
 * 管理端：创建新用户
 * 对应后端: POST /api/v1/admin/users
 */
export function createAdminUserApi(data: AdminUserCreateDTO): Promise<ApiResponse<number | string>> {
  return request.post<number | string>('/admin/users', data)
}

/**
 * 管理端：更新指定用户信息
 * 对应后端: PUT /api/v1/admin/users/{id}
 */
export function updateAdminUserApi(
  id: number | string,
  data: AdminUserUpdateDTO
): Promise<ApiResponse<void>> {
  return request.put<void>(`/admin/users/${id}`, data)
}

/**
 * 管理端：删除指定用户
 * 对应后端: DELETE /api/v1/admin/users/{id}
 */
export function deleteAdminUserApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/users/${id}`)
}

/**
 * 管理端：批量删除用户
 * 对应后端: POST /api/v1/admin/users/batch/delete
 */
export function batchDeleteAdminUsersApi(ids: (number | string)[]): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/users/batch/delete', { ids })
}

/**
 * 管理端：切换用户启用/禁用状态
 * 对应后端: PATCH /api/v1/admin/users/{id}/status/active
 */
export function toggleAdminUserStatusApi(id: number | string, status: boolean): Promise<ApiResponse<void>> {
  return request.patch<void>(`/admin/users/${id}/status/active`, null, { params: { status } })
}

/**
 * 管理端：切换用户隐藏状态
 * 对应后端: PATCH /api/v1/admin/users/{id}/status/hidden
 */
export function toggleAdminUserHiddenApi(id: number | string, status: boolean): Promise<ApiResponse<void>> {
  return request.patch<void>(`/admin/users/${id}/status/hidden`, null, { params: { status } })
}

/**
 * 管理端：获取所有可用系统角色列表
 * 对应后端: GET /api/v1/admin/users/roles
 */
export function listRolesApi(): Promise<ApiResponse<RoleVO[]>> {
  return request.get<RoleVO[]>('/admin/users/roles')
}
