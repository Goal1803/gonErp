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

            <!-- Break History -->
            <div v-if="dailyReport.breaks && dailyReport.breaks.length" class="q-mt-md">
              <div class="text-caption text-adaptive-caption q-mb-xs">
                <q-icon name="coffee" color="amber-5" size="16px" class="q-mr-xs" />
                Break History
              </div>
              <q-list dense class="break-history-list">
                <q-item v-for="(brk, idx) in dailyReport.breaks" :key="idx" class="q-px-sm">
                  <q-item-section avatar style="min-width: 28px">
                    <q-icon name="pause_circle_outline" color="amber-5" size="20px" />
                  </q-item-section>
                  <q-item-section>
                    <q-item-label class="text-adaptive">
                      {{ formatTime(brk.startTime) }} - {{ brk.endTime ? formatTime(brk.endTime) : 'ongoing' }}
                    </q-item-label>
                  </q-item-section>
                  <q-item-section side>
                    <q-item-label class="text-adaptive-caption">{{ formatDuration(brk.durationMinutes) }}</q-item-label>
                  </q-item-section>
                </q-item>
              </q-list>
            </div>

            <!-- Checkout Note -->
            <div v-if="dailyReport.dailyNotes" class="q-mt-md">
              <div class="text-caption text-adaptive-caption q-mb-xs">
                <q-icon name="sticky_note_2" color="amber-5" size="16px" class="q-mr-xs" />
                Checkout Note
              </div>
              <div class="text-adaptive" style="white-space: pre-wrap; background: var(--erp-bg-tertiary, rgba(255,255,255,0.03)); border: 1px solid var(--erp-border-subtle); border-radius: 8px; padding: 10px 12px;">
                {{ dailyReport.dailyNotes }}
              </div>
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
                class="report-table clickable-table"
                @row-click="(evt, row) => openMemberDialog(row)"
              >
                <template #body-cell-name="props">
                  <q-td :props="props">
                    <span class="text-adaptive text-weight-medium">
                      {{ buildName(props.row) }}
                    </span>
                    <q-icon name="chevron_right" color="grey-6" size="16px" class="q-ml-xs" />
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
                class="report-table clickable-table"
                @row-click="(evt, row) => openMemberDialog(row)"
              >
                <template #body-cell-name="props">
                  <q-td :props="props">
                    <span class="text-adaptive text-weight-medium">
                      {{ buildName(props.row) }}
                    </span>
                    <q-icon name="chevron_right" color="grey-6" size="16px" class="q-ml-xs" />
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

    <!-- Member Detail Dialog -->
    <q-dialog v-model="showMemberDialog" maximized transition-show="slide-up" transition-hide="slide-down">
      <q-card class="bg-dark">
        <q-bar class="bg-grey-9">
          <q-icon name="person" />
          <div class="text-weight-bold">{{ memberDialogTitle }}</div>
          <q-space />
          <q-btn dense flat icon="close" @click="showMemberDialog = false" />
        </q-bar>

        <q-card-section class="q-pt-md">
          <!-- Sub-tabs for member report views -->
          <q-tabs
            v-model="memberTab"
            dense
            class="text-adaptive-secondary q-mb-lg"
            active-color="green-5"
            indicator-color="green-5"
            align="left"
            no-caps
          >
            <q-tab name="daily" icon="today" label="Daily" />
            <q-tab name="weekly" icon="date_range" label="Weekly" />
            <q-tab name="monthly" icon="calendar_month" label="Monthly" />
          </q-tabs>

          <q-tab-panels v-model="memberTab" animated class="bg-transparent">

            <!-- Member Daily -->
            <q-tab-panel name="daily" class="q-pa-none">
              <div class="row items-center q-mb-md">
                <q-input
                  v-model="memberDailyDate"
                  type="date"
                  outlined
                  dense
                  color="green-5"
                  style="min-width: 180px"
                  @update:model-value="loadMemberDailyReport"
                />
              </div>

              <div v-if="loadingMemberDaily" class="text-center q-pa-xl">
                <q-spinner color="green-5" size="32px" />
              </div>

              <q-card v-else-if="memberDailyReport" class="premium-card" flat>
                <q-card-section>
                  <div class="flex items-center q-mb-md">
                    <div class="text-subtitle1 text-adaptive text-weight-bold">
                      <q-icon name="summarize" color="green-5" class="q-mr-sm" />
                      Daily Summary - {{ formatDisplayDate(memberDailyDate) }}
                    </div>
                    <q-space />
                    <q-btn
                      flat
                      dense
                      no-caps
                      color="blue-4"
                      icon="edit"
                      label="Edit"
                      class="q-mr-sm"
                      @click="openEditDialog"
                    />
                    <q-btn
                      flat
                      dense
                      no-caps
                      color="red-5"
                      icon="restart_alt"
                      label="Reset Day"
                      @click="confirmResetDailyEntry"
                    />
                  </div>

                  <div class="row q-col-gutter-md">
                    <div class="col-12 col-sm-6 col-md-4">
                      <div class="stat-box">
                        <q-icon name="login" color="green-5" size="24px" />
                        <div class="stat-label text-adaptive-caption">Check In</div>
                        <div class="stat-value text-adaptive">{{ formatTime(memberDailyReport.checkInTime) }}</div>
                      </div>
                    </div>
                    <div class="col-12 col-sm-6 col-md-4">
                      <div class="stat-box">
                        <q-icon name="logout" color="red-4" size="24px" />
                        <div class="stat-label text-adaptive-caption">Check Out</div>
                        <div class="stat-value text-adaptive">{{ formatTime(memberDailyReport.checkOutTime) }}</div>
                      </div>
                    </div>
                    <div class="col-12 col-sm-6 col-md-4">
                      <div class="stat-box">
                        <q-icon name="work_history" color="blue-4" size="24px" />
                        <div class="stat-label text-adaptive-caption">Work Duration</div>
                        <div class="stat-value text-adaptive">{{ formatDuration(memberDailyReport.totalWorkMinutes) }}</div>
                      </div>
                    </div>
                    <div class="col-12 col-sm-6 col-md-4">
                      <div class="stat-box">
                        <q-icon name="coffee" color="amber-5" size="24px" />
                        <div class="stat-label text-adaptive-caption">Break Duration</div>
                        <div class="stat-value text-adaptive">{{ formatDuration(memberDailyReport.totalBreakMinutes) }}</div>
                      </div>
                    </div>
                    <div class="col-12 col-sm-6 col-md-4">
                      <div class="stat-box">
                        <q-icon name="more_time" color="purple-4" size="24px" />
                        <div class="stat-label text-adaptive-caption">Overtime</div>
                        <div class="stat-value text-adaptive">{{ formatDuration(memberDailyReport.overtimeMinutes) }}</div>
                      </div>
                    </div>
                    <div class="col-12 col-sm-6 col-md-4">
                      <div class="stat-box">
                        <q-icon name="location_on" color="teal-4" size="24px" />
                        <div class="stat-label text-adaptive-caption">Location</div>
                        <div class="stat-value text-adaptive">{{ memberDailyReport.workLocation || '-' }}</div>
                      </div>
                    </div>
                  </div>

                  <div v-if="memberDailyReport.lateArrival || memberDailyReport.earlyDeparture" class="q-mt-md flex q-gutter-sm">
                    <q-badge v-if="memberDailyReport.lateArrival" color="red-7" label="Late Arrival" />
                    <q-badge v-if="memberDailyReport.earlyDeparture" color="orange-8" label="Early Departure" />
                  </div>

                  <!-- Break History -->
                  <div v-if="memberDailyReport.breaks && memberDailyReport.breaks.length" class="q-mt-md">
                    <div class="text-caption text-adaptive-caption q-mb-xs">
                      <q-icon name="coffee" color="amber-5" size="16px" class="q-mr-xs" />
                      Break History
                    </div>
                    <q-list dense class="break-history-list">
                      <q-item v-for="(brk, idx) in memberDailyReport.breaks" :key="idx" class="q-px-sm">
                        <q-item-section avatar style="min-width: 28px">
                          <q-icon name="pause_circle_outline" color="amber-5" size="20px" />
                        </q-item-section>
                        <q-item-section>
                          <q-item-label class="text-adaptive">
                            {{ formatTime(brk.startTime) }} - {{ brk.endTime ? formatTime(brk.endTime) : 'ongoing' }}
                          </q-item-label>
                        </q-item-section>
                        <q-item-section side>
                          <q-item-label class="text-adaptive-caption">{{ formatDuration(brk.durationMinutes) }}</q-item-label>
                        </q-item-section>
                      </q-item>
                    </q-list>
                  </div>

                  <div v-if="memberDailyReport.dailyNotes" class="q-mt-md">
                    <div class="text-caption text-adaptive-caption q-mb-xs">
                      <q-icon name="sticky_note_2" color="amber-5" size="16px" class="q-mr-xs" />
                      Checkout Note
                    </div>
                    <div class="text-adaptive" style="white-space: pre-wrap; background: var(--erp-bg-tertiary, rgba(255,255,255,0.03)); border: 1px solid var(--erp-border-subtle); border-radius: 8px; padding: 10px 12px;">
                      {{ memberDailyReport.dailyNotes }}
                    </div>
                  </div>
                </q-card-section>
              </q-card>

              <div v-else class="text-center q-pa-xl">
                <q-icon name="event_busy" color="grey-6" size="48px" />
                <div class="text-subtitle1 text-adaptive q-mt-md">No data for this date</div>
              </div>
            </q-tab-panel>

            <!-- Member Weekly -->
            <q-tab-panel name="weekly" class="q-pa-none">
              <div class="row items-center q-mb-md q-gutter-sm">
                <q-input
                  v-model="memberWeekStart"
                  type="date"
                  outlined
                  dense
                  color="green-5"
                  label="Week starting"
                  style="min-width: 180px"
                  @update:model-value="loadMemberWeeklyReport"
                />
                <q-btn flat dense icon="chevron_left" color="green-5" @click="memberPrevWeek" />
                <q-btn flat dense icon="chevron_right" color="green-5" @click="memberNextWeek" />
              </div>

              <div v-if="loadingMemberWeekly" class="text-center q-pa-xl">
                <q-spinner color="green-5" size="32px" />
              </div>

              <template v-else-if="memberWeeklyReport">
                <q-card class="premium-card q-mb-md" flat>
                  <q-card-section>
                    <div class="text-subtitle1 text-adaptive text-weight-bold q-mb-md">
                      <q-icon name="date_range" color="blue-4" class="q-mr-sm" />
                      Week {{ formatDisplayDate(memberWeeklyReport.weekStart) }} - {{ formatDisplayDate(memberWeeklyReport.weekEnd) }}
                    </div>

                    <div class="row q-col-gutter-md">
                      <div class="col-6 col-sm-3">
                        <div class="stat-box">
                          <div class="stat-label text-adaptive-caption">Total Hours</div>
                          <div class="stat-value text-adaptive text-h6">{{ formatDuration(memberWeeklyReport.totalWorkMinutes) }}</div>
                        </div>
                      </div>
                      <div class="col-6 col-sm-3">
                        <div class="stat-box">
                          <div class="stat-label text-adaptive-caption">Days Worked</div>
                          <div class="stat-value text-adaptive text-h6">{{ memberWeeklyReport.daysWorked }}</div>
                        </div>
                      </div>
                      <div class="col-6 col-sm-3">
                        <div class="stat-box">
                          <div class="stat-label text-adaptive-caption">Overtime</div>
                          <div class="stat-value text-adaptive text-h6">{{ formatDuration(memberWeeklyReport.totalOvertimeMinutes) }}</div>
                        </div>
                      </div>
                      <div class="col-6 col-sm-3">
                        <div class="stat-box">
                          <div class="stat-label text-adaptive-caption">Avg/Day</div>
                          <div class="stat-value text-adaptive text-h6">{{ formatDuration(memberWeeklyReport.averageWorkMinutesPerDay) }}</div>
                        </div>
                      </div>
                    </div>
                  </q-card-section>
                </q-card>

                <q-card class="premium-card" flat>
                  <q-card-section class="q-pa-none">
                    <q-table
                      :rows="memberWeeklyReport.dailyEntries || []"
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

            <!-- Member Monthly -->
            <q-tab-panel name="monthly" class="q-pa-none">
              <div class="row items-center q-mb-md q-gutter-sm">
                <q-select
                  v-model="memberMonthlyYear"
                  :options="yearOptions"
                  label="Year"
                  outlined
                  dense
                  color="green-5"
                  style="min-width: 100px"
                  @update:model-value="loadMemberMonthlyReport"
                />
                <q-select
                  v-model="memberMonthlyMonth"
                  :options="monthOptions"
                  label="Month"
                  outlined
                  dense
                  color="green-5"
                  emit-value
                  map-options
                  style="min-width: 140px"
                  @update:model-value="loadMemberMonthlyReport"
                />
              </div>

              <div v-if="loadingMemberMonthly" class="text-center q-pa-xl">
                <q-spinner color="green-5" size="32px" />
              </div>

              <template v-else-if="memberMonthlyReport">
                <q-card class="premium-card q-mb-md" flat>
                  <q-card-section>
                    <div class="text-subtitle1 text-adaptive text-weight-bold q-mb-md">
                      <q-icon name="calendar_month" color="purple-4" class="q-mr-sm" />
                      Monthly Summary - {{ monthLabel(memberMonthlyReport.month) }} {{ memberMonthlyReport.year }}
                    </div>

                    <div class="row q-col-gutter-md">
                      <div class="col-6 col-sm-4 col-md-3">
                        <div class="stat-box">
                          <q-icon name="work_history" color="green-5" size="20px" />
                          <div class="stat-label text-adaptive-caption">Total Hours</div>
                          <div class="stat-value text-adaptive text-weight-bold">{{ formatDuration(memberMonthlyReport.totalWorkMinutes) }}</div>
                        </div>
                      </div>
                      <div class="col-6 col-sm-4 col-md-3">
                        <div class="stat-box">
                          <q-icon name="event_available" color="blue-4" size="20px" />
                          <div class="stat-label text-adaptive-caption">Days Worked</div>
                          <div class="stat-value text-adaptive text-weight-bold">{{ memberMonthlyReport.daysWorked }}</div>
                        </div>
                      </div>
                      <div class="col-6 col-sm-4 col-md-3">
                        <div class="stat-box">
                          <q-icon name="more_time" color="purple-4" size="20px" />
                          <div class="stat-label text-adaptive-caption">Overtime</div>
                          <div class="stat-value text-adaptive text-weight-bold">{{ formatDuration(memberMonthlyReport.totalOvertimeMinutes) }}</div>
                        </div>
                      </div>
                      <div class="col-6 col-sm-4 col-md-3">
                        <div class="stat-box">
                          <q-icon name="event_busy" color="amber-5" size="20px" />
                          <div class="stat-label text-adaptive-caption">Days Off</div>
                          <div class="stat-value text-adaptive text-weight-bold">{{ memberMonthlyReport.daysOff }}</div>
                        </div>
                      </div>
                      <div class="col-6 col-sm-4 col-md-3">
                        <div class="stat-box">
                          <q-icon name="schedule" color="red-4" size="20px" />
                          <div class="stat-label text-adaptive-caption">Late Arrivals</div>
                          <div class="stat-value text-adaptive text-weight-bold">{{ memberMonthlyReport.lateArrivals }}</div>
                        </div>
                      </div>
                      <div class="col-6 col-sm-4 col-md-3">
                        <div class="stat-box">
                          <q-icon name="directions_run" color="orange-5" size="20px" />
                          <div class="stat-label text-adaptive-caption">Early Departures</div>
                          <div class="stat-value text-adaptive text-weight-bold">{{ memberMonthlyReport.earlyDepartures }}</div>
                        </div>
                      </div>
                      <div class="col-6 col-sm-4 col-md-3">
                        <div class="stat-box">
                          <q-icon name="coffee" color="amber-5" size="20px" />
                          <div class="stat-label text-adaptive-caption">Break Time</div>
                          <div class="stat-value text-adaptive text-weight-bold">{{ formatDuration(memberMonthlyReport.totalBreakMinutes) }}</div>
                        </div>
                      </div>
                    </div>
                  </q-card-section>
                </q-card>

                <q-card v-if="memberMonthlyReport.weeklyBreakdown && memberMonthlyReport.weeklyBreakdown.length" class="premium-card" flat>
                  <q-card-section class="q-pa-none">
                    <div class="q-pa-md">
                      <div class="text-subtitle2 text-adaptive text-weight-bold">Weekly Breakdown</div>
                    </div>
                    <q-table
                      :rows="memberMonthlyReport.weeklyBreakdown"
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

          </q-tab-panels>
        </q-card-section>
      </q-card>
    </q-dialog>

    <!-- Edit Time Entry Dialog -->
    <q-dialog v-model="showEditDialog" persistent>
      <q-card style="min-width: 500px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-adaptive">
            <q-icon name="edit" color="blue-4" class="q-mr-sm" />
            Edit Time Entry
          </div>
          <div class="text-caption text-adaptive-caption q-mt-xs">
            {{ buildName(selectedMember) }} - {{ formatDisplayDate(memberDailyDate) }}
          </div>
        </q-card-section>

        <q-card-section>
          <!-- Check In / Out Times -->
          <div class="row q-gutter-md q-mb-md">
            <div class="col">
              <q-input
                v-model="editForm.checkInTime"
                label="Check In Time"
                outlined
                dense
                color="green-5"
                type="datetime-local"
                step="60"
              />
            </div>
            <div class="col">
              <q-input
                v-model="editForm.checkOutTime"
                label="Check Out Time"
                outlined
                dense
                color="green-5"
                type="datetime-local"
                step="60"
              />
            </div>
          </div>

          <!-- Daily Notes -->
          <q-input
            v-model="editForm.dailyNotes"
            label="Checkout Note"
            outlined
            dense
            color="green-5"
            type="textarea"
            autogrow
            class="q-mb-md"
          />

          <!-- Break Entries -->
          <div class="text-subtitle2 text-adaptive text-weight-bold q-mb-sm">
            <q-icon name="coffee" color="amber-5" class="q-mr-xs" />
            Breaks
            <q-btn flat dense round icon="add" color="green-5" size="sm" class="q-ml-sm" @click="addEditBreak" />
          </div>

          <div v-for="(brk, idx) in editForm.breaks" :key="idx" class="row items-center q-gutter-sm q-mb-sm">
            <template v-if="!brk.deleted">
              <div class="col">
                <q-input
                  v-model="brk.startTime"
                  label="Start"
                  outlined
                  dense
                  color="amber-5"
                  type="datetime-local"
                  step="60"
                />
              </div>
              <div class="col">
                <q-input
                  v-model="brk.endTime"
                  label="End"
                  outlined
                  dense
                  color="amber-5"
                  type="datetime-local"
                  step="60"
                />
              </div>
              <div>
                <q-btn flat dense round icon="delete" color="red-5" size="sm" @click="brk.deleted = true" />
              </div>
            </template>
          </div>

          <div v-if="!editForm.breaks.filter(b => !b.deleted).length" class="text-caption text-grey-6 q-pa-sm text-center">
            No breaks
          </div>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" no-caps v-close-popup />
          <q-btn
            unelevated
            label="Save"
            color="green-7"
            no-caps
            :loading="savingEdit"
            @click="saveEditEntry"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

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

