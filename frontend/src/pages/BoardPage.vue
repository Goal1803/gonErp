<template>
  <q-page class="board-page">
    <!-- Board header -->
    <div class="board-header row items-center no-wrap q-px-lg q-py-sm">
      <q-btn flat round dense icon="arrow_back" color="grey-5" @click="goBack" />

      <div class="q-ml-sm">
        <div class="text-adaptive text-weight-medium" style="font-size:1.05rem">
          {{ boardStore.board?.name }}
        </div>
      </div>

      <div class="q-ml-sm" v-if="boardStore.board?.coverColor">
        <div style="width:14px; height:14px; border-radius:3px"
          :style="{ background: boardStore.board.coverColor }" />
      </div>

      <q-space />

      <!-- Card search -->
      <div class="relative-position q-mr-sm">
        <q-input
          v-model="searchQuery"
          dense
          outlined
          color="teal-5"
          placeholder="Search cards..."
          style="width: 220px"
          clearable
          @focus="searchFocused = true"
          @blur="onSearchBlur"
        >
          <template #prepend>
            <q-icon name="search" size="xs" />
          </template>
        </q-input>
        <q-list v-if="searchFocused && searchQuery && searchResults.length"
          bordered class="search-results absolute">
          <q-item v-for="card in searchResults" :key="card.id"
            clickable v-ripple dense @mousedown.prevent="openSearchResult(card)">
            <q-item-section avatar style="min-width:32px">
              <q-icon :name="card.archived ? 'inventory_2' : 'credit_card'" size="xs" :color="card.archived ? 'orange-4' : 'grey-6'" />
            </q-item-section>
            <q-item-section>
              <q-item-label :class="{ 'text-teal-4 text-weight-bold': card._exact }">
                {{ card.name }}
                <q-badge v-if="card.archived" color="orange-8" label="Archived" class="q-ml-xs" style="font-size:9px" />
              </q-item-label>
              <q-item-label caption class="text-grey-6 ellipsis">
                {{ card._colName }}
              </q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
        <div v-if="searchFocused && searchQuery && !searchResults.length"
          class="search-results absolute q-pa-sm text-adaptive-secondary text-caption">
          No cards found
        </div>
      </div>

      <!-- Live indicator -->
      <q-chip v-if="isConnected" dense color="teal-9" text-color="teal-3" size="xs" icon="circle"
        class="q-mr-sm" style="font-size:0.68rem">Live</q-chip>

      <q-btn v-if="isPodDesign" flat dense icon="bar_chart" label="Dashboard" color="amber-5" size="sm"
        class="q-mr-sm" :to="`/tasks/${boardStore.board?.id}/dashboard`" />
      <q-btn flat dense icon="group" label="Members" color="grey-5" size="sm"
        class="q-mr-sm" @click="showMembers = true" />
      <q-btn flat dense icon="edit" color="grey-5" size="sm"
        @click="showEditBoard = true" />
    </div>

    <!-- Filter bar -->
    <div class="filter-bar row items-center no-wrap q-px-lg q-py-xs q-gutter-sm">
      <q-btn flat dense :icon="showFilters ? 'filter_alt_off' : 'filter_alt'" :color="hasActiveFilters ? 'teal-4' : 'grey-5'" size="sm"
        @click="showFilters = !showFilters">
        <q-tooltip>{{ showFilters ? 'Hide filters' : 'Show filters' }}</q-tooltip>
      </q-btn>
      <template v-if="showFilters">
        <q-select v-model="filterMembers" :options="memberOptions" option-value="id" option-label="displayName"
          emit-value map-options multiple dense outlined color="teal-5" label="Members"
          style="min-width:150px" use-chips stack-label />
        <q-select v-model="filterLabels" :options="labelOptions" option-value="id" option-label="name"
          emit-value map-options multiple dense outlined color="teal-5" label="Labels"
          style="min-width:150px" use-chips stack-label />
        <q-select v-model="filterTypes" :options="typeOptions" option-value="id" option-label="name"
          emit-value map-options multiple dense outlined color="teal-5" label="Types"
          style="min-width:150px" use-chips stack-label />
        <q-select v-model="filterStatuses" :options="statusOptions"
          multiple dense outlined color="teal-5" label="Status"
          style="min-width:150px" use-chips stack-label />
        <q-input v-model="filterDateFrom" type="date" dense outlined color="teal-5" label="From" stack-label
          style="min-width:140px" clearable />
        <q-input v-model="filterDateTo" type="date" dense outlined color="teal-5" label="To" stack-label
          style="min-width:140px" clearable />
        <q-btn v-if="hasActiveFilters" flat dense icon="clear_all" color="red-4" size="sm" label="Clear"
          @click="clearFilters" />
      </template>
      <q-chip v-if="hasActiveFilters && !showFilters" dense color="teal-9" text-color="teal-3" size="xs"
        style="font-size:0.68rem" removable @remove="clearFilters">
        Filtered
      </q-chip>
    </div>

    <!-- Loading -->
    <div v-if="boardStore.loading" class="flex flex-center q-py-xl">
      <q-spinner color="teal-5" size="48px" />
    </div>

    <!-- Kanban area -->
    <div v-else class="kanban-scroll" ref="kanbanScrollEl">
      <draggable
        v-model="localColumns"
        group="columns"
        item-key="id"
        handle=".col-header"
        ghost-class="col-ghost"
        drag-class="col-drag"
        @start="onDragStart"
        @end="onColumnDragEnd"
        class="kanban-inner"
      >
        <template #item="{ element: col }">
          <kanban-column
            :column="col"
            :card-filter="cardFilter"
            :selectable="showBulkActions"
            :selected-ids="selectedCardIds"
            :board-type="boardStore.board?.boardType || 'GENERAL'"
            @open-card="openCard"
            @delete="confirmDeleteColumn"
            @delete-card="confirmDeleteCard"
            @copy-card="handleCopyCard"
            @refresh="refreshBoard"
            @drag-start="onDragStart"
            @drag-end="onDragEnd"
            @assign-card="onAssignCard"
            @toggle-select-card="onToggleSelectCard"
            @download-mockups-card="onDownloadMockupsCard"
            @toggle-select-all-column="onToggleSelectAllColumn"
          />
        </template>
      </draggable>

      <!-- Add column (hidden for POD_DESIGN boards) -->
      <div v-if="!isPodDesign" class="add-col-wrap">
        <div v-if="!addingColumn">
          <q-btn flat icon="add" label="Add column" color="grey-5"
            style="white-space:nowrap" @click="addingColumn = true" />
        </div>
        <div v-else style="width:250px">
          <q-input v-model="newColTitle" outlined color="teal-5" dense
            placeholder="Column title..." autofocus
            @keyup.enter="addColumn" @keyup.escape="addingColumn = false" />
          <div class="row q-gutter-xs q-mt-xs">
            <q-btn label="Add" color="teal-6" unelevated dense size="sm" @click="addColumn" />
            <q-btn flat label="Cancel" color="grey-5" dense size="sm" @click="addingColumn = false" />
          </div>
        </div>
      </div>
    </div>

    <!-- Bulk actions bar -->
    <div v-if="showBulkActions" class="bulk-actions-bar">
      <span class="text-white">{{ selectedCardIds.size }} card(s) selected</span>
      <q-btn flat color="blue-4" icon="person_add" label="Assign Member" no-caps @click="showBulkMemberAssign = true" />
      <q-btn v-if="isPodDesign" flat color="purple-4" icon="brush" label="Assign Designer" no-caps @click="showBulkDesignerAssign = true" />
      <q-btn v-if="isPodDesign" flat color="teal-3" icon="download" label="Download Mockups" no-caps :loading="bulkDownloading" @click="doBulkDownloadMockups" />
      <q-btn flat color="amber-4" icon="swap_horiz" label="Move to Column" no-caps @click="openBulkMove" />
      <q-btn flat color="grey-5" icon="close" label="Clear" no-caps @click="selectedCardIds.clear()" />
    </div>

    <!-- Bulk member assign dialog -->
    <q-dialog v-model="showBulkMemberAssign">
      <q-card style="min-width:350px" class="bg-dark text-white">
        <q-card-section>
          <div class="text-h6">Assign Member to {{ selectedCardIds.size }} card(s)</div>
        </q-card-section>
        <q-card-section>
          <q-select
            v-model="bulkMemberUserId"
            :options="memberOptions"
            option-value="id"
            option-label="displayName"
            emit-value
            map-options
            dense
            outlined
            color="teal-5"
            label="Select member"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup />
          <q-btn flat label="Assign" color="teal-5" :disable="!bulkMemberUserId" :loading="bulkAssigning" @click="doBulkMemberAssign" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Bulk designer assign dialog (POD_DESIGN only) -->
    <q-dialog v-model="showBulkDesignerAssign">
      <q-card style="min-width:350px" class="bg-dark text-white">
        <q-card-section>
          <div class="text-h6">Assign Designer to {{ selectedCardIds.size }} card(s)</div>
        </q-card-section>
        <q-card-section>
          <q-select
            v-model="bulkDesignerUserId"
            :options="memberOptions"
            option-value="id"
            option-label="displayName"
            emit-value
            map-options
            dense
            outlined
            color="teal-5"
            label="Select designer"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup />
          <q-btn flat label="Assign" color="purple-4" :disable="!bulkDesignerUserId" :loading="bulkAssigning" @click="doBulkDesignerAssign" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Bulk move dialog -->
    <q-dialog v-model="showBulkMove">
      <q-card style="min-width:360px" class="bg-dark text-white">
        <q-card-section>
          <div class="text-h6">Move {{ selectedCardIds.size }} card(s) to column</div>
        </q-card-section>
        <q-card-section>
          <q-select
            v-model="bulkMoveColumnId"
            :options="columnOptions"
            option-value="id"
            option-label="title"
            emit-value
            map-options
            dense
            outlined
            color="amber-4"
            label="Target column"
            autofocus
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup />
          <q-btn flat label="Move" color="amber-4" :disable="!bulkMoveColumnId" :loading="bulkMoving" @click="doBulkMove" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Quick assign dialog (single card) -->
    <q-dialog v-model="showQuickAssign">
      <q-card style="min-width:350px" class="bg-dark text-white">
        <q-card-section>
          <div class="text-h6">Quick Assign: {{ assignTarget?.name }}</div>
        </q-card-section>
        <q-card-section>
          <q-tabs v-if="isPodDesign" v-model="quickAssignTab" dense class="text-grey-5" active-color="teal-5" indicator-color="teal-5" narrow-indicator>
            <q-tab name="member" label="Member" />
            <q-tab name="designer" label="Designer" />
          </q-tabs>
          <q-select
            v-model="quickAssignUserId"
            :options="memberOptions"
            option-value="id"
            option-label="displayName"
            emit-value
            map-options
            dense
            outlined
            color="teal-5"
            :label="isPodDesign && quickAssignTab === 'designer' ? 'Select designer' : 'Select member'"
            class="q-mt-sm"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-5" v-close-popup />
          <q-btn flat label="Assign" color="teal-5" :disable="!quickAssignUserId" :loading="quickAssigning" @click="doQuickAssign" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Card detail dialog (pass boardType) -->
    <card-detail-dialog
      v-model="showCard"
      :card-id="selectedCardId"
      :board-id="boardId"
      :board-type="boardStore.board?.boardType || 'GENERAL'"
      :board-labels="boardStore.board?.labels || []"
      :board-types="boardStore.board?.types || []"
      :board-members="boardStore.board?.members || []"
      :board-columns="boardStore.board?.columns || []"
      :external-update="cardExternalUpdate"
      :comment-event="cardCommentEvent"
      :activity-event="cardActivityEvent"
      @updated="refreshBoard"
      @deleted="refreshBoard"
      @open-card="openCard"
      @dismiss-update="cardExternalUpdate = null"
    />

    <!-- Board edit dialog -->
    <board-form-dialog
      v-model="showEditBoard"
      :board="boardStore.board"
      :columns="boardStore.board?.columns || []"
      @saved="onBoardSaved"
    />

    <!-- Board members dialog -->
    <board-members-dialog
      v-model="showMembers"
      :board-id="boardId"
      :members="boardStore.board?.members || []"
      :can-manage="canManage"
      :board-type="boardStore.board?.boardType || 'GENERAL'"
      @updated="refreshBoard"
    />
  </q-page>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import draggable from 'vuedraggable'
