<template>
  <n-modal
    :show="show"
    preset="card"
    closable
    class="challenge-detail-modal max-w-xl w-full min-h-[540px] max-h-[540px] h-[540px] flex flex-col rounded-3xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-xl"
    :style="{ minHeight: '540px', maxHeight: '540px', height: '540px', display: 'flex', flexDirection: 'column' }"
    content-class="flex-1 min-h-0 flex flex-col"
    content-style="flex: 1 1 0%; min-height: 0; display: flex; flex-direction: column;"
    @update:show="handleClose"
    @close="handleClose(false)"
  >
    <template #header>
      <div class="flex items-center gap-2.5 flex-wrap min-w-0 pr-2">
        <h3 class="text-base sm:text-lg font-bold text-google-text-primary dark:text-google-text-darkPrimary truncate">
          {{ challenge?.title || challenge?.name || t('competition.challenge_detail', '赛题详情') }}
        </h3>

        <div
          v-if="challenge?.category || challenge?.categoryName"
          class="inline-flex items-center gap-1.5 text-xs font-semibold shrink-0"
          :class="getCategoryColorClass(challenge.category || challenge.categoryName)"
        >
          <n-icon :size="15" :component="getCategoryIcon(challenge.category || challenge.categoryName)" class="shrink-0" />
          <span>{{ challenge.category || challenge.categoryName || t('competition.category_misc', '杂项') }}</span>
        </div>

        <span
          v-if="challenge?.difficulty"
          class="px-2 py-0.5 rounded text-[11px] bg-google-surface-light dark:bg-google-surface-dark text-google-text-desc dark:text-google-text-darkDesc border border-google-border-light/60 dark:border-google-border-dark/60 font-mono shrink-0"
        >
          {{ challenge.difficulty }}
        </span>
      </div>
    </template>

    <template #header-extra>
      <div class="flex items-center gap-3 font-mono text-xs mr-2 shrink-0">
        <span class="font-bold text-google-blue dark:text-google-blue-dark">
          {{ challenge?.currentPoints }} pts
        </span>
        <span class="text-google-text-muted dark:text-google-text-darkMuted">
          {{ challenge?.solvesCount }} Solves
        </span>
      </div>
    </template>

    <div v-if="challenge" class="pt-1 pb-0 flex-1 min-h-0 h-full flex flex-col justify-between">
      <div class="border-b border-google-border-light/80 dark:border-google-border-dark/80 pb-3 mb-4 shrink-0">
        <VFilterBtnGroup
          v-model="activeTab"
          :options="tabOptions"
          :responsive="false"
        />
      </div>

      <div v-if="activeTab === 'content'" class="flex-1 min-h-0 h-full flex flex-col justify-between">
        <div class="flex-1 min-h-0 overflow-y-auto pr-1 space-y-5">
          <div class="space-y-2">
            <div class="text-xs font-semibold text-google-text-muted dark:text-google-text-darkMuted uppercase">
              {{ t('competition.challenge_description', '赛题描述') }}
            </div>
            <div class="text-sm leading-relaxed text-google-text-primary dark:text-google-text-darkPrimary whitespace-pre-wrap select-text p-4 rounded-xl bg-google-surface-light/60 dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60">
              {{ challenge.description || t('competition.no_description', '暂无题目详细描述。') }}
            </div>
          </div>

          <div v-if="challenge.attachmentUrl" class="space-y-2">
            <div class="text-xs font-semibold text-google-text-muted dark:text-google-text-darkMuted uppercase">
              {{ t('competition.challenge_attachment', '题目附件下载') }}
            </div>
            <VTooltip :content="t('competition.download_attachment_tip', '点击下载离线赛题附件包')">
              <a
                :href="challenge.attachmentUrl"
                download
                class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl border border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard text-xs font-medium text-google-blue dark:text-google-blue-dark hover:bg-google-surface-light dark:hover:bg-google-surface-darkElevated transition-colors shadow-sm"
              >
                <n-icon :size="15" :component="CloudDownloadOutline" />
                <span>{{ t('competition.download_attachment', '下载离线分析附件') }}</span>
              </a>
            </VTooltip>
          </div>

          <div v-if="challenge.isContainer" class="space-y-2">
            <div class="text-xs font-semibold text-google-text-muted dark:text-google-text-darkMuted uppercase">
              {{ t('competition.container_environment', '动态靶机环境') }}
            </div>

            <div v-if="activeInstance" class="p-4 rounded-xl border border-blue-200/70 dark:border-blue-900/70 bg-blue-50/40 dark:bg-blue-950/20 space-y-3">
              <div class="flex items-center justify-center text-center text-xs sm:text-sm font-mono font-semibold text-google-blue dark:text-google-blue-dark select-all break-all">
                <span>{{ instanceConnectionAddress }}</span>
              </div>

              <div class="flex items-center justify-between flex-wrap gap-2 pt-2 border-t border-blue-200/50 dark:border-blue-900/40 text-xs font-mono">
                <div class="flex items-center gap-1.5 text-google-text-muted">
                  <n-icon :size="13" :component="TimeOutline" />
                  <span>{{ t('competition.instance_remaining_time', '容器剩余') }}: {{ formatRemainingSeconds(remainingSecondsVal) }}</span>
                </div>

                <div class="flex items-center gap-2">
                  <VTooltip :content="t('competition.instance_extend_tip', '延长靶机运行时间 15 分钟')">
                    <VBtn
                      clz="primary"
                      size="small"
                      :disabled="instanceActionLoading"
                      @click="handleExtendInstance"
                    >
                      {{ t('competition.instance_extend', '续期 +15m') }}
                    </VBtn>
                  </VTooltip>
                  <VTooltip :content="t('competition.instance_stop_tip', '立即停止并释放当前靶机容器实例')">
                    <VBtn
                      clz="warning"
                      size="small"
                      :disabled="instanceActionLoading"
                      @click="handleStopInstance"
                    >
                      {{ t('competition.instance_stop', '销毁容器') }}
                    </VBtn>
                  </VTooltip>
                </div>
              </div>
            </div>

            <div v-else>
              <button
                type="button"
                class="w-full h-10 px-4 rounded-xl border border-dashed border-google-border-light dark:border-google-border-dark text-xs font-semibold text-google-blue dark:text-google-blue-dark hover:bg-blue-50/50 dark:hover:bg-blue-950/30 transition-colors inline-flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
                :disabled="instanceActionLoading"
                @click="handleStartInstance"
              >
                <n-icon :size="15" :component="RocketOutline" :class="{ 'animate-spin': instanceActionLoading }" />
                <span>{{ instanceActionLoading ? t('common.loading', '靶机调度启动中...') : t('competition.instance_start', '启动容器') }}</span>
              </button>
            </div>
          </div>
        </div>

        <div class="shrink-0 pt-3 border-t border-google-border-light/70 dark:border-google-border-dark/70 space-y-2 mt-auto">
          <div class="text-xs font-semibold text-google-text-muted dark:text-google-text-darkMuted uppercase">
            {{ t('competition.flag_submission', '提交 Flag') }}
          </div>

          <div v-if="challenge.isSolvedByMyTeam" class="p-3.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200/60 dark:border-emerald-900/50 flex items-center gap-2 text-google-green dark:text-google-green-dark text-xs font-semibold">
            <n-icon :size="16" :component="CheckmarkCircleOutline" />
            <span>{{ t('competition.challenge_already_solved_tip', '您的战队已成功攻克此题并夺旗！') }}</span>
          </div>

          <div v-else class="flex items-center gap-2">
            <VInput
              v-model:value="flagInput"
              :placeholder="t('competition.flag_input_placeholder')"
              clearable
              size="large"
              :radius="12"
              class="flex-1"
              @keydown.enter="handleFlagSubmit"
            />
            <VBtn
              clz="primary"
              :disabled="flagSubmitting || !flagInput.trim()"
              :loading="flagSubmitting"
              class="flex-shrink-0"
              @click="handleFlagSubmit"
            >
              {{ flagSubmitting ? t('common.submitting', '校验中...') : t('competition.submit_flag', '提交 Flag') }}
            </VBtn>
          </div>
        </div>
      </div>

      <div v-else-if="activeTab === 'solves'" class="flex-1 min-h-0 h-full flex flex-col justify-between">
        <template v-if="solvedTeamsList.length > 0">
          <div class="flex-1 min-h-0 flex flex-col">
            <div class="grid grid-cols-[48px_1fr_80px] sm:grid-cols-[48px_1fr_160px_84px] items-center gap-3 px-3 py-2 border-b border-google-border-light/70 dark:border-google-border-dark/70 text-[11px] font-semibold uppercase text-google-text-muted dark:text-google-text-darkMuted shrink-0 select-none">
              <span class="text-center">{{ t('competition.solve_index', '序号') }}</span>
              <span class="text-left pl-1">{{ t('competition.solve_team', '队伍') }}</span>
              <span class="text-left hidden sm:inline-block">{{ t('competition.solve_time', '提交时间') }}</span>
              <span class="text-right pr-1">{{ t('competition.solve_points', '获得分数') }}</span>
            </div>

            <div class="flex-1 min-h-0 overflow-y-auto pr-1 py-2 space-y-2">
              <div
                v-for="(team, index) in pagedSolvedTeamsList"
                :key="team.teamId || ((solvesCurrentPage - 1) * SOLVES_PAGE_SIZE + index)"
                class="grid grid-cols-[48px_1fr_80px] sm:grid-cols-[48px_1fr_160px_84px] items-center gap-3 p-3 rounded-2xl bg-google-surface-light/60 dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 hover:border-google-border-light dark:hover:border-google-border-dark transition-colors"
              >
                <div class="w-full flex items-center justify-center shrink-0">
                  <VTooltip v-if="(solvesCurrentPage - 1) * SOLVES_PAGE_SIZE + index === 0" :content="t('competition.first_blood', '一血')">
                    <n-icon :size="16" :component="Medal" class="text-amber-500 dark:text-amber-400" />
                  </VTooltip>
                  <VTooltip v-else-if="(solvesCurrentPage - 1) * SOLVES_PAGE_SIZE + index === 1" :content="t('competition.second_blood', '二血')">
                    <n-icon :size="16" :component="Medal" class="text-slate-400 dark:text-slate-300" />
                  </VTooltip>
                  <VTooltip v-else-if="(solvesCurrentPage - 1) * SOLVES_PAGE_SIZE + index === 2" :content="t('competition.third_blood', '三血')">
                    <n-icon :size="16" :component="Medal" class="text-orange-500 dark:text-orange-400" />
                  </VTooltip>
                  <span v-else class="text-xs font-mono font-medium text-google-text-muted dark:text-google-text-darkMuted">
                    {{ (solvesCurrentPage - 1) * SOLVES_PAGE_SIZE + index + 1 }}
                  </span>
                </div>

                <div class="flex items-center gap-2.5 min-w-0">
                  <VAvatar
                    :src="team.avatarUrl || ''"
                    :text="getTeamInitial(team.teamName)"
                    :size="30"
                    shape="circle"
                    class="shrink-0"
                  />
                  <span class="text-xs sm:text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary truncate block">
                    {{ team.teamName }}
                  </span>
                </div>

                <div class="hidden sm:block text-xs font-mono text-google-text-muted dark:text-google-text-darkMuted truncate">
                  {{ formatSolveTime(team.solvedAt || team.solveTime) }}
                </div>

                <div class="text-right font-mono text-xs font-bold text-google-blue dark:text-google-blue-dark truncate pr-1">
                  {{ team.points ?? team.pointsAwarded ?? challenge.currentPoints }} pts
                </div>
              </div>
            </div>
          </div>

          <div
            v-if="solvedTeamsList.length > SOLVES_PAGE_SIZE"
            class="flex items-center justify-center pt-2 shrink-0 border-t border-google-border-light/60 dark:border-google-border-dark/60 mt-auto"
          >
            <n-pagination
              v-model:page="solvesCurrentPage"
              :page-size="SOLVES_PAGE_SIZE"
              :item-count="solvedTeamsList.length"
              size="small"
            />
          </div>
        </template>

        <div v-else class="my-auto py-12 text-center">
          <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('competition.no_teams_solved', '暂无队伍解出') }}
          </div>
        </div>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import type { Component } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, NModal, NPagination, useMessage } from 'naive-ui'
