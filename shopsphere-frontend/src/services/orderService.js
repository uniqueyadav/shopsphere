import { authApi } from './authService'

export async function checkoutOrder({ addressId, paymentMethod, couponCode, idempotencyKey }) {
  const payload = { addressId, paymentMethod }
  const normalizedCoupon = typeof couponCode === 'string' ? couponCode.trim() : couponCode
  if (normalizedCoupon) payload.couponCode = normalizedCoupon

  const config = idempotencyKey ? { headers: { 'Idempotency-Key': idempotencyKey } } : undefined
  const response = await authApi.post('/orders/checkout', payload, config)
  return response.data.data
}

export async function getOrder(orderId) {
  const response = await authApi.get(`/orders/${orderId}`)
  return response.data.data
}

export async function getMyOrders() {
  const response = await authApi.get('/orders/my-orders')
  return response.data.data
}

export async function getOrderTracking(orderId) {
  const response = await authApi.get(`/orders/${orderId}/tracking`)
  return response.data.data
}

export async function cancelOrder(orderId) {
  const response = await authApi.post(`/orders/${orderId}/cancel`)
  return response.data.data
}

export async function getAdminOrders(params = {}) {
  const response = await authApi.get('/admin/orders', { params })
  return response.data.data
}

export async function getSellerOrders(params = {}) {
  const response = await authApi.get('/seller/orders', { params })
  return response.data.data
}

export async function getSellerOrderSummary() {
  const response = await authApi.get('/seller/orders/summary')
  return response.data.data
}

export async function getSellerOrderDetails(sellerOrderId) {
  const response = await authApi.get(`/seller/orders/${sellerOrderId}`)
  return response.data.data
}

export async function updateSellerOrderStatus(sellerOrderId, status) {
  const response = await authApi.patch(`/seller/orders/${sellerOrderId}/status`, { status })
  return response.data.data
}

export async function getAdminOrderStats() {
  const response = await authApi.get('/admin/orders/stats')
  return response.data.data
}

export async function getAdminOrder(orderId) {
  const response = await authApi.get(`/admin/orders/${orderId}`)
  return response.data.data
}

export async function cancelAdminOrder(orderId, reason) {
  const response = await authApi.post(`/admin/orders/${orderId}/cancel`, { reason })
  return response.data
}

export async function exportAdminOrders(params = {}) {
  const response = await authApi.get('/admin/orders/export', { params, responseType: 'blob' })
  return response.data
}
