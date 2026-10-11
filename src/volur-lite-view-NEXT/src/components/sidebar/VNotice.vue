<template>
  <VCard
    class="dock-notice-popover-card"
    :theme="currentTheme"
    :width="340"
    :gap="10"
  >
    <div class="card-top-layer">
      <div class="card-title-group">
        <span class="card-title">{{ t('common.notifications', '通知') }}</span>
        <span v-if="isLoggedIn && unreadCount > 0" class="card-unread-badge">
          {{ unreadCount }}
        </span>
      </div>
      <div class="card-actions-group">
        <button
          v-if="isLoggedIn && unreadCount > 0"
          type="button"
          class="card-text-action-btn"
          :disabled="loading"
          @click="handleMarkAllRead"
        >
          {{ t('common.mark_all_read', '全部已读') }}
        </button>
        <button
          v-if="isLoggedIn"
          type="button"
          class="card-icon-action-btn"
          :class="{ 'is-refreshing': loading }"
          :aria-label="t('common.refresh', '刷新')"
          @click="handleRefresh"
        >
          <n-icon :size="16">
            <RefreshOutline />
          </n-icon>
        </button>
        <button
          type="button"
          class="card-close-btn"
          :aria-label="t('common.close', '关闭')"
          @click="handleClose"
        >
          <n-icon :size="16">
            <CloseOutline />
          </n-icon>
        </button>
      </div>
    </div>

    <div v-if="isLoggedIn" class="card-filter-tabs">
      <button
        v-for="tab in filterTabs"
        :key="tab.key"
        type="button"
        class="filter-tab-pill"
        :class="{ 'is-active': activeTab === tab.key }"
        @click="handleTabChange(tab.key)"
      >
        <span>{{ tab.label }}</span>
        <span v-if="tab.key === 'UNREAD' && unreadCount > 0" class="tab-count-dot" />
      </button>
    </div>

    <div class="card-middle-layer">
      <div v-if="!isLoggedIn" class="notice-guest-box">
        <div class="notice-guest-icon-wrap">
          <n-icon :size="32">
            <NotificationsOutline />
          </n-icon>
        </div>
        <p class="notice-guest-tip">{{ t('common.signin_to_view_notice', '登录后查看站内通知') }}</p>
        <button type="button" class="notice-signin-btn" @click="handleGoSignin">
          {{ t('common.signin', '登录') }}
        </button>
      </div>

      <div v-else-if="loading && notificationList.length === 0" class="notice-loading-box">
        <n-spin size="small" />
      </div>

      <div v-else-if="notificationList.length === 0" class="notice-empty-box">
        <div class="notice-empty-icon-wrap">
          <n-icon :size="32">
            <NotificationsOffOutline />
          </n-icon>
        </div>
        <span class="notice-empty-text">
          {{ activeTab === 'UNREAD' ? t('common.no_unread_notice', '暂无未读通知') : t('common.no_notifications', '暂无通知') }}
        </span>
      </div>

      <div v-else class="notice-list-scroll">
        <div
          v-for="item in notificationList"
          :key="item.id"
          class="notice-item-row"
          :class="{ 'is-unread': item.status === 'UNREAD' }"
          @click="handleItemClick(item)"
        >
          <div class="item-type-icon-box" :class="getNotificationTypeClass(item.type)">
            <n-icon :size="15">
              <component :is="getNotificationTypeIcon(item.type)" />
            </n-icon>
          </div>

          <div class="item-content-box">
            <div class="item-title-row">
              <span class="item-title-text" :title="item.title">{{ item.title }}</span>
              <span class="item-time-text">{{ formatTime(item.createdAt) }}</span>
            </div>
            <div class="item-desc-text">{{ item.content }}</div>
            <div
              v-if="getCompetitionTarget(item)"
              class="item-link-text"
              @click.stop="handleNavigateToCompetition(item)"
            >
              {{ t('common.go_to_competition', '前往竞赛详情') }} &rarr;
            </div>
          </div>

          <div class="item-action-box">
            <span v-if="item.status === 'UNREAD'" class="unread-dot-badge" />
            <button
              type="button"
              class="item-delete-btn"
              :aria-label="t('common.delete', '删除')"
              @click.stop="handleDeleteItem(item)"
            >
              <n-icon :size="13">
                <TrashOutline />
              </n-icon>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="isLoggedIn" class="card-bottom-layer">
      <span class="notice-total-text">
        {{ t('common.total_notifications', { total: totalCount }) }}
      </span>
    </div>

    <n-modal
      v-model:show="showDetailModal"
      preset="card"
      :title="t('common.notification_detail', '通知详情')"
      class="v-notice-detail-modal"
      :class="currentTheme"
      style="width: 440px; max-width: 90vw; border-radius: 16px;"
    >
      <div v-if="selectedNotification" class="modal-detail-body">
        <div class="modal-detail-header">
          <h4 class="modal-detail-title">{{ selectedNotification.title }}</h4>
          <div class="modal-detail-meta">
            <span class="modal-detail-type" :class="getNotificationTypeClass(selectedNotification.type)">
              {{ getNotificationTypeLabel(selectedNotification.type) }}
            </span>
            <span class="modal-detail-time">{{ formatTime(selectedNotification.createdAt) }}</span>
          </div>
        </div>
        <div class="modal-detail-content">{{ selectedNotification.content }}</div>
      </div>
      <template #footer>
        <div class="modal-detail-footer">
          <button
            v-if="selectedNotification && getCompetitionTarget(selectedNotification)"
            type="button"
            class="modal-action-primary-btn"
            @click="handleNavigateToCompetition(selectedNotification)"
          >
            {{ t('common.go_to_competition', '前往竞赛详情') }}
          </button>
          <div v-else></div>
          <button
            type="button"
            class="modal-action-secondary-btn"
            @click="showDetailModal = false"
          >
            {{ t('common.close', '关闭') }}
          </button>
        </div>
      </template>
    </n-modal>
  </VCard>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NIcon, NSpin, NModal } from 'naive-ui'
