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
              <VolurBroadcastModal />
              <!-- 全局多语言异步加载全屏遮罩 -->
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
import { computed, defineAsyncComponent, onMounted, watch } from 'vue'
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
  enUS,
  dateEnUS,
  type GlobalThemeOverrides
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import NaiveProvider from '@/components/common/NaiveProvider.vue'
import { useAppStore } from '@/store/modules/app'

// 精准异步懒加载广播弹窗，避开 volurEx 根入口 Barrel Export 雪崩
const VolurBroadcastModal = defineAsyncComponent(
  () => import('@/public/volurEx/components/VolurBroadcastModal.vue')
)
import { useSystemStore } from '@/store/modules/system'
import { useUserStore } from '@/store/modules/user'
import { useNotificationStore } from '@/store/modules/notification'

const { locale } = useI18n()
const router = useRouter()
const appStore = useAppStore()
const systemStore = useSystemStore()
const userStore = useUserStore()
const notificationStore = useNotificationStore()

const naiveLocale = computed(() => (locale.value === 'en_US' || locale.value === 'en' ? enUS : zhCN))
const naiveDateLocale = computed(() => (locale.value === 'en_US' || locale.value === 'en' ? dateEnUS : dateZhCN))

// 全局监听登录态：用户登录后初始化全局 SSE；退出登录时安全关闭
watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      notificationStore.initSse()
    } else {
      notificationStore.destroySse()
    }
  }
)

onMounted(async () => {
  if (!systemStore.isLoaded) {
    await systemStore.fetchPublicSettings()
  }
  if (userStore.isLoggedIn) {
    userStore.fetchCurrentUser().catch(() => {})
    // 用户携带登录凭证刷新/进入网页时初始化全局 SSE
    notificationStore.initSse()
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

<style lang="scss" scoped>
.global-lang-loading-mask {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  user-select: none;

  @include theme-dark {
    background-color: rgba(11, 12, 16, 0.88);
  }
}

.lang-loading-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 28px 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.05);

  @include theme-dark {
    background: rgba(21, 24, 33, 0.7);
    border: 1px solid rgba(255, 255, 255, 0.08);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.35);
  }
}

.lang-loading-text {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.2px;
  color: #444444;

  @include theme-dark {
    color: #e2e8f0;
  }
}

.lang-mask-fade-enter-active,
.lang-mask-fade-leave-active {
  transition: opacity 0.25s ease;
}

.lang-mask-fade-enter-from,
.lang-mask-fade-leave-to {
  opacity: 0;
}
</style>

