<template>
  <div class="note-database">
    <!-- View tabs + toolbar -->
    <div class="db-toolbar row items-center no-wrap">
      <div class="row items-center no-wrap db-tabs">
        <button
          v-for="v in schema.views"
          :key="v.id"
          type="button"
          class="db-tab"
          :class="{ 'is-active': v.id === activeViewId }"
          @click="selectView(v.id)"
        >
          <q-icon :name="VIEW_ICONS[v.type]" size="15px" class="q-mr-xs" />
          {{ v.name }}
        </button>

        <q-btn v-if="!readonly" flat dense round size="10px" icon="add" class="db-tab-add">
          <q-tooltip>Thêm view</q-tooltip>
          <q-menu auto-close>
            <q-list dense style="min-width: 150px">
              <q-item v-for="t in VIEW_TYPES" :key="t.value" clickable @click="addView(t.value)">
                <q-item-section avatar style="min-width: 32px">
                  <q-icon :name="VIEW_ICONS[t.value]" size="17px" />
                </q-item-section>
                <q-item-section>{{ t.label }}</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </div>

      <q-space />

      <span v-if="rowsData.total" class="db-count">{{ rowsData.total }} dòng</span>
      <q-badge v-if="activeView?.filters?.length" color="green-8" class="q-mr-xs"
        :label="`${activeView.filters.length} lọc`" />
      <q-badge v-if="activeView?.sorts?.length" color="blue-8" class="q-mr-xs"
        :label="`${activeView.sorts.length} sắp xếp`" />

      <q-btn v-if="!readonly" flat dense no-caps size="sm" icon="tune" label="Cấu hình view"
        :disable="!activeView" @click="configOpen = true" />
      <q-btn v-if="!readonly" flat dense no-caps size="sm" icon="add" label="Dòng mới"
        @click="addRow()" />
    </div>

    <div v-if="loading" class="q-pa-lg text-center">
      <q-spinner color="green-4" size="24px" />
    </div>

    <template v-else-if="activeView">
      <db-table-view
        v-if="activeView.type === 'TABLE'"
        :rows="rowsData.rows" :properties="visibleProperties" :users="users" :readonly="readonly"
        @open-row="$emit('open-note', $event)"
        @set-value="setValue"
        @add-row="addRow()"
        @add-property="openProperty(null)"
        @edit-property="openProperty"
      />

      <db-board-view
        v-else-if="activeView.type === 'BOARD'"
        :groups="rowsData.groups" :properties="visibleProperties" :group-property="groupProperty"
        :users="users" :readonly="readonly"
        @open-row="$emit('open-note', $event)"
        @set-value="setValue"
        @add-row="addRow"
      />

      <db-gallery-view
        v-else-if="activeView.type === 'GALLERY'"
        :rows="rowsData.rows" :properties="visibleProperties" :users="users" :readonly="readonly"
        @open-row="$emit('open-note', $event)"
        @add-row="addRow()"
      />

      <db-list-view
        v-else
        :rows="rowsData.rows" :properties="visibleProperties" :users="users" :readonly="readonly"
        @open-row="$emit('open-note', $event)"
        @add-row="addRow()"
      />
    </template>

    <db-property-dialog
      v-model="propertyOpen"
      :database-id="databaseId"
      :property="editingProperty"
      @saved="reload"
    />
    <db-view-config-dialog
      v-model="configOpen"
      :view="activeView"
      :properties="schema.properties"
      :can-delete="schema.views.length > 1"
      @saved="reload"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { databaseApi } from 'src/api/notes'
import { userApi } from 'src/api/users'
import DbTableView from './db/DbTableView.vue'
import DbBoardView from './db/DbBoardView.vue'
import DbGalleryView from './db/DbGalleryView.vue'
import DbListView from './db/DbListView.vue'
import DbPropertyDialog from './db/DbPropertyDialog.vue'
import DbViewConfigDialog from './db/DbViewConfigDialog.vue'

