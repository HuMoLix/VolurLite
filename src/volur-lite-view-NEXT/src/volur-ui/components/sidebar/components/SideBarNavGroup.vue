<template>
  <div
    class="sidebar-nav-group"
    :class="{
      'is-collapsed': collapsed
    }"
  >
    <div v-if="resolvedTitle && !collapsed" class="sidebar-nav-group__title">
      {{ resolvedTitle }}
    </div>

    <div v-else-if="collapsed && groupIndex > 0" class="sidebar-nav-group__divider" role="separator" />

    <div class="sidebar-nav-group__list">
      <template v-for="item in group.items" :key="item.key">
        <VTooltip
          placement="right"
          :content="getItemTooltip(item)"
          :disabled="!collapsed"
        >
          <button
            type="button"
            class="volur-nav-item"
            :class="{
              'is-active': isItemActive(item),
              'is-collapsed': collapsed,
              'is-disabled': item.disabled,
              'has-children': item.children && item.children.length > 0,
              'is-expanded': isExpanded(item.key)
            }"
            :disabled="item.disabled"
            @click="handleItemClick(item)"
          >
            <span class="volur-nav-item__icon-wrap">
              <component
                :is="resolveItemIcon(item.icon)"
                v-if="resolveItemIcon(item.icon)"
                class="volur-nav-item__icon"
              />
            </span>

            <span v-if="!collapsed" class="volur-nav-item__label">
              {{ resolveItemLabel(item) }}
            </span>

            <span
              v-if="!collapsed && resolveItemBadge(item)"
              class="volur-nav-item__badge"
              :style="{ backgroundColor: item.badgeColor }"
            >
              {{ resolveItemBadge(item) }}
            </span>

            <span
              v-else-if="collapsed && resolveItemBadge(item)"
              class="volur-nav-item__badge-dot"
              :style="{ backgroundColor: item.badgeColor }"
            />

            <svg
              v-if="!collapsed && item.children && item.children.length > 0"
              viewBox="0 0 24 24"
              width="16"
              height="16"
              stroke="currentColor"
              stroke-width="2"
              fill="none"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="volur-nav-item__arrow"
              :class="{ 'is-expanded': isExpanded(item.key) }"
            >
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </button>
        </VTooltip>

        <div
          v-if="!collapsed && item.children && item.children.length > 0 && isExpanded(item.key)"
          class="volur-nav-sublist"
        >
          <button
            v-for="child in item.children"
            :key="child.key"
            type="button"
            class="volur-nav-item volur-nav-item--sub"
            :class="{
              'is-active': isItemActive(child),
              'is-disabled': child.disabled
            }"
            :disabled="child.disabled"
            @click="handleItemClick(child)"
          >
            <span class="volur-nav-item__icon-wrap">
              <component
                :is="resolveItemIcon(child.icon)"
                v-if="resolveItemIcon(child.icon)"
                class="volur-nav-item__icon"
              />
            </span>
            <span class="volur-nav-item__label">
              {{ resolveItemLabel(child) }}
            </span>
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, inject } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { Component } from 'vue'
import {
  TrophyOutline,
  RocketOutline,
  SchoolOutline,
  MegaphoneOutline,
  CubeOutline
} from '@vicons/ionicons5'
import { BulbOutlined } from '@vicons/antd'
import { BubbleChartOutlined } from '@vicons/material'
import { VTooltip } from '@/volur-ui/components/tooltip'
import { SIDEBAR_INJECTION_KEY } from '../context'
import type { SideBarNavGroupProps, SidebarNavItem } from '../types'

defineOptions({
  name: 'SideBarNavGroup'
})

const props = withDefaults(defineProps<SideBarNavGroupProps>(), {
  groupIndex: 0,
  collapsed: false,
  currentValue: undefined,
  accordion: false,
  collapsedIconSize: 20
})

const emit = defineEmits<{
  (e: 'select', key: string, item: SidebarNavItem): void
}>()

const route = useRoute()
const router = useRouter()
const { t, te } = useI18n()
const sidebarContext = inject(SIDEBAR_INJECTION_KEY, null)

const iconRegistry: Record<string, Component> = {
  TrophyOutline,
  RocketOutline,
  SchoolOutline,
  MegaphoneOutline,
  CubeOutline,
  BulbOutlined,
  BubbleChartOutlined
}

const expandedKeys = ref<Set<string>>(new Set())

const resolvedTitle = computed<string>(() => {
  if (props.group.titleKey && te(props.group.titleKey)) {
    return t(props.group.titleKey)
  }
  if (typeof props.group.label === 'function') {
    const val = props.group.label()
    return typeof val === 'string' ? val : ''
  }
  return props.group.title || (props.group.label as string) || ''
})

function resolveItemLabel(item: SidebarNavItem): string {
  if (item.titleKey && te(item.titleKey)) {
    return t(item.titleKey)
  }
  if (typeof item.label === 'function') {
    const val = item.label()
    return typeof val === 'string' ? val : ''
  }
  return (item.label as string) || (item.titleKey ? t(item.titleKey) : item.key)
}

function resolveItemIcon(icon?: Component | string | (() => any)): any {
  if (!icon) return undefined
  if (typeof icon === 'string') {
    return iconRegistry[icon]
  }
  return icon
}

