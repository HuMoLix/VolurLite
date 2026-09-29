<template>
  <div class="auth-card__body">
    <!-- 左侧区域：大标题、副标题与扩展芯片 -->
    <div class="auth-card__left">
      <h1 class="auth-card__title">{{ cardTitle }}</h1>
      <p v-if="cardSubtitle" class="auth-card__subtitle">{{ cardSubtitle }}</p>
      <!-- 预留扩展区域 / 已识别账号标签芯片 -->
      <div id="auth-header-extra" class="auth-card__header-extra">
        <button
          type="button"
          class="auth-account-chip"
          @click="goBackIdentifier"
          title="切换账号"
        >
          <span class="auth-account-chip__avatar">
            {{ displayInitial }}
          </span>
          <span class="auth-account-chip__label">{{ displayIdentifier }}</span>
          <svg
            class="w-3 h-3 text-google-text-muted dark:text-google-text-darkMuted fill-current"
            viewBox="7 10 10 5"
            aria-hidden="true"
          >
            <polygon points="7 10 12 15 17 10" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 右侧区域：安全验证挑战主体 (收归密码为众多 Challenge 中的一种: pwd) -->
    <div class="auth-card__right">
      <div class="auth-form-content w-full h-full flex flex-col justify-between">
        <form class="space-y-6" @submit.prevent="handleVerifyChallenge">
          <!-- 1. 子类型：pwd 密码挑战 (/v1/signin/challenge/pwd) -->
          <div v-if="challengeType === 'pwd'" class="space-y-6">
            <div>
              <GoogleTextField
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                label="输入您的密码"
                autocomplete="current-password"
                autofocus
                :error-msg="errorMsg"
                @clear="errorMsg = ''"
                @enter="handleVerifyChallenge"
              />
            </div>

            <div class="flex items-center justify-between">
              <label class="flex items-center gap-2 text-sm text-google-text-desc dark:text-google-text-darkDesc cursor-pointer select-none">
                <input
                  type="checkbox"
                  v-model="showPassword"
                  class="w-4 h-4 rounded border-google-border-light text-google-blue focus:ring-google-blue cursor-pointer"
                />
                <span>显示密码</span>
              </label>
              <a
                href="#"
                class="auth-forgot-link"
                @click.prevent="handleForgotPassword"
              >
                忘记密码？
              </a>
            </div>
          </div>

          <!-- 2. 子类型：recaptcha 人机身份验证挑战 (/v1/signin/challenge/recaptcha) -->
          <div v-else-if="challengeType === 'recaptcha'" class="space-y-5">
            <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
              为了保护您的账号不受自动化程序攻击，请勾选下方验证框完成人机身份验证。
            </p>

            <!-- 经典 Google reCAPTCHA 交互卡片 -->
            <div class="recaptcha-widget">
              <div class="recaptcha-widget__left" @click="handleRecaptchaCheck">
                <div
                  class="recaptcha-checkbox"
                  :class="{
                    'is-checking': isRecaptchaChecking,
                    'is-checked': isRecaptchaPassed
                  }"
                >
                  <div v-if="isRecaptchaChecking" class="recaptcha-spinner" />
                  <svg v-else-if="isRecaptchaPassed" class="w-6 h-6 text-google-green dark:text-google-green-dark" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                  </svg>
                </div>
                <span class="recaptcha-label select-none">
                  {{ isRecaptchaPassed ? '验证已通过' : '我不是机器人' }}
                </span>
              </div>

              <div class="recaptcha-widget__right select-none">
                <img src="/r/static/svg/favicon-simple.svg" alt="reCAPTCHA" class="w-7 h-7 object-contain opacity-70" />
                <span class="text-[10px] text-google-text-sub font-semibold">reCAPTCHA</span>
                <div class="text-[8px] text-google-text-sub space-x-1">
                  <a href="#" class="hover:underline" @click.prevent>隐私权</a>
                  <span>-</span>
                  <a href="#" class="hover:underline" @click.prevent>条款</a>
                </div>
              </div>
            </div>

            <p v-if="recaptchaError" class="text-xs text-google-red dark:text-google-red-dark">
              {{ recaptchaError }}
            </p>
          </div>

          <!-- 3. 子类型：device 设备与异常网络确认挑战 (/v1/signin/challenge/device) -->
          <div v-else-if="challengeType === 'device'" class="space-y-4">
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
          </div>

          <!-- 4. 子类型：2fa / totp 两步动态口令验证挑战 (/v1/signin/challenge/2fa) -->
          <div v-else class="space-y-4">
            <div class="auth-challenge-banner flex items-start gap-3 p-3.5 rounded-lg border border-google-border-light dark:border-google-border-dark bg-google-surface-light dark:bg-google-surface-darkElevated">
              <svg class="w-5 h-5 text-google-blue dark:text-google-blue-dark shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/>
              </svg>
              <div class="text-xs text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
                <p class="font-medium text-google-text-primary dark:text-google-text-darkPrimary mb-0.5">
                  触发 2 步安全验证
                </p>
                为保障账号资产安全，已向绑定安全邮箱发送了 6 位动态验证码。
              </div>
            </div>

            <div>
              <GoogleTextField
                v-model="verificationCode"
                label="输入 6 位安全验证码"
                autocomplete="one-time-code"
                autofocus
                :error-msg="errorMsg"
                @clear="errorMsg = ''"
                @enter="handleVerifyChallenge"
              />
            </div>

            <div class="flex items-center justify-between pt-1">
              <label class="flex items-center gap-2 text-sm text-google-text-desc dark:text-google-text-darkDesc cursor-pointer select-none">
                <input
                  type="checkbox"
                  v-model="trustDevice"
                  class="w-4 h-4 rounded border-google-border-light text-google-blue focus:ring-google-blue cursor-pointer"
                />
                <span>在此设备上信任 30 天</span>
              </label>
              <a
                href="#"
                class="auth-another-way-link"
                @click.prevent="handleTryAnotherWay"
              >
                改用其他方式
              </a>
            </div>
          </div>

          <!-- 底部操作按钮区域 (严格对齐 Google M3) -->
          <div class="flex items-center justify-between pt-6 sm:pt-10">
            <button
              type="button"
              class="auth-action-btn btn-back-action"
              @click="goBackIdentifier"
            >
              返回
            </button>
            <n-button
              type="primary"
              class="auth-action-btn btn-verify-action"
              :loading="loading"
              @click="handleVerifyChallenge"
            >
              {{ challengeActionLabel }}
            </n-button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useAuthFlowStore } from '@/store/modules/authFlow'

