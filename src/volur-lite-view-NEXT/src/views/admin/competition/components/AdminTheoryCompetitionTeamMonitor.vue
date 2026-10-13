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

        <VFilterBtnGroup
          v-model="filterMode"
          :options="filterOptions"
        />
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

          <div class="pt-2 flex items-center gap-2 border-t border-google-border-light dark:border-google-border-dark">
            <VBtn
              clz="normal"
              size="small"
              block
              :left-icon="EyeOutline"
              @click.stop="openPreview(candidate, 'SNAPSHOT')"
            >
              快照查验
            </VBtn>
            <VBtn
              clz="primary"
              size="small"
              block
              :left-icon="VideocamOutline"
              :disabled="!candidate.isOnline || !candidate.screenActive"
              @click.stop="openPreview(candidate, 'LIVE_RTC')"
            >
              实时巡视
            </VBtn>
          </div>
        </div>
      </div>
    </div>

    <VModal
      v-model:show="previewModalVisible"
      :title="`考生考场实时查验 - ${selectedCandidate?.realName || selectedCandidate?.username || ''}`"
      :width="920"
      @update:show="handleModalVisibilityChange"
    >
      <div v-if="selectedCandidate" class="space-y-4">
        <div class="flex items-center justify-between flex-wrap gap-2 text-xs text-google-text-secondary dark:text-google-text-darkSecondary pb-2 border-b border-google-border-light dark:border-google-border-dark">
          <div>
            考生姓名: <strong class="text-google-text-primary dark:text-google-text-darkPrimary">{{ selectedCandidate.realName || selectedCandidate.username }}</strong>
            <span class="ml-3">战队: <strong>{{ selectedCandidate.teamName || '个人参赛' }}</strong></span>
          </div>
          <div class="flex items-center gap-3">
            <span>
              状态:
              <strong :class="selectedCandidate.isOnline ? 'text-google-green' : 'text-gray-400'">
                {{ selectedCandidate.isOnline ? '在线答题中' : '离线' }}
              </strong>
            </span>
            <span>
              违规累计: <strong :class="selectedCandidate.violationCount > 0 ? 'text-google-red' : ''">{{ selectedCandidate.violationCount }} 次</strong>
            </span>
          </div>
        </div>

        <div class="flex items-center justify-between flex-wrap gap-2">
          <VFilterBtnGroup
            v-model="inspectMode"
            :options="inspectModeOptions"
            size="small"
            @change="handleInspectModeChange"
          />

          <div v-if="inspectMode === 'SNAPSHOT'" class="flex items-center gap-2">
            <span v-if="selectedCandidate.highResTimestamp" class="text-xs text-google-text-muted dark:text-google-text-darkMuted font-mono">
              高清帧更新于: {{ formatLastSeen(selectedCandidate.highResTimestamp) }}
            </span>
            <VBtn
              clz="primary"
              size="small"
              :left-icon="SparklesOutline"
              :loading="requestingSnapshot"
              :disabled="!selectedCandidate.isOnline"
              @click="handleRequestHighResSnapshot"
            >
              请求最新 1080P 超清快照
            </VBtn>
          </div>

          <div v-else-if="inspectMode === 'LIVE_RTC'" class="flex items-center gap-2">
            <span
              class="px-2 py-0.5 rounded text-[11px] font-medium flex items-center gap-1.5"
              :class="[
                rtcConnectionState === 'CONNECTED'
                  ? 'bg-emerald-100 text-google-green dark:bg-emerald-950 dark:text-google-green-dark'
                  : rtcConnectionState === 'CALLING' || rtcConnectionState === 'CONNECTING'
                    ? 'bg-blue-100 text-google-blue dark:bg-blue-950 dark:text-google-blue-dark animate-pulse'
                    : rtcConnectionState === 'FAILED'
                      ? 'bg-red-100 text-google-red dark:bg-red-950 dark:text-google-red-dark'
                      : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'
              ]"
            >
              <span
                class="w-1.5 h-1.5 rounded-full"
                :class="rtcConnectionState === 'CONNECTED' ? 'bg-google-green animate-ping' : 'bg-current'"
              />
              {{ rtcStatusText }}
            </span>

            <VBtn
              v-if="rtcConnectionState === 'CONNECTED' || rtcConnectionState === 'CALLING' || rtcConnectionState === 'CONNECTING'"
              clz="danger"
              size="small"
              :left-icon="CloseCircleOutline"
              @click="stopRtcInspection"
            >
              断开实时巡视
            </VBtn>
            <VBtn
              v-else
              clz="primary"
              size="small"
              :left-icon="VideocamOutline"
              :disabled="!selectedCandidate.isOnline || !selectedCandidate.screenActive"
              @click="startRtcInspection"
            >
              重新发起连线
            </VBtn>
          </div>
        </div>

        <div v-if="inspectMode === 'SNAPSHOT'" class="aspect-video bg-black rounded-xl overflow-hidden flex items-center justify-center border border-google-border-light dark:border-google-border-dark relative group">
          <img
            v-if="currentDisplayFrame"
            :src="currentDisplayFrame"
            alt="Candidate Screen Large Preview"
            class="w-full h-full object-contain"
          >
          <div v-else class="text-gray-400 text-xs">
            该考生暂无屏幕帧画面
          </div>

          <div
            v-if="selectedCandidate.highResFrame"
            class="absolute top-2 left-2 px-2 py-0.5 rounded bg-black/75 text-[10px] text-amber-300 font-mono flex items-center gap-1"
          >
            <n-icon :size="12" :component="SparklesOutline" />
            <span>1080P 超清快照</span>
          </div>
          <div
            v-else-if="selectedCandidate.screenFrame"
            class="absolute top-2 left-2 px-2 py-0.5 rounded bg-black/75 text-[10px] text-gray-300 font-mono"
          >
            320P 常规缩略帧
          </div>
        </div>

        <div v-else class="aspect-video bg-black rounded-xl overflow-hidden flex items-center justify-center border border-google-border-light dark:border-google-border-dark relative">
          <img
            v-if="currentDisplayFrame"
            :src="currentDisplayFrame"
            alt="Candidate Screen Fallback"
            class="absolute inset-0 w-full h-full object-contain pointer-events-none select-none transition-opacity duration-300"
            :class="rtcConnectionState === 'CONNECTED' ? 'opacity-0' : 'opacity-80'"
          >

          <video
            ref="rtcVideoRef"
            autoplay
            playsinline
            muted
            class="w-full h-full object-contain relative z-10 transition-opacity duration-300"
            :class="rtcConnectionState === 'CONNECTED' ? 'opacity-100' : 'opacity-0'"
          />

          <div
            v-if="rtcConnectionState !== 'CONNECTED'"
            class="absolute inset-0 z-20 bg-black/60 backdrop-blur-[2px] flex flex-col items-center justify-center text-white text-xs gap-3 p-6 text-center"
          >
            <n-spin v-if="rtcConnectionState === 'CALLING' || rtcConnectionState === 'CONNECTING'" size="medium" />
            <n-icon
              v-else-if="rtcConnectionState === 'FAILED'"
              :size="36"
              :component="AlertCircleOutline"
              class="text-google-red"
            />
            <n-icon
              v-else
              :size="36"
              :component="VideocamOutline"
              class="text-gray-400"
            />

            <div class="space-y-1 max-w-md">
              <div class="font-medium text-sm text-white">
                {{ rtcStatusText }}
              </div>
              <div v-if="rtcSubStatusText" class="text-[11px] text-gray-300">
                {{ rtcSubStatusText }}
              </div>
            </div>

            <div class="flex items-center gap-2 mt-1">
              <VBtn
                v-if="rtcConnectionState === 'FAILED' || rtcConnectionState === 'CLOSED'"
                clz="primary"
                size="small"
                :left-icon="VideocamOutline"
                :disabled="!selectedCandidate.isOnline || !selectedCandidate.screenActive"
                @click="startRtcInspection"
              >
                重新发起连线
              </VBtn>
              <VBtn
                clz="normal"
                size="small"
                :left-icon="EyeOutline"
                @click="handleInspectModeChange('SNAPSHOT')"
              >
                查看静态快照
              </VBtn>
            </div>
          </div>

          <div
            v-if="rtcConnectionState === 'CONNECTED'"
            class="absolute bottom-2 right-2 z-20 px-2 py-0.5 rounded bg-black/75 text-[10px] text-google-green font-mono flex items-center gap-1"
          >
            <span>WebRTC P2P (30 FPS)</span>
          </div>
        </div>
      </div>
    </VModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, NSpin, useMessage } from 'naive-ui'
