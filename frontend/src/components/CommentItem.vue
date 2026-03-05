<template>
  <div class="comment-item-wrap" :style="{ marginLeft: isReply ? '32px' : '0' }">
    <div class="comment-item" ref="commentEl">
      <!-- Header: avatar, name, timestamp, edit, delete -->
      <div class="row items-center q-gutter-xs q-mb-xs">
        <UserAvatar :user="comment.author" size="24px" />
        <span class="text-grey-4" style="font-size: 0.8rem">{{
          comment.author.userName
        }}</span>
        <span class="text-grey-7" style="font-size: 0.72rem"
          >· {{ formatDate(comment.createdAt) }}</span
        >
        <q-space />
        <q-btn
          v-if="canEdit && !editing"
          flat round dense icon="edit" color="grey-6" size="xs"
          @click="startEdit"
        >
          <q-tooltip>Edit</q-tooltip>
        </q-btn>
        <q-btn
          v-if="canDelete && !editing"
          flat round dense icon="close" color="grey-6" size="xs"
          @click="confirmDelete"
        />
      </div>

      <!-- Edit mode -->
      <div v-if="editing" style="margin-left: 32px">
        <textarea
          ref="editTextarea"
          v-model="editContent"
          class="edit-textarea"
          rows="3"
          @keydown.escape="cancelEdit"
        />
        <div class="row q-gutter-xs q-mt-xs">
          <q-btn label="Save" color="teal-6" unelevated dense size="sm"
            :disable="!editContent.trim()" :loading="saving"
            @click="saveEdit" />
          <q-btn flat label="Cancel" color="grey-5" dense size="sm"
            @click="cancelEdit" />
        </div>
      </div>

      <!-- Content with @mention highlights (view mode) -->
      <div
        v-else-if="comment.content"
        class="text-grey-3 comment-content"
        style="font-size: 0.85rem; margin-left: 32px"
        v-html="highlightedContent"
        @mouseover="onMentionHover"
        @mouseout="onMentionOut"
      />

      <!-- Mention hover tooltip -->
      <div
        v-if="hoveredMention"
        class="mention-tooltip"
        :style="mentionTooltipStyle"
        @mouseenter="keepTooltip"
        @mouseleave="hideTooltip"
      >
        <UserAvatar :user="hoveredMention" size="40px" />
        <div>
          <div class="mention-tooltip-name">{{ hoveredMention.firstName }} {{ hoveredMention.lastName }}</div>
          <div class="mention-tooltip-username">@{{ hoveredMention.userName }}</div>
        </div>
      </div>

      <!-- Image grid -->
      <div
        v-if="comment.imageUrls?.length"
        class="comment-image-grid"
        style="margin-left: 32px; margin-top: 6px"
      >
        <div
          v-for="(url, i) in comment.imageUrls" :key="i"
          class="comment-img-wrap"
        >
          <img
            :src="thumbUrl(url)"
            class="comment-img-thumb"
            loading="lazy"
            @click="$emit('view-images', { images: comment.imageUrls, index: i })"
          />
          <div class="comment-img-actions">
            <q-btn
              flat round dense icon="download" color="white" size="xs"
              @click.stop="downloadFile(url)"
            />
          </div>
        </div>
      </div>

      <!-- Action bar: Like · count · Reply -->
      <div v-if="!editing" class="action-bar" style="margin-left: 32px; margin-top: 4px">
        <button
          class="action-link"
          :class="{ 'action-active': likeInfo?.reacted }"
          @click="$emit('react', { commentId: comment.id, reactionType: 'LIKE' })"
        >Like</button>

        <template v-if="likeInfo?.count">
          <span class="action-dot">·</span>
          <button class="like-count" @click="showLikers = !showLikers">
            <q-icon name="thumb_up" size="11px" />
            {{ likeInfo.count }}
          </button>
        </template>

        <template v-if="!isReply">
          <span class="action-dot">·</span>
          <button class="action-link" @click="$emit('reply', comment)">Reply</button>
        </template>
      </div>

      <!-- Likers popup -->
      <div v-if="showLikers && likeInfo?.users?.length" class="likers-popup" style="margin-left: 32px">
        <div
          v-for="u in likeInfo.users" :key="u.id"
          class="liker-row"
        >
          <UserAvatar :user="u" size="18px" />
          <span>{{ u.firstName || u.userName }} {{ u.lastName || '' }}</span>
        </div>
      </div>
    </div>

    <!-- Replies -->
    <template v-if="!isReply && comment.replies?.length">
      <CommentItem
        v-for="reply in comment.replies"
        :key="reply.id"
        :comment="reply"
        :is-reply="true"
        :current-user="currentUser"
        :is-admin="isAdmin"
        :members="members"
        @delete="$emit('delete', $event)"
        @edit="$emit('edit', $event)"
        @react="$emit('react', $event)"
        @view-images="$emit('view-images', $event)"
      />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { useQuasar } from 'quasar'
