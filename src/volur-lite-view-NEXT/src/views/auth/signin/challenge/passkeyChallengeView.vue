<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleOpenPasskeyModal">
    <div class="space-y-4">
      <div class="p-4 rounded-lg border border-google-border-light dark:border-google-border-dark space-y-2 text-xs">
        <div class="flex justify-between">
          <span class="text-google-text-muted">{{ t('auth.login_account') }}</span>
          <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">{{ signinFlowStore.identifier }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-google-text-muted">{{ t('auth.attempt_time') }}</span>
          <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">{{ currentTimeFormatted }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-google-text-muted">{{ t('auth.verify_method') }}</span>
          <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">{{ t('auth.passkey_method_name') }}</span>
        </div>
      </div>

      <div v-if="deviceError" class="flex items-center gap-1.5 text-xs text-google-red dark:text-google-red-dark">
        <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current shrink-0">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
        </svg>
        <span>{{ deviceError }}</span>
      </div>
    </div>

    <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
      <n-button
        quaternary
        type="primary"
        :round="true"
        class="auth-action-btn btn-try-another-way"
        :disabled="loading || isSubmitting"
        :isScrim="true"
        @click="handleTryAnotherWayClick"
      >
        {{ t('auth.try_another_way') }}
      </n-button>
      <n-button
        type="primary"
        class="auth-action-btn btn-verify-action"
        :loading="isSubmitting"
        :disabled="loading || isSubmitting"
        :isScrim="true"
        @click="handleOpenPasskeyModal"
      >
        {{ t('common.verify') }}
      </n-button>
    </div>

    <PasskeyModal
      v-model:visible="isModalVisible"
      mode="use"
      :dsh="signinFlowStore.dsh"
      :tl="signinFlowStore.tl"
      @success="handleVerifySuccess"
      @error="handleVerifyError"
    />
  </form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useSigninFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import PasskeyModal from '@/components/security/PasskeyModal.vue'

defineOptions({
  name: 'PassKeyChallengeView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.passkey_verify_title'), () => t('auth.passkey_verify_desc'))

const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const isModalVisible = ref(false)
const deviceErrorKey = ref('')
const rawDeviceError = ref('')

const deviceError = computed(() => {
  if (deviceErrorKey.value) {
    return t(deviceErrorKey.value)
  }
  return rawDeviceError.value
})

function clearDeviceError(): void {
  deviceErrorKey.value = ''
  rawDeviceError.value = ''
}

const isSubmitting = ref(false)
const loading = computed(() => signinFlowStore.loading)

const currentTimeFormatted = computed(() => {
  return new Date().toLocaleTimeString('zh-CN', { hour12: false })
})

async function handleTryAnotherWayClick(): Promise<void> {
  if (signinFlowStore.loading || isSubmitting.value) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/challenge/selection',
    query: signinFlowStore.getQueryParams({
      from: 'pk',
      page: '1'
    })
  })
}

async function handleOpenPasskeyModal(): Promise<void> {
  if (signinFlowStore.loading || isSubmitting.value) return
  clearDeviceError()
  await signinFlowStore.triggerScrim(undefined, 1000)
  isModalVisible.value = true
}

async function handleVerifySuccess(res: any): Promise<void> {
  if (res?.success) {
    await router.push(res.redirect || '/competitions')
    return
  }

  if (res?.nextStep) {
    await router.push({
      path: `/v1/signin/challenge/${res.nextStep}`,
      query: signinFlowStore.getQueryParams()
    })
    return
  }

  if (res?.errorMessage) {
    rawDeviceError.value = res.errorMessage
  }
}

function handleVerifyError(errorMsg: string): void {
  if (errorMsg.startsWith('auth.') || errorMsg.startsWith('common.')) {
    deviceErrorKey.value = errorMsg
    rawDeviceError.value = ''
  } else {
    rawDeviceError.value = errorMsg
    deviceErrorKey.value = ''
  }
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

.btn-try-another-way {
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
    color: #0b57d0 !important;
  }

  &:disabled,
  &.n-button--disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

:global(html.dark) .btn-try-another-way {
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

.btn-verify-action {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: #0b57d0 !important;
  color: #ffffff !important;
  border: none !important;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }

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

:global(html.dark) .btn-verify-action {
  background-color: #8ab4f8 !important;
  color: #202124 !important;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }

  :deep(.n-button__content),
  .n-button__content {
    color: #202124 !important;
  }

  &:hover {
    background-color: #aecbfa !important;
    color: #202124 !important;
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
