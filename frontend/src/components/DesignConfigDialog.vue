<template>
  <q-dialog v-model="show" maximized transition-show="slide-up" transition-hide="slide-down">
    <q-card dark class="column" style="background: #0d0d0d">
      <!-- Header -->
      <q-card-section class="row items-center q-px-lg" style="border-bottom: 1px solid rgba(255,255,255,0.07)">
        <q-icon name="settings" color="teal-5" size="sm" class="q-mr-sm" />
        <div class="text-h6 text-white text-weight-medium">Design Config</div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <!-- Tabs -->
      <q-tabs v-model="activeTab" dense active-color="teal-5" indicator-color="teal-5"
        align="left" class="text-grey-5 q-px-md" style="border-bottom: 1px solid rgba(255,255,255,0.07)">
        <q-tab v-if="authStore.isSuperAdmin" name="staffRoles" label="Staff Roles" />
        <q-tab v-if="authStore.isSuperAdmin" name="userGroups" label="User Groups" />
        <q-tab name="userAssignments" label="User Assignments" />
        <q-tab name="productTypes" label="Product Types" />
        <q-tab name="niches" label="Niches" />
        <q-tab name="occasions" label="Occasions" />
      </q-tabs>

      <!-- Tab panels -->
      <q-tab-panels v-model="activeTab" animated class="col bg-transparent text-white">

        <!-- Staff Roles Tab -->
        <q-tab-panel v-if="authStore.isSuperAdmin" name="staffRoles" class="q-pa-lg">
          <div class="row items-center q-mb-md">
            <div class="text-subtitle1 text-weight-medium">Design Staff Roles</div>
            <q-space />
            <q-btn color="teal-6" unelevated icon="add" label="Add Role" @click="openRoleForm(null)" />
          </div>
          <q-table :rows="staffRoles" :columns="lookupColumns" row-key="id" dark flat
            :loading="loadingRoles" no-data-label="No staff roles yet"
            class="premium-table" hide-pagination :rows-per-page-options="[0]">
            <template #body-cell-actions="props">
              <q-td :props="props" class="q-gutter-xs">
                <q-btn flat dense round icon="edit" color="teal-5" size="sm" @click="openRoleForm(props.row)" />
                <q-btn flat dense round icon="delete" color="red-4" size="sm" @click="confirmDeleteRole(props.row)" />
              </q-td>
            </template>
          </q-table>
        </q-tab-panel>

        <!-- User Groups Tab -->
        <q-tab-panel v-if="authStore.isSuperAdmin" name="userGroups" class="q-pa-lg">
          <div class="row items-center q-mb-md">
            <div class="text-subtitle1 text-weight-medium">Design User Groups</div>
            <q-space />
            <q-btn color="teal-6" unelevated icon="add" label="Add Group" @click="openGroupForm(null)" />
          </div>
          <q-table :rows="userGroups" :columns="lookupColumns" row-key="id" dark flat
            :loading="loadingGroups" no-data-label="No user groups yet"
            class="premium-table" hide-pagination :rows-per-page-options="[0]">
            <template #body-cell-actions="props">
              <q-td :props="props" class="q-gutter-xs">
                <q-btn flat dense round icon="edit" color="teal-5" size="sm" @click="openGroupForm(props.row)" />
                <q-btn flat dense round icon="delete" color="red-4" size="sm" @click="confirmDeleteGroup(props.row)" />
              </q-td>
            </template>
          </q-table>
        </q-tab-panel>

        <!-- User Assignments Tab -->
        <q-tab-panel name="userAssignments" class="q-pa-lg">
          <div class="row items-center q-mb-md">
            <div class="text-subtitle1 text-weight-medium">User Assignments</div>
            <q-space />
            <q-btn flat color="teal-5" icon="refresh" label="Refresh" @click="loadUsers" />
          </div>
          <q-table :rows="users" :columns="userColumns" row-key="userId" dark flat
            :loading="loadingUsers" no-data-label="No users found"
            class="premium-table" hide-pagination :rows-per-page-options="[0]">
            <template #body-cell-designStaffRoles="props">
              <q-td :props="props">
                <q-chip v-for="r in props.row.designStaffRoles" :key="r.id" dense removable size="sm"
                  color="teal-9" text-color="teal-2" @remove="doRemoveStaffRole(props.row.userId, r.id)">
                  {{ r.name }}
                </q-chip>
                <span v-if="!props.row.designStaffRoles?.length" class="text-grey-6 text-caption">None</span>
              </q-td>
            </template>
            <template #body-cell-designUserGroups="props">
              <q-td :props="props">
                <q-chip v-for="g in props.row.designUserGroups" :key="g.id" dense removable size="sm"
                  color="deep-purple-9" text-color="purple-2" @remove="doRemoveUserGroup(props.row.userId, g.id)">
                  {{ g.name }}
                </q-chip>
                <span v-if="!props.row.designUserGroups?.length" class="text-grey-6 text-caption">None</span>
              </q-td>
            </template>
            <template #body-cell-actions="props">
              <q-td :props="props">
                <q-btn flat dense round icon="edit" color="teal-5" size="sm" @click="openAssignDialog(props.row)" />
              </q-td>
            </template>
          </q-table>
        </q-tab-panel>

        <!-- Product Types Tab -->
        <q-tab-panel name="productTypes" class="q-pa-lg">
          <div class="row items-center q-mb-md">
            <div class="text-subtitle1 text-weight-medium">Product Types</div>
            <q-space />
            <q-btn color="teal-6" unelevated icon="add" label="Add Product Type" @click="openLookupForm('productTypes', null)" />
          </div>
          <q-table :rows="productTypes" :columns="lookupColumns" row-key="id" dark flat
            :loading="loadingProductTypes" no-data-label="No product types yet"
            class="premium-table" hide-pagination :rows-per-page-options="[0]">
            <template #body-cell-actions="props">
              <q-td :props="props" class="q-gutter-xs">
                <q-btn flat dense round icon="edit" color="teal-5" size="sm" @click="openLookupForm('productTypes', props.row)" />
                <q-btn flat dense round icon="delete" color="red-4" size="sm" @click="confirmDeleteLookup('productTypes', props.row)" />
              </q-td>
            </template>
          </q-table>
        </q-tab-panel>

        <!-- Niches Tab -->
        <q-tab-panel name="niches" class="q-pa-lg">
          <div class="row items-center q-mb-md">
            <div class="text-subtitle1 text-weight-medium">Niches</div>
            <q-space />
            <q-btn color="teal-6" unelevated icon="add" label="Add Niche" @click="openLookupForm('niches', null)" />
          </div>
          <q-table :rows="niches" :columns="lookupColumns" row-key="id" dark flat
            :loading="loadingNiches" no-data-label="No niches yet"
            class="premium-table" hide-pagination :rows-per-page-options="[0]">
            <template #body-cell-actions="props">
              <q-td :props="props" class="q-gutter-xs">
                <q-btn flat dense round icon="edit" color="teal-5" size="sm" @click="openLookupForm('niches', props.row)" />
                <q-btn flat dense round icon="delete" color="red-4" size="sm" @click="confirmDeleteLookup('niches', props.row)" />
              </q-td>
            </template>
          </q-table>
        </q-tab-panel>

        <!-- Occasions Tab -->
        <q-tab-panel name="occasions" class="q-pa-lg">
          <div class="row items-center q-mb-md">
            <div class="text-subtitle1 text-weight-medium">Occasions</div>
            <q-space />
            <q-btn color="teal-6" unelevated icon="add" label="Add Occasion" @click="openLookupForm('occasions', null)" />
          </div>
          <q-table :rows="occasions" :columns="lookupColumns" row-key="id" dark flat
            :loading="loadingOccasions" no-data-label="No occasions yet"
            class="premium-table" hide-pagination :rows-per-page-options="[0]">
            <template #body-cell-actions="props">
              <q-td :props="props" class="q-gutter-xs">
                <q-btn flat dense round icon="edit" color="teal-5" size="sm" @click="openLookupForm('occasions', props.row)" />
                <q-btn flat dense round icon="delete" color="red-4" size="sm" @click="confirmDeleteLookup('occasions', props.row)" />
              </q-td>
            </template>
          </q-table>
        </q-tab-panel>
      </q-tab-panels>
    </q-card>
  </q-dialog>

  <!-- Reuse LookupFormDialog for Staff Roles -->
  <lookup-form-dialog v-model="showRoleForm" :item="editingRole" entity-label="Design Staff Role"
    :create-fn="designConfigApi.createStaffRole" :update-fn="designConfigApi.updateStaffRole"
    @saved="onRoleSaved" />

  <!-- Reuse LookupFormDialog for User Groups -->
  <lookup-form-dialog v-model="showGroupForm" :item="editingGroup" entity-label="Design User Group"
    :create-fn="designConfigApi.createUserGroup" :update-fn="designConfigApi.updateUserGroup"
    @saved="onGroupSaved" />

  <!-- Reuse LookupFormDialog for Lookups (Product Types, Niches, Occasions) -->
  <lookup-form-dialog v-model="showLookupForm" :item="editingLookup" :entity-label="lookupFormLabel"
    :create-fn="lookupFormCreateFn" :update-fn="lookupFormUpdateFn"
    @saved="onLookupSaved" />

  <!-- Assignment Dialog -->
  <q-dialog v-model="showAssignDialog" persistent>
    <q-card dark style="min-width: 500px; max-width: 600px" class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(0,150,136,0.2)">
        <q-icon name="person" color="teal-5" size="md" />
        <div>
          <div class="text-h6 text-white text-weight-medium">Edit Assignments</div>
          <div class="text-caption text-grey-5">{{ assigningUser?.firstName }} {{ assigningUser?.lastName }} ({{ assigningUser?.userName }})</div>
        </div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pa-lg q-gutter-y-md">
        <q-select v-model="selectedRoleIds" :options="staffRoleOptions" multiple emit-value map-options
          outlined dark color="teal-5" label="Design Staff Roles" option-value="value" option-label="label"
          use-chips stack-label />
        <q-select v-model="selectedGroupIds" :options="userGroupOptions" multiple emit-value map-options
          outlined dark color="teal-5" label="Design User Groups" option-value="value" option-label="label"
          use-chips stack-label />
      </q-card-section>

      <q-card-actions align="right" style="border-top: 1px solid rgba(0,150,136,0.2)" class="q-pa-md">
        <q-btn flat label="Cancel" color="grey-5" v-close-popup />
        <q-btn label="Save" color="teal-6" unelevated :loading="savingAssignment" @click="saveAssignment" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { designConfigApi, lookupApi } from 'src/api/tasks'
