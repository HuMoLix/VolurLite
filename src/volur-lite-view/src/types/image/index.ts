export interface AdminClusterImageVO {
  id: string | number
  repository: string
  tag: string
  fullImageName: string
  imageId?: string
  sizeBytes: number
  sizeFormatted: string
  imageType: 'CHALLENGE' | 'BASE' | 'SYSTEM' | string
  nodeIps: string[]
  onlineNodeIps?: string[]
  offlineNodeIps?: string[]
  isOffline?: boolean
  status: 'PULLED' | 'PULLING' | 'FAILED' | 'MIGRATING' | 'OFFLINE' | string
  createdAt: string
  updatedAt?: string
}

export interface DockerImageLayerVO {
  layerIndex: number
  layerId: string
  instruction: string
  createdBy: string
  sizeBytes: number
  sizeFormatted: string
  createdAt: string
  comment?: string
}

export interface AdminClusterImageDetailVO {
  id: string | number
  repository: string
  tag: string
  fullImageName: string
  imageId?: string
  sizeBytes: number
  sizeFormatted: string
  imageType: string
  nodeIps: string[]
  onlineNodeIps?: string[]
  offlineNodeIps?: string[]
  isOffline?: boolean
  status: string
  os?: string
  architecture?: string
  dockerVersion?: string
  author?: string
  workingDir?: string
  user?: string
  entrypoint?: string[]
  cmd?: string[]
  env?: string[]
  exposedPorts?: string[]
  labels?: Record<string, string>
  layers: DockerImageLayerVO[]
  createdAt: string
  updatedAt?: string
}

export interface AdminClusterImageStatisticsVO {
  totalImages: number
  challengeImages: number
  baseImages: number
  totalSizeBytes: number
  totalSizeFormatted: string
  totalNodesCovered: number
  syncingImages: number
}

export interface AdminImagePageRequestDTO {
  page?: number
  size?: number
  sortField?: string
  sortOrder?: string
  keyword?: string
  imageType?: string
  nodeIp?: string
}

export interface AdminImagePullRequestDTO {
  image: string
  targetNodeIp: string
  imageType?: string
}

export interface AdminImageTransferDTO {
  imageId: number | string
  sourceNodeIp: string
  targetNodeIp: string
}

export interface AdminImageBackupDTO {
  imageId: number | string
  backupType: 'NODE' | 'ARCHIVE'
  targetNodeIp?: string
}

export interface AdminImageTypeUpdateDTO {
  imageType: string
}
