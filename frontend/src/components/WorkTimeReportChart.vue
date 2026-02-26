<template>
  <div class="report-chart">
    <div
      v-for="(item, idx) in data"
      :key="idx"
      class="chart-row"
    >
      <div class="chart-label text-adaptive-secondary">{{ item.label }}</div>
      <div class="chart-bar-container">
        <div
          class="chart-bar"
          :style="{
            width: barWidth(item.value) + '%',
            background: color || 'var(--erp-accent, #66bb6a)'
          }"
        />
      </div>
      <div class="chart-value text-adaptive">{{ formatValue(item.value) }}</div>
    </div>
    <div v-if="!data || !data.length" class="text-caption text-adaptive-caption text-center q-pa-md">
      No data available
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  color: {
    type: String,
    default: '#66bb6a'
  },
  maxValue: {
    type: Number,
    default: 0
  },
  formatMinutes: {
    type: Boolean,
    default: false
  }
})

function barWidth(value) {
  const max = props.maxValue > 0 ? props.maxValue : Math.max(...props.data.map(d => d.value), 1)
  return Math.min(100, (value / max) * 100)
}

function formatValue(value) {
  if (props.formatMinutes) {
    const hrs = Math.floor(value / 60)
    const mins = value % 60
    if (hrs > 0) return `${hrs}h ${mins}m`
    return `${mins}m`
  }
  return value
}
</script>

<style scoped>
.report-chart {
  width: 100%;
}

.chart-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
}

.chart-label {
  min-width: 80px;
  max-width: 120px;
  font-size: 0.8rem;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chart-bar-container {
  flex: 1;
  height: 20px;
  background: var(--erp-bg-tertiary, rgba(255, 255, 255, 0.05));
  border-radius: 4px;
  overflow: hidden;
}

.chart-bar {
  height: 100%;
  border-radius: 4px;
  min-width: 2px;
  transition: width 0.4s ease;
}

.chart-value {
  min-width: 60px;
  font-size: 0.8rem;
  font-weight: 600;
  text-align: left;
}
</style>
