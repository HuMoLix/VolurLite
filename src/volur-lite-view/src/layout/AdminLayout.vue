<template>
  <n-layout class="admin-layout" has-sider>
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="230"
      :collapsed="appStore.isSidebarCollapsed"
      show-trigger
      @collapse="appStore.isSidebarCollapsed = true"
      @expand="appStore.isSidebarCollapsed = false"
      content-style="display: flex; flex-direction: column; height: 100%; justify-content: space-between;"
      class="admin-sider"
    >
      <div class="sidebar-top">
        <div class="logo-container" @click="router.push('/admin/dashboard')">
          <transition name="logo-fade" mode="out-in">
            <div
              v-if="!appStore.isSidebarCollapsed"
              :key="`expanded-${appStore.theme}`"
              class="logo-expanded-wrapper"
            >
              <img
                :src="appStore.theme === 'dark' ? '/long-logo-dark.svg' : '/long-logo.svg'"
                alt="Volur Lite"
                class="logo-img-expanded"
              />
              <n-tag size="small" :bordered="false" type="warning" class="admin-badge">
                ADMIN
              </n-tag>
            </div>
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
          class="admin-menu"
        />
      </div>

      <!-- Bottom return to platform entrance -->
      <div class="sidebar-bottom-return">
        <transition name="btn-fade" mode="out-in">
          <div v-if="appStore.isSidebarCollapsed" key="collapsed" class="return-wrapper-collapsed">
            <n-tooltip placement="right" trigger="hover">
              <template #trigger>
                <n-button
                  quaternary
                  circle
                  size="large"
                  class="return-btn-collapsed"
                  @click="router.push('/competitions')"
                >
                  <template #icon>
                    <n-icon size="20">
                      <ArrowBackOutline />
                    </n-icon>
                  </template>
                </n-button>
              </template>
              {{ t('layout.return_to_platform') }}
            </n-tooltip>
          </div>

          <div v-else key="expanded" class="return-wrapper-expanded">
            <n-button
              quaternary
              block
              size="medium"
              class="return-btn-expanded"
              @click="router.push('/competitions')"
            >
              <template #icon>
                <n-icon size="18">
                  <ArrowBackOutline />
                </n-icon>
              </template>
              {{ t('layout.return_to_competitions') }}
            </n-button>
          </div>
        </transition>
      </div>
    </n-layout-sider>

    <n-layout>
      <n-layout-header bordered class="admin-header">
        <div class="header-left">
          <n-breadcrumb>
            <n-breadcrumb-item>{{ t('layout.admin_portal') }}</n-breadcrumb-item>
            <n-breadcrumb-item v-if="parentRouteTitle">{{ parentRouteTitle }}</n-breadcrumb-item>
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

          <n-dropdown :options="userDropdownOptions" @select="handleUserDropdown">
            <div class="user-profile cursor-pointer">
              <n-avatar round size="small" :src="userStore.user?.avatar || undefined">
                {{ userStore.user?.nickname?.charAt(0) || userStore.user?.username?.charAt(0) || 'A' }}
              </n-avatar>
              <span class="user-nickname">{{ userStore.user?.nickname || userStore.user?.username || 'Admin' }}</span>
            </div>
          </n-dropdown>
        </div>
      </n-layout-header>

      <n-layout-content content-style="padding: 24px;" class="admin-content">
        <router-view v-slot="{ Component, route: currentRoute }">
          <transition name="fade-fast">
            <component :is="Component" :key="currentRoute.fullPath" />
          </transition>
        </router-view>
      </n-layout-content>

      <n-layout-footer bordered class="admin-footer">
        <div class="footer-content">
          <span>{{ systemStore.platformName }} Management Control System © {{ currentYear }}</span>
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
  NAvatar,
  NDropdown,
  NIcon,
  NTooltip,
  NTag,
  type MenuOption,
  type DropdownOption
} from 'naive-ui'
import {
  SpeedometerOutline,
  PeopleOutline,
  LibraryOutline,
  FlagOutline,
  DocumentTextOutline,
  TerminalOutline,
  ShieldOutline,
  ShieldCheckmarkOutline,
  TrophyOutline,
  MegaphoneOutline,
  CubeOutline,
  ReaderOutline,
  SettingsOutline,
  ArrowBackOutline,
  SunnyOutline,
  MoonOutline
} from '@vicons/ionicons5'
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
  return (route.meta.title as string) || t('layout.menu.dashboard')
})

const parentRouteTitle = computed(() => {
  if (route.meta.parentTitleKey) {
    return t(route.meta.parentTitleKey as string)
  }
  return (route.meta.parentTitle as string) || ''
})

const activeKey = computed(() => (route.name as string) || 'AdminDashboard')

