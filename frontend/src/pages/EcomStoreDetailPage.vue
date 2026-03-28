<template>
  <q-page padding>
    <div class="row items-center q-mb-lg">
      <q-btn flat icon="arrow_back" color="grey-5" to="/ecommerce/stores" class="q-mr-sm" />
      <div>
        <div class="text-h5 text-white text-weight-light">{{ store?.name || 'Store Detail' }}</div>
        <div class="text-caption text-grey-5">Store details and member management</div>
      </div>
      <q-space />
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
    </template>

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
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { ecomStoreApi, ecomStoreMemberApi } from 'src/api/ecommerce'
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

onMounted(() => {
  loadStore()
  loadMembers()
  loadUsers()
})
</script>
