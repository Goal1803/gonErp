<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/ecommerce" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">Orders</div>
        <div class="text-caption text-grey-5">All orders across stores</div>
      </div>
      <q-space />
      <q-btn
        v-if="selectedOrders.length"
        unelevated
        color="teal-7"
        icon="sync"
        :label="`Sync ${selectedOrders.length} to Board`"
        no-caps
        @click="showSyncDialog = true"
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
      <q-select
        v-model="filters.syncStatus"
        :options="syncStatusOptions"
        label="Sync"
        dense outlined dark
        emit-value map-options
        clearable
        style="min-width: 130px"
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
      :rows="filteredOrders"
      :columns="columns"
      row-key="id"
      flat
      bordered
      :loading="loading"
      class="erp-table"
      v-model:pagination="pagination"
      selection="multiple"
      v-model:selected="selectedOrders"
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
      <template v-slot:body-cell-synced="props">
        <q-td :props="props">
          <q-icon v-if="props.row.cardId" name="link" color="teal-4" size="sm">
            <q-tooltip>Synced to card #{{ props.row.cardId }}</q-tooltip>
          </q-icon>
          <q-icon v-else name="link_off" color="grey-7" size="sm">
            <q-tooltip>Not synced</q-tooltip>
          </q-icon>
        </q-td>
      </template>
      <template v-slot:body-cell-status="props">
        <q-td :props="props">
          <q-badge :color="statusColor(props.row.status)" :label="formatStatus(props.row.status)" />
        </q-td>
      </template>
      <template v-slot:body-cell-orderTotal="props">
        <q-td :props="props" class="text-right">
          {{ formatCurrency(props.row.orderTotal) }}
        </q-td>
      </template>
      <template v-slot:body-cell-earningAfterPlatformFee="props">
        <q-td :props="props" class="text-right">
          <span :class="props.row.earningAfterPlatformFee != null ? 'text-cyan-4' : 'text-grey-7'">
            {{ props.row.earningAfterPlatformFee != null ? formatCurrency(props.row.earningAfterPlatformFee) : '-' }}
          </span>
        </q-td>
      </template>
      <template v-slot:body-cell-refundAmount="props">
        <q-td :props="props" class="text-right">
          <span v-if="props.row.refundAmount" class="text-red-4">-{{ Number(props.row.refundAmount).toFixed(2) }}</span>
          <span v-else class="text-grey-7">-</span>
        </q-td>
      </template>
      <template v-slot:body-cell-fulfillmentCost="props">
        <q-td :props="props" class="text-right">
          <span :class="props.row.fulfillmentCost != null ? 'text-orange-4' : 'text-grey-7'">
            {{ props.row.fulfillmentCost != null ? Number(props.row.fulfillmentCost).toFixed(2) : '-' }}
          </span>
        </q-td>
      </template>
      <template v-slot:body-cell-grossProfit="props">
        <q-td :props="props" class="text-right text-weight-medium">
          <span v-if="props.row.grossProfit != null" :class="props.row.grossProfit >= 0 ? 'text-green-4' : 'text-red-4'">
            {{ formatCurrency(props.row.grossProfit) }}
          </span>
          <span v-else class="text-grey-7">-</span>
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

    <!-- Sync to Board Dialog -->
    <q-dialog v-model="showSyncDialog">
      <q-card style="min-width: 420px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Sync Orders to Board</div>
          <div class="text-caption text-grey-5">{{ syncableOrders.length }} of {{ selectedOrders.length }} selected orders can be synced (have items, not already synced)</div>
        </q-card-section>
        <q-card-section>
          <q-select
            v-model="syncBoardId"
            :options="podOrderBoardOptions"
            label="POD Order Board *"
            dense outlined dark
            emit-value map-options
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated label="Sync" color="teal-7" no-caps @click="handleSync" :loading="syncing" :disable="!syncBoardId || !syncableOrders.length" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { ecomOrderApi, ecomStoreApi } from 'src/api/ecommerce'
import { boardApi } from 'src/api/tasks'

const router = useRouter()
const $q = useQuasar()

const loading = ref(false)
const orders = ref([])
const stores = ref([])
const storeOptions = ref([])

