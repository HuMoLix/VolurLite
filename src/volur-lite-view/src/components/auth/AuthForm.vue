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
        <a href="#" @click.prevent="mode = 'forgot'">{{ t('auth.forgot_password') }}</a>
      </div>

      <n-form-item path="captchaCode" :label="t('auth.verification_code')" v-if="isCaptchaEnabled">
        <div class="auth-form__captcha-wrapper">
          <n-input
            v-model:value="loginForm.captchaCode"
            :placeholder="t('auth.code')"
            size="large"
            style="flex: 1;"
          />
          <img
            v-if="captchaImage"
            :src="captchaImage"
            @click="fetchCaptcha"
            class="auth-form__captcha-image"
            :alt="t('auth.captcha_alt')"
            :title="t('auth.captcha_refresh_title')"
          />
          <n-button
            v-else
            size="large"
            @click="fetchCaptcha"
            class="auth-form__captcha-reload-button"
          >
            {{ t('auth.verification_code') }}
          </n-button>
        </div>
      </n-form-item>

      <n-button
        type="primary"
        block
        size="large"
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
        :loading="loading"
        @click="handleForgotPasswordAction"
        class="auth-form__submit-button"
      >
        {{ t('auth.send_reset_link') }}
      </n-button>

      <div class="auth-form__footer">
        <n-text depth="3">
          {{ t('auth.remember_password') }}
          <a href="#" @click.prevent="mode = 'login'" class="auth-form__toggle-link">
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

      <n-form-item path="emailCaptcha" :label="t('auth.email_verification')">
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

    <!-- Slide Puzzle Behavioral Verification Modal -->
    <SlidePuzzleModal
      v-model:show="showPuzzleModal"
      :scene="puzzleScene"
      @success="handlePuzzleSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import SlidePuzzleModal from '@/components/verification/SlidePuzzleModal.vue'
import type { CaptchaTicketVO } from '@/types/verification'
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
import { getCaptchaApi, sendRegisterEmailCodeApi } from '@/api/verification'

interface Props {
  initialMode?: 'login' | 'register' | 'forgot'
}

const props = withDefaults(defineProps<Props>(), {
  initialMode: 'login'
})

const emit = defineEmits<{
  (e: 'success', mode: 'login' | 'register'): void
  (e: 'mode-change', mode: string): void
}>()

const { t } = useI18n()
const userStore = useUserStore()
const systemStore = useSystemStore()
const message = useMessage()

const formRef = ref<FormInst | null>(null)
const mode = ref<'login' | 'register' | 'forgot'>(props.initialMode)
const loading = ref<boolean>(false)
const countdown = ref<number>(0)

const captchaImage = ref<string>('')
const captchaId = ref<string>('')
const captchaType = ref<string>('LINE')
const captchaTicket = ref<string>('')
const showPuzzleModal = ref<boolean>(false)
const puzzleScene = ref<string>('REGISTER')
const pendingAction = ref<'SEND_EMAIL_CODE' | 'REGISTER' | 'FORGOT_PASSWORD' | null>(null)

async function handlePuzzleSuccess(ticketVO: CaptchaTicketVO) {
  captchaTicket.value = ticketVO.ticket
  if (pendingAction.value === 'SEND_EMAIL_CODE') {
    await doSendEmailCode(ticketVO.ticket)
  } else if (pendingAction.value === 'REGISTER') {
    await doRegister(ticketVO.ticket)
  } else if (pendingAction.value === 'FORGOT_PASSWORD') {
    await doSendForgotPassword(ticketVO.ticket)
  }
  pendingAction.value = null
}

const isLogin = computed(() => mode.value === 'login')
const isForgot = computed(() => mode.value === 'forgot')
const isRegisterEnabled = computed(() => systemStore.isRegistrationEnabled)
const isCaptchaEnabled = computed(() => systemStore.isCaptchaEnabled)

