<template>
  <div
    class="sidebar-bottom-area"
    :class="{ 'is-collapsed': collapsed }"
  >
    <div
      v-if="showActionBox"
      class="sidebar-action-box"
      :class="{ 'is-collapsed': collapsed }"
    >
      <slot name="action-box" :collapsed="collapsed">
        <div v-if="!collapsed" key="action-expanded" class="action-box-expanded">
          <VTooltip
            v-if="showSearch"
            placement="top"
            :content="t('common.search', '搜索')"
          >
            <button
              type="button"
              class="action-icon-btn"
              :aria-label="t('common.search', '搜索')"
              @click="handleSearchClick"
            >
              <SearchOutline class="action-icon" />
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
              <component :is="bottomActionIcon || ArrowBackOutline" class="action-icon" />
            </button>
          </VTooltip>

          <VTooltip
            v-if="showNotifications"
            placement="top"
            :disabled="isNoticePopoverShow"
            :content="t('common.notifications', '通知')"
          >
            <n-popover
              v-model:show="isNoticePopoverShow"
              trigger="click"
              placement="top"
              :show-arrow="false"
              raw
              style="box-shadow: none;"
            >
              <template #trigger>
                <button
                  type="button"
                  class="action-icon-btn"
                  :class="{ 'is-active': isNoticePopoverShow }"
                  :aria-label="t('common.notifications', '通知')"
                  @click="handleNotificationClick"
                >
                  <span class="action-icon-wrap">
                    <NotificationsOutline class="action-icon" />
                    <span v-if="unreadCount > 0" class="action-badge-dot" />
                  </span>
                </button>
              </template>

              <slot
                name="notice-card"
                :close="closeNoticePopover"
              >
                <NoticeCard
                  :theme="theme"
                  :notification-route="notificationRoute"
                  :signin-route="signinRoute"
                  @close="closeNoticePopover"
                  @item-click="(item) => emit('notification-item-click', item)"
                />
              </slot>
            </n-popover>
          </VTooltip>

          <VTooltip
            v-if="showSettings"
            placement="top"
            :disabled="isSettingPopoverShow"
            :content="t('common.settings', '设置')"
          >
            <n-popover
              v-model:show="isSettingPopoverShow"
              trigger="click"
              placement="top"
              :show-arrow="false"
              raw
              style="box-shadow: none;"
            >
              <template #trigger>
                <button
                  type="button"
                  class="action-icon-btn"
                  :class="{ 'is-active': isSettingPopoverShow }"
                  :aria-label="t('common.settings', '设置')"
                  @click="handleSettingsClick"
                >
                  <SettingsOutline class="action-icon" />
                </button>
              </template>

              <slot
                name="setting-card"
                :close="closeSettingPopover"
              >
                <SettingCard
                  :theme="theme"
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
          </VTooltip>

          <VTooltip
            v-if="showThemeToggle"
            placement="top"
            :content="isDarkMode ? t('common.theme_light', '浅色模式') : t('common.theme_dark', '深色模式')"
          >
            <button
              type="button"
              class="action-icon-btn"
              :aria-label="isDarkMode ? t('common.theme_light', '浅色模式') : t('common.theme_dark', '深色模式')"
              @click="handleThemeToggle"
            >
              <SunnyOutline v-if="isDarkMode" class="action-icon" />
              <MoonOutline v-else class="action-icon" />
            </button>
          </VTooltip>
        </div>

        <div v-else key="action-collapsed" class="action-box-collapsed">
          <VTooltip
            placement="right"
            :disabled="isAppsPopoverShow"
            :content="t('common.tools', '应用与工具')"
          >
            <n-popover
              v-model:show="isAppsPopoverShow"
              trigger="click"
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
                  @click="handleActionBoxClick"
                >
                  <span class="action-icon-wrap">
                    <AppsSharp class="action-icon" />
                    <span v-if="unreadCount > 0" class="action-badge-dot" />
                  </span>
                </button>
              </template>

              <div class="dock-apps-popover" :class="theme">
                <VTooltip v-if="showSearch" placement="right" :content="t('common.search', '搜索')">
                  <button
                    type="button"
                    class="dock-tool-btn"
                    :aria-label="t('common.search', '搜索')"
                    @click="handleCollapsedSearchClick"
                  >
                    <SearchOutline class="action-icon" />
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
                    <component :is="bottomActionIcon || ArrowBackOutline" class="action-icon" />
                  </button>
                </VTooltip>

                <VTooltip
                  v-if="showNotifications"
                  placement="right"
                  :disabled="isCollapsedNoticePopoverShow"
                  :content="t('common.notifications', '通知')"
                >
                  <n-popover
                    v-model:show="isCollapsedNoticePopoverShow"
                    trigger="click"
                    placement="right-end"
                    :show-arrow="false"
                    raw
                    style="box-shadow: none;"
                  >
                    <template #trigger>
                      <button
                        type="button"
                        class="dock-tool-btn"
                        :class="{ 'is-active': isCollapsedNoticePopoverShow }"
                        :aria-label="t('common.notifications', '通知')"
                        @click="handleCollapsedNoticeClick"
                      >
                        <span class="action-icon-wrap">
                          <NotificationsOutline class="action-icon" />
                          <span v-if="unreadCount > 0" class="action-badge-dot" />
                        </span>
                      </button>
                    </template>

                    <slot
                      name="notice-card"
                      :close="closeCollapsedNoticePopover"
                    >
                      <NoticeCard
                        :theme="theme"
                        :notification-route="notificationRoute"
                        :signin-route="signinRoute"
                        @close="closeCollapsedNoticePopover"
                        @item-click="(item) => emit('notification-item-click', item)"
                      />
                    </slot>
                  </n-popover>
                </VTooltip>

                <VTooltip
                  v-if="showSettings"
                  placement="right"
                  :disabled="isCollapsedSettingPopoverShow"
                  :content="t('common.settings', '设置')"
                >
                  <n-popover
                    v-model:show="isCollapsedSettingPopoverShow"
                    trigger="click"
                    placement="right-end"
                    :show-arrow="false"
                    raw
                    style="box-shadow: none;"
                  >
                    <template #trigger>
                      <button
                        type="button"
                        class="dock-tool-btn"
                        :class="{ 'is-active': isCollapsedSettingPopoverShow }"
                        :aria-label="t('common.settings', '设置')"
                        @click="handleCollapsedSettingClick"
                      >
                        <SettingsOutline class="action-icon" />
                      </button>
                    </template>

                    <slot
                      name="setting-card"
                      :close="closeCollapsedSettingPopover"
                    >
                      <SettingCard
                        :theme="theme"
                        :user-settings-route="settingsRoute"
                        :admin-route="adminRoute"
                        :feedback-route="feedbackRoute"
                        :terms-route="termsRoute"
                        :privacy-route="privacyRoute"
                        @close="closeCollapsedSettingPopover"
                        @item-click="(key) => emit('setting-item-click', key)"
                        @admin-click="(ev) => emit('admin-click', ev)"
                      />
                    </slot>
                  </n-popover>
                </VTooltip>

                <VTooltip
                  v-if="showThemeToggle"
                  placement="right"
                  :content="isDarkMode ? t('common.theme_light', '浅色模式') : t('common.theme_dark', '深色模式')"
                >
                  <button
                    type="button"
                    class="dock-tool-btn"
                    :aria-label="isDarkMode ? t('common.theme_light', '浅色模式') : t('common.theme_dark', '深色模式')"
                    @click="handleThemeToggle"
                  >
                    <SunnyOutline v-if="isDarkMode" class="action-icon" />
                    <MoonOutline v-else class="action-icon" />
                  </button>
                </VTooltip>
              </div>
            </n-popover>
          </VTooltip>
        </div>
      </slot>
    </div>

    <div
      v-if="showUserProfile"
      class="sidebar-user-box"
      :class="{ 'is-collapsed': collapsed }"
    >
      <slot
        name="user-profile"
        :collapsed="collapsed"
        :is-logged-in="isLoggedIn"
        :user="currentUser"
      >
        <VTooltip
          :placement="collapsed ? 'right' : 'top'"
          :disabled="isUserCardPopoverShow"
          :content="userTooltipText"
        >
          <n-popover
            v-model:show="isUserCardPopoverShow"
            :disabled="!isLoggedIn"
            trigger="click"
            :placement="collapsed ? 'right-end' : 'top'"
            :show-arrow="false"
            raw
            style="box-shadow: none;"
          >
            <template #trigger>
              <div
                class="user-card"
                :class="{ 'is-collapsed': collapsed, 'is-active': isUserCardPopoverShow }"
                @click="handleUserClick"
              >
                <div class="user-avatar-wrap">
                  <VAvatar
                    :src="isLoggedIn ? avatarUrl : ''"
                    :text="isLoggedIn ? userInitial : ''"
                    :size="collapsed ? 26 : 32"
                    :shape="avatarShape"
                    :radius="avatarRadius"
                    :ring="avatarRing"
                    :theme="theme"
                  />
                </div>

                <div v-if="!collapsed" class="user-info-text">
                  <span class="user-name">
                    {{ isLoggedIn ? displayName : t('common.signin', '登录') }}
                  </span>
                  <span v-if="isLoggedIn && userEmail" class="user-desc">
                    {{ userEmail }}
                  </span>
                </div>
              </div>
            </template>

            <slot
              name="user-card"
              :user="currentUser"
              :is-logged-in="isLoggedIn"
              :close="closeUserCardPopover"
            >
            <UserCard
              :theme="theme"
              :user="currentUser"
              :is-logged-in="isLoggedIn"
              :display-name="displayName"
              :user-email="userEmail"
              :user-initial="userInitial"
              :avatar-url="avatarUrl"
              :avatar-shape="avatarShape"
              :avatar-radius="avatarRadius"
              :avatar-ring="avatarRing"
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
      </VTooltip>
    </slot>
    </div>

    <div v-if="$slots.bottom && !collapsed" class="sidebar-bottom-action">
      <slot name="bottom" :collapsed="collapsed" />
    </div>

    <slot
      name="search-dialog"
      :show="isSearchVisible"
      :close="closeSearch"
    >
      <Search
        v-if="showSearch"
        v-model:show="isSearchVisible"
        :theme="theme"
        :placeholder="searchPlaceholder"
        :search-api="searchApi"
        @select="handleSearchSelect"
        @close="handleSearchClose"
      />
    </slot>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NPopover } from 'naive-ui'
