<template>
  <div class="db-table-wrap">
    <table class="db-table">
      <thead>
        <tr>
          <th class="db-th db-th-title">Tên</th>
          <th v-for="p in properties" :key="p.id" class="db-th">
            <div class="row items-center no-wrap">
              <q-icon :name="typeMeta(p.type).icon" size="14px" class="q-mr-xs text-grey-6" />
              <span class="ellipsis">{{ p.name }}</span>
              <q-space />
              <q-btn v-if="!readonly" flat dense round size="8px" icon="more_horiz"
                class="db-th-btn" @click="$emit('edit-property', p)" />
            </div>
          </th>
          <th class="db-th db-th-add">
            <q-btn v-if="!readonly" flat dense round size="10px" icon="add"
              @click="$emit('add-property')">
              <q-tooltip>Thêm cột</q-tooltip>
            </q-btn>
          </th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="row in rows" :key="row.id" class="db-tr">
          <td class="db-td db-td-title">
            <span class="db-row-icon">{{ row.icon || '📄' }}</span>
            <span class="db-row-title ellipsis" @click="$emit('open-row', row.id)">
              {{ row.title }}
            </span>
            <q-btn flat dense no-caps size="sm" icon="open_in_new" class="db-row-open"
              @click="$emit('open-row', row.id)">
              <q-tooltip>Mở trang</q-tooltip>
            </q-btn>
          </td>

          <td v-for="p in properties" :key="p.id" class="db-td">
            <db-property-cell
              :property="p"
              :value="row.values[String(p.id)]"
              :users="users"
              :readonly="readonly"
              @update="v => $emit('set-value', row.id, p.id, v)"
            />
          </td>
          <td class="db-td db-td-add" />
        </tr>

        <tr v-if="!rows.length">
          <td :colspan="properties.length + 2" class="db-td db-empty">
            Chưa có dòng nào khớp view này.
          </td>
        </tr>
      </tbody>
    </table>

    <q-btn v-if="!readonly" flat dense no-caps icon="add" label="Thêm dòng"
      class="db-add-row" @click="$emit('add-row')" />
  </div>
</template>

<script setup>
import DbPropertyCell from './DbPropertyCell.vue'
import { typeMeta } from './dbTypes'

defineProps({
  rows: { type: Array, default: () => [] },
  properties: { type: Array, default: () => [] },
  users: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false }
})

defineEmits(['open-row', 'set-value', 'add-row', 'add-property', 'edit-property'])
</script>
