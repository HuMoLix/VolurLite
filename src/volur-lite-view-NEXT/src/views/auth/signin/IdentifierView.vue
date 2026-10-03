<template>
  <div class="auth-card__body">
    <div class="auth-card__left">
      <h1 class="auth-card__title">登录</h1>
      <p class="auth-card__subtitle">使用您的 Volur 账号</p>
      <div id="auth-header-extra" class="auth-card__header-extra" />
    </div>

    <div class="auth-card__right">
      <div class="auth-form-content w-full h-full flex flex-col justify-between">
        <div class="flex flex-col">
          <div>
            <GoogleTextField
              v-model="identifier"
              label="邮箱或账户名称"
              autocomplete="username"
              autofocus
              :error-msg="errorMsg"
              @clear="errorMsg = ''"
              @enter="handleNext"
            />
          </div>

          <div class="text-left mt-2.5 mb-8">
            <a
              href="#"
              class="auth-forgot-link"
              @click.prevent="handleForgotAccount"
            >
              忘记了邮箱？
            </a>
          </div>

          <p class="text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
            您用的不是自己的电脑？请使用访客模式无痕登录。<a
              href="#"
              class="auth-learn-more-link"
            >了解详情</a>
          </p>
        </div>

        <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
          <button
            type="button"
            class="auth-action-btn btn-create-account"
            :disabled="loading"
            :isScrim="true"
            @click="handleCreateAccount"
          >
            创建账号
          </button>
          <n-button
            type="primary"
            class="auth-action-btn btn-next-step"
            :disabled="loading"
            @click="handleNext"
          >
            下一步
          </n-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSigninFlowStore } from '@/store/modules/flow'

defineOptions({
  name: 'IdentifierView'
})

const route = useRoute()
const router = useRouter()
const signinFlowStore = useSigninFlowStore()

const identifier = ref(signinFlowStore.identifier || '')
const errorMsg = ref('')
const loading = computed(() => signinFlowStore.loading)
let hasMounted = false

watch(identifier, () => {
  if (hasMounted && errorMsg.value) {
    errorMsg.value = ''
  }
})

function checkPendingError(): void {
  const fromStore = signinFlowStore.consumeAccountError()
  const fromQuery = (route.query.accountError as string) || ''
  const pending = fromStore || fromQuery
  if (pending) {
    errorMsg.value = pending
    if (fromQuery) {
      const nextQuery = { ...route.query }
      delete nextQuery.accountError
      router.replace({ path: route.path, query: nextQuery })
    }
  }
}

onMounted(() => {
  signinFlowStore.ensureFlowParams(route, router)
  if (signinFlowStore.identifier && !identifier.value) {
    identifier.value = signinFlowStore.identifier
  }
  checkPendingError()
  setTimeout(() => {
    hasMounted = true
  }, 100)
})

watch(
  () => route.query.accountError,
  (newVal) => {
    if (newVal && typeof newVal === 'string') {
      checkPendingError()
    }
  }
)

watch(
  () => signinFlowStore.accountError,
  (val) => {
    if (val) {
      checkPendingError()
    }
  }
)

async function handleNext() {
  if (signinFlowStore.loading) return

  const trimmed = identifier.value.trim()
  if (!trimmed) {
    errorMsg.value = '请输入邮箱或账户名称'
    return
  }

  try {
    const nextStep = await signinFlowStore.submitIdentifier(trimmed)
    await router.push({
      path: `/v1/signin/challenge/${nextStep}`,
      query: signinFlowStore.getQueryParams()
    })
  } catch (err: any) {
    errorMsg.value = err?.message || '账号核验失败，请重试'
  }
}

function handleForgotAccount() {
  if (signinFlowStore.loading) return
  window.$message.info('跳转至账号找回 /v1/recovery/identifier')
}

async function handleCreateAccount() {
  if (signinFlowStore.loading) return
  await signinFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signup/steps/name',
    query: {
      continue: signinFlowStore.continueUrl || '/competitions',
      followup: '/v1/signup',
      flowName: 'VlifWebSignUp',
      flowEntry: 'WebSignUp'
    }
  })
}
</script>

<style scoped lang="scss">
.auth-forgot-link {
  display: inline-flex;
  align-items: center;
  padding: 1px 2px !important;
  border-radius: 4px;
  background-color: transparent !important;
  color: #0b57d0 !important;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    text-decoration: none !important;
    color: #0b57d0 !important;
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
  }
}

:global(.dark) .auth-forgot-link {
  background-color: transparent !important;
  color: #8ab4f8 !important;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }
}

.auth-learn-more-link {
  color: #0b57d0 !important;
  font-weight: 500;
  text-decoration: none;
  cursor: pointer;

  &:hover {
    text-decoration: underline !important;
    color: #0b57d0 !important;
  }
}

:global(.dark) .auth-learn-more-link {
  color: #8ab4f8 !important;

  &:hover {
    color: #8ab4f8 !important;
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

.btn-create-account {
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

:global(.dark) .btn-create-account {
  color: #8ab4f8;

  &:hover:not(:disabled) {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.btn-next-step {
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

:global(.dark) .btn-next-step {
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
