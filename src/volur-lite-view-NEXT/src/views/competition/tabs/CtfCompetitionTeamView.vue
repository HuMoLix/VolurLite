<template>
  <div class="ctf-team-view flex flex-col gap-6 w-full">
    <div
      v-if="!currentTeamId && !myTeam"
      class="py-20 text-center space-y-3 rounded-2xl sm:rounded-3xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm"
    >
      <n-icon :size="48" :component="PeopleOutline" class="text-google-text-muted dark:text-google-text-darkMuted mx-auto" />
      <div class="text-sm text-google-text-desc dark:text-google-text-darkDesc">
        {{ t('competition.no_team_joined', '您尚未加入或创建参赛队伍') }}
      </div>
    </div>

    <n-spin v-else :show="loading">
      <div class="flex flex-col gap-6 w-full">
        <section class="rounded-2xl sm:rounded-3xl p-6 sm:p-8 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm flex flex-col gap-6 transition-colors">
          <div class="flex items-center justify-center gap-3.5 w-full">
            <VAvatar
              :src="displayAvatarUrl || ''"
              :text="displayTeamName ? displayTeamName.charAt(0).toUpperCase() : 'T'"
              :alt="displayTeamName"
              :size="48"
              shape="circle"
              ring
              :editable="true"
              class="shrink-0"
              @upload-success="handleTeamAvatarUploaded"
            />
            <h1
              class="m-0 text-center font-bold tracking-tight text-google-text-primary dark:text-[#FCFCFC] truncate"
              style="font-size: 30px; line-height: 1.25;"
            >
              {{ displayTeamName }}
            </h1>
          </div>

          <div class="grid grid-cols-2 sm:grid-cols-5 gap-3 sm:gap-4 w-full pt-6 border-t border-google-border-light/70 dark:border-google-border-dark/70">
            <div class="flex flex-col items-center justify-center p-4 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 text-center">
              <span class="text-xs font-medium text-google-text-desc dark:text-google-text-darkDesc mb-1.5 flex items-center gap-1.5">
                <n-icon :size="14" :component="TrophyOutline" class="text-google-yellow dark:text-google-yellow-dark" />
                <span>{{ t('competition.stat_rank', '排名') }}</span>
              </span>
              <span class="text-xl sm:text-2xl font-bold font-mono text-google-blue dark:text-google-blue-dark">
                {{ displayRankText }}
              </span>
            </div>

            <div class="flex flex-col items-center justify-center p-4 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 text-center">
              <span class="text-xs font-medium text-google-text-desc dark:text-google-text-darkDesc mb-1.5 flex items-center gap-1.5">
                <n-icon :size="14" :component="StatsChartOutline" class="text-google-blue dark:text-google-blue-dark" />
                <span>{{ t('competition.stat_total_score', '总分') }}</span>
              </span>
              <span class="text-xl sm:text-2xl font-bold font-mono text-google-blue dark:text-google-blue-dark">
                {{ totalScore }} pts
              </span>
            </div>

            <div class="flex flex-col items-center justify-center p-4 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 text-center">
              <span class="text-xs font-medium text-google-text-desc dark:text-google-text-darkDesc mb-1.5 flex items-center gap-1.5">
                <n-icon :size="14" :component="RibbonOutline" class="text-google-red dark:text-google-red-dark" />
                <span>{{ t('competition.stat_strong_category', '强项') }}</span>
              </span>
              <span class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary truncate max-w-full px-1">
                {{ strongCategory }}
              </span>
            </div>

            <div class="flex flex-col items-center justify-center p-4 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 text-center">
              <span class="text-xs font-medium text-google-text-desc dark:text-google-text-darkDesc mb-1.5 flex items-center gap-1.5">
                <n-icon :size="14" :component="CheckmarkCircleOutline" class="text-google-green dark:text-google-green-dark" />
                <span>{{ t('competition.stat_solves_count', '解题数') }}</span>
              </span>
              <span class="text-xl sm:text-2xl font-bold font-mono text-google-text-primary dark:text-google-text-darkPrimary">
                {{ totalSolvesCount }} 道
              </span>
            </div>

            <div class="col-span-2 sm:col-span-1 flex flex-col items-center justify-center p-4 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/60 dark:border-google-border-dark/60 text-center">
              <span class="text-xs font-medium text-google-text-desc dark:text-google-text-darkDesc mb-1.5 flex items-center gap-1.5">
                <n-icon :size="14" :component="FlameOutline" class="text-google-red dark:text-google-red-dark" />
                <span>{{ t('competition.stat_first_bloods', '一血数量') }}</span>
              </span>
              <span class="text-xl sm:text-2xl font-bold font-mono text-google-red dark:text-google-red-dark">
                {{ firstBloodCount }} 次
              </span>
            </div>
          </div>
        </section>

        <section class="rounded-2xl sm:rounded-3xl p-6 sm:p-8 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm flex flex-col gap-5 transition-colors">
          <div class="flex items-center justify-between pb-3 border-b border-google-border-light/70 dark:border-google-border-dark/70">
            <div class="flex items-center gap-2">
              <n-icon :size="20" :component="MedalOutline" class="text-google-blue dark:text-google-blue-dark" />
              <h2 class="m-0 text-base font-bold text-google-text-primary dark:text-google-text-darkPrimary">
                {{ t('competition.team_performance_title', '参赛成绩') }}
              </h2>
            </div>
            <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
              {{ memberAchievements.length }} 名选手
            </span>
          </div>

          <VTable
            :bordered="false"
            size="medium"
            :columns="memberColumns"
            :data="memberAchievements"
            :loading="loading"
            :scroll-x="760"
            :row-key="(row: MemberAchievement) => String(row.id)"
          >
            <template #empty>
              <div class="py-10 text-center text-xs text-google-text-muted dark:text-google-text-darkMuted">
                {{ t('competition.no_member_data', '暂无参赛选手成绩数据') }}
              </div>
            </template>
          </VTable>
        </section>

        <section class="rounded-2xl sm:rounded-3xl p-6 sm:p-8 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm flex flex-col gap-6 transition-colors">
          <div class="flex items-center justify-between pb-3 border-b border-google-border-light/70 dark:border-google-border-dark/70">
            <div class="flex items-center gap-2">
              <n-icon :size="20" :component="LayersOutline" class="text-google-blue dark:text-google-blue-dark" />
              <h2 class="m-0 text-base font-bold text-google-text-primary dark:text-google-text-darkPrimary">
                {{ t('competition.ability_radar_title', '能力雷达') }}
              </h2>
            </div>
            <span class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
              各题型攻防能力图谱
            </span>
          </div>

          <div
            v-if="radarCategories.length >= 3"
            class="flex flex-col lg:flex-row items-center justify-around gap-8 w-full py-2"
          >
            <div class="flex flex-col items-center gap-3 w-full max-w-[340px]">
              <div class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary flex items-center gap-1.5">
                <span class="w-2.5 h-2.5 rounded-full bg-google-blue dark:bg-google-blue-dark" />
                <span>队伍分值雷达图</span>
              </div>
              <div class="relative w-full aspect-square max-w-[300px]">
                <svg viewBox="0 0 320 320" class="w-full h-full select-none overflow-visible">
                  <polygon
                    v-for="(ring, rIdx) in radarRings"
                    :key="'score-ring-' + rIdx"
                    :points="ring"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1"
                    stroke-dasharray="2 2"
                    class="text-google-border-light/80 dark:text-google-border-dark/60"
                  />
                  <line
                    v-for="(spoke, sIdx) in radarSpokes"
                    :key="'score-spoke-' + sIdx"
                    :x1="centerCoord"
                    :y1="centerCoord"
                    :x2="spoke.x"
                    :y2="spoke.y"
                    stroke="currentColor"
                    stroke-width="1"
                    class="text-google-border-light/80 dark:text-google-border-dark/60"
                  />
                  <polygon
                    :points="scoreRadarPolygon"
                    class="fill-blue-500/20 stroke-google-blue dark:stroke-google-blue-dark"
                    stroke-width="2"
                    stroke-linejoin="round"
                  />
                  <circle
                    v-for="(pt, pIdx) in scoreRadarPoints"
                    :key="'score-pt-' + pIdx"
                    :cx="pt.x"
                    :cy="pt.y"
                    r="4"
                    class="fill-google-blue dark:fill-google-blue-dark stroke-white dark:stroke-google-surface-darkCard"
                    stroke-width="1.5"
                  />
                  <text
                    v-for="(label, lIdx) in scoreRadarLabels"
                    :key="'score-lbl-' + lIdx"
                    :x="label.x"
                    :y="label.y"
                    :text-anchor="label.anchor"
                    class="text-[11px] font-semibold fill-google-text-desc dark:fill-google-text-darkDesc"
                    dominant-baseline="central"
                  >
                    {{ label.text }}
                  </text>
                </svg>
              </div>
            </div>

            <div class="hidden lg:block h-64 w-px bg-google-border-light/70 dark:border-google-border-dark/70" />

            <div class="flex flex-col items-center gap-3 w-full max-w-[340px]">
              <div class="text-sm font-semibold text-google-text-primary dark:text-google-text-darkPrimary flex items-center gap-1.5">
                <span class="w-2.5 h-2.5 rounded-full bg-google-green dark:bg-google-green-dark" />
                <span>队伍解题数雷达图</span>
              </div>
              <div class="relative w-full aspect-square max-w-[300px]">
                <svg viewBox="0 0 320 320" class="w-full h-full select-none overflow-visible">
                  <polygon
                    v-for="(ring, rIdx) in radarRings"
                    :key="'solves-ring-' + rIdx"
                    :points="ring"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1"
                    stroke-dasharray="2 2"
                    class="text-google-border-light/80 dark:text-google-border-dark/60"
                  />
                  <line
                    v-for="(spoke, sIdx) in radarSpokes"
                    :key="'solves-spoke-' + sIdx"
                    :x1="centerCoord"
                    :y1="centerCoord"
                    :x2="spoke.x"
                    :y2="spoke.y"
                    stroke="currentColor"
                    stroke-width="1"
                    class="text-google-border-light/80 dark:text-google-border-dark/60"
                  />
                  <polygon
                    :points="solvesRadarPolygon"
                    class="fill-emerald-500/20 stroke-google-green dark:stroke-google-green-dark"
                    stroke-width="2"
                    stroke-linejoin="round"
                  />
                  <circle
                    v-for="(pt, pIdx) in solvesRadarPoints"
                    :key="'solves-pt-' + pIdx"
                    :cx="pt.x"
                    :cy="pt.y"
                    r="4"
                    class="fill-google-green dark:fill-google-green-dark stroke-white dark:stroke-google-surface-darkCard"
                    stroke-width="1.5"
                  />
                  <text
                    v-for="(label, lIdx) in solvesRadarLabels"
                    :key="'solves-lbl-' + lIdx"
                    :x="label.x"
                    :y="label.y"
                    :text-anchor="label.anchor"
                    class="text-[11px] font-semibold fill-google-text-desc dark:fill-google-text-darkDesc"
                    dominant-baseline="central"
                  >
                    {{ label.text }}
                  </text>
                </svg>
              </div>
            </div>
          </div>
          <div
            v-else
            class="py-12 text-center text-xs text-google-text-muted dark:text-google-text-darkMuted"
          >
            {{ t('competition.no_radar_data', '暂无足够题目分类数据生成能力雷达') }}
          </div>
        </section>

        <section class="rounded-2xl sm:rounded-3xl p-6 sm:p-8 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm flex flex-col gap-5 transition-colors">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-google-border-light/70 dark:border-google-border-dark/70">
            <div class="flex items-center gap-2">
              <n-icon :size="20" :component="CheckmarkCircleOutline" class="text-google-green dark:text-google-green-dark" />
              <h2 class="m-0 text-base font-bold text-google-text-primary dark:text-google-text-darkPrimary">
                {{ t('competition.solve_records_title', '答题记录') }}
              </h2>
            </div>
            <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
              共 {{ teamSolves.length }} 条记录
            </div>
          </div>

          <VTable
            :bordered="false"
            size="medium"
            :columns="solveColumns"
            :data="paginatedSolves"
            :loading="loading"
            :scroll-x="920"
            :row-key="(row: CtfTeamSolveRecordVO) => String(row.id || row.challengeId)"
          >
            <template #empty>
              <div class="py-12 text-center text-xs text-google-text-muted dark:text-google-text-darkMuted">
                {{ t('competition.no_solve_records', '暂无答题记录') }}
              </div>
            </template>
          </VTable>

          <div v-if="teamSolves.length > 0" class="flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-google-border-light/70 dark:border-google-border-dark/70">
            <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
              显示第 {{ (currentPage - 1) * pageSize + 1 }} - {{ Math.min(currentPage * pageSize, teamSolves.length) }} 条，共 {{ teamSolves.length }} 条
            </div>
            <VPagination
              v-model:page="currentPage"
              v-model:page-size="pageSize"
              :item-count="teamSolves.length"
              :page-sizes="[10, 20, 50]"
              show-size-picker
              show-quick-jumper
              size="small"
            />
          </div>
        </section>
      </div>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, NSpin } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import {
  TrophyOutline,
  PeopleOutline,
  FlameOutline,
  CheckmarkCircleOutline,
  LayersOutline,
  MedalOutline,
  RibbonOutline,
  StatsChartOutline
} from '@vicons/ionicons5'
import VAvatar from '@/components/sidebar/VAvatar.vue'
import { VTable, VPagination } from '@/components/common'
import { getTeamByIdApi, getTeamSolvesApi, updateMyTeamApi } from '@/api/competition'
import type {
  CtfCompetitionChallengeVO,
  MyTeamInfoVO,
  CtfScoreboardItemVO,
  CompetitionDetailVO,
  CtfTeamSolveRecordVO
} from '@/types/competition'

