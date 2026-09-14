/**
 * 题目模块类型定义 (对应 com.humolix.volur.challenge)
 */

/** 题目分类 VO */
export interface CtfCategoryVO {
  id: number
  name: string
  code: string
  description?: string
  sortOrder: number
}

/** 题目通用标签 VO */
export interface TagVO {
  id: number
  name: string
  color?: string
}

/** CTF 题目列表 VO (对应 com.humolix.volur.challenge.domain.vo.stage.ctf.CtfChallengeListVO) */
export interface CtfChallengeListVO {
  id: number | string
  name: string
  title?: string
  categoryId?: number | string
  categoryName?: string
  category?: string
  type?: string
  difficulty?: number | string
  points?: number
  solvedCount?: number
  authorName?: string
  tags?: TagVO[]
  isSolved?: boolean
  createdAt?: string
}

/** CTF 题目详情 VO */
export interface CtfChallengeDetailVO {
  id: number | string
  title: string
  category: string
  difficulty: string
  points: number
  description: string
  attachmentUrl?: string
  hints?: string[]
  isDynamicContainer: boolean
  solvedCount: number
  tags: TagVO[]
  isSolved?: boolean
}

/** AWD 靶机赛题列表 VO */
export interface AwdChallengeListVO {
  id: number | string
  title: string
  category: string
  points: number
  description?: string
}

/** 理论赛题列表 VO */
export interface TheoryChallengeListVO {
  id: number | string
  title: string
  type: 'SINGLE' | 'MULTI' | 'JUDGE'
  points: number
  category: string
}
