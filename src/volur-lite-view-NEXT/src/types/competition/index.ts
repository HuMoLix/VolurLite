export type CompetitionType = 'CTF' | 'THEORY' | 'AWD' | 'AWD_PLUS' | 'ISW'

export type CompetitionStatus =
  | 'DRAFT'
  | 'PUBLISHED'
  | 'RUNNING'
  | 'ENDED'
  | 'ARCHIVED'
  | 'REGISTRATION'
  | 'FROZEN'

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
  correctCount: number
  totalScore: number
  scoreObtained: number
  accuracyRate: number
}

export type TeamStatus =
  | 'PENDING_AUDIT'
  | 'AUDIT_DRAFT'
  | 'AUDIT_PASSED'
  | 'AUDIT_REJECTED'
  | 'DISBANDED'

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

