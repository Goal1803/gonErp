import { api } from 'src/boot/axios'

export const userRoleApi = {
  getAll: () => api.get('/user-roles'),
  getById: (id) => api.get(`/user-roles/${id}`),
  create: (data) => api.post('/user-roles', data),
  update: (id, data) => api.put(`/user-roles/${id}`, data),
  delete: (id) => api.delete(`/user-roles/${id}`)
}

export const userApi = {
  getAll: (params) => api.get('/users', { params }),
  getById: (id) => api.get(`/users/${id}`),
  create: (data) => api.post('/users', data),
  update: (id, data) => api.put(`/users/${id}`, data),
  delete: (id) => api.delete(`/users/${id}`),
  uploadAvatar: (id, formData) => api.post(`/users/${id}/avatar`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  }),
  deleteAvatar: (id) => api.delete(`/users/${id}/avatar`)
}
