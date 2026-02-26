<template>
  <q-dialog v-model="show" persistent>
    <q-card style="min-width: 500px; max-width: 600px" class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon name="event_busy" color="blue-4" size="md" />
        <div>
          <div class="text-h6 text-adaptive text-weight-medium">Request Day Off</div>
          <div class="text-caption text-adaptive-caption">Submit a new time-off request</div>
        </div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pa-lg">
        <q-form class="q-gutter-y-md">
          <!-- Day Off Type -->
          <q-select
            v-model="form.dayOffTypeId"
            :options="typeOptions"
            label="Day Off Type *"
            outlined
            color="green-5"
            emit-value
            map-options
            :rules="[v => !!v || 'Please select a type']"
            lazy-rules
          >
            <template #option="{ opt, itemProps }">
              <q-item v-bind="itemProps">
                <q-item-section avatar>
                  <div
                    class="color-dot"
                    :style="{ background: opt.color || '#4CAF50' }"
                  />
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ opt.label }}</q-item-label>
                </q-item-section>
              </q-item>
            </template>
          </q-select>

          <!-- Remaining Quota Info -->
          <div v-if="selectedTypeQuota != null" class="text-caption q-pa-sm" style="background: var(--erp-bg-tertiary); border-radius: 6px; border: 1px solid var(--erp-border-subtle);">
            <q-icon name="info" color="blue-4" class="q-mr-xs" />
            <span class="text-adaptive-secondary">
              Remaining quota: <strong class="text-adaptive">{{ selectedTypeQuota }}</strong> days
            </span>
          </div>

          <!-- Date Range -->
          <div class="row q-col-gutter-md">
            <div class="col-6">
              <q-input
                v-model="form.startDate"
                label="Start Date *"
                outlined
                color="green-5"
                type="date"
                :rules="[v => !!v || 'Start date is required']"
                lazy-rules
              />
            </div>
            <div class="col-6">
              <q-input
                v-model="form.endDate"
                label="End Date *"
                outlined
                color="green-5"
                type="date"
                :min="form.startDate"
                :rules="[
                  v => !!v || 'End date is required',
                  v => !form.startDate || v >= form.startDate || 'End date must be on or after start date'
                ]"
                lazy-rules
              />
            </div>
          </div>

          <!-- Half Day (only when single day) -->
          <q-select
            v-if="isSingleDay"
            v-model="form.halfDayType"
            :options="halfDayOptions"
            label="Half Day Type"
            outlined
            color="green-5"
            emit-value
            map-options
          />

          <!-- Computed Days -->
          <div v-if="computedDays > 0" class="text-caption q-pa-sm" style="background: var(--erp-bg-tertiary); border-radius: 6px; border: 1px solid var(--erp-border-subtle);">
            <q-icon name="calculate" color="green-5" class="q-mr-xs" />
            <span class="text-adaptive-secondary">
              Total days: <strong class="text-adaptive">{{ computedDays }}</strong>
              <span v-if="form.halfDayType !== 'FULL_DAY'"> ({{ form.halfDayType === 'MORNING' ? 'Morning' : 'Afternoon' }} half day)</span>
            </span>
          </div>

          <!-- Reason -->
          <q-input
            v-model="form.reason"
            label="Reason"
            outlined
            color="green-5"
            type="textarea"
            rows="3"
            autogrow
          />
        </q-form>
      </q-card-section>

      <q-card-actions align="right" style="border-top: 1px solid rgba(46,125,50,0.2)" class="q-pa-md">
        <q-btn flat label="Cancel" color="grey-5" v-close-popup />
        <q-btn
          label="Submit Request"
          color="primary"
          unelevated
          icon="send"
          :loading="submitting"
          :disable="!isValid"
          @click="handleSubmit"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { worktimeDayOffTypeApi, worktimeDayOffRequestApi, worktimeDayOffQuotaApi } from 'src/api/worktime'

const props = defineProps({
  modelValue: Boolean,
  initialDate: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'created'])

const $q = useQuasar()

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const submitting = ref(false)
const dayOffTypes = ref([])
const myQuotas = ref([])

const form = ref({
  dayOffTypeId: null,
  startDate: '',
  endDate: '',
  halfDayType: 'FULL_DAY',
  reason: ''
})

const halfDayOptions = [
  { label: 'Full Day', value: 'FULL_DAY' },
  { label: 'Morning', value: 'MORNING' },
  { label: 'Afternoon', value: 'AFTERNOON' }
]

const typeOptions = computed(() =>
  dayOffTypes.value.map(t => ({
    label: t.name,
    value: t.id,
    color: t.color
  }))
)

const isSingleDay = computed(() => {
  return form.value.startDate && form.value.endDate && form.value.startDate === form.value.endDate
})

// Reset half day when not single day
watch(isSingleDay, (val) => {
  if (!val) form.value.halfDayType = 'FULL_DAY'
})

// Count business days between two dates (inclusive)
function countBusinessDays(start, end) {
  const startDate = new Date(start)
  const endDate = new Date(end)
  let count = 0
  const current = new Date(startDate)
  while (current <= endDate) {
    const day = current.getDay()
    if (day !== 0 && day !== 6) count++
    current.setDate(current.getDate() + 1)
  }
  return count
}

const computedDays = computed(() => {
  if (!form.value.startDate || !form.value.endDate) return 0
  if (form.value.endDate < form.value.startDate) return 0
  const bizDays = countBusinessDays(form.value.startDate, form.value.endDate)
  if (isSingleDay.value && form.value.halfDayType !== 'FULL_DAY') {
    return 0.5
  }
  return bizDays
})

const selectedTypeQuota = computed(() => {
  if (!form.value.dayOffTypeId) return null
  const quota = myQuotas.value.find(q => q.dayOffTypeId === form.value.dayOffTypeId)
  if (!quota) return null
  return Math.max(0, (quota.totalDays || 0) - (quota.usedDays || 0))
})

const isValid = computed(() => {
  return form.value.dayOffTypeId &&
    form.value.startDate &&
    form.value.endDate &&
    form.value.endDate >= form.value.startDate &&
    computedDays.value > 0
})

// Reset form when dialog opens
watch(show, (val) => {
  if (val) {
    form.value = {
      dayOffTypeId: null,
      startDate: props.initialDate || '',
      endDate: props.initialDate || '',
      halfDayType: 'FULL_DAY',
      reason: ''
    }
    loadData()
  }
})

async function loadData() {
  try {
    const [typesRes, quotasRes] = await Promise.all([
      worktimeDayOffTypeApi.getActive(),
      worktimeDayOffQuotaApi.getMyQuotas(new Date().getFullYear())
    ])
    dayOffTypes.value = typesRes.data.data || []
    myQuotas.value = quotasRes.data.data || []
  } catch (e) {
    console.error('Failed to load day-off data', e)
  }
}

async function handleSubmit() {
  if (!isValid.value) return
  submitting.value = true
  try {
    const payload = {
      dayOffTypeId: form.value.dayOffTypeId,
      startDate: form.value.startDate,
      endDate: form.value.endDate,
      halfDayType: form.value.halfDayType,
      totalDays: computedDays.value,
      reason: form.value.reason || null
    }
    await worktimeDayOffRequestApi.create(payload)
    $q.notify({ type: 'positive', message: 'Day-off request submitted successfully' })
    emit('created')
    show.value = false
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to submit request' })
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (show.value) loadData()
})
</script>

<style scoped>
.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}
</style>