import {
  SearchOutline,
  NotificationsOutline,
  SettingsOutline,
  SunnyOutline,
  MoonOutline,
  AppsSharp,
  ArrowBackOutline
} from '@vicons/ionicons5'
import { VAvatar } from '@/components/common'
import { VTooltip } from '@/volur-ui/components/tooltip'
import { useUserStore } from '@/store/modules/user'
import { useNotificationStore } from '@/store/modules/notification'
import { useAppStore } from '@/store/modules/app'
import type { CompetitionSummaryVO } from '@/types/competition'
import type { NotificationVO } from '@/types/notification'
import {
  NoticeCard,
  SettingCard,
  UserCard,
  Search
} from './actions'
import type { SideBarNavFooterProps } from '../types'

defineOptions({
  name: 'SideBarNavFooter'
})

const props = withDefaults(defineProps<SideBarNavFooterProps>(), {
  collapsed: false,
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
  theme: 'light'
})

const emit = defineEmits<{
  (e: 'search-click', ev: MouseEvent): void
  (e: 'search-select', item: CompetitionSummaryVO): void
  (e: 'search-open'): void
  (e: 'search-close'): void
  (e: 'theme-toggle'): void
  (e: 'notification-click', ev: MouseEvent): void
  (e: 'notification-item-click', item: NotificationVO): void
  (e: 'settings-click', ev: MouseEvent): void
  (e: 'user-click', ev: MouseEvent): void
  (e: 'bottom-action-click', ev: MouseEvent): void
  (e: 'action-box-click', ev: MouseEvent): void
  (e: 'switch-account', ev: MouseEvent): void
  (e: 'logout', ev: MouseEvent): void
  (e: 'profile-click', ev: MouseEvent): void
  (e: 'user-card-open'): void
  (e: 'user-card-close'): void
  (e: 'notice-card-open'): void
  (e: 'notice-card-close'): void
  (e: 'setting-card-open'): void
  (e: 'setting-card-close'): void
  (e: 'setting-item-click', key: string): void
  (e: 'admin-click', ev: MouseEvent): void
}>()

