import { api } from 'src/boot/axios'

export const boardApi = {
  getAll: () => api.get('/tasks/boards'),
  getById: (id) => api.get(`/tasks/boards/${id}`),
  create: (data) => api.post('/tasks/boards', data),
  update: (id, data) => api.put(`/tasks/boards/${id}`, data),
  delete: (id) => api.delete(`/tasks/boards/${id}`),
  addMember: (id, data) => api.post(`/tasks/boards/${id}/members`, data),
  updateMemberRole: (id, userId, data) => api.patch(`/tasks/boards/${id}/members/${userId}`, data),
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

export const designApi = {
  getDetail: (cardId) => api.get(`/tasks/cards/${cardId}/design`),
  updateDetail: (cardId, data) => api.put(`/tasks/cards/${cardId}/design`, data),
  uploadPng: (cardId, formData) => api.post(`/tasks/cards/${cardId}/design/png-files`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }, timeout: 300000
  }),
  deletePngFile: (cardId, fileId) => api.delete(`/tasks/cards/${cardId}/design/png-files/${fileId}`),
  uploadPsd: (cardId, formData) => api.post(`/tasks/cards/${cardId}/design/psd-files`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }, timeout: 300000
  }),
  deletePsdFile: (cardId, fileId) => api.delete(`/tasks/cards/${cardId}/design/psd-files/${fileId}`),
  uploadMockup: (cardId, formData) => api.post(`/tasks/cards/${cardId}/design/mockups`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }, timeout: 300000
  }),
  deleteMockup: (cardId, mockupId) => api.delete(`/tasks/cards/${cardId}/design/mockups/${mockupId}`),
  setMainMockup: (cardId, mockupId) => api.patch(`/tasks/cards/${cardId}/design/mockups/${mockupId}/main`),
  addDesigner: (cardId, userId) => api.post(`/tasks/cards/${cardId}/design/designers/${userId}`),
  removeDesigner: (cardId, userId) => api.delete(`/tasks/cards/${cardId}/design/designers/${userId}`)
}

export const lookupApi = {
  getProductTypes: () => api.get('/tasks/product-types'),
  createProductType: (data) => api.post('/tasks/product-types', data),
  updateProductType: (id, data) => api.put(`/tasks/product-types/${id}`, data),
  deleteProductType: (id) => api.delete(`/tasks/product-types/${id}`),
  getNiches: () => api.get('/tasks/niches'),
  createNiche: (data) => api.post('/tasks/niches', data),
  updateNiche: (id, data) => api.put(`/tasks/niches/${id}`, data),
  deleteNiche: (id) => api.delete(`/tasks/niches/${id}`),
  getOccasions: () => api.get('/tasks/occasions'),
  createOccasion: (data) => api.post('/tasks/occasions', data),
  updateOccasion: (id, data) => api.put(`/tasks/occasions/${id}`, data),
  deleteOccasion: (id) => api.delete(`/tasks/occasions/${id}`)
}

export const designsApi = {
  getAll: (params) => api.get('/tasks/designs', { params })
}

export const taskConfigApi = {
  getStaffRoles: () => api.get('/tasks/config/staff-roles'),
  createStaffRole: (data) => api.post('/tasks/config/staff-roles', data),
  updateStaffRole: (id, data) => api.put(`/tasks/config/staff-roles/${id}`, data),
  deleteStaffRole: (id) => api.delete(`/tasks/config/staff-roles/${id}`),
  getUserGroups: () => api.get('/tasks/config/user-groups'),
  createUserGroup: (data) => api.post('/tasks/config/user-groups', data),
  updateUserGroup: (id, data) => api.put(`/tasks/config/user-groups/${id}`, data),
  deleteUserGroup: (id) => api.delete(`/tasks/config/user-groups/${id}`),
  getUsers: () => api.get('/tasks/config/users'),
  assignStaffRole: (userId, roleId) => api.post(`/tasks/config/users/${userId}/staff-roles/${roleId}`),
  removeStaffRole: (userId, roleId) => api.delete(`/tasks/config/users/${userId}/staff-roles/${roleId}`),
  assignUserGroup: (userId, groupId) => api.post(`/tasks/config/users/${userId}/user-groups/${groupId}`),
  removeUserGroup: (userId, groupId) => api.delete(`/tasks/config/users/${userId}/user-groups/${groupId}`)
}

export const designConfigApi = {
  getStaffRoles: () => api.get('/tasks/designs/config/staff-roles'),
  createStaffRole: (data) => api.post('/tasks/designs/config/staff-roles', data),
  updateStaffRole: (id, data) => api.put(`/tasks/designs/config/staff-roles/${id}`, data),
  deleteStaffRole: (id) => api.delete(`/tasks/designs/config/staff-roles/${id}`),
  getUserGroups: () => api.get('/tasks/designs/config/user-groups'),
  createUserGroup: (data) => api.post('/tasks/designs/config/user-groups', data),
  updateUserGroup: (id, data) => api.put(`/tasks/designs/config/user-groups/${id}`, data),
  deleteUserGroup: (id) => api.delete(`/tasks/designs/config/user-groups/${id}`),
  getUsers: () => api.get('/tasks/designs/config/users'),
  assignStaffRole: (userId, roleId) => api.post(`/tasks/designs/config/users/${userId}/staff-roles/${roleId}`),
  removeStaffRole: (userId, roleId) => api.delete(`/tasks/designs/config/users/${userId}/staff-roles/${roleId}`),
  assignUserGroup: (userId, groupId) => api.post(`/tasks/designs/config/users/${userId}/user-groups/${groupId}`),
  removeUserGroup: (userId, groupId) => api.delete(`/tasks/designs/config/users/${userId}/user-groups/${groupId}`)
}
