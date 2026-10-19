<template>
  <div
    class="volur-responsive-sidebar"
    :class="{ 'is-collapsed': isCollapsed }"
    :style="{ '--v-sidebar-width': sidebarWidth }"
  >
    <div class="sidebar-desktop-view">
      <VSideBar
        ref="vSideBarRef"
        v-model:collapsed="isCollapsed"
        v-model:value="currentValue"
        :width="width"
        :collapsed-width="collapsedWidth"
        :show-trigger="showTrigger"
        :bordered="bordered"
        :collapse-mode="collapseMode"
        :menu-options="resolvedMenuOptions"
        :accordion="accordion"
        :collapsed-icon-size="collapsedIconSize"
        :show-logo="showLogo"
        :logo-route="logoRoute"
        :logo-expanded-src="logoExpandedSrc"
        :logo-collapsed-src="logoCollapsedSrc"
        :logo-alt="logoAlt"
        :theme="theme"
        :show-toggle-trigger="showToggleTrigger"
        :show-primary-action="showPrimaryAction"
        :primary-action-text="primaryActionText"
        :primary-action-collapsed-text="primaryActionCollapsedText"
        :primary-action-icon="primaryActionIcon"
        :show-action-box="showActionBox"
        :show-notifications="showNotifications"
        :show-settings="showSettings"
        :show-search="showSearch"
        :show-theme-toggle="showThemeToggle"
        :show-user-profile="showUserProfile"
        :avatar-shape="avatarShape"
        :avatar-radius="avatarRadius"
        :avatar-ring="avatarRing"
        :notification-route="notificationRoute"
        :settingsRoute="settingsRoute"
        :admin-route="adminRoute"
        :feedback-route="feedbackRoute"
        :search-route="searchRoute"
        :search-placeholder="searchPlaceholder"
        :search-api="searchApi"
         :user-profile-route="userProfileRoute"
        :signin-route="signinRoute"
        :account-chooser-route="accountChooserRoute"
        :privacy-route="privacyRoute"
        :terms-route="termsRoute"
        :show-bottom-action="showBottomAction"
        :bottom-action-text="bottomActionText"
        :bottom-action-collapsed-text="bottomActionCollapsedText"
        :bottom-action-icon="bottomActionIcon"
        :bottom-action-route="bottomActionRoute"
        :sticky-scroll-threshold="stickyScrollThreshold"
        :content-style="contentStyle"
        :sider-class="siderClass"
        @select="handleSelect"
        @collapse="handleCollapse"
        @expand="handleExpand"
        @logo-click="emit('logo-click', $event)"
        @primary-action-click="emit('primary-action-click', $event)"
        @notification-click="emit('notification-click', $event)"
        @settings-click="emit('settings-click', $event)"
        @search-click="emit('search-click', $event)"
        @search-select="emit('search-select', $event)"
        @search-open="emit('search-open')"
        @search-close="emit('search-close')"
        @theme-toggle="emit('theme-toggle')"
        @user-click="emit('user-click', $event)"
        @switch-account="emit('switch-account', $event)"
        @logout="emit('logout', $event)"
        @profile-click="emit('profile-click', $event)"
        @user-card-open="emit('user-card-open')"
        @user-card-close="emit('user-card-close')"
        @notice-card-open="emit('notice-card-open')"
        @notice-card-close="emit('notice-card-close')"
        @setting-card-open="emit('setting-card-open')"
        @setting-card-close="emit('setting-card-close')"
        @setting-item-click="emit('setting-item-click', $event)"
        @admin-click="emit('admin-click', $event)"
        @bottom-action-click="emit('bottom-action-click', $event)"
      >
        <template v-if="$slots.logo" #logo="slotProps">
          <slot name="logo" v-bind="slotProps" />
        </template>
        <template v-if="$slots['primary-action']" #primary-action="slotProps">
          <slot name="primary-action" v-bind="slotProps" />
        </template>
        <template v-if="$slots['menu-top']" #menu-top>
          <slot name="menu-top" />
        </template>
        <template v-if="$slots.menu" #menu="slotProps">
          <slot name="menu" v-bind="slotProps" />
        </template>
        <template v-if="$slots['menu-bottom']" #menu-bottom>
          <slot name="menu-bottom" />
        </template>
        <template v-if="$slots['action-box']" #action-box="slotProps">
          <slot name="action-box" v-bind="slotProps" />
        </template>
        <template v-if="$slots['user-profile']" #user-profile="slotProps">
          <slot name="user-profile" v-bind="slotProps" />
        </template>
        <template v-if="$slots['user-card']" #user-card="slotProps">
          <slot name="user-card" v-bind="slotProps" />
        </template>
        <template v-if="$slots['notice-card']" #notice-card="slotProps">
          <slot name="notice-card" v-bind="slotProps" />
        </template>
        <template v-if="$slots['setting-card']" #setting-card="slotProps">
          <slot name="setting-card" v-bind="slotProps" />
        </template>
        <template v-if="$slots.bottom" #bottom="slotProps">
          <slot name="bottom" v-bind="slotProps" />
        </template>
        <template v-if="$slots['search-dialog']" #search-dialog="slotProps">
          <slot name="search-dialog" v-bind="slotProps" />
        </template>
      </VSideBar>
    </div>

    <div class="sidebar-mobile-view">
      <VBottomBar
        v-model:value="currentValue"
        :menu-options="resolvedMenuOptions"
        :theme="theme"
        :show-bottom-action="showBottomAction"
        :bottom-action-text="bottomActionText"
        :bottom-action-icon="bottomActionIcon"
        :bottom-action-route="bottomActionRoute"
        @select="handleSelect"
        @bottom-action-click="emit('bottom-action-click', $event)"
      >
        <template v-if="$slots['bottom-nav-items']" #items="slotProps">
          <slot name="bottom-nav-items" v-bind="slotProps" />
        </template>
      </VBottomBar>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, h } from 'vue'
