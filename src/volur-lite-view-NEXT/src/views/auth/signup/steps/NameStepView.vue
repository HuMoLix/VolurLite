<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleNext">
    <div class="space-y-3">
      <div>
        <GoogleTextField
          v-model="username"
          :label="t('auth.username_placeholder')"
          autocomplete="username"
          autofocus
          :error-msg="errorMsg"
          @clear="clearError"
          @enter="handleNext"
        />
      </div>

      <p class="text-xs text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
        {{ t('auth.username_tip') }}
      </p>
    </div>

    <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
      <button
        type="button"
        class="auth-action-btn btn-signin-instead"
        :disabled="loading"
        :isScrim="true"
        @click="goToSignIn"
      >
        {{ t('auth.signin_instead') }}
      </button>
      <n-button
        type="primary"
        class="auth-action-btn btn-primary-action"
        :disabled="loading"
        :isScrim="true"
        @click="handleNext"
      >
        {{ t('auth.next') }}
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
import { useSignupFlowStore, useSigninFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'NameStepView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.signup_create_title'), () => t('auth.username_label'))

const router = useRouter()
const signupFlowStore = useSignupFlowStore()
const signinFlowStore = useSigninFlowStore()

const username = ref(signupFlowStore.username || '')
const errorKey = ref('')
const rawErrorMsg = ref('')

const errorMsg = computed(() => {
  if (errorKey.value) {
    return t(errorKey.value)
  }
  return rawErrorMsg.value
})

function clearError(): void {
  errorKey.value = ''
  rawErrorMsg.value = ''
}

const loading = computed(() => signupFlowStore.loading)

watch(username, () => {
  if (errorKey.value || rawErrorMsg.value) {
    clearError()
  }
})

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
    errorKey.value = 'auth.username_required'
    rawErrorMsg.value = ''
    return
  }
  if (trimmed.length < 3 || trimmed.length > 32) {
    errorKey.value = 'auth.username_length_error'
    rawErrorMsg.value = ''
    return
  }
  if (!/^[a-zA-Z0-9_-]+$/.test(trimmed)) {
    errorKey.value = 'auth.username_pattern_error'
    rawErrorMsg.value = ''
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
      rawErrorMsg.value = err.message
      errorKey.value = ''
    } else {
      errorKey.value = 'auth.username_verify_failed'
      rawErrorMsg.value = ''
    }
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

.btn-signin-instead {
  color: #0b57d0;
  background-color: transparent;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  user-select: none;

  &:hover:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }

  &:active:not(:disabled) {
    background-color: rgba(11, 87, 208, 0.16) !important;
    color: #0b57d0 !important;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

:global(.dark) .btn-signin-instead {
  color: #8ab4f8;

  &:hover:not(:disabled) {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:active:not(:disabled) {
    background-color: rgba(138, 180, 248, 0.2) !important;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
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
