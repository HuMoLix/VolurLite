<template>
  <CompetitionLayout
    :competition="competition"
    mode="theory"
    :title="competition?.title"
    :start-time="competition?.startTime"
    :end-time="competition?.endTime"
    :default-title="t('competition.type_theory', '理论知识赛')"
    :warning-threshold-minutes="15"
  >
    <div
      v-if="loading"
      class="fixed top-0 left-0 right-0 h-1 z-50 overflow-hidden bg-blue-100 dark:bg-blue-950"
    >
      <div class="progress-bar-primary" />
      <div class="progress-bar-secondary" />
    </div>

    <div class="max-w-4xl mx-auto w-full px-4 sm:px-6 py-8 space-y-6 flex-1 flex flex-col justify-center">
      <section class="rounded-2xl sm:rounded-3xl p-6 sm:p-8 bg-white dark:bg-google-surface-darkCard border border-google-border-light dark:border-google-border-dark shadow-sm transition-colors space-y-6">
        <div class="flex items-start gap-4">
          <div class="p-3 rounded-2xl bg-blue-50 dark:bg-blue-950/60 text-google-blue dark:text-google-blue-dark border border-blue-100 dark:border-blue-900/50">
            <n-icon :size="28" :component="ShieldCheckmarkOutline" />
          </div>
          <div class="space-y-1">
            <h1 class="text-xl sm:text-2xl font-bold text-google-text-primary dark:text-google-text-darkPrimary">
              {{ t('competition.proctor_precheck_title', '理论知识竞赛考场准入检查') }}
            </h1>
            <p class="text-xs sm:text-sm text-google-text-secondary dark:text-google-text-darkSecondary leading-relaxed">
              {{ t('competition.proctor_precheck_desc', '根据线上考试公平公正规则，本场考试全程采用浏览器强反作弊实时监考。在获取正式试卷前，请确保您的终端满足以下安全检查要求。') }}
            </p>
          </div>
        </div>

        <div class="space-y-4">
          <div
            class="p-4 sm:p-5 rounded-2xl border transition-all"
            :class="[
              isFullscreen
                ? 'bg-emerald-50/50 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-900/50'
                : 'bg-google-surface-light dark:bg-google-surface-dark border-google-border-light dark:border-google-border-dark'
            ]"
          >
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div class="flex items-start gap-3">
                <div
                  class="p-2 rounded-xl text-base"
                  :class="[
                    isFullscreen
                      ? 'bg-emerald-100 dark:bg-emerald-900/50 text-google-green dark:text-google-green-dark'
                      : 'bg-gray-100 dark:bg-gray-800 text-google-text-muted dark:text-google-text-darkMuted'
                  ]"
                >
                  <n-icon :size="20" :component="isFullscreen ? CheckmarkCircleOutline : ExpandOutline" />
                </div>
                <div>
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-bold text-google-text-primary dark:text-google-text-darkPrimary">
                      {{ t('competition.proctor_fullscreen_title', '全屏模式检测') }}
                    </span>
                    <span
                      class="px-2 py-0.5 rounded text-[11px] font-medium"
                      :class="[
                        isFullscreen
                          ? 'bg-emerald-100 text-google-green dark:bg-emerald-950/80 dark:text-google-green-dark'
                          : 'bg-amber-100 text-amber-800 dark:bg-amber-950/80 dark:text-amber-300'
                      ]"
                    >
                      {{ isFullscreen ? t('competition.proctor_status_ready', '已就绪') : t('competition.proctor_status_required', '必须开启') }}
                    </span>
                  </div>
                  <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary mt-1">
                    {{ t('competition.proctor_fullscreen_hint', '考试期间全程必须处于全屏状态，退出全屏将被记作违规事件。') }}
                  </p>
                </div>
              </div>

              <VBtn
                :clz="isFullscreen ? 'normal' : 'primary'"
                size="medium"
                :left-icon="isFullscreen ? CheckmarkCircleOutline : ExpandOutline"
                @click="toggleFullscreen"
              >
                {{ isFullscreen ? t('competition.proctor_fullscreen_active', '保持全屏') : t('competition.proctor_fullscreen_btn', '进入全屏模式') }}
              </VBtn>
            </div>
          </div>

          <div
            class="p-4 sm:p-5 rounded-2xl border transition-all"
            :class="[
              isScreenSharingActive
                ? 'bg-emerald-50/50 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-900/50'
                : 'bg-google-surface-light dark:bg-google-surface-dark border-google-border-light dark:border-google-border-dark'
            ]"
          >
            <div class="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
              <div class="flex items-start gap-3 flex-1">
                <div
                  class="p-2 rounded-xl text-base"
                  :class="[
                    isScreenSharingActive
                      ? 'bg-emerald-100 dark:bg-emerald-900/50 text-google-green dark:text-google-green-dark'
                      : 'bg-gray-100 dark:bg-gray-800 text-google-text-muted dark:text-google-text-darkMuted'
                  ]"
                >
                  <n-icon :size="20" :component="isScreenSharingActive ? CheckmarkCircleOutline : DesktopOutline" />
                </div>
                <div class="flex-1 space-y-2">
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-bold text-google-text-primary dark:text-google-text-darkPrimary">
                      {{ t('competition.proctor_screen_title', '整块屏幕监控共享') }}
                    </span>
                    <span
                      class="px-2 py-0.5 rounded text-[11px] font-medium"
                      :class="[
                        isScreenSharingActive
                          ? 'bg-emerald-100 text-google-green dark:bg-emerald-950/80 dark:text-google-green-dark'
                          : 'bg-amber-100 text-amber-800 dark:bg-amber-950/80 dark:text-amber-300'
                      ]"
                    >
                      {{ isScreenSharingActive ? t('competition.proctor_screen_ready', '监控已连接') : t('competition.proctor_status_required', '必须开启') }}
                    </span>
                  </div>
                  <p class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary leading-relaxed">
                    {{ t('competition.proctor_screen_hint', '点击下方按钮授权屏幕共享。系统强制要求选择【整个屏幕 (Entire Screen)】，严禁选择单窗口或单个标签页，否则将被拒绝入场。') }}
                  </p>

                  <div
                    v-if="screenErrorMsg"
                    class="p-3 rounded-xl bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-900/50 text-xs text-google-red dark:text-google-red-dark flex items-center gap-2"
                  >
                    <n-icon :size="16" :component="AlertCircleOutline" />
                    <span>{{ screenErrorMsg }}</span>
                  </div>

                  <div
                    v-if="isScreenSharingActive"
                    class="mt-3 overflow-hidden rounded-xl border border-google-border-light dark:border-google-border-dark bg-black/90 aspect-video max-w-sm"
                  >
                    <video
                      ref="previewVideoRef"
                      autoplay
                      playsinline
                      muted
                      class="w-full h-full object-contain"
                    />
                  </div>
                </div>
              </div>

              <VBtn
                :clz="isScreenSharingActive ? 'normal' : 'primary'"
                size="medium"
                :left-icon="DesktopOutline"
                @click="requestScreenShare"
              >
                {{ isScreenSharingActive ? t('competition.proctor_screen_reselect', '重新授权屏幕') : t('competition.proctor_screen_btn', '授权开启全屏幕共享') }}
              </VBtn>
            </div>
          </div>

          <div class="p-4 sm:p-5 rounded-2xl bg-google-surface-light dark:bg-google-surface-dark border border-google-border-light dark:border-google-border-dark space-y-3">
            <div class="flex items-center gap-2 text-google-yellow dark:text-google-yellow-dark">
              <n-icon :size="18" :component="WarningOutline" />
              <span class="text-xs font-bold">{{ t('competition.proctor_warning_badge', '考场纪律与反作弊声明') }}</span>
            </div>
            <ul class="text-xs text-google-text-secondary dark:text-google-text-darkSecondary space-y-1.5 list-disc pl-5 leading-relaxed">
              <li>{{ t('competition.proctor_rule_1', '答题全程必须保持全屏与屏幕共享监控流处于活跃状态。') }}</li>
              <li>{{ t('competition.proctor_rule_2', '严禁切换标签页、最小化窗口、使用 Alt+Tab 切换应用或打开开发者工具。') }}</li>
              <li>{{ t('competition.proctor_rule_3', '严禁尝试使用快捷键复制粘贴试题或查看网页源码。') }}</li>
              <li>{{ t('competition.proctor_rule_4', '系统具备后台心跳时序检验与违规累计计数，违规次数达到上限后将自动触发永久封禁并作废试卷成绩。') }}</li>
            </ul>

            <label class="flex items-center gap-2 pt-2 cursor-pointer select-none">
              <input
                v-model="isPledged"
                type="checkbox"
                class="w-4 h-4 rounded border-gray-300 text-google-blue focus:ring-google-blue cursor-pointer"
              >
              <span class="text-xs font-medium text-google-text-primary dark:text-google-text-darkPrimary">
                {{ t('competition.proctor_pledge_check', '我已清楚理解以上所有考场纪律，承诺诚信应考并接受全程反作弊监考') }}
              </span>
            </label>
          </div>
        </div>

        <div class="pt-2 flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-t border-google-border-light dark:border-google-border-dark">
          <div class="text-xs text-google-text-muted dark:text-google-text-darkMuted">
            <span v-if="!isFullscreen || !isScreenSharingActive || !isPledged">
              {{ t('competition.proctor_incomplete_tip', '请完成全屏切换、屏幕共享授权并勾选诚信承诺后进入考场') }}
            </span>
            <span v-else class="text-google-green dark:text-google-green-dark font-medium">
              {{ t('competition.proctor_complete_tip', '所有安全检查项已满足，可以进入考场开始答题') }}
            </span>
          </div>

          <div class="flex items-center gap-3">
            <VBtn
              clz="normal"
              size="large"
              :left-icon="ArrowBackOutline"
              @click="handleBackToDetail"
            >
              {{ t('common.cancel', '返回') }}
            </VBtn>

            <VBtn
              clz="primary"
              size="large"
              :left-icon="ShieldCheckmarkOutline"
              :disabled="!canProceed"
              :loading="verifying"
              @click="handleProceedToExam"
            >
              {{ t('competition.proctor_enter_btn', '环境验证并通过，进入考场') }}
            </VBtn>
          </div>
        </div>
      </section>
    </div>
  </CompetitionLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon, useMessage } from 'naive-ui'
