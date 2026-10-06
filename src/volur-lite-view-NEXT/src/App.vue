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
              <transition name="lang-mask-fade">
                <div v-if="appStore.isLangSwitching" class="global-lang-loading-mask">
                  <div class="lang-loading-card">
                    <n-spin size="large" />
                    <span class="lang-loading-text">{{ appStore.langSwitchingText }}</span>
                  </div>
                </div>
              </transition>
            </naive-provider>
          </n-message-provider>
        </n-notification-provider>
      </n-dialog-provider>
    </n-loading-bar-provider>
    <n-global-style />
  </n-config-provider>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NConfigProvider,
  NLoadingBarProvider,
  NDialogProvider,
  NNotificationProvider,
  NMessageProvider,
  NGlobalStyle,
  NSpin,
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
import { useAppStore } from '@/store/modules/app'
import { initDeviceFingerprint } from '@/utils/fingerprint'
import { refreshTokenApi } from '@/api/auth'

const appStore = useAppStore()
const router = useRouter()
const route = useRoute()

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
          router.push({
            path: '/v1/signin/identifier',
            query: { redirect: route.fullPath }
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
      primaryColor: isDark ? '#8ab4f8' : '#1a73e8',
      primaryColorHover: isDark ? '#aecbfa' : '#1765cc',
      primaryColorPressed: isDark ? '#669df6' : '#1557b0',
      primaryColorSuppl: isDark ? '#8ab4f8' : '#4285f4',
      successColor: isDark ? '#81c995' : '#34a853',
      warningColor: isDark ? '#fdd663' : '#fbbc04',
      errorColor: isDark ? '#f28b82' : '#ea4335',
      infoColor: isDark ? '#78d9ec' : '#24c1e0',
      baseColor: isDark ? '#202124' : '#ffffff',
      bodyColor: isDark ? '#202124' : '#f8f9fa',
      cardColor: isDark ? '#292a2d' : '#ffffff',
      modalColor: isDark ? '#303134' : '#ffffff',
      popoverColor: isDark ? '#303134' : '#ffffff',
      tableColor: isDark ? '#292a2d' : '#ffffff',
      inputColor: isDark ? '#303134' : '#ffffff',
      borderColor: isDark ? '#3c4043' : '#dadce0',
      dividerColor: isDark ? '#3c4043' : '#e8eaed',
      textColorBase: isDark ? '#e8eaed' : '#202124',
      textColor1: isDark ? '#e8eaed' : '#202124',
      textColor2: isDark ? '#bdc1c6' : '#3c4043',
      textColor3: isDark ? '#9aa0a6' : '#5f6368',
      textColorDisabled: isDark ? '#80868b' : '#9aa0a6',
      placeholderColor: isDark ? '#80868b' : '#9aa0a6',
      borderRadius: '8px'
    },
    Button: {
      colorPrimary: isDark ? '#8ab4f8' : '#1a73e8',
      colorHoverPrimary: isDark ? '#aecbfa' : '#1765cc',
      colorPressedPrimary: isDark ? '#669df6' : '#1557b0',
      colorFocusPrimary: isDark ? '#aecbfa' : '#1765cc',
      textColorPrimary: isDark ? '#202124' : '#ffffff',
      textColorHoverPrimary: isDark ? '#202124' : '#ffffff',
      textColorPressedPrimary: isDark ? '#202124' : '#ffffff',
      textColorFocusPrimary: isDark ? '#202124' : '#ffffff',
      borderRadiusMedium: '8px',
      borderRadiusLarge: '8px'
    },
    Card: {
      color: isDark ? '#292a2d' : '#ffffff',
      borderColor: isDark ? '#3c4043' : '#dadce0',
      textColor: isDark ? '#bdc1c6' : '#3c4043',
      titleTextColor: isDark ? '#e8eaed' : '#202124'
    },
    Layout: {
      color: isDark ? '#202124' : '#f8f9fa',
      siderColor: isDark ? '#292a2d' : '#ffffff',
      headerColor: isDark ? '#292a2d' : '#ffffff',
      footerColor: isDark ? '#292a2d' : '#ffffff'
    },
    Form: {
      labelTextColor: isDark ? '#bdc1c6' : '#3c4043'
    }
  }
})
</script>

<style scoped lang="scss">
.global-lang-loading-mask {
  position: fixed;
  inset: 0;
  z-index: 99999;
  background-color: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: wait;
  user-select: none;
}

:global(html.dark) .global-lang-loading-mask {
  background-color: rgba(32, 33, 36, 0.75);
}

.lang-loading-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px 32px;
  background-color: var(--v-bg-card, #ffffff);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

:global(html.dark) .lang-loading-card {
  background-color: #292a2d;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
}

.lang-loading-text {
  font-size: 14px;
  color: var(--v-text-primary, #202124);
  font-weight: 500;
}

:global(html.dark) .lang-loading-text {
  color: #e8eaed;
}

.lang-mask-fade-enter-active,
.lang-mask-fade-leave-active {
  transition: opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.lang-mask-fade-enter-from,
.lang-mask-fade-leave-to {
  opacity: 0;
}
</style>