import {
  RocketOutline,
  CheckmarkCircleOutline,
  CloudDownloadOutline,
  TimeOutline,
  DocumentTextOutline,
  TrophyOutline,
  GlobeOutline,
  TerminalOutline,
  GitBranchOutline,
  KeyOutline,
  ExtensionPuzzleOutline,
  FolderOutline
} from '@vicons/ionicons5'
import { Medal } from '@vicons/fa'
import VAvatar from '@/components/sidebar/VAvatar.vue'
import { VTooltip, VFilterBtnGroup, VInput, VBtn } from '@/components/common'
import type { FilterOption } from '@/components/common'
import {
  submitCtfFlagApi,
  getCtfChallengeDetailApi,
  getCtfInstanceApi,
  startCtfInstanceApi,
  stopCtfInstanceApi,
  extendCtfInstanceApi
} from '@/api/competition'
import type {
  CtfCompetitionChallengeDetailVO,
  CtfChallengeInstanceVO,
  CtfChallengeSolveVO,
  CtfScoreboardItemVO,
  MyTeamInfoVO
} from '@/types/competition'

export interface CtfChallengeSolveItemVO extends CtfChallengeSolveVO {
  solveTime?: string | null
  pointsAwarded?: number
  isFirstBlood?: boolean
}

defineOptions({
  name: 'CtfCompetitionChallengeModal'
})

