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
  if (userStore.isLoggedIn) {
    userStore.fetchCurrentUser().catch(() => {})
  }
  if (systemStore.isMaintenanceMode && !userStore.isAdmin) {
    const currentPath = window.location.pathname
    if (currentPath !== '/maintenance' && !currentPath.startsWith('/auth') && !currentPath.startsWith('/admin')) {
      await router.push('/maintenance')
    }
  }
})

const theme = computed(() => (appStore.theme === 'dark' ? darkTheme : null))

const themeOverrides = computed<GlobalThemeOverrides>(() => {
  const isDark = appStore.theme === 'dark'
  return {
    common: {
      primaryColor: isDark ? '#ffffff' : '#000000',
      primaryColorHover: isDark ? '#e2e8f0' : '#222222',
      primaryColorPressed: isDark ? '#cbd5e1' : '#333333',
      primaryColorSuppl: isDark ? '#ffffff' : '#000000',
      baseColor: isDark ? '#0b0c10' : '#ffffff',
      bodyColor: isDark ? '#0b0c10' : '#f8f9fa',
      cardColor: isDark ? '#151821' : '#ffffff',
      modalColor: isDark ? '#1b1e2a' : '#ffffff',
      popoverColor: isDark ? '#1b1e2a' : '#ffffff',
      tableColor: isDark ? '#151821' : '#ffffff',
      inputColor: isDark ? '#11131a' : '#ffffff',
      borderColor: isDark ? 'rgba(255, 255, 255, 0.09)' : '#e5e4e7',
      dividerColor: isDark ? 'rgba(255, 255, 255, 0.07)' : '#f0eff2',
      textColorBase: isDark ? '#f8fafc' : '#111111',
      textColor1: isDark ? '#f8fafc' : '#111111',
      textColor2: isDark ? '#94a3b8' : '#444444',
      textColor3: isDark ? '#64748b' : '#666666',
      textColorDisabled: isDark ? '#475569' : '#a1a1aa',
      placeholderColor: isDark ? '#64748b' : '#999999',
      borderRadius: '8px'
    },
    Button: {
      colorPrimary: isDark ? '#ffffff' : '#000000',
      colorHoverPrimary: isDark ? '#e2e8f0' : '#222222',
      colorPressedPrimary: isDark ? '#cbd5e1' : '#333333',
      colorFocusPrimary: isDark ? '#e2e8f0' : '#222222',
      textColorPrimary: isDark ? '#0b0c10' : '#ffffff',
      textColorHoverPrimary: isDark ? '#0b0c10' : '#ffffff',
      textColorPressedPrimary: isDark ? '#0b0c10' : '#ffffff',
      textColorFocusPrimary: isDark ? '#0b0c10' : '#ffffff',
      borderRadiusMedium: '8px',
      borderRadiusLarge: '8px'
    },
    Card: {
      color: isDark ? '#151821' : '#ffffff',
      borderColor: isDark ? 'rgba(255, 255, 255, 0.09)' : '#e5e4e7',
      textColor: isDark ? '#94a3b8' : '#444444',
      titleTextColor: isDark ? '#f8fafc' : '#111111'
    },
    Layout: {
      color: isDark ? '#0b0c10' : '#f8f9fa',
      siderColor: isDark ? '#121826' : '#ffffff',
      headerColor: isDark ? '#121826' : '#ffffff',
      footerColor: isDark ? '#121826' : '#ffffff'
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
