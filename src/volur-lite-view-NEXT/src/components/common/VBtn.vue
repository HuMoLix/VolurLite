<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    class="v-btn"
    :class="[
      `v-btn-${clz}`,
      `size-${size}`,
      {
        'is-round': round === true,
        'w-full flex': block
      }
    ]"
    :style="customStyle"
    @click="handleClick"
    v-bind="$attrs"
  >
    <template v-if="loading">
      <n-icon :size="computedIconSize" class="animate-spin flex-shrink-0" :component="RefreshOutline" />
    </template>
    <template v-else>
      <slot name="left-icon">
        <n-icon v-if="leftIcon" :size="computedIconSize" :component="leftIcon" class="flex-shrink-0" />
      </slot>
    </template>

    <span v-if="$slots.default || text" class="inline-flex items-center">
      <slot>{{ text }}</slot>
    </span>

    <slot name="right-icon">
      <n-icon v-if="rightIcon && !loading" :size="computedIconSize" :component="rightIcon" class="flex-shrink-0" />
    </slot>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CSSProperties } from 'vue'
import { NIcon } from 'naive-ui'
import { RefreshOutline } from '@vicons/ionicons5'
import type { VBtnProps } from './types'

defineOptions({
  name: 'VBtn'
})

const props = withDefaults(defineProps<VBtnProps>(), {
  clz: 'primary',
  leftIcon: undefined,
  rightIcon: undefined,
  iconSize: undefined,
  size: 'medium',
  round: undefined,
  radius: undefined,
  disabled: false,
  loading: false,
  type: 'button',
  block: false,
  text: ''
})

const emit = defineEmits<{
  (e: 'click', ev: MouseEvent): void
}>()

const customStyle = computed<CSSProperties>(() => {
  if (props.radius !== undefined && props.radius !== null && props.radius !== '') {
    return {
      borderRadius: typeof props.radius === 'number' ? `${props.radius}px` : String(props.radius)
    }
  }
  return {}
})

const computedIconSize = computed<number>(() => {
  if (props.iconSize) {
    return props.iconSize
  }
  if (props.size === 'tiny') {
    return 12
  }
  if (props.size === 'small') {
    return 14
  }
  if (props.size === 'large') {
    return 18
  }
  return 16
})

function handleClick(ev: MouseEvent) {
  if (props.disabled || props.loading) {
    ev.preventDefault()
    ev.stopPropagation()
    return
  }
  emit('click', ev)
}
</script>

<style scoped lang="scss">
.v-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  font-family: Inter, sans-serif;
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  letter-spacing: normal;
  border-style: solid;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
  outline: none;
  box-shadow: none;
  transition: background-color 0.15s ease-in-out, border-color 0.15s ease-in-out, color 0.15s ease-in-out, box-shadow 0.1s ease-in-out;

  &:focus,
  &:active,
  &:focus-visible {
    outline: none;
  }

  &.v-btn-normal {
    height: 36px;
    padding: 0 12px;
    border-radius: 12px;
    border: 1px solid #d7d8da;
    color: #2b2d31;
    background-color: transparent;
    gap: 6px;

    &:hover:not(:disabled) {
      background-color: #EAEAEB;
    }

    &:focus,
    &:active {
      border-color: #d7d8da;
    }

    html.dark & {
      border-color: #333;
      color: #fcfcfc;

      &:hover:not(:disabled) {
        background-color: #2a2b2d;
      }

      &:focus,
      &:active {
        border-color: #333;
      }
    }
  }

  &.v-btn-primary {
    height: 36px;
    padding: 0 16px;
    border-radius: 9999px;
    border: none;
    background-color: #1a73e8;
    color: #ffffff;
    gap: 8px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

    &:hover:not(:disabled) {
      background-color: #1765cc;
    }

    &:active:not(:disabled) {
      background-color: #1557b0;
    }

    html.dark & {
      background-color: #a8c7fa;
      color: #1e1f20;

      &:hover:not(:disabled) {
        background-color: #b6d0f8;
      }
    }
  }

  &.v-btn-warning,
  &.v-btn-danger {
    height: 36px;
    padding: 0 12px;
    border-radius: 12px;
    border: 1px solid #ea43354d;
    color: #ea4335;
    background-color: #ea43350d;
    gap: 6px;

    &:hover:not(:disabled) {
      background-color: rgba(234, 67, 53, 0.1);
      border-color: rgba(234, 67, 53, 0.45);
    }

    &:focus,
    &:active {
      border-color: rgba(234, 67, 53, 0.45);
    }

    html.dark & {
      border-color: rgba(242, 139, 130, 0.3);
      color: #f28b82;
      background-color: rgba(242, 139, 130, 0.08);

      &:hover:not(:disabled) {
        background-color: rgba(242, 139, 130, 0.16);
        border-color: rgba(242, 139, 130, 0.45);
      }

      &:focus,
      &:active {
        border-color: rgba(242, 139, 130, 0.45);
      }
    }
  }

  &.size-tiny {
    height: 24px;
    padding: 0 6px;
    font-size: 11px;
    gap: 3px;
    border-radius: 6px;
  }

  &.size-small {
    height: 28px;
    padding: 0 8px;
    font-size: 12px;
    gap: 4px;
    border-radius: 8px;
  }

  &.size-medium {
    height: 36px;
    font-size: 14px;
  }

  &.size-large {
    height: 44px;
    padding: 0 24px;
    font-size: 15px;
    gap: 8px;
  }

  &.is-round {
    border-radius: 9999px !important;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }
}
</style>
