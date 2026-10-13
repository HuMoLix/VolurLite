<template>
  <div class="competition-detail-view flex-1 flex flex-col min-w-0 px-4 sm:px-6 lg:px-8 py-8 sm:py-12 transition-colors">
    <div
      v-if="loading"
      class="fixed top-0 left-0 right-0 h-1 z-50 overflow-hidden bg-blue-100 dark:bg-blue-950"
    >
      <div class="progress-bar-primary" />
      <div class="progress-bar-secondary" />
    </div>

    <div class="flex-1 flex flex-col w-full max-w-7xl mx-auto space-y-6">
      <nav class="flex items-center gap-3.5 flex-wrap min-w-0">
        <button
          type="button"
          class="back-btn"
          @click="backToList"
        >
          <n-icon :size="16" :component="ArrowBackOutline" />
          <span>{{ t('competition.back_to_list', '返回竞赛列表') }}</span>
        </button>

        <div v-if="competition?.title" class="h-4 w-px bg-google-border-light dark:border-google-border-dark hidden sm:block" />

        <div v-if="competition" class="flex items-center gap-2.5 flex-wrap min-w-0">
          <h2 class="m-0 text-base sm:text-lg font-bold tracking-tight text-google-text-primary dark:text-[#FCFCFC] truncate">
            {{ competition.title }}
          </h2>

          <span
            v-if="competition.type"
            class="px-2.5 py-0.5 rounded-full text-xs font-semibold border border-google-border-light dark:border-google-border-dark text-google-text-desc dark:text-google-text-darkDesc bg-google-surface-light dark:bg-google-surface-dark"
          >
            {{ getTypeLabel(competition.type) }}
          </span>
        </div>
      </nav>

      <div
        v-if="competition?.bannerUrl"
        class="comp-banner-box"
      >
        <img
          :src="formatBannerUrl(competition.bannerUrl)"
          :alt="competition.title"
          class="comp-banner-img"
        />
      </div>

      <section
        v-if="competition && shouldShowActionCard"
        class="rounded-2xl sm:rounded-3xl p-6 sm:p-8 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm transition-colors"
      >
        <div v-if="!userStore.isLoggedIn" class="p-5 rounded-2xl bg-blue-50/60 dark:bg-blue-950/30 border border-blue-200/60 dark:border-blue-900/60 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div class="space-y-1">
            <h3 class="m-0 text-base font-semibold text-google-blue dark:text-google-blue-dark flex items-center gap-2">
              <n-icon :size="20" :component="InformationCircleOutline" />
              <span>登录开启参赛体验</span>
            </h3>
            <p class="m-0 text-xs sm:text-sm text-google-text-desc dark:text-google-text-darkDesc">
              登录账号后即可组建或加入战队参赛，享受自动化积分演练与答题竞技。
            </p>
          </div>

          <button
            type="button"
            class="h-10 px-6 rounded-full bg-google-blue hover:bg-google-blue-hover text-white dark:bg-google-blue-dark dark:text-google-surface-dark text-sm font-semibold transition-colors cursor-pointer border-none shrink-0 self-start sm:self-center"
            @click="goToLogin"
          >
            {{ t('competition.go_to_login', '前往登录') }}
          </button>
        </div>

        <div v-else-if="!myTeam" class="space-y-4">
          <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div class="space-y-1.5">
              <h3 class="m-0 text-xl font-bold text-google-text-primary dark:text-[#FCFCFC]">
                报名参赛
              </h3>
              <p class="m-0 text-xs sm:text-sm text-google-text-desc dark:text-google-text-darkDesc">
                每队限制 {{ competition.minTeamMembers || 1 }} - {{ competition.maxTeamMembers || 4 }} 人
                <template v-if="competition.maxLeaders !== undefined && competition.maxLeaders !== null">
                  ，领队最多 {{ competition.maxLeaders }} 人
                </template>
                。填报后全队即可生效。
              </p>
            </div>

            <div class="flex items-center gap-3 flex-wrap">
              <button
                v-if="invitations.length > 0"
                type="button"
                class="h-10 px-4 rounded-full border border-google-blue/30 text-google-blue dark:text-google-blue-dark bg-blue-50/50 dark:bg-blue-950/30 hover:bg-blue-100/50 text-sm font-semibold transition-colors cursor-pointer flex items-center gap-2"
                @click="teamInvitationsModalVisible = true"
              >
                <n-icon :size="16" :component="MailOutline" />
                <span>我的邀请 ({{ invitations.length }})</span>
              </button>

              <button
                v-if="isRegistrationOpen"
                type="button"
                class="h-10 px-6 rounded-full bg-google-blue hover:bg-google-blue-hover text-white dark:bg-google-blue-dark dark:text-google-surface-dark text-sm font-semibold shadow-sm transition-colors cursor-pointer flex items-center gap-2 border-none"
                @click="openSignupModal"
              >
                <n-icon :size="16" :component="RocketOutline" />
                <span>{{ t('competition.signup_btn', '立即报名参赛') }}</span>
              </button>

              <span
                v-else
                class="px-4 py-2 rounded-full text-xs font-semibold border border-amber-300 dark:border-amber-900/60 bg-amber-50 dark:bg-amber-950/40 text-amber-800 dark:text-amber-300"
              >
                {{ getRegistrationStatusTip() }}
              </span>
            </div>
          </div>
        </div>

        <div v-else class="space-y-6">
          <div class="p-5 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/80 dark:border-google-border-dark/80 flex flex-col md:flex-row md:items-center justify-between gap-5">
            <div class="flex items-center gap-4 min-w-0">
              <VAvatar
                :src="myTeam.avatarUrl || ''"
                :alt="myTeam.teamName"
                :size="56"
                :radius="16"
                class="border border-google-border-light dark:border-google-border-dark shrink-0"
              >
                <template #guest>
                  <n-icon :size="28" :component="PeopleOutline" class="text-google-blue dark:text-google-blue-dark" />
                </template>
              </VAvatar>

              <div class="space-y-1 min-w-0">
                <div class="flex items-center gap-2.5 flex-wrap">
                  <h3 class="m-0 text-lg font-bold text-google-text-primary dark:text-[#FCFCFC] truncate">
                    {{ myTeam.teamName }}
                  </h3>
                  <span
                    class="px-2.5 py-0.5 rounded-full text-xs font-semibold"
                    :class="getTeamStatusBadgeClass(myTeam.status)"
                  >
                    {{ getTeamStatusLabel(myTeam.status) }}
                  </span>
                  <span
                    v-if="myTeam.myRole && myTeam.myRole !== 'CAPTAIN'"
                    class="px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-google-blue dark:bg-blue-950/60 dark:text-google-blue-dark"
                  >
                    {{ myTeam.myRole === 'LEADER' ? '领队' : '队员' }}
                  </span>
                </div>

                <p v-if="myTeam.description" class="m-0 text-xs text-google-text-desc dark:text-google-text-darkDesc line-clamp-1">
                  {{ myTeam.description }}
                </p>

                <div v-if="myTeam.organization" class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
                  所属单位: <span class="text-google-text-desc dark:text-google-text-darkDesc">{{ myTeam.organization }}</span>
                </div>
              </div>
            </div>

            <div class="flex items-center gap-3 shrink-0 flex-wrap">
              <button
                v-if="invitations.length > 0"
                type="button"
                class="competition-btn competition-btn-info"
                @click="teamInvitationsModalVisible = true"
              >
                <n-icon :size="16" :component="MailOutline" />
                <span>收到的邀请 ({{ invitations.length }})</span>
              </button>

              <VBtn
                clz="normal"
                :left-icon="EyeOutline"
                @click="openTeamDetailModal"
              >
                {{ t('competition.view_team_btn', '查看队伍信息') }}
              </VBtn>
            </div>
          </div>

          <div class="space-y-4">
            <div
              v-if="myTeam.status === 'PENDING_AUDIT' || myTeam.status === 'AUDIT_DRAFT'"
              class="p-4 rounded-2xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900/60 text-amber-900 dark:text-amber-200 flex items-start gap-3 text-xs sm:text-sm"
            >
              <n-icon :size="20" :component="AlertCircleOutline" class="shrink-0 text-amber-600 dark:text-amber-400 mt-0.5" />
              <div class="space-y-0.5">
                <div class="font-bold">队伍报名审核中</div>
                <div>{{ t('competition.team_pending_audit', '您的战队已提交报名，等待管理员审核通过后方可参赛。') }}</div>
              </div>
            </div>

            <div
              v-else-if="myTeam.status === 'AUDIT_REJECTED'"
              class="p-4 rounded-2xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 text-red-900 dark:text-red-200 flex items-start gap-3 text-xs sm:text-sm"
            >
              <n-icon :size="20" :component="CloseCircleOutline" class="shrink-0 text-google-red dark:text-google-red-dark mt-0.5" />
              <div class="space-y-0.5">
                <div class="font-bold">战队审核未通过</div>
                <div>{{ myTeam.auditReason || t('competition.team_audit_rejected', '未能通过主办方审核，请联系管理员。') }}</div>
              </div>
            </div>

            <div v-else-if="myTeam.status === 'AUDIT_PASSED'" class="space-y-4">
              <div
                v-if="isUpcoming"
                class="p-5 rounded-2xl bg-blue-50/70 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900/60 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
              >
                <div class="space-y-1">
                  <div class="font-bold text-google-blue dark:text-google-blue-dark text-sm sm:text-base flex items-center gap-2">
                    <n-icon :size="18" :component="CheckmarkCircleOutline" />
                    <span>报名审核通过，请等待竞赛开启</span>
                  </div>
                  <div class="text-xs sm:text-sm text-google-text-desc dark:text-google-text-darkDesc">
                    {{ t('competition.team_registered_waiting', '报名审核通过，比赛尚未开始，请准时参加。') }}
                  </div>
                </div>

                <div class="p-3 rounded-xl bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark text-center shrink-0">
                  <div class="text-[11px] text-google-text-muted dark:text-google-text-darkMuted">距离比赛开始</div>
                  <div class="text-lg font-bold font-mono text-google-blue dark:text-google-blue-dark">
                    {{ countdownToStart }}
                  </div>
                </div>
              </div>

              <div
                v-else-if="isRunning"
                class="p-6 rounded-2xl bg-gradient-to-r from-emerald-500/10 via-teal-500/10 to-blue-500/10 dark:from-emerald-950/40 dark:via-teal-950/30 dark:to-blue-950/40 border border-emerald-300 dark:border-emerald-800 flex flex-col sm:flex-row sm:items-center justify-between gap-5"
              >
                <div class="space-y-1.5">
                  <div class="inline-flex items-center gap-2 px-2.5 py-0.5 rounded-full bg-emerald-100 text-google-green dark:bg-emerald-950 dark:text-google-green-dark text-xs font-bold">
                    <span class="w-2 h-2 rounded-full bg-google-green animate-ping" />
                    <span>竞赛火热进行中</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <span class="text-xs sm:text-sm text-google-text-desc dark:text-google-text-darkDesc">竞赛结束倒计时:</span>
                    <span class="text-xl sm:text-2xl font-bold font-mono text-google-text-primary dark:text-[#FCFCFC]">
                      {{ countdownToEnd }}
                    </span>
                  </div>
                </div>

                <VBtn
                  clz="primary"
                  :left-icon="RocketOutline"
                  size="large"
                  @click="enterArena"
                >
                  {{ t('competition.enter_arena', '进入竞赛') }}
                </VBtn>
              </div>

              <div
                v-else
                class="p-5 rounded-2xl bg-gray-50 dark:bg-gray-900/60 border border-google-border-light dark:border-google-border-dark flex flex-col sm:flex-row sm:items-center justify-between gap-4"
              >
                <div class="space-y-1">
                  <div class="font-bold text-google-text-primary dark:text-[#FCFCFC] text-sm sm:text-base">
                    竞赛已结束
                  </div>
                  <div class="text-xs sm:text-sm text-google-text-muted dark:text-google-text-darkMuted">
                    当前竞赛赛程已完结，您可以进入工作台进行赛后复盘与回顾。
                  </div>
                </div>

                <VBtn
                  clz="primary"
                  :left-icon="RocketOutline"
                  @click="enterArena"
                >
                  回顾复盘
                </VBtn>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section
        v-if="competition"
        class="rounded-2xl sm:rounded-3xl p-6 sm:p-8 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm space-y-8 transition-colors"
      >
        <div class="space-y-6">
          <div class="flex items-center gap-2.5 pb-3 border-b border-google-border-light dark:border-google-border-dark">
            <n-icon :size="20" :component="DocumentTextOutline" class="text-google-blue dark:text-google-blue-dark" />
            <h3 class="m-0 text-base sm:text-lg font-bold text-google-text-primary dark:text-[#FCFCFC]">
              {{ t('competition.section_overview', '赛事简介与概况') }}
            </h3>
          </div>

          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div class="lg:col-span-2 space-y-4">
              <h4 class="m-0 text-sm font-bold text-google-text-primary dark:text-[#FCFCFC]">
                赛事详细介绍
              </h4>
              <div
                v-if="competition.description"
                class="volur-markdown-body text-xs sm:text-sm text-google-text-desc dark:text-[#D4D4D4] leading-relaxed"
                v-html="renderMarkdown(competition.description)"
              />
              <p v-else class="text-xs sm:text-sm text-google-text-muted dark:text-google-text-darkMuted leading-relaxed">
                主办方暂未发布详细介绍说明。
              </p>
            </div>

            <div class="space-y-4">
              <h4 class="m-0 text-sm font-bold text-google-text-primary dark:text-[#FCFCFC]">
                赛程
              </h4>
              <div class="p-4 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/80 dark:border-google-border-dark/80 space-y-3.5 text-xs">
                <div class="flex items-center justify-between">
                  <span class="text-google-text-muted dark:text-google-text-darkMuted">比赛赛制</span>
                  <span class="font-semibold text-google-text-primary dark:text-[#FCFCFC]">{{ getTypeLabel(competition.type) }}</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-google-text-muted dark:text-google-text-darkMuted">当前状态</span>
                  <span class="font-semibold text-google-text-primary dark:text-[#FCFCFC]">{{ getStatusText(effectiveStatus) }}</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-google-text-muted dark:text-google-text-darkMuted">已报名战队</span>
                  <span class="font-semibold text-google-text-primary dark:text-[#FCFCFC]">{{ competition.teamCount || 0 }} 队</span>
                </div>
                <div class="border-t border-google-border-light/60 dark:border-google-border-dark/60 pt-3 space-y-1.5">
                  <div class="text-google-text-muted dark:text-google-text-darkMuted">比赛时间</div>
                  <div class="comp-time-val text-google-text-desc dark:text-[#FCFCFC]">
                    {{ formatDateTime(competition.startTime) }} - {{ formatDateTime(competition.endTime) }}
                  </div>
                </div>
                <div v-if="competition.regStartTime || competition.regEndTime" class="border-t border-google-border-light/60 dark:border-google-border-dark/60 pt-3 space-y-1.5">
                  <div class="text-google-text-muted dark:text-google-text-darkMuted">报名时间</div>
                  <div class="comp-time-val text-google-text-desc dark:text-[#FCFCFC]">
                    {{ formatDateTime(competition.regStartTime) }} - {{ formatDateTime(competition.regEndTime) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="space-y-6 pt-2 border-t border-google-border-light dark:border-google-border-dark">
          <div class="flex items-center gap-2.5 pb-3 border-b border-google-border-light dark:border-google-border-dark">
            <n-icon :size="20" :component="ShieldCheckmarkOutline" class="text-google-blue dark:text-google-blue-dark" />
            <h3 class="m-0 text-base sm:text-lg font-bold text-google-text-primary dark:text-[#FCFCFC]">
              {{ t('competition.section_rules', '规则与须知') }}
            </h3>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="p-5 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/80 dark:border-google-border-dark/80 space-y-3">
              <h4 class="m-0 text-sm font-bold text-google-text-primary dark:text-[#FCFCFC] flex items-center gap-2">
                <n-icon :size="16" :component="PeopleOutline" class="text-google-blue dark:text-google-blue-dark" />
                <span>组队与人员规模</span>
              </h4>
              <ul class="m-0 pl-5 space-y-1.5 text-xs text-google-text-desc dark:text-google-text-darkDesc">
                <li>单队队员人数限制: {{ competition.minTeamMembers || 1 }} ~ {{ competition.maxTeamMembers || 4 }} 人</li>
                <li v-if="competition.maxLeaders !== undefined && competition.maxLeaders !== null">
                  指导老师/领队限制: 最多 {{ competition.maxLeaders }} 人
                </li>
                <li>创建战队后可在线发送队员邀请并管理成员</li>
              </ul>
            </div>

            <div class="p-5 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/80 dark:border-google-border-dark/80 space-y-3">
              <h4 class="m-0 text-sm font-bold text-google-text-primary dark:text-[#FCFCFC] flex items-center gap-2">
                <n-icon :size="16" :component="ShieldCheckmarkOutline" class="text-google-green dark:text-google-green-dark" />
                <span>防作弊与提交流程</span>
              </h4>
              <ul class="m-0 pl-5 space-y-1.5 text-xs text-google-text-desc dark:text-google-text-darkDesc">
                <li>队伍报名审核: {{ competition.requireTeamAudit ? '需平台管理员人工审核通过' : '创建战队即审核通过' }}</li>
                <li>Writeup 题解提交: {{ competition.allowWriteupSubmission ? '赛后支持在线提交 Writeup 题解报告' : '本赛制不开放题解提交' }}</li>
                <li>严禁多账号违规共享 Flag 或跨战队违规协同</li>
              </ul>
            </div>
          </div>

          <div v-if="competition.regRequirements" class="space-y-2">
            <h4 class="m-0 text-sm font-bold text-google-text-primary dark:text-[#FCFCFC]">
              报名专属要求与须知
            </h4>
            <div
              class="volur-markdown-body p-4 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light/80 dark:border-google-border-dark/80 text-xs text-google-text-desc dark:text-google-text-darkDesc"
              v-html="renderMarkdown(competition.regRequirements)"
            />
          </div>
        </div>

      </section>

      <footer class="mt-auto pt-8">
        <AppFooter />
      </footer>
    </div>

    <TeamFormModal
      v-model:show="teamFormModalVisible"
      :competition="competition"
      :mode="teamFormModalMode"
      :initial-team="myTeam"
      @success="handleTeamFormSuccess"
      @updated="refreshMyTeam"
      @disbanded="handleTeamDisbanded"
    />

    <TeamInvitationsModal
      v-model:show="teamInvitationsModalVisible"
      :competition-id="competition?.id || ''"
      :invitations="invitations"
      @accepted="handleInvitationHandled"
      @rejected="handleInvitationHandled"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon, useMessage } from 'naive-ui'
import {
  ArrowBackOutline,
  PeopleOutline,
  InformationCircleOutline,
  RocketOutline,
  MailOutline,
  EyeOutline,
  AlertCircleOutline,
  CloseCircleOutline,
  CheckmarkCircleOutline,
  DocumentTextOutline,
  ShieldCheckmarkOutline
} from '@vicons/ionicons5'
import {
  getCompetitionByIdApi,
  getCompetitionBySlugApi,
  getMyTeamApi,
  getMyInvitationsApi
} from '@/api/competition'
import type {
  CompetitionDetailVO,
  MyTeamInfoVO,
  TeamInvitationVO,
  CompetitionType,
  CompetitionStatus,
  TeamStatus
} from '@/types/competition'
import { useUserStore } from '@/store/modules/user'
import { formatBannerUrl } from '@/utils/imageUrl'
import { renderMarkdown } from '@/utils/markdown'
import AppFooter from '@/components/common/Footer.vue'
import VAvatar from '@/components/sidebar/VAvatar.vue'
import { VBtn } from '@/components/common'
import TeamFormModal from './components/TeamFormModal.vue'
import TeamInvitationsModal from './components/TeamInvitationsModal.vue'

defineOptions({
  name: 'CompetitionDetailView'
})

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const message = useMessage()
const userStore = useUserStore()

const loading = ref(false)
const competition = ref<CompetitionDetailVO | null>(null)
const myTeam = ref<MyTeamInfoVO | null>(null)
const invitations = ref<TeamInvitationVO[]>([])

const teamFormModalVisible = ref(false)
const teamFormModalMode = ref<'create' | 'view' | 'edit'>('view')
const teamInvitationsModalVisible = ref(false)

const nowTimestamp = ref(Date.now())
const countdownToStart = ref('00:00:00')
const countdownToEnd = ref('00:00:00')
let countdownTimer: ReturnType<typeof setInterval> | null = null

const competitionIdentifier = computed(() => {
  return String(route.params.id || route.params.slug || '')
})

const effectiveStatus = computed<CompetitionStatus | null>(() => {
  if (!competition.value) return null
  if (competition.value.status === 'DRAFT' || competition.value.status === 'ARCHIVED' || competition.value.status === 'HIDDEN') {
    return competition.value.status
  }
  const now = nowTimestamp.value
  if (competition.value.endTime && now > new Date(competition.value.endTime).getTime()) {
    return 'ENDED'
  }
  if (competition.value.startTime && now >= new Date(competition.value.startTime).getTime()) {
    return competition.value.status === 'FROZEN' ? 'FROZEN' : 'RUNNING'
  }
  const regStart = competition.value.regStartTime ? new Date(competition.value.regStartTime).getTime() : null
  const regEnd = competition.value.regEndTime ? new Date(competition.value.regEndTime).getTime() : null
  const afterRegStart = regStart === null || now >= regStart
  const beforeRegEnd = regEnd === null || now <= regEnd
  if (afterRegStart && beforeRegEnd) {
    return 'REGISTRATION'
  }
  return 'PUBLISHED'
})

const hasStarted = computed(() => {
  if (!effectiveStatus.value) return false
  return (
    effectiveStatus.value === 'RUNNING' ||
    effectiveStatus.value === 'FROZEN' ||
    effectiveStatus.value === 'ENDED' ||
    effectiveStatus.value === 'ARCHIVED'
  )
})

const isUpcoming = computed(() => {
  if (!effectiveStatus.value) return false
  return (
    effectiveStatus.value === 'PUBLISHED' ||
    effectiveStatus.value === 'REGISTRATION' ||
    effectiveStatus.value === 'DRAFT'
  )
})

const isRunning = computed(() => {
  if (!effectiveStatus.value) return false
  return effectiveStatus.value === 'RUNNING' || effectiveStatus.value === 'FROZEN'
})

const isUserRegistered = computed(() => {
  return Boolean(userStore.isLoggedIn && myTeam.value && myTeam.value.status !== 'DISBANDED')
})

const shouldShowActionCard = computed(() => {
  if (hasStarted.value && !isUserRegistered.value) {
    return false
  }
  return true
})

const isRegistrationOpen = computed(() => {
  if (!effectiveStatus.value) return false
  return effectiveStatus.value === 'REGISTRATION'
})

function getRegistrationStatusTip(): string {
  if (!competition.value) return '报名未开放'
  if (effectiveStatus.value === 'DRAFT') {
    return '竞赛未发布，暂不开放报名'
  }
  if (effectiveStatus.value === 'ARCHIVED' || effectiveStatus.value === 'ENDED') {
    return '竞赛已结束或已归档，报名通道已关闭'
  }
  if (hasStarted.value) {
    return '比赛已开始，报名通道已关闭'
  }
  if (
    competition.value.regStartTime &&
    nowTimestamp.value < new Date(competition.value.regStartTime).getTime()
  ) {
    return `报名开放时间: ${formatDateTime(competition.value.regStartTime)}`
  }
  if (
    competition.value.regEndTime &&
    nowTimestamp.value > new Date(competition.value.regEndTime).getTime()
  ) {
    return '报名已截止'
  }
  return '报名进行中'
}

function getStatusText(status?: CompetitionStatus | null): string {
  switch (status) {
    case 'DRAFT':
      return t('competition.status_draft', '草稿')
    case 'PUBLISHED':
      return t('competition.status_published', '已发布')
    case 'REGISTRATION':
      return t('competition.status_registration', '报名中')
    case 'RUNNING':
      return t('competition.status_running', '进行中')
    case 'FROZEN':
      return t('competition.status_frozen', '已封榜')
    case 'ENDED':
      return t('competition.status_ended', '已结束')
    case 'ARCHIVED':
      return t('competition.status_archived', '已归档')
    case 'HIDDEN':
      return t('competition.status_hidden', '隐藏')
    default:
      return '-'
  }
}

function getTypeLabel(type?: CompetitionType | null): string {
  switch (type) {
    case 'CTF':
      return t('competition.type_ctf', 'CTF 解题赛')
    case 'THEORY':
      return t('competition.type_theory', '理论知识赛')
    case 'AWD':
      return t('competition.type_awd', 'AWD 攻防对抗')
    case 'AWD_PLUS':
      return t('competition.type_awd_plus', 'AWD+ 综合攻防')
    case 'ISW':
      return t('competition.type_isw', 'ISW 靶场竞技')
    default:
      return type || '-'
  }
}

function getTeamStatusLabel(status?: TeamStatus): string {
  if (status === 'AUDIT_PASSED') return '审核通过'
  if (status === 'PENDING_AUDIT' || status === 'AUDIT_DRAFT') return '报名审核中'
  if (status === 'AUDIT_REJECTED') return '审核未通过'
  if (status === 'DISBANDED') return '已解散'
  return '-'
}

function getTeamStatusBadgeClass(status?: TeamStatus): string {
  if (status === 'AUDIT_PASSED') {
    return 'bg-emerald-100 text-google-green dark:bg-emerald-950/60 dark:text-google-green-dark'
  }
  if (status === 'PENDING_AUDIT' || status === 'AUDIT_DRAFT') {
    return 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300'
  }
  if (status === 'AUDIT_REJECTED') {
    return 'bg-red-100 text-google-red dark:bg-red-950/60 dark:text-google-red-dark'
  }
  return 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300'
}

function formatDateTime(val?: string | null): string {
  if (!val) return '-'
  try {
    const d = new Date(val)
    if (isNaN(d.getTime())) return val
    const pad = (n: number) => (n < 10 ? '0' + n : String(n))
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}-${pad(d.getMinutes())}-${pad(d.getSeconds())}`
  } catch {
    return val
  }
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
  const now = Date.now()
  nowTimestamp.value = now

  const pad = (n: number) => (n < 10 ? '0' + n : String(n))

  if (competition.value.startTime) {
    const start = new Date(competition.value.startTime).getTime()
    const diffStart = Math.max(0, start - now)
    const secStart = Math.floor(diffStart / 1000)
    const hStart = Math.floor(secStart / 3600)
    const mStart = Math.floor((secStart % 3600) / 60)
    const sStart = secStart % 60
    countdownToStart.value = `${pad(hStart)}:${pad(mStart)}:${pad(sStart)}`
  }

  if (competition.value.endTime) {
    const end = new Date(competition.value.endTime).getTime()
    const diffEnd = Math.max(0, end - now)
    const secEnd = Math.floor(diffEnd / 1000)
    const hEnd = Math.floor(secEnd / 3600)
    const mEnd = Math.floor((secEnd % 3600) / 60)
    const sEnd = secEnd % 60
    countdownToEnd.value = `${pad(hEnd)}:${pad(mEnd)}:${pad(sEnd)}`
  }
}

function backToList() {
  router.push('/competitions')
}

function goToLogin() {
  const redirect = encodeURIComponent(route.fullPath)
  router.push(`/v1/signin/identifier?redirect=${redirect}`)
}

function openSignupModal() {
  teamFormModalMode.value = 'create'
  teamFormModalVisible.value = true
}

function openTeamDetailModal() {
  teamFormModalMode.value = 'view'
  teamFormModalVisible.value = true
}

function enterArena() {
  if (!competition.value) return
  const identifier = competition.value.slug || competition.value.id
  if (competition.value.type === 'THEORY') {
    const isShortLink = route.path.startsWith('/c/')
    router.push(isShortLink ? `/c/${identifier}/theory/require` : `/competitions/${identifier}/theory/require`)
  } else if (competition.value.type === 'CTF') {
    router.push(`/competitions/${identifier}/ctf`)
  } else {
    if (message) {
      message.info(`${getTypeLabel(competition.value.type)} 专属竞技场工作台正在联调中`)
    }
  }
}

async function fetchCompetitionDetail() {
  if (!competitionIdentifier.value) return
  loading.value = true
  try {
    const isNumeric = /^\d+$/.test(competitionIdentifier.value)
    let compData: CompetitionDetailVO | null = null

    if (isNumeric) {
      try {
        const res = await getCompetitionByIdApi(competitionIdentifier.value)
        compData = res?.data || null
      } catch {
        const slugRes = await getCompetitionBySlugApi(competitionIdentifier.value)
        compData = slugRes?.data || null
      }
    } else {
      const slugRes = await getCompetitionBySlugApi(competitionIdentifier.value)
      compData = slugRes?.data || null
    }

    if (!compData) {
      if (message) {
        message.error(t('competition.empty_desc', '未找到对应竞赛信息'))
      }
      return
    }

    competition.value = compData
    startCountdown()

    await Promise.all([
      fetchMyTeamStatus(compData.id),
      fetchMyInvitations(compData.id)
    ])
  } catch (err: any) {
    if (message) {
      message.error(err?.message || '获取竞赛详情失败')
    }
  } finally {
    loading.value = false
  }
}

async function fetchMyTeamStatus(compId: number | string) {
  if (!userStore.isLoggedIn) {
    myTeam.value = null
    return
  }
  try {
    const res = await getMyTeamApi(compId)
    myTeam.value = res?.data || null
  } catch {
    myTeam.value = null
  }
}

async function fetchMyInvitations(compId: number | string) {
  if (!userStore.isLoggedIn) {
    invitations.value = []
    return
  }
  try {
    const res = await getMyInvitationsApi(compId)
    invitations.value = res?.data || []
  } catch {
    invitations.value = []
  }
}

async function refreshMyTeam() {
  if (competition.value?.id) {
    await fetchMyTeamStatus(competition.value.id)
    await fetchMyInvitations(competition.value.id)
  }
}

async function handleTeamFormSuccess() {
  await refreshMyTeam()
}

async function handleTeamDisbanded() {
  myTeam.value = null
  await refreshMyTeam()
}

async function handleInvitationHandled() {
  await refreshMyTeam()
}

watch(
  () => competitionIdentifier.value,
  (newVal) => {
    if (newVal) {
      fetchCompetitionDetail()
    }
  }
)

watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (competition.value?.id) {
      if (loggedIn) {
        fetchMyTeamStatus(competition.value.id)
        fetchMyInvitations(competition.value.id)
      } else {
        myTeam.value = null
        invitations.value = []
      }
    }
  }
)

onMounted(() => {
  fetchCompetitionDetail()
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>

<style scoped lang="scss">
.volur-markdown-body :deep(h1),
.volur-markdown-body :deep(h2),
.volur-markdown-body :deep(h3),
.volur-markdown-body :deep(h4) {
  margin-top: 1rem;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: inherit;
}

.volur-markdown-body :deep(p) {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
  line-height: 1.6;
}

.volur-markdown-body :deep(ul),
.volur-markdown-body :deep(ol) {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
  padding-left: 1.5rem;
}

.volur-markdown-body :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  padding: 0.15rem 0.35rem;
  border-radius: 0.25rem;
  font-size: 0.85em;
  background-color: rgba(60, 64, 67, 0.08);
}

.volur-markdown-body :deep(pre) {
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
  background-color: rgba(60, 64, 67, 0.05);
}

html.dark .volur-markdown-body :deep(code),
html.dark .volur-markdown-body :deep(pre) {
  background-color: rgba(255, 255, 255, 0.08);
}

.progress-bar-primary {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  background-color: #1a73e8;
  animation: progress-primary 1.5s cubic-bezier(0.65, 0.815, 0.735, 0.395) infinite;
}

.progress-bar-secondary {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  background-color: #8ab4f8;
  animation: progress-secondary 1.5s cubic-bezier(0.165, 0.84, 0.44, 1) infinite;
  animation-delay: 0.5s;
}

@keyframes progress-primary {
  0% {
    left: -35%;
    right: 100%;
  }
  60% {
    left: 100%;
    right: -90%;
  }
  100% {
    left: 100%;
    right: -90%;
  }
}

@keyframes progress-secondary {
  0% {
    left: -200%;
    right: 100%;
  }
  60% {
    left: 107%;
    right: -8%;
  }
  100% {
    left: 107%;
    right: -8%;
  }
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 12px;
  border-radius: 10px;
  border: none;
  outline: none;
  background-color: transparent;
  color: #565b61;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.back-btn :deep(.n-icon) {
  color: inherit;
}

.back-btn:hover {
  background-color: #efefef;
  color: #1b1b1e;
}

html.dark .back-btn {
  color: #8c8c8c;
}

html.dark .back-btn:hover {
  background-color: #2a2a2a;
  color: #d4d4d4;
}

.comp-banner-box {
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
}

.comp-banner-img {
  width: 100%;
  height: auto;
  display: block;
  object-fit: contain;
}

.comp-time-val {
  font-family: Inter, sans-serif;
}

html.dark .comp-time-val {
  color: #FCFCFC;
}

.competition-btn {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  box-sizing: border-box !important;
  gap: 6px !important;
  height: 36px !important;
  padding: 0 12px !important;
  border-radius: 12px !important;
  border: 1px solid #d7d8da !important;
  color: #2b2d31 !important;
  font-family: Inter, sans-serif !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  line-height: 20px !important;
  letter-spacing: normal !important;
  cursor: pointer !important;
  user-select: none !important;
  white-space: nowrap !important;
  background-color: transparent !important;
  outline: none !important;
  box-shadow: none !important;
  transition: background-color 0.15s ease-in-out, border-color 0.15s ease-in-out, color 0.15s ease-in-out, box-shadow 0.1s ease-in-out !important;

  &:focus,
  &:active,
  &:focus-visible {
    outline: none !important;
    box-shadow: none !important;
  }

  :deep(.n-button__content) {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    font-family: Inter, sans-serif;
    font-size: 14px;
    font-weight: 500;
    line-height: 20px;
    letter-spacing: normal;
    color: inherit;
  }

  :deep(.n-button__border) {
    border-color: #d7d8da !important;
    border-radius: 12px !important;
    box-shadow: none !important;
  }

  :deep(.n-button__state-border) {
    display: none !important;
  }

  :deep(.n-base-wave) {
    display: none !important;
    animation: none !important;
  }

  &:hover {
    background-color: rgba(60, 64, 67, 0.04) !important;
  }

  &:focus,
  &:active {
    border-color: #d7d8da !important;
    :deep(.n-button__border) {
      border-color: #d7d8da !important;
      box-shadow: none !important;
    }
  }

  &.n-button {
    border: none !important;
  }

  html.dark & {
    border-color: #333 !important;
    color: #fcfcfc !important;

    &.n-button {
      border: none !important;
    }

    :deep(.n-button__border) {
      border-color: #333 !important;
    }

    &:hover {
      background-color: rgba(255, 255, 255, 0.04) !important;
    }

    &:focus,
    &:active {
      border-color: #333 !important;
      :deep(.n-button__border) {
        border-color: #333 !important;
        box-shadow: none !important;
      }
    }
  }
}

.competition-btn-info {
  border-color: rgba(26, 115, 232, 0.3) !important;
  color: #1a73e8 !important;
  background-color: rgba(26, 115, 232, 0.05) !important;

  &:hover {
    background-color: rgba(26, 115, 232, 0.1) !important;
    border-color: rgba(26, 115, 232, 0.45) !important;
  }

  &:focus,
  &:active {
    border-color: rgba(26, 115, 232, 0.45) !important;
  }

  html.dark & {
    border-color: rgba(138, 180, 248, 0.3) !important;
    color: #8ab4f8 !important;
    background-color: rgba(138, 180, 248, 0.08) !important;

    &:hover {
      background-color: rgba(138, 180, 248, 0.16) !important;
      border-color: rgba(138, 180, 248, 0.45) !important;
    }

    &:focus,
    &:active {
      border-color: rgba(138, 180, 248, 0.45) !important;
    }
  }
}
</style>
