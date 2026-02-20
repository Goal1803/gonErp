<template>
  <div class="comment-input-wrap">
    <!-- Reply-to indicator -->
    <div v-if="replyTo" class="reply-indicator row items-center q-mb-xs">
      <q-icon name="reply" size="14px" color="teal-4" class="q-mr-xs" />
      <span class="text-teal-4" style="font-size: 0.78rem">
        Replying to <b>@{{ replyTo.author.userName }}</b>
      </span>
      <q-space />
      <q-btn flat round dense icon="close" color="grey-5" size="xs" @click="$emit('cancel-reply')" />
    </div>

    <!-- Image previews grid -->
    <div v-if="imagePreviews.length" class="preview-grid q-mb-xs">
      <div v-for="(p, i) in imagePreviews" :key="i" class="preview-item">
        <img :src="p.url" class="preview-thumb" />
        <q-btn
          flat round dense icon="close" color="red-4" size="xs"
          class="preview-remove"
          @click="removeImage(i)"
        />
      </div>
    </div>

    <!-- Input area with @mention -->
    <div class="row q-gutter-sm items-end">
      <div class="col" style="position: relative">
        <q-input
          ref="inputRef"
          v-model="text"
          outlined dark color="teal-5" dense
          placeholder="Write a comment..."
          type="textarea"
          rows="2"
          @keydown="onKeydown"
          @update:model-value="onInput"
        />
        <!-- @mention autocomplete dropdown -->
        <div
          v-if="showMentions && filteredMembers.length"
          class="mention-dropdown"
        >
          <div
            v-for="(m, i) in filteredMembers" :key="m.id"
            class="mention-item"
            :class="{ 'mention-active': i === mentionIndex }"
            @mousedown.prevent="selectMention(m)"
          >
            <UserAvatar :user="m" size="22px" />
            <span class="q-ml-xs">{{ m.firstName }} {{ m.lastName }}</span>
            <span class="text-grey-6 q-ml-xs" style="font-size: 0.72rem">@{{ m.userName }}</span>
          </div>
        </div>
      </div>
      <!-- Image attach button -->
      <q-btn
        flat round dense icon="image" color="grey-5" size="sm"
        @click="$refs.fileInput.click()"
      >
        <q-tooltip>Attach images</q-tooltip>
      </q-btn>
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        multiple
        style="display: none"
        @change="onImagesSelected"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { cardApi } from 'src/api/tasks'
import UserAvatar from 'src/components/UserAvatar.vue'

const props = defineProps({
  cardId: { type: Number, required: true },
  taggableUsers: { type: Array, default: () => [] },
  replyTo: { type: Object, default: null },
})

const emit = defineEmits(['commented', 'cancel-reply'])

const $q = useQuasar()
const text = ref('')
const sending = ref(false)
const imageFiles = ref([])       // Array of File objects
const imagePreviews = ref([])    // Array of { url, file }
const inputRef = ref(null)

// @mention state
const showMentions = ref(false)
const mentionQuery = ref('')
const mentionIndex = ref(0)
const mentionStart = ref(-1)

const filteredMembers = computed(() => {
  if (!mentionQuery.value) return props.taggableUsers.slice(0, 8)
  const q = mentionQuery.value.toLowerCase()
  return props.taggableUsers.filter(u =>
    u.userName?.toLowerCase().includes(q) ||
    u.firstName?.toLowerCase().includes(q) ||
    u.lastName?.toLowerCase().includes(q)
  ).slice(0, 8)
})

const canSend = computed(() => text.value.trim() || imageFiles.value.length)

const onInput = () => {
  requestAnimationFrame(() => {
    const val = text.value
    const textarea = inputRef.value?.$el?.querySelector('textarea')
    if (!textarea) return
    const cursorPos = textarea.selectionStart
    const beforeCursor = val.substring(0, cursorPos)

    const atIndex = beforeCursor.lastIndexOf('@')
    if (atIndex >= 0) {
      const afterAt = beforeCursor.substring(atIndex + 1)
      const charBefore = atIndex > 0 ? beforeCursor[atIndex - 1] : ' '
      if ((charBefore === ' ' || charBefore === '\n' || atIndex === 0) && !/\s/.test(afterAt)) {
        showMentions.value = true
        mentionQuery.value = afterAt
        mentionStart.value = atIndex
        mentionIndex.value = 0
        return
      }
    }
    showMentions.value = false
  })
}

