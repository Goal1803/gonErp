<template>
  <q-dialog v-model="show" persistent>
    <q-card style="min-width: 500px; max-width: 600px" class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon :name="isEdit ? 'edit' : 'person_add'" color="green-5" size="md" />
        <div>
          <div class="text-h6 text-white text-weight-medium">{{ isEdit ? 'Edit User' : 'Add New User' }}</div>
          <div class="text-caption text-grey-5">{{ isEdit ? `Editing: ${user?.userName}` : 'Create a new system user' }}</div>
        </div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pa-lg">
        <q-form @submit="handleSubmit" class="q-gutter-y-md">
          <!-- Avatar section -->
          <div class="flex items-center gap-3 q-mb-md" v-if="isEdit">
            <UserAvatar :user="avatarUser" size="64px" />
            <div>
              <q-btn flat dense color="teal-5" icon="upload" label="Upload photo" size="sm"
                @click="$refs.avatarInput.click()" :loading="uploadingAvatar" />
              <q-btn v-if="currentAvatarUrl" flat dense color="red-4" icon="delete" label="Remove" size="sm"
                class="q-ml-xs" @click="removeAvatar" />
              <input ref="avatarInput" type="file" accept="image/*" style="display:none"
                @change="onAvatarSelected" />
            </div>
          </div>

          <div class="row q-col-gutter-md">
            <!-- Username -->
            <div class="col-12">
              <q-input
                v-model="form.userName"
                label="Username *"
                outlined
                color="green-5"
                :rules="[v => !!v || 'Username is required']"
                lazy-rules
              />
            </div>

            <!-- First Name & Last Name -->
            <div class="col-6">
              <q-input
                v-model="form.firstName"
                label="First Name *"
                outlined
                color="green-5"
                :rules="[v => !!v || 'First name is required']"
                lazy-rules
              />
            </div>
            <div class="col-6">
              <q-input
                v-model="form.lastName"
                label="Last Name"
                outlined
                color="green-5"
              />
            </div>

            <!-- Date of Birth -->
            <div class="col-12">
              <q-input
                v-model="form.dateOfBirth"
                label="Date of Birth"
                outlined
                color="green-5"
                type="date"
              />
            </div>

            <!-- Role -->
            <div class="col-6">
              <q-select
                v-model="form.roleId"
                :options="roleOptions"
                label="Role *"
                outlined
                color="green-5"
                emit-value
                map-options
                :rules="[v => !!v || 'Role is required']"
                lazy-rules
              />
            </div>

            <!-- Status -->
            <div class="col-6">
              <q-select
                v-model="form.status"
                :options="statusOptions"
                label="Status"
                outlined
                color="green-5"
                emit-value
                map-options
              />
            </div>

            <!-- Designs Manager -->
            <div class="col-12">
              <q-toggle v-model="form.designsManager" label="Designs Manager" color="teal-5" />
              <div class="text-caption text-grey-6 q-ml-sm">Allow this user to see all designs in the Designs page</div>
            </div>

            <!-- Staff Roles Assignment -->
            <div class="col-12" v-if="staffRolesOptions.length">
              <q-select
                v-model="form.staffRoleIds"
                :options="staffRoleSelectOptions"
                label="Staff Roles"
                outlined color="green-5"
                multiple emit-value map-options
                use-chips
              />
            </div>

            <!-- Departments Assignment -->
            <div class="col-12" v-if="departmentsOptions.length">
              <q-select
                v-model="form.departmentIds"
                :options="departmentSelectOptions"
                label="Departments"
                outlined color="green-5"
                multiple emit-value map-options
                use-chips
              />
            </div>

            <!-- User Groups Assignment -->
            <div class="col-12" v-if="userGroupsOptions.length">
              <q-select
                v-model="form.userGroupIds"
                :options="userGroupSelectOptions"
                label="User Groups"
                outlined color="green-5"
                multiple emit-value map-options
                use-chips
              />
            </div>

            <!-- Password -->
            <div class="col-12">
              <q-input
                v-model="form.password"
                :label="isEdit ? 'New Password (leave blank to keep current)' : 'Password *'"
                outlined
                color="green-5"
                :type="showPassword ? 'text' : 'password'"
                :rules="isEdit ? [] : [v => !!v || 'Password is required']"
                lazy-rules
              >
                <template #append>
                  <q-icon
                    :name="showPassword ? 'visibility_off' : 'visibility'"
                    class="cursor-pointer text-grey-5"
                    @click="showPassword = !showPassword"
                  />
                </template>
              </q-input>
            </div>
          </div>
        </q-form>
      </q-card-section>

      <q-card-actions align="right" style="border-top: 1px solid rgba(46,125,50,0.2)" class="q-pa-md">
        <q-btn flat label="Cancel" color="grey-5" v-close-popup />
        <q-btn
          :label="isEdit ? 'Save Changes' : 'Create User'"
          color="primary"
          unelevated
          :loading="saving"
          @click="handleSubmit"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { userApi } from 'src/api/users'
import { orgStructureApi } from 'src/api/organizations'
import UserAvatar from 'src/components/UserAvatar.vue'

