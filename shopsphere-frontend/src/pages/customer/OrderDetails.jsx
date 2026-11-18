import { useEffect, useState } from 'react'
import { LazyMotion, MotionConfig, domAnimation, m } from 'framer-motion'
import { useDispatch } from 'react-redux'
import { Link, useParams } from 'react-router-dom'
import MainNavbar from '../../components/navbar/MainNavbar'
import { formatProductPrice } from '../../components/product/ProductCard'
import { addCartItem, fetchCart } from '../../features/cart/cartSlice'
import { getRequestError } from '../../services/authService'
import { cancelOrder, getOrder } from '../../services/orderService'
import { canCancelOrder, getOrderStatusLabel, reorderOrder } from './orderUtils'
import './OrderHistory.css'

function OrderDetailsSkeleton() {
  return (
    <div aria-label="Loading order details" className="order-detail-skeleton" role="status">
      <div><span /><span /><span /></div>
      <div><span /><span /><span /><span /></div>
      <span className="orders-visually-hidden">Loading your order…</span>
    </div>
  )
}

function OrderItems({ sellerOrder }) {
  return (
    <section className="order-detail-seller">
      <header>
        <div>
          <span className="orders-card-label">SOLD BY</span>
          <h3>{sellerOrder.sellerName}</h3>
        </div>
        <span className={`orders-status orders-status--${sellerOrder.orderStatus?.toLowerCase().replaceAll('_', '-')}`}>
          {getOrderStatusLabel(sellerOrder.orderStatus)}
        </span>
      </header>
      {sellerOrder.items.map((item, index) => (
        <article className="order-detail-item" key={`${item.productId}-${index}`}>
          <span aria-hidden="true" className="orders-product-mark">SS</span>
          <div className="order-detail-item-copy">
            <Link to={`/products/${item.productId}`}>{item.productName}</Link>
            <span>SKU {item.sku} · Quantity {item.quantity}</span>
            <span>{formatProductPrice(item.unitPrice)} each</span>
          </div>
          <strong>{formatProductPrice(item.lineSubtotal)}</strong>
        </article>
      ))}
      <div className="order-detail-seller-total">
        <span>Items subtotal</span><strong>{formatProductPrice(sellerOrder.subtotal)}</strong>
      </div>
      <div className="order-detail-seller-total">
        <span>Seller delivery</span><strong>{formatProductPrice(sellerOrder.deliveryCharge)}</strong>
      </div>
    </section>
  )
}