defineOptions({
  name: 'ChallengeView'
})

const route = useRoute()
const router = useRouter()
const authFlowStore = useAuthFlowStore()

// 路由挑战子类型: pwd (密码挑战) | recaptcha (人机验证) | device (新设备) | 2fa (两步验证)
const challengeType = computed(() => {
  const typeParam = (route.params.type as string) || (route.query.challengeType as string)
  return typeParam ? typeParam.toLowerCase() : 'pwd'
})

// 标题与副标题组件自主驱动
const cardTitle = computed(() => {
  if (challengeType.value === 'pwd') return '欢迎'
  if (challengeType.value === 'recaptcha') return '安全核验'
  return '安全核验与挑战'
})

const cardSubtitle = computed(() => {
  if (challengeType.value === 'pwd') return ''
  if (challengeType.value === 'recaptcha') return '为了保护您的账号安全，请完成以下人机身份验证'
  return '系统检测到本次登录可能存在风险，请完成安全挑战以继续'
})

const challengeActionLabel = computed(() => {
  if (challengeType.value === 'pwd') return '登录'
  if (challengeType.value === 'recaptcha') return '下一步'
  return '验证'
})

// 表单响应式状态
const password = ref('')
const showPassword = ref(false)
const verificationCode = ref('')
const trustDevice = ref(true)
const errorMsg = ref('')
const recaptchaError = ref('')
const loading = computed(() => authFlowStore.loading)

// reCAPTCHA 状态模拟
const isRecaptchaChecking = ref(false)
const isRecaptchaPassed = ref(false)

