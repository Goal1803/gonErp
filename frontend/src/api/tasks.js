import { api } from 'src/boot/axios'

export const boardApi = {
  getAll: () => api.get('/tasks/boards'),
  getById: (id) => api.get(`/tasks/boards/${id}`),
  create: (data) => api.post('/tasks/boards', data),
  update: (id, data) => api.put(`/tasks/boards/${id}`, data),
  delete: (id) => api.delete(`/tasks/boards/${id}`),
  addMember: (id, data) => api.post(`/tasks/boards/${id}/members`, data),
  removeMember: (id, userId) => api.delete(`/tasks/boards/${id}/members/${userId}`),
  getLabels: (id) => api.get(`/tasks/boards/${id}/labels`),
  createLabel: (id, data) => api.post(`/tasks/boards/${id}/labels`, data),
  deleteLabel: (labelId) => api.delete(`/tasks/boards/labels/${labelId}`),
  getTypes: (id) => api.get(`/tasks/boards/${id}/types`),
  createType: (id, data) => api.post(`/tasks/boards/${id}/types`, data),
  deleteType: (typeId) => api.delete(`/tasks/boards/types/${typeId}`)
}

export const columnApi = {
  create: (boardId, data) => api.post(`/tasks/boards/${boardId}/columns`, data),
  update: (id, data) => api.put(`/tasks/columns/${id}`, data),
  delete: (id) => api.delete(`/tasks/columns/${id}`),
  reorder: (boardId, orderedIds) => api.patch(`/tasks/boards/${boardId}/columns/reorder`, { orderedIds })
}

export const cardApi = {
  create: (columnId, data) => api.post(`/tasks/columns/${columnId}/cards`, data),
  getById: (id) => api.get(`/tasks/cards/${id}`),
  update: (id, data) => api.put(`/tasks/cards/${id}`, data),
  delete: (id) => api.delete(`/tasks/cards/${id}`),
  move: (id, data) => api.patch(`/tasks/cards/${id}/move`, data),
  reorder: (columnId, orderedIds) => api.patch(`/tasks/columns/${columnId}/cards/reorder`, { orderedIds }),
  addComment: (id, data) => api.post(`/tasks/cards/${id}/comments`, data),
  deleteComment: (id, commentId) => api.delete(`/tasks/cards/${id}/comments/${commentId}`),
  toggleReaction: (cardId, commentId, data) => api.post(`/tasks/cards/${cardId}/comments/${commentId}/reactions`, data),
  uploadCommentImage: (cardId, formData) => api.post(`/tasks/cards/${cardId}/comments/image`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  }),
  uploadAttachment: (id, formData) => api.post(`/tasks/cards/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  }),
  deleteAttachment: (id, attachmentId) => api.delete(`/tasks/cards/${id}/attachments/${attachmentId}`),
  addLabel: (id, labelId) => api.post(`/tasks/cards/${id}/labels/${labelId}`),
  removeLabel: (id, labelId) => api.delete(`/tasks/cards/${id}/labels/${labelId}`),
  addType: (id, typeId) => api.post(`/tasks/cards/${id}/types/${typeId}`),
  removeType: (id, typeId) => api.delete(`/tasks/cards/${id}/types/${typeId}`),
  addMember: (id, userId) => api.post(`/tasks/cards/${id}/members/${userId}`),
  removeMember: (id, userId) => api.delete(`/tasks/cards/${id}/members/${userId}`),
  addLink: (id, data) => api.post(`/tasks/cards/${id}/links`, data),
  deleteLink: (id, linkId) => api.delete(`/tasks/cards/${id}/links/${linkId}`)
}
