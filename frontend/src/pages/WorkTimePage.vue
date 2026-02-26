<template>
  <q-page class="worktime-home">
    <div class="home-content">
      <div class="text-h4 text-white text-weight-light q-mb-xs text-center">
        <q-icon name="schedule" color="orange-5" class="q-mr-sm" />
        Working Time
      </div>
      <div class="text-caption text-grey-5 text-center q-mb-xl">
        Time tracking, day off &amp; team management
      </div>

      <div class="row justify-center q-gutter-lg">
        <!-- Time Clock -->
        <div class="home-card-link" @click="router.push('/worktime/clock')">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="punch_clock" size="56px" color="green-5" />
              <div class="text-h6 text-white q-mt-md">Time Clock</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Check in, breaks &amp; check out
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Day Off -->
        <div class="home-card-link" @click="router.push('/worktime/day-off')">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="event_busy" size="56px" color="blue-4" />
              <div class="text-h6 text-white q-mt-md">Day Off</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Request &amp; manage time off
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Team Calendar -->
        <div class="home-card-link" @click="router.push('/worktime/calendar')">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="calendar_month" size="56px" color="purple-4" />
              <div class="text-h6 text-white q-mt-md">Team Calendar</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                View team schedules
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Reports (admin only) -->
        <div v-if="authStore.isAdmin" class="home-card-link" @click="router.push('/worktime/reports')">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="bar_chart" size="56px" color="amber-5" />
              <div class="text-h6 text-white q-mt-md">Reports</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Attendance &amp; time reports
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Team -->
        <div class="home-card-link" @click="router.push('/worktime/team')">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="groups" size="56px" color="teal-4" />
              <div class="text-h6 text-white q-mt-md">Team</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Team members &amp; status
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Settings (admin only) -->
        <div v-if="authStore.isAdmin" class="home-card-link" @click="router.push('/worktime/settings')">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="settings" size="56px" color="orange-5" />
              <div class="text-h6 text-white q-mt-md">Settings</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Configure work time rules
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()
</script>

<style scoped>
.worktime-home {
  background: var(--erp-bg);
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.home-content {
  max-width: 800px;
  width: 100%;
  padding: 24px;
}
.home-card-link {
  text-decoration: none;
  cursor: pointer;
}
.home-card {
  background: var(--erp-bg-tertiary);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 12px;
  width: 200px;
  cursor: pointer;
  transition: transform 0.2s, border-color 0.2s, background 0.2s;
}
.home-card:hover {
  transform: translateY(-4px);
  border-color: rgba(245, 124, 0, 0.4);
  background: var(--erp-bg-tertiary);
}
</style>
