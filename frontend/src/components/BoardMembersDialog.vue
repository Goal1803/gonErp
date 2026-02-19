<template>
  <q-dialog v-model="show" persistent>
    <q-card style="min-width: 500px; max-width: 600px" dark class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon name="group" color="teal-5" size="md" />
        <div class="text-h6 text-white text-weight-medium">Board Members</div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pa-lg">
        <!-- Current members -->
        <div class="text-caption text-grey-5 q-mb-sm">Members</div>
        <q-list dark dense>
          <q-item v-for="m in members" :key="m.id">
            <q-item-section avatar>
              <q-avatar color="teal-8" text-color="white" size="sm">
                {{ (m.user.firstName || m.user.userName)[0].toUpperCase() }}
              </q-avatar>
            </q-item-section>
            <q-item-section>
              <q-item-label class="text-white">{{ m.user.firstName }} {{ m.user.lastName }}</q-item-label>
              <q-item-label caption class="text-grey-5">@{{ m.user.userName }}</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-chip :color="roleColor(m.role)" text-color="white" dense size="sm">{{ m.role }}</q-chip>
            </q-item-section>
            <q-item-section side v-if="canManage && m.role !== 'OWNER'">
              <q-btn flat round dense icon="person_remove" color="red-4" size="xs"
                @click="removeMember(m.user.id)" />
            </q-item-section>
          </q-item>
        </q-list>

        <!-- Add member -->
        <div v-if="canManage" class="q-mt-lg">
          <div class="text-caption text-grey-5 q-mb-sm">Add Member</div>
          <div class="row q-gutter-sm items-center">
            <q-select v-model="selectedUser" :options="availableUsers" option-label="userName" option-value="id"
              label="Select user" outlined dark color="teal-5" dense class="col" emit-value map-options />
            <q-select v-model="selectedRole" :options="['MEMBER','ADMIN']" label="Role"
              outlined dark color="teal-5" dense style="width:110px" />
            <q-btn icon="person_add" color="teal-6" unelevated dense @click="addMember" :loading="adding" />
          </div>
        </div>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { boardApi } from 'src/api/tasks'
import { userApi } from 'src/api/users'

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

const availableUsers = computed(() => {
  const memberIds = props.members.map(m => m.user.id)
  return allUsers.value.filter(u => !memberIds.includes(u.id))
})

watch(() => props.modelValue, async (v) => {
  if (v) {
    try {
      const res = await userApi.getAll({ size: 200 })
      allUsers.value = res.data.data?.content || []
    } catch { /* ignore */ }
  }
})

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

const removeMember = async (userId) => {
  try {
    await boardApi.removeMember(props.boardId, userId)
    emit('updated')
    $q.notify({ type: 'positive', message: 'Member removed' })
  } catch (err) {
    $q.notify({ type: 'negative', message: err.response?.data?.message || 'Failed' })
  }
}
</script>
