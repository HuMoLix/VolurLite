<template>
  <slot />
</template>

<script setup lang="ts">
import { onBeforeMount, onUnmounted, watchEffect } from 'vue'
import { useVolurTheme } from '../_mixins/use-theme'

defineOptions({
  name: 'VGlobalStyle'
})

const { isDark } = useVolurTheme()

onBeforeMount(() => {
  if (typeof document === 'undefined') return

  const root = document.documentElement
  const body = document.body

  watchEffect(() => {
    const mode = isDark.value ? 'dark' : 'light'
    root.style.colorScheme = mode
    root.setAttribute('data-theme', mode)

    if (isDark.value) {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }

    body.style.backgroundColor = 'var(--color-v1-surface)'
    body.style.color = 'var(--color-v1-text)'
    body.style.transition = 'background-color 0.25s ease, color 0.25s ease'
    body.setAttribute('volur-styled', '')
  })
})

onUnmounted(() => {
  if (typeof document !== 'undefined') {
    document.body.removeAttribute('volur-styled')
  }
})
</script>
