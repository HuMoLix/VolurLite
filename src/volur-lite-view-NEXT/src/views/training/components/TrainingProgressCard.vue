<template>
  <div class="side-panel">
    <div class="panel-head">
      <span class="panel-icon">
        <n-icon :size="17" :component="PieChartOutline" />
      </span>
      <div class="min-w-0 flex-1">
        <h3 class="panel-title">{{ t('training.direction_progress', '方向进度') }}</h3>
        <p class="panel-sub">
          {{ progress?.solvedCount ?? 0 }} / {{ progress?.totalChallenges ?? 0 }}
          {{ t('training.solved_unit', '题') }}
        </p>
      </div>
      <span class="percent-pill">{{ overallPercent }}%</span>
    </div>

    <div v-if="loading && !progress" class="mt-4 space-y-3">
      <div v-for="i in 3" :key="i" class="space-y-1.5">
        <div class="flex items-center justify-between">
          <span class="sk sk-label" />
          <span class="sk sk-count" />
        </div>
        <span class="sk sk-bar" />
      </div>
    </div>

    <div v-else-if="!progress?.categories?.length" class="panel-empty">
      <n-icon :size="26" :component="PieChartOutline" />
      <span>{{ t('training.direction_progress_empty', '解出题目后，这里会显示各方向的完成进度') }}</span>
    </div>

    <div v-else class="mt-4 space-y-3.5">
      <div v-for="cat in progress.categories" :key="cat.category" class="cat-item">
        <div class="cat-head">
          <span class="cat-dot" :class="`tone-${categoryTone(cat.category)}`" />
          <span class="cat-name">{{ cat.category || t('training.category_misc', '杂项') }}</span>
          <span class="cat-count">{{ cat.solved }}/{{ cat.total }}</span>
        </div>
        <div class="track">
          <div
            class="fill"
            :class="`tone-${categoryTone(cat.category)}`"
            :style="{ width: `${categoryPercent(cat)}%` }"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { PieChartOutline } from '@vicons/ionicons5'
import type { TrainingCategoryProgressVO, TrainingProgressVO } from '@/types/training'

defineOptions({
  name: 'TrainingProgressCard'
})

const props = withDefaults(
  defineProps<{
    progress: TrainingProgressVO | null
    loading?: boolean
  }>(),
  {
    loading: false
  }
)

const { t } = useI18n()

const overallPercent = computed<number>(() => {
  const total = props.progress?.totalChallenges ?? 0
  const solved = props.progress?.solvedCount ?? 0
  if (total <= 0) return 0
  return Math.min(100, Math.round((solved / total) * 100))
})

function categoryPercent(cat: TrainingCategoryProgressVO): number {
  if (!cat.total) return 0
  return Math.min(100, Math.round((cat.solved / cat.total) * 100))
}

function categoryTone(category?: string): string {
  const normalized = (category || '').toLowerCase()
  if (normalized.includes('web')) return 'blue'
  if (normalized.includes('pwn') || normalized.includes('binary')) return 'red'
  if (normalized.includes('reverse') || normalized.includes('re')) return 'purple'
  if (normalized.includes('crypto')) return 'amber'
  if (normalized.includes('misc')) return 'green'
  return 'slate'
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
  color: #1a73e8;
  background: rgba(26, 115, 232, 0.1);
}

html.dark .panel-icon {
  color: #a8c7fa;
  background: rgba(168, 199, 250, 0.12);
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
  letter-spacing: 0.04em;
  color: #9aa0a6;
}

.percent-pill {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 9999px;
  font-family: 'Fira Code', ui-monospace, monospace;
  font-size: 10px;
  font-weight: 700;
  color: #1a73e8;
  background: rgba(26, 115, 232, 0.09);
}

html.dark .percent-pill {
  color: #a8c7fa;
  background: rgba(168, 199, 250, 0.1);
}

.cat-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  font-size: 11px;
  line-height: 15px;
}

.cat-dot {
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  flex-shrink: 0;

  &.tone-blue {
    background: #1a73e8;
  }

  &.tone-red {
    background: #d93025;
  }

  &.tone-purple {
    background: #8e44d9;
  }

  &.tone-amber {
    background: #f2a600;
  }

  &.tone-green {
    background: #34a853;
  }

  &.tone-slate {
    background: #9aa0a6;
  }
}

html.dark .cat-dot {
  &.tone-blue {
    background: #a8c7fa;
  }

  &.tone-red {
    background: #f28b82;
  }

  &.tone-purple {
    background: #d0a8ff;
  }

  &.tone-amber {
    background: #fdd663;
  }

  &.tone-green {
    background: #81c995;
  }

  &.tone-slate {
    background: #9aa0a6;
  }
}

.cat-name {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #5f6368;
}

html.dark .cat-name {
  color: #bdc1c6;
}

.cat-count {
  flex-shrink: 0;
  font-family: 'Fira Code', ui-monospace, monospace;
  font-size: 10px;
  color: #80868b;
}

html.dark .cat-count {
  color: #9aa0a6;
}

.track {
  height: 5px;
  border-radius: 9999px;
  background: #f1f3f4;
  overflow: hidden;
}

html.dark .track {
  background: #232427;
}

.fill {
  height: 100%;
  border-radius: 9999px;
  transition: width 0.4s cubic-bezier(0.2, 0, 0, 1);

  &.tone-blue {
    background: linear-gradient(90deg, #1a73e8, #6aa9ff);
  }

  &.tone-red {
    background: linear-gradient(90deg, #d93025, #f28b82);
  }

  &.tone-purple {
    background: linear-gradient(90deg, #8e44d9, #c58af9);
  }

  &.tone-amber {
    background: linear-gradient(90deg, #f2a600, #fdd663);
  }

  &.tone-green {
    background: linear-gradient(90deg, #34a853, #81c995);
  }

  &.tone-slate {
    background: linear-gradient(90deg, #9aa0a6, #c4c7cc);
  }
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

.sk {
  display: block;
  border-radius: 6px;
  background: linear-gradient(90deg, #f1f3f4 25%, #e8eaed 37%, #f1f3f4 63%);
  background-size: 400% 100%;
  animation: progress-shimmer 1.4s ease infinite;
}

html.dark .sk {
  background: linear-gradient(90deg, #1e1f20 25%, #2a2b2e 37%, #1e1f20 63%);
  background-size: 400% 100%;
}

.sk-label {
  width: 72px;
  height: 10px;
}

.sk-count {
  width: 30px;
  height: 10px;
}

.sk-bar {
  width: 100%;
  height: 5px;
}

@keyframes progress-shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
