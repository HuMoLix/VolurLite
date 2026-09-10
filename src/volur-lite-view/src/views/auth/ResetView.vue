<template>
  <div class="auth-form">
    <!-- 无效 Token 状态 (例如直接访问 /reset-password、访问 /reset-password?token= 或 Token 为空) -->
    <div v-if="!hasValidToken" class="auth-form__invalid">
      <div class="auth-form__header">
        <h1 class="auth-form__title">{{ t('auth.invalid_token_title') }}</h1>
        <p class="auth-form__subtitle">
          {{ t('auth.invalid_token_subtitle') }}
        </p>
      </div>

      <div class="invalid-token-card">
        <n-result
          status="error"
          :title="t('auth.invalid_token')"
          :description="t('auth.invalid_token_description')"
        >
          <template #footer>
            <n-button
              type="primary"
              block
              size="large"
              class="auth-form__submit-button"
              @click="router.push('/auth/forgot')"
            >
              {{ t('auth.reapply_reset') }}
            </n-button>
          </template>
        </n-result>
      </div>

      <div class="auth-form__footer">
        <router-link to="/auth/login" class="auth-form__toggle-link">
          {{ t('auth.back_to_sign') }}
        </router-link>
      </div>
    </div>

    <!-- 拥有有效 Token 时展示重置新密码表单 -->
    <div v-else class="auth-form__valid">
      <div class="auth-form__header">
        <h1 class="auth-form__title">{{ t('auth.set_new_password_title') }}</h1>
        <p class="auth-form__subtitle">
          {{ t('auth.set_new_password_subtitle') }}
        </p>
      </div>

      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :show-require-mark="false"
        @keyup.enter="handleReset"
      >
        <n-form-item path="password" :label="t('auth.new_password_label')">
          <n-input
            v-model:value="form.password"
            type="password"
            show-password-on="click"
            :placeholder="t('auth.new_password_placeholder')"
            size="large"
            clearable
          />
        </n-form-item>

        <n-form-item path="confirmPassword" :label="t('auth.confirm_new_password_label')">
          <n-input
            v-model:value="form.confirmPassword"
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
  NResult,
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

const token = computed<string>(() => {
  const q = route.query.token
  return typeof q === 'string' ? q.trim() : ''
})

const hasValidToken = computed<boolean>(() => token.value.length > 0)

const form = reactive({
  password: '',
  confirmPassword: ''
})

// Jakarta Validation Pattern: ^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\d@$!%*?&._#^&+=]{6,32}$
const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*?&._#^&+=])[A-Za-z\d@$!%*?&._#^&+=]{6,32}$/

function validatePasswordRule(_rule: FormItemRule, value: string) {
  if (!value) return true
  if (value.length < 6 || value.length > 32) {
    return new Error(t('auth.password_length'))
  }
  if (!PASSWORD_REGEX.test(value)) {
    return new Error(t('auth.password_pattern'))
  }
  return true
}

const rules = computed<FormRules>(() => ({
  password: [
    { required: true, message: t('auth.new_password_required'), trigger: ['blur', 'input'] },
    { validator: validatePasswordRule, trigger: ['blur', 'input'] }
  ],
  confirmPassword: [
    { required: true, message: t('auth.confirm_password_required'), trigger: ['blur', 'input'] },
    {
      validator: (_rule: FormItemRule, value: string) => {
        if (!value) return true
        if (value !== form.password) {
          return new Error(t('auth.passwords_mismatch'))
        }
        return true
      },
      trigger: ['blur', 'input']
    }
  ]
}))

async function handleReset() {
  if (!hasValidToken.value) {
    message.error(t('auth.invalid_token'))
    return
  }

  formRef.value?.validate(async (errors) => {
    if (errors) return

    loading.value = true
    try {
      await resetPasswordByTokenApi({
        token: token.value,
        newPassword: form.password
      })

      message.success(t('auth.reset_success_hint'))
      setTimeout(() => {
        router.push('/auth/login').catch(() => {})
      }, 1500)
    } catch {
      // Handled by request error interceptor
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  if (!hasValidToken.value) {
    message.error(t('auth.invalid_token'))
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
    margin-bottom: 24px;

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

  .invalid-token-card {
    padding: 8px 0 16px;

    :deep(.n-result) {
      .n-result-header__title {
        font-size: 17px;
        font-weight: 500;
        margin-top: 10px;
      }

      .n-result-body {
        margin-top: 6px;
        font-size: 13px;
        color: #666;
        line-height: 1.6;
      }

      .n-result-footer {
        margin-top: 20px;
        width: 100%;
      }
    }
  }

  .auth-form__submit-button {
    margin-top: 12px;
    height: 48px;
    font-size: 16px;
    font-weight: 500;
    border-radius: $border-radius-base;
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
