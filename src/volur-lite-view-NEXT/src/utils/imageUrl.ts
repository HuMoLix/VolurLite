export function formatBannerUrl(bannerUrl?: string | null): string {
  if (!bannerUrl || !bannerUrl.trim()) return ''
  const raw = bannerUrl.trim()
  if (
    raw.startsWith('http://') ||
    raw.startsWith('https://') ||
    raw.startsWith('data:') ||
    raw.startsWith('blob:')
  ) {
    return raw
  }
  const apiBase = import.meta.env.VITE_API_BASE_URL || ''
  const path = raw.startsWith('/') ? raw : `/${raw}`
  if (apiBase && (apiBase.startsWith('http://') || apiBase.startsWith('https://'))) {
    try {
      const origin = new URL(apiBase).origin
      return `${origin}${path}`
    } catch {
      return path
    }
  }
  return path
}
