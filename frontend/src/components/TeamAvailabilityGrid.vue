<template>
  <div class="availability-grid">
    <div
      v-for="member in sortedMembers"
      :key="member.userId"
      class="member-card premium-card"
    >
      <div class="card-inner">
        <!-- Avatar -->
        <q-avatar size="44px" class="member-avatar">
          <img v-if="member.avatarUrl" :src="member.avatarUrl" :alt="displayName(member)" />
          <span v-else class="avatar-initials">{{ initials(member) }}</span>
        </q-avatar>

        <!-- Info -->
        <div class="member-info">
          <div class="member-name text-adaptive text-weight-medium">{{ displayName(member) }}</div>
          <div class="member-username text-adaptive-caption">@{{ member.userName }}</div>
        </div>

        <!-- Status Badge -->
        <q-badge
          :color="statusColor(member.status)"
          :label="statusLabel(member.status)"
          class="status-badge"
        />
      </div>

      <!-- Details row -->
      <div v-if="member.status === 'WORKING' || member.status === 'ON_BREAK'" class="card-details">
        <div v-if="member.checkInTime" class="detail-item text-adaptive-caption">
          <q-icon name="login" size="14px" color="green-5" class="q-mr-xs" />
          {{ formatTime(member.checkInTime) }}
        </div>
        <div v-if="member.workLocation" class="detail-item text-adaptive-caption">
          <q-icon :name="locationIcon(member.workLocation)" size="14px" color="blue-4" class="q-mr-xs" />
          {{ member.workLocation }}
        </div>
      </div>
    </div>

    <div v-if="!members || !members.length" class="text-center q-pa-xl full-width">
      <q-icon name="groups" color="grey-6" size="48px" />
      <div class="text-subtitle1 text-adaptive q-mt-md">No team members found</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  members: {
    type: Array,
    default: () => []
  }
})

const statusOrder = {
  WORKING: 0,
  ON_BREAK: 1,
  NOT_CHECKED_IN: 2,
  DAY_OFF: 3,
  OFF: 4
}

const sortedMembers = computed(() => {
  if (!props.members) return []
  return [...props.members].sort((a, b) => {
    const orderA = statusOrder[a.status] ?? 99
    const orderB = statusOrder[b.status] ?? 99
    if (orderA !== orderB) return orderA - orderB
    return displayName(a).localeCompare(displayName(b))
  })
})

function displayName(member) {
  const parts = []
  if (member.firstName) parts.push(member.firstName)
  if (member.lastName) parts.push(member.lastName)
  return parts.length > 0 ? parts.join(' ') : member.userName
}

function initials(member) {
  const first = (member.firstName || member.userName || '?')[0]
  const last = (member.lastName || '')[0] || ''
  return (first + last).toUpperCase()
}

function statusColor(status) {
  switch (status) {
    case 'WORKING': return 'green-7'
    case 'ON_BREAK': return 'amber-8'
    case 'DAY_OFF': return 'blue-7'
    case 'OFF': return 'grey-7'
    case 'NOT_CHECKED_IN': return 'grey-6'
    default: return 'grey-6'
  }
}

function statusLabel(status) {
  switch (status) {
    case 'WORKING': return 'Working'
    case 'ON_BREAK': return 'On Break'
    case 'DAY_OFF': return 'Day Off'
    case 'OFF': return 'Checked Out'
    case 'NOT_CHECKED_IN': return 'Not Checked In'
    default: return status
  }
}

function formatTime(timeStr) {
  if (!timeStr) return '--:--'
  try {
    const date = new Date(timeStr)
    if (!isNaN(date.getTime())) {
      return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false })
    }
  } catch {
    // fallback
  }
  return timeStr
}

function locationIcon(location) {
  switch (location) {
    case 'OFFICE': return 'business'
    case 'REMOTE': return 'home'
    case 'HYBRID': return 'sync_alt'
    default: return 'location_on'
  }
}
</script>

<style scoped>
.availability-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

@media (max-width: 1200px) {
  .availability-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .availability-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .availability-grid {
    grid-template-columns: 1fr;
  }
}

.member-card {
  background: var(--erp-bg-tertiary);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 10px;
  padding: 14px;
  transition: border-color 0.2s;
}

.member-card:hover {
  border-color: rgba(102, 187, 106, 0.3);
}

.card-inner {
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-avatar {
  flex-shrink: 0;
  background: var(--erp-bg-elevated, #2a2a3e);
  color: var(--erp-text-secondary);
  font-size: 0.85rem;
  font-weight: 600;
}

.avatar-initials {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 0.9rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-username {
  font-size: 0.75rem;
}

.status-badge {
  flex-shrink: 0;
  font-size: 0.7rem;
  padding: 4px 8px;
}

.card-details {
  display: flex;
  gap: 12px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--erp-border-subtle);
}

.detail-item {
  display: flex;
  align-items: center;
  font-size: 0.75rem;
}
</style>