import {
  RefreshOutline,
  DesktopOutline,
  EyeOutline,
  VideocamOutline,
  SparklesOutline,
  CloseCircleOutline,
  AlertCircleOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VInput from '@/components/common/VInput.vue'
import VModal from '@/components/common/VModal.vue'
import VFilterBtnGroup from '@/components/common/VFilterBtnGroup.vue'
import type { FilterOption } from '@/components/common/types'
import {
  getAdminTheoryCandidateMonitorsApi,
  requestCandidateHighResSnapshotApi,
  sendAdminTheoryProctorRtcSignalApi
} from '@/api/competition'
import type { AdminTheoryCandidateMonitorVO, TheoryProctorRtcSignalDTO } from '@/types/competition'
import { getGlobalSseClient } from '@/utils/sse'
import { getDefaultRtcConfiguration } from '@/utils/webrtc'

defineOptions({
  name: 'AdminTheoryCompetitionTeamMonitor'
})

const props = defineProps<{
  competitionId?: number | string
}>()

const { t } = useI18n()
const message = useMessage()

type FilterMode = 'ALL' | 'ONLINE' | 'VIOLATION' | 'BANNED'
type InspectMode = 'SNAPSHOT' | 'LIVE_RTC'
type RtcConnectionState = 'IDLE' | 'CALLING' | 'CONNECTING' | 'CONNECTED' | 'FAILED' | 'CLOSED'

const loading = ref<boolean>(false)
const autoRefresh = ref<boolean>(true)
const candidates = ref<AdminTheoryCandidateMonitorVO[]>([])
const searchKeyword = ref<string>('')
const filterMode = ref<FilterMode>('ALL')

const previewModalVisible = ref<boolean>(false)
const selectedCandidate = ref<AdminTheoryCandidateMonitorVO | null>(null)
const inspectMode = ref<InspectMode>('SNAPSHOT')
const requestingSnapshot = ref<boolean>(false)

const rtcVideoRef = ref<HTMLVideoElement | null>(null)
const rtcConnectionState = ref<RtcConnectionState>('IDLE')
let rtcPeerConnection: RTCPeerConnection | null = null
let currentSessionId = ''
let currentRemoteStream: MediaStream | null = null
let adminPendingIceCandidates: RTCIceCandidateInit[] = []

let pollTimer: ReturnType<typeof setInterval> | null = null
let unsubSnapshotUpdated: (() => void) | null = null
let unsubRtcSignal: (() => void) | null = null

const onlineCount = computed(() => {
  return candidates.value.filter((c) => c.isOnline).length
})

const warningCount = computed(() => {
  return candidates.value.filter((c) => c.violationCount > 0 && !c.isBanned).length
})

const bannedCount = computed(() => {
  return candidates.value.filter((c) => c.isBanned).length
})

const filterOptions = computed<FilterOption<FilterMode>[]>(() => [
  {
    value: 'ALL',
    label: t('common.all', '全部'),
    count: candidates.value.length
  },
  {
    value: 'ONLINE',
    label: t('admin.competitions.proctor_filter_online', '仅在线'),
    count: onlineCount.value
  },
  {
    value: 'VIOLATION',
    label: t('admin.competitions.proctor_filter_violation', '有违规'),
    count: warningCount.value
  },
  {
    value: 'BANNED',
    label: t('admin.competitions.proctor_filter_banned', '已封禁'),
    count: bannedCount.value
  }
])

const inspectModeOptions = computed<FilterOption<InspectMode>[]>(() => [
  {
    value: 'SNAPSHOT',
    label: '高清快照查验'
  },
  {
    value: 'LIVE_RTC',
    label: 'WebRTC 实时巡视 (P2P)'
  }
])

const currentDisplayFrame = computed(() => {
  if (!selectedCandidate.value) return ''
  return selectedCandidate.value.highResFrame || selectedCandidate.value.screenFrame || ''
})

const rtcStatusText = computed(() => {
  switch (rtcConnectionState.value) {
    case 'CALLING':
      return '正在呼叫考生端并请求屏幕视频流...'
    case 'CONNECTING':
      return 'STUN/ICE 打洞与 P2P 协商中...'
    case 'CONNECTED':
      return 'WebRTC P2P 已建立，实时桌面监看中'
    case 'FAILED':
      return 'P2P 连线失败或被中断'
    case 'CLOSED':
      return '巡视已结束'
    default:
      return '未发起实时连线'
  }
})

const rtcSubStatusText = computed(() => {
  switch (rtcConnectionState.value) {
    case 'CALLING':
      return '已向考生端发送呼叫指令，正在等待考生端信令接入'
    case 'CONNECTING':
      return '正在通过公网 STUN 节点探测 NAT 穿透地址并交换 SDP'
    case 'FAILED':
      return '由于网络 NAT 限制、防火墙拦截或考生流异常导致 P2P 无法建立。当前呈现考生最新屏幕快照，可重试或切换至静态快照查验'
    case 'CLOSED':
      return '实时巡视已结束，当前呈现考生最新屏幕快照'
    default:
      return ''
  }
})

function bindRemoteStreamToVideo() {
  nextTick(() => {
    if (rtcVideoRef.value && currentRemoteStream) {
      if (rtcVideoRef.value.srcObject !== currentRemoteStream) {
        rtcVideoRef.value.srcObject = currentRemoteStream
      }
      rtcVideoRef.value.play().catch(() => {})
    }
  })
}

watch(rtcVideoRef, (el) => {
  if (el && currentRemoteStream) {
    el.srcObject = currentRemoteStream
    el.play().catch(() => {})
  }
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

function openPreview(candidate: AdminTheoryCandidateMonitorVO, mode: InspectMode = 'SNAPSHOT') {
  selectedCandidate.value = candidate
  inspectMode.value = mode
  previewModalVisible.value = true
  if (mode === 'LIVE_RTC') {
    startRtcInspection()
  }
}

function handleInspectModeChange(mode: InspectMode) {
  inspectMode.value = mode
  if (mode === 'LIVE_RTC') {
    startRtcInspection()
  } else {
    stopRtcInspection()
  }
}

function handleModalVisibilityChange(visible: boolean) {
  if (!visible) {
    stopRtcInspection()
    selectedCandidate.value = null
  }
}

async function handleRequestHighResSnapshot() {
  if (!props.competitionId || !selectedCandidate.value?.userId) return
  requestingSnapshot.value = true
  try {
    await requestCandidateHighResSnapshotApi(props.competitionId, selectedCandidate.value.userId)
    message.success('已向考生端发送高清抓拍指令，请稍候')
  } catch (err: any) {
    requestingSnapshot.value = false
    message.error(err?.response?.data?.message || err?.message || '发起高清快照请求失败')
  }
}

async function startRtcInspection() {
  if (!props.competitionId || !selectedCandidate.value) return
  if (!selectedCandidate.value.isOnline || !selectedCandidate.value.screenActive) {
    message.warning('该考生当前未在线或屏幕共享流已断开')
    return
  }

  stopRtcInspection()

  currentSessionId = `${props.competitionId}_${selectedCandidate.value.userId}_${Date.now()}`
  rtcConnectionState.value = 'CALLING'

  try {
    await sendAdminTheoryProctorRtcSignalApi(props.competitionId, {
      targetUserId: selectedCandidate.value.userId,
      type: 'CALL',
      sessionId: currentSessionId
    })
  } catch (err: any) {
    rtcConnectionState.value = 'FAILED'
    message.error(err?.response?.data?.message || err?.message || '发起实时呼叫信令失败')
  }
}

function stopRtcInspection() {
  if (props.competitionId && selectedCandidate.value && currentSessionId) {
    sendAdminTheoryProctorRtcSignalApi(props.competitionId, {
      targetUserId: selectedCandidate.value.userId,
      type: 'HANGUP',
      sessionId: currentSessionId
    }).catch(() => {})
  }

  adminPendingIceCandidates = []
  if (rtcPeerConnection) {
    rtcPeerConnection.close()
    rtcPeerConnection = null
  }
  currentRemoteStream = null
  if (rtcVideoRef.value) {
    rtcVideoRef.value.srcObject = null
  }
  rtcConnectionState.value = 'IDLE'
  currentSessionId = ''
}

async function handleIncomingAdminRtcSignal(payload: any) {
  if (!payload || !payload.signal || !selectedCandidate.value) return
  if (String(payload.senderUserId) !== String(selectedCandidate.value.userId)) return

  const signal: TheoryProctorRtcSignalDTO = payload.signal

  if (signal.type === 'OFFER' && signal.sdp) {
    rtcConnectionState.value = 'CONNECTING'
    adminPendingIceCandidates = []
    currentRemoteStream = null

    if (rtcPeerConnection) {
      rtcPeerConnection.close()
      rtcPeerConnection = null
    }

    const pc = new RTCPeerConnection(getDefaultRtcConfiguration())
    rtcPeerConnection = pc

    pc.ontrack = (event) => {
      const stream = event.streams && event.streams[0] ? event.streams[0] : new MediaStream([event.track])
      currentRemoteStream = stream
      bindRemoteStreamToVideo()
      rtcConnectionState.value = 'CONNECTED'

      event.track.onended = () => {
        if (rtcConnectionState.value === 'CONNECTED') {
          rtcConnectionState.value = 'FAILED'
        }
      }
    }

    pc.onicecandidate = (event) => {
      if (event.candidate && props.competitionId && selectedCandidate.value) {
        sendAdminTheoryProctorRtcSignalApi(props.competitionId, {
          targetUserId: selectedCandidate.value.userId,
          type: 'ICE',
          candidate: JSON.stringify(event.candidate.toJSON()),
          sessionId: signal.sessionId
        }).catch(() => {})
      }
    }

    pc.onconnectionstatechange = () => {
      if (pc.connectionState === 'connected') {
        rtcConnectionState.value = 'CONNECTED'
      } else if (pc.connectionState === 'failed' || pc.connectionState === 'disconnected') {
        rtcConnectionState.value = 'FAILED'
      } else if (pc.connectionState === 'closed') {
        rtcConnectionState.value = 'CLOSED'
      }
    }

    pc.oniceconnectionstatechange = () => {
      if (pc.iceConnectionState === 'failed') {
        rtcConnectionState.value = 'FAILED'
      }
    }

    await pc.setRemoteDescription(new RTCSessionDescription({ type: 'offer', sdp: signal.sdp }))

    while (adminPendingIceCandidates.length > 0) {
      const cand = adminPendingIceCandidates.shift()
      if (cand) {
        await pc.addIceCandidate(new RTCIceCandidate(cand)).catch(() => {})
      }
    }

    const answer = await pc.createAnswer()
    await pc.setLocalDescription(answer)

    if (props.competitionId && selectedCandidate.value) {
      await sendAdminTheoryProctorRtcSignalApi(props.competitionId, {
        targetUserId: selectedCandidate.value.userId,
        type: 'ANSWER',
        sdp: answer.sdp,
        sessionId: signal.sessionId
      })
    }
  } else if (signal.type === 'ICE' && signal.candidate) {
    try {
      const candidateInit = JSON.parse(signal.candidate)
      if (rtcPeerConnection && rtcPeerConnection.remoteDescription && rtcPeerConnection.remoteDescription.type) {
        await rtcPeerConnection.addIceCandidate(new RTCIceCandidate(candidateInit))
      } else {
        adminPendingIceCandidates.push(candidateInit)
      }
    } catch {}
  } else if (signal.type === 'HANGUP') {
    adminPendingIceCandidates = []
    currentRemoteStream = null
    if (rtcPeerConnection) {
      rtcPeerConnection.close()
      rtcPeerConnection = null
    }
    if (rtcVideoRef.value) {
      rtcVideoRef.value.srcObject = null
    }
    rtcConnectionState.value = 'CLOSED'
  }
}

async function fetchCandidates() {
  if (!props.competitionId) return
  loading.value = true
  try {
    const res = await getAdminTheoryCandidateMonitorsApi(props.competitionId)
    candidates.value = res?.data || []
    if (selectedCandidate.value) {
      const refreshed = candidates.value.find((c) => String(c.userId) === String(selectedCandidate.value?.userId))
      if (refreshed) {
        selectedCandidate.value = refreshed
      }
    }
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

  const sse = getGlobalSseClient()
  sse.connect()

  unsubSnapshotUpdated = sse.on('THEORY_PROCTOR_SNAPSHOT_UPDATED', (payload: any) => {
    if (!payload || String(payload.competitionId) !== String(props.competitionId)) return
    const cid = String(payload.candidateUserId)
    const target = candidates.value.find((c) => String(c.userId) === cid)
    if (target) {
      target.highResFrame = payload.highResFrame
      target.highResTimestamp = payload.highResTimestamp
    }
    if (selectedCandidate.value && String(selectedCandidate.value.userId) === cid) {
      selectedCandidate.value.highResFrame = payload.highResFrame
      selectedCandidate.value.highResTimestamp = payload.highResTimestamp
      requestingSnapshot.value = false
      message.success('已接收到考生的最新高清快照')
    }
  })

  unsubRtcSignal = sse.on('THEORY_PROCTOR_RTC_SIGNAL', async (payload: any) => {
    await handleIncomingAdminRtcSignal(payload)
  })
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (unsubSnapshotUpdated) {
    unsubSnapshotUpdated()
    unsubSnapshotUpdated = null
  }
  if (unsubRtcSignal) {
    unsubRtcSignal()
    unsubRtcSignal = null
  }
  stopRtcInspection()
})
</script>
