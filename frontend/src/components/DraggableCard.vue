<template>
  <div ref="el" class="draggable-card" :class="{ 'is-dragging': dragging }">
    <div v-if="closestEdge === 'top'" class="drop-indicator drop-top" />
    <kanban-card
      :card="card"
      :selectable="selectable"
      :selected="selected"
      :board-type="boardType"
      @open="$emit('open', $event)"
      @delete="$emit('delete', $event)"
      @copy="$emit('copy', $event)"
      @assign="$emit('assign', $event)"
      @toggle-select="$emit('toggle-select', $event)"
      @download-mockups="$emit('download-mockups', $event)"
    />
    <div v-if="closestEdge === 'bottom'" class="drop-indicator drop-bottom" />
  </div>
</template>

<script setup>
// One card in a board column, made drag-and-drop capable with Pragmatic DnD.
// Each instance registers itself as BOTH a draggable and a drop target on mount,
// and tears the registration down on unmount — which is what makes this safe to
// use inside a virtual scroller (cards mount/unmount as they scroll in/out, and
// Pragmatic DnD is fine with the source element disappearing mid-drag).
import { ref, onMounted, onUnmounted } from 'vue'
import KanbanCard from './KanbanCard.vue'
import { combine } from '@atlaskit/pragmatic-drag-and-drop/combine'
import { draggable, dropTargetForElements } from '@atlaskit/pragmatic-drag-and-drop/element/adapter'
import { attachClosestEdge, extractClosestEdge } from '@atlaskit/pragmatic-drag-and-drop-hitbox/closest-edge'

const props = defineProps({
  card: { type: Object, required: true },
  columnId: { type: [Number, String], required: true },
  selectable: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
  boardType: { type: String, default: 'GENERAL' },
})
defineEmits(['open', 'delete', 'copy', 'assign', 'toggle-select', 'download-mockups'])

const el = ref(null)
const dragging = ref(false)
const closestEdge = ref(null) // 'top' | 'bottom' | null — where the dragged card would land
let cleanup = null

onMounted(() => {
  const element = el.value
  if (!element) return
  cleanup = combine(
    draggable({
      element,
      getInitialData: () => ({
        type: 'card',
        cardId: props.card.id,
        fromColumnId: Number(props.columnId),
      }),
      onDragStart: () => { dragging.value = true },
      onDrop: () => { dragging.value = false; closestEdge.value = null },
    }),
    dropTargetForElements({
      element,
      canDrop: ({ source }) => source.data.type === 'card',
      getIsSticky: () => true,
      getData: ({ input }) => attachClosestEdge(
        { type: 'card', cardId: props.card.id, columnId: Number(props.columnId) },
        { element, input, allowedEdges: ['top', 'bottom'] },
      ),
      onDrag: ({ self, source }) => {
        // Don't draw an insertion line on the card being dragged itself.
        if (source.data.cardId === props.card.id) { closestEdge.value = null; return }
        closestEdge.value = extractClosestEdge(self.data)
      },
      onDragLeave: () => { closestEdge.value = null },
      onDrop: () => { closestEdge.value = null },
    }),
  )
})

onUnmounted(() => { if (cleanup) cleanup() })
</script>

<style scoped>
.draggable-card { position: relative; }
.draggable-card.is-dragging { opacity: 0.4; }
.drop-indicator {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: #26a69a;
  border-radius: 2px;
  z-index: 3;
  pointer-events: none;
}
.drop-indicator::before {
  content: '';
  position: absolute;
  left: -2px;
  top: -2px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #26a69a;
}
.drop-top { top: -5px; }
.drop-bottom { bottom: -5px; }
</style>
