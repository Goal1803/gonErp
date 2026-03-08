<template>
  <q-page padding>
    <div class="page-header">
      <div class="text-h5 text-white text-weight-light">
        <q-icon name="punch_clock" color="green-5" class="q-mr-sm" />
        Time Clock
      </div>
      <div class="text-caption text-grey-5 q-mt-xs">
        Track your working hours
      </div>
    </div>

    <div class="row q-col-gutter-lg">
      <!-- LEFT: Clock & Actions -->
      <div class="col-12 col-md-6">
        <!-- Real-time Clock + Work Timer -->
        <q-card class="premium-card q-mb-md" flat>
          <q-card-section class="text-center q-pa-lg">
            <!-- Real-time clock in org timezone -->
            <div class="realtime-clock text-h2 text-weight-bold text-white" style="letter-spacing: 6px;">
              {{ liveTime }}
            </div>
            <div class="text-body2 text-grey-4 q-mt-xs">{{ currentDate }}</div>
            <div class="text-caption text-grey-6 q-mt-xs">{{ orgTimezone }}</div>

            <!-- Divider -->
            <q-separator class="q-my-lg" style="opacity: 0.15;" />

            <!-- Work Duration Timer -->
            <div class="text-caption text-grey-5 text-uppercase" style="letter-spacing: 2px;">
              {{ timerLabel }}
            </div>
            <div class="work-timer text-h3 q-mt-sm" :style="{ color: timerColor }">
              {{ workTimerDisplay }}
            </div>

            <!-- Status Badge -->
            <div class="q-mt-md">
              <q-badge
                :color="statusColor"
                :label="statusLabel"
                class="text-weight-medium q-pa-sm"
                style="font-size: 0.85rem; border-radius: 12px;"
              />
            </div>
          </q-card-section>
        </q-card>

        <!-- Action Buttons -->
        <q-card class="premium-card q-mb-md" flat>
          <q-card-section class="q-pa-lg">
            <!-- Not checked in -->
            <template v-if="!clockStatus || !clockStatus.status || clockStatus.status === 'CHECKED_OUT'">
              <q-select
                v-model="workLocation"
                :options="locationOptions"
                label="Work Location"
                outlined
                dense
                emit-value
                map-options
                class="q-mb-md"
              />
              <q-btn
                color="green-7"
                icon="login"
                label="Check In"
                class="full-width"
                size="lg"
                :loading="worktimeStore.loading"
                @click="handleCheckIn"
              />
            </template>

            <!-- Checked in -->
            <template v-else-if="clockStatus.status === 'CHECKED_IN'">
              <div class="row q-gutter-md">
                <div class="col">
                  <q-btn
                    color="amber-8"
                    icon="pause"
                    label="Take a Break"
                    class="full-width"
                    size="lg"
                    :loading="worktimeStore.loading"
                    @click="handlePause"
                  />
                </div>
                <div class="col">
                  <q-btn
                    color="red-7"
                    icon="logout"
                    label="Check Out"
                    class="full-width"
                    size="lg"
                    :loading="worktimeStore.loading"
                    @click="showCheckOutDialog = true"
                  />
                </div>
              </div>
            </template>

            <!-- On break -->
            <template v-else-if="clockStatus.status === 'ON_BREAK'">
              <div class="row q-gutter-md">
                <div class="col">
                  <q-btn
                    color="green-7"
                    icon="play_arrow"
                    label="Resume Work"
                    class="full-width"
                    size="lg"
                    :loading="worktimeStore.loading"
                    @click="handleResume"
                  />
                </div>
                <div class="col">
                  <q-btn
                    color="red-7"
                    icon="logout"
                    label="Check Out"
                    class="full-width"
                    size="lg"
                    :loading="worktimeStore.loading"
                    @click="showCheckOutDialog = true"
                  />
                </div>
              </div>
            </template>
          </q-card-section>
        </q-card>
      </div>

      <!-- RIGHT: Today's Summary -->
      <div class="col-12 col-md-6">
        <!-- Summary Card -->
        <q-card class="premium-card q-mb-md" flat>
          <q-card-section>
            <div class="text-subtitle1 text-white text-weight-bold q-mb-md">
              <q-icon name="summarize" color="orange-5" class="q-mr-sm" />
              Today's Summary
            </div>

            <q-list>
              <q-item>
                <q-item-section avatar>
                  <q-icon name="login" color="green-5" />
                </q-item-section>
                <q-item-section>
                  <q-item-label class="text-white">Check-in Time</q-item-label>
                  <q-item-label caption class="text-grey-5">
                    {{ todayEntry?.checkInTime ? formatTime(todayEntry.checkInTime) : '--:--' }}
                  </q-item-label>
                </q-item-section>
              </q-item>

              <q-item>
                <q-item-section avatar>
                  <q-icon name="logout" color="red-4" />
                </q-item-section>
                <q-item-section>
                  <q-item-label class="text-white">Check-out Time</q-item-label>
                  <q-item-label caption class="text-grey-5">
                    {{ todayEntry?.checkOutTime ? formatTime(todayEntry.checkOutTime) : '--:--' }}
                  </q-item-label>
                </q-item-section>
              </q-item>

              <q-separator class="q-my-sm" />

              <q-item>
                <q-item-section avatar>
                  <q-icon name="work_history" color="blue-4" />
                </q-item-section>
                <q-item-section>
                  <q-item-label class="text-white">Work Duration</q-item-label>
                  <q-item-label caption class="text-grey-5">
                    {{ workDurationDisplay }}
                  </q-item-label>
                </q-item-section>
              </q-item>

              <q-item>
                <q-item-section avatar>
                  <q-icon name="coffee" color="amber-5" />
                </q-item-section>
                <q-item-section>
                  <q-item-label class="text-white">Break Duration</q-item-label>
                  <q-item-label caption class="text-grey-5">
                    {{ breakDurationDisplay }}
                  </q-item-label>
                </q-item-section>
              </q-item>

              <q-item v-if="todayEntry?.workLocation">
                <q-item-section avatar>
                  <q-icon name="location_on" color="purple-4" />
                </q-item-section>
                <q-item-section>
                  <q-item-label class="text-white">Location</q-item-label>
                  <q-item-label caption class="text-grey-5">
                    {{ todayEntry.workLocation }}
                  </q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-card-section>
        </q-card>

        <!-- Break Entries -->
        <q-card class="premium-card" flat>
          <q-card-section>
            <div class="text-subtitle1 text-white text-weight-bold q-mb-md">
              <q-icon name="free_breakfast" color="amber-5" class="q-mr-sm" />
              Break History
            </div>

            <div v-if="!breakEntries.length" class="text-caption text-grey-5 text-center q-pa-md">
              No breaks taken today
            </div>

            <q-list v-else separator>
              <q-item v-for="(brk, idx) in breakEntries" :key="idx">
                <q-item-section avatar>
                  <q-icon name="pause_circle" color="amber-5" />
                </q-item-section>
                <q-item-section>
                  <q-item-label class="text-white">
                    {{ formatTime(brk.startTime) }} - {{ brk.endTime ? formatTime(brk.endTime) : 'Ongoing' }}
                  </q-item-label>
                  <q-item-label caption class="text-grey-5">
                    {{ brk.durationMinutes != null ? formatDuration(brk.durationMinutes) : 'In progress...' }}
                  </q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <!-- Break Reminder Dialog -->
    <q-dialog v-model="showBreakReminder" persistent>
      <q-card style="min-width: 400px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section class="text-center q-pa-lg">
          <q-icon name="coffee" color="amber-5" size="48px" />
          <div class="text-h6 text-white q-mt-md">Time for a Break!</div>
          <div class="text-body2 text-grey-4 q-mt-sm">
            You've been working for over 4 hours without a break. Taking regular breaks helps maintain productivity and well-being.
          </div>
        </q-card-section>
        <q-card-actions align="center" class="q-pb-lg q-gutter-md">
          <q-btn flat label="Ignore" color="grey-5" no-caps @click="dismissBreakReminder" />
          <q-btn unelevated label="Take a Break" color="amber-8" icon="pause" no-caps @click="takeBreakFromReminder" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Force Checkout Dialog -->
    <q-dialog v-model="showForceCheckoutWarning" persistent>
      <q-card style="min-width: 400px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section class="text-center q-pa-lg">
          <q-icon name="midnight" color="red-4" size="48px" />
          <div class="text-h6 text-white q-mt-md">Automatically Checked Out</div>
          <div class="text-body2 text-grey-4 q-mt-sm">
            You were automatically checked out at midnight. Time entries cannot span two days. If you need to continue working, please check in again for the new day.
          </div>
        </q-card-section>
        <q-card-actions align="center" class="q-pb-lg">
          <q-btn unelevated label="OK" color="green-7" no-caps v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Check-out Dialog -->
    <q-dialog v-model="showCheckOutDialog">
      <q-card style="min-width: 400px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Check Out</div>
        </q-card-section>
        <q-card-section>
          <q-input
            v-model="checkOutNotes"
            label="Daily Notes (optional)"
            outlined
            dense
            type="textarea"
            autogrow
            :input-style="{ minHeight: '80px' }"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup />
          <q-btn
            color="red-7"
            label="Check Out"
            icon="logout"
            :loading="worktimeStore.loading"
            @click="handleCheckOut"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useQuasar } from 'quasar'
