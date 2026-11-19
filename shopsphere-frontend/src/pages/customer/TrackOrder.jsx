import { useEffect, useState } from 'react'
import { LazyMotion, MotionConfig, domAnimation, m } from 'framer-motion'
import { Link, useParams } from 'react-router-dom'
import MainNavbar from '../../components/navbar/MainNavbar'
import { getRequestError } from '../../services/authService'
import { getOrderTracking } from '../../services/orderService'
import { getOrderStatusLabel } from './orderUtils'
import './OrderHistory.css'

function TrackingSkeleton() {
  return (
    <section aria-label="Loading tracking details" className="tracking-skeleton" role="status">
      <span /><span /><span /><span />
      <span className="orders-visually-hidden">Loading tracking details…</span>
    </section>
  )
}

export default function TrackOrder() {
  const { orderId } = useParams()
  const [tracking, setTracking] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    getOrderTracking(orderId)
      .then((result) => {
        if (active) {
          setTracking(result)
          setStatus('succeeded')
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(getRequestError(requestError, 'Tracking details could not be loaded.'))
          setStatus('failed')
        }
      })
    return () => {
      active = false
    }
  }, [orderId])

  return (
    <LazyMotion features={domAnimation}>
      <MotionConfig reducedMotion="user">
        <m.div
          animate={{ opacity: 1, y: 0 }}
          className="order-history-page"
          initial={{ opacity: 0, y: 8 }}
          transition={{ duration: .3 }}
        >
          <MainNavbar />
          <main className="orders-container tracking-container">
            <nav aria-label="Breadcrumb" className="orders-breadcrumb">
              <Link to="/shop">Home</Link><span>/</span><Link to="/orders">My orders</Link><span>/</span><span aria-current="page">Track order</span>
            </nav>
            {status === 'loading' ? <TrackingSkeleton /> : status === 'failed' ? (
              <section className="orders-state orders-state--error">
                <h1>Tracking is unavailable.</h1>
                <p role="alert">{error}</p>
                <Link className="orders-primary-button" to="/orders">Back to my orders</Link>
              </section>
            ) : tracking ? (
              <>
                <header className="orders-heading tracking-heading">
                  <div>
                    <span className="orders-eyebrow">EVERY STEP, CLEARLY</span>
                    <h1>Order tracking</h1>
                    <p>Order #{tracking.orderId} · placed {new Date(tracking.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' })}</p>
                  </div>
                  <Link className="orders-secondary-button" to={`/orders/${tracking.orderId}`}>Order details</Link>
                </header>
                <div className="tracking-order-status">
                  <span>Current order status</span>
                  <strong>{getOrderStatusLabel(tracking.status)}</strong>
                  <span>Payment · {getOrderStatusLabel(tracking.paymentStatus)}</span>
                  {tracking.refundStatus && tracking.refundStatus !== 'NOT_REQUESTED' && (
                    <span>Refund · {getOrderStatusLabel(tracking.refundStatus)}</span>
                  )}
                </div>
                <div className="tracking-sellers">
                  {tracking.sellerOrders.map((sellerOrder, sellerIndex) => (
                    <m.section
                      animate={{ opacity: 1, y: 0 }}
                      className="tracking-seller-card"
                      initial={{ opacity: 0, y: 12 }}
                      key={sellerOrder.sellerOrderId}
                      transition={{ delay: sellerIndex * .06, duration: .25 }}
                    >
                      <header>
                        <div>
                          <span className="orders-card-label">SELLER SHIPMENT</span>
                          <h2>{sellerOrder.sellerName}</h2>
                        </div>
                        <span className={`orders-status orders-status--${sellerOrder.status?.toLowerCase().replaceAll('_', '-')}`}>
                          {getOrderStatusLabel(sellerOrder.status)}
                        </span>
                      </header>
                      {sellerOrder.history.length ? (
                        <ol className="tracking-timeline">
                          {sellerOrder.history.map((event, eventIndex) => {
                            const isCurrent = eventIndex === sellerOrder.history.length - 1
                            return (
                              <li className={`tracking-event${isCurrent ? ' tracking-event--current' : ''}`} key={`${event.status}-${event.changedAt}-${eventIndex}`}>
                                <span aria-hidden="true" className="tracking-event-marker" />
                                <div>
                                  <strong>{getOrderStatusLabel(event.status)}</strong>
                                  <time dateTime={event.changedAt}>
                                    {new Date(event.changedAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}
                                  </time>
                                  {event.latitude != null && event.longitude != null
                                    ? <p>Location: <a href={`https://www.google.com/maps?q=${event.latitude},${event.longitude}`} rel="noreferrer" target="_blank">{Number(event.latitude).toFixed(5)}, {Number(event.longitude).toFixed(5)}</a></p>
                                    : <p>Location not shared for this update.</p>}
                                </div>
                              </li>
                            )
                          })}
                        </ol>
                      ) : (
                        <p className="tracking-empty-history">No shipment updates are available yet.</p>
                      )}
                    </m.section>
                  ))}
                </div>
              </>
            ) : null}
          </main>
        </m.div>
      </MotionConfig>
    </LazyMotion>
  )
}
