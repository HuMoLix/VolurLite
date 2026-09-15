/**
 * 竞赛模块领域类型定义
 * 1:1 对齐后端 com.humolix.volur.competition.domain
 */

/** 竞赛类型 (一赛一制) */
export type CompetitionType = 'CTF' | 'THEORY' | 'AWD' | 'AWD_PLUS' | 'ISW'

/** 竞赛状态 */
export type CompetitionStatus =
  | 'DRAFT'
  | 'PUBLISHED'
  | 'REGISTRATION'
  | 'RUNNING'
  | 'FROZEN'
  | 'ENDED'
  | 'ARCHIVED'

/** 队伍审核状态 */
export type TeamStatus =
  | 'PENDING_AUDIT'
  | 'AUDIT_DRAFT'
  | 'AUDIT_PASSED'
  | 'AUDIT_REJECTED'
  | 'DISBANDED'

/** 战队成员角色 */
export type MemberRole = 'CAPTAIN' | 'MEMBER' | 'LEADER'

/** 战队成员加入/邀请状态 */
export type MemberStatus = 'INVITED' | 'INVITED_ACCEPTED' | 'NOT_PARTICIPATE'

/** 竞赛配置 DTO / VO */
export interface CompetitionConfigDTO {
  maxLeaders?: number
  maxTeamMembers?: number
  minTeamMembers?: number
  maxTeams?: number
  requireTeamAudit?: boolean
  freezeScoreboardMinutes?: number
  isScoreboardFrozen?: boolean
  allowWriteupSubmission?: boolean
  maxContainersPerTeam?: number
  writeupTemplateUrl?: string
  writeupTemplateName?: string
  writeupStartTime?: string
  writeupEndTime?: string
}

export interface CompetitionConfigVO {
  maxLeaders?: number
  maxTeamMembers: number
  minTeamMembers: number
  maxTeams: number
  requireTeamAudit: boolean
  freezeScoreboardMinutes: number
  isScoreboardFrozen: boolean
  allowWriteupSubmission: boolean
  maxContainersPerTeam?: number
  writeupTemplateUrl?: string
  writeupTemplateName?: string
  writeupStartTime?: string
  writeupEndTime?: string
}

/** 竞赛概要信息 VO (公开列表) */
export interface CompetitionSummaryVO {
  id: number | string
  title: string
  slug: string
  type: CompetitionType
  description?: string
  bannerUrl?: string
  status: CompetitionStatus
  regStartTime?: string
  regEndTime?: string
  startTime: string
  endTime: string
  createdAt?: string
}

/** 竞赛详情 VO (公开比赛主页) */
export interface CompetitionDetailVO {
  id: number | string
  title: string
  slug: string
  type: CompetitionType
  description?: string
  bannerUrl?: string
  status: CompetitionStatus
  regStartTime?: string
  regEndTime?: string
  startTime: string
  endTime: string
  maxLeaders?: number
  maxTeamMembers?: number
  minTeamMembers?: number
  requireTeamAudit?: boolean
  allowWriteupSubmission?: boolean
  teamCount?: number
  createdAt?: string
  config?: CompetitionConfigVO
}

/** 竞赛关于与规则 VO */
export interface CompetitionAboutVO {
  competitionId: number | string
  title: string
  type: CompetitionType
  description?: string
  rules?: string
  startTime: string
  endTime: string
  maxLeaders?: number
  maxTeamMembers?: number
  minTeamMembers?: number
  maxTeams?: number
  requireTeamAudit?: boolean
}

/** 管理端竞赛列表项 VO */
export interface AdminCompetitionListVO {
  id: number | string
  title: string
  slug: string
  type: CompetitionType
  status: CompetitionStatus
  startTime: string
  endTime: string
  teamCount: number
  participantCount: number
  createdAt: string
}

