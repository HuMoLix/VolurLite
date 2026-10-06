<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleVerifyChallenge">
    <div class="space-y-4">
      <div class="auth-challenge-banner flex items-start gap-3 p-3.5 rounded-lg border border-google-border-light dark:border-google-border-dark bg-google-surface-light dark:bg-google-surface-darkElevated">
        <svg class="w-5 h-5 text-google-blue dark:text-google-blue-dark shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/>
        </svg>
        <div class="text-xs text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
          <p class="font-medium text-google-text-primary dark:text-google-text-darkPrimary mb-0.5">
            {{ t('auth.two_step_verification') }}
          </p>
          {{ (currentType === 'totp' || currentType === 'smsotp') ? t('auth.two_step_sms_tip') : t('auth.two_step_email_tip') }}
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
      <button
        type="button"
        class="auth-action-btn btn-try-another-way"
        :isScrim="true"
        @click="handleTryAnotherWayClick"
      >
        {{ t('auth.try_another_way') }}
      </button>
      <n-button
        type="primary"
        class="auth-action-btn btn-verify-action"
        :disabled="loading"
        @click="handleVerifyChallenge"
      >
        {{ t('common.verify') }}
      </n-button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'TotpChallengeView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.security_verification_and_challenge'), '')

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const currentType = computed(() => {
  const p = route.params.type
  const typeParam = Array.isArray(p) ? p.join('/') : (p as string) || (route.query.challengeType as string) || ''
  return typeParam ? typeParam.toLowerCase() : 'etotp'
})

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
      from: currentType.value,
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
      const challengeType = currentType.value === 'totp' || currentType.value === 'smsotp' ? 'smsotp' : 'etotp'
      const res = await signinFlowStore.submitChallenge(challengeType, {
        verificationCode: verificationCode.value.trim(),
        rememberMe: trustDevice.value
      })
      if (res.success) {
        window.$message?.success?.(t('auth.security_verify_success'))
        router.push(res.redirect || signinFlowStore.continueUrl || '/competitions')
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
  color: #0b57d0;
  background-color: transparent;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
  }
}

:global(html.dark) .btn-try-another-way {
  color: #8ab4f8;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:active {
    background-color: rgba(138, 180, 248, 0.2) !important;
  }
}

.btn-verify-action {
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

:global(html.dark) .btn-verify-action {
  background-color: #8ab4f8 !important;
  color: #202124 !important;

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
