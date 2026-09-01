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

export { formatAvatarUrl, getCachedImageUrl, setMemoryImageCache, evictImageCache, preloadAndCacheImage } from './imageCache'
