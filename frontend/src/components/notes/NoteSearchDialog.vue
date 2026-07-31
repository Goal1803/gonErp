<template>
  <q-dialog v-model="open" @show="onShow">
    <q-card class="note-search-card" style="width: 640px; max-width: 92vw">
      <q-input
        ref="inputRef"
        v-model="query"
        dark
        borderless
        autofocus
        placeholder="Tìm trong tất cả ghi chú…"
        class="note-search-input q-px-md"
        @keydown.down.prevent="move(1)"
        @keydown.up.prevent="move(-1)"
        @keydown.enter.prevent="pick(selected)"
      >
        <template #prepend><q-icon name="search" /></template>
        <template #append>
          <q-spinner v-if="loading" size="18px" color="green-4" />
        </template>
      </q-input>

      <q-separator dark />

      <q-list dark class="note-search-results">
        <q-item v-if="!query.trim()" class="text-grey-6">
          <q-item-section>Gõ để tìm theo tiêu đề hoặc nội dung.</q-item-section>
        </q-item>
        <q-item v-else-if="!loading && !results.length" class="text-grey-6">
          <q-item-section>Không tìm thấy kết quả cho “{{ query }}”.</q-item-section>
        </q-item>
        <q-item
          v-for="(r, index) in results"
          :key="r.id"
          clickable
          :active="index === selected"
          active-class="bg-grey-9"
          @mouseenter="selected = index"
          @click="pick(index)"
        >
          <q-item-section avatar style="min-width: 34px">
            <span style="font-size: 17px">{{ r.icon || '📄' }}</span>
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-adaptive">{{ r.title }}</q-item-label>
            <q-item-label caption lines="1" class="text-grey-6">
              <span v-if="r.breadcrumb" class="text-grey-7">{{ r.breadcrumb }} · </span>{{ r.snippet }}
            </q-item-label>
          </q-item-section>
        </q-item>
      </q-list>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { noteApi } from 'src/api/notes'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'open'])

const open = ref(props.modelValue)
const query = ref('')
const results = ref([])
const selected = ref(0)
const loading = ref(false)
const inputRef = ref(null)
let timer = null

watch(() => props.modelValue, (v) => { open.value = v })
watch(open, (v) => emit('update:modelValue', v))

watch(query, (q) => {
  clearTimeout(timer)
  if (!q.trim()) {
    results.value = []
    return
  }
  loading.value = true
  timer = setTimeout(async () => {
    try {
      results.value = (await noteApi.search(q.trim())).data.data || []
      selected.value = 0
    } finally {
      loading.value = false
    }
  }, 220)
})

function onShow () {
  query.value = ''
  results.value = []
  nextTick(() => inputRef.value?.focus())
}

function move (delta) {
  if (!results.value.length) return
  selected.value = (selected.value + delta + results.value.length) % results.value.length
}

function pick (index) {
  const hit = results.value[index]
  if (!hit) return
  emit('open', hit.id)
  open.value = false
}
</script>
