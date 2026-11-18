<template>
  <n-modal
    :show="show"
    preset="card"
    closable
    class="training-detail-modal w-full"
    :style="{ width: '760px', maxWidth: 'calc(100vw - 32px)', height: '600px' }"
    content-class="flex-1 min-h-0 flex flex-col"
    content-style="flex: 1 1 0%; min-height: 0; display: flex; flex-direction: column;"
    @update:show="handleClose"
    @close="handleClose(false)"
  >
    <template #header>
      <div class="min-w-0 pr-2">
        <div class="flex items-center gap-2.5 flex-wrap mb-2">
          <span class="category-tile" :class="`tone-${categoryTone}`">
            <n-icon :size="16" :component="categoryIcon" />
          </span>
          <span class="header-category">{{ challenge?.categoryName || t('training.category_misc', '杂项') }}</span>
          <span class="difficulty-pill" :class="difficultyClass">
            <i class="difficulty-dot" />
            {{ difficultyLabel }}
          </span>
          <span class="points-pill">
            {{ challenge?.points ?? 0 }}
            <small>pts</small>
          </span>
          <span class="header-solves">
            <n-icon :size="13" :component="PeopleOutline" />
            {{ challenge?.solvedCount ?? 0 }} {{ t('training.solves_suffix', '人解出') }}
          </span>
        </div>
        <h2 class="detail-title">
          {{ challenge?.name || t('training.detail_title', '题目详情') }}
        </h2>
      </div>
    </template>

    <div v-if="loading" class="modal-loading">
      <n-spin size="large" />
    </div>

    <div v-else-if="challenge" class="detail-body">
      <section class="detail-section">
        <div class="section-label">
          {{ t('training.description', '题目描述') }}
        </div>
        <div
          v-if="challenge.description"
          class="volur-markdown-body detail-prose"
          v-html="renderedDescription"
        />
        <div v-else class="section-empty">
          {{ t('training.no_description', '暂无题目详细描述。') }}
        </div>
      </section>

      <section v-if="attachments.length" class="detail-section">
        <div class="section-label">
          {{ t('training.attachments', '附件下载') }}
        </div>
        <div class="attachment-list">
          <a
            v-for="file in attachments"
            :key="file.url"
            :href="file.url"
            download
            class="attachment-link"
          >
            <n-icon :size="15" :component="CloudDownloadOutline" class="shrink-0" />
            <span class="truncate">{{ file.name || t('training.download_attachment', '下载附件') }}</span>
          </a>
        </div>
      </section>

      <section v-if="challenge.dynamicContainer" class="detail-section">
        <div class="section-label">
          {{ t('training.instance_title', '动态靶机环境') }}
        </div>

        <div v-if="activeInstance" class="instance-panel">
          <div class="instance-address">
            {{ instanceAddress }}
          </div>
          <div class="instance-foot">
            <span class="instance-remaining">
              <n-icon :size="13" :component="TimeOutline" />
              {{ t('training.instance_remaining', '剩余时间') }}: {{ formatRemainingSeconds(remainingSeconds) }}
            </span>
            <div class="flex items-center gap-2">
              <VBtn
                clz="primary"
                size="small"
                :disabled="instanceActionLoading"
                @click="handleExtendInstance"
              >
                {{ t('training.instance_extend', '延长 15 分钟') }}
              </VBtn>
              <VBtn
                clz="warning"
                size="small"
                :disabled="instanceActionLoading"
                @click="handleStopInstance"
              >
                {{ t('training.instance_stop', '销毁靶机') }}
              </VBtn>
            </div>
          </div>
        </div>

        <button
          v-else
          type="button"
          class="instance-start"
          :disabled="instanceActionLoading"
          @click="handleStartInstance"
        >
          <n-icon :size="16" :component="RocketOutline" :class="{ 'animate-spin': instanceActionLoading }" />
          <span>{{ instanceActionLoading ? t('training.instance_starting', '靶机调度启动中...') : t('training.instance_start', '启动靶机') }}</span>
        </button>
      </section>
    </div>

    <div class="flag-dock">
      <div class="section-label">
        {{ t('training.flag_submission', '提交 Flag') }}
      </div>

      <div
        v-if="challenge?.solvedByMe"
        class="solved-banner"
      >
        <n-icon :size="16" :component="CheckmarkCircle" />
        <span>{{ t('training.solved_tip_modal', '你已解出该题，积分已计入训练总榜') }}</span>
      </div>

      <div v-else class="flag-row">
        <VInput
          v-model:value="flagInput"
          :placeholder="t('training.flag_placeholder', 'flag{...}')"
          clearable
          size="large"
          :radius="12"
          class="flex-1 min-w-0"
          @keydown.enter="handleFlagSubmit"
        />
        <VBtn
          clz="primary"
          :disabled="flagSubmitting || !flagInput.trim()"
          :loading="flagSubmitting"
          class="flex-shrink-0"
          @click="handleFlagSubmit"
        >
          {{ flagSubmitting ? t('training.flag_submitting', '校验中...') : t('training.flag_submit', '提交 Flag') }}
        </VBtn>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { computed, ref, watch, onUnmounted } from 'vue'
