<template>
  <div class="kanban-column">
    <!-- Column header -->
    <div class="col-header row items-center no-wrap q-px-sm q-py-xs">
      <div v-if="!editingTitle" class="text-white text-weight-medium col text-truncate"
        style="font-size:0.9rem" @dblclick="startEdit">
        {{ column.title }}
      </div>
      <q-input v-else v-model="editTitle" dense outlined color="teal-5" autofocus
        class="col" style="font-size:0.85rem"
        @keyup.enter="saveTitle" @keyup.escape="editingTitle=false" @blur="saveTitle" />

      <div class="row items-center q-ml-xs">
        <q-btn
          v-if="visibleCount"
          flat round dense size="xs" color="grey-5"
          :icon="allVisibleSelected ? 'check_box' : 'select_all'"
          :class="{ 'select-active': allVisibleSelected }"
          @click="toggleSelectAll"
        >
          <q-tooltip>
            {{ allVisibleSelected ? 'Deselect all in column' : 'Select all cards in column' }}
          </q-tooltip>
        </q-btn>
        <q-badge color="grey-8" :label="countLabel" rounded style="font-size:0.65rem" />
        <q-icon v-if="virtualMode" name="bolt" size="xs" color="amber-5" class="q-ml-xs">
          <q-tooltip>
            Virtual scroll đang bật ({{ visibleCount }} thẻ &gt; {{ VIRTUAL_THRESHOLD }}) —
            chỉ render thẻ trong tầm nhìn để giữ hiệu năng. Kéo-thả vẫn hoạt động bình thường.
          </q-tooltip>
        </q-icon>
        <q-btn flat round dense icon="more_vert" color="grey-5" size="xs" class="q-ml-xs">
          <q-menu>
            <q-list dense>
              <q-item clickable v-close-popup @click="startEdit">
                <q-item-section avatar><q-icon name="edit" size="xs" /></q-item-section>
                <q-item-section>Rename</q-item-section>
              </q-item>
              <q-item clickable v-close-popup @click="$emit('delete', column.id)">
                <q-item-section avatar><q-icon name="delete" size="xs" color="red-4" /></q-item-section>
                <q-item-section class="text-red-4">Delete Column</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </div>
    </div>

    <!-- Cards scroll container. Doubles as a Pragmatic DnD drop target so cards
         can be dropped into empty space / the end of the column, and as the
         vertical auto-scroll region during a card drag. -->
    <div class="col-cards-wrap">
    <div class="col-cards" ref="colCardsEl" @scroll="onColScroll">
      <!-- Normal rendering (≤ VIRTUAL_THRESHOLD cards) -->
      <div v-if="!virtualMode" class="col-cards-inner">
        <draggable-card
          v-for="card in displayCards"
          :key="card.id"
          :card="card"
          :column-id="column.id"
          :selectable="selectable"
          :selected="selectedIds.has(card.id)"
          :board-type="boardType"
          @open="$emit('open-card', $event)"
          @delete="$emit('delete-card', $event)"
          @copy="$emit('copy-card', $event)"
          @assign="$emit('assign-card', $event)"
          @toggle-select="$emit('toggle-select-card', $event)"
          @download-mockups="$emit('download-mockups-card', $event)"
        />
      </div>

      <!-- Virtual scrolling for very large columns (> VIRTUAL_THRESHOLD cards):
           only on-screen cards are mounted, so the DOM (and lazy cover images)
           stay light no matter the card count. Drag-and-drop keeps working
           because Pragmatic DnD tolerates the source card being unmounted
           mid-drag. scroll-target reuses the same .col-cards container so the
           jump-to-bottom FAB and auto-scroll keep working. -->
      <q-virtual-scroll
        v-else-if="colCardsEl"
        :items="displayCards"
        :scroll-target="colCardsEl"
        :virtual-scroll-item-size="120"
        class="col-cards-virtual"
        v-slot="{ item }"
      >
        <draggable-card
          :key="item.id"
          :card="item"
          :column-id="column.id"
          :selectable="selectable"
          :selected="selectedIds.has(item.id)"
          :board-type="boardType"
          @open="$emit('open-card', $event)"
          @delete="$emit('delete-card', $event)"
          @copy="$emit('copy-card', $event)"
          @assign="$emit('assign-card', $event)"
          @toggle-select="$emit('toggle-select-card', $event)"
          @download-mockups="$emit('download-mockups-card', $event)"
        />
      </q-virtual-scroll>

      <div v-if="loadingMore" class="row justify-center q-py-sm">
        <q-spinner color="teal-5" size="20px" />
      </div>
    </div>

      <!-- Jump-to-bottom FAB -->
      <transition name="fab-fade">
        <q-btn
          v-if="showJumpBottom"
          round
          dense
          color="teal-7"
          icon="keyboard_double_arrow_down"
          size="sm"
          class="jump-bottom-fab"
          @click="scrollToBottom"
        >
          <q-tooltip>Jump to bottom</q-tooltip>
        </q-btn>
      </transition>
    </div>

    <!-- Add card -->
    <div class="q-px-sm q-pb-sm">
      <div v-if="!addingCard">
        <q-btn flat dense icon="add" label="Add card" color="grey-5" size="sm"
          class="full-width" style="justify-content:flex-start" @click="addingCard=true" />
      </div>
      <div v-else>
        <q-input v-model="newCardName" outlined color="teal-5" dense placeholder="Card name..."
          autofocus @keyup.enter="addCard" @keyup.escape="addingCard=false"
          style="font-size:0.85rem" />
        <q-btn-toggle v-if="boardType === 'POD_DESIGN'" v-model="newCardGenType"
          class="q-mt-xs full-width" no-caps dense spread unelevated rounded
          toggle-color="teal-6" color="grey-3" text-color="grey-8"
          :options="[{ label: 'Designer gen', value: 'DESIGNER' }, { label: 'Seller gen', value: 'SELLER' }]"
          style="font-size:0.7rem" />
        <div class="row q-gutter-xs q-mt-xs">
          <q-btn label="Add" color="teal-6" unelevated dense size="sm" @click="addCard" />
          <q-btn flat label="Cancel" color="grey-5" dense size="sm" @click="addingCard=false" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useQuasar } from 'quasar'
