<template>
  <q-page padding class="design-dashboard">
    <!-- Top bar -->
    <div class="page-header q-mb-lg">
      <div class="row items-center q-mb-sm">
        <q-btn flat round dense icon="arrow_back" color="grey-4" class="q-mr-sm"
          @click="goBack" />
        <div class="text-h5 text-white text-weight-light">
          <q-icon name="bar_chart" color="teal-5" class="q-mr-sm" />
          {{ pageTitle }}
        </div>
      </div>

      <!-- Date range controls -->
      <div class="row items-center q-gutter-sm q-mt-sm">
        <q-btn-toggle v-model="dateMode" no-caps dense unelevated
          toggle-color="teal-8"
          text-color="grey-4"
          color="dark"
          :options="[
            { label: 'Today', value: 'today' },
            { label: 'This Week', value: 'week' },
            { label: 'This Month', value: 'month' },
            { label: 'Custom', value: 'custom' }
          ]"
          class="date-toggle"
        />
        <template v-if="dateMode === 'custom'">
          <q-input v-model="customStart" dense dark outlined type="date"
            label="From" class="custom-date-input" />
          <q-input v-model="customEnd" dense dark outlined type="date"
            label="To" class="custom-date-input" />
          <q-btn dense unelevated color="teal-7" icon="search" no-caps @click="loadDashboard" label="Apply" />
        </template>

        <q-space />

        <!-- Member filter (admin/owner only) -->
        <q-select
          v-if="isAdmin"
          v-model="selectedUserId"
          :options="memberOptions"
          emit-value map-options
          dense dark outlined
          label="Filter by member"
          style="min-width: 220px"
          clearable
        >
          <template v-slot:prepend>
            <q-icon name="person" color="grey-5" size="sm" />
          </template>
          <template v-slot:option="scope">
            <q-item v-bind="scope.itemProps">
              <q-item-section avatar>
                <q-avatar size="24px">
                  <img v-if="scope.opt.avatarUrl" :src="scope.opt.avatarUrl" />
                  <q-icon v-else name="person" color="grey-5" />
                </q-avatar>
              </q-item-section>
              <q-item-section>{{ scope.opt.label }}</q-item-section>
            </q-item>
          </template>
        </q-select>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center q-pa-xl">
      <q-spinner-dots size="48px" color="teal-5" />
      <div class="text-grey-5 q-mt-md">Loading dashboard...</div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="text-center q-pa-xl">
      <q-icon name="error_outline" size="48px" color="red-4" />
      <div class="text-grey-4 q-mt-md">{{ error }}</div>
      <q-btn flat color="teal-5" label="Retry" class="q-mt-sm" @click="loadDashboard" />
    </div>

    <!-- Dashboard content -->
    <template v-else-if="dashboard">
      <!-- Summary cards row -->
      <div class="row q-gutter-md q-mb-lg">
        <div class="col-xs-12 col-sm-6 col-md-3">
          <q-card class="stat-card" flat>
            <q-card-section class="text-center q-pa-lg">
              <q-icon name="add_circle" size="32px" color="blue-4" />
              <div class="text-h4 text-white q-mt-sm">{{ dashboard.totalCreated }}</div>
              <div class="text-caption text-grey-5">Designs Created</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-xs-12 col-sm-6 col-md-3">
          <q-card class="stat-card" flat>
            <q-card-section class="text-center q-pa-lg">
              <q-icon name="check_circle" size="32px" color="green-5" />
              <div class="text-h4 text-white q-mt-sm">{{ dashboard.totalCompleted }}</div>
              <div class="text-caption text-grey-5">Designs Completed</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-xs-12 col-sm-6 col-md-3">
          <q-card class="stat-card" flat>
            <q-card-section class="text-center q-pa-lg">
              <q-icon name="trending_up" size="32px" color="cyan-4" />
              <div class="text-h4 text-white q-mt-sm">{{ completionRate }}%</div>
              <div class="text-caption text-grey-5">Completion Rate</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-xs-12 col-sm-6 col-md-3">
          <q-card class="stat-card" flat>
            <q-card-section class="text-center q-pa-lg">
              <q-icon name="timer" size="32px" color="amber-5" />
              <div class="text-h4 text-white q-mt-sm">{{ avgDays }}</div>
              <div class="text-caption text-grey-5">Avg Days to Complete</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Rejection card -->
      <div class="row q-mb-lg" v-if="dashboard.rejectionCount != null">
        <div class="col-xs-12 col-sm-6 col-md-3">
          <q-card class="stat-card stat-card--warning" flat>
            <q-card-section class="text-center q-pa-md">
              <q-icon name="cancel" size="24px" color="orange-5" />
              <div class="text-h5 text-white q-mt-xs">{{ dashboard.rejectionCount }}</div>
              <div class="text-caption text-grey-5">Rejections</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Main content: two columns -->
      <div class="row q-gutter-lg">
        <!-- Left column: Charts -->
        <div class="col-xs-12 col-md-6">
          <!-- Designs by Stage -->
          <q-card class="dashboard-card q-mb-lg" flat>
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-md">
                <q-icon name="view_column" color="teal-4" class="q-mr-xs" />
                Designs by Stage
              </div>
              <div v-if="dashboard.designsByStage && stageEntries.length">
                <div v-for="entry in stageEntries" :key="entry.stage" class="q-mb-sm">
                  <div class="row items-center">
                    <span class="text-grey-3 stage-label ellipsis">{{ entry.stage }}</span>
                    <q-linear-progress
                      :value="entry.count / maxStageCount"
                      :color="stageColor(entry.index)"
                      class="col q-mx-sm"
                      rounded
                      size="20px"
                    />
                    <span class="text-white text-weight-bold stage-count">{{ entry.count }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="text-grey-6 text-center q-pa-md">No stage data</div>
            </q-card-section>
          </q-card>

          <!-- Daily Trend -->
          <q-card class="dashboard-card q-mb-lg" flat>
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-md">
                <q-icon name="show_chart" color="teal-4" class="q-mr-xs" />
                Daily Trend
              </div>
              <div v-if="dashboard.dailyTrends && dashboard.dailyTrends.length">
                <div v-for="day in dashboard.dailyTrends" :key="day.date" class="q-mb-sm">
                  <div class="text-caption text-grey-5 q-mb-xs">{{ formatDate(day.date) }}</div>
                  <div class="row items-center q-mb-xs">
                    <span class="trend-label text-blue-4">Created</span>
                    <div class="col q-mx-sm trend-bar-wrap">
                      <div class="trend-bar trend-bar--created" :style="{ width: trendBarWidth(day.created, maxTrendCount) }"></div>
                    </div>
                    <span class="text-white text-weight-bold trend-count">{{ day.created }}</span>
                  </div>
                  <div class="row items-center">
                    <span class="trend-label text-green-5">Completed</span>
                    <div class="col q-mx-sm trend-bar-wrap">
                      <div class="trend-bar trend-bar--completed" :style="{ width: trendBarWidth(day.completed, maxTrendCount) }"></div>
                    </div>
                    <span class="text-white text-weight-bold trend-count">{{ day.completed }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="text-grey-6 text-center q-pa-md">No trend data</div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Right column: Breakdowns -->
        <div class="col-xs-12 col-md">
          <!-- Top Performers -->
          <q-card class="dashboard-card q-mb-lg" flat v-if="sortedPerformers && sortedPerformers.length">
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-md">
                <q-icon name="emoji_events" color="amber-5" class="q-mr-xs" />
                Top Performers
              </div>
              <div v-for="(p, i) in sortedPerformers" :key="p.userId || i" class="row items-center q-mb-sm performer-row">
                <span class="text-weight-bold q-mr-sm rank-badge" :class="rankClass(i)">{{ i + 1 }}</span>
                <q-avatar size="28px" class="q-mr-sm">
                  <img v-if="p.avatarUrl" :src="p.avatarUrl" />
                  <q-icon v-else name="person" color="grey-5" />
                </q-avatar>
                <span class="text-white col ellipsis">{{ p.firstName || p.userName }}</span>
                <span class="text-blue-4 q-mx-sm" title="Created">{{ p.created }}</span>
                <span class="text-green-5 q-mx-sm" title="Completed">{{ p.completed }}</span>
                <span class="text-grey-5 text-caption" title="Avg days">{{ p.avgDaysToComplete != null ? p.avgDaysToComplete.toFixed(1) : '-' }}d</span>
              </div>
            </q-card-section>
          </q-card>

          <!-- By Member -->
          <q-card class="dashboard-card q-mb-lg" flat v-if="dashboard.memberStats && dashboard.memberStats.length">
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-md">
                <q-icon name="group" color="teal-4" class="q-mr-xs" />
                By Member
              </div>
              <q-table
                :rows="dashboard.memberStats"
                :columns="memberColumns"
                row-key="userId"
                flat
                dark
                dense
                hide-bottom
                :rows-per-page-options="[0]"
                class="member-table"
              >
                <template v-slot:body-cell-name="props">
                  <q-td :props="props">
                    <div class="row items-center no-wrap">
                      <q-avatar size="24px" class="q-mr-sm">
                        <img v-if="props.row.avatarUrl" :src="props.row.avatarUrl" />
                        <q-icon v-else name="person" color="grey-5" />
                      </q-avatar>
                      <span class="ellipsis">{{ props.value }}</span>
                    </div>
                  </q-td>
                </template>
              </q-table>
            </q-card-section>
          </q-card>

          <!-- By Product Type -->
          <q-card class="dashboard-card q-mb-lg" flat v-if="dashboard.productTypeStats && dashboard.productTypeStats.length">
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-md">
                <q-icon name="category" color="purple-4" class="q-mr-xs" />
                By Product Type
              </div>
              <div v-for="item in dashboard.productTypeStats" :key="item.name" class="row items-center q-mb-xs">
                <span class="text-grey-3 col ellipsis">{{ item.name }}</span>
                <q-badge :label="item.count" color="purple-8" text-color="white" />
              </div>
            </q-card-section>
          </q-card>

          <!-- By Niche -->
          <q-card class="dashboard-card q-mb-lg" flat v-if="dashboard.nicheStats && dashboard.nicheStats.length">
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-md">
                <q-icon name="sell" color="orange-4" class="q-mr-xs" />
                By Niche
              </div>
              <div v-for="item in dashboard.nicheStats" :key="item.name" class="row items-center q-mb-xs">
                <span class="text-grey-3 col ellipsis">{{ item.name }}</span>
                <q-badge :label="item.count" color="orange-8" text-color="white" />
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </template>
  </q-page>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { designDashboardApi, boardApi } from 'src/api/tasks'
import { useAuthStore } from 'src/stores/authStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const boardId = computed(() => route.params.boardId ? Number(route.params.boardId) : null)
const isPerBoard = computed(() => !!boardId.value)

const pageTitle = computed(() => {
  if (isPerBoard.value && dashboard.value?.boardName) {
    return `${dashboard.value.boardName} Dashboard`
  }
  return isPerBoard.value ? 'Board Dashboard' : 'All Designs Dashboard'
})

// Member filter
const selectedUserId = ref(null)
const memberOptions = ref([])
const isAdmin = computed(() => authStore.isAdmin || authStore.isSuperAdmin)

// Date range
const dateMode = ref('month')
const customStart = ref('')
const customEnd = ref('')

function getDateRange() {
  const today = new Date().toISOString().slice(0, 10)
  let start, end = today
  switch (dateMode.value) {
    case 'today':
      start = today
      break
    case 'week': {
      const d = new Date()
      const day = d.getDay()
      d.setDate(d.getDate() - day + (day === 0 ? -6 : 1))
      start = d.toISOString().slice(0, 10)
      break
    }
    case 'month': {
      const d = new Date()
      d.setDate(1)
      start = d.toISOString().slice(0, 10)
      break
    }
    case 'custom':
      start = customStart.value || today
      end = customEnd.value || today
      break
    default:
      start = today
  }
  return { start, end }
}

// Dashboard data
const dashboard = ref(null)
const loading = ref(false)
const error = ref(null)

async function loadDashboard () {
  loading.value = true
  error.value = null
  try {
    const { start, end } = getDateRange()
    const uid = selectedUserId.value || undefined
    const res = isPerBoard.value
      ? await designDashboardApi.getBoard(boardId.value, start, end, uid)
      : await designDashboardApi.getCombined(start, end, uid)
    dashboard.value = res.data.data
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to load dashboard'
  } finally {
    loading.value = false
  }
}

// Computed helpers
const completionRate = computed(() => {
  if (!dashboard.value) return 0
  if (dashboard.value.completionRate != null) return Math.round(dashboard.value.completionRate)
  if (!dashboard.value.totalCreated) return 0
  return Math.round((dashboard.value.totalCompleted / dashboard.value.totalCreated) * 100)
})

const avgDays = computed(() => {
  if (!dashboard.value) return '-'
  if (dashboard.value.avgDaysToComplete != null) return Number(dashboard.value.avgDaysToComplete).toFixed(1)
  return '-'
})

const sortedPerformers = computed(() => {
  if (!dashboard.value?.memberStats) return []
  return [...dashboard.value.memberStats].sort((a, b) => b.completed - a.completed || b.created - a.created)
})

const stageEntries = computed(() => {
  if (!dashboard.value?.designsByStage) return []
  const entries = Object.entries(dashboard.value.designsByStage)
  return entries.map(([stage, count], index) => ({ stage, count, index }))
})

const maxStageCount = computed(() => {
  if (!stageEntries.value.length) return 1
  return Math.max(1, ...stageEntries.value.map(e => e.count))
})

const maxTrendCount = computed(() => {
  if (!dashboard.value?.dailyTrends?.length) return 1
  return Math.max(1, ...dashboard.value.dailyTrends.flatMap(d => [d.created, d.completed]))
})

const stageColors = ['teal-5', 'blue-5', 'cyan-5', 'green-5', 'lime-6', 'amber-5', 'orange-5', 'deep-purple-4', 'pink-4', 'indigo-4']
function stageColor (index) {
  return stageColors[index % stageColors.length]
}

function trendBarWidth (value, max) {
  if (!max || !value) return '0%'
  return Math.round((value / max) * 100) + '%'
}

function formatDate (dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })
}

