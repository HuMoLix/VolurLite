<template>
  <CompetitionLayout
    :competition="competition"
    mode="theory"
    :title="competition?.title"
    :start-time="competition?.startTime"
    :end-time="competition?.endTime"
    :default-title="t('competition.type_theory', '理论知识赛')"
    :warning-threshold-minutes="15"
    @time-up="handleTimeUp"
  >
    <div
      v-if="loading"
      class="fixed top-0 left-0 right-0 h-1 z-50 overflow-hidden bg-blue-100 dark:bg-blue-950"
    >
      <div class="progress-bar-primary" />
      <div class="progress-bar-secondary" />
    </div>

    <div class="max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-6 sm:py-8 space-y-6 flex-1 flex flex-col">
      <section class="rounded-2xl sm:rounded-3xl p-5 sm:p-7 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm transition-colors duration-200">
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-5">
          <div class="space-y-1">
            <h1 class="text-xl sm:text-2xl font-bold tracking-tight text-google-text-primary dark:text-google-text-darkPrimary">
              {{ competition?.title || t('competition.type_theory', '理论知识赛') }}
            </h1>
            <div class="flex items-center gap-2 pt-1">
              <span
                class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium border"
                :class="[
                  isBanned
                    ? 'bg-red-50 text-google-red border-red-200 dark:bg-red-950/50 dark:text-google-red-dark dark:border-red-900/60'
                    : isExamStarted
                      ? 'bg-green-50 text-google-green border-green-200 dark:bg-green-950/50 dark:text-google-green-dark dark:border-green-900/60'
                      : 'bg-blue-50 text-google-blue border-blue-200 dark:bg-blue-950/50 dark:text-google-blue-dark dark:border-blue-900/60'
                ]"
              >
                <span
                  class="w-1.5 h-1.5 rounded-full"
                  :class="[
                    isBanned
                      ? 'bg-google-red dark:bg-google-red-dark'
                      : isExamStarted
                        ? 'bg-google-green dark:bg-google-green-dark animate-pulse'
                        : 'bg-google-blue dark:bg-google-blue-dark'
                  ]"
                />
                {{ isBanned ? t('competition.cheat_banned_badge', '考场已封禁') : isExamStarted ? t('competition.cheat_monitoring_active', '严格监考中') : t('competition.cheat_proctor_ready', '监考就绪') }}
              </span>

              <span
                v-if="violationCount > 0"
                class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-mono font-medium bg-amber-50 text-google-yellow border border-amber-200 dark:bg-amber-950/50 dark:text-google-yellow-dark dark:border-amber-900/60"
              >
                {{ t('competition.cheat_violation_count_badge', '违规记录') }}: {{ violationCount }} / {{ maxAllowedViolations }}
              </span>
            </div>
          </div>

          <div class="flex items-center flex-wrap gap-3 self-start md:self-auto">
            <VTooltip :content="t('competition.theory_total_questions_tip', '试卷考核题目总量')">
              <div class="p-3 sm:p-4 rounded-xl sm:rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 text-center min-w-[90px] cursor-default">
                <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted font-medium">
                  {{ t('competition.theory_total_questions', '总题数') }}
                </div>
                <div class="text-lg font-bold font-mono text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">
                  {{ questions.length }}
                </div>
              </div>
            </VTooltip>

            <VTooltip :content="t('competition.theory_total_score_tip', '理论知识考核试卷满分分值')">
              <div class="p-3 sm:p-4 rounded-xl sm:rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 text-center min-w-[90px] cursor-default">
                <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted font-medium">
                  {{ t('competition.theory_total_score', '试卷总分') }}
                </div>
                <div class="text-lg font-bold font-mono text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">
                  {{ paper?.totalScore ?? 0 }}
                </div>
              </div>
            </VTooltip>
          </div>
        </div>
      </section>

      <div
        v-if="isBanned"
        class="flex-1 rounded-2xl sm:rounded-3xl p-8 sm:p-12 bg-red-50/70 dark:bg-red-950/30 border border-red-200 dark:border-red-900/60 flex flex-col items-center justify-center text-center space-y-4 shadow-sm"
      >
        <div class="w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/60 text-google-red dark:text-google-red-dark flex items-center justify-center">
          <n-icon :size="36" :component="AlertCircleOutline" />
        </div>
        <div class="space-y-1.5 max-w-lg">
          <h2 class="text-xl font-bold text-google-red dark:text-google-red-dark">
            {{ t('competition.cheat_locked_title', '考核已终止：账号安全封禁') }}
          </h2>
          <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc">
            {{ banMessage || t('competition.cheat_locked_desc', '检测到违规危险操作次数已达到系统限制阈值，本次考核已被强制终止，当前用户账号已被系统安全封禁。') }}
          </p>
        </div>
        <div class="p-3.5 rounded-xl bg-white dark:bg-google-surface-darkCard border border-red-200 dark:border-red-900/60 text-xs font-mono text-google-text-muted dark:text-google-text-darkMuted">
          {{ t('competition.cheat_violation_count_badge', '违规记录') }}: {{ violationCount }} / {{ maxAllowedViolations }}
        </div>
      </div>

      <div
        v-else-if="!loading && !isExamStarted && !isSubmitted && questions.length > 0"
        class="flex-1 rounded-2xl sm:rounded-3xl p-8 sm:p-12 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm flex flex-col items-center justify-center text-center space-y-6"
      >
        <div class="w-16 h-16 rounded-full bg-blue-50 dark:bg-blue-950/60 text-google-blue dark:text-google-blue-dark flex items-center justify-center">
          <n-icon :size="36" :component="ShieldCheckmarkOutline" />
        </div>

        <div class="space-y-2 max-w-xl">
          <h2 class="text-xl font-bold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('competition.cheat_gate_title', '理论考试严格反作弊准入') }}
          </h2>
          <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
            {{ t('competition.cheat_gate_desc', '本场考试实施高强度反作弊策略。作答期间将锁定全屏状态并启用屏幕操作监控。切屏、离开全屏、复制粘贴、开发者工具或终止录屏均会被记录为违规行为，累计达阈值将自动强制交卷并封禁账号。') }}
          </p>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 w-full max-w-lg text-left text-xs">
          <div class="p-3 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 space-y-1">
            <div class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary">1. 全屏锁定</div>
            <div class="text-google-text-muted dark:text-google-text-darkMuted">考试期间全程保持全屏，按 ESC 或退出全屏将计入违规。</div>
          </div>
          <div class="p-3 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 space-y-1">
            <div class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary">2. 切屏侦测</div>
            <div class="text-google-text-muted dark:text-google-text-darkMuted">禁止使用 Alt+Tab、切换标签页或点击外部应用窗口。</div>
          </div>
          <div class="p-3 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 space-y-1">
            <div class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary">3. 阈值封禁</div>
            <div class="text-google-text-muted dark:text-google-text-darkMuted">累计违规达到 {{ maxAllowedViolations }} 次将立即作废答卷并封禁账号。</div>
          </div>
        </div>

        <VBtn
          clz="primary"
          :radius="12"
          class="px-8 py-3 text-sm font-semibold"
          @click="goToRequireCheck"
        >
          {{ t('competition.cheat_start_exam_btn', '前往考场安全检查并开启作答') }}
        </VBtn>
      </div>

      <div v-else-if="!loading && questions.length === 0" class="flex-1 flex items-center justify-center py-20">
        <div class="text-center space-y-3">
          <n-icon :size="48" :component="DocumentTextOutline" class="text-google-text-muted dark:text-google-text-darkMuted mx-auto" />
          <div class="text-sm text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('competition.theory_no_questions', '当前考核试卷暂无题目') }}
          </div>
        </div>
      </div>

      <div v-else-if="questions.length > 0" class="flex-1 flex flex-col lg:flex-row items-start gap-6">
        <div class="flex-1 min-w-0 w-full space-y-5">
          <TheoryQuestionCard
            v-if="currentQuestion"
            :question="currentQuestion"
            :index="currentIndex + 1"
            :total="questions.length"
            :model-value="answers[currentQuestion.paperId] || ''"
            :is-marked="isQuestionMarked(currentQuestion.paperId)"
            :readonly="isSubmitted"
            @update:model-value="handleAnswerUpdate"
            @toggle-mark="handleToggleMark(currentQuestion.paperId)"
          />

          <div class="flex items-center justify-between gap-4 p-4 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm">
            <VBtn
              clz="normal"
              :left-icon="ArrowBackOutline"
              :disabled="currentIndex === 0"
              @click="handlePrevQuestion"
            >
              {{ t('competition.theory_prev_question', '上一题') }}
            </VBtn>

            <div class="text-xs font-mono font-medium text-google-text-muted dark:text-google-text-darkMuted">
              {{ currentIndex + 1 }} / {{ questions.length }}
            </div>

            <VBtn
              clz="normal"
              :right-icon="ArrowForwardOutline"
              :disabled="currentIndex === questions.length - 1"
              @click="handleNextQuestion"
            >
              {{ t('competition.theory_next_question', '下一题') }}
            </VBtn>
          </div>
        </div>

        <aside class="w-full lg:w-80 flex-shrink-0 lg:sticky lg:top-24">
          <TheoryPaperSheet
            :questions="questions"
            :answers="answers"
            :marked-ids="markedIds"
            :current-index="currentIndex"
            :is-submitted="isSubmitted"
            :total-score="paper?.totalScore ?? 0"
            :my-total-score="paper?.myTotalScore"
            :submitting="submitting"
            @select-question="handleSelectQuestion"
            @submit="handleOpenSubmitConfirm"
          />
        </aside>
      </div>
    </div>

    <n-modal
      :show="warningModalVisible"
      preset="dialog"
      :mask-closable="false"
      :closable="false"
      type="warning"
      :title="t('competition.cheat_warning_title', '安全反作弊警告')"
    >
      <div class="space-y-3 py-2">
        <div class="flex items-center gap-2 text-google-yellow dark:text-google-yellow-dark">
          <n-icon :size="20" :component="WarningOutline" />
          <span class="font-semibold text-sm">{{ currentViolationReason }}</span>
        </div>
        <p class="text-xs text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
          {{ t('competition.cheat_warning_msg', '系统已记录本次异常行为并同步至后端安全审计中心。当前违规次数已达') }}
          <span class="font-mono font-bold text-google-red dark:text-google-red-dark"> {{ violationCount }} / {{ maxAllowedViolations }} </span>。
          {{ t('competition.cheat_warning_tail', '若违规次数达到阈值，考卷将强制以 0 分交卷，且该账号将被永久封禁！') }}
        </p>
      </div>

      <template #action>
        <div class="flex items-center justify-end">
          <VBtn
            clz="primary"
            @click="handleAcknowledgeWarning"
          >
            {{ t('competition.cheat_warning_confirm', '我已知晓并返回全屏答题') }}
          </VBtn>
        </div>
      </template>
    </n-modal>

    <n-modal
      :show="submitConfirmVisible"
      preset="dialog"
      :title="t('competition.theory_submit_confirm', '确定提交答卷吗？')"
      :loading="submitting"
      @close="submitConfirmVisible = false"
    >
      <div class="space-y-3 py-2">
        <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc">
          {{ unansweredCount > 0 ? t('competition.theory_submit_unanswered_warning', { count: unansweredCount }) : t('competition.theory_submit_all_answered', '您已完成全部题目作答，提交后将无法修改，确定提交吗？') }}
        </p>
        <div class="p-3 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 flex items-center justify-between text-xs font-mono">
          <span>{{ t('competition.theory_card_answered', '已答') }}: {{ answeredCount }}</span>
          <span class="text-google-yellow dark:text-google-yellow-dark">{{ t('competition.theory_card_unanswered', '未答') }}: {{ unansweredCount }}</span>
          <span>{{ t('competition.theory_total_questions', '总计') }}: {{ questions.length }}</span>
        </div>
      </div>

      <template #action>
        <div class="flex items-center justify-end gap-2.5">
          <VBtn
            clz="normal"
            @click="submitConfirmVisible = false"
          >
            {{ t('common.cancel', '继续作答') }}
          </VBtn>
          <VBtn
            clz="primary"
            :loading="submitting"
            @click="handleConfirmSubmit"
          >
            {{ t('common.confirm', '确认提交') }}
          </VBtn>
        </div>
      </template>
    </n-modal>

    <TheoryResultModal
      :show="resultModalVisible"
      :result="examResult"
      @update:show="handleResultModalClose"
      @confirm="handleResultConfirmed"
    />
  </CompetitionLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon, NModal, useMessage } from 'naive-ui'
