<template>
  <div class="kanban-card" :class="{ 'card-selected': selected }" :data-card-id="card.id" @click="$emit('open', card)">
    <!-- Selection checkbox (top-left, visible on hover or when selectable) -->
    <div class="card-select-checkbox" :class="{ 'always-visible': selectable || selected }">
      <q-checkbox
        :model-value="selected"
        dense
        size="xs"
        color="teal-5"
        @update:model-value="$emit('toggle-select', card)"
        @click.stop
      />
    </div>

    <!-- Action buttons (visible on hover) -->
    <div class="card-action-btns">
      <q-btn
        v-if="boardType === 'POD_DESIGN'"
        flat round dense icon="download" color="teal-3" size="xs"
        @click.stop="$emit('download-mockups', card)"
      >
        <q-tooltip>Download mockups</q-tooltip>
      </q-btn>
      <q-btn
        flat round dense icon="content_copy" color="grey-6" size="xs"
        @click.stop="$emit('copy', card)"
      >
        <q-tooltip>Copy card</q-tooltip>
      </q-btn>
      <q-btn
        flat round dense icon="close" color="grey-6" size="xs"
        @click.stop="$emit('delete', card)"
      >
        <q-tooltip>Delete card</q-tooltip>
      </q-btn>
    </div>

    <!-- Main image (always square, image fits without cropping) -->
    <div v-if="card.mainImageUrl" class="card-cover">
      <img :src="thumbUrl(card.mainImageUrl)" class="card-cover-inner" alt="" loading="lazy" draggable="false" />
    </div>

    <div class="card-body">
      <!-- Gen-type badge: distinguishes seller- vs designer-generated cards -->
      <div v-if="card.genType" class="pill-row">
        <span class="card-pill" :style="genBadgeStyle(card.genType)">{{ genLabel(card.genType) }}</span>
      </div>

      <!-- Labels & Types pills -->
      <div v-if="card.labels?.length || card.types?.length" class="pill-row">
        <span v-for="label in card.labels" :key="'l-' + label.id"
          class="card-pill"
          :style="{ background: label.color, color: label.textColor || '#fff' }">
          {{ label.name }}
        </span>
        <span v-for="t in card.types" :key="'t-' + t.id"
          class="card-pill type-pill"
          :style="{ background: t.color, color: t.textColor || '#fff' }">
          {{ t.name }}
        </span>
      </div>

      <!-- Name -->
      <div class="card-name">{{ card.name }}</div>
      <!-- SKU -->
      <div v-if="card.sku" class="text-grey-5" style="font-size: 0.7rem; margin-top: 2px;">{{ card.sku }}</div>

      <!-- Footer row -->
      <div class="card-footer">
        <!-- Status -->
        <q-chip :color="statusColor(card.status)" text-color="white" dense size="xs"
          style="height:18px; font-size:0.65rem">
          {{ card.status }}
        </q-chip>

        <!-- Meta icons -->
        <div class="row items-center q-gutter-xs">
          <div v-if="card.commentCount" class="meta-count">
            <q-icon name="chat_bubble_outline" size="12px" />{{ card.commentCount }}
          </div>
          <div v-if="card.attachmentCount" class="meta-count">
            <q-icon name="attach_file" size="12px" />{{ card.attachmentCount }}
          </div>
          <!-- Member avatars -->
          <div v-if="card.members?.length" class="row items-center" style="gap:4px">
            <div v-for="m in card.members.slice(0,3)" :key="m.id">
              <UserAvatar :user="m" size="18px" />
              <q-tooltip class="text-caption">{{ [m.firstName, m.lastName].filter(Boolean).join(' ') || m.userName }}</q-tooltip>
            </div>
          </div>
          <!-- Quick assign button (visible on hover) -->
          <q-btn
            class="card-assign-btn"
            flat round dense icon="person_add" color="grey-6" size="xs"
            @click.stop="$emit('assign', card)"
          >
            <q-tooltip>Assign member</q-tooltip>
          </q-btn>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import UserAvatar from 'src/components/UserAvatar.vue'
import { thumbUrl } from 'src/utils/fileUrl'

defineProps({
  card: { type: Object, required: true },
  selectable: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
  boardType: { type: String, default: 'GENERAL' }
})
defineEmits(['open', 'delete', 'copy', 'assign', 'toggle-select', 'download-mockups'])

const statusColor = (s) => ({
  OPEN: 'grey-7', IN_PROGRESS: 'blue-7', DONE: 'green-8', BLOCKED: 'red-8', CANCELLED: 'grey-9'
}[s] || 'grey-7')

// POD_DESIGN classification badge (designer- vs seller-generated)
const genLabel = (g) => g === 'SELLER' ? 'Seller gen' : 'Designer gen'
const genBadgeStyle = (g) => g === 'SELLER'
  ? { background: '#f57c00', color: '#fff' }   // orange — seller gen
  : { background: '#00897b', color: '#fff' }   // teal — designer gen
</script>

<style scoped>
.kanban-card {
  position: relative;
  background: var(--erp-bg-elevated);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.15s, transform 0.1s;
  overflow: hidden;
}
.kanban-card.card-selected {
  border-color: #26a69a;
  box-shadow: 0 0 0 2px rgba(38, 166, 154, 0.35);
}
.kanban-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.4);
  transform: translateY(-1px);
}
.card-select-checkbox {
  position: absolute;
  top: 4px;
  left: 4px;
  z-index: 3;
  opacity: 0;
  transition: opacity 0.15s;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 4px;
  padding: 1px;
  line-height: 0;
}
.card-select-checkbox.always-visible,
.kanban-card:hover .card-select-checkbox {
  opacity: 1;
}
.card-action-btns {
  position: absolute;
  top: 4px;
  right: 4px;
  z-index: 2;
  opacity: 0;
  display: flex;
  gap: 2px;
  transition: opacity 0.15s;
}
.card-action-btns .q-btn {
  background: rgba(0, 0, 0, 0.6);
}
.kanban-card:hover .card-action-btns {
  opacity: 1;
}
.card-assign-btn {
  opacity: 0;
  transition: opacity 0.15s;
}
.kanban-card:hover .card-assign-btn {
  opacity: 1;
}
.card-cover {
  position: relative;
  width: 100%;
  padding-top: 100%; /* 1:1 square */
}
.card-cover-inner {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  /* Prevent the browser's native image drag from hijacking the card drag
     (Pragmatic DnD must own the drag, otherwise drop targets reject it). */
  -webkit-user-drag: none;
  user-select: none;
}
.card-body { padding: 10px 12px; }

/* Pills row — labels & types together */
.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}
.card-pill {
  font-size: 0.65rem;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 9999px;
  line-height: 1.4;
  white-space: nowrap;
}
.type-pill {
  border: 1px dashed var(--erp-border-subtle);
}

/* Card name */
.card-name {
  color: #ffffffde;
  font-size: 0.85rem;
  font-weight: 500;
  line-height: 1.35;
  margin-bottom: 2px;
}

/* Footer */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.meta-count {
  display: flex;
  align-items: center;
  color: #9e9e9e;
  font-size: 0.72rem;
  gap: 2px;
}
</style>