import { useBoardStore } from 'src/stores/boardStore'
import { useAuthStore } from 'src/stores/authStore'
import { useBoardSocket } from 'src/composables/useBoardSocket'
import { columnApi, cardApi, designApi } from 'src/api/tasks'
import { saveBlob } from 'src/utils/fileUrl'
import { TAB_ID } from 'src/boot/axios'
import KanbanColumn from 'src/components/KanbanColumn.vue'
import CardDetailDialog from 'src/components/CardDetailDialog.vue'
import BoardFormDialog from 'src/components/BoardFormDialog.vue'
import BoardMembersDialog from 'src/components/BoardMembersDialog.vue'

const route = useRoute()
const router = useRouter()
const $q = useQuasar()
const boardStore = useBoardStore()
const authStore = useAuthStore()

const boardId = computed(() => Number(route.params.boardId))

const goBack = () => {
  if (route.name === 'designBoard') {
    router.push('/designs/boards')
  } else {
    router.push('/tasks')
  }
}

const showCard = ref(false)
const selectedCardId = ref(null)
const showEditBoard = ref(false)
const showMembers = ref(false)
const addingColumn = ref(false)
const newColTitle = ref('')
const isConnected = ref(false)
const cardExternalUpdate = ref(null) // { actorName, type }
const searchQuery = ref('')
const searchFocused = ref(false)
const cardCommentEvent = ref(null) // forwarded comment events
const cardActivityEvent = ref(null) // forwarded activity events
const kanbanScrollEl = ref(null) // ref to the horizontal scroll container

