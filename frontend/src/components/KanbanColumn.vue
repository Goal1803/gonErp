<template>
  <div class="kanban-column">
    <!-- Column header -->
    <div class="col-header row items-center no-wrap q-px-sm q-py-xs">
      <div v-if="!editingTitle" class="text-white text-weight-medium col text-truncate"
        style="font-size:0.9rem" @dblclick="startEdit">
        {{ column.title }}
      </div>
      <q-input v-else v-model="editTitle" dense dark outlined color="teal-5" autofocus
        class="col" style="font-size:0.85rem"
        @keyup.enter="saveTitle" @keyup.escape="editingTitle=false" @blur="saveTitle" />

      <div class="row items-center q-ml-xs">
        <q-badge color="grey-8" :label="column.cards?.length || 0" rounded style="font-size:0.65rem" />
        <q-btn flat round dense icon="more_vert" color="grey-5" size="xs" class="q-ml-xs">
          <q-menu dark>
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

    <!-- Cards draggable list -->
    <draggable
      v-model="localCards"
      group="cards"
      item-key="id"
      handle=".kanban-card"
      ghost-class="card-ghost"
      drag-class="card-drag"
      :data-column-id="column.id"
      @start="$emit('drag-start')"
      @end="onDragEnd"
      class="col-cards"
    >
      <template #item="{ element }">
        <kanban-card :card="element" @open="$emit('open-card', element)" />
      </template>
    </draggable>

    <!-- Add card -->
    <div class="q-px-sm q-pb-sm">
      <div v-if="!addingCard">
        <q-btn flat dense icon="add" label="Add card" color="grey-5" size="sm"
          class="full-width" style="justify-content:flex-start" @click="addingCard=true" />
      </div>
      <div v-else>
        <q-input v-model="newCardName" outlined dark color="teal-5" dense placeholder="Card name..."
          autofocus @keyup.enter="addCard" @keyup.escape="addingCard=false"
          style="font-size:0.85rem" />
        <div class="row q-gutter-xs q-mt-xs">
          <q-btn label="Add" color="teal-6" unelevated dense size="sm" @click="addCard" />
          <q-btn flat label="Cancel" color="grey-5" dense size="sm" @click="addingCard=false" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useQuasar } from 'quasar'
import draggable from 'vuedraggable'
import KanbanCard from './KanbanCard.vue'
import { columnApi, cardApi } from 'src/api/tasks'
import { useBoardStore } from 'src/stores/boardStore'

const props = defineProps({ column: { type: Object, required: true } })
const emit = defineEmits(['open-card', 'delete', 'refresh', 'drag-start', 'drag-end'])
const $q = useQuasar()
const boardStore = useBoardStore()

const editingTitle = ref(false)
const editTitle = ref('')
const addingCard = ref(false)
const newCardName = ref('')

const localCards = computed({
  get: () => props.column.cards || [],
  set: (newCards) => { props.column.cards = newCards }
})

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
    await cardApi.create(props.column.id, { name: newCardName.value.trim() })
    newCardName.value = ''
    addingCard.value = false
    emit('refresh')
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to add card' })
  }
}

const onDragEnd = async (evt) => {
  emit('drag-end')
  if (evt.from === evt.to) {
    // Same-column reorder
    const newOrder = localCards.value.map(c => c.id)
    await boardStore.reorderCards(props.column.id, newOrder)
  } else {
    // Cross-column move: read card id from the dragged element's data attribute
    const movedCardId = Number(evt.item.dataset.cardId)
    const targetColumnId = Number(evt.to.dataset.columnId)
    if (!movedCardId || !targetColumnId) {
      emit('refresh')
      return
    }
    const position = evt.newIndex + 1
    try {
      await cardApi.move(movedCardId, { targetColumnId, position })
      // Persist remaining order in source column
      const sourceOrder = localCards.value.map(c => c.id)
      if (sourceOrder.length > 0) {
        await cardApi.reorder(props.column.id, sourceOrder)
      }
    } catch {
      $q.notify({ type: 'negative', message: 'Failed to move card' })
      emit('refresh') // revert optimistic update
    }
  }
}
</script>

<style scoped>
.kanban-column {
  width: 300px;
  min-width: 300px;
  background: #161616;
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 140px);
}
.col-header {
  border-bottom: 1px solid rgba(255,255,255,0.07);
  min-height: 44px;
}
.col-cards {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 40px;
}
.card-ghost { opacity: 0.4; }
.card-drag { transform: rotate(2deg); box-shadow: 0 8px 24px rgba(0,0,0,0.5); }
</style>
