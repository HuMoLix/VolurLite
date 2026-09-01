<template>
  <n-layout class="app-layout" has-sider>
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="220"
      :collapsed="appStore.isSidebarCollapsed"
      show-trigger
      @collapse="appStore.isSidebarCollapsed = true"
      @expand="appStore.isSidebarCollapsed = false"
      content-style="display: flex; flex-direction: column; height: 100%; justify-content: space-between;"
    >
      <div class="sidebar-top">
        <div class="logo-container" @click="router.push('/competitions')">
          <transition name="logo-fade" mode="out-in">
            <img
              v-if="!appStore.isSidebarCollapsed"
              :key="`expanded-${appStore.theme}`"
              :src="appStore.theme === 'dark' ? '/long-logo-dark.svg' : '/long-logo.svg'"
              alt="Volur Lite"
              class="logo-img-expanded"
            />
            <img
              v-else
              :key="`collapsed-${appStore.theme}`"
              :src="appStore.theme === 'dark' ? '/favicon-dark.svg' : '/favicon.svg'"
              alt="Volur Lite"
              class="logo-img-collapsed"
            />
          </transition>
        </div>
        <n-menu
          :collapsed="appStore.isSidebarCollapsed"
          :collapsed-width="64"
          :collapsed-icon-size="22"
          :options="menuOptions"
          :value="activeKey"
          @update:value="handleMenuSelect"
        />
      </div>

      <!-- Admin Panel Button at sidebar bottom for Admin users -->
      <div v-if="userStore.isAdmin" class="sidebar-bottom-admin">
        <transition name="admin-fade" mode="out-in">
          <div v-if="appStore.isSidebarCollapsed" key="collapsed" class="admin-wrapper-collapsed">
            <n-tooltip placement="right" trigger="hover">
              <template #trigger>
                <n-button
                  quaternary
                  circle
                  size="large"
                  class="admin-panel-btn-collapsed"
                  @click="handleAdminPanelClick"
                >
                  <template #icon>
                    <n-icon size="20">
                      <ShieldCheckmarkOutline />
                    </n-icon>
                  </template>
                </n-button>
              </template>
              {{ t('layout.admin_panel') }}
            </n-tooltip>
          </div>

          <div v-else key="expanded" class="admin-wrapper-expanded">
            <n-button
              quaternary
              block
              size="medium"
              class="admin-panel-btn-expanded"
              @click="handleAdminPanelClick"
            >
              <template #icon>
                <n-icon size="18">
                  <ShieldCheckmarkOutline />
                </n-icon>
              </template>
              {{ t('layout.admin_panel') }}
            </n-button>
          </div>
        </transition>
      </div>
    </n-layout-sider>

    <n-layout>
      <n-layout-header bordered class="app-header">
        <div class="header-left">
          <n-breadcrumb>
            <n-breadcrumb-item>{{ systemStore.platformName }}</n-breadcrumb-item>
            <n-breadcrumb-item>{{ currentRouteTitle }}</n-breadcrumb-item>
          </n-breadcrumb>
        </div>
        <div class="header-right">
          <n-button
            quaternary
            circle
            @click="toggleLanguage"
            :title="locale === 'zh_CN' ? t('common.switch_to_english') : t('common.switch_to_chinese')"
          >
            <template #icon>
              <span style="font-size: 13px; font-weight: 600;">{{ locale === 'zh_CN' ? 'EN' : '中' }}</span>
            </template>
          </n-button>

          <n-button
            quaternary
            circle
            @click="appStore.toggleTheme"
            :title="appStore.theme === 'dark' ? t('common.switch_light_mode') : t('common.switch_dark_mode')"
          >
            <template #icon>
              <n-icon size="18">
                <SunnyOutline v-if="appStore.theme === 'dark'" />
                <MoonOutline v-else />
              </n-icon>
            </template>
          </n-button>

          <template v-if="userStore.isLoggedIn">
            <!-- In-App Notification Center to the left of User Profile -->
            <NotificationCenter />

            <n-dropdown :options="userDropdownOptions" @select="handleUserDropdown">
              <div class="user-profile cursor-pointer">
                <UserAvatar
                  size="small"
                  :src="userStore.user?.avatarUrl"
                  :username="userStore.user?.username"
                />
                <span class="user-nickname">{{ userStore.user?.username }}</span>
              </div>
            </n-dropdown>
          </template>
          <template v-else>
            <n-button type="primary" size="small" @click="router.push('/auth/login')">
              {{ t('layout.login_register') }}
            </n-button>
          </template>
        </div>
      </n-layout-header>

      <n-layout-content content-style="padding: 24px;" class="app-content">
        <router-view v-slot="{ Component, route: currentRoute }">
          <transition name="fade-fast">
            <component :is="Component" :key="currentRoute.fullPath" />
          </transition>
        </router-view>
      </n-layout-content>

      <n-layout-footer bordered class="app-footer">
        <div class="footer-content">
          <span>© {{ currentYear }} {{ systemStore.platformName }}. {{ systemStore.platformDesc }}</span>
          <a
            v-if="systemStore.icpNumber"
            href="https://beian.miit.gov.cn/"
            target="_blank"
            rel="noopener noreferrer"
            class="icp-link"
          >
            {{ systemStore.icpNumber }}
          </a>
        </div>
      </n-layout-footer>
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NLayout,
  NLayoutSider,
  NLayoutHeader,
  NLayoutContent,
  NLayoutFooter,
  NMenu,
  NBreadcrumb,
  NBreadcrumbItem,
  NButton,
  NDropdown,
  NIcon,
  NTooltip,
  type MenuOption,
  type DropdownOption
} from 'naive-ui'
import {
  SunnyOutline,
  MoonOutline,
  TrophyOutline,
  MegaphoneOutline,
  ShieldCheckmarkOutline
} from '@vicons/ionicons5'
import NotificationCenter from '@/components/notification/NotificationCenter.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { useSystemStore } from '@/store/modules/system'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const route = useRoute()
const { t, locale } = useI18n()
const appStore = useAppStore()
const userStore = useUserStore()
const systemStore = useSystemStore()
const currentYear = new Date().getFullYear()