// Member Detail Dialog
const showMemberDialog = ref(false)
const selectedMember = ref(null)
const memberTab = ref('daily')
const memberDailyDate = ref(todayStr())
const memberDailyReport = ref(null)
const loadingMemberDaily = ref(false)
const memberWeekStart = ref(getMonday(new Date()))
const memberWeeklyReport = ref(null)
const loadingMemberWeekly = ref(false)
const memberMonthlyYear = ref(currentYear)
const memberMonthlyMonth = ref(now.getMonth() + 1)
const memberMonthlyReport = ref(null)
const loadingMemberMonthly = ref(false)

const memberDialogTitle = computed(() => {
  if (!selectedMember.value) return ''
  return `${buildName(selectedMember.value)}'s Reports`
})

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

// ── Member Detail ────────────────────────────────────────────────────────────

function openMemberDialog(row) {
  selectedMember.value = row
  memberTab.value = 'daily'
  memberDailyDate.value = teamView.value === 'daily' ? teamDailyDate.value : todayStr()
  memberWeekStart.value = getMonday(new Date(memberDailyDate.value))
  memberMonthlyYear.value = teamMonthlyYear.value
  memberMonthlyMonth.value = teamMonthlyMonth.value
  memberDailyReport.value = null
  memberWeeklyReport.value = null
  memberMonthlyReport.value = null
  showMemberDialog.value = true
  loadMemberDailyReport()
}

