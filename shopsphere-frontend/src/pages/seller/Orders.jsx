import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import SellerLayout, { SellerIcon } from '../../layouts/SellerLayout'
import { getRequestError } from '../../services/authService'
import { getSellerOrderSummary, getSellerOrders, updateSellerOrderStatus } from '../../services/orderService'
import './SellerOrders.css'

const PAGE_SIZE = 5
const TABS = [
  ['ALL', 'All Orders', 'totalOrders'], ['NEW', 'New', 'newOrders'], ['PROCESSING', 'Processing', 'processing'],
  ['SHIPPED', 'Shipped', 'shipped'], ['DELIVERED', 'Delivered', 'delivered'], ['CANCELLED', 'Cancelled', 'cancelled'],
]
const NEXT_STATUS = { PLACED: 'CONFIRMED', CONFIRMED: 'PROCESSING', PROCESSING: 'SHIPPED', SHIPPED: 'OUT_FOR_DELIVERY', OUT_FOR_DELIVERY: 'DELIVERED' }
const STATUS_LABELS = { PAYMENT_PENDING: 'Payment Pending', PLACED: 'New', CONFIRMED: 'Confirmed', PROCESSING: 'Processing', SHIPPED: 'Shipped', OUT_FOR_DELIVERY: 'Out for Delivery', DELIVERED: 'Delivered', CANCELLED: 'Cancelled' }
const currency = (amount) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(amount) || 0)
const orderDate = (date) => date ? new Date(date).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—'
const csv = (value) => `"${String(value ?? '').replaceAll('"', '""')}"`

