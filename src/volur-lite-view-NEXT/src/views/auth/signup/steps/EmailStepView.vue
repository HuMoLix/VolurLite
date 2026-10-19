<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleNext">
    <div class="space-y-3">
      <div>
        <GoogleTextField
          v-model="email"
          type="email"
          :label="t('auth.email_label')"
          autocomplete="email"
          autofocus
          :error-msg="errorMsg"
          @clear="clearError"
          @enter="handleNext"
        />
      </div>

      <p class="text-xs text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
        {{ t('auth.signup.email_tip') }}
      </p>
    </div>

    <AuthActionBar
      :primary-text="t('auth.next')"
      :secondary-text="t('auth.signup.back_to_previous')"
      :primary-disabled="loading"
      :secondary-disabled="loading"
      @primary="handleNext"
      @secondary="goBackName"
    />
  </form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSignupFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'

defineOptions({
  name: 'EmailStepView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.email_label'), () => t('auth.signup.email_tip'))

const router = useRouter()
const signupFlowStore = useSignupFlowStore()

const email = ref(signupFlowStore.email || '')
const loading = computed(() => signupFlowStore.loading)

const { errorMsg, clearError, setErrorKey, setRawError } = useFormError(email)

async function goBackName() {
  if (signupFlowStore.loading) return
  await signupFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signup/steps/name',
    query: signupFlowStore.getQueryParams()
  })
}

async function handleNext() {
  if (signupFlowStore.loading) return
  const trimmed = email.value.trim()

  if (!trimmed) {
    setErrorKey('auth.email_required')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
    setErrorKey('auth.email_format_error')
    return
  }

  try {
    const nextStep = await signupFlowStore.submitSignUpEmail(trimmed)
    router.push({
      path: `/v1/signup/steps/${nextStep}`,
      query: signupFlowStore.getQueryParams()
    })
  } catch (err: any) {
    if (err?.message) {
      setRawError(err.message)
    } else {
      setErrorKey('auth.email_submit_failed')
    }
  }
}
</script>
