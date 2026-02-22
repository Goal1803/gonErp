<template>
  <q-btn flat round dense icon="notifications" color="white">
    <q-badge v-if="store.unreadCount > 0" color="red" floating rounded>
      {{ store.unreadCount > 99 ? '99+' : store.unreadCount }}
    </q-badge>

    <q-menu
      class="notification-menu"
      anchor="bottom right"
      self="top right"
      :offset="[0, 8]"
      @before-show="onMenuOpen"
    >
      <!-- Header row -->
      <div class="notif-header">
        <div class="notif-header-left">
          <q-icon name="notifications" size="22px" color="teal-4" />
          <span class="notif-title">Notifications</span>
          <q-badge
            v-if="store.unreadCount > 0"
            color="red"
            rounded
            class="q-ml-sm"
            :label="store.unreadCount > 99 ? '99+' : store.unreadCount"
          />
        </div>
        <div class="notif-header-right">
          <q-btn
            v-if="store.unreadCount > 0"
            flat dense no-caps
            icon="done_all"
            label="Mark all read"
            color="teal-4"
            size="sm"
            @click="store.markAllAsRead()"
          />
        </div>
      </div>

      <!-- Tabs -->
      <div class="notif-tabs-bar">
        <q-tabs
          v-model="tab"
          dense
          no-caps
          active-color="teal-4"
          indicator-color="teal-4"
          class="notif-tabs"
          inline-label
        >
          <q-tab name="all" icon="inbox" label="All" />
          <q-tab name="important" icon="priority_high" label="Important" />
        </q-tabs>
      </div>

      <!-- Content area -->
      <q-scroll-area style="height: 460px">
        <q-tab-panels v-model="tab" animated class="bg-transparent">
          <!-- All Tab -->
          <q-tab-panel name="all" class="q-pa-none">
            <template v-if="store.notifications.length > 0">
              <NotificationItem
                v-for="n in store.notifications"
                :key="n.id"
                :notification="n"
                @click="handleClick"
              />
            </template>
            <div v-else-if="!store.loadingAll" class="notif-empty">
              <q-icon name="notifications_none" size="48px" color="grey-7" />
              <div class="text-grey-5 q-mt-sm" style="font-size: 0.85rem">No notifications yet</div>
            </div>
            <div v-if="store.allHasMore" class="text-center q-py-md">
              <q-btn
                flat dense no-caps
                label="Load more"
                color="teal-4"
                size="sm"
                :loading="store.loadingAll"
                @click="store.fetchNotifications()"
              />
            </div>
          </q-tab-panel>

          <!-- Important Tab -->
          <q-tab-panel name="important" class="q-pa-none">
            <template v-if="store.importantNotifications.length > 0">
              <NotificationItem
                v-for="n in store.importantNotifications"
                :key="n.id"
                :notification="n"
                @click="handleClick"
              />
            </template>
            <div v-else-if="!store.loadingImportant" class="notif-empty">
              <q-icon name="star_border" size="48px" color="grey-7" />
              <div class="text-grey-5 q-mt-sm" style="font-size: 0.85rem">No important notifications</div>
            </div>
            <div v-if="store.importantHasMore" class="text-center q-py-md">
              <q-btn
                flat dense no-caps
                label="Load more"
                color="teal-4"
                size="sm"
                :loading="store.loadingImportant"
                @click="store.fetchImportantNotifications()"
              />
            </div>
          </q-tab-panel>
        </q-tab-panels>
      </q-scroll-area>
    </q-menu>
  </q-btn>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from 'src/stores/notificationStore'
import NotificationItem from 'src/components/NotificationItem.vue'

const router = useRouter()
const store = useNotificationStore()
const tab = ref('all')

function onMenuOpen() {
  store.fetchNotifications(true)
  store.fetchImportantNotifications(true)
}

function handleClick(notification) {
  if (!notification.read) {
    store.markAsRead(notification.id)
  }
  if (notification.boardId && notification.cardId) {
    const currentRoute = router.currentRoute.value
    const targetBoardPath = `/tasks/${notification.boardId}`
    const designBoardPath = `/designs/boards/${notification.boardId}`

    // Check if we're already on this board page
    if (currentRoute.path === targetBoardPath || currentRoute.path === designBoardPath) {
      // Same board — just update the query to open the card
      router.replace({ query: { cardId: notification.cardId } })
    } else {
      // Try tasks path first (most common)
      router.push({ path: targetBoardPath, query: { cardId: notification.cardId } })
    }
  }
}
</script>

<!-- Unscoped: q-menu is teleported to body, scoped styles won't reach it -->
<style>
.notification-menu {
  width: 760px !important;
  max-width: 95vw !important;
  background: #141414;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.5);
}
.notification-menu .notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 12px;
}
.notification-menu .notif-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.notification-menu .notif-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: #f5f5f5;
}
.notification-menu .notif-header-right {
  display: flex;
  align-items: center;
}
.notification-menu .notif-tabs-bar {
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.notification-menu .notif-tabs {
  max-width: 280px;
}
.notification-menu .notif-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}
</style>
