import { useEffect, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import { logout } from "../../features/auth/authSlice";

const Icon = ({ name, size = 20 }) => {
  const paths = {
    menu: (
      <>
        <path d="M4 6h16M4 12h16M4 18h16" />
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
        <path d="m3 10 2-6h14l2 6" />
        <path d="M4 10v10h16V10M9 20v-6h6v6" />
        <path d="M3 10h18" />
      </>
    ),
    home: (
      <>
        <path d="m3 10 9-7 9 7" />
        <path d="M5 9v11h14V9M9 20v-7h6v7" />
      </>
    ),
    search: (
      <>
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-4-4" />
      </>
    ),
    help: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M9.6 9a2.5 2.5 0 1 1 4.3 1.8c-1.2 1.1-1.9 1.5-1.9 3" />
        <path d="M12 17h.01" />
      </>
    ),
    bell: (
      <>
        <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
        <path d="M10 21h4" />
      </>
    ),
    chevron: <path d="m7 10 5 5 5-5" />,
    calendar: (
      <>
        <rect x="3" y="5" width="18" height="16" rx="2" />
        <path d="M16 3v4M8 3v4M3 11h18M8 15h3" />
      </>
    ),
    download: (
      <>
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
        <path d="m7 10 5 5 5-5M12 15V3" />
      </>
    ),
    logout: <><path d="M10 17l5-5-5-5M15 12H3"/><path d="M12 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-6"/></>,
  };
  return (
    <svg
      aria-hidden="true"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.9"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {paths[name]}
    </svg>
  );
};

export default function DashboardHeader({
  user,
  onMenu,
  onExport,
  exporting,
  period,
  onPeriodChange,
  pageTitle = "Dashboard Overview",
  pageSubtitle,
  breadcrumb = "Overview",
  showDateFilter = true,
  exportLabel = "Export Report",
  titleIcon,
}) {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [profileOpen, setProfileOpen] = useState(false);
  const profileRef = useRef(null);

  useEffect(() => {
    if (!profileOpen) return undefined;
    function closeOnOutsideClick(event) {
      if (!profileRef.current?.contains(event.target)) setProfileOpen(false);
    }
    function closeOnEscape(event) {
      if (event.key === "Escape") setProfileOpen(false);
    }
    document.addEventListener("pointerdown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("pointerdown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [profileOpen]);

  async function handleLogout() {
    setProfileOpen(false);
    await dispatch(logout());
    navigate("/login", { replace: true });
  }

  return (
    <>
      <header className="admin-topbar">
        {/* <button
          type="button"
          className="admin-mobile-menu"
          onClick={onMenu}
          aria-label="Open navigation"
        >
          <Icon name="menu" />
        </button> */}
        <div className="admin-breadcrumb">
          <span className="admin-home-icon">
            <Icon name="home" size={19} />
          </span>
          <strong>Dashboard</strong>
          <span>/</span>
          <span>{breadcrumb}</span>
        </div>
        <label className="admin-global-search">
          <Icon name="search" size={20} />
          <input
            type="search"
            placeholder="Search anything..."
            aria-label="Search dashboard"
          />
        </label>
        <div className="admin-top-actions">
          <button
            type="button"
            className="admin-icon-button"
            title="Help"
            aria-label="Help"
          >
            <Icon name="help" />
          </button>
          <button
            type="button"
            className="admin-icon-button admin-bell"
            title="Notifications"
            aria-label="Notifications, 3 unread"
          >
            <Icon name="bell" />
            <i>3</i>
          </button>
          <div className="admin-profile-menu" ref={profileRef}>
            <button
              type="button"
              className="admin-top-profile"
              onClick={() => setProfileOpen(!profileOpen)}
              aria-expanded={profileOpen}
              aria-haspopup="menu"
            >
              <span className="admin-avatar">
                {(user?.fullName || user?.name || "Admin")
                  .split(/\s+/)
                  .map((part) => part[0])
                  .join("")
                  .slice(0, 2)
                  .toUpperCase()}
              </span>
              <span>
                <strong>
                  {user?.fullName || user?.name || "Administrator"}
                </strong>
                <small>Super Admin</small>
              </span>
              <b>
                <Icon name="chevron" size={16} />
              </b>
            </button>
            {profileOpen && (
              <div className="admin-profile-dropdown" role="menu">
                <strong>{user?.email || "Administrator account"}</strong>
                <button className="admin-profile-logout" onClick={handleLogout} role="menuitem" type="button">
                  <Icon name="logout" size={17} />
                  Logout
                </button>
              </div>
            )}
          </div>
        </div>
      </header>
      <div className="admin-page-title-row">
        <div className={titleIcon ? "admin-title-with-icon" : ""}>
          {titleIcon && (
            <span className="admin-title-icon">
              <Icon name={titleIcon} size={30} />
            </span>
          )}
          <div>
            <h1>{pageTitle}</h1>
            <p>
              {pageSubtitle ??
                `Welcome back${user?.fullName || user?.name ? `, ${(user.fullName || user.name).split(" ")[0]}` : ""}! Here's what's happening with your marketplace today.`}
            </p>
          </div>
        </div>
        {(showDateFilter || onExport) && (
          <div className="admin-title-actions">
            {showDateFilter && (
              <label className="admin-date-select">
                <Icon name="calendar" size={18} />
                <select
                  value={period}
                  onChange={(event) => onPeriodChange(event.target.value)}
                  aria-label="Dashboard date range"
                >
                  <option value="THIRTY_DAYS">Last 30 days</option>
                  <option value="SIX_MONTHS">Last 6 months</option>
                  <option value="ONE_YEAR">Last 1 year</option>
                  <option value="ALL_TIME">All time</option>
                </select>
                <Icon name="chevron" size={15} />
              </label>
            )}
            {onExport && (
              <button
                className="admin-export-button"
                onClick={onExport}
                type="button"
                disabled={exporting}
              >
                <Icon name="download" size={17} />
                {exporting ? "Preparing..." : exportLabel}
              </button>
            )}
          </div>
        )}
      </div>
    </>
  );
}
