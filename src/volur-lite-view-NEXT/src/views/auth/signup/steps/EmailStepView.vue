<template>
  <form class="space-y-6 flex flex-col justify-between h-full" @submit.prevent="handleNext">
    <div class="space-y-3">
      <div>
        <GoogleTextField
          v-model="email"
          type="email"
          label="电子邮件地址"
          autocomplete="email"
          autofocus
          :error-msg="errorMsg"
          @clear="errorMsg = ''"
          @enter="handleNext"
        />
      </div>

      <p class="text-xs text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
        系统将向此邮箱发送包含 6 位一次性验证码的邮件以核实您的所有权。
      </p>
    </div>

    <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
      <button
        type="button"
        class="auth-action-btn btn-secondary-action"
        :disabled="loading"
        :isScrim="true"
        @click="goBackName"
      >
        返回上一步
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
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSignupFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'EmailStepView'
})

useCardHeader('添加电子邮件地址', '系统将向此邮箱发送一次性验证码以进行核验')

const router = useRouter()
const signupFlowStore = useSignupFlowStore()

const email = ref(signupFlowStore.email || '')
const errorMsg = ref('')
const loading = computed(() => signupFlowStore.loading)

watch(email, () => {
  if (errorMsg.value) {
    errorMsg.value = ''
  }
})

async function goBackName() {
  if (signupFlowStore.loading) return
  await signupFlowStore.triggerScrim(undefined, 1000)
  await router.push({
    path: '/v1/signup/steps/name',
    query: signupFlowStore.getQueryParams()
  })
}

async function handleNext() {
  if (signupFlowStore.loading) return
  const trimmed = email.value.trim()

  if (!trimmed) {
    errorMsg.value = '请输入电子邮件地址'
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
    errorMsg.value = '请输入有效的电子邮件格式'
    return
  }

  try {
    const nextStep = await signupFlowStore.submitSignUpEmail(trimmed)
    router.push({
      path: `/v1/signup/steps/${nextStep}`,
      query: signupFlowStore.getQueryParams()
    })
  } catch (err: any) {
    errorMsg.value = err?.message || '邮箱提交失败，请重试'
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