const VIEW_ICONS = {
  TABLE: 'table_rows', BOARD: 'view_kanban', GALLERY: 'grid_view', LIST: 'format_list_bulleted'
}
const VIEW_TYPES = [
  { label: 'Bảng', value: 'TABLE' },
  { label: 'Board', value: 'BOARD' },
  { label: 'Thư viện', value: 'GALLERY' },
  { label: 'Danh sách', value: 'LIST' }
]

const props = defineProps({
  databaseId: { type: [Number, String], required: true },
  readonly: { type: Boolean, default: false }
})

defineEmits(['open-note'])

const $q = useQuasar()
const schema = ref({ properties: [], views: [] })
const rowsData = ref({ rows: [], groups: [], total: 0, grouped: false })
const users = ref([])
const activeViewId = ref(null)
const loading = ref(false)
const propertyOpen = ref(false)
const configOpen = ref(false)
const editingProperty = ref(null)

const activeView = computed(() =>
  schema.value.views.find(v => v.id === activeViewId.value) || schema.value.views[0] || null)

const groupProperty = computed(() =>
  schema.value.properties.find(p => p.id === activeView.value?.groupById) || null)

// An empty visible list means "show everything", matching the backend contract.
const visibleProperties = computed(() => {
  const picked = activeView.value?.visiblePropertyIds || []
  if (!picked.length) return schema.value.properties
  return schema.value.properties.filter(p => picked.includes(p.id))
})

/** Remembers the last view per database so reopening a page feels stable. */
function rememberView (id) {
  localStorage.setItem(`notes.db.${props.databaseId}.view`, String(id))
}

function recallView () {
  const raw = localStorage.getItem(`notes.db.${props.databaseId}.view`)
  return raw ? Number(raw) : null
}

async function loadSchema () {
  schema.value = (await databaseApi.getSchema(props.databaseId)).data.data
  const remembered = recallView()
  const exists = schema.value.views.some(v => v.id === remembered)
  activeViewId.value = exists ? remembered : schema.value.views[0]?.id ?? null
}

async function loadRows () {
  if (!activeViewId.value) {
    rowsData.value = { rows: [], groups: [], total: 0, grouped: false }
    return
  }
  rowsData.value = (await databaseApi.getRows(props.databaseId, activeViewId.value)).data.data
}

async function reload () {
  loading.value = true
  try {
    await loadSchema()
    await loadRows()
  } finally {
    loading.value = false
  }
}

function selectView (id) {
  activeViewId.value = id
  rememberView(id)
  loadRows()
}

async function addView (type) {
  const view = (await databaseApi.createView(props.databaseId, { type })).data.data
  await loadSchema()
  selectView(view.id)
}

/** `groupKey` is set when adding straight into a board column. */
async function addRow (groupKey) {
  const values = {}
  if (groupKey && groupProperty.value) {
    values[String(groupProperty.value.id)] = groupKey
  }
  // Filters that pin a column to a value should also apply to rows created here,
  // otherwise the new row is invisible the moment it is created.
  for (const raw of activeView.value?.filters || []) {
    const [propertyId, op, ...rest] = String(raw).split(':')
    if (op === 'is' && rest.length && !values[propertyId]) {
      values[propertyId] = rest.join(':')
    }
  }
  try {
    await databaseApi.createRow(props.databaseId, { values })
    await loadRows()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Không tạo được dòng' })
  }
}

async function setValue (rowId, propertyId, value) {
  try {
    await databaseApi.setValues(rowId, { [String(propertyId)]: value })
    // Re-fetch: the change may move the row between groups or out of the filter.
    await loadRows()
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Không lưu được giá trị' })
  }
}

function openProperty (property) {
  editingProperty.value = property
  propertyOpen.value = true
}

watch(() => props.databaseId, reload)

onMounted(async () => {
  await reload()
  try {
    users.value = (await userApi.getAll()).data.data || []
  } catch {
    users.value = []
  }
})
</script>
