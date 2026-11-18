import { useEffect, useMemo, useState } from 'react'
import { AnimatePresence, LazyMotion, MotionConfig, domAnimation, m } from 'framer-motion'
import { Link, useNavigate } from 'react-router-dom'
import MainNavbar from '../../components/navbar/MainNavbar'
import CustomerAccountSidebar, { DashboardIcon } from '../../components/customer/CustomerAccountSidebar'
import { formatProductPrice } from '../../components/product/ProductCard'
import { getRequestError } from '../../services/authService'
import { cancelCustomerReturnRequest, createCustomerReturnRequest, getCustomerReturnRequests, getEligibleReturnItems } from '../../services/returnService'
import './Shop.css'
import './CustomerDashboard.css'
import './CustomerReturns.css'

const TABS = [
  { id: 'ALL', label: 'All Requests' },
  { id: 'RETURN', label: 'Return' },
  { id: 'REPLACEMENT', label: 'Replacement' },
  { id: 'REFUND', label: 'Refund' },
]
const REASONS = ['Damaged product', 'Defective item', 'Wrong item received', 'Wrong size or fit', 'Not as described', 'Changed my mind', 'Other']
const OPEN = new Set(['REQUESTED', 'APPROVED', 'PICKUP_SCHEDULED', 'QUALITY_CHECK', 'REFUND_PROCESSING', 'REPLACEMENT_APPROVED'])
const COMPLETED = new Set(['COMPLETED', 'REFUND_COMPLETED'])

function dateLabel(value, options = { month: 'short', day: 'numeric', year: 'numeric' }) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString('en-IN', options)
}

function typeLabel(type) { return type === 'REPLACEMENT' ? 'Replacement' : type === 'REFUND' ? 'Refund' : 'Return' }
function statusLabel(status) {
  return ({ REQUESTED: 'Request Submitted', APPROVED: 'Approved', PICKUP_SCHEDULED: 'Pickup Scheduled', QUALITY_CHECK: 'Quality Check', REFUND_PROCESSING: 'Refund Processing', REFUND_COMPLETED: 'Refund Completed', REPLACEMENT_APPROVED: 'Replacement Approved', COMPLETED: 'Completed', REJECTED: 'Rejected', CANCELLED: 'Cancelled' })[status] || status
}
function statusTone(status) {
  if (['COMPLETED', 'REFUND_COMPLETED', 'REPLACEMENT_APPROVED', 'APPROVED'].includes(status)) return 'success'
  if (['REJECTED', 'CANCELLED'].includes(status)) return 'danger'
  if (status === 'REFUND_PROCESSING') return 'warning'
  return 'active'
}
function timelineFor(request) {
  if (request.requestType === 'REFUND') return ['REQUESTED', 'APPROVED', 'REFUND_PROCESSING', 'REFUND_COMPLETED']
  if (request.requestType === 'REPLACEMENT') return ['REQUESTED', 'APPROVED', 'PICKUP_SCHEDULED', 'QUALITY_CHECK', 'REPLACEMENT_APPROVED', 'COMPLETED']
  return ['REQUESTED', 'APPROVED', 'PICKUP_SCHEDULED', 'QUALITY_CHECK', 'REFUND_PROCESSING', 'REFUND_COMPLETED']
}

function ProductThumb({ src, name }) {
  const [failed, setFailed] = useState(false)
  return src && !failed ? <img src={src} alt={name} onError={() => setFailed(true)} loading="lazy"/> : <span className="return-product-placeholder">{name?.slice(0, 1) || 'S'}</span>
}

