import { authApi } from './authService'

export async function getCart() {
  const response = await authApi.get('/cart')
  return response.data.data
}

export async function addCartItem(item) {
  const response = await authApi.post('/cart/items', item)
  return response.data.data
}

export async function updateCartItem(itemId, quantity) {
  const response = await authApi.put(`/cart/items/${itemId}`, { quantity })
  return response.data.data
}

export async function removeCartItem(itemId) {
  const response = await authApi.delete(`/cart/items/${itemId}`)
  return response.data.data
}

export async function clearCart() {
  const response = await authApi.delete('/cart/clear')
  return response.data.data
}

export async function previewCartCoupon(code) {
  const response = await authApi.post('/coupons/preview', { code })
  return response.data.data
}