import type { Component, VNodeChild } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon, type MenuOption } from 'naive-ui'
import { TrophyOutline, MegaphoneOutline } from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import type { CompetitionSummaryVO } from '@/types/competition'
import VSideBar from './VSideBar.vue'
import VBottomBar from './VBottomBar.vue'
import type { SideBarProps, SidebarNavItem } from './types'

defineOptions({
  name: 'SideBar'
})

const props = withDefaults(defineProps<SideBarProps>(), {
  width: 256,
  collapsedWidth: 74,
  collapsed: undefined,
  showTrigger: false,
  bordered: true,
  collapseMode: 'width',
  items: undefined,
  menuOptions: undefined,
  value: undefined,
  accordion: false,
  collapsedIconSize: 22,
  showLogo: true,
  logoRoute: '/competitions',
  logoExpandedSrc: '',
  logoCollapsedSrc: '',
  logoAlt: 'Volur Lite',
  theme: undefined,
  showToggleTrigger: true,
  showPrimaryAction: false,
  primaryActionText: '',
  primaryActionCollapsedText: '',
  primaryActionIcon: undefined,
  primaryActionRoute: '',
  showActionBox: true,
  showNotifications: true,
  showSettings: true,
  showSearch: true,
  showThemeToggle: true,
  showUserProfile: true,
  avatarShape: 'circle',
  avatarRadius: undefined,
  avatarRing: true,
  notificationRoute: '/notifications',
  settingsRoute: '/settings',
  adminRoute: '/admin/dashboard',
  feedbackRoute: '',
  searchRoute: '',
  searchPlaceholder: '',
  searchApi: undefined,
  userProfileRoute: '/user/profile',
  signinRoute: '/v1/signin/identifier',
  accountChooserRoute: '/v1/signin/accountchooser',
  privacyRoute: '/accounts/privacy',
  termsRoute: '/accounts/terms',
  showBottomAction: false,
  bottomActionText: '',
  bottomActionCollapsedText: '',
  bottomActionIcon: undefined,
  bottomActionRoute: '',
  stickyScrollThreshold: 10,
  contentStyle: '',
  siderClass: '',
  breakpoint: 840
})

