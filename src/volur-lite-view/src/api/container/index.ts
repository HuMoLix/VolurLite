import { request } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type { ChallengeInstanceResponseVO } from '@/types/container'

/**
 * 启动靶机容器
 * 对应后端: POST /api/v1/challenges/instances/{challengeId}/start
 */
export function startInstanceApi(challengeId: number | string): Promise<ApiResponse<ChallengeInstanceResponseVO>> {
  return request.post<ChallengeInstanceResponseVO>(`/challenges/instances/${challengeId}/start`)
}

/**
 * 销毁/停止靶机容器
 * 对应后端: POST /api/v1/challenges/instances/{challengeId}/stop
 */
export function stopInstanceApi(challengeId: number | string): Promise<ApiResponse<void>> {
  return request.post<void>(`/challenges/instances/${challengeId}/stop`)
}

/**
 * 延长靶机容器租期
 * 对应后端: POST /api/v1/challenges/instances/{challengeId}/extend
 */
export function extendInstanceApi(challengeId: number | string): Promise<ApiResponse<ChallengeInstanceResponseVO>> {
  return request.post<ChallengeInstanceResponseVO>(`/challenges/instances/${challengeId}/extend`)
}
