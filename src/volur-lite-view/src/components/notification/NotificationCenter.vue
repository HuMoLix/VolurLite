<template>
  <div class="notification-center">
    <n-popover
      trigger="click"
      placement="bottom-end"
      :width="380"
      content-style="padding: 0;"
      v-model:show="showPopover"
    >
      <template #trigger>
        <n-badge :value="unreadCount" :max="99" :show="unreadCount > 0" class="badge-wrapper">
          <n-button quaternary circle class="bell-btn" title="站内通知">
            <template #icon>
              <n-icon size="19">
                <NotificationsOutline />
              </n-icon>
            </template>
          </n-button>
        </n-badge>
      </template>

      <div class="notification-panel">
        <!-- Header -->
        <div class="panel-header">
          <div class="header-title">
            <span>站内通知</span>
            <span v-if="unreadCount > 0" class="unread-tag">{{ unreadCount }} 条未读</span>
          </div>
          <div class="header-actions">
            <n-button
              quaternary
              size="tiny"
              :disabled="unreadCount === 0"
              @click="handleMarkAllRead"
            >
              全部已读
            </n-button>
            <n-button quaternary circle size="tiny" @click="fetchNotifications">
              <template #icon>
                <n-icon :component="RefreshOutline" />
              </template>
            </n-button>
          </div>
        </div>

        <!-- Filter Tabs -->
        <n-tabs
          v-model:value="activeTab"
          size="small"
          justify-content="space-between"
          class="panel-tabs"
          @update:value="handleTabChange"
        >
          <n-tab-pane name="ALL" tab="全部" />
          <n-tab-pane name="UNREAD" tab="未读" />
          <n-tab-pane name="SYSTEM" tab="系统" />
          <n-tab-pane name="COMPETITION" tab="比赛" />
          <n-tab-pane name="TEAM" tab="组队" />
        </n-tabs>

        <!-- Notification List -->
        <div class="panel-body" :class="{ 'is-loading': loading }">
          <div v-if="loading" class="loading-state">
            <n-spin size="small" />
          </div>
          <div v-else-if="notificationList.length === 0" class="empty-state">
            <n-empty description="暂无通知消息" size="small" />
          </div>
          <div v-else class="list-container">
            <div
              v-for="item in notificationList"
              :key="item.id"
              class="notification-item"
              :class="{ unread: item.status === 'UNREAD' }"
              @click="handleItemClick(item)"
            >
              <div class="item-icon-wrapper" :class="item.type?.toLowerCase() || 'system'">
                <n-icon size="16">
                  <component :is="getTypeIcon(item.type)" />
                </n-icon>
              </div>
              <div class="item-content">
                <div class="item-title-row">
                  <span class="item-title" :title="item.title">{{ item.title }}</span>
                  <span class="item-time">{{ formatTime(item.createdAt) }}</span>
                </div>
                <div class="item-desc">{{ item.content }}</div>
                <div
                  v-if="getCompetitionTarget(item)"
                  class="item-link"
                  @click.stop="handleNavigateToCompetition(item)"
                >
                  前往竞赛主页 &rarr;
                </div>
              </div>
              <div class="item-actions">
                <span v-if="item.status === 'UNREAD'" class="unread-dot" />
                <n-button
                  quaternary
                  circle
                  size="tiny"
                  class="delete-btn"
                  @click.stop="handleDeleteItem(item)"
                >
                  <template #icon>
                    <n-icon :component="TrashOutline" size="13" />
                  </template>
                </n-button>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="panel-footer">
          <span class="total-text">共 {{ totalCount }} 条通知</span>
          <n-button text type="primary" size="tiny" class="view-more-btn" @click="handleViewMore">
            <span>{{ t('profile.notifications_view_more') }}</span>
            <template #icon>
              <n-icon size="13">
                <ChevronForwardOutline />
              </n-icon>
            </template>
          </n-button>
        </div>
      </div>
    </n-popover>

    <!-- Detail Modal -->
    <n-modal
      v-model:show="showDetailModal"
      preset="card"
      title="站内信通知详情"
      style="width: 480px;"
    >
      <div v-if="selectedNotification" class="detail-container">
        <div class="detail-header">
          <div class="detail-title">{{ selectedNotification.title }}</div>
          <div class="detail-meta">
            <n-tag size="small" round :type="getTypeTagType(selectedNotification.type)">
              {{ getTypeLabel(selectedNotification.type) }}
            </n-tag>
            <span class="detail-time">{{ formatTime(selectedNotification.createdAt) }}</span>
          </div>
        </div>
        <div class="detail-content">
          {{ selectedNotification.content }}
        </div>
      </div>
      <template #footer>
        <div class="modal-footer" style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
          <div>
            <n-button
              v-if="getCompetitionTarget(selectedNotification)"
              type="primary"
              class="theme-primary-btn"
              size="small"
              @click="handleNavigateToCompetition(selectedNotification)"
            >
              前往竞赛详情
            </n-button>
          </div>
          <n-button size="small" @click="showDetailModal = false">关闭</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  NPopover,
  NBadge,
  NButton,
  NIcon,
  NTabs,
  NTabPane,
  NEmpty,
  NSpin,
  NTag,
  NModal,
  useMessage
} from 'naive-ui'
import {
  NotificationsOutline,
  RefreshOutline,
  TrashOutline,
  MailOutline,
  TrophyOutline,
  PeopleOutline,
  RibbonOutline,
  ChevronForwardOutline
} from '@vicons/ionicons5'
import {
  getUnreadNotificationCountApi,
  getNotificationListApi,
  getNotificationDetailApi,
  markAllNotificationsReadApi,
  deleteNotificationApi
} from '@/api/notification'
import type { NotificationVO } from '@/types/notification'
import { useUserStore } from '@/store/modules/user'
import { SseClient } from '@/utils/sse'