import {
  ShieldCheckmarkOutline,
  ExpandOutline,
  DesktopOutline,
  CheckmarkCircleOutline,
  AlertCircleOutline,
  WarningOutline,
  ArrowBackOutline
} from '@vicons/ionicons5'
import { useUserStore } from '@/store/modules/user'
import { useProctorStore } from '@/store/modules/proctor'
import CompetitionLayout from '@/components/layouts/CompetitionLayout.vue'
import { VBtn } from '@/components/common'
import {
  getCompetitionByIdApi,
  getCompetitionBySlugApi,
  getMyTeamApi,
  registerProctorReadyApi
} from '@/api/competition'
import type { CompetitionDetailVO } from '@/types/competition'

defineOptions({
  name: 'TheoryCompetitionRequireView'
})

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const message = useMessage()
const userStore = useUserStore()
const proctorStore = useProctorStore()

const loading = ref<boolean>(false)
const verifying = ref<boolean>(false)
const competition = ref<CompetitionDetailVO | null>(null)

const isFullscreen = ref<boolean>(false)
const isScreenSharingActive = ref<boolean>(false)
const screenErrorMsg = ref<string>('')
const isPledged = ref<boolean>(false)
const previewVideoRef = ref<HTMLVideoElement | null>(null)
let localStream: MediaStream | null = null