const currentTimeFormatted = computed(() => {
  return new Date().toLocaleTimeString('zh-CN', { hour12: false })
})

const displayIdentifier = computed(() => {
  return authFlowStore.identifier || 'user@example.com'
})

const displayInitial = computed(() => {
  return (displayIdentifier.value.charAt(0) || 'U').toUpperCase()
})

onMounted(() => {
  // 1. 确保基础参数补齐 (dsh, flowName, flowEntry, continue 等)
  authFlowStore.ensureFlowParams(route, router)

  // 2. 账号合法性门禁校验：若未输入账号或会话已丢失，重定向回第一步输入账号
  if (!authFlowStore.identifier) {
    window.$message?.warning?.('请先输入登录账号')
    router.replace({
      path: '/v1/signin/identifier',
      query: authFlowStore.getQueryParams()
    })
    return
  }

  // 3. 状态流转票据 (TL) 门禁校验：必须持有从前序步骤签发的合法 TL 方可进入挑战页
  if (!authFlowStore.validateTransitionTicket(route)) {
    window.$message?.warning?.('认证流转票据 (TL) 缺失或已失效，请重新输入账号')
    router.replace({
      path: '/v1/signin/identifier',
      query: authFlowStore.getQueryParams()
    })
    return
  }
})

function handleRecaptchaCheck() {
  if (authFlowStore.loading || isRecaptchaPassed.value || isRecaptchaChecking.value) return

  isRecaptchaChecking.value = true
  recaptchaError.value = ''

  setTimeout(() => {
    isRecaptchaChecking.value = false
    isRecaptchaPassed.value = true
    window.$message?.success?.('人机身份验证已通过')
  }, 1000)
}

function goBackIdentifier() {
  if (authFlowStore.loading) return
  router.push({
    path: '/v1/signin/identifier',
    query: authFlowStore.getQueryParams()
  })
}

async function handleVerifyChallenge() {
  if (authFlowStore.loading) return

  // 1. 密码挑战 (pwd)
  if (challengeType.value === 'pwd') {
    if (!password.value) {
      errorMsg.value = '请输入密码'
      return
    }
    errorMsg.value = ''

    try {
      const res = await authFlowStore.submitChallenge('pwd', {
        password: password.value,
        rememberMe: trustDevice.value
      })

      if (res.success) {
        window.$message?.success?.('登录成功！正在跳转至业务主页...')
        router.push(res.redirect || authFlowStore.continueUrl || '/competitions')
      } else if (res.nextStep) {
        router.push({
          path: `/v1/signin/challenge/${res.nextStep}`,
          query: authFlowStore.getQueryParams()
        })
      }
    } catch (err: any) {
      errorMsg.value = err?.message || '密码错误，请重新输入'
    }
    return
  }

  // 2. 人机验证挑战 (recaptcha)
  if (challengeType.value === 'recaptcha') {
    if (!isRecaptchaPassed.value) {
      recaptchaError.value = '请勾选人机验证框以继续'
      return
    }
    recaptchaError.value = ''

    try {
      const res = await authFlowStore.submitChallenge('recaptcha', {
        captchaCode: 'mock_recaptcha_passed'
      })

      // 服务端重算风控评分 (Interstitial, 内部推进至 cid=2)，更新 TL 并推进至密码挑战 (cid=3)
      const nextStep = res.nextStep || 'pwd'
      router.push({
        path: `/v1/signin/challenge/${nextStep}`,
        query: authFlowStore.getQueryParams()
      })
    } catch (err: any) {
      recaptchaError.value = err?.message || '人机验证失败，请重试'
    }
    return
  }

  // 3. 新设备确认挑战 (device)
  if (challengeType.value === 'device') {
    try {
      const res = await authFlowStore.submitChallenge('device', {})
      const nextStep = res.nextStep || 'pwd'
      router.push({
        path: `/v1/signin/challenge/${nextStep}`,
        query: authFlowStore.getQueryParams()
      })
    } catch (err: any) {
      window.$message?.error?.(err?.message || '设备核验失败')
    }
    return
  }

  // 4. 2FA 动态口令挑战 (2fa / totp)
  if (!verificationCode.value.trim()) {
    errorMsg.value = '请输入安全验证码'
    return
  }
  errorMsg.value = ''

  try {
    const res = await authFlowStore.submitChallenge('2fa', {
      verificationCode: verificationCode.value.trim(),
      rememberMe: trustDevice.value
    })
    if (res.success) {
      window.$message?.success?.('双重验证通过，登录成功！')
      router.push(res.redirect || authFlowStore.continueUrl || '/competitions')
    }
  } catch (err: any) {
    errorMsg.value = err?.message || '验证码错误或已过期'
  }
}

