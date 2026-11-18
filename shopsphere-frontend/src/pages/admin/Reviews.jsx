import { useCallback, useEffect, useMemo, useState } from 'react'
import AdminLayout from '../../layouts/AdminLayout'
import { getRequestError } from '../../services/authService'
import { deleteAdminReview, exportAdminReviews, getAdminReviewStats, getAdminReviews, moderateAdminReview } from '../../services/adminReviewService'
import './AdminReviews.css'

const SIZE_OPTIONS = [10, 20, 50]
const EMPTY_STATS = { totalReviews: 0, positiveReviews: 0, neutralReviews: 0, negativeReviews: 0 }
const getRows = (data) => Array.isArray(data) ? data : data?.content || data?.items || data?.reviews || []
const getPageCount = (data) => data?.totalPages ?? data?.page?.totalPages ?? 0
const moneyDate = (value) => value ? new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—'
const text = (value, fallback = '—') => value == null || value === '' ? fallback : value

function Icon({ name, size = 19 }) {
  const paths = {
    search: <><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></>,
    reset: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.6 9a7 7 0 0 1 11.7-2L20 12M4 12l2.7 5a7 7 0 0 0 11.7-2"/></>,
    download: <><path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4"/></>,
    close: <path d="m6 6 12 12M18 6 6 18"/>,
    star: <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9L12 3Z"/>,
    bubble: <><path d="M20 11.5a7.5 7.5 0 0 1-7.5 7.5H7l-4 2 1.4-4.4A7.5 7.5 0 1 1 20 11.5Z"/><path d="m12 6 1.2 2.4 2.7.4-2 1.9.5 2.7-2.4-1.3-2.4 1.3.5-2.7-2-1.9 2.7-.4L12 6Z"/></>,
    good: <><circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/></>,
    neutral: <><circle cx="12" cy="12" r="9"/><path d="M8 10h.01M16 10h.01M8 16h8"/></>,
    bad: <><circle cx="12" cy="12" r="9"/><path d="M8 9l8 8M16 9l-8 8"/></>,
    eye: <><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></>,
    trash: <><path d="M4 7h16M10 11v6m4-6v6M6 7l1 14h10l1-14M9 7V4h6v3"/></>,
    flag: <><path d="M5 21V4m0 1c6-4 8 4 14 0v11c-6 4-8-4-14 0"/></>,
    box: <><path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="M3 8v9l9 5 9-5V8M12 13v9"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name] || paths.star}</svg>
}

function Stars({ value }) {
  const rating = Number(value) || 0
  return <span aria-label={`${rating} out of 5 stars`} className="arv-stars">{[1, 2, 3, 4, 5].map((star) => <span className={star <= Math.round(rating) ? 'is-filled' : ''} key={star}>★</span>)} <b>{rating ? rating.toFixed(1) : '—'}</b></span>
}

function Status({ status }) {
  const value = String(status || 'UNKNOWN').toUpperCase()
  return <span className={`arv-status arv-status--${value.toLowerCase()}`}><i/>{value.replaceAll('_', ' ')}</span>
}

function Avatar({ name, url }) {
  return <span className="arv-avatar">{url ? <img alt="" src={url}/> : (name || '?').split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase()}</span>
}

function ProductThumb({ review }) {
  const url = review.productImageUrl || review.product?.imageUrl || review.product?.primaryImageUrl || review.product?.images?.[0]?.url
  return <span className="arv-product-thumb">{url ? <img alt="" src={url}/> : <Icon name="box" size={20}/>}</span>
}

