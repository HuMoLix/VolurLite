import { get, post, put, del } from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/types/common/api'
import type {
  AdminRiskSettingsVO,
  AdminRiskSettingsDTO,
  AdminRiskPolicyVO,
  AdminRiskPolicyDTO,
  AdminIpRuleVO,
  AdminIpRuleCreateDTO,
  AdminIpRuleQueryDTO,
  AdminIpDiagnosisVO,
  AdminRiskStatisticsVO
} from '@/types/risk'

export function getRiskSettingsApi(): Promise<ApiResponse<AdminRiskSettingsVO>> {
  return get<AdminRiskSettingsVO>('/admin/risk/settings')
}

export function updateRiskSettingsApi(data: AdminRiskSettingsDTO): Promise<ApiResponse<void>> {
  return put<void>('/admin/risk/settings', data)
}

export function getRiskPolicyApi(): Promise<ApiResponse<AdminRiskPolicyVO>> {
  return get<AdminRiskPolicyVO>('/admin/risk/policy')
}

export function updateRiskPolicyApi(data: AdminRiskPolicyDTO): Promise<ApiResponse<void>> {
  return put<void>('/admin/risk/policy', data)
}

export function listIpRulesApi(
  params?: AdminIpRuleQueryDTO
): Promise<ApiResponse<PageResponse<AdminIpRuleVO>>> {
  return get<PageResponse<AdminIpRuleVO>>('/admin/risk/ip-rules', { params })
}

export function createIpRuleApi(data: AdminIpRuleCreateDTO): Promise<ApiResponse<void>> {
  return post<void>('/admin/risk/ip-rules', data)
}

export function revokeIpRuleApi(id: number | string): Promise<ApiResponse<void>> {
  return post<void>(`/admin/risk/ip-rules/${id}/revoke`)
}

export function deleteIpRuleApi(id: number | string): Promise<ApiResponse<void>> {
  return del<void>(`/admin/risk/ip-rules/${id}`)
}

export function diagnoseIpApi(ip: string): Promise<ApiResponse<AdminIpDiagnosisVO>> {
  return get<AdminIpDiagnosisVO>('/admin/risk/ip-rules/diagnose', { params: { ip } })
}

export function getRiskStatisticsApi(): Promise<ApiResponse<AdminRiskStatisticsVO>> {
  return get<AdminRiskStatisticsVO>('/admin/risk/stats')
}
