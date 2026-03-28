<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/ecommerce" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">Stores</div>
        <div class="text-caption text-grey-5">Manage sales channel stores</div>
      </div>
      <q-space />
      <q-btn
        v-if="authStore.isAdmin"
        unelevated
        color="cyan-7"
        icon="add"
        label="Add Store"
        no-caps
        @click="openForm(null)"
      />
    </div>

    <q-table
      :rows="stores"
      :columns="columns"
      row-key="id"
      flat
      bordered
      :loading="loading"
      class="erp-table"
      :pagination="{ rowsPerPage: 20 }"
      @row-click="(evt, row) => router.push(`/ecommerce/stores/${row.id}`)"
    >
      <template v-slot:body-cell-active="props">
        <q-td :props="props">
          <q-badge :color="props.row.active ? 'green-7' : 'grey-7'" :label="props.row.active ? 'Active' : 'Inactive'" />
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn flat dense icon="visibility" color="cyan-4" @click.stop="router.push(`/ecommerce/stores/${props.row.id}`)">
            <q-tooltip>View Detail</q-tooltip>
          </q-btn>
          <q-btn v-if="authStore.isAdmin" flat dense icon="edit" color="blue-4" @click.stop="openForm(props.row)">
            <q-tooltip>Edit</q-tooltip>
          </q-btn>
          <q-btn v-if="authStore.isAdmin" flat dense icon="delete" color="red-4" @click.stop="confirmDelete(props.row)">
            <q-tooltip>Delete</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <!-- Create/Edit Dialog -->
    <q-dialog v-model="showForm">
      <q-card style="min-width: 500px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">{{ editing ? 'Edit Store' : 'New Store' }}</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-input v-model="form.name" label="Name *" dense outlined dark :rules="[v => !!v || 'Name is required']" />
          <q-select
            v-model="form.salesChannel"
            :options="salesChannelOptions"
            label="Sales Channel"
            dense outlined dark
            emit-value map-options
          />
          <q-input v-model="form.currency" label="Currency" dense outlined dark />
          <q-input v-model="form.storeUrl" label="Store URL" dense outlined dark />
          <q-toggle v-model="form.active" label="Active" color="cyan-5" />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated :label="editing ? 'Save' : 'Create'" color="cyan-7" no-caps @click="handleSave" :loading="saving" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { ecomStoreApi } from 'src/api/ecommerce'

const router = useRouter()
const $q = useQuasar()
const authStore = useAuthStore()

const stores = ref([])
const loading = ref(false)
const showForm = ref(false)
const editing = ref(null)
const saving = ref(false)

const salesChannelOptions = [
  { label: 'Etsy', value: 'ETSY' },
  { label: 'Amazon', value: 'AMAZON' },
  { label: 'eBay', value: 'EBAY' },
  { label: 'Walmart', value: 'WALMART' },
  { label: 'Shopify', value: 'SHOPIFY' },
  { label: 'Other', value: 'OTHER' }
]

const defaultForm = () => ({
  name: '', salesChannel: 'ETSY', currency: 'EUR', storeUrl: '', active: true
})

const form = ref(defaultForm())

const columns = [
  { name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true },
  { name: 'salesChannel', label: 'Sales Channel', field: 'salesChannel', align: 'left', sortable: true },
  { name: 'currency', label: 'Currency', field: 'currency', align: 'center' },
  { name: 'storeUrl', label: 'URL', field: 'storeUrl', align: 'left' },
  { name: 'active', label: 'Status', field: 'active', align: 'center' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

function openForm (row) {
  if (row) {
    editing.value = row.id
    form.value = {
      name: row.name || '',
      salesChannel: row.salesChannel || 'Etsy',
      currency: row.currency || 'EUR',
      storeUrl: row.storeUrl || '',
      active: row.active ?? true
    }
  } else {
    editing.value = null
    form.value = defaultForm()
  }
  showForm.value = true
}

function confirmDelete (row) {
  $q.dialog({
    title: 'Delete Store',
    message: `Delete "${row.name}"?`,
    cancel: true,
    persistent: false,
    color: 'red'
  }).onOk(async () => {
    try {
      await ecomStoreApi.delete(row.id)
      $q.notify({ type: 'positive', message: 'Store deleted' })
      await loadStores()
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to delete' })
    }
  })
}

async function handleSave () {
  if (!form.value.name) {
    $q.notify({ type: 'warning', message: 'Name is required' })
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await ecomStoreApi.update(editing.value, form.value)
    } else {
      await ecomStoreApi.create(form.value)
    }
    showForm.value = false
    $q.notify({ type: 'positive', message: editing.value ? 'Store updated' : 'Store created' })
    await loadStores()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to save' })
  } finally {
    saving.value = false
  }
}

async function loadStores () {
  loading.value = true
  try {
    const res = await ecomStoreApi.getAll()
    stores.value = res.data.data || []
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to load stores' })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStores()
})
</script>
