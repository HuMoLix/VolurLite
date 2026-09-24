<template>
  <n-layout-header
    ref="headerRef"
    :bordered="bordered"
    :style="headerStyle"
    :class="['volur-header', headerClass]"
  >
    <!-- 左侧区域：面包屑导航或自定义插槽 -->
    <div class="header-left">
      <slot name="left">
        <slot name="breadcrumb">
          <n-breadcrumb v-if="showBreadcrumb">
            <!-- 场景 1: 自定义传入的 breadcrumbItems -->
            <template v-if="breadcrumbItems && breadcrumbItems.length > 0">
              <n-breadcrumb-item
                v-for="(item, index) in breadcrumbItems"
                :key="index"
              >
                <span
                  :class="{ 'breadcrumb-link': item.clickable !== false && !!item.to }"
                  :title="item.title || item.label"
                  @click="handleCustomBreadcrumbClick(item, index)"
                >
                  {{ item.label }}
                </span>
              </n-breadcrumb-item>
            </template>

            <!-- 场景 2: 管理后台模式 (Admin) -->
            <template v-else-if="mode === 'admin'">
              <n-breadcrumb-item>
                <span class="breadcrumb-link" @click="router.push('/admin/dashboard')">
                  {{ t('layout.admin_portal') }}
                </span>
              </n-breadcrumb-item>
              <n-breadcrumb-item v-if="parentRouteTitle">
                <span
                  :class="{ 'breadcrumb-link': !!parentRoutePath }"
                  @click="handleParentRouteClick"
                >
                  {{ parentRouteTitle }}
                </span>
              </n-breadcrumb-item>
              <n-breadcrumb-item v-if="currentRouteTitle">
                {{ currentRouteTitle }}
              </n-breadcrumb-item>
            </template>

            <!-- 场景 3: 前台常规模式 (App) -->
            <template v-else>
              <n-breadcrumb-item>
                <span class="breadcrumb-link" @click="router.push('/')">
                  {{ systemStore.platformName || 'Volur Lite' }}
                </span>
              </n-breadcrumb-item>

              <!-- 3.1 竞赛详情页分支 -->
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

              <!-- 3.2 普通页面分支 -->
              <template v-else>
                <n-breadcrumb-item v-if="parentRouteTitle">
                  <span
                    :class="{ 'breadcrumb-link': !!parentRoutePath }"
                    @click="handleParentRouteClick"
                  >
                    {{ parentRouteTitle }}
                  </span>
                </n-breadcrumb-item>
                <n-breadcrumb-item v-if="currentRouteTitle">
                  {{ currentRouteTitle }}
                </n-breadcrumb-item>
              </template>
            </template>
          </n-breadcrumb>
        </slot>
      </slot>
    </div>

    <!-- 中间区域插槽（如竞技倒计时、全局搜索等） -->
    <div v-if="$slots.center || $slots.default" class="header-center">
      <slot name="center">
        <slot />
      </slot>
    </div>

    <!-- 右侧区域：工具、语言、主题、通知与用户资料 -->
    <div class="header-right">
      <slot name="right">
        <!-- 业务工具插槽 -->
        <slot name="tools" />

        <!-- 中英文语言切换 -->
        <slot name="lang-switch">
          <n-button
            v-if="showLanguageSwitch"
            quaternary
            circle
            class="header-icon-btn"
            :title="locale === 'zh_CN' ? t('common.switch_to_english') : t('common.switch_to_chinese')"
            @click="toggleLanguage"
          >
            <template #icon>
              <span class="lang-text">{{ locale === 'zh_CN' ? 'EN' : '中' }}</span>
            </template>
          </n-button>
        </slot>

        <!-- 亮暗主题切换 -->
        <slot name="theme-switch">
          <n-button
            v-if="showThemeSwitch"
            quaternary
            circle
            class="header-icon-btn"
            :title="currentTheme === 'dark' ? t('common.switch_light_mode') : t('common.switch_dark_mode')"
            @click="toggleTheme"
          >
            <template #icon>
              <n-icon size="18">
                <SunnyOutline v-if="currentTheme === 'dark'" />
                <MoonOutline v-else />
              </n-icon>
            </template>
          </n-button>
        </slot>

        <!-- 额外扩展插槽 -->
        <slot name="extra" />

        <!-- 站内通知中心 -->
        <slot name="notifications">
          <NotificationCenter v-if="showNotification && effectiveIsLoggedIn" />
        </slot>

        <!-- 用户资料 / 登录入口 -->
        <slot name="user">
          <template v-if="effectiveIsLoggedIn">
            <n-dropdown :options="effectiveDropdownOptions" @select="handleUserDropdown">
              <div class="user-profile cursor-pointer">
                <AppAvatar
                  size="small"
                  :src="effectiveAvatarUrl"
                  :name="effectiveUsername"
                  :username="effectiveUsername"
                />
                <span class="user-nickname">{{ effectiveUsername }}</span>
              </div>
            </n-dropdown>
          </template>
          <template v-else-if="showUser">
            <n-button
              type="primary"
              size="small"
              class="theme-primary-btn login-btn"
              @click="handleLoginClick"
            >
              {{ loginText || t('layout.login_register') }}
            </n-button>
          </template>
        </slot>
      </slot>
    </div>
  </n-layout-header>
