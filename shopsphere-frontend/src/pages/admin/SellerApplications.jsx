import { useEffect, useState } from 'react'
import AdminLayout from '../../layouts/AdminLayout'
import { getRequestError } from '../../services/authService'
import {
  approveSellerApplication,
  getSellerApplication,
  getSellerApplications,
  rejectSellerApplication,
  exportAdminSellers,
} from '../../services/adminService'
import './SellerApplications.css'

const PAGE_SIZES = [5, 10, 25, 50]
const BUSINESS_TYPES = ['INDIVIDUAL', 'SOLE_PROPRIETORSHIP', 'PARTNERSHIP', 'LIMITED_LIABILITY_PARTNERSHIP', 'PRIVATE_LIMITED', 'PUBLIC_LIMITED', 'OTHER']

const statusLabels = {
  PENDING: 'Pending',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '—'
    : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

function Icon({ name }) {
  const paths = {
    sellers: <><circle cx="9" cy="8" r="3" /><path d="M3.5 19a5.5 5.5 0 0 1 11 0M16 5.5a3 3 0 0 1 0 5.8M17 14a5 5 0 0 1 3.5 4.8" /></>,
    pending: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
    approved: <><circle cx="12" cy="12" r="9" /><path d="m8 12 2.5 2.5L16 9" /></>,
    rejected: <><circle cx="12" cy="12" r="9" /><path d="m9 9 6 6m0-6-6 6" /></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8" /><path d="m16 16 4.5 4.5" /></>,
    store: <><path d="M4 10v10h16V10" /><path d="M3 9 5 4h14l2 5" /><path d="M3 9a2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 5 0M9 20v-6h6v6" /></>,
    mail: <><rect x="3.5" y="5" width="17" height="14" rx="2" /><path d="m4.5 7 7.5 6 7.5-6" /></>,
    phone: <path d="M7.1 3.8 9.5 7l-1.6 1.7a14.2 14.2 0 0 0 7.4 7.4L17 14.5l3.2 2.4-.8 3a1.8 1.8 0 0 1-2 1.3C9.9 19.9 4.1 14.1 2.8 6.6a1.8 1.8 0 0 1 1.3-2z" />,
    pin: <><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z" /><circle cx="12" cy="10" r="2" /></>,
    document: <><path d="M6 3.5h8l4 4V20H6z" /><path d="M14 3.5V8h4M9 12h6m-6 4h6" /></>,
  }

  return (
    <svg aria-hidden="true" className="seller-admin-icon" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" viewBox="0 0 24 24">
      {paths[name]}
    </svg>
  )
}

function LoadingSkeleton() {
  return (
    <div className="seller-admin-skeletons" aria-label="Loading seller applications" role="status">
      <div className="seller-admin-table-skeleton">
        {Array.from({ length: 5 }, (_, index) => <div className="seller-admin-row-skeleton" key={index} />)}
      </div>
      <span className="seller-admin-sr-only">Loading seller applications…</span>
    </div>
  )
}

function StatusBadge({ status }) {
  return (
    <span className={`seller-admin-status seller-admin-status--${status?.toLowerCase() || 'unknown'}`}>
      <span />
      {statusLabels[status] || status}
    </span>
  )
}

function Toast({ toast, onDismiss }) {
  if (!toast) return null
  return (
    <div className={`seller-admin-toast seller-admin-toast--${toast.type}`} role={toast.type === 'error' ? 'alert' : 'status'}>
      <span className="seller-admin-toast-icon" aria-hidden="true">{toast.type === 'success' ? '✓' : '!'}</span>
      <span>{toast.message}</span>
      <button aria-label="Dismiss notification" onClick={onDismiss} type="button">×</button>
    </div>
  )
}

function StatCard({ label, value, icon, tone, index, loading }) {
  return (
    <article className={`seller-admin-stat-card seller-admin-stat-card--${tone}`} style={{ '--card-index': index }}>
      <div className="seller-admin-stat-topline">
        <span>{label}</span>
        <span className="seller-admin-stat-icon"><Icon name={icon} /></span>
      </div>
      {loading
        ? <span className="seller-admin-number-skeleton" />
        : <strong className="seller-admin-stat-value">{value.toLocaleString()}</strong>}
      <span className="seller-admin-stat-caption">{label === 'Total Applications' ? 'All seller applications' : 'Applications'}</span>
    </article>
  )
}

function ApplicationDrawer({
  application,
  loading,
  error,
  onClose,
  onApprove,
  onReject,
  actionLoading,
}) {
  const [confirmApproval, setConfirmApproval] = useState(false)
  const [showRejectionForm, setShowRejectionForm] = useState(false)
  const [rejectionReason, setRejectionReason] = useState('')
  const [rejectionError, setRejectionError] = useState('')

  async function submitRejection(event) {
    event.preventDefault()
    if (!rejectionReason.trim()) {
      setRejectionError('A rejection reason is required.')
      return
    }
    if (rejectionReason.trim().length > 1000) {
      setRejectionError('Use 1,000 characters or fewer.')
      return
    }
    setRejectionError('')
    if (await onReject(rejectionReason.trim())) setShowRejectionForm(false)
  }

  return (
    <>
      <button aria-label="Close seller application details" className="seller-admin-overlay" onClick={onClose} type="button" />
      <aside aria-labelledby="seller-detail-title" aria-modal="true" className="seller-admin-drawer" role="dialog">
        <header className="seller-admin-drawer-header">
          <div>
            <span className="seller-admin-kicker">APPLICATION DETAILS</span>
            <h2 id="seller-detail-title">{application?.businessName || 'Seller application'}</h2>
          </div>
          <button aria-label="Close details" className="seller-admin-icon-button" onClick={onClose} type="button">×</button>
        </header>

        {loading && <div className="seller-admin-drawer-loading" role="status"><span className="seller-admin-spinner" /> Loading details…</div>}
        {!loading && error && <p className="seller-admin-error" role="alert">{error}</p>}

        {!loading && application && (
          <>
            <div className="seller-admin-drawer-status">
              <StatusBadge status={application.status} />
              <span>Submitted {formatDate(application.submittedAt)}</span>
            </div>

            <div className="seller-admin-detail-section">
              <h3>Seller contact</h3>
              <DetailRow icon="sellers" label="Full name" value={application.sellerName} />
              <DetailRow icon="mail" label="Email" value={application.email} />
              <DetailRow icon="phone" label="Phone" value={application.phone} />
            </div>

            <div className="seller-admin-detail-section">
              <h3>Business information</h3>
              <DetailRow icon="store" label="Business name" value={application.businessName} />
              <DetailRow icon="document" label="Business type" value={application.businessType?.replaceAll('_', ' ') || '—'} />
              <DetailRow icon="pin" label="Address" value={application.businessAddress} />
              <DetailRow icon="document" label="GST number" value={application.gstNumber || 'Not provided'} />
              <div className="seller-admin-description">
                <span>Business description</span>
                <p>{application.businessDescription || 'No description provided.'}</p>
              </div>
            </div>

            <div className="seller-admin-detail-section">
              <h3>Submitted documents</h3>
              <div className="seller-admin-document-empty">
                <Icon name="document" />
                <strong>Document information unavailable</strong>
                <p>This application record does not include uploaded document metadata or secure file links.</p>
              </div>
            </div>

            <div className="seller-admin-detail-section">
              <h3>Application information</h3>
              <DetailRow icon="document" label="Application ID" value={application.id} />
              <DetailRow icon="pending" label="Applied on" value={formatDate(application.submittedAt)} />
              <div className="seller-admin-description">
                <span>Application note</span>
                <p>No application note is available in the application record.</p>
              </div>
            </div>

            {application.status !== 'PENDING' && (
              <div className="seller-admin-detail-section seller-admin-review-section">
                <h3>Review record</h3>
                <DetailRow icon="sellers" label="Reviewed by" value={application.reviewedBy?.fullName || '—'} />
                <DetailRow icon="pending" label="Reviewed at" value={formatDate(application.reviewedAt)} />
                {application.rejectionReason && (
                  <div className="seller-admin-rejection-reason">
                    <span>Rejection reason</span>
                    <p>{application.rejectionReason}</p>
                  </div>
                )}
              </div>
            )}

            {application.status === 'PENDING' && (
              <div className="seller-admin-drawer-actions">
                <button className="seller-admin-reject-button" disabled={actionLoading} onClick={() => setShowRejectionForm(true)} type="button">
                  Reject application
                </button>
                <button className="seller-admin-approve-button" disabled={actionLoading} onClick={() => setConfirmApproval(true)} type="button">
                  Approve seller
                </button>
              </div>
            )}
          </>
        )}

        {confirmApproval && (
          <div className="seller-admin-modal-backdrop">
            <section aria-labelledby="seller-approve-title" aria-modal="true" className="seller-admin-confirm-modal" role="dialog">
              <span className="seller-admin-confirm-icon seller-admin-confirm-icon--approve"><Icon name="approved" /></span>
              <h3 id="seller-approve-title">Approve this seller?</h3>
              <p>{application?.businessName} will be approved and its account will receive seller access.</p>
              <div className="seller-admin-modal-actions">
                <button className="seller-admin-modal-cancel" disabled={actionLoading} onClick={() => setConfirmApproval(false)} type="button">Cancel</button>
                <button className="seller-admin-approve-button" disabled={actionLoading} onClick={async () => { if (await onApprove()) setConfirmApproval(false) }} type="button">
                  {actionLoading ? <><span className="seller-admin-spinner" /> Approving…</> : 'Confirm approval'}
                </button>
              </div>
            </section>
          </div>
        )}

        {showRejectionForm && (
          <div className="seller-admin-modal-backdrop">
            <form aria-labelledby="seller-reject-title" aria-modal="true" className="seller-admin-confirm-modal seller-admin-reject-modal" onSubmit={submitRejection} role="dialog">
              <span className="seller-admin-confirm-icon seller-admin-confirm-icon--reject"><Icon name="rejected" /></span>
              <h3 id="seller-reject-title">Reject this application?</h3>
              <p>Provide a clear reason. The applicant’s account will remain a customer account.</p>
              <label className="seller-admin-reason-label" htmlFor="seller-rejection-reason">Reason for rejection</label>
              <textarea
                aria-describedby={rejectionError ? 'seller-rejection-error' : undefined}
                aria-invalid={Boolean(rejectionError)}
                id="seller-rejection-reason"
                maxLength={1000}
                onChange={(event) => { setRejectionReason(event.target.value); setRejectionError('') }}
                placeholder="Explain what needs to be addressed…"
                value={rejectionReason}
              />
              {rejectionError && <span className="seller-admin-field-error" id="seller-rejection-error">{rejectionError}</span>}
              <span className="seller-admin-character-count">{rejectionReason.length}/1,000 characters</span>
              <div className="seller-admin-modal-actions">
                <button className="seller-admin-modal-cancel" disabled={actionLoading} onClick={() => setShowRejectionForm(false)} type="button">Cancel</button>
                <button className="seller-admin-reject-button" disabled={actionLoading} type="submit">
                  {actionLoading ? <><span className="seller-admin-spinner" /> Rejecting…</> : 'Confirm rejection'}
                </button>
              </div>
            </form>
          </div>
        )}
      </aside>
    </>
  )
}

function DetailRow({ icon, label, value }) {
  return (
    <div className="seller-admin-detail-row">
      <span className="seller-admin-detail-icon"><Icon name={icon} /></span>
      <span className="seller-admin-detail-text"><small>{label}</small><strong>{value || '—'}</strong></span>
    </div>
  )
}

export default function SellerApplications() {
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [status, setStatus] = useState('')
  const [businessType, setBusinessType] = useState('')
  const [pageSize, setPageSize] = useState(10)
  const [selectedRows, setSelectedRows] = useState([])
  const [exporting, setExporting] = useState(false)
  const [refreshVersion, setRefreshVersion] = useState(0)
  const [listState, setListState] = useState({ key: '', error: '' })
  const [selectedId, setSelectedId] = useState(null)
  const [application, setApplication] = useState(null)
  const [detailState, setDetailState] = useState({ id: null, loading: false, error: '' })
  const [actionLoading, setActionLoading] = useState(false)
  const [toast, setToast] = useState(null)
  const requestKey = `${page}|${pageSize}|${debouncedSearch}|${status}|${businessType}|${refreshVersion}`
  const loading = listState.key !== requestKey
  const error = listState.key === requestKey ? listState.error : ''
  const detailsLoading = selectedId !== null
    && (detailState.id !== selectedId || detailState.loading)
  const detailsError = detailState.id === selectedId ? detailState.error : ''

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedSearch(search.trim())
      setPage(0)
    }, 300)
    return () => window.clearTimeout(timer)
  }, [search])

  useEffect(() => {
    let active = true
    const key = requestKey
    getSellerApplications({
      page,
      size: pageSize,
      search: debouncedSearch || undefined,
      status: status || undefined,
      businessType: businessType || undefined,
    })
      .then((result) => {
        if (active) {
          setData(result)
          setListState({ key, error: '' })
        }
      })
      .catch((requestError) => {
        if (active) setListState({ key, error: getRequestError(requestError, 'Could not load seller applications.') })
      })
    return () => { active = false }
  }, [page, pageSize, debouncedSearch, status, businessType, refreshVersion, requestKey])

  useEffect(() => {
    if (selectedId === null) return undefined
    let active = true
    const id = selectedId
    getSellerApplication(id)
      .then((result) => {
        if (active) {
          setApplication(result)
          setDetailState({ id, loading: false, error: '' })
        }
      })
      .catch((requestError) => {
        if (active) {
          setDetailState({ id, loading: false, error: getRequestError(requestError, 'Could not load application details.') })
        }
      })
    return () => { active = false }
  }, [selectedId])

  useEffect(() => {
    if (!toast) return undefined
    const timer = window.setTimeout(() => setToast(null), 4500)
    return () => window.clearTimeout(timer)
  }, [toast])

  async function handleApprove() {
    if (!selectedId) return false
    setActionLoading(true)
    try {
      setApplication(await approveSellerApplication(selectedId))
      setToast({ type: 'success', message: 'Seller application approved. Seller access is now active.' })
      setRefreshVersion((current) => current + 1)
      return true
    } catch (requestError) {
      setToast({ type: 'error', message: getRequestError(requestError, 'Could not approve this application.') })
      return false
    } finally {
      setActionLoading(false)
    }
  }

  async function handleReject(reason) {
    if (!selectedId) return false
    setActionLoading(true)
    try {
      setApplication(await rejectSellerApplication(selectedId, reason))
      setToast({ type: 'success', message: 'Application rejected. The review reason has been recorded.' })
      setRefreshVersion((current) => current + 1)
      return true
    } catch (requestError) {
      setToast({ type: 'error', message: getRequestError(requestError, 'Could not reject this application.') })
      return false
    } finally {
      setActionLoading(false)
    }
  }

  function closeDetails() {
    if (actionLoading) return
    setSelectedId(null)
    setApplication(null)
    setDetailState({ id: null, loading: false, error: '' })
  }

  function openDetails(id) {
    setSelectedId(id)
    setApplication(null)
    setDetailState({ id, loading: true, error: '' })
  }

  async function handleExport() {
    setExporting(true)
    try {
      const blob = await exportAdminSellers({ search: debouncedSearch || undefined, status: status || undefined, businessType: businessType || undefined })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'shopsphere-seller-applications.csv'
      link.click()
      URL.revokeObjectURL(url)
    } catch (requestError) {
      setToast({ type: 'error', message: getRequestError(requestError, 'Could not export applications.') })
    } finally {
      setExporting(false)
    }
  }

  function resetFilters() {
    setSearch('')
    setDebouncedSearch('')
    setStatus('')
    setBusinessType('')
    setPage(0)
    setSelectedRows([])
  }

  function toggleRow(id) {
    setSelectedRows((current) => current.includes(id) ? current.filter((selectedId) => selectedId !== id) : [...current, id])
  }

  const counts = data?.statusCounts
  const totalPages = data?.totalPages ?? 0
  const stats = [
    { key: 'total', label: 'Total Applications', value: counts?.total ?? 0, icon: 'sellers', tone: 'blue' },
    { key: 'approved', label: 'Approved', value: counts?.approved ?? 0, icon: 'approved', tone: 'green' },
    { key: 'pending', label: 'Pending', value: counts?.pending ?? 0, icon: 'pending', tone: 'amber' },
    { key: 'rejected', label: 'Rejected', value: counts?.rejected ?? 0, icon: 'rejected', tone: 'rose' },
  ]

  return (
    <AdminLayout pageTitle="Seller Applications" pageSubtitle="Review and manage seller applications from users." breadcrumb="Seller Applications" showDateFilter={false} onExport={handleExport} exporting={exporting} exportLabel="Export Applications">
      <div className="seller-admin-page">
      <div className="seller-admin-content">
        <section aria-label="Seller application totals" className="seller-admin-stats">
          {stats.map((stat, index) => (
            <StatCard
              icon={stat.icon}
              index={index}
              key={stat.key}
              label={stat.label}
              loading={!data}
              tone={stat.tone}
              value={stat.value}
            />
          ))}
        </section>

        <section className="seller-admin-list-card">
          <div className="seller-admin-list-heading">
            <div>
              <h2>Applications</h2>
              <p>{data ? `${data.totalElements.toLocaleString()} matching applications` : 'Review and decide on new seller applications.'}</p>
            </div>
            <button aria-label="Refresh applications" className="seller-admin-refresh" disabled={loading} onClick={() => setRefreshVersion((current) => current + 1)} type="button">
              <svg aria-hidden="true" fill="none" viewBox="0 0 20 20"><path d="M16.5 7.5A6.8 6.8 0 0 0 4.8 5.1L3.5 6.5m0 0V3.8m0 2.7h2.7M3.5 12.5a6.8 6.8 0 0 0 11.7 2.4l1.3-1.4m0 0v2.7m0-2.7h-2.7" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" /></svg>
              Refresh
            </button>
          </div>

          <div className="seller-admin-toolbar">
            <label className="seller-admin-search">
              <Icon name="search" />
              <span className="seller-admin-sr-only">Search seller name, email or business</span>
              <input onChange={(event) => setSearch(event.target.value)} placeholder="Search name, email or business…" type="search" value={search} />
              {search && <button aria-label="Clear search" onClick={() => setSearch('')} type="button">×</button>}
            </label>
            <label className="seller-admin-filter">
              <span>Status</span>
              <select onChange={(event) => { setStatus(event.target.value); setPage(0) }} value={status}>
                <option value="">All statuses</option>
                <option value="PENDING">Pending</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
              </select>
            </label>
            <label className="seller-admin-filter">
              <span>Business type</span>
              <select onChange={(event) => { setBusinessType(event.target.value); setPage(0) }} value={businessType}>
                <option value="">All business types</option>
                {BUSINESS_TYPES.map((type) => <option key={type} value={type}>{type.replaceAll('_', ' ')}</option>)}
              </select>
            </label>
            <button className="seller-admin-open-button seller-admin-search-submit" onClick={() => { setDebouncedSearch(search.trim()); setPage(0) }} type="button"><Icon name="search" /> Search</button>
            <button className="seller-admin-refresh" onClick={resetFilters} type="button">Reset</button>
          </div>

          {error && <div className="seller-admin-error" role="alert">{error}<button onClick={() => setRefreshVersion((current) => current + 1)} type="button">Retry</button></div>}

          {!data && loading ? <LoadingSkeleton /> : (
            <div className="seller-admin-table-wrap">
              <table className="seller-admin-table">
                <thead>
                  <tr><th><input aria-label="Select all applications on this page" checked={Boolean(data?.items.length) && data.items.every((item) => selectedRows.includes(item.id))} onChange={(event) => setSelectedRows((current) => event.target.checked ? [...new Set([...current, ...data.items.map((item) => item.id)])] : current.filter((id) => !data.items.some((item) => item.id === id)))} type="checkbox" /></th><th>Applicant</th><th>Business details</th><th>Documents</th><th>Status</th><th>Applied on</th><th>Actions</th></tr>
                </thead>
                <tbody>
                  {loading && data?.items.length === 0 && (
                    <tr><td colSpan="7"><span className="seller-admin-inline-loading"><span className="seller-admin-spinner" /> Loading applications…</span></td></tr>
                  )}
                  {!loading && !data && error && (
                    <tr><td colSpan="7">
                      <div className="seller-admin-empty">
                        <strong>Applications are unavailable</strong>
                        <p>Retry to load the latest application data.</p>
                      </div>
                    </td></tr>
                  )}
                  {!loading && data?.items.length === 0 && (
                    <tr><td colSpan="7">
                      <div className="seller-admin-empty">
                        <span><Icon name="document" /></span>
                        <strong>No applications found</strong>
                        <p>Try changing the search or status filter.</p>
                      </div>
                    </td></tr>
                  )}
                  {data?.items.map((item) => (
                    <tr className="seller-admin-table-row" key={item.id}>
                      <td><input aria-label={`Select ${item.sellerName || item.businessName}`} checked={selectedRows.includes(item.id)} onChange={() => toggleRow(item.id)} type="checkbox" /></td>
                      <td><button className="seller-admin-applicant-cell" onClick={() => openDetails(item.id)} type="button"><span className="seller-admin-avatar">{(item.sellerName || '?').split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase()}</span><span><strong>{item.sellerName || 'Name unavailable'}</strong><small>{item.email || 'Email unavailable'}</small></span></button></td>
                      <td><strong>{item.businessName || 'Business name unavailable'}</strong><small className="seller-admin-table-business">{item.businessType?.replaceAll('_', ' ') || 'Business type unavailable'} · {item.businessDescription || 'No description provided'}</small></td>
                      <td><span className="seller-admin-doc-unavailable">Unavailable</span></td>
                      <td><StatusBadge status={item.status} /></td>
                      <td className="seller-admin-date">{formatDate(item.submittedAt)}</td>
                      <td><button aria-label={`View ${item.businessName} application`} className="seller-admin-open-button" onClick={() => openDetails(item.id)} type="button">View</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <footer className="seller-admin-pagination">
            <span>{data ? `Showing ${data.totalElements ? page * pageSize + 1 : 0}–${Math.min((page + 1) * pageSize, data.totalElements)} of ${data.totalElements.toLocaleString()} applications` : ' '}</span>
            <div>
              <select aria-label="Rows per page" value={pageSize} onChange={(event) => { setPageSize(Number(event.target.value)); setPage(0) }}>{PAGE_SIZES.map((size) => <option key={size} value={size}>{size} / page</option>)}</select>
              <button disabled={page === 0 || loading} onClick={() => setPage((current) => Math.max(0, current - 1))} type="button">← Previous</button>
              <button disabled={totalPages === 0 || page + 1 >= totalPages || loading} onClick={() => setPage((current) => current + 1)} type="button">Next →</button>
            </div>
          </footer>
        </section>
      </div>

      <Toast onDismiss={() => setToast(null)} toast={toast} />
      {selectedId !== null && (
        <ApplicationDrawer
          actionLoading={actionLoading}
          application={application}
          error={detailsError}
          loading={detailsLoading}
          onApprove={handleApprove}
          onClose={closeDetails}
          onReject={handleReject}
        />
      )}
      </div>
    </AdminLayout>
  )
}