import LookupFormDialog from 'src/components/LookupFormDialog.vue'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const $q = useQuasar()
const authStore = useAuthStore()

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const activeTab = ref(authStore.isSuperAdmin ? 'staffRoles' : 'userAssignments')

// ─── Staff Roles ─────────────────────────────────────────────────────
const staffRoles = ref([])
const loadingRoles = ref(false)
const showRoleForm = ref(false)
const editingRole = ref(null)

const loadStaffRoles = async () => {
  loadingRoles.value = true
  try {
    const res = await designConfigApi.getStaffRoles()
    staffRoles.value = res.data.data || []
  } catch { /* silent */ } finally { loadingRoles.value = false }
}

const openRoleForm = (item) => {
  editingRole.value = item
  showRoleForm.value = true
}

const onRoleSaved = () => {
  showRoleForm.value = false
  loadStaffRoles()
}

const confirmDeleteRole = (role) => {
  $q.dialog({
    title: 'Delete Staff Role',
    message: `Delete "${role.name}"? This cannot be undone.`,
    dark: true, cancel: true, persistent: true
  }).onOk(async () => {
    try {
      await designConfigApi.deleteStaffRole(role.id)
      $q.notify({ type: 'positive', message: 'Staff role deleted' })
      loadStaffRoles()
    } catch (err) {
      $q.notify({ type: 'negative', message: err.response?.data?.message || 'Delete failed' })
    }
  })
}

