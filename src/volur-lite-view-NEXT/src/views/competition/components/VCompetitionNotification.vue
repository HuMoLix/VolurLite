<template>
  <div
    class="v-competition-notification"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <div class="notification-icon-wrap">
      <n-icon :size="16" :component="MegaphoneOutline" class="text-google-blue dark:text-google-blue-dark" />
    </div>

    <div class="notification-divider"></div>

    <div class="notification-content">
      <div v-if="notifications.length === 0" class="empty-state">
        <span class="live-dot animate-pulse"></span>
        <span class="empty-text">
          {{ t('competition.notification_waiting', '赛事进行中...') }}
        </span>
      </div>

      <Transition v-else name="ticker-slide" mode="out-in">
        <div
          :key="currentNotification?.id || currentIndex"
          class="ticker-item"
        >
          <span
            class="blood-badge"
            :class="getBloodBadgeClass(currentNotification?.bloodType, currentNotification?.solveRank)"
          >
            {{ getBloodLabel(currentNotification?.bloodType, currentNotification?.solveRank) }}
          </span>

          <span class="team-name">
            {{ currentNotification?.teamName }}
          </span>

          <span class="solve-action">
            {{ t('competition.notification_solved_action', '成功攻破') }}
          </span>

          <span
            v-if="currentNotification?.challengeCategory"
            class="category-tag"
            :class="getCategoryColorClass(currentNotification?.challengeCategory)"
          >
            {{ currentNotification?.challengeCategory }}
          </span>

          <span class="challenge-title">
            {{ currentNotification?.challengeTitle }}
          </span>

          <span v-if="currentNotification?.pointsAwarded" class="points-tag">
            +{{ currentNotification?.pointsAwarded }} pts
          </span>

          <span v-if="currentNotification?.solvedAt" class="solve-time">
            {{ formatSolveTime(currentNotification?.solvedAt) }}
          </span>
        </div>
      </Transition>
    </div>

    <div v-if="notifications.length > 1" class="notification-counter">
      {{ currentIndex + 1 }}/{{ notifications.length }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { MegaphoneOutline } from '@vicons/ionicons5'
import { getGlobalSseClient } from '@/utils/sse'
import type { CompetitionSolveNotificationItem } from '@/types/competition'

defineOptions({
  name: 'VCompetitionNotification'
})

const props = withDefaults(
  defineProps<{
    competitionId?: number | string | null
    duration?: number
  }>(),
  {
    competitionId: null,
    duration: 4000
  }
)

const { t } = useI18n()

const notifications = ref<CompetitionSolveNotificationItem[]>([])
const currentIndex = ref<number>(0)
const isHovered = ref<boolean>(false)

let rotationTimer: ReturnType<typeof setInterval> | null = null
let unsubSse: (() => void) | null = null

const currentNotification = computed<CompetitionSolveNotificationItem | null>(() => {
  if (notifications.value.length === 0) return null
  return notifications.value[currentIndex.value] || null
})

function formatSolveTime(val?: string | null): string {
  if (!val) return ''
  try {
    const d = new Date(val)
    if (!isNaN(d.getTime())) {
      const h = String(d.getHours()).padStart(2, '0')
      const m = String(d.getMinutes()).padStart(2, '0')
      const s = String(d.getSeconds()).padStart(2, '0')
      return `${h}:${m}:${s}`
    }
    return val
  } catch {
    return val
  }
}

function getBloodBadgeClass(bloodType?: string, solveRank?: number): string {
  if (bloodType === 'FIRST_BLOOD' || solveRank === 1) {
    return 'blood-first'
  }
  if (bloodType === 'SECOND_BLOOD' || solveRank === 2) {
    return 'blood-second'
  }
  if (bloodType === 'THIRD_BLOOD' || solveRank === 3) {
    return 'blood-third'
  }
  return 'blood-normal'
}

function getBloodLabel(bloodType?: string, solveRank?: number): string {
  if (bloodType === 'FIRST_BLOOD' || solveRank === 1) {
    return t('competition.notification_first_blood', '一血')
  }
  if (bloodType === 'SECOND_BLOOD' || solveRank === 2) {
    return t('competition.notification_second_blood', '二血')
  }
  if (bloodType === 'THIRD_BLOOD' || solveRank === 3) {
    return t('competition.notification_third_blood', '三血')
  }
  return t('competition.notification_solved', '攻克')
}

function getCategoryColorClass(cat?: string): string {
  const normalized = (cat || '').toLowerCase()
  switch (normalized) {
    case 'web':
      return 'text-google-blue dark:text-google-blue-dark bg-blue-500/10'
    case 'pwn':
      return 'text-google-red dark:text-google-red-dark bg-red-500/10'
    case 'reverse':
      return 'text-purple-600 dark:text-purple-400 bg-purple-500/10'
    case 'crypto':
      return 'text-google-yellow dark:text-google-yellow-dark bg-yellow-500/15'
    case 'misc':
      return 'text-google-green dark:text-google-green-dark bg-green-500/10'
    default:
      return 'text-google-text-desc dark:text-google-text-darkDesc bg-gray-500/10'
  }
}

function startTimer(): void {
  stopTimer()
  if (notifications.value.length <= 1 || isHovered.value) return
  rotationTimer = setInterval(() => {
    if (notifications.value.length > 1 && !isHovered.value) {
      currentIndex.value = (currentIndex.value + 1) % notifications.value.length
    }
  }, props.duration)
}

function stopTimer(): void {
  if (rotationTimer) {
    clearInterval(rotationTimer)
    rotationTimer = null
  }
}

function handleMouseEnter(): void {
  isHovered.value = true
  stopTimer()
}

function handleMouseLeave(): void {
  isHovered.value = false
  startTimer()
}

function handleIncomingSolve(payload: any): void {
  if (!payload) return
  if (props.competitionId && payload.competitionId && String(props.competitionId) !== String(payload.competitionId)) {
    return
  }

  const solveItem: CompetitionSolveNotificationItem = {
    id: payload.id || `${Date.now()}-${Math.random()}`,
    competitionId: payload.competitionId,
    challengeId: payload.challengeId,
    challengeTitle: payload.challengeTitle || payload.challengeName || '',
    challengeCategory: payload.challengeCategory || payload.category || '',
    teamId: payload.teamId,
    teamName: payload.teamName || '',
    teamAvatar: payload.teamAvatar || payload.teamAvatarUrl || null,
    solveRank: payload.solveRank || (payload.isFirstBlood ? 1 : undefined),
    bloodType: payload.bloodType || (payload.isFirstBlood ? 'FIRST_BLOOD' : 'NORMAL'),
    pointsAwarded: payload.pointsAwarded || payload.points || 0,
    solvedAt: payload.solvedAt || new Date().toISOString()
  }

  notifications.value.unshift(solveItem)
  if (notifications.value.length > 50) {
    notifications.value.pop()
  }

  currentIndex.value = 0
  startTimer()
}

function pushNotification(item: CompetitionSolveNotificationItem): void {
  notifications.value.unshift(item)
  if (notifications.value.length > 50) {
    notifications.value.pop()
  }
  currentIndex.value = 0
  startTimer()
}

watch(
  () => notifications.value.length,
  () => {
    startTimer()
  }
)

onMounted(() => {
  try {
    const sse = getGlobalSseClient()
    unsubSse = sse.on('competition_event', (data: any) => {
      handleIncomingSolve(data)
    })
    sse.connect()
  } catch (err) {
    console.warn('[VCompetitionNotification] SSE connection error:', err)
  }
  startTimer()
})

onBeforeUnmount(() => {
  stopTimer()
  unsubSse?.()
  unsubSse = null
})

defineExpose({
  pushNotification,
  notifications
})
</script>

<style scoped lang="scss">
.v-competition-notification {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 40px;
  box-sizing: border-box;
  padding: 0 12px;
  background-color: transparent !important;
  border: none;
  overflow: hidden;
  user-select: none;

  .notification-icon-wrap {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .notification-divider {
    width: 1px;
    height: 14px;
    background-color: #dadce0;
    flex-shrink: 0;
  }

  .notification-content {
    flex: 1;
    min-width: 0;
    height: 100%;
    display: flex;
    align-items: center;
    overflow: hidden;
    position: relative;

    .empty-state {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
      line-height: 12px;
      color: #5f6368;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;

      .live-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background-color: #34a853;
        flex-shrink: 0;
      }

      .empty-text {
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        line-height: 12px;
      }
    }

    .ticker-item {
      display: flex;
      align-items: center;
      gap: 8px;
      width: 100%;
      font-size: 12px;
      white-space: nowrap;

      .blood-badge {
        display: inline-flex;
        align-items: center;
        padding: 1px 6px;
        border-radius: 6px;
        font-size: 10px;
        font-weight: 700;
        font-family: 'Fira Code', monospace;
        letter-spacing: 0.5px;
        flex-shrink: 0;

        &.blood-first {
          background-color: rgba(234, 67, 53, 0.12);
          color: #ea4335;
          border: 1px solid rgba(234, 67, 53, 0.25);
        }

        &.blood-second {
          background-color: rgba(95, 99, 104, 0.12);
          color: #5f6368;
          border: 1px solid rgba(95, 99, 104, 0.25);
        }

        &.blood-third {
          background-color: rgba(251, 188, 4, 0.15);
          color: #ea8600;
          border: 1px solid rgba(251, 188, 4, 0.3);
        }

        &.blood-normal {
          background-color: rgba(26, 115, 232, 0.1);
          color: #1a73e8;
          border: 1px solid rgba(26, 115, 232, 0.2);
        }
      }

      .team-name {
        font-weight: 600;
        color: #202124;
        max-width: 140px;
        overflow: hidden;
        text-overflow: ellipsis;
        flex-shrink: 0;
      }

      .solve-action {
        color: #5f6368;
        flex-shrink: 0;
      }

      .category-tag {
        padding: 0 4px;
        border-radius: 4px;
        font-size: 10px;
        font-family: 'Fira Code', monospace;
        font-weight: 600;
        text-transform: uppercase;
        flex-shrink: 0;
      }

      .challenge-title {
        font-weight: 500;
        color: #1a73e8;
        max-width: 160px;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .points-tag {
        font-family: 'Fira Code', monospace;
        font-weight: 700;
        color: #34a853;
        margin-left: auto;
        flex-shrink: 0;
      }

      .solve-time {
        font-family: 'Fira Code', monospace;
        font-size: 11px;
        color: #80868b;
        flex-shrink: 0;
      }
    }
  }

  .notification-counter {
    font-family: 'Fira Code', monospace;
    font-size: 10px;
    color: #80868b;
    flex-shrink: 0;
  }
}

html.dark .v-competition-notification {
  .notification-divider {
    background-color: #3c4043;
  }

  .notification-content {
    .empty-state {
      color: #9aa0a6;

      .live-dot {
        background-color: #81c995;
      }
    }

    .ticker-item {
      .blood-badge {
        &.blood-first {
          background-color: rgba(242, 139, 130, 0.15);
          color: #f28b82;
          border-color: rgba(242, 139, 130, 0.3);
        }

        &.blood-second {
          background-color: rgba(189, 193, 198, 0.15);
          color: #bdc1c6;
          border-color: rgba(189, 193, 198, 0.3);
        }

        &.blood-third {
          background-color: rgba(253, 214, 99, 0.18);
          color: #fdd663;
          border-color: rgba(253, 214, 99, 0.35);
        }

        &.blood-normal {
          background-color: rgba(168, 199, 250, 0.12);
          color: #a8c7fa;
          border-color: rgba(168, 199, 250, 0.25);
        }
      }

      .team-name {
        color: #e3e3e3;
      }

      .solve-action {
        color: #9aa0a6;
      }

      .challenge-title {
        color: #a8c7fa;
      }

      .points-tag {
        color: #81c995;
      }

      .solve-time {
        color: #9aa0a6;
      }
    }
  }

  .notification-counter {
    color: #9aa0a6;
  }
}

.ticker-slide-enter-active,
.ticker-slide-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.ticker-slide-enter-from {
  transform: translateY(100%);
  opacity: 0;
}

.ticker-slide-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}
</style>
