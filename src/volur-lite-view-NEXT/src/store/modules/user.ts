import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { getCookie, setCookie, removeCookie } from '@/utils/cookie'
import { getCurrentUserApi } from '@/api/user'
import type { UserSummaryVO } from '@/types/user'
import type { KnownAccount } from '@/types/auth/flow'

const STORAGE_USER_KEY = 'volur_user_summary'
const STORAGE_KNOWN_ACCOUNTS_KEY = 'volur_known_accounts'

function loadStoredUser(): UserSummaryVO | null {
  if (typeof window === 'undefined' || !window.localStorage) {
    return null
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_USER_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function loadStoredKnownAccounts(): KnownAccount[] {
  if (typeof window === 'undefined' || !window.localStorage) {
    return []
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KNOWN_ACCOUNTS_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export const useUserStore = defineStore('user', () => {
  const user = ref<UserSummaryVO | null>(loadStoredUser())
  const knownAccounts = ref<KnownAccount[]>(loadStoredKnownAccounts())
  const cachedAccount = ref<KnownAccount | null>(
    knownAccounts.value.length > 0 ? knownAccounts.value[0] : null
  )
  const isCookieLoggedIn = ref<boolean>(
    typeof document !== 'undefined' && getCookie('logged_in') === 'true'
  )

  let inFlightPromise: Promise<UserSummaryVO | null> | null = null

  watch(
    user,
    (val) => {
      if (typeof window === 'undefined' || !window.localStorage) return
      try {
        if (val) {
          window.localStorage.setItem(STORAGE_USER_KEY, JSON.stringify(val))
        } else {
          window.localStorage.removeItem(STORAGE_USER_KEY)
        }
      } catch {
      }
    },
    { deep: true }
  )

  watch(
    knownAccounts,
    (val) => {
      if (typeof window === 'undefined' || !window.localStorage) return
      try {
        window.localStorage.setItem(STORAGE_KNOWN_ACCOUNTS_KEY, JSON.stringify(val.slice(0, 10)))
      } catch {
      }
    },
    { deep: true }
  )

  const isLoggedIn = computed<boolean>(() => {
    return isCookieLoggedIn.value || !!user.value
  })

  const currentUser = computed<KnownAccount | null>(() => {
    if (!isLoggedIn.value) {
      return null
    }
    if (user.value) {
      return {
        identifier: user.value.username,
        username: user.value.username,
        email: user.value.email,
        displayName: user.value.username,
        avatarUrl: user.value.avatarUrl || undefined,
        lastLoginTime: Date.now()
      }
    }
    return cachedAccount.value
  })

  const displayName = computed<string>(() => {
    if (!isLoggedIn.value) {
      return ''
    }
    return currentUser.value?.displayName || currentUser.value?.username || ''
  })

  const userEmail = computed<string>(() => {
    if (!isLoggedIn.value) {
      return ''
    }
    return currentUser.value?.email || ''
  })

  const avatarUrl = computed<string | undefined>(() => {
    if (!isLoggedIn.value) {
      return undefined
    }
    return currentUser.value?.avatarUrl
  })

  const userInitial = computed<string>(() => {
    if (!isLoggedIn.value) {
      return ''
    }
    const name = displayName.value || userEmail.value
    if (!name) {
      return ''
    }
    return name.charAt(0).toUpperCase()
  })

  const hasKnownAccounts = computed<boolean>(() => {
    return knownAccounts.value.length > 0
  })

  const userRole = computed<string>(() => {
    return user.value?.role || 'USER'
  })

  const isAdmin = computed<boolean>(() => {
    const role = (user.value?.role || '').toUpperCase()
    return role === 'SUPER_ADMIN' || role === 'ADMIN' || role === 'ROLE_SUPER_ADMIN' || role === 'ROLE_ADMIN'
  })

  function saveAccount(account: KnownAccount): void {
    if (!account?.identifier) return
    const cleanId = account.identifier.trim().toLowerCase()
    const cleanUsername = (account.username || account.identifier).trim().toLowerCase()
    const cleanEmail = (account.email || '').trim().toLowerCase()

    const list = [...knownAccounts.value]
    const existingIndex = list.findIndex((item) => {
      const itemId = item.identifier.trim().toLowerCase()
      const itemUsername = item.username.trim().toLowerCase()
      const itemEmail = (item.email || '').trim().toLowerCase()
      return (
        itemId === cleanId ||
        (cleanUsername && itemUsername === cleanUsername) ||
        (cleanUsername && itemId === cleanUsername) ||
        (cleanEmail && itemEmail && itemEmail === cleanEmail) ||
        (cleanEmail && itemId === cleanEmail)
      )
    })

    const updatedAccount: KnownAccount = {
      identifier: account.identifier.trim(),
      username: (account.username || account.identifier).trim(),
      email: account.email ? account.email.trim() : undefined,
      displayName: account.displayName ? account.displayName.trim() : undefined,
      avatarUrl: account.avatarUrl,
      lastLoginTime: account.lastLoginTime || Date.now()
    }

    if (existingIndex >= 0) {
      const existing = list[existingIndex]
      list[existingIndex] = {
        ...existing,
        ...updatedAccount,
        email: updatedAccount.email || existing.email,
        displayName: updatedAccount.displayName || existing.displayName,
        avatarUrl: updatedAccount.avatarUrl || existing.avatarUrl
      }
    } else {
      list.unshift(updatedAccount)
    }

    knownAccounts.value = list
    cachedAccount.value = updatedAccount
  }

  function removeAccount(identifier: string): void {
    if (!identifier) return
    const cleanId = identifier.trim().toLowerCase()
    knownAccounts.value = knownAccounts.value.filter((item) => {
      const itemId = item.identifier.trim().toLowerCase()
      const itemUsername = item.username.trim().toLowerCase()
      const itemEmail = (item.email || '').trim().toLowerCase()
      return itemId !== cleanId && itemUsername !== cleanId && (!itemEmail || itemEmail !== cleanId)
    })
    if (cachedAccount.value?.identifier.trim().toLowerCase() === cleanId) {
      cachedAccount.value = knownAccounts.value.length > 0 ? knownAccounts.value[0] : null
    }
  }

  function clearAccounts(): void {
    knownAccounts.value = []
    cachedAccount.value = null
  }

  function setAuthenticatedUser(accountData: Partial<KnownAccount> & { identifier: string; username?: string; role?: string }): void {
    const identifier = accountData.identifier.trim()
    const username = (accountData.username || identifier).trim()
    const known: KnownAccount = {
      identifier,
      username,
      email: accountData.email ? accountData.email.trim() : undefined,
      displayName: accountData.displayName ? accountData.displayName.trim() : username,
      avatarUrl: accountData.avatarUrl,
      lastLoginTime: accountData.lastLoginTime || Date.now()
    }
    saveAccount(known)
    isCookieLoggedIn.value = true
    if (typeof document !== 'undefined') {
      setCookie('logged_in', 'true')
    }
    user.value = {
      id: identifier,
      username,
      email: accountData.email ? accountData.email.trim() : '',
      avatarUrl: accountData.avatarUrl || null,
      role: accountData.role || 'USER'
    }
  }

  async function fetchCurrentUser(force = false): Promise<UserSummaryVO | null> {
    const hasCookie = typeof document !== 'undefined' && getCookie('logged_in') === 'true'
    if (hasCookie) {
      isCookieLoggedIn.value = true
    }

    if (!force && !isLoggedIn.value) {
      user.value = null
      return null
    }

    if (!force && inFlightPromise) {
      return inFlightPromise
    }

    inFlightPromise = (async () => {
      try {
        const res = await getCurrentUserApi()
        if (res?.data) {
          user.value = res.data
          isCookieLoggedIn.value = true
          if (typeof document !== 'undefined') {
            setCookie('logged_in', 'true')
          }
          saveAccount({
            identifier: res.data.username,
            username: res.data.username,
            email: res.data.email,
            displayName: res.data.username,
            avatarUrl: res.data.avatarUrl || undefined,
            lastLoginTime: Date.now()
          })
          return res.data
        }
        return null
      } catch (err: any) {
        if (err?.response?.status === 401 || err?.status === 401) {
          clearUser()
        }
        return null
      } finally {
        inFlightPromise = null
      }
    })()

    return inFlightPromise
  }

  function clearUser(): void {
    user.value = null
    cachedAccount.value = null
    isCookieLoggedIn.value = false
    if (typeof document !== 'undefined') {
      removeCookie('logged_in')
    }
  }

  return {
    user,
    knownAccounts,
    cachedAccount,
    isLoggedIn,
    currentUser,
    displayName,
    userEmail,
    avatarUrl,
    userInitial,
    hasKnownAccounts,
    userRole,
    isAdmin,
    saveAccount,
    removeAccount,
    clearAccounts,
    setAuthenticatedUser,
    fetchCurrentUser,
    clearUser
  }
})
