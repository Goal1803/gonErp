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

    <!-- Uploading indicator -->
    <div v-if="sending && imageFiles.length" class="upload-indicator q-mb-xs">
      <q-spinner-dots color="teal-5" size="20px" />
      <span class="text-grey-4" style="font-size:0.82rem">Uploading {{ imageFiles.length }} image{{ imageFiles.length > 1 ? 's' : '' }}...</span>
      <q-linear-progress indeterminate color="teal-5" size="2px" class="q-mt-xs" />
    </div>
    <!-- Image previews grid -->
    <div v-else-if="imagePreviews.length" class="preview-grid q-mb-xs">
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
        <div class="mention-editor-wrap" @click="focusEditor">
          <div
            ref="editorRef"
            contenteditable="true"
            class="mention-editor"
            :class="{ 'is-empty': !hasContent }"
            @input="onEditorInput"
            @keydown="onKeydown"
            @paste="onPaste"
          ></div>
        </div>
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
  addCommentFn: { type: Function, default: null },
  uploadImageFn: { type: Function, default: null },
})

const emit = defineEmits(['commented', 'cancel-reply'])

const $q = useQuasar()
const sending = ref(false)
const imageFiles = ref([])
const imagePreviews = ref([])
const editorRef = ref(null)
const hasContent = ref(false)

// @mention state
const showMentions = ref(false)
const mentionQuery = ref('')
const mentionIndex = ref(0)
let mentionTextNode = null
let mentionAtOffset = -1

const filteredMembers = computed(() => {
  if (!mentionQuery.value) return props.taggableUsers.slice(0, 8)
  const q = mentionQuery.value.toLowerCase()
  return props.taggableUsers.filter(u =>
    u.userName?.toLowerCase().includes(q) ||
    u.firstName?.toLowerCase().includes(q) ||
    u.lastName?.toLowerCase().includes(q)
  ).slice(0, 8)
})

const canSend = computed(() => hasContent.value || imageFiles.value.length)

const focusEditor = () => {
  editorRef.value?.focus()
}

