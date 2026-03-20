<template>
  <div class="steuerberater-page">
    <!-- Standalone header (no MainLayout) -->
    <div class="sb-header q-pa-md">
      <div class="row items-center">
        <q-icon name="account_balance" color="green-5" size="md" class="q-mr-sm" />
        <div>
          <div class="text-h6 text-white">gonERP Finance</div>
          <div class="text-caption text-grey-5">Shared Monthly Report</div>
        </div>
        <q-space />
        <q-btn flat icon="download" color="green-5" label="Download CSV" no-caps @click="downloadCsv" />
      </div>
    </div>

    <div v-if="error" class="q-pa-xl text-center">
      <q-icon name="error" color="red-4" size="64px" />
      <div class="text-h6 text-white q-mt-md">{{ error }}</div>
    </div>

    <div v-else class="q-pa-md">
      <!-- Report Info -->
      <div v-if="report" class="q-mb-md">
        <div class="text-h5 text-white">{{ monthNames[report.month] }} {{ report.year }}</div>
        <q-badge :color="statusColor(report.status)" :label="report.status" />
      </div>

      <!-- Account Tabs -->
      <q-tabs v-model="activeTab" dense align="left" class="q-mb-md" active-color="green-5" indicator-color="green-5">
        <q-tab v-for="acc in accounts" :key="acc.id" :name="acc.id" :label="acc.name" no-caps />
      </q-tabs>

      <!-- Transactions Table (read-only) -->
      <q-table
        :rows="transactions"
        :columns="tableColumns"
        row-key="id"
        flat bordered dense
        :loading="loadingTx"
        class="erp-table"
        :pagination="{ rowsPerPage: 50 }"
      >
        <template v-slot:body-cell-amount="props">
          <q-td :props="props">
            <span :class="props.row.amount < 0 ? 'text-red-4' : 'text-green-4'">
              {{ props.row.amount != null ? Number(props.row.amount).toFixed(2) : '' }}
            </span>
          </q-td>
        </template>
      </q-table>

      <!-- Invoices -->
      <div v-if="invoices.length" class="q-mt-lg">
        <div class="text-h6 text-white q-mb-sm">Invoices</div>
        <q-list bordered class="rounded-borders">
          <q-item v-for="inv in invoices" :key="inv.id">
            <q-item-section avatar><q-icon name="receipt" color="blue-4" /></q-item-section>
            <q-item-section>
              <q-item-label class="text-white">{{ inv.originalFilename }}</q-item-label>
              <q-item-label caption>{{ inv.invoiceType }} {{ inv.description ? '- ' + inv.description : '' }}</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-btn flat dense icon="download" color="green-5" :href="inv.storageUrl" target="_blank" />
            </q-item-section>
          </q-item>
        </q-list>
      </div>

      <!-- Comments -->
      <div class="q-mt-lg">
        <div class="text-h6 text-white q-mb-sm">Comments</div>
        <div v-if="comments.length" class="q-mb-md">
          <q-chat-message
            v-for="c in comments" :key="c.id"
            :name="c.authorName"
            :text="[c.commentText]"
            :stamp="new Date(c.createdAt).toLocaleString()"
            bg-color="grey-9"
            text-color="white"
          />
        </div>
        <div class="row q-gutter-sm">
          <q-input v-model="commentAuthor" label="Your name" dense outlined dark class="col-3" />
          <q-input v-model="commentText" label="Add a comment..." dense outlined dark class="col" />
          <q-btn unelevated color="green-7" icon="send" no-caps @click="addComment" :disable="!commentText || !commentAuthor" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import { steuerberaterApi } from 'src/api/finance'

const $q = useQuasar()
const route = useRoute()
const token = computed(() => route.params.token)

const report = ref(null)
const accounts = ref([])
const transactions = ref([])
const invoices = ref([])
const comments = ref([])
const loadingTx = ref(false)
const activeTab = ref(null)
const error = ref(null)
const commentText = ref('')
const commentAuthor = ref('')

const monthNames = ['', 'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']
function statusColor(s) {
  return { DRAFT: 'blue-grey-7', REVIEWED: 'blue-7', SHARED: 'green-7', ARCHIVED: 'grey-7' }[s] || 'grey-7'
}

const tableColumns = computed(() => {
  if (!transactions.value.length) return []
  const rawKeys = transactions.value[0]?.rawData ? Object.keys(transactions.value[0].rawData) : []
  const cols = rawKeys.map(k => ({
    name: k, label: k, field: row => row.rawData?.[k] || '', align: 'left', sortable: true
  }))
  cols.push(
    { name: 'amount', label: 'Amount', field: 'amount', align: 'right', sortable: true },
    { name: 'note', label: 'Note', field: 'note', align: 'left' },
    { name: 'category', label: 'Category', field: 'category', align: 'left' }
  )
  return cols
})

async function loadData() {
  try {
    const [reportRes, accountsRes, invoicesRes, commentsRes] = await Promise.all([
      steuerberaterApi.getReport(token.value),
      steuerberaterApi.getAccounts(token.value),
      steuerberaterApi.getInvoices(token.value),
      steuerberaterApi.getComments(token.value)
    ])
    report.value = reportRes.data.data
    accounts.value = accountsRes.data.data
    invoices.value = invoicesRes.data.data
    comments.value = commentsRes.data.data

    if (accounts.value.length) activeTab.value = accounts.value[0].id
  } catch (e) {
    error.value = e.response?.data?.message || 'Invalid or expired link'
  }
}

async function loadTransactions() {
  if (!activeTab.value) return
  loadingTx.value = true
  try {
    const res = await steuerberaterApi.getTransactions(token.value, activeTab.value)
    transactions.value = res.data.data
  } finally { loadingTx.value = false }
}

watch(activeTab, loadTransactions)

async function addComment() {
  if (!commentText.value || !commentAuthor.value) return
  try {
    await steuerberaterApi.addComment(token.value, {
      commentText: commentText.value, authorName: commentAuthor.value
    })
    commentText.value = ''
    const res = await steuerberaterApi.getComments(token.value)
    comments.value = res.data.data
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to add comment' })
  }
}

async function downloadCsv() {
  try {
    const res = await steuerberaterApi.exportCsv(token.value, activeTab.value)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = 'transactions.csv'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    $q.notify({ type: 'negative', message: 'Export failed' })
  }
}

onMounted(loadData)
</script>

<style scoped>
.steuerberater-page {
  background: var(--erp-bg, #1a1a2e);
  min-height: 100vh;
}
.sb-header {
  background: var(--erp-bg-elevated, #16213e);
  border-bottom: 1px solid var(--erp-border, #333);
}
</style>