function handleForgotPassword() {
  if (authFlowStore.loading) return
  window.$message.info('跳转至重置密码 /v1/recovery/challenge')
}

function handleTryAnotherWay() {
  if (authFlowStore.loading) return
  window.$message?.info?.('可改用通行密钥 (Passkey) 或设备安全提示验证')
}
</script>

<style scoped lang="scss">
// 账号标签芯片 (对齐 Google Accounts 顶部切换账号按钮)
.auth-account-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 6px;
  border-radius: 9999px;
  border: 1px solid var(--v-border-base, #dadce0);
  background: transparent;
  color: var(--v-text-primary, #202124);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;

  &:hover {
    background-color: rgba(60, 64, 67, 0.08);
  }

  &__avatar {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background-color: #0b57d0;
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: 700;
  }

  &__label {
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

:global(html.dark) .auth-account-chip {
  border-color: #3c4043;
  color: var(--v-text-primary, #e8eaed);

  &:hover {
    background-color: rgba(255, 255, 255, 0.08);
  }

  &__avatar {
    background-color: #8ab4f8;
    color: #202124;
  }
}

// 忘记密码链接：与 IdentifierView 统一规范
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

:global(html.dark) .auth-forgot-link {
  color: #8ab4f8 !important;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }
}

// Google 经典风格 reCAPTCHA 交互卡片
.recaptcha-widget {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 310px;
  height: 76px;
  padding: 10px 14px;
  border-radius: 3px;
  border: 1px solid var(--v-border-base, #dadce0);
  background-color: #f9f9f9;
  box-sizing: border-box;

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
  }

  &__right {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    line-height: 1.2;
  }
}

:global(html.dark) .recaptcha-widget {
  border-color: #3c4043;
  background-color: #292a2d;
}

.recaptcha-checkbox {
  width: 26px;
  height: 26px;
  border-radius: 2px;
  border: 2px solid #c1c1c1;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &.is-checked {
    border-color: transparent;
    background-color: transparent;
  }
}

:global(html.dark) .recaptcha-checkbox {
  border-color: #5f6368;
  background-color: #202124;
}

.recaptcha-spinner {
  width: 18px;
  height: 18px;
  border: 2.5px solid #0b57d0;
  border-top-color: transparent;
  border-radius: 50%;
  animation: recaptcha-spin 0.8s linear infinite;
}

@keyframes recaptcha-spin {
  to {
    transform: rotate(360deg);
  }
}

.recaptcha-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--v-text-primary, #202124);
}

:global(html.dark) .recaptcha-label {
  color: var(--v-text-primary, #e8eaed);
}

// 改用其他方式链接
.auth-another-way-link {
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
    color: #0b57d0 !important;
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
  }
}

:global(html.dark) .auth-another-way-link {
  color: #8ab4f8 !important;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }
}

// 统一紧凑动作按钮规格 (高 36px，全药丸 rounded-full 9999px)
.auth-action-btn {
  height: 36px !important;
  min-width: 88px !important;
  padding: 0 16px !important;
  border-radius: 9999px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  box-sizing: border-box;
}

// 文本按钮：“返回” (Google M3 Text Button)
.btn-back-action {
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

:global(html.dark) .btn-back-action {
  color: #8ab4f8;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }

  &:active {
    background-color: rgba(138, 180, 248, 0.2) !important;
  }
}

// 实心主按钮：“验证 / 下一步 / 登录” (Google M3 Filled Button)
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