import UserAvatar from 'src/components/UserAvatar.vue'
import { thumbUrl } from 'src/utils/fileUrl'

const props = defineProps({
  comment: { type: Object, required: true },
  isReply: { type: Boolean, default: false },
  currentUser: { type: Object, default: null },
  isAdmin: { type: Boolean, default: false },
  members: { type: Array, default: () => [] },
})

const emit = defineEmits(['delete', 'edit', 'react', 'reply', 'view-images'])

const $q = useQuasar()
const showLikers = ref(false)
const commentEl = ref(null)
const hoveredMention = ref(null)
const mentionTooltipStyle = ref({})
let tooltipTimer = null

// Edit state
const editing = ref(false)
const editContent = ref('')
const editTextarea = ref(null)
const saving = ref(false)

const canEdit = computed(() =>
  props.comment.author.id === props.currentUser?.id
)

const canDelete = computed(() =>
  props.isAdmin || props.comment.author.id === props.currentUser?.id
)

const startEdit = () => {
  editContent.value = props.comment.content || ''
  editing.value = true
  nextTick(() => editTextarea.value?.focus())
}

const cancelEdit = () => {
  editing.value = false
  editContent.value = ''
}

const saveEdit = () => {
  if (!editContent.value.trim()) return
  emit('edit', { commentId: props.comment.id, content: editContent.value.trim() })
  editing.value = false
}

const confirmDelete = () => {
  $q.dialog({
    title: 'Delete Comment',
    message: 'Are you sure you want to delete this comment?',
    cancel: true,
    persistent: true,
    color: 'red-5',
  }).onOk(() => {
    emit('delete', props.comment)
  })
}

const likeInfo = computed(() => props.comment.reactions?.LIKE || null)

const memberMap = computed(() => {
  const map = new Map()
  for (const u of props.members) {
    if (u.userName) map.set(u.userName, u)
  }
  return map
})

const highlightedContent = computed(() => {
  if (!props.comment.content) return ''
  // Match bracket notation @[userName] (supports spaces) and legacy @word
  return props.comment.content.replace(/@\[([^\]]+)\]|@(\w+)/g, (match, bracketName, plainName) => {
    const userName = bracketName || plainName
    const user = memberMap.value.get(userName)
    if (user) {
      const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ') || userName
      return `<span class="mention-highlight" data-mention-username="${userName}">${fullName}</span>`
    }
    return `<span class="mention-highlight">${match}</span>`
  })
})

const onMentionHover = (e) => {
  const el = e.target.closest('.mention-highlight')
  if (!el) return
  clearTimeout(tooltipTimer)
  const userName = el.dataset.mentionUsername
  if (!userName) return
  const user = memberMap.value.get(userName)
  if (!user) return
  hoveredMention.value = user
  const rect = el.getBoundingClientRect()
  const parentRect = commentEl.value.getBoundingClientRect()
  mentionTooltipStyle.value = {
    top: (rect.bottom - parentRect.top + 4) + 'px',
    left: (rect.left - parentRect.left) + 'px',
  }
}

