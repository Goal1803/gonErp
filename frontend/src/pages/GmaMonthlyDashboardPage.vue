<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/finance" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">Monthly Accounting</div>
        <div class="text-caption text-grey-5">Manage monthly financial reports</div>
      </div>
      <q-space />
      <q-btn flat color="grey-5" icon="rule" label="Rules" no-caps to="/finance/gma/rules" class="q-mr-sm" />
      <q-btn flat color="grey-5" icon="shopping_cart" label="Amazon" no-caps to="/finance/gma/amazon" class="q-mr-sm" />
      <q-btn v-if="canManage" flat color="grey-5" icon="share" label="Share" no-caps to="/finance/gma/share" class="q-mr-sm" />
      <q-btn
        v-if="canManage"
        unelevated
        color="green-7"
        icon="add"
        label="New Report"
        no-caps
        @click="showCreateDialog = true"
      />
    </div>

    <!-- Reports List -->
    <q-table
      :rows="reports"
      :columns="columns"
      row-key="id"
      flat
      bordered
      :loading="loading"
      class="erp-table"
      :pagination="{ rowsPerPage: 20 }"
    >
      <template v-slot:body-cell-period="props">
        <q-td :props="props">
          <span class="text-weight-medium text-white">
            {{ monthName(props.row.month) }} {{ props.row.year }}
          </span>
        </q-td>
      </template>
      <template v-slot:body-cell-status="props">
        <q-td :props="props">
          <q-badge
            :color="statusColor(props.row.status)"
            :label="props.row.status"
          />
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn flat dense icon="open_in_new" color="green-5" @click="openReport(props.row)">
            <q-tooltip>Open</q-tooltip>
          </q-btn>
          <q-btn v-if="canManage" flat dense icon="delete" color="red-4" @click="confirmDelete(props.row)">
            <q-tooltip>Delete</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <!-- Create Dialog -->
    <q-dialog v-model="showCreateDialog">
      <q-card style="min-width: 350px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">New Monthly Report</div>
        </q-card-section>
        <q-card-section>
          <div class="row q-gutter-md">
            <q-select
              v-model="newReport.year"
              :options="yearOptions"
              label="Year"
              dense outlined
              class="col"
              dark
            />
            <q-select
              v-model="newReport.month"
              :options="monthOptions"
              option-value="value"
              option-label="label"
              emit-value
              map-options
              label="Month"
              dense outlined
              class="col"
              dark
            />
          </div>
          <q-input
            v-model="newReport.notes"
            label="Notes (optional)"
            type="textarea"
            rows="2"
            dense outlined
            class="q-mt-md"
            dark
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated label="Create" color="green-7" no-caps @click="handleCreate" :loading="creating" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { useFinanceStore } from 'src/stores/financeStore'

const $q = useQuasar()
const router = useRouter()
const authStore = useAuthStore()
const financeStore = useFinanceStore()

const reports = computed(() => financeStore.reports)
const loading = computed(() => financeStore.loading)
const showCreateDialog = ref(false)
const creating = ref(false)

const canManage = computed(() => {
  if (authStore.isAdmin) return true
  const role = authStore.currentUser?.financeRole
  return role === 'FINANCE_CFO' || role === 'FINANCE_ACCOUNTANT_MANAGER'
})

const now = new Date()
const newReport = ref({
  year: now.getFullYear(),
  month: now.getMonth() + 1,
  notes: ''
})

const yearOptions = Array.from({ length: 10 }, (_, i) => now.getFullYear() - 5 + i)

const monthOptions = [
  { label: 'January', value: 1 }, { label: 'February', value: 2 },
  { label: 'March', value: 3 }, { label: 'April', value: 4 },
  { label: 'May', value: 5 }, { label: 'June', value: 6 },
  { label: 'July', value: 7 }, { label: 'August', value: 8 },
  { label: 'September', value: 9 }, { label: 'October', value: 10 },
  { label: 'November', value: 11 }, { label: 'December', value: 12 }
]

const columns = [
  { name: 'period', label: 'Period', field: 'month', align: 'left', sortable: true },
  { name: 'status', label: 'Status', field: 'status', align: 'center' },
  { name: 'notes', label: 'Notes', field: 'notes', align: 'left' },
  { name: 'createdBy', label: 'Created By', field: 'createdBy', align: 'left' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

function monthName(m) {
  return monthOptions.find(o => o.value === m)?.label || m
}

function statusColor(status) {
  const map = { DRAFT: 'blue-grey-7', REVIEWED: 'blue-7', SHARED: 'green-7', ARCHIVED: 'grey-7' }
  return map[status] || 'grey-7'
}

function openReport(row) {
  router.push(`/finance/gma/report/${row.id}`)
}

function confirmDelete(row) {
  $q.dialog({
    title: 'Delete Report',
    message: `Delete ${monthName(row.month)} ${row.year} report?`,
    cancel: true,
    persistent: false,
    color: 'red'
  }).onOk(async () => {
    try {
      await financeStore.deleteReport(row.id)
      $q.notify({ type: 'positive', message: 'Report deleted' })
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to delete' })
    }
  })
}

async function handleCreate() {
  creating.value = true
  try {
    await financeStore.createReport(newReport.value)
    showCreateDialog.value = false
    $q.notify({ type: 'positive', message: 'Report created' })
    newReport.value = { year: now.getFullYear(), month: now.getMonth() + 1, notes: '' }
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to create' })
  } finally {
    creating.value = false
  }
}

onMounted(() => financeStore.fetchReports())
</script>
