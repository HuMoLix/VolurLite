<template>
  <div class="rounded-2xl sm:rounded-3xl p-5 sm:p-6 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-google-border-light/70 dark:border-google-border-dark/70 mb-4">
      <div class="flex items-center gap-2">
        <n-icon :size="20" :component="TrendingUpOutline" class="text-google-blue dark:text-google-blue-dark" />
        <h2 class="text-base font-bold text-google-text-primary dark:text-google-text-darkPrimary">
          {{ t('competition.score_chart_title', '解题积分趋势') }}
        </h2>
      </div>
    </div>

    <div v-if="topTeams.length === 0" class="py-16 text-center text-google-text-muted dark:text-google-text-darkMuted text-sm">
      {{ t('competition.no_chart_data', '暂无解题积分趋势数据') }}
    </div>

    <div v-else class="space-y-4">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="flex flex-wrap items-center gap-2">
          <VBtn
            v-for="(team, index) in chartSeries"
            :key="team.teamId"
            size="small"
            round
            clz="normal"
            class="!h-7 !px-2.5 !py-0 text-xs transition-all"
            :class="[
              highlightedTeamId === team.teamId
                ? '!border-google-blue dark:!border-google-blue-dark ring-2 ring-blue-400/30 shadow-sm'
                : '',
              hiddenTeamIds.has(team.teamId)
                ? 'opacity-40 border-dashed !bg-transparent text-gray-400'
                : ''
            ]"
            @click="toggleTeamVisibility(team.teamId)"
            @mouseenter="highlightedTeamId = team.teamId"
            @mouseleave="highlightedTeamId = null"
          >
            <span
              class="w-2 h-2 rounded-full shrink-0 mr-1.5"
              :style="{ backgroundColor: team.color }"
            />
            <span class="font-mono text-[10px] opacity-70 mr-1">#{{ index + 1 }}</span>
            <span class="truncate max-w-[100px] mr-1">{{ team.teamName }}</span>
            <span class="font-mono text-[10px] font-bold" :style="{ color: team.color }">
              {{ team.finalScore }}p
            </span>
          </VBtn>
        </div>

        <div class="flex items-center gap-2 shrink-0">
          <VBtn
            size="small"
            clz="normal"
            round
            @click="showAllTeams"
          >
            {{ t('competition.show_all', '全部显示') }}
          </VBtn>
          <VBtn
            size="small"
            clz="normal"
            round
            @click="hideAllTeams"
          >
            {{ t('competition.hide_all', '全部隐藏') }}
          </VBtn>
        </div>
      </div>

      <div class="relative w-full overflow-hidden select-none" ref="chartContainerRef">
        <svg
          class="w-full h-64 sm:h-72 overflow-visible"
          viewBox="0 0 960 300"
          preserveAspectRatio="none"
          @mousemove="handleMouseMove"
          @mouseleave="handleMouseLeave"
        >
          <defs>
            <linearGradient
              v-for="team in visibleSeries"
              :key="'grad-' + team.teamId"
              :id="'grad-' + team.teamId"
              x1="0"
              y1="0"
              x2="0"
              y2="1"
            >
              <stop offset="0%" :stop-color="team.color" stop-opacity="0.15" />
              <stop offset="100%" :stop-color="team.color" stop-opacity="0.0" />
            </linearGradient>
          </defs>

          <g class="grid-lines">
            <line
              v-for="(tick, i) in yTicks"
              :key="'ytick-' + i"
              :x1="padding.left"
              :y1="tick.y"
              :x2="padding.left + chartWidth"
              :y2="tick.y"
              stroke="currentColor"
              stroke-width="1"
              stroke-dasharray="3 3"
              class="text-google-border-light/80 dark:text-google-border-dark/60"
            />
            <text
              v-for="(tick, i) in yTicks"
              :key="'ytext-' + i"
              :x="padding.left - 10"
              :y="tick.y + 4"
              text-anchor="end"
              class="text-[10px] fill-google-text-muted dark:fill-google-text-darkMuted font-mono"
            >
              {{ tick.value }}
            </text>
          </g>

          <g class="x-axis-labels">
            <text
              v-for="(tick, i) in xTicks"
              :key="'xtick-' + i"
              :x="tick.x"
              :y="padding.top + chartHeight + 20"
              text-anchor="middle"
              class="text-[10px] fill-google-text-muted dark:fill-google-text-darkMuted font-mono"
            >
              {{ tick.label }}
            </text>
          </g>

          <g class="paths">
            <g v-for="team in visibleSeries" :key="'series-' + team.teamId">
              <path
                :d="team.areaPath"
                :fill="'url(#grad-' + team.teamId + ')'"
                class="transition-opacity duration-200"
                :class="highlightedTeamId && highlightedTeamId !== team.teamId ? 'opacity-20' : 'opacity-100'"
              />
              <path
                :d="team.linePath"
                fill="none"
                :stroke="team.color"
                :stroke-width="highlightedTeamId === team.teamId ? 3.5 : 2"
                stroke-linejoin="round"
                stroke-linecap="round"
                class="transition-all duration-200"
                :class="highlightedTeamId && highlightedTeamId !== team.teamId ? 'opacity-25' : 'opacity-100'"
              />
              <circle
                v-for="(pt, pIdx) in team.points"
                :key="'pt-' + pIdx"
                :cx="pt.x"
                :cy="pt.y"
                :r="highlightedTeamId === team.teamId ? 4.5 : 3"
                :fill="team.color"
                stroke="#ffffff"
                stroke-width="1.5"
                class="transition-transform duration-200 cursor-pointer"
                :class="highlightedTeamId && highlightedTeamId !== team.teamId ? 'opacity-25' : 'opacity-100'"
              />
            </g>
          </g>

          <g v-if="hoverInfo" class="crosshair-indicator">
            <line
              :x1="hoverInfo.x"
              :y1="padding.top"
              :x2="hoverInfo.x"
              :y2="padding.top + chartHeight"
              stroke="#8ab4f8"
              stroke-width="1.5"
              stroke-dasharray="4 4"
            />
          </g>
        </svg>

        <div
          v-if="hoverInfo"
          class="pointer-events-none absolute z-20 p-2.5 rounded-xl bg-white/95 dark:bg-google-surface-darkElevated/95 backdrop-blur-sm border border-google-border-light dark:border-google-border-dark shadow-lg text-xs space-y-1.5 transition-all"
          :style="{
            left: `${Math.min(Math.max(hoverInfo.relativeX, 20), chartContainerWidth - 190)}px`,
            top: '12px'
          }"
        >
          <div class="font-mono text-[11px] text-google-text-muted dark:text-google-text-darkMuted border-b border-google-border-light/60 dark:border-google-border-dark/60 pb-1 flex items-center justify-between gap-4">
            <span>{{ t('competition.timeline_point', '时间节点') }}</span>
            <span class="font-semibold text-google-text-primary dark:text-google-text-darkPrimary">{{ hoverInfo.timeLabel }}</span>
          </div>
          <div class="space-y-1 max-h-40 overflow-y-auto pr-1">
            <div
              v-for="tRank in hoverInfo.teamScores"
              :key="tRank.teamId"
              class="flex items-center justify-between gap-3 font-mono"
            >
              <div class="flex items-center gap-1.5 truncate max-w-[120px]">
                <span class="w-2 h-2 rounded-full shrink-0" :style="{ backgroundColor: tRank.color }" />
                <span class="truncate font-sans font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                  {{ tRank.teamName }}
                </span>
              </div>
              <span class="font-bold shrink-0" :style="{ color: tRank.color }">
                {{ tRank.score }} pts
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon } from 'naive-ui'
import { TrendingUpOutline } from '@vicons/ionicons5'
import { VBtn } from '@/components/common'
import type { CtfScoreboardItemVO, CtfCompetitionChallengeVO } from '@/types/competition'

