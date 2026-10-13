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
              <LanguageSwitcher />
            </naive-provider>
          </n-message-provider>
        </n-notification-provider>
      </n-dialog-provider>
    </n-loading-bar-provider>
    <n-global-style />
  </n-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, watchEffect } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
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
  zhTW,
  dateZhTW,
  enUS,
  dateEnUS,
  type GlobalThemeOverrides
} from 'naive-ui'
import NaiveProvider from '@/components/common/NaiveProvider.vue'
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue'
import { useAppStore } from '@/store/modules/app'
import { initDeviceFingerprint } from '@/utils/fingerprint'
import { refreshTokenApi } from '@/api/auth'
import { formatPageTitle } from '@/utils/title'

const appStore = useAppStore()
const router = useRouter()
const route = useRoute()
const { t } = useI18n()

watchEffect(() => {
  if (typeof document === 'undefined') return
  const titleKey = route.meta?.titleKey as string | undefined
  const rawTitle = titleKey ? t(titleKey) : (route.meta?.title as string | undefined)
  document.title = formatPageTitle(rawTitle)
})

let tokenRefreshTimer: ReturnType<typeof setInterval> | null = null
let isTokenRenewing = false

function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null
  const match = document.cookie.match(new RegExp('(?:^|;\\s*)' + name + '=([^;]*)'))
  return match ? decodeURIComponent(match[1]) : null
}

async function inspectAndRenewToken() {
  if (isTokenRenewing) return

  const isLoggedIn = getCookie('logged_in') === 'true'
  if (!isLoggedIn) return

  const expStr = getCookie('access_exp')
  if (!expStr) return

  const exp = Number(expStr)
  if (isNaN(exp)) return

  const now = Date.now()
  const remain = exp - now
  const THRESHOLD = 5 * 60 * 1000

  if (remain < THRESHOLD) {
    isTokenRenewing = true
    try {
      await refreshTokenApi()
    } catch {
      const latestExpStr = getCookie('access_exp')
      const latestExp = latestExpStr ? Number(latestExpStr) : 0
      const latestRemain = latestExp > 0 ? latestExp - Date.now() : 0

      if (latestRemain <= 0) {
        const path = route.path
        if (
          !path.startsWith('/v1/signin') &&
          !path.startsWith('/v1/signup') &&
          path !== '/v1/info/sessionexpire'
        ) {
          await router.push({
            path: '/v1/signin/identifier',
            query: {redirect: route.fullPath}
          })
        }
      }
    } finally {
      isTokenRenewing = false
    }
  }
}

onMounted(() => {
  const launchRiskProbe = () => {
    initDeviceFingerprint().catch(() => {})
  }

  if (typeof window !== 'undefined' && 'requestIdleCallback' in window) {
    window.requestIdleCallback(() => launchRiskProbe(), { timeout: 2500 })
  } else {
    setTimeout(launchRiskProbe, 120)
  }

  inspectAndRenewToken()
  tokenRefreshTimer = setInterval(inspectAndRenewToken, 15000)
})

onUnmounted(() => {
  if (tokenRefreshTimer) {
    clearInterval(tokenRefreshTimer)
    tokenRefreshTimer = null
  }
})

const theme = computed(() => (appStore.theme === 'dark' ? darkTheme : null))

const naiveLocale = computed(() => {
  switch (appStore.locale) {
    case 'zh-TW':
      return zhTW
    case 'en-US':
      return enUS
    case 'zh-CN':
    default:
      return zhCN
  }
})

const naiveDateLocale = computed(() => {
  switch (appStore.locale) {
    case 'zh-TW':
      return dateZhTW
    case 'en-US':
      return dateEnUS
    case 'zh-CN':
    default:
      return dateZhCN
  }
})

