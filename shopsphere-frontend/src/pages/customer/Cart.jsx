import { useEffect, useMemo, useRef, useState } from 'react'
import { AnimatePresence, LazyMotion, MotionConfig, domAnimation, m } from 'framer-motion'
import { Link } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import MainNavbar from '../../components/navbar/MainNavbar'
import ProductCard, { formatProductPrice } from '../../components/product/ProductCard'
import { DashboardIcon } from '../../components/customer/CustomerAccountSidebar'
import { clearCart, clearCartFeedback, fetchCart, removeCartItem, updateCartItem } from '../../features/cart/cartSlice'
import { getPublicProducts } from '../../services/productService'
import { getRequestError } from '../../services/authService'
import { previewCartCoupon } from '../../services/cartService'
import './Cart.css'
import './Shop.css'
import './CustomerCart.css'

function CartImage({ item }) {
  const [failed, setFailed] = useState(false)
  if (!item.imageUrl || failed) return <div className="customer-cart-image customer-cart-image--empty">SS</div>
  return <img className="customer-cart-image" src={item.imageUrl} alt={item.productName} loading="lazy" onError={() => setFailed(true)} />
}

function CartItemRow({ item, operation, onQuantityChange, onRemove }) {
  const busy = Boolean(operation)
  const unavailable = !item.available
  return <m.article className={`customer-cart-row${unavailable ? ' is-unavailable' : ''}`} layout initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, height: 0 }}>
    <Link className="customer-cart-product" to={`/products/${item.productId}`}>
      <CartImage item={item}/>
      <span className="customer-cart-product-copy"><strong>{item.productName}</strong><small>{[item.brand, item.sku].filter(Boolean).join(' • ') || item.sellerName}</small><em className={unavailable ? 'out' : ''}>{unavailable ? 'Unavailable' : 'In Stock'}</em></span>
    </Link>
    <strong className="customer-cart-unit-price">{formatProductPrice(item.unitPrice)}</strong>
    <div className="customer-cart-quantity" aria-label={`Quantity ${item.quantity}`}>
      <button type="button" aria-label={`Decrease ${item.productName}`} disabled={busy || unavailable || item.quantity <= 1} onClick={() => onQuantityChange(item, item.quantity - 1)}>−</button>
      <span>{item.quantity}</span>
      <button type="button" aria-label={`Increase ${item.productName}`} disabled={busy || unavailable || item.quantity >= item.stockQuantity} onClick={() => onQuantityChange(item, item.quantity + 1)}>+</button>
    </div>
    <strong className="customer-cart-line-total">{formatProductPrice(item.lineSubtotal)}</strong>
    <button className="customer-cart-remove" type="button" aria-label={`Remove ${item.productName}`} disabled={busy} onClick={() => onRemove(item.id)}><DashboardIcon name="trash"/></button>
  </m.article>
}

