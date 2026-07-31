import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { noteApi } from 'src/api/notes'

export const useNoteStore = defineStore('note', () => {
  const tree = ref([])
  const favorites = ref([])
  const templates = ref([])
  const trash = ref([])
  const current = ref(null)

  const loadingTree = ref(false)
  const loadingNote = ref(false)
  // 'saving' | 'saved' | 'error' | null — drives the header indicator.
  const saveState = ref(null)

  // Sidebar disclosure state, persisted so the tree looks the same next visit.
  const expanded = ref(new Set(readExpanded()))

  const canEdit = computed(() => {
    const n = current.value
    if (!n || n.archived) return false
    if (n.locked && n.permission !== 'OWNER') return false
    return n.permission === 'OWNER' || n.permission === 'EDIT'
  })

  function readExpanded () {
    try {
      return JSON.parse(localStorage.getItem('notes.expanded') || '[]')
    } catch {
      return []
    }
  }

  function persistExpanded () {
    localStorage.setItem('notes.expanded', JSON.stringify([...expanded.value]))
  }

  function toggleExpanded (id) {
    if (expanded.value.has(id)) expanded.value.delete(id)
    else expanded.value.add(id)
    expanded.value = new Set(expanded.value)
    persistExpanded()
  }

  function expand (id) {
    if (id == null || expanded.value.has(id)) return
    expanded.value.add(id)
    expanded.value = new Set(expanded.value)
    persistExpanded()
  }

  async function loadTree () {
    loadingTree.value = true
    try {
      const [t, f] = await Promise.all([noteApi.getTree(), noteApi.getFavorites()])
      tree.value = t.data.data || []
      favorites.value = f.data.data || []
    } finally {
      loadingTree.value = false
    }
  }

  async function loadTemplates () {
    templates.value = (await noteApi.getTemplates()).data.data || []
  }

  async function loadTrash () {
    trash.value = (await noteApi.getTrash()).data.data || []
  }

  async function loadNote (id) {
    loadingNote.value = true
    saveState.value = null
    try {
      current.value = (await noteApi.getById(id)).data.data
      // Reveal the note's ancestors so the sidebar highlights where we are.
      ;(current.value.breadcrumb || []).forEach(b => expand(b.id))
      return current.value
    } finally {
      loadingNote.value = false
    }
  }

  async function createNote (payload = {}) {
    const note = (await noteApi.create(payload)).data.data
    await loadTree()
    if (payload.parentId) expand(payload.parentId)
    return note
  }

  /**
   * Patch a note. `silent` skips the save indicator, used by the editor's
   * autosave so the header doesn't flicker on every keystroke pause.
   */
  async function updateNote (id, payload, { silent = false } = {}) {
    if (!silent) saveState.value = 'saving'
    try {
      const note = (await noteApi.update(id, payload)).data.data
      if (current.value?.id === id) {
        // Keep the relations the PUT response does not carry.
        current.value = { ...current.value, ...note }
      }
      if (payload.title !== undefined || payload.icon !== undefined) {
        patchTreeNode(id, { title: note.title, icon: note.icon })
      }
      saveState.value = 'saved'
      return note
    } catch (e) {
      saveState.value = 'error'
      throw e
    }
  }

  async function moveNote (id, parentId, position) {
    await noteApi.move(id, { parentId, position })
    await loadTree()
    if (parentId) expand(parentId)
  }

  async function duplicateNote (id) {
    const copy = (await noteApi.duplicate(id)).data.data
    await loadTree()
    return copy
  }

  async function archiveNote (id) {
    await noteApi.archive(id)
    await loadTree()
    if (current.value?.id === id) current.value = null
  }

  async function restoreNote (id) {
    await noteApi.restore(id)
    await Promise.all([loadTree(), loadTrash()])
  }

  async function deleteForever (id) {
    await noteApi.deleteForever(id)
    await loadTrash()
    if (current.value?.id === id) current.value = null
  }

  async function emptyTrash () {
    await noteApi.emptyTrash()
    await loadTrash()
  }

  async function toggleFavorite (id) {
    const favorite = (await noteApi.toggleFavorite(id)).data.data.favorite
    if (current.value?.id === id) current.value.favorite = favorite
    patchTreeNode(id, { favorite })
    favorites.value = (await noteApi.getFavorites()).data.data || []
    return favorite
  }

  /** Update one node in place so the sidebar reacts without a full reload. */
  function patchTreeNode (id, patch) {
    const walk = (nodes) => {
      for (const n of nodes) {
        if (n.id === id) {
          Object.assign(n, patch)
          return true
        }
        if (n.children?.length && walk(n.children)) return true
      }
      return false
    }
    walk(tree.value)
    const fav = favorites.value.find(f => f.id === id)
    if (fav) Object.assign(fav, patch)
  }

  function findNode (id, nodes = tree.value) {
    for (const n of nodes) {
      if (n.id === id) return n
      const hit = n.children?.length ? findNode(id, n.children) : null
      if (hit) return hit
    }
    return null
  }

  return {
    tree, favorites, templates, trash, current,
    loadingTree, loadingNote, saveState, expanded, canEdit,
    loadTree, loadTemplates, loadTrash, loadNote,
    createNote, updateNote, moveNote, duplicateNote,
    archiveNote, restoreNote, deleteForever, emptyTrash,
    toggleFavorite, toggleExpanded, expand, findNode, patchTreeNode
  }
})