function rankClass (index) {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return ''
}

const memberColumns = [
  { name: 'name', label: 'Member', field: row => row.firstName || row.userName, align: 'left', sortable: true },
  { name: 'created', label: 'Created', field: 'created', align: 'center', sortable: true },
  { name: 'completed', label: 'Completed', field: 'completed', align: 'center', sortable: true },
  { name: 'avgDays', label: 'Avg Days', field: 'avgDaysToComplete', align: 'center', sortable: true,
    format: v => v != null ? Number(v).toFixed(1) : '-' }
]

function goBack () {
  if (isPerBoard.value) {
    router.push(`/tasks/${boardId.value}`)
  } else {
    router.push('/designs')
  }
}

// Watchers
watch(dateMode, () => {
  if (dateMode.value !== 'custom') loadDashboard()
})
watch(selectedUserId, () => loadDashboard())
watch(() => route.params.boardId, () => loadDashboard())

async function loadBoardMembers() {
  if (!isPerBoard.value || !isAdmin.value) return
  try {
    const res = await boardApi.getById(boardId.value)
    const board = res.data.data
    if (board?.members?.length) {
      memberOptions.value = board.members.map(m => ({
        label: `${m.user?.firstName || ''} ${m.user?.lastName || ''}`.trim() || m.user?.userName || 'Unknown',
        value: m.user?.id,
        avatarUrl: m.user?.avatarUrl
      }))
    }
  } catch { /* ignore */ }
}

