<template>
  <div>
    <div v-for="row in rows" :key="row.id" class="db-list-row" @click="$emit('open-row', row.id)">
      <span class="db-row-icon">{{ row.icon || '📄' }}</span>
      <span class="db-list-title ellipsis">{{ row.title }}</span>
      <div class="db-list-props">
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

    <div v-if="!rows.length" class="db-empty q-pa-md">Chưa có dòng nào khớp view này.</div>

    <q-btn v-if="!readonly" flat dense no-caps icon="add" label="Thêm dòng"
      class="db-add-row" @click="$emit('add-row')" />
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
