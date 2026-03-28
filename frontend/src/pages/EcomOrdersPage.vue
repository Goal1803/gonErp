<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/ecommerce" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">Orders</div>
        <div class="text-caption text-grey-5">Manage e-commerce orders</div>
      </div>
      <q-space />
      <q-btn
        unelevated
        color="cyan-7"
        icon="file_upload"
        label="Import"
        no-caps
        @click="showImportDialog = true"
      />
    </div>

    <!-- Filters toolbar -->
    <div class="row q-gutter-sm q-mb-md items-center">
      <q-select
        v-model="filters.storeId"
        :options="storeOptions"
        label="Store"
        dense outlined dark
        emit-value map-options
        clearable
        style="min-width: 180px"
        @update:model-value="loadOrders"
      />
      <q-select
        v-model="filters.status"
        :options="statusOptions"
        label="Status"
        dense outlined dark
        multiple
        emit-value map-options
        clearable
        style="min-width: 200px"
        @update:model-value="loadOrders"
      />
      <q-input
        v-model="filters.dateFrom"
        label="From"
        type="date"
        dense outlined dark
        style="min-width: 150px"
        @update:model-value="loadOrders"
      />
      <q-input
        v-model="filters.dateTo"
        label="To"
        type="date"
        dense outlined dark
        style="min-width: 150px"
        @update:model-value="loadOrders"
      />
      <q-input
        v-model="filters.search"
        label="Search"
        dense outlined dark
        clearable
        debounce="400"
        style="min-width: 200px"
        @update:model-value="loadOrders"
      >
        <template v-slot:prepend>
          <q-icon name="search" color="grey-5" />
        </template>
      </q-input>
    </div>

    <!-- Orders table -->
    <q-table
      :rows="orders"
      :columns="columns"
      row-key="id"
      flat
      bordered
      :loading="loading"
      class="erp-table"
      v-model:pagination="pagination"
      @request="onRequest"
      @row-click="(evt, row) => router.push(`/ecommerce/orders/${row.id}`)"
    >
      <template v-slot:body-cell-productName="props">
        <q-td :props="props">
          <div class="ellipsis" style="max-width: 220px">
            {{ props.value }}
            <span v-if="props.row.items && props.row.items.length > 1" class="text-grey-5 text-caption"> +{{ props.row.items.length - 1 }} more</span>
            <q-tooltip v-if="props.row.items && props.row.items.length > 0">
              <div v-for="item in props.row.items" :key="item.id">{{ item.productName || item.sku }} x{{ item.quantity }}</div>
            </q-tooltip>
          </div>
        </q-td>
      </template>
      <template v-slot:body-cell-status="props">
        <q-td :props="props">
          <q-badge :color="statusColor(props.row.status)" :label="formatStatus(props.row.status)" />
        </q-td>
      </template>
      <template v-slot:body-cell-orderTotal="props">
        <q-td :props="props" class="text-right text-weight-medium">
          {{ formatCurrency(props.row.orderTotal) }}
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn flat dense icon="visibility" color="cyan-4" @click.stop="router.push(`/ecommerce/orders/${props.row.id}`)">
            <q-tooltip>View Detail</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <!-- Import Dialog -->
    <q-dialog v-model="showImportDialog" persistent :maximized="!!importResult">
      <q-card :style="importResult ? 'max-width: 900px; width: 100%;' : 'min-width: 520px;'" style="background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="row items-center">
            <div class="text-h6 text-white">Import Etsy Orders</div>
            <q-space />
            <div v-if="importResult" class="text-caption text-grey-5">{{ importResult.totalRowsParsed || 0 }} rows parsed</div>
          </div>
        </q-card-section>
        <q-card-section class="q-gutter-md" v-if="!importResult">
          <q-select
            v-model="importForm.storeId"
            :options="storeOptions"
            label="Store *"
            dense outlined dark
            emit-value map-options
            :rules="[v => !!v || 'Store is required']"
          />
          <q-file
            v-model="importForm.ordersFile"
            label="Etsy Sold Orders CSV"
            dense outlined dark
            accept=".csv"
          >
            <template v-slot:prepend>
              <q-icon name="attach_file" color="grey-5" />
            </template>
          </q-file>
          <q-file
            v-model="importForm.itemsFile"
            label="Etsy Sold Order Items CSV"
            dense outlined dark
            accept=".csv"
          >
            <template v-slot:prepend>
              <q-icon name="attach_file" color="grey-5" />
            </template>
          </q-file>
          <div class="text-caption text-grey-5">Upload at least one file. You can import orders and items separately — they will be merged by Order ID.</div>
        </q-card-section>

        <!-- Import result -->
        <q-card-section v-if="importResult" style="max-height: 70vh; overflow-y: auto;">
          <!-- Summary badges -->
          <div class="row q-gutter-sm q-mb-md">
            <q-badge color="green-7" class="q-pa-sm text-body2">
              {{ importResult.ordersCreated || 0 }} Created
            </q-badge>
            <q-badge color="cyan-7" class="q-pa-sm text-body2">
              {{ importResult.ordersUpdated || 0 }} Updated
            </q-badge>
            <q-badge color="blue-7" class="q-pa-sm text-body2">
              {{ importResult.orderItemsImported || 0 }} Items
            </q-badge>
            <q-badge color="orange-7" class="q-pa-sm text-body2">
              {{ importResult.skipped || 0 }} Skipped
            </q-badge>
            <q-badge v-if="importResult.errors && importResult.errors.length" color="red-7" class="q-pa-sm text-body2">
              {{ importResult.errors.length }} Errors
            </q-badge>
            <q-badge v-if="importResult.ignoredRows && importResult.ignoredRows.length" color="grey-7" class="q-pa-sm text-body2">
              {{ importResult.ignoredRows.length }} Ignored
            </q-badge>
          </div>

          <!-- Errors -->
          <q-expansion-item
            v-if="importResult.errors && importResult.errors.length"
            icon="error"
            :label="`Errors (${importResult.errors.length})`"
            header-class="text-red-4"
            default-opened
            dense dark
          >
            <q-table
              :rows="importResult.errors"
              :columns="outcomeColumns"
              row-key="orderId"
              flat bordered dense dark
              class="import-detail-table q-mb-sm"
              :pagination="{ rowsPerPage: 0 }"
              hide-pagination
            />
          </q-expansion-item>

          <!-- Ignored rows -->
          <q-expansion-item
            v-if="importResult.ignoredRows && importResult.ignoredRows.length"
            icon="visibility_off"
            :label="`Ignored Rows (${importResult.ignoredRows.length})`"
            header-class="text-grey-5"
            default-opened
            dense dark
          >
            <q-table
              :rows="importResult.ignoredRows"
              :columns="outcomeColumns"
              row-key="orderId"
              flat bordered dense dark
              class="import-detail-table q-mb-sm"
              :pagination="{ rowsPerPage: 0 }"
              hide-pagination
            />
          </q-expansion-item>

          <!-- Skipped rows -->
          <q-expansion-item
            v-if="importResult.skippedRows && importResult.skippedRows.length"
            icon="skip_next"
            :label="`Skipped (${importResult.skippedRows.length})`"
            header-class="text-orange-4"
            dense dark
          >
            <q-table
              :rows="importResult.skippedRows"
              :columns="outcomeColumns"
              row-key="orderId"
              flat bordered dense dark
              class="import-detail-table q-mb-sm"
              :pagination="{ rowsPerPage: 0 }"
              hide-pagination
            />
          </q-expansion-item>

          <!-- Updated -->
          <q-expansion-item
            v-if="importResult.updated && importResult.updated.length"
            icon="update"
            :label="`Updated (${importResult.updated.length})`"
            header-class="text-cyan-4"
            dense dark
          >
            <q-table
              :rows="importResult.updated"
              :columns="outcomeColumns"
              row-key="orderId"
              flat bordered dense dark
              class="import-detail-table q-mb-sm"
              :pagination="{ rowsPerPage: 0 }"
              hide-pagination
            />
          </q-expansion-item>

          <!-- Created -->
          <q-expansion-item
            v-if="importResult.created && importResult.created.length"
            icon="add_circle"
            :label="`Created (${importResult.created.length})`"
            header-class="text-green-4"
            dense dark
          >
            <q-table
              :rows="importResult.created"
              :columns="outcomeColumns"
              row-key="orderId"
              flat bordered dense dark
              class="import-detail-table q-mb-sm"
              :pagination="{ rowsPerPage: 10 }"
            />
          </q-expansion-item>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Close" color="grey-5" v-close-popup no-caps @click="resetImport" />
          <q-btn
            v-if="importResult"
            flat
            icon="download"
            label="Export Report"
            color="grey-4"
            no-caps
            @click="exportImportReport"
          />
          <q-btn
            v-if="!importResult"
            unelevated
            label="Import"
            color="cyan-7"
            no-caps
            @click="handleImport"
            :loading="importing"
            :disable="!importForm.storeId || (!importForm.ordersFile && !importForm.itemsFile)"
          />
          <q-btn
            v-if="importResult"
            unelevated
            label="Import More"
            color="cyan-7"
            no-caps
            @click="importResult = null"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { ecomOrderApi, ecomImportApi, ecomStoreApi } from 'src/api/ecommerce'

