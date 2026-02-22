<template>
  <div
    class="notif-row"
    :class="{ unread: !notification.read }"
    @click="$emit('click', notification)"
  >
    <!-- Avatar -->
    <UserAvatar :user="notification.actor" size="40px" class="notif-avatar" />

    <!-- Main content -->
    <div class="notif-body">
      <div class="notif-actor">{{ actorName }}</div>
      <div class="notif-message" :class="{ 'text-weight-medium': !notification.read }">
        {{ notification.message }}
      </div>
    </div>

    <!-- Card name chip -->
    <div v-if="notification.cardName" class="notif-card-chip">
      <q-icon name="credit_card" size="13px" class="q-mr-xs" />
      <span class="notif-card-name">{{ notification.cardName }}</span>
    </div>

    <!-- Right side: time + unread -->
    <div class="notif-meta">
      <span class="notif-time">{{ timeAgo(notification.createdAt) }}</span>
      <div v-if="!notification.read" class="unread-dot" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import UserAvatar from 'src/components/UserAvatar.vue'

const props = defineProps({
  notification: { type: Object, required: true }
})

defineEmits(['click'])

const actorName = computed(() => {
  const a = props.notification.actor
  if (!a) return ''
  const parts = [a.firstName, a.lastName].filter(Boolean)
  return parts.length ? parts.join(' ') : (a.userName || '')
})

function timeAgo(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now - date
  const diffSec = Math.floor(diffMs / 1000)
  const diffMin = Math.floor(diffSec / 60)
  const diffHr = Math.floor(diffMin / 60)
  const diffDay = Math.floor(diffHr / 24)

  if (diffSec < 60) return 'just now'
  if (diffMin < 60) return `${diffMin}m ago`
  if (diffHr < 24) return `${diffHr}h ago`
  if (diffDay < 7) return `${diffDay}d ago`
  return date.toLocaleDateString()
}
</script>

<style scoped>
.notif-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}
.notif-row:hover {
  background: rgba(255, 255, 255, 0.04);
}
.notif-row.unread {
  background: rgba(38, 166, 154, 0.06);
}
.notif-row.unread:hover {
  background: rgba(38, 166, 154, 0.10);
}

.notif-avatar {
  flex-shrink: 0;
}

/* Body */
.notif-body {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
.notif-actor {
  font-size: 0.8rem;
  font-weight: 600;
  color: #e0e0e0;
  line-height: 1.2;
}
.notif-message {
  font-size: 0.8rem;
  color: #9e9e9e;
  line-height: 1.35;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.notif-row.unread .notif-message {
  color: #bdbdbd;
}

/* Card chip */
.notif-card-chip {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  max-width: 200px;
  padding: 3px 10px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  color: #8e8e8e;
  font-size: 0.72rem;
}
.notif-card-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Right meta */
.notif-meta {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  min-width: 60px;
}
.notif-time {
  font-size: 0.7rem;
  color: #616161;
  white-space: nowrap;
}
.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #26a69a;
}
</style>
