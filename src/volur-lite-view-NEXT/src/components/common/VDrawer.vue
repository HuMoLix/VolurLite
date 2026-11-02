<template>
  <Teleport to="body">
    <div
      v-show="isVisible"
      class="v-drawer-root"
      :class="currentTheme"
    >
      <Transition name="v-drawer-fade">
        <div
          v-if="show"
          class="v-drawer-mask"
          @click="handleMaskClick"
        />
      </Transition>

      <Transition
        :name="transitionName"
        @after-leave="handleAfterLeave"
      >
        <div
          v-if="show"
          class="v-drawer-panel"
          :class="[`placement-${placement}`]"
          :style="panelStyle"
          role="dialog"
          aria-modal="true"
          :aria-label="title"
        >
          <header class="v-drawer-header">
            <div class="v-drawer-title-wrapper">
              <slot name="header">
                <h2 class="v-drawer-title">{{ title }}</h2>
              </slot>
            </div>
            <button
              v-if="closable"
              type="button"
              class="v-drawer-close-btn"
              :aria-label="closeAriaLabel || t('common.close', '关闭')"
              @click="handleClose"
            >
              <n-icon :size="20">
                <CloseOutline />
              </n-icon>
            </button>
          </header>

          <main class="v-drawer-body">
            <slot />
          </main>

          <footer v-if="$slots.footer" class="v-drawer-footer">
            <slot name="footer" />
          </footer>
        </div>
      </Transition>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import type { CSSProperties } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { CloseOutline } from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import type { VDrawerProps } from './types'

defineOptions({
  name: 'VDrawer'
})

const props = withDefaults(defineProps<VDrawerProps>(), {
  show: false,
  title: '',
  width: 400,
  placement: 'right',
  closable: true,
  mask: true,
  maskClosable: true,
  theme: undefined,
  closeAriaLabel: ''
})

const emit = defineEmits<{
  'update:show': [value: boolean]
  close: []
  'after-leave': []
}>()

const { t } = useI18n()
const appStore = useAppStore()

const isVisible = ref<boolean>(props.show)

const currentTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const transitionName = computed<string>(() => {
  return props.placement === 'left' ? 'v-drawer-slide-left' : 'v-drawer-slide-right'
})

const panelStyle = computed<CSSProperties>(() => {
  const w = typeof props.width === 'number' ? `${props.width}px` : props.width || '400px'
  return {
    width: w,
    maxWidth: '100vw'
  }
})

watch(
  () => props.show,
  (val) => {
    if (val) {
      isVisible.value = true
      document.body.style.overflow = 'hidden'
    }
  },
  { immediate: true }
)

function handleMaskClick() {
  if (props.maskClosable) {
    handleClose()
  }
}

function handleClose() {
  emit('update:show', false)
  emit('close')
}

function handleAfterLeave() {
  if (!props.show) {
    isVisible.value = false
    document.body.style.overflow = ''
    emit('after-leave')
  }
}

function handleKeydown(ev: KeyboardEvent) {
  if (ev.key === 'Escape' && props.show) {
    handleClose()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})
</script>

<style scoped lang="scss">
.v-drawer-root {
  position: fixed;
  inset: 0;
  z-index: 2000;
  pointer-events: none;
  background: transparent;
}

.v-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background-color: rgba(32, 33, 36, 0.4);
  pointer-events: auto;
}

.v-drawer-panel {
  position: fixed;
  top: 0;
  bottom: 0;
  z-index: 2001;
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100vh;
  box-sizing: border-box;
  background-color: var(--v-bg-surface, #ffffff);
  border-radius: 8px 0 0 8px;
  box-shadow: 0 1px 3px 0 rgba(48, 48, 48, 0.302), 0 4px 8px 3px rgba(48, 48, 48, 0.149);
  outline: none;
  overflow: hidden;
  pointer-events: auto;

  &.placement-right {
    right: 0;
  }

  &.placement-left {
    left: 0;
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.12);
  }
}

.v-drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--v-border-base, #dadce0);
  box-sizing: border-box;

  .v-drawer-title-wrapper {
    flex: 1;
    min-width: 0;

    .v-drawer-title {
      margin: 0;
      font-size: 22px;
      font-weight: 400;
      line-height: 1.4;
      color: var(--v-text-primary, #202124);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .v-drawer-close-btn {
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    margin-left: 12px;
    border-radius: 50%;
    border: none;
    background: transparent;
    color: var(--v-text-muted, #5f6368);
    cursor: pointer;
    transition: background-color 0.15s ease, color 0.15s ease;

    &:hover {
      background-color: rgba(60, 64, 67, 0.08);
      color: var(--v-text-primary, #202124);
    }
  }
}

.v-drawer-body {
  flex: 1 1 auto;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px;
  box-sizing: border-box;
}

.v-drawer-footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--v-border-base, #dadce0);
  background-color: var(--v-bg-surface, #ffffff);
  box-sizing: border-box;
}

html.dark .v-drawer-root,
.v-drawer-root.dark {
  .v-drawer-mask {
    background-color: rgba(0, 0, 0, 0.65);
  }

  .v-drawer-panel {
    background-color: #1e1f20;
    box-shadow: -4px 0 24px rgba(0, 0, 0, 0.45);

    &.placement-left {
      box-shadow: 4px 0 24px rgba(0, 0, 0, 0.45);
    }
  }

  .v-drawer-header {
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
    border: none;

    .v-drawer-title-wrapper .v-drawer-title {
      color: #e3e3e3;
    }

    .v-drawer-close-btn {
      color: #9aa0a6;

      &:hover {
        background-color: rgba(255, 255, 255, 0.08);
        color: #e3e3e3;
      }
    }
  }

  .v-drawer-footer {
    box-shadow: 0 -1px 4px rgba(0, 0, 0, 0.3);
    border: none;
    background-color: #1e1f20;
  }
}

.v-drawer-fade-enter-active,
.v-drawer-fade-leave-active {
  transition: opacity 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.v-drawer-fade-enter-from,
.v-drawer-fade-leave-to {
  opacity: 0;
}

.v-drawer-fade-enter-to,
.v-drawer-fade-leave-from {
  opacity: 1;
}

.v-drawer-slide-right-enter-active,
.v-drawer-slide-right-leave-active {
  transition: transform 0.28s cubic-bezier(0, 0, 0.2, 1);
}

.v-drawer-slide-right-enter-from,
.v-drawer-slide-right-leave-to {
  transform: translateX(100%);
}

.v-drawer-slide-right-enter-to,
.v-drawer-slide-right-leave-from {
  transform: translateX(0);
}

.v-drawer-slide-left-enter-active,
.v-drawer-slide-left-leave-active {
  transition: transform 0.28s cubic-bezier(0, 0, 0.2, 1);
}

.v-drawer-slide-left-enter-from,
.v-drawer-slide-left-leave-to {
  transform: translateX(-100%);
}

.v-drawer-slide-left-enter-to,
.v-drawer-slide-left-leave-from {
  transform: translateX(0);
}
</style>
