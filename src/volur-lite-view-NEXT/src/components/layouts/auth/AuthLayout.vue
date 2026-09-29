<template>
  <div class="auth-layout min-h-[100dvh] flex flex-col justify-between md:justify-center items-center transition-colors duration-300">
    <!-- 全屏白色半透明遮罩层 (对齐 Google Accounts .ZQxJQe 规范：等待服务端响应时覆盖整个页面并防止重复点击) -->
    <transition name="auth-scrim-fade">
      <div
        v-if="isCardLoading"
        class="auth-scrim"
        role="presentation"
        aria-hidden="true"
        @click.stop.prevent
        @mousedown.stop.prevent
        @dblclick.stop.prevent
        @keydown.stop.prevent
        @touchstart.stop.prevent
      />
    </transition>

    <div
      class="auth-card"
      :class="{ 'is-loading': isCardLoading }"
      :aria-busy="isCardLoading"
    >
      <!-- Google Accounts 原生 Material Linear Progress (卡片框上方进度条：等待服务端响应) -->
      <div
        class="auth-card__progress"
        :class="{ 'is-active': isCardLoading }"
        role="progressbar"
        aria-label="正在处理中，请稍候"
        :aria-hidden="!isCardLoading"
        aria-valuemin="0"
        aria-valuemax="100"
      >
        <div class="auth-card__progress-track" />
        <div class="auth-card__progress-bar auth-card__progress-bar--primary">
          <span class="auth-card__progress-inner" />
        </div>
        <div class="auth-card__progress-bar auth-card__progress-bar--secondary">
          <span class="auth-card__progress-inner" />
        </div>
      </div>

      <!-- 防重复点击拦截层：等待服务端响应期间阻断一切鼠标与交互操作穿透 -->
      <div
        v-if="isCardLoading"
        class="auth-card__blocker"
        aria-hidden="true"
        @click.stop.prevent
        @mousedown.stop.prevent
        @dblclick.stop.prevent
        @keydown.stop.prevent
        @touchstart.stop.prevent
      />

      <div class="auth-card__header">
        <img
          src="/r/static/svg/favicon-simple.svg"
          alt="Volur"
          class="auth-card__logo"
        />
      </div>
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" v-if="Component" />
          <slot v-else />
        </transition>
      </router-view>
    </div>

    <footer class="auth-footer text-[12px]">
      <div class="auth-footer__lang shrink-0">
        <n-dropdown :options="languageOptions" trigger="click" @select="handleLanguageSelect">
          <button
            type="button"
            class="auth-btn-link text-[12px] text-google-text-muted dark:text-google-text-darkMuted"
          >
            <span>{{ currentLangLabel }}</span>
            <svg
              class="w-2.5 h-1.5 ml-2.5 text-google-text-muted dark:text-google-text-darkMuted fill-current shrink-0"
              viewBox="7 10 10 5"
              aria-hidden="true"
            >
              <polygon points="7 10 12 15 17 10" />
            </svg>
          </button>
        </n-dropdown>
      </div>

      <div class="auth-footer__meta flex items-center flex-wrap gap-1 text-[12px] text-google-text-muted dark:text-google-text-darkMuted shrink-0">
        <a href="#" class="auth-btn-link text-[12px]">帮助</a>
        <a href="#" class="auth-btn-link text-[12px]">隐私权</a>
        <a href="#" class="auth-btn-link text-[12px]">条款</a>

        <span class="auth-footer__copyright whitespace-nowrap px-2 text-[12px] inline-flex items-center h-8 select-none">
          <span class="hidden md:inline">© 2026 Volur, Inc.</span>
          <span class="inline md:hidden">© 2026 Volur</span>
        </span>

        <a
          href="https://beian.miit.gov.cn/"
          target="_blank"
          rel="noopener noreferrer"
          class="auth-btn-link text-[12px] gap-1.5 text-google-text-muted dark:text-google-text-darkMuted"
          title="工信部备案"
        >
          <img
            src="/r/static/img/beian.png"
            alt="备案"
            class="w-3.5 h-3.5 object-contain shrink-0"
          />
          <span class="hidden md:inline"></span>
        </a>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'AuthLayout'
})

