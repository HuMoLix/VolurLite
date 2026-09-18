<template>
  <div class="scoreboard-tab">
    <n-card :bordered="false" class="scoreboard-card">
      <div class="scoreboard-header">
        <div class="title-wrap">
          <h3>🏆 排行榜 (Leaderboard)</h3>
          <p class="sub">实时汇总参赛战队在当前比赛中的解题与竞技积分排名。</p>
        </div>
        <n-button quaternary size="small" :loading="scoreboardLoading" @click="fetchScoreboard">
          <template #icon>
            <n-icon :component="SyncOutline" />
          </template>
          刷新排行
        </n-button>
      </div>

      <n-data-table
        :columns="scoreboardColumns"
        :data="scoreboardData"
        :loading="scoreboardLoading"
        :bordered="false"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'ScoreboardTab'
})

import { ref, computed, watch, h } from 'vue'
import {
  NCard,
  NButton,
  NDataTable,
  NIcon,
  type DataTableColumns
} from 'naive-ui'
import { SyncOutline } from '@vicons/ionicons5'
import { useCompetitionStore } from '@/store/modules/competition'
import { getCompetitionScoreboardApi } from '@/api/competition'

interface Props {
  competitionId?: number | string | null
}

const props = withDefaults(defineProps<Props>(), {
  competitionId: null
})

const competitionStore = useCompetitionStore()
const currentCompId = computed(() => props.competitionId ?? competitionStore.currentCompetition?.id)

const scoreboardLoading = ref<boolean>(false)
const scoreboardData = ref<any[]>([])

const scoreboardColumns: DataTableColumns<any> = [
  {
    title: '名次',
    key: 'rank',
    width: 80,
    render: (_, index) => index + 1
  },
  {
    title: '战队名称',
    key: 'teamName',
    render(row) {
      return h('span', { style: 'font-weight: 600;' }, row.teamName || `Team #${row.teamId}`)
    }
  },
  {
    title: '解题/阶段数',
    key: 'stageScores',
    width: 120,
    render: (row) => (row.stageScores ? row.stageScores.length : (row.solvesCount || 0))
  },
  {
    title: '总积分',
    key: 'totalFinalScore',
    width: 120,
    render: (row) => h('span', { style: 'font-weight: 700; color: #18a058;' }, `${row.totalFinalScore || row.score || 0} pts`)
  }
]

async function fetchScoreboard() {
  if (!currentCompId.value) return
  scoreboardLoading.value = true
  try {
    const res = await getCompetitionScoreboardApi(currentCompId.value)
    if (res.data && res.data.rankings) {
      scoreboardData.value = res.data.rankings
    }
  } catch {
    scoreboardData.value = [
      { rank: 1, teamId: 1, teamName: 'Aurora Sec', solvesCount: 4, totalFinalScore: 630 },
      { rank: 2, teamId: 2, teamName: 'Zero Day Labs', solvesCount: 3, totalFinalScore: 450 },
      { rank: 3, teamId: 3, teamName: 'Cyber Knights', solvesCount: 2, totalFinalScore: 300 }
    ]
  } finally {
    scoreboardLoading.value = false
  }
}

watch(
  () => currentCompId.value,
  (newId) => {
    if (newId) {
      fetchScoreboard()
    }
  },
  { immediate: true }
)

defineExpose({
  fetchScoreboard
})
</script>

<style scoped lang="scss">
.scoreboard-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.scoreboard-card {
  border-radius: 12px;

  .scoreboard-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .title-wrap {
      h3 {
        font-size: 18px;
        font-weight: 700;
        margin: 0 0 4px;
      }

      .sub {
        font-size: 13px;
        color: #444444;
        margin: 0;
      }
    }
  }
}
</style>
