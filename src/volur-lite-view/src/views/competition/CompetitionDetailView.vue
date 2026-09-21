<template>
  <div class="competition-detail-view">
    <!-- Subview Top Header -->
    <div class="subview-header">
      <div class="header-left">
        <n-button text size="small" class="back-btn" @click="backToList">
          <template #icon>
            <n-icon :component="ArrowBackOutline" />
          </template>
          {{ isAdminPreview ? '返回比赛管理' : t('competition.back_to_list') }}
        </n-button>

        <div class="title-wrap">
          <h2 class="subview-title">{{ competition?.title || '竞赛详情' }}</h2>
          <n-tag v-if="isAdminPreview" type="warning" size="small" round :bordered="false">
            前台预览
          </n-tag>
        </div>
      </div>
    </div>

    <!-- Detail Body -->
    <n-spin :show="loading">
      <div v-if="competition" class="detail-body">
        <!-- Banner Image -->
        <div v-if="competition.bannerUrl" class="comp-banner-box">
          <img :src="formatBannerUrl(competition.bannerUrl)" :alt="competition.title" />
        </div>

        <!-- Section 1: 报名与入场专属主控制卡片 -->
        <n-card v-if="shouldShowActionCard" class="action-status-card" :bordered="false">
          <div class="status-card-inner">
            <!-- Case A: 用户未登录 -->
            <div v-if="!userStore.isLoggedIn" class="unauth-box">
              <n-alert type="info" :title="t('competition.login_to_signup')">
                <div class="unauth-row">
                  <span>登录账号后即可组建或加入战队参赛，享受自动化积分演练。</span>
                  <n-button class="theme-primary-btn" size="small" @click="goToLogin">
                    {{ t('competition.go_to_login') }}
                  </n-button>
                </div>
              </n-alert>
            </div>

            <!-- Case B: 用户已登录，但尚未报名战队 -->
            <div v-else-if="!myTeam" class="signup-needed-box">
              <div class="signup-header">
                <div class="titles">
                  <h3>报名参赛通道</h3>
                  <p class="sub">
                    本赛事采用战队竞技制，每队 {{ competition.minTeamMembers || 1 }} - {{ competition.maxTeamMembers || 4 }} 人<template v-if="competition.maxLeaders !== undefined">，领队最多 {{ competition.maxLeaders }} 人</template>。
                  </p>
                </div>

                <!-- Registration Action Button -->
                <div class="action-btn-area">
                  <n-button
                    v-if="invitations.length > 0"
                    secondary
                    type="info"
                    size="large"
                    class="invitations-btn"
                    @click="teamInvitationsModalVisible = true"
                  >
                    <template #icon>
                      <n-icon :component="MailOutline" />
                    </template>
                    {{ t('competition.my_invitations_btn') }} ({{ invitations.length }})
                  </n-button>

                  <n-button
                    v-if="isRegistrationOpen"
                    class="theme-primary-btn signup-btn"
                    size="large"
                    @click="openSignupModal"
                  >
                    <template #icon>
                      <n-icon :component="RocketOutline" />
                    </template>
                    {{ t('competition.signup_btn') }}
                  </n-button>
                  <n-tag v-else type="warning" size="large" round>
                    {{ getRegistrationStatusTip() }}
                  </n-tag>
                </div>
              </div>
            </div>

            <!-- Case C: 用户已报名战队 -->
            <div v-else class="registered-team-box">
              <div class="team-meta-row">
                <div class="team-info">
                  <AppAvatar
                    round
                    :size="52"
                    :src="myTeam.avatarUrl"
                    :name="myTeam.teamName"
                  />
                  <div class="team-texts">
                    <div class="title-role">
                      <h4>{{ myTeam.teamName }}</h4>
                      <n-tag v-if="myTeam.myRole && myTeam.myRole !== 'CAPTAIN'" size="small" type="default" round>
                        {{ myTeam.myRole === 'LEADER' ? '领队' : '队员' }}
                      </n-tag>
                    </div>
                    <p v-if="myTeam.description" class="team-desc" :title="myTeam.description">
                      {{ formatTeamDescription(myTeam.description) }}
                    </p>
                  </div>
                </div>

                <div class="team-actions">
                  <!-- 收到的组队邀请按钮 (若有待处理邀请) -->
                  <n-button
                    v-if="invitations.length > 0"
                    secondary
                    type="info"
                    size="small"
                    @click="teamInvitationsModalVisible = true"
                  >
                    <template #icon>
                      <n-icon :component="MailOutline" />
                    </template>
                    {{ t('competition.my_invitations_btn') }} ({{ invitations.length }})
                  </n-button>

                  <!-- 查看队伍信息按钮 (全体战队成员可见) -->
                  <n-button
                    secondary
                    size="small"
                    @click="openViewTeamModal"
                  >
                    <template #icon>
                      <n-icon :component="EyeOutline" />
                    </template>
                    {{ t('competition.view_team_btn', '查看队伍信息') }}
                  </n-button>
                </div>
              </div>

              <!-- Team Audit & Arena Entry Condition Alerts -->
              <div class="team-entry-status">
                <!-- 1. 待审核 -->
                <n-alert v-if="myTeam.status === 'PENDING_AUDIT' || myTeam.status === 'AUDIT_DRAFT'" type="warning" title="队伍报名审核中">
                  {{ t('competition.team_pending_audit') }}
                </n-alert>

                <!-- 2. 审核被拒 -->
                <n-alert v-else-if="myTeam.status === 'AUDIT_REJECTED'" type="error" title="队伍审核未通过">
                  {{ myTeam.auditReason || t('competition.team_audit_rejected') }}
                </n-alert>

                <!-- 3. 审核通过 -->
                <div v-else-if="myTeam.status === 'AUDIT_PASSED'" class="audit-passed-flow">
                  <!-- 3.1 比赛未开始：等待开启 -->
                  <n-alert v-if="isUpcoming" type="info" title="报名审核通过，请等待竞赛开启">
                    {{ t('competition.team_registered_waiting') }}
                    <div class="countdown-text">
                      距离比赛开始还有: {{ countdownToStart }}
                    </div>
                  </n-alert>

                  <!-- 3.2 比赛进行中：显示实时倒计时与【进入竞赛】按钮 -->
                  <div v-else-if="isRunning" class="running-entry-box">
                    <div class="competition-countdown-bar">
                      <span class="countdown-label">竞赛结束倒计时:</span>
                      <span class="countdown-value">{{ countdownToEnd }}</span>
                    </div>
                    <n-button class="theme-primary-btn enter-arena-btn" size="large" @click="enterArena">
                      <template #icon>
                        <n-icon :component="RocketOutline" />
                      </template>
                      {{ t('competition.enter_arena') }}
                    </n-button>
                  </div>

                  <!-- 3.3 比赛已结束：倒计时结束之后保留【进入竞赛】按钮 -->
                  <div v-else class="ended-box">
                    <div class="competition-countdown-bar is-ended">
                      <span class="countdown-label">竞赛已结束</span>
                      <span class="countdown-value">00:00:00</span>
                    </div>
                    <n-button class="theme-primary-btn enter-arena-btn" size="large" @click="enterArena">
                      <template #icon>
                        <n-icon :component="RocketOutline" />
                      </template>
                      {{ t('competition.enter_arena') }}
                    </n-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </n-card>

        <!-- Section 2: 竞赛详细信息（简介与赛程、队伍与防作弊规则） -->
        <n-card class="content-detail-card" :bordered="false">
          <!-- 竞赛简介与赛程 -->
          <div class="detail-section">
            <div class="section-header">
              <n-icon :component="DocumentTextOutline" class="section-icon" />
              <h3 class="section-title">{{ t('competition.section_overview') }}</h3>
            </div>

            <!-- 竞赛简介 Markdown 渲染 -->
            <div
              v-if="competition.description"
              class="volur-markdown-body desc-markdown-content"
              v-html="renderMarkdown(competition.description)"
            />
            <p v-else class="desc-text-empty">主办方暂未发布详细介绍。</p>

            <n-descriptions label-placement="left" :column="1" bordered size="small" class="detail-descriptions">
              <n-descriptions-item :label="t('competition.start_time')">
                {{ formatDateTime(competition.startTime) }} ~ {{ formatDateTime(competition.endTime) }}
              </n-descriptions-item>
              <n-descriptions-item v-if="competition.regStartTime && competition.regEndTime" :label="t('competition.reg_time')">
                {{ formatDateTime(competition.regStartTime) }} ~ {{ formatDateTime(competition.regEndTime) }}
              </n-descriptions-item>
              <n-descriptions-item :label="t('competition.scale')">
                {{ t('competition.scale_teams', { teamCount: competition.teamCount || 0 }) }}
              </n-descriptions-item>
            </n-descriptions>
          </div>

          <n-divider class="section-divider" />

          <!-- 队伍与防作弊规则 -->
          <div class="detail-section">
            <div class="section-header">
              <n-icon :component="ShieldCheckmarkOutline" class="section-icon" />
              <h3 class="section-title">{{ t('competition.section_rules') }}</h3>
            </div>

            <n-descriptions label-placement="left" :column="1" bordered size="small" class="detail-descriptions">
              <n-descriptions-item :label="t('competition.team_rules')">
                {{ t('competition.team_limit', { min: competition.minTeamMembers || 1, max: competition.maxTeamMembers || 4 }) }}
                <template v-if="competition.maxLeaders !== undefined">
                  ，{{ t('competition.leader_limit', { max: competition.maxLeaders }) }}
                </template>
              </n-descriptions-item>
              <n-descriptions-item :label="t('competition.require_audit')">
                {{ competition.requireTeamAudit ? t('competition.audit_required') : t('competition.audit_not_required') }}
              </n-descriptions-item>
              <n-descriptions-item :label="t('competition.writeup_submission')">
                {{ competition.allowWriteupSubmission ? t('competition.writeup_allowed') : t('competition.writeup_not_allowed') }}
              </n-descriptions-item>
            </n-descriptions>
          </div>
        </n-card>
      </div>
    </n-spin>

    <!-- 战队信息与报名弹窗 (Team Form Modal) -->
    <team-form-modal
      v-model:show="teamFormModalVisible"
      :competition="competition"
      :mode="teamFormMode"
      :initial-team="myTeam"
      @success="handleTeamFormSuccess"
      @disbanded="handleTeamDisbanded"
    />

    <!-- 组队邀请弹窗 (Team Invitations Modal) -->
    <team-invitations-modal
      v-model:show="teamInvitationsModalVisible"
      :invitations="invitations"
      @accepted="handleInvitationAccepted"
      @rejected="handleInvitationRejected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NCard,
  NTag,
  NButton,
  NDivider,
  NIcon,
  NSpin,
  NDescriptions,
  NDescriptionsItem,
  NAlert
} from 'naive-ui'
import AppAvatar from '@/components/common/AppAvatar.vue'
import {
  EyeOutline,
  ArrowBackOutline,
  RocketOutline,
  DocumentTextOutline,
  ShieldCheckmarkOutline,
  MailOutline
} from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/modules/user'
import { useCompetitionStore } from '@/store/modules/competition'
import type {
  CompetitionDetailVO,
  MyTeamInfoVO,
  TeamInvitationVO
} from '@/types/competition'
import {
  getCompetitionBySlugApi,
  getCompetitionByIdApi,
  getAdminCompetitionByIdApi,
  getMyTeamApi,
  getMyInvitationsApi
} from '@/api/competition'
import { formatBannerUrl } from '@/utils/imageCache'
import { renderMarkdown } from '@/utils/markdown'
import TeamFormModal from './components/TeamFormModal.vue'
import TeamInvitationsModal from './components/TeamInvitationsModal.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const competitionStore = useCompetitionStore()