const props = defineProps<{
  show: boolean
  competitionId?: number | string | null
  challenge: CtfCompetitionChallengeDetailVO | null
  myTeam?: MyTeamInfoVO | null
  solves?: CtfChallengeSolveItemVO[]
  scoreboard?: CtfScoreboardItemVO[]
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'flag-solved', payload: { challengeId: number | string; points: number }): void
}>()

const { t } = useI18n()
const message = useMessage()

const activeTab = ref<string>('content')
const flagInput = ref<string>('')
const flagSubmitting = ref<boolean>(false)
const activeInstance = ref<CtfChallengeInstanceVO | null>(null)
const instanceActionLoading = ref<boolean>(false)
const SOLVES_PAGE_SIZE = 5
const solvesCurrentPage = ref<number>(1)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const tabOptions = computed<FilterOption<string>[]>(() => [
  {
    value: 'content',
    label: t('competition.challenge_tab_detail', '赛题内容'),
    icon: DocumentTextOutline
  },
  {
    value: 'solves',
    label: t('competition.challenge_tab_solves', '已解出战队'),
    icon: TrophyOutline
  }
])

async function syncInstance() {
  if (!props.challenge || !props.challenge.isContainer) {
    activeInstance.value = null
    clearCountdown()
    return
  }

  const inst = props.challenge.instance
  const remaining = inst ? (inst.remainingTimeSeconds ?? inst.remainingSeconds ?? 0) : 0
  if (inst && remaining > 0) {
    activeInstance.value = inst
    startCountdown()
    return
  }

  if (props.competitionId) {
    const targetId = props.challenge.id || props.challenge.challengeId
    if (targetId) {
      try {
        const res = await getCtfInstanceApi(props.competitionId, targetId)
        if (res?.data && (res.data.remainingTimeSeconds ?? res.data.remainingSeconds ?? 0) > 0) {
          activeInstance.value = res.data
          if (props.challenge) {
            props.challenge.instance = res.data
          }
          startCountdown()
          return
        }
      } catch {
      }
    }
  }

  activeInstance.value = null
  clearCountdown()
}