import {
  DocumentTextOutline,
  ArrowBackOutline,
  ArrowForwardOutline,
  WarningOutline,
  ShieldCheckmarkOutline,
  AlertCircleOutline
} from '@vicons/ionicons5'
import { useUserStore } from '@/store/modules/user'
import { useProctorStore } from '@/store/modules/proctor'
import CompetitionLayout from '@/components/layouts/CompetitionLayout.vue'
import { VTooltip, VBtn } from '@/components/common'
import {
  TheoryQuestionCard,
  TheoryPaperSheet,
  TheoryResultModal
} from '@/components/competition/theory'
import {
  getCompetitionByIdApi,
  getCompetitionBySlugApi,
  getTheoryPaperApi,
  submitTheoryPaperApi,
  reportTheoryCheatEventApi,
  sendProctorHeartbeatApi,
  uploadTheoryProctorSnapshotApi,
  sendTheoryProctorRtcSignalApi,
  getMyTeamApi
} from '@/api/competition'
import { getGlobalSseClient } from '@/utils/sse'
import { getDefaultRtcConfiguration } from '@/utils/webrtc'
import type {
  CompetitionDetailVO,
  TheoryPaperExamVO,
  TheoryPaperQuestionVO,
  TheoryPaperResultVO,
  TheoryPaperSubmitRequestDTO,
  TheoryCheatEventType,
  TheoryProctorRtcSignalDTO
} from '@/types/competition'

