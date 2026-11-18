<template>
  <article
    class="training-card"
    :class="{ 'is-solved': challenge.solvedByMe }"
    role="button"
    tabindex="0"
    @click="emit('click', challenge)"
    @keydown.enter="emit('click', challenge)"
  >
    <div class="card-top">
      <span class="category-tile" :class="`tone-${categoryTone}`">
        <n-icon :size="18" :component="categoryIcon" />
      </span>
      <span class="category-name">{{ challenge.categoryName || t('training.category_misc', '杂项') }}</span>
      <span class="points-pill">
        {{ challenge.points }}
        <small>pts</small>
      </span>
    </div>

    <h3 class="card-title">
      {{ challenge.name }}
    </h3>

    <div class="card-foot">
      <span class="difficulty-pill" :class="difficultyClass">
        <i class="difficulty-dot" />
        {{ difficultyLabel }}
      </span>

      <span class="solves">
        <n-icon :size="14" :component="PeopleOutline" />
        {{ challenge.solvedCount }} {{ t('training.solves_suffix', '人解出') }}
      </span>

      <span class="ml-auto flex items-center gap-2 shrink-0">
        <VTooltip
          v-if="challenge.dynamicContainer"
          :content="t('training.container_tip', '该题提供动态靶机环境')"
        >
          <span class="docker-mark">
            <n-icon :size="16" :component="LogoDocker" />
          </span>
        </VTooltip>

        <span v-if="challenge.solvedByMe" class="solved-badge">
          <n-icon :size="14" :component="CheckmarkCircle" />
          {{ t('training.solved_badge', '已解出') }}
        </span>
        <n-icon v-else :size="16" :component="ArrowForwardOutline" class="card-arrow" />
      </span>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import {
  GlobeOutline,
  TerminalOutline,
  GitBranchOutline,
  KeyOutline,
  ExtensionPuzzleOutline,
  FolderOutline,
  LogoDocker,
  CheckmarkCircle,
  ArrowForwardOutline,
  PeopleOutline
} from '@vicons/ionicons5'
import { VTooltip } from '@/components/common'
import type { TrainingChallengeVO } from '@/types/training'

defineOptions({
  name: 'TrainingChallengeCard'
})

const props = defineProps<{
  challenge: TrainingChallengeVO
}>()

const emit = defineEmits<{
  (e: 'click', challenge: TrainingChallengeVO): void
}>()

const { t } = useI18n()

const categoryTone = computed<string>(() => {
  const normalized = (props.challenge.categoryName || '').toLowerCase()
  if (normalized.includes('web')) return 'blue'
  if (normalized.includes('pwn') || normalized.includes('binary')) return 'red'
  if (normalized.includes('reverse') || normalized.includes('re')) return 'purple'
  if (normalized.includes('crypto')) return 'amber'
  if (normalized.includes('misc')) return 'green'
  return 'slate'
})

const categoryIcon = computed<Component>(() => {
  const normalized = (props.challenge.categoryName || '').toLowerCase()
  if (normalized.includes('web')) return GlobeOutline
  if (normalized.includes('pwn') || normalized.includes('binary')) return TerminalOutline
  if (normalized.includes('reverse') || normalized.includes('re')) return GitBranchOutline
  if (normalized.includes('crypto')) return KeyOutline
  if (normalized.includes('misc')) return ExtensionPuzzleOutline
  return FolderOutline
})

const difficultyMeta = computed<{ label: string; cls: string }>(() => {
  const raw = String(props.challenge.difficulty || '').toLowerCase()
  switch (raw) {
    case 'easy':
      return { label: t('training.difficulty_easy', '入门'), cls: 'is-easy' }
    case 'normal':
      return { label: t('training.difficulty_normal', '简单'), cls: 'is-normal' }
    case 'mid':
      return { label: t('training.difficulty_mid', '中等'), cls: 'is-mid' }
    case 'hard':
      return { label: t('training.difficulty_hard', '进阶'), cls: 'is-hard' }
    case 'difficult':
      return { label: t('training.difficulty_difficult', '困难'), cls: 'is-difficult' }
    default:
      return { label: t('training.difficulty_unknown', '未分级'), cls: 'is-unknown' }
  }
})

const difficultyLabel = computed<string>(() => difficultyMeta.value.label)
const difficultyClass = computed<string>(() => difficultyMeta.value.cls)
</script>

