<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/ecommerce/orders" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">
          Order #{{ order.platformOrderId || '...' }}
        </div>
        <div class="text-caption text-grey-5">{{ order.storeName || '' }} &middot; {{ order.salesChannel || '' }}</div>
      </div>
      <q-space />
      <q-badge
        :color="statusColor(order.status)"
        :label="formatStatus(order.status)"
        class="text-body2 q-pa-sm"
        style="font-size: 14px; cursor: pointer"
      >
        <q-menu dark>
          <q-list dense style="min-width: 200px; background: var(--erp-bg-elevated);">
            <q-item
              v-for="opt in statusOptions"
              :key="opt.value"
              clickable
              v-close-popup
              @click="updateField('status', opt.value)"
            >
              <q-item-section>
                <q-badge :color="statusColor(opt.value)" :label="opt.label" />
              </q-item-section>
            </q-item>
          </q-list>
        </q-menu>
      </q-badge>
    </div>

    <div v-if="loading" class="text-center q-pa-xl">
      <q-spinner color="cyan-5" size="48px" />
    </div>

    <div v-else class="row q-gutter-lg">
      <!-- Left column -->
      <div class="col-12 col-md-7">
        <!-- Order Items -->
        <q-card flat class="detail-card q-mb-md">
          <q-card-section>
            <div class="text-overline text-grey-5 q-mb-sm">Order Items ({{ order.items?.length || 0 }})</div>
            <q-table
              v-if="order.items && order.items.length"
              :rows="order.items"
              :columns="itemColumns"
              row-key="id"
              flat bordered dense
              class="items-table"
              :pagination="{ rowsPerPage: 0 }"
              hide-pagination
            >
              <template v-slot:body-cell-productName="props">
                <q-td :props="props">
                  <div class="ellipsis" style="max-width: 250px">
                    {{ props.row.productName }}
                    <q-tooltip v-if="props.row.productName && props.row.productName.length > 35">{{ props.row.productName }}</q-tooltip>
                  </div>
                </q-td>
              </template>
              <template v-slot:body-cell-variations="props">
                <q-td :props="props">
                  <div class="ellipsis" style="max-width: 180px">
                    {{ props.row.variations || '-' }}
                    <q-tooltip v-if="props.row.variations">{{ props.row.variations }}</q-tooltip>
                  </div>
                </q-td>
              </template>
            </q-table>
            <div v-else class="text-grey-5">No items imported yet. Import the Etsy Sold Order Items CSV to add items.</div>
          </q-card-section>
        </q-card>

        <!-- Financials -->
        <q-card flat class="detail-card q-mb-md">
          <q-card-section>
            <div class="text-overline text-grey-5 q-mb-sm">Financials</div>
            <div class="detail-grid">
              <div class="detail-label">Order Value</div>
              <div class="detail-value">{{ formatCurrency(order.orderValue) }}</div>
              <div class="detail-label">Shipping</div>
              <div class="detail-value">{{ formatCurrency(order.shippingPrice) }}</div>
              <div class="detail-label">Tax</div>
              <div class="detail-value">{{ formatCurrency(order.tax) }}</div>
              <div class="detail-label">Discount</div>
              <div class="detail-value">{{ formatCurrency(order.discount) }}</div>
              <div class="detail-label">Processing Fees</div>
              <div class="detail-value">{{ formatCurrency(order.processingFees) }}</div>
              <div class="detail-label text-weight-medium">Order Total</div>
              <div class="detail-value text-weight-medium">{{ formatCurrency(order.orderTotal) }}</div>
              <div class="detail-label text-weight-medium">Order Net</div>
              <div class="detail-value text-weight-medium">{{ formatCurrency(order.orderNet) }}</div>
            </div>
            <q-separator dark class="q-my-md" />
            <div class="detail-grid">
              <div class="detail-label">Fulfillment Cost</div>
              <div class="detail-value">
                <q-input
                  v-model.number="editForm.fulfillmentCost"
                  type="number"
                  dense outlined dark
                  step="0.01"
                  style="max-width: 140px"
                  @blur="updateField('fulfillmentCost', editForm.fulfillmentCost)"
                />
              </div>
              <div class="detail-label">Other Cost</div>
              <div class="detail-value">
                <q-input
                  v-model.number="editForm.otherCost"
                  type="number"
                  dense outlined dark
                  step="0.01"
                  style="max-width: 140px"
                  @blur="updateField('otherCost', editForm.otherCost)"
                />
              </div>
              <div class="detail-label text-weight-bold text-body1">Gross Profit</div>
              <div class="detail-value text-weight-bold text-body1" :class="grossProfit >= 0 ? 'text-green-4' : 'text-red-4'">
                {{ formatCurrency(grossProfit) }}
              </div>
            </div>
          </q-card-section>
        </q-card>

        <!-- Notes -->
        <q-card flat class="detail-card q-mb-md">
          <q-card-section>
            <div class="text-overline text-grey-5 q-mb-sm">Notes</div>
            <q-input
              v-model="editForm.notes"
              type="textarea"
              rows="3"
              dense outlined dark
              placeholder="Add notes..."
              @blur="updateField('notes', editForm.notes)"
            />
          </q-card-section>
        </q-card>
      </div>

      <!-- Right column -->
      <div class="col-12 col-md">
        <!-- Customer & Shipping -->
        <q-card flat class="detail-card q-mb-md">
          <q-card-section>
            <div class="text-overline text-grey-5 q-mb-sm">Customer & Shipping</div>
            <div class="detail-grid">
              <div class="detail-label">Customer</div>
              <div class="detail-value">{{ order.customerName || '-' }}</div>
              <div class="detail-label">Buyer ID</div>
              <div class="detail-value">{{ order.buyerUserId || '-' }}</div>
              <div class="detail-label">Email</div>
              <div class="detail-value">{{ order.customerEmail || '-' }}</div>
              <div class="detail-label">Phone</div>
              <div class="detail-value">{{ order.customerPhone || '-' }}</div>
              <div class="detail-label">Ship To</div>
              <div class="detail-value" style="white-space: pre-wrap">{{ formattedAddress }}</div>
              <div class="detail-label">Country</div>
              <div class="detail-value">{{ countryFlag(order.shipCountry) }} {{ order.shipCountry || '-' }}</div>
            </div>
          </q-card-section>
        </q-card>

        <!-- Fulfillment -->
        <q-card flat class="detail-card q-mb-md">
          <q-card-section>
            <div class="text-overline text-grey-5 q-mb-sm">Fulfillment</div>
            <div class="detail-grid">
              <div class="detail-label">Tracking Number</div>
              <div class="detail-value">
                <q-input
                  v-model="editForm.trackingNumber"
                  dense outlined dark
                  style="max-width: 220px"
                  @blur="updateField('trackingNumber', editForm.trackingNumber)"
                />
              </div>
              <div class="detail-label">Tracking Status</div>
              <div class="detail-value">
                <q-select
                  v-model="editForm.trackingStatus"
                  :options="trackingStatusOptions"
                  dense outlined dark
                  emit-value map-options
                  clearable
                  style="max-width: 220px"
                  @update:model-value="updateField('trackingStatus', editForm.trackingStatus)"
                />
              </div>
              <div class="detail-label">Shipped Date</div>
              <div class="detail-value">{{ order.shippedDate ? new Date(order.shippedDate).toLocaleDateString() : '-' }}</div>
              <div class="detail-label">Refunded</div>
              <div class="detail-value">
                <q-toggle
                  v-model="editForm.refunded"
                  color="red-5"
                  @update:model-value="updateField('refunded', editForm.refunded)"
                />
              </div>
            </div>
          </q-card-section>
        </q-card>

        <!-- Platform Data -->
        <q-card flat class="detail-card q-mb-md">
          <q-card-section>
            <div class="text-overline text-grey-5 q-mb-sm">Platform Data</div>
            <div class="detail-grid">
              <div class="detail-label">Sales Channel</div>
              <div class="detail-value">
                <q-badge :color="channelColor(order.salesChannel)" :label="order.salesChannel || '-'" />
              </div>
              <div class="detail-label">Store</div>
              <div class="detail-value">{{ order.storeName || '-' }}</div>
              <div class="detail-label">Platform Order ID</div>
              <div class="detail-value">{{ order.platformOrderId || '-' }}</div>
              <div class="detail-label">Order Date</div>
              <div class="detail-value">{{ order.orderDate ? new Date(order.orderDate).toLocaleDateString() : '-' }}</div>
              <div class="detail-label">Items Count</div>
              <div class="detail-value">{{ order.numberOfItems || order.items?.length || '-' }}</div>
              <div class="detail-label">SKU</div>
              <div class="detail-value">{{ order.sku || '-' }}</div>
            </div>
            <template v-if="order.platformData && Object.keys(order.platformData).length">
              <q-separator dark class="q-my-sm" />
              <div class="detail-grid">
                <template v-for="(val, key) in order.platformData" :key="key">
                  <div class="detail-label">{{ key }}</div>
                  <div class="detail-value">{{ val || '-' }}</div>
                </template>
              </div>
            </template>
          </q-card-section>
        </q-card>

        <!-- Raw Data -->
        <q-card flat class="detail-card q-mb-md">
          <q-card-section>
            <q-expansion-item
              label="Raw Data"
              header-class="text-overline text-grey-5"
              dense
              dark
            >
              <pre class="text-grey-4 q-pa-sm" style="font-size: 11px; overflow-x: auto; background: var(--erp-bg); border-radius: 6px; max-height: 400px;">{{ JSON.stringify(order.rawData, null, 2) }}</pre>
            </q-expansion-item>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { ecomOrderApi } from 'src/api/ecommerce'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()

