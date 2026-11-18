import { afterEach, describe, expect, it, vi } from 'vitest'
import { authApi } from './authService'
import {
  addCartItem,
  clearCart,
  getCart,
  removeCartItem,
  updateCartItem,
} from './cartService'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('cart API service', () => {
  it('uses the authenticated cart endpoints and returns their response data', async () => {
    const cart = { id: 12, items: [], subtotal: 0, totalQuantity: 0 }
    const get = vi.spyOn(authApi, 'get').mockResolvedValue({ data: { data: cart } })
    const post = vi.spyOn(authApi, 'post').mockResolvedValue({ data: { data: cart } })
    const put = vi.spyOn(authApi, 'put').mockResolvedValue({ data: { data: cart } })
    const remove = vi.spyOn(authApi, 'delete').mockResolvedValue({ data: { data: cart } })

    await expect(getCart()).resolves.toEqual(cart)
    await expect(addCartItem({ productId: 7, quantity: 2 })).resolves.toEqual(cart)
    await expect(updateCartItem(19, 3)).resolves.toEqual(cart)
    await expect(removeCartItem(19)).resolves.toEqual(cart)
    await expect(clearCart()).resolves.toEqual(cart)

    expect(get).toHaveBeenCalledWith('/cart')
    expect(post).toHaveBeenCalledWith('/cart/items', { productId: 7, quantity: 2 })
    expect(put).toHaveBeenCalledWith('/cart/items/19', { quantity: 3 })
    expect(remove).toHaveBeenNthCalledWith(1, '/cart/items/19')
    expect(remove).toHaveBeenNthCalledWith(2, '/cart/clear')
  })
})
