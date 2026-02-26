<template>
  <div class="team-calendar-wrapper">
    <!-- Loading overlay -->
    <div v-if="loading" class="text-center q-pa-xl">
      <q-spinner color="green-5" size="40px" />
      <div class="text-caption text-adaptive-caption q-mt-sm">Loading calendar...</div>
    </div>

    <div v-else class="calendar-scroll-container" ref="scrollContainerRef">
      <!-- Sticky top-left corner -->
      <div class="calendar-corner">
        <span class="text-caption text-adaptive-caption">Team</span>
      </div>

      <!-- Header row: date columns -->
      <div class="calendar-header" ref="headerRef">
        <!-- Left sentinel for infinite scroll -->
        <div ref="leftSentinelRef" class="scroll-sentinel scroll-sentinel--left" />

        <div
          v-for="dateStr in dateColumns"
          :key="dateStr"
          class="calendar-header-cell"
          :class="{
            'calendar-header-cell--today': dateStr === todayStr,
            'calendar-header-cell--weekend': isWeekendDate(dateStr),
            'calendar-header-cell--holiday': holidaySet.has(dateStr)
          }"
        >
          <div class="day-name">{{ getDayName(dateStr) }}</div>
          <div class="day-number">{{ getDayNumber(dateStr) }}</div>
          <q-tooltip v-if="holidaySet.has(dateStr)">
            {{ holidayNameMap[dateStr] }}
          </q-tooltip>
        </div>

        <!-- Right sentinel for infinite scroll -->
        <div ref="rightSentinelRef" class="scroll-sentinel scroll-sentinel--right" />
      </div>

      <!-- User rows -->
      <div class="calendar-body">
        <div
          v-for="user in users"
          :key="user.userId"
          class="calendar-row"
        >
          <!-- Sticky user cell -->
          <div class="calendar-user-cell">
            <q-avatar size="26px" class="q-mr-sm">
              <img
                v-if="user.avatarUrl"
                :src="user.avatarUrl"
                :alt="user.firstName"
              />
              <q-icon v-else name="person" color="grey-5" />
            </q-avatar>
            <div class="user-name-text ellipsis">
              {{ user.firstName || user.userName }}
              <span v-if="user.lastName" class="text-adaptive-caption"> {{ user.lastName }}</span>
            </div>
          </div>

          <!-- Day cells for this user -->
          <div class="calendar-row-cells">
            <CalendarDayOffBar
              v-for="dateStr in dateColumns"
              :key="dateStr"
              :entry="getEntry(user.userId, dateStr)"
              :is-holiday="holidaySet.has(dateStr)"
              :holiday-name="holidayNameMap[dateStr] || ''"
              :is-today="dateStr === todayStr"
              :is-weekend="isWeekendDate(dateStr)"
              class="calendar-day-cell"
              @click="onCellClick(user, dateStr)"
            />
          </div>
        </div>

        <!-- Empty state -->
        <div v-if="!users.length" class="text-center q-pa-xl">
          <div class="text-caption text-adaptive-caption">No team members found</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { worktimeCalendarApi } from 'src/api/worktime'
import { useAuthStore } from 'src/stores/authStore'
import CalendarDayOffBar from 'src/components/CalendarDayOffBar.vue'

const props = defineProps({
  /** ISO date string yyyy-MM-dd for the center/focus date */
  focusDate: { type: String, default: '' }
})

const emit = defineEmits(['cell-click', 'range-changed'])

const authStore = useAuthStore()

// State
const loading = ref(false)
const users = ref([])
const dayEntryMap = ref(new Map())  // Map<"userId-dateStr", entry>
const holidaySet = ref(new Set())
const holidayNameMap = ref({})

// Date range state
const rangeStartDate = ref(null)  // LocalDate string
const rangeEndDate = ref(null)

// Template refs
const scrollContainerRef = ref(null)
const headerRef = ref(null)
const leftSentinelRef = ref(null)
const rightSentinelRef = ref(null)

