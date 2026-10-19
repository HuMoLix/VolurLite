<template>
  <n-layout-sider
    ref="siderRef"
    :bordered="bordered"
    :collapse-mode="collapseMode"
    :collapsed-width="Number(collapsedWidth)"
    :width="Number(width)"
    :collapsed="isCollapsed"
    :show-trigger="showTrigger"
    :content-style="contentStyle"
    :class="['v-sidebar', { 'is-collapsed': isCollapsed }, siderClass]"
    @update:collapsed="handleUpdateCollapsed"
    @collapse="handleCollapse"
    @expand="handleExpand"
    @scroll="handleSidebarScroll"
  >
    <div
      class="sidebar-content-wrapper"
      @wheel="handleSidebarWheel"
    >
      <div
        class="sidebar-header"
        :class="{ 'is-sticky': isSticky, 'is-collapsed': isCollapsed }"
      >
        <div class="header-inner">
          <div
            v-if="showLogo"
            class="logo-container"
            :class="{ 'is-collapsed': isCollapsed && showToggleTrigger }"
            @click="handleLogoClick"
          >
            <slot name="logo" :collapsed="isCollapsed" :theme="currentTheme">
              <img
                :src="isCollapsed && !showToggleTrigger ? collapsedLogoSrc : expandedLogoSrc"
                :alt="logoAlt"
                :class="isCollapsed && !showToggleTrigger ? 'brand-logo-collapsed' : 'brand-logo-expanded'"
              />
            </slot>
          </div>
          <div v-else class="brand-placeholder"></div>

          <VTooltip
            v-if="showToggleTrigger"
            :placement="isCollapsed ? 'right' : 'bottom'"
            :content="isCollapsed ? t('common.expand_sidebar') : t('common.collapse_sidebar')"
          >
            <button
              type="button"
              class="sidebar-toggle-btn"
              :aria-label="isCollapsed ? t('common.expand_sidebar') : t('common.collapse_sidebar')"
              @click.stop="toggleCollapse"
            >
              <n-icon :size="20">
                <MenuOutline v-if="isCollapsed" />
                <MenuOpenOutlined v-else />
              </n-icon>
            </button>
          </VTooltip>
        </div>
      </div>

      <div
        v-if="$slots['primary-action'] || showPrimaryAction"
        class="sidebar-primary-action-wrap"
      >
        <slot name="primary-action" :collapsed="isCollapsed">
          <VTooltip
            :disabled="!isCollapsed"
            placement="right"
            :content="primaryActionCollapsedText || primaryActionText || t('common.create_new')"
          >
            <button
              type="button"
              class="primary-action-btn"
              :class="{ 'is-collapsed': isCollapsed }"
              @click="handlePrimaryActionClick"
            >
              <n-icon :size="isCollapsed ? 22 : 18" class="primary-action-icon">
                <component :is="primaryActionIconComponent" />
              </n-icon>
              <span class="action-btn-label">
                {{ primaryActionText || t('common.create_new') }}
              </span>
            </button>
          </VTooltip>
        </slot>
      </div>

      <div
        ref="navContainerRef"
        class="sidebar-nav-container"
        @scroll="handleSidebarScroll"
      >
        <slot name="menu-top" />

        <slot
          name="menu"
          :collapsed="isCollapsed"
          :options="computedMenuOptions"
          :value="currentValue"
        >
          <n-menu
            :collapsed="isCollapsed"
            :collapsed-width="Number(collapsedWidth)"
            :collapsed-icon-size="collapsedIconSize"
            :options="computedMenuOptions"
            :value="currentValue"
            :accordion="accordion"
            class="v-menu"
            @update:value="handleMenuSelect"
          />
        </slot>

        <slot name="menu-bottom" />
      </div>

      <div
        class="sidebar-bottom-area"
        :class="{ 'is-collapsed': isCollapsed }"
      >
        <div
          v-if="showActionBox"
          class="sidebar-action-box"
          :class="{ 'is-collapsed': isCollapsed }"
        >
          <slot name="action-box" :collapsed="isCollapsed">
            <transition name="dock-fade" mode="out-in">
              <div v-if="!isCollapsed" key="action-expanded" class="action-box-expanded">
                <VTooltip v-if="showSearch" placement="top" :content="t('common.search')">
                  <button
                    type="button"
                    class="action-icon-btn"
                    :aria-label="t('common.search')"
                    @click="handleSearchClick"
                  >
                    <n-icon :size="18">
                      <SearchOutline />
                    </n-icon>
                  </button>
                </VTooltip>

                <VTooltip
                  v-if="showBottomAction"
                  placement="top"
                  :content="bottomActionText || t('common.back', '返回')"
                >
                  <button
                    type="button"
                    class="action-icon-btn"
                    :aria-label="bottomActionText || t('common.back', '返回')"
                    @click="handleBottomActionClick"
                  >
                    <n-icon :size="18">
                      <component :is="bottomActionIcon || ArrowBackOutline" />
                    </n-icon>
                  </button>
                </VTooltip>

                <n-popover
                  v-if="showNotifications"
                  v-model:show="isNoticePopoverShow"
                  trigger="click"
                  placement="top"
                  :show-arrow="false"
                  raw
                  style="box-shadow: none;"
                >
                  <template #trigger>
                    <VTooltip
                      placement="top"
                      :disabled="isNoticePopoverShow"
                      :content="t('common.notifications')"
                    >
                      <button
                        type="button"
                        class="action-icon-btn"
                        :class="{ 'is-active': isNoticePopoverShow }"
                        :aria-label="t('common.notifications')"
                        @click="handleNotificationClick"
                      >
                        <n-badge :value="unreadCount" :max="99" :show="unreadCount > 0" :offset="[-2, 2]">
                          <n-icon :size="18">
                            <NotificationsOutline />
                          </n-icon>
                        </n-badge>
                      </button>
                    </VTooltip>
                  </template>

                  <slot
                    name="notice-card"
                    :close="closeNoticePopover"
                  >
                    <VNotice
                      :theme="currentTheme"
                      :notification-route="notificationRoute"
                      :signin-route="signinRoute"
                      @close="closeNoticePopover"
                    />
                  </slot>
                </n-popover>

                <n-popover
                  v-if="showSettings"
                  v-model:show="isSettingPopoverShow"
                  trigger="click"
                  placement="top"
                  :show-arrow="false"
                  raw
                  style="box-shadow: none;"
                  display-directive="show"
                >
                  <template #trigger>
                    <VTooltip
                      placement="top"
                      :disabled="isSettingPopoverShow"
                      :content="t('common.settings')"
                    >
                      <button
                        type="button"
                        class="action-icon-btn"
                        :class="{ 'is-active': isSettingPopoverShow }"
                        :aria-label="t('common.settings')"
                        @click="handleSettingsClick"
                      >
                        <n-icon :size="18">
                          <SettingsOutline />
                        </n-icon>
                      </button>
                    </VTooltip>
                  </template>

                  <slot
                    name="setting-card"
                    :close="closeSettingPopover"
                  >
                    <VSettingCard
                      :theme="currentTheme"
                      :user-settings-route="settingsRoute"
                      :admin-route="adminRoute"
                      :feedback-route="feedbackRoute"
                      :terms-route="termsRoute"
                      :privacy-route="privacyRoute"
                      @close="closeSettingPopover"
                      @item-click="(key) => emit('setting-item-click', key)"
                      @admin-click="(ev) => emit('admin-click', ev)"
                    />
                  </slot>
                </n-popover>

                <VTooltip
                  v-if="showThemeToggle"
                  placement="top"
                  :content="currentTheme === 'dark' ? t('common.theme_light') : t('common.theme_dark')"
                >
                  <button
                    type="button"
                    class="action-icon-btn"
                    :aria-label="currentTheme === 'dark' ? t('common.theme_light') : t('common.theme_dark')"
                    @click="handleThemeToggle"
                  >
                    <n-icon :size="18">
                      <SunnyOutline v-if="currentTheme === 'dark'" />
                      <MoonOutline v-else />
                    </n-icon>
                  </button>
                </VTooltip>
              </div>

              <div v-else key="action-collapsed" class="action-box-collapsed">
                <n-popover
                  v-model:show="isAppsPopoverShow"
                  trigger="hover"
                  placement="right"
                  :show-arrow="false"
                  raw
                  style="box-shadow: none;"
                >
                  <template #trigger>
                    <button
                      type="button"
                      class="action-icon-btn apps-btn"
                      :class="{ 'is-active': isAppsPopoverShow || isCollapsedNoticePopoverShow || isCollapsedSettingPopoverShow }"
                      :aria-label="t('common.tools', '应用与工具')"
                    >
                      <n-badge :value="unreadCount" :max="99" :show="unreadCount > 0" :offset="[-2, 2]">
                        <n-icon :size="18">
                          <AppsSharp />
                        </n-icon>
                      </n-badge>
                    </button>
                  </template>
                  <div class="dock-apps-popover" :class="currentTheme">
                    <VTooltip v-if="showSearch" placement="right" :content="t('common.search')">
                      <button
                        type="button"
                        class="dock-tool-btn"
                        :aria-label="t('common.search')"
                        @click="handleCollapsedSearchClick"
                      >
                        <n-icon :size="18">
                          <SearchOutline />
                        </n-icon>
                      </button>
                    </VTooltip>

                    <VTooltip
                      v-if="showBottomAction"
                      placement="right"
                      :content="bottomActionCollapsedText || bottomActionText || t('common.back', '返回')"
                    >
                      <button
                        type="button"
                        class="dock-tool-btn"
                        :aria-label="bottomActionCollapsedText || bottomActionText || t('common.back', '返回')"
                        @click="handleBottomActionClick"
                      >
                        <n-icon :size="18">
                          <component :is="bottomActionIcon || ArrowBackOutline" />
                        </n-icon>
                      </button>
                    </VTooltip>

                    <n-popover
                      v-if="showNotifications"
                      v-model:show="isCollapsedNoticePopoverShow"
                      trigger="click"
                      placement="right-end"
                      :show-arrow="false"
                      raw
                      style="box-shadow: none;"
                    >
                      <template #trigger>
                        <VTooltip
                          placement="right"
                          :disabled="isCollapsedNoticePopoverShow"
                          :content="t('common.notifications')"
                        >
                          <button
                            type="button"
                            class="dock-tool-btn"
                            :class="{ 'is-active': isCollapsedNoticePopoverShow }"
                            :aria-label="t('common.notifications')"
                            @click="handleCollapsedNotificationClick"
                          >
                            <n-badge :value="unreadCount" :max="99" :show="unreadCount > 0" :offset="[-2, 2]">
                              <n-icon :size="18">
                                <NotificationsOutline />
                              </n-icon>
                            </n-badge>
                          </button>
                        </VTooltip>
                      </template>

                      <slot
                        name="notice-card"
                        :close="closeNoticePopover"
                      >
                        <VNotice
                          :theme="currentTheme"
                          :notification-route="notificationRoute"
                          :signin-route="signinRoute"
                          @close="closeNoticePopover"
                        />
                      </slot>
                    </n-popover>

                    <n-popover
                      v-if="showSettings"
                      v-model:show="isCollapsedSettingPopoverShow"
                      trigger="click"
                      placement="right-end"
                      :show-arrow="false"
                      raw
                      style="box-shadow: none;"
                      display-directive="show"
                    >
                      <template #trigger>
                        <VTooltip
                          placement="right"
                          :disabled="isCollapsedSettingPopoverShow"
                          :content="t('common.settings')"
                        >
                          <button
                            type="button"
                            class="dock-tool-btn"
                            :class="{ 'is-active': isCollapsedSettingPopoverShow }"
                            :aria-label="t('common.settings')"
                            @click="handleCollapsedSettingsClick"
                          >
                            <n-icon :size="18">
                              <SettingsOutline />
                            </n-icon>
                          </button>
                        </VTooltip>
                      </template>

                      <slot
                        name="setting-card"
                        :close="closeSettingPopover"
                      >
                        <VSettingCard
                          :theme="currentTheme"
                          :user-settings-route="settingsRoute"
                          :admin-route="adminRoute"
                          :feedback-route="feedbackRoute"
                          :terms-route="termsRoute"
                          :privacy-route="privacyRoute"
                          @close="closeSettingPopover"
                          @item-click="(key) => emit('setting-item-click', key)"
                          @admin-click="(ev) => emit('admin-click', ev)"
                        />
                      </slot>
                    </n-popover>

                    <VTooltip
                      v-if="showThemeToggle"
                      placement="right"
                      :content="currentTheme === 'dark' ? t('common.theme_light') : t('common.theme_dark')"
                    >
                      <button
                        type="button"
                        class="dock-tool-btn"
                        :aria-label="currentTheme === 'dark' ? t('common.theme_light') : t('common.theme_dark')"
                        @click="handleThemeToggle"
                      >
                        <n-icon :size="18">
                          <SunnyOutline v-if="currentTheme === 'dark'" />
                          <MoonOutline v-else />
                        </n-icon>
                      </button>
                    </VTooltip>
                  </div>
                </n-popover>
              </div>
            </transition>
          </slot>
        </div>

        <div
          v-if="showUserProfile"
          class="sidebar-user-box"
          :class="{ 'is-collapsed': isCollapsed }"
        >
          <slot
            name="user-profile"
            :collapsed="isCollapsed"
            :is-logged-in="isLoggedIn"
            :user="currentUser"
          >
            <n-popover
              v-model:show="isUserCardPopoverShow"
              :disabled="!isLoggedIn"
              trigger="click"
              :placement="isCollapsed ? 'top-start' : 'top'"
              :show-arrow="false"
              raw
              style="box-shadow: none;"
            >
              <template #trigger>
                <VTooltip
                  v-model:show="isUserTooltipShow"
                  :tooltip-class="['user-card-tooltip', { 'is-collapsed': isCollapsed }]"
                  :disabled="isUserCardPopoverShow"
                  :placement="isCollapsed ? 'right' : 'top'"
                  :width="isCollapsed ? undefined : userCardWidth"
                  :tooltip-style="userTooltipStyle"
                  :nowrap="isCollapsed"
                >
                  <template #trigger>
                    <div
                      ref="userCardRef"
                      class="user-card"
                      :class="{ 'is-collapsed': isCollapsed, 'is-active': isUserCardPopoverShow }"
                      :aria-label="userTooltipText"
                      @click="handleUserClick"
                    >
                      <div class="user-avatar-wrap">
                        <VAvatar
                          :src="isLoggedIn ? avatarUrl : ''"
                          :text="isLoggedIn ? userInitial : ''"
                          :size="isCollapsed ? 26 : 32"
                          :shape="avatarShape"
                          :radius="avatarRadius"
                          :ring="avatarRing"
                          :theme="currentTheme"
                        />
                      </div>

                      <div class="user-info-text">
                        <span class="user-name">
                          {{ isLoggedIn ? displayName : t('common.signin') }}
                        </span>
                        <span v-if="isLoggedIn && userEmail" class="user-desc">
                          {{ userEmail }}
                        </span>
                      </div>
                    </div>
                  </template>
                  <div class="user-tooltip-box">
                    <template v-if="isLoggedIn">
                      <div class="user-tooltip-line user-tooltip-main">
                        <span class="user-tooltip-prefix">{{ t('common.volur_account', 'Volur 账号：') }}</span>
                        <span class="user-tooltip-name">{{ displayName }}</span>
                      </div>
                      <div v-if="userName && userName !== displayName" class="user-tooltip-line user-tooltip-sub">
                        <span>（{{ userName }}）</span>
                      </div>
                      <div v-if="userEmail" class="user-tooltip-line user-tooltip-sub">
                        <span>（{{ userEmail }}）</span>
                      </div>
                    </template>
                    <template v-else>
                      <div class="user-tooltip-line user-tooltip-main">
                        <span class="user-tooltip-prefix">{{ t('common.volur_account', 'Volur 账号：') }}</span>
                        <span class="user-tooltip-name">{{ t('common.signin') }}</span>
                      </div>
                    </template>
                  </div>
                </VTooltip>
              </template>

              <slot
                name="user-card"
                :user="currentUser"
                :is-logged-in="isLoggedIn"
                :close="closeUserCardPopover"
              >
                <VUserCard
                  :theme="currentTheme"
                  :user="currentUser"
                  :is-logged-in="isLoggedIn"
                  :display-name="displayName"
                  :user-email="userEmail"
                  :user-initial="userInitial"
                  :avatar-url="avatarUrl"
                  :avatar-shape="avatarShape"
                  :avatar-radius="avatarRadius"
                  :avatar-ring="avatarRing"
                  :logo-src="expandedLogoSrc"
                  :logo-alt="logoAlt"
                  :account-chooser-route="accountChooserRoute"
                  :user-profile-route="userProfileRoute"
                  :signin-route="signinRoute"
                  :privacy-route="privacyRoute"
                  :terms-route="termsRoute"
                  @close="closeUserCardPopover"
                  @switch-account="(ev) => emit('switch-account', ev)"
                  @logout="(ev) => emit('logout', ev)"
                  @profile-click="(ev) => emit('profile-click', ev)"
                />
              </slot>
            </n-popover>
          </slot>
        </div>

        <div v-if="$slots.bottom && !isCollapsed" class="sidebar-bottom-action">
          <slot name="bottom" :collapsed="isCollapsed" />
        </div>
      </div>
    </div>

    <slot
      name="search-dialog"
      :show="isSearchVisible"
      :close="closeSearch"
    >
      <VSearchTool
        v-if="showSearch"
        v-model:show="isSearchVisible"
        :theme="currentTheme"
        :placeholder="searchPlaceholder"
        :search-api="searchApi"
        @select="handleSearchSelect"
        @close="handleSearchClose"
      />
    </slot>
  </n-layout-sider>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick, h } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  NLayoutSider,
  NMenu,
  NPopover,
  NBadge,
  NIcon,
  type MenuOption
} from 'naive-ui'
import {
  TrophyOutline,
  MegaphoneOutline,
  MenuOutline,
  AddOutline,
  SearchOutline,
  NotificationsOutline,
  SettingsOutline,
  SunnyOutline,
  MoonOutline,
  AppsSharp,
  ArrowBackOutline
} from '@vicons/ionicons5'
import { MenuOpenOutlined } from '@vicons/material'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { useNotificationStore } from '@/store/modules/notification'
import type { CompetitionSummaryVO } from '@/types/competition'
import VSearchTool from './VSearchTool.vue'
import VAvatar from './VAvatar.vue'
import VNotice from './VNotice.vue'
import VUserCard from './VUserCard.vue'
import VSettingCard from './VSettingCard.vue'
import VTooltip from '@/components/common/VTooltip.vue'
import type { VSideBarProps } from './types'