defineOptions({
  name: 'TheoryCompetitionView'
})

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const message = useMessage()

const loading = ref<boolean>(false)
const submitting = ref<boolean>(false)

const userStore = useUserStore()
let autoSaveTimer: ReturnType<typeof setInterval> | null = null

const competition = ref<CompetitionDetailVO | null>(null)
const paper = ref<TheoryPaperExamVO | null>(null)

const answers = reactive<Record<string | number, string>>({})
const markedIds = ref<Set<string | number>>(new Set())
const currentIndex = ref<number>(0)

const submitConfirmVisible = ref<boolean>(false)
const resultModalVisible = ref<boolean>(false)
const examResult = ref<TheoryPaperResultVO | null>(null)

const isExamStarted = ref<boolean>(false)
const isBanned = ref<boolean>(false)
const banMessage = ref<string>('')
const violationCount = ref<number>(0)
const maxAllowedViolations = ref<number>(3)
const warningModalVisible = ref<boolean>(false)
const currentViolationReason = ref<string>('')
const proctorStore = useProctorStore()
let proctorWorker: Worker | null = null
let offscreenVideo: HTMLVideoElement | null = null
let offscreenCanvas: HTMLCanvasElement | null = null
let lastViolationReportTime = 0

const competitionIdentifier = computed<string>(() => {
  return String(route.params.slug || route.params.id || route.query.competitionId || '')
})

const questions = computed<TheoryPaperQuestionVO[]>(() => {
  return paper.value?.questions || []
})

const currentQuestion = computed<TheoryPaperQuestionVO | null>(() => {
  return questions.value[currentIndex.value] || null
})

