<template>
  <div class="auth-card__body">
    <div class="auth-card__left">
      <h1 class="auth-card__title">{{ t('auth.recovery_password_reset_title') }}</h1>
      <p class="auth-card__subtitle">{{ t('auth.recovery_password_reset_subtitle') }}</p>
      <div id="auth-header-extra" class="auth-card__header-extra" />
    </div>

    <div class="auth-card__right">
      <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleResetPassword">
        <div class="space-y-4">
          <div class="space-y-3">
            <div>
              <GoogleTextField
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                :label="t('auth.recovery_password_new_label')"
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
                :label="t('auth.recovery_password_confirm_label')"
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
              <span>{{ t('auth.show_password') }}</span>
            </label>
          </div>

          <p class="text-xs text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
            {{ t('auth.password_hint') }}
          </p>
        </div>

        <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
          <n-button
            quaternary
            type="primary"
            :round="true"
            class="auth-action-btn btn-secondary-action"
            :disabled="loading || isSubmitting"
            :isScrim="true"
            @click="handleBackToOtp"
          >
            {{ t('common.back') }}
          </n-button>
          <n-button
            type="primary"
            class="auth-action-btn btn-primary-action"
            :loading="isSubmitting"
            :disabled="loading || isSubmitting"
            :isScrim="true"
            @click="handleResetPassword"
          >
            {{ t('auth.next') }}
          </n-button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'
import { submitPasswordResetApi } from '@/api/auth'
import { encryptFlowCredential } from '@/utils/flow-crypto'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'PasswordResetView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.recovery_password_reset_title'), () => t('auth.recovery_password_reset_subtitle'))

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const isSubmitting = ref(false)
const errorKey = ref('')
const rawErrorMsg = ref('')
const confirmErrorKey = ref('')

const loading = computed(() => signinFlowStore.loading)

const errorMsg = computed(() => {
  if (errorKey.value) {
    return t(errorKey.value)
  }
  return rawErrorMsg.value
})

const confirmErrorMsg = computed(() => {
  if (confirmErrorKey.value) {
    return t(confirmErrorKey.value)
  }
  return ''
})

function clearError(): void {
  errorKey.value = ''
  rawErrorMsg.value = ''
  confirmErrorKey.value = ''
}

watch([password, confirmPassword], () => {
  if (errorKey.value || rawErrorMsg.value || confirmErrorKey.value) {
    clearError()
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
    errorKey.value = 'auth.password_required'
    rawErrorMsg.value = ''
    return
  }
  if (p.length < 6 || p.length > 32 || !PASSWORD_REGEX.test(p)) {
    errorKey.value = 'auth.password_complexity_error'
    rawErrorMsg.value = ''
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
      rawErrorMsg.value = (err as { message: string }).message
      errorKey.value = ''
    } else {
      errorKey.value = 'auth.password_setup_failed'
      rawErrorMsg.value = ''
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
}

.btn-secondary-action {
  color: #0b57d0 !important;
  background-color: transparent !important;
  border: none !important;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  :deep(.n-button__content),
  .n-button__content {
    color: #0b57d0 !important;
  }

  :deep(.n-button__state-border) {
    border: none !important;
  }

  &:hover:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;

    :deep(.n-button__content),
    .n-button__content {
      color: #0b57d0 !important;
    }
  }

  &:active:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.16) !important;
    color: #0b57d0 !important;
  }

  &:disabled,
  &.n-button--disabled {
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

  &:disabled,
  &.n-button--disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

:global(.dark) .btn-secondary-action {
  color: #8ab4f8 !important;

  :deep(.n-button__content),
  .n-button__content {
    color: #8ab4f8 !important;
  }

  &:hover:not(:disabled) {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;

    :deep(.n-button__content),
    .n-button__content {
      color: #8ab4f8 !important;
    }
  }

  &:disabled,
  &.n-button--disabled {
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