</template>

<script setup lang="ts">
import { computed, ref, h, type CSSProperties } from 'vue'
import { useRouter, useRoute, type RouteLocationRaw } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  NLayoutHeader,
  NBreadcrumb,
  NBreadcrumbItem,
  NButton,
  NDropdown,
  NIcon,
  type DropdownOption
} from 'naive-ui'
import {
  SunnyOutline,
  MoonOutline,
  PersonOutline,
  LogOutOutline,
  ArrowBackOutline
} from '@vicons/ionicons5'
import AppAvatar from '@/components/common/AppAvatar.vue'
import NotificationCenter from '@/components/notification/NotificationCenter.vue'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { useSystemStore } from '@/store/modules/system'
import { useCompetitionStore } from '@/store/modules/competition'

defineOptions({
  name: 'VolurHeader'
})

export interface VolurBreadcrumbItem {
  label: string
  to?: string | RouteLocationRaw
  clickable?: boolean
  title?: string
}

export interface VolurHeaderProps {
  /**
   * 顶部栏高度（默认 64px）
   * @default 64
   */
  height?: number | string
  /**
   * 是否显示下分割线
   * @default true
   */
  bordered?: boolean
  /**
   * 布局场景模式 ('app' | 'admin' | 'custom')
   * @default 'app'
   */
  mode?: 'app' | 'admin' | 'custom'
  /**
   * 主题（'light' | 'dark'，不传则自动响应全局 appStore.theme）
   */
  theme?: 'light' | 'dark'
  /**
   * 是否显示面包屑导航
   * @default true
   */
  showBreadcrumb?: boolean
  /**
   * 自定义面包屑数据项（传入时优先使用）
   */
  breadcrumbItems?: VolurBreadcrumbItem[]
  /**
   * 是否显示中英文语言切换按钮
   * @default true
   */
  showLanguageSwitch?: boolean
  /**
   * 是否显示亮暗色模式切换按钮
   * @default true
   */
  showThemeSwitch?: boolean
  /**
   * 是否显示站内通知中心
   * @default false
   */
  showNotification?: boolean
  /**
   * 是否显示用户个人资料 / 头像下拉栏
   * @default true
   */
  showUser?: boolean
  /**
   * 自定义用户下拉菜单选项（缺省按 mode 提供对应项）
   */
  userDropdownOptions?: DropdownOption[]
  /**
   * 自定义头像图片地址（缺省读取 userStore.user?.avatarUrl）
   */
  avatarUrl?: string | null
  /**
   * 自定义用户名（缺省读取 userStore.user?.username）
   */
  username?: string | null
  /**
   * 是否处于登录态（缺省读取 userStore.isLoggedIn）
   */
  isLoggedIn?: boolean
  /**
   * 未登录状态下点击登录跳转路由
   * @default '/auth/login'
   */
  loginRoute?: string
  /**
   * 未登录状态下登录按钮文案（缺省从 i18n 语言包读取）
   */
  loginText?: string
  /**
   * 附加到 Header 根节点的自定义 class
   */
  headerClass?: string
  /**
   * 头部自定义内联样式
   */
  headerStyle?: string | CSSProperties
}

const props = withDefaults(defineProps<VolurHeaderProps>(), {
  height: 64,
  bordered: true,
  mode: 'app',
  theme: undefined,
  showBreadcrumb: true,
  breadcrumbItems: undefined,
  showLanguageSwitch: true,
  showThemeSwitch: true,
  showNotification: false,
  showUser: true,
  userDropdownOptions: undefined,
  avatarUrl: undefined,
  username: undefined,
  isLoggedIn: undefined,
  loginRoute: '/auth/login',
  loginText: '',
  headerClass: '',
  headerStyle: ''
})

const emit = defineEmits<{
  (e: 'language-change', lang: string): void
  (e: 'theme-change', theme: 'light' | 'dark'): void
  (e: 'user-dropdown-select', key: string): void
  (e: 'login-click'): void
  (e: 'breadcrumb-click', item: VolurBreadcrumbItem, index: number): void
}>()

