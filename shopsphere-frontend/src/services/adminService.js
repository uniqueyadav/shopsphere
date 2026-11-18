import { authApi } from './authService'

export async function getCommissionRules() { return (await authApi.get('/admin/commission/rules')).data.data }
export async function getPlatformCommission() { return (await authApi.get('/admin/commission/platform')).data.data }
export async function updatePlatformCommission(payload) { return (await authApi.put('/admin/commission/platform', payload)).data.data }
export async function updateCategoryCommission(id, payload) { return (await authApi.put(`/admin/commission/categories/${id}`, payload)).data.data }
export async function previewCommission(params) { return (await authApi.get('/admin/commission/preview', { params })).data.data }

export async function getAdminDashboard(period = 'SIX_MONTHS') {
  const response = await authApi.get('/admin/analytics/dashboard', { params: { period } })
  return response.data.data
}

export async function getAdminRevenueReport(params = {}) {
  const response = await authApi.get('/admin/analytics/revenue', { params })
  return response.data.data
}
export async function exportAdminRevenueReport(params = {}) {
  return (await authApi.get('/admin/analytics/revenue/export', { params, responseType: 'blob' })).data
}

export async function getSellerApplications(params) {
  const response = await authApi.get('/admin/seller-applications', { params })
  return response.data.data
}

export async function getSellerApplication(id) {
  const response = await authApi.get(`/admin/seller-applications/${id}`)
  return response.data.data
}

export async function approveSellerApplication(id) {
  const response = await authApi.post(`/admin/seller-applications/${id}/approve`)
  return response.data.data
}

export async function rejectSellerApplication(id, rejectionReason) {
  const response = await authApi.post(`/admin/seller-applications/${id}/reject`, { rejectionReason })
  return response.data.data
}

export async function updateSellerProfile(id, payload) {
  const response = await authApi.put(`/admin/seller-applications/${id}`, payload)
  return response.data.data
}

export async function suspendSeller(id, reason) {
  const response = await authApi.post(`/admin/seller-applications/${id}/suspend`, { reason })
  return response.data.data
}

export async function activateSeller(id) {
  const response = await authApi.post(`/admin/seller-applications/${id}/activate`)
  return response.data.data
}

export async function exportAdminSellers(params = {}) {
  const response = await authApi.get('/admin/seller-applications/export', { params, responseType: 'blob' })
  return response.data
}

export async function getAdminUsers(params = {}) {
  const response = await authApi.get('/admin/users', { params })
  return response.data.data
}

export async function getAdminUserStats() {
  const response = await authApi.get('/admin/users/stats')
  return response.data.data
}

export async function getAdminUser(id) {
  const response = await authApi.get(`/admin/users/${id}`)
  return response.data.data
}

export async function updateAdminUser(id, payload) {
  const response = await authApi.patch(`/admin/users/${id}`, payload)
  return response.data.data
}

export async function updateAdminUserStatus(id, accountStatus) {
  const response = await authApi.patch(`/admin/users/${id}/status`, { accountStatus })
  return response.data.data
}

export async function exportAdminUsers(params = {}) {
  const response = await authApi.get('/admin/users/export', { params, responseType: 'blob' })
  return response.data
}
