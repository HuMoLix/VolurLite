export interface ApiResponse<T = any> {
  status: number
  errorCode: number
  success: boolean
  message: string
  data: T
  timestamp: number
  path: string
}

export interface PageResponse<T> {
  list: T[]
  total: number
  totalPages: number
  currentPage: number
  records?: T[]
  content?: T[]
  totalElements?: number
}

export interface CursorResponse<T> {
  list: T[]
  nextPageToken: string | null
  hasMore: boolean
}

export interface SliceResponse<T> {
  list: T[]
  hasNext: boolean
  hasPrevious: boolean
  currentPage: number
  pageSize: number
}

export interface PageRequestDTO {
  page?: number
  size?: number
  sortField?: string
  sortOrder?: 'ASC' | 'DESC' | 'asc' | 'desc'
  sortBy?: string
}

export interface CursorQuery {
  pageToken?: string
  size?: number
}
