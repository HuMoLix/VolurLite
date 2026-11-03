<template>
  <SideBarLayout
    ref="layoutRef"
    :collapsed="isCollapsed"
    :width="width"
    :collapsed-width="collapsedWidth"
    :bordered="bordered"
    :collapse-mode="collapseMode"
    :content-style="contentStyle"
    :sider-class="siderClass"
  >
    <template #header>
      <SideBarNavHeader
        :collapsed="isCollapsed"
        :show-logo="showLogo"
        :logo-route="logoRoute"
        :logo-expanded-src="logoExpandedSrc"
        :logo-collapsed-src="logoCollapsedSrc"
        :logo-alt="logoAlt"
        :show-toggle-trigger="showToggleTrigger"
        :theme="currentTheme"
        @logo-click="emit('logo-click', $event)"
        @toggle-collapse="toggleCollapse"
      >
        <template v-if="$slots.logo" #logo="slotProps">
          <slot name="logo" v-bind="slotProps" />
        </template>
      </SideBarNavHeader>
    </template>

    <SideBarNavContent
      :collapsed="isCollapsed"
      :groups="resolvedNavGroups"
      :current-value="currentValue"
      :accordion="accordion"
      :collapsed-icon-size="collapsedIconSize"
      :show-primary-action="showPrimaryAction"
      :primary-action-text="primaryActionText"
      :primary-action-collapsed-text="primaryActionCollapsedText"
      :primary-action-icon="primaryActionIcon"
      :primary-action-route="primaryActionRoute"
      @select="handleSelect"
      @primary-action-click="emit('primary-action-click', $event)"
    >
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
    </SideBarNavContent>

    <template #footer>
      <SideBarNavFooter
        ref="footerRef"
        :collapsed="isCollapsed"
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
        :theme="currentTheme"
        @search-click="emit('search-click', $event)"
        @search-select="(item) => emit('search-select', item)"
        @search-open="emit('search-open')"
        @search-close="emit('search-close')"
        @theme-toggle="handleThemeToggle"
        @notification-click="emit('notification-click', $event)"
        @notification-item-click="(item) => emit('notification-click', item as any)"
        @settings-click="emit('settings-click', $event)"
        @user-click="emit('user-click', $event)"
        @bottom-action-click="emit('bottom-action-click', $event)"
        @switch-account="(ev) => emit('switch-account', ev)"
        @logout="(ev) => emit('logout', ev)"
        @profile-click="(ev) => emit('profile-click', ev)"
        @user-card-open="emit('user-card-open')"
        @user-card-close="emit('user-card-close')"
        @notice-card-open="emit('notice-card-open')"
        @notice-card-close="emit('notice-card-close')"
        @setting-card-open="emit('setting-card-open')"
        @setting-card-close="emit('setting-card-close')"
        @setting-item-click="(key) => emit('setting-item-click', key)"
        @admin-click="(ev) => emit('admin-click', ev)"
      >
        <template v-if="$slots['action-box']" #action-box="slotProps">
          <slot name="action-box" v-bind="slotProps" />
        </template>
        <template v-if="$slots['user-profile']" #user-profile="slotProps">
          <slot name="user-profile" v-bind="slotProps" />
        </template>
        <template v-if="$slots['notice-card']" #notice-card="slotProps">
          <slot name="notice-card" v-bind="slotProps" />
        </template>
        <template v-if="$slots['setting-card']" #setting-card="slotProps">
          <slot name="setting-card" v-bind="slotProps" />
        </template>
        <template v-if="$slots['user-card']" #user-card="slotProps">
          <slot name="user-card" v-bind="slotProps" />
        </template>
        <template v-if="$slots['search-dialog']" #search-dialog="slotProps">
          <slot name="search-dialog" v-bind="slotProps" />
        </template>
        <template v-if="$slots.bottom" #bottom>
          <slot name="bottom" />
        </template>
      </SideBarNavFooter>
    </template>
  </SideBarLayout>

  <div v-if="$slots['bottom-nav-items']" class="sidebar-mobile-view">
    <slot name="bottom-nav-items" :groups="resolvedNavGroups" :value="currentValue" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, provide, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/modules/app'
import SideBarLayout from './layout/SideBarLayout.vue'
import SideBarNavHeader from './components/SideBarNavHeader.vue'
import SideBarNavContent from './components/SideBarNavContent.vue'
import SideBarNavFooter from './components/SideBarNavFooter.vue'
import { SIDEBAR_INJECTION_KEY } from './context'
import type { VSideBarProps, VSideBarEmits, SidebarNavItem, SidebarNavGroup } from './types'

defineOptions({
  name: 'VSideBar'
})

