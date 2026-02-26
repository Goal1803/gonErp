<template>
  <q-page padding>
    <div class="page-header">
      <div class="text-h5 text-adaptive text-weight-light">
        <q-icon name="event_busy" color="blue-4" class="q-mr-sm" />
        Day Off
      </div>
      <div class="text-caption text-adaptive-caption q-mt-xs">
        View your quotas and manage time-off requests
      </div>
    </div>

    <div v-if="loading" class="text-center q-pa-xl">
      <q-spinner color="green-5" size="40px" />
    </div>

    <template v-else>
      <!-- My Quotas Section -->
      <div class="text-subtitle1 text-adaptive text-weight-bold q-mb-md">
        <q-icon name="pie_chart" color="green-5" class="q-mr-sm" />
        My Quotas ({{ currentYear }})
      </div>

      <div v-if="!quotas.length" class="text-caption text-adaptive-caption q-pa-md q-mb-lg">
        No quotas assigned yet. Contact your administrator.
      </div>

      <div v-else class="row q-col-gutter-md q-mb-lg">
        <div v-for="quota in quotas" :key="quota.dayOffTypeId" class="col-12 col-sm-6 col-md-4 col-lg-3">
          <DayOffQuotaCard :quota="quota" />
        </div>
      </div>

      <!-- My Requests Section -->
      <div class="text-subtitle1 text-adaptive text-weight-bold q-mb-md">
        <q-icon name="list_alt" color="amber-5" class="q-mr-sm" />
        My Requests
      </div>

      <q-card class="premium-card" flat>
        <q-card-section class="q-pa-none">
          <q-table
            :rows="requests"
            :columns="requestColumns"
            row-key="id"
            flat
            :rows-per-page-options="[10, 25, 50]"
            :loading="loadingRequests"
            :no-data-label="'No requests yet'"
            class="day-off-table"
          >
            <!-- Status Badge -->
            <template #body-cell-status="props">
              <q-td :props="props">
                <q-badge
                  :color="statusColor(props.row.status)"
                  :label="props.row.status"
                  class="text-weight-medium"
                />
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
              <q-td :props="props">
                <q-btn
                  v-if="props.row.status === 'PENDING'"
                  flat
                  dense
                  color="red-4"
                  icon="cancel"
                  size="sm"
                  @click="cancelRequest(props.row)"
                >
                  <q-tooltip>Cancel Request</q-tooltip>
                </q-btn>
              </q-td>
            </template>
          </q-table>
        </q-card-section>
      </q-card>
    </template>

    <!-- FAB: Request Day Off -->
    <q-page-sticky position="bottom-right" :offset="[24, 24]">
      <q-btn
        fab
        color="blue-7"
        icon="add"
        @click="showRequestDialog = true"
      >
        <q-tooltip>Request Day Off</q-tooltip>
      </q-btn>
    </q-page-sticky>

    <!-- Request Dialog -->
    <DayOffRequestDialog
      v-model="showRequestDialog"
      @created="onRequestCreated"
    />
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { worktimeDayOffQuotaApi, worktimeDayOffRequestApi } from 'src/api/worktime'
import DayOffQuotaCard from 'src/components/DayOffQuotaCard.vue'
import DayOffRequestDialog from 'src/components/DayOffRequestDialog.vue'

const $q = useQuasar()

const loading = ref(true)
const loadingRequests = ref(false)
const quotas = ref([])
const requests = ref([])
const showRequestDialog = ref(false)

const currentYear = new Date().getFullYear()

const requestColumns = [
  { name: 'status', label: 'Status', field: 'status', align: 'left', sortable: true },
  { name: 'dayOffTypeName', label: 'Type', field: 'dayOffTypeName', align: 'left', sortable: true },
  { name: 'dates', label: 'Dates', field: 'startDate', align: 'left', sortable: true },
  { name: 'totalDays', label: 'Days', field: 'totalDays', align: 'center', sortable: true },
  { name: 'reason', label: 'Reason', field: 'reason', align: 'left' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

function statusColor(status) {
  const map = {
    PENDING: 'amber-8',
    APPROVED: 'green-7',
    DENIED: 'red-7',
    CANCELLED: 'grey-7'
  }
  return map[status] || 'grey-7'
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr + 'T00:00:00')
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  } catch {
    return dateStr
  }
}

async function loadQuotas() {
  try {
    const res = await worktimeDayOffQuotaApi.getMyQuotas(currentYear)
    quotas.value = res.data.data || []
  } catch (e) {
    console.error('Failed to load quotas', e)
  }
}

async function loadRequests() {
  loadingRequests.value = true
  try {
    const res = await worktimeDayOffRequestApi.getMyRequests()
    requests.value = res.data.data || []
  } catch (e) {
    console.error('Failed to load requests', e)
  } finally {
    loadingRequests.value = false
  }
}

async function cancelRequest(request) {
  $q.dialog({
    title: 'Cancel Request',
    message: `Cancel your ${request.dayOffTypeName} request for ${formatDate(request.startDate)}?`,
    cancel: true,
    persistent: true
  }).onOk(async () => {
    try {
      await worktimeDayOffRequestApi.cancel(request.id)
      $q.notify({ type: 'positive', message: 'Request cancelled' })
      await Promise.all([loadQuotas(), loadRequests()])
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to cancel request' })
    }
  })
}

function onRequestCreated() {
  loadQuotas()
  loadRequests()
}

onMounted(async () => {
  loading.value = true
  await Promise.all([loadQuotas(), loadRequests()])
  loading.value = false
})
</script>

<style scoped>
.color-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.day-off-table {
  background: transparent;
}
.day-off-table :deep(th) {
  color: var(--erp-text-secondary) !important;
  font-weight: 600;
}
.day-off-table :deep(td) {
  border-color: var(--erp-border-subtle) !important;
}
.day-off-table :deep(.q-table__bottom) {
  color: var(--erp-text-secondary);
}
</style>
