<template>
  <q-page padding>
    <div class="page-header">
      <div class="row items-center">
        <div class="col">
          <div class="text-h5 text-adaptive text-weight-light">
            <q-icon name="bar_chart" color="amber-5" class="q-mr-sm" />
            Reports
          </div>
          <div class="text-caption text-adaptive-caption q-mt-xs">
            Attendance and time tracking reports
          </div>
        </div>
        <div v-if="authStore.isAdmin">
          <q-btn
            color="green-7"
            icon="download"
            label="Export"
            no-caps
            unelevated
            @click="showExportDialog = true"
          />
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <q-tabs
      v-model="activeTab"
      dense
      class="text-adaptive-secondary q-mb-lg"
      active-color="green-5"
      indicator-color="green-5"
      align="left"
      no-caps
    >
      <q-tab name="daily" icon="today" label="My Daily" />
      <q-tab name="weekly" icon="date_range" label="My Weekly" />
      <q-tab name="monthly" icon="calendar_month" label="My Monthly" />
      <q-tab v-if="authStore.isAdmin" name="team" icon="groups" label="Team" />
    </q-tabs>

    <!-- Tab Panels -->
    <q-tab-panels v-model="activeTab" animated class="bg-transparent">

      <!-- ============ MY DAILY TAB ============ -->
      <q-tab-panel name="daily" class="q-pa-none">
        <div class="row items-center q-mb-md">
          <q-input
            v-model="dailyDate"
            type="date"
            outlined
            dense
            color="green-5"
            style="min-width: 180px"
            @update:model-value="loadDailyReport"
          />
        </div>

        <div v-if="loadingDaily" class="text-center q-pa-xl">
          <q-spinner color="green-5" size="32px" />
        </div>

        <q-card v-else-if="dailyReport" class="premium-card" flat>
          <q-card-section>
            <div class="text-subtitle1 text-adaptive text-weight-bold q-mb-md">
              <q-icon name="summarize" color="green-5" class="q-mr-sm" />
              Daily Summary - {{ formatDisplayDate(dailyDate) }}
            </div>

            <div class="row q-col-gutter-md">
              <div class="col-12 col-sm-6 col-md-4">
                <div class="stat-box">
                  <q-icon name="login" color="green-5" size="24px" />
                  <div class="stat-label text-adaptive-caption">Check In</div>
                  <div class="stat-value text-adaptive">{{ formatTime(dailyReport.checkInTime) }}</div>
                </div>
              </div>
              <div class="col-12 col-sm-6 col-md-4">
                <div class="stat-box">
                  <q-icon name="logout" color="red-4" size="24px" />
                  <div class="stat-label text-adaptive-caption">Check Out</div>
                  <div class="stat-value text-adaptive">{{ formatTime(dailyReport.checkOutTime) }}</div>
                </div>
              </div>
              <div class="col-12 col-sm-6 col-md-4">
                <div class="stat-box">
                  <q-icon name="work_history" color="blue-4" size="24px" />
                  <div class="stat-label text-adaptive-caption">Work Duration</div>
                  <div class="stat-value text-adaptive">{{ formatDuration(dailyReport.totalWorkMinutes) }}</div>
                </div>
              </div>
              <div class="col-12 col-sm-6 col-md-4">
                <div class="stat-box">
                  <q-icon name="coffee" color="amber-5" size="24px" />
                  <div class="stat-label text-adaptive-caption">Break Duration</div>
                  <div class="stat-value text-adaptive">{{ formatDuration(dailyReport.totalBreakMinutes) }}</div>
                </div>
              </div>
              <div class="col-12 col-sm-6 col-md-4">
                <div class="stat-box">
                  <q-icon name="more_time" color="purple-4" size="24px" />
                  <div class="stat-label text-adaptive-caption">Overtime</div>
                  <div class="stat-value text-adaptive">{{ formatDuration(dailyReport.overtimeMinutes) }}</div>
                </div>
              </div>
              <div class="col-12 col-sm-6 col-md-4">
                <div class="stat-box">
                  <q-icon name="location_on" color="teal-4" size="24px" />
                  <div class="stat-label text-adaptive-caption">Location</div>
                  <div class="stat-value text-adaptive">{{ dailyReport.workLocation || '-' }}</div>
                </div>
              </div>
            </div>

            <!-- Flags -->
            <div v-if="dailyReport.lateArrival || dailyReport.earlyDeparture" class="q-mt-md flex q-gutter-sm">
              <q-badge v-if="dailyReport.lateArrival" color="red-7" label="Late Arrival" />
              <q-badge v-if="dailyReport.earlyDeparture" color="orange-8" label="Early Departure" />
            </div>
          </q-card-section>
        </q-card>

        <div v-else class="text-center q-pa-xl">
          <q-icon name="event_busy" color="grey-6" size="48px" />
          <div class="text-subtitle1 text-adaptive q-mt-md">No data for this date</div>
        </div>
      </q-tab-panel>

      <!-- ============ MY WEEKLY TAB ============ -->
      <q-tab-panel name="weekly" class="q-pa-none">
        <div class="row items-center q-mb-md q-gutter-sm">
          <q-input
            v-model="weekStartDate"
            type="date"
            outlined
            dense
            color="green-5"
            label="Week starting"
            style="min-width: 180px"
            @update:model-value="loadWeeklyReport"
          />
          <q-btn flat dense icon="chevron_left" color="green-5" @click="prevWeek" />
          <q-btn flat dense icon="chevron_right" color="green-5" @click="nextWeek" />
        </div>

        <div v-if="loadingWeekly" class="text-center q-pa-xl">
          <q-spinner color="green-5" size="32px" />
        </div>

        <template v-else-if="weeklyReport">
          <!-- Weekly Summary -->
          <q-card class="premium-card q-mb-md" flat>
            <q-card-section>
              <div class="text-subtitle1 text-adaptive text-weight-bold q-mb-md">
                <q-icon name="date_range" color="blue-4" class="q-mr-sm" />
                Week {{ formatDisplayDate(weeklyReport.weekStart) }} - {{ formatDisplayDate(weeklyReport.weekEnd) }}
              </div>

              <div class="row q-col-gutter-md">
                <div class="col-6 col-sm-3">
                  <div class="stat-box">
                    <div class="stat-label text-adaptive-caption">Total Hours</div>
                    <div class="stat-value text-adaptive text-h6">{{ formatDuration(weeklyReport.totalWorkMinutes) }}</div>
                  </div>
                </div>
                <div class="col-6 col-sm-3">
                  <div class="stat-box">
                    <div class="stat-label text-adaptive-caption">Days Worked</div>
                    <div class="stat-value text-adaptive text-h6">{{ weeklyReport.daysWorked }}</div>
                  </div>
                </div>
                <div class="col-6 col-sm-3">
                  <div class="stat-box">
                    <div class="stat-label text-adaptive-caption">Overtime</div>
                    <div class="stat-value text-adaptive text-h6">{{ formatDuration(weeklyReport.totalOvertimeMinutes) }}</div>
                  </div>
                </div>
                <div class="col-6 col-sm-3">
                  <div class="stat-box">
                    <div class="stat-label text-adaptive-caption">Avg/Day</div>
                    <div class="stat-value text-adaptive text-h6">{{ formatDuration(weeklyReport.averageWorkMinutesPerDay) }}</div>
                  </div>
                </div>
              </div>
            </q-card-section>
          </q-card>

          <!-- Daily Hours Chart -->
          <q-card class="premium-card q-mb-md" flat>
            <q-card-section>
              <div class="text-subtitle2 text-adaptive text-weight-bold q-mb-md">Daily Hours</div>
              <WorkTimeReportChart
                :data="weeklyChartData"
                color="#66bb6a"
                :max-value="600"
                format-minutes
              />
            </q-card-section>
          </q-card>

          <!-- Daily Breakdown Table -->
          <q-card class="premium-card" flat>
            <q-card-section class="q-pa-none">
              <q-table
                :rows="weeklyReport.dailyEntries || []"
                :columns="dailyColumns"
                row-key="date"
                flat
                hide-pagination
                :rows-per-page-options="[0]"
                class="report-table"
              >
                <template #body-cell-date="props">
                  <q-td :props="props">
                    <span class="text-adaptive text-weight-medium">{{ formatDisplayDate(props.row.date) }}</span>
                  </q-td>
                </template>
                <template #body-cell-checkInTime="props">
                  <q-td :props="props">
                    <span class="text-adaptive">{{ formatTime(props.row.checkInTime) }}</span>
                  </q-td>
                </template>
                <template #body-cell-checkOutTime="props">
                  <q-td :props="props">
                    <span class="text-adaptive">{{ formatTime(props.row.checkOutTime) }}</span>
                  </q-td>
                </template>
                <template #body-cell-totalWorkMinutes="props">
                  <q-td :props="props">
                    <span class="text-adaptive text-weight-medium">{{ formatDuration(props.row.totalWorkMinutes) }}</span>
                  </q-td>
                </template>
                <template #body-cell-flags="props">
                  <q-td :props="props" class="q-gutter-xs">
                    <q-badge v-if="props.row.lateArrival" color="red-7" label="Late" />
                    <q-badge v-if="props.row.earlyDeparture" color="orange-8" label="Early" />
                  </q-td>
                </template>
              </q-table>
            </q-card-section>
          </q-card>
        </template>

        <div v-else class="text-center q-pa-xl">
          <q-icon name="event_busy" color="grey-6" size="48px" />
          <div class="text-subtitle1 text-adaptive q-mt-md">No data for this week</div>
        </div>
      </q-tab-panel>

      <!-- ============ MY MONTHLY TAB ============ -->
      <q-tab-panel name="monthly" class="q-pa-none">
        <div class="row items-center q-mb-md q-gutter-sm">
          <q-select
            v-model="monthlyYear"
            :options="yearOptions"
            label="Year"
            outlined
            dense
            color="green-5"
            style="min-width: 100px"
            @update:model-value="loadMonthlyReport"
          />
          <q-select
            v-model="monthlyMonth"
            :options="monthOptions"
            label="Month"
            outlined
            dense
            color="green-5"
            emit-value
            map-options
            style="min-width: 140px"
            @update:model-value="loadMonthlyReport"
          />
        </div>

        <div v-if="loadingMonthly" class="text-center q-pa-xl">
          <q-spinner color="green-5" size="32px" />
        </div>

        <template v-else-if="monthlyReport">
          <!-- Monthly Stats -->
          <q-card class="premium-card q-mb-md" flat>
            <q-card-section>
              <div class="text-subtitle1 text-adaptive text-weight-bold q-mb-md">
                <q-icon name="calendar_month" color="purple-4" class="q-mr-sm" />
                Monthly Summary - {{ monthLabel(monthlyReport.month) }} {{ monthlyReport.year }}
              </div>

              <div class="row q-col-gutter-md">
                <div class="col-6 col-sm-4 col-md-3">
                  <div class="stat-box">
                    <q-icon name="work_history" color="green-5" size="20px" />
                    <div class="stat-label text-adaptive-caption">Total Hours</div>
                    <div class="stat-value text-adaptive text-weight-bold">{{ formatDuration(monthlyReport.totalWorkMinutes) }}</div>
                  </div>
                </div>
                <div class="col-6 col-sm-4 col-md-3">
                  <div class="stat-box">
                    <q-icon name="event_available" color="blue-4" size="20px" />
                    <div class="stat-label text-adaptive-caption">Days Worked</div>
                    <div class="stat-value text-adaptive text-weight-bold">{{ monthlyReport.daysWorked }}</div>
                  </div>
                </div>
                <div class="col-6 col-sm-4 col-md-3">
                  <div class="stat-box">
                    <q-icon name="more_time" color="purple-4" size="20px" />
                    <div class="stat-label text-adaptive-caption">Overtime</div>
                    <div class="stat-value text-adaptive text-weight-bold">{{ formatDuration(monthlyReport.totalOvertimeMinutes) }}</div>
                  </div>
                </div>
                <div class="col-6 col-sm-4 col-md-3">
                  <div class="stat-box">
                    <q-icon name="event_busy" color="amber-5" size="20px" />
                    <div class="stat-label text-adaptive-caption">Days Off</div>
                    <div class="stat-value text-adaptive text-weight-bold">{{ monthlyReport.daysOff }}</div>
                  </div>
                </div>
                <div class="col-6 col-sm-4 col-md-3">
                  <div class="stat-box">
                    <q-icon name="schedule" color="red-4" size="20px" />
                    <div class="stat-label text-adaptive-caption">Late Arrivals</div>
                    <div class="stat-value text-adaptive text-weight-bold">{{ monthlyReport.lateArrivals }}</div>
                  </div>
                </div>
                <div class="col-6 col-sm-4 col-md-3">
                  <div class="stat-box">
                    <q-icon name="directions_run" color="orange-5" size="20px" />
                    <div class="stat-label text-adaptive-caption">Early Departures</div>
                    <div class="stat-value text-adaptive text-weight-bold">{{ monthlyReport.earlyDepartures }}</div>
                  </div>
                </div>
                <div class="col-6 col-sm-4 col-md-3">
                  <div class="stat-box">
                    <q-icon name="coffee" color="amber-5" size="20px" />
                    <div class="stat-label text-adaptive-caption">Break Time</div>
                    <div class="stat-value text-adaptive text-weight-bold">{{ formatDuration(monthlyReport.totalBreakMinutes) }}</div>
                  </div>
                </div>
              </div>
            </q-card-section>
          </q-card>

          <!-- Weekly Breakdown Table -->
          <q-card v-if="monthlyReport.weeklyBreakdown && monthlyReport.weeklyBreakdown.length" class="premium-card" flat>
            <q-card-section class="q-pa-none">
              <div class="q-pa-md">
                <div class="text-subtitle2 text-adaptive text-weight-bold">Weekly Breakdown</div>
              </div>
              <q-table
                :rows="monthlyReport.weeklyBreakdown"
                :columns="weeklyColumns"
                row-key="weekStart"
                flat
                hide-pagination
                :rows-per-page-options="[0]"
                class="report-table"
              >
                <template #body-cell-week="props">
                  <q-td :props="props">
                    <span class="text-adaptive text-weight-medium">
                      {{ formatDisplayDate(props.row.weekStart) }} - {{ formatDisplayDate(props.row.weekEnd) }}
                    </span>
                  </q-td>
                </template>
                <template #body-cell-totalWorkMinutes="props">
                  <q-td :props="props">
                    <span class="text-adaptive">{{ formatDuration(props.row.totalWorkMinutes) }}</span>
                  </q-td>
                </template>
                <template #body-cell-totalOvertimeMinutes="props">
                  <q-td :props="props">
                    <span class="text-adaptive">{{ formatDuration(props.row.totalOvertimeMinutes) }}</span>
                  </q-td>
                </template>
                <template #body-cell-averageWorkMinutesPerDay="props">
                  <q-td :props="props">
                    <span class="text-adaptive">{{ formatDuration(props.row.averageWorkMinutesPerDay) }}</span>
                  </q-td>
                </template>
              </q-table>
            </q-card-section>
          </q-card>
        </template>

        <div v-else class="text-center q-pa-xl">
          <q-icon name="event_busy" color="grey-6" size="48px" />
          <div class="text-subtitle1 text-adaptive q-mt-md">No data for this month</div>
        </div>
      </q-tab-panel>

      <!-- ============ TEAM TAB ============ -->
      <q-tab-panel v-if="authStore.isAdmin" name="team" class="q-pa-none">
        <div class="row items-center q-mb-md q-gutter-sm">
          <q-btn-toggle
            v-model="teamView"
            no-caps
            unelevated
            toggle-color="green-7"
            color="grey-8"
            text-color="white"
            :options="[
              { label: 'Daily', value: 'daily' },
              { label: 'Monthly', value: 'monthly' }
            ]"
          />
          <q-space />

          <!-- Daily Controls -->
          <template v-if="teamView === 'daily'">
            <q-input
              v-model="teamDailyDate"
              type="date"
              outlined
              dense
              color="green-5"
              style="min-width: 180px"
              @update:model-value="loadTeamDailyReport"
            />
          </template>

          <!-- Monthly Controls -->
          <template v-else>
            <q-select
              v-model="teamMonthlyYear"
              :options="yearOptions"
              outlined
              dense
              color="green-5"
              style="min-width: 100px"
              @update:model-value="loadTeamMonthlyReport"
            />
            <q-select
              v-model="teamMonthlyMonth"
              :options="monthOptions"
              outlined
              dense
              color="green-5"
              emit-value
              map-options
              style="min-width: 140px"
              @update:model-value="loadTeamMonthlyReport"
            />
          </template>
        </div>

        <!-- Team Daily View -->
        <template v-if="teamView === 'daily'">
          <div v-if="loadingTeamDaily" class="text-center q-pa-xl">
            <q-spinner color="green-5" size="32px" />
          </div>

          <q-card v-else-if="teamDailyReport && teamDailyReport.entries && teamDailyReport.entries.length" class="premium-card" flat>
            <q-card-section class="q-pa-none">
              <q-table
                :rows="teamDailyReport.entries"
                :columns="teamDailyColumns"
                row-key="userId"
                flat
                :rows-per-page-options="[15, 30, 50]"
                class="report-table"
              >
                <template #body-cell-name="props">
                  <q-td :props="props">
                    <span class="text-adaptive text-weight-medium">
                      {{ buildName(props.row) }}
                    </span>
                  </q-td>
                </template>
                <template #body-cell-checkInTime="props">
                  <q-td :props="props">
                    <span class="text-adaptive">{{ formatTime(props.row.checkInTime) }}</span>
                  </q-td>
                </template>
                <template #body-cell-checkOutTime="props">
                  <q-td :props="props">
                    <span class="text-adaptive">{{ formatTime(props.row.checkOutTime) }}</span>
                  </q-td>
                </template>
                <template #body-cell-totalWorkMinutes="props">
                  <q-td :props="props">
                    <span class="text-adaptive text-weight-medium">{{ formatDuration(props.row.totalWorkMinutes) }}</span>
                  </q-td>
                </template>
                <template #body-cell-status="props">
                  <q-td :props="props">
                    <q-badge :color="teamStatusColor(props.row.status)" :label="teamStatusLabel(props.row.status)" />
                  </q-td>
                </template>
                <template #body-cell-flags="props">
                  <q-td :props="props" class="q-gutter-xs">
                    <q-badge v-if="props.row.lateArrival" color="red-7" label="Late" />
                  </q-td>
                </template>
              </q-table>
            </q-card-section>
          </q-card>

          <div v-else class="text-center q-pa-xl">
            <q-icon name="groups" color="grey-6" size="48px" />
            <div class="text-subtitle1 text-adaptive q-mt-md">No team data for this date</div>
          </div>
        </template>

        <!-- Team Monthly View -->
        <template v-else>
          <div v-if="loadingTeamMonthly" class="text-center q-pa-xl">
            <q-spinner color="green-5" size="32px" />
          </div>

          <q-card v-else-if="teamMonthlyReport && teamMonthlyReport.members && teamMonthlyReport.members.length" class="premium-card" flat>
            <q-card-section class="q-pa-none">
              <q-table
                :rows="teamMonthlyReport.members"
                :columns="teamMonthlyColumns"
                row-key="userId"
                flat
                :rows-per-page-options="[15, 30, 50]"
                class="report-table"
              >
                <template #body-cell-name="props">
                  <q-td :props="props">
                    <span class="text-adaptive text-weight-medium">
                      {{ buildName(props.row) }}
                    </span>
                  </q-td>
                </template>
                <template #body-cell-totalWorkMinutes="props">
                  <q-td :props="props">
                    <span class="text-adaptive text-weight-medium">{{ formatDuration(props.row.totalWorkMinutes) }}</span>
                  </q-td>
                </template>
                <template #body-cell-overtimeMinutes="props">
                  <q-td :props="props">
                    <span class="text-adaptive">{{ formatDuration(props.row.overtimeMinutes) }}</span>
                  </q-td>
                </template>
              </q-table>
            </q-card-section>
          </q-card>

          <div v-else class="text-center q-pa-xl">
            <q-icon name="groups" color="grey-6" size="48px" />
            <div class="text-subtitle1 text-adaptive q-mt-md">No team data for this month</div>
          </div>
        </template>
      </q-tab-panel>

    </q-tab-panels>

    <!-- Export Dialog -->
    <WorkTimeExportDialog v-model="showExportDialog" />
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { useWorktimeStore } from 'src/stores/worktimeStore'
import { worktimeReportApi } from 'src/api/worktime'
import WorkTimeReportChart from 'src/components/WorkTimeReportChart.vue'
import WorkTimeExportDialog from 'src/components/WorkTimeExportDialog.vue'