const loginForm = reactive({
  username: '',
  password: '',
  captchaCode: ''
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
  password: [{ required: true, message: t('auth.password_required') }],
  captchaCode: [
    { required: isCaptchaEnabled.value, message: t('auth.captcha_required') },
    {
      validator(_rule: FormItemRule, value: string) {
        if (!isCaptchaEnabled.value || !value) return true
        const type = captchaType.value.toUpperCase()
        if ((type === 'LINE' || type === 'SHEAR') && value.trim().length !== 6) {
          return new Error(t('auth.graphic_captcha_length_6'))
        }
        if (type === 'MATH' && !/^-?\d+$/.test(value.trim())) {
          return new Error(t('auth.math_captcha_invalid'))
        }
        return true
      },
      trigger: ['blur', 'input']
    }
  ]
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
  emailCaptcha: [{ required: true, message: t('auth.email_code_required') }]
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

function toggleMode() {
  if (isLogin.value && !isRegisterEnabled.value) return
  mode.value = isLogin.value ? 'register' : 'login'
  emit('mode-change', mode.value)
  if (isLogin.value && isCaptchaEnabled.value) {
    fetchCaptcha()
  }
}

async function fetchCaptcha() {
  if (!isCaptchaEnabled.value) return
  try {
    const res = await getCaptchaApi(captchaId.value || undefined)
    if (res.data) {
      captchaId.value = res.data.captchaId
      captchaImage.value = res.data.backgroundImage || res.data.imageBase64 || ''
      captchaType.value = (res.data.captchaType || 'LINE').toUpperCase()
    }
  } catch (e: any) {
    console.warn('Captcha load skipped', e)
  }
}

async function sendEmailVerificationCode() {
  if (!registerForm.email) {
    message.warning(t('auth.enter_email_first'))
    return
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(registerForm.email)) {
    message.warning(t('auth.enter_valid_email'))
    return
  }

  if (countdown.value > 0) return

  if (isCaptchaEnabled.value) {
    puzzleScene.value = 'REGISTER'
    pendingAction.value = 'SEND_EMAIL_CODE'
    showPuzzleModal.value = true
  } else {
    await doSendEmailCode()
  }
}

async function doSendEmailCode(ticket?: string) {
  try {
    await sendRegisterEmailCodeApi({
      email: registerForm.email.trim(),
      username: registerForm.username ? registerForm.username.trim() : undefined,
      captchaTicket: ticket || captchaTicket.value || undefined
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
  formRef.value?.validate(async (errors) => {
    if (errors) return
    if (isCaptchaEnabled.value) {
      puzzleScene.value = 'RESET_PASSWORD'
      pendingAction.value = 'FORGOT_PASSWORD'
      showPuzzleModal.value = true
    } else {
      await doSendForgotPassword()
    }
  })
}

async function doSendForgotPassword(ticket?: string) {
  loading.value = true
  try {
    await forgotPasswordApi({
      email: forgotForm.email.trim(),
      username: forgotForm.username.trim(),
      captchaTicket: ticket || captchaTicket.value || undefined
    })
    message.success(t('auth.reset_link_sent'))
    mode.value = 'login'
  } catch {
    captchaTicket.value = ''
  } finally {
    loading.value = false
  }
}

async function handleAction() {
  formRef.value?.validate(async (errors) => {
    if (errors) return
    if (isLogin.value) {
      loading.value = true
      try {
        await userStore.login({
          username: loginForm.username,
          password: loginForm.password,
          captchaId: captchaId.value || undefined,
          captchaCode: loginForm.captchaCode || undefined
        })
        message.success(t('auth.welcome_back'))
        emit('success', 'login')
      } catch {
        if (isLogin.value && isCaptchaEnabled.value) {
          await fetchCaptcha()
          loginForm.captchaCode = ''
        }
      } finally {
        loading.value = false
      }
    } else {
      await doRegister()
    }
  })
}

async function doRegister(ticket?: string) {
  loading.value = true
  try {
    await registerApi({
      username: registerForm.username.trim(),
      password: registerForm.password,
      email: registerForm.email.trim(),
      emailCaptcha: registerForm.emailCaptcha.trim(),
      captchaTicket: ticket || undefined
    })
    message.success(t('auth.account_created_success'))
    toggleMode()
  } catch {
    captchaTicket.value = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (isLogin.value && isCaptchaEnabled.value) {
    fetchCaptcha()
  }
})
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
    margin-top: 12px;
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
