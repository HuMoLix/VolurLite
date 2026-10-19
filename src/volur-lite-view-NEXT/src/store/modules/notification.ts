import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { NotificationVO, NotificationQueryDTO } from '@/types/notification'
import {
  getNotificationListApi,
  getUnreadNotificationCountApi,
  getNotificationDetailApi,
  markAllNotificationsReadApi,
  deleteNotificationApi
} from '@/api/notification'
import { useUserStore } from './user'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationVO[]>([])
  const unreadCount = ref<number>(0)
  const totalCount = ref<number>(0)
  const loading = ref<boolean>(false)

  async function fetchUnreadCount(): Promise<void> {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      unreadCount.value = 0
      return
    }
    try {
      const res = await getUnreadNotificationCountApi()
      if (res && res.data !== undefined) {
        unreadCount.value = Number(res.data)
      }
    } catch {
      unreadCount.value = 0
    }
  }

  async function fetchNotifications(params?: NotificationQueryDTO): Promise<NotificationVO[]> {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      notifications.value = []
      totalCount.value = 0
      return []
    }
    loading.value = true
    try {
      const res = await getNotificationListApi(params)
      if (res && res.data) {
        notifications.value = res.data.list || []
        totalCount.value = res.data.total || 0
        return notifications.value
      }
      return []
    } finally {
      loading.value = false
    }
  }

  async function markAllAsRead(): Promise<void> {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return
    await markAllNotificationsReadApi()
    unreadCount.value = 0
    notifications.value.forEach((item) => {
      item.status = 'READ'
    })
  }

  async function markAsRead(id: number | string): Promise<NotificationVO | null> {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return null
    const res = await getNotificationDetailApi(id)
    const target = notifications.value.find((item) => String(item.id) === String(id))
    if (target && target.status === 'UNREAD') {
      target.status = 'READ'
      if (unreadCount.value > 0) {
        unreadCount.value -= 1
      }
    }
    return res.data || null
  }

  async function deleteNotification(id: number | string): Promise<void> {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return
    await deleteNotificationApi(id)
    const index = notifications.value.findIndex((item) => String(item.id) === String(id))
    if (index !== -1) {
      const removed = notifications.value.splice(index, 1)[0]
      if (removed.status === 'UNREAD' && unreadCount.value > 0) {
        unreadCount.value -= 1
      }
      if (totalCount.value > 0) {
        totalCount.value -= 1
      }
    }
  }

  function reset(): void {
    notifications.value = []
    unreadCount.value = 0
    totalCount.value = 0
    loading.value = false
  }

  return {
    notifications,
    unreadCount,
    totalCount,
    loading,
    fetchUnreadCount,
    fetchNotifications,
    markAllAsRead,
    markAsRead,
    deleteNotification,
    reset
  }
})
