<template>
  <n-modal
    :show="show"
    :mask-closable="false"
    preset="card"
    class="max-w-md w-full rounded-3xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-xl"
    :title="t('competition.theory_result_title', '考核成绩报告')"
    @update:show="emit('update:show', $event)"
  >
    <div class="space-y-6 text-center py-2">
      <div class="w-16 h-16 mx-auto rounded-full bg-blue-50 dark:bg-blue-950/60 flex items-center justify-center text-google-blue dark:text-google-blue-dark">
        <n-icon :size="32" :component="TrophyOutline" />
      </div>

      <div class="space-y-1">
        <h3 class="text-xl font-bold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('competition.theory_submit_success', '试卷提交成功！') }}
        </h3>
        <p class="text-xs text-google-text-desc dark:text-google-text-darkDesc">
          {{ t('competition.theory_submitted_tip', '您已完成本次考核答卷提交') }}
        </p>
      </div>

      <div class="grid grid-cols-3 gap-3 p-4 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60">
        <div class="space-y-1">
          <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('competition.theory_result_score_obtained', '最终得分') }}
          </div>
          <div class="text-lg font-bold font-mono text-google-blue dark:text-google-blue-dark">
            {{ result?.scoreObtained ?? 0 }}
          </div>
          <div class="text-[10px] text-google-text-muted dark:text-google-text-darkMuted font-mono">
            / {{ result?.totalScore ?? 0 }}
          </div>
        </div>

        <div class="space-y-1 border-x border-google-border-light/60 dark:border-google-border-dark/60 px-2">
          <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('competition.theory_result_accuracy', '正确率') }}
          </div>
          <div class="text-lg font-bold font-mono text-google-green dark:text-google-green-dark">
            {{ formattedAccuracy }}%
          </div>
        </div>

        <div class="space-y-1">
          <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('competition.theory_result_correct_count', '正确题数') }}
          </div>
          <div class="text-lg font-bold font-mono text-google-text-primary dark:text-google-text-darkPrimary">
            {{ result?.correctCount ?? 0 }}
          </div>
          <div class="text-[10px] text-google-text-muted dark:text-google-text-darkMuted font-mono">
            / {{ result?.totalQuestions ?? 0 }}
          </div>
        </div>
      </div>

      <div class="pt-2">
        <button
          type="button"
          class="w-full h-10 px-5 rounded-full text-xs font-semibold text-white bg-google-blue hover:bg-google-blue-hover active:bg-google-blue-hover shadow-sm transition-colors cursor-pointer dark:bg-google-blue-dark dark:text-google-surface-dark dark:hover:bg-blue-300"
          @click="handleConfirm"
        >
          {{ t('competition.theory_review_paper', '查看并复核答卷') }}
        </button>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NModal, NIcon } from 'naive-ui'
import { TrophyOutline } from '@vicons/ionicons5'
import type { TheoryPaperResultVO } from '@/types/competition'

defineOptions({
  name: 'TheoryResultModal'
})

const props = withDefaults(
  defineProps<{
    show: boolean
    result?: TheoryPaperResultVO | null
  }>(),
  {
    result: null
  }
)

const emit = defineEmits<{
  'update:show': [val: boolean]
  confirm: []
}>()

const { t } = useI18n()

const formattedAccuracy = computed<string>(() => {
  if (!props.result) return '0'
  if (typeof props.result.accuracyRate === 'number') {
    const rate = props.result.accuracyRate <= 1 ? props.result.accuracyRate * 100 : props.result.accuracyRate
    return Math.round(rate).toString()
  }
  if (props.result.totalQuestions && props.result.totalQuestions > 0 && props.result.correctCount !== undefined) {
    return Math.round((props.result.correctCount / props.result.totalQuestions) * 100).toString()
  }
  return '0'
})

function handleConfirm() {
  emit('update:show', false)
  emit('confirm')
}
</script>