const router = useRouter()
const $q = useQuasar()

const loading = ref(false)
const orders = ref([])
const stores = ref([])
const storeOptions = ref([])

const filters = ref({
  storeId: null,
  status: [],
  dateFrom: '',
  dateTo: '',
  search: ''
})

const pagination = ref({
  page: 1,
  rowsPerPage: 20,
  rowsNumber: 0,
  sortBy: 'orderDate',
  descending: true
})

const statusOptions = [
  { label: 'New Order', value: 'NEW_ORDER' },
  { label: 'Confirmed', value: 'CONFIRMED' },
  { label: 'Designing', value: 'DESIGNING' },
  { label: 'Fulfilled', value: 'FULFILLED' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Cancelled', value: 'CANCELLED' }
]

const columns = [
  { name: 'orderDate', label: 'Order Date', field: 'orderDate', align: 'left', sortable: true, format: v => v ? new Date(v).toLocaleDateString() : '' },
  { name: 'platformOrderId', label: 'Order ID', field: 'platformOrderId', align: 'left', sortable: true },
  { name: 'productName', label: 'Product', field: row => row.items?.[0]?.productName || row.sku || '', align: 'left' },
  { name: 'sku', label: 'SKU', field: row => row.items?.[0]?.sku || row.sku || '', align: 'left', sortable: true },
  { name: 'numberOfItems', label: 'Items', field: 'numberOfItems', align: 'center', sortable: true },
  { name: 'customerName', label: 'Customer', field: 'customerName', align: 'left', sortable: true },
  { name: 'shipCountry', label: 'Country', field: 'shipCountry', align: 'left', sortable: true },
  { name: 'orderTotal', label: 'Order Total', field: 'orderTotal', align: 'right', sortable: true },
  { name: 'status', label: 'Status', field: 'status', align: 'center', sortable: true },
  { name: 'trackingNumber', label: 'Tracking', field: 'trackingNumber', align: 'left' },
  { name: 'actions', label: 'Actions', field: 'actions', align: 'center' }
]

// Import
const showImportDialog = ref(false)
const importing = ref(false)
const importResult = ref(null)
const importForm = ref({
  storeId: null,
  itemsFile: null,
  ordersFile: null
})

const outcomeColumns = [
  { name: 'orderId', label: 'Order / Row', field: 'orderId', align: 'left', style: 'width: 130px' },
  { name: 'reason', label: 'Reason', field: 'reason', align: 'left', style: 'width: 200px' },
  { name: 'detail', label: 'Detail', field: 'detail', align: 'left' }
]

function statusColor (status) {
  const map = {
    NEW_ORDER: 'blue-7',
    CONFIRMED: 'green-7',
    DESIGNING: 'purple-7',
    FULFILLED: 'teal-7',
    COMPLETED: 'grey-7',
    CANCELLED: 'red-7'
  }
  return map[status] || 'orange-7'
}

function formatStatus (status) {
  if (!status) return ''
  return status.replace(/_/g, ' ')
}

function formatCurrency (val) {
  if (val == null) return ''
  return Number(val).toFixed(2)
}

async function loadStores () {
  try {
    const res = await ecomStoreApi.getAll()
    stores.value = res.data.data || []
    storeOptions.value = stores.value.map(s => ({ label: s.name, value: s.id }))
  } catch (e) {
    console.error('Failed to load stores', e)
  }
}

async function loadOrders () {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      limit: pagination.value.rowsPerPage,
      sortBy: pagination.value.sortBy,
      descending: pagination.value.descending
    }
    if (filters.value.storeId) params.storeId = filters.value.storeId
    if (filters.value.status && filters.value.status.length) params.status = filters.value.status.join(',')
    if (filters.value.dateFrom) params.dateFrom = filters.value.dateFrom
    if (filters.value.dateTo) params.dateTo = filters.value.dateTo
    if (filters.value.search) params.search = filters.value.search

    const res = await ecomOrderApi.getAll(params)
    orders.value = res.data.data || []
    pagination.value.rowsNumber = orders.value.length
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to load orders' })
  } finally {
    loading.value = false
  }
}

