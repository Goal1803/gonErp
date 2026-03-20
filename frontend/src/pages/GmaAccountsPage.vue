<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/finance" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">Bank Accounts</div>
        <div class="text-caption text-grey-5">Manage bank &amp; payment accounts and CSV configuration</div>
      </div>
      <q-space />
      <q-btn
        v-if="canManage"
        unelevated
        color="green-7"
        icon="add"
        label="Add Account"
        no-caps
        @click="openForm(null)"
      />
    </div>

    <q-table
      :rows="accounts"
      :columns="columns"
      row-key="id"
      flat
      bordered
      :loading="loading"
      class="erp-table"
      :pagination="{ rowsPerPage: 20 }"
    >
      <template v-slot:body-cell-active="props">
        <q-td :props="props">
          <q-badge :color="props.row.active ? 'green-7' : 'grey-7'" :label="props.row.active ? 'Active' : 'Inactive'" />
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn v-if="canManage" flat dense icon="edit" color="blue-4" @click="openForm(props.row)">
            <q-tooltip>Edit</q-tooltip>
          </q-btn>
          <q-btn v-if="canManage" flat dense icon="delete" color="red-4" @click="confirmDelete(props.row)">
            <q-tooltip>Delete</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <!-- Account Form Dialog -->
    <q-dialog v-model="showForm">
      <q-card style="min-width: 500px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">{{ editing ? 'Edit Account' : 'New Account' }}</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-input v-model="form.name" label="Account Name" dense outlined dark />
          <q-select
            v-model="form.accountType"
            :options="accountTypeOptions"
            label="Account Type"
            dense outlined dark
            emit-value map-options
          />
          <q-input v-model="form.iban" label="IBAN (optional)" dense outlined dark />
          <div class="row q-gutter-md">
            <q-select v-model="form.currency" :options="currencyOptions" label="Currency" dense outlined dark class="col" emit-value map-options />
            <q-input v-model="form.marketplace" label="Marketplace" dense outlined dark class="col" />
          </div>
          <div class="text-subtitle2 text-grey-5 q-mt-md">CSV Settings</div>
          <div class="row q-gutter-md">
            <q-input v-model="form.csvDelimiter" label="Delimiter" dense outlined dark class="col-3" />
            <q-input v-model="form.csvEncoding" label="Encoding" dense outlined dark class="col" />
            <q-input v-model.number="form.csvSkipRows" label="Skip Rows" type="number" dense outlined dark class="col-3" />
          </div>
          <q-input v-model.number="form.displayOrder" label="Display Order" type="number" dense outlined dark />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated :label="editing ? 'Save' : 'Create'" color="green-7" no-caps @click="handleSave" :loading="saving" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { useFinanceStore } from 'src/stores/financeStore'
import { financeCurrencyApi } from 'src/api/finance'

const $q = useQuasar()
const authStore = useAuthStore()
const financeStore = useFinanceStore()

const accounts = computed(() => financeStore.accounts)
const loading = computed(() => financeStore.loading)
const showForm = ref(false)
const editing = ref(null)
const saving = ref(false)
const currencyOptions = ref([])

const canManage = computed(() => {
  if (authStore.isAdmin) return true
  const role = authStore.currentUser?.financeRole
  return role === 'FINANCE_CFO' || role === 'FINANCE_ACCOUNTANT_MANAGER'
})

const defaultForm = () => ({
  name: '', accountType: 'SPARKASSE', iban: '', currency: 'EUR',
  marketplace: '', csvDelimiter: ';', csvEncoding: 'UTF-8', csvSkipRows: 0,
  displayOrder: 0
})

const form = ref(defaultForm())

const accountTypeOptions = [
  { label: 'Sparkasse', value: 'SPARKASSE' },
  { label: 'PayPal', value: 'PAYPAL' },
  { label: 'Wise (Main)', value: 'WISE_MAIN' },
  { label: 'Wise (Sub)', value: 'WISE_SUB' },
  { label: 'Payoneer', value: 'PAYONEER' },
  { label: 'Vivid', value: 'VIVID' },
  { label: 'Cash', value: 'CASH' },
  { label: 'Amazon Seller', value: 'AMAZON_SELLER' },
  { label: 'Amazon VAT', value: 'AMAZON_VAT' }
]

const columns = [
  { name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true },
  { name: 'accountType', label: 'Type', field: 'accountType', align: 'left' },
  { name: 'iban', label: 'IBAN', field: 'iban', align: 'left' },
  { name: 'currency', label: 'Currency', field: 'currency', align: 'center' },
  { name: 'active', label: 'Status', field: 'active', align: 'center' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

function openForm(row) {
  if (row) {
    editing.value = row.id
    form.value = { ...row }
  } else {
    editing.value = null
    form.value = defaultForm()
  }
  showForm.value = true
}

function confirmDelete(row) {
  $q.dialog({
    title: 'Delete Account',
    message: `Delete "${row.name}"?`,
    cancel: true,
    persistent: false,
    color: 'red'
  }).onOk(async () => {
    try {
      await financeStore.deleteAccount(row.id)
      $q.notify({ type: 'positive', message: 'Account deleted' })
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to delete' })
    }
  })
}

async function handleSave() {
  saving.value = true
  try {
    if (editing.value) {
      await financeStore.updateAccount(editing.value, form.value)
    } else {
      await financeStore.createAccount(form.value)
    }
    showForm.value = false
    $q.notify({ type: 'positive', message: editing.value ? 'Account updated' : 'Account created' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to save' })
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  financeStore.fetchAccounts()
  try {
    const res = await financeCurrencyApi.getAll()
    currencyOptions.value = (res.data.data || []).map(c => ({ label: `${c.code} - ${c.name}`, value: c.code }))
  } catch { /* ignore */ }
})
</script>
