export type CompetitionType = 'CTF' | 'THEORY' | 'AWD' | 'AWD_PLUS' | 'ISW'

export type CompetitionStatus =
  | 'DRAFT'
  | 'PUBLISHED'
  | 'RUNNING'
  | 'ENDED'
  | 'ARCHIVED'
  | 'REGISTRATION'
  | 'FROZEN'
  | 'HIDDEN'

export interface CompetitionSummaryVO {
  id: number | string
  title: string
  slug: string
  description?: string | null
  bannerUrl?: string | null
  type?: CompetitionType | null
  status?: CompetitionStatus | null
  regStartTime?: string | null
  regEndTime?: string | null
  startTime?: string | null
  endTime?: string | null
  createdAt?: string | null
}

export interface CompetitionDetailVO {
  id: number | string
  title: string
  slug: string
  description?: string | null
  bannerUrl?: string | null
  type?: CompetitionType | null
  status?: CompetitionStatus | null
  regStartTime?: string | null
  regEndTime?: string | null
  startTime?: string | null
  endTime?: string | null
  maxLeaders?: number | null
  maxTeamMembers?: number | null
  minTeamMembers?: number | null
  requireTeamAudit?: boolean | null
  allowWriteupSubmission?: boolean | null
  regRequirements?: string | null
  teamCount?: number | null
  createdAt?: string | null
}

export interface CompetitionAboutVO {
  id: number | string
  title: string
  slug: string
  description?: string | null
  bannerUrl?: string | null
  type?: CompetitionType | null
  status?: CompetitionStatus | null
  regStartTime?: string | null
  regEndTime?: string | null
  startTime?: string | null
  endTime?: string | null
  maxLeaders?: number | null
  maxTeamMembers?: number | null
  minTeamMembers?: number | null
  maxTeams?: number | null
  requireTeamAudit?: boolean | null
  regRequirements?: string | null
}

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
  level?: string | null
  isPinned?: boolean | null
  challengeId?: number | string | null
  challengeName?: string | null
  createdById?: number | string | null
  createdByName?: string | null
  createdAt: string
  updatedAt?: string | null
}

export interface CompetitionQueryDTO {
  page?: number
  size?: number
  keyword?: string
  type?: CompetitionType | string
}

export type TheoryType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'ESSAY'

export interface TheoryPaperQuestionVO {
  paperId: number | string
  challengeId: number | string
  name: string
  category: string
  type: TheoryType
  content: string
  options?: string | null
  score: number
  sortOrder: number
  myAnswer?: string | null
}

export interface TheoryPaperExamVO {
  paperId?: number | string
  competitionId: number | string
  totalQuestions: number
  totalScore: number
  isSubmitted: boolean
  myTotalScore?: number | null
  questions: TheoryPaperQuestionVO[]
}

export interface TheoryAnswerItemDTO {
  paperId: number | string
  answer?: string | null
}

export interface TheoryPaperSubmitRequestDTO {
  answers: TheoryAnswerItemDTO[]
}

export interface TheoryPaperResultVO {
  totalQuestions: number
  correctCount?: number | null
  totalScore?: number | null
  scoreObtained?: number | null
  accuracyRate?: number | null
}

export type TheoryCheatEventType =
  | 'WINDOW_BLUR'
  | 'VISIBILITY_HIDDEN'
  | 'EXIT_FULLSCREEN'
  | 'DEVTOOLS_OPEN'
  | 'STOP_SCREEN_SHARE'
  | 'RESTRICTED_KEY'
  | 'COPY_PASTE_ATTEMPT'
  | 'CONTEXTMENU_ATTEMPT'

export interface TheoryCheatReportDTO {
  eventType: TheoryCheatEventType | string
  detail?: string
  timestamp?: number
}

export interface TheoryCheatReportVO {
  violationCount: number
  maxAllowedViolations: number
  isBanned: boolean
  message: string
}

export interface TheoryProctorHeartbeatDTO {
  fullscreen?: boolean
  screenActive?: boolean
  screenFrame?: string
  timestamp?: number
  answeredCount?: number
  totalCount?: number
}

export interface TheoryProctorHeartbeatVO {
  acknowledged: boolean
  isBanned: boolean
  violationCount: number
  maxAllowedViolations: number
  serverTime: number
}

