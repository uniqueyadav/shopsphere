import { Link } from 'react-router-dom'

function Icon({ name, size = 18 }) {
  const paths = {
    close: <path d="m6 6 12 12M18 6 6 18"/>,
    print: <><path d="M6 9V3h12v6M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><path d="M6 14h12v7H6zM18 12h.01"/></>,
    truck: <><path d="M3 5h11v12H3zM14 9h4l3 4v4h-7z"/><circle cx="7.5" cy="19" r="1.5"/><circle cx="18" cy="19" r="1.5"/></>,
    pin: <><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/></>,
    card: <><rect x="2" y="5" width="20" height="14"/><path d="M2 10h20M7 15h4"/></>,
    box: <><path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="M3 8v9l9 5 9-5V8M12 13v9"/></>,
    arrow: <><path d="M5 12h14m-6-6 6 6-6 6"/></>,
    cancel: <><circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

export function orderCode(id) {
  return `ORD${String(id).padStart(6, '0')}`
}

export function orderMoney(value) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value || 0))
}

export function orderDate(value, withTime = false) {
  if (!value) return 'Not available'
  return new Intl.DateTimeFormat('en-IN', withTime
    ? { dateStyle: 'medium', timeStyle: 'short' }
    : { dateStyle: 'medium' }).format(new Date(value))
}

function statusLabel(value) {
  return String(value || 'UNKNOWN').toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
}

export function OrderStatusBadge({ status }) {
  return <span className={`ao-status ao-status--${String(status || 'pending').toLowerCase()}`}><i/>{statusLabel(status)}</span>
}

function initials(name) {
  return (name || 'Customer').split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase()
}