const onMentionOut = (e) => {
  const related = e.relatedTarget
  if (related?.closest?.('.mention-tooltip') || related?.closest?.('.mention-highlight')) return
  tooltipTimer = setTimeout(() => { hoveredMention.value = null }, 150)
}

const keepTooltip = () => clearTimeout(tooltipTimer)
const hideTooltip = () => { hoveredMention.value = null }

const formatDate = (d) => (d ? new Date(d).toLocaleString() : '')

const downloadFile = async (url) => {
  try {
    if (url.startsWith('http') && !url.startsWith(window.location.origin)) {
      window.open(url, '_blank')
      return
    }
    const res = await fetch(url)
    const blob = await res.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = url.split('/').pop()
    a.click()
    URL.revokeObjectURL(a.href)
  } catch { /* silent */ }
}
</script>

<style scoped>
.comment-item {
  position: relative;
  background: var(--erp-bg-tertiary);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 6px;
}
.comment-content :deep(.mention-highlight) {
  color: #4fc3f7;
  font-weight: 600;
  cursor: pointer;
}
.comment-content :deep(.mention-highlight:hover) {
  text-decoration: underline;
}

/* Edit textarea */
.edit-textarea {
  width: 100%;
  background: var(--erp-bg-elevated);
  border: 1px solid rgba(38, 166, 154, 0.5);
  border-radius: 4px;
  color: #e0e0e0;
  font-size: 0.85rem;
  padding: 8px;
  resize: vertical;
  outline: none;
  font-family: inherit;
  line-height: 1.5;
}
.edit-textarea:focus {
  border-color: #26a69a;
}

/* Mention hover tooltip */
.mention-tooltip {
  position: absolute;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--erp-bg-elevated);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}
.mention-tooltip-name {
  color: #e0e0e0;
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
}
.mention-tooltip-username {
  color: #9e9e9e;
  font-size: 0.75rem;
}

/* Image grid — 4 per row */
.comment-image-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
}
.comment-img-wrap {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: 4px;
}
.comment-img-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: opacity 0.15s;
}
.comment-img-thumb:hover {
  opacity: 0.85;
}
.comment-img-actions {
  position: absolute;
  top: 3px;
  right: 3px;
  opacity: 0;
  transition: opacity 0.15s;
}
.comment-img-actions .q-btn {
  background: rgba(0, 0, 0, 0.6);
}
.comment-img-wrap:hover .comment-img-actions {
  opacity: 1;
}

/* Action bar */
.action-bar {
  display: flex;
  align-items: center;
  gap: 6px;
}
.action-link {
  padding: 0;
  border: none;
  background: transparent;
  color: #757575;
  cursor: pointer;
  font-size: 0.72rem;
  font-weight: 600;
  transition: color 0.15s;
}
.action-link:hover {
  color: #b0bec5;
  text-decoration: underline;
}
.action-link.action-active {
  color: #80cbc4;
}
.action-dot {
  color: #555;
  font-size: 0.72rem;
  user-select: none;
}

/* Like count chip */
.like-count {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 6px;
  border: 1px solid var(--erp-border-subtle);
  border-radius: 10px;
  background: var(--erp-border-subtle);
  color: #9e9e9e;
  cursor: pointer;
  font-size: 0.7rem;
  font-weight: 600;
  transition: background 0.15s, border-color 0.15s;
}
.like-count:hover {
  background: var(--erp-border-subtle);
  border-color: var(--erp-border-subtle);
}

/* Likers popup */
.likers-popup {
  margin-top: 4px;
  padding: 6px 8px;
  background: var(--erp-bg-elevated);
  border: 1px solid var(--erp-border-subtle);
  border-radius: 6px;
  display: inline-flex;
  flex-direction: column;
  gap: 4px;
}
.liker-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #bdbdbd;
  font-size: 0.75rem;
}
</style>