const router = useRouter()
const route = useRoute()
const { t, locale } = useI18n()
const appStore = useAppStore()
const userStore = useUserStore()
const systemStore = useSystemStore()
const competitionStore = useCompetitionStore()

const headerRef = ref<InstanceType<typeof NLayoutHeader> | null>(null)

// 响应式主题计算
const currentTheme = computed(() => props.theme || appStore.theme)

// 响应式用户状态
const effectiveIsLoggedIn = computed(() => {
  return props.isLoggedIn !== undefined ? props.isLoggedIn : userStore.isLoggedIn
})

const effectiveUsername = computed(() => {
  if (props.username !== undefined && props.username !== null) {
    return props.username
  }
  return userStore.user?.username || (props.mode === 'admin' ? 'Admin' : 'User')
})

const effectiveAvatarUrl = computed(() => {
  if (props.avatarUrl !== undefined) {
    return props.avatarUrl
  }
  return userStore.user?.avatarUrl
})

// 面包屑辅助计算
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

function handleCustomBreadcrumbClick(item: VolurBreadcrumbItem, index: number) {
  emit('breadcrumb-click', item, index)
  if (item.clickable !== false && item.to) {
    router.push(item.to)
  }
}

// 语言切换
async function toggleLanguage() {
  const next = locale.value === 'zh_CN' ? 'en_US' : 'zh_CN'
  await appStore.switchLanguage(next)
  emit('language-change', next)
}

// 主题切换
function toggleTheme() {
  appStore.toggleTheme()
  emit('theme-change', appStore.theme)
}

// 用户下拉菜单项
const defaultDropdownOptions = computed<DropdownOption[]>(() => {
  if (props.mode === 'admin') {
    return [
      {
        label: () => t('layout.return_to_platform'),
        key: 'frontend',
        icon: () => h(NIcon, null, { default: () => h(ArrowBackOutline) })
      },
      {
        type: 'divider',
        key: 'd1'
      },
      {
        label: () => t('layout.logout'),
        key: 'logout',
        icon: () => h(NIcon, null, { default: () => h(LogOutOutline) })
      }
    ]
  }
  return [
    {
      label: () => t('layout.profile'),
      key: 'profile',
      icon: () => h(NIcon, null, { default: () => h(PersonOutline) })
    },
    {
      type: 'divider',
      key: 'd1'
    },
    {
      label: () => t('layout.logout'),
      key: 'logout',
      icon: () => h(NIcon, null, { default: () => h(LogOutOutline) })
    }
  ]
})

const effectiveDropdownOptions = computed<DropdownOption[]>(() => {
  return props.userDropdownOptions || defaultDropdownOptions.value
})

async function handleUserDropdown(key: string) {
  emit('user-dropdown-select', key)
  if (key === 'profile') {
    router.push('/profile')
  } else if (key === 'frontend') {
    router.push('/competitions')
  } else if (key === 'logout') {
    await userStore.logout()
    router.push('/auth/login')
  }
}

function handleLoginClick() {
  emit('login-click')
  if (props.loginRoute) {
    router.push(props.loginRoute)
  }
}
</script>

<style scoped lang="scss">
.volur-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-sizing: border-box;
  z-index: 9;

  // 核心视觉铁律：背景颜色与 VolurSidebar (及其 logo-container) 保持 100% 一致
  background-color: var(--n-color, #ffffff);
  border-bottom: 1px solid var(--v-border-base, #efeff5);
  transition: background-color 0.25s ease, border-color 0.25s ease;

  @include theme-dark {
    background-color: var(--n-color, #121826);
    border-bottom: 1px solid var(--v-border-base, #2e303a);
  }

  .header-left {
    display: flex;
    align-items: center;
    overflow: hidden;

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

  .header-center {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
    flex-shrink: 0;

    .header-icon-btn {
      .lang-text {
        font-size: 13px;
        font-weight: 600;
      }
    }

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
        background-color: var(--v-bg-hover, rgba(0, 0, 0, 0.04));
      }

      @include theme-dark {
        .user-nickname {
          color: var(--v-text-primary, #f8fafc);
        }

        &:hover {
          background-color: var(--v-bg-hover, rgba(255, 255, 255, 0.05));
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .volur-header {
    padding: 0 12px;

    .header-left {
      :deep(.breadcrumb-current) {
        max-width: 140px;
      }
    }

    .header-right {
      gap: 10px;

      .user-profile {
        padding: 2px 4px;

        .user-nickname {
          max-width: 80px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
  }
}

@media (max-width: 520px) {
  .volur-header {
    .header-right {
      .user-profile {
        .user-nickname {
          display: none;
        }
      }
    }
  }
}
</style>
