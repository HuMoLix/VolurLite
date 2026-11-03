<template>
  <nav
    class="v-pagination"
    :class="[
      `size-${size}`,
      effectiveTheme,
      {
        'is-disabled': disabled,
        'is-simple': simple,
        'is-round': round === true
      }
    ]"
    :style="customStyle"
    aria-label="Pagination Navigation"
  >
    <div v-if="$slots.prefix" class="v-pagination-prefix">
      <slot name="prefix" />
    </div>

    <template v-if="simple">
      <VBtn
        class="v-pagination-nav-btn"
        clz="normal"
        :size="computedBtnSize"
        :disabled="disabled || innerPage <= 1"
        :left-icon="ChevronBackOutline"
        :icon-size="computedIconSize"
        :radius="resolvedRadius"
        aria-label="Previous Page"
        @click="handlePrev"
      />

      <span class="v-pagination-simple-text">
        {{ innerPage }} / {{ totalPages }}
      </span>

      <VBtn
        class="v-pagination-nav-btn"
        clz="normal"
        :size="computedBtnSize"
        :disabled="disabled || innerPage >= totalPages"
        :left-icon="ChevronForwardOutline"
        :icon-size="computedIconSize"
        :radius="resolvedRadius"
        aria-label="Next Page"
        @click="handleNext"
      />
    </template>

    <template v-else>
      <div class="v-pagination-list">
        <VBtn
          class="v-pagination-nav-btn"
          clz="normal"
          :size="computedBtnSize"
          :disabled="disabled || innerPage <= 1"
          :left-icon="ChevronBackOutline"
          :icon-size="computedIconSize"
          :radius="resolvedRadius"
          aria-label="Previous Page"
          @click="handlePrev"
        />

        <template v-for="(item, idx) in paginationItems" :key="`${item.type}-${item.value}-${idx}`">
          <VBtn
            v-if="item.type === 'page'"
            class="v-pagination-item-btn"
            :class="{ 'is-active': item.value === innerPage }"
            :clz="item.value === innerPage ? 'primary' : 'normal'"
            :size="computedBtnSize"
            :disabled="disabled"
            :radius="resolvedRadius"
            :aria-label="`Page ${item.value}`"
            :aria-current="item.value === innerPage ? 'page' : undefined"
            @click="handlePageChange(item.value)"
          >
            {{ item.value }}
          </VBtn>

          <VBtn
            v-else
            class="v-pagination-item-btn v-pagination-ellipsis-btn"
            clz="normal"
            :size="computedBtnSize"
            :disabled="disabled"
            :left-icon="EllipsisHorizontalOutline"
            :icon-size="computedIconSize"
            :radius="resolvedRadius"
            :aria-label="item.type === 'prev-fast' ? 'Fast backward 5 pages' : 'Fast forward 5 pages'"
            @click="handleFastJump(item.type)"
          />
        </template>

        <VBtn
          class="v-pagination-nav-btn"
          clz="normal"
          :size="computedBtnSize"
          :disabled="disabled || innerPage >= totalPages"
          :left-icon="ChevronForwardOutline"
          :icon-size="computedIconSize"
          :radius="resolvedRadius"
          aria-label="Next Page"
          @click="handleNext"
        />
      </div>
    </template>

    <div v-if="showSizePicker" class="v-pagination-size-picker-wrap">
      <VSelection
        :value="innerPageSize"
        :options="pageSizeOptions"
        :disabled="disabled"
        :size="size"
        class="v-pagination-size-picker"
        @update:value="handlePageSizeChange"
      />
    </div>

    <div v-if="showQuickJumper" class="v-pagination-jumper">
      <span class="v-pagination-jumper-label">跳至</span>
      <VInput
        v-model:value="jumperInput"
        :disabled="disabled"
        :size="size"
        class="v-pagination-jumper-input"
        @keydown="(ev) => handleJumperKeyDown(ev)"
        @blur="handleJumperSubmit"
      />
      <span class="v-pagination-jumper-label">页</span>
    </div>

    <div v-if="$slots.suffix" class="v-pagination-suffix">
      <slot name="suffix" />
    </div>
  </nav>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { CSSProperties } from 'vue'
import {
  ChevronBackOutline,
  ChevronForwardOutline,
  EllipsisHorizontalOutline
} from '@vicons/ionicons5'
import type { SelectOption } from 'naive-ui'
import { useAppStore } from '@/store/modules/app'
import VBtn from './VBtn.vue'
import VInput from './VInput.vue'
import VSelection from './VSelection.vue'
import type { VPaginationProps, VBtnSize } from './types'

defineOptions({
  name: 'VPagination'
})

const props = withDefaults(defineProps<VPaginationProps>(), {
  page: 1,
  pageSize: 10,
  itemCount: undefined,
  pageCount: undefined,
  pageSizes: () => [10, 20, 50],
  showSizePicker: false,
  showQuickJumper: false,
  disabled: false,
  size: 'medium',
  pageSlot: 7,
  simple: false,
  round: false,
  radius: undefined,
  theme: undefined
})

