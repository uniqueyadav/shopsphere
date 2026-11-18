import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AdminLayout from '../../layouts/AdminLayout'
import AdminOrderPanel, { orderCode, orderDate, orderMoney, OrderStatusBadge, printOrderSummary } from '../../components/dashboard/AdminOrderPanel'
import OrderCancelDialog from '../../components/dashboard/OrderCancelDialog'
import { getRequestError } from '../../services/authService'
import { cancelAdminOrder, exportAdminOrders, getAdminOrder, getAdminOrderStats, getAdminOrders } from '../../services/orderService'
import './AdminOrders.css'

const ORDER_STATUSES = ['PAYMENT_PENDING', 'PLACED', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED']
const PAYMENT_STATUSES = ['PENDING', 'SUCCESS', 'PAID', 'FAILED', 'REFUNDED']
const PAGE_SIZES = [10, 20, 50]

function Icon({ name, size = 20 }) {
  const paths = {
    cart: <><path d="M3 3h2l2.4 12.4a2 2 0 0 0 2 1.6h8.9a2 2 0 0 0 2-1.6L22 8H6"/><circle cx="10" cy="21" r="1"/><circle cx="18" cy="21" r="1"/></>,
    check: <><circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    cancel: <><circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></>,
    reset: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.6 9a7 7 0 0 1 11.7-2L20 12M4 12l2.7 5a7 7 0 0 0 11.7-2"/></>,
    eye: <><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></>,
    more: <><circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/></>,
    download: <><path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4"/></>,
    box: <><path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="M3 8v9l9 5 9-5V8M12 13v9"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

function StatCard({ title, value, icon, tone, note }) {
  return <article className={`ao-stat ao-stat--${tone}`}><span className="ao-stat-icon"><Icon name={icon} size={25}/></span><div><span>{title}</span><strong>{value == null ? '—' : Number(value).toLocaleString('en-IN')}</strong><small>{note}</small></div></article>
}

function orderItems(order) {
  return (order.sellerOrders || []).flatMap((seller) => (seller.items || []).map((item) => ({ ...item, sellerName: seller.sellerName })))
}

function deliveryLabel(order) {
  const statuses = [...new Set((order.sellerOrders || []).map((seller) => seller.orderStatus))]
  if (!statuses.length) return 'Not assigned'
  return statuses.length === 1 ? statuses[0] : `Mixed · ${statuses.length} sellers`
}

export default function AllOrders() {
  const navigate = useNavigate()
  const [pageData, setPageData] = useState(null)
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [statsError, setStatsError] = useState('')
  const [toast, setToast] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [draft, setDraft] = useState({ search: '', orderStatus: '', paymentStatus: '', deliveryStatus: '', from: '', to: '' })
  const [filters, setFilters] = useState({ search: '', orderStatus: '', paymentStatus: '', deliveryStatus: '', from: '', to: '' })
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [menuOrderId, setMenuOrderId] = useState(null)
  const [selectedRows, setSelectedRows] = useState([])
  const [cancelOrder, setCancelOrder] = useState(null)
  const [cancelError, setCancelError] = useState('')
  const [cancelling, setCancelling] = useState(false)
  const [exporting, setExporting] = useState(false)

  const params = useMemo(() => ({
    page, size,
    ...(filters.search ? { search: filters.search } : {}),
    ...(filters.orderStatus ? { orderStatus: filters.orderStatus } : {}),
    ...(filters.paymentStatus ? { paymentStatus: filters.paymentStatus } : {}),
    ...(filters.deliveryStatus ? { deliveryStatus: filters.deliveryStatus } : {}),
    ...(filters.from ? { from: filters.from } : {}),
    ...(filters.to ? { to: filters.to } : {}),
  }), [page, size, filters])

  const loadOrders = useCallback(async () => {
    setLoading(true); setError('')
    try { setPageData(await getAdminOrders(params)) }
    catch (requestError) { setError(getRequestError(requestError, 'Could not load marketplace orders.')) }
    finally { setLoading(false) }
  }, [params])

  useEffect(() => { loadOrders() }, [loadOrders])
  useEffect(() => {
    let active = true
    getAdminOrderStats().then((result) => { if (active) setStats(result) })
      .catch((requestError) => { if (active) setStatsError(getRequestError(requestError, 'Order statistics are unavailable.')) })
    return () => { active = false }
  }, [])
  useEffect(() => {
    const timer = window.setTimeout(() => {
      setPage(0)
      setFilters((current) => ({ ...current, search: draft.search.trim() }))
    }, 450)
    return () => window.clearTimeout(timer)
  }, [draft.search])
  useEffect(() => {
    if (!toast) return undefined
    const timer = window.setTimeout(() => setToast(''), 4500)
    return () => window.clearTimeout(timer)
  }, [toast])

  function updateDraft(event) {
    setDraft((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function applyFilters(event) {
    event.preventDefault()
    if (draft.from && draft.to && draft.from > draft.to) {
      setError('Start date cannot be after end date.')
      return
    }
    setPage(0)
    setFilters({ ...draft, search: draft.search.trim() })
  }

  function resetFilters() {
    const empty = { search: '', orderStatus: '', paymentStatus: '', deliveryStatus: '', from: '', to: '' }
    setDraft(empty); setFilters(empty); setPage(0); setSize(10); setSelectedRows([])
  }

  async function exportOrders() {
    setExporting(true)
    try {
      const blob = await exportAdminOrders(filters)
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url; anchor.download = 'shopsphere-orders.csv'; anchor.click()
      URL.revokeObjectURL(url)
      setToast('Filtered orders exported.')
    } catch (requestError) { setToast(getRequestError(requestError, 'Could not export orders.')) }
    finally { setExporting(false) }
  }

  async function viewOrder(orderId) {
    setMenuOrderId(null); setDetailLoading(true); setSelectedOrder(null)
    try { setSelectedOrder(await getAdminOrder(orderId)) }
    catch (requestError) { setToast(getRequestError(requestError, 'Could not load order details.')) }
    finally { setDetailLoading(false) }
  }

  async function confirmCancellation(reason) {
    if (!cancelOrder) return
    setCancelling(true); setCancelError('')
    try {
      const result = await cancelAdminOrder(cancelOrder.id, reason)
      const updated = await getAdminOrder(cancelOrder.id)
      setSelectedOrder(updated); setCancelOrder(null); setToast(result.message || 'Order cancelled.')
      await loadOrders()
      getAdminOrderStats().then(setStats).catch(() => {})
    } catch (requestError) { setCancelError(getRequestError(requestError, 'The order could not be cancelled.')) }
    finally { setCancelling(false) }
  }

  function toggleRow(id) {
    setSelectedRows((current) => current.includes(id) ? current.filter((rowId) => rowId !== id) : [...current, id])
  }

  function toggleAll(rows) {
    const ids = rows.map((row) => row.id)
    setSelectedRows((current) => ids.every((id) => current.includes(id))
      ? current.filter((id) => !ids.includes(id))
      : [...new Set([...current, ...ids])])
  }

  const rows = pageData?.items || []
  const totalPages = pageData?.totalPages || 0

  return <AdminLayout onExport={exportOrders} exporting={exporting} exportLabel="Export Orders" pageTitle="All Orders Management" pageSubtitle="View and manage all orders across the marketplace." breadcrumb="Orders" showDateFilter={false}>
    <main className="admin-orders-page">
      {statsError && <div className="ao-alert" role="alert">{statsError}</div>}
      {error && <div className="ao-alert" role="alert">{error}<button onClick={loadOrders} type="button">Retry</button></div>}

      <section aria-label="Order statistics" className="ao-stats-grid">
        <StatCard title="Total Orders" value={stats?.total} icon="cart" tone="purple" note="All customer orders"/>
        <StatCard title="Delivered" value={stats?.delivered} icon="check" tone="green" note="Completed deliveries"/>
        <StatCard title="Processing" value={stats?.processing} icon="clock" tone="orange" note="Seller fulfillment"/>
        <StatCard title="Cancelled" value={stats?.cancelled} icon="cancel" tone="red" note="Cancelled orders"/>
      </section>

      <form className="ao-filter-bar" onSubmit={applyFilters}>
        <label className="ao-search"><Icon name="search"/><input aria-label="Search order ID, customer name, or email" autoComplete="off" name="search" onChange={updateDraft} placeholder="Search order ID, customer name or email..." value={draft.search}/></label>
        <select aria-label="Filter by order status" name="orderStatus" onChange={updateDraft} value={draft.orderStatus}><option value="">Order status: All</option>{ORDER_STATUSES.map((status) => <option key={status} value={status}>{status.replaceAll('_', ' ')}</option>)}</select>
        <select aria-label="Filter by payment status" name="paymentStatus" onChange={updateDraft} value={draft.paymentStatus}><option value="">Payment: All</option>{PAYMENT_STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}</select>
        <select aria-label="Filter by fulfillment status" name="deliveryStatus" onChange={updateDraft} value={draft.deliveryStatus}><option value="">Fulfillment: All</option>{ORDER_STATUSES.filter((status) => status !== 'PAYMENT_PENDING').map((status) => <option key={status} value={status}>{status.replaceAll('_', ' ')}</option>)}</select>
        <label className="ao-date-field"><span>From</span><input aria-label="Start date" max={draft.to || undefined} name="from" onChange={updateDraft} type="date" value={draft.from}/></label>
        <label className="ao-date-field"><span>To</span><input aria-label="End date" min={draft.from || undefined} name="to" onChange={updateDraft} type="date" value={draft.to}/></label>
        <button className="ao-button ao-button--primary" type="submit"><Icon name="search"/>Search</button>
        <button className="ao-button ao-button--outline" onClick={resetFilters} type="button"><Icon name="reset"/>Reset</button>
      </form>

      <section aria-label="Marketplace orders" className="ao-table-card">
        <div className="ao-table-scroll"><table className="ao-table">
          <thead><tr><th className="ao-checkbox-cell"><input aria-label="Select all orders on this page" checked={rows.length > 0 && rows.every((row) => selectedRows.includes(row.id))} onChange={() => toggleAll(rows)} type="checkbox"/></th><th>Order ID</th><th>Customer</th><th>Items</th><th>Amount</th><th>Payment</th><th>Fulfillment</th><th>Order Status</th><th>Order Date</th><th>Actions</th></tr></thead>
          <tbody>
            {loading && Array.from({ length: 6 }, (_, index) => <tr className="ao-skeleton-row" key={`skeleton-${index}`}><td colSpan="10"><span/></td></tr>)}
            {!loading && rows.map((order) => {
              const items = orderItems(order)
              return <tr key={order.id}>
                <td className="ao-checkbox-cell"><input aria-label={`Select order ${orderCode(order.id)}`} checked={selectedRows.includes(order.id)} onChange={() => toggleRow(order.id)} type="checkbox"/></td>
                <td><strong className="ao-order-code">#{orderCode(order.id)}</strong></td>
                <td><div className="ao-customer-cell"><span className="ao-table-avatar">{(order.customerName || 'C').split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase()}</span><div><strong>{order.customerName || 'Customer'}</strong><span>{order.customerEmail || 'Email unavailable'}</span></div></div></td>
                <td><div className="ao-items-preview">{items.slice(0, 2).map((item, index) => <span className="ao-item-thumb" key={`${order.id}-${index}`}>{item.imageUrl ? <img alt="" src={item.imageUrl}/> : <Icon name="box" size={15}/>}</span>)}{items.length > 2 && <span className="ao-more-items">+{items.length - 2}</span>}<small>{items.length} {items.length === 1 ? 'item' : 'items'}</small></div></td>
                <td><strong className="ao-amount">{orderMoney(order.totalAmount)}</strong></td>
                <td><div className="ao-payment-cell"><strong>{order.paymentMethod}</strong><span>{order.paymentStatus}</span></div></td>
                <td><span className={`ao-fulfillment-label ao-fulfillment-label--${String(order.sellerOrders?.length === 1 ? order.sellerOrders[0].orderStatus : 'mixed').toLowerCase()}`}>{deliveryLabel(order).replaceAll('_', ' ')}</span></td>
                <td><OrderStatusBadge status={order.orderStatus}/></td>
                <td>{orderDate(order.createdAt, true)}</td>
                <td><div className="ao-row-actions"><button aria-label={`View order ${orderCode(order.id)}`} onClick={() => viewOrder(order.id)} title="View order" type="button"><Icon name="eye" size={18}/></button><div className="ao-menu-wrap"><button aria-expanded={menuOrderId === order.id} aria-label={`More actions for ${orderCode(order.id)}`} onClick={() => setMenuOrderId((current) => current === order.id ? null : order.id)} type="button"><Icon name="more" size={19}/></button>{menuOrderId === order.id && <div className="ao-row-menu"><button onClick={() => viewOrder(order.id)} type="button">View order</button><button onClick={async () => { setMenuOrderId(null); try { const full = await getAdminOrder(order.id); if (!printOrderSummary(full)) setToast('Allow pop-ups in your browser to print the receipt.') } catch (requestError) { setToast(getRequestError(requestError, 'Could not load order receipt.')) } }} type="button">Print invoice</button>{order.cancellationEligible && <button className="ao-menu-danger" onClick={() => { setMenuOrderId(null); setCancelOrder(order); setCancelError('') }} type="button">Cancel order</button>}</div>}</div></div></td>
              </tr>
            })}
          </tbody>
        </table></div>
        {!loading && !rows.length && <div className="ao-empty"><span><Icon name="cart" size={28}/></span><h2>{error ? 'Orders are unavailable' : 'No orders found'}</h2><p>{error ? 'Retry the request or check your connection.' : 'Change your filters to search other orders.'}</p></div>}
      </section>

      <footer className="ao-pagination"><div><span>Showing <strong>{pageData?.totalElements ? page * size + 1 : 0}–{Math.min((page + 1) * size, pageData?.totalElements || 0)}</strong> of <strong>{Number(pageData?.totalElements || 0).toLocaleString('en-IN')}</strong> orders</span>{selectedRows.length > 0 && <small>{selectedRows.length} selected</small>}</div><div className="ao-pagination-controls"><label>Rows <select aria-label="Rows per page" onChange={(event) => { setSize(Number(event.target.value)); setPage(0); setSelectedRows([]) }} value={size}>{PAGE_SIZES.map((count) => <option key={count} value={count}>{count}</option>)}</select></label><button aria-label="Previous page" disabled={page <= 0 || loading} onClick={() => setPage((current) => current - 1)} type="button">‹</button><span>Page {totalPages ? page + 1 : 0} of {totalPages}</span><button aria-label="Next page" disabled={page + 1 >= totalPages || loading} onClick={() => setPage((current) => current + 1)} type="button">›</button></div></footer>
    </main>

    {detailLoading && <div className="ao-drawer-backdrop"><aside aria-label="Loading order details" className="ao-drawer-loading"><span className="ao-loader"/><strong>Loading order details…</strong></aside></div>}
    {selectedOrder && <div className="ao-drawer-backdrop" onClick={() => setSelectedOrder(null)} role="presentation"><aside className="ao-drawer" onClick={(event) => event.stopPropagation()} role="presentation"><AdminOrderPanel order={selectedOrder} onCancel={selectedOrder.cancellationEligible ? () => { setCancelOrder(selectedOrder); setCancelError('') } : undefined} onClose={() => setSelectedOrder(null)} onPrintBlocked={() => setToast('Allow pop-ups in your browser to print the receipt.')} onViewFull={() => navigate(`/admin/orders/${selectedOrder.id}`)}/></aside></div>}
    {cancelOrder && <OrderCancelDialog busy={cancelling} error={cancelError} onClose={() => { if (!cancelling) setCancelOrder(null) }} onConfirm={confirmCancellation} order={cancelOrder}/>}
    {toast && <div className="ao-toast" role="status">{toast}<button aria-label="Dismiss notification" onClick={() => setToast('')} type="button">×</button></div>}
  </AdminLayout>
}
