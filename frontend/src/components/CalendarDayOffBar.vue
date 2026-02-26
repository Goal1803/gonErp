<template>
  <div
    class="calendar-cell"
    :class="{
      'calendar-cell--today': isToday,
      'calendar-cell--weekend': isWeekend,
      'calendar-cell--holiday': isHoliday
    }"
  >
    <!-- Day-off entry -->
    <div
      v-if="entry && entry.dayOffTypeName"
      class="day-off-bar"
      :class="{
        'day-off-bar--pending': entry.status === 'PENDING',
        'day-off-bar--half-morning': entry.halfDayType === 'MORNING',
        'day-off-bar--half-afternoon': entry.halfDayType === 'AFTERNOON'
      }"
      :style="barStyle"
    >
      <q-tooltip>
        <div>{{ entry.userFirstName || entry.userName }}</div>
        <div>{{ entry.dayOffTypeName }} ({{ entry.status }})</div>
        <div v-if="entry.halfDayType && entry.halfDayType !== 'FULL_DAY'">
          {{ entry.halfDayType === 'MORNING' ? 'Morning' : 'Afternoon' }}
        </div>
      </q-tooltip>
    </div>

    <!-- Holiday indicator -->
    <div v-if="isHoliday && !entry" class="holiday-indicator">
      <q-icon name="celebration" size="14px" />
      <q-tooltip v-if="holidayName">{{ holidayName }}</q-tooltip>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  entry: { type: Object, default: null },
  isHoliday: { type: Boolean, default: false },
  holidayName: { type: String, default: '' },
  isToday: { type: Boolean, default: false },
  isWeekend: { type: Boolean, default: false }
})

const barStyle = computed(() => {
  if (!props.entry) return {}
  const color = props.entry.dayOffTypeColor || 'var(--erp-text-secondary)'
  return {
    '--bar-color': color,
    '--bar-color-alpha': color + '33'
  }
})
</script>

<style scoped>
.calendar-cell {
  width: 50px;
  height: 40px;
  min-width: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid var(--erp-border-subtle);
  border-bottom: 1px solid var(--erp-border-subtle);
  position: relative;
  box-sizing: border-box;
}

.calendar-cell--today {
  background: var(--erp-active-bg);
}

.calendar-cell--weekend {
  background: var(--erp-hover-bg);
  opacity: 0.7;
}

.calendar-cell--holiday {
  background: repeating-linear-gradient(
    45deg,
    transparent,
    transparent 4px,
    var(--erp-hover-bg) 4px,
    var(--erp-hover-bg) 8px
  );
}

.day-off-bar {
  width: calc(100% - 6px);
  height: 24px;
  border-radius: 4px;
  background: var(--bar-color);
  cursor: default;
}

.day-off-bar--pending {
  background: repeating-linear-gradient(
    -45deg,
    var(--bar-color),
    var(--bar-color) 3px,
    var(--bar-color-alpha) 3px,
    var(--bar-color-alpha) 6px
  );
}

.day-off-bar--half-morning {
  width: calc(50% - 3px);
  margin-right: auto;
  margin-left: 3px;
}

.day-off-bar--half-afternoon {
  width: calc(50% - 3px);
  margin-left: auto;
  margin-right: 3px;
}

.holiday-indicator {
  color: var(--erp-text-caption);
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
