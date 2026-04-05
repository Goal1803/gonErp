<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/ecommerce/stores" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">{{ store?.name || 'Store Detail' }}</div>
        <div class="text-caption text-grey-5">Store details and member management</div>
      </div>
      <q-space />
      <q-btn v-if="canSeeDashboard" unelevated color="cyan-7" icon="bar_chart" label="Dashboard" no-caps :to="`/ecommerce/stores/${$route.params.id}/dashboard`" />
    </div>

    <q-inner-loading :showing="loadingStore" color="cyan-5" />

    <template v-if="store">
      <!-- Store Info Section -->
      <q-card flat bordered class="erp-table q-mb-lg">
        <q-card-section>
          <div class="row items-center q-mb-md">
            <div class="text-h6 text-white">Store Info</div>
            <q-space />
            <q-btn v-if="authStore.isAdmin" flat icon="edit" color="blue-4" no-caps label="Edit" @click="openEditDialog" />
          </div>
          <div class="row q-col-gutter-md">
            <div class="col-6 col-md-3">
              <div class="text-caption text-grey-5">Name</div>
              <div class="text-white">{{ store.name }}</div>
            </div>
            <div class="col-6 col-md-3">
              <div class="text-caption text-grey-5">Sales Channel</div>
              <div class="text-white">{{ store.salesChannel || '-' }}</div>
            </div>
            <div class="col-6 col-md-2">
              <div class="text-caption text-grey-5">Currency</div>
              <div class="text-white">{{ store.currency || '-' }}</div>
            </div>
            <div class="col-6 col-md-2">
              <div class="text-caption text-grey-5">URL</div>
              <div class="text-white">{{ store.storeUrl || '-' }}</div>
            </div>
            <div class="col-6 col-md-2">
              <div class="text-caption text-grey-5">Status</div>
              <q-badge :color="store.active ? 'green-7' : 'grey-7'" :label="store.active ? 'Active' : 'Inactive'" />
            </div>
          </div>
        </q-card-section>
      </q-card>

      <!-- Members Section -->
      <q-card flat bordered class="erp-table">
        <q-card-section>
          <div class="row items-center q-mb-md">
            <div class="text-h6 text-white">Members</div>
            <q-space />
            <q-btn v-if="authStore.isAdmin" unelevated color="cyan-7" icon="person_add" label="Assign Member" no-caps @click="showAssignDialog = true" />
          </div>

          <q-table
            :rows="members"
            :columns="memberColumns"
            row-key="userId"
            flat
            bordered
            :loading="loadingMembers"
            class="erp-table"
            :pagination="{ rowsPerPage: 20 }"
          >
            <template v-slot:body-cell-user="props">
              <q-td :props="props">
                <div class="row items-center no-wrap q-gutter-sm">
                  <q-avatar size="28px" color="cyan-9" text-color="white">
                    {{ (props.row.firstName || props.row.userName || '?')[0].toUpperCase() }}
                  </q-avatar>
                  <div>
                    <div class="text-white text-weight-medium">{{ props.row.firstName }} {{ props.row.lastName }}</div>
                    <div class="text-caption text-grey-5">{{ props.row.userName }}</div>
                  </div>
                </div>
              </q-td>
            </template>
            <template v-slot:body-cell-role="props">
              <q-td :props="props">
                <q-badge :color="roleColor(props.row.storeRole)" :label="roleLabel(props.row.storeRole)" />
              </q-td>
            </template>
            <template v-slot:body-cell-actions="props">
              <q-td :props="props">
                <q-btn v-if="authStore.isAdmin" flat dense icon="delete" color="red-4" @click="confirmRemoveMember(props.row)">
                  <q-tooltip>Remove member</q-tooltip>
                </q-btn>
              </q-td>
            </template>
          </q-table>
        </q-card-section>
      </q-card>
      <!-- Orders Section -->
      <q-card flat bordered class="erp-table q-mt-lg">
        <q-card-section>
          <div class="row items-center q-mb-md">
            <div class="text-h6 text-white">Orders</div>
            <q-space />
            <q-btn
              v-if="selectedStoreOrders.length"
              unelevated color="teal-7" icon="sync"
              :label="`Sync ${selectedStoreOrders.length} to Board`"
              no-caps class="q-mr-sm"
              @click="showOrderSyncDialog = true"
            />
            <q-btn unelevated color="cyan-7" icon="file_upload" label="Import" no-caps @click="showOrderImport = true" />
          </div>

          <q-table
            :rows="storeOrders"
            :columns="orderColumns"
            row-key="id"
            flat bordered dense
            :loading="loadingOrders"
            class="erp-table"
            :pagination="{ rowsPerPage: 20 }"
            selection="multiple"
            v-model:selected="selectedStoreOrders"
            @row-click="(evt, row) => $router.push(`/ecommerce/orders/${row.id}`)"
          >
            <template v-slot:body-cell-synced="props">
              <q-td :props="props">
                <q-icon v-if="props.row.cardId" name="link" color="teal-4" size="sm">
                  <q-tooltip>Synced to card</q-tooltip>
                </q-icon>
                <q-icon v-else name="link_off" color="grey-7" size="sm" />
              </q-td>
            </template>
            <template v-slot:body-cell-status="props">
              <q-td :props="props">
                <q-badge :color="orderStatusColor(props.row.status)" :label="(props.row.status || '').replace(/_/g, ' ')" />
              </q-td>
            </template>
            <template v-slot:body-cell-earningAfterPlatformFee="props">
              <q-td :props="props" class="text-right">
                <span :class="props.row.earningAfterPlatformFee != null ? 'text-cyan-4' : 'text-grey-7'">
                  {{ props.row.earningAfterPlatformFee != null ? Number(props.row.earningAfterPlatformFee).toFixed(2) : '-' }}
                </span>
              </q-td>
            </template>
            <template v-slot:body-cell-refundAmount="props">
              <q-td :props="props" class="text-right">
                <span v-if="props.row.refundAmount" class="text-red-4">-{{ Number(props.row.refundAmount).toFixed(2) }}</span>
                <span v-else class="text-grey-7">-</span>
              </q-td>
            </template>
            <template v-slot:body-cell-fulfillmentCost="props">
              <q-td :props="props" class="text-right">
                <span :class="props.row.fulfillmentCost != null ? 'text-orange-4' : 'text-grey-7'">
                  {{ props.row.fulfillmentCost != null ? Number(props.row.fulfillmentCost).toFixed(2) : '-' }}
                </span>
              </q-td>
            </template>
            <template v-slot:body-cell-grossProfit="props">
              <q-td :props="props" class="text-right text-weight-medium">
                <span v-if="props.row.grossProfit != null" :class="props.row.grossProfit >= 0 ? 'text-green-4' : 'text-red-4'">
                  {{ Number(props.row.grossProfit).toFixed(2) }}
                </span>
                <span v-else class="text-grey-7">-</span>
              </q-td>
            </template>
          </q-table>
        </q-card-section>
      </q-card>

      <!-- Etsy Transactions Section -->
      <q-card flat bordered class="erp-table q-mt-lg">
        <q-card-section>
          <div class="row items-center q-mb-md">
            <div class="text-h6 text-white">Etsy Transactions</div>
            <q-space />
            <q-btn unelevated color="teal-7" icon="sync" label="Match Fees" no-caps class="q-mr-sm" @click="handleMatchFees" :loading="matchingFees" />
            <q-btn unelevated color="cyan-7" icon="file_upload" label="Upload Statement" no-caps @click="showUploadTxn = true" />
          </div>

          <!-- Filters -->
          <div class="row q-gutter-sm q-mb-sm items-center">
            <q-select v-model="txnFilters.type" :options="txnTypeOptions" label="Type" dense outlined dark emit-value map-options clearable style="min-width: 120px" />
            <q-select v-model="txnFilters.matched" :options="txnMatchedOptions" label="Matched" dense outlined dark emit-value map-options clearable style="min-width: 120px" />
            <q-input v-model="txnFilters.search" label="Search" dense outlined dark clearable debounce="300" style="min-width: 180px">
              <template v-slot:prepend><q-icon name="search" color="grey-5" /></template>
            </q-input>
            <q-space />
            <div class="text-caption text-grey-5">{{ filteredTxns.length }} / {{ transactions.length }}</div>
          </div>

          <q-table
            :rows="filteredTxns"
            :columns="txnColumns"
            row-key="id"
            flat bordered dense
            :loading="loadingTxns"
            class="erp-table"
            :pagination="{ rowsPerPage: 30 }"
          >
            <template v-slot:body-cell-type="props">
              <q-td :props="props">
                <q-badge :color="txnTypeColor(props.row.type)" :label="props.row.type" />
              </q-td>
            </template>
            <template v-slot:body-cell-feesAndTaxes="props">
              <q-td :props="props" class="text-right">
                <span :class="props.row.feesAndTaxes && props.row.feesAndTaxes < 0 ? 'text-red-4' : ''">
                  {{ props.row.feesAndTaxes != null ? Number(props.row.feesAndTaxes).toFixed(2) : '-' }}
                </span>
              </q-td>
            </template>
            <template v-slot:body-cell-amount="props">
              <q-td :props="props" class="text-right">
                <span :class="props.row.amount && props.row.amount > 0 ? 'text-green-4' : ''">
                  {{ props.row.amount != null ? Number(props.row.amount).toFixed(2) : '-' }}
                </span>
              </q-td>
            </template>
            <template v-slot:body-cell-net="props">
              <q-td :props="props" class="text-right text-weight-medium">
                {{ props.row.net != null ? Number(props.row.net).toFixed(2) : '-' }}
              </q-td>
            </template>
            <template v-slot:body-cell-matched="props">
              <q-td :props="props">
                <q-icon v-if="props.row.matched" name="check_circle" color="green-4" size="sm" />
                <q-icon v-else-if="props.row.orderIdRef" name="radio_button_unchecked" color="orange-4" size="sm" />
                <span v-else class="text-grey-7">-</span>
              </q-td>
            </template>
          </q-table>
        </q-card-section>
      </q-card>
    </template>

    <!-- Order Import Dialog -->
    <q-dialog v-model="showOrderImport" persistent>
      <q-card style="min-width: 520px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Import Etsy Orders</div>
        </q-card-section>
        <q-card-section class="q-gutter-md" v-if="!orderImportResult">
          <q-file v-model="orderImportForm.ordersFile" label="Etsy Sold Orders CSV" dense outlined dark accept=".csv">
            <template v-slot:prepend><q-icon name="attach_file" color="grey-5" /></template>
          </q-file>
          <q-file v-model="orderImportForm.itemsFile" label="Etsy Sold Order Items CSV" dense outlined dark accept=".csv">
            <template v-slot:prepend><q-icon name="attach_file" color="grey-5" /></template>
          </q-file>
          <q-separator dark />
          <q-toggle v-model="orderImportForm.syncToBoard" label="Sync new orders to POD Order board" color="cyan-5" />
          <q-select
            v-if="orderImportForm.syncToBoard"
            v-model="orderImportForm.boardId"
            :options="podOrderBoardOptions"
            label="Select POD Order Board"
            dense outlined dark emit-value map-options
          />
          <div v-if="orderImportForm.syncToBoard" class="text-caption text-cyan-5" style="background: rgba(0,188,212,0.08); border-radius: 6px; padding: 8px; border: 1px solid rgba(0,188,212,0.15);">
            Each new order will create a card in the Draft column. Both files required when syncing.
          </div>
          <div class="text-caption text-grey-5">Upload at least one file. You can import orders and items separately.</div>
        </q-card-section>
        <q-card-section v-if="orderImportResult">
          <div class="row q-gutter-sm q-mb-md">
            <q-badge color="green-7" class="q-pa-sm text-body2">{{ orderImportResult.ordersCreated || 0 }} Created</q-badge>
            <q-badge color="cyan-7" class="q-pa-sm text-body2">{{ orderImportResult.ordersUpdated || 0 }} Updated</q-badge>
            <q-badge color="orange-7" class="q-pa-sm text-body2">{{ orderImportResult.skipped || 0 }} Skipped</q-badge>
            <q-badge v-if="orderImportResult.errors && orderImportResult.errors.length" color="red-7" class="q-pa-sm text-body2">{{ orderImportResult.errors.length }} Errors</q-badge>
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Close" color="grey-5" v-close-popup no-caps @click="resetOrderImport" />
          <q-btn v-if="orderImportResult" flat label="Import More" color="cyan-7" no-caps @click="orderImportResult = null" />
          <q-btn
            v-if="!orderImportResult"
            unelevated label="Import" color="cyan-7" no-caps
            @click="handleOrderImport" :loading="importingOrders"
            :disable="(!orderImportForm.ordersFile && !orderImportForm.itemsFile) || (orderImportForm.syncToBoard && (!orderImportForm.boardId || !orderImportForm.ordersFile || !orderImportForm.itemsFile))"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Order Sync to Board Dialog -->
    <q-dialog v-model="showOrderSyncDialog">
      <q-card style="min-width: 420px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Sync Orders to Board</div>
          <div class="text-caption text-grey-5">{{ syncableStoreOrders.length }} of {{ selectedStoreOrders.length }} selected can be synced</div>
        </q-card-section>
        <q-card-section>
          <q-select v-model="syncBoardId" :options="podOrderBoardOptions" label="POD Order Board *" dense outlined dark emit-value map-options />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated label="Sync" color="teal-7" no-caps @click="handleOrderSync" :loading="syncingOrders" :disable="!syncBoardId || !syncableStoreOrders.length" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Upload Transaction Dialog -->
    <q-dialog v-model="showUploadTxn">
      <q-card style="min-width: 450px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Upload Etsy Statement</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-file v-model="txnFile" label="Etsy Statement CSV *" dense outlined dark accept=".csv">
            <template v-slot:prepend><q-icon name="attach_file" color="grey-5" /></template>
          </q-file>
          <div class="text-caption text-grey-5">You can re-upload the same or growing monthly file — duplicates are automatically skipped.</div>
          <div v-if="txnUploadResult" class="q-pa-md" style="background: var(--erp-bg-tertiary); border-radius: 8px;">
            <div class="text-green-4">Inserted: {{ txnUploadResult.inserted }}</div>
            <div class="text-orange-4">Skipped: {{ txnUploadResult.skipped }}</div>
            <div v-if="txnUploadResult.errors" class="text-red-4">Errors: {{ txnUploadResult.errors }}</div>
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Close" color="grey-5" v-close-popup no-caps @click="txnUploadResult = null; txnFile = null" />
          <q-btn unelevated label="Upload" color="cyan-7" no-caps @click="handleUploadTxn" :loading="uploadingTxn" :disable="!txnFile" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Match Result Dialog -->
    <q-dialog v-model="showMatchResult">
      <q-card style="min-width: 400px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Fee Matching Result</div>
        </q-card-section>
        <q-card-section v-if="matchResult">
          <div class="text-green-4 q-mb-xs">Matched: {{ matchResult.matched }} orders</div>
          <div class="text-orange-4 q-mb-xs">No order found: {{ matchResult.skippedNoOrder }}</div>
          <div class="text-cyan-4 q-mb-xs">Already has fee: {{ matchResult.skippedAlreadyHasFee }}</div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Close" color="grey-5" v-close-popup no-caps />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Edit Store Dialog -->
    <q-dialog v-model="showEditDialog">
      <q-card style="min-width: 500px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Edit Store</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-input v-model="editForm.name" label="Name *" dense outlined dark />
          <q-select
            v-model="editForm.salesChannel"
            :options="salesChannelOptions"
            label="Sales Channel"
            dense outlined dark
            emit-value map-options
          />
          <q-input v-model="editForm.currency" label="Currency" dense outlined dark />
          <q-input v-model="editForm.storeUrl" label="Store URL" dense outlined dark />
          <q-toggle v-model="editForm.active" label="Active" color="cyan-5" />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated label="Save" color="cyan-7" no-caps @click="handleEditSave" :loading="savingEdit" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Assign Member Dialog -->
    <q-dialog v-model="showAssignDialog">
      <q-card style="min-width: 400px; background: var(--erp-bg-elevated); border: 1px solid var(--erp-border);">
        <q-card-section>
          <div class="text-h6 text-white">Assign Member</div>
        </q-card-section>
        <q-card-section class="q-gutter-md">
          <q-select
            v-model="assignForm.userId"
            :options="availableUsers"
            option-value="id"
            option-label="label"
            emit-value
            map-options
            label="User"
            dense outlined dark
            use-input
            @filter="filterUsers"
          />
          <q-select
            v-model="assignForm.storeRole"
            :options="roleOptions"
            emit-value
            map-options
            label="Role"
            dense outlined dark
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup no-caps />
          <q-btn unelevated label="Assign" color="cyan-7" no-caps @click="handleAssign" :loading="assigning" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { ecomStoreApi, ecomStoreMemberApi, ecomTransactionApi, ecomOrderApi, ecomImportApi } from 'src/api/ecommerce'
