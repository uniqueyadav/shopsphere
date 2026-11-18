import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import Dashboard from '../pages/admin/Dashboard'

const Users = lazy(() => import('../pages/admin/Users'))
const Sellers = lazy(() => import('../pages/admin/Sellers'))
const Products = lazy(() => import('../pages/admin/Products'))
const AllOrders = lazy(() => import('../pages/admin/AllOrders'))
const AdminOrderDetails = lazy(() => import('../pages/admin/AdminOrderDetails'))
const Payments = lazy(() => import('../pages/admin/Payments'))
const Refunds = lazy(() => import('../pages/admin/Refunds'))
const Coupons = lazy(() => import('../pages/admin/Coupons'))
const Reviews = lazy(() => import('../pages/admin/Reviews'))
const DeliveryPartners = lazy(() => import('../pages/admin/DeliveryPartners'))
const CommissionSettings = lazy(() => import('../pages/admin/CommissionSettings'))
const RevenueReports = lazy(() => import('../pages/admin/RevenueReports'))
const Notifications = lazy(() => import('../pages/admin/Notifications'))
const Settings = lazy(() => import('../pages/admin/Settings'))

function AdminPageFallback() {
  return <main className="session-state" role="status"><span className="spinner"/><p>Loading admin page…</p></main>
}

export default function AdminRoutes() {
  return <Routes>
    <Route index element={<Dashboard/>}/>
    <Route path="dashboard" element={<Dashboard/>}/>
    <Route path="users" element={<Suspense fallback={<AdminPageFallback/>}><Users/></Suspense>}/>
    <Route path="sellers" element={<Suspense fallback={<AdminPageFallback/>}><Sellers/></Suspense>}/>
    <Route path="products" element={<Suspense fallback={<AdminPageFallback/>}><Products/></Suspense>}/>
    <Route path="orders" element={<Suspense fallback={<AdminPageFallback/>}><AllOrders/></Suspense>}/>
    <Route path="orders/:orderId" element={<Suspense fallback={<AdminPageFallback/>}><AdminOrderDetails/></Suspense>}/>
    <Route path="payments" element={<Suspense fallback={<AdminPageFallback/>}><Payments/></Suspense>}/>
    <Route path="refunds" element={<Suspense fallback={<AdminPageFallback/>}><Refunds/></Suspense>}/>
    <Route path="coupons" element={<Suspense fallback={<AdminPageFallback/>}><Coupons/></Suspense>}/>
    <Route path="reviews" element={<Suspense fallback={<AdminPageFallback/>}><Reviews/></Suspense>}/>
    <Route path="delivery-partners" element={<Suspense fallback={<AdminPageFallback/>}><DeliveryPartners/></Suspense>}/>
    <Route path="commission-settings" element={<Suspense fallback={<AdminPageFallback/>}><CommissionSettings/></Suspense>}/>
    <Route path="revenue-reports" element={<Suspense fallback={<AdminPageFallback/>}><RevenueReports/></Suspense>}/>
    <Route path="notifications" element={<Suspense fallback={<AdminPageFallback/>}><Notifications/></Suspense>}/>
    <Route path="settings" element={<Suspense fallback={<AdminPageFallback/>}><Settings/></Suspense>}/>
    <Route path="*" element={<Navigate to="/admin" replace/>}/>
  </Routes>
}
