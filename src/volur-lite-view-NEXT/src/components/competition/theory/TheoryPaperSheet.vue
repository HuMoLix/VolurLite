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

    <div class="space-y-4 max-h-[360px] overflow-y-auto pr-1">
      <div
        v-for="group in questionGroups"
        :key="group.type"
        class="space-y-2"
      >
        <div class="flex items-center justify-between text-xs font-medium text-google-text-desc dark:text-google-text-darkDesc px-0.5">
          <span>{{ group.title }}</span>
          <span class="font-mono text-[11px] text-google-text-muted dark:text-google-text-darkMuted">
            ({{ group.items.length }})
          </span>
        </div>
        <div class="grid grid-cols-5 gap-2">
          <VBtn
            v-for="item in group.items"
            :key="item.question.paperId"
            :clz="isCurrentAndAnswered(item.question.paperId, item.index) ? 'primary' : 'normal'"
            :radius="12"
            class="theory-sheet-item font-mono text-xs font-medium w-full relative"
            :class="getQuestionButtonClass(item.question.paperId, item.index)"
            @click="emit('select-question', item.index)"
          >
            <span>{{ item.index + 1 }}</span>
            <span
              v-if="isQuestionMarked(item.question.paperId)"
              class="absolute top-1 right-1 w-2 h-2 rounded-full bg-google-yellow dark:bg-google-yellow-dark"
            />
          </VBtn>
        </div>
      </div>
    </div>

    <div class="pt-3 border-t border-google-border-light/70 dark:border-google-border-dark/70 space-y-3">
      <div v-if="isSubmitted" class="p-3 rounded-xl bg-green-50 dark:bg-green-950/40 border border-green-200/60 dark:border-green-900/50 text-center space-y-1">
        <div class="text-xs text-google-green dark:text-google-green-dark font-medium">
          {{ t('competition.theory_submitted_tip', '您已完成本次考核答卷提交') }}
        </div>
        <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
          {{ t('competition.theory_score_hidden_notice', '答卷已封存归档，最终成绩由管理员在后台统一核定') }}
        </div>
      </div>

      <VBtn
        v-else
        clz="primary"
        block
        :disabled="submitting"
        :loading="submitting"
        :left-icon="CheckmarkCircleOutline"
        @click="emit('submit')"
      >
        {{ submitting ? t('common.submitting', '提交中...') : t('competition.theory_submit_paper', '提交答卷') }}
      </VBtn>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { GridOutline, CheckmarkCircleOutline } from '@vicons/ionicons5'
import { VBtn } from '@/components/common'
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

interface QuestionGroupItem {
  question: TheoryPaperQuestionVO
  index: number
}

interface QuestionGroup {
  type: string
  title: string
  items: QuestionGroupItem[]
}

const questionGroups = computed<QuestionGroup[]>(() => {
  const singleList: QuestionGroupItem[] = []
  const multipleList: QuestionGroupItem[] = []
  const judgeList: QuestionGroupItem[] = []
  const essayList: QuestionGroupItem[] = []
  const otherList: QuestionGroupItem[] = []

  props.questions.forEach((q, idx) => {
    const item = { question: q, index: idx }
    if (q.type === 'SINGLE_CHOICE') {
      singleList.push(item)
    } else if (q.type === 'MULTIPLE_CHOICE') {
      multipleList.push(item)
    } else if (q.type === 'TRUE_FALSE') {
      judgeList.push(item)
    } else if (q.type === 'ESSAY') {
      essayList.push(item)
    } else {
      otherList.push(item)
    }
  })

  const groups: QuestionGroup[] = []

  if (singleList.length > 0) {
    groups.push({
      type: 'SINGLE_CHOICE',
      title: t('competition.theory_single_choice', '单选题'),
      items: singleList
    })
  }

  if (multipleList.length > 0) {
    groups.push({
      type: 'MULTIPLE_CHOICE',
      title: t('competition.theory_multiple_choice', '多选题'),
      items: multipleList
    })
  }

  if (judgeList.length > 0) {
    groups.push({
      type: 'TRUE_FALSE',
      title: t('competition.theory_true_false', '判断题'),
      items: judgeList
    })
  }

  if (essayList.length > 0) {
    groups.push({
      type: 'ESSAY',
      title: t('competition.theory_essay', '简答题'),
      items: essayList
    })
  }

  if (otherList.length > 0) {
    groups.push({
      type: 'OTHER',
      title: t('competition.theory_other', '其他题型'),
      items: otherList
    })
  }

  return groups
})

function isQuestionMarked(id: string | number): boolean {
  return markedIdSet.value.has(id)
}

function isCurrentAndAnswered(id: string | number, idx: number): boolean {
  if (props.currentIndex !== idx) return false
  const ans = props.answers?.[id]
  return Boolean(ans && ans.trim().length > 0)
}

function getQuestionButtonClass(id: string | number, idx: number): string {
  const isCurrent = props.currentIndex === idx
  const ans = props.answers?.[id]
  const isAnswered = Boolean(ans && ans.trim().length > 0)

  if (isCurrent) {
    if (isAnswered) {
      return 'ring-2 ring-google-blue/30 dark:ring-blue-400/40'
    }
    return '!bg-blue-50/80 !text-google-blue !border-google-blue ring-2 ring-google-blue/30 dark:!bg-blue-950/60 dark:!text-google-blue-dark dark:ring-blue-400/40'
  }

  if (isAnswered) {
    return '!bg-blue-50/70 !text-google-blue !border-blue-200/80 dark:!bg-blue-950/40 dark:!text-google-blue-dark dark:!border-blue-900/50 hover:!bg-blue-100/70 dark:hover:!bg-blue-950/70'
  }

  return ''
}
</script>

<style scoped lang="scss">
:deep(.theory-sheet-item) {
  height: 40px !important;
  min-height: 40px !important;
  padding: 0 !important;
  font-family: monospace, monospace;
}
</style>
