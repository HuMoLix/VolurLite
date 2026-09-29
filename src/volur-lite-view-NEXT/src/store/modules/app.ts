import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'dark' | 'light'

export const useAppStore = defineStore('app', () => {
  const savedTheme = (typeof localStorage !== 'undefined' ? localStorage.getItem('volur_theme') as ThemeMode : null) || 'light'
  const theme = ref<ThemeMode>(savedTheme)
  const isSidebarCollapsed = ref<boolean>(false)

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

  // Initial apply
  applyThemeToDom(theme.value)

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

  return {
    theme,
    isSidebarCollapsed,
    toggleTheme,
    setTheme,
    toggleSidebar
  }
})