/** 管理端竞赛详情项 VO */
export interface AdminCompetitionDetailVO {
  id: number | string
  title: string
  slug: string
  type: CompetitionType
  description?: string
  bannerUrl?: string
  status: CompetitionStatus
  regStartTime?: string
  regEndTime?: string
  startTime: string
  endTime: string
  maxLeaders?: number
  maxTeamMembers?: number
  minTeamMembers?: number
  maxTeams?: number
  requireTeamAudit?: boolean
  freezeScoreboardMinutes?: number
  isScoreboardFrozen?: boolean
  allowWriteupSubmission?: boolean
  maxContainersPerTeam?: number
  writeupTemplateUrl?: string
  writeupTemplateName?: string
  writeupStartTime?: string
  writeupEndTime?: string
  config?: CompetitionConfigVO
  createdAt: string
  updatedAt: string
}

/** 创建竞赛入参 DTO */
export interface CompetitionCreateRequestDTO {
  title: string
  slug: string
  type: CompetitionType
  description?: string
  bannerUrl?: string
  status?: CompetitionStatus
  regStartTime?: string
  regEndTime?: string
  startTime: string
  endTime: string
  config?: CompetitionConfigDTO
}

/** 更新竞赛入参 DTO */
export interface CompetitionUpdateRequestDTO {
  title?: string
  slug?: string
  type?: CompetitionType
  description?: string
  bannerUrl?: string
  status?: CompetitionStatus
  regStartTime?: string
  regEndTime?: string
  startTime?: string
  endTime?: string
  config?: CompetitionConfigDTO
}

/** 参赛成员填报信息项 DTO */
export interface TeamMemberSignupDTO {
  realName: string
  gender: 'MALE' | 'FEMALE'
  role: MemberRole
  idCard: string
  email: string
  phone: string
  organization?: string
  department?: string
  position?: string
}

/** 队伍报名创建入参 DTO */
export interface TeamSignupRequestDTO {
  teamName: string
  teamSlogan?: string
  avatarUrl?: string
  members: TeamMemberSignupDTO[]
}

/** 更新队伍信息入参 DTO */
export interface TeamUpdateRequestDTO {
  teamName?: string
  teamSlogan?: string
  avatarUrl?: string
  members?: TeamMemberSignupDTO[]
}

/** 接受邀请 DTO */
export interface TeamInviteAcceptDTO {
  customFields?: Record<string, string>
}

/** 队伍邀请 VO */
export interface TeamInvitationVO {
  id: number | string
  competitionId: number | string
  competitionTitle?: string
  teamId: number | string
  teamName: string
  captainId: number | string
  captainName?: string
  role: MemberRole
  status: string
  invitedAt?: string
  realName?: string
  email?: string
  phone?: string
}

/** 战队成员 VO */
export interface TeamMemberInfoVO {
  id: number | string
  userId: number | string
  username: string
  nickname?: string
  avatarUrl?: string
  role: MemberRole
  joinedAt?: string
  status?: string
  customFields?: Record<string, string>
}

/** 我的战队详情 VO */
export interface MyTeamInfoVO {
  teamId: number | string
  competitionId?: number | string
  competitionTitle?: string
  teamName: string
  inviteCode: string
  teamSlogan?: string
  motto?: string
  avatarUrl?: string
  status: TeamStatus
  auditReason?: string
  myRole?: MemberRole
  isCaptain?: boolean
  members: CompetitionMemberVO[]
  myRegistrationData?: Record<string, string>
}

/** 参赛队伍摘要 VO */
export interface CompetitionTeamVO {
  teamId: number | string
  teamName: string
  motto?: string
  avatarUrl?: string
  status: TeamStatus
  captainUserId: number | string
  captainNickname: string
  memberCount: number
  createdAt: string
}

/** 管理端参赛队员信息 VO */
export interface CompetitionMemberVO {
  id: number | string
  userId: number | string
  username: string
  avatarUrl?: string
  realName?: string
  gender?: 'UNKNOWN' | 'MALE' | 'FEMALE'
  idCard?: string
  organization?: string
  org?: string
  department?: string
  position?: string
  phone?: string
  email?: string
  role: MemberRole
  status?: MemberStatus | string
  joinedAt?: string
}

/** 管理端参赛队伍列表项 VO */
export interface AdminTeamListVO {
  id: number | string
  competitionId: number | string
  teamName: string
  captainId: number | string
  captainUsername: string
  memberCount: number
  status: TeamStatus
  auditReason?: string
  createdAt: string
}

