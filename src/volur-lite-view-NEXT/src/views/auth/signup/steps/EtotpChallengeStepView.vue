<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleNext">
    <div class="space-y-4">
      <div>
        <GoogleTextField
          v-model="code"
          :label="t('auth.signup.enter_code_digits')"
          autocomplete="one-time-code"
          autofocus
          maxlength="6"
          :error-msg="errorMsg"
          @clear="clearError"
          @enter="handleNext"
        />
      </div>

      <div class="flex items-center justify-between text-sm">
        <span class="text-google-text-muted dark:text-google-text-darkMuted">
          {{ t('auth.signup.code_sent_to') }} {{ displayEmail }}
        </span>
        <button
          type="button"
          class="auth-resend-link"
          :disabled="isCounting || isResending"
          @click="handleResend"
        >
          <span v-if="isCounting">{{ t('auth.signup.resend_countdown', { seconds: countdown }) }}</span>
          <span v-else-if="isResending">{{ t('auth.sending') }}</span>
          <span v-else>{{ t('auth.signup.resend_code') }}</span>
        </button>
      </div>
    </div>

    <AuthActionBar
      :primary-text="t('auth.next')"
      :secondary-text="t('auth.signup.back_to_email')"
      :primary-disabled="loading"
      :secondary-disabled="loading"
      @primary="handleNext"
      @secondary="goBackEmail"
    />
  </form>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSignupFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'
import { useCountdown } from '@/composables/useCountdown'

defineOptions({
  name: 'EtotpChallengeStepView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.email_label'), () => t('auth.signup.enter_code_digits'))

const router = useRouter()
const signupFlowStore = useSignupFlowStore()

const code = ref('')
const isResending = ref(false)
const loading = computed(() => signupFlowStore.loading)

const { errorMsg, clearError, setErrorKey, setRawError } = useFormError(code)
const { countdown, isCounting, start: startCountdown } = useCountdown(60)

const displayEmail = computed(() => {
  return signupFlowStore.email || ''
})

onMounted(() => {
  startCountdown(60)
})

async function handleResend() {
  if (isCounting.value || isResending.value || signupFlowStore.loading) return
  isResending.value = true
  try {
    await signupFlowStore.resendSignUpEmail()
    window.$message?.success?.(t('auth.signup.code_resent_success'))
    startCountdown(60)
  } catch (err: any) {
    window.$message?.error?.(err?.message || t('auth.signup.resend_failed'))
  } finally {
    isResending.value = false
  }
}

async function goBackEmail() {
  if (signupFlowStore.loading) return
  await signupFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signup/steps/email',
    query: signupFlowStore.getQueryParams()
  })
}

async function handleNext() {
  if (signupFlowStore.loading) return
  const trimmed = code.value.trim()

  if (!trimmed) {
    setErrorKey('auth.code_required')
    return
  }
  if (!/^\d{6}$/.test(trimmed)) {
    setErrorKey('auth.code_digits_only')
    return
  }

  try {
    const nextStep = await signupFlowStore.submitSignUpEtotp(trimmed)
    if (!nextStep) {
      throw new Error('服务端未返回下一步认证流程')
    }
    const targetPath = `/v1/signup/steps/${nextStep}`
    router.push({
      path: targetPath,
      query: signupFlowStore.getQueryParams()
    })
  } catch (err: any) {
    if (err?.message) {
      setRawError(err.message)
    } else {
      setErrorKey('auth.code_verify_failed')
    }
  }
}
</script>
