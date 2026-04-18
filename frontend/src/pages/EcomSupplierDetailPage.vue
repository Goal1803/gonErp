<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/ecommerce/suppliers" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">{{ supplier?.name || 'Supplier' }}</div>
        <div class="text-caption text-grey-5">Supplier detail &amp; transactions</div>
      </div>
      <q-space />
    </div>

    <q-inner-loading :showing="loading" color="cyan-5" />

    <template v-if="supplier">
      <!-- Supplier Info -->
      <q-card flat bordered class="erp-card q-mb-md">
        <q-card-section>
          <div class="row q-col-gutter-md">
            <div class="col-6 col-md-3">
              <div class="text-caption text-grey-5">Name</div>
              <div class="text-white">{{ supplier.name }}</div>
            </div>
            <div class="col-6 col-md-3">
              <div class="text-caption text-grey-5">Countries</div>
              <div><q-badge v-for="c in (supplier.countries || [])" :key="c" color="blue-grey-8" class="q-mr-xs" dense>{{ c }}</q-badge></div>
            </div>
            <div class="col-6 col-md-3">
              <div class="text-caption text-grey-5">Website</div>
              <a v-if="supplier.website" :href="supplier.website" target="_blank" class="text-cyan-4">{{ supplier.website }}</a>
              <span v-else class="text-grey-6">-</span>
            </div>
            <div class="col-6 col-md-3">
              <div class="text-caption text-grey-5">Price List</div>
              <a v-if="supplier.priceListUrl" :href="supplier.priceListUrl" target="_blank" class="text-cyan-4">View</a>
              <span v-else class="text-grey-6">-</span>
            </div>
          </div>
          <div v-if="supplier.description" class="text-grey-4 q-mt-sm" style="font-size: 13px;">{{ supplier.description }}</div>
        </q-card-section>
      </q-card>

      <!-- Transactions Section -->
      <q-card flat bordered class="erp-card">
        <q-card-section>
          <div class="row items-center q-mb-md">
            <div class="text-h6 text-white">Fulfillment Orders</div>
            <q-space />
            <q-btn unelevated color="teal-7" icon="sync" label="Match to Orders" no-caps class="q-mr-sm" @click="openMatchDialog" :loading="matching" />
            <q-btn unelevated color="cyan-7" icon="file_upload" label="Upload File" no-caps @click="showUpload = true" />
          </div>

          <!-- Filters -->
          <div class="row q-gutter-sm q-mb-sm items-center">
            <q-select v-model="filters.status" :options="statusOptions" label="Status" dense outlined dark emit-value map-options clearable style="min-width: 130px" />
            <q-select v-model="filters.matched" :options="matchedOptions" label="Matched" dense outlined dark emit-value map-options clearable style="min-width: 120px" />
            <q-input v-model="filters.search" label="Search" dense outlined dark clearable debounce="300" style="min-width: 180px">
              <template v-slot:prepend><q-icon name="search" color="grey-5" /></template>
            </q-input>
            <q-space />
            <div class="text-caption text-grey-5">{{ filteredTxns.length }} / {{ transactions.length }}</div>
          </div>

          <q-table
            :rows="filteredTxns"
            :columns="columns"
            row-key="id"
            flat bordered dense
            :loading="loadingTxns"
            class="erp-table"
            :pagination="{ rowsPerPage: 30 }"
          >
            <template v-slot:body-cell-status="props">
              <q-td :props="props">
                <q-badge :color="statusColor(props.row.status)" :label="props.row.status" />
              </q-td>
            </template>
            <template v-slot:body-cell-amount="props">
              <q-td :props="props" class="text-right">
                {{ props.row.amount != null ? '$' + Number(props.row.amount).toFixed(2) : '-' }}
              </q-td>
            </template>
            <template v-slot:body-cell-matched="props">
              <q-td :props="props">
                <router-link v-if="props.row.matchedOrderId" :to="`/ecommerce/orders/${props.row.matchedOrderId}`" class="text-cyan-4" @click.stop>
                  {{ props.row.matchedPlatformOrderId }}
                </router-link>
                <q-icon v-else-if="props.row.status === 'Cancelled' || props.row.status === 'Not paid yet'" name="block" color="grey-7" size="sm" />
                <q-btn v-else flat dense no-caps size="sm" color="orange-4" icon="search" label="Match…" @click.stop="openManualMatchDialog(props.row)" />
              </q-td>
            </template>
            <template v-slot:body-cell-externalNumber="props">
              <q-td :props="props">
                <span v-if="props.row.externalNumber" class="text-cyan-4">{{ props.row.externalNumber }}</span>
                <span v-else class="text-grey-7">-</span>
              </q-td>
            </template>
          </q-table>
        </q-card-section>
      </q-card>
    </template>

    <!-- Upload Dialog -->
    <q-dialog v-model="showUpload">
      <q-card style="min-width: 450px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Upload Supplier File</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-file v-model="uploadFile" label="Supplier XLS/CSV *" dense outlined dark accept=".xls,.csv,.xlsx">
            <template v-slot:prepend><q-icon name="attach_file" color="grey-5" /></template>
          </q-file>
          <div class="text-caption text-grey-5">Re-uploading updates tracking and status for existing orders.</div>
          <div v-if="uploadResult" class="q-pa-md" style="background: var(--erp-bg-tertiary); border-radius: 8px;">
            <div class="text-green-4">Inserted: {{ uploadResult.inserted }}</div>
            <div class="text-cyan-4">Updated: {{ uploadResult.updated }}</div>
            <div class="text-orange-4">Skipped: {{ uploadResult.skipped }}</div>
            <div v-if="uploadResult.errors" class="text-red-4">Errors: {{ uploadResult.errors }}</div>
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Close" color="grey-5" v-close-popup no-caps @click="uploadResult = null; uploadFile = null" />
          <q-btn unelevated label="Upload" color="cyan-7" no-caps @click="handleUpload" :loading="uploading" :disable="!uploadFile" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Match Dialog -->
    <q-dialog v-model="showMatchDialog">
      <q-card style="min-width: 480px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Match to Orders</div>
          <div class="text-caption text-grey-5 q-mt-xs">
            Matches supplier orders to Etsy orders by customer name + address.
            Adds fulfillment cost and tracking number.
          </div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <!-- Exchange rate info -->
          <div class="q-pa-sm" style="background: var(--erp-bg-tertiary); border-radius: 6px; font-size: 13px;">
            <div class="row items-center q-mb-xs">
              <span class="text-grey-4 text-weight-medium">Currency Conversion</span>
              <q-space />
              <span class="text-grey-5">{{ supplierCurrency }} → {{ targetStoreCurrencies }}</span>
            </div>
            <div v-if="financeRateInfo" class="text-green-4">
              <q-icon name="check_circle" size="xs" class="q-mr-xs" />
              Finance rate found: 1 {{ supplierCurrency }} = {{ financeRateInfo.rate }} {{ financeRateInfo.toCurrency }} ({{ financeRateInfo.effectiveDate }})
            </div>
            <div v-else class="text-orange-4">
              <q-icon name="warning" size="xs" class="q-mr-xs" />
              No rate found in Finance > Currencies &amp; Rates. Manual rate will be used.
            </div>
          </div>
          <q-input
            v-model.number="exchangeRate"
            type="number"
            step="0.0001"
            :label="`${supplierCurrency} to ${targetStoreCurrencies} fallback rate`"
            dense outlined dark
            :hint="financeRateInfo ? 'Finance rate will be used first. This is the fallback if Finance rate is missing.' : 'Required — enter the exchange rate manually.'"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated label="Match" color="teal-7" no-caps @click="handleMatch" :loading="matching" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Manual Match Dialog (for zero-candidate unmatched rows) -->
    <q-dialog v-model="showManualMatchDialog">
      <q-card style="max-width: 820px; width: 100%; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Match Transaction to Order</div>
          <div v-if="manualMatchTxn" class="text-caption text-grey-5 q-mt-xs">
            <span class="text-white text-weight-medium">{{ manualMatchTxn.supplierOrderId }}</span>
            <span v-if="manualMatchTxn.fullName" class="q-ml-sm">— {{ manualMatchTxn.fullName }}</span>
            <span v-if="manualMatchTxn.externalNumber" class="text-cyan-4 q-ml-sm">
              · External #{{ manualMatchTxn.externalNumber }}
            </span>
            <span v-if="manualMatchTxn.amount != null" class="q-ml-sm">
              · ${{ Number(manualMatchTxn.amount).toFixed(2) }}
            </span>
          </div>
          <div v-if="financeRateInfo" class="text-caption text-green-4 q-mt-xs">
            <q-icon name="check_circle" size="xs" class="q-mr-xs" />
            Rate: 1 {{ supplierCurrency }} = {{ financeRateInfo.rate }} {{ financeRateInfo.toCurrency }}
          </div>
          <div v-else class="text-caption text-orange-4 q-mt-xs">
            <q-icon name="warning" size="xs" class="q-mr-xs" />
            No Finance rate — edit rate below or currency conversion may fail.
          </div>
        </q-card-section>
        <q-card-section class="q-gutter-sm">
          <q-input
            v-model="manualSearchQuery"
            label="Search orders (platform ID, customer name, address)"
            dense outlined dark clearable
            @update:model-value="onManualSearchInput"
          >
            <template v-slot:prepend><q-icon name="search" color="grey-5" /></template>
            <template v-slot:append>
              <q-spinner v-if="manualSearchLoading" size="16px" color="cyan-4" />
            </template>
          </q-input>
          <q-input
            v-model.number="exchangeRate"
            type="number"
            step="0.0001"
            :label="`${supplierCurrency} → ${targetStoreCurrencies} rate (fallback)`"
            dense outlined dark
          />
        </q-card-section>
        <q-card-section style="max-height: 55vh; overflow-y: auto;">
          <div v-if="!manualSearchResults.length && manualSearchQuery && manualSearchQuery.trim().length >= 2 && !manualSearchLoading" class="text-grey-5 text-caption text-center q-pa-md">
            No orders found for "{{ manualSearchQuery }}".
          </div>
          <div v-else-if="!manualSearchQuery || manualSearchQuery.trim().length < 2" class="text-grey-5 text-caption text-center q-pa-md">
            Type at least 2 characters to search.
          </div>
          <q-card
            v-for="c in manualSearchResults" :key="c.orderId"
            flat class="q-mb-sm cursor-pointer"
            :style="{
              background: manualSelectedOrderId === c.orderId ? 'rgba(0,188,212,0.12)' : 'var(--erp-bg)',
              border: manualSelectedOrderId === c.orderId ? '1px solid rgba(0,188,212,0.4)' : '1px solid var(--erp-border-subtle)',
              borderRadius: '6px'
            }"
            @click="manualSelectedOrderId = c.orderId"
          >
            <q-card-section class="q-pa-sm" style="font-size: 12px;">
              <div class="row items-center q-mb-xs">
                <q-icon v-if="manualSelectedOrderId === c.orderId" name="radio_button_checked" color="cyan-4" size="sm" class="q-mr-sm" />
                <q-icon v-else name="radio_button_unchecked" color="grey-6" size="sm" class="q-mr-sm" />
                <span class="text-white text-weight-medium">#{{ c.platformOrderId }}</span>
                <q-badge color="blue-grey-8" dense class="q-ml-sm">{{ c.storeName }}</q-badge>
                <q-badge v-if="c.status" :color="orderStatusColor(c.status)" dense class="q-ml-xs">{{ (c.status || '').replace(/_/g, ' ') }}</q-badge>
                <q-icon v-if="c.synced" name="link" color="teal-4" size="xs" class="q-ml-xs"><q-tooltip>Card synced</q-tooltip></q-icon>
                <q-icon v-if="c.hasFulfillmentCost" name="paid" color="amber-5" size="xs" class="q-ml-xs"><q-tooltip>Already has fulfillment cost</q-tooltip></q-icon>
                <q-icon v-if="c.hasTracking" name="local_shipping" color="teal-4" size="xs" class="q-ml-xs"><q-tooltip>Already has tracking</q-tooltip></q-icon>
                <q-space />
                <span class="text-white text-weight-medium" style="font-size: 13px;">{{ c.orderTotal ? Number(c.orderTotal).toFixed(2) : '-' }} {{ c.currency || '' }}</span>
              </div>
              <div class="row q-col-gutter-x-md q-col-gutter-y-xs q-ml-lg" style="color: #b0bec5;">
                <div class="col-6"><span class="text-grey-6">Date:</span> {{ c.orderDate ? new Date(c.orderDate).toLocaleDateString() : '-' }}</div>
                <div class="col-6"><span class="text-grey-6">Items:</span> {{ c.numberOfItems || '-' }}</div>
                <div class="col-6"><span class="text-grey-6">Customer:</span> {{ c.customerName || '-' }}</div>
                <div class="col-6"><span class="text-grey-6">SKU:</span> {{ c.sku || '-' }}</div>
                <div class="col-12"><span class="text-grey-6">Address:</span> {{ [c.shipStreet1, c.shipCity, c.shipState, c.shipZipcode, c.shipCountry].filter(Boolean).join(', ') }}</div>
              </div>
            </q-card-section>
          </q-card>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn
            unelevated color="teal-7" no-caps label="Confirm Match" icon="check"
            :disable="!manualSelectedOrderId"
            :loading="manualMatching"
            @click="confirmManualMatchFromSearch"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Match Result Dialog -->
    <q-dialog v-model="showMatchResult" :maximized="matchResult?.ambiguousMatches?.length > 0">
      <q-card :style="matchResult?.ambiguousMatches?.length > 0 ? 'max-width: 800px; width: 100%;' : 'min-width: 400px;'" style="background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Match Result</div>
        </q-card-section>
        <q-card-section v-if="matchResult" style="max-height: 70vh; overflow-y: auto;">
          <div class="row q-gutter-sm q-mb-md">
            <q-badge color="green-7" class="q-pa-sm text-body2">{{ matchResult.matched }} Matched</q-badge>
            <q-badge color="orange-7" class="q-pa-sm text-body2">{{ matchResult.noMatch }} No match</q-badge>
            <q-badge color="cyan-7" class="q-pa-sm text-body2">{{ matchResult.alreadyHasCost }} Already has cost</q-badge>
            <q-badge v-if="matchResult.ambiguous" color="amber-7" class="q-pa-sm text-body2">{{ matchResult.ambiguous }} Ambiguous</q-badge>
          </div>

          <div v-if="matchResult.errors && matchResult.errors.length" class="q-mb-md">
            <div class="text-red-4 text-caption" v-for="(e, i) in matchResult.errors" :key="i">{{ e }}</div>
          </div>

          <!-- Ambiguous matches — click row to expand detail -->
          <div v-if="matchResult.ambiguousMatches && matchResult.ambiguousMatches.length">
            <div class="text-overline text-amber-4 q-mb-sm">AMBIGUOUS MATCHES — Click to review and select ({{ matchResult.ambiguousMatches.length }})</div>
            <q-card v-for="(amb, ai) in matchResult.ambiguousMatches" :key="ai" flat class="q-mb-sm" style="background: var(--erp-bg-tertiary); border: 1px solid var(--erp-border-subtle); border-radius: 8px;">
              <!-- Summary row (clickable) -->
              <q-card-section class="q-pa-sm cursor-pointer" @click="amb._expanded = !amb._expanded">
                <div class="row items-center">
                  <q-icon :name="amb._expanded ? 'expand_less' : 'expand_more'" color="grey-5" class="q-mr-sm" />
                  <div class="col">
                    <span class="text-white text-weight-medium">{{ amb.fullName }}</span>
                    <span class="text-grey-5 q-ml-sm" style="font-size: 12px;">{{ amb.streetAddress }}</span>
                  </div>
                  <q-badge color="amber-8" class="q-mr-sm">{{ amb.candidates.length }} orders</q-badge>
                  <span class="text-grey-4" style="font-size: 13px;">${{ Number(amb.amount).toFixed(2) }}</span>
                </div>
              </q-card-section>

              <!-- Expanded detail -->
              <q-card-section v-if="amb._expanded" class="q-pa-md q-pt-none">
                <!-- Supplier transaction info -->
                <div class="q-mb-md q-pa-sm" style="background: var(--erp-bg); border-radius: 6px; font-size: 12px;">
                  <div class="text-overline text-grey-5 q-mb-xs" style="font-size: 10px;">SUPPLIER TRANSACTION</div>
                  <div class="row q-col-gutter-sm">
                    <div class="col-6"><span class="text-grey-5">Supplier ID:</span> <span class="text-white">{{ amb.supplierOrderId }}</span></div>
                    <div class="col-6"><span class="text-grey-5">Amount:</span> <span class="text-white">${{ Number(amb.amount).toFixed(2) }}</span></div>
                    <div class="col-6"><span class="text-grey-5">Customer:</span> <span class="text-white">{{ amb.fullName }}</span></div>
                    <div class="col-6"><span class="text-grey-5">Address:</span> <span class="text-white">{{ amb.streetAddress }}</span></div>
                    <div v-if="amb.city" class="col-6"><span class="text-grey-5">City:</span> <span class="text-white">{{ amb.city }}, {{ amb.stateRegion }} {{ amb.postalCode }}</span></div>
                    <div v-if="amb.trackingId" class="col-6"><span class="text-grey-5">Tracking:</span> <span class="text-white">{{ amb.trackingId }}</span></div>
                  </div>
                </div>

                <!-- Candidate orders -->
                <div class="text-overline text-grey-5 q-mb-xs" style="font-size: 10px;">SELECT THE CORRECT ORDER</div>
                <q-card
                  v-for="c in amb.candidates" :key="c.orderId"
                  flat class="q-mb-sm cursor-pointer"
                  :style="{
                    background: amb._selected === c.orderId ? 'rgba(0,188,212,0.12)' : 'var(--erp-bg)',
                    border: amb._selected === c.orderId ? '1px solid rgba(0,188,212,0.4)' : '1px solid var(--erp-border-subtle)',
                    borderRadius: '6px'
                  }"
                  @click="amb._selected = c.orderId"
                >
                  <q-card-section class="q-pa-sm" style="font-size: 12px;">
                    <!-- Header row -->
                    <div class="row items-center q-mb-xs">
                      <q-icon v-if="amb._selected === c.orderId" name="radio_button_checked" color="cyan-4" size="sm" class="q-mr-sm" />
                      <q-icon v-else name="radio_button_unchecked" color="grey-6" size="sm" class="q-mr-sm" />
                      <span class="text-white text-weight-medium">#{{ c.platformOrderId }}</span>
                      <q-badge color="blue-grey-8" dense class="q-ml-sm">{{ c.storeName }}</q-badge>
                      <q-badge v-if="c.status" :color="orderStatusColor(c.status)" dense class="q-ml-xs">{{ (c.status || '').replace(/_/g, ' ') }}</q-badge>
                      <q-icon v-if="c.synced" name="link" color="teal-4" size="xs" class="q-ml-xs" />
                      <q-space />
                      <span class="text-white text-weight-medium" style="font-size: 13px;">{{ c.orderTotal ? Number(c.orderTotal).toFixed(2) : '-' }} {{ c.currency || '' }}</span>
                    </div>
                    <!-- Detail grid -->
                    <div class="row q-col-gutter-x-md q-col-gutter-y-xs q-ml-lg" style="color: #b0bec5;">
                      <div class="col-6"><span class="text-grey-6">Date:</span> {{ c.orderDate ? new Date(c.orderDate).toLocaleDateString() : '-' }}</div>
                      <div class="col-6"><span class="text-grey-6">Items:</span> {{ c.numberOfItems || '-' }}</div>
                      <div class="col-6"><span class="text-grey-6">Customer:</span> {{ c.customerName || '-' }}</div>
                      <div class="col-6"><span class="text-grey-6">SKU:</span> {{ c.sku || '-' }}</div>
                      <div class="col-12"><span class="text-grey-6">Address:</span> {{ [c.shipStreet1, c.shipCity, c.shipState, c.shipZipcode, c.shipCountry].filter(Boolean).join(', ') }}</div>
                      <!-- Items -->
                      <div v-if="c.items && c.items.length" class="col-12 q-mt-xs">
                        <div v-for="(item, ii) in c.items" :key="ii" class="q-mb-xs" style="padding-left: 8px; border-left: 2px solid rgba(0,188,212,0.3);">
                          <div class="text-grey-3">{{ item.productName || 'Item' }} <span v-if="item.sku" class="text-grey-5">({{ item.sku }})</span> <span v-if="item.quantity > 1">x{{ item.quantity }}</span></div>
                          <div v-if="item.variations" class="text-grey-5" style="font-size: 11px; white-space: pre-wrap;">{{ decodeVariations(item.variations) }}</div>
                        </div>
                      </div>
                    </div>
                  </q-card-section>
                </q-card>

                <q-btn
                  v-if="amb._selected"
                  unelevated color="teal-7" size="sm" no-caps
                  label="Confirm Match"
                  icon="check"
                  class="q-mt-sm"
                  :loading="amb._loading"
                  @click="confirmManualMatch(amb)"
                />
              </q-card-section>
            </q-card>
          </div>
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
import { useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import { ecomSupplierApi, ecomSupplierTxnApi, ecomStoreApi, ecomOrderApi } from 'src/api/ecommerce'

const route = useRoute()
const $q = useQuasar()

const loading = ref(false)
const supplier = ref(null)
const transactions = ref([])
const loadingTxns = ref(false)

const filters = ref({ status: null, matched: null, search: '' })
const statusOptions = [
  { label: 'New', value: 'New' }, { label: 'Processing', value: 'Processing' },
  { label: 'Shipped', value: 'Shipped' }, { label: 'Received', value: 'Received' },
  { label: 'Cancelled', value: 'Cancelled' }, { label: 'Not paid yet', value: 'Not paid yet' }
]
const matchedOptions = [
  { label: 'Matched', value: 'yes' }, { label: 'Unmatched', value: 'no' }
]

const columns = [
  { name: 'orderDate', label: 'Date', field: 'orderDate', align: 'left', sortable: true, format: v => v ? new Date(v).toLocaleDateString() : '' },
  { name: 'supplierOrderId', label: 'Supplier ID', field: 'supplierOrderId', align: 'left', sortable: true },
  { name: 'externalNumber', label: 'External #', field: 'externalNumber', align: 'left', sortable: true },
  { name: 'fullName', label: 'Customer', field: 'fullName', align: 'left', sortable: true },
  { name: 'streetAddress', label: 'Address', field: row => [row.streetAddress, row.city, row.stateRegion, row.postalCode].filter(Boolean).join(', '), align: 'left', style: 'max-width: 250px', classes: 'ellipsis' },
  { name: 'amount', label: 'Amount', field: 'amount', align: 'right', sortable: true },
  { name: 'status', label: 'Status', field: 'status', align: 'center', sortable: true },
  { name: 'trackingId', label: 'Tracking', field: 'trackingId', align: 'left' },
  { name: 'shipMethod', label: 'Ship', field: 'shipMethod', align: 'left' },
  { name: 'matched', label: 'Order', field: 'matched', align: 'center', sortable: true }
]

const filteredTxns = computed(() => {
  return transactions.value.filter(t => {
    if (filters.value.status && t.status !== filters.value.status) return false
    if (filters.value.matched === 'yes' && !t.matched) return false
    if (filters.value.matched === 'no' && t.matched) return false
    if (filters.value.search) {
      const q = filters.value.search.toLowerCase()
      if (!(t.fullName || '').toLowerCase().includes(q) &&
          !(t.supplierOrderId || '').toLowerCase().includes(q) &&
          !(t.externalNumber || '').toLowerCase().includes(q) &&
          !(t.streetAddress || '').toLowerCase().includes(q) &&
          !(t.trackingId || '').toLowerCase().includes(q) &&
          !(t.matchedPlatformOrderId || '').includes(q)) return false
    }
    return true
  })
})

// Upload
const showUpload = ref(false)
const uploading = ref(false)
const uploadFile = ref(null)
const uploadResult = ref(null)

// Match
const showMatchDialog = ref(false)
const matching = ref(false)
const showMatchResult = ref(false)
const matchResult = ref(null)
const exchangeRate = ref(null)
const supplierCurrency = ref('USD')
const targetStoreCurrencies = ref('EUR')
const financeRateInfo = ref(null)

// Manual match (from unmatched row)
const showManualMatchDialog = ref(false)
const manualMatchTxn = ref(null)
const manualSearchQuery = ref('')
const manualSearchResults = ref([])
const manualSearchLoading = ref(false)
const manualSelectedOrderId = ref(null)
const manualMatching = ref(false)
let manualSearchDebounce = null

function orderStatusColor (s) {
  return { NEW_ORDER: 'blue-7', CONFIRMED: 'green-7', DESIGNING: 'purple-7', FULFILLED: 'teal-7', TRACK_GENERATED: 'cyan-7', COMPLETED: 'grey-7', CANCELLED: 'red-7' }[s] || 'orange-7'
}

function decodeVariations (v) {
  if (!v) return ''
  return v.replace(/&quot;/g, '"').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>')
}

function statusColor (s) {
  return { New: 'blue-7', Processing: 'purple-7', Shipped: 'teal-7', Received: 'green-7', Cancelled: 'red-7', 'Not paid yet': 'grey-7', 'Processing(holdup)': 'orange-7', 'Received(holdup)': 'amber-8' }[s] || 'grey-7'
}

async function loadSupplier () {
  loading.value = true
  try {
    const res = await ecomSupplierApi.getById(route.params.id)
    supplier.value = res.data.data
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load supplier' })
  } finally { loading.value = false }
}

async function loadTransactions () {
  loadingTxns.value = true
  try {
    const res = await ecomSupplierTxnApi.getAll(route.params.id)
    transactions.value = res.data.data || []
  } catch { /* ignore */ }
  finally { loadingTxns.value = false }
}

async function handleUpload () {
  if (!uploadFile.value) return
  uploading.value = true
  uploadResult.value = null
  try {
    const res = await ecomSupplierTxnApi.upload(route.params.id, uploadFile.value)
    uploadResult.value = res.data.data
    $q.notify({ type: 'positive', message: `Uploaded: ${uploadResult.value.inserted} new, ${uploadResult.value.updated} updated` })
    loadTransactions()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Upload failed' })
  } finally { uploading.value = false }
}

async function openMatchDialog () {
  // Detect supplier currency from transactions
  if (transactions.value.length) {
    const firstCurrency = transactions.value.find(t => t.currency)?.currency
    if (firstCurrency) supplierCurrency.value = firstCurrency
  }

  // Detect store currencies
  try {
    const res = await ecomStoreApi.getAll()
    const stores = res.data.data || []
    const currencies = [...new Set(stores.map(s => s.currency).filter(Boolean))]
    if (currencies.length) targetStoreCurrencies.value = currencies.join(', ')
  } catch { /* ignore */ }

  // Try to load finance exchange rate
  financeRateInfo.value = null
  try {
    const { financeCurrencyApi } = await import('src/api/finance')
    const res = await financeCurrencyApi.getRates()
    const rates = res.data.data || []
    // Find the most recent rate matching supplier currency → any store currency
    const targets = targetStoreCurrencies.value.split(', ').map(c => c.trim().toUpperCase())
    for (const target of targets) {
      const matching = rates
        .filter(r => r.fromCurrency === supplierCurrency.value.toUpperCase() && r.toCurrency === target)
        .sort((a, b) => b.effectiveDate?.localeCompare(a.effectiveDate))
      if (matching.length) {
        financeRateInfo.value = matching[0]
        exchangeRate.value = matching[0].rate
        break
      }
    }
  } catch { /* finance module may not be available */ }

  if (!exchangeRate.value) exchangeRate.value = 0.92

  showMatchDialog.value = true
}

async function ensureExchangeRate () {
  if (exchangeRate.value) return
  // Mirrors openMatchDialog's rate discovery so the manual picker can also use it.
  if (transactions.value.length) {
    const firstCurrency = transactions.value.find(t => t.currency)?.currency
    if (firstCurrency) supplierCurrency.value = firstCurrency
  }
  try {
    const res = await ecomStoreApi.getAll()
    const stores = res.data.data || []
    const currencies = [...new Set(stores.map(s => s.currency).filter(Boolean))]
    if (currencies.length) targetStoreCurrencies.value = currencies.join(', ')
  } catch { /* ignore */ }
  try {
    const { financeCurrencyApi } = await import('src/api/finance')
    const res = await financeCurrencyApi.getRates()
    const rates = res.data.data || []
    const targets = targetStoreCurrencies.value.split(', ').map(c => c.trim().toUpperCase())
    for (const target of targets) {
      const matching = rates
        .filter(r => r.fromCurrency === supplierCurrency.value.toUpperCase() && r.toCurrency === target)
        .sort((a, b) => b.effectiveDate?.localeCompare(a.effectiveDate))
      if (matching.length) {
        financeRateInfo.value = matching[0]
        exchangeRate.value = matching[0].rate
        break
      }
    }
  } catch { /* finance module may be unavailable */ }
}

async function openManualMatchDialog (txn) {
  manualMatchTxn.value = txn
  manualSearchQuery.value = txn.externalNumber || txn.fullName || ''
  manualSearchResults.value = []
  manualSelectedOrderId.value = null
  showManualMatchDialog.value = true
  await ensureExchangeRate()
  if (manualSearchQuery.value && manualSearchQuery.value.trim().length >= 2) doManualSearch()
}

async function doManualSearch () {
  const q = (manualSearchQuery.value || '').trim()
  if (q.length < 2) { manualSearchResults.value = []; return }
  manualSearchLoading.value = true
  try {
    const res = await ecomOrderApi.search(q, 25)
    manualSearchResults.value = res.data.data || []
  } catch {
    manualSearchResults.value = []
  } finally { manualSearchLoading.value = false }
}

function onManualSearchInput () {
  clearTimeout(manualSearchDebounce)
  manualSearchDebounce = setTimeout(doManualSearch, 300)
}

async function confirmManualMatchFromSearch () {
  if (!manualSelectedOrderId.value || !manualMatchTxn.value) return
  manualMatching.value = true
  try {
    await ecomSupplierTxnApi.manualMatch(
      route.params.id,
      manualMatchTxn.value.id,
      manualSelectedOrderId.value,
      exchangeRate.value || null
    )
    $q.notify({ type: 'positive', message: 'Matched successfully' })
    showManualMatchDialog.value = false
    loadTransactions()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Match failed' })
  } finally { manualMatching.value = false }
}

async function confirmManualMatch (amb) {
  if (!amb._selected) return
  amb._loading = true
  try {
    await ecomSupplierTxnApi.manualMatch(route.params.id, amb.transactionId, amb._selected, exchangeRate.value || null)
    $q.notify({ type: 'positive', message: 'Matched successfully', timeout: 1000 })
    // Remove from ambiguous list
    const idx = matchResult.value.ambiguousMatches.indexOf(amb)
    if (idx >= 0) matchResult.value.ambiguousMatches.splice(idx, 1)
    matchResult.value.matched = (matchResult.value.matched || 0) + 1
    matchResult.value.ambiguous = (matchResult.value.ambiguous || 1) - 1
    loadTransactions()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Match failed' })
  } finally { amb._loading = false }
}

async function handleMatch () {
  matching.value = true
  try {
    const res = await ecomSupplierTxnApi.match(route.params.id, exchangeRate.value || null)
    matchResult.value = res.data.data
    showMatchDialog.value = false
    showMatchResult.value = true
    loadTransactions()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Match failed' })
  } finally { matching.value = false }
}

onMounted(() => {
  loadSupplier()
  loadTransactions()
})
</script>

<style scoped>
.erp-card, .erp-table { background: var(--erp-bg-tertiary); border-color: var(--erp-border-subtle); }
.erp-table :deep(th) { color: #b0bec5; font-weight: 600; background: var(--erp-bg); }
.erp-table :deep(td) { color: #e0e0e0; border-color: var(--erp-border-subtle); }
</style>
