<template>
  <q-dialog v-model="show" persistent>
    <q-card style="min-width: 420px; max-width: 500px" class="premium-card">
      <q-card-section class="flex items-center gap-3" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon name="edit_calendar" color="green-5" size="md" />
        <div>
          <div class="text-h6 text-adaptive text-weight-medium">Edit Quota</div>
          <div class="text-caption text-adaptive-caption">{{ userName }} - {{ quota?.dayOffTypeName }}</div>
        </div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </q-card-section>

      <q-card-section class="q-pa-lg">
        <q-form class="q-gutter-y-md">
          <q-input
            v-model.number="form.totalDays"
            label="Total Days *"
            outlined
            color="green-5"
            type="number"
            min="0"
            step="0.5"
            :rules="[v => v != null && v >= 0 || 'Must be 0 or more']"
            lazy-rules
          />

          <q-input
            v-model.number="form.carriedOverDays"
            label="Carried Over Days"
            outlined
            color="green-5"
            type="number"
            min="0"
            step="0.5"
            hint="Days carried over from previous year"
          />

          <!-- Info display -->
          <div class="q-pa-sm" style="background: var(--erp-bg-tertiary); border-radius: 6px; border: 1px solid var(--erp-border-subtle);">
            <div class="row q-gutter-md text-center">
              <div class="col">
                <div class="text-caption text-adaptive-caption">Used</div>
                <div class="text-subtitle1 text-adaptive text-weight-bold">{{ quota?.usedDays || 0 }}</div>
              </div>
              <div class="col">
                <div class="text-caption text-adaptive-caption">Remaining</div>
                <div class="text-subtitle1 text-weight-bold" :style="{ color: remainingColor }">{{ remainingDays }}</div>
              </div>
            </div>
          </div>
        </q-form>
      </q-card-section>

      <q-card-actions align="right" style="border-top: 1px solid rgba(46,125,50,0.2)" class="q-pa-md">
        <q-btn flat label="Cancel" color="grey-5" v-close-popup />
        <q-btn
          label="Save Quota"
          color="primary"
          unelevated
          icon="save"
          :loading="saving"
          @click="handleSave"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { worktimeDayOffQuotaApi } from 'src/api/worktime'

const props = defineProps({
  modelValue: Boolean,
  quota: { type: Object, default: null },
  userName: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const $q = useQuasar()

const show = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const saving = ref(false)

const form = ref({
  totalDays: 0,
  carriedOverDays: 0
})

const remainingDays = computed(() => {
  return Math.max(0, (form.value.totalDays || 0) - (props.quota?.usedDays || 0))
})

const remainingColor = computed(() => {
  if (remainingDays.value <= 0) return '#ef5350'
  if (remainingDays.value <= 2) return '#ffa726'
  return '#66bb6a'
})

watch(() => props.quota, (q) => {
  if (q) {
    form.value = {
      totalDays: q.totalDays || 0,
      carriedOverDays: q.carriedOverDays || 0
    }
  }
}, { immediate: true })

async function handleSave() {
  if (form.value.totalDays == null || form.value.totalDays < 0) return
  saving.value = true
  try {
    await worktimeDayOffQuotaApi.setQuota(
      props.quota.userId,
      props.quota.dayOffTypeId,
      {
        totalDays: form.value.totalDays,
        carriedOverDays: form.value.carriedOverDays || 0,
        year: props.quota.year || new Date().getFullYear()
      }
    )
    $q.notify({ type: 'positive', message: 'Quota updated successfully' })
    emit('saved')
    show.value = false
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to update quota' })
  } finally {
    saving.value = false
  }
}
</script>