const $q = useQuasar()
const authStore = useAuthStore()
const worktimeStore = useWorktimeStore()

const orgTimezone = computed(() => worktimeStore.settings?.timezoneId || 'Asia/Ho_Chi_Minh')

// ── State ───────────────────────────────────────────────────────────────────

const activeTab = ref('daily')
const showExportDialog = ref(false)

// Daily
const dailyDate = ref(todayStr())
const dailyReport = ref(null)
const loadingDaily = ref(false)

// Weekly
const weekStartDate = ref(getMonday(new Date()))
const weeklyReport = ref(null)
const loadingWeekly = ref(false)

// Monthly
const now = new Date()
const currentYear = now.getFullYear()
const monthlyYear = ref(currentYear)
const monthlyMonth = ref(now.getMonth() + 1)
const monthlyReport = ref(null)
const loadingMonthly = ref(false)

// Team
const teamView = ref('daily')
const teamDailyDate = ref(todayStr())
const teamDailyReport = ref(null)
const loadingTeamDaily = ref(false)
const teamMonthlyYear = ref(currentYear)
const teamMonthlyMonth = ref(now.getMonth() + 1)
const teamMonthlyReport = ref(null)
const loadingTeamMonthly = ref(false)

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

// ── Table Columns ───────────────────────────────────────────────────────────

