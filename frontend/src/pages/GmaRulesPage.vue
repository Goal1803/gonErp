<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/finance/gma" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">Categorization Rules</div>
        <div class="text-caption text-grey-5">Auto-categorize transactions by pattern matching</div>
      </div>
      <q-space />
      <q-btn v-if="!rules.length" flat color="amber-5" icon="auto_fix_high" label="Seed Default Rules" no-caps @click="seedDefaults" class="q-mr-sm" />
      <q-btn unelevated color="green-7" icon="add" label="New Rule" no-caps @click="openForm(null)" />
    </div>

    <q-table
      :rows="rules"
      :columns="columns"
      row-key="id"
      flat bordered
      :loading="loading"
      class="erp-table"
      :pagination="{ rowsPerPage: 50 }"
    >
      <template v-slot:body-cell-active="props">
        <q-td :props="props">
          <q-badge :color="props.row.active ? 'green-7' : 'grey-7'" :label="props.row.active ? 'Active' : 'Inactive'" />
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn flat dense icon="edit" color="blue-4" @click="openForm(props.row)" />
          <q-btn flat dense icon="delete" color="red-4" @click="confirmDelete(props.row)" />
        </q-td>
      </template>
    </q-table>

    <!-- Rule Form Dialog -->
    <q-dialog v-model="showForm" persistent>
      <q-card style="min-width: 550px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">{{ editing ? 'Edit Rule' : 'New Rule' }}</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-input v-model="form.name" label="Rule Name" dense outlined dark />
          <div class="row q-gutter-md">
            <q-input v-model.number="form.priority" label="Priority" type="number" dense outlined dark class="col" />
            <q-toggle v-model="form.active" label="Active" color="green-7" dark class="col" />
          </div>
          <q-select v-model="form.accountType" :options="accountTypeOptions" label="Account Type (optional)" dense outlined dark clearable emit-value map-options />
          <div class="row q-gutter-md">
            <q-select v-model="form.fieldName" :options="fieldOptions" label="Field" dense outlined dark class="col" emit-value map-options />
            <q-select v-model="form.matchType" :options="matchTypeOptions" label="Match Type" dense outlined dark class="col" emit-value map-options />
          </div>
          <q-input v-model="form.matchValue" label="Match Value" dense outlined dark />
          <q-separator />
          <q-input v-model="form.category" label="Category" dense outlined dark />
          <q-input v-model="form.subcategory" label="Subcategory (optional)" dense outlined dark />
          <q-input v-model="form.noteTemplate" label="Note Template (optional)" dense outlined dark hint="Use {{counterparty}}, {{amount}}, {{description}}" />
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
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { financeRuleApi } from 'src/api/finance'

const $q = useQuasar()
const rules = ref([])
const loading = ref(false)
const showForm = ref(false)
const editing = ref(null)
const saving = ref(false)

const defaultForm = () => ({
  name: '', priority: 100, active: true, accountType: null,
  fieldName: 'counterparty', matchType: 'CONTAINS', matchValue: '',
  category: '', subcategory: '', noteTemplate: ''
})
const form = ref(defaultForm())

const columns = [
  { name: 'priority', label: '#', field: 'priority', align: 'center', sortable: true },
  { name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true },
  { name: 'accountType', label: 'Bank Type', field: 'accountType', align: 'left', format: v => v || 'All' },
  { name: 'fieldName', label: 'Field', field: 'fieldName', align: 'left' },
  { name: 'matchType', label: 'Match', field: 'matchType', align: 'left' },
  { name: 'matchValue', label: 'Value', field: 'matchValue', align: 'left' },
  { name: 'category', label: 'Category', field: 'category', align: 'left' },
  { name: 'subcategory', label: 'Subcategory', field: 'subcategory', align: 'left' },
  { name: 'noteTemplate', label: 'Note', field: 'noteTemplate', align: 'left' },
  { name: 'active', label: 'Status', field: 'active', align: 'center' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

const accountTypeOptions = [
  { label: 'Sparkasse', value: 'SPARKASSE' }, { label: 'PayPal', value: 'PAYPAL' },
  { label: 'Wise (Main)', value: 'WISE_MAIN' }, { label: 'Wise (Sub)', value: 'WISE_SUB' },
  { label: 'Payoneer', value: 'PAYONEER' }, { label: 'Vivid', value: 'VIVID' },
  { label: 'Cash', value: 'CASH' }, { label: 'Amazon Seller', value: 'AMAZON_SELLER' },
  { label: 'Amazon VAT', value: 'AMAZON_VAT' }
]
const fieldOptions = [
  { label: 'Counterparty', value: 'counterparty' }, { label: 'Description', value: 'description' },
  { label: 'Amount', value: 'amount' }, { label: 'Currency', value: 'currency' }
]
const matchTypeOptions = [
  { label: 'Contains', value: 'CONTAINS' }, { label: 'Exact', value: 'EXACT' },
  { label: 'Regex', value: 'REGEX' }, { label: 'Greater Than', value: 'GT' },
  { label: 'Less Than', value: 'LT' }
]

async function loadRules() {
  loading.value = true
  try {
    const res = await financeRuleApi.getAll()
    rules.value = res.data.data
  } finally { loading.value = false }
}

async function seedDefaults() {
  try {
    const res = await financeRuleApi.seedDefaults()
    $q.notify({ type: 'positive', message: `Seeded ${res.data.data} default rules` })
    await loadRules()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to seed rules' })
  }
}

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
  $q.dialog({ title: 'Delete Rule', message: `Delete "${row.name}"?`, cancel: true, color: 'red' })
    .onOk(async () => {
      try { await financeRuleApi.delete(row.id); await loadRules(); $q.notify({ type: 'positive', message: 'Deleted' }) }
      catch { $q.notify({ type: 'negative', message: 'Failed' }) }
    })
}

async function handleSave() {
  saving.value = true
  try {
    if (editing.value) {
      await financeRuleApi.update(editing.value, form.value)
    } else {
      await financeRuleApi.create(form.value)
    }
    showForm.value = false
    await loadRules()
    $q.notify({ type: 'positive', message: editing.value ? 'Updated' : 'Created' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' })
  } finally { saving.value = false }
}

onMounted(loadRules)
</script>
