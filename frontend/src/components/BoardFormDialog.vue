<template>
  <q-dialog v-model="show" persistent>
    <q-card style="min-width: 460px" class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon :name="isEdit ? 'edit' : 'dashboard_customize'" color="teal-5" size="md" />
        <div>
          <div class="text-h6 text-white text-weight-medium">{{ isEdit ? 'Edit Board' : 'New Board' }}</div>
          <div class="text-caption text-grey-5">{{ isEdit ? 'Update board details' : 'Create a new Kanban board' }}</div>
        </div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pa-lg q-gutter-y-md">
        <q-input v-model="form.name" label="Board Name *" outlined color="teal-5"
          :rules="[v => !!v || 'Board name is required']" lazy-rules>
          <template #prepend><q-icon name="dashboard" color="grey-5" /></template>
        </q-input>

        <q-input v-model="form.description" label="Description" outlined color="teal-5" type="textarea" rows="2">
          <template #prepend><q-icon name="notes" color="grey-5" /></template>
        </q-input>

        <q-select v-if="!isEdit && !forceBoardType" v-model="form.boardType" :options="boardTypeOptions" emit-value map-options
          outlined color="teal-5" label="Board Type">
          <template #prepend><q-icon name="category" color="grey-5" /></template>
        </q-select>

        <div v-if="!isEdit && form.boardType === 'POD_DESIGN'" class="text-caption text-amber-4 q-pa-sm"
          style="background: rgba(255,183,77,0.08); border-radius: 6px; border: 1px solid rgba(255,183,77,0.2)">
          POD Design boards have 10 fixed columns (Draft, Idea, Doing, etc.) that cannot be added, deleted, or renamed.
        </div>

        <div>
          <div class="text-caption text-grey-5 q-mb-sm">Cover Color</div>
          <div class="row q-gutter-sm">
            <div v-for="color in colorOptions" :key="color"
              class="cursor-pointer"
              style="width:32px; height:32px; border-radius:6px; transition: transform 0.1s"
              :style="{ background: color, transform: form.coverColor === color ? 'scale(1.25)' : 'scale(1)', outline: form.coverColor === color ? '2px solid white' : 'none', outlineOffset: '2px' }"
              @click="form.coverColor = color"
            />
          </div>
        </div>
      </q-card-section>

      <q-card-actions align="right" style="border-top: 1px solid rgba(46,125,50,0.2)" class="q-pa-md">
        <q-btn flat label="Cancel" color="grey-5" v-close-popup />
        <q-btn :label="isEdit ? 'Save Changes' : 'Create Board'" color="teal-6" unelevated
          :loading="saving" @click="handleSubmit" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { boardApi } from 'src/api/tasks'

const props = defineProps({
  modelValue: Boolean,
  board: { type: Object, default: null },
  forceBoardType: { type: String, default: null }
})
const emit = defineEmits(['update:modelValue', 'saved'])
const $q = useQuasar()

const colorOptions = ['#2E7D32', '#1565C0', '#6A1B9A', '#AD1457', '#E65100', '#F9A825', '#00695C', '#37474F']
const boardTypeOptions = [
  { label: 'General', value: 'GENERAL' },
  { label: 'POD Design', value: 'POD_DESIGN' }
]

const show = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const isEdit = computed(() => !!props.board)
const saving = ref(false)
const form = ref({ name: '', description: '', coverColor: '#2E7D32', boardType: 'GENERAL' })

watch(() => props.board, b => {
  const defaultType = props.forceBoardType || 'GENERAL'
  if (b) form.value = { name: b.name || '', description: b.description || '', coverColor: b.coverColor || '#2E7D32', boardType: b.boardType || defaultType }
  else form.value = { name: '', description: '', coverColor: '#2E7D32', boardType: defaultType }
}, { immediate: true })

const handleSubmit = async () => {
  if (!form.value.name) { $q.notify({ type: 'warning', message: 'Board name is required' }); return }
  saving.value = true
  try {
    if (isEdit.value) {
      await boardApi.update(props.board.id, form.value)
      $q.notify({ type: 'positive', message: 'Board updated' })
    } else {
      await boardApi.create(form.value)
      $q.notify({ type: 'positive', message: 'Board created' })
    }
    emit('saved')
  } catch (err) {
    $q.notify({ type: 'negative', message: err.response?.data?.message || 'Operation failed' })
  } finally {
    saving.value = false
  }
}
</script>