const emit = defineEmits<{
  (e: 'update:page', val: number): void
  (e: 'update:pageSize', val: number): void
  (e: 'change', val: number): void
  (e: 'pageSizeChange', val: number): void
}>()

const appStore = useAppStore()
const jumperInput = ref<string>('')

const effectiveTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const resolvedRadius = computed<string | undefined>(() => {
  if (props.round) {
    return '9999px'
  }
  if (props.radius !== undefined && props.radius !== null && props.radius !== '') {
    return typeof props.radius === 'number' ? `${props.radius}px` : String(props.radius)
  }
  return undefined
})

const customStyle = computed<CSSProperties>(() => {
  if (resolvedRadius.value) {
    return {
      '--v-pagination-radius': resolvedRadius.value
    } as CSSProperties
  }
  return {}
})

const computedBtnSize = computed<VBtnSize>(() => {
  if (props.size === 'small') return 'small'
  if (props.size === 'large') return 'medium'
  return 'small'
})

const computedIconSize = computed<number>(() => {
  if (props.size === 'small') return 14
  if (props.size === 'large') return 18
  return 15
})

const innerPage = computed<number>({
  get() {
    return props.page ?? 1
  },
  set(val: number) {
    emit('update:page', val)
    emit('change', val)
  }
})

const innerPageSize = computed<number>({
  get() {
    return props.pageSize ?? 10
  },
  set(val: number) {
    emit('update:pageSize', val)
    emit('pageSizeChange', val)
  }
})

const totalPages = computed<number>(() => {
  if (props.pageCount !== undefined && props.pageCount !== null && props.pageCount > 0) {
    return Math.max(1, props.pageCount)
  }
  if (props.itemCount !== undefined && props.itemCount !== null) {
    return Math.max(1, Math.ceil(props.itemCount / innerPageSize.value))
  }
  return 1
})

interface PaginationItem {
  type: 'page' | 'prev-fast' | 'next-fast'
  value: number
}

const paginationItems = computed<PaginationItem[]>(() => {
  const count = totalPages.value
  const current = innerPage.value
  const slot = Math.max(5, props.pageSlot ?? 7)

  if (count <= slot) {
    const items: PaginationItem[] = []
    for (let i = 1; i <= count; i++) {
      items.push({ type: 'page', value: i })
    }
    return items
  }

  const items: PaginationItem[] = []
  items.push({ type: 'page', value: 1 })

  const sideSlots = Math.floor((slot - 3) / 2)
  const leftBound = current - sideSlots
  const rightBound = current + sideSlots

  const showPrevFast = leftBound > 2
  const showNextFast = rightBound < count - 1

  if (showPrevFast && !showNextFast) {
    items.push({ type: 'prev-fast', value: Math.max(1, current - 5) })
    const start = count - (slot - 3)
    for (let i = start; i < count; i++) {
      items.push({ type: 'page', value: i })
    }
  } else if (!showPrevFast && showNextFast) {
    const end = slot - 2
    for (let i = 2; i <= end; i++) {
      items.push({ type: 'page', value: i })
    }
    items.push({ type: 'next-fast', value: Math.min(count, current + 5) })
  } else if (showPrevFast && showNextFast) {
    items.push({ type: 'prev-fast', value: Math.max(1, current - 5) })
    const windowSlots = slot - 4
    const half = Math.floor(windowSlots / 2)
    for (let i = current - half; i <= current + half; i++) {
      items.push({ type: 'page', value: i })
    }
    items.push({ type: 'next-fast', value: Math.min(count, current + 5) })
  } else {
    for (let i = 2; i < count; i++) {
      items.push({ type: 'page', value: i })
    }
  }

  items.push({ type: 'page', value: count })
  return items
})

const pageSizeOptions = computed<SelectOption[]>(() => {
  return props.pageSizes.map((item) => {
    if (typeof item === 'number') {
      return {
        label: `${item} 条/页`,
        value: item
      }
    }
    return item as SelectOption
  })
})

function handlePageChange(target: number) {
  if (props.disabled) return
  const clamped = Math.max(1, Math.min(totalPages.value, target))
  if (clamped !== innerPage.value) {
    innerPage.value = clamped
  }
}

function handlePrev() {
  if (props.disabled || innerPage.value <= 1) return
  handlePageChange(innerPage.value - 1)
}

function handleNext() {
  if (props.disabled || innerPage.value >= totalPages.value) return
  handlePageChange(innerPage.value + 1)
}

function handleFastJump(type: 'prev-fast' | 'next-fast') {
  if (props.disabled) return
  const step = 5
  if (type === 'prev-fast') {
    handlePageChange(innerPage.value - step)
  } else {
    handlePageChange(innerPage.value + step)
  }
}

function handlePageSizeChange(val: any) {
  const sizeNum = typeof val === 'number' ? val : parseInt(String(val), 10)
  if (isNaN(sizeNum) || sizeNum <= 0) return
  innerPageSize.value = sizeNum
  const newTotalPages = Math.max(1, Math.ceil((props.itemCount ?? 0) / sizeNum))
  if (innerPage.value > newTotalPages) {
    innerPage.value = newTotalPages
  }
}

