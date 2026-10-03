<template>
  <form class="space-y-5 flex flex-col justify-between h-full" @submit.prevent="handleNext">
    <div class="space-y-3">
      <div class="space-y-3">
        <GoogleTextField
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          label="输入您的密码"
          autocomplete="new-password"
          autofocus
          :error-msg="errors.password"
          @clear="errors.password = ''"
        />

        <GoogleTextField
          v-model="confirmPassword"
          :type="showPassword ? 'text' : 'password'"
          label="再次确认密码"
          autocomplete="new-password"
          :error-msg="errors.confirmPassword"
          @clear="errors.confirmPassword = ''"
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
          <span>显示密码</span>
        </label>
      </div>

      <p class="text-xs text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
        使用 8 个或以上字符，且混合包含字母、数字和符号以提高安全性。
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
        完成注册
      </n-button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useSignupFlowStore } from '@/store/modules/flow'
import { useCardHeader } from '@/composables/useCardHeader'

defineOptions({
  name: 'PasswordStepView'
})

useCardHeader('创建安全密码', '输入安全密码以完成账号创建')

const router = useRouter()
const signupFlowStore = useSignupFlowStore()

const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const loading = computed(() => signupFlowStore.loading)

const errors = reactive({
  password: '',
  confirmPassword: ''
})

watch(password, () => {
  if (errors.password) errors.password = ''
})

watch(confirmPassword, () => {
  if (errors.confirmPassword) errors.confirmPassword = ''
})

async function handleNext() {
  if (signupFlowStore.loading) return

  errors.password = ''
  errors.confirmPassword = ''
  let hasError = false

  if (!password.value) {
    errors.password = '请输入密码'
    hasError = true
  } else if (password.value.length < 8) {
    errors.password = '密码长度至少需为 8 位'
    hasError = true
  }

  if (!confirmPassword.value) {
    errors.confirmPassword = '请确认密码'
    hasError = true
  } else if (password.value !== confirmPassword.value) {
    errors.confirmPassword = '两次输入的密码不一致'
    hasError = true
  }

  if (hasError) return

  try {
    const { nextStep, status } = await signupFlowStore.submitSignUpPassword(password.value, confirmPassword.value)
    if (status === 'COMPLETE') {
      window.$message?.success?.('账号创建成功，欢迎加入 Volur！')
    }
    router.push({
      path: `/v1/signup/steps/${nextStep}`,
      query: signupFlowStore.getQueryParams()
    })
  } catch (err: any) {
    window.$message?.error?.(err?.message || '设置密码失败，请重试')
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