import {
  NotificationsOutline,
  NotificationsOffOutline,
  CloseOutline,
  RefreshOutline,
  TrashOutline,
  TrophyOutline,
  PeopleOutline,
  RibbonOutline
} from '@vicons/ionicons5'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { useNotificationStore } from '@/store/modules/notification'
import type { NotificationVO } from '@/types/notification'
import VCard from './VCard.vue'
import type { VNoticeProps } from './types'

defineOptions({
  name: 'VNotice'
})

const props = withDefaults(defineProps<VNoticeProps>(), {
  theme: undefined,
  notificationRoute: '/notifications',
  signinRoute: '/v1/signin/identifier',
  maxItems: 15
})

const emit = defineEmits<{
  close: []
  'item-click': [item: NotificationVO]
}>()

const router = useRouter()
const { t } = useI18n()
const appStore = useAppStore()
const userStore = useUserStore()
const notificationStore = useNotificationStore()

const activeTab = ref<string>('ALL')
const showDetailModal = ref<boolean>(false)
const selectedNotification = ref<NotificationVO | null>(null)

const currentTheme = computed<'light' | 'dark'>(() => {
  return props.theme || (appStore.theme === 'dark' ? 'dark' : 'light')
})

const isLoggedIn = computed<boolean>(() => {
  return userStore.isLoggedIn
})

const unreadCount = computed<number>(() => {
  return notificationStore.unreadCount
})

const totalCount = computed<number>(() => {
  return notificationStore.totalCount
})

const loading = computed<boolean>(() => {
  return notificationStore.loading
})

const notificationList = computed<NotificationVO[]>(() => {
  return notificationStore.notifications
})

const filterTabs = computed(() => [
  { key: 'ALL', label: t('common.all', '全部') },
  { key: 'UNREAD', label: t('common.unread', '未读') }
])

