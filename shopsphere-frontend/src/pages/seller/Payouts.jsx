import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import SellerLayout, { SellerIcon } from '../../layouts/SellerLayout'
import { getRequestError } from '../../services/authService'
import { getSellerPayouts, requestSellerPayout } from '../../services/sellerService'
import './SellerPayouts.css'

const PAGE_SIZE = 5
const money = (value) => value == null ? 'Not tracked' : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value) || 0)
const dateText = (value) => value ? new Date(value).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'

function PayoutIcon({ name, size = 22 }) {
  const paths = {
    wallet: <><rect x="3" y="5" width="18" height="15" rx="2"/><path d="M3 9h18m-5 5h2"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    check: <><circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></>,
    info: <><circle cx="12" cy="12" r="9"/><path d="M12 11v5m0-8h.01"/></>,
    history: <><path d="M3 12a9 9 0 1 0 2.6-6.4L3 8"/><path d="M3 3v5h5m4-1v5l3 2"/></>,
    download: <><path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4"/></>,
    send: <><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></>,
    bank: <><path d="m3 10 9-7 9 7M5 10v9m5-9v9m4-9v9m5-9v9M3 21h18M2 10h20"/></>,
    arrow: <><path d="M5 12h14m-6-6 6 6-6 6"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

function BalanceCard({ title, value, icon, tone, caption }) {
  return <article className={`seller-payout-stat ${tone}`}><span><PayoutIcon name={icon} size={25}/></span><div><small>{title}</small><strong>{value}</strong>{caption && <em>{caption}</em>}</div></article>
}

export default function Payouts() {
  const [range, setRange] = useState('90')
  const [customFrom, setCustomFrom] = useState('')
  const [customTo, setCustomTo] = useState('')
  const [history, setHistory] = useState(null)
  const [month, setMonth] = useState(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [amount, setAmount] = useState('')
  const [method, setMethod] = useState('BANK_ACCOUNT')
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [notice, setNotice] = useState('')
  const [accountHolder, setAccountHolder] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [ifsc, setIfsc] = useState('')
  const [upiId, setUpiId] = useState('')

  const rangeParams = useMemo(() => {
    const now = new Date()
    if (range === 'custom') {
      if (!customFrom || !customTo || customTo < customFrom) return null
      return { from: new Date(`${customFrom}T00:00:00`).toISOString(), to: new Date(`${customTo}T23:59:59.999`).toISOString() }
    }
    const from = new Date(now); from.setDate(from.getDate() - Number(range) + 1); from.setHours(0, 0, 0, 0)
    return { from: from.toISOString(), to: now.toISOString() }
  }, [range, customFrom, customTo])

  const load = useCallback(async () => {
    if (!rangeParams) return
    setLoading(true); setError('')
    const current = new Date()
    const monthFrom = new Date(current.getFullYear(), current.getMonth(), 1).toISOString()
    try {
      const ledger = await getSellerPayouts({ ...rangeParams, page, size: PAGE_SIZE })
      setHistory(ledger); setMonth(ledger); if (ledger.account) setAccountHolder((currentName) => currentName || ledger.account.accountHolder || '')
    } catch (requestError) { setError(getRequestError(requestError, 'Could not load payout information.')) }
    finally { setLoading(false) }
  }, [rangeParams, page])
  useEffect(() => { load() }, [load])

  const transactions = history?.transactions || []
  const pageCount = history?.totalPages || 0
  const visibleTransactions = transactions

  async function validateRequest(event) {
    event.preventDefault()
    setFormError(''); setNotice('')
    const value = Number(amount)
    if (!Number.isFinite(value) || value <= 0) { setFormError('Enter a payout amount greater than zero.'); return }
    if (value < Number(history?.minimumWithdrawal || 1000)) { setFormError(`Minimum payout amount is ${money(history?.minimumWithdrawal || 1000)}.`); return }
    if (history?.availableBalance == null || value > Number(history.availableBalance)) { setFormError('Payout amount is greater than your available balance.'); return }
    if (!history?.account && (!accountHolder.trim() || (method === 'BANK_ACCOUNT' ? !accountNumber || !ifsc : !upiId))) { setFormError('Enter the registered payout account details to continue.'); return }
    setSubmitting(true)
    try {
      const payload = { amount: value, method, accountHolder: accountHolder.trim() || undefined,
        accountNumber: accountNumber.trim() || undefined, ifsc: ifsc.trim().toUpperCase() || undefined, upiId: upiId.trim() || undefined }
      await requestSellerPayout(payload, globalThis.crypto?.randomUUID?.() || `payout-${Date.now()}-${Math.random().toString(36).slice(2)}`)
      setAmount(''); setNotice('Payout request submitted. It will appear in transaction history and await finance processing.')
      await load()
    } catch (requestError) { setFormError(getRequestError(requestError, 'Payout request could not be submitted.')) }
    finally { setSubmitting(false) }
  }

  function downloadStatement() {
    const rows = [['Date', 'Type', 'Amount', 'Status', 'Order Reference', 'Remarks'], ...(transactions.map((tx) => [tx.date, tx.status === 'REFUNDED' ? 'Refund' : 'Order earnings', tx.amount, tx.status, `Order ${tx.orderId}`, tx.description]))]
    const content = rows.map((row) => row.map((value) => `"${String(value ?? '').replaceAll('"', '""')}"`).join(',')).join('\r\n')
    const url = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8' })); const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'shopsphere-transaction-statement.csv'; anchor.click(); URL.revokeObjectURL(url)
  }

  const cards = [
    ['Available Balance', history?.availableBalance, 'wallet', 'blue', 'Delivered, paid earnings less payouts'],
    ['Pending Payout', history?.pendingPayout, 'clock', 'green', 'Requests awaiting finance processing'],
    ['Total Payouts', history?.totalPayouts, 'check', 'violet', 'No payout ledger available'],
    ['This Month Earnings', month?.earnings, 'calendar', 'orange', month?.earningsEstimated ? 'Estimated after commission' : 'Commission data unavailable'],
  ]

  return <SellerLayout>
    <main className="seller-payouts-page">
      <header className="seller-payouts-heading"><div><span><SellerIcon name="wallet" size={31}/></span><div><h1>Payouts Management</h1><p>Review transaction activity and payout availability</p></div></div><label><PayoutIcon name="calendar" size={18}/><select aria-label="Transaction history date range" onChange={(event) => { setRange(event.target.value); setPage(0) }} value={range}><option value="30">Last 30 Days</option><option value="90">Last 3 Months</option><option value="custom">Custom Range</option></select></label></header>
      {range === 'custom' && <div className="seller-payouts-custom"><label>From<input max={customTo || undefined} onChange={(e) => setCustomFrom(e.target.value)} type="date" value={customFrom}/></label><label>To<input min={customFrom || undefined} onChange={(e) => setCustomTo(e.target.value)} type="date" value={customTo}/></label></div>}
      {error && <div className="seller-payouts-alert" role="alert">{error}<button onClick={load} type="button">Retry</button></div>}
      <section className="seller-payouts-stats" aria-label="Payout summary">{cards.map(([title, value, icon, tone, caption]) => <BalanceCard key={title} title={title} value={loading ? '…' : money(value)} icon={icon} tone={tone} caption={caption}/>)}</section>

      {notice && <div className="seller-payout-notice" role="status">{notice}</div>}
      <section className="seller-payouts-upper">
        <article className="seller-payout-panel seller-payout-request"><header><span><SellerIcon name="wallet" size={21}/></span><h2>Request a Payout</h2></header><div className="seller-payout-form-layout"><div className="seller-payout-available"><small>Available Balance</small><strong>{loading ? '…' : money(history?.availableBalance)}</strong><span className="seller-payout-unavailable-tag">{history?.availableBalance > 0 ? 'Available for request' : 'No cleared balance'}</span></div><form onSubmit={validateRequest} noValidate><label>Payout Amount<input inputMode="decimal" min="0.01" onChange={(e) => { setAmount(e.target.value); setFormError('') }} placeholder={`Minimum ${money(history?.minimumWithdrawal)}`} step="0.01" type="number" value={amount}/><small>Minimum withdrawal: {money(history?.minimumWithdrawal)}</small></label><label>Payment Method<select onChange={(e) => setMethod(e.target.value)} value={method}><option value="BANK_ACCOUNT">Bank Account</option><option value="UPI">UPI</option></select></label>{history?.account?.method === method && <div className="seller-payout-registered">Registered {method === 'UPI' ? 'UPI' : 'bank account'}: <strong>{history.account.maskedDestination}</strong><small>Leave account fields empty to use this registered destination.</small></div>}<label>Account Holder<input autoComplete="name" onChange={(e) => setAccountHolder(e.target.value)} placeholder="Account holder name" value={accountHolder}/></label>{method === 'BANK_ACCOUNT' ? <><label>Account Number<input autoComplete="off" inputMode="numeric" onChange={(e) => setAccountNumber(e.target.value)} placeholder="8–34 digit account number" value={accountNumber}/></label><label>IFSC Code<input autoComplete="off" onChange={(e) => setIfsc(e.target.value.toUpperCase())} placeholder="e.g. ABCD0123456" value={ifsc}/></label></> : <label>UPI ID<input autoComplete="off" onChange={(e) => setUpiId(e.target.value)} placeholder="name@bank" value={upiId}/></label>}{formError && <p className="seller-payout-form-error" role="alert">{formError}</p>}<button disabled={submitting || loading} type="submit"><PayoutIcon name="send" size={18}/>{submitting ? 'Submitting…' : 'Request Payout'}</button></form><div className="seller-payout-illustration"><span><PayoutIcon name="wallet" size={62}/></span><strong>Request payouts securely<br/>with encrypted account details</strong></div></div></article>
        <article className="seller-payout-panel seller-payout-info"><header><span><PayoutIcon name="info" size={21}/></span><h2>Payout Information</h2></header><ul><li><i>✓</i><span>Minimum withdrawal amount: {money(history?.minimumWithdrawal)}</span></li><li><i>✓</i><span>Payout processing: Finance review, then bank transfer</span></li><li><i>✓</i><span>Request frequency: Once every {history?.cooldownDays ?? 7} days</span></li><li><i>✓</i><span>Supported methods: Bank Account and UPI</span></li></ul><p><PayoutIcon name="info" size={18}/>Requests are reserved from your balance. Finance marks a request paid only after recording the external bank transfer reference.</p></article>
      </section>

      <section className="seller-payout-panel seller-payout-history"><header><div><span><PayoutIcon name="history" size={22}/></span><h2>Transaction History</h2></div><button disabled={!transactions.length} onClick={downloadStatement} type="button"><PayoutIcon name="download" size={17}/>Download Statement</button></header><div className="seller-payout-table-scroll"><table><thead><tr><th>Date</th><th>Type</th><th>Amount</th><th>Status</th><th>Order Reference</th><th>Remarks</th><th>Details</th></tr></thead><tbody>{loading ? <tr><td className="seller-payout-empty" colSpan="7">Loading transaction activity…</td></tr> : visibleTransactions.length ? visibleTransactions.map((tx) => { const refund = tx.status === 'REFUNDED'; return <tr key={tx.sellerOrderId}><td>{dateText(tx.date)}</td><td>{refund ? 'Refund' : 'Order earnings'}</td><td className={Number(tx.amount) < 0 ? 'negative' : 'positive'}>{money(tx.amount)}</td><td><span className={`seller-payout-status ${refund ? 'refunded' : 'order'}`}>{refund ? 'Refunded' : tx.status?.replaceAll('_', ' ')}</span></td><td>Order #{tx.orderId}</td><td>{refund ? 'Order refund recorded' : 'Paid order activity'}</td><td><Link to={`/seller/orders/${tx.sellerOrderId}`}>View Details <PayoutIcon name="arrow" size={14}/></Link></td></tr> }) : <tr><td className="seller-payout-empty" colSpan="7">No order or refund activity for this date range.</td></tr>}</tbody></table></div><footer className="seller-payout-pagination"><span>Showing {transactions.length ? page * PAGE_SIZE + 1 : 0} to {Math.min((page + 1) * PAGE_SIZE, transactions.length)} of {transactions.length} recorded order transactions</span><div><button aria-label="Previous page" disabled={page <= 0} onClick={() => setPage((current) => current - 1)} type="button">‹</button><span>{pageCount ? `${page + 1} / ${pageCount}` : '0 / 0'}</span><button aria-label="Next page" disabled={page >= pageCount - 1} onClick={() => setPage((current) => current + 1)} type="button">›</button></div></footer></section>
      <p className="seller-payout-footnote">Transaction history shows successful orders and completed refunds, not bank transfers. Available balance, pending payout and total payouts require a seller payout ledger that is not present in the current backend.</p>
    </main>
  </SellerLayout>
}
