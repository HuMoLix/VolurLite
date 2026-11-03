<template>
  <div
    class="v-date-picker"
    :class="[
      effectiveTheme,
      {
        'theme-dark': effectiveTheme === 'dark',
        'is-disabled': disabled
      }
    ]"
  >
    <n-popover
      v-model:show="showPopover"
      trigger="click"
      placement="bottom-start"
      :show-arrow="false"
      :disabled="disabled"
      raw
    >
      <template #trigger>
        <div v-if="isRange" class="v-date-picker-range flex items-center gap-2 w-full">
          <VInput
            :value="displayStart"
            :placeholder="startPlaceholder || '开始时间'"
            :size="size"
            :disabled="disabled"
            readonly
            class="flex-1 cursor-pointer"
          >
            <template #prefix>
              <n-icon :component="CalendarOutline" class="text-google-text-secondary dark:text-google-text-darkSecondary" />
            </template>
          </VInput>
          <span class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary select-none flex-shrink-0">
            {{ separator || '至' }}
          </span>
          <VInput
            :value="displayEnd"
            :placeholder="endPlaceholder || '结束时间'"
            :size="size"
            :disabled="disabled"
            readonly
            class="flex-1 cursor-pointer"
          >
            <template #prefix>
              <n-icon :component="CalendarOutline" class="text-google-text-secondary dark:text-google-text-darkSecondary" />
            </template>
          </VInput>
        </div>

        <VInput
          v-else
          :value="displaySingle"
          :placeholder="placeholder || '请选择时间'"
          :size="size"
          :disabled="disabled"
          readonly
          class="w-full cursor-pointer"
        >
          <template #prefix>
            <n-icon :component="CalendarOutline" class="text-google-text-secondary dark:text-google-text-darkSecondary" />
          </template>
        </VInput>
      </template>

      <div class="v-date-picker-card p-3 rounded-2xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark shadow-2xl">
        <n-date-picker
          panel
          :type="type"
          v-model:value="panelValue"
          :actions="null"
          :is-date-disabled="isDateDisabled"
          :theme-overrides="datePickerThemeOverrides"
          class="v-date-picker-panel"
        />

        <div class="v-date-picker-footer flex items-center justify-between pt-3 mt-2 border-t border-google-border-light dark:border-google-border-dark">
          <VBtn
            clz="normal"
            size="small"
            @click="handleClear"
          >
            清空
          </VBtn>
          <div class="flex items-center gap-2">
            <VBtn
              clz="normal"
              size="small"
              @click="handleNow"
            >
              现在
            </VBtn>
            <VBtn
              clz="primary"
              size="small"
              @click="handleConfirm"
            >
              确定
            </VBtn>
          </div>
        </div>
      </div>
    </n-popover>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { NDatePicker, NPopover, NIcon } from 'naive-ui'
import { CalendarOutline } from '@vicons/ionicons5'
import VInput from './VInput.vue'
import VBtn from './VBtn.vue'
import { useAppStore } from '@/store/modules/app'
import type { VDatePickerProps } from './types'

defineOptions({
  name: 'VDatePicker'
})

const props = withDefaults(defineProps<VDatePickerProps>(), {
  modelValue: undefined,
  value: undefined,
  startValue: undefined,
  endValue: undefined,
  formattedValue: undefined,
  type: 'datetime',
  placeholder: '',
  startPlaceholder: '',
  endPlaceholder: '',
  clearable: true,
  disabled: false,
  size: 'medium',
  round: false,
  radius: undefined,
  theme: undefined,
  format: undefined,
  valueFormat: undefined,
  separator: '至',
  isDateDisabled: undefined
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: any): void
  (e: 'update:value', val: any): void
  (e: 'update:start-value', val: string | number): void
  (e: 'update:end-value', val: string | number): void
  (e: 'change', val: any): void
  (e: 'clear'): void
  (e: 'confirm', val: any): void
}>()

const appStore = useAppStore()
const showPopover = ref(false)
const panelValue = ref<any>(null)

const isRange = computed(() => {
  return props.type === 'daterange' || props.type === 'datetimerange'
})

const effectiveTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

function getTimestamp(val: any): number | null {
  if (val === null || val === undefined || val === '') return null
  if (typeof val === 'number') return val
  const parsed = Date.parse(String(val))
  return isNaN(parsed) ? null : parsed
}

function toIsoInstantString(ts: number, type: string): string | number {
  if (props.valueFormat === 'timestamp') {
    return ts
  }
  const d = new Date(ts)
  if (type === 'date' || type === 'daterange') {
    const pad = (n: number) => String(n).padStart(2, '0')
    const y = d.getFullYear()
    const m = pad(d.getMonth() + 1)
    const day = pad(d.getDate())
    return `${y}-${m}-${day}`
  }
  return d.toISOString()
}

