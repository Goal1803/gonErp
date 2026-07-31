<template>
  <q-dialog v-model="open" @show="load">
    <q-card style="width: 520px; max-width: 92vw">
      <q-card-section class="row items-center q-pb-sm">
        <div class="text-subtitle1 text-weight-medium">Chia sẻ “{{ note?.title }}”</div>
        <q-space />
        <q-btn flat dense round icon="close" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pt-none">
        <div class="text-caption text-grey-6 q-mb-sm">
          Người được thêm sẽ thấy trang này và mọi trang con của nó.
        </div>

        <div class="row q-gutter-sm items-center">
          <q-select
            v-model="pickedUser"
            :options="userOptions"
            option-label="label"
            option-value="id"
            label="Thêm người"
            dense outlined use-input hide-selected fill-input emit-value map-options
            class="col"
            @filter="filterUsers"
          />
          <q-select
            v-model="permission"
            :options="[{ label: 'Chỉnh sửa', value: 'EDIT' }, { label: 'Chỉ xem', value: 'VIEW' }]"
            dense outlined emit-value map-options style="width: 130px"
          />
          <q-btn unelevated color="green-7" icon="add" dense :disable="!pickedUser" @click="add" />
        </div>
      </q-card-section>

      <q-separator />

      <q-list separator>
        <q-item>
          <q-item-section avatar>
            <q-avatar size="32px" color="green-8" text-color="white">
              {{ (note?.owner?.firstName || '?')[0] }}
            </q-avatar>
          </q-item-section>
          <q-item-section>
            <q-item-label>{{ fullName(note?.owner) }}</q-item-label>
            <q-item-label caption>Chủ sở hữu</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-for="m in members" :key="m.id">
          <q-item-section avatar>
            <q-avatar size="32px" color="grey-7" text-color="white">
              {{ (m.user.firstName || '?')[0] }}
            </q-avatar>
          </q-item-section>
          <q-item-section>
            <q-item-label>{{ fullName(m.user) }}</q-item-label>
            <q-item-label caption>
              {{ m.permission === 'EDIT' ? 'Chỉnh sửa' : 'Chỉ xem' }}
              <span v-if="m.inherited"> · kế thừa từ “{{ m.noteTitle }}”</span>
            </q-item-label>
          </q-item-section>
          <q-item-section side v-if="!m.inherited && isOwner">
            <q-btn flat dense round icon="close" size="sm" color="grey-6"
              @click="remove(m.user.id)" />
          </q-item-section>
        </q-item>
      </q-list>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { noteApi } from 'src/api/notes'
import { userApi } from 'src/api/users'

const props = defineProps({
  modelValue: Boolean,
  note: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue'])

const $q = useQuasar()
const open = ref(props.modelValue)
const members = ref([])
const allUsers = ref([])
const userOptions = ref([])
const pickedUser = ref(null)
const permission = ref('EDIT')

const isOwner = computed(() => props.note?.permission === 'OWNER')

watch(() => props.modelValue, (v) => { open.value = v })
watch(open, (v) => emit('update:modelValue', v))

function fullName (u) {
  if (!u) return ''
  return [u.firstName, u.lastName].filter(Boolean).join(' ') || u.userName
}

async function load () {
  if (!props.note) return
  members.value = (await noteApi.getMembers(props.note.id)).data.data || []
  if (!allUsers.value.length) {
    const res = await userApi.getAll()
    allUsers.value = (res.data.data || []).map(u => ({ ...u, label: fullName(u) }))
  }
  userOptions.value = allUsers.value
}

function filterUsers (val, update) {
  update(() => {
    const q = val.toLowerCase()
    userOptions.value = allUsers.value.filter(u => u.label.toLowerCase().includes(q))
  })
}

async function add () {
  try {
    await noteApi.addMember(props.note.id, { userId: pickedUser.value, permission: permission.value })
    pickedUser.value = null
    await load()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Không thêm được người dùng' })
  }
}

async function remove (userId) {
  await noteApi.removeMember(props.note.id, userId)
  await load()
}
</script>
