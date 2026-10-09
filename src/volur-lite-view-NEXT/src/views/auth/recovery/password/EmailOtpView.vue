<template>
  <AuthCardShell
    :title="t('auth.recovery.emailotp_title')"
    :subtitle="t('auth.recovery.emailotp_subtitle')"
  >
    <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleVerify">
      <div class="space-y-4">
        <div>
          <GoogleTextField
            v-model="code"
            type="text"
            maxlength="6"
            :label="t('auth.signup.enter_code_digits')"
            autocomplete="one-time-code"
            autofocus
            :error-msg="errorMsg"
            @clear="clearError"
            @enter="handleVerify"
          />
        </div>

        <div class="flex items-center justify-between">
          <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('auth.signin.two_step_email_tip') }}
          </span>
          <button
            type="button"
            class="auth-resend-btn"
            :disabled="isCounting || isSending || loading"
            @click="sendCode"
          >
            <span v-if="isCounting">
              {{ t('auth.signup.resend_countdown', { seconds: countdown }) }}
            </span>
            <span v-else>
              {{ isSending ? t('auth.sending') : t('auth.signup.resend_code') }}
            </span>
          </button>
        </div>
      </div>

      <AuthActionBar
        :primary-text="t('auth.next')"
        :secondary-text="t('common.back')"
        :primary-disabled="loading || isSubmitting"
        :secondary-disabled="loading || isSubmitting"
        :primary-loading="isSubmitting"
        @primary="handleVerify"
        @secondary="handleBackToSelection"
      />
    </form>
  </AuthCardShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { sendPasswordRecoveryEmailOtpApi, verifyPasswordRecoveryEmailOtpApi } from '@/api/auth'
import { AuthCardShell, AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'
import { useCountdown } from '@/composables/useCountdown'

defineOptions({
  name: 'PasswordEmailOtpView'
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const code = ref('')
const isSending = ref(false)
const isSubmitting = ref(false)
const loading = computed(() => signinFlowStore.loading)

const { errorMsg, clearError, setErrorKey, setRawError } = useFormError(code)
const { countdown, isCounting, start: startCountdown } = useCountdown(60)

async function sendCode(): Promise<void> {
  if (isSending.value || isCounting.value) return
  isSending.value = true
  try {
    await sendPasswordRecoveryEmailOtpApi(signinFlowStore.dsh, signinFlowStore.tl)
    startCountdown(60)
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
      setRawError((err as { message: string }).message)
    } else {
      setErrorKey('auth.signup.resend_failed')
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
    setErrorKey('auth.code_required')
    return
  }
  if (!/^\d{6}$/.test(trimmed)) {
    setErrorKey('auth.code_digits_only')
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
      setRawError((err as { message: string }).message)
    } else {
      setErrorKey('auth.code_verify_failed')
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>
