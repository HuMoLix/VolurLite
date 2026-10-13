<template>
  <div
    class="v-icon-picker-wrapper"
    :class="[effectiveTheme, { 'theme-dark': effectiveTheme === 'dark' }]"
    :style="{
      width: typeof width === 'number' ? `${width}px` : width,
      '--v-icon-picker-radius': resolvedRadius
    }"
  >
    <n-popover
      v-model:show="popoverVisible"
      trigger="click"
      :placement="placement"
      :disabled="disabled"
      :z-index="zIndex"
      :to="to"
      raw
      :show-arrow="false"
      class="v-icon-picker-popover"
    >
      <template #trigger>
        <div
          class="v-icon-picker-trigger"
          :class="[
            `size-${size}`,
            {
              'is-focused': popoverVisible,
              'is-disabled': disabled
            }
          ]"
          tabindex="0"
          @keydown.enter.stop="togglePopover"
          @keydown.space.stop="togglePopover"
        >
          <div class="trigger-icon-box flex items-center justify-center shrink-0">
            <VIcon v-if="currentValue" :name="currentValue" :size="iconDisplaySize" />
            <n-icon v-else :size="iconDisplaySize" class="trigger-placeholder-icon">
              <ShapesOutline />
            </n-icon>
          </div>

          <div
            class="trigger-text"
            :class="{ 'is-placeholder': !currentValue }"
          >
            {{ currentValue || placeholder }}
          </div>

          <div class="trigger-actions flex items-center gap-1 shrink-0">
            <span
              v-if="clearable && currentValue && !disabled"
              class="clear-btn p-0.5 hover:opacity-80 transition-opacity"
              :title="t('common.clear', '清空')"
              @click.stop="handleClear"
            >
              <n-icon :size="14">
                <CloseCircleOutline />
              </n-icon>
            </span>
            <n-icon :size="14" class="arrow-icon" :class="{ 'rotate-180': popoverVisible }">
              <ChevronDownOutline />
            </n-icon>
          </div>
        </div>
      </template>

      <div
        class="picker-panel-card relative p-3 rounded-xl bg-white dark:bg-[#292a2d] border border-google-border-light dark:border-google-border-dark shadow-2xl flex flex-col gap-3 box-border"
        :style="{ width: `${popoverWidth}px`, zIndex: zIndex }"
        @mousedown.stop
        @click.stop
      >
        <div class="panel-header">
          <VSearch
            v-model:value="searchKeyword"
            size="small"
            clearable
            :placeholder="t('common.search_icon_placeholder', '搜索图标 (如: Globe, Shield, Key...)')"
            @update:value="handleSearchChange"
          />
        </div>

        <div class="panel-category-tabs">
          <VFilterBtnGroup
            v-model:value="activeTabKey"
            :options="categoryTabs"
            size="small"
            :responsive="false"
            :bullet="false"
            @change="handleTabSelect"
          />
        </div>

        <div class="panel-grid-container h-60 overflow-y-auto pr-1">
          <div v-if="currentDisplayIcons.length > 0" class="icons-grid grid grid-cols-6 gap-1.5">
            <VTooltip
              v-for="iconName in paginatedIcons"
              :key="iconName"
              placement="top"
              :show-arrow="false"
              :delay="100"
              :z-index="4000"
            >
              <template #trigger>
                <div
                  class="icon-cell h-9 flex items-center justify-center rounded-lg border cursor-pointer transition-all"
                  :class="currentValue === iconName ? 'border-google-blue dark:border-google-blue-dark bg-google-blue/15 dark:bg-google-blue-dark/20 text-google-blue dark:text-google-blue-dark font-semibold shadow-sm' : 'border-transparent bg-gray-50 dark:bg-[#202124] text-google-text-primary dark:text-google-text-darkPrimary hover:border-google-blue dark:hover:border-google-blue-dark hover:bg-google-blue/10 dark:hover:bg-google-blue-dark/10'"
                  @click="handleSelectIcon(iconName)"
                >
                  <VIcon :name="iconName" :size="20" />
                </div>
              </template>
              <div class="font-mono text-xs">{{ iconName }}</div>
            </VTooltip>
          </div>

          <div v-else class="empty-state h-full flex flex-col items-center justify-center gap-2 text-google-text-muted dark:text-google-text-darkMuted py-8">
            <n-icon :size="28" class="opacity-50">
              <SearchOutline />
            </n-icon>
            <span class="text-xs">{{ t('common.no_icon_found', '未找到相关图标') }}</span>
          </div>
        </div>

        <div v-if="totalPageCount > 1" class="panel-pagination-bar flex items-center justify-between pt-2 border-t border-google-border-light dark:border-google-border-dark text-xs">
          <span class="text-google-text-muted dark:text-google-text-darkMuted text-[11px]">
            共 {{ currentDisplayIcons.length }} 个
          </span>
          <VPagination
            v-model:page="currentPage"
            :page-size="pageSize"
            :item-count="currentDisplayIcons.length"
            simple
            size="small"
          />
        </div>
      </div>
    </n-popover>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { NPopover, NIcon } from 'naive-ui'
import {
  SearchOutline,
  ChevronDownOutline,
  CloseCircleOutline,
  ShapesOutline
} from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import VIcon, { ICON_SETS, getAllIconNames } from './VIcon.vue'
import VSearch from './VSearch.vue'
import VFilterBtnGroup from './VFilterBtnGroup.vue'
import VTooltip from './VTooltip.vue'
import VPagination from './VPagination.vue'
import type { VIconPickerProps, FilterOption } from './types'

defineOptions({
  name: 'VIconPicker'
})

