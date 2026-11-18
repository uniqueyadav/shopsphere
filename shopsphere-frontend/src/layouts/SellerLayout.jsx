import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { logout } from '../features/auth/authSlice'
import './SellerLayout.css'

const navigation = [
  ['Dashboard', '/seller', 'home', true],
  ['Products', '/seller/products', 'box', true],
  ['Add Product', '/seller/products/new', 'plus', true],
  ['Inventory', '/seller/inventory', 'inventory', true],
  ['Orders', '/seller/orders', 'cart', true],
  ['Earnings', '/seller/earnings', 'wallet', true],
  ['Payouts', '/seller/payouts', 'card', true],
  ['Sales Reports', '/seller/reports', 'chart'],
  ['Store Settings', '/seller/store-settings', 'gear', true],
  ['Seller Profile', '/seller/profile', 'user'],
  ['Support', '/seller/support', 'help'],
]

function Icon({ name, size = 20 }) {
  const paths = {
    home: <><path d="m3 10 9-7 9 7"/><path d="M5 9v11h14V9M9 20v-7h6v7"/></>,
    box: <><path d="m12 3 9 4.5v9L12 21l-9-4.5v-9L12 3Z"/><path d="m3.5 7.5 8.5 4 8.5-4M12 12v9M8 5l9 4.5"/></>,
    plus: <><path d="M12 5v14M5 12h14"/></>,
    inventory: <><path d="M4 7h16v13H4zM3 4h18v3H3zM8 11h8m-8 4h5"/></>,
    cart: <><path d="M3 4h2l2.2 11.2a2 2 0 0 0 2 1.6h8.4a2 2 0 0 0 1.9-1.4L21 9H6"/><circle cx="10" cy="20" r="1"/><circle cx="18" cy="20" r="1"/></>,
    wallet: <><rect x="3" y="5" width="18" height="15" rx="2"/><path d="M3 9h18m-5 5h2"/></>,
    card: <><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 10h18m-14 5h4"/></>,
    chart: <><path d="M4 19V9m5 10V5m5 14v-7m5 7V3M2 20h20"/></>,
    gear: <><circle cx="12" cy="12" r="3"/><path d="m19.4 15 .1.1 1.5 1.5-1.7 1.7-1.6-1.6a7 7 0 0 1-2 .8V20h-2.4v-2.3a7 7 0 0 1-2-.8l-1.6 1.6L8 16.8 9.6 15a7 7 0 0 1-.8-2H6.5v-2.4h2.3a7 7 0 0 1 .8-2L8 7l1.7-1.7 1.6 1.6a7 7 0 0 1 2-.8V3.8h2.4v2.3a7 7 0 0 1 2 .8l1.6-1.6L21 7l-1.6 1.6a7 7 0 0 1 .8 2h2.3V13h-2.3a7 7 0 0 1-.8 2Z"/></>,
    user: <><circle cx="12" cy="8" r="3.4"/><path d="M5 20a7 7 0 0 1 14 0"/></>,
    help: <><circle cx="12" cy="12" r="9"/><path d="M9.5 9a2.6 2.6 0 1 1 4.5 1.8c-1.2 1.1-2 1.6-2 3.2M12 17h.01"/></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/></>,
    store: <><path d="M3 10h18l-2-6H5zM5 10v10h14V10M9 20v-6h6v6"/></>,
    star: <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3l-5.6 2.9 1.1-6.2L3 9.6l6.2-.9L12 3Z"/>,
    check: <><path d="M20 7 10 17l-5-5"/><circle cx="12" cy="12" r="9"/></>,
    pause: <><circle cx="12" cy="12" r="9"/><path d="M10 8v8m4-8v8"/></>,
    eye: <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z"/><circle cx="12" cy="12" r="2.5"/></>,
    logout: <><path d="M10 17l5-5-5-5M15 12H3"/><path d="M12 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-6"/></>,
    chevron: <path d="m7 10 5 5 5-5"/>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name] || paths.box}</svg>
}

export { Icon as SellerIcon }

export default function SellerLayout({ children, storeName = '', searchValue = '', onSearchChange }) {
  const user = useSelector((state) => state.auth.user)
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const [profileOpen, setProfileOpen] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)

  async function signOut() {
    await dispatch(logout())
    navigate('/login', { replace: true })
  }

  return <div className="seller-shell">
    {mobileOpen && <button aria-label="Close seller navigation" className="seller-scrim" onClick={() => setMobileOpen(false)} type="button"/>}
    <aside className={`seller-sidebar${mobileOpen ? ' is-open' : ''}`}>
      <Link className="seller-brand" to="/seller"><span className="seller-brand-mark"><Icon name="store"/></span><span>Shop<span>Sphere</span></span></Link>
      <span className="seller-sidebar-label">SELLER CENTER</span>
      <nav className="seller-navigation" aria-label="Seller navigation">
        {navigation.map(([label, to, icon, available]) => available
          ? <NavLink end={label === 'Dashboard'} className={({ isActive }) => `seller-nav-link${isActive ? ' active' : ''}`} key={label} onClick={() => setMobileOpen(false)} to={to}><Icon name={icon}/><span>{label}</span></NavLink>
          : <span aria-disabled="true" className="seller-nav-link is-disabled" key={label} title="This seller section is not available yet"><Icon name={icon}/><span>{label}</span><small>Soon</small></span>)}
      </nav>
      <section className="seller-growth-card"><strong>Grow your business</strong><span>with ShopSphere</span><Link to="/seller/products/new">Add a product <Icon name="plus" size={15}/></Link></section>
      <button className="seller-logout-link" onClick={signOut} type="button"><Icon name="logout"/><span>Logout</span></button>
    </aside>
    <section className="seller-workspace">
      <header className="seller-topbar">
        <button aria-label="Open navigation" className="seller-mobile-menu" onClick={() => setMobileOpen(true)} type="button">☰</button>
        <label className="seller-global-search"><Icon name="search"/><input aria-label="Search dashboard data" onChange={(event) => onSearchChange?.(event.target.value)} placeholder="Search products, orders..." value={searchValue}/><button aria-label="Search" type="button"><Icon name="search"/></button></label>
        <button aria-label="Notifications" className="seller-top-icon" title="Seller notifications"><Icon name="bell"/></button>
        <span className="seller-top-divider"/>
        <div className="seller-store-selector"><Icon name="store"/><span>{storeName || 'My Store'}</span><Icon name="chevron" size={15}/></div>
        <div className="seller-profile-menu">
          <button aria-expanded={profileOpen} className="seller-profile-button" onClick={() => setProfileOpen((open) => !open)} type="button"><span className="seller-avatar">{(user?.fullName || 'Seller').split(/\s+/).map((part) => part[0]).slice(0, 2).join('').toUpperCase()}</span><span className="seller-profile-copy"><strong>{user?.fullName || 'Seller'}</strong><small>Seller</small></span><Icon name="chevron" size={15}/></button>
          {profileOpen && <div className="seller-profile-dropdown"><strong>{user?.email}</strong><button onClick={signOut} type="button"><Icon name="logout" size={17}/> Logout</button></div>}
        </div>
      </header>
      {children}
    </section>
  </div>
}
