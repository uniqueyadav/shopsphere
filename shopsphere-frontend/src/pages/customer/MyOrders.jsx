import { useEffect, useMemo, useState } from 'react'
import { useDispatch } from 'react-redux'
import { Link, useNavigate } from 'react-router-dom'
import MainNavbar from '../../components/navbar/MainNavbar'
import CustomerAccountSidebar from '../../components/customer/CustomerAccountSidebar'
import { DashboardIcon } from '../../components/customer/CustomerAccountSidebar'
import { addCartItem, fetchCart } from '../../features/cart/cartSlice'
import { getRequestError } from '../../services/authService'
import { cancelOrder, getMyOrders } from '../../services/orderService'
import { formatProductPrice } from '../../components/product/ProductCard'
import { canCancelOrder, getOrderStatusLabel, reorderOrder } from './orderUtils'
import './Shop.css'
import './OrderHistory.css'
import './CustomerDashboard.css'

const FILTERS = [
  { id: 'ALL', label: 'All Orders' },
  { id: 'PROCESSING', label: 'Processing' },
  { id: 'SHIPPED', label: 'Shipped' },
  { id: 'DELIVERED', label: 'Delivered' },
  { id: 'CANCELLED', label: 'Cancelled' },
  { id: 'RETURNS', label: 'Returns' },
]

function orderCode(order) {
  const date = new Date(order.createdAt)
  const stamp = Number.isNaN(date.getTime()) ? '00000000' : date.toISOString().slice(0, 10).replaceAll('-', '')
  return `SS${stamp}${String(order.id).padStart(5, '0')}`
}

