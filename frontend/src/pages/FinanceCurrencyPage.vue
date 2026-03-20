<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/finance" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">Currencies &amp; Rates</div>
        <div class="text-caption text-grey-5">Manage currencies and conversion rates</div>
      </div>
    </div>

    <!-- Currencies Section -->
    <div class="row items-center q-mb-sm">
      <div class="text-subtitle1 text-white text-weight-medium">Currencies</div>
      <q-space />
      <q-btn unelevated color="green-7" icon="add" label="Add Currency" no-caps dense @click="openCurrencyForm(null)" />
    </div>

    <q-table :rows="currencies" :columns="currCols" row-key="id" flat bordered dense class="erp-table q-mb-xl" :pagination="{ rowsPerPage: 20 }">
      <template v-slot:body-cell-base="props">
        <q-td :props="props">
          <q-badge v-if="props.row.base" color="amber-7" label="Base" />
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn flat dense icon="edit" color="blue-4" @click="openCurrencyForm(props.row)" />
          <q-btn flat dense icon="delete" color="red-4" @click="deleteCurrency(props.row)" />
        </q-td>
      </template>
    </q-table>

    <!-- Rates Section -->
    <div class="row items-center q-mb-sm">
      <div class="text-subtitle1 text-white text-weight-medium">Conversion Rates</div>
      <q-space />
      <q-btn unelevated color="green-7" icon="add" label="Add Rate" no-caps dense @click="showRateForm = true" />
    </div>

    <q-table :rows="rates" :columns="rateCols" row-key="id" flat bordered dense class="erp-table" :pagination="{ rowsPerPage: 30 }">
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn flat dense icon="delete" color="red-4" @click="deleteRate(props.row)" />
        </q-td>
      </template>
    </q-table>

    <!-- Currency Form Dialog -->
    <q-dialog v-model="showCurrencyForm">
      <q-card style="min-width: 350px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">{{ editingCurrency ? 'Edit Currency' : 'Add Currency' }}</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-input v-model="currForm.code" label="Code (e.g. EUR)" dense outlined dark :disable="!!editingCurrency" />
          <q-input v-model="currForm.name" label="Name" dense outlined dark />
          <q-input v-model="currForm.symbol" label="Symbol (optional)" dense outlined dark />
          <q-toggle v-model="currForm.base" label="Base currency" color="amber-7" dark />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated :label="editingCurrency ? 'Save' : 'Create'" color="green-7" no-caps @click="saveCurrency" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Rate Form Dialog -->
    <q-dialog v-model="showRateForm">
      <q-card style="min-width: 400px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Add Conversion Rate</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <div class="row q-gutter-md">
            <q-select v-model="rateForm.fromCurrency" :options="currencyCodeOptions" label="From" dense outlined dark class="col" />
            <q-select v-model="rateForm.toCurrency" :options="currencyCodeOptions" label="To" dense outlined dark class="col" />
          </div>
          <q-input v-model.number="rateForm.rate" label="Rate" type="number" step="0.0001" dense outlined dark />
          <q-input v-model="rateForm.effectiveDate" label="Effective Date" type="date" dense outlined dark />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated label="Create" color="green-7" no-caps @click="saveRate" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { financeCurrencyApi } from 'src/api/finance'

const $q = useQuasar()
const currencies = ref([])
const rates = ref([])
const showCurrencyForm = ref(false)
const showRateForm = ref(false)
const editingCurrency = ref(null)

const currForm = ref({ code: '', name: '', symbol: '', base: false })
const rateForm = ref({ fromCurrency: '', toCurrency: '', rate: 1, effectiveDate: new Date().toISOString().slice(0, 10) })

const currencyCodeOptions = computed(() => currencies.value.map(c => c.code))

const currCols = [
  { name: 'code', label: 'Code', field: 'code', align: 'left', sortable: true },
  { name: 'name', label: 'Name', field: 'name', align: 'left' },
  { name: 'symbol', label: 'Symbol', field: 'symbol', align: 'center' },
  { name: 'base', label: 'Base', field: 'base', align: 'center' },
  { name: 'actions', label: '', align: 'right' }
]
const rateCols = [
  { name: 'fromCurrency', label: 'From', field: 'fromCurrency', align: 'left', sortable: true },
  { name: 'toCurrency', label: 'To', field: 'toCurrency', align: 'left' },
  { name: 'rate', label: 'Rate', field: 'rate', align: 'right' },
  { name: 'effectiveDate', label: 'Effective Date', field: 'effectiveDate', align: 'left', sortable: true },
  { name: 'actions', label: '', align: 'right' }
]

async function load() {
  try {
    const [c, r] = await Promise.all([financeCurrencyApi.getAll(), financeCurrencyApi.getRates()])
    currencies.value = c.data.data
    rates.value = r.data.data
  } catch { /* ignore */ }
}

function openCurrencyForm(row) {
  if (row) {
    editingCurrency.value = row.id
    currForm.value = { code: row.code, name: row.name, symbol: row.symbol || '', base: row.base }
  } else {
    editingCurrency.value = null
    currForm.value = { code: '', name: '', symbol: '', base: false }
  }
  showCurrencyForm.value = true
}

async function saveCurrency() {
  try {
    if (editingCurrency.value) {
      await financeCurrencyApi.update(editingCurrency.value, currForm.value)
    } else {
      await financeCurrencyApi.create(currForm.value)
    }
    showCurrencyForm.value = false
    await load()
    $q.notify({ type: 'positive', message: 'Saved' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' })
  }
}

async function deleteCurrency(row) {
  try { await financeCurrencyApi.delete(row.id); await load() }
  catch (e) { $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' }) }
}

async function saveRate() {
  try {
    await financeCurrencyApi.createRate(rateForm.value)
    showRateForm.value = false
    await load()
    $q.notify({ type: 'positive', message: 'Rate added' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' })
  }
}

async function deleteRate(row) {
  try { await financeCurrencyApi.deleteRate(row.id); await load() }
  catch (e) { $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' }) }
}

onMounted(load)
</script>