async function loadMemberDailyReport() {
  if (!selectedMember.value || !memberDailyDate.value) return
  loadingMemberDaily.value = true
  try {
    const res = await worktimeReportApi.getMemberDailyReport(selectedMember.value.userId, memberDailyDate.value)
    memberDailyReport.value = res.data.data
  } catch (e) {
    console.error('Failed to load member daily report', e)
    memberDailyReport.value = null
  } finally {
    loadingMemberDaily.value = false
  }
}

async function loadMemberWeeklyReport() {
  if (!selectedMember.value || !memberWeekStart.value) return
  loadingMemberWeekly.value = true
  try {
    const res = await worktimeReportApi.getMemberWeeklyReport(selectedMember.value.userId, memberWeekStart.value)
    memberWeeklyReport.value = res.data.data
  } catch (e) {
    console.error('Failed to load member weekly report', e)
    memberWeeklyReport.value = null
  } finally {
    loadingMemberWeekly.value = false
  }
}

async function loadMemberMonthlyReport() {
  if (!selectedMember.value) return
  loadingMemberMonthly.value = true
  try {
    const res = await worktimeReportApi.getMemberMonthlyReport(selectedMember.value.userId, memberMonthlyYear.value, memberMonthlyMonth.value)
    memberMonthlyReport.value = res.data.data
  } catch (e) {
    console.error('Failed to load member monthly report', e)
    memberMonthlyReport.value = null
  } finally {
    loadingMemberMonthly.value = false
  }
}

