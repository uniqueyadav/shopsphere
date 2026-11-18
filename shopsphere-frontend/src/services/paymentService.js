import { authApi } from './authService'

export async function createPaymentOrder({ orderId, idempotencyKey }) {
  const response = await authApi.post(
    '/payments/create-order',
    { orderId },
    { headers: { 'Idempotency-Key': idempotencyKey } },
  )
  return response.data.data
}

export async function verifyPayment({ razorpayOrderId, razorpayPaymentId, razorpaySignature }) {
  const response = await authApi.post('/payments/verify', {
    razorpayOrderId,
    razorpayPaymentId,
    razorpaySignature,
  })
  return response.data.data
}

export async function failPayment({ razorpayOrderId, reason }) {
  const response = await authApi.post('/payments/fail', { razorpayOrderId, reason })
  return response.data.data
}