watch(
  () => props.show,
  (newVal) => {
    if (newVal) {
      flagInput.value = ''
      activeTab.value = 'content'
      solvesCurrentPage.value = 1
      syncInstance()
    } else {
      clearCountdown()
    }
  }
)

watch(
  () => props.challenge?.id,
  () => {
    solvesCurrentPage.value = 1
    if (props.show) {
      syncInstance()
    }
  }
)

const solvedTeamsList = computed<CtfChallengeSolveItemVO[]>(() => {
  if (Array.isArray(props.solves) && props.solves.length > 0) {
    return props.solves
  }
  const chalSolves = props.challenge?.solves as CtfChallengeSolveItemVO[] | undefined
  if (Array.isArray(chalSolves) && chalSolves.length > 0) {
    return chalSolves
  }
  const topTeams = props.challenge?.topTeams
  if (Array.isArray(topTeams) && topTeams.length > 0) {
    return topTeams.map((item, idx) => {
      const name = typeof item === 'string' ? item : String((item as { teamName?: string })?.teamName || item)
      const scoreboardItem = props.scoreboard?.find((s) => s.teamName === name)
      return {
        teamName: name,
        avatarUrl: scoreboardItem?.avatarUrl,
        solvedAt: scoreboardItem?.lastSolveTime,
        points: props.challenge?.currentPoints,
        isFirstBlood: idx === 0
      }
    })
  }
  return []
})

