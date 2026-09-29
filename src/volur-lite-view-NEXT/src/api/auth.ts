import { get, post } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type {
  FlowInitVO,
  FlowInitParams,
  SignInFlowResponseVO,
  SignInIdentifierRequestDTO,
  SignInChallengeRequestDTO
} from '@/types/auth/flow'

export function initFlowSessApi(params: FlowInitParams): Promise<ApiResponse<FlowInitVO>> {
  return get<FlowInitVO>('/flow/initSess', { params })
}

export function submitSignInIdentifierApi(
  data: SignInIdentifierRequestDTO
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/auth/signin/identifier', data)
}

export function submitSignInChallengeApi(
  data: SignInChallengeRequestDTO
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/auth/signin/challenge', data)
}
