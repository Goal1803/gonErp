<template>
  <q-page padding>
    <!-- Page Header -->
    <div class="page-header flex items-center justify-between">
      <div>
        <div class="text-h5 text-white text-weight-light">
          <q-icon name="group" color="green-5" class="q-mr-sm" />
          User Manager
        </div>
        <div class="text-caption text-grey-5 q-mt-xs">Manage system users and their roles</div>
      </div>
      <q-btn
        icon="person_add"
        label="Add User"
        color="primary"
        unelevated
        @click="openAddDialog"
      />
    </div>

    <!-- Filters Bar -->
    <q-card flat class="premium-card q-mb-md">
      <q-card-section class="row q-col-gutter-sm items-center">
        <div class="col-xs-12 col-sm-4">
          <q-input
            v-model="filter.search"
            placeholder="Search by username, first/last name..."
            outlined
            dense
            dark
            color="green-5"
            clearable
            debounce="400"
            @update:model-value="loadUsers"
          >
            <template #prepend>
              <q-icon name="search" color="grey-5" />
            </template>
          </q-input>
        </div>
        <div class="col-xs-12 col-sm-3">
          <q-select
            v-model="filter.status"
            :options="statusOptions"
            label="Filter by Status"
            outlined
            dense
            dark
            color="green-5"
            clearable
            emit-value
            map-options
            @update:model-value="loadUsers"
          />
        </div>
        <div class="col-auto">
          <q-btn flat icon="refresh" color="green-5" @click="loadUsers" round dense>
            <q-tooltip>Refresh</q-tooltip>
          </q-btn>
        </div>
        <div class="col-auto q-ml-auto">
          <span class="text-grey-5 text-caption">{{ pagination.rowsNumber }} user(s) found</span>
        </div>
      </q-card-section>
    </q-card>

    <!-- Users Table -->
    <q-table
      :rows="users"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      flat
      dark
      class="premium-card"
      @request="onRequest"
    >
      <template #body-cell-status="props">
        <q-td :props="props">
          <span
            class="q-px-sm q-py-xs rounded-borders text-caption text-weight-medium"
            :class="{
              'status-active': props.value === 'ACTIVE',
              'status-pending': props.value === 'PENDING',
              'status-deleted': props.value === 'DELETED'
            }"
          >
            {{ props.value }}
          </span>
        </q-td>
      </template>

      <template #body-cell-role="props">
        <q-td :props="props">
          <q-chip
            dense
            square
            :label="props.row.role?.name"
            :color="props.row.role?.name === 'ADMIN' ? 'green-9' : 'grey-9'"
            text-color="white"
            size="sm"
          />
        </q-td>
      </template>

      <template #body-cell-dateOfBirth="props">
        <q-td :props="props">
          {{ props.value ? formatDate(props.value) : '-' }}
        </q-td>
      </template>

      <template #body-cell-actions="props">
        <q-td :props="props" auto-width>
          <q-btn
            flat
            round
            dense
            icon="edit"
            color="green-5"
            size="sm"
            @click="openEditDialog(props.row)"
          >
            <q-tooltip>Edit User</q-tooltip>
          </q-btn>
          <q-btn
            flat
            round
            dense
            icon="delete"
            color="red-5"
            size="sm"
            :disable="props.row.status === 'DELETED'"
            @click="confirmDelete(props.row)"
          >
            <q-tooltip>{{ props.row.status === 'DELETED' ? 'Already deleted' : 'Delete User' }}</q-tooltip>
          </q-btn>
        </q-td>
      </template>

      <template #no-data>
        <div class="full-width text-center q-pa-xl text-grey-5">
          <q-icon name="person_off" size="3rem" class="q-mb-sm" />
          <div>No users found</div>
        </div>
      </template>
    </q-table>

    <!-- Add/Edit Dialog -->
    <UserFormDialog
      v-model="dialog.show"
      :user="dialog.user"
      :roles="roles"
      @saved="onUserSaved"
    />
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { userApi, userRoleApi } from 'src/api/users'
import UserFormDialog from 'src/components/UserFormDialog.vue'

const $q = useQuasar()

const users = ref([])
const roles = ref([])
const loading = ref(false)

const filter = ref({
  search: '',
  status: null
})

const pagination = ref({
  sortBy: 'id',
  descending: false,
  page: 1,
  rowsPerPage: 20,
  rowsNumber: 0
})

const statusOptions = [
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Deleted', value: 'DELETED' }
]

const columns = [
  { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left', style: 'width: 60px' },
  { name: 'userName', label: 'Username', field: 'userName', sortable: true, align: 'left' },
  { name: 'firstName', label: 'First Name', field: 'firstName', sortable: true, align: 'left' },
  { name: 'lastName', label: 'Last Name', field: 'lastName', sortable: true, align: 'left' },
  { name: 'dateOfBirth', label: 'Date of Birth', field: 'dateOfBirth', align: 'left' },
  { name: 'role', label: 'Role', field: 'role', align: 'left' },
  { name: 'status', label: 'Status', field: 'status', sortable: true, align: 'left' },
  { name: 'actions', label: 'Actions', field: 'actions', align: 'center' }
]

const dialog = ref({ show: false, user: null })

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('en-GB')
}

const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page - 1,
      size: pagination.value.rowsPerPage,
      sortBy: pagination.value.sortBy || 'id',
      sortDir: pagination.value.descending ? 'desc' : 'asc'
    }
    if (filter.value.status) params.status = filter.value.status
    if (filter.value.search) params.search = filter.value.search

    const res = await userApi.getAll(params)
    const data = res.data.data
    users.value = data.content
    pagination.value.rowsNumber = data.totalElements
  } catch (err) {
    $q.notify({ type: 'negative', message: 'Failed to load users' })
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  try {
    const res = await userRoleApi.getAll()
    roles.value = res.data.data
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load roles' })
  }
}

const onRequest = (props) => {
  pagination.value = props.pagination
  loadUsers()
}

const openAddDialog = () => {
  dialog.value = { show: true, user: null }
}

const openEditDialog = (user) => {
  dialog.value = { show: true, user: { ...user } }
}

const confirmDelete = (user) => {
  $q.dialog({
    title: 'Delete User',
    message: `Are you sure you want to delete <strong>${user.userName}</strong>? This will change their status to DELETED.`,
    html: true,
    cancel: true,
    color: 'negative',
    dark: true
  }).onOk(async () => {
    try {
      await userApi.delete(user.id)
      $q.notify({ type: 'positive', message: 'User deleted successfully' })
      loadUsers()
    } catch (err) {
      $q.notify({ type: 'negative', message: err.response?.data?.message || 'Failed to delete user' })
    }
  })
}

const onUserSaved = () => {
  dialog.value.show = false
  loadUsers()
}

onMounted(() => {
  loadUsers()
  loadRoles()
})
</script>