const dailyColumns = [
  { name: 'date', label: 'Date', field: 'date', align: 'left', sortable: true },
  { name: 'checkInTime', label: 'Check In', field: 'checkInTime', align: 'left' },
  { name: 'checkOutTime', label: 'Check Out', field: 'checkOutTime', align: 'left' },
  { name: 'totalWorkMinutes', label: 'Work', field: 'totalWorkMinutes', align: 'center', sortable: true },
  { name: 'flags', label: '', field: 'flags', align: 'right' }
]

const weeklyColumns = [
  { name: 'week', label: 'Week', field: 'weekStart', align: 'left' },
  { name: 'daysWorked', label: 'Days', field: 'daysWorked', align: 'center', sortable: true },
  { name: 'totalWorkMinutes', label: 'Total Hours', field: 'totalWorkMinutes', align: 'center', sortable: true },
  { name: 'totalOvertimeMinutes', label: 'Overtime', field: 'totalOvertimeMinutes', align: 'center', sortable: true },
  { name: 'averageWorkMinutesPerDay', label: 'Avg/Day', field: 'averageWorkMinutesPerDay', align: 'center' }
]

const teamDailyColumns = [
  { name: 'name', label: 'Name', field: row => buildName(row), align: 'left', sortable: true },
  { name: 'checkInTime', label: 'Check In', field: 'checkInTime', align: 'left' },
  { name: 'checkOutTime', label: 'Check Out', field: 'checkOutTime', align: 'left' },
  { name: 'totalWorkMinutes', label: 'Work', field: 'totalWorkMinutes', align: 'center', sortable: true },
  { name: 'status', label: 'Status', field: 'status', align: 'center' },
  { name: 'workLocation', label: 'Location', field: 'workLocation', align: 'center' },
  { name: 'flags', label: '', field: 'flags', align: 'right' }
]

