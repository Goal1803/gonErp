<template>
  <q-page class="notes-page row no-wrap">
    <!-- ─────────── Sidebar ─────────── -->
    <aside class="notes-sidebar column no-wrap">
      <div class="notes-sidebar-top">
        <q-btn flat dense no-caps class="full-width justify-start" icon="search"
          label="Tìm kiếm" @click="searchOpen = true">
          <q-tooltip>Ctrl/Cmd + K</q-tooltip>
        </q-btn>
        <q-btn flat dense no-caps class="full-width justify-start" icon="add"
          label="Trang mới" @click="createNote(null)" />
      </div>

      <q-scroll-area class="col">
        <template v-if="store.favorites.length">
          <div class="notes-section-label">Yêu thích</div>
          <div
            v-for="fav in store.favorites"
            :key="`fav-${fav.id}`"
            class="note-tree-row"
            :class="{ 'is-active': fav.id === activeId }"
            style="padding-left: 6px"
            @click="openNote(fav.id)"
          >
            <span class="note-tree-caret is-hidden" />
            <span class="note-tree-icon">{{ fav.icon || '📄' }}</span>
            <span class="note-tree-title ellipsis">{{ fav.title }}</span>
          </div>
        </template>

        <div class="notes-section-label">Trang của tôi</div>
        <div v-if="store.loadingTree" class="q-pa-md text-center">
          <q-spinner color="green-4" size="22px" />
        </div>
        <div v-else-if="!store.tree.length" class="notes-empty-hint">
          Chưa có trang nào. Bấm “Trang mới” để bắt đầu.
        </div>
        <note-tree
          v-else
          :nodes="store.tree"
          :active-id="activeId"
          @open="openNote"
          @create-child="createNote"
          @toggle-favorite="store.toggleFavorite"
          @duplicate="duplicate"
          @rename="startRename"
          @archive="archive"
          @move="onMove"
        />

        <q-separator dark class="q-my-sm" />

        <div class="note-tree-row" @click="openTemplates">
          <span class="note-tree-caret is-hidden" />
          <q-icon name="dashboard_customize" size="15px" class="note-tree-icon" />
          <span class="note-tree-title">Mẫu ({{ store.templates.length }})</span>
        </div>
        <div class="note-tree-row" @click="openTrash">
          <span class="note-tree-caret is-hidden" />
          <q-icon name="delete_outline" size="15px" class="note-tree-icon" />
          <span class="note-tree-title">Thùng rác</span>
        </div>
      </q-scroll-area>
    </aside>

    <!-- ─────────── Main ─────────── -->
    <section class="notes-main col column no-wrap">
      <!-- Trash view -->
      <div v-if="view === 'trash'" class="notes-doc q-pa-lg">
        <div class="row items-center q-mb-md">
          <div class="text-h6">Thùng rác</div>
          <q-space />
          <q-btn v-if="store.trash.length" flat dense no-caps color="red-4"
            label="Xoá vĩnh viễn tất cả" @click="confirmEmptyTrash" />
        </div>
        <div v-if="!store.trash.length" class="text-grey-6">Thùng rác trống.</div>
        <q-list separator>
          <q-item v-for="n in store.trash" :key="n.id">
            <q-item-section avatar style="min-width: 34px">
              <span style="font-size: 17px">{{ n.icon || '📄' }}</span>
            </q-item-section>
            <q-item-section>
              <q-item-label>{{ n.title }}</q-item-label>
              <q-item-label caption>Xoá lúc {{ formatDate(n.lastUpdatedAt) }}</q-item-label>
            </q-item-section>
            <q-item-section side class="row q-gutter-xs no-wrap">
              <q-btn flat dense no-caps size="sm" label="Khôi phục" color="green-4"
                @click="store.restoreNote(n.id)" />
              <q-btn flat dense no-caps size="sm" label="Xoá hẳn" color="red-4"
                @click="confirmDeleteForever(n)" />
            </q-item-section>
          </q-item>
        </q-list>
      </div>

      <!-- Template gallery -->
      <div v-else-if="view === 'templates'" class="notes-doc q-pa-lg">
        <div class="text-h6 q-mb-xs">Mẫu</div>
        <div class="text-caption text-grey-6 q-mb-md">
          Tạo trang mới từ một mẫu, hoặc mở menu “…” của một trang bất kỳ và chọn
          “Lưu thành mẫu”.
        </div>
        <div v-if="!store.templates.length" class="text-grey-6">Chưa có mẫu nào.</div>
        <div class="row q-col-gutter-md">
          <div v-for="t in store.templates" :key="t.id" class="col-12 col-sm-6 col-md-4">
            <q-card flat bordered class="template-card">
              <q-card-section>
                <div class="text-h5 q-mb-xs">{{ t.icon || '📄' }}</div>
                <div class="text-subtitle2 ellipsis">{{ t.title }}</div>
              </q-card-section>
              <q-card-actions align="right">
                <q-btn flat dense no-caps size="sm" label="Mở" @click="openNote(t.id)" />
                <q-btn unelevated dense no-caps size="sm" color="green-7"
                  label="Dùng mẫu" @click="createFromTemplate(t.id)" />
              </q-card-actions>
            </q-card>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else-if="!note" class="notes-doc column flex-center text-grey-6"
        style="min-height: 60vh">
        <q-icon name="sticky_note_2" size="52px" class="q-mb-md" />
        <div>Chọn một trang ở bên trái, hoặc tạo trang mới.</div>
      </div>

      <!-- Document -->
      <template v-else>
        <header class="notes-header row items-center no-wrap">
          <div class="row items-center no-wrap col ellipsis">
            <template v-for="crumb in note.breadcrumb" :key="crumb.id">
              <span class="notes-crumb" @click="openNote(crumb.id)">
                {{ crumb.icon || '📄' }} {{ crumb.title }}
              </span>
              <span class="notes-crumb-sep">/</span>
            </template>
            <span class="notes-crumb is-current ellipsis">
              {{ note.icon || '📄' }} {{ note.title }}
            </span>
          </div>

          <q-space />

          <span class="notes-save-state">{{ saveLabel }}</span>

          <q-btn flat dense round size="sm" :icon="note.favorite ? 'star' : 'star_border'"
            :color="note.favorite ? 'amber-5' : 'grey-6'" @click="store.toggleFavorite(note.id)">
            <q-tooltip>{{ note.favorite ? 'Bỏ yêu thích' : 'Thêm yêu thích' }}</q-tooltip>
          </q-btn>

          <q-btn flat dense no-caps size="sm" icon="group_add" label="Chia sẻ"
            @click="shareOpen = true" />

          <q-btn flat dense round size="sm" icon="more_horiz">
            <q-menu auto-close>
              <q-list dense style="min-width: 210px">
                <q-item clickable @click="toggleFullWidth">
                  <q-item-section avatar><q-icon name="width_wide" size="18px" /></q-item-section>
                  <q-item-section>{{ note.fullWidth ? 'Chiều rộng chuẩn' : 'Toàn chiều rộng' }}</q-item-section>
                </q-item>
                <q-item v-if="note.permission === 'OWNER'" clickable @click="toggleLock">
                  <q-item-section avatar>
                    <q-icon :name="note.locked ? 'lock_open' : 'lock'" size="18px" />
                  </q-item-section>
                  <q-item-section>{{ note.locked ? 'Mở khoá trang' : 'Khoá trang' }}</q-item-section>
                </q-item>
                <q-item clickable @click="duplicate(note.id)">
                  <q-item-section avatar><q-icon name="content_copy" size="18px" /></q-item-section>
                  <q-item-section>Nhân bản</q-item-section>
                </q-item>
                <q-item clickable @click="toggleDatabase">
                  <q-item-section avatar><q-icon name="table_rows" size="18px" /></q-item-section>
                  <q-item-section>
                    {{ note.database ? 'Chuyển về trang thường' : 'Chuyển thành database' }}
                  </q-item-section>
                </q-item>
                <q-item clickable @click="toggleTemplate">
                  <q-item-section avatar><q-icon name="dashboard_customize" size="18px" /></q-item-section>
                  <q-item-section>{{ note.template ? 'Bỏ khỏi mẫu' : 'Lưu thành mẫu' }}</q-item-section>
                </q-item>
                <q-separator />
                <q-item clickable @click="archive(note.id)">
                  <q-item-section avatar><q-icon name="delete_outline" size="18px" color="red-4" /></q-item-section>
                  <q-item-section class="text-red-4">Chuyển vào thùng rác</q-item-section>
                </q-item>
              </q-list>
            </q-menu>
          </q-btn>
        </header>

        <q-scroll-area class="col">
          <div class="notes-doc" :class="{ 'is-full-width': note.fullWidth }">
            <div v-if="note.locked && note.permission !== 'OWNER'" class="notes-locked-bar">
              <q-icon name="lock" size="16px" class="q-mr-xs" /> Trang này đang bị khoá, chỉ đọc.
            </div>
            <div v-else-if="note.permission === 'VIEW'" class="notes-locked-bar">
              <q-icon name="visibility" size="16px" class="q-mr-xs" /> Bạn chỉ có quyền xem trang này.
            </div>

            <div class="notes-title-row">
              <q-btn flat dense class="notes-icon-btn" :disable="!store.canEdit">
                <span>{{ note.icon || '📄' }}</span>
                <q-menu>
                  <div class="emoji-grid">
                    <button v-for="e in EMOJIS" :key="e" type="button" class="emoji-cell"
                      @click="setIcon(e)">{{ e }}</button>
                  </div>
                  <q-separator />
                  <q-item clickable dense v-close-popup @click="setIcon('')">
                    <q-item-section class="text-caption">Bỏ biểu tượng</q-item-section>
                  </q-item>
                </q-menu>
              </q-btn>

              <input
                v-model="titleDraft"
                class="notes-title-input"
                placeholder="Untitled"
                :readonly="!store.canEdit"
                @input="queueTitleSave"
                @keydown.enter.prevent="focusEditor"
              />
            </div>

            <note-editor
              ref="editorRef"
              :key="note.id"
              :model-value="note.content || ''"
              :editable="store.canEdit"
              :note-id="note.id"
              @change="queueContentSave"
              @open-note="openNote"
              @tree-changed="store.loadTree()"
            />

            <note-database
              v-if="note.database"
              :key="`db-${note.id}`"
              :database-id="note.id"
              :readonly="!store.canEdit"
              @open-note="openNote"
            />

            <!-- Child pages, the way Notion lists sub-pages at the bottom -->
            <div v-if="note.children.length" class="notes-subpages">
              <div class="notes-section-title">Trang con</div>
              <div v-for="c in note.children" :key="c.id" class="notes-ref-row"
                @click="openNote(c.id)">
                <span class="note-tree-icon">{{ c.icon || '📄' }}</span>{{ c.title }}
              </div>
            </div>

            <div v-if="note.backlinks.length" class="notes-subpages">
              <div class="notes-section-title">
                Được nhắc tới ở ({{ note.backlinks.length }})
              </div>
              <div v-for="b in note.backlinks" :key="b.id" class="notes-ref-row"
                @click="openNote(b.id)">
                <span class="note-tree-icon">{{ b.icon || '📄' }}</span>{{ b.title }}
              </div>
            </div>

            <div class="notes-footer-meta">
              Cập nhật {{ formatDate(note.lastUpdatedAt) }}
              <span v-if="note.lastUpdatedBy"> bởi {{ note.lastUpdatedBy }}</span>
            </div>
          </div>
        </q-scroll-area>
      </template>
    </section>

    <note-search-dialog v-model="searchOpen" @open="openNote" />
    <note-share-dialog v-model="shareOpen" :note="note" />
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar, date as qdate } from 'quasar'
import { useNoteStore } from 'src/stores/noteStore'
import { databaseApi } from 'src/api/notes'
import NoteTree from 'components/notes/NoteTree.vue'
import NoteEditor from 'components/notes/NoteEditor.vue'
import NoteSearchDialog from 'components/notes/NoteSearchDialog.vue'
import NoteShareDialog from 'components/notes/NoteShareDialog.vue'
import NoteDatabase from 'components/notes/NoteDatabase.vue'

