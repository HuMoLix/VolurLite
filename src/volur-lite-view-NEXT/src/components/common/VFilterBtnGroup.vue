<template>
  <div class="v-filter-btn-group flex items-center gap-2 flex-wrap">
    <button
      v-for="tab in visibleTabs"
      :key="String(tab.value)"
      type="button"
      class="v-filter-btn competition-filter-btn"
      :class="[
        sizeClass,
        {
          'is-selected': isSelected(tab.value),
          'has-icon': Boolean(tab.icon || $slots.prefix || $slots.icon),
          'no-bullet': !bullet,
          'is-disabled': Boolean(tab.disabled || disabled)
        }
      ]"
      :disabled="tab.disabled || disabled"
      @click="handleSelect(tab)"
    >
      <slot name="prefix" :option="tab" :selected="isSelected(tab.value)">
        <n-icon
          v-if="tab.icon"
          :size="iconSize"
          class="v-filter-btn-icon flex-shrink-0"
          :component="tab.icon"
        />
      </slot>
      <slot name="label" :option="tab" :selected="isSelected(tab.value)">
        <span>{{ tab.label }}</span>
      </slot>
      <slot name="suffix" :option="tab" :selected="isSelected(tab.value)">
        <span
          v-if="tab.count !== undefined"
          class="v-filter-btn-count"
          :class="{ 'is-selected': isSelected(tab.value) }"
        >
          {{ tab.count }}
        </span>
      </slot>
    </button>

    <n-popover
      v-if="showMoreMenu"
      v-model:show="isMoreMenuShow"
      trigger="click"
      placement="bottom-start"
      :show-arrow="false"
      raw
    >
      <template #trigger>
        <button
          type="button"
          class="v-filter-btn competition-filter-btn menu-trigger-btn"
          :class="[
            sizeClass,
            {
              'is-selected': isDropdownTabSelected,
              'has-icon': true
            }
          ]"
          :title="isDropdownTabSelected ? selectedDropdownTab?.label : moreLabel"
        >
          <n-icon :size="iconSize" class="flex-shrink-0" :component="MenuOutline" />
          <span v-if="isDropdownTabSelected" class="ml-1.5">{{ selectedDropdownTab?.label }}</span>
          <span
            v-if="isDropdownTabSelected && selectedDropdownTab?.count !== undefined"
            class="v-filter-btn-count is-selected"
          >
            {{ selectedDropdownTab.count }}
          </span>
        </button>
      </template>

      <VCard
        :theme="currentTheme"
        :width="dropdownWidth"
        padding="6px"
        :gap="2"
      >
        <div class="v-filter-menu-list">
          <button
            v-for="tab in dropdownTabs"
            :key="String(tab.value)"
            type="button"
            class="v-filter-menu-item"
            :class="{
              'is-selected': isSelected(tab.value),
              'is-disabled': Boolean(tab.disabled || disabled)
            }"
            :disabled="tab.disabled || disabled"
            @click="handleDropdownSelect(tab)"
          >
            <div class="flex items-center gap-2 min-w-0">
              <n-icon
                v-if="tab.icon"
                :size="iconSize"
                class="flex-shrink-0"
                :component="tab.icon"
              />
              <span class="v-filter-menu-label">{{ tab.label }}</span>
            </div>
            <div class="flex items-center gap-2 flex-shrink-0">
              <span
                v-if="tab.count !== undefined"
                class="v-filter-menu-count"
                :class="{ 'is-selected': isSelected(tab.value) }"
              >
                {{ tab.count }}
              </span>
              <n-icon
                v-if="isSelected(tab.value)"
                :size="iconSize"
                class="v-filter-menu-check"
                :component="CheckmarkOutline"
              />
            </div>
          </button>
        </div>
      </VCard>
    </n-popover>
  </div>
</template>

<script setup lang="ts" generic="T extends string | number = string | number">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { NPopover, NIcon } from 'naive-ui'
import { MenuOutline, CheckmarkOutline } from '@vicons/ionicons5'
import { VCard } from '@/components/sidebar'
import { useAppStore } from '@/store/modules/app'
import type { FilterOption, VFilterBtnGroupProps } from './types'

defineOptions({
  name: 'VFilterBtnGroup'
})

const props = withDefaults(defineProps<VFilterBtnGroupProps<T>>(), {
  modelValue: undefined,
  value: undefined,
  responsive: true,
  bullet: true,
  disabled: false,
  maxVisible: undefined,
  breakpoints: () => ({ mobile: 460, tablet: 860 }),
  collapseVisible: () => ({ mobile: 2, tablet: 3 }),
  dropdownWidth: 190,
  moreText: undefined,
  size: 'medium'
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: T): void
  (e: 'update:value', val: T): void
  (e: 'change', val: T, option: FilterOption<T>): void
}>()

