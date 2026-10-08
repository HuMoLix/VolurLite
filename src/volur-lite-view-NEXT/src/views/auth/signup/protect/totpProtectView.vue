<template>
  <div class="flex flex-col justify-between h-full">
    <div class="flex-1 flex flex-col justify-center">
      <div class="flex flex-col items-center text-center py-4">
        <div class="w-16 h-16 rounded-full bg-google-blue/10 dark:bg-google-blue-dark/15 flex items-center justify-center text-google-blue dark:text-google-blue-dark mb-4">
          <svg class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            <polyline points="9 12 11 14 15 10" />
          </svg>
        </div>
        <h2 class="text-base font-medium text-google-text-primary dark:text-google-text-darkPrimary mb-2">
          {{ t('auth.totp_protect_title') }}
        </h2>
        <p class="text-sm text-google-text-secondary dark:text-google-text-darkSecondary max-w-sm leading-relaxed mb-4">
          {{ t('auth.totp_protect_desc') }}
        </p>
        <div class="p-3.5 rounded-xl bg-gray-50 dark:bg-neutral-800/60 border border-google-border-base dark:border-neutral-700/60 max-w-sm text-xs text-google-text-muted dark:text-google-text-darkMuted leading-relaxed text-left">
          {{ t('auth.totp_protect_notice') }}
        </div>
      </div>
    </div>

    <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
      <n-button
        quaternary
        type="primary"
        :round="true"
        class="auth-action-btn btn-secondary-action"
        :disabled="loading"
        :isScrim="true"
        @click="handleBackOptions"
      >
        {{ t('common.back') }}
      </n-button>
      <n-button
        type="primary"
        class="auth-action-btn btn-primary-action"
        :disabled="loading"
        :isScrim="true"
        @click="handleBackOptions"
      >
        {{ t('auth.got_it') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useSignupFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'TotpProtectView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.totp_protect_title'), () => t('auth.totp_protect_desc'))

const router = useRouter()
const signupFlowStore = useSignupFlowStore()
const loading = computed(() => signupFlowStore.loading)

async function handleBackOptions() {
  if (signupFlowStore.loading) return
  await signupFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signup/steps/pt/options',
    query: signupFlowStore.getQueryParams()
  })
}
</script>

<style scoped lang="scss">
.auth-action-btn {
  height: 36px !important;
  min-width: 88px !important;
  padding: 0 16px !important;
  border-radius: 9999px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  box-sizing: border-box;
}

.btn-secondary-action {
  color: #0b57d0 !important;
  background-color: transparent !important;
  border: none !important;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  :deep(.n-button__content),
  .n-button__content {
    color: #0b57d0 !important;
  }

  :deep(.n-button__state-border) {
    border: none !important;
  }

  &:hover:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;

    :deep(.n-button__content),
    .n-button__content {
      color: #0b57d0 !important;
    }
  }

  &:active:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.16) !important;
  }

  &:disabled,
  &.n-button--disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

:global(html.dark) .btn-secondary-action {
  color: #8ab4f8 !important;

  :deep(.n-button__content),
  .n-button__content {
    color: #8ab4f8 !important;
  }

  &:hover:not(:disabled) {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;

    :deep(.n-button__content),
    .n-button__content {
      color: #8ab4f8 !important;
    }
  }

  &:active:not(:disabled) {
    background-color: rgba(138, 180, 248, 0.2) !important;
  }

  &:disabled,
  &.n-button--disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.btn-primary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: #0b57d0 !important;
  color: #ffffff !important;
  border: none !important;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  :deep(.n-button__content),
  .n-button__content {
    color: #ffffff !important;
  }

  :deep(.n-button__state-border) {
    border: none !important;
  }

  &:hover {
    background-color: #0842a0 !important;
    color: #ffffff !important;
    box-shadow: 0 1px 2px 0 rgba(60, 64, 67, 0.3), 0 1px 3px 1px rgba(60, 64, 67, 0.15);

    :deep(.n-button__content),
    .n-button__content {
      color: #ffffff !important;
    }
  }

  &:active {
    background-color: #063980 !important;
    color: #ffffff !important;
    transform: scale(0.98);
    box-shadow: 0 1px 2px 0 rgba(60, 64, 67, 0.3);

    :deep(.n-button__content),
    .n-button__content {
      color: #ffffff !important;
    }
  }

  &:focus,
  &:focus-visible {
    background-color: #0b57d0 !important;
    color: #ffffff !important;

    :deep(.n-button__content),
    .n-button__content {
      color: #ffffff !important;
    }
  }
}

:global(html.dark) .btn-primary-action {
  background-color: #8ab4f8 !important;
  color: #202124 !important;

  :deep(.n-button__content),
  .n-button__content {
    color: #202124 !important;
  }

  &:hover {
    background-color: #aecbfa !important;
    color: #202124 !important;

    :deep(.n-button__content),
    .n-button__content {
      color: #202124 !important;
    }
  }

  &:active {
    background-color: #669df6 !important;
    color: #202124 !important;
    transform: scale(0.98);

    :deep(.n-button__content),
    .n-button__content {
      color: #202124 !important;
    }
  }
}
</style>