defineOptions({
  name: 'CtfCompetitionTeamView'
})

const props = withDefaults(
  defineProps<{
    competition?: CompetitionDetailVO | null
    myTeam?: MyTeamInfoVO | null
    solvedChallenges?: CtfCompetitionChallengeVO[]
    myTeamScore?: number
    scoreboard?: CtfScoreboardItemVO[]
    challenges?: CtfCompetitionChallengeVO[]
    teamId?: string | number | null
  }>(),
  {
    competition: null,
    myTeam: null,
    solvedChallenges: () => [],
    myTeamScore: 0,
    scoreboard: () => [],
    challenges: () => [],
    teamId: null
  }
)

const { t } = useI18n()

const loading = ref<boolean>(false)
const otherTeamInfo = ref<MyTeamInfoVO | null>(null)
const teamSolves = ref<CtfTeamSolveRecordVO[]>([])
const currentPage = ref<number>(1)
const pageSize = ref<number>(10)

const centerCoord = 160
const radarRadius = 100

const currentTeamId = computed<string | number | null>(() => {
  if (props.teamId) {
    return props.teamId
  }
  return props.myTeam?.teamId || null
})

const isViewingOtherTeam = computed<boolean>(() => {
  if (!props.teamId) return false
  if (!props.myTeam) return true
  return String(props.teamId) !== String(props.myTeam.teamId)
})

