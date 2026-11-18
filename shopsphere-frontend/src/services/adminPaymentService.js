import { authApi } from './authService'

export async function getAdminPayments(params = {}) {
  const response = await authApi.get('/admin/payments', { params })
  return response.data.data
}

export async function getAdminPaymentStats(params = {}) {
  const response = await authApi.get('/admin/payments/stats', { params })
  return response.data.data
}

export async function getAdminPayment(paymentId) {
  const response = await authApi.get(`/admin/payments/${paymentId}`)
  return response.data.data
}

export async function exportAdminPayments(params = {}) {
  const response = await authApi.get('/admin/payments/export', { params, responseType: 'blob' })
  return response.data
}
