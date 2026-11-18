import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import MainNavbar from '../../components/navbar/MainNavbar'
import ProductGrid from '../../components/product/ProductGrid'
import { getActiveCategories } from '../../services/categoryService'
import { getRequestError } from '../../services/authService'
import { getProductFacets, getPublicProducts } from '../../services/productService'
import useDebounce from '../../hooks/useDebounce'
import './Shop.css'

const PAGE_SIZE = 8
const PRICE_BANDS = [
  { label: 'Under ₹5,000', min: '', max: '5000' },
  { label: '₹5,000 – ₹15,000', min: '5000', max: '15000' },
  { label: '₹15,000 – ₹30,000', min: '15000', max: '30000' },
  { label: '₹30,000 – ₹50,000', min: '30000', max: '50000' },
  { label: 'Above ₹50,000', min: '50000', max: '' },
]

function Icon({ name }) {
  const paths = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></>,
    arrow: <><path d="M5 12h14M13 6l6 6-6 6"/></>,
    heart: <path d="M20.8 8.7c0 4.1-8.8 10-8.8 10S3.2 12.8 3.2 8.7A4.3 4.3 0 0 1 11 5.9l1 1 1-1a4.3 4.3 0 0 1 7.8 2.8Z"/>,
    menu: <><path d="M4 6h16M4 12h16M4 18h16"/></>,
    close: <><path d="m6 6 12 12M18 6 6 18"/></>,
  }
  return <svg aria-hidden="true" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" viewBox="0 0 24 24">{paths[name]}</svg>
}

function FilterBlock({ title, children }) {
  return <section className="shop-filter-block"><h3>{title}</h3>{children}</section>
}

function CategoryArtwork({ category, compact = false }) {
  const [failed, setFailed] = useState(false)
  return category.imageUrl && !failed
    ? <img className={compact ? 'shop-category-image is-compact' : 'shop-category-image'} src={category.imageUrl} alt="" loading="lazy" onError={() => setFailed(true)}/>
    : <Icon name="grid"/>
}

