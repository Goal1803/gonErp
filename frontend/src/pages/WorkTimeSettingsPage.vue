<template>
  <q-page padding>
    <div class="page-header">
      <div class="text-h5 text-white text-weight-light">
        <q-icon name="settings" color="orange-5" class="q-mr-sm" />
        Work Time Settings
      </div>
      <div class="text-caption text-grey-5 q-mt-xs">
        Configure working time rules for your organization
      </div>
    </div>

    <div v-if="initialLoading" class="text-center q-pa-xl">
      <q-spinner color="green-5" size="40px" />
    </div>

    <template v-else>
      <div class="row q-col-gutter-md">
        <!-- Work Hours -->
        <div class="col-12 col-md-6">
          <q-card class="premium-card q-mb-md" flat>
            <q-card-section>
              <div class="text-subtitle1 text-white text-weight-bold q-mb-md">
                <q-icon name="access_time" color="green-5" class="q-mr-sm" />
                Work Hours
              </div>
              <div class="row q-col-gutter-sm">
                <div class="col-12 col-sm-6">
                  <q-input
                    v-model="form.workStartTime"
                    label="Work Start Time"
                    outlined
                    dense
                    type="time"
                  />
                </div>
                <div class="col-12 col-sm-6">
                  <q-input
                    v-model="form.workEndTime"
                    label="Work End Time"
                    outlined
                    dense
                    type="time"
                  />
                </div>
                <div class="col-12 col-sm-6">
                  <q-input
                    v-model.number="form.dailyWorkHours"
                    label="Daily Work Hours"
                    outlined
                    dense
                    type="number"
                    step="0.5"
                    min="1"
                    max="24"
                  />
                </div>
                <div class="col-12 col-sm-6">
                  <q-input
                    v-model.number="form.weeklyWorkHours"
                    label="Weekly Work Hours"
                    outlined
                    dense
                    type="number"
                    step="0.5"
                    min="1"
                    max="168"
                  />
                </div>
                <div class="col-12 col-sm-6">
                  <q-input
                    v-model.number="form.lateThresholdMinutes"
                    label="Late Threshold (min)"
                    outlined
                    dense
                    type="number"
                    min="0"
                    hint="Grace period before marking late"
                  />
                </div>
                <div class="col-12 col-sm-6">
                  <q-input
                    v-model.number="form.earlyLeaveThresholdMinutes"
                    label="Early Leave Threshold (min)"
                    outlined
                    dense
                    type="number"
                    min="0"
                    hint="Threshold before marking early leave"
                  />
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Break Policy -->
        <div class="col-12 col-md-6">
          <q-card class="premium-card q-mb-md" flat>
            <q-card-section>
              <div class="text-subtitle1 text-white text-weight-bold q-mb-md">
                <q-icon name="free_breakfast" color="amber-5" class="q-mr-sm" />
                Break Policy
              </div>
              <div class="row q-col-gutter-sm">
                <div class="col-12 col-sm-6">
                  <q-input
                    v-model.number="form.breakDurationMinutes"
                    label="Break Duration (min)"
                    outlined
                    dense
                    type="number"
                    min="0"
                  />
                </div>
                <div class="col-12 col-sm-6">
                  <q-input
                    v-model.number="form.maxBreaksPerDay"
                    label="Max Breaks Per Day"
                    outlined
                    dense
                    type="number"
                    min="0"
                  />
                </div>
                <div class="col-12">
                  <q-toggle
                    v-model="form.autoDeductBreak"
                    label="Auto-deduct break from work time"
                    color="green-7"
                  />
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Tracking -->
        <div class="col-12 col-md-6">
          <q-card class="premium-card q-mb-md" flat>
            <q-card-section>
              <div class="text-subtitle1 text-white text-weight-bold q-mb-md">
                <q-icon name="gps_fixed" color="blue-4" class="q-mr-sm" />
                Tracking
              </div>
              <div class="row q-col-gutter-sm">
                <div class="col-12">
                  <q-toggle
                    v-model="form.requireLocation"
                    label="Require work location on check-in"
                    color="green-7"
                  />
                </div>
                <div class="col-12">
                  <q-toggle
                    v-model="form.requireNotes"
                    label="Require daily notes on check-out"
                    color="green-7"
                  />
                </div>
                <div class="col-12">
                  <q-toggle
                    v-model="form.allowRemote"
                    label="Allow remote work"
                    color="green-7"
                  />
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Schedule -->
        <div class="col-12 col-md-6">
          <q-card class="premium-card q-mb-md" flat>
            <q-card-section>
              <div class="text-subtitle1 text-white text-weight-bold q-mb-md">
                <q-icon name="date_range" color="purple-4" class="q-mr-sm" />
                Schedule
              </div>
              <div class="row q-col-gutter-sm">
                <div class="col-12">
                  <q-select
                    v-model="form.workDays"
                    :options="dayOptions"
                    label="Working Days"
                    outlined
                    dense
                    multiple
                    emit-value
                    map-options
                    use-chips
                  />
                </div>
                <div class="col-12 col-sm-6">
                  <q-select
                    v-model="form.timezoneId"
                    :options="timezoneOptions"
                    label="Timezone"
                    outlined
                    dense
                    emit-value
                    map-options
                    use-input
                    @filter="filterTimezones"
                  />
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Carry Over -->
        <div class="col-12 col-md-6">
          <q-card class="premium-card q-mb-md" flat>
            <q-card-section>
              <div class="text-subtitle1 text-white text-weight-bold q-mb-md">
                <q-icon name="sync" color="teal-4" class="q-mr-sm" />
                Carry Over
              </div>
              <div class="row q-col-gutter-sm">
                <div class="col-12 col-sm-6">
                  <q-input
                    v-model.number="form.annualLeaveDays"
                    label="Annual Leave Days"
                    outlined
                    dense
                    type="number"
                    min="0"
                  />
                </div>
                <div class="col-12 col-sm-6">
                  <q-input
                    v-model.number="form.maxCarryOverDays"
                    label="Max Carry Over Days"
                    outlined
                    dense
                    type="number"
                    min="0"
                  />
                </div>
                <div class="col-12">
                  <q-toggle
                    v-model="form.allowCarryOver"
                    label="Allow carry over unused leave days"
                    color="green-7"
                  />
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Features -->
        <div class="col-12 col-md-6">
          <q-card class="premium-card q-mb-md" flat>
            <q-card-section>
              <div class="text-subtitle1 text-white text-weight-bold q-mb-md">
                <q-icon name="extension" color="red-4" class="q-mr-sm" />
                Features
              </div>
              <div class="row q-col-gutter-sm">
                <div class="col-12">
                  <q-toggle
                    v-model="form.overtimeEnabled"
                    label="Enable overtime tracking"
                    color="green-7"
                  />
                </div>
                <div class="col-12" v-if="form.overtimeEnabled">
                  <q-input
                    v-model.number="form.overtimeMultiplier"
                    label="Overtime Multiplier"
                    outlined
                    dense
                    type="number"
                    step="0.1"
                    min="1"
                    hint="e.g. 1.5 for 1.5x pay"
                  />
                </div>
                <div class="col-12">
                  <q-toggle
                    v-model="form.autoCheckOutEnabled"
                    label="Auto check-out at end of day"
                    color="green-7"
                  />
                </div>
                <div class="col-12" v-if="form.autoCheckOutEnabled">
                  <q-input
                    v-model="form.autoCheckOutTime"
                    label="Auto Check-out Time"
                    outlined
                    dense
                    type="time"
                  />
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Save Button -->
      <div class="row justify-end q-mt-md">
        <q-btn
          color="green-7"
          label="Save Settings"
          icon="save"
          size="md"
          :loading="saving"
          @click="saveSettings"
        />
      </div>
    </template>
  </q-page>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useWorktimeStore } from 'src/stores/worktimeStore'

