import { authApi } from './authService'

export async function getSellerInventory(params) {
  const response = await authApi.get('/seller/inventory', { params })
  return response.data.data
}

export async function getSellerInventorySummary() {
  const response = await authApi.get('/seller/inventory/summary')
  return response.data.data
}

export async function updateSellerStock(productId, stockQuantity) {
  const response = await authApi.put(`/seller/inventory/${productId}/stock`, { stockQuantity })
  return response.data.data
}

export async function getSellerStockHistory(productId) {
  const response = await authApi.get(`/seller/inventory/${productId}/history`)
  return response.data.data
}
