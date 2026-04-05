<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" :to="isStoreDashboard ? `/ecommerce/stores/${$route.params.id}` : '/ecommerce'" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">Etsy Dashboard</div>
        <div class="text-caption text-grey-5">Revenue, profit &amp; cost analysis (all amounts in EUR)</div>
      </div>
      <q-space />
    </div>

    <!-- Filters -->
    <div class="row q-gutter-sm q-mb-lg items-center">
      <q-select v-model="storeId" :options="storeOptions" label="Store" dense outlined dark emit-value map-options :clearable="!isStoreDashboard" :disable="isStoreDashboard" style="min-width: 180px" @update:model-value="loadDashboard" />
      <q-input v-model="startDate" label="From" type="date" dense outlined dark style="min-width: 150px" @update:model-value="loadDashboard" />
      <q-input v-model="endDate" label="To" type="date" dense outlined dark style="min-width: 150px" @update:model-value="loadDashboard" />
      <q-btn-group flat>
        <q-btn flat dense no-caps label="This Month" color="grey-5" @click="setRange('month')" />
        <q-btn flat dense no-caps label="Last Month" color="grey-5" @click="setRange('lastMonth')" />
        <q-btn flat dense no-caps label="This Year" color="grey-5" @click="setRange('year')" />
      </q-btn-group>
    </div>

    <q-inner-loading :showing="loading" color="cyan-5" />

    <template v-if="d">
      <!-- Summary Cards -->
      <div class="row q-gutter-md q-mb-lg">
        <div class="col" v-for="card in summaryCards" :key="card.label">
          <q-card flat class="dash-card">
            <q-card-section class="q-pa-md">
              <div class="text-caption text-grey-5">{{ card.label }}</div>
              <div class="text-h5 text-weight-medium" :class="card.color">{{ card.value }}</div>
              <div v-if="card.sub" class="text-caption" :class="card.subColor || 'text-grey-5'">{{ card.sub }}</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <div class="row q-gutter-lg">
        <!-- Left column -->
        <div class="col-12 col-md-8">
          <!-- Profit Breakdown Bar Chart -->
          <q-card flat class="dash-card q-mb-md">
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-md">Profit Breakdown</div>
              <div v-for="item in d.profitBreakdown" :key="item.label" class="row items-center q-mb-sm">
                <div class="text-grey-4" style="width: 120px; font-size: 13px;">{{ item.label }}</div>
                <div class="col q-mx-sm">
                  <div style="height: 20px; border-radius: 4px; position: relative; overflow: hidden; background: rgba(255,255,255,0.05);">
                    <div :style="{ width: barWidth(item.value) + '%', background: barColor(item.label), height: '100%', borderRadius: '4px' }" />
                  </div>
                </div>
                <div class="text-right" :class="Number(item.value) >= 0 ? 'text-green-4' : 'text-red-4'" style="width: 80px; font-size: 13px;">
                  {{ fmt(item.value) }}
                </div>
              </div>
            </q-card-section>
          </q-card>

          <!-- Daily Trend Line Chart -->
          <q-card flat class="dash-card q-mb-md">
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-sm">Daily Trend</div>
              <div v-if="d.dailyTrend && d.dailyTrend.length" style="overflow-x: auto;">
                <svg :width="Math.max(d.dailyTrend.length * 28, 600)" height="200" class="trend-svg">
                  <!-- Grid lines -->
                  <line v-for="i in 4" :key="'g'+i" :x1="40" :x2="Math.max(d.dailyTrend.length * 28, 600)" :y1="i * 40" :y2="i * 40" stroke="rgba(255,255,255,0.06)" />
                  <!-- Revenue line -->
                  <polyline :points="trendLine('revenue')" fill="none" stroke="#42A5F5" stroke-width="2" stroke-linejoin="round" />
                  <!-- Profit line -->
                  <polyline :points="trendLine('profit')" fill="none" stroke="#66BB6A" stroke-width="2" stroke-linejoin="round" />
                  <!-- Zero line -->
                  <line :x1="40" :x2="Math.max(d.dailyTrend.length * 28, 600)" :y1="zeroY" :y2="zeroY" stroke="rgba(255,255,255,0.15)" stroke-dasharray="4" />
                  <!-- X labels -->
                  <text v-for="(day, idx) in d.dailyTrend" :key="'xl'+idx"
                    :x="40 + idx * 28 + 14" y="195" text-anchor="middle"
                    fill="#78909C" font-size="9" v-if="idx % Math.ceil(d.dailyTrend.length / 15) === 0">
                    {{ day.date.slice(5) }}
                  </text>
                </svg>
                <div class="row q-gutter-md q-mt-xs">
                  <div class="row items-center"><div style="width:12px;height:3px;background:#42A5F5;border-radius:2px" class="q-mr-xs" /><span class="text-grey-5" style="font-size:11px">Revenue</span></div>
                  <div class="row items-center"><div style="width:12px;height:3px;background:#66BB6A;border-radius:2px" class="q-mr-xs" /><span class="text-grey-5" style="font-size:11px">Profit</span></div>
                </div>
              </div>
              <div v-else class="text-grey-6 text-center q-pa-lg">No data for selected period</div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Right column -->
        <div class="col-12 col-md">
          <!-- Order Stats -->
          <q-card flat class="dash-card q-mb-md">
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-sm">Order Stats</div>
              <div class="stat-grid">
                <div><span class="text-grey-5">Total Orders</span><span class="text-white">{{ d.totalOrders }}</span></div>
                <div><span class="text-grey-5">Fulfilled</span><span class="text-green-4">{{ d.fulfilledOrders }}</span></div>
                <div><span class="text-grey-5">Pending Fulfillment</span><span class="text-orange-4">{{ d.pendingFulfillmentOrders }}</span></div>
                <div><span class="text-grey-5">Refunded</span><span class="text-red-4">{{ d.refundedOrders }}</span></div>
                <div><span class="text-grey-5">Avg Revenue/Order</span><span class="text-white">{{ fmt(d.avgRevenuePerOrder) }}</span></div>
                <div><span class="text-grey-5">Avg Profit/Order</span><span class="text-green-4">{{ fmt(d.avgProfitPerOrder) }}</span></div>
                <div><span class="text-grey-5">Profit Margin</span><span class="text-cyan-4">{{ d.profitMarginPct }}%</span></div>
              </div>
            </q-card-section>
          </q-card>

          <!-- Pending Fulfillment -->
          <q-card flat class="dash-card q-mb-md" v-if="d.pendingFulfillmentOrders > 0">
            <q-card-section>
              <div class="text-subtitle1 text-orange-4 q-mb-sm">Pending Fulfillment</div>
              <div class="text-grey-4" style="font-size: 13px;">
                <div>{{ d.pendingFulfillmentOrders }} orders without fulfillment cost</div>
                <div>Revenue at risk: <span class="text-white">{{ fmt(d.pendingFulfillmentRevenue) }}</span></div>
                <div>Earning at risk: <span class="text-white">{{ fmt(d.pendingFulfillmentEarning) }}</span></div>
              </div>
            </q-card-section>
          </q-card>

          <!-- Etsy Balance -->
          <q-card flat class="dash-card q-mb-md">
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-sm">Etsy Balance</div>
              <div class="stat-grid">
                <div><span class="text-grey-5">Sales</span><span class="text-green-4">{{ fmt(d.etsySales) }}</span></div>
                <div><span class="text-grey-5">Fees &amp; Tax</span><span class="text-red-4">{{ fmt(d.etsyFees) }}</span></div>
                <div><span class="text-grey-5">Refunds</span><span class="text-red-4">{{ fmt(d.etsyRefunds) }}</span></div>
                <div><span class="text-grey-5">Ads</span><span class="text-red-4">{{ fmt(d.etsyAds) }}</span></div>
                <div><span class="text-grey-5">Listing Fees</span><span class="text-red-4">{{ fmt(d.etsyListingFees) }}</span></div>
                <div><span class="text-grey-5">Deposits</span><span class="text-cyan-4">{{ fmt(d.etsyDeposits) }}</span></div>
                <div style="border-top: 1px solid rgba(255,255,255,0.1); padding-top: 6px; margin-top: 4px;">
                  <span class="text-grey-4 text-weight-medium">Net Balance</span>
                  <span class="text-white text-weight-medium">{{ fmt(d.etsyNetBalance) }}</span>
                </div>
              </div>
            </q-card-section>
          </q-card>

          <!-- Orders by Status -->
          <q-card flat class="dash-card q-mb-md">
            <q-card-section>
              <div class="text-subtitle1 text-white q-mb-sm">Orders by Status</div>
              <div v-for="s in d.ordersByStatus" :key="s.status" class="row items-center q-mb-xs">
                <q-badge :color="statusColor(s.status)" :label="s.status.replace(/_/g, ' ')" style="min-width: 120px" />
                <div class="col q-mx-sm">
                  <div style="height: 14px; border-radius: 3px; overflow: hidden; background: rgba(255,255,255,0.05);">
                    <div :style="{ width: (s.count / d.totalOrders * 100) + '%', background: statusHex(s.status), height: '100%' }" />
                  </div>
                </div>
                <span class="text-grey-4" style="width: 30px; text-align: right; font-size: 12px;">{{ s.count }}</span>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </template>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useRoute } from 'vue-router'