const $q = useQuasar()
const worktimeStore = useWorktimeStore()

const initialLoading = ref(true)
const saving = ref(false)

const form = reactive({
  workStartTime: '08:00',
  workEndTime: '17:00',
  dailyWorkHours: 8,
  weeklyWorkHours: 40,
  lateThresholdMinutes: 15,
  earlyLeaveThresholdMinutes: 15,
  breakDurationMinutes: 60,
  maxBreaksPerDay: 3,
  autoDeductBreak: true,
  requireLocation: false,
  requireNotes: false,
  allowRemote: true,
  workDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'],
  timezoneId: 'Asia/Ho_Chi_Minh',
  annualLeaveDays: 12,
  maxCarryOverDays: 5,
  allowCarryOver: true,
  overtimeEnabled: false,
  overtimeMultiplier: 1.5,
  autoCheckOutEnabled: false,
  autoCheckOutTime: '22:00'
})

const dayOptions = [
  { label: 'Monday', value: 'MONDAY' },
  { label: 'Tuesday', value: 'TUESDAY' },
  { label: 'Wednesday', value: 'WEDNESDAY' },
  { label: 'Thursday', value: 'THURSDAY' },
  { label: 'Friday', value: 'FRIDAY' },
  { label: 'Saturday', value: 'SATURDAY' },
  { label: 'Sunday', value: 'SUNDAY' }
]

const allTimezones = [
  { label: 'Asia/Ho_Chi_Minh (GMT+7)', value: 'Asia/Ho_Chi_Minh' },
  { label: 'Asia/Tokyo (GMT+9)', value: 'Asia/Tokyo' },
  { label: 'Asia/Shanghai (GMT+8)', value: 'Asia/Shanghai' },
  { label: 'Asia/Singapore (GMT+8)', value: 'Asia/Singapore' },
  { label: 'America/New_York (EST)', value: 'America/New_York' },
  { label: 'America/Los_Angeles (PST)', value: 'America/Los_Angeles' },
  { label: 'America/Chicago (CST)', value: 'America/Chicago' },
  { label: 'Europe/London (GMT)', value: 'Europe/London' },
  { label: 'Europe/Berlin (CET)', value: 'Europe/Berlin' },
  { label: 'Europe/Paris (CET)', value: 'Europe/Paris' },
  { label: 'Australia/Sydney (AEST)', value: 'Australia/Sydney' },
  { label: 'Pacific/Auckland (NZST)', value: 'Pacific/Auckland' },
  { label: 'UTC', value: 'UTC' }
]

const timezoneOptions = ref(allTimezones)

function filterTimezones(val, update) {
  update(() => {
    if (!val) {
      timezoneOptions.value = allTimezones
    } else {
      const needle = val.toLowerCase()
      timezoneOptions.value = allTimezones.filter(tz => tz.label.toLowerCase().includes(needle))
    }
  })
}

async function loadSettings() {
  initialLoading.value = true
  try {
    const data = await worktimeStore.fetchSettings()
    if (data) {
      Object.keys(form).forEach(key => {
        if (data[key] !== undefined && data[key] !== null) {
          form[key] = data[key]
        }
      })
    }
  } catch {
    // defaults already set
  } finally {
    initialLoading.value = false
  }
}

async function saveSettings() {
  saving.value = true
  try {
    await worktimeStore.updateSettings({ ...form })
    $q.notify({ type: 'positive', message: 'Settings saved successfully' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to save settings' })
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>
