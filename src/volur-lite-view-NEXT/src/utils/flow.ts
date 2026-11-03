export const FLOW_SESSION_TTL_MS = 15 * 60 * 1000

export function parseDshTimestamp(dsh?: string | null): number | null {
  if (!dsh || typeof dsh !== 'string') {
    return null
  }
  const colonIndex = dsh.indexOf(':')
  if (colonIndex === -1) {
    return null
  }
  const timePart = dsh.slice(colonIndex + 1)
  const micro = Number(timePart)
  if (Number.isNaN(micro) || micro <= 0) {
    return null
  }
  return Math.floor(micro / 1000)
}

export function isDshExpired(dsh?: string | null, ttlMs = FLOW_SESSION_TTL_MS): boolean {
  if (!dsh || typeof dsh !== 'string') {
    return false
  }
  const createdAtMs = parseDshTimestamp(dsh)
  if (!createdAtMs) {
    return false
  }
  const now = Date.now()
  return now - createdAtMs > ttlMs
}

export function buildSessionExpireLocation(currentRoute?: { path?: string; fullPath?: string; query?: Record<string, any> }) {
  const path = currentRoute?.path || (typeof window !== 'undefined' ? window.location.pathname : '')
  const query = currentRoute?.query || {}

  let continueTarget = (query.continue as string) || ''
  let followupTarget = (query.followup as string) || ''

  if (!followupTarget || followupTarget === '/') {
    if (path.startsWith('/v1/signup')) {
      followupTarget = '/v1/signup'
    } else if (path.startsWith('/v1/signin')) {
      followupTarget = '/v1/signin/identifier'
    } else if (path && path !== '/v1/info/sessionexpire') {
      followupTarget = path
    } else {
      followupTarget = '/v1/signin/identifier'
    }
  }

  if (!continueTarget) {
    continueTarget = (query.redirect as string) || '/competitions'
  }

  return {
    path: '/v1/info/sessionexpire',
    query: {
      continue: continueTarget,
      followup: followupTarget
    }
  }
}

export function buildSessionExpireHref(): string {
  if (typeof window === 'undefined') {
    return '/v1/info/sessionexpire?continue=/competitions&followup=/v1/signin/identifier'
  }
  const pathname = window.location.pathname
  const searchParams = new URLSearchParams(window.location.search)
  const continueTarget = searchParams.get('continue') || searchParams.get('redirect') || '/competitions'
  let followupTarget = searchParams.get('followup') || ''

  if (!followupTarget || followupTarget === '/') {
    if (pathname.startsWith('/v1/signup')) {
      followupTarget = '/v1/signup'
    } else if (pathname.startsWith('/v1/signin')) {
      followupTarget = '/v1/signin/identifier'
    } else if (pathname && pathname !== '/v1/info/sessionexpire') {
      followupTarget = pathname
    } else {
      followupTarget = '/v1/signin/identifier'
    }
  }

  const destParams = new URLSearchParams()
  destParams.set('continue', continueTarget)
  destParams.set('followup', followupTarget)
  return `/v1/info/sessionexpire?${destParams.toString()}`
}
