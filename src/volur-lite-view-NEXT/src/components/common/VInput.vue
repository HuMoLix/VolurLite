<template>
  <n-input
    ref="inputInstRef"
    v-model:value="innerValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    :size="size"
    :round="round"
    :autofocus="autoFocus"
    :type="type"
    :maxlength="maxlength"
    :show-count="showCount"
    :readonly="readonly"
    :autosize="autosize"
    :rows="rows"
    :theme-overrides="inputThemeOverrides"
    :style="{ '--v-input-radius': resolvedRadius }"
    class="v-input"
    :class="{ 'theme-dark': effectiveTheme === 'dark' }"
    @keydown="(ev) => emit('keydown', ev)"
    @keyup="(ev) => emit('keyup', ev)"
    @clear="handleClear"
    @focus="(ev: FocusEvent) => emit('focus', ev)"
    @blur="(ev: FocusEvent) => emit('blur', ev)"
    @change="(val: string) => emit('change', val)"
  >
    <template v-if="$slots.prefix" #prefix>
      <slot name="prefix" />
    </template>

    <template v-if="$slots.suffix" #suffix>
      <slot name="suffix" />
    </template>

    <template v-if="$slots['clear-icon']" #clear-icon>
      <slot name="clear-icon" />
    </template>
  </n-input>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NInput } from 'naive-ui'
import { useAppStore } from '@/store/modules/app'
import type { VInputProps } from './types'

defineOptions({
  name: 'VInput'
})

const props = withDefaults(defineProps<VInputProps>(), {
  modelValue: undefined,
  value: undefined,
  placeholder: '',
  clearable: false,
  disabled: false,
  size: 'medium',
  round: false,
  radius: undefined,
  theme: undefined,
  autoFocus: false,
  type: 'text',
  maxlength: undefined,
  showCount: false,
  readonly: false,
  autosize: undefined,
  rows: undefined
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'update:value', val: string): void
  (e: 'clear'): void
  (e: 'focus', ev: FocusEvent): void
  (e: 'blur', ev: FocusEvent): void
  (e: 'change', val: string): void
  (e: 'keydown', ev: KeyboardEvent): void
  (e: 'keyup', ev: KeyboardEvent): void
}>()

const appStore = useAppStore()
const inputInstRef = ref<InstanceType<typeof NInput> | null>(null)

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

const innerValue = computed<string>({
  get() {
    if (props.value !== undefined && props.value !== null) {
      return props.value
    }
    if (props.modelValue !== undefined && props.modelValue !== null) {
      return props.modelValue
    }
    return ''
  },
  set(val: string) {
    emit('update:value', val)
    emit('update:modelValue', val)
  }
})

const inputThemeOverrides = computed(() => {
  const isDark = effectiveTheme.value === 'dark'
  return {
    borderRadius: resolvedRadius.value,
    border: isDark ? '1px solid #262626' : '1px solid #d7d8da80',
    borderHover: isDark ? '1px solid #262626' : '1px solid #d7d8da80',
    borderFocus: isDark ? '1px solid #333' : '1px solid #b7babd',
    borderDisabled: isDark ? '1px solid #262626' : '1px solid #d7d8da80',
    colorDisabled: isDark ? 'rgba(255, 255, 255, 0.04)' : '#f5f6f7',
    textColorDisabled: isDark ? '#73777d' : '#80868b',
    placeholderColorDisabled: isDark ? '#55595d' : '#bdc1c6',
    iconColorDisabled: isDark ? '#55595d' : '#9aa0a6',
    countTextColorDisabled: isDark ? '#55595d' : '#9aa0a6',
    boxShadowFocus: 'none',
    textColor: isDark ? '#d4d4d4' : '#1b1b1e',
    caretColor: isDark ? '#d4d4d4' : '#2b2d31',
    fontSizeMedium: '14px',
    fontSizeSmall: '13px',
    fontSizeLarge: '14px',
    heightSmall: '28px',
    heightMedium: '36px',
    heightLarge: '44px',
    lineHeight: '21px',
    placeholderColor: isDark ? '#80868b' : '#9aa0a6'
  }
})

function handleClear() {
  emit('clear')
}

defineExpose({
  focus: () => inputInstRef.value?.focus(),
  blur: () => inputInstRef.value?.blur(),
  select: () => inputInstRef.value?.select(),
  clear: () => {
    innerValue.value = ''
    handleClear()
  }
})
</script>

