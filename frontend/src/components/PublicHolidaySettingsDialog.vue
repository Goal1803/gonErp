<template>
  <q-dialog v-model="show" persistent>
    <q-card style="min-width: 540px; max-width: 640px" class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon name="celebration" color="purple-4" size="md" />
        <div>
          <div class="text-h6 text-adaptive text-weight-medium">Public Holidays</div>
          <div class="text-caption text-adaptive-caption">Manage public holidays for your organization</div>
        </div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pa-lg" style="max-height: 50vh; overflow-y: auto;">
        <!-- Loading -->
        <div v-if="loading" class="text-center q-pa-md">
          <q-spinner color="green-5" size="32px" />
        </div>

        <!-- Existing Holidays List -->
        <template v-else>
          <div v-if="!holidays.length" class="text-caption text-adaptive-caption q-pa-md text-center">
            No holidays defined yet
          </div>

          <q-list v-else separator class="q-mb-md">
            <q-item v-for="holiday in holidays" :key="holiday.id" class="q-px-none">
              <q-item-section avatar>
                <div class="color-swatch" :style="{ background: holiday.color || '#9C27B0' }">
                  <q-icon name="celebration" size="14px" color="white" />
                </div>
              </q-item-section>
              <q-item-section>
                <q-item-label class="text-adaptive">{{ holiday.name }}</q-item-label>
                <q-item-label caption class="text-adaptive-caption">
                  {{ formatDate(holiday.holidayDate) }}
                  <span v-if="holiday.endDate && holiday.endDate !== holiday.holidayDate">
                    &mdash; {{ formatDate(holiday.endDate) }}
                  </span>
                </q-item-label>
              </q-item-section>
              <q-item-section side>
                <div class="row items-center q-gutter-xs">
                  <q-badge
                    v-if="holiday.isRecurring"
                    color="purple-8"
                    label="Recurring"
                    class="text-weight-medium"
                  />
                  <q-btn
                    flat
                    dense
                    round
                    icon="delete"
                    color="red-4"
                    size="sm"
                    @click="deleteHoliday(holiday)"
                  >
                    <q-tooltip>Delete</q-tooltip>
                  </q-btn>
                </div>
              </q-item-section>
            </q-item>
          </q-list>
        </template>
      </q-card-section>

      <!-- Add Form -->
      <q-card-section class="q-pa-lg" style="border-top: 1px solid var(--erp-border-subtle);">
        <div class="text-subtitle2 text-adaptive text-weight-medium q-mb-sm">Add New Holiday</div>
        <q-form class="q-gutter-y-sm">
          <div class="row q-col-gutter-md">
            <div class="col-4">
              <q-input
                v-model="addForm.holidayDate"
                label="Start date *"
                outlined dense color="green-5"
                type="date"
                :rules="[v => !!v || 'Start date is required']"
                lazy-rules
              />
            </div>
            <div class="col-4">
              <q-input
                v-model="addForm.endDate"
                label="End date (optional)"
                outlined dense color="green-5"
                type="date"
                :min="addForm.holidayDate"
                hint="Leave empty for single day"
              />
            </div>
            <div class="col-4">
              <q-input
                v-model="addForm.name"
                label="Name *"
                outlined dense color="green-5"
                :rules="[v => !!v || 'Name is required']"
                lazy-rules
              />
            </div>
          </div>

          <!-- Color swatches -->
          <div>
            <div class="text-caption text-adaptive-caption q-mb-xs">Color</div>
            <div class="row q-gutter-sm items-center">
              <div
                v-for="swatch in colorSwatches"
                :key="swatch"
                class="color-pick cursor-pointer"
                :class="{ 'color-pick-active': addForm.color === swatch }"
                :style="{ background: swatch }"
                @click="addForm.color = swatch"
              />
              <q-input
                v-model="addForm.color"
                dense outlined color="green-5"
                style="max-width: 140px"
                hint="Hex color"
              >
                <template #prepend>
                  <div class="color-preview" :style="{ background: addForm.color || '#9C27B0' }" />
                </template>
              </q-input>
            </div>
          </div>

          <div class="row items-center justify-between">
            <q-toggle
              v-model="addForm.isRecurring"
              label="Repeats every year"
              color="purple-7"
              dense
            />
            <q-btn
              label="Add Holiday"
              color="primary"
              unelevated
              dense
              icon="add"
              :loading="saving"
              :disable="!addForm.holidayDate || !addForm.name"
              @click="addHoliday"
            />
          </div>
        </q-form>
      </q-card-section>

      <q-card-actions align="right" style="border-top: 1px solid rgba(46,125,50,0.2)" class="q-pa-md">
        <q-btn flat label="Close" color="grey-5" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { worktimeHolidayApi } from 'src/api/worktime'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'changed'])

const $q = useQuasar()

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const loading = ref(false)
const saving = ref(false)
const holidays = ref([])

const addForm = ref({
  holidayDate: '',
  endDate: '',
  name: '',
  color: '#9C27B0',
  isRecurring: false
})

const colorSwatches = [
  '#9C27B0', '#E91E63', '#F44336', '#FF5722',
  '#FF9800', '#4CAF50', '#009688', '#00BCD4',
  '#3F51B5', '#2196F3', '#607D8B', '#795548'
]

function formatDate(dateStr) {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr + 'T00:00:00')
    return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' })
  } catch {
    return dateStr
  }
}

async function loadHolidays() {
  loading.value = true
  try {
    const res = await worktimeHolidayApi.getAll()
    holidays.value = (res.data.data || []).sort((a, b) => {
      if (a.holidayDate < b.holidayDate) return -1
      if (a.holidayDate > b.holidayDate) return 1
      return 0
    })
  } catch (e) {
    console.error('Failed to load holidays', e)
  } finally {
    loading.value = false
  }
}

async function addHoliday() {
  if (!addForm.value.holidayDate || !addForm.value.name?.trim()) return
  if (addForm.value.endDate && addForm.value.endDate < addForm.value.holidayDate) {
    $q.notify({ type: 'warning', message: 'End date cannot be before start date' })
    return
  }
  saving.value = true
  try {
    await worktimeHolidayApi.create({
      holidayDate: addForm.value.holidayDate,
      endDate: addForm.value.endDate || null,
      name: addForm.value.name.trim(),
      color: addForm.value.color || null,
      isRecurring: addForm.value.isRecurring
    })
    $q.notify({ type: 'positive', message: 'Holiday added' })
    addForm.value = { holidayDate: '', endDate: '', name: '', color: '#9C27B0', isRecurring: false }
    await loadHolidays()
    emit('changed')
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to add holiday' })
  } finally {
    saving.value = false
  }
}

async function deleteHoliday(holiday) {
  $q.dialog({
    title: 'Delete Holiday',
    message: `Delete "${holiday.name}" (${formatDate(holiday.holidayDate)})?`,
    cancel: true,
    persistent: true
  }).onOk(async () => {
    try {
      await worktimeHolidayApi.delete(holiday.id)
      $q.notify({ type: 'positive', message: 'Holiday deleted' })
      await loadHolidays()
      emit('changed')
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to delete holiday' })
    }
  })
}

watch(show, (val) => {
  if (val) {
    loadHolidays()
  }
})
</script>

<style scoped>
.color-swatch {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.color-pick {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  border: 2px solid transparent;
  transition: border-color 0.15s, transform 0.15s;
}
.color-pick:hover { transform: scale(1.15); }
.color-pick-active {
  border-color: #fff;
  box-shadow: 0 0 0 2px rgba(255,255,255,0.3);
}
.color-preview {
  width: 18px;
  height: 18px;
  border-radius: 4px;
}
</style>