export interface AdminTheoryCandidateMonitorVO {
  userId: number | string
  username: string
  realName?: string | null
  teamName?: string | null
  isOnline: boolean
  fullscreen: boolean
  screenActive: boolean
  screenFrame?: string | null
  highResFrame?: string | null
  highResTimestamp?: number | null
  violationCount: number
  isBanned: boolean
  answeredCount: number
  totalCount: number
  lastHeartbeatTime: number
}

export interface TheoryProctorSnapshotDTO {
  screenFrame: string
  reason?: string
  timestamp?: number
}

export interface TheoryProctorRtcSignalDTO {
  targetUserId: number | string
  type: 'CALL' | 'OFFER' | 'ANSWER' | 'ICE' | 'HANGUP'
  sdp?: string
  candidate?: string
  sdpMid?: string
  sdpMLineIndex?: number
  sessionId?: string
}


export interface TheoryBankSummaryVO {
  id: number | string
  name: string
  questionCount?: number
}

export interface TheoryCompetitionConfigVO {
  competitionId: number | string
  bankIds: (number | string)[]
  banks: TheoryBankSummaryVO[]
  singleChoiceCount: number
  singleChoiceScore: number
  multipleChoiceCount: number
  multipleChoiceScore: number
  trueFalseCount: number
  trueFalseScore: number
  essayCount: number
  essayScore: number
  isRandom: boolean
  shuffleOptions?: boolean
  totalQuestions: number
  totalScore: number
}

export interface TheoryCompetitionConfigDTO {
  bankIds: (number | string)[]
  singleChoiceCount?: number
  singleChoiceScore?: number
  multipleChoiceCount?: number
  multipleChoiceScore?: number
  trueFalseCount?: number
  trueFalseScore?: number
  essayCount?: number
  essayScore?: number
  isRandom?: boolean
  shuffleOptions?: boolean
}

export interface TheoryCompetitionPaperGenerateRequestDTO {
  bankIds: (number | string)[]
  singleChoiceCount?: number
  singleChoiceScore?: number
  multipleChoiceCount?: number
  multipleChoiceScore?: number
  trueFalseCount?: number
  trueFalseScore?: number
  essayCount?: number
  essayScore?: number
  isRandom?: boolean
  selectedQuestionIds?: (number | string)[]
}

export interface TheoryPaperQuestionAdminVO {
  paperId: number | string
  challengeId: number | string
  name: string
  bankName?: string
  category?: string
  type: TheoryType
  score: number
  sortOrder: number
}

export type TeamStatus =
  | 'PENDING_AUDIT'
  | 'AUDIT_DRAFT'
  | 'AUDIT_PASSED'
  | 'AUDIT_REJECTED'
  | 'DISBANDED'
  | 'BANNED'
  | 'ENDED'

export type MemberRole = 'CAPTAIN' | 'MEMBER' | 'LEADER'

export type MemberStatus = 'INVITED' | 'INVITED_ACCEPTED' | 'NOT_PARTICIPATE'

export type OrganizationType =
  | 'SCHOOL'
  | 'ENTERPRISE'
  | 'MILITARY'
  | 'ENERGY'
  | 'FINANCE'
  | 'RESEARCH'
  | 'OTHER'

export interface CompetitionMemberVO {
  id: number | string
  userId: number | string
  username: string
  avatarUrl?: string | null
  realName?: string | null
  gender?: 'UNKNOWN' | 'MALE' | 'FEMALE' | null
  phone?: string | null
  email?: string | null
  idCard?: string | null
  employeeNumber?: string | null
  department?: string | null
  position?: string | null
  role: MemberRole
  status: MemberStatus
  joinedAt?: string | null
}

export interface MyTeamInfoVO {
  teamId: number | string
  competitionId?: number | string | null
  competitionTitle?: string | null
  teamName: string
  avatarUrl?: string | null
  description?: string | null
  organization?: string | null
  organizationType?: OrganizationType | null
  status: TeamStatus
  auditReason?: string | null
  myRole?: MemberRole | null
  isCaptain?: boolean | null
  members: CompetitionMemberVO[]
  myRegistrationData?: Record<string, string> | null
  createdAt?: string | null
}

export interface TeamInvitationVO {
  id: number | string
  competitionId: number | string
  competitionTitle?: string | null
  teamId: number | string
  teamName: string
  captainId: number | string
  captainName?: string | null
  role: MemberRole
  status: MemberStatus
  invitedAt?: string | null
  realName?: string | null
  email?: string | null
  phone?: string | null
}

