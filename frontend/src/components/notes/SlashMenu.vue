<template>
  <div class="suggestion-popup">
    <div v-if="!items.length" class="suggestion-empty">Không có khối nào khớp</div>
    <template v-else>
      <template v-for="(item, index) in items" :key="item.title">
        <div v-if="item.group !== items[index - 1]?.group" class="suggestion-group">
          {{ item.group }}
        </div>
        <button
          type="button"
          class="suggestion-item"
          :class="{ 'is-active': index === selected }"
          @mouseenter="selected = index"
          @click="pick(index)"
        >
          <q-icon :name="item.icon" size="18px" class="suggestion-icon" />
          <span>{{ item.title }}</span>
        </button>
      </template>
    </template>
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
  nextTick(scrollIntoView)
}

function scrollIntoView () {
  document.querySelector('.suggestion-item.is-active')
    ?.scrollIntoView({ block: 'nearest' })
}

function onKeyDown ({ event }) {
  if (event.key === 'ArrowDown') { move(1); return true }
  if (event.key === 'ArrowUp') { move(-1); return true }
  if (event.key === 'Enter' || event.key === 'Tab') { pick(selected.value); return true }
  return false
}

defineExpose({ onKeyDown })
</script>
