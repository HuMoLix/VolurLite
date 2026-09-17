<template>
  <div
    class="competition-arena-layout"
    :class="[appStore.theme, layoutClass]"
  >
    <!-- Arena Top Navigation Bar (Derived from CtfCompetitionLayout Header) -->
    <header ref="headerRef" class="arena-header">
      <!-- Header Left: Exit button, Logo & Competition Title -->
      <div ref="headerLeftRef" class="header-left">
        <slot name="header-left">
          <n-button
            quaternary
            circle
            size="small"
            class="exit-btn"
            :title="exitText || t('competition.exit_arena', '返回竞赛详情')"
            @click="exitArena"
          >
            <template #icon>
              <n-icon :size="18" :component="ArrowBackOutline" />
            </template>
          </n-button>

          <div class="divider" />

          <div class="competition-brand" @click="goToCompetitionHome">
            <img
              :src="appStore.theme === 'dark' ? '/favicon-dark.svg' : '/favicon.svg'"
              alt="Volur Lite"
              class="arena-logo"
            />
            <h1
              ref="titleRef"
              class="comp-title"
              :style="{
                fontSize: `${titleFontSize}px`,
                maxWidth: titleMaxWidth ? `${titleMaxWidth}px` : undefined
              }"
              :title="competitionTitle"
            >
              {{ competitionTitle }}
            </h1>
          </div>
        </slot>
      </div>

      <!-- Header Center: Real-time Countdown Timer -->
      <div ref="headerCenterRef" class="header-center">
        <slot name="header-center">
          <div class="countdown-pill" :class="{ 'warning': isTimeLow }" :title="countdownLabel">
            <n-icon :component="HourglassOutline" class="timer-icon" />
            <span class="timer-val">{{ countdownText }}</span>
          </div>
        </slot>
      </div>

      <!-- Header Right: Team Pill, Tools, User Profile -->
      <div ref="headerRightRef" class="header-right">
        <slot name="header-right">
          <!-- Team Info Pill / Candidate Pill -->
          <slot name="header-team">
            <div v-if="teamName" class="team-pill">
              <n-icon :component="PeopleOutline" />
              <span class="team-name">{{ teamName }}</span>
            </div>
            <div v-else-if="showCandidatePill && userStore.user?.username" class="candidate-pill">
              <n-icon :component="PersonOutline" />
              <span class="candidate-name">{{ userStore.user?.username }}</span>
            </div>
          </slot>

          <!-- Language Switcher -->
          <n-button
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

          <!-- Theme Switcher -->
          <n-button
            quaternary
            circle
            class="header-icon-btn"
            :title="appStore.theme === 'dark' ? t('common.switch_light_mode') : t('common.switch_dark_mode')"
            @click="toggleTheme"
          >
            <template #icon>
              <n-icon size="18">
                <SunnyOutline v-if="appStore.theme === 'dark'" />
                <MoonOutline v-else />
              </n-icon>
            </template>
          </n-button>

          <!-- User Dropdown -->
          <n-dropdown :options="userDropdownOptions" @select="handleUserSelect">
            <div class="user-profile cursor-pointer">
              <AppAvatar
                size="small"
                :src="userStore.user?.avatarUrl"
                :name="userStore.user?.username || userRoleFallback"
                :username="userStore.user?.username || userRoleFallback"
              />
              <span class="user-nickname">{{ userStore.user?.username || userRoleFallback }}</span>
            </div>
          </n-dropdown>
        </slot>
      </div>
    </header>

    <!-- Main Arena Workspace -->
    <main class="arena-main" :class="mainClass">
      <slot>
        <router-view v-slot="{ Component }">
          <transition name="arena-page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </slot>
    </main>

    <!-- Footer: Uses common VolurFooter -->
    <slot name="footer">
      <volur-footer v-if="showFooter" :mode="footerMode" />
    </slot>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'CompetitionLayout'
})