const props = withDefaults(defineProps<VIconPickerProps>(), {
  modelValue: undefined,
  value: undefined,
  placeholder: '选择图标',
  disabled: false,
  clearable: true,
  size: 'medium',
  placement: 'bottom-start',
  width: '100%',
  popoverWidth: 460,
  round: false,
  radius: undefined,
  theme: undefined,
  zIndex: 3000,
  to: 'body'
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: string | null): void
  (e: 'update:value', val: string | null): void
  (e: 'change', val: string | null): void
}>()

const { t } = useI18n()
const appStore = useAppStore()

const popoverVisible = ref(false)
const searchKeyword = ref('')
const activeTabKey = ref('all')
const currentPage = ref(1)
const pageSize = 48

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

const currentValue = computed<string | null>(() => {
  if (props.value !== undefined) return props.value
  if (props.modelValue !== undefined) return props.modelValue
  return null
})

const iconDisplaySize = computed<number>(() => {
  if (props.size === 'small') return 16
  if (props.size === 'large') return 22
  return 18
})

const categoryTabs: FilterOption<string>[] = [
  { label: 'all', value: 'all' },
  { label: 'fluent', value: 'fluent' },
  { label: 'ionicons4', value: 'ionicons4' },
  { label: 'ionicons5', value: 'ionicons5' },
  { label: 'antd', value: 'antd' },
  { label: 'fa', value: 'fa' },
  { label: 'material', value: 'material' },
  { label: 'tabler', value: 'tabler' },
  { label: 'carbon', value: 'carbon' }
]

const allIconNamesList = getAllIconNames()

const currentDisplayIcons = computed<string[]>(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  let list: string[]

  if (activeTabKey.value === 'all') {
    list = allIconNamesList
  } else {
    list = ICON_SETS[activeTabKey.value] || []
  }

  if (keyword) {
    return list.filter((name) => name.toLowerCase().includes(keyword))
  }
  return list
})

const totalPageCount = computed<number>(() => {
  return Math.ceil(currentDisplayIcons.value.length / pageSize)
})

const paginatedIcons = computed<string[]>(() => {
  const start = (currentPage.value - 1) * pageSize
  return currentDisplayIcons.value.slice(start, start + pageSize)
})

function togglePopover() {
  if (!props.disabled) {
    popoverVisible.value = !popoverVisible.value
  }
}

function handleTabSelect(key: string) {
  activeTabKey.value = key
  currentPage.value = 1
}

function handleSearchChange() {
  currentPage.value = 1
}

function handleSelectIcon(iconName: string) {
  emit('update:value', iconName)
  emit('update:modelValue', iconName)
  emit('change', iconName)
  popoverVisible.value = false
}

function handleClear() {
  emit('update:value', null)
  emit('update:modelValue', null)
  emit('change', null)
}

watch(popoverVisible, (visible) => {
  if (visible) {
    searchKeyword.value = ''
    currentPage.value = 1
  }
})
</script>

<style scoped lang="scss">
.v-icon-picker-wrapper {
  --v-icon-picker-radius: 8px;
  display: inline-block;
  vertical-align: middle;
}

.v-icon-picker-trigger {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  border-radius: var(--v-icon-picker-radius, 8px);
  font-family: Inter, sans-serif;
  font-optical-sizing: auto;
  font-weight: 400;
  cursor: pointer;
  user-select: none;
  outline: none;
  background-color: transparent;
  border: 1px solid #d7d8da80;
  color: #1b1b1e;
  transition: border-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &.size-small {
    height: 28px;
    font-size: 13px;
    line-height: 20px;
    padding: 0 8px;
  }

  &.size-medium {
    height: 36px;
    font-size: 14px;
    line-height: 21px;
    padding: 0 10px;
  }

  &.size-large {
    height: 44px;
    font-size: 14px;
    line-height: 21px;
    padding: 0 12px;
  }

  &:hover:not(.is-disabled) {
    border-color: #d7d8da80;
  }

  &.is-focused:not(.is-disabled) {
    border-color: #b7babd;
  }

  .trigger-icon-box {
    color: inherit;
  }

  .trigger-placeholder-icon {
    color: #9aa0a6;
  }

  .trigger-text {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #1b1b1e;

    &.is-placeholder {
      color: #9aa0a6;
    }
  }

  .trigger-actions {
    color: #565b61;
  }

  .arrow-icon {
    color: #565b61;
    transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), color 0.2s;
  }

  &.is-disabled {
    cursor: not-allowed;
    background-color: #f5f6f7;
    border-color: #d7d8da80;
    color: #80868b;

    .trigger-placeholder-icon {
      color: #bdc1c6;
    }

    .trigger-text {
      color: #80868b;
      &.is-placeholder {
        color: #bdc1c6;
      }
    }

    .trigger-actions {
      color: #9aa0a6;
    }

    .arrow-icon {
      color: #9aa0a6;
    }
  }
}

.theme-dark,
html.dark {
  .v-icon-picker-trigger {
    border-color: #262626;
    color: #d4d4d4;

    &:hover:not(.is-disabled) {
      border-color: #262626;
    }

    &.is-focused:not(.is-disabled) {
      border-color: #333;
    }

    .trigger-placeholder-icon {
      color: #80868b;
    }

    .trigger-text {
      color: #d4d4d4;

      &.is-placeholder {
        color: #80868b;
      }
    }

    .trigger-actions {
      color: #8c8c8c;
    }

    .arrow-icon {
      color: #8c8c8c;
    }

    &.is-disabled {
      background-color: rgba(255, 255, 255, 0.04);
      border-color: #262626;
      color: #73777d;

      .trigger-placeholder-icon {
        color: #55595d;
      }

      .trigger-text {
        color: #73777d;
        &.is-placeholder {
          color: #55595d;
        }
      }

      .trigger-actions {
        color: #55595d;
      }

      .arrow-icon {
        color: #55595d;
      }
    }
  }
}
</style>
