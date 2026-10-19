<template>
  <div class="flex flex-col justify-between h-full">
    <div class="flex-1 flex flex-col justify-center">
      <div v-if="!totpRegistered">
        <div v-if="isLoadingSetup" class="flex flex-col items-center justify-center py-10">
          <n-spin size="medium" />
          <p class="text-xs text-google-text-muted mt-3">
            {{ t('common.loading') }}
          </p>
        </div>

        <div v-else-if="setupData">
          <div v-if="currentStep === 'qrcode'" class="space-y-4 animate-fadeIn">
            <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary leading-relaxed text-center">
              {{ t('auth.signup.totp_scan_guide') }}
            </p>

            <div v-if="!showManualKey" class="flex justify-center">
              <div class="p-2 rounded-xl border border-google-border-base dark:border-neutral-700 flex items-center justify-center">
                <n-qr-code
                  :value="setupData.otpAuthUrl"
                  :size="152"
                  :padding="0"
                  background-color="transparent"
                  error-correction-level="M"
                />
              </div>
            </div>

            <div
              v-else
              class="p-4 rounded-xl border border-google-border-base dark:border-neutral-700 flex flex-col gap-2 animate-fadeIn"
            >
              <div class="flex items-center justify-between text-xs">
                <span class="text-google-text-muted dark:text-google-text-darkMuted">{{ t('auth.signup.totp_manual_secret_label') }}</span>
                <button
                  type="button"
                  class="text-google-blue dark:text-google-blue-dark hover:underline font-medium focus:outline-none bg-transparent p-0 border-0 cursor-pointer"
                  @click="copySecret"
                >
                  {{ t('auth.signup.totp_copy_secret') }}
                </button>
              </div>
              <div class="font-mono text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary select-all tracking-wider break-all py-1">
                {{ setupData.secret }}
              </div>
            </div>

            <div class="text-center pt-1">
              <button
                type="button"
                class="text-xs text-google-blue dark:text-google-blue-dark hover:underline focus:outline-none bg-transparent p-0 border-0 cursor-pointer transition-colors inline-flex items-center"
                @click="showManualKey = !showManualKey"
              >
                {{ showManualKey ? t('auth.signup.totp_scan_qr_toggle') : t('auth.signup.totp_manual_entry_toggle') }}
              </button>
            </div>
          </div>

          <div v-else-if="currentStep === 'input'" class="space-y-5 animate-fadeIn">
            <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary leading-relaxed">
              {{ t('auth.signup.totp_step_verify_desc') }}
            </p>

            <div>
              <GoogleTextField
                v-model="code"
                :label="t('auth.signup.totp_code_label')"
                :placeholder="t('auth.signup.totp_input_placeholder')"
                autocomplete="one-time-code"
                autofocus
                :maxlength="6"
                :disabled="loading || isSubmitting"
                :error-msg="codeError"
                @update:model-value="clearCodeError"
                @clear="clearCodeError"
                @enter="handleVerify"
              />
            </div>
          </div>
        </div>

        <div v-else class="text-center py-6">
          <p class="text-xs text-google-red dark:text-google-red-dark mb-3">
            {{ t('auth.signup.totp_load_failed') }}
          </p>
          <n-button
            secondary
            size="small"
            :round="true"
            @click="loadSetup"
          >
            {{ t('common.retry') }}
          </n-button>
        </div>
      </div>

      <div v-else class="flex flex-col items-center text-center py-4 animate-fadeIn">
        <div class="w-16 h-16 rounded-full bg-green-500/10 dark:bg-green-500/20 flex items-center justify-center text-green-600 dark:text-green-400 mb-4">
          <svg class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>
        <h2 class="text-base font-medium text-google-text-primary dark:text-google-text-darkPrimary mb-2">
          {{ t('auth.signup.totp_registered_title') }}
        </h2>
        <p class="text-sm text-google-text-secondary dark:text-google-text-darkSecondary max-w-sm leading-relaxed">
          {{ t('auth.signup.totp_registered_desc') }}
        </p>
      </div>
    </div>

    <AuthActionBar
      :primary-text="primaryButtonText"
      :secondary-text="secondaryButtonText"
      :primary-disabled="primaryButtonDisabled"
      :secondary-disabled="loading || isSubmitting"
      :primary-loading="isSubmitting"
      :is-scrim="true"
      primary-class="btn-verify-action"
      secondary-class="btn-try-another-way"
      @primary="handlePrimaryAction"
      @secondary="handleSecondaryAction"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NQrCode, NSpin, NButton } from 'naive-ui'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSignupFlowStore } from '@/store/modules/flow'
import { useUserStore } from '@/store/modules/user'
import { useCardHeader } from '@/composables/useCardHeader'
import { useFormError } from '@/composables/useFormError'
import { AuthActionBar } from '@/components/auth'
import { fetchTotpSetupApi } from '@/api/auth'
import type { TotpSetupVO } from '@/types/auth/flow'

