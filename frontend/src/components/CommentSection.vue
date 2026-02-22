<template>
  <div class="comment-section">
    <div class="text-caption text-grey-5 q-mb-sm row items-center gap-2">
      <q-icon name="chat" /> Comments
    </div>
    <CommentItem
      v-for="c in localComments"
      :key="c.id"
      :comment="c"
      :current-user="currentUser"
      :is-admin="isAdmin"
      :members="members"
      @delete="onDelete($event)"
      @edit="onEdit($event)"
      @react="onReact($event)"
      @reply="replyTo = $event"
      @view-images="$emit('view-images', $event)"
    />
    <CommentInput
      :card-id="entityId"
      :taggable-users="taggableUsers"
      :reply-to="replyTo"
      :add-comment-fn="addCommentFn"
      :upload-image-fn="uploadImageFn"
      @commented="onCommented"
      @cancel-reply="replyTo = null"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import CommentItem from 'src/components/CommentItem.vue'
import CommentInput from 'src/components/CommentInput.vue'

const props = defineProps({
  entityId: { type: Number, required: true },
  comments: { type: Array, default: () => [] },
  currentUser: { type: Object, default: null },
  isAdmin: { type: Boolean, default: false },
  members: { type: Array, default: () => [] },
  taggableUsers: { type: Array, default: () => [] },
  addCommentFn: { type: Function, default: null },
  updateCommentFn: { type: Function, default: null },
  deleteCommentFn: { type: Function, default: null },
  reactFn: { type: Function, default: null },
  uploadImageFn: { type: Function, default: null },
  incomingEvent: { type: Object, default: null },
})

defineEmits(['view-images'])

const $q = useQuasar()
const localComments = ref([])
const replyTo = ref(null)

// Initialize from props
watch(() => props.comments, (val) => {
  localComments.value = val ? JSON.parse(JSON.stringify(val)) : []
}, { immediate: true })

const findComment = (commentId) => {
  for (const c of localComments.value) {
    if (c.id === commentId) return c
    if (c.replies) {
      const reply = c.replies.find(r => r.id === commentId)
      if (reply) return reply
    }
  }
  return null
}

const onCommented = (comment) => {
  if (comment.parentId) {
    const parent = localComments.value.find(c => c.id === comment.parentId)
    if (parent) {
      if (!parent.replies) parent.replies = []
      parent.replies.push(comment)
    }
  } else {
    localComments.value.push(comment)
  }
  replyTo.value = null
}

const onEdit = async ({ commentId, content }) => {
  if (!props.updateCommentFn) return
  try {
    const res = await props.updateCommentFn(props.entityId, commentId, { content })
    const updated = res.data.data
    const comment = findComment(commentId)
    if (comment) {
      comment.content = updated.content
    }
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to update comment' })
  }
}

const onDelete = async (c) => {
  if (!props.deleteCommentFn) return
  try {
    await props.deleteCommentFn(props.entityId, c.id)
    if (c.parentId) {
      const parent = localComments.value.find(p => p.id === c.parentId)
      if (parent) {
        parent.replies = parent.replies.filter(r => r.id !== c.id)
      }
    } else {
      localComments.value = localComments.value.filter(x => x.id !== c.id)
    }
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to delete comment' })
  }
}

const onReact = async ({ commentId, reactionType }) => {
  if (!props.reactFn) return
  try {
    const res = await props.reactFn(props.entityId, commentId, { reactionType })
    const reactions = res.data.data
    const comment = findComment(commentId)
    if (comment) {
      comment.reactions = reactions
    }
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to toggle reaction' })
  }
}

// Watch incoming real-time events and patch local state
watch(() => props.incomingEvent, (event) => {
  if (!event) return
  const { type, payload, commentId } = event

  switch (type) {
    case 'COMMENT_ADDED': {
      if (!payload) break
      // Avoid duplicate
      const exists = findComment(payload.id)
      if (exists) break
      if (payload.parentId) {
        const parent = localComments.value.find(c => c.id === payload.parentId)
        if (parent) {
          if (!parent.replies) parent.replies = []
          if (!parent.replies.some(r => r.id === payload.id)) {
            parent.replies.push(payload)
          }
        }
      } else {
        localComments.value.push(payload)
      }
      break
    }
    case 'COMMENT_UPDATED': {
      if (!payload) break
      const comment = findComment(payload.id)
      if (comment) {
        comment.content = payload.content
      }
      break
    }
    case 'COMMENT_DELETED': {
      const delId = commentId || payload?.commentId
      if (!delId) break
      // Try top-level
      const idx = localComments.value.findIndex(c => c.id === delId)
      if (idx !== -1) {
        localComments.value.splice(idx, 1)
        break
      }
      // Try replies
      for (const c of localComments.value) {
        if (c.replies) {
          const rIdx = c.replies.findIndex(r => r.id === delId)
          if (rIdx !== -1) {
            c.replies.splice(rIdx, 1)
            break
          }
        }
      }
      break
    }
  }
})
</script>
