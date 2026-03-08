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
          <q-btn v-if="activeTab === 'userGroups'" flat round dense icon="people" color="blue-5" size="sm"
            @click="openMembers(props.row)">
            <q-tooltip>Manage Members</q-tooltip>
          </q-btn>
          <template v-if="!props.row.isDefault">
            <q-btn flat round dense icon="edit" color="green-5" size="sm" @click="openEdit(props.row)">
              <q-tooltip>Edit</q-tooltip>
            </q-btn>
            <q-btn flat round dense icon="delete" color="red-5" size="sm" @click="confirmDelete(props.row)">
              <q-tooltip>Delete</q-tooltip>
            </q-btn>
          </template>
          <span v-else-if="activeTab !== 'userGroups'" class="text-grey-7 text-caption">Read-only</span>
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

    <!-- Members Dialog -->
    <q-dialog v-model="membersDialog.show" persistent maximized transition-show="slide-up" transition-hide="slide-down">
      <q-card class="column" style="background: #1a1a2e">
        <!-- Header -->
        <q-bar class="bg-dark text-white">
          <q-icon name="groups" />
          <span class="q-ml-sm text-weight-medium">{{ membersDialog.group?.name }} — Members</span>
          <q-space />
          <q-btn dense flat icon="close" @click="membersDialog.show = false" />
        </q-bar>

        <q-card-section class="col q-pa-md" style="overflow: auto">
          <!-- Add member -->
          <div class="row items-center q-mb-md q-gutter-sm">
            <q-select
              v-model="membersDialog.selectedUser"
              :options="availableUsers"
              option-label="label"
              option-value="id"
              label="Add member"
              outlined dense
              use-input
              input-debounce="200"
              @filter="filterUsers"
              class="col"
              style="max-width: 400px"
              :loading="membersDialog.loadingUsers"
            >
              <template #option="{ opt, itemProps }">
                <q-item v-bind="itemProps">
                  <q-item-section avatar>
                    <UserAvatar :user="opt.user" size="28px" />
                  </q-item-section>
                  <q-item-section>
                    <q-item-label>{{ opt.label }}</q-item-label>
                    <q-item-label caption>{{ opt.user.userName }}</q-item-label>
                  </q-item-section>
                </q-item>
              </template>
              <template #no-option>
                <q-item><q-item-section class="text-grey">No users available</q-item-section></q-item>
              </template>
            </q-select>
            <q-btn icon="add" label="Add" color="primary" unelevated size="sm" :disable="!membersDialog.selectedUser"
              :loading="membersDialog.adding" @click="addMember" />
          </div>

          <!-- Members table -->
          <q-table
            :rows="membersDialog.members"
            :columns="memberColumns"
            :loading="membersDialog.loading"
            row-key="id"
            flat
            class="premium-card"
            :pagination="{ rowsPerPage: 50 }"
          >
            <template #body-cell-avatar="props">
              <q-td :props="props" auto-width>
                <UserAvatar :user="props.row" size="32px" />
              </q-td>
            </template>
            <template #body-cell-name="props">
              <q-td :props="props">
                {{ props.row.firstName }} {{ props.row.lastName }}
              </q-td>
            </template>
            <template #body-cell-actions="props">
              <q-td :props="props" auto-width>
                <q-btn flat round dense icon="person_remove" color="red-5" size="sm"
                  @click="confirmRemoveMember(props.row)">
                  <q-tooltip>Remove from group</q-tooltip>
                </q-btn>
              </q-td>
            </template>
            <template #no-data>
              <div class="full-width text-center q-pa-xl text-grey-5">
                <q-icon name="group_off" size="3rem" class="q-mb-sm" />
                <div>No members in this group</div>
              </div>
            </template>
          </q-table>
        </q-card-section>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { orgStructureApi } from 'src/api/organizations'
import { userApi } from 'src/api/users'
import LookupFormDialog from 'src/components/LookupFormDialog.vue'
import UserAvatar from 'src/components/UserAvatar.vue'

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

// ─── Members management ──────────────────────────────────────────
const membersDialog = ref({
  show: false,
  group: null,
  members: [],
  loading: false,
  allUsers: [],
  loadingUsers: false,
  selectedUser: null,
  adding: false
})

const memberColumns = [
  { name: 'avatar', label: '', field: 'avatarUrl', align: 'left', style: 'width: 48px' },
  { name: 'userName', label: 'Username', field: 'userName', sortable: true, align: 'left' },
  { name: 'name', label: 'Name', field: 'firstName', sortable: true, align: 'left' },
  { name: 'actions', label: '', field: 'actions', align: 'center' }
]

const availableUsers = ref([])

const openMembers = async (group) => {
  membersDialog.value = {
    show: true,
    group,
    members: [],
    loading: true,
    allUsers: [],
    loadingUsers: false,
    selectedUser: null,
    adding: false
  }
  await loadMembers()
  await loadAllUsers()
}

const loadMembers = async () => {
  membersDialog.value.loading = true
  try {
    const res = await orgStructureApi.getGroupMembers(membersDialog.value.group.id)
    membersDialog.value.members = res.data.data || []
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load members' })
  } finally {
    membersDialog.value.loading = false
  }
}

const loadAllUsers = async () => {
  membersDialog.value.loadingUsers = true
  try {
    const res = await userApi.getAll({ size: 1000, status: 'ACTIVE' })
    membersDialog.value.allUsers = (res.data.data?.content || [])
  } catch {
    // silent
  } finally {
    membersDialog.value.loadingUsers = false
  }
}

const filterUsers = (val, update) => {
  update(() => {
    const memberIds = new Set(membersDialog.value.members.map(m => m.id))
    const needle = (val || '').toLowerCase()
    availableUsers.value = membersDialog.value.allUsers
      .filter(u => !memberIds.has(u.id))
      .filter(u => !needle ||
        u.userName.toLowerCase().includes(needle) ||
        (u.firstName || '').toLowerCase().includes(needle) ||
        (u.lastName || '').toLowerCase().includes(needle)
      )
      .map(u => ({
        id: u.id,
        label: `${u.firstName || ''} ${u.lastName || ''}`.trim() || u.userName,
        user: u
      }))
  })
}

const addMember = async () => {
  if (!membersDialog.value.selectedUser) return
  membersDialog.value.adding = true
  try {
    await orgStructureApi.assignUserGroup(membersDialog.value.selectedUser.id, membersDialog.value.group.id)
    membersDialog.value.selectedUser = null
    await loadMembers()
    $q.notify({ type: 'positive', message: 'Member added' })
  } catch (err) {
    $q.notify({ type: 'negative', message: err.response?.data?.message || 'Failed to add member' })
  } finally {
    membersDialog.value.adding = false
  }
}

const confirmRemoveMember = (user) => {
  $q.dialog({
    title: 'Remove Member',
    message: `Remove <strong>${user.firstName} ${user.lastName}</strong> from <strong>${membersDialog.value.group.name}</strong>?`,
    html: true, cancel: true, color: 'negative'
  }).onOk(async () => {
    try {
      await orgStructureApi.removeUserGroup(user.id, membersDialog.value.group.id)
      await loadMembers()
      $q.notify({ type: 'positive', message: 'Member removed' })
    } catch (err) {
      $q.notify({ type: 'negative', message: err.response?.data?.message || 'Failed to remove member' })
    }
  })
}

watch(activeTab, () => {})

onMounted(() => {
  loadData('staffRoles')
  loadData('departments')
  loadData('userGroups')
})
</script>
