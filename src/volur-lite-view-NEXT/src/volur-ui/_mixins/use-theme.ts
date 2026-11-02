import { inject, computed } from 'vue'
import { VOLUR_CONTEXT_KEY } from '../provider/context'

export function useVolurTheme() {
  const context = inject(VOLUR_CONTEXT_KEY, null)

  const isDark = computed(() => {
    return context?.theme.value === 'dark'
  })

  return {
    context,
    isDark
  }
}