import type { Component } from 'vue'
import { useI18n } from 'vue-i18n'
import { NIcon, NModal, NSpin, useMessage } from 'naive-ui'
import {
  RocketOutline,
  CheckmarkCircle,
  CloudDownloadOutline,
  TimeOutline,
  PeopleOutline,
  GlobeOutline,
  TerminalOutline,
  GitBranchOutline,
  KeyOutline,
  ExtensionPuzzleOutline,
  FolderOutline
} from '@vicons/ionicons5'
import { VBtn, VInput } from '@/components/common'
import {
  extendInstanceApi,
  startInstanceApi,
  stopInstanceApi
} from '@/api/container'
import {
  getTrainingInstanceApi,
  submitTrainingFlagApi
} from '@/api/training'
import { renderMarkdown } from '@/utils/markdown'
import type { ChallengeInstanceResponseVO } from '@/types/container'
import type { TrainingChallengeDetailVO } from '@/types/training'

defineOptions({
  name: 'TrainingChallengeModal'
})

const props = withDefaults(
  defineProps<{
    show: boolean
    challenge: TrainingChallengeDetailVO | null
    loading?: boolean
  }>(),
  {
    loading: false
  }
)

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'solved', payload: { challengeId: number | string; points: number; totalPoints: number }): void
}>()

const { t } = useI18n()
const message = useMessage()

const flagInput = ref('')
const flagSubmitting = ref(false)
const activeInstance = ref<ChallengeInstanceResponseVO | null>(null)
const instanceActionLoading = ref(false)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const attachments = computed(() => props.challenge?.attachments || [])

const renderedDescription = computed(() => renderMarkdown(props.challenge?.description))

const categoryTone = computed<string>(() => {
  const normalized = (props.challenge?.categoryName || '').toLowerCase()
  if (normalized.includes('web')) return 'blue'
  if (normalized.includes('pwn') || normalized.includes('binary')) return 'red'
  if (normalized.includes('reverse') || normalized.includes('re')) return 'purple'
  if (normalized.includes('crypto')) return 'amber'
  if (normalized.includes('misc')) return 'green'
  return 'slate'
})

const categoryIcon = computed<Component>(() => {
  const normalized = (props.challenge?.categoryName || '').toLowerCase()
  if (normalized.includes('web')) return GlobeOutline
  if (normalized.includes('pwn') || normalized.includes('binary')) return TerminalOutline
  if (normalized.includes('reverse') || normalized.includes('re')) return GitBranchOutline
  if (normalized.includes('crypto')) return KeyOutline
  if (normalized.includes('misc')) return ExtensionPuzzleOutline
  return FolderOutline
})