const slug = computed(() => String(route.params.id || route.params.slug || ''))
const isAdminPreview = computed(() => route.path.startsWith('/admin'))

const loading = ref<boolean>(false)
const competition = ref<CompetitionDetailVO | null>(null)
const myTeam = ref<MyTeamInfoVO | null>(null)

// Current timestamp for reactive countdown & status
const nowTimestamp = ref<number>(Date.now())

// Countdown to start & end
const countdownToStart = ref<string>('00:00:00')
const countdownToEnd = ref<string>('00:00:00')
let countdownTimer: ReturnType<typeof setInterval> | null = null

// Team Form & Invitations Modal State
const teamFormModalVisible = ref<boolean>(false)
const teamFormMode = ref<'create' | 'view' | 'edit'>('create')
const teamInvitationsModalVisible = ref<boolean>(false)
const invitations = ref<TeamInvitationVO[]>([])

/** 比赛是否已开始（包括进行中、冻结或已结束/归档） */
const hasStarted = computed(() => {
  if (!competition.value) return false
  if (competition.value.status === 'DRAFT') return false
  if (
    competition.value.status === 'RUNNING' ||
    competition.value.status === 'FROZEN' ||
    competition.value.status === 'ENDED' ||
    competition.value.status === 'ARCHIVED'
  ) {
    return true
  }
  if (competition.value.startTime) {
    return nowTimestamp.value >= new Date(competition.value.startTime).getTime()
  }
  return false
})