const { t } = useI18n()
const router = useRouter()
const message = useMessage()
const userStore = useUserStore()

const showPopover = ref(false)
const unreadCount = ref(0)
const totalCount = ref(0)
const loading = ref(false)
const activeTab = ref('ALL')
const notificationList = ref<NotificationVO[]>([])

const showDetailModal = ref(false)
const selectedNotification = ref<NotificationVO | null>(null)

let sseClient: SseClient | null = null

function formatTime(timeStr?: string) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  if (diffMins < 1) return '刚刚'
  if (diffMins < 60) return `${diffMins} 分钟前`
  const diffHours = Math.floor(diffMins / 60)
  if (diffHours < 24) return `${diffHours} 小时前`
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function getTypeIcon(type?: string) {
  switch (type) {
    case 'COMPETITION':
      return TrophyOutline
    case 'TEAM':
      return PeopleOutline
    case 'AWARD':
      return RibbonOutline
    default:
      return MailOutline
  }
}

function getTypeLabel(type?: string) {
  switch (type) {
    case 'COMPETITION':
      return '竞赛事件'
    case 'TEAM':
      return '组队协作'
    case 'AWARD':
      return '荣誉奖励'
    default:
      return '系统通知'
  }
}

function getTypeTagType(type?: string): 'info' | 'success' | 'warning' | 'default' {
  switch (type) {
    case 'COMPETITION':
      return 'warning'
    case 'TEAM':
      return 'info'
    case 'AWARD':
      return 'success'
    default:
      return 'default'
  }
}

async function fetchUnreadCount() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getUnreadNotificationCountApi()
    if (res.data !== undefined) {
      unreadCount.value = Number(res.data)
    }
  } catch (error) {
    console.error('Failed to fetch unread count:', error)
  }
}