const onEditorInput = () => {
  hasContent.value = !!editorRef.value?.textContent?.trim()
  requestAnimationFrame(() => {
    const sel = window.getSelection()
    if (!sel.rangeCount || !editorRef.value?.contains(sel.focusNode)) {
      showMentions.value = false
      return
    }
    const node = sel.focusNode
    if (node.nodeType !== Node.TEXT_NODE) {
      showMentions.value = false
      return
    }
    const cursorOffset = sel.focusOffset
    const textBefore = node.textContent.substring(0, cursorOffset)
    const atIndex = textBefore.lastIndexOf('@')
    if (atIndex >= 0) {
      const afterAt = textBefore.substring(atIndex + 1)
      const charBefore = atIndex > 0 ? textBefore[atIndex - 1] : ' '
      if ((charBefore === ' ' || charBefore === '\n' || atIndex === 0) && !/\s/.test(afterAt)) {
        showMentions.value = true
        mentionQuery.value = afterAt
        mentionTextNode = node
        mentionAtOffset = atIndex
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
  } else if (e.key === 'Enter' && e.shiftKey) {
    e.preventDefault()
    const sel = window.getSelection()
    if (!sel.rangeCount) return
    const range = sel.getRangeAt(0)
    range.deleteContents()
    const br = document.createElement('br')
    range.insertNode(br)
    range.setStartAfter(br)
    range.collapse(true)
    sel.removeAllRanges()
    sel.addRange(range)
    hasContent.value = true
  }
}

const onPaste = (e) => {
  e.preventDefault()
  const text = e.clipboardData?.getData('text/plain') || ''
  const sel = window.getSelection()
  if (!sel.rangeCount) return
  const range = sel.getRangeAt(0)
  range.deleteContents()
  const textNode = document.createTextNode(text)
  range.insertNode(textNode)
  range.setStartAfter(textNode)
  range.collapse(true)
  sel.removeAllRanges()
  sel.addRange(range)
  hasContent.value = true
}

const selectMention = (user) => {
  if (!mentionTextNode || !editorRef.value) return
  const sel = window.getSelection()
  if (!sel.rangeCount) return
  const cursorOffset = sel.focusOffset

  const fullText = mentionTextNode.textContent
  const beforeAt = fullText.substring(0, mentionAtOffset)
  const afterCursor = fullText.substring(cursorOffset)

  // Build mention token
  const span = document.createElement('span')
  span.className = 'mention-token'
  span.contentEditable = 'false'
  span.dataset.username = user.userName
  const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ') || user.userName
  span.textContent = '@' + fullName

  // Replace text node with: [beforeAt text] [mention token] [space + afterCursor text]
  const parent = mentionTextNode.parentNode
  if (beforeAt) {
    parent.insertBefore(document.createTextNode(beforeAt), mentionTextNode)
  }
  parent.insertBefore(span, mentionTextNode)
  const afterNode = document.createTextNode('\u00A0' + afterCursor)
  parent.insertBefore(afterNode, mentionTextNode)
  parent.removeChild(mentionTextNode)

  // Place cursor after the space
  const range = document.createRange()
  range.setStart(afterNode, 1)
  range.collapse(true)
  sel.removeAllRanges()
  sel.addRange(range)

  showMentions.value = false
  mentionTextNode = null
  hasContent.value = true
}

// Serialize editor DOM to text with @[userName] notation
const getEditorContent = () => {
  if (!editorRef.value) return ''
  let result = ''
  const walk = (node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      result += node.textContent.replace(/\u00A0/g, ' ')
    } else if (node.nodeName === 'BR') {
      result += '\n'
    } else if (node.classList?.contains('mention-token')) {
      result += '@[' + node.dataset.username + ']'
    } else {
      const isBlock = ['DIV', 'P'].includes(node.nodeName)
      if (isBlock && result.length > 0 && !result.endsWith('\n')) {
        result += '\n'
      }
      for (const child of node.childNodes) walk(child)
    }
  }
  for (const child of editorRef.value.childNodes) walk(child)
  return result
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
      const imgRes = props.uploadImageFn
        ? await props.uploadImageFn(props.cardId, fd)
        : await cardApi.uploadCommentImage(props.cardId, fd)
      uploadedUrls.push(imgRes.data.data.url)
    }

    const content = getEditorContent()

    // Extract mentioned user IDs from @[userName] tokens
    const mentionedUserIds = []
    const mentionRegex = /@\[([^\]]+)\]/g
    let match
    while ((match = mentionRegex.exec(content)) !== null) {
      const user = props.taggableUsers.find(u => u.userName === match[1])
      if (user) mentionedUserIds.push(user.id)
    }

    const data = {
      content: content.trim() || null,
      imageUrls: uploadedUrls.length ? uploadedUrls : null,
      parentCommentId: props.replyTo?.id || null,
      mentionedUserIds: mentionedUserIds.length ? mentionedUserIds : null,
    }
    const res = props.addCommentFn
      ? await props.addCommentFn(props.cardId, data)
      : await cardApi.addComment(props.cardId, data)
    emit('commented', res.data.data)

    // Clear
    editorRef.value.innerHTML = ''
    hasContent.value = false
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
      editorRef.value?.focus()
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

/* Contenteditable mention editor */
.mention-editor-wrap {
  border: 1px solid rgba(38, 166, 154, 0.5);
  border-radius: 4px;
  padding: 8px 12px;
  min-height: 52px;
  max-height: 200px;
  overflow-y: auto;
  cursor: text;
  transition: border-color 0.2s;
}
.mention-editor-wrap:focus-within {
  border-color: #26a69a;
}
.mention-editor {
  outline: none;
  color: #e0e0e0;
  font-size: 0.875rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
  position: relative;
}
.mention-editor.is-empty::before {
  content: 'Write a comment...';
  color: rgba(255, 255, 255, 0.3);
  pointer-events: none;
}
.mention-editor :deep(.mention-token) {
  color: #4fc3f7;
  font-weight: 600;
  background: rgba(79, 195, 247, 0.1);
  border-radius: 3px;
  padding: 1px 3px;
  user-select: all;
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
.upload-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(38, 166, 154, 0.08);
  border: 1px solid rgba(38, 166, 154, 0.2);
  border-radius: 4px;
  flex-wrap: wrap;
}
.upload-indicator .q-linear-progress {
  width: 100%;
}
</style>
