<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/finance/gma" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">Share Links</div>
        <div class="text-caption text-grey-5">Create secure links for your Steuerberater</div>
      </div>
      <q-space />
      <q-btn unelevated color="green-7" icon="add_link" label="New Link" no-caps @click="showCreate = true" />
    </div>

    <q-table
      :rows="links"
      :columns="columns"
      row-key="id"
      flat bordered
      :loading="loading"
      class="erp-table"
      :pagination="{ rowsPerPage: 20 }"
    >
      <template v-slot:body-cell-status="props">
        <q-td :props="props">
          <q-badge :color="props.row.active ? 'green-7' : 'grey-7'" :label="props.row.active ? 'Active' : 'Inactive'" />
        </q-td>
      </template>
      <template v-slot:body-cell-link="props">
        <q-td :props="props">
          <q-btn flat dense icon="content_copy" color="blue-4" @click="copyLink(props.row)">
            <q-tooltip>Copy link</q-tooltip>
          </q-btn>
          <q-btn flat dense icon="open_in_new" color="green-5" @click="openLink(props.row)">
            <q-tooltip>Open</q-tooltip>
          </q-btn>
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn flat dense :icon="props.row.active ? 'pause' : 'play_arrow'" :color="props.row.active ? 'orange-5' : 'green-5'" @click="toggleLink(props.row)">
            <q-tooltip>{{ props.row.active ? 'Deactivate' : 'Activate' }}</q-tooltip>
          </q-btn>
          <q-btn flat dense icon="delete" color="red-4" @click="confirmDelete(props.row)" />
        </q-td>
      </template>
    </q-table>

    <!-- Create Dialog -->
    <q-dialog v-model="showCreate">
      <q-card style="min-width: 400px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Create Share Link</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-select
            v-model="createForm.monthlyReportId"
            :options="reports"
            option-value="id"
            :option-label="r => `${monthNames[r.month]} ${r.year}`"
            emit-value map-options
            label="Monthly Report"
            dense outlined dark
          />
          <q-input v-model="createForm.recipientName" label="Recipient Name" dense outlined dark />
          <q-input v-model="createForm.recipientEmail" label="Recipient Email" dense outlined dark />
          <q-input v-model="createForm.expiresAt" label="Expires At (optional)" type="datetime-local" dense outlined dark />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated label="Create" color="green-7" no-caps @click="handleCreate" :loading="creating" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useQuasar, copyToClipboard } from 'quasar'
import { financeShareLinkApi } from 'src/api/finance'
import { useFinanceStore } from 'src/stores/financeStore'

const $q = useQuasar()
const financeStore = useFinanceStore()

const links = ref([])
const loading = ref(false)
const showCreate = ref(false)
const creating = ref(false)
const reports = ref([])

const monthNames = ['', 'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']

const createForm = ref({ monthlyReportId: null, recipientName: '', recipientEmail: '', expiresAt: null })

const columns = [
  { name: 'report', label: 'Report', field: row => `${monthNames[row.reportMonth]} ${row.reportYear}`, align: 'left' },
  { name: 'recipientName', label: 'Recipient', field: 'recipientName', align: 'left' },
  { name: 'recipientEmail', label: 'Email', field: 'recipientEmail', align: 'left' },
  { name: 'status', label: 'Status', field: 'active', align: 'center' },
  { name: 'accessCount', label: 'Views', field: 'accessCount', align: 'center' },
  { name: 'link', label: 'Link', field: 'token', align: 'center' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

async function loadLinks() {
  loading.value = true
  try {
    const res = await financeShareLinkApi.getAll()
    links.value = res.data.data
  } finally { loading.value = false }
}

function copyLink(row) {
  const url = `${window.location.origin}/#/finance/steuerberater/${row.token}`
  copyToClipboard(url).then(() => $q.notify({ type: 'positive', message: 'Link copied!' }))
}

function openLink(row) {
  window.open(`/#/finance/steuerberater/${row.token}`, '_blank')
}

async function toggleLink(row) {
  try {
    await financeShareLinkApi.toggleActive(row.id)
    await loadLinks()
  } catch { $q.notify({ type: 'negative', message: 'Failed' }) }
}

function confirmDelete(row) {
  $q.dialog({ title: 'Delete Link', message: 'Delete this share link?', cancel: true, color: 'red' })
    .onOk(async () => {
      try { await financeShareLinkApi.delete(row.id); await loadLinks() }
      catch { $q.notify({ type: 'negative', message: 'Failed' }) }
    })
}

async function handleCreate() {
  creating.value = true
  try {
    await financeShareLinkApi.create(createForm.value)
    showCreate.value = false
    createForm.value = { monthlyReportId: null, recipientName: '', recipientEmail: '', expiresAt: null }
    await loadLinks()
    $q.notify({ type: 'positive', message: 'Share link created' })
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' })
  } finally { creating.value = false }
}

onMounted(async () => {
  await financeStore.fetchReports()
  reports.value = financeStore.reports
  await loadLinks()
})
</script>
