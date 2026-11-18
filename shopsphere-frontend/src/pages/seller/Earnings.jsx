import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import SellerLayout, { SellerIcon } from '../../layouts/SellerLayout'
import { getRequestError } from '../../services/authService'
import { getSellerEarnings } from '../../services/sellerService'
import './SellerEarnings.css'

const money = (value) => value == null ? 'Not tracked' : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value) || 0)
const dateText = (value) => value ? new Date(value).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'
const dateInput = (date) => { const d = new Date(date); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }
const palette = ['#0864f7', '#ff9d1c', '#17b96b', '#b64be8', '#6eb9fa', '#ee6582', '#6d79e8']

function EarningsIcon({ name, size = 22 }) {
  const paths = {
    sales: <><path d="M3 19V9m6 10V5m6 14v-7m6 7V3M2 20h20"/></>,
    wallet: <><rect x="3" y="5" width="18" height="15" rx="2"/><path d="M3 9h18m-5 5h2"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    check: <><circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/></>,
    download: <><path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4"/></>,
    card: <><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 10h18m-14 5h4"/></>,
    settings: <><circle cx="12" cy="12" r="3"/><path d="m19 13 2 1-2 4-2-1a8 8 0 0 1-2 1l-.5 2h-5L9 18a8 8 0 0 1-2-1l-2 1-2-4 2-1v-2l-2-1 2-4 2 1a8 8 0 0 1 2-1l.5-2h5l.5 2a8 8 0 0 1 2 1l2-1 2 4-2 1z"/></>,
    arrow: <><path d="M5 12h14m-6-6 6 6-6 6"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

function SummaryCard({ title, value, tone, icon, hint }) {
  return <article className={`seller-earning-stat ${tone}`}><span><EarningsIcon name={icon} size={25}/></span><div><small>{title}</small><strong>{value}</strong>{hint && <em>{hint}</em>}</div></article>
}

function TrendChart({ points }) {
  const width = 700, height = 230, pad = { top: 15, right: 16, bottom: 34, left: 62 }
  const values = (points || []).flatMap((point) => [Number(point.sales) || 0, Number(point.earnings) || 0])
  const max = Math.max(...values, 1) * 1.1
  const x = (index) => pad.left + (points.length < 2 ? (width - pad.left - pad.right) / 2 : index * (width - pad.left - pad.right) / (points.length - 1))
  const y = (value) => height - pad.bottom - (Number(value || 0) / max) * (height - pad.top - pad.bottom)
  const line = (key) => points.map((point, index) => `${index ? 'L' : 'M'} ${x(index)} ${y(point[key])}`).join(' ')
  if (!points?.length) return <div className="seller-earning-empty">No paid sales in this date range.</div>
  const labels = points.length > 5 ? [points[0], points[Math.floor((points.length - 1) / 2)], points.at(-1)] : points
  const grid = [0, .25, .5, .75, 1]
  return <div className="seller-earning-chart-wrap"><svg className="seller-earning-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Sales and estimated earnings trend">
    {grid.map((ratio) => { const value = max * ratio; const yy = y(value); return <g key={ratio}><line x1={pad.left} x2={width - pad.right} y1={yy} y2={yy} stroke="#e5ebfa"/><text x={pad.left - 10} y={yy + 4} textAnchor="end">{value >= 1000 ? `₹${Math.round(value / 1000)}k` : `₹${Math.round(value)}`}</text></g> })}
    {points.length > 1 && <path d={`${line('earnings')} L ${x(points.length - 1)} ${height - pad.bottom} L ${x(0)} ${height - pad.bottom} Z`} fill="#0b65f51c"/>}
    <path d={line('sales')} fill="none" stroke="#ff9817" strokeWidth="3" strokeLinejoin="round" strokeLinecap="round"/>
    {points.some((point) => point.earnings != null) && <path d={line('earnings')} fill="none" stroke="#0864f7" strokeWidth="3" strokeLinejoin="round" strokeLinecap="round"/>}
    {points.map((point, index) => <g key={point.date}><circle cx={x(index)} cy={y(point.sales)} r="4" fill="#ff9817"/>{point.earnings != null && <circle cx={x(index)} cy={y(point.earnings)} r="4" fill="#0864f7"/>}</g>)}
    {labels.map((point) => { const index = points.indexOf(point); return <text key={point.date} x={x(index)} y={height - 8} textAnchor="middle">{dateText(point.date)}</text> })}
  </svg></div>
}

function Donut({ categories }) {
  const total = categories.reduce((sum, item) => sum + Number(item.sales || 0), 0)
  let current = 0
  const stops = categories.map((item, index) => { const start = current; current += total ? Number(item.sales || 0) / total * 100 : 0; return `${palette[index % palette.length]} ${start}% ${current}%` })
  if (!total) return <p className="seller-earning-empty">No category sales in this date range.</p>
  return <div className="seller-earning-donut-layout"><div className="seller-earning-donut" style={{ background: `conic-gradient(${stops.join(',')})` }}><div><strong>{money(total)}</strong><span>Sales</span></div></div><ul>{categories.map((item, index) => <li key={item.categoryId}><i style={{ background: palette[index % palette.length] }}/><span>{item.categoryName}</span><b>{Math.round(Number(item.sales) / total * 100)}%</b></li>)}</ul></div>
}

export default function Earnings() {
  const [range, setRange] = useState('30')
  const [customFrom, setCustomFrom] = useState('')
  const [customTo, setCustomTo] = useState('')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const dates = useMemo(() => {
    const end = new Date(); end.setHours(23, 59, 59, 999)
    const start = new Date()
    if (range === 'custom') {
      if (!customFrom || !customTo || customFrom > customTo) return null
      return { from: new Date(`${customFrom}T00:00:00`).toISOString(), to: new Date(`${customTo}T23:59:59.999`).toISOString() }
    }
    start.setDate(start.getDate() - Number(range) + 1); start.setHours(0, 0, 0, 0)
    return { from: start.toISOString(), to: end.toISOString() }
  }, [range, customFrom, customTo])

  const load = useCallback(async () => {
    if (!dates) return
    setLoading(true); setError('')
    try { setData(await getSellerEarnings(dates)) }
    catch (requestError) { setError(getRequestError(requestError, 'Could not load seller earnings.')) }
    finally { setLoading(false) }
  }, [dates])
  useEffect(() => { load() }, [load])

  const download = () => {
    if (!data) return
    const rows = [['Date', 'Description', 'Amount', 'Status'], ...(data.transactions || []).map((tx) => [tx.date, tx.description, tx.amount, tx.status]), [], ['Gross sales', data.grossSales], ['Refunds', data.refundedAmount], ['Net sales', data.sales], ['Platform commission', data.platformCommission], ['Estimated earnings', data.earnings], ['Pending balance', data.pendingBalance], ['Total payouts', data.totalPayouts]]
    const csv = rows.map((row) => row.map((value) => `"${String(value ?? '').replaceAll('"', '""')}"`).join(',')).join('\r\n')
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' })); const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'shopsphere-earnings-statement.csv'; anchor.click(); URL.revokeObjectURL(url)
  }

  const categories = data?.categories || []
  const transactions = data?.transactions || []
  const stats = [['Total Sales', data?.sales, 'blue', 'sales', 'After completed refunds'], ['Total Earnings', data?.earnings, 'green', 'wallet', data?.earningsEstimated ? 'Estimated from commission rules' : 'Commission not recorded'], ['Pending Balance', data?.pendingBalance, 'orange', 'clock', 'Payout ledger unavailable'], ['Total Payouts', data?.totalPayouts, 'violet', 'check', 'Payout ledger unavailable']]

  return <SellerLayout>
    <main className="seller-earnings-page">
      <header className="seller-earnings-heading"><div className="seller-earnings-title"><span><SellerIcon name="chart" size={31}/></span><div><h1>Earnings Dashboard</h1><p>Track sales, estimated earnings and refunds</p></div></div><label className="seller-earnings-range"><span>Date range</span><select aria-label="Earnings date range" onChange={(event) => setRange(event.target.value)} value={range}><option value="7">Last 7 Days</option><option value="30">Last 30 Days</option><option value="90">Last 90 Days</option><option value="custom">Custom range</option></select></label></header>
      {range === 'custom' && <div className="seller-earnings-custom"><label>From<input max={customTo || undefined} onChange={(e) => setCustomFrom(e.target.value)} type="date" value={customFrom}/></label><label>To<input min={customFrom || undefined} onChange={(e) => setCustomTo(e.target.value)} type="date" value={customTo}/></label></div>}
      {error && <div className="seller-earnings-alert" role="alert">{error}<button onClick={load} type="button">Retry</button></div>}
      <section className="seller-earnings-stats" aria-label="Earnings summary">{stats.map(([title, value, tone, icon, hint]) => <SummaryCard key={title} title={title} value={loading ? '…' : money(value)} tone={tone} icon={icon} hint={hint}/>)}</section>
      <section className="seller-earnings-grid">
        <article className="seller-earnings-panel seller-earnings-overview" id="sales-report"><header><div><SellerIcon name="chart" size={21}/><h2>Earnings Overview</h2></div><div className="seller-earnings-legend"><span><i className="earnings-dot"/>Earnings</span><span><i className="sales-dot"/>Sales</span></div></header><TrendChart points={data?.trend || []}/></article>
        <article className="seller-earnings-panel seller-earnings-category"><header><div><SellerIcon name="box" size={21}/><h2>Sales by Category</h2></div></header><Donut categories={categories}/></article>
        <article className="seller-earnings-panel seller-earnings-transactions"><header><h2>Recent Transactions</h2><span>{transactions.length} records</span></header><div className="seller-earnings-table-scroll"><table><thead><tr><th>Date</th><th>Description</th><th>Amount</th><th>Status</th></tr></thead><tbody>{loading ? <tr><td colSpan="4" className="seller-earnings-table-empty">Loading transactions…</td></tr> : transactions.length ? transactions.map((tx) => <tr key={tx.sellerOrderId}><td>{dateText(tx.date)}</td><td><Link to={`/seller/orders/${tx.sellerOrderId}`}>{tx.description}</Link></td><td className={Number(tx.amount) < 0 ? 'amount-negative' : 'amount-positive'}>{money(tx.amount)}</td><td><span className={`seller-earnings-status ${tx.status === 'REFUNDED' ? 'refunded' : 'cleared'}`}>{tx.status === 'REFUNDED' ? 'Refunded' : tx.status?.replaceAll('_', ' ')}</span></td></tr>) : <tr><td colSpan="4" className="seller-earnings-table-empty">No paid transactions for this date range.</td></tr>}</tbody></table></div></article>
        <article className="seller-earnings-panel seller-earnings-quick"><header><h2>Quick Actions</h2></header><div className="seller-earnings-actions"><a href="#sales-report"><EarningsIcon name="sales"/>View Sales Report</a><button onClick={download} disabled={!data}><EarningsIcon name="download"/>Download Statement</button><button disabled={!data?.payoutTrackingAvailable} title="Seller payout requests are not available in the current backend"><EarningsIcon name="card"/>Request Payout</button><button disabled title="Payout settings are not available in the current backend"><EarningsIcon name="settings"/>Manage Payout Settings</button></div><p>Payout requests and payout settings are not connected to an existing seller payout workflow.</p></article>
      </section>
      <p className="seller-earnings-footnote">Gross sales, refunds and category totals use this seller’s successful-payment order items. Net earnings are estimates calculated from effective commission rules; commission is not stored as an order-time ledger entry. No seller payout ledger is available.</p>
    </main>
  </SellerLayout>
}
