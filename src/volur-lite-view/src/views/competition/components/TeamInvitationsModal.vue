<template>
  <n-modal
    :show="show"
    preset="card"
    class="team-invitations-modal"
    style="width: 680px; max-width: 95vw; border-radius: 12px;"
    :title="t('competition.invitations_modal_title')"
    :bordered="false"
    @update:show="handleClose"
  >
    <div v-if="invitations.length === 0" class="empty-wrap">
      <n-empty :description="t('competition.no_invitations')" />
    </div>

    <div v-else class="invitations-list">
      <div
        v-for="item in invitations"
        :key="item.id"
        class="invitation-card"
      >
        <div class="invitation-main">
          <div class="header-row">
            <h4 class="team-name">{{ item.teamName }}</h4>
            <n-tag
              size="small"
              :type="item.role === 'LEADER' ? 'warning' : 'info'"
              round
            >
              邀请角色: {{ item.role === 'LEADER' ? '领队' : '队员' }}
            </n-tag>
          </div>

          <div class="info-row">
            <span v-if="item.captainName" class="meta-item">
              队长: <strong>{{ item.captainName }}</strong>
            </span>
            <span v-if="item.realName" class="meta-item">
              填报姓名: <strong>{{ item.realName }}</strong>
            </span>
            <span v-if="item.invitedAt" class="meta-item time">
              {{ formatDateTime(item.invitedAt) }}
            </span>
          </div>
        </div>

        <div class="invitation-actions">
          <n-popconfirm
            :positive-text="t('common.confirm', '确认')"
            :negative-text="t('common.cancel', '取消')"
            @positive-click="handleReject(item)"
          >
            <template #trigger>
              <n-button
                size="small"
                quaternary
                type="error"
                :loading="operatingId === item.id && opType === 'reject'"
              >
                {{ t('competition.reject_invitation') }}
              </n-button>
            </template>
            <span>确定拒绝加入该战队吗？</span>
          </n-popconfirm>

          <n-button
            size="small"
            class="theme-primary-btn"
            :loading="operatingId === item.id && opType === 'accept'"
            @click="handleAccept(item)"
          >
            {{ t('competition.accept_invitation') }}
          </n-button>
        </div>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  NModal,
  NEmpty,
  NTag,
  NButton,
  NPopconfirm,
  useMessage
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import type { TeamInvitationVO } from '@/types/competition'
import { acceptInvitationApi, rejectInvitationApi } from '@/api/competition'

defineProps<{
  show: boolean
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
const opType = ref<'accept' | 'reject' | null>(null)

function handleClose() {
  emit('update:show', false)
}

function formatDateTime(timeStr?: string) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  return d.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function handleAccept(item: TeamInvitationVO) {
  operatingId.value = item.id
  opType.value = 'accept'
  try {
    await acceptInvitationApi(item.competitionId, item.teamId)
    message.success('已接受邀请，成功加入战队')
    emit('accepted')
    emit('update:show', false)
  } catch {
    // Handled by interceptor
  } finally {
    operatingId.value = null
    opType.value = null
  }
}

async function handleReject(item: TeamInvitationVO) {
  operatingId.value = item.id
  opType.value = 'reject'
  try {
    await rejectInvitationApi(item.competitionId, item.teamId)
    message.success('已拒绝该组队邀请')
    emit('rejected')
  } catch {
    // Handled by interceptor
  } finally {
    operatingId.value = null
    opType.value = null
  }
}
</script>

<style lang="scss" scoped>
.team-invitations-modal {
  .empty-wrap {
    padding: 36px 0;
  }

  .invitations-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    max-height: 480px;
    overflow-y: auto;
    padding-right: 4px;

    .invitation-card {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 14px 18px;
      border: 1px solid var(--v-border-base, #e5e4e7);
      border-radius: 8px;
      background: var(--v-bg-card-alt, #fafafc);

      .invitation-main {
        display: flex;
        flex-direction: column;
        gap: 6px;
        flex: 1;
        min-width: 0;

        .header-row {
          display: flex;
          align-items: center;
          gap: 10px;

          .team-name {
            margin: 0;
            font-size: 15px;
            font-weight: 600;
            color: var(--v-text-primary, #111111);
          }
        }

        .info-row {
          display: flex;
          align-items: center;
          gap: 14px;
          flex-wrap: wrap;
          font-size: 12px;
          color: #666666;

          .meta-item {
            strong {
              color: #222222;
            }
          }

          .time {
            color: #888888;
          }
        }
      }

      .invitation-actions {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-shrink: 0;

        .theme-primary-btn {
          background-color: #000000 !important;
          color: #ffffff !important;
          border: none;
          font-weight: 500;

          &:hover {
            background-color: #222222 !important;
          }
        }
      }
    }
  }
}
</style>
