import { createI18n } from 'vue-i18n'
import zhCnCommon from '../locales/zh-CN/common.json'
import zhCnAuthCommon from '../locales/zh-CN/auth/auth.json'
import zhCnAuthSignin from '../locales/zh-CN/auth/signin.json'
import zhCnAuthSignup from '../locales/zh-CN/auth/signup.json'
import zhCnAuthRecovery from '../locales/zh-CN/auth/recovery.json'
import zhCnCaptchaRecaptcha from '../locales/zh-CN/captcha/recaptcha.json'
import zhCnCompetition from '../locales/zh-CN/competition.json'
import zhCnAdmin from '../locales/zh-CN/admin.json'

const zhCnAuth = {
  ...zhCnAuthCommon,
  signin: zhCnAuthSignin,
  signup: zhCnAuthSignup,
  recovery: zhCnAuthRecovery
}

const zhCnCaptcha = {
  recaptcha: zhCnCaptchaRecaptcha
}

const localeModules = import.meta.glob(['../locales/**/*.json', '!../locales/zh-CN/**/*.json', '!../locales/zh-CN/*.json'])

export type SupportedLocale = 'zh-CN' | 'zh-TW' | 'en-US'
export type LocaleNamespace = 'common' | 'auth' | 'captcha' | 'admin' | 'competition' | 'all'

export const DEFAULT_LOCALE: SupportedLocale = 'zh-CN'

const loadedPaths = new Set<string>([
  '../locales/zh-CN/common.json',
  '../locales/zh-CN/auth/auth.json',
  '../locales/zh-CN/auth/signin.json',
  '../locales/zh-CN/auth/signup.json',
  '../locales/zh-CN/auth/recovery.json',
  '../locales/zh-CN/captcha/recaptcha.json',
  '../locales/zh-CN/competition.json',
  '../locales/zh-CN/admin.json'
])

export function normalizeLocale(val: unknown): SupportedLocale | null {
  if (typeof val !== 'string') return null
  const cleaned = val.trim().toLowerCase().replace('_', '-')
  if (cleaned === 'zh-cn' || cleaned === 'zh') {
    return 'zh-CN'
  }
  if (cleaned === 'zh-tw' || cleaned === 'zh-hk' || cleaned === 'zh-mo') {
    return 'zh-TW'
  }
  if (cleaned === 'en-us' || cleaned === 'en' || cleaned === 'en-gb') {
    return 'en-US'
  }
  return null
}

export function toUrlLang(locale: SupportedLocale): string {
  switch (locale) {
    case 'zh-TW':
      return 'zh-tw'
    case 'en-US':
      return 'en-us'
    case 'zh-CN':
    default:
      return 'zh-cn'
  }
}

export function resolveLocale(urlLang?: unknown, storeLocale?: unknown): SupportedLocale {
  const fromUrl = normalizeLocale(urlLang)
  if (fromUrl) {
    return fromUrl
  }
  const fromStore = normalizeLocale(storeLocale)
  if (fromStore) {
    return fromStore
  }
  return DEFAULT_LOCALE
}

export function getNamespaceForPath(filePath: string): LocaleNamespace {
  if (filePath.includes('/auth/')) {
    return 'auth'
  }
  if (filePath.includes('/captcha/')) {
    return 'captcha'
  }
  if (filePath.endsWith('/admin.json') || filePath.includes('/admin/')) {
    return 'admin'
  }
  if (filePath.endsWith('/competition.json') || filePath.includes('/competition/')) {
    return 'competition'
  }
  return 'common'
}

export function getNamespacesForRoute(routePath: string): LocaleNamespace[] {
  const namespaces: LocaleNamespace[] = ['common']
  if (
    routePath.startsWith('/v1/signin') ||
    routePath.startsWith('/v1/signup') ||
    routePath.startsWith('/v1/recovery') ||
    routePath.startsWith('/v1/info') ||
    routePath.startsWith('/accounts')
  ) {
    namespaces.push('auth', 'captcha')
  }
  if (routePath.startsWith('/admin')) {
    namespaces.push('admin')
  }
  if (routePath.startsWith('/competitions') || routePath.startsWith('/c/')) {
    namespaces.push('competition')
  }
  return namespaces
}

export async function loadLocaleMessagesAsync(
  locale: string,
  namespaces: LocaleNamespace | LocaleNamespace[] = ['common', 'auth', 'captcha']
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
    const matched = path.match(/\/locales\/[^/]+\/(.+)\.json$/i)
    if (matched && matched[1]) {
      const parts = matched[1].split('/')
      if (parts.length === 1) {
        messages[parts[0]] = content
      } else if (parts.length === 2) {
        const [dir, file] = parts
        if (dir === 'auth') {
          if (!messages.auth) messages.auth = {}
          if (file === 'auth') {
            Object.assign(messages.auth, content)
          } else {
            messages.auth[file] = content
          }
        } else if (dir === 'captcha') {
          if (!messages.captcha) messages.captcha = {}
          messages.captcha[file] = content
        } else {
          if (!messages[dir]) messages[dir] = {}
          messages[dir][file] = content
        }
      }
    }
  }

  ;(i18n.global as any).mergeLocaleMessage(locale, messages)
  if (locale === 'zh-CN') {
    ;(i18n.global as any).mergeLocaleMessage('zh', messages)
  }

  for (const p of pathsToLoad) {
    loadedPaths.add(p)
  }

  return (i18n.global as any).getLocaleMessage(locale) || {}
}

export async function loadLocaleForRouteAsync(locale: string, routePath: string): Promise<void> {
  const namespaces = getNamespacesForRoute(routePath)
  await loadLocaleMessagesAsync(locale, namespaces)
}

const initialSaved = typeof localStorage !== 'undefined' ? localStorage.getItem('volur_locale') : null
const initialLocale = resolveLocale(null, initialSaved)

export const i18n = createI18n({
  legacy: false,
  locale: initialLocale,
  fallbackLocale: {
    'zh-TW': ['zh-CN'],
    'zh': ['zh-CN'],
    default: ['zh-CN']
  },
  messages: {
    'zh-CN': {
      common: zhCnCommon,
      auth: zhCnAuth,
      captcha: zhCnCaptcha,
      competition: zhCnCompetition,
      admin: zhCnAdmin
    },
    'zh': {
      common: zhCnCommon,
      auth: zhCnAuth,
      captcha: zhCnCaptcha,
      competition: zhCnCompetition,
      admin: zhCnAdmin
    }
  }
})

export default i18n