const isSubmitted = computed<boolean>(() => {
  return Boolean(paper.value?.isSubmitted)
})

const answeredCount = computed<number>(() => {
  let count = 0
  for (const q of questions.value) {
    const a = answers[q.paperId]
    if (a && a.trim().length > 0) {
      count++
    }
  }
  return count
})

const unansweredCount = computed<number>(() => {
  return Math.max(0, questions.value.length - answeredCount.value)
})

function isQuestionMarked(paperId: string | number): boolean {
  return markedIds.value.has(paperId)
}

function handleAnswerUpdate(val: string) {
  if (!currentQuestion.value || isSubmitted.value || isBanned.value) return
  answers[currentQuestion.value.paperId] = val
}

function handleToggleMark(paperId: string | number) {
  if (markedIds.value.has(paperId)) {
    markedIds.value.delete(paperId)
  } else {
    markedIds.value.add(paperId)
  }
}

function handleSelectQuestion(index: number) {
  if (index >= 0 && index < questions.value.length) {
    currentIndex.value = index
  }
}

function handlePrevQuestion() {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

function handleNextQuestion() {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
  }
}

function handleOpenSubmitConfirm() {
  if (isSubmitted.value || isBanned.value) return
  submitConfirmVisible.value = true
}

function getDraftStorageKey(): string {
  const compId = competition.value?.id || competitionIdentifier.value
  if (!compId) return ''
  const uid = userStore.user?.id || 'guest'
  return `volur_theory_draft_${compId}_${uid}`
}

function saveDraftAnswers() {
  if (isSubmitted.value || isBanned.value) return
  const key = getDraftStorageKey()
  if (!key || typeof window === 'undefined' || !window.localStorage) return

  try {
    const draft = {
      answers: { ...answers },
      markedIds: Array.from(markedIds.value),
      currentIndex: currentIndex.value,
      savedAt: Date.now()
    }
    window.localStorage.setItem(key, JSON.stringify(draft))
  } catch {}
}

function restoreDraftAnswers() {
  if (isSubmitted.value || isBanned.value) return
  const key = getDraftStorageKey()
  if (!key || typeof window === 'undefined' || !window.localStorage) return

  try {
    const raw = window.localStorage.getItem(key)
    if (!raw) return
    const draft = JSON.parse(raw)
    if (draft?.answers && typeof draft.answers === 'object') {
      for (const [k, v] of Object.entries(draft.answers)) {
        if (typeof v === 'string') {
          if (answers[k] === undefined || answers[k] === null || answers[k] === '') {
            answers[k] = v
          }
        }
      }
    }
    if (Array.isArray(draft?.markedIds)) {
      markedIds.value = new Set(draft.markedIds)
    }
    if (
      typeof draft?.currentIndex === 'number' &&
      draft.currentIndex >= 0 &&
      draft.currentIndex < questions.value.length
    ) {
      currentIndex.value = draft.currentIndex
    }
  } catch {}
}

function clearDraftAnswers() {
  const key = getDraftStorageKey()
  if (!key || typeof window === 'undefined' || !window.localStorage) return
  try {
    window.localStorage.removeItem(key)
  } catch {}
}

async function reportViolation(eventType: TheoryCheatEventType, detail: string) {
  if (!competition.value?.id || isSubmitted.value || isBanned.value) return

  const now = Date.now()
  if (now - lastViolationReportTime < 2000) {
    return
  }
  lastViolationReportTime = now

  currentViolationReason.value = detail

  const highResFrame = captureHighResSnapshot()
  if (highResFrame && competition.value?.id) {
    uploadTheoryProctorSnapshotApi(competition.value.id, {
      screenFrame: highResFrame,
      reason: eventType,
      timestamp: now
    }).catch(() => {})
  }

  try {
    const res = await reportTheoryCheatEventApi(competition.value.id, {
      eventType,
      detail,
      timestamp: now
    })

    if (res?.data) {
      violationCount.value = res.data.violationCount
      maxAllowedViolations.value = res.data.maxAllowedViolations

      if (res.data.isBanned) {
        isBanned.value = true
        banMessage.value = res.data.message
        clearDraftAnswers()
        warningModalVisible.value = false
        stopProctoring()
      } else {
        warningModalVisible.value = true
      }
    }
  } catch {}
}

let highResCanvas: HTMLCanvasElement | null = null
let candidatePeerConnection: RTCPeerConnection | null = null
let candidatePendingIceCandidates: RTCIceCandidateInit[] = []
let activeAdminUserId: number | string | null = null
let activeAdminSessionId = ''
let unsubSseSnapshotRequest: (() => void) | null = null
let unsubSseRtcSignal: (() => void) | null = null

function setupOffscreenVideo(stream: MediaStream) {
  if (typeof document === 'undefined') return
  if (!offscreenVideo) {
    offscreenVideo = document.createElement('video')
    offscreenVideo.autoplay = true
    offscreenVideo.playsInline = true
    offscreenVideo.muted = true
  }
  offscreenVideo.srcObject = stream
  offscreenVideo.play().catch(() => {})

  if (!offscreenCanvas) {
    offscreenCanvas = document.createElement('canvas')
    offscreenCanvas.width = 320
    offscreenCanvas.height = 180
  }
}