onMounted(() => {
  loadDashboard()
  loadBoardMembers()
})
</script>

<style scoped>
.design-dashboard {
  background: var(--erp-bg);
  min-height: 100vh;
}

.date-toggle {
  border: 1px solid var(--erp-border-subtle);
  border-radius: 8px;
}

.custom-date-input {
  max-width: 160px;
}

.stat-card {
  background: var(--erp-bg-tertiary);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 12px;
  transition: transform 0.15s;
}
.stat-card:hover {
  transform: translateY(-2px);
}
.stat-card--warning {
  border-color: rgba(255, 152, 0, 0.25);
}

.dashboard-card {
  background: var(--erp-bg-tertiary);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 12px;
}

.stage-label {
  min-width: 120px;
  max-width: 160px;
  font-size: 13px;
}
.stage-count {
  min-width: 30px;
  text-align: right;
}

.trend-label {
  min-width: 72px;
  font-size: 12px;
  font-weight: 500;
}
.trend-count {
  min-width: 28px;
  text-align: right;
  font-size: 13px;
}
.trend-bar-wrap {
  height: 14px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 7px;
  overflow: hidden;
}
.trend-bar {
  height: 100%;
  border-radius: 7px;
  transition: width 0.3s ease;
}
.trend-bar--created {
  background: linear-gradient(90deg, #42a5f5, #1e88e5);
}
.trend-bar--completed {
  background: linear-gradient(90deg, #66bb6a, #43a047);
}

.performer-row {
  padding: 4px 0;
}
.rank-badge {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.08);
  color: #aaa;
}
.rank-gold {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
}
.rank-silver {
  background: rgba(189, 189, 189, 0.2);
  color: #bdbdbd;
}
.rank-bronze {
  background: rgba(255, 152, 0, 0.2);
  color: #ff9800;
}

.member-table :deep(.q-table) {
  background: transparent;
}
.member-table :deep(th) {
  color: #9e9e9e;
  font-weight: 500;
}
</style>
