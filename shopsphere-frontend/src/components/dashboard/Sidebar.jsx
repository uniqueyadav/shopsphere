import { NavLink } from "react-router-dom";

const items = [
  ["Dashboard", "/admin", "home"],
  ["Users", "/admin/users", "users"],
  ["Sellers", "/admin/sellers", "store"],
  ["Seller Applications", "/admin/seller-applications", "clipboard"],
  ["Categories", "/admin/categories", "grid"],
  ["Products", "/admin/products", "box"],
  ["All Orders", "/admin/orders", "cart"],
  ["Payments", "/admin/payments", "card"],
  ["Refunds", "/admin/refunds", "refresh"],
  ["Coupons", "/admin/coupons", "tag"],
  ["Reviews", "/admin/reviews", "star"],
  ["Delivery Partners", "/admin/delivery-partners", "truck"],
  ["Courier Dashboard", "/admin/courier-dashboard", "chart"],
  ["Commission Settings", "/admin/commission-settings", "percent"],
  ["Revenue Reports", "/admin/revenue-reports", "chart"],
  ["Notifications", "/admin/notifications", "bell"],
  ["Settings", "/admin/settings", "settings"],
];

const paths = {
  home: (
    <>
      <path d="m3 10 9-7 9 7" />
      <path d="M5 9v11h14V9M9 20v-7h6v7" />
    </>
  ),
  users: (
    <>
      <path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="10" cy="7" r="4" />
      <path d="M20 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8" />
    </>
  ),
  store: (
    <>
      <path d="m3 9 2-6h14l2 6" />
      <path d="M4 9v11h16V9M9 20v-6h6v6" />
    </>
  ),
  clipboard: (
    <>
      <rect x="5" y="4" width="14" height="17" rx="2" />
      <path d="M9 4.5h6v3H9zM9 12h6M9 16h6" />
    </>
  ),
  grid: (
    <>
      <rect x="3" y="3" width="8" height="8" rx="1" />
      <rect x="13" y="3" width="8" height="8" rx="1" />
      <rect x="3" y="13" width="8" height="8" rx="1" />
      <rect x="13" y="13" width="8" height="8" rx="1" />
    </>
  ),
  box: (
    <>
      <path d="m12 3 9 5-9 5-9-5 9-5Z" />
      <path d="M3 8v9l9 5 9-5V8M12 13v9" />
    </>
  ),
  cart: (
    <>
      <path d="M3 3h2l2.4 12.4a2 2 0 0 0 2 1.6h8.9a2 2 0 0 0 2-1.6L22 8H6" />
      <circle cx="10" cy="21" r="1" />
      <circle cx="18" cy="21" r="1" />
    </>
  ),
  card: (
    <>
      <rect x="2" y="5" width="20" height="14" rx="2" />
      <path d="M2 10h20M7 15h3" />
    </>
  ),
  refresh: (
    <>
      <path d="M20 7v5h-5M4 17v-5h5" />
      <path d="M5.6 9a7 7 0 0 1 11.7-2L20 12M4 12l2.7 5a7 7 0 0 0 11.7-2" />
    </>
  ),
  tag: (
    <>
      <path d="M20.6 13.4 13 21l-10-10V3h8l9.6 9.6a.6.6 0 0 1 0 .8Z" />
      <circle cx="7.5" cy="7.5" r="1" />
    </>
  ),
  star: (
    <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3l-5.6 2.9 1.1-6.2L3 9.6l6.2-.9L12 3Z" />
  ),
  truck: (
    <>
      <path d="M3 6h11v11H3zM14 10h4l3 3v4h-7z" />
      <circle cx="7.5" cy="19" r="1.5" />
      <circle cx="18" cy="19" r="1.5" />
    </>
  ),
  percent: (
    <>
      <circle cx="7" cy="7" r="2.5" />
      <circle cx="17" cy="17" r="2.5" />
      <path d="m19 5-14 14" />
    </>
  ),
  chart: (
    <>
      <path d="M4 20V10M10 20V4M16 20v-7M22 20V7" />
    </>
  ),
  bell: (
    <>
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
    </>
  ),
  settings: (
    <>
      <circle cx="12" cy="12" r="3" />
      <path d="m19.4 15 .1.1 1.4 1.1-1.4 2.4-1.7-.6a8 8 0 0 1-1.7 1l-.3 1.8h-2.8l-.3-1.8a8 8 0 0 1-1.7-1l-1.7.6-1.4-2.4L7.3 15a8 8 0 0 1 0-2l-1.4-1.1 1.4-2.4 1.7.6a8 8 0 0 1 1.7-1l.3-1.8h2.8l.3 1.8a8 8 0 0 1 1.7 1l1.7-.6 1.4 2.4L19.3 13a8 8 0 0 1 .1 2Z" />
    </>
  ),
};

export default function Sidebar({
  user,
  collapsed,
  mobileOpen,
  onToggle,
  onClose,
}) {
  return (
    <>
      <button
        type="button"
        className={`admin-mobile-scrim ${mobileOpen ? "is-open" : ""}`}
        onClick={onClose}
        aria-label="Close navigation"
      />
      <aside
        className={`admin-sidebar ${collapsed ? "is-collapsed" : ""} ${mobileOpen ? "mobile-open" : ""}`}
      >
        <div className="admin-brand-row">
          <span className="admin-brand-mark">◆</span>
          {!collapsed && (
            <span className="admin-brand">
              Shop<span>Sphere</span>
            </span>
          )}
          <button
            className="admin-sidebar-toggle"
            onClick={onToggle}
            type="button"
            aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          >
            {collapsed ? "›" : "‹"}
          </button>
        </div>
        {!collapsed && <div className="admin-sidebar-label">ADMIN PANEL</div>}
        <nav className="admin-sidebar-nav" aria-label="Admin navigation">
          {items.map(([label, to, icon, badge], index) => (
            <NavLink
              key={label}
              to={to}
              end={index === 0}
              onClick={onClose}
              title={collapsed ? label : undefined}
              className={({ isActive }) =>
                `admin-nav-link ${isActive ? "active" : ""}`
              }
            >
              <span className="admin-nav-icon">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  {paths[icon]}
                </svg>
              </span>
              {!collapsed && (
                <>
                  <span className="admin-nav-label">{label}</span>
                  {badge && <span className="admin-nav-badge">{badge}</span>}
                </>
              )}
            </NavLink>
          ))}
        </nav>
        <div className="admin-sidebar-profile">
          <span className="admin-avatar admin-avatar-large">
            {(user?.fullName || user?.name || "Admin")
              .split(/\s+/)
              .map((part) => part[0])
              .join("")
              .slice(0, 2)
              .toUpperCase()}
          </span>
          {!collapsed && (
            <>
              <span className="admin-profile-text">
                <strong>
                  {user?.fullName || user?.name || "Administrator"}
                </strong>
                <small>Super Admin</small>
              </span>
              <span className="admin-profile-arrow">›</span>
            </>
          )}
        </div>
      </aside>
    </>
  );
}