import DraggableCard from './DraggableCard.vue'
import { columnApi, cardApi } from 'src/api/tasks'
import { useBoardStore } from 'src/stores/boardStore'
import { combine } from '@atlaskit/pragmatic-drag-and-drop/combine'
import { dropTargetForElements } from '@atlaskit/pragmatic-drag-and-drop/element/adapter'
import { autoScrollForElements } from '@atlaskit/pragmatic-drag-and-drop-auto-scroll/element'

const props = defineProps({
  column: { type: Object, required: true },
  cardFilter: { type: Function, default: null },
  selectable: { type: Boolean, default: false },
  selectedIds: { type: Set, default: () => new Set() },
  boardType: { type: String, default: 'GENERAL' },
})
const emit = defineEmits(['open-card', 'delete', 'delete-card', 'copy-card', 'refresh', 'assign-card', 'toggle-select-card', 'download-mockups-card', 'toggle-select-all-column'])
const $q = useQuasar()
const boardStore = useBoardStore()

const editingTitle = ref(false)
const editTitle = ref('')
const addingCard = ref(false)
const newCardName = ref('')
const newCardGenType = ref('DESIGNER')
const colCardsEl = ref(null)

// Display only filtered cards — actual reordering is applied to the full
// column.cards array in BoardPage's drop handler, keyed by card id.
const displayCards = computed(() => {
  const all = props.column.cards || []
  if (!props.cardFilter) return all
  return all.filter(props.cardFilter)
})

const visibleCount = computed(() => displayCards.value.length)

// Show "loaded/total" when the column has more cards to lazily fetch.
const countLabel = computed(() => {
  const total = props.column.totalCards || 0
  return total > visibleCount.value ? `${visibleCount.value}/${total}` : `${visibleCount.value}`
})

// Above this many visible cards, render via virtual scrolling (only on-screen
// cards in the DOM) instead of a plain v-for. Drag-and-drop works in both modes.
const VIRTUAL_THRESHOLD = 200
const virtualMode = computed(() => displayCards.value.length > VIRTUAL_THRESHOLD)

const allVisibleSelected = computed(() => {
  if (!displayCards.value.length) return false
  return displayCards.value.every(c => props.selectedIds.has(c.id))
})

// Jump-to-bottom FAB: only visible when the column is scrollable AND not near the bottom.
const showJumpBottom = ref(false)

