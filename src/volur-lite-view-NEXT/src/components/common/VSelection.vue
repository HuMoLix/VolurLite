<template>
  <n-select
    ref="selectInstRef"
    v-model:value="innerValue"
    :options="options"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    :size="size"
    :multiple="multiple"
    :filterable="filterable"
    :max-tag-count="maxTagCount"
    :consistent-menu-width="consistentMenuWidth"
    :placement="placement"
    :loading="loading"
    :to="resolvedTo"
    :menu-props="resolvedMenuProps"
    :theme-overrides="selectThemeOverrides"
    :style="{ '--v-selection-radius': resolvedRadius }"
    class="v-selection"
    :class="[effectiveTheme, { 'theme-dark': effectiveTheme === 'dark' }]"
    @focus="(ev: FocusEvent) => emit('focus', ev)"
    @blur="(ev: FocusEvent) => emit('blur', ev)"
    @clear="handleClear"
    @update:show="(show: boolean) => emit('update:show', show)"
    v-bind="$attrs"
  >
    <template v-if="$slots.arrow" #arrow>
      <slot name="arrow" />
    </template>
    <template v-if="$slots.empty" #empty>
      <slot name="empty" />
    </template>
    <template v-if="$slots.action" #action>
      <slot name="action" />
    </template>
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>
  </n-select>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NSelect, type SelectInst } from 'naive-ui'
import { useAppStore } from '@/store/modules/app'
import type { VSelectionProps, SelectValue } from './types'

defineOptions({
  name: 'VSelection'
})

const props = withDefaults(defineProps<VSelectionProps>(), {
  modelValue: undefined,
  value: undefined,
  options: () => [],
  placeholder: '',
  clearable: false,
  disabled: false,
  size: 'medium',
  round: false,
  radius: undefined,
  theme: undefined,
  multiple: false,
  filterable: false,
  maxTagCount: undefined,
  consistentMenuWidth: true,
  placement: 'bottom-start',
  loading: false,
  to: undefined,
  zIndex: undefined,
  menuProps: undefined
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: SelectValue): void
  (e: 'update:value', val: SelectValue): void
  (e: 'clear'): void
  (e: 'focus', ev: FocusEvent): void
  (e: 'blur', ev: FocusEvent): void
  (e: 'update:show', show: boolean): void
}>()

const appStore = useAppStore()
const selectInstRef = ref<SelectInst | null>(null)

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
  return '8px'
})

const resolvedTo = computed(() => {
  if (props.to !== undefined) {
    return props.to
  }
  if (props.zIndex !== undefined) {
    return 'body'
  }
  return undefined
})

const resolvedMenuProps = computed(() => {
  if (props.zIndex !== undefined) {
    return {
      ...(props.menuProps || {}),
      style: {
        zIndex: props.zIndex,
        ...((props.menuProps && props.menuProps.style) || {})
      }
    }
  }
  return props.menuProps
})

const innerValue = computed<SelectValue>({
  get() {
    if (props.value !== undefined) {
      return props.value
    }
    if (props.modelValue !== undefined) {
      return props.modelValue
    }
    return null
  },
  set(val: SelectValue) {
    emit('update:value', val)
    emit('update:modelValue', val)
  }
})