const difficultyMeta = computed<{ label: string; cls: string }>(() => {
  const raw = String(props.challenge?.difficulty || '').toLowerCase()
  switch (raw) {
    case 'easy':
      return { label: t('training.difficulty_easy', '入门'), cls: 'is-easy' }
    case 'normal':
      return { label: t('training.difficulty_normal', '简单'), cls: 'is-normal' }
    case 'mid':
      return { label: t('training.difficulty_mid', '中等'), cls: 'is-mid' }
    case 'hard':
      return { label: t('training.difficulty_hard', '进阶'), cls: 'is-hard' }
    case 'difficult':
      return { label: t('training.difficulty_difficult', '困难'), cls: 'is-difficult' }
    default:
      return { label: t('training.difficulty_unknown', '未分级'), cls: 'is-unknown' }
  }
})

const difficultyLabel = computed<string>(() => difficultyMeta.value.label)
const difficultyClass = computed<string>(() => difficultyMeta.value.cls)

const instanceAddress = computed<string>(() => {
  if (!activeInstance.value) return ''
  return activeInstance.value.accessUrl || ''
})

const remainingSeconds = computed<number>(() => activeInstance.value?.remainingTimeSeconds ?? 0)

function formatRemainingSeconds(seconds: number): string {
  const safe = Math.max(0, seconds)
  const m = Math.floor(safe / 60)
  const s = safe % 60
  return `${m}m ${s < 10 ? '0' : ''}${s}s`
}

function handleClose(value: boolean) {
  emit('update:show', value)
}

function startCountdown() {
  clearCountdown()
  countdownTimer = setInterval(() => {
    if (!activeInstance.value) return
    if (activeInstance.value.remainingTimeSeconds > 0) {
      activeInstance.value.remainingTimeSeconds -= 1
    } else {
      activeInstance.value = null
      clearCountdown()
    }
  }, 1000)
}

function clearCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

async function syncInstance() {
  clearCountdown()
  activeInstance.value = null
  if (!props.challenge?.dynamicContainer) return

  try {
    const res = await getTrainingInstanceApi(props.challenge.id, true)
    if (res?.data && (res.data.remainingTimeSeconds ?? 0) > 0) {
      activeInstance.value = res.data
      startCountdown()
    }
  } catch {
    // 没有运行中的实例属于正常状态
  }
}

watch(
  () => props.show,
  (visible) => {
    if (visible) {
      flagInput.value = ''
      syncInstance()
    } else {
      clearCountdown()
    }
  }
)

watch(
  () => props.challenge?.id,
  () => {
    if (props.show) {
      flagInput.value = ''
      syncInstance()
    }
  }
)

async function handleFlagSubmit() {
  if (!props.challenge || flagSubmitting.value) return
  const trimmed = flagInput.value.trim()
  if (!trimmed) {
    message.warning(t('training.flag_empty', '请输入 Flag'))
    return
  }

  flagSubmitting.value = true
  try {
    const res = await submitTrainingFlagApi(props.challenge.id, trimmed)
    const data = res?.data
    if (!data) return

    if (data.result === 'CORRECT') {
      message.success(
        `${t('training.flag_correct_prefix', '回答正确，获得')} ${data.pointsEarned} ${t('training.points_unit', '分')}`
      )
      props.challenge.solvedByMe = true
      props.challenge.solvedCount += 1
      flagInput.value = ''
      emit('solved', {
        challengeId: props.challenge.id,
        points: data.pointsEarned,
        totalPoints: data.totalPoints
      })
    } else if (data.result === 'DUPLICATE') {
      message.info(t('training.flag_duplicate', '该题你已解出过，无需重复提交'))
      props.challenge.solvedByMe = true
      emit('solved', {
        challengeId: props.challenge.id,
        points: 0,
        totalPoints: data.totalPoints
      })
    } else {
      message.error(t('training.flag_wrong', 'Flag 不正确，请再试一次'))
    }
  } catch {
    // 全局拦截器已提示错误
  } finally {
    flagSubmitting.value = false
  }
}

