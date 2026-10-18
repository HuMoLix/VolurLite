<template>
  <div class="w-full space-y-6">
    <VCtfScoreboardChart
      :scoreboard="scoreboard"
      :challenges="challenges"
      :start-time="competition?.startTime"
      :end-time="competition?.endTime"
    />

    <div class="rounded-2xl sm:rounded-3xl p-5 sm:p-6 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-google-border-light/70 dark:border-google-border-dark/70 mb-4">
        <div class="flex items-center gap-2">
          <n-icon :size="20" :component="TrophyOutline" class="text-google-yellow dark:text-google-yellow-dark" />
          <h2 class="text-base font-bold text-google-text-primary dark:text-google-text-darkPrimary">
            {{ t('competition.scoreboard_title', '排行榜') }}
          </h2>
        </div>

        <div class="flex items-center gap-2">
          <VBtn
            v-if="scoreboard.length > 0"
            size="small"
            clz="normal"
            round
            :left-icon="DownloadOutline"
            @click="handleExportPublicScoreboard"
          >
            {{ t('competition.export_scoreboard', '导出榜单') }}
          </VBtn>
          <VBtn
            size="small"
            clz="normal"
            round
            :loading="loadingSolves"
            :left-icon="RefreshOutline"
            @click="handleRefresh"
          >
            {{ t('competition.refresh', '刷新') }}
          </VBtn>
        </div>
      </div>

      <VTable
        :columns="columns"
        :data="paginatedScoreboard"
        :loading="loadingSolves"
        :scroll-x="tableScrollX"
        :single-line="false"
        :row-class-name="getRowClassName"
        size="small"
      >
        <template #empty>
          <div class="py-12 text-center text-xs text-google-text-muted dark:text-google-text-darkMuted">
            {{ t('competition.no_scoreboard_data', '暂无榜单数据') }}
          </div>
        </template>
      </VTable>

      <div v-if="scoreboard.length > 0" class="flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-google-border-light/70 dark:border-google-border-dark/70">
        <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
          显示第 {{ (currentPage - 1) * pageSize + 1 }} - {{ Math.min(currentPage * pageSize, scoreboard.length) }} 支队伍，共 {{ scoreboard.length }} 支队伍
        </div>
        <VPagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="scoreboard.length"
          :page-sizes="[10, 20, 50, 100]"
          show-size-picker
          show-quick-jumper
          size="small"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import {
  TrophyOutline,
  RefreshOutline,
  FlagOutline,
  DownloadOutline
} from '@vicons/ionicons5'
import { Medal } from '@vicons/fa'
import { VTooltip, VTable, VBtn, VPagination } from '@/components/common'
import { VCtfScoreboardChart } from '@/components/competition/ctf'
import { getCtfChallengeDetailApi } from '@/api/competition'
import type {
  CtfScoreboardItemVO,
  MyTeamInfoVO,
  CtfCompetitionChallengeVO,
  CompetitionDetailVO
} from '@/types/competition'

defineOptions({
  name: 'CtfCompetitionScoreboardView'
})

const props = withDefaults(
  defineProps<{
    scoreboard: CtfScoreboardItemVO[]
    myTeam: MyTeamInfoVO | null
    challenges?: CtfCompetitionChallengeVO[]
    competition?: CompetitionDetailVO | null
  }>(),
  {
    challenges: () => [],
    competition: null
  }
)

const emit = defineEmits<{
  (e: 'select-team', teamId: string | number): void
}>()

const { t } = useI18n()

interface ChallengeSolveInfo {
  points?: number
  solvedAt?: string
  isFirstBlood?: boolean
  solveRank?: number
}

const loadingSolves = ref(false)
const solvedTeamsMap = ref<Map<string, Map<string, ChallengeSolveInfo>>>(new Map())

const currentPage = ref<number>(1)
const pageSize = ref<number>(20)

const paginatedScoreboard = computed<CtfScoreboardItemVO[]>(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return (props.scoreboard || []).slice(start, start + pageSize.value)
})

watch(pageSize, () => {
  currentPage.value = 1
})

watch(
  () => props.scoreboard,
  () => {
    const maxPage = Math.max(1, Math.ceil((props.scoreboard?.length || 0) / pageSize.value))
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage
    }
  }
)

interface CategoryChallengeGroup {
  name: string
  challenges: CtfCompetitionChallengeVO[]
}

const groupedChallenges = computed<CategoryChallengeGroup[]>(() => {
  const groups: Record<string, CtfCompetitionChallengeVO[]> = {}
  for (const ch of props.challenges || []) {
    const cat = ch.categoryName || ch.category || 'Misc'
    if (!groups[cat]) {
      groups[cat] = []
    }
    groups[cat].push(ch)
  }
  return Object.entries(groups).map(([name, list]) => ({
    name,
    challenges: list
  }))
})

