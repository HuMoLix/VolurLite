import { get, post } from '@/utils/request'
import type { ApiResponse } from '@/types/common/api'
import type {
  FlowInitVO,
  FlowInitParams,
  SignInFlowResponseVO,
  SignInIdentifierRequestDTO,
  SignInChallengeRequestDTO,
  ChallengeOptionVO
} from '@/types/auth/flow'

export function initFlowSessApi(params: FlowInitParams): Promise<ApiResponse<FlowInitVO>> {
  return get<FlowInitVO>('/flow/initSess', { params, skipToast: true })
}

export function submitSignInIdentifierApi(
  data: SignInIdentifierRequestDTO
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/auth/signin/identifier', data, { skipToast: true })
}

export function submitSignInChallengeApi(
  data: SignInChallengeRequestDTO
): Promise<ApiResponse<SignInFlowResponseVO>> {
  return post<SignInFlowResponseVO>('/auth/signin/challenge', data, { skipToast: true })
}

export function fetchChallengeSelectionApi(params?: {
  dsh?: string
  currentType?: string
}): Promise<ApiResponse<ChallengeOptionVO[]>> {
  return get<ChallengeOptionVO[]>('/signin/challenge/selection', { params, skipToast: true })
}
