import { useCallback, useEffect, useMemo, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { logout } from '../../features/auth/authSlice'
import { getRequestError } from '../../services/authService'
import { acceptDeliveryAssignment, getDeliveryAssignmentDetails, setDeliveryAvailability, updateDeliveryAssignmentStatus } from '../../services/deliveryService'
import './DeliveryOrderDetails.css'

const money = value => `₹${Number(value || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const labels = { PAYMENT_PENDING: 'Payment pending', PLACED: 'Order placed', CONFIRMED: 'Confirmed', PROCESSING: 'Preparing', SHIPPED: 'Ready for delivery', OUT_FOR_DELIVERY: 'In transit', DELIVERED: 'Delivered', CANCELLED: 'Cancelled', ACCEPTED: 'Assignment accepted' }
const fmtDate = value => value ? new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : ''
function Icon({ name, size = 20 }) {
  const paths = {
    truck: <><path d="M3 6h11v11H3zM14 10h4l3 4v3h-7z"/><circle cx="7" cy="19" r="2"/><circle cx="18" cy="19" r="2"/></>,
    box: <><path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="M3 8v9l9 5 9-5V8M12 13v9"/></>,
    back: <><path d="m15 18-6-6 6-6"/><path d="M9 12h12"/><path d="M5 4H3v16h2"/></>,
    pin: <><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/></>,
    phone: <path d="M7 3H5a2 2 0 0 0-2 2c0 8.8 7.2 16 16 16a2 2 0 0 0 2-2v-2l-5-2-2 3a14 14 0 0 1-6-6l3-2-2-5H7Z"/>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    user: <><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></>,
    card: <><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 10h18M7 15h3"/></>,
    route: <><path d="M4 19V5m0 0 4 4m-4-4L1 8M20 5v14m0 0-4-4m4 4 3-4"/><path d="M7 12h10"/></>,
    refresh: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.6 9a7 7 0 0 1 11.7-2L20 12M4 12l2.7 5a7 7 0 0 0 11.7-2"/></>,
    check: <><circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/></>,
    logout: <><path d="m10 17 5-5-5-5m5 5H3"/><path d="M12 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-6"/></>,
    dashboard: <><path d="m3 10 9-7 9 7M5 9v11h14V9M9 20v-7h6v7"/></>,
    history: <><path d="M3 12a9 9 0 1 0 2.6-6.4L3 8"/><path d="M3 3v5h5M12 7v5l3 2"/></>,
    userCircle: <><circle cx="12" cy="12" r="9"/><circle cx="12" cy="9" r="3"/><path d="M6.5 19a6 6 0 0 1 11 0"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">{paths[name] || paths.box}</svg>
}
function distanceKm(a) {
  if ([a.pickupLatitude, a.pickupLongitude, a.dropLatitude, a.dropLongitude].some(v => v == null)) return null
  const rad = n => Number(n) * Math.PI / 180
  const dLat = rad(a.dropLatitude) - rad(a.pickupLatitude), dLon = rad(a.dropLongitude) - rad(a.pickupLongitude)
  const lat1 = rad(a.pickupLatitude), lat2 = rad(a.dropLatitude)
  const h = Math.min(1, Math.max(0, Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) ** 2))
  return 6371 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h))
}
function Status({ status }) { return <span className={`cod-status cod-status--${String(status || '').toLowerCase()}`}><i/>{labels[status] || String(status || 'Unknown').replaceAll('_', ' ')}</span> }

export default function DeliveryOrderDetails() {
  const { assignmentId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const dispatch = useDispatch()
  const currentUser = useSelector(state => state.auth.user)
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const load = useCallback(async (showLoading = true) => {
    if (showLoading) setLoading(true)
    try { setOrder(await getDeliveryAssignmentDetails(assignmentId)); setError('') }
    catch (e) { setError(getRequestError(e, 'Could not load this delivery order.')) }
    finally { if (showLoading) setLoading(false) }
  }, [assignmentId])
  useEffect(() => { load() }, [load])
  useEffect(() => {
    const timer = window.setInterval(() => load(false), 10000)
    return () => window.clearInterval(timer)
  }, [load])
  const km = useMemo(() => order ? distanceKm(order) : null, [order])
  const preDelivery = order && ['PLACED', 'CONFIRMED', 'PROCESSING', 'SHIPPED'].includes(order.status)
  const action = !order ? null : !order.acceptedAt && preDelivery ? 'ACCEPT' : order.acceptedAt && order.status === 'SHIPPED' ? 'START' : order.status === 'OUT_FOR_DELIVERY' ? 'DELIVER' : null
  const placedAt = order?.timeline?.find(event => event.status === 'PLACED')?.changedAt || order?.createdAt
  const routeUrl = order ? `https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(order.pickupLatitude != null ? `${order.pickupLatitude},${order.pickupLongitude}` : order.pickupAddress)}&destination=${encodeURIComponent(order.dropLatitude != null ? `${order.dropLatitude},${order.dropLongitude}` : order.customerAddress)}` : '#'
  let mapUrl = null
  if (order?.pickupLatitude != null && order?.dropLatitude != null) {
    const minLat = Math.min(order.pickupLatitude, order.dropLatitude) - .012, maxLat = Math.max(order.pickupLatitude, order.dropLatitude) + .012
    const minLon = Math.min(order.pickupLongitude, order.dropLongitude) - .012, maxLon = Math.max(order.pickupLongitude, order.dropLongitude) + .012
    mapUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${minLon}%2C${minLat}%2C${maxLon}%2C${maxLat}&layer=mapnik&marker=${order.dropLatitude}%2C${order.dropLongitude}`
  }
  async function runAction() {
    if (!order || !action) return
    setBusy(true); setError('')
    try {
      if (action === 'ACCEPT') {
        if (!order.partnerOnline) await setDeliveryAvailability(true)
        await acceptDeliveryAssignment(order.assignmentId)
      }
      else await updateDeliveryAssignmentStatus(order.assignmentId, action === 'START' ? 'OUT_FOR_DELIVERY' : 'DELIVERED')
      await load(false)
    } catch (e) { setError(getRequestError(e, 'Could not update this delivery.')) }
    finally { setBusy(false) }
  }
  const actionText = action === 'ACCEPT' ? 'Accept Order' : action === 'START' ? 'Start Delivery' : action === 'DELIVER' ? 'Mark as Delivered' : order?.acceptedAt && ['PLACED', 'CONFIRMED', 'PROCESSING'].includes(order.status) ? 'Waiting for seller to ship' : 'No action available'
  const tabs = [['Dashboard', 'dashboard'], ['Assigned Orders', 'box'], ['Delivery History', 'history'], ['Earnings', 'card'], ['Profile', 'userCircle']]
  function goTab(tab) { navigate('/delivery/dashboard', { state: { initialTab: tab } }) }
  return <div className="courier-order-shell">
    <aside className="courier-order-sidebar"><a className="cod-brand" href="/"><span><Icon name="bag" size={26}/></span><b>ShopSphere</b></a><small>COURIER PANEL</small><nav>{tabs.map(([tab, icon]) => <button className={tab === 'Assigned Orders' ? 'active' : ''} key={tab} onClick={() => goTab(tab)} type="button"><Icon name={icon}/>{tab}</button>)}</nav><div className="cod-side-user"><span>{(order?.partnerName || currentUser?.fullName || 'DP').split(/\s+/).map(x => x[0]).join('').slice(0, 2)}</span><div><b>{order?.partnerName || currentUser?.fullName || 'Delivery Partner'}</b><small>Delivery Partner · Online</small></div></div></aside>
    <main className="courier-order-main"><header className="cod-topbar"><span>Dashboard <b>›</b> Assigned Orders <b>›</b> Order Details</span><div><strong>{order?.partnerName || currentUser?.fullName || 'Delivery Partner'}</strong><button onClick={() => dispatch(logout())} type="button"><Icon name="logout" size={16}/> Sign out</button></div></header>
      <section className="cod-content"><div className="cod-heading"><div><button className="cod-back" onClick={() => navigate('/delivery/dashboard', { state: { initialTab: 'Assigned Orders' } })} type="button"><Icon name="back" size={18}/> Back to Orders</button><div><p>DELIVERY PARTNER PANEL</p><h1>Order Details</h1><span>View shipment information and update delivery progress.</span></div></div><div className="cod-heading-actions">{order && <Status status={order.status}/>}<button className="cod-primary" disabled={busy || loading || !action} onClick={runAction} type="button"><Icon name={action === 'DELIVER' ? 'check' : 'refresh'} size={17}/>{busy ? 'Updating…' : actionText}</button></div></div>
        {error && <div className="cod-error" role="alert">{error}<button onClick={() => load()} type="button">Retry</button></div>}
        {loading ? <div className="cod-loading">Loading delivery order…</div> : !order ? <div className="cod-loading">This order is unavailable or is not assigned to your account.</div> : <>
          <section className="cod-kpis"><article><span className="purple"><Icon name="box" size={25}/></span><div><small>Order ID</small><strong>#{order.orderId}</strong><em>Placed {fmtDate(placedAt)}</em></div></article><article><span className="orange"><Icon name="card" size={25}/></span><div><small>Delivery Earnings</small><strong>{money(order.deliveryFee)}</strong><em>Per completed seller shipment</em></div></article><article><span className="blue"><Icon name="pin" size={25}/></span><div><small>Distance</small><strong>{km == null ? '—' : `${km.toFixed(1)} km`}</strong><em>{km == null ? 'Location pins unavailable' : `Approx. ${Math.max(5, Math.ceil(km / 25 * 60))} min by road`}</em></div></article><article><span className="mint"><Icon name="card" size={25}/></span><div><small>Payment Method</small><strong>{order.paymentMethod === 'COD' ? 'Cash on Delivery' : order.paymentMethod}</strong><em>{order.paymentStatus === 'SUCCESS' ? 'Paid successfully' : order.paymentStatus.replaceAll('_', ' ')}</em></div></article></section>
          <div className="cod-grid"><div className="cod-left"><div className="cod-people-grid"><section className="cod-card"><header><h2><Icon name="user"/> Customer Details</h2>{order.customerPhone && <a className="cod-call" href={`tel:${order.customerPhone}`}><Icon name="phone" size={16}/> Call Customer</a>}</header><div className="cod-person"><span className="cod-avatar"><Icon name="user" size={28}/></span><div><strong>{order.customerName}</strong><a href={`tel:${order.customerPhone}`}>{order.customerPhone || 'Phone unavailable'}</a><p><Icon name="pin" size={15}/>{order.customerAddress}</p></div></div>{(order.deliveryInstructions || order.callBeforeDelivery || order.leaveAtDoor) && <div className="cod-instructions"><strong>Delivery instructions</strong><span>{order.deliveryInstructions || [order.callBeforeDelivery && 'Call before delivery', order.leaveAtDoor && 'Leave package at door'].filter(Boolean).join(' · ')}</span></div>}</section>
            <section className="cod-card"><header><h2><Icon name="box"/> Pickup Details</h2></header><div className="cod-pickup"><span className="cod-store-mark"><Icon name="box" size={24}/></span><div><strong>{order.pickupName}</strong><p>{order.pickupAddress}</p>{order.pickupPhone && <a href={`tel:${order.pickupPhone}`}><Icon name="phone" size={14}/>{order.pickupPhone}</a>}</div></div></section></div>
            <section className="cod-card cod-route-card"><header><h2><Icon name="pin"/> Delivery Route</h2><a href={routeUrl} rel="noreferrer" target="_blank">View Full Map <span>→</span></a></header>{mapUrl ? <iframe title="Pickup and delivery map" loading="lazy" src={mapUrl}/> : <div className="cod-map-fallback"><Icon name="route" size={34}/><strong>Route locations are not pinned yet</strong><span>Use the addresses below to navigate.</span></div>}<div className="cod-route-labels"><span><i className="pickup-dot"/> Pickup · {order.pickupName}</span><span><i className="drop-dot"/> Drop · {order.customerAddress}</span></div></section>
            <section className="cod-card cod-items"><header><h2><Icon name="box"/> Order Items <b>{order.items.length}</b></h2></header>{order.items.map(item => <article key={item.productId}><span className="cod-item-image">{item.imageUrl ? <img alt="" src={item.imageUrl}/> : <Icon name="box" size={24}/>}</span><div><strong>{item.name}</strong><small>{item.sku} · Qty: {item.quantity}</small></div><b>{money(item.lineSubtotal)}</b></article>)}</section>
          </div><aside className="cod-right"><section className="cod-card cod-timeline"><header><h2><span className="cod-shield">✓</span> Delivery Timeline</h2></header><ol>{order.timeline.map((event, index) => <li className={`cod-timeline-event ${event.status === order.status ? 'current' : 'done'}`} key={`${event.status}-${event.changedAt}-${index}`}><span className="cod-timeline-dot">{event.status === order.status ? <Icon name="truck" size={14}/> : <Icon name="check" size={15}/>}</span><div><strong>{labels[event.status] || event.status.replaceAll('_', ' ')}</strong><small>{fmtDate(event.changedAt)}</small>{event.note && event.status === order.status && <em>{event.note}</em>}</div></li>)}{order.acceptedAt && !order.timeline.some(event => event.status === 'ACCEPTED') && <li className="cod-timeline-event done"><span className="cod-timeline-dot"><Icon name="check" size={15}/></span><div><strong>Assignment accepted</strong><small>{fmtDate(order.acceptedAt)}</small></div></li>}</ol></section><section className="cod-card cod-summary"><header><h2><Icon name="card"/> Order Summary</h2></header><dl><div><dt>Items total</dt><dd>{money(order.subtotal)}</dd></div><div><dt>Delivery fee</dt><dd>{money(order.deliveryFee)}</dd></div><div className="total"><dt>Shipment total</dt><dd>{money(order.totalAmount)}</dd></div></dl></section><div className="cod-bottom-actions"><a href={routeUrl} rel="noreferrer" target="_blank"><Icon name="route" size={18}/> Navigate</a><button className="cod-primary" disabled={busy || !action} onClick={runAction} type="button"><Icon name={action === 'DELIVER' ? 'check' : 'refresh'} size={17}/>{busy ? 'Updating…' : actionText}</button></div></aside></div>
        </>}
      </section></main>
  </div>
}