export interface TeamMemberSignupDTO {
  realName: string
  gender: 'MALE' | 'FEMALE'
  role: MemberRole
  phone: string
  email: string
  idCard: string
  employeeNumber?: string
  department?: string
  position?: string
}

export interface TeamLeadersSignupDTO {
  realName: string
  phone: string
  gender?: 'MALE' | 'FEMALE'
  email?: string
  idCard?: string
  employeeNumber?: string
  department?: string
  position?: string
}

export interface TeamSignupRequestDTO {
  teamName: string
  avatarUrl?: string
  description?: string
  organization?: string
  organizationType?: OrganizationType
  members: TeamMemberSignupDTO[]
  leaders?: TeamLeadersSignupDTO[]
}

export interface TeamUpdateRequestDTO {
  teamName?: string
  avatarUrl?: string
  description?: string
  organization?: string
  organizationType?: OrganizationType
  members?: TeamMemberSignupDTO[]
  leaders?: TeamLeadersSignupDTO[]
}

export interface TeamInviteAcceptDTO {
  customFields?: Record<string, string>
}

export interface CtfCompetitionChallengeVO {
  id: number | string
  challengeId: number | string
  title?: string
  name?: string
  category?: string
  categoryName?: string
  difficulty?: string | number | null
  initialPoints?: number
  minPoints?: number
  decayCount?: number
  currentPoints: number
  solvesCount: number
  sortOrder?: number
  isSolvedByMyTeam?: boolean
  isContainer?: boolean
  topTeams?: string[]
}

export interface CtfCompetitionChallengeDetailVO {
  id: number | string
  challengeId: number | string
  title?: string
  name?: string
  category?: string
  categoryName?: string
  difficulty?: string | number | null
  description?: string | null
  attachmentUrl?: string | null
  attachmentName?: string | null
  currentPoints: number
  initialPoints?: number
  minPoints?: number
  decayCount?: number
  solvesCount: number
  isContainer?: boolean
  isSolvedByMyTeam?: boolean
  containerPort?: number | null
  containerHost?: string | null
  topTeams?: string[]
  solves?: CtfChallengeSolveVO[]
  instance?: CtfChallengeInstanceVO | null
}

export interface CtfChallengeSolveVO {
  teamId?: number | string
  teamName: string
  avatarUrl?: string | null
  solvedAt?: string | null
  points?: number
}

export interface CtfFlagSubmitRequestDTO {
  flag: string
}

export interface CtfChallengeInstanceVO {
  challengeId?: number | string
  instanceId?: number | string
  accessUrl?: string | null
  host?: string | null
  port?: number | null
  status?: string | null
  createdAt?: string | null
  expiresAt?: string | null
  remainingTimeSeconds?: number | null
  remainingSeconds?: number | null
}

export interface CtfScoreboardItemVO {
  rank: number
  teamId: number | string
  teamName: string
  avatarUrl?: string | null
  score: number
  solvesCount: number
  lastSolveTime?: string | null
}

export interface CtfScoreboardVO {
  competitionId: number | string
  isFrozen?: boolean | null
  rankings: CtfScoreboardItemVO[]
}

export interface CompetitionSolveNotificationItem {
  id?: string | number
  competitionId?: string | number
  challengeId?: string | number
  challengeTitle?: string
  challengeCategory?: string
  teamId?: string | number
  teamName: string
  teamAvatar?: string | null
  userId?: string | number
  username?: string | null
  userNickname?: string | null
  solveRank?: number
  bloodType?: 'FIRST_BLOOD' | 'SECOND_BLOOD' | 'THIRD_BLOOD' | 'NORMAL' | string
  pointsAwarded?: number
  solvedAt?: string | null
}

export interface CtfTeamSolveRecordVO {
  id: number | string
  challengeId?: number | string | null
  challengeName?: string | null
  category?: string | null
  submissionMode?: string | null
  userId?: number | string | null
  username?: string | null
  points: number
  isFirstBlood: boolean
  solvedAt: string
}

export interface CompetitionWriteupVO {
  id: number | string
  competitionId: number | string
  teamId: number | string
  teamName: string
  userId: number | string
  username: string
  fileName: string
  fileUrl: string
  fileSize?: number | null
  fileType?: string | null
  status?: string | null
  score?: number | null
  comment?: string | null
  createdAt: string
  updatedAt?: string | null
}

export interface CompetitionWriteupSubmitDTO {
  fileName: string
  fileUrl: string
  fileSize?: number
  captchaId: string
  captchaCode: string
}