async function handleStartInstance() {
  if (!props.challenge || instanceActionLoading.value) return
  instanceActionLoading.value = true
  try {
    const res = await startInstanceApi(props.challenge.sourceChallengeId)
    if (res?.data) {
      activeInstance.value = res.data
      startCountdown()
      message.success(res.message || t('training.instance_started', '靶机已启动'))
    }
  } catch {
    // 全局拦截器已提示错误
  } finally {
    instanceActionLoading.value = false
  }
}

async function handleExtendInstance() {
  if (!props.challenge || !activeInstance.value || instanceActionLoading.value) return
  instanceActionLoading.value = true
  try {
    const res = await extendInstanceApi(props.challenge.sourceChallengeId)
    if (res?.data) {
      activeInstance.value = res.data
      message.success(res.message || t('training.instance_extended', '靶机运行时间已延长 15 分钟'))
    }
  } catch {
    // 全局拦截器已提示错误
  } finally {
    instanceActionLoading.value = false
  }
}

async function handleStopInstance() {
  if (!props.challenge || !activeInstance.value || instanceActionLoading.value) return
  instanceActionLoading.value = true
  try {
    const res = await stopInstanceApi(props.challenge.sourceChallengeId)
    clearCountdown()
    activeInstance.value = null
    message.success(res.message || t('training.instance_stopped', '靶机已销毁'))
  } catch {
    // 全局拦截器已提示错误
  } finally {
    instanceActionLoading.value = false
  }
}

onUnmounted(() => {
  clearCountdown()
})
</script>

<style scoped lang="scss">
/* ------------------------------------------------------------------ */
/* Header                                                              */
/* ------------------------------------------------------------------ */

.category-tile {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 9px;
  flex-shrink: 0;

  &.tone-blue {
    color: #1a73e8;
    background: rgba(26, 115, 232, 0.1);
  }

  &.tone-red {
    color: #d93025;
    background: rgba(217, 48, 37, 0.09);
  }

  &.tone-purple {
    color: #8e44d9;
    background: rgba(142, 68, 217, 0.09);
  }

  &.tone-amber {
    color: #b06000;
    background: rgba(251, 188, 4, 0.14);
  }

  &.tone-green {
    color: #1e8e3e;
    background: rgba(52, 168, 83, 0.1);
  }

  &.tone-slate {
    color: #5f6368;
    background: rgba(95, 99, 104, 0.08);
  }
}

html.dark .category-tile {
  &.tone-blue {
    color: #a8c7fa;
    background: rgba(168, 199, 250, 0.12);
  }

  &.tone-red {
    color: #f28b82;
    background: rgba(242, 139, 130, 0.12);
  }

  &.tone-purple {
    color: #d0a8ff;
    background: rgba(208, 168, 255, 0.12);
  }

  &.tone-amber {
    color: #fdd663;
    background: rgba(253, 214, 99, 0.12);
  }

  &.tone-green {
    color: #81c995;
    background: rgba(129, 201, 149, 0.12);
  }

  &.tone-slate {
    color: #9aa0a6;
    background: rgba(154, 160, 166, 0.12);
  }
}

.header-category {
  font-size: 12px;
  font-weight: 600;
  color: #5f6368;
}

html.dark .header-category {
  color: #9aa0a6;
}

.difficulty-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;

  .difficulty-dot {
    width: 5px;
    height: 5px;
    border-radius: 9999px;
    background: currentColor;
  }

  &.is-easy {
    color: #1e8e3e;
    background: rgba(52, 168, 83, 0.1);
  }

  &.is-normal {
    color: #1a73e8;
    background: rgba(26, 115, 232, 0.09);
  }

  &.is-mid {
    color: #b06000;
    background: rgba(251, 188, 4, 0.14);
  }

  &.is-hard {
    color: #c5520a;
    background: rgba(234, 134, 53, 0.12);
  }

  &.is-difficult {
    color: #c5221f;
    background: rgba(234, 67, 53, 0.1);
  }

  &.is-unknown {
    color: #5f6368;
    background: rgba(95, 99, 104, 0.08);
  }
}

