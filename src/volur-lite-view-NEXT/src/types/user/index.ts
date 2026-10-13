import type { PageRequestDTO } from '../common/api'

export type Gender = 'UNKNOWN' | 'MALE' | 'FEMALE'
export type PrivacyType = 'PUBLIC' | 'PRIVATE'

export interface UserSummaryVO {
  id: number | string
  username: string
  email: string
  avatarUrl?: string | null
  role: string
  org?: string | null
  bio?: string | null
  createdAt?: string
}

export interface UserProfileResponseVO {
  id: number | string
  username: string
  email: string
  phone?: string
  gender?: Gender
  birthday?: string
  role: string
  org?: string
  major?: string
  bio?: string
  avatarUrl?: string
  bannerUrl?: string
  notice?: string
  emailPrivacy?: PrivacyType
  phonePrivacy?: PrivacyType
  orgPrivacy?: PrivacyType
  createdAt: string
}

export interface UpdateUserProfileRequestDTO {
  gender?: Gender
  birthday?: string
  org?: string
  major?: string
  bio?: string
  emailPrivacy?: PrivacyType
  phonePrivacy?: PrivacyType
  orgPrivacy?: PrivacyType
}

export interface UpdateAvatarRequestDTO {
  avatarUrl: string
}

export interface ChangeUserPasswordRequestDTO {
  oldPassword: string
  newPassword: string
}

export interface AdminUserDetailsVO {
  id: string | number
  username: string
  email: string
  phone?: string
  realName?: string
  gender?: string
  birthday?: string
  role: string
  org?: string
  major?: string
  bio?: string
  avatarUrl?: string
  status: boolean
  isHidden?: boolean
  lastLoginIp?: string
  createdAt: string
  updatedAt?: string
}

export interface AdminUserStatisticsVO {
  totalUsers: number
  adminCount: number
  bannedCount: number
  activeCount: number
}

export interface AdminUserPageRequestDTO extends PageRequestDTO {
  keyword?: string
  role?: string
  status?: boolean
  startTime?: string
  endTime?: string
}

export interface AdminUserCreateDTO {
  username: string
  password: string
  email: string
  roleName?: string
  org?: string
  bio?: string
}

export interface AdminUserUpdateDTO {
  username?: string
  email?: string
  password?: string
  roleName?: string
  org?: string
  bio?: string
  status?: boolean
}

export interface RoleVO {
  id: number
  name: string
  description?: string
}