function formatDisplay(val: any, type: string): string {
  if (!val) return ''
  const ts = getTimestamp(val)
  if (!ts) return String(val)
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  const y = d.getFullYear()
  const m = pad(d.getMonth() + 1)
  const day = pad(d.getDate())
  if (type === 'date' || type === 'daterange') {
    return `${y}-${m}-${day}`
  }
  const h = pad(d.getHours())
  const min = pad(d.getMinutes())
  const s = pad(d.getSeconds())
  return `${y}-${m}-${day} ${h}:${min}:${s}`
}

const currentStartVal = computed(() => {
  if (props.startValue !== undefined && props.startValue !== null) return props.startValue
  if (Array.isArray(props.value) && props.value[0] !== undefined) return props.value[0]
  if (Array.isArray(props.modelValue) && props.modelValue[0] !== undefined) return props.modelValue[0]
  return ''
})

const currentEndVal = computed(() => {
  if (props.endValue !== undefined && props.endValue !== null) return props.endValue
  if (Array.isArray(props.value) && props.value[1] !== undefined) return props.value[1]
  if (Array.isArray(props.modelValue) && props.modelValue[1] !== undefined) return props.modelValue[1]
  return ''
})

const currentSingleVal = computed(() => {
  if (props.value !== undefined && props.value !== null && !Array.isArray(props.value)) return props.value
  if (props.modelValue !== undefined && props.modelValue !== null && !Array.isArray(props.modelValue)) return props.modelValue
  return ''
})

const displayStart = computed(() => formatDisplay(currentStartVal.value, props.type))
const displayEnd = computed(() => formatDisplay(currentEndVal.value, props.type))
const displaySingle = computed(() => formatDisplay(currentSingleVal.value, props.type))

watch(showPopover, (visible) => {
  if (visible) {
    if (isRange.value) {
      const s = getTimestamp(currentStartVal.value)
      const e = getTimestamp(currentEndVal.value)
      if (s && e) {
        panelValue.value = [s, e]
      } else if (s) {
        panelValue.value = [s, s + 3600000 * 24]
      } else {
        panelValue.value = null
      }
    } else {
      panelValue.value = getTimestamp(currentSingleVal.value)
    }
  }
})

function handleConfirm() {
  if (isRange.value) {
    if (Array.isArray(panelValue.value) && panelValue.value[0] && panelValue.value[1]) {
      const [s, e] = panelValue.value
      const isNum = typeof currentStartVal.value === 'number'
      const startOut = isNum ? s : toIsoInstantString(s, props.type)
      const endOut = isNum ? e : toIsoInstantString(e, props.type)
      emit('update:start-value', startOut)
      emit('update:end-value', endOut)
      emit('update:value', [startOut, endOut])
      emit('update:modelValue', [startOut, endOut])
      emit('change', [startOut, endOut])
      emit('confirm', [startOut, endOut])
    }
  } else {
    if (panelValue.value) {
      const isNum = typeof currentSingleVal.value === 'number'
      const out = isNum ? panelValue.value : toIsoInstantString(panelValue.value, props.type)
      emit('update:value', out)
      emit('update:modelValue', out)
      emit('change', out)
      emit('confirm', out)
    }
  }
  showPopover.value = false
}

function handleClear() {
  panelValue.value = null
  if (isRange.value) {
    emit('update:start-value', '')
    emit('update:end-value', '')
    emit('update:value', [])
    emit('update:modelValue', [])
    emit('change', [])
  } else {
    emit('update:value', '')
    emit('update:modelValue', '')
    emit('change', '')
  }
  emit('clear')
  showPopover.value = false
}

function handleNow() {
  const now = Date.now()
  if (isRange.value) {
    panelValue.value = [now, now + 3600000 * 24]
  } else {
    panelValue.value = now
  }
}

const datePickerThemeOverrides = computed(() => {
  const isDark = effectiveTheme.value === 'dark'
  return {
    panelColor: isDark ? '#292a2d' : '#ffffff',
    panelTextColor: isDark ? '#e8eaed' : '#202124',
    itemTextColor: isDark ? '#e8eaed' : '#202124',
    itemColorHover: isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(60, 64, 67, 0.08)',
    itemColorActive: isDark ? '#8ab4f8' : '#1a73e8',
    itemTextColorActive: '#ffffff',
    itemTextColorCurrent: isDark ? '#8ab4f8' : '#1a73e8',
    panelHeaderDividerColor: isDark ? '#3c4043' : '#d7d8da80'
  }
})
</script>

<style scoped lang="scss">
.v-date-picker {
  width: 100%;
  display: block;

  .v-date-picker-range {
    width: 100%;
  }

  :deep(.v-input) {
    cursor: pointer !important;

    .n-input__input-el {
      cursor: pointer !important;
    }
  }
}

.v-date-picker-card {
  box-sizing: border-box;

  .v-date-picker-panel {
    border: none !important;
  }
}
</style>