/** 比赛是否尚未开始 */
const isUpcoming = computed(() => {
  if (!competition.value) return false
  if (hasStarted.value) return false
  return competition.value.status === 'PUBLISHED' || competition.value.status === 'REGISTRATION' || competition.value.status === 'DRAFT'
})

/** 比赛是否处于进行中 */
const isRunning = computed(() => {
  if (!competition.value) return false
  if (competition.value.status === 'RUNNING' || competition.value.status === 'FROZEN') return true
  if (competition.value.status === 'ENDED' || competition.value.status === 'ARCHIVED' || competition.value.status === 'DRAFT') {
    return false
  }
  const start = new Date(competition.value.startTime).getTime()
  const end = new Date(competition.value.endTime).getTime()
  return nowTimestamp.value >= start && nowTimestamp.value <= end
})

/** 当前用户是否已完成该赛事的战队报名 */
const isUserRegistered = computed(() => {
  return Boolean(userStore.isLoggedIn && myTeam.value && myTeam.value.status !== 'DISBANDED')
})

/**
 * 是否展示“报名与入场专属主控制卡片”
 * 铁律：当比赛开始之后，用户若尚未报名参赛，则彻底不展示主控制卡片，仅展示简介与赛程、队伍与防作弊规则
 */
const shouldShowActionCard = computed(() => {
  if (hasStarted.value && !isUserRegistered.value) {
    return false
  }
  return true
})

