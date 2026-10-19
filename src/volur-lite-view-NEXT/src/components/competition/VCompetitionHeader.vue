<template>
  <header
    ref="headerRef"
    class="v-competition-header sticky top-0 z-40 h-16 w-full px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4 bg-white/95 dark:bg-google-surface-darkCard/95 backdrop-blur-md select-none transition-all duration-200"
  >
    <div ref="headerLeftRef" class="flex items-center gap-3 min-w-0 flex-shrink">
      <slot name="header-left">
        <VTooltip :content="exitText || t('competition.exit_arena', '返回竞赛详情')">
          <n-button
            quaternary
            circle
            size="small"
            class="text-google-text-desc dark:text-google-text-darkDesc hover:text-google-text-primary dark:hover:text-google-text-darkPrimary"
            @click="handleExit"
          >
            <template #icon>
              <n-icon :size="18" :component="ArrowBackOutline" />
            </template>
          </n-button>
        </VTooltip>

        <div class="w-[1px] h-4 bg-google-border-light dark:border-google-border-dark flex-shrink-0" />

        <div
          class="flex items-center gap-2.5 min-w-0 cursor-pointer group"
          @click="handleBrandClick"
        >
          <img
            src="/r/static/svg/favicon-simple.svg"
            alt="Volur"
            class="w-6 h-6 object-contain flex-shrink-0 transition-transform group-hover:scale-105"
          />
          <VTooltip :content="computedTitle" placement="bottom">
            <h1
              ref="titleRef"
              class="text-sm sm:text-base font-semibold tracking-[0.025em] text-google-text-primary dark:text-google-text-darkPrimary truncate"
              :style="{
                fontSize: titleFontSize ? `${titleFontSize}px` : undefined,
                maxWidth: titleMaxWidth ? `${titleMaxWidth}px` : undefined,
                letterSpacing: '0.025em'
              }"
            >
              {{ computedTitle }}
            </h1>
          </VTooltip>
        </div>
      </slot>
    </div>

    <div ref="headerCenterRef" class="hidden md:flex items-center justify-center flex-shrink-0">
      <slot name="header-center" />
    </div>

    <div ref="headerRightRef" class="flex items-center gap-2 sm:gap-3 flex-shrink-0">
      <slot name="header-right">
        <n-popover
          v-model:show="isLanguageMenuShow"
          trigger="click"
          placement="bottom"
          :show-arrow="false"
          raw
          style="box-shadow: none;"
          :delay="100"
          :duration="150"
        >
          <template #trigger>
            <div>
              <VTooltip
                :disabled="isLanguageMenuShow"
                :content="t('common.language_settings', '语言设置')"
              >
                <n-button
                  quaternary
                  circle
                  size="small"
                  class="text-google-text-desc dark:text-google-text-darkDesc hover:text-google-text-primary dark:hover:text-google-text-darkPrimary"
                >
                  <template #icon>
                    <n-icon :size="18" :component="GlobeOutline" />
                  </template>
                </n-button>
              </VTooltip>
            </div>
          </template>

          <VCard
            class="v-header-language-card"
            :theme="currentTheme"
            :width="150"
            :padding="'6px'"
            :gap="2"
          >
            <div class="submenu-list">
              <div
                v-for="item in languageOptions"
                :key="item.key"
                class="submenu-item"
                :class="{ 'is-selected': currentLocale === item.key }"
                role="button"
                tabindex="0"
                @click="handleSelectLanguage(item.key)"
              >
                <span class="submenu-label">{{ item.label }}</span>
                <n-icon v-if="currentLocale === item.key" :size="16" class="submenu-check">
                  <CheckmarkOutline />
                </n-icon>
              </div>
            </div>
          </VCard>
        </n-popover>

        <VTooltip :content="appStore.theme === 'dark' ? t('common.switch_light_mode', '切换亮色模式') : t('common.switch_dark_mode', '切换暗色模式')">
          <n-button
            quaternary
            circle
            size="small"
            class="text-google-text-desc dark:text-google-text-darkDesc hover:text-google-text-primary dark:hover:text-google-text-darkPrimary"
            @click="handleToggleTheme"
          >
            <template #icon>
              <n-icon :size="16">
                <SunnyOutline v-if="appStore.theme === 'dark'" />
                <MoonOutline v-else />
              </n-icon>
            </template>
          </n-button>
        </VTooltip>

        <n-dropdown
          v-if="userStore.isLoggedIn"
          :options="userDropdownOptions"
          @select="handleUserDropdownSelect"
        >
          <div class="flex items-center gap-2 cursor-pointer p-0.5 rounded-full hover:bg-google-surface-light dark:hover:bg-google-surface-dark transition-colors">
            <VAvatar
              :src="userStore.avatarUrl || ''"
              :text="userStore.userInitial"
              :size="28"
              shape="circle"
              :ring="true"
            />
            <span class="hidden lg:inline text-xs font-medium text-google-text-primary dark:text-google-text-darkPrimary max-w-[100px] truncate">
              {{ userStore.displayName }}
            </span>
          </div>
        </n-dropdown>
        <VTooltip
          v-else
          :content="t('common.signin', '登录')"
        >
          <n-button
            quaternary
            size="small"
            class="text-xs font-medium text-google-blue dark:text-google-blue-dark"
            @click="goToLogin"
          >
            {{ t('common.signin', '登录') }}
          </n-button>
        </VTooltip>
      </slot>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NButton, NIcon, NDropdown, NPopover, useMessage, type DropdownOption } from 'naive-ui'