async function fetchNotifications() {
  if (!userStore.isLoggedIn) return
  loading.value = true
  try {
    const params: { page: number; size: number; unreadOnly?: boolean; type?: string } = {
      page: 1,
      size: 20
    }
    if (activeTab.value === 'UNREAD') {
      params.unreadOnly = true
    } else if (activeTab.value !== 'ALL') {
      params.type = activeTab.value
    }

    const res = await getNotificationListApi(params)
    if (res.data) {
      notificationList.value = res.data.list || []
      totalCount.value = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to fetch notification list:', error)
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  fetchNotifications()
}

function handleViewMore() {
  showPopover.value = false
  router.push({ path: '/profile', query: { tab: 'notifications' } })
}

function getCompetitionTarget(notification: NotificationVO | null): string | null {
  if (!notification || !notification.actionData) return null
  try {
    const data = JSON.parse(notification.actionData)
    if (data.slug) return String(data.slug)
    if (data.competitionId) return String(data.competitionId)
  } catch {
    const trimmed = notification.actionData.trim()
    if (trimmed.startsWith('/competitions/')) {
      return trimmed.replace('/competitions/', '')
    }
    if (/^[a-zA-Z0-9_-]+$/.test(trimmed)) {
      return trimmed
    }
  }
  return null
}

function handleNavigateToCompetition(notification: NotificationVO | null) {
  const target = getCompetitionTarget(notification)
  if (target) {
    showDetailModal.value = false
    showPopover.value = false
    router.push(`/competitions/${target}`)
  }
}

async function handleItemClick(item: NotificationVO) {
  selectedNotification.value = item
  showDetailModal.value = true

  try {
    const res = await getNotificationDetailApi(item.id)
    if (res.data) {
      selectedNotification.value = res.data
    }
    if (item.status === 'UNREAD') {
      item.status = 'READ'
      if (unreadCount.value > 0) {
        unreadCount.value--
      }
    }
  } catch (error) {
    console.error('Failed to fetch notification detail:', error)
  }
}

async function handleMarkAllRead() {
  try {
    await markAllNotificationsReadApi()
    notificationList.value.forEach((item) => (item.status = 'READ'))
    unreadCount.value = 0
    message.success('已全部标记为已读')
  } catch (error: any) {
    message.error(error.message || '操作失败')
  }
}

async function handleDeleteItem(item: NotificationVO) {
  try {
    await deleteNotificationApi(item.id)
    notificationList.value = notificationList.value.filter((n) => n.id !== item.id)
    totalCount.value = Math.max(0, totalCount.value - 1)
    if (item.status === 'UNREAD' && unreadCount.value > 0) {
      unreadCount.value--
    }
    message.success('通知已删除')
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

function initSse() {
  if (!userStore.isLoggedIn) return
  try {
    sseClient = new SseClient('/api/v1/realtime/stream')
    sseClient.on('notification', (data: NotificationVO) => {
      unreadCount.value++
      message.info(`收到新通知: ${data.title}`)
      if (showPopover.value) {
        fetchNotifications()
      }
    })
    sseClient.connect()
  } catch (err) {
    console.error('[SSE] Failed to initialize in notification center:', err)
  }
}

watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      fetchUnreadCount()
      initSse()
    } else {
      unreadCount.value = 0
      notificationList.value = []
      sseClient?.close()
      sseClient = null
    }
  }
)

watch(showPopover, (show) => {
  if (show) {
    fetchNotifications()
  }
})

onMounted(() => {
  if (userStore.isLoggedIn) {
    fetchUnreadCount()
    initSse()
  }
})

onUnmounted(() => {
  sseClient?.close()
  sseClient = null
})
</script>

<style scoped lang="scss">
.notification-center {
  display: inline-flex;
  align-items: center;
}

.badge-wrapper {
  display: inline-flex;
  align-items: center;
}

.bell-btn {
  color: var(--v-text-primary, #333);
}

.notification-panel {
  display: flex;
  flex-direction: column;
  background-color: var(--n-color, #ffffff);
  border-radius: 8px;
  overflow: hidden;

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);

    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);

      .unread-tag {
        font-size: 11px;
        background: rgba(239, 68, 68, 0.1);
        color: #ef4444;
        padding: 1px 6px;
        border-radius: 10px;
        font-weight: 500;
      }
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .panel-tabs {
    padding: 0 12px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  }

  .panel-body {
    min-height: 200px;
    max-height: 360px;
    overflow-y: auto;
    position: relative;

    .loading-state,
    .empty-state {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 200px;
    }

    .list-container {
      display: flex;
      flex-direction: column;

      .notification-item {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 12px 16px;
        cursor: pointer;
        transition: background-color 0.15s ease;
        border-bottom: 1px solid rgba(0, 0, 0, 0.04);
        position: relative;

        &:hover {
          background-color: rgba(0, 0, 0, 0.02);

          .delete-btn {
            opacity: 1;
          }
        }

        &.unread {
          background-color: rgba(99, 102, 241, 0.03);

          .item-title {
            font-weight: 600;
            color: var(--v-text-primary, #111111);
          }
        }

        .item-icon-wrapper {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          background-color: rgba(99, 102, 241, 0.1);
          color: #6366f1;

          &.competition {
            background-color: rgba(245, 158, 11, 0.1);
            color: #f59e0b;
          }

          &.team {
            background-color: rgba(16, 185, 129, 0.1);
            color: #10b981;
          }

          &.award {
            background-color: rgba(236, 72, 153, 0.1);
            color: #ec4899;
          }
        }

        .item-content {
          flex: 1;
          min-width: 0;
          display: flex;
          flex-direction: column;
          gap: 4px;

          .item-title-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 8px;

            .item-title {
              font-size: 13px;
              color: var(--v-text-primary, #222222);
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .item-time {
              font-size: 11px;
              color: #999999;
              flex-shrink: 0;
            }
          }

          .item-desc {
            font-size: 12px;
            color: #444444;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            line-height: 1.4;
          }

          .item-link {
            font-size: 11px;
            color: #2563eb;
            cursor: pointer;
            margin-top: 2px;
            font-weight: 500;

            &:hover {
              text-decoration: underline;
            }
          }
        }

        .item-actions {
          display: flex;
          align-items: center;
          gap: 6px;
          flex-shrink: 0;
          padding-top: 2px;

          .unread-dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background-color: #ef4444;
          }

          .delete-btn {
            opacity: 0;
            transition: opacity 0.15s ease;
          }
        }
      }
    }
  }

  .panel-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 16px;
    border-top: 1px solid rgba(0, 0, 0, 0.04);
    font-size: 12px;
    color: #888888;
  }
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .detail-header {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .detail-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--v-text-primary, #111111);
    }

    .detail-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
      color: #888888;
    }
  }

  .detail-content {
    font-size: 14px;
    color: #444444;
    line-height: 1.6;
    background: rgba(0, 0, 0, 0.02);
    padding: 12px;
    border-radius: 6px;
    white-space: pre-wrap;
  }
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;

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
</style>