const updateJumpBottom = () => {
  const el = colCardsEl.value
  if (!el) { showJumpBottom.value = false; return }
  const canScroll = el.scrollHeight - el.clientHeight > 40
  const nearBottom = el.scrollHeight - el.clientHeight - el.scrollTop < 20
  showJumpBottom.value = canScroll && !nearBottom
}

// Lazy loading: fetch the next page when the user scrolls near the bottom.
const hasMore = computed(() => boardStore.columnHasMore(props.column))
const loadingMore = computed(() => !!boardStore.loadingMore[props.column.id])

const maybeLoadMore = () => {
  const el = colCardsEl.value
  if (!el || !hasMore.value || loadingMore.value) return
  if (el.scrollHeight - el.clientHeight - el.scrollTop < 300) {
    boardStore.loadMoreCards(props.column.id)
  }
}

const onColScroll = () => {
  updateJumpBottom()
  maybeLoadMore()
}

const scrollToBottom = () => {
  const el = colCardsEl.value
  if (!el) return
  el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
}

// Re-evaluate visibility whenever the card list size changes.
watch(() => displayCards.value.length, () => nextTick(updateJumpBottom))

const toggleSelectAll = () => {
  emit('toggle-select-all-column', {
    ids: displayCards.value.map(c => c.id),
    selectAll: !allVisibleSelected.value
  })
}

const startEdit = () => {
  editTitle.value = props.column.title
  editingTitle.value = true
}

const saveTitle = async () => {
  if (!editTitle.value.trim() || editTitle.value === props.column.title) {
    editingTitle.value = false
    return
  }
  try {
    await columnApi.update(props.column.id, { title: editTitle.value.trim() })
    props.column.title = editTitle.value.trim()
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to rename column' })
  } finally {
    editingTitle.value = false
  }
}

const addCard = async () => {
  if (!newCardName.value.trim()) return
  try {
    const payload = { name: newCardName.value.trim() }
    if (props.boardType === 'POD_DESIGN') payload.genType = newCardGenType.value
    await cardApi.create(props.column.id, payload)
    newCardName.value = ''
    newCardGenType.value = 'DESIGNER'
    addingCard.value = false
    emit('refresh')
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to add card' })
  }
}

// ─── Pragmatic DnD: column-level drop target + vertical auto-scroll ───────────
// The card-level draggable/drop-target registration lives in DraggableCard.vue;
// here we register the column body so a card can be dropped onto empty space
// (lands at the end), and enable auto-scroll while dragging within the column.
let dndCleanup = null

onMounted(() => {
  const el = colCardsEl.value
  if (!el) return
  dndCleanup = combine(
    dropTargetForElements({
      element: el,
      canDrop: ({ source }) => source.data.type === 'card',
      getIsSticky: () => true,
      getData: () => ({ type: 'column', columnId: props.column.id }),
    }),
    autoScrollForElements({
      element: el,
      canScroll: ({ source }) => source.data.type === 'card',
    }),
  )
})

onUnmounted(() => {
  if (dndCleanup) dndCleanup()
})
</script>

<style scoped>
.kanban-column {
  width: 250px;
  min-width: 250px;
  background: var(--erp-bg-tertiary);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  /* No max-height here — the scroll container below is bounded instead */
}
.col-header {
  border-bottom: 1px solid var(--erp-border-subtle);
  min-height: 44px;
}
.col-cards-wrap {
  position: relative;
}
.col-cards {
  overflow-y: auto;
  max-height: calc(100vh - 200px);
  min-height: 40px;
}
.jump-bottom-fab {
  position: absolute;
  right: 8px;
  bottom: 8px;
  z-index: 5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.45);
  opacity: 0.92;
}
.jump-bottom-fab:hover { opacity: 1; }
.fab-fade-enter-active, .fab-fade-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}
.fab-fade-enter-from, .fab-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
.col-cards-inner {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 40px;
}
/* Virtual-scroll mode: QVirtualScroll positions items itself, so space cards
   via per-item margin rather than flex gap. */
.col-cards-virtual {
  padding: 8px;
  min-height: 40px;
}
.col-cards-virtual :deep(.draggable-card) {
  margin-bottom: 8px;
}
.select-active :deep(.q-icon) { color: #26a69a !important; }
</style>
