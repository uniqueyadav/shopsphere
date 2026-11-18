import { authApi } from './authService'

export async function getWishlist() {
  const response = await authApi.get('/wishlist')
  return response.data.data
}

export async function addWishlistItem(productId) {
  const response = await authApi.post(`/wishlist/items/${productId}`)
  return response.data.data
}

export async function removeWishlistItem(productId) {
  const response = await authApi.delete(`/wishlist/items/${productId}`)
  return response.data.data
}

export async function clearWishlist() {
  const response = await authApi.delete('/wishlist/clear')
  return response.data.data
}