defineOptions({
  name: 'VSideBar'
})

const props = withDefaults(defineProps<VSideBarProps>(), {
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
  siderClass: ''
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

const router = useRouter()
const { t } = useI18n()
const appStore = useAppStore()
const userStore = useUserStore()
const notificationStore = useNotificationStore()

const isSearchVisible = ref<boolean>(false)
const isAppsPopoverShow = ref<boolean>(false)
const isUserCardPopoverShow = ref<boolean>(false)
const isNoticePopoverShow = ref<boolean>(false)
const isCollapsedNoticePopoverShow = ref<boolean>(false)
const isSettingPopoverShow = ref<boolean>(false)
const isCollapsedSettingPopoverShow = ref<boolean>(false)

const unreadCount = computed<number>(() => notificationStore.unreadCount)

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

const expandedLogoSrc = computed<string>(() => {
  if (props.logoExpandedSrc) return props.logoExpandedSrc
  return '/r/static/svg/VolurCTFStudio.svg'
})

const collapsedLogoSrc = computed<string>(() => {
  if (props.logoCollapsedSrc) return props.logoCollapsedSrc
  return currentTheme.value === 'dark'
    ? '/r/static/svg/favicon-dark.svg'
    : '/r/static/svg/favicon.svg'
})

const primaryActionIconComponent = computed(() => {
  return props.primaryActionIcon || AddOutline
})

const defaultMenuOptions = computed<MenuOption[]>(() => [
  {
    label: () => t('common.competitions'),
    key: 'competitions',
    icon: () => h(NIcon, null, { default: () => h(TrophyOutline) })
  },
  {
    label: () => t('common.announcements'),
    key: 'announcements',
    icon: () => h(NIcon, null, { default: () => h(MegaphoneOutline) })
  }
])

const computedMenuOptions = computed<MenuOption[]>(() => {
  if (props.menuOptions && props.menuOptions.length > 0) {
    return props.menuOptions
  }
  return defaultMenuOptions.value
})

const isLoggedIn = computed<boolean>(() => {
  return userStore.isLoggedIn
})

const currentUser = computed(() => {
  return userStore.currentUser
})

const displayName = computed<string>(() => {
  return userStore.displayName
})

const userEmail = computed<string>(() => {
  return userStore.userEmail
})

const avatarUrl = computed<string | undefined>(() => {
  return isLoggedIn.value ? userStore.avatarUrl : undefined
})

const userInitial = computed<string>(() => {
  return userStore.userInitial
})

const userName = computed<string>(() => {
  return userStore.currentUser?.username || ''
})

const userCardRef = ref<HTMLElement | null>(null)
const userCardWidth = ref<number>(
  typeof props.width === 'number' ? Math.max(props.width - 16, 240) : 240
)

function updateUserCardWidth() {
  if (userCardRef.value && !isCollapsed.value) {
    const rect = userCardRef.value.getBoundingClientRect()
    if (rect.width >= 120) {
      userCardWidth.value = Math.round(rect.width)
    } else {
      userCardWidth.value = typeof props.width === 'number' ? Math.max(props.width - 16, 240) : 240
    }
  }
}

let transitionTimer: ReturnType<typeof setTimeout> | null = null

function scheduleUpdateUserCardWidth() {
  updateUserCardWidth()
  if (transitionTimer) {
    clearTimeout(transitionTimer)
  }
  transitionTimer = setTimeout(() => {
    updateUserCardWidth()
  }, 260)
}

const isUserTooltipShow = ref<boolean>(false)

const userTooltipStyle = computed(() => {
  if (isCollapsed.value) {
    return { '--v-user-card-tooltip-width': 'auto' }
  }
  return { '--v-user-card-tooltip-width': `${userCardWidth.value}px` }
})

watch(isUserCardPopoverShow, (val) => {
  if (val) {
    isUserTooltipShow.value = false
  }
})

const userTooltipText = computed<string>(() => {
  const prefix = t('common.volur_account', 'Volur 账号：')
  if (isLoggedIn.value) {
    const lines = [`${prefix}${displayName.value}`]
    if (userName.value && userName.value !== displayName.value) {
      lines.push(`（${userName.value}）`)
    }
    if (userEmail.value) {
      lines.push(`（${userEmail.value}）`)
    }
    return lines.join('\n')
  }
  return `${prefix}${t('common.signin')}`
})

watch(
  () => userStore.isLoggedIn,
  (val) => {
    if (val) {
      userStore.fetchCurrentUser().catch(() => {})
      notificationStore.fetchUnreadCount().catch(() => {})
    } else {
      isUserCardPopoverShow.value = false
      isUserTooltipShow.value = false
      userStore.clearUser()
      notificationStore.reset()
    }
  }
)

watch(isNoticePopoverShow, (val) => {
  if (val) {
    isUserCardPopoverShow.value = false
    isSettingPopoverShow.value = false
    isCollapsedSettingPopoverShow.value = false
    emit('notice-card-open')
  } else {
    emit('notice-card-close')
  }
})

watch(isCollapsedNoticePopoverShow, (val) => {
  if (val) {
    isUserCardPopoverShow.value = false
    isSettingPopoverShow.value = false
    isCollapsedSettingPopoverShow.value = false
    emit('notice-card-open')
  } else {
    emit('notice-card-close')
  }
})

watch(isUserCardPopoverShow, (val) => {
  if (val) {
    isNoticePopoverShow.value = false
    isCollapsedNoticePopoverShow.value = false
    isSettingPopoverShow.value = false
    isCollapsedSettingPopoverShow.value = false
    emit('user-card-open')
  } else {
    emit('user-card-close')
  }
})

watch(isSettingPopoverShow, (val) => {
  if (val) {
    isUserCardPopoverShow.value = false
    isNoticePopoverShow.value = false
    isCollapsedNoticePopoverShow.value = false
    isCollapsedSettingPopoverShow.value = false
    emit('setting-card-open')
  } else {
    emit('setting-card-close')
  }
})

watch(isCollapsedSettingPopoverShow, (val) => {
  if (val) {
    isUserCardPopoverShow.value = false
    isNoticePopoverShow.value = false
    isCollapsedNoticePopoverShow.value = false
    isSettingPopoverShow.value = false
    emit('setting-card-open')
  } else {
    emit('setting-card-close')
  }
})

function handleUpdateCollapsed(val: boolean) {
  isUserCardPopoverShow.value = false
  isNoticePopoverShow.value = false
  isCollapsedNoticePopoverShow.value = false
  isSettingPopoverShow.value = false
  isCollapsedSettingPopoverShow.value = false
  isCollapsed.value = val
  if (modelCollapsed.value !== undefined) {
    modelCollapsed.value = val
  }
  appStore.isSidebarCollapsed = val
  if (val) {
    emit('collapse')
  } else {
    nextTick(() => {
      scheduleUpdateUserCardWidth()
    })
    emit('expand')
  }
}

function handleCollapse() {
  isUserCardPopoverShow.value = false
  isNoticePopoverShow.value = false
  isCollapsedNoticePopoverShow.value = false
  isSettingPopoverShow.value = false
  isCollapsedSettingPopoverShow.value = false
  isCollapsed.value = true
  if (modelCollapsed.value !== undefined) {
    modelCollapsed.value = true
  }
  appStore.isSidebarCollapsed = true
  emit('collapse')
}

function handleExpand() {
  isUserCardPopoverShow.value = false
  isNoticePopoverShow.value = false
  isCollapsedNoticePopoverShow.value = false
  isSettingPopoverShow.value = false
  isCollapsedSettingPopoverShow.value = false
  isCollapsed.value = false
  if (modelCollapsed.value !== undefined) {
    modelCollapsed.value = false
  }
  appStore.isSidebarCollapsed = false
  nextTick(() => {
    scheduleUpdateUserCardWidth()
  })
  emit('expand')
}

function handleLogoClick(ev: MouseEvent) {
  emit('logo-click', ev)
  if (props.logoRoute) {
    router.push(props.logoRoute).catch(() => {})
  }
}

function handlePrimaryActionClick(ev: MouseEvent) {
  emit('primary-action-click', ev)
  if (props.primaryActionRoute) {
    router.push(props.primaryActionRoute).catch(() => {})
  }
}

function handleSearchClick(ev: MouseEvent) {
  emit('search-click', ev)
  if (props.searchRoute) {
    router.push(props.searchRoute).catch(() => {})
  } else {
    isSearchVisible.value = true
    emit('search-open')
  }
}

function openSearch() {
  isSearchVisible.value = true
  emit('search-open')
}

function closeSearch() {
  isSearchVisible.value = false
  emit('search-close')
}

function handleSearchSelect(item: CompetitionSummaryVO) {
  emit('search-select', item)
  if (item.slug) {
    router.push(`/c/${item.slug}`).catch(() => {})
  } else if (item.id) {
    router.push(`/competitions/${item.id}`).catch(() => {})
  }
}

function handleSearchClose() {
  emit('search-close')
}

function handleNotificationClick(ev: MouseEvent) {
  emit('notification-click', ev)
  isUserCardPopoverShow.value = false
}

function handleSettingsClick(ev: MouseEvent) {
  emit('settings-click', ev)
  isUserCardPopoverShow.value = false
  isNoticePopoverShow.value = false
}

function handleThemeToggle() {
  appStore.toggleTheme()
  emit('theme-toggle')
}

function handleCollapsedSearchClick(ev: MouseEvent) {
  isAppsPopoverShow.value = false
  handleSearchClick(ev)
}

function handleCollapsedNotificationClick(ev: MouseEvent) {
  emit('notification-click', ev)
  isUserCardPopoverShow.value = false
}

function closeNoticePopover() {
  isNoticePopoverShow.value = false
  isCollapsedNoticePopoverShow.value = false
  emit('notice-card-close')
}

function handleCollapsedSettingsClick(ev: MouseEvent) {
  emit('settings-click', ev)
  isUserCardPopoverShow.value = false
  isNoticePopoverShow.value = false
  isCollapsedNoticePopoverShow.value = false
}

function closeSettingPopover() {
  isSettingPopoverShow.value = false
  isCollapsedSettingPopoverShow.value = false
  isAppsPopoverShow.value = false
  emit('setting-card-close')
}

function handleUserClick(ev: MouseEvent) {
  isUserTooltipShow.value = false
  emit('user-click', ev)
  if (!isLoggedIn.value) {
    if (props.signinRoute) {
      router.push(props.signinRoute).catch(() => {})
    }
  }
}

function closeUserCardPopover() {
  isUserCardPopoverShow.value = false
  isUserTooltipShow.value = false
  emit('user-card-close')
}

function handleMenuSelect(key: string, item: MenuOption) {
  currentValue.value = key
  emit('select', key, item)
}

function handleBottomActionClick(ev: MouseEvent) {
  isAppsPopoverShow.value = false
  emit('bottom-action-click', ev)
  if (props.bottomActionRoute) {
    router.push(props.bottomActionRoute).catch(() => {})
  }
}

const siderRef = ref<InstanceType<typeof NLayoutSider> | null>(null)
const navContainerRef = ref<HTMLElement | null>(null)
const isSticky = ref<boolean>(false)

const scrollContainerRef = computed<HTMLElement | null>(() => {
  return navContainerRef.value || siderRef.value?.$el?.querySelector('.n-layout-sider-scroll-container') || null
})

function handleSidebarScroll(ev: Event) {
  const target = ev.target as HTMLElement | null
  if (target) {
    isSticky.value = target.scrollTop > props.stickyScrollThreshold
  }
}

function handleSidebarWheel(e: WheelEvent) {
  if (navContainerRef.value && e.deltaY !== 0) {
    const el = navContainerRef.value
    if (el.scrollHeight > el.clientHeight) {
      if (e.target && el.contains(e.target as Node)) {
        return
      }
      e.preventDefault()
      el.scrollTop += e.deltaY
    }
  }
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.fetchCurrentUser().catch(() => {})
    notificationStore.fetchUnreadCount().catch(() => {})
  }
  if (navContainerRef.value) {
    navContainerRef.value.addEventListener('scroll', handleSidebarScroll, { passive: true })
  }
  const el = siderRef.value?.$el?.querySelector('.n-layout-sider-scroll-container') as HTMLElement | null
  if (el) {
    el.addEventListener('scroll', handleSidebarScroll, { passive: true })
  }
  nextTick(() => {
    scheduleUpdateUserCardWidth()
  })
  if (typeof ResizeObserver !== 'undefined' && userCardRef.value) {
    resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const w = entry.contentRect.width || (entry.target as HTMLElement).offsetWidth
        if (!isCollapsed.value && w >= 120) {
          userCardWidth.value = Math.round(w)
        }
      }
    })
    resizeObserver.observe(userCardRef.value)
  }
  window.addEventListener('resize', updateUserCardWidth, { passive: true })
})

