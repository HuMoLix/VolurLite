<template>
  <div class="auth-card__body">
    <!-- 左侧区域：大标题与副标题 -->
    <div class="auth-card__left">
      <h1 class="auth-card__title">登录</h1>
      <p class="auth-card__subtitle">使用您的 Volur 账号</p>
      <!-- 预留扩展区域 (如已识别账号标签芯片) -->
      <div id="auth-header-extra" class="auth-card__header-extra" />
    </div>

    <!-- 右侧区域：表单与操作按钮 -->
    <div class="auth-card__right">
      <div class="auth-form-content w-full h-full flex flex-col justify-between">
        <!-- 输入与辅助提示区域 -->
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

          <!-- 忘记邮箱链接 (仅在hover时展示背景颜色 rgba(11, 87, 208, 0.08)，padding 为 1px 2px) -->
          <div class="text-left mt-2.5 mb-8">
            <a
              href="#"
              class="auth-forgot-link"
              @click.prevent="handleForgotAccount"
            >
              忘记了邮箱？
            </a>
          </div>

          <!-- 访客模式提示与了解详情链接 -->
          <p class="text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
            您用的不是自己的电脑？请使用访客模式无痕登录。<a
              href="#"
              class="auth-learn-more-link"
            >了解详情</a>
          </p>
        </div>

        <!-- 底部操作按钮区域 (右对齐，创建账号在下一步左侧，两者尺寸紧凑且完全相同) -->
        <div class="flex items-center justify-end gap-2.5 pt-6 sm:pt-8">
          <button
            type="button"
            class="auth-action-btn btn-create-account"
            @click="handleCreateAccount"
          >
            创建账号
          </button>
          <n-button
            type="primary"
            class="auth-action-btn btn-next-step"
            :loading="loading"
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import GoogleTextField from '@/components/google/GoogleTextField.vue'
import { useAuthFlowStore } from '@/store/modules/authFlow'

defineOptions({
  name: 'IdentifierView'
})

const route = useRoute()
const router = useRouter()
const authFlowStore = useAuthFlowStore()

const identifier = ref('')
const errorMsg = ref('')
const loading = computed(() => authFlowStore.loading)

onMounted(() => {
  // 确保进入登录页时，自动挂载与规范化 Google 风格 URL 状态机参数 (continue, flowName, flowEntry, dsh)
  authFlowStore.ensureFlowParams(route, router)
  if (authFlowStore.identifier) {
    identifier.value = authFlowStore.identifier
  }
})

async function handleNext() {
  if (authFlowStore.loading) return

  const trimmed = identifier.value.trim()
  if (!trimmed) {
    errorMsg.value = '请输入邮箱或账户名称'
    return
  }

  errorMsg.value = ''

  try {
    const nextStep = await authFlowStore.submitIdentifier(trimmed)
    router.push({
      path: `/v1/signin/challenge/${nextStep}`,
      query: authFlowStore.getQueryParams()
    })
  } catch (err: any) {
    errorMsg.value = err?.message || '账号核验失败，请重试'
  }
}

function handleForgotAccount() {
  if (authFlowStore.loading) return
  window.$message.info('跳转至账号找回 /v1/recovery/identifier')
}

function handleCreateAccount() {
  if (authFlowStore.loading) return
  authFlowStore.setFlow('VlifWebSignUp', 'WebSignUp')
  router.push({
    path: '/v1/signup',
    query: authFlowStore.getQueryParams({
      flowName: 'VlifWebSignUp',
      flowEntry: 'WebSignUp'
    })
  })
}
</script>

<style scoped lang="scss">
// 忘记邮箱链接：默认透明，仅在 hover 时展示背景颜色 rgba(11, 87, 208, 0.08)，无下划线，padding 为 1px 2px
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

// 了解详情链接：强制使用 #0b57d0，防止被全局 a { color: inherit } 覆盖为灰色
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

// 统一紧凑动作按钮规格 (创建账号与下一步尺寸严格相同：高 36px、宽 88px)
.auth-action-btn {
  height: 36px !important;
  min-width: 88px !important;
  padding: 0 16px !important;
  border-radius: 9999px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  box-sizing: border-box;
}

// 创建账号按钮：正常状态为透明底/蓝色文字，hover 时背景颜色为 rgba(11, 87, 208, 0.08)
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

  &:hover {
    background-color: rgba(11, 87, 208, 0.08) !important;
    color: #0b57d0 !important;
  }

  &:active {
    background-color: rgba(11, 87, 208, 0.16) !important;
    color: #0b57d0 !important;
  }
}

:global(.dark) .btn-create-account {
  color: #8ab4f8;

  &:hover {
    background-color: rgba(138, 180, 248, 0.12) !important;
    color: #8ab4f8 !important;
  }
}

// 下一步按钮：实心蓝底白字，hover 时适度加深，点击时加深一点点 (#063980) 与微缩放动效
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
    background-color: #063980 !important; // 加深一点点的颜色
    color: #ffffff !important;
    transform: scale(0.98); // 轻微缩放点击动效
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