const EMOJIS = [
  '📄', '📝', '📌', '📁', '📊', '📈', '💡', '🎯', '✅', '⭐', '🔥', '🚀',
  '💼', '💰', '🧾', '📦', '🛒', '🏭', '👥', '🗓️', '⏰', '🔧', '🐛', '🎨',
  '🌍', '🇻🇳', '🇩🇪', '❤️', '⚠️', '❓', '🔒', '🏷️'
]

const route = useRoute()
const router = useRouter()
const $q = useQuasar()
const store = useNoteStore()

// 'doc' | 'trash' | 'templates'
const view = ref('doc')
const searchOpen = ref(false)
const shareOpen = ref(false)
const titleDraft = ref('')
const editorRef = ref(null)

const note = computed(() => store.current)
const activeId = computed(() => note.value?.id ?? null)

const saveLabel = computed(() => ({
  saving: 'Đang lưu…',
  saved: 'Đã lưu',
  error: 'Lưu lỗi'
}[store.saveState] || ''))

let titleTimer = null
let contentTimer = null

function formatDate (value) {
  return value ? qdate.formatDate(value, 'DD/MM/YYYY HH:mm') : ''
}

async function openNote (id) {
  if (!id) return
  view.value = 'doc'
  if (String(route.params.noteId) !== String(id)) {
    router.push({ name: 'note', params: { noteId: id } })
    return
  }
  await load(id)
}

