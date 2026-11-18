// Runtime validation coverage for category API calls.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { authApi } from './authService'
import {
  createCategory,
  deleteCategory,
  getActiveCategories,
  getAdminCategories,
  getCategoryStats,
  updateCategory,
} from './categoryService'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('category API service', () => {
  it('loads public active categories for marketplace navigation and filters', async () => {
    const categories = [{ id: 5, name: 'Home' }]
    const get = vi.spyOn(authApi, 'get').mockResolvedValue({ data: { data: { items: categories } } })

    await expect(getActiveCategories()).resolves.toEqual(categories)
    expect(get).toHaveBeenCalledWith('/categories', { params: { page: 0, size: 100 } })
  })

  it('requests a paginated, searchable admin category page', async () => {
    const page = { items: [], page: 1, size: 10, totalElements: 0, totalPages: 0 }
    const get = vi.spyOn(authApi, 'get').mockResolvedValue({ data: { data: page } })

    await expect(getAdminCategories({ page: 1, size: 10, search: 'home' })).resolves.toEqual(page)
    expect(get).toHaveBeenCalledWith('/admin/categories', {
      params: { page: 1, size: 10, search: 'home' },
    })
  })

  it('derives accurate status totals from all paginated categories', async () => {
    const firstPage = {
      items: [{ isActive: true }, { isActive: false }],
      totalElements: 3,
      totalPages: 2,
    }
    const secondPage = { items: [{ isActive: true }], totalElements: 3, totalPages: 2 }
    const get = vi.spyOn(authApi, 'get')
      .mockResolvedValueOnce({ data: { data: firstPage } })
      .mockResolvedValueOnce({ data: { data: secondPage } })

    await expect(getCategoryStats()).resolves.toEqual({ total: 3, active: 2, inactive: 1 })
    expect(get).toHaveBeenNthCalledWith(1, '/admin/categories', { params: { page: 0, size: 100 } })
    expect(get).toHaveBeenNthCalledWith(2, '/admin/categories', { params: { page: 1, size: 100 } })
  })

  it('sends create, update, and delete requests to the admin category endpoints', async () => {
    const created = { id: 11, name: 'Home', isActive: true }
    const updated = { id: 11, name: 'Home & Living', isActive: false }
    const removed = { id: 11, name: 'Home & Living', isActive: false }
    const post = vi.spyOn(authApi, 'post').mockResolvedValue({ data: { data: created } })
    const put = vi.spyOn(authApi, 'put').mockResolvedValue({ data: { data: updated } })
    const remove = vi.spyOn(authApi, 'delete').mockResolvedValue({ data: { data: removed } })
    const payload = { name: 'Home', slug: null, isActive: true }
    const updatePayload = { ...payload, name: 'Home & Living', isActive: false }

    await expect(createCategory(payload)).resolves.toEqual(created)
    await expect(updateCategory(11, updatePayload)).resolves.toEqual(updated)
    await expect(deleteCategory(11)).resolves.toEqual(removed)

    expect(post).toHaveBeenCalledWith('/admin/categories', payload)
    expect(put).toHaveBeenCalledWith('/admin/categories/11', updatePayload)
    expect(remove).toHaveBeenCalledWith('/admin/categories/11')
  })
})
