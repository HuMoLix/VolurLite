<template>
  <n-config-provider
    :theme="naiveTheme"
    :theme-overrides="mergedThemeOverrides"
    :locale="computedNaiveLocale"
    :date-locale="computedNaiveDateLocale"
    v-bind="$attrs"
  >
    <slot />
  </n-config-provider>
</template>

<script setup lang="ts">
import { computed, provide, toRef } from 'vue'
import {
  NConfigProvider,
  darkTheme,
  zhCN,
  dateZhCN,
  zhTW,
  dateZhTW,
  enUS,
  dateEnUS,
  type GlobalThemeOverrides
} from 'naive-ui'
import { createVolurTheme } from '../_styles/theme'
import { VOLUR_CONTEXT_KEY } from './context'

defineOptions({
  name: 'VConfigProvider'
})

interface Props {
  theme?: 'light' | 'dark'
  locale?: string
  themeOverrides?: GlobalThemeOverrides
}

const props = withDefaults(defineProps<Props>(), {
  theme: 'light',
  locale: 'zh-CN',
  themeOverrides: undefined
})

const naiveTheme = computed(() => {
  return props.theme === 'dark' ? darkTheme : null
})

const computedNaiveLocale = computed(() => {
  switch (props.locale) {
    case 'zh-TW':
      return zhTW
    case 'en-US':
      return enUS
    case 'zh-CN':
    default:
      return zhCN
  }
})

const computedNaiveDateLocale = computed(() => {
  switch (props.locale) {
    case 'zh-TW':
      return dateZhTW
    case 'en-US':
      return dateEnUS
    case 'zh-CN':
    default:
      return dateZhCN
  }
})

const mergedThemeOverrides = computed<GlobalThemeOverrides>(() => {
  const baseOverrides = createVolurTheme(props.theme === 'dark')
  if (!props.themeOverrides) {
    return baseOverrides
  }
  return {
    ...baseOverrides,
    ...props.themeOverrides,
    common: {
      ...baseOverrides.common,
      ...props.themeOverrides.common
    }
  }
})

provide(VOLUR_CONTEXT_KEY, {
  theme: toRef(props, 'theme'),
  locale: toRef(props, 'locale'),
  themeOverrides: mergedThemeOverrides,
  naiveLocale: computedNaiveLocale,
  naiveDateLocale: computedNaiveDateLocale
})
</script>