const emit = defineEmits<{
  select: [key: string, item: MenuOption]
  collapse: []
  expand: []
  'logo-click': [ev: MouseEvent]
  'primary-action-click': [ev: MouseEvent]
  'notification-click': [ev: MouseEvent]
  'settings-click': [ev: MouseEvent]
  'search-click': [ev: MouseEvent]
  'search-select': [item: CompetitionSummaryVO]
  'search-open': []
  'search-close': []
  'theme-toggle': []
  'user-click': [ev: MouseEvent]
  'switch-account': [ev: MouseEvent]
  logout: [ev: MouseEvent]
  'profile-click': [ev: MouseEvent]
  'user-card-open': []
  'user-card-close': []
  'notice-card-open': []
  'notice-card-close': []
  'setting-card-open': []
  'setting-card-close': []
  'setting-item-click': [key: string]
  'admin-click': [ev: MouseEvent]
  'bottom-action-click': [ev: MouseEvent]
}>()

const modelCollapsed = defineModel<boolean>('collapsed')
const modelValue = defineModel<string>('value')
const appStore = useAppStore()
const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const defaultNavItems: SidebarNavItem[] = [
  {
    key: 'competitions',
    route: '/competitions',
    titleKey: 'common.competitions',
    label: '赛事大厅',
    icon: 'TrophyOutline'
  },
  // {
  //   key: 'announcements',
  //   route: '/announcements',
  //   titleKey: 'common.announcements',
  //   label: '公告中心',
  //   icon: 'MegaphoneOutline'
  // }
]

const iconRegistry: Record<string, Component> = {
  TrophyOutline,
  MegaphoneOutline
}

function resolveItemIcon(icon?: Component | string | (() => VNodeChild)): (() => VNodeChild) | undefined {
  if (!icon) {
    return undefined
  }
  if (typeof icon === 'function') {
    return icon as () => VNodeChild
  }
  const comp = typeof icon === 'string' ? iconRegistry[icon] : icon
  if (!comp) {
    return undefined
  }
  return () => h(NIcon, null, { default: () => h(comp) })
}

function transformItemsToOptions(navItems: SidebarNavItem[]): MenuOption[] {
  return navItems.map((item) => {
    const option: MenuOption & { route?: string } = {
      key: item.key,
      label: () => (item.titleKey ? t(item.titleKey) : (typeof item.label === 'function' ? item.label() : item.label || '')),
      icon: resolveItemIcon(item.icon),
      route: item.route
    }
    if (item.children && item.children.length > 0) {
      option.children = transformItemsToOptions(item.children)
    }
    return option
  })
}

const resolvedMenuOptions = computed<MenuOption[]>(() => {
  if (props.menuOptions && props.menuOptions.length > 0) {
    return props.menuOptions
  }
  const source = props.items && props.items.length > 0 ? props.items : defaultNavItems
  return transformItemsToOptions(source)
})

const keyRouteMap = computed<Map<string, string>>(() => {
  const map = new Map<string, string>()
  function traverse(list: SidebarNavItem[]) {
    for (const item of list) {
      if (item.route) {
        map.set(item.key, item.route)
      }
      if (item.children) {
        traverse(item.children)
      }
    }
  }
  const source = props.items && props.items.length > 0 ? props.items : defaultNavItems
  traverse(source)
  return map
})

const isCollapsed = computed<boolean>({
  get() {
    if (modelCollapsed.value !== undefined) {
      return modelCollapsed.value
    }
    if (props.collapsed !== undefined) {
      return props.collapsed
    }
    return appStore.isSidebarCollapsed
  },
  set(val: boolean) {
    if (modelCollapsed.value !== undefined) {
      modelCollapsed.value = val
    }
    appStore.isSidebarCollapsed = val
  }
})