const router = useRouter()
const { t, te } = useI18n()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const appStore = useAppStore()

const isSearchVisible = ref<boolean>(false)
const isNoticePopoverShow = ref<boolean>(false)
const isSettingPopoverShow = ref<boolean>(false)
const isUserCardPopoverShow = ref<boolean>(false)

const isAppsPopoverShow = ref<boolean>(false)
const isCollapsedNoticePopoverShow = ref<boolean>(false)
const isCollapsedSettingPopoverShow = ref<boolean>(false)

const isDarkMode = computed<boolean>(() => {
  return props.theme === 'dark' || appStore.theme === 'dark'
})

const unreadCount = computed<number>(() => {
  return notificationStore.unreadCount || 0
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

const userTooltipText = computed<string>(() => {
  if (isLoggedIn.value) {
    return displayName.value ? `${displayName.value} (${userEmail.value})` : userEmail.value
  }
  return te('common.signin') ? t('common.signin') : '登录'
})

function openSearch() {
  isSearchVisible.value = true
  emit('search-open')
}

function handleSearchClick(ev: MouseEvent) {
  emit('search-click', ev)
  openSearch()
}

function handleSearchClose() {
  closeSearch()
}

function closeSearch() {
  isSearchVisible.value = false
  emit('search-close')
}

function handleSearchSelect(item: CompetitionSummaryVO) {
  emit('search-select', item)
  isSearchVisible.value = false
  emit('search-close')
  if (item.id) {
    router.push(`/competitions/${item.id}`).catch(() => {})
  }
}

watch(isNoticePopoverShow, (val) => {
  if (val) {
    isSettingPopoverShow.value = false
    isUserCardPopoverShow.value = false
    emit('notice-card-open')
  } else {
    emit('notice-card-close')
  }
})

watch(isSettingPopoverShow, (val) => {
  if (val) {
    isNoticePopoverShow.value = false
    isUserCardPopoverShow.value = false
    emit('setting-card-open')
  } else {
    emit('setting-card-close')
  }
})

watch(isUserCardPopoverShow, (val) => {
  if (val) {
    isNoticePopoverShow.value = false
    isSettingPopoverShow.value = false
    emit('user-card-open')
  } else {
    emit('user-card-close')
  }
})

watch(isCollapsedNoticePopoverShow, (val) => {
  if (val) {
    isCollapsedSettingPopoverShow.value = false
  }
})

watch(isCollapsedSettingPopoverShow, (val) => {
  if (val) {
    isCollapsedNoticePopoverShow.value = false
  }
})

function handleNotificationClick(ev: MouseEvent) {
  emit('notification-click', ev)
}

function closeNoticePopover() {
  isNoticePopoverShow.value = false
}

function handleSettingsClick(ev: MouseEvent) {
  emit('settings-click', ev)
}

function closeSettingPopover() {
  isSettingPopoverShow.value = false
}

function handleUserClick(ev: MouseEvent) {
  emit('user-click', ev)
  if (!isLoggedIn.value) {
    if (props.signinRoute) {
      router.push(props.signinRoute).catch(() => {})
    }
  }
}

function closeUserCardPopover() {
  isUserCardPopoverShow.value = false
}

function handleActionBoxClick(ev: MouseEvent) {
  emit('action-box-click', ev)
}

function handleCollapsedSearchClick(ev: MouseEvent) {
  isAppsPopoverShow.value = false
  handleSearchClick(ev)
}

function handleCollapsedNoticeClick() {}

function closeCollapsedNoticePopover() {
  isCollapsedNoticePopoverShow.value = false
}

function handleCollapsedSettingClick() {}

function closeCollapsedSettingPopover() {
  isCollapsedSettingPopoverShow.value = false
}

function handleBottomActionClick(ev: MouseEvent) {
  emit('bottom-action-click', ev)
  if (props.bottomActionRoute) {
    router.push(props.bottomActionRoute).catch(() => {})
  }
}

function handleThemeToggle() {
  emit('theme-toggle')
}

defineExpose({
  openSearch,
  closeSearch,
  isSearchVisible,
  isNoticePopoverShow,
  isSettingPopoverShow,
  isUserCardPopoverShow,
  isAppsPopoverShow,
  closeNoticePopover,
  closeSettingPopover,
  closeUserCardPopover
})
</script>

<style scoped lang="scss">
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
  overflow: visible;
  transition: padding 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &.is-collapsed {
    padding: 8px 0 12px;
    align-items: center;
  }

  .sidebar-action-box {
    width: 100%;
    box-sizing: border-box;
    overflow: visible;
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
        border: 1px solid var(--color-v1-outline-var);
        background: transparent;
        color: var(--color-v1-text-var);
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        outline: none;
        box-sizing: border-box;
        transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
                    color 0.2s cubic-bezier(0.2, 0, 0, 1),
                    border-color 0.2s ease;

        &:hover,
        &.is-active {
          color: var(--color-v1-text);
          background-color: var(--color-v1-button-container-high);
          border-color: var(--color-v1-outline);
        }

        &:active {
          background-color: rgba(60, 64, 67, 0.12);
        }

        .action-icon {
          width: 18px;
          height: 18px;
          display: block;
        }

        .action-icon-wrap {
          position: relative;
          display: flex;
          align-items: center;
          justify-content: center;

          .action-badge-dot {
            position: absolute;
            top: -2px;
            right: -2px;
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background-color: var(--color-v1-primary-highest);
          }
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
        border: 1px solid var(--color-v1-outline-var);
        background: transparent;
        color: var(--color-v1-text-var);
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        outline: none;
        box-sizing: border-box;
        transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
                    color 0.2s cubic-bezier(0.2, 0, 0, 1),
                    border-color 0.2s ease;

        &:hover,
        &.is-active {
          color: var(--color-v1-text);
          background-color: var(--color-v1-button-container-high);
          border-color: var(--color-v1-outline);
        }

        &:active {
          background-color: rgba(60, 64, 67, 0.12);
        }

        .action-icon {
          width: 18px;
          height: 18px;
          display: block;
        }

        .action-icon-wrap {
          position: relative;
          display: flex;
          align-items: center;
          justify-content: center;

          .action-badge-dot {
            position: absolute;
            top: -2px;
            right: -2px;
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background-color: var(--color-v1-primary-highest);
          }
        }
      }
    }
  }

  .sidebar-user-box {
    width: 100%;
    box-sizing: border-box;
    display: flex;
    justify-content: center;
    overflow: visible;

    &.is-collapsed {
      width: 38px;
    }

    .user-card {
      display: flex;
      align-items: center;
      width: 100%;
      padding: 6px 10px;
      border-radius: 12px;
      border: 1px solid var(--color-v1-outline-var);
      background-color: transparent;
      cursor: pointer;
      user-select: none;
      overflow: hidden;
      box-sizing: border-box;
      transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
                  height 0.25s cubic-bezier(0.4, 0, 0.2, 1),
                  padding 0.25s cubic-bezier(0.4, 0, 0.2, 1),
                  background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
                  border-color 0.2s ease;

      &:hover,
      &.is-active {
        background-color: var(--color-v1-button-container-high);
        border-color: var(--color-v1-outline);

        .user-info-text .user-name {
          color: var(--color-v1-text);
        }

        :deep(.v-avatar.has-ring::after) {
          border-color: var(--color-v1-primary-highest);
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
        transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
                    height 0.25s cubic-bezier(0.4, 0, 0.2, 1);
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
        transition: max-width 0.25s cubic-bezier(0.4, 0, 0.2, 1),
                    opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1),
                    margin-left 0.25s cubic-bezier(0.4, 0, 0.2, 1);

        .user-name {
          font-size: 12px;
          font-weight: 500;
          color: var(--color-v1-text-var);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          line-height: 1.3;
        }

        .user-desc {
          font-size: 12px;
          color: var(--color-v1-text-disable);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          line-height: 1.3;
        }
      }

      &.is-collapsed {
        width: 38px;
        height: 38px;
        padding: 0;
        justify-content: center;

        .user-avatar-wrap {
          width: 26px;
          height: 26px;
        }

        .user-info-text {
          max-width: 0;
          opacity: 0;
          margin-left: 0;
          pointer-events: none;
        }
      }
    }
  }

  .sidebar-bottom-action {
    width: 100%;
    display: flex;
    justify-content: center;
  }
}

.dock-apps-popover {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 8px 6px;
  background-color: light-dark(#ffffff, #292a2d);
  border: 1px solid var(--color-v1-outline-var);
  border-radius: 16px;
  box-shadow: var(--v1-shadow-sm);
  box-sizing: border-box;

  .dock-tool-btn {
    width: 38px;
    height: 38px;
    border-radius: 12px;
    border: 1px solid var(--color-v1-outline-var);
    background: transparent;
    color: var(--color-v1-text-var);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    outline: none;
    box-sizing: border-box;
    transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1),
                color 0.2s cubic-bezier(0.2, 0, 0, 1),
                border-color 0.2s ease;

    &:hover,
    &.is-active {
      color: var(--color-v1-text);
      background-color: var(--color-v1-button-container-high);
      border-color: var(--color-v1-outline);
    }

    &:active {
      background-color: rgba(60, 64, 67, 0.12);
    }

    .action-icon {
      width: 18px;
      height: 18px;
      display: block;
    }

    .action-icon-wrap {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;

      .action-badge-dot {
        position: absolute;
        top: -2px;
        right: -2px;
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background-color: var(--color-v1-primary-highest);
      }
    }
  }
}
</style>
