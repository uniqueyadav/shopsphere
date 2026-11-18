import { describe, expect, it, vi } from 'vitest'
import { canCancelOrder, getOrderStatusLabel, reorderOrder } from './orderUtils'

describe('customer order helpers', () => {
  it('allows cancellation only while every seller shipment is awaiting processing', () => {
    expect(canCancelOrder({
      sellerOrders: [{ orderStatus: 'PLACED' }, { orderStatus: 'CONFIRMED' }],
    })).toBe(true)
    expect(canCancelOrder({
      sellerOrders: [{ orderStatus: 'PLACED' }, { orderStatus: 'PROCESSING' }],
    })).toBe(false)
    expect(canCancelOrder({ sellerOrders: [] })).toBe(false)
  })

  it('reorders real item IDs and reports items no longer available', async () => {
    const addItem = vi.fn()
      .mockResolvedValueOnce({})
      .mockRejectedValueOnce('This product is no longer available.')
    const order = {
      sellerOrders: [
        { items: [
          { productId: 7, productName: 'First item', quantity: 2 },
          { productId: 8, productName: 'Second item', quantity: 1 },
        ] },
      ],
    }

    await expect(reorderOrder(order, addItem)).resolves.toEqual({
      addedItems: 1,
      failedItems: ['Second item'],
    })
    expect(addItem).toHaveBeenNthCalledWith(1, { productId: 7, quantity: 2 })
    expect(addItem).toHaveBeenNthCalledWith(2, { productId: 8, quantity: 1 })
  })

  it('formats order status labels for display', () => {
    expect(getOrderStatusLabel('OUT_FOR_DELIVERY')).toBe('Out For Delivery')
  })
})
