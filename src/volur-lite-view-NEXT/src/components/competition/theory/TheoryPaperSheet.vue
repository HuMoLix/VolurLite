<template>
  <div class="theory-paper-sheet bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark rounded-2xl p-5 shadow-sm transition-colors duration-200 flex flex-col gap-5">
    <div class="flex items-center justify-between pb-3 border-b border-google-border-light/70 dark:border-google-border-dark/70">
      <div class="flex items-center gap-2">
        <n-icon :size="18" :component="GridOutline" class="text-google-blue dark:text-google-blue-dark" />
        <h3 class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('competition.theory_card_jump', '答题卡') }}
        </h3>
      </div>

      <div class="text-xs font-mono font-medium text-google-text-desc dark:text-google-text-darkDesc">
        {{ answeredCount }} / {{ questions.length }}
      </div>
    </div>

    <div class="space-y-1.5">
      <div class="flex items-center justify-between text-xs text-google-text-muted dark:text-google-text-darkMuted">
        <span>{{ t('competition.theory_answered_progress', '答题进度') }}</span>
        <span class="font-mono">{{ progressPercentage }}%</span>
      </div>
      <div class="w-full h-1.5 rounded-full bg-google-surface-light dark:bg-google-surface-dark overflow-hidden">
        <div
          class="h-full bg-google-blue dark:bg-google-blue-dark rounded-full transition-all duration-300"
          :style="{ width: `${progressPercentage}%` }"
        />
      </div>
    </div>

    <div class="flex items-center justify-between text-[11px] text-google-text-muted dark:text-google-text-darkMuted px-1">
      <div class="flex items-center gap-1.5">
        <span class="w-2.5 h-2.5 rounded-full bg-google-blue dark:bg-google-blue-dark" />
        <span>{{ t('competition.theory_card_answered', '已答') }} ({{ answeredCount }})</span>
      </div>
      <div class="flex items-center gap-1.5">
        <span class="w-2.5 h-2.5 rounded-full bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light dark:border-google-border-dark" />
        <span>{{ t('competition.theory_card_unanswered', '未答') }} ({{ unansweredCount }})</span>
      </div>
      <div class="flex items-center gap-1.5">
        <span class="w-2.5 h-2.5 rounded-full bg-google-yellow dark:bg-google-yellow-dark" />
        <span>{{ t('competition.theory_card_marked', '待查') }} ({{ markedCount }})</span>
      </div>
    </div>

    <div class="grid grid-cols-5 gap-2 max-h-[340px] overflow-y-auto pr-1">
      <button
        v-for="(q, idx) in questions"
        :key="q.paperId"
        type="button"
        class="relative h-10 rounded-xl font-mono text-xs font-medium transition-all flex items-center justify-center cursor-pointer border"
        :class="getQuestionButtonClass(q.paperId, idx)"
        @click="emit('select-question', idx)"
      >
        <span>{{ idx + 1 }}</span>
        <span
          v-if="isQuestionMarked(q.paperId)"
          class="absolute top-1 right-1 w-2 h-2 rounded-full bg-google-yellow dark:bg-google-yellow-dark"
        />
      </button>
    </div>

    <div class="pt-3 border-t border-google-border-light/70 dark:border-google-border-dark/70 space-y-3">
      <div v-if="isSubmitted" class="p-3 rounded-xl bg-green-50 dark:bg-green-950/40 border border-green-200/60 dark:border-green-900/50 text-center">
        <div class="text-xs text-google-green dark:text-google-green-dark font-medium">
          {{ t('competition.theory_submitted_tip', '您已完成本次考核答卷提交') }}
        </div>
        <div v-if="myTotalScore !== null && myTotalScore !== undefined" class="text-base font-bold text-google-green dark:text-google-green-dark mt-1 font-mono">
          {{ myTotalScore }} / {{ totalScore }} {{ t('competition.theory_score_unit', '分') }}
        </div>
      </div>

      <button
        v-else
        type="button"
        class="w-full h-10 px-4 rounded-xl text-xs font-semibold inline-flex items-center justify-center gap-2 transition-all cursor-pointer shadow-sm text-white bg-google-blue hover:bg-google-blue-hover active:bg-google-blue-hover disabled:opacity-50 disabled:cursor-not-allowed dark:bg-google-blue-dark dark:text-google-surface-dark dark:hover:bg-blue-300"
        :disabled="submitting"
        @click="emit('submit')"
      >
        <n-icon v-if="submitting" :size="14" class="animate-spin" :component="SyncOutline" />
        <n-icon v-else :size="14" :component="CheckmarkCircleOutline" />
        <span>{{ submitting ? t('common.submitting', '提交中...') : t('competition.theory_submit_paper', '提交答卷') }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { GridOutline, CheckmarkCircleOutline, SyncOutline } from '@vicons/ionicons5'
import type { TheoryPaperQuestionVO } from '@/types/competition'

defineOptions({
  name: 'TheoryPaperSheet'
})

const props = withDefaults(
  defineProps<{
    questions: TheoryPaperQuestionVO[]
    answers?: Record<string | number, string>
    markedIds?: (string | number)[] | Set<string | number>
    currentIndex: number
    isSubmitted?: boolean
    totalScore?: number
    myTotalScore?: number | null
    submitting?: boolean
  }>(),
  {
    answers: () => ({}),
    markedIds: () => [],
    isSubmitted: false,
    totalScore: 100,
    myTotalScore: null,
    submitting: false
  }
)

const emit = defineEmits<{
  'select-question': [index: number]
  submit: []
}>()

const { t } = useI18n()

const markedIdSet = computed<Set<string | number>>(() => {
  if (props.markedIds instanceof Set) {
    return props.markedIds
  }
  return new Set(props.markedIds || [])
})

const answeredCount = computed<number>(() => {
  let count = 0
  for (const q of props.questions) {
    const ans = props.answers[q.paperId]
    if (ans && ans.trim().length > 0) {
      count++
    }
  }
  return count
})

const unansweredCount = computed<number>(() => {
  return Math.max(0, props.questions.length - answeredCount.value)
})

const markedCount = computed<number>(() => {
  let count = 0
  for (const q of props.questions) {
    if (markedIdSet.value.has(q.paperId)) {
      count++
    }
  }
  return count
})

const progressPercentage = computed<number>(() => {
  if (props.questions.length === 0) return 0
  return Math.round((answeredCount.value / props.questions.length) * 100)
})

function isQuestionMarked(id: string | number): boolean {
  return markedIdSet.value.has(id)
}

function getQuestionButtonClass(id: string | number, idx: number): string {
  const isCurrent = props.currentIndex === idx
  const ans = props.answers[id]
  const isAnswered = ans && ans.trim().length > 0

  if (isCurrent) {
    if (isAnswered) {
      return 'bg-google-blue text-white border-google-blue ring-2 ring-google-blue/30 dark:bg-google-blue-dark dark:text-google-surface-dark dark:ring-blue-400/40'
    }
    return 'bg-blue-50 text-google-blue border-google-blue ring-2 ring-google-blue/30 dark:bg-blue-950/60 dark:text-google-blue-dark dark:ring-blue-400/40'
  }

  if (isAnswered) {
    return 'bg-blue-50/70 text-google-blue border-blue-200/60 dark:bg-blue-950/40 dark:text-google-blue-dark dark:border-blue-900/50 hover:bg-blue-100/70 dark:hover:bg-blue-950/70'
  }

  return 'bg-white dark:bg-google-surface-darkCard text-google-text-desc dark:text-google-text-darkDesc border-google-border-light dark:border-google-border-dark hover:bg-google-surface-light dark:hover:bg-google-surface-darkElevated'
}
</script>
