<template>
  <div
    class="volur-markdown-editor"
    :class="{
      'is-focused': isFocused,
      'is-disabled': disabled
    }"
  >
    <!-- Editor Header Toolbar -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <!-- Mode switch: Edit / Preview with sliding animation -->
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
            <span>{{ t('common.markdown_edit') }}</span>
          </button>
          <button
            type="button"
            class="mode-btn"
            :class="{ 'is-active': activeMode === 'preview' }"
            :disabled="disabled"
            @click="activeMode = 'preview'"
          >
            <n-icon :component="EyeOutline" />
            <span>{{ t('common.markdown_preview') }}</span>
          </button>
        </div>

        <!-- Quick Markdown formatting tools (visible in Edit mode) -->
        <div
          v-show="activeMode === 'edit' && showToolbarActions"
          class="toolbar-actions"
        >
          <button
            type="button"
            class="action-btn"
            :title="t('common.markdown_action_bold')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('bold')"
          >
            <strong>B</strong>
          </button>
          <button
            type="button"
            class="action-btn italic"
            :title="t('common.markdown_action_italic')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('italic')"
          >
            <em>I</em>
          </button>
          <span class="action-divider" />
          <button
            type="button"
            class="action-btn"
            :title="t('common.markdown_action_code')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('code')"
          >
            <n-icon :component="CodeSlashOutline" />
          </button>
          <button
            type="button"
            class="action-btn quote-btn"
            :title="t('common.markdown_action_quote')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('quote')"
          >
            &ldquo;
          </button>
          <button
            type="button"
            class="action-btn"
            :title="t('common.markdown_action_list_ul')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('ul')"
          >
            <n-icon :component="ListOutline" />
          </button>
          <button
            type="button"
            class="action-btn text-icon"
            :title="t('common.markdown_action_list_ol')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('ol')"
          >
            1.
          </button>
          <span class="action-divider" />
          <button
            type="button"
            class="action-btn"
            :title="t('common.markdown_action_link')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('link')"
          >
            <n-icon :component="LinkOutline" />
          </button>
          <button
            type="button"
            class="action-btn"
            :title="t('common.markdown_action_table')"
            :disabled="disabled"
            @mousedown.prevent="executeAction('table')"
          >
            <n-icon :component="GridOutline" />
          </button>
        </div>
      </div>

      <div v-if="showTip" class="toolbar-right">
        <span class="toolbar-tip">{{ tipText || t('common.markdown_tip') }}</span>
      </div>
    </div>

    <!-- Mode 1: Edit Mode -->
    <div v-show="activeMode === 'edit'" class="editor-input-wrap">
      <n-input
        ref="inputInstRef"
        :value="currentValue"
        type="textarea"
        :rows="rows"
        :disabled="disabled"
        :placeholder="placeholder || t('common.markdown_placeholder')"
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

    <!-- Editor Status Bar & Shortcut Assistance -->
    <div v-if="showStatusBar" class="editor-statusbar">
      <div class="statusbar-left">
        <span v-if="isFocused" class="shortcut-tip">
          {{ t('common.markdown_shortcuts_hint') }}
        </span>
        <span v-else class="status-hint">
          {{ tipText || t('common.markdown_tip') }}
        </span>
      </div>
      <div class="statusbar-right">
        <span v-if="activeMode === 'edit'" class="stat-item">
          {{ t('common.markdown_cursor', { line: cursorLine, col: cursorCol }) }}
        </span>
        <span v-if="activeMode === 'edit'" class="stat-divider">·</span>
        <span class="stat-item">
          {{ t('common.markdown_words', { count: charCount }) }}
        </span>
        <span class="stat-divider">·</span>
        <span class="stat-item">
          {{ t('common.markdown_lines', { count: lineCount }) }}
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
import { renderMarkdown } from '@/utils/markdown'

export interface VolurMarkdownEditorProps {
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
  tabSize?: number
  autoPair?: boolean
  autoIndent?: boolean
  showStatusBar?: boolean
  showToolbarActions?: boolean
}

const props = withDefaults(defineProps<VolurMarkdownEditorProps>(), {
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
  showToolbarActions: true
})

const emit = defineEmits<{
  (e: 'update:value', val: string): void
  (e: 'update:modelValue', val: string): void
  (e: 'change', val: string): void
  (e: 'focus', event: FocusEvent): void
  (e: 'blur', event: FocusEvent): void
}>()

