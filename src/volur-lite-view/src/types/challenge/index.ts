/**
 * 题目模块类型定义 (对应 com.humolix.volur.challenge)
 */

export type ChallengeType = 'STATIC' | 'DYNAMIC' | 'CONTAINER' | 'DYNAMIC_CONTAINER'

/** 赛题难度等级 (对应 com.humolix.volur.challenge.domain.enums.ChallengeDifficulty) */
export type ChallengeDifficulty = 'easy' | 'normal' | 'mid' | 'hard' | 'difficult'

export type TheoryType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'ESSAY'

/** 题目分类 VO (对应 com.humolix.volur.challenge.domain.vo.ctf.CtfCategoryVO) */
export interface CtfCategoryVO {
  id: number | string
  name: string
  icon?: string
  description?: string
  sortOrder: number
}

/** 分类创建 DTO */
export interface CtfCategoryCreateRequestDTO {
  name: string
  icon?: string
  description?: string
  sortOrder?: number
}

/** 分类更新 DTO */
export interface CtfCategoryUpdateRequestDTO {
  name?: string
  icon?: string
  description?: string
  sortOrder?: number
}

/** 题目通用标签 VO */
export interface TagVO {
  id: number | string
  name: string
  color?: string
}

/** CTF 题目列表 VO (对应 com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeListVO) */
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

/** CTF 赛题附件 VO (对应 com.humolix.volur.challenge.domain.vo.ctf.CtfAttachmentVO) */
export interface CtfAttachmentVO {
  name: string
  url: string
  size?: number
}
export type CtfAttachmentDTO = CtfAttachmentVO

/** CTF 题目详情 VO (对应 com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeDetailVO) */
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

/** CTF 题目创建 DTO */
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

/** CTF 题目更新 DTO */
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

/** 理论题目列表 VO (对应 com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeListVO) */
export interface TheoryChallengeListVO {
  id: number | string
  name: string
  category?: string
  type: TheoryType
  score?: number
  difficulty?: number
  authorName?: string
  createdAt?: string
}

/** 理论题目详情 VO (对应 com.humolix.volur.challenge.domain.vo.theory.TheoryChallengeDetailVO) */
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
  authorId?: number | string
  authorName?: string
  createdAt?: string
  updatedAt?: string
}

/** 理论题目创建 DTO */
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
}

/** 理论题目更新 DTO */
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
}

/** 题目导入结果 VO (对应 com.humolix.volur.challenge.domain.vo.ChallengeImportResultVO) */
export interface ChallengeImportResultVO {
  totalCount: number
  successCount: number
  failedCount: number
  errors: string[]
  importedChallengeIds: (number | string)[]
  triggeredImagePullTasks: string[]
}

/** AWD 靶机赛题列表 VO */
export interface AwdChallengeListVO {
  id: number | string
  title: string
  category: string
  points: number
  description?: string
}