async function load (id) {
  try {
    const n = await store.loadNote(id)
    titleDraft.value = n.title === 'Untitled' ? '' : n.title
  } catch (e) {
    const status = e.response?.status
    $q.notify({
      type: 'negative',
      message: status === 403
        ? 'Bạn không có quyền xem trang này.'
        : 'Không mở được trang.'
    })
    router.replace({ name: 'notes' })
  }
}

async function createNote (parentId) {
  const created = await store.createNote({ parentId: parentId || null })
  openNote(created.id)
}

async function createFromTemplate (templateId) {
  const created = await store.createNote({ fromTemplateId: templateId })
  openNote(created.id)
}

async function duplicate (id) {
  const copy = await store.duplicateNote(id)
  openNote(copy.id)
}

function startRename (node) {
  $q.dialog({
    title: 'Đổi tên',
    prompt: { model: node.title, type: 'text' },
    cancel: true
  }).onOk(async (title) => {
    await store.updateNote(node.id, { title })
    if (activeId.value === node.id) titleDraft.value = title
  })
}

function archive (id) {
  $q.dialog({
    title: 'Chuyển vào thùng rác',
    message: 'Trang này và mọi trang con sẽ được chuyển vào thùng rác. Bạn có thể khôi phục sau.',
    cancel: true,
    ok: { label: 'Chuyển', color: 'red-6', unelevated: true }
  }).onOk(async () => {
    await store.archiveNote(id)
    if (activeId.value === id || !store.current) router.replace({ name: 'notes' })
  })
}

