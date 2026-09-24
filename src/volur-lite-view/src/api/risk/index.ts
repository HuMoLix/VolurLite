import { request } from '@/utils/request'
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

/**
 * 获取风控全局基础设置
 * 对应后端: GET /api/v1/admin/risk/settings
 */
export function getRiskSettingsApi(): Promise<ApiResponse<AdminRiskSettingsVO>> {
  return request.get<AdminRiskSettingsVO>('/admin/risk/settings')
}

/**
 * 更新风控全局基础设置
 * 对应后端: PUT /api/v1/admin/risk/settings
 */
export function updateRiskSettingsApi(data: AdminRiskSettingsDTO): Promise<ApiResponse<void>> {
  return request.put<void>('/admin/risk/settings', data)
}

/**
 * 获取风控策略与防御阈值配置
 * 对应后端: GET /api/v1/admin/risk/policy
 */
export function getRiskPolicyApi(): Promise<ApiResponse<AdminRiskPolicyVO>> {
  return request.get<AdminRiskPolicyVO>('/admin/risk/policy')
}

/**
 * 更新风控策略与防御阈值配置
 * 对应后端: PUT /api/v1/admin/risk/policy
 */
export function updateRiskPolicyApi(data: AdminRiskPolicyDTO): Promise<ApiResponse<void>> {
  return request.put<void>('/admin/risk/policy', data)
}

/**
 * 分页查询 IP 规则列表
 * 对应后端: GET /api/v1/admin/risk/ip-rules
 */
export function listIpRulesApi(
  params?: AdminIpRuleQueryDTO
): Promise<ApiResponse<PageResponse<AdminIpRuleVO>>> {
  return request.get<PageResponse<AdminIpRuleVO>>('/admin/risk/ip-rules', { params })
}

/**
 * 手动添加 IP 规则 (封禁或白名单)
 * 对应后端: POST /api/v1/admin/risk/ip-rules
 */
export function createIpRuleApi(data: AdminIpRuleCreateDTO): Promise<ApiResponse<void>> {
  return request.post<void>('/admin/risk/ip-rules', data)
}

/**
 * 撤销/解封指定 IP 规则
 * 对应后端: POST /api/v1/admin/risk/ip-rules/{id}/revoke
 */
export function revokeIpRuleApi(id: number | string): Promise<ApiResponse<void>> {
  return request.post<void>(`/admin/risk/ip-rules/${id}/revoke`)
}

/**
 * 删除指定 IP 规则
 * 对应后端: DELETE /api/v1/admin/risk/ip-rules/{id}
 */
export function deleteIpRuleApi(id: number | string): Promise<ApiResponse<void>> {
  return request.delete<void>(`/admin/risk/ip-rules/${id}`)
}

/**
 * IP 实时风险探针诊断
 * 对应后端: GET /api/v1/admin/risk/ip-rules/diagnose
 */
export function diagnoseIpApi(ip: string): Promise<ApiResponse<AdminIpDiagnosisVO>> {
  return request.get<AdminIpDiagnosisVO>('/admin/risk/ip-rules/diagnose', { params: { ip } })
}

/**
 * 获取风控与 IP 治理统计指标
 * 对应后端: GET /api/v1/admin/risk/stats
 */
export function getRiskStatisticsApi(): Promise<ApiResponse<AdminRiskStatisticsVO>> {
  return request.get<AdminRiskStatisticsVO>('/admin/risk/stats')
}
