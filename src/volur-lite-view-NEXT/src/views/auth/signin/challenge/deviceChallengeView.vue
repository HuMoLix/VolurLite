<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleVerifyChallenge">
    <div class="space-y-4">
      <div class="auth-challenge-banner flex items-start gap-3 p-3.5 rounded-lg border border-google-border-light dark:border-google-border-dark bg-google-surface-light dark:bg-google-surface-darkElevated">
        <svg class="w-5 h-5 text-google-yellow dark:text-google-yellow-dark shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
        </svg>
        <div class="text-xs text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
          <p class="font-medium text-google-text-primary dark:text-google-text-darkPrimary mb-0.5">
            检测到新设备或非典型网络环境
          </p>
          请确认本次登录是否由您本人发起。确认后将引导前往输入密码。
        </div>
      </div>

      <div class="p-4 rounded-lg border border-google-border-light dark:border-google-border-dark space-y-2 text-xs">
        <div class="flex justify-between">
          <span class="text-google-text-muted">尝试登录时间</span>
          <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">{{ currentTimeFormatted }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-google-text-muted">登录客户端特征</span>
          <span class="font-medium text-google-text-primary dark:text-google-text-darkPrimary">Chrome / Web Client</span>
        </div>
        <div class="flex justify-between">
          <span class="text-google-text-muted">会话状态句柄 (DSH)</span>
          <span class="font-mono text-google-text-sub truncate max-w-[200px]">{{ authFlowStore.dsh }}</span>
        </div>
      </div>

      <div v-if="deviceError" class="flex items-center gap-1.5 text-xs text-google-red dark:text-google-red-dark">
        <svg viewBox="0 0 24 24" class="w-3.5 h-3.5 fill-current shrink-0">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
        </svg>
        <span>{{ deviceError }}</span>
      </div>
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
        验证
      </n-button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useAuthFlowStore } from '@/store/modules/authFlow'

defineOptions({
  name: 'DeviceChallengeView'
})

const router = useRouter()
const authFlowStore = useAuthFlowStore()

const deviceError = ref('')
const isSubmitting = ref(false)
const loading = computed(() => authFlowStore.loading)

const currentTimeFormatted = computed(() => {
  return new Date().toLocaleTimeString('zh-CN', { hour12: false })
})

async function handleTryAnotherWayClick() {
  if (authFlowStore.loading) return
  await authFlowStore.triggerScrim(async () => {
    await new Promise(resolve => setTimeout(resolve, 800))
    await router.push({
      path: '/v1/signin/challenge/selection',
      query: authFlowStore.getQueryParams({
        from: 'device',
        page: '1'
      })
    })
  }, 1000)
}

async function handleVerifyChallenge() {
  if (authFlowStore.loading || isSubmitting.value) return
  isSubmitting.value = true

  try {
    try {
      const res = await authFlowStore.submitChallenge('device', {})
      const nextStep = res.nextStep || 'pwd'
      router.push({
        path: `/v1/signin/challenge/${nextStep}`,
        query: authFlowStore.getQueryParams()
      })
    } catch (err: any) {
      deviceError.value = err?.message || '设备核验失败'
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