import { boardApi } from 'src/api/tasks'
import { api } from 'src/boot/axios'

const route = useRoute()
const $q = useQuasar()
const authStore = useAuthStore()

const store = ref(null)
const loadingStore = ref(false)
const members = ref([])
const loadingMembers = ref(false)

// Edit store
const showEditDialog = ref(false)
const savingEdit = ref(false)
const editForm = ref({ name: '', salesChannel: 'Etsy', currency: 'EUR', storeUrl: '', active: true })

// Assign member
const showAssignDialog = ref(false)
const assigning = ref(false)
const assignForm = ref({ userId: null, storeRole: 'MEMBER' })
const allUsers = ref([])
const availableUsers = ref([])

const DASHBOARD_ROLES = ['STORE_ADMIN', 'SELLER', 'SELLER_SUPPORT', 'FULFILLMENT_STAFF']
const canSeeDashboard = computed(() => {
  if (authStore.isSuperAdmin || authStore.isAdmin) return true
  const me = members.value.find(m => m.userId === authStore.currentUser?.userId)
  return me && DASHBOARD_ROLES.includes(me.storeRole)
})

const salesChannelOptions = [
  { label: 'Etsy', value: 'Etsy' },
  { label: 'Amazon', value: 'Amazon' },
  { label: 'eBay', value: 'eBay' },
  { label: 'Walmart', value: 'Walmart' },
  { label: 'Shopify', value: 'Shopify' },
  { label: 'Other', value: 'Other' }
]