export default function Shop() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [searchInput, setSearchInput] = useState(searchParams.get('search') || '')
  const search = useDebounce(searchInput.trim(), 300)
  const [categories, setCategories] = useState([])
  const [facets, setFacets] = useState({ brands: [], categories: [] })
  const [filters, setFilters] = useState(() => ({
    categoryId: searchParams.get('categoryId') || '',
    minPrice: '', maxPrice: '', brand: '', minRating: '', discountedOnly: false, availability: 'ALL',
  }))
  const [products, setProducts] = useState([])
  const [page, setPage] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [sort, setSort] = useState('popular')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [categoryError, setCategoryError] = useState('')
  const [filtersOpen, setFiltersOpen] = useState(true)
  const [refreshCount, setRefreshCount] = useState(0)

  useEffect(() => {
    let mounted = true
    getActiveCategories().then((items) => { if (mounted) setCategories(items || []) })
      .catch((requestError) => { if (mounted) setCategoryError(getRequestError(requestError, 'Categories could not be loaded.')) })
    getProductFacets().then((result) => { if (mounted) setFacets(result || { brands: [], categories: [] }) })
      .catch((requestError) => { if (mounted) setCategoryError(getRequestError(requestError, 'Product filters could not be loaded.')) })
    return () => { mounted = false }
  }, [])

  useEffect(() => {
    let mounted = true
    const controller = new AbortController()
    const params = { page, size: PAGE_SIZE, sort, availability: filters.availability }
    if (search) params.search = search
    if (filters.categoryId) params.categoryId = filters.categoryId
    if (filters.brand) params.brand = filters.brand
    if (filters.minPrice) params.minPrice = filters.minPrice
    if (filters.maxPrice) params.maxPrice = filters.maxPrice
    if (filters.discountedOnly) params.discountedOnly = true
    if (filters.minRating) params.minimumRating = filters.minRating
    setLoading(true)
    setError('')
    getPublicProducts(params, controller.signal)
      .then((result) => {
        if (!mounted) return
        setProducts(result.items || [])
        setTotalElements(result.totalElements || 0)
        setTotalPages(result.totalPages || 0)
      })
      .catch((requestError) => {
        if (!mounted || controller.signal.aborted) return
        setProducts([]); setTotalElements(0); setTotalPages(0)
        setError(getRequestError(requestError, 'Products could not be loaded.'))
      })
      .finally(() => { if (mounted) setLoading(false) })
    return () => { mounted = false; controller.abort() }
  }, [filters, page, search, sort, refreshCount])

  const selectedCategory = useMemo(() => categories.find((category) => String(category.id) === filters.categoryId), [categories, filters.categoryId])
  const publicCategoryCounts = useMemo(() => new Map((facets.categories || []).map((item) => [String(item.categoryId), item.productCount])), [facets])
  const visibleCategories = useMemo(() => categories.filter((category) => publicCategoryCounts.has(String(category.id))), [categories, publicCategoryCounts])
  const rootCategories = useMemo(() => visibleCategories.filter((category) => !category.parentId), [visibleCategories])
  const heroProduct = products.find((product) => product.images?.length)
  const heroImage = heroProduct?.images?.find((image) => image.isPrimary)?.url || heroProduct?.images?.[0]?.url
  const firstResult = totalElements ? page * PAGE_SIZE + 1 : 0
  const lastResult = Math.min((page + 1) * PAGE_SIZE, totalElements)

  function updateFilter(key, value) {
    setPage(0)
    setFilters((current) => ({ ...current, [key]: value }))
    if (key === 'categoryId') {
      const next = new URLSearchParams(searchParams)
      if (value) next.set('categoryId', value)
      else next.delete('categoryId')
      setSearchParams(next, { replace: true })
    }
  }

  function resetFilters() {
    setPage(0)
    setFilters({ categoryId: '', minPrice: '', maxPrice: '', brand: '', minRating: '', discountedOnly: false, availability: 'ALL' })
    setSearchInput('')
    setSearchParams({}, { replace: true })
  }

  function selectPriceBand(band) { setFilters((current) => ({ ...current, minPrice: band.min, maxPrice: band.max })); setPage(0) }

  const filterPanel = (
    <aside className="shop-filters" aria-label="Shop filters">
      <div className="shop-filter-title">
        <h2><Icon name="grid"/> Filters</h2>
        <div className="shop-filter-title-actions">
          <button type="button" onClick={resetFilters}>Reset</button>
          <button aria-label="Close filters" className="shop-filter-close" onClick={() => setFiltersOpen(false)} type="button"><Icon name="close"/></button>
        </div>
      </div>
      <FilterBlock title="Categories">
        <div className="shop-filter-options shop-category-options">
          {visibleCategories.slice(0, 9).map((category) => (
            <button aria-pressed={filters.categoryId === String(category.id)} className={filters.categoryId === String(category.id) ? 'shop-category-option is-active' : 'shop-category-option'} key={category.id} onClick={() => updateFilter('categoryId', filters.categoryId === String(category.id) ? '' : String(category.id))} type="button">
              <span aria-hidden="true" className={filters.categoryId === String(category.id) ? 'shop-category-check is-checked' : 'shop-category-check'}>{filters.categoryId === String(category.id) && <svg viewBox="0 0 16 16"><path d="m3 8 3.2 3.2L13 4.8"/></svg>}</span><span className={category.imageUrl ? 'shop-category-dot has-image' : 'shop-category-dot has-icon'}><CategoryArtwork category={category} compact/></span>{category.name}<span className="shop-count">({Number(publicCategoryCounts.get(String(category.id))).toLocaleString()})</span>
            </button>
          ))}
          {!visibleCategories.length && <p className="shop-muted">No product categories available.</p>}
        </div>
      </FilterBlock>
      <FilterBlock title="Price Range">
        {PRICE_BANDS.map((band) => <label className="shop-check-row" key={band.label}><input checked={filters.minPrice === band.min && filters.maxPrice === band.max && (Boolean(band.min) || Boolean(band.max))} name="shop-price-band" onChange={() => selectPriceBand(band)} type="radio"/><span>{band.label}</span></label>)}
      </FilterBlock>
      <FilterBlock title="Brand">
        <div className="shop-filter-options">
          {(facets.brands || []).slice(0, 7).map((brand) => <label className="shop-check-row" key={brand.brand}><input checked={filters.brand === brand.brand} onChange={() => updateFilter('brand', filters.brand === brand.brand ? '' : brand.brand)} type="checkbox"/><span>{brand.brand}<em>({Number(brand.productCount).toLocaleString()})</em></span></label>)}
          {!facets.brands?.length && <p className="shop-muted">Brand filters appear when listed products include a brand.</p>}
        </div>
      </FilterBlock>
      <FilterBlock title="Customer Ratings">
        {[4, 3, 2].map((rating) => <label className="shop-check-row" key={rating}><input checked={Number(filters.minRating) === rating} name="shop-rating" onChange={() => updateFilter('minRating', Number(filters.minRating) === rating ? '' : String(rating))} type="radio"/><span>{rating} <b className="shop-stars">★</b> &amp; above</span></label>)}
      </FilterBlock>
      <FilterBlock title="Offers">
        <label className="shop-check-row"><input checked={filters.discountedOnly} onChange={(event) => updateFilter('discountedOnly', event.target.checked)} type="checkbox"/><span>Discounted products</span></label>
      </FilterBlock>
      <FilterBlock title="Availability">
        <label className="shop-check-row"><input type="checkbox" checked={filters.availability === 'IN_STOCK'} onChange={(event) => updateFilter('availability', event.target.checked ? 'IN_STOCK' : 'ALL')}/><span>In stock</span></label>
        <label className="shop-check-row"><input type="checkbox" checked={filters.availability === 'OUT_OF_STOCK'} onChange={(event) => updateFilter('availability', event.target.checked ? 'OUT_OF_STOCK' : 'ALL')}/><span>Out of stock</span></label>
      </FilterBlock>
    </aside>
  )

  return (
    <main className="marketplace-page shop-page">
      <MainNavbar shopVariant onSearchChange={setSearchInput} onSearchSubmit={(event) => event.preventDefault()} searchValue={searchInput}/>
      <nav className="shop-secondary-nav" aria-label="Shop navigation">
        <div className="shop-secondary-inner">
          <button type="button" className={!filters.categoryId ? 'is-selected' : ''} onClick={() => updateFilter('categoryId', '')}><Icon name="grid"/> All Categories <span>⌄</span></button>
          <button type="button" onClick={() => updateFilter('discountedOnly', true)}>Deals</button><button type="button" onClick={() => setSort('newest')}>New Arrivals</button>
          <button type="button" onClick={() => setSort('popular')}>Best Sellers</button>
          <Link to="/orders">Track Order</Link><span>Customer Support</span>
        </div>
      </nav>

      <div className="shop-shell">
        <div className="shop-breadcrumb"><Link to="/">Home</Link><span>›</span><span>{selectedCategory?.name || 'Shop'}</span></div>
        {categoryError && <p className="shop-error shop-error--filters" role="status">{categoryError}</p>}
        <div className={filtersOpen ? 'shop-layout' : 'shop-layout shop-layout--filters-closed'}>
          {filtersOpen && filterPanel}
          <section className="shop-main" aria-label="Shop products">
            <section className="shop-hero">
              <div className="shop-hero-copy"><span>SHOPSPHERE COLLECTIONS</span><h1>{selectedCategory ? 'Explore ' + selectedCategory.name : 'Find your next favourite.'}</h1><p>Browse products listed by ShopSphere sellers.</p><button onClick={() => document.getElementById('shop-product-list')?.scrollIntoView({ behavior: 'smooth' })} type="button">Shop Now <Icon name="arrow"/></button></div>
              <div className="shop-hero-art" aria-hidden="true">{heroImage && <img src={heroImage} alt=""/>}<span className="shop-hero-orb"/></div>
            </section>

            <nav className="shop-category-tiles" aria-label="Popular categories">
              {rootCategories.slice(0, 7).map((category) => <button className={filters.categoryId === String(category.id) ? 'shop-category-tile is-active' : 'shop-category-tile'} key={category.id} onClick={() => updateFilter('categoryId', String(category.id))} type="button"><span className="shop-category-tile-icon"><CategoryArtwork category={category}/></span><strong>{category.name}</strong><small>{Number(publicCategoryCounts.get(String(category.id))).toLocaleString()} products</small></button>)}
              {!rootCategories.length && <p className="shop-muted">Categories with active products will appear here.</p>}
            </nav>

            <div className="shop-result-toolbar" id="shop-product-list">
              <p>{loading ? 'Loading products…' : 'Showing ' + firstResult + '–' + lastResult + ' of ' + Number(totalElements).toLocaleString() + ' products'}</p>
              <div className="shop-toolbar-controls"><button aria-expanded={filtersOpen} className="shop-filter-toggle" onClick={() => setFiltersOpen((open) => !open)} type="button"><Icon name="menu"/> <span>Filters</span></button><label>Sort By: <select onChange={(event) => setSort(event.target.value)} value={sort}><option value="popular">Popularity</option><option value="newest">Newest</option><option value="price_asc">Price: Low to High</option><option value="price_desc">Price: High to Low</option></select></label></div>
            </div>

            {error ? <div className="shop-empty-state" role="alert"><h2>Products could not be loaded</h2><p>{error}</p><button onClick={() => setRefreshCount((current) => current + 1)} type="button">Retry</button></div> : <ProductGrid loading={loading} products={products}/>}
            {!loading && !error && products.length === 0 && <div className="shop-empty-state"><h2>No products found</h2><p>Try another category or adjust your filters.</p><button onClick={resetFilters} type="button">Clear filters</button></div>}
            {!loading && totalPages > 1 && <nav className="shop-pagination" aria-label="Product pages"><span>Showing {firstResult}–{lastResult} of {Number(totalElements).toLocaleString()} products</span><div><button disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))} type="button" aria-label="Previous page">‹</button><span>Page {page + 1} of {totalPages}</span><button disabled={page + 1 >= totalPages} onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))} type="button" aria-label="Next page">›</button></div></nav>}
          </section>
        </div>
      </div>
      {filtersOpen && <div className="shop-mobile-filter-backdrop" onClick={() => setFiltersOpen(false)}><div className="shop-mobile-filter-sheet" onClick={(event) => event.stopPropagation()}>{filterPanel}</div></div>}
    </main>
  )
}
