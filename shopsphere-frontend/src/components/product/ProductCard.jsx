import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { m } from 'framer-motion'
import { useDispatch, useSelector } from 'react-redux'
import { addWishlistItem, removeWishlistItem } from '../../features/wishlist/wishlistSlice'
import { addCartItem } from '../../features/cart/cartSlice'

const currency = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2,
})

export function formatProductPrice(value) {
  return currency.format(Number(value))
}

export function getProductDiscount(product) {
  const price = Number(product.price)
  const discountPrice = Number(product.discountPrice)
  if (!Number.isFinite(price) || !Number.isFinite(discountPrice) || discountPrice <= 0 || discountPrice >= price) {
    return null
  }
  return Math.round(((price - discountPrice) / price) * 100)
}

export default function ProductCard({ product, index = 0 }) {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const userRole = useSelector((state) => state.auth.user?.role)
  const { wishlist, status: wishlistStatus, operation } = useSelector((state) => state.wishlist)
  const cartOperation = useSelector((state) => state.cart.operation)
  const [wishlistFeedback, setWishlistFeedback] = useState(null)
  const images = product.images || []
  const primaryImage = images.find((image) => image.isPrimary) || images[0]
  const discount = getProductDiscount(product)
  const inStock = product.stockQuantity > 0
  const isWishlisted = Boolean(wishlist?.items.some((item) => item.productId === product.id))

  async function toggleWishlist() {
    setWishlistFeedback(null)
    try {
      if (isWishlisted) {
        await dispatch(removeWishlistItem(product.id)).unwrap()
        setWishlistFeedback({ type: 'success', text: 'Removed from wishlist.' })
      } else {
        await dispatch(addWishlistItem(product.id)).unwrap()
        setWishlistFeedback({ type: 'success', text: 'Saved to wishlist.' })
      }
    } catch (error) {
      setWishlistFeedback({
        type: 'error',
        text: typeof error === 'string' ? error : 'Wishlist could not be updated.',
      })
    }
  }

  async function addToCart() {
    if (userRole !== 'CUSTOMER') {
      navigate('/login', { state: { from: `/products/${product.id}` } })
      return
    }
    try {
      await dispatch(addCartItem({ productId: product.id, quantity: 1 })).unwrap()
    } catch (error) {
      setWishlistFeedback({ type: 'error', text: typeof error === 'string' ? error : 'This product could not be added to your cart.' })
    }
  }

  return (
    <m.article
      className="market-product-card"
      custom={index}
      variants={{
        hidden: { opacity: 0, y: 12 },
        visible: (itemIndex) => ({
          opacity: 1,
          y: 0,
          transition: { delay: Math.min(itemIndex % 12, 8) * 0.035, duration: 0.28 },
        }),
      }}
      whileHover={{ y: -3 }}
    >
      <Link aria-label={`View ${product.productName}`} className="market-product-image-link" to={`/products/${product.id}`}>
        <div className="market-product-image">
          {primaryImage
            ? <m.img alt={product.productName} decoding="async" loading="lazy" src={primaryImage.url} whileHover={{ scale: 1.055 }} transition={{ duration: 0.35 }} />
            : (
              <span aria-label="No product image" className="market-product-image-empty">
                <svg aria-hidden="true" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.4" viewBox="0 0 24 24">
                  <rect height="16" rx="2" width="18" x="3" y="4" />
                  <circle cx="8.5" cy="9" r="1.5" />
                  <path d="m21 15-5-5L5 20" />
                </svg>
                <span>Image coming soon</span>
              </span>
            )}
          {discount && <span className="market-discount-badge">{discount}% off</span>}
        </div>
      </Link>

      <div className="market-product-copy">
        <Link className="market-product-name" to={`/products/${product.id}`}>{product.productName}</Link>
        <div className="market-product-rating" aria-label={`${Number(product.averageRating || 0).toFixed(1)} out of 5 from ${product.reviewCount || 0} reviews`}>
          <span aria-hidden="true">★</span>
          <strong>{Number(product.averageRating || 0).toFixed(1)}</strong>
          <span>({Number(product.reviewCount || 0).toLocaleString()} reviews)</span>
        </div>
        <div className="market-product-price">
          <strong>{formatProductPrice(discount ? product.discountPrice : product.price)}</strong>
          {discount && <del>{formatProductPrice(product.price)}</del>}
        </div>
        <div className="market-product-actions">
          <m.button
            aria-label={`${isWishlisted ? 'Remove from' : 'Add to'} wishlist: ${product.productName}`}
            aria-pressed={isWishlisted}
            className={`market-wishlist-button${isWishlisted ? ' market-wishlist-button--active' : ''}`}
            disabled={userRole !== 'CUSTOMER' || Boolean(operation) || wishlistStatus === 'loading'}
            onClick={toggleWishlist}
            title={userRole !== 'CUSTOMER'
              ? 'Sign in with a customer account to save this product'
              : wishlistStatus === 'loading'
                ? 'Loading your wishlist'
                : `${isWishlisted ? 'Remove from' : 'Add to'} wishlist`}
            type="button"
            whileTap={{ scale: 0.88 }}
          >
            <svg aria-hidden="true" fill={isWishlisted ? 'currentColor' : 'none'} stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" viewBox="0 0 24 24">
              <path d="M20.8 8.7c0 4.1-8.8 10-8.8 10S3.2 12.8 3.2 8.7A4.3 4.3 0 0 1 11 5.9l1 1 1-1a4.3 4.3 0 0 1 7.8 2.8Z" />
            </svg>
          </m.button>
          <button
            className="market-cart-button"
            disabled={!inStock || Boolean(cartOperation)}
            onClick={addToCart}
            title={!inStock ? 'Out of stock' : 'Add to cart'}
            type="button"
          >
            {inStock ? 'Add to Cart' : 'Out of stock'}
          </button>
        </div>
        {wishlistFeedback && (
          <span
            className={`market-wishlist-feedback market-wishlist-feedback--${wishlistFeedback.type}`}
            role={wishlistFeedback.type === 'error' ? 'alert' : 'status'}
          >
            {wishlistFeedback.text}
          </span>
        )}
        <Link className="market-product-details-link" to={`/products/${product.id}`}>View details <span aria-hidden="true">→</span></Link>
      </div>
    </m.article>
  )
}
