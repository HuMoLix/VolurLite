<template>
  <div class="auth-layout min-h-[100dvh] flex flex-col justify-between md:justify-center items-center transition-colors duration-300">
    <transition name="auth-scrim-fade">
      <div
        v-if="isScrimVisible"
        class="auth-scrim"
        aria-hidden="true"
      />
    </transition>

    <div
      class="auth-card"
      :class="{ 'is-loading': isCardLoading }"
      :aria-busy="isCardLoading"
      @click="handleCardActionClick"
    >
      <div
        class="auth-card__progress"
        :class="{ 'is-active': isCardLoading }"
        role="progressbar"
        :aria-label="t('common.processing')"
        :aria-hidden="!isCardLoading"
        aria-valuemin="0"
        aria-valuemax="100"
      >
        <div class="auth-card__progress-track" />
        <div class="auth-card__progress-bar auth-card__progress-bar--primary">
          <span class="auth-card__progress-inner" />
        </div>
        <div class="auth-card__progress-bar auth-card__progress-bar--secondary">
          <span class="auth-card__progress-inner" />
        </div>
      </div>

      <div
        v-if="isCardLoading"
        class="auth-card__blocker"
        aria-hidden="true"
        @click.stop.prevent
        @mousedown.stop.prevent
        @dblclick.stop.prevent
        @keydown.stop.prevent
        @touchstart.stop.prevent
      />

      <div class="auth-card__header">
        <img
          src="/r/static/svg/favicon-simple.svg"
          alt="Volur"
          class="auth-card__logo"
        />
      </div>
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" v-if="Component" />
          <slot v-else />
        </transition>
      </router-view>
    </div>

    <footer class="auth-footer text-[12px]">
      <div class="auth-footer__lang shrink-0">
        <n-dropdown :options="languageOptions" trigger="click" @select="handleLanguageSelect">
          <button
            type="button"
            class="auth-btn-link text-[12px] text-google-text-muted dark:text-[#e8eaed]"
          >
            <span>{{ currentLangLabel }}</span>
            <svg
              class="w-2.5 h-1.5 ml-2.5 text-google-text-muted dark:text-[#e8eaed] fill-current shrink-0"
              viewBox="7 10 10 5"
              aria-hidden="true"
            >
              <polygon points="7 10 12 15 17 10" />
            </svg>
          </button>
        </n-dropdown>
      </div>

      <div class="auth-footer__meta flex items-center flex-wrap gap-1 text-[12px] text-google-text-muted dark:text-[#e8eaed] shrink-0">
        <a :href="`/accounts/help?lang=${currentUrlLang}`" class="auth-btn-link text-[12px]">{{ t('common.help') }}</a>
        <a :href="`/accounts/privacy?lang=${currentUrlLang}`" class="auth-btn-link text-[12px]">{{ t('common.privacy') }}</a>
        <a :href="`/accounts/terms?lang=${currentUrlLang}`" class="auth-btn-link text-[12px]">{{ t('common.terms') }}</a>

        <span class="auth-footer__copyright whitespace-nowrap px-2 text-[12px] inline-flex items-center h-8 select-none">
          <span class="hidden md:inline">© 2026 Volur, Inc.</span>
          <span class="inline md:hidden">© 2026 Volur</span>
        </span>

        <a
          href="https://beian.miit.gov.cn/"
          target="_blank"
          rel="noopener noreferrer"
          class="auth-btn-link text-[12px] gap-1.5 text-google-text-muted dark:text-[#e8eaed]"
          :title="t('common.beian')"
        >
          <img
            src="/r/static/img/beian.png"
            alt="备案"
            class="w-3.5 h-3.5 object-contain shrink-0"
          />
          <span class="hidden md:inline"></span>
        </a>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'AuthLayout'
})

import { computed, provide, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NDropdown } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useSigninFlowStore, useSignupFlowStore, MIN_LOADING_DURATION_MS } from '@/store/modules/flow'
import { useAppStore } from '@/store/modules/app'
import { normalizeLocale, toUrlLang } from '@/i18n'

const props = withDefaults(
  defineProps<{
    loading?: boolean
  }>(),
  {
    loading: undefined
  }
)

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const signinFlowStore = useSigninFlowStore()
const signupFlowStore = useSignupFlowStore()