const teamMonthlyColumns = [
  { name: 'name', label: 'Name', field: row => buildName(row), align: 'left', sortable: true },
  { name: 'daysWorked', label: 'Days', field: 'daysWorked', align: 'center', sortable: true },
  { name: 'totalWorkMinutes', label: 'Total Hours', field: 'totalWorkMinutes', align: 'center', sortable: true },
  { name: 'overtimeMinutes', label: 'Overtime', field: 'overtimeMinutes', align: 'center', sortable: true },
  { name: 'lateArrivals', label: 'Late', field: 'lateArrivals', align: 'center', sortable: true },
  { name: 'earlyDepartures', label: 'Early', field: 'earlyDepartures', align: 'center', sortable: true },
  { name: 'daysOff', label: 'Days Off', field: 'daysOff', align: 'center', sortable: true }
]

// ── Computed ────────────────────────────────────────────────────────────────

const weeklyChartData = computed(() => {
  if (!weeklyReport.value?.dailyEntries) return []
  return weeklyReport.value.dailyEntries.map(entry => ({
    label: formatShortDate(entry.date),
    value: entry.totalWorkMinutes
  }))
})

// ── Data Loading ────────────────────────────────────────────────────────────

async function loadDailyReport() {
  if (!dailyDate.value) return
  loadingDaily.value = true
  try {
    const res = await worktimeReportApi.getDailyReport(dailyDate.value)
    dailyReport.value = res.data.data
  } catch (e) {
    console.error('Failed to load daily report', e)
    dailyReport.value = null
  } finally {
    loadingDaily.value = false
  }
}

