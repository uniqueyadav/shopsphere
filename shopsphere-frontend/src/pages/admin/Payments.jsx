import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import AdminLayout from '../../layouts/AdminLayout'
import { getRequestError } from '../../services/authService'
import { exportAdminPayments, getAdminPayment, getAdminPayments, getAdminPaymentStats } from '../../services/adminPaymentService'
import './AdminPayments.css'

const STATUSES = ['PENDING', 'SUCCESS', 'PAID', 'FAILED', 'REFUNDED']
const METHODS = ['ONLINE', 'COD']
const PAGE_SIZES = [10, 20, 50]

function Icon({ name, size = 19 }) {
  const paths = {
    card: <><rect x="2.5" y="5" width="19" height="14" rx="2"/><path d="M3 10h18M7 15h3"/></>,
    success: <><circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    fail: <><circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></>,
    reset: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.6 9a7 7 0 0 1 11.7-2L20 12M4 12l2.7 5a7 7 0 0 0 11.7-2"/></>,
    eye: <><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></>,
    close: <path d="m6 6 12 12M18 6 6 18"/>,
    download: <><path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4"/></>,
    order: <><path d="M4 4h16v16H4zM8 8h8M8 12h8M8 16h4"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

function money(value, currency = 'INR') {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: currency || 'INR', maximumFractionDigits: 2 }).format(Number(value || 0))
}
function dateTime(value) {
  return value ? new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : 'Unavailable'
}
function orderCode(id) { return `ORD${String(id ?? '').padStart(6, '0')}` }
function statusLabel(status) { return String(status || 'UNKNOWN').replaceAll('_', ' ') }
function Status({ status }) { return <span className={`ap-status ap-status--${String(status || '').toLowerCase()}`}><i/>{statusLabel(status)}</span> }
function Avatar({ name }) { return <span aria-hidden="true" className="ap-avatar">{(name || 'C').split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase()}</span> }

function Stat({ title, amount, count, icon, tone }) {
  return <article className={`ap-stat ap-stat--${tone}`}><span className="ap-stat-icon"><Icon name={icon} size={24}/></span><div><span className="ap-stat-title">{title}</span><strong>{amount == null ? '—' : money(amount)}</strong><small>{count == null ? 'No transaction data' : `${Number(count).toLocaleString('en-IN')} transactions`}</small></div></article>
}

