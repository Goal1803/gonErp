<template>
  <q-page class="designs-home">
    <div class="home-content">
      <div class="text-h4 text-white text-weight-light q-mb-xs text-center">
        <q-icon name="palette" color="teal-5" class="q-mr-sm" />
        Designs
      </div>
      <div class="text-caption text-grey-5 text-center q-mb-xl">
        POD Design management
      </div>

      <div class="row justify-center q-gutter-lg">
        <!-- Designs card -->
        <router-link to="/designs/list" class="home-card-link">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="style" size="56px" color="teal-4" />
              <div class="text-h6 text-white q-mt-md">Designs</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Browse and manage all designs
              </div>
            </q-card-section>
          </q-card>
        </router-link>

        <!-- Boards card -->
        <router-link to="/designs/boards" class="home-card-link">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="dashboard_customize" size="56px" color="teal-4" />
              <div class="text-h6 text-white q-mt-md">Boards</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Manage design boards
              </div>
            </q-card-section>
          </q-card>
        </router-link>

        <!-- Settings card (admin only) -->
        <div v-if="authStore.isAdmin" class="home-card-link" @click="showConfigDialog = true">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="settings" size="56px" color="teal-4" />
              <div class="text-h6 text-white q-mt-md">Settings</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Configure design options
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

    <design-config-dialog v-model="showConfigDialog" />
    </div>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from 'src/stores/authStore'
import DesignConfigDialog from 'src/components/DesignConfigDialog.vue'

const authStore = useAuthStore()
const showConfigDialog = ref(false)
</script>

<style scoped>
.designs-home {
  background: #0d0d0d;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.home-content {
  max-width: 600px;
  width: 100%;
  padding: 24px;
}
.home-card-link {
  text-decoration: none;
}
.home-card {
  background: #161616;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  width: 200px;
  cursor: pointer;
  transition: transform 0.2s, border-color 0.2s, background 0.2s;
}
.home-card:hover {
  transform: translateY(-4px);
  border-color: rgba(38, 166, 154, 0.4);
  background: #1a1a1a;
}
</style>
