<template>
  <VDrawer
    :show="show"
    :title="displayTitle"
    :width="width"
    :theme="theme"
    @update:show="handleUpdateShow"
    @close="handleClose"
  >
    <div class="v-feedback-container">
      <div class="feedback-field-group">
        <label class="feedback-label">
          {{ t('common.describe_feedback_required', 'Describe your feedback (required)') }}
        </label>
        <n-input
          v-model:value="feedbackContent"
          type="textarea"
          :placeholder="computedPlaceholder"
          :autosize="{ minRows: 6, maxRows: 12 }"
          :maxlength="2000"
          show-count
          class="feedback-textarea"
        />
      </div>
    </div>

    <template #footer>
      <div class="feedback-footer-actions">
        <n-button
          quaternary
          :round="true"
          class="feedback-action-btn feedback-cancel-btn"
          :aria-label="t('common.cancel', 'Cancel')"
          @click="handleClose"
        >
          {{ t('common.cancel', 'Cancel') }}
        </n-button>
        <n-button
          type="primary"
          :round="true"
          class="feedback-action-btn feedback-submit-btn"
          :disabled="!isSubmittable"
          :loading="isSubmitting"
          :aria-label="t('common.send', 'Send')"
          @click="handleSubmit"
        >
          {{ t('common.send', 'Send') }}
        </n-button>
      </div>
    </template>
  </VDrawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NInput, NButton } from 'naive-ui'
import VDrawer from './VDrawer.vue'
import type { VFeedbackProps } from './types'

defineOptions({
  name: 'VFeedback'
})

const props = withDefaults(defineProps<VFeedbackProps>(), {
  show: false,
  theme: undefined,
  title: '',
  width: 400,
  placeholder: ''
})

const emit = defineEmits<{
  'update:show': [value: boolean]
  close: []
  submit: [payload: { content: string }]
}>()

const { t } = useI18n()

const feedbackContent = ref<string>('')
const isSubmitting = ref<boolean>(false)

const displayTitle = computed<string>(() => {
  return props.title || t('common.send_feedback_to_vulor', 'Send feedback to Vulor')
})

const computedPlaceholder = computed<string>(() => {
  return props.placeholder || t('common.feedback_placeholder', 'Tell us what prompted this feedback...')
})

const isSubmittable = computed<boolean>(() => {
  return feedbackContent.value.trim().length > 0 && !isSubmitting.value
})

function handleUpdateShow(val: boolean) {
  emit('update:show', val)
}

function handleClose() {
  emit('update:show', false)
  emit('close')
}

function handleSubmit() {
  const content = feedbackContent.value.trim()
  if (!content) {
    if (window.$message) {
      window.$message.warning(t('common.feedback_empty_tip', 'Please enter your feedback'))
    }
    return
  }

  if (isSubmitting.value) {
    return
  }

  isSubmitting.value = true
  emit('submit', { content })

  if (window.$message) {
    window.$message.success(t('common.feedback_success', 'Thank you for your feedback!'))
  }

  feedbackContent.value = ''
  isSubmitting.value = false
  handleClose()
}
</script>

<style scoped lang="scss">
.v-feedback-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;

  .feedback-field-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;

    .feedback-label {
      font-size: 14px;
      font-weight: 500;
      color: #3c4043;
      user-select: none;
    }

    .feedback-textarea {
      font-size: 14px;
      line-height: 1.6;
    }
  }
}

.feedback-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;

  .feedback-action-btn {
    min-width: 80px;
    height: 36px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
  }

  .feedback-submit-btn {
    background-color: #1a73e8;
    color: #ffffff;
    border: none;

    &:hover {
      background-color: #1765cc;
    }

    &:disabled {
      background-color: rgba(60, 64, 67, 0.12);
      color: rgba(60, 64, 67, 0.38);
    }
  }
}

html.dark {
  .v-feedback-container {
    .feedback-field-group {
      .feedback-label {
        color: #bdc1c6;
      }
    }
  }

  .feedback-footer-actions {
    .feedback-submit-btn {
      background-color: #a8c7fa;
      color: #040e24;

      &:hover {
        background-color: #b6d0f8;
      }

      &:disabled {
        background-color: rgba(255, 255, 255, 0.12);
        color: rgba(255, 255, 255, 0.38);
      }
    }
  }
}
</style>
