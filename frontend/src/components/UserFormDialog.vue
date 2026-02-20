<template>
  <q-dialog v-model="show" persistent>
    <q-card style="min-width: 500px; max-width: 600px" dark class="premium-card">
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
                dark
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
                dark
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
                dark
                color="green-5"
              />
            </div>

            <!-- Date of Birth -->
            <div class="col-12">
              <q-input
                v-model="form.dateOfBirth"
                label="Date of Birth"
                outlined
                dark
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
                dark
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
                dark
                color="green-5"
                emit-value
                map-options
              />
            </div>

            <!-- Password -->
            <div class="col-12">
              <q-input
                v-model="form.password"
                :label="isEdit ? 'New Password (leave blank to keep current)' : 'Password *'"
                outlined
                dark
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
import UserAvatar from 'src/components/UserAvatar.vue'

const props = defineProps({
  modelValue: Boolean,
  user: { type: Object, default: null },
  roles: { type: Array, default: () => [] }
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
  roleId: null
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

watch(() => props.user, (u) => {
  if (u) {
    form.value = {
      userName: u.userName || '',
      firstName: u.firstName || '',
      lastName: u.lastName || '',
      dateOfBirth: u.dateOfBirth || null,
      status: u.status || 'PENDING',
      password: '',
      roleId: u.role?.id || null
    }
    currentAvatarUrl.value = u.avatarUrl || null
  } else {
    form.value = { ...defaultForm }
    currentAvatarUrl.value = null
  }
}, { immediate: true })

const handleSubmit = async () => {
  saving.value = true
  try {
    const payload = { ...form.value }
    if (isEdit.value && !payload.password) {
      delete payload.password
    }

    if (isEdit.value) {
      await userApi.update(props.user.id, payload)
      $q.notify({ type: 'positive', message: 'User updated successfully' })
    } else {
      await userApi.create(payload)
      $q.notify({ type: 'positive', message: 'User created successfully' })
    }
    emit('saved')
  } catch (err) {
    const msg = err.response?.data?.message || 'Operation failed'
    $q.notify({ type: 'negative', message: msg })
  } finally {
    saving.value = false
  }
}
</script>
