<template>
  <div class="auth-form">
    <div class="auth-form__header">
      <h1 class="auth-form__title">{{ t('auth.set_new_password_title') }}</h1>
      <p class="auth-form__subtitle">
        {{ t('auth.set_new_password_subtitle') }}
      </p>
    </div>

    <n-form
      ref="formRef"
      :model="resetForm"
      :rules="rules"
      :show-require-mark="false"
      @keyup.enter="handleReset"
    >
      <n-form-item path="password" :label="t('auth.new_password_label')">
        <n-input
          v-model:value="resetForm.password"
          type="password"
          show-password-on="click"
          :placeholder="t('auth.new_password_placeholder')"
          size="large"
          clearable
        />
      </n-form-item>

      <n-form-item path="confirmPassword" :label="t('auth.confirm_new_password_label')">
        <n-input
          v-model:value="resetForm.confirmPassword"
          type="password"
          show-password-on="click"
          :placeholder="t('auth.confirm_new_password_placeholder')"
          size="large"
          clearable
        />
      </n-form-item>

      <n-button
        type="primary"
        block
        size="large"
        :loading="loading"
        @click="handleReset"
        class="auth-form__submit-button"
      >
        {{ t('auth.reset_password_button') }}
      </n-button>
    </n-form>

    <div class="auth-form__footer">
      <n-text depth="3">
        {{ t('auth.remember_it_now') }}
        <router-link to="/auth/login" class="auth-form__toggle-link">
          {{ t('auth.back_to_sign') }}
        </router-link>
      </n-text>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
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
import { resetPasswordByTokenApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const { t } = useI18n()

const formRef = ref<FormInst | null>(null)
const loading = ref<boolean>(false)
const token = ref<string>('')

const resetForm = reactive({
  password: '',
  confirmPassword: ''
})

const rules = computed<FormRules>(() => ({
  password: [
    { required: true, message: t('auth.new_password_required') },
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
  confirmPassword: [
    { required: true, message: t('auth.confirm_password_required') },
    {
      validator: (_rule: FormItemRule, value: string) => {
        if (!value) return true
        if (value !== resetForm.password) {
          return new Error(t('auth.passwords_mismatch'))
        }
        return true
      },
      trigger: ['blur', 'input']
    }
  ]
}))

async function handleReset() {
  formRef.value?.validate(async (errors) => {
    if (errors) return

    if (!token.value) {
      message.error(t('auth.invalid_token'))
      return
    }

    loading.value = true
    try {
      await resetPasswordByTokenApi({
        token: token.value,
        newPassword: resetForm.password
      })

      message.success(t('auth.reset_success_hint'))
      setTimeout(() => {
        router.push('/auth/login').catch((err) => {
          console.error('Route navigation failed:', err)
        })
      }, 1500)
    } catch {
      // Global Axios interceptor handles toast error
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  const tokenValue = (route.query.token as string) || ''
  if (!tokenValue) {
    message.warning(t('auth.no_token_warning'))
  } else {
    token.value = tokenValue
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
