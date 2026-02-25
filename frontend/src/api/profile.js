import { api } from 'src/boot/axios'

export const profileApi = {
  get: () => api.get('/profile'),
  update: (data) => api.put('/profile', data),
  uploadAvatar: (formData) => api.post('/profile/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  }),
  deleteAvatar: () => api.delete('/profile/avatar'),
  changePassword: (data) => api.put('/profile/password', data)
}