const roleOptions = [
  { label: 'Store Admin', value: 'STORE_ADMIN' },
  { label: 'Seller', value: 'SELLER' },
  { label: 'Seller Support', value: 'SELLER_SUPPORT' },
  { label: 'Fulfillment Staff', value: 'FULFILLMENT_STAFF' },
  { label: 'Designer', value: 'DESIGNER' },
  { label: 'Member', value: 'MEMBER' }
]

const memberColumns = [
  { name: 'user', label: 'User', field: 'userName', align: 'left', sortable: true },
  { name: 'role', label: 'Role', field: 'storeRole', align: 'center' },
  { name: 'actions', label: '', field: 'actions', align: 'right' }
]

function roleColor (role) {
  const map = {
    STORE_ADMIN: 'purple-7',
    SELLER: 'blue-7',
    SELLER_SUPPORT: 'blue-7',
    FULFILLMENT_STAFF: 'orange-7',
    DESIGNER: 'teal-7',
    MEMBER: 'grey-7'
  }
  return map[role] || 'grey-7'
}

function roleLabel (role) {
  const map = {
    STORE_ADMIN: 'Admin',
    SELLER: 'Seller',
    SELLER_SUPPORT: 'Seller Support',
    FULFILLMENT_STAFF: 'Fulfillment',
    DESIGNER: 'Designer',
    MEMBER: 'Member'
  }
  return map[role] || role
}