html.dark .difficulty-pill {
  &.is-easy {
    color: #81c995;
    background: rgba(129, 201, 149, 0.12);
  }

  &.is-normal {
    color: #a8c7fa;
    background: rgba(168, 199, 250, 0.12);
  }

  &.is-mid {
    color: #fdd663;
    background: rgba(253, 214, 99, 0.12);
  }

  &.is-hard {
    color: #fcad70;
    background: rgba(252, 173, 112, 0.12);
  }

  &.is-difficult {
    color: #f28b82;
    background: rgba(242, 139, 130, 0.12);
  }

  &.is-unknown {
    color: #9aa0a6;
    background: rgba(154, 160, 166, 0.12);
  }
}

.points-pill {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  padding: 3px 9px;
  border-radius: 9999px;
  font-family: 'Fira Code', ui-monospace, monospace;
  font-size: 12px;
  font-weight: 700;
  line-height: 17px;
  color: #1a73e8;
  background: #eef4ff;

  small {
    font-size: 9px;
    font-weight: 500;
    opacity: 0.7;
  }
}

html.dark .points-pill {
  color: #a8c7fa;
  background: rgba(168, 199, 250, 0.12);
}

.header-solves {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #80868b;
}

html.dark .header-solves {
  color: #9aa0a6;
}

.detail-title {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  line-height: 27px;
  letter-spacing: -0.01em;
  color: #202124;
  word-break: break-word;
}

html.dark .detail-title {
  color: #e8eaed;
}

/* ------------------------------------------------------------------ */
/* Body                                                                */
/* ------------------------------------------------------------------ */

.modal-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.detail-section {
  padding-bottom: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid #f0f2f4;
}

.detail-section:last-child {
  padding-bottom: 4px;
  margin-bottom: 0;
  border-bottom: none;
}

html.dark .detail-section {
  border-bottom-color: #26272a;
}

.section-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.09em;
  text-transform: uppercase;
  color: #9aa0a6;
  margin-bottom: 10px;
}

.section-empty {
  font-size: 12px;
  color: #80868b;
}

html.dark .section-empty {
  color: #9aa0a6;
}

.detail-prose {
  font-size: 13px;
  line-height: 1.8;
  color: #3c4043;
}

html.dark .detail-prose {
  color: #bdc1c6;
}

.detail-prose :deep(h1),
.detail-prose :deep(h2),
.detail-prose :deep(h3),
.detail-prose :deep(h4) {
  margin-top: 1.2em;
  margin-bottom: 0.5em;
  font-weight: 650;
  line-height: 1.35;
  color: #202124;
}

html.dark .detail-prose :deep(h1),
html.dark .detail-prose :deep(h2),
html.dark .detail-prose :deep(h3),
html.dark .detail-prose :deep(h4) {
  color: #e8eaed;
}

.detail-prose :deep(p) {
  margin: 0.6em 0;
}

.detail-prose :deep(p:first-child) {
  margin-top: 0;
}

.detail-prose :deep(p:last-child) {
  margin-bottom: 0;
}

.detail-prose :deep(ul),
.detail-prose :deep(ol) {
  margin: 0.6em 0;
  padding-left: 1.5rem;
}

.detail-prose :deep(code) {
  font-family: 'Fira Code', ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  padding: 0.15rem 0.35rem;
  border-radius: 0.3rem;
  font-size: 0.86em;
  background-color: rgba(60, 64, 67, 0.07);
}

.detail-prose :deep(pre) {
  padding: 0.85rem 1rem;
  border-radius: 0.6rem;
  overflow-x: auto;
  background-color: #f6f8fa;
  border: 1px solid #eceef1;
}

html.dark .detail-prose :deep(code) {
  background-color: rgba(255, 255, 255, 0.08);
}

html.dark .detail-prose :deep(pre) {
  background-color: #17181a;
  border-color: #2a2b2e;
}

.detail-prose :deep(blockquote) {
  margin: 0.8em 0;
  padding: 0.4em 1em;
  border-left: 3px solid rgba(26, 115, 232, 0.4);
  color: #5f6368;
  background: rgba(26, 115, 232, 0.04);
  border-radius: 0 8px 8px 0;
}