const props = withDefaults(defineProps<VSideBarProps>(), {
  width: 220,
  collapsedWidth: 74,
  collapsed: undefined,
  showTrigger: false,
  bordered: true,
  collapseMode: 'width',
  items: undefined,
  groups: undefined,
  menuGroups: undefined,
  value: undefined,
  accordion: false,
  collapsedIconSize: 20,
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

const emit = defineEmits<VSideBarEmits>()

const modelCollapsed = defineModel<boolean>('collapsed')
const modelValue = defineModel<string>('value')

const appStore = useAppStore()
const router = useRouter()
const route = useRoute()

const layoutRef = ref<InstanceType<typeof SideBarLayout> | null>(null)
const footerRef = ref<InstanceType<typeof SideBarNavFooter> | null>(null)
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

const defaultNavGroups: SidebarNavGroup[] = [
  {
    key: 'explore',
    titleKey: 'common.nav_group_explore',
    title: 'EXPLORE',
    items: [
      {
        key: 'competitions',
        route: '/competitions',
        titleKey: 'common.competitions',
        label: '赛事大厅',
        icon: 'TrophyOutline'
      },
      {
        key: 'challenges',
        route: '/challenges',
        titleKey: 'common.challenges',
        label: '题目训练',
        icon: 'RocketOutline'
      },
      {
        key: 'announcements',
        route: '/announcements',
        titleKey: 'common.announcements',
        label: '公告中心',
        icon: 'MegaphoneOutline'
      }
    ]
  },
  {
    key: 'learn',
    titleKey: 'common.nav_group_learn',
    title: 'LEARN',
    items: [
      {
        key: 'learning',
        route: '/learning',
        titleKey: 'common.learning',
        label: '学习中心',
        icon: 'SchoolOutline'
      }
    ]
  },
  {
    key: 'more',
    titleKey: 'common.nav_group_more',
    title: 'MORE',
    items: [
      {
        key: 'resources',
        route: '/resources',
        titleKey: 'common.resources',
        label: '资源中心',
        icon: 'CubeOutline'
      },
      {
        key: 'experimental',
        route: '/experimental',
        titleKey: 'common.experimental',
        label: '实验性功能',
        icon: 'BubbleChartOutlined'
      },
      {
        key: 'help',
        route: '/help',
        titleKey: 'common.help_center',
        label: '帮助中心',
        icon: 'BulbOutlined'
      }
    ]
  }
]

const resolvedNavGroups = computed<SidebarNavGroup[]>(() => {
  if (props.groups && props.groups.length > 0) {
    return props.groups
  }
  if (props.menuGroups && props.menuGroups.length > 0) {
    return props.menuGroups.map((g) => ({
      key: g.key,
      title: g.title,
      titleKey: g.titleKey,
      label: g.label,
      items: (g.items || g.options || []) as SidebarNavItem[]
    }))
  }
  if (props.items && props.items.length > 0) {
    return [
      {
        key: 'default',
        items: props.items
      }
    ]
  }
  return defaultNavGroups
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

const currentTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

function matchNavKey(items: SidebarNavItem[], path: string): string | undefined {
  for (const item of items) {
    if (item.children && item.children.length > 0) {
      const childMatch = matchNavKey(item.children, path)
      if (childMatch) {
        return childMatch
      }
    }
    if (item.route && (path === item.route || path.startsWith(`${item.route}/`))) {
      return item.key
    }
  }
  return undefined
}

const currentValue = computed<string | undefined>({
  get() {
    if (modelValue.value !== undefined) {
      return modelValue.value
    }
    if (props.value !== undefined) {
      return props.value
    }
    const currentPath = route.path
    for (const group of resolvedNavGroups.value) {
      const matched = matchNavKey(group.items, currentPath)
      if (matched) {
        return matched
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

function handleSelect(key: string, item: SidebarNavItem) {
  currentValue.value = key
  emit('select', key, item)
  if (item.route && route.path !== item.route) {
    router.push(item.route).catch(() => {})
  }
}

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
  if (isCollapsed.value) {
    emit('collapse')
  } else {
    emit('expand')
  }
}

function expand() {
  if (isCollapsed.value) {
    isCollapsed.value = false
    emit('expand')
  }
}

function collapse() {
  if (!isCollapsed.value) {
    isCollapsed.value = true
    emit('collapse')
  }
}

function scrollToTop(options?: ScrollToOptions) {
  layoutRef.value?.scrollToTop(options)
}

function handleThemeToggle() {
  appStore.toggleTheme()
  emit('theme-toggle')
}

function openSearch() {
  footerRef.value?.openSearch()
}

function closeSearch() {
  footerRef.value?.closeSearch()
}

provide(SIDEBAR_INJECTION_KEY, {
  isCollapsed,
  currentValue,
  theme: currentTheme,
  selectItem: handleSelect,
  toggleCollapse
})

defineExpose({
  isCollapsed,
  isDesktop,
  layoutRef,
  resolvedNavGroups,
  currentValue,
  scrollToTop,
  toggleCollapse,
  expand,
  collapse,
  openSearch,
  closeSearch
})
</script>

<style scoped lang="scss">
.sidebar-mobile-view {
  display: block;

  @media (min-width: 840px) {
    display: none;
  }
}
</style>
