<template>
  <q-page padding>
    <!-- Page Header -->
    <div class="page-header flex items-center justify-between">
      <div>
        <div class="text-h5 text-white text-weight-light">
          <q-icon name="settings" color="green-5" class="q-mr-sm" />
          Lookups
        </div>
        <div class="text-caption text-grey-5 q-mt-xs">Manage product types, niches, and occasions</div>
      </div>
    </div>

    <!-- Tabs -->
    <q-tabs v-model="activeTab" dense active-color="teal-5" indicator-color="teal-5"
      class="text-grey-5 q-mb-md" align="left" narrow-indicator>
      <q-tab name="productTypes" label="Product Types" icon="inventory_2" />
      <q-tab name="niches" label="Niches" icon="category" />
      <q-tab name="occasions" label="Occasions" icon="celebration" />
    </q-tabs>

    <!-- Action bar -->
    <div class="flex items-center justify-between q-mb-md">
      <span class="text-grey-5 text-caption">{{ currentItems.length }} {{ currentLabel.toLowerCase() }}(s)</span>
      <q-btn icon="add" :label="'Add ' + currentLabel" color="primary" unelevated size="sm" @click="openAdd" />
    </div>

    <!-- Table -->
    <q-table
      :rows="currentItems"
      :columns="columns"
      :loading="currentLoading"
      row-key="id"
      flat
      dark
      class="premium-card"
      :pagination="{ rowsPerPage: 50 }"
    >
      <template #body-cell-actions="props">
        <q-td :props="props" auto-width>
          <q-btn flat round dense icon="edit" color="green-5" size="sm" @click="openEdit(props.row)">
            <q-tooltip>Edit</q-tooltip>
          </q-btn>
          <q-btn flat round dense icon="delete" color="red-5" size="sm" class="q-ml-xs" @click="confirmDelete(props.row)">
            <q-tooltip>Delete</q-tooltip>
          </q-btn>
        </q-td>
      </template>
      <template #no-data>
        <div class="full-width text-center q-pa-xl text-grey-5">
          <q-icon name="inbox" size="3rem" class="q-mb-sm" />
          <div>No {{ currentLabel.toLowerCase() }}s found</div>
        </div>
      </template>
    </q-table>

    <!-- Form Dialog -->
    <LookupFormDialog
      v-model="dialog.show"
      :item="dialog.item"
      :entity-label="dialog.entityLabel"
      :create-fn="dialog.createFn"
      :update-fn="dialog.updateFn"
      @saved="onSaved"
    />
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { lookupApi } from 'src/api/tasks'
import LookupFormDialog from 'src/components/LookupFormDialog.vue'

const $q = useQuasar()

const activeTab = ref('productTypes')

const productTypes = ref([])
const niches = ref([])
const occasions = ref([])
const loadingPT = ref(false)
const loadingNi = ref(false)
const loadingOc = ref(false)

const columns = [
  { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left', style: 'width: 60px' },
  { name: 'name', label: 'Name', field: 'name', sortable: true, align: 'left' },
  { name: 'description', label: 'Description', field: 'description', align: 'left' },
  { name: 'actions', label: 'Actions', field: 'actions', align: 'center' }
]

const dialog = ref({
  show: false,
  item: null,
  entityLabel: '',
  createFn: null,
  updateFn: null
})

const apiMap = {
  productTypes: {
    label: 'Product Type',
    load: lookupApi.getProductTypes,
    create: lookupApi.createProductType,
    update: lookupApi.updateProductType,
    delete: lookupApi.deleteProductType,
    data: productTypes,
    loading: loadingPT
  },
  niches: {
    label: 'Niche',
    load: lookupApi.getNiches,
    create: lookupApi.createNiche,
    update: lookupApi.updateNiche,
    delete: lookupApi.deleteNiche,
    data: niches,
    loading: loadingNi
  },
  occasions: {
    label: 'Occasion',
    load: lookupApi.getOccasions,
    create: lookupApi.createOccasion,
    update: lookupApi.updateOccasion,
    delete: lookupApi.deleteOccasion,
    data: occasions,
    loading: loadingOc
  }
}

const currentConfig = computed(() => apiMap[activeTab.value])
const currentItems = computed(() => currentConfig.value.data.value)
const currentLoading = computed(() => currentConfig.value.loading.value)
const currentLabel = computed(() => currentConfig.value.label)

const loadData = async (type) => {
  const cfg = apiMap[type]
  cfg.loading.value = true
  try {
    const res = await cfg.load()
    cfg.data.value = res.data.data || []
  } catch {
    $q.notify({ type: 'negative', message: `Failed to load ${cfg.label.toLowerCase()}s` })
  } finally {
    cfg.loading.value = false
  }
}

const openAdd = () => {
  const cfg = currentConfig.value
  dialog.value = {
    show: true,
    item: null,
    entityLabel: cfg.label,
    createFn: cfg.create,
    updateFn: cfg.update
  }
}

const openEdit = (item) => {
  const cfg = currentConfig.value
  dialog.value = {
    show: true,
    item: { ...item },
    entityLabel: cfg.label,
    createFn: cfg.create,
    updateFn: cfg.update
  }
}

const confirmDelete = (item) => {
  const cfg = currentConfig.value
  $q.dialog({
    title: `Delete ${cfg.label}`,
    message: `Are you sure you want to delete <strong>${item.name}</strong>?`,
    html: true,
    cancel: true,
    color: 'negative',
    dark: true
  }).onOk(async () => {
    try {
      await cfg.delete(item.id)
      $q.notify({ type: 'positive', message: `${cfg.label} deleted` })
      loadData(activeTab.value)
    } catch (err) {
      $q.notify({ type: 'negative', message: err.response?.data?.message || 'Delete failed' })
    }
  })
}

const onSaved = () => {
  dialog.value.show = false
  loadData(activeTab.value)
}

onMounted(() => {
  loadData('productTypes')
  loadData('niches')
  loadData('occasions')
})
</script>
