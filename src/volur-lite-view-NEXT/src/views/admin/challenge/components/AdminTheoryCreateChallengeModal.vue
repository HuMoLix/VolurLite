<template>
  <VModal
    :show="show"
    :title="isEditMode ? t('admin.challenges.edit_theory_title', '编辑题目') : t('admin.challenges.create_theory_title', '新增题目')"
    :width="700"
    @update:show="handleUpdateShow"
  >
    <div class="flex flex-col gap-4 max-h-[72vh] overflow-y-auto pr-1 py-1">
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
          {{ t('admin.challenges.theory_name', '题目简述/题名') }} *
        </label>
        <VInput
          v-model:value="form.name"
          :placeholder="t('admin.challenges.theory_name_placeholder', '简要题目命名')"
          clearable
        />
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.challenges.type', '题型') }} *
          </label>
          <VSelection
            v-model:value="form.type"
            :options="theoryTypeOptions"
            :placeholder="t('admin.challenges.select_type', '请选择题型')"
            class="w-full"
            @update:value="handleTypeChange"
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.challenges.category', '分类') }}
          </label>
          <VInput
            v-model:value="form.category"
            :placeholder="'例如: Web安全 / 密码学'"
            clearable
          />
        </div>
      </div>

      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
          {{ t('admin.challenges.content', '题目完整题干') }} *
        </label>
        <VInput
          v-model:value="form.content"
          type="textarea"
          :rows="4"
          :placeholder="t('admin.challenges.content_placeholder', '请输入题目题干内容...')"
        />
      </div>

      <div v-if="form.type === 'SINGLE_CHOICE' || form.type === 'MULTIPLE_CHOICE'" class="flex flex-col gap-2">
        <div class="flex items-center justify-between">
          <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
            {{ t('admin.challenges.options_and_answer', '题目选项与正确答案') }} *
          </label>
          <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
            {{ form.type === 'SINGLE_CHOICE' ? t('admin.challenges.single_choice_hint', '点击左侧复选框选择唯一正确答案') : t('admin.challenges.multi_choice_hint', '勾选左侧复选框设置一个或多个正确答案') }}
          </span>
        </div>

        <div class="space-y-2">
          <div
            v-for="(opt, idx) in optionList"
            :key="opt.key"
            class="flex items-center gap-2 p-2 rounded-xl border transition-colors"
            :class="[
              isOptionCorrect(opt.key)
                ? 'bg-blue-50/50 dark:bg-blue-950/20 border-google-blue/40 dark:border-google-blue-dark/40'
                : 'bg-google-surface-light dark:bg-google-surface-dark border-google-border-light dark:border-google-border-dark'
            ]"
          >
            <VSelectBox
              :checked="isOptionCorrect(opt.key)"
              class="shrink-0 ml-1"
              @change="toggleOptionAnswer(opt.key)"
            />

            <span class="text-xs font-semibold text-google-text-primary dark:text-google-text-darkPrimary shrink-0">
              {{ opt.key }}.
            </span>

            <VInput
              v-model:value="opt.val"
              :placeholder="`请输入选项 ${opt.key} 的文本内容`"
              class="flex-1"
              clearable
            />

            <VBtn
              clz="normal"
              size="small"
              :left-icon="TrashOutline"
              :disabled="optionList.length <= 2"
              class="shrink-0 text-google-red hover:text-google-red"
              @click="removeOption(idx)"
            />
          </div>
        </div>

        <div class="flex items-center justify-between pt-1">
          <VBtn
            clz="normal"
            size="small"
            :left-icon="AddOutline"
            :disabled="optionList.length >= 10"
            @click="addOption"
          >
            {{ t('admin.challenges.add_option_btn', '添加选项') }}
          </VBtn>

          <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('admin.challenges.option_count_tip', '支持 2 ~ 10 个选项') }}
          </span>
        </div>
      </div>

      <div v-else-if="form.type === 'TRUE_FALSE'" class="flex flex-col gap-2">
        <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
          {{ t('admin.challenges.standard_answer', '标准参考答案') }} *
        </label>
        <div class="grid grid-cols-2 gap-3">
          <VBtn
            :clz="tfAnswer === 'TRUE' ? 'primary' : 'normal'"
            size="medium"
            :left-icon="tfAnswer === 'TRUE' ? CheckmarkOutline : undefined"
            class="w-full justify-center"
            @click="tfAnswer = 'TRUE'"
          >
            {{ t('competition.theory_true', '正确 (True)') }}
          </VBtn>

          <VBtn
            :clz="tfAnswer === 'FALSE' ? 'primary' : 'normal'"
            size="medium"
            :left-icon="tfAnswer === 'FALSE' ? CheckmarkOutline : undefined"
            class="w-full justify-center"
            @click="tfAnswer = 'FALSE'"
          >
            {{ t('competition.theory_false', '错误 (False)') }}
          </VBtn>
        </div>
      </div>

      <div v-else-if="form.type === 'ESSAY'" class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
          {{ t('admin.challenges.essay_standard_answer', '标准参考答案 / 裁判评判标准') }} *
        </label>
        <VInput
          v-model:value="essayAnswer"
          type="textarea"
          :rows="4"
          :placeholder="t('admin.challenges.essay_answer_placeholder', '请输入主观题参考标准答案、关键得分要点或裁判评判指引...')"
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-google-text-desc dark:text-google-text-darkDesc">
          {{ t('admin.challenges.explanation', '答案解析说明') }}
        </label>
        <VInput
          v-model:value="form.explanation"
          type="textarea"
          :rows="3"
          :placeholder="t('admin.challenges.explanation_placeholder', '可选，公布答案时向选手展示的题目解析')"
        />
      </div>
    </div>

    <template #footer>
      <div class="flex items-center justify-end gap-3 w-full">
        <VBtn clz="normal" @click="handleUpdateShow(false)">
          {{ t('common.cancel', '取消') }}
        </VBtn>
        <VBtn
          clz="primary"
          :loading="saving"
          @click="handleSave"
        >
          {{ t('common.save', '保存题目') }}
        </VBtn>
      </div>
    </template>
  </VModal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import {
  AddOutline,
  TrashOutline,
  CheckmarkOutline
} from '@vicons/ionicons5'
import VBtn from '@/components/common/VBtn.vue'
import VModal from '@/components/common/VModal.vue'
import VInput from '@/components/common/VInput.vue'
import VSelection from '@/components/common/VSelection.vue'
import VSelectBox from '@/components/common/VSelectBox.vue'
import {
  createAdminTheoryBankQuestionApi,
  updateAdminTheoryBankQuestionApi,
  getAdminTheoryChallengeDetailApi
} from '@/api/challenge'
import type { TheoryType } from '@/types/challenge'

