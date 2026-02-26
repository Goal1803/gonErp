<template>
  <q-page padding>
    <div class="page-header">
      <div class="text-h5 text-white text-weight-light">
        <q-icon name="dashboard" color="green-5" class="q-mr-sm" />
        Dashboard
      </div>
      <div class="text-caption text-grey-5 q-mt-xs">
        Welcome back, <span class="text-green-4">{{ authStore.currentUser?.userName }}</span>
        <template v-if="authStore.organizationName">
          &mdash; {{ authStore.organizationName }}
        </template>
      </div>
    </div>

    <!-- Platform Management (SUPER_ADMIN) -->
    <template v-if="authStore.isSuperAdmin">
      <q-item-label header class="text-grey-5 text-uppercase text-caption q-mb-sm">
        Platform Management
      </q-item-label>
      <div class="row q-gutter-md q-mb-lg">
        <div class="col-xs-12 col-sm-6 col-md-4 col-lg-3">
          <q-card class="premium-card cursor-pointer" flat v-ripple @click="router.push('/organizations')">
            <q-card-section class="q-pa-lg">
              <div class="flex items-center gap-3 q-mb-sm">
                <q-icon name="corporate_fare" color="purple-4" size="lg" />
                <div>
                  <div class="text-white text-weight-medium">Organizations</div>
                  <div class="text-caption text-grey-5">Manage tenant organizations</div>
                </div>
              </div>
              <div v-if="orgCount !== null" class="text-h4 text-purple-4 text-weight-bold q-mt-sm">
                {{ orgCount }}
                <span class="text-caption text-grey-5 text-weight-regular">organization(s)</span>
              </div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-xs-12 col-sm-6 col-md-4 col-lg-3">
          <q-card class="premium-card cursor-pointer" flat v-ripple @click="router.push('/organization-types')">
            <q-card-section class="q-pa-lg">
              <div class="flex items-center gap-3 q-mb-sm">
                <q-icon name="account_tree" color="amber-5" size="lg" />
                <div>
                  <div class="text-white text-weight-medium">Organization Types</div>
                  <div class="text-caption text-grey-5">Types & default structures</div>
                </div>
              </div>
              <div v-if="orgTypeCount !== null" class="text-h4 text-amber-5 text-weight-bold q-mt-sm">
                {{ orgTypeCount }}
                <span class="text-caption text-grey-5 text-weight-regular">type(s)</span>
              </div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-xs-12 col-sm-6 col-md-4 col-lg-3">
          <q-card class="premium-card cursor-pointer" flat v-ripple @click="router.push('/users')">
            <q-card-section class="q-pa-lg">
              <div class="flex items-center gap-3 q-mb-sm">
                <q-icon name="group" color="green-5" size="lg" />
                <div>
                  <div class="text-white text-weight-medium">All Users</div>
                  <div class="text-caption text-grey-5">Manage users across all orgs</div>
                </div>
              </div>
              <div v-if="userCount !== null" class="text-h4 text-green-5 text-weight-bold q-mt-sm">
                {{ userCount }}
                <span class="text-caption text-grey-5 text-weight-regular">user(s)</span>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </template>

    <!-- Organization Admin Section -->
    <template v-if="authStore.isAdmin && !authStore.isSuperAdmin">
      <q-item-label header class="text-grey-5 text-uppercase text-caption q-mb-sm">
        Organization
      </q-item-label>
      <div class="row q-gutter-md q-mb-lg">
        <div class="col-xs-12 col-sm-6 col-md-4 col-lg-3">
          <q-card class="premium-card cursor-pointer" flat v-ripple @click="router.push('/users')">
            <q-card-section class="q-pa-lg">
              <div class="flex items-center gap-3">
                <q-icon name="group" color="green-5" size="lg" />
                <div>
                  <div class="text-white text-weight-medium">User Manager</div>
                  <div class="text-caption text-grey-5">Manage your organization's users</div>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-xs-12 col-sm-6 col-md-4 col-lg-3">
          <q-card class="premium-card cursor-pointer" flat v-ripple @click="router.push('/org-structure')">
            <q-card-section class="q-pa-lg">
              <div class="flex items-center gap-3">
                <q-icon name="account_tree" color="teal-5" size="lg" />
                <div>
                  <div class="text-white text-weight-medium">Org Structure</div>
                  <div class="text-caption text-grey-5">Staff roles, departments, groups</div>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </template>

    <!-- Modules -->
    <q-item-label header class="text-grey-5 text-uppercase text-caption q-mb-sm">
      Modules
    </q-item-label>
    <div class="row q-gutter-md">
      <div
        v-for="module in visibleModules"
        :key="module.title"
        class="col-xs-12 col-sm-6 col-md-4 col-lg-3"
      >
        <q-card
          class="premium-card cursor-pointer"
          flat
          v-ripple
          :style="module.disabled ? 'opacity: 0.5; pointer-events: none' : ''"
          @click="!module.disabled && module.to && router.push(module.to)"
        >
          <q-card-section class="q-pa-lg">
            <div class="flex items-center gap-3 q-mb-sm">
              <q-icon :name="module.icon" :color="module.color" size="lg" />
              <div>
                <div class="text-white text-weight-medium">{{ module.title }}</div>
                <div class="text-caption text-grey-5">{{ module.description }}</div>
              </div>
            </div>
            <q-badge v-if="module.disabled" color="orange-8" label="Coming Soon" />
          </q-card-section>
        </q-card>
      </div>
    </div>

    <div class="q-mt-xl text-center text-grey-7 text-caption">
      gonERP v1.0 &mdash; Built for scale
    </div>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const orgCount = ref(null)
const orgTypeCount = ref(null)
const userCount = ref(null)

const allModules = [
  { title: 'Image Manager', description: 'Manage media files', icon: 'image', color: 'blue-4', to: '/images', disabled: false, flag: 'hasImageManager' },
  { title: 'Task Manager', description: 'Project & task tracking', icon: 'task_alt', color: 'teal-5', to: '/tasks', disabled: false, flag: 'hasTaskManager' },
  { title: 'Designs', description: 'Design management', icon: 'palette', color: 'purple-4', to: '/designs', disabled: false, flag: 'hasDesigns' },
  { title: 'Working Time', description: 'Time tracking & day off', icon: 'schedule', color: 'orange-5', to: '/worktime', disabled: false, flag: 'hasWorkTime' },
  { title: 'Finance', description: 'Financial management', icon: 'account_balance', color: 'yellow-6', to: null, disabled: true },
  { title: 'Sales', description: 'Sales & CRM', icon: 'point_of_sale', color: 'orange-5', to: null, disabled: true },
  { title: 'Marketing', description: 'Marketing campaigns', icon: 'campaign', color: 'purple-4', to: null, disabled: true },
  { title: 'Inventory', description: 'Stock management', icon: 'inventory', color: 'cyan-5', to: null, disabled: true }
]

const visibleModules = computed(() =>
  allModules.filter(m => !m.flag || authStore[m.flag])
)

const loadStats = async () => {
  if (!authStore.isSuperAdmin) return
  try {
    const { orgApi, orgTypeApi } = await import('src/api/organizations')
    const { userApi } = await import('src/api/users')
    const [orgs, types, users] = await Promise.all([
      orgApi.getAll(),
      orgTypeApi.getAll(),
      userApi.getAll({ page: 0, size: 1 })
    ])
    orgCount.value = orgs.data.data?.length ?? 0
    orgTypeCount.value = types.data.data?.length ?? 0
    userCount.value = users.data.data?.totalElements ?? 0
  } catch {
    // Stats are optional
  }
}

onMounted(() => {
  loadStats()
})
</script>