function captureScreenFrame(): string {
  if (!offscreenVideo || !offscreenCanvas || offscreenVideo.videoWidth === 0) {
    return ''
  }
  try {
    const ctx = offscreenCanvas.getContext('2d')
    if (!ctx) return ''
    ctx.drawImage(offscreenVideo, 0, 0, 320, 180)
    return offscreenCanvas.toDataURL('image/jpeg', 0.5)
  } catch {
    return ''
  }
}

function captureHighResSnapshot(): string {
  if (!offscreenVideo || offscreenVideo.videoWidth === 0) {
    return ''
  }
  try {
    const w = offscreenVideo.videoWidth || 1920
    const h = offscreenVideo.videoHeight || 1080
    if (!highResCanvas) {
      highResCanvas = document.createElement('canvas')
    }
    highResCanvas.width = w
    highResCanvas.height = h
    const ctx = highResCanvas.getContext('2d')
    if (!ctx) return ''
    ctx.drawImage(offscreenVideo, 0, 0, w, h)
    return highResCanvas.toDataURL('image/jpeg', 0.85)
  } catch {
    return ''
  }
}

function startProctorHeartbeatWorker() {
  if (proctorWorker) {
    proctorWorker.postMessage({ action: 'STOP' })
    proctorWorker.terminate()
    proctorWorker = null
  }
  if (typeof Worker === 'undefined') return
  proctorWorker = new Worker(
    new URL('@/workers/theoryHeartbeat.worker.ts', import.meta.url),
    { type: 'module' }
  )
  proctorWorker.onmessage = async (e) => {
    if (e.data?.type === 'HEARTBEAT_TICK') {
      await handleHeartbeatTick()
    }
  }
  proctorWorker.postMessage({ action: 'START', interval: 5000 })
}

async function handleHeartbeatTick() {
  if (!competition.value?.id || isSubmitted.value || isBanned.value) return

  const isFs = Boolean(document.fullscreenElement)
  const isScActive = Boolean(
    proctorStore.screenStream &&
    proctorStore.screenStream.getVideoTracks().some((t) => t.readyState === 'live')
  )
  const frameBase64 = captureScreenFrame()

  try {
    const res = await sendProctorHeartbeatApi(competition.value.id, {
      fullscreen: isFs,
      screenActive: isScActive,
      screenFrame: frameBase64,
      timestamp: Date.now(),
      answeredCount: answeredCount.value,
      totalCount: questions.value.length
    })

    if (res?.data) {
      violationCount.value = res.data.violationCount
      maxAllowedViolations.value = res.data.maxAllowedViolations

      if (res.data.isBanned) {
        isBanned.value = true
        banMessage.value = '您已被系统判定严重违规或防作弊异常，考场已被永久封禁。'
        stopProctoring()
        clearDraftAnswers()
      }
    }
  } catch {}
}

async function handleIncomingRtcSignal(payload: any) {
  if (!competition.value?.id || !payload?.signal) return
  const signal: TheoryProctorRtcSignalDTO = payload.signal
  const senderUserId = payload.senderUserId

  if (signal.type === 'CALL') {
    if (!proctorStore.screenStream) {
      if (senderUserId && signal.sessionId) {
        sendTheoryProctorRtcSignalApi(competition.value.id, {
          targetUserId: senderUserId,
          type: 'HANGUP',
          sessionId: signal.sessionId
        }).catch(() => {})
      }
      return
    }

    activeAdminUserId = senderUserId
    activeAdminSessionId = signal.sessionId || ''
    candidatePendingIceCandidates = []

    if (candidatePeerConnection) {
      candidatePeerConnection.close()
      candidatePeerConnection = null
    }

    const pc = new RTCPeerConnection(getDefaultRtcConfiguration())
    candidatePeerConnection = pc

    const videoTrack = proctorStore.screenStream.getVideoTracks().find((t) => t.readyState === 'live')
    if (videoTrack) {
      pc.addTrack(videoTrack, proctorStore.screenStream)
    }

    pc.onicecandidate = (event) => {
      if (event.candidate && competition.value?.id) {
        sendTheoryProctorRtcSignalApi(competition.value.id, {
          targetUserId: senderUserId,
          type: 'ICE',
          candidate: JSON.stringify(event.candidate.toJSON()),
          sessionId: signal.sessionId
        }).catch(() => {})
      }
    }

    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)

    await sendTheoryProctorRtcSignalApi(competition.value.id, {
      targetUserId: senderUserId,
      type: 'OFFER',
      sdp: offer.sdp,
      sessionId: signal.sessionId
    })
  } else if (signal.type === 'ANSWER') {
    if (candidatePeerConnection && signal.sdp) {
      await candidatePeerConnection.setRemoteDescription(
        new RTCSessionDescription({ type: 'answer', sdp: signal.sdp })
      )
      while (candidatePendingIceCandidates.length > 0) {
        const cand = candidatePendingIceCandidates.shift()
        if (cand) {
          await candidatePeerConnection.addIceCandidate(new RTCIceCandidate(cand)).catch(() => {})
        }
      }
    }
  } else if (signal.type === 'ICE') {
    if (signal.candidate) {
      try {
        const candidateInit = JSON.parse(signal.candidate)
        if (candidatePeerConnection && candidatePeerConnection.remoteDescription && candidatePeerConnection.remoteDescription.type) {
          await candidatePeerConnection.addIceCandidate(new RTCIceCandidate(candidateInit))
        } else {
          candidatePendingIceCandidates.push(candidateInit)
        }
      } catch {}
    }
  } else if (signal.type === 'HANGUP') {
    candidatePendingIceCandidates = []
    if (candidatePeerConnection) {
      candidatePeerConnection.close()
      candidatePeerConnection = null
    }
    activeAdminUserId = null
    activeAdminSessionId = ''
  }
}

