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

    <n-layout class="admin-main-layout">
      <volur-header mode="admin" />

      <n-layout-content content-style="padding: 24px;" class="admin-content">
        <router-view v-slot="{ Component, route: currentRoute }">
          <transition name="fade-fast" mode="out-in">
            <component :is="Component" :key="currentRoute.fullPath" />
          </transition>
        </router-view>
      </n-layout-content>

      <volur-footer mode="admin" />
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { computed, h, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NLayout,
  NLayoutContent,
  NIcon,
  useNotification,
  type MenuOption
} from 'naive-ui'
import VolurSidebar from '../components/VolurSidebar.vue'
import VolurHeader from '../components/VolurHeader.vue'
import VolurFooter from '../components/VolurFooter.vue'
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
  ServerOutline,
  HardwareChipOutline,
  LayersOutline,
  TimeOutline
} from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import { useI18n } from 'vue-i18n'
import { getGlobalSseClient } from '@/utils/sse'
import type { AdminAsyncTaskVO } from '@/types/task'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const appStore = useAppStore()

const activeKey = computed(() => {
  const name = (route.name as string) || ''
  if (name.startsWith('AdminCompetition')) return 'AdminCompetitions'
  if (name.startsWith('AdminCtfChallenge')) return 'AdminCtfChallenges'
  return name || 'AdminDashboard'
})

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

function handleMenuSelect(key: string) {
  if (route.name === key) return
  router.push({ name: key }).catch(() => {})
}

const notification = useNotification()
let unsubTaskEvent: (() => void) | null = null

onMounted(() => {
  try {
    const sse = getGlobalSseClient()
    unsubTaskEvent = sse.on('task_event', (data: AdminAsyncTaskVO) => {
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
    sse.connect()
  } catch (e) {
    console.warn('[AdminLayout] SSE connect error', e)
  }
})

onUnmounted(() => {
  unsubTaskEvent?.()
  unsubTaskEvent = null
})
</script>

<style scoped lang="scss">
.admin-layout {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: var(--v-bg-base);
  transition: background-color 0.25s ease;
}

.admin-main-layout {
  background-color: var(--v-bg-base);
  transition: background-color 0.25s ease;
}

.admin-content {
  background-color: var(--v-bg-base);
  overflow-y: auto;
  transition: background-color 0.25s ease;
}



.fade-fast-enter-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.fade-fast-enter-from {
  opacity: 0;
  transform: translateY(4px);
}
</style>
