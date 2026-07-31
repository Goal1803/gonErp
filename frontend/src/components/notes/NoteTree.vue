<template>
  <div class="note-tree">
    <div
      v-for="node in nodes"
      :key="node.id"
      class="note-tree-branch"
      @dragover.prevent="onDragOver($event, node)"
      @dragleave="dropTarget = null"
      @drop.prevent="onDrop($event, node)"
    >
      <div
        class="note-tree-row"
        :class="{
          'is-active': node.id === activeId,
          'is-drop-inside': dropTarget === node.id && dropMode === 'inside',
          'is-drop-after': dropTarget === node.id && dropMode === 'after'
        }"
        :style="{ paddingLeft: `${depth * 14 + 6}px` }"
        draggable="true"
        @dragstart="onDragStart($event, node)"
        @dragend="dropTarget = null"
        @click="$emit('open', node.id)"
      >
        <q-btn
          flat dense round size="9px"
          class="note-tree-caret"
          :class="{ 'is-hidden': !node.children?.length }"
          :icon="isExpanded(node.id) ? 'expand_more' : 'chevron_right'"
          @click.stop="store.toggleExpanded(node.id)"
        />

        <span class="note-tree-icon">{{ node.icon || '📄' }}</span>
        <span class="note-tree-title ellipsis">{{ node.title || 'Untitled' }}</span>

        <q-icon v-if="node.shared" name="group" size="13px" class="note-tree-badge">
          <q-tooltip>Đang chia sẻ</q-tooltip>
        </q-icon>

        <q-btn
          flat dense round size="9px" icon="add" class="note-tree-action"
          @click.stop="$emit('create-child', node.id)"
        >
          <q-tooltip>Thêm trang con</q-tooltip>
        </q-btn>

        <q-btn flat dense round size="9px" icon="more_horiz" class="note-tree-action" @click.stop>
          <q-menu auto-close>
            <q-list dense style="min-width: 180px">
              <q-item clickable @click="$emit('toggle-favorite', node.id)">
                <q-item-section avatar>
                  <q-icon :name="node.favorite ? 'star' : 'star_border'" size="18px" />
                </q-item-section>
                <q-item-section>{{ node.favorite ? 'Bỏ yêu thích' : 'Thêm yêu thích' }}</q-item-section>
              </q-item>
              <q-item clickable @click="$emit('duplicate', node.id)">
                <q-item-section avatar><q-icon name="content_copy" size="18px" /></q-item-section>
                <q-item-section>Nhân bản</q-item-section>
              </q-item>
              <q-item clickable @click="$emit('rename', node)">
                <q-item-section avatar><q-icon name="edit" size="18px" /></q-item-section>
                <q-item-section>Đổi tên</q-item-section>
              </q-item>
              <q-separator />
              <q-item clickable @click="$emit('archive', node.id)">
                <q-item-section avatar><q-icon name="delete_outline" size="18px" color="red-4" /></q-item-section>
                <q-item-section class="text-red-4">Chuyển vào thùng rác</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </div>

      <note-tree
        v-if="node.children?.length && isExpanded(node.id)"
        :nodes="node.children"
        :active-id="activeId"
        :depth="depth + 1"
        @open="$emit('open', $event)"
        @create-child="$emit('create-child', $event)"
        @toggle-favorite="$emit('toggle-favorite', $event)"
        @duplicate="$emit('duplicate', $event)"
        @rename="$emit('rename', $event)"
        @archive="$emit('archive', $event)"
        @move="$emit('move', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useNoteStore } from 'src/stores/noteStore'

defineOptions({ name: 'NoteTree' })

defineProps({
  nodes: { type: Array, default: () => [] },
  activeId: { type: [Number, String], default: null },
  depth: { type: Number, default: 0 }
})

const emit = defineEmits([
  'open', 'create-child', 'toggle-favorite', 'duplicate', 'rename', 'archive', 'move'
])

const store = useNoteStore()
const dropTarget = ref(null)
// 'inside' nests the dragged note under the row, 'after' drops it as a sibling.
const dropMode = ref('inside')

function isExpanded (id) {
  return store.expanded.has(id)
}

function onDragStart (event, node) {
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('application/x-note-id', String(node.id))
}

function onDragOver (event, node) {
  const box = event.currentTarget.getBoundingClientRect()
  // Bottom quarter of the row means "place after me", the rest means "nest inside".
  dropMode.value = (event.clientY - box.top) / box.height > 0.75 ? 'after' : 'inside'
  dropTarget.value = node.id
}

function onDrop (event, node) {
  const dragged = Number(event.dataTransfer.getData('application/x-note-id'))
  dropTarget.value = null
  if (!dragged || dragged === node.id) return
  emit('move', dropMode.value === 'inside'
    ? { id: dragged, parentId: node.id, position: null }
    : { id: dragged, parentId: node.parentId ?? null, position: node.position + 1 })
}
</script>
