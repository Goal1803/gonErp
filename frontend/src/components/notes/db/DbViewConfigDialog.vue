<template>
  <q-dialog v-model="open" @show="reset">
    <q-card style="width: 620px; max-width: 94vw">
      <q-card-section class="row items-center q-pb-sm">
        <div class="text-subtitle1 text-weight-medium">Cấu hình view</div>
        <q-space />
        <q-btn flat dense round icon="close" v-close-popup />
      </q-card-section>

      <q-card-section class="q-gutter-md q-pt-none">
        <div class="row q-gutter-sm">
          <q-input v-model="form.name" label="Tên view" dense outlined class="col" />
          <q-select v-model="form.type" :options="VIEW_TYPES" label="Kiểu" dense outlined
            emit-value map-options style="width: 150px" />
        </div>

        <!-- Group by: required for a board, optional elsewhere -->
        <q-select
          v-model="form.groupById"
          :options="groupOptions"
          :label="form.type === 'BOARD' ? 'Nhóm theo (bắt buộc cho Board)' : 'Nhóm theo'"
          dense outlined emit-value map-options clearable
          :hint="form.type === 'BOARD' && !form.groupById
            ? 'Board cần một cột kiểu “Chọn một” để làm các cột trên bảng.' : ''"
        />

        <!-- ── Filters ── -->
        <div>
          <div class="row items-center q-mb-xs">
            <div class="text-caption text-grey-6">Bộ lọc (tất cả điều kiện phải đúng)</div>
            <q-space />
            <q-btn flat dense no-caps size="sm" icon="add" label="Thêm điều kiện"
              :disable="!properties.length" @click="addFilter" />
          </div>

          <div v-if="!form.filters.length" class="text-caption text-grey-7">Không lọc.</div>

          <div v-for="(f, i) in form.filters" :key="`f${i}`" class="row items-center q-gutter-xs q-mb-xs">
            <q-select v-model="f.propertyId" :options="propertyOptions" dense outlined
              emit-value map-options style="width: 150px" @update:model-value="onFilterPropertyChange(f)" />
            <q-select v-model="f.op" :options="operatorsFor(typeOf(f.propertyId))" dense outlined
              emit-value map-options style="width: 130px" />
            <template v-if="!isNoOperand(f)">
              <q-select v-if="isOptionType(f.propertyId)" v-model="f.value"
                :options="optionChoices(f.propertyId)" dense outlined emit-value map-options class="col" />
              <q-input v-else v-model="f.value" dense outlined class="col"
                :type="inputTypeOf(f.propertyId)" placeholder="Giá trị" />
            </template>
            <q-space v-else />
            <q-btn flat dense round size="sm" icon="close" color="grey-6"
              @click="form.filters.splice(i, 1)" />
          </div>
        </div>

        <!-- ── Sorts ── -->
        <div>
          <div class="row items-center q-mb-xs">
            <div class="text-caption text-grey-6">Sắp xếp (theo thứ tự ưu tiên)</div>
            <q-space />
            <q-btn flat dense no-caps size="sm" icon="add" label="Thêm sắp xếp"
              :disable="!properties.length" @click="addSort" />
          </div>

          <div v-if="!form.sorts.length" class="text-caption text-grey-7">
            Theo thứ tự thêm vào.
          </div>

          <div v-for="(s, i) in form.sorts" :key="`s${i}`" class="row items-center q-gutter-xs q-mb-xs">
            <q-select v-model="s.propertyId" :options="propertyOptions" dense outlined
              emit-value map-options class="col" />
            <q-select v-model="s.dir" :options="DIRECTIONS" dense outlined emit-value map-options
              style="width: 130px" />
            <q-btn flat dense round size="sm" icon="close" color="grey-6"
              @click="form.sorts.splice(i, 1)" />
          </div>
        </div>

        <!-- ── Visible columns ── -->
        <div>
          <div class="text-caption text-grey-6 q-mb-xs">Cột hiển thị</div>
          <q-option-group
            v-model="form.visiblePropertyIds"
            :options="propertyOptions"
            type="checkbox" dense inline color="green-6"
          />
          <div class="text-caption text-grey-7">Bỏ tick tất cả để hiện mọi cột.</div>
        </div>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn v-if="canDelete" flat no-caps label="Xoá view" color="red-5" @click="remove" />
        <q-space />
        <q-btn flat no-caps label="Huỷ" v-close-popup />
        <q-btn unelevated no-caps color="green-7" label="Lưu" @click="save" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { databaseApi } from 'src/api/notes'
