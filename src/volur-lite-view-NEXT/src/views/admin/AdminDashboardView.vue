<template>
  <div class="admin-dashboard-view flex flex-col gap-6">
    <div class="welcome-banner p-6 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex flex-col md:flex-row items-start md:items-center justify-between gap-4 transition-colors">
      <div>
        <h2 class="text-xl font-bold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('admin.dashboard.title', '工作台') }}
        </h2>
        <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc mt-1">
          {{ t('admin.dashboard.desc', 'Volur Lite 平台运营中枢与底层环境概览') }}
        </p>
      </div>

      <div class="flex items-center gap-3">
        <VBtn
          clz="normal"
          size="medium"
          :left-icon="SettingsOutline"
          @click="router.push('/admin/system')"
        >
          {{ t('admin.dashboard.quick_system', '后台设置') }}
        </VBtn>
        <VBtn
          clz="primary"
          size="medium"
          :left-icon="PeopleOutline"
          @click="router.push('/admin/users')"
        >
          {{ t('admin.dashboard.quick_users', '用户管理') }}
        </VBtn>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="stat-card p-5 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-4 transition-colors">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark shrink-0">
          <n-icon size="24">
            <PeopleOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.dashboard.stat_users', '注册用户') }}
          </div>
          <div class="text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">
            {{ userStats.totalUsers }}
          </div>
        </div>
      </div>

      <div class="stat-card p-5 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-4 transition-colors">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center bg-green-50 dark:bg-green-900/30 text-google-green dark:text-google-green-dark shrink-0">
          <n-icon size="24">
            <CheckmarkCircleOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.dashboard.stat_active_users', '正常启用') }}
          </div>
          <div class="text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">
            {{ userStats.activeCount }}
          </div>
        </div>
      </div>

      <div class="stat-card p-5 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-4 transition-colors">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center bg-amber-50 dark:bg-amber-900/30 text-google-yellow dark:text-google-yellow-dark shrink-0">
          <n-icon size="24">
            <HardwareChipOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.dashboard.stat_redis_latency', 'Redis 响应延迟') }}
          </div>
          <div class="text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">
            {{ overview.redisLatencyMs }}ms
          </div>
        </div>
      </div>

      <div class="stat-card p-5 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex items-center gap-4 transition-colors">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center bg-cyan-50 dark:bg-cyan-900/30 text-cyan-600 dark:text-cyan-400 shrink-0">
          <n-icon size="24">
            <RadioOutline />
          </n-icon>
        </div>
        <div class="min-w-0 flex-1">
          <div class="text-xs font-medium text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.system.overview.sse_status', '实时 SSE 总线') }}
          </div>
          <div class="text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">
            {{ overview.sseStatus === 'ONLINE' ? 'ONLINE' : 'OFFLINE' }}
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="p-6 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex flex-col justify-between gap-4 transition-colors">
        <div>
          <div class="w-10 h-10 rounded-xl flex items-center justify-center bg-blue-50 dark:bg-blue-900/30 text-google-blue dark:text-google-blue-dark mb-4">
            <n-icon size="20">
              <SettingsOutline />
            </n-icon>
          </div>
          <h3 class="text-base font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('admin.dashboard.quick_system', '后台设置') }}
          </h3>
          <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc mt-1">
            {{ t('admin.dashboard.quick_system_desc', '配置系统基础参数、发信服务与维护模式') }}
          </p>
        </div>
        <VBtn
          clz="normal"
          size="medium"
          block
          @click="router.push('/admin/system')"
        >
          {{ t('admin.dashboard.quick_system', '前往设置') }}
        </VBtn>
      </div>

      <div class="p-6 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex flex-col justify-between gap-4 transition-colors">
        <div>
          <div class="w-10 h-10 rounded-xl flex items-center justify-center bg-green-50 dark:bg-green-900/30 text-google-green dark:text-google-green-dark mb-4">
            <n-icon size="20">
              <PeopleOutline />
            </n-icon>
          </div>
          <h3 class="text-base font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('admin.dashboard.quick_users', '用户管理') }}
          </h3>
          <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc mt-1">
            {{ t('admin.dashboard.quick_users_desc', '检索、编辑、分配角色及管控全站用户') }}
          </p>
        </div>
        <VBtn
          clz="normal"
          size="medium"
          block
          @click="router.push('/admin/users')"
        >
          {{ t('admin.dashboard.quick_users', '用户列表') }}
        </VBtn>
      </div>

      <div class="p-6 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark flex flex-col justify-between gap-4 transition-colors">
        <div>
          <div class="w-10 h-10 rounded-xl flex items-center justify-center bg-purple-50 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 mb-4">
            <n-icon size="20">
              <TrophyOutline />
            </n-icon>
          </div>
          <h3 class="text-base font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('admin.dashboard.quick_competitions', '赛事大厅') }}
          </h3>
          <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc mt-1">
            {{ t('admin.dashboard.quick_competitions_desc', '前往公开赛事大厅与赛题演练') }}
          </p>
        </div>
        <VBtn
          clz="normal"
          size="medium"
          block
          @click="router.push('/competitions')"
        >
          {{ t('admin.dashboard.quick_competitions', '前台大厅') }}
        </VBtn>
      </div>
    </div>

    <div class="p-6 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark transition-colors">
      <h3 class="text-base font-semibold text-google-text-primary dark:text-google-text-darkPrimary mb-4">
        {{ t('admin.dashboard.system_info', '系统运行环境') }}
      </h3>

      <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
        <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-darkElevated">
          <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.system.overview.kernel_version', '系统内核版本') }}
          </div>
          <div class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary mt-1">
            {{ overview.kernelVersion || 'Volur Lite v1.0.0-SNAPSHOT' }}
          </div>
        </div>

        <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-darkElevated">
          <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.system.overview.runtime_env', '运行底层环境') }}
          </div>
          <div class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary mt-1">
            {{ overview.runtimeEnv || 'Spring Boot 3.4.3 / JDK 25' }}
          </div>
        </div>

        <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-darkElevated">
          <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.system.overview.redis_status', 'Redis 缓存状态') }}
          </div>
          <div class="text-sm font-semibold text-google-green dark:text-google-green-dark mt-1">
            {{ overview.redisStatus === 'CONNECTED' ? 'CONNECTED' : 'DISCONNECTED' }}
          </div>
        </div>

        <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-darkElevated">
          <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.system.overview.sse_status', '实时推送总线') }}
          </div>
          <div class="text-sm font-semibold text-google-blue dark:text-google-blue-dark mt-1">
            {{ overview.sseStatus === 'ONLINE' ? 'ONLINE' : 'OFFLINE' }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import {
  PeopleOutline,
  CheckmarkCircleOutline,
  HardwareChipOutline,
  RadioOutline,
  SettingsOutline,
  TrophyOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import { getAdminSystemOverviewApi } from '@/api/system'
import { getAdminUserStatisticsApi } from '@/api/user'
import type { AdminSystemOverviewVO } from '@/types/system'
import type { AdminUserStatisticsVO } from '@/types/user'

defineOptions({
  name: 'AdminDashboardView'
})

const router = useRouter()
const { t } = useI18n()

const overview = reactive<AdminSystemOverviewVO>({
  kernelVersion: 'Volur Lite v1.0.0-SNAPSHOT',
  springBootVersion: '3.4.3',
  javaVersion: 'JDK 25',
  runtimeEnv: 'Spring Boot 3.4.3 / JDK 25',
  osInfo: '',
  redisStatus: 'CONNECTED',
  redisLatencyMs: 0,
  sseStatus: 'ONLINE',
  uptimeSeconds: 0,
  heapMemoryUsedMb: 0,
  heapMemoryMaxMb: 0
})

const userStats = reactive<AdminUserStatisticsVO>({
  totalUsers: 0,
  adminCount: 0,
  bannedCount: 0,
  activeCount: 0
})

async function fetchOverview() {
  try {
    const res = await getAdminSystemOverviewApi()
    if (res.data) {
      Object.assign(overview, res.data)
    }
  } catch {}
}

async function fetchUserStats() {
  try {
    const res = await getAdminUserStatisticsApi()
    if (res.data) {
      Object.assign(userStats, res.data)
    }
  } catch {}
}

onMounted(() => {
  fetchOverview()
  fetchUserStats()
})
</script>