defineOptions({
  name: 'VCtfScoreboardChart'
})

const props = withDefaults(
  defineProps<{
    scoreboard: CtfScoreboardItemVO[]
    challenges?: CtfCompetitionChallengeVO[]
    startTime?: string | null
    endTime?: string | null
  }>(),
  {
    challenges: () => [],
    startTime: null,
    endTime: null
  }
)

const { t } = useI18n()

const chartContainerRef = ref<HTMLElement | null>(null)
const chartContainerWidth = ref<number>(800)
const highlightedTeamId = ref<number | string | null>(null)
const hiddenTeamIds = ref<Set<number | string>>(new Set())

interface HoverInfo {
  x: number
  relativeX: number
  timeLabel: string
  teamScores: Array<{
    teamId: number | string
    teamName: string
    color: string
    score: number
  }>
}

const hoverInfo = ref<HoverInfo | null>(null)

const TEAM_COLORS = [
  '#1a73e8',
  '#ea4335',
  '#fbbc04',
  '#34a853',
  '#24c1e0',
  '#af5cf7',
  '#fa7b17',
  '#009688',
  '#e91e63',
  '#607d8b'
]

const padding = { top: 20, right: 30, bottom: 40, left: 60 }
const svgWidth = 960
const svgHeight = 300
const chartWidth = svgWidth - padding.left - padding.right
const chartHeight = svgHeight - padding.top - padding.bottom