const { t } = useI18n()
const appStore = useAppStore()

const isMoreMenuShow = ref(false)
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)

const currentTheme = computed<'light' | 'dark'>(() => {
  return appStore.theme === 'dark' ? 'dark' : 'light'
})

const currentValue = computed<T | undefined>(() => {
  if (props.modelValue !== undefined) return props.modelValue
  if (props.value !== undefined) return props.value
  return undefined
})

const normalizedOptions = computed<FilterOption<T>[]>(() => {
  return (props.options || []).map((opt) => {
    if (typeof opt === 'object' && opt !== null) {
      return {
        ...opt,
        label: opt.label !== undefined ? opt.label : String(opt.value)
      }
    }
    return {
      value: opt as T,
      label: String(opt)
    }
  })
})

const visibleCount = computed(() => {
  const total = normalizedOptions.value.length
  if (props.maxVisible !== undefined) {
    return Math.min(Math.max(0, props.maxVisible), total)
  }
  if (!props.responsive) {
    return total
  }
  const mobileBp = props.breakpoints?.mobile ?? 460
  const tabletBp = props.breakpoints?.tablet ?? 860
  const mobileVisible = props.collapseVisible?.mobile ?? 2
  const tabletVisible = props.collapseVisible?.tablet ?? 3

  if (windowWidth.value < mobileBp) {
    return Math.min(mobileVisible, total)
  }
  if (windowWidth.value < tabletBp) {
    return Math.min(tabletVisible, total)
  }
  return total
})

const visibleTabs = computed(() => {
  return normalizedOptions.value.slice(0, visibleCount.value)
})

const dropdownTabs = computed(() => {
  return normalizedOptions.value.slice(visibleCount.value)
})

const showMoreMenu = computed(() => dropdownTabs.value.length > 0)

const selectedDropdownTab = computed(() => {
  return dropdownTabs.value.find((tab) => tab.value === currentValue.value)
})

const isDropdownTabSelected = computed(() => {
  return Boolean(selectedDropdownTab.value)
})

const moreLabel = computed(() => {
  return props.moreText || t('common.more', '更多')
})

const sizeClass = computed(() => {
  if (props.size === 'small') return 'size-small'
  if (props.size === 'large') return 'size-large'
  return 'size-medium'
})

const iconSize = computed(() => {
  if (props.size === 'small') return 14
  if (props.size === 'large') return 18
  return 16
})

function isSelected(val: T) {
  return currentValue.value === val
}

function handleSelect(tab: FilterOption<T>) {
  if (tab.disabled || props.disabled) return
  emit('update:modelValue', tab.value)
  emit('update:value', tab.value)
  emit('change', tab.value, tab)
}

function handleDropdownSelect(tab: FilterOption<T>) {
  if (tab.disabled || props.disabled) return
  isMoreMenuShow.value = false
  handleSelect(tab)
}

function handleResize() {
  windowWidth.value = window.innerWidth
}

onMounted(() => {
  if (props.responsive) {
    window.addEventListener('resize', handleResize)
  }
})

onUnmounted(() => {
  if (props.responsive) {
    window.removeEventListener('resize', handleResize)
  }
})
</script>

<style scoped lang="scss">
.v-filter-btn-group {
  user-select: none;
}

