<template>
  <div class="kanban-card" :data-card-id="card.id" @click="$emit('open', card)">
    <!-- Main image -->
    <div v-if="card.mainImageUrl" class="card-cover"
      :style="{ backgroundImage: `url(${card.mainImageUrl})` }" />

    <div class="card-body">
      <!-- Labels -->
      <div v-if="card.labels?.length" class="row q-gutter-xs q-mb-xs">
        <div v-for="label in card.labels" :key="label.id"
          class="label-chip"
          :style="{ background: label.color }"
          :title="label.name" />
      </div>

      <!-- Name -->
      <div class="text-white text-weight-medium" style="font-size:0.88rem; line-height:1.3">
        {{ card.name }}
      </div>

      <!-- Footer row -->
      <div class="row items-center justify-between q-mt-sm">
        <!-- Status -->
        <q-chip :color="statusColor(card.status)" text-color="white" dense size="xs"
          style="height:18px; font-size:0.65rem">
          {{ card.status }}
        </q-chip>

        <!-- Meta icons -->
        <div class="row items-center q-gutter-xs">
          <div v-if="card.commentCount" class="row items-center text-grey-5" style="font-size:0.72rem; gap:2px">
            <q-icon name="chat_bubble_outline" size="12px" />{{ card.commentCount }}
          </div>
          <div v-if="card.attachmentCount" class="row items-center text-grey-5" style="font-size:0.72rem; gap:2px">
            <q-icon name="attach_file" size="12px" />{{ card.attachmentCount }}
          </div>
          <!-- Member avatars -->
          <div v-if="card.members?.length" class="row" style="gap:-4px">
            <q-avatar v-for="(m, i) in card.members.slice(0,3)" :key="m.id"
              color="teal-8" text-color="white" size="18px"
              :style="{ marginLeft: i > 0 ? '-6px' : '0', fontSize:'0.6rem', zIndex: 3-i }"
              :title="m.userName">
              {{ (m.firstName || m.userName)[0].toUpperCase() }}
            </q-avatar>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ card: { type: Object, required: true } })
defineEmits(['open'])

const statusColor = (s) => ({
  OPEN: 'grey-7', IN_PROGRESS: 'blue-7', DONE: 'green-8', BLOCKED: 'red-8', CANCELLED: 'grey-9'
}[s] || 'grey-7')
</script>

<style scoped>
.kanban-card {
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
.card-cover {
  width: 100%;
  height: 100px;
  background-size: cover;
  background-position: center;
}
.card-body { padding: 10px 12px; }
.label-chip {
  height: 6px;
  min-width: 32px;
  border-radius: 3px;
  flex: 0 0 auto;
}
</style>