function openEditDialog () {
  editForm.value = {
    name: store.value.name || '',
    salesChannel: store.value.salesChannel || 'Etsy',
    currency: store.value.currency || 'EUR',
    storeUrl: store.value.storeUrl || '',
    active: store.value.active ?? true
  }
  showEditDialog.value = true
}

async function handleEditSave () {
  if (!editForm.value.name) {
    $q.notify({ type: 'warning', message: 'Name is required' })
    return
  }
  savingEdit.value = true
  try {
    await ecomStoreApi.update(route.params.id, editForm.value)
    showEditDialog.value = false
    $q.notify({ type: 'positive', message: 'Store updated' })
    await loadStore()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed to update' })
  } finally {
    savingEdit.value = false
  }
}

async function loadStore () {
  loadingStore.value = true
  try {
    const res = await ecomStoreApi.getById(route.params.id)
    store.value = res.data.data
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to load store' })
  } finally {
    loadingStore.value = false
  }
}

async function loadMembers () {
  loadingMembers.value = true
  try {
    const res = await ecomStoreMemberApi.getByStore(route.params.id)
    members.value = res.data.data || []
  } catch (e) {
    $q.notify({ type: 'negative', message: 'Failed to load members' })
  } finally {
    loadingMembers.value = false
  }
}

