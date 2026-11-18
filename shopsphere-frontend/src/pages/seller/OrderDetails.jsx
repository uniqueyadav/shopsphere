import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import SellerLayout, { SellerIcon } from '../../layouts/SellerLayout'
import { getRequestError } from '../../services/authService'
import { getSellerOrderDetails, updateSellerOrderStatus } from '../../services/orderService'
import './SellerOrderDetails.css'

const NEXT_STATUS = { PLACED: 'CONFIRMED', CONFIRMED: 'PROCESSING', PROCESSING: 'SHIPPED', SHIPPED: 'OUT_FOR_DELIVERY', OUT_FOR_DELIVERY: 'DELIVERED' }
const LABELS = { PAYMENT_PENDING: 'Payment pending', PLACED: 'Order placed', CONFIRMED: 'Confirmed', PROCESSING: 'Processing', SHIPPED: 'Shipped', OUT_FOR_DELIVERY: 'Out for delivery', DELIVERED: 'Delivered', CANCELLED: 'Cancelled' }
const STAGES = ['PLACED', 'PROCESSING', 'SHIPPED', 'DELIVERED']
const currency = (value) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value) || 0)
const dateTime = (value) => value ? new Date(value).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : 'Not recorded'
const dateOnly = (value) => value ? new Date(value).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : 'Not recorded'
const asStatus = (status) => status === 'CONFIRMED' ? 'PROCESSING' : status === 'OUT_FOR_DELIVERY' ? 'SHIPPED' : status