/** 管理端参赛队伍详情 VO */
export interface AdminTeamDetailVO {
  id: number | string
  competitionId: number | string
  competitionTitle?: string
  teamName: string
  teamSlogan?: string
  avatarUrl?: string
  inviteCode?: string
  captainId: number | string
  captainUsername: string
  status: TeamStatus
  auditReason?: string
  members: CompetitionMemberVO[]
  createdAt: string
}

/** 审核队伍 DTO */
export interface TeamAuditRequestDTO {
  status: TeamStatus
  auditReason?: string
}

/** 挂载 CTF 赛题 DTO */
export interface CtfChallengeMountRequestDTO {
  challengeId: number | string
  initialPoints?: number
  minPoints?: number
  decayCount?: number
  sortOrder?: number
  isVisible?: boolean
}

/** 批量挂载 CTF 赛题 DTO */
export interface CtfChallengeBatchMountRequestDTO {
  items: CtfChallengeMountRequestDTO[]
}

/** 竞赛 CTF 题目概要 VO */
export interface CtfCompetitionChallengeVO {
  id: number | string
  challengeId: number | string
  name?: string
  title?: string
  categoryId?: number | string
  categoryName?: string
  category?: string
  type?: string
  difficulty?: number | string
  initialPoints?: number
  currentPoints: number
  solvesCount: number
  sortOrder: number
  isVisible?: boolean
  isSolvedByMyTeam?: boolean
}

/** 竞赛 CTF 题目详情 VO */
export interface CtfCompetitionChallengeDetailVO {
  id: number | string
  challengeId: number | string
  title: string
  category: string
  difficulty: string
  description?: string
  attachmentUrl?: string
  currentPoints: number
  initialPoints: number
  minPoints: number
  decayCount: number
  solvesCount: number
  isContainer?: boolean
  isSolvedByMyTeam?: boolean
}

/** CTF 赛题提交 Flag DTO */
export interface CtfFlagSubmitRequestDTO {
  flag: string
}

/** 计分榜阶段得分详情 VO */
export interface StageScoreDetailVO {
  stage: CompetitionType
  stageName: string
  rawScore: number
  weightPercent: number
  weightedScore: number
}

/** 综合天梯榜队伍得分 VO */
export interface CompositeTeamScoreVO {
  rank: number
  teamId: number | string
  teamName: string
  avatarUrl?: string
  totalFinalScore: number
  stageScores: StageScoreDetailVO[]
}

/** 竞赛综合排行榜 VO */
export interface CompositeScoreboardVO {
  competitionId: number | string
  competitionTitle: string
  isFrozen: boolean
  rankings: CompositeTeamScoreVO[]
}

/** CTF 专项解题排行榜单项 VO */
export interface CtfTeamRankVO {
  rank: number
  teamId: number | string
  teamName: string
  avatarUrl?: string
  score: number
  solvesCount: number
  lastSolveTime?: string
}

/** CTF 专项解题排行榜 VO */
export interface CtfScoreboardVO {
  competitionId: number | string
  isFrozen: boolean
  rankings: CtfTeamRankVO[]
}
export type CtfStageScoreboardVO = CtfScoreboardVO

/** 比赛内部专属公告 DTO & VO */
export interface CompetitionAnnouncementDTO {
  title: string
  content: string
  level?: string
  isPinned?: boolean
  challengeId?: number | string
  challengeName?: string
}

export interface CompetitionAnnouncementVO {
  id: number | string
  competitionId: number | string
  title: string
  content: string
  level: string
  isPinned: boolean
  challengeId?: number | string
  challengeName?: string
  createdById?: number | string
  createdByName?: string
  createdAt: string
  updatedAt?: string
}

/** 比赛题解 WP VO & DTO */
export interface CompetitionWriteupVO {
  id: number | string
  competitionId: number | string
  teamId: number | string
  teamName: string
  userId: number | string
  username: string
  fileName: string
  fileUrl: string
  fileSize: number
  fileType: string
  status: string
  score?: number
  comment?: string
  createdAt: string
  updatedAt?: string
}

export interface CompetitionWriteupAuditDTO {
  status?: string
  score?: number
  comment?: string
}

