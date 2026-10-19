<template>
  <div
    class="v-card"
    :class="[
      currentTheme,
      {
        dark: currentTheme === 'dark',
        'is-bordered': bordered
      }
    ]"
    :style="cardStyle"
  >
    <slot name="header">
      <div v-if="hasHeader" class="card-top-layer">
        <slot name="header-left">
          <div v-if="title" class="card-title-group">
            <span class="card-title">{{ title }}</span>
          </div>
        </slot>
        <slot name="header-center" />
        <slot name="header-right">
          <button
            v-if="closable"
            type="button"
            class="card-close-btn"
            :aria-label="closeAriaLabel || t('common.close', '关闭')"
            @click="handleClose"
          >
            <n-icon :size="16">
              <CloseOutline />
            </n-icon>
          </button>
        </slot>
      </div>
    </slot>

    <slot />

    <slot name="footer">
      <div v-if="$slots['footer-content']" class="card-bottom-layer">
        <slot name="footer-content" />
      </div>
    </slot>
  </div>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { CloseOutline } from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import type { VCardProps } from './types'

defineOptions({
  name: 'VCard'
})

const props = withDefaults(defineProps<VCardProps>(), {
  theme: undefined,
  width: 280,
  minWidth: undefined,
  maxWidth: undefined,
  height: undefined,
  padding: '14px 16px 12px',
  radius: 16,
  gap: 12,
  bordered: false,
  closable: false,
  title: '',
  closeAriaLabel: ''
})

const emit = defineEmits<{
  close: [ev: MouseEvent]
}>()

const slots = useSlots()
const { t } = useI18n()
const appStore = useAppStore()

const currentTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const hasHeader = computed<boolean>(() => {
  return Boolean(
    props.title ||
    props.closable ||
    slots['header-left'] ||
    slots['header-center'] ||
    slots['header-right']
  )
})

const cardStyle = computed(() => {
  const style: Record<string, string> = {}
  if (props.width !== undefined && props.width !== null) {
    style.width = typeof props.width === 'number' ? `${props.width}px` : props.width
  }
  if (props.minWidth !== undefined && props.minWidth !== null) {
    style.minWidth = typeof props.minWidth === 'number' ? `${props.minWidth}px` : props.minWidth
  }
  if (props.maxWidth !== undefined && props.maxWidth !== null) {
    style.maxWidth = typeof props.maxWidth === 'number' ? `${props.maxWidth}px` : props.maxWidth
  }
  if (props.height !== undefined && props.height !== null) {
    style.height = typeof props.height === 'number' ? `${props.height}px` : props.height
  }
  if (props.padding !== undefined && props.padding !== null) {
    style.padding = props.padding
  }
  if (props.radius !== undefined && props.radius !== null) {
    style.borderRadius = typeof props.radius === 'number' ? `${props.radius}px` : props.radius
  }
  if (props.gap !== undefined && props.gap !== null) {
    style.gap = typeof props.gap === 'number' ? `${props.gap}px` : props.gap
  }
  return style
})

function handleClose(ev: MouseEvent) {
  emit('close', ev)
}
</script>

<style scoped lang="scss">
:global(.n-popover:has(.v-card)) {
  box-shadow: none !important;
  background-color: transparent !important;
}

.v-card {
  width: 280px;
  background-color: #fcfcfc;
  border: none;
  border-radius: 16px;
  box-sizing: border-box;
  padding: 14px 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  user-select: none;
  box-shadow: 0 4px 20px rgba(60, 64, 67, 0.15), 0 1px 4px rgba(60, 64, 67, 0.1);

  &.is-bordered {
    border: 1px solid #d7d8da80;
  }

  .card-top-layer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 28px;
    flex-shrink: 0;
    position: relative;

    .card-top-left,
    .card-title-group {
      display: flex;
      align-items: center;
      gap: 6px;
      min-width: 0;

      .card-title {
        font-size: 14px;
        font-weight: 600;
        color: #202124;
        line-height: 1.3;
      }
    }

    .card-top-center {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .card-top-right,
    .card-actions-group {
      display: flex;
      align-items: center;
      gap: 4px;
      flex-shrink: 0;
    }

    .card-close-btn {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      border: none;
      background: transparent;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: #5f6368;
      transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
                  color 0.2s cubic-bezier(0.2, 0, 0, 1);
      outline: none;
      padding: 0;
      flex-shrink: 0;

      &:hover {
        background-color: rgba(60, 64, 67, 0.08);
        color: #202124;
      }

      &:active {
        background-color: rgba(60, 64, 67, 0.12);
      }
    }
  }

  .card-bottom-layer {
    display: flex;
    align-items: center;
    width: 100%;
    flex-shrink: 0;
  }

  &.dark,
  html.dark & {
    background-color: #191919;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.6), 0 1px 4px rgba(0, 0, 0, 0.4);

    &.is-bordered {
      border: 1px solid #3c4043;
    }

    .card-top-layer {
      .card-top-left,
      .card-title-group {
        .card-title {
          color: #e8eaed;
        }
      }

      .card-close-btn {
        color: #9aa0a6;

        &:hover {
          background-color: rgba(255, 255, 255, 0.08);
          color: #e8eaed;
        }

        &:active {
          background-color: rgba(255, 255, 255, 0.12);
        }
      }
    }
  }
}
</style>
