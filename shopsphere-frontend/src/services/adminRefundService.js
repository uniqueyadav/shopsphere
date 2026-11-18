import { authApi } from './authService'

export async function getAdminRefunds(params = {}) {
  const response = await authApi.get('/admin/refunds', { params })
  return response.data.data
}

export async function getAdminRefundStats(params = {}) {
  const response = await authApi.get('/admin/refunds/stats', { params })
  return response.data.data
}

export async function getAdminRefund(paymentTransactionId) {
  const response = await authApi.get(`/admin/refunds/${paymentTransactionId}`)
  return response.data.data
}

export async function exportAdminRefunds(params = {}) {
  const response = await authApi.get('/admin/refunds/export', { params, responseType: 'blob' })
  return response.data
}
