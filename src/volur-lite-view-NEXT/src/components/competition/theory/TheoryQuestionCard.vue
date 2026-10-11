<template>
  <div class="theory-question-card bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark rounded-2xl p-5 sm:p-7 shadow-sm transition-colors duration-200 space-y-6">
    <div class="flex items-center justify-between flex-wrap gap-3 pb-4 border-b border-google-border-light/70 dark:border-google-border-dark/70">
      <div class="flex items-center flex-wrap gap-2">
        <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-50 text-google-blue dark:bg-blue-950/60 dark:text-google-blue-dark">
          {{ t('competition.theory_question_num', { current: index, total: total }) }}
        </span>

        <span
          class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border"
          :class="typeBadgeClass"
        >
          {{ typeLabel }}
        </span>

        <span
          v-if="question.category"
          class="inline-flex items-center px-2 py-0.5 rounded text-xs text-google-text-muted dark:text-google-text-darkMuted bg-google-surface-light dark:bg-google-surface-dark"
        >
          {{ question.category }}
        </span>

        <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-mono font-medium text-google-text-desc dark:text-google-text-darkDesc bg-google-surface-light dark:bg-google-surface-dark">
          {{ question.score }} {{ t('competition.theory_score_unit', '分') }}
        </span>
      </div>

      <button
        type="button"
        class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium transition-colors cursor-pointer border"
        :class="[
          isMarked
            ? 'bg-amber-50 text-google-yellow border-amber-300 dark:bg-amber-950/50 dark:text-google-yellow-dark dark:border-amber-800'
            : 'bg-transparent text-google-text-muted dark:text-google-text-darkMuted border-transparent hover:bg-google-surface-light dark:hover:bg-google-surface-dark'
        ]"
        :disabled="readonly"
        @click="emit('toggle-mark')"
      >
        <n-icon :size="14" :component="isMarked ? Bookmark : BookmarkOutline" />
        <span>{{ isMarked ? t('competition.theory_marked', '已标记') : t('competition.theory_mark_for_review', '标记待查') }}</span>
      </button>
    </div>

    <div class="space-y-4">
      <div class="text-base sm:text-lg leading-relaxed text-google-text-primary dark:text-google-text-darkPrimary select-text whitespace-pre-wrap font-normal">
        {{ question.content }}
      </div>

      <div v-if="question.name && question.name !== question.content" class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
        {{ question.name }}
      </div>
    </div>

    <div class="pt-2">
      <div v-if="question.type === 'SINGLE_CHOICE'" class="space-y-2.5">
        <button
          v-for="opt in formattedOptions"
          :key="opt.key"
          type="button"
          class="w-full text-left p-3.5 sm:p-4 rounded-xl border flex items-center gap-3.5 transition-all text-sm group"
          :class="[
            currentAnswer === opt.key
              ? 'border-google-blue bg-blue-50/60 text-google-blue dark:border-google-blue-dark dark:bg-blue-950/40 dark:text-google-blue-dark shadow-sm'
              : 'border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard text-google-text-primary dark:text-google-text-darkPrimary hover:bg-google-surface-light dark:hover:bg-google-surface-darkElevated',
            readonly ? 'cursor-default' : 'cursor-pointer'
          ]"
          :disabled="readonly"
          @click="selectSingle(opt.key)"
        >
          <div
            class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-semibold flex-shrink-0 transition-colors"
            :class="[
              currentAnswer === opt.key
                ? 'bg-google-blue text-white dark:bg-google-blue-dark dark:text-google-surface-dark'
                : 'bg-google-surface-light dark:bg-google-surface-dark text-google-text-desc dark:text-google-text-darkDesc group-hover:bg-google-border-light/60 dark:group-hover:bg-neutral-700'
            ]"
          >
            {{ opt.key }}
          </div>
          <span class="flex-1 leading-normal select-text">{{ opt.label }}</span>
        </button>
      </div>

      <div v-else-if="question.type === 'MULTIPLE_CHOICE'" class="space-y-2.5">
        <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted mb-2">
          {{ t('competition.theory_multiple_choice_hint', '（多选题：可选择一个或多个正确答案）') }}
        </div>
        <button
          v-for="opt in formattedOptions"
          :key="opt.key"
          type="button"
          class="w-full text-left p-3.5 sm:p-4 rounded-xl border flex items-center gap-3.5 transition-all text-sm group"
          :class="[
            isMultipleOptionSelected(opt.key)
              ? 'border-google-blue bg-blue-50/60 text-google-blue dark:border-google-blue-dark dark:bg-blue-950/40 dark:text-google-blue-dark shadow-sm'
              : 'border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard text-google-text-primary dark:text-google-text-darkPrimary hover:bg-google-surface-light dark:hover:bg-google-surface-darkElevated',
            readonly ? 'cursor-default' : 'cursor-pointer'
          ]"
          :disabled="readonly"
          @click="toggleMultiple(opt.key)"
        >
          <div
            class="w-7 h-7 rounded-lg flex items-center justify-center text-xs font-semibold flex-shrink-0 transition-colors"
            :class="[
              isMultipleOptionSelected(opt.key)
                ? 'bg-google-blue text-white dark:bg-google-blue-dark dark:text-google-surface-dark'
                : 'bg-google-surface-light dark:bg-google-surface-dark text-google-text-desc dark:text-google-text-darkDesc group-hover:bg-google-border-light/60 dark:group-hover:bg-neutral-700'
            ]"
          >
            {{ opt.key }}
          </div>
          <span class="flex-1 leading-normal select-text">{{ opt.label }}</span>
        </button>
      </div>

      <div v-else-if="question.type === 'TRUE_FALSE'" class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <button
          type="button"
          class="p-4 rounded-xl border flex items-center justify-center gap-3 transition-all text-sm font-medium"
          :class="[
            currentAnswer === 'TRUE'
              ? 'border-google-green bg-green-50/60 text-google-green dark:border-google-green-dark dark:bg-green-950/40 dark:text-google-green-dark shadow-sm'
              : 'border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard text-google-text-primary dark:text-google-text-darkPrimary hover:bg-google-surface-light dark:hover:bg-google-surface-darkElevated',
            readonly ? 'cursor-default' : 'cursor-pointer'
          ]"
          :disabled="readonly"
          @click="selectSingle('TRUE')"
        >
          <n-icon :size="18" :component="CheckmarkOutline" />
          <span>{{ t('competition.theory_true', '正确 (True)') }}</span>
        </button>

        <button
          type="button"
          class="p-4 rounded-xl border flex items-center justify-center gap-3 transition-all text-sm font-medium"
          :class="[
            currentAnswer === 'FALSE'
              ? 'border-google-red bg-red-50/60 text-google-red dark:border-google-red-dark dark:bg-red-950/40 dark:text-google-red-dark shadow-sm'
              : 'border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard text-google-text-primary dark:text-google-text-darkPrimary hover:bg-google-surface-light dark:hover:bg-google-surface-darkElevated',
            readonly ? 'cursor-default' : 'cursor-pointer'
          ]"
          :disabled="readonly"
          @click="selectSingle('FALSE')"
        >
          <n-icon :size="18" :component="CloseOutline" />
          <span>{{ t('competition.theory_false', '错误 (False)') }}</span>
        </button>
      </div>

      <div v-else-if="question.type === 'ESSAY'" class="space-y-3">
        <n-input
          :value="currentAnswer"
          type="textarea"
          :placeholder="readonly ? t('competition.theory_unanswered', '未作答') : t('competition.theory_input_placeholder', '请输入您的作答内容...')"
          :rows="5"
          :disabled="readonly"
          class="text-sm font-sans"
          @update:value="handleEssayInput"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, NInput } from 'naive-ui'