onBeforeUnmount(() => {
  if (transitionTimer) {
    clearTimeout(transitionTimer)
    transitionTimer = null
  }
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (navContainerRef.value) {
    navContainerRef.value.removeEventListener('scroll', handleSidebarScroll)
  }
  const el = siderRef.value?.$el?.querySelector('.n-layout-sider-scroll-container') as HTMLElement | null
  if (el) {
    el.removeEventListener('scroll', handleSidebarScroll)
  }
  window.removeEventListener('resize', updateUserCardWidth)
})

function scrollToTop(options?: ScrollToOptions) {
  if (navContainerRef.value) {
    navContainerRef.value.scrollTo({ top: 0, behavior: 'smooth', ...options })
  }
  if (siderRef.value) {
    siderRef.value.scrollTo({ top: 0, behavior: 'smooth', ...options })
  }
}

function toggleCollapse() {
  isUserCardPopoverShow.value = false
  isNoticePopoverShow.value = false
  isCollapsedNoticePopoverShow.value = false
  isSettingPopoverShow.value = false
  isCollapsedSettingPopoverShow.value = false
  const next = !isCollapsed.value
  isCollapsed.value = next
  if (modelCollapsed.value !== undefined) {
    modelCollapsed.value = next
  }
  appStore.isSidebarCollapsed = next
  if (!next) {
    nextTick(() => {
      scheduleUpdateUserCardWidth()
    })
  }
  if (next) {
    emit('collapse')
  } else {
    emit('expand')
  }
}

