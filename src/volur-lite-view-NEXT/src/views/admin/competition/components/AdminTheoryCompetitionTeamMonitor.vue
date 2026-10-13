<template>
  <div class="space-y-6">
    <div class="p-6 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark space-y-5">
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h3 class="text-base font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('admin.competitions.theory_proctor_monitor_title', '理论知识赛考场监考大屏') }}
          </h3>
          <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
            {{ t('admin.competitions.theory_proctor_monitor_desc', '实时轮询监控考生的全屏状态、整块屏幕共享流画面帧及防作弊违规累计，支持多维度检索与画面放大查验。') }}
          </p>
        </div>

        <div class="flex items-center gap-3 flex-wrap">
          <label class="flex items-center gap-2 text-xs text-google-text-secondary dark:text-google-text-darkSecondary cursor-pointer select-none">
            <input
              v-model="autoRefresh"
              type="checkbox"
              class="w-4 h-4 rounded border-gray-300 text-google-blue focus:ring-google-blue cursor-pointer"
            >
            <span>{{ t('admin.competitions.proctor_auto_refresh', '自动轮询刷新 (5秒)') }}</span>
          </label>

          <VBtn
            clz="primary"
            size="medium"
            :left-icon="RefreshOutline"
            :loading="loading"
            @click="fetchCandidates"
          >
            {{ t('common.refresh', '刷新大屏') }}
          </VBtn>
        </div>
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light dark:border-google-border-dark">
          <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted font-medium">
            {{ t('admin.competitions.proctor_total_candidates', '在册考生总数') }}
          </div>
          <div class="text-xl font-bold font-mono text-google-text-primary dark:text-google-text-darkPrimary mt-1">
            {{ candidates.length }}
          </div>
        </div>

        <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light dark:border-google-border-dark">
          <div class="text-xs text-google-green dark:text-google-green-dark font-medium">
            {{ t('admin.competitions.proctor_online_count', '在线答题中') }}
          </div>
          <div class="text-xl font-bold font-mono text-google-green dark:text-google-green-dark mt-1">
            {{ onlineCount }}
          </div>
        </div>

        <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light dark:border-google-border-dark">
          <div class="text-xs text-google-yellow dark:text-google-yellow-dark font-medium">
            {{ t('admin.competitions.proctor_warning_count', '存在违规考生') }}
          </div>
          <div class="text-xl font-bold font-mono text-google-yellow dark:text-google-yellow-dark mt-1">
            {{ warningCount }}
          </div>
        </div>

        <div class="p-4 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light dark:border-google-border-dark">
          <div class="text-xs text-google-red dark:text-google-red-dark font-medium">
            {{ t('admin.competitions.proctor_banned_count', '已封禁考生') }}
          </div>
          <div class="text-xl font-bold font-mono text-google-red dark:text-google-red-dark mt-1">
            {{ bannedCount }}
          </div>
        </div>
      </div>

      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-2">
        <div class="flex items-center gap-2 max-w-sm w-full">
          <VInput
            v-model:value="searchKeyword"
            placeholder="按考生姓名/用户名/战队名称过滤..."
            clearable
          />
        </div>

        <div class="flex items-center gap-2">
          <button
            class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
            :class="[
              filterMode === 'ALL'
                ? 'bg-google-blue text-white'
                : 'bg-gray-100 dark:bg-gray-800 text-google-text-secondary dark:text-google-text-darkSecondary'
            ]"
            @click="filterMode = 'ALL'"
          >
            全部 ({{ candidates.length }})
          </button>
          <button
            class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
            :class="[
              filterMode === 'ONLINE'
                ? 'bg-google-blue text-white'
                : 'bg-gray-100 dark:bg-gray-800 text-google-text-secondary dark:text-google-text-darkSecondary'
            ]"
            @click="filterMode = 'ONLINE'"
          >
            仅在线 ({{ onlineCount }})
          </button>
          <button
            class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
            :class="[
              filterMode === 'VIOLATION'
                ? 'bg-google-blue text-white'
                : 'bg-gray-100 dark:bg-gray-800 text-google-text-secondary dark:text-google-text-darkSecondary'
            ]"
            @click="filterMode = 'VIOLATION'"
          >
            有违规 ({{ warningCount }})
          </button>
          <button
            class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
            :class="[
              filterMode === 'BANNED'
                ? 'bg-google-blue text-white'
                : 'bg-gray-100 dark:bg-gray-800 text-google-text-secondary dark:text-google-text-darkSecondary'
            ]"
            @click="filterMode = 'BANNED'"
          >
            已封禁 ({{ bannedCount }})
          </button>
        </div>
      </div>
    </div>

    <div v-if="filteredCandidates.length === 0" class="p-12 text-center rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark">
      <div class="text-sm text-google-text-muted dark:text-google-text-darkMuted">
        {{ searchKeyword ? '没有符合过滤条件的考生' : '暂无考生进入考场或上报监考心跳' }}
      </div>
    </div>

    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <div
        v-for="candidate in filteredCandidates"
        :key="candidate.userId"
        class="rounded-2xl bg-white dark:bg-[#292a2d] border transition-shadow hover:shadow-md flex flex-col overflow-hidden"
        :class="[
          candidate.isBanned
            ? 'border-red-300 dark:border-red-900/60'
            : candidate.violationCount > 0
              ? 'border-amber-300 dark:border-amber-900/60'
              : 'border-google-border-light dark:border-google-border-dark'
        ]"
      >
        <div class="p-3.5 border-b border-google-border-light dark:border-google-border-dark flex items-center justify-between gap-2">
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-1.5">
              <span
                class="w-2 h-2 rounded-full flex-shrink-0"
                :class="candidate.isOnline ? 'bg-google-green dark:bg-google-green-dark animate-pulse' : 'bg-gray-400'"
              />
              <span class="text-xs font-bold text-google-text-primary dark:text-google-text-darkPrimary truncate">
                {{ candidate.realName || candidate.username }}
              </span>
              <span v-if="candidate.realName" class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted truncate">
                ({{ candidate.username }})
              </span>
            </div>
            <div class="text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary truncate mt-0.5">
              {{ candidate.teamName || '个人参赛' }}
            </div>
          </div>

          <span
            v-if="candidate.isBanned"
            class="px-2 py-0.5 rounded text-[10px] font-bold bg-red-100 text-google-red dark:bg-red-950 dark:text-google-red-dark flex-shrink-0"
          >
            已封禁
          </span>
          <span
            v-else-if="candidate.violationCount > 0"
            class="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300 flex-shrink-0"
          >
            违规: {{ candidate.violationCount }}
          </span>
          <span
            v-else
            class="px-2 py-0.5 rounded text-[10px] font-medium bg-emerald-100 text-google-green dark:bg-emerald-950 dark:text-google-green-dark flex-shrink-0"
          >
            正常
          </span>
        </div>

        <div
          class="relative aspect-video bg-black/95 flex items-center justify-center overflow-hidden cursor-pointer group"
          @click="openPreview(candidate)"
        >
          <img
            v-if="candidate.screenFrame"
            :src="candidate.screenFrame"
            alt="Candidate Screen Preview"
            class="w-full h-full object-contain"
          >
          <div
            v-else
            class="flex flex-col items-center justify-center text-gray-500 text-xs gap-1.5 p-4"
          >
            <n-icon :size="24" :component="DesktopOutline" />
            <span>{{ candidate.isOnline ? '正在等待屏幕画面帧...' : '离线 (未接收监控帧)' }}</span>
          </div>

          <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-1.5 text-white text-xs font-medium">
            <n-icon :size="16" :component="EyeOutline" />
            <span>点击查看大图</span>
          </div>

          <div class="absolute bottom-1.5 right-1.5 px-1.5 py-0.5 rounded bg-black/70 text-[10px] text-white font-mono">
            {{ formatLastSeen(candidate.lastHeartbeatTime) }}
          </div>
        </div>

        <div class="p-3.5 space-y-2.5 flex-1 flex flex-col justify-between bg-google-surface-light/30 dark:bg-google-surface-dark/30">
          <div class="flex items-center justify-between text-[11px]">
            <span class="text-google-text-secondary dark:text-google-text-darkSecondary">
              全屏监控:
              <strong :class="candidate.fullscreen ? 'text-google-green' : 'text-google-red'">
                {{ candidate.fullscreen ? '处于全屏' : '非全屏' }}
              </strong>
            </span>

            <span class="text-google-text-secondary dark:text-google-text-darkSecondary">
              屏幕流:
              <strong :class="candidate.screenActive ? 'text-google-green' : 'text-google-red'">
                {{ candidate.screenActive ? '活跃' : '已断开' }}
              </strong>
            </span>
          </div>

          <div class="space-y-1">
            <div class="flex items-center justify-between text-[11px] text-google-text-secondary dark:text-google-text-darkSecondary">
              <span>答题进度</span>
              <span class="font-mono font-medium">{{ candidate.answeredCount }} / {{ candidate.totalCount }}</span>
            </div>
            <div class="w-full bg-gray-200 dark:bg-gray-700 h-1.5 rounded-full overflow-hidden">
              <div
                class="bg-google-blue h-full rounded-full transition-all duration-300"
                :style="{ width: getProgressPercent(candidate.answeredCount, candidate.totalCount) }"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <VModal
      v-model:show="previewModalVisible"
      :title="`考生实时屏幕查验 - ${selectedCandidate?.realName || selectedCandidate?.username || ''}`"
      :width="900"
    >
      <div v-if="selectedCandidate" class="space-y-4">
        <div class="flex items-center justify-between flex-wrap gap-2 text-xs text-google-text-secondary dark:text-google-text-darkSecondary pb-2 border-b border-google-border-light dark:border-google-border-dark">
          <div>
            考生姓名: <strong class="text-google-text-primary dark:text-google-text-darkPrimary">{{ selectedCandidate.realName || selectedCandidate.username }}</strong>
            <span class="ml-3">战队: <strong>{{ selectedCandidate.teamName || '个人参赛' }}</strong></span>
          </div>
          <div>
            状态:
            <span :class="selectedCandidate.isOnline ? 'text-google-green font-bold' : 'text-gray-400'">
              {{ selectedCandidate.isOnline ? '在线答题中' : '离线' }}
            </span>
            <span class="ml-3">
              违规累计: <strong :class="selectedCandidate.violationCount > 0 ? 'text-google-red font-bold' : ''">{{ selectedCandidate.violationCount }} 次</strong>
            </span>
            <span class="ml-3 font-mono">
              最后上报: {{ formatLastSeen(selectedCandidate.lastHeartbeatTime) }}
            </span>
          </div>
        </div>

        <div class="aspect-video bg-black rounded-xl overflow-hidden flex items-center justify-center border border-google-border-light dark:border-google-border-dark">
          <img
            v-if="selectedCandidate.screenFrame"
            :src="selectedCandidate.screenFrame"
            alt="Candidate Screen Large Preview"
            class="w-full h-full object-contain"
          >
          <div v-else class="text-gray-400 text-xs">
            该考生暂无屏幕帧画面
          </div>
        </div>
      </div>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, useMessage } from 'naive-ui'
