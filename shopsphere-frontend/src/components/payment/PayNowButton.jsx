import { useRef, useState } from 'react'
import { getRequestError } from '../../services/authService'
import { createPaymentOrder, failPayment, verifyPayment } from '../../services/paymentService'

const checkoutScriptUrl = 'https://checkout.razorpay.com/v1/checkout.js'

function createAttemptKey() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function loadRazorpayCheckout() {
  if (window.Razorpay) return Promise.resolve()

  return new Promise((resolve, reject) => {
    let script = document.querySelector(`script[src="${checkoutScriptUrl}"]`)
    if (!script) {
      script = document.createElement('script')
      script.src = checkoutScriptUrl
      script.async = true
    }

    script.addEventListener('load', () => {
      if (window.Razorpay) resolve()
      else reject(new Error('Razorpay checkout did not initialize.'))
    }, { once: true })
    script.addEventListener('error', () => reject(new Error('Razorpay checkout could not be loaded.')), { once: true })
    if (!script.isConnected) document.body.appendChild(script)
  })
}

export default function PayNowButton({ orderId, onPaymentVerified, className = 'checkout-place-order' }) {
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState(null)
  const attemptKey = useRef(null)

  async function beginPayment() {
    if (processing) return
    setProcessing(true)
    setError(null)
    let paymentOrder = null
    let settled = false

    const reportFailure = async (reason) => {
      if (settled) return
      settled = true
      try {
        if (paymentOrder) await failPayment({ razorpayOrderId: paymentOrder.razorpayOrderId, reason })
        setError(reason === 'CANCELLED'
          ? 'Payment was cancelled. You can try again when you’re ready.'
          : 'Payment did not complete. Please try again.')
      } catch (requestError) {
        setError(getRequestError(requestError, 'Payment could not be updated. Please refresh the order and try again.'))
      } finally {
        attemptKey.current = null
        setProcessing(false)
      }
    }

    try {
      await loadRazorpayCheckout()
      if (!attemptKey.current) attemptKey.current = createAttemptKey()
      paymentOrder = await createPaymentOrder({ orderId, idempotencyKey: attemptKey.current })
      const checkout = new window.Razorpay({
        key: paymentOrder.keyId,
        amount: paymentOrder.amountPaise,
        currency: paymentOrder.currency,
        name: 'ShopSphere',
        description: `Order #${orderId}`,
        order_id: paymentOrder.razorpayOrderId,
        handler: async (result) => {
          if (settled) return
          try {
            const verifiedOrder = await verifyPayment({
              razorpayOrderId: result.razorpay_order_id,
              razorpayPaymentId: result.razorpay_payment_id,
              razorpaySignature: result.razorpay_signature,
            })
            settled = true
            attemptKey.current = null
            setProcessing(false)
            onPaymentVerified(verifiedOrder)
          } catch (requestError) {
            settled = true
            attemptKey.current = null
            setProcessing(false)
            setError(getRequestError(
              requestError,
              'Payment could not be verified yet. Refresh your order status before trying again.',
            ))
          }
        },
        modal: {
          ondismiss: () => { void reportFailure('CANCELLED') },
        },
        theme: { color: '#315b43' },
      })
      checkout.on('payment.failed', () => { void reportFailure('FAILED') })
      checkout.open()
    } catch (requestError) {
      if (paymentOrder && !settled) {
        await reportFailure('FAILED')
      } else {
        setError(getRequestError(requestError, 'Secure checkout could not be started. Please try again.'))
        setProcessing(false)
      }
    }
  }

  return (
    <div className="payment-action">
      <button
        aria-busy={processing}
        className={className}
        disabled={processing}
        onClick={beginPayment}
        type="button"
      >
        {processing ? 'Opening secure checkout…' : 'Pay now'}
        <span aria-hidden="true">→</span>
      </button>
      {error && <p className="checkout-error" role="alert">{error}</p>}
    </div>
  )
}
