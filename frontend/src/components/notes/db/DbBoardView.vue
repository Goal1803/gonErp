<template>
  <div v-if="!groupProperty" class="db-empty q-pa-lg">
    View này chưa chọn cột để nhóm. Mở “Cấu hình view” và chọn một cột kiểu “Chọn một”.
  </div>

  <div v-else class="db-board">
    <div
      v-for="group in groups"
      :key="group.key ?? '__none__'"
      class="db-board-col"
      @dragover.prevent="dragOver = group.key ?? '__none__'"
      @dragleave="dragOver = null"
      @drop.prevent="onDrop(group)"
      :class="{ 'is-drop': dragOver === (group.key ?? '__none__') }"
    >
      <div class="db-board-head">
        <span class="db-chip" :class="`db-chip-${group.color || 'grey'}`">{{ group.label }}</span>
        <span class="db-board-count">{{ group.rows.length }}</span>
      </div>

      <div class="db-board-cards">
        <div
          v-for="row in group.rows"
          :key="row.id"
          class="db-card"
          :draggable="!readonly"
          @dragstart="dragging = row"
          @dragend="dragging = null"
          @click="$emit('open-row', row.id)"
        >
          <div class="db-card-title">
            <span class="db-row-icon">{{ row.icon || '📄' }}</span>{{ row.title }}
          </div>
          <div class="db-card-props">
            <template v-for="p in visibleOnCard" :key="p.id">
              <db-property-cell
                v-if="row.values[String(p.id)]"
                :property="p"
                :value="row.values[String(p.id)]"
                :users="users"
                readonly
              />
            </template>
          </div>
        </div>

        <q-btn v-if="!readonly" flat dense no-caps size="sm" icon="add" label="Thêm"
          class="db-board-add" @click="$emit('add-row', group.key)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import DbPropertyCell from './DbPropertyCell.vue'

const props = defineProps({
  groups: { type: Array, default: () => [] },
  properties: { type: Array, default: () => [] },
  groupProperty: { type: Object, default: null },
  users: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['open-row', 'set-value', 'add-row'])

const dragging = ref(null)
const dragOver = ref(null)

// The grouping column already names the card's column, so don't repeat it.
const visibleOnCard = computed(() =>
  props.properties.filter(p => p.id !== props.groupProperty?.id))

function onDrop (group) {
  dragOver.value = null
  const row = dragging.value
  dragging.value = null
  if (!row || props.readonly || !props.groupProperty) return
  emit('set-value', row.id, props.groupProperty.id, group.key ?? '')
}
</script>
