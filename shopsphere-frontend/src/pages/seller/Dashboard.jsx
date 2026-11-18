import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import SellerLayout, { SellerIcon } from '../../layouts/SellerLayout'
import { getRequestError } from '../../services/authService'
import { getSellerDashboard } from '../../services/sellerService'
import './SellerDashboard.css'
import './SellerTypography.css'

const PERIODS = [['SEVEN_DAYS', 'Last 7 Days'], ['THIRTY_DAYS', 'Last 30 Days'], ['SIX_MONTHS', 'Last 6 Months'], ['ONE_YEAR', 'Last 1 Year'], ['ALL_TIME', 'All Time']]
const money = (value) => value == null ? '—' : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(Number(value))
const growth = (value) => value == null ? null : `${Number(value) > 0 ? '+' : ''}${Number(value).toFixed(1)}%`
const dateLabel = (value) => new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short' }).format(new Date(value))

function SalesChart({ points }) {
  if (!points?.length) return <div className="sd-chart-empty">No paid sales recorded for this period.</div>
  const width = 700
  const height = 230
  const padding = { top: 15, right: 14, bottom: 34, left: 54 }
  const values = points.map((point) => Number(point.sales || 0))
  const maximum = Math.max(...values, 1)
  const coordinates = values.map((value, index) => ({
    x: padding.left + (points.length === 1 ? (width - padding.left - padding.right) / 2 : index * (width - padding.left - padding.right) / (points.length - 1)),
    y: padding.top + (1 - value / maximum) * (height - padding.top - padding.bottom),
  }))
  const line = coordinates.map((point) => `${point.x},${point.y}`).join(' ')
  const area = `${coordinates[0].x},${height - padding.bottom} ${line} ${coordinates.at(-1).x},${height - padding.bottom}`
  const ticks = [0, .25, .5, .75, 1]
  return <div className="sd-chart-scroll"><svg className="sd-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Seller sales overview chart">
    {ticks.map((fraction) => { const y = padding.top + (1 - fraction) * (height - padding.top - padding.bottom); return <g key={fraction}><line x1={padding.left} x2={width - padding.right} y1={y} y2={y} className="sd-grid-line"/><text x={padding.left - 9} y={y + 4} textAnchor="end" className="sd-axis-label">{money(maximum * fraction)}</text></g> })}
    <polygon points={area} className="sd-chart-area"/><polyline points={line} className="sd-chart-line"/>
    {coordinates.map((point, index) => <circle key={`${points[index].date}-${index}`} cx={point.x} cy={point.y} r="3.5" className="sd-chart-dot"><title>{dateLabel(points[index].date)} · {money(values[index])}</title></circle>)}
    {points.filter((_, index) => index === 0 || index === points.length - 1 || index % Math.ceil(points.length / 5) === 0).map((point) => { const index = points.indexOf(point); return <text key={`${point.date}-label`} x={coordinates[index].x} y={height - 10} textAnchor="middle" className="sd-axis-label">{dateLabel(point.date)}</text> })}
  </svg></div>
}

function MetricCard({ icon, label, value, change, tone, note }) {
  return <article className={`sd-metric sd-metric-${tone}`}><span className="sd-metric-icon"><SellerIcon name={icon} size={23}/></span><div className="sd-metric-copy"><small>{label}</small><strong>{value}</strong>{change != null ? <em className={Number(change) < 0 ? 'is-down' : 'is-up'}>{Number(change) > 0 ? '↑' : Number(change) < 0 ? '↓' : '↔'} {growth(change)} <span>vs previous period</span></em> : <em>{note}</em>}</div></article>
}

