<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/ecommerce" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">POD Suppliers</div>
        <div class="text-caption text-grey-5">Manage print-on-demand suppliers</div>
      </div>
      <q-space />
      <q-btn unelevated color="cyan-7" icon="add" label="Add Supplier" no-caps @click="openCreate" />
    </div>

    <q-table
      :rows="suppliers"
      :columns="columns"
      row-key="id"
      flat bordered
      :loading="loading"
      class="erp-table"
      :pagination="{ rowsPerPage: 20 }"
    >
      <template v-slot:body-cell-countries="props">
        <q-td :props="props">
          <q-badge v-for="c in (props.row.countries || [])" :key="c" color="blue-grey-8" class="q-mr-xs" dense>{{ c }}</q-badge>
        </q-td>
      </template>
      <template v-slot:body-cell-website="props">
        <q-td :props="props">
          <a v-if="props.row.website" :href="props.row.website" target="_blank" class="text-cyan-4" @click.stop>{{ props.row.website }}</a>
          <span v-else class="text-grey-6">-</span>
        </q-td>
      </template>
      <template v-slot:body-cell-priceListUrl="props">
        <q-td :props="props">
          <a v-if="props.row.priceListUrl" :href="props.row.priceListUrl" target="_blank" class="text-cyan-4" @click.stop>View</a>
          <span v-else class="text-grey-6">-</span>
        </q-td>
      </template>
      <template v-slot:body-cell-active="props">
        <q-td :props="props">
          <q-badge :color="props.row.active ? 'green-7' : 'grey-7'" :label="props.row.active ? 'Active' : 'Inactive'" />
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn flat dense icon="edit" color="cyan-4" size="sm" @click="openEdit(props.row)" />
          <q-btn flat dense icon="delete" color="red-4" size="sm" @click="confirmDelete(props.row)" />
        </q-td>
      </template>
    </q-table>

    <!-- Create/Edit Dialog -->
    <q-dialog v-model="showForm" persistent>
      <q-card style="min-width: 500px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">{{ editingId ? 'Edit Supplier' : 'New Supplier' }}</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-input v-model="form.name" label="Supplier Name *" dense outlined dark :rules="[v => !!v || 'Required']" />
          <q-input v-model="form.description" label="Description" dense outlined dark type="textarea" rows="2" />
          <q-select
            v-model="form.countries"
            :options="countryOptions"
            label="Countries"
            dense outlined dark
            multiple
            emit-value map-options
            use-chips
          />
          <q-input v-model="form.website" label="Website" dense outlined dark />
          <q-input v-model="form.priceListUrl" label="Price List URL" dense outlined dark />
          <q-toggle v-if="editingId" v-model="form.active" label="Active" color="cyan-5" />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated :label="editingId ? 'Save' : 'Create'" color="cyan-7" no-caps @click="handleSave" :loading="saving" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { ecomSupplierApi } from 'src/api/ecommerce'

const $q = useQuasar()
const loading = ref(false)
const suppliers = ref([])
const showForm = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = ref({ name: '', description: '', countries: [], website: '', priceListUrl: '', active: true })

const countryOptions = [
  { label: 'US', value: 'US' },
  { label: 'EU', value: 'EU' },
  { label: 'Vietnam', value: 'Vietnam' },
  { label: 'China', value: 'China' }
]

const columns = [
  { name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true },
  { name: 'description', label: 'Description', field: 'description', align: 'left', style: 'max-width: 250px', classes: 'ellipsis' },
  { name: 'countries', label: 'Countries', field: 'countries', align: 'left' },
  { name: 'website', label: 'Website', field: 'website', align: 'left' },
  { name: 'priceListUrl', label: 'Price List', field: 'priceListUrl', align: 'center' },
  { name: 'active', label: 'Status', field: 'active', align: 'center' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

async function loadSuppliers () {
  loading.value = true
  try {
    const res = await ecomSupplierApi.getAll()
    suppliers.value = res.data.data || []
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load suppliers' })
  } finally {
    loading.value = false
  }
}

function openCreate () {
  editingId.value = null
  form.value = { name: '', description: '', countries: [], website: '', priceListUrl: '', active: true }
  showForm.value = true
}

function openEdit (s) {
  editingId.value = s.id
  form.value = { name: s.name, description: s.description || '', countries: s.countries || [], website: s.website || '', priceListUrl: s.priceListUrl || '', active: s.active }
  showForm.value = true
}

async function handleSave () {
  if (!form.value.name) { $q.notify({ type: 'warning', message: 'Name is required' }); return }
  saving.value = true
  try {
    if (editingId.value) {
      await ecomSupplierApi.update(editingId.value, form.value)
    } else {
      await ecomSupplierApi.create(form.value)
    }
    showForm.value = false
    $q.notify({ type: 'positive', message: editingId.value ? 'Supplier updated' : 'Supplier created' })
    loadSuppliers()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' })
  } finally {
    saving.value = false
  }
}

function confirmDelete (s) {
  $q.dialog({
    title: 'Delete Supplier',
    message: `Delete "${s.name}"?`,
    cancel: true,
    color: 'red'
  }).onOk(async () => {
    try {
      await ecomSupplierApi.delete(s.id)
      $q.notify({ type: 'positive', message: 'Supplier deleted' })
      loadSuppliers()
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' })
    }
  })
}

onMounted(loadSuppliers)
</script>

<style scoped>
.erp-table { background: var(--erp-bg-tertiary); border-color: var(--erp-border-subtle); }
.erp-table :deep(th) { color: #b0bec5; font-weight: 600; background: var(--erp-bg); }
.erp-table :deep(td) { color: #e0e0e0; border-color: var(--erp-border-subtle); }
</style>