const menuOptions = computed<MenuOption[]>(() => [
  {
    label: () => t('layout.menu.dashboard'),
    key: 'AdminDashboard',
    icon: () => h(NIcon, null, { default: () => h(SpeedometerOutline) })
  },
  {
    label: () => t('layout.menu.users'),
    key: 'AdminUsers',
    icon: () => h(NIcon, null, { default: () => h(PeopleOutline) })
  },
  {
    label: () => t('layout.menu.challenges'),
    key: 'AdminChallengesGroup',
    icon: () => h(NIcon, null, { default: () => h(LibraryOutline) }),
    children: [
      {
        label: () => t('layout.menu.ctf_challenges'),
        key: 'AdminCtfChallenges',
        icon: () => h(NIcon, null, { default: () => h(FlagOutline) })
      },
      {
        label: () => t('layout.menu.theory_challenges'),
        key: 'AdminTheoryChallenges',
        icon: () => h(NIcon, null, { default: () => h(DocumentTextOutline) })
      },
      {
        label: () => t('layout.menu.isw_challenges'),
        key: 'AdminIswChallenges',
        icon: () => h(NIcon, null, { default: () => h(TerminalOutline) })
      },
      {
        label: () => t('layout.menu.awd_challenges'),
        key: 'AdminAwdChallenges',
        icon: () => h(NIcon, null, { default: () => h(ShieldOutline) })
      },
      {
        label: () => t('layout.menu.awd_plus_challenges'),
        key: 'AdminAwdPlusChallenges',
        icon: () => h(NIcon, null, { default: () => h(ShieldCheckmarkOutline) })
      }
    ]
  },
  {
    label: () => t('layout.menu.competitions'),
    key: 'AdminCompetitions',
    icon: () => h(NIcon, null, { default: () => h(TrophyOutline) })
  },
  {
    label: () => t('layout.menu.broadcasts'),
    key: 'AdminBroadcasts',
    icon: () => h(NIcon, null, { default: () => h(MegaphoneOutline) })
  },
  {
    label: () => t('layout.menu.containers'),
    key: 'AdminContainers',
    icon: () => h(NIcon, null, { default: () => h(CubeOutline) })
  },
  {
    label: () => t('layout.menu.logs'),
    key: 'AdminLogs',
    icon: () => h(NIcon, null, { default: () => h(ReaderOutline) })
  },
  {
    label: () => t('layout.menu.system'),
    key: 'AdminSystem',
    icon: () => h(NIcon, null, { default: () => h(SettingsOutline) })
  }
])

const userDropdownOptions = computed<DropdownOption[]>(() => [
  { label: () => t('layout.return_to_platform'), key: 'frontend' },
  { label: () => t('layout.logout'), key: 'logout' }
])

function handleMenuSelect(key: string) {
  if (route.name === key) return
  router.push({ name: key }).catch(() => {})
}

async function handleUserDropdown(key: string) {
  if (key === 'frontend') {
    router.push('/competitions')
  } else if (key === 'logout') {
    await userStore.logout()
    router.push('/auth/login')
  }
}
</script>

<style scoped lang="scss">
.admin-layout {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.admin-sider {
  background-color: var(--n-color, #ffffff);
  z-index: 10;

  .sidebar-top {
    display: flex;
    flex-direction: column;
  }

  .logo-container {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;
    cursor: pointer;
    border-bottom: 1px solid var(--v-border-base, #efeff5);

    .logo-expanded-wrapper {
      display: flex;
      align-items: center;
      gap: 8px;

      .logo-img-expanded {
        height: 28px;
        object-fit: contain;
      }

      .admin-badge {
        font-weight: 700;
        letter-spacing: 0.5px;
        font-size: 10px;
        padding: 0 4px;
      }
    }

    .logo-img-collapsed {
      width: 28px;
      height: 28px;
    }
  }

  .sidebar-bottom-return {
    padding: 16px;
    border-top: 1px solid var(--v-border-base, #efeff5);

    .return-wrapper-collapsed {
      display: flex;
      justify-content: center;

      .return-btn-collapsed {
        color: #444444;

        &:hover {
          color: #000000;
          background-color: rgba(0, 0, 0, 0.05);
        }
      }
    }

    .return-wrapper-expanded {
      .return-btn-expanded {
        font-size: 13px;
        font-weight: 500;
        color: #444444;
        border-radius: 6px;

        &:hover {
          color: #000000;
          background-color: rgba(0, 0, 0, 0.05);
        }
      }
    }
  }
}

.admin-header {
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
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 4px 8px;
      border-radius: 6px;
      transition: background-color 0.2s ease;

      &:hover {
        background-color: rgba(0, 0, 0, 0.04);
      }

      .user-nickname {
        font-size: 13px;
        font-weight: 500;
        color: var(--v-text-primary, #111111);
      }
    }
  }
}

.admin-content {
  background-color: var(--v-bg-page, #f8f9fa);
  overflow-y: auto;
}

.admin-footer {
  background-color: var(--n-color, #ffffff);
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

.fade-fast-enter-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.fade-fast-enter-from {
  opacity: 0;
  transform: translateY(4px);
}
</style>