// Observers
let leftObserver = null
let rightObserver = null
let isExtending = false

// Today
const todayStr = computed(() => {
  const d = new Date()
  return formatLocalDate(d)
})

// Computed date columns array
const dateColumns = computed(() => {
  if (!rangeStartDate.value || !rangeEndDate.value) return []
  const cols = []
  const start = parseDate(rangeStartDate.value)
  const end = parseDate(rangeEndDate.value)
  const current = new Date(start)
  while (current <= end) {
    cols.push(formatLocalDate(current))
    current.setDate(current.getDate() + 1)
  }
  return cols
})

// ── Helpers ──────────────────────────────────────────────────────

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

function addDays(dateStr, days) {
  const d = parseDate(dateStr)
  d.setDate(d.getDate() + days)
  return formatLocalDate(d)
}

function getDayName(dateStr) {
  const d = parseDate(dateStr)
  return d.toLocaleDateString('en-US', { weekday: 'short' }).charAt(0)
    + d.toLocaleDateString('en-US', { weekday: 'short' }).charAt(1)
}

function getDayNumber(dateStr) {
  return parseInt(dateStr.split('-')[2], 10)
}

function isWeekendDate(dateStr) {
  const d = parseDate(dateStr)
  const dow = d.getDay()
  return dow === 0 || dow === 6
}

function getEntry(userId, dateStr) {
  return dayEntryMap.value.get(`${userId}-${dateStr}`) || null
}

function onCellClick(user, dateStr) {
  // Only allow clicking on own empty cells
  const currentUserId = authStore.currentUser?.id
  if (user.userId === currentUserId && !getEntry(user.userId, dateStr) && !isWeekendDate(dateStr)) {
    emit('cell-click', { userId: user.userId, date: dateStr })
  }
}

// ── Data Loading ─────────────────────────────────────────────────

async function loadCalendarData(startDate, endDate, merge = false) {
  if (!merge) loading.value = true
  try {
    const res = await worktimeCalendarApi.getData(startDate, endDate)
    const data = res.data.data
    if (!data) return

    if (!merge) {
      // Fresh load: replace everything
      users.value = data.users || []

      dayEntryMap.value = new Map()
      holidaySet.value = new Set()
      holidayNameMap.value = {}
    }

    // Merge days into the map
    const newMap = merge ? new Map(dayEntryMap.value) : new Map()
    if (!merge) {
      // Populate from scratch
      for (const day of (data.days || [])) {
        const key = `${day.userId}-${day.date}`
        newMap.set(key, day)
      }
    } else {
      for (const day of (data.days || [])) {
        const key = `${day.userId}-${day.date}`
        newMap.set(key, day)
      }
    }
    dayEntryMap.value = newMap

    // Merge holidays
    const newHolidaySet = merge ? new Set(holidaySet.value) : new Set()
    const newHolidayNameMap = merge ? { ...holidayNameMap.value } : {}
    for (const h of (data.holidays || [])) {
      newHolidaySet.add(h.holidayDate)
      newHolidayNameMap[h.holidayDate] = h.name
    }
    holidaySet.value = newHolidaySet
    holidayNameMap.value = newHolidayNameMap

  } catch (e) {
    console.error('Failed to load calendar data', e)
  } finally {
    loading.value = false
  }
}

// ── Initial Load ─────────────────────────────────────────────────

async function initCalendar() {
  const focus = props.focusDate || todayStr.value
  const start = addDays(focus, -7)
  const end = addDays(focus, 30)

  rangeStartDate.value = start
  rangeEndDate.value = end

  await loadCalendarData(start, end, false)
  emit('range-changed', { startDate: start, endDate: end })

  await nextTick()
  scrollToToday()
  setupObservers()
}

