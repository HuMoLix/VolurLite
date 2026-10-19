<template>
  <div class="flex flex-col lg:flex-row gap-6 items-start">
    <aside class="w-full lg:w-60 flex-shrink-0 flex flex-col gap-4">
      <VCtfCategory
        v-model="selectedCategory"
        :categories="categories"
        :category-counts="categoryCountsMap"
        :total-count="challenges.length"
      />
      <VSearch
        v-model:value="searchQuery"
        :placeholder="t('competition.search_challenge', '搜索赛题名称或分类...')"
        clearable
        round
      />
      <VCtfCompetitionStatistic
        :competition="competition"
        :team="myTeam"
        :score="myTeamScore"
        :solves-count="solvedChallengesCount"
        :rank="myTeamRank"
        @time-up="emit('time-up')"
      />
    </aside>

    <main class="flex-1 min-w-0 w-full space-y-6">
      <div v-if="filteredChallenges.length === 0" class="py-20 text-center space-y-3 rounded-2xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark">
        <n-icon :size="48" :component="FlashOutline" class="text-google-text-muted dark:text-google-text-darkMuted mx-auto" />
        <div class="text-sm text-google-text-desc dark:text-google-text-darkDesc">
          {{ t('competition.no_challenges_found', '当前分类或搜索条件下暂无赛题') }}
        </div>
      </div>

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-5">
        <CtfCompetitionChallengeCard
          v-for="ch in filteredChallenges"
          :key="ch.id"
          :challenge="ch"
          @click="openChallengeDetail(ch)"
        />
      </div>
    </main>

    <CtfCompetitionChallengeModal
      v-model:show="detailModalVisible"
      :competition-id="competition?.id"
      :challenge="activeChallengeDetail"
      :my-team="myTeam"
      :scoreboard="scoreboard"
      @flag-solved="handleFlagSolvedModal"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, useMessage } from 'naive-ui'
import { FlashOutline } from '@vicons/ionicons5'
import { VSearch } from '@/components/common'
import { VCtfCategory, VCtfCompetitionStatistic } from '@/components/competition'
import { CtfCompetitionChallengeCard, CtfCompetitionChallengeModal } from '../components'
import { getCtfChallengeDetailApi } from '@/api/competition'
import type {
  CompetitionDetailVO,
  CtfCompetitionChallengeVO,
  CtfCompetitionChallengeDetailVO,
  MyTeamInfoVO,
  CtfScoreboardItemVO
} from '@/types/competition'

defineOptions({
  name: 'CtfCompetitionChallengeListView'
})

const props = withDefaults(
  defineProps<{
    competition: CompetitionDetailVO | null
    challenges: CtfCompetitionChallengeVO[]
    myTeam?: MyTeamInfoVO | null
    scoreboard?: CtfScoreboardItemVO[]
  }>(),
  {
    myTeam: null,
    scoreboard: () => []
  }
)

const emit = defineEmits<{
  (e: 'flag-solved', payload: { challengeId: number | string; points: number }): void
  (e: 'time-up'): void
}>()

const { t } = useI18n()

const selectedCategory = ref<string>('全部')
const searchQuery = ref<string>('')
const detailModalVisible = ref<boolean>(false)
const activeChallengeDetail = ref<CtfCompetitionChallengeDetailVO | null>(null)

const myTeamScoreboardItem = computed<CtfScoreboardItemVO | null>(() => {
  if (!props.myTeam) return null
  return (
    (props.scoreboard || []).find(
      (s) =>
        (props.myTeam?.teamId && String(s.teamId) === String(props.myTeam.teamId)) ||
        s.teamName === props.myTeam?.teamName
    ) || null
  )
})

const solvedChallengesCount = computed<number>(() => {
  if (myTeamScoreboardItem.value?.solvesCount !== undefined) {
    return myTeamScoreboardItem.value.solvesCount
  }
  return props.challenges.filter((ch) => ch.isSolvedByMyTeam).length
})

const myTeamScore = computed<number>(() => {
  if (myTeamScoreboardItem.value?.score !== undefined) {
    return myTeamScoreboardItem.value.score
  }
  return props.challenges
    .filter((ch) => ch.isSolvedByMyTeam)
    .reduce((acc, curr) => acc + (curr.currentPoints || 0), 0)
})

const myTeamRank = computed<number | string>(() => {
  return myTeamScoreboardItem.value?.rank ?? '-'
})

const categories = computed<string[]>(() => {
  const set = new Set<string>(['全部'])
  for (const ch of props.challenges) {
    const cat = ch.category || ch.categoryName
    if (cat) {
      set.add(cat)
    }
  }
  return Array.from(set)
})

const categoryCountsMap = computed<Record<string, number>>(() => {
  const map: Record<string, number> = {
    '全部': props.challenges.length
  }
  for (const ch of props.challenges) {
    const cat = ch.category || ch.categoryName
    if (cat) {
      map[cat] = (map[cat] || 0) + 1
    }
  }
  return map
})

const filteredChallenges = computed<CtfCompetitionChallengeVO[]>(() => {
  return props.challenges.filter((ch) => {
    const chCategory = ch.category || ch.categoryName || ''
    const chTitle = ch.title || ch.name || ''
    const matchCat = selectedCategory.value === '全部' || chCategory === selectedCategory.value
    const matchSearch =
      !searchQuery.value.trim() ||
      chTitle.toLowerCase().includes(searchQuery.value.trim().toLowerCase()) ||
      chCategory.toLowerCase().includes(searchQuery.value.trim().toLowerCase())
    return matchCat && matchSearch
  })
})

const message = useMessage()

async function openChallengeDetail(ch: CtfCompetitionChallengeVO) {
  if (!props.competition?.id) return
  const targetId = ch.id || ch.challengeId
  try {
    const res = await getCtfChallengeDetailApi(props.competition.id, targetId)
    if (res?.data) {
      const topTeams = (res.data.topTeams && res.data.topTeams.length > 0) ? res.data.topTeams : ch.topTeams
      activeChallengeDetail.value = { ...res.data, topTeams }
      detailModalVisible.value = true
    }
  } catch (err: any) {
    if (message) {
      message.error(err?.message || t('competition.fetch_challenge_failed', '获取赛题详情失败'))
    }
  }
}

function handleFlagSolvedModal(payload: { challengeId: number | string; points: number }) {
  const targetId = payload.challengeId
  const found = props.challenges.find((c) => c.id === targetId || c.challengeId === targetId)
  if (found) {
    found.isSolvedByMyTeam = true
    found.solvesCount++
    if (props.myTeam?.teamName) {
      if (!found.topTeams) {
        found.topTeams = [props.myTeam.teamName]
      } else if (!found.topTeams.includes(props.myTeam.teamName) && found.topTeams.length < 3) {
        found.topTeams.push(props.myTeam.teamName)
      }
    }
  }
  emit('flag-solved', payload)
}
</script>
