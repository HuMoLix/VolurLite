<template>
  <nav
    class="mobile-bottom-nav"
    :class="[currentTheme]"
    role="navigation"
    aria-label="Mobile Navigation"
  >
    <div class="bottom-nav-inner">
      <slot
        name="items"
        :items="computedNavItems"
        :active-key="currentValue"
        :select="handleItemSelect"
      >
        <button
          v-for="item in computedNavItems"
          :key="String(item.key)"
          type="button"
          class="nav-tab-btn"
          :class="{ 'is-active': currentValue === String(item.key) }"
          @click="handleItemSelect(item.key, item)"
        >
          <div class="nav-tab-pill">
            <span class="nav-tab-icon">
              <component :is="item.icon" v-if="typeof item.icon === 'function'" />
              <n-icon v-else-if="item.icon" :size="20">
                <component :is="item.icon" />
              </n-icon>
            </span>
          </div>
          <span class="nav-tab-label">{{ resolveLabel(item.label) }}</span>
        </button>
      </slot>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, type MenuOption } from 'naive-ui'
import {
  TrophyOutline,
  MegaphoneOutline
} from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import type { VBottomBarProps } from './types'

defineOptions({
  name: 'VBottomBar'
})

const props = withDefaults(defineProps<VBottomBarProps>(), {
  value: undefined,
  items: undefined,
  menuOptions: undefined,
  theme: undefined,
  showBottomAction: false,
  bottomActionText: '',
  bottomActionIcon: undefined,
  bottomActionRoute: ''
})

const emit = defineEmits<{
  select: [key: string, item: MenuOption]
  'bottom-action-click': [ev: MouseEvent]
}>()

const modelValue = defineModel<string>('value')
const { t } = useI18n()
const appStore = useAppStore()

const currentValue = computed<string | undefined>({
  get() {
    if (modelValue.value !== undefined) {
      return modelValue.value
    }
    return props.value
  },
  set(val: string | undefined) {
    if (val !== undefined) {
      if (modelValue.value !== undefined) {
        modelValue.value = val
      }
    }
  }
})

const currentTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const defaultNavItems = computed<MenuOption[]>(() => [
  {
    label: () => t('common.competitions'),
    key: 'competitions',
    icon: () => h(NIcon, { size: 20 }, { default: () => h(TrophyOutline) })
  },
  {
    label: () => t('common.announcements'),
    key: 'announcements',
    icon: () => h(NIcon, { size: 20 }, { default: () => h(MegaphoneOutline) })
  }
])

const computedNavItems = computed<MenuOption[]>(() => {
  if (props.menuOptions && props.menuOptions.length > 0) {
    return props.menuOptions
  }
  return defaultNavItems.value
})

function resolveLabel(label: unknown): string {
  if (typeof label === 'function') {
    const res = label()
    return typeof res === 'string' ? res : ''
  }
  return typeof label === 'string' ? label : ''
}

function handleItemSelect(key: string | number | undefined, item: MenuOption) {
  if (key !== undefined) {
    currentValue.value = String(key)
    emit('select', String(key), item)
  }
}
</script>

<style scoped lang="scss">
.mobile-bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 50;
  box-sizing: border-box;
  background-color: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border-top: 1px solid #dadce0;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.04);
  padding-bottom: env(safe-area-inset-bottom, 0px);
  user-select: none;

  .bottom-nav-inner {
    display: flex;
    align-items: center;
    justify-content: space-around;
    height: 56px;
    max-width: 600px;
    margin: 0 auto;
    padding: 0 12px;
  }

  .nav-tab-btn {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    height: 100%;
    padding: 4px 8px;
    background: transparent;
    border: none;
    outline: none;
    cursor: pointer;
    text-decoration: none;
    color: #5f6368;
    transition: color 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    -webkit-tap-highlight-color: transparent;

    .nav-tab-pill {
      display: flex;
      align-items: center;
      justify-content: center;
      min-width: 52px;
      height: 28px;
      padding: 0 12px;
      border-radius: 14px;
      background-color: transparent;
      transition: background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      .nav-tab-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
      }
    }

    .nav-tab-label {
      font-size: 11px;
      font-weight: 500;
      line-height: 1;
      letter-spacing: 0.1px;
    }

    &:hover {
      color: #202124;

      .nav-tab-pill {
        background-color: rgba(60, 64, 67, 0.06);
      }
    }

    &.is-active {
      color: #1a73e8;

      .nav-tab-pill {
        background-color: rgba(26, 115, 232, 0.12);
      }

      .nav-tab-label {
        font-weight: 600;
      }
    }
  }

  &.dark {
    background-color: rgba(14, 14, 14, 0.92);
    border-top-color: #3c4043;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.3);

    .nav-tab-btn {
      color: #9aa0a6;

      &:hover {
        color: #e8eaed;

        .nav-tab-pill {
          background-color: rgba(255, 255, 255, 0.08);
        }
      }

      &.is-active {
        color: #a8c7fa;

        .nav-tab-pill {
          background-color: rgba(168, 199, 250, 0.16);
        }

        .nav-tab-label {
          color: #a8c7fa;
        }
      }
    }
  }
}
</style>
