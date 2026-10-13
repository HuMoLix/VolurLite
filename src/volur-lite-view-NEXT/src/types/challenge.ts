export type ChallengeType = 'STATIC' | 'DYNAMIC' | 'CONTAINER'

export type ChallengeDifficulty = 'easy' | 'normal' | 'mid' | 'hard' | 'difficult'

export type TheoryType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'ESSAY'

export interface CtfCategoryVO {
  id: number | string
  name: string
  icon?: string
  description?: string
  sortOrder: number
}

export interface CtfCategoryCreateRequestDTO {
  name: string
  icon?: string
  description?: string
  sortOrder?: number
}

export interface CtfCategoryUpdateRequestDTO {
  name?: string
  icon?: string
  description?: string
  sortOrder?: number
}

export interface TagVO {
  id: number | string
  name: string
  color?: string
}

export interface CtfChallengeListVO {
  id: number | string
  name: string
  categoryId?: number | string
  categoryName?: string
  categoryIcon?: string
  authorId?: number | string
  authorName?: string
  type?: ChallengeType
  difficulty?: ChallengeDifficulty | number
  flag?: string
  attachmentUrl?: string
  attachmentName?: string
  imageName?: string
  containerPort?: number
  createdAt?: string
}

export interface CtfAttachmentVO {
  name: string
  url: string
  size?: number
  uploader?: string
  uploadedAt?: string
  rawFile?: File
}

export type CtfAttachmentDTO = CtfAttachmentVO

export interface CtfChallengeDetailVO {
  id: number | string
  name: string
  description?: string
  categoryId: number | string
  categoryName?: string
  categoryIcon?: string
  authorId?: number | string
  authorName?: string
  type: ChallengeType
  difficulty?: ChallengeDifficulty | number
  flag?: string
  attachmentUrl?: string
  attachmentName?: string
  attachments?: CtfAttachmentVO[]
  imageName?: string
  containerPort?: number
  cpuLimit?: number
  memoryLimit?: number
  createdAt?: string
  updatedAt?: string
}

export interface CtfChallengeCreateRequestDTO {
  name: string
  description?: string
  categoryId: number | string
  type: ChallengeType
  difficulty?: ChallengeDifficulty | number
  flag?: string
  attachmentUrl?: string
  attachmentName?: string
  attachments?: CtfAttachmentDTO[]
  imageName?: string
  containerPort?: number
  cpuLimit?: number
  memoryLimit?: number
}

export interface CtfChallengeUpdateRequestDTO {
  name?: string
  description?: string
  categoryId?: number | string
  type?: ChallengeType
  difficulty?: ChallengeDifficulty | number
  flag?: string
  attachmentUrl?: string
  attachmentName?: string
  attachments?: CtfAttachmentDTO[]
  imageName?: string
  containerPort?: number
  cpuLimit?: number
  memoryLimit?: number
}

export interface TheoryBankListVO {
  id: number | string
  name: string
  description?: string
  category?: string
  questionCount?: number
  authorName?: string
  createdAt?: string
  updatedAt?: string
}

export interface TheoryBankDetailVO {
  id: number | string
  name: string
  description?: string
  category?: string
  questionCount?: number
  authorId?: number | string
  authorName?: string
  createdAt?: string
  updatedAt?: string
}

export interface TheoryBankCreateRequestDTO {
  name: string
  description?: string
  category?: string
}

export interface TheoryBankUpdateRequestDTO {
  name?: string
  description?: string
  category?: string
}

export interface TheoryChallengeListVO {
  id: number | string
  name: string
  category?: string
  type: TheoryType
  score?: number
  difficulty?: number
  bankId?: number | string
  bankName?: string
  authorName?: string
  createdAt?: string
}

export interface TheoryChallengeDetailVO {
  id: number | string
  name: string
  category?: string
  type: TheoryType
  content: string
  options?: string
  standardAnswer: string
  explanation?: string
  score?: number
  difficulty?: number
  bankId?: number | string
  bankName?: string
  authorId?: number | string
  authorName?: string
  createdAt?: string
  updatedAt?: string
}

export interface TheoryChallengeCreateRequestDTO {
  name: string
  category?: string
  type: TheoryType
  content: string
  options?: string
  standardAnswer: string
  explanation?: string
  score?: number
  difficulty?: number
  bankId?: number | string
}

export interface TheoryChallengeUpdateRequestDTO {
  name?: string
  category?: string
  type?: TheoryType
  content?: string
  options?: string
  standardAnswer?: string
  explanation?: string
  score?: number
  difficulty?: number
  bankId?: number | string
}

export interface ChallengeImportResultVO {
  totalCount: number
  successCount: number
  failedCount: number
  errors: string[]
  importedChallengeIds: (number | string)[]
  triggeredImagePullTasks: string[]
}

export interface AwdChallengeListVO {
  id: number | string
  title: string
  category: string
  points: number
  description?: string
}