async function loadWeeklyReport() {
  if (!weekStartDate.value) return
  loadingWeekly.value = true
  try {
    const res = await worktimeReportApi.getWeeklyReport(weekStartDate.value)
    weeklyReport.value = res.data.data
  } catch (e) {
    console.error('Failed to load weekly report', e)
    weeklyReport.value = null
  } finally {
    loadingWeekly.value = false
  }
}

async function loadMonthlyReport() {
  loadingMonthly.value = true
  try {
    const res = await worktimeReportApi.getMonthlyReport(monthlyYear.value, monthlyMonth.value)
    monthlyReport.value = res.data.data
  } catch (e) {
    console.error('Failed to load monthly report', e)
    monthlyReport.value = null
  } finally {
    loadingMonthly.value = false
  }
}

async function loadTeamDailyReport() {
  if (!teamDailyDate.value) return
  loadingTeamDaily.value = true
  try {
    const res = await worktimeReportApi.getTeamDailyReport(teamDailyDate.value)
    teamDailyReport.value = res.data.data
  } catch (e) {
    console.error('Failed to load team daily report', e)
    teamDailyReport.value = null
  } finally {
    loadingTeamDaily.value = false
  }
}

async function loadTeamMonthlyReport() {
  loadingTeamMonthly.value = true
  try {
    const res = await worktimeReportApi.getTeamMonthlyReport(teamMonthlyYear.value, teamMonthlyMonth.value)
    teamMonthlyReport.value = res.data.data
  } catch (e) {
    console.error('Failed to load team monthly report', e)
    teamMonthlyReport.value = null
  } finally {
    loadingTeamMonthly.value = false
  }
}

