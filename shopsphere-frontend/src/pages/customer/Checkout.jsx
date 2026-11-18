import { useEffect, useState } from 'react'
import { AnimatePresence, LazyMotion, MotionConfig, domAnimation, m } from 'framer-motion'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import MainNavbar from '../../components/navbar/MainNavbar'
import { formatProductPrice } from '../../components/product/ProductCard'
import { fetchAddresses } from '../../features/addresses/addressSlice'
import { fetchCart } from '../../features/cart/cartSlice'
import { getRequestError } from '../../services/authService'
import { checkoutOrder } from '../../services/orderService'
import { previewCartCoupon } from '../../services/cartService'
import { DashboardIcon } from '../../components/customer/CustomerAccountSidebar'
import './Checkout.css'
import './Shop.css'

function createCheckoutKey() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function CheckoutSkeleton() {
  return (
    <div aria-label="Loading checkout" className="checkout-skeleton" role="status">
      <div className="checkout-skeleton-main"><span /><span /><span /><span /></div>
      <div className="checkout-skeleton-summary"><span /><span /><span /></div>
      <span className="checkout-visually-hidden">Loading your checkout…</span>
    </div>
  )
}

function CheckoutItem({ item }) {
  return (
    <article className="checkout-item">
      {item.imageUrl
        ? <img alt="" className="checkout-item-image" decoding="async" loading="lazy" src={item.imageUrl} />
        : <div aria-hidden="true" className="checkout-item-image checkout-item-image--empty">SS</div>}
      <div className="checkout-item-copy">
        <span className="checkout-item-seller">{item.sellerName}</span>
        <Link to={`/products/${item.productId}`}>{item.productName}</Link>
        <span>Qty {item.quantity} · {formatProductPrice(item.unitPrice)} each</span>
      </div>
      <strong>{formatProductPrice(item.lineSubtotal)}</strong>
    </article>
  )
}

