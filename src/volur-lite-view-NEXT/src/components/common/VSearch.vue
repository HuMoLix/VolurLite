<template>
  <VInput
    ref="inputInstRef"
    v-model:value="innerValue"
    :placeholder="computedPlaceholder"
    :clearable="clearable"
    :disabled="disabled"
    :size="size"
    :round="round"
    :radius="radius"
    :autofocus="autoFocus"
    :theme="theme"
    class="v-search"
    @keydown.enter="handleEnter"
    @clear="handleClear"
    @focus="(ev: FocusEvent) => emit('focus', ev)"
    @blur="(ev: FocusEvent) => emit('blur', ev)"
    v-bind="$attrs"
  >
    <template #prefix>
      <slot name="prefix">
        <n-icon
          :component="SearchOutline"
          class="v-search-prefix-icon cursor-pointer"
          @click.stop="handleSearchClick"
        />
      </slot>
    </template>

    <template v-if="$slots.suffix" #suffix>
      <slot name="suffix" />
    </template>
  </VInput>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { SearchOutline } from '@vicons/ionicons5'
import VInput from './VInput.vue'
import type { VSearchProps } from './types'

defineOptions({
  name: 'VSearch'
})

const props = withDefaults(defineProps<VSearchProps>(), {
  modelValue: undefined,
  value: undefined,
  placeholder: '',
  clearable: true,
  disabled: false,
  size: 'medium',
  round: false,
  radius: undefined,
  theme: undefined,
  autoFocus: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'update:value', val: string): void
  (e: 'search', val: string): void
  (e: 'clear'): void
  (e: 'focus', ev: FocusEvent): void
  (e: 'blur', ev: FocusEvent): void
}>()

const { t } = useI18n()
const inputInstRef = ref<InstanceType<typeof VInput> | null>(null)

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

const computedPlaceholder = computed<string>(() => {
  if (props.placeholder) {
    return props.placeholder
  }
  return t('common.search', '搜索...')
})

function handleEnter() {
  if (props.disabled) return
  emit('search', innerValue.value.trim())
}

function handleSearchClick() {
  if (props.disabled) return
  emit('search', innerValue.value.trim())
  inputInstRef.value?.focus()
}

function handleClear() {
  emit('clear')
  emit('search', '')
}

defineExpose({
  focus: () => inputInstRef.value?.focus(),
  blur: () => inputInstRef.value?.blur(),
  select: () => inputInstRef.value?.select(),
  clear: handleClear
})
</script>

<style scoped lang="scss">
.v-search {
  .v-search-prefix-icon {
    font-size: 18px;
    color: inherit;
    cursor: pointer;
    transition: color 0.15s ease-in-out;

    &:hover {
      color: #1b1b1e;
    }
  }

  &.n-input--small {
    .v-search-prefix-icon {
      font-size: 15px;
    }
  }

  &.n-input--large {
    .v-search-prefix-icon {
      font-size: 20px;
    }
  }

  &.n-input--disabled {
    .v-search-prefix-icon {
      cursor: not-allowed !important;
      pointer-events: none;
    }
  }

  &.theme-dark,
  html.dark & {
    .v-search-prefix-icon:hover {
      color: #f5f5f5;
    }

    &.n-input--disabled {
      .v-search-prefix-icon {
        cursor: not-allowed !important;
      }
    }
  }
}
</style>
