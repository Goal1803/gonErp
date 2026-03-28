<template>
  <q-page class="ecommerce-home">
    <div class="home-content">
      <div class="text-h4 text-white text-weight-light q-mb-xs text-center">
        <q-icon name="storefront" color="cyan-5" class="q-mr-sm" />
        E-commerce
      </div>
      <div class="text-caption text-grey-5 text-center q-mb-xl">
        Order management &amp; sales channels
      </div>

      <!-- Apps -->
      <div class="text-overline text-grey-5 q-mb-sm">Apps</div>
      <div class="row justify-center q-gutter-lg q-mb-xl">
        <div class="home-card-link" @click="router.push('/ecommerce/stores')">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="store" size="56px" color="cyan-5" />
              <div class="text-h6 text-white q-mt-md">Stores</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Manage sales channel stores
              </div>
            </q-card-section>
          </q-card>
        </div>

        <div class="home-card-link" @click="router.push('/ecommerce/orders')">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="shopping_cart" size="56px" color="cyan-5" />
              <div class="text-h6 text-white q-mt-md">Orders</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Manage orders &amp; fulfillment
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Future: Products -->
        <div class="home-card-link" style="opacity: 0.4; cursor: not-allowed">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="inventory" size="56px" color="grey-6" />
              <div class="text-h6 text-white q-mt-md">Products</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Coming soon
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Settings (admin, future) -->
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
.ecommerce-home {
  background: var(--erp-bg);
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.home-content {
  max-width: 900px;
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
  border-color: rgba(0, 188, 212, 0.4);
  background: var(--erp-bg-tertiary);
}
</style>
