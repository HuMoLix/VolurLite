import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { CompetitionDetailVO, CompositeScoreboardVO, MyTeamInfoVO } from '@/types/competition'
import {
  getCompetitionByIdApi,
  getCompetitionBySlugApi,
  getMyTeamApi,
  getCompetitionScoreboardApi
} from '@/api/competition'

export const useCompetitionStore = defineStore('competition', () => {
  const currentCompetition = ref<CompetitionDetailVO | null>(null)
  const myTeam = ref<MyTeamInfoVO | null>(null)
  const currentScoreboard = ref<CompositeScoreboardVO | null>(null)
  const loading = ref<boolean>(false)
  const teamLoading = ref<boolean>(false)

  // 在途 Promise 缓存池，彻底避免组件生命周期并发时发起的重复 HTTP 请求
  const inFlightCompPromises = new Map<string, Promise<CompetitionDetailVO | null>>()
  const inFlightTeamPromises = new Map<string, Promise<MyTeamInfoVO | null>>()

  /**
   * 根据 slug 获取竞赛详情（具备缓存与在途 Promise 去重复用能力）
   */
  async function fetchCompetitionBySlug(slug: string, force = false): Promise<CompetitionDetailVO | null> {
    if (!slug) return null

    // 1. 若已有当前竞赛缓存且与目标 slug 或 ID 匹配，且非强制刷新，直接返回缓存
    if (
      !force &&
      currentCompetition.value &&
      (currentCompetition.value.slug === slug || String(currentCompetition.value.id) === slug)
    ) {
      return currentCompetition.value
    }

    // 2. 若同 slug 的 HTTP 请求已经在途执行中，直接复用 Promise
    if (inFlightCompPromises.has(slug)) {
      return inFlightCompPromises.get(slug)!
    }

    // 3. 发起真实请求并加入在途池
    const promise = (async () => {
      loading.value = true
      try {
        const res = await getCompetitionBySlugApi(slug)
        currentCompetition.value = res.data || null
        return res.data || null
      } catch (err) {
        currentCompetition.value = null
        throw err
      } finally {
        loading.value = false
        inFlightCompPromises.delete(slug)
      }
    })()

    inFlightCompPromises.set(slug, promise)
    return promise
  }

  /**
   * 根据 ID 获取竞赛详情（具备缓存与在途 Promise 去重复用能力）
   */
  async function fetchCompetitionDetail(id: number | string, force = false): Promise<CompetitionDetailVO | null> {
    const key = String(id)
    if (!id) return null

    if (!force && currentCompetition.value && String(currentCompetition.value.id) === key) {
      return currentCompetition.value
    }

    if (inFlightCompPromises.has(key)) {
      return inFlightCompPromises.get(key)!
    }

    const promise = (async () => {
      loading.value = true
      try {
        const res = await getCompetitionByIdApi(id)
        currentCompetition.value = res.data || null
        return res.data || null
      } catch (err) {
        currentCompetition.value = null
        throw err
      } finally {
        loading.value = false
        inFlightCompPromises.delete(key)
      }
    })()

    inFlightCompPromises.set(key, promise)
    return promise
  }

  /**
   * 获取登录选手在指定竞赛中的战队信息（具备在途 Promise 去重复用能力）
   */
  async function fetchMyTeam(competitionId: number | string, force = false): Promise<MyTeamInfoVO | null> {
    const key = String(competitionId)
    if (!competitionId) return null

    if (!force && myTeam.value && String(myTeam.value.competitionId || '') === key) {
      return myTeam.value
    }

    if (inFlightTeamPromises.has(key)) {
      return inFlightTeamPromises.get(key)!
    }

    const promise = (async () => {
      teamLoading.value = true
      try {
        const res = await getMyTeamApi(competitionId)
        myTeam.value = res.data || null
        return res.data || null
      } catch (err) {
        myTeam.value = null
        throw err
      } finally {
        teamLoading.value = false
        inFlightTeamPromises.delete(key)
      }
    })()

    inFlightTeamPromises.set(key, promise)
    return promise
  }

  /**
   * 获取计分板数据
   */
  async function fetchScoreboard(competitionId: number | string): Promise<CompositeScoreboardVO | null> {
    try {
      const res = await getCompetitionScoreboardApi(competitionId)
      currentScoreboard.value = res.data || null
      return res.data || null
    } catch {
      return null
    }
  }

  /**
   * 清理竞赛状态与在途缓存
   */
  function clearCompetitionState(): void {
    currentCompetition.value = null
    myTeam.value = null
    currentScoreboard.value = null
    inFlightCompPromises.clear()
    inFlightTeamPromises.clear()
  }

  return {
    currentCompetition,
    myTeam,
    currentScoreboard,
    loading,
    teamLoading,
    fetchCompetitionBySlug,
    fetchCompetitionDetail,
    fetchMyTeam,
    fetchScoreboard,
    clearCompetitionState
  }
})