function onRequest (props) {
  pagination.value.page = props.pagination.page
  pagination.value.rowsPerPage = props.pagination.rowsPerPage
  pagination.value.sortBy = props.pagination.sortBy
  pagination.value.descending = props.pagination.descending
  loadOrders()
}

async function handleImport () {
  if (!importForm.value.storeId || (!importForm.value.ordersFile && !importForm.value.itemsFile)) return
  importing.value = true
  importResult.value = null
  $q.loading.show({ message: 'Importing orders...' })
  try {
    const res = await ecomImportApi.importEtsy(
      importForm.value.storeId,
      importForm.value.ordersFile,
      importForm.value.itemsFile
    )
    importResult.value = res.data.data
    const r = importResult.value
    const hasIssues = (r.errors?.length || 0) + (r.ignoredRows?.length || 0)
    $q.notify({
      type: hasIssues ? 'warning' : 'positive',
      message: `Import done: ${r.ordersCreated || 0} created, ${r.ordersUpdated || 0} updated, ${r.skipped || 0} skipped` + (hasIssues ? ` — ${hasIssues} issue(s)` : '')
    })
    loadOrders()
  } catch (e) {
    const msg = e.response?.data?.message || 'Import failed'
    $q.notify({ type: 'negative', message: msg })
  } finally {
    importing.value = false
    $q.loading.hide()
  }
}

