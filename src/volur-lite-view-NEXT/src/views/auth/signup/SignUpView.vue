<template>
  <div class="auth-card__body">
    <!-- 左侧区域：大标题与副标题 -->
    <div class="auth-card__left">
      <h1 class="auth-card__title">创建 Volur 账号</h1>
      <p class="auth-card__subtitle">输入您的基本信息以开启网络安全演练之旅</p>
      <!-- 预留扩展区域 -->
      <div id="auth-header-extra" class="auth-card__header-extra" />
    </div>

    <!-- 右侧区域：注册表单与操作按钮 -->
    <div class="auth-card__right">
      <div class="auth-form-content w-full h-full flex flex-col justify-between">
        <!-- 注册表单输入区域 -->
        <form class="space-y-4" @submit.prevent="handleSignUp">
      <div>
        <GoogleTextField
          v-model="username"
          label="用户名"
          autocomplete="username"
          autofocus
          :error-msg="errors.username"
          @clear="errors.username = ''"
        />
      </div>

      <div>
        <GoogleTextField
          v-model="email"
          type="email"
          label="电子邮件地址"
          autocomplete="email"
          :error-msg="errors.email"
          @clear="errors.email = ''"
        />
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <GoogleTextField
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          label="密码"
          autocomplete="new-password"
          :error-msg="errors.password"
          @clear="errors.password = ''"
        />
        <GoogleTextField
          v-model="confirmPassword"
          :type="showPassword ? 'text' : 'password'"
          label="确认密码"
          autocomplete="new-password"
          :error-msg="errors.confirmPassword"
          @clear="errors.confirmPassword = ''"
        />
      </div>

      <!-- Google Accounts 样式的“显示密码”复选框 -->
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

      <!-- 底部动作栏 (严格对齐 Google Accounts Glif 交互与动效) -->
      <div class="flex items-center justify-between pt-6 sm:pt-8">
        <button
          type="button"
          class="auth-action-btn btn-signin-instead"
          @click="goToSignIn"
        >
          改用现有账号登录
        </button>
        <n-button
          type="primary"
          class="auth-action-btn btn-register-action"
          :disabled="loading"
          @click="handleSignUp"
        >
          注册
        </n-button>
      </div>
    </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useAuthFlowStore } from '@/store/modules/authFlow'

const route = useRoute()
const router = useRouter()
const authFlowStore = useAuthFlowStore()

const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const loading = computed(() => authFlowStore.loading)

const errors = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

onMounted(() => {
  authFlowStore.ensureFlowParams(route, router)
})

function goToSignIn() {
  if (authFlowStore.loading) return
  authFlowStore.setFlow('VlifWebLogin', 'WebServiceLogin')
  router.push({
    path: '/v1/signin/identifier',
    query: authFlowStore.getQueryParams({
      flowName: 'VlifWebLogin',
      flowEntry: 'WebServiceLogin'
    })
  })
}

function handleSignUp() {
  if (authFlowStore.loading) return

  // 重置历史错误提示
  errors.username = ''
  errors.email = ''
  errors.password = ''
  errors.confirmPassword = ''

  let hasError = false

  if (!username.value.trim()) {
    errors.username = '请输入用户名'
    hasError = true
  }

  if (!email.value.trim()) {
    errors.email = '请输入电子邮件地址'
    hasError = true
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    errors.email = '请输入有效的电子邮件格式'
    hasError = true
  }

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

  if (hasError) {
    window.$message.error('请修正表单中的错误项')
    return
  }

  authFlowStore.setLoading(true)
  setTimeout(() => {
    authFlowStore.setLoading(false)
    window.$message.success('账号创建成功，正在前往登录...')
    authFlowStore.setIdentifier(email.value)
    router.push({
      path: '/v1/signin/identifier',
      query: authFlowStore.getQueryParams()
    })
  }, 400)
}
</script>

<style scoped lang="scss">
// 统一紧凑动作按钮规格 (高 36px，全药丸 rounded-full 9999px，严格对齐 Google M3)
.auth-action-btn {
  height: 36px !important;
  min-width: 88px !important;
  padding: 0 16px !important;
  border-radius: 9999px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  box-sizing: border-box;
}

// 文本按钮：“改用现有账号登录” (Google M3 Text Button)
// 正常状态为透明底色与蓝色文字，hover 时展示透明状态层 rgba(11, 87, 208, 0.08)
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

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
    color: #0b57d0 !important;
  }
}

:global(.dark) .btn-signin-instead {
  color: #8ab4f8;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:active {
    background-color: rgba(138, 180, 248, 0.2) !important;
  }
}

// 实心主按钮：“注册” (Google M3 Filled Button)
// 实心蓝底白字，hover 时适度加深并伴随轻微阴影，点击 active 时伴随 scale(0.98) 物理微缩放动效
.btn-register-action {
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
    transform: scale(0.98); // 关键动效：点击微缩放触感反馈
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

:global(.dark) .btn-register-action {
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