const isRegistrationOpen = computed(() => {
  if (!competition.value) return false
  if (competition.value.status === 'DRAFT' || competition.value.status === 'ARCHIVED' || competition.value.status === 'ENDED') {
    return false
  }
  // 比赛已开始，严格禁止报名
  if (hasStarted.value) {
    return false
  }
  if (competition.value.regStartTime && nowTimestamp.value < new Date(competition.value.regStartTime).getTime()) {
    return false
  }
  if (competition.value.regEndTime && nowTimestamp.value > new Date(competition.value.regEndTime).getTime()) {
    return false
  }
  return true
})

function getRegistrationStatusTip(): string {
  if (!competition.value) return '报名未开放'
  if (competition.value.status === 'DRAFT') {
    return '竞赛未发布，暂不开放报名'
  }
  if (competition.value.status === 'ARCHIVED' || competition.value.status === 'ENDED') {
    return '竞赛已结束或已归档，报名通道已关闭'
  }
  if (hasStarted.value) {
    return '比赛已开始，报名通道已关闭'
  }
  if (competition.value.regStartTime && nowTimestamp.value < new Date(competition.value.regStartTime).getTime()) {
    return `报名开放时间: ${formatDateTime(competition.value.regStartTime)}`
  }
  if (competition.value.regEndTime && nowTimestamp.value > new Date(competition.value.regEndTime).getTime()) {
    return '报名已截止'
  }
  return '报名进行中'
}

