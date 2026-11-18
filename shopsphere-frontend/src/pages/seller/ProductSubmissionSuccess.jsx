import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import SellerLayout from '../../layouts/SellerLayout'
import { getRequestError } from '../../services/authService'
import { getSellerProduct } from '../../services/productService'
import './ProductSubmissionSuccess.css'

const statusLabels = {
  DRAFT: 'Draft',
  PENDING: 'Under Review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
}

function Icon({ name, size = 22 }) {
  const paths = {
    check: <path d="m5 12 4 4L19 6"/>,
    calendar: <><rect x="3" y="5" width="18" height="16"/><path d="M16 3v4M8 3v4M3 10h18M8 14h3m-3 4h6"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    mail: <><rect x="3" y="5" width="18" height="14"/><path d="m4 7 8 6 8-6"/></>,
    store: <><path d="M3 10h18l-2-6H5zM5 10v10h14V10M9 20v-6h6v6"/></>,
    home: <><path d="m3 10 9-7 9 7M5 9v11h14V9M9 20v-7h6v7"/></>,
    plus: <><path d="M12 5v14M5 12h14"/></>,
    refresh: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.6 9a7 7 0 0 1 11.7-2L20 12M4 12l2.7 5a7 7 0 0 0 11.7-2"/></>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

function SuccessIllustration() {
  return <svg className="pss-illustration" viewBox="0 0 420 240" role="img" aria-label="Product submission succeeded">
    <path d="M32 209c44-24 74-21 111 0 32 18 84 18 119 0 42-21 78-23 126 0" fill="none" stroke="#b8d4ff" strokeWidth="3"/>
    <path d="m95 197 30-65 29 65m112 0 28-65 33 65" fill="#b5d1ff" stroke="#8db8ff" strokeWidth="2"/>
    <path d="M148 106h124l30 24v67H117v-67z" fill="#ffb617" stroke="#ee9a00" strokeWidth="3"/>
    <path d="M117 130h185v67H117z" fill="#ffc43d"/>
    <path d="M176 130v67m68-67v67" stroke="#ef9a00" strokeWidth="3"/>
    <path d="M179 109v17m32-17v17m32-17v17" stroke="#da8400" strokeWidth="8"/>
    <path d="M195 163h30v34h-30z" fill="#fff4d8"/>
    <path d="M204 176h12v21h-12z" fill="#075bea"/>
    <circle cx="210" cy="55" r="48" fill="#20c768"/>
    <path d="m187 55 15 15 32-34" fill="none" stroke="#fff" strokeWidth="10" strokeLinecap="square" strokeLinejoin="round"/>
    <path d="m73 68 10 11m253-17-12 12M116 34l-7-10m200 11 8-13M65 120l14-2m263 1-13-3" stroke="#ffb000" strokeWidth="6" strokeLinecap="square"/>
    <path d="m101 91 7 7m205-5-8 8M146 50l4 4m125 7 4-4" stroke="#1685ff" strokeWidth="5" strokeLinecap="square"/>
  </svg>
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

export default function ProductSubmissionSuccess() {
  const { productId } = useParams()
  const location = useLocation()
  const [product, setProduct] = useState(location.state?.product || null)
  const [loading, setLoading] = useState(!location.state?.product)
  const [error, setError] = useState('')

  async function loadProduct() {
    setLoading(true)
    setError('')
    try {
      const result = await getSellerProduct(productId)
      setProduct(result)
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not load this product submission.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    getSellerProduct(productId).then((result) => {
      if (active) setProduct(result)
    }).catch((requestError) => {
      if (active) setError(getRequestError(requestError, 'Could not load this product submission.'))
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [productId])

  const status = product?.status
  const statusLabel = statusLabels[status] || status?.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase()) || '—'
  const submittedAt = product?.updatedAt || product?.createdAt
  const isPending = status === 'PENDING'

  return <SellerLayout>
    <main className="pss-page">
      {loading && !product ? <section className="pss-loading" role="status"><span/><p>Loading submission details…</p></section> : !product ? <section className="pss-error"><h1>Submission details unavailable</h1><p>{error || 'This product submission could not be found for your seller account.'}</p><button onClick={loadProduct} type="button"><Icon name="refresh"/>Retry</button><Link to="/seller/products">Back to Products</Link></section> : <>
        {error && <div className="pss-refresh-error" role="status">Showing the submitted product details. The latest status could not be refreshed.<button onClick={loadProduct} type="button"><Icon name="refresh" size={16}/>Retry</button></div>}
        <section className="pss-content">
          <SuccessIllustration/>
          <span className={`pss-result-icon ${isPending ? '' : 'pss-result-icon--neutral'}`}><Icon name={isPending ? 'check' : 'clock'} size={36}/></span>
          <p className="pss-eyebrow">PRODUCT SUBMISSION</p>
          <h1>{isPending ? <>Product Submitted <strong>Successfully!</strong></> : <>Product submission <strong>{statusLabel}</strong></>}</h1>
          <p className="pss-lead">Your product <b>{product.productName}</b> {isPending ? 'has been submitted for review. We will notify you when the review status changes.' : `currently has status: ${statusLabel}.`}</p>

          <section className="pss-details" aria-label="Submission details">
            <article><span className="pss-detail-icon"><Icon name="store"/></span><div><small>Product Name</small><strong>{product.productName}</strong></div></article>
            <article><span className="pss-detail-icon"><Icon name="calendar"/></span><div><small>Submitted On</small><strong>{formatDate(submittedAt)}</strong></div></article>
            <article><span className="pss-detail-icon"><Icon name="clock"/></span><div><small>Approval Status</small><strong className={`pss-status pss-status-${(status || 'unknown').toLowerCase()}`}>{statusLabel}</strong></div></article>
          </section>

          <section className="pss-next"><h2><span>✦</span>What happens next?</h2><div className="pss-steps">
            <article><span className="pss-next-icon"><Icon name="clock"/></span><div><strong>1. Admin Review</strong><p>Your product is waiting for review by the ShopSphere admin team.</p></div></article><i aria-hidden="true">→</i>
            <article><span className="pss-next-icon"><Icon name="mail"/></span><div><strong>2. Email Notification</strong><p>We will email you when the product review is complete.</p></div></article><i aria-hidden="true">→</i>
            <article><span className="pss-next-icon"><Icon name="store"/></span><div><strong>3. Product Goes Live</strong><p>Once approved, your product can appear in the ShopSphere marketplace.</p></div></article>
          </div></section>

          <footer className="pss-actions"><Link className="pss-dashboard-button" to="/seller/dashboard"><Icon name="home"/>Go to Dashboard</Link><Link className="pss-add-button" onClick={() => sessionStorage.removeItem('shopsphere.seller.addProduct.form.v1')} to="/seller/products/new"><Icon name="plus"/>Add Another Product</Link></footer>
          <p className="pss-publish-note">Products remain unpublished until approved by the admin team.</p>
        </section>
      </>}
    </main>
  </SellerLayout>
}
