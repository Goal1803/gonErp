<template>
  <q-page class="board-page">
    <!-- Board header -->
    <div class="board-header row items-center no-wrap q-px-lg q-py-sm">
      <q-btn flat round dense icon="arrow_back" color="grey-5" @click="router.push('/tasks')" />

      <div class="q-ml-sm">
        <div class="text-white text-weight-medium" style="font-size:1.05rem">
          {{ boardStore.board?.name }}
        </div>
      </div>

      <div class="q-ml-sm" v-if="boardStore.board?.coverColor">
        <div style="width:14px; height:14px; border-radius:3px"
          :style="{ background: boardStore.board.coverColor }" />
      </div>

      <q-space />

      <!-- Live indicator -->
      <q-chip v-if="isConnected" dense color="teal-9" text-color="teal-3" size="xs" icon="circle"
        class="q-mr-sm" style="font-size:0.68rem">Live</q-chip>

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
          emit-value map-options multiple dense outlined dark color="teal-5" label="Members"
          style="min-width:150px" clearable use-chips stack-label />
        <q-select v-model="filterLabels" :options="labelOptions" option-value="id" option-label="name"
          emit-value map-options multiple dense outlined dark color="teal-5" label="Labels"
          style="min-width:150px" clearable use-chips stack-label />
        <q-select v-model="filterTypes" :options="typeOptions" option-value="id" option-label="name"
          emit-value map-options multiple dense outlined dark color="teal-5" label="Types"
          style="min-width:150px" clearable use-chips stack-label />
        <q-select v-model="filterStatuses" :options="statusOptions"
          multiple dense outlined dark color="teal-5" label="Status"
          style="min-width:150px" clearable use-chips stack-label />
        <q-input v-model="filterDateFrom" type="date" dense outlined dark color="teal-5" label="From" stack-label
          style="min-width:140px" clearable />
        <q-input v-model="filterDateTo" type="date" dense outlined dark color="teal-5" label="To" stack-label
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
            @open-card="openCard"
            @delete="confirmDeleteColumn"
            @delete-card="confirmDeleteCard"
            @refresh="refreshBoard"
            @drag-start="onDragStart"
            @drag-end="onDragEnd"
          />
        </template>
      </draggable>

      <!-- Add column (hidden for POD_DESIGN boards) -->
      <div v-if="!isPodDesign" class="add-col-wrap">
        <div v-if="!addingColumn">
          <q-btn flat icon="add" label="Add column" color="grey-5"
            style="white-space:nowrap" @click="addingColumn = true" />
        </div>
        <div v-else style="width:280px">
          <q-input v-model="newColTitle" outlined dark color="teal-5" dense
            placeholder="Column title..." autofocus
            @keyup.enter="addColumn" @keyup.escape="addingColumn = false" />
          <div class="row q-gutter-xs q-mt-xs">
            <q-btn label="Add" color="teal-6" unelevated dense size="sm" @click="addColumn" />
            <q-btn flat label="Cancel" color="grey-5" dense size="sm" @click="addingColumn = false" />
          </div>
        </div>
      </div>
    </div>

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
      @updated="refreshBoard"
      @deleted="refreshBoard"
      @dismiss-update="cardExternalUpdate = null"
    />

    <!-- Board edit dialog -->
    <board-form-dialog
      v-model="showEditBoard"
      :board="boardStore.board"
      @saved="onBoardSaved"
    />

    <!-- Board members dialog -->
    <board-members-dialog
      v-model="showMembers"
      :board-id="boardId"
      :members="boardStore.board?.members || []"
      :can-manage="canManage"
      @updated="refreshBoard"
    />
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import draggable from 'vuedraggable'
import { useBoardStore } from 'src/stores/boardStore'
import { useAuthStore } from 'src/stores/authStore'
import { useBoardSocket } from 'src/composables/useBoardSocket'
import { columnApi, cardApi } from 'src/api/tasks'
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

const showCard = ref(false)
const selectedCardId = ref(null)
const showEditBoard = ref(false)
const showMembers = ref(false)
const addingColumn = ref(false)
const newColTitle = ref('')
const isConnected = ref(false)
const cardExternalUpdate = ref(null) // { actorName, type }
const kanbanScrollEl = ref(null) // ref to the horizontal scroll container

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
  selectedCardId.value = card.id
  cardExternalUpdate.value = null
  showCard.value = true
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
    dark: true,
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

const confirmDeleteCard = (card) => {
  $q.dialog({
    title: 'Delete Card',
    message: `Delete "${card.name}"? This cannot be undone.`,
    cancel: true,
    persistent: true,
    dark: true,
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
  const clientX = e.touches ? e.touches[0]?.clientX : e.clientX
  if (clientX == null) return

  const EDGE = 120     // activation zone width in px
  const MAX_SPEED = 18 // max scroll speed (px per frame)
  const rect = kanbanScrollEl.value.getBoundingClientRect()

  // Use the kanban container's own edges for both directions
  const distRight = rect.right - clientX
  const distLeft  = clientX - rect.left

  if (distRight < EDGE) {
    // Clamp so going past the right edge gives max speed, not > max
    dragScrollSpeed = Math.ceil(MAX_SPEED * (1 - Math.max(0, distRight) / EDGE))
  } else if (distLeft < EDGE) {
    // Clamp so going into the sidebar (distLeft < 0) gives max speed, not > max
    dragScrollSpeed = -Math.ceil(MAX_SPEED * (1 - Math.max(0, distLeft) / EDGE))
  } else {
    dragScrollSpeed = 0
  }
}

const onDragStart = () => {
  isDragging = true
  dragScrollSpeed = 0
  if (!scrollRaf) scrollRaf = requestAnimationFrame(scrollLoop)
}

const onDragEnd = () => {
  isDragging = false
  dragScrollSpeed = 0
}

// ─── WebSocket real-time updates ────────────────────────────────────────────

// Structural events that require a full board refresh
const BOARD_LEVEL_EVENTS = new Set([
  'CARD_CREATED', 'CARD_DELETED', 'CARD_MOVED', 'CARDS_REORDERED',
  'COLUMN_CREATED', 'COLUMN_UPDATED', 'COLUMN_DELETED', 'COLUMNS_REORDERED'
])

const handleBoardEvent = (event) => {
  const { type, tabId, actorName, cardId } = event

  // Skip events originated from this same tab — already applied optimistically
  if (tabId && tabId === TAB_ID) return

  if (BOARD_LEVEL_EVENTS.has(type)) {
    refreshBoard()
  }

  // If the card detail dialog is open for the affected card, show a conflict banner
  if (showCard.value && cardId && selectedCardId.value === cardId) {
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

onMounted(() => {
  refreshBoard()
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('dragover',  onDragMove)   // fires during HTML5 drag (mousemove is suppressed)
  document.addEventListener('touchmove', onDragMove, { passive: true })
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('dragover',  onDragMove)
  document.removeEventListener('touchmove', onDragMove)
  if (scrollRaf) cancelAnimationFrame(scrollRaf)
})
</script>

<style scoped>
.board-page {
  background: #0d0d0d;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.board-header {
  background: #111;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255,255,255,0.07);
}
.filter-bar {
  background: #111;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255,255,255,0.05);
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
  background: rgba(255,255,255,0.03);
  border: 1px dashed rgba(255,255,255,0.1);
  border-radius: 10px;
  display: flex;
  align-items: flex-start;
}
.col-ghost { opacity: 0.35; }
.col-drag { transform: rotate(1deg); box-shadow: 0 12px 32px rgba(0,0,0,0.6); }
</style>
