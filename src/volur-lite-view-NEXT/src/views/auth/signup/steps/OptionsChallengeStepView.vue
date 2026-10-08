<template>
  <div class="flex flex-col justify-between h-full">
    <div>
      <p class="text-base text-google-text-primary dark:text-google-text-darkPrimary mb-5 font-normal leading-6">
        {{ t('auth.signup_options_prompt') }}
      </p>

      <div class="auth-selection-list">
        <div
          v-for="opt in options"
          :key="opt.type"
          class="auth-selection-item"
          :isScrim="true"
          role="button"
          tabindex="0"
          @click="handleSelectOption(opt)"
          @keydown.enter.space.prevent="handleSelectOption(opt)"
        >
          <div class="auth-selection-item__left">
            <div class="auth-selection-item__icon">
              <svg
                v-if="opt.type === 'passkey'"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <circle cx="7.5" cy="15.5" r="3.5" />
                <path d="M10 13l9-9" />
                <path d="M16 4l3 3" />
                <path d="M14 6l2 2" />
              </svg>
              <svg
                v-else
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                <polyline points="9 12 11 14 15 10" />
              </svg>
            </div>
            <div class="auth-selection-item__content">
              <div class="auth-selection-item__title">
                {{ opt.type === 'passkey' ? t('auth.passkey_verify_title') : t('auth.totp_protect_title') }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
      <n-button
        quaternary
        type="primary"
        :round="true"
        class="auth-action-btn btn-secondary-action"
        :disabled="loading"
        :isScrim="true"
        @click="handleSkip"
      >
        {{ t('auth.later') }}
      </n-button>
      <n-button
        type="primary"
        class="auth-action-btn btn-primary-action"
        :disabled="loading"
        :isScrim="true"
        @click="handleContinue"
      >
        {{ t('common.next') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useSignupFlowStore } from '@/store/modules/flow'
import { fetchSignUpOptionsApi } from '@/api/auth'
import type { SignUpOptionVO } from '@/types/auth/flow'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'OptionsChallengeStepView'
})

const { t } = useI18n()
useCardHeader(() => t('auth.security_verification'), () => t('auth.signup_options_prompt'))

const router = useRouter()
const signupFlowStore = useSignupFlowStore()
const loading = computed(() => signupFlowStore.loading)

const options = ref<SignUpOptionVO[]>([
  {
    type: 'passkey',
    title: '',
    icon: 'key',
    available: true
  },
  {
    type: 'totp',
    title: '',
    icon: 'shield',
    available: true
  }
])

onMounted(async () => {
  if (signupFlowStore.dsh && signupFlowStore.tl) {
    try {
      const res = await fetchSignUpOptionsApi({
        dsh: signupFlowStore.dsh,
        tl: signupFlowStore.tl
      })
      if (res.data && res.data.length > 0) {
        options.value = res.data
      }
    } catch {
    }
  }
})

async function handleSelectOption(opt: SignUpOptionVO) {
  if (signupFlowStore.loading) return
  const targetPath = opt.type === 'passkey' ? '/v1/signup/steps/pt/pk' : '/v1/signup/steps/pt/totp'
  await signupFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: targetPath,
    query: signupFlowStore.getQueryParams()
  })
}

async function handleSkip() {
  if (signupFlowStore.loading) return
  const redirect = signupFlowStore.continueUrl || '/competitions'
  await signupFlowStore.triggerScrim(undefined, 1000)
  signupFlowStore.resetFlow()
  await router.push(redirect)
}

async function handleContinue() {
  if (signupFlowStore.loading) return
  const redirect = signupFlowStore.continueUrl || '/competitions'
  await signupFlowStore.triggerScrim(undefined, 1000)
  signupFlowStore.resetFlow()
  await router.push(redirect)
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
  }

  &:disabled,
  &.n-button--disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

:global(html.dark) .btn-secondary-action {
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

  &:active:not(:disabled) {
    background-color: rgba(138, 180, 248, 0.2) !important;
  }

  &:disabled,
  &.n-button--disabled {
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

:global(html.dark) .btn-primary-action {
  background-color: #8ab4f8 !important;
  color: #202124 !important;

  :deep(.n-button__content),
  .n-button__content {
    color: #202124 !important;
  }

  &:hover {
    background-color: #aecbfa !important;
    color: #202124 !important;

    :deep(.n-button__content),
    .n-button__content {
      color: #202124 !important;
    }
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
