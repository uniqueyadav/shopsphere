import { useEffect, useMemo, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Link, useNavigate } from 'react-router-dom'
import { AnimatePresence, LazyMotion, MotionConfig, domAnimation, m } from 'framer-motion'
import MainNavbar from '../../components/navbar/MainNavbar'
import CustomerAccountSidebar, { DashboardIcon } from '../../components/customer/CustomerAccountSidebar'
import ProductCard, { formatProductPrice, getProductDiscount } from '../../components/product/ProductCard'
import { addCartItem, clearCartFeedback } from '../../features/cart/cartSlice'
import { clearWishlist, clearWishlistFeedback, fetchWishlist, removeWishlistItem } from '../../features/wishlist/wishlistSlice'
import { getPublicProducts } from '../../services/productService'
import './Shop.css'
import './CustomerDashboard.css'
import './CustomerWishlist.css'

function WishlistProductCard({ item, operation, cartOperation, onMove, onRemove }) {
  const discount = getProductDiscount(item)
  const isBusy = Boolean(operation || cartOperation)
  return <m.article className="customer-wishlist-card" layout initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, scale: .96, height: 0 }} transition={{ duration: .2 }} whileHover={{ y: -2 }}>
    <div className="customer-wishlist-image-wrap">
      <Link aria-label={`View ${item.productName}`} className="customer-wishlist-image-link" to={`/products/${item.productId}`}>
        {item.imageUrl ? <img alt={item.productName} loading="lazy" src={item.imageUrl} /> : <span className="customer-wishlist-image-empty">{item.productName?.slice(0, 1) || 'S'}</span>}
      </Link>
      {discount > 0 && <span className="customer-wishlist-discount">{discount}% OFF</span>}
      <button aria-label={`Remove ${item.productName} from wishlist`} className="customer-wishlist-heart" disabled={isBusy} onClick={() => onRemove(item.productId)} type="button"><svg aria-hidden="true" fill="currentColor" viewBox="0 0 24 24"><path d="M20.8 8.7c0 4.1-8.8 10-8.8 10S3.2 12.8 3.2 8.7A4.3 4.3 0 0 1 11 5.9l1 1 1-1a4.3 4.3 0 0 1 7.8 2.8Z" /></svg></button>
    </div>
    <div className="customer-wishlist-copy">
      <Link className="customer-wishlist-name" to={`/products/${item.productId}`}>{item.productName}</Link>
      {item.brand && <span className="customer-wishlist-brand">{item.brand}</span>}
      <div className="customer-wishlist-rating" aria-label={`${Number(item.averageRating || 0).toFixed(1)} out of 5 from ${item.reviewCount || 0} reviews`}><svg aria-hidden="true" viewBox="0 0 24 24"><path d="m12 2.8 2.8 5.7 6.3.9-4.6 4.4 1.1 6.3-5.6-3-5.6 3 1.1-6.3L3 9.4l6.2-.9L12 2.8Z" /></svg><span>{Number(item.averageRating || 0).toFixed(1)}</span><small>({Number(item.reviewCount || 0).toLocaleString()} reviews)</small></div>
      <div className="customer-wishlist-price"><strong>{formatProductPrice(discount ? item.discountPrice : item.price)}</strong>{discount > 0 && <del>{formatProductPrice(item.price)}</del>}</div>
      <span className={`customer-wishlist-stock${item.available ? '' : ' is-unavailable'}`}><i aria-hidden="true">{item.available ? '✓' : '!'}</i>{item.available ? 'In Stock' : 'Currently unavailable'}</span>
      <div className="customer-wishlist-actions">
        <button className="customer-wishlist-add" disabled={!item.available || isBusy} onClick={() => onMove(item)} type="button"><DashboardIcon name="cart" />{cartOperation?.productId === item.productId ? 'Adding…' : 'Add to Cart'}</button>
        <button className="customer-wishlist-remove" disabled={isBusy} onClick={() => onRemove(item.productId)} type="button"><DashboardIcon name="trash" />Remove from Wishlist</button>
      </div>
    </div>
  </m.article>
}