const loading = ref(true)
const order = ref({})
const editForm = ref({
  fulfillmentCost: 0,
  otherCost: 0,
  notes: '',
  trackingNumber: '',
  trackingStatus: null,
  refunded: false
})

const statusOptions = [
  { label: 'Draft', value: 'DRAFT' },
  { label: 'New Order', value: 'NEW_ORDER' },
  { label: 'Clarify with Customer', value: 'CLARIFY_WITH_CUSTOMER' },
  { label: 'Confirmed', value: 'CONFIRMED' },
  { label: 'Designing', value: 'DESIGNING' },
  { label: 'Design Checking', value: 'DESIGN_CHECKING' },
  { label: 'Need to Fix', value: 'NEED_TO_FIX' },
  { label: 'Fix Checking', value: 'FIX_CHECKING' },
  { label: 'Fixing', value: 'FIXING' },
  { label: 'Confirming Design', value: 'CONFIRMING_DESIGN_WITH_CUSTOMER' },
  { label: 'Design Approved', value: 'DESIGN_APPROVED' },
  { label: 'Fulfilled', value: 'FULFILLED' },
  { label: 'Track Generated', value: 'TRACK_GENERATED' },
  { label: 'Track Added to Store', value: 'TRACK_ADDED_TO_STORE' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Cancelled', value: 'CANCELLED' }
]

const trackingStatusOptions = [
  { label: 'Pending', value: 'PENDING' },
  { label: 'In Transit', value: 'IN_TRANSIT' },
  { label: 'Delivered', value: 'DELIVERED' },
  { label: 'Returned', value: 'RETURNED' }
]

const itemColumns = [
  { name: 'productName', label: 'Product', field: 'productName', align: 'left' },
  { name: 'sku', label: 'SKU', field: 'sku', align: 'left' },
  { name: 'variations', label: 'Variations', field: 'variations', align: 'left' },
  { name: 'quantity', label: 'Qty', field: 'quantity', align: 'center' },
  { name: 'itemPrice', label: 'Price', field: 'itemPrice', align: 'right', format: v => v != null ? Number(v).toFixed(2) : '-' },
  { name: 'itemTotal', label: 'Total', field: 'itemTotal', align: 'right', format: v => v != null ? Number(v).toFixed(2) : '-' }
]

const grossProfit = computed(() => {
  const net = Number(order.value.orderNet) || 0
  const fc = Number(editForm.value.fulfillmentCost) || 0
  const oc = Number(editForm.value.otherCost) || 0
  return net - fc - oc
})

const formattedAddress = computed(() => {
  const o = order.value
  const parts = [o.shipStreet1, o.shipStreet2, o.shipCity, o.shipState, o.shipZipcode].filter(Boolean)
  return parts.join('\n') || '-'
})

function statusColor (status) {
  const map = {
    DRAFT: 'grey-7', NEW_ORDER: 'blue-7', CLARIFY_WITH_CUSTOMER: 'orange-7',
    CONFIRMED: 'green-7', DESIGNING: 'purple-7', DESIGN_CHECKING: 'purple-5',
    NEED_TO_FIX: 'red-7', FIX_CHECKING: 'red-5', FIXING: 'orange-7',
    CONFIRMING_DESIGN_WITH_CUSTOMER: 'amber-7', DESIGN_APPROVED: 'teal-7',
    FULFILLED: 'teal-5', TRACK_GENERATED: 'cyan-7', TRACK_ADDED_TO_STORE: 'cyan-5',
    COMPLETED: 'grey-7', CANCELLED: 'red-9'
  }
  return map[status] || 'orange-7'
}

function formatStatus (status) {
  if (!status) return ''
  return status.replace(/_/g, ' ')
}

function formatCurrency (val) {
  if (val == null) return '-'
  return Number(val).toFixed(2)
}

function channelColor (ch) {
  const map = { ETSY: 'orange-7', AMAZON: 'amber-8', SHOPIFY: 'green-7' }
  return map[ch] || 'grey-7'
}

function countryFlag (code) {
  if (!code || code.length !== 2) return ''
  const offset = 127397
  return String.fromCodePoint(...[...code.toUpperCase()].map(c => c.charCodeAt(0) + offset))
}

async function loadOrder () {
  loading.value = true
  try {
    const res = await ecomOrderApi.getById(route.params.id)
    order.value = res.data.data
    editForm.value = {
      fulfillmentCost: order.value.fulfillmentCost || 0,
      otherCost: order.value.otherCost || 0,
      notes: order.value.notes || '',
      trackingNumber: order.value.trackingNumber || '',
      trackingStatus: order.value.trackingStatus || null,
      refunded: order.value.refunded || false
    }
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to load order' })
    router.push('/ecommerce/orders')
  } finally {
    loading.value = false
  }
}

async function updateField (field, value) {
  try {
    const res = await ecomOrderApi.update(order.value.id, { [field]: value })
    order.value = res.data.data
    $q.notify({ type: 'positive', message: 'Updated', timeout: 1000 })
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to update' })
  }
}

onMounted(() => {
  loadOrder()
})
</script>

<style scoped>
.detail-card {
  background: var(--erp-bg-tertiary);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 10px;
}
.detail-grid {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 10px 16px;
  align-items: center;
}
.detail-label {
  color: #90a4ae;
  font-size: 13px;
}
.detail-value {
  color: #e0e0e0;
  font-size: 13px;
}
.items-table {
  background: var(--erp-bg-tertiary);
  border-color: var(--erp-border-subtle);
}
.items-table :deep(th) {
  color: #b0bec5;
  font-weight: 600;
  background: var(--erp-bg);
}
.items-table :deep(td) {
  color: #e0e0e0;
  border-color: var(--erp-border-subtle);
}
</style>