import { useWorktimeStore } from 'src/stores/worktimeStore'
import { worktimeUserConfigApi } from 'src/api/worktime'

const $q = useQuasar()
const worktimeStore = useWorktimeStore()
const userConfig = ref(null)

const liveTime = ref('')
const currentDate = ref('')
const workSeconds = ref(0)
const breakSeconds = ref(0)
const workLocation = ref('OFFICE')
const checkOutNotes = ref('')
const showCheckOutDialog = ref(false)
const showBreakReminder = ref(false)
const showForceCheckoutWarning = ref(false)
let clockTimer = null
let breakReminderDismissed = false
let lastCheckedDate = ''

// BroadcastChannel for cross-tab break reminder sync
let breakChannel = null
if (typeof BroadcastChannel !== 'undefined') {
  breakChannel = new BroadcastChannel('worktime-break-reminder')
  breakChannel.onmessage = (event) => {
    if (event.data?.type === 'BREAK_STARTED' || event.data?.type === 'BREAK_DISMISSED') {
      showBreakReminder.value = false
      if (event.data.type === 'BREAK_DISMISSED') breakReminderDismissed = true
    }
    if (event.data?.type === 'BREAK_STARTED') {
      // Refresh status since break was started from another tab
      worktimeStore.fetchClockStatus()
      worktimeStore.fetchTodayEntry()
    }
    if (event.data?.type === 'FORCE_CHECKOUT') {
      showForceCheckoutWarning.value = true
      worktimeStore.fetchClockStatus()
      worktimeStore.fetchTodayEntry()
    }
  }
}

