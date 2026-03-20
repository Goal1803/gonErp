<template>
  <q-page class="finance-home">
    <div class="home-content">
      <div class="text-h4 text-white text-weight-light q-mb-xs text-center">
        <q-icon name="account_balance" color="green-5" class="q-mr-sm" />
        Finance
      </div>
      <div class="text-caption text-grey-5 text-center q-mb-xl">
        Financial management &amp; accounting
      </div>

      <!-- Apps -->
      <div class="text-overline text-grey-5 q-mb-sm">Apps</div>
      <div class="row justify-center q-gutter-lg q-mb-xl">
        <div class="home-card-link" @click="router.push('/finance/gma')">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="summarize" size="56px" color="green-5" />
              <div class="text-h6 text-white q-mt-md">Monthly Accounting</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                German monthly bank reconciliation
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Settings -->
      <div v-if="canManage" class="text-overline text-grey-5 q-mb-sm">Settings</div>
      <div v-if="canManage" class="row justify-center q-gutter-lg">
        <div class="home-card-link" @click="router.push('/finance/accounts')">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="account_balance_wallet" size="56px" color="blue-4" />
              <div class="text-h6 text-white q-mt-md">Bank Accounts</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Manage bank &amp; payment accounts
              </div>
            </q-card-section>
          </q-card>
        </div>

        <div class="home-card-link" @click="router.push('/finance/currencies')">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="currency_exchange" size="56px" color="amber-5" />
              <div class="text-h6 text-white q-mt-md">Currencies</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Currencies &amp; conversion rates
              </div>
            </q-card-section>
          </q-card>
        </div>

        <div v-if="authStore.isAdmin" class="home-card-link" @click="router.push('/finance/config')">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="admin_panel_settings" size="56px" color="orange-5" />
              <div class="text-h6 text-white q-mt-md">Config</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Manage finance roles
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const canManage = computed(() => {
  if (authStore.isAdmin) return true
  const role = authStore.currentUser?.financeRole
  return role === 'FINANCE_CFO' || role === 'FINANCE_ACCOUNTANT_MANAGER'
})
</script>

<style scoped>
.finance-home {
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
  border-color: rgba(76, 175, 80, 0.4);
  background: var(--erp-bg-tertiary);
}
</style>
