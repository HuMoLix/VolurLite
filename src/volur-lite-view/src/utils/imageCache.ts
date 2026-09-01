/**
 * 前端图片资源缓存引擎与 URL 格式化工具
 * 结合浏览器 Cache-Control 与内存 Blob 缓存，避免重复请求后端图片
 */

const memoryImageCache = new Map<string, string>()

/**
 * 格式化后端图片/头像地址，确保支持绝对路径、相对路径及代理环境
 */
export function formatAvatarUrl(avatarUrl?: string | null, bustCache = false): string | undefined {
  if (!avatarUrl || !avatarUrl.trim()) return undefined
  const raw = avatarUrl.trim()

  // 若存在本地预加载/裁剪生成的内存 Blob，直接返回
  if (memoryImageCache.has(raw)) {
    return memoryImageCache.get(raw)!
  }

  if (raw.startsWith('http://') || raw.startsWith('https://') || raw.startsWith('data:') || raw.startsWith('blob:')) {
    return bustCache ? `${raw}${raw.includes('?') ? '&' : '?'}t=${Date.now()}` : raw
  }

  const apiBase = import.meta.env.VITE_API_BASE_URL || ''
  let finalUrl = raw.startsWith('/') ? raw : `/${raw}`

  if (apiBase && (apiBase.startsWith('http://') || apiBase.startsWith('https://'))) {
    try {
      const origin = new URL(apiBase).origin
      finalUrl = `${origin}${finalUrl}`
    } catch {
      // fallback to relative
    }
  }

  if (bustCache) {
    finalUrl += `${finalUrl.includes('?') ? '&' : '?'}t=${Date.now()}`
  }

  return finalUrl
}

/**
 * 获取缓存中的图片 URL（优先取内存 Blob，其次返回格式化后的 URL）
 */
export function getCachedImageUrl(avatarUrl?: string | null): string | undefined {
  return formatAvatarUrl(avatarUrl, false)
}

/**
 * 注入本地生成的 Blob 缓存（例如头像裁剪完成后直接注入，免去网络往返拉取）
 */
export function setMemoryImageCache(originalUrl: string, blobUrl: string): void {
  if (originalUrl && blobUrl) {
    // 销毁旧的 blob url 防止内存泄露
    const old = memoryImageCache.get(originalUrl)
    if (old && old.startsWith('blob:') && old !== blobUrl) {
      URL.revokeObjectURL(old)
    }
    memoryImageCache.set(originalUrl, blobUrl)
  }
}

/**
 * 移除指定图片的内存缓存并释放 Blob URL
 */
export function evictImageCache(originalUrl: string): void {
  if (originalUrl && memoryImageCache.has(originalUrl)) {
    const blobUrl = memoryImageCache.get(originalUrl)
    if (blobUrl && blobUrl.startsWith('blob:')) {
      URL.revokeObjectURL(blobUrl)
    }
    memoryImageCache.delete(originalUrl)
  }
}

/**
 * 预加载图片并写入内存缓存
 */
export async function preloadAndCacheImage(url: string): Promise<string> {
  const formatted = formatAvatarUrl(url, false)
  if (!formatted) return ''

  if (memoryImageCache.has(url)) {
    return memoryImageCache.get(url)!
  }

  try {
    const response = await fetch(formatted, { credentials: 'omit' })
    if (!response.ok) return formatted
    const blob = await response.blob()
    const objectUrl = URL.createObjectURL(blob)
    setMemoryImageCache(url, objectUrl)
    return objectUrl
  } catch {
    return formatted
  }
}
