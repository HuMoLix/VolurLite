<template>
  <v-config-provider
    :theme="theme"
    :locale="locale"
    :theme-overrides="themeOverrides"
  >
    <v-loading-bar-provider>
      <v-dialog-provider>
        <v-notification-provider>
          <v-message-provider>
            <slot />
          </v-message-provider>
        </v-notification-provider>
      </v-dialog-provider>
    </v-loading-bar-provider>
    <v-global-style />
  </v-config-provider>
</template>

<script setup lang="ts">
import type { GlobalThemeOverrides } from 'naive-ui'
import VConfigProvider from './VConfigProvider.vue'
import VLoadingBarProvider from './VLoadingBarProvider.vue'
import VDialogProvider from './VDialogProvider.vue'
import VNotificationProvider from './VNotificationProvider.vue'
import VMessageProvider from './VMessageProvider.vue'
import VGlobalStyle from './VGlobalStyle.vue'

defineOptions({
  name: 'VolurConfigProvider'
})

interface Props {
  theme?: 'light' | 'dark'
  locale?: string
  themeOverrides?: GlobalThemeOverrides
}

withDefaults(defineProps<Props>(), {
  theme: 'light',
  locale: 'zh-CN',
  themeOverrides: undefined
})
</script>
