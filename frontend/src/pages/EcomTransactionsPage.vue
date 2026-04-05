<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/ecommerce" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">Etsy Transactions</div>
        <div class="text-caption text-grey-5">Upload statements and match platform fees to orders</div>
      </div>
      <q-space />
      <q-btn unelevated color="teal-7" icon="sync" label="Match Fees to Orders" no-caps class="q-mr-sm" @click="handleMatch" :loading="matching" />
      <q-btn unelevated color="cyan-7" icon="file_upload" label="Upload Statement" no-caps @click="showUpload = true" />
    </div>

    <!-- Store selector + Filters -->
    <div class="row q-gutter-sm q-mb-md items-center">
      <q-select
        v-model="storeId"
        :options="storeOptions"
        label="Store"
        dense outlined dark
        emit-value map-options
        style="min-width: 180px"
        @update:model-value="loadTransactions"
      />
      <q-select
        v-model="filters.type"
        :options="typeOptions"
        label="Type"
        dense outlined dark
        emit-value map-options
        clearable
        style="min-width: 140px"
      />
      <q-select
        v-model="filters.matched"
        :options="matchedOptions"
        label="Matched"
        dense outlined dark
        emit-value map-options
        clearable
        style="min-width: 130px"
      />
      <q-input
        v-model="filters.search"
        label="Search (Order ID, Title...)"
        dense outlined dark
        clearable
        debounce="300"
        style="min-width: 220px"
      >
        <template v-slot:prepend>
          <q-icon name="search" color="grey-5" />
        </template>
      </q-input>
      <q-space />
      <div class="text-caption text-grey-5">{{ filteredTransactions.length }} of {{ transactions.length }} rows</div>
    </div>

    <!-- Transactions table -->
    <q-table
      :rows="filteredTransactions"
      :columns="columns"
      row-key="id"
      flat bordered
      :loading="loading"
      class="erp-table"
      :pagination="{ rowsPerPage: 50 }"
      dense
    >
      <template v-slot:body-cell-type="props">
        <q-td :props="props">
          <q-badge :color="typeColor(props.row.type)" :label="props.row.type" />
        </q-td>
      </template>
      <template v-slot:body-cell-feesAndTaxes="props">
        <q-td :props="props" class="text-right">
          <span :class="props.row.feesAndTaxes && props.row.feesAndTaxes < 0 ? 'text-red-4' : ''">
            {{ props.row.feesAndTaxes != null ? Number(props.row.feesAndTaxes).toFixed(2) : '-' }}
          </span>
        </q-td>
      </template>
      <template v-slot:body-cell-amount="props">
        <q-td :props="props" class="text-right">
          <span :class="props.row.amount && props.row.amount > 0 ? 'text-green-4' : ''">
            {{ props.row.amount != null ? Number(props.row.amount).toFixed(2) : '-' }}
          </span>
        </q-td>
      </template>
      <template v-slot:body-cell-net="props">
        <q-td :props="props" class="text-right text-weight-medium">
          {{ props.row.net != null ? Number(props.row.net).toFixed(2) : '-' }}
        </q-td>
      </template>
      <template v-slot:body-cell-matched="props">
        <q-td :props="props">
          <q-icon v-if="props.row.matched" name="check_circle" color="green-4" size="sm" />
          <q-icon v-else-if="props.row.orderIdRef" name="radio_button_unchecked" color="orange-4" size="sm" />
          <span v-else class="text-grey-7">-</span>
        </q-td>
      </template>
      <template v-slot:body-cell-orderIdRef="props">
        <q-td :props="props">
          <router-link v-if="props.row.orderIdRef" :to="orderLink(props.row.orderIdRef)" class="text-cyan-4" @click.stop>
            {{ props.row.orderIdRef }}
          </router-link>
          <span v-else class="text-grey-7">-</span>
        </q-td>
      </template>
    </q-table>

    <!-- Upload Dialog -->
    <q-dialog v-model="showUpload">
      <q-card style="min-width: 450px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Upload Etsy Statement</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-select
            v-model="uploadStoreId"
            :options="storeOptions"
            label="Store *"
            dense outlined dark
            emit-value map-options
          />
          <q-file
            v-model="uploadFile"
            label="Etsy Statement CSV *"
            dense outlined dark
            accept=".csv"
          >
            <template v-slot:prepend>
              <q-icon name="attach_file" color="grey-5" />
            </template>
          </q-file>
          <div class="text-caption text-grey-5">You can re-upload the same or growing monthly file — duplicates are automatically skipped.</div>
          <div v-if="uploadResult" class="q-pa-md" style="background: var(--erp-bg-tertiary); border-radius: 8px;">
            <div class="text-green-4">Inserted: {{ uploadResult.inserted }}</div>
            <div class="text-orange-4">Skipped (duplicates): {{ uploadResult.skipped }}</div>
            <div v-if="uploadResult.errors" class="text-red-4">Errors: {{ uploadResult.errors }}</div>
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Close" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated label="Upload" color="cyan-7" no-caps @click="handleUpload" :loading="uploading" :disable="!uploadStoreId || !uploadFile" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Match Result Dialog -->
    <q-dialog v-model="showMatchResult">
      <q-card style="min-width: 400px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Fee Matching Result</div>
        </q-card-section>
        <q-card-section v-if="matchResult">
          <div class="text-green-4 q-mb-xs">Matched: {{ matchResult.matched }} orders</div>
          <div class="text-orange-4 q-mb-xs">Skipped (no order found): {{ matchResult.skippedNoOrder }}</div>
          <div class="text-cyan-4 q-mb-xs">Skipped (already has fee): {{ matchResult.skippedAlreadyHasFee }}</div>
          <div class="text-grey-4 q-mb-xs">Total unmatched order IDs: {{ matchResult.total }}</div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Close" color="grey-5" v-close-popup no-caps />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { ecomStoreApi, ecomTransactionApi } from 'src/api/ecommerce'