// ─── User Groups ─────────────────────────────────────────────────────
const userGroups = ref([])
const loadingGroups = ref(false)
const showGroupForm = ref(false)
const editingGroup = ref(null)

const loadUserGroups = async () => {
  loadingGroups.value = true
  try {
    const res = await designConfigApi.getUserGroups()
    userGroups.value = res.data.data || []
  } catch { /* silent */ } finally { loadingGroups.value = false }
}

const openGroupForm = (item) => {
  editingGroup.value = item
  showGroupForm.value = true
}

const onGroupSaved = () => {
  showGroupForm.value = false
  loadUserGroups()
}

const confirmDeleteGroup = (group) => {
  $q.dialog({
    title: 'Delete User Group',
    message: `Delete "${group.name}"? This cannot be undone.`,
    dark: true, cancel: true, persistent: true
  }).onOk(async () => {
    try {
      await designConfigApi.deleteUserGroup(group.id)
      $q.notify({ type: 'positive', message: 'User group deleted' })
      loadUserGroups()
    } catch (err) {
      $q.notify({ type: 'negative', message: err.response?.data?.message || 'Delete failed' })
    }
  })
}

// ─── User Assignments ────────────────────────────────────────────────
const users = ref([])
const loadingUsers = ref(false)
const showAssignDialog = ref(false)
const assigningUser = ref(null)
const selectedRoleIds = ref([])
const selectedGroupIds = ref([])
const savingAssignment = ref(false)

