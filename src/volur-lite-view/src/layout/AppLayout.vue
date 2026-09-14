<template>
  <n-layout class="app-layout" has-sider>
    <volur-sidebar
      v-model:collapsed="appStore.isSidebarCollapsed"
      :menu-options="menuOptions"
      :value="activeKey"
      logo-route="/competitions"
      :show-bottom-action="userStore.isAdmin"
      :bottom-action-text="t('layout.admin_panel')"
      :bottom-action-icon="ShieldCheckmarkOutline"
      bottom-action-route="/admin/dashboard"
      @select="handleMenuSelect"
    />

    <n-layout>
      <n-layout-header bordered class="app-header">
        <div class="header-left">
          <n-breadcrumb>
            <n-breadcrumb-item>
              <span class="breadcrumb-link" @click="router.push('/')">
                {{ systemStore.platformName }}
              </span>
            </n-breadcrumb-item>

            <!-- Case A: 竞赛详情页 (Volur Lite / 竞赛演练 / <比赛名>) -->
            <template v-if="isCompetitionDetail">
              <n-breadcrumb-item>
                <span class="breadcrumb-link" @click="router.push('/competitions')">
                  {{ t('competition.page_title') }}
                </span>
              </n-breadcrumb-item>
              <n-breadcrumb-item>
                <span class="breadcrumb-current" :title="competitionTitle">
                  {{ competitionTitle }}
                </span>
              </n-breadcrumb-item>
            </template>

            <!-- Case B: 其他常规页面或带 parentTitle 的页面 -->
            <template v-else>
              <n-breadcrumb-item v-if="parentRouteTitle">
                <span
                  :class="{ 'breadcrumb-link': !!parentRoutePath }"
                  @click="handleParentRouteClick"
                >
                  {{ parentRouteTitle }}
                </span>
              </n-breadcrumb-item>
              <n-breadcrumb-item>{{ currentRouteTitle }}</n-breadcrumb-item>
            </template>
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
                <AppAvatar
                  size="small"
                  :src="userStore.user?.avatarUrl"
                  :name="userStore.user?.username"
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
            <img
                src="/beian.png"
                alt="工信部备案图标"
                class="icp-icon"
            />
            <span>{{ systemStore.icpNumber }}</span>
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
  NLayoutHeader,
  NLayoutContent,
  NLayoutFooter,
  NBreadcrumb,
  NBreadcrumbItem,
  NButton,
  NDropdown,
  NIcon,
  type MenuOption,
  type DropdownOption
} from 'naive-ui'
import VolurSidebar from '@/public/layout/components/VolurSidebar.vue'
import {
  SunnyOutline,
  MoonOutline,
  TrophyOutline,
  MegaphoneOutline,
  ShieldCheckmarkOutline
} from '@vicons/ionicons5'
import NotificationCenter from '@/components/notification/NotificationCenter.vue'
import AppAvatar from '@/components/common/AppAvatar.vue'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { useSystemStore } from '@/store/modules/system'
import { useCompetitionStore } from '@/store/modules/competition'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const route = useRoute()
const { t, locale } = useI18n()
const appStore = useAppStore()
const userStore = useUserStore()
const systemStore = useSystemStore()
const competitionStore = useCompetitionStore()
const currentYear = new Date().getFullYear()

function toggleLanguage() {
  const next = locale.value === 'zh_CN' ? 'en_US' : 'zh_CN'
  locale.value = next
  localStorage.setItem('locale', next)
}

const isCompetitionDetail = computed(() => {
  return route.name === 'CompetitionDetailBySlug' ||
    route.name === 'CompetitionShortLink' ||
    route.path.startsWith('/c/') ||
    (route.path.startsWith('/competitions/') && route.path !== '/competitions')
})

const competitionTitle = computed(() => {
  return competitionStore.currentCompetition?.title || (route.meta.title as string) || t('competition.detail_title')
})

const parentRouteTitle = computed(() => {
  if (route.meta.parentTitleKey) {
    return t(route.meta.parentTitleKey as string)
  }
  return (route.meta.parentTitle as string) || ''
})

const parentRoutePath = computed(() => {
  return (route.meta.parentPath as string) || ''
})

function handleParentRouteClick() {
  if (parentRoutePath.value) {
    router.push(parentRoutePath.value)
  }
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


async function handleUserDropdown(key: string) {
  if (key === 'profile') {
    await router.push('/profile')
  } else if (key === 'logout') {
    await userStore.logout()
    await router.push('/auth/login')
  }
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
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

    :deep(.breadcrumb-link) {
      cursor: pointer;
      transition: color 0.2s ease, opacity 0.2s ease;

      &:hover {
        color: var(--v-text-primary, #000000);
        opacity: 0.8;
      }
    }

    :deep(.breadcrumb-current) {
      max-width: 360px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      display: inline-block;
      vertical-align: bottom;
    }
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
      gap: 4px;
      display: inline-flex;
      align-items: center;
      color: #444444;
      text-decoration: none;
      transition: color 0.2s ease;

      &:hover {
        color: #000000;
        text-decoration: underline;
      }
    }

    .icp-icon {
      width: 14px;
      height: 14px;
      vertical-align: middle;
      object-fit: contain;
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
