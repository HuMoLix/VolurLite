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
          <div class="space-y-2.5">
            <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-50 dark:bg-emerald-950/60 text-google-green dark:text-google-green-dark text-xs font-semibold">
              <span class="w-2 h-2 rounded-full bg-google-green dark:bg-google-green-dark" />
              <span>{{ t('competition.type_theory', '理论知识考核') }}</span>
            </div>
            <h1 class="text-xl sm:text-2xl font-bold tracking-tight text-google-text-primary dark:text-google-text-darkPrimary">
              {{ competition?.title || t('competition.type_theory', '理论知识赛') }}
            </h1>
            <p class="text-xs sm:text-sm text-google-text-desc dark:text-google-text-darkDesc max-w-2xl leading-relaxed">
              {{ isSubmitted ? t('competition.theory_read_only_tip', '试卷已提交，当前处于只读复核状态') : (competition?.description || t('competition.theory_exam_desc', '请在规定时间内完成客观试题并提交答卷')) }}
            </p>
          </div>

          <div class="flex items-center flex-wrap gap-3 self-start md:self-auto">
            <div class="p-3 sm:p-4 rounded-xl sm:rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 text-center min-w-[90px]">
              <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted font-medium">
                {{ t('competition.theory_total_questions', '总题数') }}
              </div>
              <div class="text-lg font-bold font-mono text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">
                {{ questions.length }}
              </div>
            </div>

            <div class="p-3 sm:p-4 rounded-xl sm:rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 text-center min-w-[90px]">
              <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted font-medium">
                {{ t('competition.theory_total_score', '试卷总分') }}
              </div>
              <div class="text-lg font-bold font-mono text-google-text-primary dark:text-google-text-darkPrimary mt-0.5">
                {{ paper?.totalScore || 100 }}
              </div>
            </div>

            <div
              v-if="isSubmitted"
              class="p-3 sm:p-4 rounded-xl sm:rounded-2xl bg-green-50 dark:bg-green-950/50 border border-green-200/60 dark:border-green-900/60 text-center min-w-[90px]"
            >
              <div class="text-[11px] text-google-green dark:text-google-green-dark font-medium">
                {{ t('competition.theory_my_score', '最终得分') }}
              </div>
              <div class="text-lg font-bold font-mono text-google-green dark:text-google-green-dark mt-0.5">
                {{ paper?.myTotalScore ?? 0 }}
              </div>
            </div>
          </div>
        </div>
      </section>

      <div v-if="!loading && questions.length === 0" class="flex-1 flex items-center justify-center py-20">
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
            <button
              type="button"
              class="h-9 px-4 rounded-xl border text-xs font-semibold inline-flex items-center gap-2 transition-colors cursor-pointer border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard text-google-text-primary dark:text-google-text-darkPrimary hover:bg-google-surface-light dark:hover:bg-google-surface-darkElevated disabled:opacity-40 disabled:cursor-not-allowed"
              :disabled="currentIndex === 0"
              @click="handlePrevQuestion"
            >
              <n-icon :size="14" :component="ArrowBackOutline" />
              <span>{{ t('competition.theory_prev_question', '上一题') }}</span>
            </button>

            <div class="text-xs font-mono font-medium text-google-text-muted dark:text-google-text-darkMuted">
              {{ currentIndex + 1 }} / {{ questions.length }}
            </div>

            <button
              type="button"
              class="h-9 px-4 rounded-xl border text-xs font-semibold inline-flex items-center gap-2 transition-colors cursor-pointer border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard text-google-text-primary dark:text-google-text-darkPrimary hover:bg-google-surface-light dark:hover:bg-google-surface-darkElevated disabled:opacity-40 disabled:cursor-not-allowed"
              :disabled="currentIndex === questions.length - 1"
              @click="handleNextQuestion"
            >
              <span>{{ t('competition.theory_next_question', '下一题') }}</span>
              <n-icon :size="14" :component="ArrowForwardOutline" />
            </button>
          </div>
        </div>

        <aside class="w-full lg:w-80 flex-shrink-0 lg:sticky lg:top-24">
          <TheoryPaperSheet
            :questions="questions"
            :answers="answers"
            :marked-ids="markedIds"
            :current-index="currentIndex"
            :is-submitted="isSubmitted"
            :total-score="paper?.totalScore || 100"
            :my-total-score="paper?.myTotalScore"
            :submitting="submitting"
            @select-question="handleSelectQuestion"
            @submit="handleOpenSubmitConfirm"
          />
        </aside>
      </div>
    </div>

    <n-modal
      :show="submitConfirmVisible"
      preset="dialog"
      :title="t('competition.theory_submit_confirm', '确定提交答卷吗？')"
      :positive-text="t('common.confirm', '确认提交')"
      :negative-text="t('common.cancel', '继续作答')"
      :loading="submitting"
      @positive-click="handleConfirmSubmit"
      @negative-click="submitConfirmVisible = false"
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
    </n-modal>

    <TheoryResultModal
      :show="resultModalVisible"
      :result="examResult"
      @update:show="resultModalVisible = $event"
      @confirm="handleResultConfirmed"
    />
  </CompetitionLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, reactive, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon, NModal, useMessage } from 'naive-ui'
