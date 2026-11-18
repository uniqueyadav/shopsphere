import { useEffect, useMemo, useState } from 'react'
import SellerLayout, { SellerIcon } from '../../layouts/SellerLayout'
import { getRequestError } from '../../services/authService'
import { getActiveCategories } from '../../services/categoryService'
import { getSellerInventory, getSellerInventorySummary, getSellerStockHistory, updateSellerStock } from '../../services/inventoryService'
import { deleteSellerProduct } from '../../services/productService'
import './Inventory.css'

const PAGE_SIZE = 8
const LOW_STOCK_LIMIT = 10
const csvCell = (value) => `"${String(value ?? '').replaceAll('"', '""')}"`
const stockStatus = (quantity) => Number(quantity) <= 0 ? 'Out of Stock' : Number(quantity) <= LOW_STOCK_LIMIT ? 'Low Stock' : 'In Stock'
const imageFor = (product) => product.images?.find((image) => image.isPrimary)?.url || product.images?.[0]?.url
const updatedDate = (product) => product.updatedAt || product.createdAt

function InventoryIcon({ name, size = 20 }) {
  const paths = {
    cube: <><path d="m12 3 9 4.5v9L12 21l-9-4.5v-9L12 3Z"/><path d="m3 7.5 9 4.5 9-4.5M12 12v9"/></>,
    check: <><circle cx="12" cy="12" r="9"/><path d="m8 12 2.5 2.5L16 9"/></>,
    warning: <><path d="m10.3 4.3-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-2.7l-8-14a2 2 0 0 0-3.4 0Z"/><path d="M12 9v4m0 4h.01"/></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 4.5 4.5"/></>,
    edit: <><path d="m14 5 5 5M4 20l4.5-1 10-10a2.1 2.1 0 0 0-3-3l-10 10L4 20Z"/></>,
    history: <><path d="M3 12a9 9 0 1 0 2.6-6.4L3 8"/><path d="M3 3v5h5m4-1v5l3 2"/></>,
    trash: <><path d="M4 7h16m-10 4v6m4-6v6M6 7l1 14h10l1-14M9 7V4h6v3"/></>,
    download: <><path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4"/></>,
    refresh: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.6 9a7 7 0 0 1 11.7-2L20 12M4 12l2.7 5a7 7 0 0 0 11.7-2"/></>,
    info: <><circle cx="12" cy="12" r="9"/><path d="M12 11v5m0-8h.01"/></>,
    left: <path d="m15 18-6-6 6-6"/>, right: <path d="m9 18 6-6-6-6"/>,
  }
  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

function StatCard({ label, value, kind, icon }) {
  return <article className={`inventory-stat inventory-stat-${kind}`}><span><InventoryIcon name={icon} size={25}/></span><div><strong>{value}</strong><small>{label}</small></div></article>
}

export default function Inventory() {
  const [products, setProducts] = useState([])
  const [summary, setSummary] = useState(null)
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [status, setStatus] = useState('')
  const [stock, setStock] = useState('')
  const [dateRange, setDateRange] = useState('30')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState(null)
  const [quantity, setQuantity] = useState('')
  const [busy, setBusy] = useState(false)
  const [history, setHistory] = useState(null)
  const [historyEvents, setHistoryEvents] = useState([])
  const [historyLoading, setHistoryLoading] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let active = true
    getActiveCategories().then((items) => { if (active) setCategories(Array.isArray(items) ? items : []) }).catch(() => {})
    return () => { active = false }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => setSearch(searchInput.trim()), 300)
    return () => window.clearTimeout(timer)
  }, [searchInput])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    async function loadInventory() {
      const stateFilter = { 'In Stock': 'IN_STOCK', 'Low Stock': 'LOW_STOCK', 'Out of Stock': 'OUT_OF_STOCK' }[status]
      const stockFilter = { low: 'LOW_STOCK', zero: 'OUT_OF_STOCK' }[stock]
      const cutoff = dateRange === 'all' ? undefined : new Date(Date.now() - Number(dateRange) * 86400000).toISOString()
      const params = { size: 100, search: search || undefined, categoryId: category || undefined, status: stateFilter, stock: stockFilter, updatedAfter: cutoff }
      const firstPage = await getSellerInventory({ ...params, page: 0 })
      const [remaining, inventorySummary] = await Promise.all([
        Promise.all(Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) => getSellerInventory({ ...params, page: index + 1 }))),
        getSellerInventorySummary(),
      ])
      return { items: [firstPage, ...remaining].flatMap((result) => result.items || []), summary: inventorySummary }
    }
    loadInventory()
      .then((result) => { if (active) { setProducts(result.items); setSummary(result.summary) } })
      .catch((requestError) => { if (active) setError(getRequestError(requestError, 'Could not load inventory.')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [category, dateRange, refresh, search, status, stock])

  const stats = useMemo(() => ({
    total: products.length,
    inStock: products.filter((item) => stockStatus(item.stockQuantity) === 'In Stock').length,
    low: products.filter((item) => stockStatus(item.stockQuantity) === 'Low Stock').length,
    out: products.filter((item) => stockStatus(item.stockQuantity) === 'Out of Stock').length,
  }), [products])

  const filtered = useMemo(() => {
    const cutoff = dateRange === 'all' ? null : Date.now() - Number(dateRange) * 86400000
    return products.filter((item) => {
      const productStatus = stockStatus(item.stockQuantity)
      const quantityValue = Number(item.stockQuantity)
      const updated = updatedDate(item)
      return (!status || productStatus === status)
        && (!stock || (stock === 'available' ? quantityValue > 0 : stock === 'zero' ? quantityValue === 0 : quantityValue > 0 && quantityValue <= LOW_STOCK_LIMIT))
        && (!cutoff || (updated && new Date(updated).getTime() >= cutoff))
    })
  }, [dateRange, products, status, stock])
  const pageCount = Math.ceil(filtered.length / PAGE_SIZE)
  const rows = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)
  const shownFrom = filtered.length ? page * PAGE_SIZE + 1 : 0
  const shownTo = Math.min((page + 1) * PAGE_SIZE, filtered.length)

  useEffect(() => { setPage(0) }, [category, dateRange, search, status, stock])

  function exportInventory() {
    const header = ['Product', 'Category', 'SKU', 'Stock Quantity', 'Status', 'Last Updated']
    const lines = filtered.map((item) => [item.productName, item.categoryName, item.sku, item.stockQuantity, stockStatus(item.stockQuantity), updatedDate(item)].map(csvCell).join(','))
    const blob = new Blob([[header.map(csvCell).join(','), ...lines].join('\r\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'shopsphere-inventory.csv'
    link.click()
    URL.revokeObjectURL(url)
  }

  async function saveStock(event) {
    event.preventDefault()
    const parsed = Number(quantity)
    if (!Number.isInteger(parsed) || parsed < 0 || parsed > 2147483647) {
      setError('Enter a whole number from 0 to 2,147,483,647.')
      return
    }
    setBusy(true)
    setError('')
    try {
      await updateSellerStock(selected.id, parsed)
      setSelected(null)
      setNotice(`Stock updated for ${selected.productName}.`)
      setRefresh((value) => value + 1)
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not update stock.'))
    } finally { setBusy(false) }
  }

  function removeProduct(product) {
    setError('')
    setDeleteTarget(product)
  }

  async function confirmDeleteProduct() {
    if (!deleteTarget) return
    const product = deleteTarget
    setBusy(true)
    setError('')
    try {
      await deleteSellerProduct(product.id)
      setDeleteTarget(null)
      setNotice(`${product.productName} was permanently deleted.`)
      setRefresh((value) => value + 1)
    } catch (requestError) { setDeleteTarget(null); setError(getRequestError(requestError, 'Could not delete this product.')) }
    finally { setBusy(false) }
  }

  async function showHistory(product) {
    setHistory(product)
    setHistoryEvents([])
    setHistoryLoading(true)
    try {
      const events = await getSellerStockHistory(product.id)
      setHistoryEvents(Array.isArray(events) ? events : [])
    } catch (requestError) {
      setError(getRequestError(requestError, 'Could not load stock history.'))
    } finally { setHistoryLoading(false) }
  }

  return <SellerLayout>
    <main className="inventory-page">
      <div className="inventory-heading">
        <div className="inventory-title"><span className="inventory-title-icon"><InventoryIcon name="cube" size={34}/></span><div><h1>Inventory Management</h1><p>Track and manage your product inventory</p></div></div>
        <button className="inventory-button inventory-export" onClick={exportInventory} type="button"><InventoryIcon name="download"/> Export Inventory</button>
      </div>

      {error && <div className="inventory-alert is-error" role="alert">{error}<button onClick={() => setError('')} type="button" aria-label="Dismiss">×</button></div>}
      {notice && <div className="inventory-alert is-success" role="status">{notice}<button onClick={() => setNotice('')} type="button" aria-label="Dismiss">×</button></div>}

      <section aria-label="Inventory summary" className="inventory-stats">
        <StatCard label="Total Products" value={loading ? '—' : summary?.totalProducts ?? stats.total} kind="blue" icon="cube"/>
        <StatCard label="In Stock" value={loading ? '—' : summary?.inStock ?? stats.inStock} kind="green" icon="check"/>
        <StatCard label="Low Stock" value={loading ? '—' : summary?.lowStock ?? stats.low} kind="orange" icon="warning"/>
        <StatCard label="Out of Stock" value={loading ? '—' : summary?.outOfStock ?? stats.out} kind="violet" icon="cube"/>
      </section>

      <section aria-label="Inventory filters" className="inventory-filters">
        <label className="inventory-search"><InventoryIcon name="search"/><input aria-label="Search products" onChange={(event) => setSearchInput(event.target.value)} placeholder="Search products..." value={searchInput}/></label>
        <select aria-label="Filter by category" onChange={(event) => setCategory(event.target.value)} value={category}><option value="">All Categories</option>{categories.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select>
        <select aria-label="Filter by status" onChange={(event) => setStatus(event.target.value)} value={status}><option value="">All Status</option><option>In Stock</option><option>Low Stock</option><option>Out of Stock</option></select>
        <select aria-label="Filter by stock" onChange={(event) => setStock(event.target.value)} value={stock}><option value="">Stock: All</option><option value="available">Available</option><option value="low">Low stock</option><option value="zero">Zero stock</option></select>
        <select aria-label="Filter by last updated" onChange={(event) => setDateRange(event.target.value)} value={dateRange}><option value="7">Last 7 Days</option><option value="30">Last 30 Days</option><option value="90">Last 90 Days</option><option value="all">Any Date</option></select>
      </section>

      <section className="inventory-table-card" aria-label="Products inventory">
        <div className="inventory-table-scroll"><table className="inventory-table"><thead><tr><th scope="col">Product</th><th scope="col">Category</th><th scope="col">SKU</th><th scope="col">Stock Quantity</th><th scope="col">Status</th><th scope="col">Last Updated</th><th scope="col">Actions</th></tr></thead>
          <tbody>{loading ? <tr><td className="inventory-empty" colSpan="7">Loading your inventory…</td></tr> : rows.length ? rows.map((item) => {
            const label = stockStatus(item.stockQuantity)
            const date = updatedDate(item)
            return <tr key={item.id}><td><div className="inventory-product">{imageFor(item) ? <img alt="" loading="lazy" src={imageFor(item)}/> : <span className="inventory-placeholder"><InventoryIcon name="cube"/></span>}<strong>{item.productName}</strong></div></td><td>{item.categoryName || '—'}</td><td>{item.sku || '—'}</td><td className={label === 'Low Stock' ? 'is-low-quantity' : ''}>{item.stockQuantity}</td><td><span className={`inventory-status status-${label.toLowerCase().replaceAll(' ', '-')}`}><i/>{label}</span></td><td>{date ? new Date(date).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' }) : '—'}</td><td><div className="inventory-actions"><button aria-label={`Edit stock for ${item.productName}`} className="inventory-action" disabled={busy} onClick={() => { setSelected(item); setQuantity(String(item.stockQuantity)); setError('') }} title="Edit stock" type="button"><InventoryIcon name="edit" size={17}/></button><button aria-label={`View stock history for ${item.productName}`} className="inventory-action" onClick={() => showHistory(item)} title="Stock history" type="button"><InventoryIcon name="history" size={17}/></button><button aria-label={`Delete ${item.productName}`} className="inventory-action is-danger" disabled={busy} onClick={() => removeProduct(item)} title="Delete product" type="button"><InventoryIcon name="trash" size={17}/></button></div></td></tr>
          }) : <tr><td className="inventory-empty" colSpan="7">No products match these filters.</td></tr>}</tbody>
        </table></div>
      </section>

      <div className="inventory-pagination"><p>Showing <strong>{shownFrom}–{shownTo}</strong> of <strong>{filtered.length}</strong> products</p><div><button aria-label="Previous page" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))} type="button"><InventoryIcon name="left"/></button><span>Page {pageCount ? page + 1 : 0} of {pageCount}</span><button aria-label="Next page" disabled={page >= pageCount - 1} onClick={() => setPage((value) => Math.min(pageCount - 1, value + 1))} type="button"><InventoryIcon name="right"/></button></div></div>
      <div className="inventory-footer"><p><InventoryIcon name="info"/> Keep inventory quantities accurate to avoid order cancellations.</p><button className="inventory-button" disabled={loading} onClick={() => setRefresh((value) => value + 1)} type="button"><InventoryIcon name="refresh"/> Update Inventory</button></div>
    </main>

    {selected && <div className="inventory-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !busy) setSelected(null) }}><section aria-labelledby="inventory-edit-title" aria-modal="true" className="inventory-modal" role="dialog"><button aria-label="Close" className="inventory-modal-close" onClick={() => setSelected(null)} type="button">×</button><h2 id="inventory-edit-title">Update stock</h2><p>{selected.productName} <span>· {selected.sku}</span></p><form onSubmit={saveStock}><label htmlFor="inventory-quantity">Stock quantity</label><input autoFocus id="inventory-quantity" inputMode="numeric" max="2147483647" min="0" onChange={(event) => setQuantity(event.target.value)} required step="1" type="number" value={quantity}/><small>Enter a whole number from 0 upward. Zero marks this product out of stock.</small><div className="inventory-modal-actions"><button className="inventory-button is-muted" disabled={busy} onClick={() => setSelected(null)} type="button">Cancel</button><button className="inventory-button" disabled={busy} type="submit">{busy ? 'Saving…' : 'Save stock'}</button></div></form></section></div>}
    {history && <div className="inventory-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setHistory(null) }}><section aria-labelledby="inventory-history-title" aria-modal="true" className="inventory-modal" role="dialog"><button aria-label="Close" className="inventory-modal-close" onClick={() => setHistory(null)} type="button">×</button><h2 id="inventory-history-title">Stock history</h2><p>{history.productName}</p>{historyLoading ? <div className="inventory-history-note">Loading stock history…</div> : historyEvents.length ? <div className="inventory-history-list">{historyEvents.map((event) => <article key={event.id}><strong>{event.previousQuantity} <span>→</span> {event.newQuantity}</strong><small>{event.changeType.replaceAll('_', ' ')} · {new Date(event.changedAt).toLocaleString()}</small>{event.changedByName && <small>Updated by {event.changedByName}</small>}</article>)}</div> : <div className="inventory-history-note"><InventoryIcon name="info"/><span>No stock changes have been recorded for this product yet. Current quantity: {history.stockQuantity}.</span></div>}<button className="inventory-button inventory-history-done" onClick={() => setHistory(null)} type="button">Done</button></section></div>}
    {deleteTarget && <div className="inventory-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !busy) setDeleteTarget(null) }}><section aria-labelledby="inventory-delete-title" aria-modal="true" className="inventory-modal inventory-delete-modal" role="alertdialog"><span className="inventory-delete-icon"><InventoryIcon name="trash" size={22}/></span><h2 id="inventory-delete-title">Delete product?</h2><p><strong>{deleteTarget.productName}</strong> will be permanently removed with its inventory history. Products attached to existing orders cannot be deleted.</p><div className="inventory-modal-actions"><button className="inventory-button is-muted" disabled={busy} onClick={() => setDeleteTarget(null)} type="button">Cancel</button><button className="inventory-button is-delete" disabled={busy} onClick={confirmDeleteProduct} type="button">{busy ? 'Deleting…' : 'Delete product'}</button></div></section></div>}
  </SellerLayout>
}
