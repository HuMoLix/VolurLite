<template>
  <AuthCardShell
    :title="t('auth.recovery.reset_title')"
    :subtitle="t('auth.recovery.reset_subtitle')"
  >
    <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleResetPassword">
      <div class="space-y-4">
        <div class="space-y-3">
          <div>
            <GoogleTextField
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              :label="t('auth.recovery.new_password_label')"
              autocomplete="new-password"
              autofocus
              :error-msg="errorMsg"
              @clear="clearError"
              @enter="handleResetPassword"
            />
          </div>

          <div>
            <GoogleTextField
              v-model="confirmPassword"
              :type="showPassword ? 'text' : 'password'"
              :label="t('auth.recovery.confirm_password_label')"
              autocomplete="new-password"
              :error-msg="confirmErrorMsg"
              @clear="clearError"
              @enter="handleResetPassword"
            />
          </div>
        </div>

        <div class="flex items-center justify-between">
          <label class="flex items-center gap-2 text-sm text-google-text-desc dark:text-google-text-darkDesc cursor-pointer select-none">
            <input
              v-model="showPassword"
              type="checkbox"
              class="w-4 h-4 rounded border-google-border-light text-google-blue focus:ring-google-blue cursor-pointer"
            />
            <span>{{ t('auth.signin.show_password') }}</span>
          </label>
        </div>

        <p class="text-xs text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
          {{ t('auth.signup.password_hint') }}
        </p>
      </div>

      <AuthActionBar
        :primary-text="t('auth.next')"
        :secondary-text="t('common.back')"
        :primary-disabled="loading || isSubmitting"
        :secondary-disabled="loading || isSubmitting"
        :primary-loading="isSubmitting"
        @primary="handleResetPassword"
        @secondary="handleBackToOtp"
      />
    </form>
  </AuthCardShell>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { submitPasswordResetApi } from '@/api/auth'
import { encryptFlowCredential } from '@/utils/flow-crypto'
import { AuthCardShell, AuthActionBar } from '@/components/auth'
import { useFormError } from '@/composables/useFormError'

defineOptions({
  name: 'PasswordResetView'
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const isSubmitting = ref(false)
const confirmErrorKey = ref('')

const loading = computed(() => signinFlowStore.loading)
const { errorMsg, clearError: clearMainError, setErrorKey, setRawError } = useFormError([password, confirmPassword])

const confirmErrorMsg = computed(() => {
  if (confirmErrorKey.value) {
    return t(confirmErrorKey.value)
  }
  return ''
})

function clearError(): void {
  clearMainError()
  confirmErrorKey.value = ''
}

watch([password, confirmPassword], () => {
  if (confirmErrorKey.value) {
    confirmErrorKey.value = ''
  }
})

onMounted(async () => {
  await signinFlowStore.ensureFlowParams(route, router)
  if (!signinFlowStore.dsh || !signinFlowStore.tl) {
    await router.push({
      path: '/v1/signin/identifier',
      query: signinFlowStore.getQueryParams()
    })
  }
})

async function handleBackToOtp(): Promise<void> {
  if (loading.value || isSubmitting.value) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/recovery/password/emailotp',
    query: signinFlowStore.getQueryParams({ recoveryType: 'password' })
  })
}

const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\d@$!%*?&._#^&+=]{6,32}$/

async function handleResetPassword(): Promise<void> {
  if (loading.value || isSubmitting.value) return
  const p = password.value.trim()
  const cp = confirmPassword.value.trim()

  if (!p) {
    setErrorKey('auth.signin.password_required')
    return
  }
  if (p.length < 6 || p.length > 32 || !PASSWORD_REGEX.test(p)) {
    setErrorKey('auth.password_complexity_error')
    return
  }
  if (!cp) {
    confirmErrorKey.value = 'auth.confirm_password_required'
    return
  }
  if (p !== cp) {
    confirmErrorKey.value = 'auth.passwords_not_match'
    return
  }

  isSubmitting.value = true
  try {
    const encryptedPassword = await encryptFlowCredential(
      p,
      signinFlowStore.flowName || 'VlifWebLogin',
      signinFlowStore.flowEntry || 'WebServiceLogin',
      signinFlowStore.dsh
    )
    const encryptedConfirmPassword = await encryptFlowCredential(
      cp,
      signinFlowStore.flowName || 'VlifWebLogin',
      signinFlowStore.flowEntry || 'WebServiceLogin',
      signinFlowStore.dsh
    )

    await signinFlowStore.withLoading(async () => {
      await submitPasswordResetApi({
        dsh: signinFlowStore.dsh,
        tl: signinFlowStore.tl,
        cid: signinFlowStore.cid,
        password: encryptedPassword,
        confirmPassword: encryptedConfirmPassword
      })
    }, 1000)

    signinFlowStore.resetFlow()
    await signinFlowStore.triggerScrim(undefined, 1000)
    await router.push({
      path: '/v1/signin/identifier',
      query: signinFlowStore.getQueryParams()
    })
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
      setRawError((err as { message: string }).message)
    } else {
      setErrorKey('auth.signup.password_setup_failed')
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>
