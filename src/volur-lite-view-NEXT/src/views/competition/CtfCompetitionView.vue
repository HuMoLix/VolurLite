<template>
  <CompetitionLayout
    :competition="competition"
    mode="ctf"
    :title="competition?.title"
    :start-time="competition?.startTime"
    :end-time="competition?.endTime"
    :default-title="t('competition.type_ctf', 'CTF 竞技场')"
    :warning-threshold-minutes="30"
    @time-up="handleTimeUp"
  >
    <div
      v-if="loading"
      class="fixed top-0 left-0 right-0 h-1 z-50 overflow-hidden bg-blue-100 dark:bg-blue-950"
    >
      <div class="progress-bar-primary" />
      <div class="progress-bar-secondary" />
    </div>

    <div class="max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-6 sm:py-8 space-y-6 flex-1 flex flex-col">
      <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-3 border-b border-google-border-light/80 dark:border-google-border-dark/80 pb-3">
        <div class="shrink-0">
          <VFilterBtnGroup
            v-model="activeTab"
            :options="tabOptions"
          />
        </div>
        <div class="flex-1 min-w-0 lg:max-w-xl xl:max-w-2xl">
          <VCompetitionNotification
            :competition-id="competition?.id"
          />
        </div>
      </div>

      <CtfCompetitionChallengeListView
        v-if="activeTab === 'challenges'"
        :competition="competition"
        :challenges="challenges"
        :my-team="myTeam"
        :scoreboard="scoreboard"
        @flag-solved="handleFlagSolved"
        @time-up="handleTimeUp"
      />

      <CtfCompetitionAnnouncementListView
        v-else-if="activeTab === 'announcements'"
        :announcements="announcements"
      />

      <CtfCompetitionScoreboardView
        v-else-if="activeTab === 'scoreboard'"
        :scoreboard="scoreboard"
        :my-team="myTeam"
        :challenges="challenges"
        :competition="competition"
        @select-team="handleSelectTeam"
      />

      <CtfCompetitionTeamView
        v-else-if="activeTab === 'team'"
        :competition="competition"
        :my-team="myTeam"
        :solved-challenges="solvedChallenges"
        :my-team-score="myTeamScore"
        :scoreboard="scoreboard"
        :challenges="challenges"
        :team-id="routeTeamId"
      />
    </div>
  </CompetitionLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import {
  FlashOutline,
  TrophyOutline,
  PeopleOutline,
  MegaphoneOutline
} from '@vicons/ionicons5'
import CompetitionLayout from '@/components/layouts/CompetitionLayout.vue'
import { VFilterBtnGroup } from '@/components/common'
import { VCompetitionNotification } from './components'
import {
  CtfCompetitionChallengeListView,
  CtfCompetitionAnnouncementListView,
  CtfCompetitionScoreboardView,
  CtfCompetitionTeamView
} from './tabs'
import {
  getCompetitionByIdApi,
  getCompetitionBySlugApi,
  listCtfChallengesApi,
  listAnnouncementsApi,
  getCtfScoreboardApi,
  getMyTeamApi
} from '@/api/competition'
import type {
  CompetitionDetailVO,
  CompetitionAnnouncementVO,
  CtfCompetitionChallengeVO,
  CtfScoreboardItemVO,
  CtfScoreboardVO,
  MyTeamInfoVO
} from '@/types/competition'
import { useUserStore } from '@/store/modules/user'

defineOptions({
  name: 'CTFCompetitionView'
})

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const message = useMessage()
const userStore = useUserStore()

const loading = ref<boolean>(false)

const competition = ref<CompetitionDetailVO | null>(null)
const challenges = ref<CtfCompetitionChallengeVO[]>([])
const announcements = ref<CompetitionAnnouncementVO[]>([])
const scoreboard = ref<CtfScoreboardItemVO[]>([])
const myTeam = ref<MyTeamInfoVO | null>(null)

const competitionIdentifier = computed<string>(() => {
  return String(route.params.slug || route.params.id || route.query.competitionId || '')
})

const competitionSlug = computed<string>(() => {
  return competition.value?.slug || competitionIdentifier.value
})

const routeTeamId = computed<string | null>(() => {
  return route.params.teamId ? String(route.params.teamId) : null
})

const activeTab = computed<'challenges' | 'announcements' | 'scoreboard' | 'team'>({
  get() {
    const p = route.path
    if (p.includes('/ctf/annoucements') || p.includes('/ctf/announcements')) {
      return 'announcements'
    }
    if (p.includes('/ctf/scoreboard')) {
      return 'scoreboard'
    }
    if (p.includes('/ctf/team')) {
      return 'team'
    }
    return 'challenges'
  },
  set(val) {
    navigateTab(val)
  }
})