function expand() {
  isUserCardPopoverShow.value = false
  isNoticePopoverShow.value = false
  isCollapsedNoticePopoverShow.value = false
  isSettingPopoverShow.value = false
  isCollapsedSettingPopoverShow.value = false
  isCollapsed.value = false
  if (modelCollapsed.value !== undefined) {
    modelCollapsed.value = false
  }
  appStore.isSidebarCollapsed = false
  nextTick(() => {
    scheduleUpdateUserCardWidth()
  })
  emit('expand')
}

function collapse() {
  isUserCardPopoverShow.value = false
  isNoticePopoverShow.value = false
  isCollapsedNoticePopoverShow.value = false
  isSettingPopoverShow.value = false
  isCollapsedSettingPopoverShow.value = false
  isCollapsed.value = true
  if (modelCollapsed.value !== undefined) {
    modelCollapsed.value = true
  }
  appStore.isSidebarCollapsed = true
  emit('collapse')
}

defineExpose({
  isCollapsed,
  isSticky,
  isSearchVisible,
  isUserCardPopoverShow,
  isUserTooltipShow,
  isNoticePopoverShow,
  isCollapsedNoticePopoverShow,
  isSettingPopoverShow,
  isCollapsedSettingPopoverShow,
  unreadCount,
  userTooltipText,
  userCardWidth,
  siderRef,
  navContainerRef,
  scrollContainerRef,
  scrollToTop,
  toggleCollapse,
  expand,
  collapse,
  openSearch,
  closeSearch,
  closeUserCardPopover,
  closeNoticePopover,
  closeSettingPopover
})
</script>

