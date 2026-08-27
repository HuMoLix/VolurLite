import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AuthenticateRequestDTO, AuthenticateResponseVO } from '@/types/auth'
import type { UserProfileResponseVO } from '@/types/user'
import { loginApi, logoutApi } from '@/api/auth'
import { getCurrentUserProfileApi } from '@/api/user'
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
  const user = ref<UserProfileResponseVO | null>(getUserInfo())

  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => user.value?.role || 'ANONYMOUS')
  const isAdmin = computed(() => {
    const r = (user.value?.role || '').toUpperCase()
    return r === 'SUPER_ADMIN' || r === 'ADMIN' || r === 'ROLE_SUPER_ADMIN' || r === 'ROLE_ADMIN'
  })

  async function login(credentials: AuthenticateRequestDTO): Promise<AuthenticateResponseVO> {
    const res = await loginApi(credentials)
    const data = res.data
    token.value = data.accessToken
    setAccessToken(data.accessToken)
    setRefreshToken(data.refreshToken)

    // Fetch full profile
    await fetchUserProfile()
    return data
  }

  async function fetchUserProfile(): Promise<UserProfileResponseVO | null> {
    try {
      const res = await getCurrentUserProfileApi()
      user.value = res.data
      setUserInfo(res.data)
      return res.data
    } catch {
      return null
    }
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
    fetchUserProfile
  }
})