const topTeams = computed<CtfScoreboardItemVO[]>(() => {
  return props.scoreboard.slice(0, 10)
})

function parseTimestamp(dateStr?: string | null): number {
  if (!dateStr) return 0
  const d = new Date(dateStr)
  return isNaN(d.getTime()) ? 0 : d.getTime()
}

function formatTime(timestamp: number | string): string {
  const d = new Date(timestamp)
  if (isNaN(d.getTime())) return ''
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  const ss = String(d.getSeconds()).padStart(2, '0')
  return `${hh}:${mm}:${ss}`
}

function toggleTeamVisibility(teamId: number | string) {
  if (hiddenTeamIds.value.has(teamId)) {
    hiddenTeamIds.value.delete(teamId)
  } else {
    hiddenTeamIds.value.add(teamId)
  }
}

function showAllTeams() {
  hiddenTeamIds.value.clear()
}

function hideAllTeams() {
  hiddenTeamIds.value = new Set(chartSeries.value.map((s) => s.teamId))
}

interface TeamDataPoint {
  x: number
  y: number
  score: number
  time: number
}

interface TeamChartSeries {
  teamId: number | string
  teamName: string
  color: string
  finalScore: number
  points: TeamDataPoint[]
  linePath: string
  areaPath: string
  rawProgression: Array<{ time: number; score: number }>
}

const timeBounds = computed<{ min: number; max: number }>(() => {
  let min = parseTimestamp(props.startTime)
  let max = parseTimestamp(props.endTime)
  const now = Date.now()

  const solveTimes: number[] = []
  for (const team of topTeams.value) {
    const t = parseTimestamp(team.lastSolveTime)
    if (t > 0) solveTimes.push(t)
  }

  if (solveTimes.length > 0) {
    const minSolve = Math.min(...solveTimes)
    const maxSolve = Math.max(...solveTimes)
    if (min === 0 || min > minSolve) {
      min = minSolve - 3600 * 1000
    }
    if (max === 0 || max < maxSolve) {
      max = Math.max(maxSolve + 1800 * 1000, now)
    }
  } else {
    if (min === 0) min = now - 3600 * 4 * 1000
    if (max === 0) max = now
  }

  if (max <= min) {
    max = min + 3600 * 4 * 1000
  }

  return { min, max }
})

const maxScoreValue = computed<number>(() => {
  let highest = 0
  for (const team of topTeams.value) {
    if (team.score > highest) highest = team.score
  }
  return Math.max(highest * 1.15, 100)
})

const yTicks = computed(() => {
  const count = 5
  const max = maxScoreValue.value
  const step = Math.ceil(max / count / 50) * 50
  const ticks = []
  for (let i = 0; i <= count; i++) {
    const val = i * step
    const y = padding.top + chartHeight - (val / (step * count)) * chartHeight
    ticks.push({ value: val, y })
  }
  return ticks
})

const xTicks = computed(() => {
  const count = 5
  const { min, max } = timeBounds.value
  const diff = max - min
  const ticks = []
  for (let i = 0; i <= count; i++) {
    const t = min + (diff * i) / count
    const x = padding.left + (chartWidth * i) / count
    ticks.push({ label: formatTime(t), x, time: t })
  }
  return ticks
})