const pagedSolvedTeamsList = computed<CtfChallengeSolveItemVO[]>(() => {
  const start = (solvesCurrentPage.value - 1) * SOLVES_PAGE_SIZE
  return solvedTeamsList.value.slice(start, start + SOLVES_PAGE_SIZE)
})

function handleClose(value: boolean) {
  emit('update:show', value)
}

function getTeamInitial(name?: string): string {
  if (!name) return 'T'
  return name.trim().charAt(0).toUpperCase() || 'T'
}

function getCategoryIcon(cat?: string | null): Component {
  const normalized = (cat || '').toLowerCase()
  switch (normalized) {
    case 'web':
      return GlobeOutline
    case 'pwn':
      return TerminalOutline
    case 'reverse':
      return GitBranchOutline
    case 'crypto':
      return KeyOutline
    case 'misc':
      return ExtensionPuzzleOutline
    default:
      return FolderOutline
  }
}

function getCategoryColorClass(cat?: string | null): string {
  const normalized = (cat || '').toLowerCase()
  switch (normalized) {
    case 'web':
      return 'text-google-blue dark:text-google-blue-dark'
    case 'pwn':
      return 'text-google-red dark:text-google-red-dark'
    case 'reverse':
      return 'text-purple-600 dark:text-purple-400'
    case 'crypto':
      return 'text-google-yellow dark:text-google-yellow-dark'
    case 'misc':
      return 'text-google-green dark:text-google-green-dark'
    default:
      return 'text-google-text-desc dark:text-google-text-darkDesc'
  }
}

