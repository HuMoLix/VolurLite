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
            {{ t('auth.signin.two_step_verification') }}
          </p>
          {{ t('auth.signin.two_step_sms_tip') }}
        </div>
      </div>

      <div>
        <GoogleTextField
          v-model="verificationCode"
          :label="t('auth.signin.enter_code_label')"
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
            v-model="trustDevice"
            type="checkbox"
            class="w-4 h-4 rounded border-google-border-light text-google-blue focus:ring-google-blue cursor-pointer"
          />
          <span>{{ t('auth.signin.trust_device_30_days') }}</span>
        </label>
        <a
          href="#"
          class="auth-forgot-link"
          :isScrim="true"
          @click.prevent="handleTryAnotherWayClick"
        >
          {{ t('auth.signin.another_way') }}
        </a>
      </div>
    </div>

    <AuthActionBar
      :primary-text="t('common.verify')"
      :secondary-text="t('auth.signin.try_another_way')"
      :primary-disabled="loading || isSubmitting"
      :secondary-disabled="loading || isSubmitting"
      :primary-loading="isSubmitting"
      primary-class="btn-verify-action"
      secondary-class="btn-try-another-way"
      @primary="handleVerifyChallenge"
      @secondary="handleTryAnotherWayClick"
    />
  </form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'

defineOptions({
  name: 'SmsotpChallengeView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.signin.security_verification_and_challenge'), '')

const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const verificationCode = ref('')
const trustDevice = ref(true)
const isSubmitting = ref(false)
const loading = computed(() => signinFlowStore.loading)

const { errorMsg, clearError, setErrorKey, setRawError } = useFormError(verificationCode)

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
      setErrorKey('auth.signin.security_code_required')
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
        setRawError(err.message)
      } else {
        setErrorKey('auth.signin.security_code_invalid')
      }
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>
