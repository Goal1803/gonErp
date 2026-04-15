<template>
  <q-page padding>
    <div class="page-header">
      <div class="row items-center justify-between q-gutter-sm q-mb-md">
        <div>
          <div class="text-h5 text-adaptive text-weight-light">
            <q-icon name="calendar_month" color="purple-4" class="q-mr-sm" />
            Team Calendar
          </div>
          <div class="text-caption text-adaptive-caption q-mt-xs">
            Days on the X-axis, team on the Y-axis. Click a bar to see details
            <span v-if="authStore.isAdmin">or approve / deny.</span>
          </div>
        </div>

        <div class="row items-center q-gutter-xs">
          <q-btn flat round dense icon="chevron_left" color="grey-5" @click="navigateMonth(-1)">
            <q-tooltip>Previous month</q-tooltip>
          </q-btn>
          <q-btn flat dense no-caps class="text-adaptive text-weight-medium"
                 style="min-width: 140px" @click="goToToday">
            {{ monthLabel }}
          </q-btn>
          <q-btn flat round dense icon="chevron_right" color="grey-5" @click="navigateMonth(1)">
            <q-tooltip>Next month</q-tooltip>
          </q-btn>
          <q-separator vertical inset class="q-mx-sm" />
          <q-btn flat dense no-caps icon="today" label="Today" color="green-5" @click="goToToday" />
          <q-btn v-if="authStore.isAdmin" flat dense no-caps icon="celebration" label="Holidays"
                 color="purple-4" @click="showHolidayDialog = true" />
        </div>
      </div>

      <!-- Filters -->
      <div class="row q-col-gutter-sm items-center q-mb-md">
        <div class="col-12 col-md-4">
          <q-select
            v-model="filterTypeIds"
            :options="typeOptions"
            label="Filter by type"
            outlined dense color="green-5"
            emit-value map-options multiple use-chips clearable
          >
            <template #option="{ opt, itemProps }">
              <q-item v-bind="itemProps">
                <q-item-section avatar>
                  <div class="color-dot" :style="{ background: opt.color || '#4CAF50' }" />
                </q-item-section>
                <q-item-section>{{ opt.label }}</q-item-section>
              </q-item>
            </template>
          </q-select>
        </div>
        <div class="col-auto">
          <q-toggle v-model="hidePending" color="amber-7" label="Hide pending" />
        </div>
      </div>
    </div>

    <!-- Monthly grid -->
    <MonthlyCalendarGrid
      ref="gridRef"
      :month-start="monthStart"
      :type-ids="filterTypeIds || []"
      :hide-pending="hidePending"
      :is-admin="authStore.isAdmin"
      :current-user-id="authStore.currentUser?.id"
      @entry-click="onEntryClick"
      @empty-cell-click="onEmptyCellClick"
    />

    <!-- Entry action popover (admin + viewer) -->
    <q-dialog v-model="entryDialog" :position="dialogPosition">
      <q-card class="entry-card">
        <q-card-section class="q-pb-sm">
          <div class="row items-center q-gutter-sm">
            <q-badge :color="statusColor(selectedEntry?.status)"
                     :label="selectedEntry?.status" class="text-weight-medium" />
            <div class="color-dot" :style="{ background: selectedEntry?.dayOffTypeColor || '#4CAF50' }" />
            <span class="text-adaptive text-weight-medium">{{ selectedEntry?.dayOffTypeName }}</span>
          </div>
          <div class="text-caption text-adaptive-secondary q-mt-xs">
            {{ selectedEntry?.userFirstName || selectedEntry?.userName }}
            <span v-if="selectedEntry?.userLastName">{{ selectedEntry.userLastName }}</span>
            · {{ selectedEntry?.date }}
            <span v-if="selectedEntry?.halfDayType && selectedEntry?.halfDayType !== 'FULL_DAY'">
              · {{ selectedEntry.halfDayType === 'MORNING' ? 'Morning half-day' : 'Afternoon half-day' }}
            </span>
          </div>
        </q-card-section>

        <q-separator />

        <q-card-section v-if="authStore.isAdmin && selectedEntry?.status === 'PENDING'" class="q-pt-sm">
          <q-input v-model="actionComment" label="Comment (optional)" outlined dense color="green-5"
                   type="textarea" rows="2" autogrow />
        </q-card-section>

        <q-card-section v-if="authStore.isAdmin && selectedEntry?.status === 'APPROVED'" class="q-pt-sm">
          <q-input v-model="actionComment" label="Reason for revoking (optional)" outlined dense color="green-5"
                   type="textarea" rows="2" autogrow />
        </q-card-section>

        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat label="Close" color="grey-5" v-close-popup />
          <template v-if="authStore.isAdmin && selectedEntry?.status === 'PENDING'">
            <q-btn color="red-7" icon="block" label="Deny" unelevated :loading="acting" @click="doDeny" />
            <q-btn color="green-7" icon="check" label="Approve" unelevated :loading="acting" @click="doApprove" />
          </template>
          <template v-else-if="authStore.isAdmin && selectedEntry?.status === 'APPROVED'">
            <q-btn color="orange-6" icon="undo" label="Revoke approval" unelevated :loading="acting" @click="doRevoke" />
          </template>
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Day-off Request Dialog (clicking own empty cell) -->
    <DayOffRequestDialog
      v-model="showRequestDialog"
      :initial-date="selectedDate"
      @created="onRequestCreated"
    />

    <!-- Public Holiday Settings Dialog (admin) -->
    <PublicHolidaySettingsDialog
      v-model="showHolidayDialog"
      @changed="onHolidaysChanged"
    />
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { worktimeDayOffTypeApi, worktimeDayOffRequestApi } from 'src/api/worktime'
import MonthlyCalendarGrid from 'src/components/MonthlyCalendarGrid.vue'
import DayOffRequestDialog from 'src/components/DayOffRequestDialog.vue'
import PublicHolidaySettingsDialog from 'src/components/PublicHolidaySettingsDialog.vue'

