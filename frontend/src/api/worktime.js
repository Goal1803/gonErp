import { api } from 'src/boot/axios'

export const worktimeSettingsApi = {
  get: () => api.get('/worktime/settings'),
  update: (data) => api.put('/worktime/settings', data)
}

export const worktimeClockApi = {
  checkIn: (data) => api.post('/worktime/clock/check-in', data || {}),
  pause: () => api.post('/worktime/clock/pause'),
  resume: () => api.post('/worktime/clock/resume'),
  checkOut: (data) => api.post('/worktime/clock/check-out', data || {}),
  getStatus: () => api.get('/worktime/clock/status'),
  getToday: () => api.get('/worktime/clock/today')
}

// Phase 2: Day-Off Management APIs
export const worktimeDayOffTypeApi = {
  getAll: () => api.get('/worktime/day-off-types'),
  getActive: () => api.get('/worktime/day-off-types/active'),
  create: (data) => api.post('/worktime/day-off-types', data),
  update: (id, data) => api.put(`/worktime/day-off-types/${id}`, data),
  delete: (id) => api.delete(`/worktime/day-off-types/${id}`)
}

export const worktimeDayOffQuotaApi = {
  getMyQuotas: (year) => api.get('/worktime/quotas/my', { params: { year } }),
  getUserQuotas: (userId, year) => api.get(`/worktime/quotas/user/${userId}`, { params: { year } }),
  setQuota: (userId, typeId, data) => api.put(`/worktime/quotas/user/${userId}/type/${typeId}`, data),
  bulkAssign: (data) => api.post('/worktime/quotas/bulk-assign', data)
}

export const worktimeDayOffRequestApi = {
  getMyRequests: () => api.get('/worktime/day-off-requests/my'),
  create: (data) => api.post('/worktime/day-off-requests', data),
  cancel: (id) => api.delete(`/worktime/day-off-requests/${id}`),
  getPending: () => api.get('/worktime/day-off-requests/pending'),
  approve: (id, comment) => api.patch(`/worktime/day-off-requests/${id}/approve`, { comment }),
  deny: (id, comment) => api.patch(`/worktime/day-off-requests/${id}/deny`, { comment })
}

// Phase 3: Team Calendar & Public Holidays
export const worktimeHolidayApi = {
  getAll: () => api.get('/worktime/holidays'),
  getForRange: (startDate, endDate) => api.get('/worktime/holidays/range', { params: { startDate, endDate } }),
  create: (data) => api.post('/worktime/holidays', data),
  update: (id, data) => api.put(`/worktime/holidays/${id}`, data),
  delete: (id) => api.delete(`/worktime/holidays/${id}`)
}

export const worktimeCalendarApi = {
  getData: (startDate, endDate) => api.get('/worktime/calendar', { params: { startDate, endDate } })
}

export const worktimeReportApi = {
  getDailyReport: (date) => api.get('/worktime/reports/daily', { params: { date } }),
  getWeeklyReport: (weekStart) => api.get('/worktime/reports/weekly', { params: { weekStart } }),
  getMonthlyReport: (year, month) => api.get('/worktime/reports/monthly', { params: { year, month } }),
  getTeamDailyReport: (date) => api.get('/worktime/reports/team/daily', { params: { date } }),
  getTeamMonthlyReport: (year, month) => api.get('/worktime/reports/team/monthly', { params: { year, month } }),
  getMemberDailyReport: (userId, date) => api.get(`/worktime/reports/member/${userId}/daily`, { params: { date } }),
  editMemberDailyEntry: (userId, date, data) => api.put(`/worktime/reports/member/${userId}/daily`, data, { params: { date } }),
  resetMemberDailyEntry: (userId, date) => api.delete(`/worktime/reports/member/${userId}/daily`, { params: { date } }),
  getMemberWeeklyReport: (userId, weekStart) => api.get(`/worktime/reports/member/${userId}/weekly`, { params: { weekStart } }),
  getMemberMonthlyReport: (userId, year, month) => api.get(`/worktime/reports/member/${userId}/monthly`, { params: { year, month } }),
  getTeamCheckoutNotes: (date) => api.get('/worktime/reports/team/checkout-notes', { params: { date } }),
  getUserCheckoutNotesHistory: (userId, startDate, endDate) => api.get(`/worktime/reports/member/${userId}/checkout-notes`, { params: { startDate, endDate } }),
  exportCSV: (year, month) => api.get('/worktime/reports/export', { params: { year, month, type: 'csv' }, responseType: 'blob' })
}

export const worktimeTeamApi = {
  getAvailability: () => api.get('/worktime/team/availability')
}

export const worktimeUserConfigApi = {
  getMyConfig: () => api.get('/worktime/user-config/me'),
  getOrgConfigs: () => api.get('/worktime/user-config'),
  getUserConfig: (userId) => api.get(`/worktime/user-config/${userId}`),
  updateUserConfig: (userId, data) => api.put(`/worktime/user-config/${userId}`, data)
}