defineOptions({
  name: 'TotpProtectView'
})

const { t } = useI18n()
const router = useRouter()
const signupFlowStore = useSignupFlowStore()

const currentStep = ref<'qrcode' | 'input'>('qrcode')
const isLoadingSetup = ref(false)
const isSubmitting = ref(false)
const totpRegistered = ref(false)
const showManualKey = ref(false)
const code = ref('')
const setupData = ref<TotpSetupVO | null>(null)

const loading = computed(() => signupFlowStore.loading)

useCardHeader(
  () => {
    if (totpRegistered.value) return t('auth.signup.totp_registered_title')
    if (currentStep.value === 'input') return t('auth.signup.totp_step_verify_title')
    return t('auth.signup.totp_protect_title')
  },
  () => {
    if (totpRegistered.value) return t('auth.signup.totp_registered_desc')
    if (currentStep.value === 'input') return t('auth.signup.totp_step_verify_desc')
    return t('auth.signup.totp_protect_desc')
  }
)

const { errorMsg: codeError, clearError: clearCodeError, setError: setCodeError } = useFormError()

const primaryButtonText = computed(() => {
  if (totpRegistered.value) return t('common.next')
  if (currentStep.value === 'input') return t('common.verify')
  return t('common.next')
})

const secondaryButtonText = computed(() => {
  if (totpRegistered.value) return t('auth.signup.back_options')
  if (currentStep.value === 'input') return t('common.back')
  return t('auth.signup.back_options')
})

const primaryButtonDisabled = computed(() => {
  if (loading.value || isSubmitting.value) return true
  if (totpRegistered.value) return false
  if (currentStep.value === 'qrcode') return !setupData.value
  return code.value.trim().length !== 6
})

async function loadSetup(): Promise<void> {
  if (!signupFlowStore.dsh || !signupFlowStore.tl) return
  isLoadingSetup.value = true
  clearCodeError()
  try {
    const res = await fetchTotpSetupApi({
      dsh: signupFlowStore.dsh,
      tl: signupFlowStore.tl
    })
    if (res.data) {
      setupData.value = res.data
    }
  } catch (err: any) {
    setCodeError(err?.message || t('auth.signup.totp_load_failed'))
  } finally {
    isLoadingSetup.value = false
  }
}

async function copySecret(): Promise<void> {
  if (!setupData.value?.secret) return
  try {
    await navigator.clipboard.writeText(setupData.value.secret)
    window.$message?.success?.(t('auth.signup.totp_secret_copied'))
  } catch {
  }
}

async function handleGoToInput(): Promise<void> {
  if (loading.value || isSubmitting.value || !setupData.value) return
  await signupFlowStore.triggerScrim(undefined, 600)
  currentStep.value = 'input'
}

async function handleBackToQrcode(): Promise<void> {
  if (loading.value || isSubmitting.value) return
  clearCodeError()
  await signupFlowStore.triggerScrim(undefined, 600)
  currentStep.value = 'qrcode'
}

async function handleVerify(): Promise<void> {
  if (loading.value || isSubmitting.value || totpRegistered.value) return
  const cleanCode = code.value.trim()
  if (!cleanCode) {
    setCodeError(t('auth.signup.totp_error_required'))
    return
  }
  if (!/^\d{6}$/.test(cleanCode)) {
    setCodeError(t('auth.signup.totp_error_format'))
    return
  }

  isSubmitting.value = true
  clearCodeError()
  try {
    await signupFlowStore.submitTotpEnable(cleanCode)
    totpRegistered.value = true
    window.$message?.success?.(t('auth.signup.totp_registered_title'))
  } catch (err: any) {
    setCodeError(err?.message || t('auth.code_error'))
  } finally {
    isSubmitting.value = false
  }
}

async function handlePrimaryAction(): Promise<void> {
  if (totpRegistered.value) {
    await handleFinish()
  } else if (currentStep.value === 'qrcode') {
    await handleGoToInput()
  } else {
    await handleVerify()
  }
}

async function handleSecondaryAction(): Promise<void> {
  if (totpRegistered.value) {
    await handleBackOptions()
  } else if (currentStep.value === 'input') {
    await handleBackToQrcode()
  } else {
    await handleBackOptions()
  }
}

async function handleBackOptions(): Promise<void> {
  if (loading.value || isSubmitting.value) return
  await signupFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signup/steps/pt/options',
    query: signupFlowStore.getQueryParams()
  })
}

async function handleFinish(): Promise<void> {
  if (loading.value || isSubmitting.value) return
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

onMounted(() => {
  loadSetup()
})
</script>

<style scoped>
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fadeIn {
  animation: fadeIn 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>
