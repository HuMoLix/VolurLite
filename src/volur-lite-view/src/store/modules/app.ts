import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'dark' | 'light'

export const useAppStore = defineStore('app', () => {
  const savedTheme = (localStorage.getItem('volur_theme') as ThemeMode) || 'light'
  const theme = ref<ThemeMode>(savedTheme)
  const isSidebarCollapsed = ref<boolean>(false)

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

  return {
    theme,
    isSidebarCollapsed,
    toggleTheme,
    setTheme,
    toggleSidebar
  }
})