function formatTime(timeStr?: string): string {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  if (diffMs < 0) return t('common.time_just_now', '刚刚')
  const diffMins = Math.floor(diffMs / 60000)
  if (diffMins < 1) return t('common.time_just_now', '刚刚')
  if (diffMins < 60) return t('common.time_minutes_ago', { n: diffMins })
  const diffHours = Math.floor(diffMins / 60)
  if (diffHours < 24) return t('common.time_hours_ago', { n: diffHours })
  const diffDays = Math.floor(diffHours / 24)
  if (diffDays === 1) return t('common.time_yesterday', '昨天')
  if (diffDays < 7) return t('common.time_days_ago', { n: diffDays })
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${m}-${d}`
}

function getNotificationTypeIcon(type?: string) {
  switch (type) {
    case 'COMPETITION':
      return TrophyOutline
    case 'TEAM':
      return PeopleOutline
    case 'AWARD':
      return RibbonOutline
    default:
      return NotificationsOutline
  }
}

function getNotificationTypeClass(type?: string): string {
  switch (type) {
    case 'COMPETITION':
      return 'is-competition'
    case 'TEAM':
      return 'is-team'
    case 'AWARD':
      return 'is-award'
    default:
      return 'is-system'
  }
}

function getNotificationTypeLabel(type?: string): string {
  switch (type) {
    case 'COMPETITION':
      return t('common.competition_notice', '竞赛消息')
    case 'TEAM':
      return t('common.team_notice', '组队消息')
    case 'AWARD':
      return t('common.award_notice', '荣誉奖励')
    default:
      return t('common.system_notice', '系统通知')
  }
}

function getCompetitionTarget(notification: NotificationVO | null): string | null {
  if (!notification || !notification.actionData) return null
  try {
    const data = JSON.parse(notification.actionData)
    if (data.slug) return String(data.slug)
    if (data.competitionId) return String(data.competitionId)
    if (data.id) return String(data.id)
  } catch {
    const trimmed = notification.actionData.trim()
    if (trimmed.startsWith('/competitions/')) {
      return trimmed.replace('/competitions/', '')
    }
    if (trimmed.startsWith('/c/')) {
      return trimmed.replace('/c/', '')
    }
    if (/^[a-zA-Z0-9_-]+$/.test(trimmed)) {
      return trimmed
    }
  }
  return null
}

async function fetchList() {
  if (!isLoggedIn.value) return
  const params: { page: number; size: number; unreadOnly?: boolean; type?: string } = {
    page: 1,
    size: props.maxItems
  }
  if (activeTab.value === 'UNREAD') {
    params.unreadOnly = true
  } else if (activeTab.value !== 'ALL') {
    params.type = activeTab.value
  }
  await notificationStore.fetchNotifications(params)
}

function handleTabChange(key: string) {
  activeTab.value = key
  fetchList()
}

function handleRefresh() {
  fetchList()
  notificationStore.fetchUnreadCount()
}

async function handleMarkAllRead() {
  try {
    await notificationStore.markAllAsRead()
  } catch {
  }
}

async function handleDeleteItem(item: NotificationVO) {
  try {
    await notificationStore.deleteNotification(item.id)
  } catch {
  }
}

async function handleItemClick(item: NotificationVO) {
  emit('item-click', item)
  if (item.status === 'UNREAD') {
    try {
      await notificationStore.markAsRead(item.id)
    } catch {
    }
  }
  const compTarget = getCompetitionTarget(item)
  if (compTarget) {
    handleNavigateToCompetition(item)
    return
  }
  selectedNotification.value = item
  showDetailModal.value = true
}

function handleNavigateToCompetition(notification: NotificationVO | null) {
  const target = getCompetitionTarget(notification)
  if (target) {
    showDetailModal.value = false
    emit('close')
    router.push(`/competitions/${target}`).catch(() => {})
  }
}

function handleGoSignin() {
  emit('close')
  if (props.signinRoute) {
    router.push(props.signinRoute).catch(() => {})
  }
}

function handleClose() {
  emit('close')
}

watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      notificationStore.fetchUnreadCount()
      fetchList()
    } else {
      notificationStore.reset()
    }
  }
)

onMounted(() => {
  if (isLoggedIn.value) {
    notificationStore.fetchUnreadCount()
    fetchList()
  }
})
</script>

<style scoped lang="scss">
.dock-notice-popover-card {
  width: 340px;
  background-color: #fcfcfc;
  border-radius: 16px;
  box-sizing: border-box;
  padding: 14px 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  user-select: none;
  box-shadow: 0 4px 20px rgba(60, 64, 67, 0.15), 0 1px 4px rgba(60, 64, 67, 0.1);

  .card-top-layer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 28px;
    flex-shrink: 0;

    .card-title-group {
      display: flex;
      align-items: center;
      gap: 6px;

      .card-title {
        font-size: 14px;
        font-weight: 600;
        color: #202124;
        line-height: 1.3;
      }

      .card-unread-badge {
        font-size: 11px;
        font-weight: 500;
        background-color: rgba(234, 67, 53, 0.12);
        color: #d93025;
        padding: 1px 6px;
        border-radius: 10px;
        line-height: 1.3;
      }
    }

    .card-actions-group {
      display: flex;
      align-items: center;
      gap: 4px;

      .card-text-action-btn {
        font-size: 11px;
        color: #1a73e8;
        background: transparent;
        border: none;
        padding: 3px 6px;
        border-radius: 6px;
        cursor: pointer;
        outline: none;
        transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1), color 0.2s ease;

        &:hover:not(:disabled) {
          background-color: rgba(26, 115, 232, 0.08);
        }

        &:disabled {
          opacity: 0.4;
          cursor: not-allowed;
        }
      }

      .card-icon-action-btn {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        border: none;
        background: transparent;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        color: #5f6368;
        outline: none;
        padding: 0;
        transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1), color 0.2s ease;

        &:hover {
          background-color: rgba(60, 64, 67, 0.08);
          color: #202124;
        }

        &.is-refreshing {
          animation: notice-spin 0.8s linear infinite;
        }
      }

      .card-close-btn {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        border: none;
        background: transparent;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        color: #5f6368;
        outline: none;
        padding: 0;
        transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1), color 0.2s ease;

        &:hover {
          background-color: rgba(60, 64, 67, 0.08);
          color: #202124;
        }

        &:active {
          background-color: rgba(60, 64, 67, 0.12);
        }
      }
    }
  }

  .card-filter-tabs {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 2px 0;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }

    .filter-tab-pill {
      font-size: 12px;
      font-weight: 500;
      padding: 3px 10px;
      border-radius: 14px;
      border: 1px solid transparent;
      background: transparent;
      color: #5f6368;
      cursor: pointer;
      outline: none;
      white-space: nowrap;
      display: inline-flex;
      align-items: center;
      transition: all 0.2s cubic-bezier(0.2, 0, 0, 1);

      &:hover {
        background-color: #f1f3f4;
        color: #202124;
      }

      &.is-active {
        background-color: #e8f0fe;
        color: #1a73e8;
        border-color: rgba(26, 115, 232, 0.2);
      }

      .tab-count-dot {
        width: 5px;
        height: 5px;
        border-radius: 50%;
        background-color: #ea4335;
        margin-left: 4px;
      }
    }
  }

  .card-middle-layer {
    width: 100%;
    min-height: 180px;
    max-height: 280px;
    overflow-y: auto;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    scrollbar-width: thin;
    scrollbar-color: rgba(60, 64, 67, 0.2) transparent;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background-color: rgba(60, 64, 67, 0.2);
      border-radius: 4px;
    }

    .notice-guest-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 10px;
      height: 180px;
      text-align: center;

      .notice-guest-icon-wrap {
        color: #9aa0a6;
      }

      .notice-guest-tip {
        font-size: 12px;
        color: #5f6368;
        margin: 0;
      }

      .notice-signin-btn {
        height: 32px;
        border-radius: 16px;
        background-color: #1a73e8;
        color: #ffffff;
        font-size: 12px;
        font-weight: 500;
        padding: 0 18px;
        border: none;
        cursor: pointer;
        outline: none;
        transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1);

        &:hover {
          background-color: #1765cc;
        }

        &:active {
          background-color: #1557b0;
        }
      }
    }

    .notice-loading-box,
    .notice-empty-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      height: 180px;
      color: #5f6368;

      .notice-empty-icon-wrap {
        color: #9aa0a6;
      }

      .notice-empty-text {
        font-size: 12px;
      }
    }

    .notice-list-scroll {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .notice-item-row {
        display: flex;
        align-items: flex-start;
        gap: 10px;
        padding: 8px 10px;
        border-radius: 10px;
        cursor: pointer;
        box-sizing: border-box;
        transition: background-color 0.18s cubic-bezier(0.2, 0, 0, 1);

        &:hover {
          background-color: #f1f3f4;

          .item-action-box .item-delete-btn {
            opacity: 1;
          }
        }

        &.is-unread {
          background-color: rgba(26, 115, 232, 0.04);

          .item-content-box .item-title-row .item-title-text {
            font-weight: 600;
          }
        }

        .item-type-icon-box {
          width: 30px;
          height: 30px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          margin-top: 1px;

          &.is-competition {
            background-color: rgba(251, 188, 4, 0.16);
            color: #b06000;
          }

          &.is-team {
            background-color: rgba(52, 168, 83, 0.14);
            color: #137333;
          }

          &.is-award {
            background-color: rgba(234, 67, 53, 0.12);
            color: #c5221f;
          }

          &.is-system {
            background-color: rgba(26, 115, 232, 0.12);
            color: #1a73e8;
          }
        }

        .item-content-box {
          flex: 1;
          min-width: 0;
          display: flex;
          flex-direction: column;
          gap: 2px;

          .item-title-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 6px;

            .item-title-text {
              font-size: 13px;
              font-weight: 500;
              color: #202124;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .item-time-text {
              font-size: 11px;
              color: #80868b;
              flex-shrink: 0;
            }
          }

          .item-desc-text {
            font-size: 12px;
            color: #5f6368;
            line-height: 1.4;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }

          .item-link-text {
            font-size: 11px;
            color: #1a73e8;
            margin-top: 2px;
            font-weight: 500;
            cursor: pointer;

            &:hover {
              text-decoration: underline;
            }
          }
        }

        .item-action-box {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 6px;
          flex-shrink: 0;
          margin-top: 4px;

          .unread-dot-badge {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background-color: #1a73e8;
          }

          .item-delete-btn {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            border: none;
            background: transparent;
            color: #80868b;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            opacity: 0;
            padding: 0;
            outline: none;
            transition: all 0.15s ease;

            &:hover {
              background-color: rgba(234, 67, 53, 0.12);
              color: #ea4335;
            }
          }
        }
      }
    }
  }

  .card-bottom-layer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding-top: 6px;
    flex-shrink: 0;

    .notice-total-text {
      font-size: 11px;
      color: #80868b;
    }
  }

  &.dark,
  html.dark & {
    background-color: #191919;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.6), 0 1px 4px rgba(0, 0, 0, 0.4);

    .card-top-layer {
      .card-title-group {
        .card-title {
          color: #e8eaed;
        }

        .card-unread-badge {
          background-color: rgba(242, 139, 130, 0.2);
          color: #f28b82;
        }
      }

      .card-actions-group {
        .card-text-action-btn {
          color: #8ab4f8;

          &:hover:not(:disabled) {
            background-color: rgba(138, 180, 248, 0.12);
          }
        }

        .card-icon-action-btn,
        .card-close-btn {
          color: #9aa0a6;

          &:hover {
            background-color: rgba(255, 255, 255, 0.08);
            color: #e8eaed;
          }

          &:active {
            background-color: rgba(255, 255, 255, 0.12);
          }
        }
      }
    }

    .card-filter-tabs {
      .filter-tab-pill {
        color: #9aa0a6;

        &:hover {
          background-color: #303134;
          color: #e8eaed;
        }

        &.is-active {
          background-color: rgba(138, 180, 248, 0.16);
          color: #8ab4f8;
          border-color: rgba(138, 180, 248, 0.3);
        }

        .tab-count-dot {
          background-color: #f28b82;
        }
      }
    }

    .card-middle-layer {
      scrollbar-color: rgba(255, 255, 255, 0.2) transparent;

      &::-webkit-scrollbar-thumb {
        background-color: rgba(255, 255, 255, 0.2);
      }

      .notice-guest-box {
        .notice-guest-icon-wrap {
          color: #80868b;
        }

        .notice-guest-tip {
          color: #9aa0a6;
        }

        .notice-signin-btn {
          background-color: #8ab4f8;
          color: #202124;

          &:hover {
            background-color: #aecbfa;
          }

          &:active {
            background-color: #79a6f6;
          }
        }
      }

      .notice-loading-box,
      .notice-empty-box {
        color: #9aa0a6;

        .notice-empty-icon-wrap {
          color: #80868b;
        }
      }

      .notice-list-scroll {
        .notice-item-row {
          &:hover {
            background-color: #2a2a2a;
          }

          &.is-unread {
            background-color: rgba(138, 180, 248, 0.06);
          }

          .item-type-icon-box {
            &.is-competition {
              background-color: rgba(253, 214, 99, 0.16);
              color: #fdd663;
            }

            &.is-team {
              background-color: rgba(129, 201, 149, 0.16);
              color: #81c995;
            }

            &.is-award {
              background-color: rgba(242, 139, 130, 0.16);
              color: #f28b82;
            }

            &.is-system {
              background-color: rgba(138, 180, 248, 0.16);
              color: #8ab4f8;
            }
          }

          .item-content-box {
            .item-title-row {
              .item-title-text {
                color: #e8eaed;
              }

              .item-time-text {
                color: #9aa0a6;
              }
            }

            .item-desc-text {
              color: #bdc1c6;
            }

            .item-link-text {
              color: #8ab4f8;
            }
          }

          .item-action-box {
            .unread-dot-badge {
              background-color: #8ab4f8;
            }

            .item-delete-btn {
              color: #9aa0a6;

              &:hover {
                background-color: rgba(242, 139, 130, 0.16);
                color: #f28b82;
              }
            }
          }
        }
      }
    }

    .card-bottom-layer {
      .notice-total-text {
        color: #9aa0a6;
      }
    }
  }
}

.modal-detail-body {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .modal-detail-header {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .modal-detail-title {
      font-size: 15px;
      font-weight: 600;
      color: #202124;
      margin: 0;
    }

    .modal-detail-meta {
      display: flex;
      align-items: center;
      gap: 8px;

      .modal-detail-type {
        font-size: 11px;
        font-weight: 500;
        padding: 2px 8px;
        border-radius: 10px;

        &.is-competition {
          background-color: rgba(251, 188, 4, 0.16);
          color: #b06000;
        }

        &.is-team {
          background-color: rgba(52, 168, 83, 0.14);
          color: #137333;
        }

        &.is-award {
          background-color: rgba(234, 67, 53, 0.12);
          color: #c5221f;
        }

        &.is-system {
          background-color: rgba(26, 115, 232, 0.12);
          color: #1a73e8;
        }
      }

      .modal-detail-time {
        font-size: 11px;
        color: #80868b;
      }
    }
  }

  .modal-detail-content {
    font-size: 13px;
    color: #3c4043;
    line-height: 1.6;
    background-color: rgba(60, 64, 67, 0.04);
    padding: 12px;
    border-radius: 8px;
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.modal-detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  .modal-action-primary-btn {
    height: 32px;
    border-radius: 16px;
    background-color: #1a73e8;
    color: #ffffff;
    font-size: 12px;
    font-weight: 500;
    padding: 0 16px;
    border: none;
    cursor: pointer;
    outline: none;
    transition: background-color 0.2s cubic-bezier(0.2, 0, 0, 1);

    &:hover {
      background-color: #1765cc;
    }
  }

  .modal-action-secondary-btn {
    height: 32px;
    border-radius: 16px;
    background-color: transparent;
    border: 1px solid #d7d8da;
    color: #3c4043;
    font-size: 12px;
    font-weight: 500;
    padding: 0 16px;
    cursor: pointer;
    outline: none;
    transition: all 0.2s cubic-bezier(0.2, 0, 0, 1);

    &:hover {
      background-color: #f1f3f4;
      color: #202124;
    }
  }
}

html.dark {
  .modal-detail-body {
    .modal-detail-header {
      .modal-detail-title {
        color: #e8eaed;
      }

      .modal-detail-meta {
        .modal-detail-type {
          &.is-competition {
            background-color: rgba(253, 214, 99, 0.16);
            color: #fdd663;
          }

          &.is-team {
            background-color: rgba(129, 201, 149, 0.16);
            color: #81c995;
          }

          &.is-award {
            background-color: rgba(242, 139, 130, 0.16);
            color: #f28b82;
          }

          &.is-system {
            background-color: rgba(138, 180, 248, 0.16);
            color: #8ab4f8;
          }
        }

        .modal-detail-time {
          color: #9aa0a6;
        }
      }
    }

    .modal-detail-content {
      color: #bdc1c6;
      background-color: rgba(255, 255, 255, 0.04);
    }
  }

  .modal-detail-footer {
    .modal-action-primary-btn {
      background-color: #8ab4f8;
      color: #202124;

      &:hover {
        background-color: #aecbfa;
      }
    }

    .modal-action-secondary-btn {
      border-color: #5f6368;
      color: #bdc1c6;

      &:hover {
        background-color: #303134;
        color: #e8eaed;
      }
    }
  }
}

@keyframes notice-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