const locationOptions = [
  { label: 'Office', value: 'OFFICE' },
  { label: 'Remote', value: 'REMOTE' },
  { label: 'On Site', value: 'ON_SITE' }
]

const { clockStatus, todayEntry } = storeToRefs(worktimeStore)

const orgTimezone = computed(() => userConfig.value?.timezoneId || worktimeStore.settings?.timezoneId || 'Asia/Ho_Chi_Minh')

const breakEntries = computed(() => {
  return todayEntry.value?.breaks || []
})

const statusLabel = computed(() => {
  if (!clockStatus.value || !clockStatus.value.status) return 'Not Checked In'
  if (clockStatus.value.status === 'CHECKED_OUT') return 'Checked Out'
  if (clockStatus.value.status === 'CHECKED_IN') return 'Working'
  if (clockStatus.value.status === 'ON_BREAK') return 'On Break'
  return clockStatus.value.status
})

const statusColor = computed(() => {
  if (!clockStatus.value || !clockStatus.value.status) return 'grey-7'
  if (clockStatus.value.status === 'CHECKED_OUT') return 'grey-7'
  if (clockStatus.value.status === 'CHECKED_IN') return 'green-7'
  if (clockStatus.value.status === 'ON_BREAK') return 'amber-8'
  return 'grey-7'
})