.v-filter-btn {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  box-sizing: border-box !important;
  padding: 6px 12px !important;
  border-radius: 12px !important;
  border: 1px solid #d7d8da80 !important;
  box-shadow: 0 1px 2px 0 #0000000d !important;
  color: #2b2d31 !important;
  font-family: Inter, sans-serif !important;
  font-size: 14px !important;
  line-height: 20px !important;
  letter-spacing: normal !important;
  cursor: pointer !important;
  user-select: none !important;
  white-space: nowrap !important;
  background-color: transparent !important;
  outline: none !important;
  transition: background-color 0.15s ease-in-out, border-color 0.15s ease-in-out, color 0.15s ease-in-out, box-shadow 0.1s ease-in-out, padding-left 0.15s ease-in-out !important;

  &:focus,
  &:active,
  &:focus-visible {
    outline: none !important;
  }

  &:hover {
    background-color: #eaeaeb !important;
  }

  &.is-selected,
  &.selected {
    background-color: #f4f4f5 !important;
    border-color: #d7d8da !important;
  }

  &.is-selected:not(.has-icon):not(.no-bullet),
  &.selected:not(.has-icon):not(.no-bullet) {
    padding-left: 26px !important;
    background-size: 100% !important;
    background-image: radial-gradient(circle at 16px center, var(--color-v3-text, currentColor) 3px, transparent 3.4px) !important;
    background-repeat: no-repeat !important;
  }

  &.has-icon {
    gap: 6px !important;
  }

  &.size-small {
    padding: 4px 10px !important;
    font-size: 13px !important;
    line-height: 18px !important;
    border-radius: 10px !important;

    &.is-selected:not(.has-icon):not(.no-bullet),
    &.selected:not(.has-icon):not(.no-bullet) {
      padding-left: 22px !important;
      background-image: radial-gradient(circle at 12px center, var(--color-v3-text, currentColor) 2.5px, transparent 2.9px) !important;
    }
  }

  &.size-large {
    padding: 8px 16px !important;
    font-size: 15px !important;
    line-height: 22px !important;
    border-radius: 14px !important;

    &.is-selected:not(.has-icon):not(.no-bullet),
    &.selected:not(.has-icon):not(.no-bullet) {
      padding-left: 30px !important;
      background-image: radial-gradient(circle at 18px center, var(--color-v3-text, currentColor) 3.5px, transparent 4px) !important;
    }
  }

  html.dark & {
    border-color: #262626 !important;
    color: #fcfcfc !important;

    &:hover {
      background-color: #2a2b2d !important;
    }

    &.is-selected,
    &.selected {
      background-color: #262626 !important;
      border-color: #333 !important;
    }
  }

  &.menu-trigger-btn {
    &.is-selected,
    &.selected {
      padding-left: 12px !important;
      background-image: none !important;
      background-color: #f4f4f5 !important;
      border-color: #d7d8da !important;
    }

    html.dark & {
      &.is-selected,
      &.selected {
        background-color: #262626 !important;
        border-color: #333 !important;
      }
    }
  }

  &.is-disabled {
    cursor: not-allowed !important;
    opacity: 0.5 !important;
  }
}

.v-filter-btn-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  line-height: 1;
  font-family: monospace, ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 9999px;
  background-color: rgba(60, 64, 67, 0.08);
  color: var(--color-v3-text-muted, #5f6368);
  margin-left: 4px;
  transition: background-color 0.15s ease-in-out, color 0.15s ease-in-out;

  html.dark & {
    background-color: rgba(255, 255, 255, 0.08);
    color: var(--color-v3-text-muted, #9aa0a6);
  }

  &.is-selected {
    background-color: rgba(0, 0, 0, 0.08);
    color: #1b1b1e;

    html.dark & {
      background-color: rgba(255, 255, 255, 0.16);
      color: #fcfcfc;
    }
  }
}

.v-filter-menu-list {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 2px;
}

.v-filter-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 36px;
  padding: 0 10px;
  border-radius: 8px;
  border: none;
  background-color: transparent;
  cursor: pointer;
  color: #3c4043;
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  outline: none;
  box-sizing: border-box;
  transition: background-color 0.15s cubic-bezier(0.2, 0, 0, 1),
              color 0.15s cubic-bezier(0.2, 0, 0, 1);

  &:hover {
    background-color: rgba(60, 64, 67, 0.08);
    color: #202124;
  }

  &:active {
    background-color: rgba(60, 64, 67, 0.12);
  }

  &.is-selected {
    color: #1a73e8;
    font-weight: 600;

    &:hover {
      background-color: rgba(26, 115, 232, 0.08);
    }
  }

  &.is-disabled {
    cursor: not-allowed;
    opacity: 0.5;
  }

  .v-filter-menu-label {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .v-filter-menu-check {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    color: #1a73e8;
    flex-shrink: 0;
  }

  .v-filter-menu-count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    font-family: monospace, ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    padding: 1px 6px;
    border-radius: 9999px;
    background-color: rgba(60, 64, 67, 0.08);
    color: #5f6368;
  }

  html.dark & {
    color: #bdc1c6;

    &:hover {
      background-color: rgba(255, 255, 255, 0.08);
      color: #e8eaed;
    }

    &:active {
      background-color: rgba(255, 255, 255, 0.12);
    }

    &.is-selected {
      color: #a8c7fa;

      &:hover {
        background-color: rgba(168, 199, 250, 0.12);
      }

      .v-filter-menu-check {
        color: #a8c7fa;
      }

      .v-filter-menu-count {
        background-color: rgba(255, 255, 255, 0.16);
        color: #a8c7fa;
      }
    }
  }
}
</style>
