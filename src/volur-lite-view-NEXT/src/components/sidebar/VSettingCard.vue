<template>
  <VCard
    class="v-setting-card"
    :theme="currentTheme"
    :width="width"
    :padding="'6px'"
    :gap="2"
  >
    <div class="setting-list">
      <div
        class="setting-item"
        role="button"
        tabindex="0"
        @click="handleUserSettingsClick"
      >
        <div class="item-left">
          <n-icon :size="18" class="item-icon">
            <SettingsOutline />
          </n-icon>
          <span class="item-label">{{ t('common.user_settings', 'User settings') }}</span>
        </div>
      </div>

      <div
        v-if="userStore.isAdmin"
        class="setting-item"
        role="button"
        tabindex="0"
        @click="handleAdminClick"
      >
        <div class="item-left">
          <n-icon :size="18" class="item-icon">
            <SpeedometerOutline />
          </n-icon>
          <span class="item-label">{{ t('common.platform_admin', '平台管理') }}</span>
        </div>
      </div>

      <n-popover
        v-model:show="isLanguageMenuShow"
        trigger="hover"
        placement="right-start"
        :show-arrow="false"
        raw
        style="box-shadow: none;"
        :delay="100"
        :duration="150"
      >
        <template #trigger>
          <div
            class="setting-item has-submenu"
            :class="{ 'is-active': isLanguageMenuShow }"
            role="button"
            tabindex="0"
          >
            <div class="item-left">
              <n-icon :size="18" class="item-icon">
                <GlobeOutline />
              </n-icon>
              <span class="item-label">{{ t('common.language_settings', 'Language settings') }}</span>
            </div>
            <n-icon :size="14" class="item-arrow">
              <IosArrowForward />
            </n-icon>
          </div>
        </template>

        <VCard
          class="v-setting-submenu-card"
          :theme="currentTheme"
          :width="150"
          :padding="'6px'"
          :gap="2"
        >
          <div class="submenu-list">
            <div
              v-for="item in languageOptions"
              :key="item.key"
              class="submenu-item"
              :class="{ 'is-selected': currentLocale === item.key }"
              role="button"
              tabindex="0"
              @click="handleSelectLanguage(item.key)"
            >
              <span class="submenu-label">{{ item.label }}</span>
              <n-icon v-if="currentLocale === item.key" :size="16" class="submenu-check">
                <CheckmarkOutline />
              </n-icon>
            </div>
          </div>
        </VCard>
      </n-popover>

      <div class="setting-divider" />

      <div
        class="setting-item"
        role="button"
        tabindex="0"
        @click="handleFeedbackClick"
      >
        <div class="item-left">
          <n-icon :size="18" class="item-icon">
            <FlagOutline />
          </n-icon>
          <span class="item-label">{{ t('common.send_feedback', 'Send feedback') }}</span>
        </div>
      </div>

      <div
        class="setting-item"
        role="button"
        tabindex="0"
        @click="handleTermsClick"
      >
        <div class="item-left">
          <n-icon :size="18" class="item-icon">
            <DocumentTextOutline />
          </n-icon>
          <span class="item-label">{{ t('common.terms_of_service', 'Terms of service') }}</span>
        </div>
      </div>

      <div
        class="setting-item"
        role="button"
        tabindex="0"
        @click="handlePrivacyClick"
      >
        <div class="item-left">
          <n-icon :size="18" class="item-icon">
            <ShieldCheckmarkOutline />
          </n-icon>
          <span class="item-label">{{ t('common.privacy_policy', 'Privacy policy') }}</span>
        </div>
      </div>
    </div>
  </VCard>

  <VFeedback
    v-model:show="isFeedbackDrawerShow"
    :theme="currentTheme"
    @submit="handleFeedbackSubmit"
  />
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon, NPopover } from 'naive-ui'
import {
  SettingsOutline,
  SpeedometerOutline,
  GlobeOutline,
  FlagOutline,
  DocumentTextOutline,
  ShieldCheckmarkOutline,
  CheckmarkOutline
} from '@vicons/ionicons5'
import { IosArrowForward } from '@vicons/ionicons4'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import type { SupportedLocale } from '@/i18n'
import VCard from './VCard.vue'
import VFeedback from './VFeedback.vue'
import type { VSettingCardProps } from './types'

defineOptions({
  name: 'VSettingCard'
})

const props = withDefaults(defineProps<VSettingCardProps>(), {
  theme: undefined,
  width: 240,
  userSettingsRoute: '/settings',
  adminRoute: '/admin/dashboard',
  feedbackRoute: '',
  termsRoute: '/accounts/terms',
  privacyRoute: '/accounts/privacy',
  closeAriaLabel: ''
})

const emit = defineEmits<{
  close: []
  'item-click': [key: string]
  'user-settings-click': [ev: MouseEvent]
  'admin-click': [ev: MouseEvent]
  'language-change': [locale: SupportedLocale]
  'feedback-click': [ev: MouseEvent]
  'feedback-submit': [payload: { content: string }]
  'terms-click': [ev: MouseEvent]
  'privacy-click': [ev: MouseEvent]
}>()

const router = useRouter()
const { t } = useI18n()
const appStore = useAppStore()
const userStore = useUserStore()

const isLanguageMenuShow = ref<boolean>(false)
const isFeedbackDrawerShow = ref<boolean>(false)

const currentTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const currentLocale = computed<SupportedLocale>(() => {
  return appStore.locale
})

const languageOptions: { key: SupportedLocale; label: string }[] = [
  { key: 'zh-CN', label: '简体中文' },
  { key: 'zh-TW', label: '繁體中文' },
  { key: 'en-US', label: 'English' }
]