async function loadUsers () {
  try {
    const res = await api.get('/users', { params: { size: 200 } })
    const users = res.data.data?.content || res.data.data || []
    allUsers.value = users.map(u => ({
      id: u.id,
      label: `${u.firstName || ''} ${u.lastName || ''} (${u.userName})`.trim()
    }))
    availableUsers.value = allUsers.value
  } catch { /* ignore */ }
}

function filterUsers (val, update) {
  update(() => {
    const needle = val.toLowerCase()
    availableUsers.value = allUsers.value.filter(u => u.label.toLowerCase().includes(needle))
  })
}

function confirmRemoveMember (row) {
  $q.dialog({
    title: 'Remove Member',
    message: `Remove ${row.firstName || ''} ${row.lastName || ''} from this store?`,
    cancel: true,
    color: 'red'
  }).onOk(async () => {
    try {
      await ecomStoreMemberApi.remove(route.params.id, row.userId)
      await loadMembers()
      $q.notify({ type: 'positive', message: 'Member removed' })
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' })
    }
  })
}

async function handleAssign () {
  if (!assignForm.value.userId) {
    $q.notify({ type: 'warning', message: 'Select a user' })
    return
  }
  assigning.value = true
  try {
    await ecomStoreMemberApi.assign(route.params.id, assignForm.value)
    showAssignDialog.value = false
    await loadMembers()
    $q.notify({ type: 'positive', message: 'Member assigned' })
    assignForm.value = { userId: null, storeRole: 'MEMBER' }
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Failed' })
  } finally {
    assigning.value = false
  }
}

