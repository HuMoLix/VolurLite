<template>
  <div class="auth-form">
    <div class="auth-form__header">
      <h1 class="auth-form__title">
        {{ isLogin ? t('auth.welcome_title') : (isForgot ? t('auth.reset_password_title') : t('auth.create_account_title')) }}
      </h1>
      <p class="auth-form__subtitle">
        {{ isLogin ? t('auth.welcome_subtitle') : (isForgot ? t('auth.reset_password_subtitle') : t('auth.create_account_subtitle')) }}
      </p>
    </div>

    <!-- Login Form -->
    <n-form
      v-if="isLogin"
      ref="formRef"
      :model="loginForm"
      :rules="loginRules"
      @keyup.enter="handleAction"
      :show-require-mark="false"
    >
      <n-form-item path="username" :label="t('auth.username')">
        <n-input
          v-model:value="loginForm.username"
          :placeholder="t('auth.enter_username')"
          size="large"
          clearable
        />
      </n-form-item>

      <n-form-item path="password" :label="t('auth.password')">
        <n-input
          v-model:value="loginForm.password"
          type="password"
          show-password-on="click"
          :placeholder="t('auth.enter_password')"
          size="large"
          clearable
        />
      </n-form-item>

      <div class="auth-form__forgot-password-link-wrapper">
        <a href="#" @click.prevent="setMode('forgot')">{{ t('auth.forgot_password') }}</a>
      </div>

      <n-form-item :label="t('auth.verification_code')" v-if="isCaptchaEnabled" :show-feedback="false">
        <VolurCaptcha
          ref="loginCaptchaRef"
          scene="LOGIN"
          :account="loginForm.username"
          @enter="handleAction"
        />
      </n-form-item>

      <n-button
        type="primary"
        block
        size="large"
        attr-type="button"
        :disabled="loading"
        :loading="loading"
        @click="handleAction"
        class="auth-form__submit-button"
      >
        {{ t('auth.continue') }}
      </n-button>
    </n-form>

    <!-- Forgot Password Form -->
    <n-form
      v-else-if="isForgot"
      ref="formRef"
      :model="forgotForm"
      :rules="forgotRules"
      @keyup.enter="handleForgotPasswordAction"
      :show-require-mark="false"
    >
      <n-form-item path="username" :label="t('auth.username')">
        <n-input
          v-model:value="forgotForm.username"
          :placeholder="t('auth.enter_username')"
          size="large"
          clearable
        />
      </n-form-item>

      <n-form-item path="email" :label="t('auth.email_address')">
        <n-input
          v-model:value="forgotForm.email"
          :placeholder="t('auth.email_placeholder')"
          size="large"
          clearable
        />
      </n-form-item>

      <n-button
        type="primary"
        block
        size="large"
        attr-type="button"
        :disabled="loading"
        :loading="loading"
        @click="handleForgotPasswordAction"
        class="auth-form__submit-button"
      >
        {{ t('auth.send_reset_link') }}
      </n-button>

      <div class="auth-form__footer">
        <n-text depth="3">
          {{ t('auth.remember_password') }}
          <a href="#" @click.prevent="setMode('login')" class="auth-form__toggle-link">
            {{ t('auth.log_in') }}
          </a>
        </n-text>
      </div>
    </n-form>

    <!-- Register Form -->
    <n-form
      v-else
      ref="formRef"
      :model="registerForm"
      :rules="registerRules"
      @keyup.enter="handleAction"
      :show-require-mark="false"
    >
      <n-form-item path="username" :label="t('auth.username')">
        <n-input
          v-model:value="registerForm.username"
          :placeholder="t('auth.choose_username')"
          size="large"
          clearable
        />
      </n-form-item>

      <n-form-item path="password" :label="t('auth.password')">
        <n-input
          v-model:value="registerForm.password"
          type="password"
          show-password-on="click"
          :placeholder="t('auth.create_strong_password')"
          size="large"
          clearable
        />
      </n-form-item>

      <n-form-item path="email" :label="t('auth.email_address')">
        <n-input
          v-model:value="registerForm.email"
          :placeholder="t('auth.email_placeholder')"
          size="large"
          clearable
        />
      </n-form-item>

      <n-form-item v-if="isEmailVerificationRequired" path="emailCaptcha" :label="t('auth.email_verification')">
        <div class="auth-form__email-code-wrapper">
          <n-input
            v-model:value="registerForm.emailCaptcha"
            :placeholder="t('auth.six_digit_code')"
            size="large"
            style="flex: 1;"
          />
          <n-button
            size="large"
            :disabled="countdown > 0 || !registerForm.email"
            @click="sendEmailVerificationCode"
            class="auth-form__send-code-button"
          >
            {{ countdown > 0 ? `${countdown}s` : t('auth.send_code') }}
          </n-button>
        </div>
      </n-form-item>

      <n-button
        type="primary"
        block
        size="large"
        attr-type="button"
        :disabled="loading"
        :loading="loading"
        @click="handleAction"
        class="auth-form__submit-button"
      >
        {{ t('auth.sign_up') }}
      </n-button>
    </n-form>

    <!-- Bottom Mode Toggle Footer -->
    <div class="auth-form__footer" v-if="!isForgot">
      <n-text depth="3">
        <template v-if="isLogin">
          <template v-if="isRegisterEnabled">
            {{ t('auth.no_account_question') }}
            <a href="#" @click.prevent="toggleMode" class="auth-form__toggle-link">
              {{ t('auth.sign_up_link') }}
            </a>
          </template>
        </template>
        <template v-else>
          {{ t('auth.has_account_question') }}
          <a href="#" @click.prevent="toggleMode" class="auth-form__toggle-link">
            {{ t('auth.log_in_link') }}
          </a>
        </template>
      </n-text>
    </div>

    <!-- 全局行为人机验证协调组件 (注册 / 找回密码场景) -->
    <VolurCaptcha
      ref="behaviorCaptchaRef"
      headless
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import VolurCaptcha from '@/public/volurEx/components/captcha/VolurCaptcha.vue'
import type { CaptchaVerifyRequestDTO } from '@/public/volurEx/components/captcha'
import {
  NForm,
  NFormItem,
  NInput,
  NButton,
  NText,
  useMessage,
  type FormInst,
  type FormRules,
  type FormItemRule
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/modules/user'
import { useSystemStore } from '@/store/modules/system'
import { registerApi, forgotPasswordApi } from '@/api/auth'
import { sendRegisterEmailCodeApi } from '@/api/verification'

type AuthMode = 'login' | 'register' | 'forgot'

interface Props {
  initialMode?: AuthMode
}

const props = withDefaults(defineProps<Props>(), {})

const emit = defineEmits<{
  (e: 'success', type: AuthMode): void
  (e: 'mode-change', mode: AuthMode): void
}>()

const mode = defineModel<AuthMode>('mode', { default: 'login' })

onMounted(() => {
  if (props.initialMode && props.initialMode !== mode.value) {
    mode.value = props.initialMode
  }
})

const { t } = useI18n()
const userStore = useUserStore()
const systemStore = useSystemStore()
const message = useMessage()

const formRef = ref<FormInst | null>(null)
const loading = ref<boolean>(false)
const countdown = ref<number>(0)
const loginCaptchaRef = ref<InstanceType<typeof VolurCaptcha> | null>(null)
const behaviorCaptchaRef = ref<InstanceType<typeof VolurCaptcha> | null>(null)
const captchaTicket = ref<string>('')

const isLogin = computed(() => mode.value === 'login')
const isForgot = computed(() => mode.value === 'forgot')
const isRegisterEnabled = computed(() => systemStore.isRegistrationEnabled)
const isEmailVerificationRequired = computed(() => systemStore.isEmailVerificationRequired)
const isCaptchaEnabled = computed(() => systemStore.isCaptchaEnabled)

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  email: '',
  emailCaptcha: ''
})