const props = defineProps({
  modelValue: Boolean,
  user: { type: Object, default: null },
  roles: { type: Array, default: () => [] },
  staffRolesOptions: { type: Array, default: () => [] },
  departmentsOptions: { type: Array, default: () => [] },
  userGroupsOptions: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const $q = useQuasar()

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const isEdit = computed(() => !!props.user)

const saving = ref(false)
const showPassword = ref(false)

const defaultForm = {
  userName: '',
  firstName: '',
  lastName: '',
  dateOfBirth: null,
  status: 'PENDING',
  password: '',
  roleId: null,
  designsManager: false,
  staffRoleIds: [],
  departmentIds: [],
  userGroupIds: []
}

const form = ref({ ...defaultForm })

const currentAvatarUrl = ref(null)
const uploadingAvatar = ref(false)

const avatarUser = computed(() => ({
  firstName: form.value.firstName,
  lastName: form.value.lastName,
  userName: form.value.userName,
  avatarUrl: currentAvatarUrl.value,
  id: props.user?.id
}))

const staffRoleSelectOptions = computed(() =>
  props.staffRolesOptions.map(r => ({ label: r.name, value: r.id }))
)

const departmentSelectOptions = computed(() =>
  props.departmentsOptions.map(d => ({ label: d.name, value: d.id }))
)

const userGroupSelectOptions = computed(() =>
  props.userGroupsOptions.map(g => ({ label: g.name, value: g.id }))
)

const onAvatarSelected = async (e) => {
  const file = e.target.files?.[0]
  if (!file || !props.user?.id) return
  uploadingAvatar.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await userApi.uploadAvatar(props.user.id, fd)
    currentAvatarUrl.value = res.data.data.avatarUrl
    $q.notify({ type: 'positive', message: 'Avatar uploaded' })
    emit('saved')
  } catch {
    $q.notify({ type: 'negative', message: 'Avatar upload failed' })
  } finally {
    uploadingAvatar.value = false
    e.target.value = ''
  }
}

const removeAvatar = async () => {
  if (!props.user?.id) return
  try {
    await userApi.deleteAvatar(props.user.id)
    currentAvatarUrl.value = null
    $q.notify({ type: 'positive', message: 'Avatar removed' })
    emit('saved')
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to remove avatar' })
  }
}

const statusOptions = [
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Deleted', value: 'DELETED' }
]

const roleOptions = computed(() =>
  props.roles.map(r => ({ label: r.name, value: r.id }))
)

// Helper to find IDs from names
const findIdsByNames = (names, options) => {
  if (!names || !options) return []
  return options
    .filter(o => names.includes(o.name))
    .map(o => o.id)
}

watch(() => props.user, (u) => {
  if (u) {
    form.value = {
      userName: u.userName || '',
      firstName: u.firstName || '',
      lastName: u.lastName || '',
      dateOfBirth: u.dateOfBirth || null,
      status: u.status || 'PENDING',
      password: '',
      roleId: u.role?.id || null,
      designsManager: u.designsManager || false,
      staffRoleIds: findIdsByNames(u.staffRoles, props.staffRolesOptions),
      departmentIds: findIdsByNames(u.departments, props.departmentsOptions),
      userGroupIds: findIdsByNames(u.userGroups, props.userGroupsOptions)
    }
    currentAvatarUrl.value = u.avatarUrl || null
  } else {
    form.value = { ...defaultForm, staffRoleIds: [], departmentIds: [], userGroupIds: [] }
    currentAvatarUrl.value = null
  }
}, { immediate: true })

const syncAssignments = async (userId) => {
  if (!props.staffRolesOptions.length) return

  const currentSR = findIdsByNames(props.user?.staffRoles, props.staffRolesOptions)
  const currentDept = findIdsByNames(props.user?.departments, props.departmentsOptions)
  const currentUG = findIdsByNames(props.user?.userGroups, props.userGroupsOptions)

  // Staff Roles
  const toAddSR = form.value.staffRoleIds.filter(id => !currentSR.includes(id))
  const toRemoveSR = currentSR.filter(id => !form.value.staffRoleIds.includes(id))
  for (const id of toAddSR) await orgStructureApi.assignStaffRole(userId, id)
  for (const id of toRemoveSR) await orgStructureApi.removeStaffRole(userId, id)

  // Departments
  const toAddDept = form.value.departmentIds.filter(id => !currentDept.includes(id))
  const toRemoveDept = currentDept.filter(id => !form.value.departmentIds.includes(id))
  for (const id of toAddDept) await orgStructureApi.assignDepartment(userId, id)
  for (const id of toRemoveDept) await orgStructureApi.removeDepartment(userId, id)

  // User Groups
  const toAddUG = form.value.userGroupIds.filter(id => !currentUG.includes(id))
  const toRemoveUG = currentUG.filter(id => !form.value.userGroupIds.includes(id))
  for (const id of toAddUG) await orgStructureApi.assignUserGroup(userId, id)
  for (const id of toRemoveUG) await orgStructureApi.removeUserGroup(userId, id)
}

const handleSubmit = async () => {
  saving.value = true
  try {
    const payload = {
      userName: form.value.userName,
      firstName: form.value.firstName,
      lastName: form.value.lastName,
      dateOfBirth: form.value.dateOfBirth,
      status: form.value.status,
      roleId: form.value.roleId,
      designsManager: form.value.designsManager
    }
    if (form.value.password) {
      payload.password = form.value.password
    }

    let userId
    if (isEdit.value) {
      await userApi.update(props.user.id, payload)
      userId = props.user.id
      $q.notify({ type: 'positive', message: 'User updated successfully' })
    } else {
      if (!payload.password) {
        $q.notify({ type: 'negative', message: 'Password is required' })
        saving.value = false
        return
      }
      const res = await userApi.create(payload)
      userId = res.data.data.id
      $q.notify({ type: 'positive', message: 'User created successfully' })
    }

    // Sync structure assignments
    await syncAssignments(userId)

    emit('saved')
  } catch (err) {
    const msg = err.response?.data?.message || 'Operation failed'
    $q.notify({ type: 'negative', message: msg })
  } finally {
    saving.value = false
  }
}
</script>
