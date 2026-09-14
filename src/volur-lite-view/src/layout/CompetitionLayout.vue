<template>
  <div class="competition-arena-layout" :class="[appStore.theme]">
    <!-- Arena Top Navigation Bar -->
    <header class="arena-header">
      <!-- Header Left: Exit button, Logo & Competition Title -->
      <div class="header-left">
        <n-button quaternary size="small" class="exit-btn" @click="exitArena">
          <template #icon>
            <n-icon :component="ArrowBackOutline" />
          </template>
          {{ t('competition.exit_arena') }}
        </n-button>

        <div class="divider" />

        <div class="competition-brand" @click="goToCompetitionHome">
          <img
            :src="appStore.theme === 'dark' ? '/favicon-dark.svg' : '/favicon.svg'"
            alt="Volur Lite"
            class="arena-logo"
          />
          <h1 class="comp-title">{{ competitionTitle }}</h1>
        </div>

        <div class="badges-wrap">
          <n-tag :type="typeTagType" round size="small" :bordered="false" class="type-tag">
            {{ typeLabel }}
          </n-tag>
          <n-tag :type="statusTagType" round size="small">
            {{ statusLabel }}
          </n-tag>
        </div>
      </div>

      <!-- Header Center: Real-time Countdown Timer -->
      <div class="header-center">
        <div class="countdown-pill" :class="{ 'warning': isTimeLow }">
          <n-icon :component="HourglassOutline" class="timer-icon" />
          <span class="timer-label">{{ countdownLabel }}:</span>
          <span class="timer-val">{{ countdownText }}</span>
        </div>
      </div>

      <!-- Header Right: Team Pill, Tools, User Profile -->
      <div class="header-right">
        <!-- Team Info Pill -->
        <div v-if="teamName" class="team-pill">
          <n-icon :component="PeopleOutline" />
          <span class="team-name">{{ teamName }}</span>
        </div>

        <!-- Language Switcher -->
        <n-button
          quaternary
          circle
          size="small"
          :title="locale === 'zh_CN' ? t('common.switch_to_english') : t('common.switch_to_chinese')"
          @click="toggleLanguage"
        >
          <template #icon>
            <n-icon :component="LanguageOutline" />
          </template>
        </n-button>

        <!-- Theme Switcher -->
        <n-button
          quaternary
          circle
          size="small"
          :title="appStore.theme === 'dark' ? t('layout.switch_to_light') : t('layout.switch_to_dark')"
          @click="toggleTheme"
        >
          <template #icon>
            <n-icon :component="appStore.theme === 'dark' ? SunnyOutline : MoonOutline" />
          </template>
        </n-button>

        <!-- User Dropdown -->
        <n-dropdown :options="userDropdownOptions" @select="handleUserSelect">
          <div class="user-profile-trigger">
            <AppAvatar
              round
              size="small"
              :src="userStore.user?.avatarUrl"
              :name="userStore.user?.username || 'Player'"
            />
            <span class="username">{{ userStore.user?.username || 'Player' }}</span>
          </div>
        </n-dropdown>
      </div>
    </header>

    <!-- Main Arena Workspace -->
    <main class="arena-main">
      <router-view v-slot="{ Component }">
        <transition name="arena-page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NIcon,
  NTag,
  NDropdown,
  useMessage,
  type DropdownOption
} from 'naive-ui'
import AppAvatar from '@/components/common/AppAvatar.vue'
import {
  ArrowBackOutline,
  HourglassOutline,
  PeopleOutline,
  LanguageOutline,
  SunnyOutline,
  MoonOutline,
  PersonOutline,
  LogOutOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { getCompetitionBySlugApi, getMyTeamApi } from '@/api/competition'
import type { CompetitionDetailVO, CompetitionType, CompetitionStatus } from '@/types/competition'

const { t, locale } = useI18n()
const message = useMessage()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const competition = ref<CompetitionDetailVO | null>(null)
const teamName = ref<string>('')
const countdownText = ref<string>('00:00:00')
const countdownLabel = ref<string>('剩余时间')
const isTimeLow = ref<boolean>(false)
let timerId: ReturnType<typeof setInterval> | null = null

const slug = computed(() => String(route.params.slug || ''))

const competitionTitle = computed(() => {
  return competition.value?.title || '网络安全竞技场'
})

const typeLabel = computed(() => {
  const typeMap: Record<CompetitionType, string> = {
    CTF: t('competition.type_ctf'),
    THEORY: t('competition.type_theory'),
    AWD: t('competition.type_awd'),
    AWD_PLUS: t('competition.type_awd_plus'),
    ISW: t('competition.type_isw')
  }
  return competition.value ? (typeMap[competition.value.type] || competition.value.type) : 'CTF'
})

const typeTagType = computed<'info' | 'success' | 'warning' | 'error' | 'default'>(() => {
  switch (competition.value?.type) {
    case 'CTF': return 'info'
    case 'THEORY': return 'success'
    case 'AWD': return 'warning'
    case 'AWD_PLUS': return 'error'
    default: return 'default'
  }
})

const statusLabel = computed(() => {
  const statusMap: Record<CompetitionStatus, string> = {
    DRAFT: t('competition.status_draft'),
    PUBLISHED: t('competition.status_published'),
    REGISTRATION: t('competition.status_registration'),
    RUNNING: t('competition.status_running'),
    FROZEN: t('competition.status_frozen'),
    ENDED: t('competition.status_ended'),
    ARCHIVED: t('competition.status_archived')
  }
  return competition.value ? (statusMap[competition.value.status] || competition.value.status) : 'RUNNING'
})

const statusTagType = computed<'warning' | 'success' | 'info' | 'error' | 'default'>(() => {
  switch (competition.value?.status) {
    case 'RUNNING': return 'success'
    case 'FROZEN': return 'warning'
    case 'PUBLISHED':
    case 'REGISTRATION': return 'info'
    default: return 'default'
  }
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

import { h } from 'vue'

function toggleLanguage() {
  const newLocale = locale.value === 'zh_CN' ? 'en_US' : 'zh_CN'
  locale.value = newLocale
  localStorage.setItem('volur_locale', newLocale)
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
  if (slug.value) {
    router.push(`/c/${slug.value}`)
  } else {
    router.push('/competitions')
  }
}

function goToCompetitionHome() {
  if (slug.value) {
    router.push(`/arena/${slug.value}`)
  }
}

function updateCountdown() {
  if (!competition.value) return

  const now = new Date().getTime()
  const start = new Date(competition.value.startTime).getTime()
  const end = new Date(competition.value.endTime).getTime()

  if (now < start) {
    countdownLabel.value = t('competition.countdown_pending')
    const diff = Math.max(0, start - now)
    countdownText.value = formatDiff(diff)
    isTimeLow.value = false
  } else if (now <= end) {
    countdownLabel.value = t('competition.countdown_running')
    const diff = Math.max(0, end - now)
    countdownText.value = formatDiff(diff)
    isTimeLow.value = diff < 1000 * 60 * 30 // less than 30 mins
  } else {
    countdownLabel.value = t('competition.countdown_ended')
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
    const res = await getCompetitionBySlugApi(slug.value)
    if (res.data) {
      competition.value = res.data
      updateCountdown()
      timerId = setInterval(updateCountdown, 1000)

      // Fetch Team details
      try {
        const teamRes = await getMyTeamApi(res.data.id)
        if (teamRes.data) {
          teamName.value = teamRes.data.teamName
        }
      } catch {
        // User not in team or not logged in
      }
    }
  } catch {
    message.error(t('competition.not_found_by_slug'))
  }
}

onMounted(() => {
  initArena()
})

onUnmounted(() => {
  if (timerId) {
    clearInterval(timerId)
    timerId = null
  }
})
</script>

<style scoped lang="scss">
.competition-arena-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--v-bg-primary, #f7f9fb);
  color: var(--v-text-primary, #111111);
  transition: background-color 0.25s ease, color 0.25s ease;

  &.dark {
    background-color: #0d1117;
    color: #e6edf3;
  }
}

.arena-header {
  height: 60px;
  background: var(--v-bg-card, #ffffff);
  border-bottom: 1px solid var(--v-border-color, rgba(0, 0, 0, 0.08));
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);

  .dark & {
    background: #161b22;
    border-bottom-color: rgba(255, 255, 255, 0.1);
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;

  .exit-btn {
    color: #444444;
    font-weight: 500;

    &:hover {
      color: #000000;
    }

    .dark & {
      color: #8b949e;

      &:hover {
        color: #ffffff;
      }
    }
  }

  .divider {
    width: 1px;
    height: 20px;
    background: var(--v-border-color, rgba(0, 0, 0, 0.12));

    .dark & {
      background: rgba(255, 255, 255, 0.15);
    }
  }

  .competition-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;

    .arena-logo {
      height: 26px;
      width: auto;
    }

    .comp-title {
      font-size: 16px;
      font-weight: 700;
      margin: 0;
      max-width: 320px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .badges-wrap {
    display: flex;
    align-items: center;
    gap: 6px;

    .type-tag {
      font-weight: 600;
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
    background: rgba(0, 0, 0, 0.05);
    border: 1px solid rgba(0, 0, 0, 0.1);
    padding: 6px 14px;
    border-radius: 20px;
    font-size: 13px;
    font-weight: 600;

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

    .dark & {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.12);
    }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;

  .team-pill {
    display: flex;
    align-items: center;
    gap: 6px;
    background: rgba(0, 0, 0, 0.04);
    border-radius: 6px;
    padding: 4px 10px;
    font-size: 12px;
    font-weight: 600;

    .dark & {
      background: rgba(255, 255, 255, 0.06);
    }
  }

  .user-profile-trigger {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 2px 6px;
    border-radius: 6px;

    &:hover {
      background: rgba(0, 0, 0, 0.04);
    }

    .username {
      font-size: 13px;
      font-weight: 500;
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
</style>