function confirmDeleteForever (n) {
  $q.dialog({
    title: 'Xoá vĩnh viễn',
    message: `Xoá hẳn “${n.title}” và mọi trang con? Không thể hoàn tác.`,
    cancel: true,
    ok: { label: 'Xoá hẳn', color: 'red-6', unelevated: true }
  }).onOk(() => store.deleteForever(n.id))
}

function confirmEmptyTrash () {
  $q.dialog({
    title: 'Dọn thùng rác',
    message: 'Xoá vĩnh viễn mọi trang bạn sở hữu trong thùng rác? Không thể hoàn tác.',
    cancel: true,
    ok: { label: 'Xoá tất cả', color: 'red-6', unelevated: true }
  }).onOk(() => store.emptyTrash())
}

async function onMove ({ id, parentId, position }) {
  try {
    await store.moveNote(id, parentId, position)
  } catch (e) {
    $q.notify({
      type: 'negative',
      message: e.response?.data?.message || 'Không di chuyển được trang'
    })
  }
}

function queueTitleSave () {
  clearTimeout(titleTimer)
  store.saveState = 'saving'
  titleTimer = setTimeout(() => {
    store.updateNote(note.value.id, { title: titleDraft.value })
  }, 600)
}

function queueContentSave (json) {
  clearTimeout(contentTimer)
  store.saveState = 'saving'
  contentTimer = setTimeout(() => {
    store.updateNote(note.value.id, { content: json })
  }, 900)
}

function focusEditor () {
  editorRef.value?.editor?.commands.focus('start')
}

async function setIcon (icon) {
  await store.updateNote(note.value.id, { icon })
}

async function toggleFullWidth () {
  await store.updateNote(note.value.id, { fullWidth: !note.value.fullWidth })
}

async function toggleLock () {
  await store.updateNote(note.value.id, { locked: !note.value.locked })
}

async function toggleDatabase () {
  if (note.value.database) {
    $q.dialog({
      title: 'Chuyển về trang thường',
      message: 'Các cột, view và giá trị sẽ bị xoá. Các dòng vẫn còn dưới dạng trang con.',
      cancel: true,
      ok: { label: 'Chuyển', color: 'red-6', unelevated: true }
    }).onOk(async () => {
      await databaseApi.revert(note.value.id)
      await load(note.value.id)
      await store.loadTree()
    })
    return
  }
  await databaseApi.convert(note.value.id)
  await load(note.value.id)
  await store.loadTree()
}

async function toggleTemplate () {
  await store.updateNote(note.value.id, { template: !note.value.template })
  await Promise.all([store.loadTree(), store.loadTemplates()])
}

async function openTemplates () {
  await store.loadTemplates()
  view.value = 'templates'
}

async function openTrash () {
  await store.loadTrash()
  view.value = 'trash'
}

function onGlobalKey (e) {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    searchOpen.value = true
  }
}

watch(() => route.params.noteId, (id) => {
  if (id) load(Number(id))
})

onMounted(async () => {
  window.addEventListener('keydown', onGlobalKey)
  await Promise.all([store.loadTree(), store.loadTemplates()])
  if (route.params.noteId) await load(Number(route.params.noteId))
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onGlobalKey)
  clearTimeout(titleTimer)
  clearTimeout(contentTimer)
})
</script>
