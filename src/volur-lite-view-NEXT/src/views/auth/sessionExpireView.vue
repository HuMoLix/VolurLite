<template>
  <div class="auth-card__body">
    <div class="auth-card__left">
      <h1 class="auth-card__title">会话已超时</h1>
      <p class="auth-card__subtitle">由于长时间未进行任何操作，此会话已结束</p>
    </div>

    <div class="auth-card__right">
      <div class="auth-form-content w-full h-full flex flex-col justify-between">
        <div class="flex flex-col text-left py-4">
          <p class="text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed max-w-sm mb-6">
            为了保护您的账户信息，未完成的流程已自动失效。请重新开始。
          </p>
        </div>

        <div class="flex items-center justify-end pt-6 sm:pt-8">
          <n-button
            type="primary"
            class="auth-action-btn btn-primary-action btn-next-step"
            :disabled="loading"
            :isScrim="true"
            @click="handleRetry"
          >
            重试
          </n-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'SessionExpireView'
})

import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useSigninFlowStore, useSignupFlowStore } from '@/store/modules/flow'

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()
const signupFlowStore = useSignupFlowStore()

const activeFlowStore = computed(() => {
  const followup = (route.query.followup as string) || ''
  const cont = (route.query.continue as string) || ''
  if (followup.includes('signup') || cont.includes('signup')) {
    return signupFlowStore
  }
  return signinFlowStore
})

const loading = computed(() => activeFlowStore.value.loading)

onMounted(() => {
  if (!route.query.continue || !route.query.followup || route.query.followup === '/') {
    const cont = (route.query.continue as string) || (route.query.redirect as string) || '/competitions'
    const follow =
      route.query.followup && route.query.followup !== '/'
        ? (route.query.followup as string)
        : cont.includes('signup')
          ? '/v1/signup'
          : '/v1/signin/identifier'
    router.replace({
      path: route.path,
      query: {
        ...route.query,
        continue: cont,
        followup: follow
      }
    })
  }
})

function resolveTargetDestination(): { path: string; query?: Record<string, string> } {
  const followup = (route.query.followup as string) || ''
  const cont = (route.query.continue as string) || ''

  if (followup) {
    if (followup.startsWith('/v1/signup') || followup === 'signup') {
      return { path: '/v1/signup', query: cont ? { continue: cont } : undefined }
    }
    if (followup.startsWith('/v1/signin') || followup === 'signin') {
      return { path: '/v1/signin/identifier', query: cont ? { continue: cont } : undefined }
    }
    if (followup.startsWith('/') && followup !== '/') {
      return { path: followup, query: cont ? { continue: cont } : undefined }
    }
  }

  if (cont) {
    if (cont.startsWith('/v1/signup') || cont === 'signup') {
      return { path: '/v1/signup' }
    }
    if (cont.startsWith('/v1/signin') || cont === 'signin') {
      return { path: '/v1/signin/identifier' }
    }
  }

  return { path: '/v1/signin/identifier', query: cont && cont !== '/' ? { continue: cont } : undefined }
}

async function handleRetry() {
  if (loading.value) return
  const target = resolveTargetDestination()
  await activeFlowStore.value.triggerScrim(undefined, 1000)
  signinFlowStore.resetFlow()
  signupFlowStore.resetFlow()
  await router.push(target)
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
