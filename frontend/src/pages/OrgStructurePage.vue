<template>
  <q-page padding>
    <!-- Page Header -->
    <div class="page-header flex items-center justify-between">
      <div>
        <div class="text-h5 text-white text-weight-light">
          <q-icon name="account_tree" color="green-5" class="q-mr-sm" />
          Organization Structure
        </div>
        <div class="text-caption text-grey-5 q-mt-xs">Manage staff roles, departments, and user groups</div>
      </div>
    </div>

    <!-- Tabs -->
    <q-tabs v-model="activeTab" dense active-color="teal-5" indicator-color="teal-5"
      class="text-grey-5 q-mb-md" align="left" narrow-indicator>
      <q-tab name="staffRoles" label="Staff Roles" icon="badge" />
      <q-tab name="departments" label="Departments" icon="business" />
      <q-tab name="userGroups" label="User Groups" icon="groups" />
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
      class="premium-card"
      :pagination="{ rowsPerPage: 50 }"
    >
      <template #body-cell-name="props">
        <q-td :props="props">
          <span>{{ props.value }}</span>
          <q-icon v-if="props.row.isDefault" name="lock" size="xs" color="grey-6" class="q-ml-xs">
            <q-tooltip>Default (inherited from org type)</q-tooltip>
          </q-icon>
        </q-td>
      </template>

      <template #body-cell-type="props">
        <q-td :props="props">
          <q-chip
            dense square size="sm"
            :color="props.row.isDefault ? 'grey-8' : 'teal-9'"
            text-color="white"
            :label="props.row.isDefault ? 'Default' : 'Custom'"
          />
        </q-td>
      </template>

      <template #body-cell-actions="props">
        <q-td :props="props" auto-width>
          <template v-if="!props.row.isDefault">
            <q-btn flat round dense icon="edit" color="green-5" size="sm" @click="openEdit(props.row)">
              <q-tooltip>Edit</q-tooltip>
            </q-btn>
            <q-btn flat round dense icon="delete" color="red-5" size="sm" @click="confirmDelete(props.row)">
              <q-tooltip>Delete</q-tooltip>
            </q-btn>
          </template>
          <span v-else class="text-grey-7 text-caption">Read-only</span>
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
      :entity-label="currentLabel"
      :create-fn="dialog.createFn"
      :update-fn="dialog.updateFn"
      @saved="onSaved"
    />
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { orgStructureApi } from 'src/api/organizations'
import LookupFormDialog from 'src/components/LookupFormDialog.vue'

const $q = useQuasar()

const activeTab = ref('staffRoles')

const staffRoles = ref([])
const departments = ref([])
const userGroups = ref([])
const loadingSR = ref(false)
const loadingDept = ref(false)
const loadingUG = ref(false)

const columns = [
  { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left', style: 'width: 60px' },
  { name: 'name', label: 'Name', field: 'name', sortable: true, align: 'left' },
  { name: 'description', label: 'Description', field: 'description', align: 'left' },
  { name: 'type', label: 'Type', field: 'isDefault', align: 'left' },
  { name: 'actions', label: 'Actions', field: 'actions', align: 'center' }
]

const dialog = ref({
  show: false,
  item: null,
  createFn: null,
  updateFn: null
})

const apiMap = {
  staffRoles: {
    label: 'Staff Role',
    load: orgStructureApi.getStaffRoles,
    create: orgStructureApi.createStaffRole,
    update: orgStructureApi.updateStaffRole,
    delete: orgStructureApi.deleteStaffRole,
    data: staffRoles,
    loading: loadingSR
  },
  departments: {
    label: 'Department',
    load: orgStructureApi.getDepartments,
    create: orgStructureApi.createDepartment,
    update: orgStructureApi.updateDepartment,
    delete: orgStructureApi.deleteDepartment,
    data: departments,
    loading: loadingDept
  },
  userGroups: {
    label: 'User Group',
    load: orgStructureApi.getUserGroups,
    create: orgStructureApi.createUserGroup,
    update: orgStructureApi.updateUserGroup,
    delete: orgStructureApi.deleteUserGroup,
    data: userGroups,
    loading: loadingUG
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
    createFn: cfg.create,
    updateFn: cfg.update
  }
}

const openEdit = (item) => {
  const cfg = currentConfig.value
  dialog.value = {
    show: true,
    item: { ...item },
    createFn: cfg.create,
    updateFn: cfg.update
  }
}

const confirmDelete = (item) => {
  const cfg = currentConfig.value
  $q.dialog({
    title: `Delete ${cfg.label}`,
    message: `Delete <strong>${item.name}</strong>?`,
    html: true, cancel: true, color: 'negative'
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

watch(activeTab, () => {})

onMounted(() => {
  loadData('staffRoles')
  loadData('departments')
  loadData('userGroups')
})
</script>
