<template>
  <q-page padding>
    <div class="page-header">
      <div class="text-h5 text-adaptive text-weight-light">
        <q-icon name="admin_panel_settings" color="orange-5" class="q-mr-sm" />
        Day Off Management
      </div>
      <div class="text-caption text-adaptive-caption q-mt-xs">
        Review requests, manage quotas, and configure day-off types
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
      <q-tab name="pending" icon="pending_actions" label="Pending Requests">
        <q-badge v-if="pendingRequests.length" color="red-7" floating>{{ pendingRequests.length }}</q-badge>
      </q-tab>
      <q-tab name="quotas" icon="pie_chart" label="All Quotas" />
      <q-tab name="types" icon="category" label="Day Off Types" />
    </q-tabs>

    <!-- Tab Panels -->
    <q-tab-panels v-model="activeTab" animated class="bg-transparent">

      <!-- ============ PENDING REQUESTS TAB ============ -->
      <q-tab-panel name="pending" class="q-pa-none">
        <div v-if="loadingPending" class="text-center q-pa-xl">
          <q-spinner color="green-5" size="32px" />
        </div>

        <div v-else-if="!pendingRequests.length" class="text-center q-pa-xl">
          <q-icon name="check_circle" color="green-5" size="48px" />
          <div class="text-subtitle1 text-adaptive q-mt-md">No pending requests</div>
          <div class="text-caption text-adaptive-caption">All caught up!</div>
        </div>

        <q-card v-else class="premium-card" flat>
          <q-card-section class="q-pa-none">
            <q-table
              :rows="pendingRequests"
              :columns="pendingColumns"
              row-key="id"
              flat
              :rows-per-page-options="[10, 25, 50]"
              class="admin-table"
            >
              <!-- User -->
              <template #body-cell-userName="props">
                <q-td :props="props">
                  <span class="text-adaptive text-weight-medium">{{ props.row.userName }}</span>
                </q-td>
              </template>

              <!-- Type with color -->
              <template #body-cell-dayOffTypeName="props">
                <q-td :props="props">
                  <div class="flex items-center no-wrap">
                    <div
                      class="color-dot q-mr-sm"
                      :style="{ background: props.row.dayOffTypeColor || '#4CAF50' }"
                    />
                    <span class="text-adaptive">{{ props.row.dayOffTypeName }}</span>
                  </div>
                </q-td>
              </template>

              <!-- Dates -->
              <template #body-cell-dates="props">
                <q-td :props="props">
                  <span class="text-adaptive">{{ formatDate(props.row.startDate) }}</span>
                  <span v-if="props.row.startDate !== props.row.endDate" class="text-adaptive-caption">
                    &mdash; {{ formatDate(props.row.endDate) }}
                  </span>
                  <div v-if="props.row.halfDayType && props.row.halfDayType !== 'FULL_DAY'" class="text-caption text-adaptive-caption">
                    {{ props.row.halfDayType === 'MORNING' ? 'Morning' : 'Afternoon' }} half day
                  </div>
                </q-td>
              </template>

              <!-- Total Days -->
              <template #body-cell-totalDays="props">
                <q-td :props="props">
                  <span class="text-adaptive text-weight-medium">{{ props.row.totalDays }}</span>
                </q-td>
              </template>

              <!-- Reason -->
              <template #body-cell-reason="props">
                <q-td :props="props">
                  <span class="text-adaptive-secondary ellipsis" style="max-width: 200px; display: inline-block;">
                    {{ props.row.reason || '-' }}
                  </span>
                </q-td>
              </template>

              <!-- Actions -->
              <template #body-cell-actions="props">
                <q-td :props="props" class="q-gutter-xs">
                  <q-btn
                    flat dense
                    color="green-5"
                    icon="check_circle"
                    size="sm"
                    @click="approveRequest(props.row)"
                  >
                    <q-tooltip>Approve</q-tooltip>
                  </q-btn>
                  <q-btn
                    flat dense
                    color="red-4"
                    icon="cancel"
                    size="sm"
                    @click="denyRequest(props.row)"
                  >
                    <q-tooltip>Deny</q-tooltip>
                  </q-btn>
                </q-td>
              </template>
            </q-table>
          </q-card-section>
        </q-card>
      </q-tab-panel>

      <!-- ============ ALL QUOTAS TAB ============ -->
      <q-tab-panel name="quotas" class="q-pa-none">
        <div class="row items-center q-mb-md">
          <q-select
            v-model="quotaYear"
            :options="yearOptions"
            label="Year"
            outlined
            dense
            color="green-5"
            style="min-width: 120px"
            @update:model-value="loadAllQuotas"
          />
          <q-space />
          <q-input
            v-model="quotaSearch"
            dense
            outlined
            color="green-5"
            placeholder="Search user..."
            class="q-ml-md"
            style="min-width: 200px"
          >
            <template #prepend>
              <q-icon name="search" color="grey-5" />
            </template>
          </q-input>
        </div>

        <div v-if="loadingQuotas" class="text-center q-pa-xl">
          <q-spinner color="green-5" size="32px" />
        </div>

        <q-card v-else class="premium-card" flat>
          <q-card-section class="q-pa-none">
            <q-table
              :rows="filteredQuotas"
              :columns="quotaColumns"
              row-key="id"
              flat
              :rows-per-page-options="[15, 30, 50]"
              class="admin-table"
            >
              <!-- User -->
              <template #body-cell-userName="props">
                <q-td :props="props">
                  <span class="text-adaptive text-weight-medium">{{ props.row.userName }}</span>
                </q-td>
              </template>

              <!-- Type with color -->
              <template #body-cell-dayOffTypeName="props">
                <q-td :props="props">
                  <div class="flex items-center no-wrap">
                    <div
                      class="color-dot q-mr-sm"
                      :style="{ background: props.row.dayOffTypeColor || '#4CAF50' }"
                    />
                    <span class="text-adaptive">{{ props.row.dayOffTypeName }}</span>
                  </div>
                </q-td>
              </template>

              <!-- Total Days (editable) -->
              <template #body-cell-totalDays="props">
                <q-td :props="props">
                  <span class="text-adaptive text-weight-bold cursor-pointer" style="border-bottom: 1px dashed var(--erp-border);">
                    {{ props.row.totalDays }}
                    <q-tooltip>Click to edit</q-tooltip>
                  </span>
                  <q-popup-edit
                    v-model="props.row.totalDays"
                    v-slot="scope"
                    buttons
                    @save="(val) => saveInlineQuota(props.row, val)"
                  >
                    <q-input
                      v-model.number="scope.value"
                      type="number"
                      min="0"
                      step="0.5"
                      dense
                      autofocus
                      color="green-5"
                    />
                  </q-popup-edit>
                </q-td>
              </template>

              <!-- Used Days -->
              <template #body-cell-usedDays="props">
                <q-td :props="props">
                  <span class="text-adaptive">{{ props.row.usedDays }}</span>
                </q-td>
              </template>

              <!-- Remaining -->
              <template #body-cell-remaining="props">
                <q-td :props="props">
                  <span :style="{ color: remainingColor(props.row) }" class="text-weight-medium">
                    {{ Math.max(0, (props.row.totalDays || 0) - (props.row.usedDays || 0)) }}
                  </span>
                </q-td>
              </template>

              <!-- Carried Over -->
              <template #body-cell-carriedOverDays="props">
                <q-td :props="props">
                  <span class="text-adaptive-secondary">{{ props.row.carriedOverDays || 0 }}</span>
                </q-td>
              </template>

              <!-- Edit Action -->
              <template #body-cell-actions="props">
                <q-td :props="props">
                  <q-btn
                    flat dense
                    color="green-5"
                    icon="edit"
                    size="sm"
                    @click="openQuotaEdit(props.row)"
                  >
                    <q-tooltip>Edit Quota</q-tooltip>
                  </q-btn>
                </q-td>
              </template>
            </q-table>
          </q-card-section>
        </q-card>
      </q-tab-panel>

      <!-- ============ DAY OFF TYPES TAB ============ -->
      <q-tab-panel name="types" class="q-pa-none">
        <div class="row items-center q-mb-md">
          <q-space />
          <q-btn
            color="green-7"
            icon="add"
            label="New Type"
            no-caps
            unelevated
            @click="openTypeCreate"
          />
        </div>

        <div v-if="loadingTypes" class="text-center q-pa-xl">
          <q-spinner color="green-5" size="32px" />
        </div>

        <div v-else-if="!dayOffTypes.length" class="text-center q-pa-xl">
          <q-icon name="category" color="grey-6" size="48px" />
          <div class="text-subtitle1 text-adaptive q-mt-md">No day-off types configured</div>
          <div class="text-caption text-adaptive-caption">Create your first day-off type to get started</div>
        </div>

        <div v-else class="q-gutter-y-sm">
          <q-card
            v-for="dt in dayOffTypes"
            :key="dt.id"
            class="premium-card"
            flat
          >
            <q-card-section class="row items-center no-wrap q-pa-md">
              <!-- Color chip + Name -->
              <div class="flex items-center no-wrap col">
                <q-chip
                  :style="{ background: dt.color || '#4CAF50', color: '#fff' }"
                  dense
                  class="q-mr-sm"
                >
                  {{ dt.name }}
                </q-chip>
                <q-badge v-if="!dt.active" color="grey-7" label="Inactive" class="q-ml-sm" />
                <q-badge v-if="dt.isPaid" color="green-8" label="Paid" class="q-ml-sm" />
                <q-badge v-else color="orange-8" label="Unpaid" class="q-ml-sm" />
              </div>

              <!-- Default Quota -->
              <div class="text-caption text-adaptive-secondary q-mx-md">
                Default: {{ dt.defaultQuota }} days
              </div>

              <!-- Order -->
              <div class="text-caption text-adaptive-caption q-mx-md">
                Order: {{ dt.displayOrder }}
              </div>

              <!-- Actions -->
              <div class="q-gutter-xs">
                <q-btn
                  flat dense
                  color="green-5"
                  icon="edit"
                  size="sm"
                  @click="openTypeEdit(dt)"
                >
                  <q-tooltip>Edit</q-tooltip>
                </q-btn>
                <q-btn
                  flat dense
                  color="red-4"
                  icon="delete"
                  size="sm"
                  @click="deleteType(dt)"
                >
                  <q-tooltip>Delete</q-tooltip>
                </q-btn>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </q-tab-panel>
    </q-tab-panels>

    <!-- Approve Dialog -->
    <q-dialog v-model="showApproveDialog" persistent>
      <q-card style="min-width: 400px" class="premium-card">
        <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
          <q-icon name="check_circle" color="green-5" size="md" />
          <div>
            <div class="text-h6 text-adaptive text-weight-medium">Approve Request</div>
            <div class="text-caption text-adaptive-caption">
              {{ selectedRequest?.userName }} - {{ selectedRequest?.dayOffTypeName }}
            </div>
          </div>
        </q-card-section>
        <q-card-section>
          <q-input
            v-model="approveComment"
            label="Comment (optional)"
            outlined
            color="green-5"
            type="textarea"
            rows="2"
            autogrow
          />
        </q-card-section>
        <q-card-actions align="right" style="border-top: 1px solid rgba(46,125,50,0.2)" class="q-pa-md">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup />
          <q-btn
            label="Approve"
            color="green-7"
            unelevated
            icon="check"
            :loading="processing"
            @click="confirmApprove"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Deny Dialog -->
    <q-dialog v-model="showDenyDialog" persistent>
      <q-card style="min-width: 400px" class="premium-card">
        <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
          <q-icon name="cancel" color="red-4" size="md" />
          <div>
            <div class="text-h6 text-adaptive text-weight-medium">Deny Request</div>
            <div class="text-caption text-adaptive-caption">
              {{ selectedRequest?.userName }} - {{ selectedRequest?.dayOffTypeName }}
            </div>
          </div>
        </q-card-section>
        <q-card-section>
          <q-input
            v-model="denyComment"
            label="Reason for denial *"
            outlined
            color="green-5"
            type="textarea"
            rows="2"
            autogrow
            :rules="[v => !!v?.trim() || 'Please provide a reason']"
            lazy-rules
          />
        </q-card-section>
        <q-card-actions align="right" style="border-top: 1px solid rgba(46,125,50,0.2)" class="q-pa-md">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup />
          <q-btn
            label="Deny"
            color="red-7"
            unelevated
            icon="block"
            :loading="processing"
            :disable="!denyComment?.trim()"
            @click="confirmDeny"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Quota Edit Dialog -->
    <DayOffAdminQuotaDialog
      v-model="showQuotaDialog"
      :quota="selectedQuota"
      :user-name="selectedQuotaUserName"
      @saved="onQuotaSaved"
    />

    <!-- Type Settings Dialog -->
    <DayOffTypeSettingsDialog
      v-model="showTypeDialog"
      :day-off-type="selectedType"
      @saved="onTypeSaved"
    />
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import {
  worktimeDayOffRequestApi,
  worktimeDayOffQuotaApi,
  worktimeDayOffTypeApi
} from 'src/api/worktime'
import DayOffAdminQuotaDialog from 'src/components/DayOffAdminQuotaDialog.vue'
import DayOffTypeSettingsDialog from 'src/components/DayOffTypeSettingsDialog.vue'

