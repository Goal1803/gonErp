<template>
  <div class="clock-widget" @click="goToClock">
    <div class="flex items-center no-wrap q-gutter-sm">
      <!-- Status Icon -->
      <q-icon
        :name="statusIcon"
        :color="statusIconColor"
        size="sm"
      />

      <!-- Status & Time -->
      <div class="column">
        <div class="text-caption text-weight-medium text-white" style="line-height: 1.2;">
          {{ statusText }}
        </div>
        <div class="text-caption text-grey-5" style="line-height: 1.2;">
          {{ elapsedDisplay }}
        </div>
      </div>

      <!-- Quick Action -->
      <q-btn
        v-if="actionLabel"
        :icon="actionIcon"
        :color="actionColor"
        size="sm"
        round
        flat
        :loading="worktimeStore.loading"
        @click.stop="handleAction"
      >
        <q-tooltip>{{ actionLabel }}</q-tooltip>
      </q-btn>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useWorktimeStore } from 'src/stores/worktimeStore'

const router = useRouter()
const $q = useQuasar()
const worktimeStore = useWorktimeStore()

const elapsedSeconds = ref(0)
let elapsedTimer = null

const { clockStatus } = storeToRefs(worktimeStore)

const statusText = computed(() => {
  if (!clockStatus.value || clockStatus.value.status === 'CHECKED_OUT') return 'Not clocked in'
  if (clockStatus.value.status === 'CHECKED_IN') return 'Working'
  if (clockStatus.value.status === 'ON_BREAK') return 'On Break'
  return clockStatus.value.status
})

const statusIcon = computed(() => {
  if (!clockStatus.value || clockStatus.value.status === 'CHECKED_OUT') return 'radio_button_unchecked'
  if (clockStatus.value.status === 'CHECKED_IN') return 'play_circle'
  if (clockStatus.value.status === 'ON_BREAK') return 'pause_circle'
  return 'schedule'
})

const statusIconColor = computed(() => {
  if (!clockStatus.value || clockStatus.value.status === 'CHECKED_OUT') return 'grey-6'
  if (clockStatus.value.status === 'CHECKED_IN') return 'green-5'
  if (clockStatus.value.status === 'ON_BREAK') return 'amber-5'
  return 'grey-6'
})

const actionLabel = computed(() => {
  if (!clockStatus.value || clockStatus.value.status === 'CHECKED_OUT') return 'Check In'
  if (clockStatus.value.status === 'CHECKED_IN') return 'Pause'
  if (clockStatus.value.status === 'ON_BREAK') return 'Resume'
  return null
})

const actionIcon = computed(() => {
  if (!clockStatus.value || clockStatus.value.status === 'CHECKED_OUT') return 'login'
  if (clockStatus.value.status === 'CHECKED_IN') return 'pause'
  if (clockStatus.value.status === 'ON_BREAK') return 'play_arrow'
  return 'schedule'
})

const actionColor = computed(() => {
  if (!clockStatus.value || clockStatus.value.status === 'CHECKED_OUT') return 'green-5'
  if (clockStatus.value.status === 'CHECKED_IN') return 'amber-5'
  if (clockStatus.value.status === 'ON_BREAK') return 'green-5'
  return 'grey-5'
})

const elapsedDisplay = computed(() => {
  if (!clockStatus.value || clockStatus.value.status === 'CHECKED_OUT') return '--:--'
  const hrs = Math.floor(elapsedSeconds.value / 3600)
  const mins = Math.floor((elapsedSeconds.value % 3600) / 60)
  const secs = elapsedSeconds.value % 60
  return `${String(hrs).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
})

function computeElapsed() {
  if (!clockStatus.value || clockStatus.value.status === 'CHECKED_OUT') {
    elapsedSeconds.value = 0
    return
  }
  // If the backend provides elapsedSeconds or checkInTime, calculate from it
  if (clockStatus.value.checkInTime) {
    const checkIn = new Date(clockStatus.value.checkInTime)
    const now = new Date()
    let totalSeconds = Math.floor((now - checkIn) / 1000)
    // Subtract break duration if available
    if (clockStatus.value.breakDurationMinutes) {
      totalSeconds -= clockStatus.value.breakDurationMinutes * 60
    }
    elapsedSeconds.value = Math.max(0, totalSeconds)
  } else if (clockStatus.value.elapsedMinutes != null) {
    elapsedSeconds.value = clockStatus.value.elapsedMinutes * 60
  }
}

async function handleAction() {
  try {
    if (!clockStatus.value || clockStatus.value.status === 'CHECKED_OUT') {
      await worktimeStore.checkIn()
      $q.notify({ type: 'positive', message: 'Checked in' })
    } else if (clockStatus.value.status === 'CHECKED_IN') {
      await worktimeStore.pause()
      $q.notify({ type: 'positive', message: 'Break started' })
    } else if (clockStatus.value.status === 'ON_BREAK') {
      await worktimeStore.resume()
      $q.notify({ type: 'positive', message: 'Work resumed' })
    }
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Action failed' })
  }
}

function goToClock() {
  router.push('/worktime/clock')
}

onMounted(async () => {
  await worktimeStore.fetchClockStatus()
  computeElapsed()
  elapsedTimer = setInterval(() => {
    if (clockStatus.value && clockStatus.value.status !== 'CHECKED_OUT') {
      elapsedSeconds.value++
    }
  }, 1000)
})

onUnmounted(() => {
  if (elapsedTimer) clearInterval(elapsedTimer)
})
</script>

<style scoped>
.clock-widget {
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  background: var(--erp-bg-tertiary);
  border: 1px solid var(--erp-border-subtle);
}
.clock-widget:hover {
  background: var(--erp-hover-bg);
  border-color: var(--erp-border);
}
</style>