const selectThemeOverrides = computed(() => {
  const isDark = effectiveTheme.value === 'dark'
  return {
    peers: {
      InternalSelection: {
        borderRadius: resolvedRadius.value,
        border: isDark ? '1px solid #262626' : '1px solid #d7d8da80',
        borderHover: isDark ? '1px solid #262626' : '1px solid #d7d8da80',
        borderActive: isDark ? '1px solid #333' : '1px solid #b7babd',
        borderFocus: isDark ? '1px solid #333' : '1px solid #b7babd',
        borderDisabled: isDark ? '1px solid #262626' : '1px solid #d7d8da80',
        boxShadowHover: 'none',
        boxShadowActive: 'none',
        boxShadowFocus: 'none',
        color: 'transparent',
        colorActive: 'transparent',
        colorDisabled: isDark ? 'rgba(255, 255, 255, 0.04)' : '#f5f6f7',
        textColor: isDark ? '#d4d4d4' : '#1b1b1e',
        textColorDisabled: isDark ? '#73777d' : '#80868b',
        placeholderColor: isDark ? '#80868b' : '#9aa0a6',
        placeholderColorDisabled: isDark ? '#55595d' : '#bdc1c6',
        caretColor: isDark ? '#d4d4d4' : '#2b2d31',
        arrowColor: isDark ? '#8c8c8c' : '#565b61',
        arrowColorDisabled: isDark ? '#55595d' : '#9aa0a6',
        fontSizeMedium: '14px',
        fontSizeSmall: '13px',
        fontSizeLarge: '14px',
        heightSmall: '28px',
        heightMedium: '36px',
        heightLarge: '44px'
      },
      InternalSelectMenu: {
        borderRadius: '10px',
        color: isDark ? '#292a2d' : '#ffffff',
        optionTextColor: isDark ? '#e8eaed' : '#202124',
        optionTextColorActive: isDark ? '#8ab4f8' : '#1a73e8',
        optionColorPending: isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(60, 64, 67, 0.08)',
        optionColorActive: isDark ? 'rgba(138, 180, 248, 0.12)' : 'rgba(26, 115, 232, 0.08)',
        optionColorActivePending: isDark ? 'rgba(138, 180, 248, 0.16)' : 'rgba(26, 115, 232, 0.12)',
        optionCheckColor: isDark ? '#8ab4f8' : '#1a73e8'
      }
    }
  }
})

function handleClear() {
  emit('clear')
}

defineExpose({
  focus: () => selectInstRef.value?.focus(),
  blur: () => selectInstRef.value?.blur()
})
</script>