function exportImportReport () {
  if (!importResult.value) return
  const r = importResult.value
  const rows = [['Status', 'Order ID', 'Reason', 'Detail']]

  const addSection = (label, list) => {
    if (!list || !list.length) return
    for (const item of list) {
      rows.push([label, item.orderId || '', item.reason || '', item.detail || ''])
    }
  }

  addSection('ERROR', r.errors)
  addSection('IGNORED', r.ignoredRows)
  addSection('SKIPPED', r.skippedRows)
  addSection('UPDATED', r.updated)
  addSection('CREATED', r.created)

  // Summary row
  rows.push([])
  rows.push(['Summary'])
  rows.push(['Total Rows Parsed', r.totalRowsParsed || 0])
  rows.push(['Orders Created', r.ordersCreated || 0])
  rows.push(['Orders Updated', r.ordersUpdated || 0])
  rows.push(['Items Imported', r.orderItemsImported || 0])
  rows.push(['Skipped', r.skipped || 0])
  rows.push(['Errors', r.errors?.length || 0])
  rows.push(['Ignored', r.ignoredRows?.length || 0])

  const csvContent = rows.map(row =>
    row.map(cell => {
      const str = String(cell)
      return str.includes(',') || str.includes('"') || str.includes('\n')
        ? '"' + str.replace(/"/g, '""') + '"'
        : str
    }).join(',')
  ).join('\n')

  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `import-report-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

function resetImport () {
  importForm.value = { storeId: null, itemsFile: null, ordersFile: null }
  importResult.value = null
}

onMounted(() => {
  loadStores()
  loadOrders()
})
</script>

<style scoped>
.erp-table {
  background: var(--erp-bg-tertiary);
  border-color: var(--erp-border-subtle);
}
.erp-table :deep(th) {
  color: #b0bec5;
  font-weight: 600;
  background: var(--erp-bg);
}
.erp-table :deep(td) {
  color: #e0e0e0;
  border-color: var(--erp-border-subtle);
}
.erp-table :deep(tr:hover td) {
  background: rgba(0, 188, 212, 0.06);
  cursor: pointer;
}
.import-detail-table {
  background: var(--erp-bg);
  border-color: var(--erp-border-subtle);
}
.import-detail-table :deep(th) {
  color: #90a4ae;
  font-weight: 600;
  background: var(--erp-bg);
  font-size: 12px;
}
.import-detail-table :deep(td) {
  color: #cfd8dc;
  border-color: var(--erp-border-subtle);
  font-size: 12px;
}
</style>
