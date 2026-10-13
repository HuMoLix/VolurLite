<template>
  <div class="v-ctf-competition-statistic rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark p-4 shadow-sm flex flex-col gap-3.5 select-none transition-colors duration-200">
    <div class="flex items-center gap-3 min-w-0">
      <VAvatar
        :src="team?.avatarUrl || ''"
        :text="teamInitial"
        :size="36"
        shape="circle"
        ring
        class="flex-shrink-0"
      />
      <div class="min-w-0 flex-1">
        <div class="text-sm font-bold text-google-text-primary dark:text-google-text-darkPrimary truncate">
          {{ team?.teamName || t('competition.my_team', '我的战队') }}
        </div>
        <div v-if="team?.organization" class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted truncate">
          {{ team.organization }}
        </div>
      </div>
    </div>

    <div class="flex flex-col gap-2 p-3 rounded-xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60">
      <div class="flex items-center justify-between text-xs">
        <span class="text-[#3c4043] dark:text-google-text-darkDesc font-normal">
          {{ t('competition.current_score', '队伍得分') }}
        </span>
        <span class="font-mono font-normal text-[#3c4043] dark:text-google-text-darkPrimary">
          {{ score ?? 0 }} pts
        </span>
      </div>

      <div class="flex items-center justify-between text-xs">
        <span class="text-[#3c4043] dark:text-google-text-darkDesc font-normal">
          {{ t('competition.solved_challenges', '解题数量') }}
        </span>
        <span class="font-mono font-normal text-[#3c4043] dark:text-google-text-darkPrimary">
          {{ solvesCount ?? 0 }}
        </span>
      </div>

      <div class="flex items-center justify-between text-xs">
        <span class="text-[#3c4043] dark:text-google-text-darkDesc font-normal">
          {{ t('competition.current_rank', '当前排名') }}
        </span>
        <span class="font-mono font-normal text-[#3c4043] dark:text-google-text-darkPrimary">
          {{ rankText }}
        </span>
      </div>
    </div>

    <div class="pt-3 border-t border-google-border-light/70 dark:border-google-border-dark/70 flex items-center justify-center gap-2 text-xs font-normal text-[#3c4043] dark:text-google-text-darkPrimary">
      <n-icon :size="14" :component="HourglassOutline" :class="{ 'animate-spin': isTimeLow }" />
      <span class="font-mono font-normal text-[#3c4043] dark:text-google-text-darkPrimary">
        {{ countdownText }}
      </span>
    </div>

    <div class="pt-1">
      <VBtn
        clz="normal"
        block
        @click="handleOpenWriteupModal"
      >
        {{ t('competition.submit_writeup', '提交 Write Up') }}
      </VBtn>
    </div>

    <VWriteupModal
      v-model:show="showWriteupModal"
      :competition="competition"
      :team="team"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { HourglassOutline } from '@vicons/ionicons5'
import { VBtn } from '@/components/common'
import VAvatar from '@/components/sidebar/VAvatar.vue'
import VWriteupModal from './VWriteupModal.vue'
import type { CompetitionDetailVO, MyTeamInfoVO } from '@/types/competition'

defineOptions({
  name: 'VCtfCompetitionStatistic'
})

export interface VCtfCompetitionStatisticProps {
  competition?: CompetitionDetailVO | null
  team?: MyTeamInfoVO | null
  score?: number
  solvesCount?: number
  rank?: number | string
  startTime?: string | null
  endTime?: string | null
  warningThresholdMinutes?: number
}

const props = withDefaults(defineProps<VCtfCompetitionStatisticProps>(), {
  competition: null,
  team: null,
  score: 0,
  solvesCount: 0,
  rank: '-',
  startTime: undefined,
  endTime: undefined,
  warningThresholdMinutes: 30
})

const emit = defineEmits<{
  (e: 'time-up'): void
  (e: 'submit-writeup'): void
}>()

const { t } = useI18n()

const showWriteupModal = ref<boolean>(false)

function handleOpenWriteupModal() {
  showWriteupModal.value = true
  emit('submit-writeup')
}

const countdownText = ref<string>('00:00:00')
const isTimeLow = ref<boolean>(false)
const timerStatus = ref<'pending' | 'running' | 'ended'>('running')
let timerId: ReturnType<typeof setInterval> | null = null

const teamInitial = computed(() => {
  const name = props.team?.teamName || ''
  return name.trim().charAt(0).toUpperCase() || 'T'
})

const rankText = computed(() => {
  if (props.rank === undefined || props.rank === null || props.rank === '-') {
    return '-'
  }
  return typeof props.rank === 'number' ? `#${props.rank}` : String(props.rank)
})

const effectiveStartTime = computed<string | null>(() => {
  return props.startTime || props.competition?.startTime || null
})

const effectiveEndTime = computed<string | null>(() => {
  return props.endTime || props.competition?.endTime || null
})

function formatDiff(ms: number): string {
  const totalSecs = Math.floor(ms / 1000)
  const days = Math.floor(totalSecs / 86400)
  const hours = Math.floor((totalSecs % 86400) / 3600)
  const minutes = Math.floor((totalSecs % 3600) / 60)
  const seconds = totalSecs % 60
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  if (days > 0) {
    return `${days}d ${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
  }
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}

function updateCountdownTimer() {
  const startMs = effectiveStartTime.value ? new Date(effectiveStartTime.value).getTime() : 0
  const endMs = effectiveEndTime.value ? new Date(effectiveEndTime.value).getTime() : 0
  if (!startMs && !endMs) {
    countdownText.value = '--:--:--'
    return
  }

  const now = Date.now()
  if (startMs > 0 && now < startMs) {
    countdownText.value = formatDiff(startMs - now)
    isTimeLow.value = false
    timerStatus.value = 'pending'
  } else if (endMs > 0 && now <= endMs) {
    const remainingMs = endMs - now
    countdownText.value = formatDiff(remainingMs)
    const warnMs = props.warningThresholdMinutes * 60 * 1000
    isTimeLow.value = remainingMs <= warnMs
    timerStatus.value = 'running'
  } else if (endMs > 0 && now > endMs) {
    const wasRunning = timerStatus.value === 'running'
    timerStatus.value = 'ended'
    countdownText.value = '00:00:00'
    isTimeLow.value = false
    if (wasRunning) {
      emit('time-up')
    }
  }
}

watch([effectiveStartTime, effectiveEndTime], () => {
  updateCountdownTimer()
})

onMounted(() => {
  updateCountdownTimer()
  timerId = setInterval(updateCountdownTimer, 1000)
})

onUnmounted(() => {
  if (timerId) {
    clearInterval(timerId)
    timerId = null
  }
})
</script>