import {
  RefreshOutline,
  DesktopOutline,
  EyeOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VModal from '@/components/common/VModal.vue'
import { getAdminTheoryCandidateMonitorsApi } from '@/api/competition'
import type { AdminTheoryCandidateMonitorVO } from '@/types/competition'

defineOptions({
  name: 'AdminTheoryCompetitionTeamMonitor'
})

const props = defineProps<{
  competitionId?: number | string
}>()

const { t } = useI18n()
const message = useMessage()

const loading = ref<boolean>(false)
const autoRefresh = ref<boolean>(true)
const candidates = ref<AdminTheoryCandidateMonitorVO[]>([])
const searchKeyword = ref<string>('')
const filterMode = ref<'ALL' | 'ONLINE' | 'VIOLATION' | 'BANNED'>('ALL')

const previewModalVisible = ref<boolean>(false)
const selectedCandidate = ref<AdminTheoryCandidateMonitorVO | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

const onlineCount = computed(() => {
  return candidates.value.filter((c) => c.isOnline).length
})

const warningCount = computed(() => {
  return candidates.value.filter((c) => c.violationCount > 0 && !c.isBanned).length
})

const bannedCount = computed(() => {
  return candidates.value.filter((c) => c.isBanned).length
})

const filteredCandidates = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  return candidates.value.filter((c) => {
    if (filterMode.value === 'ONLINE' && !c.isOnline) return false
    if (filterMode.value === 'VIOLATION' && c.violationCount <= 0) return false
    if (filterMode.value === 'BANNED' && !c.isBanned) return false

    if (!kw) return true
    const uname = (c.username || '').toLowerCase()
    const rname = (c.realName || '').toLowerCase()
    const tname = (c.teamName || '').toLowerCase()
    return uname.includes(kw) || rname.includes(kw) || tname.includes(kw)
  })
})

