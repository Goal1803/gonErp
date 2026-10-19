<template>
  <q-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" maximized
    transition-show="fade" transition-hide="fade">
    <q-card class="search-grid-dialog column no-wrap">
      <!-- Toolbar -->
      <div class="search-grid-toolbar row items-center no-wrap q-px-lg q-py-sm q-gutter-sm">
        <q-icon name="grid_view" size="sm" color="teal-5" />
        <q-input
          v-model="query"
          dense outlined autofocus clearable
          color="teal-5"
          placeholder="Search cards..."
          style="width: 320px"
          debounce="300"
        >
          <template #prepend><q-icon name="search" size="xs" /></template>
        </q-input>
        <q-btn-toggle
          v-model="archivedFilter"
          dense no-caps unelevated
          toggle-color="teal-7"
          color="grey-9"
          text-color="grey-4"
          :options="[
            { label: 'All', value: 'all' },
            { label: 'Active', value: 'active' },
            { label: 'Archived', value: 'archived' }
          ]"
        />
        <q-select
          v-if="columnOptions.length > 1"
          v-model="columnFilter"
          :options="columnOptions"
          dense outlined emit-value map-options clearable
          color="teal-5"
          label="Column"
          style="min-width: 160px"
        />
        <div class="text-adaptive-secondary text-caption q-ml-md">
          <q-spinner v-if="loading" size="14px" class="q-mr-xs" />
          {{ filtered.length }} result{{ filtered.length === 1 ? '' : 's' }}
        </div>
        <q-space />
        <q-btn flat round dense icon="close" color="grey-5" v-close-popup />
      </div>

      <!-- Grid -->
      <div class="search-grid-body col scroll q-pa-lg">
        <div v-if="!loading && query && !filtered.length" class="text-center text-adaptive-secondary q-pa-xl">
          No cards found
        </div>
        <div v-else-if="!query" class="text-center text-adaptive-secondary q-pa-xl">
          Type to search cards
        </div>
        <div v-else class="search-grid">
          <div v-for="card in filtered" :key="card.id" class="search-grid-item" @click="$emit('open-card', card)">
            <div class="search-grid-img">
              <img v-if="card.mainImageUrl" :src="thumbUrl(card.mainImageUrl)" loading="lazy" alt="" />
              <q-icon v-else name="image_not_supported" size="32px" color="grey-7" />
              <q-badge v-if="card.archived" color="orange-8" label="Archived" class="search-grid-badge" />
            </div>
            <div class="q-pa-sm">
              <div class="search-grid-name text-adaptive" :title="card.name">{{ card.name }}</div>
              <div class="text-caption text-grey-6 ellipsis">{{ card._colName }}</div>
            </div>
          </div>
        </div>
      </div>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { cardApi } from 'src/api/tasks'
import { thumbUrl } from 'src/utils/fileUrl'

const props = defineProps({
  modelValue: Boolean,
  boardId: [Number, String],
  initialQuery: { type: String, default: '' },
  boardColumns: { type: Array, default: () => [] }
})
defineEmits(['update:modelValue', 'open-card'])

const query = ref('')
const results = ref([])
const loading = ref(false)
const archivedFilter = ref('all')
const columnFilter = ref(null)
let requestSeq = 0

const columnNameOf = (card) => {
  const col = props.boardColumns.find(c => (c.cards || []).some(cc => cc.id === card.id))
  return col?.title || card.stage || ''
}

const runSearch = async (q) => {
  const seq = ++requestSeq
  if (!q || !q.trim()) { results.value = []; loading.value = false; return }
  loading.value = true
  try {
    const res = await cardApi.search(props.boardId, q.trim(), true)
    if (seq !== requestSeq) return
    const lower = q.trim().toLowerCase()
    results.value = (res.data.data || [])
      .map(card => ({ ...card, _colName: columnNameOf(card), _exact: card.name?.toLowerCase() === lower }))
      .sort((a, b) => (b._exact ? 1 : 0) - (a._exact ? 1 : 0))
  } catch {
    if (seq === requestSeq) results.value = []
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

watch(() => props.modelValue, (open) => {
  if (!open) return
  columnFilter.value = null
  if (query.value === props.initialQuery) runSearch(query.value)
  else query.value = props.initialQuery
})
watch(query, (q) => runSearch(q))

const columnOptions = computed(() => {
  const names = [...new Set(results.value.map(c => c._colName).filter(Boolean))].sort()
  return names.map(n => ({ label: n, value: n }))
})

const filtered = computed(() => results.value.filter(c => {
  if (archivedFilter.value === 'active' && c.archived) return false
  if (archivedFilter.value === 'archived' && !c.archived) return false
  if (columnFilter.value && c._colName !== columnFilter.value) return false
  return true
}))
</script>

<style scoped>
.search-grid-dialog {
  background: var(--erp-bg);
}
.search-grid-toolbar {
  background: var(--erp-bg-secondary);
  border-bottom: 1px solid var(--erp-border-subtle);
  flex-shrink: 0;
}
.search-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}
.search-grid-item {
  background: var(--erp-bg-elevated);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.12s, border-color 0.12s;
}
.search-grid-item:hover {
  transform: translateY(-2px);
  border-color: var(--q-teal-5, #26a69a);
}
.search-grid-img {
  position: relative;
  aspect-ratio: 1 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--erp-bg-secondary);
}
.search-grid-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.search-grid-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  font-size: 9px;
}
.search-grid-name {
  font-size: 0.82rem;
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.5em;
}
@media (max-width: 600px) {
  .search-grid { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .search-grid-toolbar { flex-wrap: wrap; }
}
</style>
