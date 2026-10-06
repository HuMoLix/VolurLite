import type { KnownAccount } from '@/types/auth/flow'

const STORAGE_KNOWN_ACCOUNTS_KEY = 'volur_known_accounts'

export function getKnownAccounts(): KnownAccount[] {
  if (typeof window === 'undefined' || !window.localStorage) {
    return []
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KNOWN_ACCOUNTS_KEY)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export function saveKnownAccount(account: KnownAccount): void {
  if (typeof window === 'undefined' || !window.localStorage || !account?.identifier) {
    return
  }
  const accounts = getKnownAccounts()
  const cleanId = account.identifier.trim().toLowerCase()
  const existingIndex = accounts.findIndex(
    (item) => item.identifier.trim().toLowerCase() === cleanId || item.username.trim().toLowerCase() === cleanId
  )
  const updatedAccount: KnownAccount = {
    ...account,
    identifier: account.identifier.trim(),
    username: account.username.trim(),
    lastLoginTime: account.lastLoginTime || Date.now()
  }

  if (existingIndex >= 0) {
    accounts[existingIndex] = {
      ...accounts[existingIndex],
      ...updatedAccount
    }
  } else {
    accounts.unshift(updatedAccount)
  }

  try {
    window.localStorage.setItem(STORAGE_KNOWN_ACCOUNTS_KEY, JSON.stringify(accounts.slice(0, 10)))
  } catch {
  }
}

export function removeKnownAccount(identifier: string): void {
  if (typeof window === 'undefined' || !window.localStorage || !identifier) {
    return
  }
  const cleanId = identifier.trim().toLowerCase()
  const accounts = getKnownAccounts().filter(
    (item) => item.identifier.trim().toLowerCase() !== cleanId && item.username.trim().toLowerCase() !== cleanId
  )
  try {
    window.localStorage.setItem(STORAGE_KNOWN_ACCOUNTS_KEY, JSON.stringify(accounts))
  } catch {
  }
}

export function clearKnownAccounts(): void {
  if (typeof window === 'undefined' || !window.localStorage) {
    return
  }
  try {
    window.localStorage.removeItem(STORAGE_KNOWN_ACCOUNTS_KEY)
  } catch {
  }
}

export function hasKnownAccounts(): boolean {
  return getKnownAccounts().length > 0
}
