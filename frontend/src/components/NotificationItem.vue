<template>
  <q-item
    clickable
    v-ripple
    class="notification-item"
    :class="{ unread: !notification.read }"
    @click="$emit('click', notification)"
  >
    <q-item-section avatar>
      <UserAvatar :user="notification.actor" size="36px" />
    </q-item-section>

    <q-item-section>
      <q-item-label
        class="notification-message"
        :class="{ 'text-weight-bold': !notification.read }"
      >
        {{ notification.message }}
      </q-item-label>
      <q-item-label caption class="flex items-center gap-1 q-mt-xs">
        <q-icon
          v-if="notification.important"
          name="priority_high"
          color="orange-5"
          size="14px"
        />
        <span class="text-grey-5">{{ timeAgo(notification.createdAt) }}</span>
      </q-item-label>
    </q-item-section>

    <q-item-section v-if="!notification.read" side>
      <div class="unread-dot"></div>
    </q-item-section>
  </q-item>
</template>

<script setup>
import UserAvatar from 'src/components/UserAvatar.vue'

defineProps({
  notification: { type: Object, required: true }
})

defineEmits(['click'])

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
.notification-item {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  min-height: 64px;
}
.notification-item.unread {
  background: rgba(38, 166, 154, 0.08);
}
.notification-item:hover {
  background: rgba(255, 255, 255, 0.05);
}
.notification-message {
  font-size: 0.82rem;
  line-height: 1.35;
  color: #e0e0e0;
}
.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #26a69a;
}
</style>