export default function Wishlist() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { wishlist, status, operation, error, notice } = useSelector((state) => state.wishlist)
  const cartOperation = useSelector((state) => state.cart.operation)
  const [feedback, setFeedback] = useState(null)
  const [recommendations, setRecommendations] = useState([])
  const [productSearch, setProductSearch] = useState('')
  const items = wishlist?.items || []
  const loading = status === 'loading' && !wishlist

  useEffect(() => {
    if (status === 'idle') dispatch(fetchWishlist())
  }, [dispatch, status])

  useEffect(() => {
    let active = true
    getPublicProducts({ page: 0, size: 12, sort: 'popular', availability: 'IN_STOCK' })
      .then((result) => { if (active) setRecommendations(result?.items || []) })
      .catch(() => { if (active) setRecommendations([]) })
    return () => { active = false }
  }, [])

  const suggestedProducts = useMemo(() => {
    const savedIds = new Set(items.map((item) => item.productId))
    return recommendations.filter((product) => !savedIds.has(product.id)).slice(0, 5)
  }, [items, recommendations])

  async function handleMoveToCart(item) {
    setFeedback(null)
    dispatch(clearWishlistFeedback())
    dispatch(clearCartFeedback())
    try {
      await dispatch(addCartItem({ productId: item.productId, quantity: 1 })).unwrap()
      await dispatch(removeWishlistItem(item.productId)).unwrap()
      setFeedback({ type: 'success', text: 'Product added to your cart and removed from your wishlist.' })
    } catch (message) {
      setFeedback({ type: 'error', text: typeof message === 'string' ? message : 'This product could not be moved to your cart.' })
    }
  }

  function handleRemove(productId) {
    setFeedback(null)
    dispatch(clearWishlistFeedback())
    dispatch(removeWishlistItem(productId))
  }

  function handleClear() {
    setFeedback(null)
    dispatch(clearWishlistFeedback())
    dispatch(clearWishlist())
  }

  async function handleShare() {
    const shareData = { title: 'My ShopSphere Wishlist', text: `I saved ${items.length} products on ShopSphere.`, url: window.location.href }
    try {
      if (navigator.share) await navigator.share(shareData)
      else {
        await navigator.clipboard.writeText(`${shareData.text} ${shareData.url}`)
        setFeedback({ type: 'success', text: 'Wishlist link copied.' })
      }
    } catch (shareError) {
      if (shareError?.name !== 'AbortError') setFeedback({ type: 'error', text: 'Wishlist could not be shared from this browser.' })
    }
  }

  return <LazyMotion features={domAnimation}><MotionConfig reducedMotion="user">
    <div className="marketplace-page shop-page customer-dashboard-page customer-wishlist-page">
      <MainNavbar shopVariant onSearchChange={setProductSearch} onSearchSubmit={(event) => { event.preventDefault(); navigate(`/shop?search=${encodeURIComponent(productSearch)}`) }} searchValue={productSearch} />
      <nav className="shop-secondary-nav" aria-label="Shop navigation"><div className="shop-secondary-inner">
        <Link to="/shop">▦ &nbsp; All Categories <span>⌄</span></Link><Link to="/shop?discountedOnly=true">Deals</Link><Link to="/shop?sort=newest">New Arrivals</Link><Link to="/shop?sort=popular">Best Sellers</Link><Link to="/orders">Track Order</Link><a href="mailto:support@shopsphere.com">Customer Support</a>
      </div></nav>

      <main className="customer-dashboard-layout customer-wishlist-layout">
        <CustomerAccountSidebar active="My Wishlist" />
        <section className="customer-wishlist-main">
          <header className="customer-wishlist-heading">
            <div><h1><svg aria-hidden="true" viewBox="0 0 24 24"><path d="M20.8 8.7c0 4.1-8.8 10-8.8 10S3.2 12.8 3.2 8.7A4.3 4.3 0 0 1 11 5.9l1 1 1-1a4.3 4.3 0 0 1 7.8 2.8Z" /></svg>My Wishlist</h1><p>{loading ? 'Loading your saved items…' : `Your saved items (${wishlist?.totalItems ?? items.length} ${items.length === 1 ? 'product' : 'products'})`}</p></div>
            <div className="customer-wishlist-heading-actions"><button className="wishlist-share-action" disabled={!items.length} onClick={handleShare} type="button"><DashboardIcon name="share" />Share Wishlist</button><button className="wishlist-clear-action" disabled={!items.length || Boolean(operation) || Boolean(cartOperation)} onClick={handleClear} type="button"><DashboardIcon name="trash" />{operation?.type === 'clear' ? 'Clearing…' : 'Clear All'}</button></div>
          </header>

          {(error && wishlist || feedback?.type === 'error') && <p className="customer-wishlist-feedback is-error" role="alert">{feedback?.type === 'error' ? feedback.text : error}</p>}
          {(notice || feedback?.type === 'success') && <p className="customer-wishlist-feedback" role="status">{feedback?.type === 'success' ? feedback.text : notice}</p>}

          {loading ? <div className="customer-wishlist-grid" role="status" aria-label="Loading wishlist">{[0, 1, 2, 3, 4].map((key) => <div className="customer-wishlist-skeleton" key={key} />)}</div>
            : status === 'failed' && !wishlist ? <section className="customer-wishlist-empty"><h2>Your wishlist couldn’t be loaded</h2><p>{error}</p><button onClick={() => dispatch(fetchWishlist())} type="button">Try again</button></section>
              : items.length === 0 ? <section className="customer-wishlist-empty"><h2>Your wishlist is ready for inspiration</h2><p>Save the products you love and come back when you’re ready.</p><Link to="/shop">Explore the shop</Link></section>
                : <div className="customer-wishlist-grid" aria-label="Saved products"><AnimatePresence initial={false}>{items.map((item) => <WishlistProductCard cartOperation={cartOperation} item={item} key={item.productId} onMove={handleMoveToCart} onRemove={handleRemove} operation={operation} />)}</AnimatePresence></div>}

          {suggestedProducts.length > 0 && <section className="customer-recommendations"><header><h2><DashboardIcon name="thumbs" />You May Also Like</h2><Link to="/shop">View More <DashboardIcon name="arrow" /></Link></header><div className="market-product-grid">{suggestedProducts.map((product, index) => <ProductCard index={index} key={product.id} product={product} />)}</div></section>}
        </section>
      </main>
    </div>
  </MotionConfig></LazyMotion>
}