const $q = useQuasar()

// --- State ---
const activeTab = ref('pending')

// Pending Requests
const loadingPending = ref(false)
const pendingRequests = ref([])
const showApproveDialog = ref(false)
const showDenyDialog = ref(false)
const selectedRequest = ref(null)
const approveComment = ref('')
const denyComment = ref('')
const processing = ref(false)

// Quotas
const loadingQuotas = ref(false)
const allQuotas = ref([])
const quotaSearch = ref('')
const quotaYear = ref(new Date().getFullYear())
const showQuotaDialog = ref(false)
const selectedQuota = ref(null)
const selectedQuotaUserName = ref('')

// Types
const loadingTypes = ref(false)
const dayOffTypes = ref([])
const showTypeDialog = ref(false)
const selectedType = ref(null)

const currentYear = new Date().getFullYear()
const yearOptions = [currentYear - 1, currentYear, currentYear + 1]

// --- Columns ---
const pendingColumns = [
  { name: 'userName', label: 'User', field: 'userName', align: 'left', sortable: true },
  { name: 'dayOffTypeName', label: 'Type', field: 'dayOffTypeName', align: 'left', sortable: true },
  { name: 'dates', label: 'Dates', field: 'startDate', align: 'left', sortable: true },
  { name: 'totalDays', label: 'Days', field: 'totalDays', align: 'center', sortable: true },
  { name: 'reason', label: 'Reason', field: 'reason', align: 'left' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

const quotaColumns = [
  { name: 'userName', label: 'User', field: 'userName', align: 'left', sortable: true },
  { name: 'dayOffTypeName', label: 'Type', field: 'dayOffTypeName', align: 'left', sortable: true },
  { name: 'totalDays', label: 'Total', field: 'totalDays', align: 'center', sortable: true },
  { name: 'usedDays', label: 'Used', field: 'usedDays', align: 'center', sortable: true },
  { name: 'remaining', label: 'Remaining', field: row => (row.totalDays || 0) - (row.usedDays || 0), align: 'center', sortable: true },
  { name: 'carriedOverDays', label: 'Carried Over', field: 'carriedOverDays', align: 'center', sortable: true },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

// --- Computed ---
const filteredQuotas = computed(() => {
  if (!quotaSearch.value) return allQuotas.value
  const needle = quotaSearch.value.toLowerCase()
  return allQuotas.value.filter(q =>
    (q.userName || '').toLowerCase().includes(needle) ||
    (q.dayOffTypeName || '').toLowerCase().includes(needle)
  )
})

// --- Helpers ---
function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr + 'T00:00:00')
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  } catch {
    return dateStr
  }
}