export default function ShoppingCart() {
  const dispatch = useDispatch()
  const { cart, status, operation, error, notice } = useSelector((state) => state.cart)
  const items = cart?.items || []
  const cartSignature = items.map((item) => `${item.id}:${item.quantity}`).join('|')
  const [couponInput, setCouponInput] = useState('')
  const [coupon, setCoupon] = useState(null)
  const [couponError, setCouponError] = useState('')
  const [couponBusy, setCouponBusy] = useState(false)
  const [recommendations, setRecommendations] = useState([])

  useEffect(() => { if (status === 'idle') dispatch(fetchCart()) }, [dispatch, status])
  useEffect(() => {
    let mounted = true
    getPublicProducts({ page: 0, size: 12, sort: 'popular', availability: 'IN_STOCK' })
      .then((result) => { if (mounted) setRecommendations(result.items || []) })
      .catch(() => { if (mounted) setRecommendations([]) })
    return () => { mounted = false }
  }, [])

  const previousCartSignature = useRef(cartSignature)
  useEffect(() => {
    if (previousCartSignature.current !== cartSignature) {
      previousCartSignature.current = cartSignature
      if (coupon) {
        setCoupon(null)
        setCouponError('Cart changed. Apply the coupon again to refresh your discount.')
      }
    }
  }, [cartSignature])

  const excludedIds = useMemo(() => new Set(items.map((item) => item.productId)), [items])
  const suggested = recommendations.filter((item) => !excludedIds.has(item.id)).slice(0, 6)
  const subtotal = Number(cart?.subtotal || 0)
  const deliveryBySeller = new Map()
  items.forEach((item) => { if (!deliveryBySeller.has(item.sellerId)) deliveryBySeller.set(item.sellerId, Number(item.sellerDeliveryFee || 0)) })
  const delivery = [...deliveryBySeller.values()].reduce((sum, fee) => sum + fee, 0)
  const discount = Number(coupon?.discountAmount || 0)
  const total = Math.max(0, subtotal - discount + delivery)
  const canCheckout = items.length > 0 && !items.some((item) => !item.available) && !operation
  const loading = status === 'loading' && !cart

  function invalidateCoupon() {
    if (coupon) setCouponError('Cart changed. Apply the coupon again to refresh your discount.')
    setCoupon(null)
  }
  async function applyCoupon(event) {
    event.preventDefault()
    if (!couponInput.trim() || couponBusy) return
    setCouponBusy(true); setCouponError('')
    try {
      const quote = await previewCartCoupon(couponInput.trim())
      setCoupon({ code: quote.code, discountAmount: quote.discountAmount })
      setCouponInput(quote.code)
    } catch (requestError) {
      setCoupon(null)
      setCouponError(getRequestError(requestError, 'Coupon could not be applied.'))
    } finally { setCouponBusy(false) }
  }

  return <LazyMotion features={domAnimation}><MotionConfig reducedMotion="user"><m.div className="marketplace-page shop-page customer-cart-page" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
    <MainNavbar shopVariant/>
    <nav className="shop-secondary-nav" aria-label="Shop navigation"><div className="shop-secondary-inner">
      <Link to="/shop"><DashboardIcon name="grid"/> All Categories <span>⌄</span></Link><Link to="/shop?deal=true">Deals</Link><Link to="/shop?sort=newest">New Arrivals</Link><Link to="/shop?sort=popular">Best Sellers</Link><Link to="/orders">Track Order</Link><a href="mailto:support@shopsphere.com">Customer Support</a>
    </div></nav>

    <main className="customer-cart-shell">
      <header className="customer-cart-heading"><div className="customer-cart-heading-icon"><DashboardIcon name="cart"/></div><div><h1>Your Cart</h1><p>Review your items and proceed to checkout</p></div><Link to="/shop"><DashboardIcon name="arrow"/> Continue Shopping</Link></header>
      {error && cart && <div className="customer-cart-alert" role="alert"><span>{error}</span><button type="button" onClick={() => dispatch(clearCartFeedback())}>×</button></div>}
      {notice && <div className="customer-cart-notice" role="status">{notice}</div>}
      {loading ? <section className="customer-cart-empty"><h2>Loading your cart…</h2></section> : status === 'failed' && !cart ? <section className="customer-cart-empty"><h2>Your cart couldn’t be loaded</h2><p>{error}</p><button onClick={() => dispatch(fetchCart())} type="button">Try again</button></section> : !items.length ? <section className="customer-cart-empty"><div className="customer-cart-empty-icon"><DashboardIcon name="cart"/></div><h2>Your cart is empty</h2><p>Find something you love and it will appear here.</p><Link to="/shop">Start shopping</Link></section> : <>
        <div className="customer-cart-layout">
          <section className="customer-cart-table" aria-label="Cart items">
            <div className="customer-cart-table-head"><span>Product</span><span>Price</span><span>Quantity</span><span>Total</span><span>Action</span></div>
            <AnimatePresence initial={false}>{items.map((item) => <CartItemRow key={item.id} item={item} operation={operation} onQuantityChange={(row, quantity) => { invalidateCoupon(); dispatch(updateCartItem({ itemId: row.id, quantity })) }} onRemove={(id) => { invalidateCoupon(); dispatch(removeCartItem(id)) }}/>)}</AnimatePresence>
            <form className="customer-cart-coupon" onSubmit={applyCoupon}><label><DashboardIcon name="tag"/><input aria-label="Coupon code" placeholder="Enter coupon code" value={couponInput} onChange={(event) => setCouponInput(event.target.value.toUpperCase())}/></label><button disabled={couponBusy || !couponInput.trim()} type="submit">{couponBusy ? 'Applying…' : coupon ? 'Update Coupon' : 'Apply Coupon'}</button>{couponError && <span role="alert">{couponError}</span>}</form>
          </section>

          <aside className="customer-cart-summary" aria-label="Order summary">
            <h2><span><DashboardIcon name="tag"/></span>Order Summary</h2>
            <div className="customer-cart-summary-lines"><div><span>Subtotal ({cart?.totalQuantity || 0} Items)</span><strong>{formatProductPrice(subtotal)}</strong></div>{coupon && <div className="is-discount"><span>Discount · {coupon.code} <button type="button" onClick={() => { setCoupon(null); setCouponInput(''); setCouponError('') }}>Remove</button></span><strong>− {formatProductPrice(discount)}</strong></div>}<div><span>Shipping Charges</span><strong className={delivery === 0 ? 'is-free' : ''}>{delivery === 0 ? 'Free' : formatProductPrice(delivery)}</strong></div></div>
            <div className="customer-cart-grand-total"><span>Total Amount</span><strong>{formatProductPrice(total)}</strong></div>
            <Link className={`customer-cart-checkout${canCheckout ? '' : ' is-disabled'}`} aria-disabled={!canCheckout} state={{ couponCode: coupon?.code || '' }} onClick={(event) => { if (!canCheckout) event.preventDefault() }} to="/checkout"><span>▣</span> Proceed to Checkout <DashboardIcon name="arrow"/></Link>
            {items.some((item) => !item.available) && <p className="customer-cart-summary-error">Remove unavailable items before checkout.</p>}
            <div className="customer-cart-secure"><DashboardIcon name="check"/><span>Your information is secure<br/>and encrypted</span></div>
            <div className="customer-cart-promises"><div><DashboardIcon name="truck"/><strong>Free Shipping</strong><span>Seller fees at checkout</span></div><div><DashboardIcon name="check"/><strong>Secure Payment</strong><span>Protected checkout</span></div><div><DashboardIcon name="support"/><strong>24/7 Support</strong><span>Always here</span></div></div>
          </aside>
        </div>
        <section className="customer-cart-recommendations"><header><h2><span>★</span> Recommended for You</h2><Link to="/shop">View More <DashboardIcon name="arrow"/></Link></header>{suggested.length > 0 && <div className="market-product-grid">{suggested.map((product, index) => <ProductCard key={product.id} product={product} index={index}/>)}</div>}</section>
      </>}
    </main>
  </m.div></MotionConfig></LazyMotion>
}