<style scoped lang="scss">
.v-sidebar {
  height: 100%;
  background-color: #fafafa !important;
  border-right: none !important;
  z-index: 10;
  overflow: hidden !important;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1), max-width 0.25s cubic-bezier(0.4, 0, 0.2, 1), min-width 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s ease, border-color 0.25s ease;
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;

  &::-webkit-scrollbar {
    display: none !important;
    width: 0 !important;
    height: 0 !important;
  }

  :deep(.n-layout-sider__border) {
    background-color: #d7d8da80 !important;
  }

  &:not(.is-collapsed):not(.n-layout-sider--collapsed) {
    width: 256px !important;
    max-width: 256px !important;
    min-width: 256px !important;
  }

  &.is-collapsed,
  &.n-layout-sider--collapsed {
    width: 74px !important;
    max-width: 74px !important;
    min-width: 74px !important;

    .sidebar-content-wrapper {
      padding: 0 8px;
      align-items: center;
      width: 100%;
      height: 100%;
      max-height: 100%;
      overflow: hidden;
    }

    .sidebar-header {
      width: 100%;
      margin: 0;
      padding: 0;
    }

    .sidebar-primary-action-wrap {
      padding: 8px 0 12px;
      display: flex;
      justify-content: center;
    }

    .sidebar-nav-container {
      align-items: center;
      scrollbar-gutter: auto;

      .v-menu {
        width: 38px !important;
        margin: 0 auto;
        overflow: visible !important;
        flex-shrink: 0 !important;
        height: auto !important;
        min-height: min-content !important;

        &.n-menu,
        :deep(.n-menu) {
          overflow: visible !important;
          flex-shrink: 0 !important;
          height: auto !important;
          min-height: min-content !important;
        }

        :deep(.n-menu-item) {
          width: 38px !important;
          margin: 6px auto;
          flex-shrink: 0 !important;

          .n-menu-item-content {
            width: 38px !important;
            height: 38px !important;
            padding: 0 !important;
            display: flex !important;
            align-items: center !important;
            justify-content: center !important;

            .n-menu-item-content-header {
              max-width: 0 !important;
              width: 0 !important;
              height: 0 !important;
              margin: 0 !important;
              padding: 0 !important;
              overflow: hidden !important;
              opacity: 0 !important;
              visibility: hidden !important;
              pointer-events: none !important;
            }

            .n-menu-item-content__icon {
              margin: 0 !important;
              display: flex !important;
              align-items: center !important;
              justify-content: center !important;
            }

            .n-menu-item-content__arrow {
              display: none !important;
            }
          }
        }
      }
    }

    .sidebar-bottom-area {
      padding: 8px 0 12px;
      align-items: center;
    }
  }

  :deep(.n-layout-sider-scroll-container) {
    height: 100%;
    max-height: 100%;
    width: 100%;
    background-color: transparent !important;
    display: flex;
    flex-direction: column;
    overflow: hidden !important;
    scrollbar-width: none !important;
    -ms-overflow-style: none !important;

    &::-webkit-scrollbar {
      display: none !important;
      width: 0 !important;
      height: 0 !important;
    }
  }

  .sidebar-content-wrapper {
    height: 100%;
    max-height: 100%;
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    padding: 0 8px;
    width: 100%;
    overflow: hidden;
    scrollbar-width: none !important;
    -ms-overflow-style: none !important;

    &::-webkit-scrollbar {
      display: none !important;
      width: 0 !important;
      height: 0 !important;
    }
  }

  .sidebar-header {
    position: sticky;
    top: 0;
    z-index: 20;
    height: 60px;
    width: 100%;
    margin: 0;
    padding: 0;
    flex-shrink: 0;
    box-sizing: border-box;
    transition: background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;

    &.is-sticky {
      background-color: rgba(250, 250, 250, 0.95);
      backdrop-filter: blur(12px) saturate(180%);
      -webkit-backdrop-filter: blur(12px) saturate(180%);
      border-bottom: 1px solid #d7d8da80;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
    }

    .header-inner {
      height: 100%;
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0;
      box-sizing: border-box;
      overflow: hidden;

      .logo-container {
        display: flex;
        align-items: center;
        cursor: pointer;
        user-select: none;
        overflow: hidden;
        min-width: 0;
        max-width: 180px;
        opacity: 1;
        padding: 0 18px;
        transition: max-width 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1), padding 0.25s cubic-bezier(0.4, 0, 0.2, 1);

        &.is-collapsed {
          max-width: 0 !important;
          opacity: 0 !important;
          padding: 0 !important;
          pointer-events: none;
        }

        .brand-logo-expanded {
          height: 16px;
          width: auto;
          max-width: 160px;
          object-fit: contain;
          white-space: nowrap;
        }

        .brand-logo-collapsed {
          width: 32px;
          height: 32px;
          object-fit: contain;
        }
      }

      .brand-placeholder {
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
        color: #3c4043;
        transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1), color 0.2s cubic-bezier(0.2, 0, 0, 1), margin 0.25s cubic-bezier(0.4, 0, 0.2, 1);
        flex-shrink: 0;
        outline: none;

        &:hover {
          background-color: rgba(60, 64, 67, 0.08);
          color: #202124;
        }

        &:active {
          background-color: rgba(60, 64, 67, 0.12);
        }
      }
    }

    &.is-collapsed {
      .header-inner {
        justify-content: center;

        .sidebar-toggle-btn {
          margin: 0 auto;
        }
      }
    }
  }

  .sidebar-primary-action-wrap {
    padding: 8px 0 12px;
    flex-shrink: 0;
    width: 100%;
    display: flex;
    justify-content: center;
    box-sizing: border-box;

    .primary-action-btn {
      width: 100%;
      height: 40px;
      border-radius: 12px;
      border: none;
      background-color: #1a73e8;
      color: #ffffff;
      font-size: 14px;
      font-weight: 500;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 16px;
      cursor: pointer;
      box-shadow: 0 1px 3px 0 rgba(60, 64, 67, 0.3), 0 4px 8px 3px rgba(60, 64, 67, 0.15);
      transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1), height 0.25s cubic-bezier(0.4, 0, 0.2, 1), padding 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.2s cubic-bezier(0.2, 0, 0, 1), box-shadow 0.2s cubic-bezier(0.2, 0, 0, 1), transform 0.15s ease;
      overflow: hidden;

      &:hover {
        background-color: #1765cc;
        box-shadow: 0 2px 6px 2px rgba(60, 64, 67, 0.15), 0 1px 2px 0 rgba(60, 64, 67, 0.3);
      }

      &:active {
        background-color: #1557b0;
        transform: scale(0.98);
      }

      .primary-action-icon {
        flex-shrink: 0;
      }

      .action-btn-label {
        max-width: 160px;
        opacity: 1;
        margin-left: 10px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        transition: max-width 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1), margin-left 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      }

      &.is-collapsed {
        width: 38px !important;
        height: 38px !important;
        padding: 0 !important;

        .action-btn-label {
          max-width: 0 !important;
          opacity: 0 !important;
          margin-left: 0 !important;
          pointer-events: none;
        }
      }
    }
  }

  .sidebar-nav-container {
    flex: 1;
    min-height: 0;
    width: 100%;
    margin-right: 0;
    padding-right: 0;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
    overflow-x: hidden;
    overscroll-behavior: contain;
    -webkit-overflow-scrolling: touch;
    touch-action: pan-y;
    scrollbar-gutter: stable;

    scrollbar-width: thin;
    scrollbar-color: rgba(60, 64, 67, 0.2) transparent;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(60, 64, 67, 0.2);
      border-radius: 4px;

      &:hover {
        background: rgba(60, 64, 67, 0.35);
      }
    }

    .v-menu {
      user-select: none;
      padding: 4px 0 16px;
      width: 100%;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 4px;
      overflow: visible !important;
      flex-shrink: 0 !important;
      height: auto !important;
      min-height: min-content !important;

      &.n-menu,
      :deep(.n-menu) {
        display: flex;
        flex-direction: column;
        gap: 4px;
        width: 100%;
        overflow: visible !important;
        flex-shrink: 0 !important;
        height: auto !important;
        min-height: min-content !important;
      }

      :deep(.n-menu-item) {
        margin: 2px 0;
        height: 40px;
        width: 100%;
        border-radius: 16px;
        display: flex;
        align-items: center;
        background-color: transparent;
        cursor: pointer;
        flex-shrink: 0 !important;
        transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1), margin 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.2s cubic-bezier(0.2, 0, 0, 1);

        &:hover {
          background-color: #efefef;

          .n-menu-item-content-header {
            color: #1b1b1e !important;
          }
          .n-menu-item-content__icon {
            color: #1b1b1e !important;
          }
          .n-menu-item-content__arrow {
            color: #1b1b1e !important;
          }
        }

        &:has(.n-menu-item-content--selected),
        &:has(.n-menu-item-content--child-active) {
          background-color: #ececef;

          .n-menu-item-content__icon {
            color: #1b1b1e !important;
          }

          .n-menu-item-content-header {
            color: #1b1b1e !important;
          }

          .n-menu-item-content__arrow {
            color: #1b1b1e !important;
          }

          &:hover {
            background-color: #ececef;
          }
        }

        .n-menu-item-content {
          height: 40px;
          line-height: 40px;
          border-radius: 12px;
          padding: 0 12px !important;
          display: flex;
          align-items: center;
          width: 100%;
          background: transparent !important;
          overflow: hidden;
          transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1), padding 0.25s cubic-bezier(0.4, 0, 0.2, 1);

          &::before {
            display: none !important;
          }

          .n-menu-item-content__icon {
            color: #565b61;
            transition: color 0.2s cubic-bezier(0.2, 0, 0, 1), margin 0.25s cubic-bezier(0.4, 0, 0.2, 1);
            margin-right: 12px !important;
            flex-shrink: 0;
          }

          .n-menu-item-content-header {
            color: #565b61;
            font-weight: 500;
            font-size: 14px;
            max-width: 160px;
            opacity: 1;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            transition: color 0.2s cubic-bezier(0.2, 0, 0, 1), max-width 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1);

            a {
              color: inherit;
              text-decoration: none;
            }
          }

          .n-menu-item-content__arrow {
            margin-left: auto;
            color: #565b61;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: color 0.2s cubic-bezier(0.2, 0, 0, 1), transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
          }
        }
      }

      :deep(.n-submenu) {
        margin-top: 0 !important;
        margin-bottom: 0 !important;
        width: 100%;
        display: flex;
        flex-direction: column;
        overflow: visible !important;
        flex-shrink: 0 !important;
        height: auto !important;

        > .n-menu-item {
          width: 100%;
          margin: 2px 0;
          flex-shrink: 0 !important;
        }

        .n-submenu-children {
          width: 100%;
          display: flex;
          flex-direction: column;
          gap: 2px;
          padding: 2px 0;
          overflow: visible !important;
          flex-shrink: 0 !important;
          height: auto !important;

          .n-menu-item {
            width: 100%;
            margin: 1px 0;
            flex-shrink: 0 !important;

            .n-menu-item-content {
              padding-left: 32px !important;
            }
          }
        }
      }

      &.n-menu--collapsed {
        :deep(.n-submenu) {
          margin: 6px auto !important;
          width: 38px !important;
          display: flex;
          justify-content: center;
          align-items: center;
          flex-shrink: 0 !important;

          > .n-menu-item {
            margin: 0 auto !important;
            width: 38px !important;
            height: 38px !important;
            flex-shrink: 0 !important;
          }

          .n-submenu-children {
            display: none !important;
          }
        }

        :deep(.n-menu-item) {
          margin: 6px auto;
          width: 38px;
          height: 38px;
          border-radius: 12px;
          display: flex;
          justify-content: center;
          align-items: center;
          background-color: transparent;
          cursor: pointer;
          overflow: hidden;
          flex-shrink: 0 !important;
          transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1);

          &:hover {
            background-color: #efefef;

            .n-menu-item-content__icon {
              color: #1b1b1e !important;
            }
          }

          &:has(.n-menu-item-content--selected),
          &:has(.n-menu-item-content--child-active) {
            background-color: #ececef;

            .n-menu-item-content__icon {
              color: #1b1b1e !important;
            }

            &:hover {
              background-color: #ececef;
            }
          }

          .n-menu-item-content {
            width: 38px;
            height: 38px;
            line-height: 38px;
            padding: 0 !important;
            border-radius: 12px;
            display: flex;
            justify-content: center;
            align-items: center;
            background: transparent !important;
            overflow: hidden;

            &::before {
              display: none !important;
            }

            .n-menu-item-content__icon {
              margin: 0 !important;
              color: #565b61;
              display: flex;
              align-items: center;
              justify-content: center;
              transition: color 0.2s cubic-bezier(0.2, 0, 0, 1);
            }

            .n-menu-item-content-header {
              max-width: 0 !important;
              width: 0 !important;
              height: 0 !important;
              margin: 0 !important;
              padding: 0 !important;
              overflow: hidden !important;
              opacity: 0 !important;
              visibility: hidden !important;
              pointer-events: none !important;
            }

            .n-menu-item-content__arrow {
              display: none !important;
            }
          }
        }
      }
    }
  }

  .sidebar-bottom-area {
    margin-top: auto;
    flex-shrink: 0;
    border-top: none;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 8px 0 12px;
    width: 100%;
    overflow: hidden;
    transition: padding 0.25s cubic-bezier(0.4, 0, 0.2, 1);

    &.is-collapsed {
      padding: 8px 0 12px;
      align-items: center;
    }

    .sidebar-action-box {
      width: 100%;
      box-sizing: border-box;
      overflow: hidden;
      display: flex;
      justify-content: center;

      &.is-collapsed {
        width: 38px;
      }

      .action-box-expanded {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        width: 100%;

        .action-icon-btn {
          flex: 1;
          height: 32px;
          border-radius: 12px;
          border: 1px solid #d7d8da80;
          background: transparent;
          color: #3c4043;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1), color 0.2s cubic-bezier(0.2, 0, 0, 1), border-color 0.2s ease;
          outline: none;

          &:hover {
            color: #202124;
            background-color: #f4f5f5;
          }

          &:active {
            background-color: rgba(60, 64, 67, 0.12);
          }
        }
      }

      .action-box-collapsed {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 6px;
        width: 38px;

        .action-icon-btn {
          width: 38px;
          height: 38px;
          border-radius: 12px;
          border: 1px solid #d7d8da80;
          background: transparent;
          color: #3c4043;
          display: flex;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1), color 0.2s cubic-bezier(0.2, 0, 0, 1), border-color 0.2s ease;
          outline: none;

          &:hover,
          &.is-active {
            color: #202124;
            background-color: #f4f5f5;
          }

          &:active {
            background-color: rgba(60, 64, 67, 0.12);
          }
        }
      }
    }

    .sidebar-user-box {
      width: 100%;
      box-sizing: border-box;
      display: flex;
      justify-content: center;
      overflow: hidden;

      &.is-collapsed {
        width: 38px;
      }

      .user-card {
        display: flex;
        align-items: center;
        width: 100%;
        padding: 6px 10px;
        border-radius: 12px;
        border: 1px solid #d7d8da80;
        background-color: transparent;
        cursor: pointer;
        user-select: none;
        overflow: hidden;
        box-sizing: border-box;
        transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1), height 0.25s cubic-bezier(0.4, 0, 0.2, 1), padding 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.2s cubic-bezier(0.2, 0, 0, 1), border-color 0.2s ease;

        &:hover,
        &.is-active {
          background-color: #f4f5f5;

          :deep(.v-avatar.has-ring::after) {
            border-color: #1a73e8;
          }
        }

        .user-avatar-wrap {
          position: relative;
          width: 32px;
          height: 32px;
          flex-shrink: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1), height 0.25s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .user-info-text {
          flex: 1;
          min-width: 0;
          display: flex;
          flex-direction: column;
          gap: 1px;
          margin-left: 10px;
          max-width: 150px;
          opacity: 1;
          white-space: nowrap;
          overflow: hidden;
          font-size: 12px;
          color: #565b61;
          transition: max-width 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1), margin-left 0.25s cubic-bezier(0.4, 0, 0.2, 1);

          .user-name {
            font-size: 12px;
            font-weight: 500;
            color: #565b61;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            line-height: 1.3;
          }

          .user-desc {
            font-size: 12px;
            color: #565b61;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            line-height: 1.3;
          }
        }

        &.is-collapsed {
          width: 38px !important;
          height: 38px !important;
          padding: 0 !important;
          justify-content: center !important;

          .user-avatar-wrap {
            width: 26px;
            height: 26px;
          }

          .user-info-text {
            max-width: 0 !important;
            opacity: 0 !important;
            margin-left: 0 !important;
            pointer-events: none;
          }
        }
      }
    }

    .sidebar-bottom-action {
      width: 100%;
      display: flex;
      justify-content: center;

      .bottom-action-btn {
        width: 100%;
        height: 36px;
        border-radius: 12px;
        border: 1px solid #d7d8da80;
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 0 12px;
        font-size: 13px;
        font-weight: 500;
        color: #3c4043;
        background: transparent;
        cursor: pointer;
        overflow: hidden;
        transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1), height 0.25s cubic-bezier(0.4, 0, 0.2, 1), padding 0.25s cubic-bezier(0.4, 0, 0.2, 1), color 0.2s cubic-bezier(0.2, 0, 0, 1), background-color 0.2s cubic-bezier(0.2, 0, 0, 1), border-color 0.2s ease;

        &:hover {
          color: #202124;
          background-color: #f4f5f5;
        }

        .bottom-action-label {
          max-width: 160px;
          opacity: 1;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          transition: max-width 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        }

        &.is-collapsed {
          width: 38px !important;
          height: 38px !important;
          padding: 0 !important;
          justify-content: center !important;
          gap: 0 !important;

          .bottom-action-label {
            max-width: 0 !important;
            opacity: 0 !important;
            pointer-events: none;
          }
        }
      }
    }
  }
}