import { ref, computed, onMounted, onUnmounted, watch, nextTick, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NIcon,
  NDropdown,
  useMessage,
  type DropdownOption
} from 'naive-ui'
import AppAvatar from '@/components/common/AppAvatar.vue'
import VolurFooter from '../../components/VolurFooter.vue'
import {
  ArrowBackOutline,
  HourglassOutline,
  PeopleOutline,
  PersonOutline,
  SunnyOutline,
  MoonOutline,
  LogOutOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { useCompetitionStore } from '@/store/modules/competition'

export interface CountdownLabelsConfig {
  pending?: string
  running?: string
  ended?: string
}

export interface CompetitionLayoutProps {
  /** 竞赛模式：ctf | theory | custom */
  mode?: 'ctf' | 'theory' | 'custom'
  /** 默认赛事标题兜底 */
  defaultTitle?: string
  /** 退出按钮文案 */
  exitText?: string
  /** 自定义退出路由 */
  exitRoute?: string
  /** 自定义赛事主页路由 */
  homeRoute?: string
  /** 用户角色默认标识（如 'Player' 或 'Candidate'） */
  userRoleFallback?: string
  /** 无队伍时是否显示选手/考生个人胶囊 */
  showCandidatePill?: boolean
  /** 倒计时警报阈值（分钟，默认 30） */
  warningThresholdMinutes?: number
  /** 自定义倒计时各状态文案 */
  countdownLabels?: CountdownLabelsConfig
  /** 根布局自定义类名 */
  layoutClass?: string
  /** 主区域自定义类名 */
  mainClass?: string
  /** 是否显示底部栏，默认 true */
  showFooter?: boolean
  /** 底部栏模式，默认 'competition' */
  footerMode?: 'competition' | 'app' | 'admin' | 'auth' | 'custom'
}

const props = withDefaults(defineProps<CompetitionLayoutProps>(), {
  mode: 'ctf',
  defaultTitle: '网络安全竞技场',
  exitText: undefined,
  exitRoute: undefined,
  homeRoute: undefined,
  userRoleFallback: 'Player',
  showCandidatePill: false,
  warningThresholdMinutes: 30,
  countdownLabels: undefined,
  layoutClass: '',
  mainClass: '',
  showFooter: true,
  footerMode: 'competition'
})

const { t, locale } = useI18n()
const message = useMessage()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const competitionStore = useCompetitionStore()

const headerRef = ref<HTMLElement | null>(null)
const headerLeftRef = ref<HTMLElement | null>(null)
const titleRef = ref<HTMLElement | null>(null)
const headerCenterRef = ref<HTMLElement | null>(null)
const headerRightRef = ref<HTMLElement | null>(null)

const competition = computed(() => competitionStore.currentCompetition)
const teamName = computed(() => competitionStore.myTeam?.teamName || '')
const countdownText = ref<string>('00:00:00')
const countdownLabel = ref<string>('剩余时间')
const isTimeLow = ref<boolean>(false)
let timerId: ReturnType<typeof setInterval> | null = null

const titleFontSize = ref<number>(16)
const titleMaxWidth = ref<number | null>(null)
let resizeObserver: ResizeObserver | null = null
let measureCanvas: HTMLCanvasElement | null = null
let measureCtx: CanvasRenderingContext2D | null = null

function measureTextWidth(text: string, fontSize: number): number {
  if (!measureCanvas) {
    measureCanvas = document.createElement('canvas')
    measureCtx = measureCanvas.getContext('2d')
  }
  if (!measureCtx) return text.length * fontSize * 0.95
  measureCtx.font = `700 ${fontSize}px "Lato", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif`
  return measureCtx.measureText(text).width
}

function adjustTitleFontSize() {
  if (!headerRef.value || !titleRef.value || !headerLeftRef.value) return

  const headerWidth = headerRef.value.clientWidth
  const centerWidth = headerCenterRef.value?.offsetWidth || 0
  const rightWidth = headerRightRef.value?.offsetWidth || 0

  // 计算左侧除标题外的元素宽度（返回按钮 + 分割线 + Logo）
  const leftItems = headerLeftRef.value.querySelectorAll('.exit-btn, .divider, .arena-logo')
  let leftOtherWidth = 0
  leftItems.forEach((el) => {
    leftOtherWidth += (el as HTMLElement).offsetWidth || 0
  })
  // 加上左侧内部的间距（exit-btn 与 divider 与 logo 与 title 之间的间距 12px + 12px + 10px = 34px）
  leftOtherWidth += 34

  // 头部水平内边距：宽屏 48px，小屏 (<=768px) 24px
  const headerPadding = headerWidth <= 768 ? 24 : 48
  // 左右列与中间倒计时的缓冲安全间距（32px）
  const safetyGaps = 32

  const maxAvailableWidth = Math.max(0, headerWidth - headerPadding - leftOtherWidth - centerWidth - rightWidth - safetyGaps)
  titleMaxWidth.value = Math.floor(maxAvailableWidth)

  const baseFontSize = 16
  const minFontSize = 11

  if (maxAvailableWidth <= 40) {
    titleFontSize.value = minFontSize
    return
  }

  const text = competitionTitle.value || ''
  const widthAtBase = measureTextWidth(text, baseFontSize)

  if (widthAtBase <= maxAvailableWidth) {
    // 空间充足：使用标准字号，不截断
    titleFontSize.value = baseFontSize
  } else {
    // 空间受限：优先动态缩小字号，让文字尽量完整显示
    const estimatedSize = Math.floor((baseFontSize * (maxAvailableWidth / widthAtBase)) * 10) / 10
    let candidateSize = Math.min(baseFontSize, Math.max(minFontSize, estimatedSize))

    // 逐级微调测试，避免由于字体或间距浮点测算偏差导致边缘溢出
    while (candidateSize > minFontSize) {
      const currentWidth = measureTextWidth(text, candidateSize)
      if (currentWidth <= maxAvailableWidth) {
        break
      }
      candidateSize = Math.max(minFontSize, Math.round((candidateSize - 0.5) * 10) / 10)
    }

    // 当且仅当字号缩小到 minFontSize 仍无法容纳时，才停止缩小，由 CSS 的 text-overflow: ellipsis 触发 "..." 截断
    titleFontSize.value = candidateSize
  }
}

const slug = computed(() => String(route.params.slug || ''))

const competitionTitle = computed(() => {
  return competition.value?.title || props.defaultTitle || '网络安全竞技场'
})

watch(competitionTitle, async () => {
  await nextTick()
  adjustTitleFontSize()
})

const userDropdownOptions: DropdownOption[] = [
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
    label: () => t('auth.log_out'),
    key: 'logout',
    icon: () => h(NIcon, null, { default: () => h(LogOutOutline) })
  }
]