const languageOptions = [
  { label: '简体中文', key: 'zh-CN' },
  { label: '繁体中文', key: 'zh-TW' },
  { label: 'English', key: 'en-US' }
]

const currentUrlLang = computed(() => toUrlLang(appStore.locale))

const currentLangLabel = computed(() => {
  const match = languageOptions.find(item => item.key === appStore.locale)
  return match?.label || '简体中文'
})

function syncLangFromQuery(): void {
  if (route.query.lang) {
    const rawLang = Array.isArray(route.query.lang) ? route.query.lang[0] : route.query.lang
    const normalized = normalizeLocale(rawLang)
    if (normalized && normalized !== appStore.locale) {
      appStore.switchLanguage(normalized, route.path)
    }
  }
}

onMounted(() => {
  syncLangFromQuery()
})

watch(
  () => route.query.lang,
  () => {
    syncLangFromQuery()
  }
)

async function handleLanguageSelect(key: string): Promise<void> {
  const normalized = normalizeLocale(key)
  if (!normalized) return
  const nextLangParam = toUrlLang(normalized)
  const currentQueryLang = Array.isArray(route.query.lang) ? route.query.lang[0] : route.query.lang
  if (currentQueryLang !== nextLangParam) {
    await router.replace({
      query: {
        ...route.query,
        lang: nextLangParam
      }
    }).catch(() => {})
  }
  await appStore.switchLanguage(normalized, route.path)
}

const currentFlowStore = computed(() => {
  if (route.path.startsWith('/v1/signup')) {
    return signupFlowStore
  }
  if (route.path.startsWith('/v1/info')) {
    const followup = (route.query.followup as string) || ''
    const cont = (route.query.continue as string) || ''
    if (followup.includes('signup') || cont.includes('signup')) {
      return signupFlowStore
    }
  }
  return signinFlowStore
})

async function triggerScrim<T = void>(
  action?: () => Promise<T> | T,
  durationMs = MIN_LOADING_DURATION_MS
): Promise<T | undefined> {
  return currentFlowStore.value.triggerScrim(action, durationMs)
}

function handleCardActionClick(event: MouseEvent): void {
  const target = event.target as HTMLElement | null
  const scrimBtn = target?.closest(
    '[isScrim="true"], [isscrim="true"], [is-scrim="true"], [data-scrim="true"]'
  )
  if (!scrimBtn) return

  if (
    scrimBtn.classList.contains('btn-next-step') ||
    scrimBtn.classList.contains('btn-verify-action') ||
    scrimBtn.classList.contains('btn-primary-action') ||
    scrimBtn.classList.contains('btn-secondary-action') ||
    scrimBtn.classList.contains('btn-try-another-way') ||
    scrimBtn.classList.contains('btn-create-account') ||
    scrimBtn.classList.contains('btn-signin-instead') ||
    scrimBtn.classList.contains('btn-remove-account') ||
    scrimBtn.classList.contains('remove-icon-btn') ||
    scrimBtn.classList.contains('auth-selection-item') ||
    scrimBtn.classList.contains('auth-account-chip') ||
    scrimBtn.getAttribute('type') === 'submit'
  ) {
    return
  }

  if (!signinFlowStore.loading && !signupFlowStore.loading) {
    triggerScrim(undefined, MIN_LOADING_DURATION_MS)
  }
}

const isCardLoading = computed(() => {
  if (typeof props.loading === 'boolean') {
    return props.loading
  }
  return signinFlowStore.loading || signupFlowStore.loading
})

const isScrimVisible = computed(() => isCardLoading.value)

provide('authLoading', isCardLoading)
provide('setAuthLoading', (val: boolean) => {
  currentFlowStore.value.setLoading(val)
})
provide('triggerScrim', triggerScrim)
</script>