function getProgressPercent(answered: number, total: number): string {
  if (!total || total <= 0) return '0%'
  const p = Math.min(100, Math.max(0, Math.round((answered / total) * 100)))
  return `${p}%`
}

function formatLastSeen(timestamp?: number): string {
  if (!timestamp) return '未上报'
  const diff = Date.now() - timestamp
  if (diff < 10000) return '刚刚'
  const sec = Math.floor(diff / 1000)
  if (sec < 60) return `${sec} 秒前`
  const min = Math.floor(sec / 60)
  return `${min} 分钟前`
}

function openPreview(candidate: AdminTheoryCandidateMonitorVO) {
  selectedCandidate.value = candidate
  previewModalVisible.value = true
}

async function fetchCandidates() {
  if (!props.competitionId) return
  loading.value = true
  try {
    const res = await getAdminTheoryCandidateMonitorsApi(props.competitionId)
    candidates.value = res?.data || []
  } catch (err: any) {
    if (message) {
      message.error(err?.response?.data?.message || err?.message || '拉取监考数据失败')
    }
  } finally {
    loading.value = false
  }
}

watch(autoRefresh, (enabled) => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (enabled) {
    pollTimer = setInterval(() => {
      fetchCandidates()
    }, 5000)
  }
})

watch(() => props.competitionId, (newId) => {
  if (newId) {
    fetchCandidates()
  }
})

onMounted(() => {
  fetchCandidates()
  if (autoRefresh.value) {
    pollTimer = setInterval(() => {
      fetchCandidates()
    }, 5000)
  }
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>