function resolveItemBadge(item: SidebarNavItem): string | number | null {
  if (!item.badge) return null
  if (typeof item.badge === 'function') {
    const val = item.badge()
    return typeof val === 'string' || typeof val === 'number' ? val : null
  }
  return item.badge
}

function getItemTooltip(item: SidebarNavItem): string {
  const label = resolveItemLabel(item)
  const badge = resolveItemBadge(item)
  return badge ? `${label} (${badge})` : label
}

function isExpanded(key: string): boolean {
  return expandedKeys.value.has(key)
}

function toggleExpand(key: string) {
  if (expandedKeys.value.has(key)) {
    expandedKeys.value.delete(key)
  } else {
    if (props.accordion) {
      expandedKeys.value.clear()
    }
    expandedKeys.value.add(key)
  }
}

function isItemActive(item: SidebarNavItem): boolean {
  if (props.currentValue !== undefined) {
    if (props.currentValue === item.key) return true
    if (item.children?.some(c => c.key === props.currentValue)) return true
    return false
  }
  if (item.route) {
    if (route.path === item.route || route.path.startsWith(`${item.route}/`)) {
      return true
    }
    if (item.key === 'competitions' && route.path.startsWith('/c/')) {
      return true
    }
  }
  if (item.children) {
    return item.children.some(c => c.route && (route.path === c.route || route.path.startsWith(`${c.route}/`)))
  }
  return false
}

function autoExpandActive() {
  for (const item of props.group.items) {
    if (item.children && isItemActive(item)) {
      expandedKeys.value.add(item.key)
    }
  }
}

watch(
  () => route.path,
  () => {
    autoExpandActive()
  },
  { immediate: true }
)

function handleItemClick(item: SidebarNavItem) {
  if (item.children && item.children.length > 0) {
    if (props.collapsed) {
      if (sidebarContext) {
        sidebarContext.isCollapsed.value = false
      }
      expandedKeys.value.add(item.key)
      if (!item.route) return
    } else {
      toggleExpand(item.key)
      if (!item.route) return
    }
  }
  emit('select', item.key, item)
  if (item.route && route.path !== item.route) {
    router.push(item.route).catch(() => {})
  }
}
</script>

<style scoped lang="scss">
.sidebar-nav-group {
  width: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;

  &__title {
    height: 20px;
    line-height: 20px;
    padding: 0 8px;
    margin-bottom: 4px;
    font-size: 12px;
    font-weight: 400;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--color-v1-text-disable);
    user-select: none;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    box-sizing: border-box;
  }

  &__divider {
    display: block;
    width: calc(100% - 20px);
    height: 1px;
    margin: 12px 10px;
    background-color: var(--color-v1-outline-var);
    flex-shrink: 0;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 4px;
    width: 100%;
    box-sizing: border-box;
  }

  &.is-collapsed {
    align-items: center;

    .sidebar-nav-group__list {
      align-items: center;
      gap: 6px;
    }
  }
}

.volur-nav-item {
  position: relative;
  width: 100%;
  margin: 0;
  height: 36px;
  border-radius: 12px;
  border: none;
  background: transparent;
  color: var(--color-v1-text-var);
  font-weight: 500;
  display: flex;
  align-items: center;
  padding: 0 12px;
  gap: 12px;
  cursor: pointer;
  user-select: none;
  text-decoration: none;
  box-sizing: border-box;
  outline: none;
  transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
              color 0.2s cubic-bezier(0.2, 0, 0, 1),
              width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
              padding 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover:not(.is-disabled):not(.is-active) {
    background-color: var(--color-v1-nav-item-hover);
    color: var(--color-v1-text);
  }

  &.is-active {
    background-color: var(--color-v1-nav-item-active);
    color: var(--color-v3-text);

    .volur-nav-item__icon-wrap {
      color: var(--color-v3-text);
    }
  }

  &.is-disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }

  &__icon-wrap {
    width: 18px;
    height: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: inherit;
  }

  &__icon {
    width: inherit;
    height: inherit;
  }

  &__label {
    flex: 1;
    min-width: 0;
    font-size: 14px;
    font-weight: inherit;
    color: inherit;
    text-align: left;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__badge {
    padding: 2px 6px;
    border-radius: 9999px;
    font-size: 11px;
    font-weight: 600;
    line-height: 1;
    background-color: var(--color-v1-primary-highest);
    color: #ffffff;
    flex-shrink: 0;
  }

  &__badge-dot {
    position: absolute;
    top: 6px;
    right: 6px;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: var(--color-v1-primary-highest);
  }

  &__arrow {
    flex-shrink: 0;
    transition: transform 0.2s ease;
    color: var(--color-v1-text-disable);

    &.is-expanded {
      transform: rotate(180deg);
    }
  }

  &--sub {
    width: 100%;
    padding-left: 28px;
    height: 36px;
    font-size: 13px;
  }

  &.is-collapsed {
    width: 36px;
    height: 36px;
    margin: 0 auto;
    padding: 0;
    justify-content: center;
    border-radius: 12px;
  }
}

.volur-nav-sublist {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  box-sizing: border-box;
}
</style>
