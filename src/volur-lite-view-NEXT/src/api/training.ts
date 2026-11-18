import { del, get, patch, post } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type { ChallengeInstanceResponseVO } from '@/types/container'
import type {
  AdminTrainingChallengeQuery,
  TrainingCategoryVO,
  TrainingChallengeDetailVO,
  TrainingChallengeQuery,
  TrainingChallengeUpdateDTO,
  TrainingChallengeVO,
  TrainingFlagResultVO,
  TrainingImportResultVO,
  TrainingLeaderboardVO,
  TrainingProgressVO
} from '@/types/training'

/**
 * 后端把 long 序列化为字符串（避免 JS 精度丢失），这里统一转成 number，
 * 保证前端做计数、进度计算时不会出现 "5" + 1 = "51" 之类的拼接问题。
 */
function toNumber(value: unknown, fallback = 0): number {
  if (value === null || value === undefined || value === '') return fallback
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function normalizeChallenge<T extends TrainingChallengeVO>(item: T): T {
  return {
    ...item,
    points: toNumber(item.points),
    solvedCount: toNumber(item.solvedCount)
  }
}

function normalizeChallengeDetail(item: TrainingChallengeDetailVO): TrainingChallengeDetailVO {
  return {
    ...item,
    points: toNumber(item.points),
    solvedCount: toNumber(item.solvedCount)
  }
}

function normalizeProgress(progress: TrainingProgressVO): TrainingProgressVO {
  return {
    ...progress,
    totalChallenges: toNumber(progress.totalChallenges),
    solvedCount: toNumber(progress.solvedCount),
    totalPoints: toNumber(progress.totalPoints),
    rank: toNumber(progress.rank),
    categories: (progress.categories || []).map((category) => ({
      ...category,
      total: toNumber(category.total),
      solved: toNumber(category.solved)
    }))
  }
}

function normalizeLeaderboard(leaderboard: TrainingLeaderboardVO): TrainingLeaderboardVO {
  return {
    items: (leaderboard.items || []).map((item) => ({
      ...item,
      rank: toNumber(item.rank),
      points: toNumber(item.points),
      solvedCount: toNumber(item.solvedCount)
    })),
    myRank: leaderboard.myRank
      ? {
          ...leaderboard.myRank,
          rank: toNumber(leaderboard.myRank.rank),
          points: toNumber(leaderboard.myRank.points),
          solvedCount: toNumber(leaderboard.myRank.solvedCount)
        }
      : null
  }
}

function normalizeChallengePage(
  data: PageResponse<TrainingChallengeVO>
): PageResponse<TrainingChallengeVO> {
  return {
    ...data,
    list: (data.list || []).map(normalizeChallenge),
    total: toNumber(data.total),
    totalPages: toNumber(data.totalPages),
    currentPage: toNumber(data.currentPage, 1)
  }
}

export async function getTrainingChallengeListApi(
  params?: TrainingChallengeQuery
): Promise<ApiResponse<PageResponse<TrainingChallengeVO>>> {
  const res = await get<PageResponse<TrainingChallengeVO>>('/training/challenges', { params })
  if (res?.data) {
    res.data = normalizeChallengePage(res.data)
  }
  return res
}

export async function getTrainingChallengeDetailApi(
  id: number | string
): Promise<ApiResponse<TrainingChallengeDetailVO>> {
  const res = await get<TrainingChallengeDetailVO>(`/training/challenges/${id}`)
  if (res?.data) {
    res.data = normalizeChallengeDetail(res.data)
  }
  return res
}

export async function submitTrainingFlagApi(
  id: number | string,
  flag: string
): Promise<ApiResponse<TrainingFlagResultVO>> {
  const res = await post<TrainingFlagResultVO>(`/training/challenges/${id}/flag`, { flag })
  if (res?.data) {
    res.data.pointsEarned = toNumber(res.data.pointsEarned)
    res.data.totalPoints = toNumber(res.data.totalPoints)
  }
  return res
}

/**
 * 查询训练题当前用户的靶机状态。
 * 没有运行中的实例时后端返回 404，这里默认静默，由调用方决定展示方式。
 */
export async function getTrainingInstanceApi(
  id: number | string,
  silent = false
): Promise<ApiResponse<ChallengeInstanceResponseVO>> {
  const res = await get<ChallengeInstanceResponseVO>(
    `/training/challenges/${id}/instance`,
    silent ? { skipToast: true } : undefined
  )
  if (res?.data) {
    res.data.remainingTimeSeconds = toNumber(res.data.remainingTimeSeconds)
  }
  return res
}

export async function getTrainingCategoriesApi(): Promise<ApiResponse<TrainingCategoryVO[]>> {
  const res = await get<TrainingCategoryVO[]>('/training/categories')
  if (res?.data) {
    res.data = (res.data || []).map((category) => ({
      ...category,
      total: toNumber(category.total),
      solved: toNumber(category.solved)
    }))
  }
  return res
}

export async function getTrainingProgressApi(): Promise<ApiResponse<TrainingProgressVO>> {
  const res = await get<TrainingProgressVO>('/training/progress')
  if (res?.data) {
    res.data = normalizeProgress(res.data)
  }
  return res
}

export async function getTrainingLeaderboardApi(
  limit = 10
): Promise<ApiResponse<TrainingLeaderboardVO>> {
  const res = await get<TrainingLeaderboardVO>('/training/leaderboard', { params: { limit } })
  if (res?.data) {
    res.data = normalizeLeaderboard(res.data)
  }
  return res
}

// ---------------------------------------------------------------------------
// 管理端：训练池维护
// ---------------------------------------------------------------------------

export async function getAdminTrainingChallengeListApi(
  params?: AdminTrainingChallengeQuery
): Promise<ApiResponse<PageResponse<TrainingChallengeVO>>> {
  const res = await get<PageResponse<TrainingChallengeVO>>('/admin/training/challenges', { params })
  if (res?.data) {
    res.data = normalizeChallengePage(res.data)
  }
  return res
}

export function importAdminTrainingChallengesApi(
  challengeIds: (number | string)[]
): Promise<ApiResponse<TrainingImportResultVO>> {
  return post<TrainingImportResultVO>('/admin/training/challenges/import', { challengeIds })
}

export function updateAdminTrainingChallengeApi(
  id: number | string,
  data: TrainingChallengeUpdateDTO
): Promise<ApiResponse<void>> {
  return patch<void>(`/admin/training/challenges/${id}`, data)
}

export function deleteAdminTrainingChallengeApi(
  id: number | string
): Promise<ApiResponse<void>> {
  return del<void>(`/admin/training/challenges/${id}`)
}