<style scoped lang="scss">
.v-selection {
  border-radius: var(--v-selection-radius, 8px) !important;
  font-size: 14px;
  font-weight: 400;
  font-family: Inter, sans-serif;
  font-optical-sizing: auto;
  color: #1b1b1e;
  background-color: transparent;

  :deep(.n-base-selection) {
    border-radius: var(--v-selection-radius, 8px) !important;
    background-color: transparent;
    transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  }

  :deep(.n-base-selection-label) {
    border-radius: var(--v-selection-radius, 8px) !important;
    background-color: transparent;
    font-size: 14px;
    font-weight: 400;
    line-height: 21px;
    font-family: Inter, sans-serif;
    font-optical-sizing: auto;
    color: #1b1b1e;
  }

  :deep(.n-base-selection-input) {
    font-size: 14px;
    font-weight: 400;
    line-height: 21px;
    font-family: Inter, sans-serif;
    font-optical-sizing: auto;
    color: #1b1b1e;
    caret-color: #2b2d31 !important;
  }

  :deep(.n-base-selection-placeholder) {
    font-size: 14px;
    font-weight: 400;
    line-height: 21px;
    font-family: Inter, sans-serif;
    font-optical-sizing: auto;
    color: #9aa0a6;

    span {
      font-size: 14px;
      font-weight: 400;
      line-height: 21px;
      font-family: Inter, sans-serif;
      font-optical-sizing: auto;
    }
  }

  :deep(.n-base-suffix__arrow) {
    color: #565b61;
    transition: color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  }

  :deep(.n-base-selection__border) {
    border-color: #d7d8da80 !important;
    border-radius: var(--v-selection-radius, 8px) !important;
    transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  }

  :deep(.n-base-selection__state-border) {
    border-radius: var(--v-selection-radius, 8px) !important;
    box-shadow: none !important;
    transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &:hover:not(.n-select--disabled) {
    :deep(.n-base-selection__border) {
      border-color: #d7d8da80 !important;
    }
    :deep(.n-base-selection__state-border) {
      border-color: #d7d8da80 !important;
      box-shadow: none !important;
    }
  }

  &.n-select--focus:not(.n-select--disabled),
  &:focus-within:not(.n-select--disabled),
  :deep(.n-base-selection--focus:not(.n-base-selection--disabled)),
  :deep(.n-base-selection--active:not(.n-base-selection--disabled)) {
    :deep(.n-base-selection__border) {
      border-color: #b7babd !important;
    }
    :deep(.n-base-selection__state-border) {
      border-color: #b7babd !important;
      box-shadow: none !important;
    }
  }

  &.n-select--disabled,
  :deep(.n-base-selection--disabled) {
    cursor: not-allowed !important;
    background-color: #f5f6f7 !important;

    :deep(.n-base-selection) {
      background-color: #f5f6f7 !important;
    }

    :deep(.n-base-selection-label) {
      cursor: not-allowed !important;
      background-color: transparent !important;
      color: #80868b !important;
      -webkit-text-fill-color: #80868b !important;
    }

    :deep(.n-base-selection-placeholder) {
      color: #bdc1c6 !important;

      span {
        color: #bdc1c6 !important;
      }
    }

    :deep(.n-base-selection__border) {
      border-color: #d7d8da80 !important;
    }

    :deep(.n-base-selection__state-border) {
      display: none !important;
    }

    :deep(.n-base-suffix__arrow) {
      cursor: not-allowed !important;
      color: #9aa0a6 !important;
    }
  }

  &.n-select--small,
  :deep(.n-base-selection--small) {
    font-size: 13px;

    :deep(.n-base-selection-label) {
      font-size: 13px;
      line-height: 20px;
    }

    :deep(.n-base-selection-placeholder) {
      font-size: 13px;
      line-height: 20px;

      span {
        font-size: 13px;
        line-height: 20px;
      }
    }
  }

  &.theme-dark,
  html.dark & {
    color: #d4d4d4;

    :deep(.n-base-selection-label) {
      color: #d4d4d4;
    }

    :deep(.n-base-selection-input) {
      color: #d4d4d4;
      caret-color: #d4d4d4 !important;
    }

    :deep(.n-base-selection-placeholder) {
      color: #80868b;
    }

    :deep(.n-base-suffix__arrow) {
      color: #8c8c8c;
    }

    :deep(.n-base-selection__border) {
      border-color: #262626 !important;
    }

    &:hover:not(.n-select--disabled) {
      :deep(.n-base-selection__border) {
        border-color: #262626 !important;
      }
      :deep(.n-base-selection__state-border) {
        border-color: #262626 !important;
        box-shadow: none !important;
      }
    }

    &.n-select--focus:not(.n-select--disabled),
    &:focus-within:not(.n-select--disabled),
    :deep(.n-base-selection--focus:not(.n-base-selection--disabled)),
    :deep(.n-base-selection--active:not(.n-base-selection--disabled)) {
      :deep(.n-base-selection__border) {
        border-color: #333 !important;
      }
      :deep(.n-base-selection__state-border) {
        border-color: #333 !important;
        box-shadow: none !important;
      }
    }

    &.n-select--disabled,
    :deep(.n-base-selection--disabled) {
      background-color: rgba(255, 255, 255, 0.04) !important;

      :deep(.n-base-selection) {
        background-color: rgba(255, 255, 255, 0.04) !important;
      }

      :deep(.n-base-selection-label) {
        color: #73777d !important;
        -webkit-text-fill-color: #73777d !important;
      }

      :deep(.n-base-selection-placeholder) {
        color: #55595d !important;

        span {
          color: #55595d !important;
        }
      }

      :deep(.n-base-selection__border) {
        border-color: #262626 !important;
      }

      :deep(.n-base-suffix__arrow) {
        color: #55595d !important;
      }
    }
  }
}
</style>
