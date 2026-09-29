<template>
  <n-config-provider
    :theme="theme"
    :theme-overrides="themeOverrides"
    :locale="zhCN"
    :date-locale="dateZhCN"
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
  type GlobalThemeOverrides
} from 'naive-ui'
import NaiveProvider from '@/components/common/NaiveProvider.vue'
import { useAppStore } from '@/store/modules/app'
import { initDeviceFingerprint } from '@/utils/fingerprint'

const appStore = useAppStore()

onMounted(() => {
  const launchRiskProbe = () => {
    initDeviceFingerprint().catch(() => {
    })
  }

  if (typeof window !== 'undefined' && 'requestIdleCallback' in window) {
    window.requestIdleCallback(() => launchRiskProbe(), { timeout: 2500 })
  } else {
    setTimeout(launchRiskProbe, 120)
  }
})

const theme = computed(() => (appStore.theme === 'dark' ? darkTheme : null))

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

<style>
/* Base application styling handled by styles/index.scss */
</style>
