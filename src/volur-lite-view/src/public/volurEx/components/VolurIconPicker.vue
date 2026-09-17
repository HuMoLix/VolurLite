<template>
  <div class="volur-icon-picker-wrapper" :style="{ width: typeof width === 'number' ? `${width}px` : width }">
    <n-popover
      v-model:show="popoverVisible"
      trigger="click"
      :placement="placement"
      :disabled="disabled"
      raw
      :show-arrow="false"
      class="volur-icon-picker-popover"
    >
      <template #trigger>
        <div
          class="volur-icon-picker-trigger"
          :class="{
            'is-focused': popoverVisible,
            'is-disabled': disabled,
            [`size-${size}`]: true
          }"
          tabindex="0"
          @keydown.enter.stop="togglePopover"
          @keydown.space.stop="togglePopover"
        >
          <!-- 左侧图标预览 -->
          <div class="trigger-icon-box">
            <VolurIcon v-if="modelValue" :name="modelValue" :size="iconDisplaySize" />
            <n-icon v-else :size="iconDisplaySize" class="trigger-placeholder-icon">
              <ShapesOutline />
            </n-icon>
          </div>

          <!-- 中间文本显示 -->
          <div class="trigger-text" :class="{ 'is-placeholder': !modelValue }">
            {{ modelValue || placeholder }}
          </div>

          <!-- 右侧动作区域 -->
          <div class="trigger-actions">
            <span
              v-if="clearable && modelValue && !disabled"
              class="clear-btn"
              title="清空图标"
              @click.stop="handleClear"
            >
              <n-icon :size="14">
                <CloseCircleOutline />
              </n-icon>
            </span>
            <n-icon :size="14" class="arrow-icon" :class="{ 'is-open': popoverVisible }">
              <ChevronDownOutline />
            </n-icon>
          </div>
        </div>
      </template>

      <!-- 弹出面板内容 -->
      <div class="picker-panel-card" :style="{ width: `${popoverWidth}px` }">
        <!-- 头部搜索栏 -->
        <div class="panel-header">
          <n-input
            v-model:value="searchKeyword"
            size="small"
            clearable
            placeholder="搜索 SVG 图标 (如: Globe, Code, Shield, Key...)"
            @update:value="handleSearchChange"
          >
            <template #prefix>
              <n-icon :size="16" class="search-prefix-icon">
                <SearchOutline />
              </n-icon>
            </template>
          </n-input>
        </div>

        <!-- 分类切换标签 -->
        <div class="panel-category-tabs">
          <button
            v-for="tab in categoryTabs"
            :key="tab.key"
            type="button"
            class="tab-btn"
            :class="{ 'is-active': activeTabKey === tab.key }"
            @click="handleTabSelect(tab.key)"
          >
            {{ tab.name }}
          </button>
        </div>

        <!-- 图标展示网格区 -->
        <div class="panel-grid-container">
          <div v-if="currentDisplayIcons.length > 0" class="icons-grid">
            <n-tooltip
              v-for="iconName in paginatedIcons"
              :key="iconName"
              placement="top"
              :show-arrow="false"
              :delay="100"
            >
              <template #trigger>
                <div
                  class="icon-cell"
                  :class="{ 'is-selected': modelValue === iconName }"
                  @click="handleSelectIcon(iconName)"
                >
                  <VolurIcon :name="iconName" :size="22" />
                </div>
              </template>
              <div class="icon-tooltip-text">{{ iconName }}</div>
            </n-tooltip>
          </div>

          <!-- 空状态 -->
          <div v-else class="empty-state">
            <n-icon :size="32" class="empty-icon">
              <SearchOutline />
            </n-icon>
            <span class="empty-text">未找到相关 SVG 图标</span>
          </div>
        </div>

        <!-- 分页控制器 -->
        <div v-if="totalPageCount > 1" class="panel-pagination-bar">
          <span class="total-hint">共 {{ currentDisplayIcons.length }} 个</span>
          <n-pagination
            v-model:page="currentPage"
            :page-size="pageSize"
            :item-count="currentDisplayIcons.length"
            simple
            size="small"
          />
        </div>

        <!-- 底部信息与动作 -->
        <div class="panel-footer">
          <div class="footer-selected-info">
            <span class="label">当前选择：</span>
            <div v-if="modelValue" class="selected-badge">
              <VolurIcon :name="modelValue" :size="14" />
              <span class="name">{{ modelValue }}</span>
            </div>
            <span v-else class="none-text">未选择</span>
          </div>

          <div class="footer-actions">
            <n-button
              v-if="modelValue"
              size="tiny"
              quaternary
              @click="handleClear"
            >
              清空
            </n-button>
            <n-button
              size="tiny"
              secondary
              @click="popoverVisible = false"
            >
              完成
            </n-button>
          </div>
        </div>
      </div>
    </n-popover>
  </div>
</template>