const $q = useQuasar()

const loading = ref(false)
const transactions = ref([])
const storeId = ref(null)
const storeOptions = ref([])
const orderIdMap = ref({}) // for linking

const filters = ref({ type: null, matched: null, search: '' })

const typeOptions = [
  { label: 'Fee', value: 'Fee' },
  { label: 'Tax', value: 'Tax' },
  { label: 'Sale', value: 'Sale' },
  { label: 'Refund', value: 'Refund' },
  { label: 'Marketing', value: 'Marketing' },
  { label: 'Buyer Fee', value: 'Buyer Fee' },
  { label: 'Deposit', value: 'Deposit' }
]

const matchedOptions = [
  { label: 'Matched', value: 'yes' },
  { label: 'Unmatched', value: 'no' }
]

const columns = [
  { name: 'txnDate', label: 'Date', field: 'txnDate', align: 'left', sortable: true, format: v => v || '' },
  { name: 'type', label: 'Type', field: 'type', align: 'center', sortable: true },
  { name: 'title', label: 'Title', field: 'title', align: 'left', style: 'max-width: 280px', classes: 'ellipsis' },
  { name: 'orderIdRef', label: 'Order ID', field: 'orderIdRef', align: 'left', sortable: true },
  { name: 'amount', label: 'Amount', field: 'amount', align: 'right', sortable: true },
  { name: 'feesAndTaxes', label: 'Fees & Taxes', field: 'feesAndTaxes', align: 'right', sortable: true },
  { name: 'net', label: 'Net', field: 'net', align: 'right', sortable: true },
  { name: 'currency', label: 'Cur', field: 'currency', align: 'center' },
  { name: 'matched', label: 'Matched', field: 'matched', align: 'center', sortable: true }
]

const filteredTransactions = computed(() => {
  return transactions.value.filter(t => {
    if (filters.value.type && t.type !== filters.value.type) return false
    if (filters.value.matched === 'yes' && !t.matched) return false
    if (filters.value.matched === 'no' && t.matched) return false
    if (filters.value.search) {
      const q = filters.value.search.toLowerCase()
      if (!(t.title || '').toLowerCase().includes(q) &&
          !(t.info || '').toLowerCase().includes(q) &&
          !(t.orderIdRef || '').includes(q)) return false
    }
    return true
  })
})

// Upload
const showUpload = ref(false)
const uploading = ref(false)
const uploadFile = ref(null)
const uploadStoreId = ref(null)
const uploadResult = ref(null)

// Match
const matching = ref(false)
const showMatchResult = ref(false)
const matchResult = ref(null)

function typeColor (type) {
  const map = { Fee: 'red-7', Tax: 'orange-7', Sale: 'green-7', Refund: 'purple-7', Marketing: 'blue-7', 'Buyer Fee': 'amber-8', Deposit: 'teal-7' }
  return map[type] || 'grey-7'
}

function orderLink (orderIdRef) {
  // We'd need to look up the order by platformOrderId, but for simplicity link to orders page with search
  return `/ecommerce/orders?search=${orderIdRef}`
}

async function loadStores () {
  try {
    const res = await ecomStoreApi.getAll()
    const stores = res.data.data || []
    storeOptions.value = stores.map(s => ({ label: s.name, value: s.id }))
    if (stores.length && !storeId.value) {
      storeId.value = stores[0].id
      uploadStoreId.value = stores[0].id
      loadTransactions()
    }
  } catch { /* ignore */ }
}

async function loadTransactions () {
  if (!storeId.value) return
  loading.value = true
  try {
    const res = await ecomTransactionApi.getAll(storeId.value)
    transactions.value = res.data.data || []
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load transactions' })
  } finally {
    loading.value = false
  }
}

async function handleUpload () {
  if (!uploadStoreId.value || !uploadFile.value) return
  uploading.value = true
  uploadResult.value = null
  try {
    const res = await ecomTransactionApi.upload(uploadStoreId.value, uploadFile.value)
    uploadResult.value = res.data.data
    $q.notify({ type: 'positive', message: `Uploaded: ${uploadResult.value.inserted} new, ${uploadResult.value.skipped} skipped` })
    if (storeId.value === uploadStoreId.value) loadTransactions()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Upload failed' })
  } finally {
    uploading.value = false
  }
}

async function handleMatch () {
  if (!storeId.value) {
    $q.notify({ type: 'warning', message: 'Select a store first' })
    return
  }
  matching.value = true
  try {
    const res = await ecomTransactionApi.matchFees(storeId.value)
    matchResult.value = res.data.data
    showMatchResult.value = true
    loadTransactions()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Matching failed' })
  } finally {
    matching.value = false
  }
}

onMounted(loadStores)
</script>

<style scoped>
.erp-table { background: var(--erp-bg-tertiary); border-color: var(--erp-border-subtle); }
.erp-table :deep(th) { color: #b0bec5; font-weight: 600; background: var(--erp-bg); }
.erp-table :deep(td) { color: #e0e0e0; border-color: var(--erp-border-subtle); }
</style>