const authStore = useAuthStore()
const $q = useQuasar()

const gridRef = ref(null)

// ── Month navigation ───────────────────────────────────────
function pad(n) { return String(n).padStart(2, '0') }
function iso(d) { return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` }
function firstOfMonth(d) { return new Date(d.getFullYear(), d.getMonth(), 1) }

const monthStart = ref(iso(firstOfMonth(new Date())))

const monthLabel = computed(() => {
  const [y, m] = monthStart.value.split('-').map(Number)
  return new Date(y, m - 1, 1).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
})

function navigateMonth(dir) {
  const [y, m] = monthStart.value.split('-').map(Number)
  const d = new Date(y, m - 1 + dir, 1)
  monthStart.value = iso(d)
}
function goToToday() {
  monthStart.value = iso(firstOfMonth(new Date()))
}

// ── Filters ─────────────────────────────────────────────────
const filterTypeIds = ref([])
const hidePending = ref(false)
const typeOptions = ref([])

// ── Entry dialog state ──────────────────────────────────────
const entryDialog = ref(false)
const dialogPosition = ref('standard')
const selectedEntry = ref(null)
const actionComment = ref('')
const acting = ref(false)

// ── Self-request dialog ─────────────────────────────────────
const showRequestDialog = ref(false)
const selectedDate = ref('')

// ── Holiday dialog ─────────────────────────────────────────
const showHolidayDialog = ref(false)

function statusColor(status) {
  const map = { PENDING: 'amber-8', APPROVED: 'green-7', DENIED: 'red-7', CANCELLED: 'grey-7' }
  return map[status] || 'grey-7'
}

function onEntryClick({ entry }) {
  selectedEntry.value = entry
  actionComment.value = ''
  entryDialog.value = true
}

function onEmptyCellClick({ date }) {
  selectedDate.value = date
  showRequestDialog.value = true
}

async function doApprove() {
  if (!selectedEntry.value?.requestId) return
  acting.value = true
  try {
    await worktimeDayOffRequestApi.approve(selectedEntry.value.requestId, actionComment.value || null)
    $q.notify({ type: 'positive', message: 'Request approved' })
    entryDialog.value = false
    await gridRef.value?.reload()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Approve failed' })
  } finally {
    acting.value = false
  }
}

async function doDeny() {
  if (!selectedEntry.value?.requestId) return
  if (!actionComment.value?.trim()) {
    $q.notify({ type: 'warning', message: 'Please provide a reason' })
    return
  }
  acting.value = true
  try {
    await worktimeDayOffRequestApi.deny(selectedEntry.value.requestId, actionComment.value)
    $q.notify({ type: 'positive', message: 'Request denied' })
    entryDialog.value = false
    await gridRef.value?.reload()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Deny failed' })
  } finally {
    acting.value = false
  }
}

async function doRevoke() {
  if (!selectedEntry.value?.requestId) return
  acting.value = true
  try {
    await worktimeDayOffRequestApi.adminRevoke(selectedEntry.value.requestId, actionComment.value || null)
    $q.notify({ type: 'positive', message: 'Approval revoked' })
    entryDialog.value = false
    await gridRef.value?.reload()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Revoke failed' })
  } finally {
    acting.value = false
  }
}

function onRequestCreated() { gridRef.value?.reload() }
function onHolidaysChanged() { gridRef.value?.reload() }

onMounted(async () => {
  try {
    const res = await worktimeDayOffTypeApi.getActive()
    typeOptions.value = (res.data.data || []).map(t => ({ label: t.name, value: t.id, color: t.color }))
  } catch { /* silent */ }
})
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}
.entry-card {
  min-width: 360px;
  max-width: 440px;
  background: var(--erp-bg-secondary);
  color: var(--erp-text);
}
</style>