const staffRoleOptions = computed(() => staffRoles.value.map(r => ({ label: r.name, value: r.id })))
const userGroupOptions = computed(() => userGroups.value.map(g => ({ label: g.name, value: g.id })))

const loadUsers = async () => {
  loadingUsers.value = true
  try {
    const res = await designConfigApi.getUsers()
    users.value = res.data.data || []
  } catch { /* silent */ } finally { loadingUsers.value = false }
}

const openAssignDialog = (user) => {
  assigningUser.value = user
  selectedRoleIds.value = (user.designStaffRoles || []).map(r => r.id)
  selectedGroupIds.value = (user.designUserGroups || []).map(g => g.id)
  showAssignDialog.value = true
}

const saveAssignment = async () => {
  savingAssignment.value = true
  try {
    const userId = assigningUser.value.userId
    const currentRoleIds = (assigningUser.value.designStaffRoles || []).map(r => r.id)
    const currentGroupIds = (assigningUser.value.designUserGroups || []).map(g => g.id)

    // Diff roles
    const rolesToAdd = selectedRoleIds.value.filter(id => !currentRoleIds.includes(id))
    const rolesToRemove = currentRoleIds.filter(id => !selectedRoleIds.value.includes(id))
    // Diff groups
    const groupsToAdd = selectedGroupIds.value.filter(id => !currentGroupIds.includes(id))
    const groupsToRemove = currentGroupIds.filter(id => !selectedGroupIds.value.includes(id))

    await Promise.all([
      ...rolesToAdd.map(rid => designConfigApi.assignStaffRole(userId, rid)),
      ...rolesToRemove.map(rid => designConfigApi.removeStaffRole(userId, rid)),
      ...groupsToAdd.map(gid => designConfigApi.assignUserGroup(userId, gid)),
      ...groupsToRemove.map(gid => designConfigApi.removeUserGroup(userId, gid))
    ])

    $q.notify({ type: 'positive', message: 'Assignments updated' })
    showAssignDialog.value = false
    loadUsers()
  } catch (err) {
    $q.notify({ type: 'negative', message: err.response?.data?.message || 'Update failed' })
  } finally {
    savingAssignment.value = false
  }
}

const doRemoveStaffRole = async (userId, roleId) => {
  try {
    await designConfigApi.removeStaffRole(userId, roleId)
    $q.notify({ type: 'positive', message: 'Staff role removed' })
    loadUsers()
  } catch (err) {
    $q.notify({ type: 'negative', message: err.response?.data?.message || 'Remove failed' })
  }
}

