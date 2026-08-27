import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { NotificationVO, BroadcastMessageVO } from '@/types/notification'
import {
  getNotificationListApi,
  getUnreadNotificationCountApi,
  getAdminBroadcastListApi
} from '@/api/notification'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationVO[]>([])
  const broadcasts = ref<BroadcastMessageVO[]>([])
  const unreadCount = ref<number>(0)

  async function fetchNotifications(): Promise<void> {
    try {
      const [listRes, countRes] = await Promise.all([
        getNotificationListApi({ size: 10 }),
        getUnreadNotificationCountApi()
      ])
      notifications.value = listRes.data.list
      unreadCount.value = countRes.data
    } catch (e) {
      console.error('Failed to fetch notifications', e)
    }
  }

  async function fetchBroadcasts(): Promise<void> {
    try {
      const res = await getAdminBroadcastListApi({ size: 10 })
      broadcasts.value = res.data.list
    } catch (e) {
      console.error('Failed to fetch broadcasts', e)
    }
  }

  function addBroadcast(msg: BroadcastMessageVO): void {
    broadcasts.value.unshift(msg)
  }

  function addNotification(notif: NotificationVO): void {
    notifications.value.unshift(notif)
    unreadCount.value += 1
  }

  return {
    notifications,
    broadcasts,
    unreadCount,
    fetchNotifications,
    fetchBroadcasts,
    addBroadcast,
    addNotification
  }
})
