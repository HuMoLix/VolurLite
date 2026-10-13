<template>
  <div class="app-layout h-screen max-h-screen flex overflow-hidden bg-white dark:bg-[#191919] transition-colors duration-300">
    <slot name="sidebar" :collapsed="isCollapsed" :toggle-collapse="toggleCollapse">
      <SideBar
        v-if="showSidebar"
        ref="sideBarRef"
        v-model:collapsed="isCollapsed"
        v-model:value="currentValue"
        :width="width"
        :collapsed-width="collapsedWidth"
        :show-trigger="showTrigger"
        :bordered="bordered"
        :collapse-mode="collapseMode"
        :items="items"
        :menu-options="menuOptions"
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
        :primary-action-route="primaryActionRoute"
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
        :settings-route="settingsRoute"
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
        :breakpoint="breakpoint"
        @select="handleSelect"
        @collapse="emit('collapse')"
        @expand="emit('expand')"
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
        <template v-if="$slots.bottom" #bottom="slotProps">
          <slot name="bottom" v-bind="slotProps" />
        </template>
        <template v-if="$slots['search-dialog']" #search-dialog="slotProps">
          <slot name="search-dialog" v-bind="slotProps" />
        </template>
        <template v-if="$slots['bottom-nav-items']" #bottom-nav-items="slotProps">
          <slot name="bottom-nav-items" v-bind="slotProps" />
        </template>
      </SideBar>
    </slot>

    <div
      ref="appMainRef"
      class="app-main flex-1 flex flex-col min-w-0 h-full overflow-y-auto pb-16 md:pb-0"
      :class="mainClass"
      :style="mainStyle"
    >
      <slot name="header" />
      <main class="app-content flex-1 flex flex-col min-w-0 max-w-[100rem] w-full mx-auto">
        <slot>
          <router-view />
        </slot>
      </main>
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { Component, CSSProperties } from 'vue'
import type { MenuOption } from 'naive-ui'
import SideBar from '@/components/sidebar/SideBar.vue'
import type { SidebarNavItem } from '@/components/sidebar/types'
import { useAppStore } from '@/store/modules/app'
import type { CompetitionSummaryVO } from '@/types/competition'

defineOptions({
  name: 'AppLayout'
})

export interface AppLayoutProps {
  width?: number | string
  collapsedWidth?: number | string
  collapsed?: boolean
  showTrigger?: boolean | 'bar' | 'arrow-circle'
  bordered?: boolean
  collapseMode?: 'width' | 'transform'
  items?: SidebarNavItem[]
  menuOptions?: MenuOption[]
  value?: string
  accordion?: boolean
  collapsedIconSize?: number
  showLogo?: boolean
  logoRoute?: string | null
  logoExpandedSrc?: string
  logoCollapsedSrc?: string
  logoAlt?: string
  theme?: 'light' | 'dark'
  showToggleTrigger?: boolean
  showPrimaryAction?: boolean
  primaryActionText?: string
  primaryActionCollapsedText?: string
  primaryActionIcon?: Component
  primaryActionRoute?: string
  showActionBox?: boolean
  showNotifications?: boolean
  showSettings?: boolean
  showSearch?: boolean
  showThemeToggle?: boolean
  showUserProfile?: boolean
  avatarShape?: 'square' | 'circle'
  avatarRadius?: number | string
  avatarRing?: boolean
  notificationRoute?: string
  settingsRoute?: string
  searchRoute?: string
  searchPlaceholder?: string
  searchApi?: (keyword: string) => Promise<any>
  userProfileRoute?: string
  signinRoute?: string
  accountChooserRoute?: string
  privacyRoute?: string
  termsRoute?: string
  showBottomAction?: boolean
  bottomActionText?: string
  bottomActionCollapsedText?: string
  bottomActionIcon?: Component
  bottomActionRoute?: string
  stickyScrollThreshold?: number
  contentStyle?: string | CSSProperties
  siderClass?: string
  breakpoint?: number
  showSidebar?: boolean
  mainClass?: string
  mainStyle?: string | CSSProperties
}

const props = withDefaults(defineProps<AppLayoutProps>(), {
  showSidebar: true,
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
  breakpoint: 840,
  mainClass: '',
  mainStyle: undefined
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
  'bottom-action-click': [ev: MouseEvent]
}>()

const modelCollapsed = defineModel<boolean>('collapsed')
const modelValue = defineModel<string>('value')

const appStore = useAppStore()
const route = useRoute()

const sideBarRef = ref<InstanceType<typeof SideBar> | null>(null)
const appMainRef = ref<HTMLElement | null>(null)

watch(
  () => route?.path,
  () => {
    appMainRef.value?.scrollTo({ top: 0, behavior: 'auto' })
  }
)

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

const currentValue = computed<string | undefined>({
  get() {
    if (modelValue.value !== undefined) {
      return modelValue.value
    }
    return props.value
  },
  set(val: string | undefined) {
    if (val !== undefined && modelValue.value !== undefined) {
      modelValue.value = val
    }
  }
})

function handleSelect(key: string, item: MenuOption) {
  emit('select', key, item)
  if (modelValue.value !== undefined) {
    modelValue.value = key
  }
}

function toggleCollapse() {
  sideBarRef.value?.toggleCollapse()
}

function expand() {
  sideBarRef.value?.expand()
}

function collapse() {
  sideBarRef.value?.collapse()
}

function openSearch() {
  sideBarRef.value?.openSearch()
}

function closeSearch() {
  sideBarRef.value?.closeSearch()
}

function scrollToTop(options?: ScrollToOptions) {
  appMainRef.value?.scrollTo(options ?? { top: 0, behavior: 'smooth' })
  sideBarRef.value?.scrollToTop(options)
}

defineExpose({
  sideBarRef,
  appMainRef,
  isCollapsed,
  toggleCollapse,
  expand,
  collapse,
  openSearch,
  closeSearch,
  scrollToTop
})
</script>

<style scoped lang="scss">
.app-layout {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  max-height: 100vh;
  max-height: 100dvh;
  overflow: hidden;
  background-color: #fff;

  html.dark & {
    background-color: #191919;
  }
}

.app-main {
  position: relative;
  height: 100%;
  max-height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior-y: contain;
  -webkit-overflow-scrolling: touch;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: var(--v-scrollbar-thumb) transparent;
  background: #fff;
  transition: background-color 0.3s ease;

  &::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background-color: var(--v-scrollbar-thumb);
    border-radius: 9999px;
    border: 2px solid transparent;
    background-clip: padding-box;

    &:hover {
      background-color: var(--v-scrollbar-thumb-hover);
    }
  }

  &::-webkit-scrollbar-corner {
    background: transparent;
  }

  html.dark & {
    background-color: #191919;
  }
}

.app-content {
  position: relative;
}

main {
  max-width: 100rem;
  width: 100%;
  margin: 0 auto;
}
</style>
