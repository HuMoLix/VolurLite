<template>
  <n-modal
    :show="show"
    preset="card"
    class="team-invitations-modal w-full max-w-2xl rounded-2xl sm:rounded-3xl border border-google-border-light dark:border-google-border-dark bg-white dark:bg-google-surface-darkCard"
    :title="t('competition.my_invitations_btn', '收到的组队邀请')"
    :bordered="false"
    @update:show="handleClose"
  >
    <div v-if="invitations.length === 0" class="py-12 flex flex-col items-center justify-center text-center">
      <div class="w-14 h-14 rounded-full bg-google-surface-light dark:bg-google-surface-dark flex items-center justify-center text-google-text-muted dark:text-google-text-darkMuted mb-3">
        <n-icon :size="28" :component="MailOutline" />
      </div>
      <p class="text-sm text-google-text-desc dark:text-google-text-darkDesc">
        {{ t('competition.no_invitations', '暂无待处理的组队邀请') }}
      </p>
    </div>

    <div v-else class="space-y-3.5 max-h-[60vh] overflow-y-auto pr-1">
      <div
        v-for="item in invitations"
        :key="item.id"
        class="p-4 rounded-xl sm:rounded-2xl border border-google-border-light/80 dark:border-google-border-dark/80 bg-google-surface-light/50 dark:bg-google-surface-dark/40 flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition-colors"
      >
        <div class="space-y-1.5 min-w-0 flex-1">
          <div class="flex items-center gap-2 flex-wrap">
            <h4 class="m-0 text-base font-semibold text-google-text-primary dark:text-[#FCFCFC] truncate">
              {{ item.teamName }}
            </h4>
            <span
              class="px-2 py-0.5 rounded-full text-xs font-medium"
              :class="item.role === 'LEADER' ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300' : 'bg-blue-100 text-google-blue dark:bg-blue-950/60 dark:text-google-blue-dark'"
            >
              {{ item.role === 'LEADER' ? '领队/指导老师' : '参赛队员' }}
            </span>
          </div>

          <div class="flex items-center flex-wrap gap-x-4 gap-y-1 text-xs text-google-text-desc dark:text-google-text-darkDesc">
            <span v-if="item.captainName">
              队长: <strong class="text-google-text-primary dark:text-google-text-darkPrimary">{{ item.captainName }}</strong>
            </span>
            <span v-if="item.realName">
              填报姓名: <strong class="text-google-text-primary dark:text-google-text-darkPrimary">{{ item.realName }}</strong>
            </span>
            <span v-if="item.invitedAt" class="text-google-text-muted dark:text-google-text-darkMuted font-mono">
              {{ formatDateTime(item.invitedAt) }}
            </span>
          </div>
        </div>

        <div class="flex items-center gap-2.5 shrink-0 self-end sm:self-center">
          <n-popconfirm
            positive-text="确认拒绝"
            negative-text="取消"
            @positive-click="handleReject(item)"
          >
            <template #trigger>
              <button
                type="button"
                class="h-8 px-3 rounded-full border border-red-200 dark:border-red-900/60 text-google-red dark:text-google-red-dark bg-white dark:bg-google-surface-darkCard hover:bg-red-50 dark:hover:bg-red-950/30 text-xs font-medium transition-colors cursor-pointer"
                :disabled="operatingId === item.id"
              >
                {{ t('competition.reject_invitation', '拒绝') }}
              </button>
            </template>
            <span>确定拒绝加入该战队吗？</span>
          </n-popconfirm>

          <button
            type="button"
            class="h-8 px-4 rounded-full bg-google-blue hover:bg-google-blue-hover text-white dark:bg-google-blue-dark dark:text-google-surface-dark text-xs font-medium transition-colors cursor-pointer border-none"
            :disabled="operatingId === item.id"
            @click="handleAccept(item)"
          >
            {{ t('competition.accept_invitation', '接受') }}
          </button>
        </div>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { NModal, NIcon, NPopconfirm, useMessage } from 'naive-ui'
import { MailOutline } from '@vicons/ionicons5'
import { useI18n } from 'vue-i18n'
import type { TeamInvitationVO } from '@/types/competition'
import { acceptInvitationApi, rejectInvitationApi } from '@/api/competition'

defineOptions({
  name: 'TeamInvitationsModal'
})

const props = defineProps<{
  show: boolean
  competitionId: number | string
  invitations: TeamInvitationVO[]
}>()

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'accepted'): void
  (e: 'rejected'): void
}>()

const { t } = useI18n()
const message = useMessage()

const operatingId = ref<number | string | null>(null)

function handleClose() {
  emit('update:show', false)
}

function formatDateTime(timeStr?: string | null): string {
  if (!timeStr) return '-'
  try {
    const d = new Date(timeStr)
    if (isNaN(d.getTime())) return timeStr
    const pad = (n: number) => (n < 10 ? '0' + n : String(n))
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch {
    return timeStr
  }
}

async function handleAccept(item: TeamInvitationVO) {
  if (operatingId.value) return
  operatingId.value = item.id
  try {
    await acceptInvitationApi(props.competitionId, item.teamId)
    if (message) {
      message.success('已接受邀请，成功加入战队')
    }
    emit('accepted')
    handleClose()
  } catch (err: any) {
    if (message) {
      message.error(err?.message || '加入队伍失败')
    }
  } finally {
    operatingId.value = null
  }
}

async function handleReject(item: TeamInvitationVO) {
  if (operatingId.value) return
  operatingId.value = item.id
  try {
    await rejectInvitationApi(props.competitionId, item.teamId)
    if (message) {
      message.info('已拒绝该组队邀请')
    }
    emit('rejected')
  } catch (err: any) {
    if (message) {
      message.error(err?.message || '处理邀请失败')
    }
  } finally {
    operatingId.value = null
  }
}
</script>