const onKeydown = (e) => {
  // @mention dropdown is open — handle navigation
  if (showMentions.value && filteredMembers.value.length) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      mentionIndex.value = (mentionIndex.value + 1) % filteredMembers.value.length
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      mentionIndex.value = (mentionIndex.value - 1 + filteredMembers.value.length) % filteredMembers.value.length
    } else if (e.key === 'Enter' || e.key === 'Tab') {
      e.preventDefault()
      selectMention(filteredMembers.value[mentionIndex.value])
    } else if (e.key === 'Escape') {
      showMentions.value = false
    }
    return
  }

  // Enter to send (Shift+Enter for newline)
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    if (canSend.value && !sending.value) sendComment()
  }
}

const selectMention = (user) => {
  const textarea = inputRef.value?.$el?.querySelector('textarea')
  if (!textarea) return
  const cursorPos = textarea.selectionStart
  const before = text.value.substring(0, mentionStart.value)
  const after = text.value.substring(cursorPos)
  text.value = before + '@' + user.userName + ' ' + after
  showMentions.value = false

  const newPos = mentionStart.value + user.userName.length + 2
  requestAnimationFrame(() => {
    textarea.focus()
    textarea.setSelectionRange(newPos, newPos)
  })
}

const onImagesSelected = (e) => {
  const files = e.target.files
  if (!files?.length) return
  for (const file of files) {
    imageFiles.value.push(file)
    imagePreviews.value.push({ url: URL.createObjectURL(file), file })
  }
  e.target.value = ''
}

const removeImage = (index) => {
  URL.revokeObjectURL(imagePreviews.value[index].url)
  imagePreviews.value.splice(index, 1)
  imageFiles.value.splice(index, 1)
}

const clearImages = () => {
  for (const p of imagePreviews.value) URL.revokeObjectURL(p.url)
  imagePreviews.value = []
  imageFiles.value = []
}

const sendComment = async () => {
  if (!canSend.value) return
  sending.value = true
  try {
    // Upload all images
    const uploadedUrls = []
    for (const file of imageFiles.value) {
      const fd = new FormData()
      fd.append('file', file)
      const imgRes = await cardApi.uploadCommentImage(props.cardId, fd)
      uploadedUrls.push(imgRes.data.data.url)
    }

    // Extract mentioned user IDs
    const mentionedUserIds = []
    const mentionRegex = /@(\w+)/g
    let match
    while ((match = mentionRegex.exec(text.value)) !== null) {
      const user = props.taggableUsers.find(u => u.userName === match[1])
      if (user) mentionedUserIds.push(user.id)
    }

    const data = {
      content: text.value.trim() || null,
      imageUrls: uploadedUrls.length ? uploadedUrls : null,
      parentCommentId: props.replyTo?.id || null,
      mentionedUserIds: mentionedUserIds.length ? mentionedUserIds : null,
    }
    const res = await cardApi.addComment(props.cardId, data)
    emit('commented', res.data.data)

    text.value = ''
    clearImages()
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to add comment' })
  } finally {
    sending.value = false
  }
}

watch(() => props.replyTo, (v) => {
  if (v) {
    requestAnimationFrame(() => {
      inputRef.value?.$el?.querySelector('textarea')?.focus()
    })
  }
})
</script>

<style scoped>
.comment-input-wrap {
  margin-top: 8px;
}
.reply-indicator {
  background: rgba(38, 166, 154, 0.1);
  border: 1px solid rgba(38, 166, 154, 0.2);
  border-radius: 6px;
  padding: 4px 8px;
}

/* Preview grid */
.preview-grid {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.preview-item {
  position: relative;
}
.preview-thumb {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.preview-remove {
  position: absolute;
  top: -4px;
  right: -4px;
  background: rgba(0, 0, 0, 0.7);
}

.mention-dropdown {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  max-height: 200px;
  overflow-y: auto;
  background: #1e1e1e;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  z-index: 100;
  margin-bottom: 4px;
}
.mention-item {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  cursor: pointer;
  font-size: 0.82rem;
  color: #e0e0e0;
}
.mention-item:hover,
.mention-item.mention-active {
  background: rgba(38, 166, 154, 0.15);
}
</style>
