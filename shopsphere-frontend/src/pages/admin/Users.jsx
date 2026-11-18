import { useCallback, useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import AdminLayout from '../../layouts/AdminLayout'
import StatsCard from '../../components/dashboard/StatsCard'
import {
  exportAdminUsers,
  getAdminUser,
  getAdminUserStats,
  getAdminUsers,
  updateAdminUser,
  updateAdminUserStatus,
} from '../../services/adminService'
import { getRequestError } from '../../services/authService'
import './Users.css'
import './users-popups.css'

const ROLE_OPTIONS = ['CUSTOMER', 'SELLER', 'DELIVERY_PARTNER', 'SUPER_ADMIN']
const PAGE_SIZES = [10, 25, 50, 100]

function Icon({ name, size = 18 }) {
  const paths = {
    users: <><path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="10" cy="7" r="4"/><path d="M20 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8"/></>,
    search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
    reset: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.6 9a7 7 0 0 1 11.7-2L20 12M4 12l2.7 5a7 7 0 0 0 11.7-2"/></>,
    eye: <><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></>,
    edit: <><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></>,
    more: <><circle cx="12" cy="5" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="12" cy="19" r="1"/></>,
    close: <><path d="m18 6-12 12M6 6l12 12"/></>,
    download: <><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="m7 10 5 5 5-5M12 15V3"/></>,
    chevron: <path d="m9 18 6-6-6-6"/>,
    lock: <><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 1 1 8 0v3"/></>,
    unlock: <><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 7.5-2"/></>,
    check: <path d="m5 12 4 4L19 6"/>,
    left: <path d="m15 18-6-6 6-6"/>,
    right: <path d="m9 18 6-6-6-6"/>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
}

function RoleBadge({ role }) {
  return <span className={`users-role users-role--${role?.toLowerCase() || 'unknown'}`}>{(role || 'Unavailable').replaceAll('_', ' ')}</span>
}

function StatusBadge({ status }) {
  const label = status === 'SUSPENDED' ? 'Blocked' : status === 'INACTIVE' ? 'Inactive' : status === 'ACTIVE' ? 'Active' : 'Unavailable'
  return <span className={`users-status users-status--${status?.toLowerCase() || 'unknown'}`}><i/>{label}</span>
}

function UserDrawer({ user, details, loading, error, onClose, onEdit }) {
  if (!user) return null
  const record = details?.user || user
  return <div className="users-drawer-layer">
    <button className="users-drawer-scrim" type="button" aria-label="Close user details" onClick={onClose}/>
    <aside className="users-drawer" role="dialog" aria-modal="true" aria-labelledby="user-drawer-title">
      <header className="users-drawer-head"><div><span className="users-kicker">USER PROFILE</span><h2 id="user-drawer-title">User details</h2></div><button className="users-icon-button" type="button" onClick={onClose} aria-label="Close"><Icon name="close"/></button></header>
      {loading ? <div className="users-drawer-loading"><i/><i/><i/><i/></div> : error ? <div className="users-inline-error">{error}</div> : <>
        <section className="users-profile-hero"><div className="users-avatar users-avatar--large">{record.fullName?.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase() || '?'}</div><div><h3>{record.fullName || 'Unnamed user'}</h3><p>{record.email}</p><RoleBadge role={record.role}/></div></section>
        <section className="users-detail-section"><h3>Account information</h3><dl>
          <div><dt>Mobile number</dt><dd>{record.phone || 'Not provided'}</dd></div>
          <div><dt>Role</dt><dd><RoleBadge role={record.role}/></dd></div>
          <div><dt>Account status</dt><dd><StatusBadge status={record.accountStatus}/></dd></div>
          <div><dt>Registration date</dt><dd>{formatDate(record.createdAt)}</dd></div>
        </dl></section>
        {details?.storeName&&<section className="users-detail-section"><h3>Seller information</h3><dl>
          <div><dt>Store name</dt><dd>{details.storeName}</dd></div>
          {details.businessType&&<div><dt>Business type</dt><dd>{details.businessType.replaceAll('_',' ')}</dd></div>}
          {details.sellerStatus&&<div><dt>Seller approval</dt><dd>{details.sellerStatus.replaceAll('_',' ')}</dd></div>}
          {details.businessAddress&&<div><dt>Business address</dt><dd>{details.businessAddress}</dd></div>}
        </dl></section>}
        <footer className="users-drawer-actions"><button type="button" className="users-secondary-button" onClick={onClose}>Close</button><button type="button" className="users-primary-button" onClick={()=>onEdit(record)}><Icon name="edit" size={16}/> Edit user</button></footer>
      </>}
    </aside>
  </div>
}

export default function Users() {
  const [users, setUsers] = useState([])
  const [stats, setStats] = useState(null)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [statsError, setStatsError] = useState('')
  const [error, setError] = useState('')
  const [exporting, setExporting] = useState(false)
  const [searchInput, setSearchInput] = useState('')
  const [filters, setFilters] = useState({ search: '', role: '', status: '' })
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [selectedIds, setSelectedIds] = useState([])
  const [menuId, setMenuId] = useState(null)
  const [drawerUser, setDrawerUser] = useState(null)
  const [drawerDetails, setDrawerDetails] = useState(null)
  const [drawerLoading, setDrawerLoading] = useState(false)
  const [drawerError, setDrawerError] = useState('')
  const [editUser, setEditUser] = useState(null)
  const [editValues, setEditValues] = useState({ fullName: '', email: '', phone: '' })
  const [saving, setSaving] = useState(false)
  const [confirmUser, setConfirmUser] = useState(null)
  const [actionError, setActionError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const result = await getAdminUsers({ page, size, search: filters.search || undefined, role: filters.role || undefined, status: filters.status || undefined })
      setUsers(result.items || [])
      setTotalElements(result.totalElements || 0)
      setTotalPages(result.totalPages || 0)
      setSelectedIds([])
    } catch (requestError) {
      setError(getRequestError(requestError, 'Users could not be loaded.'))
      setUsers([])
      setTotalElements(0)
      setTotalPages(0)
    } finally {
      setLoading(false)
    }
  }, [filters, page, size])

  const loadStats = useCallback(async () => {
    try {
      setStats(await getAdminUserStats())
      setStatsError('')
    } catch (requestError) {
      setStats(null)
      setStatsError(getRequestError(requestError, 'User statistics are unavailable.'))
    }
  }, [])

  useEffect(() => { load() }, [load])
  useEffect(() => { loadStats() }, [loadStats])

  const statCards = useMemo(() => [
    { title: 'Total Users', value: stats?.totalUsers?.toLocaleString() ?? '—', icon: 'customers', color: '#554cf1', tone: 'purple' },
    { title: 'Customers', value: stats?.customers?.toLocaleString() ?? '—', icon: 'customers', color: '#16a985', tone: 'green' },
    { title: 'Sellers', value: stats?.sellers?.toLocaleString() ?? '—', icon: 'sellers', color: '#f28b27', tone: 'orange' },
    { title: 'Delivery Partners', value: stats?.deliveryPartners?.toLocaleString() ?? '—', icon: 'orders', color: '#3284ee', tone: 'blue' },
  ], [stats])

  async function openUser(user) {
    setDrawerUser(user)
    setDrawerDetails(null)
    setDrawerError('')
    setDrawerLoading(true)
    try { setDrawerDetails(await getAdminUser(user.id)) }
    catch (requestError) { setDrawerError(getRequestError(requestError, 'User details could not be loaded.')) }
    finally { setDrawerLoading(false) }
  }

  function startEdit(user) {
    setDrawerUser(null)
    setEditUser(user)
    setEditValues({ fullName: user.fullName || '', email: user.email || '', phone: user.phone || '' })
    setActionError('')
  }

  async function saveUser(event) {
    event.preventDefault()
    setSaving(true)
    setActionError('')
    try {
      await updateAdminUser(editUser.id, editValues)
      setEditUser(null)
      await Promise.all([load(), loadStats()])
    } catch (requestError) { setActionError(getRequestError(requestError, 'User details could not be saved.')) }
    finally { setSaving(false) }
  }

  async function changeStatus(user, accountStatus) {
    setActionError('')
    try {
      await updateAdminUserStatus(user.id, accountStatus)
      setConfirmUser(null)
      setMenuId(null)
      await Promise.all([load(), loadStats()])
    } catch (requestError) { setActionError(getRequestError(requestError, 'User status could not be updated.')) }
  }

  async function handleExport() {
    setExporting(true)
    setActionError('')
    try {
      const blob = await exportAdminUsers({ search: filters.search || undefined, role: filters.role || undefined, status: filters.status || undefined })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'shopsphere-users.csv'
      link.click()
      URL.revokeObjectURL(url)
    } catch (requestError) { setActionError(getRequestError(requestError, 'Users could not be exported.')) }
    finally { setExporting(false) }
  }

  function applyFilters(event) {
    event.preventDefault()
    setPage(0)
    setFilters((current) => ({ ...current, search: searchInput.trim() }))
  }

  function resetFilters() {
    setSearchInput('')
    setFilters({ search: '', role: '', status: '' })
    setPage(0)
  }

  const firstRecord = totalElements === 0 ? 0 : page * size + 1
  const lastRecord = Math.min((page + 1) * size, totalElements)
  const pageNumbers = Array.from({ length: totalPages }, (_, index) => index).filter((number) => number === 0 || number === totalPages - 1 || Math.abs(number - page) <= 1)

  return <AdminLayout pageTitle="Users Management" pageSubtitle="Manage all users, view details and control their status." breadcrumb="Users" titleIcon="users" showDateFilter={false} onExport={handleExport} exporting={exporting} exportLabel="Export Users">
    <motion.div className="admin-dashboard-content users-page-content" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: .22 }}>
      {actionError&&<div className="users-action-error" role="alert">{actionError}<button type="button" onClick={()=>setActionError('')} aria-label="Dismiss"><Icon name="close" size={15}/></button></div>}
      {statsError&&<div className="users-stats-unavailable" role="status">Statistics unavailable: {statsError}</div>}
      <div className="admin-stats-grid users-stats-grid">{statCards.map((metric)=><StatsCard key={metric.title} {...metric}/>)}</div>

      <form className="users-filter-bar" onSubmit={applyFilters}>
        <label className="users-search"><Icon name="search" size={20}/><input type="search" placeholder="Search by name, email or phone..." value={searchInput} onChange={(event)=>setSearchInput(event.target.value)} aria-label="Search by name, email or phone"/></label>
        <label className="users-select-wrap"><span className="users-sr-only">Filter by role</span><select value={filters.role} onChange={(event)=>{setPage(0);setFilters((current)=>({...current,role:event.target.value}))}}><option value="">All Roles</option>{ROLE_OPTIONS.map((role)=><option key={role} value={role}>{role.replaceAll('_',' ')}</option>)}</select></label>
        <label className="users-select-wrap"><span className="users-sr-only">Filter by status</span><select value={filters.status} onChange={(event)=>{setPage(0);setFilters((current)=>({...current,status:event.target.value}))}}><option value="">All Status</option><option value="ACTIVE">Active</option><option value="SUSPENDED">Blocked</option><option value="INACTIVE">Inactive</option></select></label>
        <button className="users-primary-button users-search-button" type="submit"><Icon name="search"/> Search</button>
        <button className="users-secondary-button users-reset-button" type="button" onClick={resetFilters}><Icon name="reset"/> Reset</button>
      </form>

      <section className="users-table-card" aria-label="Users list">
        {loading?<div className="users-table-skeleton" role="status" aria-label="Loading users">{Array.from({length:6},(_,index)=><div key={index}><i/><i/><i/><i/></div>)}</div>:error?<div className="users-empty-state users-empty-state--error"><strong>Users couldn’t be loaded</strong><p>{error}</p><button type="button" className="users-secondary-button" onClick={load}>Try again</button></div>:users.length===0?<div className="users-empty-state"><span><Icon name="users" size={28}/></span><strong>No users found</strong><p>Try another search or reset the filters.</p><button type="button" className="users-secondary-button" onClick={resetFilters}>Reset filters</button></div>:<>
          {selectedIds.length>0&&<div className="users-selection-bar"><strong>{selectedIds.length} selected</strong><button type="button" onClick={()=>setSelectedIds([])}>Clear selection</button></div>}
          <div className="users-table-scroll"><table className="users-table"><thead><tr><th className="users-check-cell"><input type="checkbox" aria-label="Select all users on this page" checked={users.length>0&&selectedIds.length===users.length} onChange={(event)=>setSelectedIds(event.target.checked?users.map((user)=>user.id):[])}/></th><th>User</th><th>Email</th><th>Phone</th><th>Role</th><th>Status</th><th>Join Date</th><th>Actions</th></tr></thead><tbody>{users.map((user)=><tr key={user.id}>
            <td className="users-check-cell"><input type="checkbox" aria-label={`Select ${user.fullName}`} checked={selectedIds.includes(user.id)} onChange={(event)=>setSelectedIds((current)=>event.target.checked?[...current,user.id]:current.filter((id)=>id!==user.id))}/></td>
            <td><button type="button" className="users-person" onClick={()=>openUser(user)}><span className="users-avatar">{user.fullName?.split(/\s+/).map((part)=>part[0]).join('').slice(0,2).toUpperCase()||'?'}</span><span><strong>{user.fullName||'Unnamed user'}</strong><small>{user.role?.replaceAll('_',' ')||'Unknown role'}</small></span></button></td>
            <td className="users-email-cell">{user.email||'—'}</td><td>{user.phone||'—'}</td><td><RoleBadge role={user.role}/></td><td><StatusBadge status={user.accountStatus}/></td><td className="users-date-cell">{formatDate(user.createdAt)}</td>
            <td className="users-actions-cell"><button className="users-icon-button" type="button" title="View user" aria-label={`View ${user.fullName}`} onClick={()=>openUser(user)}><Icon name="eye" size={19}/></button><button className="users-icon-button" type="button" title="Edit user" aria-label={`Edit ${user.fullName}`} onClick={()=>startEdit(user)}><Icon name="edit" size={17}/></button><div className="users-row-menu-wrap"><button className="users-icon-button" type="button" title="More actions" aria-label={`More actions for ${user.fullName}`} aria-expanded={menuId===user.id} onClick={()=>setMenuId(menuId===user.id?null:user.id)}><Icon name="more"/></button>{menuId===user.id&&<div className="users-row-menu"><button type="button" onClick={()=>{setMenuId(null);openUser(user)}}><Icon name="eye" size={15}/> View details</button><button type="button" onClick={()=>{setMenuId(null);startEdit(user)}}><Icon name="edit" size={15}/> Edit user</button>{user.role==='SUPER_ADMIN'?<span className="users-menu-note">Admin account status is protected</span>:user.accountStatus==='ACTIVE'?<button className="users-menu-danger" type="button" onClick={()=>{setMenuId(null);setConfirmUser(user)}}><Icon name="lock" size={15}/> Block user</button>:<button type="button" onClick={()=>changeStatus(user,'ACTIVE')}><Icon name="unlock" size={15}/> Activate user</button>}</div>}</div></td>
          </tr>)}</tbody></table></div>
          <footer className="users-table-footer"><p>Showing <strong>{firstRecord}–{lastRecord}</strong> of <strong>{totalElements.toLocaleString()}</strong> users</p><div className="users-pagination"><button className="users-icon-button" type="button" aria-label="Previous page" disabled={page<=0} onClick={()=>setPage((current)=>Math.max(0,current-1))}><Icon name="left"/></button>{pageNumbers.map((number,index)=><span className="users-page-group" key={number}>{index>0&&number-pageNumbers[index-1]>1&&<i>…</i>}<button type="button" className={number===page?'is-current':''} aria-current={number===page?'page':undefined} onClick={()=>setPage(number)}>{number+1}</button></span>)}<button className="users-icon-button" type="button" aria-label="Next page" disabled={page>=totalPages-1} onClick={()=>setPage((current)=>Math.min(totalPages-1,current+1))}><Icon name="right"/></button><label className="users-page-size"><select value={size} onChange={(event)=>{setSize(Number(event.target.value));setPage(0)}} aria-label="Users per page">{PAGE_SIZES.map((option)=><option value={option} key={option}>{option} / page</option>)}</select></label></div></footer>
        </>}
      </section>
    </motion.div>
    <UserDrawer user={drawerUser} details={drawerDetails} loading={drawerLoading} error={drawerError} onClose={()=>setDrawerUser(null)} onEdit={startEdit}/>
    {editUser&&<div className="users-modal-layer"><button className="users-modal-scrim" type="button" aria-label="Close edit form" onClick={()=>setEditUser(null)}/><section className="users-edit-modal" role="dialog" aria-modal="true" aria-labelledby="users-edit-title"><header><div><span className="users-kicker">USER MANAGEMENT</span><h2 id="users-edit-title">Edit user</h2></div><button className="users-icon-button" type="button" onClick={()=>setEditUser(null)} aria-label="Close"><Icon name="close"/></button></header><form onSubmit={saveUser}><label>Full name<input required maxLength={100} value={editValues.fullName} onChange={(event)=>setEditValues((current)=>({...current,fullName:event.target.value}))}/></label><label>Email address<input required type="email" maxLength={254} value={editValues.email} onChange={(event)=>setEditValues((current)=>({...current,email:event.target.value}))}/></label><label>Phone number<input type="tel" maxLength={20} value={editValues.phone} onChange={(event)=>setEditValues((current)=>({...current,phone:event.target.value}))}/></label><div className="users-role-locked"><span>Role</span><RoleBadge role={editUser.role}/><small>Roles are managed through their respective approval workflows.</small></div>{actionError&&<p className="users-form-error" role="alert">{actionError}</p>}<footer><button className="users-secondary-button" type="button" onClick={()=>setEditUser(null)}>Cancel</button><button className="users-primary-button" type="submit" disabled={saving}>{saving?'Saving...':'Save changes'}</button></footer></form></section></div>}
    {confirmUser&&<div className="users-modal-layer"><button className="users-modal-scrim" type="button" aria-label="Cancel block action" onClick={()=>setConfirmUser(null)}/><section className="users-confirm-modal" role="alertdialog" aria-modal="true" aria-labelledby="users-confirm-title"><span className="users-confirm-icon"><Icon name="lock" size={22}/></span><h2 id="users-confirm-title">Block this user?</h2><p><strong>{confirmUser.fullName}</strong> will no longer be able to sign in while their account is blocked.</p>{actionError&&<p className="users-form-error" role="alert">{actionError}</p>}<footer><button type="button" className="users-secondary-button" onClick={()=>setConfirmUser(null)}>Cancel</button><button type="button" className="users-danger-button" onClick={()=>changeStatus(confirmUser,'SUSPENDED')}>Block user</button></footer></section></div>}
  </AdminLayout>
}
