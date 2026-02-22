<template>
  <q-avatar :size="size" :style="avatarStyle">
    <img v-if="user?.avatarUrl" :src="user.avatarUrl + '?thumb=true'" loading="lazy" />
    <span v-else :style="{ fontSize: fontSize }">{{ initials }}</span>
  </q-avatar>
</template>

<script setup>
import { computed } from 'vue'

const PALETTE = [
  '#e53935', '#d81b60', '#8e24aa', '#5e35b1',
  '#3949ab', '#1e88e5', '#00acc1', '#00897b',
  '#43a047', '#7cb342', '#fb8c00', '#6d4c41'
]

const props = defineProps({
  user: { type: Object, default: null },
  size: { type: String, default: '28px' }
})

const fullName = computed(() => {
  if (!props.user) return ''
  const parts = [props.user.firstName, props.user.lastName].filter(Boolean)
  return parts.length ? parts.join(' ') : (props.user.userName || '')
})

const initials = computed(() => {
  const words = fullName.value.trim().split(/\s+/)
  if (words.length >= 2) return (words[0][0] + words[1][0]).toUpperCase()
  if (words.length === 1 && words[0]) return words[0][0].toUpperCase()
  return '?'
})

const deterministicColor = computed(() => {
  const key = props.user?.id ?? props.user?.userName ?? fullName.value
  let hash = 0
  const str = String(key)
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  return PALETTE[Math.abs(hash) % PALETTE.length]
})

const avatarStyle = computed(() => {
  if (props.user?.avatarUrl) return {}
  return { background: deterministicColor.value, color: '#fff' }
})

const fontSize = computed(() => {
  const px = parseInt(props.size)
  if (isNaN(px)) return '0.7rem'
  return Math.max(px * 0.38, 9) + 'px'
})
</script>