const forgotForm = reactive({
  username: '',
  email: ''
})

const loginRules = computed<FormRules>(() => ({
  username: [{ required: true, message: t('auth.username_required') }],
  password: [{ required: true, message: t('auth.password_required') }]
}))

const registerRules = computed<FormRules>(() => ({
  username: [
    { required: true, message: t('auth.username_required') },
    {
      validator(_rule: FormItemRule, value: string) {
        if (!value) return true
        if (value.length < 3 || value.length > 32) {
          return new Error(t('auth.username_length'))
        }
        if (!/^[a-zA-Z0-9_-]+$/.test(value)) {
          return new Error(t('auth.username_pattern'))
        }
        return true
      },
      trigger: ['blur', 'input']
    }
  ],
  password: [
    { required: true, message: t('auth.password_required') },
    {
      validator(_rule: FormItemRule, value: string) {
        if (!value) return true
        if (value.length < 6 || value.length > 32) {
          return new Error(t('auth.password_length'))
        }
        if (!/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\d@$!%*?&._#^&+=]{6,32}$/.test(value)) {
          return new Error(t('auth.password_pattern'))
        }
        return true
      },
      trigger: ['blur', 'input']
    }
  ],
  email: [
    { required: true, message: t('auth.email_required') },
    {
      validator(_rule: FormItemRule, value: string) {
        if (!value) return true
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        if (!emailRegex.test(value)) {
          return new Error(t('auth.invalid_email'))
        }
        return true
      },
      trigger: ['blur', 'input']
    }
  ],
  emailCaptcha: isEmailVerificationRequired.value
    ? [
        { required: true, message: t('auth.email_code_required') },
        {
          validator(_rule: FormItemRule, value: string) {
            if (!value) return true
            if (!/^\d{6}$/.test(value.trim())) {
              return new Error(t('auth.six_digit_code') || '请输入6位数字验证码')
            }
            return true
          },
          trigger: ['blur', 'input']
        }
      ]
    : []
}))