const activeTeamData = computed<MyTeamInfoVO | null>(() => {
  if (isViewingOtherTeam.value) {
    return otherTeamInfo.value
  }
  return props.myTeam
})

const currentScoreboardItem = computed<CtfScoreboardItemVO | null>(() => {
  if (!currentTeamId.value) return null
  return props.scoreboard.find((s) => String(s.teamId) === String(currentTeamId.value)) || null
})

const displayTeamName = computed<string>(() => {
  return activeTeamData.value?.teamName || currentScoreboardItem.value?.teamName || t('competition.default_team_name', '参赛队伍')
})

const displayAvatarUrl = computed<string | null>(() => {
  return activeTeamData.value?.avatarUrl || currentScoreboardItem.value?.avatarUrl || null
})

const displayRankText = computed<string>(() => {
  const rank = currentScoreboardItem.value?.rank
  return rank ? `第 ${rank} 名` : '--'
})

const totalScore = computed<number>(() => {
  if (currentScoreboardItem.value?.score !== undefined) {
    return currentScoreboardItem.value.score
  }
  if (!isViewingOtherTeam.value && props.myTeamScore !== undefined && props.myTeamScore > 0) {
    return props.myTeamScore
  }
  return teamSolves.value.reduce((acc, s) => acc + (s.points || 0), 0)
})

