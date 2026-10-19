<template>
  <button
    type="button"
    role="checkbox"
    :aria-checked="indeterminate ? 'mixed' : innerChecked"
    :disabled="disabled"
    class="v-select-box"
    :class="[
      `size-${size}`,
      effectiveTheme,
      {
        'is-checked': innerChecked,
        'is-disabled': disabled,
        'is-indeterminate': indeterminate,
        'is-round': round,
        'theme-dark': effectiveTheme === 'dark'
      }
    ]"
    :style="{ '--v-select-box-radius': resolvedRadius }"
    @click="handleClick"
    @keydown.space.prevent="handleClick"
    v-bind="$attrs"
  >
    <span class="v-select-box__inner">
      <n-icon v-if="innerChecked" :size="computedIconSize" :component="CheckmarkOutline" class="v-select-box__icon" />
      <span v-else-if="indeterminate" class="v-select-box__indeterminate" />
    </span>
    <span v-if="$slots.default || label" class="v-select-box__label">
      <slot>{{ label }}</slot>
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NIcon } from 'naive-ui'
import { CheckmarkOutline } from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import type { VSelectBoxProps } from './types'

defineOptions({
  name: 'VSelectBox'
})

const props = withDefaults(defineProps<VSelectBoxProps>(), {
  modelValue: undefined,
  checked: undefined,
  disabled: false,
  indeterminate: false,
  size: 'medium',
  round: false,
  radius: undefined,
  theme: undefined,
  label: ''
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'update:checked', val: boolean): void
  (e: 'change', val: boolean): void
  (e: 'click', ev: MouseEvent): void
}>()

const appStore = useAppStore()

const effectiveTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const resolvedRadius = computed<string>(() => {
  if (props.round) {
    return '9999px'
  }
  if (props.radius !== undefined && props.radius !== null && props.radius !== '') {
    return typeof props.radius === 'number' ? `${props.radius}px` : String(props.radius)
  }
  if (props.size === 'small') {
    return '4px'
  }
  return '6px'
})

const innerChecked = computed<boolean>(() => {
  if (props.checked !== undefined && props.checked !== null) {
    return !!props.checked
  }
  if (props.modelValue !== undefined && props.modelValue !== null) {
    return !!props.modelValue
  }
  return false
})

const computedIconSize = computed<number>(() => {
  if (props.size === 'small') return 12
  if (props.size === 'large') return 16
  return 14
})

function handleClick(ev: MouseEvent | KeyboardEvent) {
  if (props.disabled) return
  const nextVal = !innerChecked.value
  emit('update:modelValue', nextVal)
  emit('update:checked', nextVal)
  emit('change', nextVal)
  if (ev instanceof MouseEvent) {
    emit('click', ev)
  }
}
</script>

<style scoped lang="scss">
.v-select-box {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
  background: transparent;
  border: none;
  padding: 0;
  margin: 0;
  outline: none;
  font-family: inherit;

  .v-select-box__inner {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--v-select-box-radius, 6px);
    border: 1px solid #d7d8da80;
    background-color: transparent;
    transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1),
                background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1),
                box-shadow 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    flex-shrink: 0;
    box-sizing: border-box;
  }

  .v-select-box__label {
    font-size: 14px;
    font-weight: 400;
    line-height: 20px;
    color: #1b1b1e;
    transition: color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .v-select-box__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    stroke-width: 2.5px;
  }

  .v-select-box__indeterminate {
    width: 60%;
    height: 2px;
    background-color: #ffffff;
    border-radius: 1px;
  }

  &:hover:not(.is-disabled) {
    .v-select-box__inner {
      border-color: #b7babd;
    }
  }

  &:focus-visible:not(.is-disabled) {
    .v-select-box__inner {
      border-color: #b7babd;
      box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.2);
    }
  }

  &.is-checked,
  &.is-indeterminate {
    .v-select-box__inner {
      background-color: #1a73e8;
      border-color: #1a73e8;
      color: #ffffff;
    }

    &:hover:not(.is-disabled) {
      .v-select-box__inner {
        background-color: #1765cc;
        border-color: #1765cc;
      }
    }
  }

  &.is-disabled {
    cursor: not-allowed;

    .v-select-box__inner {
      background-color: #f5f6f7;
      border-color: #d7d8da80;
      color: #bdc1c6;
    }

    &.is-checked .v-select-box__inner,
    &.is-indeterminate .v-select-box__inner {
      background-color: #e8eaed;
      border-color: #d7d8da;
      color: #9aa0a6;
    }

    .v-select-box__label {
      color: #80868b;
      cursor: not-allowed;
    }
  }

  &.size-small {
    .v-select-box__inner {
      width: 16px;
      height: 16px;
    }
    .v-select-box__label {
      font-size: 13px;
    }
  }

  &.size-medium {
    .v-select-box__inner {
      width: 20px;
      height: 20px;
    }
    .v-select-box__label {
      font-size: 14px;
    }
  }

  &.size-large {
    .v-select-box__inner {
      width: 24px;
      height: 24px;
    }
    .v-select-box__label {
      font-size: 15px;
    }
  }

  &.is-round .v-select-box__inner {
    border-radius: 9999px !important;
  }

  &.theme-dark,
  html.dark & {
    .v-select-box__inner {
      border-color: #262626;
    }

    .v-select-box__label {
      color: #d4d4d4;
    }

    &:hover:not(.is-disabled) {
      .v-select-box__inner {
        border-color: #333;
      }
    }

    &:focus-visible:not(.is-disabled) {
      .v-select-box__inner {
        border-color: #333;
        box-shadow: 0 0 0 2px rgba(138, 180, 248, 0.2);
      }
    }

    &.is-checked,
    &.is-indeterminate {
      .v-select-box__inner {
        background-color: #8ab4f8;
        border-color: #8ab4f8;
        color: #202124;
      }

      .v-select-box__indeterminate {
        background-color: #202124;
      }

      &:hover:not(.is-disabled) {
        .v-select-box__inner {
          background-color: #93bbf9;
          border-color: #93bbf9;
        }
      }
    }

    &.is-disabled {
      .v-select-box__inner {
        background-color: rgba(255, 255, 255, 0.04);
        border-color: #262626;
        color: #55595d;
      }

      &.is-checked .v-select-box__inner,
      &.is-indeterminate .v-select-box__inner {
        background-color: rgba(255, 255, 255, 0.08);
        border-color: #262626;
        color: #55595d;
      }

      .v-select-box__label {
        color: #73777d;
      }
    }
  }
}
</style>
