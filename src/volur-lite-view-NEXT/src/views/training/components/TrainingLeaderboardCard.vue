<template>
  <div class="side-panel">
    <div class="panel-head">
      <span class="panel-icon is-amber">
        <n-icon :size="17" :component="TrophyOutline" />
      </span>
      <div class="min-w-0 flex-1">
        <h3 class="panel-title">{{ t('training.leaderboard', '训练排行榜') }}</h3>
        <p class="panel-sub">{{ t('training.leaderboard_top', 'TOP 8') }}</p>
      </div>
    </div>

    <div v-if="loading && !leaderboard" class="mt-4 space-y-2">
      <div v-for="i in 5" :key="i" class="skeleton-row">
        <span class="sk sk-rank" />
        <span class="sk sk-avatar" />
        <span class="sk sk-line" />
        <span class="sk sk-points" />
      </div>
    </div>

    <div v-else-if="!leaderboard?.items?.length" class="panel-empty">
      <n-icon :size="26" :component="TrophyOutline" />
      <span>{{ t('training.leaderboard_empty', '暂无排行数据，快来成为第一位解题者') }}</span>
    </div>

    <template v-else>
      <div class="mt-3 space-y-1">
        <div
          v-for="item in leaderboard.items"
          :key="String(item.userId)"
          class="rank-row"
          :class="{ 'is-me': isMe(item) }"
        >
          <span class="rank-badge" :class="rankClass(item.rank)">
            {{ item.rank }}
          </span>
          <VAvatar
            :src="item.avatar || ''"
            :text="getInitial(item.username)"
            :size="28"
            shape="circle"
            class="shrink-0"
          />
          <div class="min-w-0 flex-1">
            <div class="rank-name">
              {{ item.username }}
              <span v-if="isMe(item)" class="me-tag">{{ t('training.you', '你') }}</span>
            </div>
            <div class="rank-sub">
              {{ item.solvedCount }} {{ t('training.solved_unit', '题') }}
            </div>
          </div>
          <div class="rank-points">
            {{ item.points }}
            <small>pts</small>
          </div>
        </div>
      </div>

      <div
        v-if="myRank && !myRankVisible"
        class="my-rank-card"
      >
        <span class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted">
          {{ t('training.my_rank', '我的排名') }}
        </span>
        <span class="font-mono text-xs font-bold text-google-text-primary dark:text-google-text-darkPrimary">
          #{{ myRank.rank }}
          <span class="font-normal text-google-text-muted dark:text-google-text-darkMuted ml-1">
            {{ myRank.points }} pts
          </span>
        </span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { TrophyOutline } from '@vicons/ionicons5'
import { VAvatar } from '@/components/common'
import type { TrainingLeaderboardItemVO, TrainingLeaderboardVO } from '@/types/training'

defineOptions({
  name: 'TrainingLeaderboardCard'
})

const props = withDefaults(
  defineProps<{
    leaderboard: TrainingLeaderboardVO | null
    loading?: boolean
    myUsername?: string
  }>(),
  {
    loading: false,
    myUsername: ''
  }
)

const { t } = useI18n()

const myRank = computed(() => props.leaderboard?.myRank ?? null)

const myRankVisible = computed<boolean>(() => {
  if (!props.myUsername) return false
  return (props.leaderboard?.items || []).some((item) => item.username === props.myUsername)
})

function isMe(item: TrainingLeaderboardItemVO): boolean {
  return Boolean(props.myUsername) && item.username === props.myUsername
}

function getInitial(name?: string): string {
  if (!name) return 'U'
  return name.trim().charAt(0).toUpperCase() || 'U'
}

function rankClass(rank: number): string {
  if (rank === 1) return 'is-gold'
  if (rank === 2) return 'is-silver'
  if (rank === 3) return 'is-bronze'
  return ''
}
</script>

<style scoped lang="scss">
.side-panel {
  padding: 18px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #e5e8ec;
  box-shadow: 0 1px 2px rgba(60, 64, 67, 0.04);
}

html.dark .side-panel {
  background: #0e0e0e;
  border-color: #2a2b2e;
  box-shadow: none;
}

.panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  flex-shrink: 0;

  &.is-amber {
    color: #b06000;
    background: rgba(251, 188, 4, 0.14);
  }
}