const { t } = useI18n()

// =========================================================================
// Reactive State
// =========================================================================
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

// =========================================================================
// Helpers & Text Operations
// =========================================================================
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

/**
 * Replace text range and preserve native input / history events
 */
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

/**
 * Insert text at cursor with native undo support where possible
 */
function insertText(textarea: HTMLTextAreaElement, text: string) {
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  if (!document.execCommand('insertText', false, text)) {
    replaceRange(textarea, start, end, text, 'end')
  } else {
    handleInput(textarea.value)
  }
}

/**
 * Wrap selected text with prefix & suffix or insert placeholder
 */
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
    // Toggle unwrap if already wrapped
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

// =========================================================================
// Tab & Shift+Tab Indentation
// =========================================================================
function handleTab(textarea: HTMLTextAreaElement, isShift: boolean) {
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const val = textarea.value
  const tabSpaces = ' '.repeat(props.tabSize)

  const isMultiLine = val.substring(start, end).includes('\n')

  // Single line / point cursor (Normal Tab)
  if (!isMultiLine && !isShift) {
    if (!document.execCommand('insertText', false, tabSpaces)) {
      replaceRange(textarea, start, end, tabSpaces, 'end')
    } else {
      handleInput(textarea.value)
    }
    updateCursor()
    return
  }

  // Multi-line block indentation
  const firstLineStart = val.lastIndexOf('\n', start - 1) === -1 ? 0 : val.lastIndexOf('\n', start - 1) + 1
  let lastLineEnd = val.indexOf('\n', end)
  if (lastLineEnd === -1) lastLineEnd = val.length

  const lines = val.substring(firstLineStart, lastLineEnd).split('\n')

  if (!isShift) {
    // Indent block: add spaces to beginning of each line
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
    // Unindent block: remove up to tabSize spaces from line start
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

// =========================================================================
// Enter Key: Auto-Indent & Markdown List Continuation
// =========================================================================
function handleEnter(textarea: HTMLTextAreaElement): boolean {
  if (!props.autoIndent) return false

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const val = textarea.value

  if (start !== end) return false

  const lineStart = val.lastIndexOf('\n', start - 1) === -1 ? 0 : val.lastIndexOf('\n', start - 1) + 1
  const lineBeforeCursor = val.substring(lineStart, start)

  // 1. Task List: e.g. "  - [ ] ", "  - [x] "
  const taskListMatch = lineBeforeCursor.match(/^(\s*)([-*+]\s+\[[ xX]\]\s+)(.*)$/)
  if (taskListMatch) {
    const [, indent, , content] = taskListMatch
    if (!content.trim()) {
      // Empty task item: user wants to exit list
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

  // 2. Unordered List: e.g. "  - ", "  * ", "  + "
  const bulletMatch = lineBeforeCursor.match(/^(\s*)([-*+])\s+(.*)$/)
  if (bulletMatch) {
    const [, indent, bullet, content] = bulletMatch
    if (!content.trim()) {
      // Empty bullet: exit list
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

  // 3. Ordered List: e.g. "  1. ", "  2. "
  const numListMatch = lineBeforeCursor.match(/^(\s*)(\d+)\.\s+(.*)$/)
  if (numListMatch) {
    const [, indent, numStr, content] = numListMatch
    if (!content.trim()) {
      // Empty numbered item: exit list
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

  // 4. Blockquote: e.g. "  > "
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

  // 5. Code block or indented text: preserve leading whitespace
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

// =========================================================================
// Auto-Pairing & Backspace Handling
// =========================================================================
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

  // Case 1: Text is selected -> wrap selection
  if (start !== end && PAIR_MAP[key]) {
    const selected = val.substring(start, end)
    const wrapped = `${key}${selected}${PAIR_MAP[key]}`
    replaceRange(textarea, start, end, wrapped, 'preserve', start + 1, end + 1)
    updateCursor()
    return true
  }

  // Case 2: Cursor without selection
  if (start === end) {
    // Skip closing character if next character is already it
    if (CLOSING_SET.has(key) && val[start] === key) {
      textarea.setSelectionRange(start + 1, start + 1)
      updateCursor()
      return true
    }
    // Insert pair and place cursor in middle
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

// =========================================================================
// Keyboard Shortcuts (Ctrl/Cmd + B, I, K)
// =========================================================================
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

// =========================================================================
// Global Keydown Router
// =========================================================================
function handleKeydown(e: KeyboardEvent) {
  if (props.disabled) return
  // IME safety check
  if (e.isComposing || e.keyCode === 229) return

  const textarea = getTextarea()
  if (!textarea) return

  // 1. Markdown shortcuts (Ctrl+B, Ctrl+I, Ctrl+K)
  if (handleShortcuts(e, textarea)) return

  // 2. Tab / Shift+Tab Indentation
  if (e.key === 'Tab') {
    e.preventDefault()
    handleTab(textarea, e.shiftKey)
    return
  }

  // 3. Enter (Auto-Indent & List Continuation)
  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.metaKey && !e.altKey) {
    if (handleEnter(textarea)) {
      e.preventDefault()
      return
    }
  }

  // 4. Auto Pair
  if (['(', '[', '{', '"', "'", '`', ')', ']', '}'].includes(e.key)) {
    if (handleAutoPair(textarea, e.key)) {
      e.preventDefault()
      return
    }
  }

  // 5. Backspace inside pair
  if (e.key === 'Backspace') {
    if (handleBackspace(textarea)) {
      e.preventDefault()
      return
    }
  }

  nextTick(updateCursor)
}

// =========================================================================
// Toolbar Quick Action Click Handler
// =========================================================================
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
.volur-markdown-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--v-border-base, #e5e4e7);
  border-radius: 8px;
  overflow: hidden;
  background: var(--v-bg-card, #ffffff);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  // Focused state (no ring shadow per user requirement)
  &.is-focused {
    box-shadow: none !important;
  }

  &.is-disabled {
    opacity: 0.65;
    pointer-events: none;
  }

  .editor-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 40px;
    min-height: 40px;
    padding: 0 14px;
    background: rgba(0, 0, 0, 0.02);
    border-bottom: 1px solid var(--v-border-base, #e5e4e7);
    box-sizing: border-box;

    .toolbar-left {
      display: inline-flex;
      align-items: center;
      height: 100%;
      gap: 12px;

      .editor-mode-switch {
        position: relative;
        display: inline-flex;
        align-items: center;
        width: 156px;
        padding: 2px;
        background: rgba(0, 0, 0, 0.05);
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
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
          transition: transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);
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
          height: 26px;
          padding: 0 8px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 6px;
          border: none;
          background: transparent;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 500;
          color: var(--v-text-desc, #444444);
          cursor: pointer;
          user-select: none;
          transition: color 0.2s ease, font-weight 0.2s ease;

          .n-icon {
            font-size: 14px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            transition: color 0.2s ease;
          }

          &:hover:not(.is-active) {
            color: #111111;
          }

          &.is-active {
            color: #111111;
            font-weight: 600;
          }
        }
      }

      .toolbar-actions {
        display: inline-flex;
        align-items: center;
        gap: 3px;
        padding-left: 10px;
        border-left: 1px solid var(--v-border-base, #e5e4e7);

        .action-divider {
          width: 1px;
          height: 14px;
          background: var(--v-border-base, #e5e4e7);
          margin: 0 3px;
        }

        .action-btn {
          height: 26px;
          min-width: 26px;
          padding: 0 5px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          background: transparent;
          border: none;
          border-radius: 4px;
          color: var(--v-text-desc, #444444);
          font-size: 12px;
          cursor: pointer;
          transition: background-color 0.15s ease, color 0.15s ease;
          user-select: none;

          &:hover {
            background: rgba(0, 0, 0, 0.06);
            color: #000000;
          }

          &:active {
            background: rgba(0, 0, 0, 0.1);
          }

          &.quote-btn {
            font-size: 15px;
            font-family: serif;
          }

          &.text-icon {
            font-size: 11px;
            font-family: var(--v-font-mono, monospace);
            font-weight: 600;
          }

          .n-icon {
            font-size: 14px;
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
        font-size: 12px;
        line-height: 1;
        color: var(--v-text-desc, #444444);
        user-select: none;
      }
    }
  }

  .editor-input-wrap {
    width: 100%;

    .markdown-textarea {
      border: none !important;
      background: transparent !important;

      :deep(.n-input__border),
      :deep(.n-input__state-border) {
        display: none !important;
      }

      :deep(.n-input-wrapper) {
        padding: 0 !important;
      }

      :deep(.n-input__textarea) {
        position: relative;
      }

      :deep(.n-input__textarea-el) {
        font-family: var(--v-font-mono, 'Fira Code', monospace), -apple-system, sans-serif;
        font-size: 13px;
        line-height: 1.6;
        padding: 12px 14px;
        color: var(--v-text-primary, #111111);
        caret-color: #666666; // 比 placeholder (#999999) 深一步
        box-sizing: border-box;
      }

      :deep(.n-input__placeholder) {
        padding: 12px 14px !important;
        font-family: var(--v-font-mono, 'Fira Code', monospace), -apple-system, sans-serif !important;
        font-size: 13px !important;
        line-height: 1.6 !important;
        color: #999999 !important;
        box-sizing: border-box;
        pointer-events: none;

        > span {
          display: inline-block;
          font-family: inherit !important;
          font-size: inherit !important;
          line-height: 1.6 !important;
        }
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

  .editor-statusbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 30px;
    padding: 6px 14px;
    background: rgba(0, 0, 0, 0.015);
    border-top: 1px solid var(--v-border-base, #e5e4e7);
    font-size: 11px;
    line-height: 1;
    color: #666666;
    box-sizing: border-box;
    user-select: none;

    .statusbar-left {
      display: flex;
      align-items: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;

      .shortcut-tip {
        color: var(--v-text-desc, #444444);
        font-weight: 500;
      }

      .status-hint {
        color: var(--v-text-muted, #888888);
      }
    }

    .statusbar-right {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-shrink: 0;

      .stat-item {
        font-variant-numeric: tabular-nums;
        color: var(--v-text-desc, #444444);
      }

      .stat-divider {
        color: var(--v-border-base, #cccccc);
      }
    }
  }

  // =========================================================================
  // Dark Theme
  // =========================================================================
  html.dark &,
  [data-theme='dark'] & {
    background: var(--v-bg-card, #151821);
    border-color: var(--v-border-base, rgba(255, 255, 255, 0.09));

    &.is-focused {
      box-shadow: none !important;
    }

    .editor-toolbar {
      background: rgba(255, 255, 255, 0.02);
      border-bottom-color: var(--v-border-base, rgba(255, 255, 255, 0.09));

      .toolbar-left {
        .editor-mode-switch {
          background: rgba(255, 255, 255, 0.07);

          .mode-slider {
            background: #232733;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.35);
          }

          .mode-btn {
            color: var(--v-text-desc, #94a3b8);

            &:hover:not(.is-active) {
              color: #ffffff;
            }

            &.is-active {
              color: #ffffff;
            }
          }
        }

        .toolbar-actions {
          border-left-color: rgba(255, 255, 255, 0.09);

        .action-divider {
          background: rgba(255, 255, 255, 0.09);
        }

        .action-btn {
          color: var(--v-text-desc, #94a3b8);

          &:hover {
            background: rgba(255, 255, 255, 0.08);
            color: #ffffff;
          }

          &:active {
            background: rgba(255, 255, 255, 0.14);
          }
        }
      }
    }

    .toolbar-right .toolbar-tip {
      color: var(--v-text-desc, #94a3b8);
    }
  }

    .editor-input-wrap .markdown-textarea {
      :deep(.n-input__textarea-el) {
        caret-color: #94a3b8;
        color: #e2e8f0;
      }

      :deep(.n-input__placeholder) {
        color: #64748b !important;
      }
    }

    .editor-preview-wrap {
      background: var(--v-bg-card, #151821);

      .preview-empty {
        color: #64748b;
      }
    }

    .editor-statusbar {
      background: rgba(255, 255, 255, 0.015);
      border-top-color: var(--v-border-base, rgba(255, 255, 255, 0.09));
      color: #64748b;

      .statusbar-left {
        .shortcut-tip {
          color: var(--v-text-desc, #94a3b8);
        }

        .status-hint {
          color: #64748b;
        }
      }

      .statusbar-right {
        .stat-item {
          color: var(--v-text-desc, #94a3b8);
        }

        .stat-divider {
          color: rgba(255, 255, 255, 0.15);
        }
      }
    }
  }
}
</style>