const timerLabel = computed(() => {
  const s = clockStatus.value?.status
  if (s === 'ON_BREAK') return 'Working Time (paused)'
  if (s === 'CHECKED_IN') return 'Working Time'
  if (s === 'CHECKED_OUT') return 'Total Worked Today'
  return 'Working Time'
})

const timerColor = computed(() => {
  const s = clockStatus.value?.status
  if (s === 'CHECKED_IN') return '#4caf50'
  if (s === 'ON_BREAK') return '#ffc107'
  return 'rgba(255,255,255,0.5)'
})

function pad2(n) {
  return String(n).padStart(2, '0')
}

const workTimerDisplay = computed(() => {
  const s = workSeconds.value
  return `${pad2(Math.floor(s / 3600))}:${pad2(Math.floor((s % 3600) / 60))}:${pad2(s % 60)}`
})

const workDurationDisplay = computed(() => {
  const s = workSeconds.value
  if (s === 0 && (!clockStatus.value?.status || clockStatus.value.status === 'CHECKED_OUT')) {
    const mins = todayEntry.value?.totalWorkMinutes
    return mins != null ? formatDuration(mins) : '--'
  }
  const hrs = Math.floor(s / 3600)
  const mins = Math.floor((s % 3600) / 60)
  if (hrs > 0) return `${hrs}h ${mins}m`
  return `${mins}m`
})

const breakDurationDisplay = computed(() => {
  const s = breakSeconds.value
  if (s === 0) {
    const mins = todayEntry.value?.totalBreakMinutes
    return mins != null ? formatDuration(mins) : '--'
  }
  const hrs = Math.floor(s / 3600)
  const mins = Math.floor((s % 3600) / 60)
  if (hrs > 0) return `${hrs}h ${mins}m`
  return `${mins}m`
})

function computeTimers() {
  const entry = todayEntry.value
  const status = clockStatus.value?.status

  // Not checked in yet
  if (!entry || !entry.checkInTime || !status) {
    workSeconds.value = 0
    breakSeconds.value = 0
    return
  }

  // Already checked out — show final totals
  if (status === 'CHECKED_OUT') {
    workSeconds.value = (entry.totalWorkMinutes || 0) * 60
    breakSeconds.value = (entry.totalBreakMinutes || 0) * 60
    return
  }

  // Currently working or on break — compute live elapsed
  const checkIn = new Date(entry.checkInTime)
  const now = new Date()
  const totalElapsed = Math.max(0, Math.floor((now - checkIn) / 1000))

  // Total completed break seconds
  let totalBreakSecs = (entry.totalBreakMinutes || 0) * 60

  // If on break, add the ongoing open break duration
  if (status === 'ON_BREAK') {
    const openBreak = (entry.breaks || []).find(b => !b.endTime)
    if (openBreak) {
      const breakStart = new Date(openBreak.startTime)
      totalBreakSecs += Math.max(0, Math.floor((now - breakStart) / 1000))
    }
  }

  breakSeconds.value = totalBreakSecs
  workSeconds.value = Math.max(0, totalElapsed - totalBreakSecs)
}

function tick() {
  const now = new Date()
  const tz = orgTimezone.value
  liveTime.value = now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false, timeZone: tz })
  currentDate.value = now.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric', timeZone: tz })
  computeTimers()
  checkBreakReminder()
  checkMidnightForceCheckout(tz)
}