function placedDate(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date unavailable'
  return date.toLocaleString('en-IN', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function matchesFilter(order, filter) {
  if (filter === 'ALL') return true
  if (filter === 'RETURNS') return order.refundStatus && order.refundStatus !== 'NOT_REQUESTED'
  if (filter === 'PROCESSING') return ['PAYMENT_PENDING', 'PLACED', 'CONFIRMED', 'PROCESSING'].includes(order.orderStatus)
  if (filter === 'SHIPPED') return ['SHIPPED', 'OUT_FOR_DELIVERY'].includes(order.orderStatus)
  return order.orderStatus === filter
}

function statusTone(status) {
  if (status === 'DELIVERED') return 'delivered'
  if (status === 'CANCELLED') return 'cancelled'
  if (['SHIPPED', 'OUT_FOR_DELIVERY'].includes(status)) return 'shipped'
  return 'processing'
}

function progressState(status) {
  const stages = ['PLACED', 'PROCESSING', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED']
  if (status === 'CANCELLED') return -1
  if (status === 'PAYMENT_PENDING') return 0
  if (status === 'CONFIRMED') return 1
  return stages.indexOf(status)
}

function OrderProgress({ order }) {
  const stage = progressState(order.orderStatus)
  if (stage < 0) {
    return <div className="customer-cancelled-summary"><span><DashboardIcon name="close" /></span><div><strong>Order Cancelled</strong><small>Placed {placedDate(order.createdAt)}</small></div></div>
  }
  const stages = [
    ['PLACED', 'Order Placed'], ['PROCESSING', 'Packed'], ['SHIPPED', 'Shipped'],
    ['OUT_FOR_DELIVERY', 'Out for Delivery'], ['DELIVERED', 'Delivered'],
  ]
  const currentIndex = Math.max(0, stage)
  const visibleStages = order.orderStatus === 'DELIVERED' ? [stages[0], stages[2], stages[4]] : stages.slice(0, Math.max(3, currentIndex + 1))
  return (
    <div className="customer-progress" aria-label={`Order status: ${getOrderStatusLabel(order.orderStatus)}`}>
      {visibleStages.map(([id, label], index) => {
        const actualIndex = stages.findIndex(([stageId]) => stageId === id)
        const complete = actualIndex <= currentIndex
        const current = actualIndex === currentIndex
        return <div className={`customer-progress-step${complete ? ' is-complete' : ''}${current ? ' is-current' : ''}`} key={id}>
          <span className="customer-progress-dot">{complete ? (current ? '•' : '✓') : ''}</span>
          {index < visibleStages.length - 1 && <span className={`customer-progress-line${actualIndex < currentIndex ? ' is-complete' : ''}`} />}
          <strong>{label}</strong>
          <small>{id === 'PLACED' ? placedDate(order.createdAt) : current ? 'Latest update' : ' '}</small>
        </div>
      })}
    </div>
  )
}

function OrderCard({ order, onReorder, onCancel, working }) {
  const items = (order.sellerOrders || []).flatMap((sellerOrder) => (sellerOrder.items || []).map((item) => ({ ...item, sellerName: sellerOrder.sellerName })))
  const canReorder = ['DELIVERED', 'CANCELLED'].includes(order.orderStatus)
  return (
    <article className="customer-order-card">
      <header className="customer-order-top">
        <div className="customer-order-reference">
          <strong>Order ID: #{orderCode(order)}</strong>
          <button type="button" title="Copy order ID" onClick={() => navigator.clipboard?.writeText(orderCode(order))}><DashboardIcon name="copy" /></button>
          <span>Placed on: {placedDate(order.createdAt)}</span>
        </div>
        <span className={`customer-order-status customer-order-status--${statusTone(order.orderStatus)}`}>
          <i aria-hidden="true"><DashboardIcon name={order.orderStatus === 'DELIVERED' ? 'check' : order.orderStatus === 'CANCELLED' ? 'close' : 'truck'} /></i>
          {getOrderStatusLabel(order.orderStatus)}
        </span>
        <div className="customer-order-total">Total Amount: <strong>{formatProductPrice(order.totalAmount)}</strong></div>
      </header>
      <div className="customer-order-body">
        <div className="customer-order-products" aria-label={`${items.length} products in order`}>
          {items.slice(0, 3).map((item, index) => <Link className="customer-order-product" key={`${item.productId}-${index}`} to={`/products/${item.productId}`} title={item.productName}>
            {item.imageUrl ? <img src={item.imageUrl} alt={item.productName} /> : <span className="customer-product-placeholder">{item.productName?.slice(0, 1) || 'S'}</span>}
          </Link>)}
          {items.length > 3 && <span className="customer-order-more">+{items.length - 3} More</span>}
          {!items.length && <span className="customer-product-placeholder">SS</span>}
        </div>
        <OrderProgress order={order} />
        <div className="customer-order-actions">
          {order.orderStatus !== 'CANCELLED' && order.orderStatus !== 'DELIVERED' && <Link className="customer-action-primary" to={`/orders/${order.id}/tracking`}><DashboardIcon name="pin" />Track Order</Link>}
          {canReorder && <button className="customer-action-primary" disabled={working} onClick={() => onReorder(order)} type="button"><DashboardIcon name="cart" />{working ? 'Adding…' : 'Buy Again'}</button>}
          {canCancelOrder(order) && <button className="customer-action-outline" disabled={working} onClick={() => onCancel(order)} type="button">Cancel Order</button>}
          <Link className="customer-action-outline" to={`/orders/${order.id}`}>View Details <DashboardIcon name="arrow" /></Link>
        </div>
      </div>
    </article>
  )
}

export default function MyOrders() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const [orders, setOrders] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [productSearch, setProductSearch] = useState('')
  const [page, setPage] = useState(1)
  const [workingOrder, setWorkingOrder] = useState(null)
  const [notice, setNotice] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)
  const pageSize = 3

  useEffect(() => {
    let active = true
    getMyOrders().then((data) => {
      if (active) { setOrders(data); setStatus('succeeded') }
    }).catch((requestError) => {
      if (active) { setError(getRequestError(requestError, 'Your orders could not be loaded.')); setStatus('failed') }
    })
    return () => { active = false }
  }, [reloadKey])

  const counts = useMemo(() => Object.fromEntries(FILTERS.map(({ id }) => [id, orders.filter((order) => matchesFilter(order, id)).length])), [orders])
  const filteredOrders = useMemo(() => orders.filter((order) => matchesFilter(order, filter) && (!search || `${orderCode(order)} ${order.id} ${order.sellerOrders?.flatMap((seller) => seller.items || []).map((item) => item.productName).join(' ')}`.toLowerCase().includes(search.toLowerCase()))), [filter, orders, search])
  const pageCount = Math.max(1, Math.ceil(filteredOrders.length / pageSize))
  const pageOrders = filteredOrders.slice((page - 1) * pageSize, page * pageSize)

  async function handleReorder(order) {
    setWorkingOrder(order.id); setNotice(null)
    const result = await reorderOrder(order, (item) => dispatch(addCartItem(item)).unwrap())
    await dispatch(fetchCart()); setWorkingOrder(null)
    setNotice(result.failedItems.length ? `${result.addedItems} item${result.addedItems === 1 ? '' : 's'} added. ${result.failedItems.length} unavailable item${result.failedItems.length === 1 ? '' : 's'} skipped.` : 'Order items were added to your cart.')
  }

  async function handleCancel(order) {
    if (!window.confirm('Cancel this order?')) return
    setWorkingOrder(order.id); setNotice(null)
    try {
      await cancelOrder(order.id)
      const refreshed = await getMyOrders(); setOrders(refreshed); setNotice('Your order was cancelled.')
    } catch (requestError) { setNotice(getRequestError(requestError, 'This order could not be cancelled.')) }
    finally { setWorkingOrder(null) }
  }

  return <div className="marketplace-page shop-page customer-dashboard-page">
    <MainNavbar
      shopVariant
      onSearchChange={setProductSearch}
      onSearchSubmit={(event) => {
        event.preventDefault()
        navigate(`/shop?search=${encodeURIComponent(productSearch)}`)
      }}
      searchValue={productSearch}
    />
    <nav className="shop-secondary-nav" aria-label="Shop navigation">
      <div className="shop-secondary-inner">
        <Link to="/shop">▦ &nbsp; All Categories <span>⌄</span></Link>
        <Link to="/shop?discountedOnly=true">Deals</Link>
        <Link to="/shop?sort=newest">New Arrivals</Link>
        <Link to="/shop?sort=popular">Best Sellers</Link>
        <Link to="/orders">Track Order</Link>
        <a href="mailto:support@shopsphere.com">Customer Support</a>
      </div>
    </nav>

    <main className="customer-dashboard-layout">
      <CustomerAccountSidebar active="My Orders" />

      <section className="customer-orders-main">
        <header className="customer-orders-heading"><div><h1>My Orders</h1><p>Track and manage your orders</p></div><label className="customer-order-search"><span>⌕</span><input aria-label="Search orders" onChange={(event) => { setSearch(event.target.value); setPage(1) }} placeholder="Search orders..." value={search} /></label></header>
        <div className="customer-order-tabs" role="tablist" aria-label="Order status filters">{FILTERS.map(({ id, label }) => <button aria-selected={filter === id} className={filter === id ? 'is-active' : ''} key={id} onClick={() => { setFilter(id); setPage(1) }} role="tab" type="button">{label} ({counts[id] || 0})</button>)}</div>
        {notice && <p className="customer-dashboard-notice" role="status">{notice}</p>}
        {status === 'loading' && <div className="customer-dashboard-loading" role="status">Loading your orders…</div>}
        {status === 'failed' && <div className="customer-dashboard-empty"><h2>We couldn’t load your orders</h2><p>{error}</p><button onClick={() => { setStatus('loading'); setReloadKey((key) => key + 1) }} type="button">Try again</button></div>}
        {status === 'succeeded' && orders.length === 0 && <div className="customer-dashboard-empty"><h2>Your orders will appear here</h2><p>Explore ShopSphere and find something you love.</p><Link to="/shop">Start shopping</Link></div>}
        {status === 'succeeded' && orders.length > 0 && pageOrders.length === 0 && <div className="customer-dashboard-empty"><h2>No matching orders</h2><p>Try a different status or search term.</p></div>}
        {status === 'succeeded' && pageOrders.length > 0 && <div className="customer-orders-list">{pageOrders.map((order) => <OrderCard key={order.id} onCancel={handleCancel} onReorder={handleReorder} order={order} working={workingOrder === order.id} />)}</div>}
        {status === 'succeeded' && filteredOrders.length > pageSize && <nav className="customer-pagination" aria-label="Order pages"><button aria-label="Previous page" disabled={page <= 1} onClick={() => setPage((current) => Math.max(1, current - 1))} type="button">‹</button>{Array.from({ length: pageCount }, (_, index) => index + 1).map((number) => <button aria-current={page === number ? 'page' : undefined} className={page === number ? 'is-active' : ''} key={number} onClick={() => setPage(number)} type="button">{number}</button>)}<button aria-label="Next page" disabled={page >= pageCount} onClick={() => setPage((current) => Math.min(pageCount, current + 1))} type="button">›</button></nav>}
      </section>
    </main>
  </div>
}