import { computed, provide, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { NDropdown } from 'naive-ui'
import { useAuthFlowStore } from '@/store/modules/authFlow'

const props = withDefaults(
  defineProps<{
    loading?: boolean
  }>(),
  {
    loading: undefined
  }
)

const route = useRoute()
const authFlowStore = useAuthFlowStore()

// 当 props.loading 显式传入时优先使用，否则联动 authFlowStore.loading
const isCardLoading = computed(() => {
  if (typeof props.loading === 'boolean') {
    return props.loading
  }
  return authFlowStore.loading
})

// 向后代组件提供统一的 loading 状态与控制方法依赖注入
provide('authLoading', isCardLoading)
provide('setAuthLoading', (val: boolean) => {
  authFlowStore.setLoading(val)
})

// 监听路由切换：当跳转至新路由时，确保自动复位 loading 状态，杜绝界面意外锁死
watch(
  () => route.fullPath,
  () => {
    if (authFlowStore.loading) {
      authFlowStore.setLoading(false)
    }
  }
)

// 多语言切换配置 (仅保留简体中文、繁体中文、English)
const currentLang = ref('zh-CN')
const languageOptions = [
  { label: '简体中文', key: 'zh-CN' },
  { label: '繁体中文', key: 'zh-TW' },
  { label: 'English', key: 'en-US' }
]

const currentLangLabel = computed(() => {
  const match = languageOptions.find(item => item.key === currentLang.value)
  return match?.label || '简体中文'
})

function handleLanguageSelect(key: string) {
  currentLang.value = key
}
</script>

<style scoped lang="scss">
// 认证整体背景色：电脑端为 Google Material 3 表面色 (#f0f4f9 / 暗色 #1e1f20)，手机端为无缝底色 (#ffffff / 暗色 #202124)
.auth-layout {
  width: 100%;
  box-sizing: border-box;

  // 手机端：纯白 (亮色) / Google Dark 900 (暗色)
  background-color: var(--v-bg-surface, #ffffff);
  padding: 0;

  // 电脑端：标准 Google Material 3 表面色 (#f0f4f9 / 暗色 #1e1f20)
  @media (min-width: 840px) {
    background-color: var(--sys-color-surface-container, #f0f4f9);
    padding: 32px 24px;
  }
}

// -------------------------------------------------------------
// Google Accounts 全屏白色半透明遮罩层 (对齐 Google Accounts .ZQxJQe 规范)
// -------------------------------------------------------------
.auth-scrim {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100vw;
  height: 100vh;
  background-color: #ffffff;
  opacity: 0.5; // 对齐 Google 原生 .ZQxJQe: background-color: #fff; opacity: .5;
  outline: none;
  z-index: 5; // 对齐 Google 原生 .ZQxJQe: z-index: 5
  cursor: wait;
  pointer-events: all;
  -webkit-tap-highlight-color: transparent;
}

:global(html.dark) .auth-scrim {
  background-color: #1e1f20 !important;
  opacity: 0.55 !important;
}

.auth-scrim-fade-enter-active,
.auth-scrim-fade-leave-active {
  transition: opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.auth-scrim-fade-enter-from,
.auth-scrim-fade-leave-to {
  opacity: 0 !important;
}

// 核心卡片容器：根据视口宽度使用 @media 响应式区分电脑端与手机端
.auth-card {
  position: relative; // 保证卡片顶部进度条与防重遮罩精准以卡片为锚点
  width: 100%;
  box-sizing: border-box;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &.is-loading {
    user-select: none;
  }

  // 手机端：不显示 PC 端的背景和边框，只显示卡片框中的内容 (上下布局，无缝融合)
  display: flex;
  flex-direction: column;
  background-color: transparent !important;
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  padding: 32px 24px 20px 24px;
  max-width: 100%;
  min-height: unset;

  // -------------------------------------------------------------
  // Google Material Linear Progress (对齐 Google Accounts Glif 规范)
  // -------------------------------------------------------------
  &__progress {
    position: absolute;
    top: 0;
    left: 24px;
    right: 24px;
    height: 4px;
    overflow: hidden;
    border-radius: 2px;
    z-index: 10;
    pointer-events: none;
    opacity: 0;
    transition: opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1);

    @media (min-width: 840px) {
      left: 44px;
      right: 44px;
    }

    &.is-active {
      opacity: 1;
    }

    &-track {
      position: absolute;
      inset: 0;
      background-color: var(--google-progress-track, #e8f0fe);
      border-radius: inherit;
    }

    &-bar {
      position: absolute;
      top: 0;
      bottom: 0;
      width: 100%;
      transform-origin: top left;
      will-change: transform;

      &--primary {
        left: -145.166611%;
      }

      &--secondary {
        left: -54.888891%;
      }
    }

    &-inner {
      display: inline-block;
      position: absolute;
      inset: 0;
      background-color: var(--google-progress-indicator, #0b57d0);
      border-radius: inherit;
      will-change: transform;
    }

    &.is-active {
      .auth-card__progress-bar--primary {
        animation: mdc-linear-progress-primary-indeterminate-translate 2s linear infinite;

        .auth-card__progress-inner {
          animation: mdc-linear-progress-primary-indeterminate-scale 2s linear infinite;
        }
      }

      .auth-card__progress-bar--secondary {
        animation: mdc-linear-progress-secondary-indeterminate-translate 2s linear infinite;

        .auth-card__progress-inner {
          animation: mdc-linear-progress-secondary-indeterminate-scale 2s linear infinite;
        }
      }
    }
  }

  // 防重复点击遮罩层：高优先级盖在卡片表单之上，拦截所有点击并赋予等待光标
  &__blocker {
    position: absolute;
    inset: 0;
    z-index: 8;
    background: transparent;
    cursor: wait;
    pointer-events: all;
    border-radius: inherit;
  }

  &__header {
    width: 100%;
    display: flex;
    justify-content: flex-start;
    margin-bottom: 16px;
  }

  &__logo {
    width: 48px;
    height: 48px;
    padding: 4px;
    box-sizing: border-box;
    object-fit: contain;
  }

  &__body,
  :deep(.auth-card__body) {
    width: 100%;
    display: flex;
    flex-direction: column;
    flex: 1;
  }

  &__left,
  :deep(.auth-card__left) {
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    text-align: left;
    margin-bottom: 24px;
  }

  &__title,
  :deep(.auth-card__title) {
    font-size: 24px;
    font-weight: 400;
    line-height: 1.33;
    color: var(--v-text-primary, #202124);
    margin: 0 0 8px 0;
  }

  &__subtitle,
  :deep(.auth-card__subtitle) {
    font-size: 14px;
    line-height: 1.43;
    color: var(--v-text-desc, #3c4043);
    margin: 0;
  }

  &__header-extra,
  :deep(.auth-card__header-extra) {
    margin-top: 12px;
    width: 100%;
  }

  &__right,
  :deep(.auth-card__right) {
    width: 100%;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    flex: 1;
  }

  // 电脑端：@media 切换为白底 (暗色为深灰卡片底) 无边框圆角卡片，Logo 置顶，左侧标题与右侧表单由 flex 管理且同行对齐
  @media (min-width: 840px) {
    flex-direction: column;
    max-width: 1000px;
    min-height: 390px;
    padding: 36px 44px;
    background-color: var(--v-bg-card, #ffffff) !important;
    border: none !important;
    border-radius: 28px !important;
    box-shadow: 0 1px 3px 0 rgba(60, 64, 67, 0.08) !important;

    &__header {
      margin-bottom: 24px;
    }

    &__logo {
      width: 48px;
      height: 48px;
      padding: 4px;
    }

    &__body,
    :deep(.auth-card__body) {
      flex-direction: row;
      flex: 1;
      align-items: stretch;
    }

    &__left,
    :deep(.auth-card__left) {
      flex: 0 0 50%;
      max-width: 50%;
      padding-right: 32px;
      margin-bottom: 0;
    }

    &__title,
    :deep(.auth-card__title) {
      font-size: 32px;
      line-height: 1.25;
      margin: 0 0 12px 0;
    }

    &__subtitle,
    :deep(.auth-card__subtitle) {
      font-size: 16px;
      line-height: 1.5;
    }

    &__right,
    :deep(.auth-card__right) {
      flex: 0 0 50%;
      max-width: 50%;
      padding-left: 20px;
      padding-top: 0;
    }
  }
}

:global(html.dark) .auth-card {
  @media (min-width: 840px) {
    box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.4) !important;
  }
}

// 统一认证页脚：手机端置底自适应 / 电脑端对齐卡片宽度 (1000px)
.auth-footer {
  width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.3s ease;

  // 手机端：吸底展示，单一行排列
  margin-top: auto;
  padding: 12px 16px 20px 16px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;

  // 电脑端：对齐卡片宽度 (1000px) 并紧贴其下
  @media (min-width: 840px) {
    max-width: 1000px;
    margin-top: 14px;
    padding: 0 16px;
    overflow-x: visible;
  }
}

// 页面切换微动画
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

// 暗色模式下的 Google 进度条色彩体系
:global(html.dark) {
  .auth-card__progress-track {
    background-color: rgba(138, 180, 248, 0.24) !important;
  }

  .auth-card__progress-inner {
    background-color: #8ab4f8 !important;
  }
}

// -------------------------------------------------------------
// Google 原生 Material Linear Progress 关键帧动画
// -------------------------------------------------------------
@keyframes mdc-linear-progress-primary-indeterminate-translate {
  0% {
    transform: translateX(0);
  }
  20% {
    animation-timing-function: cubic-bezier(0.5, 0, 0.701732, 0.495819);
    transform: translateX(0);
  }
  59.15% {
    animation-timing-function: cubic-bezier(0.302435, 0.381352, 0.55, 0.956352);
    transform: translateX(83.67142%);
  }
  to {
    transform: translateX(200.611057%);
  }
}

@keyframes mdc-linear-progress-primary-indeterminate-scale {
  0% {
    transform: scaleX(0.08);
  }
  36.65% {
    animation-timing-function: cubic-bezier(0.334731, 0.12482, 0.785844, 1);
    transform: scaleX(0.08);
  }
  69.15% {
    animation-timing-function: cubic-bezier(0.06, 0.11, 0.6, 1);
    transform: scaleX(0.661479);
  }
  to {
    transform: scaleX(0.08);
  }
}

@keyframes mdc-linear-progress-secondary-indeterminate-translate {
  0% {
    animation-timing-function: cubic-bezier(0.15, 0, 0.515058, 0.409685);
    transform: translateX(0);
  }
  25% {
    animation-timing-function: cubic-bezier(0.31033, 0.284058, 0.8, 0.733712);
    transform: translateX(37.651913%);
  }
  48.35% {
    animation-timing-function: cubic-bezier(0.4, 0.627035, 0.6, 0.902026);
    transform: translateX(84.386165%);
  }
  to {
    transform: translateX(160.277782%);
  }
}

@keyframes mdc-linear-progress-secondary-indeterminate-scale {
  0% {
    animation-timing-function: cubic-bezier(0.205028, 0.057051, 0.57661, 0.453971);
    transform: scaleX(0.08);
  }
  19.15% {
    animation-timing-function: cubic-bezier(0.152313, 0.196432, 0.648374, 1.004315);
    transform: scaleX(0.457104);
  }
  44.15% {
    animation-timing-function: cubic-bezier(0.257759, -0.003163, 0.211762, 1.38179);
    transform: scaleX(0.72796);
  }
  to {
    transform: scaleX(0.08);
  }
}
</style>
