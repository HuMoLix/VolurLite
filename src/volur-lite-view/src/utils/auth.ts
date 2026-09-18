const ACCESS_TOKEN_KEY = 'volur_access_token'
const REFRESH_TOKEN_KEY = 'volur_refresh_token'
const USER_INFO_KEY = 'volur_user_info'

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(token: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

export function getUserInfo<T = any>(): T | null {
  const data = localStorage.getItem(USER_INFO_KEY)
  if (!data) return null
  try {
    return JSON.parse(data)
  } catch {
    return null
  }
}

export function setUserInfo<T = any>(info: T): void {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
}

export function clearAuthStorage(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

export interface DecodedTokenPayload {
  sub?: string
  userId?: string | number
  exp?: number
  [key: string]: any
}

/**
 * 从 JWT Access Token 中解码 Payload（无需依赖外部库）
 */
export function getDecodedTokenPayload(): DecodedTokenPayload | null {
  const token = getAccessToken()
  if (!token || !token.includes('.')) return null
  try {
    const parts = token.split('.')
    if (parts.length >= 2) {
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      )
      return JSON.parse(jsonPayload)
    }
  } catch {
    return null
  }
  return null
}

/**
 * 汇总当前登录选手的全量身份候选集（多源容错聚合：Token / LocalStorage / Store / WebSocket）
 */
export function getCurrentAuthCandidateIdentities(extraUser?: any, extraReadyData?: any): {
  candidateIds: Set<string>
  candidateUsernames: Set<string>
} {
  const candidateIds = new Set<string>()
  const candidateUsernames = new Set<string>()

  // 1. JWT Payload
  const jwt = getDecodedTokenPayload()
  if (jwt?.userId != null && jwt.userId !== '') candidateIds.add(String(jwt.userId))
  if (jwt?.sub) candidateUsernames.add(String(jwt.sub).trim().toLowerCase())

  // 2. localStorage 用户基础信息
  const local: any = getUserInfo()
  if (local?.userId != null && local.userId !== '') candidateIds.add(String(local.userId))
  if (local?.id != null && local.id !== '') candidateIds.add(String(local.id))
  if (local?.username) candidateUsernames.add(String(local.username).trim().toLowerCase())

  // 3. Pinia userStore.user
  if (extraUser) {
    if (extraUser.userId != null && extraUser.userId !== '') candidateIds.add(String(extraUser.userId))
    if (extraUser.id != null && extraUser.id !== '') candidateIds.add(String(extraUser.id))
    if (extraUser.username) candidateUsernames.add(String(extraUser.username).trim().toLowerCase())
  }

  // 4. WebSocket readyData
  if (extraReadyData) {
    if (extraReadyData.userId != null && extraReadyData.userId !== '') candidateIds.add(String(extraReadyData.userId))
    if (extraReadyData.username) candidateUsernames.add(String(extraReadyData.username).trim().toLowerCase())
  }

  return { candidateIds, candidateUsernames }
}

/**
 * 严格判定指定对象/成员是否属于当前选手本人
 * 支持根据 userId、id 及 username（忽略大小写）进行多重双向精确比对
 */
export function isSelfMemberIdentity(
  target: { userId?: string | number | null; id?: string | number | null; username?: string | null } | null | undefined,
  extraUser?: any,
  extraReadyData?: any
): boolean {
  if (!target) return false
  const { candidateIds, candidateUsernames } = getCurrentAuthCandidateIdentities(extraUser, extraReadyData)

  const tUserId = target.userId != null && target.userId !== '' ? String(target.userId) : ''
  const tId = target.id != null && target.id !== '' ? String(target.id) : ''
  for (const cid of candidateIds) {
    if (cid && (cid === tUserId || cid === tId)) {
      return true
    }
  }

  const tUsername = (target.username || '').trim().toLowerCase()
  if (tUsername && candidateUsernames.has(tUsername)) {
    return true
  }

  return false
}

export { formatAvatarUrl, getCachedImageUrl, setMemoryImageCache, evictImageCache, preloadAndCacheImage } from './imageCache'
