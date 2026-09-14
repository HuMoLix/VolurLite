<template>
  <div class="volur-markdown-editor">
    <!-- Editor Header Toolbar -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <n-tabs
          v-model:value="activeMode"
          type="segment"
          size="small"
          class="editor-mode-tabs"
        >
          <n-tab name="edit">
            <span class="tab-label">
              <n-icon :component="CreateOutline" />
              {{ t('common.markdown_edit') }}
            </span>
          </n-tab>
          <n-tab name="preview">
            <span class="tab-label">
              <n-icon :component="EyeOutline" />
              {{ t('common.markdown_preview') }}
            </span>
          </n-tab>
        </n-tabs>
      </div>

      <div v-if="showTip" class="toolbar-right">
        <span class="toolbar-tip">{{ tipText || t('common.markdown_tip') }}</span>
      </div>
    </div>

    <!-- Mode 1: Edit Mode -->
    <div v-show="activeMode === 'edit'" class="editor-input-wrap">
      <n-input
        :value="currentValue"
        type="textarea"
        :rows="rows"
        :disabled="disabled"
        :placeholder="placeholder || t('common.markdown_placeholder')"
        class="markdown-textarea"
        @update:value="handleInput"
      />
    </div>

    <!-- Mode 2: Preview Mode -->
    <div
      v-show="activeMode === 'preview'"
      class="editor-preview-wrap"
      :style="previewStyle"
    >
      <div
        v-if="currentValue && currentValue.trim()"
        class="volur-markdown-body desc-preview-content"
        v-html="renderedHtml"
      />
      <div v-else class="preview-empty">
        {{ emptyPreviewText || t('common.markdown_empty_preview') }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NTabs, NTab, NInput, NIcon } from 'naive-ui'
import { CreateOutline, EyeOutline } from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { renderMarkdown } from '@/utils/markdown'

interface Props {
  value?: string
  modelValue?: string
  placeholder?: string
  rows?: number
  minHeight?: string | number
  maxHeight?: string | number
  disabled?: boolean
  showTip?: boolean
  tipText?: string
  emptyPreviewText?: string
}

const props = withDefaults(defineProps<Props>(), {
  value: undefined,
  modelValue: undefined,
  placeholder: '',
  rows: 8,
  minHeight: '200px',
  maxHeight: '480px',
  disabled: false,
  showTip: true,
  tipText: '',
  emptyPreviewText: ''
})

const emit = defineEmits<{
  (e: 'update:value', val: string): void
  (e: 'update:modelValue', val: string): void
  (e: 'change', val: string): void
}>()

const { t } = useI18n()

const activeMode = ref<'edit' | 'preview'>('edit')

const currentValue = computed(() => {
  if (props.value !== undefined) return props.value
  if (props.modelValue !== undefined) return props.modelValue
  return ''
})

const renderedHtml = computed(() => {
  return renderMarkdown(currentValue.value)
})

const previewStyle = computed(() => {
  const minH = typeof props.minHeight === 'number' ? `${props.minHeight}px` : props.minHeight
  const maxH = typeof props.maxHeight === 'number' ? `${props.maxHeight}px` : props.maxHeight
  return {
    minHeight: minH,
    maxHeight: maxH
  }
})

function handleInput(val: string) {
  emit('update:value', val)
  emit('update:modelValue', val)
  emit('change', val)
}
</script>

<style scoped lang="scss">
.volur-markdown-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--v-border-base, #e5e4e7);
  border-radius: 8px;
  overflow: hidden;
  background: var(--v-bg-card, #ffffff);

  .editor-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: rgba(0, 0, 0, 0.02);
    border-bottom: 1px solid var(--v-border-base, #e5e4e7);

    .toolbar-left {
      .editor-mode-tabs {
        width: 180px;

        .tab-label {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 13px;
        }
      }
    }

    .toolbar-right {
      .toolbar-tip {
        font-size: 12px;
        color: #444444;
      }
    }
  }

  .editor-input-wrap {
    .markdown-textarea {
      border: none !important;

      :deep(.n-input__border),
      :deep(.n-input__state-border) {
        display: none !important;
      }

      :deep(.n-input__textarea-el) {
        font-family: var(--v-font-mono, 'Fira Code', monospace), -apple-system, sans-serif;
        font-size: 13px;
        line-height: 1.6;
        padding: 12px;
      }
    }
  }

  .editor-preview-wrap {
    overflow-y: auto;
    padding: 16px 20px;
    background: var(--v-bg-card, #ffffff);

    .preview-empty {
      color: var(--v-text-muted, #999999);
      font-size: 13px;
      text-align: center;
      padding: 40px 0;
    }
  }
}
</style>
