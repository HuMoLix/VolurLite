import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AuthenticateRequestDTO, AuthenticateResponseVO } from '@/types/auth'
import type { UserSummaryVO, UserProfileResponseVO } from '@/types/user'
import { loginApi, logoutApi } from '@/api/auth'
import { getCurrentUserApi, getCurrentUserProfileApi } from '@/api/user'
import {
  getAccessToken,
  setAccessToken,
  setRefreshToken,
  getUserInfo,
  setUserInfo,
  clearAuthStorage
} from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getAccessToken())
  const user = ref<UserSummaryVO | null>(getUserInfo())

  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => user.value?.role || 'ANONYMOUS')
  const isAdmin = computed(() => {
    const r = (user.value?.role || '').toUpperCase()
    return r === 'SUPER_ADMIN' || r === 'ADMIN' || r === 'ROLE_SUPER_ADMIN' || r === 'ROLE_ADMIN'
  })

  let inFlightCurrentUserPromise: Promise<UserSummaryVO | null> | null = null

  /**
   * 获取当前登录用户概要信息 (对应 GET /api/v1/users/me，用于顶部栏与全局鉴权)
   * 具备在途 Promise 去重机制
   */
  async function fetchCurrentUser(force = false): Promise<UserSummaryVO | null> {
    if (!token.value) {
      user.value = null
      return null
    }

    if (!force && inFlightCurrentUserPromise) {
      return inFlightCurrentUserPromise
    }

    inFlightCurrentUserPromise = (async () => {
      try {
        const res = await getCurrentUserApi()
        user.value = res.data
        setUserInfo(res.data)
        return res.data
      } catch {
        return null
      } finally {
        inFlightCurrentUserPromise = null
      }
    })()

    return inFlightCurrentUserPromise
  }

  /**
   * 获取当前用户完整资料 (对应 GET /api/v1/users/profile)
   */
  async function fetchUserProfile(): Promise<UserProfileResponseVO | null> {
    if (!token.value) return null
    try {
      const res = await getCurrentUserProfileApi()
      // 同步更新概要信息中的关键字段
      if (user.value) {
        user.value.avatarUrl = res.data.avatarUrl
        user.value.org = res.data.org
        user.value.bio = res.data.bio
        setUserInfo(user.value)
      }
      return res.data
    } catch {
      return null
    }
  }

  /**
   * 立即就地更新当前 Pinia 实体中的头像 URL，避免冗余网络请求
   */
  function updateUserAvatar(avatarUrl: string): void {
    if (user.value) {
      user.value = { ...user.value, avatarUrl }
      setUserInfo(user.value)
    } else {
      user.value = {
        id: '',
        username: '',
        email: '',
        role: 'USER',
        avatarUrl,
        createdAt: ''
      }
      setUserInfo(user.value)
    }
  }

  /**
   * 立即就地更新当前 Pinia 实体中的基础概要信息
   */
  function updateUserInfo(partial: Partial<UserSummaryVO>): void {
    if (user.value) {
      user.value = { ...user.value, ...partial }
      setUserInfo(user.value)
    }
  }

  async function login(credentials: AuthenticateRequestDTO): Promise<AuthenticateResponseVO> {
    const res = await loginApi(credentials)
    const data = res.data
    token.value = data.accessToken
    setAccessToken(data.accessToken)
    setRefreshToken(data.refreshToken)

    // 获取并填充当前用户基础概要信息
    await fetchCurrentUser(true)

    // 立即初始化全局 SSE 长连接
    try {
      const { useNotificationStore } = await import('./notification')
      useNotificationStore().initSse()
    } catch {
      // ignore
    }

    return data
  }

  async function logout(): Promise<void> {
    try {
      if (token.value) {
        await logoutApi()
      }
    } finally {
      token.value = null
      user.value = null
      clearAuthStorage()

      // 退出登录时主动断开 SSE
      try {
        const { useNotificationStore } = await import('./notification')
        useNotificationStore().destroySse()
      } catch {
        // ignore
      }
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    userRole,
    isAdmin,
    login,
    logout,
    fetchCurrentUser,
    fetchUserProfile,
    updateUserAvatar,
    updateUserInfo
  }
})