function isChallengeSolved(team: CtfScoreboardItemVO, ch: CtfCompetitionChallengeVO): boolean {
  if (props.myTeam && team.teamName === props.myTeam.teamName && ch.isSolvedByMyTeam) {
    return true
  }

  const chKey = String(ch.id || ch.challengeId)
  const solvesMap = solvedTeamsMap.value.get(chKey)
  if (solvesMap && solvesMap.has(team.teamName)) {
    return true
  }

  if (ch.topTeams && ch.topTeams.includes(team.teamName)) {
    return true
  }

  return false
}

function getTeamSolveRecord(team: CtfScoreboardItemVO, ch: CtfCompetitionChallengeVO): ChallengeSolveInfo | undefined {
  const chKey = String(ch.id || ch.challengeId)
  const solvesMap = solvedTeamsMap.value.get(chKey)
  const solveInfo = solvesMap?.get(team.teamName)
  if (solveInfo) {
    return solveInfo
  }
  if (ch.topTeams && Array.isArray(ch.topTeams)) {
    const topIndex = ch.topTeams.indexOf(team.teamName)
    if (topIndex !== -1) {
      return {
        points: ch.currentPoints,
        isFirstBlood: topIndex === 0,
        solveRank: topIndex + 1
      }
    }
  }
  return undefined
}

