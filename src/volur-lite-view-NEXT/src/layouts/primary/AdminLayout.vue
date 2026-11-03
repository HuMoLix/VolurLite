<template>
  <div class="admin-layout h-screen max-h-screen flex overflow-hidden bg-google-surface-light dark:bg-google-surface-dark transition-colors duration-300">
    <VSideBar
      ref="sideBarRef"
      v-model:collapsed="isCollapsed"
      :value="activeKey"
      :groups="adminNavGroups"
      :show-logo="true"
      :logo-route="'/admin/dashboard'"
      :show-bottom-action="true"
      :bottom-action-text="t('admin.layout.return_platform', '返回前台')"
      :bottom-action-collapsed-text="t('admin.layout.return_short', '返回')"
      :bottom-action-icon="ArrowBackOutline"
      :bottom-action-route="'/competitions'"
      :show-search="false"
      :show-notifications="true"
      :show-settings="true"
      :show-user-profile="true"
      @select="handleSelect"
    />

    <div
      ref="adminMainRef"
      class="admin-main flex-1 flex flex-col min-w-0 h-full overflow-y-auto"
    >
      <main class="admin-content flex-1 p-6 md:p-8 max-w-[100rem] w-full mx-auto">
        <router-view v-slot="{ Component }">
          <transition name="fade-fast" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  SpeedometerOutline,
  PeopleOutline,
  LibraryOutline,
  FlagOutline,
  TerminalOutline,
  ShieldOutline,
  ShieldCheckmarkOutline,
  TrophyOutline,
  MegaphoneOutline,
  HardwareChipOutline,
  CubeOutline,
  LayersOutline,
  DocumentTextOutline,
  ListOutline,
  MailOutline,
  OptionsOutline,
  FlashOutline,
  GlobeOutline,
  SettingsOutline,
  ArrowBackOutline
} from '@vicons/ionicons5'
import VSideBar from '@/volur-ui/components/sidebar/VSideBar.vue'
import type { SidebarNavItem, SidebarNavGroup } from '@/volur-ui/components/sidebar'
import { useAppStore } from '@/store/modules/app'

defineOptions({
  name: 'AdminLayout'
})

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const appStore = useAppStore()

const sideBarRef = ref<InstanceType<typeof VSideBar> | null>(null)
const adminMainRef = ref<HTMLElement | null>(null)

watch(
  () => route.path,
  () => {
    adminMainRef.value?.scrollTo({ top: 0, behavior: 'auto' })
  }
)

function scrollToTop(options?: ScrollToOptions) {
  adminMainRef.value?.scrollTo(options ?? { top: 0, behavior: 'smooth' })
  sideBarRef.value?.scrollToTop(options)
}

const isCollapsed = computed<boolean>({
  get() {
    return appStore.isSidebarCollapsed
  },
  set(val: boolean) {
    appStore.isSidebarCollapsed = val
  }
})

const activeKey = computed<string>(() => {
  if (route.path.startsWith('/admin/dashboard')) return 'dashboard'
  if (route.path.startsWith('/admin/users')) return 'users'
  if (route.path.startsWith('/admin/challenges/ctf')) return 'challenge-ctf'
  if (route.path.startsWith('/admin/challenges/theory')) return 'challenge-theory'
  if (route.path.startsWith('/admin/challenges/isw')) return 'challenge-isw'
  if (route.path.startsWith('/admin/challenges/awdp')) return 'challenge-awdp'
  if (route.path.startsWith('/admin/challenges/awd')) return 'challenge-awd'
  if (route.path.startsWith('/admin/competitions')) return 'competitions'
  if (route.path.startsWith('/admin/broadcasts')) return 'broadcasts'
  if (route.path.startsWith('/admin/clusters')) return 'clusters'
  if (route.path.startsWith('/admin/containers')) return 'containers'
  if (route.path.startsWith('/admin/images')) return 'images'
  if (route.path.startsWith('/admin/logs')) return 'logs'
  if (route.path.startsWith('/admin/tasks')) return 'tasks'
  if (route.path.startsWith('/admin/emails')) return 'emails'
  if (route.path.startsWith('/admin/risk/settings')) return 'risk-settings'
  if (route.path.startsWith('/admin/risk/rules')) return 'risk-rules'
  if (route.path.startsWith('/admin/risk/ip')) return 'risk-ip'
  if (route.path.startsWith('/admin/system')) return 'system'
  return 'dashboard'
})

