<template>
  <q-page padding>
    <div class="page-header">
      <div class="text-h5 text-white text-weight-light">
        <q-icon name="dashboard" color="green-5" class="q-mr-sm" />
        Dashboard
      </div>
      <div class="text-caption text-grey-5 q-mt-xs">
        Welcome back, <span class="text-green-4">{{ authStore.currentUser?.userName }}</span>
      </div>
    </div>

    <div class="row q-gutter-md">
      <div
        v-for="module in modules"
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
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const modules = [
  { title: 'User Manager', description: 'Manage system users', icon: 'group', color: 'green-5', to: '/users', disabled: false },
  { title: 'Image Manager', description: 'Manage media files', icon: 'image', color: 'blue-4', to: '/images', disabled: false },
  { title: 'Finance', description: 'Financial management', icon: 'account_balance', color: 'yellow-6', to: null, disabled: true },
  { title: 'Sales', description: 'Sales & CRM', icon: 'point_of_sale', color: 'orange-5', to: null, disabled: true },
  { title: 'Marketing', description: 'Marketing campaigns', icon: 'campaign', color: 'purple-4', to: null, disabled: true },
  { title: 'Inventory', description: 'Stock management', icon: 'inventory', color: 'cyan-5', to: null, disabled: true },
  { title: 'Task Manager', description: 'Project & task tracking', icon: 'task_alt', color: 'teal-5', to: null, disabled: true }
]
</script>
