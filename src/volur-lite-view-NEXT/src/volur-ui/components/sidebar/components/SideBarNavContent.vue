<template>
  <div
    class="sidebar-nav-content"
    :class="{
      'is-collapsed': collapsed
    }"
  >
    <div
      v-if="$slots['primary-action'] || showPrimaryAction"
      class="sidebar-nav-content__action-wrap"
    >
      <slot name="primary-action" :collapsed="collapsed">
        <VTooltip
          placement="right"
          :content="primaryActionCollapsedText || primaryActionText || '新建'"
          :disabled="!collapsed"
        >
          <button
            type="button"
            class="primary-action-btn"
            :class="{ 'is-collapsed': collapsed }"
            @click="handlePrimaryActionClick"
          >
            <component
              :is="primaryActionIcon"
              v-if="primaryActionIcon"
              class="primary-action-btn__icon"
            />
            <svg
              v-else
              viewBox="0 0 24 24"
              width="20"
              height="20"
              stroke="currentColor"
              stroke-width="2.5"
              fill="none"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="primary-action-btn__icon"
            >
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span v-if="!collapsed" class="primary-action-btn__label">
              {{ primaryActionText || '新建' }}
            </span>
          </button>
        </VTooltip>
      </slot>
    </div>

    <slot name="menu-top" />

    <div class="sidebar-nav-content__groups">
      <slot
        name="menu"
        :collapsed="collapsed"
        :groups="groups"
        :current-value="currentValue"
      >
        <SideBarNavGroup
          v-for="(group, groupIndex) in groups"
          :key="group.key"
          :group="group"
          :group-index="groupIndex"
          :collapsed="collapsed"
          :current-value="currentValue"
          @select="handleSelect"
        />
      </slot>
    </div>

    <slot name="menu-bottom" />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { VTooltip } from '@/volur-ui/components/tooltip'
import SideBarNavGroup from './SideBarNavGroup.vue'
import type { SideBarNavContentProps, SidebarNavItem } from '../types'

defineOptions({
  name: 'SideBarNavContent'
})

const props = withDefaults(defineProps<SideBarNavContentProps>(), {
  collapsed: false,
  groups: () => [],
  currentValue: undefined,
  showPrimaryAction: false,
  primaryActionText: '',
  primaryActionCollapsedText: '',
  primaryActionIcon: undefined,
  primaryActionRoute: ''
})

const emit = defineEmits<{
  (e: 'select', key: string, item: SidebarNavItem): void
  (e: 'primary-action-click', ev: MouseEvent): void
}>()

const router = useRouter()

function handlePrimaryActionClick(ev: MouseEvent) {
  emit('primary-action-click', ev)
  if (props.primaryActionRoute) {
    router.push(props.primaryActionRoute).catch(() => {})
  }
}

function handleSelect(key: string, item: SidebarNavItem) {
  emit('select', key, item)
}
</script>

<style scoped lang="scss">
.sidebar-nav-content {
  width: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;

  &__action-wrap {
    padding: 8px 0 12px;
    width: 100%;
    display: flex;
    justify-content: center;
    box-sizing: border-box;
    flex-shrink: 0;
  }

  .primary-action-btn {
    width: 100%;
    height: 40px;
    border-radius: 12px;
    border: none;
    background-color: var(--color-v1-primary-highest, #1a73e8);
    color: var(--color-v1-on-primary-highest, #ffffff);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    letter-spacing: 0.02em;
    box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.2), 0 2px 4px 1px rgba(0, 0, 0, 0.1);
    transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
                box-shadow 0.2s cubic-bezier(0.2, 0, 0, 1),
                transform 0.15s ease,
                width 0.25s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      background-color: light-dark(#1765cc, #b6d0f8);
    }

    &:active {
      transform: scale(0.98);
    }

    &__icon {
      width: 18px;
      height: 18px;
      flex-shrink: 0;
    }

    &__label {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    &.is-collapsed {
      width: 44px;
      padding: 0;
      justify-content: center;
    }
  }

  &__groups {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 4px 0 16px;
    width: 100%;
    box-sizing: border-box;
  }

  &.is-collapsed {
    .sidebar-nav-content__action-wrap {
      padding: 8px 0 12px;
      justify-content: center;
    }

    .sidebar-nav-content__groups {
      gap: 0;
      align-items: center;
    }
  }
}
</style>