function remainingColor(row) {
  const remaining = Math.max(0, (row.totalDays || 0) - (row.usedDays || 0))
  if (remaining <= 0) return '#ef5350'
  if (remaining <= 2) return '#ffa726'
  return '#66bb6a'
}

// --- Load Data ---
async function loadPendingRequests() {
  loadingPending.value = true
  try {
    const res = await worktimeDayOffRequestApi.getPending()
    pendingRequests.value = res.data.data || []
  } catch (e) {
    console.error('Failed to load pending requests', e)
  } finally {
    loadingPending.value = false
  }
}

async function loadAllQuotas() {
  loadingQuotas.value = true
  try {
    const res = await worktimeDayOffQuotaApi.bulkAssign({ year: quotaYear.value, fetchOnly: true })
    allQuotas.value = res.data.data || []
  } catch (e) {
    // Fallback: the API might not support fetchOnly, try alternative
    console.error('Failed to load all quotas', e)
    allQuotas.value = []
  } finally {
    loadingQuotas.value = false
  }
}

async function loadDayOffTypes() {
  loadingTypes.value = true
  try {
    const res = await worktimeDayOffTypeApi.getAll()
    dayOffTypes.value = res.data.data || []
  } catch (e) {
    console.error('Failed to load day-off types', e)
  } finally {
    loadingTypes.value = false
  }
}

