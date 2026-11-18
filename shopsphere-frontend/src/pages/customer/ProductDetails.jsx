import { useEffect, useMemo, useRef, useState } from 'react'
import { LazyMotion, MotionConfig, domAnimation, m } from 'framer-motion'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import MainNavbar from '../../components/navbar/MainNavbar'
import ProductGallery from '../../components/product/ProductGallery'
import ProductGrid from '../../components/product/ProductGrid'
import { formatProductPrice, getProductDiscount } from '../../components/product/ProductCard'
import { getRequestError } from '../../services/authService'
import { getProductReviewSummary, getPublicProduct, getPublicProductBySlug, getPublicProducts } from '../../services/productService'
import { addCartItem, clearCartFeedback } from '../../features/cart/cartSlice'
import { addWishlistItem, clearWishlistFeedback, removeWishlistItem } from '../../features/wishlist/wishlistSlice'
import './Shop.css'
import './ProductDetails.css'

function LoadingProduct() {
  return <div className="product-detail-loading" role="status" aria-label="Loading product details"><div/><div><i/><i/><i/><i/></div></div>
}

function ProductUnavailable({ notFound, message }) {
  return <section className="product-detail-empty" role="alert"><span>{notFound ? '404' : '!'}</span><h1>{notFound ? 'Product not found' : 'Product could not be loaded'}</h1><p>{message}</p><Link to="/shop">Browse the shop</Link></section>
}

function StarRow({ rating }) {
  return <span aria-label={rating.toFixed(1) + ' out of 5 stars'} className="product-stars">{[1, 2, 3, 4, 5].map((star) => <span aria-hidden="true" className={rating >= star - 0.35 ? 'is-filled' : ''} key={star}>★</span>)}</span>
}

function StoreSearchLink({ storeName }) {
  if (!storeName) return <span className="product-store-unavailable">Store details unavailable</span>
  return <Link to={'/shop?search=' + encodeURIComponent(storeName)}>Visit Store <span aria-hidden="true">›</span></Link>
}