<style scoped lang="scss">
.v-input {
  --n-caret-color: #2b2d31 !important;
  border-radius: var(--v-input-radius, 8px) !important;
  font-size: 14px;
  font-weight: 400;
  line-height: 21px;
  font-family: Inter, sans-serif;
  font-optical-sizing: auto;
  color: #1b1b1e;
  background-color: transparent;

  :deep(.n-input-wrapper) {
    border-radius: var(--v-input-radius, 8px) !important;
  }

  :deep(.n-input__border) {
    border-color: #d7d8da80 !important;
    border-radius: var(--v-input-radius, 8px) !important;
    transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  }

  :deep(.n-input__state-border) {
    border-radius: var(--v-input-radius, 8px) !important;
    box-shadow: none !important;
    transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  }

  :deep(.n-input__input-el) {
    font-size: 14px;
    font-weight: 400;
    line-height: inherit;
    font-family: Inter, sans-serif;
    font-optical-sizing: auto;
    color: #1b1b1e;
    caret-color: #2b2d31 !important;

    &::placeholder {
      font-size: 14px;
      font-weight: 400;
      line-height: inherit;
      font-family: Inter, sans-serif;
      font-optical-sizing: auto;
      color: #9aa0a6;
    }
  }

  :deep(.n-input__textarea-el) {
    font-size: 14px;
    font-weight: 400;
    line-height: 21px;
    font-family: Inter, sans-serif;
    font-optical-sizing: auto;
    color: #1b1b1e;
    caret-color: #2b2d31 !important;

    &::placeholder {
      font-size: 14px;
      font-weight: 400;
      line-height: 21px;
      font-family: Inter, sans-serif;
      font-optical-sizing: auto;
      color: #9aa0a6;
    }
  }

  :deep(.n-input__placeholder) {
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

  :deep(.n-input__prefix) {
    margin-right: 8px;
    color: #565b61;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  &:hover:not(.n-input--disabled) {
    :deep(.n-input__border) {
      border-color: #d7d8da80 !important;
    }
    :deep(.n-input__state-border) {
      border-color: #d7d8da80 !important;
      box-shadow: none !important;
    }
  }

  &.n-input--focus:not(.n-input--disabled),
  &:focus-within:not(.n-input--disabled) {
    :deep(.n-input__border) {
      border-color: #b7babd !important;
    }
    :deep(.n-input__state-border) {
      border-color: #b7babd !important;
      box-shadow: none !important;
    }
  }

  &.n-input--disabled {
    cursor: not-allowed !important;
    background-color: #f5f6f7 !important;

    :deep(.n-input-wrapper) {
      cursor: not-allowed !important;
      background-color: transparent !important;
    }

    :deep(.n-input__border) {
      border-color: #d7d8da80 !important;
    }

    :deep(.n-input__state-border) {
      display: none !important;
    }

    :deep(.n-input__input-el),
    :deep(.n-input__textarea-el) {
      cursor: not-allowed !important;
      color: #80868b !important;
      -webkit-text-fill-color: #80868b !important;

      &::placeholder {
        color: #bdc1c6 !important;
        -webkit-text-fill-color: #bdc1c6 !important;
      }
    }

    :deep(.n-input__placeholder) {
      color: #bdc1c6 !important;

      span {
        color: #bdc1c6 !important;
      }
    }

    :deep(.n-input__prefix),
    :deep(.n-input__suffix) {
      cursor: not-allowed !important;
      color: #9aa0a6 !important;
      pointer-events: none;
    }

    :deep(.n-input-word-count) {
      color: #bdc1c6 !important;
    }
  }

  &.n-input--small,
  :deep(.n-input--small) {
    font-size: 13px;

    :deep(.n-input__input-el) {
      font-size: 13px;
    }

    :deep(.n-input__placeholder) {
      font-size: 13px;

      span {
        font-size: 13px;
      }
    }
  }

  &.theme-dark,
  html.dark & {
    --n-caret-color: #d4d4d4 !important;
    color: #d4d4d4;

    :deep(.n-input__border) {
      border-color: #262626 !important;
    }

    :deep(.n-input__input-el),
    :deep(.n-input__textarea-el) {
      color: #d4d4d4;
      caret-color: #d4d4d4 !important;

      &::placeholder {
        color: #80868b;
      }
    }

    :deep(.n-input__placeholder) {
      color: #80868b;
    }

    :deep(.n-input__prefix) {
      color: #8c8c8c;
    }

    &:hover:not(.n-input--disabled) {
      :deep(.n-input__border) {
        border-color: #262626 !important;
      }
      :deep(.n-input__state-border) {
        border-color: #262626 !important;
        box-shadow: none !important;
      }
    }

    &.n-input--focus:not(.n-input--disabled),
    &:focus-within:not(.n-input--disabled) {
      :deep(.n-input__border) {
        border-color: #333 !important;
      }
      :deep(.n-input__state-border) {
        border-color: #333 !important;
        box-shadow: none !important;
      }
    }

    &.n-input--disabled {
      background-color: rgba(255, 255, 255, 0.04) !important;

      :deep(.n-input__border) {
        border-color: #262626 !important;
      }

      :deep(.n-input__input-el),
      :deep(.n-input__textarea-el) {
        color: #73777d !important;
        -webkit-text-fill-color: #73777d !important;

        &::placeholder {
          color: #55595d !important;
          -webkit-text-fill-color: #55595d !important;
        }
      }

      :deep(.n-input__placeholder) {
        color: #55595d !important;

        span {
          color: #55595d !important;
        }
      }

      :deep(.n-input__prefix),
      :deep(.n-input__suffix) {
        color: #55595d !important;
      }

      :deep(.n-input-word-count) {
        color: #55595d !important;
      }
    }
  }
}
</style>
