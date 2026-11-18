import { authApi } from './authService'

export async function getCustomerReturnRequests() {
  const response = await authApi.get('/returns')
  return response.data.data
}

export async function getEligibleReturnItems() {
  const response = await authApi.get('/returns/eligible-items')
  return response.data.data
}

export async function createCustomerReturnRequest(request) {
  const response = await authApi.post('/returns', request)
  return response.data.data
}

export async function cancelCustomerReturnRequest(requestId) {
  const response = await authApi.post(`/returns/${requestId}/cancel`)
  return response.data.data
}

export async function getAdminReturnRequests() {
  const response = await authApi.get('/admin/return-requests')
  return response.data.data
}

export async function updateAdminReturnRequest(requestId, update) {
  const response = await authApi.patch(`/admin/return-requests/${requestId}/status`, update)
  return response.data.data
}
