<template>
  <div>
    <div class="db-gallery">
      <div v-for="row in rows" :key="row.id" class="db-gallery-card" @click="$emit('open-row', row.id)">
        <div class="db-gallery-cover">{{ row.icon || '📄' }}</div>
        <div class="db-gallery-body">
          <div class="db-gallery-title ellipsis">{{ row.title }}</div>
          <div class="db-card-props">
            <template v-for="p in properties" :key="p.id">
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
      </div>

      <div v-if="!readonly" class="db-gallery-card db-gallery-new" @click="$emit('add-row')">
        <q-icon name="add" size="26px" />
        <span>Thêm dòng</span>
      </div>
    </div>

    <div v-if="!rows.length" class="db-empty q-pa-md">Chưa có dòng nào khớp view này.</div>
  </div>
</template>

<script setup>
import DbPropertyCell from './DbPropertyCell.vue'

defineProps({
  rows: { type: Array, default: () => [] },
  properties: { type: Array, default: () => [] },
  users: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false }
})

defineEmits(['open-row', 'add-row'])
</script>
