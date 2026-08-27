import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { CompetitionDetailVO, CompositeScoreboardVO } from '@/types/competition'
import { getCompetitionByIdApi, getCompetitionScoreboardApi } from '@/api/competition'

export const useCompetitionStore = defineStore('competition', () => {
  const currentCompetition = ref<CompetitionDetailVO | null>(null)
  const currentScoreboard = ref<CompositeScoreboardVO | null>(null)
  const loading = ref<boolean>(false)

  async function fetchCompetitionDetail(id: number | string): Promise<CompetitionDetailVO | null> {
    loading.value = true
    try {
      const res = await getCompetitionByIdApi(id)
      currentCompetition.value = res.data
      return res.data
    } catch {
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchScoreboard(competitionId: number | string): Promise<CompositeScoreboardVO | null> {
    try {
      const res = await getCompetitionScoreboardApi(competitionId)
      currentScoreboard.value = res.data
      return res.data
    } catch {
      return null
    }
  }

  function clearCompetitionState(): void {
    currentCompetition.value = null
    currentScoreboard.value = null
  }

  return {
    currentCompetition,
    currentScoreboard,
    loading,
    fetchCompetitionDetail,
    fetchScoreboard,
    clearCompetitionState
  }
})
