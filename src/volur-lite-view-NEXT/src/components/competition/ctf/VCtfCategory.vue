<template>
  <div class="v-ctf-category w-full">
    <div
      v-if="title || $slots.header"
      class="flex items-center justify-between pb-2 mb-2 px-1"
    >
      <slot name="header">
        <div class="flex items-center gap-2">
          <n-icon :size="16" :component="LayersOutline" class="text-google-blue dark:text-google-blue-dark" />
          <span class="text-xs sm:text-sm font-bold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ title || t('competition.challenge_categories', '赛题分类') }}
          </span>
        </div>
        <span v-if="totalCount !== undefined" class="text-[11px] font-mono text-google-text-muted dark:text-google-text-darkMuted">
          {{ totalCount }}
        </span>
      </slot>
    </div>

    <nav class="flex flex-col space-y-1">
      <button
        v-for="item in normalizedCategories"
        :key="item.value"
        type="button"
        class="challenge-btn"
        :class="{ 'is-selected': modelValue === item.value }"
        @click="handleSelect(item.value)"
      >
        <div class="challenge-btn-content">
          <n-icon
            :size="18"
            :component="item.icon || getCategoryIcon(item.value)"
            class="challenge-btn-icon"
          />
          <span class="challenge-btn-label">{{ item.label }}</span>
        </div>
      </button>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import type { Component } from 'vue'
import {
  LayersOutline,
  GlobeOutline,
  TerminalOutline,
  GitBranchOutline,
  KeyOutline,
  ExtensionPuzzleOutline,
  FolderOutline,
  AppsOutline
} from '@vicons/ionicons5'

export interface CtfCategoryItem {
  label?: string
  name?: string
  value?: string
  count?: number
  icon?: Component
}

const props = withDefaults(
  defineProps<{
    modelValue?: string
    categories?: (string | CtfCategoryItem)[]
    categoryCounts?: Record<string, number>
    title?: string
    totalCount?: number
  }>(),
  {
    modelValue: '全部',
    categories: () => ['全部'],
    categoryCounts: () => ({}),
    title: '',
    totalCount: undefined
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
}>()

const { t } = useI18n()

interface NormalizedCategory {
  label: string
  value: string
  count?: number
  icon?: Component
}

function getCategoryIcon(cat: string): Component {
  switch (cat.toLowerCase()) {
    case '全部':
    case 'all':
      return AppsOutline
    case 'web':
      return GlobeOutline
    case 'pwn':
      return TerminalOutline
    case 'reverse':
      return GitBranchOutline
    case 'crypto':
      return KeyOutline
    case 'misc':
      return ExtensionPuzzleOutline
    default:
      return FolderOutline
  }
}

const normalizedCategories = computed<NormalizedCategory[]>(() => {
  return props.categories.map((c) => {
    if (typeof c === 'string') {
      const count = props.categoryCounts?.[c]
      return {
        label: c,
        value: c,
        count,
        icon: getCategoryIcon(c)
      }
    }
    const val = c.value || c.name || c.label || ''
    const lbl = c.label || c.name || c.value || ''
    const count = c.count !== undefined ? c.count : props.categoryCounts?.[val]
    return {
      label: lbl,
      value: val,
      count,
      icon: c.icon || getCategoryIcon(val)
    }
  })
})

function handleSelect(val: string) {
  emit('update:modelValue', val)
  emit('change', val)
}
</script>

<style scoped lang="scss">
.challenge-btn {
  box-sizing: border-box;
  margin: 2px 0;
  height: 40px;
  width: 100%;
  border-radius: 16px;
  display: flex;
  align-items: center;
  background-color: transparent;
  border: none;
  outline: none;
  cursor: pointer;
  padding: 0;
  transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1), color 0.2s cubic-bezier(0.2, 0, 0, 1);

  &:hover {
    background-color: #efefef;

    .challenge-btn-label {
      color: #1b1b1e !important;
    }

    .challenge-btn-icon {
      color: #1b1b1e !important;
    }
  }

  &.is-selected {
    background-color: #ececef;

    .challenge-btn-icon {
      color: #1b1b1e !important;
    }

    .challenge-btn-label {
      color: #1b1b1e !important;
      font-weight: 600;
    }

    &:hover {
      background-color: #ececef;
    }
  }

  .challenge-btn-content {
    height: 40px;
    line-height: 40px;
    border-radius: 16px;
    padding: 0 12px;
    display: flex;
    align-items: center;
    width: 100%;
    background: transparent;
    overflow: hidden;

    .challenge-btn-icon {
      color: #565b61;
      font-size: 18px;
      margin-right: 12px !important;
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: color 0.2s cubic-bezier(0.2, 0, 0, 1);
    }

    .challenge-btn-label {
      color: #565b61;
      font-weight: 500;
      font-size: 14px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      flex: 1;
      text-align: left;
      transition: color 0.2s cubic-bezier(0.2, 0, 0, 1);
    }
  }
}

html.dark {
  .challenge-btn {
    background-color: transparent;

    &:hover {
      background-color: #2a2a2a;

      .challenge-btn-label {
        color: #d4d4d4 !important;
      }

      .challenge-btn-icon {
        color: #d4d4d4 !important;
      }
    }

    &.is-selected {
      background-color: #2a2a2a;

      .challenge-btn-icon {
        color: #d4d4d4 !important;
      }

      .challenge-btn-label {
        color: #d4d4d4 !important;
        font-weight: 600;
      }

      &:hover {
        background-color: #2a2a2a;
      }
    }

    .challenge-btn-content {
      .challenge-btn-icon {
        color: #8c8c8c;
      }

      .challenge-btn-label {
        color: #8c8c8c;
      }
    }
  }
}
</style>
