import { useCallback, useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import AdminLayout from '../../layouts/AdminLayout'
import StatsCard from '../../components/dashboard/StatsCard'
import {
  activateSeller,
  approveSellerApplication,
  exportAdminSellers,
  getSellerApplication,
  getSellerApplications,
  rejectSellerApplication,
  suspendSeller,
  updateSellerProfile,
} from '../../services/adminService'
import { getRequestError } from '../../services/authService'
import './Sellers.css'

const PAGE_SIZES = [5, 10, 25, 50]
const BUSINESS_TYPES = ['INDIVIDUAL', 'SOLE_PROPRIETORSHIP', 'PARTNERSHIP', 'LIMITED_LIABILITY_PARTNERSHIP', 'PRIVATE_LIMITED', 'PUBLIC_LIMITED', 'OTHER']

function Icon({ name, size = 18 }) {
  const paths = {
    store: <><path d="m3 10 2-6h14l2 6"/><path d="M4 10v10h16V10M9 20v-6h6v6"/><path d="M3 10h18"/></>,
    search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
    reset: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.6 9a7 7 0 0 1 11.7-2L20 12M4 12l2.7 5a7 7 0 0 0 11.7-2"/></>,
    eye: <><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></>,
    edit: <><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></>,
    more: <><circle cx="12" cy="5" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="12" cy="19" r="1"/></>,
    close: <><path d="m18 6-12 12M6 6l12 12"/></>,
    download: <><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="m7 10 5 5 5-5M12 15V3"/></>,
    left: <path d="m15 18-6-6 6-6"/>,
    right: <path d="m9 18 6-6-6-6"/>,
    check: <path d="m5 12 4 4L19 6"/>,
    reject: <path d="m18 6-12 12M6 6l12 12"/>,
    lock: <><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 1 1 8 0v3"/></>,
    unlock: <><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 7.5-2"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

const dateLabel = (value) => {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString(undefined, { month: 'short', day: '2-digit', year: 'numeric' })
}

function sellerStatus(seller) {
  if (seller.accountStatus === 'SUSPENDED') return 'SUSPENDED'
  if (seller.accountStatus === 'INACTIVE') return 'INACTIVE'
  return seller.status || 'UNKNOWN'
}

function StatusBadge({ seller }) {
  const status = sellerStatus(seller)
  const label = { APPROVED: 'Approved', PENDING: 'Pending', REJECTED: 'Rejected', SUSPENDED: 'Suspended', INACTIVE: 'Inactive' }[status] || 'Unavailable'
  return <span className={`sellers-status sellers-status--${status.toLowerCase()}`}><i/>{label}</span>
}

function SellerDrawer({ seller, details, loading, error, close, edit }) {
  if (!seller) return null
  const row = details || seller
  const initials = row.sellerName?.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase() || '?'
  return <div className="sellers-drawer-layer"><button className="sellers-scrim" type="button" aria-label="Close seller details" onClick={close}/><aside className="sellers-drawer" role="dialog" aria-modal="true" aria-labelledby="seller-drawer-title">
    <header className="sellers-drawer-header"><div><span className="sellers-kicker">SELLER PROFILE</span><h2 id="seller-drawer-title">Store details</h2></div><button className="sellers-icon-button" type="button" aria-label="Close" onClick={close}><Icon name="close"/></button></header>
    {loading?<div className="sellers-drawer-loading" role="status"><i/><i/><i/><i/></div>:error?<div className="sellers-error-inline">{error}</div>:<>
      <section className="sellers-profile"><div className="sellers-avatar sellers-avatar--large">{initials}</div><div><h3>{row.sellerName}</h3><p>{row.email}</p><StatusBadge seller={row}/></div></section>
      <section className="sellers-info-section"><h3>Seller information</h3><dl><div><dt>Phone</dt><dd>{row.phone||'Not provided'}</dd></div><div><dt>Seller since</dt><dd>{dateLabel(row.submittedAt)}</dd></div><div><dt>Approval status</dt><dd><StatusBadge seller={row}/></dd></div>{row.rejectionReason&&<div><dt>Rejection reason</dt><dd>{row.rejectionReason}</dd></div>}{row.suspensionReason&&<div><dt>Suspension reason</dt><dd>{row.suspensionReason}</dd></div>}</dl></section>
      <section className="sellers-info-section"><h3>Store information</h3><dl><div><dt>Store name</dt><dd>{row.businessName}</dd></div><div><dt>Business type</dt><dd>{row.businessType?.replaceAll('_',' ')||'Not provided'}</dd></div><div><dt>Description</dt><dd>{row.businessDescription||'Not provided'}</dd></div><div><dt>Business address</dt><dd>{row.businessAddress||'Not provided'}</dd></div><div><dt>GST number</dt><dd>{row.gstNumber||'Not provided'}</dd></div></dl></section>
      <section className="sellers-metrics"><article><span>Total products</span><strong>{row.totalProducts?.toLocaleString()??'—'}</strong></article><article><span>Total orders</span><strong>{row.totalOrders?.toLocaleString()??'—'}</strong></article></section>
      <footer className="sellers-drawer-actions"><button type="button" className="sellers-secondary-button" onClick={close}>Close</button><button type="button" className="sellers-primary-button" onClick={()=>edit(row)}><Icon name="edit" size={16}/> Edit seller</button></footer>
    </>}
  </aside></div>
}

export default function Sellers() {
  const [sellers, setSellers] = useState([])
  const [counts, setCounts] = useState(null)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(5)
  const [total, setTotal] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [filters, setFilters] = useState({ search: '', status: '', businessType: '' })
  const [searchInput, setSearchInput] = useState('')
  const [selected, setSelected] = useState([])
  const [menuId, setMenuId] = useState(null)
  const [drawerSeller, setDrawerSeller] = useState(null)
  const [drawerDetails, setDrawerDetails] = useState(null)
  const [drawerLoading, setDrawerLoading] = useState(false)
  const [drawerError, setDrawerError] = useState('')
  const [editSeller, setEditSeller] = useState(null)
  const [editValues, setEditValues] = useState({})
  const [saving, setSaving] = useState(false)
  const [dialog, setDialog] = useState(null)
  const [reason, setReason] = useState('')
  const [working, setWorking] = useState(false)
  const [exporting, setExporting] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const params = {
        page, size,
        search: filters.search || undefined,
        status: ['PENDING', 'APPROVED', 'REJECTED'].includes(filters.status) ? filters.status : undefined,
        accountStatus: ['SUSPENDED', 'INACTIVE'].includes(filters.status) ? filters.status : undefined,
        businessType: filters.businessType || undefined,
      }
      const response = await getSellerApplications(params)
      setSellers(response.items || [])
      setCounts(response.statusCounts || null)
      setTotal(response.totalElements || 0)
      setTotalPages(response.totalPages || 0)
      setSelected([])
    } catch (requestError) {
      setError(getRequestError(requestError, 'Seller records could not be loaded.'))
      setSellers([])
      setTotal(0)
      setTotalPages(0)
    } finally { setLoading(false) }
  }, [filters, page, size])

  useEffect(() => { load() }, [load])

  const statCards = useMemo(() => [
    { title: 'Total Sellers', value: counts?.total?.toLocaleString() ?? '—', icon: 'sellers', color: '#3284ee', tone: 'blue' },
    { title: 'Approved Sellers', value: counts?.approved?.toLocaleString() ?? '—', icon: 'customers', color: '#16a985', tone: 'green' },
    { title: 'Pending Approval', value: counts?.pending?.toLocaleString() ?? '—', icon: 'orders', color: '#f28b27', tone: 'orange' },
    { title: 'Rejected Sellers', value: counts?.rejected?.toLocaleString() ?? '—', icon: 'customers', color: '#e34f62', tone: 'purple' },
  ], [counts])

  async function viewSeller(seller) {
    setDrawerSeller(seller)
    setDrawerDetails(null)
    setDrawerError('')
    setDrawerLoading(true)
    try { setDrawerDetails(await getSellerApplication(seller.id)) }
    catch (requestError) { setDrawerError(getRequestError(requestError, 'Seller details could not be loaded.')) }
    finally { setDrawerLoading(false) }
  }

  function beginEdit(seller) {
    setDrawerSeller(null)
    setEditSeller(seller)
    setEditValues({
      storeName: seller.businessName || '',
      sellerName: seller.sellerName || '',
      email: seller.email || '',
      phone: seller.phone || '',
      businessType: seller.businessType || 'OTHER',
      businessDescription: seller.businessDescription || '',
      businessAddress: seller.businessAddress || '',
      gstNumber: seller.gstNumber || '',
    })
    setActionError('')
  }

  async function saveSeller(event) {
    event.preventDefault()
    setSaving(true)
    setActionError('')
    try { await updateSellerProfile(editSeller.id, editValues); setEditSeller(null); await load() }
    catch (requestError) { setActionError(getRequestError(requestError, 'Seller details could not be saved.')) }
    finally { setSaving(false) }
  }

  async function runAction() {
    if (!dialog) return
    setWorking(true)
    setActionError('')
    try {
      if (dialog.action === 'approve') await approveSellerApplication(dialog.seller.id)
      if (dialog.action === 'reject') await rejectSellerApplication(dialog.seller.id, reason.trim())
      if (dialog.action === 'suspend') await suspendSeller(dialog.seller.id, reason.trim())
      if (dialog.action === 'activate') await activateSeller(dialog.seller.id)
      setDialog(null)
      setReason('')
      await load()
    } catch (requestError) { setActionError(getRequestError(requestError, 'The seller action could not be completed.')) }
    finally { setWorking(false) }
  }

  async function exportSellers() {
    setExporting(true)
    setActionError('')
    try {
      const params = {
        search: filters.search || undefined,
        status: ['PENDING', 'APPROVED', 'REJECTED'].includes(filters.status) ? filters.status : undefined,
        accountStatus: ['SUSPENDED', 'INACTIVE'].includes(filters.status) ? filters.status : undefined,
        businessType: filters.businessType || undefined,
      }
      const blob = await exportAdminSellers(params)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'shopsphere-sellers.csv'
      link.click()
      URL.revokeObjectURL(url)
    } catch (requestError) { setActionError(getRequestError(requestError, 'Seller records could not be exported.')) }
    finally { setExporting(false) }
  }

  function applyFilters(event) {
    event.preventDefault()
    setPage(0)
    setFilters((current) => ({ ...current, search: searchInput.trim() }))
  }

  function resetFilters() {
    setSearchInput('')
    setFilters({ search: '', status: '', businessType: '' })
    setPage(0)
  }

  const pageNumbers = Array.from({ length: totalPages }, (_, index) => index).filter((number) => number === 0 || number === totalPages - 1 || Math.abs(number - page) <= 1)
  const first = total ? page * size + 1 : 0
  const last = Math.min((page + 1) * size, total)
  const hasAllSelected = sellers.length > 0 && selected.length === sellers.length

  return <AdminLayout pageTitle="Sellers Management" pageSubtitle="Manage seller accounts, view store details and control their status." breadcrumb="Sellers" titleIcon="store" showDateFilter={false} onExport={exportSellers} exporting={exporting} exportLabel="Export Sellers">
    <motion.div className="admin-dashboard-content sellers-page-content" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: .22 }}>
      {actionError&&<div className="sellers-action-error" role="alert">{actionError}<button type="button" onClick={()=>setActionError('')} aria-label="Dismiss"><Icon name="close" size={15}/></button></div>}
      <div className="admin-stats-grid sellers-stats-grid">{statCards.map((metric)=><StatsCard key={metric.title} {...metric}/>)}</div>
      <form className="sellers-filters" onSubmit={applyFilters}>
        <label className="sellers-search"><Icon name="search" size={20}/><input type="search" placeholder="Search by store name, seller name or email..." value={searchInput} onChange={(event)=>setSearchInput(event.target.value)} aria-label="Search sellers by store, seller name or email"/></label>
        <label className="sellers-select"><span className="sellers-sr-only">Filter by seller status</span><select value={filters.status} onChange={(event)=>{setPage(0);setFilters((current)=>({...current,status:event.target.value}))}}><option value="">All Status</option><option value="APPROVED">Approved</option><option value="PENDING">Pending</option><option value="REJECTED">Rejected</option><option value="SUSPENDED">Suspended</option><option value="INACTIVE">Inactive</option></select></label>
        <label className="sellers-select"><span className="sellers-sr-only">Filter by business type</span><select value={filters.businessType} onChange={(event)=>{setPage(0);setFilters((current)=>({...current,businessType:event.target.value}))}}><option value="">All Business Types</option>{BUSINESS_TYPES.map((type)=><option key={type} value={type}>{type.replaceAll('_',' ')}</option>)}</select></label>
        <button type="button" className="sellers-secondary-button" onClick={resetFilters}><Icon name="reset"/> Reset</button><button type="submit" className="sellers-primary-button"><Icon name="search"/> Search</button>
      </form>
      <section className="sellers-table-card" aria-label="Sellers list">
        {loading?<div className="sellers-table-skeleton" role="status" aria-label="Loading sellers">{Array.from({length:5},(_,index)=><div key={index}><i/><i/><i/><i/></div>)}</div>:error?<div className="sellers-empty sellers-empty--error"><strong>Sellers could not be loaded</strong><p>{error}</p><button type="button" className="sellers-secondary-button" onClick={load}>Try again</button></div>:sellers.length===0?<div className="sellers-empty"><span><Icon name="store" size={28}/></span><strong>No sellers found</strong><p>Try a different search or clear your filters.</p><button type="button" className="sellers-secondary-button" onClick={resetFilters}>Reset filters</button></div>:<>
          {selected.length>0&&<div className="sellers-selection-bar"><strong>{selected.length} selected</strong><button type="button" onClick={()=>setSelected([])}>Clear selection</button></div>}
          <div className="sellers-table-scroll"><table className="sellers-table"><thead><tr><th className="sellers-check"><input type="checkbox" aria-label="Select all sellers on this page" checked={hasAllSelected} onChange={(event)=>setSelected(event.target.checked?sellers.map((seller)=>seller.id):[])}/></th><th>Seller</th><th>Store Details</th><th>Business Type</th><th>Status</th><th>Join Date</th><th>Actions</th></tr></thead><tbody>{sellers.map((seller)=>{
            const status = sellerStatus(seller)
            const initials = seller.sellerName?.split(/\s+/).map((part)=>part[0]).join('').slice(0,2).toUpperCase() || '?'
            return <tr key={seller.id}>
              <td className="sellers-check"><input type="checkbox" aria-label={`Select ${seller.sellerName}`} checked={selected.includes(seller.id)} onChange={(event)=>setSelected((current)=>event.target.checked?[...current,seller.id]:current.filter((id)=>id!==seller.id))}/></td>
              <td><button className="sellers-person" type="button" onClick={()=>viewSeller(seller)}><span className="sellers-avatar">{initials}</span><span><strong>{seller.sellerName||'Unnamed seller'}</strong><small>{seller.email||'—'}</small></span></button></td>
              <td className="sellers-store"><strong>{seller.businessName||'—'}</strong><small>{seller.businessDescription||'Store description unavailable'}</small></td>
              <td><span className={`sellers-business-type sellers-business-type--${seller.businessType?.toLowerCase()||'other'}`}>{seller.businessType?.replaceAll('_',' ')||'Not provided'}</span></td><td><StatusBadge seller={seller}/></td><td className="sellers-date">{dateLabel(seller.submittedAt)}</td>
              <td className="sellers-actions"><button className="sellers-icon-button" type="button" title="View seller" aria-label={`View ${seller.sellerName}`} onClick={()=>viewSeller(seller)}><Icon name="eye" size={19}/></button><button className="sellers-icon-button" type="button" title="Edit seller" aria-label={`Edit ${seller.sellerName}`} onClick={()=>beginEdit(seller)}><Icon name="edit" size={17}/></button><div className="sellers-menu-wrap"><button className="sellers-icon-button" type="button" title="More actions" aria-label={`More actions for ${seller.sellerName}`} aria-expanded={menuId===seller.id} onClick={()=>setMenuId(menuId===seller.id?null:seller.id)}><Icon name="more"/></button>{menuId===seller.id&&<div className="sellers-row-menu"><button type="button" onClick={()=>{setMenuId(null);viewSeller(seller)}}><Icon name="eye" size={15}/> View details</button><button type="button" onClick={()=>{setMenuId(null);beginEdit(seller)}}><Icon name="edit" size={15}/> Edit seller</button>{status==='PENDING'&&<><button type="button" onClick={()=>{setMenuId(null);setDialog({action:'approve',seller})}}><Icon name="check" size={15}/> Approve seller</button><button className="is-danger" type="button" onClick={()=>{setMenuId(null);setReason('');setDialog({action:'reject',seller})}}><Icon name="reject" size={15}/> Reject seller</button></>}{status==='APPROVED'&&<button className="is-danger" type="button" onClick={()=>{setMenuId(null);setReason('');setDialog({action:'suspend',seller})}}><Icon name="lock" size={15}/> Suspend seller</button>}{status==='SUSPENDED'&&<button type="button" onClick={()=>{setMenuId(null);setDialog({action:'activate',seller})}}><Icon name="unlock" size={15}/> Activate seller</button>}</div>}</div></td>
            </tr>
          })}</tbody></table></div>
          <footer className="sellers-table-footer"><p>Showing <strong>{first}–{last}</strong> of <strong>{total.toLocaleString()}</strong> sellers</p><div className="sellers-pagination"><button className="sellers-icon-button" type="button" aria-label="Previous page" disabled={page<=0} onClick={()=>setPage((current)=>Math.max(0,current-1))}><Icon name="left"/></button>{pageNumbers.map((number,index)=><span className="sellers-page-group" key={number}>{index>0&&number-pageNumbers[index-1]>1&&<i>…</i>}<button className={number===page?'is-current':''} type="button" aria-current={number===page?'page':undefined} onClick={()=>setPage(number)}>{number+1}</button></span>)}<button className="sellers-icon-button" type="button" aria-label="Next page" disabled={page>=totalPages-1} onClick={()=>setPage((current)=>Math.min(totalPages-1,current+1))}><Icon name="right"/></button><label className="sellers-page-size"><select value={size} aria-label="Sellers per page" onChange={(event)=>{setSize(Number(event.target.value));setPage(0)}}>{PAGE_SIZES.map((value)=><option value={value} key={value}>{value} / page</option>)}</select></label></div></footer>
        </>}
      </section>
    </motion.div>
    <SellerDrawer seller={drawerSeller} details={drawerDetails} loading={drawerLoading} error={drawerError} close={()=>setDrawerSeller(null)} edit={beginEdit}/>
    {editSeller&&<div className="sellers-modal-layer"><button className="sellers-scrim" type="button" aria-label="Close edit seller form" onClick={()=>setEditSeller(null)}/><section className="sellers-edit-modal" role="dialog" aria-modal="true" aria-labelledby="seller-edit-title"><header><div><span className="sellers-kicker">SELLER MANAGEMENT</span><h2 id="seller-edit-title">Edit seller information</h2></div><button className="sellers-icon-button" type="button" aria-label="Close" onClick={()=>setEditSeller(null)}><Icon name="close"/></button></header><form onSubmit={saveSeller}><label>Store name<input required maxLength={150} value={editValues.storeName||''} onChange={(event)=>setEditValues((current)=>({...current,storeName:event.target.value}))}/></label><label>Seller name<input required maxLength={100} value={editValues.sellerName||''} onChange={(event)=>setEditValues((current)=>({...current,sellerName:event.target.value}))}/></label><div className="sellers-form-grid"><label>Email<input required type="email" maxLength={254} value={editValues.email||''} onChange={(event)=>setEditValues((current)=>({...current,email:event.target.value}))}/></label><label>Phone<input required type="tel" maxLength={20} value={editValues.phone||''} onChange={(event)=>setEditValues((current)=>({...current,phone:event.target.value}))}/></label></div><label>Business type<select value={editValues.businessType||'OTHER'} onChange={(event)=>setEditValues((current)=>({...current,businessType:event.target.value}))}>{BUSINESS_TYPES.map((type)=><option key={type} value={type}>{type.replaceAll('_',' ')}</option>)}</select></label><label>Business description<textarea rows="3" maxLength={2000} value={editValues.businessDescription||''} onChange={(event)=>setEditValues((current)=>({...current,businessDescription:event.target.value}))}/></label><label>Business address<textarea required rows="3" maxLength={1000} value={editValues.businessAddress||''} onChange={(event)=>setEditValues((current)=>({...current,businessAddress:event.target.value}))}/></label><label>GST number<input maxLength={15} value={editValues.gstNumber||''} onChange={(event)=>setEditValues((current)=>({...current,gstNumber:event.target.value.toUpperCase()}))}/></label>{actionError&&<p className="sellers-form-error" role="alert">{actionError}</p>}<footer><button type="button" className="sellers-secondary-button" onClick={()=>setEditSeller(null)}>Cancel</button><button type="submit" className="sellers-primary-button" disabled={saving}>{saving?'Saving...':'Save changes'}</button></footer></form></section></div>}
    {dialog&&<div className="sellers-modal-layer"><button className="sellers-scrim" type="button" aria-label="Cancel seller action" onClick={()=>setDialog(null)}/><section className="sellers-confirm-modal" role="alertdialog" aria-modal="true" aria-labelledby="seller-confirm-title"><span className={`sellers-confirm-mark sellers-confirm-mark--${dialog.action}`}><Icon name={dialog.action==='approve'?'check':dialog.action==='activate'?'unlock':dialog.action==='suspend'?'lock':'reject'} size={22}/></span><h2 id="seller-confirm-title">{{approve:'Approve seller?',reject:'Reject seller application?',suspend:'Suspend this seller?',activate:'Activate this seller?'}[dialog.action]}</h2><p>{dialog.action==='approve'?<>Approve <strong>{dialog.seller.sellerName}</strong> and enable their seller access?</>:dialog.action==='reject'?<>This will reject <strong>{dialog.seller.sellerName}</strong>'s application.</>:dialog.action==='suspend'?<>Suspending <strong>{dialog.seller.sellerName}</strong> will prevent this account from signing in.</>:<>Restore seller sign-in access for <strong>{dialog.seller.sellerName}</strong>?</>}</p>{['reject','suspend'].includes(dialog.action)&&<label className="sellers-reason-field">{dialog.action==='reject'?'Rejection reason':'Suspension reason'}<textarea rows="4" maxLength={1000} required value={reason} onChange={(event)=>setReason(event.target.value)} placeholder="Enter a clear reason..."/></label>}{actionError&&<p className="sellers-form-error" role="alert">{actionError}</p>}<footer><button type="button" className="sellers-secondary-button" onClick={()=>setDialog(null)}>Cancel</button><button type="button" className={['reject','suspend'].includes(dialog.action)?'sellers-danger-button':'sellers-primary-button'} disabled={working||(['reject','suspend'].includes(dialog.action)&&!reason.trim())} onClick={runAction}>{working?'Please wait...':({approve:'Approve seller',reject:'Reject application',suspend:'Suspend seller',activate:'Activate seller'}[dialog.action])}</button></footer></section></div>}
  </AdminLayout>
}