function handleUserSettingsClick(ev: MouseEvent) {
  emit('item-click', 'user-settings')
  emit('user-settings-click', ev)
  if (props.userSettingsRoute) {
    router.push(props.userSettingsRoute).catch(() => {})
  }
  emit('close')
}

function handleAdminClick(ev: MouseEvent) {
  emit('item-click', 'admin')
  emit('admin-click', ev)
  if (props.adminRoute) {
    router.push(props.adminRoute).catch(() => {})
  }
  emit('close')
}

async function handleSelectLanguage(langKey: SupportedLocale) {
  isLanguageMenuShow.value = false
  if (appStore.locale !== langKey) {
    const currentRoutePath = router.currentRoute.value.path
    await appStore.switchLanguage(langKey, currentRoutePath)
    emit('language-change', langKey)
  }
  emit('close')
}

function handleFeedbackClick(ev: MouseEvent) {
  emit('item-click', 'feedback')
  emit('feedback-click', ev)
  if (props.feedbackRoute) {
    router.push(props.feedbackRoute).catch(() => {})
  } else {
    isFeedbackDrawerShow.value = true
  }
  emit('close')
}

function handleFeedbackSubmit(payload: { content: string }) {
  emit('feedback-submit', payload)
}

function handleTermsClick(ev: MouseEvent) {
  emit('item-click', 'terms')
  emit('terms-click', ev)
  if (props.termsRoute) {
    router.push(props.termsRoute).catch(() => {})
  }
  emit('close')
}

function handlePrivacyClick(ev: MouseEvent) {
  emit('item-click', 'privacy')
  emit('privacy-click', ev)
  if (props.privacyRoute) {
    router.push(props.privacyRoute).catch(() => {})
  }
  emit('close')
}

defineExpose({
  isFeedbackDrawerShow,
  openFeedback: () => {
    isFeedbackDrawerShow.value = true
  },
  closeFeedback: () => {
    isFeedbackDrawerShow.value = false
  }
})

</script>

<style scoped lang="scss">
:global(.n-popover:has(.v-setting-card)),
:global(.n-popover:has(.v-setting-submenu-card)) {
  box-shadow: none !important;
  background-color: transparent !important;
}

.v-setting-card {
  user-select: none;

  .setting-list {
    display: flex;
    flex-direction: column;
    width: 100%;
    gap: 2px;
  }

  .setting-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 38px;
    padding: 0 10px;
    border-radius: 8px;
    cursor: pointer;
    color: #3c4043;
    transition: background-color 0.15s cubic-bezier(0.2, 0, 0, 1),
                color 0.15s cubic-bezier(0.2, 0, 0, 1);
    outline: none;

    &:hover,
    &.is-active {
      background-color: rgba(60, 64, 67, 0.08);
      color: #202124;

      .item-icon {
        color: #202124;
      }

      .item-arrow {
        color: #202124;
      }
    }

    &:active {
      background-color: rgba(60, 64, 67, 0.12);
    }

    .item-left {
      display: flex;
      align-items: center;
      gap: 12px;
      min-width: 0;
    }

    .item-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 18px;
      height: 18px;
      color: #5f6368;
      flex-shrink: 0;
      transition: color 0.15s cubic-bezier(0.2, 0, 0, 1);
    }

    .item-label {
      font-size: 13.5px;
      font-weight: 500;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      line-height: 1.4;
    }

    .item-arrow {
      color: #80868b;
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: color 0.15s cubic-bezier(0.2, 0, 0, 1);
    }
  }

  .setting-divider {
    height: 1px;
    margin: 4px 6px;
    background-color: #e8eaed;
    flex-shrink: 0;
  }

  &.dark,
  html.dark & {
    .setting-item {
      color: #e8eaed;

      &:hover,
      &.is-active {
        background-color: rgba(255, 255, 255, 0.08);
        color: #ffffff;

        .item-icon {
          color: #ffffff;
        }

        .item-arrow {
          color: #ffffff;
        }
      }

      &:active {
        background-color: rgba(255, 255, 255, 0.12);
      }

      .item-icon {
        color: #9aa0a6;
      }

      .item-arrow {
        color: #9aa0a6;
      }
    }

    .setting-divider {
      background-color: #3c4043;
    }
  }
}

.v-setting-submenu-card {
  user-select: none;

  .submenu-list {
    display: flex;
    flex-direction: column;
    width: 100%;
    gap: 2px;
  }

  .submenu-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 36px;
    padding: 0 10px;
    border-radius: 8px;
    cursor: pointer;
    color: #3c4043;
    transition: background-color 0.15s cubic-bezier(0.2, 0, 0, 1),
                color 0.15s cubic-bezier(0.2, 0, 0, 1);
    outline: none;

    &:hover {
      background-color: rgba(60, 64, 67, 0.08);
      color: #202124;
    }

    &:active {
      background-color: rgba(60, 64, 67, 0.12);
    }

    &.is-selected {
      color: #1a73e8;
      font-weight: 600;

      &:hover {
        background-color: rgba(26, 115, 232, 0.08);
      }
    }

    .submenu-label {
      font-size: 13px;
      line-height: 1.4;
    }

    .submenu-check {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 16px;
      height: 16px;
      color: #1a73e8;
      flex-shrink: 0;
    }
  }

  &.dark,
  html.dark & {
    .submenu-item {
      color: #e8eaed;

      &:hover {
        background-color: rgba(255, 255, 255, 0.08);
        color: #ffffff;
      }

      &:active {
        background-color: rgba(255, 255, 255, 0.12);
      }

      &.is-selected {
        color: #a8c7fa;

        &:hover {
          background-color: rgba(168, 199, 250, 0.12);
        }

        .submenu-check {
          color: #a8c7fa;
        }
      }
    }
  }
}
</style>