function OrderIcon({ name, size = 19 }) {
  const paths = {
    cart: <><path d="M3 4h2l2.2 11.2a2 2 0 0 0 2 1.6h8.4a2 2 0 0 0 1.9-1.4L21 9H6"/><circle cx="10" cy="20" r="1"/><circle cx="18" cy="20" r="1"/></>,
    check: <><circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    cancel: <><circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></>,
    download: <><path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4"/></>,
    eye: <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z"/><circle cx="12" cy="12" r="2.5"/></>,
    truck: <><path d="M3 6h11v11H3zM14 10h4l3 3v4h-7z"/><circle cx="7.5" cy="19" r="1.5"/><circle cx="18" cy="19" r="1.5"/></>,
    more: <><circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/></>,
    filter: <><path d="M4 6h16M7 12h10m-7 6h4"/></>,
    close: <path d="m6 6 12 12M18 6 6 18"/>,
    info: <><circle cx="12" cy="12" r="9"/><path d="M12 11v5m0-8h.01"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

function SummaryCard({ title, value, tone, icon }) {
  return <article className={`seller-order-stat tone-${tone}`}><span><OrderIcon name={icon} size={25}/></span><div><strong>{value ?? '—'}</strong><small>{title}</small></div></article>
}

function statusClass(status) {
  if (status === 'DELIVERED') return 'delivered'
  if (status === 'CANCELLED') return 'cancelled'
  if (status === 'SHIPPED' || status === 'OUT_FOR_DELIVERY') return 'shipped'
  if (status === 'PAYMENT_PENDING') return 'pending'
  return status === 'PROCESSING' || status === 'CONFIRMED' ? 'processing' : 'new'
}

export default function Orders() {
  const navigate = useNavigate()
  const [pageData, setPageData] = useState(null)
  const [summary, setSummary] = useState(null)
  const [tab, setTab] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('')
  const [dateFilter, setDateFilter] = useState('30')
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [refreshKey, setRefreshKey] = useState(0)
  const [selected, setSelected] = useState(null)
  const [viewMode, setViewMode] = useState('')
  const [menuId, setMenuId] = useState(null)

  useEffect(() => {
    const timer = window.setTimeout(() => { setSearch(searchInput.trim()); setPage(0) }, 300)
    return () => window.clearTimeout(timer)
  }, [searchInput])

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    const now = Date.now()
    const from = dateFilter === 'all' ? undefined : new Date(now - Number(dateFilter) * 86400000).toISOString()
    const selectedStatus = statusFilter || tab
    try {
      const [orders, counts] = await Promise.all([
        getSellerOrders({ page, size: PAGE_SIZE, search: search || undefined, status: selectedStatus === 'ALL' ? undefined : selectedStatus, from }),
        getSellerOrderSummary(),
      ])
      setPageData(orders)
      setSummary(counts)
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not load seller orders.'))
    } finally { setLoading(false) }
  }, [dateFilter, page, refreshKey, search, statusFilter, tab])

  useEffect(() => { load() }, [load])

  const rows = pageData?.items || []
  const totalPages = pageData?.totalPages || 0
  const totalElements = pageData?.totalElements || 0
  const start = totalElements ? page * PAGE_SIZE + 1 : 0
  const end = Math.min((page + 1) * PAGE_SIZE, totalElements)
  const pages = useMemo(() => {
    const count = Math.min(totalPages, 5)
    const first = Math.max(0, Math.min(page - 2, totalPages - count))
    return Array.from({ length: count }, (_, index) => first + index)
  }, [page, totalPages])

  function changeTab(next) {
    setTab(next)
    setStatusFilter('')
    setPage(0)
  }

  function changeStatusFilter(value) {
    setStatusFilter(value)
    setTab('ALL')
    setPage(0)
  }

  async function exportOrders() {
    setError('')
    try {
      const params = {
        size: 100, search: search || undefined,
        status: statusFilter || (tab === 'ALL' ? undefined : tab),
        from: dateFilter === 'all' ? undefined : new Date(Date.now() - Number(dateFilter) * 86400000).toISOString(),
      }
      const firstPage = await getSellerOrders({ ...params, page: 0 })
      const remainingPages = await Promise.all(Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) => getSellerOrders({ ...params, page: index + 1 })))
      const allPages = [firstPage, ...remainingPages]
      const orders = allPages.flatMap((result) => result.items || [])
      const data = [['Order ID', 'Customer', 'Email', 'Products', 'Total', 'Status', 'Order Date'], ...orders.map((order) => [
        `ORD-${order.orderId}`, order.customerName, order.customerEmail,
        order.items.map((item) => `${item.productName} x ${item.quantity}`).join('; '), order.totalAmount,
        order.status, order.orderDate,
      ])].map((row) => row.map(csv).join(',')).join('\r\n')
      const url = URL.createObjectURL(new Blob([data], { type: 'text/csv;charset=utf-8' }))
      const link = document.createElement('a')
      link.href = url
      link.download = 'shopsphere-orders.csv'
      link.click()
      URL.revokeObjectURL(url)
    } catch (requestError) { setError(getRequestError(requestError, 'Could not export orders.')) }
  }

  async function advanceOrder(order) {
    const nextStatus = NEXT_STATUS[order.status]
    if (!nextStatus) return
    setBusyId(order.sellerOrderId)
    setMenuId(null)
    setError('')
    try {
      await updateSellerOrderStatus(order.sellerOrderId, nextStatus)
      setRefreshKey((current) => current + 1)
      if (selected?.sellerOrderId === order.sellerOrderId) setSelected({ ...order, status: nextStatus })
    } catch (requestError) { setError(getRequestError(requestError, 'Could not update this order status.')) }
    finally { setBusyId(null) }
  }

  return <SellerLayout onSearchChange={setSearchInput} searchValue={searchInput}>
    <main className="seller-orders-page">
      <header className="seller-orders-heading"><div className="seller-orders-title"><span><SellerIcon name="cart" size={30}/></span><div><h1>Orders Management</h1><p>View and manage orders containing your products</p></div></div><button className="seller-orders-export" onClick={exportOrders} type="button"><OrderIcon name="download"/> Export Orders</button></header>
      {error && <div className="seller-orders-alert" role="alert">{error}<button onClick={() => setError('')} type="button" aria-label="Dismiss">×</button></div>}

      <section aria-label="Order summary" className="seller-orders-stats">
        <SummaryCard title="Total Orders" value={summary?.totalOrders} tone="blue" icon="cart"/>
        <SummaryCard title="Delivered" value={summary?.delivered} tone="green" icon="check"/>
        <SummaryCard title="Processing" value={summary?.processing} tone="orange" icon="clock"/>
        <SummaryCard title="Cancelled" value={summary?.cancelled} tone="violet" icon="cancel"/>
      </section>

      <nav aria-label="Filter orders by status" className="seller-order-tabs">{TABS.map(([key, label, countKey]) => <button aria-pressed={tab === key && !statusFilter} className={tab === key && !statusFilter ? 'is-active' : ''} key={key} onClick={() => changeTab(key)} type="button">{label}<small>{summary?.[countKey] ?? '—'}</small></button>)}</nav>
      <section aria-label="Order filters" className="seller-order-filters">
        <label className="seller-order-search"><OrderIcon name="search"/><input aria-label="Search order ID or customer" onChange={(event) => setSearchInput(event.target.value)} placeholder="Search order by ID, customer..." value={searchInput}/></label>
        <label className="seller-order-select"><OrderIcon name="filter"/><select aria-label="Filter orders by status" onChange={(event) => changeStatusFilter(event.target.value)} value={statusFilter}><option value="">All Status</option><option value="NEW">New</option><option value="PROCESSING">Processing</option><option value="SHIPPED">Shipped</option><option value="DELIVERED">Delivered</option><option value="CANCELLED">Cancelled</option></select></label>
        <select aria-label="Filter orders by date" className="seller-order-date" onChange={(event) => { setDateFilter(event.target.value); setPage(0) }} value={dateFilter}><option value="7">Last 7 Days</option><option value="30">Last 30 Days</option><option value="90">Last 90 Days</option><option value="all">All Dates</option></select>
      </section>

      <section className="seller-orders-table-card" aria-label="Seller orders"><div className="seller-orders-scroll"><table className="seller-orders-table"><thead><tr><th>Order ID</th><th>Customer</th><th>Products</th><th>Total Amount</th><th>Status</th><th>Order Date</th><th>Actions</th></tr></thead>
        <tbody>{loading ? <tr><td className="seller-orders-empty" colSpan="7">Loading orders…</td></tr> : rows.length ? rows.map((order) => <tr key={order.sellerOrderId}>
          <td><strong className="seller-order-id">#ORD-{order.orderId}</strong></td>
          <td><div className="seller-order-customer"><span>{(order.customerName || 'C').slice(0, 1).toUpperCase()}</span><div><strong>{order.customerName}</strong><small>{order.customerEmail}</small></div></div></td>
          <td><div className="seller-order-products">{order.items.slice(0, 2).map((item) => <div className="seller-order-product" key={item.productId}><span>{item.imageUrl ? <img alt="" loading="lazy" src={item.imageUrl}/> : <OrderIcon name="cart" size={16}/>}</span><small>{item.productName} × {item.quantity}</small></div>)}{order.items.length > 2 && <small className="seller-order-more">+ {order.items.length - 2} more</small>}</div></td>
          <td className="seller-order-total">{currency(order.totalAmount)}</td>
          <td><span className={`seller-order-status ${statusClass(order.status)}`}><i/>{STATUS_LABELS[order.status] || order.status}</span></td>
          <td>{orderDate(order.orderDate)}</td>
          <td><div className="seller-order-actions"><button aria-label={`View order ${order.orderId}`} className="seller-order-action" onClick={() => navigate(`/seller/orders/${order.sellerOrderId}`)} title="View order" type="button"><OrderIcon name="eye" size={18}/></button><button aria-label={`Track order ${order.orderId}`} className="seller-order-action" onClick={() => navigate(`/seller/orders/${order.sellerOrderId}#tracking`)} title="Track order" type="button"><OrderIcon name="truck" size={18}/></button><div className="seller-order-menu-wrap"><button aria-label={`More actions for order ${order.orderId}`} aria-expanded={menuId === order.sellerOrderId} className="seller-order-action" onClick={() => setMenuId((current) => current === order.sellerOrderId ? null : order.sellerOrderId)} title="More actions" type="button"><OrderIcon name="more"/></button>{menuId === order.sellerOrderId && <div className="seller-order-menu"><button disabled={!NEXT_STATUS[order.status] || busyId === order.sellerOrderId} onClick={() => advanceOrder(order)} type="button">{NEXT_STATUS[order.status] ? `Mark ${STATUS_LABELS[NEXT_STATUS[order.status]]}` : 'No further status'}</button></div>}</div></div></td>
        </tr>) : <tr><td className="seller-orders-empty" colSpan="7">No orders match your search and filters.</td></tr>}</tbody>
      </table></div></section>

      <footer className="seller-orders-pagination"><p>Showing <strong>{start}–{end}</strong> of <strong>{totalElements}</strong> orders</p><div><button aria-label="Previous page" disabled={page <= 0 || loading} onClick={() => setPage((value) => Math.max(0, value - 1))} type="button">‹</button>{pages.map((number) => <button aria-current={page === number ? 'page' : undefined} className={page === number ? 'is-current' : ''} key={number} onClick={() => setPage(number)} type="button">{number + 1}</button>)}<button aria-label="Next page" disabled={page >= totalPages - 1 || loading} onClick={() => setPage((value) => Math.min(totalPages - 1, value + 1))} type="button">›</button></div></footer>
    </main>

    {selected && viewMode && <div className="seller-order-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) { setSelected(null); setViewMode('') } }}><section aria-labelledby="seller-order-modal-title" aria-modal="true" className="seller-order-modal" role="dialog"><button aria-label="Close" className="seller-order-modal-close" onClick={() => { setSelected(null); setViewMode('') }} type="button"><OrderIcon name="close"/></button>
      <h2 id="seller-order-modal-title">{viewMode === 'tracking' ? 'Order tracking' : 'Order details'}</h2><p className="seller-order-modal-subtitle">#ORD-{selected.orderId} · {orderDate(selected.orderDate)}</p>
      {viewMode === 'details' ? <><div className="seller-order-detail-grid"><div><small>Customer</small><strong>{selected.customerName}</strong><span>{selected.customerEmail}</span></div><div><small>Payment</small><strong>{selected.paymentMethod?.replaceAll('_', ' ')}</strong><span>{selected.paymentStatus?.replaceAll('_', ' ')}</span></div><div><small>Ship to</small><strong>{selected.shippingName}</strong><span>{selected.shippingMobile}</span><span>{selected.shippingAddress}</span></div><div><small>Order total</small><strong>{currency(selected.totalAmount)}</strong><span className={`seller-order-status ${statusClass(selected.status)}`}><i/>{STATUS_LABELS[selected.status] || selected.status}</span></div></div><div className="seller-order-detail-items"><h3>Products</h3>{selected.items.map((item) => <div key={item.productId}><span>{item.imageUrl ? <img alt="" src={item.imageUrl}/> : <OrderIcon name="cart"/>}</span><strong>{item.productName} <small>× {item.quantity}</small></strong><b>{currency(item.lineSubtotal)}</b></div>)}</div>{NEXT_STATUS[selected.status] && <button className="seller-order-primary" disabled={busyId === selected.sellerOrderId} onClick={() => advanceOrder(selected)} type="button">{busyId === selected.sellerOrderId ? 'Updating…' : `Mark ${STATUS_LABELS[NEXT_STATUS[selected.status]]}`}</button>}</> : <div className="seller-order-timeline">{selected.history?.length ? selected.history.map((event, index) => <article className={index === selected.history.length - 1 ? 'is-latest' : ''} key={`${event.status}-${event.changedAt}`}><span/><div><strong>{STATUS_LABELS[event.status] || event.status}</strong><small>{orderDate(event.changedAt)}</small>{event.note && <p>{event.note}</p>}</div></article>) : <p className="seller-order-no-history">No tracking updates yet.</p>}</div>}
    </section></div>}
  </SellerLayout>
}