// ─── Multi-select & Bulk Actions ──────────────────────────────────────────────
const selectedCardIds = reactive(new Set())
const showBulkActions = computed(() => selectedCardIds.size > 0)

const showBulkMemberAssign = ref(false)
const showBulkDesignerAssign = ref(false)
const bulkMemberUserId = ref(null)
const bulkDesignerUserId = ref(null)
const bulkAssigning = ref(false)
const showBulkMove = ref(false)
const bulkMoveColumnId = ref(null)
const bulkMoving = ref(false)
const columnOptions = computed(() =>
  (boardStore.board?.columns || []).map(c => ({ id: c.id, title: c.title }))
)
const openBulkMove = () => {
  if (!selectedCardIds.size) return
  bulkMoveColumnId.value = null
  showBulkMove.value = true
}
const doBulkMove = async () => {
  if (!bulkMoveColumnId.value || !selectedCardIds.size) return
  bulkMoving.value = true
  try {
    const ids = [...selectedCardIds]
    const res = await cardApi.bulkMove(ids, bulkMoveColumnId.value)
    const moved = res.data.data?.moved ?? ids.length
    $q.notify({ type: 'positive', message: `Moved ${moved} card(s)` })
    selectedCardIds.clear()
    showBulkMove.value = false
    await refreshBoard()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Bulk move failed' })
  } finally {
    bulkMoving.value = false
  }
}

