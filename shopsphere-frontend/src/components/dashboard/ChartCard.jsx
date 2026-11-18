const formatCompact = (value) => {
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`
  if (value >= 1_000) return `${(value / 1_000).toFixed(0)}K`
  return String(Math.round(value || 0))
}

export default function ChartCard({ title, series = [], period, onPeriodChange, loading }) {
  const width = 760
  const height = 230
  const left = 48
  const right = 14
  const top = 14
  const bottom = 30
  const values = series.flatMap((point) => [Number(point.revenue) || 0, Number(point.orders) || 0])
  const max = Math.max(...values, 1) * 1.15
  const x = (index) => left + (index / Math.max(series.length - 1, 1)) * (width - left - right)
  const y = (value) => top + (1 - value / max) * (height - top - bottom)
  const line = (key) => series.map((point, index) => `${x(index)},${y(Number(point[key]) || 0)}`).join(' ')
  const grid = [0, 1, 2, 3, 4].map((step) => max * step / 4)

  return <section className="admin-panel admin-revenue-panel">
    <div className="admin-panel-heading"><div><span className="admin-heading-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 19V10M10 19V5M16 19v-7M22 19V3"/></svg></span><h2>{title}</h2></div><div className="admin-chart-tools"><div className="admin-chart-legend"><span><i className="legend-revenue"/>Revenue</span><span><i className="legend-orders"/>Orders</span></div><div className="admin-period-switch" role="group" aria-label="Chart period">{[['SIX_MONTHS','6 Months'],['ONE_YEAR','1 Year'],['ALL_TIME','All Time']].map(([key,label])=><button key={key} type="button" className={period===key?'selected':''} onClick={()=>onPeriodChange(key)}>{label}</button>)}</div></div></div>
    {loading ? <div className="admin-chart-loading" aria-label="Loading revenue analytics"><span/><span/><span/><span/><span/><span/></div> : series.length ? <div className="admin-chart-scroll"><svg className="admin-line-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Revenue and orders over time">
      <defs><linearGradient id="revenueFill" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor="#5147f5" stopOpacity=".17"/><stop offset="100%" stopColor="#5147f5" stopOpacity="0"/></linearGradient></defs>
      {grid.map((value,index)=><g key={index}><line x1={left} x2={width-right} y1={y(value)} y2={y(value)} className="admin-grid-line"/><text x={left-9} y={y(value)+4} textAnchor="end" className="admin-axis-label">{formatCompact(value)}</text></g>)}
      <polygon points={`${left},${height-bottom} ${line('revenue')} ${width-right},${height-bottom}`} fill="url(#revenueFill)"/>
      <polyline points={line('revenue')} className="admin-line-revenue"/>
      <polyline points={line('orders')} className="admin-line-orders"/>
      {series.map((point,index)=><g key={point.label}><circle cx={x(index)} cy={y(Number(point.revenue)||0)} r="4" className="admin-dot-revenue"/><circle cx={x(index)} cy={y(Number(point.orders)||0)} r="4" className="admin-dot-orders"/><text x={x(index)} y={height-7} textAnchor="middle" className="admin-axis-label">{point.label}</text></g>)}
    </svg></div> : <div className="admin-inline-empty">Revenue analytics are unavailable for this period.</div>}
  </section>
}