const themeOverrides = computed<GlobalThemeOverrides>(() => {
  const isDark = appStore.theme === 'dark'
  return {
    common: {
      primaryColor: isDark ? '#a8c7fa' : '#1a73e8',
      primaryColorHover: isDark ? '#b6d0f8' : '#1765cc',
      primaryColorPressed: isDark ? '#a8c7fa' : '#1557b0',
      primaryColorSuppl: isDark ? '#a8c7fa' : '#4285f4',
      successColor: isDark ? '#81c995' : '#34a853',
      warningColor: isDark ? '#fdd663' : '#fbbc04',
      errorColor: isDark ? '#f28b82' : '#ea4335',
      infoColor: isDark ? '#78d9ec' : '#24c1e0',
      baseColor: isDark ? '#1e1f20' : '#ffffff',
      bodyColor: isDark ? '#1e1f20' : '#f8f9fa',
      cardColor: isDark ? '#0e0e0e' : '#ffffff',
      modalColor: isDark ? '#303134' : '#ffffff',
      popoverColor: isDark ? '#303134' : '#ffffff',
      tableColor: isDark ? '#292a2d' : '#ffffff',
      inputColor: isDark ? '#303134' : '#ffffff',
      borderColor: isDark ? '#3c4043' : '#dadce0',
      dividerColor: isDark ? '#3c4043' : '#e8eaed',
      textColorBase: isDark ? '#e3e3e3' : '#202124',
      textColor1: isDark ? '#e3e3e3' : '#202124',
      textColor2: isDark ? '#bdc1c6' : '#3c4043',
      textColor3: isDark ? '#9aa0a6' : '#5f6368',
      textColorDisabled: isDark ? '#80868b' : '#9aa0a6',
      placeholderColor: isDark ? '#80868b' : '#9aa0a6',
      borderRadius: '8px'
    },
    Button: {
      colorPrimary: isDark ? '#a8c7fa' : '#1a73e8',
      colorHoverPrimary: isDark ? '#b6d0f8' : '#1765cc',
      colorPressedPrimary: isDark ? '#a8c7fa' : '#1557b0',
      colorFocusPrimary: isDark ? '#b6d0f8' : '#1765cc',
      textColorPrimary: isDark ? '#062e6f' : '#ffffff',
      textColorHoverPrimary: isDark ? '#062e6f' : '#ffffff',
      textColorPressedPrimary: isDark ? '#062e6f' : '#ffffff',
      textColorFocusPrimary: isDark ? '#062e6f' : '#ffffff',
      borderRadiusMedium: '8px',
      borderRadiusLarge: '8px'
    },
    Card: {
      color: isDark ? '#0e0e0e' : '#ffffff',
      borderColor: isDark ? '#3c4043' : '#dadce0',
      textColor: isDark ? '#bdc1c6' : '#3c4043',
      titleTextColor: isDark ? '#e3e3e3' : '#202124'
    },
    Layout: {
      color: isDark ? '#1e1f20' : '#f8f9fa',
      siderColor: isDark ? '#292a2d' : '#ffffff',
      headerColor: isDark ? '#292a2d' : '#ffffff',
      footerColor: isDark ? '#292a2d' : '#ffffff'
    },
    Form: {
      labelTextColor: isDark ? '#bdc1c6' : '#3c4043'
    },
    Modal: {
      maskColor: isDark ? 'rgba(0, 0, 0, 0.6)' : 'rgba(0, 0, 0, 0.4)'
    },
    Tooltip: {
      color: isDark ? '#292a2d' : '#ffffff',
      textColor: isDark ? '#bdc1c6' : '#2b2d31',
      boxShadow: isDark
        ? '0 1px 3px 0 rgba(0, 0, 0, 0.4), 0 1px 2px -1px rgba(0, 0, 0, 0.4)'
        : '0 1px 3px 0 rgba(10, 13, 18, 0.1), 0 1px 2px -1px rgba(10, 13, 18, 0.1)',
      padding: '8px 12px',
      borderRadius: '8px'
    }
  }
})
</script>

