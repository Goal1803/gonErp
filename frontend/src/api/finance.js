import { api } from 'src/boot/axios'

export const financeConfigApi = {
  getRoles: () => api.get('/finance/config/roles'),
  assignRole: (data) => api.post('/finance/config/roles', data),
  removeRole: (userId) => api.delete(`/finance/config/roles/${userId}`)
}

export const financeAccountApi = {
  getAll: () => api.get('/finance/accounts'),
  getById: (id) => api.get(`/finance/accounts/${id}`),
  create: (data) => api.post('/finance/accounts', data),
  update: (id, data) => api.put(`/finance/accounts/${id}`, data),
  delete: (id) => api.delete(`/finance/accounts/${id}`)
}

export const financeReportApi = {
  getAll: () => api.get('/finance/reports'),
  getById: (id) => api.get(`/finance/reports/${id}`),
  create: (data) => api.post('/finance/reports', data),
  update: (id, data) => api.put(`/finance/reports/${id}`, data),
  delete: (id) => api.delete(`/finance/reports/${id}`)
}

export const financeTransactionApi = {
  getByReport: (reportId) => api.get(`/finance/transactions/report/${reportId}`),
  getByReportAndAccount: (reportId, accountId) => api.get(`/finance/transactions/report/${reportId}/account/${accountId}`),
  update: (id, data) => api.put(`/finance/transactions/${id}`, data),
  toggleCompleted: (id) => api.patch(`/finance/transactions/${id}/toggle-completed`),
  resetOne: (id) => api.post(`/finance/transactions/${id}/reset`),
  resetAll: (reportId) => api.post(`/finance/transactions/reset-all/report/${reportId}`),
  reExtract: (reportId) => api.post(`/finance/transactions/re-extract/report/${reportId}`),
  getFilesByReport: (reportId) => api.get(`/finance/transactions/files/report/${reportId}`),
  uploadFile: (reportId, accountId, fileType, subfolder, file) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('reportId', reportId)
    formData.append('accountId', accountId)
    formData.append('fileType', fileType)
    if (subfolder) formData.append('subfolder', subfolder)
    return api.post('/finance/transactions/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  deleteFile: (fileId) => api.delete(`/finance/transactions/files/${fileId}`),
  moveFile: (fileId, subfolder) => api.patch(`/finance/transactions/files/${fileId}/move`, null, { params: { subfolder } }),
  renameFolder: (reportId, accountId, oldName, newName) => api.patch('/finance/transactions/files/rename-folder', null, { params: { reportId, accountId, oldName, newName } })
}

export const financeInvoiceApi = {
  getByReport: (reportId) => api.get(`/finance/invoices/report/${reportId}`),
  getByTransaction: (transactionId) => api.get(`/finance/invoices/transaction/${transactionId}`),
  upload: (reportId, transactionId, invoiceType, description, file) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('reportId', reportId)
    if (transactionId) formData.append('transactionId', transactionId)
    if (invoiceType) formData.append('invoiceType', invoiceType)
    if (description) formData.append('description', description)
    return api.post('/finance/invoices/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  delete: (id) => api.delete(`/finance/invoices/${id}`)
}

export const financeRuleApi = {
  getAll: () => api.get('/finance/rules'),
  create: (data) => api.post('/finance/rules', data),
  update: (id, data) => api.put(`/finance/rules/${id}`, data),
  delete: (id) => api.delete(`/finance/rules/${id}`),
  categorize: (reportId) => api.post(`/finance/rules/categorize/${reportId}`),
  seedDefaults: () => api.post('/finance/rules/seed-defaults')
}

export const financeShareLinkApi = {
  getAll: () => api.get('/finance/share-links'),
  getByReport: (reportId) => api.get(`/finance/share-links/report/${reportId}`),
  create: (data) => api.post('/finance/share-links', data),
  toggleActive: (id) => api.patch(`/finance/share-links/${id}/toggle`),
  delete: (id) => api.delete(`/finance/share-links/${id}`)
}

export const financeExportApi = {
  exportExcel: (reportId, accountId) => {
    const params = accountId ? `?accountId=${accountId}` : ''
    return api.get(`/finance/export/excel/report/${reportId}${params}`, { responseType: 'blob' })
  },
  exportZip: (reportId) => api.get(`/finance/export/zip/report/${reportId}`, { responseType: 'blob' }),
  generateZip: (reportId) => api.post(`/finance/export/generate/${reportId}`),
  getHistory: (reportId) => api.get(`/finance/export/history/${reportId}`),
  deleteExport: (id) => api.delete(`/finance/export/${id}`)
}

export const financeAmazonApi = {
  getByReport: (reportId) => api.get(`/finance/amazon-reconciliation/report/${reportId}`),
  generate: (reportId, marketplace) => {
    const params = marketplace ? `?marketplace=${marketplace}` : ''
    return api.post(`/finance/amazon-reconciliation/generate/${reportId}${params}`)
  }
}

export const financeCurrencyApi = {
  getAll: () => api.get('/finance/currencies'),
  create: (data) => api.post('/finance/currencies', data),
  update: (id, data) => api.put(`/finance/currencies/${id}`, data),
  delete: (id) => api.delete(`/finance/currencies/${id}`),
  getRates: () => api.get('/finance/currencies/rates'),
  createRate: (data) => api.post('/finance/currencies/rates', data),
  deleteRate: (id) => api.delete(`/finance/currencies/rates/${id}`)
}

// Public Steuerberater API (no auth)
export const steuerberaterApi = {
  getReport: (token) => api.get(`/finance/public/${token}/report`),
  getAccounts: (token) => api.get(`/finance/public/${token}/accounts`),
  getTransactions: (token, accountId) => {
    const params = accountId ? `?accountId=${accountId}` : ''
    return api.get(`/finance/public/${token}/transactions${params}`)
  },
  getInvoices: (token) => api.get(`/finance/public/${token}/invoices`),
  getComments: (token) => api.get(`/finance/public/${token}/comments`),
  addComment: (token, data) => api.post(`/finance/public/${token}/comments`, data),
  exportCsv: (token, accountId) => {
    const params = accountId ? `?accountId=${accountId}` : ''
    return api.get(`/finance/public/${token}/export/csv${params}`, { responseType: 'blob' })
  }
}
