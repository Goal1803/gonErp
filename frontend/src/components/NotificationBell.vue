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
      <!-- Header -->
      <div class="notification-header">
        <div class="text-weight-bold text-white" style="font-size: 1rem">Notifications</div>
        <q-btn
          v-if="store.unreadCount > 0"
          flat
          dense
          no-caps
          label="Mark all read"
          color="teal-4"
          size="sm"
          @click="store.markAllAsRead()"
        />
      </div>

      <!-- Tabs -->
      <q-tabs v-model="tab" dense active-color="teal-4" indicator-color="teal-4" class="notification-tabs">
        <q-tab name="all" label="All" />
        <q-tab name="important" label="Important" />
      </q-tabs>

      <!-- Content -->
      <q-scroll-area style="height: 380px">
        <q-tab-panels v-model="tab" animated class="bg-transparent">
          <!-- All Tab -->
          <q-tab-panel name="all" class="q-pa-none">
            <q-list v-if="store.notifications.length > 0">
              <NotificationItem
                v-for="n in store.notifications"
                :key="n.id"
                :notification="n"
                @click="handleClick"
              />
            </q-list>
            <div v-else-if="!store.loadingAll" class="text-center text-grey-5 q-pa-lg">
              No notifications
            </div>
            <div v-if="store.allHasMore" class="text-center q-py-sm">
              <q-btn
                flat
                dense
                no-caps
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
            <q-list v-if="store.importantNotifications.length > 0">
              <NotificationItem
                v-for="n in store.importantNotifications"
                :key="n.id"
                :notification="n"
                @click="handleClick"
              />
            </q-list>
            <div v-else-if="!store.loadingImportant" class="text-center text-grey-5 q-pa-lg">
              No important notifications
            </div>
            <div v-if="store.importantHasMore" class="text-center q-py-sm">
              <q-btn
                flat
                dense
                no-caps
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

<style scoped>
.notification-menu {
  width: 380px;
  max-width: 95vw;
  background: #1a1a1a;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
}
.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 8px;
}
.notification-tabs {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
</style>
