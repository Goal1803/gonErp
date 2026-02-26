<template>
  <q-page padding>
    <!-- Page Header -->
    <div class="page-header flex items-center justify-between">
      <div>
        <div class="text-h5 text-white text-weight-light">
          <q-icon name="account_tree" color="green-5" class="q-mr-sm" />
          Organization Types
        </div>
        <div class="text-caption text-grey-5 q-mt-xs">Manage org types and their default structure</div>
      </div>
      <q-btn icon="add" label="Add Type" color="primary" unelevated @click="openAddType" />
    </div>

    <!-- Org Types Table -->
    <q-table
      :rows="orgTypes"
      :columns="typeColumns"
      :loading="loadingTypes"
      row-key="id"
      flat
      class="premium-card q-mb-lg"
      :pagination="{ rowsPerPage: 20 }"
    >
      <template #body-cell-name="props">
        <q-td :props="props">
          <a class="text-green-4 cursor-pointer" @click="selectType(props.row)">{{ props.value }}</a>
        </q-td>
      </template>

      <template #body-cell-actions="props">
        <q-td :props="props" auto-width>
          <q-btn flat round dense icon="edit" color="green-5" size="sm" @click="openEditType(props.row)">
            <q-tooltip>Edit</q-tooltip>
          </q-btn>
          <q-btn flat round dense icon="delete" color="red-5" size="sm" @click="confirmDeleteType(props.row)">
            <q-tooltip>Delete</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <!-- Defaults Section (when a type is selected) -->
    <template v-if="selectedType">
      <div class="text-h6 text-white text-weight-light q-mb-md">
        Defaults for: <span class="text-green-4">{{ selectedType.name }}</span>
      </div>

      <q-tabs v-model="defaultsTab" dense active-color="teal-5" indicator-color="teal-5"
        class="text-grey-5 q-mb-md" align="left" narrow-indicator>
        <q-tab name="staffRoles" label="Staff Roles" icon="badge" />
        <q-tab name="departments" label="Departments" icon="business" />
        <q-tab name="userGroups" label="User Groups" icon="groups" />
      </q-tabs>

      <div class="flex items-center justify-between q-mb-md">
        <span class="text-grey-5 text-caption">{{ currentDefaults.length }} item(s)</span>
        <q-btn icon="add" :label="'Add ' + defaultsTabLabel" color="primary" unelevated size="sm" @click="openAddDefault" />
      </div>

      <q-table
        :rows="currentDefaults"
        :columns="defaultColumns"
        :loading="loadingDefaults"
        row-key="id"
        flat
        class="premium-card"
        :pagination="{ rowsPerPage: 50 }"
      >
        <template #body-cell-actions="props">
          <q-td :props="props" auto-width>
            <q-btn flat round dense icon="delete" color="red-5" size="sm" @click="confirmDeleteDefault(props.row)">
              <q-tooltip>Delete</q-tooltip>
            </q-btn>
          </q-td>
        </template>
        <template #no-data>
          <div class="full-width text-center q-pa-xl text-grey-5">
            <q-icon name="inbox" size="3rem" class="q-mb-sm" />
            <div>No {{ defaultsTabLabel.toLowerCase() }}s found</div>
          </div>
        </template>
      </q-table>
    </template>

    <!-- Org Type Form Dialog -->
    <LookupFormDialog
      v-model="typeDialog.show"
      :item="typeDialog.item"
      entity-label="Organization Type"
      :create-fn="orgTypeApi.create"
      :update-fn="orgTypeApi.update"
      @saved="onTypeSaved"
    />

    <!-- Default Item Form Dialog -->
    <LookupFormDialog
      v-model="defaultDialog.show"
      :item="null"
      :entity-label="defaultsTabLabel"
      :create-fn="defaultCreateFn"
      :update-fn="() => {}"
      @saved="onDefaultSaved"
    />
  </q-page>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { orgTypeApi } from 'src/api/organizations'
import LookupFormDialog from 'src/components/LookupFormDialog.vue'

const $q = useQuasar()

const orgTypes = ref([])
const loadingTypes = ref(false)
const selectedType = ref(null)
const defaultsTab = ref('staffRoles')

const staffRoles = ref([])
const departments = ref([])
const userGroups = ref([])
const loadingDefaults = ref(false)