function toggleLanguage() {
  const newLocale = locale.value === 'zh_CN' ? 'en_US' : 'zh_CN'
  locale.value = newLocale
  localStorage.setItem('locale', newLocale)
}

function toggleTheme() {
  appStore.toggleTheme()
}

function handleUserSelect(key: string) {
  if (key === 'profile') {
    router.push('/profile')
  } else if (key === 'logout') {
    userStore.logout()
    message.success(t('auth.logout_success'))
    router.push('/competitions')
  }
}

function exitArena() {
  if (props.exitRoute) {
    router.push(props.exitRoute)
  } else if (slug.value) {
    router.push(`/c/${slug.value}`)
  } else {
    router.push('/competitions')
  }
}

function goToCompetitionHome() {
  if (props.homeRoute) {
    router.push(props.homeRoute)
  } else if (slug.value) {
    router.push(`/arena/${slug.value}`)
  }
}

function updateCountdown() {
  if (!competition.value) return

  const now = new Date().getTime()
  const start = new Date(competition.value.startTime).getTime()
  const end = new Date(competition.value.endTime).getTime()

  const pendingLabel = props.countdownLabels?.pending || t('competition.countdown_pending', '剩余时间')
  const runningLabel = props.countdownLabels?.running || t('competition.countdown_running', '剩余时间')
  const endedLabel = props.countdownLabels?.ended || t('competition.countdown_ended', '理论考核已结束')

  if (now < start) {
    countdownLabel.value = pendingLabel
    const diff = Math.max(0, start - now)
    countdownText.value = formatDiff(diff)
    isTimeLow.value = false
  } else if (now <= end) {
    countdownLabel.value = runningLabel
    const diff = Math.max(0, end - now)
    countdownText.value = formatDiff(diff)
    const thresholdMs = 1000 * 60 * props.warningThresholdMinutes
    isTimeLow.value = diff < thresholdMs
  } else {
    countdownLabel.value = endedLabel
    countdownText.value = '00:00:00'
    isTimeLow.value = false
  }
}