const onToggleSelectAllColumn = ({ ids, selectAll }) => {
  if (selectAll) {
    for (const id of ids) selectedCardIds.add(id)
  } else {
    for (const id of ids) selectedCardIds.delete(id)
  }
}

const onToggleSelectCard = (card) => {
  if (selectedCardIds.has(card.id)) {
    selectedCardIds.delete(card.id)
  } else {
    selectedCardIds.add(card.id)
  }
}

const doBulkMemberAssign = async () => {
  if (!bulkMemberUserId.value) return
  bulkAssigning.value = true
  try {
    const promises = [...selectedCardIds].map(cardId =>
      cardApi.addMember(cardId, bulkMemberUserId.value)
    )
    await Promise.allSettled(promises)
    $q.notify({ type: 'positive', message: `Member assigned to ${selectedCardIds.size} card(s)` })
    selectedCardIds.clear()
    showBulkMemberAssign.value = false
    bulkMemberUserId.value = null
    await refreshBoard()
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to assign member to some cards' })
  } finally {
    bulkAssigning.value = false
  }
}

const bulkDownloading = ref(false)
const sanitizeFilename = (s) => (s || 'mockups').replace(/[\\/:*?"<>|\r\n\t]/g, '_').trim() || 'mockups'

const onDownloadMockupsCard = async (card) => {
  try {
    $q.notify({ type: 'info', message: `Preparing zip for "${card.name}"...`, timeout: 1500 })
    const res = await designApi.downloadMockupsZip(card.id)
    saveBlob(res.data, sanitizeFilename(card.name) + '.zip')
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to download mockups' })
  }
}

const doBulkDownloadMockups = async () => {
  if (!selectedCardIds.size) return
  bulkDownloading.value = true
  try {
    const ids = [...selectedCardIds]
    const res = await designApi.downloadMockupsZipBulk(ids)
    saveBlob(res.data, `mockups-${ids.length}-cards.zip`)
    $q.notify({ type: 'positive', message: `Downloaded mockups for ${ids.length} card(s)` })
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to download mockups' })
  } finally {
    bulkDownloading.value = false
  }
}

const doBulkDesignerAssign = async () => {
  if (!bulkDesignerUserId.value) return
  bulkAssigning.value = true
  try {
    const promises = [...selectedCardIds].map(cardId =>
      designApi.addDesigner(cardId, bulkDesignerUserId.value)
    )
    await Promise.allSettled(promises)
    $q.notify({ type: 'positive', message: `Designer assigned to ${selectedCardIds.size} card(s)` })
    selectedCardIds.clear()
    showBulkDesignerAssign.value = false
    bulkDesignerUserId.value = null
    await refreshBoard()
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to assign designer to some cards' })
  } finally {
    bulkAssigning.value = false
  }
}

// ─── Quick Assign (single card) ───────────────────────────────────────────────
const assignTarget = ref(null)
const showQuickAssign = ref(false)
const quickAssignUserId = ref(null)
const quickAssignTab = ref('member')
const quickAssigning = ref(false)

const onAssignCard = (card) => {
  assignTarget.value = card
  quickAssignUserId.value = null
  quickAssignTab.value = 'member'
  showQuickAssign.value = true
}

const doQuickAssign = async () => {
  if (!quickAssignUserId.value || !assignTarget.value) return
  quickAssigning.value = true
  try {
    if (isPodDesign.value && quickAssignTab.value === 'designer') {
      await designApi.addDesigner(assignTarget.value.id, quickAssignUserId.value)
      $q.notify({ type: 'positive', message: 'Designer assigned' })
    } else {
      await cardApi.addMember(assignTarget.value.id, quickAssignUserId.value)
      $q.notify({ type: 'positive', message: 'Member assigned' })
    }
    showQuickAssign.value = false
    quickAssignUserId.value = null
    assignTarget.value = null
    await refreshBoard()
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to assign' })
  } finally {
    quickAssigning.value = false
  }
}

// ─── Filters ─────────────────────────────────────────────────────────────────
const showFilters = ref(false)
const filterMembers = ref([])
const filterLabels = ref([])
const filterTypes = ref([])
const filterStatuses = ref([])
const filterDateFrom = ref(null)
const filterDateTo = ref(null)

const statusOptions = ['OPEN', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'CANCELLED']

const memberOptions = computed(() => {
  const members = boardStore.board?.members || []
  return members.map(m => {
    const u = m.user || m
    return {
      id: u.id,
      displayName: [u.firstName, u.lastName].filter(Boolean).join(' ') || u.userName
    }
  })
})

const labelOptions = computed(() => boardStore.board?.labels || [])
const typeOptions = computed(() => boardStore.board?.types || [])

const hasActiveFilters = computed(() =>
  filterMembers.value.length > 0 ||
  filterLabels.value.length > 0 ||
  filterTypes.value.length > 0 ||
  filterStatuses.value.length > 0 ||
  filterDateFrom.value ||
  filterDateTo.value
)

const clearFilters = () => {
  filterMembers.value = []
  filterLabels.value = []
  filterTypes.value = []
  filterStatuses.value = []
  filterDateFrom.value = null
  filterDateTo.value = null
}

const cardFilter = computed(() => {
  if (!hasActiveFilters.value) return null
  return (card) => {
    // Member filter
    if (filterMembers.value.length > 0) {
      const cardMemberIds = (card.members || []).map(m => m.id)
      if (!filterMembers.value.some(id => cardMemberIds.includes(id))) return false
    }
    // Label filter
    if (filterLabels.value.length > 0) {
      const cardLabelIds = (card.labels || []).map(l => l.id)
      if (!filterLabels.value.some(id => cardLabelIds.includes(id))) return false
    }
    // Type filter
    if (filterTypes.value.length > 0) {
      const cardTypeIds = (card.types || []).map(t => t.id)
      if (!filterTypes.value.some(id => cardTypeIds.includes(id))) return false
    }
    // Status filter
    if (filterStatuses.value.length > 0) {
      if (!filterStatuses.value.includes(card.status)) return false
    }
    // Date range filter
    if (filterDateFrom.value || filterDateTo.value) {
      const cardDate = card.createdAt ? card.createdAt.substring(0, 10) : null
      if (!cardDate) return false
      if (filterDateFrom.value && cardDate < filterDateFrom.value) return false
      if (filterDateTo.value && cardDate > filterDateTo.value) return false
    }
    return true
  }
})

const isPodDesign = computed(() => boardStore.board?.boardType === 'POD_DESIGN')

const canManage = computed(() => {
  if (authStore.isAdmin) return true
  const me = boardStore.board?.members?.find(m => m.user?.id === authStore.currentUser?.id)
  return me?.role === 'OWNER' || me?.role === 'ADMIN'
})

// Bind draggable columns directly to board.columns for optimistic reorder
const localColumns = computed({
  get: () => boardStore.board?.columns || [],
  set: (cols) => {
    if (boardStore.board) boardStore.board.columns = cols
  }
})

const openCard = (card) => {
  selectedCardId.value = typeof card === 'object' ? card.id : card
  cardExternalUpdate.value = null
  showCard.value = true
}

const searchResults = ref([])
let searchDebounce = null

watch(searchQuery, (q) => {
  clearTimeout(searchDebounce)
  if (!q || !q.trim()) { searchResults.value = []; return }
  searchDebounce = setTimeout(async () => {
    try {
      const res = await cardApi.search(boardId.value, q.trim(), true)
      const results = (res.data.data || []).map(card => {
        // Find column name from board data, or use stage
        const col = boardStore.board?.columns?.find(c => (c.cards || []).some(cc => cc.id === card.id))
        return { ...card, _colName: col?.title || card.stage || '', _exact: card.name?.toLowerCase() === q.trim().toLowerCase() }
      })
      results.sort((a, b) => (b._exact ? 1 : 0) - (a._exact ? 1 : 0))
      searchResults.value = results.slice(0, 12)
    } catch { searchResults.value = [] }
  }, 300)
})

const openSearchResult = (card) => {
  searchQuery.value = ''
  searchFocused.value = false
  openCard(card)
}

const onSearchBlur = () => {
  searchFocused.value = false
}

const onColumnDragEnd = async () => {
  onDragEnd()
  const orderedIds = localColumns.value.map(c => c.id)
  await boardStore.reorderColumns(boardId.value, orderedIds)
}

const addColumn = async () => {
  if (!newColTitle.value.trim()) return
  try {
    await columnApi.create(boardId.value, { title: newColTitle.value.trim() })
    newColTitle.value = ''
    addingColumn.value = false
    await refreshBoard()
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to add column' })
  }
}

const confirmDeleteColumn = (columnId) => {
  $q.dialog({
    title: 'Delete Column',
    message: 'Delete this column and all its cards? This cannot be undone.',
    cancel: true,
    persistent: true,
    color: 'red-5'
  }).onOk(async () => {
    try {
      await columnApi.delete(columnId)
      await refreshBoard()
    } catch (err) {
      $q.notify({ type: 'negative', message: err.response?.data?.message || 'Failed to delete column' })
    }
  })
}

const handleCopyCard = async (card) => {
  try {
    const res = await cardApi.copy(card.id)
    $q.notify({ type: 'positive', message: 'Card copied to first column' })
    await refreshBoard()
    const newCardId = res.data?.data?.id
    if (newCardId) {
      openCard(newCardId)
    }
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to copy card' })
  }
}

const confirmDeleteCard = (card) => {
  $q.dialog({
    title: 'Delete Card',
    message: `Delete "${card.name}"? This cannot be undone.`,
    cancel: true,
    persistent: true,
    color: 'red-5'
  }).onOk(async () => {
    try {
      await cardApi.delete(card.id)
      await refreshBoard()
    } catch (err) {
      $q.notify({ type: 'negative', message: err.response?.data?.message || 'Failed to delete card' })
    }
  })
}

const refreshBoard = async () => {
  await boardStore.loadBoard(boardId.value)
}

const onBoardSaved = () => {
  showEditBoard.value = false
  refreshBoard()
}

// ─── Auto-scroll during drag ─────────────────────────────────────────────────

let isDragging = false
let dragScrollSpeed = 0
let scrollRaf = null
let dragMoveRafPending = false

const scrollLoop = () => {
  if (dragScrollSpeed !== 0 && kanbanScrollEl.value) {
    kanbanScrollEl.value.scrollLeft += dragScrollSpeed
  }
  if (isDragging) {
    scrollRaf = requestAnimationFrame(scrollLoop)
  } else {
    scrollRaf = null
  }
}

const onDragMove = (e) => {
  if (!isDragging || !kanbanScrollEl.value) return
  // Throttle to one computation per animation frame
  if (dragMoveRafPending) return
  dragMoveRafPending = true
  const clientX = e.touches ? e.touches[0]?.clientX : e.clientX
  requestAnimationFrame(() => {
    dragMoveRafPending = false
    if (!isDragging || !kanbanScrollEl.value || clientX == null) return

    const EDGE = 120     // activation zone width in px
    const MAX_SPEED = 18 // max scroll speed (px per frame)
    const rect = kanbanScrollEl.value.getBoundingClientRect()

    const distRight = rect.right - clientX
    const distLeft  = clientX - rect.left

    if (distRight < EDGE) {
      dragScrollSpeed = Math.ceil(MAX_SPEED * (1 - Math.max(0, distRight) / EDGE))
    } else if (distLeft < EDGE) {
      dragScrollSpeed = -Math.ceil(MAX_SPEED * (1 - Math.max(0, distLeft) / EDGE))
    } else {
      dragScrollSpeed = 0
    }
  })
}

const attachHScrollListeners = () => {
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('dragover',  onDragMove)
  document.addEventListener('touchmove', onDragMove, { passive: true })
}

const detachHScrollListeners = () => {
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('dragover',  onDragMove)
  document.removeEventListener('touchmove', onDragMove)
}

const onDragStart = () => {
  isDragging = true
  dragScrollSpeed = 0
  attachHScrollListeners()
  if (!scrollRaf) scrollRaf = requestAnimationFrame(scrollLoop)
}

const onDragEnd = () => {
  isDragging = false
  dragScrollSpeed = 0
  detachHScrollListeners()
}

// ─── WebSocket real-time updates ────────────────────────────────────────────

// Events handled in real-time by child components — no conflict banner needed
const REALTIME_EVENTS = new Set(['COMMENT_ADDED', 'COMMENT_UPDATED', 'COMMENT_DELETED', 'ACTIVITY_LOGGED'])

const findColumn = (columnId) =>
  boardStore.board?.columns?.find(c => c.id === columnId)

const findCardInBoard = (cardId) => {
  for (const col of (boardStore.board?.columns || [])) {
    const idx = (col.cards || []).findIndex(c => c.id === cardId)
    if (idx !== -1) return { column: col, cardIndex: idx }
  }
  return null
}

const handleBoardEvent = (event) => {
  const { type, tabId, actorName, cardId, columnId, payload } = event

  // Skip events originated from this same tab — already applied optimistically
  if (tabId && tabId === TAB_ID) return

  switch (type) {
    case 'CARD_CREATED': {
      const col = findColumn(columnId)
      if (col && payload) {
        // Regular members only see cards they are a member of
        if (!canManage.value) {
          const myId = authStore.currentUser?.userId ?? authStore.currentUser?.id
          const isMember = (payload.members || []).some(m => m.id === myId)
          if (!isMember) break
        }
        if (!col.cards) col.cards = []
        if (!col.cards.some(c => c.id === payload.id)) {
          col.cards.push(payload)
        }
      }
      break
    }
    case 'CARD_UPDATED': {
      const found = findCardInBoard(cardId)
      if (found && payload) {
        Object.assign(found.column.cards[found.cardIndex], payload)
      }
      break
    }
    case 'CARD_DELETED': {
      const found = findCardInBoard(cardId)
      if (found) {
        found.column.cards.splice(found.cardIndex, 1)
      }
      break
    }
    case 'CARD_MOVED': {
      if (payload) {
        const fromCol = findColumn(payload.fromColumnId)
        const toCol = findColumn(payload.toColumnId)
        if (fromCol && toCol) {
          const idx = (fromCol.cards || []).findIndex(c => c.id === cardId)
          if (idx !== -1) {
            const [card] = fromCol.cards.splice(idx, 1)
            if (!toCol.cards) toCol.cards = []
            toCol.cards.push(card)
          }
        }
      }
      break
    }
    case 'CARDS_REORDERED': {
      const col = findColumn(columnId)
      if (col && Array.isArray(payload)) {
        const cardMap = new Map((col.cards || []).map(c => [c.id, c]))
        col.cards = payload.map(id => cardMap.get(id)).filter(Boolean)
      }
      break
    }
    case 'CARD_ARCHIVED': {
      const found = findCardInBoard(cardId)
      if (found) {
        found.column.cards.splice(found.cardIndex, 1)
      }
      break
    }
    case 'CARD_UNARCHIVED': {
      if (payload) {
        const col = findColumn(columnId)
        if (col) {
          if (!col.cards) col.cards = []
          if (!col.cards.some(c => c.id === payload.id)) {
            col.cards.push(payload)
          }
        }
      }
      break
    }
    case 'CARDS_AUTO_ARCHIVED': {
      refreshBoard()
      break
    }
    case 'COLUMN_CREATED': {
      if (payload && boardStore.board?.columns) {
        if (!boardStore.board.columns.some(c => c.id === payload.id)) {
          if (!payload.cards) payload.cards = []
          boardStore.board.columns.push(payload)
        }
      }
      break
    }
    case 'COLUMN_UPDATED': {
      const col = findColumn(columnId)
      if (col && payload) {
        if (payload.title !== undefined) col.title = payload.title
        if (payload.position !== undefined) col.position = payload.position
      }
      break
    }
    case 'COLUMN_DELETED': {
      if (boardStore.board?.columns) {
        boardStore.board.columns = boardStore.board.columns.filter(c => c.id !== columnId)
      }
      break
    }
    case 'COLUMNS_REORDERED': {
      if (boardStore.board?.columns && Array.isArray(payload)) {
        const colMap = new Map(boardStore.board.columns.map(c => [c.id, c]))
        boardStore.board.columns = payload.map(id => colMap.get(id)).filter(Boolean)
      }
      break
    }
    case 'CARD_MEMBER_ADDED': {
      const user = payload?.user ?? payload
      const cardData = payload?.card
      const found = findCardInBoard(cardId)
      if (found && user) {
        const card = found.column.cards[found.cardIndex]
        if (!card.members) card.members = []
        if (!card.members.some(m => m.id === user.id)) {
          card.members.push(user)
        }
      } else if (!found && !canManage.value && user) {
        // Card not in our view — if the added member is me, show the card
        const myId = authStore.currentUser?.userId ?? authStore.currentUser?.id
        if (user.id === myId) {
          if (cardData) {
            const col = findColumn(columnId)
            if (col) {
              if (!col.cards) col.cards = []
              if (!col.cards.some(c => c.id === cardData.id)) {
                col.cards.push(cardData)
              }
            }
          } else {
            refreshBoard()
          }
        }
      }
      break
    }
    case 'CARD_MEMBER_REMOVED': {
      const userId = payload?.userId
      const found = findCardInBoard(cardId)
      if (found && userId) {
        const card = found.column.cards[found.cardIndex]
        card.members = (card.members || []).filter(m => m.id !== userId)
        // If I was removed and I'm not board owner/admin, hide the card
        const myId = authStore.currentUser?.userId ?? authStore.currentUser?.id
        if (!canManage.value && userId === myId) {
          found.column.cards.splice(found.cardIndex, 1)
        }
      }
      break
    }
    case 'COMMENT_ADDED':
    case 'COMMENT_UPDATED':
    case 'COMMENT_DELETED': {
      if (showCard.value && cardId && selectedCardId.value === cardId) {
        cardCommentEvent.value = { type, payload, commentId: payload?.id || payload?.commentId }
      }
      break
    }
    case 'ACTIVITY_LOGGED': {
      if (showCard.value && cardId && selectedCardId.value === cardId) {
        cardActivityEvent.value = payload
      }
      break
    }
  }

  // If the card detail dialog is open for the affected card, show a conflict banner
  if (showCard.value && cardId && selectedCardId.value === cardId && !REALTIME_EVENTS.has(type)) {
    cardExternalUpdate.value = { actorName, type }
  }
}

const { connect } = useBoardSocket(
  () => boardId.value,
  handleBoardEvent,
  () => { isConnected.value = true }
)

// Re-connect when board changes (e.g. navigating between boards)
watch(boardId, async (id) => {
  if (id) {
    await refreshBoard()
    connect()
  }
})

// Watch for ?cardId= query changes (e.g. from notification click while on same board)
watch(() => route.query.cardId, (cardId) => {
  if (cardId) {
    selectedCardId.value = Number(cardId)
    cardExternalUpdate.value = null
    showCard.value = true
    router.replace({ query: {} })
  }
})

onMounted(async () => {
  await refreshBoard()

  // Deep-link: open card dialog if ?cardId= is present
  const cardIdParam = route.query.cardId
  if (cardIdParam) {
    selectedCardId.value = Number(cardIdParam)
    showCard.value = true
    router.replace({ query: {} })
  }

})

onUnmounted(() => {
  detachHScrollListeners()
  if (scrollRaf) cancelAnimationFrame(scrollRaf)
})
</script>

<style scoped>
.search-results {
  top: 100%;
  left: 0;
  right: 0;
  z-index: 100;
  max-height: 300px;
  overflow-y: auto;
  background: var(--erp-bg-elevated);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 6px;
  margin-top: 2px;
}
.board-page {
  background: var(--erp-bg);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.board-header {
  background: var(--erp-bg-secondary);
  flex-shrink: 0;
  border-bottom: 1px solid var(--erp-border-subtle);
}
.filter-bar {
  background: var(--erp-bg-secondary);
  flex-shrink: 0;
  border-bottom: 1px solid var(--erp-border-subtle);
  overflow-x: auto;
}
.kanban-scroll {
  flex: 1;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 20px;
  display: flex;
  align-items: flex-start;
}
.kanban-inner {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.add-col-wrap {
  flex-shrink: 0;
  padding: 8px;
  min-width: 180px;
  background: var(--erp-hover-bg);
  border: 1px dashed var(--erp-border-subtle);
  border-radius: 10px;
  display: flex;
  align-items: flex-start;
}
.col-ghost { opacity: 0.35; }
.col-drag { transform: rotate(1deg); box-shadow: 0 12px 32px var(--erp-shadow); }

/* Bulk actions bar */
.bulk-actions-bar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(30, 30, 40, 0.95);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 12px;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 9999;
  box-shadow: 0 8px 32px rgba(0,0,0,0.5);
}
</style>