function formatDateTime(val?: string | null): string {
  if (!val) return '-'
  try {
    const d = new Date(val)
    if (isNaN(d.getTime())) return val
    const pad = (n: number) => (n < 10 ? '0' + n : n)
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch {
    return val
  }
}


function backToList() {
  if (isAdminPreview.value) {
    if (route.params.id) {
      router.push(`/admin/competitions/${route.params.id}/edit`)
    } else {
      router.push('/admin/competitions')
    }
  } else {
    router.push('/competitions')
  }
}

function goToLogin() {
  if (competition.value) {
    router.push(`/auth/login?redirect=/competitions/${competition.value.slug || competition.value.id}`)
  } else {
    router.push('/auth/login')
  }
}

function openSignupModal() {
  teamFormMode.value = 'create'
  teamFormModalVisible.value = true
}

function openViewTeamModal() {
  teamFormMode.value = 'view'
  teamFormModalVisible.value = true
}

async function handleTeamFormSuccess() {
  if (competition.value) {
    await fetchMyTeamStatus(competition.value.id)
    await fetchMyInvitations(competition.value.id)
  }
}

async function handleTeamDisbanded() {
  myTeam.value = null
  if (competition.value) {
    await fetchMyTeamStatus(competition.value.id)
    await fetchMyInvitations(competition.value.id)
  }
}

async function fetchMyInvitations(compId: number | string) {
  if (isAdminPreview.value || !userStore.isLoggedIn) {
    invitations.value = []
    return
  }
  try {
    const res = await getMyInvitationsApi(compId)
    invitations.value = res.data || []
  } catch {
    invitations.value = []
  }
}

async function handleInvitationAccepted() {
  if (competition.value) {
    await fetchMyTeamStatus(competition.value.id)
    await fetchMyInvitations(competition.value.id)
  }
}

async function handleInvitationRejected() {
  if (competition.value) {
    await fetchMyInvitations(competition.value.id)
  }
}

function enterArena() {
  if (!competition.value) return
  router.push(`/arena/${competition.value.slug}`)
}

async function fetchMyTeamStatus(compId: number | string) {
  if (isAdminPreview.value) {
    myTeam.value = null
    return
  }
  try {
    const res = await getMyTeamApi(compId)
    if (res.data) {
      myTeam.value = res.data
    } else {
      myTeam.value = null
    }
  } catch {
    myTeam.value = null
  }
}

function formatTeamDescription(desc?: string | null): string {
  if (!desc) return ''
  const trimmed = desc.trim()
  if (trimmed.length > 30) {
    return trimmed.slice(0, 30) + '....'
  }
  return trimmed
}

function startCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  updateCountdown()
  countdownTimer = setInterval(updateCountdown, 1000)
}

function updateCountdown() {
  if (!competition.value) return
  const now = new Date().getTime()
  nowTimestamp.value = now

  const pad = (n: number) => (n < 10 ? '0' + n : String(n))

  // 1. 距离开始倒计时
  if (competition.value.startTime) {
    const start = new Date(competition.value.startTime).getTime()
    const diffStart = Math.max(0, start - now)
    const totalSecStart = Math.floor(diffStart / 1000)
    const hStart = Math.floor(totalSecStart / 3600)
    const mStart = Math.floor((totalSecStart % 3600) / 60)
    const sStart = totalSecStart % 60
    countdownToStart.value = `${pad(hStart)}:${pad(mStart)}:${pad(sStart)}`
  }

  // 2. 距离结束倒计时
  if (competition.value.endTime) {
    const end = new Date(competition.value.endTime).getTime()
    const diffEnd = Math.max(0, end - now)
    const totalSecEnd = Math.floor(diffEnd / 1000)
    const hEnd = Math.floor(totalSecEnd / 3600)
    const mEnd = Math.floor((totalSecEnd % 3600) / 60)
    const sEnd = totalSecEnd % 60
    countdownToEnd.value = `${pad(hEnd)}:${pad(mEnd)}:${pad(sEnd)}`
  }
}

async function loadCompetition() {
  if (!slug.value) return
  loading.value = true
  try {
    const rawTarget = slug.value.trim()
    let res
    if (isAdminPreview.value) {
      res = await getAdminCompetitionByIdApi(rawTarget)
    } else if (/^\d+$/.test(rawTarget)) {
      res = await getCompetitionByIdApi(rawTarget)
    } else {
      res = await getCompetitionBySlugApi(rawTarget)
    }

    if (res?.data) {
      const data = res.data as any
      const normalizedDetail: CompetitionDetailVO = {
        ...data,
        minTeamMembers: data.minTeamMembers ?? data.config?.minTeamMembers,
        maxTeamMembers: data.maxTeamMembers ?? data.config?.maxTeamMembers,
        requireTeamAudit: data.requireTeamAudit ?? data.config?.requireTeamAudit,
        allowWriteupSubmission: data.allowWriteupSubmission ?? data.config?.allowWriteupSubmission,
        registrationFields: data.registrationFields ?? data.fieldSchemas
      }
      competition.value = normalizedDetail
      competitionStore.currentCompetition = normalizedDetail
      if (userStore.isLoggedIn && !isAdminPreview.value) {
        fetchMyTeamStatus(data.id)
        fetchMyInvitations(data.id)
      } else {
        myTeam.value = null
        invitations.value = []
      }
      startCountdown()
    } else {
      router.replace('/404')
    }
  } catch {
    router.replace('/404')
  } finally {
    loading.value = false
  }
}