import { ecomStoreApi } from 'src/api/ecommerce'
import { api } from 'src/boot/axios'

const route = useRoute()
const isStoreDashboard = computed(() => !!route.params.id)

const $q = useQuasar()
const loading = ref(false)
const d = ref(null)
const storeId = ref(null)
const storeOptions = ref([])

// Default: current month
const now = new Date()
const startDate = ref(new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10))
const endDate = ref(now.toISOString().slice(0, 10))

function setRange (type) {
  const n = new Date()
  if (type === 'month') {
    startDate.value = new Date(n.getFullYear(), n.getMonth(), 1).toISOString().slice(0, 10)
    endDate.value = n.toISOString().slice(0, 10)
  } else if (type === 'lastMonth') {
    startDate.value = new Date(n.getFullYear(), n.getMonth() - 1, 1).toISOString().slice(0, 10)
    endDate.value = new Date(n.getFullYear(), n.getMonth(), 0).toISOString().slice(0, 10)
  } else if (type === 'year') {
    startDate.value = new Date(n.getFullYear(), 0, 1).toISOString().slice(0, 10)
    endDate.value = n.toISOString().slice(0, 10)
  }
  loadDashboard()
}

const summaryCards = computed(() => {
  if (!d.value) return []
  return [
    { label: 'Revenue', value: fmt(d.value.totalRevenue), color: 'text-white' },
    { label: 'Platform Fees', value: fmt(d.value.totalPlatformFee), color: 'text-red-4' },
    { label: 'Fulfillment', value: fmt(d.value.totalFulfillmentCost), color: 'text-orange-4' },
    { label: 'Refunds', value: fmt(d.value.totalRefundAmount), color: 'text-red-4', sub: d.value.refundedOrders + ' orders' },
    { label: 'Gross Profit', value: fmt(d.value.totalGrossProfit), color: Number(d.value.totalGrossProfit) >= 0 ? 'text-green-4' : 'text-red-4', sub: d.value.profitMarginPct + '% margin', subColor: 'text-cyan-4' },
    { label: 'Pending', value: d.value.pendingFulfillmentOrders + ' orders', color: 'text-orange-4', sub: fmt(d.value.pendingFulfillmentRevenue) + ' at risk' }
  ]
})