function setupProctorRealtimeListeners() {
  const sse = getGlobalSseClient()
  sse.connect()

  unsubSseSnapshotRequest = sse.on('THEORY_PROCTOR_SNAPSHOT_REQUEST', async () => {
    if (!competition.value?.id || isSubmitted.value || isBanned.value) return
    const highResFrame = captureHighResSnapshot()
    if (highResFrame) {
      await uploadTheoryProctorSnapshotApi(competition.value.id, {
        screenFrame: highResFrame,
        reason: 'INSPECT',
        timestamp: Date.now()
      })
    }
  })

  unsubSseRtcSignal = sse.on('THEORY_PROCTOR_RTC_SIGNAL', async (payload: any) => {
    await handleIncomingRtcSignal(payload)
  })
}

function cleanupProctorRealtimeListeners() {
  if (unsubSseSnapshotRequest) {
    unsubSseSnapshotRequest()
    unsubSseSnapshotRequest = null
  }
  if (unsubSseRtcSignal) {
    unsubSseRtcSignal()
    unsubSseRtcSignal = null
  }
  candidatePendingIceCandidates = []
  if (candidatePeerConnection) {
    candidatePeerConnection.close()
    candidatePeerConnection = null
  }
  activeAdminUserId = null
  activeAdminSessionId = ''
}

function stopProctoring() {
  cleanupProctorRealtimeListeners()
  if (proctorWorker) {
    proctorWorker.postMessage({ action: 'STOP' })
    proctorWorker.terminate()
    proctorWorker = null
  }
  if (offscreenVideo) {
    offscreenVideo.srcObject = null
  }
  proctorStore.clearProctorState()
  if (document.fullscreenElement && document.exitFullscreen) {
    document.exitFullscreen().catch(() => {})
  }
}

function initProctorSession() {
  if (isSubmitted.value || isBanned.value) return
  if (!proctorStore.screenStream) return

  setupOffscreenVideo(proctorStore.screenStream)
  setupProctorRealtimeListeners()
  const track = proctorStore.screenStream.getVideoTracks()[0]
  if (track) {
    track.onended = () => {
      if (candidatePeerConnection && activeAdminUserId && activeAdminSessionId && competition.value?.id) {
        sendTheoryProctorRtcSignalApi(competition.value.id, {
          targetUserId: activeAdminUserId,
          type: 'HANGUP',
          sessionId: activeAdminSessionId
        }).catch(() => {})
      }
      if (candidatePeerConnection) {
        candidatePeerConnection.close()
        candidatePeerConnection = null
      }
      if (isExamStarted.value && !isSubmitted.value && !isBanned.value) {
        reportViolation('STOP_SCREEN_SHARE', '考生主动终止屏幕共享监控流')
      }
    }
  }

  if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
    document.documentElement.requestFullscreen().catch(() => {})
  }

  isExamStarted.value = true
  startProctorHeartbeatWorker()
}

function goToRequireCheck() {
  const isShortLink = route.path.startsWith('/c/')
  const slug = competition.value?.slug || competition.value?.id || competitionIdentifier.value
  router.replace(isShortLink ? `/c/${slug}/theory/require` : `/competitions/${slug}/theory/require`)
}

function handleAcknowledgeWarning() {
  warningModalVisible.value = false
  if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
    document.documentElement.requestFullscreen().catch(() => {})
  }
}

function handleVisibilityChange() {
  if (!isExamStarted.value || isSubmitted.value || isBanned.value) return
  if (document.hidden) {
    reportViolation('VISIBILITY_HIDDEN', '切换至其他标签页或最小化浏览器窗口')
  }
}

function handleWindowBlur() {
  if (!isExamStarted.value || isSubmitted.value || isBanned.value) return
  if (warningModalVisible.value || submitConfirmVisible.value) return
  reportViolation('WINDOW_BLUR', '窗口失去焦点，疑似使用 Alt+Tab 或点击外部第三方窗口')
}

function handleFullscreenChange() {
  if (!isExamStarted.value || isSubmitted.value || isBanned.value) return
  if (!document.fullscreenElement && !warningModalVisible.value && !submitConfirmVisible.value) {
    reportViolation('EXIT_FULLSCREEN', '退出全屏考试状态')
  }
}

function handleClipboard(ev: ClipboardEvent) {
  if (!isExamStarted.value || isSubmitted.value || isBanned.value) return
  ev.preventDefault()
  reportViolation('COPY_PASTE_ATTEMPT', '尝试复制、剪切或粘贴试题内容')
}

function handleContextMenu(ev: MouseEvent) {
  if (!isExamStarted.value || isSubmitted.value || isBanned.value) return
  ev.preventDefault()
  reportViolation('CONTEXTMENU_ATTEMPT', '尝试打开右键菜单')
}