html.dark .detail-prose :deep(blockquote) {
  color: #9aa0a6;
  background: rgba(168, 199, 250, 0.06);
}

.detail-prose :deep(img) {
  max-width: 100%;
  border-radius: 12px;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 420px;
  padding: 10px 14px;
  border-radius: 13px;
  border: 1px solid #e5e8ec;
  background: #fff;
  font-size: 12px;
  font-weight: 600;
  color: #1a73e8;
  text-decoration: none;
  transition: background-color 0.15s ease, border-color 0.15s ease;

  &:hover {
    background: #f6f9ff;
    border-color: rgba(26, 115, 232, 0.3);
  }
}

html.dark .attachment-link {
  background: #161719;
  border-color: #2a2b2e;
  color: #a8c7fa;

  &:hover {
    background: #1c1d20;
    border-color: rgba(168, 199, 250, 0.3);
  }
}

.instance-panel {
  padding: 16px;
  border-radius: 15px;
  border: 1px solid rgba(26, 115, 232, 0.2);
  background: rgba(26, 115, 232, 0.05);
}

html.dark .instance-panel {
  border-color: rgba(168, 199, 250, 0.2);
  background: rgba(168, 199, 250, 0.05);
}

.instance-address {
  text-align: center;
  font-family: 'Fira Code', ui-monospace, monospace;
  font-size: 12px;
  font-weight: 700;
  color: #1a73e8;
  word-break: break-all;
  user-select: all;
  padding: 4px 0 12px;
}

html.dark .instance-address {
  color: #a8c7fa;
}

.instance-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px dashed rgba(26, 115, 232, 0.2);
  font-family: 'Fira Code', ui-monospace, monospace;
  font-size: 11px;
}

html.dark .instance-foot {
  border-top-color: rgba(168, 199, 250, 0.18);
}

.instance-remaining {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #5f6368;
}

html.dark .instance-remaining {
  color: #9aa0a6;
}

.instance-start {
  width: 100%;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 13px;
  border: 1px dashed rgba(26, 115, 232, 0.4);
  background: rgba(26, 115, 232, 0.04);
  font-size: 12px;
  font-weight: 600;
  color: #1a73e8;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;

  &:hover:not(:disabled) {
    background: rgba(26, 115, 232, 0.09);
    border-color: rgba(26, 115, 232, 0.55);
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

html.dark .instance-start {
  color: #a8c7fa;
  border-color: rgba(168, 199, 250, 0.35);
  background: rgba(168, 199, 250, 0.05);

  &:hover:not(:disabled) {
    background: rgba(168, 199, 250, 0.1);
    border-color: rgba(168, 199, 250, 0.5);
  }
}

/* ------------------------------------------------------------------ */
/* Flag dock                                                           */
/* ------------------------------------------------------------------ */

.flag-dock {
  flex-shrink: 0;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f0f2f4;
}

html.dark .flag-dock {
  border-top-color: #26272a;
}

.flag-dock .section-label {
  margin-bottom: 8px;
}

.flag-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.solved-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 13px;
  border: 1px solid rgba(52, 168, 83, 0.3);
  background: rgba(52, 168, 83, 0.07);
  font-size: 12px;
  font-weight: 600;
  color: #1e8e3e;
}

html.dark .solved-banner {
  color: #81c995;
  border-color: rgba(129, 201, 149, 0.28);
  background: rgba(129, 201, 149, 0.08);
}

:global(.training-detail-modal) {
  height: 600px !important;
  display: flex !important;
  flex-direction: column !important;
}

:global(.training-detail-modal .n-card-content),
:global(.training-detail-modal .n-card__content),
:deep(.n-card-content),
:deep(.n-card__content) {
  flex: 1 1 0% !important;
  min-height: 0 !important;
  display: flex !important;
  flex-direction: column !important;
}

:deep(.n-card-header__close .n-base-close),
:deep(.n-base-close) {
  border-radius: 9999px !important;
}
</style>
