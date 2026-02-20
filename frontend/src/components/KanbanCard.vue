<template>
  <div class="kanban-card" :data-card-id="card.id" @click="$emit('open', card)">
    <!-- Delete button (visible on hover) -->
    <q-btn
      flat round dense icon="close" color="grey-6" size="xs"
      class="card-delete-btn"
      @click.stop="$emit('delete', card)"
    >
      <q-tooltip>Delete card</q-tooltip>
    </q-btn>

    <!-- Main image (always square, image fits without cropping) -->
    <div v-if="card.mainImageUrl" class="card-cover">
      <img :src="card.mainImageUrl" class="card-cover-inner" alt="" />
    </div>

    <div class="card-body">
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
          <div v-if="card.members?.length" class="row" style="gap:-4px">
            <div v-for="(m, i) in card.members.slice(0,3)" :key="m.id"
              :style="{ marginLeft: i > 0 ? '-6px' : '0', zIndex: 3-i }">
              <UserAvatar :user="m" size="18px" />
              <q-tooltip class="text-caption">{{ [m.firstName, m.lastName].filter(Boolean).join(' ') || m.userName }}</q-tooltip>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import UserAvatar from 'src/components/UserAvatar.vue'

defineProps({ card: { type: Object, required: true } })
defineEmits(['open', 'delete'])

const statusColor = (s) => ({
  OPEN: 'grey-7', IN_PROGRESS: 'blue-7', DONE: 'green-8', BLOCKED: 'red-8', CANCELLED: 'grey-9'
}[s] || 'grey-7')
</script>

<style scoped>
.kanban-card {
  position: relative;
  background: #1e1e1e;
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.15s, transform 0.1s;
  overflow: hidden;
}
.kanban-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.4);
  transform: translateY(-1px);
}
.card-delete-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  z-index: 2;
  opacity: 0;
  background: rgba(0, 0, 0, 0.6);
  transition: opacity 0.15s;
}
.kanban-card:hover .card-delete-btn {
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
  border: 1px dashed rgba(255,255,255,0.25);
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