html.dark .panel-icon.is-amber {
  color: #fdd663;
  background: rgba(253, 214, 99, 0.12);
}

.panel-title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  line-height: 18px;
  color: #202124;
}

html.dark .panel-title {
  color: #e8eaed;
}

.panel-sub {
  margin: 1px 0 0;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #9aa0a6;
}

.panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 26px 12px 18px;
  text-align: center;
  font-size: 11px;
  line-height: 17px;
  color: #9aa0a6;

  :deep(svg) {
    color: #c4c7cc;
  }
}

html.dark .panel-empty {
  color: #80868b;

  :deep(svg) {
    color: #4a4d52;
  }
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 6px 8px;
  border-radius: 11px;
  transition: background-color 0.15s ease;

  &:hover {
    background: #f8f9fa;
  }

  &.is-me {
    background: rgba(26, 115, 232, 0.07);
  }
}

html.dark .rank-row {
  &:hover {
    background: #1a1b1d;
  }

  &.is-me {
    background: rgba(168, 199, 250, 0.09);
  }
}

.rank-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 8px;
  flex-shrink: 0;
  font-family: 'Fira Code', ui-monospace, monospace;
  font-size: 10px;
  font-weight: 700;
  color: #5f6368;
  background: #f1f3f4;

  &.is-gold {
    color: #8a5b00;
    background: linear-gradient(135deg, #fff3c4, #fdd663);
  }

  &.is-silver {
    color: #4b5563;
    background: linear-gradient(135deg, #f3f4f6, #d7dbe0);
  }

  &.is-bronze {
    color: #9a3412;
    background: linear-gradient(135deg, #ffedd5, #fdba74);
  }
}

html.dark .rank-badge {
  color: #9aa0a6;
  background: #232427;

  &.is-gold {
    color: #3d2a00;
    background: linear-gradient(135deg, #fdd663, #e3a400);
  }

  &.is-silver {
    color: #2b3038;
    background: linear-gradient(135deg, #d7dbe0, #9aa3ad);
  }

  &.is-bronze {
    color: #3d1a00;
    background: linear-gradient(135deg, #fdba74, #d97706);
  }
}

.rank-name {
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
  color: #3c4043;
}

html.dark .rank-name {
  color: #bdc1c6;
}

.rank-sub {
  font-size: 10px;
  line-height: 14px;
  color: #9aa0a6;
}

.me-tag {
  flex-shrink: 0;
  padding: 0 5px;
  border-radius: 9999px;
  font-size: 9px;
  font-weight: 700;
  line-height: 15px;
  color: #1a73e8;
  background: rgba(26, 115, 232, 0.12);
}

html.dark .me-tag {
  color: #a8c7fa;
  background: rgba(168, 199, 250, 0.14);
}

.rank-points {
  flex-shrink: 0;
  font-family: 'Fira Code', ui-monospace, monospace;
  font-size: 12px;
  font-weight: 700;
  color: #1a73e8;

  small {
    font-size: 9px;
    font-weight: 500;
    opacity: 0.65;
    margin-left: 1px;
  }
}

html.dark .rank-points {
  color: #a8c7fa;
}

.my-rank-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(26, 115, 232, 0.06);
  border: 1px dashed rgba(26, 115, 232, 0.25);
}

html.dark .my-rank-card {
  background: rgba(168, 199, 250, 0.07);
  border-color: rgba(168, 199, 250, 0.22);
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 6px 8px;
}

.sk {
  display: block;
  border-radius: 6px;
  background: linear-gradient(90deg, #f1f3f4 25%, #e8eaed 37%, #f1f3f4 63%);
  background-size: 400% 100%;
  animation: panel-shimmer 1.4s ease infinite;
}

html.dark .sk {
  background: linear-gradient(90deg, #1e1f20 25%, #2a2b2e 37%, #1e1f20 63%);
  background-size: 400% 100%;
}

.sk-rank {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.sk-avatar {
  width: 28px;
  height: 28px;
  border-radius: 9999px;
  flex-shrink: 0;
}

.sk-line {
  flex: 1;
  height: 10px;
}

.sk-points {
  width: 38px;
  height: 10px;
  flex-shrink: 0;
}

@keyframes panel-shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
