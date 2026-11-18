import { Link, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { logout } from '../../features/auth/authSlice'

const ITEMS = [
  { label: 'My Orders', path: '/orders', icon: 'orders' },
  { label: 'My Wishlist', path: '/wishlist', icon: 'heart' },
  { label: 'My Addresses', path: '/addresses', icon: 'pin' },
  { label: 'My Wallet', path: '/account', icon: 'wallet' },
  { label: 'My Reviews', path: '/account', icon: 'star' },
  { label: 'Returns & Refunds', path: '/returns', icon: 'return' },
  { label: 'Account Settings', path: '/account', icon: 'settings' },
  { label: 'Help & Support', path: 'mailto:support@shopsphere.com', icon: 'support' },
]

export function DashboardIcon({ name, className }) {
  const drawings = {
    orders: <><rect x="5" y="7" width="14" height="14" rx="2"/><path d="M9 7V5a3 3 0 0 1 6 0v2M9 12h6M9 16h4"/></>,
    heart: <path d="M20.8 8.7c0 4.1-8.8 10-8.8 10S3.2 12.8 3.2 8.7A4.3 4.3 0 0 1 11 5.9l1 1 1-1a4.3 4.3 0 0 1 7.8 2.8Z"/>,
    pin: <><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/></>,
    wallet: <><rect x="3" y="6" width="18" height="14" rx="2"/><path d="M3 10h18M7 6V4h12v2M16 15h2"/></>,
    star: <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9L12 3Z"/>,
    return: <><path d="M4 7v5h5M5 12a8 8 0 1 1 2.3 5.7"/><path d="m4 12 4-4"/></>,
    settings: <><circle cx="12" cy="12" r="3"/><path d="M19.4 15a8 8 0 0 0 0-6l1.4-1.1-1.5-2.6-1.7.7a8 8 0 0 0-5.2-1.4L12 3h-3l-.4 1.8a8 8 0 0 0-2.4 1.4l-1.7-.7L3 8.1 4.4 9.2a8 8 0 0 0 0 5L3 15.3l1.5 2.6 1.7-.7a8 8 0 0 0 5.2 1.4L12 20h3l.4-1.8a8 8 0 0 0 2.4-1.4l1.7.7 1.5-2.6L19.4 15Z"/></>,
    support: <><path d="M4 13v-2a8 8 0 0 1 16 0v2"/><rect x="3" y="12" width="4" height="7" rx="2"/><rect x="17" y="12" width="4" height="7" rx="2"/><path d="M17 19a5 5 0 0 1-5 3h-2"/></>,
    gift: <><path d="M3 10h18v11H3zM2 6h20v4H2zM12 6v15"/><path d="M12 6H7.5a2.5 2.5 0 1 1 2.4-3.2L12 6Zm0 0h4.5a2.5 2.5 0 1 0-2.4-3.2L12 6Z"/></>,
    cart: <><path d="M3 4h2l2.2 11.2a2 2 0 0 0 2 1.6h8.4a2 2 0 0 0 1.9-1.4L21 9H6"/><circle cx="10" cy="20" r="1"/><circle cx="18" cy="20" r="1"/></>,
    check: <path d="m5 12 4 4L19 6"/>,
    close: <path d="m6 6 12 12M18 6 6 18"/>,
    truck: <><path d="M3 6h11v11H3zM14 10h4l3 3v4h-7z"/><circle cx="7.5" cy="19" r="1.5"/><circle cx="17.5" cy="19" r="1.5"/></>,
    copy: <><rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V5a1 1 0 0 0-1-1H5a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h3"/></>,
    arrow: <><path d="M5 12h14M13 6l6 6-6 6"/></>,
    share: <><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.7 10.6 6.6-4.2M8.7 13.4l6.6 4.2"/></>,
    trash: <><path d="M4 7h16M10 11v6M14 11v6M6 7l1 14h10l1-14M9 7V4h6v3"/></>,
    thumbs: <><path d="M7 10v11H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3Zm0 0 5-8c2 0 3 1 3 3l-1 5h5a3 3 0 0 1 3 4l-2 6a3 3 0 0 1-3 2H7"/></>,
    home: <><path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-6v-7h-4v7H4a1 1 0 0 1-1-1V10Z"/></>,
    work: <><rect x="3" y="6" width="18" height="15" rx="2"/><path d="M8 6V4h8v2M3 12h18m-12 0v2m6-2v2"/></>,
    phone: <><path d="M7 3H5a2 2 0 0 0-2 2c0 8.8 7.2 16 16 16a2 2 0 0 0 2-2v-2l-5-2-2 3a14 14 0 0 1-8-8l3-2-2-5Z"/></>,
    note: <><rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 8h6M9 12h6M9 16h4"/></>,
    edit: <><path d="m4 16-.8 4.8L8 20l11-11-4-4L4 16Z"/><path d="m13.5 6.5 4 4"/></>,
    info: <><circle cx="12" cy="12" r="10"/><path d="M12 11v5m0-8h.01"/></>,
    plus: <path d="M12 5v14M5 12h14"/>,
    search: <><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></>,
    logout: <><path d="M10 17l5-5-5-5M15 12H3"/><path d="M12 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-6"/></>,
    repeat: <><path d="m17 2 4 4-4 4"/><path d="M3 11V9a3 3 0 0 1 3-3h15M7 22l-4-4 4-4"/><path d="M21 13v2a3 3 0 0 1-3 3H3"/></>,
    grid: <><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>,
    tag: <><path d="M20 13 13 20 3 10V4h6l11 9Z"/><circle cx="7.5" cy="7.5" r="1"/></>,
    star: <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9L12 3Z"/>,
  }
  return <svg className={className} aria-hidden="true" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" viewBox="0 0 24 24">{drawings[name]}</svg>
}

export default function CustomerAccountSidebar({ active = 'My Orders' }) {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const user = useSelector((state) => state.auth.user)

  async function handleLogout() {
    await dispatch(logout())
    navigate('/login', { replace: true })
  }

  return <aside className="customer-sidebar">
    <div className="customer-profile">
      <div className="customer-avatar" aria-hidden="true">{user?.fullName?.split(' ').map((part) => part[0]).slice(0, 2).join('') || '👤'}</div>
      <strong>{user?.fullName || 'ShopSphere Customer'}</strong>
      <span>{user?.email || ''}</span>
    </div>
    <nav aria-label="My account" className="customer-side-links">
      {ITEMS.map((item) => <Link key={item.label} className={item.label === active ? 'is-active' : ''} to={item.path}>
        <span><DashboardIcon name={item.icon} /></span>{item.label}
      </Link>)}
    </nav>
    <button className="customer-sidebar-logout" onClick={handleLogout} type="button"><DashboardIcon name="logout" />Sign Out</button>
    <section className="customer-coupon">
      <span className="coupon-gift"><DashboardIcon name="gift" /></span>
      <strong>Get 10% OFF</strong><p>on your next order</p><b>WELCOME10</b>
      <Link to="/shop">Shop Now <DashboardIcon name="arrow" /></Link>
    </section>
  </aside>
}
