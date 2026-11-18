import { afterEach, describe, expect, it, vi } from 'vitest'
import { authApi } from './authService'
import { createPaymentOrder, failPayment, verifyPayment } from './paymentService'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('payment API service', () => {
  it('creates a server-priced payment order with an idempotency key', async () => {
    const paymentOrder = { razorpayOrderId: 'order_test_1', amountPaise: 12500 }
    const post = vi.spyOn(authApi, 'post').mockResolvedValue({ data: { data: paymentOrder } })

    await expect(createPaymentOrder({
      orderId: 42,
      idempotencyKey: 'payment-attempt-1',
    })).resolves.toEqual(paymentOrder)

    expect(post).toHaveBeenCalledWith(
      '/payments/create-order',
      { orderId: 42 },
      { headers: { 'Idempotency-Key': 'payment-attempt-1' } },
    )
  })

  it('sends provider callback values only to verification and reports cancelled attempts', async () => {
    const post = vi.spyOn(authApi, 'post')
      .mockResolvedValueOnce({ data: { data: { paymentStatus: 'SUCCESS' } } })
      .mockResolvedValueOnce({ data: { data: { status: 'FAILED' } } })

    await expect(verifyPayment({
      razorpayOrderId: 'order_test_1',
      razorpayPaymentId: 'pay_test_1',
      razorpaySignature: 'signed',
    })).resolves.toEqual({ paymentStatus: 'SUCCESS' })
    await expect(failPayment({
      razorpayOrderId: 'order_test_1',
      reason: 'CANCELLED',
    })).resolves.toEqual({ status: 'FAILED' })

    expect(post).toHaveBeenNthCalledWith(1, '/payments/verify', {
      razorpayOrderId: 'order_test_1',
      razorpayPaymentId: 'pay_test_1',
      razorpaySignature: 'signed',
    })
    expect(post).toHaveBeenNthCalledWith(2, '/payments/fail', {
      razorpayOrderId: 'order_test_1',
      reason: 'CANCELLED',
    })
  })
})
