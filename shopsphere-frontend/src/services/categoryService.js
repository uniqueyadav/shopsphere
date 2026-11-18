import { authApi } from './authService'

export async function getActiveCategories() {
  const response = await authApi.get('/categories', { params: { page: 0, size: 100 } })
  return response.data.data.items
}

export async function getAdminCategories(params) {
  const response = await authApi.get('/admin/categories', { params })
  return response.data.data
}

export async function getCategoryStats() {
  const initialPage = await authApi.get('/admin/categories', { params: { page: 0, size: 100 } })
  const firstPage = initialPage.data.data || {}
  const pagesToFetch = Math.max(1, Number(firstPage.totalPages ?? 1))
  const allItems = [...(firstPage.items || [])]

  for (let page = 1; page < pagesToFetch; page += 1) {
    const response = await authApi.get('/admin/categories', { params: { page, size: 100 } })
    const pageData = response.data.data || {}
    allItems.push(...(pageData.items || []))
  }

  const total = allItems.length
  const active = allItems.filter((item) => Boolean(item.isActive)).length
  const inactive = total - active

  return { total, active, inactive }
}

export async function getCategoryParents() {
  const response = await authApi.get('/admin/categories/parents')
  return response.data.data
}

export async function uploadCategoryImage(file) {
  const formData = new FormData()
  formData.append('image', file)
  const response = await authApi.post('/admin/categories/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data.data
}

export async function deleteUnusedCategoryImage(imagePublicId) {
  const response = await authApi.delete('/admin/categories/images', { params: { imagePublicId } })
  return response.data.data
}

export async function createCategory(category) {
  const response = await authApi.post('/admin/categories', category)
  return response.data.data
}

export async function updateCategory(id, category) {
  const response = await authApi.put(`/admin/categories/${id}`, category)
  return response.data.data
}

export async function deleteCategory(id) {
  const response = await authApi.delete(`/admin/categories/${id}`)
  return response.data.data
}
