import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AdminLayout from '../../layouts/AdminLayout'
import { getRequestError } from '../../services/authService'
import { getAdminCourierDashboard } from '../../services/deliveryService'
import './CourierDashboard.css'

const fmt = (value) => new Intl.NumberFormat('en-IN').format(value || 0)
const shortDate = (value) => new Date(`${value}T00:00:00`).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })
const statusLabel = (value) => String(value || '').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())

function TrendChart({ rows }) {
  const width = 760; const height = 220; const pad = 28
  const max = Math.max(1, ...rows.map((r) => Math.max(r.total, r.successful, r.failed)))
  const point = (value, index) => `${pad + index * ((width - pad * 2) / Math.max(rows.length - 1, 1))},${height - pad - value / max * (height - pad * 2)}`
  const line = (key) => rows.map((r, i) => `${i ? 'L' : 'M'}${point(r[key], i)}`).join(' ')
  const labels = rows.filter((_, i) => i === 0 || i === rows.length - 1 || i % Math.max(1, Math.ceil(rows.length / 6)) === 0)
  return <div className="courier-chart-scroll"><svg className="courier-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Daily deliveries, completed deliveries and cancellations trend">
    {[0, 1, 2, 3].map((i) => <line key={i} x1={pad} x2={width - pad} y1={pad + i * ((height - pad * 2) / 3)} y2={pad + i * ((height - pad * 2) / 3)} className="courier-gridline"/>)}
    <path d={line('total')} className="courier-line courier-line-total"/><path d={line('successful')} className="courier-line courier-line-success"/><path d={line('failed')} className="courier-line courier-line-failed"/>
    {labels.map((r) => { const i = rows.indexOf(r); return <text key={r.date} x={pad + i * ((width - pad * 2) / Math.max(rows.length - 1, 1))} y={height - 4} textAnchor="middle" className="courier-axis-label">{shortDate(r.date)}</text> })}
  </svg></div>
}

export default function CourierDashboard() {
  const [days, setDays] = useState(30)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const navigate = useNavigate()
  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { setData(await getAdminCourierDashboard(days)) } catch (e) { setError(getRequestError(e, 'Courier dashboard could not be loaded.')) } finally { setLoading(false) }
  }, [days])
  useEffect(() => { load() }, [load])
  const recent = useMemo(() => (data?.recentDeliveries || []).filter((row) => `${row.orderId} ${row.customerName} ${row.fulfillmentPartner} ${row.city}`.toLowerCase().includes(search.toLowerCase())), [data, search])
  const cards = [
    ['Fulfillment Orders', data?.totalDeliveries, '🚚', 'violet'],
    ['Delivered', data?.successfulDeliveries, '✓', 'green'],
    ['In Transit', data?.inTransit, '◷', 'amber'],
    ['Cancelled', data?.failedDeliveries, '×', 'red'],
  ]
  const rate = data?.totalDeliveries ? Math.round(data.successfulDeliveries / data.totalDeliveries * 100) : 0
  return <AdminLayout pageTitle="Courier Dashboard" pageSubtitle="Track order fulfillment, delivery trends and courier performance." breadcrumb="Courier Dashboard" showDateFilter={false} onExport={() => navigate('/admin/delivery-partners')} exportLabel="Manage Partners" titleIcon="truck">
    <main className="courier-dashboard">
      <div className="courier-toolbar"><span>Fulfillment overview for the selected period</span><label>Period <select value={days} onChange={(e) => setDays(Number(e.target.value))}><option value={7}>Last 7 days</option><option value={30}>Last 30 days</option><option value={90}>Last 90 days</option></select></label></div>
      {error && <div className="courier-error" role="alert">{error}<button onClick={load} type="button">Retry</button></div>}
      <section className="courier-kpis">{cards.map(([label, value, icon, tone]) => <article className="courier-kpi" key={label}><span className={`courier-kpi-icon ${tone}`}>{icon}</span><div><small>{label}</small><strong>{loading ? '—' : fmt(value)}</strong><span>{days} day period</span></div></article>)}</section>
      <section className="courier-main-grid">
        <article className="courier-panel courier-trend"><header><div><h2>Delivery Trend</h2><p>Fulfillment order status over time</p></div><div className="courier-legend"><span><i className="violet-dot"/>All orders</span><span><i className="green-dot"/>Delivered</span><span><i className="red-dot"/>Cancelled</span></div></header>{loading ? <div className="courier-empty">Loading trend…</div> : data?.trend?.length ? <TrendChart rows={data.trend}/> : <div className="courier-empty">No fulfillment activity in this period yet.</div>}</article>
        <article className="courier-panel courier-performance"><header><div><h2>Fulfillment Snapshot</h2><p>Current period completion</p></div></header><div className="courier-ring-wrap"><div className="courier-ring" style={{ '--rate': `${rate}%` }}><strong>{rate}%</strong><small>Delivered</small></div></div><div className="courier-snapshot"><span><i className="green-dot"/>Delivered<strong>{fmt(data?.successfulDeliveries)}</strong></span><span><i className="blue-dot"/>In transit<strong>{fmt(data?.inTransit)}</strong></span><span><i className="red-dot"/>Cancelled<strong>{fmt(data?.failedDeliveries)}</strong></span></div><p className="courier-note">Delivery partner assignment and live GPS are not yet recorded for orders.</p></article>
      </section>
      <section className="courier-panel courier-recent"><header><div><h2>Recent Deliveries</h2><p>Latest seller shipments created during this period</p></div><label className="courier-table-search"><span>⌕</span><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search orders or customers" aria-label="Search recent deliveries"/></label></header><div className="courier-table-scroll"><table><thead><tr><th>Order</th><th>Customer</th><th>Fulfillment Partner</th><th>Destination</th><th>Status</th><th>Created</th></tr></thead><tbody>{loading ? <tr><td colSpan="6" className="courier-empty">Loading deliveries…</td></tr> : recent.length ? recent.map((row) => <tr key={row.sellerOrderId}><td><strong>#{row.orderId}</strong><small>Shipment #{row.sellerOrderId}</small></td><td>{row.customerName}</td><td>{row.fulfillmentPartner}</td><td>{row.city || '—'}</td><td><span className={`courier-status ${String(row.status).toLowerCase()}`}>{statusLabel(row.status)}</span></td><td>{new Date(row.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</td></tr>) : <tr><td colSpan="6" className="courier-empty">No deliveries match this search.</td></tr>}</tbody></table></div><footer><span>Showing {recent.length} recent shipments</span><button onClick={() => navigate('/admin/delivery-partners')} type="button">Manage delivery partners →</button></footer></section>
    </main>
  </AdminLayout>
}
