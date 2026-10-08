<template>
  <form class="space-y-5 flex flex-col justify-between h-full" @submit.prevent="handleNext">
    <div class="space-y-3">
      <div class="space-y-3">
        <GoogleTextField
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          :label="t('auth.password_label')"
          autocomplete="new-password"
          autofocus
          :error-msg="passwordError"
          @clear="clearPasswordError"
        />

        <GoogleTextField
          v-model="confirmPassword"
          :type="showPassword ? 'text' : 'password'"
          :label="t('auth.confirm_password_label')"
          autocomplete="new-password"
          :error-msg="confirmPasswordError"
          @clear="clearConfirmPasswordError"
          @enter="handleNext"
        />
      </div>

      <div class="flex items-center gap-2 pt-0.5">
        <label class="inline-flex items-center gap-2 text-sm text-google-text-desc dark:text-google-text-darkDesc cursor-pointer select-none">
          <input
            type="checkbox"
            v-model="showPassword"
            class="w-4 h-4 rounded border-google-border-light text-google-blue focus:ring-google-blue cursor-pointer"
          />
          <span>{{ t('auth.show_password') }}</span>
        </label>
      </div>

      <p class="text-xs text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
        {{ t('auth.password_hint') }}
      </p>
    </div>

    <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
      <n-button
        type="primary"
        class="auth-action-btn btn-primary-action"
        :disabled="loading"
        :isScrim="true"
        @click="handleNext"
      >
        {{ t('auth.finish_signup') }}
      </n-button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSignupFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'PasswordStepView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.password_label'), () => t('auth.password_hint'))

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
    passwordKey.value = 'auth.password_required'
    hasError = true
  } else if (p.length < 6 || p.length > 32 || !PASSWORD_REGEX.test(p)) {
    passwordKey.value = 'auth.password_complexity_error'
    hasError = true
  }

  if (!cp) {
    confirmPasswordKey.value = 'auth.confirm_password_required'
    hasError = true
  } else if (p !== cp) {
    confirmPasswordKey.value = 'auth.passwords_not_match'
    hasError = true
  }

  if (hasError) return

  try {
    const { nextStep, status } = await signupFlowStore.submitSignUpPassword(p, cp)
    if (status === 'COMPLETE') {
      window.$message?.success?.(t('auth.account_created_success'))
    }
    router.push({
      path: `/v1/signup/steps/${nextStep}`,
      query: signupFlowStore.getQueryParams()
    })
  } catch (err: any) {
    window.$message?.error?.(err?.message || t('auth.password_setup_failed'))
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
}

.btn-primary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: #0b57d0 !important;
  color: #ffffff !important;
  border: none !important;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  :deep(.n-button__content),
  .n-button__content {
    color: #ffffff !important;
  }

  :deep(.n-button__state-border) {
    border: none !important;
  }

  &:hover {
    background-color: #0842a0 !important;
    color: #ffffff !important;
    box-shadow: 0 1px 2px 0 rgba(60, 64, 67, 0.3), 0 1px 3px 1px rgba(60, 64, 67, 0.15);

    :deep(.n-button__content),
    .n-button__content {
      color: #ffffff !important;
    }
  }

  &:active {
    background-color: #063980 !important;
    color: #ffffff !important;
    transform: scale(0.98);
    box-shadow: 0 1px 2px 0 rgba(60, 64, 67, 0.3);

    :deep(.n-button__content),
    .n-button__content {
      color: #ffffff !important;
    }
  }

  &:focus,
  &:focus-visible {
    background-color: #0b57d0 !important;
    color: #ffffff !important;

    :deep(.n-button__content),
    .n-button__content {
      color: #ffffff !important;
    }
  }
}

:global(.dark) .btn-primary-action {
  background-color: #8ab4f8 !important;
  color: #202124 !important;

  :deep(.n-button__content),
  .n-button__content {
    color: #202124 !important;
  }

  &:hover {
    background-color: #aecbfa !important;
    color: #202124 !important;
  }

  &:active {
    background-color: #669df6 !important;
    color: #202124 !important;
    transform: scale(0.98);

    :deep(.n-button__content),
    .n-button__content {
      color: #202124 !important;
    }
  }
}
</style>
