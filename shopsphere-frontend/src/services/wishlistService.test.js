import { afterEach, describe, expect, it, vi } from 'vitest'
import { authApi } from './authService'
import {
  addWishlistItem,
  clearWishlist,
  getWishlist,
  removeWishlistItem,
} from './wishlistService'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('wishlist API service', () => {
  it('uses the authenticated wishlist endpoints and returns their response data', async () => {
    const wishlist = { id: 8, items: [], totalItems: 0 }
    const get = vi.spyOn(authApi, 'get').mockResolvedValue({ data: { data: wishlist } })
    const post = vi.spyOn(authApi, 'post').mockResolvedValue({ data: { data: wishlist } })
    const remove = vi.spyOn(authApi, 'delete').mockResolvedValue({ data: { data: wishlist } })

    await expect(getWishlist()).resolves.toEqual(wishlist)
    await expect(addWishlistItem(12)).resolves.toEqual(wishlist)
    await expect(removeWishlistItem(12)).resolves.toEqual(wishlist)
    await expect(clearWishlist()).resolves.toEqual(wishlist)

    expect(get).toHaveBeenCalledWith('/wishlist')
    expect(post).toHaveBeenCalledWith('/wishlist/items/12')
    expect(remove).toHaveBeenNthCalledWith(1, '/wishlist/items/12')
    expect(remove).toHaveBeenNthCalledWith(2, '/wishlist/clear')
  })
})
