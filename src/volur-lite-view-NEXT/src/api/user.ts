import { get, put } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type {
  UserSummaryVO,
  UserProfileResponseVO,
  UpdateUserProfileRequestDTO,
  UpdateAvatarRequestDTO
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