<script lang="ts">
export interface VolurIconPickerProps {
  value?: string | null
  placeholder?: string
  disabled?: boolean
  clearable?: boolean
  size?: 'small' | 'medium' | 'large'
  placement?:
    | 'top'
    | 'bottom'
    | 'left'
    | 'right'
    | 'top-start'
    | 'top-end'
    | 'bottom-start'
    | 'bottom-end'
    | 'left-start'
    | 'left-end'
    | 'right-start'
    | 'right-end'
  width?: string | number
  popoverWidth?: number
}
</script>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { NPopover, NInput, NIcon, NTooltip, NPagination, NButton } from 'naive-ui'
import {
  SearchOutline,
  ChevronDownOutline,
  CloseCircleOutline,
  ShapesOutline
} from '@vicons/ionicons5'
import VolurIcon, { CURATED_ICON_CATEGORIES, getAllIconNames } from './VolurIcon.vue'

const props = withDefaults(defineProps<VolurIconPickerProps>(), {
  value: undefined,
  placeholder: '选择图标',
  disabled: false,
  clearable: true,
  size: 'medium',
  placement: 'bottom-start',
  width: '100%',
  popoverWidth: 360
})

const emit = defineEmits<{
  (e: 'update:value', value: string | null): void
  (e: 'change', value: string | null): void
}>()

// 状态管理
const popoverVisible = ref(false)
const searchKeyword = ref('')
const activeTabKey = ref('security_ctf')
const currentPage = ref(1)
const pageSize = 48

// 图标池
const allIconList = getAllIconNames()

// 分类 Tabs
const categoryTabs = [
  ...CURATED_ICON_CATEGORIES,
  {
    key: 'all',
    name: '全部图标',
    icons: allIconList
  }
]

// 当前值
const modelValue = computed(() => props.value)

// 图标展示尺寸
const iconDisplaySize = computed(() => {
  if (props.size === 'small') return 16
  if (props.size === 'large') return 22
  return 18
})

// 根据分类与搜索过滤当前图标列表
const currentDisplayIcons = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()

  // 如果处于搜索状态，全局搜索或当前 tab 搜索
  if (keyword) {
    return allIconList.filter(name => name.toLowerCase().includes(keyword))
  }

  // 正常模式：按选中的 Tab 过滤
  const targetCategory = categoryTabs.find(c => c.key === activeTabKey.value)
  if (targetCategory) {
    return targetCategory.icons
  }
  return allIconList
})

// 总页数
const totalPageCount = computed(() => {
  return Math.ceil(currentDisplayIcons.value.length / pageSize)
})

// 分页切片
const paginatedIcons = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return currentDisplayIcons.value.slice(start, start + pageSize)
})

// 交互逻辑
function togglePopover() {
  if (!props.disabled) {
    popoverVisible.value = !popoverVisible.value
  }
}

function handleSearchChange() {
  currentPage.value = 1
}

function handleTabSelect(key: string) {
  activeTabKey.value = key
  searchKeyword.value = ''
  currentPage.value = 1
}

function handleSelectIcon(iconName: string) {
  emit('update:value', iconName)
  emit('change', iconName)
  popoverVisible.value = false
}

function handleClear() {
  emit('update:value', null)
  emit('change', null)
}

watch(popoverVisible, visible => {
  if (visible) {
    currentPage.value = 1
  }
})
</script>

<style scoped lang="scss">
.volur-icon-picker-wrapper {
  display: inline-block;
  vertical-align: middle;
}

/* 触发器选择框 */
.volur-icon-picker-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  box-sizing: border-box;
  background-color: var(--n-color, #ffffff);
  border: 1px solid var(--n-border-color, #dcdfe6);
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;

  &:hover:not(.is-disabled) {
    border-color: #000000;
  }

  &.is-focused {
    border-color: #000000;
    box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.08);
  }

  &.is-disabled {
    cursor: not-allowed;
    background-color: #f5f7fa;
    opacity: 0.6;
  }

  /* 尺寸 */
  &.size-small {
    height: 28px;
    padding: 0 8px;
    font-size: 12px;

    .trigger-icon-box {
      width: 18px;
      height: 18px;
    }
  }

  &.size-medium {
    height: 34px;
    padding: 0 10px;
    font-size: 13px;

    .trigger-icon-box {
      width: 22px;
      height: 22px;
    }
  }

  &.size-large {
    height: 40px;
    padding: 0 12px;
    font-size: 14px;

    .trigger-icon-box {
      width: 26px;
      height: 26px;
    }
  }

  .trigger-icon-box {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: var(--v-text-primary, #111111);

    .trigger-placeholder-icon {
      color: #999999;
    }
  }

  .trigger-text {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-family: 'Fira Code', monospace, sans-serif;
    color: var(--v-text-primary, #222222);

    &.is-placeholder {
      font-family: inherit;
      color: #999999;
    }
  }

  .trigger-actions {
    display: flex;
    align-items: center;
    gap: 4px;
    flex-shrink: 0;

    .clear-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      color: #999999;
      transition: color 0.15s ease;

      &:hover {
        color: #ef4444;
      }
    }

    .arrow-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      color: #999999;
      transition: transform 0.2s ease;

      &.is-open {
        transform: rotate(180deg);
      }
    }
  }
}

