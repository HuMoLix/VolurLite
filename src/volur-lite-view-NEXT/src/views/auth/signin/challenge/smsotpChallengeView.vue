<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleVerifyChallenge">
    <div class="space-y-4">
      <div class="auth-challenge-banner flex items-start gap-3 p-3.5 rounded-lg border border-google-border-light dark:border-google-border-dark bg-google-surface-light dark:bg-google-surface-darkElevated">
        <svg class="w-5 h-5 text-google-blue dark:text-google-blue-dark shrink-0 mt-0.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          <line x1="8" y1="9" x2="16" y2="9"/>
          <line x1="8" y1="13" x2="13" y2="13"/>
        </svg>
        <div class="text-xs text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
          <p class="font-medium text-google-text-primary dark:text-google-text-darkPrimary mb-0.5">
            {{ t('auth.two_step_verification') }}
          </p>
          {{ t('auth.two_step_sms_tip') }}
        </div>
      </div>

      <div>
        <GoogleTextField
          v-model="verificationCode"
          :label="t('auth.enter_code_label')"
          autocomplete="one-time-code"
          autofocus
          :error-msg="errorMsg"
          @clear="clearError"
          @enter="handleVerifyChallenge"
        />
      </div>

      <div class="flex items-center justify-between pt-1">
        <label class="flex items-center gap-2 text-sm text-google-text-desc dark:text-google-text-darkDesc cursor-pointer select-none">
          <input
            type="checkbox"
            v-model="trustDevice"
            class="w-4 h-4 rounded border-google-border-light text-google-blue focus:ring-google-blue cursor-pointer"
          />
          <span>{{ t('auth.trust_device_30_days') }}</span>
        </label>
        <a
          href="#"
          class="auth-another-way-link"
          :isScrim="true"
          @click.prevent="handleTryAnotherWayClick"
        >
          {{ t('auth.another_way') }}
        </a>
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
        @click="handleVerifyChallenge"
      >
        {{ t('common.verify') }}
      </n-button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'SmsotpChallengeView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.security_verification_and_challenge'), '')

const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const verificationCode = ref('')
const trustDevice = ref(true)
const errorKey = ref('')
const rawErrorMsg = ref('')

const errorMsg = computed(() => {
  if (errorKey.value) {
    return t(errorKey.value)
  }
  return rawErrorMsg.value
})

function clearError(): void {
  errorKey.value = ''
  rawErrorMsg.value = ''
}

const isSubmitting = ref(false)
const loading = computed(() => signinFlowStore.loading)

watch(verificationCode, () => {
  if (errorKey.value || rawErrorMsg.value) {
    clearError()
  }
})

async function handleTryAnotherWayClick() {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/challenge/selection',
    query: signinFlowStore.getQueryParams({
      from: 'smsotp',
      page: '1'
    })
  })
}

async function handleVerifyChallenge() {
  if (signinFlowStore.loading || isSubmitting.value) return
  isSubmitting.value = true

  try {
    if (!verificationCode.value.trim()) {
      errorKey.value = 'auth.security_code_required'
      rawErrorMsg.value = ''
      return
    }

    try {
      const res = await signinFlowStore.submitChallenge('smsotp', {
        verificationCode: verificationCode.value.trim(),
        rememberMe: trustDevice.value
      })
      if (res.success) {
        router.push(res.redirect || signinFlowStore.continueUrl || '/competitions')
      } else if (res.nextStep) {
        router.push({
          path: `/v1/signin/challenge/${res.nextStep}`,
          query: signinFlowStore.getQueryParams()
        })
      }
    } catch (err: any) {
      if (err?.message) {
        rawErrorMsg.value = err.message
        errorKey.value = ''
      } else {
        errorKey.value = 'auth.security_code_invalid'
        rawErrorMsg.value = ''
      }
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-another-way-link {
  display: inline-flex;
  align-items: center;
  padding: 1px 2px !important;
  border-radius: 4px;
  background-color: transparent !important;
  color: #0b57d0 !important;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
  }
}

:global(html.dark) .auth-another-way-link {
  color: #8ab4f8 !important;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }
}

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