// === Store Orders ===
const storeOrders = ref([])
const loadingOrders = ref(false)
const showOrderImport = ref(false)
const importingOrders = ref(false)
const orderImportResult = ref(null)
const orderImportForm = ref({ ordersFile: null, itemsFile: null, syncToBoard: false, boardId: null })
const podOrderBoardOptions = ref([])

const selectedStoreOrders = ref([])
const showOrderSyncDialog = ref(false)
const syncBoardId = ref(null)
const syncingOrders = ref(false)

const orderColumns = [
  { name: 'synced', label: '', field: 'cardId', align: 'center', style: 'width: 40px' },
  { name: 'orderDate', label: 'Date', field: 'orderDate', align: 'left', sortable: true, format: v => v ? new Date(v).toLocaleDateString() : '' },
  { name: 'platformOrderId', label: 'Order ID', field: 'platformOrderId', align: 'left', sortable: true },
  { name: 'customerName', label: 'Customer', field: 'customerName', align: 'left', sortable: true },
  { name: 'sku', label: 'SKU', field: row => row.items?.[0]?.sku || row.sku || '', align: 'left' },
  { name: 'numberOfItems', label: 'Items', field: 'numberOfItems', align: 'center' },
  { name: 'orderTotal', label: 'Revenue', field: 'orderTotal', align: 'right', sortable: true, format: v => v != null ? Number(v).toFixed(2) : '' },
  { name: 'earningAfterPlatformFee', label: 'Earning', field: 'earningAfterPlatformFee', align: 'right', sortable: true },
  { name: 'refundAmount', label: 'Refund', field: 'refundAmount', align: 'right', sortable: true },
  { name: 'fulfillmentCost', label: 'Fulfillment', field: 'fulfillmentCost', align: 'right', sortable: true },
  { name: 'grossProfit', label: 'Profit', field: 'grossProfit', align: 'right', sortable: true },
  { name: 'status', label: 'Status', field: 'status', align: 'center', sortable: true }
]

