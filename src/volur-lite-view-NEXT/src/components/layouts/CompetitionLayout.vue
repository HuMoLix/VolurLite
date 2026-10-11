<template>
  <div
    class="competition-layout min-h-screen flex flex-col bg-google-surface-light dark:bg-google-surface-dark transition-colors duration-300"
    :class="layoutClass"
  >
    <header
      ref="headerRef"
      class="sticky top-0 z-40 h-16 w-full px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4 bg-white/90 dark:bg-google-surface-darkCard/90 backdrop-blur-md border-b border-google-border-light dark:border-google-border-dark select-none transition-colors duration-200"
    >
      <div ref="headerLeftRef" class="flex items-center gap-3 min-w-0 flex-shrink">
        <slot name="header-left">
          <n-button
            quaternary
            circle
            size="small"
            class="text-google-text-desc dark:text-google-text-darkDesc hover:text-google-text-primary dark:hover:text-google-text-darkPrimary"
            :title="exitText || t('competition.exit_arena', '返回竞赛详情')"
            @click="handleExit"
          >
            <template #icon>
              <n-icon :size="18" :component="ArrowBackOutline" />
            </template>
          </n-button>

          <div class="w-[1px] h-4 bg-google-border-light dark:bg-google-border-dark flex-shrink-0" />

          <div
            class="flex items-center gap-2.5 min-w-0 cursor-pointer group"
            @click="handleBrandClick"
          >
            <img
              src="/r/static/svg/favicon-simple.svg"
              alt="Volur"
              class="w-6 h-6 object-contain flex-shrink-0 transition-transform group-hover:scale-105"
            />
            <h1
              ref="titleRef"
              class="text-sm sm:text-base font-semibold tracking-tight text-google-text-primary dark:text-google-text-darkPrimary truncate"
              :style="{
                fontSize: titleFontSize ? `${titleFontSize}px` : undefined,
                maxWidth: titleMaxWidth ? `${titleMaxWidth}px` : undefined
              }"
              :title="computedTitle"
            >
              {{ computedTitle }}
            </h1>
          </div>
        </slot>
      </div>

      <div ref="headerCenterRef" class="hidden md:flex items-center justify-center flex-shrink-0">
        <slot name="header-center">
          <div
            v-if="hasCountdown"
            class="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-mono border transition-all duration-300"
            :class="countdownBadgeClass"
            :title="countdownLabel"
          >
            <n-icon :size="14" :component="HourglassOutline" :class="{ 'animate-spin': isTimeLow }" />
            <span class="font-medium tracking-wide">{{ countdownText }}</span>
          </div>
        </slot>
      </div>

      <div ref="headerRightRef" class="flex items-center gap-2 sm:gap-3 flex-shrink-0">
        <slot name="header-right">
          <slot name="header-team">
            <div
              v-if="computedTeamName"
              class="hidden sm:inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-blue-50 text-google-blue dark:bg-blue-950/50 dark:text-google-blue-dark border border-blue-200/50 dark:border-blue-900/40"
            >
              <n-icon :size="14" :component="PeopleOutline" />
              <span class="max-w-[120px] truncate">{{ computedTeamName }}</span>
            </div>
            <div
              v-else-if="showCandidatePill && candidateDisplayName"
              class="hidden sm:inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-google-surface-light dark:bg-google-surface-dark text-google-text-desc dark:text-google-text-darkDesc border border-google-border-light dark:border-google-border-dark"
            >
              <n-icon :size="14" :component="PersonOutline" />
              <span class="max-w-[120px] truncate">{{ candidateDisplayName }}</span>
            </div>
          </slot>

          <n-button
            quaternary
            circle
            size="small"
            class="text-google-text-desc dark:text-google-text-darkDesc hover:text-google-text-primary dark:hover:text-google-text-darkPrimary"
            :title="currentLocaleDisplay === 'zh-CN' ? t('common.switch_to_english', '切换至英文') : t('common.switch_to_chinese', '切换至中文')"
            @click="handleToggleLanguage"
          >
            <template #icon>
              <span class="text-xs font-semibold">{{ currentLocaleDisplay === 'zh-CN' ? 'EN' : '中' }}</span>
            </template>
          </n-button>

          <n-button
            quaternary
            circle
            size="small"
            class="text-google-text-desc dark:text-google-text-darkDesc hover:text-google-text-primary dark:hover:text-google-text-darkPrimary"
            :title="appStore.theme === 'dark' ? t('common.switch_light_mode', '切换亮色模式') : t('common.switch_dark_mode', '切换暗色模式')"
            @click="handleToggleTheme"
          >
            <template #icon>
              <n-icon :size="16">
                <SunnyOutline v-if="appStore.theme === 'dark'" />
                <MoonOutline v-else />
              </n-icon>
            </template>
          </n-button>

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
          <n-button
            v-else
            quaternary
            size="small"
            class="text-xs font-medium text-google-blue dark:text-google-blue-dark"
            @click="goToLogin"
          >
            {{ t('common.signin', '登录') }}
          </n-button>
        </slot>
      </div>
    </header>

    <div
      v-if="hasCountdown"
      class="md:hidden flex items-center justify-center py-2 px-4 bg-white dark:bg-google-surface-darkCard border-b border-google-border-light dark:border-google-border-dark select-none"
    >
      <div
        class="inline-flex items-center gap-2 px-3.5 py-1 rounded-full text-xs font-mono border"
        :class="countdownBadgeClass"
      >
        <n-icon :size="13" :component="HourglassOutline" :class="{ 'animate-spin': isTimeLow }" />
        <span class="font-medium tracking-wide">{{ countdownLabel }}: {{ countdownText }}</span>
      </div>
    </div>

    <main class="flex-1 flex flex-col min-w-0 w-full" :class="mainClass">
      <slot>
        <router-view />
      </slot>
    </main>

    <slot name="footer">
      <AppFooter v-if="showFooter" />
    </slot>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NButton, NIcon, NDropdown, useMessage, type DropdownOption } from 'naive-ui'