function scrollToToday() {
  if (!scrollContainerRef.value) return
  const todayIndex = dateColumns.value.indexOf(todayStr.value)
  if (todayIndex < 0) return

  // Scroll so today is roughly centered
  const userCellWidth = 160
  const cellWidth = 50
  const containerWidth = scrollContainerRef.value.clientWidth
  const targetScroll = userCellWidth + (todayIndex * cellWidth) - (containerWidth / 2) + (cellWidth / 2)
  scrollContainerRef.value.scrollLeft = Math.max(0, targetScroll)
}

// ── IntersectionObserver for Infinite Scroll ─────────────────────

function setupObservers() {
  if (!scrollContainerRef.value) return

  const options = {
    root: scrollContainerRef.value,
    rootMargin: '0px 100px 0px 100px',
    threshold: 0.1
  }

  // Left sentinel: extend range backward
  if (leftSentinelRef.value) {
    leftObserver = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting && !isExtending) {
          extendLeft()
        }
      }
    }, options)
    leftObserver.observe(leftSentinelRef.value)
  }

  // Right sentinel: extend range forward
  if (rightSentinelRef.value) {
    rightObserver = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting && !isExtending) {
          extendRight()
        }
      }
    }, options)
    rightObserver.observe(rightSentinelRef.value)
  }
}

function teardownObservers() {
  if (leftObserver) {
    leftObserver.disconnect()
    leftObserver = null
  }
  if (rightObserver) {
    rightObserver.disconnect()
    rightObserver = null
  }
}

async function extendLeft() {
  if (isExtending) return
  isExtending = true

  try {
    const oldStart = rangeStartDate.value
    const newStart = addDays(oldStart, -14)

    // Save current scroll position relative to content
    const container = scrollContainerRef.value
    const oldScrollLeft = container ? container.scrollLeft : 0
    const oldColumnsCount = dateColumns.value.length

    rangeStartDate.value = newStart
    await loadCalendarData(newStart, addDays(oldStart, -1), true)
    emit('range-changed', { startDate: newStart, endDate: rangeEndDate.value })

    await nextTick()

    // Restore scroll position: new columns were prepended, so add their width
    if (container) {
      const addedColumns = dateColumns.value.length - oldColumnsCount
      container.scrollLeft = oldScrollLeft + (addedColumns * 50)
    }
  } finally {
    isExtending = false
  }
}

async function extendRight() {
  if (isExtending) return
  isExtending = true

  try {
    const oldEnd = rangeEndDate.value
    const newEnd = addDays(oldEnd, 14)

    rangeEndDate.value = newEnd
    await loadCalendarData(addDays(oldEnd, 1), newEnd, true)
    emit('range-changed', { startDate: rangeStartDate.value, endDate: newEnd })
  } finally {
    isExtending = false
  }
}

// ── Sync header scroll with body scroll ──────────────────────────

function onScroll() {
  if (!scrollContainerRef.value || !headerRef.value) return
  headerRef.value.style.transform = `translateX(-${scrollContainerRef.value.scrollLeft - 160}px)`
}

// ── Expose reload for parent ─────────────────────────────────────

async function reload() {
  if (rangeStartDate.value && rangeEndDate.value) {
    await loadCalendarData(rangeStartDate.value, rangeEndDate.value, false)
  }
}

defineExpose({ reload, scrollToToday })

// ── Watch focus date changes from parent ─────────────────────────

watch(() => props.focusDate, async (newFocus) => {
  if (!newFocus) return
  // Check if focus date is within current range
  if (newFocus >= rangeStartDate.value && newFocus <= rangeEndDate.value) {
    // Just scroll to it
    await nextTick()
    const idx = dateColumns.value.indexOf(newFocus)
    if (idx >= 0 && scrollContainerRef.value) {
      const userCellWidth = 160
      const cellWidth = 50
      const containerWidth = scrollContainerRef.value.clientWidth
      const targetScroll = userCellWidth + (idx * cellWidth) - (containerWidth / 2) + (cellWidth / 2)
      scrollContainerRef.value.scrollLeft = Math.max(0, targetScroll)
    }
  } else {
    // Reload around the new focus
    teardownObservers()
    const start = addDays(newFocus, -7)
    const end = addDays(newFocus, 30)
    rangeStartDate.value = start
    rangeEndDate.value = end
    await loadCalendarData(start, end, false)
    emit('range-changed', { startDate: start, endDate: end })
    await nextTick()
    scrollToToday()
    setupObservers()
  }
})