// --- Pending Request Actions ---
function approveRequest(request) {
  selectedRequest.value = request
  approveComment.value = ''
  showApproveDialog.value = true
}

function denyRequest(request) {
  selectedRequest.value = request
  denyComment.value = ''
  showDenyDialog.value = true
}

async function confirmApprove() {
  if (!selectedRequest.value) return
  processing.value = true
  try {
    await worktimeDayOffRequestApi.approve(selectedRequest.value.id, approveComment.value || null)
    $q.notify({ type: 'positive', message: 'Request approved' })
    showApproveDialog.value = false
    await loadPendingRequests()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to approve' })
  } finally {
    processing.value = false
  }
}

async function confirmDeny() {
  if (!selectedRequest.value || !denyComment.value?.trim()) return
  processing.value = true
  try {
    await worktimeDayOffRequestApi.deny(selectedRequest.value.id, denyComment.value)
    $q.notify({ type: 'positive', message: 'Request denied' })
    showDenyDialog.value = false
    await loadPendingRequests()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to deny' })
  } finally {
    processing.value = false
  }
}

// --- Quota Actions ---
function openQuotaEdit(quota) {
  selectedQuota.value = { ...quota }
  selectedQuotaUserName.value = quota.userName || ''
  showQuotaDialog.value = true
}