function formatSolveTime(val?: string | null): string {
  if (!val) return '--'
  if (/^\d{4}-\d{2}-\d{2} \d{2}-\d{2}-\d{2}$/.test(val)) {
    return val
  }
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(val)) {
    return val.replace(/:/g, '-')
  }
  const pad = (n: number) => (n < 10 ? '0' + n : String(n))
  try {
    let d = new Date(val)
    if (isNaN(d.getTime())) {
      if (val.includes('分钟前')) {
        const mins = parseInt(val, 10)
        d = new Date(Date.now() - (isNaN(mins) ? 0 : mins * 60 * 1000))
      } else if (val.includes('小时前')) {
        const hrs = parseInt(val, 10)
        d = new Date(Date.now() - (isNaN(hrs) ? 0 : hrs * 3600 * 1000))
      } else if (val.includes(':')) {
        const parts = val.split(':').map((p) => parseInt(p, 10))
        d = new Date()
        if (parts.length >= 2 && !isNaN(parts[0]) && !isNaN(parts[1])) {
          d.setHours(parts[0], parts[1], parts[2] || 0, 0)
        }
      }
    }
    if (!isNaN(d.getTime())) {
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}-${pad(d.getMinutes())}-${pad(d.getSeconds())}`
    }
    return val
  } catch {
    return val
  }
}

function formatRemainingSeconds(secs: number): string {
  const m = Math.floor(secs / 60)
  const s = secs % 60
  return `${m}m ${s < 10 ? '0' : ''}${s}s`
}

const instanceConnectionAddress = computed(() => {
  if (!activeInstance.value) return ''
  if (activeInstance.value.accessUrl) return activeInstance.value.accessUrl
  if (activeInstance.value.host && activeInstance.value.port) {
    return `${activeInstance.value.host}:${activeInstance.value.port}`
  }
  return ''
})

const remainingSecondsVal = computed(() => {
  if (!activeInstance.value) return 0
  return activeInstance.value.remainingTimeSeconds ?? activeInstance.value.remainingSeconds ?? 0
})

async function handleFlagSubmit() {
  if (!props.challenge || !props.competitionId || flagSubmitting.value) return
  const trimmed = flagInput.value.trim()
  if (!trimmed) {
    if (message) {
      message.warning(t('competition.flag_empty_tip', '请输入有效的 Flag 格式'))
    }
    return
  }

  flagSubmitting.value = true
  try {
    const targetId = props.challenge.id || props.challenge.challengeId
    const res = await submitCtfFlagApi(props.competitionId, targetId, { flag: trimmed })
    if (res?.data === true) {
      if (message) {
        message.success(res?.message || t('competition.flag_correct', '恭喜！Flag 校验正确，夺得本题积分！'))
      }
      props.challenge.isSolvedByMyTeam = true
      props.challenge.solvesCount++

      if (props.myTeam?.teamName) {
        if (!props.challenge.topTeams) {
          props.challenge.topTeams = [props.myTeam.teamName]
        } else if (!props.challenge.topTeams.includes(props.myTeam.teamName) && props.challenge.topTeams.length < 3) {
          props.challenge.topTeams.push(props.myTeam.teamName)
        }
      }

      let awardedPoints = props.challenge.currentPoints
      try {
        const detailRes = await getCtfChallengeDetailApi(props.competitionId, targetId)
        if (detailRes?.data) {
          Object.assign(props.challenge, detailRes.data)
          if (props.myTeam?.teamId && Array.isArray(detailRes.data.solves)) {
            const mySolve = detailRes.data.solves.find(
              (s: any) => String(s.teamId) === String(props.myTeam?.teamId)
            )
            if (mySolve?.points !== undefined) {
              awardedPoints = mySolve.points
            }
          }
        }
      } catch {
      }

      emit('flag-solved', {
        challengeId: targetId,
        points: awardedPoints
      })
    } else {
      if (message) {
        message.error(res?.message || t('competition.flag_incorrect', 'Flag 校验失败，请仔细复查 Payload 与签名'))
      }
    }
  } catch (err: any) {
    if (message) {
      message.error(err?.message || t('competition.flag_incorrect', 'Flag 校验失败，请仔细复查 Payload 与签名'))
    }
  } finally {
    flagSubmitting.value = false
  }
}

async function handleStartInstance() {
  if (!props.challenge || !props.competitionId || instanceActionLoading.value) return
  instanceActionLoading.value = true

  try {
    const targetId = props.challenge.id || props.challenge.challengeId
    const res = await startCtfInstanceApi(props.competitionId, targetId)
    if (res?.data) {
      activeInstance.value = res.data
      if (props.challenge) {
        props.challenge.instance = res.data
      }
      startCountdown()
      if (message) {
        message.success(res?.message || t('competition.instance_started', '靶机容器已成功拉起并分配独立测试端口'))
      }
    }
  } catch (err: any) {
    if (message) {
      message.error(err?.message || t('competition.instance_start_failed', '启动靶机失败'))
    }
  } finally {
    instanceActionLoading.value = false
  }
}

async function handleExtendInstance() {
  if (!props.challenge || !props.competitionId || !activeInstance.value || instanceActionLoading.value) return
  instanceActionLoading.value = true

  try {
    const targetId = props.challenge.id || props.challenge.challengeId
    const res = await extendCtfInstanceApi(props.competitionId, targetId)
    if (res?.data) {
      activeInstance.value = res.data
      if (props.challenge) {
        props.challenge.instance = res.data
      }
      if (message) {
        message.success(res?.message || t('competition.instance_extended', '靶机租赁时间已成功延长 15 分钟'))
      }
    }
  } catch (err: any) {
    if (message) {
      message.error(err?.message || t('competition.instance_extend_failed', '延长靶机租期失败'))
    }
  } finally {
    instanceActionLoading.value = false
  }
}

async function handleStopInstance() {
  if (!props.challenge || !props.competitionId || !activeInstance.value || instanceActionLoading.value) return
  instanceActionLoading.value = true

  try {
    const targetId = props.challenge.id || props.challenge.challengeId
    const res = await stopCtfInstanceApi(props.competitionId, targetId)
    clearCountdown()
    activeInstance.value = null
    if (props.challenge) {
      props.challenge.instance = null
    }
    if (message) {
      message.success(res?.message || t('competition.instance_stopped', '靶机容器资源已顺利回收与释放'))
    }
  } catch (err: any) {
    if (message) {
      message.error(err?.message || t('competition.instance_stop_failed', '销毁靶机容器失败'))
    }
  } finally {
    instanceActionLoading.value = false
  }
}

function startCountdown() {
  clearCountdown()
  countdownTimer = setInterval(() => {
    if (!activeInstance.value) return
    const currentRemaining = activeInstance.value.remainingTimeSeconds ?? activeInstance.value.remainingSeconds
    if (typeof currentRemaining === 'number') {
      if (currentRemaining > 0) {
        const next = currentRemaining - 1
        activeInstance.value.remainingTimeSeconds = next
        activeInstance.value.remainingSeconds = next
      } else {
        activeInstance.value = null
        if (props.challenge) {
          props.challenge.instance = null
        }
        clearCountdown()
      }
    }
  }, 1000)
}

function clearCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

onUnmounted(() => {
  clearCountdown()
})
</script>

<style scoped lang="scss">
.flag-input {
  border: 1px solid #dadce0;
  border-radius: 12px;
  font-family: inherit;
  color: #202124;

  &::placeholder {
    color: #80868b;
  }

  &:focus {
    border-color: #1a73e8;
  }
}

html.dark .flag-input {
  border-color: #3c4043;
  color: #e3e3e3;

  &::placeholder {
    color: #9aa0a6;
  }

  &:focus {
    border-color: #a8c7fa;
  }
}

:global(.challenge-detail-modal) {
  min-height: 540px !important;
  max-height: 540px !important;
  height: 540px !important;
  display: flex !important;
  flex-direction: column !important;
}

:global(.challenge-detail-modal .n-card-content),
:global(.challenge-detail-modal .n-card__content),
:deep(.n-card-content),
:deep(.n-card__content) {
  flex: 1 1 0% !important;
  min-height: 0 !important;
  display: flex !important;
  flex-direction: column !important;
}

:deep(.n-card-header__close .n-base-close),
:deep(.n-base-close) {
  border-radius: 9999px !important;
}
</style>