const competitionIdentifier = computed(() => {
  return String(route.params.slug || route.params.id || route.query.competitionId || '')
})

const canProceed = computed(() => {
  return isFullscreen.value && isScreenSharingActive.value && isPledged.value && !verifying.value
})

function checkFullscreenState() {
  isFullscreen.value = Boolean(document.fullscreenElement)
}

async function toggleFullscreen() {
  if (document.fullscreenElement) {
    try {
      await document.exitFullscreen()
    } catch {}
  } else {
    try {
      await document.documentElement.requestFullscreen()
    } catch {
      if (message) {
        message.warning(t('competition.proctor_fullscreen_denied', '无法自动进入全屏，请在浏览器权限设置中允许全屏'))
      }
    }
  }
  checkFullscreenState()
}

async function requestScreenShare() {
  screenErrorMsg.value = ''
  if (!navigator.mediaDevices || typeof navigator.mediaDevices.getDisplayMedia !== 'function') {
    screenErrorMsg.value = t('competition.proctor_screen_unsupported', '当前浏览器不支持屏幕捕获 API，请使用现代 Chrome、Edge 或 Firefox 浏览器')
    return
  }

  try {
    const stream = await navigator.mediaDevices.getDisplayMedia({
      video: { displaySurface: 'monitor' },
      audio: false
    })

    const videoTrack = stream.getVideoTracks()[0]
    if (!videoTrack) {
      throw new Error(t('competition.proctor_track_not_found', '未能成功获取屏幕视频轨道'))
    }

    const settings: any = videoTrack.getSettings ? videoTrack.getSettings() : {}
    if (settings.displaySurface && settings.displaySurface !== 'monitor') {
      videoTrack.stop()
      stream.getTracks().forEach((track) => track.stop())
      screenErrorMsg.value = t('competition.proctor_must_entire_screen', '系统检测到您选择了【窗口】或【标签页】共享。考场强制要求共享【整块屏幕 (Entire Screen)】，请重新选择！')
      if (message) {
        message.error(screenErrorMsg.value)
      }
      isScreenSharingActive.value = false
      return
    }

    if (localStream) {
      localStream.getTracks().forEach((track) => track.stop())
    }
    localStream = stream
    isScreenSharingActive.value = true

    videoTrack.onended = () => {
      isScreenSharingActive.value = false
      localStream = null
      if (message) {
        message.warning(t('competition.proctor_stream_disconnected', '屏幕共享流已断开，请重新授权共享'))
      }
    }

    await nextTick()
    if (previewVideoRef.value) {
      previewVideoRef.value.srcObject = stream
      previewVideoRef.value.play().catch(() => {})
    }
  } catch (err: any) {
    if (err?.name !== 'NotAllowedError') {
      screenErrorMsg.value = err?.message || t('competition.proctor_screen_failed', '屏幕共享授权失败')
      if (message) {
        message.error(screenErrorMsg.value)
      }
    }
  }
}

