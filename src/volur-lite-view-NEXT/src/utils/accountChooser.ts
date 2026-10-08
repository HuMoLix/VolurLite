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
  const cleanUsername = (account.username || account.identifier).trim().toLowerCase()
  const cleanEmail = (account.email || '').trim().toLowerCase()

  const existingIndex = accounts.findIndex((item) => {
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
    const existing = accounts[existingIndex]
    accounts[existingIndex] = {
      ...existing,
      ...updatedAccount,
      email: updatedAccount.email || existing.email,
      displayName: updatedAccount.displayName || existing.displayName,
      avatarUrl: updatedAccount.avatarUrl || existing.avatarUrl
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
  const accounts = getKnownAccounts().filter((item) => {
    const itemId = item.identifier.trim().toLowerCase()
    const itemUsername = item.username.trim().toLowerCase()
    const itemEmail = (item.email || '').trim().toLowerCase()
    return itemId !== cleanId && itemUsername !== cleanId && (!itemEmail || itemEmail !== cleanId)
  })
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