const syncableStoreOrders = computed(() =>
  selectedStoreOrders.value.filter(o => !o.cardId && o.items && o.items.length && o.items.some(i => i.productName || i.platformItemId))
)

async function handleOrderSync () {
  if (!syncBoardId.value || !syncableStoreOrders.value.length) return
  syncingOrders.value = true
  try {
    const ids = syncableStoreOrders.value.map(o => o.id)
    const res = await ecomOrderApi.syncToBoard(syncBoardId.value, ids)
    const r = res.data.data
    showOrderSyncDialog.value = false
    selectedStoreOrders.value = []
    $q.notify({ type: 'positive', message: `Synced ${r.synced} orders, ${r.skipped} skipped` })
    loadStoreOrders()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Sync failed' })
  } finally {
    syncingOrders.value = false
  }
}

function orderStatusColor (status) {
  return { NEW_ORDER: 'blue-7', CONFIRMED: 'green-7', DESIGNING: 'purple-7', FULFILLED: 'teal-7', COMPLETED: 'grey-7', CANCELLED: 'red-7' }[status] || 'orange-7'
}

async function loadStoreOrders () {
  loadingOrders.value = true
  try {
    const res = await ecomOrderApi.getAll({ storeId: route.params.id })
    storeOrders.value = res.data.data || []
  } catch { /* ignore */ }
  finally { loadingOrders.value = false }
}

async function loadPodOrderBoards () {
  try {
    const res = await boardApi.getAll()
    podOrderBoardOptions.value = (res.data.data || [])
      .filter(b => b.boardType === 'POD_ORDER')
      .map(b => ({ label: b.name, value: b.id }))
  } catch { /* ignore */ }
}

async function handleOrderImport () {
  const f = orderImportForm.value
  if (!f.ordersFile && !f.itemsFile) return
  importingOrders.value = true
  orderImportResult.value = null
  $q.loading.show({ message: 'Importing orders...' })
  try {
    const res = await ecomImportApi.importEtsy(
      route.params.id, f.ordersFile, f.itemsFile,
      f.syncToBoard ? f.boardId : null
    )
    orderImportResult.value = res.data.data
    const r = orderImportResult.value
    $q.notify({ type: 'positive', message: `Import done: ${r.ordersCreated || 0} created, ${r.ordersUpdated || 0} updated` })
    loadStoreOrders()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Import failed' })
  } finally {
    importingOrders.value = false
    $q.loading.hide()
  }
}

function resetOrderImport () {
  orderImportForm.value = { ordersFile: null, itemsFile: null, syncToBoard: false, boardId: null }
  orderImportResult.value = null
}

