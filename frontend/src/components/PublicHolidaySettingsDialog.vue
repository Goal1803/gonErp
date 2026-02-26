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
                <q-icon name="event" color="purple-4" />
              </q-item-section>
              <q-item-section>
                <q-item-label class="text-adaptive">{{ holiday.name }}</q-item-label>
                <q-item-label caption class="text-adaptive-caption">
                  {{ formatDate(holiday.holidayDate) }}
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
            <div class="col-5">
              <q-input
                v-model="addForm.holidayDate"
                label="Date *"
                outlined
                dense
                color="green-5"
                type="date"
                :rules="[v => !!v || 'Date is required']"
                lazy-rules
              />
            </div>
            <div class="col-7">
              <q-input
                v-model="addForm.name"
                label="Name *"
                outlined
                dense
                color="green-5"
                :rules="[v => !!v || 'Name is required']"
                lazy-rules
              />
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
  name: '',
  isRecurring: false
})

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
  saving.value = true
  try {
    await worktimeHolidayApi.create({
      holidayDate: addForm.value.holidayDate,
      name: addForm.value.name.trim(),
      isRecurring: addForm.value.isRecurring
    })
    $q.notify({ type: 'positive', message: 'Holiday added' })
    addForm.value = { holidayDate: '', name: '', isRecurring: false }
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