function handleKeydown(ev: KeyboardEvent) {
  if (isBanned.value) {
    ev.preventDefault()
    return
  }

  if (
    ev.key === 'F12' ||
    (ev.ctrlKey && ev.shiftKey && (ev.key === 'I' || ev.key === 'i' || ev.key === 'C' || ev.key === 'c' || ev.key === 'J' || ev.key === 'j')) ||
    (ev.ctrlKey && (ev.key === 'U' || ev.key === 'u' || ev.key === 'S' || ev.key === 's' || ev.key === 'P' || ev.key === 'p'))
  ) {
    ev.preventDefault()
    if (isExamStarted.value && !isSubmitted.value) {
      reportViolation('RESTRICTED_KEY', `尝试使用受限控制台快捷键: ${ev.key}`)
    }
    return
  }

  const target = ev.target as HTMLElement
  if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) {
    return
  }

  if (ev.key === 'ArrowLeft') {
    handlePrevQuestion()
  } else if (ev.key === 'ArrowRight') {
    handleNextQuestion()
  }
}

async function handleConfirmSubmit() {
  if (submitting.value || isSubmitted.value || !competition.value?.id || isBanned.value) return

  submitting.value = true
  try {
    const payload: TheoryPaperSubmitRequestDTO = {
      answers: questions.value.map((q) => ({
        paperId: q.paperId,
        answer: answers[q.paperId] ? answers[q.paperId].trim() : ''
      }))
    }

    const res = await submitTheoryPaperApi(competition.value.id, payload)
    if (!res?.data) {
      throw new Error(t('competition.theory_submit_failed', '提交试卷失败'))
    }

    examResult.value = res.data
    if (paper.value) {
      paper.value.isSubmitted = true
    }

    submitConfirmVisible.value = false
    resultModalVisible.value = true
    clearDraftAnswers()
    stopProctoring()

    if (message) {
      message.success(t('competition.theory_submit_success', '试卷提交成功！'))
    }
  } catch (err: any) {
    if (message) {
      message.error(err?.message || t('competition.theory_submit_failed', '提交试卷失败'))
    }
  } finally {
    submitting.value = false
  }
}

function handleResultConfirmed() {
  resultModalVisible.value = false
  const isShortLink = route.path.startsWith('/c/')
  const slug = competition.value?.slug || competition.value?.id || competitionIdentifier.value
  router.replace(isShortLink ? `/c/${slug}` : `/competitions/${slug}`)
}

function handleResultModalClose(val: boolean) {
  resultModalVisible.value = val
  if (!val) {
    const isShortLink = route.path.startsWith('/c/')
    const slug = competition.value?.slug || competition.value?.id || competitionIdentifier.value
    router.replace(isShortLink ? `/c/${slug}` : `/competitions/${slug}`)
  }
}

function handleTimeUp() {
  if (!isSubmitted.value && !isBanned.value && questions.value.length > 0) {
    if (message) {
      message.warning(t('competition.countdown_ended', '考核时间已截止，正在自动交卷...'))
    }
    handleConfirmSubmit()
  }
}