// === Etsy Transactions ===
const transactions = ref([])
const loadingTxns = ref(false)
const showUploadTxn = ref(false)
const uploadingTxn = ref(false)
const txnFile = ref(null)
const txnUploadResult = ref(null)
const matchingFees = ref(false)
const showMatchResult = ref(false)
const matchResult = ref(null)
const txnFilters = ref({ type: null, matched: null, search: '' })

const txnTypeOptions = [
  { label: 'Fee', value: 'Fee' }, { label: 'Tax', value: 'Tax' },
  { label: 'Sale', value: 'Sale' }, { label: 'Refund', value: 'Refund' },
  { label: 'Marketing', value: 'Marketing' }, { label: 'Buyer Fee', value: 'Buyer Fee' },
  { label: 'Deposit', value: 'Deposit' }
]
const txnMatchedOptions = [
  { label: 'Matched', value: 'yes' }, { label: 'Unmatched', value: 'no' }
]

const txnColumns = [
  { name: 'txnDate', label: 'Date', field: 'txnDate', align: 'left', sortable: true },
  { name: 'type', label: 'Type', field: 'type', align: 'center', sortable: true },
  { name: 'title', label: 'Title', field: 'title', align: 'left', style: 'max-width: 260px', classes: 'ellipsis' },
  { name: 'orderIdRef', label: 'Order', field: 'orderIdRef', align: 'left', sortable: true },
  { name: 'amount', label: 'Amount', field: 'amount', align: 'right', sortable: true },
  { name: 'feesAndTaxes', label: 'Fees', field: 'feesAndTaxes', align: 'right', sortable: true },
  { name: 'net', label: 'Net', field: 'net', align: 'right', sortable: true },
  { name: 'matched', label: '', field: 'matched', align: 'center', sortable: true }
]

function txnTypeColor (type) {
  return { Fee: 'red-7', Tax: 'orange-7', Sale: 'green-7', Refund: 'purple-7', Marketing: 'blue-7', 'Buyer Fee': 'amber-8', Deposit: 'teal-7' }[type] || 'grey-7'
}

const filteredTxns = computed(() => {
  return transactions.value.filter(t => {
    if (txnFilters.value.type && t.type !== txnFilters.value.type) return false
    if (txnFilters.value.matched === 'yes' && !t.matched) return false
    if (txnFilters.value.matched === 'no' && t.matched) return false
    if (txnFilters.value.search) {
      const q = txnFilters.value.search.toLowerCase()
      if (!(t.title || '').toLowerCase().includes(q) && !(t.orderIdRef || '').includes(q) && !(t.info || '').toLowerCase().includes(q)) return false
    }
    return true
  })
})

async function loadTransactions () {
  loadingTxns.value = true
  try {
    const res = await ecomTransactionApi.getAll(route.params.id)
    transactions.value = res.data.data || []
  } catch { /* ignore */ }
  finally { loadingTxns.value = false }
}

async function handleUploadTxn () {
  if (!txnFile.value) return
  uploadingTxn.value = true
  txnUploadResult.value = null
  try {
    const res = await ecomTransactionApi.upload(route.params.id, txnFile.value)
    txnUploadResult.value = res.data.data
    $q.notify({ type: 'positive', message: `Uploaded: ${txnUploadResult.value.inserted} new, ${txnUploadResult.value.skipped} skipped` })
    loadTransactions()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Upload failed' })
  } finally { uploadingTxn.value = false }
}

async function handleMatchFees () {
  matchingFees.value = true
  try {
    const res = await ecomTransactionApi.matchFees(route.params.id)
    matchResult.value = res.data.data
    showMatchResult.value = true
    loadTransactions()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Matching failed' })
  } finally { matchingFees.value = false }
}

onMounted(() => {
  loadStore()
  loadMembers()
  loadUsers()
  loadStoreOrders()
  loadTransactions()
  loadPodOrderBoards()
})
</script>
