/**
 * 后端通用 API 响应包装模型 (对应 com.humolix.volur.framework.common.api.ApiResponse)
 */
export interface ApiResponse<T = any> {
  status: number
  errorCode: number
  success: boolean
  message: string
  data: T
  timestamp: number
  path: string
}

/**
 * 标准分页响应模型 (对应 com.humolix.volur.framework.common.api.PageResponse)
 */
export interface PageResponse<T> {
  list: T[]
  total: number
  totalPages: number
  currentPage: number
}

/**
 * 游标/流式无限滚动分页响应模型 (对应 com.humolix.volur.framework.common.api.CursorResponse)
 */
export interface CursorResponse<T> {
  list: T[]
  nextPageToken: string | null
  hasMore: boolean
}

/**
 * 无总数切片分页响应模型 (对应 com.humolix.volur.framework.common.api.SliceResponse)
 */
export interface SliceResponse<T> {
  list: T[]
  hasNext: boolean
  hasPrevious: boolean
  currentPage: number
  pageSize: number
}

/**
 * 基础分页查询请求入参 DTO (对应 com.humolix.volur.framework.common.dto.PageRequestDTO)
 */
export interface PageRequestDTO {
  page?: number
  size?: number
  sortField?: string
  sortOrder?: 'ASC' | 'DESC' | 'asc' | 'desc'
  sortBy?: string
}

export type PageQuery = PageRequestDTO

/**
 * 游标分页查询请求入参 DTO
 */
export interface CursorQuery {
  pageToken?: string
  size?: number
}
