<template>
  <q-dialog v-model="show" persistent>
    <q-card style="min-width: 420px; max-width: 500px" dark class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon :name="isEdit ? 'edit' : 'add_circle'" color="green-5" size="md" />
        <div>
          <div class="text-h6 text-white text-weight-medium">{{ isEdit ? 'Edit' : 'Add' }} {{ entityLabel }}</div>
          <div class="text-caption text-grey-5">{{ isEdit ? `Editing: ${item?.name}` : `Create a new ${entityLabel.toLowerCase()}` }}</div>
        </div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pa-lg">
        <q-form @submit="handleSubmit" class="q-gutter-y-md">
          <q-input
            v-model="form.name"
            label="Name *"
            outlined
            dark
            color="green-5"
            :rules="[v => !!v || 'Name is required']"
            lazy-rules
          />
          <q-input
            v-model="form.description"
            label="Description"
            outlined
            dark
            color="green-5"
            type="textarea"
            rows="3"
          />
        </q-form>
      </q-card-section>

      <q-card-actions align="right" style="border-top: 1px solid rgba(46,125,50,0.2)" class="q-pa-md">
        <q-btn flat label="Cancel" color="grey-5" v-close-popup />
        <q-btn
          :label="isEdit ? 'Save Changes' : 'Create'"
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

const props = defineProps({
  modelValue: Boolean,
  item: { type: Object, default: null },
  entityLabel: { type: String, default: 'Item' },
  createFn: { type: Function, required: true },
  updateFn: { type: Function, required: true }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const $q = useQuasar()

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const isEdit = computed(() => !!props.item)
const saving = ref(false)

const form = ref({ name: '', description: '' })

watch(() => props.item, (item) => {
  if (item) {
    form.value = { name: item.name || '', description: item.description || '' }
  } else {
    form.value = { name: '', description: '' }
  }
}, { immediate: true })

const handleSubmit = async () => {
  if (!form.value.name?.trim()) return
  saving.value = true
  try {
    if (isEdit.value) {
      await props.updateFn(props.item.id, form.value)
      $q.notify({ type: 'positive', message: `${props.entityLabel} updated` })
    } else {
      await props.createFn(form.value)
      $q.notify({ type: 'positive', message: `${props.entityLabel} created` })
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