export interface CompetitionWriteupTemplateVO {
  templateUrl?: string | null
  templateName?: string | null
}

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
  regRequirements?: string
  enableBloodBonus?: boolean
  firstBloodBonusPercentage?: number
  secondBloodBonusPercentage?: number
  thirdBloodBonusPercentage?: number
  config?: Record<string, any>
  createdAt: string
  updatedAt: string
}

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
  config?: Record<string, any>
}

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
  config?: Record<string, any>
}

export interface AdminTeamListVO {
  id: number | string
  competitionId: number | string
  teamName: string
  avatarUrl?: string
  organization?: string
  organizationType?: OrganizationType
  captainId: number | string
  captainUsername: string
  memberCount: number
  status: TeamStatus
  score?: number | null
  auditReason?: string
  createdAt: string
}

export interface AdminTeamStatsVO {
  total: number
  pending: number
  approved: number
  rejected: number
  banned?: number
  ended?: number
}

export interface AdminTeamDetailVO {
  id: number | string
  competitionId: number | string
  competitionTitle?: string
  teamName: string
  avatarUrl?: string
  description?: string
  organization?: string
  organizationType?: OrganizationType
  captainId: number | string
  captainUsername: string
  status: TeamStatus
  score?: number | null
  auditReason?: string
  members: CompetitionMemberVO[]
  createdAt: string
}

export interface TeamAuditRequestDTO {
  status: TeamStatus
  auditReason?: string
}

export interface AdminTeamMemberDTO {
  id?: number | string
  realName?: string
  gender?: 'MALE' | 'FEMALE'
  role?: MemberRole
  phone?: string
  email?: string
  idCard?: string
  employeeNumber?: string
  department?: string
  position?: string
}

export interface AdminTeamUpdateDTO {
  teamName: string
  description?: string
  organization?: string
  organizationType?: OrganizationType
  members?: AdminTeamMemberDTO[]
  leaders?: AdminTeamMemberDTO[]
}

export interface BatchTeamAuditDTO {
  teamIds: (number | string)[]
  status: TeamStatus
  auditReason?: string
}

export interface BatchTeamDeleteDTO {
  teamIds: (number | string)[]
}

export interface CtfChallengeMountRequestDTO {
  challengeId: number | string
  initialPoints?: number
  minPoints?: number
  decayCount?: number
  sortOrder?: number
  isVisible?: boolean
}

export interface CtfChallengeBatchMountRequestDTO {
  items: CtfChallengeMountRequestDTO[]
}

export interface CompetitionWriteupAuditDTO {
  status?: string
  score?: number
  comment?: string
}

export interface AdminCompetitionLogVO {
  id: string | number
  competitionId: string | number
  userId?: string | number
  username?: string
  teamId?: string | number
  teamName?: string
  challengeId?: string | number
  challengeTitle?: string
  action: string
  logType: string
  status: boolean
  operatorIp?: string
  requestMethod?: string
  requestUri?: string
  durationMs?: number
  params?: string
  result?: string
  errorMessage?: string
  userAgent?: string
  createdAt: string
}

export interface AdminCompetitionLogStatisticsVO {
  totalLogs: number
  containerLogs: number
  flagLogs: number
  errorLogs: number
}

export interface AdminCompetitionLogQueryDTO {
  page?: number
  size?: number
  username?: string
  teamName?: string
  challengeTitle?: string
  logType?: string
  status?: boolean
  startTime?: string
  endTime?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export interface StageScoreDetailVO {
  stage: CompetitionType
  stageName: string
  rawScore: number
  weightPercent: number
  weightedScore: number
}

export interface CompositeTeamScoreVO {
  rank: number
  teamId: number | string
  teamName: string
  avatarUrl?: string
  totalFinalScore: number
  stageScores: StageScoreDetailVO[]
}

export interface CompositeScoreboardVO {
  competitionId: number | string
  competitionTitle: string
  isFrozen: boolean
  rankings: CompositeTeamScoreVO[]
}

export interface CtfCompetitionSubmissionVO {
  id: number | string
  userId?: number | string | null
  username?: string | null
  teamId?: number | string | null
  teamName?: string | null
  challengeId?: number | string | null
  challengeName?: string | null
  challengeCategory?: string | null
  submittedFlag: string
  status: 'CORRECT' | 'WRONG' | 'DUPLICATE' | 'INVALID' | string
  createdAt: string
}




