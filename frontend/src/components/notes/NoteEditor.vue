<template>
  <div class="note-editor-wrap">
    <bubble-menu
      v-if="editor"
      :editor="editor"
      :options="{ placement: 'top' }"
      class="note-bubble"
    >
      <q-btn v-for="mark in bubbleMarks" :key="mark.name" flat dense size="sm"
        :icon="mark.icon" :color="editor.isActive(mark.name) ? 'green-4' : 'grey-4'"
        @click="mark.run()">
        <q-tooltip>{{ mark.label }}</q-tooltip>
      </q-btn>
      <q-separator vertical dark class="q-mx-xs" />
      <q-btn flat dense size="sm" icon="link" :color="editor.isActive('link') ? 'green-4' : 'grey-4'"
        @click="setLink">
        <q-tooltip>Chèn liên kết</q-tooltip>
      </q-btn>
      <q-btn flat dense size="sm" icon="format_clear" color="grey-4"
        @click="editor.chain().focus().unsetAllMarks().run()">
        <q-tooltip>Xoá định dạng</q-tooltip>
      </q-btn>
    </bubble-menu>

    <editor-content :editor="editor" class="note-editor" />
  </div>
</template>

<script setup>
import { onBeforeUnmount, watch } from 'vue'
import { noteApi } from 'src/api/notes'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { BubbleMenu } from '@tiptap/vue-3/menus'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import Image from '@tiptap/extension-image'
import Highlight from '@tiptap/extension-highlight'
import TextAlign from '@tiptap/extension-text-align'
import { TableKit } from '@tiptap/extension-table'
import { TaskList, TaskItem } from '@tiptap/extension-list'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import { createLowlight, common } from 'lowlight'

import { Callout } from './tiptap/Callout'
import { ToggleBlock, ToggleSummary, ToggleContent } from './tiptap/ToggleBlock'
import { SlashCommand } from './tiptap/SlashCommand'
import { NoteMention } from './tiptap/NoteMention'
import { BlockHandle } from './tiptap/BlockHandle'
import { Subpage } from './tiptap/Subpage'

const props = defineProps({
  // TipTap document as a JSON string; '' means an empty note.
  modelValue: { type: String, default: '' },
  editable: { type: Boolean, default: true },
  // Needed so "/Trang con" knows which note to nest the new page under.
  noteId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:modelValue', 'change', 'open-note', 'tree-changed'])

const lowlight = createLowlight(common)

const bubbleMarks = [
  { name: 'bold', icon: 'format_bold', label: 'Đậm', run: () => editor.value.chain().focus().toggleBold().run() },
  { name: 'italic', icon: 'format_italic', label: 'Nghiêng', run: () => editor.value.chain().focus().toggleItalic().run() },
  { name: 'underline', icon: 'format_underlined', label: 'Gạch chân', run: () => editor.value.chain().focus().toggleUnderline().run() },
  { name: 'strike', icon: 'format_strikethrough', label: 'Gạch ngang', run: () => editor.value.chain().focus().toggleStrike().run() },
  { name: 'code', icon: 'code', label: 'Code', run: () => editor.value.chain().focus().toggleCode().run() },
  { name: 'highlight', icon: 'border_color', label: 'Tô nền', run: () => editor.value.chain().focus().toggleHighlight().run() }
]

function parseContent (raw) {
  if (!raw) return ''
  try {
    return JSON.parse(raw)
  } catch {
    // Older notes may hold plain text rather than a document.
    return raw
  }
}

const editor = useEditor({
  content: parseContent(props.modelValue),
  editable: props.editable,
  extensions: [
    StarterKit.configure({
      // Replaced by the lowlight-highlighted version below.
      codeBlock: false,
      link: { openOnClick: false, autolink: true, HTMLAttributes: { rel: 'noopener noreferrer' } }
    }),
    Placeholder.configure({
      placeholder: ({ node }) => {
        if (node.type.name === 'heading') return 'Tiêu đề'
        if (node.type.name === 'toggleSummary') return 'Tiêu đề mục gập'
        return "Gõ '/' để chèn khối, '@' để liên kết tới trang khác…"
      },
      includeChildren: true
    }),
    CodeBlockLowlight.configure({ lowlight }),
    TaskList,
    TaskItem.configure({ nested: true }),
    Image.configure({ inline: false, allowBase64: true }),
    Highlight.configure({ multicolor: false }),
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    TableKit.configure({ table: { resizable: true } }),
    Callout,
    Subpage,
    ToggleBlock,
    ToggleSummary,
    ToggleContent,
    NoteMention,
    SlashCommand,
    BlockHandle
  ],
  onUpdate: ({ editor: ed }) => {
    const json = JSON.stringify(ed.getJSON())
    emit('update:modelValue', json)
    emit('change', json)
  }
})

// Clicking an @mention navigates instead of following the raw hash link.
function onEditorClick (event) {
  const subpage = event.target.closest?.('.note-subpage')
  if (subpage) {
    event.preventDefault()
    const id = subpage.getAttribute('data-id')
    if (id) emit('open-note', Number(id))
    return
  }
  const mention = event.target.closest?.('.note-mention')
  if (!mention) return
  event.preventDefault()
  const id = mention.getAttribute('href')?.split('/').pop()
  if (id) emit('open-note', Number(id))
}

/** Creates a real child note, then drops a pointer to it into the document. */
async function createSubpage () {
  if (!props.noteId) return
  const created = (await noteApi.create({ parentId: props.noteId })).data.data
  editor.value?.chain().focus().insertSubpage({
    id: String(created.id), title: created.title, icon: created.icon || '📄'
  }).run()
  emit('tree-changed')
}

watch(editor, (ed) => {
  if (!ed) return
  ed.view.dom.addEventListener('click', onEditorClick)
  // The slash menu reaches the handler through storage, since an extension has
  // no way to know which note is open.
  ed.storage.subpage.create = createSubpage
})

watch(() => props.editable, (value) => {
  editor.value?.setEditable(value)
})

// Swapping notes: replace the document without pushing an update back up.
watch(() => props.modelValue, (value) => {
  if (!editor.value) return
  const incoming = JSON.stringify(parseContent(value))
  if (incoming === JSON.stringify(editor.value.getJSON())) return
  editor.value.commands.setContent(parseContent(value), { emitUpdate: false })
})

function setLink () {
  const previous = editor.value.getAttributes('link').href
  const url = window.prompt('URL:', previous || 'https://')
  if (url === null) return
  if (url === '') {
    editor.value.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }
  editor.value.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
}

onBeforeUnmount(() => {
  editor.value?.view?.dom?.removeEventListener('click', onEditorClick)
  editor.value?.destroy()
})

defineExpose({ editor })
</script>
