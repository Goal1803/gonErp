<template>
  <q-page padding>
    <div class="page-header">
      <div class="row items-center justify-between">
        <div>
          <div class="text-h5 text-adaptive text-weight-light">
            <q-icon name="calendar_month" color="purple-4" class="q-mr-sm" />
            Team Calendar
          </div>
          <div class="text-caption text-adaptive-caption q-mt-xs">
            View team day-off schedule at a glance
          </div>
        </div>

        <div class="row items-center q-gutter-sm">
          <!-- Month navigation -->
          <q-btn
            flat
            round
            dense
            icon="chevron_left"
            color="grey-5"
            @click="navigateMonth(-1)"
          >
            <q-tooltip>Previous Month</q-tooltip>
          </q-btn>

          <q-btn
            flat
            dense
            no-caps
            class="text-adaptive text-weight-medium"
            style="min-width: 120px"
            @click="goToToday"
          >
            {{ monthYearLabel }}
          </q-btn>

          <q-btn
            flat
            round
            dense
            icon="chevron_right"
            color="grey-5"
            @click="navigateMonth(1)"
          >
            <q-tooltip>Next Month</q-tooltip>
          </q-btn>

          <q-separator vertical inset class="q-mx-sm" />

          <q-btn
            flat
            dense
            no-caps
            icon="today"
            label="Today"
            color="green-5"
            @click="goToToday"
          >
            <q-tooltip>Scroll to Today</q-tooltip>
          </q-btn>

          <!-- Admin: Manage Holidays -->
          <q-btn
            v-if="authStore.isAdmin"
            flat
            dense
            no-caps
            icon="celebration"
            label="Holidays"
            color="purple-4"
            @click="showHolidayDialog = true"
          >
            <q-tooltip>Manage Public Holidays</q-tooltip>
          </q-btn>
        </div>
      </div>
    </div>

    <!-- Calendar Grid -->
    <TeamCalendarGrid
      ref="calendarGridRef"
      :focus-date="focusDate"
      @cell-click="onCellClick"
      @range-changed="onRangeChanged"
    />

    <!-- Day Off Request Dialog (for clicking own empty cell) -->
    <DayOffRequestDialog
      v-model="showRequestDialog"
      :initial-date="selectedDate"
      @created="onRequestCreated"
    />

    <!-- Public Holiday Settings Dialog (admin) -->
    <PublicHolidaySettingsDialog
      v-model="showHolidayDialog"
      @changed="onHolidaysChanged"
    />
  </q-page>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from 'src/stores/authStore'
import TeamCalendarGrid from 'src/components/TeamCalendarGrid.vue'
import DayOffRequestDialog from 'src/components/DayOffRequestDialog.vue'
import PublicHolidaySettingsDialog from 'src/components/PublicHolidaySettingsDialog.vue'

const authStore = useAuthStore()

const calendarGridRef = ref(null)

// Focus date for the calendar
const focusDate = ref(formatLocalDate(new Date()))

// Current visible range (from calendar grid)
const currentRange = ref({ startDate: '', endDate: '' })

// Dialogs
const showRequestDialog = ref(false)
const showHolidayDialog = ref(false)
const selectedDate = ref('')

// ── Month/Year label ──────────────────────────────────────────────

const monthYearLabel = computed(() => {
  // Show the month of the focusDate
  const d = parseDate(focusDate.value)
  return d.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
})

// ── Helpers ────────────────────────────────────────────────────────

function formatLocalDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function parseDate(str) {
  const [y, m, d] = str.split('-').map(Number)
  return new Date(y, m - 1, d)
}

// ── Navigation ─────────────────────────────────────────────────────

function navigateMonth(direction) {
  const current = parseDate(focusDate.value)
  current.setMonth(current.getMonth() + direction)
  current.setDate(1) // go to first day of the new month
  focusDate.value = formatLocalDate(current)
}

function goToToday() {
  focusDate.value = formatLocalDate(new Date())
}

// ── Events ─────────────────────────────────────────────────────────

function onCellClick({ date }) {
  selectedDate.value = date
  showRequestDialog.value = true
}

function onRangeChanged(range) {
  currentRange.value = range
}

function onRequestCreated() {
  // Reload calendar data
  if (calendarGridRef.value) {
    calendarGridRef.value.reload()
  }
}

function onHolidaysChanged() {
  // Reload calendar data after holidays changed
  if (calendarGridRef.value) {
    calendarGridRef.value.reload()
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}
</style>