function RequestForm({ eligibleItems, loading, submitting, error, onClose, onSubmit }) {
  const [itemId, setItemId] = useState(eligibleItems[0]?.orderItemId ? String(eligibleItems[0].orderItemId) : '')
  const [requestType, setRequestType] = useState('RETURN')
  const [reason, setReason] = useState(REASONS[0])
  const [description, setDescription] = useState('')
  useEffect(() => {
    if (!itemId && eligibleItems.length) setItemId(String(eligibleItems[0].orderItemId))
  }, [eligibleItems, itemId])
  const selectedItem = eligibleItems.find((item) => String(item.orderItemId) === itemId)
  function submit(event) {
    event.preventDefault()
    if (!itemId || !reason) return
    onSubmit({ orderItemId: Number(itemId), requestType, reason, description: description.trim() || null })
  }
  return <div className="returns-backdrop" onClick={submitting ? undefined : onClose}>
    <m.section className="returns-form-modal" role="dialog" aria-modal="true" aria-labelledby="returns-form-title" onClick={(event) => event.stopPropagation()} initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 10 }}>
      <header><div><span className="returns-eyebrow">CUSTOMER CARE</span><h2 id="returns-form-title">Request a return</h2><p>Choose a delivered item and tell us how we can help.</p></div><button type="button" onClick={onClose} disabled={submitting} aria-label="Close">×</button></header>
      {loading ? <div className="returns-modal-loading">Loading eligible items…</div> : !eligibleItems.length ? <div className="returns-no-eligible"><strong>No eligible items found</strong><p>Items can be requested within 7 days after delivery. Check your orders for delivery updates.</p><Link to="/orders" onClick={onClose}>View My Orders</Link></div> : <form onSubmit={submit}>
        <label className="returns-field"><span>Product</span><select value={itemId} onChange={(event) => setItemId(event.target.value)} required>{eligibleItems.map((item) => <option key={item.orderItemId} value={item.orderItemId}>{item.productName} · Order #{item.orderId}</option>)}</select></label>
        {selectedItem && <div className="returns-selected-item"><ProductThumb src={selectedItem.imageUrl} name={selectedItem.productName}/><div><strong>{selectedItem.productName}</strong><span>Order #{selectedItem.orderId} · Qty {selectedItem.quantity}</span><small>Return by {dateLabel(selectedItem.eligibleUntil)}</small></div><b>{formatProductPrice(selectedItem.amount)}</b></div>}
        <label className="returns-field"><span>Request type</span><select value={requestType} onChange={(event) => setRequestType(event.target.value)}><option value="RETURN">Return</option><option value="REPLACEMENT">Replacement</option><option value="REFUND">Refund</option></select></label>
        <label className="returns-field"><span>Reason</span><select value={reason} onChange={(event) => setReason(event.target.value)}>{REASONS.map((item) => <option key={item}>{item}</option>)}</select></label>
        <label className="returns-field"><span>More details <em>Optional</em></span><textarea maxLength={1000} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Add details that can help us process your request."/></label>
        {error && <p className="returns-form-error" role="alert">{error}</p>}
        <div className="returns-modal-actions"><button type="button" className="returns-cancel-button" disabled={submitting} onClick={onClose}>Cancel</button><button type="submit" className="returns-submit-button" disabled={!itemId || submitting}>{submitting ? 'Submitting…' : 'Submit Request'}</button></div>
      </form>}
    </m.section>
  </div>
}

function RequestDetails({ request, busy, onClose, onCancel }) {
  const timeline = timelineFor(request)
  const currentIndex = timeline.indexOf(request.status)
  const terminal = ['REJECTED', 'CANCELLED'].includes(request.status)
  return <div className="returns-backdrop" onClick={onClose}><m.section className="returns-details-modal" role="dialog" aria-modal="true" aria-labelledby="return-detail-title" onClick={(event) => event.stopPropagation()} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 10 }}>
    <header><div><span className="returns-eyebrow">REQUEST #{request.id}</span><h2 id="return-detail-title">{request.productName}</h2><p>Order #{request.orderId} · {typeLabel(request.requestType)}</p></div><button type="button" onClick={onClose} aria-label="Close">×</button></header>
    <div className="returns-detail-summary"><span className="returns-status-pill is-active">{statusLabel(request.status)}</span><strong>{formatProductPrice(request.itemAmount)}</strong></div>
    <div className="returns-detail-meta"><div><small>Request date</small><strong>{dateLabel(request.requestedAt)}</strong></div><div><small>Reason</small><strong>{request.reason}</strong></div><div><small>Sold by</small><strong>{request.sellerName}</strong></div></div>
    {request.description && <p className="returns-detail-description">{request.description}</p>}
    {!terminal && <div className="returns-detail-timeline"><h3>Request progress</h3><div className="returns-timeline-list">{timeline.map((status, index) => {
      const done = currentIndex >= index
      const event = request.history?.find((item) => item.status === status)
      return <div className={`returns-timeline-event${done ? ' is-done' : ''}${status === request.status ? ' is-current' : ''}`} key={status}><span className="returns-timeline-mark">{done ? <DashboardIcon name="check"/> : ''}</span><div><strong>{statusLabel(status)}</strong><small>{event ? dateLabel(event.changedAt, { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : 'Waiting for update'}</small>{event?.note && <p>{event.note}</p>}</div></div>
    })}</div></div>}
    {terminal && <div className={`returns-terminal-note is-${request.status.toLowerCase()}`}>{request.resolutionNote || statusLabel(request.status)}</div>}
    {request.expectedPickupAt && <p className="returns-pickup-note">Expected pickup: <strong>{dateLabel(request.expectedPickupAt)}</strong></p>}
    <footer>{request.status === 'REQUESTED' && <button type="button" className="returns-danger-button" disabled={busy} onClick={() => onCancel(request.id)}>{busy ? 'Cancelling…' : 'Cancel Request'}</button>}<button type="button" className="returns-submit-button" onClick={onClose}>Done</button></footer>
  </m.section></div>
}

function RequestCard({ request, onOpen }) {
  const flow = timelineFor(request)
  const activeIndex = flow.indexOf(request.status)
  const timeline = request.status === 'REJECTED' || request.status === 'CANCELLED' ? [] : flow
  return <article className="returns-request-card">
    <div className="returns-request-main"><div className="returns-product-image"><ProductThumb src={request.imageUrl} name={request.productName}/></div><div className="returns-product-copy"><strong>{request.productName}</strong><span>Sold by: {request.sellerName}</span><small>Order ID: #SS{String(request.orderId).padStart(8, '0')}</small></div></div>
    <div className="returns-request-fact"><small>Request Date</small><strong>{dateLabel(request.requestedAt)}</strong></div>
    <div className="returns-request-fact"><small>Request Type</small><strong>{typeLabel(request.requestType)}</strong></div>
    <div className="returns-request-fact returns-reason-fact"><small>Reason</small><strong>{request.reason}</strong></div>
    <div className="returns-request-status"><span className={`returns-status-pill is-${statusTone(request.status)}`}><DashboardIcon name={request.status === 'REFUND_PROCESSING' ? 'wallet' : request.status === 'COMPLETED' || request.status === 'REFUND_COMPLETED' ? 'check' : 'truck'}/>{statusLabel(request.status)}</span><small>{request.expectedPickupAt ? `Expected: ${dateLabel(request.expectedPickupAt)}` : request.status === 'REFUND_PROCESSING' ? 'Processing…' : 'Status updates by notification.'}</small></div>
    <div className="returns-request-price"><strong>{formatProductPrice(request.itemAmount)}</strong><button type="button" onClick={() => onOpen(request)}>View Details</button></div>
    {timeline.length > 0 && <div className="returns-card-timeline" style={{ '--returns-steps': timeline.length }} aria-label={`Progress: ${statusLabel(request.status)}`}>{timeline.map((status, index) => <div className={`returns-card-step${activeIndex >= index ? ' is-done' : ''}${activeIndex === index ? ' is-current' : ''}`} key={status}><span>{activeIndex > index ? <DashboardIcon name="check"/> : status === 'PICKUP_SCHEDULED' && activeIndex === index ? <DashboardIcon name="truck"/> : activeIndex === index ? '•' : ''}</span><small>{statusLabel(status)}</small></div>)}</div>}
    {timeline.length === 0 && <div className="returns-rejected-line">{request.resolutionNote || statusLabel(request.status)}</div>}
  </article>
}

export default function Returns() {
  const navigate = useNavigate()
  const [requests, setRequests] = useState([])
  const [eligibleItems, setEligibleItems] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [tab, setTab] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [productSearch, setProductSearch] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [eligibleLoading, setEligibleLoading] = useState(false)
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [selectedRequest, setSelectedRequest] = useState(null)
  const [workingId, setWorkingId] = useState(null)

  async function reload() {
    setStatus('loading'); setError('')
    try { setRequests(await getCustomerReturnRequests()); setStatus('succeeded') }
    catch (requestError) { setError(getRequestError(requestError, 'Return requests could not be loaded.')); setStatus('failed') }
  }
  useEffect(() => { reload() }, [])

  const counts = useMemo(() => ({
    active: requests.filter((item) => OPEN.has(item.status)).length,
    refund: requests.filter((item) => item.status === 'REFUND_PROCESSING').length,
    completed: requests.filter((item) => COMPLETED.has(item.status)).length,
  }), [requests])
  const filtered = useMemo(() => requests.filter((request) => {
    const typeMatch = tab === 'ALL' || request.requestType === tab
    const statusMatch = statusFilter === 'ALL' || request.status === statusFilter
    const query = search.trim().toLowerCase()
    const searchMatch = !query || [request.productName, request.sellerName, request.reason, String(request.orderId), String(request.id)].join(' ').toLowerCase().includes(query)
    return typeMatch && statusMatch && searchMatch
  }), [requests, tab, statusFilter, search])

  async function openForm() {
    setFormError(''); setEligibleLoading(true); setFormOpen(true)
    try { setEligibleItems(await getEligibleReturnItems()) }
    catch (requestError) { setFormError(getRequestError(requestError, 'Eligible items could not be loaded.')) }
    finally { setEligibleLoading(false) }
  }

  async function submitRequest(payload) {
    setSubmitting(true); setFormError(''); setNotice('')
    try {
      await createCustomerReturnRequest(payload)
      setFormOpen(false); setNotice('Your return request was submitted.')
      await reload()
    } catch (requestError) { setFormError(getRequestError(requestError, 'Your request could not be submitted.')) }
    finally { setSubmitting(false) }
  }

  async function cancelRequest(requestId) {
    setWorkingId(requestId)
    try {
      const updated = await cancelCustomerReturnRequest(requestId)
      setSelectedRequest(updated); setNotice('Request cancelled.')
      await reload()
    } catch (requestError) { setNotice(getRequestError(requestError, 'Request could not be cancelled.')) }
    finally { setWorkingId(null) }
  }

  return <LazyMotion features={domAnimation}><MotionConfig reducedMotion="user"><div className="marketplace-page shop-page customer-dashboard-page customer-returns-page">
    <MainNavbar shopVariant onSearchChange={setProductSearch} onSearchSubmit={(event) => { event.preventDefault(); navigate(`/shop?search=${encodeURIComponent(productSearch)}`) }} searchValue={productSearch}/>
    <nav className="shop-secondary-nav" aria-label="Shop navigation"><div className="shop-secondary-inner"><Link to="/shop"><DashboardIcon name="grid"/>All Categories <span>⌄</span></Link><Link to="/shop?discountedOnly=true">Deals</Link><Link to="/shop?sort=newest">New Arrivals</Link><Link to="/shop?sort=popular">Best Sellers</Link><Link to="/orders">Track Order</Link><a href="mailto:support@shopsphere.com">Customer Support</a></div></nav>
    <main className="customer-returns-shell"><nav className="customer-returns-breadcrumb" aria-label="Breadcrumb"><Link to="/shop">Home</Link><span>›</span><Link to="/orders">My Account</Link><span>›</span><span aria-current="page">Returns &amp; Refunds</span></nav>
      <div className="customer-returns-layout"><CustomerAccountSidebar active="Returns & Refunds"/>
        <section className="customer-returns-main">
          <header className="customer-returns-heading"><div><h1>Returns &amp; Refunds</h1><p>Manage your return, replacement and refund requests</p></div></header>
          <div className="returns-overview-cards"><button type="button" onClick={() => { setTab('ALL'); setStatusFilter('ALL') }}><span className="returns-overview-icon is-blue"><DashboardIcon name="return"/></span><span><small>Active Returns</small><strong>{counts.active}</strong></span><DashboardIcon name="arrow"/></button><button type="button" onClick={() => { setTab('REFUND'); setStatusFilter('REFUND_PROCESSING') }}><span className="returns-overview-icon is-purple">₹</span><span><small>Refunds In Progress</small><strong>{counts.refund}</strong></span><DashboardIcon name="arrow"/></button><button type="button" onClick={() => { setTab('ALL'); setStatusFilter('ALL') }}><span className="returns-overview-icon is-green"><DashboardIcon name="check"/></span><span><small>Completed Returns</small><strong>{counts.completed}</strong></span><DashboardIcon name="arrow"/></button></div>
          <div className="returns-toolbar"><div className="returns-tabs" role="tablist" aria-label="Request type">{TABS.map((item) => <button className={tab === item.id ? 'is-active' : ''} role="tab" aria-selected={tab === item.id} type="button" key={item.id} onClick={() => setTab(item.id)}>{item.id === 'ALL' ? <DashboardIcon name="grid"/> : item.id === 'RETURN' ? <DashboardIcon name="return"/> : item.id === 'REPLACEMENT' ? <DashboardIcon name="repeat"/> : <DashboardIcon name="wallet"/>}{item.label} ({item.id === 'ALL' ? requests.length : requests.filter((request) => request.requestType === item.id).length})</button>)}</div><button className="returns-new-button" type="button" onClick={openForm}><DashboardIcon name="plus"/>Request a Return</button></div>
          <div className="returns-list-heading"><h2>Your Return &amp; Refund Requests</h2><div><label className="returns-search"><DashboardIcon name="search"/><input aria-label="Search requests" placeholder="Search requests..." value={search} onChange={(event) => setSearch(event.target.value)}/></label><select aria-label="Filter request status" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}><option value="ALL">All Status</option>{['REQUESTED','APPROVED','PICKUP_SCHEDULED','QUALITY_CHECK','REFUND_PROCESSING','REFUND_COMPLETED','REPLACEMENT_APPROVED','COMPLETED','REJECTED','CANCELLED'].map((item) => <option value={item} key={item}>{statusLabel(item)}</option>)}</select></div></div>
          {notice && <p className="returns-notice" role="status">{notice}</p>}
          {status === 'loading' ? <div className="returns-empty-state" role="status">Loading your requests…</div> : status === 'failed' ? <div className="returns-empty-state"><h3>Requests could not be loaded</h3><p>{error}</p><button type="button" onClick={reload}>Try again</button></div> : filtered.length === 0 ? <div className="returns-empty-state"><span><DashboardIcon name="return"/></span><h3>{requests.length ? 'No matching requests' : 'No return requests yet'}</h3><p>{requests.length ? 'Try changing your search or filters.' : 'Delivered items eligible for return can be requested within 7 days.'}</p>{!requests.length && <button type="button" onClick={openForm}>Request a Return</button>}</div> : <div className="returns-request-list">{filtered.map((request) => <RequestCard key={request.id} request={request} onOpen={setSelectedRequest}/>)}</div>}
        </section>
        <aside className="returns-help-column"><section className="returns-policy-card"><h2><span><DashboardIcon name="check"/></span>Return Policy</h2><ul><li>Eligible products can be returned within 7 days of delivery.</li><li>Keep original packaging and accessories.</li><li>Refund goes to original payment method or wallet.</li><li>Status updates appear in your request timeline.</li></ul></section><section className="returns-support-card"><h2><DashboardIcon name="support"/>Need Help?</h2><p>Our support team is here to help with your returns &amp; refunds.</p><a href="mailto:support@shopsphere.com"><DashboardIcon name="phone"/>Contact Support</a></section></aside>
      </div>
    </main>
    <AnimatePresence>{formOpen && <RequestForm key="new-return-request" eligibleItems={eligibleItems} loading={eligibleLoading} submitting={submitting} error={formError} onClose={() => setFormOpen(false)} onSubmit={submitRequest}/>}</AnimatePresence>
    <AnimatePresence>{selectedRequest && <RequestDetails key={selectedRequest.id} request={selectedRequest} busy={workingId === selectedRequest.id} onClose={() => setSelectedRequest(null)} onCancel={cancelRequest}/>}</AnimatePresence>
  </div></MotionConfig></LazyMotion>
}
