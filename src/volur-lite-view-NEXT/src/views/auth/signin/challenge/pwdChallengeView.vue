<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleVerifyChallenge">
    <div>
      <div>
        <GoogleTextField
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          :label="t('auth.signin.password_label')"
          autocomplete="current-password"
          autofocus
          :error-msg="errorMsg"
          @clear="clearError"
          @enter="handleVerifyChallenge"
        />
      </div>

      <div class="flex items-center justify-between mt-2">
        <label class="flex items-center gap-2 text-sm text-google-text-desc dark:text-google-text-darkDesc cursor-pointer select-none">
          <input
            v-model="showPassword"
            type="checkbox"
            class="w-4 h-4 rounded border-google-border-light text-google-blue focus:ring-google-blue cursor-pointer"
          />
          <span>{{ t('auth.signin.show_password') }}</span>
        </label>
        <a
          href="#"
          class="auth-forgot-link"
          :isScrim="true"
          @click.prevent="handleForgotPassword"
        >
          {{ t('auth.signin.forgot_password') }}
        </a>
      </div>
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
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'

defineOptions({
  name: 'PwdChallengeView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.signin.password_verification'), '')

const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const password = ref('')
const showPassword = ref(false)
const isSubmitting = ref(false)
const loading = computed(() => signinFlowStore.loading)

const { errorMsg, clearError, setErrorKey, setRawError } = useFormError(password)

async function handleForgotPassword() {
  if (signinFlowStore.loading) return
  signinFlowStore.tl = ''
  signinFlowStore.cid = 0
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/recovery/password/selection',
    query: signinFlowStore.getQueryParams({ recoveryType: 'password' })
  })
}

async function handleTryAnotherWayClick() {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/challenge/selection',
    query: signinFlowStore.getQueryParams({
      from: 'pwd',
      page: '1'
    })
  })
}

async function handleVerifyChallenge() {
  if (signinFlowStore.loading || isSubmitting.value) return
  isSubmitting.value = true

  try {
    if (!password.value) {
      setErrorKey('auth.signin.password_required')
      return
    }

    try {
      const res = await signinFlowStore.submitChallenge('pwd', {
        password: password.value,
        rememberMe: true
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
        setErrorKey('auth.signin.password_incorrect')
      }
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>
