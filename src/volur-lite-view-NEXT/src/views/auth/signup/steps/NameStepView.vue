<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleNext">
    <div class="space-y-3">
      <div>
        <GoogleTextField
          v-model="username"
          :label="t('auth.signup.username_placeholder')"
          autocomplete="username"
          autofocus
          :error-msg="errorMsg"
          @clear="clearError"
          @enter="handleNext"
        />
      </div>

      <p class="text-xs text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
        {{ t('auth.signup.username_tip') }}
      </p>
    </div>

    <AuthActionBar
      :primary-text="t('auth.next')"
      :secondary-text="t('auth.signin_instead')"
      :primary-disabled="loading"
      :secondary-disabled="loading"
      secondary-class="btn-signin-instead"
      @primary="handleNext"
      @secondary="goToSignIn"
    />
  </form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSignupFlowStore, useSigninFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'

defineOptions({
  name: 'NameStepView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.signup.title'), () => t('auth.username_label'))

const router = useRouter()
const signupFlowStore = useSignupFlowStore()
const signinFlowStore = useSigninFlowStore()

const username = ref(signupFlowStore.username || '')
const loading = computed(() => signupFlowStore.loading)

const { errorMsg, clearError, setErrorKey, setRawError } = useFormError(username)

async function goToSignIn() {
  if (signupFlowStore.loading) return
  await signupFlowStore.triggerScrim(undefined, 1000)
  signinFlowStore.setFlow('VlifWebLogin', 'WebServiceLogin')
  await router.push({
    path: '/v1/signin/identifier',
    query: {
      continue: signupFlowStore.continueUrl || '/competitions',
      followup: '/v1/signin/identifier',
      flowName: 'VlifWebLogin',
      flowEntry: 'WebServiceLogin'
    }
  })
}

async function handleNext() {
  if (signupFlowStore.loading) return
  const trimmed = username.value.trim()

  if (!trimmed) {
    setErrorKey('auth.username_required')
    return
  }
  if (trimmed.length < 3 || trimmed.length > 32) {
    setErrorKey('auth.username_length_error')
    return
  }
  if (!/^[a-zA-Z0-9_-]+$/.test(trimmed)) {
    setErrorKey('auth.username_pattern_error')
    return
  }

  try {
    const res = await signupFlowStore.submitSignUpName(trimmed)
    if (!res.nextStep) {
      throw new Error('服务端未返回下一步认证流程')
    }
    const targetPath = `/v1/signup/steps/${res.nextStep}`
    router.push({
      path: targetPath,
      query: signupFlowStore.getQueryParams()
    })
  } catch (err: any) {
    if (err?.message) {
      setRawError(err.message)
    } else {
      setErrorKey('auth.username_verify_failed')
    }
  }
}
</script>
