export type TrainingDifficulty = 'easy' | 'normal' | 'mid' | 'hard' | 'difficult'

export interface TrainingChallengeVO {
  id: number | string
  /** 题库原题 ID：靶机启停接口使用它 */
  sourceChallengeId: number | string
  name: string
  categoryId?: number | string | null
  categoryName?: string | null
  difficulty?: TrainingDifficulty | string | null
  points: number
  solvedCount: number
  solvedByMe: boolean
  hasAttachment: boolean
  /** 原题是否配置了靶机镜像 */
  dynamicContainer: boolean
  status?: string
}

export interface TrainingAttachmentVO {
  name: string
  url: string
}

export interface TrainingChallengeDetailVO {
  id: number | string
  sourceChallengeId: number | string
  name: string
  categoryId?: number | string | null
  categoryName?: string | null
  difficulty?: TrainingDifficulty | string | null
  points: number
  solvedCount: number
  solvedByMe: boolean
  solvedAt?: string | null
  description?: string | null
  attachments?: TrainingAttachmentVO[] | null
  dynamicContainer: boolean
}

export interface TrainingCategoryProgressVO {
  category: string
  total: number
  solved: number
}

export interface TrainingCategoryVO {
  /** 题库分类 ID；未归类题目为 null */
  id: number | string | null
  name: string
  total: number
  solved: number
}

export interface TrainingProgressVO {
  totalChallenges: number
  solvedCount: number
  totalPoints: number
  rank: number
  categories: TrainingCategoryProgressVO[]
}

export interface TrainingLeaderboardItemVO {
  rank: number
  userId: number | string
  username: string
  avatar?: string | null
  points: number
  solvedCount: number
}

export interface TrainingMyRankVO {
  rank: number
  points: number
  solvedCount: number
}

export interface TrainingLeaderboardVO {
  items: TrainingLeaderboardItemVO[]
  myRank: TrainingMyRankVO | null
}

export type TrainingFlagResult = 'CORRECT' | 'DUPLICATE' | 'WRONG'

export interface TrainingFlagResultVO {
  result: TrainingFlagResult
  pointsEarned: number
  totalPoints: number
}

export interface TrainingChallengeQuery {
  page?: number
  size?: number
  categoryId?: number | string
  difficulty?: TrainingDifficulty | string
  keyword?: string
}

export type TrainingChallengeStatus = 'ENABLED' | 'DISABLED'

export interface AdminTrainingChallengeQuery {
  page?: number
  size?: number
  keyword?: string
  status?: TrainingChallengeStatus
}

export interface TrainingImportSkippedVO {
  challengeId: number | string
  reason: 'ALREADY_IN_POOL' | 'NOT_FOUND'
}

export interface TrainingImportResultVO {
  imported: number
  skipped: TrainingImportSkippedVO[]
}

export interface TrainingChallengeUpdateDTO {
  points?: number
  status?: TrainingChallengeStatus
}
