<template>
  <div
    class="v-markdown-editor"
    :class="[
      effectiveTheme,
      {
        'theme-dark': effectiveTheme === 'dark',
        'is-focused': isFocused,
        'is-disabled': disabled
      }
    ]"
  >
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <div
          class="editor-mode-switch"
          :class="{ 'is-preview': activeMode === 'preview' }"
        >
          <div class="mode-slider" />
          <button
            type="button"
            class="mode-btn"
            :class="{ 'is-active': activeMode === 'edit' }"
            :disabled="disabled"
            @click="activeMode = 'edit'"
          >
            <n-icon :component="CreateOutline" />
            <span>{{ t('common.markdown_edit', '编辑') }}</span>
          </button>
          <button
            type="button"
            class="mode-btn"
            :class="{ 'is-active': activeMode === 'preview' }"
            :disabled="disabled"
            @click="activeMode = 'preview'"
          >
            <n-icon :component="EyeOutline" />
            <span>{{ t('common.markdown_preview', '预览') }}</span>
          </button>
        </div>

        <div
          v-show="activeMode === 'edit' && showToolbarActions"
          class="toolbar-actions"
        >
          <button
            type="button"
            class="action-btn"
            :title="t('common.markdown_action_bold', '加粗 (Ctrl+B)')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('bold')"
          >
            <strong>B</strong>
          </button>
          <button
            type="button"
            class="action-btn italic"
            :title="t('common.markdown_action_italic', '斜体 (Ctrl+I)')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('italic')"
          >
            <em>I</em>
          </button>
          <span class="action-divider" />
          <button
            type="button"
            class="action-btn"
            :title="t('common.markdown_action_code', '行内代码')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('code')"
          >
            <n-icon :component="CodeSlashOutline" />
          </button>
          <button
            type="button"
            class="action-btn quote-btn"
            :title="t('common.markdown_action_quote', '引用')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('quote')"
          >
            &ldquo;
          </button>
          <button
            type="button"
            class="action-btn"
            :title="t('common.markdown_action_list_ul', '无序列表')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('ul')"
          >
            <n-icon :component="ListOutline" />
          </button>
          <button
            type="button"
            class="action-btn text-icon"
            :title="t('common.markdown_action_list_ol', '有序列表')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('ol')"
          >
            1.
          </button>
          <span class="action-divider" />
          <button
            type="button"
            class="action-btn"
            :title="t('common.markdown_action_link', '超链接 (Ctrl+K)')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('link')"
          >
            <n-icon :component="LinkOutline" />
          </button>
          <button
            type="button"
            class="action-btn"
            :title="t('common.markdown_action_table', '插入表格')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('table')"
          >
            <n-icon :component="GridOutline" />
          </button>
        </div>
      </div>

      <div v-if="showTip" class="toolbar-right">
        <span class="toolbar-tip">{{ tipText || t('common.markdown_tip', '支持 Markdown 语法排版') }}</span>
      </div>
    </div>

    <div v-show="activeMode === 'edit'" class="editor-input-wrap">
      <n-input
        ref="inputInstRef"
        :value="currentValue"
        type="textarea"
        :rows="rows"
        :disabled="disabled"
        :placeholder="placeholder || t('common.markdown_placeholder', '请输入内容，支持 Markdown 语法（标题、加粗、列表、表格、代码块等）...')"
        class="markdown-textarea"
        @update:value="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown="handleKeydown"
        @click="updateCursor"
        @keyup="updateCursor"
        @select="updateCursor"
      />
    </div>

    <div
      v-show="activeMode === 'preview'"
      class="editor-preview-wrap"
      :style="previewStyle"
    >
      <div
        v-if="currentValue && currentValue.trim()"
        class="v-markdown-body"
        v-html="renderedHtml"
      />
      <div v-else class="preview-empty">
        {{ emptyPreviewText || t('common.markdown_empty_preview', '暂无内容，请在编辑模式下输入 Markdown 文本') }}
      </div>
    </div>

    <div v-if="showStatusBar" class="editor-statusbar">
      <div class="statusbar-left">
        <span v-if="isFocused" class="shortcut-tip">
          {{ t('common.markdown_shortcuts_hint', '快捷键: Ctrl/⌘+B 粗体 · Ctrl/⌘+I 斜体 · Ctrl/⌘+K 链接 · Tab 缩进') }}
        </span>
        <span v-else class="status-hint">
          {{ tipText || t('common.markdown_tip', '支持 Markdown 语法排版') }}
        </span>
      </div>
      <div class="statusbar-right">
        <span v-if="activeMode === 'edit'" class="stat-item">
          {{ t('common.markdown_cursor', { line: cursorLine, col: cursorCol }) || `行 ${cursorLine}, 列 ${cursorCol}` }}
        </span>
        <span v-if="activeMode === 'edit'" class="stat-divider">·</span>
        <span class="stat-item">
          {{ t('common.markdown_words', { count: charCount }) || `${charCount} 字符` }}
        </span>
        <span class="stat-divider">·</span>
        <span class="stat-item">
          {{ t('common.markdown_lines', { count: lineCount }) || `${lineCount} 行` }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { NInput, NIcon } from 'naive-ui'
import {
  CreateOutline,
  EyeOutline,
  CodeSlashOutline,
  LinkOutline,
  GridOutline,
  ListOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/store/modules/app'
import { renderMarkdown } from '@/utils/markdown'
import type { VMarkdownEditorProps } from './types'

defineOptions({
  name: 'VMarkdownEditor'
})

const props = withDefaults(defineProps<VMarkdownEditorProps>(), {
  value: undefined,
  modelValue: undefined,
  placeholder: '',
  rows: 8,
  minHeight: '200px',
  maxHeight: '480px',
  disabled: false,
  showTip: true,
  tipText: '',
  emptyPreviewText: '',
  tabSize: 2,
  autoPair: true,
  autoIndent: true,
  showStatusBar: true,
  showToolbarActions: true,
  theme: undefined
})

const emit = defineEmits<{
  (e: 'update:value', val: string): void
  (e: 'update:modelValue', val: string): void
  (e: 'change', val: string): void
  (e: 'focus', event: FocusEvent): void
  (e: 'blur', event: FocusEvent): void
}>()

const { t } = useI18n()
const appStore = useAppStore()

const effectiveTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const activeMode = ref<'edit' | 'preview'>('edit')
const isFocused = ref(false)
const inputInstRef = ref<InstanceType<typeof NInput> | null>(null)

const cursorLine = ref(1)
const cursorCol = ref(1)

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

const charCount = computed(() => currentValue.value.length)
const lineCount = computed(() => (currentValue.value ? currentValue.value.split('\n').length : 1))

function getTextarea(): HTMLTextAreaElement | null {
  const inst = inputInstRef.value as unknown as {
    textareaElRef?: HTMLTextAreaElement | null
    $el?: HTMLElement
  }
  if (inst?.textareaElRef) {
    return inst.textareaElRef
  }
  return inst?.$el?.querySelector('textarea') || null
}

function handleInput(val: string) {
  emit('update:value', val)
  emit('update:modelValue', val)
  emit('change', val)
}

function updateCursor() {
  const textarea = getTextarea()
  if (!textarea) return
  const pos = textarea.selectionStart || 0
  const val = textarea.value || ''
  const linesBefore = val.substring(0, pos).split('\n')
  cursorLine.value = linesBefore.length
  cursorCol.value = linesBefore[linesBefore.length - 1].length + 1
}

function handleFocus(e: FocusEvent) {
  isFocused.value = true
  updateCursor()
  emit('focus', e)
}

function handleBlur(e: FocusEvent) {
  isFocused.value = false
  emit('blur', e)
}

function replaceRange(
  textarea: HTMLTextAreaElement,
  start: number,
  end: number,
  replacement: string,
  selectionMode: 'select' | 'start' | 'end' | 'preserve' = 'end',
  newSelectStart?: number,
  newSelectEnd?: number
) {
  textarea.setRangeText(replacement, start, end, selectionMode)
  textarea.dispatchEvent(new Event('input', { bubbles: true }))
  handleInput(textarea.value)
  if (newSelectStart !== undefined && newSelectEnd !== undefined) {
    textarea.setSelectionRange(newSelectStart, newSelectEnd)
  }
}

function insertText(textarea: HTMLTextAreaElement, text: string) {
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  if (!document.execCommand('insertText', false, text)) {
    replaceRange(textarea, start, end, text, 'end')
  } else {
    handleInput(textarea.value)
  }
}

function wrapOrInsert(
  textarea: HTMLTextAreaElement,
  prefix: string,
  suffix: string,
  defaultPlaceholder = ''
) {
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const val = textarea.value

  if (start !== end) {
    const selected = val.substring(start, end)
    if (
      selected.startsWith(prefix) &&
      selected.endsWith(suffix) &&
      selected.length >= prefix.length + suffix.length
    ) {
      const unwrapped = selected.slice(prefix.length, -suffix.length)
      replaceRange(textarea, start, end, unwrapped, 'preserve', start, start + unwrapped.length)
    } else {
      const wrapped = `${prefix}${selected}${suffix}`
      replaceRange(textarea, start, end, wrapped, 'preserve', start + prefix.length, end + prefix.length)
    }
  } else {
    const insert = `${prefix}${defaultPlaceholder}${suffix}`
    if (!document.execCommand('insertText', false, insert)) {
      replaceRange(textarea, start, end, insert, 'end')
    } else {
      handleInput(textarea.value)
    }
    if (defaultPlaceholder) {
      textarea.setSelectionRange(start + prefix.length, start + prefix.length + defaultPlaceholder.length)
    } else {
      textarea.setSelectionRange(start + prefix.length, start + prefix.length)
    }
  }
  updateCursor()
}

function handleTab(textarea: HTMLTextAreaElement, isShift: boolean) {
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const val = textarea.value
  const tabSpaces = ' '.repeat(props.tabSize)

  const isMultiLine = val.substring(start, end).includes('\n')

  if (!isMultiLine && !isShift) {
    if (!document.execCommand('insertText', false, tabSpaces)) {
      replaceRange(textarea, start, end, tabSpaces, 'end')
    } else {
      handleInput(textarea.value)
    }
    updateCursor()
    return
  }

  const firstLineStart = val.lastIndexOf('\n', start - 1) === -1 ? 0 : val.lastIndexOf('\n', start - 1) + 1
  let lastLineEnd = val.indexOf('\n', end)
  if (lastLineEnd === -1) lastLineEnd = val.length

  const lines = val.substring(firstLineStart, lastLineEnd).split('\n')

  if (!isShift) {
    const newBlock = lines.map(line => tabSpaces + line).join('\n')
    const addedTotal = tabSpaces.length * lines.length
    replaceRange(
      textarea,
      firstLineStart,
      lastLineEnd,
      newBlock,
      'preserve',
      start + tabSpaces.length,
      end + addedTotal
    )
  } else {
    let firstLineRemoved = 0
    let totalRemoved = 0

    const newLines = lines.map((line, idx) => {
      let removed = 0
      for (let i = 0; i < props.tabSize; i++) {
        if (line.startsWith(' ')) {
          line = line.substring(1)
          removed++
        } else {
          break
        }
      }
      if (idx === 0) firstLineRemoved = removed
      totalRemoved += removed
      return line
    })

    if (totalRemoved === 0) return

    const newBlock = newLines.join('\n')
    const newStart = Math.max(firstLineStart, start - firstLineRemoved)
    const newEnd = Math.max(newStart, end - totalRemoved)
    replaceRange(textarea, firstLineStart, lastLineEnd, newBlock, 'preserve', newStart, newEnd)
  }
  updateCursor()
}

function handleEnter(textarea: HTMLTextAreaElement): boolean {
  if (!props.autoIndent) return false

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const val = textarea.value

  if (start !== end) return false

  const lineStart = val.lastIndexOf('\n', start - 1) === -1 ? 0 : val.lastIndexOf('\n', start - 1) + 1
  const lineBeforeCursor = val.substring(lineStart, start)

  const taskListMatch = lineBeforeCursor.match(/^(\s*)([-*+]\s+\[[ xX]\]\s+)(.*)$/)
  if (taskListMatch) {
    const [, indent, , content] = taskListMatch
    if (!content.trim()) {
      replaceRange(textarea, lineStart, start, indent, 'end')
      updateCursor()
      return true
    }
    const insert = `\n${indent}- [ ] `
    if (!document.execCommand('insertText', false, insert)) {
      replaceRange(textarea, start, end, insert, 'end')
    } else {
      handleInput(textarea.value)
    }
    updateCursor()
    return true
  }

  const bulletMatch = lineBeforeCursor.match(/^(\s*)([-*+])\s+(.*)$/)
  if (bulletMatch) {
    const [, indent, bullet, content] = bulletMatch
    if (!content.trim()) {
      replaceRange(textarea, lineStart, start, indent, 'end')
      updateCursor()
      return true
    }
    const insert = `\n${indent}${bullet} `
    if (!document.execCommand('insertText', false, insert)) {
      replaceRange(textarea, start, end, insert, 'end')
    } else {
      handleInput(textarea.value)
    }
    updateCursor()
    return true
  }

  const numListMatch = lineBeforeCursor.match(/^(\s*)(\d+)\.\s+(.*)$/)
  if (numListMatch) {
    const [, indent, numStr, content] = numListMatch
    if (!content.trim()) {
      replaceRange(textarea, lineStart, start, indent, 'end')
      updateCursor()
      return true
    }
    const nextNum = parseInt(numStr, 10) + 1
    const insert = `\n${indent}${nextNum}. `
    if (!document.execCommand('insertText', false, insert)) {
      replaceRange(textarea, start, end, insert, 'end')
    } else {
      handleInput(textarea.value)
    }
    updateCursor()
    return true
  }

  const quoteMatch = lineBeforeCursor.match(/^(\s*)>\s+(.*)$/)
  if (quoteMatch) {
    const [, indent, content] = quoteMatch
    if (!content.trim()) {
      replaceRange(textarea, lineStart, start, indent, 'end')
      updateCursor()
      return true
    }
    const insert = `\n${indent}> `
    if (!document.execCommand('insertText', false, insert)) {
      replaceRange(textarea, start, end, insert, 'end')
    } else {
      handleInput(textarea.value)
    }
    updateCursor()
    return true
  }

  const indentMatch = lineBeforeCursor.match(/^(\s+)/)
  if (indentMatch && indentMatch[1]) {
    const indent = indentMatch[1]
    const insert = `\n${indent}`
    if (!document.execCommand('insertText', false, insert)) {
      replaceRange(textarea, start, end, insert, 'end')
    } else {
      handleInput(textarea.value)
    }
    updateCursor()
    return true
  }

  return false
}

const PAIR_MAP: Record<string, string> = {
  '(': ')',
  '[': ']',
  '{': '}',
  '"': '"',
  "'": "'",
  '`': '`'
}
const CLOSING_SET = new Set([')', ']', '}', '"', "'", '`'])

function handleAutoPair(textarea: HTMLTextAreaElement, key: string): boolean {
  if (!props.autoPair) return false

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const val = textarea.value

  if (start !== end && PAIR_MAP[key]) {
    const selected = val.substring(start, end)
    const wrapped = `${key}${selected}${PAIR_MAP[key]}`
    replaceRange(textarea, start, end, wrapped, 'preserve', start + 1, end + 1)
    updateCursor()
    return true
  }

  if (start === end) {
    if (CLOSING_SET.has(key) && val[start] === key) {
      textarea.setSelectionRange(start + 1, start + 1)
      updateCursor()
      return true
    }
    if (PAIR_MAP[key]) {
      const pair = `${key}${PAIR_MAP[key]}`
      if (!document.execCommand('insertText', false, pair)) {
        replaceRange(textarea, start, end, pair, 'end')
      } else {
        handleInput(textarea.value)
      }
      textarea.setSelectionRange(start + 1, start + 1)
      updateCursor()
      return true
    }
  }

  return false
}

function handleBackspace(textarea: HTMLTextAreaElement): boolean {
  if (!props.autoPair) return false
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const val = textarea.value

  if (start === end && start > 0) {
    const prevChar = val[start - 1]
    const nextChar = val[start]
    if (PAIR_MAP[prevChar] === nextChar) {
      replaceRange(textarea, start - 1, start + 1, '', 'end')
      updateCursor()
      return true
    }
  }
  return false
}

function handleShortcuts(e: KeyboardEvent, textarea: HTMLTextAreaElement): boolean {
  if (!e.ctrlKey && !e.metaKey) return false

  const key = e.key.toLowerCase()

  if (key === 'b') {
    e.preventDefault()
    wrapOrInsert(textarea, '**', '**')
    return true
  }
  if (key === 'i') {
    e.preventDefault()
    wrapOrInsert(textarea, '*', '*')
    return true
  }
  if (key === 'k') {
    e.preventDefault()
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const selected = textarea.value.substring(start, end) || 'link'
    const linkMd = `[${selected}](url)`
    replaceRange(textarea, start, end, linkMd, 'end')
    const urlStart = start + selected.length + 3
    textarea.setSelectionRange(urlStart, urlStart + 3)
    updateCursor()
    return true
  }

  return false
}

function handleKeydown(e: KeyboardEvent) {
  if (props.disabled) return
  if (e.isComposing || e.keyCode === 229) return

  const textarea = getTextarea()
  if (!textarea) return

  if (handleShortcuts(e, textarea)) return

  if (e.key === 'Tab') {
    e.preventDefault()
    handleTab(textarea, e.shiftKey)
    return
  }

  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.metaKey && !e.altKey) {
    if (handleEnter(textarea)) {
      e.preventDefault()
      return
    }
  }

  if (['(', '[', '{', '"', "'", '`', ')', ']', '}'].includes(e.key)) {
    if (handleAutoPair(textarea, e.key)) {
      e.preventDefault()
      return
    }
  }

  if (e.key === 'Backspace') {
    if (handleBackspace(textarea)) {
      e.preventDefault()
      return
    }
  }

  nextTick(updateCursor)
}

function executeAction(actionType: string) {
  const textarea = getTextarea()
  if (!textarea || props.disabled) return
  textarea.focus()

  switch (actionType) {
    case 'bold':
      wrapOrInsert(textarea, '**', '**')
      break
    case 'italic':
      wrapOrInsert(textarea, '*', '*')
      break
    case 'code':
      wrapOrInsert(textarea, '`', '`')
      break
    case 'quote':
      wrapOrInsert(textarea, '> ', '')
      break
    case 'ul':
      wrapOrInsert(textarea, '- ', '')
      break
    case 'ol':
      wrapOrInsert(textarea, '1. ', '')
      break
    case 'link': {
      const start = textarea.selectionStart
      const end = textarea.selectionEnd
      const selected = textarea.value.substring(start, end) || 'link'
      const linkMd = `[${selected}](url)`
      replaceRange(textarea, start, end, linkMd, 'end')
      const urlStart = start + selected.length + 3
      textarea.setSelectionRange(urlStart, urlStart + 3)
      break
    }
    case 'table': {
      const tableTemplate = '\n| 列 1 | 列 2 | 列 3 |\n| --- | --- | --- |\n| 内容 | 内容 | 内容 |\n'
      insertText(textarea, tableTemplate)
      break
    }
  }
  updateCursor()
}
</script>

<style scoped lang="scss">
.v-markdown-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  border-radius: var(--v-input-radius, 8px) !important;
  border: 1px solid #d7d8da80 !important;
  overflow: hidden;
  background-color: transparent;
  box-shadow: none !important;
  transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover:not(.is-disabled) {
    border-color: #d7d8da80 !important;
  }

  &.is-focused,
  &:focus-within:not(.is-disabled) {
    border-color: #b7babd !important;
    box-shadow: none !important;
  }

  &.is-disabled {
    cursor: not-allowed !important;
    background-color: #f5f6f7 !important;
    opacity: 0.8;
    pointer-events: none;
    border-color: #d7d8da80 !important;
  }

  .editor-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 38px;
    min-height: 38px;
    padding: 0 10px;
    background: #f7f8f9;
    border-bottom: 1px solid #d7d8da80;
    box-sizing: border-box;

    .toolbar-left {
      display: inline-flex;
      align-items: center;
      height: 100%;
      gap: 8px;

      .editor-mode-switch {
        position: relative;
        display: inline-flex;
        align-items: center;
        width: 136px;
        padding: 2px;
        background: #ebecee;
        border-radius: 6px;
        box-sizing: border-box;
        user-select: none;

        .mode-slider {
          position: absolute;
          top: 2px;
          bottom: 2px;
          left: 2px;
          width: calc(50% - 2px);
          background: #ffffff;
          border-radius: 4px;
          box-shadow: 0 1px 2px rgba(60, 64, 67, 0.12), 0 1px 3px 1px rgba(60, 64, 67, 0.08);
          transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
          pointer-events: none;
          z-index: 1;
        }

        &.is-preview .mode-slider {
          transform: translateX(100%);
        }

        .mode-btn {
          position: relative;
          z-index: 2;
          flex: 1;
          height: 24px;
          padding: 0 6px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 4px;
          border: none;
          background: transparent;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 500;
          font-family: Inter, sans-serif;
          color: #5f6368;
          cursor: pointer;
          user-select: none;
          transition: color 0.15s ease;

          .n-icon {
            font-size: 13px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
          }

          &:hover:not(.is-active) {
            color: #1b1b1e;
          }

          &.is-active {
            color: #1b1b1e;
            font-weight: 600;
          }
        }
      }

      .toolbar-actions {
        display: inline-flex;
        align-items: center;
        gap: 2px;
        padding-left: 6px;
        border-left: 1px solid #d7d8da80;

        .action-divider {
          width: 1px;
          height: 14px;
          background: #d7d8da80;
          margin: 0 3px;
        }

        .action-btn {
          height: 24px;
          min-width: 24px;
          padding: 0 4px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          background: transparent;
          border: none;
          border-radius: 4px;
          color: #565b61;
          font-size: 12px;
          font-family: Inter, sans-serif;
          cursor: pointer;
          transition: background-color 0.15s cubic-bezier(0.4, 0, 0.2, 1), color 0.15s ease;
          user-select: none;

          &:hover {
            background: #e8eaed;
            color: #1b1b1e;
          }

          &:active {
            background: #dadce0;
          }

          &.quote-btn {
            font-size: 15px;
            font-family: serif;
          }

          &.text-icon {
            font-size: 11px;
            font-family: 'Fira Code', monospace;
            font-weight: 600;
          }

          .n-icon {
            font-size: 13px;
          }
        }
      }
    }

    .toolbar-right {
      display: inline-flex;
      align-items: center;
      height: 100%;

      .toolbar-tip {
        display: inline-flex;
        align-items: center;
        font-size: 11px;
        line-height: 1;
        font-family: Inter, sans-serif;
        color: #80868b;
        user-select: none;
      }
    }
  }

  .editor-input-wrap {
    width: 100%;

    .markdown-textarea {
      border: none !important;
      background: transparent !important;
      font-size: 14px;
      line-height: 21px;

      :deep(.n-input__border),
      :deep(.n-input__state-border) {
        display: none !important;
      }

      :deep(.n-input-wrapper) {
        padding: 0 !important;
        border-radius: 0 !important;
      }

      :deep(.n-input__textarea) {
        position: relative;
      }

      :deep(.n-input__textarea-el) {
        font-family: 'Fira Code', Inter, sans-serif;
        font-size: 14px;
        font-weight: 400;
        line-height: 21px;
        font-optical-sizing: auto;
        padding: 12px 14px;
        color: #1b1b1e;
        caret-color: #2b2d31 !important;
        box-sizing: border-box;

        &::placeholder {
          font-family: Inter, sans-serif;
          font-size: 14px;
          font-weight: 400;
          line-height: 21px;
          color: #9aa0a6;
        }
      }

      :deep(.n-input__placeholder) {
        padding: 12px 14px !important;
        font-family: Inter, sans-serif !important;
        font-size: 14px !important;
        font-weight: 400 !important;
        line-height: 21px !important;
        color: #9aa0a6 !important;
        box-sizing: border-box;
        pointer-events: none;

        > span {
          display: inline-block;
          font-family: inherit !important;
          font-size: 14px !important;
          line-height: 21px !important;
        }
      }
    }
  }

  .editor-preview-wrap {
    overflow-y: auto;
    padding: 14px 18px;
    background: #ffffff;

    .preview-empty {
      color: #9aa0a6;
      font-size: 13px;
      font-family: Inter, sans-serif;
      text-align: center;
      padding: 36px 0;
    }
  }

  .v-markdown-body {
    font-size: 14px;
    font-family: Inter, sans-serif;
    line-height: 21px;
    color: #1b1b1e;

    :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
      font-weight: 600;
      margin-top: 1em;
      margin-bottom: 0.5em;
      line-height: 1.3;
      color: #1b1b1e;
    }
    :deep(h1) { font-size: 1.5em; border-bottom: 1px solid #d7d8da80; padding-bottom: 0.3em; }
    :deep(h2) { font-size: 1.3em; border-bottom: 1px solid #d7d8da80; padding-bottom: 0.3em; }
    :deep(h3) { font-size: 1.15em; }
    :deep(p) { margin: 0.6em 0; }
    :deep(ul), :deep(ol) { padding-left: 1.5em; margin: 0.5em 0; }
    :deep(ul) { list-style-type: disc; }
    :deep(ol) { list-style-type: decimal; }
    :deep(li) { margin: 0.25em 0; }
    :deep(blockquote) {
      margin: 0.8em 0;
      padding: 0.4em 1em;
      border-left: 3px solid #b7babd;
      background: #f7f8f9;
      color: #565b61;
      border-radius: 0 4px 4px 0;
    }
    :deep(pre) {
      background: #1b1b1e;
      color: #e8eaed;
      padding: 12px;
      border-radius: var(--v-input-radius, 8px);
      overflow-x: auto;
      font-family: 'Fira Code', monospace;
      font-size: 13px;
      margin: 0.8em 0;
    }
    :deep(code) {
      font-family: 'Fira Code', monospace;
      font-size: 0.9em;
      background: rgba(0, 0, 0, 0.05);
      padding: 2px 5px;
      border-radius: 4px;
      color: #d93025;
    }
    :deep(pre code) {
      background: transparent;
      padding: 0;
      color: inherit;
    }
    :deep(table) {
      width: 100%;
      border-collapse: collapse;
      margin: 0.8em 0;
      font-size: 13px;
    }
    :deep(th), :deep(td) {
      border: 1px solid #d7d8da80;
      padding: 6px 12px;
      text-align: left;
    }
    :deep(th) {
      background: #f7f8f9;
      font-weight: 600;
    }
    :deep(a) {
      color: #1a73e8;
      text-decoration: none;
      &:hover { text-decoration: underline; }
    }
    :deep(hr) {
      border: none;
      border-top: 1px solid #d7d8da80;
      margin: 1.2em 0;
    }
    :deep(img) {
      max-width: 100%;
      border-radius: 6px;
    }
  }

  .editor-statusbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 28px;
    padding: 4px 12px;
    background: #f7f8f9;
    border-top: 1px solid #d7d8da80;
    font-size: 12px;
    font-family: Inter, sans-serif;
    line-height: 1;
    color: #80868b;
    box-sizing: border-box;
    user-select: none;

    .statusbar-left {
      display: flex;
      align-items: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;

      .shortcut-tip {
        color: #565b61;
        font-weight: 400;
      }

      .status-hint {
        color: #80868b;
      }
    }

    .statusbar-right {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-shrink: 0;

      .stat-item {
        font-variant-numeric: tabular-nums;
        color: #565b61;
      }

      .stat-divider {
        color: #d7d8da80;
      }
    }
  }

  &.theme-dark,
  html.dark &,
  [data-theme='dark'] & {
    border-color: #262626 !important;
    background-color: transparent;

    &:hover:not(.is-disabled) {
      border-color: #262626 !important;
    }

    &.is-focused,
    &:focus-within:not(.is-disabled) {
      border-color: #333 !important;
      box-shadow: none !important;
    }

    &.is-disabled {
      background-color: rgba(255, 255, 255, 0.04) !important;
      border-color: #262626 !important;
    }

    .editor-toolbar {
      background: #191a1b;
      border-bottom-color: #262626;

      .toolbar-left {
        .editor-mode-switch {
          background: #262626;

          .mode-slider {
            background: #141415;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
          }

          .mode-btn {
            color: #80868b;

            &:hover:not(.is-active) {
              color: #d4d4d4;
            }

            &.is-active {
              color: #d4d4d4;
            }
          }
        }

        .toolbar-actions {
          border-left-color: #262626;

          .action-divider {
            background: #262626;
          }

          .action-btn {
            color: #8c8c8c;

            &:hover {
              background: #2b2d31;
              color: #d4d4d4;
            }

            &:active {
              background: #333;
            }
          }
        }
      }

      .toolbar-right .toolbar-tip {
        color: #80868b;
      }
    }

    .editor-input-wrap .markdown-textarea {
      :deep(.n-input__textarea-el) {
        color: #d4d4d4;
        caret-color: #d4d4d4 !important;

        &::placeholder {
          color: #80868b;
        }
      }

      :deep(.n-input__placeholder) {
        color: #80868b !important;
      }
    }

    .editor-preview-wrap {
      background: #141415;

      .preview-empty {
        color: #80868b;
      }
    }

    .v-markdown-body {
      color: #d4d4d4;

      :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
        color: #e3e3e3;
      }
      :deep(h1), :deep(h2) {
        border-bottom-color: #262626;
      }
      :deep(blockquote) {
        border-left-color: #444;
        background: rgba(255, 255, 255, 0.03);
        color: #a6a6a6;
      }
      :deep(code) {
        background: rgba(255, 255, 255, 0.08);
        color: #f28b82;
      }
      :deep(pre) {
        background: #111112;
        color: #d4d4d4;
        border: 1px solid #262626;
      }
      :deep(th), :deep(td) {
        border-color: #262626;
      }
      :deep(th) {
        background: #1a1a1c;
      }
      :deep(a) {
        color: #8ab4f8;
      }
      :deep(hr) {
        border-top-color: #262626;
      }
    }

    .editor-statusbar {
      background: #191a1b;
      border-top-color: #262626;
      color: #80868b;

      .statusbar-left {
        .shortcut-tip {
          color: #a6a6a6;
        }

        .status-hint {
          color: #80868b;
        }
      }

      .statusbar-right {
        .stat-item {
          color: #a6a6a6;
        }

        .stat-divider {
          color: #262626;
        }
      }
    }
  }
}
</style>