function formatSolveDateTime(val?: string | null): string {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function getChallengeSolveTooltip(team: CtfScoreboardItemVO, ch: CtfCompetitionChallengeVO): string {
  const chName = ch.title || ch.name
  const cat = ch.categoryName || ch.category || 'Misc'
  const solved = isChallengeSolved(team, ch)
  if (solved) {
    const solveDetail = getTeamSolveRecord(team, ch)
    const pts = solveDetail?.points ?? ch.currentPoints
    const timeStr = formatSolveDateTime(solveDetail?.solvedAt)
    return timeStr ? `+${pts} ${timeStr}` : `+${pts}`
  }
  return `[${cat}] ${chName} (${ch.currentPoints}) - 尚未解出`
}


const tableScrollX = computed<number>(() => {
  const baseWidth = 371
  const challengeColsWidth = (props.challenges?.length || 0) * 120
  return Math.max(900, baseWidth + challengeColsWidth)
})

function getRowClassName(row: CtfScoreboardItemVO): string {
  if (props.myTeam && row.teamName === props.myTeam.teamName) {
    return 'my-team-row'
  }
  return ''
}

function getMedalIconColor(rank: number): string {
  if (rank === 1) {
    return 'text-amber-500 dark:text-amber-400'
  }
  if (rank === 2) {
    return 'text-slate-400 dark:text-slate-300'
  }
  return 'text-orange-500 dark:text-orange-400'
}

const columns = computed<DataTableColumns<CtfScoreboardItemVO>>(() => {
  const baseCols: DataTableColumns<CtfScoreboardItemVO> = [
    {
      title: () =>
        h(
          'span',
          { class: 'text-[#202124] dark:text-[#e3e3e3] font-medium' },
          t('competition.rank', '排名')
        ),
      key: 'rank',
      width: 76,
      align: 'center' as const,
      fixed: 'left',
      render: (row) =>
        h(
          'span',
          {
            class: 'scoreboard-cell-text cursor-default'
          },
          row.rank
        )
    },
    {
      title: () =>
        h(
          'span',
          { class: 'text-[#202124] dark:text-[#e3e3e3] font-medium' },
          t('competition.team_name', '队伍名称')
        ),
      key: 'teamName',
      width: 200,
      maxWidth: 200,
      align: 'center' as const,
      fixed: 'left',
      render: (row) => {
        return h(
          'div',
          { class: 'flex items-center justify-center min-w-0 max-w-[190px] mx-auto px-1' },
          [
            h(
              VTooltip,
              { content: row.teamName },
              {
                default: () =>
                  h(
                    'span',
                    {
                      class:
                        'scoreboard-cell-text truncate max-w-[180px] block cursor-pointer',
                      onClick: () => emit('select-team', row.teamId)
                    },
                    row.teamName
                  )
              }
            )
          ]
        )
      }
    },
    {
      title: () =>
        h(
          'span',
          { class: 'text-[#202124] dark:text-[#e3e3e3] font-medium' },
          t('competition.score', '总分')
        ),
      key: 'score',
      width: 95,
      align: 'center' as const,
      fixed: 'left',
      render: (row) =>
        h(
          'span',
          {
            class: 'scoreboard-score-cell cursor-default'
          },
          row.score
        )
    }
  ]

  for (const catGroup of groupedChallenges.value) {
    baseCols.push({
      title: () =>
        h(
          'div',
          {
            class:
              'text-xs font-bold uppercase tracking-wider text-[#202124] dark:text-[#e3e3e3] py-0.5 text-center flex items-center justify-center w-full'
          },
          catGroup.name
        ),
      key: `cat_${catGroup.name}`,
      align: 'center' as const,
      children: catGroup.challenges.map((ch) => {
        const chKey = String(ch.id || ch.challengeId)
        return {
          title: () =>
            h(
              VTooltip,
              { content: `[${catGroup.name}] ${ch.title || ch.name} (${ch.currentPoints})` },
              {
                default: () =>
                  h(
                    'div',
                    {
                      class:
                        'flex flex-col items-center justify-center text-center py-0.5 px-1 w-full max-w-[150px] mx-auto cursor-default'
                    },
                    [
                      h(
                        'span',
                        {
                          class:
                            'truncate block w-full max-w-[140px] text-xs font-medium text-[#202124] dark:text-[#e3e3e3] text-center'
                        },
                        ch.title || ch.name
                      ),
                      h(
                        'span',
                        {
                          class: 'text-[10px] scoreboard-score mt-0.5 text-center block w-full'
                        },
                        ch.currentPoints
                      )
                    ]
                  )
              }
            ),
          key: `challenge_${chKey}`,
          minWidth: 96,
          maxWidth: 156,
          align: 'center' as const,
          render: (row: CtfScoreboardItemVO) => {
            const solved = isChallengeSolved(row, ch)
            const tooltipContent = getChallengeSolveTooltip(row, ch)
            if (solved) {
              const solveDetail = getTeamSolveRecord(row, ch)
              const solveRank = solveDetail?.solveRank ?? (solveDetail?.isFirstBlood ? 1 : undefined)
              const isTopThree = solveRank === 1 || solveRank === 2 || solveRank === 3

              return h(
                'div',
                { class: 'flex items-center justify-center w-full' },
                [
                  h(
                    VTooltip,
                    { content: tooltipContent },
                    {
                      default: () =>
                        h(
                          'span',
                          { class: 'inline-flex items-center justify-center cursor-default' },
                          [
                            isTopThree
                              ? h(NIcon, {
                                  size: 18,
                                  component: Medal,
                                  class: `${getMedalIconColor(solveRank!)} shrink-0`
                                })
                              : h(NIcon, {
                                  size: 18,
                                  component: FlagOutline,
                                  class: 'solved-flag-icon text-google-red dark:text-google-red-dark shrink-0'
                                })
                          ]
                        )
                    }
                  )
                ]
              )
            }
            return h(
              'div',
              { class: 'flex items-center justify-center w-full' },
              [
                h(
                  VTooltip,
                  { content: tooltipContent },
                  {
                    default: () =>
                      h(
                        'span',
                        {
                          class:
                            'text-google-text-muted/30 dark:text-google-text-darkMuted/30 text-xs select-none cursor-default py-0.5 px-2'
                        },
                        '—'
                      )
                  }
                )
              ]
            )
          }
        }
      })
    })
  }

  if (groupedChallenges.value.length === 0) {
    baseCols.push(
      {
        title: () =>
          h(
            'span',
            { class: 'text-[#202124] dark:text-[#e3e3e3] font-medium' },
            t('competition.solves', '解题数')
          ),
        key: 'solvesCount',
        width: 100,
        align: 'center' as const,
        render: (row) =>
          h(
            VTooltip,
            { content: `解题数: ${row.solvesCount}` },
            {
              default: () =>
                h(
                  'span',
                  { class: 'scoreboard-cell-text cursor-default' },
                  row.solvesCount
                )
            }
          )
      },
      {
        title: () =>
          h(
            'span',
            { class: 'text-[#202124] dark:text-[#e3e3e3] font-medium' },
            t('competition.last_solve', '最后提交')
          ),
        key: 'lastSolveTime',
        width: 160,
        align: 'center' as const,
        render: (row) =>
          h(
            VTooltip,
            { content: row.lastSolveTime ? `最后解题时间: ${row.lastSolveTime}` : '暂无提交' },
            {
              default: () =>
                h(
                  'span',
                  { class: 'scoreboard-cell-text cursor-default' },
                  row.lastSolveTime || '--'
                )
            }
          )
      }
    )
  }

  return baseCols
})

async function loadChallengeSolves() {
  if (!props.competition?.id || !props.challenges || props.challenges.length === 0) return
  loadingSolves.value = true

  const compId = props.competition.id
  try {
    const promises = props.challenges.map(async (ch) => {
      const chId = ch.id || ch.challengeId
      if (!chId) return
      try {
        const res = await getCtfChallengeDetailApi(compId, chId)
        if (res?.data?.solves) {
          const teamMap = new Map<string, ChallengeSolveInfo>()
          for (let i = 0; i < res.data.solves.length; i++) {
            const s = res.data.solves[i]
            if (s.teamName) {
              teamMap.set(s.teamName, {
                points: s.points,
                solvedAt: s.solvedAt || undefined,
                isFirstBlood: i === 0,
                solveRank: i + 1
              })
            }
          }
          solvedTeamsMap.value.set(String(chId), teamMap)
        }
      } catch {
      }
    })
    await Promise.allSettled(promises)
  } finally {
    loadingSolves.value = false
  }
}

function handleRefresh() {
  loadChallengeSolves()
}

function handleExportPublicScoreboard() {
  if (!props.scoreboard || props.scoreboard.length === 0) return
  const headers = ['排名', '战队名称', '总得分', '解题数']
  const rows = props.scoreboard.map((item) => [
    item.rank ?? '-',
    `"${(item.teamName || '').replace(/"/g, '""')}"`,
    item.score ?? 0,
    item.solvesCount ?? 0
  ])
  const bom = '\uFEFF'
  const csvContent = bom + [headers.join(','), ...rows.map((r) => r.join(','))].join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  const title = (props.competition?.title || 'ctf_scoreboard').replace(/[\\/:*?"<>|\s]/g, '_')
  a.download = `${title}_scoreboard_${Date.now()}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
}

watch(
  () => props.challenges,
  (newVal) => {
    if (newVal && newVal.length > 0) {
      loadChallengeSolves()
    }
  },
  { deep: true }
)

onMounted(() => {
  loadChallengeSolves()
})
</script>

<style scoped lang="scss">
:deep(.n-data-table) {
  font-family: Inter, sans-serif;

  .n-data-table-th {
    font-family: Inter, sans-serif;
    color: #202124;
    text-align: center;

    .n-data-table-th__title-wrapper {
      font-family: Inter, sans-serif;
      display: flex;
      justify-content: center;
      align-items: center;
      width: 100%;
      text-align: center;
      color: #202124;
    }

    .n-data-table-th__title {
      font-family: Inter, sans-serif;
      display: flex;
      justify-content: center;
      align-items: center;
      width: 100%;
      text-align: center;
      color: #202124;
    }
  }

  .n-data-table-th,
  .n-data-table-td {
    font-family: Inter, sans-serif;
    border-right: 1px solid #e0e2e6;
    text-align: center;

    &:last-child {
      border-right: none;
    }
  }

  .n-data-table-td {
    font-family: Inter, sans-serif;
    color: #3c4043;
    font-weight: 500;
  }
}

html.dark {
  :deep(.n-data-table) {
    .n-data-table-th {
      color: #e3e3e3;

      .n-data-table-th__title-wrapper,
      .n-data-table-th__title {
        color: #e3e3e3;
      }
    }

    .n-data-table-td {
      color: #e8eaed;
      font-weight: 500;
    }

    .n-data-table-th,
    .n-data-table-td {
      border-right: 1px solid #3c4043;

      &:last-child {
        border-right: none;
      }
    }
  }
}

:deep(.my-team-row) {
  .n-data-table-td {
    background-color: rgba(26, 115, 232, 0.08) !important;
    font-weight: 600;
  }
}

html.dark {
  :deep(.my-team-row) {
    .n-data-table-td {
      background-color: rgba(168, 199, 250, 0.08) !important;
      font-weight: 600;
    }
  }
}

.scoreboard-cell-text {
  font-family: Inter, sans-serif;
  color: #3c4043;
  font-weight: 500 !important;
  font-size: 14px;

  html.dark & {
    color: #e8eaed;
  }
}

.scoreboard-score-cell {
  font-family: Inter, sans-serif;
  color: #3c4043;
  font-weight: 500 !important;
  font-size: 14px;

  html.dark & {
    color: #e8eaed;
  }
}

.scoreboard-score {
  font-family: Inter, sans-serif;
  color: #5f6368;
  font-weight: 500 !important;
}

html.dark .scoreboard-score {
  color: #9aa0a6;
}

.solved-flag-icon {
  color: #ea4335;

  html.dark & {
    color: #f28b82;
  }

  :deep(svg path) {
    fill: currentColor !important;
  }
}
</style>