/* 弹出卡片面板 */
.picker-panel-card {
  box-sizing: border-box;
  background-color: var(--v-bg-card, #ffffff);
  border: 1px solid var(--v-border-base, #e5e7eb);
  border-radius: 8px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.12);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel-header {
  .search-prefix-icon {
    color: #888888;
  }
}

/* 分类 Tab 切换 */
.panel-category-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  padding-bottom: 2px;

  &::-webkit-scrollbar {
    height: 3px;
  }
  &::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.15);
    border-radius: 3px;
  }

  .tab-btn {
    flex-shrink: 0;
    padding: 3px 8px;
    font-size: 11px;
    border: none;
    border-radius: 4px;
    background: transparent;
    color: #666666;
    cursor: pointer;
    transition: all 0.15s ease;

    &:hover {
      background-color: rgba(0, 0, 0, 0.05);
      color: #111111;
    }

    &.is-active {
      background-color: #000000;
      color: #ffffff;
      font-weight: 500;
    }
  }
}

/* 图标网格区 */
.panel-grid-container {
  height: 240px;
  overflow-y: auto;
  box-sizing: border-box;
  padding-right: 2px;

  &::-webkit-scrollbar {
    width: 5px;
  }
  &::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.15);
    border-radius: 4px;
  }
}

.icons-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 6px;

  .icon-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 38px;
    border-radius: 6px;
    border: 1px solid transparent;
    background-color: rgba(0, 0, 0, 0.02);
    color: var(--v-text-primary, #333333);
    cursor: pointer;
    transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      border-color: #000000;
      background-color: rgba(0, 0, 0, 0.06);
      transform: translateY(-1px);
    }

    &.is-selected {
      border-color: #000000;
      background-color: rgba(0, 0, 0, 0.08);
      box-shadow: 0 0 0 1px #000000 inset;
      color: #000000;
    }
  }
}

.icon-tooltip-text {
  font-family: 'Fira Code', monospace;
  font-size: 11px;
}

/* 空状态 */
.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #999999;

  .empty-icon {
    opacity: 0.5;
  }

  .empty-text {
    font-size: 12px;
  }
}

/* 分页控制栏 */
.panel-pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 4px;
  border-top: 1px solid var(--v-border-base, #f0f0f0);

  .total-hint {
    font-size: 11px;
    color: #888888;
  }
}

/* 底部操作 */
.panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 6px;
  border-top: 1px solid var(--v-border-base, #f0f0f0);

  .footer-selected-info {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #666666;

    .label {
      font-size: 11px;
    }

    .selected-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 1px 6px;
      border-radius: 3px;
      background-color: rgba(0, 0, 0, 0.05);
      color: #111111;
      font-family: 'Fira Code', monospace;
      font-size: 11px;
    }

    .none-text {
      font-size: 11px;
      color: #aaaaaa;
    }
  }

  .footer-actions {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

/* 黑夜模式深度适配 (Obsidian Dark) */
:global(html.dark) {
  .volur-icon-picker-trigger {
    background-color: #151821;
    border-color: rgba(255, 255, 255, 0.12);

    &:hover:not(.is-disabled) {
      border-color: rgba(255, 255, 255, 0.35);
    }

    &.is-focused {
      border-color: #ffffff;
      box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.15);
    }

    .trigger-text {
      color: #f1f5f9;

      &.is-placeholder {
        color: #64748b;
      }
    }
  }

  .picker-panel-card {
    background-color: #151821;
    border-color: rgba(255, 255, 255, 0.12);
    box-shadow: 0 12px 36px rgba(0, 0, 0, 0.5);
  }

  .panel-category-tabs {
    .tab-btn {
      color: #94a3b8;

      &:hover {
        background-color: rgba(255, 255, 255, 0.08);
        color: #ffffff;
      }

      &.is-active {
        background-color: #ffffff;
        color: #0b0c10;
      }
    }
  }

  .icons-grid {
    .icon-cell {
      background-color: rgba(255, 255, 255, 0.04);
      color: #e2e8f0;

      &:hover {
        border-color: #ffffff;
        background-color: rgba(255, 255, 255, 0.1);
      }

      &.is-selected {
        border-color: #ffffff;
        background-color: rgba(255, 255, 255, 0.16);
        box-shadow: 0 0 0 1px #ffffff inset;
        color: #ffffff;
      }
    }
  }

  .panel-pagination-bar,
  .panel-footer {
    border-color: rgba(255, 255, 255, 0.08);

    .selected-badge {
      background-color: rgba(255, 255, 255, 0.1);
      color: #f8fafc;
    }
  }
}
</style>
