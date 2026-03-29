import { api } from 'src/boot/axios'

export const ecomStoreApi = {
  getAll: () => api.get('/ecommerce/stores'),
  getById: (id) => api.get(`/ecommerce/stores/${id}`),
  create: (data) => api.post('/ecommerce/stores', data),
  update: (id, data) => api.put(`/ecommerce/stores/${id}`, data),
  delete: (id) => api.delete(`/ecommerce/stores/${id}`)
}

export const ecomStoreMemberApi = {
  getByStore: (storeId) => api.get(`/ecommerce/stores/${storeId}/members`),
  assign: (storeId, data) => api.post(`/ecommerce/stores/${storeId}/members`, data),
  remove: (storeId, userId) => api.delete(`/ecommerce/stores/${storeId}/members/${userId}`)
}

export const ecomOrderApi = {
  getAll: (params) => api.get('/ecommerce/orders', { params }),
  getById: (id) => api.get(`/ecommerce/orders/${id}`),
  update: (id, data) => api.put(`/ecommerce/orders/${id}`, data),
  delete: (id) => api.delete(`/ecommerce/orders/${id}`),
  syncToBoard: (boardId, orderIds) => api.post(`/ecommerce/orders/sync-to-board/${boardId}`, orderIds)
}

export const ecomSupplierApi = {
  getAll: () => api.get('/ecommerce/suppliers'),
  getById: (id) => api.get(`/ecommerce/suppliers/${id}`),
  create: (data) => api.post('/ecommerce/suppliers', data),
  update: (id, data) => api.put(`/ecommerce/suppliers/${id}`, data),
  delete: (id) => api.delete(`/ecommerce/suppliers/${id}`)
}

export const ecomImportApi = {
  importEtsy: (storeId, ordersFile, itemsFile, boardId) => {
    const formData = new FormData()
    if (ordersFile) formData.append('ordersFile', ordersFile)
    if (itemsFile) formData.append('itemsFile', itemsFile)
    if (boardId) formData.append('boardId', boardId)
    return api.post(`/ecommerce/import/etsy/${storeId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
