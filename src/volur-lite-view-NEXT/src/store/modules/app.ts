import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import {
  type SupportedLocale,
  DEFAULT_LOCALE,
  resolveLocale,
  normalizeLocale,
  loadLocaleForRouteAsync,
  i18n
} from '@/i18n'

export type ThemeMode = 'dark' | 'light'

export const useAppStore = defineStore('app', () => {
  const savedTheme = (typeof localStorage !== 'undefined' ? localStorage.getItem('volur_theme') as ThemeMode : null) || 'light'
  const theme = ref<ThemeMode>(savedTheme)
  const isSidebarCollapsed = ref<boolean>(false)

  const savedLocale = typeof localStorage !== 'undefined' ? localStorage.getItem('volur_locale') : null
  const locale = ref<SupportedLocale>(resolveLocale(null, savedLocale))

  const isLangSwitching = ref<boolean>(false)
  const langSwitchingText = ref<string>('正在加载语言资源...')

  function updateFavicon(mode: ThemeMode) {
    if (typeof document !== 'undefined') {
      let link = document.querySelector<HTMLLinkElement>("link[rel~='icon']")
      if (!link) {
        link = document.createElement('link')
        link.rel = 'icon'
        link.type = 'image/svg+xml'
        document.head.appendChild(link)
      }
      link.href = mode === 'dark' ? '/r/static/svg/favicon-dark.svg' : '/r/static/svg/favicon.svg'
    }
  }

  function applyThemeToDom(mode: ThemeMode) {
    if (typeof document !== 'undefined') {
      const root = document.documentElement
      if (mode === 'dark') {
        root.classList.add('dark')
      } else {
        root.classList.remove('dark')
      }
      root.setAttribute('data-theme', mode)
      updateFavicon(mode)
    }
  }

  function applyLocaleToDom(targetLocale: SupportedLocale) {
    if (typeof document !== 'undefined') {
      document.documentElement.lang = targetLocale
    }
  }

  applyThemeToDom(theme.value)
  applyLocaleToDom(locale.value)

  const initialPath = typeof window !== 'undefined' ? window.location.pathname : '/'
  loadLocaleForRouteAsync(locale.value, initialPath).catch(() => {})

  watch(theme, (newTheme) => {
    applyThemeToDom(newTheme)
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('volur_theme', newTheme)
    }
  })

  function toggleTheme(): void {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  function setTheme(mode: ThemeMode): void {
    theme.value = mode
  }

  function toggleSidebar(): void {
    isSidebarCollapsed.value = !isSidebarCollapsed.value
  }

  function setLocale(target: SupportedLocale | string, persist = true): void {
    const resolved = normalizeLocale(target) || DEFAULT_LOCALE
    locale.value = resolved
    if (i18n.global.locale) {
      ;(i18n.global.locale as any).value = resolved
    }
    applyLocaleToDom(resolved)
    if (persist && typeof localStorage !== 'undefined') {
      localStorage.setItem('volur_locale', resolved)
    }
  }

  async function switchLanguage(targetLocale: SupportedLocale | string, routePath?: string): Promise<void> {
    const resolved = normalizeLocale(targetLocale) || DEFAULT_LOCALE
    if (isLangSwitching.value) return
    isLangSwitching.value = true
    langSwitchingText.value =
      resolved === 'en-US'
        ? 'Loading language resources...'
        : resolved === 'zh-TW'
          ? '正在載入語言資源...'
          : '正在加载语言资源...'

    try {
      const path = routePath || (typeof window !== 'undefined' ? window.location.pathname : '/')
      await loadLocaleForRouteAsync(resolved, path)
      setLocale(resolved, true)
      await new Promise((resolve) => setTimeout(resolve, 200))
    } finally {
      isLangSwitching.value = false
    }
  }

  return {
    theme,
    locale,
    isSidebarCollapsed,
    isLangSwitching,
    langSwitchingText,
    toggleTheme,
    setTheme,
    toggleSidebar,
    setLocale,
    switchLanguage
  }
})