watch(
  () => [route.params.slug, route.params.id],
  ([newSlug, newId]) => {
    if (newSlug || newId) {
      loadCompetition()
    }
  }
)

onMounted(() => {
  loadCompetition()
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  competitionStore.clearCompetitionState()
})
</script>

<style scoped lang="scss">
.competition-detail-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.subview-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;

  .header-left {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .back-btn {
      align-self: flex-start;
      padding: 0;
      color: #444444;
      background-color: transparent !important;
      background: transparent !important;
      transition: color 0.15s ease;

      &:hover,
      &:focus,
      &:active {
        color: #111111 !important;
        background-color: transparent !important;
        background: transparent !important;

        :deep(.n-icon) {
          color: #111111 !important;
        }
      }

      :deep(.n-button__state-border) {
        display: none !important;
      }
    }

    .title-wrap {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 10px;

      .subview-title {
        font-size: 24px;
        font-weight: 700;
        margin: 0;
        color: var(--v-text-primary, #111111);
      }
    }
  }
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .comp-banner-box {
    width: 100%;
    border-radius: 12px;
    overflow: hidden;
    background-color: rgba(0, 0, 0, 0.04);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);

    img {
      width: 100%;
      height: auto;
      display: block;
      object-fit: contain;
    }
  }

  .action-status-card {
    border-radius: 12px;
    background: var(--v-bg-card, #ffffff);

    .status-card-inner {
      padding: 6px;
    }

    .unauth-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      margin-top: 8px;
    }

    .signup-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;

      .titles {
        h3 {
          font-size: 18px;
          font-weight: 700;
          margin: 0 0 4px;
        }
        .sub {
          font-size: 13px;
          color: #444444;
          margin: 0;
        }
      }

      .action-btn-area {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
      }

      .signup-btn {
        padding: 0 28px;
      }
    }

    .registered-team-box {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .team-meta-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 16px;

        .team-info {
          display: flex;
          align-items: center;
          gap: 14px;

          .team-texts {
            .title-role {
              display: flex;
              align-items: center;
              gap: 8px;

              h4 {
                font-size: 18px;
                font-weight: 700;
                margin: 0;
              }
            }

            .team-desc {
              font-size: 13px;
              color: #444444;
              margin: 4px 0 0 0;
              line-height: 1.4;
              word-break: break-all;
            }
          }
        }

        .team-actions {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
        }
      }

      .running-entry-box,
      .ended-box {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 16px;
        background: rgba(0, 0, 0, 0.03);
        border: 1px solid var(--v-border-base, #e5e4e7);
        border-radius: 10px;
        padding: 16px 20px;

        .competition-countdown-bar {
          display: flex;
          align-items: center;
          gap: 10px;

          .countdown-label {
            font-size: 14px;
            font-weight: 600;
            color: #444444;
          }

          .countdown-value {
            font-family: var(--v-font-mono, 'Fira Code', monospace);
            font-size: 18px;
            font-weight: 700;
            color: var(--v-primary, #000000);
            letter-spacing: 0.5px;
          }

          &.is-ended {
            .countdown-label {
              color: #888888;
            }
            .countdown-value {
              color: #888888;
            }
          }
        }

        .enter-arena-btn {
          padding: 0 32px;
          height: 44px;
          font-size: 15px;
        }
      }

      .countdown-text {
        font-family: var(--v-font-mono, 'Fira Code', monospace);
        font-weight: 700;
        margin-top: 6px;
        font-size: 14px;
      }
    }
  }

  .content-detail-card {
    border-radius: 12px;
    background: var(--v-bg-card, #ffffff);

    .detail-section {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .section-header {
        display: flex;
        align-items: center;
        gap: 8px;

        .section-icon {
          font-size: 18px;
          color: var(--v-text-primary, #111111);
        }

        .section-title {
          font-size: 16px;
          font-weight: 700;
          margin: 0;
          color: var(--v-text-primary, #111111);
        }
      }

      .desc-text-empty {
        font-size: 14px;
        color: var(--v-text-muted, #666666);
        margin: 0;
      }

      .desc-markdown-content {
        margin: 0;
      }

      .detail-descriptions {
        background: transparent;
      }
    }

    .section-divider {
      margin: 24px 0;
    }
  }
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.theme-primary-btn {
  background: #000000 !important;
  color: #ffffff !important;
  border-radius: 8px;
  font-weight: 600;

  &:hover {
    background: #222222 !important;
  }
}
</style>
