import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import SellerLayout, { SellerIcon } from '../../layouts/SellerLayout'
import { getRequestError } from '../../services/authService'
import { getActiveCategories } from '../../services/categoryService'
import { createSellerProduct, deleteSellerProduct, getSellerProduct, getSellerProducts } from '../../services/productService'
import './SellerProductsManagement.css'

const PAGE_SIZES = [5, 10, 20]
const TABS = [
  { id: 'ALL', label: 'All Products', query: {} },
  { id: 'ACTIVE', label: 'Active', query: { status: 'APPROVED', active: true } },
  { id: 'INACTIVE', label: 'Inactive', query: { active: false } },
  { id: 'OUT_OF_STOCK', label: 'Out of Stock', query: { active: true, outOfStock: true } },
]
const SORTS = [
  ['newest', 'Newest first'],
  ['name_asc', 'Name A–Z'],
  ['name_desc', 'Name Z–A'],
  ['price_asc', 'Price low to high'],
  ['price_desc', 'Price high to low'],
  ['stock_asc', 'Stock low to high'],
  ['stock_desc', 'Stock high to low'],
]

const money = (value) => value == null ? '—' : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value))

function Icon({ name, size = 19 }) {
  const shapes = {
    box: <><path d="m12 3 9 4.5v9L12 21l-9-4.5v-9L12 3Z"/><path d="m3.5 7.5 8.5 4 8.5-4M12 12v9"/></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></>,
    filter: <><path d="M4 6h16M7 12h10m-7 6h4"/></>,
    sort: <><path d="M8 4v16m0-16L5 7m3-3 3 3m5 13V4m0 16 3-3m-3 3-3-3"/></>,
    plus: <><path d="M12 5v14M5 12h14"/></>,
    edit: <><path d="m14 5 5 5M4 20l4.5-1 10-10a2.1 2.1 0 0 0-3-3l-10 10L4 20Z"/></>,
    copy: <><rect x="8" y="8" width="12" height="12"/><path d="M16 8V5a1 1 0 0 0-1-1H5a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h3"/></>,
    trash: <><path d="M4 7h16m-10 4v6m4-6v6M6 7l1 14h10l1-14M9 7V4h6v3"/></>,
    image: <><rect x="3" y="4" width="18" height="16"/><circle cx="8.5" cy="9" r="1.5"/><path d="m21 15-5-5L5 20"/></>,
    left: <><path d="m15 18-6-6 6-6"/></>,
    right: <><path d="m9 18 6-6-6-6"/></>,
    down: <path d="m7 10 5 5 5-5"/>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">{shapes[name]}</svg>
}

function productState(product) {
  if (!product.isActive) return { label: 'Inactive', className: 'inactive' }
  if (Number(product.stockQuantity) <= 0) return { label: 'Out of Stock', className: 'out' }
  if (product.status === 'APPROVED') return { label: 'Active', className: 'active' }
  const label = (product.status || 'DRAFT').toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
  return { label, className: label.toLowerCase().replaceAll(' ', '-') }
}

function ProductThumbnail({ product }) {
  const image = product.images?.find((item) => item.isPrimary) || product.images?.[0]
  return <span className="spm-thumb">{image?.url ? <img alt="" loading="lazy" src={image.url}/> : <Icon name="image" size={25}/>}</span>
}

function StatCard({ label, value, note, icon, tone }) {
  return <article className={`spm-stat spm-stat-${tone}`}><span className="spm-stat-icon"><SellerIcon name={icon} size={23}/></span><div><small>{label}</small><strong>{value}</strong><em>{note}</em></div></article>
}

export default function Products() {
  const [productsPage, setProductsPage] = useState(null)
  const [stats, setStats] = useState({ total: null, active: null, inactive: null, out: null })
  const [categories, setCategories] = useState([])
  const [tab, setTab] = useState('ALL')
  const [categoryId, setCategoryId] = useState('')
  const [sort, setSort] = useState('newest')
  const [pageNumber, setPageNumber] = useState(0)
  const [pageSize, setPageSize] = useState(5)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [statsLoading, setStatsLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [refreshKey, setRefreshKey] = useState(0)

  const selectedTab = TABS.find((item) => item.id === tab) || TABS[0]

  const loadStats = useCallback(async () => {
    setStatsLoading(true)
    try {
      const base = { page: 0, size: 1 }
      const [all, active, inactive, out] = await Promise.all([
        getSellerProducts({ ...base, sort: 'newest' }),
        getSellerProducts({ ...base, status: 'APPROVED', active: true, sort: 'newest' }),
        getSellerProducts({ ...base, active: false, sort: 'newest' }),
        getSellerProducts({ ...base, active: true, outOfStock: true, sort: 'newest' }),
      ])
      setStats({ total: all.totalElements, active: active.totalElements, inactive: inactive.totalElements, out: out.totalElements })
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not load product statistics.'))
    } finally {
      setStatsLoading(false)
    }
  }, [])

  useEffect(() => {
    let current = true
    getActiveCategories().then((result) => {
      if (current) setCategories(Array.isArray(result) ? result : [])
    }).catch(() => {
      if (current) setCategories([])
    })
    return () => { current = false }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setSearch(searchInput.trim())
      setPageNumber(0)
    }, 300)
    return () => window.clearTimeout(timer)
  }, [searchInput])

  useEffect(() => { loadStats() }, [loadStats, refreshKey])

  useEffect(() => {
    let current = true
    setLoading(true)
    setError('')
    const params = {
      page: pageNumber,
      size: pageSize,
      search: search || undefined,
      categoryId: categoryId || undefined,
      sort,
      ...selectedTab.query,
    }
    getSellerProducts(params).then((result) => {
      if (current) setProductsPage(result)
    }).catch((requestError) => {
      if (current) setError(getRequestError(requestError, 'Could not load your products.'))
    }).finally(() => {
      if (current) setLoading(false)
    })
    return () => { current = false }
  }, [categoryId, pageNumber, pageSize, refreshKey, search, selectedTab.query, sort])

  const products = productsPage?.items || []
  const totalPages = productsPage?.totalPages || 0
  const firstItem = productsPage?.totalElements ? pageNumber * pageSize + 1 : 0
  const lastItem = Math.min((pageNumber + 1) * pageSize, productsPage?.totalElements || 0)
  const pageButtons = useMemo(() => {
    const count = Math.min(totalPages, 5)
    const start = Math.max(0, Math.min(pageNumber - 2, totalPages - count))
    return Array.from({ length: count }, (_, index) => start + index)
  }, [pageNumber, totalPages])

  async function duplicateProduct(id) {
    setBusyId(id)
    setError('')
    setNotice('')
    try {
      const product = await getSellerProduct(id)
      const uniqueSuffix = Date.now().toString(36).toUpperCase()
      const copyName = `${product.productName} (Copy)`.slice(0, 150)
      await createSellerProduct({
        productName: copyName,
        slug: `${product.slug || product.productName.toLowerCase().replace(/[^a-z0-9]+/g, '-')}-copy-${uniqueSuffix}`.slice(0, 180),
        description: product.description || '',
        brand: product.brand || '',
        sku: `COPY-${product.id}-${uniqueSuffix}`,
        categoryId: product.categoryId,
        price: product.price,
        discountPrice: product.discountPrice,
        stockQuantity: product.stockQuantity,
      })
      setNotice('Product duplicated as a draft. Add its photos before publishing.')
      setTab('ALL')
      setCategoryId('')
      setSearchInput('')
      setSearch('')
      setSort('newest')
      setPageNumber(0)
      setRefreshKey((current) => current + 1)
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not duplicate this product.'))
    } finally {
      setBusyId(null)
    }
  }

  async function deleteProduct() {
    if (!deleteTarget) return
    const product = deleteTarget
    setBusyId(product.id)
    setError('')
    setNotice('')
    try {
      await deleteSellerProduct(product.id)
      setDeleteTarget(null)
      setNotice('Product permanently deleted.')
      setRefreshKey((current) => current + 1)
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not delete this product.'))
      setDeleteTarget(null)
    } finally {
      setBusyId(null)
    }
  }

  function chooseTab(nextTab) {
    setTab(nextTab)
    setPageNumber(0)
  }

  return <SellerLayout onSearchChange={setSearchInput} searchValue={searchInput}>
    <main className="spm-page">
      <header className="spm-heading">
        <div className="spm-title-wrap"><span className="spm-title-icon"><Icon name="box" size={27}/></span><div><h1>My Products</h1><p>Manage your products and keep your store updated.</p></div></div>
        <Link className="spm-add-button" to="/seller/products/new"><Icon name="plus"/>Add Product</Link>
      </header>

      {error && <div className="spm-message spm-message-error" role="alert">{error}<button onClick={() => { setError(''); setRefreshKey((value) => value + 1) }} type="button">Retry</button></div>}
      {notice && <div className="spm-message spm-message-success" role="status">{notice}<button aria-label="Dismiss notification" onClick={() => setNotice('')} type="button">×</button></div>}

      <section aria-label="Product statistics" className="spm-stats">
        <StatCard label="Total Products" value={statsLoading ? '…' : (stats.total ?? '—')} note="Products in your catalogue" icon="box" tone="blue"/>
        <StatCard label="Active Products" value={statsLoading ? '…' : (stats.active ?? '—')} note="Approved and available" icon="check" tone="green"/>
        <StatCard label="Inactive Products" value={statsLoading ? '…' : (stats.inactive ?? '—')} note="Deactivated products" icon="pause" tone="orange"/>
        <StatCard label="Total Views" value="—" note="Product view tracking unavailable" icon="eye" tone="violet"/>
      </section>

      <section className="spm-toolbar" aria-label="Product filters">
        <div className="spm-tabs" role="tablist" aria-label="Filter by product status">
          {TABS.map((item) => <button aria-selected={tab === item.id} className={tab === item.id ? 'is-active' : ''} key={item.id} onClick={() => chooseTab(item.id)} role="tab" type="button"><span>{item.label}</span><small>{statsLoading ? '…' : item.id === 'ALL' ? stats.total ?? '—' : item.id === 'ACTIVE' ? stats.active ?? '—' : item.id === 'INACTIVE' ? stats.inactive ?? '—' : stats.out ?? '—'}</small></button>)}
        </div>
        <div className="spm-controls">
          <label className="spm-search"><Icon name="search"/><input aria-label="Search products" onChange={(event) => setSearchInput(event.target.value)} placeholder="Search products…" value={searchInput}/></label>
          <label className="spm-select"><Icon name="filter"/><select aria-label="Filter category" onChange={(event) => { setCategoryId(event.target.value); setPageNumber(0) }} value={categoryId}><option value="">All categories</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select><Icon name="down" size={15}/></label>
          <label className="spm-select"><Icon name="sort"/><select aria-label="Sort products" onChange={(event) => { setSort(event.target.value); setPageNumber(0) }} value={sort}>{SORTS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select><Icon name="down" size={15}/></label>
        </div>
      </section>

      <section className="spm-table-card" aria-label="Products">
        <div className="spm-table-scroll"><table className="spm-table"><thead><tr><th scope="col">Product</th><th scope="col">Category</th><th scope="col">Price</th><th scope="col">Stock</th><th scope="col">Status</th><th scope="col">Views</th><th scope="col">Actions</th></tr></thead>
          <tbody>
            {loading && Array.from({ length: Math.min(pageSize, 5) }, (_, index) => <tr key={`loading-${index}`}><td colSpan="7"><span className="spm-loading-row"/></td></tr>)}
            {!loading && products.map((product) => {
              const status = productState(product)
              const busy = busyId === product.id
              return <tr key={product.id}>
                <td><div className="spm-product-cell"><ProductThumbnail product={product}/><div><strong>{product.productName}</strong><small>{product.brand || product.sku || '—'}</small></div></div></td>
                <td><span className="spm-category">{product.categoryName || '—'}</span></td>
                <td className="spm-price">{money(product.discountPrice || product.price)}{product.discountPrice != null && <del>{money(product.price)}</del>}</td>
                <td><span className={Number(product.stockQuantity) <= 5 ? 'spm-stock is-low' : 'spm-stock'}>{product.stockQuantity ?? '—'}</span></td>
                <td><span className={`spm-status spm-status-${status.className}`}><i/>{status.label}</span></td>
                <td className="spm-views" title="Product view tracking is not available">—</td>
                <td><div className="spm-actions"><Link aria-label={`Edit ${product.productName}`} className="spm-icon-button" to={`/seller/products/${product.id}/edit`} title="Edit product"><Icon name="edit"/></Link><button aria-label={`Duplicate ${product.productName}`} className="spm-icon-button" disabled={busy} onClick={() => duplicateProduct(product.id)} title="Duplicate product" type="button"><Icon name="copy"/></button><button aria-label={`Delete ${product.productName}`} className="spm-icon-button is-danger" disabled={busy} onClick={() => setDeleteTarget(product)} title="Delete product" type="button"><Icon name="trash"/></button></div></td>
              </tr>
            })}
            {!loading && products.length === 0 && <tr><td className="spm-empty-cell" colSpan="7">{search || categoryId ? 'No products match your search and filters.' : 'No products found in this section.'}<Link to="/seller/products/new">Add Product</Link></td></tr>}
          </tbody>
        </table></div>
      </section>

      <footer className="spm-pagination"><p>Showing <strong>{firstItem}–{lastItem}</strong> of <strong>{productsPage?.totalElements ?? 0}</strong> products</p><div className="spm-page-controls"><button aria-label="Previous page" disabled={pageNumber <= 0 || loading} onClick={() => setPageNumber((value) => Math.max(0, value - 1))} type="button"><Icon name="left"/></button>{pageButtons.map((number) => <button aria-current={number === pageNumber ? 'page' : undefined} className={number === pageNumber ? 'is-current' : ''} key={number} onClick={() => setPageNumber(number)} type="button">{number + 1}</button>)}<button aria-label="Next page" disabled={pageNumber >= totalPages - 1 || loading} onClick={() => setPageNumber((value) => Math.min(totalPages - 1, value + 1))} type="button"><Icon name="right"/></button><label><select aria-label="Rows per page" onChange={(event) => { setPageSize(Number(event.target.value)); setPageNumber(0) }} value={pageSize}>{PAGE_SIZES.map((size) => <option key={size} value={size}>{size} / page</option>)}</select><Icon name="down" size={15}/></label></div></footer>
    </main>
    {deleteTarget && <div className="spm-confirm-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && busyId !== deleteTarget.id) setDeleteTarget(null) }}><section aria-labelledby="spm-delete-title" aria-modal="true" className="spm-confirm-dialog" role="alertdialog"><span className="spm-confirm-icon"><Icon name="trash" size={22}/></span><h2 id="spm-delete-title">Delete product?</h2><p><strong>{deleteTarget.productName}</strong> will be permanently removed from your catalogue, including its images and inventory history. Products linked to existing orders cannot be deleted.</p><div className="spm-confirm-actions"><button disabled={busyId === deleteTarget.id} onClick={() => setDeleteTarget(null)} type="button">Cancel</button><button className="spm-confirm-delete" disabled={busyId === deleteTarget.id} onClick={deleteProduct} type="button">{busyId === deleteTarget.id ? 'Deleting…' : 'Delete product'}</button></div></section></div>}
  </SellerLayout>
}