// Chart helpers
const trendMax = computed(() => {
  if (!d.value?.dailyTrend?.length) return 1
  return Math.max(...d.value.dailyTrend.map(t => Math.max(Math.abs(Number(t.revenue)), Math.abs(Number(t.profit)))), 1)
})

const zeroY = computed(() => {
  const max = trendMax.value
  const minVal = d.value?.dailyTrend ? Math.min(...d.value.dailyTrend.map(t => Math.min(Number(t.revenue), Number(t.profit)))) : 0
  if (minVal >= 0) return 170
  return 170 * max / (max - minVal)
})

function trendLine (field) {
  if (!d.value?.dailyTrend?.length) return ''
  const max = trendMax.value
  return d.value.dailyTrend.map((day, idx) => {
    const x = 40 + idx * 28 + 14
    const val = Number(day[field])
    const y = 170 - (val / max) * 150 + 10
    return `${x},${y}`
  }).join(' ')
}

function barWidth (value) {
  if (!d.value?.totalRevenue || Number(d.value.totalRevenue) === 0) return 0
  return Math.min(Math.abs(Number(value)) / Number(d.value.totalRevenue) * 100, 100)
}

function barColor (label) {
  const map = { Revenue: '#42A5F5', 'Platform Fees': '#EF5350', Refunds: '#FF7043', Fulfillment: '#FFA726', 'Other Costs': '#AB47BC', 'Gross Profit': '#66BB6A' }
  return map[label] || '#78909C'
}

