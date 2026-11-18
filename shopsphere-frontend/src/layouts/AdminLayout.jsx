import { useState } from 'react'
import { useSelector } from 'react-redux'
import Sidebar from '../components/dashboard/Sidebar'
import DashboardHeader from '../components/dashboard/DashboardHeader'
import '../styles/admin-dashboard.css'

export default function AdminLayout({ children, onExport, exporting, period, onPeriodChange, pageTitle, pageSubtitle, breadcrumb, showDateFilter, exportLabel, titleIcon }) {
  const user = useSelector((state)=>state.auth.user)
  const [collapsed,setCollapsed] = useState(false)
  const [mobileOpen,setMobileOpen] = useState(false)
  return <div className={`admin-shell ${collapsed?'sidebar-collapsed':''}`}><Sidebar user={user} collapsed={collapsed} mobileOpen={mobileOpen} onToggle={()=>setCollapsed(!collapsed)} onClose={()=>setMobileOpen(false)}/><main className="admin-main"><DashboardHeader user={user} onMenu={()=>setMobileOpen(true)} onExport={onExport} exporting={exporting} period={period} onPeriodChange={onPeriodChange} pageTitle={pageTitle} pageSubtitle={pageSubtitle} breadcrumb={breadcrumb} showDateFilter={showDateFilter} exportLabel={exportLabel} titleIcon={titleIcon}/>{children}</main></div>
}
