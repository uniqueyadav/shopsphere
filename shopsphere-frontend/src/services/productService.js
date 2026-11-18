import { authApi } from './authService'

export async function getPublicProducts(params, signal) {
  const config = { params }
  if (signal) config.signal = signal
  const response = await authApi.get('/products', config)
  return response.data.data
}

export async function getProductFacets(signal) {
  const response = signal
    ? await authApi.get('/products/facets', { signal })
    : await authApi.get('/products/facets')
  return response.data.data
}

export async function getPublicProduct(productId, signal) {
  const response = signal
    ? await authApi.get(`/products/${productId}`, { signal })
    : await authApi.get(`/products/${productId}`)
  return response.data.data
}

export async function getProductReviewSummary(productId, signal) {
  const url = `/products/${productId}/review-summary`
  const response = signal
    ? await authApi.get(url, { signal })
    : await authApi.get(url)
  return response.data.data
}

export async function getPublicProductBySlug(slug, signal) {
  const url = `/products/slug/${encodeURIComponent(slug)}`
  const response = signal
    ? await authApi.get(url, { signal })
    : await authApi.get(url)
  return response.data.data
}

export async function getSellerProducts(params) {
  const response = await authApi.get('/seller/products', { params })
  return response.data.data
}

export async function getSellerProduct(productId) {
  const response = await authApi.get(`/seller/products/${productId}`)
  return response.data.data
}

export async function createSellerProduct(product) {
  const response = await authApi.post('/seller/products', product)
  return response.data.data
}

export async function updateSellerProduct(productId, product) {
  const response = await authApi.put(`/seller/products/${productId}`, product)
  return response.data.data
}

export async function deleteSellerProduct(productId) {
  const response = await authApi.delete(`/seller/products/${productId}`)
  return response.data.data
}

export async function getSellerProductImages(productId) {
  const response = await authApi.get(`/seller/products/${productId}/images`)
  return response.data.data
}

export async function uploadSellerProductImages(productId, files, onUploadProgress) {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  const response = await authApi.post(
    `/seller/products/${productId}/images`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress,
    },
  )
  return response.data.data
}

export async function setSellerProductPrimaryImage(productId, imageId) {
  const response = await authApi.put(`/seller/products/${productId}/images/${imageId}/primary`)
  return response.data.data
}

export async function reorderSellerProductImages(productId, imageIds) {
  const response = await authApi.put(`/seller/products/${productId}/images/reorder`, { imageIds })
  return response.data.data
}

export async function deleteSellerProductImage(productId, imageId) {
  const response = await authApi.delete(`/seller/products/${productId}/images/${imageId}`)
  return response.data.data
}

export async function submitSellerProductForApproval(productId) {
  const response = await authApi.post(`/seller/products/${productId}/submit-for-approval`)
  return response.data.data
}

export async function getAdminProducts(params) {
  const response = await authApi.get('/admin/products', { params })
  return response.data.data
}

export async function getAdminProductStats() {
  const response = await authApi.get('/admin/products/stats')
  return response.data.data
}

export async function getAdminProductSellers() {
  const response = await authApi.get('/admin/products/sellers')
  return response.data.data
}

export async function getAdminProduct(productId) {
  const response = await authApi.get(`/admin/products/${productId}`)
  return response.data.data
}

export async function createAdminProduct(sellerId, product) {
  const response = await authApi.post('/admin/products', { sellerId, product })
  return response.data.data
}

export async function updateAdminProduct(productId, product) {
  const response = await authApi.put(`/admin/products/${productId}`, product)
  return response.data.data
}

export async function setAdminProductActive(productId, active) {
  const response = await authApi.post(`/admin/products/${productId}/active`, null, { params: { value: active } })
  return response.data.data
}

export async function approveAdminProduct(productId) {
  const response = await authApi.post(`/admin/products/${productId}/approve`)
  return response.data.data
}

export async function rejectAdminProduct(productId, rejectionReason) {
  const response = await authApi.post(`/admin/products/${productId}/reject`, { rejectionReason })
  return response.data.data
}

export async function exportAdminProducts(params) {
  const response = await authApi.get('/admin/products/export', { params, responseType: 'blob' })
  return response.data
}

export async function uploadAdminProductImages(productId, files) {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  const response = await authApi.post(`/admin/products/${productId}/images`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data.data
}

export async function setAdminProductPrimaryImage(productId, imageId) {
  const response = await authApi.put(`/admin/products/${productId}/images/${imageId}/primary`)
  return response.data.data
}

export async function deleteAdminProductImage(productId, imageId) {
  const response = await authApi.delete(`/admin/products/${productId}/images/${imageId}`)
  return response.data.data
}
