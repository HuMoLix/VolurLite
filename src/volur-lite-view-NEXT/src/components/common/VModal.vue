<template>
  <n-modal
    :show="show"
    :mask="mask"
    :mask-closable="maskClosable"
    :close-on-esc="closeOnEsc"
    :auto-focus="autoFocus"
    :trap-focus="trapFocus"
    :z-index="zIndex"
    :to="to"
    @update:show="handleUpdateShow"
    @mask-click="handleMaskClick"
    @esc="handleEsc"
    @after-enter="emit('after-enter')"
    @after-leave="emit('after-leave')"
  >
    <div
      class="v-modal-panel"
      :class="[currentTheme, panelClass]"
      :style="panelStyle"
      role="dialog"
      aria-modal="true"
      :aria-label="title"
    >
      <slot name="close">
        <button
          v-if="closable"
          type="button"
          class="v-modal-close-btn"
          :aria-label="closeAriaLabel || t('common.close', '关闭')"
          @click="handleClose"
        >
          <n-icon :size="20">
            <CloseOutline />
          </n-icon>
        </button>
      </slot>

      <header
        v-if="showHeader && ($slots.header || title || description)"
        class="v-modal-header"
        :class="headerClass"
      >
        <div class="v-modal-title-wrapper">
          <slot name="header">
            <h2 class="v-modal-title">{{ title }}</h2>
            <p v-if="description" class="v-modal-desc">{{ description }}</p>
          </slot>
        </div>
      </header>

      <div class="v-modal-container" :class="contentClass">
        <slot name="container">
          <slot />
        </slot>
      </div>

      <footer
        v-if="$slots.footer"
        class="v-modal-footer"
        :class="footerClass"
      >
        <slot name="footer" />
      </footer>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CSSProperties } from 'vue'
import { useI18n } from 'vue-i18n'
import { NModal, NIcon } from 'naive-ui'
import { CloseOutline } from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import type { VModalProps } from './types'

defineOptions({
  name: 'VModal'
})

const props = withDefaults(defineProps<VModalProps>(), {
  show: false,
  title: '',
  description: '',
  width: 560,
  maxWidth: '92vw',
  height: undefined,
  maxHeight: '88vh',
  bordered: true,
  round: true,
  radius: 16,
  closable: true,
  mask: true,
  maskClosable: true,
  closeOnEsc: true,
  autoFocus: true,
  trapFocus: false,
  zIndex: undefined,
  to: undefined,
  showHeader: true,
  theme: undefined,
  panelClass: '',
  panelStyle: undefined,
  contentClass: '',
  headerClass: '',
  footerClass: '',
  closeAriaLabel: ''
})

const emit = defineEmits<{
  'update:show': [value: boolean]
  close: []
  'after-enter': []
  'after-leave': []
  'mask-click': []
  esc: []
}>()

const { t } = useI18n()
const appStore = useAppStore()

const currentTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const panelStyle = computed<CSSProperties>(() => {
  const w = typeof props.width === 'number' ? `${props.width}px` : props.width
  const maxW = typeof props.maxWidth === 'number' ? `${props.maxWidth}px` : props.maxWidth
  const h = typeof props.height === 'number' ? `${props.height}px` : props.height
  const maxH = typeof props.maxHeight === 'number' ? `${props.maxHeight}px` : props.maxHeight
  const r = props.round
    ? (typeof props.radius === 'number' ? `${props.radius}px` : props.radius || '16px')
    : '0px'

  const style: CSSProperties = {
    width: w,
    maxWidth: maxW,
    borderRadius: r,
    ...(props.panelStyle || {})
  }
  if (h) {
    style.height = h
  }
  if (maxH) {
    style.maxHeight = maxH
  }
  return style
})

function handleClose() {
  emit('update:show', false)
  emit('close')
}

function handleUpdateShow(value: boolean) {
  emit('update:show', value)
  if (!value) {
    emit('close')
  }
}

function handleMaskClick() {
  emit('mask-click')
}

function handleEsc() {
  emit('esc')
}
</script>

<style scoped lang="scss">
.v-modal-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  background-color: #ffffff;
  color: #202124;
  border: 1px solid #dadce0;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.12);
  outline: none;
  overflow: hidden;

  &.dark {
    background-color: #1e1f20;
    color: #e3e3e3;
    border-color: #3c4043;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45);
  }
}

html.dark .v-modal-panel {
  background-color: #1e1f20;
  color: #e3e3e3;
  border-color: #3c4043;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45);
}

.v-modal-header {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 0;
  padding-right: 52px;
  box-sizing: border-box;

  .v-modal-title-wrapper {
    flex: 1;
    min-width: 0;

    .v-modal-title {
      margin: 0;
      font-size: 18px;
      font-weight: 500;
      line-height: 1.4;
      color: #202124;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .v-modal-desc {
      margin: 4px 0 0;
      font-size: 13px;
      font-weight: 400;
      line-height: 1.4;
      color: #5f6368;
    }
  }
}

.v-modal-container {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 20px;
  box-sizing: border-box;

  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: rgba(60, 64, 67, 0.2);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background-color: rgba(60, 64, 67, 0.35);
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

.v-modal-footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 0 20px 20px;
  box-sizing: border-box;
}

.v-modal-close-btn {
  position: absolute;
  top: 12px;
  right: 14px;
  z-index: 10;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #5f6368;
  cursor: pointer;
  outline: none;
  transition: background-color 0.15s ease, color 0.15s ease;

  &:hover {
    background-color: rgba(60, 64, 67, 0.08);
    color: #202124;
  }
}

html.dark,
.dark {
  .v-modal-header {
    .v-modal-title-wrapper {
      .v-modal-title {
        color: #e3e3e3;
      }

      .v-modal-desc {
        color: #9aa0a6;
      }
    }
  }

  .v-modal-close-btn {
    color: #9aa0a6;

    &:hover {
      background-color: rgba(255, 255, 255, 0.08);
      color: #e3e3e3;
    }
  }

  .v-modal-container {
    &::-webkit-scrollbar-thumb {
      background-color: rgba(255, 255, 255, 0.2);
    }

    &::-webkit-scrollbar-thumb:hover {
      background-color: rgba(255, 255, 255, 0.35);
    }
  }
}
</style>