import {
  DocumentTextOutline,
  ArrowBackOutline,
  ArrowForwardOutline
} from '@vicons/ionicons5'
import CompetitionLayout from '@/components/layouts/CompetitionLayout.vue'
import {
  TheoryQuestionCard,
  TheoryPaperSheet,
  TheoryResultModal
} from '@/components/competition/theory'
import {
  getCompetitionByIdApi,
  getCompetitionBySlugApi,
  getTheoryPaperApi,
  submitTheoryPaperApi
} from '@/api/competition'
import type {
  CompetitionDetailVO,
  TheoryPaperExamVO,
  TheoryPaperQuestionVO,
  TheoryPaperResultVO,
  TheoryPaperSubmitRequestDTO
} from '@/types/competition'

defineOptions({
  name: 'TheoryCompetitionView'
})

const route = useRoute()
const { t } = useI18n()
const message = useMessage()

const loading = ref<boolean>(false)
const submitting = ref<boolean>(false)

const competition = ref<CompetitionDetailVO | null>(null)
const paper = ref<TheoryPaperExamVO | null>(null)

const answers = reactive<Record<string | number, string>>({})
const markedIds = ref<Set<string | number>>(new Set())
const currentIndex = ref<number>(0)

const submitConfirmVisible = ref<boolean>(false)
const resultModalVisible = ref<boolean>(false)
const examResult = ref<TheoryPaperResultVO | null>(null)

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
  if (!currentQuestion.value || isSubmitted.value) return
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
  if (isSubmitted.value) return
  submitConfirmVisible.value = true
}

async function handleConfirmSubmit() {
  if (submitting.value || isSubmitted.value || !competition.value?.id) return

  submitting.value = true
  try {
    const payload: TheoryPaperSubmitRequestDTO = {
      answers: questions.value.map((q) => ({
        paperId: q.paperId,
        answer: answers[q.paperId] ? answers[q.paperId].trim() : ''
      }))
    }

    const res = await submitTheoryPaperApi(competition.value.id, payload)
    if (res?.data) {
      examResult.value = res.data
      if (paper.value) {
        paper.value.isSubmitted = true
        paper.value.myTotalScore = res.data.scoreObtained
      }
      submitConfirmVisible.value = false
      resultModalVisible.value = true
      if (message) {
        message.success(t('competition.theory_submit_success', '试卷提交成功！'))
      }
    }
  } catch (err: any) {
    if (message) {
      message.error(err?.message || '提交试卷失败')
    }
  } finally {
    submitting.value = false
  }
}

function handleResultConfirmed() {
  resultModalVisible.value = false
}

function handleTimeUp() {
  if (!isSubmitted.value && questions.value.length > 0) {
    if (message) {
      message.warning(t('competition.countdown_ended', '考核时间已截止，正在自动交卷...'))
    }
    handleConfirmSubmit()
  }
}

function handleKeydown(ev: KeyboardEvent) {
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

async function fetchCompetitionAndPaper() {
  if (!competitionIdentifier.value) return

  loading.value = true
  try {
    const isNumeric = /^\d+$/.test(competitionIdentifier.value)
    let compData: CompetitionDetailVO | null = null

    if (isNumeric) {
      const compRes = await getCompetitionByIdApi(competitionIdentifier.value)
      compData = compRes?.data || null
    } else {
      const compRes = await getCompetitionBySlugApi(competitionIdentifier.value)
      compData = compRes?.data || null
    }

    if (!compData) {
      if (message) {
        message.error(t('competition.empty_desc', '未找到对应竞赛信息'))
      }
      return
    }

    competition.value = compData

    const paperRes = await getTheoryPaperApi(compData.id)
    if (paperRes?.data) {
      paper.value = paperRes.data
      for (const q of paperRes.data.questions || []) {
        if (q.myAnswer !== undefined && q.myAnswer !== null) {
          answers[q.paperId] = q.myAnswer
        }
      }
    }
  } catch (err: any) {
    if (message) {
      message.error(err?.message || '获取理论试卷失败')
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
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
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
