<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleVerifyChallenge">
    <div class="space-y-4">
      <div>
        <GoogleTextField
          v-model="verificationCode"
          :label="t('auth.signin.enter_email_code_label')"
          autocomplete="one-time-code"
          autofocus
          :error-msg="errorMsg"
          @clear="clearError"
          @enter="handleVerifyChallenge"
        />
      </div>

      <div class="flex items-center justify-start pt-1">
        <label class="flex items-center gap-2 text-sm text-google-text-desc dark:text-google-text-darkDesc cursor-pointer select-none">
          <input
            v-model="trustDevice"
            type="checkbox"
            class="w-4 h-4 rounded border-google-border-light text-google-blue focus:ring-google-blue cursor-pointer"
          />
          <span>{{ t('auth.signin.trust_device_30_days') }}</span>
        </label>
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
  name: 'EmailotpChallengeView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.signin.email_verification'), '')

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
      from: 'emailotp',
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
      const res = await signinFlowStore.submitChallenge('emailotp', {
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