function toggleLanguage() {
  const next = locale.value === 'zh_CN' ? 'en_US' : 'zh_CN'
  locale.value = next
  localStorage.setItem('locale', next)
}

const currentRouteTitle = computed(() => {
  if (route.meta.titleKey) {
    return t(route.meta.titleKey as string)
  }
  return (route.meta.title as string) || ''
})

const activeKey = computed(() => (route.name as string) || 'Competitions')

const menuOptions = computed<MenuOption[]>(() => [
  {
    label: () => t('layout.competitions'),
    key: 'Competitions',
    icon: () => h(NIcon, null, { default: () => h(TrophyOutline) })
  },
  {
    label: () => t('layout.announcements'),
    key: 'Announcements',
    icon: () => h(NIcon, null, { default: () => h(MegaphoneOutline) })
  }
])

const userDropdownOptions = computed<DropdownOption[]>(() => [
  { label: () => t('layout.profile'), key: 'profile' },
  { label: () => t('layout.logout'), key: 'logout' }
])

function handleMenuSelect(key: string) {
  if (route.name === key) return
  router.push({ name: key }).catch(() => {})
}

function handleAdminPanelClick() {
  router.push('/admin/dashboard')
}

async function handleUserDropdown(key: string) {
  if (key === 'profile') {
    router.push('/profile')
  } else if (key === 'logout') {
    await userStore.logout()
    router.push('/auth/login')
  }
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.sidebar-top {
  display: flex;
  flex-direction: column;
}

.logo-container {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  cursor: pointer;
  overflow: hidden;
  position: relative;

  .logo-img-expanded {
    height: 26px;
    width: auto;
    max-width: 160px;
    object-fit: contain;
  }

  .logo-img-collapsed {
    width: 32px;
    height: 32px;
    object-fit: contain;
  }
}

.sidebar-bottom-admin {
  padding: 12px 10px;
  border-top: 1px solid var(--v-border-base, #efeff5);
  display: flex;
  justify-content: center;
  overflow: hidden;

  .admin-wrapper-expanded {
    width: 100%;
  }

  .admin-wrapper-collapsed {
    display: flex;
    justify-content: center;
  }

  .admin-panel-btn-expanded {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    font-weight: 500;
    color: var(--v-text-desc, #444444);
    border-radius: 6px;
    transition: color 0.2s ease, background-color 0.2s ease;

    &:hover {
      color: var(--v-text-primary, #111111);
      background-color: rgba(0, 0, 0, 0.04);
    }

    @include theme-dark {
      &:hover {
        background-color: rgba(255, 255, 255, 0.08);
      }
    }
  }

  .admin-panel-btn-collapsed {
    color: var(--v-text-desc, #444444);
    transition: color 0.2s ease, background-color 0.2s ease;

    &:hover {
      color: var(--v-text-primary, #111111);
      background-color: rgba(0, 0, 0, 0.04);
    }

    @include theme-dark {
      &:hover {
        background-color: rgba(255, 255, 255, 0.08);
      }
    }
  }
}

.logo-fade-enter-active,
.logo-fade-leave-active,
.admin-fade-enter-active,
.admin-fade-leave-active {
  transition: opacity 0.22s cubic-bezier(0.4, 0, 0.2, 1), transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

.logo-fade-enter-from,
.admin-fade-enter-from {
  opacity: 0;
  transform: scale(0.88);
}

.logo-fade-leave-to,
.admin-fade-leave-to {
  opacity: 0;
  transform: scale(0.88);
}

.app-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background-color: var(--n-color, #ffffff);

  .header-left {
    display: flex;
    align-items: center;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .user-profile {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      padding: 4px 8px;
      border-radius: 6px;
      cursor: pointer;
      user-select: none;
      transition: background-color 0.2s ease;

      :deep(.n-avatar) {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      .user-nickname {
        font-size: 13px;
        font-weight: 500;
        color: var(--v-text-primary, #111111);
        line-height: 1;
        display: inline-flex;
        align-items: center;
      }

      &:hover {
        background-color: rgba(0, 0, 0, 0.04);
      }
    }
  }
}

.app-content {
  min-height: calc(100vh - 64px - 48px);
  background-color: var(--v-bg-page, #f8f9fa);
  overflow-y: auto;
}

.app-footer {
  background-color: var(--n-color, #ffffff);
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 24px;
  box-sizing: border-box;

  .footer-content {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: center;
    gap: 16px;
    font-size: 13px;
    color: #444444;

    .icp-link {
      color: #444444;
      text-decoration: none;
      transition: color 0.2s ease;

      &:hover {
        color: #000000;
        text-decoration: underline;
      }
    }
  }
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.2s ease;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.fade-fast-enter-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.fade-fast-enter-from {
  opacity: 0;
  transform: translateY(4px);
}
</style>
