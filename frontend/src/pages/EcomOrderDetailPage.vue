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
        <div class="text-overline text-grey-5 q-mb-sm">Order Items ({{ order.items?.length || 0 }})</div>
        <template v-if="order.items && order.items.length">
          <q-card v-for="(item, idx) in order.items" :key="item.id || idx" flat class="item-card q-mb-sm">
            <q-card-section class="q-pa-md">
              <div class="row items-start q-gutter-md">
                <!-- Item number badge -->
                <q-avatar size="32px" color="cyan-9" text-color="white" font-size="14px" class="q-mt-xs">
                  {{ idx + 1 }}
                </q-avatar>
                <div class="col">
                  <!-- Product name -->
                  <div class="text-white text-weight-medium" style="font-size: 14px; line-height: 1.4;">
                    {{ item.productName || 'Unnamed item' }}
                  </div>
                  <!-- SKU + Listing + Qty row -->
                  <div class="row items-center q-gutter-sm q-mt-xs">
                    <div class="variation-item-inline">
                      <span class="variation-label">SKU</span>
                      <span class="variation-value">{{ item.sku || '-' }}</span>
                    </div>
                    <div class="variation-item-inline">
                      <span class="variation-label">Quantity</span>
                      <span class="variation-value">{{ item.quantity }}</span>
                    </div>
                  </div>
                  <!-- Variations -->
                  <div v-if="item.variations" class="q-mt-sm">
                    <div class="variation-list">
                      <template v-for="(v, vi) in parseVariations(item.variations)" :key="vi">
                        <!-- Inline chip for short single-line values -->
                        <div v-if="!v.multiline" class="variation-item-inline">
                          <span class="variation-label">{{ v.key }}</span>
                          <span class="variation-value">{{ v.value }}</span>
                        </div>
                        <!-- Block display for multi-line values (e.g., Personalization) -->
                        <div v-else class="variation-item-block">
                          <div class="variation-block-label">{{ v.key }}</div>
                          <pre class="variation-block-value">{{ v.value }}</pre>
                        </div>
                      </template>
                    </div>
                  </div>
                </div>
                <!-- Price -->
                <div class="text-right q-mt-xs" style="min-width: 80px;">
                  <div class="text-white text-weight-medium" style="font-size: 15px;">{{ formatCurrency(item.itemTotal) }}</div>
                  <div v-if="item.quantity > 1" class="text-grey-5" style="font-size: 11px;">{{ formatCurrency(item.itemPrice) }} ea</div>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </template>
        <q-card v-else flat class="detail-card q-mb-md">
          <q-card-section>
            <div class="text-grey-5">No items imported yet. Import the Etsy Sold Order Items CSV to add items.</div>
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

function decodeHtmlEntities (str) {
  if (!str) return str
  return str
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&#39;/g, "'")
    .replace(/&apos;/g, "'")
}

function parseVariations (variationsStr) {
  if (!variationsStr) return []
  const decoded = decodeHtmlEntities(variationsStr)
  // Split on comma followed by a key pattern (Word:) — but only when the comma
  // is NOT inside multi-line content (i.e., only split on commas before a new key)
  const parts = decoded.split(/,(?=\s*[A-Za-z][A-Za-z0-9 _-]*:)/)
  return parts.map(part => {
    const colonIdx = part.indexOf(':')
    if (colonIdx > 0) {
      const key = part.substring(0, colonIdx).trim()
      const value = part.substring(colonIdx + 1)
      // Preserve leading/trailing whitespace per line but trim the whole block
      const trimmedValue = value.replace(/^\s*\n/, '').replace(/\n\s*$/, '')
      return {
        key,
        value: trimmedValue || value.trim(),
        multiline: value.includes('\n')
      }
    }
    return { key: '', value: part.trim(), multiline: part.includes('\n') }
  }).filter(v => v.value)
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
.item-card {
  background: var(--erp-bg-tertiary);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 10px;
  transition: border-color 0.15s;
}
.item-card:hover {
  border-color: rgba(0, 188, 212, 0.3);
}
.variation-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.variation-item-inline {
  display: inline-flex;
  align-items: baseline;
  background: rgba(0, 188, 212, 0.08);
  border: 1px solid rgba(0, 188, 212, 0.18);
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 13px;
  line-height: 1.4;
  align-self: flex-start;
}
.variation-item-inline .variation-label {
  color: #80cbc4;
  font-weight: 600;
  margin-right: 6px;
  white-space: nowrap;
}
.variation-item-inline .variation-label::after {
  content: ':';
}
.variation-item-inline .variation-value {
  color: #e0e0e0;
}
.variation-item-block {
  background: rgba(0, 188, 212, 0.06);
  border: 1px solid rgba(0, 188, 212, 0.15);
  border-radius: 8px;
  padding: 8px 12px;
}
.variation-block-label {
  color: #80cbc4;
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}
.variation-block-value {
  color: #e0e0e0;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}
</style>
