<template>
  <form class="space-y-5 flex flex-col justify-between h-full" @submit.prevent="handleNext">
    <div class="space-y-3">
      <div class="space-y-3">
        <GoogleTextField
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          :label="t('auth.signin.password_label')"
          autocomplete="new-password"
          autofocus
          :error-msg="passwordError"
          @clear="clearPasswordError"
        />

        <GoogleTextField
          v-model="confirmPassword"
          :type="showPassword ? 'text' : 'password'"
          :label="t('auth.signin.confirm_password_label')"
          autocomplete="new-password"
          :error-msg="confirmPasswordError"
          @clear="clearConfirmPasswordError"
          @enter="handleNext"
        />
      </div>

      <div class="flex items-center gap-2 pt-0.5">
        <label class="inline-flex items-center gap-2 text-sm text-google-text-desc dark:text-google-text-darkDesc cursor-pointer select-none">
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
      :primary-text="t('auth.signup.finish_signup')"
      :primary-disabled="loading"
      :show-secondary="false"
      @primary="handleNext"
    />
  </form>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSignupFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'
import { AuthActionBar } from '@/components/auth'

defineOptions({
  name: 'PasswordStepView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.signin.password_label'), () => t('auth.signup.password_hint'))

const router = useRouter()
const signupFlowStore = useSignupFlowStore()

const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const loading = computed(() => signupFlowStore.loading)

const passwordKey = ref('')
const rawPasswordMsg = ref('')
const confirmPasswordKey = ref('')
const rawConfirmPasswordMsg = ref('')

const passwordError = computed(() => {
  if (passwordKey.value) {
    return t(passwordKey.value)
  }
  return rawPasswordMsg.value
})

const confirmPasswordError = computed(() => {
  if (confirmPasswordKey.value) {
    return t(confirmPasswordKey.value)
  }
  return rawConfirmPasswordMsg.value
})

function clearPasswordError(): void {
  passwordKey.value = ''
  rawPasswordMsg.value = ''
}

function clearConfirmPasswordError(): void {
  confirmPasswordKey.value = ''
  rawConfirmPasswordMsg.value = ''
}

watch(password, () => {
  if (passwordKey.value || rawPasswordMsg.value) {
    clearPasswordError()
  }
})

watch(confirmPassword, () => {
  if (confirmPasswordKey.value || rawConfirmPasswordMsg.value) {
    clearConfirmPasswordError()
  }
})

const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\d@$!%*?&._#^&+=]{6,32}$/

async function handleNext() {
  if (signupFlowStore.loading) return

  clearPasswordError()
  clearConfirmPasswordError()
  let hasError = false

  const p = password.value.trim()
  const cp = confirmPassword.value.trim()

  if (!p) {
    passwordKey.value = 'auth.signin.password_required'
    hasError = true
  } else if (p.length < 6 || p.length > 32 || !PASSWORD_REGEX.test(p)) {
    passwordKey.value = 'auth.password_complexity_error'
    hasError = true
  }

  if (!cp) {
    confirmPasswordKey.value = 'auth.confirm_password_required'
    hasError = true
  } else if (p && cp && p !== cp) {
    confirmPasswordKey.value = 'auth.passwords_not_match'
    hasError = true
  }

  if (hasError) return

  try {
    const res = await signupFlowStore.submitSignUpPassword(p, cp)
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
      rawPasswordMsg.value = err.message
      passwordKey.value = ''
    } else {
      passwordKey.value = 'auth.signup.password_setup_failed'
      rawPasswordMsg.value = ''
    }
  }
}
</script>
