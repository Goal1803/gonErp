<template>
  <q-card class="premium-card quota-card" flat>
    <q-card-section class="q-pa-md">
      <div class="flex items-center no-wrap q-mb-sm">
        <div
          class="color-dot q-mr-sm"
          :style="{ background: quota.dayOffTypeColor || '#4CAF50' }"
        />
        <div class="text-subtitle2 text-adaptive text-weight-bold ellipsis">
          {{ quota.dayOffTypeName }}
        </div>
        <q-space />
        <q-badge
          v-if="quota.carriedOverDays > 0"
          color="blue-grey-7"
          text-color="white"
          class="text-caption"
        >
          +{{ quota.carriedOverDays }} carried
        </q-badge>
      </div>

      <q-linear-progress
        :value="progressValue"
        :color="progressColor"
        track-color="grey-9"
        class="q-mb-sm"
        rounded
        size="8px"
      />

      <div class="row items-center justify-between">
        <div class="col text-center">
          <div class="text-h6 text-adaptive text-weight-bold">{{ quota.usedDays }}</div>
          <div class="text-caption text-adaptive-caption">Used</div>
        </div>
        <q-separator vertical class="q-mx-sm" style="border-color: var(--erp-border-subtle)" />
        <div class="col text-center">
          <div class="text-h6 text-weight-bold" :style="{ color: remainingColor }">{{ remainingDays }}</div>
          <div class="text-caption text-adaptive-caption">Remaining</div>
        </div>
        <q-separator vertical class="q-mx-sm" style="border-color: var(--erp-border-subtle)" />
        <div class="col text-center">
          <div class="text-h6 text-adaptive-secondary text-weight-bold">{{ quota.totalDays }}</div>
          <div class="text-caption text-adaptive-caption">Total</div>
        </div>
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  quota: {
    type: Object,
    required: true
    // Expected: { totalDays, usedDays, carriedOverDays, dayOffTypeName, dayOffTypeColor }
  }
})

const remainingDays = computed(() => {
  return Math.max(0, (props.quota.totalDays || 0) - (props.quota.usedDays || 0))
})

const progressValue = computed(() => {
  if (!props.quota.totalDays) return 0
  return Math.min(1, (props.quota.usedDays || 0) / props.quota.totalDays)
})

const progressColor = computed(() => {
  const pct = progressValue.value
  if (pct >= 0.9) return 'red-7'
  if (pct >= 0.7) return 'amber-7'
  return 'green-7'
})

const remainingColor = computed(() => {
  if (remainingDays.value <= 0) return '#ef5350'
  if (remainingDays.value <= 2) return '#ffa726'
  return '#66bb6a'
})
</script>

<style scoped>
.quota-card {
  min-width: 220px;
}
.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
