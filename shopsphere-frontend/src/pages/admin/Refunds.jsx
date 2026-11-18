import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import AdminLayout from '../../layouts/AdminLayout'
import { getRequestError } from '../../services/authService'
import { exportAdminRefunds, getAdminRefund, getAdminRefundStats, getAdminRefunds } from '../../services/adminRefundService'
import './AdminRefunds.css'

const STATUSES = ['PENDING', 'REFUNDED', 'FAILED']
const METHODS = ['ONLINE', 'COD']
const PAGE_SIZES = [10, 20, 50]

function Icon({ name, size = 19 }) {
  const paths = {
    refund: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.6 9a7 7 0 0 1 11.7-2L20 12M4 12l2.7 5a7 7 0 0 0 11.7-2"/></>,
    success: <><circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    fail: <><circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></>,
    reset: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.6 9a7 7 0 0 1 11.7-2L20 12M4 12l2.7 5a7 7 0 0 0 11.7-2"/></>,
    eye: <><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></>,
    close: <path d="m6 6 12 12M18 6 6 18"/>,
    download: <><path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4"/></>,
    order: <><path d="M4 4h16v16H4zM8 8h8M8 12h8M8 16h4"/></>,
    box: <><path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="M3 8v9l9 5 9-5V8M12 13v9"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

function money(value, currency = 'INR') { return new Intl.NumberFormat('en-IN', { style: 'currency', currency: currency || 'INR', maximumFractionDigits: 2 }).format(Number(value || 0)) }
function dateTime(value) { return value ? new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : 'Unavailable' }
function orderCode(id) { return `ORD${String(id ?? '').padStart(6, '0')}` }
function refundStatusLabel(status) { return ({ REFUNDED: 'Successful', PENDING: 'Pending', FAILED: 'Failed' })[status] || String(status || 'Unknown').replaceAll('_', ' ') }
function Status({ status }) { return <span className={`ar-status ar-status--${String(status || '').toLowerCase()}`}><i/>{refundStatusLabel(status)}</span> }
function Avatar({ name }) { return <span className="ar-avatar" aria-hidden="true">{(name || 'C').split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase()}</span> }

function Stat({ title, count, amount, icon, tone }) {
  return <article className={`ar-stat ar-stat--${tone}`}><span className="ar-stat-icon"><Icon name={icon} size={24}/></span><div><span>{title}</span><strong>{count == null ? '—' : Number(count).toLocaleString('en-IN')}</strong><small>{amount == null ? 'Amount unavailable' : money(amount)}</small></div></article>
}

function RefundModal({ refund, onClose, closeRef }) {
  useEffect(() => {
    closeRef.current?.focus()
    const onKey = (event) => { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [closeRef, onClose])
  return <div className="ar-overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }} role="presentation"><section aria-labelledby="ar-modal-title" aria-modal="true" className="ar-modal" role="dialog">
    <header><span className="ar-mark"><Icon name="refund"/></span><div><h2 id="ar-modal-title">Refund Details</h2><small>{refund.refundReference || `Payment transaction #${refund.paymentTransactionId}`}</small></div><button aria-label="Close refund details" className="ar-icon-btn" onClick={onClose} ref={closeRef} type="button"><Icon name="close"/></button></header>
    <dl className="ar-modal-grid"><div><dt>Order ID</dt><dd>{orderCode(refund.orderId)}</dd></div><div><dt>Customer</dt><dd>{refund.customerName || 'Unavailable'}</dd></div><div><dt>Customer email</dt><dd className="ar-wrap">{refund.customerEmail || 'Unavailable'}</dd></div><div><dt>Refund amount</dt><dd>{money(refund.refundAmount, refund.currency)}</dd></div><div><dt>Original order amount</dt><dd>{money(refund.orderAmount, refund.currency)}</dd></div><div><dt>Refund method</dt><dd>{refund.paymentMethod || 'Unavailable'}</dd></div><div><dt>Status</dt><dd><Status status={refund.status}/></dd></div><div><dt>Order date</dt><dd>{dateTime(refund.orderDate)}</dd></div><div className="ar-modal-wide"><dt>Gateway refund ID</dt><dd className="ar-wrap">{refund.refundReference || 'Not available'}</dd></div><div className="ar-modal-wide"><dt>Reason</dt><dd>{refund.reason || 'No cancellation reason recorded'}</dd></div><div className="ar-modal-wide"><dt>Admin notes</dt><dd>Not available in the current refund workflow.</dd></div></dl>
    <footer><button className="ar-button" onClick={onClose} type="button">Close</button><Link className="ar-button ar-button--primary" onClick={onClose} to={`/admin/orders/${refund.orderId}`}>View Order <span aria-hidden="true">→</span></Link></footer>
  </section></div>
}

function RefundDrawer({ refund, onClose }) {
  useEffect(() => { const onKey = (event) => { if (event.key === 'Escape') onClose() }; window.addEventListener('keydown', onKey); return () => window.removeEventListener('keydown', onKey) }, [onClose])
  return <div className="ar-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }} role="presentation"><aside aria-label="Refund details" aria-modal="true" className="ar-drawer" role="dialog">
    <header className="ar-drawer-head"><span className="ar-mark"><Icon name="refund"/></span><div><h2>Refund Details</h2><small>{refund.refundReference || `Transaction #${refund.paymentTransactionId}`}</small></div><button aria-label="Close refund details" className="ar-icon-btn" onClick={onClose} type="button"><Icon name="close"/></button></header>
    <div className="ar-scroll"><section className="ar-refund-summary"><Status status={refund.status}/><strong>{money(refund.refundAmount, refund.currency)}</strong><span>{refund.refundReference ? `Refund ID: ${refund.refundReference}` : 'Gateway refund reference unavailable'}</span><button className="ar-button" disabled title="The backend does not generate refund receipts" type="button"><Icon name="download"/>Receipt unavailable</button></section>
      <section className="ar-section"><h3><Icon name="order"/>Order Information</h3><div className="ar-order-items">{refund.items?.length ? refund.items.map((item, index) => <div key={`${item.sku}-${index}`}><span className="ar-product-icon"><Icon name="box"/></span><span><strong>{item.productName}</strong><small>SKU: {item.sku || 'Unavailable'} · Qty: {item.quantity}</small></span></div>) : <p>No order items available.</p>}</div><dl className="ar-facts"><div><dt>Customer</dt><dd>{refund.customerName || 'Unavailable'}<small>{refund.customerEmail || 'Email unavailable'}</small></dd></div><div><dt>Order ID</dt><dd>{orderCode(refund.orderId)}</dd></div><div><dt>Order Date</dt><dd>{dateTime(refund.orderDate)}</dd></div><div><dt>Order amount</dt><dd>{money(refund.orderAmount, refund.currency)}</dd></div></dl></section>
      <section className="ar-section"><h3><Icon name="clock"/>Refund Status</h3><div className="ar-timeline"><span className="ar-timeline-dot"/><div><strong>{refundStatusLabel(refund.status)}</strong><small>Latest order update: {dateTime(refund.lastUpdated)}</small></div></div><p className="ar-note">The current backend stores the current refund status but does not keep a timestamped refund event history.</p></section>
      <section className="ar-section"><h3><Icon name="refund"/>Additional Information</h3><dl className="ar-facts"><div><dt>Payment method</dt><dd>{refund.paymentMethod || 'Unavailable'}</dd></div><div><dt>Original transaction ID</dt><dd className="ar-wrap">{refund.transactionId || 'Unavailable'}</dd></div><div><dt>Gateway refund ID</dt><dd className="ar-wrap">{refund.refundReference || 'Unavailable'}</dd></div><div><dt>Reason</dt><dd>{refund.reason || 'Not recorded'}</dd></div></dl></section>
      {refund.status === 'PENDING' && <p className="ar-note ar-note--info">This refund was already initiated by the order cancellation workflow and is awaiting a gateway webhook. It cannot be approved a second time.</p>}
    </div><footer className="ar-drawer-foot"><Link className="ar-button ar-button--primary" onClick={onClose} to={`/admin/orders/${refund.orderId}`}>View Order Details <span aria-hidden="true">→</span></Link></footer>
  </aside></div>
}

export default function Refunds() {
  const [draft, setDraft] = useState({ search: '', status: '', method: '', from: '', to: '' })
  const [filters, setFilters] = useState({ search: '', status: '', method: '', from: '', to: '' })
  const [pageData, setPageData] = useState(null)
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [selected, setSelected] = useState(null)
  const [selectedRows, setSelectedRows] = useState([])
  const [detailLoading, setDetailLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [toast, setToast] = useState('')
  const closeRef = useRef(null)

  const query = useMemo(() => ({ ...(filters.search ? { search: filters.search } : {}), ...(filters.status ? { status: filters.status } : {}), ...(filters.method ? { method: filters.method } : {}), ...(filters.from ? { from: filters.from } : {}), ...(filters.to ? { to: filters.to } : {}) }), [filters])
  const params = useMemo(() => ({ ...query, page, size }), [query, page, size])
  const loadRefunds = useCallback(async () => { setLoading(true); setError(''); try { setPageData(await getAdminRefunds(params)) } catch (requestError) { setError(getRequestError(requestError, 'Could not load marketplace refunds.')) } finally { setLoading(false) } }, [params])
  useEffect(() => { loadRefunds() }, [loadRefunds])
  useEffect(() => { let active = true; getAdminRefundStats(query).then((value) => { if (active) setStats(value) }).catch((requestError) => { if (active) setToast(getRequestError(requestError, 'Refund statistics are unavailable.')) }); return () => { active = false } }, [query])
  useEffect(() => { if (!toast) return undefined; const timer = window.setTimeout(() => setToast(''), 4500); return () => window.clearTimeout(timer) }, [toast])

  function updateDraft(event) { setDraft((current) => ({ ...current, [event.target.name]: event.target.value })) }
  function applyFilters(event) { event.preventDefault(); if (draft.from && draft.to && draft.from > draft.to) { setError('Start date cannot be after end date.'); return } setPage(0); setSelectedRows([]); setFilters({ ...draft, search: draft.search.trim() }) }
  function resetFilters() { const empty = { search: '', status: '', method: '', from: '', to: '' }; setDraft(empty); setFilters(empty); setPage(0); setSelectedRows([]) }
  async function viewRefund(row, mode = 'modal') { setSelected({ ...row, mode }); setDetailLoading(true); try { const detail = await getAdminRefund(row.paymentTransactionId); setSelected({ ...detail, mode }) } catch (requestError) { setToast(getRequestError(requestError, 'Could not load refund details.')); setSelected(null) } finally { setDetailLoading(false) } }
  async function exportRefunds() { setExporting(true); try { const blob = await exportAdminRefunds(query); const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'shopsphere-refunds.csv'; anchor.click(); URL.revokeObjectURL(url); setToast('Filtered refund report exported.') } catch (requestError) { setToast(getRequestError(requestError, 'Could not export refunds.')) } finally { setExporting(false) } }
  const rows = pageData?.items || []

  return <AdminLayout onExport={exportRefunds} exporting={exporting} exportLabel="Export Refunds" pageTitle="Refunds Management" pageSubtitle="Track customer refunds and their gateway status across the marketplace." breadcrumb="Refunds" showDateFilter={false} titleIcon="refund">
    <main className="admin-refunds-page">
      {error && <div className="ar-alert" role="alert">{error}<button onClick={loadRefunds} type="button">Retry</button></div>}
      <div className="ar-workflow-note"><Icon name="clock" size={17}/>Refunds shown here are initiated by the existing full-refund cancellation workflow. Gateway callbacks update pending refunds; admin review and rejection are not currently supported by the backend.</div>
      <section aria-label="Refund statistics" className="ar-stats-grid"><Stat title="Total Refunds" count={stats?.totalCount} amount={stats?.totalAmount} icon="refund" tone="purple"/><Stat title="Successful Refunds" count={stats?.refundedCount} amount={stats?.refundedAmount} icon="success" tone="green"/><Stat title="Pending Refunds" count={stats?.pendingCount} amount={stats?.pendingAmount} icon="clock" tone="orange"/><Stat title="Failed Refunds" count={stats?.failedCount} amount={stats?.failedAmount} icon="fail" tone="red"/></section>
      <form className="ar-filter-bar" onSubmit={applyFilters}><label className="ar-search"><Icon name="search"/><input autoComplete="off" name="search" onChange={updateDraft} placeholder="Search order, refund or transaction ID, customer..." value={draft.search}/></label><select aria-label="Refund status" name="status" onChange={updateDraft} value={draft.status}><option value="">Refund Status: All</option>{STATUSES.map((status) => <option key={status} value={status}>{refundStatusLabel(status)}</option>)}</select><select aria-label="Payment method" name="method" onChange={updateDraft} value={draft.method}><option value="">Payment Method: All</option>{METHODS.map((method) => <option key={method} value={method}>{method}</option>)}</select><label className="ar-date"><span>From</span><input aria-label="Start date" max={draft.to || undefined} name="from" onChange={updateDraft} type="date" value={draft.from}/></label><label className="ar-date"><span>To</span><input aria-label="End date" min={draft.from || undefined} name="to" onChange={updateDraft} type="date" value={draft.to}/></label><button className="ar-button ar-button--primary" type="submit"><Icon name="search"/>Search</button><button className="ar-button" onClick={resetFilters} type="button"><Icon name="reset"/>Reset</button></form>
      <section aria-label="Marketplace refunds" className="ar-table-card"><div className="ar-table-scroll"><table className="ar-table"><thead><tr><th className="ar-check"><input aria-label="Select all refunds on page" checked={rows.length > 0 && rows.every((row) => selectedRows.includes(row.paymentTransactionId))} onChange={() => setSelectedRows((current) => rows.every((row) => current.includes(row.paymentTransactionId)) ? current.filter((id) => !rows.some((row) => row.paymentTransactionId === id)) : [...new Set([...current, ...rows.map((row) => row.paymentTransactionId)])])} type="checkbox"/></th><th>Refund Ref</th><th>Order ID</th><th>Customer</th><th>Order Amount</th><th>Refund Amount</th><th>Refund Status</th><th>Last Updated</th><th>Actions</th></tr></thead><tbody>{loading && Array.from({ length: 6 }, (_, index) => <tr className="ar-skeleton" key={`skeleton-${index}`}><td colSpan="9"><span/></td></tr>)}{!loading && rows.map((refund) => <tr key={refund.paymentTransactionId}><td className="ar-check"><input aria-label={`Select payment transaction ${refund.paymentTransactionId}`} checked={selectedRows.includes(refund.paymentTransactionId)} onChange={() => setSelectedRows((current) => current.includes(refund.paymentTransactionId) ? current.filter((id) => id !== refund.paymentTransactionId) : [...current, refund.paymentTransactionId])} type="checkbox"/></td><td><strong className="ar-ref-id">{refund.refundReference || `Payment #${refund.paymentTransactionId}`}</strong></td><td><strong className="ar-order-id">#{orderCode(refund.orderId)}</strong></td><td><div className="ar-customer"><Avatar name={refund.customerName}/><span><strong>{refund.customerName || 'Customer unavailable'}</strong><small>{refund.customerEmail || 'Email unavailable'}</small></span></div></td><td>{money(refund.orderAmount, refund.currency)}</td><td><strong className="ar-amount">{money(refund.refundAmount, refund.currency)}</strong></td><td><Status status={refund.status}/></td><td>{dateTime(refund.lastUpdated)}</td><td><div className="ar-actions"><button aria-label="View refund summary" onClick={() => viewRefund(refund, 'modal')} title="Refund summary" type="button"><Icon name="eye"/></button><button aria-label="View refund drawer" onClick={() => viewRefund(refund, 'drawer')} title="Refund details" type="button"><Icon name="order"/></button></div></td></tr>)}</tbody></table></div>{!loading && !rows.length && <div className="ar-empty"><span><Icon name="refund" size={28}/></span><h2>{error ? 'Refunds are unavailable' : 'No refund records found'}</h2><p>{error ? 'Retry the request or check your connection.' : 'Refund records appear after the gateway refund workflow has been initiated.'}</p></div>}</section>
      <footer className="ar-pagination"><span>Showing <strong>{pageData?.totalElements ? page * size + 1 : 0}–{Math.min((page + 1) * size, pageData?.totalElements || 0)}</strong> of <strong>{Number(pageData?.totalElements || 0).toLocaleString('en-IN')}</strong> refunds{selectedRows.length ? ` · ${selectedRows.length} selected` : ''}</span><div><label>Rows <select aria-label="Rows per page" onChange={(event) => { setSize(Number(event.target.value)); setPage(0); setSelectedRows([]) }} value={size}>{PAGE_SIZES.map((value) => <option key={value} value={value}>{value}</option>)}</select></label><button aria-label="Previous page" disabled={page <= 0 || loading} onClick={() => setPage((value) => value - 1)} type="button">‹</button><span>Page {pageData?.totalPages ? page + 1 : 0} of {pageData?.totalPages || 0}</span><button aria-label="Next page" disabled={page + 1 >= (pageData?.totalPages || 0) || loading} onClick={() => setPage((value) => value + 1)} type="button">›</button></div></footer>
    </main>
    {detailLoading && <div className="ar-backdrop"><aside aria-label="Loading refund details" className="ar-drawer-loading"><span className="ar-loader"/><strong>Loading refund details…</strong></aside></div>}
    {selected?.mode === 'modal' && <RefundModal closeRef={closeRef} onClose={() => setSelected(null)} refund={selected}/>}{selected?.mode === 'drawer' && <RefundDrawer onClose={() => setSelected(null)} refund={selected}/>}
    {toast && <div className="ar-toast" role="status">{toast}<button aria-label="Dismiss notification" onClick={() => setToast('')} type="button">×</button></div>}
  </AdminLayout>
}