import {
  ArrowBackOutline,
  GlobeOutline,
  SunnyOutline,
  MoonOutline,
  LogOutOutline,
  PersonOutline,
  CheckmarkOutline
} from '@vicons/ionicons5'
import VAvatar from '@/components/sidebar/VAvatar.vue'
import { VCard } from '@/components/sidebar'
import { VTooltip } from '@/components/common'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { logoutApi } from '@/api/auth'
import type { SupportedLocale } from '@/i18n'
import type { CompetitionDetailVO, CompetitionSummaryVO } from '@/types/competition'

defineOptions({
  name: 'VCompetitionHeader'
})

export interface CountdownLabelsConfig {
  pending?: string
  running?: string
  ended?: string
}

export interface VCompetitionHeaderProps {
  competition?: CompetitionDetailVO | CompetitionSummaryVO | null
  mode?: 'ctf' | 'theory' | 'custom'
  title?: string
  defaultTitle?: string
  startTime?: string | null
  endTime?: string | null
  exitText?: string
  exitRoute?: string
  homeRoute?: string
  userRoleFallback?: string
  teamName?: string | null
  candidateName?: string | null
  showCandidatePill?: boolean
  warningThresholdMinutes?: number
  countdownLabels?: CountdownLabelsConfig
}

const props = withDefaults(defineProps<VCompetitionHeaderProps>(), {
  competition: null,
  mode: 'ctf',
  title: undefined,
  defaultTitle: undefined,
  startTime: undefined,
  endTime: undefined,
  exitText: undefined,
  exitRoute: undefined,
  homeRoute: undefined,
  userRoleFallback: 'Player',
  teamName: undefined,
  candidateName: undefined,
  showCandidatePill: true,
  warningThresholdMinutes: 15,
  countdownLabels: undefined
})

const emit = defineEmits<{
  (e: 'exit', ev: MouseEvent): void
  (e: 'time-up'): void
}>()

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const message = useMessage()
const appStore = useAppStore()
const userStore = useUserStore()

const headerRef = ref<HTMLElement | null>(null)
const headerLeftRef = ref<HTMLElement | null>(null)
const titleRef = ref<HTMLElement | null>(null)
const headerCenterRef = ref<HTMLElement | null>(null)
const headerRightRef = ref<HTMLElement | null>(null)

const isLanguageMenuShow = ref<boolean>(false)

const titleFontSize = ref<number | null>(null)
const titleMaxWidth = ref<number | null>(null)
let resizeObserver: ResizeObserver | null = null

const currentTheme = computed<'light' | 'dark'>(() => {
  return appStore.theme === 'dark' ? 'dark' : 'light'
})

const currentLocale = computed<SupportedLocale>(() => {
  return appStore.locale
})

const languageOptions: { key: SupportedLocale; label: string }[] = [
  { key: 'zh-CN', label: '简体中文' },
  { key: 'zh-TW', label: '繁體中文' },
  { key: 'en-US', label: 'English' }
]