export default function Checkout() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const { cart, status: cartStatus, operation, error: cartError } = useSelector((state) => state.cart)
  const { items: addresses, status: addressStatus, error: addressError } = useSelector((state) => state.addresses)
  const customerId = useSelector((state) => state.auth.user?.id)
  const storageKey = `shopsphere.checkout.idempotency.${customerId || 'customer'}`
  const addressStorageKey = `shopsphere.checkout.address.${customerId || 'customer'}`
  const [selectedAddressId, setSelectedAddressId] = useState(
    () => sessionStorage.getItem(addressStorageKey) || '',
  )
  const [paymentMethod, setPaymentMethod] = useState('ONLINE')
  const [couponCode, setCouponCode] = useState(() => location.state?.couponCode || '')
  const [couponDiscount, setCouponDiscount] = useState(0)
  const [couponMessage, setCouponMessage] = useState('')
  const [couponBusy, setCouponBusy] = useState(false)
  const [addressEditorOpen, setAddressEditorOpen] = useState(false)
  const [paymentEditorOpen, setPaymentEditorOpen] = useState(false)
  const [idempotencyKey, setIdempotencyKey] = useState(() => {
    const storedKey = sessionStorage.getItem(storageKey)
    if (storedKey) return storedKey
    const key = createCheckoutKey()
    sessionStorage.setItem(storageKey, key)
    return key
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [shopSearch, setShopSearch] = useState('')

  useEffect(() => {
    dispatch(fetchCart())
    dispatch(fetchAddresses())
  }, [dispatch])

  useEffect(() => {
    if (!location.state?.couponCode) return
    let active = true
    previewCartCoupon(location.state.couponCode)
      .then((preview) => { if (active) { setCouponCode(preview.code); setCouponDiscount(Number(preview.discountAmount || 0)); setCouponMessage('Coupon applied successfully.') } })
      .catch((requestError) => { if (active) setCouponMessage(getRequestError(requestError, 'Coupon could not be applied.')) })
    return () => { active = false }
  }, [location.state?.couponCode])

  const cartItems = cart?.items || []
  const sellerFees = new Map()
  cartItems.forEach((item) => {
    if (!sellerFees.has(item.sellerId)) {
      sellerFees.set(item.sellerId, {
        name: item.sellerName,
        fee: Number(item.sellerDeliveryFee || 0),
      })
    }
  })
  const deliveryCharges = Array.from(sellerFees.values()).reduce((sum, seller) => sum + seller.fee, 0)
  const subtotal = Number(cart?.subtotal || 0)
  const total = Math.max(0, subtotal + deliveryCharges - couponDiscount)
  const defaultAddress = addresses.find((address) => address.defaultAddress)
  const selectedAddress = addresses.find((address) => String(address.id) === String(selectedAddressId))
  const activeAddressId = selectedAddress
    ? String(selectedAddress.id)
    : defaultAddress
      ? String(defaultAddress.id)
      : ''
  const checkoutLoading = cartStatus === 'loading' || addressStatus === 'loading'
  const canSubmit = cartItems.length > 0
    && cartItems.every((item) => item.available)
    && Boolean(activeAddressId)
    && cartStatus !== 'failed'
    && addressStatus !== 'failed'
    && !submitting
    && !operation

  function chooseAddress(addressId) {
    setSelectedAddressId(addressId)
    setAddressEditorOpen(false)
    sessionStorage.setItem(addressStorageKey, addressId)
    setError(null)
    const key = createCheckoutKey()
    setIdempotencyKey(key)
    sessionStorage.setItem(storageKey, key)
  }

  function choosePayment(method) {
    setPaymentMethod(method)
    setError(null)
    const key = createCheckoutKey()
    setIdempotencyKey(key)
    sessionStorage.setItem(storageKey, key)
    setPaymentEditorOpen(false)
  }

  function changeCouponCode(value) {
    setCouponCode(value.toUpperCase().replace(/[^A-Z0-9_-]/g, ''))
    setCouponDiscount(0)
    setCouponMessage('')
    const key = createCheckoutKey()
    setIdempotencyKey(key)
    sessionStorage.setItem(storageKey, key)
  }

  async function applyCoupon() {
    if (!couponCode.trim()) { setCouponMessage('Enter a coupon code first.'); return }
    setCouponBusy(true)
    setCouponMessage('')
    try {
      const preview = await previewCartCoupon(couponCode.trim())
      setCouponCode(preview.code)
      setCouponDiscount(Number(preview.discountAmount || 0))
      setCouponMessage('Coupon applied successfully.')
      const key = createCheckoutKey()
      setIdempotencyKey(key)
      sessionStorage.setItem(storageKey, key)
    } catch (requestError) {
      setCouponDiscount(0)
      setCouponMessage(getRequestError(requestError, 'This coupon could not be applied.'))
    } finally { setCouponBusy(false) }
  }

  async function placeOrder(event) {
    event.preventDefault()
    if (!activeAddressId) {
      setError('Choose a delivery address to continue.')
      return
    }
    if (!cartItems.length || !cartItems.every((item) => item.available)) {
      setError('Remove unavailable products before placing your order.')
      return
    }

    setSubmitting(true)
    setError(null)
    sessionStorage.setItem(addressStorageKey, String(activeAddressId))
    try {
      const order = await checkoutOrder({
        addressId: Number(activeAddressId),
        paymentMethod,
        couponCode,
        idempotencyKey,
      })
      sessionStorage.removeItem(storageKey)
      sessionStorage.removeItem(addressStorageKey)
      dispatch(fetchCart())
      navigate(`/orders/${order.id}/confirmation`, { state: { order } })
    } catch (requestError) {
      setError(getRequestError(requestError, 'Your order could not be placed. Please try again.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <LazyMotion features={domAnimation}>
      <MotionConfig reducedMotion="user">
        <m.div
          animate={{ opacity: 1, y: 0 }}
          className="checkout-page shop-page"
          initial={{ opacity: 0, y: 8 }}
          transition={{ duration: .3 }}
        >
          <MainNavbar shopVariant onSearchChange={setShopSearch} onSearchSubmit={(event) => { event.preventDefault(); navigate(`/shop?search=${encodeURIComponent(shopSearch)}`) }} searchValue={shopSearch}/>
          <nav className="shop-secondary-nav" aria-label="Shop navigation"><div className="shop-secondary-inner">
            <Link to="/shop"><DashboardIcon name="grid"/>All Categories <DashboardIcon className="checkout-nav-chevron" name="arrow"/></Link>
            <Link to="/shop?discountedOnly=true"><DashboardIcon name="tag"/>Deals</Link>
            <Link to="/shop?sort=newest"><DashboardIcon name="star"/>New Arrivals</Link>
            <Link to="/shop?sort=popular"><DashboardIcon name="thumbs"/>Best Sellers</Link>
            <Link to="/orders"><DashboardIcon name="truck"/>Track Order</Link>
            <a href="mailto:support@shopsphere.com"><DashboardIcon name="support"/>Customer Support</a>
          </div></nav>
          <main className="checkout-container checkout-review-container">
            <header className="checkout-review-heading">
              <span className="checkout-review-success"><svg aria-hidden="true" viewBox="0 0 24 24"><path d="m5 12 4 4L19 6"/></svg></span>
              <div className="checkout-review-title"><h1>Review Your Order</h1><p>Please review your order details before placing the order</p></div>
              <ol className="checkout-progress" aria-label="Checkout progress"><li className="is-complete"><span>✓</span><small>Shipping Address</small></li><li className="is-complete"><span>✓</span><small>Payment Method</small></li><li className="is-current"><span>3</span><small>Review &amp; Place Order</small></li></ol>
            </header>

            {checkoutLoading ? (
              <CheckoutSkeleton />
            ) : cartStatus === 'failed' && !cart ? (
              <section className="checkout-empty">
                <h2>Your cart couldn’t be loaded.</h2>
                <p>Return to your cart and try again.</p>
                <Link className="checkout-primary-link" to="/cart">Back to cart</Link>
              </section>
            ) : cartItems.length === 0 ? (
              <section className="checkout-empty">
                <h2>There’s nothing to check out yet.</h2>
                <p>Add items to your cart and they’ll be waiting here.</p>
                <Link className="checkout-primary-link" to="/shop">Explore the shop</Link>
              </section>
            ) : (
              <form className="checkout-layout checkout-review-layout" onSubmit={placeOrder}>
                <div className="checkout-main checkout-review-main">
                  <section aria-labelledby="checkout-address-title" className="checkout-section checkout-review-section">
                    <header className="checkout-section-heading checkout-review-section-heading">
                      <span className="checkout-review-icon"><svg viewBox="0 0 24 24"><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/></svg></span>
                      <h2 id="checkout-address-title">Shipping Address</h2>
                      <Link className="checkout-edit-button" state={{ returnTo: '/checkout' }} to="/addresses">Add address</Link>
                      <button className="checkout-edit-button" onClick={() => setAddressEditorOpen((open) => !open)} type="button">{addressEditorOpen ? 'Done' : 'Edit'}</button>
                    </header>
                    {addressStatus === 'failed' ? (
                      <div className="checkout-no-address">
                        <p>{addressError || 'Saved addresses could not be loaded.'}</p>
                        <button className="checkout-secondary-link" onClick={() => dispatch(fetchAddresses())} type="button">Try again</button>
                      </div>
                    ) : addresses.length === 0 ? (
                      <div className="checkout-no-address">
                        <p>You need a saved address before placing an order.</p>
                        <Link className="checkout-secondary-link" state={{ returnTo: '/checkout' }} to="/addresses">Add a delivery address</Link>
                      </div>
                    ) : (
                      <div className={`checkout-address-list checkout-review-address-list${addressEditorOpen ? ' is-editing' : ''}`}>
                        {addresses.map((address) => (
                          <label
                            className={`checkout-address-option${String(address.id) === String(activeAddressId) ? ' checkout-address-option--selected' : ''}`}
                            key={address.id}
                          >
                            <input
                              checked={String(address.id) === String(activeAddressId)}
                              name="addressId"
                              onChange={() => chooseAddress(String(address.id))}
                              type="radio"
                              value={address.id}
                            />
                            <span className="checkout-radio-mark" />
                            <span className="checkout-address-content">
                              <span className="checkout-address-name">
                                {address.fullName}
                                <span className={`checkout-address-type checkout-address-type--${address.addressType.toLowerCase()}`}>{address.addressType}</span>
                                {address.defaultAddress && <span className="checkout-default-tag">DEFAULT</span>}
                              </span>
                              <span>{address.houseFlatNumber}, {address.streetArea}{address.landmark ? `, near ${address.landmark}` : ''}</span>
                              <span>{address.city}, {address.state} - {address.pinCode}, India</span>
                              {addressEditorOpen && <span>{address.mobileNumber}</span>}
                            </span>
                            {!addressEditorOpen && <span className="checkout-address-type checkout-review-address-badge">✓ {address.addressType}</span>}
                          </label>
                        ))}
                      </div>
                    )}
                  </section>

                  <section aria-labelledby="checkout-payment-title" className="checkout-section checkout-review-section">
                    <header className="checkout-section-heading checkout-review-section-heading">
                      <span className="checkout-review-icon"><svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 10h18M7 15h3"/></svg></span>
                      <h2 id="checkout-payment-title">Payment Method</h2>
                      <button className="checkout-edit-button" onClick={() => setPaymentEditorOpen((open) => !open)} type="button">{paymentEditorOpen ? 'Done' : 'Edit'}</button>
                    </header>
                    <div className={`checkout-payment-options checkout-review-payment-options${paymentEditorOpen ? ' is-editing' : ''}`}>
                      <label className={`checkout-payment-option${paymentMethod === 'COD' ? ' checkout-payment-option--selected' : ''}`}>
                        <input
                          checked={paymentMethod === 'COD'}
                          name="paymentMethod"
                          onChange={() => choosePayment('COD')}
                          type="radio"
                          value="COD"
                        />
                        <span className="checkout-radio-mark" />
                        <span className="checkout-payment-copy"><strong>Cash on Delivery</strong><small>Pay when your order arrives</small></span>
                        {paymentMethod === 'COD' && <span className="checkout-payment-check">✓</span>}
                      </label>
                      <label className={`checkout-payment-option${paymentMethod === 'ONLINE' ? ' checkout-payment-option--selected' : ''}`}>
                        <input
                          checked={paymentMethod === 'ONLINE'}
                          name="paymentMethod"
                          onChange={() => choosePayment('ONLINE')}
                          type="radio"
                          value="ONLINE"
                        />
                        <span className="checkout-radio-mark" />
                        <span className="checkout-upi-mark" aria-hidden="true">UPI<span>▰</span></span>
                        <span className="checkout-payment-copy"><strong>UPI Payment</strong><small>Pay securely with UPI, cards or net banking</small></span>
                        {paymentMethod === 'ONLINE' && <span className="checkout-payment-check">✓</span>}
                      </label>
                    </div>
                    {!paymentEditorOpen && <p className="checkout-review-selected-payment">Selected: {paymentMethod === 'ONLINE' ? 'UPI / Online Payment' : 'Cash on Delivery'}</p>}
                  </section>

                  <section aria-labelledby="checkout-items-title" className="checkout-section checkout-items-section checkout-review-section">
                    <header className="checkout-section-heading checkout-review-section-heading">
                      <span className="checkout-review-icon"><svg viewBox="0 0 24 24"><path d="m12 2 9 5-9 5-9-5 9-5Z"/><path d="m3 7 9 5 9-5M3 7v10l9 5 9-5V7M12 12v10"/></svg></span>
                      <h2 id="checkout-items-title">Your Order Items</h2>
                      <Link to="/cart">Edit cart</Link>
                    </header>
                    <div className="checkout-items-list checkout-review-items-list">
                      {cartItems.map((item) => (
                        <CheckoutItem item={item} key={item.id} />
                      ))}
                    </div>
                    {cartItems.some((item) => !item.available) && (
                      <p className="checkout-stock-warning" role="alert">Some items are no longer available. Return to your cart to remove them.</p>
                    )}
                  </section>
                </div>

                <aside aria-label="Order total" className="checkout-summary checkout-review-summary">
                  <h2><span className="checkout-summary-title-icon">▤</span>Order Summary</h2>
                  <div className="checkout-summary-lines">
                    <div><span>Subtotal ({cart.totalQuantity} {cart.totalQuantity === 1 ? 'Item' : 'Items'})</span><strong>{formatProductPrice(subtotal)}</strong></div>
                    {couponDiscount > 0 && <div><span>Discount {couponCode && `(${couponCode})`}</span><strong className="checkout-discount-amount">- {formatProductPrice(couponDiscount)}</strong></div>}
                    <div><span>Shipping Charges</span><strong>{deliveryCharges ? formatProductPrice(deliveryCharges) : 'Free'}</strong></div>
                    <div className="checkout-summary-coupon"><label htmlFor="checkout-coupon">Coupon code</label><div><input id="checkout-coupon" autoCapitalize="characters" autoComplete="off" maxLength="40" onChange={(event) => changeCouponCode(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); applyCoupon() } }} placeholder="Enter coupon code" value={couponCode}/><button type="button" onClick={applyCoupon} disabled={couponBusy}>{couponBusy ? '...' : 'Apply'}</button></div>{couponMessage && <small className={couponDiscount > 0 ? 'is-success' : 'is-error'} role="status">{couponMessage}</small>}</div>
                  </div>
                  <div className="checkout-summary-total">
                    <span>Total Amount</span><strong>{formatProductPrice(total)}</strong>
                  </div>
                  <div className="checkout-review-secure"><span>✓</span><div><strong>Your order is secure and encrypted</strong><small>Your personal information is safe with us.</small></div></div>
                  {cartStatus === 'failed' && cart && (
                    <div className="checkout-error" role="alert">
                      <p>{cartError || 'Your cart could not be refreshed. Try again before checking out.'}</p>
                      <button onClick={() => dispatch(fetchCart())} type="button">Refresh cart</button>
                    </div>
                  )}
                  <AnimatePresence>
                    {error && (
                      <m.p
                        animate={{ opacity: 1, y: 0 }}
                        className="checkout-error"
                        exit={{ opacity: 0, y: -4 }}
                        initial={{ opacity: 0, y: -4 }}
                        role="alert"
                      >{error}</m.p>
                    )}
                  </AnimatePresence>
                  <button className="checkout-place-order" disabled={!canSubmit} type="submit">
                    {submitting
                      ? 'Placing Order…'
                      : <><span aria-hidden="true">▣</span>Place Order<span aria-hidden="true">→</span></>}
                  </button>
                  {!activeAddressId && addresses.length > 0 && <p className="checkout-button-note">Select a delivery address to continue.</p>}
                  <label className="checkout-terms"><input type="checkbox" defaultChecked required/> I agree to the <a href="#terms">Terms &amp; Conditions</a> and <a href="#privacy">Privacy Policy</a></label>
                </aside>
              </form>
            )}
            {!checkoutLoading && cartItems.length > 0 && <footer className="checkout-benefits"><div><span>▰</span><strong>Shipping</strong><small>Seller fees shown above</small></div><div><span>✓</span><strong>Secure Payment</strong><small>100% secure</small></div><div><span>♧</span><strong>24/7 Support</strong><small>Always here</small></div><div><span>↻</span><strong>Easy Returns</strong><small>7-day return window</small></div></footer>}
          </main>
        </m.div>
      </MotionConfig>
    </LazyMotion>
  )
}