const sidebarWidth = computed<string>(() => {
  const current = isCollapsed.value ? props.collapsedWidth : props.width
  return typeof current === 'number' ? `${current}px` : String(current)
})

const currentValue = computed<string | undefined>({
  get() {
    if (modelValue.value !== undefined) {
      return modelValue.value
    }
    if (props.value !== undefined) {
      return props.value
    }
    const currentPath = route.path
    for (const [key, targetRoute] of keyRouteMap.value.entries()) {
      if (currentPath === targetRoute || currentPath.startsWith(`${targetRoute}/`)) {
        return key
      }
    }
    if (currentPath.startsWith('/c/')) {
      return 'competitions'
    }
    return undefined
  },
  set(val: string | undefined) {
    if (val !== undefined && modelValue.value !== undefined) {
      modelValue.value = val
    }
  }
})

const vSideBarRef = ref<InstanceType<typeof VSideBar> | null>(null)
const isDesktop = ref<boolean>(typeof window !== 'undefined' ? window.innerWidth >= props.breakpoint : true)

let mediaQueryList: MediaQueryList | null = null

function updateMediaMatch(e: MediaQueryListEvent | MediaQueryList) {
  isDesktop.value = e.matches
}

onMounted(() => {
  if (typeof window !== 'undefined') {
    mediaQueryList = window.matchMedia(`(min-width: ${props.breakpoint}px)`)
    isDesktop.value = mediaQueryList.matches
    mediaQueryList.addEventListener('change', updateMediaMatch)
  }
})

onBeforeUnmount(() => {
  if (mediaQueryList) {
    mediaQueryList.removeEventListener('change', updateMediaMatch)
  }
})

function handleSelect(key: string, item: MenuOption) {
  currentValue.value = key
  emit('select', key, item)
  const targetRoute = (item as MenuOption & { route?: string })?.route || keyRouteMap.value.get(key)
  if (targetRoute && route.path !== targetRoute) {
    router.push(targetRoute).catch(() => {})
  }
}

function scrollToTop(options?: ScrollToOptions) {
  vSideBarRef.value?.scrollToTop(options)
}

function toggleCollapse() {
  vSideBarRef.value?.toggleCollapse()
}

function handleCollapse() {
  isCollapsed.value = true
  if (modelCollapsed.value !== undefined) {
    modelCollapsed.value = true
  }
  appStore.isSidebarCollapsed = true
  emit('collapse')
}

function handleExpand() {
  isCollapsed.value = false
  if (modelCollapsed.value !== undefined) {
    modelCollapsed.value = false
  }
  appStore.isSidebarCollapsed = false
  emit('expand')
}

function expand() {
  vSideBarRef.value?.expand()
}

function collapse() {
  vSideBarRef.value?.collapse()
}

function openSearch() {
  vSideBarRef.value?.openSearch()
}

function closeSearch() {
  vSideBarRef.value?.closeSearch()
}

defineExpose({
  isCollapsed,
  isDesktop,
  vSideBarRef,
  resolvedMenuOptions,
  keyRouteMap,
  scrollToTop,
  toggleCollapse,
  expand,
  collapse,
  openSearch,
  closeSearch
})
</script>

<style scoped lang="scss">
.volur-responsive-sidebar {
  display: contents;

  @media (min-width: 840px) {
    display: block;
    flex-shrink: 0;
    width: var(--v-sidebar-width, 256px);
    transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .sidebar-desktop-view {
    display: none;
    position: fixed;
    top: 0;
    bottom: 0;
    left: 0;
    height: 100vh;
    height: 100dvh;
    width: var(--v-sidebar-width, 256px);
    flex-shrink: 0;
    z-index: 30;
    transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);

    @media (min-width: 840px) {
      display: flex;
      flex-direction: column;
    }
  }

  .sidebar-mobile-view {
    display: block;

    @media (min-width: 840px) {
      display: none;
    }
  }
}
</style>