function formatDiff(ms: number): string {
  const totalSeconds = Math.floor(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const pad = (n: number) => (n < 10 ? '0' + n : String(n))
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}

async function initArena() {
  if (!slug.value) return

  try {
    const comp = await competitionStore.fetchCompetitionBySlug(slug.value)
    if (comp) {
      updateCountdown()
      if (timerId) clearInterval(timerId)
      timerId = setInterval(updateCountdown, 1000)

      // Fetch Team details via store (in-flight deduplicated)
      try {
        await competitionStore.fetchMyTeam(comp.id)
      } catch {
        // User not in team or not logged in
      }
    }
  } catch {
    message.error(t('competition.not_found_by_slug'))
  }
}

watch(slug, (newSlug) => {
  if (newSlug) {
    initArena()
  }
})

onMounted(() => {
  initArena()
  nextTick(() => {
    adjustTitleFontSize()
    if (headerRef.value) {
      resizeObserver = new ResizeObserver(() => {
        adjustTitleFontSize()
      })
      resizeObserver.observe(headerRef.value)
    }
  })
  window.addEventListener('resize', adjustTitleFontSize)
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
  window.removeEventListener('resize', adjustTitleFontSize)
})
</script>

<style scoped lang="scss">
.competition-arena-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--v-bg-base);
  color: var(--v-text-primary);
  transition: background-color 0.25s ease, color 0.25s ease;
}

.arena-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-sizing: border-box;
  box-shadow: var(--v-shadow-sm);

  // 与 VolurSidebar 和 VolurHeader 背景色严格一致
  background-color: var(--n-color, #ffffff);
  border-bottom: 1px solid var(--v-border-base, #efeff5);
  transition: background-color 0.25s ease, border-color 0.25s ease;

  @include theme-dark {
    background-color: var(--n-color, #121826);
    border-bottom: 1px solid var(--v-border-base, #2e303a);
  }

  .dark & {
    background-color: var(--n-color, #121826);
    border-bottom: 1px solid var(--v-border-base, #2e303a);
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 0 1 auto;

  .exit-btn {
    color: var(--v-text-desc);
    font-weight: 500;
    flex-shrink: 0;

    &:hover {
      color: var(--v-text-primary);
    }
  }

  .divider {
    width: 1px;
    height: 20px;
    background: var(--v-border-base);
    flex-shrink: 0;
  }

  .competition-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
    min-width: 0;
    max-width: 100%;

    .arena-logo {
      height: 26px;
      width: auto;
      flex-shrink: 0;
    }

    .comp-title {
      font-weight: 700;
      margin: 0;
      white-space: nowrap;
      color: var(--v-text-primary);
      line-height: 1.2;
      overflow: hidden;
      text-overflow: ellipsis;
      flex-shrink: 1;
      min-width: 0;
      transition: font-size 0.15s ease;
    }
  }
}

.header-center {
  display: flex;
  align-items: center;

  .countdown-pill {
    display: flex;
    align-items: center;
    gap: 6px;
    background: var(--v-bg-muted);
    border: 1px solid var(--v-border-subtle);
    padding: 6px 14px;
    border-radius: 20px;
    font-size: 13px;
    font-weight: 600;
    color: var(--v-text-primary);

    .timer-icon {
      font-size: 15px;
    }

    .timer-val {
      font-family: var(--v-font-mono, 'Fira Code', monospace);
      font-size: 14px;
      letter-spacing: 0.5px;
    }

    &.warning {
      background: rgba(237, 106, 16, 0.12);
      border-color: rgba(237, 106, 16, 0.4);
      color: #ed6a10;
    }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;

  .team-pill,
  .candidate-pill {
    display: flex;
    align-items: center;
    gap: 6px;
    background: var(--v-bg-muted);
    border: 1px solid var(--v-border-subtle);
    border-radius: 6px;
    padding: 4px 10px;
    font-size: 12px;
    font-weight: 600;
    color: var(--v-text-desc);
  }

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

    .dark & {
      .user-nickname {
        color: var(--v-text-primary, #f8fafc);
      }

      &:hover {
        background-color: var(--v-bg-hover, rgba(255, 255, 255, 0.05));
      }
    }
  }
}

.arena-main {
  flex: 1;
  width: 100%;
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 20px;
}

.arena-page-fade-enter-active,
.arena-page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.arena-page-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.arena-page-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 768px) {
  .arena-header {
    padding: 0 12px;

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
  .arena-header {
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
