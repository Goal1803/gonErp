import { api } from 'src/boot/axios'

export const noteApi = {
  getTree: () => api.get('/notes'),
  getFavorites: () => api.get('/notes/favorites'),
  getRecent: (limit = 10) => api.get('/notes/recent', { params: { limit } }),
  getTemplates: () => api.get('/notes/templates'),
  getTrash: () => api.get('/notes/trash'),
  getById: (id) => api.get(`/notes/${id}`),

  search: (q, limit = 30) => api.get('/notes/search', { params: { q, limit } }),
  // Backs the "@" picker inside the editor; empty q returns recent notes.
  mentions: (q, limit = 8) => api.get('/notes/mentions', { params: { q, limit } }),

  create: (data) => api.post('/notes', data),
  update: (id, data) => api.put(`/notes/${id}`, data),
  move: (id, data) => api.patch(`/notes/${id}/move`, data),
  duplicate: (id) => api.post(`/notes/${id}/duplicate`),

  // Soft delete — the note and its children go to the trash.
  archive: (id) => api.delete(`/notes/${id}`),
  restore: (id) => api.post(`/notes/${id}/restore`),
  deleteForever: (id) => api.delete(`/notes/${id}/permanent`),
  emptyTrash: () => api.delete('/notes/trash'),

  toggleFavorite: (id) => api.post(`/notes/${id}/favorite`),

  getMembers: (id) => api.get(`/notes/${id}/members`),
  addMember: (id, data) => api.post(`/notes/${id}/members`, data),
  removeMember: (id, userId) => api.delete(`/notes/${id}/members/${userId}`)
}

/** Notion-style databases: a note flagged as a database, its children are rows. */
export const databaseApi = {
  convert: (noteId) => api.post(`/notes/${noteId}/database`),
  revert: (noteId) => api.delete(`/notes/${noteId}/database`),
  getSchema: (id) => api.get(`/notes/databases/${id}`),

  createProperty: (id, data) => api.post(`/notes/databases/${id}/properties`, data),
  updateProperty: (propertyId, data) => api.put(`/notes/properties/${propertyId}`, data),
  deleteProperty: (propertyId) => api.delete(`/notes/properties/${propertyId}`),

  createView: (id, data) => api.post(`/notes/databases/${id}/views`, data),
  updateView: (viewId, data) => api.put(`/notes/views/${viewId}`, data),
  deleteView: (viewId) => api.delete(`/notes/views/${viewId}`),

  // Rows come back already filtered/sorted, and grouped when the view groups.
  getRows: (id, viewId) => api.get(`/notes/databases/${id}/rows`, { params: { viewId } }),
  createRow: (id, data) => api.post(`/notes/databases/${id}/rows`, data || {}),
  setValues: (rowId, values) => api.patch(`/notes/rows/${rowId}/values`, { values })
}
