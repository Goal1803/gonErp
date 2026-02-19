<template>
  <q-dialog v-model="show" persistent>
    <q-card style="min-width: 480px" dark class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon :name="isEdit ? 'edit' : 'add_photo_alternate'" color="green-5" size="md" />
        <div>
          <div class="text-h6 text-white text-weight-medium">{{ isEdit ? 'Edit Image' : 'Add New Image' }}</div>
          <div class="text-caption text-grey-5">{{ isEdit ? `Editing: ${image?.name}` : 'Upload an image from your computer' }}</div>
        </div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pa-lg">
        <q-form class="q-gutter-y-md">
          <q-input
            v-model="form.name"
            label="Image Name"
            outlined
            dark
            color="green-5"
          >
            <template #prepend>
              <q-icon name="label" color="grey-5" />
            </template>
          </q-input>

          <q-file
            v-model="selectedFile"
            :label="isEdit ? 'Replace Image (optional)' : 'Select Image *'"
            outlined
            dark
            color="green-5"
            accept="image/*"
            @update:model-value="onFileSelected"
          >
            <template #prepend>
              <q-icon name="upload_file" color="grey-5" />
            </template>
            <template #append>
              <q-icon
                v-if="selectedFile"
                name="close"
                color="grey-5"
                class="cursor-pointer"
                @click.stop="clearFile"
              />
            </template>
          </q-file>

          <!-- Preview -->
          <div v-if="previewUrl">
            <div class="text-caption text-grey-5 q-mb-sm">
              {{ selectedFile ? 'New Image Preview' : 'Current Image' }}
            </div>
            <div
              style="width: 100%; height: 200px; border: 1px solid rgba(46,125,50,0.2); border-radius: 6px; overflow: hidden; background: #111"
            >
              <img
                :src="previewUrl"
                alt="preview"
                style="width: 100%; height: 100%; object-fit: contain"
              />
            </div>
          </div>
        </q-form>
      </q-card-section>

      <q-card-actions align="right" style="border-top: 1px solid rgba(46,125,50,0.2)" class="q-pa-md">
        <q-btn flat label="Cancel" color="grey-5" v-close-popup />
        <q-btn
          :label="isEdit ? 'Save Changes' : 'Upload Image'"
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
import { imageApi } from 'src/api/images'

const props = defineProps({
  modelValue: Boolean,
  image: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const $q = useQuasar()

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const isEdit = computed(() => !!props.image)
const saving = ref(false)

const form = ref({ name: '' })
const selectedFile = ref(null)
const previewUrl = ref(null)

watch(() => props.image, (img) => {
  selectedFile.value = null
  if (img) {
    form.value = { name: img.name || '' }
    previewUrl.value = img.url || null
  } else {
    form.value = { name: '' }
    previewUrl.value = null
  }
}, { immediate: true })

const onFileSelected = (file) => {
  if (file) {
    previewUrl.value = URL.createObjectURL(file)
  } else {
    previewUrl.value = isEdit.value ? (props.image?.url || null) : null
  }
}

const clearFile = () => {
  selectedFile.value = null
  previewUrl.value = isEdit.value ? (props.image?.url || null) : null
}

const handleSubmit = async () => {
  if (!isEdit.value && !selectedFile.value) {
    $q.notify({ type: 'warning', message: 'Please select an image file to upload' })
    return
  }
  saving.value = true
  try {
    const formData = new FormData()
    formData.append('name', form.value.name)
    if (selectedFile.value) {
      formData.append('file', selectedFile.value)
    }
    if (isEdit.value) {
      await imageApi.update(props.image.id, formData)
      $q.notify({ type: 'positive', message: 'Image updated successfully' })
    } else {
      await imageApi.create(formData)
      $q.notify({ type: 'positive', message: 'Image uploaded successfully' })
    }
    emit('saved')
  } catch (err) {
    $q.notify({ type: 'negative', message: err.response?.data?.message || 'Operation failed' })
  } finally {
    saving.value = false
  }
}
</script>