<style scoped lang="scss">
.auth-layout {
  width: 100%;
  box-sizing: border-box;
  background-color: var(--v-bg-surface, #ffffff);
  padding: 0;

  @media (min-width: 840px) {
    background-color: var(--sys-color-surface-container, #f0f4f9);
    padding: 32px 24px;
  }
}

.auth-scrim {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(0.1px);
  z-index: 40;
  pointer-events: all;
  cursor: wait;
}


.auth-scrim-fade-enter-active,
.auth-scrim-fade-leave-active {
  transition: opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.auth-scrim-fade-enter-from,
.auth-scrim-fade-leave-to {
  opacity: 0;
}

.auth-card {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  transition: background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &.is-loading {
    user-select: none;
  }

  display: flex;
  flex-direction: column;
  background-color: transparent !important;
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  padding: 32px 24px 20px 24px;
  max-width: 100%;
  min-height: unset;

  &__progress {
    position: absolute;
    top: 0;
    left: 24px;
    right: 24px;
    height: 4px;
    overflow: hidden;
    border-radius: 2px;
    z-index: 30;
    pointer-events: none;
    opacity: 0;
    transition: opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1);

    @media (min-width: 840px) {
      left: 44px;
      right: 44px;
    }

    &.is-active {
      opacity: 1;
    }

    &-track {
      position: absolute;
      inset: 0;
      background-color: var(--google-progress-track, #e8f0fe);
      border-radius: inherit;
    }

    &-bar {
      position: absolute;
      top: 0;
      bottom: 0;
      width: 100%;
      transform-origin: top left;
      will-change: transform;

      &--primary {
        left: -145.166611%;
      }

      &--secondary {
        left: -54.888891%;
      }
    }

    &-inner {
      display: inline-block;
      position: absolute;
      inset: 0;
      background-color: var(--google-progress-indicator, #0b57d0);
      border-radius: inherit;
      will-change: transform;
    }

    &.is-active {
      .auth-card__progress-bar--primary {
        animation: mdc-linear-progress-primary-indeterminate-translate 2s linear infinite;

        .auth-card__progress-inner {
          animation: mdc-linear-progress-primary-indeterminate-scale 2s linear infinite;
        }
      }

      .auth-card__progress-bar--secondary {
        animation: mdc-linear-progress-secondary-indeterminate-translate 2s linear infinite;

        .auth-card__progress-inner {
          animation: mdc-linear-progress-secondary-indeterminate-scale 2s linear infinite;
        }
      }
    }
  }

  &__blocker {
    position: absolute;
    inset: 0;
    z-index: 20;
    background-color: rgba(255, 255, 255, 0.6);
    backdrop-filter: blur(0.1px);
    cursor: wait;
    pointer-events: all;
    border-radius: inherit;
    transition: opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  }


  &__header {
    width: 100%;
    display: flex;
    justify-content: flex-start;
    margin-bottom: 16px;
  }

  &__logo {
    width: 48px;
    height: 48px;
    padding: 4px;
    box-sizing: border-box;
    object-fit: contain;
  }

  &__body,
  :deep(.auth-card__body) {
    width: 100%;
    display: flex;
    flex-direction: column;
    flex: 1;
  }

  &__left,
  :deep(.auth-card__left) {
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    text-align: left;
    margin-bottom: 24px;
  }

  &__title,
  :deep(.auth-card__title) {
    font-size: 24px;
    font-weight: 400;
    line-height: 1.33;
    color: var(--v-text-primary, #202124);
    margin: 0 0 8px 0;
  }

  &__subtitle,
  :deep(.auth-card__subtitle) {
    font-size: 14px;
    line-height: 1.43;
    color: var(--v-text-desc, #3c4043);
    margin: 0;
  }

  &__header-extra,
  :deep(.auth-card__header-extra) {
    margin-top: 12px;
    width: 100%;
  }

  &__right,
  :deep(.auth-card__right) {
    width: 100%;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    flex: 1;
  }

  @media (min-width: 840px) {
    flex-direction: column;
    max-width: 1000px;
    min-height: 390px;
    padding: 36px 44px;
    background-color: var(--v-bg-card, #ffffff) !important;
    border: none !important;
    border-radius: 28px !important;
    box-shadow: 0 1px 3px 0 rgba(60, 64, 67, 0.08) !important;

    &__header {
      margin-bottom: 24px;
    }

    &__logo {
      width: 48px;
      height: 48px;
      padding: 4px;
    }

    &__body,
    :deep(.auth-card__body) {
      flex-direction: row;
      flex: 1;
      align-items: stretch;
    }

    &__left,
    :deep(.auth-card__left) {
      flex: 0 0 50%;
      max-width: 50%;
      padding-right: 32px;
      margin-bottom: 0;
    }

    &__title,
    :deep(.auth-card__title) {
      font-size: 40px;
      line-height: 1.25;
      margin: 0 0 12px 0;
    }

    &__subtitle,
    :deep(.auth-card__subtitle) {
      font-size: 16px;
      line-height: 1.5;
    }

    &__right,
    :deep(.auth-card__right) {
      flex: 0 0 50%;
      max-width: 50%;
      padding-left: 20px;
      padding-top: 0;
    }
  }

  :deep(.auth-action-btn),
  :deep(.n-button) {
    .n-base-loading,
    .n-button__loading,
    .n-base-loading__icon,
    .n-button__icon {
      display: none !important;
      animation: none !important;
      transform: none !important;
    }
  }
}



.auth-footer {
  width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;

  margin-top: auto;
  padding: 12px 16px 20px 16px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;

  @media (min-width: 840px) {
    max-width: 1000px;
    margin-top: 14px;
    padding: 0 16px;
    overflow-x: visible;
  }
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

html.dark {
  .auth-layout {
    background-color: #1e1f20;
  }

  .auth-scrim {
    background-color: rgba(0, 0, 0, 0.6);
  }

  .auth-card {
    color: #e3e3e3;

    @media (min-width: 840px) {
      background-color: #0e0e0e !important;
      box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.4) !important;
    }

    &__blocker {
      background-color: rgba(0, 0, 0, 0.6);
    }

    &__title,
    :deep(.auth-card__title) {
      color: #e3e3e3;
    }

    &__subtitle,
    :deep(.auth-card__subtitle) {
      color: #bdc1c6;
    }
  }

  .auth-footer {
    color: #e8eaed;

    .auth-btn-link,
    .auth-footer__meta,
    .auth-footer__copyright,
    svg {
      color: #e8eaed;
    }
  }

  .auth-card__progress-track {
    background-color: rgba(168, 199, 250, 0.24) !important;
  }

  .auth-card__progress-inner {
    background-color: #a8c7fa !important;
  }
}

@keyframes mdc-linear-progress-primary-indeterminate-translate {
  0% {
    transform: translateX(0);
  }
  20% {
    animation-timing-function: cubic-bezier(0.5, 0, 0.701732, 0.495819);
    transform: translateX(0);
  }
  59.15% {
    animation-timing-function: cubic-bezier(0.302435, 0.381352, 0.55, 0.956352);
    transform: translateX(83.67142%);
  }
  to {
    transform: translateX(200.611057%);
  }
}

@keyframes mdc-linear-progress-primary-indeterminate-scale {
  0% {
    transform: scaleX(0.08);
  }
  36.65% {
    animation-timing-function: cubic-bezier(0.334731, 0.12482, 0.785844, 1);
    transform: scaleX(0.08);
  }
  69.15% {
    animation-timing-function: cubic-bezier(0.06, 0.11, 0.6, 1);
    transform: scaleX(0.661479);
  }
  to {
    transform: scaleX(0.08);
  }
}

@keyframes mdc-linear-progress-secondary-indeterminate-translate {
  0% {
    transform: translateX(0);
  }
  25% {
    animation-timing-function: cubic-bezier(0.15, 0, 0.515058, 0.409685);
    transform: translateX(0);
  }
  48.35% {
    animation-timing-function: cubic-bezier(0.4, 0.627035, 0.6, 0.902026);
    transform: translateX(84.386165%);
  }
  to {
    transform: translateX(160.277782%);
  }
}

@keyframes mdc-linear-progress-secondary-indeterminate-scale {
  0% {
    transform: scaleX(0.08);
  }
  19.15% {
    animation-timing-function: cubic-bezier(0.152313, 0.196432, 0.648374, 1.004315);
    transform: scaleX(0.457104);
  }
  44.15% {
    animation-timing-function: cubic-bezier(0.257759, -0.003163, 0.211762, 1.38179);
    transform: scaleX(0.72796);
  }
  to {
    transform: scaleX(0.08);
  }
}
</style>