const doRemoveUserGroup = async (userId, groupId) => {
  try {
    await designConfigApi.removeUserGroup(userId, groupId)
    $q.notify({ type: 'positive', message: 'User group removed' })
    loadUsers()
  } catch (err) {
    $q.notify({ type: 'negative', message: err.response?.data?.message || 'Remove failed' })
  }
}

// ─── Lookups (Product Types, Niches, Occasions) ─────────────────────
const productTypes = ref([])
const niches = ref([])
const occasions = ref([])
const loadingProductTypes = ref(false)
const loadingNiches = ref(false)
const loadingOccasions = ref(false)
const showLookupForm = ref(false)
const editingLookup = ref(null)
const lookupFormLabel = ref('')
const lookupFormCreateFn = ref(() => {})
const lookupFormUpdateFn = ref(() => {})
const activeLookupType = ref('')

const lookupApiMap = {
  productTypes: {
    label: 'Product Type',
    load: lookupApi.getProductTypes,
    create: lookupApi.createProductType,
    update: lookupApi.updateProductType,
    delete: lookupApi.deleteProductType,
    data: productTypes,
    loading: loadingProductTypes
  },
  niches: {
    label: 'Niche',
    load: lookupApi.getNiches,
    create: lookupApi.createNiche,
    update: lookupApi.updateNiche,
    delete: lookupApi.deleteNiche,
    data: niches,
    loading: loadingNiches
  },
  occasions: {
    label: 'Occasion',
    load: lookupApi.getOccasions,
    create: lookupApi.createOccasion,
    update: lookupApi.updateOccasion,
    delete: lookupApi.deleteOccasion,
    data: occasions,
    loading: loadingOccasions
  }
}

const loadLookupData = async (type) => {
  const cfg = lookupApiMap[type]
  cfg.loading.value = true
  try {
    const res = await cfg.load()
    cfg.data.value = res.data.data || []
  } catch { /* silent */ } finally { cfg.loading.value = false }
}

const openLookupForm = (type, item) => {
  const cfg = lookupApiMap[type]
  activeLookupType.value = type
  editingLookup.value = item ? { ...item } : null
  lookupFormLabel.value = cfg.label
  lookupFormCreateFn.value = cfg.create
  lookupFormUpdateFn.value = cfg.update
  showLookupForm.value = true
}

const onLookupSaved = () => {
  showLookupForm.value = false
  loadLookupData(activeLookupType.value)
}

const confirmDeleteLookup = (type, item) => {
  const cfg = lookupApiMap[type]
  $q.dialog({
    title: `Delete ${cfg.label}`,
    message: `Delete "${item.name}"? This cannot be undone.`,
    dark: true, cancel: true, persistent: true
  }).onOk(async () => {
    try {
      await cfg.delete(item.id)
      $q.notify({ type: 'positive', message: `${cfg.label} deleted` })
      loadLookupData(type)
    } catch (err) {
      $q.notify({ type: 'negative', message: err.response?.data?.message || 'Delete failed' })
    }
  })
}

// ─── Table columns ───────────────────────────────────────────────────
const lookupColumns = [
  { name: 'id', label: 'ID', field: 'id', align: 'left', style: 'width: 60px' },
  { name: 'name', label: 'Name', field: 'name', align: 'left' },
  { name: 'description', label: 'Description', field: 'description', align: 'left' },
  { name: 'actions', label: 'Actions', field: 'actions', align: 'center', style: 'width: 100px' }
]

const userColumns = [
  { name: 'userName', label: 'Username', field: 'userName', align: 'left' },
  { name: 'name', label: 'Name', field: row => `${row.firstName || ''} ${row.lastName || ''}`.trim(), align: 'left' },
  { name: 'designStaffRoles', label: 'Design Staff Roles', align: 'left' },
  { name: 'designUserGroups', label: 'Design User Groups', align: 'left' },
  { name: 'actions', label: 'Actions', align: 'center', style: 'width: 80px' }
]

// ─── Load data on dialog open ────────────────────────────────────────
watch(show, (val) => {
  if (val) {
    loadStaffRoles()
    loadUserGroups()
    loadUsers()
    loadLookupData('productTypes')
    loadLookupData('niches')
    loadLookupData('occasions')
  }
})
</script>
