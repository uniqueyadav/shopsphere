import { useCallback, useEffect, useMemo, useState } from 'react'
import AdminLayout from '../../layouts/AdminLayout'
import { getRequestError } from '../../services/authService'
import { getActiveCategories } from '../../services/categoryService'
import {
  approveAdminProduct,
  createAdminProduct,
  deleteAdminProductImage,
  exportAdminProducts,
  getAdminProduct,
  getAdminProductSellers,
  getAdminProducts,
  getAdminProductStats,
  rejectAdminProduct,
  setAdminProductActive,
  setAdminProductPrimaryImage,
  updateAdminProduct,
  uploadAdminProductImages,
} from '../../services/productService'
import './AdminProducts.css'

const PAGE_SIZE = 10
const initialForm = {
  productName: '', sku: '', brand: '', categoryId: '', description: '',
  price: '', discountPrice: '', stockQuantity: '0', sellerId: '',
}

function Icon({ name, size = 20 }) {
  const paths = {
    box: <><path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="M3 8v9l9 5 9-5V8M12 13v9"/></>,
    check: <><circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    close: <><path d="m6 6 12 12M18 6 6 18"/></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></>,
    reset: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.6 9a7 7 0 0 1 11.7-2L20 12M4 12l2.7 5a7 7 0 0 0 11.7-2"/></>,
    eye: <><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></>,
    edit: <><path d="m4 16.5-.8 4.3 4.3-.8L19 7.5 16.5 5z"/><path d="m14.8 6.7 2.5 2.5"/></>,
    download: <><path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4"/></>,
    plus: <path d="M12 5v14m-7-7h14"/>,
    image: <><rect x="3" y="4" width="18" height="16"/><circle cx="8.5" cy="9" r="1.5"/><path d="m21 15-5-5L5 20"/></>,
    archive: <><path d="M3 5h18v4H3zM5 9v11h14V9m-9 4h4"/></>,
    arrow: <><path d="M5 12h14m-6-6 6 6-6 6"/></>,
  }
  return <svg aria-hidden="true" className="ap-icon" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name] || paths.box}</svg>
}

function money(value) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value || 0))
}

function dateLabel(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(value))
}

function statusTone(status, active) {
  if (!active) return 'archived'
  return String(status || 'DRAFT').toLowerCase()
}

function ProductStatus({ product }) {
  return <span className={`ap-status ap-status--${statusTone(product.status, product.isActive)}`}><i/>{product.isActive ? product.status : 'ARCHIVED'}</span>
}

function SummaryCard({ title, value, icon, tone, note }) {
  return <article className={`ap-stat ap-stat--${tone}`}>
    <span className="ap-stat-icon"><Icon name={icon} size={24}/></span>
    <div><span className="ap-stat-label">{title}</span><strong>{value == null ? '—' : Number(value || 0).toLocaleString('en-IN')}</strong><small>{note}</small></div>
  </article>
}

function ProductImage({ product, large = false }) {
  const image = product.images?.find((item) => item.isPrimary) || product.images?.[0]
  return <div className={`ap-product-image${large ? ' ap-product-image--large' : ''}`}>
    {image?.url ? <img alt="" src={image.url}/> : <Icon name="image" size={large ? 30 : 22}/>}
  </div>
}