function ReviewModal({ review, onClose, onSubmit, busy, error }) {
  const [action, setAction] = useState('APPROVED')
  const [note, setNote] = useState('')
  const [reason, setReason] = useState('')
  useEffect(() => {
    const previous = document.activeElement
    const closeOnEscape = (event) => { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', closeOnEscape)
    return () => { window.removeEventListener('keydown', closeOnEscape); previous?.focus?.() }
  }, [onClose])
  return <div className="arv-overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }} role="presentation"><section aria-labelledby="arv-modal-title" aria-modal="true" className="arv-modal" role="dialog">
    <header><span className="arv-mark"><Icon name="star"/></span><div><h2 id="arv-modal-title">Moderate Review</h2><small>Review #{review.id}</small></div><button aria-label="Close moderation" className="arv-icon-btn" onClick={onClose} type="button"><Icon name="close"/></button></header>
    <div className="arv-modal-product"><ProductThumb review={review}/><div><strong>{text(review.productName || review.product?.productName)}</strong><small>{text(review.customerName || review.customer?.fullName)} · {text(review.productSku || review.product?.sku, 'SKU unavailable')}</small><Stars value={review.rating}/></div></div>
    <label className="arv-field">Review content<textarea readOnly value={review.reviewText || review.comment || review.content || ''}/></label>
    <div className="arv-current-status">Current status <Status status={review.status}/></div>
    <fieldset className="arv-actions"><legend>Moderation action</legend>{['APPROVED', 'HIDDEN', 'REJECTED'].map((value) => <label key={value}><input checked={action === value} name="review-action" onChange={() => setAction(value)} type="radio" value={value}/>{value[0] + value.slice(1).toLowerCase()}</label>)}</fieldset>
    {action === 'REJECTED' && <label className="arv-field">Rejection reason<textarea maxLength={500} onChange={(event) => setReason(event.target.value)} required value={reason}/></label>}
    <label className="arv-field">Admin note (optional)<textarea maxLength={1000} onChange={(event) => setNote(event.target.value)} placeholder="Add a note for the moderation record…" value={note}/></label>
    {error && <p className="arv-inline-error" role="alert">{error}</p>}
    <footer><button className="arv-button" onClick={onClose} type="button">Cancel</button><button className="arv-button arv-button--primary" disabled={busy || (action === 'REJECTED' && !reason.trim())} onClick={() => onSubmit({ status: action, adminNote: note.trim(), rejectionReason: reason.trim() })} type="button">{busy ? 'Updating…' : 'Update Review'}</button></footer>
  </section></div>
}

function DetailsDrawer({ review, onClose, onModerate, onDelete, busy }) {
  useEffect(() => { const onKey = (event) => { if (event.key === 'Escape') onClose() }; window.addEventListener('keydown', onKey); return () => window.removeEventListener('keydown', onKey) }, [onClose])
  const product = review.product || {}
  const customer = review.customer || {}
  return <div className="arv-drawer-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }} role="presentation"><aside aria-label="Review details" aria-modal="true" className="arv-drawer" role="dialog">
    <header><span className="arv-mark"><Icon name="star"/></span><h2>Review Details</h2><button aria-label="Close details" className="arv-icon-btn" onClick={onClose} type="button"><Icon name="close"/></button></header>
    <div className="arv-drawer-content"><div className="arv-product-summary"><ProductThumb review={review}/><div><strong>{text(review.productName || product.productName)}</strong><Stars value={review.rating}/><small>SKU: {text(review.productSku || product.sku, 'Unavailable')}</small><Status status={review.status}/></div></div>
      <section className="arv-info-section"><h3>Customer Information</h3><div className="arv-customer"><Avatar name={review.customerName || customer.fullName} url={review.customerAvatarUrl || customer.avatarUrl}/><div><strong>{text(review.customerName || customer.fullName)}</strong><a href={`mailto:${review.customerEmail || customer.email || ''}`}>{text(review.customerEmail || customer.email)}</a></div></div></section>
      <section className="arv-info-section"><h3>Review Information</h3><dl><div><dt>Rating</dt><dd><Stars value={review.rating}/></dd></div><div><dt>Review date</dt><dd>{moneyDate(review.createdAt || review.reviewDate)}</dd></div><div><dt>Status</dt><dd><Status status={review.status}/></dd></div><div><dt>Verified purchase</dt><dd>{review.verifiedPurchase == null ? 'Unavailable' : review.verifiedPurchase ? 'Yes' : 'No'}</dd></div></dl><blockquote>{text(review.reviewText || review.comment || review.content, 'No review text')}</blockquote></section>
      <section className="arv-info-section"><h3>Product Statistics</h3><dl><div><dt>Total reviews</dt><dd>{text(review.productReviewCount ?? product.reviewCount)}</dd></div><div><dt>Average rating</dt><dd>{product.averageRating ?? review.productAverageRating ?? 'Unavailable'}</dd></div></dl></section>
      <section className="arv-info-section"><h3>Actions</h3><div className="arv-drawer-actions"><button className="arv-button" disabled={busy} onClick={() => onModerate(review)} type="button">Moderate Review</button><button className="arv-button arv-button--danger" disabled={busy} onClick={() => onDelete(review)} type="button"><Icon name="trash"/> Delete Review</button></div></section></div>
  </aside></div>
}

export default function Reviews() {
  const [pageData, setPageData] = useState(null)
  const [stats, setStats] = useState(EMPTY_STATS)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')
  const [query, setQuery] = useState('')
  const [filters, setFilters] = useState({ search: '', rating: '', status: '', productId: '', reported: '', from: '', to: '' })
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [selected, setSelected] = useState([])
  const [drawer, setDrawer] = useState(null)
  const [moderating, setModerating] = useState(null)
  const [modalError, setModalError] = useState('')
  const [confirmDelete, setConfirmDelete] = useState(null)

  const load = useCallback(async (activeFilters = filters, activePage = page, activeSize = size) => {
    setLoading(true); setError('')
    try {
      const params = { ...activeFilters, page: activePage, size: activeSize }
      Object.keys(params).forEach((key) => { if (params[key] === '') delete params[key] })
      const [reviews, summary] = await Promise.all([getAdminReviews(params), getAdminReviewStats(activeFilters)])
      setPageData(reviews); setStats({ ...EMPTY_STATS, ...(summary || {}) }); setSelected([])
    } catch (requestError) { setError(getRequestError(requestError, 'Could not load reviews. The review management API may not be available yet.')) }
    finally { setLoading(false) }
  }, [filters, page, size])
  useEffect(() => { load() }, [load])
  useEffect(() => { if (!toast) return undefined; const timer = window.setTimeout(() => setToast(''), 3200); return () => window.clearTimeout(timer) }, [toast])

  const rows = useMemo(() => getRows(pageData), [pageData])
  const pages = getPageCount(pageData)
  const applyFilters = (event) => { event.preventDefault(); const next = { ...filters, search: query.trim() }; setFilters(next); setPage(0); load(next, 0, size) }
  const resetFilters = () => { const next = { search: '', rating: '', status: '', productId: '', reported: '', from: '', to: '' }; setQuery(''); setFilters(next); setPage(0); load(next, 0, size) }
  const selectRow = (id) => setSelected((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id])
  const exportRows = async () => { setExporting(true); try { const blob = await exportAdminReviews(filters); const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'shopsphere-reviews.csv'; anchor.click(); URL.revokeObjectURL(url) } catch (e) { setError(getRequestError(e, 'Review export is unavailable.')) } finally { setExporting(false) } }
  const moderate = async (payload) => { setBusy(true); setModalError(''); try { await moderateAdminReview(moderating.id, payload); setModerating(null); setToast('Review moderation updated.'); await load(); if (drawer?.id === moderating.id) setDrawer({ ...drawer, status: payload.status }) } catch (e) { setModalError(getRequestError(e, 'Could not update this review.')) } finally { setBusy(false) } }
  const removeReview = async () => { setBusy(true); try { await deleteAdminReview(confirmDelete.id); setConfirmDelete(null); if (drawer?.id === confirmDelete.id) setDrawer(null); setToast('Review deleted.'); await load() } catch (e) { setError(getRequestError(e, 'Could not delete this review.')); setConfirmDelete(null) } finally { setBusy(false) } }
  const statCards = [{ title: 'Total Reviews', key: 'totalReviews', icon: 'bubble', tone: 'purple' }, { title: 'Positive Reviews', key: 'positiveReviews', icon: 'good', tone: 'green' }, { title: 'Neutral Reviews', key: 'neutralReviews', icon: 'neutral', tone: 'orange' }, { title: 'Negative Reviews', key: 'negativeReviews', icon: 'bad', tone: 'red' }]

  return <AdminLayout pageTitle="Reviews Management" pageSubtitle="Moderate product reviews and manage customer feedback across your marketplace." breadcrumb="Dashboard / Reviews" showDateFilter={false} onExport={exportRows} exporting={exporting} exportLabel="Export Reviews" titleIcon="reviews">
    <main className="admin-reviews-page">
      {toast && <div className="arv-toast" role="status">{toast}</div>}
      {error && <div className="arv-error" role="alert"><span>{error}</span><button className="arv-button" onClick={() => load()} type="button">Retry</button></div>}
      <section aria-label="Review statistics" className="arv-stats">{statCards.map((card) => <article className={`arv-stat arv-stat--${card.tone}`} key={card.title}><span className="arv-stat-icon"><Icon name={card.icon} size={25}/></span><div><span>{card.title}</span><strong>{stats[card.key] == null ? '—' : Number(stats[card.key] || 0).toLocaleString('en-IN')}</strong><small>Based on current review records</small></div></article>)}</section>
      <form className="arv-filter" onSubmit={applyFilters}><label className="arv-search"><Icon name="search"/><input onChange={(event) => setQuery(event.target.value)} placeholder="Search product, customer or review text…" value={query}/></label><select aria-label="Rating filter" onChange={(event) => setFilters({ ...filters, rating: event.target.value })} value={filters.rating}><option value="">Rating: All</option>{[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{value} stars</option>)}</select><select aria-label="Status filter" onChange={(event) => setFilters({ ...filters, status: event.target.value })} value={filters.status}><option value="">Status: All</option>{['PENDING', 'APPROVED', 'HIDDEN', 'REJECTED', 'REPORTED'].map((value) => <option key={value} value={value}>{value}</option>)}</select><input aria-label="Product ID filter" className="arv-product-filter" onChange={(event) => setFilters({ ...filters, productId: event.target.value })} placeholder="Product ID" value={filters.productId}/><label className="arv-reported"><input checked={filters.reported === 'true'} onChange={(event) => setFilters({ ...filters, reported: event.target.checked ? 'true' : '' })} type="checkbox"/> Reported</label><button className="arv-button" onClick={resetFilters} type="button"><Icon name="reset"/> Reset</button><button className="arv-button arv-button--primary" type="submit"><Icon name="search"/> Search</button><div className="arv-date-filters"><label>From <input onChange={(event) => setFilters({ ...filters, from: event.target.value })} type="date" value={filters.from}/></label><label>To <input onChange={(event) => setFilters({ ...filters, to: event.target.value })} type="date" value={filters.to}/></label></div></form>
      <section aria-label="Reviews" className="arv-table-card"><div className="arv-table-scroll"><table className="arv-table"><thead><tr><th className="arv-check"><input aria-label="Select all reviews" checked={rows.length > 0 && selected.length === rows.length} onChange={(event) => setSelected(event.target.checked ? rows.map((item) => item.id) : [])} type="checkbox"/></th><th>Product</th><th>Customer</th><th>Rating</th><th>Review</th><th>Status</th><th>Review Date</th><th>Actions</th></tr></thead><tbody>
        {loading && <tr><td className="arv-state" colSpan="8"><span className="arv-spinner"/>Loading reviews…</td></tr>}
        {!loading && rows.length === 0 && <tr><td className="arv-state" colSpan="8"><Icon name="bubble" size={27}/><strong>No reviews found</strong><span>Reviews will appear here when they are available from the backend.</span></td></tr>}
        {!loading && rows.map((review) => <tr key={review.id}><td className="arv-check"><input aria-label={`Select review ${review.id}`} checked={selected.includes(review.id)} onChange={() => selectRow(review.id)} type="checkbox"/></td><td><div className="arv-product-cell"><ProductThumb review={review}/><span><strong>{text(review.productName || review.product?.productName)}</strong><small>SKU: {text(review.productSku || review.product?.sku, 'Unavailable')}</small></span></div></td><td><div className="arv-customer-cell"><Avatar name={review.customerName || review.customer?.fullName} url={review.customerAvatarUrl || review.customer?.avatarUrl}/><span><strong>{text(review.customerName || review.customer?.fullName)}</strong><small>{text(review.customerEmail || review.customer?.email)}</small></span></div></td><td><Stars value={review.rating}/></td><td><div className="arv-review-cell"><span>{text(review.reviewText || review.comment || review.content, 'No review text')}</span><button onClick={() => setDrawer(review)} type="button">View More</button></div></td><td><Status status={review.status}/></td><td>{moneyDate(review.createdAt || review.reviewDate)}</td><td><div className="arv-row-actions"><button aria-label="View review details" onClick={() => setDrawer(review)} type="button"><Icon name="eye"/></button><button aria-label="Moderate review" onClick={() => { setModerating(review); setModalError('') }} type="button"><Icon name="star"/></button><button aria-label="Delete review" onClick={() => setConfirmDelete(review)} type="button"><Icon name="trash"/></button></div></td></tr>)}
      </tbody></table></div></section>
      <footer className="arv-pagination"><span>{pageData?.totalElements != null ? `Showing ${rows.length ? page * size + 1 : 0}–${page * size + rows.length} of ${Number(pageData.totalElements).toLocaleString('en-IN')} reviews` : `${rows.length} reviews on this page`}</span><div><label>Rows <select onChange={(event) => { const next = Number(event.target.value); setSize(next); setPage(0); load(filters, 0, next) }} value={size}>{SIZE_OPTIONS.map((value) => <option key={value}>{value}</option>)}</select></label><button aria-label="Previous page" disabled={page <= 0 || loading} onClick={() => { const next = page - 1; setPage(next); load(filters, next, size) }} type="button">‹</button><span>Page {page + 1}{pages ? ` of ${pages}` : ''}</span><button aria-label="Next page" disabled={loading || (pages > 0 && page + 1 >= pages) || rows.length < size} onClick={() => { const next = page + 1; setPage(next); load(filters, next, size) }} type="button">›</button></div></footer>
    </main>
    {drawer && <DetailsDrawer busy={busy} onClose={() => setDrawer(null)} onDelete={setConfirmDelete} onModerate={(item) => { setModerating(item); setModalError('') }} review={drawer}/>}
    {moderating && <ReviewModal busy={busy} error={modalError} onClose={() => setModerating(null)} onSubmit={moderate} review={moderating}/>}
    {confirmDelete && <div className="arv-overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) setConfirmDelete(null) }} role="presentation"><section aria-labelledby="arv-delete-title" aria-modal="true" className="arv-confirm" role="alertdialog"><header><span className="arv-mark arv-mark--danger"><Icon name="trash"/></span><div><h2 id="arv-delete-title">Delete review?</h2><small>This action cannot be undone.</small></div><button aria-label="Close confirmation" className="arv-icon-btn" onClick={() => setConfirmDelete(null)} type="button"><Icon name="close"/></button></header><p>Delete the review for <strong>{text(confirmDelete.productName || confirmDelete.product?.productName)}</strong> by <strong>{text(confirmDelete.customerName || confirmDelete.customer?.fullName)}</strong>?</p><blockquote>{text(confirmDelete.reviewText || confirmDelete.comment || confirmDelete.content, 'No review text')}</blockquote><footer><button className="arv-button" onClick={() => setConfirmDelete(null)} type="button">Cancel</button><button className="arv-button arv-button--danger" disabled={busy} onClick={removeReview} type="button">{busy ? 'Deleting…' : 'Delete Review'}</button></footer></section></div>}
  </AdminLayout>
}
