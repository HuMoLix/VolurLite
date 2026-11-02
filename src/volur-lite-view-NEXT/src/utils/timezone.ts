function pad(n: number): string {
  return String(n).padStart(2, '0')
}

function toDate(date: Date, pattern: string): string {
  const year = String(date.getFullYear())
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hours = pad(date.getHours())
  const minutes = pad(date.getMinutes())
  const seconds = pad(date.getSeconds())
  const ms = String(date.getMilliseconds()).padStart(3, '0')

  return pattern
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
    .replace('SSS', ms)
}

function toUtc(utcStr: string): Date {
  const str = utcStr.trim()
  if (!str.includes('Z') && !/[+-]\d{2}(:\d{2})?$/.test(str)) {
    return new Date(str.replace(' ', 'T') + 'Z')
  }
  return new Date(str)
}

async function fetchWorldTime(): Promise<number | null> {
  const sources = [
    async () => {
      const res = await fetch('https://timeapi.io/api/time/current/zone?timeZone=UTC', {
        signal: AbortSignal.timeout(3000)
      })
      const data = await res.json()
      const dt = String(data.dateTime)
      return new Date(dt.endsWith('Z') ? dt : dt + 'Z').getTime()
    },
    async () => {
      const res = await fetch('https://www.cloudflare.com/cdn-cgi/trace', {
        signal: AbortSignal.timeout(3000)
      })
      const text = await res.text()
      const match = text.match(/ts=(\d+(\.\d+)?)/)
      return match ? Math.round(Number(match[1]) * 1000) : null
    }
  ]

  for (const src of sources) {
    try {
      const ts = await src()
      if (ts && !Number.isNaN(ts)) {
        return ts
      }
    } catch {
    }
  }
  return null
}

async function fetchServerTime(): Promise<number | null> {
  if (typeof window === 'undefined' || typeof fetch === 'undefined') {
    return null
  }
  try {
    const res = await fetch(window.location.origin, {
      method: 'HEAD',
      cache: 'no-store',
      signal: AbortSignal.timeout(3000)
    })
    const header = res.headers.get('date')
    if (!header) {
      return null
    }
    const ts = Date.parse(header)
    return Number.isNaN(ts) ? null : ts
  } catch {
    return null
  }
}

export function getTimeZone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
  } catch {
    return 'UTC'
  }
}

export function date2utc(dateStr: string): string {
  const date = dateStr.includes(' ') && !dateStr.includes('T') ? dateStr.replace(' ', 'T') : dateStr
  return new Date(date).toISOString()
}

export function utc2date(utcStr: string, pattern: string = 'YYYY-MM-DD HH:mm:ss'): string {
  return toDate(toUtc(utcStr), pattern)
}

export function ts2utc(ts: number | string): string {
  return new Date(Number(ts)).toISOString()
}

export function utc2ts(utcStr: string): number {
  return toUtc(utcStr).getTime()
}

export function date2ts(dateStr: string): number {
  const date = dateStr.includes(' ') && !dateStr.includes('T') ? dateStr.replace(' ', 'T') : dateStr
  return new Date(date).getTime()
}

export function ts2date(ts: number | string, pattern: string = 'YYYY-MM-DD HH:mm:ss'): string {
  return toDate(new Date(Number(ts)), pattern)
}

export async function isTimeVerify(): Promise<boolean> {
  try {
    let ts = await fetchWorldTime()
    if (!ts) {
      ts = await fetchServerTime()
    }

    if (!ts || Number.isNaN(ts)) {
      return true
    }

    return Math.abs(Date.now() - ts) <= 30000
  } catch {
    return true
  }
}