defineOptions({
  name: 'AdminTheoryCreateChallengeModal'
})

interface OptionItem {
  key: string
  val: string
}

const props = withDefaults(
  defineProps<{
    show: boolean
    bankId: number | string
    bankCategory?: string
    questionId?: number | string | null
  }>(),
  {
    bankCategory: '',
    questionId: null
  }
)

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'success'): void
}>()

const { t } = useI18n()
const message = useMessage()

const saving = ref(false)
const isEditMode = computed(() => Boolean(props.questionId))

const form = reactive({
  name: '',
  category: '',
  type: 'SINGLE_CHOICE' as TheoryType,
  content: '',
  explanation: ''
})

const optionList = ref<OptionItem[]>([
  { key: 'A', val: '' },
  { key: 'B', val: '' },
  { key: 'C', val: '' },
  { key: 'D', val: '' }
])

const selectedAnswerKeys = ref<Set<string>>(new Set(['A']))
const tfAnswer = ref<'TRUE' | 'FALSE'>('TRUE')
const essayAnswer = ref('')

const theoryTypeOptions = computed(() => [
  { label: t('admin.challenges.type_single_choice', '单项选择题'), value: 'SINGLE_CHOICE' },
  { label: t('admin.challenges.type_multiple_choice', '多项选择题'), value: 'MULTIPLE_CHOICE' },
  { label: t('admin.challenges.type_true_false', '判断题'), value: 'TRUE_FALSE' },
  { label: t('admin.challenges.type_essay', '主观问答题'), value: 'ESSAY' }
])

function handleUpdateShow(val: boolean) {
  emit('update:show', val)
}

function isOptionCorrect(key: string): boolean {
  return selectedAnswerKeys.value.has(key)
}

