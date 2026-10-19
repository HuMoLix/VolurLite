<template>
  <div class="min-h-screen p-6 md:p-12 transition-colors duration-300 bg-google-surface-light dark:bg-google-surface-dark">
    <!-- 头部区域 (Google 风格 Flex 布局与 Typography) -->
    <header class="max-w-6xl mx-auto flex flex-col md:flex-row md:items-center md:justify-between pb-8 border-b border-google-border-light dark:border-google-border-dark gap-4">
      <div>
        <div class="flex items-center gap-3">
          <h1 class="text-3xl font-extrabold tracking-tight text-google-text-primary dark:text-google-text-darkPrimary">
            Volur Lite <span class="text-google-blue dark:text-google-blue-dark">NEXT</span>
          </h1>
          <n-tag type="info" size="small" round :bordered="false">
            v1.0 Architecture
          </n-tag>
        </div>
        <p class="mt-2 text-sm text-google-text-desc dark:text-google-text-darkDesc">
          Vue 3 + TypeScript (ES2026) + Tailwind CSS + Naive UI (Google Material Theme System)
        </p>
      </div>

      <div class="flex items-center gap-3">
        <n-button type="primary" size="medium" @click="handleActionDemo">
          体验 Naive 消息交互
        </n-button>
        <n-button size="medium" @click="goToLogin">
          体验 /v1/signin/identifier
        </n-button>
        <n-button size="medium" @click="showModal = true">
          打开模态框
        </n-button>
      </div>
    </header>

    <!-- 核心特性卡片网格 (Tailwind Grid + Naive UI Card) -->
    <main class="max-w-6xl mx-auto mt-10">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <!-- 卡片 1: Google 色彩体系 -->
        <n-card hoverable class="rounded-2xl border border-google-border-light dark:border-google-border-dark shadow-sm transition-all duration-300">
          <template #header>
            <div class="flex items-center gap-2">
              <span class="w-3 h-3 rounded-full bg-google-blue"></span>
              <span class="text-lg font-bold text-google-text-primary dark:text-google-text-darkPrimary">Google Material 色彩</span>
            </div>
          </template>
          <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
            全面对齐 Google Workspace 规范：经典 Google Blue 作为主色，辅以 Green、Yellow、Red 语义色彩与阶梯灰黑底色。
          </p>
          <div class="mt-4 flex flex-wrap gap-2">
            <span class="px-2.5 py-1 text-xs rounded-full bg-blue-50 text-google-blue dark:bg-blue-950/50 dark:text-google-blue-dark font-mono">
              Google Blue #1a73e8
            </span>
            <span class="px-2.5 py-1 text-xs rounded-full bg-green-50 text-google-green dark:bg-green-950/50 dark:text-green-300 font-mono">
              Green #34a853
            </span>
          </div>
        </n-card>

        <!-- 卡片 2: 前端路由版本控制 -->
        <n-card hoverable class="rounded-2xl border border-google-border-light dark:border-google-border-dark shadow-sm transition-all duration-300">
          <template #header>
            <div class="flex items-center gap-2">
              <span class="w-3 h-3 rounded-full bg-google-yellow"></span>
              <span class="text-lg font-bold text-google-text-primary dark:text-google-text-darkPrimary">版本化路由 (/v1/...)</span>
            </div>
          </template>
          <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
            所有业务路由强制携带版本前缀，例如登录两步阶段（`/v1/signin/identifier` 与 `/v1/signin/challenge`）。
          </p>
          <div class="mt-4 flex flex-wrap gap-2">
            <n-tag size="small" type="info" :bordered="false">/v1/signin/identifier</n-tag>
            <n-tag size="small" type="warning" :bordered="false">/v1/competitions</n-tag>
          </div>
        </n-card>

        <!-- 卡片 3: Tailwind + Naive UI 协同 -->
        <n-card hoverable class="rounded-2xl border border-google-border-light dark:border-google-border-dark shadow-sm transition-all duration-300">
          <template #header>
            <div class="flex items-center gap-2">
              <span class="w-3 h-3 rounded-full bg-google-green"></span>
              <span class="text-lg font-bold text-google-text-primary dark:text-google-text-darkPrimary">Tailwind + Naive UI</span>
            </div>
          </template>
          <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
            通过禁用 Tailwind 默认 preflight，消除样式冲刷隐患；Tailwind 负责排版，Naive UI 负责表单、表格与弹窗受控渲染。
          </p>
          <div class="mt-4 flex flex-wrap gap-2">
            <span class="px-2.5 py-1 text-xs rounded-full bg-purple-50 text-purple-700 dark:bg-purple-950/50 dark:text-purple-300 font-mono">
              no-preflight-clash
            </span>
          </div>
        </n-card>
      </div>

      <!-- 示例展示区域 -->
      <section class="mt-10 p-6 sm:p-8 rounded-3xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm transition-colors duration-300">
        <h2 class="text-xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mb-2">组件混编与交互测试</h2>
        <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc mb-6">
          下方输入框与按钮由 Naive UI 渲染，外层容器间距与网格由 Tailwind CSS 驱动，配色与动画由 Google 风格主题变量控制。
        </p>

        <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 max-w-xl">
          <n-input
            v-model:value="inputValue"
            placeholder="输入测试内容..."
            clearable
            class="flex-1 rounded-lg"
          />
          <n-button type="primary" class="rounded-full px-6 font-medium" @click="handleSend">
            发送测试
          </n-button>
        </div>
      </section>
    </main>

    <!-- 弹窗交互 -->
    <n-modal
      v-model:show="showModal"
      preset="card"
      title="Volur Lite NEXT 模态框"
      class="max-w-md rounded-2xl"
    >
      <div class="space-y-4">
        <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
          Tailwind CSS 工具类与 Naive UI 的 Modal 组件已无缝融通，Preflight 样式未对弹窗产生任何负面干扰。
        </p>
        <div class="flex justify-end gap-3 pt-2">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" @click="showModal = false">确定</n-button>
        </div>
      </div>
    </n-modal>

    <!-- 固定在页面左下角的全局控制器 (严格遵循 view-NEXT-rules.md 铁律) -->
    <aside class="fixed bottom-5 left-5 z-[1000]">
      <button
        class="flex items-center gap-2 px-3.5 py-2 text-xs font-medium rounded-full border border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard text-google-text-primary dark:text-google-text-darkPrimary shadow-sm hover:shadow transition-all"
        @click="appStore.toggleTheme"
        :title="appStore.theme === 'dark' ? '切换为亮色模式' : '切换为暗色模式'"
      >
        <span v-if="appStore.theme === 'dark'">☀️ 亮色</span>
        <span v-else>🌙 暗色</span>
      </button>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NCard, NButton, NTag, NModal, NInput } from 'naive-ui'
import { useAppStore } from '@/store/modules/app'

const router = useRouter()
const appStore = useAppStore()
const showModal = ref(false)
const inputValue = ref('')

function handleActionDemo() {
  window.$message.success('Google Material 主题适配就绪！Tailwind + Naive UI 协同运行中。')
}

function goToLogin() {
  router.push('/v1/signin/identifier')
}

function handleSend() {
  if (!inputValue.value.trim()) {
    window.$message.warning('请输入一些内容后再发送')
    return
  }
  window.$message.info(`发送成功: ${inputValue.value}`)
  inputValue.value = ''
}
</script>
