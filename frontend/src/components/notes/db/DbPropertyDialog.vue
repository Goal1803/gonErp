<template>
  <q-dialog v-model="open" @show="reset">
    <q-card style="width: 470px; max-width: 92vw">
      <q-card-section class="row items-center q-pb-sm">
        <div class="text-subtitle1 text-weight-medium">
          {{ property ? 'Sửa cột' : 'Thêm cột' }}
        </div>
        <q-space />
        <q-btn flat dense round icon="close" v-close-popup />
      </q-card-section>

      <q-card-section class="q-gutter-md q-pt-none">
        <q-input v-model="form.name" label="Tên cột" dense outlined autofocus />

        <q-select
          v-model="form.type"
          :options="PROPERTY_TYPES"
          label="Kiểu dữ liệu"
          dense outlined emit-value map-options
        >
          <template #option="scope">
            <q-item v-bind="scope.itemProps">
              <q-item-section avatar style="min-width: 34px">
                <q-icon :name="scope.opt.icon" size="18px" />
              </q-item-section>
              <q-item-section>{{ scope.opt.label }}</q-item-section>
            </q-item>
          </template>
        </q-select>

        <div v-if="property && form.type !== property.type" class="text-caption text-orange-5">
          Đổi kiểu sẽ đọc lại toàn bộ giá trị đang có; giá trị nào không hợp kiểu mới sẽ bị xoá.
        </div>

        <q-select
          v-if="form.type === 'NUMBER'"
          v-model="form.numberFormat"
          :options="NUMBER_FORMATS"
          label="Định dạng số" dense outlined emit-value map-options
        />

        <!-- Option editor for SELECT / MULTI_SELECT -->
        <div v-if="['SELECT', 'MULTI_SELECT'].includes(form.type)">
          <div class="text-caption text-grey-6 q-mb-xs">Các lựa chọn</div>

          <div v-for="(opt, i) in form.options" :key="i" class="row items-center q-gutter-xs q-mb-xs">
            <q-input v-model="opt.label" dense outlined class="col" placeholder="Nhãn" />
            <q-btn flat dense round size="sm" :style="{ color: colorHex(opt.color) }" icon="circle">
              <q-menu auto-close>
                <div class="row q-pa-sm" style="max-width: 190px">
                  <q-btn v-for="c in OPTION_COLORS" :key="c" flat dense round size="sm"
                    icon="circle" :style="{ color: colorHex(c) }" @click="opt.color = c" />
                </div>
              </q-menu>
            </q-btn>
            <q-btn flat dense round size="sm" icon="close" color="grey-6"
              @click="form.options.splice(i, 1)" />
          </div>

          <q-btn flat dense no-caps size="sm" icon="add" label="Thêm lựa chọn"
            @click="form.options.push({ id: '', label: '', color: 'grey' })" />
        </div>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn v-if="property" flat no-caps label="Xoá cột" color="red-5" @click="remove" />
        <q-space />
        <q-btn flat no-caps label="Huỷ" v-close-popup />
        <q-btn unelevated no-caps color="green-7" label="Lưu" :disable="!form.name.trim()"
          @click="save" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useQuasar, colors } from 'quasar'
import { databaseApi } from 'src/api/notes'
import { PROPERTY_TYPES, OPTION_COLORS, parseOptions, buildOption, slugify } from './dbTypes'

const NUMBER_FORMATS = [
  { label: 'Thường', value: 'PLAIN' },
  { label: 'Số nguyên', value: 'INTEGER' },
  { label: 'Tiền (₫)', value: 'MONEY' },
  { label: 'Phần trăm', value: 'PERCENT' }
]

const props = defineProps({
  modelValue: Boolean,
  databaseId: { type: [Number, String], default: null },
  // null = creating a new column
  property: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const $q = useQuasar()
const open = ref(props.modelValue)
const form = ref({ name: '', type: 'TEXT', numberFormat: 'PLAIN', options: [] })

watch(() => props.modelValue, (v) => { open.value = v })
watch(open, (v) => emit('update:modelValue', v))

function colorHex (name) {
  try {
    return colors.getPaletteColor(name === 'grey' ? 'grey-6' : `${name}-5`)
  } catch {
    return '#9e9e9e'
  }
}

function reset () {
  if (props.property) {
    form.value = {
      name: props.property.name,
      type: props.property.type,
      numberFormat: props.property.numberFormat || 'PLAIN',
      options: parseOptions(props.property.options).map(o => ({ ...o }))
    }
  } else {
    form.value = { name: '', type: 'TEXT', numberFormat: 'PLAIN', options: [] }
  }
}

function encodeOptions () {
  const taken = []
  return form.value.options
    .filter(o => o.label.trim())
    .map(o => {
      // Keep the existing id so stored cell values keep pointing at this option.
      const id = o.id || slugify(o.label, taken)
      taken.push(id)
      return buildOption(id, o.label, o.color)
    })
}

async function save () {
  const payload = {
    name: form.value.name.trim(),
    type: form.value.type,
    numberFormat: form.value.type === 'NUMBER' ? form.value.numberFormat : null,
    options: ['SELECT', 'MULTI_SELECT'].includes(form.value.type) ? encodeOptions() : []
  }
  try {
    if (props.property) await databaseApi.updateProperty(props.property.id, payload)
    else await databaseApi.createProperty(props.databaseId, payload)
    emit('saved')
    open.value = false
  } catch (e) {
    $q.notify({ type: 'negative', message: e.response?.data?.message || 'Không lưu được cột' })
  }
}

function remove () {
  $q.dialog({
    title: 'Xoá cột',
    message: `Xoá cột “${props.property.name}” và toàn bộ giá trị của nó? Không thể hoàn tác.`,
    cancel: true,
    ok: { label: 'Xoá', color: 'red-6', unelevated: true }
  }).onOk(async () => {
    await databaseApi.deleteProperty(props.property.id)
    emit('saved')
    open.value = false
  })
}
</script>