// ── Lifecycle ────────────────────────────────────────────────────

onMounted(() => {
  initCalendar()
})

onBeforeUnmount(() => {
  teardownObservers()
})
</script>

<style scoped>
.team-calendar-wrapper {
  border: 1px solid var(--erp-border-subtle);
  border-radius: 8px;
  overflow: hidden;
  background: var(--erp-bg-secondary);
}

.calendar-scroll-container {
  position: relative;
  overflow-x: auto;
  overflow-y: auto;
  max-height: calc(100vh - 220px);
}

/* ── Corner cell (sticky top-left) ─────────────── */
.calendar-corner {
  position: sticky;
  top: 0;
  left: 0;
  z-index: 30;
  width: 160px;
  min-width: 160px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--erp-bg-tertiary);
  border-bottom: 2px solid var(--erp-border-subtle);
  border-right: 2px solid var(--erp-border-subtle);
  box-sizing: border-box;
}

/* ── Header row ────────────────────────────────── */
.calendar-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  margin-left: 160px;
  margin-top: -52px;
  height: 52px;
  background: var(--erp-bg-tertiary);
  border-bottom: 2px solid var(--erp-border-subtle);
}

.calendar-header-cell {
  width: 50px;
  min-width: 50px;
  height: 52px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-right: 1px solid var(--erp-border-subtle);
  box-sizing: border-box;
  cursor: default;
  user-select: none;
}

.calendar-header-cell .day-name {
  font-size: 10px;
  font-weight: 600;
  color: var(--erp-text-caption);
  text-transform: uppercase;
  line-height: 1;
}

.calendar-header-cell .day-number {
  font-size: 14px;
  font-weight: 500;
  color: var(--erp-text);
  line-height: 1.3;
}

.calendar-header-cell--today {
  background: var(--erp-active-bg);
}

.calendar-header-cell--today .day-number {
  color: #4CAF50;
  font-weight: 700;
}

.calendar-header-cell--weekend {
  background: var(--erp-hover-bg);
  opacity: 0.7;
}

.calendar-header-cell--holiday {
  background: repeating-linear-gradient(
    45deg,
    transparent,
    transparent 4px,
    var(--erp-hover-bg) 4px,
    var(--erp-hover-bg) 8px
  );
}

/* ── Scroll sentinels ──────────────────────────── */
.scroll-sentinel {
  width: 1px;
  min-width: 1px;
  height: 100%;
  flex-shrink: 0;
}

/* ── Body ──────────────────────────────────────── */
.calendar-body {
  position: relative;
}

.calendar-row {
  display: flex;
  min-height: 40px;
}

.calendar-row:hover .calendar-user-cell {
  background: var(--erp-bg-elevated);
}

/* ── Sticky user cell ──────────────────────────── */
.calendar-user-cell {
  position: sticky;
  left: 0;
  z-index: 10;
  width: 160px;
  min-width: 160px;
  display: flex;
  align-items: center;
  padding: 0 8px;
  background: var(--erp-bg-secondary);
  border-right: 2px solid var(--erp-border-subtle);
  border-bottom: 1px solid var(--erp-border-subtle);
  box-sizing: border-box;
  overflow: hidden;
}

.user-name-text {
  font-size: 12px;
  font-weight: 500;
  color: var(--erp-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 110px;
}

/* ── Day cells row ─────────────────────────────── */
.calendar-row-cells {
  display: flex;
}

.calendar-day-cell {
  cursor: pointer;
}

.calendar-day-cell:hover {
  opacity: 0.85;
}
</style>