function navigateTab(tab: 'challenges' | 'announcements' | 'scoreboard' | 'team') {
  const slug = competitionSlug.value
  const isShortLink = route.path.startsWith('/c/')
  const prefix = isShortLink ? `/c/${slug}/ctf` : `/competitions/${slug}/ctf`
  if (tab === 'announcements') {
    router.push(`${prefix}/annoucements`)
  } else if (tab === 'scoreboard') {
    router.push(`${prefix}/scoreboard`)
  } else if (tab === 'team') {
    router.push(`${prefix}/team`)
  } else {
    router.push(`${prefix}/challenges`)
  }
}

function handleSelectTeam(teamId: string | number) {
  const slug = competitionSlug.value
  const isShortLink = route.path.startsWith('/c/')
  const prefix = isShortLink ? `/c/${slug}/ctf` : `/competitions/${slug}/ctf`
  router.push(`${prefix}/team/${teamId}`)
}

watch(
  () => route.path,
  (path) => {
    if (path.endsWith('/ctf') || path.endsWith('/ctf/')) {
      const slug = competitionSlug.value
      const isShortLink = path.startsWith('/c/')
      const prefix = isShortLink ? `/c/${slug}/ctf` : `/competitions/${slug}/ctf`
      router.replace(`${prefix}/challenges`)
    }
  },
  { immediate: true }
)

const tabOptions = computed(() => [
  {
    value: 'challenges',
    label: t('competition.tab_challenges', '赛题列表'),
    icon: FlashOutline
  },
  {
    value: 'announcements',
    label: t('competition.tab_announcements', '公告列表'),
    icon: MegaphoneOutline
  },
  {
    value: 'scoreboard',
    label: t('competition.tab_scoreboard', '排行榜'),
    icon: TrophyOutline
  },
  {
    value: 'team',
    label: t('competition.tab_my_team', '我的队伍'),
    icon: PeopleOutline
  }
])

const solvedChallenges = computed<CtfCompetitionChallengeVO[]>(() => {
  return challenges.value.filter((ch) => ch.isSolvedByMyTeam)
})

const myTeamScore = computed<number>(() => {
  if (myTeam.value) {
    const found = scoreboard.value.find(
      (s) =>
        (myTeam.value?.teamId && String(s.teamId) === String(myTeam.value.teamId)) ||
        s.teamName === myTeam.value?.teamName
    )
    if (found?.score !== undefined) {
      return found.score
    }
  }
  return solvedChallenges.value.reduce((acc, curr) => acc + (curr.currentPoints || 0), 0)
})

async function handleFlagSolved(payload: { challengeId: number | string; points: number }) {
  const targetId = payload.challengeId
  const found = challenges.value.find((c) => c.challengeId === targetId || c.id === targetId)
  if (found) {
    found.isSolvedByMyTeam = true
    found.solvesCount++
    if (myTeam.value?.teamName) {
      if (!found.topTeams) {
        found.topTeams = [myTeam.value.teamName]
      } else if (!found.topTeams.includes(myTeam.value.teamName) && found.topTeams.length < 3) {
        found.topTeams.push(myTeam.value.teamName)
      }
    }
  }
  if (myTeam.value) {
    const teamScoreItem = scoreboard.value.find(
      (s) =>
        (myTeam.value?.teamId && String(s.teamId) === String(myTeam.value.teamId)) ||
        s.teamName === myTeam.value?.teamName
    )
    if (teamScoreItem) {
      teamScoreItem.score += payload.points
      teamScoreItem.solvesCount++
    }
  }
  if (competition.value?.id) {
    try {
      const chRes = await listCtfChallengesApi(competition.value.id)
      if (chRes?.data) {
        challenges.value = chRes.data
      }
    } catch {
    }
    refreshScoreboard(competition.value.id)
  }
}

async function refreshScoreboard(competitionId: number | string) {
  try {
    const scoreRes = await getCtfScoreboardApi(competitionId)
    const scoreData = scoreRes?.data
    if (Array.isArray(scoreData)) {
      scoreboard.value = scoreData
    } else if (scoreData && Array.isArray((scoreData as CtfScoreboardVO).rankings)) {
      scoreboard.value = (scoreData as CtfScoreboardVO).rankings
    }
  } catch {
  }
}

function handleTimeUp() {
  if (message) {
    message.warning(t('competition.countdown_ended', 'CTF 对抗时间已截止，赛题已封锁！'))
  }
}

