/**
 * 用户模块类型定义 (对应 com.humolix.volur.user)
 */
import type { PageRequestDTO } from '../common/api'

/** 当前登录用户信息响应 VO */
export interface UserProfileResponseVO {
  id: number | string
  username: string
  nickname: string
  email: string
  avatar?: string
  bio?: string
  role: string
  createdAt: string
  updatedAt?: string
}

/** 用户资料更新请求 DTO */
export interface UpdateUserProfileRequestDTO {
  nickname?: string
  bio?: string
  avatar?: string
}

/** 用户详细信息 VO */
export interface UserDetailsVO {
  id: number | string
  username: string
  nickname: string
  email: string
  avatar?: string
  bio?: string
  role: string
  enabled: boolean
  createdAt: string
  updatedAt?: string
}

/** 管理端用户详情 VO (对应 com.humolix.volur.user.domain.vo.AdminUserDetailsVO) */
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
  isHidden: boolean
  lastLoginIp?: string
  createdAt: string
  updatedAt?: string
}

/** 管理端用户统计 VO (对应 com.humolix.volur.user.domain.vo.AdminUserStatisticsVO) */
export interface AdminUserStatisticsVO {
  totalUsers: number
  adminCount: number
  bannedCount: number
  activeCount: number
}

/** 管理端用户分页查询请求 DTO (对应 com.humolix.volur.user.domain.dto.AdminUserPageRequestDTO) */
export interface AdminUserPageRequestDTO extends PageRequestDTO {
  keyword?: string
  role?: string
  status?: boolean
  startTime?: string
  endTime?: string
}

export type AdminUserQueryDTO = AdminUserPageRequestDTO

/** 管理端创建用户 DTO */
export interface AdminUserCreateDTO {
  username: string
  password: string
  email: string
  roleName?: string
  org?: string
  bio?: string
}

/** 管理端更新用户 DTO */
export interface AdminUserUpdateDTO {
  username?: string
  email?: string
  password?: string
  roleName?: string
  org?: string
  bio?: string
  status?: boolean
}

/** 用户角色 VO */
export interface RoleVO {
  id: number
  name: string
  description?: string
}
