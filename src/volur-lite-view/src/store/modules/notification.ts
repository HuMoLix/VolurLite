import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { NotificationVO, BroadcastMessageVO } from '@/types/notification'
import type { AdminAsyncTaskVO } from '@/types/task'
import {
  getNotificationListApi,
  getUnreadNotificationCountApi,
  getAdminBroadcastListApi
} from '@/api/notification'
import { getGlobalSseClient } from '@/utils/sse'
import { useUserStore } from './user'

const DISMISSED_BROADCAST_STORAGE_KEY = 'volur_dismissed_broadcasts'

function getDismissedBroadcastIds(): Set<string> {
  try {
    const raw = sessionStorage.getItem(DISMISSED_BROADCAST_STORAGE_KEY)
    if (!raw) return new Set()
    return new Set(JSON.parse(raw))
  } catch {
    return new Set()
  }
}

function markBroadcastDismissed(id: number | string): void {
  try {
    const set = getDismissedBroadcastIds()
    set.add(String(id))
    sessionStorage.setItem(DISMISSED_BROADCAST_STORAGE_KEY, JSON.stringify(Array.from(set)))
  } catch {
    // ignore
  }
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationVO[]>([])
  const broadcasts = ref<BroadcastMessageVO[]>([])
  const unreadCount = ref<number>(0)

  // Centered Modal Broadcast State
  const currentBroadcast = ref<BroadcastMessageVO | null>(null)
  const showBroadcastModal = ref<boolean>(false)

  // SSE Connection State
  const isSseConnected = ref<boolean>(false)
  let unsubscribers: Array<() => void> = []

  async function fetchUnreadCount(): Promise<void> {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      unreadCount.value = 0
      return
    }
    try {
      const res = await getUnreadNotificationCountApi()
      if (res.data !== undefined) {
        unreadCount.value = Number(res.data)
      }
    } catch (e) {
      console.error('[NotificationStore] Failed to fetch unread count:', e)
    }
  }

  async function fetchNotifications(size = 10): Promise<void> {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return
    try {
      const res = await getNotificationListApi({ size })
      if (res.data) {
        notifications.value = res.data.list || []
      }
    } catch (e) {
      console.error('[NotificationStore] Failed to fetch notifications:', e)
    }
  }

  async function fetchBroadcasts(size = 10): Promise<void> {
    try {
      const res = await getAdminBroadcastListApi({ size })
      if (res.data) {
        broadcasts.value = res.data.list || []
      }
    } catch (e) {
      console.error('[NotificationStore] Failed to fetch broadcasts:', e)
    }
  }

  function openBroadcastModal(broadcast: BroadcastMessageVO): void {
    currentBroadcast.value = broadcast
    showBroadcastModal.value = true
  }

  function closeBroadcastModal(): void {
    if (currentBroadcast.value?.id) {
      markBroadcastDismissed(currentBroadcast.value.id)
    }
    showBroadcastModal.value = false
    currentBroadcast.value = null
  }

  function handleIncomingNotification(notif: NotificationVO): void {
    if (!notif) return
    notifications.value.unshift(notif)
    unreadCount.value += 1
    if (window.$message) {
      window.$message.info(notif.title ? `收到新通知: ${notif.title}` : '收到新通知')
    }
    window.dispatchEvent(new CustomEvent('volur:new-notification', { detail: notif }))
  }

  function handleIncomingBroadcast(broadcast: BroadcastMessageVO): void {
    if (!broadcast) return
    broadcasts.value.unshift(broadcast)

    const displayType = broadcast.displayType || 'BANNER'
    if (displayType === 'MODAL') {
      const dismissedIds = getDismissedBroadcastIds()
      const isAlreadyDismissed = broadcast.id ? dismissedIds.has(String(broadcast.id)) : false
      // 满足强制弹出，或本次会话尚未弹窗过
      if (broadcast.forcePopup || !isAlreadyDismissed) {
        openBroadcastModal(broadcast)
      }
    } else if (displayType === 'BANNER') {
      window.$notification?.info({
        title: broadcast.title || '系统公告',
        content: broadcast.content,
        duration: broadcast.forcePopup ? 0 : 8000
      })
    } else if (displayType === 'TOAST') {
      window.$message?.info(`[公告] ${broadcast.title}: ${broadcast.content}`)
    } else if (displayType === 'DRAWER') {
      window.$notification?.create({
        title: broadcast.title || '系统广播',
        content: broadcast.content,
        duration: 10000
      })
    }

    window.dispatchEvent(new CustomEvent('volur:broadcast-received', { detail: broadcast }))
  }

  function handleIncomingTaskEvent(task: AdminAsyncTaskVO): void {
    if (!task) return
    const userStore = useUserStore()
    // 异步任务面向管理员下发与提示
    if (userStore.isAdmin) {
      if (task.status === 'SUCCESS') {
        const durationText = task.executionDurationMs
          ? ` (耗时: ${(task.executionDurationMs / 1000).toFixed(1)}s)`
          : ''
        window.$notification?.success({
          title: '任务执行完成',
          content: `${task.taskName}${durationText}`,
          duration: 4500
        })
        window.dispatchEvent(new CustomEvent('volur:async-task-completed', { detail: task }))
      } else if (task.status === 'FAILED') {
        window.$notification?.error({
          title: '任务执行失败',
          content: `${task.taskName}: ${task.errorMessage || '未知异常'}`,
          duration: 6000
        })
        window.dispatchEvent(new CustomEvent('volur:async-task-failed', { detail: task }))
      }
    }
  }

  /**
   * 初始化全局 SSE 实时长连接
   * 铁律：仅在用户登录后或携带登录凭证时调用！
   */
  function initSse(): void {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      return
    }

    if (isSseConnected.value) {
      return
    }

    try {
      const sse = getGlobalSseClient()

      // 清理既有监听，防止重复绑定
      unsubscribers.forEach((unsub) => unsub())
      unsubscribers = []

      // 注册核心业务事件监听
      unsubscribers.push(sse.on('notification', (data: NotificationVO) => handleIncomingNotification(data)))
      unsubscribers.push(sse.on('announcement', (data: BroadcastMessageVO) => handleIncomingBroadcast(data)))
      unsubscribers.push(sse.on('promotion', (data: BroadcastMessageVO) => handleIncomingBroadcast(data)))
      unsubscribers.push(sse.on('task_event', (data: AdminAsyncTaskVO) => handleIncomingTaskEvent(data)))

      sse.connect()
      isSseConnected.value = true

      // 建立连接后拉取未读数
      fetchUnreadCount()
    } catch (err) {
      console.error('[NotificationStore] Failed to initialize SSE:', err)
    }
  }

  /**
   * 销毁并断开全局 SSE 实时长连接（用户退出登录时调用）
   */
  function destroySse(): void {
    try {
      unsubscribers.forEach((unsub) => unsub())
      unsubscribers = []

      const sse = getGlobalSseClient()
      sse.close()

      isSseConnected.value = false
      unreadCount.value = 0
      notifications.value = []
      showBroadcastModal.value = false
      currentBroadcast.value = null
    } catch (err) {
      console.warn('[NotificationStore] Error closing SSE:', err)
    }
  }

  return {
    notifications,
    broadcasts,
    unreadCount,
    currentBroadcast,
    showBroadcastModal,
    isSseConnected,
    fetchUnreadCount,
    fetchNotifications,
    fetchBroadcasts,
    openBroadcastModal,
    closeBroadcastModal,
    handleIncomingNotification,
    handleIncomingBroadcast,
    handleIncomingTaskEvent,
    initSse,
    destroySse
  }
})
