<template>
  <q-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" persistent>
    <q-card style="min-width: 400px" class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid var(--erp-border-subtle)">
        <q-icon name="download" color="green-5" size="md" />
        <div>
          <div class="text-h6 text-adaptive text-weight-medium">Export Report</div>
          <div class="text-caption text-adaptive-caption">Download team monthly report as CSV</div>
        </div>
      </q-card-section>

      <q-card-section class="q-gutter-md">
        <q-select
          v-model="selectedYear"
          :options="yearOptions"
          label="Year"
          outlined
          dense
          color="green-5"
        />
        <q-select
          v-model="selectedMonth"
          :options="monthOptions"
          label="Month"
          outlined
          dense
          color="green-5"
          emit-value
          map-options
        />
      </q-card-section>

      <q-card-actions align="right" style="border-top: 1px solid var(--erp-border-subtle)" class="q-pa-md">
        <q-btn flat label="Cancel" color="grey-5" v-close-popup />
        <q-btn
          label="Download CSV"
          color="green-7"
          unelevated
          icon="download"
          :loading="downloading"
          @click="handleDownload"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useQuasar } from 'quasar'
import { worktimeReportApi } from 'src/api/worktime'

defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue'])

const $q = useQuasar()
const downloading = ref(false)

const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = now.getMonth() + 1

const selectedYear = ref(currentYear)
const selectedMonth = ref(currentMonth)

const yearOptions = [currentYear - 1, currentYear, currentYear + 1]
const monthOptions = [
  { label: 'January', value: 1 },
  { label: 'February', value: 2 },
  { label: 'March', value: 3 },
  { label: 'April', value: 4 },
  { label: 'May', value: 5 },
  { label: 'June', value: 6 },
  { label: 'July', value: 7 },
  { label: 'August', value: 8 },
  { label: 'September', value: 9 },
  { label: 'October', value: 10 },
  { label: 'November', value: 11 },
  { label: 'December', value: 12 }
]

async function handleDownload() {
  downloading.value = true
  try {
    const response = await worktimeReportApi.exportCSV(selectedYear.value, selectedMonth.value)
    const blob = new Blob([response.data], { type: 'text/csv' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `team-report-${selectedYear.value}-${String(selectedMonth.value).padStart(2, '0')}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    $q.notify({ type: 'positive', message: 'Report downloaded' })
    emit('update:modelValue', false)
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to export report' })
  } finally {
    downloading.value = false
  }
}
</script>
