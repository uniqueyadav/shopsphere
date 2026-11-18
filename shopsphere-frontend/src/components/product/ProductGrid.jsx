import { LazyMotion, MotionConfig, domAnimation, m } from 'framer-motion'
import ProductCard from './ProductCard'

export default function ProductGrid({ products, loading }) {
  if (loading) {
    return (
      <div aria-label="Loading products" className="market-product-grid" role="status">
        {Array.from({ length: 8 }, (_, index) => (
          <div className="market-product-skeleton" key={`product-skeleton-${index}`}>
            <div />
            <span />
            <span />
            <span />
          </div>
        ))}
        <span className="market-visually-hidden">Loading products…</span>
      </div>
    )
  }

  if (products.length === 0) {
    return null
  }

  return (
    <LazyMotion features={domAnimation}>
      <MotionConfig reducedMotion="user">
        <m.div
          animate="visible"
          className="market-product-grid"
          initial="hidden"
          variants={{
            hidden: {},
            visible: { transition: { staggerChildren: 0.035 } },
          }}
        >
          {products.map((product, index) => (
            <ProductCard index={index} key={product.id} product={product} />
          ))}
        </m.div>
      </MotionConfig>
    </LazyMotion>
  )
}