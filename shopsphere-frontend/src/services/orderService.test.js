import { afterEach, describe, expect, it, vi } from 'vitest'
import { authApi } from './authService'
import { cancelOrder, checkoutOrder, getMyOrders, getOrder, getOrderTracking } from './orderService'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('order API service', () => {
  it('submits checkout with the idempotency key and reads order data', async () => {
    const order = { id: 42, totalAmount: 125 }
    const cancellation = { order, refundPending: false }
    const post = vi.spyOn(authApi, 'post').mockResolvedValue({ data: { data: order } })
    const get = vi.spyOn(authApi, 'get')
      .mockResolvedValueOnce({ data: { data: order } })
      .mockResolvedValueOnce({ data: { data: [order] } })
      .mockResolvedValueOnce({ data: { data: { orderId: 42 } } })

    await expect(checkoutOrder({
      addressId: 7,
      paymentMethod: 'COD',
      idempotencyKey: 'checkout-attempt-1',
    })).resolves.toEqual(order)
    await expect(getOrder(42)).resolves.toEqual(order)
    await expect(getMyOrders()).resolves.toEqual([order])
    await expect(getOrderTracking(42)).resolves.toEqual({ orderId: 42 })
    post.mockResolvedValueOnce({ data: { data: cancellation } })
    await expect(cancelOrder(42)).resolves.toEqual(cancellation)

    expect(post).toHaveBeenCalledWith(
      '/orders/checkout',
      { addressId: 7, paymentMethod: 'COD' },
      { headers: { 'Idempotency-Key': 'checkout-attempt-1' } },
    )
    expect(get).toHaveBeenNthCalledWith(1, '/orders/42')
    expect(get).toHaveBeenNthCalledWith(2, '/orders/my-orders')
    expect(get).toHaveBeenNthCalledWith(3, '/orders/42/tracking')
    expect(post).toHaveBeenNthCalledWith(2, '/orders/42/cancel')
  })
})