const selectedOrders = ref([])

const filters = ref({
  storeId: null,
  status: [],
  dateFrom: '',
  dateTo: '',
  search: '',
  syncStatus: null
})

const syncStatusOptions = [
  { label: 'Synced', value: 'synced' },
  { label: 'Not Synced', value: 'not_synced' }
]

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

const filteredOrders = computed(() => {
  let result = orders.value
  if (filters.value.syncStatus === 'synced') result = result.filter(o => o.cardId)
  else if (filters.value.syncStatus === 'not_synced') result = result.filter(o => !o.cardId)
  if (filters.value.search) {
    const q = filters.value.search.toLowerCase()
    result = result.filter(o =>
      (o.platformOrderId || '').toLowerCase().includes(q) ||
      (o.customerName || '').toLowerCase().includes(q) ||
      (o.sku || '').toLowerCase().includes(q) ||
      (o.supplierName || '').toLowerCase().includes(q) ||
      (o.supplierTransactionId || '').toLowerCase().includes(q) ||
      (o.items || []).some(i => (i.productName || '').toLowerCase().includes(q) || (i.sku || '').toLowerCase().includes(q))
    )
  }
  return result
})

const columns = [
  { name: 'synced', label: '', field: 'cardId', align: 'center', style: 'width: 40px' },
  { name: 'orderDate', label: 'Order Date', field: 'orderDate', align: 'left', sortable: true, format: v => v ? new Date(v).toLocaleDateString() : '' },
  { name: 'platformOrderId', label: 'Order ID', field: 'platformOrderId', align: 'left', sortable: true },
  { name: 'productName', label: 'Product', field: row => row.items?.[0]?.productName || row.sku || '', align: 'left' },
  { name: 'sku', label: 'SKU', field: row => row.items?.[0]?.sku || row.sku || '', align: 'left', sortable: true },
  { name: 'numberOfItems', label: 'Items', field: 'numberOfItems', align: 'center', sortable: true },
  { name: 'customerName', label: 'Customer', field: 'customerName', align: 'left', sortable: true },
  { name: 'orderTotal', label: 'Revenue', field: 'orderTotal', align: 'right', sortable: true },
  { name: 'earningAfterPlatformFee', label: 'Earning', field: 'earningAfterPlatformFee', align: 'right', sortable: true },
  { name: 'refundAmount', label: 'Refund', field: 'refundAmount', align: 'right', sortable: true },
  { name: 'fulfillmentCost', label: 'Fulfillment', field: 'fulfillmentCost', align: 'right', sortable: true },
  { name: 'grossProfit', label: 'Profit', field: 'grossProfit', align: 'right', sortable: true },
  { name: 'status', label: 'Status', field: 'status', align: 'center', sortable: true },
  { name: 'actions', label: 'Actions', field: 'actions', align: 'center' }
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

// Sync to board
const podOrderBoardOptions = ref([])
const showSyncDialog = ref(false)
const syncBoardId = ref(null)
const syncing = ref(false)

const syncableOrders = computed(() =>
  selectedOrders.value.filter(o => !o.cardId && o.items && o.items.length && o.items.some(i => i.productName || i.platformItemId))
)

async function handleSync () {
  if (!syncBoardId.value || !syncableOrders.value.length) return
  syncing.value = true
  try {
    const ids = syncableOrders.value.map(o => o.id)
    const res = await ecomOrderApi.syncToBoard(syncBoardId.value, ids)
    const r = res.data.data
    showSyncDialog.value = false
    selectedOrders.value = []
    $q.notify({ type: 'positive', message: `Synced ${r.synced} orders, ${r.skipped} skipped` })
    loadOrders()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Sync failed' })
  } finally {
    syncing.value = false
  }
}

async function loadPodOrderBoards () {
  try {
    const res = await boardApi.getAll()
    podOrderBoardOptions.value = (res.data.data || [])
      .filter(b => b.boardType === 'POD_ORDER')
      .map(b => ({ label: b.name, value: b.id }))
  } catch { /* ignore */ }
}

onMounted(() => {
  loadStores()
  loadOrders()
  loadPodOrderBoards()
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
</style>