const totalSolvesCount = computed<number>(() => {
  if (currentScoreboardItem.value?.solvesCount !== undefined) {
    return currentScoreboardItem.value.solvesCount
  }
  return teamSolves.value.length
})

const firstBloodCount = computed<number>(() => {
  return teamSolves.value.filter((s) => s.isFirstBlood).length
})

const radarCategories = computed<string[]>(() => {
  const catSet = new Set<string>()
  for (const c of props.challenges) {
    const cat = c.categoryName || c.category
    if (cat) {
      catSet.add(cat)
    }
  }
  for (const s of teamSolves.value) {
    if (s.category) {
      catSet.add(s.category)
    }
  }
  return Array.from(catSet)
})

const strongCategory = computed<string>(() => {
  if (teamSolves.value.length === 0) return '--'
  const catScoreMap: Record<string, number> = {}
  for (const s of teamSolves.value) {
    const cat = s.category || 'Misc'
    catScoreMap[cat] = (catScoreMap[cat] || 0) + (s.points || 0)
  }
  let bestCat = '--'
  let maxScore = -1
  for (const [cat, pts] of Object.entries(catScoreMap)) {
    if (pts > maxScore) {
      maxScore = pts
      bestCat = cat
    }
  }
  return bestCat
})

interface MemberAchievement {
  id: string | number
  name: string
  avatarUrl?: string | null
  role: string
  strongCategory: string
  solvesCount: number
  firstBloodCount: number
  score: number
}

