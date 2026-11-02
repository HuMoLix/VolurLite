<template>
  <div class="admin-layout h-screen max-h-screen flex overflow-hidden bg-google-surface-light dark:bg-google-surface-dark transition-colors duration-300">
    <SideBar
      ref="sideBarRef"
      v-model:collapsed="isCollapsed"
      :value="activeKey"
      :menu-options="adminMenuOptions"
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
import { ref, computed, watch, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon, type MenuOption } from 'naive-ui'
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
  ServerOutline,
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
import SideBar from '@/components/sidebar/SideBar.vue'
import { useAppStore } from '@/store/modules/app'

defineOptions({
  name: 'AdminLayout'
})

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const appStore = useAppStore()

const sideBarRef = ref<InstanceType<typeof SideBar> | null>(null)
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

const adminMenuOptions = computed<MenuOption[]>(() => [
  {
    label: () => t('admin.dashboard.title', '工作台'),
    key: 'dashboard',
    route: '/admin/dashboard',
    icon: () => h(NIcon, null, { default: () => h(SpeedometerOutline) })
  },
  {
    label: () => t('admin.users.title', '用户管理'),
    key: 'users',
    route: '/admin/users',
    icon: () => h(NIcon, null, { default: () => h(PeopleOutline) })
  },
  {
    label: () => t('admin.challenges.title', '题库管理'),
    key: 'challenges',
    icon: () => h(NIcon, null, { default: () => h(LibraryOutline) }),
    children: [
      {
        label: () => t('admin.challenges.ctf_title', 'CTF 题库'),
        key: 'challenge-ctf',
        route: '/admin/challenges/ctf',
        icon: () => h(NIcon, null, { default: () => h(FlagOutline) })
      },
      {
        label: () => t('admin.challenges.theory_title', '理论题库'),
        key: 'challenge-theory',
        route: '/admin/challenges/theory',
        icon: () => h(NIcon, null, { default: () => h(DocumentTextOutline) })
      },
      {
        label: () => t('admin.challenges.isw_title', 'ISW 题库'),
        key: 'challenge-isw',
        route: '/admin/challenges/isw',
        icon: () => h(NIcon, null, { default: () => h(TerminalOutline) })
      },
      {
        label: () => t('admin.challenges.awd_title', 'AWD 题库'),
        key: 'challenge-awd',
        route: '/admin/challenges/awd',
        icon: () => h(NIcon, null, { default: () => h(ShieldOutline) })
      },
      {
        label: () => t('admin.challenges.awd_plus_title', 'AWD+ 题库'),
        key: 'challenge-awdp',
        route: '/admin/challenges/awdp',
        icon: () => h(NIcon, null, { default: () => h(ShieldCheckmarkOutline) })
      }
    ]
  },
  {
    label: () => t('admin.competitions.title', '竞赛管理'),
    key: 'competitions',
    route: '/admin/competitions',
    icon: () => h(NIcon, null, { default: () => h(TrophyOutline) })
  },
  {
    label: () => t('admin.broadcasts.title', '公告管理'),
    key: 'broadcasts',
    route: '/admin/broadcasts',
    icon: () => h(NIcon, null, { default: () => h(MegaphoneOutline) })
  },
  {
    label: () => t('admin.clusters.group_title', '集群容器'),
    key: 'cluster-group',
    icon: () => h(NIcon, null, { default: () => h(ServerOutline) }),
    children: [
      {
        label: () => t('admin.clusters.title', '集群设置'),
        key: 'clusters',
        route: '/admin/clusters',
        icon: () => h(NIcon, null, { default: () => h(HardwareChipOutline) })
      },
      {
        label: () => t('admin.containers.title', '容器管理'),
        key: 'containers',
        route: '/admin/containers',
        icon: () => h(NIcon, null, { default: () => h(CubeOutline) })
      },
      {
        label: () => t('admin.images.title', '镜像管理'),
        key: 'images',
        route: '/admin/images',
        icon: () => h(NIcon, null, { default: () => h(LayersOutline) })
      }
    ]
  },
  {
    label: () => t('admin.logs.title', '系统日志'),
    key: 'logs',
    route: '/admin/logs',
    icon: () => h(NIcon, null, { default: () => h(DocumentTextOutline) })
  },
  {
    label: () => t('admin.tasks.title', '任务管理'),
    key: 'tasks',
    route: '/admin/tasks',
    icon: () => h(NIcon, null, { default: () => h(ListOutline) })
  },
  {
    label: () => t('admin.emails.title', '邮件管理'),
    key: 'emails',
    route: '/admin/emails',
    icon: () => h(NIcon, null, { default: () => h(MailOutline) })
  },
  {
    label: () => t('admin.risk.title', '风控管理'),
    key: 'risk-group',
    icon: () => h(NIcon, null, { default: () => h(ShieldCheckmarkOutline) }),
    children: [
      {
        label: () => t('admin.risk.settings.title', '风控设置'),
        key: 'risk-settings',
        route: '/admin/risk/settings',
        icon: () => h(NIcon, null, { default: () => h(OptionsOutline) })
      },
      {
        label: () => t('admin.risk.policy.title', '策略设置'),
        key: 'risk-rules',
        route: '/admin/risk/rules',
        icon: () => h(NIcon, null, { default: () => h(FlashOutline) })
      },
      {
        label: () => t('admin.risk.ip.title', 'IP 治理'),
        key: 'risk-ip',
        route: '/admin/risk/ip',
        icon: () => h(NIcon, null, { default: () => h(GlobeOutline) })
      }
    ]
  },
  {
    label: () => t('admin.system.title', '后台设置'),
    key: 'system',
    route: '/admin/system',
    icon: () => h(NIcon, null, { default: () => h(SettingsOutline) })
  }
])

function handleSelect(key: string, item: MenuOption) {
  if ((item as any)?.children && (item as any).children.length > 0) {
    return
  }
  const targetRoute = (item as MenuOption & { route?: string })?.route || `/admin/${key}`
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