function handleJumperSubmit() {
  if (props.disabled || !jumperInput.value.trim()) return
  const parsed = parseInt(jumperInput.value.trim(), 10)
  if (!isNaN(parsed)) {
    handlePageChange(parsed)
  }
  jumperInput.value = ''
}

function handleJumperKeyDown(ev: KeyboardEvent) {
  if (ev.key === 'Enter') {
    handleJumperSubmit()
  }
}
</script>

<style scoped lang="scss">
.v-pagination {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-family: Inter, sans-serif;
  user-select: none;
  box-sizing: border-box;

  &.is-disabled {
    opacity: 0.55;
    pointer-events: none;
    cursor: not-allowed;
  }

  .v-pagination-prefix,
  .v-pagination-suffix {
    display: inline-flex;
    align-items: center;
    font-size: 13px;
    color: #5f6368;

    html.dark & {
      color: #9aa0a6;
    }
  }

  .v-pagination-simple-text {
    font-size: 13px;
    font-weight: 500;
    color: #3c4043;
    padding: 0 4px;

    html.dark & {
      color: #e8eaed;
    }
  }

  .v-pagination-list {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  :deep(.v-pagination-item-btn),
  :deep(.v-pagination-nav-btn) {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    font-family: Inter, sans-serif;
    font-weight: 500;
    line-height: 1;
    min-width: 32px;
    width: 32px;
    height: 32px !important;
    padding: 0 !important;
    border-radius: var(--v-pagination-radius, 8px) !important;
    font-size: 13px;
    transition: background-color 0.15s ease-in-out, border-color 0.15s ease-in-out, color 0.15s ease-in-out;
  }

  :deep(.v-pagination-item-btn.v-btn-normal),
  :deep(.v-pagination-nav-btn.v-btn-normal) {
    border-color: transparent !important;
    background-color: transparent;
    color: #3c4043;

    &:hover:not(:disabled) {
      background-color: #f1f3f4;
      color: #202124;
    }

    &:active:not(:disabled) {
      background-color: #e8eaed;
    }

    html.dark & {
      border-color: transparent !important;
      background-color: transparent;
      color: #bdc1c6;

      &:hover:not(:disabled) {
        background-color: #303134;
        color: #e8eaed;
      }

      &:active:not(:disabled) {
        background-color: #3c4043;
      }
    }
  }

  :deep(.v-pagination-item-btn.v-btn-primary) {
    background-color: #1a73e8;
    color: #ffffff;
    border-color: #1a73e8 !important;

    &:hover:not(:disabled) {
      background-color: #1765cc;
      border-color: #1765cc !important;
    }

    &:active:not(:disabled) {
      background-color: #1557b0;
      border-color: #1557b0 !important;
    }

    html.dark & {
      background-color: #8ab4f8;
      color: #202124;
      border-color: #8ab4f8 !important;

      &:hover:not(:disabled) {
        background-color: #aecbfa;
        border-color: #aecbfa !important;
      }
    }
  }

  :deep(.v-pagination-ellipsis-btn) {
    color: #5f6368 !important;

    html.dark & {
      color: #9aa0a6 !important;
    }
  }

  .v-pagination-size-picker-wrap {
    display: inline-flex;
    align-items: center;
  }

  :deep(.v-pagination-size-picker) {
    width: 104px;
  }

  .v-pagination-jumper {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #5f6368;

    html.dark & {
      color: #9aa0a6;
    }
  }

  :deep(.v-pagination-jumper-input) {
    width: 52px;

    .n-input__input-el {
      text-align: center;
    }
  }

  &.size-small {
    gap: 8px;

    :deep(.v-pagination-item-btn),
    :deep(.v-pagination-nav-btn) {
      min-width: 28px;
      width: 28px;
      height: 28px !important;
      border-radius: var(--v-pagination-radius, 6px) !important;
      font-size: 12px;
    }

    :deep(.v-pagination-size-picker) {
      width: 96px;
    }

    :deep(.v-pagination-jumper-input) {
      width: 48px;
    }

    .v-pagination-prefix,
    .v-pagination-suffix,
    .v-pagination-simple-text,
    .v-pagination-jumper {
      font-size: 12px;
    }
  }

  &.size-large {
    gap: 12px;

    :deep(.v-pagination-item-btn),
    :deep(.v-pagination-nav-btn) {
      min-width: 38px;
      width: 38px;
      height: 38px !important;
      border-radius: var(--v-pagination-radius, 10px) !important;
      font-size: 14px;
    }

    :deep(.v-pagination-size-picker) {
      width: 118px;
    }

    :deep(.v-pagination-jumper-input) {
      width: 58px;
    }

    .v-pagination-prefix,
    .v-pagination-suffix,
    .v-pagination-simple-text,
    .v-pagination-jumper {
      font-size: 14px;
    }
  }

  &.is-round {
    :deep(.v-pagination-item-btn),
    :deep(.v-pagination-nav-btn) {
      border-radius: 9999px !important;
    }
  }
}
</style>
