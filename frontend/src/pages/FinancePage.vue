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

        <div v-if="authStore.isAdmin" class="home-card-link" @click="showImportExportDialog = true">
          <q-card class="home-card" flat>
            <q-card-section class="column items-center q-pa-lg">
              <q-icon name="import_export" size="56px" color="teal-5" />
              <div class="text-h6 text-white q-mt-md">Import / Export Config</div>
              <div class="text-caption text-grey-5 q-mt-xs text-center">
                Export or import finance configuration
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </div>

    <!-- Import / Export Dialog -->
    <q-dialog v-model="showImportExportDialog" persistent>
      <q-card style="min-width: 500px; background: var(--erp-bg-tertiary); color: white;">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">
            <q-icon name="import_export" class="q-mr-sm" color="teal-5" />
            Import / Export Configuration
          </div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-separator dark class="q-my-sm" />

        <!-- Export Section -->
        <q-card-section>
          <div class="text-subtitle1 text-weight-medium q-mb-sm">Export</div>
          <div class="text-caption text-grey-5 q-mb-md">
            Download all currencies, bank accounts, and categorization rules as a JSON file.
          </div>
          <q-btn
            label="Export Configuration"
            icon="download"
            color="teal-5"
            no-caps
            :loading="exporting"
            @click="doExport"
          />
        </q-card-section>

        <q-separator dark class="q-my-sm" />

        <!-- Import Section -->
        <q-card-section>
          <div class="text-subtitle1 text-weight-medium q-mb-sm">Import</div>
          <div class="text-caption text-grey-5 q-mb-md">
            Upload a previously exported JSON file. Existing items (matched by code/name) will be skipped.
          </div>

          <q-file
            v-model="importFile"
            label="Select JSON file"
            accept=".json"
            outlined
            dark
            dense
            class="q-mb-md"
            @update:model-value="onFileSelected"
          >
            <template v-slot:prepend>
              <q-icon name="attach_file" />
            </template>
          </q-file>

          <!-- Preview -->
          <div v-if="importPreview" class="q-mb-md q-pa-sm" style="background: rgba(255,255,255,0.05); border-radius: 8px;">
            <div class="text-caption text-grey-4">
              Preview: <strong>{{ importPreview.currencies }} currencies</strong>,
              <strong>{{ importPreview.accounts }} accounts</strong>,
              <strong>{{ importPreview.rules }} rules</strong>
            </div>
            <div v-if="importPreview.orgName" class="text-caption text-grey-5">
              From: {{ importPreview.orgName }} (exported {{ importPreview.exportedAt }})
            </div>
          </div>

          <q-btn
            label="Import Configuration"
            icon="upload"
            color="teal-7"
            no-caps
            :loading="importing"
            :disable="!importData"
            @click="doImport"
          />

          <!-- Result -->
          <div v-if="importResult" class="q-mt-md q-pa-sm" style="background: rgba(76,175,80,0.15); border-radius: 8px;">
            <div class="text-caption text-green-4">{{ importResult }}</div>
          </div>
        </q-card-section>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/authStore'
import { useQuasar } from 'quasar'
import { financeConfigApi } from 'src/api/finance'

const router = useRouter()
const authStore = useAuthStore()
const $q = useQuasar()

const canManage = computed(() => {
  if (authStore.isAdmin) return true
  const role = authStore.currentUser?.financeRole
  return role === 'FINANCE_CFO' || role === 'FINANCE_ACCOUNTANT_MANAGER'
})

// Import / Export state
const showImportExportDialog = ref(false)
const exporting = ref(false)
const importing = ref(false)
const importFile = ref(null)
const importData = ref(null)
const importPreview = ref(null)
const importResult = ref(null)

async function doExport () {
  exporting.value = true
  try {
    const res = await financeConfigApi.exportConfig()
    const bundle = res.data.data
    const json = JSON.stringify(bundle, null, 2)
    const orgName = (bundle.organizationName || 'org').replace(/[^a-zA-Z0-9]/g, '_')
    const date = new Date().toISOString().slice(0, 10)
    const filename = `finance_config_${orgName}_${date}.json`

    const blob = new Blob([json], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)

    $q.notify({ type: 'positive', message: 'Configuration exported successfully' })
  } catch (err) {
    $q.notify({ type: 'negative', message: 'Export failed: ' + (err.response?.data?.message || err.message) })
  } finally {
    exporting.value = false
  }
}

function onFileSelected (file) {
  importData.value = null
  importPreview.value = null
  importResult.value = null
  if (!file) return

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const parsed = JSON.parse(e.target.result)
      importData.value = parsed
      importPreview.value = {
        currencies: (parsed.currencies || []).length,
        accounts: (parsed.accounts || []).length,
        rules: (parsed.rules || []).length,
        orgName: parsed.organizationName || null,
        exportedAt: parsed.exportedAt || null
      }
    } catch {
      $q.notify({ type: 'negative', message: 'Invalid JSON file' })
      importFile.value = null
    }
  }
  reader.readAsText(file)
}

async function doImport () {
  if (!importData.value) return
  importing.value = true
  importResult.value = null
  try {
    const res = await financeConfigApi.importConfig(importData.value)
    importResult.value = res.data.data || res.data.message || 'Import complete'
    $q.notify({ type: 'positive', message: 'Configuration imported successfully' })
  } catch (err) {
    $q.notify({ type: 'negative', message: 'Import failed: ' + (err.response?.data?.message || err.message) })
  } finally {
    importing.value = false
  }
}
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
