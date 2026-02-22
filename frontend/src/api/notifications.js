import { api } from 'src/boot/axios'

export const notificationApi = {
  getAll: (page = 0, size = 20) => api.get('/notifications', { params: { page, size } }),
  getImportant: (page = 0, size = 20) => api.get('/notifications/important', { params: { page, size } }),
  getUnreadCount: () => api.get('/notifications/unread-count'),
  markAsRead: (id) => api.patch(`/notifications/${id}/read`),
  markAllAsRead: () => api.patch('/notifications/read-all')
}