function handleBackToDetail() {
  if (localStream) {
    localStream.getTracks().forEach((track) => track.stop())
    localStream = null
  }
  const isShortLink = route.path.startsWith('/c/')
  const slug = competition.value?.slug || competition.value?.id || competitionIdentifier.value
  router.push(isShortLink ? `/c/${slug}` : `/competitions/${slug}`)
}

async function handleProceedToExam() {
  if (!canProceed.value || !competition.value?.id || !localStream) return

  checkFullscreenState()
  if (!isFullscreen.value) {
    if (message) {
      message.warning(t('competition.proctor_require_fullscreen', '必须处于全屏状态方可进入考场'))
    }
    await toggleFullscreen()
    return
  }

  const liveTrack = localStream.getVideoTracks().find((t) => t.readyState === 'live')
  if (!liveTrack) {
    isScreenSharingActive.value = false
    screenErrorMsg.value = t('competition.proctor_stream_invalid', '屏幕监控流已失效，请重新授权')
    return
  }

  verifying.value = true
  try {
    await registerProctorReadyApi(competition.value.id)
    proctorStore.setProctorReady(competition.value.id, localStream)

    const isShortLink = route.path.startsWith('/c/')
    const slug = competition.value.slug || competition.value.id
    const targetPath = isShortLink ? `/c/${slug}/theory` : `/competitions/${slug}/theory`
    router.replace(targetPath)
  } catch (err: any) {
    if (message) {
      message.error(err?.response?.data?.message || err?.message || t('competition.proctor_verify_failed', '准入验证失败，请重试'))
    }
  } finally {
    verifying.value = false
  }
}

async function fetchCompetition() {
  if (!competitionIdentifier.value) return

  if (!userStore.isLoggedIn) {
    router.replace(`/v1/signin/identifier?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

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

    let currentTeam: any = null
    try {
      const teamRes = await getMyTeamApi(compData.id)
      currentTeam = teamRes?.data || null
    } catch {
      currentTeam = null
    }

    if (currentTeam && currentTeam.status === 'ENDED' && !userStore.isAdmin) {
      if (message) {
        message.warning(t('competition.theory_exam_ended_tip', '您的战队答题已结束，无法查看试卷题目'))
      }
      handleBackToDetail()
      return
    }

    const isAuthorized = Boolean(userStore.isAdmin) || (currentTeam !== null && currentTeam.status === 'AUDIT_PASSED')
    if (!isAuthorized) {
      if (message) {
        message.error(t('competition.not_participating', '您尚未加入已审核通过的参赛队伍，无法进入考场'))
      }
      handleBackToDetail()
      return
    }
  } catch (err: any) {
    if (message) {
      message.error(err?.message || t('competition.fetch_failed', '获取竞赛数据失败'))
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCompetition()
  checkFullscreenState()
  document.addEventListener('fullscreenchange', checkFullscreenState)
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', checkFullscreenState)
  if (!proctorStore.isPrecheckPassed && localStream) {
    localStream.getTracks().forEach((track) => track.stop())
    localStream = null
  }
})
</script>

<style scoped lang="scss">
.progress-bar-primary {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 100%;
  transform-origin: top left;
  background-color: #1a73e8;
  animation: mdc-linear-progress-primary-indeterminate-translate 2s linear infinite;
}

.progress-bar-secondary {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 100%;
  transform-origin: top left;
  background-color: #1a73e8;
  animation: mdc-linear-progress-secondary-indeterminate-translate 2s linear infinite;
}

@keyframes mdc-linear-progress-primary-indeterminate-translate {
  0% {
    transform: translateX(-145.166611%) scaleX(0.5);
  }
  20% {
    transform: translateX(-54.526915%) scaleX(0.798733);
  }
  59.15% {
    transform: translateX(95.443374%) scaleX(0.384373);
  }
  100% {
    transform: translateX(366.852433%) scaleX(0.01);
  }
}

@keyframes mdc-linear-progress-secondary-indeterminate-translate {
  0% {
    transform: translateX(-54.888891%) scaleX(0.1);
  }
  25% {
    transform: translateX(-17.236978%) scaleX(0.536345);
  }
  48.35% {
    transform: translateX(29.49714%) scaleX(0.741079);
  }
  100% {
    transform: translateX(107.384235%) scaleX(0.01);
  }
}
</style>
