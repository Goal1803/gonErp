<template>
  <q-dialog v-model="show" persistent>
    <q-card style="min-width: 480px; max-width: 560px" class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon :name="isEdit ? 'edit' : 'add_circle'" color="green-5" size="md" />
        <div>
          <div class="text-h6 text-adaptive text-weight-medium">{{ isEdit ? 'Edit' : 'Create' }} Day Off Type</div>
          <div class="text-caption text-adaptive-caption">{{ isEdit ? `Editing: ${dayOffType?.name}` : 'Define a new day-off type' }}</div>
        </div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pa-lg">
        <q-form class="q-gutter-y-md">
          <!-- Name -->
          <q-input
            v-model="form.name"
            label="Name *"
            outlined
            color="green-5"
            :rules="[v => !!v || 'Name is required']"
            lazy-rules
          />

          <!-- Color -->
          <div>
            <div class="text-caption text-adaptive-caption q-mb-xs">Color</div>
            <div class="row q-gutter-sm items-center">
              <div
                v-for="swatch in colorSwatches"
                :key="swatch"
                class="color-swatch cursor-pointer"
                :class="{ 'color-swatch-active': form.color === swatch }"
                :style="{ background: swatch }"
                @click="form.color = swatch"
              />
              <q-input
                v-model="form.color"
                dense
                outlined
                color="green-5"
                class="col"
                style="max-width: 140px"
                hint="Hex color"
              >
                <template #prepend>
                  <div
                    class="color-preview"
                    :style="{ background: form.color || '#4CAF50' }"
                  />
                </template>
              </q-input>
            </div>
          </div>

          <!-- Is Paid + Active -->
          <div class="row q-gutter-md">
            <div class="col">
              <q-toggle
                v-model="form.isPaid"
                label="Is Paid"
                color="green-7"
              />
            </div>
            <div class="col">
              <q-toggle
                v-model="form.active"
                label="Active"
                color="green-7"
              />
            </div>
          </div>

          <!-- Default Quota + Display Order -->
          <div class="row q-col-gutter-md">
            <div class="col-6">
              <q-input
                v-model.number="form.defaultQuota"
                label="Default Quota (days)"
                outlined
                color="green-5"
                type="number"
                min="0"
                step="0.5"
              />
            </div>
            <div class="col-6">
              <q-input
                v-model.number="form.displayOrder"
                label="Display Order"
                outlined
                color="green-5"
                type="number"
                min="0"
                step="1"
              />
            </div>
          </div>
        </q-form>
      </q-card-section>

      <q-card-actions align="right" style="border-top: 1px solid rgba(46,125,50,0.2)" class="q-pa-md">
        <q-btn flat label="Cancel" color="grey-5" v-close-popup />
        <q-btn
          :label="isEdit ? 'Save Changes' : 'Create Type'"
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
import { worktimeDayOffTypeApi } from 'src/api/worktime'

const props = defineProps({
  modelValue: Boolean,
  dayOffType: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const $q = useQuasar()

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const isEdit = computed(() => !!props.dayOffType)
const saving = ref(false)

const colorSwatches = [
  '#4CAF50', '#2196F3', '#FF9800', '#E91E63',
  '#9C27B0', '#00BCD4', '#FF5722', '#607D8B',
  '#795548', '#F44336', '#3F51B5', '#009688'
]

const defaultForm = {
  name: '',
  color: '#4CAF50',
  isPaid: true,
  defaultQuota: 12,
  displayOrder: 0,
  active: true
}

const form = ref({ ...defaultForm })

watch(() => props.dayOffType, (dt) => {
  if (dt) {
    form.value = {
      name: dt.name || '',
      color: dt.color || '#4CAF50',
      isPaid: dt.isPaid !== false,
      defaultQuota: dt.defaultQuota || 0,
      displayOrder: dt.displayOrder || 0,
      active: dt.active !== false
    }
  } else {
    form.value = { ...defaultForm }
  }
}, { immediate: true })

async function handleSubmit() {
  if (!form.value.name?.trim()) {
    $q.notify({ type: 'negative', message: 'Name is required' })
    return
  }
  saving.value = true
  try {
    const payload = { ...form.value }
    if (isEdit.value) {
      await worktimeDayOffTypeApi.update(props.dayOffType.id, payload)
      $q.notify({ type: 'positive', message: 'Day off type updated' })
    } else {
      await worktimeDayOffTypeApi.create(payload)
      $q.notify({ type: 'positive', message: 'Day off type created' })
    }
    emit('saved')
    show.value = false
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Operation failed' })
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.color-swatch {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 2px solid transparent;
  transition: border-color 0.2s, transform 0.2s;
}
.color-swatch:hover {
  transform: scale(1.15);
}
.color-swatch-active {
  border-color: #fff;
  box-shadow: 0 0 0 2px rgba(255,255,255,0.3);
}
.color-preview {
  width: 20px;
  height: 20px;
  border-radius: 4px;
}
</style>
