<template>
  <div class="flex flex-col justify-between h-full">
    <div class="flex-1 flex flex-col justify-center">
      <div v-if="!passkeyRegistered" class="space-y-4">
        <div class="p-4 rounded-lg border border-google-border-light dark:border-google-border-dark space-y-2 text-xs">
          <div class="flex justify-between">
            <span class="text-google-text-muted">{{ t('auth.signup.passkey_account_label') }}</span>
            <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">{{ displayAccount }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-google-text-muted">{{ t('auth.signup.passkey_method_label') }}</span>
            <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">{{ t('auth.signup.passkey_method_value') }}</span>
          </div>
        </div>

        <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary leading-relaxed">
          {{ t('auth.signup.passkey_guide_text') }}
        </p>

        <div v-if="deviceError" class="flex items-center gap-1.5 text-xs text-google-red dark:text-google-red-dark">
          <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current shrink-0">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
          </svg>
          <span>{{ deviceError }}</span>
        </div>
      </div>

      <div v-else class="flex flex-col items-center text-center py-4">
        <div class="w-16 h-16 rounded-full bg-green-500/10 dark:bg-green-500/20 flex items-center justify-center text-green-600 dark:text-green-400 mb-4">
          <svg class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>
        <h2 class="text-base font-medium text-google-text-primary dark:text-google-text-darkPrimary mb-2">
          {{ t('auth.signup.passkey_registered_title') }}
        </h2>
        <p class="text-sm text-google-text-secondary dark:text-google-text-darkSecondary max-w-sm leading-relaxed">
          {{ t('auth.signup.passkey_registered_desc') }}
        </p>
      </div>
    </div>

    <AuthActionBar
      :primary-text="passkeyRegistered ? t('common.next') : t('auth.signup.create_passkey')"
      :secondary-text="passkeyRegistered ? t('auth.signup.back_options') : t('auth.later')"
      :primary-disabled="loading || isSubmitting"
      :secondary-disabled="loading || isSubmitting"
      :primary-loading="isSubmitting"
      primary-class="btn-verify-action"
      secondary-class="btn-try-another-way"
      @primary="passkeyRegistered ? handleFinish() : handleOpenPasskeyModal()"
      @secondary="handleBackOptions"
    />

    <PasskeyModal
      v-model:visible="isModalVisible"
      mode="create"
      :dsh="signupFlowStore.dsh"
      :tl="signupFlowStore.tl"
      :username="signupFlowStore.username"
      @success="handleRegisterSuccess"
      @error="handleRegisterError"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSignupFlowStore } from '@/store/modules/flow'
import { useUserStore } from '@/store/modules/user'
import { useCardHeader } from '@/composables/useCardHeader'
import PasskeyModal from '@/components/security/PasskeyModal.vue'
import { AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'

defineOptions({
  name: 'PasskeyProtectView'
})

const { t } = useI18n()
const router = useRouter()
const signupFlowStore = useSignupFlowStore()

const isModalVisible = ref(false)
const isSubmitting = ref(false)
const passkeyRegistered = ref(false)
const loading = computed(() => signupFlowStore.loading)

const displayAccount = computed(() => {
  return signupFlowStore.username || signupFlowStore.email || ''
})

useCardHeader(
  () => passkeyRegistered.value ? t('auth.signup.passkey_registered_title') : t('auth.signup.passkey_protect_title'),
  () => passkeyRegistered.value ? t('auth.signup.passkey_registered_desc') : t('auth.signup.passkey_protect_desc')
)

const { errorMsg: deviceError, clearError: clearDeviceError, setError: setDeviceError } = useFormError()

async function handleOpenPasskeyModal(): Promise<void> {
  if (signupFlowStore.loading || isSubmitting.value) return
  clearDeviceError()
  isSubmitting.value = true
  try {
    await signupFlowStore.triggerScrim(undefined, 1000)
    isModalVisible.value = true
  } finally {
    isSubmitting.value = false
  }
}

function handleRegisterSuccess(res: any): void {
  if (res?.success) {
    passkeyRegistered.value = true
    window.$message?.success?.(t('auth.signup.passkey_registered_title'))
  }
}

function handleRegisterError(errorMsg: string): void {
  if (errorMsg.startsWith('auth.') || errorMsg.startsWith('common.')) {
    setDeviceError(t(errorMsg))
  } else {
    setDeviceError(errorMsg)
  }
}

async function handleBackOptions(): Promise<void> {
  if (signupFlowStore.loading || isSubmitting.value) return
  await signupFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signup/steps/pt/options',
    query: signupFlowStore.getQueryParams()
  })
}

async function handleFinish(): Promise<void> {
  if (signupFlowStore.loading || isSubmitting.value) return
  const redirect = signupFlowStore.continueUrl || '/competitions'
  await signupFlowStore.triggerScrim(undefined, 1000)
  if (signupFlowStore.username) {
    const userStore = useUserStore()
    userStore.setAuthenticatedUser({
      identifier: signupFlowStore.username,
      username: signupFlowStore.username,
      email: signupFlowStore.email
    })
    await userStore.fetchCurrentUser(true).catch(() => {})
  }
  signupFlowStore.resetFlow()
  await router.push(redirect)
}
</script>
