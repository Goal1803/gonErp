<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/finance/gma" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">Amazon Reconciliation</div>
        <div class="text-caption text-grey-5">Reconcile Amazon transactions per marketplace</div>
      </div>
      <q-space />
      <q-select
        v-model="selectedReport"
        :options="reports"
        option-value="id"
        :option-label="r => `${monthNames[r.month]} ${r.year}`"
        emit-value map-options
        label="Report"
        dense outlined dark
        style="min-width: 200px"
        class="q-mr-sm"
      />
      <q-btn unelevated color="green-7" icon="sync" label="Generate" no-caps @click="generate" :loading="generating" :disable="!selectedReport" />
    </div>

    <div v-if="reconciliations.length" class="q-gutter-md">
      <q-card v-for="recon in reconciliations" :key="recon.id" flat bordered class="erp-card">
        <q-card-section>
          <div class="text-h6 text-white">{{ recon.marketplace }}</div>
        </q-card-section>
        <q-card-section>
          <div class="row q-gutter-lg">
            <div>
              <div class="text-caption text-grey-5">Total Sales</div>
              <div class="text-h6 text-green-4">{{ formatAmount(recon.totalSales) }}</div>
            </div>
            <div>
              <div class="text-caption text-grey-5">Total Fees</div>
              <div class="text-h6 text-red-4">{{ formatAmount(recon.totalFees) }}</div>
            </div>
            <div>
              <div class="text-caption text-grey-5">Total Payouts</div>
              <div class="text-h6 text-blue-4">{{ formatAmount(recon.totalPayouts) }}</div>
            </div>
            <div>
              <div class="text-caption text-grey-5">Discrepancy</div>
              <div class="text-h6" :class="Number(recon.discrepancy) === 0 ? 'text-green-4' : 'text-orange-4'">
                {{ formatAmount(recon.discrepancy) }}
                <q-icon v-if="Number(recon.discrepancy) === 0" name="check_circle" color="green-4" class="q-ml-xs" />
                <q-icon v-else name="warning" color="orange-4" class="q-ml-xs" />
              </div>
            </div>
          </div>
        </q-card-section>
        <q-card-section v-if="recon.reconciliationData?.byType">
          <div class="text-subtitle2 text-grey-5 q-mb-sm">Breakdown by Type</div>
          <q-list dense bordered class="rounded-borders">
            <q-item v-for="(amount, type) in recon.reconciliationData.byType" :key="type">
              <q-item-section>
                <q-item-label class="text-white">{{ type }}</q-item-label>
              </q-item-section>
              <q-item-section side>
                <q-item-label :class="Number(amount) < 0 ? 'text-red-4' : 'text-green-4'">
                  {{ formatAmount(amount) }}
                </q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-card-section>
      </q-card>
    </div>

    <div v-else class="text-center text-grey-5 q-pa-xl">
      Select a report and click Generate to create Amazon reconciliation
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { financeAmazonApi } from 'src/api/finance'
import { useFinanceStore } from 'src/stores/financeStore'

const $q = useQuasar()
const financeStore = useFinanceStore()
const reports = ref([])
const selectedReport = ref(null)
const reconciliations = ref([])
const generating = ref(false)

const monthNames = ['', 'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']

function formatAmount(val) {
  if (val == null) return '-'
  return Number(val).toLocaleString('de-DE', { style: 'currency', currency: 'EUR' })
}

async function loadReconciliations() {
  if (!selectedReport.value) return
  try {
    const res = await financeAmazonApi.getByReport(selectedReport.value)
    reconciliations.value = res.data.data
  } catch { reconciliations.value = [] }
}

watch(selectedReport, loadReconciliations)

async function generate() {
  if (!selectedReport.value) return
  generating.value = true
  try {
    await financeAmazonApi.generate(selectedReport.value)
    await loadReconciliations()
    $q.notify({ type: 'positive', message: 'Reconciliation generated' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' })
  } finally { generating.value = false }
}

onMounted(async () => {
  await financeStore.fetchReports()
  reports.value = financeStore.reports
  if (reports.value.length) selectedReport.value = reports.value[0].id
})
</script>
