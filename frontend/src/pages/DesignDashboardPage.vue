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
      <!-- Created in Period -->
      <div class="text-overline text-grey-5 q-mb-xs">Created in Period</div>
      <div class="row q-gutter-md q-mb-md no-wrap">
        <div class="col">
          <q-card class="stat-card" flat>
            <q-card-section class="text-center q-pa-lg">
              <q-icon name="add_circle" size="32px" color="blue-4" />
              <div class="text-h4 text-white q-mt-sm">{{ dashboard.totalCreated }}<span v-if="dashboard.totalDrafts" class="text-caption text-grey-5 q-ml-xs">({{ dashboard.totalDrafts }} drafts)</span></div>
              <div class="text-caption text-grey-5">Designs Created</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col">
          <q-card class="stat-card" flat>
            <q-card-section class="text-center q-pa-lg">
              <q-icon name="check_circle" size="32px" color="green-5" />
              <div class="text-h4 text-white q-mt-sm">{{ dashboard.totalCompleted }}</div>
              <div class="text-caption text-grey-5">Completed of Created</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col">
          <q-card class="stat-card" flat>
            <q-card-section class="text-center q-pa-lg">
              <q-icon name="cancel" size="32px" color="red-4" />
              <div class="text-h4 text-white q-mt-sm">{{ dashboard.totalCancelled }}</div>
              <div class="text-caption text-grey-5">Cancelled of Created</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col">
          <q-card class="stat-card" flat>
            <q-card-section class="text-center q-pa-lg">
              <q-icon name="trending_up" size="32px" color="cyan-4" />
              <div class="text-h4 text-white q-mt-sm">{{ completionRate }}%</div>
              <div class="text-caption text-grey-5">Completion Rate</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Activity in Period -->
      <div class="text-overline text-grey-5 q-mb-xs">Activity in Period</div>
      <div class="row q-gutter-md q-mb-lg no-wrap">
        <div class="col">
          <q-card class="stat-card" flat>
            <q-card-section class="text-center q-pa-lg">
              <q-icon name="check_circle" size="32px" color="green-5" />
              <div class="text-h4 text-white q-mt-sm">{{ dashboard.activityCompleted }}</div>
              <div class="text-caption text-grey-5">Designs Completed</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col">
          <q-card class="stat-card" flat>
            <q-card-section class="text-center q-pa-lg">
              <q-icon name="cancel" size="32px" color="red-4" />
              <div class="text-h4 text-white q-mt-sm">{{ dashboard.activityCancelled }}</div>
              <div class="text-caption text-grey-5">Designs Cancelled</div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col">
          <q-card class="stat-card" flat>
            <q-card-section class="text-center q-pa-lg">
              <q-icon name="timer" size="32px" color="amber-5" />
              <div class="text-h4 text-white q-mt-sm">{{ activityAvgHours }}h</div>
              <div class="text-caption text-grey-5">Avg Hours to Complete</div>
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

          <!-- Daily Trend Line Chart -->
          <q-card class="dashboard-card q-mb-lg" flat>
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-md">
                <q-icon name="show_chart" color="teal-4" class="q-mr-xs" />
                Daily Trend
              </div>
              <div v-if="dashboard.dailyTrends && dashboard.dailyTrends.length">
                <!-- Legend -->
                <div class="row q-gutter-md q-mb-sm">
                  <div class="row items-center"><div style="width:12px;height:3px;background:#42A5F5;border-radius:2px" class="q-mr-xs"></div><span class="text-caption text-grey-4">Created</span></div>
                  <div class="row items-center"><div style="width:12px;height:3px;background:#66BB6A;border-radius:2px" class="q-mr-xs"></div><span class="text-caption text-grey-4">Completed (of created)</span></div>
                  <div class="row items-center"><div style="width:12px;height:3px;background:#FFA726;border-radius:2px" class="q-mr-xs"></div><span class="text-caption text-grey-4">Completed (activity)</span></div>
                </div>
                <!-- SVG Chart -->
                <div style="position:relative; width:100%; overflow-x:auto;">
                  <svg :width="chartWidth" :height="chartHeight" class="trend-chart">
                    <!-- Grid lines -->
                    <line v-for="i in 5" :key="'grid'+i"
                      :x1="chartPadding.left" :x2="chartWidth - chartPadding.right"
                      :y1="chartPadding.top + (chartInnerH / 4) * (i - 1)" :y2="chartPadding.top + (chartInnerH / 4) * (i - 1)"
                      stroke="rgba(255,255,255,0.06)" stroke-width="1" />
                    <!-- Y axis labels -->
                    <text v-for="i in 5" :key="'ylabel'+i"
                      :x="chartPadding.left - 6"
                      :y="chartPadding.top + (chartInnerH / 4) * (i - 1) + 4"
                      fill="rgba(255,255,255,0.3)" font-size="10" text-anchor="end">
                      {{ Math.round(maxTrendCount * (5 - i) / 4) }}
                    </text>
                    <!-- X axis labels (show every Nth) -->
                    <text v-for="(day, idx) in chartXLabels" :key="'xlabel'+idx"
                      v-show="idx % labelStep === 0 || idx === chartXLabels.length - 1"
                      :x="chartX(idx)" :y="chartHeight - 4"
                      fill="rgba(255,255,255,0.4)" font-size="10" text-anchor="middle">
                      {{ day }}
                    </text>
                    <!-- Created line -->
                    <polyline :points="createdLinePoints" fill="none" stroke="#42A5F5" stroke-width="2" stroke-linejoin="round" />
                    <!-- Completed line -->
                    <polyline :points="completedLinePoints" fill="none" stroke="#66BB6A" stroke-width="2" stroke-linejoin="round" />
                    <!-- Created area -->
                    <polygon :points="createdAreaPoints" fill="rgba(66,165,245,0.1)" />
                    <!-- Completed area -->
                    <polygon :points="completedAreaPoints" fill="rgba(102,187,106,0.1)" />
                    <!-- Activity Completed line -->
                    <polyline :points="activityCompletedLinePoints" fill="none" stroke="#FFA726" stroke-width="2" stroke-linejoin="round" />
                    <!-- Activity Completed area -->
                    <polygon :points="activityCompletedAreaPoints" fill="rgba(255,167,38,0.1)" />
                    <!-- Created dots -->
                    <circle v-for="(day, idx) in dashboard.dailyTrends" :key="'cd'+idx"
                      :cx="chartX(idx)" :cy="chartY(day.created)" r="3" fill="#42A5F5"
                      :class="{ 'chart-dot-active': hoveredDayIdx === idx }" />
                    <!-- Completed dots -->
                    <circle v-for="(day, idx) in dashboard.dailyTrends" :key="'cpd'+idx"
                      :cx="chartX(idx)" :cy="chartY(day.completed)" r="3" fill="#66BB6A"
                      :class="{ 'chart-dot-active': hoveredDayIdx === idx }" />
                    <!-- Activity Completed dots -->
                    <circle v-for="(day, idx) in dashboard.dailyTrends" :key="'acd'+idx"
                      :cx="chartX(idx)" :cy="chartY(day.activityCompleted || 0)" r="3" fill="#FFA726"
                      :class="{ 'chart-dot-active': hoveredDayIdx === idx }" />
                    <!-- Hover vertical line -->
                    <line v-if="hoveredDayIdx !== null"
                      :x1="chartX(hoveredDayIdx)" :x2="chartX(hoveredDayIdx)"
                      :y1="chartPadding.top" :y2="chartPadding.top + chartInnerH"
                      stroke="rgba(255,255,255,0.15)" stroke-width="1" stroke-dasharray="3,3" />
                    <!-- Invisible hit areas for hover -->
                    <rect v-for="(day, idx) in dashboard.dailyTrends" :key="'hit'+idx"
                      :x="chartX(idx) - 15" :y="chartPadding.top"
                      width="30" :height="chartInnerH"
                      fill="transparent" style="cursor:pointer"
                      @mouseenter="onChartHover(idx, $event)"
                      @mouseleave="hoveredDayIdx = null" />
                  </svg>
                  <!-- Tooltip -->
                  <div v-if="hoveredDayIdx !== null && chartTooltip" class="chart-tooltip" :style="chartTooltipStyle">
                    <div class="text-weight-bold q-mb-xs">{{ chartTooltip.date }}</div>
                    <div class="row items-center q-mb-xs">
                      <div style="width:8px;height:8px;border-radius:50%;background:#42A5F5" class="q-mr-xs"></div>
                      Created: <span class="text-weight-bold q-ml-xs">{{ chartTooltip.created }}<span v-if="chartTooltip.drafts" class="text-grey-5"> ({{ chartTooltip.drafts }} drafts)</span></span>
                    </div>
                    <div class="row items-center q-mb-xs">
                      <div style="width:8px;height:8px;border-radius:50%;background:#66BB6A" class="q-mr-xs"></div>
                      Completed (of created): <span class="text-weight-bold q-ml-xs">{{ chartTooltip.completed }}</span>
                    </div>
                    <div class="row items-center">
                      <div style="width:8px;height:8px;border-radius:50%;background:#FFA726" class="q-mr-xs"></div>
                      Completed (activity): <span class="text-weight-bold q-ml-xs">{{ chartTooltip.activityCompleted }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="text-grey-6 text-center q-pa-md">No trend data</div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Right column: Breakdowns -->
        <div class="col-xs-12 col-md">
          <!-- Idea Creators -->
          <q-card class="dashboard-card q-mb-lg" flat v-if="dashboard.ideaCreatorStats && dashboard.ideaCreatorStats.length">
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-md">
                <q-icon name="lightbulb" color="amber-5" class="q-mr-xs" />
                Idea Creators
              </div>
              <div v-for="(p, i) in dashboard.ideaCreatorStats" :key="p.userId || i" class="row items-center q-mb-sm performer-row">
                <span class="text-weight-bold q-mr-sm rank-badge" :class="rankClass(i)">{{ i + 1 }}</span>
                <q-avatar size="28px" class="q-mr-sm">
                  <img v-if="p.avatarUrl" :src="p.avatarUrl" />
                  <q-icon v-else name="person" color="grey-5" />
                </q-avatar>
                <span class="text-white col ellipsis">{{ p.firstName || p.userName }}</span>
                <q-badge color="blue-8" :label="`${p.created} created${p.drafts ? ' (' + p.drafts + ' drafts)' : ''}`" class="q-mx-xs" />
                <q-badge color="green-8" :label="`${p.completed} done`" class="q-mx-xs" />
              </div>
            </q-card-section>
          </q-card>

          <!-- Designers -->
          <q-card class="dashboard-card q-mb-lg" flat v-if="dashboard.designerStats && dashboard.designerStats.length">
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-md">
                <q-icon name="brush" color="purple-4" class="q-mr-xs" />
                Designers
              </div>
              <div v-for="(p, i) in dashboard.designerStats" :key="p.userId || i" class="row items-center q-mb-sm performer-row">
                <span class="text-weight-bold q-mr-sm rank-badge" :class="rankClass(i)">{{ i + 1 }}</span>
                <q-avatar size="28px" class="q-mr-sm">
                  <img v-if="p.avatarUrl" :src="p.avatarUrl" />
                  <q-icon v-else name="person" color="grey-5" />
                </q-avatar>
                <span class="text-white col ellipsis">{{ p.firstName || p.userName }}</span>
                <q-badge color="blue-8" :label="`${p.created} assigned`" class="q-mx-xs" />
                <q-badge color="green-8" :label="`${p.completed} done`" class="q-mx-xs" />
              </div>
            </q-card-section>
          </q-card>

          <!-- All Members -->
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
                <q-badge :label="`${item.count}${item.drafts ? ' (' + item.drafts + ' drafts)' : ''}`" color="purple-8" text-color="white" />
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
                <q-badge :label="`${item.count}${item.drafts ? ' (' + item.drafts + ' drafts)' : ''}`" color="orange-8" text-color="white" />
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
const customStart = ref(new Date().getFullYear() + '-01-01')
const customEnd = ref(new Date().toISOString().slice(0, 10))

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

const avgHours = computed(() => {
  if (!dashboard.value) return '-'
  if (dashboard.value.avgHoursToComplete != null) return Number(dashboard.value.avgHoursToComplete).toFixed(1)
  return '-'
})

const activityAvgHours = computed(() => {
  if (!dashboard.value) return '-'
  if (dashboard.value.activityAvgHoursToComplete != null) return Number(dashboard.value.activityAvgHoursToComplete).toFixed(1)
  return '-'
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
  return Math.max(1, ...dashboard.value.dailyTrends.flatMap(d => [d.created, d.completed, d.activityCompleted || 0]))
})

const stageColors = ['teal-5', 'blue-5', 'cyan-5', 'green-5', 'lime-6', 'amber-5', 'orange-5', 'deep-purple-4', 'pink-4', 'indigo-4']
function stageColor (index) {
  return stageColors[index % stageColors.length]
}

// Line chart helpers
const chartPadding = { top: 10, right: 15, bottom: 30, left: 35 }
const chartHeight = 220

const chartWidth = computed(() => {
  const days = dashboard.value?.dailyTrends?.length || 1
  return Math.max(400, days * 30 + chartPadding.left + chartPadding.right)
})

// Show every Nth label to avoid overlap
const labelStep = computed(() => {
  const days = dashboard.value?.dailyTrends?.length || 1
  if (days <= 10) return 1
  if (days <= 20) return 2
  if (days <= 40) return 5
  if (days <= 90) return 7
  return 14
})

const chartInnerH = computed(() => chartHeight - chartPadding.top - chartPadding.bottom)

function chartX(idx) {
  const days = dashboard.value?.dailyTrends?.length || 1
  const innerW = chartWidth.value - chartPadding.left - chartPadding.right
  if (days <= 1) return chartPadding.left + innerW / 2
  return chartPadding.left + (idx / (days - 1)) * innerW
}

function chartY(value) {
  const max = maxTrendCount.value || 1
  return chartPadding.top + chartInnerH.value * (1 - value / max)
}

const chartXLabels = computed(() => {
  if (!dashboard.value?.dailyTrends) return []
  return dashboard.value.dailyTrends.map(d => {
    const parts = d.date.split('-')
    return `${parts[2]}/${parts[1]}`
  })
})

const createdLinePoints = computed(() => {
  if (!dashboard.value?.dailyTrends) return ''
  return dashboard.value.dailyTrends.map((d, i) => `${chartX(i)},${chartY(d.created)}`).join(' ')
})

const completedLinePoints = computed(() => {
  if (!dashboard.value?.dailyTrends) return ''
  return dashboard.value.dailyTrends.map((d, i) => `${chartX(i)},${chartY(d.completed)}`).join(' ')
})

const createdAreaPoints = computed(() => {
  if (!dashboard.value?.dailyTrends?.length) return ''
  const days = dashboard.value.dailyTrends
  const baseline = chartPadding.top + chartInnerH.value
  const top = days.map((d, i) => `${chartX(i)},${chartY(d.created)}`).join(' ')
  return `${chartX(0)},${baseline} ${top} ${chartX(days.length - 1)},${baseline}`
})

const completedAreaPoints = computed(() => {
  if (!dashboard.value?.dailyTrends?.length) return ''
  const days = dashboard.value.dailyTrends
  const baseline = chartPadding.top + chartInnerH.value
  const top = days.map((d, i) => `${chartX(i)},${chartY(d.completed)}`).join(' ')
  return `${chartX(0)},${baseline} ${top} ${chartX(days.length - 1)},${baseline}`
})

const activityCompletedLinePoints = computed(() => {
  if (!dashboard.value?.dailyTrends) return ''
  return dashboard.value.dailyTrends.map((d, i) => `${chartX(i)},${chartY(d.activityCompleted || 0)}`).join(' ')
})

const activityCompletedAreaPoints = computed(() => {
  if (!dashboard.value?.dailyTrends?.length) return ''
  const days = dashboard.value.dailyTrends
  const baseline = chartPadding.top + chartInnerH.value
  const top = days.map((d, i) => `${chartX(i)},${chartY(d.activityCompleted || 0)}`).join(' ')
  return `${chartX(0)},${baseline} ${top} ${chartX(days.length - 1)},${baseline}`
})

function formatDate (dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })
}

