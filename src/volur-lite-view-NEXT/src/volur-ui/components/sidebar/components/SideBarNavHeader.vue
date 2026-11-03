<template>
  <div
    class="sidebar-nav-header"
    :class="{
      'is-collapsed': collapsed
    }"
  >
    <div class="sidebar-nav-header__inner">
      <div
        v-if="showLogo"
        class="sidebar-nav-header__logo"
        :class="{ 'is-collapsed': collapsed && showToggleTrigger }"
        @click="handleLogoClick"
      >
        <slot name="logo" :collapsed="collapsed" :theme="theme">
          <img
            v-if="resolvedLogoSrc"
            :src="resolvedLogoSrc"
            :alt="logoAlt"
            :class="[
              collapsed && !showToggleTrigger ? 'brand-logo-collapsed' : 'brand-logo-expanded',
              { 'is-dark': isDarkMode }
            ]"
            :style="isDarkMode && (!collapsed || showToggleTrigger) ? { filter: 'brightness(0) invert(1)' } : undefined"
          />
        </slot>
      </div>

      <div v-else class="sidebar-nav-header__placeholder" />

      <VTooltip
        v-if="showToggleTrigger"
        placement="bottom"
        :content="collapsed ? resolveAriaLabel('expand') : resolveAriaLabel('collapse')"
      >
        <button
          type="button"
          class="sidebar-toggle-btn"
          :aria-label="collapsed ? resolveAriaLabel('expand') : resolveAriaLabel('collapse')"
          @click.stop="handleToggleClick"
        >
          <MenuOutline v-if="collapsed" class="sidebar-toggle-btn__icon" />
          <MenuOpenOutlined v-else class="sidebar-toggle-btn__icon" />
        </button>
      </VTooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MenuOutline } from '@vicons/ionicons5'
import { MenuOpenOutlined } from '@vicons/material'
import { useAppStore } from '@/store/modules/app'
import { VTooltip } from '@/volur-ui/components/tooltip'
import type { SideBarNavHeaderProps } from '../types'

defineOptions({
  name: 'SideBarNavHeader'
})

const props = withDefaults(defineProps<SideBarNavHeaderProps>(), {
  collapsed: false,
  showLogo: true,
  logoRoute: '/competitions',
  logoExpandedSrc: '',
  logoCollapsedSrc: '',
  logoAlt: 'Volur Lite',
  showToggleTrigger: true,
  theme: 'light'
})

const emit = defineEmits<{
  (e: 'logo-click', ev: MouseEvent): void
  (e: 'toggle-collapse'): void
}>()

const appStore = useAppStore()
const router = useRouter()
const { t, te } = useI18n()

const isDarkMode = computed<boolean>(() => {
  return props.theme === 'dark' || appStore.theme === 'dark'
})

const resolvedLogoSrc = computed<string>(() => {
  if (props.collapsed && !props.showToggleTrigger) {
    if (props.logoCollapsedSrc) return props.logoCollapsedSrc
    return isDarkMode.value ? '/r/static/svg/favicon-dark.svg' : '/r/static/svg/favicon.svg'
  }
  return props.logoExpandedSrc || '/r/static/svg/VolurCTFStudio.svg'
})

function resolveAriaLabel(action: 'expand' | 'collapse'): string {
  const key = action === 'expand' ? 'common.expand_sidebar' : 'common.collapse_sidebar'
  if (te(key)) {
    return t(key)
  }
  return action === 'expand' ? '展开侧边栏' : '收起侧边栏'
}

function handleLogoClick(ev: MouseEvent) {
  emit('logo-click', ev)
  if (props.logoRoute) {
    router.push(props.logoRoute).catch(() => {})
  }
}

function handleToggleClick() {
  emit('toggle-collapse')
}
</script>

<style scoped lang="scss">
.sidebar-nav-header {
  width: 100%;
  padding: 12px 0;
  flex-shrink: 0;
  box-sizing: border-box;

  &__inner {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    box-sizing: border-box;
    overflow: hidden;
  }

  &__logo {
    display: flex;
    align-items: center;
    cursor: pointer;
    user-select: none;
    overflow: hidden;
    min-width: 0;
    max-width: 200px;
    height: 38px;
    border-radius: 12px;
    opacity: 1;
    padding: 0 8px;
    box-sizing: border-box;
    transition: max-width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
                opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1),
                padding 0.25s cubic-bezier(0.4, 0, 0.2, 1),
                background-color 0.2s cubic-bezier(0.2, 0, 0, 1);

    &:hover {
      background-color: var(--color-v1-button-container-high);
    }

    &.is-collapsed {
      max-width: 0 !important;
      opacity: 0 !important;
      padding: 0 !important;
      pointer-events: none;
    }

    .brand-logo-expanded {
      height: 15px;
      width: auto;
      max-width: 160px;
      object-fit: contain;
      white-space: nowrap;
      transition: filter 0.2s ease;

      &.is-dark {
        filter: brightness(0) invert(1);
      }
    }

    .brand-logo-collapsed {
      width: 28px;
      height: 28px;
      object-fit: contain;
    }
  }

  &__placeholder {
    flex: 1;
  }

  .sidebar-toggle-btn {
    width: 38px;
    height: 38px;
    border-radius: 12px;
    border: none;
    background: transparent;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: var(--color-v1-text-var, light-dark(#3c4043, #bdc1c6));
    transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
                color 0.2s cubic-bezier(0.2, 0, 0, 1),
                margin 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    flex-shrink: 0;
    outline: none;

    &:hover {
      background-color: var(--color-v1-button-container-high);
      color: var(--color-v1-text, light-dark(#202124, #e3e3e3));
    }

    &:active {
      background-color: rgba(60, 64, 67, 0.12);
    }

    &__icon {
      width: 20px;
      height: 20px;
      display: block;
    }
  }

  &.is-collapsed {
    .sidebar-nav-header__inner {
      justify-content: center;

      .sidebar-toggle-btn {
        margin: 0 auto;
      }
    }
  }
}
</style>