const adminNavGroups = computed<SidebarNavGroup[]>(() => [
  {
    key: 'main',
    titleKey: 'admin.nav.main',
    title: '主要',
    items: [
      {
        key: 'dashboard',
        titleKey: 'admin.dashboard.title',
        label: () => t('admin.dashboard.title', '工作台'),
        route: '/admin/dashboard',
        icon: SpeedometerOutline
      },
      {
        key: 'users',
        titleKey: 'admin.users.title',
        label: () => t('admin.users.title', '用户管理'),
        route: '/admin/users',
        icon: PeopleOutline
      },
      {
        key: 'competitions',
        titleKey: 'admin.competitions.title',
        label: () => t('admin.competitions.title', '竞赛管理'),
        route: '/admin/competitions',
        icon: TrophyOutline
      },
      {
        key: 'broadcasts',
        titleKey: 'admin.broadcasts.title',
        label: () => t('admin.broadcasts.title', '公告管理'),
        route: '/admin/broadcasts',
        icon: MegaphoneOutline
      }
    ]
  },
  {
    key: 'challenges',
    titleKey: 'admin.nav.challenges',
    title: '赛题',
    items: [
      {
        key: 'challenges',
        titleKey: 'admin.challenges.title',
        label: () => t('admin.challenges.title', '题库管理'),
        icon: LibraryOutline,
        children: [
          {
            key: 'challenge-ctf',
            titleKey: 'admin.challenges.ctf_title',
            label: () => t('admin.challenges.ctf_title', 'CTF 题库'),
            route: '/admin/challenges/ctf',
            icon: FlagOutline
          },
          {
            key: 'challenge-theory',
            titleKey: 'admin.challenges.theory_title',
            label: () => t('admin.challenges.theory_title', '理论题库'),
            route: '/admin/challenges/theory',
            icon: DocumentTextOutline
          },
          {
            key: 'challenge-isw',
            titleKey: 'admin.challenges.isw_title',
            label: () => t('admin.challenges.isw_title', 'ISW 题库'),
            route: '/admin/challenges/isw',
            icon: TerminalOutline
          },
          {
            key: 'challenge-awd',
            titleKey: 'admin.challenges.awd_title',
            label: () => t('admin.challenges.awd_title', 'AWD 题库'),
            route: '/admin/challenges/awd',
            icon: ShieldOutline
          },
          {
            key: 'challenge-awdp',
            titleKey: 'admin.challenges.awd_plus_title',
            label: () => t('admin.challenges.awd_plus_title', 'AWD+ 题库'),
            route: '/admin/challenges/awdp',
            icon: ShieldCheckmarkOutline
          }
        ]
      }
    ]
  },
  {
    key: 'dispatch',
    titleKey: 'admin.nav.dispatch',
    title: '调度',
    items: [
      {
        key: 'clusters',
        titleKey: 'admin.clusters.title',
        label: () => t('admin.clusters.title', '集群设置'),
        route: '/admin/clusters',
        icon: HardwareChipOutline
      },
      {
        key: 'containers',
        titleKey: 'admin.containers.title',
        label: () => t('admin.containers.title', '容器管理'),
        route: '/admin/containers',
        icon: CubeOutline
      },
      {
        key: 'images',
        titleKey: 'admin.images.title',
        label: () => t('admin.images.title', '镜像管理'),
        route: '/admin/images',
        icon: LayersOutline
      },
      {
        key: 'logs',
        titleKey: 'admin.logs.title',
        label: () => t('admin.logs.title', '系统日志'),
        route: '/admin/logs',
        icon: DocumentTextOutline
      },
      {
        key: 'tasks',
        titleKey: 'admin.tasks.title',
        label: () => t('admin.tasks.title', '任务管理'),
        route: '/admin/tasks',
        icon: ListOutline
      },
      {
        key: 'emails',
        titleKey: 'admin.emails.title',
        label: () => t('admin.emails.title', '邮件管理'),
        route: '/admin/emails',
        icon: MailOutline
      }
    ]
  },
  {
    key: 'security',
    titleKey: 'admin.nav.security',
    title: '安全',
    items: [
      {
        key: 'risk-group',
        titleKey: 'admin.risk.title',
        label: () => t('admin.risk.title', '风控管理'),
        icon: ShieldCheckmarkOutline,
        children: [
          {
            key: 'risk-settings',
            titleKey: 'admin.risk.settings.title',
            label: () => t('admin.risk.settings.title', '风控设置'),
            route: '/admin/risk/settings',
            icon: OptionsOutline
          },
          {
            key: 'risk-rules',
            titleKey: 'admin.risk.policy.title',
            label: () => t('admin.risk.policy.title', '策略设置'),
            route: '/admin/risk/rules',
            icon: FlashOutline
          },
          {
            key: 'risk-ip',
            titleKey: 'admin.risk.ip.title',
            label: () => t('admin.risk.ip.title', 'IP 治理'),
            route: '/admin/risk/ip',
            icon: GlobeOutline
          }
        ]
      },
      {
        key: 'system',
        titleKey: 'admin.system.title',
        label: () => t('admin.system.title', '后台设置'),
        route: '/admin/system',
        icon: SettingsOutline
      }
    ]
  }
])

function handleSelect(key: string, item: SidebarNavItem) {
  if (item.children && item.children.length > 0) {
    return
  }
  const targetRoute = item.route || `/admin/${key}`
  if (targetRoute && route.path !== targetRoute) {
    router.push(targetRoute).catch(() => {})
  }
}

defineExpose({
  sideBarRef,
  adminMainRef,
  scrollToTop
})
</script>

<style scoped lang="scss">
.admin-layout {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  max-height: 100vh;
  max-height: 100dvh;
  overflow: hidden;
}

.admin-main {
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
  background-color: #fff;
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
    background-color: #202124;
  }
}

.fade-fast-enter-active,
.fade-fast-leave-active {
  transition: opacity 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-fast-enter-from,
.fade-fast-leave-to {
  opacity: 0;
}
</style>