html.dark {
  .v-sidebar {
    background-color: #191919 !important;

    :deep(.n-layout-sider__border) {
      background-color: #262626 !important;
    }

    scrollbar-width: none !important;
    -ms-overflow-style: none !important;

    &::-webkit-scrollbar {
      display: none !important;
      width: 0 !important;
      height: 0 !important;
    }

    :deep(.n-layout-sider-scroll-container) {
      scrollbar-width: none !important;
      -ms-overflow-style: none !important;

      &::-webkit-scrollbar {
        display: none !important;
        width: 0 !important;
        height: 0 !important;
      }
    }

    .sidebar-header {
      &.is-sticky {
        background-color: rgba(25, 25, 25, 0.95);
        border-bottom-color: #262626;
      }

      .logo-container .brand-logo-expanded {
        filter: brightness(0) invert(1);
      }

      .sidebar-toggle-btn {
        color: #bdc1c6;

        &:hover {
          background-color: rgba(255, 255, 255, 0.08);
          color: #e3e3e3;
        }

        &:active {
          background-color: rgba(255, 255, 255, 0.12);
        }
      }
    }

    .sidebar-primary-action-wrap {
      .primary-action-btn {
        background-color: #a8c7fa;
        color: #062e6f;
        box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.4), 0 4px 8px 3px rgba(0, 0, 0, 0.3);

        &:hover {
          background-color: #b6d0f8;
        }

        &:active {
          background-color: #669df6;
        }
      }
    }

    .sidebar-nav-container {
      scrollbar-color: rgba(255, 255, 255, 0.2) transparent;

      &::-webkit-scrollbar-thumb {
        background: rgba(255, 255, 255, 0.2);

        &:hover {
          background: rgba(255, 255, 255, 0.35);
        }
      }

      .v-menu {
        :deep(.n-menu-item) {
          background-color: transparent;

          &:hover {
            background-color: #2a2a2a;

            .n-menu-item-content-header {
              color: #d4d4d4 !important;
            }
            .n-menu-item-content__icon {
              color: #d4d4d4 !important;
            }
            .n-menu-item-content__arrow {
              color: #d4d4d4 !important;
            }
          }

          &:has(.n-menu-item-content--selected),
          &:has(.n-menu-item-content--child-active) {
            background-color: #2a2a2a;

            .n-menu-item-content__icon {
              color: #d4d4d4 !important;
            }

            .n-menu-item-content-header {
              color: #d4d4d4 !important;
            }

            .n-menu-item-content__arrow {
              color: #d4d4d4 !important;
            }

            &:hover {
              background-color: #2a2a2a;
            }
          }

          .n-menu-item-content {
            color: #8c8c8c;
            background: transparent !important;

            &::before {
              display: none !important;
            }

            .n-menu-item-content__icon {
              color: #8c8c8c;
            }

            .n-menu-item-content-header {
              color: #8c8c8c;

              a {
                color: inherit;
              }
            }

            .n-menu-item-content__arrow {
              color: #8c8c8c;
            }
          }
        }

        &.n-menu--collapsed {
          :deep(.n-submenu) {
            > .n-menu-item {
              margin: 0 auto !important;
            }
          }

          :deep(.n-menu-item) {
            background-color: transparent;

            &:hover {
              background-color: #2a2a2a;

              .n-menu-item-content__icon {
                color: #d4d4d4 !important;
              }
            }

            &:has(.n-menu-item-content--selected),
            &:has(.n-menu-item-content--child-active) {
              background-color: #2a2a2a;

              .n-menu-item-content__icon {
                color: #d4d4d4 !important;
              }

              &:hover {
                background-color: #2a2a2a;
              }
            }

            .n-menu-item-content {
              background: transparent !important;

              &::before {
                display: none !important;
              }

              .n-menu-item-content__icon {
                margin: 0 !important;
                display: flex !important;
                align-items: center !important;
                justify-content: center !important;
                color: #8c8c8c;
              }

              .n-menu-item-content-header {
                max-width: 0 !important;
                width: 0 !important;
                height: 0 !important;
                margin: 0 !important;
                padding: 0 !important;
                overflow: hidden !important;
                opacity: 0 !important;
                visibility: hidden !important;
                pointer-events: none !important;
              }

              .n-menu-item-content__arrow {
                display: none !important;
              }
            }
          }
        }
      }
    }

    .sidebar-bottom-area {
      border-top: none;

      .sidebar-action-box {
        .action-box-expanded .action-icon-btn,
        .action-box-collapsed .action-icon-btn {
          color: #8c8c8c;
          border-color: #262626;

          &:hover,
          &.is-active {
            color: #d4d4d4;
            background-color: #323232;
          }

          &:active {
            background-color: rgba(255, 255, 255, 0.12);
          }
        }
      }

      .sidebar-user-box {
        .user-card {
          background-color: #1f1f1f;
          border-color: #262626;

          &:hover,
          &.is-active {
            background-color: #323232;

            .user-info-text .user-name {
              color: #d4d4d4;
            }

            :deep(.v-avatar.has-ring::after) {
              border-color: #8ab4f8;
            }
          }

          .user-info-text {
            .user-name {
              color: #8c8c8c;
            }

            .user-desc {
              color: #8c8c8c;
            }
          }
        }
      }

      .sidebar-bottom-action {
        .bottom-action-btn {
          color: #8c8c8c;
          border-color: #262626;

          &:hover {
            color: #d4d4d4;
            background-color: #323232;
          }
        }
      }
    }
  }
}