import {
  ArrowBackOutline,
  HourglassOutline,
  PeopleOutline,
  PersonOutline,
  SunnyOutline,
  MoonOutline,
  LogOutOutline
} from '@vicons/ionicons5'
import AppFooter from '@/components/common/Footer.vue'
import VAvatar from '@/components/sidebar/VAvatar.vue'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { logoutApi } from '@/api/auth'
import type { CompetitionDetailVO, CompetitionSummaryVO } from '@/types/competition'

defineOptions({
  name: 'CompetitionLayout'
})

export interface CountdownLabelsConfig {
  pending?: string
  running?: string
  ended?: string
}

export interface CompetitionLayoutProps {
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
  layoutClass?: string
  mainClass?: string
  showFooter?: boolean
}

const props = withDefaults(defineProps<CompetitionLayoutProps>(), {
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
  countdownLabels: undefined,
  layoutClass: '',
  mainClass: '',
  showFooter: true
})

const emit = defineEmits<{
  exit: [ev: MouseEvent]
  'time-up': []
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

const titleFontSize = ref<number | null>(null)
const titleMaxWidth = ref<number | null>(null)
let resizeObserver: ResizeObserver | null = null

const countdownText = ref<string>('00:00:00')
const countdownLabel = ref<string>('')
const isTimeLow = ref<boolean>(false)
const timerStatus = ref<'pending' | 'running' | 'ended'>('running')
let timerId: ReturnType<typeof setInterval> | null = null

const computedTitle = computed<string>(() => {
  if (props.title) return props.title
  if (props.competition?.title) return props.competition.title
  if (props.defaultTitle) return props.defaultTitle
  if (props.mode === 'theory') return t('competition.type_theory', '理论知识赛')
  return t('competition.hero_title', '网络安全实战攻防竞技')
})

const computedTeamName = computed<string>(() => {
  return props.teamName || ''
})

const candidateDisplayName = computed<string>(() => {
  if (props.candidateName) return props.candidateName
  if (userStore.displayName) return userStore.displayName
  return props.userRoleFallback || 'Candidate'
})

const effectiveStartTime = computed<string | null>(() => {
  return props.startTime || props.competition?.startTime || null
})

const effectiveEndTime = computed<string | null>(() => {
  return props.endTime || props.competition?.endTime || null
})

const hasCountdown = computed<boolean>(() => {
  return Boolean(effectiveStartTime.value || effectiveEndTime.value)
})

const countdownBadgeClass = computed<string>(() => {
  if (timerStatus.value === 'ended') {
    return 'bg-gray-100 text-google-text-muted border-google-border-light dark:bg-neutral-800 dark:text-google-text-darkMuted dark:border-neutral-700'
  }
  if (timerStatus.value === 'pending') {
    return 'bg-blue-50 text-google-blue border-blue-200/60 dark:bg-blue-950/40 dark:text-google-blue-dark dark:border-blue-900/60'
  }
  if (isTimeLow.value) {
    return 'bg-red-50 text-google-red border-red-200 dark:bg-red-950/40 dark:text-google-red-dark dark:border-red-900/60'
  }
  return 'bg-green-50 text-google-green border-green-200/60 dark:bg-green-950/40 dark:text-google-green-dark dark:border-green-900/60'
})

const currentLocaleDisplay = computed<string>(() => {
  return appStore.locale
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

function formatDiff(ms: number): string {
  const totalSecs = Math.floor(ms / 1000)
  const days = Math.floor(totalSecs / 86400)
  const hours = Math.floor((totalSecs % 86400) / 3600)
  const minutes = Math.floor((totalSecs % 3600) / 60)
  const seconds = totalSecs % 60
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  if (days > 0) {
    return `${days}d ${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
  }
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}

function updateCountdownTimer() {
  if (!hasCountdown.value) return

  const now = Date.now()
  const startMs = effectiveStartTime.value ? new Date(effectiveStartTime.value).getTime() : 0
  const endMs = effectiveEndTime.value ? new Date(effectiveEndTime.value).getTime() : 0

  const pendingLbl = props.countdownLabels?.pending || t('competition.countdown_pending', '距离开始还有')
  const runningLbl = props.countdownLabels?.running || t('competition.countdown_running', '剩余时间')
  const endedLbl = props.countdownLabels?.ended || t('competition.countdown_ended', '考核已结束')

  if (startMs > 0 && now < startMs) {
    timerStatus.value = 'pending'
    countdownLabel.value = pendingLbl
    countdownText.value = formatDiff(startMs - now)
    isTimeLow.value = false
  } else if (endMs > 0 && now <= endMs) {
    timerStatus.value = 'running'
    countdownLabel.value = runningLbl
    const remainingMs = endMs - now
    countdownText.value = formatDiff(remainingMs)
    const warnMs = props.warningThresholdMinutes * 60 * 1000
    isTimeLow.value = remainingMs <= warnMs
  } else if (endMs > 0 && now > endMs) {
    const wasRunning = timerStatus.value === 'running'
    timerStatus.value = 'ended'
    countdownLabel.value = endedLbl
    countdownText.value = '00:00:00'
    isTimeLow.value = false
    if (wasRunning) {
      emit('time-up')
    }
  }
}

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

async function handleToggleLanguage() {
  const target = appStore.locale === 'zh-CN' ? 'en-US' : 'zh-CN'
  await appStore.switchLanguage(target)
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

watch([effectiveStartTime, effectiveEndTime], () => {
  updateCountdownTimer()
})

watch(computedTitle, async () => {
  await nextTick()
  adjustTitleLayout()
})

onMounted(() => {
  updateCountdownTimer()
  if (hasCountdown.value) {
    timerId = setInterval(updateCountdownTimer, 1000)
  }
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
  if (timerId) {
    clearInterval(timerId)
    timerId = null
  }
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})
</script>

<style scoped lang="scss">
.competition-layout {
  position: relative;
  width: 100%;
}
</style>