<style scoped lang="scss">
.training-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 184px;
  padding: 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #e5e8ec;
  box-shadow: 0 1px 2px rgba(60, 64, 67, 0.04);
  cursor: pointer;
  overflow: hidden;
  transition:
    transform 0.22s cubic-bezier(0.2, 0, 0, 1),
    box-shadow 0.22s cubic-bezier(0.2, 0, 0, 1),
    border-color 0.22s cubic-bezier(0.2, 0, 0, 1);

  &::after {
    content: '';
    position: absolute;
    left: 20px;
    right: 20px;
    bottom: 0;
    height: 2px;
    border-radius: 2px 2px 0 0;
    background: linear-gradient(90deg, #1a73e8, #6aa9ff);
    transform: scaleX(0);
    transform-origin: left;
    transition: transform 0.28s cubic-bezier(0.2, 0, 0, 1);
  }

  &:hover {
    transform: translateY(-3px);
    border-color: rgba(26, 115, 232, 0.35);
    box-shadow: 0 12px 28px rgba(26, 115, 232, 0.12);
  }

  &:hover::after {
    transform: scaleX(1);
  }

  &:focus-visible {
    outline: 2px solid rgba(26, 115, 232, 0.65);
    outline-offset: 2px;
  }

  &.is-solved {
    border-color: rgba(52, 168, 83, 0.3);
    background: linear-gradient(180deg, rgba(52, 168, 83, 0.05), #fff 46%);
  }

  &.is-solved::after {
    background: linear-gradient(90deg, #34a853, #81c995);
  }
}

html.dark .training-card {
  background: #0e0e0e;
  border-color: #2a2b2e;
  box-shadow: none;

  &:hover {
    border-color: rgba(138, 180, 248, 0.4);
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.45);
  }

  &.is-solved {
    border-color: rgba(129, 201, 149, 0.28);
    background: linear-gradient(180deg, rgba(129, 201, 149, 0.06), #0e0e0e 46%);
  }
}

.card-top {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.category-tile {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  flex-shrink: 0;

  &.tone-blue {
    color: #1a73e8;
    background: rgba(26, 115, 232, 0.1);
  }

  &.tone-red {
    color: #d93025;
    background: rgba(217, 48, 37, 0.09);
  }

  &.tone-purple {
    color: #8e44d9;
    background: rgba(142, 68, 217, 0.09);
  }

  &.tone-amber {
    color: #b06000;
    background: rgba(251, 188, 4, 0.14);
  }

  &.tone-green {
    color: #1e8e3e;
    background: rgba(52, 168, 83, 0.1);
  }

  &.tone-slate {
    color: #5f6368;
    background: rgba(95, 99, 104, 0.08);
  }
}

html.dark .category-tile {
  &.tone-blue {
    color: #a8c7fa;
    background: rgba(168, 199, 250, 0.12);
  }

  &.tone-red {
    color: #f28b82;
    background: rgba(242, 139, 130, 0.12);
  }

  &.tone-purple {
    color: #d0a8ff;
    background: rgba(208, 168, 255, 0.12);
  }

  &.tone-amber {
    color: #fdd663;
    background: rgba(253, 214, 99, 0.12);
  }

  &.tone-green {
    color: #81c995;
    background: rgba(129, 201, 149, 0.12);
  }

  &.tone-slate {
    color: #9aa0a6;
    background: rgba(154, 160, 166, 0.12);
  }
}

.category-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
  color: #5f6368;
}

html.dark .category-name {
  color: #9aa0a6;
}

.points-pill {
  margin-left: auto;
  flex-shrink: 0;
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  padding: 3px 9px;
  border-radius: 9999px;
  font-family: 'Fira Code', ui-monospace, monospace;
  font-size: 13px;
  font-weight: 700;
  line-height: 18px;
  color: #1a73e8;
  background: #eef4ff;

  small {
    font-size: 9px;
    font-weight: 500;
    opacity: 0.7;
    letter-spacing: 0.02em;
  }
}

html.dark .points-pill {
  color: #a8c7fa;
  background: rgba(168, 199, 250, 0.12);
}

.card-title {
  margin: 18px 0 0;
  font-size: 17px;
  font-weight: 650;
  line-height: 24px;
  letter-spacing: -0.01em;
  color: #202124;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 48px;
  word-break: break-word;
}

html.dark .card-title {
  color: #e8eaed;
}

.card-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: auto;
  padding-top: 16px;
  font-size: 11px;
  color: #80868b;
}

html.dark .card-foot {
  color: #9aa0a6;
}

.difficulty-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
  flex-shrink: 0;

  .difficulty-dot {
    width: 5px;
    height: 5px;
    border-radius: 9999px;
    background: currentColor;
  }

  &.is-easy {
    color: #1e8e3e;
    background: rgba(52, 168, 83, 0.1);
  }

  &.is-normal {
    color: #1a73e8;
    background: rgba(26, 115, 232, 0.09);
  }

  &.is-mid {
    color: #b06000;
    background: rgba(251, 188, 4, 0.14);
  }

  &.is-hard {
    color: #c5520a;
    background: rgba(234, 134, 53, 0.12);
  }

  &.is-difficult {
    color: #c5221f;
    background: rgba(234, 67, 53, 0.1);
  }

  &.is-unknown {
    color: #5f6368;
    background: rgba(95, 99, 104, 0.08);
  }
}

html.dark .difficulty-pill {
  &.is-easy {
    color: #81c995;
    background: rgba(129, 201, 149, 0.12);
  }

  &.is-normal {
    color: #a8c7fa;
    background: rgba(168, 199, 250, 0.12);
  }

  &.is-mid {
    color: #fdd663;
    background: rgba(253, 214, 99, 0.12);
  }

  &.is-hard {
    color: #fcad70;
    background: rgba(252, 173, 112, 0.12);
  }

  &.is-difficult {
    color: #f28b82;
    background: rgba(242, 139, 130, 0.12);
  }

  &.is-unknown {
    color: #9aa0a6;
    background: rgba(154, 160, 166, 0.12);
  }
}

.solves {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.docker-mark {
  display: inline-flex;
  align-items: center;
  color: #2496ed;
  opacity: 0.9;
}

.card-arrow {
  color: #bdc1c6;
  transition: transform 0.18s ease, color 0.18s ease;
}

html.dark .card-arrow {
  color: #5f6368;
}

.training-card:hover .card-arrow {
  color: #1a73e8;
  transform: translateX(2px);
}

html.dark .training-card:hover .card-arrow {
  color: #a8c7fa;
}

.solved-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 7px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
  color: #1e8e3e;
  background: rgba(52, 168, 83, 0.1);
  flex-shrink: 0;
}

html.dark .solved-badge {
  color: #81c995;
  background: rgba(129, 201, 149, 0.12);
}
</style>