// ── Edit Time Entry ──────────────────────────────────────────────────────────

const showEditDialog = ref(false)
const savingEdit = ref(false)
const editForm = ref({ checkInTime: '', checkOutTime: '', dailyNotes: '', breaks: [] })

function toLocalDatetimeStr(isoStr) {
  if (!isoStr) return ''
  try {
    const d = new Date(isoStr)
    const offset = d.getTimezoneOffset()
    const local = new Date(d.getTime() - offset * 60000)
    return local.toISOString().slice(0, 16)
  } catch { return '' }
}

function fromLocalDatetimeStr(localStr) {
  if (!localStr) return null
  try {
    const d = new Date(localStr)
    return d.toISOString()
  } catch { return null }
}

function openEditDialog() {
  const r = memberDailyReport.value
  if (!r) return
  editForm.value = {
    checkInTime: toLocalDatetimeStr(r.checkInTime),
    checkOutTime: toLocalDatetimeStr(r.checkOutTime),
    dailyNotes: r.dailyNotes || '',
    breaks: (r.breaks || []).map(b => ({
      id: b.id,
      startTime: toLocalDatetimeStr(b.startTime),
      endTime: toLocalDatetimeStr(b.endTime),
      deleted: false
    }))
  }
  showEditDialog.value = true
}