export default function Products() {
  const [pageData, setPageData] = useState(null)
  const [stats, setStats] = useState(null)
  const [categories, setCategories] = useState([])
  const [sellers, setSellers] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [sellerId, setSellerId] = useState('')
  const [visibility, setVisibility] = useState('')
  const [filters, setFilters] = useState({ search: '', status: '', categoryId: '', sellerId: '', active: '' })
  const [page, setPage] = useState(0)
  const [drawerProduct, setDrawerProduct] = useState(null)
  const [drawerTab, setDrawerTab] = useState('overview')
  const [editorOpen, setEditorOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [step, setStep] = useState(1)
  const [form, setForm] = useState(initialForm)
  const [files, setFiles] = useState([])
  const [modalError, setModalError] = useState('')
  const [rejectProduct, setRejectProduct] = useState(null)
  const [rejectReason, setRejectReason] = useState('')
  const [confirmArchive, setConfirmArchive] = useState(null)
  const [confirmImageDelete, setConfirmImageDelete] = useState(null)

  const pageParams = useMemo(() => ({
    page, size: PAGE_SIZE,
    ...(filters.search ? { search: filters.search } : {}),
    ...(filters.status ? { status: filters.status } : {}),
    ...(filters.categoryId ? { categoryId: filters.categoryId } : {}),
    ...(filters.sellerId ? { sellerId: filters.sellerId } : {}),
    ...(filters.active !== '' ? { active: filters.active } : {}),
  }), [page, filters])

  const loadPage = useCallback(async (showLoader = true) => {
    if (showLoader) setLoading(true)
    setError('')
    try {
      const [result, countData] = await Promise.all([getAdminProducts(pageParams), getAdminProductStats()])
      setPageData(result)
      setStats(countData || null)
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not load marketplace products.'))
    } finally {
      if (showLoader) setLoading(false)
    }
  }, [pageParams])

  useEffect(() => { loadPage() }, [loadPage])
  useEffect(() => {
    let mounted = true
    Promise.all([getActiveCategories(), getAdminProductSellers()])
      .then(([categoryRows, sellerRows]) => {
        if (mounted) { setCategories(categoryRows || []); setSellers(sellerRows || []) }
      })
      .catch(() => { if (mounted) setToast('Some filter options could not be loaded.') })
    return () => { mounted = false }
  }, [])
  useEffect(() => {
    if (!toast) return undefined
    const timer = window.setTimeout(() => setToast(''), 4200)
    return () => window.clearTimeout(timer)
  }, [toast])

  function applyFilters(event) {
    event.preventDefault()
    setPage(0)
    setFilters({ search: query.trim(), status, categoryId, sellerId, active: visibility })
  }

  function resetFilters() {
    setQuery(''); setStatus(''); setCategoryId(''); setSellerId(''); setVisibility(''); setPage(0)
    setFilters({ search: '', status: '', categoryId: '', sellerId: '', active: '' })
  }

  async function exportRows() {
    setBusy(true)
    try {
      const blob = await exportAdminProducts(filters)
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url; anchor.download = 'shopsphere-products.csv'; anchor.click()
      URL.revokeObjectURL(url)
      setToast('Filtered product report exported.')
    } catch (requestError) { setToast(getRequestError(requestError, 'Could not export products.')) }
    finally { setBusy(false) }
  }

  function startCreate() {
    setEditingProduct(null); setForm({ ...initialForm, sellerId: sellers[0] ? String(sellers[0].id) : '' })
    setFiles([]); setStep(1); setModalError(''); setEditorOpen(true)
  }

  async function startEdit(product) {
    setModalError('')
    try {
      const fullProduct = await getAdminProduct(product.id)
      setEditingProduct(fullProduct)
      setForm({
        productName: fullProduct.productName || '', sku: fullProduct.sku || '', brand: fullProduct.brand || '',
        categoryId: String(fullProduct.categoryId || ''), description: fullProduct.description || '',
        price: String(fullProduct.price ?? ''), discountPrice: fullProduct.discountPrice == null ? '' : String(fullProduct.discountPrice),
        stockQuantity: String(fullProduct.stockQuantity ?? 0), sellerId: String(fullProduct.sellerId || ''),
      })
      setFiles([]); setStep(1); setEditorOpen(true)
    } catch (requestError) { setToast(getRequestError(requestError, 'Could not open product editor.')) }
  }

  function updateForm(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  async function saveProduct(event) {
    event.preventDefault()
    if (step < 3) { setStep((current) => current + 1); return }
    setBusy(true); setModalError('')
    const productPayload = {
      productName: form.productName.trim(), slug: '', sku: form.sku.trim(), brand: form.brand.trim(),
      categoryId: Number(form.categoryId), description: form.description.trim(), price: Number(form.price),
      discountPrice: form.discountPrice === '' ? null : Number(form.discountPrice), stockQuantity: Number(form.stockQuantity),
    }
    try {
      const saved = editingProduct
        ? await updateAdminProduct(editingProduct.id, productPayload)
        : await createAdminProduct(Number(form.sellerId), productPayload)
      let imageWarning = ''
      if (files.length) {
        try { await uploadAdminProductImages(saved.id, files) }
        catch (imageError) { imageWarning = ` Product saved, but images were not uploaded: ${getRequestError(imageError, 'upload failed')}` }
      }
      setEditorOpen(false); setFiles([]); setToast(`${editingProduct ? 'Product updated.' : 'Product created as a draft.'}${imageWarning}`)
      await loadPage(false)
      if (drawerProduct?.id === saved.id) setDrawerProduct(await getAdminProduct(saved.id))
    } catch (requestError) { setModalError(getRequestError(requestError, 'Could not save this product.')) }
    finally { setBusy(false) }
  }

  async function openDetails(product) {
    setDrawerTab('overview')
    try { setDrawerProduct(await getAdminProduct(product.id)) }
    catch (requestError) { setToast(getRequestError(requestError, 'Could not load product details.')) }
  }

  async function runProductAction(action, product) {
    setBusy(true)
    try {
      if (action === 'approve') await approveAdminProduct(product.id)
      if (action === 'reject') await rejectAdminProduct(product.id, rejectReason.trim())
      if (action === 'archive') await setAdminProductActive(product.id, false)
      if (action === 'activate') await setAdminProductActive(product.id, true)
      setRejectProduct(null); setConfirmArchive(null); setRejectReason('')
      setToast(action === 'approve' ? 'Product approved.' : action === 'reject' ? 'Product rejected.' : action === 'archive' ? 'Product archived.' : 'Product activated.')
      await loadPage(false)
      if (drawerProduct?.id === product.id) setDrawerProduct(await getAdminProduct(product.id))
    } catch (requestError) { setToast(getRequestError(requestError, 'The product action could not be completed.')) }
    finally { setBusy(false) }
  }

  async function markPrimary(imageId) {
    if (!drawerProduct) return
    try {
      const images = await setAdminProductPrimaryImage(drawerProduct.id, imageId)
      setDrawerProduct((current) => ({ ...current, images }))
    } catch (requestError) { setToast(getRequestError(requestError, 'Could not set the primary image.')) }
  }

  async function removeImage(imageId) {
    if (!drawerProduct) return
    setConfirmImageDelete({ product: drawerProduct, imageId })
  }

  async function confirmRemoveImage() {
    if (!confirmImageDelete) return
    try {
      const { product, imageId } = confirmImageDelete
      const images = await deleteAdminProductImage(product.id, imageId)
      setDrawerProduct((current) => ({ ...current, images }))
      setConfirmImageDelete(null)
      await loadPage(false)
    } catch (requestError) { setToast(getRequestError(requestError, 'Could not remove this image.')) }
  }

  const rows = pageData?.items || []
  const totalPages = pageData?.totalPages || 0

  return <AdminLayout onExport={exportRows} exporting={busy} exportLabel="Export Products" pageTitle="Products Management" pageSubtitle="Manage marketplace products, review submissions and control product visibility." breadcrumb="Products" showDateFilter={false}>
    <main className="admin-products-page">
      <div className="ap-page-actions"><button className="ap-button ap-button--primary" onClick={startCreate} type="button"><Icon name="plus"/>Add Product</button></div>

      {error && <div className="ap-alert" role="alert">{error}<button onClick={() => loadPage()} type="button">Retry</button></div>}

      <section aria-label="Product statistics" className="ap-stat-grid">
        <SummaryCard title="Total Products" value={stats?.total} icon="box" tone="purple" note="Across the marketplace"/>
        <SummaryCard title="Approved" value={stats?.approved} icon="check" tone="green" note="Approved listings"/>
        <SummaryCard title="Pending Review" value={stats?.pending} icon="clock" tone="orange" note="Awaiting moderation"/>
        <SummaryCard title="Rejected" value={stats?.rejected} icon="close" tone="red" note="Requires seller changes"/>
      </section>

      <form className="ap-filter-bar" onSubmit={applyFilters}>
        <label className="ap-search"><Icon name="search"/><input aria-label="Search products" onChange={(event) => setQuery(event.target.value)} placeholder="Search product name, SKU, brand or seller..." value={query}/></label>
        <select aria-label="Filter by category" onChange={(event) => setCategoryId(event.target.value)} value={categoryId}><option value="">All Categories</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select>
        <select aria-label="Filter by status" onChange={(event) => setStatus(event.target.value)} value={status}><option value="">All Statuses</option><option value="DRAFT">Draft</option><option value="PENDING">Pending</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option></select>
        <select aria-label="Filter by seller" onChange={(event) => setSellerId(event.target.value)} value={sellerId}><option value="">All Sellers</option>{sellers.map((seller) => <option key={seller.id} value={seller.id}>{seller.storeName}</option>)}</select>
        <select aria-label="Filter by visibility" onChange={(event) => setVisibility(event.target.value)} value={visibility}><option value="">Any Visibility</option><option value="true">Active</option><option value="false">Archived</option></select>
        <button className="ap-button ap-button--primary ap-filter-submit" type="submit"><Icon name="search"/>Search</button>
        <button className="ap-button ap-button--secondary ap-reset" onClick={resetFilters} type="button"><Icon name="reset"/>Reset</button>
      </form>

      <section aria-label="Marketplace product list" className="ap-table-card">
        <div className="ap-table-scroll"><table className="ap-table">
          <thead><tr><th className="ap-check-cell"><input aria-label="Select all visible products" type="checkbox"/></th><th>Product</th><th>Category</th><th>Seller</th><th>Price</th><th>Stock</th><th>Status</th><th>Updated</th><th>Actions</th></tr></thead>
          <tbody>
            {loading && Array.from({ length: 5 }, (_, index) => <tr className="ap-skeleton-row" key={`loading-${index}`}><td colSpan="9"><span/></td></tr>)}
            {!loading && rows.map((product) => <tr key={product.id}>
              <td className="ap-check-cell"><input aria-label={`Select ${product.productName}`} type="checkbox"/></td>
              <td><div className="ap-product-cell"><ProductImage product={product}/><div><strong>{product.productName}</strong><span>SKU: {product.sku}</span></div></div></td>
              <td><span className="ap-category-name">{product.categoryName || 'Uncategorized'}</span></td>
              <td><span className="ap-seller-name">{product.storeName || '—'}</span></td>
              <td><strong className="ap-price">{money(product.discountPrice || product.price)}</strong>{product.discountPrice != null && <del className="ap-old-price">{money(product.price)}</del>}</td>
              <td><span className={product.stockQuantity < 10 ? 'ap-stock ap-stock--low' : 'ap-stock'}>{product.stockQuantity} units</span></td>
              <td><ProductStatus product={product}/></td>
              <td>{dateLabel(product.updatedAt || product.createdAt)}</td>
              <td><div className="ap-row-actions">{(product.status === 'DRAFT' || product.status === 'PENDING') && product.isActive && <button aria-label={`Approve ${product.productName}`} disabled={busy} onClick={() => runProductAction('approve', product)} title="Approve product" type="button"><Icon name="check" size={18}/></button>}<button aria-label={`View ${product.productName}`} onClick={() => openDetails(product)} title="View product" type="button"><Icon name="eye" size={18}/></button><button aria-label={`Edit ${product.productName}`} onClick={() => startEdit(product)} title="Edit product" type="button"><Icon name="edit" size={18}/></button><button aria-label={product.isActive ? `Archive ${product.productName}` : `Activate ${product.productName}`} onClick={() => product.isActive ? setConfirmArchive(product) : runProductAction('activate', product)} title={product.isActive ? 'Archive product' : 'Activate product'} type="button"><Icon name="archive" size={18}/></button></div></td>
            </tr>)}
          </tbody>
        </table></div>
        {!loading && !rows.length && <div className="ap-empty"><span className="ap-empty-icon"><Icon name="box" size={26}/></span><h2>{error ? 'Product list unavailable' : 'No products found'}</h2><p>{error ? 'Retry the request or check your connection.' : 'Try changing the filters, or add a product to the marketplace.'}</p>{!error && <button className="ap-button ap-button--primary" onClick={startCreate} type="button"><Icon name="plus"/>Add Product</button>}</div>}
      </section>
      <footer className="ap-pagination"><span>Showing <strong>{pageData?.totalElements ? page * PAGE_SIZE + 1 : 0}–{Math.min((page + 1) * PAGE_SIZE, pageData?.totalElements || 0)}</strong> of <strong>{(pageData?.totalElements || 0).toLocaleString('en-IN')}</strong> products</span><div><button aria-label="Previous page" disabled={page <= 0 || loading} onClick={() => setPage((current) => current - 1)} type="button">‹</button><span>Page {totalPages ? page + 1 : 0} of {totalPages}</span><button aria-label="Next page" disabled={page + 1 >= totalPages || loading} onClick={() => setPage((current) => current + 1)} type="button">›</button></div></footer>
    </main>

    {editorOpen && <div className="ap-overlay" role="presentation"><section aria-labelledby="ap-editor-title" aria-modal="true" className="ap-dialog ap-editor-dialog" role="dialog">
      <header className="ap-dialog-header"><span className="ap-dialog-icon"><Icon name="box"/></span><div><h2 id="ap-editor-title">{editingProduct ? 'Edit Product' : 'Add New Product'}</h2><p>Step {step} of 3 · {['Product information', 'Pricing and inventory', 'Product images'][step - 1]}</p></div><button aria-label="Close editor" className="ap-icon-button" onClick={() => setEditorOpen(false)} type="button"><Icon name="close"/></button></header>
      <div aria-label="Product editor steps" className="ap-steps">{['Basic info', 'Price & stock', 'Images'].map((label, index) => <button aria-current={step === index + 1 ? 'step' : undefined} className={step >= index + 1 ? 'is-current' : ''} key={label} onClick={() => index + 1 < step && setStep(index + 1)} type="button"><b>{index + 1}</b><span>{label}</span></button>)}</div>
      <form className="ap-editor-form" onSubmit={saveProduct}>
        {modalError && <p className="ap-form-error" role="alert">{modalError}</p>}
        {step === 1 && <div className="ap-form-grid">
          {!editingProduct && <label className="ap-field ap-field--wide"><span>Seller <i>*</i></span><select name="sellerId" onChange={updateForm} required value={form.sellerId}><option value="">Select active approved seller</option>{sellers.map((seller) => <option key={seller.id} value={seller.id}>{seller.storeName} {seller.sellerName ? `· ${seller.sellerName}` : ''}</option>)}</select>{!sellers.length && <small>No active approved sellers available.</small>}</label>}
          <label className="ap-field ap-field--wide"><span>Product name <i>*</i></span><input autoFocus maxLength="150" name="productName" onChange={updateForm} required value={form.productName}/></label>
          <label className="ap-field"><span>SKU <i>*</i></span><input maxLength="100" name="sku" onChange={updateForm} pattern="[A-Za-z0-9._/-]+" required value={form.sku}/></label>
          <label className="ap-field"><span>Brand</span><input maxLength="100" name="brand" onChange={updateForm} value={form.brand}/></label>
          <label className="ap-field ap-field--wide"><span>Category <i>*</i></span><select name="categoryId" onChange={updateForm} required value={form.categoryId}><option value="">Select category</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
          <label className="ap-field ap-field--wide"><span>Description</span><textarea maxLength="5000" name="description" onChange={updateForm} rows="3" value={form.description}/></label>
        </div>}
        {step === 2 && <div className="ap-form-grid">
          <label className="ap-field"><span>Regular price (INR) <i>*</i></span><input min="0.01" name="price" onChange={updateForm} required step="0.01" type="number" value={form.price}/></label>
          <label className="ap-field"><span>Discount price (INR)</span><input min="0.01" name="discountPrice" onChange={updateForm} step="0.01" type="number" value={form.discountPrice}/></label>
          <label className="ap-field"><span>Stock quantity <i>*</i></span><input min="0" name="stockQuantity" onChange={updateForm} required step="1" type="number" value={form.stockQuantity}/></label>
          <div className="ap-info-note ap-field--wide">Creating a product saves it as <strong>Draft</strong>. Seller submitted products are reviewed through the moderation workflow.</div>
        </div>}
        {step === 3 && <div className="ap-image-step"><label className="ap-field"><span>Product images</span><input accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp" multiple onChange={(event) => setFiles(Array.from(event.target.files || []))} type="file"/><small>JPG, PNG, or WebP. Maximum 5 MB per image. Images upload after the product record is saved.</small></label>{files.length > 0 && <div className="ap-file-list">{files.map((file) => <span key={`${file.name}-${file.size}`}>{file.name}<small>{(file.size / 1024 / 1024).toFixed(2)} MB</small></span>)}</div>}{editingProduct?.images?.length > 0 && <p className="ap-info-note">Current images are managed from the product details panel.</p>}</div>}
        <footer className="ap-dialog-footer"><button className="ap-button ap-button--secondary" onClick={() => step === 1 ? setEditorOpen(false) : setStep((current) => current - 1)} type="button">{step === 1 ? 'Cancel' : 'Back'}</button><button className="ap-button ap-button--primary" disabled={busy || (!editingProduct && !sellers.length)} type="submit">{busy ? 'Saving…' : step === 3 ? (editingProduct ? 'Save Changes' : 'Create Draft') : <>Next <Icon name="arrow" size={17}/></>}</button></footer>
      </form>
    </section></div>}

    {drawerProduct && <div className="ap-drawer-backdrop" onClick={() => setDrawerProduct(null)} role="presentation"><aside aria-label="Product details" aria-modal="true" className="ap-drawer" onClick={(event) => event.stopPropagation()} role="dialog">
      <header className="ap-drawer-header"><span className="ap-dialog-icon"><Icon name="box"/></span><h2>Product Details</h2><button aria-label="Close product details" className="ap-icon-button" onClick={() => setDrawerProduct(null)} type="button"><Icon name="close"/></button></header>
      <div className="ap-drawer-product"><ProductImage large product={drawerProduct}/><div><h3>{drawerProduct.productName}</h3><p>SKU: {drawerProduct.sku}</p><ProductStatus product={drawerProduct}/><strong>{money(drawerProduct.discountPrice || drawerProduct.price)}</strong>{drawerProduct.discountPrice != null && <del>{money(drawerProduct.price)}</del>}</div></div>
      <nav aria-label="Product detail tabs" className="ap-drawer-tabs">{['overview', 'images', 'variants', 'reviews'].map((tab) => <button aria-current={drawerTab === tab ? 'page' : undefined} className={drawerTab === tab ? 'is-active' : ''} key={tab} onClick={() => setDrawerTab(tab)} type="button">{tab[0].toUpperCase() + tab.slice(1)}</button>)}</nav>
      <div className="ap-drawer-body">
        {drawerTab === 'overview' && <><dl className="ap-detail-list"><div><dt>Category</dt><dd>{drawerProduct.categoryName || '—'}</dd></div><div><dt>Seller</dt><dd>{drawerProduct.storeName || '—'}</dd></div><div><dt>Brand</dt><dd>{drawerProduct.brand || '—'}</dd></div><div><dt>Stock</dt><dd className={drawerProduct.stockQuantity < 10 ? 'ap-stock--low' : ''}>{drawerProduct.stockQuantity} units</dd></div><div><dt>Created</dt><dd>{dateLabel(drawerProduct.createdAt)}</dd></div><div><dt>Updated</dt><dd>{dateLabel(drawerProduct.updatedAt)}</dd></div><div><dt>Slug</dt><dd>{drawerProduct.slug}</dd></div><div><dt>Review status</dt><dd>{drawerProduct.status}{drawerProduct.rejectionReason ? ` · ${drawerProduct.rejectionReason}` : ''}</dd></div></dl><h3 className="ap-section-title">Description</h3><p className="ap-description">{drawerProduct.description || 'No product description has been provided.'}</p></>}
        {drawerTab === 'images' && <div className="ap-gallery">{drawerProduct.images?.length ? drawerProduct.images.map((image) => <article key={image.id}><img alt="Product" src={image.url}/><div><span>{image.isPrimary ? 'Primary image' : `Image ${image.displayOrder + 1}`}</span><div>{!image.isPrimary && <button onClick={() => markPrimary(image.id)} type="button">Set primary</button>}<button className="ap-danger-link" onClick={() => removeImage(image.id)} type="button">Remove</button></div></div></article>) : <div className="ap-drawer-empty">No product images uploaded.</div>}</div>}
        {drawerTab === 'variants' && <div className="ap-drawer-empty"><strong>Variants are not available</strong><p>The current product model does not store variant data.</p></div>}
        {drawerTab === 'reviews' && <div className="ap-drawer-empty"><strong>Review summary unavailable</strong><p>Product review data is not currently exposed by the backend.</p></div>}
      </div>
      <footer className="ap-drawer-footer"><button className="ap-button ap-button--secondary" onClick={() => startEdit(drawerProduct)} type="button"><Icon name="edit"/>Edit Product</button>{(drawerProduct.status === 'PENDING' || drawerProduct.status === 'DRAFT') && drawerProduct.isActive && <><button className="ap-button ap-button--success" disabled={busy} onClick={() => runProductAction('approve', drawerProduct)} type="button"><Icon name="check"/>Approve</button>{drawerProduct.status === 'PENDING' && <button className="ap-button ap-button--danger" onClick={() => { setRejectProduct(drawerProduct); setRejectReason('') }} type="button"><Icon name="close"/>Reject</button>}</>}</footer>
    </aside></div>}

    {rejectProduct && <div className="ap-overlay ap-overlay--front"><section aria-labelledby="ap-reject-title" aria-modal="true" className="ap-dialog ap-confirm-dialog" role="dialog"><header className="ap-dialog-header"><span className="ap-dialog-icon ap-dialog-icon--danger"><Icon name="close"/></span><div><h2 id="ap-reject-title">Reject product</h2><p>Provide a reason for the seller.</p></div><button aria-label="Close" className="ap-icon-button" onClick={() => setRejectProduct(null)} type="button"><Icon name="close"/></button></header><label className="ap-field"><span>Rejection reason <i>*</i></span><textarea maxLength="1000" onChange={(event) => setRejectReason(event.target.value)} required rows="4" value={rejectReason}/></label><footer className="ap-dialog-footer"><button className="ap-button ap-button--secondary" onClick={() => setRejectProduct(null)} type="button">Cancel</button><button className="ap-button ap-button--danger" disabled={busy || !rejectReason.trim()} onClick={() => runProductAction('reject', rejectProduct)} type="button">Confirm rejection</button></footer></section></div>}
    {confirmArchive && <div className="ap-overlay ap-overlay--front"><section aria-labelledby="ap-archive-title" aria-modal="true" className="ap-dialog ap-confirm-dialog" role="dialog"><header className="ap-dialog-header"><span className="ap-dialog-icon ap-dialog-icon--warning"><Icon name="archive"/></span><div><h2 id="ap-archive-title">Archive product?</h2><p>{confirmArchive.productName} will no longer be visible in the marketplace.</p></div><button aria-label="Close" className="ap-icon-button" onClick={() => setConfirmArchive(null)} type="button"><Icon name="close"/></button></header><footer className="ap-dialog-footer"><button className="ap-button ap-button--secondary" onClick={() => setConfirmArchive(null)} type="button">Keep product</button><button className="ap-button ap-button--danger" disabled={busy} onClick={() => runProductAction('archive', confirmArchive)} type="button">Archive product</button></footer></section></div>}
    {confirmImageDelete && <div className="ap-overlay ap-overlay--front"><section aria-labelledby="ap-image-remove-title" aria-modal="true" className="ap-dialog ap-confirm-dialog" role="dialog"><header className="ap-dialog-header"><span className="ap-dialog-icon ap-dialog-icon--danger"><Icon name="image"/></span><div><h2 id="ap-image-remove-title">Remove product image?</h2><p>This removes the image from the product and Cloudinary storage.</p></div><button aria-label="Close" className="ap-icon-button" onClick={() => setConfirmImageDelete(null)} type="button"><Icon name="close"/></button></header><footer className="ap-dialog-footer"><button className="ap-button ap-button--secondary" onClick={() => setConfirmImageDelete(null)} type="button">Cancel</button><button className="ap-button ap-button--danger" onClick={confirmRemoveImage} type="button">Remove image</button></footer></section></div>}
    {toast && <div className="ap-toast" role="status">{toast}<button aria-label="Dismiss" onClick={() => setToast('')} type="button"><Icon name="close" size={16}/></button></div>}
  </AdminLayout>
}
