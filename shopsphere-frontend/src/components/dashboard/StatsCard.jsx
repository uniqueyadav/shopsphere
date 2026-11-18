import { useId } from 'react'

const icons = {
  revenue: <><path d="M12 2v20"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></>,
  orders: <><path d="M3 3h2l2.4 12.4a2 2 0 0 0 2 1.6h8.9a2 2 0 0 0 2-1.6L22 8H6"/><circle cx="10" cy="21" r="1"/><circle cx="18" cy="21" r="1"/></>,
  customers: <><path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="10" cy="7" r="4"/><path d="M20 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8"/></>,
  sellers: <><path d="m3 9 2-6h14l2 6"/><path d="M4 9v11h16V9"/><path d="M9 20v-6h6v6M3 9h18"/></>,
}

function Sparkline({ values, color, id }) {
  if (!values?.length) return <div className="admin-sparkline-empty" aria-hidden="true" />
  const max = Math.max(...values, 1)
  const points = values.map((value, index) => `${(index / Math.max(values.length - 1, 1)) * 100},${30 - (value / max) * 26}`).join(' ')
  return <svg className="admin-sparkline" viewBox="0 0 100 34" preserveAspectRatio="none" role="img" aria-label="Trend over selected period"><defs><linearGradient id={id} x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor={color} stopOpacity=".24"/><stop offset="100%" stopColor={color} stopOpacity="0"/></linearGradient></defs><polygon points={`0,34 ${points} 100,34`} fill={`url(#${id})`}/><polyline points={points} fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" vectorEffect="non-scaling-stroke"/></svg>
}

export default function StatsCard({ title, value, growth, previousLabel = 'vs previous period', icon, color, tone, trend }) {
  const uid = useId().replace(/:/g, '')
  const positive = Number(growth) >= 0
  return <article className="admin-stat-card">
    <div className={`admin-stat-icon ${tone}`}><svg viewBox="0 0 24 24" aria-hidden="true">{icons[icon]}</svg></div>
    <div className="admin-stat-copy"><span className="admin-stat-title">{title}</span><strong className="admin-stat-value">{value ?? '—'}</strong><div className="admin-stat-meta"><span className={growth == null ? 'admin-neutral' : positive ? 'admin-positive' : 'admin-negative'}>{growth == null ? '—' : `${positive ? '↑' : '↓'} ${Math.abs(Number(growth)).toFixed(1)}%`}</span><span>{previousLabel}</span></div></div>
    <Sparkline values={trend} color={color} id={`spark-${uid}`}/>
  </article>
}
