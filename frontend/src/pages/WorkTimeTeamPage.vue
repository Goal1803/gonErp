<template>
  <q-page padding>
    <div class="page-header">
      <div class="row items-center">
        <div class="col">
          <div class="text-h5 text-adaptive text-weight-light">
            <q-icon name="groups" color="teal-4" class="q-mr-sm" />
            Team Availability
          </div>
          <div class="text-caption text-adaptive-caption q-mt-xs">
            {{ availability ? formatDisplayDate(availability.date) : 'Today' }}
            <span v-if="lastRefresh" class="q-ml-sm">&middot; Updated {{ lastRefresh }}</span>
          </div>
        </div>
        <div class="q-gutter-sm">
          <q-btn
            flat
            round
            icon="refresh"
            color="green-5"
            :loading="loading"
            @click="loadAvailability"
          >
            <q-tooltip>Refresh</q-tooltip>
          </q-btn>
        </div>
      </div>
    </div>

    <!-- Summary Cards -->
    <div v-if="availability && availability.members" class="row q-col-gutter-md q-mb-lg">
      <div class="col-6 col-sm-3">
        <q-card class="premium-card" flat>
          <q-card-section class="text-center q-pa-md">
            <div class="text-h4 text-weight-bold" style="color: #66bb6a;">{{ statusCount('WORKING') }}</div>
            <div class="text-caption text-adaptive-caption">Working</div>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-6 col-sm-3">
        <q-card class="premium-card" flat>
          <q-card-section class="text-center q-pa-md">
            <div class="text-h4 text-weight-bold" style="color: #ffa726;">{{ statusCount('ON_BREAK') }}</div>
            <div class="text-caption text-adaptive-caption">On Break</div>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-6 col-sm-3">
        <q-card class="premium-card" flat>
          <q-card-section class="text-center q-pa-md">
            <div class="text-h4 text-weight-bold" style="color: #42a5f5;">{{ statusCount('DAY_OFF') }}</div>
            <div class="text-caption text-adaptive-caption">Day Off</div>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-6 col-sm-3">
        <q-card class="premium-card" flat>
          <q-card-section class="text-center q-pa-md">
            <div class="text-h4 text-weight-bold" style="color: #bdbdbd;">{{ statusCount('NOT_CHECKED_IN') + statusCount('OFF') }}</div>
            <div class="text-caption text-adaptive-caption">Not Active</div>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading && !availability" class="text-center q-pa-xl">
      <q-spinner color="green-5" size="40px" />
      <div class="text-caption text-adaptive-caption q-mt-md">Loading team status...</div>
    </div>

    <!-- Grid -->
    <TeamAvailabilityGrid
      v-if="availability"
      :members="availability.members || []"
    />

    <!-- Empty -->
    <div v-if="availability && (!availability.members || !availability.members.length)" class="text-center q-pa-xl">
      <q-icon name="groups" color="grey-6" size="56px" />
      <div class="text-subtitle1 text-adaptive q-mt-md">No team members found</div>
      <div class="text-caption text-adaptive-caption">Make sure your organization has members</div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { worktimeTeamApi } from 'src/api/worktime'
import TeamAvailabilityGrid from 'src/components/TeamAvailabilityGrid.vue'

const availability = ref(null)
const loading = ref(false)
const lastRefresh = ref('')
let refreshTimer = null

async function loadAvailability() {
  loading.value = true
  try {
    const res = await worktimeTeamApi.getAvailability()
    availability.value = res.data.data
    lastRefresh.value = new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false })
  } catch (e) {
    console.error('Failed to load team availability', e)
  } finally {
    loading.value = false
  }
}

function statusCount(status) {
  if (!availability.value?.members) return 0
  return availability.value.members.filter(m => m.status === status).length
}

function formatDisplayDate(dateStr) {
  if (!dateStr) return 'Today'
  try {
    const d = new Date(dateStr + 'T00:00:00')
    return d.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })
  } catch {
    return dateStr
  }
}

onMounted(() => {
  loadAvailability()
  // Auto-refresh every 60 seconds
  refreshTimer = setInterval(loadAvailability, 60000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>
