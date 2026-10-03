<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleNext">
    <div class="space-y-4">
      <div>
        <GoogleTextField
          v-model="code"
          label="输入 6 位数字验证码"
          autocomplete="one-time-code"
          autofocus
          maxlength="6"
          :error-msg="errorMsg"
          @clear="errorMsg = ''"
          @enter="handleNext"
        />
      </div>

      <div class="flex items-center justify-between text-sm">
        <span class="text-google-text-muted dark:text-google-text-darkMuted">
          验证码已发送至 {{ displayEmail }}
        </span>
        <button
          type="button"
          class="auth-resend-link"
          :disabled="countdown > 0 || isResending"
          @click="handleResend"
        >
          <span v-if="countdown > 0">{{ countdown }} 秒后可重新发送</span>
          <span v-else-if="isResending">发送中...</span>
          <span v-else>重新发送验证码</span>
        </button>
      </div>
    </div>

    <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
      <button
        type="button"
        class="auth-action-btn btn-secondary-action"
        :disabled="loading"
        :isScrim="true"
        @click="goBackEmail"
      >
        返回修改邮箱
      </button>
      <n-button
        type="primary"
        class="auth-action-btn btn-primary-action"
        :disabled="loading"
        :isScrim="true"
        @click="handleNext"
      >
        下一步
      </n-button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSignupFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'EtotpChallengeStepView'
})

useCardHeader('验证电子邮件', '请输入发送到您邮箱的 6 位安全验证码')

const router = useRouter()
const signupFlowStore = useSignupFlowStore()

const code = ref('')
const errorMsg = ref('')
const countdown = ref(60)
const isResending = ref(false)
const loading = computed(() => signupFlowStore.loading)
let timer: ReturnType<typeof setInterval> | null = null

const displayEmail = computed(() => {
  return signupFlowStore.email || '您的邮箱'
})

watch(code, () => {
  if (errorMsg.value) {
    errorMsg.value = ''
  }
})

onMounted(() => {
  startCountdown()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function startCountdown() {
  countdown.value = 60
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value -= 1
    } else {
      if (timer) clearInterval(timer)
    }
  }, 1000)
}

async function handleResend() {
  if (countdown.value > 0 || isResending.value || signupFlowStore.loading) return
  isResending.value = true
  try {
    await signupFlowStore.resendSignUpEmail()
    window.$message?.success?.('验证码已重新发送')
    startCountdown()
  } catch (err: any) {
    window.$message?.error?.(err?.message || '重新发送失败，请稍后重试')
  } finally {
    isResending.value = false
  }
}

async function goBackEmail() {
  if (signupFlowStore.loading) return
  await signupFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signup/steps/email',
    query: signupFlowStore.getQueryParams()
  })
}

async function handleNext() {
  if (signupFlowStore.loading) return
  const trimmed = code.value.trim()

  if (!trimmed) {
    errorMsg.value = '请输入 6 位验证码'
    return
  }
  if (!/^\d{6}$/.test(trimmed)) {
    errorMsg.value = '验证码必须为 6 位纯数字'
    return
  }

  try {
    const nextStep = await signupFlowStore.submitSignUpEtotp(trimmed)
    router.push({
      path: `/v1/signup/steps/${nextStep}`,
      query: signupFlowStore.getQueryParams()
    })
  } catch (err: any) {
    errorMsg.value = err?.message || '验证码核验未通过，请重试'
  }
}
</script>

<style scoped lang="scss">
.auth-resend-link {
  background: none;
  border: none;
  color: #0b57d0;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  outline: none;
  transition: opacity 0.2s ease;

  &:hover:not(:disabled) {
    text-decoration: underline;
  }

  &:disabled {
    color: var(--v-text-muted, #5f6368);
    cursor: not-allowed;
    opacity: 0.8;
  }
}

:global(.dark) .auth-resend-link {
  color: #8ab4f8;

  &:disabled {
    color: var(--v-text-muted, #9aa0a6);
  }
}

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
    color: #0b57d0 !important;
  }
}

:global(.dark) .btn-secondary-action {
  color: #8ab4f8;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:active {
    background-color: rgba(138, 180, 248, 0.2) !important;
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