export function printOrderSummary(order) {
  const printWindow = window.open('', '_blank', 'width=900,height=760')
  if (!printWindow) return false
  const escape = (value) => String(value ?? '').replace(/[&<>"']/g, (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character])
  const items = (order.sellerOrders || []).flatMap((seller) => (seller.items || []).map((item) => `
    <tr><td>${escape(item.productName)}<small>${escape(item.sku || '')} · ${escape(seller.sellerName)}</small></td><td>${item.quantity}</td><td>${escape(orderMoney(item.unitPrice))}</td><td>${escape(orderMoney(item.lineSubtotal))}</td></tr>`)).join('')
  const address = order.shippingAddress
  const addressText = [address?.fullName, address?.houseFlatNumber, address?.streetArea, address?.landmark, address?.city, address?.state, address?.pinCode, address?.mobileNumber].filter(Boolean).join(', ')
  printWindow.document.write(`<!doctype html><html><head><title>Order ${escape(orderCode(order.id))}</title><meta charset="utf-8"><style>body{font:14px Arial,sans-serif;color:#17213d;margin:40px}h1{font-size:24px;margin:0 0 4px}h2{font-size:15px;margin:25px 0 8px}p{margin:5px 0;color:#44516d}.head{display:flex;justify-content:space-between;border-bottom:2px solid #343c58;padding-bottom:18px}.muted{font-size:12px;color:#63708a}table{width:100%;border-collapse:collapse;margin-top:8px}th,td{text-align:left;padding:10px;border:1px solid #ccd2df}th{background:#f1f2f8}small{display:block;margin-top:4px;color:#63708a}.totals{margin:18px 0 0 auto;width:290px}.totals p{display:flex;justify-content:space-between}.total{border-top:1px solid #bac4d7;padding-top:10px;font-size:17px;font-weight:bold}.note{margin-top:28px;border-top:1px solid #d5dbea;padding-top:12px;font-size:11px}@media print{body{margin:20px}}</style></head><body><div class="head"><div><h1>ShopSphere Order Receipt</h1><p>Order ${escape(orderCode(order.id))}</p></div><div><p>${escape(orderDate(order.createdAt, true))}</p><p>${escape(statusLabel(order.orderStatus))}</p></div></div><h2>Customer and shipping</h2><p>${escape(order.customerName)} · ${escape(order.customerEmail)}</p><p>${escape(addressText || 'Shipping address unavailable')}</p><h2>Items</h2><table><thead><tr><th>Product / Seller</th><th>Qty</th><th>Unit price</th><th>Line total</th></tr></thead><tbody>${items}</tbody></table><div class="totals"><p><span>Subtotal</span><strong>${escape(orderMoney(order.subtotal))}</strong></p><p><span>Shipping</span><strong>${escape(orderMoney(order.deliveryCharges))}</strong></p><p class="total"><span>Total</span><strong>${escape(orderMoney(order.totalAmount))}</strong></p></div><h2>Payment</h2><p>${escape(order.paymentMethod)} · ${escape(order.paymentStatus)} · Refund: ${escape(order.refundStatus)}</p><p class="note">Order receipt only. This order record does not contain GST or tax-invoice fields.</p><script>window.onload=()=>window.print()</script></body></html>`)
  printWindow.document.close()
  return true
}

export default function AdminOrderPanel({ order, onClose, onCancel, onViewFull, onPrintBlocked, variant = 'drawer' }) {
  const sellerOrders = order.sellerOrders || []
  const itemCount = sellerOrders.reduce((total, seller) => total + (seller.items || []).reduce((sum, item) => sum + item.quantity, 0), 0)
  const address = order.shippingAddress
  const addressText = [address?.houseFlatNumber, address?.streetArea, address?.landmark, address?.city, address?.state, address?.pinCode].filter(Boolean).join(', ')
  const printable = () => { if (!printOrderSummary(order)) onPrintBlocked?.() }

  return <section className={`ao-order-panel${variant === 'full' ? ' ao-order-panel--full' : ''}`}>
    <header className="ao-panel-header"><span className="ao-panel-mark"><Icon name="box" size={20}/></span><div><h2>Order Details</h2><p>Parent order · {sellerOrders.length} seller {sellerOrders.length === 1 ? 'order' : 'orders'}</p></div>{onClose && <button aria-label="Close order details" className="ao-icon-button" onClick={onClose} type="button"><Icon name="close"/></button>}</header>
    <div className="ao-panel-scroll">
      <div className="ao-order-banner"><div><OrderStatusBadge status={order.orderStatus}/><h3>#{orderCode(order.id)}</h3><p>{orderDate(order.createdAt, true)}</p></div><button className="ao-button ao-button--outline" onClick={printable} type="button"><Icon name="print"/>Print Invoice</button></div>

      <section className="ao-panel-section"><div className="ao-customer-row"><div className="ao-customer-avatar">{initials(order.customerName)}</div><div className="ao-customer-copy"><strong>{order.customerName || 'Customer'}</strong><a href={`mailto:${encodeURIComponent(order.customerEmail || '')}`}>{order.customerEmail || 'Email unavailable'}</a><span>{order.customerPhone || 'Phone unavailable'}</span></div></div><h3><Icon name="pin"/>Shipping address</h3><p className="ao-address">{address?.fullName ? `${address.fullName}, ` : ''}{addressText || 'Shipping address unavailable'}{address?.mobileNumber ? ` · ${address.mobileNumber}` : ''}</p></section>

      <section className="ao-panel-section"><h3><Icon name="box"/>Order items <span>{itemCount} units</span></h3>{sellerOrders.map((seller) => <article className="ao-seller-block" key={seller.sellerOrderId}><header><div><strong>{seller.sellerName}</strong><small>Seller order #{seller.sellerOrderId}</small></div><OrderStatusBadge status={seller.orderStatus}/></header>{seller.items?.map((item) => <div className="ao-detail-item" key={`${seller.sellerOrderId}-${item.productId}`}><div className="ao-item-image">{item.imageUrl ? <img alt="" src={item.imageUrl}/> : <Icon name="box"/>}</div><div className="ao-item-copy"><strong>{item.productName}</strong><small>{item.sku ? `SKU: ${item.sku} · ` : ''}{seller.sellerName}</small><span>Qty {item.quantity} × {orderMoney(item.unitPrice)}</span></div><strong className="ao-item-total">{orderMoney(item.lineSubtotal)}</strong></div>)}</article>)}</section>

      <section className="ao-panel-section"><h3>Price summary</h3><dl className="ao-summary"><div><dt>Subtotal</dt><dd>{orderMoney(order.subtotal)}</dd></div><div><dt>Shipping charges</dt><dd>{orderMoney(order.deliveryCharges)}</dd></div><div className="ao-summary-total"><dt>Total amount</dt><dd>{orderMoney(order.totalAmount)}</dd></div></dl><p className="ao-muted-note">Coupon, discount, and tax breakdowns are not stored on this order record.</p></section>

      <section className="ao-panel-section"><h3><Icon name="card"/>Payment details</h3><dl className="ao-detail-grid"><div><dt>Method</dt><dd>{order.paymentMethod}</dd></div><div><dt>Payment status</dt><dd><OrderStatusBadge status={order.paymentStatus}/></dd></div><div><dt>Refund status</dt><dd>{order.refundStatus}</dd></div>{order.payment && <><div><dt>Transaction</dt><dd>#{order.payment.transactionId}</dd></div><div><dt>Payment reference</dt><dd>{order.payment.paymentReference || 'Not assigned'}</dd></div><div><dt>Payment recorded</dt><dd>{orderDate(order.payment.updatedAt || order.payment.createdAt, true)}</dd></div>{order.payment.refundReference && <div><dt>Refund reference</dt><dd>{order.payment.refundReference}</dd></div>}</>}</dl>{!order.payment && <p className="ao-muted-note">No payment transaction record is available for this order.</p>}</section>

      <section className="ao-panel-section"><h3><Icon name="truck"/>Fulfillment</h3><p className="ao-muted-note">Delivery partner, tracking number, and estimated delivery date are not available in the current backend.</p>{sellerOrders.map((seller) => <div className="ao-fulfillment-row" key={`fulfillment-${seller.sellerOrderId}`}><span>{seller.sellerName}</span><OrderStatusBadge status={seller.orderStatus}/></div>)}</section>

      <section className="ao-panel-section"><h3>Order timeline</h3>{order.timeline?.length ? <ol className="ao-timeline">{order.timeline.map((event, index) => <li key={`${event.timestamp}-${event.source}-${index}`}><span className="ao-timeline-dot"/><div><strong>{event.label}</strong><small>{orderDate(event.timestamp, true)} · {event.source}</small>{event.note && <p>{event.note}</p>}</div></li>)}</ol> : <p className="ao-muted-note">No order status history is available.</p>}</section>
      <p className="ao-invoice-note">Print Invoice creates a printable order receipt. A GST invoice service is not present in the backend.</p>
    </div>
    <footer className="ao-panel-footer">{order.cancellationEligible && onCancel && <button className="ao-button ao-button--danger" onClick={onCancel} type="button"><Icon name="cancel"/>Cancel order</button>}{onViewFull && <button className="ao-button ao-button--primary" onClick={onViewFull} type="button">View Full Details <Icon name="arrow"/></button>}<Link className="ao-button ao-button--outline" to="/admin/sellers">Seller management</Link></footer>
  </section>
}
