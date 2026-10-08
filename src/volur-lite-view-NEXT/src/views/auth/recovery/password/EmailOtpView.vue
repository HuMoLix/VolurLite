<template>
  <div class="auth-card__body">
    <div class="auth-card__left">
      <h1 class="auth-card__title">{{ t('auth.recovery_password_emailotp_title') }}</h1>
      <p class="auth-card__subtitle">{{ t('auth.recovery_password_emailotp_subtitle') }}</p>
      <div id="auth-header-extra" class="auth-card__header-extra" />
    </div>

    <div class="auth-card__right">
      <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleVerify">
        <div class="space-y-4">
          <div>
            <GoogleTextField
              v-model="code"
              type="text"
              maxlength="6"
              :label="t('auth.enter_code_digits')"
              autocomplete="one-time-code"
              autofocus
              :error-msg="errorMsg"
              @clear="clearError"
              @enter="handleVerify"
            />
          </div>

          <div class="flex items-center justify-between">
            <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
              {{ t('auth.two_step_email_tip') }}
            </span>
            <button
              type="button"
              class="auth-resend-btn"
              :disabled="!canResend || isSending || loading"
              @click="sendCode"
            >
              <span v-if="countdown > 0">
                {{ t('auth.resend_countdown', { seconds: countdown }) }}
              </span>
              <span v-else>
                {{ isSending ? t('auth.sending') : t('auth.resend_code') }}
              </span>
            </button>
          </div>
        </div>

        <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
          <n-button
            quaternary
            type="primary"
            :round="true"
            class="auth-action-btn btn-secondary-action"
            :disabled="loading || isSubmitting"
            :isScrim="true"
            @click="handleBackToSelection"
          >
            {{ t('common.back') }}
          </n-button>
          <n-button
            type="primary"
            class="auth-action-btn btn-primary-action"
            :loading="isSubmitting"
            :disabled="loading || isSubmitting"
            :isScrim="true"
            @click="handleVerify"
          >
            {{ t('auth.next') }}
          </n-button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { sendPasswordRecoveryEmailOtpApi, verifyPasswordRecoveryEmailOtpApi } from '@/api/auth'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'PasswordEmailOtpView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.recovery_password_emailotp_title'), () => t('auth.recovery_password_emailotp_subtitle'))

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const code = ref('')
const countdown = ref(0)
const isSending = ref(false)
const isSubmitting = ref(false)
const errorKey = ref('')
const rawErrorMsg = ref('')
let timer: ReturnType<typeof setInterval> | null = null

const loading = computed(() => signinFlowStore.loading)
const canResend = computed(() => countdown.value <= 0)

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

watch(code, () => {
  if (errorKey.value || rawErrorMsg.value) {
    clearError()
  }
})

function startCountdown(seconds: number = 60): void {
  if (timer) {
    clearInterval(timer)
  }
  countdown.value = seconds
  timer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value -= 1
    } else {
      if (timer) {
        clearInterval(timer)
        timer = null
      }
    }
  }, 1000)
}

async function sendCode(): Promise<void> {
  if (isSending.value || countdown.value > 0) return
  isSending.value = true
  try {
    await sendPasswordRecoveryEmailOtpApi(signinFlowStore.dsh, signinFlowStore.tl)
    startCountdown(60)
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
      rawErrorMsg.value = (err as { message: string }).message
      errorKey.value = ''
    } else {
      errorKey.value = 'auth.resend_failed'
      rawErrorMsg.value = ''
    }
  } finally {
    isSending.value = false
  }
}

onMounted(async () => {
  await signinFlowStore.ensureFlowParams(route, router)
  if (!signinFlowStore.dsh || !signinFlowStore.tl) {
    await router.push({
      path: '/v1/signin/identifier',
      query: signinFlowStore.getQueryParams()
    })
    return
  }
  await sendCode()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})

async function handleBackToSelection(): Promise<void> {
  if (loading.value || isSubmitting.value) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/recovery/password/selection',
    query: signinFlowStore.getQueryParams({ recoveryType: 'password' })
  })
}

async function handleVerify(): Promise<void> {
  if (loading.value || isSubmitting.value) return
  const trimmed = code.value.trim()
  if (!trimmed) {
    errorKey.value = 'auth.code_required'
    rawErrorMsg.value = ''
    return
  }
  if (!/^\d{6}$/.test(trimmed)) {
    errorKey.value = 'auth.code_digits_only'
    rawErrorMsg.value = ''
    return
  }

  isSubmitting.value = true
  try {
    let nextStep = ''
    await signinFlowStore.withLoading(async () => {
      const res = await verifyPasswordRecoveryEmailOtpApi({
        dsh: signinFlowStore.dsh,
        tl: signinFlowStore.tl,
        cid: signinFlowStore.cid,
        code: trimmed
      })
      if (res.data?.tl) {
        signinFlowStore.tl = res.data.tl
      }
      if (typeof res.data?.cid === 'number') {
        signinFlowStore.cid = res.data.cid
      }
      nextStep = res.data?.nextStep || ''
    }, 1000)

    if (nextStep === 'reset') {
      await router.push({
        path: '/v1/recovery/password/reset',
        query: signinFlowStore.getQueryParams({ recoveryType: 'password' })
      })
    } else {
      throw new Error('未知的下一步挑战流程: ' + nextStep)
    }
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
      rawErrorMsg.value = (err as { message: string }).message
      errorKey.value = ''
    } else {
      errorKey.value = 'auth.code_verify_failed'
      rawErrorMsg.value = ''
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-resend-btn {
  background: none;
  border: none;
  color: #0b57d0;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  white-space: nowrap;

  &:hover:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.08);
  }

  &:disabled {
    color: #9aa0a6;
    cursor: not-allowed;
  }
}

:global(.dark) .auth-resend-btn {
  color: #8ab4f8;

  &:hover:not(:disabled) {
    background-color: rgba(138, 180, 248, 0.12);
  }

  &:disabled {
    color: #5f6368;
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
    color: #0b57d0 !important;
  }

  &:disabled,
  &.n-button--disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.btn-primary-action {
  background-color: #0b57d0 !important;
  color: #ffffff !important;
  border: none !important;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  &:hover:not(:disabled) {
    background-color: #0842a0 !important;
    box-shadow: 0 1px 2px rgba(60, 64, 67, 0.3), 0 1px 3px 1px rgba(60, 64, 67, 0.15);
  }

  &:active:not(:disabled) {
    background-color: #0b57d0 !important;
  }

  &:disabled,
  &.n-button--disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

:global(.dark) .btn-secondary-action {
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

  &:disabled,
  &.n-button--disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

:global(.dark) .btn-primary-action {
  background-color: #8ab4f8 !important;
  color: #041e49 !important;

  &:hover:not(:disabled) {
    background-color: #aecbfa !important;
  }
}
</style>