export default function ProductDetails() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const user = useSelector((state) => state.auth.user)
  const cartOperation = useSelector((state) => state.cart.operation)
  const { wishlist, status: wishlistStatus, operation: wishlistOperation } = useSelector((state) => state.wishlist)
  const [productResult, setProductResult] = useState({ id: null, product: null, error: '', notFound: false })
  const [relatedResult, setRelatedResult] = useState({ productId: null, products: [], error: '' })
  const [reviewSummary, setReviewSummary] = useState(null)
  const [quantity, setQuantity] = useState(1)
  const [cartMessage, setCartMessage] = useState('')
  const [wishlistMessage, setWishlistMessage] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [activeTab, setActiveTab] = useState('description')
  const descriptionRef = useRef(null)
  const specificationsRef = useRef(null)
  const reviewsRef = useRef(null)
  const product = productResult.id === productId ? productResult.product : null
  const loading = Boolean(productId) && productResult.id !== productId
  const discount = product ? getProductDiscount(product) : null
  const stockQuantity = Number(product?.stockQuantity) || 0
  const inStock = stockQuantity > 0
  const isAvailable = product?.status === 'APPROVED' && product?.isActive === true
  const canPurchase = Boolean(isAvailable && inStock && user?.role === 'CUSTOMER')
  const isWishlisted = Boolean(wishlist?.items?.some((item) => item.productId === product?.id))
  const currentRelated = relatedResult.productId === product?.id ? relatedResult : null
  const relatedProducts = currentRelated?.products || []
  const price = useMemo(() => product ? formatProductPrice(discount ? product.discountPrice : product.price) : '', [discount, product])
  const rating = Number(reviewSummary?.averageRating ?? product?.averageRating ?? 0)
  const reviewCount = Number(reviewSummary?.reviewCount ?? product?.reviewCount ?? 0)
  const priceDiscount = discount ? formatProductPrice(product.price) : ''

  useEffect(() => {
    if (!productId) return undefined
    const controller = new AbortController()
    let active = true
    setReviewSummary(null)
    const fetchProduct = /^[1-9]\d*$/.test(productId) ? getPublicProduct(productId, controller.signal) : getPublicProductBySlug(productId, controller.signal)
    fetchProduct.then((result) => {
      if (!active) return
      if (result.status !== 'APPROVED' || result.isActive !== true) {
        setProductResult({ id: productId, product: null, error: '', notFound: true })
        return
      }
      setProductResult({ id: productId, product: result, error: '', notFound: false })
      setQuantity(1)
    }).catch((error) => {
      if (!active || controller.signal.aborted) return
      setProductResult({ id: productId, product: null, error: getRequestError(error, 'Product details could not be loaded. Please try again.'), notFound: error.response?.status === 404 })
    })
    return () => { active = false; controller.abort() }
  }, [productId])

  useEffect(() => {
    if (!product?.id) return undefined
    const controller = new AbortController()
    let active = true
    getProductReviewSummary(product.id, controller.signal)
      .then((summary) => { if (active) setReviewSummary(summary) })
      .catch((error) => { if (active && !controller.signal.aborted) setReviewSummary(null) })
    return () => { active = false; controller.abort() }
  }, [product?.id])

  useEffect(() => {
    if (!product?.categoryId) return undefined
    const controller = new AbortController()
    let active = true
    getPublicProducts({ page: 0, size: 6, categoryId: product.categoryId, sort: 'popular' }, controller.signal)
      .then((result) => { if (active) setRelatedResult({ productId: product.id, products: (result.items || []).filter((item) => item.id !== product.id), error: '' }) })
      .catch((error) => { if (active && !controller.signal.aborted) setRelatedResult({ productId: product.id, products: [], error: getRequestError(error, 'Related products could not be loaded.') }) })
    return () => { active = false; controller.abort() }
  }, [product?.categoryId, product?.id])

  function changeTab(tab, ref) {
    setActiveTab(tab)
    ref.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  function submitSearch(event) {
    event.preventDefault()
    const query = searchTerm.trim()
    if (query) navigate('/shop?search=' + encodeURIComponent(query))
  }

  async function addToCart(andCheckout = false) {
    setCartMessage('')
    if (user?.role !== 'CUSTOMER') {
      navigate('/login', { state: { from: window.location.pathname } })
      return
    }
    dispatch(clearCartFeedback())
    try {
      await dispatch(addCartItem({ productId: product.id, quantity })).unwrap()
      if (andCheckout) navigate('/checkout')
      else setCartMessage('Added to your cart.')
    } catch (error) {
      setCartMessage(typeof error === 'string' ? error : 'This item could not be added to your cart.')
    }
  }

  async function toggleWishlist() {
    if (user?.role !== 'CUSTOMER') {
      navigate('/login', { state: { from: window.location.pathname } })
      return
    }
    setWishlistMessage('')
    dispatch(clearWishlistFeedback())
    try {
      if (isWishlisted) {
        await dispatch(removeWishlistItem(product.id)).unwrap()
        setWishlistMessage('Removed from your wishlist.')
      } else {
        await dispatch(addWishlistItem(product.id)).unwrap()
        setWishlistMessage('Added to your wishlist.')
      }
    } catch (error) {
      setWishlistMessage(typeof error === 'string' ? error : 'Wishlist could not be updated.')
    }
  }

  const notFound = productResult.id === productId && productResult.notFound
  const unavailableMessage = !inStock ? 'This product is out of stock.' : 'Sign in as a customer to purchase this product.'

  return (
    <LazyMotion features={domAnimation}>
      <MotionConfig reducedMotion="user">
        <m.div animate={{ opacity: 1 }} className="marketplace-page shop-page product-detail-page" initial={{ opacity: 0 }} transition={{ duration: 0.25 }}>
          <MainNavbar shopVariant searchValue={searchTerm} onSearchChange={setSearchTerm} onSearchSubmit={submitSearch}/>
          <nav className="shop-secondary-nav product-secondary-nav" aria-label="Shop navigation">
            <div className="shop-secondary-inner">
              <Link to="/shop">▦ <span>All Categories</span>⌄</Link>
              <Link to="/shop?discountedOnly=true">Deals</Link><Link to="/shop?sort=newest">New Arrivals</Link>
              <Link to="/shop?sort=popular">Best Sellers</Link><Link to="/orders">Track Order</Link><span>Customer Support</span>
            </div>
          </nav>
          <main className="product-detail-shell">
            <nav aria-label="Breadcrumb" className="product-detail-breadcrumb">
              <Link to="/shop">Home</Link><span>›</span>
              {product?.categoryName && <><Link to={'/shop?categoryId=' + product.categoryId}>{product.categoryName}</Link><span>›</span></>}
              <span aria-current="page">{product?.productName || 'Product'}</span>
            </nav>

            {loading ? <LoadingProduct/> : !product ? <ProductUnavailable notFound={notFound} message={productResult.error || 'This item is not available in the approved catalog.'}/> : (
              <>
                <section className="product-detail-top">
                  <div className="product-gallery-panel">
                    <ProductGallery images={product.images || []} productName={product.productName}/>
                  </div>

                  <section className="product-summary" aria-labelledby="product-title">
                    <div className="product-listing-badges">
                      <span className="product-listing-category">{product.categoryName || 'ShopSphere'}</span>
                      {rating >= 4.5 && reviewCount > 0 && <span className="product-top-rated">Top Rated</span>}
                    </div>
                    <h1 id="product-title">{product.productName}</h1>
                    <p className="product-summary-subtitle">{product.description || product.brand || 'Product details from the ShopSphere seller.'}</p>
                    <div className="product-summary-rating">
                      <strong>{rating.toFixed(1)} ★</strong><StarRow rating={rating}/>
                      <button onClick={() => changeTab('reviews', reviewsRef)} type="button">{reviewCount.toLocaleString()} Ratings</button>
                    </div>
                    <div className="product-summary-price">
                      <strong>{price}</strong>
                      {discount && <><del>{priceDiscount}</del><span>{discount}% OFF</span></>}
                    </div>
                    {discount && <p className="product-tax-note">Current listed offer price</p>}

                    <section className="product-offer-box" aria-label="Current offer">
                      <h2><span aria-hidden="true">◆</span> Current Offer</h2>
                      {discount ? <p><span>✓</span> Save {discount}% compared with the listed regular price.</p> : <p>There are no discounted offers on this listing currently.</p>}
                      {product.discountPrice && <p><span>✓</span> Offer price: {formatProductPrice(product.discountPrice)}</p>}
                    </section>

                    <div className="product-delivery-box">
                      <div><span aria-hidden="true">▣</span><strong>Availability</strong><span>{inStock ? stockQuantity.toLocaleString() + ' in stock' : 'Out of stock'}</span></div>
                      <div><span aria-hidden="true">◆</span><strong>Sold by</strong><span>{product.storeName || 'ShopSphere seller'}</span></div>
                    </div>
                  </section>

                  <aside className="product-purchase-card" aria-label="Purchase options">
                    <p>Sold by</p>
                    <h2>{product.storeName || 'ShopSphere Seller'}</h2>
                    {reviewCount > 0 ? <div className="product-seller-rating"><strong>{rating.toFixed(1)} ★</strong> <span>({reviewCount.toLocaleString()} Ratings)</span></div> : <p className="product-no-rating">No approved ratings yet</p>}
                    <StoreSearchLink storeName={product.storeName}/>
                    <div className="product-assurances">
                      <div><span>↻</span><p><strong>Seller listed item</strong><small>Marketplace seller details shown above</small></p></div>
                      <div><span>◇</span><p><strong>Secure checkout</strong><small>Checkout through ShopSphere</small></p></div>
                      <div><span>✓</span><p><strong>Availability</strong><small>{inStock ? stockQuantity.toLocaleString() + ' units listed' : 'Currently unavailable'}</small></p></div>
                    </div>
                    <label className="product-quantity-field">Quantity
                      <div><button aria-label="Decrease quantity" disabled={!inStock || quantity <= 1} onClick={() => setQuantity((value) => Math.max(1, value - 1))} type="button">−</button><input aria-label="Quantity" max={Math.max(1, stockQuantity)} min="1" onChange={(event) => setQuantity(Math.min(Math.max(1, Number(event.target.value) || 1), Math.max(1, stockQuantity)))} type="number" value={quantity}/><button aria-label="Increase quantity" disabled={!inStock || quantity >= stockQuantity} onClick={() => setQuantity((value) => Math.min(stockQuantity, value + 1))} type="button">+</button></div>
                    </label>
                    <button className="product-add-cart" disabled={!canPurchase || Boolean(cartOperation)} onClick={() => addToCart(false)} title={!canPurchase ? unavailableMessage : 'Add this product to your cart'} type="button">🛒 {cartOperation?.type === 'add' ? 'Adding…' : 'Add to Cart'}</button>
                    <button className="product-buy-now" disabled={!canPurchase || Boolean(cartOperation)} onClick={() => addToCart(true)} title={!canPurchase ? unavailableMessage : 'Add to cart and go to checkout'} type="button">⚡ Buy Now</button>
                    <button className={isWishlisted ? 'product-wishlist is-saved' : 'product-wishlist'} onClick={toggleWishlist} type="button"><span>{isWishlisted ? '♥' : '♡'}</span> {isWishlisted ? 'Added to Wishlist' : 'Add to Wishlist'}</button>
                    {(cartMessage || wishlistMessage || (!inStock && unavailableMessage)) && <p className="product-action-message" role="status">{cartMessage || wishlistMessage || unavailableMessage}</p>}
                  </aside>
                </section>

                <nav className="product-detail-tabs" aria-label="Product information sections">
                  <button className={activeTab === 'description' ? 'is-active' : ''} onClick={() => changeTab('description', descriptionRef)} type="button">Description</button>
                  <button className={activeTab === 'specifications' ? 'is-active' : ''} onClick={() => changeTab('specifications', specificationsRef)} type="button">Specifications</button>
                  <button className={activeTab === 'reviews' ? 'is-active' : ''} onClick={() => changeTab('reviews', reviewsRef)} type="button">Reviews ({reviewCount.toLocaleString()})</button>
                </nav>

                <section className="product-detail-lower">
                  <div className="product-information-column">
                    <section className="product-description-section" id="product-description" ref={descriptionRef}>
                      <h2>About This Item</h2>
                      <p>{product.description || 'The seller has not added a product description.'}</p>
                      <ul><li><span>✓</span>Listed by {product.storeName || 'a ShopSphere seller'}</li><li><span>✓</span>Category: {product.categoryName || 'Not specified'}</li><li><span>✓</span>{inStock ? 'Available to order' : 'Currently out of stock'}</li></ul>
                    </section>
                    <section className="product-specifications-section" id="product-specifications" ref={specificationsRef}>
                      <h2>Specifications</h2>
                      <dl><div><dt>Brand</dt><dd>{product.brand || 'Not specified'}</dd></div><div><dt>SKU</dt><dd>{product.sku || 'Not specified'}</dd></div><div><dt>Category</dt><dd>{product.categoryName || 'Not specified'}</dd></div><div><dt>Seller</dt><dd>{product.storeName || 'ShopSphere seller'}</dd></div><div><dt>Stock</dt><dd>{stockQuantity.toLocaleString()}</dd></div></dl>
                    </section>
                  </div>
                  <aside className="product-reviews-card" id="product-reviews" ref={reviewsRef}>
                    <h2>Customer Reviews</h2>
                    <div className="product-review-score"><strong>{rating.toFixed(1)}</strong><StarRow rating={rating}/><span>Based on {reviewCount.toLocaleString()} ratings</span></div>
                    {[5, 4, 3, 2, 1].map((star) => {
                      const count = Number(reviewSummary?.ratingCounts?.[star] || 0)
                      const percentage = reviewCount ? Math.round(count * 100 / reviewCount) : 0
                      return <div className="product-review-distribution" key={star}><span>{star} <b>★</b></span><div><i style={{ width: percentage + '%' }}/></div><small>{percentage}%</small></div>
                    })}
                    <p className="product-review-note">{reviewCount ? 'Only approved customer reviews are included.' : 'No approved reviews have been submitted for this product yet.'}</p>
                  </aside>
                </section>

                {product.categoryId && <section className="product-related-section">
                  <div className="product-related-heading"><h2>You May Also Like</h2><Link to={'/shop?categoryId=' + encodeURIComponent(product.categoryId)}>View More <span>→</span></Link></div>
                  {currentRelated?.error ? <p role="status">{currentRelated.error}</p> : <ProductGrid loading={!currentRelated} products={relatedProducts}/>}
                </section>}
              </>
            )}
          </main>
        </m.div>
      </MotionConfig>
    </LazyMotion>
  )
}
