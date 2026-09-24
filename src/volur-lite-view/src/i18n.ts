import { createI18n } from 'vue-i18n'

// 采用动态导入（不传 eager: true），按需懒加载指定语言资源包
const localeModules = import.meta.glob('./locales/**/*.json')

export type LocaleNamespace = 'core' | 'admin' | 'competition' | 'profile' | 'all'

const loadedPaths = new Set<string>()

const savedLocale = typeof localStorage !== 'undefined' ? localStorage.getItem('locale') : null
export const defaultLocale =
  savedLocale === 'zh' || savedLocale === 'zh_CN'
    ? 'zh_CN'
    : savedLocale === 'en' || savedLocale === 'en_US'
      ? 'en_US'
      : 'zh_CN'

/**
 * 根据语言文件路径归类命名空间
 */
export function getNamespaceForPath(filePath: string): LocaleNamespace {
  if (filePath.includes('/admin/')) {
    return 'admin'
  }
  if (filePath.endsWith('/competition.json')) {
    return 'competition'
  }
  if (filePath.endsWith('/profile.json')) {
    return 'profile'
  }
  return 'core'
}

/**
 * 根据路由路径推导需要加载的命名空间集合
 */
export function getNamespacesForRoute(routePath: string): LocaleNamespace[] {
  const namespaces: LocaleNamespace[] = ['core']
  if (routePath.startsWith('/admin')) {
    namespaces.push('admin')
  } else if (
    routePath.startsWith('/competitions') ||
    routePath.startsWith('/c/') ||
    routePath.startsWith('/arena')
  ) {
    namespaces.push('competition')
  } else if (routePath.startsWith('/profile')) {
    namespaces.push('profile')
  }
  return namespaces
}

/**
 * 检查指定语言下的某命名空间或核心包是否已加载
 */
export function isLocaleNamespaceLoaded(locale: string, ns: LocaleNamespace): boolean {
  const prefix = `/locales/${locale}/`
  const matching = Object.keys(localeModules).filter((path) => {
    return path.includes(prefix) && (ns === 'all' || getNamespaceForPath(path) === ns)
  })
  return matching.length > 0 && matching.every((p) => loadedPaths.has(p))
}

export function isLocaleLoaded(locale: string): boolean {
  return isLocaleNamespaceLoaded(locale, 'core')
}

/**
 * 异步按需加载指定语言（如 zh_CN / en_US）下指定命名空间的 JSON 语言片段
 * 默认首屏仅拉取核心词条 ('core')，避免无关路由包造成网络瀑布阻断
 */
export async function loadLocaleMessagesAsync(
  locale: string,
  namespaces: LocaleNamespace | LocaleNamespace[] = ['core']
): Promise<Record<string, any>> {
  const nsList = Array.isArray(namespaces) ? namespaces : [namespaces]
  const prefix = `/locales/${locale}/`

  const pathsToLoad = Object.keys(localeModules).filter((path) => {
    if (!path.includes(prefix)) return false
    if (loadedPaths.has(path)) return false
    if (nsList.includes('all')) return true
    const ns = getNamespaceForPath(path)
    return nsList.includes(ns)
  })

  if (pathsToLoad.length === 0) {
    return (i18n.global as any).getLocaleMessage(locale) || {}
  }

  const entries = await Promise.all(
    pathsToLoad.map(async (path) => {
      const loader = localeModules[path]
      const mod = (await loader()) as any
      return { path, content: mod.default || mod }
    })
  )

  const messages: Record<string, any> = {}

  for (const { path, content } of entries) {
    const matched = path.match(/\/locales\/(.+)\.json$/i)
    if (matched && matched[1]) {
      const parts = matched[1].split('/')
      // parts[0] 是语言代码 (如 'zh_CN')
      const subparts = parts.slice(1)
      if (subparts.length === 1) {
        messages[subparts[0]] = content
      } else {
        let current = messages
        for (let i = 0; i < subparts.length - 1; i++) {
          const dir = subparts[i]
          if (!current[dir] || typeof current[dir] !== 'object') {
            current[dir] = {}
          }
          current = current[dir]
        }
        current[subparts[subparts.length - 1]] = content
      }
    }
  }

  ;(i18n.global as any).mergeLocaleMessage(locale, messages)
  for (const p of pathsToLoad) {
    loadedPaths.add(p)
  }

  return (i18n.global as any).getLocaleMessage(locale) || {}
}

/**
 * 路由守卫与动态导航辅助：自动根据路由目标加载所需的多语言命名空间
 */
export async function loadLocaleForRouteAsync(locale: string, routePath: string): Promise<void> {
  const namespaces = getNamespacesForRoute(routePath)
  await loadLocaleMessagesAsync(locale, namespaces)
}

const i18n = createI18n({
  legacy: false,
  locale: defaultLocale,
  fallbackLocale: 'zh_CN',
  messages: {}
})

export default i18n