const typeColumns = [
  { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left', style: 'width: 60px' },
  { name: 'name', label: 'Name', field: 'name', sortable: true, align: 'left' },
  { name: 'description', label: 'Description', field: 'description', align: 'left' },
  { name: 'staffRoleCount', label: 'Staff Roles', field: 'staffRoleCount', align: 'left' },
  { name: 'departmentCount', label: 'Departments', field: 'departmentCount', align: 'left' },
  { name: 'userGroupCount', label: 'User Groups', field: 'userGroupCount', align: 'left' },
  { name: 'actions', label: 'Actions', field: 'actions', align: 'center' }
]

const defaultColumns = [
  { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left', style: 'width: 60px' },
  { name: 'name', label: 'Name', field: 'name', sortable: true, align: 'left' },
  { name: 'description', label: 'Description', field: 'description', align: 'left' },
  { name: 'actions', label: 'Actions', field: 'actions', align: 'center' }
]

const typeDialog = ref({ show: false, item: null })
const defaultDialog = ref({ show: false })

const currentDefaults = computed(() => {
  if (defaultsTab.value === 'staffRoles') return staffRoles.value
  if (defaultsTab.value === 'departments') return departments.value
  return userGroups.value
})

const defaultsTabLabel = computed(() => {
  if (defaultsTab.value === 'staffRoles') return 'Staff Role'
  if (defaultsTab.value === 'departments') return 'Department'
  return 'User Group'
})

const defaultCreateFn = computed(() => {
  if (!selectedType.value) return () => {}
  const typeId = selectedType.value.id
  if (defaultsTab.value === 'staffRoles') return (data) => orgTypeApi.createStaffRole(typeId, data)
  if (defaultsTab.value === 'departments') return (data) => orgTypeApi.createDepartment(typeId, data)
  return (data) => orgTypeApi.createUserGroup(typeId, data)
})

const loadOrgTypes = async () => {
  loadingTypes.value = true
  try {
    const res = await orgTypeApi.getAll()
    orgTypes.value = res.data.data || []
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load org types' })
  } finally {
    loadingTypes.value = false
  }
}

const selectType = (type) => {
  selectedType.value = type
  loadDefaults()
}

const loadDefaults = async () => {
  if (!selectedType.value) return
  loadingDefaults.value = true
  const typeId = selectedType.value.id
  try {
    const [sr, dept, ug] = await Promise.all([
      orgTypeApi.getStaffRoles(typeId),
      orgTypeApi.getDepartments(typeId),
      orgTypeApi.getUserGroups(typeId)
    ])
    staffRoles.value = sr.data.data || []
    departments.value = dept.data.data || []
    userGroups.value = ug.data.data || []
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load defaults' })
  } finally {
    loadingDefaults.value = false
  }
}

const openAddType = () => {
  typeDialog.value = { show: true, item: null }
}

const openEditType = (type) => {
  typeDialog.value = { show: true, item: { ...type } }
}

const confirmDeleteType = (type) => {
  $q.dialog({
    title: 'Delete Organization Type',
    message: `Delete <strong>${type.name}</strong>?`,
    html: true, cancel: true, color: 'negative'
  }).onOk(async () => {
    try {
      await orgTypeApi.delete(type.id)
      $q.notify({ type: 'positive', message: 'Org type deleted' })
      if (selectedType.value?.id === type.id) selectedType.value = null
      loadOrgTypes()
    } catch (err) {
      $q.notify({ type: 'negative', message: err.response?.data?.message || 'Delete failed' })
    }
  })
}

const openAddDefault = () => {
  defaultDialog.value = { show: true }
}

const confirmDeleteDefault = (item) => {
  const label = defaultsTabLabel.value
  $q.dialog({
    title: `Delete ${label}`,
    message: `Delete <strong>${item.name}</strong>?`,
    html: true, cancel: true, color: 'negative'
  }).onOk(async () => {
    try {
      if (defaultsTab.value === 'staffRoles') await orgTypeApi.deleteStaffRole(item.id)
      else if (defaultsTab.value === 'departments') await orgTypeApi.deleteDepartment(item.id)
      else await orgTypeApi.deleteUserGroup(item.id)
      $q.notify({ type: 'positive', message: `${label} deleted` })
      loadDefaults()
      loadOrgTypes()
    } catch (err) {
      $q.notify({ type: 'negative', message: err.response?.data?.message || 'Delete failed' })
    }
  })
}

const onTypeSaved = () => {
  typeDialog.value.show = false
  loadOrgTypes()
}

const onDefaultSaved = () => {
  defaultDialog.value.show = false
  loadDefaults()
  loadOrgTypes()
}

watch(defaultsTab, () => {})

onMounted(() => {
  loadOrgTypes()
})
</script>
