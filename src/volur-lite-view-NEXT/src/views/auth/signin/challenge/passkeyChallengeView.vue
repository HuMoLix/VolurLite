<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleOpenPasskeyModal">
    <div class="space-y-4">
      <div class="p-4 rounded-lg border border-google-border-light dark:border-google-border-dark space-y-2 text-xs">
        <div class="flex justify-between">
          <span class="text-google-text-muted">{{ t('auth.signin.login_account') }}</span>
          <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">{{ signinFlowStore.identifier }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-google-text-muted">{{ t('auth.signin.attempt_time') }}</span>
          <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">{{ currentTimeFormatted }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-google-text-muted">{{ t('auth.signin.verify_method') }}</span>
          <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">{{ t('auth.signin.passkey_method_name') }}</span>
        </div>
      </div>

      <div v-if="deviceError" class="flex items-center gap-1.5 text-xs text-google-red dark:text-google-red-dark">
        <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current shrink-0">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
        </svg>
        <span>{{ deviceError }}</span>
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
      @primary="handleOpenPasskeyModal"
      @secondary="handleTryAnotherWayClick"
    />

    <PasskeyModal
      v-model:visible="isModalVisible"
      mode="use"
      :dsh="signinFlowStore.dsh"
      :tl="signinFlowStore.tl"
      @success="handleVerifySuccess"
      @error="handleVerifyError"
    />
  </form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSigninFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import PasskeyModal from '@/components/security/PasskeyModal.vue'
import { AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'

defineOptions({
  name: 'PassKeyChallengeView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.signin.passkey_verify_title'), () => t('auth.signin.passkey_verify_desc'))

const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const isModalVisible = ref(false)
const isSubmitting = ref(false)
const loading = computed(() => signinFlowStore.loading)

const { errorMsg: deviceError, clearError: clearDeviceError, setError: setDeviceError } = useFormError()

const currentTimeFormatted = computed(() => {
  return new Date().toLocaleTimeString('zh-CN', { hour12: false })
})

async function handleTryAnotherWayClick(): Promise<void> {
  if (signinFlowStore.loading || isSubmitting.value) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signin/challenge/selection',
    query: signinFlowStore.getQueryParams({
      from: 'pk',
      page: '1'
    })
  })
}

async function handleOpenPasskeyModal(): Promise<void> {
  if (signinFlowStore.loading || isSubmitting.value) return
  clearDeviceError()
  await signinFlowStore.triggerScrim(undefined, 1000)
  isModalVisible.value = true
}

async function handleVerifySuccess(res: any): Promise<void> {
  if (res?.success) {
    await router.push(res.redirect || '/competitions')
    return
  }

  if (res?.nextStep) {
    await router.push({
      path: `/v1/signin/challenge/${res.nextStep}`,
      query: signinFlowStore.getQueryParams()
    })
    return
  }

  if (res?.errorMessage) {
    setDeviceError(res.errorMessage)
  }
}

function handleVerifyError(errorMsg: string): void {
  setDeviceError(errorMsg)
}
</script>
