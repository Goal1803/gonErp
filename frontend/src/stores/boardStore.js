import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { boardApi, columnApi, cardApi } from 'src/api/tasks'

export const useBoardStore = defineStore('board', () => {
  const board = ref(null)
  const loading = ref(false)
  // Lazy card loading: when pageSize is set, the board loads only the first
  // page of cards per column and the rest are fetched on scroll. filterParams
  // holds the active server-side filter so "load more" stays consistent.
  const pageSize = ref(null)
  const filterParams = ref({})
  const loadingMore = reactive({}) // columnId -> bool

  async function loadBoard(id, opts = {}) {
    loading.value = true
    if (opts.pageSize !== undefined) pageSize.value = opts.pageSize
    if (opts.filter !== undefined) filterParams.value = opts.filter || {}
    try {
      const params = { ...filterParams.value }
      if (pageSize.value) params.pageSize = pageSize.value
      const res = await boardApi.getById(id, params)
      board.value = res.data.data
    } catch (e) {
      console.error('Failed to load board', e)
    } finally {
      loading.value = false
    }
  }

  function columnHasMore(column) {
    if (!pageSize.value || !column) return false
    return (column.cards?.length || 0) < (column.totalCards || 0)
  }

  async function loadMoreCards(columnId) {
    if (!pageSize.value) return
    const col = board.value?.columns?.find(c => c.id === columnId)
    if (!col || loadingMore[columnId]) return
    if ((col.cards?.length || 0) >= (col.totalCards || 0)) return
    loadingMore[columnId] = true
    try {
      const page = Math.floor((col.cards?.length || 0) / pageSize.value)
      const params = { ...filterParams.value, page, size: pageSize.value }
      const res = await columnApi.cards(columnId, params)
      const data = res.data.data || {}
      const existing = new Set((col.cards || []).map(c => c.id))
      const fresh = (data.content || []).filter(c => !existing.has(c.id))
      col.cards = [...(col.cards || []), ...fresh]
      if (typeof data.total === 'number') col.totalCards = data.total
    } catch (e) {
      console.error('Failed to load more cards', e)
    } finally {
      loadingMore[columnId] = false
    }
  }

  async function reorderColumns(boardId, orderedIds) {
    // Capture previous order for rollback
    const previousIds = board.value?.columns?.map(c => c.id) || []
    // Optimistic update
    if (board.value?.columns) {
      const colMap = Object.fromEntries(board.value.columns.map(c => [c.id, c]))
      board.value.columns = orderedIds.map((id, i) => ({ ...colMap[id], position: i + 1 }))
    }
    try {
      await columnApi.reorder(boardId, orderedIds)
    } catch (e) {
      // Rollback
      if (board.value?.columns) {
        const colMap = Object.fromEntries(board.value.columns.map(c => [c.id, c]))
        board.value.columns = previousIds.map((id, i) => ({ ...colMap[id], position: i + 1 }))
      }
      console.error('Failed to reorder columns', e)
    }
  }

  async function reorderCards(columnId, orderedIds) {
    try {
      await cardApi.reorder(columnId, orderedIds)
    } catch (e) {
      console.error('Failed to persist card reorder', e)
    }
  }

  async function moveCard(cardId, sourceColumnId, targetColumnId, position) {
    try {
      await cardApi.move(cardId, { targetColumnId, position })
    } catch (e) {
      console.error('Failed to move card', e)
      // Reload board to revert optimistic change
      if (board.value) await loadBoard(board.value.id)
    }
  }

  return {
    board,
    loading,
    pageSize,
    filterParams,
    loadingMore,
    loadBoard,
    columnHasMore,
    loadMoreCards,
    reorderColumns,
    reorderCards,
    moveCard
  }
})
