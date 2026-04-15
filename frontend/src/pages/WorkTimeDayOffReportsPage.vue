<template>
  <q-page padding>
    <div class="page-header">
      <div class="text-h5 text-adaptive text-weight-light">
        <q-icon name="insights" color="blue-4" class="q-mr-sm" />
        Day Off Reports
      </div>
      <div class="text-caption text-adaptive-caption q-mt-xs">
        Org-wide utilization, status breakdown, and top users
      </div>
    </div>

    <!-- Filters -->
    <div class="row q-col-gutter-sm items-end q-mb-md">
      <div class="col-6 col-md-3">
        <q-input v-model="filters.from" label="From" type="date" outlined dense color="green-5" />
      </div>
      <div class="col-6 col-md-3">
        <q-input v-model="filters.to" label="To" type="date" outlined dense color="green-5" />
      </div>
      <div class="col-6 col-md-2 row q-gutter-xs">
        <q-btn color="green-7" icon="search" label="Apply" no-caps unelevated dense @click="load" />
        <q-btn flat color="grey-5" icon="download" label="CSV" no-caps dense @click="exportCsv" />
      </div>
      <div class="col-12 col-md-4 text-right text-caption text-adaptive-caption">
        Quick ranges:
        <q-btn flat dense size="sm" no-caps label="This month" @click="setThisMonth" />
        <q-btn flat dense size="sm" no-caps label="This year" @click="setThisYear" />
        <q-btn flat dense size="sm" no-caps label="All time" @click="setAllTime" />
      </div>
    </div>

    <div v-if="loading" class="text-center q-pa-xl">
      <q-spinner color="green-5" size="36px" />
    </div>

    <template v-else>
      <!-- KPI cards -->
      <div class="row q-col-gutter-md q-mb-lg">
        <div class="col-6 col-md-3">
          <q-card flat class="premium-card kpi">
            <div class="kpi-label">Approved</div>
            <div class="kpi-value text-green-6">{{ summary.approvedCount || 0 }}</div>
          </q-card>
        </div>
        <div class="col-6 col-md-3">
          <q-card flat class="premium-card kpi">
            <div class="kpi-label">Pending</div>
            <div class="kpi-value text-amber-7">{{ summary.pendingCount || 0 }}</div>
          </q-card>
        </div>
        <div class="col-6 col-md-3">
          <q-card flat class="premium-card kpi">
            <div class="kpi-label">Denied</div>
            <div class="kpi-value text-red-6">{{ summary.deniedCount || 0 }}</div>
          </q-card>
        </div>
        <div class="col-6 col-md-3">
          <q-card flat class="premium-card kpi">
            <div class="kpi-label">Total Approved Days</div>
            <div class="kpi-value text-blue-5">{{ (summary.totalApprovedDays || 0).toFixed(1) }}</div>
          </q-card>
        </div>
      </div>

      <div class="row q-col-gutter-md">
        <!-- Days by type (bar chart, CSS-based) -->
        <div class="col-12 col-md-6">
          <q-card flat class="premium-card q-pa-md">
            <div class="text-subtitle1 text-adaptive q-mb-md">
              <q-icon name="pie_chart" color="green-5" class="q-mr-sm" /> Approved days by type
            </div>
            <div v-if="!(summary.daysByType || []).length" class="text-caption text-adaptive-caption">
              No data.
            </div>
            <div v-for="row in summary.daysByType || []" :key="row.name" class="q-mb-sm">
              <div class="row items-center q-mb-xs">
                <div class="color-dot q-mr-sm" :style="{ background: row.color || '#4CAF50' }" />
                <span class="text-adaptive">{{ row.name }}</span>
                <q-space />
                <span class="text-adaptive text-weight-medium">{{ row.days.toFixed(1) }} days</span>
              </div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: barPct(row.days) + '%', background: row.color || '#4CAF50' }" />
              </div>
            </div>
          </q-card>
        </div>

        <!-- Top users -->
        <div class="col-12 col-md-6">
          <q-card flat class="premium-card q-pa-md">
            <div class="text-subtitle1 text-adaptive q-mb-md">
              <q-icon name="leaderboard" color="amber-5" class="q-mr-sm" /> Top 10 users (approved days)
            </div>
            <div v-if="!(summary.topUsers || []).length" class="text-caption text-adaptive-caption">
              No data.
            </div>
            <div v-for="(u, i) in summary.topUsers || []" :key="u.userId" class="row items-center q-py-xs">
              <span class="rank q-mr-sm">#{{ i + 1 }}</span>
              <span class="text-adaptive">{{ u.userName }}</span>
              <q-space />
              <span class="text-adaptive text-weight-medium">{{ u.days.toFixed(1) }}</span>
            </div>
          </q-card>
        </div>
      </div>
    </template>
  </q-page>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useQuasar } from 'quasar'
import { worktimeDayOffRequestApi } from 'src/api/worktime'

const $q = useQuasar()
const loading = ref(false)
const summary = ref({})
const filters = ref({ from: '', to: '' })

const maxTypeDays = computed(() => {
  const arr = summary.value.daysByType || []
  return arr.reduce((m, r) => Math.max(m, r.days || 0), 0) || 1
})
function barPct(v) { return Math.max(2, (v / maxTypeDays.value) * 100) }

async function load() {
  loading.value = true
  try {
    const params = {}
    if (filters.value.from) params.from = filters.value.from
    if (filters.value.to) params.to = filters.value.to
    const res = await worktimeDayOffRequestApi.reportSummary(params)
    summary.value = res.data.data || {}
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load report' })
  } finally {
    loading.value = false
  }
}

async function exportCsv() {
  try {
    const params = {}
    if (filters.value.from) params.from = filters.value.from
    if (filters.value.to) params.to = filters.value.to
    const res = await worktimeDayOffRequestApi.exportCsv(params)
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url; a.download = 'day-off-report.csv'
    document.body.appendChild(a); a.click(); a.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to export CSV' })
  }
}

function pad(n) { return String(n).padStart(2, '0') }
function iso(d) { return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` }

function setThisMonth() {
  const now = new Date()
  filters.value.from = iso(new Date(now.getFullYear(), now.getMonth(), 1))
  filters.value.to = iso(new Date(now.getFullYear(), now.getMonth() + 1, 0))
  load()
}
function setThisYear() {
  const y = new Date().getFullYear()
  filters.value.from = `${y}-01-01`
  filters.value.to = `${y}-12-31`
  load()
}
function setAllTime() {
  filters.value.from = ''
  filters.value.to = ''
  load()
}

onMounted(() => {
  setThisYear()
})
</script>

<style scoped>
.kpi { padding: 14px 18px; }
.kpi-label { font-size: 0.72rem; color: var(--erp-text-secondary); text-transform: uppercase; letter-spacing: 0.4px; }
.kpi-value { font-size: 1.8rem; font-weight: 300; margin-top: 4px; }
.color-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.bar-track { height: 8px; background: var(--erp-border-subtle); border-radius: 999px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 999px; transition: width 0.3s; }
.rank { color: var(--erp-text-secondary); font-weight: 600; min-width: 28px; }
</style>
