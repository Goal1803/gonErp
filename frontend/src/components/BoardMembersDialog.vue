<template>
  <q-dialog v-model="show" persistent>
    <q-card style="min-width: 520px; max-width: 640px" class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon name="group" color="teal-5" size="md" />
        <div class="text-h6 text-white text-weight-medium">Board Members</div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pa-lg">
        <!-- Add member -->
        <div v-if="canManage" class="q-mb-lg">
          <div class="text-caption text-grey-5 q-mb-sm">Add Member</div>
          <div class="row q-gutter-sm items-center">
            <q-select
              v-model="selectedUser"
              :options="filteredUsers"
              option-value="id"
              :option-label="u => userDisplayName(u)"
              label="Search user..."
              outlined color="teal-5" dense class="col"
              emit-value map-options
              use-input input-debounce="200"
              @filter="onFilterUsers"
              :loading="loadingUsers"
            >
              <template #option="{ opt, itemProps }">
                <q-item v-bind="itemProps">
                  <q-item-section avatar>
                    <UserAvatar :user="opt" size="28px" />
                  </q-item-section>
                  <q-item-section>
                    <q-item-label>{{ opt.firstName }} {{ opt.lastName }}</q-item-label>
                    <q-item-label caption class="text-grey-5">@{{ opt.userName }}</q-item-label>
                  </q-item-section>
                </q-item>
              </template>
              <template #no-option>
                <q-item>
                  <q-item-section class="text-grey-5">No users found</q-item-section>
                </q-item>
              </template>
            </q-select>
            <q-select v-model="selectedRole" :options="['MEMBER','ADMIN']" label="Role"
              outlined color="teal-5" dense style="width:110px" />
            <q-btn icon="person_add" color="teal-6" unelevated dense @click="addMember" :loading="adding"
              :disable="!selectedUser" />
          </div>
        </div>

        <!-- Current members -->
        <div class="text-caption text-grey-5 q-mb-sm">Members ({{ members.length }})</div>
        <q-list dense separator>
          <q-item v-for="m in members" :key="m.id">
            <q-item-section avatar>
              <UserAvatar :user="m.user" size="32px" />
            </q-item-section>
            <q-item-section>
              <q-item-label class="text-white">{{ m.user.firstName }} {{ m.user.lastName }}</q-item-label>
              <q-item-label caption class="text-grey-5">@{{ m.user.userName }}</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-select
                v-if="canManage && m.role !== 'OWNER'"
                :model-value="m.role"
                :options="['MEMBER','ADMIN']"
                dense borderless color="teal-5"
                style="min-width:100px"
                @update:model-value="changeRole(m.user.id, $event)"
              />
              <q-chip v-else :color="roleColor(m.role)" text-color="white" dense size="sm">{{ m.role }}</q-chip>
            </q-item-section>
            <q-item-section side v-if="canManage && m.role !== 'OWNER'">
              <q-btn flat round dense icon="person_remove" color="red-4" size="xs"
                @click="confirmRemove(m)">
                <q-tooltip>Remove member</q-tooltip>
              </q-btn>
            </q-item-section>
          </q-item>
        </q-list>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { boardApi } from 'src/api/tasks'
import { userApi } from 'src/api/users'
import UserAvatar from 'src/components/UserAvatar.vue'

const props = defineProps({
  modelValue: Boolean,
  boardId: { type: Number, default: null },
  members: { type: Array, default: () => [] },
  canManage: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'updated'])
const $q = useQuasar()

const show = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const selectedUser = ref(null)
const selectedRole = ref('MEMBER')
const adding = ref(false)
const allUsers = ref([])
const filteredUsers = ref([])
const loadingUsers = ref(false)

const userDisplayName = (u) => {
  if (!u) return ''
  const name = [u.firstName, u.lastName].filter(Boolean).join(' ')
  return name ? `${name} (@${u.userName})` : `@${u.userName}`
}

const availableUsers = computed(() => {
  const memberIds = new Set(props.members.map(m => m.user.id))
  return allUsers.value.filter(u => !memberIds.has(u.id))
})

const fetchUsers = async () => {
  loadingUsers.value = true
  try {
    const res = await userApi.getAll({ size: 500 })
    allUsers.value = res.data.data?.content || res.data.data || []
  } catch { /* ignore */ }
  loadingUsers.value = false
}

watch(() => props.modelValue, (v) => {
  if (v) fetchUsers()
})

const onFilterUsers = (val, update) => {
  update(() => {
    if (!val) {
      filteredUsers.value = availableUsers.value
    } else {
      const q = val.toLowerCase()
      filteredUsers.value = availableUsers.value.filter(u =>
        u.userName?.toLowerCase().includes(q) ||
        u.firstName?.toLowerCase().includes(q) ||
        u.lastName?.toLowerCase().includes(q)
      )
    }
  })
}

const roleColor = (role) => ({ OWNER: 'orange-8', ADMIN: 'teal-7', MEMBER: 'grey-7' }[role] || 'grey-7')

const addMember = async () => {
  if (!selectedUser.value) return
  adding.value = true
  try {
    await boardApi.addMember(props.boardId, { userId: selectedUser.value, role: selectedRole.value })
    emit('updated')
    selectedUser.value = null
    $q.notify({ type: 'positive', message: 'Member added' })
  } catch (err) {
    $q.notify({ type: 'negative', message: err.response?.data?.message || 'Failed to add member' })
  } finally {
    adding.value = false
  }
}

const changeRole = async (userId, newRole) => {
  try {
    await boardApi.updateMemberRole(props.boardId, userId, { role: newRole })
    emit('updated')
    $q.notify({ type: 'positive', message: 'Role updated' })
  } catch (err) {
    $q.notify({ type: 'negative', message: err.response?.data?.message || 'Failed to update role' })
  }
}

const confirmRemove = (member) => {
  const name = [member.user.firstName, member.user.lastName].filter(Boolean).join(' ') || member.user.userName
  $q.dialog({
    title: 'Remove Member',
    message: `Remove "${name}" from this board? They will no longer have access.`,
    cancel: true,
    persistent: true,
    color: 'red-5'
  }).onOk(async () => {
    try {
      await boardApi.removeMember(props.boardId, member.user.id)
      emit('updated')
      $q.notify({ type: 'positive', message: 'Member removed' })
    } catch (err) {
      $q.notify({ type: 'negative', message: err.response?.data?.message || 'Failed to remove member' })
    }
  })
}
</script>