async function fetchCtfData() {
  if (!competitionIdentifier.value) {
    competition.value = null
    challenges.value = []
    announcements.value = []
    scoreboard.value = []
    myTeam.value = null
    return
  }

  if (!userStore.isLoggedIn) {
    router.replace(`/v1/signin/identifier?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  loading.value = true
  try {
    const isNumeric = /^\d+$/.test(competitionIdentifier.value)
    let compData: CompetitionDetailVO | null = null

    if (isNumeric) {
      try {
        const compRes = await getCompetitionByIdApi(competitionIdentifier.value)
        compData = compRes?.data || null
      } catch {
        const slugRes = await getCompetitionBySlugApi(competitionIdentifier.value)
        compData = slugRes?.data || null
      }
    } else {
      const slugRes = await getCompetitionBySlugApi(competitionIdentifier.value)
      compData = slugRes?.data || null
    }

    if (!compData) {
      competition.value = null
      challenges.value = []
      announcements.value = []
      scoreboard.value = []
      myTeam.value = null
      if (message) {
        message.error(t('competition.empty_desc', '未找到对应竞赛信息'))
      }
      return
    }

    competition.value = compData

    let currentTeam: MyTeamInfoVO | null = null
    try {
      const teamRes = await getMyTeamApi(compData.id)
      currentTeam = teamRes?.data || null
    } catch {
      currentTeam = null
    }
    myTeam.value = currentTeam

    if (currentTeam && currentTeam.status === 'BANNED') {
      if (message) {
        message.error(t('competition.team_banned', '您的战队已被封禁，无法参加此场比赛'))
      }
      const isShortLink = route.path.startsWith('/c/')
      const slug = compData.slug || compData.id
      router.replace(isShortLink ? `/c/${slug}` : `/competitions/${slug}`)
      return
    }

    const isAuthorized = Boolean(userStore.isAdmin) || (currentTeam !== null && currentTeam.status === 'AUDIT_PASSED')
    if (!isAuthorized) {
      if (message) {
        message.error(t('competition.not_participating', '您尚未加入已审核通过的参赛队伍，无法进入竞赛工作台'))
      }
      const isShortLink = route.path.startsWith('/c/')
      const slug = compData.slug || compData.id
      router.replace(isShortLink ? `/c/${slug}` : `/competitions/${slug}`)
      return
    }

    const [chRes, annRes, scoreRes] = await Promise.allSettled([
      listCtfChallengesApi(compData.id),
      listAnnouncementsApi(compData.id),
      getCtfScoreboardApi(compData.id)
    ])

    if (chRes.status === 'fulfilled' && chRes.value?.data) {
      challenges.value = chRes.value.data
    } else {
      challenges.value = []
      if (chRes.status === 'rejected') {
        const reason = (chRes as PromiseRejectedResult).reason
        if (reason?.response?.status === 403) {
          if (message) {
            message.error(reason?.response?.data?.message || t('competition.not_participating', '您尚未加入已审核通过的参赛队伍，无法进入竞赛工作台'))
          }
          const isShortLink = route.path.startsWith('/c/')
          const slug = compData.slug || compData.id
          router.replace(isShortLink ? `/c/${slug}` : `/competitions/${slug}`)
          return
        }
      }
    }

    if (annRes.status === 'fulfilled' && annRes.value?.data) {
      announcements.value = annRes.value.data
    } else {
      announcements.value = []
    }

    if (scoreRes.status === 'fulfilled' && scoreRes.value?.data) {
      const scoreData = scoreRes.value.data
      if (Array.isArray(scoreData)) {
        scoreboard.value = scoreData
      } else if (scoreData && Array.isArray((scoreData as CtfScoreboardVO).rankings)) {
        scoreboard.value = (scoreData as CtfScoreboardVO).rankings
      } else {
        scoreboard.value = []
      }
    } else {
      scoreboard.value = []
    }
  } catch (err: any) {
    if (message) {
      message.error(err?.message || t('competition.fetch_failed', '获取竞赛数据失败'))
    }
  } finally {
    loading.value = false
  }
}

watch(competitionIdentifier, (newVal) => {
  if (newVal) {
    fetchCtfData()
  }
})

onMounted(() => {
  fetchCtfData()
})
</script>

<style scoped lang="scss">
.progress-bar-primary {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 100%;
  transform-origin: top left;
  background-color: #1a73e8;
  animation: mdc-linear-progress-primary-indeterminate-translate 2s linear infinite;
}

.progress-bar-secondary {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 100%;
  transform-origin: top left;
  background-color: #1a73e8;
  animation: mdc-linear-progress-secondary-indeterminate-translate 2s linear infinite;
}

@keyframes mdc-linear-progress-primary-indeterminate-translate {
  0% {
    transform: translateX(-145.166611%) scaleX(0.5);
  }
  20% {
    transform: translateX(-54.526915%) scaleX(0.798733);
  }
  59.15% {
    transform: translateX(95.443374%) scaleX(0.384373);
  }
  100% {
    transform: translateX(366.852433%) scaleX(0.01);
  }
}

@keyframes mdc-linear-progress-secondary-indeterminate-translate {
  0% {
    transform: translateX(-54.888891%) scaleX(0.1);
  }
  25% {
    transform: translateX(-17.236978%) scaleX(0.536345);
  }
  48.35% {
    transform: translateX(29.49714%) scaleX(0.741079);
  }
  100% {
    transform: translateX(107.384235%) scaleX(0.01);
  }
}
</style>
