<template>
  <n-config-provider
    :theme="theme"
    :theme-overrides="themeOverrides"
    :locale="naiveLocale"
    :date-locale="naiveDateLocale"
  >
    <n-loading-bar-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <n-message-provider>
            <naive-provider>
              <router-view />
            </naive-provider>
          </n-message-provider>
        </n-notification-provider>
      </n-dialog-provider>
    </n-loading-bar-provider>
    <n-global-style />
  </n-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import {
  NConfigProvider,
  NLoadingBarProvider,
  NDialogProvider,
  NNotificationProvider,
  NMessageProvider,
  NGlobalStyle,
  darkTheme,
  zhCN,
  dateZhCN,
  enUS,
  dateEnUS,
  type GlobalThemeOverrides
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import NaiveProvider from '@/components/common/NaiveProvider.vue'
import { useAppStore } from '@/store/modules/app'
import { useSystemStore } from '@/store/modules/system'
import { useUserStore } from '@/store/modules/user'

const { locale } = useI18n()
const router = useRouter()
const appStore = useAppStore()
const systemStore = useSystemStore()
const userStore = useUserStore()

const naiveLocale = computed(() => (locale.value === 'en_US' || locale.value === 'en' ? enUS : zhCN))
const naiveDateLocale = computed(() => (locale.value === 'en_US' || locale.value === 'en' ? dateEnUS : dateZhCN))

onMounted(async () => {
  if (!systemStore.isLoaded) {
    await systemStore.fetchPublicSettings()
  }
  if (userStore.isLoggedIn && !userStore.user) {
    try {
      await userStore.fetchUserProfile()
    } catch {
      // ignore
    }
  }
  if (systemStore.isMaintenanceMode && !userStore.isAdmin) {
    const currentPath = window.location.pathname
    if (currentPath !== '/maintenance' && !currentPath.startsWith('/auth') && !currentPath.startsWith('/admin')) {
      router.push('/maintenance')
    }
  }
})

const theme = computed(() => (appStore.theme === 'dark' ? darkTheme : null))

const themeOverrides = computed<GlobalThemeOverrides>(() => {
  const isDark = appStore.theme === 'dark'
  return {
    common: {
      primaryColor: isDark ? '#ffffff' : '#000000',
      primaryColorHover: isDark ? '#e5e5e5' : '#222222',
      primaryColorPressed: isDark ? '#cccccc' : '#333333',
      primaryColorSuppl: isDark ? '#ffffff' : '#000000',
      textColor2: isDark ? '#9ca3af' : '#444444',
      textColor3: isDark ? '#6b7280' : '#666666',
      borderRadius: '8px'
    },
    Button: {
      colorPrimary: isDark ? '#ffffff' : '#000000',
      colorHoverPrimary: isDark ? '#e5e5e5' : '#222222',
      colorPressedPrimary: isDark ? '#cccccc' : '#333333',
      colorFocusPrimary: isDark ? '#e5e5e5' : '#222222',
      textColorPrimary: isDark ? '#000000' : '#ffffff',
      textColorHoverPrimary: isDark ? '#000000' : '#ffffff',
      textColorPressedPrimary: isDark ? '#000000' : '#ffffff',
      textColorFocusPrimary: isDark ? '#000000' : '#ffffff',
      borderRadiusMedium: '8px',
      borderRadiusLarge: '8px'
    },
    Form: {
      labelTextColor: isDark ? '#cbd5e1' : '#444444'
    }
  }
})
</script>

<style>
/* Base app styles handled by styles/index.scss */
</style>