function toggleOptionAnswer(key: string) {
  if (form.type === 'SINGLE_CHOICE') {
    selectedAnswerKeys.value = new Set([key])
  } else if (form.type === 'MULTIPLE_CHOICE') {
    const next = new Set(selectedAnswerKeys.value)
    if (next.has(key)) {
      if (next.size > 1) {
        next.delete(key)
      } else {
        message.warning(t('admin.challenges.min_one_correct_answer', '多选题至少保留一个正确答案'))
        return
      }
    } else {
      next.add(key)
    }
    selectedAnswerKeys.value = next
  }
}

function addOption() {
  if (optionList.value.length >= 10) return
  const nextKey = String.fromCharCode(65 + optionList.value.length)
  optionList.value.push({ key: nextKey, val: '' })
}

function removeOption(idx: number) {
  if (optionList.value.length <= 2) return
  const removedKey = optionList.value[idx]?.key
  optionList.value.splice(idx, 1)

  optionList.value.forEach((item, i) => {
    item.key = String.fromCharCode(65 + i)
  })

  if (removedKey && selectedAnswerKeys.value.has(removedKey)) {
    const next = new Set(
      Array.from(selectedAnswerKeys.value)
        .filter((k) => k !== removedKey)
        .map((k) => {
          const charCode = k.charCodeAt(0)
          const removedCharCode = removedKey.charCodeAt(0)
          if (charCode > removedCharCode) {
            return String.fromCharCode(charCode - 1)
          }
          return k
        })
    )
    if (next.size === 0) {
      next.add('A')
    }
    selectedAnswerKeys.value = next
  }
}

function handleTypeChange(val: any) {
  const typeVal = String(val || 'SINGLE_CHOICE') as TheoryType
  form.type = typeVal
  if (typeVal === 'SINGLE_CHOICE') {
    if (selectedAnswerKeys.value.size === 0) {
      selectedAnswerKeys.value = new Set(['A'])
    } else {
      const first = Array.from(selectedAnswerKeys.value)[0] || 'A'
      selectedAnswerKeys.value = new Set([first])
    }
    if (optionList.value.length < 2) {
      resetDefaultOptions()
    }
  } else if (typeVal === 'MULTIPLE_CHOICE') {
    if (selectedAnswerKeys.value.size === 0) {
      selectedAnswerKeys.value = new Set(['A'])
    }
    if (optionList.value.length < 2) {
      resetDefaultOptions()
    }
  } else if (typeVal === 'TRUE_FALSE') {
    tfAnswer.value = 'TRUE'
  }
}

function resetDefaultOptions() {
  optionList.value = [
    { key: 'A', val: '' },
    { key: 'B', val: '' },
    { key: 'C', val: '' },
    { key: 'D', val: '' }
  ]
  selectedAnswerKeys.value = new Set(['A'])
}

function resetForm() {
  form.name = ''
  form.category = props.bankCategory || ''
  form.type = 'SINGLE_CHOICE'
  form.content = ''
  form.explanation = ''
  resetDefaultOptions()
  tfAnswer.value = 'TRUE'
  essayAnswer.value = ''
}

function parseOptionsFromJson(raw?: string | null): OptionItem[] {
  if (!raw || !raw.trim()) return []
  try {
    const parsed = JSON.parse(raw.trim())
    if (Array.isArray(parsed) && parsed.length > 0) {
      return parsed.map((item, idx) => {
        const key = String.fromCharCode(65 + idx)
        if (typeof item === 'string') {
          return { key, val: item.replace(/^[A-Z][\.\、\:\s]\s*/, '').trim() }
        }
        if (typeof item === 'object' && item !== null) {
          const val = item.val || item.text || item.label || item.content || ''
          return { key: item.key || key, val: String(val) }
        }
        return { key, val: String(item) }
      })
    }
    if (typeof parsed === 'object' && parsed !== null) {
      return Object.entries(parsed).map(([k, v]) => ({
        key: k.toUpperCase(),
        val: String(v)
      }))
    }
  } catch {
    const lines = raw.split(/\r?\n/).filter((l) => l.trim().length > 0)
    if (lines.length > 0) {
      return lines.map((line, idx) => ({
        key: String.fromCharCode(65 + idx),
        val: line.replace(/^[A-Z][\.\、\:\s]\s*/, '').trim()
      }))
    }
  }
  return []
}

