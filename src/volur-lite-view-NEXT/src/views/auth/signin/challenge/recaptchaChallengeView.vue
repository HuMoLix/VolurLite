<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleVerifyChallenge">
    <div class="space-y-5">
      <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
        {{ t('captcha.recaptcha.tip') }}
      </p>

      <Recaptcha
        v-model:passed="isRecaptchaPassed"
        v-model:token="recaptchaToken"
        v-model:ticket="recaptchaTicket"
        :dsh="signinFlowStore.dsh"
        :tl="signinFlowStore.tl"
        :error-msg="recaptchaError"
        @success="handleRecaptchaSuccess"
        @error="handleRecaptchaError"
      />
    </div>

    <AuthActionBar
      :primary-text="t('auth.next')"
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
import Recaptcha from '@/components/security/recaptcha.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'

defineOptions({
  name: 'RecaptchaChallengeView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.security_verification'), () => t('auth.signin.security_verification_prompt'))

const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const isRecaptchaPassed = ref(false)
const recaptchaToken = ref('')
const recaptchaTicket = ref('')
const isSubmitting = ref(false)
const loading = computed(() => signinFlowStore.loading)

const { errorMsg: recaptchaError, clearError: clearRecaptchaError, setErrorKey: setRecaptchaErrorKey, setRawError: setRawRecaptchaError } = useFormError(isRecaptchaPassed)

function handleRecaptchaSuccess(payload: { token: string; ticket: string; metrics?: Record<string, unknown> }): void {
  clearRecaptchaError()
  recaptchaToken.value = payload.token
  recaptchaTicket.value = payload.ticket || payload.token
}

function handleRecaptchaError(err: string): void {
  if (err) {
    setRawRecaptchaError(err)
  } else {
    setRecaptchaErrorKey('captcha.recaptcha.failed')
  }
}

async function handleTryAnotherWayClick() {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/challenge/selection',
    query: signinFlowStore.getQueryParams({
      from: 'recaptcha',
      page: '1'
    })
  })
}

async function handleVerifyChallenge() {
  if (signinFlowStore.loading || isSubmitting.value) return
  isSubmitting.value = true

  try {
    if (!isRecaptchaPassed.value || !recaptchaTicket.value) {
      setRecaptchaErrorKey('captcha.recaptcha.required')
      return
    }

    try {
      const res = await signinFlowStore.submitChallenge('recaptcha', {
        captchaTicket: recaptchaTicket.value,
        captchaCode: recaptchaTicket.value
      })

      if (res.nextStep === 'identifier') {
        const errorText = res.errorMessage || 'auth.account_not_found'
        signinFlowStore.setAccountError(errorText)
        router.replace({
          path: '/v1/signin/identifier',
          query: {
            ...signinFlowStore.getQueryParams(),
            accountError: errorText
          }
        })
        return
      }

      if (!res.nextStep) {
        throw new Error('服务端未返回下一步挑战流程')
      }
      router.push({
        path: `/v1/signin/challenge/${res.nextStep}`,
        query: signinFlowStore.getQueryParams()
      })
    } catch (err: any) {
      const msg = err?.message || ''
      if (msg.includes('用户名或邮箱不存在') || msg.includes('不存在')) {
        const errorText = msg || 'auth.account_not_found'
        signinFlowStore.setAccountError(errorText)
        router.replace({
          path: '/v1/signin/identifier',
          query: {
            ...signinFlowStore.getQueryParams(),
            accountError: errorText
          }
        })
        return
      }
      if (msg) {
        setRawRecaptchaError(msg)
      } else {
        setRecaptchaErrorKey('captcha.recaptcha.failed')
      }
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>