function addEditBreak() {
  editForm.value.breaks.push({ id: null, startTime: '', endTime: '', deleted: false })
}

async function saveEditEntry() {
  savingEdit.value = true
  try {
    const payload = {
      checkInTime: fromLocalDatetimeStr(editForm.value.checkInTime),
      checkOutTime: fromLocalDatetimeStr(editForm.value.checkOutTime),
      dailyNotes: editForm.value.dailyNotes,
      breaks: editForm.value.breaks.map(b => ({
        id: b.id,
        startTime: fromLocalDatetimeStr(b.startTime),
        endTime: fromLocalDatetimeStr(b.endTime),
        deleted: b.deleted
      }))
    }
    const res = await worktimeReportApi.editMemberDailyEntry(selectedMember.value.userId, memberDailyDate.value, payload)
    memberDailyReport.value = res.data.data
    showEditDialog.value = false
    $q.notify({ type: 'positive', message: 'Time entry updated successfully' })
  } catch (e) {
    console.error('Failed to save edit', e)
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to save changes' })
  } finally {
    savingEdit.value = false
  }
}

function confirmResetDailyEntry() {
  const memberName = buildName(selectedMember.value)
  const dateLabel = formatDisplayDate(memberDailyDate.value)
  $q.dialog({
    title: 'Reset Daily Entry',
    message: `Are you sure you want to reset all time tracking data for <strong>${memberName}</strong> on <strong>${dateLabel}</strong>?<br><br>This will permanently delete the check-in, check-out, all breaks, and checkout note for this day. This action cannot be undone.<br><br>If the date is today, the user will be able to check in again.`,
    html: true,
    cancel: { flat: true, color: 'grey-5', label: 'Cancel', noCaps: true },
    ok: { unelevated: true, color: 'red-7', label: 'Reset', noCaps: true },
    persistent: true
  }).onOk(async () => {
    try {
      await worktimeReportApi.resetMemberDailyEntry(selectedMember.value.userId, memberDailyDate.value)
      $q.notify({ type: 'positive', message: `Daily entry for ${memberName} on ${dateLabel} has been reset` })
      memberDailyReport.value = null
      loadMemberDailyReport()
    } catch (e) {
      console.error('Failed to reset daily entry', e)
      $q.notify({ type: 'negative', message: 'Failed to reset daily entry' })
    }
  })
}

function memberPrevWeek() {
  const d = new Date(memberWeekStart.value)
  d.setDate(d.getDate() - 7)
  memberWeekStart.value = toDateStr(d)
  loadMemberWeeklyReport()
}

function memberNextWeek() {
  const d = new Date(memberWeekStart.value)
  d.setDate(d.getDate() + 7)
  memberWeekStart.value = toDateStr(d)
  loadMemberWeeklyReport()
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

watch(memberTab, (tab) => {
  if (!selectedMember.value) return
  if (tab === 'daily' && !memberDailyReport.value) loadMemberDailyReport()
  if (tab === 'weekly' && !memberWeeklyReport.value) loadMemberWeeklyReport()
  if (tab === 'monthly' && !memberMonthlyReport.value) loadMemberMonthlyReport()
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

.break-history-list {
  background: var(--erp-bg-tertiary, rgba(255, 255, 255, 0.03));
  border: 1px solid var(--erp-border-subtle);
  border-radius: 8px;
}

.clickable-table :deep(tbody tr) {
  cursor: pointer;
  transition: background 0.15s;
}

.clickable-table :deep(tbody tr:hover) {
  background: rgba(102, 187, 106, 0.08) !important;
}
</style>
