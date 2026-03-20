<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/finance" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">Finance Configuration</div>
        <div class="text-caption text-grey-5">Assign finance roles to organization users</div>
      </div>
      <q-space />
      <q-btn unelevated color="green-7" icon="person_add" label="Assign Role" no-caps @click="showAssignDialog = true" />
    </div>

    <!-- Current Roles Table -->
    <q-table
      :rows="roles"
      :columns="columns"
      row-key="id"
      flat
      bordered
      :loading="loading"
      class="erp-table"
      :pagination="{ rowsPerPage: 20 }"
    >
      <template v-slot:body-cell-user="props">
        <q-td :props="props">
          <div class="row items-center no-wrap q-gutter-sm">
            <q-avatar size="28px" color="green-9" text-color="white">
              {{ (props.row.firstName || props.row.userName || '?')[0].toUpperCase() }}
            </q-avatar>
            <div>
              <div class="text-white text-weight-medium">{{ props.row.firstName }} {{ props.row.lastName }}</div>
              <div class="text-caption text-grey-5">{{ props.row.userName }}</div>
            </div>
          </div>
        </q-td>
      </template>
      <template v-slot:body-cell-role="props">
        <q-td :props="props">
          <q-badge :color="roleColor(props.row.financeRole)" :label="roleLabel(props.row.financeRole)" />
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn flat dense icon="delete" color="red-4" @click="confirmRemove(props.row)">
            <q-tooltip>Remove role</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <!-- Assign Dialog -->
    <q-dialog v-model="showAssignDialog">
      <q-card style="min-width: 400px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Assign Finance Role</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-select
            v-model="assignForm.userId"
            :options="availableUsers"
            option-value="id"
            option-label="label"
            emit-value
            map-options
            label="User"
            dense outlined dark
            use-input
            @filter="filterUsers"
          />
          <q-select
            v-model="assignForm.financeRole"
            :options="roleOptions"
            emit-value
            map-options
            label="Finance Role"
            dense outlined dark
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated label="Assign" color="green-7" no-caps @click="handleAssign" :loading="assigning" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { financeConfigApi } from 'src/api/finance'
import { api } from 'src/boot/axios'

const $q = useQuasar()

const roles = ref([])
const loading = ref(false)
const showAssignDialog = ref(false)
const assigning = ref(false)
const allUsers = ref([])
const availableUsers = ref([])

const assignForm = ref({ userId: null, financeRole: 'FINANCE_ACCOUNTANT' })

const roleOptions = [
  { label: 'CFO', value: 'FINANCE_CFO' },
  { label: 'Accountant Manager', value: 'FINANCE_ACCOUNTANT_MANAGER' },
  { label: 'Accountant', value: 'FINANCE_ACCOUNTANT' }
]

const columns = [
  { name: 'user', label: 'User', field: 'userName', align: 'left', sortable: true },
  { name: 'role', label: 'Finance Role', field: 'financeRole', align: 'center' },
  { name: 'createdBy', label: 'Assigned By', field: 'createdBy', align: 'left' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

function roleColor(role) {
  const map = { FINANCE_CFO: 'purple-7', FINANCE_ACCOUNTANT_MANAGER: 'blue-7', FINANCE_ACCOUNTANT: 'green-7' }
  return map[role] || 'grey-7'
}

function roleLabel(role) {
  const map = { FINANCE_CFO: 'CFO', FINANCE_ACCOUNTANT_MANAGER: 'Acc. Manager', FINANCE_ACCOUNTANT: 'Accountant' }
  return map[role] || role
}

async function loadRoles() {
  loading.value = true
  try {
    const res = await financeConfigApi.getRoles()
    roles.value = res.data.data
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  try {
    const res = await api.get('/users', { params: { size: 200 } })
    const users = res.data.data?.content || res.data.data || []
    allUsers.value = users.map(u => ({
      id: u.id,
      label: `${u.firstName || ''} ${u.lastName || ''} (${u.userName})`.trim()
    }))
    availableUsers.value = allUsers.value
  } catch { /* ignore */ }
}

function filterUsers(val, update) {
  update(() => {
    const needle = val.toLowerCase()
    availableUsers.value = allUsers.value.filter(u => u.label.toLowerCase().includes(needle))
  })
}

function confirmRemove(row) {
  $q.dialog({
    title: 'Remove Role',
    message: `Remove finance role from ${row.firstName} ${row.lastName}?`,
    cancel: true,
    color: 'red'
  }).onOk(async () => {
    try {
      await financeConfigApi.removeRole(row.userId)
      await loadRoles()
      $q.notify({ type: 'positive', message: 'Role removed' })
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' })
    }
  })
}

async function handleAssign() {
  if (!assignForm.value.userId) {
    $q.notify({ type: 'warning', message: 'Select a user' })
    return
  }
  assigning.value = true
  try {
    await financeConfigApi.assignRole(assignForm.value)
    showAssignDialog.value = false
    await loadRoles()
    $q.notify({ type: 'positive', message: 'Role assigned' })
    assignForm.value = { userId: null, financeRole: 'FINANCE_ACCOUNTANT' }
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' })
  } finally {
    assigning.value = false
  }
}

onMounted(() => {
  loadRoles()
  loadUsers()
})
</script>
