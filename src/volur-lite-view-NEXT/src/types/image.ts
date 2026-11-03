export interface AdminClusterImageVO {
  id: string | number
  repository: string
  tag: string
  fullImageName: string
  imageId?: string
  sizeBytes: number
  inRegistry?: boolean
  createdAt: string
  updatedAt?: string
}

export interface DockerImageLayerVO {
  layerIndex: number
  layerId: string
  instruction: string
  createdBy: string
  sizeBytes: number
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
  inRegistry?: boolean
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
  totalSizeBytes: number
  registryOnline?: boolean
  registryImagesCount?: number
}

export interface AdminImagePageRequestDTO {
  page?: number
  size?: number
  sortField?: string
  sortOrder?: string
  keyword?: string
}

export interface AdminImagePullRequestDTO {
  image: string
  authServer?: string
  username?: string
  password?: string
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
