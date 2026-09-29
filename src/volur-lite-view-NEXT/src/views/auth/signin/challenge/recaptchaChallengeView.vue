<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleVerifyChallenge">
    <div class="space-y-5">
      <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
        为了保护您的账号不受自动化程序攻击，请勾选下方验证框完成人机身份验证。
      </p>

      <Recaptcha
        v-model:passed="isRecaptchaPassed"
        v-model:token="recaptchaToken"
        v-model:ticket="recaptchaTicket"
        :dsh="authFlowStore.dsh"
        :tl="authFlowStore.tl"
        v-model:error-msg="recaptchaError"
        @success="handleRecaptchaSuccess"
        @error="handleRecaptchaError"
      />
    </div>

    <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
      <button
        type="button"
        class="auth-action-btn btn-try-another-way"
        :isScrim="true"
        @click="handleTryAnotherWayClick"
      >
        试试其他方式
      </button>
      <n-button
        type="primary"
        class="auth-action-btn btn-verify-action"
        :disabled="loading"
        @click="handleVerifyChallenge"
      >
        下一步
      </n-button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import Recaptcha from '@/components/security/recaptcha.vue'
import { useAuthFlowStore } from '@/store/modules/authFlow'

defineOptions({
  name: 'RecaptchaChallengeView'
})

const router = useRouter()
const authFlowStore = useAuthFlowStore()

const isRecaptchaPassed = ref(false)
const recaptchaToken = ref('')
const recaptchaTicket = ref('')
const recaptchaError = ref('')
const isSubmitting = ref(false)
const loading = computed(() => authFlowStore.loading)

watch(isRecaptchaPassed, (val) => {
  if (val && recaptchaError.value) {
    recaptchaError.value = ''
  }
})

function handleRecaptchaSuccess(payload: { token: string; ticket: string; metrics?: Record<string, unknown> }): void {
  recaptchaError.value = ''
  recaptchaToken.value = payload.token
  recaptchaTicket.value = payload.ticket || payload.token
}

function handleRecaptchaError(err: string): void {
  recaptchaError.value = err || '人机验证失败，请重试'
}

async function handleTryAnotherWayClick() {
  if (authFlowStore.loading) return
  await authFlowStore.triggerScrim(async () => {
    await new Promise(resolve => setTimeout(resolve, 800))
    const msg = 'Volur 无法验证此账号归您所有。请稍后重试，或前往“账号恢复”页面寻求帮助。'
    await router.push({
      path: '/v1/signin/challenge/rejected',
      query: authFlowStore.getQueryParams({
        rejectedmsg: encodeURIComponent(msg)
      })
    })
  }, 1000)
}

async function handleVerifyChallenge() {
  if (authFlowStore.loading || isSubmitting.value) return
  isSubmitting.value = true

  try {
    if (!isRecaptchaPassed.value || !recaptchaTicket.value) {
      recaptchaError.value = '请完成人机验证框以继续'
      return
    }

    try {
      const res = await authFlowStore.submitChallenge('recaptcha', {
        captchaTicket: recaptchaTicket.value,
        captchaCode: recaptchaTicket.value
      })

      if (res.nextStep === 'identifier') {
        const errorText = res.errorMessage || '用户名或邮箱不存在'
        authFlowStore.setAccountError(errorText)
        router.replace({
          path: '/v1/signin/identifier',
          query: {
            ...authFlowStore.getQueryParams(),
            accountError: errorText
          }
        })
        return
      }

      const nextStep = res.nextStep || 'pwd'
      router.push({
        path: `/v1/signin/challenge/${nextStep}`,
        query: authFlowStore.getQueryParams()
      })
    } catch (err: any) {
      const msg = err?.message || ''
      if (msg.includes('用户名或邮箱不存在') || msg.includes('不存在')) {
        const errorText = msg || '用户名或邮箱不存在'
        authFlowStore.setAccountError(errorText)
        router.replace({
          path: '/v1/signin/identifier',
          query: {
            ...authFlowStore.getQueryParams(),
            accountError: errorText
          }
        })
        return
      }
      recaptchaError.value = msg || '人机验证失败，请重试'
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

.btn-try-another-way {
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

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
  }
}

:global(html.dark) .btn-try-another-way {
  color: #8ab4f8;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:active {
    background-color: rgba(138, 180, 248, 0.2) !important;
  }
}

.btn-verify-action {
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

:global(html.dark) .btn-verify-action {
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
