<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleVerifyChallenge">
    <div class="space-y-5">
      <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
        {{ t('auth.recaptcha_tip') }}
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

    <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
      <button
        type="button"
        class="auth-action-btn btn-secondary-action"
        :disabled="loading || isSubmitting"
        :isScrim="true"
        @click="handleBackToSelection"
      >
        {{ t('common.back') }}
      </button>
      <n-button
        type="primary"
        class="auth-action-btn btn-primary-action"
        :loading="isSubmitting"
        :disabled="loading || isSubmitting"
        :isScrim="true"
        @click="handleVerifyChallenge"
      >
        {{ t('auth.next') }}
      </n-button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import Recaptcha from '@/components/security/recaptcha.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { submitRecaptchaChallengeApi } from '@/api/captcha'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'RecaptchaChallengeView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.security_verification'), () => t('auth.security_verification_prompt'))

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const isRecaptchaPassed = ref(false)
const recaptToken = ref('')
const recaptTicket = ref('')
const recaptchaErrorKey = ref('')
const rawRecaptchaError = ref('')
const isSubmitting = ref(false)

const recaptchaError = computed(() => {
  if (recaptchaErrorKey.value) {
    return t(recaptchaErrorKey.value)
  }
  return rawRecaptchaError.value
})

function clearRecaptchaError(): void {
  recaptchaErrorKey.value = ''
  rawRecaptchaError.value = ''
}

const loading = computed(() => signinFlowStore.loading)

watch(isRecaptchaPassed, (val) => {
  if (val && (recaptchaErrorKey.value || rawRecaptchaError.value)) {
    clearRecaptchaError()
  }
})

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
    rawRecaptchaError.value = err
    recaptchaErrorKey.value = ''
  } else {
    recaptchaErrorKey.value = 'auth.recaptcha_failed'
    rawRecaptchaError.value = ''
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
    recaptchaErrorKey.value = 'auth.recaptcha_required'
    rawRecaptchaError.value = ''
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
      rawRecaptchaError.value = err.message
      recaptchaErrorKey.value = ''
    } else {
      recaptchaErrorKey.value = 'auth.recaptcha_failed'
      rawRecaptchaError.value = ''
    }
  } finally {
    isSubmitting.value = false
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
  cursor: pointer;
}

.btn-secondary-action {
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

  &:hover:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }

  &:active:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.16) !important;
    color: #0b57d0 !important;
  }

  &:disabled {
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

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

:global(.dark) .btn-secondary-action {
  color: #8ab4f8;

  &:hover:not(:disabled) {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:disabled {
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