export default function Dashboard() {
  const [period, setPeriod] = useState('THIRTY_DAYS')
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')

  async function loadDashboard() {
    setLoading(true)
    setError('')
    try { setDashboard(await getSellerDashboard(period)) }
    catch (requestError) { setError(getRequestError(requestError, 'Could not load your seller dashboard.')) }
    finally { setLoading(false) }
  }

  useEffect(() => { loadDashboard() }, [period])

  const query = search.trim().toLowerCase()
  const topProducts = useMemo(() => (dashboard?.topProducts || []).filter((product) => !query || product.name?.toLowerCase().includes(query)), [dashboard, query])
  const recentOrders = useMemo(() => (dashboard?.recentOrders || []).filter((order) => !query || `${order.orderId} ${order.customerName} ${order.status}`.toLowerCase().includes(query)), [dashboard, query])
  const maxUnits = Math.max(...(dashboard?.topProducts || []).map((product) => Number(product.unitsSold || 0)), 1)
  const shortName = dashboard?.sellerName?.trim().split(/\s+/)[0] || 'Seller'

  return <SellerLayout onSearchChange={setSearch} searchValue={search} storeName={dashboard?.storeName}>
    <main className="seller-dashboard-page">
      <header className="sd-welcome"><div><h1>Welcome back, {shortName}!</h1><p>Here’s what’s happening with your store today.</p></div><label className="sd-period"><span>Reporting period</span><select onChange={(event) => setPeriod(event.target.value)} value={period}>{PERIODS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label></header>
      {error && <div className="sd-error" role="alert"><span>{error}</span><button onClick={loadDashboard} type="button">Retry</button></div>}
      <section className="sd-metrics" aria-label="Seller performance summary">
        <MetricCard change={dashboard?.sales?.growthPercent} icon="wallet" label="Total Sales" note="No paid sales in this period" tone="blue" value={loading ? 'Loading…' : money(dashboard?.sales?.value)}/>
        <MetricCard change={dashboard?.orders?.growthPercent} icon="cart" label="Total Orders" note="No orders in this period" tone="cyan" value={loading ? 'Loading…' : dashboard?.orders?.value == null ? '—' : Number(dashboard.orders.value).toLocaleString('en-IN')}/>
        <MetricCard icon="wallet" label="Earnings" note="Payout totals are not available yet" tone="violet" value="—"/>
        <MetricCard icon="star" label="Store Rating" note={dashboard?.reviewCount ? `${dashboard.reviewCount} approved reviews` : 'No approved reviews yet'} tone="yellow" value={loading ? 'Loading…' : dashboard?.storeRating == null ? '—' : `${Number(dashboard.storeRating).toFixed(1)} / 5`}/>
      </section>

      <section className="sd-primary-grid">
        <article className="sd-panel sd-sales-panel"><header className="sd-panel-heading"><div><span className="sd-heading-icon blue"><SellerIcon name="chart"/></span><div><h2>Sales Overview</h2><p>Paid seller order subtotal by day</p></div></div><select aria-label="Sales chart period" onChange={(event) => setPeriod(event.target.value)} value={period}>{PERIODS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></header>{loading ? <div className="sd-panel-loading" role="status">Loading sales data…</div> : <SalesChart points={dashboard?.trend}/>}</article>
        <article className="sd-panel sd-products-panel"><header className="sd-panel-heading"><div><span className="sd-heading-icon orange"><SellerIcon name="star"/></span><div><h2>Top Selling Products</h2><p>Ranked by units sold in this period</p></div></div><Link to="/seller/products">View all</Link></header>{loading ? <div className="sd-panel-loading" role="status">Loading product data…</div> : topProducts.length ? <div className="sd-top-products">{topProducts.map((product, index) => <article className="sd-product-row" key={product.id}><strong className="sd-rank">{index + 1}</strong><span className="sd-product-image">{product.imageUrl ? <img alt="" src={product.imageUrl}/> : <SellerIcon name="box" size={24}/>}</span><div className="sd-product-details"><strong>{product.name}</strong><div className="sd-progress"><span style={{ width: `${Math.max(5, Number(product.unitsSold) / maxUnits * 100)}%` }}/></div></div><strong className="sd-product-sales">{money(product.sales)}</strong><small>{product.unitsSold} sold</small></article>)}</div> : <div className="sd-empty">No paid product sales for this period.</div>}</article>
      </section>

      <section className="sd-secondary-grid">
        <article className="sd-panel sd-orders-panel"><header className="sd-panel-heading"><div><span className="sd-heading-icon blue"><SellerIcon name="cart"/></span><div><h2>Recent Orders</h2><p>Latest orders assigned to your store</p></div></div></header>{loading ? <div className="sd-panel-loading" role="status">Loading recent orders…</div> : recentOrders.length ? <div className="sd-table-wrap"><table><thead><tr><th>Order ID</th><th>Customer</th><th>Amount</th><th>Status</th><th>Date</th></tr></thead><tbody>{recentOrders.map((order) => <tr key={order.sellerOrderId}><td>#{order.orderId}</td><td>{order.customerName}</td><td>{money(order.amount)}</td><td><span className={`sd-status sd-status-${order.status.toLowerCase().replaceAll('_', '-')}`}>{order.status.replaceAll('_', ' ')}</span></td><td>{new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(order.createdAt))}</td></tr>)}</tbody></table></div> : <div className="sd-empty">No orders were found for this period.</div>}</article>
        <article className="sd-panel sd-actions-panel"><header className="sd-panel-heading"><div><span className="sd-heading-icon blue"><SellerIcon name="plus"/></span><div><h2>Quick Actions</h2><p>Common store tasks</p></div></div></header><div className="sd-quick-actions"><Link className="sd-quick-action add" to="/seller/products/new"><SellerIcon name="plus"/><strong>Add Product</strong></Link><span className="sd-quick-action inventory" aria-disabled="true" title="Inventory page is not available yet"><SellerIcon name="inventory"/><strong>Manage Inventory</strong></span><span className="sd-quick-action reports" aria-disabled="true" title="Sales reports page is not available yet"><SellerIcon name="chart"/><strong>View Reports</strong></span><span className="sd-quick-action support" aria-disabled="true" title="Seller support page is not available yet"><SellerIcon name="help"/><strong>Get Support</strong></span></div>{dashboard?.lowStockProductCount > 0 && <Link className="sd-stock-alert" to="/seller/products">{dashboard.lowStockProductCount} approved products have 5 or fewer units in stock. Review inventory.</Link>}</article>
      </section>
    </main>
  </SellerLayout>
}