const chartSeries = computed<TeamChartSeries[]>(() => {
  const { min: minT, max: maxT } = timeBounds.value
  const maxS = maxScoreValue.value
  const diffT = maxT - minT || 1

  return topTeams.value.map((team, index) => {
    const color = TEAM_COLORS[index % TEAM_COLORS.length]
    const progression: Array<{ time: number; score: number }> = []

    progression.push({ time: minT, score: 0 })

    const teamLastSolve = parseTimestamp(team.lastSolveTime)
    const solvesCount = Math.max(team.solvesCount || 1, 1)

    if (team.score > 0) {
      const finishTime = teamLastSolve > minT ? teamLastSolve : minT + diffT * 0.8
      const timeInterval = (finishTime - minT) / solvesCount
      const scoreStep = team.score / solvesCount

      for (let sIdx = 1; sIdx <= solvesCount; sIdx++) {
        const ptTime = Math.min(minT + timeInterval * sIdx, maxT)
        const ptScore = Math.round(scoreStep * sIdx)
        progression.push({ time: ptTime, score: ptScore })
      }

      if (finishTime < maxT) {
        progression.push({ time: maxT, score: team.score })
      }
    } else {
      progression.push({ time: maxT, score: 0 })
    }

    const points: TeamDataPoint[] = progression.map((pt) => {
      const x = padding.left + ((pt.time - minT) / diffT) * chartWidth
      const y = padding.top + chartHeight - (pt.score / maxS) * chartHeight
      return {
        x: Math.min(Math.max(x, padding.left), padding.left + chartWidth),
        y: Math.min(Math.max(y, padding.top), padding.top + chartHeight),
        score: pt.score,
        time: pt.time
      }
    })

    let linePath = ''
    let areaPath = ''
    if (points.length > 0) {
      linePath = `M ${points[0].x} ${points[0].y}`
      for (let p = 1; p < points.length; p++) {
        linePath += ` H ${points[p].x} V ${points[p].y}`
      }

      areaPath = `${linePath} V ${padding.top + chartHeight} H ${points[0].x} Z`
    }

    return {
      teamId: team.teamId,
      teamName: team.teamName,
      color,
      finalScore: team.score,
      points,
      linePath,
      areaPath,
      rawProgression: progression
    }
  })
})

const visibleSeries = computed(() => {
  return chartSeries.value.filter((s) => !hiddenTeamIds.value.has(s.teamId))
})

function handleMouseMove(e: MouseEvent) {
  if (!chartContainerRef.value) return
  const rect = chartContainerRef.value.getBoundingClientRect()
  chartContainerWidth.value = rect.width

  const mouseX = e.clientX - rect.left
  const svgScale = svgWidth / rect.width
  const svgX = mouseX * svgScale

  if (svgX < padding.left || svgX > padding.left + chartWidth) {
    hoverInfo.value = null
    return
  }

  const { min, max } = timeBounds.value
  const ratio = (svgX - padding.left) / chartWidth
  const curTime = min + (max - min) * ratio

  const teamScores = visibleSeries.value
    .map((series) => {
      let scoreAtT = 0
      for (const pt of series.rawProgression) {
        if (pt.time <= curTime) {
          scoreAtT = pt.score
        } else {
          break
        }
      }
      return {
        teamId: series.teamId,
        teamName: series.teamName,
        color: series.color,
        score: scoreAtT
      }
    })
    .sort((a, b) => b.score - a.score)

  hoverInfo.value = {
    x: svgX,
    relativeX: mouseX,
    timeLabel: formatTime(curTime),
    teamScores
  }
}

function handleMouseLeave() {
  hoverInfo.value = null
}

function updateContainerWidth() {
  if (chartContainerRef.value) {
    chartContainerWidth.value = chartContainerRef.value.getBoundingClientRect().width
  }
}

onMounted(() => {
  updateContainerWidth()
  window.addEventListener('resize', updateContainerWidth)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateContainerWidth)
})
</script>