import {
  Bookmark,
  BookmarkOutline,
  CheckmarkOutline,
  CloseOutline
} from '@vicons/ionicons5'
import type { TheoryPaperQuestionVO } from '@/types/competition'

defineOptions({
  name: 'TheoryQuestionCard'
})

interface OptionItem {
  key: string
  label: string
}

const props = withDefaults(
  defineProps<{
    question: TheoryPaperQuestionVO
    index: number
    total: number
    modelValue?: string
    isMarked?: boolean
    readonly?: boolean
  }>(),
  {
    modelValue: '',
    isMarked: false,
    readonly: false
  }
)

const emit = defineEmits<{
  'update:modelValue': [val: string]
  'toggle-mark': []
}>()

const { t } = useI18n()

const currentAnswer = computed<string>(() => props.modelValue || '')

const typeLabel = computed<string>(() => {
  switch (props.question.type) {
    case 'SINGLE_CHOICE':
      return t('competition.theory_single_choice', '单选题')
    case 'MULTIPLE_CHOICE':
      return t('competition.theory_multiple_choice', '多选题')
    case 'TRUE_FALSE':
      return t('competition.theory_true_false', '判断题')
    case 'ESSAY':
      return t('competition.theory_essay', '简答题')
    default:
      return props.question.type
  }
})

