import { useEffect, useState } from 'react'
import { LazyMotion, MotionConfig, domAnimation, m } from 'framer-motion'
import { Link, useLocation, useParams } from 'react-router-dom'
import MainNavbar from '../../components/navbar/MainNavbar'
import PayNowButton from '../../components/payment/PayNowButton'
import { formatProductPrice } from '../../components/product/ProductCard'
import { getRequestError } from '../../services/authService'
import { getOrder } from '../../services/orderService'
import './Checkout.css'

export default function OrderConfirmation() {
  const { orderId } = useParams()
  const location = useLocation()
  const [order, setOrder] = useState(location.state?.order || null)
  const [status, setStatus] = useState(order ? 'succeeded' : 'loading')
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    getOrder(orderId)
      .then((result) => {
        if (active) {
          setOrder(result)
          setStatus('succeeded')
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(getRequestError(requestError, 'This order could not be loaded.'))
          setStatus('failed')
        }
      })
    return () => {
      active = false
    }
  }, [orderId])

  const paymentPending = order?.paymentMethod === 'ONLINE' && order.paymentStatus === 'PENDING'
  const paymentRefunded = order?.paymentMethod === 'ONLINE' && order.paymentStatus === 'REFUNDED'

  return (
    <LazyMotion features={domAnimation}>
      <MotionConfig reducedMotion="user">
        <m.div
          animate={{ opacity: 1, y: 0 }}
          className="checkout-page"
          initial={{ opacity: 0, y: 8 }}
          transition={{ duration: .3 }}
        >
          <MainNavbar />
          <main className="order-confirmation-container">
            {status === 'loading' ? (
              <section aria-label="Loading order" className="order-confirmation-loading" role="status">
                <span className="checkout-spinner" />
                <p>Loading your order details…</p>
              </section>
            ) : status === 'failed' ? (
              <section className="checkout-empty">
                <h1>We couldn’t load this order.</h1>
                <p role="alert">{error}</p>
                <Link className="checkout-primary-link" to="/shop">Continue shopping</Link>
              </section>
            ) : order ? (
              <m.section
                animate={{ opacity: 1, y: 0 }}
                className="order-confirmation-card"
                initial={{ opacity: 0, y: 12 }}
                transition={{ duration: .3 }}
              >
                <div aria-hidden="true" className="order-confirmation-icon">
                  {paymentPending || paymentRefunded
                    ? <span aria-hidden="true">₹</span>
                    : <svg fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" viewBox="0 0 24 24">
                      <path d="m5 12 4 4L19 6" />
                    </svg>}
                </div>
                <span className="checkout-eyebrow">THANK YOU FOR SHOPPING WITH SHOPSPHERE</span>
                <h1>
                  {paymentPending
                    ? 'Complete your payment.'
                    : paymentRefunded
                      ? 'Your payment was refunded.'
                      : 'Your order is placed.'}
                </h1>
                <p className="order-confirmation-intro">
                  {paymentPending
                    ? 'Your order is reserved. Complete payment to confirm it; you can safely retry if checkout is cancelled.'
                    : paymentRefunded
                      ? 'The payment was refunded. Contact support if you have questions about the refund.'
                    : 'We’ve received your order and will keep you updated as it moves along.'}
                </p>

                <div className="order-confirmation-number">
                  <span>ORDER NUMBER</span>
                  <strong>#{order.id}</strong>
                  <span className="order-confirmation-status">{order.orderStatus}</span>
                </div>

                <div className="order-confirmation-content">
                  <section className="order-confirmation-products">
                    <h2>Order summary</h2>
                    {order.sellerOrders.map((sellerOrder) => (
                      <div className="confirmation-seller-group" key={sellerOrder.sellerOrderId}>
                        <div className="confirmation-seller-heading">
                          <strong>{sellerOrder.sellerName}</strong>
                          <span>{sellerOrder.orderStatus}</span>
                        </div>
                        {sellerOrder.items.map((item) => (
                          <div className="confirmation-order-item" key={`${sellerOrder.sellerOrderId}-${item.productId}`}>
                            <span>{item.productName} <small>× {item.quantity}</small></span>
                            <strong>{formatProductPrice(item.lineSubtotal)}</strong>
                          </div>
                        ))}
                        <div className="confirmation-seller-fee">
                          <span>Seller delivery</span><strong>{formatProductPrice(sellerOrder.deliveryCharge)}</strong>
                        </div>
                      </div>
                    ))}
                    <div className="confirmation-total">
                      <span>Order total</span><strong>{formatProductPrice(order.totalAmount)}</strong>
                    </div>
                    <p className="confirmation-payment-note">
                      {order.paymentMethod === 'COD'
                        ? 'Cash on delivery · Payment is due when your order arrives.'
                        : order.paymentStatus === 'SUCCESS'
                          ? 'Razorpay payment received and verified.'
                          : `Payment status · ${order.paymentStatus}`}
                    </p>
                    {paymentPending && (
                      <PayNowButton
                        className="checkout-place-order confirmation-pay-button"
                        onPaymentVerified={setOrder}
                        orderId={order.id}
                      />
                    )}
                  </section>

                  <section className="order-confirmation-address">
                    <h2>Delivering to</h2>
                    <strong>{order.shippingAddress.fullName}</strong>
                    <p>{order.shippingAddress.houseFlatNumber}, {order.shippingAddress.streetArea}</p>
                    {order.shippingAddress.landmark && <p>Near {order.shippingAddress.landmark}</p>}
                    <p>{order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.pinCode}</p>
                    <p>{order.shippingAddress.mobileNumber}</p>
                  </section>
                </div>

                <div className="order-confirmation-actions">
                  <Link className="checkout-secondary-link" to={`/orders/${order.id}`}>View order details</Link>
                  <Link className="checkout-secondary-link" to={`/orders/${order.id}/tracking`}>Track order</Link>
                  <Link className="checkout-primary-link" to="/shop">Continue shopping</Link>
                  <Link className="checkout-secondary-link" to="/orders">My orders</Link>
                </div>
              </m.section>
            ) : null}
          </main>
        </m.div>
      </MotionConfig>
    </LazyMotion>
  )
}