const forgotRules = computed<FormRules>(() => ({
  username: [{ required: true, message: t('auth.username_required') }],
  email: [
    { required: true, message: t('auth.email_required') },
    {
      validator(_rule: FormItemRule, value: string) {
        if (!value) return true
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        if (!emailRegex.test(value)) {
          return new Error(t('auth.invalid_email'))
        }
        return true
      },
      trigger: ['blur', 'input']
    }
  ]
}))

function setMode(newMode: AuthMode) {
  mode.value = newMode
  emit('mode-change', newMode)
}

function toggleMode() {
  const nextMode: AuthMode = isLogin.value ? 'register' : 'login'
  setMode(nextMode)
}

async function sendEmailVerificationCode() {
  if (!registerForm.email) {
    message.warning(t('auth.enter_email_first'))
    return
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(registerForm.email.trim())) {
    message.warning(t('auth.enter_valid_email'))
    return
  }

  if (countdown.value > 0) return

  let ticket: string | undefined
  if (isCaptchaEnabled.value) {
    try {
      const ticketVO = await behaviorCaptchaRef.value?.verifyBehavior('REGISTER')
      ticket = ticketVO?.ticket
      if (ticket) {
        captchaTicket.value = ticket
      }
    } catch {
      return
    }
  }

  await doSendEmailCode(ticket)
}