// ── Week Navigation ─────────────────────────────────────────────────────────

function prevWeek() {
  const d = new Date(weekStartDate.value)
  d.setDate(d.getDate() - 7)
  weekStartDate.value = toDateStr(d)
  loadWeeklyReport()
}

function nextWeek() {
  const d = new Date(weekStartDate.value)
  d.setDate(d.getDate() + 7)
  weekStartDate.value = toDateStr(d)
  loadWeeklyReport()
}

// ── Helpers ─────────────────────────────────────────────────────────────────

function todayStr() {
  return toDateStr(new Date())
}

function toDateStr(d) {
  return d.toISOString().slice(0, 10)
}

function getMonday(d) {
  const date = new Date(d)
  const day = date.getDay()
  const diff = date.getDate() - day + (day === 0 ? -6 : 1)
  date.setDate(diff)
  return toDateStr(date)
}

function formatTime(timeStr) {
  if (!timeStr) return '--:--'
  try {
    const date = new Date(timeStr)
    if (!isNaN(date.getTime())) {
      return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: orgTimezone.value })
    }
  } catch {
    // fallback
  }
  return timeStr
}

function formatDuration(minutes) {
  if (minutes == null || minutes === 0) return '0m'
  const hrs = Math.floor(minutes / 60)
  const mins = minutes % 60
  if (hrs > 0) return `${hrs}h ${mins}m`
  return `${mins}m`
}

