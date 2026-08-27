/**
 * 竞赛模块类型定义 (对应 com.humolix.volur.competition)
 */

/** 竞赛摘要列表 VO */
export interface CompetitionSummaryVO {
  id: number | string
  title: string
  description: string
  bannerUrl?: string
  status: 'PENDING' | 'RUNNING' | 'ENDED'
  startTime: string
  endTime: string
  teamCount: number
  participantCount: number
}

/** 竞赛详情 VO */
export interface CompetitionDetailVO {
  id: number | string
  title: string
  description: string
  bannerUrl?: string
  rules?: string
  status: 'PENDING' | 'RUNNING' | 'ENDED'
  startTime: string
  endTime: string
  allowTeamRegistration: boolean
  teamSizeLimit: number
  stages: CompetitionStageVO[]
}

/** 竞赛赛段 VO */
export interface CompetitionStageVO {
  id: number | string
  competitionId: number | string
  stageType: 'CTF' | 'AWD' | 'AWD_PLUS' | 'ISW' | 'THEORY'
  stageName: string
  weight: number
  startTime: string
  endTime: string
  status: 'PENDING' | 'RUNNING' | 'ENDED'
}

/** 战队详情 VO */
export interface CompetitionTeamVO {
  id: number | string
  competitionId: number | string
  name: string
  motto?: string
  logoUrl?: string
  captainId: number | string
  captainName: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  members: CompetitionMemberVO[]
}

/** 战队成员 VO */
export interface CompetitionMemberVO {
  userId: number | string
  username: string
  nickname: string
  avatar?: string
  isCaptain: boolean
  joinedAt: string
}

/** Flag 提交 DTO */
export interface FlagSubmitDTO {
  competitionId: number | string
  challengeId: number | string
  flag: string
}

/** 战队创建 DTO */
export interface TeamCreateDTO {
  competitionId: number | string
  name: string
  motto?: string
  logoUrl?: string
}

/** 战队邀请 DTO */
export interface TeamInviteRequestDTO {
  competitionId: number | string
  teamId: number | string
  inviteeUserId: number | string
}

/** 综合排行榜 VO */
export interface CompositeScoreboardVO {
  competitionId: number | string
  updatedAt: string
  rankings: CompositeTeamScoreVO[]
}

/** 综合战队得分 VO */
export interface CompositeTeamScoreVO {
  rank: number
  teamId: number | string
  teamName: string
  totalScore: number
  stageScores: Record<string, number>
}
