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
  deleteLabel: (labelId) => api.delete(`/tasks/boards/labels/${labelId}`)
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
  addComment: (id, content) => api.post(`/tasks/cards/${id}/comments`, { content }),
  deleteComment: (id, commentId) => api.delete(`/tasks/cards/${id}/comments/${commentId}`),
  uploadAttachment: (id, formData) => api.post(`/tasks/cards/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  deleteAttachment: (id, attachmentId) => api.delete(`/tasks/cards/${id}/attachments/${attachmentId}`),
  addLabel: (id, labelId) => api.post(`/tasks/cards/${id}/labels/${labelId}`),
  removeLabel: (id, labelId) => api.delete(`/tasks/cards/${id}/labels/${labelId}`),
  addMember: (id, userId) => api.post(`/tasks/cards/${id}/members/${userId}`),
  removeMember: (id, userId) => api.delete(`/tasks/cards/${id}/members/${userId}`)
}
