<template>
  <div class="auth-page-layout">
    <header class="top-bar">
      <router-link to="/" class="logo">
        <img
          :src="logoUrl"
          alt="Volur Lite"
          class="logo-img"
        />
      </router-link>
    </header>

    <main class="auth-content">
      <div class="auth-card-container">
        <router-view />
      </div>
    </main>

    <volur-footer mode="auth" />

    <div class="bottom-left-controls">
      <n-button
        strong
        secondary
        circle
        size="medium"
        @click="toggleLanguage"
        :title="locale === 'zh_CN' ? t('common.switch_to_english') : t('common.switch_to_chinese')"
        class="control-btn"
      >
        <template #icon>
          <span class="lang-text">{{ locale === 'zh_CN' ? 'EN' : '中' }}</span>
        </template>
      </n-button>

      <n-button
        strong
        secondary
        circle
        size="medium"
        @click="appStore.toggleTheme"
        :title="appStore.theme === 'dark' ? t('common.switch_light_mode') : t('common.switch_dark_mode')"
        class="control-btn"
      >
        <template #icon>
          <n-icon size="18">
            <SunnyOutline v-if="appStore.theme === 'dark'" />
            <MoonOutline v-else />
          </n-icon>
        </template>
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NButton, NIcon } from 'naive-ui'
import { SunnyOutline, MoonOutline } from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import VolurFooter from '../components/VolurFooter.vue'
import { useAppStore } from '@/store/modules/app'
import { useSystemStore } from '@/store/modules/system'

const { t, locale } = useI18n()
const appStore = useAppStore()
const systemStore = useSystemStore()

const logoUrl = computed(() => {
  if (appStore.theme === 'dark') {
    return systemStore.getSetting('PLATFORM_LOGO_DARK', '/long-logo-dark.svg')
  }
  return systemStore.getSetting('PLATFORM_LOGO', '/long-logo.svg')
})

function toggleLanguage() {
  const next = locale.value === 'zh_CN' ? 'en_US' : 'zh_CN'
  locale.value = next
  localStorage.setItem('locale', next)
}
</script>

<style scoped lang="scss">
.auth-page-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  background-color: #ffffff;
  color: var(--v-text-primary);
  transition: background-color 0.25s ease, color 0.25s ease;

  @include theme-dark {
    background-color: var(--v-bg-base);
  }
}

.top-bar {
  padding: 24px 32px;
  display: flex;
  align-items: center;

  .logo {
    display: inline-flex;
    align-items: center;
    text-decoration: none;
    transition: opacity 0.2s ease;

    &:hover {
      opacity: 0.85;
    }

    .logo-img {
      height: 28px;
      width: auto;
      max-width: 145px;
      object-fit: contain;
    }
  }
}

.auth-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;

  .auth-card-container {
    width: 100%;
    max-width: 440px;
    padding: 0 32px;
    box-sizing: border-box;
  }
}

.bottom-left-controls {
  position: fixed;
  bottom: 20px;
  left: 20px;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 10px;

  .control-btn {
    box-shadow: var(--v-shadow-md);

    .lang-text {
      font-size: 13px;
    }
  }
}

@media (max-width: 480px) {
  .top-bar {
    padding: 16px 20px;
  }

  .auth-content {
    align-items: flex-start;
    padding-top: 24px;

    .auth-card-container {
      padding: 0 16px;
    }
  }

  .bottom-left-controls {
    bottom: 12px;
    left: 12px;
  }
}
</style>
