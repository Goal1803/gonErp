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
        <!-- Digital Clock -->
        <q-card class="premium-card q-mb-md" flat>
          <q-card-section class="text-center q-pa-lg">
            <div class="digital-clock text-h2 text-weight-bold" style="color: var(--erp-text); letter-spacing: 4px;">
              {{ currentTime }}
            </div>
            <div class="text-caption text-grey-5 q-mt-sm">{{ currentDate }}</div>

            <!-- Status Badge -->
            <div class="q-mt-md">
              <q-badge
                :color="statusColor"
                :label="statusLabel"
                class="text-weight-medium q-pa-sm"
                style="font-size: 0.85rem;"
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
                    {{ todayEntry?.totalWorkMinutes != null ? formatDuration(todayEntry.totalWorkMinutes) : '--' }}
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
                    {{ todayEntry?.totalBreakMinutes != null ? formatDuration(todayEntry.totalBreakMinutes) : '--' }}
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

const $q = useQuasar()
const worktimeStore = useWorktimeStore()

const currentTime = ref('')
const currentDate = ref('')
const workLocation = ref('OFFICE')
const checkOutNotes = ref('')
const showCheckOutDialog = ref(false)
let clockTimer = null

const locationOptions = [
  { label: 'Office', value: 'OFFICE' },
  { label: 'Remote', value: 'REMOTE' },
  { label: 'On Site', value: 'ON_SITE' }
]

const { clockStatus, todayEntry } = storeToRefs(worktimeStore)

const breakEntries = computed(() => {
  return todayEntry.value?.breaks || []
})

const statusLabel = computed(() => {
  if (!clockStatus.value || clockStatus.value.status === 'CHECKED_OUT') return 'Not Checked In'
  if (clockStatus.value.status === 'CHECKED_IN') return 'Working'
  if (clockStatus.value.status === 'ON_BREAK') return 'On Break'
  return clockStatus.value.status
})

const statusColor = computed(() => {
  if (!clockStatus.value || clockStatus.value.status === 'CHECKED_OUT') return 'grey-7'
  if (clockStatus.value.status === 'CHECKED_IN') return 'green-7'
  if (clockStatus.value.status === 'ON_BREAK') return 'amber-8'
  return 'grey-7'
})

function updateClock() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })
  currentDate.value = now.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
}

function formatTime(timeStr) {
  if (!timeStr) return '--:--'
  // Handle ISO datetime or time-only strings
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

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  worktimeStore.fetchClockStatus()
  worktimeStore.fetchTodayEntry()
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.digital-clock {
  font-family: 'Roboto Mono', monospace;
}
</style>
