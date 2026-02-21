<template>
  <q-layout view="lHh Lpr lFf">
    <!-- HEADER -->
    <q-header elevated class="header-toolbar">
      <q-toolbar>
        <q-btn
          flat
          dense
          round
          icon="menu"
          aria-label="Menu"
          color="white"
          @click="toggleLeftDrawer"
        />

        <q-toolbar-title class="flex items-center gap-3 q-ml-sm">
          <div class="logo-text">gonERP</div>
          <q-badge
            color="orange-8"
            label="v1.0"
            class="q-ml-xs"
            style="font-size: 0.65rem"
          />
        </q-toolbar-title>

        <div class="flex items-center gap-2">
          <q-chip
            dense
            square
            :label="authStore.currentUser?.role"
            :color="roleChipColor"
            text-color="white"
            class="text-weight-medium"
          />
          <q-btn flat round dense>
            <UserAvatar :user="authStore.currentUser" size="32px" />
            <q-tooltip>{{ authStore.currentUser?.userName }}</q-tooltip>
            <q-menu>
              <q-list dense style="min-width: 160px">
                <q-item>
                  <q-item-section>
                    <q-item-label class="text-weight-bold text-white">
                      {{ authStore.currentUser?.userName }}
                    </q-item-label>
                    <q-item-label caption class="text-green-4">
                      {{ authStore.currentUser?.role }}
                    </q-item-label>
                  </q-item-section>
                </q-item>
                <q-separator dark />
                <q-item clickable v-ripple @click="handleLogout">
                  <q-item-section avatar>
                    <q-icon name="logout" size="sm" color="red-4" />
                  </q-item-section>
                  <q-item-section>
                    <q-item-label class="text-red-4">Logout</q-item-label>
                  </q-item-section>
                </q-item>
              </q-list>
            </q-menu>
          </q-btn>
        </div>
      </q-toolbar>
    </q-header>

    <!-- LEFT DRAWER -->
    <q-drawer
      v-model="leftDrawerOpen"
      show-if-above
      :width="240"
      class="main-drawer"
    >
      <!-- Drawer Header -->
      <div class="q-pa-md flex items-center gap-2" style="border-bottom: 1px solid rgba(46,125,50,0.2)">
        <q-icon name="business" color="green-5" size="md" />
        <div>
          <div class="text-weight-bold text-white" style="font-size: 0.85rem">
            {{ authStore.isSuperAdmin ? 'gonERP Platform' : (authStore.organizationName || 'gonERP System') }}
          </div>
          <div class="text-caption text-grey-5">
            {{ authStore.isSuperAdmin ? 'Super Admin Console' : 'Enterprise Resource Planning' }}
          </div>
        </div>
      </div>

      <!-- Navigation -->
      <q-scroll-area class="fit" style="padding-top: 8px">
        <q-list padding>
          <!-- Dashboard -->
          <q-item-label header class="text-grey-6 text-uppercase text-caption q-mb-xs">
            Main
          </q-item-label>
          <q-item
            clickable
            v-ripple
            to="/dashboard"
            exact
            class="rounded-borders q-mb-xs"
          >
            <q-item-section avatar>
              <q-icon name="dashboard" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Dashboard</q-item-label>
            </q-item-section>
          </q-item>

          <!-- Platform Section (SUPER_ADMIN only) -->
          <template v-if="authStore.isSuperAdmin">
            <q-item-label header class="text-grey-6 text-uppercase text-caption q-mb-xs q-mt-sm">
              Platform
            </q-item-label>

            <q-item clickable v-ripple to="/organizations" exact class="rounded-borders q-mb-xs">
              <q-item-section avatar>
                <q-icon name="corporate_fare" />
              </q-item-section>
              <q-item-section>
                <q-item-label>Organizations</q-item-label>
              </q-item-section>
            </q-item>

            <q-item clickable v-ripple to="/organization-types" exact class="rounded-borders q-mb-xs">
              <q-item-section avatar>
                <q-icon name="account_tree" />
              </q-item-section>
              <q-item-section>
                <q-item-label>Organization Types</q-item-label>
              </q-item-section>
            </q-item>
          </template>

          <!-- Admin Section -->
          <q-item-label
            v-if="authStore.isAdmin"
            header
            class="text-grey-6 text-uppercase text-caption q-mb-xs q-mt-sm"
          >
            Administration
          </q-item-label>

          <q-item
            v-if="authStore.isAdmin"
            clickable
            v-ripple
            to="/users"
            exact
            class="rounded-borders q-mb-xs"
          >
            <q-item-section avatar>
              <q-icon name="group" />
            </q-item-section>
            <q-item-section>
              <q-item-label>User Manager</q-item-label>
            </q-item-section>
          </q-item>

          <!-- Organization Section (ADMIN only, not SUPER_ADMIN) -->
          <template v-if="authStore.isAdmin && !authStore.isSuperAdmin">
            <q-item-label header class="text-grey-6 text-uppercase text-caption q-mb-xs q-mt-sm">
              Organization
            </q-item-label>

            <q-item clickable v-ripple to="/org-structure" exact class="rounded-borders q-mb-xs">
              <q-item-section avatar>
                <q-icon name="account_tree" />
              </q-item-section>
              <q-item-section>
                <q-item-label>Org Structure</q-item-label>
              </q-item-section>
            </q-item>
          </template>

          <!-- Content Section -->
          <q-item-label header class="text-grey-6 text-uppercase text-caption q-mb-xs q-mt-sm">
            Content
          </q-item-label>

          <q-item
            v-if="authStore.hasImageManager"
            clickable
            v-ripple
            to="/images"
            exact
            class="rounded-borders q-mb-xs"
          >
            <q-item-section avatar>
              <q-icon name="image" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Image Manager</q-item-label>
            </q-item-section>
          </q-item>

          <!-- Task Manager -->
          <q-item
            v-if="authStore.hasTaskManager"
            clickable
            v-ripple
            to="/tasks"
            class="rounded-borders q-mb-xs"
          >
            <q-item-section avatar>
              <q-icon name="task_alt" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Task Manager</q-item-label>
            </q-item-section>
          </q-item>

          <!-- Designs -->
          <q-item
            v-if="authStore.hasDesigns"
            clickable
            v-ripple
            to="/designs"
            class="rounded-borders q-mb-xs"
          >
            <q-item-section avatar>
              <q-icon name="palette" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Designs</q-item-label>
            </q-item-section>
          </q-item>

          <!-- Coming Soon -->
          <q-item-label header class="text-grey-6 text-uppercase text-caption q-mb-xs q-mt-sm">
            Coming Soon
          </q-item-label>

          <q-item v-for="item in comingSoon" :key="item.label" class="rounded-borders q-mb-xs" style="opacity: 0.4; cursor: not-allowed">
            <q-item-section avatar>
              <q-icon :name="item.icon" />
            </q-item-section>
            <q-item-section>
              <q-item-label>{{ item.label }}</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-badge color="orange-8" label="Soon" />
            </q-item-section>
          </q-item>
        </q-list>
      </q-scroll-area>

      <!-- Drawer Footer -->
      <div
        class="q-pa-sm text-center text-grey-7"
        style="position: absolute; bottom: 0; left: 0; right: 0; border-top: 1px solid rgba(46,125,50,0.15); font-size: 0.7rem"
      >
        gonERP &copy; {{ new Date().getFullYear() }}
      </div>
    </q-drawer>

    <!-- PAGE CONTENT -->
    <q-page-container>
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/authStore'
import { useQuasar } from 'quasar'
import UserAvatar from 'src/components/UserAvatar.vue'

const $q = useQuasar()
const router = useRouter()
const authStore = useAuthStore()

const leftDrawerOpen = ref(true)

const toggleLeftDrawer = () => {
  leftDrawerOpen.value = !leftDrawerOpen.value
}

const roleChipColor = computed(() => {
  if (authStore.isSuperAdmin) return 'purple-8'
  if (authStore.currentUser?.role === 'ADMIN') return 'green-8'
  return 'grey-8'
})

const comingSoon = [
  { label: 'Finance', icon: 'account_balance' },
  { label: 'Sales', icon: 'point_of_sale' },
  { label: 'Marketing', icon: 'campaign' },
  { label: 'Inventory', icon: 'inventory' }
]

const handleLogout = () => {
  $q.dialog({
    title: 'Logout',
    message: 'Are you sure you want to logout?',
    cancel: true,
    persistent: false,
    color: 'primary',
    dark: true
  }).onOk(() => {
    authStore.logout()
    router.push('/login')
    $q.notify({ type: 'positive', message: 'Logged out successfully' })
  })
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