.dock-fade-enter-active,
.dock-fade-leave-active {
  transition: opacity 0.15s cubic-bezier(0.4, 0, 0.2, 1), transform 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.dock-fade-enter-from,
.dock-fade-leave-to {
  opacity: 0;
  transform: scale(0.96);
}

.action-fade-enter-active,
.action-fade-leave-active {
  transition: opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1), transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.action-fade-enter-from,
.action-fade-leave-to {
  opacity: 0;
  transform: scale(0.9);
}
</style>

<style lang="scss">
.n-popover:has(.dock-apps-popover) {
  box-shadow: none !important;
}

.n-popover.n-tooltip.user-card-tooltip {
  width: var(--v-user-card-tooltip-width, 240px) !important;
  min-width: var(--v-user-card-tooltip-width, 240px) !important;
  max-width: var(--v-user-card-tooltip-width, 240px) !important;
  box-sizing: border-box !important;
  padding: 8px 12px !important;

  &.is-collapsed {
    width: max-content !important;
    min-width: 140px !important;
    max-width: 260px !important;
  }

  .n-popover__content {
    width: 100% !important;
    max-width: 100% !important;
    box-sizing: border-box !important;
    display: block !important;
  }
}

.user-tooltip-box {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  text-align: left;
  box-sizing: border-box;

  .user-tooltip-line {
    width: 100%;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 1.4;

    &.user-tooltip-main {
      font-size: 12px;
      font-weight: 500;
      color: #202124;

      .user-tooltip-prefix {
        color: #5f6368;
      }

      .user-tooltip-name {
        color: #202124;
      }
    }

    &.user-tooltip-sub {
      font-size: 11px;
      color: #5f6368;
    }
  }
}

html.dark {
  .user-tooltip-box {
    .user-tooltip-line {
      &.user-tooltip-main {
        color: #e8eaed;

        .user-tooltip-prefix {
          color: #9aa0a6;
        }

        .user-tooltip-name {
          color: #e8eaed;
        }
      }

      &.user-tooltip-sub {
        color: #9aa0a6;
      }
    }
  }
}

.dock-apps-popover {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 8px 6px;
  background-color: #fafafa;
  border: 1px solid #d7d8da80;
  border-radius: 16px;
  box-sizing: border-box;

  .dock-tool-btn {
    width: 38px;
    height: 38px;
    border-radius: 12px;
    border: 1px solid #d7d8da80;
    background: transparent;
    color: #3c4043;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
                color 0.2s cubic-bezier(0.2, 0, 0, 1),
                border-color 0.2s ease;
    outline: none;

    &:hover {
      color: #202124;
      background-color: #f4f5f5;
    }

    &:active {
      background-color: rgba(60, 64, 67, 0.12);
    }
  }

  &.dark,
  html.dark & {
    background-color: #191919;
    border: 1px solid #262626;

    .dock-tool-btn {
      color: #8c8c8c;
      border: 1px solid #262626;

      &:hover {
        color: #d4d4d4;
        background-color: #323232;
      }

      &:active {
        background-color: rgba(255, 255, 255, 0.12);
      }
    }
  }
}

.n-popover:has(.v-card),
.n-popover:has(.v-user-card),
.n-popover:has(.dock-user-popover-card),
.n-popover:has(.dock-notice-popover-card) {
  box-shadow: none !important;
  background-color: transparent !important;
}
</style>