const memberAchievements = computed<MemberAchievement[]>(() => {
  const team = activeTeamData.value
  if (team?.members && team.members.length > 0) {
    return team.members.map((m, idx) => {
      const userSolves = teamSolves.value.filter(
        (s) => (s.userId && String(s.userId) === String(m.userId)) || (s.username && s.username === m.username)
      )
      const catMap: Record<string, number> = {}
      for (const s of userSolves) {
        const cat = s.category || 'Misc'
        catMap[cat] = (catMap[cat] || 0) + (s.points || 0)
      }
      let memberBestCat = '--'
      let maxPts = -1
      for (const [cat, pts] of Object.entries(catMap)) {
        if (pts > maxPts) {
          maxPts = pts
          memberBestCat = cat
        }
      }

      return {
        id: m.id || idx,
        name: m.realName || m.username,
        avatarUrl: m.avatarUrl || null,
        role: m.role,
        strongCategory: memberBestCat,
        solvesCount: userSolves.length,
        firstBloodCount: userSolves.filter((s) => s.isFirstBlood).length,
        score: userSolves.reduce((acc, s) => acc + (s.points || 0), 0)
      }
    })
  }

  const solverMap: Record<string, { solves: CtfTeamSolveRecordVO[]; name: string }> = {}
  for (const s of teamSolves.value) {
    const key = s.username || '匿名选手'
    if (!solverMap[key]) {
      solverMap[key] = { solves: [], name: key }
    }
    solverMap[key].solves.push(s)
  }

  return Object.values(solverMap).map((entry, idx) => {
    const userSolves = entry.solves
    const catMap: Record<string, number> = {}
    for (const s of userSolves) {
      const cat = s.category || 'Misc'
      catMap[cat] = (catMap[cat] || 0) + (s.points || 0)
    }
    let memberBestCat = '--'
    let maxPts = -1
    for (const [cat, pts] of Object.entries(catMap)) {
      if (pts > maxPts) {
        maxPts = pts
        memberBestCat = cat
      }
    }
    return {
      id: idx + 1,
      name: entry.name,
      avatarUrl: null,
      role: idx === 0 ? 'CAPTAIN' : 'MEMBER',
      strongCategory: memberBestCat,
      solvesCount: userSolves.length,
      firstBloodCount: userSolves.filter((s) => s.isFirstBlood).length,
      score: userSolves.reduce((acc, s) => acc + (s.points || 0), 0)
    }
  })
})