function formatDisplayDate(dateStr) {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr + 'T00:00:00')
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', weekday: 'short' })
  } catch {
    return dateStr
  }
}

function formatShortDate(dateStr) {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr + 'T00:00:00')
    return d.toLocaleDateString('en-US', { weekday: 'short' })
  } catch {
    return dateStr
  }
}

function monthLabel(m) {
  const labels = ['', 'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']
  return labels[m] || ''
}

function buildName(row) {
  const parts = []
  if (row.firstName) parts.push(row.firstName)
  if (row.lastName) parts.push(row.lastName)
  return parts.length > 0 ? parts.join(' ') : row.userName
}

function teamStatusColor(status) {
  switch (status) {
    case 'CHECKED_IN': return 'green-7'
    case 'ON_BREAK': return 'amber-8'
    case 'CHECKED_OUT': return 'grey-7'
    default: return 'grey-6'
  }
}

function teamStatusLabel(status) {
  switch (status) {
    case 'CHECKED_IN': return 'Working'
    case 'ON_BREAK': return 'On Break'
    case 'CHECKED_OUT': return 'Done'
    default: return status || '-'
  }
}

// ── Tab change watchers ─────────────────────────────────────────────────────

watch(activeTab, (tab) => {
  if (tab === 'daily' && !dailyReport.value) loadDailyReport()
  if (tab === 'weekly' && !weeklyReport.value) loadWeeklyReport()
  if (tab === 'monthly' && !monthlyReport.value) loadMonthlyReport()
  if (tab === 'team') {
    if (teamView.value === 'daily' && !teamDailyReport.value) loadTeamDailyReport()
    if (teamView.value === 'monthly' && !teamMonthlyReport.value) loadTeamMonthlyReport()
  }
})

watch(teamView, (view) => {
  if (view === 'daily' && !teamDailyReport.value) loadTeamDailyReport()
  if (view === 'monthly' && !teamMonthlyReport.value) loadTeamMonthlyReport()
})

// ── Init ────────────────────────────────────────────────────────────────────

onMounted(() => {
  worktimeStore.fetchSettings()
  loadDailyReport()
})
</script>

<style scoped>
.stat-box {
  background: var(--erp-bg-tertiary, rgba(255, 255, 255, 0.03));
  border: 1px solid var(--erp-border-subtle);
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}

.stat-label {
  font-size: 0.75rem;
  margin-top: 6px;
}

.stat-value {
  font-size: 1rem;
  margin-top: 4px;
}

.report-table {
  background: transparent;
}

.report-table :deep(th) {
  color: var(--erp-text-secondary) !important;
  font-weight: 600;
}

.report-table :deep(td) {
  border-color: var(--erp-border-subtle) !important;
}

.report-table :deep(.q-table__bottom) {
  color: var(--erp-text-secondary);
}
</style>
