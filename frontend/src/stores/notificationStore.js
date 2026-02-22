import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationApi } from 'src/api/notifications'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const importantNotifications = ref([])
  const unreadCount = ref(0)
  const allPage = ref(0)
  const importantPage = ref(0)
  const allHasMore = ref(true)
  const importantHasMore = ref(true)
  const loadingAll = ref(false)
  const loadingImportant = ref(false)

  async function fetchUnreadCount() {
    try {
      const res = await notificationApi.getUnreadCount()
      unreadCount.value = res.data.data.count
    } catch (e) {
      console.error('Failed to fetch unread count', e)
    }
  }

  async function fetchNotifications(reset = false) {
    if (loadingAll.value) return
    if (reset) {
      allPage.value = 0
      allHasMore.value = true
      notifications.value = []
    }
    if (!allHasMore.value) return
    loadingAll.value = true
    try {
      const res = await notificationApi.getAll(allPage.value, 20)
      const page = res.data.data
      if (reset) {
        notifications.value = page.content
      } else {
        notifications.value.push(...page.content)
      }
      allHasMore.value = !page.last
      allPage.value++
    } catch (e) {
      console.error('Failed to fetch notifications', e)
    } finally {
      loadingAll.value = false
    }
  }

  async function fetchImportantNotifications(reset = false) {
    if (loadingImportant.value) return
    if (reset) {
      importantPage.value = 0
      importantHasMore.value = true
      importantNotifications.value = []
    }
    if (!importantHasMore.value) return
    loadingImportant.value = true
    try {
      const res = await notificationApi.getImportant(importantPage.value, 20)
      const page = res.data.data
      if (reset) {
        importantNotifications.value = page.content
      } else {
        importantNotifications.value.push(...page.content)
      }
      importantHasMore.value = !page.last
      importantPage.value++
    } catch (e) {
      console.error('Failed to fetch important notifications', e)
    } finally {
      loadingImportant.value = false
    }
  }

  async function markAsRead(id) {
    try {
      await notificationApi.markAsRead(id)
      const n = notifications.value.find(n => n.id === id)
      if (n && !n.read) {
        n.read = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
      const imp = importantNotifications.value.find(n => n.id === id)
      if (imp) imp.read = true
    } catch (e) {
      console.error('Failed to mark as read', e)
    }
  }

  async function markAllAsRead() {
    try {
      await notificationApi.markAllAsRead()
      notifications.value.forEach(n => { n.read = true })
      importantNotifications.value.forEach(n => { n.read = true })
      unreadCount.value = 0
    } catch (e) {
      console.error('Failed to mark all as read', e)
    }
  }

  function addRealTimeNotification(notification) {
    notifications.value.unshift(notification)
    if (notification.important) {
      importantNotifications.value.unshift(notification)
    }
    unreadCount.value++
  }

  return {
    notifications,
    importantNotifications,
    unreadCount,
    allHasMore,
    importantHasMore,
    loadingAll,
    loadingImportant,
    fetchUnreadCount,
    fetchNotifications,
    fetchImportantNotifications,
    markAsRead,
    markAllAsRead,
    addRealTimeNotification
  }
})
