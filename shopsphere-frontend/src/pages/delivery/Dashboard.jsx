import { useCallback, useEffect, useMemo, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useLocation, useNavigate } from 'react-router-dom'
import { logout } from '../../features/auth/authSlice'
import { getRequestError } from '../../services/authService'
import { acceptDeliveryAssignment, getDeliveryDashboard, setDeliveryAvailability, updateDeliveryAssignmentStatus, updateDeliveryLocation } from '../../services/deliveryService'
import './DeliveryDashboard.css'
import './DeliveryLocation.css'
import './DeliveryTypography.css'
import './DeliveryIcons.css'
import './DeliveryAssignedOrders.css'

const money = (value) => `₹${Number(value || 0).toLocaleString('en-IN')}`
const tabs = ['Dashboard', 'Assigned Orders', 'Delivery History', 'Earnings', 'Profile']

function CourierIcon({ name, size = 20 }) {
  const shapes = {
    dashboard: <><path d="m3 10 9-7 9 7"/><path d="M5 9v11h14V9M9 20v-7h6v7"/></>,
    box: <><path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="M3 8v9l9 5 9-5V8M12 13v9"/></>,
    history: <><path d="M3 12a9 9 0 1 0 2.6-6.4L3 8"/><path d="M3 3v5h5M12 7v5l3 2"/></>,
    rupee: <><path d="M6 4h12M6 8h12M8 4c6 0 6 8 0 8h-2l9 8"/></>,
    user: <><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></>,
    truck: <><path d="M3 6h11v11H3zM14 10h4l3 4v3h-7z"/><circle cx="7" cy="19" r="2"/><circle cx="18" cy="19" r="2"/></>,
    check: <><circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    pin: <><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/></>,
    chevron: <path d="m9 18 6-6-6-6"/>,
    logout: <><path d="M10 17l5-5-5-5M15 12H3"/><path d="M12 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-6"/></>,
    play: <><circle cx="12" cy="12" r="9"/><path d="m10 8 6 4-6 4V8Z"/></>,
    phone: <path d="M7 3H5a2 2 0 0 0-2 2c0 8.8 7.2 16 16 16a2 2 0 0 0 2-2v-2l-5-2-2 3a14 14 0 0 1-6-6l3-2-2-5H7Z"/>,
    close: <path d="m6 6 12 12M18 6 6 18"/>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">{shapes[name] || shapes.box}</svg>
}

function AssignedOrdersPanel({ assignments, online, busy, deliveredToday, earningsToday, onRefresh, onAccept, onStatus }) {
  const navigate = useNavigate()
  const [stage, setStage] = useState('NEW')
  const [query, setQuery] = useState('')
  const [area, setArea] = useState('')
  const [payment, setPayment] = useState('')
  const [range, setRange] = useState('ALL')
  const [page, setPage] = useState(0)
  const pagesize = 3
  const areaOptions = [...new Set(assignments.map((a) => a.dropAddress.split(',').slice(-3, -2)[0]?.trim()).filter(Boolean))]
  const preDelivery = (a) => ['PLACED', 'CONFIRMED', 'PROCESSING', 'SHIPPED'].includes(a.status)
  const stages = [
    ['NEW', 'New Assignments', assignments.filter((a) => !a.acceptedAt && preDelivery(a)).length],
    ['ACCEPTED', 'Accepted', assignments.filter((a) => a.acceptedAt && preDelivery(a)).length],
    ['ROUTE', 'Out for Delivery', assignments.filter((a) => a.status === 'OUT_FOR_DELIVERY').length],
    ['DONE', 'Completed', assignments.filter((a) => a.status === 'DELIVERED').length],
    ['CANCELLED', 'Cancelled', assignments.filter((a) => a.status === 'CANCELLED').length],
  ]
  const filtered = assignments.filter((a) => {
    const stageMatch = stage === 'NEW' ? !a.acceptedAt && preDelivery(a) : stage === 'ACCEPTED' ? a.acceptedAt && preDelivery(a) : stage === 'ROUTE' ? a.status === 'OUT_FOR_DELIVERY' : stage === 'CANCELLED' ? a.status === 'CANCELLED' : a.status === 'DELIVERED'
    const textMatch = `${a.orderId} ${a.sellerOrderId} ${a.customerName} ${a.customerPhone} ${a.pickupName} ${a.dropAddress}`.toLowerCase().includes(query.trim().toLowerCase())
    const areaMatch = !area || a.dropAddress.toLowerCase().includes(area.toLowerCase())
    const paymentMatch = !payment || a.paymentMethod === payment
    const age = Date.now() - new Date(a.createdAt).getTime()
    const dateMatch = range === 'ALL' || (range === 'TODAY' ? new Date(a.createdAt).toDateString() === new Date().toDateString() : age <= Number(range) * 86400000)
    return stageMatch && textMatch && areaMatch && paymentMatch && dateMatch
  })
  const pages = Math.max(1, Math.ceil(filtered.length / pagesize))
  const current = filtered.slice(page * pagesize, (page + 1) * pagesize)
  function reset() { setQuery(''); setArea(''); setPayment(''); setRange('ALL'); setPage(0) }
  function switchStage(next) { setStage(next); setPage(0) }
  return <section className="assigned-page">
    <header className="assigned-page-heading"><div className="assigned-page-tools"><span className={`assigned-online ${online ? 'is-online' : ''}`}><i/>{online ? 'You are Online' : 'You are Offline'}</span><button className="assigned-refresh" disabled={busy} onClick={onRefresh} type="button"><CourierIcon name="history" size={17}/> Refresh</button></div></header>
    <div className="assigned-summary"><article><span className="assigned-icon purple"><CourierIcon name="box"/></span><div><small>Available Orders</small><strong>{stages[0][2]}</strong><em>Ready to accept</em></div></article><article><span className="assigned-icon green"><CourierIcon name="check"/></span><div><small>Your Active Orders</small><strong>{stages[1][2] + stages[2][2]}</strong><em>Accepted and in progress</em></div></article><article><span className="assigned-icon orange"><CourierIcon name="history"/></span><div><small>Delivered Today</small><strong>{deliveredToday}</strong><em>Completed successfully</em></div></article><article><span className="assigned-icon mint"><CourierIcon name="rupee"/></span><div><small>Today's Earnings</small><strong>{money(earningsToday)}</strong><em>Completed delivery fees</em></div></article></div>
    <nav className="assigned-stages">{stages.map(([id, label, count]) => <button className={stage === id ? 'selected' : ''} key={id} onClick={() => switchStage(id)} type="button"><CourierIcon name={id === 'NEW' ? 'box' : id === 'ACCEPTED' ? 'check' : id === 'ROUTE' ? 'truck' : 'history'} size={17}/>{label}<b>{count}</b></button>)}</nav>
    <div className="assigned-filters"><label><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></svg><input onChange={(e) => { setQuery(e.target.value); setPage(0) }} placeholder="Search order, tracking ID or customer..." value={query}/></label><select aria-label="Area" onChange={(e) => { setArea(e.target.value); setPage(0) }} value={area}><option value="">All Areas</option>{areaOptions.map((v) => <option key={v}>{v}</option>)}</select><select aria-label="Payment method" onChange={(e) => { setPayment(e.target.value); setPage(0) }} value={payment}><option value="">All Payment Methods</option><option value="COD">Cash on delivery</option><option value="ONLINE">Online</option></select><select aria-label="Date range" onChange={(e) => { setRange(e.target.value); setPage(0) }} value={range}><option value="ALL">All time</option><option value="TODAY">Today</option><option value="7">Last 7 days</option><option value="30">Last 30 days</option></select><button className="assigned-reset" onClick={reset} type="button"><CourierIcon name="history" size={16}/> Reset</button></div>
    <div className="assigned-cards">{current.map((a) => <article className="assigned-card" key={a.assignmentId}><span className="assigned-card-mark"><CourierIcon name="truck" size={27}/></span><div className="assigned-order-meta"><div><h3>Order #{a.orderId}</h3><span className={`assigned-state ${!a.acceptedAt && preDelivery(a) ? 'new' : a.status.toLowerCase()}`}>{a.status === 'CANCELLED' ? 'Cancelled' : !a.acceptedAt && preDelivery(a) ? 'New Assignment' : a.status === 'SHIPPED' ? 'Ready for delivery' : a.status === 'PROCESSING' ? 'Preparing' : a.status === 'CONFIRMED' || a.status === 'PLACED' ? 'Awaiting seller' : a.status.replaceAll('_', ' ')}</span></div><p><CourierIcon name="clock" size={15}/> Assigned {new Date(a.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</p><p><CourierIcon name="user" size={15}/>{a.customerName}{a.customerPhone && <a href={`tel:${a.customerPhone}`}><CourierIcon name="phone" size={14}/>{a.customerPhone}</a>}</p></div><div className="assigned-addresses"><p><CourierIcon name="pin" size={17}/><span><strong>Pickup: {a.pickupName}</strong><small>{a.pickupAddress}</small></span></p><p><CourierIcon name="pin" size={17}/><span><strong>Drop: Customer</strong><small>{a.dropAddress}</small></span></p></div><div className="assigned-earning"><small>Delivery fee</small><strong>{money(a.earning)}</strong><em>{a.paymentMethod === 'COD' ? 'Cash on delivery' : 'Paid online'}</em></div><div className="assigned-actions">{!a.acceptedAt && preDelivery(a) ? <button className="accept-order" disabled={busy || !online} onClick={() => onAccept(a)} type="button"><CourierIcon name="check" size={16}/> Accept Order</button> : a.acceptedAt && a.status === 'SHIPPED' ? <button className="accept-order" disabled={busy} onClick={() => onStatus(a, 'OUT_FOR_DELIVERY')} type="button"><CourierIcon name="play" size={16}/> Start Delivery</button> : a.status === 'OUT_FOR_DELIVERY' ? <button className="accept-order" disabled={busy} onClick={() => onStatus(a, 'DELIVERED')} type="button"><CourierIcon name="check" size={16}/> Mark Delivered</button> : a.status === 'DELIVERED' ? <span className="completed-label"><CourierIcon name="check" size={16}/> Delivered</span> : <span className="completed-label cancelled-label">{a.status === 'CANCELLED' ? 'Cancelled' : 'Waiting for seller'}</span>}<button className="view-order" onClick={() => navigate(`/delivery/orders/${a.assignmentId}`, { state: { initialTab: "Assigned Orders" } })} type="button"><CourierIcon name="user" size={16}/> View Details</button></div></article>)}{current.length === 0 && <div className="assigned-empty"><CourierIcon name="box" size={30}/><strong>{stage === 'NEW' && !online ? 'Go online to receive available orders' : 'No orders match these filters'}</strong><span>{stage === 'NEW' ? 'New shipments for your service area will appear here.' : 'Try another status tab or reset your filters.'}</span></div>}</div>
    <footer className="assigned-pagination"><span>Showing {filtered.length ? page * pagesize + 1 : 0}-{Math.min((page + 1) * pagesize, filtered.length)} of {filtered.length} orders</span><div><button disabled={page === 0} onClick={() => setPage(page - 1)} type="button">‹</button><b>{page + 1} / {pages}</b><button disabled={page + 1 >= pages} onClick={() => setPage(page + 1)} type="button">›</button></div></footer>

  </section>
}

export default function DeliveryDashboard() {
  const dispatch = useDispatch()
  const currentUser = useSelector((state) => state.auth.user)
  const [data, setData] = useState(null)
  const location = useLocation()
  const [tab, setTab] = useState(location.state?.initialTab || tabs[0])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [locationBusy, setLocationBusy] = useState(false)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { setData(await getDeliveryDashboard()) }
    catch (e) { setError(getRequestError(e, 'Could not load your delivery dashboard.')) }
    finally { setLoading(false) }
  }, [])
  useEffect(() => { load() }, [load])
  useEffect(() => {
    if (!data?.online || !['Dashboard', 'Assigned Orders'].includes(tab)) return undefined
    const timer = window.setInterval(async () => {
      try { setData(await getDeliveryDashboard()) } catch { /* Keep the current list visible during temporary network issues. */ }
    }, 5000)
    return () => window.clearInterval(timer)
  }, [data?.online, tab])
  const assignments = data?.assignments || []
  const active = useMemo(() => assignments.filter((a) => ['PLACED', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'OUT_FOR_DELIVERY'].includes(a.status)), [assignments])
  const completed = useMemo(() => assignments.filter((a) => a.status === 'DELIVERED'), [assignments])
  const visible = tab === 'Assigned Orders' ? active : tab === 'Delivery History' ? completed : assignments
  const filtered = visible.filter((a) => `${a.orderId} ${a.customerName} ${a.pickupName} ${a.dropAddress}`.toLowerCase().includes(search.toLowerCase()))

  async function toggleOnline() {
    setBusy(true); setError('')
    try { await setDeliveryAvailability(!data.online); await load() }
    catch (e) { setError(getRequestError(e, 'Could not update availability. Your profile may need admin approval.')) }
    finally { setBusy(false) }
  }
  async function updateStatus(assignment, status) {
    setBusy(true); setError('')
    try { await updateDeliveryAssignmentStatus(assignment.assignmentId, status); await load() }
    catch (e) { setError(getRequestError(e, 'Could not update this delivery.')) }
    finally { setBusy(false) }
  }

  async function acceptAssignment(assignment) {
    setBusy(true); setError('')
    try { await acceptDeliveryAssignment(assignment.assignmentId); await load() }
    catch (e) { setError(getRequestError(e, 'Could not accept this delivery.')) }
    finally { setBusy(false) }
  }

  function shareLocation() {
    if (!navigator.geolocation) { setError('This browser does not support location sharing.'); return }
    setLocationBusy(true); setError('')
    navigator.geolocation.getCurrentPosition(async ({ coords }) => {
      try { await updateDeliveryLocation(coords.latitude, coords.longitude); await load() }
      catch (e) { setError(getRequestError(e, 'Could not update your location.')) }
      finally { setLocationBusy(false) }
    }, (e) => { setError(e.code === 1 ? 'Allow location access in your browser to share your location.' : 'Could not read your current location.'); setLocationBusy(false) }, { enableHighAccuracy: true, timeout: 12000, maximumAge: 60000 })
  }

  const heading = tab === 'Dashboard' ? `Good ${new Date().getHours() < 12 ? 'morning' : 'afternoon'}, ${(data?.fullName || currentUser?.fullName || 'Partner').split(' ')[0]}!` : tab
  return <div className="delivery-shell">
    <aside className="delivery-sidebar"><a className="delivery-brand" href="/"><CourierIcon name="truck" size={28}/> <span>ShopSphere</span></a><small className="delivery-side-label">COURIER PANEL</small><nav>{tabs.map((item, i) => <button className={tab === item ? 'active' : ''} key={item} onClick={() => { setTab(item); setSearch('') }} type="button"><span><CourierIcon name={['dashboard', 'box', 'history', 'rupee', 'user'][i]}/></span>{item}{item === 'Assigned Orders' && active.length > 0 && <b>{active.length}</b>}</button>)}</nav><div className="delivery-online-card"><i className={data?.online ? 'on' : ''}/><div><strong>{data?.online ? 'Online' : 'Offline'}</strong><small>{data?.online ? 'Available for deliveries' : 'Go online to receive orders'}</small></div><button aria-label="Toggle online availability" disabled={busy || loading} onClick={toggleOnline} type="button"><i className={data?.online ? 'on' : ''}/></button></div><button className="delivery-side-account" onClick={() => setTab('Profile')} type="button"><span>{(data?.fullName || currentUser?.fullName || 'DP').split(/\s+/).map((x) => x[0]).join('').slice(0, 2)}</span><div><strong>{data?.fullName || currentUser?.fullName || 'Delivery Partner'}</strong><small>Delivery Partner</small></div></button></aside>
    <main className="delivery-main"><header className="delivery-topbar"><span>Dashboard <CourierIcon name="chevron" size={15}/> {tab}</span><div><span>{data?.fullName || currentUser?.fullName}</span><button onClick={() => dispatch(logout())} type="button"><CourierIcon name="logout" size={16}/> Sign out</button></div></header><section className="delivery-content">
      <div className="delivery-page-heading"><div><p>DELIVERY PARTNER PANEL</p><h1>{heading}</h1><span>{tab === 'Dashboard' ? "Here's your delivery overview for today." : `Manage your ${tab.toLowerCase()} and partner account.`}</span></div><button className={`delivery-online-button ${data?.online ? 'online' : ''}`} disabled={busy || loading} onClick={toggleOnline} type="button"><i/>{data?.online ? 'Go Offline' : 'Go Online'}</button></div>
      {error && <div className="delivery-alert" role="alert">{error}<button onClick={load} type="button">Retry</button></div>}
      {loading ? <div className="delivery-loading">Loading your courier dashboard…</div> : data && <>
        {data.approvalStatus !== 'APPROVED' && <div className="delivery-approval"><strong>Partner approval: {data.approvalStatus}</strong><span>Your account is ready. Admin approval is needed before you can go online and receive deliveries.</span></div>}
        {(tab === 'Dashboard' || tab === 'Earnings') && <section className="delivery-kpis"><article><span className="mint"><CourierIcon name="box" size={23}/></span><div><small>Assigned Today</small><strong>{data.assignedToday}</strong></div></article><article><span className="blue"><CourierIcon name="check" size={23}/></span><div><small>Delivered Today</small><strong>{data.deliveredToday}</strong></div></article><article><span className="orange"><CourierIcon name="clock" size={23}/></span><div><small>Pending Deliveries</small><strong>{data.pendingDeliveries}</strong></div></article><article><span className="purple"><CourierIcon name="rupee" size={23}/></span><div><small>{tab === 'Earnings' ? 'Total Earnings' : "Today's Earnings"}</small><strong>{money(tab === 'Earnings' ? data.earningsTotal : data.earningsToday)}</strong></div></article></section>}
        {tab === 'Earnings' && <p className="delivery-earnings-note">Earnings are estimated from delivery fees on completed shipments. Cash-out and payout processing are not configured yet.</p>}
        {tab === 'Profile' ? <section className="delivery-panel delivery-profile"><h2>Partner Profile</h2><div><small>Full name</small><strong>{data.fullName}</strong></div><div><small>Email</small><strong>{data.email}</strong></div><div><small>Phone</small><strong>{data.phone || 'Not provided'}</strong></div><div><small>Vehicle</small><strong>{data.vehicleType}</strong></div><div><small>Service city</small><strong>{data.city || 'Not set'}</strong></div><div><small>Approval status</small><strong>{data.approvalStatus}</strong></div></section> : tab === 'Assigned Orders' ? <AssignedOrdersPanel assignments={assignments} online={data.online} busy={busy} deliveredToday={data.deliveredToday} earningsToday={data.earningsToday} onRefresh={load} onAccept={acceptAssignment} onStatus={updateStatus}/> : <>
          <div className="delivery-work-grid"><section className="delivery-panel delivery-route"><header><div><h2><CourierIcon name="pin"/> {tab === 'Dashboard' ? 'Current Location' : 'Your Delivery Route'}</h2><p>{data.city || 'Set your service city in your profile'}</p></div><button disabled={!data.online || locationBusy} onClick={shareLocation} type="button">{locationBusy ? 'Updating…' : <><CourierIcon name="pin" size={16}/> Update Location</>}</button></header>{data.latitude != null && data.longitude != null ? <><iframe title="Courier current location" loading="lazy" src={`https://www.openstreetmap.org/export/embed.html?bbox=${data.longitude - .015}%2C${data.latitude - .01}%2C${data.longitude + .015}%2C${data.latitude + .01}&layer=mapnik&marker=${data.latitude}%2C${data.longitude}`}/><p className="delivery-route-caption">Location last shared {data.locationUpdatedAt ? new Date(data.locationUpdatedAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : 'recently'}. Browser location permission is required.</p></> : <div className="delivery-no-map"><span><CourierIcon name="pin" size={32}/></span><strong>Share your current location</strong><small>Go online, then allow browser location access to show your position.</small></div>}<button className="delivery-route-link" onClick={() => setTab('Assigned Orders')} type="button">View assigned orders <CourierIcon name="chevron" size={14}/></button></section><section className="delivery-panel delivery-breakdown"><h2><CourierIcon name="history"/> Delivery Status</h2><div className="delivery-donut"><strong>{assignments.length}<small>Total Orders</small></strong></div><p><i className="dot green"/>Delivered <b>{completed.length}</b></p><p><i className="dot orange"/>On route <b>{active.filter((a) => a.status === 'OUT_FOR_DELIVERY').length}</b></p><p><i className="dot blue"/>Assigned <b>{active.filter((a) => a.status === 'SHIPPED').length}</b></p></section></div>
          <section className="delivery-panel delivery-orders"><header><div><h2><CourierIcon name="box"/> {tab === 'Dashboard' ? 'Recent Orders' : tab}</h2><p>Shipment status and delivery details</p></div><label className="delivery-search"><span><svg aria-hidden="true" viewBox="0 0 24 24"><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></svg></span><input aria-label="Search orders" onChange={(e) => setSearch(e.target.value)} placeholder="Search orders or customers" value={search}/></label></header><div className="delivery-table-wrap"><table><thead><tr><th>Order</th><th>Customer</th><th>Pickup</th><th>Drop Location</th><th>Status</th><th>Earning</th><th>Action</th></tr></thead><tbody>{filtered.slice(0, tab === 'Dashboard' ? 5 : 100).map((a) => <tr key={a.assignmentId}><td><strong>#{a.orderId}</strong><small>Shipment #{a.sellerOrderId}</small></td><td>{a.customerName}<small>{a.customerPhone && <a href={`tel:${a.customerPhone}`}>{a.customerPhone}</a>}</small></td><td>{a.pickupName}<small>{a.pickupAddress}</small></td><td>{a.dropAddress}</td><td><span className={`delivery-status ${a.status.toLowerCase()}`}>{a.status.replaceAll('_', ' ')}</span></td><td>{money(a.earning)}</td><td>{!a.acceptedAt && ['PLACED', 'CONFIRMED', 'PROCESSING', 'SHIPPED'].includes(a.status) ? <button disabled={busy || !data.online} onClick={() => acceptAssignment(a)} type="button"><CourierIcon name="check" size={15}/> Accept</button> : a.acceptedAt && a.status === 'SHIPPED' ? <button disabled={busy} onClick={() => updateStatus(a, 'OUT_FOR_DELIVERY')} type="button"><CourierIcon name="play" size={15}/> Start route</button> : a.status === 'OUT_FOR_DELIVERY' ? <button disabled={busy} onClick={() => updateStatus(a, 'DELIVERED')} type="button"><CourierIcon name="check" size={15}/> Mark delivered</button> : a.acceptedAt && ['PLACED', 'CONFIRMED', 'PROCESSING'].includes(a.status) ? 'Waiting for seller' : '—'}</td></tr>)}{filtered.length === 0 && <tr><td className="delivery-empty" colSpan="7">{tab === 'Assigned Orders' ? 'No active deliveries assigned yet. Go online to receive available shipments.' : 'No deliveries to show yet.'}</td></tr>}</tbody></table></div></section>
        </>}
      </>}
    </section></main>
  </div>
}