const typeBadgeClass = computed<string>(() => {
  switch (props.question.type) {
    case 'SINGLE_CHOICE':
      return 'bg-emerald-50 text-google-green border-emerald-200/50 dark:bg-emerald-950/40 dark:text-google-green-dark dark:border-emerald-900/50'
    case 'MULTIPLE_CHOICE':
      return 'bg-blue-50 text-google-blue border-blue-200/50 dark:bg-blue-950/40 dark:text-google-blue-dark dark:border-blue-900/50'
    case 'TRUE_FALSE':
      return 'bg-amber-50 text-google-yellow border-amber-200/50 dark:bg-amber-950/40 dark:text-google-yellow-dark dark:border-amber-900/50'
    case 'ESSAY':
      return 'bg-purple-50 text-purple-600 border-purple-200/50 dark:bg-purple-950/40 dark:text-purple-400 dark:border-purple-900/50'
    default:
      return 'bg-google-surface-light text-google-text-desc border-google-border-light dark:bg-google-surface-dark dark:text-google-text-darkDesc'
  }
})

const formattedOptions = computed<OptionItem[]>(() => {
  if (!props.question.options) return []
  const raw = props.question.options.trim()
  if (!raw) return []

  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      return parsed.map((item, idx) => {
        const key = String.fromCharCode(65 + idx)
        const text = typeof item === 'string' ? item : JSON.stringify(item)
        const cleanedText = text.replace(/^[A-Z][\.\、\:\s]\s*/, '')
        return { key, label: cleanedText }
      })
    }
    if (typeof parsed === 'object' && parsed !== null) {
      return Object.entries(parsed).map(([k, v]) => ({
        key: k.toUpperCase(),
        label: String(v)
      }))
    }
  } catch {
  }

  const lines = raw.split(/\r?\n/).filter((l) => l.trim().length > 0)
  return lines.map((line, idx) => {
    const key = String.fromCharCode(65 + idx)
    const cleaned = line.replace(/^[A-Z][\.\、\:\s]\s*/, '')
    return { key, label: cleaned }
  })
})

function selectSingle(key: string) {
  if (props.readonly) return
  emit('update:modelValue', key)
}

function isMultipleOptionSelected(key: string): boolean {
  if (!currentAnswer.value) return false
  const selectedSet = new Set(
    currentAnswer.value
      .split(/[,，\s]+/)
      .map((s) => s.trim().toUpperCase())
      .filter(Boolean)
  )
  if (selectedSet.has(key)) return true
  return currentAnswer.value.toUpperCase().includes(key)
}

function toggleMultiple(key: string) {
  if (props.readonly) return
  const currentTokens = currentAnswer.value
    ? currentAnswer.value
        .split(/[,，\s]+/)
        .map((s) => s.trim().toUpperCase())
        .filter(Boolean)
    : []

  const set = new Set(currentTokens)
  if (set.has(key)) {
    set.delete(key)
  } else {
    set.add(key)
  }

  const sortedList = Array.from(set).sort()
  emit('update:modelValue', sortedList.join(','))
}

function handleEssayInput(val: string) {
  if (props.readonly) return
  emit('update:modelValue', val)
}
</script>
