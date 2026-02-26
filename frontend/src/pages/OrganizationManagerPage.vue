<template>
  <q-page padding>
    <!-- Page Header -->
    <div class="page-header flex items-center justify-between">
      <div>
        <div class="text-h5 text-white text-weight-light">
          <q-icon name="corporate_fare" color="green-5" class="q-mr-sm" />
          Organizations
        </div>
        <div class="text-caption text-grey-5 q-mt-xs">Manage tenant organizations</div>
      </div>
      <q-btn icon="add" label="Add Organization" color="primary" unelevated @click="openAdd" />
    </div>

    <!-- Table -->
    <q-table
      :rows="organizations"
      :columns="columns"
      :loading="loading"
      row-key="id"
      flat
      class="premium-card"
      :pagination="{ rowsPerPage: 20 }"
    >
      <template #body-cell-active="props">
        <q-td :props="props">
          <q-chip
            dense square size="sm"
            :color="props.value ? 'green-9' : 'red-9'"
            text-color="white"
            :label="props.value ? 'Active' : 'Inactive'"
          />
        </q-td>
      </template>

      <template #body-cell-modules="props">
        <q-td :props="props">
          <q-chip v-if="props.row.moduleTaskManager" dense size="sm" color="teal-9" text-color="white" label="Tasks" />
          <q-chip v-if="props.row.moduleImageManager" dense size="sm" color="blue-9" text-color="white" label="Images" />
          <q-chip v-if="props.row.moduleDesigns" dense size="sm" color="purple-9" text-color="white" label="Designs" />
        </q-td>
      </template>

      <template #body-cell-actions="props">
        <q-td :props="props" auto-width>
          <q-btn flat round dense icon="edit" color="green-5" size="sm" @click="openEdit(props.row)">
            <q-tooltip>Edit</q-tooltip>
          </q-btn>
          <q-btn
            flat round dense size="sm"
            :icon="props.row.active ? 'block' : 'check_circle'"
            :color="props.row.active ? 'orange-5' : 'green-5'"
            @click="toggleActive(props.row)"
          >
            <q-tooltip>{{ props.row.active ? 'Deactivate' : 'Activate' }}</q-tooltip>
          </q-btn>
        </q-td>
      </template>

      <template #no-data>
        <div class="full-width text-center q-pa-xl text-grey-5">
          <q-icon name="corporate_fare" size="3rem" class="q-mb-sm" />
          <div>No organizations found</div>
        </div>
      </template>
    </q-table>

    <!-- Form Dialog -->
    <OrgFormDialog
      v-model="dialog.show"
      :org="dialog.org"
      :org-types="orgTypes"
      @saved="onSaved"
    />
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { orgApi, orgTypeApi } from 'src/api/organizations'
import OrgFormDialog from 'src/components/OrgFormDialog.vue'

const $q = useQuasar()

const organizations = ref([])
const orgTypes = ref([])
const loading = ref(false)

const columns = [
  { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left', style: 'width: 60px' },
  { name: 'name', label: 'Name', field: 'name', sortable: true, align: 'left' },
  { name: 'slug', label: 'Slug', field: 'slug', align: 'left' },
  { name: 'orgTypeName', label: 'Type', field: 'orgTypeName', align: 'left' },
  { name: 'active', label: 'Active', field: 'active', align: 'left' },
  { name: 'modules', label: 'Modules', field: 'modules', align: 'left' },
  { name: 'userCount', label: 'Users', field: 'userCount', sortable: true, align: 'left' },
  { name: 'actions', label: 'Actions', field: 'actions', align: 'center' }
]

const dialog = ref({ show: false, org: null })

const loadOrganizations = async () => {
  loading.value = true
  try {
    const res = await orgApi.getAll()
    organizations.value = res.data.data || []
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load organizations' })
  } finally {
    loading.value = false
  }
}

const loadOrgTypes = async () => {
  try {
    const res = await orgTypeApi.getAll()
    orgTypes.value = res.data.data || []
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load org types' })
  }
}

const openAdd = () => {
  dialog.value = { show: true, org: null }
}

const openEdit = (org) => {
  dialog.value = { show: true, org: { ...org } }
}

const toggleActive = async (org) => {
  try {
    await orgApi.toggleActive(org.id)
    $q.notify({ type: 'positive', message: `Organization ${org.active ? 'deactivated' : 'activated'}` })
    loadOrganizations()
  } catch (err) {
    $q.notify({ type: 'negative', message: err.response?.data?.message || 'Operation failed' })
  }
}

const onSaved = () => {
  dialog.value.show = false
  loadOrganizations()
}

onMounted(() => {
  loadOrganizations()
  loadOrgTypes()
})
</script>