export default function OrderDetails() {
  const { orderId } = useParams()
  const dispatch = useDispatch()
  const [order, setOrder] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)
  const [operation, setOperation] = useState(null)

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

  async function handleCancel() {
    const retryingRefund = order.refundStatus === 'FAILED'
    if (!retryingRefund && !window.confirm('Cancel this order? This action cannot be undone.')) return
    setOperation('cancel')
    setError(null)
    setNotice(null)
    try {
      const result = await cancelOrder(order.id)
      setOrder(result.order)
      setNotice(retryingRefund
        ? result.refundPending
          ? 'Your refund retry was accepted and is being processed.'
          : 'Your refund has been completed.'
        : result.refundPending
          ? 'Your order is cancelled. The prepaid refund is being processed.'
          : 'Your order has been cancelled.')
    } catch (requestError) {
      setError(getRequestError(requestError, 'This order could not be cancelled.'))
    } finally {
      setOperation(null)
    }
  }

  async function handleReorder() {
    setOperation('reorder')
    setError(null)
    setNotice(null)
    const result = await reorderOrder(order, (item) => dispatch(addCartItem(item)).unwrap())
    await dispatch(fetchCart())
    setOperation(null)
    setNotice(result.failedItems.length
      ? `${result.addedItems} item${result.addedItems === 1 ? '' : 's'} added. ${result.failedItems.length} could not be added because availability may have changed.`
      : 'Order items were added to your cart.')
  }

  return (
    <LazyMotion features={domAnimation}>
      <MotionConfig reducedMotion="user">
        <m.div
          animate={{ opacity: 1, y: 0 }}
          className="order-history-page"
          initial={{ opacity: 0, y: 8 }}
          transition={{ duration: .3 }}
        >
          <MainNavbar />
          <main className="orders-container order-detail-container">
            <nav aria-label="Breadcrumb" className="orders-breadcrumb">
              <Link to="/shop">Home</Link><span>/</span><Link to="/orders">My orders</Link><span>/</span><span aria-current="page">Order details</span>
            </nav>
            {status === 'loading' ? <OrderDetailsSkeleton /> : status === 'failed' ? (
              <section className="orders-state orders-state--error">
                <h1>We couldn’t find this order.</h1>
                <p role="alert">{error}</p>
                <Link className="orders-primary-button" to="/orders">Back to my orders</Link>
              </section>
            ) : order ? (
              <m.div animate={{ opacity: 1, y: 0 }} className="order-detail-layout" initial={{ opacity: 0, y: 10 }}>
                <div className="order-detail-main">
                  <header className="order-detail-heading">
                    <div>
                      <span className="orders-eyebrow">ORDER DETAILS</span>
                      <h1>Order #{order.id}</h1>
                      <p>Placed {new Date(order.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</p>
                    </div>
                    <span className={`orders-status orders-status--${order.orderStatus?.toLowerCase().replaceAll('_', '-')}`}>
                      {getOrderStatusLabel(order.orderStatus)}
                    </span>
                  </header>

                  {notice && <p className="orders-notice" role="status">{notice}</p>}
                  {error && <p className="orders-error" role="alert">{error}</p>}

                  <div className="order-detail-sellers">
                    {order.sellerOrders.map((sellerOrder) => <OrderItems key={sellerOrder.sellerOrderId} sellerOrder={sellerOrder} />)}
                  </div>
                </div>

                <aside className="order-detail-aside">
                  <section className="order-detail-panel">
                    <h2>Payment summary</h2>
                    <div><span>Items subtotal</span><strong>{formatProductPrice(order.subtotal)}</strong></div>
                    <div><span>Delivery</span><strong>{formatProductPrice(order.deliveryCharges)}</strong></div>
                    <div className="order-detail-grand-total"><span>Total</span><strong>{formatProductPrice(order.totalAmount)}</strong></div>
                    <p className="order-detail-payment-method">
                      {order.paymentMethod === 'COD' ? 'Cash on delivery' : 'Razorpay · Online payment'}
                      <span>{getOrderStatusLabel(order.paymentStatus)}</span>
                    </p>
                    {order.refundStatus && order.refundStatus !== 'NOT_REQUESTED' && (
                      <p className={`order-refund-status order-refund-status--${order.refundStatus.toLowerCase()}`}>
                        Refund · {getOrderStatusLabel(order.refundStatus)}
                      </p>
                    )}
                  </section>

                  <section className="order-detail-panel order-detail-address">
                    <h2>Delivery address</h2>
                    <strong>{order.shippingAddress.fullName}</strong>
                    <p>{order.shippingAddress.houseFlatNumber}, {order.shippingAddress.streetArea}</p>
                    {order.shippingAddress.landmark && <p>Near {order.shippingAddress.landmark}</p>}
                    <p>{order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.pinCode}</p>
                    <p>{order.shippingAddress.mobileNumber}</p>
                    <span className="order-address-type">{getOrderStatusLabel(order.shippingAddress.addressType)}</span>
                  </section>

                  <section className="order-detail-panel order-detail-actions">
                    <Link className="orders-primary-button" to={`/orders/${order.id}/tracking`}>Track this order</Link>
                    {canCancelOrder(order) && (
                      <button className="orders-danger-button" disabled={Boolean(operation)} onClick={handleCancel} type="button">
                        {operation === 'cancel' ? 'Cancelling…' : 'Cancel order'}
                      </button>
                    )}
                    {order.orderStatus === 'CANCELLED' && order.refundStatus === 'FAILED' && (
                      <button className="orders-secondary-button" disabled={Boolean(operation)} onClick={handleCancel} type="button">
                        {operation === 'cancel' ? 'Retrying refund…' : 'Retry refund'}
                      </button>
                    )}
                    {['DELIVERED', 'CANCELLED'].includes(order.orderStatus) && (
                      <button className="orders-secondary-button" disabled={Boolean(operation)} onClick={handleReorder} type="button">
                        {operation === 'reorder' ? 'Adding to cart…' : 'Reorder these items'}
                      </button>
                    )}
                  </section>
                </aside>
              </m.div>
            ) : null}
          </main>
        </m.div>
      </MotionConfig>
    </LazyMotion>
  )
}