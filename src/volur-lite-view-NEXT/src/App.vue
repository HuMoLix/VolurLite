<template>
  <v-config-provider
    :theme="appStore.theme"
    :locale="appStore.locale"
  >
    <v-loading-bar-provider>
      <v-dialog-provider>
        <v-notification-provider>
          <v-message-provider>
            <router-view />
            <LanguageSwitcher />
          </v-message-provider>
        </v-notification-provider>
      </v-dialog-provider>
    </v-loading-bar-provider>
    <v-global-style />
  </v-config-provider>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, watchEffect } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  VConfigProvider,
  VLoadingBarProvider,
  VDialogProvider,
  VNotificationProvider,
  VMessageProvider,
  VGlobalStyle
} from '@/volur-ui'
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue'
import { useAppStore } from '@/store/modules/app'
import { useSystemStore } from '@/store/modules/system'
import { initDeviceFingerprint } from '@/utils/fingerprint'
import { refreshTokenApi } from '@/api/auth'
import { formatPageTitle } from '@/utils/title'

const appStore = useAppStore()
const systemStore = useSystemStore()
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

  if (!systemStore.isLoaded) {
    systemStore.fetchPublicSettings().catch(() => {})
  }
})

onUnmounted(() => {
  if (tokenRefreshTimer) {
    clearInterval(tokenRefreshTimer)
    tokenRefreshTimer = null
  }
})
</script>