import { operatorsFor, parseOptions } from './dbTypes'

const VIEW_TYPES = [
  { label: 'Bảng', value: 'TABLE' },
  { label: 'Board', value: 'BOARD' },
  { label: 'Thư viện', value: 'GALLERY' },
  { label: 'Danh sách', value: 'LIST' }
]

const DIRECTIONS = [
  { label: 'Tăng dần', value: 'ASC' },
  { label: 'Giảm dần', value: 'DESC' }
]

const props = defineProps({
  modelValue: Boolean,
  view: { type: Object, default: null },
  properties: { type: Array, default: () => [] },
  canDelete: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const $q = useQuasar()
const open = ref(props.modelValue)
const form = ref({
  name: '', type: 'TABLE', groupById: null, filters: [], sorts: [], visiblePropertyIds: []
})

watch(() => props.modelValue, (v) => { open.value = v })
watch(open, (v) => emit('update:modelValue', v))

const propertyOptions = computed(() =>
  props.properties.map(p => ({ label: p.name, value: p.id })))

// Only single-select columns make sensible board columns.
const groupOptions = computed(() =>
  props.properties.filter(p => p.type === 'SELECT').map(p => ({ label: p.name, value: p.id })))

function typeOf (propertyId) {
  return props.properties.find(p => p.id === propertyId)?.type || 'TEXT'
}

function isOptionType (propertyId) {
  return ['SELECT', 'MULTI_SELECT'].includes(typeOf(propertyId))
}

function inputTypeOf (propertyId) {
  const t = typeOf(propertyId)
  if (t === 'NUMBER') return 'number'
  if (t === 'DATE') return 'date'
  return 'text'
}

function optionChoices (propertyId) {
  const p = props.properties.find(x => x.id === propertyId)
  return parseOptions(p?.options).map(o => ({ label: o.label, value: o.id }))
}

function isNoOperand (f) {
  return operatorsFor(typeOf(f.propertyId)).find(o => o.value === f.op)?.noOperand === true
}

function onFilterPropertyChange (f) {
  // Operators are type-specific, so reset to the first valid one.
  f.op = operatorsFor(typeOf(f.propertyId))[0].value
  f.value = ''
}

function reset () {
  const v = props.view
  form.value = {
    name: v?.name || 'View',
    type: v?.type || 'TABLE',
    groupById: v?.groupById ?? null,
    visiblePropertyIds: [...(v?.visiblePropertyIds || [])],
    filters: (v?.filters || []).map(raw => {
      const [propertyId, op, ...rest] = String(raw).split(':')
      return { propertyId: Number(propertyId), op, value: rest.join(':') }
    }),
    sorts: (v?.sorts || []).map(raw => {
      const [propertyId, dir] = String(raw).split(':')
      return { propertyId: Number(propertyId), dir: dir || 'ASC' }
    })
  }
}

function addFilter () {
  const p = props.properties[0]
  form.value.filters.push({
    propertyId: p.id, op: operatorsFor(p.type)[0].value, value: ''
  })
}

function addSort () {
  form.value.sorts.push({ propertyId: props.properties[0].id, dir: 'ASC' })
}

async function save () {
  const payload = {
    name: form.value.name.trim() || 'View',
    type: form.value.type,
    // 0 is the agreed "clear it" signal, since null means "leave unchanged".
    groupById: form.value.groupById ?? 0,
    visiblePropertyIds: form.value.visiblePropertyIds,
    filters: form.value.filters
      .filter(f => f.propertyId && f.op)
      .map(f => `${f.propertyId}:${f.op}:${isNoOperand(f) ? '' : f.value ?? ''}`),
    sorts: form.value.sorts
      .filter(s => s.propertyId)
      .map(s => `${s.propertyId}:${s.dir}`)
  }
  try {
    await databaseApi.updateView(props.view.id, payload)
    emit('saved')
    open.value = false
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Không lưu được view' })
  }
}

function remove () {
  $q.dialog({
    title: 'Xoá view',
    message: `Xoá view “${props.view.name}”? Dữ liệu các dòng vẫn giữ nguyên.`,
    cancel: true,
    ok: { label: 'Xoá', color: 'red-6', unelevated: true }
  }).onOk(async () => {
    try {
      await databaseApi.deleteView(props.view.id)
      emit('saved')
      open.value = false
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data?.message || 'Không xoá được view' })
    }
  })
}
</script>