async function saveInlineQuota(row, newTotalDays) {
  try {
    await worktimeDayOffQuotaApi.setQuota(row.userId, row.dayOffTypeId, {
      totalDays: newTotalDays,
      carriedOverDays: row.carriedOverDays || 0,
      year: quotaYear.value
    })
    $q.notify({ type: 'positive', message: 'Quota updated' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to update quota' })
    await loadAllQuotas()
  }
}

function onQuotaSaved() {
  loadAllQuotas()
}

// --- Type Actions ---
function openTypeCreate() {
  selectedType.value = null
  showTypeDialog.value = true
}

function openTypeEdit(dt) {
  selectedType.value = { ...dt }
  showTypeDialog.value = true
}

function deleteType(dt) {
  $q.dialog({
    title: 'Delete Day Off Type',
    message: `Are you sure you want to delete "${dt.name}"? This action cannot be undone.`,
    cancel: true,
    persistent: true
  }).onOk(async () => {
    try {
      await worktimeDayOffTypeApi.delete(dt.id)
      $q.notify({ type: 'positive', message: 'Day off type deleted' })
      await loadDayOffTypes()
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to delete type' })
    }
  })
}

function onTypeSaved() {
  loadDayOffTypes()
}

// --- Init ---
onMounted(() => {
  loadPendingRequests()
  loadAllQuotas()
  loadDayOffTypes()
})
</script>

<style scoped>
.color-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.admin-table {
  background: transparent;
}
.admin-table :deep(th) {
  color: var(--erp-text-secondary) !important;
  font-weight: 600;
}
.admin-table :deep(td) {
  border-color: var(--erp-border-subtle) !important;
}
.admin-table :deep(.q-table__bottom) {
  color: var(--erp-text-secondary);
}
</style>
