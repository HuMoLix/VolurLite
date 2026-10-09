<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleVerifyChallenge">
    <div class="space-y-5">
      <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
        {{ t('captcha.recaptcha.tip') }}
      </p>

      <Recaptcha
        v-model:passed="isRecaptchaPassed"
        v-model:token="recaptToken"
        v-model:ticket="recaptTicket"
        :dsh="signinFlowStore.dsh"
        :tl="signinFlowStore.tl"
        :error-msg="recaptchaError"
        @success="handleRecaptchaSuccess"
        @error="handleRecaptchaError"
      />
    </div>

    <AuthActionBar
      :primary-text="t('auth.next')"
      :secondary-text="t('common.back')"
      :primary-disabled="loading || isSubmitting"
      :secondary-disabled="loading || isSubmitting"
      :primary-loading="isSubmitting"
      @primary="handleVerifyChallenge"
      @secondary="handleBackToSelection"
    />
  </form>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Recaptcha from '@/components/security/recaptcha.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { submitRecaptchaChallengeApi } from '@/api/captcha'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'

defineOptions({
  name: 'RecaptchaChallengeView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.security_verification'), () => t('auth.signin.security_verification_prompt'))

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const isRecaptchaPassed = ref(false)
const recaptToken = ref('')
const recaptTicket = ref('')
const isSubmitting = ref(false)
const loading = computed(() => signinFlowStore.loading)

const { errorMsg: recaptchaError, clearError: clearRecaptchaError, setErrorKey: setRecaptchaErrorKey, setRawError: setRawRecaptchaError } = useFormError(isRecaptchaPassed)

onMounted(() => {
  signinFlowStore.ensureFlowParams(route, router)
})

function handleRecaptchaSuccess(payload: { token: string; ticket: string }): void {
  clearRecaptchaError()
  recaptToken.value = payload.token
  recaptTicket.value = payload.ticket || payload.token
}

function handleRecaptchaError(err: string): void {
  if (err) {
    setRawRecaptchaError(err)
  } else {
    setRecaptchaErrorKey('captcha.recaptcha.failed')
  }
}

async function handleBackToSelection(): Promise<void> {
  if (loading.value || isSubmitting.value) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  const isPassword = route.query.recoveryType === 'password'
  await router.push({
    path: isPassword ? '/v1/recovery/password/selection' : '/v1/recovery/username/selection',
    query: signinFlowStore.getQueryParams({ recoveryType: isPassword ? 'password' : 'username' })
  })
}

async function handleVerifyChallenge(): Promise<void> {
  if (loading.value || isSubmitting.value) return
  if (!isRecaptchaPassed.value || !recaptTicket.value) {
    setRecaptchaErrorKey('captcha.recaptcha.required')
    return
  }

  isSubmitting.value = true
  try {
    let nextStep = ''
    const recoveryType = (route.query.recoveryType as string) || ''
    await signinFlowStore.withLoading(async () => {
      const res = await submitRecaptchaChallengeApi({
        dsh: signinFlowStore.dsh,
        tl: signinFlowStore.tl,
        cid: signinFlowStore.cid,
        recaptchaTicket: recaptTicket.value,
        recoveryType: recoveryType || undefined
      })
      if (res.data?.tl) {
        signinFlowStore.tl = res.data.tl
      }
      if (typeof res.data?.cid === 'number') {
        signinFlowStore.cid = res.data.cid
      }
      nextStep = res.data?.nextStep || ''
    }, 1000)

    if (nextStep === 'emailotp') {
      await router.push({
        path: '/v1/recovery/password/emailotp',
        query: signinFlowStore.getQueryParams({ recoveryType: 'password' })
      })
    } else if (nextStep === 'email') {
      await router.push({
        path: '/v1/recovery/username/email',
        query: signinFlowStore.getQueryParams({ recoveryType: 'username' })
      })
    } else if (nextStep === 'smsotp') {
      await router.push({
        path: '/v1/recovery/challenge/smsotp',
        query: signinFlowStore.getQueryParams({ recoveryType: (route.query.recoveryType as string) || 'username' })
      })
    } else {
      throw new Error('未知的下一步挑战流程: ' + nextStep)
    }
  } catch (err: any) {
    if (err?.message) {
      setRawRecaptchaError(err.message)
    } else {
      setRecaptchaErrorKey('captcha.recaptcha.failed')
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>