async function doSendEmailCode(ticket?: string) {
  try {
    const effectiveTicket = ticket || captchaTicket.value || undefined
    await sendRegisterEmailCodeApi({
      email: registerForm.email.trim(),
      username: registerForm.username ? registerForm.username.trim() : undefined,
      captcha: effectiveTicket ? {
        captchaTicket: effectiveTicket,
        scene: 'REGISTER',
        account: registerForm.email.trim()
      } : undefined
    })
    message.success(t('auth.verification_code_sent'))

    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch {
    captchaTicket.value = ''
  }
}

async function handleForgotPasswordAction() {
  if (loading.value) return
  loading.value = true
  try {
    const valid = await formRef.value?.validate().then(() => true).catch(() => false)
    if (!valid) {
      loading.value = false
      return
    }
    let ticket: string | undefined
    if (isCaptchaEnabled.value) {
      try {
        const ticketVO = await behaviorCaptchaRef.value?.verifyBehavior('RESET_PASSWORD')
        ticket = ticketVO?.ticket
        if (ticket) {
          captchaTicket.value = ticket
        }
      } catch {
        loading.value = false
        return
      }
    }
    await doSendForgotPassword(ticket)
  } catch {
    loading.value = false
  }
}

async function doSendForgotPassword(ticket?: string) {
  loading.value = true
  try {
    const effectiveTicket = ticket || captchaTicket.value || undefined
    await forgotPasswordApi({
      username: forgotForm.username.trim(),
      email: forgotForm.email.trim(),
      captcha: effectiveTicket ? {
        captchaTicket: effectiveTicket,
        scene: 'RESET_PASSWORD',
        account: forgotForm.username.trim()
      } : undefined
    })
    message.success(t('auth.reset_link_sent'))
    emit('success', 'forgot')
    setMode('login')
  } catch {
    captchaTicket.value = ''
  } finally {
    loading.value = false
  }
}

async function handleAction() {
  if (loading.value) return
  loading.value = true

  try {
    const valid = await formRef.value?.validate().then(() => true).catch(() => false)
    if (!valid) {
      loading.value = false
      return
    }

    if (isLogin.value) {
      try {
        let captchaPayload: CaptchaVerifyRequestDTO | undefined
        if (isCaptchaEnabled.value && loginCaptchaRef.value) {
          captchaPayload = await loginCaptchaRef.value.getPayload()
        }

        await userStore.login({
          username: loginForm.username,
          password: loginForm.password,
          captcha: captchaPayload
        })
        message.success(t('auth.welcome_back'))
        emit('success', 'login')
      } catch (error: any) {
        const errorMsg = error?.message || ''
        if (!errorMsg.includes('cancelled') && !errorMsg.includes('取消')) {
          if (loginCaptchaRef.value) {
            loginCaptchaRef.value.handleFailure(error)
          }
        }
      } finally {
        loading.value = false
      }
    } else {
      let ticket: string | undefined
      if (!isEmailVerificationRequired.value && isCaptchaEnabled.value) {
        try {
          const ticketVO = await behaviorCaptchaRef.value?.verifyBehavior('REGISTER')
          ticket = ticketVO?.ticket
          if (ticket) {
            captchaTicket.value = ticket
          }
        } catch {
          loading.value = false
          return
        }
      }
      await doRegister(ticket)
    }
  } catch {
    loading.value = false
  }
}

async function doRegister(ticket?: string) {
  loading.value = true
  try {
    const effectiveTicket = ticket || captchaTicket.value || undefined
    await registerApi({
      username: registerForm.username.trim(),
      password: registerForm.password,
      email: registerForm.email.trim(),
      emailCaptcha: isEmailVerificationRequired.value ? registerForm.emailCaptcha.trim() : undefined,
      captcha: effectiveTicket ? {
        captchaTicket: effectiveTicket,
        scene: 'REGISTER',
        account: registerForm.username.trim()
      } : undefined
    })
    message.success(t('auth.account_created_success'))
    emit('success', 'register')
    toggleMode()
  } catch {
    captchaTicket.value = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
$border-radius-base: 8px;
$text-main: #111;
$text-muted: #666;

.auth-form {
  width: 100%;
  padding: 8px 4px;

  &__header {
    margin-bottom: 28px;

    .auth-form__title {
      font-size: 24px;
      font-weight: 400;
      letter-spacing: -0.5px;
      margin: 0;
    }

    .auth-form__subtitle {
      font-size: 24px;
      font-weight: 400;
      color: #767676;
      margin-top: -12px;
    }
  }

  &__forgot-password-link-wrapper {
    text-align: right;
    margin: -20px 0 0 0;

    a {
      font-size: 13px;
      color: $text-muted;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  .auth-form__captcha-wrapper,
  .auth-form__email-code-wrapper {
    display: flex;
    gap: 12px;
    width: 100%;

    .auth-form__captcha-image {
      cursor: pointer;
      height: 40px;
      width: 110px;
      border-radius: 6px;
      border: 1px solid #efeff5;
      object-fit: cover;
      transition: opacity 0.2s;

      &:hover {
        opacity: 0.8;
      }
    }

    .auth-form__send-code-button {
      width: 110px;
    }
  }

  .auth-form__submit-button {
    margin-top: 20px;
    height: 48px;
    font-size: 16px;
    font-weight: 500;
  }

  .auth-form__footer {
    margin-top: 24px;
    text-align: center;
    font-size: 14px;

    .auth-form__toggle-link {
      color: $text-main;
      font-weight: 500;
      text-decoration: none;
      margin-left: 4px;
      transition: color 0.2s;

      &:hover {
        text-decoration: underline;
        color: #000;
      }
    }
  }

  :deep(.n-input) {
    border-radius: $border-radius-base;

    &:hover,
    &:focus-within {
      border-color: #bbb;
    }
  }

  :deep(.n-form-item-label) {
    font-weight: 500;
    color: #444;
    font-size: 13px;
    padding-bottom: 6px;
  }

  :deep(.n-button) {
    border-radius: $border-radius-base;
  }
}
</style>