async function loadDetail(id: number | string) {
  try {
    const res = await getAdminTheoryChallengeDetailApi(id)
    if (res.data) {
      const d = res.data
      form.name = d.name || ''
      form.category = d.category || ''
      form.type = (d.type || 'SINGLE_CHOICE') as TheoryType
      form.content = d.content || ''
      form.explanation = d.explanation || ''

      const parsedOpts = parseOptionsFromJson(d.options)
      if (parsedOpts.length >= 2) {
        optionList.value = parsedOpts
      } else {
        resetDefaultOptions()
      }

      const rawAns = (d.standardAnswer || '').trim().toUpperCase()
      if (form.type === 'TRUE_FALSE') {
        tfAnswer.value = rawAns === 'FALSE' || rawAns === 'B' ? 'FALSE' : 'TRUE'
      } else if (form.type === 'ESSAY') {
        essayAnswer.value = d.standardAnswer || ''
      } else if (form.type === 'MULTIPLE_CHOICE') {
        const tokens = rawAns
          .split(/[,，\s]+/)
          .map((s) => s.trim())
          .filter(Boolean)
        const matched = new Set<string>()
        tokens.forEach((t) => {
          if (t.length === 1) {
            matched.add(t)
          } else {
            for (const ch of t) {
              if (/[A-Z]/.test(ch)) matched.add(ch)
            }
          }
        })
        selectedAnswerKeys.value = matched.size > 0 ? matched : new Set(['A'])
      } else {
        const first = rawAns.charAt(0) || 'A'
        selectedAnswerKeys.value = new Set([first])
      }
    }
  } catch {
  }
}

watch(
  () => props.show,
  (val) => {
    if (val) {
      if (props.questionId) {
        loadDetail(props.questionId)
      } else {
        resetForm()
      }
    }
  },
  { immediate: true }
)

async function handleSave() {
  if (!form.name.trim()) {
    message.warning(t('admin.challenges.name_required', '请填写题目名称'))
    return
  }
  if (!form.content.trim()) {
    message.warning(t('admin.challenges.content_required', '请填写题目题干内容'))
    return
  }

  let finalOptions: string | undefined = undefined
  let finalStandardAnswer = ''

  if (form.type === 'SINGLE_CHOICE') {
    const hasEmptyOption = optionList.value.some((opt) => !opt.val.trim())
    if (hasEmptyOption) {
      message.warning(t('admin.challenges.option_content_required', '请完整填写所有选项内容'))
      return
    }
    const chosen = Array.from(selectedAnswerKeys.value)[0] || 'A'
    finalStandardAnswer = chosen
    finalOptions = JSON.stringify(optionList.value.map((o) => ({ key: o.key, val: o.val.trim() })))
  } else if (form.type === 'MULTIPLE_CHOICE') {
    const hasEmptyOption = optionList.value.some((opt) => !opt.val.trim())
    if (hasEmptyOption) {
      message.warning(t('admin.challenges.option_content_required', '请完整填写所有选项内容'))
      return
    }
    if (selectedAnswerKeys.value.size === 0) {
      message.warning(t('admin.challenges.select_correct_answer', '请至少勾选一个正确答案'))
      return
    }
    finalStandardAnswer = Array.from(selectedAnswerKeys.value).sort().join(',')
    finalOptions = JSON.stringify(optionList.value.map((o) => ({ key: o.key, val: o.val.trim() })))
  } else if (form.type === 'TRUE_FALSE') {
    finalStandardAnswer = tfAnswer.value
    finalOptions = JSON.stringify([
      { key: 'A', val: '正确' },
      { key: 'B', val: '错误' }
    ])
  } else if (form.type === 'ESSAY') {
    if (!essayAnswer.value.trim()) {
      message.warning(t('admin.challenges.essay_answer_required', '请填写主观问答题的参考标准答案或评判标准'))
      return
    }
    finalStandardAnswer = essayAnswer.value.trim()
    finalOptions = undefined
  }

  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      category: form.category.trim() || undefined,
      type: form.type,
      content: form.content.trim(),
      options: finalOptions,
      standardAnswer: finalStandardAnswer,
      explanation: form.explanation.trim() || undefined
    }

    if (isEditMode.value && props.questionId) {
      await updateAdminTheoryBankQuestionApi(props.bankId, props.questionId, payload)
      message.success(t('admin.challenges.update_success', '题目更新成功'))
    } else {
      await createAdminTheoryBankQuestionApi(props.bankId, payload)
      message.success(t('admin.challenges.create_success', '题目创建成功'))
    }

    emit('success')
    emit('update:show', false)
  } catch {
  } finally {
    saving.value = false
  }
}
</script>