function PaymentDrawer({ payment, onClose }) {
  if (!payment) return null
  const refundStatus = payment.refundStatus || 'NOT_REQUESTED'
  return <div className="ap-backdrop" onClick={onClose} role="presentation"><aside aria-label="Payment details" className="ap-drawer" onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true">
    <header className="ap-drawer-head"><span className="ap-mark"><Icon name="card"/></span><div><h2>Payment Details</h2><small>Transaction #{payment.id}</small></div><button aria-label="Close payment details" className="ap-icon-btn" onClick={onClose} type="button"><Icon name="close"/></button></header>
    <div className="ap-drawer-scroll">
      <section className="ap-payment-banner"><Status status={payment.status}/><strong>{money(payment.amount, payment.currency)}</strong><span>{dateTime(payment.createdAt)}</span><button className="ap-button ap-button--muted" disabled title="Receipt generation is not available yet" type="button"><Icon name="download"/>Receipt unavailable</button></section>
      <section className="ap-detail-section"><h3><Icon name="order"/>Order Information</h3><dl className="ap-detail-grid"><div><dt>Order ID</dt><dd>{orderCode(payment.orderId)}</dd></div><div><dt>Customer</dt><dd>{payment.customerName || 'Unavailable'}</dd></div><div><dt>Email</dt><dd className="ap-wrap">{payment.customerEmail || 'Unavailable'}</dd></div><div><dt>Phone</dt><dd>{payment.customerPhone || 'Unavailable'}</dd></div></dl></section>
      <section className="ap-detail-section"><h3><Icon name="card"/>Payment Information</h3><dl className="ap-detail-grid"><div><dt>Payment method</dt><dd>{payment.paymentMethod || 'Unavailable'}</dd></div><div><dt>Transaction ID</dt><dd className="ap-wrap">{payment.transactionId || 'Not assigned'}</dd></div><div><dt>Gateway order ID</dt><dd className="ap-wrap">{payment.gatewayOrderId || 'Unavailable'}</dd></div><div><dt>Payment status</dt><dd><Status status={payment.status}/></dd></div></dl>{payment.failureReason && <p className="ap-failure-note">Failure code: {payment.failureReason}</p>}</section>
      <section className="ap-detail-section"><h3><Icon name="reset"/>Refund Information</h3><dl className="ap-detail-grid"><div><dt>Refund status</dt><dd>{statusLabel(refundStatus)}</dd></div><div><dt>Refund amount</dt><dd>{money(payment.refundAmount, payment.currency)}</dd></div><div className="ap-grid-wide"><dt>Refund reference</dt><dd className="ap-wrap">{payment.refundReference || 'No refund reference'}</dd></div></dl></section>
      <section className="ap-detail-section"><h3><Icon name="order"/>Order Summary</h3><dl className="ap-summary"><div><dt>Subtotal</dt><dd>{money(payment.subtotal, payment.currency)}</dd></div><div><dt>Shipping charge</dt><dd>{money(payment.deliveryCharges, payment.currency)}</dd></div><div className="ap-summary-total"><dt>Order total</dt><dd>{money(payment.totalAmount, payment.currency)}</dd></div></dl><p className="ap-muted">Tax and discount breakdown are not stored separately in the current order API.</p></section>
      {payment.sellerOrders?.length > 0 && <section className="ap-detail-section"><h3><Icon name="order"/>Seller Suborders</h3><div className="ap-suborders">{payment.sellerOrders.map((sellerOrder) => <div key={sellerOrder.id}><strong>{sellerOrder.sellerName || `Seller order ${sellerOrder.id}`}</strong><span>Suborder #{sellerOrder.id}</span><b>{money(sellerOrder.totalAmount, payment.currency)}</b></div>)}</div><p className="ap-muted">These are order amounts, not seller payouts.</p></section>}
    </div>
    <footer className="ap-drawer-foot"><Link className="ap-button ap-button--primary" onClick={onClose} to={`/admin/orders/${payment.orderId}`}>View Order Details <span aria-hidden="true">→</span></Link></footer>
  </aside></div>
}

export default function Payments() {
  const [draft, setDraft] = useState({ search: '', status: '', method: '', from: '', to: '' })
  const [filters, setFilters] = useState({ search: '', status: '', method: '', from: '', to: '' })
  const [pageData, setPageData] = useState(null)
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [selected, setSelected] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [toast, setToast] = useState('')
  const [selectedRows, setSelectedRows] = useState([])

  const params = useMemo(() => ({ page, size, ...(filters.search ? { search: filters.search } : {}), ...(filters.status ? { status: filters.status } : {}), ...(filters.method ? { method: filters.method } : {}), ...(filters.from ? { from: filters.from } : {}), ...(filters.to ? { to: filters.to } : {}) }), [page, size, filters])
  const statParams = useMemo(() => ({ ...(filters.search ? { search: filters.search } : {}), ...(filters.status ? { status: filters.status } : {}), ...(filters.method ? { method: filters.method } : {}), ...(filters.from ? { from: filters.from } : {}), ...(filters.to ? { to: filters.to } : {}) }), [filters])

  const loadPayments = useCallback(async () => {
    setLoading(true); setError('')
    try { setPageData(await getAdminPayments(params)) }
    catch (requestError) { setError(getRequestError(requestError, 'Could not load marketplace payments.')) }
    finally { setLoading(false) }
  }, [params])
  useEffect(() => { loadPayments() }, [loadPayments])
  useEffect(() => {
    let active = true
    getAdminPaymentStats(statParams).then((value) => { if (active) setStats(value) }).catch((requestError) => { if (active) setToast(getRequestError(requestError, 'Payment statistics are unavailable.')) })
    return () => { active = false }
  }, [statParams])
  useEffect(() => { if (!toast) return undefined; const timer = window.setTimeout(() => setToast(''), 4500); return () => window.clearTimeout(timer) }, [toast])

  function updateDraft(event) { setDraft((current) => ({ ...current, [event.target.name]: event.target.value })) }
  function applyFilters(event) {
    event.preventDefault()
    if (draft.from && draft.to && draft.from > draft.to) { setError('Start date cannot be after end date.'); return }
    setPage(0); setSelectedRows([]); setFilters({ ...draft, search: draft.search.trim() })
  }
  function resetFilters() { const empty = { search: '', status: '', method: '', from: '', to: '' }; setDraft(empty); setFilters(empty); setPage(0); setSelectedRows([]) }
  async function viewPayment(id) {
    setSelected(null); setDetailLoading(true)
    try { setSelected(await getAdminPayment(id)) }
    catch (requestError) { setToast(getRequestError(requestError, 'Could not load payment details.')) }
    finally { setDetailLoading(false) }
  }
  async function exportPayments() {
    setExporting(true)
    try { const blob = await exportAdminPayments(statParams); const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'shopsphere-payments.csv'; anchor.click(); URL.revokeObjectURL(url); setToast('Filtered payment report exported.') }
    catch (requestError) { setToast(getRequestError(requestError, 'Could not export payments.')) }
    finally { setExporting(false) }
  }
  const rows = pageData?.items || []
  const successfulCount = Number(stats?.successfulCount || 0) + Number(stats?.successfulCount == null ? stats?.paidCount || 0 : 0)
  const successfulAmount = Number(stats?.successfulAmount || 0) + Number(stats?.successfulAmount == null ? stats?.paidAmount || 0 : 0)

  return <AdminLayout onExport={exportPayments} exporting={exporting} exportLabel="Export Payments" pageTitle="Payments Management" pageSubtitle="Track and manage all payment transactions across the marketplace." breadcrumb="Payments" showDateFilter={false} titleIcon="card">
    <main className="admin-payments-page">
      {error && <div className="ap-alert" role="alert">{error}<button onClick={loadPayments} type="button">Retry</button></div>}
      <section aria-label="Payment statistics" className="ap-stats-grid">
        <Stat title="Total Payments" amount={stats?.totalAmount} count={stats?.totalCount} icon="card" tone="purple"/>
        <Stat title="Successful" amount={successfulAmount} count={successfulCount} icon="success" tone="green"/>
        <Stat title="Pending" amount={stats?.pendingAmount} count={stats?.pendingCount} icon="clock" tone="orange"/>
        <Stat title="Failed" amount={stats?.failedAmount} count={stats?.failedCount} icon="fail" tone="red"/>
      </section>
      <form className="ap-filter-bar" onSubmit={applyFilters}>
        <label className="ap-search"><Icon name="search"/><input autoComplete="off" name="search" onChange={updateDraft} placeholder="Search order, transaction ID or customer..." value={draft.search}/></label>
        <select aria-label="Payment status" name="status" onChange={updateDraft} value={draft.status}><option value="">Payment Status: All</option>{STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}</select>
        <select aria-label="Payment method" name="method" onChange={updateDraft} value={draft.method}><option value="">Payment Method: All</option>{METHODS.map((method) => <option key={method} value={method}>{method}</option>)}</select>
        <label className="ap-date"><span>From</span><input aria-label="Start date" max={draft.to || undefined} name="from" onChange={updateDraft} type="date" value={draft.from}/></label>
        <label className="ap-date"><span>To</span><input aria-label="End date" min={draft.from || undefined} name="to" onChange={updateDraft} type="date" value={draft.to}/></label>
        <button className="ap-button ap-button--primary" type="submit"><Icon name="search"/>Search</button><button className="ap-button" onClick={resetFilters} type="button"><Icon name="reset"/>Reset</button>
      </form>
      <section aria-label="Marketplace payments" className="ap-table-card"><div className="ap-table-scroll"><table className="ap-table">
        <thead><tr><th className="ap-check"><input aria-label="Select all payments on page" checked={rows.length > 0 && rows.every((row) => selectedRows.includes(row.id))} onChange={() => setSelectedRows((current) => rows.every((row) => current.includes(row.id)) ? current.filter((id) => !rows.some((row) => row.id === id)) : [...new Set([...current, ...rows.map((row) => row.id)])])} type="checkbox"/></th><th>Order ID</th><th>Customer</th><th>Payment Method</th><th>Amount</th><th>Status</th><th>Transaction ID</th><th>Payment Date</th><th>Actions</th></tr></thead>
        <tbody>
          {loading && Array.from({ length: 6 }, (_, index) => <tr className="ap-skeleton" key={`skeleton-${index}`}><td colSpan="9"><span/></td></tr>)}
          {!loading && rows.map((payment) => <tr key={payment.id}>
            <td className="ap-check"><input aria-label={`Select payment ${payment.id}`} checked={selectedRows.includes(payment.id)} onChange={() => setSelectedRows((current) => current.includes(payment.id) ? current.filter((id) => id !== payment.id) : [...current, payment.id])} type="checkbox"/></td>
            <td><strong className="ap-order-id">#{orderCode(payment.orderId)}</strong></td>
            <td><div className="ap-customer"><Avatar name={payment.customerName}/><div><strong>{payment.customerName || 'Customer unavailable'}</strong><span>{payment.customerEmail || 'Email unavailable'}</span></div></div></td>
            <td><span className="ap-method"><span aria-hidden="true" className="ap-provider-icon"><Icon name="card" size={17}/></span>{payment.paymentMethod || 'Unknown'}</span></td>
            <td><strong className="ap-amount">{money(payment.amount, payment.currency)}</strong></td><td><Status status={payment.status}/></td>
            <td><span className="ap-transaction">{payment.transactionId || payment.gatewayOrderId || 'Not assigned'}</span></td><td>{dateTime(payment.createdAt)}</td>
            <td><div className="ap-actions"><button aria-label={`View payment ${payment.id}`} onClick={() => viewPayment(payment.id)} title="View payment" type="button"><Icon name="eye" size={18}/></button><Link aria-label={`View order ${orderCode(payment.orderId)}`} title="View order" to={`/admin/orders/${payment.orderId}`}><Icon name="order" size={17}/></Link></div></td>
          </tr>)}
        </tbody>
      </table></div>
      {!loading && !rows.length && <div className="ap-empty"><span><Icon name="card" size={28}/></span><h2>{error ? 'Payments are unavailable' : 'No payments found'}</h2><p>{error ? 'Retry the request or check your connection.' : 'There are no transactions matching the selected filters.'}</p></div>}
      </section>
      <footer className="ap-pagination"><span>Showing <strong>{pageData?.totalElements ? page * size + 1 : 0}–{Math.min((page + 1) * size, pageData?.totalElements || 0)}</strong> of <strong>{Number(pageData?.totalElements || 0).toLocaleString('en-IN')}</strong> payments{selectedRows.length > 0 ? ` · ${selectedRows.length} selected` : ''}</span><div><label>Rows <select aria-label="Rows per page" onChange={(event) => { setSize(Number(event.target.value)); setPage(0); setSelectedRows([]) }} value={size}>{PAGE_SIZES.map((value) => <option key={value} value={value}>{value}</option>)}</select></label><button aria-label="Previous page" disabled={page <= 0 || loading} onClick={() => setPage((value) => value - 1)} type="button">‹</button><span>Page {pageData?.totalPages ? page + 1 : 0} of {pageData?.totalPages || 0}</span><button aria-label="Next page" disabled={page + 1 >= (pageData?.totalPages || 0) || loading} onClick={() => setPage((value) => value + 1)} type="button">›</button></div></footer>
    </main>
    {detailLoading && <div className="ap-backdrop"><aside aria-label="Loading payment details" className="ap-drawer-loading"><span className="ap-loader"/><strong>Loading payment details…</strong></aside></div>}
    {selected && <PaymentDrawer onClose={() => setSelected(null)} payment={selected}/>}
    {toast && <div className="ap-toast" role="status">{toast}<button aria-label="Dismiss notification" onClick={() => setToast('')} type="button">×</button></div>}
  </AdminLayout>
}