const computedTitle = computed<string>(() => {
  if (props.title) return props.title
  if (props.competition?.title) return props.competition.title
  if (props.defaultTitle) return props.defaultTitle
  if (props.mode === 'theory') return t('competition.type_theory', '理论知识赛')
  return t('competition.hero_title', '网络安全实战攻防竞技')
})

const userDropdownOptions = computed<DropdownOption[]>(() => [
  {
    label: () => t('competition.user_center', '个人中心'),
    key: 'profile',
    icon: () => h(NIcon, null, { default: () => h(PersonOutline) })
  },
  {
    type: 'divider',
    key: 'd1'
  },
  {
    label: () => t('auth.log_out', '退出账号'),
    key: 'logout',
    icon: () => h(NIcon, null, { default: () => h(LogOutOutline) })
  }
])

function adjustTitleLayout() {
  if (!headerRef.value || !headerLeftRef.value) return
  const totalWidth = headerRef.value.clientWidth
  const centerWidth = headerCenterRef.value?.offsetWidth || 0
  const rightWidth = headerRightRef.value?.offsetWidth || 0
  const safeGap = 48
  const maxAvailable = Math.max(120, totalWidth - centerWidth - rightWidth - safeGap - 70)
  titleMaxWidth.value = Math.floor(maxAvailable)
}

function handleExit(ev: MouseEvent) {
  emit('exit', ev)
  if (props.exitRoute) {
    router.push(props.exitRoute)
    return
  }
  const slug = (route.params.slug as string) || (props.competition?.slug as string)
  if (slug) {
    router.push(`/c/${slug}`)
    return
  }
  const compId = (route.params.id as string) || (props.competition?.id as string)
  if (compId) {
    router.push(`/competitions/${compId}`)
    return
  }
  router.push('/competitions')
}

function handleBrandClick() {
  if (props.homeRoute) {
    router.push(props.homeRoute)
    return
  }
  const slug = (route.params.slug as string) || (props.competition?.slug as string)
  if (slug) {
    router.push(`/c/${slug}`)
    return
  }
  router.push('/competitions')
}

async function handleSelectLanguage(langKey: SupportedLocale) {
  isLanguageMenuShow.value = false
  if (appStore.locale !== langKey) {
    const currentRoutePath = router.currentRoute.value.path
    await appStore.switchLanguage(langKey, currentRoutePath)
  }
}

function handleToggleTheme() {
  appStore.toggleTheme()
}

async function handleUserDropdownSelect(key: string) {
  if (key === 'profile') {
    router.push('/user/profile')
  } else if (key === 'logout') {
    try {
      await logoutApi()
    } catch {
    } finally {
      userStore.clearUser()
      if (message) {
        message.success(t('auth.logout_success', '已安全退出登录'))
      }
      router.push('/competitions')
    }
  }
}

function goToLogin() {
  router.push({
    path: '/v1/signin/identifier',
    query: { redirect: route.fullPath }
  })
}


watch(computedTitle, async () => {
  await nextTick()
  adjustTitleLayout()
})

onMounted(() => {
  nextTick(() => {
    adjustTitleLayout()
    if (headerRef.value && typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(() => {
        adjustTitleLayout()
      })
      resizeObserver.observe(headerRef.value)
    }
  })
})

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})
</script>

<style scoped lang="scss">
.v-competition-header {
  box-shadow: 0 1px 3px 0 rgba(60, 64, 67, 0.08), 0 1px 2px 0 rgba(60, 64, 67, 0.04);
}

html.dark .v-competition-header {
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.4);
}

.v-header-language-card {
  .submenu-list {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .submenu-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    border-radius: 8px;
    cursor: pointer;
    font-size: 13px;
    color: var(--v-text-primary, #1f1f1f);
    transition: background-color 0.15s ease;

    &:hover {
      background-color: var(--v-item-hover, rgba(0, 0, 0, 0.04));
    }

    &.is-selected {
      font-weight: 500;
      color: #1a73e8;
    }
  }

  .submenu-check {
    color: #1a73e8;
  }
}

html.dark .v-header-language-card {
  .submenu-item {
    color: #e3e3e3;

    &:hover {
      background-color: rgba(255, 255, 255, 0.08);
    }

    &.is-selected {
      color: #8ab4f8;
    }
  }

  .submenu-check {
    color: #8ab4f8;
  }
}
</style>
