import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import i18n, { loadLocaleForRouteAsync } from '@/i18n'

export type ThemeMode = 'dark' | 'light'

export const useAppStore = defineStore('app', () => {
  const savedTheme = (localStorage.getItem('volur_theme') as ThemeMode) || 'light'
  const theme = ref<ThemeMode>(savedTheme)
  const isSidebarCollapsed = ref<boolean>(false)

  // 全局多语言动态切换加载遮罩状态
  const isLangSwitching = ref<boolean>(false)
  const langSwitchingText = ref<string>('正在切换语言...')

  function applyThemeToDom(mode: ThemeMode) {
    if (typeof document !== 'undefined') {
      const root = document.documentElement
      if (mode === 'dark') {
        root.classList.add('dark')
      } else {
        root.classList.remove('dark')
      }
      root.setAttribute('data-theme', mode)
    }
  }

  // Initial apply
  applyThemeToDom(theme.value)

  watch(theme, (newTheme) => {
    applyThemeToDom(newTheme)
    localStorage.setItem('volur_theme', newTheme)
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

  /**
   * 异步切换全局语言：开启全屏 Loading 遮罩，加载完成后展示页面
   */
  async function switchLanguage(targetLocale: 'zh_CN' | 'en_US' | string): Promise<void> {
    if (isLangSwitching.value) return
    isLangSwitching.value = true
    langSwitchingText.value =
      targetLocale === 'zh_CN' ? '正在加载语言资源...' : 'Loading language resources...'

    try {
      const currentPath = typeof window !== 'undefined' ? window.location.pathname : '/'
      await loadLocaleForRouteAsync(targetLocale, currentPath)
      ;(i18n.global as any).locale.value = targetLocale
      localStorage.setItem('locale', targetLocale)
      // 平滑视觉过渡
      await new Promise((resolve) => setTimeout(resolve, 200))
    } catch (err) {
      console.error('[i18n] 切换多语言失败', err)
    } finally {
      isLangSwitching.value = false
    }
  }

  return {
    theme,
    isSidebarCollapsed,
    isLangSwitching,
    langSwitchingText,
    toggleTheme,
    setTheme,
    toggleSidebar,
    switchLanguage
  }
})