// Chart tooltip
const hoveredDayIdx = ref(null)
const chartTooltipPos = ref({ x: 0, y: 0 })

const chartTooltip = computed(() => {
  if (hoveredDayIdx.value === null || !dashboard.value?.dailyTrends) return null
  const day = dashboard.value.dailyTrends[hoveredDayIdx.value]
  if (!day) return null
  return {
    date: formatDate(day.date),
    created: day.created,
    drafts: day.drafts || 0,
    completed: day.completed,
    activityCompleted: day.activityCompleted || 0
  }
})

const chartTooltipStyle = computed(() => ({
  left: chartTooltipPos.value.x + 'px',
  top: chartTooltipPos.value.y + 'px'
}))

function onChartHover(idx, event) {
  hoveredDayIdx.value = idx
  chartTooltipPos.value = {
    x: event.clientX + 14,
    y: event.clientY - 50
  }
}

function rankClass (index) {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return ''
}

const memberColumns = [
  { name: 'name', label: 'Member', field: row => row.firstName || row.userName, align: 'left', sortable: true },
  { name: 'created', label: 'Created', field: 'created', align: 'center', sortable: true,
    format: (v, row) => row.drafts ? `${v} (${row.drafts} drafts)` : `${v}` },
  { name: 'completed', label: 'Completed', field: 'completed', align: 'center', sortable: true },
  { name: 'avgHours', label: 'Avg Hours', field: 'avgHoursToComplete', align: 'center', sortable: true,
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
  if (!isAdmin.value) return
  try {
    if (isPerBoard.value) {
      // Single board — load members from that board
      const res = await boardApi.getById(boardId.value)
      const board = res.data.data
      if (board?.members?.length) {
        memberOptions.value = board.members.map(m => ({
          label: `${m.user?.firstName || ''} ${m.user?.lastName || ''}`.trim() || m.user?.userName || 'Unknown',
          value: m.user?.id,
          avatarUrl: m.user?.avatarUrl
        }))
      }
    } else {
      // Combined — load all POD_DESIGN boards and merge members
      const res = await boardApi.getAll()
      const boards = (res.data.data || []).filter(b => b.boardType === 'POD_DESIGN')
      const memberMap = new Map()
      for (const b of boards) {
        try {
          const bRes = await boardApi.getById(b.id)
          const board = bRes.data.data
          if (board?.members) {
            for (const m of board.members) {
              if (m.user?.id && !memberMap.has(m.user.id)) {
                memberMap.set(m.user.id, {
                  label: `${m.user.firstName || ''} ${m.user.lastName || ''}`.trim() || m.user.userName || 'Unknown',
                  value: m.user.id,
                  avatarUrl: m.user.avatarUrl
                })
              }
            }
          }
        } catch { /* ignore */ }
      }
      memberOptions.value = Array.from(memberMap.values())
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

.trend-chart {
  display: block;
}
.trend-chart circle {
  transition: r 0.15s;
}
.trend-chart .chart-dot-active {
  r: 5;
}
.chart-tooltip {
  position: fixed;
  background: rgba(30, 30, 40, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 12px;
  color: white;
  pointer-events: none;
  z-index: 9999;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(0,0,0,0.4);
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
