import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import AdminLayout from '../../layouts/AdminLayout'
import AdminOrderPanel from '../../components/dashboard/AdminOrderPanel'
import OrderCancelDialog from '../../components/dashboard/OrderCancelDialog'
import { getRequestError } from '../../services/authService'
import { cancelAdminOrder, getAdminOrder } from '../../services/orderService'
import './AdminOrders.css'

export default function AdminOrderDetails() {
  const { orderId } = useParams()
  const navigate = useNavigate()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [cancelOpen, setCancelOpen] = useState(false)
  const [cancelError, setCancelError] = useState('')
  const [busy, setBusy] = useState(false)
  const [toast, setToast] = useState('')

  const loadOrder = useCallback(async () => {
    setLoading(true); setError('')
    try { setOrder(await getAdminOrder(orderId)) }
    catch (requestError) { setError(getRequestError(requestError, 'Could not load order details.')) }
    finally { setLoading(false) }
  }, [orderId])

  useEffect(() => { loadOrder() }, [loadOrder])

  async function confirmCancellation(reason) {
    setBusy(true); setCancelError('')
    try {
      const result = await cancelAdminOrder(order.id, reason)
      setOrder(await getAdminOrder(order.id)); setCancelOpen(false)
      setToast(result.message || 'Order cancelled.')
    } catch (requestError) { setCancelError(getRequestError(requestError, 'The order could not be cancelled.')) }
    finally { setBusy(false) }
  }

  return <AdminLayout breadcrumb="Orders / Details" pageTitle="Order Details" pageSubtitle="Review customer, payment and seller fulfillment records." showDateFilter={false}>
    <main className="admin-order-details-page">
      <div className="aod-toolbar"><button className="ao-button ao-button--outline" onClick={() => navigate('/admin/orders')} type="button">← All Orders</button>{order && <span>Order #{String(order.id).padStart(6, '0')}</span>}</div>
      {loading && <div className="aod-state"><span className="ao-loader"/><strong>Loading order details…</strong></div>}
      {!loading && error && <div className="ao-alert" role="alert">{error}<button onClick={loadOrder} type="button">Retry</button></div>}
      {!loading && order && <AdminOrderPanel onCancel={order.cancellationEligible ? () => { setCancelOpen(true); setCancelError('') } : undefined} onClose={() => navigate('/admin/orders')} onPrintBlocked={() => setToast('Allow pop-ups in your browser to print the receipt.')} order={order} variant="full"/>}
    </main>
    {cancelOpen && order && <OrderCancelDialog busy={busy} error={cancelError} onClose={() => { if (!busy) setCancelOpen(false) }} onConfirm={confirmCancellation} order={order}/>}
    {toast && <div className="ao-toast" role="status">{toast}<button aria-label="Dismiss notification" onClick={() => setToast('')} type="button">×</button></div>}
  </AdminLayout>
}
