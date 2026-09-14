<template>
  <n-layout class="admin-layout" has-sider>
    <volur-sidebar
      v-model:collapsed="appStore.isSidebarCollapsed"
      :menu-options="menuOptions"
      :value="activeKey"
      logo-route="/admin/dashboard"
      show-bottom-action
      :bottom-action-text="t('layout.return_to_competitions')"
      :bottom-action-collapsed-text="t('layout.return_to_platform')"
      :bottom-action-icon="ArrowBackOutline"
      bottom-action-route="/competitions"
      @select="handleMenuSelect"
    />

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
              <UserAvatar
                size="small"
                :src="userStore.user?.avatarUrl"
                :username="userStore.user?.username || 'Admin'"
              />
              <span class="user-nickname">{{ userStore.user?.username || 'Admin' }}</span>
            </div>
          </n-dropdown>
        </div>
      </n-layout-header>

      <n-layout-content content-style="padding: 24px;" class="admin-content">
        <router-view v-slot="{ Component, route: currentRoute }">
          <transition name="fade-fast" mode="out-in">
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
import { computed, h, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NLayout,
  NLayoutHeader,
  NLayoutContent,
  NLayoutFooter,
  NBreadcrumb,
  NBreadcrumbItem,
  NButton,
  NDropdown,
  NIcon,
  useNotification,
  type MenuOption,
  type DropdownOption
} from 'naive-ui'
import VolurSidebar from '@/public/layout/components/VolurSidebar.vue'
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
  MailOutline,
  SettingsOutline,
  ArrowBackOutline,
  SunnyOutline,
  MoonOutline,
  ServerOutline,
  HardwareChipOutline,
  LayersOutline,
  TimeOutline
} from '@vicons/ionicons5'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { useSystemStore } from '@/store/modules/system'
import { useI18n } from 'vue-i18n'
import { SseClient } from '@/utils/sse'
import type { AdminAsyncTaskVO } from '@/types/task'

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
    label: () => t('layout.menu.clusters'),
    key: 'AdminClusterGroup',
    icon: () => h(NIcon, null, { default: () => h(ServerOutline) }),
    children: [
      {
        label: () => t('layout.menu.cluster_settings'),
        key: 'AdminClusterSettings',
        icon: () => h(NIcon, null, { default: () => h(HardwareChipOutline) })
      },
      {
        label: () => t('layout.menu.containers'),
        key: 'AdminContainers',
        icon: () => h(NIcon, null, { default: () => h(CubeOutline) })
      },
      {
        label: () => t('layout.menu.images'),
        key: 'AdminImages',
        icon: () => h(NIcon, null, { default: () => h(LayersOutline) })
      }
    ]
  },
  {
    label: () => t('layout.menu.logs'),
    key: 'AdminLogs',
    icon: () => h(NIcon, null, { default: () => h(ReaderOutline) })
  },
  {
    label: () => t('layout.menu.tasks'),
    key: 'AdminTasks',
    icon: () => h(NIcon, null, { default: () => h(TimeOutline) })
  },
  {
    label: () => t('layout.menu.emails'),
    key: 'AdminEmails',
    icon: () => h(NIcon, null, { default: () => h(MailOutline) })
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
    await router.push('/competitions')
  } else if (key === 'logout') {
    await userStore.logout()
    await router.push('/auth/login')
  }
}

const notification = useNotification()
let adminSseClient: SseClient | null = null

onMounted(() => {
  try {
    adminSseClient = new SseClient('/api/v1/realtime/stream')
    adminSseClient.on('task_event', (data: AdminAsyncTaskVO) => {
      if (!data) return

      if (data.status === 'SUCCESS') {
        const durationText = data.executionDurationMs
          ? ` (耗时: ${(data.executionDurationMs / 1000).toFixed(1)}s)`
          : ''
        notification.success({
          title: t('admin.tasks.toast.completed'),
          content: `${data.taskName}${durationText}`,
          duration: 4500
        })
        window.dispatchEvent(new CustomEvent('volur:async-task-completed', { detail: data }))
      } else if (data.status === 'FAILED') {
        notification.error({
          title: t('admin.tasks.toast.failed'),
          content: `${data.taskName}: ${data.errorMessage || '未知异常'}`,
          duration: 6000
        })
        window.dispatchEvent(new CustomEvent('volur:async-task-failed', { detail: data }))
      }
    })
    adminSseClient.connect()
  } catch (e) {
    console.warn('[AdminLayout] SSE connect error', e)
  }
})

onUnmounted(() => {
  adminSseClient?.close()
  adminSseClient = null
})
</script>

<style scoped lang="scss">
.admin-layout {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
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
