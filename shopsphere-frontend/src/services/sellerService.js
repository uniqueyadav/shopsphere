import { authApi } from './authService'

export async function applyForSeller(application) {
  const response = await authApi.post('/seller/applications', application)
  return response.data.data
}

export async function getSellerDeliveryFee() {
  const response = await authApi.get('/seller/me/delivery-fee')
  return response.data.data
}

export async function updateSellerDeliveryFee(deliveryFee) {
  const response = await authApi.put('/seller/me/delivery-fee', { deliveryFee })
  return response.data.data
}

export async function getSellerDashboard(period = 'THIRTY_DAYS') {
  const response = await authApi.get('/seller/analytics/dashboard', { params: { period } })
  return response.data.data
}

export async function getSellerEarnings(params = {}) {
  const response = await authApi.get('/seller/analytics/earnings', { params })
  return response.data.data
}

export async function getSellerPayouts(params = {}) {
  const response = await authApi.get('/seller/payouts', { params })
  return response.data.data
}

export async function requestSellerPayout(payout, idempotencyKey) {
  const response = await authApi.post('/seller/payouts', payout, { headers: { 'Idempotency-Key': idempotencyKey } })
  return response.data.data
}
