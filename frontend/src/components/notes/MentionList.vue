<template>
  <div class="suggestion-popup">
    <div class="suggestion-group">Liên kết tới trang</div>
    <div v-if="!items.length" class="suggestion-empty">Không tìm thấy trang</div>
    <button
      v-for="(item, index) in items"
      :key="item.id"
      type="button"
      class="suggestion-item"
      :class="{ 'is-active': index === selected }"
      @mouseenter="selected = index"
      @click="pick(index)"
    >
      <span class="suggestion-icon">{{ item.icon || '📄' }}</span>
      <span class="ellipsis">{{ item.title }}</span>
    </button>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  command: { type: Function, required: true }
})

const selected = ref(0)

watch(() => props.items, () => { selected.value = 0 })

function pick (index) {
  const item = props.items[index]
  if (item) props.command(item)
}

function move (delta) {
  if (!props.items.length) return
  selected.value = (selected.value + delta + props.items.length) % props.items.length
  nextTick(() => document.querySelector('.suggestion-item.is-active')
    ?.scrollIntoView({ block: 'nearest' }))
}

function onKeyDown ({ event }) {
  if (event.key === 'ArrowDown') { move(1); return true }
  if (event.key === 'ArrowUp') { move(-1); return true }
  if (event.key === 'Enter' || event.key === 'Tab') { pick(selected.value); return true }
  return false
}

defineExpose({ onKeyDown })
</script>
