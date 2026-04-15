<template>
  <div class="monthly-wrap">
    <!-- Legend -->
    <div class="legend q-mb-sm">
      <span class="legend-item"><span class="swatch swatch-approved" /> Approved</span>
      <span class="legend-item"><span class="swatch swatch-pending" /> Pending</span>
      <span class="legend-item"><span class="swatch swatch-weekend" /> Weekend</span>
      <span class="legend-item"><span class="swatch swatch-holiday" /> Holiday</span>
      <span class="legend-item"><span class="swatch swatch-today" /> Today</span>
    </div>

    <div v-if="loading" class="text-center q-pa-xl">
      <q-spinner color="green-5" size="36px" />
    </div>

    <div v-else class="monthly-grid" :style="gridStyle">
      <!-- Corner -->
      <div class="cell cell-corner">
        <span class="text-caption text-adaptive-caption">Team</span>
      </div>
      <!-- Day headers -->
      <div
        v-for="d in days"
        :key="'h-' + d.date"
        class="cell cell-header"
        :class="{
          'cell-today': d.date === todayStr,
          'cell-weekend': d.isWeekend,
          'cell-holiday': holidaySet.has(d.date)
        }"
        :style="holidaySet.has(d.date) && holidayColorMap[d.date]
          ? { '--holiday-color': holidayColorMap[d.date] }
          : {}"
      >
        <div v-if="holidaySet.has(d.date)" class="holiday-strip"
             :style="{ background: holidayColorMap[d.date] || '#9C27B0' }" />
        <div class="day-name">{{ d.dayName }}</div>
        <div class="day-number">{{ d.dayNumber }}</div>
        <q-tooltip v-if="holidaySet.has(d.date)">{{ holidayNameMap[d.date] }}</q-tooltip>
      </div>

      <!-- User rows -->
      <template v-for="user in visibleUsers" :key="user.userId">
        <div class="cell cell-user">
          <q-avatar size="24px" class="q-mr-sm">
            <img v-if="user.avatarUrl" :src="user.avatarUrl" :alt="user.firstName" />
            <q-icon v-else name="person" color="grey-5" />
          </q-avatar>
          <div class="user-name ellipsis">
            {{ user.firstName || user.userName }}
            <span v-if="user.lastName" class="text-adaptive-caption">{{ user.lastName }}</span>
          </div>
        </div>
        <div
          v-for="d in days"
          :key="user.userId + '-' + d.date"
          class="cell cell-day"
          :class="{
            'cell-today': d.date === todayStr,
            'cell-weekend': d.isWeekend,
            'cell-holiday': holidaySet.has(d.date),
            'cell-clickable': canClickCell(user, d, getEntry(user.userId, d.date))
          }"
          @click="onCellClick($event, user, d, getEntry(user.userId, d.date))"
        >
          <template v-if="getEntry(user.userId, d.date)">
            <div
              class="bar"
              :class="{
                'bar-approved': getEntry(user.userId, d.date).status === 'APPROVED',
                'bar-pending': getEntry(user.userId, d.date).status === 'PENDING',
                'bar-half-morning': getEntry(user.userId, d.date).halfDayType === 'MORNING',
                'bar-half-afternoon': getEntry(user.userId, d.date).halfDayType === 'AFTERNOON'
              }"
            >
              <span class="type-dot" :style="{ background: getEntry(user.userId, d.date).dayOffTypeColor || '#4CAF50' }" />
              <q-tooltip anchor="top middle" self="bottom middle" :delay="300">
                <div class="text-weight-medium">{{ user.firstName || user.userName }}{{ user.lastName ? ' ' + user.lastName : '' }}</div>
                <div>{{ getEntry(user.userId, d.date).dayOffTypeName }}
                  <span class="text-caption q-ml-xs">({{ getEntry(user.userId, d.date).status }})</span>
                </div>
                <div v-if="getEntry(user.userId, d.date).halfDayType && getEntry(user.userId, d.date).halfDayType !== 'FULL_DAY'"
                     class="text-caption">
                  {{ getEntry(user.userId, d.date).halfDayType === 'MORNING' ? 'Morning half-day' : 'Afternoon half-day' }}
                </div>
                <div v-if="isAdmin" class="text-caption text-amber-4 q-mt-xs">Click for actions</div>
              </q-tooltip>
            </div>
          </template>
          <q-icon v-else-if="holidaySet.has(d.date)" name="celebration" size="14px" color="grey-6" />
        </div>
      </template>

    </div>

    <!-- Empty state (outside the grid so it isn't clipped) -->
    <div v-if="!loading && !users.length" class="text-center q-pa-xl text-adaptive-caption">
      <q-icon name="group_off" size="40px" color="grey-6" class="q-mb-sm" />
      <div>No active users found in your organization.</div>
    </div>
    <div v-else-if="!loading && users.length && !visibleUsers.length" class="text-center q-pa-lg text-adaptive-caption">
      No users match the current filters.
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { worktimeCalendarApi } from 'src/api/worktime'

const props = defineProps({
  /** First-of-month date string yyyy-MM-dd */
  monthStart: { type: String, required: true },
  typeIds: { type: Array, default: () => [] },
  userIds: { type: Array, default: () => [] },
  hidePending: { type: Boolean, default: false },
  isAdmin: { type: Boolean, default: false },
  currentUserId: { type: Number, default: null }
})

const emit = defineEmits(['entry-click', 'empty-cell-click'])

const loading = ref(false)
const users = ref([])
const dayEntryMap = ref(new Map())
const holidaySet = ref(new Set())
const holidayNameMap = ref({})
const holidayColorMap = ref({})

// ── Date helpers ────────────────────────────────────────────────
function pad(n) { return String(n).padStart(2, '0') }
function iso(d) { return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` }
function parseIso(s) { const [y, m, d] = s.split('-').map(Number); return new Date(y, m - 1, d) }

const todayStr = computed(() => iso(new Date()))

// days array for the given month
const days = computed(() => {
  const start = parseIso(props.monthStart)
  start.setDate(1)
  const year = start.getFullYear()
  const month = start.getMonth()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const out = []
  for (let i = 1; i <= daysInMonth; i++) {
    const d = new Date(year, month, i)
    const dow = d.getDay()
    out.push({
      date: iso(d),
      dayNumber: i,
      dayName: d.toLocaleDateString('en-US', { weekday: 'short' }).slice(0, 2),
      isWeekend: dow === 0 || dow === 6
    })
  }
  return out
})

const gridStyle = computed(() => ({
  gridTemplateColumns: `170px repeat(${days.value.length}, minmax(34px, 1fr))`
}))

const visibleUsers = computed(() => {
  if (!props.userIds?.length) return users.value
  const set = new Set(props.userIds)
  return users.value.filter(u => set.has(u.userId))
})

function getEntry(userId, dateStr) {
  const e = dayEntryMap.value.get(`${userId}-${dateStr}`) || null
  if (!e) return null
  if (props.hidePending && e.status === 'PENDING') return null
  if (props.typeIds?.length && !props.typeIds.includes(e.dayOffTypeId)) return null
  return e
}

function canClickCell(user, day, entry) {
  if (entry) return true // admin can act, anyone can see details
  // Empty cell: current user can create own request on their row (weekdays only)
  if (user.userId === props.currentUserId && !day.isWeekend && !holidaySet.value.has(day.date)) return true
  return false
}

function onCellClick(ev, user, day, entry) {
  if (entry) {
    emit('entry-click', { entry, user, date: day.date, target: ev.currentTarget })
    return
  }
  if (user.userId === props.currentUserId && !day.isWeekend && !holidaySet.value.has(day.date)) {
    emit('empty-cell-click', { userId: user.userId, date: day.date })
  }
}

// ── Data loading ────────────────────────────────────────────────
async function load() {
  loading.value = true
  try {
    const start = days.value[0].date
    const end = days.value[days.value.length - 1].date
    const res = await worktimeCalendarApi.getData(start, end)
    const data = res.data.data || {}
    users.value = data.users || []
    const map = new Map()
    for (const d of (data.days || [])) {
      map.set(`${d.userId}-${d.date}`, d)
    }
    dayEntryMap.value = map
    // Expand multi-day holidays into every date they cover.
    const hset = new Set()
    const hnames = {}
    const hcolors = {}
    for (const h of (data.holidays || [])) {
      const start = h.holidayDate
      const end = h.endDate || h.holidayDate
      const [sy, sm, sd] = start.split('-').map(Number)
      const [ey, em, ed] = end.split('-').map(Number)
      const d = new Date(sy, sm - 1, sd)
      const last = new Date(ey, em - 1, ed)
      while (d <= last) {
        const key = iso(d)
        hset.add(key)
        if (!hnames[key]) hnames[key] = h.name
        if (!hcolors[key] && h.color) hcolors[key] = h.color
        d.setDate(d.getDate() + 1)
      }
    }
    holidaySet.value = hset
    holidayNameMap.value = hnames
    holidayColorMap.value = hcolors
  } catch (e) {
    console.error('Failed to load calendar data', e)
  } finally {
    loading.value = false
  }
}

watch(() => props.monthStart, load)
onMounted(load)

defineExpose({ reload: load })
</script>

<style scoped>
.monthly-wrap {
  border: 1px solid var(--erp-border-subtle);
  border-radius: 8px;
  background: var(--erp-bg-secondary);
  padding: 12px;
}

.legend {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 0.75rem;
  color: var(--erp-text-secondary);
}
.legend-item { display: inline-flex; align-items: center; gap: 6px; }
.swatch { width: 14px; height: 14px; border-radius: 3px; display: inline-block; }
.swatch-approved { background: #2196F3; }
.swatch-pending { background: repeating-linear-gradient(-45deg, #ffb74d, #ffb74d 4px, #5d4a2a 4px, #5d4a2a 8px); }
.swatch-weekend { background: rgba(255,255,255,0.05); border: 1px solid var(--erp-border-subtle); }
.swatch-holiday { background: repeating-linear-gradient(45deg, transparent, transparent 3px, rgba(255,255,255,0.12) 3px, rgba(255,255,255,0.12) 6px); border: 1px solid var(--erp-border-subtle); }
.swatch-today { background: transparent; border: 2px solid #26a69a; }

.monthly-grid {
  display: grid;
  overflow-x: auto;
  background: var(--erp-bg-secondary);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 6px;
}

.cell {
  border-right: 1px solid var(--erp-border-subtle);
  border-bottom: 1px solid var(--erp-border-subtle);
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  position: relative;
}
.cell-corner {
  background: var(--erp-bg-tertiary);
  position: sticky;
  left: 0;
  z-index: 2;
  justify-content: flex-start;
  padding-left: 12px;
  font-weight: 600;
}
.cell-header {
  background: var(--erp-bg-tertiary);
  flex-direction: column;
  padding: 4px 0;
  min-height: 44px;
}
.cell-header .day-name {
  font-size: 0.64rem;
  color: var(--erp-text-caption);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
.cell-header .day-number {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--erp-text);
}
.cell-user {
  justify-content: flex-start;
  padding: 4px 10px;
  position: sticky;
  left: 0;
  z-index: 1;
  background: var(--erp-bg-secondary);
  font-size: 0.82rem;
}
.user-name { min-width: 0; }

.cell-day {
  padding: 3px;
}

.cell-weekend {
  background: rgba(255,255,255,0.025);
}
.cell-holiday {
  background: repeating-linear-gradient(45deg, transparent, transparent 4px, rgba(255,255,255,0.06) 4px, rgba(255,255,255,0.06) 8px);
}
.holiday-strip {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  opacity: 0.9;
}
.cell-today {
  box-shadow: inset 0 0 0 2px rgba(38, 166, 154, 0.6);
}
.cell-clickable { cursor: pointer; }
.cell-clickable:hover { background: rgba(255,255,255,0.04); }

.bar {
  width: 100%;
  height: 22px;
  border-radius: 4px;
  position: relative;
  box-shadow: 0 1px 2px rgba(0,0,0,0.25);
  transition: transform 0.1s, box-shadow 0.1s;
  display: flex;
  align-items: center;
  padding-left: 5px;
}
.bar:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0,0,0,0.35);
}

/* APPROVED → solid blue */
.bar-approved {
  background: #1E88E5;
  border: 1px solid #1565C0;
}

/* PENDING → amber diagonal stripes */
.bar-pending {
  background: repeating-linear-gradient(
    -45deg,
    #ffb74d,
    #ffb74d 5px,
    #7a5a20 5px,
    #7a5a20 10px
  );
  border: 1px solid #ef6c00;
}

/* Tiny type-color accent dot on the bar */
.type-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.25);
}
.bar-half-morning { width: 50%; margin-right: auto; }
.bar-half-afternoon { width: 50%; margin-left: auto; }

.empty-row {
  grid-column: 1 / -1;
}
</style>