async function fetchCompetitionAndPaper() {
  if (!competitionIdentifier.value) {
    competition.value = null
    paper.value = null
    return
  }

  if (!userStore.isLoggedIn) {
    router.replace(`/v1/signin/identifier?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  loading.value = true
  try {
    const isNumeric = /^\d+$/.test(competitionIdentifier.value)
    let compData: CompetitionDetailVO | null = null

    if (isNumeric) {
      try {
        const compRes = await getCompetitionByIdApi(competitionIdentifier.value)
        compData = compRes?.data || null
      } catch {
        const slugRes = await getCompetitionBySlugApi(competitionIdentifier.value)
        compData = slugRes?.data || null
      }
    } else {
      const slugRes = await getCompetitionBySlugApi(competitionIdentifier.value)
      compData = slugRes?.data || null
    }

    if (!compData) {
      competition.value = null
      paper.value = null
      if (message) {
        message.error(t('competition.empty_desc', '未找到对应竞赛信息'))
      }
      return
    }

    competition.value = compData

    let currentTeam: any = null
    try {
      const teamRes = await getMyTeamApi(compData.id)
      currentTeam = teamRes?.data || null
    } catch {
      currentTeam = null
    }

    if (currentTeam && currentTeam.status === 'BANNED') {
      if (message) {
        message.error(t('competition.team_banned', '您的战队已被封禁，无法参加此场比赛'))
      }
      const isShortLink = route.path.startsWith('/c/')
      const slug = compData.slug || compData.id
      router.replace(isShortLink ? `/c/${slug}` : `/competitions/${slug}`)
      return
    }

    if (currentTeam && currentTeam.status === 'ENDED' && !userStore.isAdmin) {
      if (message) {
        message.warning(t('competition.theory_exam_ended_tip', '您的战队答题已结束，无法查看试卷题目'))
      }
      const isShortLink = route.path.startsWith('/c/')
      const slug = compData.slug || compData.id
      router.replace(isShortLink ? `/c/${slug}` : `/competitions/${slug}`)
      return
    }

    const isAuthorized = Boolean(userStore.isAdmin) || (currentTeam !== null && currentTeam.status === 'AUDIT_PASSED')
    if (!isAuthorized) {
      if (message) {
        message.error(t('competition.not_participating', '您尚未加入已审核通过的参赛队伍，无法进入竞赛工作台'))
      }
      const isShortLink = route.path.startsWith('/c/')
      const slug = compData.slug || compData.id
      router.replace(isShortLink ? `/c/${slug}` : `/competitions/${slug}`)
      return
    }

    try {
      const paperRes = await getTheoryPaperApi(compData.id)
      for (const key of Object.keys(answers)) {
        delete answers[key]
      }
      markedIds.value.clear()
      currentIndex.value = 0

      if (paperRes?.data) {
        paper.value = paperRes.data
        for (const q of paperRes.data.questions || []) {
          if (q.myAnswer !== undefined && q.myAnswer !== null) {
            answers[q.paperId] = q.myAnswer
          }
        }
        if (!paperRes.data.isSubmitted) {
          const hasValidStream = Boolean(
            proctorStore.isPrecheckPassed &&
            proctorStore.screenStream &&
            proctorStore.screenStream.getVideoTracks().some((t) => t.readyState === 'live')
          )
          if (!hasValidStream) {
            const isShortLink = route.path.startsWith('/c/')
            const slug = compData.slug || compData.id
            router.replace(isShortLink ? `/c/${slug}/theory/require` : `/competitions/${slug}/theory/require`)
            return
          }
          restoreDraftAnswers()
          initProctorSession()
        } else if (!userStore.isAdmin) {
          clearDraftAnswers()
          stopProctoring()
          if (message) {
            message.warning(t('competition.theory_exam_ended_tip', '您的战队答题已结束，无法查看试卷题目'))
          }
          const isShortLink = route.path.startsWith('/c/')
          const slug = compData.slug || compData.id
          router.replace(isShortLink ? `/c/${slug}` : `/competitions/${slug}`)
          return
        } else {
          clearDraftAnswers()
          stopProctoring()
        }
      } else {
        paper.value = null
      }
    } catch (err: any) {
      const isShortLink = route.path.startsWith('/c/')
      const slug = compData.slug || compData.id
      const status = err?.response?.status || err?.status
      const msg = err?.response?.data?.message || err?.message
      if (status === 403) {
        if (msg && msg.includes('答题已结束')) {
          if (message) {
            message.warning(msg)
          }
          router.replace(isShortLink ? `/c/${slug}` : `/competitions/${slug}`)
          return
        }
        router.replace(isShortLink ? `/c/${slug}/theory/require` : `/competitions/${slug}/theory/require`)
        return
      }
      if (message) {
        message.error(msg || t('competition.fetch_failed', '获取试卷数据失败'))
      }
      router.replace(isShortLink ? `/c/${slug}` : `/competitions/${slug}`)
      return
    }
  } catch (err: any) {
    competition.value = null
    paper.value = null
    if (message) {
      message.error(err?.message || t('competition.fetch_failed', '获取试卷数据失败'))
    }
  } finally {
    loading.value = false
  }
}

watch(competitionIdentifier, (newVal) => {
  if (newVal) {
    fetchCompetitionAndPaper()
  }
})

onMounted(() => {
  fetchCompetitionAndPaper()
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('beforeunload', saveDraftAnswers)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('blur', handleWindowBlur)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('copy', handleClipboard)
  document.addEventListener('cut', handleClipboard)
  document.addEventListener('paste', handleClipboard)
  document.addEventListener('contextmenu', handleContextMenu)
  autoSaveTimer = setInterval(saveDraftAnswers, 3000)
})

onUnmounted(() => {
  saveDraftAnswers()
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
    autoSaveTimer = null
  }
  stopProctoring()
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('beforeunload', saveDraftAnswers)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('blur', handleWindowBlur)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('copy', handleClipboard)
  document.removeEventListener('cut', handleClipboard)
  document.removeEventListener('paste', handleClipboard)
  document.removeEventListener('contextmenu', handleContextMenu)
})
</script>

<style scoped lang="scss">
.progress-bar-primary {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 100%;
  transform-origin: top left;
  background-color: #1a73e8;
  animation: mdc-linear-progress-primary-indeterminate-translate 2s linear infinite;
}

.progress-bar-secondary {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 100%;
  transform-origin: top left;
  background-color: #1a73e8;
  animation: mdc-linear-progress-secondary-indeterminate-translate 2s linear infinite;
}

@keyframes mdc-linear-progress-primary-indeterminate-translate {
  0% {
    transform: translateX(-145.166611%) scaleX(0.5);
  }
  20% {
    transform: translateX(-54.526915%) scaleX(0.798733);
  }
  59.15% {
    transform: translateX(95.443374%) scaleX(0.384373);
  }
  100% {
    transform: translateX(366.852433%) scaleX(0.01);
  }
}

@keyframes mdc-linear-progress-secondary-indeterminate-translate {
  0% {
    transform: translateX(-54.888891%) scaleX(0.1);
  }
  25% {
    transform: translateX(-17.236978%) scaleX(0.536345);
  }
  48.35% {
    transform: translateX(29.49714%) scaleX(0.741079);
  }
  100% {
    transform: translateX(107.384235%) scaleX(0.01);
  }
}
</style>