function DetailIcon({ name, size = 20 }) {
  const paths = {
    back: <><path d="M19 12H5m7 7-7-7 7-7"/></>,
    package: <><path d="m12 3 9 4.5v9L12 21l-9-4.5v-9L12 3Z"/><path d="m3.5 7.5 8.5 4 8.5-4M12 12v9"/></>,
    truck: <><path d="M3 6h11v11H3zM14 10h4l3 3v4h-7z"/><circle cx="7.5" cy="19" r="1.5"/><circle cx="18" cy="19" r="1.5"/></>,
    refresh: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.5 9A7 7 0 0 1 18 6l2 2M4 16l2 2a7 7 0 0 0 12.5-3"/></>,
    card: <><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 10h18m-14 5h4"/></>,
    pin: <><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

function StatusPill({ status }) {
  const tone = status === 'DELIVERED' ? 'green' : status === 'CANCELLED' ? 'red' : ['SHIPPED', 'OUT_FOR_DELIVERY'].includes(status) ? 'blue' : ['PROCESSING', 'CONFIRMED'].includes(status) ? 'yellow' : 'blue'
  return <span className={`seller-order-detail-pill ${tone}`}>{LABELS[status] || status?.replaceAll('_', ' ')}</span>
}

export default function OrderDetails() {
  const { sellerOrderId } = useParams()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [updating, setUpdating] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try { setOrder(await getSellerOrderDetails(sellerOrderId)) }
    catch (requestError) { setError(getRequestError(requestError, 'Could not load this seller order.')); setOrder(null) }
    finally { setLoading(false) }
  }, [sellerOrderId])

  useEffect(() => { load() }, [load])

  const currentStageIndex = useMemo(() => STAGES.indexOf(asStatus(order?.status)), [order?.status])
  const nextStatus = NEXT_STATUS[order?.status]

  async function updateStatus() {
    if (!nextStatus) return
    setUpdating(true); setError(''); setNotice('')
    try {
      await updateSellerOrderStatus(sellerOrderId, nextStatus)
      setNotice(`Order updated to ${LABELS[nextStatus] || nextStatus}.`)
      await load()
    } catch (requestError) { setError(getRequestError(requestError, 'Could not update this order status.')) }
    finally { setUpdating(false) }
  }

  if (loading && !order) return <SellerLayout><main className="seller-order-detail-page"><div className="seller-order-detail-state" role="status">Loading order details…</div></main></SellerLayout>

  return <SellerLayout>
    <main className="seller-order-detail-page">
      <header className="seller-order-detail-heading">
        <div><Link className="seller-order-back" to="/seller/orders"><DetailIcon name="back" size={18}/> Back to Orders</Link><div className="seller-order-detail-title"><span><SellerIcon name="cart" size={27}/></span><div><h1>Order Details</h1><p>Review products, fulfillment and delivery information</p></div></div></div>
        {order && <div className="seller-order-heading-status"><StatusPill status={order.status}/><small>Order date: {dateTime(order.orderDate)}</small></div>}
      </header>

      {error && <div className="seller-order-detail-alert" role="alert">{error}<button onClick={() => setError('')} type="button" aria-label="Dismiss">×</button></div>}
      {notice && <div className="seller-order-detail-notice" role="status">{notice}</div>}
      {!order ? <section className="seller-order-detail-state"><h2>Order unavailable</h2><p>This order could not be loaded for your seller account.</p><Link to="/seller/orders">Return to orders</Link></section> : <>
        <section className="seller-order-overview" aria-label="Order overview">
          <article><span className="overview-icon blue"><DetailIcon name="package" size={27}/></span><div><small>Order ID</small><strong>#SS{order.orderId}</strong><span>Seller order #{order.sellerOrderId}</span></div></article>
          <article><span className="overview-avatar">{(order.customerName || 'C').slice(0, 1).toUpperCase()}</span><div><small>Customer</small><strong>{order.customerName || 'Customer'}</strong><span>{order.customerEmail || 'Email unavailable'}</span></div></article>
          <article><span className="overview-icon blue"><SellerIcon name="wallet" size={27}/></span><div><small>Total Amount</small><strong>{currency(order.totalAmount)}</strong><span>Seller order total</span></div></article>
          <article><span className="overview-icon gold"><DetailIcon name="card" size={27}/></span><div><small>Payment Method</small><strong>{order.paymentMethod?.replaceAll('_', ' ') || 'Not recorded'}</strong><span>{order.paymentStatus?.replaceAll('_', ' ') || 'Payment status unavailable'}</span></div></article>
        </section>

        <div className="seller-order-detail-columns">
          <div className="seller-order-detail-main">
            <section className="seller-detail-card">
              <header><span><SellerIcon name="box" size={21}/></span><h2>Order Items <small>({order.items?.length || 0})</small></h2></header>
              <div className="seller-detail-items">{order.items?.map((item) => <article key={`${item.productId}-${item.sku}`}><div className="seller-detail-product-image">{item.imageUrl ? <img alt={item.productName} src={item.imageUrl}/> : <SellerIcon name="box" size={24}/>}</div><div className="seller-detail-product-info"><strong>{item.productName}</strong><span>{item.sku ? `SKU: ${item.sku}` : 'Variant not recorded'} · Qty: {item.quantity}</span><small>Unit price {currency(item.unitPrice)}</small></div><b>{currency(item.lineSubtotal)}</b></article>)}</div>
              <dl className="seller-detail-pricing"><div><dt>Subtotal</dt><dd>{currency(order.subtotal)}</dd></div><div><dt>Discount</dt><dd>{order.discountAmount == null ? 'Not recorded' : `− ${currency(order.discountAmount)}`}</dd></div><div><dt>Tax</dt><dd>{order.taxAmount == null ? 'Not recorded' : currency(order.taxAmount)}</dd></div><div><dt>Shipping charges</dt><dd>{order.shippingCharges == null ? 'Not recorded' : currency(order.shippingCharges)}</dd></div><div className="seller-detail-grand-total"><dt>Total Amount</dt><dd>{currency(order.totalAmount)}</dd></div></dl>
            </section>

            <section className="seller-detail-shipping">
              <header><span><DetailIcon name="pin" size={21}/></span><h2>Shipping Information</h2></header>
              <div className="seller-detail-shipping-grid"><div><small>Deliver to</small><strong>{order.shippingName || order.customerName || 'Not recorded'}</strong>{order.shippingMobile && <span>{order.shippingMobile}</span>}<p>{order.shippingAddress || 'Shipping address not recorded'}</p></div><div><small>Shipping method</small><strong>{order.shippingMethod || 'Not recorded'}</strong><small>Estimated delivery</small><strong>{dateOnly(order.estimatedDelivery)}</strong></div></div>
            </section>
          </div>

          <div className="seller-order-detail-side">
            <section className="seller-detail-card" id="tracking">
              <header><span><DetailIcon name="truck" size={22}/></span><h2>Order Status Tracking</h2></header>
              {order.status === 'CANCELLED' ? <div className="seller-detail-cancelled"><StatusPill status="CANCELLED"/><p>This seller order was cancelled.</p></div> : <div className="seller-detail-timeline">{STAGES.map((stage, index) => {
                const completed = currentStageIndex >= index && currentStageIndex !== -1
                const event = order.history?.filter((entry) => asStatus(entry.status) === stage || (stage === 'SHIPPED' && entry.status === 'OUT_FOR_DELIVERY')).at(-1)
                const current = index === currentStageIndex
                return <article className={`${completed ? 'is-complete' : ''}${current ? ' is-current' : ''}`} key={stage}><span className="seller-detail-step-dot">{completed ? '✓' : index + 1}</span><div><strong>{LABELS[stage]}</strong><small>{event ? dateTime(event.changedAt) : current ? 'Current status' : 'Pending'}</small>{event?.note && <p>{event.note}</p>}</div><em>{completed ? (current ? 'Current' : 'Completed') : 'Pending'}</em></article>
              })}</div>}
            </section>
            <section className="seller-detail-card seller-detail-actions"><header><span><SellerIcon name="gear" size={21}/></span><h2>Actions</h2></header><div className="seller-detail-action-buttons"><a className="seller-detail-action-link" href="#tracking"><DetailIcon name="truck"/> Track Order</a>{nextStatus ? <button className="seller-detail-action-primary" disabled={updating} onClick={updateStatus} type="button"><DetailIcon name="refresh"/>{updating ? 'Updating…' : `Mark ${LABELS[nextStatus]}`}</button> : <span className="seller-detail-action-note">No further status update is available.</span>}</div><p className="seller-detail-action-note">Order cancellation and return requests are not available in the seller workflow.</p></section>
          </div>
        </div>
      </>}
    </main>
  </SellerLayout>
}