function checkBreakReminder() {
  if (breakReminderDismissed) return
  const status = clockStatus.value?.status
  if (status !== 'CHECKED_IN') return

  const entry = todayEntry.value
  if (!entry || !entry.checkInTime) return

  // Compute continuous work time (since last break end or check-in)
  const breaks = entry.breaks || []
  const completedBreaks = breaks.filter(b => b.endTime)
  let lastBreakEnd = null
  if (completedBreaks.length > 0) {
    lastBreakEnd = completedBreaks.reduce((latest, b) => {
      const t = new Date(b.endTime).getTime()
      return t > latest ? t : latest
    }, 0)
  }

  const sinceTime = lastBreakEnd ? new Date(lastBreakEnd) : new Date(entry.checkInTime)
  const continuousMinutes = (Date.now() - sinceTime.getTime()) / 60000

  if (continuousMinutes >= 240) {
    showBreakReminder.value = true
  }
}

function checkMidnightForceCheckout(tz) {
  const status = clockStatus.value?.status
  if (status !== 'CHECKED_IN' && status !== 'ON_BREAK') return

  const entry = todayEntry.value
  if (!entry || !entry.workDate) return

  // Get current date in org timezone
  const now = new Date()
  const todayInTz = now.toLocaleDateString('en-CA', { timeZone: tz }) // YYYY-MM-DD format

  if (todayInTz !== entry.workDate) {
    // Date has changed — force checkout happened (or needs to happen)
    showForceCheckoutWarning.value = true
    if (breakChannel) breakChannel.postMessage({ type: 'FORCE_CHECKOUT' })
    // Refresh from backend (scheduler will have force-checked-out)
    worktimeStore.fetchClockStatus()
    worktimeStore.fetchTodayEntry()
  }
}

function formatTime(timeStr) {
  if (!timeStr) return '--:--'
  try {
    const date = new Date(timeStr)
    if (!isNaN(date.getTime())) {
      return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: orgTimezone.value })
    }
  } catch {
    // fallback
  }
  return timeStr
}

function formatDuration(minutes) {
  if (minutes == null) return '--'
  const hrs = Math.floor(minutes / 60)
  const mins = minutes % 60
  if (hrs > 0) return `${hrs}h ${mins}m`
  return `${mins}m`
}

async function handleCheckIn() {
  try {
    await worktimeStore.checkIn({ workLocation: workLocation.value })
    $q.notify({ type: 'positive', message: 'Checked in successfully' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to check in' })
  }
}

async function handlePause() {
  try {
    await worktimeStore.pause()
    $q.notify({ type: 'positive', message: 'Break started' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to start break' })
  }
}

async function handleResume() {
  try {
    await worktimeStore.resume()
    $q.notify({ type: 'positive', message: 'Work resumed' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to resume' })
  }
}

async function takeBreakFromReminder() {
  showBreakReminder.value = false
  if (breakChannel) breakChannel.postMessage({ type: 'BREAK_STARTED' })
  await handlePause()
}

function dismissBreakReminder() {
  showBreakReminder.value = false
  breakReminderDismissed = true
  if (breakChannel) breakChannel.postMessage({ type: 'BREAK_DISMISSED' })
}

async function handleCheckOut() {
  try {
    await worktimeStore.checkOut({ dailyNotes: checkOutNotes.value || null })
    showCheckOutDialog.value = false
    checkOutNotes.value = ''
    $q.notify({ type: 'positive', message: 'Checked out successfully' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to check out' })
  }
}

onMounted(async () => {
  tick()
  await Promise.all([
    worktimeStore.fetchClockStatus(),
    worktimeStore.fetchTodayEntry(),
    worktimeStore.fetchSettings(),
    worktimeUserConfigApi.getMyConfig().then(res => { userConfig.value = res.data.data }).catch(() => {})
  ])
  computeTimers()
  clockTimer = setInterval(tick, 1000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (breakChannel) breakChannel.close()
})
</script>

<style scoped>
.realtime-clock {
  font-family: 'Roboto Mono', monospace;
  letter-spacing: 6px;
  text-shadow: 0 0 20px rgba(255, 255, 255, 0.1);
}

.work-timer {
  font-family: 'Roboto Mono', monospace;
  letter-spacing: 4px;
  font-weight: 700;
}
</style>
