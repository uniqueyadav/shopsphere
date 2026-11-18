import { LazyMotion, MotionConfig, domAnimation, m } from 'framer-motion'
import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import ImageUploader from '../../components/forms/ImageUploader'
import ProductEditor from '../../components/product/ProductEditor'
import { getRequestError } from '../../services/authService'
import { getSellerProduct, updateSellerProduct } from '../../services/productService'
import './ProductManagement.css'

export default function EditProduct() {
  const { productId } = useParams()
  const location = useLocation()
  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState(location.state?.notice || '')

  useEffect(() => {
    let active = true
    getSellerProduct(productId)
      .then((result) => {
        if (active) setProduct(result)
      })
      .catch((requestError) => {
        if (active) setError(getRequestError(requestError, 'Could not load this product.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [productId])

  async function handleSubmit(values) {
    setSaving(true)
    setError('')
    setNotice('')
    try {
      setProduct(await updateSellerProduct(productId, values))
      setNotice('Product details saved.')
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not update this product.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <LazyMotion features={domAnimation}>
      <MotionConfig reducedMotion="user">
        <m.main
          animate={{ opacity: 1, y: 0 }}
          className="seller-products-page"
          initial={{ opacity: 0, y: 10 }}
          transition={{ duration: 0.3 }}
        >
          <div className="seller-products-topbar">
            <Link className="seller-products-brand" to="/account">ShopSphere <span>Seller Studio</span></Link>
            <Link className="seller-products-account" to="/seller/products">My products</Link>
          </div>
          <div className="seller-products-content">
            <div className="seller-products-breadcrumb">
              <Link to="/seller/products">Products</Link><span>/</span><strong>Edit product</strong>
            </div>
            {loading
              ? <div aria-label="Loading product" className="product-editor-skeleton" role="status" />
              : error && !product
                ? <p className="product-page-error" role="alert">{error}</p>
                : product && (
                  <>
                    <header className="seller-products-heading">
                      <div>
                        <span className="product-eyebrow">CATALOGUE · {product.status}</span>
                        <h1>Edit product</h1>
                        <p>Update product details and manage its image gallery.</p>
                      </div>
                    </header>
                    {notice && <p className="product-page-notice" role="status">{notice}</p>}
                    <section className="product-editor-card">
                      <ProductEditor
                        error={error}
                        onSubmit={handleSubmit}
                        product={product}
                        saving={saving}
                        submitLabel="Save changes"
                      />
                    </section>
                    <ImageUploader key={product.id} productId={product.id} />
                  </>
                )}
          </div>
        </m.main>
      </MotionConfig>
    </LazyMotion>
  )
}