const radarSpokes = computed<{ x: number; y: number }[]>(() => {
  const cats = radarCategories.value
  const total = cats.length
  if (total < 3) return []
  return cats.map((_, i) => {
    const angle = -Math.PI / 2 + i * ((2 * Math.PI) / total)
    return {
      x: centerCoord + radarRadius * Math.cos(angle),
      y: centerCoord + radarRadius * Math.sin(angle)
    }
  })
})

const radarRings = computed<string[]>(() => {
  const cats = radarCategories.value
  const total = cats.length
  if (total < 3) return []
  const levels = [0.25, 0.5, 0.75, 1.0]
  return levels.map((lvl) => {
    const r = radarRadius * lvl
    return cats
      .map((_, i) => {
        const angle = -Math.PI / 2 + i * ((2 * Math.PI) / total)
        const x = centerCoord + r * Math.cos(angle)
        const y = centerCoord + r * Math.sin(angle)
        return `${x.toFixed(1)},${y.toFixed(1)}`
      })
      .join(' ')
  })
})

const scoreRadarPolygon = computed<string>(() => {
  const cats = radarCategories.value
  const total = cats.length
  if (total < 3) return ''
  const catScoreMap: Record<string, number> = {}
  for (const s of teamSolves.value) {
    const cat = s.category || 'Misc'
    catScoreMap[cat] = (catScoreMap[cat] || 0) + (s.points || 0)
  }
  const maxScore = Math.max(100, ...Object.values(catScoreMap))

  return cats
    .map((cat, i) => {
      const pts = catScoreMap[cat] || 0
      const ratio = Math.min(1, pts / maxScore)
      const r = radarRadius * (0.15 + 0.85 * ratio)
      const angle = -Math.PI / 2 + i * ((2 * Math.PI) / total)
      const x = centerCoord + r * Math.cos(angle)
      const y = centerCoord + r * Math.sin(angle)
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
})

const scoreRadarPoints = computed<{ x: number; y: number }[]>(() => {
  const cats = radarCategories.value
  const total = cats.length
  if (total < 3) return []
  const catScoreMap: Record<string, number> = {}
  for (const s of teamSolves.value) {
    const cat = s.category || 'Misc'
    catScoreMap[cat] = (catScoreMap[cat] || 0) + (s.points || 0)
  }
  const maxScore = Math.max(100, ...Object.values(catScoreMap))

  return cats.map((cat, i) => {
    const pts = catScoreMap[cat] || 0
    const ratio = Math.min(1, pts / maxScore)
    const r = radarRadius * (0.15 + 0.85 * ratio)
    const angle = -Math.PI / 2 + i * ((2 * Math.PI) / total)
    return {
      x: centerCoord + r * Math.cos(angle),
      y: centerCoord + r * Math.sin(angle)
    }
  })
})

const scoreRadarLabels = computed<{ text: string; x: number; y: number; anchor: string }[]>(() => {
  const cats = radarCategories.value
  const total = cats.length
  if (total < 3) return []
  const catScoreMap: Record<string, number> = {}
  for (const s of teamSolves.value) {
    const cat = s.category || 'Misc'
    catScoreMap[cat] = (catScoreMap[cat] || 0) + (s.points || 0)
  }

  return cats.map((cat, i) => {
    const angle = -Math.PI / 2 + i * ((2 * Math.PI) / total)
    const labelRadius = radarRadius + 24
    const x = centerCoord + labelRadius * Math.cos(angle)
    const y = centerCoord + labelRadius * Math.sin(angle)
    let anchor = 'middle'
    if (Math.cos(angle) > 0.3) anchor = 'start'
    else if (Math.cos(angle) < -0.3) anchor = 'end'

    const pts = catScoreMap[cat] || 0
    return {
      text: `${cat} (${pts}p)`,
      x,
      y,
      anchor
    }
  })
})

const solvesRadarPolygon = computed<string>(() => {
  const cats = radarCategories.value
  const total = cats.length
  if (total < 3) return ''
  const catSolvesMap: Record<string, number> = {}
  for (const s of teamSolves.value) {
    const cat = s.category || 'Misc'
    catSolvesMap[cat] = (catSolvesMap[cat] || 0) + 1
  }
  const maxSolves = Math.max(3, ...Object.values(catSolvesMap))

  return cats
    .map((cat, i) => {
      const count = catSolvesMap[cat] || 0
      const ratio = Math.min(1, count / maxSolves)
      const r = radarRadius * (0.15 + 0.85 * ratio)
      const angle = -Math.PI / 2 + i * ((2 * Math.PI) / total)
      const x = centerCoord + r * Math.cos(angle)
      const y = centerCoord + r * Math.sin(angle)
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
})

const solvesRadarPoints = computed<{ x: number; y: number }[]>(() => {
  const cats = radarCategories.value
  const total = cats.length
  if (total < 3) return []
  const catSolvesMap: Record<string, number> = {}
  for (const s of teamSolves.value) {
    const cat = s.category || 'Misc'
    catSolvesMap[cat] = (catSolvesMap[cat] || 0) + 1
  }
  const maxSolves = Math.max(3, ...Object.values(catSolvesMap))

  return cats.map((cat, i) => {
    const count = catSolvesMap[cat] || 0
    const ratio = Math.min(1, count / maxSolves)
    const r = radarRadius * (0.15 + 0.85 * ratio)
    const angle = -Math.PI / 2 + i * ((2 * Math.PI) / total)
    return {
      x: centerCoord + r * Math.cos(angle),
      y: centerCoord + r * Math.sin(angle)
    }
  })
})

const solvesRadarLabels = computed<{ text: string; x: number; y: number; anchor: string }[]>(() => {
  const cats = radarCategories.value
  const total = cats.length
  if (total < 3) return []
  const catSolvesMap: Record<string, number> = {}
  for (const s of teamSolves.value) {
    const cat = s.category || 'Misc'
    catSolvesMap[cat] = (catSolvesMap[cat] || 0) + 1
  }

  return cats.map((cat, i) => {
    const angle = -Math.PI / 2 + i * ((2 * Math.PI) / total)
    const labelRadius = radarRadius + 24
    const x = centerCoord + labelRadius * Math.cos(angle)
    const y = centerCoord + labelRadius * Math.sin(angle)
    let anchor = 'middle'
    if (Math.cos(angle) > 0.3) anchor = 'start'
    else if (Math.cos(angle) < -0.3) anchor = 'end'

    const count = catSolvesMap[cat] || 0
    return {
      text: `${cat} (${count}题)`,
      x,
      y,
      anchor
    }
  })
})

const paginatedSolves = computed<CtfTeamSolveRecordVO[]>(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return teamSolves.value.slice(start, start + pageSize.value)
})

function formatSolveTime(instantOrString: string | number | Date | null | undefined): string {
  if (!instantOrString) return '--'
  const date = new Date(instantOrString)
  if (isNaN(date.getTime())) return '--'
  const Y = date.getFullYear()
  const M = String(date.getMonth() + 1).padStart(2, '0')
  const D = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${Y}-${M}-${D} ${h}:${m}:${s}`
}

function getRoleLabel(role: string): string {
  if (role === 'CAPTAIN') return '队长'
  if (role === 'LEADER') return '领队'
  return '队员'
}

async function handleTeamAvatarUploaded(newUrl: string) {
  if (activeTeamData.value) {
    activeTeamData.value.avatarUrl = newUrl
  }
  const compId = props.competition?.id
  if (compId && !isViewingOtherTeam.value) {
    try {
      await updateMyTeamApi(compId, { avatarUrl: newUrl })
    } catch {
    }
  }
}

const memberColumns = computed<DataTableColumns<MemberAchievement>>(() => [
  {
    title: '序号',
    key: 'index',
    width: 64,
    align: 'center',
    render: (_row, idx) => idx + 1
  },
  {
    title: '选手名称',
    key: 'name',
    minWidth: 160,
    render: (row) =>
      h('div', { class: 'flex items-center gap-2' }, [
        h(VAvatar, {
          src: row.avatarUrl || '',
          text: row.name.charAt(0).toUpperCase(),
          size: 26,
          shape: 'circle',
          ring: true
        }),
        h('span', { class: 'truncate max-w-[160px]' }, row.name)
      ])
  },
  {
    title: '角色',
    key: 'role',
    width: 100,
    align: 'center',
    render: (row) => getRoleLabel(row.role)
  },
  {
    title: '强项',
    key: 'strongCategory',
    width: 120,
    align: 'center',
    render: (row) => row.strongCategory
  },
  {
    title: '解题数',
    key: 'solvesCount',
    width: 100,
    align: 'center',
    render: (row) => row.solvesCount
  },
  {
    title: '一血数量',
    key: 'firstBloodCount',
    width: 100,
    align: 'center',
    render: (row) => row.firstBloodCount
  },
  {
    title: '得分',
    key: 'score',
    width: 120,
    align: 'center',
    render: (row) => row.score ?? 0
  }
])

const solveColumns = computed<DataTableColumns<CtfTeamSolveRecordVO>>(() => [
  {
    title: '序号',
    key: 'index',
    width: 64,
    align: 'center',
    render: (_row, idx) => (currentPage.value - 1) * pageSize.value + idx + 1
  },
  {
    title: '答题时间',
    key: 'solvedAt',
    minWidth: 170,
    align: 'center',
    render: (row) => formatSolveTime(row.solvedAt)
  },
  {
    title: '答题人',
    key: 'username',
    minWidth: 120,
    align: 'center',
    render: (row) => row.username || '--'
  },
  {
    title: '题目名称',
    key: 'challengeName',
    minWidth: 180,
    align: 'center',
    render: (row) =>
      h(
        'span',
        { class: 'truncate max-w-[200px] inline-block align-middle' },
        row.challengeName || '--'
      )
  },
  {
    title: '答题方式',
    key: 'submissionMode',
    width: 120,
    align: 'center',
    render: (row) => row.submissionMode || '常规提交'
  },
  {
    title: '题目类型',
    key: 'category',
    width: 100,
    align: 'center',
    render: (row) => row.category || 'Misc'
  },
  {
    title: '得分',
    key: 'points',
    width: 100,
    align: 'center',
    render: (row) => row.points ?? 0
  },
  {
    title: '是否一血',
    key: 'isFirstBlood',
    width: 100,
    align: 'center',
    render: (row) => (row.isFirstBlood ? '是' : '否')
  }
])

async function loadTeamData() {
  const compId = props.competition?.id
  const targetId = currentTeamId.value
  if (!compId || !targetId) {
    teamSolves.value = []
    otherTeamInfo.value = null
    return
  }

  loading.value = true
  try {
    if (isViewingOtherTeam.value) {
      try {
        const teamRes = await getTeamByIdApi(compId, targetId)
        otherTeamInfo.value = teamRes?.data || null
      } catch {
        otherTeamInfo.value = null
      }
    } else {
      otherTeamInfo.value = null
    }

    try {
      const solvesRes = await getTeamSolvesApi(compId, targetId)
      if (solvesRes?.data && Array.isArray(solvesRes.data)) {
        teamSolves.value = solvesRes.data
      } else {
        teamSolves.value = []
      }
    } catch {
      teamSolves.value = []
    }

    if (teamSolves.value.length === 0 && !isViewingOtherTeam.value && props.solvedChallenges.length > 0) {
      teamSolves.value = props.solvedChallenges.map((ch, idx) => ({
        id: ch.id || idx,
        challengeId: ch.id || ch.challengeId,
        challengeName: ch.title || ch.name || '',
        category: ch.categoryName || ch.category || 'Misc',
        submissionMode: ch.isContainer ? '动态容器 Flag' : '静态 Flag',
        userId: null,
        username: props.myTeam?.teamName || '',
        points: ch.currentPoints || 0,
        isFirstBlood: Boolean(ch.topTeams && ch.topTeams[0] === props.myTeam?.teamName),
        solvedAt: new Date().toISOString()
      }))
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.teamId, props.myTeam?.teamId, props.competition?.id],
  () => {
    currentPage.value = 1
    loadTeamData()
  },
  { immediate: true }
)

watch(pageSize, () => {
  currentPage.value = 1
})

onMounted(() => {
  loadTeamData()
})
</script>
