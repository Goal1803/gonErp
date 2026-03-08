import { api } from 'src/boot/axios'

export const orgApi = {
  getAll: () => api.get('/organizations'),
  getById: (id) => api.get(`/organizations/${id}`),
  create: (data) => api.post('/organizations', data),
  update: (id, data) => api.put(`/organizations/${id}`, data),
  toggleActive: (id) => api.patch(`/organizations/${id}/toggle-active`)
}

export const orgTypeApi = {
  getAll: () => api.get('/organization-types'),
  getById: (id) => api.get(`/organization-types/${id}`),
  create: (data) => api.post('/organization-types', data),
  update: (id, data) => api.put(`/organization-types/${id}`, data),
  delete: (id) => api.delete(`/organization-types/${id}`),
  getStaffRoles: (typeId) => api.get(`/organization-types/${typeId}/staff-roles`),
  createStaffRole: (typeId, data) => api.post(`/organization-types/${typeId}/staff-roles`, data),
  deleteStaffRole: (id) => api.delete(`/organization-types/staff-roles/${id}`),
  getDepartments: (typeId) => api.get(`/organization-types/${typeId}/departments`),
  createDepartment: (typeId, data) => api.post(`/organization-types/${typeId}/departments`, data),
  deleteDepartment: (id) => api.delete(`/organization-types/departments/${id}`),
  getUserGroups: (typeId) => api.get(`/organization-types/${typeId}/user-groups`),
  createUserGroup: (typeId, data) => api.post(`/organization-types/${typeId}/user-groups`, data),
  deleteUserGroup: (id) => api.delete(`/organization-types/user-groups/${id}`)
}

export const orgStructureApi = {
  getStaffRoles: () => api.get('/org-structure/staff-roles'),
  createStaffRole: (data) => api.post('/org-structure/staff-roles', data),
  updateStaffRole: (id, data) => api.put(`/org-structure/staff-roles/${id}`, data),
  deleteStaffRole: (id) => api.delete(`/org-structure/staff-roles/${id}`),
  getDepartments: () => api.get('/org-structure/departments'),
  createDepartment: (data) => api.post('/org-structure/departments', data),
  updateDepartment: (id, data) => api.put(`/org-structure/departments/${id}`, data),
  deleteDepartment: (id) => api.delete(`/org-structure/departments/${id}`),
  getUserGroups: () => api.get('/org-structure/user-groups'),
  getGroupMembers: (groupId) => api.get(`/org-structure/user-groups/${groupId}/members`),
  changeGroupRole: (groupId, userId, role) => api.patch(`/org-structure/user-groups/${groupId}/members/${userId}/role`, null, { params: { role } }),
  transferOwnership: (groupId, userId) => api.patch(`/org-structure/user-groups/${groupId}/transfer-ownership/${userId}`),
  createUserGroup: (data) => api.post('/org-structure/user-groups', data),
  updateUserGroup: (id, data) => api.put(`/org-structure/user-groups/${id}`, data),
  deleteUserGroup: (id) => api.delete(`/org-structure/user-groups/${id}`),
  assignStaffRole: (userId, roleId) => api.post(`/org-structure/users/${userId}/staff-roles/${roleId}`),
  removeStaffRole: (userId, roleId) => api.delete(`/org-structure/users/${userId}/staff-roles/${roleId}`),
  assignDepartment: (userId, deptId) => api.post(`/org-structure/users/${userId}/departments/${deptId}`),
  removeDepartment: (userId, deptId) => api.delete(`/org-structure/users/${userId}/departments/${deptId}`),
  assignUserGroup: (userId, groupId) => api.post(`/org-structure/users/${userId}/user-groups/${groupId}`),
  removeUserGroup: (userId, groupId) => api.delete(`/org-structure/users/${userId}/user-groups/${groupId}`)
}
