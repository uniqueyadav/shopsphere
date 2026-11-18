import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi } from './authService'
import {
  deleteSellerProductImage,
  getPublicProduct,
  getPublicProductBySlug,
  getPublicProducts,
  getSellerProductImages,
  reorderSellerProductImages,
  setSellerProductPrimaryImage,
  uploadSellerProductImages,
} from './productService'

describe('product API service', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('queries public marketplace APIs with server-side filters', async () => {
    const listing = { items: [{ id: 8 }], totalElements: 1 }
    const get = vi.spyOn(authApi, 'get')
      .mockResolvedValueOnce({ data: { data: listing } })
      .mockResolvedValueOnce({ data: { data: listing.items[0] } })

    await expect(getPublicProducts({ categoryId: 3, discountedOnly: true })).resolves.toEqual(listing)
    await expect(getPublicProduct(8)).resolves.toEqual(listing.items[0])
    expect(get).toHaveBeenNthCalledWith(1, '/products', {
      params: { categoryId: 3, discountedOnly: true },
    })
    expect(get).toHaveBeenNthCalledWith(2, '/products/8')
  })

  it('supports abort signals for superseded public product requests', async () => {
    const signal = new AbortController().signal
    const get = vi.spyOn(authApi, 'get').mockResolvedValue({ data: { data: { items: [] } } })

    await getPublicProducts({ page: 0 }, signal)

    expect(get).toHaveBeenCalledWith('/products', { params: { page: 0 }, signal })
  })

  it('loads public product details by slug', async () => {
    const product = { id: 8, slug: 'green-tea-set' }
    const get = vi.spyOn(authApi, 'get').mockResolvedValue({ data: { data: product } })

    await expect(getPublicProductBySlug('green-tea-set')).resolves.toEqual(product)

    expect(get).toHaveBeenCalledWith('/products/slug/green-tea-set')
  })

  it('loads images through the authenticated seller API', async () => {
    const images = [{ id: 8, url: 'https://images.example.test/photo.png', isPrimary: true }]
    const get = vi.spyOn(authApi, 'get').mockResolvedValue({ data: { data: images } })

    await expect(getSellerProductImages(31)).resolves.toEqual(images)
    expect(get).toHaveBeenCalledWith('/seller/products/31/images')
  })

  it('uploads a multi-file form with progress reporting', async () => {
    const post = vi.spyOn(authApi, 'post').mockResolvedValue({ data: { data: [] } })
    const onUploadProgress = vi.fn()
    const files = [new Blob(['one'], { type: 'image/png' }), new Blob(['two'], { type: 'image/jpeg' })]

    await uploadSellerProductImages(31, files, onUploadProgress)

    const [url, formData, options] = post.mock.calls[0]
    expect(url).toBe('/seller/products/31/images')
    expect(formData.getAll('files')).toHaveLength(2)
    expect(options.headers['Content-Type']).toBe('multipart/form-data')
    expect(options.onUploadProgress).toBe(onUploadProgress)
  })

  it('uses the owner-scoped API for primary selection, reordering and deletion', async () => {
    const put = vi.spyOn(authApi, 'put').mockResolvedValue({ data: { data: [] } })
    const remove = vi.spyOn(authApi, 'delete').mockResolvedValue({ data: { data: [] } })

    await setSellerProductPrimaryImage(31, 4)
    await reorderSellerProductImages(31, [4, 5])
    await deleteSellerProductImage(31, 5)

    expect(put).toHaveBeenNthCalledWith(1, '/seller/products/31/images/4/primary')
    expect(put).toHaveBeenNthCalledWith(2, '/seller/products/31/images/reorder', { imageIds: [4, 5] })
    expect(remove).toHaveBeenCalledWith('/seller/products/31/images/5')
  })
})
