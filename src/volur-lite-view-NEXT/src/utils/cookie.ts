export interface CookieOptions {
  days?: number
  path?: string
  sameSite?: 'Lax' | 'Strict' | 'None'
  secure?: boolean
}

export function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null
  const matches = document.cookie.match(
    new RegExp('(?:^|; )' + name.replace(/([.$?*|{}()[\]\\/+^])/g, '\\$1') + '=([^;]*)')
  )
  return matches ? decodeURIComponent(matches[1]) : null
}

export function setCookie(
  name: string,
  value: string,
  daysOrOptions: number | CookieOptions = 30
): void {
  if (typeof document === 'undefined') return

  const options: CookieOptions =
    typeof daysOrOptions === 'number' ? { days: daysOrOptions } : daysOrOptions

  const days = options.days ?? 30
  const path = options.path ?? '/'
  const sameSite = options.sameSite ?? 'Lax'
  const isSecure =
    options.secure ?? (typeof window !== 'undefined' && window.location.protocol === 'https:')

  const expires = new Date(Date.now() + days * 864e5).toUTCString()
  document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=${path}; SameSite=${sameSite}${isSecure ? '; Secure' : ''}`
}

export function removeCookie(name: string, path = '/'): void {
  if (typeof document === 'undefined') return
  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=${path}; SameSite=Lax`
}
