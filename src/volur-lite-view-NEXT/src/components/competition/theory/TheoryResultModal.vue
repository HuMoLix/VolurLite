<template>
  <n-modal
    :show="show"
    :mask-closable="false"
    preset="card"
    class="max-w-md w-full rounded-3xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-xl"
    :title="t('competition.theory_submit_success', '试卷提交成功！')"
    @update:show="emit('update:show', $event)"
  >
    <div class="space-y-6 text-center py-2">
      <div class="w-16 h-16 mx-auto rounded-full bg-blue-50 dark:bg-blue-950/60 flex items-center justify-center text-google-blue dark:text-google-blue-dark">
        <n-icon :size="32" :component="CheckmarkCircleOutline" />
      </div>

      <div class="space-y-1">
        <h3 class="text-xl font-bold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('competition.theory_submit_success', '试卷提交成功！') }}
        </h3>
        <p class="text-xs text-google-text-desc dark:text-google-text-darkDesc">
          {{ t('competition.theory_submitted_tip', '您已完成本次考核答卷提交') }}
        </p>
      </div>

      <div class="p-4 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 space-y-3 text-left">
        <div class="flex items-center justify-between text-xs">
          <span class="text-google-text-muted dark:text-google-text-darkMuted">{{ t('competition.theory_total_questions', '答卷总题数') }}</span>
          <span class="font-bold font-mono text-google-text-primary dark:text-google-text-darkPrimary">{{ result?.totalQuestions ?? 0 }} 题</span>
        </div>
        <div class="flex items-center justify-between text-xs">
          <span class="text-google-text-muted dark:text-google-text-darkMuted">考核状态</span>
          <span class="font-medium text-google-green dark:text-google-green-dark">已提交封存</span>
        </div>
        <div class="pt-2 border-t border-google-border-light/60 dark:border-google-border-dark/60 text-xs text-google-text-desc dark:text-google-text-darkDesc leading-relaxed">
          {{ t('competition.theory_score_hidden_notice', '答卷已封存归档，最终成绩由管理员在后台统一核定') }}。答题已结束，试卷题目已锁定且无法再次查看。
        </div>
      </div>

      <div class="pt-2">
        <VBtn
          clz="primary"
          block
          @click="handleConfirm"
        >
          {{ t('competition.theory_back_to_competition', '返回竞赛主页') }}
        </VBtn>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { NModal, NIcon } from 'naive-ui'
import { CheckmarkCircleOutline } from '@vicons/ionicons5'
import { VBtn } from '@/components/common'
import type { TheoryPaperResultVO } from '@/types/competition'

defineOptions({
  name: 'TheoryResultModal'
})

withDefaults(
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

function handleConfirm() {
  emit('update:show', false)
  emit('confirm')
}
</script>
