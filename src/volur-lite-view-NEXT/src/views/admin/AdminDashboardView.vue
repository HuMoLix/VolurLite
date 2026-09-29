<template>
  <div class="min-h-screen p-6 md:p-10 bg-google-surface-light dark:bg-google-surface-dark transition-colors duration-300">
    <div class="max-w-6xl mx-auto">
      <header class="flex items-center justify-between pb-6 border-b border-google-border-light dark:border-google-border-dark mb-8">
        <div>
          <h1 class="text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary">
            管理员控制台 (Admin Dashboard)
          </h1>
          <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc mt-1">
            Volur Lite NEXT 平台运营中枢与集群监控
          </p>
        </div>
        <n-button type="default" @click="goHome">
          返回主站
        </n-button>
      </header>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <n-card title="赛事管理" hoverable class="rounded-2xl border border-google-border-light dark:border-google-border-dark">
          <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc">
            赛事配置、题目下发与赛队审核中枢。
          </p>
        </n-card>
        <n-card title="集群调度" hoverable class="rounded-2xl border border-google-border-light dark:border-google-border-dark">
          <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc">
            K3s / Docker 靶机节点状态与动态端口池监控。
          </p>
        </n-card>
        <n-card title="风控与审计" hoverable class="rounded-2xl border border-google-border-light dark:border-google-border-dark">
          <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc">
            设备指纹防御、行为风险阻断与安全操作日志。
          </p>
        </n-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { NCard, NButton } from 'naive-ui'

const router = useRouter()

function goHome() {
  router.push('/competitions')
}
</script>