function statusColor (s) {
  const map = { NEW_ORDER: 'blue-7', CONFIRMED: 'green-7', DESIGNING: 'purple-7', FULFILLED: 'teal-7', TRACK_GENERATED: 'cyan-7', TRACK_ADDED_TO_STORE: 'cyan-5', COMPLETED: 'grey-7', CANCELLED: 'red-7', DRAFT: 'grey-8' }
  return map[s] || 'orange-7'
}

function statusHex (s) {
  const map = { NEW_ORDER: '#1976D2', CONFIRMED: '#388E3C', DESIGNING: '#7B1FA2', FULFILLED: '#00897B', TRACK_GENERATED: '#00ACC1', COMPLETED: '#616161', CANCELLED: '#D32F2F', DRAFT: '#424242' }
  return map[s] || '#F57C00'
}

const currencySymbol = computed(() => {
  const code = d.value?.currency || 'EUR'
  const map = { EUR: '€', USD: '$', GBP: '£', VND: '₫', CNY: '¥' }
  return map[code] || code + ' '
})

function fmt (v) {
  if (v == null) return '-'
  return currencySymbol.value + Number(v).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadStores () {
  try {
    const res = await ecomStoreApi.getAll()
    const etsyStores = (res.data.data || []).filter(s => s.salesChannel === 'ETSY')
    if (isStoreDashboard.value) {
      // Store-specific dashboard: lock to this store
      const sid = Number(route.params.id)
      const allStores = res.data.data || []
      const store = allStores.find(s => s.id === sid)
      storeOptions.value = store ? [{ label: store.name, value: store.id }] : []
      storeId.value = sid
    } else {
      storeOptions.value = [
        { label: 'All Etsy Stores', value: null },
        ...etsyStores.map(s => ({ label: s.name, value: s.id }))
      ]
    }
  } catch { /* ignore */ }
}

async function loadDashboard () {
  if (!startDate.value || !endDate.value) return
  loading.value = true
  try {
    const params = { startDate: startDate.value, endDate: endDate.value }
    if (storeId.value) params.storeId = storeId.value
    const res = await api.get('/ecommerce/dashboard', { params })
    d.value = res.data.data
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to load dashboard' })
  } finally { loading.value = false }
}

onMounted(() => {
  loadStores()
  loadDashboard()
})
</script>

<style scoped>
.dash-card {
  background: var(--erp-bg-tertiary);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 10px;
}
.stat-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.stat-grid > div {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
.trend-svg {
  display: block;
}
</style>
