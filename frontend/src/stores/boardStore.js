import { defineStore } from 'pinia'
import { ref } from 'vue'
import { boardApi, columnApi, cardApi } from 'src/api/tasks'

export const useBoardStore = defineStore('board', () => {
  const board = ref(null)
  const loading = ref(false)

  async function loadBoard(id) {
    loading.value = true
    try {
      const res = await boardApi.getById(id)
      board.value = res.data.data
    } catch (e) {
      console.error('Failed to load board', e)
    } finally {
      loading.value = false
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
    loadBoard,
    reorderColumns,
    reorderCards,
    moveCard
  }
})
